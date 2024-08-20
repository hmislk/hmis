/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.facade;

import com.divudi.entity.channel.AgentReferenceBook;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Sniper
 */
@Stateless
public class AgentReferenceBookFacade extends AbstractFacade<AgentReferenceBook> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public AgentReferenceBookFacade() {
        super(AgentReferenceBook.class);
    }
    
}
