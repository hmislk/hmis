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
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final List<PaymentMethod> NON_CASH_METHODS = Arrays.asList(
            PaymentMethod.Card,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.ewallet,
            PaymentMethod.OnlineSettlement,
            PaymentMethod.OnCall
    );

    /**
     * Returns the non-cash payments currently held by the given user that are
     * eligible for settlement.
     *
     * Eligibility:
     *  - paymentMethod is one of the non-cash methods
     *  - paymentSettled is false
     *  - currentHolder is the given user
     *  - cashbookEntryCompleted is true (i.e. has been through first handover)
     *  - not retired, not cancelled
     */
    public List<Payment> findPendingSettlementPayments(WebUser holder) {
        if (holder == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT p FROM Payment p"
                + " WHERE p.paymentMethod IN :methods"
                + " AND p.paymentSettled = false"
                + " AND p.currentHolder = :holder"
                + " AND p.cashbookEntryCompleted = true"
                + " AND p.retired = false"
                + " AND p.cancelled = false"
                + " ORDER BY p.paymentMethod, p.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("methods", NON_CASH_METHODS);
        params.put("holder", holder);
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

        Department department = actor.getDepartment();
        Institution institution = department == null ? null : department.getInstitution();

        double total = 0.0;
        for (Payment p : payments) {
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
            total += p.getPaidValue();
        }

        Bill settlementBill = new Bill();
        settlementBill.setBillType(BillType.PaymentSettlementBill);
        settlementBill.setBillTypeAtomic(BillTypeAtomic.PAYMENT_SETTLEMENT_BILL);
        settlementBill.setBillDate(new Date());
        settlementBill.setBillTime(new Date());
        settlementBill.setCreatedAt(new Date());
        settlementBill.setCreater(actor);
        settlementBill.setFromWebUser(actor);
        settlementBill.setFromDepartment(department);
        settlementBill.setFromInstitution(institution);
        settlementBill.setDepartment(department);
        settlementBill.setInstitution(institution);
        settlementBill.setReferenceNumber(referenceNumber);
        settlementBill.setComments(comments);
        settlementBill.setTotal(total);
        settlementBill.setNetTotal(total);
        billFacade.create(settlementBill);

        for (Payment p : payments) {
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
        if (settlementBill.getBillTypeAtomic() != BillTypeAtomic.PAYMENT_SETTLEMENT_BILL) {
            throw new IllegalStateException("Bill is not a payment settlement bill");
        }
        if (settlementBill.isCancelled()) {
            throw new IllegalStateException("Settlement bill is already cancelled");
        }

        String jpql = "SELECT p FROM Payment p WHERE p.paymentSettlementBill = :b";
        Map<String, Object> params = new HashMap<>();
        params.put("b", settlementBill);
        List<Payment> settledPayments = paymentFacade.findByJpql(jpql, params);

        for (Payment p : settledPayments) {
            p.setPaymentSettled(false);
            p.setPaymentSettlementBill(null);
            p.setCurrentHolder(actor);
            paymentFacade.edit(p);
        }

        CancelledBill cancellationBill = new CancelledBill();
        cancellationBill.setBillType(BillType.PaymentSettlementBill);
        cancellationBill.setBillTypeAtomic(BillTypeAtomic.PAYMENT_SETTLEMENT_BILL_CANCELLED);
        cancellationBill.setBillClassType(BillClassType.CancelledBill);
        cancellationBill.setDepartment(settlementBill.getDepartment());
        cancellationBill.setInstitution(settlementBill.getInstitution());
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
        billFacade.create(cancellationBill);

        settlementBill.setCancelled(true);
        settlementBill.setCancelledBill(cancellationBill);
        billFacade.edit(settlementBill);

        return cancellationBill;
    }
}
