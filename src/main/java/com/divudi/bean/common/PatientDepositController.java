/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.report.ReportController;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientDeposit;
import com.divudi.entity.PatientDepositHistory;
import com.divudi.facade.PatientDepositFacade;
import com.divudi.facade.PatientDepositHistoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientDepositController implements Serializable, ControllerWithPatient {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    PatientController patientController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    ReportController reportController;
    @EJB
    private PatientDepositFacade patientDepositFacade;
    @EJB
    private PatientDepositHistoryFacade patientDepositHistoryFacade;
    private PatientDeposit current;
    private List<PatientDeposit> items = null;
    private boolean printPreview;
    private PaymentMethodData paymentMethodData;
    private Bill bill;
    private BillItem billItem;
    private PaymentMethod paymentMethod;
    private Patient patient;
    private Boolean patientDetailsEditable = false;
    private List<PatientDepositHistory> latestPatientDeposits;
    private List<PatientDepositHistory> latestPatientDepositHistory;

    private int patientDepositManagementIndex = 0;

    public String navigateToAddNewPatientDeposit() {
        patientController.clearDataForPatientDeposite();
        return "/patient_deposit/receive?faces-redirect=true";
    }

    public void clearDataForPatientDeposit() {
        patientController.setCurrent(null);
        current = null;
        patient = new Patient();
        latestPatientDepositHistory = new ArrayList<>();
        latestPatientDeposits = new ArrayList<>();
        patientController.clearDataForPatientDeposite();
    }

    public void clearDataForPatientDepositHistory() {
        patientController.setCurrent(null);
        reportController.setFromDate(null);
        reportController.setToDate(null);
    }

    public void getPatientDepositOnPatientDepositAdding() {
        patientController.quickSearchPatientLongPhoneNumber(this);
        current = null;
        if (patient == null) {
            return;
        }
        current = getDepositOfThePatient(patient, sessionController.getDepartment());
        fillLatestPatientDeposits(current);
        fillLatestPatientDepositHistory(current);
        System.out.println("current = " + current);
    }

    public void getPatientDepositOnPatientDepositAddingMulti() {
        patientController.selectQuickOneFromQuickSearchPatient(this);
        current = null;
        if (patient == null) {
            return;
        }
        current = getDepositOfThePatient(patient, sessionController.getDepartment());
        fillLatestPatientDeposits(current);
        fillLatestPatientDepositHistory(current);
        System.out.println("current = " + current);
    }

    public void settlePatientDeposit() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            return;
        }
        int code = patientController.settlePatientDepositReceiveNew();
        
        if(code == 1){
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        }else if(code == 2){
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
             return;
        }
        
        updateBalance(patientController.getBill(), current);
        billBeanController.createPayment(patientController.getBill(),
                patientController.getBill().getPaymentMethod(),
                patientController.getPaymentMethodData());
    }

    public void settlePatientDepositReturn() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            return;
        }
        if (current.getBalance() < patientController.getBill().getNetTotal()) {
            JsfUtil.addErrorMessage("Can't Refund a Total More that Deposit");
            return;
        }
        int code = patientController.settlePatientDepositReturnNew();

        if (code == 1) {
            JsfUtil.addErrorMessage("No Patient");
            return;
        } else if (code == 2) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        } else if (code == 3) {
            JsfUtil.addErrorMessage("The Refunded Value is Missing");
            return;
        } else if (code == 4) {
            JsfUtil.addErrorMessage("The Refunded Value is more than the Current Deposit Value of the Patient");
            return;
        } else if (code == 5) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return;
        } else if (code == 6) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return;
        }

        System.out.println("patientController.getBill() = " + patientController.getBill());
        updateBalance(patientController.getBill(), current);
        billBeanController.createPayment(patientController.getBill(),
                patientController.getBill().getPaymentMethod(),
                patientController.getPaymentMethodData());
    }

    public void updateBalance(Bill b, PatientDeposit pd) {
        switch (b.getBillTypeAtomic()) {
            case PATIENT_DEPOSIT:
                handlePatientDepositBill(b, pd);
                break;

            case PATIENT_DEPOSIT_REFUND:
                handlePatientDepositBillReturn(b, pd);
                break;

            case OPD_BATCH_BILL_WITH_PAYMENT:
                handleOPDBill(b, pd);
                break;

            case OPD_BATCH_BILL_CANCELLATION:
                handleOPDBillCancel(b, pd);
                break;

            case OPD_BILL_CANCELLATION:
                handleOPDBillCancel(b, pd);
                break;

            case OPD_BILL_REFUND:
                handleOPDBillCancel(b, pd);
                break;

            default:
                throw new AssertionError();
        }
    }

    public void handlePatientDepositBill(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance + b.getNetTotal();
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDeposit, pd, b, beforeBalance, afterBalance, Math.abs(b.getNetTotal()));
    }

    public void handlePatientDepositBillReturn(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositReturn, pd, b, beforeBalance, afterBalance, 0 - Math.abs(b.getNetTotal()));
    }

    public void handleOPDBill(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilization, pd, b, beforeBalance, afterBalance, 0 - Math.abs(b.getNetTotal()));
    }

    public void handleOPDBillCancel(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance + Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilizationCancel, pd, b, beforeBalance, afterBalance, Math.abs(b.getNetTotal()));
    }

    public void handleOPDBillRefund(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance + Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilizationReturn, pd, b, beforeBalance, afterBalance, Math.abs(b.getNetTotal()));
    }

    public void createPatientDepositHitory(HistoryType ht, PatientDeposit pd, Bill b, Double beforeBalance, Double afterBalance, Double transactionValue) {
        PatientDepositHistory pdh = new PatientDepositHistory();
        pdh.setPatientDeposit(pd);
        pdh.setBill(b);
        pdh.setHistoryType(ht);
        pdh.setBalanceBeforeTransaction(beforeBalance);
        pdh.setTransactionValue(transactionValue);
        pdh.setBalanceAfterTransaction(afterBalance);
        pdh.setCreater(sessionController.getLoggedUser());
        pdh.setCreatedAt(new Date());
        pdh.setDepartment(sessionController.getDepartment());
        pdh.setInstitution(sessionController.getInstitution());

        patientDepositHistoryFacade.create(pdh);
    }

    public PatientDeposit getDepositOfThePatient(Patient p, Department d) {
        Map m = new HashMap<>();
        String jpql = "select pd from PatientDeposit pd"
                + " where pd.patient.id=:pt "
                + " and pd.department.id=:dep "
                + " and pd.retired=:ret";

        m.put("pt", p.getId());
        m.put("dep", d.getId());
        m.put("ret", false);

        PatientDeposit pd = patientDepositFacade.findFirstByJpql(jpql, m);
        System.out.println("pd = " + pd);

        if (pd == null) {
            pd = new PatientDeposit();
            pd.setBalance(0.0);
            pd.setPatient(p);
            pd.setDepartment(sessionController.getDepartment());
            pd.setInstitution(sessionController.getInstitution());
            pd.setCreater(sessionController.getLoggedUser());
            pd.setCreatedAt(new Date());
            patientDepositFacade.create(pd);
        }
        return pd;
    }

    public void fillLatestPatientDeposits(PatientDeposit pd) {
        Map m = new HashMap<>();

        String jpql = "select pdh from PatientDepositHistory pdh "
                + " where pdh.patientDeposit.id=:pd "
                + " and pdh.historyType=:ht"
                + " and pdh.retired=:ret order by pdh.id";

        m.put("pd", pd.getId());
        m.put("ht", HistoryType.PatientDeposit);
        m.put("ret", false);

        latestPatientDeposits = patientDepositHistoryFacade.findByJpql(jpql, m, 10);
    }

    public void fillLatestPatientDepositHistory(PatientDeposit pd) {
        Map m = new HashMap<>();

        String jpql = "select pdh from PatientDepositHistory pdh "
                + " where pdh.patientDeposit.id=:pd "
                + " and pdh.retired=:ret order by pdh.id";

        m.put("pd", pd.getId());
        m.put("ret", false);

        latestPatientDepositHistory = patientDepositHistoryFacade.findByJpql(jpql, m, 10);
    }

    public PatientDepositFacade getPatientDepositFacade() {
        return patientDepositFacade;
    }

    public void setPatientDepositFacade(PatientDepositFacade patientDepositFacade) {
        this.patientDepositFacade = patientDepositFacade;
    }

    public PatientDeposit getCurrent() {
        return current;
    }

    public void setCurrent(PatientDeposit current) {
        this.current = current;
    }

    public List<PatientDeposit> getItems() {
        return items;
    }

    public void setItems(List<PatientDeposit> items) {
        this.items = items;
    }

    public int getPatientDepositManagementIndex() {
        return patientDepositManagementIndex;
    }

    public void setPatientDepositManagementIndex(int patientDepositManagementIndex) {
        this.patientDepositManagementIndex = patientDepositManagementIndex;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public PaymentMethodData getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public PatientDepositHistoryFacade getPatientDepositHistoryFacade() {
        return patientDepositHistoryFacade;
    }

    public void setPatientDepositHistoryFacade(PatientDepositHistoryFacade patientDepositHistoryFacade) {
        this.patientDepositHistoryFacade = patientDepositHistoryFacade;
    }

    public Boolean getPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    public void setPatientDetailsEditable(Boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public List<PatientDepositHistory> getLatestPatientDeposits() {
        return latestPatientDeposits;
    }

    public void setLatestPatientDeposits(List<PatientDepositHistory> latestPatientDeposits) {
        this.latestPatientDeposits = latestPatientDeposits;
    }

    public List<PatientDepositHistory> getLatestPatientDepositHistory() {
        return latestPatientDepositHistory;
    }

    public void setLatestPatientDepositHistory(List<PatientDepositHistory> latestPatientDepositHistory) {
        this.latestPatientDepositHistory = latestPatientDepositHistory;
    }

    /**
     *
     */
    @FacesConverter(forClass = PatientDeposit.class)
    public static class PatientDepositConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientDepositController controller = (PatientDepositController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientDepositController");
            return controller.getPatientDepositFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof PatientDeposit) {
                PatientDeposit o = (PatientDeposit) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientDeposit.class.getName());
            }
        }
    }

}
