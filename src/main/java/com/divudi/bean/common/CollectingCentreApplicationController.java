package com.divudi.bean.common;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.HistoryType;
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
