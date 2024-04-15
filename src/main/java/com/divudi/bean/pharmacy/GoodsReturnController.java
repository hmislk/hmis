/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Item;
import com.divudi.entity.Payment;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
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
public class GoodsReturnController implements Serializable {

    /**
     * EJBs
     */
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
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    /**
     * Controllers
     */
    @Inject
    PharmacyCalculation pharmacyCalculation;
    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private SessionController sessionController;
    /**
     * Properties
     */
    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
    private List<BillItem> billItems;
    
    private String comment;
    ///////

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        makeNull();
        this.bill = bill;
        getReturnBill().setToInstitution(getBill().getFromInstitution());
        generateBillComponent();
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new BilledBill();
            returnBill.setBillType(BillType.PharmacyGrnReturn);

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
        if ((tmp.getPharmaceuticalBillItem().getQtyInUnit()+tmp.getTmpFreeQty())> getPharmacyRecieveBean().calQty(tmp.getReferanceBillItem().getReferanceBillItem().getPharmaceuticalBillItem())) {
            tmp.setTmpQty(0.0);
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
//        getReturnBill().setToInstitution(getBill().getFromInstitution());
        getReturnBill().setToDepartment(getBill().getFromDepartment());
        getReturnBill().setFromInstitution(getBill().getToInstitution());
        getReturnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnReturn, BillClassType.BilledBill, BillNumberSuffix.GRNRET));
        getReturnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnReturn, BillClassType.BilledBill, BillNumberSuffix.GRNRET));

        getReturnBill().setInstitution(getSessionController().getInstitution());
        getReturnBill().setDepartment(getSessionController().getDepartment());
        // getReturnBill().setReferenceBill(getBill());
        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());
        getReturnBill().setComments(comment);

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

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
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            boolean returnFlag = getPharmacyBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            if (!returnFlag) {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
                getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            }

            getReturnBill().getBillItems().add(i);

        }

        getBillFacade().edit(getReturnBill());

    }

    private void saveComponent(Payment p) {
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
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            boolean returnFlag = getPharmacyBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()+i.getPharmaceuticalBillItem().getFreeQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            if (!returnFlag) {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
                getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            }
            saveBillFee(i, p);
            getReturnBill().getBillItems().add(i);

        }

        getBillFacade().edit(getReturnBill());

    }

    private boolean checkStock(PharmaceuticalBillItem pharmaceuticalBillItem) {
        double stockQty = getPharmacyBean().getStockQty(pharmaceuticalBillItem.getItemBatch(), getSessionController().getDepartment());

        if (pharmaceuticalBillItem.getQtyInUnit()+ pharmaceuticalBillItem.getFreeQtyInUnit() > stockQty) {
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
            
            if (bi.getTmpFreeQty() == 0.0) {
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
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }
        if (checkGrnItems()) {
            JsfUtil.addErrorMessage("ITems for this GRN Already issued so you can't Return ");
            return;
        }

        saveReturnBill();
        Payment p = createPayment(getReturnBill(), getReturnBill().getPaymentMethod());
//        saveComponent();
        saveComponent(p);

        calTotal();
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getReturnBill());
        
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

            double rBilled = getPharmacyRecieveBean().getTotalQty(grnPh.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
            double rCacnelled = getPharmacyRecieveBean().getTotalQty(grnPh.getBillItem(), BillType.PharmacyGrnReturn, new CancelledBill());

            double netQty = Math.abs(rBilled) - Math.abs(rCacnelled);
            
            double rFreeBilled = getPharmacyRecieveBean().getTotalFreeQty(grnPh.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
            double rFreeCacnelled = getPharmacyRecieveBean().getTotalFreeQty(grnPh.getBillItem(), BillType.PharmacyGrnReturn, new CancelledBill());
            double netFreeQty = Math.abs(rFreeBilled) - Math.abs(rFreeCacnelled);
            //System.err.println("Billed " + rBilled);
            //System.err.println("Cancelled " + rCacnelled);
            //System.err.println("Net " + netQty);
            retPh.setQtyInUnit((double) (grnPh.getQtyInUnit() - netQty));
            
            retPh.setFreeQtyInUnit((double) (grnPh.getFreeQtyInUnit() - netFreeQty));

            List<Item> suggessions = new ArrayList<>();
            Item item = bi.getItem();

//            if (item instanceof Amp) {
//                suggessions.add(item);
//                suggessions.add(getPharmacyBean().getAmpp((Amp) item));
//            } else if (item instanceof Ampp) {
//                suggessions.add(((Ampp) item).getAmp());
//                suggessions.add(item);
//            }
//
//            
//            bi.setTmpSuggession(suggessions);
            bi.setPharmaceuticalBillItem(retPh);


            getBillItems().add(bi);

        }

    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }
    
    public String navigateToGrnReturnBill(Bill b){
        setReturnBill(b);
        if(returnBill==null){
            JsfUtil.addErrorMessage("No Bill get selected");
            return "";
        }
        return "/pharmacy/pharmacy_return_good";
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

    public void saveBillFee(BillItem bi, Payment p) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(bi.getNetValue());
        bf.setFeeGrossValue(bi.getGrossValue());
        bf.setSettleValue(bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
        createBillFeePaymentAndPayment(bf, p);
    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

//    public void onEditItem(BillItem tmp) {
//        double pur = getPharmacyBean().getLastPurchaseRate(tmp.getPharmaceuticalBillItem().getBillItem().getItem(), tmp.getPharmaceuticalBillItem().getBillItem().getReferanceBillItem().getBill().getDepartment());
//        double ret = getPharmacyBean().getLastRetailRate(tmp.getPharmaceuticalBillItem().getBillItem().getItem(), tmp.getPharmaceuticalBillItem().getBillItem().getReferanceBillItem().getBill().getDepartment());
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

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
