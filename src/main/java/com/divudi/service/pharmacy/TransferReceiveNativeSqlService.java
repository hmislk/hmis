/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.data.dto.TransferReceiveItemPrintDto;
import com.divudi.core.data.dto.TransferReceiveItemRowDto;
import com.divudi.core.data.dto.TransferReceivePrintDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.StockHistory;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists pharmacy transfer receive bills using native SQL for hot-path
 * operations (stock deduction from staff stock, addition to dept stock,
 * aggregates, StockHistory INSERT).
 *
 * Avoids the dominant N+1 cost in the original TransferReceiveController:
 *  - generateBillComponent() calls calculateRemainingQtyWithFreshData() per item
 *    which in turn loads all receive bills and their BillItem collections (N+1)
 *  - fillData() iterates entity graphs with lazy-loaded associations
 *  - PharmacyBean.addToStockHistory() runs 9 separate JPQL aggregate queries per item
 *
 * Two-query design:
 *  1. Load all issued items with their data in a single join query
 *  2. Load total received qty grouped by issuedBillItemId in a single aggregate query
 *  Merge in Java to determine remaining qty per item.
 */
@Stateless
public class TransferReceiveNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(TransferReceiveNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    // Cached resolved table names (cross-deployment case safety via INFORMATION_SCHEMA)
    private volatile String tStockHistory = null;
    private volatile String tStock = null;
    private volatile String tItemBatch = null;
    private volatile String tDepartment = null;
    private volatile String tBill = null;
    private volatile String tBillItem = null;
    private volatile String tPharmBillItem = null;
    private volatile String tItem = null;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Loads issued items for the native receive UI.
     * Returns only items with remaining qty greater than zero (tolerance 0.001).
     *
     * Uses a two-query approach to avoid N+1:
     *   Query 1 - Load all issued items with their batch/item data via a single JOIN
     *   Query 2 - Load total received units grouped by issuedBillItemId (all non-cancelled receives)
     * Then merges in Java to compute remaining quantities.
     *
     * @param issuedBillId   the ID of the PharmacyTransferIssue bill
     * @param byPurchaseRate if true, use purchase rate as transfer rate
     * @param byCostRate     if true, use cost rate as transfer rate (ignored if byPurchaseRate)
     * @return list of item rows with remaining qty greater than zero, in bill item order
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TransferReceiveItemRowDto> loadIssuedItemsForReceive(
            long issuedBillId, boolean byPurchaseRate, boolean byCostRate) {

        // Query 1: Load all issued items with their batch and item data
        String sql1 = "SELECT"
                + " bi.ID, bi.qty, bi.netValue, bi.rate, bi.netRate, bi.searialNo,"
                + " pbi.stock_ID, pbi.itemBatch_ID, pbi.qty AS pbiQty,"
                + " ib.batchNo, ib.dateOfExpire, ib.purcahseRate, ib.retailsaleRate,"
                + " ib.wholesaleRate, COALESCE(ib.costRate, 0) AS costRate,"
                + " i.ID AS itemId, i.name AS itemName, COALESCE(i.code, '') AS itemCode,"
                + " i.DTYPE AS itemDtype, COALESCE(i.dblValue, 1) AS dblValue"
                + " FROM " + billItemTable() + " bi"
                + " JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID"
                + " JOIN " + itemBatchTable() + " ib ON ib.ID = pbi.itemBatch_ID"
                + " JOIN " + itemTable() + " i ON i.ID = ib.item_ID"
                + " WHERE bi.bill_ID = ?"
                + " AND (bi.retired IS NULL OR bi.retired = 0)"
                + " ORDER BY bi.ID";

        // Query 2: Total received units per issued bill item ID (all non-cancelled receive bills)
        String sql2 = "SELECT rbi.referanceBillItem_ID, SUM(ABS(rpbi.qty))"
                + " FROM " + billTable() + " rb"
                + " JOIN " + billItemTable() + " rbi ON rbi.bill_ID = rb.ID"
                + " JOIN " + pharmBillItemTable() + " rpbi ON rpbi.billItem_ID = rbi.ID"
                + " WHERE rb.backwardReferenceBill_ID = ?"
                + " AND rb.billType = 'PharmacyTransferReceive'"
                + " AND rb.DTYPE != 'CancelledBill'"
                + " AND (rbi.retired IS NULL OR rbi.retired = 0)"
                + " GROUP BY rbi.referanceBillItem_ID";

        @SuppressWarnings("unchecked")
        List<Object[]> issuedRows = em.createNativeQuery(sql1)
                .setParameter(1, issuedBillId)
                .getResultList();

        @SuppressWarnings("unchecked")
        List<Object[]> receivedRows = em.createNativeQuery(sql2)
                .setParameter(1, issuedBillId)
                .getResultList();

        // Build received map: issuedBillItemId -> totalReceivedUnits
        Map<Long, Double> receivedMap = new HashMap<>();
        for (Object[] r : receivedRows) {
            if (r[0] != null && r[1] != null) {
                receivedMap.put(((Number) r[0]).longValue(), ((Number) r[1]).doubleValue());
            }
        }

        // Build DTO list
        List<TransferReceiveItemRowDto> result = new ArrayList<>();
        int serial = 1;

        for (Object[] row : issuedRows) {
            // row[0]=bi.ID, row[1]=bi.qty(packs), row[2]=bi.netValue, row[3]=bi.rate,
            // row[4]=bi.netRate, row[5]=bi.searialNo,
            // row[6]=pbi.stock_ID, row[7]=pbi.itemBatch_ID, row[8]=pbi.qty(units in stock),
            // row[9]=ib.batchNo, row[10]=ib.dateOfExpire, row[11]=ib.purcahseRate,
            // row[12]=ib.retailsaleRate, row[13]=ib.wholesaleRate, row[14]=ib.costRate,
            // row[15]=i.ID, row[16]=i.name, row[17]=i.code, row[18]=i.DTYPE, row[19]=i.dblValue

            long biId = ((Number) row[0]).longValue();

            // pbi.qty holds units as a negative value (issue direction); take abs
            double issuedUnits = Math.abs(toDouble(row[8]));
            double alreadyReceived = receivedMap.getOrDefault(biId, 0.0);
            double remaining = issuedUnits - alreadyReceived;

            if (remaining <= 0.001) {
                continue; // fully received — skip
            }

            String dtype = row[18] != null ? row[18].toString() : "";
            double dblValue = toDouble(row[19]);
            // Ampp and Vmpp items have units per pack stored in dblValue
            double unitsPerPack = (("Ampp".equals(dtype) || "Vmpp".equals(dtype)) && dblValue > 0)
                    ? dblValue : 1.0;

            double purchaseRate = toDouble(row[11]);
            double retailRate = toDouble(row[12]);
            double wholesaleRate = toDouble(row[13]);
            double costRate = toDouble(row[14]);

            BigDecimal grossRate;
            if (byPurchaseRate) {
                grossRate = BigDecimal.valueOf(purchaseRate);
            } else if (byCostRate) {
                grossRate = BigDecimal.valueOf(costRate);
            } else {
                grossRate = BigDecimal.valueOf(retailRate); // default: retail
            }
            // grossRate is per unit; scale to per pack for UI display and totals
            grossRate = grossRate.multiply(BigDecimal.valueOf(unitsPerPack));

            double remainingPacks = remaining / (unitsPerPack > 0 ? unitsPerPack : 1.0);
            // rate/netRate displayed per pack
            double ratePerPack = grossRate.doubleValue();

            TransferReceiveItemRowDto dto = new TransferReceiveItemRowDto();
            dto.setSerialNo(serial++);
            dto.setIssuedBillItemId(biId);
            dto.setStaffStockId(((Number) row[6]).longValue());
            dto.setItemBatchId(((Number) row[7]).longValue());
            dto.setItemId(((Number) row[15]).longValue());
            dto.setAmpItemId(((Number) row[15]).longValue()); // may be refined by controller for Ampp items
            dto.setItemName(row[16] != null ? row[16].toString() : "");
            dto.setItemCode(row[17] != null ? row[17].toString() : "");
            dto.setBatchNo(row[9] != null ? row[9].toString() : "");
            dto.setDateOfExpire(row[10] instanceof java.sql.Date ? new Date(((java.sql.Date) row[10]).getTime())
                    : (row[10] instanceof Date ? (Date) row[10] : null));
            dto.setUnitsPerPack(unitsPerPack);
            dto.setPurchaseRate(purchaseRate);
            dto.setRetailRate(retailRate);
            dto.setWholesaleRate(wholesaleRate);
            dto.setCostRate(costRate);
            dto.setBatchRetailRate(retailRate);
            dto.setBatchPurchaseRate(purchaseRate);
            dto.setBatchWholesaleRate(wholesaleRate);
            dto.setBatchCostRate(costRate > 0 ? costRate : null);
            dto.setGrossRate(grossRate);
            dto.setRate(ratePerPack);
            dto.setNetRate(ratePerPack);
            dto.setIssuedQty(issuedUnits);
            dto.setRemainingUnits(remaining);
            dto.setReceivingQty(BigDecimal.valueOf(remainingPacks));
            // value = abs(grossRate × remainingPacks) — positive display value
            dto.setValue(grossRate.multiply(BigDecimal.valueOf(remainingPacks)).abs().doubleValue());
            result.add(dto);
        }
        return result;
    }

    /**
     * Retires a pre-saved PHARMACY_RECEIVE_PRE bill after it has been superseded
     * by a full PHARMACY_RECEIVE settlement in settleApprove().
     * Sets retired=1 so the bill is hidden from list queries.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void retirePreBill(long preBillId, long retirerId) {
        em.createNativeQuery("UPDATE " + billTable()
                + " SET retired=1, retiredAt=NOW(), retirer_ID=? WHERE ID=? AND retired=0")
                .setParameter(1, retirerId)
                .setParameter(2, preBillId)
                .executeUpdate();
    }

    /**
     * Checks that each item's source stock (staffStockId) holds at least the
     * units being requested before settlement begins.
     *
     * Returns a list of human-readable error strings — one per stock row that
     * would go negative. An empty list means all stocks are sufficient.
     *
     * Call this before settle() so the user gets a clear message rather than
     * a mid-transaction RuntimeException.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> checkSourceStockSufficiency(List<TransferReceiveItemRowDto> items) {
        List<String> errors = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return errors;
        }

        // Aggregate required units by source stock ID
        // (same stock row could appear in multiple items if batches share a stock entry)
        Map<Long, Double> requiredByStock = new HashMap<>();
        Map<Long, String> labelByStock = new HashMap<>();
        for (TransferReceiveItemRowDto item : items) {
            if (item.getReceivingQty() == null
                    || item.getReceivingQty().compareTo(BigDecimal.ZERO) <= 0
                    || item.getStaffStockId() == null) {
                continue;
            }
            double units = item.getReceivingQty().doubleValue() * item.getUnitsPerPack();
            long sid = item.getStaffStockId();
            requiredByStock.merge(sid, units, Double::sum);
            labelByStock.putIfAbsent(sid, item.getItemName() + " (batch " + item.getBatchNo() + ")");
        }

        for (Map.Entry<Long, Double> entry : requiredByStock.entrySet()) {
            long stockId = entry.getKey();
            double required = entry.getValue();
            double available = fetchStockQty(stockId);
            if (available < required - 0.001) {
                errors.add(labelByStock.get(stockId)
                        + ": needs " + required
                        + " units, only " + available + " available in source stock");
            }
        }
        return errors;
    }

    /**
     * Settles the transfer receive using native SQL for stock operations and BillItem inserts.
     * Replicates the logic from TransferReceiveController.settle() and fillData().
     *
     * Steps:
     *  1. Persist bill header via JPA (correct DTYPE + IDENTITY PK)
     *  2. Native INSERT BillItem + PharmaceuticalBillItem per item
     *  3. Deduct from staff stock, find/create dept stock, add to dept stock
     *  4. Compute aggregates + INSERT StockHistory
     *  5. Evict L2 cache
     *  6. Finance details via JPA (BillItemFinanceDetails + BillFinanceDetails)
     *  7. Update bill totals
     *  8. Link backwardReferenceBill; try to insert forwardReferenceBills join row
     *  9. Update fullyIssued on the issued bill if all items received
     * 10. Build and return TransferReceivePrintDto (caller enriches dept/institution fields)
     *
     * @param bill              pre-populated BilledBill with type, dates, parties set by controller
     * @param items             row DTOs from the UI; items with zero or null receivingQty are skipped
     * @param receivingDeptId   ID of the receiving department (for stock creation)
     * @param receivingInstId   ID of the receiving institution (for stock history aggregates)
     * @return partial print DTO populated with financial totals and line items
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public TransferReceivePrintDto settle(
            Bill bill,
            List<TransferReceiveItemRowDto> items,
            long receivingDeptId,
            long receivingInstId) {

        long t0 = System.currentTimeMillis();

        // Filter to items with positive receivingQty before persisting anything
        List<TransferReceiveItemRowDto> itemsToProcess = new ArrayList<>();
        for (TransferReceiveItemRowDto item : items) {
            BigDecimal rqty = item.getReceivingQty();
            if (rqty != null && rqty.compareTo(BigDecimal.ZERO) > 0) {
                itemsToProcess.add(item);
            }
        }

        if (itemsToProcess.isEmpty()) {
            LOGGER.log(Level.WARNING, "[TRNativeSettle] No items with positive qty — rejecting before persist");
            throw new RuntimeException("Nothing to receive — all quantities are zero.");
        }

        // Step 1: Persist bill header via JPA
        bill.setBillItems(null);
        em.persist(bill);
        em.flush();
        long billId = bill.getId();

        LOGGER.log(Level.INFO, "[TRNativeSettle] Bill header persisted id={0} ms={1}",
                new Object[]{billId, System.currentTimeMillis() - t0});

        Date now = new Date();
        long createrId = (bill.getCreater() != null && bill.getCreater().getId() != null)
                ? bill.getCreater().getId() : 0L;

        // Step 2: Native INSERT BillItem + PharmaceuticalBillItem
        long[] processedBiIds = new long[itemsToProcess.size()];
        long[] processedPbIds = new long[itemsToProcess.size()];

        for (int i = 0; i < itemsToProcess.size(); i++) {
            TransferReceiveItemRowDto item = itemsToProcess.get(i);
            double packs = item.getReceivingQty().doubleValue();
            double units = packs * item.getUnitsPerPack();
            BigDecimal grossRate = item.getGrossRate() != null ? item.getGrossRate() : BigDecimal.ZERO;
            // netValue = -(grossRate * packs) — negative for receive bill (money going out)
            double netValue = grossRate.multiply(BigDecimal.valueOf(packs)).negate().doubleValue();

            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, rate, netRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType,"
                + " referanceBillItem_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
                .setParameter(1, billId)
                .setParameter(2, item.getItemId())
                .setParameter(3, packs)
                .setParameter(4, item.getItemName())
                .setParameter(5, netValue)
                .setParameter(6, netValue)
                .setParameter(7, grossRate.doubleValue())
                .setParameter(8, grossRate.doubleValue())
                .setParameter(9, new Timestamp(now.getTime()))
                .setParameter(10, createrId)
                .setParameter(11, item.getIssuedBillItemId())
                .executeUpdate();
            processedBiIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, itemBatch_ID, stock_ID, qty, stringValue,"
                + " costRate, purchaseRate, retailRate, wholesaleRate, doe)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?)")
                .setParameter(1, processedBiIds[i])
                .setParameter(2, item.getItemBatchId())
                .setParameter(3, item.getStaffStockId()) // initially staff stock; updated after add-to-dept
                .setParameter(4, units)
                .setParameter(5, item.getItemName())
                .setParameter(6, item.getCostRate())
                .setParameter(7, item.getPurchaseRate())
                .setParameter(8, item.getRetailRate())
                .setParameter(9, item.getWholesaleRate())
                .setParameter(10, item.getDateOfExpire() != null
                        ? new Timestamp(item.getDateOfExpire().getTime()) : null)
                .executeUpdate();
            processedPbIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        }

        LOGGER.log(Level.INFO, "[TRNativeSettle] BillItem+PBI inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 3 + 4: Stock operations and StockHistory per item
        for (int i = 0; i < itemsToProcess.size(); i++) {
            TransferReceiveItemRowDto item = itemsToProcess.get(i);
            double packs = item.getReceivingQty().doubleValue();
            double units = packs * item.getUnitsPerPack();

            // 3a. Deduct from staff stock (atomic — throws if insufficient)
            deductStock(item.getStaffStockId(), units);

            // 3b. Find or create dept stock and add qty
            long deptStockId = findOrCreateDeptStock(receivingDeptId, item.getItemBatchId(), units);

            // 3c. Update PharmaceuticalBillItem.stock_ID to point to the dept stock
            em.createNativeQuery("UPDATE " + pharmBillItemTable() + " SET stock_ID=? WHERE ID=?")
                .setParameter(1, deptStockId)
                .setParameter(2, processedPbIds[i])
                .executeUpdate();

            // 3d. Compute post-addition stock qty and aggregates
            double postAddQty = fetchStockQty(deptStockId);
            long ampItemId = (item.getAmpItemId() != null) ? item.getAmpItemId() : item.getItemId();

            StockAggregateResult agg = computeAggregates(
                    item.getItemId(), ampItemId, item.getItemBatchId(), receivingDeptId, receivingInstId,
                    postAddQty,
                    item.getBatchRetailRate(), item.getBatchPurchaseRate(),
                    item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate());

            insertStockHistory(processedPbIds[i], item, agg, ampItemId,
                    item.getItemBatchId(), receivingDeptId, receivingInstId);
        }

        // Step 5: Evict L2 cache for natively written tables
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(StockHistory.class);
        cache.evict(Stock.class);
        cache.evict(BillItem.class);
        cache.evict(Bill.class);

        LOGGER.log(Level.INFO, "[TRNativeSettle] Stock ops + history done ms={0}", System.currentTimeMillis() - t0);

        // Step 6: Finance details via JPA
        double[] totals = insertFinanceDetails(billId, processedBiIds, processedPbIds, itemsToProcess);
        // totals: [absNetTotal, totalCostValue, totalPurchaseValue, totalRetailSaleValue, totalWholesaleValue]

        // Step 7: Update bill-level totals (negative for transfer receive)
        double netTotal = -totals[0]; // negative
        em.createNativeQuery("UPDATE " + billTable() + " SET total=?, netTotal=? WHERE ID=?")
                .setParameter(1, netTotal)
                .setParameter(2, netTotal)
                .setParameter(3, billId)
                .executeUpdate();

        // Step 8: Link backwardReferenceBill and forwardReferenceBills
        if (bill.getBackwardReferenceBill() != null && bill.getBackwardReferenceBill().getId() != null) {
            long issuedBillId = bill.getBackwardReferenceBill().getId();

            em.createNativeQuery("UPDATE " + billTable() + " SET backwardReferenceBill_ID=? WHERE ID=?")
                    .setParameter(1, issuedBillId)
                    .setParameter(2, billId)
                    .executeUpdate();

            // Insert into the Bill.forwardReferenceBills join table
            try {
                String fwdTable = resolveTable("BILL_FORWARDREFERENCEBILLS");
                em.createNativeQuery(
                    "INSERT IGNORE INTO " + fwdTable
                    + " (Bill_ID, forwardReferenceBills_ID) VALUES (?,?)")
                    .setParameter(1, issuedBillId)
                    .setParameter(2, billId)
                    .executeUpdate();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[TRNativeSettle] Could not insert forwardReferenceBill link: {0}",
                        ex.getMessage());
            }

            // Step 9: Update fullyIssued if all items are now received
            updateFullyIssuedIfComplete(issuedBillId,
                    (bill.getCreater() != null && bill.getCreater().getId() != null)
                            ? bill.getCreater().getId() : null);
        }

        LOGGER.log(Level.INFO, "[TRNativeSettle] DONE items={0} ms={1}",
                new Object[]{itemsToProcess.size(), System.currentTimeMillis() - t0});

        // Step 10: Build and return print DTO (caller fills dept/institution/issue fields)
        TransferReceivePrintDto printDto = new TransferReceivePrintDto();
        printDto.setReceiveNo(bill.getDeptId());
        printDto.setReceivedAt(bill.getCreatedAt());
        printDto.setNetTotal(Math.abs(netTotal));
        printDto.setTotalCostValue(totals[1]);
        printDto.setTotalPurchaseValue(totals[2]);
        printDto.setTotalRetailSaleValue(totals[3]);
        printDto.setTotalWholesaleValue(totals[4]);
        printDto.setComments(bill.getComments());

        List<TransferReceiveItemPrintDto> printItems = new ArrayList<>();
        for (int i = 0; i < itemsToProcess.size(); i++) {
            TransferReceiveItemRowDto src = itemsToProcess.get(i);
            TransferReceiveItemPrintDto pi = new TransferReceiveItemPrintDto();
            pi.setSerialNo(src.getSerialNo());
            pi.setItemName(src.getItemName());
            pi.setItemCode(src.getItemCode());
            pi.setBatchNo(src.getBatchNo());
            pi.setDateOfExpire(src.getDateOfExpire());
            double packs = src.getReceivingQty().doubleValue();
            double units = packs * src.getUnitsPerPack();
            pi.setQty(packs);
            pi.setQtyInUnits(units);
            pi.setIssuedQty(src.getIssuedQty());
            BigDecimal grossRate = src.getGrossRate() != null ? src.getGrossRate() : BigDecimal.ZERO;
            double ratePerPack = grossRate.doubleValue();
            pi.setRate(ratePerPack);
            pi.setNetRate(ratePerPack);
            double lineNetValue = grossRate.multiply(BigDecimal.valueOf(packs)).doubleValue();
            pi.setNetValue(lineNetValue);
            pi.setPurchaseRate(src.getPurchaseRate());
            pi.setRetailRate(src.getRetailRate());
            pi.setCostRate(src.getCostRate());
            BigDecimal qtyByUnits = BigDecimal.valueOf(units);
            pi.setValueAtPurchaseRate(BigDecimal.valueOf(src.getPurchaseRate()).multiply(qtyByUnits));
            pi.setValueAtRetailRate(BigDecimal.valueOf(src.getRetailRate()).multiply(qtyByUnits));
            pi.setLineGrossTotal(grossRate.multiply(BigDecimal.valueOf(packs)));
            printItems.add(pi);
        }
        printDto.setItems(printItems);
        return printDto;
    }

    // -----------------------------------------------------------------------
    // fullyIssued update
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Public check methods
    // -----------------------------------------------------------------------

    /**
     * Returns true if all items of the given issued bill have been fully received
     * (no remaining qty greater than 0.001 tolerance).
     * Single native SQL query — avoids N+1 entity graph loading.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isAlreadyReceived(long issuedBillId) {
        String sql = "SELECT"
                + " COALESCE(SUM(ABS(pbi.qty)), 0) AS totalIssued,"
                + " COALESCE((SELECT SUM(ABS(rpbi.qty))"
                + "   FROM " + billTable() + " rb"
                + "   JOIN " + billItemTable() + " rbi ON rbi.bill_ID = rb.ID"
                + "   JOIN " + pharmBillItemTable() + " rpbi ON rpbi.billItem_ID = rbi.ID"
                + "   WHERE rb.backwardReferenceBill_ID = ?"
                + "   AND rb.billType = 'PharmacyTransferReceive'"
                + "   AND rb.DTYPE != 'CancelledBill'"
                + "   AND (rbi.retired IS NULL OR rbi.retired = 0)), 0) AS totalReceived"
                + " FROM " + billItemTable() + " bi"
                + " JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID"
                + " WHERE bi.bill_ID = ? AND (bi.retired IS NULL OR bi.retired = 0)";

        Object[] row = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, issuedBillId)
                .setParameter(2, issuedBillId)
                .getSingleResult();

        double totalIssued = toDouble(row[0]);
        double totalReceived = toDouble(row[1]);

        if (totalIssued <= 0) {
            return false;
        }
        return totalReceived >= totalIssued - 0.001;
    }

    /**
     * Checks whether all issued items are now fully received and, if so,
     * sets fullyIssued=1 on the issued bill via a single UPDATE.
     */
    private void updateFullyIssuedIfComplete(long issuedBillId, Long updaterId) {
        String sql = "SELECT"
                + " SUM(ABS(pbi.qty)) AS totalIssued,"
                + " COALESCE((SELECT SUM(ABS(rpbi.qty))"
                + "   FROM " + billTable() + " rb"
                + "   JOIN " + billItemTable() + " rbi ON rbi.bill_ID = rb.ID"
                + "   JOIN " + pharmBillItemTable() + " rpbi ON rpbi.billItem_ID = rbi.ID"
                + "   WHERE rb.backwardReferenceBill_ID = ?"
                + "   AND rb.billType = 'PharmacyTransferReceive'"
                + "   AND rb.DTYPE != 'CancelledBill'"
                + "   AND (rbi.retired IS NULL OR rbi.retired = 0)), 0) AS totalReceived"
                + " FROM " + billItemTable() + " bi"
                + " JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID"
                + " WHERE bi.bill_ID = ?"
                + " AND (bi.retired IS NULL OR bi.retired = 0)";

        Object[] row = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, issuedBillId)
                .setParameter(2, issuedBillId)
                .getSingleResult();

        double totalIssued = toDouble(row[0]);
        double totalReceived = toDouble(row[1]);

        if (totalIssued > 0 && totalReceived >= totalIssued - 0.001) {
            String updateSql = "UPDATE " + billTable()
                    + " SET fullyIssued=1, fullyIssuedAt=NOW()"
                    + (updaterId != null ? ", fullyIssuedBy_ID=" + updaterId : "")
                    + " WHERE ID=? AND (fullyIssued IS NULL OR fullyIssued=0)";
            em.createNativeQuery(updateSql)
                    .setParameter(1, issuedBillId)
                    .executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // Stock deduction and addition
    // -----------------------------------------------------------------------

    /**
     * Atomically deducts qty from the given stock row.
     * Throws RuntimeException if the stock is insufficient.
     */
    private void deductStock(long stockId, double qty) {
        int updated = em.createNativeQuery(
                "UPDATE " + stockTable() + " SET stock=stock-? WHERE ID=? AND stock>=?")
                .setParameter(1, qty)
                .setParameter(2, stockId)
                .setParameter(3, qty)
                .executeUpdate();
        if (updated == 0) {
            throw new RuntimeException(
                    "Insufficient stock for stock ID " + stockId + " (requested qty=" + qty + ")");
        }
    }

    /**
     * Finds an existing (non-retired) stock record for the given department + itemBatch,
     * adds qty to it, and returns its ID.
     * If no record exists, creates one with the given qty and returns the new ID.
     *
     * Note: in a concurrent environment two threads could both reach the INSERT branch.
     * The SELECT is done first (optimistic) and MySQL's IDENTITY guarantees the caller
     * always gets a valid ID.  A UNIQUE KEY on (department_ID, itemBatch_ID) is the
     * proper long-term fix but is out of scope for this service.
     */
    private long findOrCreateDeptStock(long deptId, long itemBatchId, double addQty) {
        @SuppressWarnings("unchecked")
        List<Object> found = em.createNativeQuery(
                "SELECT ID FROM " + stockTable()
                + " WHERE department_ID=? AND itemBatch_ID=?"
                + " AND (retired IS NULL OR retired=0) LIMIT 1")
                .setParameter(1, deptId)
                .setParameter(2, itemBatchId)
                .getResultList();

        if (!found.isEmpty()) {
            long stockId = ((Number) found.get(0)).longValue();
            em.createNativeQuery("UPDATE " + stockTable() + " SET stock=stock+? WHERE ID=?")
                    .setParameter(1, addQty)
                    .setParameter(2, stockId)
                    .executeUpdate();
            return stockId;
        } else {
            em.createNativeQuery(
                "INSERT INTO " + stockTable()
                + " (department_ID, itemBatch_ID, stock, retired)"
                + " VALUES (?,?,?,0)")
                .setParameter(1, deptId)
                .setParameter(2, itemBatchId)
                .setParameter(3, addQty)
                .executeUpdate();
            return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        }
    }

    private double fetchStockQty(long stockId) {
        Object result = em.createNativeQuery(
                "SELECT stock FROM " + stockTable() + " WHERE ID=?")
                .setParameter(1, stockId)
                .getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }

    // -----------------------------------------------------------------------
    // Aggregate computation (2 queries per item, replacing 9 JPQL calls)
    // -----------------------------------------------------------------------

    private StockAggregateResult computeAggregates(
            long itemId, long ampItemId, long itemBatchId,
            long departmentId, long institutionId,
            double postAddStockQty,
            double retailRate, double purchaseRate, double costRate) {

        StockAggregateResult r = new StockAggregateResult();
        r.setStockQty(postAddStockQty);

        String batchSql =
            "SELECT"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN s.stock ELSE 0 END) AS instBatchQty,"
            + "  SUM(s.stock) AS totalBatchQty"
            + " FROM " + stockTable() + " s"
            + " JOIN " + departmentTable() + " d ON s.department_ID = d.ID"
            + " WHERE s.itemBatch_ID = ?";

        Object[] batchRow = (Object[]) em.createNativeQuery(batchSql)
                .setParameter(1, institutionId)
                .setParameter(2, itemBatchId)
                .getSingleResult();

        double instBatchQty  = batchRow[0] == null ? 0.0 : ((Number) batchRow[0]).doubleValue();
        double totalBatchQty = batchRow[1] == null ? 0.0 : ((Number) batchRow[1]).doubleValue();

        r.setInstitutionBatchQty(instBatchQty);
        r.setTotalBatchQty(totalBatchQty);
        r.setInstitutionBatchStockValueAtPurchaseRate(instBatchQty * purchaseRate);
        r.setTotalBatchStockValueAtPurchaseRate(totalBatchQty * purchaseRate);
        r.setInstitutionBatchStockValueAtSaleRate(instBatchQty * retailRate);
        r.setTotalBatchStockValueAtSaleRate(totalBatchQty * retailRate);
        r.setInstitutionBatchStockValueAtCostRate(instBatchQty * costRate);
        r.setTotalBatchStockValueAtCostRate(totalBatchQty * costRate);

        String itemSql =
            "SELECT"
            + "  SUM(CASE WHEN s.department_ID = ? THEN s.stock ELSE 0 END) AS deptItemQty,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN s.stock ELSE 0 END) AS instItemQty,"
            + "  SUM(s.stock) AS totalItemQty,"
            + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.purcahseRate,0)*s.stock ELSE 0 END) AS deptItemPurchVal,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.purcahseRate,0)*s.stock ELSE 0 END) AS instItemPurchVal,"
            + "  SUM(COALESCE(ib.purcahseRate,0)*s.stock) AS totalItemPurchVal,"
            + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.costRate,0)*s.stock ELSE 0 END) AS deptItemCostVal,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.costRate,0)*s.stock ELSE 0 END) AS instItemCostVal,"
            + "  SUM(COALESCE(ib.costRate,0)*s.stock) AS totalItemCostVal,"
            + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.retailsaleRate,0)*s.stock ELSE 0 END) AS deptItemRetailVal,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.retailsaleRate,0)*s.stock ELSE 0 END) AS instItemRetailVal,"
            + "  SUM(COALESCE(ib.retailsaleRate,0)*s.stock) AS totalItemRetailVal"
            + " FROM " + stockTable() + " s"
            + " JOIN " + itemBatchTable() + " ib ON s.itemBatch_ID = ib.ID"
            + " JOIN " + departmentTable() + " d ON s.department_ID = d.ID"
            + " WHERE ib.item_ID = ?";

        Object[] itemRow = (Object[]) em.createNativeQuery(itemSql)
                .setParameter(1, departmentId)
                .setParameter(2, institutionId)
                .setParameter(3, departmentId)
                .setParameter(4, institutionId)
                .setParameter(5, departmentId)
                .setParameter(6, institutionId)
                .setParameter(7, departmentId)
                .setParameter(8, institutionId)
                .setParameter(9, itemId)
                .getSingleResult();

        r.setDepartmentItemStock(toDouble(itemRow[0]));
        r.setInstitutionItemStock(toDouble(itemRow[1]));
        r.setTotalItemStock(toDouble(itemRow[2]));
        r.setItemStockValueAtPurchaseRate(toDouble(itemRow[3]));
        r.setInstitutionItemStockValueAtPurchaseRate(toDouble(itemRow[4]));
        r.setTotalItemStockValueAtPurchaseRate(toDouble(itemRow[5]));
        r.setItemStockValueAtCostRate(toDouble(itemRow[6]));
        r.setInstitutionItemStockValueAtCostRate(toDouble(itemRow[7]));
        r.setTotalItemStockValueAtCostRate(toDouble(itemRow[8]));
        r.setItemStockValueAtSaleRate(toDouble(itemRow[9]));
        r.setInstitutionItemStockValueAtSaleRate(toDouble(itemRow[10]));
        r.setTotalItemStockValueAtSaleRate(toDouble(itemRow[11]));

        return r;
    }

    private static double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    // -----------------------------------------------------------------------
    // StockHistory native INSERT
    // -----------------------------------------------------------------------

    private void insertStockHistory(long pbId, TransferReceiveItemRowDto item, StockAggregateResult agg,
                                    long ampItemId, long itemBatchId, long departmentId, long institutionId) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        em.createNativeQuery(
            "INSERT INTO " + stockHistoryTable()
            + " (pbItem_ID, itemBatch_ID, institution_ID, department_ID, item_ID,"
            + " retailRate, wholesaleRate, purchaseRate, costRate,"
            + " stockAt, fromDate, createdAt,"
            + " stockQty, instituionBatchQty, totalBatchQty,"
            + " itemStock, institutionItemStock, totalItemStock,"
            + " stockSaleValue, stockPurchaseValue, stockCostValue,"
            + " institutionBatchStockValueAtSaleRate, totalBatchStockValueAtSaleRate,"
            + " institutionBatchStockValueAtPurchaseRate, totalBatchStockValueAtPurchaseRate,"
            + " institutionBatchStockValueAtCostRate, totalBatchStockValueAtCostRate,"
            + " itemStockValueAtSaleRate, institutionItemStockValueAtSaleRate, totalItemStockValueAtSaleRate,"
            + " itemStockValueAtPurchaseRate, institutionItemStockValueAtPurchaseRate, totalItemStockValueAtPurchaseRate,"
            + " itemStockValueAtCostRate, institutionItemStockValueAtCostRate, totalItemStockValueAtCostRate,"
            + " hxYear, hxMonth, hxDate, hxWeek,"
            + " retired)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)")
            .setParameter(1, pbId)
            .setParameter(2, itemBatchId)
            .setParameter(3, institutionId)
            .setParameter(4, departmentId)
            .setParameter(5, ampItemId)
            .setParameter(6, item.getBatchRetailRate())
            .setParameter(7, item.getBatchWholesaleRate())
            .setParameter(8, item.getBatchPurchaseRate())
            .setParameter(9, item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate())
            .setParameter(10, new java.sql.Date(now.getTime()))
            .setParameter(11, new Timestamp(now.getTime()))
            .setParameter(12, new Timestamp(now.getTime()))
            .setParameter(13, agg.getStockQty())
            .setParameter(14, agg.getInstitutionBatchQty())
            .setParameter(15, agg.getTotalBatchQty())
            .setParameter(16, agg.getDepartmentItemStock())
            .setParameter(17, agg.getInstitutionItemStock())
            .setParameter(18, agg.getTotalItemStock())
            .setParameter(19, agg.getStockQty() * item.getBatchRetailRate())
            .setParameter(20, agg.getStockQty() * item.getBatchPurchaseRate())
            .setParameter(21, agg.getStockQty() * (item.getBatchCostRate() != null
                    ? item.getBatchCostRate() : item.getBatchPurchaseRate()))
            .setParameter(22, agg.getInstitutionBatchStockValueAtSaleRate())
            .setParameter(23, agg.getTotalBatchStockValueAtSaleRate())
            .setParameter(24, agg.getInstitutionBatchStockValueAtPurchaseRate())
            .setParameter(25, agg.getTotalBatchStockValueAtPurchaseRate())
            .setParameter(26, agg.getInstitutionBatchStockValueAtCostRate())
            .setParameter(27, agg.getTotalBatchStockValueAtCostRate())
            .setParameter(28, agg.getItemStockValueAtSaleRate())
            .setParameter(29, agg.getInstitutionItemStockValueAtSaleRate())
            .setParameter(30, agg.getTotalItemStockValueAtSaleRate())
            .setParameter(31, agg.getItemStockValueAtPurchaseRate())
            .setParameter(32, agg.getInstitutionItemStockValueAtPurchaseRate())
            .setParameter(33, agg.getTotalItemStockValueAtPurchaseRate())
            .setParameter(34, agg.getItemStockValueAtCostRate())
            .setParameter(35, agg.getInstitutionItemStockValueAtCostRate())
            .setParameter(36, agg.getTotalItemStockValueAtCostRate())
            .setParameter(37, cal.get(Calendar.YEAR))
            .setParameter(38, cal.get(Calendar.MONTH))
            .setParameter(39, cal.get(Calendar.DATE))
            .setParameter(40, cal.get(Calendar.WEEK_OF_YEAR))
            .executeUpdate();
    }

    // -----------------------------------------------------------------------
    // Finance details (JPA IDENTITY — adapts InpatientDirectIssue version for receive)
    // -----------------------------------------------------------------------

    /**
     * Creates BillItemFinanceDetails and BillFinanceDetails rows via JPA.
     * Values are stored as negative for the receive bill (money going out),
     * except cost/purchase/retail totals which are absolute.
     *
     * @return double[] {absNetTotal, totalCostValue, totalPurchaseValue, totalRetailSaleValue, totalWholesaleValue}
     */
    private double[] insertFinanceDetails(long billId, long[] biIds, long[] pbIds,
                                          List<TransferReceiveItemRowDto> items) {
        BigDecimal totalCostValue      = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue  = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue = BigDecimal.ZERO;
        BigDecimal totalQuantity       = BigDecimal.ZERO;
        BigDecimal billNetTotal        = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            TransferReceiveItemRowDto item = items.get(i);

            BigDecimal packs = item.getReceivingQty();
            BigDecimal unitsPerPack = BigDecimal.valueOf(item.getUnitsPerPack() > 0 ? item.getUnitsPerPack() : 1.0);
            BigDecimal qty = packs; // qty in packs stored in BillItem
            BigDecimal qtyByUnits = packs.multiply(unitsPerPack);

            BigDecimal grossRate = item.getGrossRate() != null ? item.getGrossRate() : BigDecimal.ZERO;
            BigDecimal lineGrossTotal = grossRate.multiply(packs).negate(); // negative

            BigDecimal batchRetail    = BigDecimal.valueOf(item.getBatchRetailRate());
            BigDecimal batchPurchase  = BigDecimal.valueOf(item.getBatchPurchaseRate());
            BigDecimal batchWholesale = BigDecimal.valueOf(item.getBatchWholesaleRate());
            BigDecimal costRate       = (item.getBatchCostRate() != null && item.getBatchCostRate() > 0)
                    ? BigDecimal.valueOf(item.getBatchCostRate())
                    : batchPurchase;

            BigDecimal itemCostValue      = costRate.multiply(qtyByUnits);
            BigDecimal itemRetailValue    = batchRetail.multiply(qtyByUnits);
            BigDecimal itemPurchaseValue  = batchPurchase.multiply(qtyByUnits);
            BigDecimal itemWholesaleValue = batchWholesale.multiply(qtyByUnits);

            BillItemFinanceDetails bifd = new BillItemFinanceDetails();
            bifd.setBillItem(em.getReference(BillItem.class, biIds[i]));
            bifd.setCreatedAt(new Date());

            bifd.setLineNetRate(grossRate);
            bifd.setGrossRate(grossRate);
            bifd.setLineGrossRate(grossRate);
            bifd.setBillCostRate(BigDecimal.ZERO);
            bifd.setTotalCostRate(costRate);
            bifd.setLineCostRate(costRate);
            bifd.setCostRate(costRate);
            bifd.setPurchaseRate(BigDecimal.valueOf(item.getPurchaseRate()));
            bifd.setRetailSaleRate(BigDecimal.valueOf(item.getRetailRate()));
            bifd.setWholesaleRate(BigDecimal.valueOf(item.getWholesaleRate()));

            bifd.setLineGrossTotal(lineGrossTotal);
            bifd.setGrossTotal(lineGrossTotal);
            bifd.setLineNetTotal(lineGrossTotal);
            bifd.setNetTotal(lineGrossTotal);

            bifd.setLineCost(itemCostValue);
            bifd.setBillCost(BigDecimal.ZERO);
            bifd.setTotalCost(itemCostValue);

            bifd.setValueAtCostRate(costRate.multiply(qtyByUnits));
            bifd.setValueAtPurchaseRate(batchPurchase.multiply(qtyByUnits));
            bifd.setValueAtRetailRate(batchRetail.multiply(qtyByUnits));
            bifd.setValueAtWholesaleRate(batchWholesale.multiply(qtyByUnits));

            bifd.setQuantity(qty);
            bifd.setQuantityByUnits(qtyByUnits);
            bifd.setTotalQuantity(qty);
            bifd.setFreeQuantity(BigDecimal.ZERO);
            bifd.setFreeQuantityByUnits(BigDecimal.ZERO);
            bifd.setUnitsPerPack(unitsPerPack);

            bifd.setLineDiscount(BigDecimal.ZERO);
            bifd.setLineExpense(BigDecimal.ZERO);
            bifd.setLineTax(BigDecimal.ZERO);
            bifd.setTotalDiscount(BigDecimal.ZERO);
            bifd.setTotalExpense(BigDecimal.ZERO);
            bifd.setTotalTax(BigDecimal.ZERO);
            bifd.setProfitMargin(BigDecimal.ZERO);

            em.persist(bifd);
            em.flush();

            // Link BillItemFinanceDetails into BillItem
            em.createNativeQuery("UPDATE " + billItemTable()
                    + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bifd.getId())
                    .setParameter(2, biIds[i])
                    .executeUpdate();

            // Update PharmaceuticalBillItem with computed rate values
            em.createNativeQuery("UPDATE " + pharmBillItemTable()
                    + " SET costRate=?, costValue=?, retailValue=?, purchaseValue=? WHERE ID=?")
                    .setParameter(1, costRate.doubleValue())
                    .setParameter(2, itemCostValue.doubleValue())
                    .setParameter(3, itemRetailValue.doubleValue())
                    .setParameter(4, itemPurchaseValue.doubleValue())
                    .setParameter(5, pbIds[i])
                    .executeUpdate();

            totalCostValue       = totalCostValue.add(itemCostValue);
            totalPurchaseValue   = totalPurchaseValue.add(itemPurchaseValue);
            totalRetailSaleValue = totalRetailSaleValue.add(itemRetailValue);
            totalWholesaleValue  = totalWholesaleValue.add(itemWholesaleValue);
            totalQuantity        = totalQuantity.add(qty);
            // billNetTotal accumulates the abs net value (grossRate * packs)
            billNetTotal         = billNetTotal.add(grossRate.multiply(packs));
        }

        // BillFinanceDetails — one row per bill; netTotal stored as negative
        Bill billRef = em.getReference(Bill.class, billId);
        BillFinanceDetails bfd = new BillFinanceDetails();
        bfd.setBill(billRef);
        bfd.setCreatedAt(new Date());
        bfd.setNetTotal(billNetTotal.negate());
        bfd.setGrossTotal(billNetTotal.negate());
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalRetailSaleValue(totalRetailSaleValue);
        bfd.setTotalWholesaleValue(totalWholesaleValue);
        bfd.setTotalQuantity(totalQuantity);
        bfd.setTotalFreeQuantity(BigDecimal.ZERO);
        em.persist(bfd);
        em.flush();

        em.createNativeQuery("UPDATE " + billTable() + " SET BILLFINANCEDETAILS_ID=? WHERE ID=?")
                .setParameter(1, bfd.getId())
                .setParameter(2, billId)
                .executeUpdate();

        return new double[]{
            billNetTotal.doubleValue(),        // [0] absNetTotal
            totalCostValue.doubleValue(),      // [1] totalCostValue
            totalPurchaseValue.doubleValue(),  // [2] totalPurchaseValue
            totalRetailSaleValue.doubleValue(),// [3] totalRetailSaleValue
            totalWholesaleValue.doubleValue()  // [4] totalWholesaleValue
        };
    }

    // -----------------------------------------------------------------------
    // Table name resolution (INFORMATION_SCHEMA, cached after first call)
    // -----------------------------------------------------------------------

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"
                + " WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
                .setParameter(1, upperName)
                .getSingleResult();
        return name.toString();
    }

    private String stockHistoryTable() {
        if (tStockHistory == null) tStockHistory = resolveTable("STOCKHISTORY");
        return tStockHistory;
    }

    private String stockTable() {
        if (tStock == null) tStock = resolveTable("STOCK");
        return tStock;
    }

    private String itemBatchTable() {
        if (tItemBatch == null) tItemBatch = resolveTable("ITEMBATCH");
        return tItemBatch;
    }

    private String departmentTable() {
        if (tDepartment == null) tDepartment = resolveTable("DEPARTMENT");
        return tDepartment;
    }

    private String billTable() {
        if (tBill == null) tBill = resolveTable("BILL");
        return tBill;
    }

    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
    }

    private String itemTable() {
        if (tItem == null) tItem = resolveTable("ITEM");
        return tItem;
    }
}
