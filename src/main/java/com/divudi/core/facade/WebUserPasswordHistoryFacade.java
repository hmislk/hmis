/*
 * Author : WebUserPasswordHistoryFacade
 */
package com.divudi.core.facade;

import com.divudi.core.entity.WebUserPasswordHistory;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author OpenAI
 */
@Stateless
public class WebUserPasswordHistoryFacade extends AbstractFacade<WebUserPasswordHistory> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public WebUserPasswordHistoryFacade() {
        super(WebUserPasswordHistory.class);
    }

}
