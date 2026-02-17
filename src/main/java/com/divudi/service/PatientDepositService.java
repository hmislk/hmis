package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.HistoryType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.PatientDepositHistory;
import com.divudi.core.entity.Payment;
import com.divudi.core.facade.PatientDepositFacade;
import com.divudi.core.facade.PatientDepositHistoryFacade;
import com.divudi.core.facade.PatientFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author M H Buddhika Ariyaratne, buddhika.ari@gmail.com
 *
 */
@Stateless
public class PatientDepositService {

    @EJB
    private PatientDepositFacade patientDepositFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    PatientDepositHistoryFacade patientDepositHistoryFacade;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

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

        save(p);

        // Refresh patient to ensure it's in current UnitOfWork
        Patient managedPatient = patientFacade.find(p.getId());

        if (pd == null) {
            pd = new PatientDeposit();
            pd.setBalance(0.0);
            pd.setPatient(managedPatient);
            pd.setDepartment(d);
            pd.setInstitution(d.getInstitution());
            pd.setSite(d.getSite());
            pd.setCreatedAt(new Date());
            patientDepositFacade.create(pd);
        }
        return pd;
    }

    private void save(Patient p) {
        if (p == null) {
            return;
        }
        if (p.getId() == null) {
            patientFacade.create(p);
        } else {
            patientFacade.edit(p);
        }
    }

    public void updateBalance(Payment p, PatientDeposit pd) {
        switch (p.getBill().getBillTypeAtomic()) {
            case PATIENT_DEPOSIT:
                handleInPayment(p, pd);
                break;

            case PATIENT_DEPOSIT_REFUND:
                handleOutPayment(p, pd);
                break;

            case OPD_BATCH_BILL_WITH_PAYMENT:
            case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
            case INWARD_APPOINTMENT_BILL:
                handleOutPayment(p, pd);
                break;

            case PHARMACY_RETAIL_SALE:
                handleOutPayment(p, pd);
                break;

            case PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER:
                handleOutPayment(p, pd);
                break;

            case PHARMACY_RETAIL_SALE_CANCELLED:
                handleInPayment(p, pd);
                break;

            case PHARMACY_RETAIL_SALE_REFUND:
            case PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS:
            case PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS:
                handleInPayment(p, pd);
                break;

            case OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BILL_CANCELLATION:
            case PACKAGE_OPD_BILL_REFUND:
            case INWARD_APPOINTMENT_CANCEL_BILL:
                handleInPayment(p, pd);
                break;

            case OPD_BILL_CANCELLATION:
                handleInPayment(p, pd);
                break;

            case OPD_BILL_REFUND:
                handleInPayment(p, pd);
                break;

            case PATIENT_DEPOSIT_CANCELLED:
                handleOutPayment(p, pd);
                break;

            case INWARD_DEPOSIT:
                handleOutPayment(p, pd);
                break;

            default:
                throw new AssertionError();
        }
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
            case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
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
            case PACKAGE_OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BILL_CANCELLATION:
            case PACKAGE_OPD_BILL_REFUND:
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

    public void handlePatientDepositBillCancel(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositCancel, pd, b, beforeBalance, afterBalance, 0 - Math.abs(b.getNetTotal()));
    }

    public void handleOPDBill(Bill b, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs(b.getNetTotal());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilization, pd, b, beforeBalance, afterBalance, 0 - Math.abs(b.getNetTotal()));
    }

    public void handleInPayment(Payment p, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance + Math.abs(p.getPaidValue());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilization, pd, p.getBill(), beforeBalance, afterBalance,  Math.abs(p.getPaidValue()));
    }

    public void handleOutPayment(Payment p, PatientDeposit pd) {
        Double beforeBalance = pd.getBalance();
        Double afterBalance = beforeBalance - Math.abs( p.getPaidValue());
        pd.setBalance(afterBalance);
        patientDepositFacade.edit(pd);
        JsfUtil.addSuccessMessage("Balance Updated.");
        createPatientDepositHitory(HistoryType.PatientDepositUtilization, pd, p.getBill(), beforeBalance, afterBalance,  0 - Math.abs(p.getPaidValue()));
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
        pdh.setCreater(b.getCreater());
        pdh.setCreatedAt(new Date());
        pdh.setDepartment(b.getDepartment());
        pdh.setInstitution(b.getInstitution());

        // Set site from department
        Institution site = null;
        if (b.getDepartment() != null && b.getDepartment().getSite() != null) {
            site = b.getDepartment().getSite();
        }
        pdh.setSite(site);

        // Calculate and set this patient's aggregate balances
        Patient patient = pd.getPatient();
        pdh.setPatientDepositBalanceForSite(calculatePatientBalanceForSite(patient, site));
        pdh.setPatientDepositBalanceForInstitution(calculatePatientBalanceForInstitution(patient, b.getInstitution()));
        pdh.setPatientDepositBalanceForAllInstitutions(calculatePatientBalanceGlobal(patient));

        // Calculate and set all patients' aggregate balances
        pdh.setAllPatientsDepositBalanceForDepartment(calculateAllPatientsBalanceForDepartment(b.getDepartment()));
        pdh.setAllPatientsDepositBalanceForSite(calculateAllPatientsBalanceForSite(site));
        pdh.setAllPatientsDepositBalanceForInstitution(calculateAllPatientsBalanceForInstitution(b.getInstitution()));
        pdh.setAllPatientsDepositBalanceForAllInstitutions(calculateAllPatientsBalanceGlobal());

        patientDepositHistoryFacade.create(pdh);
    }

    // ============================================
    // THIS PATIENT'S BALANCE CALCULATION METHODS
    // ============================================

    /**
     * Calculate this patient's total deposit balance across all departments in a site.
     */
    private Double calculatePatientBalanceForSite(Patient patient, Institution site) {
        if (patient == null || site == null) {
            return 0.0;
        }
        String jpql = "SELECT COALESCE(SUM(pd.balance), 0) FROM PatientDeposit pd "
                + "WHERE pd.patient = :patient "
                + "AND pd.site = :site "
                + "AND pd.retired = false";
        Map<String, Object> m = new HashMap<>();
        m.put("patient", patient);
        m.put("site", site);
        return patientDepositFacade.findDoubleByJpql(jpql, m);
    }

    /**
     * Calculate this patient's total deposit balance across all departments in an institution.
     */
    private Double calculatePatientBalanceForInstitution(Patient patient, Institution institution) {
        if (patient == null || institution == null) {
            return 0.0;
        }
        String jpql = "SELECT COALESCE(SUM(pd.balance), 0) FROM PatientDeposit pd "
                + "WHERE pd.patient = :patient "
                + "AND pd.institution = :institution "
                + "AND pd.retired = false";
        Map<String, Object> m = new HashMap<>();
        m.put("patient", patient);
        m.put("institution", institution);
        return patientDepositFacade.findDoubleByJpql(jpql, m);
    }

    /**
     * Calculate this patient's total deposit balance globally (no filter).
     */
    private Double calculatePatientBalanceGlobal(Patient patient) {
        if (patient == null) {
            return 0.0;
        }
        String jpql = "SELECT COALESCE(SUM(pd.balance), 0) FROM PatientDeposit pd "
                + "WHERE pd.patient = :patient "
                + "AND pd.retired = false";
        Map<String, Object> m = new HashMap<>();
        m.put("patient", patient);
        return patientDepositFacade.findDoubleByJpql(jpql, m);
    }

    // ============================================
    // ALL PATIENTS' BALANCE CALCULATION METHODS
    // ============================================

    /**
     * Calculate sum of ALL patients' deposit balances for a department.
     */
    private Double calculateAllPatientsBalanceForDepartment(Department department) {
        if (department == null) {
            return 0.0;
        }
        String jpql = "SELECT COALESCE(SUM(pd.balance), 0) FROM PatientDeposit pd "
                + "WHERE pd.department = :department "
                + "AND pd.retired = false";
        Map<String, Object> m = new HashMap<>();
        m.put("department", department);
        return patientDepositFacade.findDoubleByJpql(jpql, m);
    }

    /**
     * Calculate sum of ALL patients' deposit balances for a site.
     */
    private Double calculateAllPatientsBalanceForSite(Institution site) {
        if (site == null) {
            return 0.0;
        }
        String jpql = "SELECT COALESCE(SUM(pd.balance), 0) FROM PatientDeposit pd "
                + "WHERE pd.site = :site "
                + "AND pd.retired = false";
        Map<String, Object> m = new HashMap<>();
        m.put("site", site);
        return patientDepositFacade.findDoubleByJpql(jpql, m);
    }

    /**
     * Calculate sum of ALL patients' deposit balances for an institution.
     */
    private Double calculateAllPatientsBalanceForInstitution(Institution institution) {
        if (institution == null) {
            return 0.0;
        }
        String jpql = "SELECT COALESCE(SUM(pd.balance), 0) FROM PatientDeposit pd "
                + "WHERE pd.institution = :institution "
                + "AND pd.retired = false";
        Map<String, Object> m = new HashMap<>();
        m.put("institution", institution);
        return patientDepositFacade.findDoubleByJpql(jpql, m);
    }

    /**
     * Calculate sum of ALL patients' deposit balances globally.
     */
    private Double calculateAllPatientsBalanceGlobal() {
        String jpql = "SELECT COALESCE(SUM(pd.balance), 0) FROM PatientDeposit pd "
                + "WHERE pd.retired = false";
        Map<String, Object> m = new HashMap<>();
        return patientDepositFacade.findDoubleByJpql(jpql, m);
    }

}
