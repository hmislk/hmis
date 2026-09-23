/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.pharmacy.PharmacyPoCancellationException.Reason;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Shared cancel-approval logic for a Pharmacy Purchase Order Approval
 * ({@code BillTypeAtomic.PHARMACY_ORDER_APPROVAL}) bill, used by both the
 * JSF UI ({@code PharmacyBillSearch.pharmacyPoCancel()}) and the REST API
 * ({@code PharmacyPurchaseOrdersApi}).
 *
 * <p>Lifted out of {@code PharmacyBillSearch.pharmacyPoCancel()} /
 * {@code pharmacyCancelBillItems(CancelledBill)} (issue #23944), which fixes
 * a duplicate-row bug in the original: that method wrote each cancelled
 * {@code BillItem} up to three times via repeated {@code edit()}/cascade
 * calls on a still-transient entity. This service follows the pattern of
 * the already-correct sibling overload,
 * {@code pharmacyCancelBillItems(CancelledBill, List<Payment>)}: build the
 * inverted {@code BillItem} + {@code PharmaceuticalBillItem} pair fully in
 * memory, then persist the {@code BillItem} with a single {@code create()}
 * call (the {@code PharmaceuticalBillItem} cascades with it).</p>
 *
 * @author Buddhika
 */
@Stateless
public class PharmacyPurchaseOrderApprovalCancellationService {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private BillNumberGenerator billNumberGenerator;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    public CancelledBill cancelApproval(Long approvalBillId, String comment, WebUser actor) throws PharmacyPoCancellationException {
        if (approvalBillId == null) {
            throw new PharmacyPoCancellationException(Reason.NOT_FOUND, "No Bill to cancel");
        }

        Bill bill = billFacade.find(approvalBillId);
        if (bill == null) {
            throw new PharmacyPoCancellationException(Reason.NOT_FOUND, "No Bill to cancel");
        }

        if (bill.getBillType() != BillType.PharmacyOrderApprove) {
            throw new PharmacyPoCancellationException(Reason.NOT_APPROVAL_TYPE, "This bill is not a Pharmacy Purchase Order Approval bill");
        }

        if (bill.isCancelled()) {
            throw new PharmacyPoCancellationException(Reason.ALREADY_CANCELLED, "Already Cancelled. Can not cancel again");
        }

        if (!configOptionApplicationController.getBooleanValueByKey("Pharmacy Purchase Order Bill can be Cancelled", true)) {
            throw new PharmacyPoCancellationException(Reason.CONFIG_DISABLED, "Cancelling Pharmacy Purchase Order Bills is disabled");
        }

        if (checkGrnBlocksCancellation(bill)) {
            // Message intentionally mirrors PharmacyBillSearch.pharmacyErrorCheck()'s
            // wording verbatim - see checkGrnBlocksCancellation() for the (confusingly
            // named but working, do-not-fix) query it replicates.
            throw new PharmacyPoCancellationException(Reason.GRN_EXISTS, "Grn already head been Come u can't bill ");
        }

        // Atomically claim the cancellation with a conditional UPDATE before doing
        // any of the write work below - the isCancelled() check above can read from
        // the L2 cache and is not itself safe against a double-click or a concurrent
        // UI+API request for the same approval, which would otherwise each pass the
        // check and create their own CancelledBill/contra lines (#23988 review).
        // Mirrors the established claimReturnCancellationOrReportError() pattern in
        // PharmacyBillSearch.
        Map<String, Object> claim = new HashMap<>();
        claim.put("id", bill.getId());
        int claimed = billFacade.updateByJpql(
                "UPDATE Bill b SET b.cancelled = true WHERE b.id = :id AND b.cancelled = false", claim);
        if (claimed != 1) {
            throw new PharmacyPoCancellationException(Reason.ALREADY_CANCELLED, "Already Cancelled. Can not cancel again");
        }
        bill.setCancelled(true);

        CancelledBill cb = new CancelledBill();
        cb.setBilledBill(bill);
        cb.copy(bill);
        cb.setReferenceBill(bill.getReferenceBill());
        cb.invertAndAssignValuesFromOtherBill(bill);

        cb.setPaymentScheme(bill.getPaymentScheme());
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(actor);

        cb.setDepartment(bill.getDepartment());
        cb.setInstitution(bill.getInstitution());

        cb.setComments(comment);
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_APPROVAL_CANCELLED);
        cb.setCompleted(true);

        cb.setDeptId(billNumberGenerator.institutionBillNumberGenerator(bill.getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));
        cb.setInsId(billNumberGenerator.institutionBillNumberGenerator(bill.getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));

        billFacade.create(cb);

        cancelBillItems(bill, cb, actor);

        // Only the request bill's own referenceBill link is cleared - the approval's
        // referenceBill keeps pointing at its request permanently. That link is the
        // only surviving historical connection between the two and issue #23944's
        // status API depends on it, so it must NOT be nulled here (deliberate
        // divergence from the old pharmacyPoCancel(), which nulled both).
        //
        // Only clear it when the request's CURRENT referenceBill still points at
        // THIS approval. Live-testing against real local data (request 1115003)
        // showed a request can have more than one un-cancelled approval bill on
        // record (an earlier, superseded one whose own referenceBill still points
        // at the request, alongside the current live one the request actually
        // points back at) - cancelling the stale one must not blow away the
        // request's link to a different, still-live approval.
        Bill request = bill.getReferenceBill();
        if (request != null && request.getReferenceBill() != null
                && request.getReferenceBill().getId().equals(bill.getId())) {
            request.setReferenceBill(null);
            billFacade.edit(request);
        }

        bill.setCancelled(true);
        bill.setCancelledBill(cb);
        billFacade.edit(bill);

        return cb;
    }

    /**
     * Checks whether a live GRN has been raised against this approval.
     *
     * <p>{@code PharmacyBillSearch.checkGrn()} - the only prior caller of
     * this guard, reached exclusively through the old {@code pharmacyPoCancel()}
     * this service replaces (confirmed: {@code checkGrn()} has exactly one
     * caller in the whole codebase) - has two compounding bugs, found by
     * live-testing against real local data while building this service, not
     * assumed from reading it:
     * <ol>
     * <li>Its {@code b.billType=:btp} filter uses {@code BillType.PharmacyOrder}.
     * That is NOT a GRN bill type - it is the PO *request* bill's own
     * {@code BillType} (see {@code BillTypeAtomic.PHARMACY_ORDER}/
     * {@code PHARMACY_ORDER_APPROVAL}, both mapped to {@code BillType.PharmacyOrder}
     * in {@code BillTypeAtomic.java}). Because a live approval's request has
     * {@code request.referenceBill = approval} (see
     * {@code PurchaseOrderApprovingNativeSqlController.java:399-408}), this
     * filter always matches the request bill itself, never an actual GRN -
     * confirmed via {@code GET /api/pharmacy_purchase_orders/status} against
     * local bill 1509603/1115003, which returned the request bill (1115003)
     * in the "grns" list. Real GRN bills use {@code BillType.PharmacyGrnBill}
     * / {@code BillType.PharmacyGrnBillImport} (see {@code GrnController.java},
     * {@code GrnCostingController.java}) - used below instead.</li>
     * <li>Its return value is inverted: {@code !tmp.isEmpty()} (GRN-shaped
     * row found) returns {@code false}, so its caller's
     * "Grn already head been Come u can't bill" block-on-true check never
     * fires.</li>
     * </ol>
     * Combined, the existing guard has always been a complete no-op in
     * production - it never blocks approval-cancellation for any reason.
     * Neither bug is copied here: #23944 explicitly requires "refuses when
     * a GRN exists", and shipping the legacy behavior verbatim would ship
     * that requirement broken. The legacy {@code checkGrn()} itself is left
     * untouched (out of scope - fixing it is a separate concern from this
     * new, isolated method), but is now provably dead code.</p>
     */
    private boolean checkGrnBlocksCancellation(Bill approvalBill) {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and b.cancelled=false and b.billType IN :btps and "
                + " b.referenceBill=:ref and b.referenceBill.cancelled=false ";
        Map<String, Object> hm = new HashMap<>();
        hm.put("ref", approvalBill);
        hm.put("btps", java.util.Arrays.asList(BillType.PharmacyGrnBill, BillType.PharmacyGrnBillImport));
        List<Bill> tmp = billFacade.findByJpql(sql, hm);

        return !tmp.isEmpty();
    }

    /**
     * Builds and persists each cancelled {@code BillItem} exactly once,
     * following {@code PharmacyBillSearch.pharmacyCancelBillItems(CancelledBill, List<Payment>)}
     * (the correct sibling): the inverted {@code BillItem} and its
     * {@code PharmaceuticalBillItem} are fully built in memory and linked
     * both ways before a single {@code billItemFacade.create(...)} call,
     * which cascades the {@code PharmaceuticalBillItem} with it. No second
     * create/edit call follows for either entity - that duplicate write is
     * exactly the bug this service was extracted to fix.
     */
    private void cancelBillItems(Bill originalBill, CancelledBill newlyCreatedCancellingBill, WebUser actor) {
        for (BillItem originalBillItem : originalBill.getBillItems()) {
            BillItem newlyCreatedReturningItem = new BillItem();
            newlyCreatedReturningItem.copy(originalBillItem);
            newlyCreatedReturningItem.setBill(newlyCreatedCancellingBill);
            newlyCreatedReturningItem.invertValue(originalBillItem);

            if (newlyCreatedCancellingBill.getBillType() == BillType.PharmacyGrnBill || newlyCreatedCancellingBill.getBillType() == BillType.PharmacyGrnReturn) {
                newlyCreatedReturningItem.setReferanceBillItem(originalBillItem.getReferanceBillItem());
            } else {
                newlyCreatedReturningItem.setReferanceBillItem(originalBillItem);
            }

            newlyCreatedReturningItem.setCreatedAt(new Date());
            newlyCreatedReturningItem.setCreater(actor);

            PharmaceuticalBillItem newlyCreatedReturningPharmaceuticalBillItem = new PharmaceuticalBillItem();
            newlyCreatedReturningPharmaceuticalBillItem.copy(originalBillItem.getPharmaceuticalBillItem());
            newlyCreatedReturningPharmaceuticalBillItem.invertValue(originalBillItem.getPharmaceuticalBillItem());

            // Relationship is persistAll, so the PharmaceuticalBillItem does not need
            // (and must not get) a separate create() call - see the correct sibling.
            newlyCreatedReturningItem.setPharmaceuticalBillItem(newlyCreatedReturningPharmaceuticalBillItem);
            newlyCreatedReturningPharmaceuticalBillItem.setBillItem(newlyCreatedReturningItem);

            billItemFacade.create(newlyCreatedReturningItem);

            newlyCreatedCancellingBill.getBillItems().add(newlyCreatedReturningItem);
        }

        billFacade.edit(newlyCreatedCancellingBill);
    }
}
