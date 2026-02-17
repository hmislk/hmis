package com.divudi.core.facade;


import com.divudi.core.entity.cashTransaction.DenominationTransaction;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dr M H Buddhika Ariyaratne - buddhika.ari@gmail.com
 *
 */
@Stateless
public class DenominationTransactionFacade extends AbstractFacade<DenominationTransaction> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public DenominationTransactionFacade() {
        super(DenominationTransaction.class);
    }

}
