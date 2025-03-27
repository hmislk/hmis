package com.divudi.facade;

import com.divudi.entity.report.ReportLog;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ReportLogFacade extends AbstractFacade<ReportLog> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager entityManager;

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public ReportLogFacade() {
        super(ReportLog.class);
    }
}
