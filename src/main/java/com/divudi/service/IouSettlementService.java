package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.facade.PaymentFacade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class IouSettlementService {

    @EJB
    private PaymentFacade paymentFacade;

    /**
     * Marks the IOUs as settled, all or none, before a settlement writes
     * anything else. The update matches only an open IOU, so when two requests
     * race (a double click, or two sessions) the database lets one through and
     * the other changes no row.
     *
     * @throws IouAlreadySettledException if any IOU is no longer open; nothing
     * is changed
     */
    public void claimOpenIous(List<Long> iouPaymentIds) {
        // The same order for every request, so two overlapping claims cannot deadlock
        List<Long> orderedIds = new ArrayList<>(iouPaymentIds);
        Collections.sort(orderedIds);
        String jpql = "update Payment p "
                + " set p.cancelled=:claimed "
                + " where p.id=:id "
                + " and p.retired=:ret "
                + " and p.paymentMethod=:pm "
                + " and p.cancelled=:open "
                + " and p.cancelledBill is null "
                + " and p.paidValue > 0 ";
        for (Long id : orderedIds) {
            Map<String, Object> params = new HashMap<>();
            params.put("claimed", true);
            params.put("id", id);
            params.put("ret", false);
            params.put("pm", PaymentMethod.IOU);
            params.put("open", false);
            if (paymentFacade.updateByJpql(jpql, params) != 1) {
                throw new IouAlreadySettledException("An IOU you selected is already settled or converted to cash. Reopen Settle IOUs and select again.");
            }
        }
    }

    /**
     * Gives back a claim made by {@link #claimOpenIous(List)} when the
     * settlement bill could not be created, so the IOUs are open again. Only a
     * claimed IOU that is not yet linked to a bill is changed: a row with a
     * cancelledBill belongs to a settlement or conversion that was saved, and
     * stays as it is.
     *
     * @param claimedIouPaymentIds the ids this request claimed
     * @return the number of IOUs reopened
     */
    public int releaseClaimedIous(List<Long> claimedIouPaymentIds) {
        if (claimedIouPaymentIds == null || claimedIouPaymentIds.isEmpty()) {
            return 0;
        }
        String jpql = "update Payment p "
                + " set p.cancelled=:open "
                + " where p.id in :ids "
                + " and p.retired=:ret "
                + " and p.paymentMethod=:pm "
                + " and p.cancelled=:claimed "
                + " and p.cancelledBill is null ";
        Map<String, Object> params = new HashMap<>();
        params.put("open", false);
        params.put("ids", new ArrayList<>(claimedIouPaymentIds));
        params.put("ret", false);
        params.put("pm", PaymentMethod.IOU);
        params.put("claimed", true);
        return paymentFacade.updateByJpql(jpql, params);
    }
}
