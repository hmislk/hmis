/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.StockBill;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.service.BillService;
import com.divudi.core.util.BigDecimalUtil;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class IssueReturnController implements Serializable {

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

    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private DisposalReturnWorkflowController disposalReturnWorkflowController;

    private Bill originalBill;
    private Bill returnBill;
    private boolean printPreview;
    ////////

    private List<BillItem> originalBillItems;
    private List<BillItem> returnBillItems;

    private List<BillItem> selectedBillItems;

    public void saveDisposalIssueReturnBill() {
        // No validation required for saving drafts - users can save incomplete data
        saveBill();
        saveBillComponents();
        JsfUtil.addSuccessMessage("Saved");
    }

    public void finalizeDisposalIssueReturnBill() {
        // Validate return quantities before finalizing
        if (!validateReturnQuantities()) {
            return;
        }

        saveDisposalIssueReturnBill();
        getReturnBill().setEditedAt(new Date());
        getReturnBill().setEditor(sessionController.getLoggedUser());
        getReturnBill().setChecked(true);
        getReturnBill().setCheckeAt(new Date());
        getReturnBill().setCheckedBy(sessionController.getLoggedUser());
        getBillFacade().edit(getReturnBill());

        // Refresh the bill to ensure bill items collection is loaded for print preview
        returnBill = getBillFacade().find(getReturnBill().getId());

        printPreview=true;
        JsfUtil.addSuccessMessage("Finalized");
    }

    public void settleDisposalIssueReturnBill() {
        if (getReturnBill().getCheckedBy() == null) {
            JsfUtil.addErrorMessage("Pleace Finalise Bill First. Can not Return");
            return;
        }
        if (!hasQtyToReturn(returnBillItems)) {
            JsfUtil.addErrorMessage("Return Quantity is Zero. Can not Return");
            return;
        }
        // Final validation before settlement to ensure data integrity
        if (!validateReturnQuantities()) {
            return;
        }
        calculateBillTotal();
        saveSettlingBill();
        saveSettlingBillComponents();

        getReturnBill().setReferenceBill(getOriginalBill());
        getBillFacade().edit(getReturnBill());

        getOriginalBill().setRefundedBill(getReturnBill());
        getOriginalBill().setRefunded(true);
        getOriginalBill().getRefundBills().add(getReturnBill());

        getBillFacade().edit(getOriginalBill());

        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");

    }

    public boolean hasQtyToReturn(List<BillItem> bIList) {
        for (BillItem billItem : bIList) {
            if (billItem.getQty() > 0.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates that no item is being returned more than the available quantity
     * Checks against original issued quantity and previously returned quantities
     * @return true if all return quantities are valid, false otherwise
     */
    public boolean validateReturnQuantities() {
        if (returnBillItems == null || returnBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items found to return");
            return false;
        }

        boolean hasErrors = false;
        for (BillItem returnItem : returnBillItems) {
            if (returnItem == null || returnItem.getReferanceBillItem() == null) {
                continue;
            }

            // Use absolute value since disposal issues have negative quantities
            double requestedReturnQty = Math.abs(returnItem.getQty());
            if (requestedReturnQty <= 0) {
                continue; // Skip items with zero return quantity
            }

            // Use the comprehensive validation method without auto-correction
            if (!validateItemReturnQuantity(returnItem)) {
                hasErrors = true;
                // Continue checking all items to show all errors at once
            }
        }

        return !hasErrors;
    }

    public Bill getOriginalBill() {
        return originalBill;
    }

    public String navigateToReturnDisposalIssueBill(Bill originalIssueBill) {
        if (originalIssueBill == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        if (originalIssueBill.getId() == null) {
            JsfUtil.addErrorMessage("Programming Error");
            return null;
        }
        if (originalIssueBill.isFullReturned()) {
            JsfUtil.addErrorMessage("This disposal issue bill has been fully returned. No further returns are allowed.");
            return null;
        }
        if (!getSessionController().getDepartment().getId().equals(originalIssueBill.getDepartment().getId())) {
            JsfUtil.addErrorMessage("U can't return another department's Issue.please log to specific department");
            return null;
        }

        // Check for existing pending disposal returns for this specific bill
        if (disposalReturnWorkflowController.hasPendingDisposalReturnForSpecificBill(originalIssueBill)) {
            JsfUtil.addErrorMessage("Cannot create new return for this disposal issue bill. There is already a pending disposal return for this bill. Please finalize and approve the existing return first, or cancel it before creating a new one.");
            return null;
        }

        resetBillValues();
        originalBill = originalIssueBill;
        if (!generateBillComponent()) {
            JsfUtil.addErrorMessage("Programming Error");
            return null;
        }
        return "/pharmacy/pharmacy_bill_return_issue?faces-redirect=true";
    }

    public void setOriginalBill(Bill originalBill) {
        this.originalBill = originalBill;
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new RefundBill();
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

    @Inject
    private PharmacyCalculation pharmacyRecieveBean;

    @Deprecated
    public void onEdit(BillItem tmp) {
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();

        if (tmp.getQty() == null) {
            JsfUtil.addErrorMessage("Qty Null");
            return;
        }

        if (tmp.getQty() > tmp.getPharmaceuticalBillItem().getQty()) {
            tmp.setQty(0.0);
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
            return;
        }
        if (tmp.getQty() > tmp.getRemainingQty()) {
            tmp.setQty(0.0);
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
            return;
        }

        calTotal();
        //   getPharmacyController().setPharmacyItem(tmp.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    @Deprecated // use resetBillValues() 
    public void makeNull() {
        originalBill = null;
        returnBill = null;
        printPreview = false;
        originalBillItems = null;

    }

    public void resetBillValues() {
        originalBill = null;
        returnBill = null;
        printPreview = false;
        originalBillItems = null;
    }

    private void saveBill() {
        if (getReturnBill().getReferenceBill() == null) {
            getReturnBill().setReferenceBill(originalBill);
        }
        if (getReturnBill().getId() == null) {
            getReturnBill().setDepartment(sessionController.getDepartment());
            getReturnBill().setInstitution(sessionController.getInstitution());
            getReturnBill().setSite(sessionController.getLoggedSite());
            getReturnBill().setCreatedAt(new Date());
            getReturnBill().setCreater(sessionController.getLoggedUser());
            getBillFacade().create(getReturnBill());
        } else {
            getBillFacade().edit(getReturnBill());
        }
    }

    private void saveSettlingBill() {

        getReturnBill().setBillType(BillType.PharmacyIssue);
        getReturnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        getReturnBill().setBilledBill(getOriginalBill());
        getReturnBill().setCompleted(true);
        getReturnBill().setCompletedAt(new Date());
        getReturnBill().setCompletedBy(sessionController.getLoggedUser());

        getReturnBill().setForwardReferenceBill(getOriginalBill().getForwardReferenceBill());

        // Handle Department ID generation (independent)
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyIssue, BillClassType.RefundBill, BillNumberSuffix.PHISSRET);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        } else {
            // Smart fallback logic
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Department Code + Institution Code + Year + Yearly Number", false)
                    || configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department
            } else {
                // Preserve old behavior: use existing institution originalBill number generator
                insId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyIssue, BillClassType.RefundBill, BillNumberSuffix.PHISSRET);
            }
        }
        getReturnBill().setDeptId(deptId);
        getReturnBill().setInsId(insId);
        billFacade.edit(returnBill);
    }

    private void saveSettlingBillComponents() {
        boolean fullyReturned = true;
        List<BillItem> itemsToProcess = new ArrayList<>(getReturnBillItems());
        for (BillItem i : itemsToProcess) {
            i.getPharmaceuticalBillItem().setQty(Math.abs(i.getQty()));

            i.setBill(getReturnBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setQty(Math.abs(i.getPharmaceuticalBillItem().getQty()));

            double value = i.getRate() * i.getQty();
            i.setGrossValue(0 - value);
            i.setNetValue(0 - value);

            BillItem referenceBillItem = i.getReferanceBillItem();
            BillItemFinanceDetails financeDetailsOfOriginal = referenceBillItem != null ? referenceBillItem.getBillItemFinanceDetails() : null;
            BillItemFinanceDetails financeDetailsOfReturn = i.getBillItemFinanceDetails();

            BigDecimal settledQty = financeDetailsOfReturn == null
                    ? BigDecimal.ZERO
                    : BigDecimalUtil.valueOrZero(financeDetailsOfReturn.getQuantity());

            if (financeDetailsOfOriginal != null) {
                // Use absolute values for all calculations since disposal issues have negative quantities
                BigDecimal alreadyReturned = BigDecimalUtil.valueOrZero(financeDetailsOfOriginal.getReturnQuantity()).abs();
                BigDecimal settledQtyAbs = settledQty.abs();
                BigDecimal updatedReturnQty = alreadyReturned.add(settledQtyAbs);
                financeDetailsOfOriginal.setReturnQuantity(updatedReturnQty);

                BigDecimal originalQty = BigDecimalUtil.valueOrZero(financeDetailsOfOriginal.getQuantity()).abs();
                if (originalQty.compareTo(updatedReturnQty) > 0) {
                    fullyReturned = false;
                }

                // Calculate and update remaining quantity in BillItem (in packs for AMPP, units otherwise)
                BigDecimal remainingQty = originalQty.subtract(updatedReturnQty);
                referenceBillItem.setRemainingQty(remainingQty.doubleValue());

                // Update remaining quantities in PharmaceuticalBillItem if available
                if (referenceBillItem.getPharmaceuticalBillItem() != null) {
                    PharmaceuticalBillItem originalPbi = referenceBillItem.getPharmaceuticalBillItem();

                    // PharmaceuticalBillItem always uses units
                    // Calculate remaining quantity in units using absolute values
                    BigDecimal originalQtyInUnits = BigDecimalUtil.valueOrZero(financeDetailsOfOriginal.getQuantityByUnits()).abs();

                    // Calculate total returned quantity in units by using units per pack
                    BigDecimal returnedQtyInUnits;
                    if (financeDetailsOfOriginal.getUnitsPerPack() != null &&
                        financeDetailsOfOriginal.getUnitsPerPack().compareTo(BigDecimal.ZERO) > 0) {
                        // For AMPP items: convert returned packs to units
                        returnedQtyInUnits = updatedReturnQty.multiply(financeDetailsOfOriginal.getUnitsPerPack());
                    } else {
                        // For non-pack items: returned qty equals returned units
                        returnedQtyInUnits = updatedReturnQty;
                    }

                    BigDecimal remainingQtyInUnits = originalQtyInUnits.subtract(returnedQtyInUnits);
                    originalPbi.setRemainingQty(remainingQtyInUnits.doubleValue());

                    // Calculate remaining quantity in packs
                    // For AMPP items: convert units back to packs
                    // For non-AMPP items: same as units
                    if (financeDetailsOfOriginal.getUnitsPerPack() != null &&
                        financeDetailsOfOriginal.getUnitsPerPack().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal remainingQtyPack = remainingQtyInUnits.divide(
                            financeDetailsOfOriginal.getUnitsPerPack(),
                            2,
                            java.math.RoundingMode.HALF_UP
                        );
                        originalPbi.setRemainingQtyPack(remainingQtyPack.doubleValue());
                    } else {
                        originalPbi.setRemainingQtyPack(remainingQtyInUnits.doubleValue());
                    }
                }

                getBillItemFacade().edit(referenceBillItem);
            } else {
                fullyReturned = false;
            }

            getBillItemFacade().edit(i);

            getPharmacyBean().addToStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            // No need to add to collection - bill relationship already set at line 361 and persisted at line 437

        }
        if(fullyReturned){
            getOriginalBill().setFullReturned(true);
            getOriginalBill().setFullReturnedAt(new Date());
            getOriginalBill().setFullReturnedBy(sessionController.getLoggedUser());
            getBillFacade().edit(getOriginalBill());
        }
        getBillFacade().edit(getReturnBill());

    }

    private void saveBillComponents() {
        for (BillItem i : getReturnBillItems()) {
            i.setBill(getReturnBill());
            if (i.getId() == null) {
                i.setCreatedAt(Calendar.getInstance().getTime());
                i.setCreater(getSessionController().getLoggedUser());
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }
        }
    }

    public void updateMargin(BillItem bi, Department matrixDepartment, PaymentMethod paymentMethod) {
        double rate = Math.abs(bi.getRate());
        double margin = 0;

        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod);

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
        bill.setNetTotal(netTotal);
        getBillFacade().edit(bill);

    }

    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    InwardBeanController inwardBean;

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    private void calTotal() {
        double grossTotal = 0.0;

        for (BillItem p : getOriginalBillItems()) {
            grossTotal += p.getNetRate() * p.getQty();

        }

        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(grossTotal);

        //  return grossTotal;
    }

    public boolean generateBillComponent() {
        returnBill = new RefundBill();
        returnBill.setReferenceBill(originalBill);
        returnBill.setBillType(BillType.PharmacyDisposalIssue);
        returnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        originalBillItems = billService.fetchBillItems(originalBill);
        returnBillItems = new ArrayList<>();
        if (originalBillItems == null || originalBillItems.isEmpty()) {
            return false;
        }
        for (BillItem originalBillItem : originalBillItems) {

            BillItem returningBillItem = new BillItem();
            returningBillItem.setBill(returnBill);
            returningBillItem.setReferenceBill(originalBill);
            returningBillItem.setReferanceBillItem(originalBillItem);
            returningBillItem.copy(originalBillItem);
            returningBillItem.setQty(originalBillItem.getRemainingQty());
            returningBillItem.setRemainingQty(originalBillItem.getRemainingQty());

            PharmaceuticalBillItem returningPbi = returningBillItem.getPharmaceuticalBillItem();
            returningPbi.copy(originalBillItem.getPharmaceuticalBillItem());

            returningPbi.setBillItem(returningBillItem);
            returningBillItem.setPharmaceuticalBillItem(returningPbi);

            returnBillItems.add(returningBillItem);
        }
        return true;
    }

    @Deprecated
    public void generateBillComponentOld() {
        originalBillItems = new ArrayList<>();

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getOriginalBill())) {
            double rFund = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), BillType.PharmacyIssue);
            double tmpQty = Math.abs(i.getQtyInUnit()) - Math.abs(rFund);

            if (tmpQty <= 0) {
                continue;
            }

            BillItem bi = new BillItem();
            bi.setBill(getReturnBill());
            bi.setReferenceBill(getOriginalBill());
            bi.setReferanceBillItem(i.getBillItem());
            bi.copy(i.getBillItem());
            bi.setQty(tmpQty);
            bi.setRemainingQty(tmpQty);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            tmp.copy(i);
            tmp.setQtyInUnit(tmpQty);

            bi.setPharmaceuticalBillItem(tmp);
            originalBillItems.add(bi);
        }
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

    public void deleteSelectedItems() {
        if (selectedBillItems == null || selectedBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items selected for deletion");
            return;
        }
        for (BillItem selectedItem : selectedBillItems) {
            if (selectedItem.getId() != null) {
                selectedItem.setBill(null);
                selectedItem.setRetired(true);
                billItemFacade.edit(selectedItem);
            }
            returnBillItems.remove(selectedItem);
        }
        selectedBillItems.clear();
        calculateBillTotal();
        JsfUtil.addSuccessMessage("Selected items deleted successfully");
    }

    public void deleteItem(BillItem itemToDelete) {
        if (itemToDelete == null) {
            JsfUtil.addErrorMessage("No item to delete");
            return;
        }
        if (itemToDelete.getId() != null) {
            itemToDelete.setBill(null);
            itemToDelete.setRetired(true);
            billItemFacade.edit(itemToDelete);
        }
        returnBillItems.remove(itemToDelete);
        calculateBillTotal();
        JsfUtil.addSuccessMessage("Item deleted successfully");
    }

    public List<BillItem> getOriginalBillItems() {
        if (originalBillItems == null) {

            originalBillItems = new ArrayList<>();
        }
        return originalBillItems;
    }

    public void setOriginalBillItems(List<BillItem> originalBillItems) {
        this.originalBillItems = originalBillItems;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    public List<BillItem> getSelectedBillItems() {
        if (selectedBillItems == null) {
            selectedBillItems = new ArrayList<>();
        }
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public List<BillItem> getReturnBillItems() {
        return returnBillItems;
    }

    public void setReturnBillItems(List<BillItem> returnBillItems) {
        this.returnBillItems = returnBillItems;
    }

    /**
     * Validates return quantity for a specific item against both remaining quantity and stock availability
     * This version is for finalization - does NOT auto-correct, only shows errors
     * @param bi The bill item to validate
     * @return true if validation passes, false if validation fails
     */
    public boolean validateItemReturnQuantity(BillItem bi) {
        return validateItemReturnQuantity(bi, false);
    }

    /**
     * Validates return quantity for a specific item against both remaining quantity and stock availability
     * Similar to GRN return validation logic but adapted for issue returns
     * @param bi The bill item to validate
     * @param autoCorrect If true, automatically corrects invalid quantities; if false, only shows error
     * @return true if validation passes, false if validation fails
     */
    public boolean validateItemReturnQuantity(BillItem bi, boolean autoCorrect) {
        if (bi == null) {
            return false;
        }

        double requestedQty = Math.abs(bi.getQty());
        String itemName = bi.getItem() != null ? bi.getItem().getName() : "Unknown Item";

        // First check: Validate against original issued quantity and already returned quantity
        if (bi.getReferanceBillItem() != null && requestedQty > 0) {
            BillItem originalItem = bi.getReferanceBillItem();
            // Use absolute values since disposal issues have negative quantities
            double originalIssuedQty = Math.abs(originalItem.getQty());

            // Get already returned quantity from finance details (always stored as positive)
            double alreadyReturnedQty = 0.0;
            if (originalItem.getBillItemFinanceDetails() != null &&
                originalItem.getBillItemFinanceDetails().getReturnQuantity() != null) {
                alreadyReturnedQty = Math.abs(originalItem.getBillItemFinanceDetails().getReturnQuantity().doubleValue());
            }

            // Calculate maximum returnable quantity
            double maxReturnableQty = originalIssuedQty - alreadyReturnedQty;

            // Validate requested quantity doesn't exceed what's available to return
            if (requestedQty > maxReturnableQty) {
                if (autoCorrect) {
                    JsfUtil.addErrorMessage(String.format(
                        "Return quantity %.2f for '%s' exceeds available quantity. " +
                        "Original issued: %.2f, Already returned: %.2f, Maximum returnable: %.2f. " +
                        "Corrected to maximum available.",
                        requestedQty, itemName, originalIssuedQty, alreadyReturnedQty, maxReturnableQty
                    ));
                    bi.setQty(maxReturnableQty);
                    bi.setRemainingQty(maxReturnableQty);
                } else {
                    JsfUtil.addErrorMessage(String.format(
                        "Return quantity %.2f for '%s' exceeds available quantity. " +
                        "Original issued: %.2f, Already returned: %.2f, Maximum returnable: %.2f. " +
                        "Please correct the quantity before finalizing.",
                        requestedQty, itemName, originalIssuedQty, alreadyReturnedQty, maxReturnableQty
                    ));
                }
                return false;
            }
        }

        // Second check: Validate against remaining quantity (what can be returned) - fallback validation
        if (requestedQty > 0 && bi.getRemainingQty() > 0 && requestedQty > bi.getRemainingQty()) {
            if (autoCorrect) {
                JsfUtil.addErrorMessage(String.format(
                    "Return quantity %.2f for '%s' exceeds available quantity %.2f. Corrected to maximum available.",
                    requestedQty, itemName, bi.getRemainingQty()
                ));
                bi.setQty(bi.getRemainingQty());
            } else {
                JsfUtil.addErrorMessage(String.format(
                    "Return quantity %.2f for '%s' exceeds available quantity %.2f. Please correct the quantity before finalizing.",
                    requestedQty, itemName, bi.getRemainingQty()
                ));
            }
            return false;
        }

        // NOTE: We do NOT check stock levels for disposal issue returns
        // Because returns ADD to stock, not subtract from it
        // Stock can be zero or negative, and returns will increase it

        return true; // All validations passed
    }

    public void calculateBillItemTotals(BillItem bi) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getItemBatch() == null) {
            return;
        }
        if (bi.getItem() == null) {
            return;
        }
        if (bi.getItem().getId() == null) {
            return;
        }
        double qty = bi.getQty();

        // Comprehensive validation with auto-correction for user input
        if (!validateItemReturnQuantity(bi, true)) {
            // Validation failed, quantity has been auto-corrected
            // Re-calculate with the corrected quantity
            qty = bi.getQty();
        }

        // Get rates from item batch - these are always in units
        double costRate = bi.getPharmaceuticalBillItem().getItemBatch().getCostRate();
        double purchaseRate = bi.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate();
        double retailRate = bi.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate();
        double wholesaleRate = bi.getPharmaceuticalBillItem().getItemBatch().getWholesaleRate();

        // Handle AMPP conversion to units if needed
        double qtyInUnits;
        if (bi.getItem() instanceof Ampp) {
            // Convert qty to units for AMPP items
            qtyInUnits = qty * bi.getItem().getDblValue();
            bi.getPharmaceuticalBillItem().setQty(qtyInUnits);
        } else {
            qtyInUnits = qty;
            bi.getPharmaceuticalBillItem().setQty(qtyInUnits);
        }

        // Calculate basic bill item values
        double rate = bi.getRate();
        double grossValue = rate * qty;
        bi.setGrossValue(grossValue);

        // For return bills, net value equals gross value (no discounts/margins in returns)
        bi.setNetValue(grossValue);

        // Initialize BillItemFinanceDetails if null
        if (bi.getBillItemFinanceDetails() == null) {
            bi.setBillItemFinanceDetails(new BillItemFinanceDetails());
        }

        BillItemFinanceDetails bifd = bi.getBillItemFinanceDetails();

        // Calculate all rate-based values using quantity in units
        bifd.setValueAtCostRate(BigDecimal.valueOf(costRate * qtyInUnits));
        bifd.setValueAtPurchaseRate(BigDecimal.valueOf(purchaseRate * qtyInUnits));
        bifd.setValueAtRetailRate(BigDecimal.valueOf(retailRate * qtyInUnits));
        bifd.setValueAtWholesaleRate(BigDecimal.valueOf(wholesaleRate * qtyInUnits));

        // Set line-level rates and totals
        bifd.setLineGrossRate(BigDecimal.valueOf(rate));
        bifd.setLineNetRate(BigDecimal.valueOf(rate));
        bifd.setLineGrossTotal(BigDecimal.valueOf(grossValue));
        bifd.setLineNetTotal(BigDecimal.valueOf(grossValue));

        // Set overall rates and totals (same as line for single items)
        bifd.setGrossRate(BigDecimal.valueOf(rate));
        bifd.setNetRate(BigDecimal.valueOf(rate));
        bifd.setGrossTotal(BigDecimal.valueOf(grossValue));
        bifd.setNetTotal(BigDecimal.valueOf(grossValue));

        // Set quantities in finance details
        bifd.setQuantity(BigDecimal.valueOf(qty));
        bifd.setQuantityByUnits(BigDecimal.valueOf(qtyInUnits));
        bifd.setTotalQuantity(BigDecimal.valueOf(qty));
        bifd.setTotalQuantityByUnits(BigDecimal.valueOf(qtyInUnits));

        // Set retail and wholesale rates per unit
        bifd.setRetailSaleRate(BigDecimal.valueOf(retailRate));
        bifd.setWholesaleRate(BigDecimal.valueOf(wholesaleRate));

        // For returns, discounts, taxes, and expenses are typically zero
        bifd.setLineDiscount(BigDecimal.ZERO);
        bifd.setBillDiscount(BigDecimal.ZERO);
        bifd.setTotalDiscount(BigDecimal.ZERO);
        bifd.setLineDiscountRate(BigDecimal.ZERO);
        bifd.setBillDiscountRate(BigDecimal.ZERO);
        bifd.setTotalDiscountRate(BigDecimal.ZERO);

        bifd.setLineTax(BigDecimal.ZERO);
        bifd.setBillTax(BigDecimal.ZERO);
        bifd.setTotalTax(BigDecimal.ZERO);
        bifd.setLineTaxRate(BigDecimal.ZERO);
        bifd.setBillTaxRate(BigDecimal.ZERO);
        bifd.setTotalTaxRate(BigDecimal.ZERO);

        bifd.setLineExpense(BigDecimal.ZERO);
        bifd.setBillExpense(BigDecimal.ZERO);
        bifd.setTotalExpense(BigDecimal.ZERO);
        bifd.setLineExpenseRate(BigDecimal.ZERO);
        bifd.setBillExpenseRate(BigDecimal.ZERO);
        bifd.setTotalExpenseRate(BigDecimal.ZERO);

        bifd.setLineCost(BigDecimal.valueOf(costRate * qtyInUnits));
        bifd.setBillCost(BigDecimal.valueOf(costRate * qtyInUnits));
        bifd.setTotalCost(BigDecimal.valueOf(costRate * qtyInUnits));
        bifd.setLineCostRate(BigDecimal.valueOf(costRate));
        bifd.setBillCostRate(BigDecimal.valueOf(costRate));
        bifd.setTotalCostRate(BigDecimal.valueOf(costRate));

        // Set return-specific values
        bifd.setReturnQuantity(BigDecimal.valueOf(qty));
        bifd.setTotalReturnQuantity(BigDecimal.valueOf(qty));
        bifd.setReturnGrossTotal(BigDecimal.valueOf(grossValue));
        bifd.setReturnNetTotal(BigDecimal.valueOf(grossValue));

        // Set units per pack (for AMPP items)
        if (bi.getItem() instanceof Ampp && bi.getItem().getDblValue() !=  0.0) {
            bifd.setUnitsPerPack(BigDecimal.valueOf(bi.getItem().getDblValue()));
        } else {
            bifd.setUnitsPerPack(BigDecimal.ONE);
        }

        // Set pharmaceutical bill item rates for tracking
        bi.getPharmaceuticalBillItem().setCostRate(costRate);
        bi.getPharmaceuticalBillItem().setPurchaseRate(purchaseRate);
        bi.getPharmaceuticalBillItem().setRetailRate(retailRate);
        bi.getPharmaceuticalBillItem().setWholesaleRate(wholesaleRate);

        calculateBillTotal();
    }

    public void calculateBillTotal() {
        if (returnBillItems == null || returnBillItems.isEmpty()) {
            return;
        }
        // Always calculate from returnBillItems for issue returns
        calculateBillTotalFromItems(returnBillItems);
    }

    private void calculateBillTotalFromItems(List<BillItem> billItems) {
        // Initialize totals
        double netTotal = 0.0;
        double grossTotal = 0.0;
        double totalDiscountValue = 0.0;
        double totalTaxValue = 0.0;
        double totalExpenseValue = 0.0;

        // Initialize BillFinanceDetails aggregates
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalQuantityInUnits = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;
        BigDecimal lineDiscountTotal = BigDecimal.ZERO;
        BigDecimal lineTaxTotal = BigDecimal.ZERO;
        BigDecimal lineExpenseTotal = BigDecimal.ZERO;
        BigDecimal lineCostTotal = BigDecimal.ZERO;

        // Iterate through bill items and sum up totals
        for (BillItem bi : billItems) {
            if (bi == null) {
                continue;
            }

            // Add to gross and net totals
            double netValue = bi.getNetValue();
            double grossValue = bi.getGrossValue();

            netTotal += netValue;
            grossTotal += grossValue;

            // Add quantities
            totalQuantity = totalQuantity.add(BigDecimal.valueOf(bi.getQty()));

            // Add quantity in units (for AMPP conversion)
            if (bi.getPharmaceuticalBillItem() != null) {
                totalQuantityInUnits = totalQuantityInUnits.add(BigDecimal.valueOf(bi.getPharmaceuticalBillItem().getQty()));
            } else {
                totalQuantityInUnits = totalQuantityInUnits.add(BigDecimal.valueOf(bi.getQty()));
            }

            // Add line totals from BillItemFinanceDetails if available
            if (bi.getBillItemFinanceDetails() != null) {
                BillItemFinanceDetails bifd = bi.getBillItemFinanceDetails();

                if (bifd.getValueAtCostRate() != null) {
                    totalCostValue = totalCostValue.add(bifd.getValueAtCostRate());
                }
                if (bifd.getValueAtPurchaseRate() != null) {
                    totalPurchaseValue = totalPurchaseValue.add(bifd.getValueAtPurchaseRate());
                }
                if (bifd.getValueAtRetailRate() != null) {
                    totalRetailSaleValue = totalRetailSaleValue.add(bifd.getValueAtRetailRate());
                }
                if (bifd.getValueAtWholesaleRate() != null) {
                    totalWholesaleValue = totalWholesaleValue.add(bifd.getValueAtWholesaleRate());
                }

                // Line-level totals
                if (bifd.getLineGrossTotal() != null) {
                    lineGrossTotal = lineGrossTotal.add(bifd.getLineGrossTotal());
                }
                if (bifd.getLineNetTotal() != null) {
                    lineNetTotal = lineNetTotal.add(bifd.getLineNetTotal());
                }
                if (bifd.getLineDiscount() != null) {
                    lineDiscountTotal = lineDiscountTotal.add(bifd.getLineDiscount());
                }
                if (bifd.getLineTax() != null) {
                    lineTaxTotal = lineTaxTotal.add(bifd.getLineTax());
                }
                if (bifd.getLineExpense() != null) {
                    lineExpenseTotal = lineExpenseTotal.add(bifd.getLineExpense());
                }
                if (bifd.getLineCost() != null) {
                    lineCostTotal = lineCostTotal.add(bifd.getLineCost());
                }
            }

            // NO discount, tax, and expense from bill items
            // For return bills, these are typically zero but we sum them anyway

        }

        // Update Bill entity fields
        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(netTotal);
        getReturnBill().setGrantTotal(netTotal); // Grant total typically equals net total for returns
        getReturnBill().setBillTotal(netTotal); // Bill total typically equals net total for returns

        // For return bills, discounts and taxes are typically zero
        getReturnBill().setDiscount(totalDiscountValue);
        getReturnBill().setTax(totalTaxValue);
        getReturnBill().setVat(0.0); // VAT is typically zero for returns
        getReturnBill().setVatPlusNetTotal(netTotal); // Net total without VAT

        // Set expense total
        getReturnBill().setExpenseTotal(totalExpenseValue);
        getReturnBill().setExpensesTotalConsideredForCosting(totalExpenseValue);
        getReturnBill().setExpensesTotalNotConsideredForCosting(0.0);

        // Set sale and free values (for returns, free value is typically zero)
        getReturnBill().setSaleValue(netTotal);
        getReturnBill().setFreeValue(0.0);

        // Initialize and populate BillFinanceDetails
        if (getReturnBill().getBillFinanceDetails() == null) {
            getReturnBill().setBillFinanceDetails(new BillFinanceDetails());
        }

        BillFinanceDetails bfd = getReturnBill().getBillFinanceDetails();

        // Set discount totals
        bfd.setBillDiscount(BigDecimal.ZERO); // Bill-level discount is zero for returns
        bfd.setLineDiscount(lineDiscountTotal);
        bfd.setTotalDiscount(lineDiscountTotal);

        // Set expense totals
        bfd.setBillExpense(BigDecimal.ZERO); // Bill-level expense is zero for returns
        bfd.setLineExpense(lineExpenseTotal);
        bfd.setTotalExpense(lineExpenseTotal);
        bfd.setBillExpensesConsideredForCosting(BigDecimal.ZERO);
        bfd.setBillExpensesNotConsideredForCosting(BigDecimal.ZERO);

        // Set cost totals
        bfd.setBillCostValue(BigDecimal.ZERO); // Bill-level cost is zero for returns
        bfd.setLineCostValue(lineCostTotal);
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalCostValueFree(BigDecimal.ZERO);
        bfd.setTotalCostValueNonFree(totalCostValue);

        // Set tax totals
        bfd.setBillTaxValue(BigDecimal.ZERO); // Bill-level tax is zero for returns
        bfd.setItemTaxValue(lineTaxTotal);
        bfd.setTotalTaxValue(lineTaxTotal);

        // Set purchase value totals
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalPurchaseValueFree(BigDecimal.ZERO);
        bfd.setTotalPurchaseValueNonFree(totalPurchaseValue);

        // Set retail and wholesale value totals
        bfd.setTotalRetailSaleValue(totalRetailSaleValue);
        bfd.setTotalRetailSaleValueFree(BigDecimal.ZERO);
        bfd.setTotalRetailSaleValueNonFree(totalRetailSaleValue);

        bfd.setTotalWholesaleValue(totalWholesaleValue);
        bfd.setTotalWholesaleValueFree(BigDecimal.ZERO);
        bfd.setTotalWholesaleValueNonFree(totalWholesaleValue);

        // Set quantity totals
        bfd.setTotalQuantity(totalQuantity);
        bfd.setTotalFreeQuantity(BigDecimal.ZERO);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQuantityInUnits);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(BigDecimal.ZERO);

        // Set gross and net totals
        bfd.setLineGrossTotal(lineGrossTotal);
        bfd.setBillGrossTotal(BigDecimal.ZERO); // Bill-level gross is zero for returns
        bfd.setGrossTotal(BigDecimal.valueOf(grossTotal));

        bfd.setLineNetTotal(lineNetTotal);
        bfd.setBillNetTotal(BigDecimal.ZERO); // Bill-level net is zero for returns
        bfd.setNetTotal(BigDecimal.valueOf(netTotal));

        // Set free item values (typically zero for returns)
        bfd.setTotalOfFreeItemValues(BigDecimal.ZERO);
        bfd.setTotalOfFreeItemValuesFree(BigDecimal.ZERO);
        bfd.setTotalOfFreeItemValuesNonFree(BigDecimal.ZERO);

        // Set adjustment values (same as totals for returns)
        bfd.setTotalBeforeAdjustmentValue(BigDecimal.valueOf(grossTotal));
        bfd.setTotalAfterAdjustmentValue(BigDecimal.valueOf(netTotal));
    }

}
