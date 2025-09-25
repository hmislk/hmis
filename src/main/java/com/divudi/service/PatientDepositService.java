package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.HistoryType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
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

        if (pd == null) {
            pd = new PatientDeposit();
            pd.setBalance(0.0);
            pd.setPatient(p);
            pd.setDepartment(d);
            pd.setInstitution(d.getInstitution());
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
                handleOutPayment(p, pd);
                break;

            case PHARMACY_RETAIL_SALE:
                handleOutPayment(p, pd);
                break;

            case PHARMACY_RETAIL_SALE_CANCELLED:
                handleInPayment(p, pd);
                break;

            case PHARMACY_RETAIL_SALE_REFUND:
                handleInPayment(p, pd);
                break;

            case OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BILL_CANCELLATION:
            case PACKAGE_OPD_BILL_REFUND:
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
        patientDepositHistoryFacade.create(pdh);
    }


}
