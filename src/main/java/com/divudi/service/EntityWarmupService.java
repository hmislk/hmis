package com.divudi.service;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.Vmp;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Warms up EclipseLink entity descriptors at application startup so the first
 * user-initiated operation does not pay the descriptor-init cost.
 *
 * Runs asynchronously so deployment is never blocked, even if a warmup query
 * stalls.
 *
 * IMPORTANT: an ID-only JPQL ({@code SELECT e.id FROM Entity e}) is NOT enough.
 * That path initialises the ReadAllQuery descriptor but does not exercise the
 * by-primary-key ReadObjectQuery, row-materialisation path, or the identity-map
 * cache region used by {@code em.find(Entity.class, id)}. On the first
 * {@code find()} after deploy, EclipseLink still pays a 20-second first-touch
 * cost for each entity class. To avoid that, we do one real {@code find()} per
 * class — fetching one row fully — so the user-facing path is already warm.
 */
@Singleton
@Startup
public class EntityWarmupService {

    private static final Logger LOGGER = Logger.getLogger(EntityWarmupService.class.getName());

    private static final Class<?>[] WARMUP_CLASSES = new Class<?>[]{
        ItemBatch.class,
        Item.class,
        Stock.class,
        Amp.class,
        Vmp.class,
        Department.class,
        Category.class
    };

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @PostConstruct
    public void init() {
        warmupAsync();
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void warmupAsync() {
        long total = System.currentTimeMillis();
        for (Class<?> entityClass : WARMUP_CLASSES) {
            warmupOne(entityClass);
        }
        LOGGER.log(Level.INFO, "[EntityWarmup] TOTAL: {0}ms",
                System.currentTimeMillis() - total);
    }

    private void warmupOne(Class<?> entityClass) {
        String name = entityClass.getSimpleName();
        try {
            long tId = System.currentTimeMillis();
            List<Long> ids = em.createQuery(
                    "SELECT e.id FROM " + name + " e", Long.class)
                    .setMaxResults(1)
                    .getResultList();
            long idMs = System.currentTimeMillis() - tId;

            if (ids.isEmpty()) {
                LOGGER.log(Level.INFO, "[EntityWarmup] {0} empty table, id-query {1}ms",
                        new Object[]{name, idMs});
                return;
            }

            long tFind = System.currentTimeMillis();
            em.find(entityClass, ids.get(0));
            long findMs = System.currentTimeMillis() - tFind;

            LOGGER.log(Level.INFO,
                    "[EntityWarmup] {0} warmed: idQuery={1}ms find={2}ms",
                    new Object[]{name, idMs, findMs});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[EntityWarmup] skipped " + name + ": " + e.getMessage());
        }
    }
}
