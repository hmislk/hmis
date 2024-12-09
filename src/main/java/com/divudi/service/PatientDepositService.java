package com.divudi.service;

import com.divudi.bean.common.util.JsfUtil;
import static com.divudi.data.BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION;
import static com.divudi.data.BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT;
import static com.divudi.data.BillTypeAtomic.OPD_BILL_CANCELLATION;
import static com.divudi.data.BillTypeAtomic.OPD_BILL_REFUND;
import static com.divudi.data.BillTypeAtomic.PATIENT_DEPOSIT;
import static com.divudi.data.BillTypeAtomic.PATIENT_DEPOSIT_CANCELLED;
import static com.divudi.data.BillTypeAtomic.PATIENT_DEPOSIT_REFUND;
import com.divudi.data.HistoryType;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientDeposit;
import com.divudi.entity.PatientDepositHistory;
import com.divudi.facade.PatientDepositFacade;
import com.divudi.facade.PatientDepositHistoryFacade;
import com.divudi.facade.PatientFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

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
