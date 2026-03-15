package com.divudi.service.pharmacy;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
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
 *  3. Bulk-insert BillItems using native SQL in batches (no JPA lifecycle overhead).
 *  4. Bulk-insert PharmaceuticalBillItems using native SQL in batches.
 *
 * Only used for PharmacySnapshotBill creation. All other Bill types use JPA cascade.
 */
@Stateless
public class StockTakePersistService {

    private static final Logger LOGGER = Logger.getLogger(StockTakePersistService.class.getName());

    /** Rows per native INSERT ... VALUES (...),(...),... statement */
    private static final int BATCH_SIZE = 100;

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
        // Step 2: bulk-insert BillItems with native multi-row INSERT
        // -----------------------------------------------------------------------
        long t1 = System.currentTimeMillis();
        long[] billItemIds = insertBillItemsBulk(items, billId);
        System.out.println("[StockTakePersist] Step2 BillItems inserted. count=" + items.size()
                + "  ms=" + (System.currentTimeMillis() - t1));

        // Write IDs back onto the in-memory objects so callers can reference them
        for (int i = 0; i < items.size(); i++) {
            if (billItemIds[i] > 0) {
                items.get(i).setId(billItemIds[i]);
            }
        }

        // -----------------------------------------------------------------------
        // Step 3: bulk-insert PharmaceuticalBillItems
        // -----------------------------------------------------------------------
        long t2 = System.currentTimeMillis();
        insertPharmBillItemsBulk(items, billItemIds);
        System.out.println("[StockTakePersist] Step3 PharmaceuticalBillItems inserted. count=" + items.size()
                + "  ms=" + (System.currentTimeMillis() - t2));

        System.out.println("[StockTakePersist] TOTAL persist time ms=" + (System.currentTimeMillis() - t0));
    }

    // -----------------------------------------------------------------------
    // private helpers
    // -----------------------------------------------------------------------

    /**
     * Insert BillItems in multi-row native INSERT batches.
     * Returns array of generated IDs (indexed same as items list).
     */
    private long[] insertBillItemsBulk(List<BillItem> items, long billId) {
        long[] ids = new long[items.size()];
        int total = items.size();

        for (int start = 0; start < total; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, total);
            List<BillItem> batch = items.subList(start, end);

            StringBuilder sql = new StringBuilder(
                    "INSERT INTO billitem (bill_ID, item_ID, qty, descreption, netValue, "
                    + "createdAt, creater_ID, retired, refunded, billItemRefunded, consideredForCosting) VALUES ");

            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("(?,?,?,?,?,?,?,0,0,0,1)");
            }

            Query q = em.createNativeQuery(sql.toString());
            int p = 1;
            for (BillItem bi : batch) {
                q.setParameter(p++, billId);
                q.setParameter(p++, bi.getItem() != null ? bi.getItem().getId() : null);
                q.setParameter(p++, bi.getQty() != null ? bi.getQty() : 0.0);
                q.setParameter(p++, bi.getDescreption());
                q.setParameter(p++, bi.getNetValue());
                q.setParameter(p++, bi.getCreatedAt() != null
                        ? new java.sql.Timestamp(bi.getCreatedAt().getTime()) : null);
                q.setParameter(p++, bi.getCreater() != null ? bi.getCreater().getId() : null);
            }
            q.executeUpdate();
        }

        // Retrieve the generated IDs in insertion order
        // MySQL guarantees LAST_INSERT_ID() points to the first ID of the last multi-row insert,
        // but the safest approach is to query by bill_ID ordered by id.
        @SuppressWarnings("unchecked")
        List<Object> generatedIds = em.createNativeQuery(
                "SELECT id FROM billitem WHERE bill_ID = ? ORDER BY id ASC")
                .setParameter(1, billId)
                .getResultList();

        for (int i = 0; i < generatedIds.size() && i < ids.length; i++) {
            Object raw = generatedIds.get(i);
            ids[i] = raw instanceof Number ? ((Number) raw).longValue() : Long.parseLong(raw.toString());
        }

        return ids;
    }

    /**
     * Insert PharmaceuticalBillItems in multi-row native INSERT batches.
     */
    private void insertPharmBillItemsBulk(List<BillItem> items, long[] billItemIds) {
        int total = items.size();

        for (int start = 0; start < total; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, total);

            StringBuilder sql = new StringBuilder(
                    "INSERT INTO pharmaceuticalbillitem (billItem_ID, itemBatch_ID, stock_ID, qty, stringValue, costRate) VALUES ");

            // Build only rows that have a valid pbi and a known billItemId
            List<BillItem> batchItems = items.subList(start, end);
            int rowCount = 0;
            for (int i = 0; i < batchItems.size(); i++) {
                int globalIdx = start + i;
                if (billItemIds[globalIdx] == 0) continue;
                PharmaceuticalBillItem pbi = batchItems.get(i).getPharmaceuticalBillItem();
                if (pbi == null) continue;
                if (rowCount > 0) sql.append(",");
                sql.append("(?,?,?,?,?,?)");
                rowCount++;
            }

            if (rowCount == 0) continue;

            Query q = em.createNativeQuery(sql.toString());
            int p = 1;
            for (int i = 0; i < batchItems.size(); i++) {
                int globalIdx = start + i;
                if (billItemIds[globalIdx] == 0) continue;
                PharmaceuticalBillItem pbi = batchItems.get(i).getPharmaceuticalBillItem();
                if (pbi == null) continue;

                q.setParameter(p++, billItemIds[globalIdx]);
                q.setParameter(p++, pbi.getItemBatch() != null ? pbi.getItemBatch().getId() : null);
                q.setParameter(p++, pbi.getStock() != null ? pbi.getStock().getId() : null);
                q.setParameter(p++, pbi.getQty());
                q.setParameter(p++, pbi.getStringValue());
                q.setParameter(p++, pbi.getCostRate());
            }
            q.executeUpdate();
        }
    }
}
