/*
 * To change this currentlate, choose Tools | currentlates
 * and open the currentlate in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.memberShip.PaymentSchemeController;
import com.divudi.bean.inward.AdmissionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.Admission;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class CashRecieveBillController implements Serializable {

    private Bill current;
    private boolean printPreview = false;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    private SessionController sessionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @Inject
    private BillController billController;
    private BillItem currentBillItem;
    private BillItem removingItem;
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    private PaymentMethodData paymentMethodData;
    private Institution institution;
    String comment;

    public void makeNull() {
        printPreview = false;
        currentBillItem = null;
        removingItem = null;
        billItems = null;
        paymentMethodData = null;
        institution = null;
    }

    public void selectInstitutionListener() {
        Institution ins = institution;
        makeNull();

        System.err.println("Select Listener");
        List<Bill> list = getBillController().getCreditBills(ins);
        System.err.println("Size " + list.size());
        for (Bill b : list) {
            getCurrentBillItem().setReferenceBill(b);
            selectBillListener();
            addToBill();
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Inject
    private AdmissionController admissionController;

    public void selectInstitutionListenerBht() {
        Institution ins = institution;
        makeNull();

        System.err.println("Select Listener");
        List<Admission> list = getAdmissionController().getCreditBillsBht(ins);
        System.err.println("Size " + list.size());
        for (PatientEncounter b : list) {
            getCurrentBillItem().setPatientEncounter(b);
            selectBhtListener();
            addToBht();
        }
    }

    public void changeNetValueListener(BillItem billItem) {

//        if (!isPaidAmountOk(billItem)) {
//            billItem.setNetValue(0);
////            UtilityController.addSuccessMessage("U cant add more than ballance");
////            return;
//        }
        calTotal();
    }

    public void changeNetValueListenerBht(BillItem billItem) {

//        if (!isBhtPaidAmountOk(billItem)) {
//            billItem.setNetValue(0);
////            UtilityController.addSuccessMessage("U cant add more than ballance");
////            return;
//        }
        calTotal();
    }

    private boolean isPaidAmountOk(BillItem tmp) {

        double refBallance = getReferenceBallance(tmp);
        double netValue = Math.abs(tmp.getNetValue());

        System.err.println("RefBallance " + refBallance);
        System.err.println("Net Value " + tmp.getNetValue());

        if (refBallance >= netValue) {
            System.err.println("1");
            return true;
        }

        if (netValue - refBallance < 0.1) {
            System.err.println("2");
            return true;
        }
        return false;
    }

    private boolean isBhtPaidAmountOk(BillItem tmp) {

        double refBallance = getReferenceBhtBallance(tmp);
        double netValue = Math.abs(tmp.getNetValue());

        System.err.println("RefBallance " + refBallance);
        System.err.println("Net Value " + tmp.getNetValue());

        if (refBallance >= netValue) {
            System.err.println("1");
            return true;
        }

        if (netValue - refBallance < 0.1) {
            System.err.println("2");
            return true;
        }
        return false;
    }

    private double getReferenceBallance(BillItem billItem) {
        double refBallance = 0;
        double neTotal = Math.abs(billItem.getReferenceBill().getNetTotal());
        double paidAmt = Math.abs(getCreditBean().getPaidAmount(billItem.getReferenceBill(), BillType.CashRecieveBill));

        refBallance = neTotal - (paidAmt);

        return refBallance;
    }

    private double getReferenceBhtBallance(BillItem billItem) {
        double refBallance = 0;
        double used = Math.abs(billItem.getPatientEncounter().getCreditUsedAmount());
        double paidAmt = Math.abs(getCreditBean().getPaidAmount(billItem.getPatientEncounter(), BillType.CashRecieveBill));

        refBallance = used - (paidAmt);

        return refBallance;
    }

    public void selectBillListener() {
        double dbl = getReferenceBallance(getCurrentBillItem());

        System.err.println("Ballance Amount " + dbl);
        if (dbl > 0.1) {
            getCurrentBillItem().setNetValue(dbl);
        }

    }

    public void selectBhtListener() {
        double dbl = getReferenceBhtBallance(getCurrentBillItem());

        System.err.println("Ballance Amount " + dbl);
        if (dbl > 0.1) {
            getCurrentBillItem().setNetValue(dbl);
        }

    }

    public void remove(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());
        calTotalWithResetingIndex();
    }

    private boolean errorCheckForAdding() {
        if (getCurrentBillItem().getReferenceBill().getCreditCompany() == null) {
            UtilityController.addErrorMessage("U cant add without credit company name");
            return true;
        }

//        double dbl = getReferenceBallance(getCurrentBillItem());
//
//        if (dbl < Math.abs(getCurrentBillItem().getNetValue())) {
//            UtilityController.addErrorMessage("U Cant Recieve Over Than Due");
//            return true;
//        }
        for (BillItem b : getBillItems()) {
            if (b.getReferenceBill() != null && b.getReferenceBill().getCreditCompany() != null) {
                if (!Objects.equals(getCurrentBillItem().getReferenceBill().getCreditCompany().getId(), b.getReferenceBill().getCreditCompany().getId())) {
                    UtilityController.addErrorMessage("U can add only one type Credit companies at Once");
                    return true;
                }
            }
        }

        return false;
    }

    private boolean errorCheckForAddingBht() {
        if (getCurrentBillItem().getPatientEncounter().getCreditCompany() == null) {
            UtilityController.addErrorMessage("U cant add without credit company name");
            return true;
        }

//        double dbl = getReferenceBhtBallance(getCurrentBillItem());
//
//        if (dbl < Math.abs(getCurrentBillItem().getNetValue())) {
//            UtilityController.addErrorMessage("U Cant Recieve Over Than Due");
//            return true;
//        }
        for (BillItem b : getBillItems()) {
            if (b.getPatientEncounter() != null && b.getPatientEncounter().getCreditCompany() != null) {
                if (!Objects.equals(getCurrentBillItem().getPatientEncounter().getCreditCompany().getId(), b.getPatientEncounter().getCreditCompany().getId())) {
                    UtilityController.addErrorMessage("U can add only one type Credit companies at Once");
                    return true;
                }
            }
        }

        return false;
    }

    public void calTotalWithResetingIndex() {
        double n = 0.0;
        int index = 0;
        for (BillItem b : billItems) {
            b.setSearialNo(index++);
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(n);
        // ////System.out.println("AAA : " + n);
    }

    public void addToBill() {
        if (errorCheckForAdding()) {
            return;
        }

        getCurrent().setFromInstitution(getCurrentBillItem().getReferenceBill().getCreditCompany());
        //     getCurrentBillItem().getBill().setNetTotal(getCurrentBillItem().getNetValue());
        //     getCurrentBillItem().getBill().setTotal(getCurrent().getNetTotal());

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getBillItems().add(getCurrentBillItem());

        currentBillItem = null;
        calTotal();

    }

    public void addToBht() {
        if (errorCheckForAddingBht()) {
            return;
        }

        getCurrent().setFromInstitution(getCurrentBillItem().getPatientEncounter().getCreditCompany());
        //     getCurrentBillItem().getBill().setNetTotal(getCurrentBillItem().getNetValue());
        //     getCurrentBillItem().getBill().setTotal(getCurrent().getNetTotal());

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getBillItems().add(getCurrentBillItem());

        currentBillItem = null;
        calTotal();

    }

    private List<Bill> creditBills;

    public void addAllBill() {
        for (Bill b : creditBills) {
            currentBillItem = null;
            getCurrentBillItem().setReferenceBill(b);
            addToBill();
        }
    }

    private void calTotal() {
        double n = 0.0;
        for (BillItem b : billItems) {
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(n);
        ////System.out.println("AAA : " + n);
    }

//    public double getDue() {
//        if (getPatientEncounter() == null) {
//            return 0.0;
//        }
//
//        String sql = "SELECT  sum(b.netTotal) FROM BilledBill b WHERE b.retired=false  and b.billType=com.divudi.data.BillType.InwardBill and b.cancelledBill is null and b.patientEncounter.id=" + getPatientEncounter().getId();
//        Double tmp = getBillFacade().findAggregateDbl(sql);
//
//        sql = "SELECT  sum(b.netTotal) FROM BilledBill b WHERE b.retired=false  and b.billType=com.divudi.data.BillType.InwardPaymentBill and b.cancelledBill is null and b.patientEncounter.id=" + getPatientEncounter().getId();
//        tmp = tmp - getBillFacade().findAggregateDbl(sql);
//
//        return tmp;
//    }
    public CashRecieveBillController() {
    }

    @Inject
    private PaymentSchemeController paymentSchemeController;

    private boolean errorCheck() {
        if (getBillItems().isEmpty()) {
            UtilityController.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getFromInstitution() == null) {
            UtilityController.addErrorMessage("Select Credit Company");
            return true;
        }

        if (!Objects.equals(getBillItems().get(0).getReferenceBill().getCreditCompany().getId(), getCurrent().getFromInstitution().getId())) {
            UtilityController.addErrorMessage("Select same credit company as BillItem ");
            return true;
        }

//        if (getCurrent().getPaymentScheme() == null) {
//            return true;
//        }
        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private boolean errorCheckBht() {
        if (getBillItems().isEmpty()) {
            UtilityController.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getFromInstitution() == null) {
            UtilityController.addErrorMessage("Select Credit Company");
            return true;
        }

        if (!Objects.equals(getBillItems().get(0).getPatientEncounter().getCreditCompany().getId(), getCurrent().getFromInstitution().getId())) {
            UtilityController.addErrorMessage("Select same credit company as BillItem ");
            return true;
        }

//        if (getCurrent().getPaymentScheme() == null) {
//            return true;
//        }
        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
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

        getCurrent().setComments(comment);

        getCurrent().setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getCurrent().setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());

        getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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

        if (errorCheck()) {
            return;
        }

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.CashRecieveBill);
        saveBillItem();

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        //   savePayments();
        UtilityController.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleBillBht() {

        if (errorCheckBht()) {
            return;
        }

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.CashRecieveBill);
        saveBillItemBht();

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        //   savePayments();
        UtilityController.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    private void savePayments() {
        for (BillItem b : getBillItems()) {
            Bill bil = saveBhtPaymentBill(b);
            saveBhtBillItem(bil);
        }
    }

    private Bill saveBhtPaymentBill(BillItem b) {
        Bill tmp = new BilledBill();

        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), tmp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPAY));

        tmp.setBillType(BillType.InwardPaymentBill);
        tmp.setPatientEncounter(b.getPatientEncounter());
        tmp.setPatient(b.getPatientEncounter().getPatient());
        tmp.setPaymentScheme(getCurrent().getPaymentScheme());
        tmp.setPaymentMethod(getCurrent().getPaymentMethod());
        tmp.setInstitution(sessionController.getInstitution());
        tmp.setDepartment(sessionController.getDepartment());
        tmp.setNetTotal(b.getNetValue());
        tmp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        tmp.setCreater(getSessionController().getLoggedUser());

//        getBillNumberBean().departmentBillNumberGenerator(tmp, BillClassType.BilledBill, BillNumberSuffix.NONE);
//        getBillNumberBean().institutionBillNumberGenerator(tmp, BillClassType.BilledBill, BillNumberSuffix.INWPAY);

        getBillFacade().create(tmp);

        return tmp;
    }

    public void removeAll() {
        for (BillItem b : selectedBillItems) {
            remove(b);
        }

        //  calTotalWithResetingIndex();
        selectedBillItems = null;
    }

    private void saveBhtBillItem(Bill b) {
        BillItem temBi = new BillItem();
        temBi.setBill(b);
        temBi.setNetValue(b.getNetTotal());
        temBi.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temBi.setCreater(getSessionController().getLoggedUser());
        getBillItemFacade().create(temBi);
    }

    private void saveBillItem() {
        for (BillItem tmp : getBillItems()) {
            tmp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(tmp.getNetValue());
            getBillItemFacade().create(tmp);

            updateReferenceBill(tmp);

        }
    }

    private void saveBillItemBht() {
        for (BillItem tmp : getBillItems()) {
            tmp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(tmp.getNetValue());
            getBillItemFacade().create(tmp);

            updateReferenceBht(tmp);

        }

    }

    @EJB
    private CreditBean creditBean;

    private void updateReferenceBill(BillItem tmp) {
        double dbl = getCreditBean().getPaidAmount(tmp.getReferenceBill(), BillType.CashRecieveBill);

        tmp.getReferenceBill().setPaidAmount(0 - dbl);
        getBillFacade().edit(tmp.getReferenceBill());

    }

    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    private void updateReferenceBht(BillItem tmp) {
        double dbl = getCreditBean().getPaidAmount(tmp.getPatientEncounter(), BillType.CashRecieveBill);

        tmp.getPatientEncounter().setCreditPaidAmount(0 - dbl);
        getPatientEncounterFacade().edit(tmp.getPatientEncounter());
    }

//    private void updateReferenceBill(BillItem tmp) {
//        double ballance, refBallance = 0;
//
//        //System.err.println("Paid Amount " + tmp.getReferenceBill().getPaidAmount());
//        //System.err.println("Net Total " + tmp.getReferenceBill().getNetTotal());
//        //System.err.println("Net Value " + tmp.getNetValue());
//        refBallance = tmp.getReferenceBill().getNetTotal() - tmp.getReferenceBill().getPaidAmount();
//
//        //System.err.println("refBallance " + refBallance);
//        //   ballance=refBallance-tmp.getNetValue();
//        if (refBallance > tmp.getNetValue()) {
//            tmp.getReferenceBill().setPaidAmount(tmp.getReferenceBill().getPaidAmount() + tmp.getNetValue());
//        } else {
//            tmp.getReferenceBill().setPaidAmount(refBallance - tmp.getNetValue());
//        }
//        //System.err.println("Updated " + tmp.getReferenceBill().getPaidAmount());
//
////        if (tmp.getReferenceBill().getPaidAmount() != 0.0) {
////            tmp.getReferenceBill().setPaidAmount(tmp.getReferenceBill().getPaidAmount() + tmp.getNetValue());
////        } else {
////            tmp.getReferenceBill().setPaidAmount(tmp.getNetValue());
////        }
//        getBillFacade().edit(tmp.getReferenceBill());
//
//    }
    public void recreateModel() {
        current = null;
        printPreview = false;
        currentBillItem = null;
        paymentMethodData = null;
        billItems = null;
        selectedBillItems = null;
        comment = null;
    }

    public String prepareNewBill() {
        recreateModel();
        return "";
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

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
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

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            //  currentBillItem.setBill(new );
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

    public BillItem getRemovingItem() {
        return removingItem;
    }

    public void setRemovingItem(BillItem removingItem) {
        this.removingItem = removingItem;
    }

    public List<Bill> getCreditBills() {
        return creditBills;
    }

    public void setCreditBills(List<Bill> creditBills) {
        recreateModel();
        this.creditBills = creditBills;
        addAllBill();
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

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
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

    public List<BillItem> getSelectedBillItems() {
        if (selectedBillItems == null) {
            selectedBillItems = new ArrayList<>();
        }
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public AdmissionController getAdmissionController() {
        return admissionController;
    }

    public void setAdmissionController(AdmissionController admissionController) {
        this.admissionController = admissionController;
    }
}
