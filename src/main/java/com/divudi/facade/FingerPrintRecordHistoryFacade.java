/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.facade;

import com.divudi.entity.hr.FingerPrintRecordHistory;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class FingerPrintRecordHistoryFacade extends AbstractFacade<FingerPrintRecordHistory> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public FingerPrintRecordHistoryFacade() {
        super(FingerPrintRecordHistory.class);
    }
    
}
