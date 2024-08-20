/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.inward.TimedItemCategory;
import com.divudi.bean.common.util.JsfUtil;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class TimedItemCategoryFacade extends AbstractFacade<TimedItemCategory> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null){
            JsfUtil.addErrorMessage("null em");
        }
        if(em == null){}return em;
    }

    public TimedItemCategoryFacade() {
        super(TimedItemCategory.class);
    }
    
}
