/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.facade;

import com.divudi.entity.pharmacy.Vtm;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class VtmFacade extends AbstractFacade<Vtm> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public VtmFacade() {
        super(Vtm.class);
    }
    
}
