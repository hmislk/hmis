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
            double valueWithoutHospitalFee,
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
                agentHistory.setBeforeBallance(collectingCentre.getBallance());
                Double ccBalanceAfterTx = collectingCentre.getBallance() - valueWithoutHospitalFee;
                agentHistory.setAfterBallance(ccBalanceAfterTx);
                agentHistory.setTransactionValue(valueWithoutHospitalFee);
                agentHistory.setCollectingCentertransactionValue(collectingCenterFeeValue);
                agentHistory.setReferenceNumber(refNo);
                agentHistory.setHistoryType(historyType);
                agentHistoryFacade.create(agentHistory);
                collectingCentre.setBallance(ccBalanceAfterTx);
                institutionFacade.edit(collectingCentre);
            }

        } finally {
            lock.unlock();
        }
    }

}
