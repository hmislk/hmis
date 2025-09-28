/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.util.JsfUtil;
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
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.StockBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
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

    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
    ////////

    private List<BillItem> billItems;
    ///////
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;

    public void saveIssueBillReturn() {
        saveBill();
        saveComponentForSaveReturnBill();
        JsfUtil.addSuccessMessage("Saved");
    }

    public void finalizeIssueBillReturn() {
        if (getBill().getId() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        getBill().setEditedAt(new Date());
        getBill().setEditor(sessionController.getLoggedUser());
        getBill().setCheckeAt(new Date());
        getBill().setCheckedBy(sessionController.getLoggedUser());
        getBillFacade().edit(getBill());
        JsfUtil.addSuccessMessage("Finalized");
    }

    public void settle() {

        if (getBill().getCheckedBy() == null) {
            JsfUtil.addErrorMessage("Pleace Finalise Bill First. Can not Return");
            return;
        }
        if (!hasQtyToReturn(billItems)) {
            JsfUtil.addErrorMessage("Return Quantity is Zero. Can not Return");
            return;
        }
        saveReturnBill();
        saveComponent();

        if (getBill().getPatientEncounter() != null) {
            updateMargin(getReturnBill().getBillItems(), getReturnBill(), getReturnBill().getFromDepartment(), getBill().getPatientEncounter().getPaymentMethod());
        }
        getReturnBill().setReferenceBill(getBill());
        getBillFacade().edit(getReturnBill());
        getBill().setRefundedBill(getReturnBill());
        getBill().setRefunded(true);
        getBill().getRefundBills().add(getReturnBill());
        getBill().getReturnBhtIssueBills().add(getReturnBill());
        getBillFacade().edit(getBill());

        /// setOnlyReturnValue();
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

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        makeNull();

        if (bill.getDepartment() == null) {
            return;
        }

//        if (getSessionController().getDepartment().getId() != bill.getDepartment().getId()) {
//            JsfUtil.addErrorMessage("U can't return another department's Issue.please log to specific department");
//            return;
//        }
        if (!getSessionController().getDepartment().getId().equals(bill.getDepartment().getId())) {
            JsfUtil.addErrorMessage("U can't return another department's Issue.please log to specific department");
            return;
        }

        this.bill = bill;
        generateBillComponent();
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

    @Inject
    private PharmacyCalculation pharmacyRecieveBean;

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

    public void makeNull() {
        bill = null;
        returnBill = null;
        printPreview = false;
        billItems = null;

    }

    private void saveBill() {
        getBill().setEditor(getSessionController().getLoggedUser());
        getBill().setEditedAt(Calendar.getInstance().getTime());

//        getReturnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyIssue, BillClassType.PreBill, BillNumberSuffix.PHISSRET));
//        getReturnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyIssue, BillClassType.PreBill, BillNumberSuffix.PHISSRET));
        //   getReturnBill().setInsId(getBill().getInsId());
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }

    }

    private void saveReturnBill() {

        getReturnBill().copy(getBill());

        getReturnBill().setBillType(BillType.PharmacyIssue);
        getReturnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE_RETURN);
        getReturnBill().setBilledBill(getBill());

        getReturnBill().setForwardReferenceBill(getBill().getForwardReferenceBill());

        getReturnBill().setTotal(0 - getReturnBill().getTotal());
        getReturnBill().setNetTotal(getReturnBill().getTotal());

        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        getReturnBill().setDepartment(getSessionController().getDepartment());
        getReturnBill().setInstitution(getSessionController().getInstitution());

        // Handle Department ID generation (independent)
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE_RETURN);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE_RETURN);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyIssue, BillClassType.RefundBill, BillNumberSuffix.PHISSRET);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE_RETURN);
        } else {
            // Smart fallback logic
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
                configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Return - Prefix + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department
            } else {
                // Preserve old behavior: use existing institution bill number generator
                insId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyIssue, BillClassType.RefundBill, BillNumberSuffix.PHISSRET);
            }
        }

        getReturnBill().setDeptId(deptId);
        getReturnBill().setInsId(insId);

        //   getReturnBill().setInsId(getBill().getInsId());
        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    private void saveComponent() {

        double purchaseValue = 0.0;
        double retailValue = 0.0;

        for (BillItem i : getBillItems()) {
            double itemPurchaseValue;
            double itemRetialValue;

            i.getPharmaceuticalBillItem().setQtyInUnit(i.getQty());

            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            i.setBill(getReturnBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setQty(0 - i.getPharmaceuticalBillItem().getQty());

            double value = i.getRate() * i.getQty();
            i.setGrossValue(0 - value);
            i.setNetValue(0 - value);

            itemPurchaseValue = i.getPharmaceuticalBillItem().getPurchaseRate() * i.getQty();
            itemRetialValue = i.getPharmaceuticalBillItem().getRetailRate() * i.getQty();

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
            getPharmacyBean().addToStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            //   i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem().setRemainingQty(i.getRemainingQty() - i.getQty());
            //   getPharmaceuticalBillItemFacade().edit(i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem());
            //      updateRemainingQty(i);
            getReturnBill().getBillItems().add(i);

            purchaseValue += itemPurchaseValue;
            retailValue += itemRetialValue;
        }
        StockBill clonedStockBill = getBill().getStockBill().createNewBill();
        clonedStockBill.setStockValueAtPurchaseRates(purchaseValue);
        clonedStockBill.setStockValueAsSaleRate(retailValue);
        getReturnBill().setStockBill(clonedStockBill);
        getReturnBill().getStockBill().invertStockBillValues(getReturnBill());

        getBillFacade().edit(getReturnBill());

    }

    private void saveComponentForSaveReturnBill() {
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQtyInUnit(i.getQty());
            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            i.setBill(getBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());

            double value = i.getRate() * i.getQty();
            i.setGrossValue(0 - value);
            i.setNetValue(0 - value);

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(tmpPh);
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }
            getBillItemFacade().edit(i);
            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }
            getPharmaceuticalBillItemFacade().edit(tmpPh);
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

        for (BillItem p : getBillItems()) {
            grossTotal += p.getNetRate() * p.getQty();

        }

        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(grossTotal);

        //  return grossTotal;
    }

    public void generateBillComponent() {
        billItems = new ArrayList<>();

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getBill())) {
            double rFund = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), BillType.PharmacyIssue);
            double tmpQty = Math.abs(i.getQtyInUnit()) - Math.abs(rFund);

            if (tmpQty <= 0) {
                continue;
            }

            BillItem bi = new BillItem();
            bi.setBill(getReturnBill());
            bi.setReferenceBill(getBill());
            bi.setReferanceBillItem(i.getBillItem());
            bi.copy(i.getBillItem());
            bi.setQty(tmpQty);
            bi.setRemainingQty(tmpQty);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            tmp.copy(i);
            tmp.setQtyInUnit(tmpQty);

            bi.setPharmaceuticalBillItem(tmp);
            billItems.add(bi);
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

    public List<BillItem> getBillItems() {
        if (billItems == null) {

            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
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

}
