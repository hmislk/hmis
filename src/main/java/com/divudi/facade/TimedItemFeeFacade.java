/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.facade;

import com.divudi.entity.inward.TimedItemFee;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class TimedItemFeeFacade extends AbstractFacade<TimedItemFee> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public TimedItemFeeFacade() {
        super(TimedItemFee.class);
    }
    
}
