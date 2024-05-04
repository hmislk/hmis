/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;


import com.divudi.entity.channel.SessionInstanceActivity;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class SessionInstanceActivityFacade extends AbstractFacade<SessionInstanceActivity> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public SessionInstanceActivityFacade() {
        super(SessionInstanceActivity.class);
    }
    
}
