/*
 * To change this template, choose Tools | Templates
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.lab.TestFlag;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class TestFlagFacade extends AbstractFacade<TestFlag> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public TestFlagFacade() {
        super(TestFlag.class);
    }
    
}
