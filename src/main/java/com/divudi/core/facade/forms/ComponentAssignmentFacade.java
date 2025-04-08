/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade.forms;


import com.divudi.core.entity.forms.ComponentAsignment;
import com.divudi.core.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Thamara Waidyarathna
 */
@Stateless
public class ComponentAssignmentFacade extends AbstractFacade<ComponentAsignment> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public ComponentAssignmentFacade() {
        super(ComponentAsignment.class);
    }

}
