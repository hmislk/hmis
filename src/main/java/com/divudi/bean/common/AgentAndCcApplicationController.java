package com.divudi.bean.common;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.HistoryType;
import static com.divudi.data.HistoryType.CollectingCentreBilling;
import static com.divudi.data.HistoryType.CollectingCentreDebitNoteCancel;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.InstitutionFacade;
import javax.inject.Named;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.ApplicationScoped;

/**
 *
 * @author Buddhika
 */
@Named
@ApplicationScoped
public class AgentAndCcApplicationController {

    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    InstitutionFacade institutionFacade;

    // A map to store locks for each collecting centre based on its ID
    private final Map<Long, Lock> lockMap = new ConcurrentHashMap<>();

    public AgentAndCcApplicationController() {
    }

    public void updateCcBalance(Institution collectingCentre,
            double hospitalFee,
            double collectingCentreFee,
            double staffFee,
            double transactionValue,
            HistoryType historyType,
            Bill bill) {
        updateCcBalance(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, historyType, bill, "");
    }

    public void updateCcBalance(Institution collectingCentre,
            double hospitalFee,
            double collectingCentreFee,
            double staffFee,
            double transactionValue,
            HistoryType historyType,
            Bill bill,
            String comments) {
        switch (historyType) {
            case CollectingCentreBalanceUpdateBill:
                handleCcBalanceUpdateBill(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill, comments);
                break;
            case CollectingCentreBillingCancel:
                handleCcBillingCancel(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreCreditNoteCancel:
                handleCcCreditNoteCancel(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreDebitNote:
                handleCcDebitNote(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreDeposit:
                handleCcDeposit(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreDepositCancel:
                handleCcDepositCancel(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill, comments);
                break;
            case CollectingCentreBilling:
                handleCollectingCentreBilling(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreBillingRefund:
                //System.out.println("Refund");
                handleCcBillingRefund(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingentrePaymentMadeBill:
                handleCcCollectingCentrePaymentMade(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreCreditNote:
                handleCcCollectingCentreCreditNote(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreDebitNoteCancel:
                handleCcDebitNoteCancel(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill, comments);
            default:
//                handleDefault(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
        }
    }

    
    public Double ccBalanceBefore(Bill b){
        String jpql;
        jpql = "select h.balanceBeforeTransaction "
                + " from AgentHistory h "
                + " where h.bill=:bill";
        Map params = new HashMap();
        params.put("bill", b);
        return agentHistoryFacade.findDoubleByJpql(jpql, params);
    }
    
    public Double ccBalanceAfter(Bill b){
        String jpql;
        jpql = "select h.balanceAfterTransaction "
                + " from AgentHistory h "
                + " where h.bill=:bill";
        Map params = new HashMap();
        params.put("bill", b);
        return agentHistoryFacade.findDoubleByJpql(jpql, params);
    }
    
    private void handleCcBalanceUpdateBill(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill, String comments) {

        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreBalanceUpdateBill);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(0);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(0);
            agentHistory.setComment(comments);

            double balanceBeforeTx = collectingCentre.getBallance();

            double balanceAfterTx = balanceBeforeTx;

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

//            collectingCentre.setBallance(balanceAfterTx);
//            institutionFacade.edit(collectingCentre);
        } finally {
            lock.unlock();
        }

    }

    private void handleCcBillingCancel(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreBillingCancel);
            agentHistory.setCompanyTransactionValue(0 - Math.abs(hospitalFee));
            agentHistory.setAgentTransactionValue(0 - Math.abs(collectingCentreFee));
            agentHistory.setStaffTrasnactionValue(0 - Math.abs(staffFee));
            //agentHistory.setTransactionValue(0 - Math.abs(transactionValue));
            agentHistory.setTransactionValue(hospitalFee);
            agentHistory.setPaidAmountByAgency(null);

            double balanceBeforeTx = collectingCentre.getBallance();
            double balanceAfterTx = balanceBeforeTx + Math.abs(hospitalFee);

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCcBillingRefund(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreBillingCancel);
            agentHistory.setCompanyTransactionValue(0 - Math.abs(hospitalFee));
            agentHistory.setAgentTransactionValue(0 - Math.abs(collectingCentreFee));
            agentHistory.setStaffTrasnactionValue(0 - Math.abs(staffFee));
            agentHistory.setTransactionValue(hospitalFee);
            agentHistory.setPaidAmountByAgency(null);

            double balanceBeforeTx = collectingCentre.getBallance();
            double balanceAfterTx = balanceBeforeTx + Math.abs(hospitalFee);
            
            System.out.println("Before Balance = " + collectingCentre.getBallance());
            System.out.println("Refund Value = " + hospitalFee);
            
            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);
            
            System.out.println("After Balance = " + collectingCentre.getBallance());

        } finally {
            lock.unlock();
        }
    }

    private void handleCcCollectingCentreCreditNote(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreCreditNote);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(0);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(Math.abs(transactionValue));
            agentHistory.setAdjustmentToAgencyBalance(Math.abs(transactionValue));
            agentHistory.setPaidAmountByAgency(null);

            double balanceBeforeTx = collectingCentre.getBallance();
            double balanceAfterTx = balanceBeforeTx + Math.abs(transactionValue);

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCcCollectingCentrePaymentMade(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreCreditNote);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(0);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(Math.abs(transactionValue));
            agentHistory.setPaidAmountByAgency(null);

            double balanceBeforeTx = collectingCentre.getBallance();
            double balanceAfterTx = balanceBeforeTx + Math.abs(transactionValue);

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCcCreditNoteCancel(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Implementation for CollectingCentreCreditNoteCancel
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreCreditNoteCancel);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(0);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(0 - Math.abs(transactionValue));
            agentHistory.setAdjustmentToAgencyBalance(0 - Math.abs(transactionValue));
            agentHistory.setPaidAmountByAgency(null);

            double balanceBeforeTx = collectingCentre.getBallance();
            double balanceAfterTx = balanceBeforeTx + transactionValue;

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCcDebitNoteCancel(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill, String comment) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreDebitNoteCancel);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(0);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(Math.abs(transactionValue));
            agentHistory.setAdjustmentToAgencyBalance(Math.abs(transactionValue));
            agentHistory.setPaidAmountByAgency(null);
            agentHistory.setComment(comment);

            double balanceBeforeTx = collectingCentre.getBallance();
            double balanceAfterTx = balanceBeforeTx + Math.abs(transactionValue);

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCcDebitNote(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreDebitNote);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(0);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(0 - Math.abs(transactionValue));
            agentHistory.setAdjustmentToAgencyBalance(0 - Math.abs(transactionValue));
            agentHistory.setPaidAmountByAgency(null);

            double balanceBeforeTx = collectingCentre.getBallance();
            double balanceAfterTx = balanceBeforeTx - Math.abs(transactionValue);

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCcDeposit(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreDeposit);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(transactionValue);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(transactionValue);
            agentHistory.setPaidAmountByAgency(transactionValue);

            double balanceBeforeTx = collectingCentre.getBallance();

            double balanceAfterTx = balanceBeforeTx + collectingCentreFee;

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCcDepositCancel(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill, String comments) {
        // Implementation for CollectingCentreDepositCancel
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreDepositCancel);
            agentHistory.setCompanyTransactionValue(0);
            agentHistory.setAgentTransactionValue(transactionValue);
            agentHistory.setStaffTrasnactionValue(0);
            agentHistory.setTransactionValue(0 - Math.abs(transactionValue));
            agentHistory.setPaidAmountByAgency(transactionValue);
            agentHistory.setComment(comments);

            double balanceBeforeTx = collectingCentre.getBallance();

            double balanceAfterTx = balanceBeforeTx + collectingCentreFee;

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleCollectingCentreBilling(Institution collectingCentre,
            double hospitalFee,
            double collectingCentreFee,
            double staffFee,
            double transactionValue,
            Bill bill) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            collectingCentre = institutionFacade.findWithoutCache(collectingCentre.getId());
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(bill.getCreater());
            agentHistory.setBill(bill);
            agentHistory.setInstitution(bill.getInstitution());
            agentHistory.setDepartment(bill.getDepartment());
            agentHistory.setAgency(collectingCentre);
            agentHistory.setReferenceNumber(bill.getAgentRefNo());
            agentHistory.setHistoryType(HistoryType.CollectingCentreBilling);
            agentHistory.setCompanyTransactionValue(hospitalFee);
            agentHistory.setAgentTransactionValue(collectingCentreFee);
            agentHistory.setStaffTrasnactionValue(staffFee);
            //agentHistory.setTransactionValue(transactionValue);
            agentHistory.setTransactionValue(0 - Math.abs(hospitalFee));

            double balanceBeforeTx = collectingCentre.getBallance();

            double balanceAfterTx = balanceBeforeTx - hospitalFee;

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.editAndCommit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

//    @Deprecated
//    private void handleDefault(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
//        // Default case handling if necessary
//    }
//
//    @Deprecated
//    public void updateBalance(Institution collectingCentre,
//            double collectingCenterFeeValue,
//            double valueWithoutccFee,
//            double transactionValue,
//            HistoryType historyType,
//            Bill bill,
//            String refNo) {
//        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
//        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
//        lock.lock();
//        try {
//            if (bill.getBillTypeAtomic() == BillTypeAtomic.CC_BILL) {
//                AgentHistory agentHistory = new AgentHistory();
//                agentHistory.setCreatedAt(new Date());
//                agentHistory.setCreater(bill.getCreater());
//                agentHistory.setBill(bill);
//                agentHistory.setInstitution(bill.getInstitution());
//                agentHistory.setDepartment(bill.getDepartment());
//                agentHistory.setAgency(collectingCentre);
//                agentHistory.setReferenceNumber(refNo);
//                agentHistory.setHistoryType(historyType);
//
//                double balanceAfterTx = 0;
//                double balanceAfterTransaction = 0;
//                double agentBalanceAfterTx = 0;
//
//                switch (historyType) {
//                    case CollectingCentreBilling:
//
//                        break;
//                    default:
//                        balanceAfterTx = collectingCentre.getBallance() - valueWithoutccFee;
//                        balanceAfterTransaction = collectingCentre.getAgentBalance() - valueWithoutccFee;
//                        agentBalanceAfterTx = collectingCentre.getCompanyBalance() - valueWithoutccFee;
//                }
//
//                agentHistory.setBalanceBeforeTransaction(collectingCentre.getBallance());
//                agentHistory.setBalanceAfterTransaction(balanceAfterTx);
//                agentHistory.setTransactionValue(valueWithoutccFee);
//
//                agentHistory.setCompanyBalanceBefore(collectingCentre.getCompanyBalance());
//                agentHistory.setCompanyBalanceAfter(collectingCentre.getCompanyBalance());
//
//                agentHistory.setCompanyTransactionValue(valueWithoutccFee);
//                agentHistory.setTransactionValue(valueWithoutccFee);
//
//                agentHistory.setCollectingCentertransactionValue(collectingCenterFeeValue);
//                agentHistoryFacade.create(agentHistory);
//
//                collectingCentre.setBallance(balanceAfterTx);
//                institutionFacade.editAndCommit(collectingCentre);
//            }
//
//        } finally {
//            lock.unlock();
//        }
//    }

}
