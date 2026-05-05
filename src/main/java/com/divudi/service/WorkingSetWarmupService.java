package com.divudi.service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.queries.FetchGroup;

/**
 * Bulk-loads the active pharmacy working set into the EclipseLink L2 cache at
 * deploy time, so the first user-driven Settle on a never-before-touched
 * Stock / ItemBatch / Item does not pay the 15-second EAGER-cascade tax.
 *
 * Three independent FetchGroup loads, each in its own REQUIRES_NEW so a
 * failure in one does not roll back the others, and so cache write locks are
 * released between phases (avoids the 50,000-SELECT recursive deadlock the
 * earlier plain-JPA bulk load triggered):
 *
 *   1. Stock(id, stock)        — for `WHERE s.stock > 0`
 *   2. ItemBatch(id)           — id only, just to put rows in the L2 map
 *   3. Item(id, name)          — id + name (the only field downstream walks)
 *
 * Each FetchGroup names ONLY the listed attributes; EclipseLink suppresses
 * resolution of every other @ManyToOne edge (Department / Dealer / Make /
 * Institution / WebUser / Category / etc.). This produces ONE SELECT per
 * batch instead of an N+1 EAGER cascade.
 *
 * Probe timings on this developer machine (Issue #20138):
 *   - plain JPA load 1 Stock row, full EAGER cascade .. 17,679 ms
 *   - FetchGroup id+stock, 1 row .....................      2 ms
 *   - FetchGroup id+stock, 100 rows ..................     17 ms
 *   - FetchGroup id+stock, 6,020 rows .................    175 ms
 * After the warmup, em.find on a warmed Stock and walking
 * stock.itemBatch.item.name took 0 ms — the lazy fill resolves against
 * already-cached entities for free.
 *
 * Runs on the EntityWarmupService startup daemon thread so deploy is never
 * blocked. Each step's failure is logged but ignored.
 */
@Stateless
public class WorkingSetWarmupService {

    private static final Logger LOGGER = Logger.getLogger(WorkingSetWarmupService.class.getName());
    private static final int CHUNK_SIZE = 1000;

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    public void warmActiveStockWorkingSet() {
        long total = System.currentTimeMillis();

        List<Long> stockIds = loadStockIds();
        if (stockIds == null || stockIds.isEmpty()) {
            LOGGER.log(Level.INFO, "[WorkingSetWarmup] no in-stock rows — skipping");
            return;
        }
        warmStocks(stockIds);

        List<Long> itemBatchIds = loadActiveItemBatchIds();
        if (itemBatchIds != null && !itemBatchIds.isEmpty()) {
            warmItemBatches(itemBatchIds);
        }

        List<Long> itemIds = loadActiveItemIds();
        if (itemIds != null && !itemIds.isEmpty()) {
            warmItems(itemIds);
        }

        LOGGER.log(Level.INFO, "[WorkingSetWarmup] TOTAL: {0}ms",
                System.currentTimeMillis() - total);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Long> loadStockIds() {
        try {
            long t = System.currentTimeMillis();
            List<Long> ids = em.createQuery(
                    "SELECT s.id FROM Stock s WHERE s.stock > 0", Long.class)
                    .getResultList();
            LOGGER.log(Level.INFO, "[WorkingSetWarmup] Stock id query: {0}ms, size={1}",
                    new Object[]{System.currentTimeMillis() - t, ids.size()});
            return ids;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[WorkingSetWarmup] Stock id query failed: " + e.getMessage(), e);
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void warmStocks(List<Long> ids) {
        try {
            FetchGroup fg = new FetchGroup("stock-min");
            fg.addAttribute("id");
            fg.addAttribute("stock");
            long t = System.currentTimeMillis();
            int loaded = bulkLoad("SELECT s FROM Stock s WHERE s.id IN :ids", ids, fg);
            LOGGER.log(Level.INFO, "[WorkingSetWarmup] Stock bulk load: {0}ms, loaded={1}",
                    new Object[]{System.currentTimeMillis() - t, loaded});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[WorkingSetWarmup] Stock warm failed: " + e.getMessage(), e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Long> loadActiveItemBatchIds() {
        try {
            long t = System.currentTimeMillis();
            List<Long> ids = em.createQuery(
                    "SELECT DISTINCT s.itemBatch.id FROM Stock s WHERE s.stock > 0",
                    Long.class).getResultList();
            LOGGER.log(Level.INFO, "[WorkingSetWarmup] ItemBatch id query: {0}ms, size={1}",
                    new Object[]{System.currentTimeMillis() - t, ids.size()});
            return ids;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[WorkingSetWarmup] ItemBatch id query failed: " + e.getMessage(), e);
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void warmItemBatches(List<Long> ids) {
        try {
            FetchGroup fg = new FetchGroup("itembatch-min");
            fg.addAttribute("id");
            long t = System.currentTimeMillis();
            int loaded = bulkLoad("SELECT b FROM ItemBatch b WHERE b.id IN :ids", ids, fg);
            LOGGER.log(Level.INFO, "[WorkingSetWarmup] ItemBatch bulk load: {0}ms, loaded={1}",
                    new Object[]{System.currentTimeMillis() - t, loaded});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[WorkingSetWarmup] ItemBatch warm failed: " + e.getMessage(), e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Long> loadActiveItemIds() {
        try {
            long t = System.currentTimeMillis();
            List<Long> ids = em.createQuery(
                    "SELECT DISTINCT s.itemBatch.item.id FROM Stock s WHERE s.stock > 0",
                    Long.class).getResultList();
            LOGGER.log(Level.INFO, "[WorkingSetWarmup] Item id query: {0}ms, size={1}",
                    new Object[]{System.currentTimeMillis() - t, ids.size()});
            return ids;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[WorkingSetWarmup] Item id query failed: " + e.getMessage(), e);
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void warmItems(List<Long> ids) {
        try {
            FetchGroup fg = new FetchGroup("item-min");
            fg.addAttribute("id");
            fg.addAttribute("name");
            long t = System.currentTimeMillis();
            int loaded = bulkLoad("SELECT i FROM Item i WHERE i.id IN :ids", ids, fg);
            LOGGER.log(Level.INFO, "[WorkingSetWarmup] Item bulk load: {0}ms, loaded={1}",
                    new Object[]{System.currentTimeMillis() - t, loaded});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[WorkingSetWarmup] Item warm failed: " + e.getMessage(), e);
        }
    }

    private int bulkLoad(String jpql, List<Long> ids, FetchGroup fg) {
        int loaded = 0;
        for (int i = 0; i < ids.size(); i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, ids.size());
            List<Long> chunk = ids.subList(i, end);
            Query q = em.createQuery(jpql)
                    .setParameter("ids", chunk)
                    .setHint(QueryHints.FETCH_GROUP, fg);
            List<?> r = q.getResultList();
            loaded += r.size();
        }
        return loaded;
    }
}
