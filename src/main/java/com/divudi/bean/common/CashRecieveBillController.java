/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.inward.AdmissionController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CreditBean;
import com.divudi.service.StaffService;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class CashRecieveBillController implements Serializable {

    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StaffService staffBean;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private CreditBean creditBean;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    PaymentService paymentService;

    @Inject
    private BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    private BillController billController;
    @Inject
    private CashBookEntryController cashBookEntryController;
    @Inject
    private DrawerController drawerController;
    @Inject
    WebUserController webUserController;

    private Bill current;
    private boolean printPreview = false;
    private BillItem currentBillItem;
    private BillItem removingItem;
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    private Bill selectedBill;
    private PaymentMethodData paymentMethodData;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private String comment;

    public void makeNull() {
        printPreview = false;
        currentBillItem = null;
        removingItem = null;
        billItems = null;
        selectedBillItems = null;
        paymentMethodData = null;
        institution = null;
        recreateModel();
    }

    public void selectInstitutionListener() {
        Institution ins = institution;
        makeNull();

        List<Bill> list = getBillController().getCreditBills(ins);
        for (Bill b : list) {
            getCurrentBillItem().setReferenceBill(b);
            selectBillListener();
            if (getCurrentBillItem().getNetValue() == 0.0) {
                continue;

            }
            addToBill();

        }
//        if (billItems != null) {
//            selectedBillItems.addAll(billItems);
//        }
        calTotal();
    }

    public void selectVoucherListener() {
        Institution ins = selectedBill.getCreditCompany();
        current = selectedBill;
        makeNull();

        List<Bill> list = getBillController().getCreditBills(ins);
        for (Bill b : list) {
            getCurrentBillItem().setReferenceBill(b);
            selectBillListener();
            if (getCurrentBillItem().getNetValue() == 0.0) {
                continue;
            }
            addToBillForVoucher();
        }
//        if (billItems != null) {
//            selectedBillItems.addAll(billItems);
//        }
        calTotalForVoucher();
    }

    public void selectInstitutionListenerPharmacy() {
        Institution ins = institution;
        makeNull();

        List<Bill> list = getBillController().getCreditBillsPharmacy(ins);
        for (Bill b : list) {
            getCurrentBillItem().setReferenceBill(b);
            selectBillListener();
            addToBillPharmacy();
        }
//        if (billItems != null) {
//            selectedBillItems.addAll(billItems);
//        }
        calTotal();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Inject
    private AdmissionController admissionController;

//    public void selectInstitutionListenerBht() {
//        Institution ins = institution;
//        makeNull();
//
//        List<Admission> list = getAdmissionController().getCreditBillsBht(ins);
//        for (PatientEncounter b : list) {
//            getCurrentBillItem().setPatientEncounter(b);
//            selectBhtListener();
//            addToBht();
//        }
//        if (billItems != null) {
//            selectedBillItems.addAll(billItems);
//        }
//        calTotal();
//    }

    public void selectInstitutionListenerBht() {
        Institution ins = institution;
        makeNull();

        List<Bill> list = getAdmissionController().getCreditPaymentBillsBht(ins);
        for (Bill b : list) {
            getCurrentBillItem().setPatientEncounter(b.getPatientEncounter());
            getCurrentBillItem().setNetValue(b.getNetTotal());
            getCurrentBillItem().getPatientEncounter().setCreditCompany(b.getCreditCompany());
            getCurrentBillItem().setBill(b);
//            selectBhtListener();
            addToBht();
        }
        if (billItems != null) {
            selectedBillItems.addAll(billItems);
        }
        calTotal();
    }

    public void changeNetValueListener(BillItem billItem) {

//        if (!isPaidAmountOk(billItem)) {
//            billItem.setNetValue(0);
////            JsfUtil.addSuccessMessage("U cant add more than ballance");
////            return;
//        }
        calTotal();
    }

    public void changeNetValueListenerForVoucher(BillItem billItem) {

//        if (!isPaidAmountOk(billItem)) {
//            billItem.setNetValue(0);
////            JsfUtil.addSuccessMessage("U cant add more than ballance");
////            return;
//        }
        calTotalForVoucher();
    }

    public void changeNetValueListenerBht(BillItem billItem) {

//        if (!isBhtPaidAmountOk(billItem)) {
//            billItem.setNetValue(0);
////            JsfUtil.addSuccessMessage("U cant add more than ballance");
////            return;
//        }
        calTotal();
    }

    private boolean isPaidAmountOk(BillItem tmp) {

        double refBallance = getReferenceBallance(tmp);
        double netValue = Math.abs(tmp.getNetValue());

        if (refBallance >= netValue) {
            return true;
        }

        if (netValue - refBallance < 0.1) {
            return true;
        }
        return false;
    }

    private boolean isBhtPaidAmountOk(BillItem tmp) {

        double refBallance = getReferenceBhtBallance(tmp);
        double netValue = Math.abs(tmp.getNetValue());

        if (refBallance >= netValue) {
            return true;
        }

        if (netValue - refBallance < 0.1) {
            return true;
        }
        return false;
    }

    private double getReferenceBallance(BillItem billItem) {
        double refBallance = 0;
        double neTotal = Math.abs(billItem.getReferenceBill().getNetTotal() + billItem.getReferenceBill().getVat());
        double refAmount = Math.abs(getCreditBean().getRefundAmount(billItem.getReferenceBill()));
        double paidAmt = Math.abs(getCreditBean().getTotalCreditSettledAmount(billItem.getReferenceBill()));
        refBallance = neTotal - (paidAmt + refAmount);
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
        System.out.println(getCurrentBillItem().getReferenceBill());

        if (dbl > 0.1) {
            getCurrentBillItem().setNetValue(dbl);
        }

    }

    public void selectBhtListener() {
        double dbl = getReferenceBhtBallance(getCurrentBillItem());

        if (dbl > 0.1) {
            getCurrentBillItem().setNetValue(dbl);
        }

    }

    public void remove(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());
        getSelectedBillItems().remove(billItem.getSearialNo());
        calTotalWithResetingIndex();
    }

    private boolean errorCheckForAdding() {
        if (getCurrentBillItem().getReferenceBill().getCreditCompany() == null) {
            JsfUtil.addErrorMessage("U cant add without credit company name");
            return true;
        }

        for (BillItem b : getBillItems()) {
            if (b.getReferenceBill().getId() == getCurrentBillItem().getReferenceBill().getId()) {
                JsfUtil.addErrorMessage("This bill you already added.");
                return true;
            }

        }

        if (getCurrentBillItem().getNetValue() > getCurrentBillItem().getReferenceBill().getNetTotal()) {
            JsfUtil.addErrorMessage("Amount is more than the required bill value.");
            return true;
        }

//        double dbl = getReferenceBallance(getCurrentBillItem());
//
//        if (dbl < Math.abs(getCurrentBillItem().getNetValue())) {
//            JsfUtil.addErrorMessage("U Cant Recieve Over Than Due");
//            return true;
//        }
        for (BillItem b : getBillItems()) {
            if (b.getReferenceBill() != null && b.getReferenceBill().getCreditCompany() != null) {
                if (!Objects.equals(getCurrentBillItem().getReferenceBill().getCreditCompany().getId(), b.getReferenceBill().getCreditCompany().getId())) {
                    JsfUtil.addErrorMessage("U can add only one type Credit companies at Once");
                    return true;
                }
            }
        }

        return false;
    }

    private boolean errorCheckForAddingPharmacy() {
        if (getCurrentBillItem().getReferenceBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("U cant add without credit company name");
            return true;
        }

//        double dbl = getReferenceBallance(getCurrentBillItem());
//
//        if (dbl < Math.abs(getCurrentBillItem().getNetValue())) {
//            JsfUtil.addErrorMessage("U Cant Recieve Over Than Due");
//            return true;
//        }
        for (BillItem b : getBillItems()) {
            if (b.getReferenceBill() != null && b.getReferenceBill().getToInstitution() != null) {
                if (!Objects.equals(getCurrentBillItem().getReferenceBill().getToInstitution().getId(), b.getReferenceBill().getToInstitution().getId())) {
                    JsfUtil.addErrorMessage("U can add only one type Credit companies at Once");
                    return true;
                }
            }
            if (b.getReferenceBill() != null) {
                if (Objects.equals(getCurrentBillItem().getReferenceBill().getId(), b.getReferenceBill().getId())) {
                    JsfUtil.addErrorMessage("U can add only one Bill at Once");
                    return true;
                }
            }
        }

        return false;
    }

    private boolean errorCheckForAddingBht() {
        if (getCurrentBillItem().getPatientEncounter().getCreditCompany() == null) {
            JsfUtil.addErrorMessage("U cant add without credit company name");
            return true;
        }

//        double dbl = getReferenceBhtBallance(getCurrentBillItem());
//
//        if (dbl < Math.abs(getCurrentBillItem().getNetValue())) {
//            JsfUtil.addErrorMessage("U Cant Recieve Over Than Due");
//            return true;
//        }
        for (BillItem b : getBillItems()) {
            if (b.getPatientEncounter() != null && b.getPatientEncounter().getCreditCompany() != null) {
                if (!Objects.equals(getCurrentBillItem().getPatientEncounter().getCreditCompany().getId(), b.getPatientEncounter().getCreditCompany().getId())) {
                    JsfUtil.addErrorMessage("U can add only one type Credit companies at Once");
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
        n = 0.0;
        index = 0;
        for (BillItem b : selectedBillItems) {
            b.setSearialNo(index++);
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(n);
        // //////// // System.out.println("AAA : " + n);
    }

    public void addToBill() {
        if (errorCheckForAdding()) {
            return;
        }
        if (getCurrent().getCreditCompany() == null){
            getCurrent().setCreditCompany(getCurrentBillItem().getReferenceBill().getCreditCompany());
        }
        getCurrent().setFromInstitution(getCurrentBillItem().getReferenceBill().getCreditCompany());
        getCurrentBillItem().setBill(getCurrentBillItem().getReferenceBill());
        getCurrentBillItem().setPatientEncounter(getCurrentBillItem().getReferenceBill().getPatientEncounter());
        getCurrentBillItem().setSearialNo(getBillItems().size());
        getSelectedBillItems().add(getCurrentBillItem());
        getBillItems().add(getCurrentBillItem());
        currentBillItem = null;
        calTotal();
    }

    public void addToBillForVoucher() {
        if (errorCheckForAdding()) {
            return;
        }

        getCurrent().setFromInstitution(getCurrentBillItem().getReferenceBill().getCreditCompany());
        //     getCurrentBillItem().getBill().setNetTotal(getCurrentBillItem().getNetValue());
        //     getCurrentBillItem().getBill().setTotal(getCurrent().getNetTotal());

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getSelectedBillItems().add(getCurrentBillItem());
        getBillItems().add(getCurrentBillItem());

        currentBillItem = null;
        calTotalForVoucher();

    }

    public void addToBillPharmacy() {
        if (errorCheckForAddingPharmacy()) {
            return;
        }

        getCurrent().setFromInstitution(getCurrentBillItem().getReferenceBill().getToInstitution());
        //     getCurrentBillItem().getBill().setNetTotal(getCurrentBillItem().getNetValue());
        //     getCurrentBillItem().getBill().setTotal(getCurrent().getNetTotal());

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getSelectedBillItems().add(getCurrentBillItem());
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
        getSelectedBillItems().add(getCurrentBillItem());

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

    public void calTotal() {
        double n = 0.0;
//        //// // System.out.println("getBillItems().size() = " + getBillItems().size());
//        //// // System.out.println("getSelectedBillItems().size() = " + getSelectedBillItems().size());
        for (BillItem b : selectedBillItems) {
//            //// // System.out.println("b.getNetValue() = " + b.getNetValue());
//            //// // System.out.println("b.getSearialNo() = " + b.getSearialNo());
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(n);
        //////// // System.out.println("AAA : " + n);
    }

    public void calulateTotalForSettlingCreditForOpdPackageBills() {
        double n = 0.0;
        for (BillItem b : selectedBillItems) {
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(n);
    }

    public void calulateTotalForSettlingCreditForOpdBatchBills() {
        double n = 0.0;
        for (BillItem b : selectedBillItems) {
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(n);
    }

    public void calulateTotalForSettlingCreditForInwardCreditCompanyPaymentBills() {
        double n = 0.0;
        for (BillItem b : selectedBillItems) {
            n += b.getNetValue();
        }
        getCurrent().setNetTotal(n);
    }

    public void calTotalForVoucher() {
        double n = 0.0;
//        //// // System.out.println("getBillItems().size() = " + getBillItems().size());
//        //// // System.out.println("getSelectedBillItems().size() = " + getSelectedBillItems().size());
        for (BillItem b : selectedBillItems) {
//            //// // System.out.println("b.getNetValue() = " + b.getNetValue());
//            //// // System.out.println("b.getSearialNo() = " + b.getSearialNo());
            n += b.getNetValue();
        }
        getCurrent().setTotal(n);
        //////// // System.out.println("AAA : " + n);
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
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Credit Company");
            return true;
        }

        if (!Objects.equals(getBillItems().get(0).getReferenceBill().getCreditCompany().getId(), getCurrent().getFromInstitution().getId())) {
            JsfUtil.addErrorMessage("Select same credit company as BillItem ");
            return true;
        }

//        if (getCurrent().getPaymentScheme() == null) {
//            return true;
//        }
        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private boolean errorCheckPharmacy() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Credit Company");
            return true;
        }

        if (!Objects.equals(getBillItems().get(0).getReferenceBill().getToInstitution().getId(), getCurrent().getFromInstitution().getId())) {
            JsfUtil.addErrorMessage("Select same credit company as BillItem ");
            return true;
        }

//        if (getCurrent().getPaymentScheme() == null) {
//            return true;
//        }
        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private boolean errorCheckBht() {
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return true;
        }

        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Selected Bill Item ");
            return true;
        }

        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Credit Company");
            return true;
        }

        if (!Objects.equals(getBillItems().get(0).getPatientEncounter().getCreditCompany().getId(), getCurrent().getFromInstitution().getId())) {
            JsfUtil.addErrorMessage("Select same credit company as BillItem ");
            return true;
        }

//        if (getCurrent().getPaymentScheme() == null) {
//            return true;
//        }
        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

        return false;
    }

    private void saveBill(BillType billType, BillTypeAtomic billTypeAtomic) {
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), billTypeAtomic);
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(billType);
        getCurrent().setBillTypeAtomic(billTypeAtomic);
        getCurrent().setCreditCompany(institution);
        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setComments(comment);
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

    private void saveCancelBill(BillType billType, BillTypeAtomic billTypeAtomic, Bill b) {

        b.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.CancelledBill, BillNumberSuffix.CRDCAN));
        b.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.CancelledBill, BillNumberSuffix.CRDCAN));

        b.setBillType(billType);
        b.setBillTypeAtomic(billTypeAtomic);

        b.setDepartment(getSessionController().getLoggedUser().getDepartment());
        b.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        b.setComments(comment);

        b.setBillDate(new Date());
        b.setBillTime(new Date());

        b.setCreatedAt(new Date());
        b.setCreater(getSessionController().getLoggedUser());

        b.setNetTotal(b.getNetTotal());

        if (b.getId() == null) {
            getBillFacade().create(b);
        } else {
            getBillFacade().edit(b);
        }

    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void settleCreditForOpdPackageBills() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return;
        }
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Credit Company");
            return;
        }
        for (BillItem item : getBillItems()) {
            if (!Objects.equals(item.getReferenceBill().getCreditCompany().getId(), getCurrent().getFromInstitution().getId())) {
                JsfUtil.addErrorMessage("All Bills Settling Should be from a one single company.");
                return;
            }
        }
        if (getCurrent().getPaymentMethod() == null) {
            return;
        }
        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return;
        }
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        calulateTotalForSettlingCreditForOpdPackageBills();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.CashRecieveBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setComments(comment);
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

        for (BillItem savingBillItem : getBillItems()) {
            savingBillItem.setCreatedAt(new Date());
            savingBillItem.setCreater(getSessionController().getLoggedUser());
            savingBillItem.setBill(getCurrent());
            savingBillItem.setGrossValue(savingBillItem.getNetValue());
            getCurrent().getBillItems().add(savingBillItem);
            if (savingBillItem.getId() == null) {
                getBillItemFacade().create(savingBillItem);
            } else {
                getBillItemFacade().edit(savingBillItem);
            }
            updateSettlingCreditBillSettledValues(savingBillItem);
        }
        paymentService.createPayment(current, paymentMethodData);
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
    }

    public void settleCreditForOpdBatchBills() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return;
        }
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Credit Company");
            return;
        }
        for (BillItem item : getBillItems()) {
            if (!Objects.equals(item.getReferenceBill().getCreditCompany().getId(), getCurrent().getFromInstitution().getId())) {
                JsfUtil.addErrorMessage("All Bills Settling Should be from a one single company.");
                return;
            }
        }
        if (getCurrent().getPaymentMethod() == null) {
            return;
        }
        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return;
        }
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        calulateTotalForSettlingCreditForOpdBatchBills();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.CashRecieveBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setComments(comment);
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

        for (BillItem savingBillItem : getBillItems()) {
            savingBillItem.setCreatedAt(new Date());
            savingBillItem.setCreater(getSessionController().getLoggedUser());
            savingBillItem.setBill(getCurrent());
            savingBillItem.setGrossValue(savingBillItem.getNetValue());
            getCurrent().getBillItems().add(savingBillItem);
            if (savingBillItem.getId() == null) {
                getBillItemFacade().create(savingBillItem);
            } else {
                getBillItemFacade().edit(savingBillItem);
            }
            updateSettlingCreditBillSettledValues(savingBillItem);
        }
        paymentService.createPayment(current, getPaymentMethodData());
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }
        calTotal();

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        System.out.println(getSelectedBillItems());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.CashRecieveBill, BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        saveBillItem();

        List payments = createPayment(current, current.getPaymentMethod());
        drawerController.updateDrawerForIns(payments);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleCreditForInwardCreditCompanyPaymentBills() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return;
        }
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Credit Company");
            return;
        }
        for (BillItem item : getBillItems()) {
            if (!Objects.equals(item.getReferenceBill().getCreditCompany().getId(), getCurrent().getFromInstitution().getId())) {
                JsfUtil.addErrorMessage("All Bills Settling Should be from a one single company.");
                return;
            }
        }
        if (getCurrent().getPaymentMethod() == null) {
            return;
        }
        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), getPaymentMethodData())) {
            return;
        }
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
        calulateTotalForSettlingCreditForInwardCreditCompanyPaymentBills();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.CashRecieveBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setComments(comment);
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

       updateReferanceBills();

        for (BillItem savingBillItem : getBillItems()) {
            savingBillItem.setCreatedAt(new Date());
            savingBillItem.setCreater(getSessionController().getLoggedUser());
            savingBillItem.setBill(getCurrent());
            savingBillItem.setGrossValue(savingBillItem.getNetValue());
            getCurrent().getBillItems().add(savingBillItem);
            if (savingBillItem.getId() == null) {
                getBillItemFacade().create(savingBillItem);
            } else {
                getBillItemFacade().edit(savingBillItem);
            }
            getBillBean().updateInwardDipositList(savingBillItem.getPatientEncounter(), getCurrent());
            updateReferenceBht(savingBillItem);
        }
        paymentService.createPayment(current, getPaymentMethodData());
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
    }

    public void settleBillViaVoucher() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        current = selectedBill;

        if (errorCheck()) {
            return;
        }

        calTotalForVoucher();

        if (getCurrent().getTotal() > getCurrent().getBalance()) {
            JsfUtil.addErrorMessage("Bills Total is More Than Voucher");
            return;
        }

        getCurrent().setBalance(getCurrent().getBalance() - getCurrent().getTotal());
        getCurrent().setTotal(getCurrent().getNetTotal());
        saveBillItem();
        billFacade.edit(current);

//        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
//        getSessionController().setLoggedUser(wb);
        //   savePayments();
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleBillForApproval() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

//        if (errorCheck()) {
//            return;
//        }
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Error : No Bill");
            return;
        }

        //calTotal();
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        System.out.println(getSelectedBillItems());

        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setBalance(getCurrent().getNetTotal());
        getCurrent().setCreditCompany(institution);

        saveBill(BillType.CashRecieveBill, BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        //saveBillItem();

        List payments = createPayment(current, current.getPaymentMethod());
        drawerController.updateDrawerForIns(payments);
        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        //   savePayments();
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public String navigateToCancelOpdBill() {
        return "/credit/views/bill_cancel?faces-redirect=true";
    }

    public void cancelBillToApprove(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("Error : No Bill");
            return;
        }
        if (b.getBalance() != b.getNetTotal()) {
            JsfUtil.addErrorMessage("Error : Payments Have Been Made By This Voucher");
            return;
        }

        CancelledBill cb = new CancelledBill();
        cb.copy(b);
        cb.setPaymentMethod(paymentMethod);
        saveCancelBill(BillType.CashRecieveBill, BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION, cb);
        b.setCancelledBill(cb);
        b.setCancelled(true);
        cb.setReferenceBill(b);
        List payments = createPayment(cb, paymentMethod);
        drawerController.updateDrawerForOuts(payments);
        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        //   savePayments();
        getBillFacade().edit(b);
        JsfUtil.addSuccessMessage("Bill Canceled");
        printPreview = true;
    }

    public void approveBill(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No Bill");
        }
        b.setApproveAt(new Date());
        b.setApproveUser(sessionController.getLoggedUser());
        billFacade.edit(b);
        JsfUtil.addSuccessMessage("Approved");
    }

    public boolean checkForExpireofApproval(Bill b) {
        if (webUserController.hasPrivilege("PettyCashBillApprove")) {
            return false;
        } else {
            if (b == null || b.getId() == null) {
                return true;
            }
            Date now = new Date();
            long differenceInMillis = now.getTime() - b.getCreatedAt().getTime();

            // Check if the difference is more than one day (24 hours in milliseconds)
            long oneDayInMillis = 24 * 60 * 60 * 1000;
            if (differenceInMillis > oneDayInMillis) {
                return true;
            } else {
                return false;
            }
        }
    }

    public List<Bill> fetchCreditCompanyVouchers() {
        List<BillTypeAtomic> billTypes = new ArrayList<>();
        billTypes.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        String sql = "SELECT b FROM Bill b "
                + "WHERE b.billTypeAtomic IN :billTypes "
                + "AND b.approveAt IS NOT NULL "
                + "AND b.retired = false "
                + "AND b.balance > 0 "
                + "ORDER BY b.id DESC";
        System.out.println("sql = " + sql);
        Map<String, Object> temMap = new HashMap<>();
        temMap.put("billTypes", billTypes);
        List<Bill> bills = getBillFacade().findByJpql(sql, temMap);
        return bills;
    }

    public void settleBillPharmacy() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheckPharmacy()) {
            return;
        }

        calTotal();

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.CashRecieveBill, BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_RECEIVED);
        saveBillItem();
        //   savePayments();
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleBillBht() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheckBht()) {
            return;
        }

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(BillType.CashRecieveBill, BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
        updateReferanceBills();
        saveBillItemBht();


        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        //   savePayments();
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void updateReferanceBills(){
        for(BillItem b : getBillItems()){
            b.getReferenceBill().setPaid(true);
            b.getReferenceBill().setPaidAmount(b.getReferenceBill().getPaidAmount() + b.getNetValue());
            b.getReferenceBill().setPaidBill(getCurrent());
            billFacade.edit(b.getReferenceBill());
        }
    }

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:
                        break;
                    case Agent:
                        break;
                    case Credit:
                        break;
                    case PatientDeposit:
                        break;
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        break;
                    case OnCall:
                        break;
                    case OnlineSettlement:
                        break;
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                        break;
                    case YouOweMe:
                        break;
                    case MultiplePaymentMethods:
                        break;
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(sessionController.getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    break;
                case Cash:
                    break;
                case ewallet:
                    break;
                case Agent:
                    break;
                case Credit:
                    break;
                case PatientDeposit:
                    break;
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                case OnCall:
                    break;
                case OnlineSettlement:
                    break;
                case Staff:
                    break;
                case YouOweMe:
                    break;
                case MultiplePaymentMethods:
                    break;
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);
            cashBookEntryController.writeCashBookEntryAtPaymentCreation(p);
            ps.add(p);
        }
        return ps;
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
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());

//        getBillNumberBean().departmentBillNumberGenerator(tmp, BillClassType.BilledBill, BillNumberSuffix.NONE);
//        getBillNumberBean().institutionBillNumberGenerator(tmp, BillClassType.BilledBill, BillNumberSuffix.INWPAY);
        getBillFacade().create(tmp);

        return tmp;
    }

    public void removeAll() {
        List<BillItem> tmp = new ArrayList<>();
        for (BillItem b : selectedBillItems) {
            tmp.add(getBillItems().get(b.getSearialNo()));
        }
        billItems.removeAll(tmp);
        for (BillItem b : billItems) {
//            getBillItems().remove(b.getSearialNo());

        }
        calTotalWithResetingIndex();

        selectedBillItems = new ArrayList<>();
        selectedBillItems.addAll(billItems);
    }

    private void saveBhtBillItem(Bill b) {
        BillItem temBi = new BillItem();
        temBi.setBill(b);
        temBi.setNetValue(b.getNetTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(getSessionController().getLoggedUser());
        getBillItemFacade().create(temBi);
    }

    private void saveBillItem() {
        for (BillItem savingBillItem : getSelectedBillItems()) {
            savingBillItem.setCreatedAt(new Date());
            savingBillItem.setCreater(getSessionController().getLoggedUser());
            savingBillItem.setBill(getCurrent());
            savingBillItem.setGrossValue(savingBillItem.getNetValue());
            getCurrent().getBillItems().add(savingBillItem);
            if (savingBillItem.getId() == null) {
                getBillItemFacade().create(savingBillItem);
            } else {
                getBillItemFacade().edit(savingBillItem);
            }
            updateReferenceBill(savingBillItem);
        }
    }

    private void saveBillItemForSponser(BillTypeAtomic bta) {
        for (BillItem savingBillItem : getSelectedBillItems()) {
            savingBillItem.setCreatedAt(new Date());
            savingBillItem.setCreater(getSessionController().getLoggedUser());
            savingBillItem.setBill(getCurrent());
            savingBillItem.setGrossValue(savingBillItem.getNetValue());
            getCurrent().getBillItems().add(savingBillItem);
            if (savingBillItem.getId() == null) {
                getBillItemFacade().create(savingBillItem);
            } else {
                getBillItemFacade().edit(savingBillItem);
            }
            updateSettlingCreditBillSettledValues(savingBillItem);
        }
    }

    private void saveBillItemBht() {
        for (BillItem tmp : getSelectedBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(tmp.getNetValue());
            getCurrent().getBillItems().add(tmp);
            getBillItemFacade().create(tmp);

            getBillBean().updateInwardDipositList(tmp.getPatientEncounter(), getCurrent());
            updateReferenceBht(tmp);

        }

    }

    private void updateReferenceBill(BillItem tmp) {
        double dbl = getCreditBean().getPaidAmount(tmp.getReferenceBill(), BillType.CashRecieveBill);
        tmp.getReferenceBill().setPaidAmount(0 - dbl);
        getBillFacade().edit(tmp.getReferenceBill());
    }

    private void updateSettlingCreditBillSettledValues(BillItem billItemWithReferanceToCreditBill) {
        System.out.println("Starting updateSettlingCreditBillSettledValues");
        System.out.println("Bill Item Reference: " + billItemWithReferanceToCreditBill);

        double settledCreditValueByCompanies = getCreditBean().getSettledAmountByCompany(billItemWithReferanceToCreditBill.getReferenceBill());
        System.out.println("Settled Credit Value By Companies: " + settledCreditValueByCompanies);

        double settledCreditValueByPatient = getCreditBean().getSettledAmountByPatient(billItemWithReferanceToCreditBill.getReferenceBill());
        System.out.println("Settled Credit Value By Patient: " + settledCreditValueByPatient);

        double settleCreditValueTotal = settledCreditValueByCompanies + settledCreditValueByPatient;
        System.out.println("Total Settled Credit Value: " + settleCreditValueTotal);

        billItemWithReferanceToCreditBill.getReferenceBill().setPaidAmount(settleCreditValueTotal);
        System.out.println("Paid Amount Set: " + settleCreditValueTotal);

        billItemWithReferanceToCreditBill.getReferenceBill().setSettledAmountByPatient(settledCreditValueByPatient);
        System.out.println("Settled Amount By Patient Set: " + settledCreditValueByPatient);

        billItemWithReferanceToCreditBill.getReferenceBill().setSettledAmountBySponsor(settledCreditValueByCompanies);
        System.out.println("Settled Amount By Sponsor Set: " + settledCreditValueByCompanies);

        double absBillAmount = Math.abs(billItemWithReferanceToCreditBill.getReferenceBill().getNetTotal());
        double absSettledAmount = Math.abs(billItemWithReferanceToCreditBill.getReferenceBill().getPaidAmount());
        double difference = absBillAmount - absSettledAmount;
        double absDifference = Math.abs(difference);
        if (absDifference < 1.0) {
            billItemWithReferanceToCreditBill.getReferenceBill().setPaidAt(new Date());
        }

        getBillFacade().edit(billItemWithReferanceToCreditBill.getReferenceBill());
        System.out.println("Reference Bill Updated: " + billItemWithReferanceToCreditBill.getReferenceBill());
        System.out.println("Completed updateSettlingCreditBillSettledValues");
    }

    private void updateReferenceBht(BillItem tmp) {
        double dbl = getCreditBean().getPaidAmount(tmp.getPatientEncounter(), BillType.CashRecieveBill);

        tmp.getReferenceBill().getPatientEncounter().setCreditPaidAmount(0 - dbl);
        getPatientEncounterFacade().edit(tmp.getReferenceBill().getPatientEncounter());
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
        institution = null;
        comment = null;
        selectedBill = null;
    }

    public String prepareNewBill() {
        recreateModel();
        return "";
    }

    public String navigateToCancelCreditSettleBill() {
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

    public Bill getSelectedBill() {
        return selectedBill;
    }

    public void setSelectedBill(Bill selectedBill) {
        this.selectedBill = selectedBill;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}
