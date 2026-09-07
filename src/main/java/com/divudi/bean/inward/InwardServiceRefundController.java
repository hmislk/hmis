/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 * Inward service bill refund (issue #21247): return selected inward service
 * items from a bill without cancelling the whole bill. Kept separate from
 * OPD / Collecting-Centre refunds (BillSearch) because inpatient refunds have
 * different concerns - no drawer / cash-in-hand check, credit-spend against
 * the BHT encounter rather than a cash collection. The lab-sampling guard
 * (issue #22987) reuses the same PatientInvestigationStatus whitelist and
 * dataEntered check already used by InwardSearch#checkCancelBill /
 * #checkInvestigation for the sibling "Cancel whole bill" flow, so a
 * laboratory investigation item can't be returned once its sample has been
 * received (or its report already entered) - only before the lab has taken
 * the sample, or again once the lab has retired/rejected it.
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@ViewScoped
public class InwardServiceRefundController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    private InwardSearch inwardSearch;
    @Inject
    private BillController billController;
    @Inject
    private AdmissionController admissionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private EnumController enumController;
    @Inject
    private ConfigOptionController configOptionController;

    private List<BillItem> refundingItems;
    private String comment;
    private PaymentMethod paymentMethod;
    private double refundAmount;
    private boolean printPreview;

    // <editor-fold defaultstate="collapsed" desc="Print format settings dialog">
    /*
     * Backing values for the Print Settings dialog on
     * inward_bill_service_refund.xhtml, so a department can switch a refund
     * print format on or off from the page itself instead of going to Config
     * Options. See
     * developer_docs/configuration/printer-configuration-system.md.
     *
     * This page had no config keys before - the format came only from the
     * deprecated departmentPreference selector - so every key here is new and
     * department-scoped, and every one defaults to false. A department that
     * never opens the dialog keeps printing exactly what its preference says.
     */
    private static final String KEY_CUSTOM_1 = "Inward Service Bill Refund - Show Custom 1 Format";
    private static final String KEY_FIVE_FIVE = "Inward Service Bill Refund - Show 5x5 Format";
    private static final String KEY_FIVE_FIVE_PRINTED = "Inward Service Bill Refund - Show 5x5 Pre-printed Format";
    private static final String KEY_POS = "Inward Service Bill Refund - Show POS Format";
    private static final String KEY_A4 = "Inward Service Bill Refund - Show A4 Format";
    private static final String KEY_A4_PRINTED = "Inward Service Bill Refund - Show A4 Pre-printed Format";

    private boolean printFormatCustom1;
    private boolean printFormatFiveFive;
    private boolean printFormatFiveFivePrinted;
    private boolean printFormatPos;
    private boolean printFormatA4;
    private boolean printFormatA4Printed;

    public void loadPrintConfig() {
        printFormatCustom1 = configOptionController.getBooleanValueByKeyReadOnly(KEY_CUSTOM_1, false);
        printFormatFiveFive = configOptionController.getBooleanValueByKeyReadOnly(KEY_FIVE_FIVE, false);
        printFormatFiveFivePrinted = configOptionController.getBooleanValueByKeyReadOnly(KEY_FIVE_FIVE_PRINTED, false);
        printFormatPos = configOptionController.getBooleanValueByKeyReadOnly(KEY_POS, false);
        printFormatA4 = configOptionController.getBooleanValueByKeyReadOnly(KEY_A4, false);
        printFormatA4Printed = configOptionController.getBooleanValueByKeyReadOnly(KEY_A4_PRINTED, false);
    }

    public void savePrintConfig() {
        if (!webUserController.hasPrivilege("ChangeReceiptPrintingPaperTypes")) {
            JsfUtil.addErrorMessage("You do not have privilege to change print format settings");
            return;
        }
        // These keys are meant to be department-scoped. With no department
        // selected (SessionController.loginActionWithoutDepartment()),
        // ConfigOptionController.setBooleanValueByKey falls back to the plain
        // application key, which every department without an override inherits
        // - so one unscoped save would silently change the format for the whole
        // application. Refuse rather than write the wrong scope.
        if (sessionController.getDepartment() == null) {
            JsfUtil.addErrorMessage("Select a department before changing print format settings");
            return;
        }
        try {
            configOptionController.setBooleanValueByKey(KEY_CUSTOM_1, printFormatCustom1);
            configOptionController.setBooleanValueByKey(KEY_FIVE_FIVE, printFormatFiveFive);
            configOptionController.setBooleanValueByKey(KEY_FIVE_FIVE_PRINTED, printFormatFiveFivePrinted);
            configOptionController.setBooleanValueByKey(KEY_POS, printFormatPos);
            configOptionController.setBooleanValueByKey(KEY_A4, printFormatA4);
            configOptionController.setBooleanValueByKey(KEY_A4_PRINTED, printFormatA4Printed);
            JsfUtil.addSuccessMessage("Print format settings saved successfully");
            loadPrintConfig();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving print format settings: " + e.getMessage());
        }
    }

    public boolean isPrintFormatCustom1() {
        return printFormatCustom1;
    }

    public void setPrintFormatCustom1(boolean printFormatCustom1) {
        this.printFormatCustom1 = printFormatCustom1;
    }

    public boolean isPrintFormatFiveFive() {
        return printFormatFiveFive;
    }

    public void setPrintFormatFiveFive(boolean printFormatFiveFive) {
        this.printFormatFiveFive = printFormatFiveFive;
    }

    public boolean isPrintFormatFiveFivePrinted() {
        return printFormatFiveFivePrinted;
    }

    public void setPrintFormatFiveFivePrinted(boolean printFormatFiveFivePrinted) {
        this.printFormatFiveFivePrinted = printFormatFiveFivePrinted;
    }

    public boolean isPrintFormatPos() {
        return printFormatPos;
    }

    public void setPrintFormatPos(boolean printFormatPos) {
        this.printFormatPos = printFormatPos;
    }

    public boolean isPrintFormatA4() {
        return printFormatA4;
    }

    public void setPrintFormatA4(boolean printFormatA4) {
        this.printFormatA4 = printFormatA4;
    }

    public boolean isPrintFormatA4Printed() {
        return printFormatA4Printed;
    }

    public void setPrintFormatA4Printed(boolean printFormatA4Printed) {
        this.printFormatA4Printed = printFormatA4Printed;
    }
    // </editor-fold>

    // Navigated to from the inward service bill reprint page. The bill lives in
    // the session-scoped InwardSearch (set on the reprint page), so this
    // controller stays view-scoped and reads the bill from there - surviving the
    // faces-redirect to the refund page. Here we only reset the refund-workflow
    // state for the fresh view.
    public String navigateToRefundInwardServiceBill(Bill b) {
        if (b == null || b.getId() == null) {
            JsfUtil.addErrorMessage("No bill to refund");
            return null;
        }
        if (b.getCheckeAt() != null) {
            JsfUtil.addErrorMessage("This bill is already checked. A checked bill's services cannot be returned.");
            return null;
        }
        inwardSearch.setBill(billFacade.find(b.getId()));
        return "/inward/inward_bill_service_refund?faces-redirect=true";
    }

    // Back navigation. The bill / encounter context already lives in
    // InwardSearch (and AdmissionController), so these just route.
    public String navigateBackToReprint() {
        return "/inward/inward_reprint_bill_service?faces-redirect=true";
    }

    public String navigateBackToInpatientProfile() {
        Bill bill = getBill();
        if (bill != null && bill.getPatientEncounter() instanceof Admission) {
            admissionController.setCurrent((Admission) bill.getPatientEncounter());
        }
        return "/inward/admission_profile?faces-redirect=true";
    }

    public String navigateToInwardServiceSearch() {
        return "/inward/inward_search_service?faces-redirect=true";
    }

    // Recalculate the total from the currently selected items only. Bound to the
    // datatable selection ajax events. Guards against re-refunding an item.
    public void calculateRefundTotal() {
        refundAmount = 0.0;
        if (refundingItems == null) {
            return;
        }
        for (BillItem bi : refundingItems) {
            refundAmount += bi.getNetValue();
        }
    }

    public String refundInwardServiceBill() {
        Bill sessionBill = getBill();
        if (sessionBill == null || sessionBill.getId() == null) {
            JsfUtil.addErrorMessage("No bill to refund");
            return null;
        }
        // Reload and revalidate from the DB right before mutating - the bill is
        // session-held and may be stale. Push the fresh entity back into
        // InwardSearch so downstream getBill() calls use it too.
        Bill bill = billFacade.find(sessionBill.getId());
        if (bill == null || bill.isRetired()) {
            JsfUtil.addErrorMessage("Bill not available");
            return null;
        }
        if (bill.getCheckedBy() != null) {
            JsfUtil.addErrorMessage("Checked Bill. Can not return");
            return null;
        }
        if (bill.getPatientEncounter() != null && bill.getPatientEncounter().isNursingDischarged()
                && !webUserController.hasPrivilege("InwardProcessReturnAfterNursingDischarge")) {
            JsfUtil.addErrorMessage("Cannot return services: nursing discharge has been confirmed for this patient.");
            return null;
        }
        if (bill.getPatientEncounter() != null && bill.getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry, patient is discharged.");
            return null;
        }
        inwardSearch.setBill(bill);
        if (!isSupportedForReturn(bill)) {
            JsfUtil.addErrorMessage("Unsupported bill type for inward service return");
            return null;
        }
        if (refundingItems == null || refundingItems.isEmpty()) {
            JsfUtil.addErrorMessage("Select at least one item to return");
            return null;
        }
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return null;
        }
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill is cancelled. Cannot return");
            return null;
        }
        // Per-item / per-fee guards (same verification OPD refund applies):
        //  - already returned: a non-retired refund item references it.
        //  - fee already paid to the service provider (staff professional
        //    payment done): block unless that payment has been cancelled.
        for (BillItem selected : refundingItems) {
            BillItem original = billItemFacade.find(selected.getId());
            if (original == null) {
                continue;
            }
            if (itemAlreadyReturned(original)) {
                JsfUtil.addErrorMessage("One or more selected items are already returned");
                return null;
            }
            String labGuardError = checkLabSampleGuard(original);
            if (labGuardError != null) {
                JsfUtil.addErrorMessage(labGuardError);
                return null;
            }
            for (BillFee bf : originalFeesOf(original)) {
                if (billController.hasRefunded(bf)) {
                    JsfUtil.addErrorMessage("One or more fees are already refunded. Cannot refund again.");
                    return null;
                }
                if (billController.hasPaidToStaff(bf)) {
                    JsfUtil.addErrorMessage("One or more fees are already paid to the Service Provider. Cancel the payment before returning.");
                    return null;
                }
            }
        }

        RefundBill rb = createRefundBill();
        refundSelectedItems(rb);

        // Totals are the sum of the inverted (negative) selected items, not a
        // copy of the original bill - this is what makes a partial return work.
        billFacade.editAndCommit(rb);

        // Flag the original bill as refunded on every pass - this is the guard
        // that prevents the bill from being cancelled once any item has been
        // returned. Later passes can still return the remaining items because
        // the Return button is gated on "all items returned", not on this flag.
        bill.setRefundedBill(rb);
        List<Bill> refundBills = bill.getRefundBills() != null
                ? new ArrayList<>(bill.getRefundBills())
                : new ArrayList<>();
        refundBills.add(rb);
        bill.setRefundBills(refundBills);
        bill.getForwardReferenceBills().add(rb);
        bill.setRefunded(true);
        billFacade.editAndCommit(bill);

        // Reload into session so the print-preview (which reads getBill()) and
        // the item list reflect the persisted refunded state.
        inwardSearch.setBill(billFacade.find(bill.getId()));
        printPreview = true;
        JsfUtil.addSuccessMessage("Returned");
        return null;
    }

    private RefundBill createRefundBill() {
        Bill bill = getBill();
        RefundBill rb = new RefundBill();
        rb.setBilledBill(bill);
        rb.setReferenceBill(bill);
        rb.setBillType(BillType.InwardBill);
        rb.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
        rb.setIpOpOrCc("IP");

        rb.setPatient(bill.getPatient());
        rb.setPatientEncounter(bill.getPatientEncounter());
        rb.setCreditCompany(bill.getCreditCompany());

        rb.setFromDepartment(bill.getFromDepartment());
        rb.setFromInstitution(bill.getFromInstitution());
        rb.setToDepartment(bill.getToDepartment());
        rb.setToInstitution(bill.getToInstitution());

        rb.setDepartment(sessionController.getDepartment());
        rb.setInstitution(sessionController.getInstitution());

        Date now = new Date();
        rb.setBillDate(now);
        rb.setBillTime(now);
        rb.setCreatedAt(now);
        rb.setCreater(sessionController.getLoggedUser());
        rb.setComments(comment);
        rb.setPaymentMethod(getPaymentMethod());

        String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(), BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
        rb.setDeptId(billNumber);
        rb.setInsId(billNumber);

        // Totals computed from the selected items only.
        calculateRefundTotal();
        rb.setTotal(0 - sumGross());
        rb.setDiscount(0 - sumDiscount());
        rb.setNetTotal(0 - Math.abs(refundAmount));

        billFacade.create(rb);
        return rb;
    }

    // True when no original item on the bill remains un-returned. Refund items
    // (which carry a referanceBillItem) are excluded. Used by the page to
    // disable the Return button once everything has been returned, while still
    // allowing successive partial returns until then (issue #21247).
    public boolean isFullyReturned() {
        Bill bill = getBill();
        if (bill == null || bill.getId() == null) {
            return false;
        }
        String jpql = "SELECT COUNT(bi) FROM BillItem bi "
                + "WHERE bi.retired = false "
                + "AND bi.bill.id = :billId "
                + "AND bi.referanceBillItem IS NULL "
                + "AND bi.refunded = false";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", bill.getId());
        long remaining = billItemFacade.findLongByJpql(jpql, params);
        return remaining == 0L;
    }

    private double sumGross() {
        double t = 0.0;
        for (BillItem bi : refundingItems) {
            t += bi.getGrossValue();
        }
        return Math.abs(t);
    }

    private double sumDiscount() {
        double t = 0.0;
        for (BillItem bi : refundingItems) {
            t += bi.getDiscount();
        }
        return Math.abs(t);
    }

    private void refundSelectedItems(RefundBill rb) {
        for (BillItem selected : refundingItems) {
            BillItem original = billItemFacade.find(selected.getId());

            BillItem rbi = new BillItem();
            rbi.copy(original);
            rbi.invertValue(original);
            rbi.setBill(rb);
            rbi.setReferanceBillItem(original);
            rbi.setCreatedAt(new Date());
            rbi.setCreater(sessionController.getLoggedUser());
            billItemFacade.create(rbi);

            // Invert each original fee onto the refund item. referenceBillFee
            // links the inverted fee back to the original - this is what makes
            // the fee count as "refunded" on later passes (BillController.hasRefunded).
            for (BillFee obf : originalFeesOf(original)) {
                BillFee rbf = new BillFee();
                rbf.copy(obf);
                rbf.invertValue(obf);
                rbf.setBill(rb);
                rbf.setBillItem(rbi);
                rbf.setReferenceBillFee(obf);
                rbf.setSettleValue(0 - obf.getSettleValue());
                rbf.setCreatedAt(new Date());
                rbf.setCreater(sessionController.getLoggedUser());
                billFeeFacade.create(rbf);
            }

            // Flag the original item as returned.
            original.setRefunded(true);
            original.setBillItemRefunded(true);
            billItemFacade.edit(original);

            rb.getBillItems().add(rbi);
        }
    }

    private List<BillFee> originalFeesOf(BillItem original) {
        String jpql = "SELECT bf FROM BillFee bf "
                + "WHERE bf.retired = false AND bf.billItem.id = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", original.getId());
        return billFeeFacade.findByJpql(jpql, params);
    }

    // Only the original billed inward service / outside-charge bills can be
    // returned through this flow (not cancellations or already-refund bills).
    private boolean isSupportedForReturn(Bill bill) {
        BillTypeAtomic bta = bill.getBillTypeAtomic();
        return bta == BillTypeAtomic.INWARD_SERVICE_BILL
                || bta == BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL;
    }

    private boolean itemAlreadyReturned(BillItem original) {
        String jpql = "SELECT bi FROM BillItem bi "
                + "WHERE bi.retired = false "
                + "AND bi.referanceBillItem.id = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", original.getId());
        return billItemFacade.findFirstByJpql(jpql, params) != null;
    }

    /**
     * Lab-sampling guard for returning a laboratory investigation item
     * (issue #22987). Not every BillItem on an inward service bill is a lab
     * investigation (professional/other services have none), so a missing
     * PatientInvestigation simply means this guard doesn't apply.
     *
     * Reuses the exact same status whitelist and dataEntered check already
     * used by InwardSearch#checkCancelBill / #checkInvestigation for the
     * sibling "Cancel whole bill" flow, so the two flows agree on when a lab
     * item may be returned:
     *  - before the lab has received the sample (status still in
     *    EnumController#getAvailableStatusforCancel - Ordered / Barcode
     *    Generated / Sample Collected / Sample Sent / Sample Rejected): OK.
     *  - once the sample has been received/accepted and is being processed:
     *    blocked.
     *  - once a result has been entered (dataEntered): blocked, even if the
     *    status itself would otherwise allow it.
     *
     * @return a user-facing error message if the return should be blocked,
     * or null if it's allowed.
     */
    private String checkLabSampleGuard(BillItem original) {
        String jpql = "SELECT p FROM PatientInvestigation p WHERE p.retired = false AND p.billItem = :bi";
        Map<String, Object> params = new HashMap<>();
        params.put("bi", original);
        PatientInvestigation investigation = patientInvestigationFacade.findFirstByJpql(jpql, params);
        if (investigation == null) {
            return null;
        }
        if (Boolean.TRUE.equals(investigation.getDataEntered())) {
            return "Cannot return \"" + original.getItem().getName() + "\" - the laboratory report has already been entered for this test.";
        }
        if (!enumController.getAvailableStatusforCancel().contains(investigation.getStatus())) {
            return "Cannot return \"" + original.getItem().getName() + "\" - this test has already been received by the Laboratory.";
        }
        return null;
    }

    // Getters / Setters
    // The bill lives in the session-scoped InwardSearch so it survives the
    // faces-redirect into this view-scoped controller.
    public Bill getBill() {
        return inwardSearch.getBill();
    }

    public List<BillItem> getRefundingItems() {
        if (refundingItems == null) {
            refundingItems = new ArrayList<>();
        }
        return refundingItems;
    }

    public void setRefundingItems(List<BillItem> refundingItems) {
        this.refundingItems = refundingItems;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PaymentMethod getPaymentMethod() {
        if (paymentMethod == null && getBill() != null) {
            paymentMethod = getBill().getPaymentMethod();
        }
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }
}
