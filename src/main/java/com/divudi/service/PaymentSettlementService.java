package com.divudi.service;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PaymentFacade;
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
 * Settlement does NOT write to the Cash Book. The payment was already recorded
 * in the cash book at first handover. See
 * developer_docs/cashier-drawer-cashbook-master-design.md Phase 4 for the full design.
 */
@Stateless
public class PaymentSettlementService {

    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;

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
     * Creates a PAYMENT_SETTLEMENT_BILL grouping the given payments and marks
     * them settled. No cash book entry is written.
     *
     * @param payments the non-cash payments being settled
     * @param referenceNumber external reference (POS batch ID, bank slip number, etc.)
     * @param comments free-text notes
     * @param actor the logged-in user creating the settlement
     * @return the persisted settlement Bill
     */
    public Bill settlePayments(List<Payment> payments,
                               String referenceNumber,
                               String comments,
                               WebUser actor) {
        if (payments == null || payments.isEmpty()) {
            throw new IllegalArgumentException("No payments selected for settlement");
        }
        if (actor == null) {
            throw new IllegalArgumentException("Actor is required");
        }

        // Re-fetch each payment from the DB inside this transaction (addresses CodeRabbit
        // concurrency concern + Codex holder-ownership concern). Validate against the
        // authoritative state, not the request-carried entities.
        List<Payment> authoritativePayments = new ArrayList<>();
        Department settlementDepartment = null;

        double total = 0.0;
        for (Payment incoming : payments) {
            if (incoming == null || incoming.getId() == null) {
                throw new IllegalArgumentException("Invalid payment in selection");
            }
            Payment p = paymentFacade.find(incoming.getId());
            if (p == null) {
                throw new IllegalStateException("Payment " + incoming.getId() + " not found");
            }
            if (!p.getCashbookEntryCompleted()) {
                throw new IllegalStateException(
                        "Payment " + p.getId() + " has no cash book entry — cannot settle"
                                + " before it has been through first handover");
            }
            if (p.isPaymentSettled()) {
                throw new IllegalStateException("Payment " + p.getId() + " is already settled");
            }
            if (NON_CASH_METHODS.indexOf(p.getPaymentMethod()) < 0) {
                throw new IllegalStateException(
                        "Payment " + p.getId() + " is not a non-cash payment — settlement does not apply");
            }
            // Holder ownership: the user submitting the settlement must currently hold each
            // payment. Guards against a race where the payment changed hands between the page
            // load and submit, or against a privileged user manipulating the form payload.
            if (p.getCurrentHolder() == null || !p.getCurrentHolder().equals(actor)) {
                throw new IllegalStateException(
                        "Payment " + p.getId() + " is not currently held by you — cannot settle");
            }
            if (p.getDepartment() == null) {
                throw new IllegalStateException(
                        "Payment " + p.getId() + " has no department — cannot settle");
            }
            if (settlementDepartment == null) {
                settlementDepartment = p.getDepartment();
            } else if (!p.getDepartment().equals(settlementDepartment)) {
                throw new IllegalStateException(
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
            p.setPaymentSettled(true);
            p.setPaymentSettlementBill(settlementBill);
            p.setCurrentHolder(null);
            paymentFacade.edit(p);
        }

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
     * Cash book is NOT touched.
     */
    public Bill cancelSettlement(Bill settlementBill, String comments, WebUser actor) {
        if (settlementBill == null) {
            throw new IllegalArgumentException("Settlement bill is required");
        }
        if (actor == null) {
            throw new IllegalArgumentException("Actor is required");
        }
        if (settlementBill.getBillTypeAtomic() != BillTypeAtomic.PAYMENT_SETTLEMENT_BILL) {
            throw new IllegalStateException("Bill is not a payment settlement bill");
        }
        if (settlementBill.isCancelled()) {
            throw new IllegalStateException("Settlement bill is already cancelled");
        }

        // Restore each payment to the holder it had at the time of settlement.
        // The settlement bill's fromWebUser is the user who created it, which is the
        // holder who owned the payments when they were settled.
        WebUser originalHolder = settlementBill.getFromWebUser();
        if (originalHolder == null) {
            throw new IllegalStateException(
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

        settlementBill.setCancelled(true);
        settlementBill.setCancelledBill(cancellationBill);
        settlementBill.setEditor(actor);
        settlementBill.setEditedAt(new Date());
        billFacade.edit(settlementBill);

        return cancellationBill;
    }
}
