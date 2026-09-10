/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.pharmacy.PharmacyTransferIssueDraftItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class PharmacyTransferIssueDraftItemFacade extends AbstractFacade<PharmacyTransferIssueDraftItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PharmacyTransferIssueDraftItemFacade() {
        super(PharmacyTransferIssueDraftItem.class);
    }

    /**
     * The saved item-selection snapshot for a native Fast Issue draft, in the order the user
     * saw them at Save time.
     */
    public List<PharmacyTransferIssueDraftItem> findByDraftBill(Bill draftBill) {
        String jpql = "select d from PharmacyTransferIssueDraftItem d "
                + " where d.draftBill = :draftBill order by d.serialNo";
        Map<String, Object> params = new HashMap<>();
        params.put("draftBill", draftBill);
        return findByJpql(jpql, params);
    }
}
