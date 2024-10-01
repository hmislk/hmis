/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmaceuticalItemController;
import com.divudi.bean.pharmacy.PharmacyController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
public class StoreGoodsReturnController implements Serializable {

    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
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
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    StoreBean storeBean;
    @EJB
    private BillItemFacade billItemFacade;
    @Inject
    StoreCalculation storeCalculation;

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        makeNull();
        this.bill = bill;
        generateBillComponent();
        getReturnBill().setToInstitution(bill.getFromInstitution());
        getReturnBill().setPaymentMethod(bill.getPaymentMethod());
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new BilledBill();
            returnBill.setBillType(BillType.StoreGrnReturn);
            returnBill.setBillTypeAtomic(BillTypeAtomic.STORE_GRN_RETURN);
            returnBill.setReferenceBill(getBill());

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

        if (tmp.getPharmaceuticalBillItem().getQtyInUnit() > storeCalculation.calQty(tmp.getReferanceBillItem().getReferanceBillItem().getPharmaceuticalBillItem())) {
            tmp.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
        }
        
        if (tmp.getPharmaceuticalBillItem().getFreeQtyInUnit() > storeCalculation.calFreeQty(tmp.getReferanceBillItem().getReferanceBillItem().getPharmaceuticalBillItem())) {
            tmp.setTmpFreeQty(0.0);
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
        }

        calTotal();
        getPharmacyController().setPharmacyItem(tmp.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    public void makeNull() {
        bill = null;
        returnBill = null;
        printPreview = false;
        billItems = null;
    }

    private void saveReturnBill() {
        getReturnBill().setInvoiceDate(getBill().getInvoiceDate());
        getReturnBill().setReferenceBill(getBill());
        getReturnBill().setToInstitution(getBill().getFromInstitution());
        getReturnBill().setToDepartment(getBill().getFromDepartment());
        getReturnBill().setFromInstitution(getBill().getToInstitution());
        getReturnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StoreGrnReturn, BillClassType.BilledBill, BillNumberSuffix.GRNRET));
        getReturnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StoreGrnReturn, BillClassType.BilledBill, BillNumberSuffix.GRNRET));

        getReturnBill().setInstitution(getSessionController().getInstitution());
        getReturnBill().setDepartment(getSessionController().getDepartment());
        // getReturnBill().setReferenceBill(getBill());
        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        getBillFacade().create(getReturnBill());

    }

    private void saveComponent() {
        for (BillItem i : getBillItems()) {

            if (i.getTmpQty() == 0.0) {
                continue;
            }

            i.setBill(getReturnBill());
            i.setNetValue(i.getPharmaceuticalBillItem().getQtyInUnit() * i.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            getBillItemFacade().create(i);

            getPharmaceuticalBillItemFacade().create(tmpPh);

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            boolean returnFlag = getStoreBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            if (!returnFlag) {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
                getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            }

            getReturnBill().getBillItems().add(i);

        }

        getBillFacade().edit(getReturnBill());

    }

    private boolean checkStock(PharmaceuticalBillItem pharmaceuticalBillItem) {
        double stockQty = getStoreBean().getStockQty(pharmaceuticalBillItem.getItemBatch(), getSessionController().getDepartment());

        if (pharmaceuticalBillItem.getQtyInUnit() > stockQty) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkGrnItems() {
        for (BillItem bi : getBillItems()) {
            if (bi.getTmpQty() == 0.0) {
                continue;
            }

            if (checkStock(bi.getPharmaceuticalBillItem())) {
                return true;
            }
        }

        return false;
    }

    public void settle() {
        if (getReturnBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Dealor");
            return;
        }
        if (getReturnBill().getComments() == null || getReturnBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }
        if (checkGrnItems()) {
            JsfUtil.addErrorMessage("ITems for this GRN Already issued so you can't Return ");
            return;
        }

        saveReturnBill();
        saveComponent();

        calTotal();
        getBillFacade().edit(getReturnBill());

        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");

    }

    private void calTotal() {
        double grossTotal = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            grossTotal += p.getPharmaceuticalBillItem().getPurchaseRate() * p.getTmpQty();
            p.setSearialNo(serialNo++);
        }

        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(grossTotal);

        //  return grossTotal;
    }

    private void generateBillComponent() {
        billItems = null;
        for (PharmaceuticalBillItem grnPh : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getBill())) {
            BillItem bi = new BillItem();
            bi.setItem(grnPh.getBillItem().getItem());
            //Grns BillItem
            bi.setReferanceBillItem(grnPh.getBillItem());
            bi.setSearialNo(getBillItems().size());
            PharmaceuticalBillItem retPh = new PharmaceuticalBillItem();
            retPh.copy(grnPh);
            retPh.setBillItem(bi);

            double rBilled = storeCalculation.getTotalQty(grnPh.getBillItem(), BillType.StoreGrnReturn, new BilledBill());
            double rCacnelled = storeCalculation.getTotalQty(grnPh.getBillItem(), BillType.StoreGrnReturn, new CancelledBill());

            double netQty = Math.abs(rBilled) - Math.abs(rCacnelled);
            
            double rFreeBilled = storeCalculation.getTotalFreeQty(grnPh.getBillItem(), BillType.StoreGrnReturn, new BilledBill());
            double rFreeCacnelled = storeCalculation.getTotalFreeQty(grnPh.getBillItem(), BillType.StoreGrnReturn, new CancelledBill());
            double netFreeQty = Math.abs(rFreeBilled) - Math.abs(rFreeCacnelled);

            //System.err.println("Billed " + rBilled);
            //System.err.println("Cancelled " + rCacnelled);
            //System.err.println("Net " + netQty);
            retPh.setQty((double) (grnPh.getQtyInUnit() - netQty));
            retPh.setQtyInUnit((double) (grnPh.getQtyInUnit() - netQty));
            
            retPh.setFreeQty((double) (grnPh.getQtyInUnit() - netFreeQty));
            retPh.setFreeQtyInUnit((double) (grnPh.getFreeQtyInUnit() - netFreeQty));

            List<Item> suggessions = new ArrayList<>();
            Item item = bi.getItem();

//            if (item instanceof Amp) {
//                suggessions.add(item);
//                suggessions.add(getStoreBean().getAmpp((Amp) item));
//            } else if (item instanceof Ampp) {
//                suggessions.add(((Ampp) item).getAmp());
//                suggessions.add(item);
//            }
//
//            
//            bi.setTmpSuggession(suggessions);
            bi.setTmpQty((double) (grnPh.getQtyInUnit() - netQty));
            bi.setPharmaceuticalBillItem(retPh);


            getBillItems().add(bi);
            calTotal();

        }

    }

//    public void onEditItem(BillItem tmp) {
//        double pur = getStoreBean().getLastPurchaseRate(tmp.getPharmaceuticalBillItem().getBillItem().getItem(), tmp.getPharmaceuticalBillItem().getBillItem().getReferanceBillItem().getBill().getDepartment());
//        double ret = getStoreBean().getLastRetailRate(tmp.getPharmaceuticalBillItem().getBillItem().getItem(), tmp.getPharmaceuticalBillItem().getBillItem().getReferanceBillItem().getBill().getDepartment());
//
//        tmp.getPharmaceuticalBillItem().setPurchaseRateInUnit(pur);
//        tmp.getPharmaceuticalBillItem().setRetailRateInUnit(ret);
//        tmp.getPharmaceuticalBillItem().setLastPurchaseRateInUnit(pur);
//
//        // onEdit(tmp);
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

    public StoreBean getStoreBean() {
        return storeBean;
    }

    public void setStoreBean(StoreBean storeBean) {
        this.storeBean = storeBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
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

}
