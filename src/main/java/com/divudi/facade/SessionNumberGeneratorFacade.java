/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.facade;

import com.divudi.entity.SessionNumberGenerator;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Neo
 */
@Stateless
public class SessionNumberGeneratorFacade extends AbstractFacade<SessionNumberGenerator> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public SessionNumberGeneratorFacade() {
        super(SessionNumberGenerator.class);
    }
    
}
