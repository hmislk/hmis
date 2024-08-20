/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.pharmacy.AdjustmentCategory;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class AdjustmentCategoryFacade extends AbstractFacade<AdjustmentCategory> {
    @PersistenceContext(unitName = "hmisPU")    
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public AdjustmentCategoryFacade() {
        super(AdjustmentCategory.class);
    }
    
}
