/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.facade;

import com.divudi.entity.inward.InwardService;
import com.divudi.facade.util.JsfUtil;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class InwardServiceFacade extends AbstractFacade<InwardService> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){
            JsfUtil.addErrorMessage("Null em");
        }
        if(em == null){}return em;
    }

    public InwardServiceFacade() {
        super(InwardService.class);
    }
    
}
