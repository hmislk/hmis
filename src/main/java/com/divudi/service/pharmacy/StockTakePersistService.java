package com.divudi.service.pharmacy;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Persists snapshot bill and its items using native SQL batch inserts
 * to avoid the 2000+ individual JPA INSERT round-trips caused by cascade.
 *
 * Strategy:
 *  1. Persist the Bill header via JPA (gets DTYPE, ID, all Bill fields).
 *  2. Flush to obtain the Bill ID.
 *  3. Allocate IDs for BillItems from the EclipseLink SEQ_GEN sequence table.
 *  4. Bulk-insert BillItems using native SQL with explicit IDs.
 *  5. Allocate IDs for PharmaceuticalBillItems from SEQ_GEN.
 *  6. Bulk-insert PharmaceuticalBillItems using native SQL with explicit IDs.
 *
 * EclipseLink uses a single SEQ_GEN row in the `sequence` table for all entities.
 * IDs are allocated in blocks to minimise lock contention on the sequence table.
 *
 * Only used for PharmacySnapshotBill creation. All other Bill types use JPA cascade.
 */
@Stateless
public class StockTakePersistService {

    private static final Logger LOGGER = Logger.getLogger(StockTakePersistService.class.getName());

    /** Rows per native INSERT ... VALUES (...),(...),... statement */
    private static final int BATCH_SIZE = 100;

    /**
     * EclipseLink allocation size — must match the value used by the JPA provider.
     * Default for GenerationType.AUTO with EclipseLink sequence table is 50.
     */
    private static final int SEQ_ALLOC_SIZE = 50;

    private static final String SEQ_NAME = "SEQ_GEN";

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    /**
     * Persist the snapshot bill header via JPA and all child rows via native SQL multi-row INSERT.
     *
     * @param snapshotBill In-memory bill built by StockCountGenerationService
     * @throws Exception on any persistence failure
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persistSnapshotBill(Bill snapshotBill) throws Exception {

        long t0 = System.currentTimeMillis();

        List<BillItem> items = snapshotBill.getBillItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("snapshotBill has no items");
        }

        // -----------------------------------------------------------------------
        // Step 1: persist the Bill header via JPA.
        //         Clear the collection first so JPA does NOT cascade to BillItems.
        // -----------------------------------------------------------------------
        snapshotBill.setBillItems(null);
        em.persist(snapshotBill);
        em.flush();  // forces the INSERT and populates snapshotBill.id

        long billId = snapshotBill.getId();
        System.out.println("[StockTakePersist] Step1 Bill header persisted. ID=" + billId
                + "  ms=" + (System.currentTimeMillis() - t0));

        // Restore the list for callers
        snapshotBill.setBillItems(items);

        // -----------------------------------------------------------------------
        // Step 2: Allocate IDs for BillItems from SEQ_GEN, then bulk-insert
        // -----------------------------------------------------------------------
        long t1 = System.currentTimeMillis();
        long[] billItemIds = allocateIds(items.size());
        insertBillItemsBulk(items, billId, billItemIds);
        System.out.println("[StockTakePersist] Step2 BillItems inserted. count=" + items.size()
                + "  ms=" + (System.currentTimeMillis() - t1));

        // Write IDs back onto the in-memory objects so callers can reference them
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setId(billItemIds[i]);
        }

        // -----------------------------------------------------------------------
        // Step 3: Allocate IDs for PharmaceuticalBillItems, then bulk-insert
        // -----------------------------------------------------------------------
        long t2 = System.currentTimeMillis();
        long[] pbIds = allocateIds(items.size());
        insertPharmBillItemsBulk(items, billItemIds, pbIds);
        System.out.println("[StockTakePersist] Step3 PharmaceuticalBillItems inserted. count=" + items.size()
                + "  ms=" + (System.currentTimeMillis() - t2));

        System.out.println("[StockTakePersist] TOTAL persist time ms=" + (System.currentTimeMillis() - t0));
    }

    // -----------------------------------------------------------------------
    // private helpers
    // -----------------------------------------------------------------------

    /**
     * Allocate `count` IDs from the EclipseLink SEQ_GEN sequence table.
     * Issues one or more locked UPDATE+SELECT pairs, each reserving SEQ_ALLOC_SIZE IDs,
     * until enough IDs are accumulated.
     *
     * @param count number of IDs needed
     * @return array of `count` unique IDs safe to use in native SQL INSERTs
     */
    private long[] allocateIds(int count) {
        List<Long> ids = new ArrayList<>(count);

        while (ids.size() < count) {
            // Reserve the next block atomically
            em.createNativeQuery(
                    "UPDATE sequence SET SEQ_COUNT = SEQ_COUNT + ? WHERE SEQ_NAME = ?")
                    .setParameter(1, SEQ_ALLOC_SIZE)
                    .setParameter(2, SEQ_NAME)
                    .executeUpdate();

            // Read back the new high-water mark
            Object result = em.createNativeQuery(
                    "SELECT SEQ_COUNT FROM sequence WHERE SEQ_NAME = ?")
                    .setParameter(1, SEQ_NAME)
                    .getSingleResult();

            long newCount = result instanceof Number ? ((Number) result).longValue()
                    : Long.parseLong(result.toString());

            // IDs in this block: [newCount - SEQ_ALLOC_SIZE, newCount - 1]
            long blockStart = newCount - SEQ_ALLOC_SIZE;
            for (int i = 0; i < SEQ_ALLOC_SIZE && ids.size() < count; i++) {
                ids.add(blockStart + i);
            }
        }

        long[] result = new long[count];
        for (int i = 0; i < count; i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    /**
     * Insert BillItems in multi-row native INSERT batches with explicit IDs.
     */
    private void insertBillItemsBulk(List<BillItem> items, long billId, long[] ids) {
        int total = items.size();

        for (int start = 0; start < total; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, total);
            List<BillItem> batch = items.subList(start, end);

            StringBuilder sql = new StringBuilder(
                    "INSERT INTO billitem (ID, bill_ID, item_ID, qty, descreption, netValue, "
                    + "catId, createdAt, creater_ID, retired, refunded, billItemRefunded, consideredForCosting) VALUES ");

            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("(?,?,?,?,?,?,?,?,?,0,0,0,1)");
            }

            Query q = em.createNativeQuery(sql.toString());
            int p = 1;
            for (int i = 0; i < batch.size(); i++) {
                BillItem bi = batch.get(i);
                q.setParameter(p++, ids[start + i]);
                q.setParameter(p++, billId);
                q.setParameter(p++, bi.getItem() != null ? bi.getItem().getId() : null);
                q.setParameter(p++, bi.getQty() != null ? bi.getQty() : 0.0);
                q.setParameter(p++, bi.getDescreption());
                q.setParameter(p++, bi.getNetValue());
                q.setParameter(p++, bi.getCatId());
                q.setParameter(p++, bi.getCreatedAt() != null
                        ? new java.sql.Timestamp(bi.getCreatedAt().getTime()) : null);
                q.setParameter(p++, bi.getCreater() != null ? bi.getCreater().getId() : null);
            }
            q.executeUpdate();
        }
    }

    /**
     * Insert PharmaceuticalBillItems in multi-row native INSERT batches with explicit IDs.
     */
    private void insertPharmBillItemsBulk(List<BillItem> items, long[] billItemIds, long[] pbIds) {
        int total = items.size();

        for (int start = 0; start < total; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, total);

            StringBuilder sql = new StringBuilder(
                    "INSERT INTO pharmaceuticalbillitem "
                    + "(ID, billItem_ID, itemBatch_ID, stock_ID, qty, stringValue, costRate, purchaseRate, retailRate, doe, description) VALUES ");

            List<BillItem> batchItems = items.subList(start, end);
            int rowCount = 0;
            for (int i = 0; i < batchItems.size(); i++) {
                int globalIdx = start + i;
                PharmaceuticalBillItem pbi = batchItems.get(i).getPharmaceuticalBillItem();
                if (pbi == null) continue;
                if (rowCount > 0) sql.append(",");
                sql.append("(?,?,?,?,?,?,?,?,?,?,?)");
                rowCount++;
            }

            if (rowCount == 0) continue;

            Query q = em.createNativeQuery(sql.toString());
            int p = 1;
            for (int i = 0; i < batchItems.size(); i++) {
                int globalIdx = start + i;
                PharmaceuticalBillItem pbi = batchItems.get(i).getPharmaceuticalBillItem();
                if (pbi == null) continue;

                q.setParameter(p++, pbIds[globalIdx]);
                q.setParameter(p++, billItemIds[globalIdx]);
                q.setParameter(p++, pbi.getItemBatch() != null ? pbi.getItemBatch().getId() : null);
                q.setParameter(p++, pbi.getStock() != null ? pbi.getStock().getId() : null);
                q.setParameter(p++, pbi.getQty());
                q.setParameter(p++, pbi.getStringValue());
                q.setParameter(p++, pbi.getCostRate());
                q.setParameter(p++, pbi.getPurchaseRate());
                q.setParameter(p++, pbi.getRetailRate());
                q.setParameter(p++, pbi.getDoe() != null
                        ? new java.sql.Timestamp(pbi.getDoe().getTime()) : null);
                q.setParameter(p++, pbi.getDescription());
            }
            q.executeUpdate();
        }
    }
}
