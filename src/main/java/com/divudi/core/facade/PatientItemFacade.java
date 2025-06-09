/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.PatientItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class PatientItemFacade extends AbstractFacade<PatientItem> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em==null){
            ////// // System.out.println("em is nulkl");
        }
        if(em == null){}return em;
    }

    public PatientItemFacade() {
        super(PatientItem.class);
    }

}
