/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberController;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;
import static org.apache.xmlbeans.impl.values.NamespaceContext.getCurrent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyPurchaseController implements Serializable {

    @Inject
    private SessionController sessionController;
    private BilledBill bill;
    @EJB
    private BillFacade billFacade;
    @Inject
    private BillNumberController billNumberBean;
    @Inject
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private AmpFacade ampFacade;
    @Inject
    PharmacyCalculation pharmacyBillBean;
    ////////////
    private BillItem currentBillItem;
    //private PharmacyItemData currentPharmacyItemData;
    private boolean printPreview;
    ///////////
    //  private List<PharmacyItemData> pharmacyItemDatas;

    public void makeNull() {
        //  currentPharmacyItemData = null;
        printPreview = false;
        currentBillItem = null;
        bill = null;
        billItems = null;
    }

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();

    }

    public void remove(BillItem b) {
        getBillItems().remove(b.getSearialNo());
    }

    public PharmacyCalculation getPharmacyBillBean() {
        return pharmacyBillBean;
    }

    public void setPharmacyBillBean(PharmacyCalculation pharmacyBillBean) {
        this.pharmacyBillBean = pharmacyBillBean;
    }

    public PharmacyPurchaseController() {
    }

    public void onEditPurchaseRate(BillItem tmp) {

        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        tmp.getPharmaceuticalBillItem().setRetailRate((double) retail);

        onEdit(tmp);
    }

    public void onEditPurchaseRate() {

        double retail = getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() + (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate((double) retail);

        onEdit(getCurrentBillItem());
    }

    public void onEdit(BillItem tmp) {

        if (tmp.getPharmaceuticalBillItem().getPurchaseRate() > tmp.getPharmaceuticalBillItem().getRetailRate()) {
            tmp.getPharmaceuticalBillItem().setRetailRate(0);
            UtilityController.addErrorMessage("You cant set retail price below purchase rate");
        }

        if (tmp.getPharmaceuticalBillItem().getDoe() != null) {
            if (tmp.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                tmp.getPharmaceuticalBillItem().setDoe(null);
                UtilityController.addErrorMessage("Check Date of Expiry");
                //    return;
            }
        }

        calTotal();
    }

    public void setBatch(BillItem pid) {
        Date date = pid.getPharmaceuticalBillItem().getDoe();
        DateFormat df = new SimpleDateFormat("ddMMyyyy");
        String reportDate = df.format(date);
// Print what date is today!
        //       //System.err.println("Report Date: " + reportDate);
        pid.getPharmaceuticalBillItem().setStringValue(reportDate);

        onEdit(pid);
    }

    public void setBatch() {
        Date date = getCurrentBillItem().getPharmaceuticalBillItem().getDoe();
        DateFormat df = new SimpleDateFormat("ddMMyyyy");
        String reportDate = df.format(date);
// Print what date is today!
        //       //System.err.println("Report Date: " + reportDate);
        getCurrentBillItem().getPharmaceuticalBillItem().setStringValue(reportDate);

        //     onEdit(pid);
    }

    public String errorCheck() {
        String msg = "";

        if (getBill().getFromInstitution() == null) {
            msg = "Please select Dealor";
            return msg;
        }

        if (getBillItems().isEmpty()) {
            msg = "Empty Items";
            return msg;
        }

        return msg;
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void settle() {

        if (getBill().getFromInstitution() == null) {
            UtilityController.addErrorMessage("Select Dealor");
            return;
        }
        if (getBill().getInvoiceNumber() == null || "".equals(getBill().getInvoiceNumber().trim())) {
            UtilityController.addErrorMessage("Please Fill invoice number");
            return;
        }

        //Need to Add History
        String msg = errorCheck();
        if (!msg.isEmpty()) {
            UtilityController.addErrorMessage(msg);
            return;
        }

        saveBill();
        //   saveBillComponent();
        getPharmacyBillBean().calSaleFreeValue(getBill());

        for (BillItem i : getBillItems()) {
            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());
            getBillItemFacade().create(i);

            getPharmaceuticalBillItemFacade().create(tmpPh);

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatch(i);
            double addingQty = tmpPh.getQtyInUnit() + tmpPh.getFreeQtyInUnit();

            tmpPh.setItemBatch(itemBatch);
            Stock stock = getPharmacyBean().addToStock(tmpPh, Math.abs(addingQty), getSessionController().getDepartment());

            tmpPh.setStock(stock);
            getPharmaceuticalBillItemFacade().edit(tmpPh);

            getBill().getBillItems().add(i);
        }

        getBillFacade().edit(getBill());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        UtilityController.addSuccessMessage("Successfully Billed");
        printPreview = true;
        //   recreate();
    }
//
//    public void recreate() {
//       
////        cashPaid = 0.0;
//        currentPharmacyItemData = null;
//        pharmacyItemDatas = null;
//    }

    private List<BillItem> billItems;

    public void addItem() {

        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() <= 0) {
            UtilityController.addErrorMessage("Please enter Purchase Rate");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getDoe() == null) {
            UtilityController.addErrorMessage("Please Set DAte of Expiry");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getQty() <= 0) {
            UtilityController.addErrorMessage("Please enter Purchase QTY");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() > getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate()) {
            UtilityController.addErrorMessage("Please enter Sale Rate Should be Over Purchase Rate");
            return;
        }

        if (getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate() <= 0) {
            UtilityController.addErrorMessage("Please enter Sale Price");
            return;
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getBillItems().add(currentBillItem);

        currentBillItem = null;

        calTotal();
    }

    public void saveBill() {

        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill(), BillType.PharmacyPurchaseBill, BillNumberSuffix.PHPUR));
        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill(), BillType.PharmacyPurchaseBill, BillNumberSuffix.PHPUR));

        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setDepartment(getSessionController().getDepartment());

        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());

        getBillFacade().edit(getBill());

    }

    public BillItem getBillItem(Item i) {
        BillItem tmp = new BillItem();
        tmp.setBill(getBill());
        tmp.setItem(i);

        //   getBillItemFacade().create(tmp);
        return tmp;
    }

    public PharmaceuticalBillItem getPharmacyBillItem(BillItem b) {
        PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
        tmp.setBillItem(b);
        //   tmp.setQty(getPharmacyBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        //     tmp.setPurchaseRate(getPharmacyBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        tmp.setRetailRate(getPharmacyBillBean().calRetailRate(tmp));
//        if (b.getId() == null || b.getId() == 0) {
//            getPharmaceuticalBillItemFacade().create(tmp);
//        } else {
//            getPharmaceuticalBillItemFacade().edit(tmp);
//        }
        return tmp;
    }

    public double getNetTotal() {

        double tmp = getBill().getTotal() + getBill().getTax() - getBill().getDiscount();
        getBill().setNetTotal(0 - tmp);

        return tmp;
    }

    public void calTotal() {
        double tot = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            p.setQty((double) p.getPharmaceuticalBillItem().getQtyInUnit());
            p.setRate(p.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            p.setSearialNo(serialNo++);
            double netValue = p.getQty() * p.getRate();

            p.setNetValue(0 - netValue);

            tot += p.getNetValue();

        }

        getBill().setTotal(tot);
        getBill().setNetTotal(tot);

    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.PharmacyPurchaseBill);
        }
        return bill;
    }

    public void setBill(BilledBill bill) {
        this.bill = bill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillNumberController getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberController billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            PharmaceuticalBillItem cuPharmaceuticalBillItem = new PharmaceuticalBillItem();
            currentBillItem.setPharmaceuticalBillItem(cuPharmaceuticalBillItem);
            cuPharmaceuticalBillItem.setBillItem(currentBillItem);
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
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
