package com.divudi.bean.inward;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.CancelledBillFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.RefundBillFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.data.ProfessionalPaymentVoucherGroup;
import com.divudi.service.AuditService;
import com.divudi.service.DrawerService;
import com.divudi.service.ProfessionalPaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for handling surgery payments for inward surgeons
 *
 * @author Development Team
 */
@Named
@SessionScoped
public class InwardSurgeryPaymentBillController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private RefundBillFacade refundBillFacade;
    @EJB
    private CancelledBillFacade cancelledBillFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    ProfessionalPaymentService professionalPaymentService;
    @EJB
    DrawerService drawerService;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    StaffFacade staffFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;
    @EJB
    private AuditService auditService;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    DrawerController drawerController;
    @Inject
    private WebUserController webUserController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private static final long serialVersionUID = 1L;
    private Date fromDate;
    private Date toDate;

    private Bill current;
    private List<ProfessionalPaymentVoucherGroup> individualVoucherGroups;
    private Bill individualVoucherGroupsBill;
    private List<Bill> items = null;

    private Staff currentSurgeon;
    private List<BillFee> dueSurgeryFees;
    private List<BillFee> payingSurgeryFees;
    private boolean allowUserToSelectPayWithholdingTaxDuringSurgeryPayments;
    private String withholdingTaxCalculationStatus;
    private List<String> withholdingTaxCalculationStatuses;
    private double withholdingTax;
    private double totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute;
    private Double withholdingTaxLimit;
    private Double withholdingTaxPercentage;
    private double totalDue;
    private double totalOnHold;
    private double totalPaying;
    private double totalPayingWithoutWht;
    private boolean holdOverrideAcknowledged;
    private String holdOverrideReason;
    private List<BillFee> feesHeldAtSettle;

    private Boolean printPreview = false;
    private PaymentMethod paymentMethod;
    
    private SearchKeyword searchKeyword;
    private AdmissionType admissionType;
    private Institution institution;
    private boolean feeCollectedByDoctor = false; // Flag to mark fee as collected by surgeon
    private BillItem surgery; // Selected surgery for filtering

    private List<BillComponent> billComponents;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToInwardSurgeryPayment() {
        specialization = null;
        currentSurgeon = null;
        paymentMethod = null;
        dueSurgeryFees = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        feeCollectedByDoctor = false;
        return "/inward/inward_bill_surgery_payment?faces-redirect=true";
    }

    public String navigateToViewInwardSurgeryPayments() {
        recreateModel();
        fetchWithholdingDetailConfiguration();
        return "/inward/inward_bill_surgery_payment?faces-redirect=true";
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Main Methods">
    public void calculateDueFeesForSurgeriesForSelectedPeriod() {
        List<BillTypeAtomic> btcs = new ArrayList<>();
        btcs.add(BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);

        String sql;
        Map<String, Object> h = new HashMap<>();
        sql = "select bf from BillFee bf where "
                + " bf.retired=false "
                + " and bf.bill.billTypeAtomic in :btcs "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.createdAt between :fd and :td "
                + " and (bf.feeValue - bf.paidValue) > 0 "
                // Held fees are listed (flagged "On Hold" in the UI) rather than
                // hidden, so the payer can see the money exists — issue #22484.
                + " and bf.staff=:stf ";

        sql += " order by bf.createdAt desc";

        h.put("fd", fromDate);
        h.put("td", toDate);
        h.put("stf", currentSurgeon);
        h.put("btcs", btcs);
        dueSurgeryFees = getBillFeeFacade().findByJpql(sql, h, TemporalType.TIMESTAMP);
        
        List<BillFee> removeingBillFees = new ArrayList<>();
        for (BillFee bf : dueSurgeryFees) {
            h = new HashMap<>();
            h.put("btp", BillType.InwardBill);
            sql = "SELECT bi FROM BillItem bi where bi.retired=false "
                    + " and bi.bill.cancelled=false "
                    + " and bi.bill.billType=:btp "
                    + " and bi.referanceBillItem.id= " + bf.getBillItem().getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql, h);

            if (rbi != null) {
                removeingBillFees.add(bf);
            }
        }
        dueSurgeryFees.removeAll(removeingBillFees);

        // Default the UI-only "amount to pay now" to the full outstanding
        // balance for each due fee, so existing full-payment behaviour is
        // unchanged unless the cashier edits it down for a partial payment.
        for (BillFee sf : dueSurgeryFees) {
            sf.setPayingAmount(sf.getFeeValue() - sf.getPaidValue());
        }

        calculateTotalPaymentsForTheSurgeonForCurrentMonthForCurrentInstitution();
        performCalculations();
    }

    private void calculateTotalPaymentsForTheSurgeonForCurrentMonthForCurrentInstitution() {
        if (currentSurgeon == null) {
            return;
        }
        totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute
                = professionalPaymentService.findSumOfProfessionalPaymentsDone(sessionController.getInstitution(), currentSurgeon);
    }

    public void performCalculations() {
        calculateTotalDue();
        calculatePaymentsSelected();

        switch (getWithholdingTaxCalculationStatus()) {
            case "Depending On Payments":
                calculateWithholdingTaxDependingOnPayments();
                break;
            case "Include Withholding Tax":
                calculateWithWithholdingTax();
                break;
            case "Exclude Withholding Tax":
                calculateWithoutWithholdingTax();
                break;
            default:
                calculateWithholdingTaxDependingOnPayments();
        }
    }

    /**
     * Splits the listed dues into a payable total and an on-hold total, so the
     * payable figure is not inflated by fees that cannot be paid — issue #22483.
     */
    private void calculateTotalDue() {
        totalDue = 0.0;
        totalOnHold = 0.0;
        if (dueSurgeryFees == null) {
            return;
        }
        for (BillFee f : dueSurgeryFees) {
            double outstanding = f.getFeeValue() - f.getPaidValue();
            if (f.isProfessionalPaymentHeld()) {
                totalOnHold += outstanding;
            } else {
                totalDue += outstanding;
            }
        }
    }

    private void calculatePaymentsSelected() {
        totalPaying = 0;
        if (payingSurgeryFees == null) {
            return;
        }
        for (BillFee f : payingSurgeryFees) {
            Double payingAmount = f.getPayingAmount();
            totalPaying = totalPaying + (payingAmount != null ? payingAmount : (f.getFeeValue() - f.getPaidValue()));
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Professional payment hold (#22483, #22484)">
    /**
     * Called by the due-fee table's selection AJAX events. Changing the
     * selection invalidates any earlier override acknowledgement, so a held fee
     * cannot be slipped in after the confirmation box was ticked.
     */
    public void onSelectionChanged() {
        resetHoldOverride();
        performCalculations();
    }

    private void resetHoldOverride() {
        holdOverrideAcknowledged = false;
        holdOverrideReason = null;
        feesHeldAtSettle = null;
    }

    public List<BillFee> getHeldFeesSelected() {
        List<BillFee> held = new ArrayList<>();
        if (payingSurgeryFees == null) {
            return held;
        }
        for (BillFee bf : payingSurgeryFees) {
            if (bf.isProfessionalPaymentHeld()) {
                held.add(bf);
            }
        }
        return held;
    }

    public boolean isSelectionContainsHeldFees() {
        return !getHeldFeesSelected().isEmpty();
    }

    public boolean isCanOverrideHold() {
        return webUserController.hasPrivilege("InwardPayProfessionalFeesWhileOnHold");
    }

    public String getHeldFeesSelectedBhtNumbers() {
        List<String> bhtNos = new ArrayList<>();
        for (BillFee bf : getHeldFeesSelected()) {
            PatientEncounter pe = bf.getPatienEncounter();
            String bhtNo = pe != null && pe.getBhtNo() != null ? pe.getBhtNo() : "(no BHT)";
            if (!bhtNos.contains(bhtNo)) {
                bhtNos.add(bhtNo);
            }
        }
        return String.join(", ", bhtNos);
    }

    public double getHeldFeesSelectedValue() {
        double total = 0.0;
        for (BillFee bf : getHeldFeesSelected()) {
            total = total + (bf.getFeeValue() - bf.getPaidValue());
        }
        return total;
    }

    /**
     * Guards the selection against held fees. Returns an error message to show,
     * or null when the payment may proceed. A user without
     * {@code InwardPayProfessionalFeesWhileOnHold} is blocked outright; a user
     * who holds it must explicitly acknowledge the override (issue #22483).
     */
    private String checkHoldsOnSelection() {
        List<Long> heldNow = findCurrentlyHeldFeeIds(payingSurgeryFees);
        // Remember what this authoritative read found, so the settle path audits
        // exactly what the guard let through without re-querying.
        feesHeldAtSettle = new ArrayList<>();
        if (payingSurgeryFees != null) {
            for (BillFee bf : payingSurgeryFees) {
                if (bf.getId() != null && heldNow.contains(bf.getId())) {
                    feesHeldAtSettle.add(bf);
                }
            }
        }
        if (heldNow.isEmpty()) {
            return null;
        }
        String bhtNumbers = describeBhtsForFeeIds(heldNow);
        if (!isCanOverrideHold()) {
            return "Cannot pay: professional payments are on hold for " + bhtNumbers
                    + ". Release the hold before paying, or ask a user with the"
                    + " 'Pay Professional Fees While On Hold' privilege.";
        }
        // A hold applied after this page was loaded was never shown to the user,
        // so an acknowledgement given before it existed cannot cover it.
        for (BillFee bf : payingSurgeryFees) {
            if (bf.getId() != null && heldNow.contains(bf.getId()) && !bf.isProfessionalPaymentHeld()) {
                resetHoldOverride();
                return "Professional payments for " + bhtNumbers + " were put on hold while this page was open."
                        + " Run the search again to see the current hold status before settling.";
            }
        }
        if (!holdOverrideAcknowledged) {
            return "The selection includes professional payments on hold for " + bhtNumbers
                    + ". Tick the override confirmation and give a reason before settling.";
        }
        if (holdOverrideReason == null || holdOverrideReason.trim().isEmpty()) {
            return "Please give a reason for paying professional payments that are on hold.";
        }
        return null;
    }

    /**
     * Fee IDs in {@code selection} that are on hold <em>right now</em>, read
     * fresh from the database rather than from the in-memory selection.
     *
     * This bean is {@code @SessionScoped}, so the due-fee list may have been
     * loaded minutes ago and its hold flags can be stale — a hold applied by
     * another user in the meantime must still block the payment. A scalar
     * projection is used so the check reads columns rather than being served a
     * cached entity, and the encounter is joined with an explicit LEFT JOIN so
     * fees with no admission are not silently dropped from the fee-level
     * branch of the OR. (Issue #22483)
     */
    private List<Long> findCurrentlyHeldFeeIds(List<BillFee> selection) {
        List<Long> heldIds = new ArrayList<>();
        if (selection == null || selection.isEmpty()) {
            return heldIds;
        }
        List<Long> ids = new ArrayList<>();
        for (BillFee bf : selection) {
            if (bf != null && bf.getId() != null) {
                ids.add(bf.getId());
            }
        }
        if (ids.isEmpty()) {
            return heldIds;
        }
        String jpql = "select bf.id from BillFee bf "
                + " left join bf.patienEncounter pe "
                + " where bf.id in :ids "
                + " and (bf.feePaymentOnHold = true or pe.professionalPaymentsOnHold = true) ";
        Map<String, Object> params = new HashMap<>();
        params.put("ids", ids);
        for (Object o : getBillFeeFacade().findObjects(jpql, params)) {
            if (o instanceof Number) {
                heldIds.add(((Number) o).longValue());
            }
        }
        return heldIds;
    }

    /**
     * BHT numbers for the given fee IDs, taken from the loaded selection. The
     * BHT number itself never changes, so the in-memory copy is safe here even
     * when the hold flags on it are stale.
     */
    private String describeBhtsForFeeIds(List<Long> feeIds) {
        List<String> bhtNos = new ArrayList<>();
        if (payingSurgeryFees == null) {
            return "";
        }
        for (BillFee bf : payingSurgeryFees) {
            if (bf.getId() == null || !feeIds.contains(bf.getId())) {
                continue;
            }
            PatientEncounter pe = bf.getPatienEncounter();
            String bhtNo = pe != null && pe.getBhtNo() != null ? pe.getBhtNo() : "(no BHT)";
            if (!bhtNos.contains(bhtNo)) {
                bhtNos.add(bhtNo);
            }
        }
        return String.join(", ", bhtNos);
    }

    /**
     * Records the override on the payment bill and in the admission's audit
     * trail, so a payment made past a hold is traceable afterwards.
     */
    private void recordHoldOverride(Bill paymentBill, List<BillFee> held) {
        if (held == null || held.isEmpty()) {
            return;
        }
        List<String> overriddenBhtNos = new ArrayList<>();
        for (BillFee bf : held) {
            PatientEncounter bhtPe = bf.getPatienEncounter();
            String bhtNo = bhtPe != null && bhtPe.getBhtNo() != null ? bhtPe.getBhtNo() : "(no BHT)";
            if (!overriddenBhtNos.contains(bhtNo)) {
                overriddenBhtNos.add(bhtNo);
            }
        }
        String reason = holdOverrideReason == null ? "" : holdOverrideReason.trim();
        String note = "Paid while on hold (" + String.join(", ", overriddenBhtNos) + ") by "
                + sessionController.getLoggedUser().getName() + ". Reason: " + reason;
        if (paymentBill != null) {
            String existing = paymentBill.getComments();
            paymentBill.setComments(existing == null || existing.trim().isEmpty()
                    ? note : existing + " | " + note);
        }
        for (BillFee bf : held) {
            PatientEncounter pe = bf.getPatienEncounter();
            if (pe == null) {
                continue;
            }
            Map<String, Object> before = new LinkedHashMap<>();
            before.put("billFeeId", bf.getId());
            before.put("feePaymentOnHold", bf.isFeePaymentOnHold());
            before.put("bhtProfessionalPaymentsOnHold", pe.isProfessionalPaymentsOnHold());
            Map<String, Object> after = new LinkedHashMap<>(before);
            after.put("paidWhileOnHold", Boolean.TRUE);
            after.put("overrideReason", reason);
            after.put("paymentBillId", paymentBill != null ? paymentBill.getId() : null);
            auditService.logEncounterAudit(pe, "Surgery Professional Fee Paid While On Hold",
                    before, after, sessionController.getLoggedUser(), "BillFee", bf.getId());
        }
    }

    public boolean isHoldOverrideAcknowledged() {
        return holdOverrideAcknowledged;
    }

    public void setHoldOverrideAcknowledged(boolean holdOverrideAcknowledged) {
        this.holdOverrideAcknowledged = holdOverrideAcknowledged;
    }

    public String getHoldOverrideReason() {
        return holdOverrideReason;
    }

    public void setHoldOverrideReason(String holdOverrideReason) {
        this.holdOverrideReason = holdOverrideReason;
    }

    public double getTotalOnHold() {
        return totalOnHold;
    }

    public void setTotalOnHold(double totalOnHold) {
        this.totalOnHold = totalOnHold;
    }
    // </editor-fold>

    private void calculateWithholdingTaxDependingOnPayments() {
        if (totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute == 0.0) {
            withholdingTax = 0.0;
            totalPayingWithoutWht = totalPaying;
            return;
        }
        Double paidValue = Math.abs(totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute);
        if (getWithholdingTaxLimit() < paidValue) {
            withholdingTax = totalPaying * (getWithholdingTaxPercentage() / 100);
        } else {
            withholdingTax = 0.0;
        }
        totalPayingWithoutWht = totalPaying - withholdingTax;
    }

    private void calculateWithWithholdingTax() {
        withholdingTax = totalPaying * (getWithholdingTaxPercentage() / 100);
        totalPayingWithoutWht = totalPaying - withholdingTax;
    }

    private void calculateWithoutWithholdingTax() {
        withholdingTax = 0.0;
        totalPayingWithoutWht = totalPaying - withholdingTax;
    }

    public void settle() {
        System.out.println("totalsettle = " + totalPaying);
        System.out.println("this = " + getPayingSurgeryFees().size());
        
        if (errorCheck()) {
            return;
        }
        
        if (paymentMethod == PaymentMethod.Cash && !feeCollectedByDoctor) {
            Drawer userDrawer = drawerService.getUsersDrawer(sessionController.getLoggedUser());
            if (userDrawer != null) {
                double drawerBalance = userDrawer.getCashInHandValue() != null ? userDrawer.getCashInHandValue() : 0.0;
                double paymentAmount = getTotalPayingWithoutWht();

                boolean allowNegativeDrawer = configOptionApplicationController.getBooleanValueByKey(
                        "Inward Professional Payments - Allow Negative Drawer Balance", false);
                if (configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true) && !allowNegativeDrawer) {
                    if (drawerBalance < paymentAmount) {
                        JsfUtil.addErrorMessage("Not enough cash in your drawer to make this payment");
                        return;
                    }
                }
            }
        }
        
        performCalculations();
        Bill newlyCreatedPaymentBill = createPaymentBill();
        current = newlyCreatedPaymentBill;
        getBillFacade().create(newlyCreatedPaymentBill);
        Payment newlyCreatedPayment = createPayment(newlyCreatedPaymentBill, paymentMethod);
        
        if (!feeCollectedByDoctor) {
            drawerController.updateDrawerForOuts(newlyCreatedPayment);
        }
        
        saveBillCompo(newlyCreatedPaymentBill, newlyCreatedPayment);
        List<BillFee> heldPaid = feesHeldAtSettle;
        boolean paidPastHold = heldPaid != null && !heldPaid.isEmpty();
        if (paidPastHold) {
            recordHoldOverride(newlyCreatedPaymentBill, heldPaid);
            getBillFacade().edit(newlyCreatedPaymentBill);
        }
        printPreview = true;
        if (paidPastHold) {
            JsfUtil.addSuccessMessage("Surgery Payment Successfully Processed. Payments on hold were paid using your"
                    + " override privilege — this has been recorded on the payment bill and in the admission's audit trail.");
        } else {
            JsfUtil.addSuccessMessage("Surgery Payment Successfully Processed");
        }
        resetHoldOverride();
    }

    public void settleWithoutPayment() {
        System.out.println("totalsettle (without payment) = " + totalPaying);
        System.out.println("this = " + getPayingSurgeryFees().size());
        
        if (errorCheck()) {
            return;
        }
        
        performCalculations();

        // Record the override before the fees are marked paid, so the hold is
        // captured even if a later write fails part-way (#22483).
        List<BillFee> heldPaid = feesHeldAtSettle;
        boolean paidPastHold = heldPaid != null && !heldPaid.isEmpty();
        if (paidPastHold) {
            recordHoldOverride(null, heldPaid);
        }

        // Update bill fees without creating payment records
        for (BillFee originalBillFee : getPayingSurgeryFees()) {
            double outstanding = originalBillFee.getFeeValue() - originalBillFee.getPaidValue();
            Double payingAmount = originalBillFee.getPayingAmount();
            double amountPaidNow = payingAmount != null ? payingAmount : outstanding;

            originalBillFee.setPaidValue(originalBillFee.getPaidValue() + amountPaidNow);
            originalBillFee.setSettleValue(originalBillFee.getSettleValue() + amountPaidNow);

            // Mark as collected by doctor if flag is set
            if (feeCollectedByDoctor) {
                originalBillFee.setFeeCollectedByDoctor(true);
            }

            getBillFeeFacade().edit(originalBillFee);
        }

        printPreview = true;
        if (paidPastHold) {
            JsfUtil.addSuccessMessage("Surgery Fees Successfully Settled (No Payment Record Created). Payments on hold"
                    + " were settled using your override privilege — this has been recorded in the admission's audit trail.");
        } else {
            JsfUtil.addSuccessMessage("Surgery Fees Successfully Settled (No Payment Record Created)");
        }
        resetHoldOverride();
    }

    private boolean errorCheck() {
        String holdError = checkHoldsOnSelection();
        if (holdError != null) {
            JsfUtil.addErrorMessage(holdError);
            return true;
        }
        if (currentSurgeon == null) {
            JsfUtil.addErrorMessage("Please select a Surgeon");
            return true;
        }
        if (dueSurgeryFees == null) {
            JsfUtil.addErrorMessage("Please select surgeries to pay");
            return true;
        }
        if (getPayingSurgeryFees() != null) {
            for (BillFee f : getPayingSurgeryFees()) {
                double outstanding = f.getFeeValue() - f.getPaidValue();
                Double payingAmount = f.getPayingAmount();
                if (payingAmount == null || payingAmount <= 0) {
                    JsfUtil.addErrorMessage("Please enter a valid paying amount for all selected surgery fees");
                    return true;
                }
                if (payingAmount - outstanding > 0.1) {
                    JsfUtil.addErrorMessage("Paying amount cannot exceed the outstanding due amount for a surgery fee");
                    return true;
                }
            }
        }
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Please select surgeries to pay");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    private Bill createPaymentBill() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.PaymentBill);
        tmp.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getLoggedUser().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE));
        tmp.setInsId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE));

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getLoggedUser().getInstitution());
        tmp.setNetTotal(0 - totalPayingWithoutWht);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setStaff(currentSurgeon);
        tmp.setToStaff(currentSurgeon);
        tmp.setTotal(0 - totalPaying);
        tmp.setTax(withholdingTax);

        return tmp;
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
    }

    private void saveBillCompo(Bill paymentBill, Payment paymentBillPayment) {
        for (BillFee originalBillFee : getPayingSurgeryFees()) {
            double outstanding = originalBillFee.getFeeValue() - originalBillFee.getPaidValue();
            Double payingAmount = originalBillFee.getPayingAmount();
            double amountPaidNow = payingAmount != null ? payingAmount : outstanding;

            saveBillItemForPaymentBill(paymentBill, originalBillFee, paymentBillPayment, amountPaidNow);
            originalBillFee.setPaidValue(originalBillFee.getPaidValue() + amountPaidNow);
            originalBillFee.setSettleValue(originalBillFee.getSettleValue() + amountPaidNow);

            // Mark as collected by doctor if flag is set
            if (feeCollectedByDoctor) {
                originalBillFee.setFeeCollectedByDoctor(true);
            }

            getBillFeeFacade().edit(originalBillFee);
        }
    }

    private void saveBillItemForPaymentBill(Bill newPaymentBill, BillFee originalBillFee, Payment p, double amountPaidNow) {
        BillItem newlyCreatedPayingBillItem = new BillItem();
        newlyCreatedPayingBillItem.setReferanceBillItem(originalBillFee.getBillItem());
        newlyCreatedPayingBillItem.setReferenceBill(originalBillFee.getBill());
        newlyCreatedPayingBillItem.setPaidForBillFee(originalBillFee);
        newlyCreatedPayingBillItem.setBill(newPaymentBill);
        newlyCreatedPayingBillItem.setCreatedAt(Calendar.getInstance().getTime());
        newlyCreatedPayingBillItem.setCreater(getSessionController().getLoggedUser());
        newlyCreatedPayingBillItem.setDiscount(0.0);
        newlyCreatedPayingBillItem.setGrossValue(amountPaidNow);
        newlyCreatedPayingBillItem.setNetValue(amountPaidNow);
        newlyCreatedPayingBillItem.setQty(1.0);
        newlyCreatedPayingBillItem.setRate(amountPaidNow);
        getBillItemFacade().create(newlyCreatedPayingBillItem);

        BillFee newlyCreatedBillFee = saveBillFee(newlyCreatedPayingBillItem, p);

        originalBillFee.setReferenceBillFee(newlyCreatedBillFee);
        getBillFeeFacade().edit(originalBillFee);

        newPaymentBill.getBillItems().add(newlyCreatedPayingBillItem);
    }

    public BillFee saveBillFee(BillItem bi, Payment p) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setReferenceBillFee(bi.getPaidForBillFee());
        bf.setReferenceBillItem(bi.getReferanceBillItem());
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(0 - bi.getNetValue());
        bf.setFeeGrossValue(0 - bi.getGrossValue());
        bf.setSettleValue(0 - bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
        createBillFeePaymentAndPayment(bf, p);
        return bf;
    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        // BillFeePayment is deprecated and no longer used
    }

    private void fetchWithholdingDetailConfiguration() {
        allowUserToSelectPayWithholdingTaxDuringSurgeryPayments
                = configOptionApplicationController.getBooleanValueByKey(
                        "Allow User To Select Whether To Pay Withholding Tax During Professional Payments", true);

        withholdingTaxCalculationStatuses = new ArrayList<>();
        withholdingTaxCalculationStatuses.add("Depending On Payments");
        withholdingTaxCalculationStatuses.add("Include Withholding Tax");
        withholdingTaxCalculationStatuses.add("Exclude Withholding Tax");

        if (configOptionApplicationController.getBooleanValueByKey(
                "Withholding Tax Calculated Depending On This Month's Payments During Professional Payments", false)) {
            withholdingTaxCalculationStatus = "Depending On Payments";
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Withholding Tax Is Always Calculated During Professional Payments", true)) {
            withholdingTaxCalculationStatus = "Include Withholding Tax";
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Withholding Tax Is Never Calculated During Professional Payments", false)) {
            withholdingTaxCalculationStatus = "Exclude Withholding Tax";
        } else {
            withholdingTaxCalculationStatus = "Depending On Payments";
        }
    }

    public void recreateModel() {
        printPreview = false;
        items = null;
        dueSurgeryFees = null;
        payingSurgeryFees = null;
        fromDate = null;
        toDate = null;
        current = null;
        currentSurgeon = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        paymentMethod = null;
        feeCollectedByDoctor = false;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Autocomplete Methods">
    public List<Staff> completeSurgeon(String query) {
        List<Staff> suggestions;
        if (query == null || query.trim().isEmpty()) {
            suggestions = new ArrayList<>();
        } else {
            Map<String, Object> params = new HashMap<>();
            String jpql = "SELECT p FROM Staff p WHERE p.retired = false "
                    + "AND (UPPER(p.person.name) LIKE :query OR UPPER(p.code) LIKE :query) "
                    + "ORDER BY p.person.name";
            params.put("query", "%" + query.toUpperCase() + "%");
            suggestions = staffFacade.findByJpql(jpql, params);
        }
        return suggestions;
    }

    public List<BillItem> completeSurgery(String query) {
        List<BillItem> suggestions = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return suggestions;
        }
        
        try {
            Map<String, Object> params = new HashMap<>();
            String jpql = "SELECT DISTINCT bi FROM BillItem bi "
                    + "JOIN bi.bill b "
                    + "WHERE bi.retired = false "
                    + "AND b.billTypeAtomic = :billTypeAtomic "
                    + "AND b.cancelled = false "
                    + "AND UPPER(bi.item.name) LIKE :query ";
            
            params.put("billTypeAtomic", BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
            params.put("query", "%" + query.toUpperCase() + "%");
            
            // If surgeon is selected, filter surgeries by that surgeon's bills
            if (currentSurgeon != null) {
                jpql += "AND EXISTS (SELECT 1 FROM BillFee bf WHERE bf.billItem.id = bi.id AND bf.staff.id = :surgeonId) ";
                params.put("surgeonId", currentSurgeon.getId());
            }
            
            jpql += "ORDER BY bi.item.name";
            
            suggestions = getBillItemFacade().findByJpql(jpql, params);
        } catch (Exception e) {
            System.out.println("Error in completeSurgery: " + e.getMessage());
            e.printStackTrace();
        }
        
        return suggestions;
    }

    public boolean hasFeeCollectedByDoctor() {
        if (current == null) {
            return false;
        }
        
        List<BillFee> billFees = getBillFees();
        for (BillFee bf : billFees) {
            if (bf.isFeeCollectedByDoctor()) {
                return true;
            }
        }
        return false;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public List<BillComponent> getBillComponents() {
        if (getCurrent() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
            billComponents = getBillComponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    private List<BillFee> billFees;

    public List<BillFee> getBillFees() {
        if (getCurrent() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
                if (billFees == null) {
                    billFees = new ArrayList<>();
                }
            }
        }
        return billFees;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Bill getCurrent() {
        return current;
    }

    public List<ProfessionalPaymentVoucherGroup> getIndividualVoucherGroups() {
        if (individualVoucherGroups == null || individualVoucherGroupsBill != current) {
            individualVoucherGroups = professionalPaymentService
                    .groupPaymentBillItemsByPatientOrBht(current);
            individualVoucherGroupsBill = current;
        }
        return individualVoucherGroups;
    }

    public void setCurrent(Bill current) {
        this.current = current;
        this.billFees = null;  // Clear cache when current bill changes
    }

    public List<Bill> getItems() {
        return items;
    }

    public void setItems(List<Bill> items) {
        this.items = items;
    }

    public Staff getCurrentSurgeon() {
        return currentSurgeon;
    }

    public void setCurrentSurgeon(Staff currentSurgeon) {
        this.currentSurgeon = currentSurgeon;
    }

    public List<BillFee> getDueSurgeryFees() {
        return dueSurgeryFees;
    }

    public void setDueSurgeryFees(List<BillFee> dueSurgeryFees) {
        this.dueSurgeryFees = dueSurgeryFees;
    }

    public List<BillFee> getPayingSurgeryFees() {
        return payingSurgeryFees;
    }

    public void setPayingSurgeryFees(List<BillFee> payingSurgeryFees) {
        this.payingSurgeryFees = payingSurgeryFees;
    }

    public boolean isAllowUserToSelectPayWithholdingTaxDuringSurgeryPayments() {
        return allowUserToSelectPayWithholdingTaxDuringSurgeryPayments;
    }

    public void setAllowUserToSelectPayWithholdingTaxDuringSurgeryPayments(boolean allowUserToSelectPayWithholdingTaxDuringSurgeryPayments) {
        this.allowUserToSelectPayWithholdingTaxDuringSurgeryPayments = allowUserToSelectPayWithholdingTaxDuringSurgeryPayments;
    }

    public String getWithholdingTaxCalculationStatus() {
        if (withholdingTaxCalculationStatus == null) {
            withholdingTaxCalculationStatus = "Depending On Payments";
        }
        return withholdingTaxCalculationStatus;
    }

    public void setWithholdingTaxCalculationStatus(String withholdingTaxCalculationStatus) {
        this.withholdingTaxCalculationStatus = withholdingTaxCalculationStatus;
    }

    public List<String> getWithholdingTaxCalculationStatuses() {
        return withholdingTaxCalculationStatuses;
    }

    public void setWithholdingTaxCalculationStatuses(List<String> withholdingTaxCalculationStatuses) {
        this.withholdingTaxCalculationStatuses = withholdingTaxCalculationStatuses;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public void setWithholdingTax(double withholdingTax) {
        this.withholdingTax = withholdingTax;
    }

    public double getTotalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute() {
        return totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute;
    }

    public void setTotalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute(double totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute) {
        this.totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute = totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute;
    }

    public Double getWithholdingTaxLimit() {
        if (withholdingTaxLimit == null) {
            withholdingTaxLimit = configOptionApplicationController.getDoubleValueByKey("Withholding Tax Limit", 0.0);
        }
        return withholdingTaxLimit;
    }

    public void setWithholdingTaxLimit(Double withholdingTaxLimit) {
        this.withholdingTaxLimit = withholdingTaxLimit;
    }

    public Double getWithholdingTaxPercentage() {
        if (withholdingTaxPercentage == null) {
            withholdingTaxPercentage = configOptionApplicationController.getDoubleValueByKey("Withholding Tax Percentage", 0.0);
        }
        return withholdingTaxPercentage;
    }

    public void setWithholdingTaxPercentage(Double withholdingTaxPercentage) {
        this.withholdingTaxPercentage = withholdingTaxPercentage;
    }

    public double getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(double totalDue) {
        this.totalDue = totalDue;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public double getTotalPayingWithoutWht() {
        return totalPayingWithoutWht;
    }

    public void setTotalPayingWithoutWht(double totalPayingWithoutWht) {
        this.totalPayingWithoutWht = totalPayingWithoutWht;
    }

    public Boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(Boolean printPreview) {
        this.printPreview = printPreview;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public boolean isFeeCollectedByDoctor() {
        return feeCollectedByDoctor;
    }

    public void setFeeCollectedByDoctor(boolean feeCollectedByDoctor) {
        this.feeCollectedByDoctor = feeCollectedByDoctor;
    }

    public BillItem getSurgery() {
        return surgery;
    }

    public void setSurgery(BillItem surgery) {
        this.surgery = surgery;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    private String specialization; // For future specialization filtering

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    // </editor-fold>
}
