package com.divudi.service;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.EntityManagerFactory;

// EclipseLink fallback
import org.eclipse.persistence.jpa.JpaHelper;

@Stateless
public class CacheAdminService {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager emMain;

    @PersistenceContext(unitName = "hmisAuditPU", name = "hmisAuditPU")
    private EntityManager emAudit;

    public void clearAll() {
        clear(emMain, "hmisPU");
        clear(emAudit, "hmisAuditPU");
    }

    private void clear(EntityManager em, String puName) {
        try {
            if (em == null) {
                return;
            }
            EntityManagerFactory emf = em.getEntityManagerFactory();
            if (emf != null) {
                try {
                    emf.getCache().evictAll();
                } catch (Exception e) {
                    // Fallback for rare contexts where getCache is unavailable
                    try {
                        JpaHelper.getServerSession(emf)
                                .getIdentityMapAccessor()
                                .initializeAllIdentityMaps();
                    } catch (Exception ignored) {
                        // swallow — best-effort clear
                    }
                }
            }
        } catch (Exception ignored) {
            // best-effort; no rethrow
        }
    }
}

