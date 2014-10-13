/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.facade;

import com.divudi.entity.Sex;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * 
 * @author Dr. M H B Ariyaratne <buddhika.ari at gmail.com>
 Dr. M H B Ariyaratne, MBBS, MSc(Biomedical Informatics)
 MO (Health Information)
 Department of Health Services, Southern Province
 buddhika.ari@gmail.com
 +94 71 58 123 99
 *
 */
@Stateless
public class SexFacade extends AbstractFacade<Sex> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public SexFacade() {
        super(Sex.class);
    }

}
