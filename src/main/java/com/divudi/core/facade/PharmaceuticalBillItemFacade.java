/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.data.dto.ItemMovementSummaryDTO;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

/**
 *
 * @author safrin
 */
@Stateless
public class PharmaceuticalBillItemFacade extends AbstractFacade<PharmaceuticalBillItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public PharmaceuticalBillItemFacade() {
        super(PharmaceuticalBillItem.class);
    }

    public List<PharmaceuticalBillItem> getPharmaceuticalBillItems(Bill bill) {
//        System.out.println("getPharmaceuticalBillItems method bill = " + bill);
        String sql = "Select p "
                + " from PharmaceuticalBillItem p "
                + " where p.billItem.bill=:b "
                + " and p.billItem.retired=false";
        HashMap hm = new HashMap();
        hm.put("b", bill);
        List<PharmaceuticalBillItem> btm = findByJpql(sql, hm);
//        System.out.println("btm = " + btm.size());
        return btm;
    }

    public List<ItemMovementSummaryDTO> findItemMovementSummaryDTOs(String jpql, Map<String, Object> parameters, TemporalType tt) {
        TypedQuery<ItemMovementSummaryDTO> qry = getEntityManager().createQuery(jpql, ItemMovementSummaryDTO.class);
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            if (e.getValue() instanceof Date) {
                qry.setParameter(e.getKey(), (Date) e.getValue(), tt);
            } else {
                qry.setParameter(e.getKey(), e.getValue());
            }
        }
        return qry.getResultList();
    }

}
