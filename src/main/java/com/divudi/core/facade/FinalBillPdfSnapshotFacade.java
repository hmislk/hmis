package com.divudi.core.facade;

import com.divudi.core.entity.FinalBillPdfSnapshot;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class FinalBillPdfSnapshotFacade extends AbstractFacade<FinalBillPdfSnapshot> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public FinalBillPdfSnapshotFacade() {
        super(FinalBillPdfSnapshot.class);
    }

    public FinalBillPdfSnapshot findByBillId(Long billId) {
        if (billId == null) {
            return null;
        }
        TypedQuery<FinalBillPdfSnapshot> q = em.createQuery(
                "select s from FinalBillPdfSnapshot s where s.bill.id = :billId and s.retired = false",
                FinalBillPdfSnapshot.class);
        q.setParameter("billId", billId);
        List<FinalBillPdfSnapshot> results = q.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
