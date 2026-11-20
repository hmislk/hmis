package com.divudi.core.facade;

import com.divudi.core.entity.MaintenanceWindow;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class MaintenanceWindowFacade extends AbstractFacade<MaintenanceWindow> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public MaintenanceWindowFacade() {
        super(MaintenanceWindow.class);
    }
}
