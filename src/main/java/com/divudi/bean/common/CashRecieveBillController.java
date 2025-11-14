/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.inward.AdmissionController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CreditBean;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(CashRecieveBillController.class.getName());

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
    Department department;
    Institution site;
    
    private Institution creditCompany;
    private String comment;
    private String selectedBillType;

    public void makeNull() {
        printPreview = false;
        currentBillItem = null;
        removingItem = null;
        billItems = null;
        selectedBillItems = null;
        paymentMethodData = null;
        creditCompany = null;
        recreateModel();
    }

    public void selectInstitutionListener() {
        Institution ins = creditCompany;
        makeNull();

        setCreditCompany(ins);
        if (ins == null) {
            return;
        }
        if (getCurrent() != null) {
            getCurrent().setCreditCompany(ins);
        }

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
        if (selectedBill == null) {
            return;
        }
        Institution ins = selectedBill.getCreditCompany();
        current = selectedBill;
        makeNull();

        setCreditCompany(ins);
        if (ins == null) {
            return;
        }
        if (getCurrent() != null) {
            getCurrent().setCreditCompany(ins);
        }

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
        Institution ins = creditCompany;
        makeNull();

        setCreditCompany(ins);
        if (ins == null) {
            return;
        }
        if (getCurrent() != null) {
            getCurrent().setCreditCompany(ins);
        }

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
//        Institution ins = creditCompany;
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
        Institution ins = creditCompany;
        makeNull();

        setCreditCompany(ins);
        if (ins == null) {
            return;
        }
        if (getCurrent() != null) {
            getCurrent().setCreditCompany(ins);
        }

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
        System.out.println("getReferenceBallance");
        System.out.println("billItem = " + billItem);
        
        double refBallance = 0;
        double neTotal = Math.abs(billItem.getReferenceBill().getNetTotal() + billItem.getReferenceBill().getVat());
        double refAmount = Math.abs(getCreditBean().getRefundAmount(billItem.getReferenceBill()));
        System.out.println("refAmount = " + refAmount);
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
        // Check if the item is already persisted to database
        if (billItem.getId() != null) {
            // If saved, retire the item instead of deleting to maintain audit trail
            billItem.setBill(null);  // Remove relationship
            billItem.setRetired(true);
            billItem.setRetiredAt(new Date());
            billItem.setRetirer(getSessionController().getLoggedUser());
            getBillItemFacade().edit(billItem);  // Persist the retired state

            // Reload all bill items from database to reflect changes
            if (getCurrent() != null && getCurrent().getId() != null) {
                getCurrent().setBillItems(getBillFacade().find(getCurrent().getId()).getBillItems());
            }
        } else {
            // If not previously persisted, just remove from lists
            getBillItems().remove(billItem.getSearialNo());
            getSelectedBillItems().remove(billItem.getSearialNo());
        }

        calTotalWithResetingIndex();
    }

    private boolean errorCheckForAdding() {
        // Check if referenceBill is null first
        if (getCurrentBillItem().getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Please select a bill to add");
            return true;
        }

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
        // Check if referenceBill is null first
        if (getCurrentBillItem().getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Please select a bill to add");
            return true;
        }

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

    /**
     * Combined add to bill method that automatically detects the bill type from the selected bill
     * and routes to appropriate method (addToBill() for OPD bills or addToBillPharmacy() for pharmacy bills)
     * Does not depend on selectedBillType - automatically detects from bill properties
     */
    public void addToBillCombined() {
        // Validate that referenceBill is not null before routing
        if (getCurrentBillItem().getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Please select a bill to add");
            return;
        }

        Bill referenceBill = getCurrentBillItem().getReferenceBill();

        // Determine the bill type from the reference bill properties
        // First check BillTypeAtomic for OPD bills
        BillTypeAtomic billTypeAtomic = referenceBill.getBillTypeAtomic();
        if (billTypeAtomic != null) {
            // Check if it's an OPD Batch bill
            if (billTypeAtomic == BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT
                    || billTypeAtomic == BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER) {
                addToBill();
                return;
            }
            // Check if it's an OPD Package bill
            if (billTypeAtomic == BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT
                    || billTypeAtomic == BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER) {
                addToBill();
                return;
            }
        }

        // Check BillType for Pharmacy bills
        BillType billType = referenceBill.getBillType();
        if (billType != null) {
            if (billType == BillType.PharmacySale || billType == BillType.PharmacyWholeSale) {
                addToBillPharmacy();
                return;
            }
        }

        // If we reach here, the bill type is not supported
        JsfUtil.addErrorMessage("Unsupported bill type. Cannot add this bill to the collection.");
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
//        String sql = "SELECT  sum(b.netTotal) FROM BilledBill b WHERE b.retired=false  and b.billType=com.divudi.core.data.BillType.InwardBill and b.cancelledBill is null and b.patientEncounter.id=" + getPatientEncounter().getId();
//        Double tmp = getBillFacade().findAggregateDbl(sql);
//
//        sql = "SELECT  sum(b.netTotal) FROM BilledBill b WHERE b.retired=false  and b.billType=com.divudi.core.data.BillType.InwardPaymentBill and b.cancelledBill is null and b.patientEncounter.id=" + getPatientEncounter().getId();
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
        if (creditCompany != null) {
            getCurrent().setCreditCompany(creditCompany);
        }
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

        // Use defensive copy to avoid ConcurrentModificationException when modifying collections during iteration
        for (BillItem savingBillItem : new ArrayList<>(getBillItems())) {
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

        // Use defensive copy to avoid ConcurrentModificationException when modifying collections during iteration
        for (BillItem savingBillItem : new ArrayList<>(getBillItems())) {
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
        // Validate creditCompany first
        if (creditCompany == null) {
            JsfUtil.addErrorMessage("Please select a credit company");
            return;
        }

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
        getCurrent().setCreditCompany(creditCompany);

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

    /**
     * @deprecated This method will be removed in the next iteration.
     * Pharmacy credit bills are now managed through settleBillCombined() method,
     * which handles both OPD and Pharmacy credit bills using the unified OPD Credit Settle bill type.
     * The separate Pharmacy Credit Settle bill type (BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_RECEIVED)
     * is being deprecated in favor of the unified OPD Credit Settle bill type.
     * Please use the Combined OPD Credit Collection page (/credit/credit_company_bill_opd_combined.xhtml) instead.
     */
    @Deprecated
    public void settleBillPharmacy() {
        // Enhanced pharmacy bill settlement with improved functionality

        // Enhanced validation to ensure proper fromInstitution setting for pharmacy bills
        // Auto-detection must happen BEFORE errorCheckPharmacy() to avoid early return
        if (getCurrent().getFromInstitution() == null && !getBillItems().isEmpty()) {
            // Auto-set fromInstitution from the first bill's toInstitution (credit company)
            BillItem firstItem = getBillItems().get(0);
            if (firstItem.getReferenceBill() != null && firstItem.getReferenceBill().getToInstitution() != null) {
                getCurrent().setFromInstitution(firstItem.getReferenceBill().getToInstitution());
            }
        }

        if (errorCheckPharmacy()) {
            return;
        }

        // Use enhanced total calculation method consistent with OPD settlements
        calulateTotalForSettlingCreditForOpdBatchBills();

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setTotal(getCurrent().getNetTotal());

        // Generate department bill number for proper tracking
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_RECEIVED);
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.CashRecieveBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_RECEIVED);

        // Set additional required fields
        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setComments(comment);
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setNetTotal(getCurrent().getNetTotal());

        // Save the main settlement bill
        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

        // Enhanced bill item processing with proper audit trails and settlement tracking
        // Use defensive copy to avoid ConcurrentModificationException and iterate over the same collection as other settlement flows
        for (BillItem savingBillItem : new ArrayList<>(getBillItems())) {
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

            // Enhanced settlement tracking - replaces simple updateReferenceBill with comprehensive settlement tracking
            updateSettlingCreditBillSettledValues(savingBillItem);
        }

        // Add payment service integration for proper payment record keeping
        paymentService.createPayment(current, getPaymentMethodData());

        JsfUtil.addSuccessMessage("Pharmacy Bill Settled Successfully");
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
        // Get the absolute payment value
        double paymentValue = Math.abs(tmp.getNetValue());

        // Step 1: Update the original bill (tmp.referenceBill)
        Bill originalBill = tmp.getReferenceBill();
        if (originalBill == null) {
            throw new RuntimeException("Reference bill is null for BillItem ID: " + tmp.getId()
                    + ". This indicates a data integrity issue.");
        }

        // Update original bill's financial fields
        updateBillFinancialFieldsForPayment(originalBill, paymentValue);

        // Step 2: Get and update the reference bill's reference bill if it exists
        Bill referenceBillOfOriginal = originalBill.getReferenceBill();
        if (referenceBillOfOriginal != null) {
            // Update reference bill's financial fields as well
            updateBillFinancialFieldsForPayment(referenceBillOfOriginal, paymentValue);
        }
    }

    /**
     * Helper method to update bill financial fields when receiving credit company payment
     * @param bill The bill to update
     * @param paymentValue The absolute payment value
     */
    private void updateBillFinancialFieldsForPayment(Bill bill, double paymentValue) {
        // Update paidAmount - add the payment amount
        double currentPaidAmount = bill.getPaidAmount();
        bill.setPaidAmount(currentPaidAmount + paymentValue);

        // Update balance - deduct the payment amount (only if balance > 0)
        double currentBalance = bill.getBalance();
        if (currentBalance > 0) {
            bill.setBalance(currentBalance - paymentValue);
        }

        // Save the updated bill
        getBillFacade().edit(bill);
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
        creditCompany = null;
        comment = null;
        selectedBill = null;
        selectedBillType = null;
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

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public double getRefundAmountForBill(Bill bill) {
        if (bill == null) {
            return 0.0;
        }
        return creditBean.getRefundAmount(bill);
    }

    // Combined Credit Collection Methods

    /**
     * Legacy property - kept for backward compatibility with other pages
     * The simplified combined credit collection page does not use this property
     */
    public String getSelectedBillType() {
        return selectedBillType;
    }

    public void setSelectedBillType(String selectedBillType) {
        this.selectedBillType = selectedBillType;
    }

    /**
     * Legacy method - called when bill type dropdown changes to clear current selections
     * Kept for backward compatibility with pages that still use bill type selection
     */
    public void billTypeChanged() {
        makeNull();
        selectedBillType = this.selectedBillType; // Preserve the selected bill type
    }

    /**
     * Combined autocomplete method that searches across ALL bill types (OPD Batch, OPD Package, and Pharmacy)
     * Does not depend on selectedBillType - searches all credit bills and combines results
     * @param query The search query string
     * @return List of bills matching the query from all bill types
     */
    public List<Bill> completeCombinedCreditBills(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Create combined list to hold results from all bill types
        List<Bill> combinedResults = new ArrayList<>();

        // Get results from OPD Batch bills
        List<Bill> opdBatchBills = billController.completeOpdCreditBatchBill(query);
        if (opdBatchBills != null && !opdBatchBills.isEmpty()) {
            combinedResults.addAll(opdBatchBills);
        }

        // Get results from OPD Package bills
        List<Bill> opdPackageBills = billController.completeOpdCreditPackageBatchBill(query);
        if (opdPackageBills != null && !opdPackageBills.isEmpty()) {
            combinedResults.addAll(opdPackageBills);
        }

        // Get results from Pharmacy bills
        List<Bill> pharmacyBills = billController.completePharmacyCreditBill(query);
        if (pharmacyBills != null && !pharmacyBills.isEmpty()) {
            combinedResults.addAll(pharmacyBills);
        }

        return combinedResults;
    }

    /**
     * Combined settlement method that routes to appropriate settlement method based on bill types in the list
     * Determines the bill type from the bills in the selectedBillItems list
     */
    public void settleCombinedCreditBills() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return;
        }

        // Determine bill type from the first bill item
        BillItem firstItem = getSelectedBillItems().get(0);
        if (firstItem.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Invalid bill reference");
            return;
        }

        BillTypeAtomic billTypeAtomic = firstItem.getReferenceBill().getBillTypeAtomic();

        // Route to appropriate settlement method based on bill type atomic
        if (billTypeAtomic == BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT
                || billTypeAtomic == BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER) {
            settleCreditForOpdBatchBills();
        } else if (billTypeAtomic == BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT
                || billTypeAtomic == BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER) {
            settleCreditForOpdPackageBills();
        } else if (firstItem.getReferenceBill().getBillType() == BillType.PharmacySale
                || firstItem.getReferenceBill().getBillType() == BillType.PharmacyWholeSale) {
            settleBillPharmacy();
        } else {
            JsfUtil.addErrorMessage("Unsupported bill type for settlement");
        }
    }

    /**
     * Universal settlement method that handles all bill types (OPD, pharmacy, etc.)
     * and creates settlements under OPD_CREDIT_COMPANY_PAYMENT_RECEIVED
     * This method consolidates pharmacy credit settlements with OPD settlements as per new requirement
     * Based on settleCreditForOpdBatchBills but extended to handle all bill types
     */
    public void settleUniversalCreditBills() {
        if (getSelectedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Item ");
            return;
        }
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Credit Company");
            return;
        }

        // Enhanced validation to handle different credit company reference patterns
        for (BillItem item : getBillItems()) {
            Institution billCreditCompany = null;

            // Handle different ways credit company is referenced in different bill types
            if (item.getReferenceBill().getCreditCompany() != null) {
                // OPD bills use creditCompany field
                billCreditCompany = item.getReferenceBill().getCreditCompany();
            } else if (item.getReferenceBill().getToInstitution() != null) {
                // Pharmacy bills use toInstitution field
                billCreditCompany = item.getReferenceBill().getToInstitution();
            }

            if (billCreditCompany == null || !Objects.equals(billCreditCompany.getId(), getCurrent().getFromInstitution().getId())) {
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

        // Generate department bill number for OPD credit company payment
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);

        // Calculate total using the same method as OPD batch bills
        calulateTotalForSettlingCreditForOpdBatchBills();

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.CashRecieveBill);

        // IMPORTANT: All settlements now use OPD_CREDIT_COMPANY_PAYMENT_RECEIVED regardless of original bill type
        getCurrent().setBillTypeAtomic(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);

        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setComments(comment);
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setNetTotal(getCurrent().getNetTotal());

        // Save the main settlement bill
        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

        // Save bill items and update reference bills
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

            // Update settled values in reference bills (works for both OPD and pharmacy bills)
            updateSettlingCreditBillSettledValues(savingBillItem);
        }

        // Create payment records
        paymentService.createPayment(current, getPaymentMethodData());

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
    }

}
