/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.Bill;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import java.util.HashMap;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
        String sql = "Select p "
                + " from PharmaceuticalBillItem p "
                + " where p.billItem.bill=:b "
                + " and p.billItem.retired=false";
        HashMap hm = new HashMap();
        hm.put("b", bill);
        List<PharmaceuticalBillItem> btm = findByJpql(sql, hm);
        return btm;
    }

}
