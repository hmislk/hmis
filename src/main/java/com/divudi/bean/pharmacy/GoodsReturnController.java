/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
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
            returnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
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

    @Inject
    private PharmacyCalculation pharmacyRecieveBean;

    public void onEdit(BillItem tmp) {
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();
        if (tmp.getPharmaceuticalBillItem().getQtyInUnit() > getPharmacyRecieveBean().calculateRemainigQtyFromOrder(tmp.getReferanceBillItem().getReferanceBillItem().getPharmaceuticalBillItem())) {
            tmp.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
        }
        if (tmp.getPharmaceuticalBillItem().getFreeQtyInUnit() > getPharmacyRecieveBean().calculateRemainingFreeQtyFromOrder(tmp.getReferanceBillItem().getReferanceBillItem().getPharmaceuticalBillItem())) {
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
        getReturnBill().setBilledBill(getBill());

        String billNumber = getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_GRN_RETURN);

        getReturnBill().setDeptId(billNumber);
        getReturnBill().setInsId(billNumber);

        getReturnBill().setInstitution(getSessionController().getInstitution());
        getReturnBill().setDepartment(getSessionController().getDepartment());

        getReturnBill().setFromInstitution(getBill().getToInstitution());

        getReturnBill().setToDepartment(getBill().getDepartment());

        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());
        //Out of all the payment methods allowed, only credit payments should not have a balance. They are paid in full amount and balance is zero.
        if (getReturnBill().getPaymentMethod() != PaymentMethod.Credit) {
            getReturnBill().setBalance(0d);
            getReturnBill().setPaid(true);
            getReturnBill().setPaidAmount(getReturnBill().getNetTotal());
            getReturnBill().setPaidAt(new Date());
        } else {
            getReturnBill().setBalance(getReturnBill().getNetTotal());
            getReturnBill().setPaid(false);
            getReturnBill().setPaidAmount(0d);
        }

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        } else {
            getBillFacade().edit(getReturnBill());
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

            if ((i.getTmpQty() == 0.0) && (i.getTmpFreeQty() == 0.0)) {
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

            boolean returnFlag = getPharmacyBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            if (!returnFlag) {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
                getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            }

            double invertReturnQty = i.getPharmaceuticalBillItem().getQty();
            double invertReturnFreeQty = i.getPharmaceuticalBillItem().getFreeQty();
            i.getPharmaceuticalBillItem().setQty(0 - Math.abs(invertReturnQty));
            i.getPharmaceuticalBillItem().setFreeQty(0 - Math.abs(invertReturnFreeQty));
            getBillItemFacade().edit(i);

            saveBillFee(i, p);
            getReturnBill().getBillItems().add(i);

        }

        getBillFacade().edit(getReturnBill());

    }

    private boolean checkStock(PharmaceuticalBillItem pharmaceuticalBillItem) {
        double stockQty = getPharmacyBean().getBatchStockQty(pharmaceuticalBillItem.getItemBatch(), getSessionController().getDepartment());

        if (pharmaceuticalBillItem.getQtyInUnit() + pharmaceuticalBillItem.getFreeQtyInUnit() > stockQty) {
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

    public void removeItem(BillItem bi) {
        getBillItems().remove(bi.getSearialNo());
        calTotal();
    }

    public void settle() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Select a Bill");
            return;
        }
        if (returnBill == null) {
            JsfUtil.addErrorMessage("Programming Error. Contact System Administrator.");
            return;
        }
        if (getReturnBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Dealor");
            return;
        }
        if (getReturnBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        }
        switch (getReturnBill().getPaymentMethod()) {
            case MultiplePaymentMethods:
                JsfUtil.addErrorMessage("Multiple Payment Methods NOT allowed");
                return;
            case Agent:
            case OnCall:
            case PatientDeposit:
            case PatientPoints:
            case Staff:
            case Staff_Welfare:
            case None:
                JsfUtil.addErrorMessage("This Payment Method is NOT allowed");
                return;
        }
        if (getReturnBill().getComments() == null || getReturnBill().getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }
        if (checkGrnItems()) {
            JsfUtil.addErrorMessage("ITems for this GRN Already issued so you can't Return ");
            return;
        }
        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items selected for return");
            return;
        }

        if (!hasPositiveItemQuantity(billItems)) {
            JsfUtil.addErrorMessage("Items for This GRN Already Returned So You can't Return ");
            return;
        }

        saveReturnBill();

        Payment p = createPayment(getReturnBill(), getReturnBill().getPaymentMethod());
        saveComponent(p);

        calTotal();
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getReturnBill());

        updateOriginalBill();

        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");

    }

    private boolean hasPositiveItemQuantity(final List<BillItem> billItems) {
        for (BillItem billItem : billItems) {
            double itemQty = billItem.getPharmaceuticalBillItem().getQty();

            if (itemQty > 0.0) {
                return true;
            }
        }

        return false;
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
            retPh.setQty(grnPh.getQtyInUnit() - netQty);
            retPh.setQtyInUnit(grnPh.getQtyInUnit() - netQty);

            retPh.setFreeQty(grnPh.getQtyInUnit() - netFreeQty);
            retPh.setFreeQtyInUnit(grnPh.getFreeQtyInUnit() - netFreeQty);

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
            bi.setTmpQty(grnPh.getQtyInUnit() - netQty);
            bi.setTmpFreeQty(grnPh.getFreeQtyInUnit() - netFreeQty);
            bi.setPharmaceuticalBillItem(retPh);

            getBillItems().add(bi);

            calTotal();
            getPharmacyController().setPharmacyItem(bi.getPharmaceuticalBillItem().getBillItem().getItem());

        }

    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        p.setComments(bill.getPaymentMemo());
        setPaymentMethodData(p, pm);
        return p;
    }

    public String navigateToGrnReturnBill(Bill b) {
        setReturnBill(b);
        if (returnBill == null) {
            JsfUtil.addErrorMessage("No Bill get selected");
            return "";
        }
        return "/pharmacy/pharmacy_return_good?faces-redirect=true";
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

    private void updateOriginalBill() {
        returnBill.setBilledBill(bill);
        billFacade.edit(returnBill);
        bill.getRefundBills().add(returnBill);
        bill.setRefundAmount(Math.abs(bill.getRefundAmount()) + Math.abs(returnBill.getNetTotal()));
        bill.setBalance(Math.abs(bill.getNetTotal()) - Math.abs(bill.getRefundAmount()));
        bill.setBackwardReferenceBill(getReturnBill());
        billFacade.edit(bill);
    }

}
