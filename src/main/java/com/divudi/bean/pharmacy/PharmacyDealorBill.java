/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PaymentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
public class PharmacyDealorBill implements Serializable {

    //Atribtes
    private boolean printPreview;
    private Bill current;
    private PaymentMethodData paymentMethodData;
    private BillItem currentBillItem;
    private Institution institution;
    //List
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    //EJB
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    //Inject
    @Inject
    private SessionController sessionController;
    @Inject
    CommonController commonController;
    

    public void makeNull() {
        printPreview = false;
        current = null;
        currentBillItem = null;
        institution = null;
        paymentMethodData = null;
        selectedBillItems = null;
        billItems = null;
    }

    private boolean errorCheckForAdding() {
        if (getCurrentBillItem().getReferenceBill().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("U cant add without credit company name");
            return true;
        }

        if (!isPaidAmountOk(getCurrentBillItem())) {
            JsfUtil.addSuccessMessage("U cant add more than ballance");
            return true;
        }

        for (BillItem b : getBillItems()) {
            if (b.getReferenceBill() != null && b.getReferenceBill().getFromInstitution() != null) {
                if (!Objects.equals(getCurrentBillItem().getReferenceBill().getFromInstitution().getId(), b.getReferenceBill().getFromInstitution().getId())) {
                    JsfUtil.addErrorMessage("U can add only one type Credit companies at Once");
                    return true;
                }
            }
        }

        return false;
    }

    @EJB
    CreditBean creditBean;

    private double getReferenceBallance(BillItem billItem) {
        double refBallance = 0;
        double neTotal = Math.abs(billItem.getReferenceBill().getNetTotal());
        double returned = Math.abs(billItem.getReferenceBill().getTmpReturnTotal());
        double paidAmt = Math.abs(getCreditBean().getPaidAmount(billItem.getReferenceBill(), BillType.GrnPaymentPre));

        refBallance = neTotal - (paidAmt + returned);

        return refBallance;
    }

    public void selectListener() {

        double ballanceAmt = getReferenceBallance(getCurrentBillItem());

        if (ballanceAmt > 0.1) {
            getCurrentBillItem().setNetValue(ballanceAmt);
        }

    }

    @Inject
    private BillController billController;

    public void selectInstitutionListener() {
        Institution ins = institution;
        makeNull();

        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        List<Bill> list = getBillController().getDealorBills(ins, billTypesListBilled);
        for (Bill b : list) {
            getCurrentBillItem().setReferenceBill(b);
            double returned = Math.abs(getCreditBean().getGrnReturnValue(getCurrentBillItem().getReferenceBill(), billTypesListReturn));
            getCurrentBillItem().getReferenceBill().setTmpReturnTotal(returned);

            selectListener();
            addToBill();
        }
        calTotalBySelectedBillTems();
    }

    public void addToBill() {

        if (errorCheckForAdding()) {
            return;
        }

        getCurrent().setToInstitution(getCurrentBillItem().getReferenceBill().getFromInstitution());
        //     getCurrentBillItem().getBill().setNetTotal(getCurrentBillItem().getNetValue());
        //     getCurrentBillItem().getBill().setTotal(getCurrent().getNetTotal());

        if (getCurrentBillItem().getNetValue() != 0) {
            //  System.err.println("11 " + getCurrentBillItem().getReferenceBill().getDeptId());
            //   System.err.println("aa " + getCurrentBillItem().getNetValue());
            getCurrentBillItem().setSearialNo(getBillItems().size());
            getBillItems().add(getCurrentBillItem());
        }

        currentBillItem = null;
        calTotal();
    }

    public void changeNetValueListener(BillItem billItem) {

        if (!isPaidAmountOk(billItem)) {
            billItem.setNetValue(0);
//            JsfUtil.addSuccessMessage("U cant add more than ballance");
//            return;
        }

        calTotal();
    }

    public void calTotal() {

        double n = 0.0;
        for (BillItem b : billItems) {
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(0 - n);
        // //////// // System.out.println("AAA : " + n);
    }

    public void calTotalBySelectedBillTems() {
        if (selectedBillItems == null) {
            getCurrent().setNetTotal(0);
            return;
        }

        double n = 0.0;
        for (BillItem b : selectedBillItems) {
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(0 - n);
        // //////// // System.out.println("AAA : " + n);
    }

    public void calTotalWithResetingIndex() {
        double n = 0.0;
        int index = 0;
        for (BillItem b : billItems) {
            b.setSearialNo(index++);
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(0 - n);
        // //////// // System.out.println("AAA : " + n);
    }

    public void removeAll() {
        for (BillItem b : selectedBillItems) {

            remove(b);
        }

        //   calTotalWithResetingIndex();
        selectedBillItems = null;
    }

    public void remove(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());
        calTotalWithResetingIndex();
    }

    public void removeWithoutIndex(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());

    }

    @Inject
    private PaymentSchemeController paymentSchemeController;

    private boolean errorCheck() {
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Select Cant settle without Dealor");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private void saveBill(BillType billType) {

        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.BilledBill, BillNumberSuffix.CRDPAY));
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.BilledBill, BillNumberSuffix.CRDPAY));

        getCurrent().setBillType(billType);

        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setNetTotal(getCurrent().getNetTotal());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

    }

    @Inject
    private BillBeanController billBean;
    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void settleBill() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.GrnPaymentPre);
        saveBillItem();

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        
        if(getCurrent().getPaymentMethod()==PaymentMethod.Cheque){
            
        }

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
        
        
    }

    public void settleBillAll() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        if (getSelectedBillItems() == null || getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("There is No Bills seected to settle");
            return;
        }

        calTotalBySelectedBillTems();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.GrnPaymentPre);
        Payment p = createPayment(getCurrent(), getCurrent().getPaymentMethod());
        saveBillItemBySelectedItems(p);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

        
    }

    private void saveBillItem() {
        for (BillItem tmp : getBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(0 - tmp.getNetValue());

            if (tmp.getId() == null) {
                getBillItemFacade().create(tmp);
            }

            updateReferenceBill(tmp);

        }

    }

    private void saveBillItemBySelectedItems() {
        for (BillItem tmp : getSelectedBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(0 - tmp.getNetValue());

            if (tmp.getId() == null) {
                getBillItemFacade().create(tmp);
            }

            updateReferenceBill(tmp);

        }

    }

    private void saveBillItemBySelectedItems(Payment p) {
        for (BillItem tmp : getSelectedBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(0 - tmp.getNetValue());

            if (tmp.getId() == null) {
                getBillItemFacade().create(tmp);
            }
            saveBillFee(tmp, p);
            updateReferenceBill(tmp);

        }

    }

    private boolean isPaidAmountOk(BillItem tmp) {

        double refBallance = getReferenceBallance(tmp);
        double netValue = Math.abs(tmp.getNetValue());

        //   ballance=refBallance-tmp.getNetValue();
        if (refBallance >= netValue) {
            return true;
        }

        if (netValue - refBallance < 0.1) {
            return true;
        }

        return false;
    }

    private void updateReferenceBill(BillItem tmp) {
        double dbl = getCreditBean().getPaidAmount(tmp.getReferenceBill(), BillType.GrnPaymentPre);

        tmp.getReferenceBill().setPaidAmount(0 - dbl);
        getBillFacade().edit(tmp.getReferenceBill());

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

    /**
     * Creates a new instance of pharmacyDealorBill
     */
    public PharmacyDealorBill() {
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
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

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    
}
