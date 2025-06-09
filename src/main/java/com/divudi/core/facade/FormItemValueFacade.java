/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.FormItemValue;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class FormItemValueFacade extends AbstractFacade<FormItemValue> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public FormItemValueFacade() {
        super(FormItemValue.class);
    }

}
