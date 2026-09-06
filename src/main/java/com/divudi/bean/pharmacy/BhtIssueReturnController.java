/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.inward.InwardBeanController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.data.dto.pharmacy.BhtIssueReturnItemStatusDto;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.service.BillService;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class BhtIssueReturnController implements Serializable {

    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
    private List<BillItem> billItems;
    private String returnComment = "";
    private double discountTotal = 0.0;
    private List<BhtIssueReturnItemStatusDto> runningStatusRows;

    
    ///////
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillService billService;
    @EJB
    private BillFeeFacade billFeeFacade;
    
    ////////
    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    InwardBeanController inwardBean;
    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private PharmacyCalculation pharmacyRecieveBean;
    
    
    public String navigateToReturnPharmacyDirectIssueToInpatients(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No Bill provided");
            return null;
        }
        bill = b;
        return navigateToReturnPharmacyDirectIssueToInpatients();
    }

    public String navigateToReturnPharmacyDirectIssueToInpatients() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        printPreview = false;
        billItems = null;
        runningStatusRows = null;
        returnComment = "";

        if (bill.getDepartment() == null) {
            JsfUtil.addErrorMessage("No Department for the Bill");
            return null;
        }

//        if (getSessionController().getDepartment().getId() != bill.getDepartment().getId()) {
//            JsfUtil.addErrorMessage("U can't return another department's Issue.please log to specific department");
//            return;
//        }
        if (!configOptionApplicationController.getBooleanValueByKey("Inward Pharmacy Request - Enable Receiving Department to Return the Drugs", false)) {
            if (!getSessionController().getDepartment().getId().equals(bill.getDepartment().getId())) {
                JsfUtil.addErrorMessage("You can't return another department's Issue.please log to specific department");
                return null;
            }
        } else {
            if (!getSessionController().getDepartment().getId().equals(bill.getDepartment().getId()) && !getSessionController().getDepartment().getId().equals(bill.getFromDepartment().getId())) {
                JsfUtil.addErrorMessage("You can't return another department's Issue.please log to specific department");
                return null;
            }
        }
        returnBill = null;
        getReturnBill();
        returnBill.copy(bill);
        generateBillComponent();
        return "/inward/pharmacy_bill_return_bht_issue?faces-redirect=true";
    }

    public Bill getBill() {
        return bill;
    }

//    public void setBill(Bill bill) {
//        makeNull();
//
//        if (bill.getDepartment() == null) {
//            return;
//        }
//
////        if (getSessionController().getDepartment().getId() != bill.getDepartment().getId()) {
////            JsfUtil.addErrorMessage("U can't return another department's Issue.please log to specific department");
////            return;
////        }
//        if (!getSessionController().getDepartment().getId().equals(bill.getDepartment().getId())) {
//            JsfUtil.addErrorMessage("U can't return another department's Issue.please log to specific department");
//            return;
//        }
//
//        this.bill = bill;
//        returnBill = null;
//        returnBill.copy(bill);
//        generateBillComponent();
//    }
    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new RefundBill();
            //    returnBill.setBillType(BillType.PharmacyBhtPre);

        }

        return returnBill;
    }

    public void setReturnBill(Bill returnBill) {
        this.returnBill = returnBill;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public void onEdit(BillItem tmp) {
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();

        if (tmp.getQty() > getPharmacyRecieveBean().calQty4(tmp.getReferanceBillItem())) {
            tmp.setQty(0.0);
            calTotal();
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
        } else {
            calTotal();
        }

        //   getPharmacyController().setPharmacyItem(tmp.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    public void makeNull() {
        bill = null;
        returnBill = null;
        printPreview = false;
        billItems = null;
        runningStatusRows = null;
        returnComment = "";
    }

    private void saveReturnBill() {

//        getReturnBill().copy(getBill());
        getReturnBill().setBillType(getBill().getBillType());
        // Discharge medicine issues return to the discharge return atomic; all other inward direct issues to the regular one.
        BillTypeAtomic returnAtomic = getBill().getBillTypeAtomic() == BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE
                ? BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN
                : BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN;
        getReturnBill().setBillTypeAtomic(returnAtomic);
        getReturnBill().setBilledBill(getBill());
        getReturnBill().setComments(returnComment);
        getReturnBill().setForwardReferenceBill(getBill().getForwardReferenceBill());
        // copy() is deliberately not used here (it also overwrites department/institution,
        // set explicitly below to the returning department), but patientEncounter/patient
        // still need to be carried over - without them this return bill is invisible to every
        // patientEncounter-scoped query (Interim Bill Medicine total, Medicine Issue tab), so
        // the returned value silently never gets deducted (issue #22990).
        getReturnBill().setPatientEncounter(getBill().getPatientEncounter());
        getReturnBill().setPatient(getBill().getPatient());

        getReturnBill().setTotal(0 - Math.abs(getReturnBill().getTotal()));
        getReturnBill().setNetTotal(0 - Math.abs(getReturnBill().getNetTotal()));
        getReturnBill().setMargin(0 - Math.abs(getReturnBill().getMargin()));

        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        getReturnBill().setDepartment(getSessionController().getDepartment());
        getReturnBill().setInstitution(getSessionController().getInstitution());

        String departmentId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), returnAtomic);
        getReturnBill().setInsId(departmentId);
        getReturnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.PHISSRET));

        //   getReturnBill().setInsId(getBill().getInsId());
        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    private void saveReturnIssueBill() {

//        getReturnBill().copy(getBill());
        getReturnBill().setBillType(getBill().getBillType());
        getReturnBill().setBillTypeAtomic(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
        getReturnBill().setBilledBill(getBill());
        // See saveReturnBill() above - patientEncounter/patient must be carried over
        // explicitly since copy() is not used here (issue #22990).
        getReturnBill().setPatientEncounter(getBill().getPatientEncounter());
        getReturnBill().setPatient(getBill().getPatient());

        getReturnBill().setForwardReferenceBill(getBill().getForwardReferenceBill());
        getReturnBill().setComments(returnComment);
        getReturnBill().setTotal(0 - Math.abs(getReturnBill().getTotal()));
        getReturnBill().setNetTotal(0 - Math.abs(getReturnBill().getNetTotal()));
        getReturnBill().setMargin(0 - Math.abs(getReturnBill().getMargin()));

        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        getReturnBill().setDepartment(getSessionController().getDepartment());
        getReturnBill().setInstitution(getSessionController().getInstitution());

        String departmentId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
        getReturnBill().setInsId(departmentId);
        getReturnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.PHISSRET));

        //   getReturnBill().setInsId(getBill().getInsId());
        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    private void saveComponent() {
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQtyInUnit(i.getQty());

            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            i.setBill(getReturnBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setQty(i.getPharmaceuticalBillItem().getQty());

//            double value = i.getRate() * i.getQty();
//            i.setGrossValue(0 - value);
//            i.setNetValue(0 - value);
            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            //System.err.println("STOCK " + i.getPharmaceuticalBillItem().getStock());
            if (!configOptionApplicationController.getBooleanValueByKey("Inward Pharmacy Request - Enable Receiving Department to Return the Drugs", false)) {
                getPharmacyBean().addToStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());
            } else {
                getPharmacyBean().addToStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getBill().getDepartment());
            }

            //   i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem().setRemainingQty(i.getRemainingQty() - i.getQty());
            //   getPharmaceuticalBillItemFacade().edit(i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem());
            //      updateRemainingQty(i);
            getReturnBill().getBillItems().add(i);
        }

    }

    /**
     * The room category of the patient's current room, or null when the patient
     * is not in a room (or the room has no facility charge / category). Drives the
     * room-category dimension of the inward pharmacy-margin matrix (issue #21981);
     * null means "wildcard row only", preserving legacy behaviour.
     */
    private RoomCategory resolveCurrentRoomCategory(PatientEncounter encounter) {
        if (encounter == null
                || encounter.getCurrentPatientRoom() == null
                || encounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            return null;
        }
        return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getRoomCategory();
    }

    public void updateMargin(BillItem bi, Department matrixDepartment, PaymentMethod paymentMethod) {
        double rate = Math.abs(bi.getRate());
        double margin = 0;

        PatientEncounter encounter = bi.getBill() != null ? bi.getBill().getPatientEncounter() : null;
        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod, null,
                encounter != null ? encounter.getAdmissionType() : null, resolveCurrentRoomCategory(encounter));

        if (priceMatrix != null) {
            margin = ((bi.getGrossValue() * priceMatrix.getMargin()) / 100);
        }

        bi.setMarginValue(margin);

        bi.setNetValue((bi.getGrossValue() + bi.getMarginValue()) - bi.getDiscount());
//        bi.setNetValue((bi.getGrossValue() + bi.getMarginValue()));
        bi.setAdjustedValue((bi.getGrossValue() + bi.getMarginValue()));
        getBillItemFacade().edit(bi);
    }

    public void updateMargin(List<BillItem> billItems, Bill bill, Department matrixDepartment, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
        for (BillItem bi : billItems) {

            updateMargin(bi, matrixDepartment, paymentMethod);
            total += bi.getGrossValue();
            netTotal += bi.getNetValue();
        }

        bill.setTotal(total);
        bill.setMargin(netTotal - total);
        bill.setNetTotal(netTotal);
        getBillFacade().edit(bill);

    }

    public void settle() {

        // Re-entrancy guard: a double-click or resubmission on the session-scoped
        // controller must not create a second return bill / duplicate stock
        // adjustments for the same returnBill instance.
        if (getReturnBill().getId() != null) {
            JsfUtil.addErrorMessage("This return has already been settled.");
            return;
        }

        if (returnComment == null || returnComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Return comment is Mandatory..");
            return;
        }

        // Recompute from the just-submitted quantities. calTotal() was previously only
        // triggered by the per-row blur AJAX (onEdit()); relying on that cached total here
        // raced the full-page Return postback and could see a stale/zero total even when
        // the submitted qty values were valid (issue #21883).
        calTotal();

        if (getBill().getBillItems() != null) {
//            System.out.println("this = " + getBill().getBillItems().size() );

            for (BillItem bi : billItems) {

//                System.out.println("bi = " + bi);
//                System.out.println("bi.getPharmaceuticalBillItem().getQtyInUnit() = " + bi.getPharmaceuticalBillItem().getQtyInUnit());
//                System.out.println("bi.getQty() = " + bi.getQty());
//                System.out.println("bi.getPharmaceuticalBillItem().getQty() = " + bi.getPharmaceuticalBillItem().getQty());
                double returnedQty = getPharmacyRecieveBean().getTotalQty(
                        bi.getReferanceBillItem(),
                        getBill().getBillType()
                );
                double liveAvailableQty = Math.abs(
                        bi.getReferanceBillItem().getPharmaceuticalBillItem().getQtyInUnit()
                ) - Math.abs(returnedQty);
                if (bi.getQty() > liveAvailableQty) {
//                    System.out.println("bi.getQty = " + bi.getQty());
                    JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
                    return;
                }
            }
        }

// Validate against returned quantity, not gross value - a fully-margin item
        // (Gross Rate 0.00, e.g. a service-charge-only line) has a legitimately zero
        // returnBill.getTotal() even when a valid quantity was entered (issue #21883).
        double totalReturnQty = 0.0;
        for (BillItem bi : billItems) {
            totalReturnQty += Math.abs(bi.getQty());
        }
        if (totalReturnQty == 0) {
            JsfUtil.addErrorMessage("Add Valied Return Quntity");
            return;
        }

//        if (getBill().getCheckedBy() != null) {
//            JsfUtil.addErrorMessage("Checked Bill. Can not Return");
//            return;
//        }
        if (getBill().getPatientEncounter().isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot return medicines: nursing discharge has already been confirmed for this patient.");
            return;
        }
        if (getBill().getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry, patient is discharged.");
            return;
        }
        if (getBill().getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("This Bill Already Discharged");
            return;
        }

        if (getBill().getBillTypeAtomic() == BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD) {
            saveReturnIssueBill();
        } else {
            saveReturnBill();
        }
        saveComponent();
        billService.createBillFinancialDetailsForInpatientDirectIssueReturnBill(getReturnBill());

        getReturnBill().setReferenceBill(getBill());
//        updateMargin(getReturnBill().getBillItems(), getReturnBill(), getReturnBill().getFromDepartment(), getBill().getPatientEncounter().getPaymentMethod());
        getBillFacade().edit(getReturnBill());

        getBill().getReturnBhtIssueBills().add(getReturnBill());
        getBillFacade().edit(getBill());

        /// setOnlyReturnValue();
        buildRunningStatusRows();
        printPreview = true;
        returnComment = "";
        JsfUtil.addSuccessMessage("Successfully Returned");

    }

    /**
     * Builds the per-item quantity reconciliation rows shown on the "Running
     * Update Status" tab after a return is settled (issue #23338).
     * <p>
     * Iterates the ORIGINAL bill's pharmaceutical items rather than
     * {@link #billItems}: {@link #generateBillComponent()} skips lines whose
     * balance already reached zero (the {@code if (tmpQty <= 0) { continue; }}
     * at ~line 549), so a table built from {@code billItems} would silently
     * omit fully-returned items - exactly the lines a reconciliation view
     * needs to show (with Balance 0).
     * <p>
     * Must run after persistence: {@link PharmacyCalculation#getTotalQty}
     * is an aggregate over saved return bill items, so it only includes this
     * settlement once {@link #saveComponent()} has committed.
     * <p>
     * {@code getTotalQty(BillItem, BillType)} is deliberately the same call
     * {@link #generateBillComponent()} (~line 546) and {@link #settle()}'s
     * validation loop (~line 411) already use, so this table cannot disagree
     * with the "Balance Qty in Unit" the pre-settle grid shows.
     */
    private void buildRunningStatusRows() {
        runningStatusRows = new ArrayList<>();
        if (bill == null) {
            return;
        }
        for (PharmaceuticalBillItem originalPbi : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getBill())) {
            BillItem originalBillItem = originalPbi.getBillItem();
            if (originalBillItem == null) {
                continue;
            }

            double saleQty = Math.abs(originalPbi.getQty());

            // Total returned across ALL returns of this line, including the one just settled.
            double totalReturnedQty = Math.abs(
                    getPharmacyRecieveBean().getTotalQty(originalBillItem, getBill().getBillType()));

            // The slice of that total contributed by this settlement.
            double thisTimeReturnedQty = 0.0;
            if (returnBill != null && returnBill.getBillItems() != null) {
                for (BillItem rbi : returnBill.getBillItems()) {
                    if (rbi.getReferanceBillItem() != null
                            && rbi.getReferanceBillItem().equals(originalBillItem)) {
                        thisTimeReturnedQty += Math.abs(rbi.getQty());
                    }
                }
            }

            double previouslyReturnedQty = totalReturnedQty - thisTimeReturnedQty;
            double balanceQty = saleQty - totalReturnedQty;

            String itemName = originalBillItem.getItem() == null ? "" : originalBillItem.getItem().getName();

            runningStatusRows.add(new BhtIssueReturnItemStatusDto(
                    itemName, saleQty, previouslyReturnedQty, thisTimeReturnedQty, totalReturnedQty, balanceQty));
        }
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    private void calTotal() {
        double grossTotal = 0.0;
        double netTotal = 0.0;
        double marginTotal = 0.0;
        double discTotal = 0.0;

        for (BillItem p : getBillItems()) {
            grossTotal += p.getRate() * p.getQty();
            marginTotal += p.getMarginRate() * p.getQty();
            discTotal += p.getDiscountRate() * p.getQty();
            netTotal += p.getNetRate() * p.getQty();

            p.setNetValue(p.getNetRate() * p.getQty());
            p.setGrossValue(p.getRate() * p.getQty());
            p.setMarginValue(p.getMarginRate() * p.getQty());
            p.setDiscount(p.getDiscountRate() * p.getQty());

        }

        getReturnBill().setTotal(grossTotal);
        getReturnBill().setMargin(marginTotal);
        getReturnBill().setNetTotal(netTotal);
        discountTotal = discTotal;

        //  return grossTotal;
    }

    public double getDiscountTotal() {
        return discountTotal;
    }

    public void generateBillComponent() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getBill())) {
            BillItem bi = new BillItem();
            bi.setBill(getReturnBill());
            bi.setReferenceBill(getBill());
            bi.setReferanceBillItem(i.getBillItem());
            bi.copy(i.getBillItem());
            // marginRate is never persisted at issue time (only marginValue is - see
            // InpatientDirectIssueNativeSqlService.settle()'s BILLITEM insert column list),
            // so it must be reconstructed here. netRate = rate + marginRate - discountRate,
            // so marginRate = netRate - rate + discountRate. Omitting "+ discountRate"
            // (the previous formula) understates margin by the discount amount whenever
            // the original item had a nonzero discount (issue #23334).
            bi.setMarginRate(bi.getNetRate() - bi.getRate() + bi.getDiscountRate());
            bi.setQty(0.0);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            tmp.copy(i);

            double rFund = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), getBill().getBillType());

            double tmpQty = (Math.abs(i.getQtyInUnit())) - Math.abs(rFund);
            if (tmpQty <= 0) {
                continue;
            }

            tmp.setQtyInUnit(tmpQty);
            // Returning qty stays at the 0.0 default set above so the user must
            // opt in to returning each item; tmpQty (the true max returnable
            // balance) is kept on remainingQty for display/reference only, not
            // written back into the editable qty (issue #23023).
            bi.setRemainingQty(tmpQty);

            bi.setPharmaceuticalBillItem(tmp);

            getBillItems().add(bi);
        }
        calTotal();
    }

//    private double calRemainingQty(PharmaceuticalBillItem i) {
//        if (i.getRemainingQty() == 0.0) {
////            if (i.getBillItem().getItem() instanceof Ampp) {
////                return (i.getQty()) * i.getBillItem().getItem().getDblValue();
////            } else {
////                return i.getQty();
////            }
//            return i.getQty();
//        } else {
//            return i.getRemainingQty();
//        }
//
//    }    
    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmaceuticalItemController getPharmaceuticalItemController() {
        return pharmaceuticalItemController;
    }

    public void setPharmaceuticalItemController(PharmaceuticalItemController pharmaceuticalItemController) {
        this.pharmaceuticalItemController = pharmaceuticalItemController;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmacyCalculation getPharmacyRecieveBean() {
        return pharmacyRecieveBean;
    }

    public void setPharmacyRecieveBean(PharmacyCalculation pharmacyRecieveBean) {
        this.pharmacyRecieveBean = pharmacyRecieveBean;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {

            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public List<BhtIssueReturnItemStatusDto> getRunningStatusRows() {
        if (runningStatusRows == null) {
            runningStatusRows = new ArrayList<>();
        }
        return runningStatusRows;
    }

    public void setRunningStatusRows(List<BhtIssueReturnItemStatusDto> runningStatusRows) {
        this.runningStatusRows = runningStatusRows;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public String getReturnComment() {
        return returnComment;
    }

    public void setReturnComment(String returnComment) {
        this.returnComment = returnComment;
    }

}
