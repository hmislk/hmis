/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.lab.LabTestHistoryController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.ItemLight;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.ServiceSessionBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillSession;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Packege;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillSessionFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillValidation;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.service.StaffService;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillPackageController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillFacade billFacade;
    @EJB
    BillService billService;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    ServiceSessionBean serviceSessionBean;
    @EJB
    BillSessionFacade billSessionFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    StaffService staffBean;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    StaffService staffService;
    @EJB
    PaymentService paymentService;

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Injects">
    @Inject
    EnumController enumController;
    @Inject
    SessionController sessionController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private BillSearch billSearch;
    @Inject
    private WebUserController webUserController;
    @Inject
    ItemApplicationController itemApplicationController;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    ItemController itemController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    PageMetadataRegistry pageMetadataRegistry;
    @Inject
    OpdBillController opdBillController;
    @Inject
    ApplicationController applicationController;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    LabTestHistoryController labTestHistoryController;
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private List<Item> malePackaes;
    private List<Item> femalePackaes;
    private List<Item> bothPackaes;
    private List<Item> packaes;
    private static final long serialVersionUID = 1L;
    private boolean duplicatePrint;
    private Bill batchBill;
    private Bill bill;
    private boolean printPreview;
    //Interface Data
    private PaymentScheme paymentScheme;
    private PaymentMethod paymentMethod;
    private Patient patient;
    private Doctor referredBy;
    private Institution creditCompany;
    private Staff staff;
    private double total;
    private double discount;
    private double netTotal;
    private double tenderedAmount;
    private double cashBalance;
    private Institution chequeBank;
    private BillItem currentBillItem;
    private Institution collectingCentre;
    private Staff toStaff;

    //Bill Items
    private List<BillComponent> lstBillComponents;
    private List<BillFee> lstBillFees;
    private List<BillItem> lstBillItems;
    private List<BillEntry> lstBillEntries;
    private Integer index;

    List<Bill> bills;
    private YearMonthDay yearMonthDay;
    private PaymentMethodData paymentMethodData;
    private Institution referredByInstitution;
    private String referralId;

    private ItemLight itemLight;
    private List<ItemLight> opdPackages;
    private boolean patientDetailsEditable;
    private List<Payment> payments;

    private String comment;
    boolean batchBillCancellationStarted;
    private List<PaymentMethod> paymentMethods;
    private double remainAmount;
    private List<Payment> originalBillPayments;
    private boolean originalBillCredit;

    //</editor-fold>
    private void savePatient() {
        if (getPatient().getId() == null) {
            if (getPatient().getPerson().getName() != null) {
                String updatedPatientName;
                updatedPatientName = opdBillController.changeTextCases(getPatient().getPerson().getName(), getSessionController().getApplicationPreference().getChangeTextCasesPatientName());
                getPatient().getPerson().setName(updatedPatientName);
            }
            getPatient().setPhn(applicationController.createNewPersonalHealthNumber(getSessionController().getInstitution()));
            getPatient().setCreatedInstitution(getSessionController().getInstitution());
            getPatient().setCreater(getSessionController().getLoggedUser());
            getPatient().setCreatedAt(new Date());
            if (getPatient().getPerson().getId() != null) {
//                getPatientFacade().edit(getPatient());
                getPersonFacade().edit(getPatient().getPerson());
            } else {
                getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getPatient().getPerson().setCreatedAt(new Date());
//                getPatientFacade().create(getPatient());
                getPersonFacade().create(getPatient().getPerson());
            }
            try {
                getPatientFacade().create(getPatient());
            } catch (Exception e) {
                getPatientFacade().edit(getPatient());
            }
        } else {
            if (getPatient().getPerson().getId() != null) {
//                getPatientFacade().edit(getPatient());
                getPersonFacade().edit(getPatient().getPerson());
            } else {
                getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getPatient().getPerson().setCreatedAt(new Date());
//                getPatientFacade().create(getPatient());
                getPersonFacade().create(getPatient().getPerson());
            }
        }
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public void putToBills() {
        bills = new ArrayList<>();
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getTransDepartment());

        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();

            saveBill(d, myBill);

            List<BillEntry> tmp = new ArrayList<>();
            List<BillItem> list = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {

                if (Objects.equals(e.getBillItem().getItem().getTransDepartment().getId(), d.getId())) {
                    getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    // getBillBean().calculateBillItem(myBill, e);
                    list.add(e.getBillItem());
                    tmp.add(e);
                }
            }
            myBill.setBillItems(list);
            getBillBean().calculateBillItems(myBill, tmp);
            bills.add(myBill);
        }
    }

    private void saveBatchBill() {
        batchBill = new BilledBill();
        batchBill.setBillType(BillType.OpdBathcBill);
        batchBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        batchBill.setBillPackege((Packege) currentBillItem.getItem());
        batchBill.setPaymentScheme(paymentScheme);
        batchBill.setPaymentMethod(paymentMethod);
        batchBill.setCreatedAt(new Date());
        batchBill.setCreater(getSessionController().getLoggedUser());

        batchBill.setDepartment(getSessionController().getLoggedUser().getDepartment());
        batchBill.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        batchBill.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        batchBill.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        batchBill.setStaff(staff);
        batchBill.setReferredBy(referredBy);
        batchBill.setCreditCompany(creditCompany);

        getBillBean().setPaymentMethodData(batchBill, paymentMethod, getPaymentMethodData());

        batchBill.setBillDate(new Date());
        batchBill.setBillTime(new Date());
        batchBill.setPatient(getPatient());
        batchBill.setPaymentMethod(paymentMethod);
        batchBill.setPaymentScheme(getPaymentScheme());

        String deptID = billNumberBean.departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);

        batchBill.setDeptId(deptID);
        batchBill.setInsId(deptID);

        getBillFacade().create(batchBill);

        double dbl = 0;
        double dblTot = 0;
        for (Bill b : bills) {
            b.setBackwardReferenceBill(batchBill);
            dbl += b.getNetTotal();
            dblTot += b.getTotal();
            getBillFacade().edit(b);

            batchBill.getForwardReferenceBills().add(b);
        }

        batchBill.setNetTotal(dbl);
        batchBill.setTotal(dblTot);
        batchBill.setDiscount(dblTot - dbl);

        // Initialize balance field for credit bills
        if (paymentMethod == PaymentMethod.Credit) {
            double totalAmount = Math.abs(batchBill.getNetTotal());
            if (batchBill.getVat() != 0.0) {
                totalAmount += Math.abs(batchBill.getVat());
            }
            batchBill.setBalance(totalAmount);
        }

        getBillFacade().edit(batchBill);

    }

    public void cancellAll() {
        Bill tmp = new CancelledBill();
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(tmp);

        Bill billedBill = null;
        for (Bill b : bills) {
            billedBill = b.getBackwardReferenceBill();
            getBillSearch().setBill((BilledBill) b);
            getBillSearch().setPaymentMethod(b.getPaymentMethod());
            getBillSearch().setComment("Batch Cancell");
            getBillSearch().cancelOpdBill();
        }

        tmp.copy(billedBill);
        tmp.setBilledBill(billedBill);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(tmp, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
    }

    private void saveBillItemSessions() {
        for (BillEntry be : lstBillEntries) {
            BillItem temBi = be.getBillItem();
            BillSession temBs = getServiceSessionBean().createBillSession(temBi);
            temBi.setBillSession(temBs);
            if (temBs != null) {
                getBillSessionFacade().create(temBs);
            }
        }
    }

    @Deprecated // Instead Use checkPaymentDetails
    public boolean validatePaymentMethodDeta() {
        boolean error = false;

        if (getPaymentMethod() == PaymentMethod.Card) {
            if (getPaymentMethodData().getCreditCard().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Package Billing - CreditCard Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Credit Card Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.Cheque) {
            if (getPaymentMethodData().getCheque().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Package Billing - Cheque Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Cheque Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.ewallet) {
            if (getPaymentMethodData().getEwallet().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Package Billing - E-Wallet Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a E-Wallet Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.Slip) {
            if (getPaymentMethodData().getSlip().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Package Billing - Slip Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Slip Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.Credit) {
            if (getPaymentMethodData().getCredit().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Package Billing - Credit Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Credit Comment..");
                error = true;
            }
            if (getPaymentMethodData().getCredit().getInstitution() == null) {
                JsfUtil.addErrorMessage("Please Enter a Credit Company.");
                error = true;
            } else {
                creditCompany = getPaymentMethodData().getCredit().getInstitution();
            }
        }
        return error;
    }

    private boolean checkPatientDetails() {
        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Name to Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().isEmpty()
                    || getPatient().getPerson().getSex() == null || getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Cannot bill without Patient Name, Age or Sex.");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                JsfUtil.addErrorMessage("Please select title.");
                return true;
            }
            if (getPatient().getPerson().getSex() == null) {
                JsfUtil.addErrorMessage("Please select gender.");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Please select patient date of birth.");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Phone Number to save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getPhone() == null || getPatient().getPerson().getPhone().trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please enter a phone number.");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Area to save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getArea() == null || getPatient().getPerson().getArea().getName().trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please select patient area.");
                return true;
            }
        }
        
        if(configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management in the system", false) 
                && configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management for OPD from the system", false)){
            if(getPatient().isBlacklisted()){
                JsfUtil.addErrorMessage("This patient is blacklisted from the system. Can't Bill.");
                return true;
            }
        }

        if (getLstBillEntries().isEmpty()) {
            JsfUtil.addErrorMessage("No investigations are added to the bill to settle.");
            return true;
        }

        return false;
    }

    @Deprecated // Use PaymentService
    private boolean checkPaymentDetails() {
        System.out.println("checkPaymentDetails");
        System.out.println("getPaymentMethod() = " + getPaymentMethod());
        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method.");
            return true;
        }

        if (getPaymentMethod() == PaymentMethod.Credit) {
            System.out.println("credit");
            if (getPaymentMethodData().getCredit().getComment() == null
                    && configOptionApplicationController.getBooleanValueByKey("Package Billing - Credit Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please enter a Credit comment.");
                return true;
            }
            if (getPaymentMethodData().getCredit().getComment().trim().isEmpty()
                    && configOptionApplicationController.getBooleanValueByKey("Package Billing - Credit Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please enter a Credit comment.");
                return true;
            }
            System.out.println("getPaymentMethodData().getCredit().getInstitution() = " + getPaymentMethodData().getCredit().getInstitution());
            if (getPaymentMethodData().getCredit().getInstitution() == null) {
                JsfUtil.addErrorMessage("Please enter a Credit Company !");
                return true;
            } else {
                creditCompany = getPaymentMethodData().getCredit().getInstitution();
            }
        }

        if (getPaymentMethod() == PaymentMethod.Card
                && getPaymentMethodData().getCreditCard().getComment().trim().isEmpty()
                && configOptionApplicationController.getBooleanValueByKey("Package Billing - CreditCard Comment is Mandatory", false)) {
            JsfUtil.addErrorMessage("Please enter a Credit Card comment.");
            return true;
        }

        if (getPaymentMethod() == PaymentMethod.Cheque
                && getPaymentMethodData().getCheque().getComment().trim().isEmpty()
                && configOptionApplicationController.getBooleanValueByKey("Package Billing - Cheque Comment is Mandatory", false)) {
            JsfUtil.addErrorMessage("Please enter a Cheque comment.");
            return true;
        }

        if (getPaymentMethod() == PaymentMethod.ewallet
                && getPaymentMethodData().getEwallet().getComment().trim().isEmpty()
                && configOptionApplicationController.getBooleanValueByKey("Package Billing - E-Wallet Comment is Mandatory", false)) {
            JsfUtil.addErrorMessage("Please enter an E-Wallet comment.");
            return true;
        }

        if (getPaymentMethod() == PaymentMethod.Slip
                && getPaymentMethodData().getSlip().getComment().trim().isEmpty()
                && configOptionApplicationController.getBooleanValueByKey("Package Billing - Slip Comment is Mandatory", false)) {
            JsfUtil.addErrorMessage("Please enter a Slip comment.");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }
        return false;
    }

    public String cancelPackageBill() {
        batchBillCancellationStarted = true;
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No bill");
            batchBillCancellationStarted = false;
            return "";
        }
        if (getBill().getId() == null) {
            JsfUtil.addErrorMessage("No Saved bill");
            batchBillCancellationStarted = false;
            return "";
        }

        if (getBill().getBackwardReferenceBill().getPaymentMethod() == PaymentMethod.Credit) {
            List<BillItem> items = billService.checkCreditBillPaymentReciveFromCreditCompany(getBill().getBackwardReferenceBill());

            if (items != null && !items.isEmpty()) {
                JsfUtil.addErrorMessage("This bill has been paid for by the credit company. Therefore, it cannot be canceled.");
                return "";
            }
        }

        if (!hasPrivilegeToCancelPackageBill(getBill())) {
            batchBillCancellationStarted = false;
            return "";
        }

        if (errorsPresentOnOpdBillCancellation()) {
            batchBillCancellationStarted = false;
            return "";
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method.");
            batchBillCancellationStarted = false;
            return "";
        }

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);

        Bill cancellationBill = new CancelledBill();
        cancellationBill.copy(bill);
        cancellationBill.setDepartment(sessionController.getDepartment());
        cancellationBill.setInstitution(sessionController.getInstitution());
        cancellationBill.setFromDepartment(bill.getFromDepartment());
        cancellationBill.setToDepartment(bill.getToDepartment());
        cancellationBill.setFromInstitution(bill.getFromInstitution());
        cancellationBill.setToInstitution(bill.getToInstitution());
        cancellationBill.setBillType(BillType.OpdBill);
        cancellationBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        cancellationBill.setInsId(deptId);
        cancellationBill.setDeptId(deptId);
        cancellationBill.setCreatedAt(new Date());
        cancellationBill.setCreater(getSessionController().getLoggedUser());
        cancellationBill.setTotal(0 - Math.abs(bill.getTotal()));
        cancellationBill.setHospitalFee(0 - Math.abs(bill.getHospitalFee()));
        cancellationBill.setCollctingCentreFee(0 - Math.abs(bill.getCollctingCentreFee()));
        cancellationBill.setProfessionalFee(0 - Math.abs(bill.getProfessionalFee()));
        cancellationBill.setGrantTotal(0 - Math.abs(bill.getGrantTotal()));
        cancellationBill.setDiscount(0 - Math.abs(bill.getDiscount()));
        cancellationBill.setNetTotal(0 - Math.abs(bill.getNetTotal()));
        cancellationBill.setPaymentMethod(paymentMethod);
        cancellationBill.setBilledBill(bill);
        getBillFacade().create(cancellationBill);

        bill.setCancelled(true);
        bill.setCancelledBill(cancellationBill);
        if (bill.getId() == null) {
            getBillFacade().create(bill);
        } else {
            getBillFacade().edit(bill);
        }

        // Update batch bill balance for credit payment method
        updateBatchBillFinancialFieldsForPackageCancellation(bill, cancellationBill);

        List<BillItem> originalBillItem = getBillBean().fillBillItems(bill);

        for (BillItem bi : originalBillItem) {
            BillItem cancelBillItem = new BillItem();
            cancelBillItem.copy(bi);
            cancelBillItem.setReferanceBillItem(bi);
            cancelBillItem.setBill(cancellationBill);
            cancelBillItem.setCreatedAt(new Date());
            cancelBillItem.setCreater(getSessionController().getLoggedUser());
            //Create Cancel BillItem
            if (cancelBillItem.getId() == null) {
                billItemFacade.create(cancelBillItem);
            } else {
                billItemFacade.edit(cancelBillItem);
            }

            List<BillFee> originalBillItemFees = getBillBean().fetchBillFees(bi);

            double hospitalFee = 0.0;
            double ccFee = 0.0;
            double staffFee = 0.0;
            double reagentFee = 0.0;
            double otherFee = 0.0;

            for (BillFee fee : originalBillItemFees) {
                BillFee cancelBillItemFee = new BillFee();
                cancelBillItemFee.copy(fee);
                cancelBillItemFee.setReferenceBillFee(fee);
                cancelBillItemFee.setBill(cancellationBill);
                cancelBillItemFee.setBillItem(cancelBillItem);

                //Create Cancel BillItemFee
                if (cancelBillItemFee.getId() == null) {
                    billFeeFacade.create(cancelBillItemFee);
                } else {
                    billFeeFacade.edit(cancelBillItemFee);
                }

                if (cancelBillItemFee.getFee().getFeeType() == FeeType.CollectingCentre) {
                    ccFee += cancelBillItemFee.getFeeValue();
                } else if (cancelBillItemFee.getFee().getFeeType() == FeeType.Staff) {
                    staffFee += cancelBillItemFee.getFeeValue();
                } else {
                    hospitalFee += cancelBillItemFee.getFeeValue();
                }

                if (cancelBillItemFee.getFee().getFeeType() == FeeType.Chemical) {
                    reagentFee += cancelBillItemFee.getFeeValue();
                } else if (cancelBillItemFee.getFee().getFeeType() == FeeType.Additional) {
                    otherFee += cancelBillItemFee.getFeeValue();
                }

                //update Original BillItemFee
                fee.setReferenceBillFee(cancelBillItemFee);
                if (cancelBillItemFee.getId() == null) {
                    billFeeFacade.create(fee);
                } else {
                    billFeeFacade.edit(fee);
                }
            }

            cancelBillItem.setHospitalFee(hospitalFee);
            cancelBillItem.setStaffFee(staffFee);
            cancelBillItem.setCollectingCentreFee(ccFee);
            cancelBillItem.setReagentFee(reagentFee);
            cancelBillItem.setOtherFee(otherFee);

            if (cancelBillItem.getId() == null) {
                billItemFacade.create(cancelBillItem);
            } else {
                billItemFacade.edit(cancelBillItem);
            }

            //update Original BillItem
            bi.setReferanceBillItem(cancelBillItem);
            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }

        try {
            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBill(getBill())) {
                    labTestHistoryController.addCancelHistory(pi, sessionController.getDepartment(), comment);
                }
            }
        } catch (Exception e) {
            System.out.println("Error = " + e);
        }

        // Apply negative signs to payment data for cancellation (reversal)
        applyCancellationSignToPaymentData();

        // Create payments with negated values for cancellation
        payments = paymentService.createPayment(cancellationBill, getPaymentMethodData());

        // Update patient deposit balances and create proper history records using modern service
        paymentService.updateBalances(payments);

        printPreview = true;
        batchBillCancellationStarted = false;
        return null;
    }

    public boolean checkCancelBill(Bill originalBill) {
        List<PatientInvestigationStatus> availableStatus = enumController.getAvailableStatusforCancel();
        boolean canCancelBill = false;
        if (availableStatus.contains(originalBill.getStatus())) {
            canCancelBill = true;
        }
        return canCancelBill;
    }

    private boolean hasPrivilegeToCancelPackageBill(Bill billToCancel) {
        boolean labStatusOk = checkCancelBill(billToCancel);

        if (!labStatusOk) {
            if (!getWebUserController().hasPrivilege("OpdPackageBillCancel")) {
                JsfUtil.addErrorMessage("This bill is processed in the Laboratory.");
                JsfUtil.addErrorMessage(
                        "You have no Privilege to Cancel Package Bills. Please Contact System Administrator.");
                return false;
            }
            // Bill is processed in lab but user has special privilege - no error message needed
        } else {
            if (!getWebUserController().hasPrivilege("OpdCancel")) {
                JsfUtil.addErrorMessage(
                        "You have no Privilege to Cancel Package Bills. Please Contact System Administrator.");
                return false;
            }
        }

        return true;
    }

    public String cancelPackageBatchBill() {
        batchBillCancellationStarted = true;
        if (getBatchBill() == null) {
            JsfUtil.addErrorMessage("No bill");
            batchBillCancellationStarted = false;
            return "";
        }
        if (getBatchBill().getId() == null) {
            JsfUtil.addErrorMessage("No Saved bill");
            batchBillCancellationStarted = false;
            return "";
        }

        if (getBatchBill().getPaymentMethod() == PaymentMethod.Credit) {
            List<BillItem> items = billService.checkCreditBillPaymentReciveFromCreditCompany(getBatchBill());

            if (items != null && !items.isEmpty()) {
                JsfUtil.addErrorMessage("This bill has been paid for by the credit company. Therefore, it cannot be canceled.");
                return "";
            }
        }

        for (Bill singleBill : billBean.validBillsOfBatchBill(getBatchBill())) {
            if (!hasPrivilegeToCancelPackageBill(singleBill)) {
                batchBillCancellationStarted = false;
                return "";
            }
        }

        if (errorsPresentOnOpdBatchBillCancellation()) {
            batchBillCancellationStarted = false;
            return "";
        }

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);

        Bill cancellationBatchBill = new CancelledBill();
        cancellationBatchBill.copy(batchBill);
        cancellationBatchBill.setDepartment(sessionController.getDepartment());
        cancellationBatchBill.setInstitution(sessionController.getInstitution());
        cancellationBatchBill.setFromDepartment(batchBill.getFromDepartment());
        cancellationBatchBill.setToDepartment(batchBill.getToDepartment());
        cancellationBatchBill.setFromInstitution(batchBill.getFromInstitution());
        cancellationBatchBill.setToInstitution(batchBill.getToInstitution());
        cancellationBatchBill.setBillType(BillType.OpdBathcBill);
        cancellationBatchBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        cancellationBatchBill.setInsId(deptId);
        cancellationBatchBill.setDeptId(deptId);
        cancellationBatchBill.setCreatedAt(new Date());
        cancellationBatchBill.setCreater(getSessionController().getLoggedUser());
        cancellationBatchBill.setTotal(0 - Math.abs(batchBill.getTotal()));
        cancellationBatchBill.setHospitalFee(0 - Math.abs(batchBill.getHospitalFee()));
        cancellationBatchBill.setCollctingCentreFee(0 - Math.abs(batchBill.getCollctingCentreFee()));
        cancellationBatchBill.setProfessionalFee(0 - Math.abs(batchBill.getProfessionalFee()));
        cancellationBatchBill.setGrantTotal(0 - Math.abs(batchBill.getGrantTotal()));
        cancellationBatchBill.setDiscount(0 - Math.abs(batchBill.getDiscount()));
        cancellationBatchBill.setNetTotal(0 - Math.abs(batchBill.getNetTotal()));
        if (paymentMethod != null) {
            cancellationBatchBill.setPaymentMethod(paymentMethod);
        }
        cancellationBatchBill.setBilledBill(batchBill);

        // Transfer staff data to paymentMethodData before creating payments
        transferStaffDataToPaymentMethodData();

        // Apply negative signs for cancellation
        applyCancellationSignToPaymentData();

        getBillFacade().create(cancellationBatchBill);

        batchBill.setCancelled(true);
        batchBill.setCancelledBill(cancellationBatchBill);

        // Reset balance to 0 for credit payment batch bill cancellations
        if (batchBill.getPaymentMethod() == PaymentMethod.Credit && batchBill.getBalance() > 0) {
            double oldBalance = batchBill.getBalance();
            batchBill.setBalance(0);

            System.out.println("=== Package Batch Bill Cancellation - Balance Reset ===");
            System.out.println("Batch Bill: " + batchBill.getInsId());
            System.out.println("Old Balance: " + oldBalance + " → New Balance: 0.0");
        }

        getBillFacade().edit(batchBill);

        bills = billService.fetchIndividualBillsOfBatchBill(batchBill);

        for (Bill originalBill : bills) {
            cancelSingleBillWhenCancellingPackageBatchBill(originalBill, cancellationBatchBill);
            try {
                if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                    for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBill(originalBill)) {
                        labTestHistoryController.addCancelHistory(pi, sessionController.getDepartment(), comment);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error = " + e);
            }

        }

        // Create payments using modern PaymentService with paymentMethodData
        payments = paymentService.createPayment(cancellationBatchBill, paymentMethodData);

        // Update all balances automatically
        paymentService.updateBalances(payments);
        printPreview = true;
        batchBillCancellationStarted = false;
        return "/opd/opd_package_batch_bill_print?faces-redirect=true";
    }

    public String navigateToCancelOpdPackageBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill is selected to cancel");
            return "";
        }
        if (bill.getBackwardReferenceBill() == null) {
            JsfUtil.addErrorMessage("No Batch Bill is selected to cancel");
            return "";
        }
        // Remove payment method restrictions - allow all payment methods
        paymentMethods = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            paymentMethods.add(pm);
        }

        // Debug bill relationships to understand batch vs individual bills
        System.out.println("=== BILL RELATIONSHIP DEBUGGING ===");
        System.out.println("Current bill ID: " + bill.getId());
        System.out.println("Current bill type: " + bill.getBillType());
        System.out.println("Current bill atomic: " + bill.getBillTypeAtomic());
        System.out.println("Backward reference bill ID: " + (bill.getBackwardReferenceBill() != null ? bill.getBackwardReferenceBill().getId() : "null"));
        System.out.println("Backward reference bill type: " + (bill.getBackwardReferenceBill() != null ? bill.getBackwardReferenceBill().getBillType() : "null"));
        System.out.println("Backward reference bill atomic: " + (bill.getBackwardReferenceBill() != null ? bill.getBackwardReferenceBill().getBillTypeAtomic() : "null"));

        // Check if backward reference has a forward reference (batch bill)
        if (bill.getBackwardReferenceBill() != null && bill.getBackwardReferenceBill().getForwardReferenceBill() != null) {
            Bill batchBill = bill.getBackwardReferenceBill().getForwardReferenceBill();
            System.out.println("Found BATCH BILL - ID: " + batchBill.getId());
            System.out.println("Batch bill type: " + batchBill.getBillType());
            System.out.println("Batch bill atomic: " + batchBill.getBillTypeAtomic());
            System.out.println("Batch bill payment method: " + batchBill.getPaymentMethod());

            // Try to get payments from batch bill instead of individual bill
            List<Payment> batchBillPayments = billService.fetchBillPayments(batchBill);
            System.out.println("Batch bill payments count: " + (batchBillPayments != null ? batchBillPayments.size() : 0));

            if (batchBillPayments != null && !batchBillPayments.isEmpty()) {
                for (Payment p : batchBillPayments) {
                    System.out.println("Batch bill payment: " + p.getPaymentMethod() +
                                     " | Amount: " + p.getPaidValue() +
                                     " | Staff: " + (p.getToStaff() != null ? p.getToStaff().getPerson().getName() : "null"));
                }
                System.out.println("Using BATCH BILL payments for cancellation");
                originalBillPayments = batchBillPayments;
                originalBillCredit = (batchBill.getPaymentMethod() == PaymentMethod.Credit);
            } else {
                System.out.println("No payments on batch bill, trying individual bill");
                originalBillPayments = billService.fetchBillPayments(bill.getBackwardReferenceBill());
                originalBillCredit = (bill.getBackwardReferenceBill().getPaymentMethod() == PaymentMethod.Credit);
            }
        } else {
            System.out.println("No batch bill found, using backward reference bill");
            originalBillPayments = billService.fetchBillPayments(bill.getBackwardReferenceBill());
            originalBillCredit = (bill.getBackwardReferenceBill().getPaymentMethod() == PaymentMethod.Credit);
        }

        // Debug final payment details
        System.out.println("Final payment count: " + (originalBillPayments != null ? originalBillPayments.size() : 0));
        if (originalBillPayments != null) {
            for (Payment p : originalBillPayments) {
                System.out.println("Final payment: " + p.getPaymentMethod() +
                                 " | Amount: " + p.getPaidValue() +
                                 " | Staff: " + (p.getToStaff() != null ? p.getToStaff().getPerson().getName() : "null"));
            }
        }
        System.out.println("=== END BILL RELATIONSHIP DEBUGGING ===");

        // Initialize payment data from original payments
        initializeCancellationPaymentFromOriginalPayments(originalBillPayments);

        // Set default payment method to original or Cash as fallback
        if (configOptionApplicationController.getBooleanValueByKey("Set the Original Bill PaymentMethod to Cancelation Bill")) {
            paymentMethod = bill.getBackwardReferenceBill().getPaymentMethod();
        } else {
            // Default to original payment method if available, otherwise Cash
            if (originalBillPayments != null && !originalBillPayments.isEmpty()) {
                paymentMethod = originalBillPayments.get(0).getPaymentMethod();
            } else {
                paymentMethod = PaymentMethod.Cash;
            }
        }
        printPreview = false;
        return "/opd/opd_package_bill_cancel?faces-redirect=true";
    }

    /**
     * Initialize payment method data from original payments for cancellation
     * Overloaded version that accepts a specific bill to use for net total
     * Used for batch bill cancellations where batchBill is used instead of bill
     */
    private void initializeCancellationPaymentFromOriginalPayments(List<Payment> originalPayments, Bill billToUse) {
        if (originalPayments == null || originalPayments.isEmpty()) {
            return;
        }

        if (billToUse == null) {
            return;
        }

        // Initialize payment method data if not already done
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }

        // Handle single payment method scenario
        if (originalPayments.size() == 1) {
            Payment originalPayment = originalPayments.get(0);

            // Initialize payment method data based on original payment method (using absolute values for UI display)
            switch (originalPayment.getPaymentMethod()) {
                case Cash:
                    paymentMethodData.getCash().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    break;
                case Card:
                    paymentMethodData.getCreditCard().setInstitution(originalPayment.getBank());
                    paymentMethodData.getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                    paymentMethodData.getCreditCard().setComment(originalPayment.getComments());
                    paymentMethodData.getCreditCard().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    break;
                case Cheque:
                    paymentMethodData.getCheque().setInstitution(originalPayment.getBank());
                    paymentMethodData.getCheque().setDate(originalPayment.getChequeDate());
                    paymentMethodData.getCheque().setNo(originalPayment.getChequeRefNo());
                    paymentMethodData.getCheque().setComment(originalPayment.getComments());
                    paymentMethodData.getCheque().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    break;
                case Slip:
                    paymentMethodData.getSlip().setInstitution(originalPayment.getBank());
                    paymentMethodData.getSlip().setDate(originalPayment.getPaymentDate());
                    paymentMethodData.getSlip().setReferenceNo(originalPayment.getReferenceNo());
                    paymentMethodData.getSlip().setComment(originalPayment.getComments());
                    paymentMethodData.getSlip().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    break;
                case ewallet:
                    paymentMethodData.getEwallet().setInstitution(
                        originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                    paymentMethodData.getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                    paymentMethodData.getEwallet().setNo(originalPayment.getReferenceNo());
                    paymentMethodData.getEwallet().setReferralNo(originalPayment.getPolicyNo());
                    paymentMethodData.getEwallet().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    paymentMethodData.getEwallet().setComment(originalPayment.getComments());
                    break;
                case PatientDeposit:
                    paymentMethodData.getPatient_deposit().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    paymentMethodData.getPatient_deposit().setComment(originalPayment.getComments());
                    // Load and set the PatientDeposit object for displaying balance
                    if (billToUse.getPatient() != null) {
                        paymentMethodData.getPatient_deposit().setPatient(billToUse.getPatient());
                        com.divudi.core.entity.PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                                billToUse.getPatient(), sessionController.getDepartment());
                        if (pd != null && pd.getId() != null) {
                            paymentMethodData.getPatient_deposit().getPatient().setHasAnAccount(true);
                            paymentMethodData.getPatient_deposit().setPatientDepost(pd);
                        }
                    }
                    break;
                case Credit:
                    paymentMethodData.getCredit().setInstitution(originalPayment.getCreditCompany());
                    paymentMethodData.getCredit().setReferenceNo(originalPayment.getReferenceNo());
                    paymentMethodData.getCredit().setReferralNo(originalPayment.getPolicyNo());
                    paymentMethodData.getCredit().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    paymentMethodData.getCredit().setComment(originalPayment.getComments());
                    break;
                case Staff:
                    Staff staffForCredit = originalPayment.getToStaff();
                    if (staffForCredit == null) {
                        staffForCredit = billToUse.getToStaff();
                    }

                    // Set staff member details for Staff Credit
                    paymentMethodData.getStaffCredit().setToStaff(staffForCredit);
                    paymentMethodData.getStaffCredit().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    paymentMethodData.getStaffCredit().setComment(originalPayment.getComments());
                    break;
                case Staff_Welfare:
                    Staff staffForWelfare = originalPayment.getToStaff();
                    if (staffForWelfare == null) {
                        staffForWelfare = billToUse.getToStaff();
                    }

                    // Set staff member details for Staff Welfare
                    paymentMethodData.getStaffWelfare().setToStaff(staffForWelfare);
                    paymentMethodData.getStaffWelfare().setTotalValue(Math.abs(billToUse.getNetTotal()));
                    paymentMethodData.getStaffWelfare().setComment(originalPayment.getComments());
                    break;
                default:
                    // For other payment methods, just set the total value
                    break;
            }
        } else {
            // Handle multiple payment methods scenario
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();

            // Create ComponentDetail for each original payment
            for (Payment originalPayment : originalPayments) {
                com.divudi.core.data.dataStructure.ComponentDetail cd = new com.divudi.core.data.dataStructure.ComponentDetail();
                cd.setPaymentMethod(originalPayment.getPaymentMethod());
                cd.setPaymentMethodData(new PaymentMethodData());
                double refundAmount = Math.abs(originalPayment.getPaidValue());
                cd.setTotalValue(refundAmount);

                // Populate payment method data for this component
                switch (originalPayment.getPaymentMethod()) {
                    case Cash:
                        cd.getPaymentMethodData().getCash().setTotalValue(refundAmount);
                        break;
                    case Card:
                        cd.getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                        cd.getPaymentMethodData().getCreditCard().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getCreditCard().setTotalValue(refundAmount);
                        break;
                    case Cheque:
                        cd.getPaymentMethodData().getCheque().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                        cd.getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                        cd.getPaymentMethodData().getCheque().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getCheque().setTotalValue(refundAmount);
                        break;
                    case Slip:
                        cd.getPaymentMethodData().getSlip().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getSlip().setDate(originalPayment.getPaymentDate());
                        cd.getPaymentMethodData().getSlip().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getSlip().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getSlip().setTotalValue(refundAmount);
                        break;
                    case ewallet:
                        cd.getPaymentMethodData().getEwallet().setInstitution(
                            originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                        cd.getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getEwallet().setNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getEwallet().setReferralNo(originalPayment.getPolicyNo());
                        cd.getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getEwallet().setTotalValue(refundAmount);
                        break;
                    case PatientDeposit:
                        cd.getPaymentMethodData().getPatient_deposit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                        if (billToUse.getPatient() != null) {
                            cd.getPaymentMethodData().getPatient_deposit().setPatient(billToUse.getPatient());
                            // Load and set the PatientDeposit object for displaying balance
                            com.divudi.core.entity.PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                                    billToUse.getPatient(), sessionController.getDepartment());
                            if (pd != null && pd.getId() != null) {
                                cd.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                                cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                            }
                        }
                        break;
                    case Staff:
                        cd.getPaymentMethodData().getStaffCredit().setToStaff(originalPayment.getToStaff());
                        cd.getPaymentMethodData().getStaffCredit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getStaffCredit().setComment(originalPayment.getComments());
                        break;
                    case Staff_Welfare:
                        cd.getPaymentMethodData().getStaffWelfare().setToStaff(originalPayment.getToStaff());
                        cd.getPaymentMethodData().getStaffWelfare().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getStaffWelfare().setComment(originalPayment.getComments());
                        break;
                    case Credit:
                        cd.getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                        cd.getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                        cd.getPaymentMethodData().getCredit().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getCredit().setTotalValue(refundAmount);
                        break;
                    case OnlineSettlement:
                        cd.getPaymentMethodData().getOnlineSettlement().setTotalValue(refundAmount);
                        break;
                    case IOU:
                        cd.getPaymentMethodData().getIou().setTotalValue(refundAmount);
                        break;
                    default:
                        // For any other payment methods not explicitly handled
                        break;
                }

                paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().add(cd);
            }
        }
    }

    /**
     * Initialize payment method data from original payments for cancellation
     * Based on pharmacy return pattern - adapts payment details from original bill
     */
    private void initializeCancellationPaymentFromOriginalPayments(List<Payment> originalPayments) {
        if (originalPayments == null || originalPayments.isEmpty()) {
            return;
        }

        // Initialize payment method data if not already done
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }

        // Handle single payment method scenario
        if (originalPayments.size() == 1) {
            Payment originalPayment = originalPayments.get(0);

            // Initialize payment method data based on original payment method (using absolute values for UI display)
            switch (originalPayment.getPaymentMethod()) {
                case Cash:
                    paymentMethodData.getCash().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Card:
                    paymentMethodData.getCreditCard().setInstitution(originalPayment.getBank());
                    paymentMethodData.getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                    paymentMethodData.getCreditCard().setComment(originalPayment.getComments());
                    paymentMethodData.getCreditCard().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Cheque:
                    paymentMethodData.getCheque().setInstitution(originalPayment.getBank());
                    paymentMethodData.getCheque().setDate(originalPayment.getChequeDate());
                    paymentMethodData.getCheque().setNo(originalPayment.getChequeRefNo());
                    paymentMethodData.getCheque().setComment(originalPayment.getComments());
                    paymentMethodData.getCheque().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case Slip:
                    paymentMethodData.getSlip().setInstitution(originalPayment.getBank());
                    paymentMethodData.getSlip().setDate(originalPayment.getPaymentDate());
                    paymentMethodData.getSlip().setReferenceNo(originalPayment.getReferenceNo());
                    paymentMethodData.getSlip().setComment(originalPayment.getComments());
                    paymentMethodData.getSlip().setTotalValue(Math.abs(bill.getNetTotal()));
                    break;
                case ewallet:
                    paymentMethodData.getEwallet().setInstitution(
                        originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                    paymentMethodData.getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                    paymentMethodData.getEwallet().setNo(originalPayment.getReferenceNo());
                    paymentMethodData.getEwallet().setReferralNo(originalPayment.getPolicyNo());
                    paymentMethodData.getEwallet().setTotalValue(Math.abs(bill.getNetTotal()));
                    paymentMethodData.getEwallet().setComment(originalPayment.getComments());
                    break;
                case PatientDeposit:
                    if (bill != null) {
                        paymentMethodData.getPatient_deposit().setTotalValue(Math.abs(bill.getNetTotal()));
                    }
                    paymentMethodData.getPatient_deposit().setComment(originalPayment.getComments());
                    // Load and set the PatientDeposit object for displaying balance
                    if (bill != null && bill.getPatient() != null) {
                        paymentMethodData.getPatient_deposit().setPatient(bill.getPatient());
                        com.divudi.core.entity.PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                                bill.getPatient(), sessionController.getDepartment());
                        if (pd != null && pd.getId() != null) {
                            paymentMethodData.getPatient_deposit().getPatient().setHasAnAccount(true);
                            paymentMethodData.getPatient_deposit().setPatientDepost(pd);
                        }
                    }
                    break;
                case Credit:
                    paymentMethodData.getCredit().setInstitution(originalPayment.getCreditCompany());
                    paymentMethodData.getCredit().setReferenceNo(originalPayment.getReferenceNo());
                    paymentMethodData.getCredit().setReferralNo(originalPayment.getPolicyNo());
                    paymentMethodData.getCredit().setTotalValue(Math.abs(bill.getNetTotal()));
                    paymentMethodData.getCredit().setComment(originalPayment.getComments());
                    break;
                case Staff:
                    Staff staffForCredit = originalPayment.getToStaff();
                    if (staffForCredit == null && bill != null) {
                        staffForCredit = bill.getToStaff();
                    }

                    // Debug logging for Staff Credit
                    System.out.println("Staff Credit - Final Staff: " +
                        (staffForCredit != null ? staffForCredit.getPerson().getName() : "null"));

                    // Set staff member details for Staff Credit
                    paymentMethodData.getStaffCredit().setToStaff(staffForCredit);
                    paymentMethodData.getStaffCredit().setTotalValue(Math.abs(bill.getNetTotal()));
                    paymentMethodData.getStaffCredit().setComment(originalPayment.getComments());

                    JsfUtil.addSuccessMessage("Auto-populated Staff Credit: " +
                        (staffForCredit != null ? staffForCredit.getPerson().getName() : "No staff found"));
                    break;
                case Staff_Welfare:
                    Staff staffForWelfare = originalPayment.getToStaff();
                    if (staffForWelfare == null && bill != null) {
                        staffForWelfare = bill.getToStaff();
                    }

                    // Debug logging
                    System.out.println("Staff Welfare - Original Payment ToStaff: " +
                        (originalPayment.getToStaff() != null ? originalPayment.getToStaff().getPerson().getName() : "null"));
                    System.out.println("Staff Welfare - Bill ToStaff: " +
                        (bill.getToStaff() != null ? bill.getToStaff().getPerson().getName() : "null"));
                    System.out.println("Staff Welfare - Final Staff: " +
                        (staffForWelfare != null ? staffForWelfare.getPerson().getName() : "null"));

                    // Set staff member details for Staff Welfare
                    paymentMethodData.getStaffWelfare().setToStaff(staffForWelfare);
                    paymentMethodData.getStaffWelfare().setTotalValue(Math.abs(bill.getNetTotal()));
                    paymentMethodData.getStaffWelfare().setComment(originalPayment.getComments());

                    JsfUtil.addSuccessMessage("Auto-populated Staff Welfare: " +
                        (staffForWelfare != null ? staffForWelfare.getPerson().getName() : "No staff found"));
                    break;
                default:
                    // For other payment methods, just set the total value
                    break;
            }
        } else {
            // Handle multiple payment methods scenario
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();

            // Create ComponentDetail for each original payment
            for (Payment originalPayment : originalPayments) {
                com.divudi.core.data.dataStructure.ComponentDetail cd = new com.divudi.core.data.dataStructure.ComponentDetail();
                cd.setPaymentMethod(originalPayment.getPaymentMethod());
                cd.setPaymentMethodData(new PaymentMethodData());
                double refundAmount = Math.abs(originalPayment.getPaidValue());
                cd.setTotalValue(refundAmount);

                // Populate payment method data for this component
                switch (originalPayment.getPaymentMethod()) {
                    case Cash:
                        cd.getPaymentMethodData().getCash().setTotalValue(refundAmount);
                        break;
                    case Card:
                        cd.getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                        cd.getPaymentMethodData().getCreditCard().setComment(originalPayment.getComments());
                        cd.getPaymentMethodData().getCreditCard().setTotalValue(refundAmount);
                        break;
                    case PatientDeposit:
                        cd.getPaymentMethodData().getPatient_deposit().setTotalValue(refundAmount);
                        if (bill != null && bill.getPatient() != null) {
                            cd.getPaymentMethodData().getPatient_deposit().setPatient(bill.getPatient());
                        }
                        break;
                    case Staff:
                        cd.getPaymentMethodData().getStaffCredit().setToStaff(originalPayment.getToStaff());
                        cd.getPaymentMethodData().getStaffCredit().setTotalValue(refundAmount);
                        break;
                    case Staff_Welfare:
                        cd.getPaymentMethodData().getStaffWelfare().setToStaff(originalPayment.getToStaff());
                        cd.getPaymentMethodData().getStaffWelfare().setTotalValue(refundAmount);
                        System.out.println("Multiple Payment - Staff Welfare: " +
                            (originalPayment.getToStaff() != null ? originalPayment.getToStaff().getPerson().getName() : "null"));
                        break;
                    // Add more payment methods as needed
                }

                paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().add(cd);
            }
        }
    }

    /**
     * Apply negative signs to payment data for cancellation (reversal)
     * Based on pharmacy return pattern - negates all payment values
     */
    private void applyCancellationSignToPaymentData() {
        PaymentMethodData data = getPaymentMethodData();
        if (data == null) {
            return;
        }

        PaymentMethod selectedPaymentMethod = getPaymentMethod();
        if (selectedPaymentMethod == null) {
            return;
        }

        if (selectedPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            com.divudi.core.data.dataStructure.ComponentDetail multiple = data.getPaymentMethodMultiple();
            if (multiple != null && multiple.getMultiplePaymentMethodComponentDetails() != null) {
                for (com.divudi.core.data.dataStructure.ComponentDetail component : multiple.getMultiplePaymentMethodComponentDetails()) {
                    if (component == null) {
                        continue;
                    }
                    negateComponentTotal(component);
                    negatePaymentMethodData(component.getPaymentMethodData());
                }
            }
        } else {
            negatePaymentMethodData(data);
        }
    }

    /**
     * Negate payment method data values for cancellation
     */
    private void negatePaymentMethodData(PaymentMethodData paymentMethodData) {
        if (paymentMethodData == null) {
            return;
        }

        negateComponentTotal(paymentMethodData.getCash());
        negateComponentTotal(paymentMethodData.getCreditCard());
        negateComponentTotal(paymentMethodData.getCheque());
        negateComponentTotal(paymentMethodData.getSlip());
        negateComponentTotal(paymentMethodData.getEwallet());
        negateComponentTotal(paymentMethodData.getPatient_deposit());
        negateComponentTotal(paymentMethodData.getCredit());
        negateComponentTotal(paymentMethodData.getStaffCredit());
        negateComponentTotal(paymentMethodData.getStaffWelfare());
        negateComponentTotal(paymentMethodData.getOnlineSettlement());
        negateComponentTotal(paymentMethodData.getIou());
    }

    /**
     * Negate component detail total value
     */
    private void negateComponentTotal(com.divudi.core.data.dataStructure.ComponentDetail componentDetail) {
        if (componentDetail == null) {
            return;
        }
        componentDetail.setTotalValue(0 - Math.abs(componentDetail.getTotalValue()));
    }

    /**
     * Transfer staff member data from toStaff property to paymentMethodData
     * This fixes the issue where staff member wasn't being saved to Payment records
     */
    private void transferStaffDataToPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }

        if (toStaff != null) {
            switch (paymentMethod) {
                case Staff_Welfare:
                    paymentMethodData.getStaffWelfare().setToStaff(toStaff);
                    paymentMethodData.getStaffWelfare().setTotalValue(batchBill.getNetTotal());
                    break;
                case Staff:
                case OnCall:
                    paymentMethodData.getStaffCredit().setToStaff(toStaff);
                    paymentMethodData.getStaffCredit().setTotalValue(batchBill.getNetTotal());
                    break;
                default:
                    // For other payment methods, toStaff might be used differently or not at all
                    break;
            }
        }

        // Also transfer credit company if using Credit payment method
        if (paymentMethod == PaymentMethod.Credit && creditCompany != null) {
            paymentMethodData.getCredit().setInstitution(creditCompany);
            paymentMethodData.getCredit().setTotalValue(batchBill.getNetTotal());
        }
    }

    public void cancelSingleBillWhenCancellingPackageBatchBill(Bill originalBill, Bill cancellationBatchBill) {
        if (originalBill == null && originalBill == null) {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(originalBill.getDepartment(), BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);

        CancelledBill individualCancelltionBill = new CancelledBill();
        individualCancelltionBill.copy(originalBill);
        individualCancelltionBill.invertAndAssignValuesFromOtherBill(originalBill);
        individualCancelltionBill.setBillType(BillType.OpdBill);
        individualCancelltionBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        individualCancelltionBill.setDeptId(deptId);
        individualCancelltionBill.setInsId(deptId);
        individualCancelltionBill.setPaymentMethod(cancellationBatchBill.getPaymentMethod());
        individualCancelltionBill.setBilledBill(cancellationBatchBill);
        individualCancelltionBill.setBillDate(new Date());
        individualCancelltionBill.setBillTime(new Date());
        individualCancelltionBill.setCreatedAt(new Date());
        individualCancelltionBill.setCreater(getSessionController().getLoggedUser());
        individualCancelltionBill.setDepartment(getSessionController().getDepartment());
        individualCancelltionBill.setInstitution(getSessionController().getInstitution());
        individualCancelltionBill.setForwardReferenceBill(cancellationBatchBill);
        individualCancelltionBill.setComments(comment);
        billService.saveBill(individualCancelltionBill);

        List<BillItem> list = createBillItemsForOpdBatchBillCancellation(originalBill, individualCancelltionBill);
        try {
            individualCancelltionBill.setBillItems(list);
        } catch (Exception e) {

        }
        billService.saveBill(individualCancelltionBill);

        originalBill.setCancelled(true);
        originalBill.setCancelledBill(individualCancelltionBill);
        billService.saveBill(originalBill);
    }

    private List<BillItem> createBillItemsForOpdBatchBillCancellation(Bill originalBill, Bill cancellationBill) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem originalBillItem : originalBill.getBillItems()) {
            BillItem newBillItem = new BillItem();
            newBillItem.setBill(cancellationBill);
            newBillItem.setItem(originalBillItem.getItem());
            newBillItem.setNetValue(0 - originalBillItem.getNetValue());
            newBillItem.setGrossValue(0 - originalBillItem.getGrossValue());
            newBillItem.setHospitalFee(0 - originalBillItem.getHospitalFee());
            newBillItem.setCollectingCentreFee(0 - originalBillItem.getCollectingCentreFee());
            newBillItem.setStaffFee(0 - originalBillItem.getStaffFee());
            newBillItem.setRate(0 - originalBillItem.getRate());
            newBillItem.setVat(0 - originalBillItem.getVat());
            newBillItem.setVatPlusNetValue(0 - originalBillItem.getVatPlusNetValue());
            newBillItem.setCatId(originalBillItem.getCatId());
            newBillItem.setDeptId(originalBillItem.getDeptId());
            newBillItem.setInsId(originalBillItem.getInsId());
            newBillItem.setDiscount(0 - originalBillItem.getDiscount());
            newBillItem.setQty(0 - originalBillItem.getQty());
            newBillItem.setRate(originalBillItem.getRate());
            newBillItem.setCreatedAt(new Date());
            newBillItem.setCreater(getSessionController().getLoggedUser());
            newBillItem.setPaidForBillFee(originalBillItem.getPaidForBillFee());
            newBillItem.setReferanceBillItem(originalBillItem);
            billItemFacade.create(newBillItem);

            cancelBillComponents(originalBill, cancellationBill, originalBillItem, newBillItem);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + originalBillItem.getId();
            List<BillFee> originalBillFees = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(originalBill, cancellationBill, originalBillItem, newBillItem, originalBillFees);

            list.add(newBillItem);

        }

        return list;
    }

    public void cancelBillFee(Bill originalBill, Bill cancellationBill, BillItem originalBillItem, BillItem cancellationBillItem, List<BillFee> originalBillFees) {
        for (BillFee originalBillFee : originalBillFees) {
            BillFee newBillFee = new BillFee();
            newBillFee.setFee(originalBillFee.getFee());
            newBillFee.setPatienEncounter(originalBillFee.getPatienEncounter());
            newBillFee.setPatient(originalBillFee.getPatient());
            newBillFee.setDepartment(originalBillFee.getDepartment());
            newBillFee.setInstitution(originalBillFee.getInstitution());
            newBillFee.setSpeciality(originalBillFee.getSpeciality());
            newBillFee.setStaff(originalBillFee.getStaff());
            newBillFee.setReferenceBillFee(originalBillFee);
            newBillFee.setBill(cancellationBill);
            newBillFee.setBillItem(originalBillItem);
            newBillFee.setFeeValue(0 - originalBillFee.getFeeValue());
            newBillFee.setFeeGrossValue(0 - originalBillFee.getFeeGrossValue());
            newBillFee.setFeeDiscount(0 - originalBillFee.getFeeDiscount());
            newBillFee.setSettleValue(0 - originalBillFee.getSettleValue());
            newBillFee.setFeeVat(0 - originalBillFee.getFeeVat());
            newBillFee.setFeeVatPlusValue(0 - originalBillFee.getFeeVatPlusValue());
            newBillFee.setCreatedAt(new Date());
            newBillFee.setCreater(getSessionController().getLoggedUser());
            getBillFeeFacade().create(newBillFee);
        }
    }

    private void cancelBillComponents(Bill originalBill, Bill cancellationBill, BillItem originalBillItem, BillItem newBillItem) {
        String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + originalBill.getId();
        List<BillComponent> billComponents = billComponentFacade.findByJpql(sql);
        if (billComponents == null) {
            billComponents = new ArrayList<>();
        }
        for (BillComponent originalBillComponent : billComponents) {
            BillComponent newBillComponent = new BillComponent();
            newBillComponent.setCatId(originalBillComponent.getCatId());
            newBillComponent.setDeptId(originalBillComponent.getDeptId());
            newBillComponent.setInsId(originalBillComponent.getInsId());
            newBillComponent.setDepartment(originalBillComponent.getDepartment());
            newBillComponent.setDeptId(originalBillComponent.getDeptId());
            newBillComponent.setInstitution(originalBillComponent.getInstitution());
            newBillComponent.setItem(originalBillComponent.getItem());
            newBillComponent.setName(originalBillComponent.getName());
            newBillComponent.setPackege(originalBillComponent.getPackege());
            newBillComponent.setSpeciality(originalBillComponent.getSpeciality());
            newBillComponent.setStaff(originalBillComponent.getStaff());
            newBillComponent.setBill(cancellationBill);
            newBillComponent.setBillItem(newBillItem);
            newBillComponent.setCreatedAt(new Date());
            newBillComponent.setCreater(getSessionController().getLoggedUser());
            billComponentFacade.create(newBillComponent);

        }

    }

    private boolean errorsPresentOnOpdBatchBillCancellation() {
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }
        batchBill = billService.reloadBill(batchBill);
        if (batchBill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return true;
        }
        if (batchBill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return true;
        }
        List<Bill> individualBills = billService.fetchIndividualBillsOfBatchBill(batchBill);
        if (individualBills == null) {
            JsfUtil.addErrorMessage("No Individual Bills");
            return true;
        }
        for (Bill individualBill : individualBills) {
            if (individualBill.isCancelled()) {
                JsfUtil.addErrorMessage("One individual bill of this batch bill is already Cancelled. Can not cancel Batch Bill !!! ");
                return true;
            }
            if (individualBill.isRefunded()) {
                JsfUtil.addErrorMessage("One individual bill of this batch bill is already Refunded. Can not cancel Batch Bill !!! ");
                return true;
            }
        }
        return false;
    }

    private boolean errorsPresentOnOpdBillCancellation() {
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }
        bill = billService.reloadBill(bill);
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return true;
        }
        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return true;
        }
        batchBill = bill.getBackwardReferenceBill();
        if (batchBill == null) {
            batchBill = billService.fetchBatchBillOfIndividualBill(bill);
        }
        if (batchBill == null) {
            JsfUtil.addErrorMessage("No Batch Bills");
            return true;
        }
        if (batchBill.isCancelled()) {
            JsfUtil.addErrorMessage("Package Batch Bill is cancelled. Can not cancel");
            return true;
        }
        return false;
    }

    private boolean performErrorChecks() {
        if (checkPatientDetails()) {
            return true;
        }
        BillValidation bv = paymentService.checkForErrorsInPaymentDetailsForInBills(getPaymentMethod(), getPaymentMethodData(), netTotal, getPatient(), sessionController.getDepartment());

        if (bv.isErrorPresent()) {
            JsfUtil.addErrorMessage(bv.getErrorMessage());
            return true;
        } else {
            if (bv.getCompany() != null && getCreditCompany() == null) {
                setCreditCompany(bv.getCompany());
            }
        }
        return false;
    }

    public void settleBill() {
//        if (validatePaymentMethodDeta()) {
//            return;
//        }
//        if (errorCheck()) {
//            return;
//        }

        if (performErrorChecks()) {
            return;
        }

        savePatient();
        if (getBillBean().calculateNumberOfBillsPerOrder(getLstBillEntries()) == 1) {
            BilledBill temp = new BilledBill();
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getTransDepartment(), temp);
//            getBillBean().saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser());
            b.setBillItems(getBillBean().saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser()));
            getBillBean().calculateBillItems(b, getLstBillEntries());
            getBills().add(b);

        } else {
            putToBills();
        }

        saveBatchBill();

        // Transfer staff member data to paymentMethodData before payment creation
        transferStaffDataToPaymentMethodData();

        List<Payment> ps = paymentService.createPayment(getBatchBill(), paymentMethodData);
        paymentService.updateBalances(ps);
        payments = ps;
//        calculateBillfeePayments(lstBillFees, payments.get(0));

//        if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff_Welfare) {
//            staffBean.updateStaffWelfare(toStaff, batchBill.getNetTotal());
//            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
//        } else if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff) {
//            staffBean.updateStaffCredit(toStaff, batchBill.getNetTotal());
//            JsfUtil.addSuccessMessage("Staff Credit Updated");
//        }
//
//        if (paymentMethod == PaymentMethod.PatientDeposit) {
//            if (getPatient().getRunningBalance() != null) {
//                getPatient().setRunningBalance(getPatient().getRunningBalance() - netTotal);
//            } else {
//                getPatient().setRunningBalance(0.0 - netTotal);
//            }
//            getPatientFacade().edit(getPatient());
//        }
        saveBillItemSessions();
//        drawerController.updateDrawerForIns(ps);
        clearBillItemValues();

        JsfUtil.addSuccessMessage(
                "Bill Saved");
        printPreview = true;
    }

    private Bill saveBill(Department bt, BilledBill temp) {

        //getCurrent().setCashBalance(cashBalance);
        //getCurrent().setCashPaid(cashPaid);
        //  temp.setBillType(bt);
        temp.setBillType(BillType.OpdBill);
        temp.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        temp.setBillPackege((Packege) currentBillItem.getItem());

        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setStaff(staff);
        temp.setReferredBy(referredBy);
        temp.setCreditCompany(creditCompany);

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPatient(getPatient());
//        temp.setPatientEncounter(patientEncounter);
        temp.setPaymentMethod(paymentMethod);
        temp.setPaymentScheme(getPaymentScheme());

        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());

        String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(bt, BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        temp.setDeptId(billNumber);
        temp.setInsId(billNumber);
        temp.setComments(comment);

        if (temp.getId() == null) {
            // Initialize balance field for credit bills before creating
            if (paymentMethod == PaymentMethod.Credit) {
                double totalAmount = Math.abs(temp.getNetTotal());
                if (temp.getVat() != 0.0) {
                    totalAmount += Math.abs(temp.getVat());
                }
                temp.setBalance(totalAmount);
            }
            getFacade().create(temp);
        }
        return temp;

    }

    public List<Payment> createPayments(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
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

                    case Agent:
                    case Credit:
                        p.setReferenceNo(cd.getPaymentMethodData().getCredit().getReferralNo());
                        p.setComments(cd.getPaymentMethodData().getCredit().getComment());
                    case PatientDeposit:
                        if (getPatient().getRunningBalance() != null) {
                            getPatient().setRunningBalance(getPatient().getRunningBalance() - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        } else {
                            getPatient().setRunningBalance(0.0 - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        }
                        getPatientFacade().edit(getPatient());
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:

                case Agent:
                case Credit:
                    p.setReferenceNo(paymentMethodData.getCredit().getReferralNo());
                    p.setComments(paymentMethodData.getCredit().getComment());
                case PatientDeposit:
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    public void calculateBillfeePayments(List<BillFee> billFees, Payment p) {
        for (BillFee bf : billFees) {
            bf.setSettleValue(bf.getFeeValue());
            setBillFeePaymentAndPayment(bf.getFeeValue(), bf, p);
            getBillFeeFacade().edit(bf);

        }
    }

    public void setBillFeePaymentAndPayment(double amount, BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(amount);
        bfp.setInstitution(bf.getBillItem().getItem().getTransInstitution());
        bfp.setDepartment(bf.getBillItem().getItem().getTransDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        billFeePaymentFacade.create(bfp);
    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                // Fixed: Sum only the selected payment method's value for this component
                multiplePaymentMethodTotalValue += calculateSelectedPaymentTotal(cd);
            }
            remainAmount = total - multiplePaymentMethodTotalValue;
            return total - multiplePaymentMethodTotalValue;

        }
        remainAmount = total;
        return total;
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        System.out.println("recieveRemainAmountAutomatically");
        System.out.println("paymentMethod = " + paymentMethod);
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            switch (pm.getPaymentMethod()) {
                case Cash:
                    pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
                    break;
                case Card:
                    pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
                    break;
                case Cheque:
                    pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
                    break;
                case Slip:
                    pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
                    break;
                case ewallet:
                    pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
                    break;
                case PatientDeposit:
                    System.out.println("patient deposit");
                    pm.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                    PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
                    System.out.println("pd = " + pd);
                    pm.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    System.out.println("pm.getPaymentMethodData().getPatient_deposit().getTotalValue() = " + pm.getPaymentMethodData().getPatient_deposit().getTotalValue());

                    if (pm.getPaymentMethodData().getPatient_deposit().getTotalValue() < 0.01) {
                        if (remainAmount >= pm.getPaymentMethodData().getPatient_deposit().getPatientDepost().getBalance()) {
                            pm.getPaymentMethodData().getPatient_deposit().setTotalValue(pm.getPaymentMethodData().getPatient_deposit().getPatientDepost().getBalance());
                        } else {
                            pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
                        }
                    } else if (pm.getPaymentMethodData().getPatient_deposit().getTotalValue() > pm.getPaymentMethodData().getPatient_deposit().getPatientDepost().getBalance()) {
                        pm.getPaymentMethodData().getPatient_deposit().setTotalValue(pm.getPaymentMethodData().getPatient_deposit().getPatientDepost().getBalance());
                    }

                    break;
                case Credit:
                    pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
                    break;
                case Staff:
                    pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
                    break;
                case OnlineSettlement:
                    pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(remainAmount);
                    break;
                case Staff_Welfare:
                    pm.getPaymentMethodData().getStaffWelfare().setTotalValue(remainAmount);
                    break;
                case IOU:
                    pm.getPaymentMethodData().getIou().setTotalValue(remainAmount);
                    break;
                case YouOweMe:
                    // YouOweMe maps to the same IOU field in PaymentMethodData
                    pm.getPaymentMethodData().getIou().setTotalValue(remainAmount);
                    break;
                case OnCall:
                    pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + pm.getPaymentMethod());
            }

        }
        System.out.println("this = " + this);
        listnerForPaymentMethodChange();

    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null ||
            paymentMethodData == null ||
            paymentMethodData.getPaymentMethodMultiple() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
    }

    @Deprecated //Instead use checkPaymentDetails
    private boolean errorCheck() {

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Name to Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getName() == null
                    || getPatient().getPerson().getName().trim().equals("")
                    || getPatient().getPerson().getSex() == null
                    || getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                JsfUtil.addErrorMessage("Please select title");
                return true;
            }
            if (getPatient().getPerson().getSex() == null) {
                JsfUtil.addErrorMessage("Please select gender");
                return true;
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Please select patient date of birth");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Phone Number to save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getPhone() == null) {
                JsfUtil.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
            if (getPatient().getPerson().getPhone().trim().equals("")) {
                JsfUtil.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Area to save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getArea() == null) {
                JsfUtil.addErrorMessage("Please Select Pataient Area");
                return true;
            }
            if (getPatient().getPerson().getArea().getName().trim().equals("")) {
                JsfUtil.addErrorMessage("Please Select Patient Area");
                return true;
            }
        }

        if (getLstBillEntries().isEmpty()) {

            JsfUtil.addErrorMessage("No investigations are added to the bill to settle");
            return true;
        }

        if (getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }
        if (paymentMethod == PaymentMethod.Credit && creditCompany == null) {
            JsfUtil.addErrorMessage("Plase Select Credit Company");
            return true;
        }

        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (!getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
            double runningBalance;
            if (getPatient().getRunningBalance() != null) {
                runningBalance = getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.Credit) {
            if (creditCompany == null && collectingCentre == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company or Collecting centre.");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Package Bill – Credit Company Policy Number required", false)) {
            if (paymentMethod == PaymentMethod.Credit && paymentMethodData.getCredit().getReferralNo().trim().equalsIgnoreCase("")) {
                JsfUtil.addErrorMessage("Plase Add the Policy No");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }

            if (toStaff.getCurrentCreditValue() + netTotal > toStaff.getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + netTotal > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
                    if (!getPatient().getHasAnAccount()) {
                        JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                        return true;
                    }
                    double creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
                    double runningBalance;
                    if (getPatient().getRunningBalance() != null) {
                        runningBalance = getPatient().getRunningBalance();
                    } else {
                        runningBalance = 0.0;
                    }
                    double availableForPurchase = runningBalance + creditLimitAbsolute;

                    if (cd.getPaymentMethodData().getPatient_deposit().getTotalValue() > availableForPurchase) {
                        JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                        return true;
                    }

                }
                if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                    if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (cd.getPaymentMethodData().getStaffCredit().getToStaff().getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > cd.getPaymentMethodData().getStaffCredit().getToStaff().getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                }
                // Fixed: Sum only the selected payment method's value for this component
                multiplePaymentMethodTotalValue += calculateSelectedPaymentTotal(cd);
            }
            double differenceOfBillTotalAndPaymentValue = netTotal - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }

        }

        return false;
    }

    /**
     * Calculates the total payment value for the selected payment method only.
     * This ensures accurate validation by summing only the relevant payment method value.
     * Copied from OpdBillController.java to fix multiple payment validation error.
     *
     * @param cd ComponentDetail containing payment method and associated data
     * @return Total value for the selected payment method, or 0.0 if invalid
     */
    private double calculateSelectedPaymentTotal(ComponentDetail cd) {
        if (cd == null || cd.getPaymentMethodData() == null || cd.getPaymentMethod() == null) {
            return 0.0;
        }

        switch (cd.getPaymentMethod()) {
            case Cash:
                return cd.getPaymentMethodData().getCash().getTotalValue();
            case Card:
                return cd.getPaymentMethodData().getCreditCard().getTotalValue();
            case Cheque:
                return cd.getPaymentMethodData().getCheque().getTotalValue();
            case ewallet:
                return cd.getPaymentMethodData().getEwallet().getTotalValue();
            case PatientDeposit:
                return cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
            case Slip:
                return cd.getPaymentMethodData().getSlip().getTotalValue();
            case Staff:
                return cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            case Staff_Welfare:
                return cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
            case Credit:
                return cd.getPaymentMethodData().getCredit().getTotalValue();
            case OnlineSettlement:
                return cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
            case IOU:
                return cd.getPaymentMethodData().getIou().getTotalValue();
            case YouOweMe:
                // YouOweMe maps to the same IOU field in PaymentMethodData
                return cd.getPaymentMethodData().getIou().getTotalValue();
            default:
                // Return 0.0 for unexpected or unsupported payment methods
                return 0.0;
        }
    }

    private void addEntry(BillItem bi) {
        if (bi == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (bi.getItem() == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }

        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(bi);
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bi));
        addingEntry.setLstBillFees(getBillBean().billFeefromBillItemPackage(bi, currentBillItem.getItem()));
        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bi));
        getLstBillEntries().add(addingEntry);
        bi.setRate(getBillBean().billItemRate(addingEntry));
        bi.setQty(1.0);
        bi.setNetValue(bi.getRate() * bi.getQty()); // Price == Rate as Qty is 1 here

        calTotals();
        if (bi.getNetValue() == 0.0) {
            JsfUtil.addErrorMessage("Please enter the rate");
            return;
        }
        //      clearBillItemValues();
        recreateBillItems();
    }

    public void addToBill() {
        if (getLstBillEntries().size() > 0) {
            JsfUtil.addErrorMessage("You can not add more than on package at a time create new bill");
            return;
        }
        if (getCurrentBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select an Item");
            return;
        }

        List<Item> itemList = getBillBean().itemFromPackage(currentBillItem.getItem());
        for (Item i : itemList) {
            if (i.getDepartment() == null) {
                JsfUtil.addErrorMessage("Under administration, add a Department for item " + i.getName());
                return;
            }

            BillItem tmp = new BillItem();
            tmp.setItem(i);
            addEntry(tmp);
        }
        JsfUtil.addSuccessMessage("Item Added");
    }

    public void clearBillItemValues() {
        setCurrentBillItem(null);
        recreateBillItems();
    }

    private void recreateBillItems() {
        //Only remove Total and BillComponenbts,Fee and Sessions. NOT bill Entries
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;

        //billTotal = 0.0;
    }

    public void calTotals() {
        double tot = 0.0;
        double net = 0.0;
        double dis = 0.0;

        for (BillEntry be : getLstBillEntries()) {
            BillItem bi = be.getBillItem();
            bi.setDiscount(0.0);
            bi.setGrossValue(0.0);
            bi.setNetValue(0.0);

            for (BillFee bf : be.getLstBillFees()) {
                tot += bf.getFeeGrossValue();
                net += bf.getFeeValue();
                bf.getBillItem().setNetValue(bf.getBillItem().getNetValue() + bf.getFeeValue());
                bf.getBillItem().setGrossValue(bf.getBillItem().getGrossValue() + bf.getFeeGrossValue());
            }
        }
        setDiscount(dis);
        setTotal(net);
        setNetTotal(net);
        if (paymentMethod == PaymentMethod.Cash) {
            cashBalance = getTenderedAmount() - getNetTotal();
        }
        System.out.println("Cash Balance = " + getCashBalance());
    }

    public void feeChanged() {
        lstBillItems = null;
        getLstBillItems();
        calTotals();
    }

    public void clearBillValues() {
        setPatient(null);
        setReferredBy(null);
        setCreditCompany(null);
        setYearMonthDay(null);
        setBills(null);
        paymentMethodData = null;
        payments = null;
        setChequeBank(null);

        setCurrentBillItem(null);
        setLstBillComponents(null);
        setLstBillEntries(null);
        setLstBillFees(null);
        setStaff(null);
        setToStaff(null);
        lstBillEntries = new ArrayList<>();
        //   setForeigner(false);
        calTotals();

        setTenderedAmount(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);
        printPreview = false;
    }

    public void prepareNewBill() {
        clearBillItemValues();
        clearBillValues();
        setPrintPreview(true);
        printPreview = false;
    }

    public void removeBillItem() {
        currentBillItem = null;
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        lstBillEntries = null;
        setTenderedAmount(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);
        setTotal(0.0);
        setNetTotal(0.0);
    }

    public void removeBillItem(Item itm) {
        List<Item> itemList = new ArrayList<>();
        for (BillEntry be : getLstBillEntries()) {
            itemList.add(be.getBillItem().getItem());
        }
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        lstBillEntries = null;
        for (Item i : itemList) {
            if (!i.getName().equals(itm.getName())) {
                if (i.getDepartment() == null) {
                    JsfUtil.addErrorMessage("Under administration, add a Department for item " + i.getName());
                    return;
                }

                BillItem tmp = new BillItem();
                tmp.setItem(i);
                addEntry(tmp);
            }
        }
        calTotals();
    }

    public List<BillItem> fillPackageBillItem(Bill bill) {
        List<BillItem> billItem = new ArrayList<>();

        String jpql;
        Map m = new HashMap();
        jpql = "select bi from BillItem bi "
                + " where bi.retired = false "
                + " and bi.bill.backwardReferenceBill =:pBill";
        m.put("pBill", bill);

        billItem = getBillItemFacade().findByJpql(jpql, m);

        return billItem;
    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<BillEntry>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);

            }
        }
        lstBillEntries = temp;
        lstBillComponents = getBillBean().billComponentsFromBillEntries(lstBillEntries);
        lstBillFees = getBillBean().billFeesFromBillEntries(lstBillEntries);
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable = true;
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    // <editor-fold defaultstate="collapsed" desc="navigater">
    String navigateToNewOpdPackageBill(Patient patient) {
        clearBillItemValues();
        clearBillValues();
        printPreview = false;
        this.patient = patient;
        return "/opd/opd_bill_package?faces-redirect=true";
    }

    public String navigateToSearchOpdPackageBills() {
        batchBill = null;
        bills = null;
        return "/opd/opd_package_bill_search?faces-redirect=true";
    }

    public String navigateToManageOpdPackageBatchBill(Bill bb) {
        System.out.println("navigateToManageOpdPackageBatchBill = ");
        if (bb == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        if (bb.getId() == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        System.out.println("bb.getBillTypeAtomic() = " + bb.getBillTypeAtomic());
        if (bb.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No bill type");
            return null;
        }
        if (bb.getBillTypeAtomic() != BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT) {
            JsfUtil.addErrorMessage("No bill type");
            return null;
        }
        batchBill = bb;
        Long batchBillId = bb.getId();
        batchBill = billFacade.find(batchBillId);
        bills = billService.fetchIndividualBillsOfBatchBill(batchBill);
        payments = billService.fetchBillPayments(batchBill);
        for (Bill b : bills) {
            billService.initiateBillItemsAndBillFees(b);
        }
        duplicatePrint = true;
        return "/opd/opd_package_batch_bill_print?faces-redirect=true";
    }

    public String navigateToCancelOpdPackageBatchBill() {
        if (batchBill == null) {
            JsfUtil.addErrorMessage("No Batch bill is selected");
            return "";
        }
        bills = billService.fetchIndividualBillsOfBatchBill(batchBill);

        // Load original payment details from batch bill
        originalBillPayments = billService.fetchBillPayments(batchBill);

        // Initialize cancellation payment data from original payments using batchBill for net total
        initializeCancellationPaymentFromOriginalPayments(originalBillPayments, batchBill);

        // Allow all payment methods (remove restriction)
        paymentMethods = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            paymentMethods.add(pm);
        }

        // Set default payment method to original or Cash as fallback
        if (originalBillPayments != null && !originalBillPayments.isEmpty()) {
            paymentMethod = originalBillPayments.get(0).getPaymentMethod();
        } else {
            paymentMethod = PaymentMethod.Cash;
        }

        patient = batchBill.getPatient();
        comment = null;
        printPreview = false;
        batchBillCancellationStarted = false;
        return "/opd/opd_package_batch_bill_cancel?faces-redirect=true";
    }

    public String navigateToMedicalPakageBillingFromMenu() {
        clearBillValues();
        setPatient(getPatient());
//        appointmentController.prepereForInwardAppointPatient();
//        appointmentController.setSearchedPatient(getCurrent());
//        appointmentController.getCurrentAppointment().setPatient(getCurrent());
//        appointmentController.getCurrentBill().setPatient(getCurrent());
        return "/opd_bill_package_medical?faces-redirect=true";
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getter and Setter">
    public List<ItemLight> getOpdPackages() {
        if (opdPackages == null) {
            opdPackages = itemApplicationController.getPackages();
        }
        return opdPackages;
    }

    public void setOpdPackages(List<ItemLight> opdPackages) {
        this.opdPackages = opdPackages;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    private BillFacade getEjbFacade() {
        return billFacade;
    }

    private SessionController getSessionController() {
        return sessionController;
    }

    public BillPackageController() {
    }

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("opd/opd_bill_package");
        metadata.setPageName("OPD Bill Package");
        metadata.setDescription("OPD package billing with gender-based package selection and multiple payment methods");
        metadata.setControllerClass("BillPackageController");

        // Configuration Options - Patient Management
        metadata.addConfigOption(new ConfigOptionInfo(
            "Save the Patient with Patient Status",
            "Enables patient status field for local/foreign classification",
            "Lines 347, 357, 466-470: Patient status field visibility and display",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable patient specific status management in the system",
            "Shows patient-specific status badges and management features",
            "Lines 419-423: Patient specific status badge display",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable blacklist patient management in the system",
            "Enables blacklist patient management features globally",
            "Line 425: Blacklist patient badge rendering (combined with OPD-specific setting)",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable blacklist patient management for OPD from the system",
            "Enables blacklist patient management specifically for OPD modules",
            "Line 425: Combined with global blacklist setting for OPD blacklist badge",
            OptionScope.APPLICATION
        ));

        // Configuration Options - Package Management
        metadata.addConfigOption(new ConfigOptionInfo(
            "Package bill – Reloading of Packages with Consideration of Gender",
            "Shows gender badges in package selection and filters packages by patient gender",
            "Line 540: Gender badge column visibility in package selection",
            OptionScope.APPLICATION
        ));

        // Privileges
        metadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to page configuration",
            "Config button visibility"
        ));

        // Register the metadata
        pageMetadataRegistry.registerPage(metadata);
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
        calTotals();
    }

    public Doctor getReferredBy() {

        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public Institution getCreditCompany() {

        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<BillComponent> getLstBillComponents() {
        if (lstBillComponents == null) {
            lstBillComponents = getBillBean().billComponentsFromBillEntries(getLstBillEntries());
        }

        return lstBillComponents;
    }

    public void setLstBillComponents(List<BillComponent> lstBillComponents) {
        this.lstBillComponents = lstBillComponents;
    }

    public List<BillFee> getLstBillFees() {
        if (lstBillFees == null) {
            lstBillFees = getBillBean().billFeesFromBillEntries(getLstBillEntries());
        }

        return lstBillFees;
    }

    public void setLstBillFees(List<BillFee> lstBillFees) {
        this.lstBillFees = lstBillFees;
    }

    public List<BillItem> getLstBillItems() {
        if (lstBillItems == null) {
            lstBillItems = new ArrayList<BillItem>();
        }
        return lstBillItems;
    }

    public void setLstBillItems(List<BillItem> lstBillItems) {
        this.lstBillItems = lstBillItems;
    }

    public List<BillEntry> getLstBillEntries() {
        if (lstBillEntries == null) {
            lstBillEntries = new ArrayList<BillEntry>();
        }
        return lstBillEntries;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Bill> getBills() {
        if (bills == null) {
            bills = new ArrayList<Bill>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    @Override
    public void listnerForPaymentMethodChange() {
        System.out.println("listenerForPaymentMethodChange for BillPackageController ");
        System.out.println("paymentMethod = " + paymentMethod);
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
            System.out.println("this = " + this);
        } else if (paymentMethod == PaymentMethod.Staff) {
            // Initialize staff credit payment method
            getPaymentMethodData().getStaffCredit().setTotalValue(netTotal);
            if (toStaff != null) {
                getPaymentMethodData().getStaffCredit().setToStaff(toStaff);
            }
        } else if (paymentMethod == PaymentMethod.Staff_Welfare) {
            // Initialize staff welfare payment method
            getPaymentMethodData().getStaffWelfare().setTotalValue(netTotal);
            if (toStaff != null) {
                getPaymentMethodData().getStaffWelfare().setToStaff(toStaff);
            }
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            if (getPaymentMethodData().getPatient_deposit().getTotalValue() < 0.01) {
                getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            }
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                System.out.println("pd = " + pd);
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    System.out.println("cd = " + cd);
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        System.out.println("cd = " + cd);
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
        calTotals();
    }

    public void setLstBillEntries(List<BillEntry> lstBillEntries) {
        this.lstBillEntries = lstBillEntries;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public Institution getChequeBank() {

        return chequeBank;
    }

    public void setChequeBank(Institution chequeBank) {
        this.chequeBank = chequeBank;
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

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;

    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;

    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;

    }

    public BillSearch getBillSearch() {
        return billSearch;
    }

    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public Institution getReferredByInstitution() {
        return referredByInstitution;
    }

    public void setReferredByInstitution(Institution referredByInstitution) {
        this.referredByInstitution = referredByInstitution;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public ItemLight getItemLight() {
        if (getCurrentBillItem().getItem() != null) {
            this.itemLight = new ItemLight(getCurrentBillItem().getItem());
        }
        return itemLight;
    }

    public void setItemLight(ItemLight itemLight) {
        this.itemLight = itemLight;
        if (this.itemLight != null) {
            getCurrentBillItem().setItem(itemController.findItem(this.itemLight.getId()));
        }
    }

    public void feeChangeListener(BillFee bf) {
        if (bf.getFeeGrossValue() == null) {
            bf.setFeeGrossValue(0.0);
//            return;
        }

        lstBillItems = null;
        getLstBillItems();
        bf.setTmpChangedValue(bf.getFeeGrossValue());
        calTotals();
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public List<Payment> getPayments() {
        if (payments == null) {
            payments = new ArrayList<>();
        }
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public List<Item> getMalePackaes() {
        return malePackaes;
    }

    public void setMalePackaes(List<Item> malePackaes) {
        this.malePackaes = malePackaes;
    }

    public List<Item> getFemalePackaes() {
        return femalePackaes;
    }

    public void setFemalePackaes(List<Item> femalePackaes) {
        this.femalePackaes = femalePackaes;
    }

    public List<Item> getBothPackaes() {
        return bothPackaes;
    }

    public void setBothPackaes(List<Item> bothPackaes) {
        this.bothPackaes = bothPackaes;
    }

    public List<Item> getGenderBasedPackaes() {
        if (packaes == null) {
            fillPackages();
        }
        if (configOptionApplicationController.getBooleanValueByKey("Package bill – Reloading of Packages with Consideration of Gender")) {
            if (getPatient() == null) {
                return packaes;
            } else if (getPatient().getPerson().getSex() == null) {
                return packaes;
            } else if (getPatient().getPerson().getSex() == Sex.Male) {
                return malePackaes;
            } else if (getPatient().getPerson().getSex() == Sex.Female) {
                return femalePackaes;
            } else {
                return bothPackaes;
            }
        }
        return packaes;
    }

    public void setPackaes(List<Item> packaes) {
        this.packaes = packaes;
    }

    public List<Item> getPackaes() {
        if (packaes == null) {
            packaes = itemController.getPackaes();
            fillPackages();
        }
        return packaes;
    }

    private List<Item> listOfTheNonExpiredPackages;

    public void reloadPackages() {
        itemController.reloadItems();
        itemApplicationController.reloadItems();
        fillPackages();
    }

    private void fillPackages() {
        packaes = itemController.getPackaes();
        if (packaes == null) {
            return;
        }

        getListOfTheNonExpiredPackages();

        if (configOptionApplicationController.getBooleanValueByKey("Package bill – Reloading packages Considering the Expiry Date.", false)) {

            int k = 0;
            for (Item i : packaes) {
                if (i.getExpiryDate() == null) {
                    listOfTheNonExpiredPackages.add(i);
                } else {
                    if (new Date().after(i.getExpiryDate())) {
                    } else {
                        listOfTheNonExpiredPackages.add(i);
                    }
                }
            }
        } else {
            listOfTheNonExpiredPackages = packaes;
        }

        malePackaes = new ArrayList<>();
        femalePackaes = new ArrayList<>();
        bothPackaes = new ArrayList<>();

        packaes = listOfTheNonExpiredPackages;

        for (Item i : listOfTheNonExpiredPackages) {
            if (i.getForGender() == null) {
                bothPackaes.add(i);
            } else if (i.getForGender().equalsIgnoreCase("Male")) {
                malePackaes.add(i);
            } else if (i.getForGender().equalsIgnoreCase("Female")) {
                femalePackaes.add(i);
            } else if (i.getForGender().equalsIgnoreCase("Both")) {
                bothPackaes.add(i);
                malePackaes.add(i);
                femalePackaes.add(i);
            } else {
                bothPackaes.add(i);
            }

        }
    }

    public List<Item> getListOfTheNonExpiredPackages() {
        if (listOfTheNonExpiredPackages == null) {
            listOfTheNonExpiredPackages = new ArrayList<>();
        }
        return listOfTheNonExpiredPackages;
    }

    public void setListOfTheNonExpiredPackages(List<Item> listOfTheNonExpiredPackages) {
        this.listOfTheNonExpiredPackages = listOfTheNonExpiredPackages;
    }

    public double getTenderedAmount() {
        return tenderedAmount;
    }

    public void setTenderedAmount(double tenderedAmount) {
        this.tenderedAmount = tenderedAmount;
    }

    public boolean isDuplicatePrint() {
        return duplicatePrint;
    }

    public void setDuplicatePrint(boolean duplicatePrint) {
        this.duplicatePrint = duplicatePrint;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public List<Payment> getOriginalBillPayments() {
        return originalBillPayments;
    }

    public void setOriginalBillPayments(List<Payment> originalBillPayments) {
        this.originalBillPayments = originalBillPayments;
    }

    public boolean isOriginalBillCredit() {
        return originalBillCredit;
    }

    public void setOriginalBillCredit(boolean originalBillCredit) {
        this.originalBillCredit = originalBillCredit;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public double getRemainAmount() {
        return remainAmount;
    }

    public void setRemainAmount(double remainAmount) {
        this.remainAmount = remainAmount;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    /**
     * Updates the batch bill's financial tracking fields when an individual package bill
     * is cancelled within a credit payment batch.
     *
     * <p>This method ensures accurate credit balance tracking for partial batch bill
     * cancellations by reducing the batch bill's balance, paidAmount, and increasing
     * refundAmount proportionally to the cancelled individual package bill's net total.</p>
     *
     * <p><b>Healthcare Domain Context:</b> When package bills are paid using Credit payment
     * method, the net total becomes the "due amount" (stored in balance field). Credit
     * companies settle these dues periodically. Accurate balance tracking is critical
     * for credit company settlement reports and financial reconciliation.</p>
     *
     * <p><b>Pattern:</b> Mirrors pharmacy implementation and BillSearch implementation
     * for individual OPD bill cancellations.</p>
     *
     * @param individualPackageBill The individual package bill being cancelled
     * @param cancellationBill The cancellation bill created for the individual package bill
     * @throws IllegalArgumentException if bills are null
     * @throws IllegalStateException if financial data is invalid
     * @see <a href="https://github.com/hmislk/hmis/issues/17138">GitHub Issue #17138</a>
     */
    private void updateBatchBillFinancialFieldsForPackageCancellation(Bill individualPackageBill, Bill cancellationBill) {
        // Validate inputs
        if (individualPackageBill == null) {
            throw new IllegalArgumentException("Individual package bill cannot be null");
        }

        if (cancellationBill == null) {
            throw new IllegalArgumentException("Cancellation bill cannot be null");
        }

        Bill batchBill = individualPackageBill.getBackwardReferenceBill();

        // Not all individual bills have batch bills (e.g., direct package bills)
        if (batchBill == null) {
            return;
        }

        // Only update balance for Credit payment method bills
        if (batchBill.getPaymentMethod() != PaymentMethod.Credit) {
            return;
        }

        // Validate numeric fields
        if (cancellationBill.getNetTotal() == 0.0) {
            throw new IllegalStateException("Cancellation bill net total is invalid");
        }

        // Refresh batch bill from database to ensure latest data and trigger optimistic locking
        batchBill = billFacade.find(batchBill.getId());

        if (batchBill == null) {
            throw new IllegalStateException("Batch bill not found in database");
        }

        // Calculate refund amount (always positive)
        double refundAmount = Math.abs(cancellationBill.getNetTotal());

        // Validate refund amount doesn't exceed original batch bill total
        if (refundAmount > batchBill.getNetTotal()) {
            throw new IllegalStateException(
                String.format("CRITICAL: Refund amount (%.2f) exceeds batch bill total (%.2f). " +
                             "Batch Bill: %s, Individual Package Bill: %s",
                             refundAmount, batchBill.getNetTotal(),
                             batchBill.getInsId(), individualPackageBill.getInsId())
            );
        }

        // Store old values for audit trail
        double oldBalance = batchBill.getBalance();
        double oldPaidAmount = batchBill.getPaidAmount();
        double oldRefundAmount = batchBill.getRefundAmount();

        // Update refundAmount - add the return amount
        batchBill.setRefundAmount(batchBill.getRefundAmount() + refundAmount);

        // Update paidAmount - deduct the return amount (only if payment exists)
        if (batchBill.getPaidAmount() > 0) {
            batchBill.setPaidAmount(Math.max(0d, batchBill.getPaidAmount() - refundAmount));
        }

        // Update balance (due amount) - deduct the return amount (only if balance > 0)
        if (batchBill.getBalance() > 0) {
            batchBill.setBalance(Math.max(0d, batchBill.getBalance() - refundAmount));
        }

        try {
            // Save the updated bill
            billFacade.edit(batchBill);

            System.out.println("=== Package Batch Bill Balance Updated ===");
            System.out.println("Batch Bill ID: " + batchBill.getInsId());
            System.out.println("Individual Package Bill: " + individualPackageBill.getInsId());
            System.out.println("Refund Amount: " + refundAmount);
            System.out.println("Old Balance: " + oldBalance + " → New Balance: " + batchBill.getBalance());
            System.out.println("Old Paid: " + oldPaidAmount + " → New Paid: " + batchBill.getPaidAmount());

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error updating batch bill balance: " + e.getMessage());
            System.err.println("Failed to update batch bill balance: " + e.getMessage());
            e.printStackTrace();
            // Don't re-throw to prevent cancellation from failing completely
            // The individual package bill cancellation should still succeed
        }
    }

}
