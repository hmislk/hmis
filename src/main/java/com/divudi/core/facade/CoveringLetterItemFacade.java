/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.core.facade;

import com.divudi.core.entity.clinical.CoveringLetterItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author buddhika
 */
@Stateless
public class CoveringLetterItemFacade extends AbstractFacade<CoveringLetterItem> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public CoveringLetterItemFacade() {
        super(CoveringLetterItem.class);
    }

}
