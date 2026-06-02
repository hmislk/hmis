/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Atomic 6-step archival batch for ItemBatch and its linked Stock rows.
 *
 * Each invocation runs in its own REQUIRES_NEW transaction. The order of
 * operations preserves FK integrity:
 *
 *   1. INSERT INTO ITEMBATCHARCHIVE  — copy ItemBatch rows to archive
 *   2. INSERT INTO STOCKARCHIVE      — copy linked Stock rows to archive
 *   3. UPDATE PHARMACEUTICALBILLITEM SET ARCHIVEDITEMBATCH_ID = ITEMBATCH_ID
 *      for the archived batch IDs  — record the archive pointer before nullifying
 *   4. UPDATE PHARMACEUTICALBILLITEM SET ITEMBATCH_ID = NULL
 *      for the archived batch IDs  — release the live FK so DELETE can proceed
 *   5. DELETE FROM STOCK             — remove live Stock rows (no FK to ITEMBATCH)
 *   6. DELETE FROM ITEMBATCH         — safe: no more FKs pointing at these rows
 *
 * Table names come from ItemBatchArchivalService (compile-time constants),
 * never from user input, so direct string concatenation is safe from injection.
 *
 * Issue #20724.
 */
@Stateless
public class ItemBatchArchivalBatchTx {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    /**
     * Move the ItemBatch rows identified by {@code ids} (and all their linked
     * Stock rows) into their respective archive tables, updating PBI pointers
     * atomically.
     *
     * @param itemBatchTable      live ItemBatch table name
     * @param itemBatchArchive    archive table name for ItemBatch
     * @param stockTable          live Stock table name
     * @param stockArchive        archive table name for Stock
     * @param pbiTable            PharmaceuticalBillItem table name
     * @param itemBatchCols       comma-separated column list for ItemBatch (excl. ARCHIVEDAT)
     * @param stockCols           comma-separated column list for Stock (excl. ARCHIVEDAT)
     * @param ids                 ItemBatch primary-key IDs to archive
     * @return number of ItemBatch rows deleted from the live table
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int archiveBatch(String itemBatchTable, String itemBatchArchive,
                            String stockTable, String stockArchive,
                            String pbiTable,
                            String itemBatchCols, String stockCols,
                            List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        // Step 1 — copy ItemBatch rows to archive, skipping any that gained stock
        // between fetchNextBatchIds() and this transaction (race condition guard).
        String placeholders = placeholdersFor(ids);
        String archivedSubquery = "SELECT ID FROM " + itemBatchArchive + " WHERE ID IN (" + placeholders + ")";
        Query q1 = em.createNativeQuery(
                "INSERT INTO " + itemBatchArchive + " (" + itemBatchCols + ", ARCHIVEDAT) "
                + "SELECT " + itemBatchCols + ", NOW() FROM " + itemBatchTable
                + " WHERE ID IN (" + placeholders + ")"
                + " AND NOT EXISTS ("
                + "   SELECT 1 FROM " + stockTable
                + "   WHERE ITEMBATCH_ID = " + itemBatchTable + ".ID AND STOCK > 0)");
        bindIds(q1, ids);
        q1.executeUpdate();

        // Steps 2-6 operate only on IDs actually inserted in step 1, so any batch
        // that was skipped by the race-condition guard above is automatically excluded.

        // Step 2 — copy linked Stock rows to archive
        Query q2 = em.createNativeQuery(
                "INSERT INTO " + stockArchive + " (" + stockCols + ", ARCHIVEDAT) "
                + "SELECT " + stockCols + ", NOW() FROM " + stockTable
                + " WHERE ITEMBATCH_ID IN (" + archivedSubquery + ")");
        bindIds(q2, ids);
        q2.executeUpdate();

        // Step 3 — record archive pointer on PBI before releasing the live FK
        Query q3 = em.createNativeQuery(
                "UPDATE " + pbiTable
                + " SET ARCHIVEDITEMBATCH_ID = ITEMBATCH_ID"
                + " WHERE ITEMBATCH_ID IN (" + archivedSubquery + ")");
        bindIds(q3, ids);
        q3.executeUpdate();

        // Step 4 — nullify the live ItemBatch FK on PBI so the DELETE can proceed
        Query q4 = em.createNativeQuery(
                "UPDATE " + pbiTable
                + " SET ITEMBATCH_ID = NULL"
                + " WHERE ITEMBATCH_ID IN (" + archivedSubquery + ")");
        bindIds(q4, ids);
        q4.executeUpdate();

        // Step 5 — delete live Stock rows
        Query q5 = em.createNativeQuery(
                "DELETE FROM " + stockTable
                + " WHERE ITEMBATCH_ID IN (" + archivedSubquery + ")");
        bindIds(q5, ids);
        q5.executeUpdate();

        // Step 6 — delete live ItemBatch rows (PBI FK nullified in step 4)
        Query q6 = em.createNativeQuery(
                "DELETE FROM " + itemBatchTable
                + " WHERE ID IN (" + archivedSubquery + ")");
        bindIds(q6, ids);
        return q6.executeUpdate();
    }

    private static String placeholdersFor(List<Long> ids) {
        return IntStream.range(0, ids.size())
                .mapToObj(i -> "?")
                .collect(Collectors.joining(","));
    }

    private static void bindIds(Query query, List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            query.setParameter(i + 1, ids.get(i));
        }
    }
}
