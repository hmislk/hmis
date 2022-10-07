/*
 * To change this template, choose Tools | Templates
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.Speciality;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author buddhika
 */
@Stateless
public class SpecialityFacade extends AbstractFacade<Speciality> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public SpecialityFacade() {
        super(Speciality.class);
    }
    
}
