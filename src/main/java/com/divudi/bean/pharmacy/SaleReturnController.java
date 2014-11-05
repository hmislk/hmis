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
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SaleReturnController implements Serializable {

    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
    String comment;
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
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    PaymentMethodData paymentMethodData;

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        makeNull();
        this.bill = bill;
        generateBillComponent();
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new RefundBill();
            //     returnBill.setBillType(BillType.PharmacySale);

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

        if (tmp.getQty() > getPharmacyRecieveBean().calQty3(tmp.getReferanceBillItem())) {
            tmp.setQty(0.0);
            UtilityController.addErrorMessage("You cant return over than ballanced Qty ");
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private void savePreReturnBill() {
        //   getReturnBill().setReferenceBill(getBill());

        getReturnBill().copy(getBill());

        getReturnBill().setBillType(BillType.PharmacyPre);

        getReturnBill().setBilledBill(getBill());

        getReturnBill().setTotal(0 - getReturnBill().getTotal());
        getReturnBill().setNetTotal(getReturnBill().getTotal());

        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        getReturnBill().setInstitution(getSessionController().getInstitution());
        getReturnBill().setDepartment(getSessionController().getDepartment());

        getReturnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(
                getSessionController().getInstitution(), new RefundBill(), BillType.PharmacyPre, BillNumberSuffix.PHRET));

        getReturnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(
                getSessionController().getDepartment(), new RefundBill(), BillType.PharmacyPre, BillNumberSuffix.PHRET));

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    private Bill saveSaleReturnBill() {
        RefundBill refundBill = new RefundBill();
        refundBill.copy(getReturnBill());
        refundBill.setBillType(BillType.PharmacySale);
        refundBill.setReferenceBill(getReturnBill());

        refundBill.setTotal(getReturnBill().getTotal());
        refundBill.setNetTotal(getReturnBill().getTotal());

        refundBill.setCreater(getSessionController().getLoggedUser());
        refundBill.setCreatedAt(Calendar.getInstance().getTime());

        refundBill.setInstitution(getSessionController().getInstitution());
        refundBill.setDepartment(getSessionController().getDepartment());
        refundBill.setComments(comment);

//        refundBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(
//                getSessionController().getInstitution(), new RefundBill(), BillType.PharmacySale, BillNumberSuffix.SALRET));
        refundBill.setInsId(getReturnBill().getInsId());
        refundBill.setDeptId(getReturnBill().getDeptId());

        if (refundBill.getId() == null) {
            getBillFacade().create(refundBill);
        }

        updatePreReturnBill(refundBill);

        return refundBill;

    }

    public void updatePreReturnBill(Bill ref) {
        getReturnBill().setReferenceBill(ref);
        getBillFacade().edit(getReturnBill());
    }

    private void savePreComponent() {
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQty((double) (double) i.getQty());
            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            //   i.getBillItem().setQty(i.getPharmaceuticalBillItem().getQty());
            double value = i.getNetRate() * i.getQty();
            i.setGrossValue(0 - value);
            i.setNetValue(0 - value);

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            getBillItemFacade().create(i);

            tmpPh.setBillItem(i);
            getPharmaceuticalBillItemFacade().create(tmpPh);

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            getPharmacyBean().addToStock(tmpPh.getStock(), Math.abs(tmpPh.getQtyInUnit()), tmpPh, getSessionController().getDepartment());

            //   i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem().setRemainingQty(i.getRemainingQty() - i.getQty());
            //   getPharmaceuticalBillItemFacade().edit(i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem());
            //      updateRemainingQty(i);
            getReturnBill().getBillItems().add(i);
        }

        updateReturnTotal();

    }

    private void saveSaleComponent(Bill bill) {
        for (BillItem i : getReturnBill().getBillItems()) {
            BillItem b = new BillItem();
            b.copy(i);
            b.setBill(bill);
            b.setCreatedAt(Calendar.getInstance().getTime());
            b.setCreater(getSessionController().getLoggedUser());

            getBillItemFacade().create(b);

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(i.getPharmaceuticalBillItem());
            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(b);

            bill.getBillItems().add(b);
        }

        getBillFacade().edit(bill);

    }

    private void updateReturnTotal() {
        double tot = 0;
        for (BillItem b : getReturnBill().getBillItems()) {
            tot += b.getNetValue();
        }

        getReturnBill().setTotal(tot);
        getReturnBill().setNetTotal(tot);
        getBillFacade().edit(getReturnBill());
    }

//    private void updateRemainingQty(PharmacyItemData pharmacyItemData) {
//        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + pharmacyItemData.getPoBillItem().getId();
//        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstBySQL(sql);
//        po.setRemainingQty(po.getRemainingQty() + pharmacyItemData.getPharmaceuticalBillItem().getQty());
//
//        //System.err.println("Added Remaini Qty " + pharmacyItemData.getPharmaceuticalBillItem().getQty());
//        //System.err.println("Final Remaini Qty " + po.getRemainingQty());
//        getPharmaceuticalBillItemFacade().edit(po);
//
//    }
    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void settle() {
        if (getReturnBill().getTotal() == 0) {
            UtilityController.addErrorMessage("Total is Zero cant' return");
            return;
        }

        savePreReturnBill();
        savePreComponent();

        getBill().getReturnPreBills().add(getReturnBill());
        getBillFacade().edit(getBill());

        Bill b = saveSaleReturnBill();
        saveSaleComponent(b);

//        getReturnBill().getReturnCashBills().add(b);
        getBillFacade().edit(getReturnBill());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getReturnBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        printPreview = true;
        UtilityController.addSuccessMessage("Successfully Returned");

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
        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getBill())) {
            BillItem bi = new BillItem();
            bi.setBill(getReturnBill());
            bi.setReferenceBill(getBill());
            bi.setReferanceBillItem(i.getBillItem());
            bi.copy(i.getBillItem());
            bi.setQty(0.0);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            tmp.copy(i);

            double rFund = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), BillType.PharmacyPre, new RefundBill());
            //  double rCacnelled = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), BillType.PharmacySale, new CancelledBill());

            //System.err.println("Refund " + rFund);
//                //System.err.println("Cancelled "+rCacnelled);
//                //System.err.println("Net "+(rBilled-rCacnelled));
            tmp.setQtyInUnit((double) (Math.abs(i.getQty()) - Math.abs(rFund)));

            bi.setPharmaceuticalBillItem(tmp);

            getBillItems().add(bi);

        }

    }

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

}
