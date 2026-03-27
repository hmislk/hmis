package com.divudi.service.pharmacy;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
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
 *  3. Allocate IDs for BillItems via SequenceAllocatorService (EclipseLink API).
 *  4. Bulk-insert BillItems using native SQL with explicit IDs.
 *  5. Allocate IDs for PharmaceuticalBillItems via SequenceAllocatorService.
 *  6. Bulk-insert PharmaceuticalBillItems using native SQL with explicit IDs.
 *
 * Sequence allocation uses EclipseLink's internal non-JTA sequence mechanism, which
 * keeps its in-memory cache in sync and avoids lock-wait conflicts.
 *
 * Only used for PharmacySnapshotBill creation. All other Bill types use JPA cascade.
 */
@Stateless
public class StockTakePersistService {

    private static final Logger LOGGER = Logger.getLogger(StockTakePersistService.class.getName());

    /** Rows per native INSERT ... VALUES (...),(...),... statement */
    private static final int BATCH_SIZE = 100;

    /** Cached table names — resolved once via INFORMATION_SCHEMA to handle case-sensitive
     *  MySQL installations (lower_case_table_names=0) where table names may be upper or lower case. */
    private volatile String resolvedBillItemTable = null;
    private volatile String resolvedPharmBillItemTable = null;

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private SequenceAllocatorService sequenceAllocator;

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

        // Detach snapshotBill immediately so JPA cascade does NOT fire when we
        // restore the billItems list below. Native SQL handles all child inserts.
        em.clear();

        // Restore the list onto the now-detached bill so callers can still reference it.
        snapshotBill.setBillItems(items);

        // -----------------------------------------------------------------------
        // Step 2: Allocate IDs for BillItems (REQUIRES_NEW — commits immediately),
        //         then bulk-insert
        // -----------------------------------------------------------------------
        long t1 = System.currentTimeMillis();
        long[] billItemIds = sequenceAllocator.allocate(items.size(), BillItem.class);
        insertBillItemsBulk(items, billId, billItemIds);
        System.out.println("[StockTakePersist] Step2 BillItems inserted. count=" + items.size()
                + "  ms=" + (System.currentTimeMillis() - t1));

        // Write IDs back onto the in-memory objects so callers can reference them
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setId(billItemIds[i]);
        }

        // -----------------------------------------------------------------------
        // Step 3: Allocate IDs for PharmaceuticalBillItems (REQUIRES_NEW — commits
        //         immediately), then bulk-insert
        // -----------------------------------------------------------------------
        long t2 = System.currentTimeMillis();
        long[] pbIds = sequenceAllocator.allocate(items.size(), PharmaceuticalBillItem.class);
        insertPharmBillItemsBulk(items, billItemIds, pbIds);
        System.out.println("[StockTakePersist] Step3 PharmaceuticalBillItems inserted. count=" + items.size()
                + "  ms=" + (System.currentTimeMillis() - t2));

        System.out.println("[StockTakePersist] TOTAL persist time ms=" + (System.currentTimeMillis() - t0));
    }

    // -----------------------------------------------------------------------
    // private helpers
    // -----------------------------------------------------------------------

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
                .setParameter(1, upperName)
                .getSingleResult();
        String resolved = name.toString();
        System.out.println("[StockTakePersist] Resolved table name: " + upperName + " -> " + resolved);
        return resolved;
    }

    private String billItemTable() {
        if (resolvedBillItemTable == null) resolvedBillItemTable = resolveTable("BILLITEM");
        return resolvedBillItemTable;
    }

    private String pharmBillItemTable() {
        if (resolvedPharmBillItemTable == null) resolvedPharmBillItemTable = resolveTable("PHARMACEUTICALBILLITEM");
        return resolvedPharmBillItemTable;
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
                    "INSERT INTO " + billItemTable() + " (ID, bill_ID, item_ID, qty, descreption, netValue, "
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
                    "INSERT INTO " + pharmBillItemTable()
                    + " (ID, billItem_ID, itemBatch_ID, stock_ID, qty, stringValue, costRate, purchaseRate, retailRate, doe, description) VALUES ");

            List<BillItem> batchItems = items.subList(start, end);
            int rowCount = 0;
            for (int i = 0; i < batchItems.size(); i++) {
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
