/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
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
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BilledBillFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
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
public class InwardPaymentController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private BillNumberGenerator billNumberBean;
    private BilledBill current;
    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private SessionController sessionController;
    private boolean printPreview;
    private double due;
    String comment;

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();
    }

    @Inject
    private PaymentSchemeController paymentSchemeController;
    private PaymentMethodData paymentMethodData;

    public void bhtListener() {
        if (current.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return;
        }
        due = getFinalBillDue();

    }

    public String navigateToInpationDashbord() {
        return "/inward/admission_profile?faces-redirect=true";
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

    @Inject
    private InwardBeanController inwardBean;

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return true;
        }

        if (getCurrent().getTotal() == 0.0) {
            JsfUtil.addErrorMessage("Please enter paying amount");
            return true;
        }
        if (getPaymentSchemeController().errorCheckPaymentMethod(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

//        if (due != 0) {
//
//            if ((due < getCurrent().getTotal())) {
//                double different = Math.abs((due - getCurrent().getTotal()));
//
//                if (different > 0.1) {
//                    JsfUtil.addErrorMessage("U cant recieve payment thenn due");
//                    return true;
//                }
//            }
//        }
        return false;

    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public String fillDataForInpatientsDepositBill(String template, Bill bill) {
        //System.out.println("fillDataForInpatientsDepositBill");
        if (isInvalidInwardDepositBill(template, bill)) {
            //System.out.println("Not Valid = " + bill);
            return "";
        }

        PatientEncounter pe = bill.getPatientEncounter();
        Patient patient = pe.getPatient();
        Person person = patient.getPerson();

        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        
        String finalBillInsId ="";
        String finalBillDeptId = "";
        
        if(pe.getFinalBill()!=null){
            finalBillInsId=pe.getFinalBill().getInsId();
            finalBillDeptId=pe.getFinalBill().getDeptId();
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

        saveBill();
        saveBillItem();

        getBillBean().updateInwardDipositList(getCurrent().getPatientEncounter(), getCurrent());

        if (getCurrent().getPatientEncounter().isPaymentFinalized()) {
            getInwardBean().updateFinalFill(getCurrent().getPatientEncounter());

            if (getCurrent().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                getInwardBean().updateCreditDetail(getCurrent().getPatientEncounter(), getCurrent().getPatientEncounter().getFinalBill().getNetTotal());
            }
        }

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Payment Bill Saved");
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
    }

    @Inject
    private BillBeanController billBean;

    private void saveBill() {
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setBillType(BillType.InwardPaymentBill);
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPAY));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPAY));

//        getCurrent().setForwardReferenceBill(getCurrent().getPatientEncounter().getFinalBill());
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        double dbl = Math.abs(getCurrent().getTotal());
        getCurrent().setTotal(dbl);
        getCurrent().setNetTotal(dbl);
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

}
