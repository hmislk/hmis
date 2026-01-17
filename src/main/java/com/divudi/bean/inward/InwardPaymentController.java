/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.PatientDepositController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
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
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.PatientDepositService;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InwardPaymentController implements Serializable, ControllerWithMultiplePayments {

    private static final long serialVersionUID = 1L;

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    PaymentService paymentService;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    PatientDepositService patientDepositService;
    @EJB
    PatientFacade patientFacade;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private InwardBeanController inwardBean;
    @Inject
    private BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private BilledBill current;
    private boolean printPreview;
    private double due;
    String comment;

    private PaymentMethod paymentMethod;
    private double remainAmount;
    private double total;
    private Patient patient;
    private PaymentMethodData paymentMethodData;
    
    // </editor-fold>

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();
    }

    public void bhtListener() {
        if (current.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return;
        }
        due = getFinalBillDue();
        patient = current.getPatientEncounter().getPatient();
        paymentMethod = null;
        paymentMethodData = new PaymentMethodData();

    }

    public String navigateToInpationDashbord() {
        return "/inward/admission_profile?faces-redirect=true";
    }

    public String navigateToPatientRefund() {
        paymentMethodData = new PaymentMethodData();
        return "inward_cancel_bill_refund?faces-redirect=true";
    }

    private double getFinalBillDue() {
        String sql = "Select b From BilledBill b where"
                + " b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter=:pe "
                + " order by b.id desc";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);
        hm.put("pe", getCurrent().getPatientEncounter());

        Bill b = getBilledBillFacade().findFirstByJpql(sql, hm);

        if (b == null) {
            return 0;
        }

        return b.getNetTotal() - (b.getPaidAmount() + getCurrent().getPatientEncounter().getCreditPaidAmount());

//        double billValue = Math.abs(b.getNetTotal());
//        double paidByPatient = Math.abs(b.getPaidAmount());
//        double creditUsedAmount = Math.abs(getCurrent().getPatientEncounter().getCreditUsedAmount());
//        double creditPaidAmount = Math.abs(getCurrent().getPatientEncounter().getCreditPaidAmount());
//        double netCredit = creditUsedAmount - creditPaidAmount;
//
//        return billValue - (paidByPatient + netCredit);
    }

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please Select a Payment Method");
            return true;
        }

        if (checkErrorsInPaymentMethod(paymentMethod, paymentMethodData)) {
            return true;
        }
        
        return false;

    }
    
    private boolean checkErrorsInPaymentMethod(PaymentMethod method, PaymentMethodData methodData) {
        if (method == null) {
            JsfUtil.addErrorMessage("Please Select a Payment Method");
            return true;
        }
        double amountToCheck = 0.0;

        if (method == PaymentMethod.Cash) {
            amountToCheck = getCurrent().getTotal();
            if (amountToCheck <= 0.0) {
                JsfUtil.addErrorMessage("Please enter a payment amount");
                return true;
            }
        } else {
            if (methodData == null) {
                JsfUtil.addErrorMessage("Error in Payment Data.");
                return true;
            }

            switch (method) {
                case Card:
                    if (methodData.getCreditCard() == null) {
                        JsfUtil.addErrorMessage("Error in card payment Details");
                        return true;
                    } else {
                        if (methodData.getCreditCard().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the card payment amount");
                            return true;
                        }
                        if (methodData.getCreditCard().getInstitution() == null || methodData.getCreditCard().getNo() == null) {
                            JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getCreditCard().getTotalValue();
                    }
                    break;

                case Cheque:
                    if (methodData.getCheque() == null) {
                        JsfUtil.addErrorMessage("Error in Cheque payment Details");
                        return true;
                    } else {
                        if (methodData.getCheque().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the cheque payment amount");
                            return true;
                        }
                        if (methodData.getCheque().getInstitution() == null || methodData.getCheque().getNo() == null || methodData.getCheque().getDate() == null) {
                            JsfUtil.addErrorMessage("Please select Cheque Number, Bank and Cheque Date.");
                            return true;
                        }
                        amountToCheck = methodData.getCheque().getTotalValue();
                    }
                    break;

                case Slip:
                    if (methodData.getSlip() == null) {
                        JsfUtil.addErrorMessage("Error in Slip payment Details");
                        return true;
                    } else {
                        if (methodData.getSlip().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the slip payment details");
                            return true;
                        }
                        if (methodData.getSlip().getInstitution() == null || methodData.getSlip().getDate() == null) {
                            JsfUtil.addErrorMessage("Please Fill Bank and Slip Date.");
                            return true;
                        }
                        amountToCheck = methodData.getSlip().getTotalValue();
                    }
                    break;

                case ewallet:
                    if (methodData.getEwallet() == null) {
                        JsfUtil.addErrorMessage("Error in eWallet payment Details");
                        return true;
                    } else {
                        if (methodData.getEwallet().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the eWallet payment Amount");
                            return true;
                        }
                        if (methodData.getEwallet().getInstitution() == null || ((methodData.getEwallet().getReferenceNo() == null || methodData.getEwallet().getReferenceNo().trim().isEmpty()) && (methodData.getEwallet().getNo() == null || methodData.getEwallet().getNo().trim().isEmpty()))) {
                            JsfUtil.addErrorMessage("Please Fill eWallet Reference Number and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getEwallet().getTotalValue();
                    }
                    break;

                case PatientDeposit:
                    if (methodData.getPatient_deposit() == null) {
                        JsfUtil.addErrorMessage("Error in Patient Deposit Details");
                        return true;
                    } else {
                        double creditLimitAbsolute = Math.abs(patient.getCreditLimit());
                        PatientDeposit pd = methodData.getPatient_deposit().getPatientDepost();

                        double availableForPurchase = pd.getBalance() + creditLimitAbsolute;
                        double payhingThisTimeValue;

                        if (methodData.getPatient_deposit().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the Patient Deposit payment Amount");
                            return true;
                        } else {
                            payhingThisTimeValue = methodData.getPatient_deposit().getTotalValue();
                            if (payhingThisTimeValue > availableForPurchase) {
                                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                                return true;
                            }
                            amountToCheck = methodData.getPatient_deposit().getTotalValue();
                        }
                    }
                    break;

                case OnlineSettlement:
                    if (methodData.getOnlineSettlement() == null) {
                        JsfUtil.addErrorMessage("Error in Online Settlement Details");
                        return true;
                    } else {
                        if (methodData.getOnlineSettlement().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the Online Settlement payment Amount");
                            return true;
                        }
                        if (methodData.getOnlineSettlement().getInstitution() == null || (methodData.getOnlineSettlement().getReferenceNo() == null || methodData.getOnlineSettlement().getReferenceNo().trim().isEmpty()) && methodData.getOnlineSettlement().getDate() == null) {
                            JsfUtil.addErrorMessage("Please Fill Online Settlement Reference Number, Date and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getOnlineSettlement().getTotalValue();
                    }
                    break;

                case MultiplePaymentMethods:
                    if (methodData.getPaymentMethodMultiple() == null || methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null || methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                        JsfUtil.addErrorMessage("Please configure payment amounts");
                        return true;
                    }

                    List<ComponentDetail> paymentDetailsList = methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();

                    if (paymentDetailsList.isEmpty()) {
                        JsfUtil.addErrorMessage("Please select the first payment method.");
                        return true;
                    }

                    if (paymentDetailsList.size() == 1) {
                        JsfUtil.addErrorMessage("You can't use only one payment method.");
                        return true;
                    }

                    // Calculate total from all components
                    double multipleTotal = 0.0;
                    int componentCount = 0;
                    for (ComponentDetail cd : paymentDetailsList) {
                        componentCount++;
                        if (cd != null && cd.getPaymentMethodData() != null) {
                            // Check based on payment method type in the component
                            Double checkAmount = 0.0;
                            switch (cd.getPaymentMethod()) {
                                case Cash:
                                    if (cd.getPaymentMethodData().getCash() == null) {
                                        JsfUtil.addErrorMessage("Please enter the amount to be paid from Cash.");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCash().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter a cash amount");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Card:
                                    if (cd.getPaymentMethodData().getCreditCard() == null) {
                                        JsfUtil.addErrorMessage("Error in card payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCreditCard().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Card.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCreditCard().getTotalValue() <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter card payment amount");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCreditCard().getInstitution() == null || cd.getPaymentMethodData().getCreditCard().getNo() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Cheque:
                                    if (cd.getPaymentMethodData().getCheque() == null) {
                                        JsfUtil.addErrorMessage("Error in Cheque payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCheque().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Cheque.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCheque().getInstitution() == null || cd.getPaymentMethodData().getCheque().getNo() == null || cd.getPaymentMethodData().getCheque().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please select Cheque Number, Bank and Cheque Date.");
                                            return true;
                                        }

                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Slip:
                                    if (cd.getPaymentMethodData().getSlip() == null) {
                                        JsfUtil.addErrorMessage("Error in Slip payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getSlip().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Back Slip.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getSlip().getInstitution() == null || cd.getPaymentMethodData().getSlip().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Bank and Slip Date.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case OnlineSettlement:
                                    if (cd.getPaymentMethodData().getOnlineSettlement() == null) {
                                        JsfUtil.addErrorMessage("Error in Online Settlement Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Online Settlement.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getOnlineSettlement().getInstitution() == null || (cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo() == null || cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo().trim().isEmpty()) && cd.getPaymentMethodData().getOnlineSettlement().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Online Settlement Reference Number, Date and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case Staff_Welfare:
                                    if (cd.getPaymentMethodData().getStaffWelfare() == null) {
                                        JsfUtil.addErrorMessage("Error in Staff Welfare Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getStaffWelfare().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Staff Welfare.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Welfare Staff Name");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case ewallet:
                                    if (cd.getPaymentMethodData().getEwallet() == null) {
                                        JsfUtil.addErrorMessage("Error in eWallet payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getEwallet().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from eWallet.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getEwallet().getInstitution() == null || ((cd.getPaymentMethodData().getEwallet().getReferenceNo() == null || cd.getPaymentMethodData().getEwallet().getReferenceNo().trim().isEmpty()) && (cd.getPaymentMethodData().getEwallet().getNo() == null || cd.getPaymentMethodData().getEwallet().getNo().trim().isEmpty()))) {
                                            JsfUtil.addErrorMessage("Please Fill eWallet Reference Number and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case PatientDeposit:
                                    if (cd.getPaymentMethodData().getPatient_deposit() == null) {
                                        JsfUtil.addErrorMessage("Error in Patient Deposit Details");
                                        return true;
                                    } else {
                                        double currentPatientCreditLimitAbsolute = Math.abs(patient.getCreditLimit());
                                        PatientDeposit currentPatientDeposit = cd.getPaymentMethodData().getPatient_deposit().getPatientDepost();

                                        double maximumAmount = currentPatientDeposit.getBalance() + currentPatientCreditLimitAbsolute;
                                        double payhingThisTimeAmount = cd.getPaymentMethodData().getPatient_deposit().getTotalValue();

                                        if (payhingThisTimeAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Patient Deposit.");
                                            return true;
                                        }
                                        if (payhingThisTimeAmount > maximumAmount) {
                                            JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                                            return true;
                                        }
                                        multipleTotal += payhingThisTimeAmount;
                                    }
                                    break;
                                default:
                                    System.out.println("[DEBUG] Processing default/unknown payment method: " + method);
                            }
                        }
                    }

                    if (multipleTotal <= 0.0) {
                        JsfUtil.addErrorMessage("Please enter valid payment amounts");
                        return true;
                    }
                    amountToCheck = multipleTotal;
                    break;
                default:
                    System.out.println("[DEBUG] Processing default/unknown payment method: " + method);
            }
        }

        getCurrent().setTotal(amountToCheck);
        getCurrent().setNetTotal(amountToCheck);

        return false;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public String fillDataForInpatientsDepositBill(String template, Bill bill) {
        if (isInvalidInwardDepositBill(template, bill)) {
            return "";
        }

        PatientEncounter pe = bill.getPatientEncounter();
        Patient patient = pe.getPatient();
        Person person = patient.getPerson();

        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

        String finalBillInsId = "";
        String finalBillDeptId = "";

        if (pe.getFinalBill() != null) {
            finalBillInsId = pe.getFinalBill().getInsId();
            finalBillDeptId = pe.getFinalBill().getDeptId();
        }

        String output;
        output = template
                .replace("{dept_id}", bill.getDeptId() != null ? String.valueOf(bill.getDeptId()) : "")
                .replace("{ins_id}", bill.getInsId() != null ? String.valueOf(bill.getInsId()) : "")
                .replace("{gross_total}", CommonFunctions.convertDoubleToString(bill.getTotal()))
                .replace("{discount}", CommonFunctions.convertDoubleToString(bill.getDiscount()))
                .replace("{net_total}", decimalFormat.format(bill.getNetTotal()))
                .replace("{net_total_in_words}", CommonFunctions.convertToWord(bill.getTotal()))
                .replace("{cancelled}", bill.isRefunded() ? "Yes" : "No")
                .replace("{returned}", bill.isCancelled() ? "Yes" : "No")
                .replace("{cashier_username}", bill.getCreater() != null && bill.getCreater().getName() != null ? bill.getCreater().getName() : "")
                .replace("{patient_nic}", person.getNic() != null ? person.getNic() : "")
                .replace("{patient_phn_number}", patient.getPhn() != null ? patient.getPhn() : "")
                .replace("{admission_number}", pe.getBhtNo() != null ? pe.getBhtNo() : "")
                .replace("{fian_bill_number}", finalBillDeptId != null ? finalBillDeptId : "")
                .replace("{fian_bill_number_dept_id}", finalBillDeptId != null ? finalBillDeptId : "")
                .replace("{fian_bill_number_ins_id}", finalBillInsId != null ? finalBillInsId : "")
                .replace("{admission_date}", pe.getDateOfAdmission() != null ? formatDate(pe.getDateOfAdmission(), sessionController) : "")
                .replace("{date_of_admission}", pe.getDateOfAdmission() != null ? formatDate(pe.getDateOfAdmission(), sessionController) : "")
                .replace("{bht}", pe.getBhtNo() != null ? pe.getBhtNo() : "")
                .replace("{date_of_discharge}", pe.getDateOfDischarge() != null ? formatDate(pe.getDateOfDischarge(), sessionController) : "")
                .replace("{admission_type}", getAdmissionType(pe) != null ? getAdmissionType(pe) : "")
                .replace("{patient_name}", person.getNameWithTitle() != null ? person.getNameWithTitle() : "")
                .replace("{patient_age}", patient.getAgeOnBilledDate(pe.getDateOfAdmission()) != null ? patient.getAgeOnBilledDate(pe.getDateOfAdmission()) : "")
                .replace("{patient_sex}", person.getSex() != null ? person.getSex().name() : "")
                .replace("{patient_address}", person.getAddress() != null ? person.getAddress() : "")
                .replace("{patient_phone}", person.getPhone() != null ? person.getPhone() : "")
                .replace("{from_institution}", getInstitutionName(pe) != null ? getInstitutionName(pe) : "")
                .replace("{to_institution}", getInstitutionName(pe) != null ? getInstitutionName(pe) : "")
                .replace("{from_department}", getDepartmentName(pe) != null ? getDepartmentName(pe) : "")
                .replace("{to_department}", getDepartmentName(pe) != null ? getDepartmentName(pe) : "")
                .replace("{payment_method}", pe.getPaymentMethod() != null && pe.getPaymentMethod().getLabel() != null ? pe.getPaymentMethod().getLabel() : "")
                .replace("{bill_date}", bill.getBillDate() != null ? formatDate(bill.getBillDate(), sessionController) : "")
                .replace("{bill_time}", bill.getBillTime() != null ? formatTime(bill.getBillTime(), sessionController) : "")
                .replace("{time_of_admission}", pe.getDateOfAdmission() != null ? formatDate(pe.getDateOfAdmission(), sessionController) : "")
                .replace("{time_of_discharge}", pe.getDateOfDischarge() != null ? formatTime(pe.getDateOfDischarge(), sessionController) : "");

        return output;
    }

    private String formatDate(Date date, SessionController sessionController) {
        return date != null ? CommonFunctions.dateToString(date, sessionController.getApplicationPreference().getLongDateFormat()) : "";
    }

    private String formatTime(Date time, SessionController sessionController) {
        return time != null ? CommonFunctions.dateToString(time, sessionController.getApplicationPreference().getLongTimeFormat()) : "";
    }

    private String getAdmissionType(PatientEncounter pe) {
        return pe.getAdmissionType() != null ? pe.getAdmissionType().getName() : "";
    }

    private String getInstitutionName(PatientEncounter pe) {
        return pe.getInstitution() != null ? pe.getInstitution().getName() : "";
    }

    private String getDepartmentName(PatientEncounter pe) {
        return pe.getDepartment() != null ? pe.getDepartment().getName() : "";
    }

    private boolean isInvalidInwardDepositBill(String template, Bill bill) {
        return template == null || template.trim().isEmpty()
                || bill == null || bill.getPatientEncounter() == null
                || bill.getPatientEncounter().getPatient() == null
                || bill.getPatientEncounter().getPatient().getPerson() == null;
    }
    
    public void pay() {
        if (errorCheck()) {
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Inward Deposit Payment Bill Payment Type Required", false)
                && configOptionApplicationController.getBooleanValueByKey("Get Payment Type Instead of Comment", false)) {

            if (comment == null || comment.trim().isEmpty()) { // Trim to handle whitespace-only cases
                JsfUtil.addErrorMessage("Please Select a Payment Type");
                return;
            }
        }

        saveBill();
        saveBillItem();

        paymentService.createPayment(
                current,
                current.getPaymentMethod(), 
                paymentMethodData, 
                sessionController.getInstitution(), 
                sessionController.getDepartment(),
                sessionController.getLoggedUser());

        getBillBean().updateInwardDipositList(getCurrent().getPatientEncounter(), getCurrent());

        if (getCurrent().getPatientEncounter().isPaymentFinalized()) {
            getInwardBean().updateFinalFill(getCurrent().getPatientEncounter());

            if (getCurrent().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                getInwardBean().updateCreditDetail(getCurrent().getPatientEncounter(), getCurrent().getPatientEncounter().getFinalBill().getNetTotal());
            }
        }
        JsfUtil.addSuccessMessage("Payment Bill Saved");
        paymentMethod = null;
        printPreview = true;
    }

    public Bill pay(PaymentMethod paymentMethod, PatientEncounter patientEncounter, double value) {
        makeNull();
        getCurrent().setPaymentMethod(paymentMethod);
        getCurrent().setPatientEncounter(patientEncounter);
        getCurrent().setTotal(value);

        if (errorCheck()) {
            return null;
        }

        saveBill();
        saveBillItem();
        JsfUtil.addSuccessMessage("Payment Bill Saved");

        Bill curr = getCurrent();

        makeNull();

        return curr;
    }
    
    public void updatePaymentData() {
        if (current == null) {
            JsfUtil.addErrorMessage("No current bill available");
            return;
        }
       
        PaymentMethod pm = getPaymentMethod();
        
        if (pm != null) {
            if (pm == PaymentMethod.PatientDeposit) {

                if (patient == null) {
                    JsfUtil.addErrorMessage("No Patient is selected. Can't proceed with Patient Deposits");
                    return;
                }
                if (patient.getId() == null) {
                    JsfUtil.addErrorMessage("No Patient is selected. Can't proceed with Patient Deposits");
                    return ;
                    
                } else {
                    patient = patientFacade.find(patient.getId());

                    if (patient == null) {
                        JsfUtil.addErrorMessage("Patient not found in system");
                        return;
                    }

                    if (paymentMethodData == null) {
                        paymentMethodData = new PaymentMethodData();
                    }

                    paymentMethodData.getPatient_deposit().setPatient(patient);
                    if (!patient.getHasAnAccount()) {
                        JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                        return;
                    }
                    PatientDeposit pd = patientDepositService.getDepositOfThePatient(patient, sessionController.getDepartment());
                    paymentMethodData.getPatient_deposit().setPatientDepost(pd);
                }
            }
        }
    }

    
    public void changePaymentMethodChange() {
        if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();

            getPaymentMethodData().getPatient_deposit().setPatient(getCurrent().getPatientEncounter().getPatient());

            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(getCurrent().getPatientEncounter().getPatient(), sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(getCurrent().getPatientEncounter().getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }
        } else {
            listnerForPaymentMethodChange();
        }
    }

    public void listnerForPaymentMethodChange() {

        if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(getCurrent().getPatientEncounter().getPatient());
            getPaymentMethodData().getPatient_deposit().setTotalValue(getCurrent().getTotal());
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(getCurrent().getPatientEncounter().getPatient(), sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (getPaymentMethod() == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(getCurrent().getTotal());
        } else if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            getPaymentMethodData().getPatient_deposit().setPatient(getCurrent().getPatientEncounter().getPatient());

            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(getCurrent().getPatientEncounter().getPatient(), sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(getCurrent().getPatientEncounter().getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void makeNull() {
        current = null;
        printPreview = false;
        comment = null;
        paymentMethod = null;
        total = 0.0;
    }

    private void saveBill() {
        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setBillType(BillType.InwardPaymentBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_DEPOSIT);
        getCurrent().setPaymentMethod(paymentMethod);
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPAY));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPAY));
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setComments(comment);

        if (getCurrent().getId() == null) {
            getBilledBillFacade().create(getCurrent());
        }

    }

    private void saveBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getCurrent());
        temBi.setGrossValue(getCurrent().getTotal());
        temBi.setNetValue(getCurrent().getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(getSessionController().getLoggedUser());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }

    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        double multiplePaymentMethodTotalValue = 0.0;
        if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            if (paymentMethodData != null && paymentMethodData.getPaymentMethodMultiple() != null && paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd == null || cd.getPaymentMethodData() == null) {
                        continue;
                    }
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
                }
            }
            getCurrent().setTotal(multiplePaymentMethodTotalValue);
            getCurrent().setNetTotal(multiplePaymentMethodTotalValue);

        }
        return multiplePaymentMethodTotalValue;
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        calculatRemainForMultiplePaymentTotal();
        if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            if (paymentMethodData == null
                    || paymentMethodData.getPaymentMethodMultiple() == null
                    || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                    || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                return;
            }

            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);

            switch (pm.getPaymentMethod()) {
                case Cash:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCash().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCash().setTotalValue(0.0);
                    }
                    break;
                case Card:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCreditCard().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCreditCard().setTotalValue(0.0);
                    }
                    break;
                case Cheque:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCheque().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCheque().setTotalValue(0.0);
                    }
                    break;
                case Slip:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getSlip().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getSlip().setTotalValue(0.0);
                    }
                    break;
                case ewallet:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getEwallet().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getEwallet().setTotalValue(0.0);
                    }
                    break;
                case PatientDeposit:
                    Patient p = patientFacade.find(patient.getId());

                    if (p == null) {
                        break;
                    } else {
                        pm.getPaymentMethodData().getPatient_deposit().setPatient(p);
                        PatientDeposit pd = patientDepositService.getDepositOfThePatient(p, sessionController.getDepartment());

                        if (pd != null) {
                            pm.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                            pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                        }
                    }
                    break;
                case Credit:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCredit().setTotalValue(0.0);
                    }
                    break;
                case Staff:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffCredit().setTotalValue(0.0);
                    }
                    break;
                case Staff_Welfare:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffWelfare().setTotalValue(0.0);
                    }
                    break;
                case OnlineSettlement:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getOnlineSettlement().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(0.0);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + pm.getPaymentMethod());
            }
        }
        listnerForPaymentMethodChange();
    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null
                || paymentMethodData == null
                || paymentMethodData.getPaymentMethodMultiple() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
    }

    public BilledBillFacade getBilledBillFacade() {
        return billedBillFacade;
    }

    public void setBilledBillFacade(BilledBillFacade billedBillFacade) {
        this.billedBillFacade = billedBillFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BilledBill getCurrent() {
        if (current == null) {
            current = new BilledBill();
            current.setBillType(BillType.InwardPaymentBill);
        }

        return current;
    }

    public void setCurrent(BilledBill current) {
        this.current = current;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
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

    public double getDue() {
        return due;
    }

    public void setDue(double due) {
        this.due = due;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getRemainAmount() {
        return remainAmount;
    }

    public void setRemainAmount(double remainAmount) {
        this.remainAmount = remainAmount;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

}
