/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import java.util.ArrayList;
import java.util.Date;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 *
 * @author buddhika
 */
@Stateless
public class BillItemFacade extends AbstractFacade<BillItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public BillItemFacade() {
        super(BillItem.class);
    }

    /**
     * Atomically reserves a block of IDs from the EclipseLink SEQUENCE table
     * and returns the first ID in the block.
     *
     * <p><b>MySQL-specific:</b> Uses {@code LAST_INSERT_ID(expr)} as an atomic
     * read-modify-write on the {@code sequence} table row where
     * {@code SEQ_NAME = 'SEQ_GEN'}. This row is EclipseLink's internal sequence
     * counter; it must exist before this method is called. If the row is absent
     * the UPDATE affects 0 rows, {@code LAST_INSERT_ID()} returns 0, and the
     * method throws {@link IllegalStateException} — check that EclipseLink has
     * been initialised (i.e. at least one JPA entity has been persisted) before
     * using bulk inserts.</p>
     *
     * <p><b>Migration note:</b> This syntax is MySQL-only and will not work on
     * PostgreSQL or other databases without rewriting the native queries.</p>
     *
     * <p>Runs in {@code REQUIRES_NEW} so the UPDATE commits (and the row lock
     * releases) immediately, before the caller's bulk-INSERT transaction begins.
     * This prevents lock-wait timeouts when EclipseLink's own sequence
     * allocation later tries to update the same SEQUENCE row.</p>
     *
     * <p>EclipseLink allocation size is 50, so {@code toReserve} is always a
     * multiple of 50.</p>
     *
     * @param count number of IDs needed
     * @return first ID in the allocated block
     * @throws IllegalStateException if the SEQ_GEN row is missing or
     *         LAST_INSERT_ID() returns null — ensure EclipseLink has persisted
     *         at least one entity before calling this method
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public long allocateSequenceBlock(int count) {
        int allocationSize = 50;
        int blocks = (count + allocationSize - 1) / allocationSize;
        int toReserve = blocks * allocationSize;

        // After the GenerationType.IDENTITY migration (v2.2.0), EclipseLink no longer
        // updates the SEQUENCE table — MySQL per-table AUTO_INCREMENT handles ID
        // assignment instead.  This means SEQUENCE.SEQ_COUNT can lag behind the real
        // AUTO_INCREMENT values, causing allocateSequenceBlock() to hand out IDs that
        // already exist in the target tables (PK conflicts on bulk INSERT).
        //
        // Guard: before advancing the counter, ensure SEQ_COUNT is at or above the
        // highest AUTO_INCREMENT across all entity tables that have a BIGINT ID column.
        // GREATEST() makes the UPDATE a no-op when the counter is already safe.
        Query sync = em.createNativeQuery(
                "UPDATE SEQUENCE s "
                + "JOIN (SELECT COALESCE(MAX(t.AUTO_INCREMENT), 0) AS max_ai "
                + "      FROM information_schema.TABLES t "
                + "      JOIN information_schema.COLUMNS c "
                + "        ON t.TABLE_NAME = c.TABLE_NAME AND t.TABLE_SCHEMA = c.TABLE_SCHEMA "
                + "      WHERE t.TABLE_SCHEMA = DATABASE() "
                + "        AND c.COLUMN_NAME = 'ID' "
                + "        AND c.DATA_TYPE = 'bigint' "
                + "        AND c.EXTRA LIKE '%auto_increment%') ai "
                + "SET s.SEQ_COUNT = GREATEST(s.SEQ_COUNT, ai.max_ai) "
                + "WHERE s.SEQ_NAME = 'SEQ_GEN'");
        sync.executeUpdate();

        Query update = em.createNativeQuery(
                "UPDATE sequence SET SEQ_COUNT = LAST_INSERT_ID(SEQ_COUNT + ?) WHERE SEQ_NAME = 'SEQ_GEN'");
        update.setParameter(1, toReserve);
        update.executeUpdate();

        Query select = em.createNativeQuery("SELECT LAST_INSERT_ID()");
        select.setMaxResults(1);
        List<?> rows = select.getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            throw new IllegalStateException("Cannot read LAST_INSERT_ID() after SEQUENCE update");
        }
        long newMax = ((Number) rows.get(0)).longValue();
        return newMax - toReserve + 1;
    }

    /**
     * Loads a Bill with all its BillItems and PharmaceuticalBillItems in a fresh
     * REQUIRES_NEW transaction. This is necessary after bulk native SQL INSERTs
     * because those rows are invisible to the caller's JPA persistence context
     * (EclipseLink L1 cache has no knowledge of them). A new transaction gets a
     * brand-new persistence context that reads directly from the committed DB rows.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Bill loadBillWithItemsFresh(Long billId) {
        String jpql = "select b from Bill b "
                + "left join fetch b.department bd "
                + "left join fetch bd.institution "
                + "left join fetch b.billItems bi "
                + "left join fetch bi.item "
                + "left join fetch bi.pharmaceuticalBillItem pbi "
                + "left join fetch pbi.itemBatch "
                + "left join fetch pbi.stock "
                + "left join fetch bi.referanceBillItem rbi "
                + "left join fetch rbi.pharmaceuticalBillItem rpbi "
                + "left join fetch rpbi.stock "
                + "where b.id = :bid";
        List<Bill> result = em.createQuery(jpql, Bill.class)
                .setParameter("bid", billId)
                .getResultList();
        System.out.println("DEBUG: loadBillWithItemsFresh REQUIRES_NEW result size=" + result.size()
                + (result.isEmpty() ? "" : " billItems=" + result.get(0).getBillItems().size()));
        return result.isEmpty() ? em.find(Bill.class, billId) : result.get(0);
    }

    /**
     * Inserts BillItem rows in bulk using native SQL, in a REQUIRES_NEW transaction
     * so the rows commit immediately and are visible to subsequent reads.
     *
     * @param billId     the parent Bill ID
     * @param ids        pre-allocated IDs for each row (same order as data)
     * @param itemIds    Item IDs
     * @param physQtys   physical quantities
     * @param createdAt  creation timestamp
     * @param createrId  creator WebUser ID
     * @param refBillItemIds  referanceBillItem IDs
     * @param adjustedValues  variance values
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void insertBillItemsBulk(Long billId, List<Long> ids, List<Long> itemIds,
            List<Double> physQtys, Date createdAt, Long createrId,
            List<Long> refBillItemIds, List<Double> adjustedValues) throws Exception {
        int batchSize = 50;
        int total = ids.size();
        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            StringBuilder sql = new StringBuilder(
                "INSERT INTO billitem (ID, bill_id, item_id, qty, createdAt, creater_id, referanceBillItem_id, adjustedValue, retired, searialno) VALUES ");
            List<Object> params = new ArrayList<>();
            for (int j = i; j < end; j++) {
                if (j > i) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?, ?, ?, ?, 0, ?)");
                params.add(ids.get(j));
                params.add(billId);
                params.add(itemIds.get(j));
                params.add(physQtys.get(j));
                params.add(createdAt);
                params.add(createrId);
                params.add(refBillItemIds.get(j));
                params.add(adjustedValues.get(j));
                params.add(j + 1);
            }
            Query q = em.createNativeQuery(sql.toString());
            for (int p = 0; p < params.size(); p++) q.setParameter(p + 1, params.get(p));
            q.executeUpdate();
            System.out.println("PERF: BillItem batch " + (i / batchSize + 1) + " done (" + (end - i) + " rows)");
        }
        System.out.println("PERF: All BillItems created, total: " + total);
    }

    /**
     * Inserts PharmaceuticalBillItem rows in bulk using native SQL, in a REQUIRES_NEW
     * transaction so the rows commit immediately and are visible to subsequent reads.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void insertPharmaBillItemsBulk(List<Long> pbiIds, List<Long> billItemIds,
            List<Long> itemBatchIds, List<Long> stockIds, List<Double> physQtys) throws Exception {
        int batchSize = 50;
        int total = pbiIds.size();
        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            StringBuilder sql = new StringBuilder(
                "INSERT INTO pharmaceuticalbillitem (ID, billItem_id, itemBatch_id, stock_id, qty, freeQty) VALUES ");
            List<Object> params = new ArrayList<>();
            for (int j = i; j < end; j++) {
                if (j > i) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?, 0.0)");
                params.add(pbiIds.get(j));
                params.add(billItemIds.get(j));
                params.add(itemBatchIds.get(j));
                params.add(stockIds.get(j));
                params.add(physQtys.get(j));
            }
            Query q = em.createNativeQuery(sql.toString());
            for (int p = 0; p < params.size(); p++) q.setParameter(p + 1, params.get(p));
            q.executeUpdate();
            System.out.println("PERF: PharmaBillItem batch " + (i / batchSize + 1) + " done (" + (end - i) + " rows)");
        }
        System.out.println("PERF: All PharmaBillItems created, total: " + total);
    }

}
