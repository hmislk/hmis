/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.lab.Machine;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Sniper 619
 */
@Stateless
public class MachineFacade extends AbstractFacade<Machine> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public MachineFacade() {
        super(Machine.class);
    }

}
