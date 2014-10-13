/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.facade;

import com.divudi.entity.BatchBill;
import com.divudi.facade.util.JsfUtil;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class BatchBillFacade extends AbstractFacade<BatchBill> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
            JsfUtil.addErrorMessage("null em");
        }
        if (em == null) {
        }
        return em;
    }

    public BatchBillFacade() {
        super(BatchBill.class);
    }

}
