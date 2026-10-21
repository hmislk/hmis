package com.divudi.service;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.RequestStatus;
import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Request;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.CashBook;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.RequestFacade;
import com.divudi.ejb.BillNumberGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Handles the Non-Cash Payment Settlement workflow (issue #17964).
 *
 * Settlement marks a non-cash Payment (Card / Cheque / Slip / eWallet / Online
 * settlement / similar) as confirmed by the bank or processor. After settlement,
 * the payment leaves the cashier system entirely and stops appearing in handovers.
 *
 * Settlement is a two-step maker-checker workflow (#24541): the holder submits a
 * request (payments linked to a pending PAYMENT_SETTLEMENT_BILL, nothing moves),
 * and a different user with ApproveNonCashSettlement approves it, which settles the
 * payments, debits the holder's drawer and debits the department's cash book
 * (#24627); cancelling an approved settlement reverses both.
 * See developer_docs/plans/2026-10-16-non-cash-settlement-approval-design.md.
 */
@Stateless
public class PaymentSettlementService {

    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @EJB
    private RequestFacade requestFacade;
    @EJB
    private RequestService requestService;
    @EJB
    private DrawerService drawerService;
    @EJB
    private CashbookService cashbookService;

    public static final String REQUEST_NO_PREFIX = "NCSET-";

    private static final List<PaymentMethod> NON_CASH_METHODS = Arrays.asList(
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.ewallet,
            PaymentMethod.OnlineSettlement
    );

    /**
     * Returns the distinct departments where the given holder currently has
     * non-cash payments eligible for settlement. Drives the department selector
     * on the Settle Non-Cash page.
     */
    public List<Department> findPendingSettlementDepartments(WebUser holder) {
        if (holder == null) {
            return new ArrayList<>();
        }
        // Note: ORDER BY p.department.name is illegal under JPA when used with DISTINCT
        // on a projection that does not include the ordered field — EclipseLink throws,
        // and paymentFacade.findObjects swallows the exception and returns null.
        // Sort in Java after the fetch.
        String jpql = "SELECT DISTINCT p.department FROM Payment p"
                + " WHERE p.paymentMethod IN :methods"
                + " AND p.paymentSettled = false"
                + " AND p.paymentSettlementBill IS NULL"
                + " AND p.handingOverStarted = false"
                + " AND p.currentHolder = :holder"
                + " AND p.cashbookEntryCompleted = true"
                + " AND p.retired = false"
                + " AND p.cancelled = false"
                + " AND p.department IS NOT NULL";
        Map<String, Object> params = new HashMap<>();
        params.put("methods", NON_CASH_METHODS);
        params.put("holder", holder);
        List<?> raw = paymentFacade.findObjects(jpql, params);
        List<Department> departments = new ArrayList<>();
        if (raw == null) {
            return departments;
        }
        for (Object o : raw) {
            if (o instanceof Department) {
                departments.add((Department) o);
            }
        }
        departments.sort(Comparator.comparing(
                Department::getName,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return departments;
    }

    /**
     * Returns the non-cash payments currently held by the given user, in the
     * given department, that are eligible for settlement.
     *
     * Eligibility:
     *  - paymentMethod is one of the non-cash methods
     *  - paymentSettled is false
     *  - currentHolder is the given user
     *  - department is the given department
     *  - cashbookEntryCompleted is true (i.e. has been through first handover)
     *  - not retired, not cancelled
     */
    public List<Payment> findPendingSettlementPayments(WebUser holder, Department department) {
        if (holder == null || department == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT p FROM Payment p"
                + " WHERE p.paymentMethod IN :methods"
                + " AND p.paymentSettled = false"
                + " AND p.paymentSettlementBill IS NULL"
                + " AND p.handingOverStarted = false"
                + " AND p.currentHolder = :holder"
                + " AND p.department = :department"
                + " AND p.cashbookEntryCompleted = true"
                + " AND p.retired = false"
                + " AND p.cancelled = false"
                + " ORDER BY p.paymentMethod, p.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("methods", NON_CASH_METHODS);
        params.put("holder", holder);
        params.put("department", department);
        return paymentFacade.findByJpql(jpql, params);
    }

    /**
     * Creates a pending PAYMENT_SETTLEMENT_BILL and a NON_CASH_SETTLEMENT_APPROVAL
     * request. Payments are linked to the bill but not settled; drawer and cash book untouched.
     *
     * @param payments the non-cash payments being settled
     * @param referenceNumber external reference (POS batch ID, bank slip number, etc.)
     * @param comments free-text notes
     * @param actor the logged-in user creating the settlement
     * @return the persisted settlement Bill
     */
    public Bill requestSettlement(List<Payment> payments,
                               String referenceNumber,
                               String comments,
                               WebUser actor) {
        if (payments == null || payments.isEmpty()) {
            throw new NonCashSettlementException("No payments selected for settlement");
        }
        if (actor == null) {
            throw new NonCashSettlementException("Actor is required");
        }

        // Re-fetch each payment from the DB inside this transaction (addresses CodeRabbit
        // concurrency concern + Codex holder-ownership concern). Validate against the
        // authoritative state, not the request-carried entities.
        List<Payment> authoritativePayments = new ArrayList<>();
        Department settlementDepartment = null;

        double total = 0.0;
        for (Payment incoming : payments) {
            if (incoming == null || incoming.getId() == null) {
                throw new NonCashSettlementException("Invalid payment in selection");
            }
            Payment p = paymentFacade.findWithLock(incoming.getId());
            if (p == null) {
                throw new NonCashSettlementException("Payment " + incoming.getId() + " not found");
            }
            if (!p.getCashbookEntryCompleted()) {
                throw new NonCashSettlementException(
                        "Payment " + p.getId() + " has no cash book entry — cannot settle"
                                + " before it has been through first handover");
            }
            if (p.isPaymentSettled()) {
                throw new NonCashSettlementException("Payment " + p.getId() + " is already settled");
            }
            if (p.getPaymentSettlementBill() != null) {
                throw new NonCashSettlementException(
                        "Payment " + p.getId() + " is already in a settlement request awaiting approval");
            }
            if (p.isHandingOverStarted()) {
                throw new NonCashSettlementException(
                        "Payment " + p.getId() + " is in a handover that has not been accepted yet — cannot settle");
            }
            if (NON_CASH_METHODS.indexOf(p.getPaymentMethod()) < 0) {
                throw new NonCashSettlementException(
                        "Payment " + p.getId() + " is not a non-cash payment — settlement does not apply");
            }
            // Holder ownership: the user submitting the settlement must currently hold each
            // payment. Guards against a race where the payment changed hands between the page
            // load and submit, or against a privileged user manipulating the form payload.
            if (p.getCurrentHolder() == null || !p.getCurrentHolder().equals(actor)) {
                throw new NonCashSettlementException(
                        "Payment " + p.getId() + " is not currently held by you — cannot settle");
            }
            if (p.getDepartment() == null) {
                throw new NonCashSettlementException(
                        "Payment " + p.getId() + " has no department — cannot settle");
            }
            if (settlementDepartment == null) {
                settlementDepartment = p.getDepartment();
            } else if (!p.getDepartment().equals(settlementDepartment)) {
                throw new NonCashSettlementException(
                        "All payments in a settlement must belong to the same department"
                                + " — payment " + p.getId() + " belongs to a different department");
            }
            authoritativePayments.add(p);
            total += p.getPaidValue();
        }

        Institution settlementInstitution = settlementDepartment.getInstitution();

        Bill settlementBill = new Bill();
        settlementBill.setBillType(BillType.PaymentSettlementBill);
        settlementBill.setBillTypeAtomic(BillTypeAtomic.PAYMENT_SETTLEMENT_BILL);
        settlementBill.setBillDate(new Date());
        settlementBill.setBillTime(new Date());
        settlementBill.setCreatedAt(new Date());
        settlementBill.setCreater(actor);
        settlementBill.setFromWebUser(actor);
        settlementBill.setFromDepartment(settlementDepartment);
        settlementBill.setFromInstitution(settlementInstitution);
        settlementBill.setDepartment(settlementDepartment);
        settlementBill.setInstitution(settlementInstitution);
        settlementBill.setReferenceNumber(referenceNumber);
        settlementBill.setComments(comments);
        settlementBill.setTotal(total);
        settlementBill.setNetTotal(total);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(
                settlementDepartment, BillTypeAtomic.PAYMENT_SETTLEMENT_BILL);
        settlementBill.setDeptId(deptId);
        settlementBill.setInsId(deptId);

        billFacade.create(settlementBill);

        for (Payment p : authoritativePayments) {
            p.setPaymentSettlementBill(settlementBill);
            paymentFacade.edit(p);
        }

        Request request = new Request();
        request.setRequestType(RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        request.setStatus(RequestStatus.PENDING);
        request.setBill(settlementBill);
        request.setRequester(actor);
        request.setRequestAt(new Date());
        request.setRequestReason(comments);
        request.setReferenceNumber(referenceNumber);
        request.setTargetWebUser(actor);
        request.setInstitution(settlementInstitution);
        request.setDepartment(settlementDepartment);
        requestService.save(request, actor);
        // Request ids are IDENTITY; flush so getId() is populated before building the request number.
        requestFacade.flush();
        request.setRequestNo(REQUEST_NO_PREFIX + request.getId());
        requestService.save(request, actor);

        return settlementBill;
    }

    /**
     * Cancels a previously created settlement bill, reversing the field updates
     * on every payment it settled. The payments re-enter the handover flow with
     * the cancelling actor as the new currentHolder.
     *
     * Follows the project's standard cancellation pattern: a new CancelledBill of
     * type PAYMENT_SETTLEMENT_BILL_CANCELLED is created and linked to the original
     * via billedBill / backwardReferenceBill.
     *
     * The cash book entries written at approval are reversed; settlements approved before #24627 wrote none, so nothing is reversed for them.
     * The holder's drawer is credited back only for settlements approved under #24541 (approveAt set); legacy settlements never debited it.
     */
    public Bill cancelSettlement(Bill settlementBill, String comments, WebUser actor) {
        if (settlementBill == null) {
            throw new NonCashSettlementException("Settlement bill is required");
        }
        if (actor == null) {
            throw new NonCashSettlementException("Actor is required");
        }
        settlementBill = billFacade.findWithLock(settlementBill.getId());
        if (settlementBill == null) {
            throw new NonCashSettlementException("Settlement bill not found");
        }
        if (settlementBill.getBillTypeAtomic() != BillTypeAtomic.PAYMENT_SETTLEMENT_BILL) {
            throw new NonCashSettlementException("Bill is not a payment settlement bill");
        }
        if (settlementBill.isCancelled()) {
            throw new NonCashSettlementException("Settlement bill is already cancelled");
        }
        if (settlementBill.isRetired()) {
            throw new NonCashSettlementException("Settlement bill was rejected or withdrawn — nothing to cancel");
        }
        Request pending = findRequestOfBill(settlementBill);
        if (pending != null && (pending.getStatus() == RequestStatus.PENDING
                || pending.getStatus() == RequestStatus.UNDER_REVIEW)) {
            throw new NonCashSettlementException("Settlement is still awaiting approval — reject or withdraw it instead");
        }
        boolean reverseDrawer = NonCashSettlementRules.reversesDrawerOnCancel(settlementBill);

        // Restore each payment to the holder it had at the time of settlement.
        // The settlement bill's fromWebUser is the user who created it, which is the
        // holder who owned the payments when they were settled.
        WebUser originalHolder = settlementBill.getFromWebUser();
        if (originalHolder == null) {
            throw new NonCashSettlementException(
                    "Settlement bill " + settlementBill.getId() + " has no fromWebUser — cannot restore holder");
        }

        String jpql = "SELECT p FROM Payment p WHERE p.paymentSettlementBill = :b";
        Map<String, Object> params = new HashMap<>();
        params.put("b", settlementBill);
        List<Payment> settledPayments = paymentFacade.findByJpql(jpql, params);

        for (Payment p : settledPayments) {
            p.setPaymentSettled(false);
            p.setPaymentSettlementBill(null);
            p.setCurrentHolder(originalHolder);
            paymentFacade.edit(p);
        }

        CancelledBill cancellationBill = new CancelledBill();
        cancellationBill.setBillType(BillType.PaymentSettlementBill);
        cancellationBill.setBillTypeAtomic(BillTypeAtomic.PAYMENT_SETTLEMENT_BILL_CANCELLED);
        cancellationBill.setBillClassType(BillClassType.CancelledBill);
        cancellationBill.setDepartment(settlementBill.getDepartment());
        cancellationBill.setInstitution(settlementBill.getInstitution());
        cancellationBill.setFromDepartment(settlementBill.getFromDepartment());
        cancellationBill.setFromInstitution(settlementBill.getFromInstitution());
        cancellationBill.setFromWebUser(settlementBill.getFromWebUser());
        cancellationBill.setCreater(actor);
        cancellationBill.setCreatedAt(new Date());
        cancellationBill.setBillDate(new Date());
        cancellationBill.setBillTime(new Date());
        cancellationBill.setComments(comments);
        cancellationBill.setBilledBill(settlementBill);
        cancellationBill.setBackwardReferenceBill(settlementBill);
        cancellationBill.setTotal(-settlementBill.getTotal());
        cancellationBill.setNetTotal(-settlementBill.getNetTotal());

        if (settlementBill.getDepartment() != null) {
            String cancelDeptId = billNumberGenerator.departmentBillNumberGeneratorYearly(
                    settlementBill.getDepartment(), BillTypeAtomic.PAYMENT_SETTLEMENT_BILL_CANCELLED);
            cancellationBill.setDeptId(cancelDeptId);
            cancellationBill.setInsId(cancelDeptId);
        }

        billFacade.create(cancellationBill);

        // Put the settled payments back into the cash book they left at approval.
        cashbookService.reverseFundMovementEntries(settlementBill, cancellationBill, actor);

        if (reverseDrawer) {
            Drawer drawer = drawerService.getUsersDrawer(originalHolder);
            if (drawer == null) {
                throw new NonCashSettlementException("Drawer not found for " + originalHolder.getName());
            }
            for (Map.Entry<PaymentMethod, Double> e : NonCashSettlementRules.totalsByMethod(settledPayments).entrySet()) {
                drawerService.applyDrawerAdjustment(drawer, e.getKey(), e.getValue(), cancellationBill, actor);
            }
        }

        settlementBill.setCancelled(true);
        settlementBill.setCancelledBill(cancellationBill);
        settlementBill.setEditor(actor);
        settlementBill.setEditedAt(new Date());
        billFacade.edit(settlementBill);

        return cancellationBill;
    }

    public Request findPendingRequestOf(WebUser requester) {
        if (requester == null) {
            return null;
        }
        String jpql = "SELECT q FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type"
                + " AND q.status = :status"
                + " AND q.requester = :requester"
                + " ORDER BY q.id";
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        params.put("status", RequestStatus.PENDING);
        params.put("requester", requester);
        return requestFacade.findFirstByJpql(jpql, params);
    }

    public List<Request> findPendingRequestsOf(WebUser requester) {
        if (requester == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT q FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type"
                + " AND q.status = :status"
                + " AND q.requester = :requester"
                + " ORDER BY q.id";
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        params.put("status", RequestStatus.PENDING);
        params.put("requester", requester);
        return requestFacade.findByJpql(jpql, params);
    }

    public long countPendingRequests() {
        String jpql = "SELECT COUNT(q) FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type"
                + " AND q.status = :status";
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        params.put("status", RequestStatus.PENDING);
        return requestFacade.findLongByJpql(jpql, params);
    }

    public List<Request> findRequests(RequestStatus status, Department department, Date fromDate, Date toDate) {
        StringBuilder jpql = new StringBuilder("SELECT q FROM Request q"
                + " WHERE q.retired = false"
                + " AND q.requestType = :type");
        Map<String, Object> params = new HashMap<>();
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        if (status != null) {
            jpql.append(" AND q.status = :status");
            params.put("status", status);
        }
        if (department != null) {
            jpql.append(" AND q.department = :dep");
            params.put("dep", department);
        }
        if (fromDate != null) {
            jpql.append(" AND q.requestAt >= :fd");
            params.put("fd", fromDate);
        }
        if (toDate != null) {
            jpql.append(" AND q.requestAt <= :td");
            params.put("td", toDate);
        }
        jpql.append(" ORDER BY q.id DESC");
        return requestFacade.findByJpql(jpql.toString(), params, javax.persistence.TemporalType.TIMESTAMP);
    }

    public List<Payment> findPaymentsOfSettlementBill(Bill settlementBill) {
        if (settlementBill == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT p FROM Payment p WHERE p.paymentSettlementBill = :b ORDER BY p.paymentMethod, p.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("b", settlementBill);
        return paymentFacade.findByJpql(jpql, params);
    }

    private Request loadPendingRequest(Long requestId) {
        if (requestId == null) {
            throw new NonCashSettlementException("Request is required");
        }
        // Row lock so concurrent approve/reject/withdraw serialise and the second sees the non-pending status.
        Request request = requestFacade.findWithLock(requestId);
        if (request == null || request.isRetired()
                || request.getRequestType() != RequestType.NON_CASH_SETTLEMENT_APPROVAL) {
            throw new NonCashSettlementException("Settlement request not found");
        }
        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.UNDER_REVIEW) {
            throw new NonCashSettlementException("Request " + request.getRequestNo() + " is no longer pending");
        }
        Bill bill = request.getBill();
        if (bill == null || bill.isRetired() || bill.isCancelled() || bill.getApproveAt() != null) {
            throw new NonCashSettlementException("Request " + request.getRequestNo() + " has no pending settlement bill");
        }
        return request;
    }

    public void approveSettlement(Long requestId, WebUser approver) {
        Request request = loadPendingRequest(requestId);
        WebUser requester = request.getRequester();
        String blocker = NonCashSettlementRules.approvalBlocker(requester, approver);
        if (blocker != null) {
            throw new NonCashSettlementException(blocker);
        }
        Bill bill = request.getBill();
        List<Payment> payments = findPaymentsOfSettlementBill(bill);
        if (payments.isEmpty()) {
            throw new NonCashSettlementException("Request " + request.getRequestNo() + " has no payments");
        }
        for (Payment p : payments) {
            String paymentBlocker = NonCashSettlementRules.paymentApprovalBlocker(p, bill, requester);
            if (paymentBlocker != null) {
                throw new NonCashSettlementException(paymentBlocker);
            }
        }
        Drawer drawer = drawerService.getUsersDrawer(requester);
        if (drawer == null) {
            throw new NonCashSettlementException("Drawer not found for " + requester.getName());
        }

        for (Payment p : payments) {
            p.setPaymentSettled(true);
            p.setCurrentHolder(null);
            paymentFacade.edit(p);
        }
        // Settled payments leave the cash book of the settlement's department (#24627).
        for (Payment p : payments) {
            Department cashbookDepartment = bill.getDepartment() != null ? bill.getDepartment() : p.getDepartment();
            CashBook cashbook = cashbookService.findAndSaveCashBookForDepartment(cashbookDepartment);
            cashbookService.writeFundMovementEntry(p, -Math.abs(p.getPaidValue()), cashbook, bill, approver);
        }
        // applyDrawerAdjustment flushes and clears the persistence context; payment edits above are already merged, bill/request below are re-merged.
        for (Map.Entry<PaymentMethod, Double> e : NonCashSettlementRules.totalsByMethod(payments).entrySet()) {
            drawerService.applyDrawerAdjustment(drawer, e.getKey(), -e.getValue(), bill, approver);
        }

        Date now = new Date();
        bill.setApproveUser(approver);
        bill.setApproveAt(now);
        billFacade.edit(bill);

        request.setApproved(true);
        request.setApprovedBy(approver);
        request.setApprovedAt(now);
        request.setStatus(RequestStatus.APPROVED);
        requestService.save(request, approver);
    }

    public void rejectSettlement(Long requestId, WebUser actor, String reason) {
        if (actor == null) {
            throw new NonCashSettlementException("Actor is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new NonCashSettlementException("A reason is required to reject a settlement request");
        }
        Request request = loadPendingRequest(requestId);
        String blocker = NonCashSettlementRules.approvalBlocker(request.getRequester(), actor);
        if (blocker != null) {
            throw new NonCashSettlementException(blocker.replace("approve", "reject"));
        }
        releasePayments(request, actor, reason.trim());
        Date now = new Date();
        request.setRejected(true);
        request.setRejectedBy(actor);
        request.setRejectedAt(now);
        request.setRejectionReason(reason.trim());
        request.setStatus(RequestStatus.REJECTED);
        requestService.save(request, actor);
    }

    public void withdrawSettlement(Long requestId, WebUser actor) {
        if (actor == null) {
            throw new NonCashSettlementException("Actor is required");
        }
        Request request = loadPendingRequest(requestId);
        if (!actor.equals(request.getRequester())) {
            throw new NonCashSettlementException("Only the user who made the request can withdraw it");
        }
        releasePayments(request, actor, "Withdrawn by requester");
        Date now = new Date();
        request.setCancelled(true);
        request.setCancelledBy(actor);
        request.setCancelledAt(now);
        request.setCancellationReason("Withdrawn by requester");
        request.setStatus(RequestStatus.CANCELLED);
        requestService.save(request, actor);
    }

    private void releasePayments(Request request, WebUser actor, String reason) {
        Bill bill = request.getBill();
        for (Payment p : findPaymentsOfSettlementBill(bill)) {
            if (p.isPaymentSettled()) {
                continue;
            }
            p.setPaymentSettlementBill(null);
            paymentFacade.edit(p);
        }
        bill.setRetired(true);
        bill.setRetirer(actor);
        bill.setRetiredAt(new Date());
        bill.setRetireComments(reason);
        billFacade.edit(bill);
    }

    private Request findRequestOfBill(Bill bill) {
        String jpql = "SELECT q FROM Request q WHERE q.retired = false AND q.bill = :b AND q.requestType = :type ORDER BY q.id DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("b", bill);
        params.put("type", RequestType.NON_CASH_SETTLEMENT_APPROVAL);
        return requestFacade.findFirstByJpql(jpql, params);
    }
}
