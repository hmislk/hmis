/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.facade;

import com.divudi.entity.IssueRateMargins;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author pdhs
 */
@Stateless
public class IssueRateMarginsFacade extends AbstractFacade<IssueRateMargins> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public IssueRateMarginsFacade() {
        super(IssueRateMargins.class);
    }
    
}
