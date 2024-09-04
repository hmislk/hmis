package com.divudi.bean.common;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.HistoryType;
import static com.divudi.data.HistoryType.CollectingCentreBilling;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.InstitutionFacade;
import javax.inject.Named;
import java.util.Date;
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
public class CollectingCentreApplicationController {

    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    InstitutionFacade institutionFacade;

    // A map to store locks for each collecting centre based on its ID
    private final Map<Long, Lock> lockMap = new ConcurrentHashMap<>();

    public CollectingCentreApplicationController() {
    }

    public void updateBalance(Institution collectingCentre,
            double hospitalFee,
            double collectingCentreFee,
            double staffFee,
            double transactionValue,
            HistoryType historyType,
            Bill bill) {
        switch (historyType) {
            case CollectingCentreBalanceUpdateBill:
                handleBalanceUpdateBill(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreBillingCancel:
                handleBillingCancel(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreCreditNoteCancel:
                handleCreditNoteCancel(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreDebitNote:
                handleDebitNote(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreDeposit:
                handleDeposit(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreDepositCancel:
                handleDepositCancel(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            case CollectingCentreBilling:
                handleCollectingCentreBilling(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
            default:
                handleDefault(collectingCentre, hospitalFee, collectingCentreFee, staffFee, transactionValue, bill);
                break;
        }
    }

    private void handleBalanceUpdateBill(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Implementation for CollectingCentreBalanceUpdateBill
    }

    private void handleBillingCancel(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Implementation for CollectingCentreBillingCancel
    }

    private void handleCreditNoteCancel(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Implementation for CollectingCentreCreditNoteCancel
    }

    private void handleDebitNote(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Implementation for CollectingCentreDebitNote
    }

    private void handleDeposit(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Implementation for CollectingCentreDeposit
    }

    private void handleDepositCancel(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Implementation for CollectingCentreDepositCancel
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
            agentHistory.setTransactionValue(transactionValue);
            
            
             double balanceBeforeTx = collectingCentre.getCompanyBalance();
             
            double balanceAfterTx = balanceBeforeTx - collectingCentreFee;

          
            

            agentHistory.setBalanceBeforeTransaction(balanceBeforeTx);
            agentHistory.setBalanceAfterTransaction(balanceAfterTx);
            
            
            agentHistory.setCompanyBalanceAfter(collectingCentre.getCompanyBalance());

            agentHistoryFacade.create(agentHistory);

            collectingCentre.setBallance(balanceAfterTx);
            institutionFacade.edit(collectingCentre);

        } finally {
            lock.unlock();
        }
    }

    private void handleDefault(Institution collectingCentre, double hospitalFee, double collectingCentreFee, double staffFee, double transactionValue, Bill bill) {
        // Default case handling if necessary
    }

    public void updateBalance(Institution collectingCentre,
            double collectingCenterFeeValue,
            double valueWithoutccFee,
            double transactionValue,
            HistoryType historyType,
            Bill bill,
            String refNo) {
        Long collectingCentreId = collectingCentre.getId(); // Assuming each Institution has a unique ID
        Lock lock = lockMap.computeIfAbsent(collectingCentreId, id -> new ReentrantLock());
        lock.lock();
        try {
            if (bill.getBillTypeAtomic() == BillTypeAtomic.CC_BILL) {
                AgentHistory agentHistory = new AgentHistory();
                agentHistory.setCreatedAt(new Date());
                agentHistory.setCreater(bill.getCreater());
                agentHistory.setBill(bill);
                agentHistory.setInstitution(bill.getInstitution());
                agentHistory.setDepartment(bill.getDepartment());
                agentHistory.setAgency(collectingCentre);
                agentHistory.setReferenceNumber(refNo);
                agentHistory.setHistoryType(historyType);

                double balanceAfterTx = 0;
                double balanceAfterTransaction = 0;
                double agentBalanceAfterTx = 0;

                switch (historyType) {
                    case CollectingCentreBilling:

                        break;
                    default:
                        balanceAfterTx = collectingCentre.getBallance() - valueWithoutccFee;
                        balanceAfterTransaction = collectingCentre.getAgentBalance() - valueWithoutccFee;
                        agentBalanceAfterTx = collectingCentre.getCompanyBalance() - valueWithoutccFee;
                }

                agentHistory.setBalanceBeforeTransaction(collectingCentre.getBallance());
                agentHistory.setBalanceAfterTransaction(balanceAfterTx);
                agentHistory.setTransactionValue(valueWithoutccFee);

                agentHistory.setCompanyBalanceBefore(collectingCentre.getCompanyBalance());
                agentHistory.setCompanyBalanceAfter(collectingCentre.getCompanyBalance());

                agentHistory.setCompanyTransactionValue(valueWithoutccFee);
                agentHistory.setTransactionValue(valueWithoutccFee);

                agentHistory.setCollectingCentertransactionValue(collectingCenterFeeValue);
                agentHistoryFacade.create(agentHistory);

                collectingCentre.setBallance(balanceAfterTx);
                institutionFacade.edit(collectingCentre);
            }

        } finally {
            lock.unlock();
        }
    }

}
