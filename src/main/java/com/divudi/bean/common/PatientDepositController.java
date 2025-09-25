/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.bean.report.ReportController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.PatientDepositHistory;
import com.divudi.core.entity.Payment;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientDepositFacade;
import com.divudi.core.facade.PatientDepositHistoryFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.service.PatientDepositService;
import com.divudi.service.PaymentService;
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
    @Inject
    DrawerController drawerController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    OpdBillController opdBillController;

    @EJB
    PatientFacade patientFacade;
    @EJB
    private PatientDepositFacade patientDepositFacade;
    @EJB
    private PatientDepositHistoryFacade patientDepositHistoryFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    PatientDepositService patientDepositService;
    @EJB
    PaymentService paymentService;

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
        clearDataForPatientDeposit();
        return "/patient_deposit/receive?faces-redirect=true";
    }

    public String navigateToPatientDepositRefundFromOPDBill(Patient p) {
        clearDataForPatientDeposit();
        patient = p;
        current = getDepositOfThePatient(patient, sessionController.getDepartment());
        patientController.listnerForPaymentMethodChange();
        return "/patient_deposit/pay?faces-redirect=true";
    }

    public String navigateToReciveDepositWithPatient(Patient p) {
        clearDataForPatientDeposit();
        patient = p;
        current = getDepositOfThePatient(patient, sessionController.getDepartment());
        fillLatestPatientDeposits(current);
        fillLatestPatientDepositHistory(current);
        return "/patient_deposit/receive?faces-redirect=true";
    }


    public void clearDataForPatientDeposit() {
        patientController.setCurrent(null);
        current = null;
        patient = new Patient();
        bill=null;
        paymentMethodData=null;
        latestPatientDepositHistory = new ArrayList<>();
        latestPatientDeposits = new ArrayList<>();
        patientController.clearDataForPatientDeposite();
        paymentMethodData = new PaymentMethodData();
        billItem = new BillItem();
        printPreview = false;
    }

    public String navigateToNewPatientDepositCancel() {
        if (patientController.getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Canceled Bill");
            return "";
        }
        patientController.preparePatientDepositCancel();
        return "/patient_deposit/view/bill_cancel?faces-redirect=true";
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
        if (patient.getId() == null) {
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

    private boolean validatePaymentMethodDataForPatientDeposit() {
        boolean error = false;
        if (getBill().getNetTotal() < 0.01) {
            JsfUtil.addErrorMessage("Please Enter a value to deposit");
            error = true;
        }
        if (null == getBill().getPaymentMethod()) {
            JsfUtil.addErrorMessage("Please select a payment method");
            error = true;
        } else {
            switch (getBill().getPaymentMethod()) {
                case Card:
                    if (getPaymentMethodData().getCreditCard().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - CreditCard Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a Credit Card Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getCreditCard().setTotalValue(getBill().getNetTotal());
                    break;
                case Cheque:
                    if (getPaymentMethodData().getCheque().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - Cheque Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a Cheque Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getCheque().setTotalValue(getBill().getNetTotal());
                    break;
                case ewallet:
                    if (getPaymentMethodData().getEwallet().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - E-Wallet Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a E-Wallet Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getEwallet().setTotalValue(getBill().getNetTotal());
                    break;
                case Slip:
                    if (getPaymentMethodData().getSlip().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - Slip Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a Slip Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getSlip().setTotalValue(getBill().getNetTotal());
                    break;
                case Cash:
                    getPaymentMethodData().getCash().setTotalValue(getBill().getNetTotal());
                    break;
                default:
                    JsfUtil.addErrorMessage("This payment method is NOT valid for Patient Deposits");
                    error = true;
                    break;
            }
        }
        return error;
    }

    private boolean validatePaymentMethodDataForPatientDepositReturn() {
        boolean error = false;
        if (patientController.getBill().getNetTotal() < 0.01) {
            JsfUtil.addErrorMessage("Please Enter a value to deposit");
            error = true;
        }
        if (null == patientController.getBill().getPaymentMethod()) {
            JsfUtil.addErrorMessage("Please select a payment method");
            error = true;
        } else {
            switch (patientController.getBill().getPaymentMethod()) {
                case Card:
                    if (getPaymentMethodData().getCreditCard().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - CreditCard Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a Credit Card Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getCreditCard().setTotalValue(patientController.getBill().getNetTotal());
                    break;
                case Cheque:
                    if (getPaymentMethodData().getCheque().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - Cheque Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a Cheque Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getCheque().setTotalValue(patientController.getBill().getNetTotal());
                    break;
                case ewallet:
                    if (getPaymentMethodData().getEwallet().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - E-Wallet Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a E-Wallet Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getEwallet().setTotalValue(patientController.getBill().getNetTotal());
                    break;
                case Slip:
                    if (getPaymentMethodData().getSlip().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("Patient Deposit - Slip Comment is Mandatory", false)) {
                        JsfUtil.addErrorMessage("Please Enter a Slip Comment..");
                        error = true;
                    }
                    getPaymentMethodData().getSlip().setTotalValue(patientController.getBill().getNetTotal());
                    break;
                case Cash:
                    getPaymentMethodData().getCash().setTotalValue(patientController.getBill().getNetTotal());
                    break;
                default:
                    JsfUtil.addErrorMessage("This payment method is NOT valid for Patient Deposits");
                    error = true;
                    break;
            }
        }
        return error;
    }

    public void settlePatientDeposit() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("No Patient Deposit");
            return;
        }
        if (validatePaymentMethodDataForPatientDeposit()) {
            return;
        }
        patient.setHasAnAccount(true);
        patientController.save(patient);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PATIENT_DEPOSIT);

        getBill().setInsId(deptId);
        getBill().setDeptId(deptId);

        getBill().setBillType(BillType.PatientPaymentReceiveBill);
        getBill().setBillClassType(BillClassType.BilledBill);
        getBill().setBillTypeAtomic(BillTypeAtomic.PATIENT_DEPOSIT);
        getBill().setPatient(patient);

        getBill().setCreatedAt(new Date());
        getBill().setCreater(sessionController.getLoggedUser());
        getBill().setBillDate(new Date());
        getBill().setBillTime(new Date());

        getBill().setDepartment(sessionController.getLoggedUser().getDepartment());
        getBill().setInstitution(sessionController.getLoggedUser().getInstitution());
        getBill().setGrantTotal(getBill().getNetTotal());
        getBill().setTotal(getBill().getNetTotal());
        getBill().setDiscount(0.0);
        getBill().setDiscountPercent(0);

        if (getBill().getId() == null) {
            billFacade.create(getBill());
        } else {
            billFacade.edit(getBill());
        }

        BillItem addingSingleBillItem = new BillItem();
        addingSingleBillItem.setNetValue(getBill().getNetTotal());
        addingSingleBillItem.setBill(getBill());
        addingSingleBillItem.setGrossValue(getBill().getNetTotal());
        addingSingleBillItem.setDiscount(0.0);
        addingSingleBillItem.setItem(null);
        addingSingleBillItem.setQty(1.0);
        addingSingleBillItem.setRate(getBill().getNetTotal());
        billItemFacade.create(addingSingleBillItem);
        paymentService.createPayment(bill, getPaymentMethodData());
        patientDepositService.updateBalance(bill, current);
        printPreview = true;
    }

    public void settlePatientDepositCancel() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            return;
        }
        if (patient.getId() == null) {
            JsfUtil.addErrorMessage("Entered Patient is Not Registered");
            return;
        }
        current = getDepositOfThePatient(patientController.getBill().getPatient(), sessionController.getDepartment());
        if (patientController.getBill().getNetTotal() > current.getBalance()) {
            JsfUtil.addErrorMessage("Insufficient Balance");
            return;
        }

        int code = patientController.settlePatientDepositReceiveCancelNew();

        if (code == 1) {
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        } else if (code == 2) {
            JsfUtil.addErrorMessage("Please enter all relavent Payment Method Details");
            return;
        }

        updateBalance(patientController.getCancelBill(), current);
        List<Payment> p = billBeanController.createPayment(patientController.getCancelBill(),
                patientController.getCancelBill().getPaymentMethod(),
                patientController.getPaymentMethodData());
        drawerController.updateDrawerForOuts(p);
    }

    public void settlePatientDepositReturn() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            return;
        }
        if (validatePaymentMethodDataForPatientDepositReturn()) {
            return;
        }

        if (current == null) {
            JsfUtil.addErrorMessage("No current. please start from beginning");
            return;
        }
        if (current.getBalance() == null) {
            current.setBalance(0.0);
        }
        if (patientController.getBill() == null) {
            JsfUtil.addErrorMessage("No Bill in patient controller. please start from beginning");
            return;
        }
        patientController.setBillNetTotal();
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

        updateBalance(patientController.getBill(), current);
        List<Payment> ps = billBeanController.createPayment(patientController.getBill(),
                patientController.getBill().getPaymentMethod(),
                patientController.getPaymentMethodData());
        drawerController.updateDrawerForOuts(ps);
    }

    @Deprecated // Use the methods in  PatientDepositService
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

            case PHARMACY_RETAIL_SALE:
                handleOPDBill(b, pd);
                break;

            case PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER:
                handleOPDBill(b, pd);
                break;

            case PHARMACY_RETAIL_SALE_CANCELLED:
                handleOPDBillCancel(b, pd);
                break;

            case PHARMACY_RETAIL_SALE_REFUND:
                handleOPDBillCancel(b, pd);
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

            case PATIENT_DEPOSIT_CANCELLED:
                handlePatientDepositBillCancel(b, pd);
                break;

            default:
                throw new AssertionError();
        }
    }

    @Deprecated // Use the methods in  PatientDepositService
    public void handlePatientDepositBill(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance + b.getNetTotal();
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDeposit, pd, b, beforeBalance, afterBalance, Math.abs(b.getNetTotal()));
    }

    @Deprecated // Use the methods in  PatientDepositService
    public void handlePatientDepositBillReturn(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositReturn, pd, b, beforeBalance, afterBalance, 0 - Math.abs(b.getNetTotal()));
    }

    @Deprecated // Use the methods in  PatientDepositService
    public void handlePatientDepositBillCancel(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositCancel, pd, b, beforeBalance, afterBalance, 0 - Math.abs(b.getNetTotal()));
    }

    @Deprecated // Use the methods in  PatientDepositService
    public void handleOPDBill(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilization, pd, b, beforeBalance, afterBalance, 0 - Math.abs(b.getNetTotal()));
    }

    @Deprecated // Use the methods in  PatientDepositService
    public void handleOPDBillCancel(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance + Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilizationCancel, pd, b, beforeBalance, afterBalance, Math.abs(b.getNetTotal()));
    }

    @Deprecated // Use the methods in  PatientDepositService
    public void handleOPDBillRefund(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance + Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilizationReturn, pd, b, beforeBalance, afterBalance, Math.abs(b.getNetTotal()));
    }

    @Deprecated // Use the methods in  PatientDepositService
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

    @Deprecated // Use the methods in  PatientDepositService
    public PatientDeposit getDepositOfThePatient(Patient p, Department d) {
        Map m = new HashMap<>();
        boolean departmentSpecificDeposits = configOptionApplicationController.getBooleanValueByKey(
                "Patient Deposits are Department Specific", false
        );

        String jpql = "select pd from PatientDeposit pd"
                + " where pd.patient.id=:pt "
                + (departmentSpecificDeposits ? " and pd.department.id=:dep " : "")
                + " and pd.retired=:ret";

        m.put("pt", p.getId());
        if (departmentSpecificDeposits) {
            m.put("dep", d.getId());
        }
        m.put("ret", false);

        PatientDeposit pd = patientDepositFacade.findFirstByJpql(jpql, m);

        patientController.save(p);

        // Refresh patient to ensure it's in current UnitOfWork
        Patient managedPatient = patientFacade.find(p.getId());

        if (pd == null) {
            pd = new PatientDeposit();
            pd.setBalance(0.0);
            pd.setPatient(managedPatient);
            pd.setDepartment(d);
            pd.setInstitution(d.getInstitution());
            pd.setCreater(sessionController.getLoggedUser());
            pd.setCreatedAt(new Date());
            patientDepositFacade.create(pd);
        }
        return pd;
    }

    public PatientDeposit checkDepositOfThePatient(Patient p, Department d) {
        if (p == null) {
            return new PatientDeposit();
        }
        Map m = new HashMap<>();
        boolean departmentSpecificDeposits = configOptionApplicationController.getBooleanValueByKey(
                "Patient Deposits are Department Specific", false
        );

        String jpql = "select pd from PatientDeposit pd"
                + " where pd.patient.id=:pt "
                + (departmentSpecificDeposits ? " and pd.department.id=:dep " : "")
                + " and pd.retired=:ret";

        m.put("pt", p.getId());
        if (departmentSpecificDeposits) {
            m.put("dep", d.getId());
        }
        m.put("ret", false);

        PatientDeposit pd = patientDepositFacade.findFirstByJpql(jpql, m);
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
        if(paymentMethodData==null){
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new Bill();
        }
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

    @Override
    public void listnerForPaymentMethodChange() {
        // ToDo: Add Logic
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
