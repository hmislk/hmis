/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.data.dto.TransferIssueItemPrintDto;
import com.divudi.core.data.dto.TransferIssueItemRowDto;
import com.divudi.core.data.dto.TransferIssuePrintDto;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists pharmacy transfer issue bills using native SQL for hot-path operations
 * (stock deduction from dept stock, staff stock creation/update, StockHistory INSERT).
 *
 * Parallel to TransferReceiveNativeSqlService; implements the issue direction:
 *  - Dept stock is deducted (FROM dept)
 *  - Staff stock is incremented (TO staff)
 *
 * Two-query load design:
 *  1. Load all requested items with item / pack-size data in a single join query
 *  2. Load available dept stock grouped by (ampItemId) for all relevant items
 *  Allocate quantities across batches in Java (FEFO order).
 *
 * Related issues: #20583 (parent), #20584 (substitute stock), #20585 (print formats)
 */
@Stateless
public class TransferIssueNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(TransferIssueNativeSqlService.class.getName());

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
     * Loads requested items for the native issue UI.
     * Returns only items with remaining qty greater than zero (tolerance 0.001).
     * Skips Vmpp items (same behaviour as TransferIssueForRequestsController).
     *
     * Uses a two-query approach:
     *   Query 1 - Load all requested bill items with item / pack-size data
     *   Query 2 - Load all available stock in the issuing dept for the relevant amp items (FEFO)
     * Allocates quantities across batches in Java (FEFO order), producing one DTO row
     * per (requested item, batch) combination — matching the multi-batch behaviour of
     * TransferIssueForRequestsController.generateBillComponent().
     *
     * @param requestedBillId  ID of the PharmacyTransferRequest bill
     * @param departmentId     ID of the issuing department (stock source)
     * @param byPurchaseRate   use purchase rate as transfer price
     * @param byCostRate       use cost rate as transfer price (ignored if byPurchaseRate)
     * @return list of issue item rows ready to display; empty rows included for items with no stock
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TransferIssueItemRowDto> loadRequestedItemsForIssue(
            long requestedBillId, long departmentId, boolean byPurchaseRate, boolean byCostRate) {

        // Query 1: requested bill items
        // Note: issuedPhamaceuticalItemQty is @Transient (never stored in DB) — derive
        // alreadyIssuedQty from qty - remainingQty instead.
        String sql1 = "SELECT"
                + " bi.ID AS reqBillItemId,"
                + " bi.qty AS requestedQty,"
                + " (bi.qty - COALESCE(bi.remainingQty, bi.qty)) AS alreadyIssuedQty,"
                + " COALESCE(bi.remainingQty, bi.qty) AS remainingQty,"
                + " COALESCE(bi.searialNo, 0) AS searialNo,"
                + " i.ID AS itemId,"
                + " i.name AS itemName,"
                + " COALESCE(i.code, '') AS itemCode,"
                + " i.DTYPE AS itemDtype,"
                + " COALESCE(i.dblValue, 1.0) AS packSize,"
                + " CASE WHEN i.DTYPE = 'Ampp' THEN COALESCE(i.amp_ID, i.ID) ELSE i.ID END AS ampItemId"
                + " FROM " + billItemTable() + " bi"
                + " JOIN " + itemTable() + " i ON i.ID = bi.item_ID"
                + " WHERE bi.bill_ID = ? AND (bi.retired IS NULL OR bi.retired = 0)"
                + " ORDER BY bi.searialNo";

        @SuppressWarnings("unchecked")
        List<Object[]> reqRows = em.createNativeQuery(sql1)
                .setParameter(1, requestedBillId)
                .getResultList();

        if (reqRows == null || reqRows.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect unique amp item IDs (skipping Vmpp)
        List<Long> ampItemIds = new ArrayList<>();
        for (Object[] row : reqRows) {
            String dtype = row[8] == null ? "" : row[8].toString();
            if ("Vmpp".equals(dtype) || "Vmp".equals(dtype)) {
                continue;
            }
            long ampItemId = ((Number) row[10]).longValue();
            if (!ampItemIds.contains(ampItemId)) {
                ampItemIds.add(ampItemId);
            }
        }

        if (ampItemIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Query 2: available dept stock for those amp items (FEFO order)
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < ampItemIds.size(); i++) {
            if (i > 0) inClause.append(',');
            inClause.append(ampItemIds.get(i));
        }

        String sql2 = "SELECT"
                + " s.ID AS stockId,"
                + " s.stock AS availableQty,"
                + " s.itemBatch_ID,"
                + " ib.batchNo,"
                + " ib.dateOfExpire,"
                + " ib.purcahseRate AS purchaseRate,"
                + " ib.retailsaleRate AS retailRate,"
                + " COALESCE(ib.costRate, 0) AS costRate,"
                + " COALESCE(ib.wholesaleRate, 0) AS wholesaleRate,"
                + " ib.item_ID AS ampItemId"
                + " FROM " + stockTable() + " s"
                + " JOIN " + itemBatchTable() + " ib ON ib.ID = s.itemBatch_ID"
                + " WHERE s.department_ID = ?"
                + " AND s.stock > 0.001"
                + " AND (s.retired IS NULL OR s.retired = 0)"
                + " AND ib.item_ID IN (" + inClause + ")"
                + " ORDER BY ib.dateOfExpire ASC";

        @SuppressWarnings("unchecked")
        List<Object[]> stockRows = em.createNativeQuery(sql2)
                .setParameter(1, departmentId)
                .getResultList();

        // Build map: ampItemId → list of stock rows (FEFO order maintained)
        Map<Long, List<Object[]>> stockByAmpItem = new LinkedHashMap<>();
        if (stockRows != null) {
            for (Object[] row : stockRows) {
                long ampId = ((Number) row[9]).longValue();
                stockByAmpItem.computeIfAbsent(ampId, k -> new ArrayList<>()).add(row);
            }
        }

        // Allocate quantities across batches
        List<TransferIssueItemRowDto> result = new ArrayList<>();
        int serialNo = 0;

        for (Object[] reqRow : reqRows) {
            String dtype = reqRow[8] == null ? "" : reqRow[8].toString();
            if ("Vmpp".equals(dtype) || "Vmp".equals(dtype)) {
                continue;
            }

            long reqBillItemId  = ((Number) reqRow[0]).longValue();
            double requestedQty = toDouble(reqRow[1]);
            double alreadyIssued = toDouble(reqRow[2]);
            double remainingQty  = toDouble(reqRow[3]);
            long itemId          = ((Number) reqRow[5]).longValue();
            String itemName      = reqRow[6] == null ? "" : reqRow[6].toString();
            String itemCode      = reqRow[7] == null ? "" : reqRow[7].toString();
            double packSize      = toDouble(reqRow[9]);
            if (packSize <= 0) packSize = 1.0;
            long ampItemId       = ((Number) reqRow[10]).longValue();

            if (remainingQty <= 0.001) {
                continue;
            }

            double remainingUnits = remainingQty * packSize;
            List<Object[]> stocks = stockByAmpItem.get(ampItemId);

            if (stocks == null || stocks.isEmpty()) {
                // No stock — include an empty placeholder row so user sees the gap
                TransferIssueItemRowDto empty = new TransferIssueItemRowDto();
                empty.setSerialNo(serialNo++);
                empty.setRequestedBillItemId(reqBillItemId);
                empty.setItemId(itemId);
                empty.setAmpItemId(ampItemId);
                empty.setItemName(itemName);
                empty.setItemCode(itemCode);
                empty.setItemDtype(dtype);
                empty.setUnitsPerPack(packSize);
                empty.setRequestedQty(requestedQty);
                empty.setAlreadyIssuedQty(alreadyIssued);
                empty.setRemainingQty(remainingQty);
                empty.setIssuingQty(BigDecimal.ZERO);
                empty.setGrossRate(BigDecimal.ZERO);
                result.add(empty);
                continue;
            }

            double totalAllocatedUnits = 0.0;

            for (Object[] sRow : stocks) {
                if (totalAllocatedUnits >= remainingUnits - 0.001) break;

                double availableUnits = toDouble(sRow[1]);
                if (availableUnits <= 0.001) continue;

                double thisAllocationUnits = Math.min(availableUnits, remainingUnits - totalAllocatedUnits);

                // For Ampp items, only issue complete packs
                if (packSize > 1.0) {
                    double completePacks = Math.floor(thisAllocationUnits / packSize);
                    if (completePacks < 1.0) continue;
                    thisAllocationUnits = completePacks * packSize;
                }

                if (thisAllocationUnits <= 0.001) continue;

                double thisAllocationPacks = thisAllocationUnits / packSize;

                long stockId     = ((Number) sRow[0]).longValue();
                long itemBatchId = ((Number) sRow[2]).longValue();
                String batchNo   = sRow[3] == null ? "" : sRow[3].toString();
                Date doe         = toUtilDate(sRow[4]);
                double purchaseRate  = toDouble(sRow[5]);
                double retailRate    = toDouble(sRow[6]);
                double costRate      = toDouble(sRow[7]);
                double wholesaleRate = toDouble(sRow[8]);

                BigDecimal grossRate = computeTransferRate(purchaseRate, retailRate, costRate, packSize, byPurchaseRate, byCostRate);

                TransferIssueItemRowDto dto = new TransferIssueItemRowDto();
                dto.setSerialNo(serialNo++);
                dto.setRequestedBillItemId(reqBillItemId);
                dto.setItemId(itemId);
                dto.setAmpItemId(ampItemId);
                dto.setItemName(itemName);
                dto.setItemCode(itemCode);
                dto.setItemDtype(dtype);
                dto.setUnitsPerPack(packSize);
                dto.setDeptStockId(stockId);
                dto.setItemBatchId(itemBatchId);
                dto.setBatchNo(batchNo);
                dto.setDateOfExpire(doe);
                dto.setAvailableStock(availableUnits);
                dto.setRequestedQty(requestedQty);
                dto.setAlreadyIssuedQty(alreadyIssued);
                dto.setRemainingQty(remainingQty);
                dto.setIssuingQty(BigDecimal.valueOf(thisAllocationPacks));
                dto.setGrossRate(grossRate);
                dto.setLineTotal(grossRate.multiply(BigDecimal.valueOf(thisAllocationPacks)).doubleValue());
                dto.setPurchaseRate(purchaseRate);
                dto.setRetailRate(retailRate);
                dto.setWholesaleRate(wholesaleRate);
                dto.setCostRate(costRate);
                dto.setBatchRetailRate(retailRate);
                dto.setBatchPurchaseRate(purchaseRate);
                dto.setBatchWholesaleRate(wholesaleRate);
                dto.setBatchCostRate(costRate > 0 ? costRate : null);

                result.add(dto);
                totalAllocatedUnits += thisAllocationUnits;
            }

            if (totalAllocatedUnits <= 0.001) {
                // Stocks exist in map but none allocated (all too small for pack), add empty row
                TransferIssueItemRowDto empty = new TransferIssueItemRowDto();
                empty.setSerialNo(serialNo++);
                empty.setRequestedBillItemId(reqBillItemId);
                empty.setItemId(itemId);
                empty.setAmpItemId(ampItemId);
                empty.setItemName(itemName);
                empty.setItemCode(itemCode);
                empty.setItemDtype(dtype);
                empty.setUnitsPerPack(packSize);
                empty.setRequestedQty(requestedQty);
                empty.setAlreadyIssuedQty(alreadyIssued);
                empty.setRemainingQty(remainingQty);
                empty.setIssuingQty(BigDecimal.ZERO);
                empty.setGrossRate(BigDecimal.ZERO);
                result.add(empty);
            }
        }

        return result;
    }

    /**
     * Checks that each item's source stock (deptStockId) holds at least the units
     * being requested before settlement begins.
     *
     * Returns a list of human-readable error strings — one per stock row that would go
     * negative. An empty list means all stocks are sufficient.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> checkStockSufficiency(List<TransferIssueItemRowDto> items) {
        List<String> errors = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return errors;
        }

        Map<Long, Double> requiredByStock = new HashMap<>();
        Map<Long, String> labelByStock    = new HashMap<>();

        for (TransferIssueItemRowDto item : items) {
            if (item.getIssuingQty() == null
                    || item.getIssuingQty().compareTo(BigDecimal.ZERO) <= 0
                    || item.getDeptStockId() == null) {
                continue;
            }
            double units = item.getIssuingQty().doubleValue() * item.getUnitsPerPack();
            long   sid   = item.getDeptStockId();
            requiredByStock.merge(sid, units, Double::sum);
            labelByStock.putIfAbsent(sid, item.getItemName() + " (batch " + item.getBatchNo() + ")");
        }

        for (Map.Entry<Long, Double> entry : requiredByStock.entrySet()) {
            long   stockId   = entry.getKey();
            double required  = entry.getValue();
            double available = fetchStockQty(stockId);
            if (available < required - 0.001) {
                errors.add(labelByStock.get(stockId)
                        + ": needs " + required
                        + " units, only " + available + " available in dept stock");
            }
        }
        return errors;
    }

    /**
     * Settles the transfer issue using native SQL for stock operations and BillItem inserts.
     * Replicates the logic of TransferIssueForRequestsController.settle() (without UserStock).
     *
     * Steps:
     *  1. Persist bill header via JPA (correct DTYPE + IDENTITY PK)
     *  2. Native INSERT BillItem + PharmaceuticalBillItem per item (negative qty for stock-out)
     *  3. Deduct from dept stock (atomic); find/create staff stock; add to staff stock
     *  4. Compute aggregates + INSERT StockHistory (dept deduction snapshot)
     *  5. Evict L2 cache
     *  6. Finance details via JPA (BillItemFinanceDetails + BillFinanceDetails)
     *  7. Update bill totals (positive netTotal for issue = revenue)
     *  8. Link backwardReferenceBill; insert forwardReferenceBills join row
     *  9. Update issuedPhamaceuticalItemQty / remainingQty on each requested BillItem
     * 10. Update fullyIssued on the requested bill if all items are now issued
     * 11. Build and return TransferIssuePrintDto (caller enriches dept/institution fields)
     *
     * @param bill            pre-populated BilledBill with insId, deptId, creater, createdAt,
     *                        fromDept, toDept, toStaff, backwardRef, billType set by controller
     * @param items           DTO rows from the UI; rows with null deptStockId or zero issuingQty skipped
     * @param issuingDeptId   ID of the issuing department (for StockHistory aggregates)
     * @param issuingInstId   ID of the issuing institution (for StockHistory aggregates)
     * @param staffId         ID of the staff being issued to (for staff stock find/create)
     * @return partial print DTO populated with financial totals and line items
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public TransferIssuePrintDto settle(
            Bill bill,
            List<TransferIssueItemRowDto> items,
            long issuingDeptId,
            long issuingInstId,
            Long staffId) {

        long t0 = System.currentTimeMillis();

        // Filter to items with positive issuingQty and known dept stock
        List<TransferIssueItemRowDto> itemsToProcess = new ArrayList<>();
        for (TransferIssueItemRowDto item : items) {
            BigDecimal iqty = item.getIssuingQty();
            if (iqty != null && iqty.compareTo(BigDecimal.ZERO) > 0 && item.getDeptStockId() != null) {
                itemsToProcess.add(item);
            }
        }

        if (itemsToProcess.isEmpty()) {
            LOGGER.log(Level.WARNING, "[TINativeSettle] No items with positive qty — rejecting before persist");
            throw new RuntimeException("Nothing to issue — all quantities are zero or no stock allocated.");
        }

        // Step 1: Persist bill header via JPA
        bill.setBillItems(null);
        em.persist(bill);
        em.flush();
        long billId = bill.getId();

        LOGGER.log(Level.INFO, "[TINativeSettle] Bill header persisted id={0} ms={1}",
                new Object[]{billId, System.currentTimeMillis() - t0});

        Date now = new Date();
        long createrId = (bill.getCreater() != null && bill.getCreater().getId() != null)
                ? bill.getCreater().getId() : 0L;

        // Step 2: Native INSERT BillItem + PharmaceuticalBillItem per item
        long[] processedBiIds = new long[itemsToProcess.size()];
        long[] processedPbIds = new long[itemsToProcess.size()];
        long[] staffStockIds  = new long[itemsToProcess.size()];

        for (int i = 0; i < itemsToProcess.size(); i++) {
            TransferIssueItemRowDto item = itemsToProcess.get(i);
            double packs = item.getIssuingQty().doubleValue();
            double units = packs * (item.getUnitsPerPack() > 0 ? item.getUnitsPerPack() : 1.0);
            BigDecimal grossRate = item.getGrossRate() != null ? item.getGrossRate() : BigDecimal.ZERO;
            // netValue = positive for issue (revenue from stock going out)
            double netValue = grossRate.multiply(BigDecimal.valueOf(packs)).doubleValue();

            // qty = negative for issue (stock-out direction)
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, rate, netRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType,"
                + " discount, vat, vatPlusNetValue, remainingQty, searialno,"
                + " referanceBillItem_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',0,0,0,0,?,?)")
                .setParameter(1, billId)
                .setParameter(2, item.getItemId())
                .setParameter(3, -packs)
                .setParameter(4, item.getItemName())
                .setParameter(5, netValue)
                .setParameter(6, netValue)
                .setParameter(7, grossRate.doubleValue())
                .setParameter(8, grossRate.doubleValue())
                .setParameter(9, new Timestamp(now.getTime()))
                .setParameter(10, createrId)
                .setParameter(11, i)
                .setParameter(12, item.getRequestedBillItemId())
                .executeUpdate();
            processedBiIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

            // Find or create staff stock first so we have the staffStockId for the PBI INSERT
            Long staffStockId = (staffId != null && staffId > 0)
                    ? findOrCreateStaffStock(staffId, item.getItemBatchId(), units)
                    : null;
            staffStockIds[i] = staffStockId != null ? staffStockId : -1L;

            // PharmaceuticalBillItem: qty = units (negative, stock-out); qtypacks = packs (negative)
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, itemBatch_ID, stock_ID, staffStock_ID,"
                + " qty, qtypacks, stringValue,"
                + " costRate, purchaseRate, retailRate, wholesaleRate, doe,"
                + " remainingQty, remainingQtyPack)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,0,0)")
                .setParameter(1, processedBiIds[i])
                .setParameter(2, item.getItemBatchId())
                .setParameter(3, item.getDeptStockId())
                .setParameter(4, staffStockId)
                .setParameter(5, -units)
                .setParameter(6, -packs)
                .setParameter(7, item.getItemName())
                .setParameter(8, item.getCostRate())
                .setParameter(9, item.getPurchaseRate())
                .setParameter(10, item.getRetailRate())
                .setParameter(11, item.getWholesaleRate())
                .setParameter(12, item.getDateOfExpire() != null
                        ? new Timestamp(item.getDateOfExpire().getTime()) : null)
                .executeUpdate();
            processedPbIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        }

        LOGGER.log(Level.INFO, "[TINativeSettle] BillItem+PBI inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 3 + 4: Stock operations and StockHistory per item
        for (int i = 0; i < itemsToProcess.size(); i++) {
            TransferIssueItemRowDto item = itemsToProcess.get(i);
            double packs = item.getIssuingQty().doubleValue();
            double units = packs * (item.getUnitsPerPack() > 0 ? item.getUnitsPerPack() : 1.0);

            // 3a. Atomically deduct from dept stock
            deductStock(item.getDeptStockId(), units);

            // 3b. Add to staff stock (already created in step 2; just increment qty)
            if (staffStockIds[i] > 0) {
                em.createNativeQuery("UPDATE " + stockTable() + " SET stock=stock+? WHERE ID=?")
                        .setParameter(1, units)
                        .setParameter(2, staffStockIds[i])
                        .executeUpdate();
            }

            // 3c. Compute post-deduction dept stock qty and aggregates
            double postDeductQty = fetchStockQty(item.getDeptStockId());
            long ampItemId = (item.getAmpItemId() != null) ? item.getAmpItemId() : item.getItemId();

            StockAggregateResult agg = computeAggregates(
                    item.getItemId(), ampItemId, item.getItemBatchId(),
                    issuingDeptId, issuingInstId,
                    postDeductQty,
                    item.getBatchRetailRate(), item.getBatchPurchaseRate(),
                    item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate());

            // 3d. INSERT StockHistory for the dept stock deduction
            insertStockHistory(processedPbIds[i], item, agg, ampItemId,
                    item.getItemBatchId(), issuingDeptId, issuingInstId);
        }

        // Step 5: Evict L2 cache for natively written tables
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(StockHistory.class);
        cache.evict(Stock.class);
        cache.evict(BillItem.class);
        cache.evict(Bill.class);

        LOGGER.log(Level.INFO, "[TINativeSettle] Stock ops + history done ms={0}", System.currentTimeMillis() - t0);

        // Step 6: Finance details via JPA
        double[] totals = insertFinanceDetails(billId, processedBiIds, processedPbIds, itemsToProcess);
        // totals: [netTotal, totalCostValue, totalPurchaseValue, totalRetailSaleValue, totalWholesaleValue]

        // Step 7: Update bill-level totals (positive for transfer issue = revenue)
        double netTotal = totals[0];
        em.createNativeQuery("UPDATE " + billTable() + " SET total=?, netTotal=? WHERE ID=?")
                .setParameter(1, netTotal)
                .setParameter(2, netTotal)
                .setParameter(3, billId)
                .executeUpdate();

        // Step 8: Link backwardReferenceBill and forwardReferenceBills join table
        long requestedBillId = -1L;
        if (bill.getBackwardReferenceBill() != null && bill.getBackwardReferenceBill().getId() != null) {
            requestedBillId = bill.getBackwardReferenceBill().getId();

            em.createNativeQuery("UPDATE " + billTable() + " SET backwardReferenceBill_ID=? WHERE ID=?")
                    .setParameter(1, requestedBillId)
                    .setParameter(2, billId)
                    .executeUpdate();

            try {
                String fwdTable = resolveTable("BILL_FORWARDREFERENCEBILLS");
                em.createNativeQuery(
                    "INSERT IGNORE INTO " + fwdTable
                    + " (Bill_ID, forwardReferenceBills_ID) VALUES (?,?)")
                    .setParameter(1, requestedBillId)
                    .setParameter(2, billId)
                    .executeUpdate();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "[TINativeSettle] Could not insert forwardReferenceBill link: {0}",
                        ex.getMessage());
            }
        }

        // Step 9: Update remainingQty on each requested BillItem atomically.
        // The WHERE clause guards against concurrent over-issue: if another transaction
        // already consumed the remaining qty, updateCount=0 and we abort.
        // Note: issuedPhamaceuticalItemQty is @Transient and has no DB column — do NOT update it.
        for (TransferIssueItemRowDto item : itemsToProcess) {
            double packs = item.getIssuingQty().doubleValue();
            int updated = em.createNativeQuery(
                "UPDATE " + billItemTable()
                + " SET remainingQty = GREATEST(0, COALESCE(remainingQty, qty) - ?)"
                + " WHERE ID = ? AND COALESCE(remainingQty, qty) >= ?")
                .setParameter(1, packs)
                .setParameter(2, item.getRequestedBillItemId())
                .setParameter(3, packs - 0.001)
                .executeUpdate();
            if (updated == 0) {
                throw new RuntimeException(
                    "Concurrent modification: insufficient remaining qty for " + item.getItemName()
                    + ". Please reload the request and retry.");
            }
        }

        // Step 10: Update fullyIssued on requested bill if all items are now issued
        if (requestedBillId > 0) {
            updateFullyIssuedIfComplete(requestedBillId,
                    (bill.getCreater() != null && bill.getCreater().getId() != null)
                            ? bill.getCreater().getId() : null);
        }

        LOGGER.log(Level.INFO, "[TINativeSettle] DONE items={0} ms={1}",
                new Object[]{itemsToProcess.size(), System.currentTimeMillis() - t0});

        // Step 11: Build and return print DTO (caller fills dept/institution/party fields)
        TransferIssuePrintDto printDto = new TransferIssuePrintDto();
        printDto.setIssueNo(bill.getDeptId());
        printDto.setIssuedAt(bill.getCreatedAt());
        printDto.setNetTotal(netTotal);
        printDto.setTotalCostValue(totals[1]);
        printDto.setTotalPurchaseValue(totals[2]);
        printDto.setTotalRetailSaleValue(totals[3]);
        printDto.setTotalWholesaleValue(totals[4]);
        printDto.setComments(bill.getComments());

        List<TransferIssueItemPrintDto> printItems = new ArrayList<>();
        for (int i = 0; i < itemsToProcess.size(); i++) {
            TransferIssueItemRowDto src = itemsToProcess.get(i);
            TransferIssueItemPrintDto pi = new TransferIssueItemPrintDto();
            pi.setSerialNo(src.getSerialNo());
            pi.setItemName(src.getItemName());
            pi.setItemCode(src.getItemCode());
            pi.setBatchNo(src.getBatchNo());
            pi.setDateOfExpire(src.getDateOfExpire());
            double packs = src.getIssuingQty().doubleValue();
            double units = packs * (src.getUnitsPerPack() > 0 ? src.getUnitsPerPack() : 1.0);
            pi.setQty(packs);
            pi.setQtyInUnits(units);
            pi.setRequestedQty(src.getRequestedQty());
            BigDecimal grossRate = src.getGrossRate() != null ? src.getGrossRate() : BigDecimal.ZERO;
            pi.setRate(grossRate.doubleValue());
            pi.setNetRate(grossRate.doubleValue());
            pi.setNetValue(grossRate.multiply(BigDecimal.valueOf(packs)).doubleValue());
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

    private void updateFullyIssuedIfComplete(long requestedBillId, Long updaterId) {
        // Count request bill items that still have remaining qty > 0
        Object countObj = em.createNativeQuery(
                "SELECT COUNT(*) FROM " + billItemTable()
                + " WHERE bill_ID = ?"
                + " AND (retired IS NULL OR retired = 0)"
                + " AND COALESCE(remainingQty, qty) > 0.001")
                .setParameter(1, requestedBillId)
                .getSingleResult();

        long remaining = ((Number) countObj).longValue();

        if (remaining == 0) {
            String updateSql = "UPDATE " + billTable()
                    + " SET fullyIssued=1, fullyIssuedAt=NOW()"
                    + (updaterId != null ? ", fullyIssuedBy_ID=" + updaterId : "")
                    + " WHERE ID=? AND (fullyIssued IS NULL OR fullyIssued=0)";
            em.createNativeQuery(updateSql)
                    .setParameter(1, requestedBillId)
                    .executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // Stock deduction, staff stock creation
    // -----------------------------------------------------------------------

    /** Atomically deducts qty from the given dept stock row. Throws if insufficient. */
    private void deductStock(long stockId, double qty) {
        int updated = em.createNativeQuery(
                "UPDATE " + stockTable() + " SET stock=stock-? WHERE ID=? AND stock>=?")
                .setParameter(1, qty)
                .setParameter(2, stockId)
                .setParameter(3, qty)
                .executeUpdate();
        if (updated == 0) {
            throw new RuntimeException(
                    "Insufficient dept stock for stock ID " + stockId + " (qty=" + qty + ")");
        }
    }

    /**
     * Finds or creates a staff stock record for the given staff + itemBatch atomically.
     * Uses INSERT IGNORE so concurrent calls for the same (staff_ID, itemBatch_ID) pair
     * are safe: only one row is created even if two transactions race.
     * Returns the staff stock ID — does NOT increment qty yet.
     */
    private long findOrCreateStaffStock(long staffId, long itemBatchId, double initialQty) {
        // Attempt atomic insert; duplicate (staff_ID, itemBatch_ID) is silently ignored.
        em.createNativeQuery(
            "INSERT IGNORE INTO " + stockTable()
            + " (staff_ID, itemBatch_ID, stock, retired,"
            + " itemName, barcode, longCode, dateOfExpire, retailsaleRate)"
            + " SELECT ?, ib.ID, 0, 0,"
            + "   COALESCE(i.name, 'UNKNOWN'),"
            + "   COALESCE(i.barcode, ''),"
            + "   0,"
            + "   ib.dateOfExpire,"
            + "   ib.retailsaleRate"
            + " FROM " + itemBatchTable() + " ib"
            + " JOIN " + itemTable() + " i ON i.ID = ib.item_ID"
            + " WHERE ib.ID = ?"
            + "   AND NOT EXISTS ("
            + "     SELECT 1 FROM " + stockTable() + " s2"
            + "     WHERE s2.staff_ID=? AND s2.itemBatch_ID=?"
            + "       AND (s2.retired IS NULL OR s2.retired=0))")
            .setParameter(1, staffId)
            .setParameter(2, itemBatchId)
            .setParameter(3, staffId)
            .setParameter(4, itemBatchId)
            .executeUpdate();

        // Always re-select — works whether we just inserted or the row already existed.
        @SuppressWarnings("unchecked")
        List<Object> found = em.createNativeQuery(
                "SELECT ID FROM " + stockTable()
                + " WHERE staff_ID=? AND itemBatch_ID=?"
                + " AND (retired IS NULL OR retired=0) LIMIT 1")
                .setParameter(1, staffId)
                .setParameter(2, itemBatchId)
                .getResultList();

        if (found.isEmpty()) {
            throw new RuntimeException(
                "Could not find or create staff stock for staffId=" + staffId
                + " itemBatchId=" + itemBatchId);
        }
        return ((Number) found.get(0)).longValue();
    }

    private double fetchStockQty(long stockId) {
        Object result = em.createNativeQuery(
                "SELECT stock FROM " + stockTable() + " WHERE ID=?")
                .setParameter(1, stockId)
                .getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }

    // -----------------------------------------------------------------------
    // Aggregate computation (same as TransferReceiveNativeSqlService)
    // -----------------------------------------------------------------------

    private StockAggregateResult computeAggregates(
            long itemId, long ampItemId, long itemBatchId,
            long departmentId, long institutionId,
            double postUpdateStockQty,
            double retailRate, double purchaseRate, double costRate) {

        StockAggregateResult r = new StockAggregateResult();
        r.setStockQty(postUpdateStockQty);

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
                .setParameter(9, ampItemId)
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

    private static java.util.Date toUtilDate(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Date)      return new java.util.Date(((java.sql.Date) o).getTime());
        if (o instanceof java.sql.Timestamp) return new java.util.Date(((java.sql.Timestamp) o).getTime());
        if (o instanceof java.time.LocalDateTime) {
            return java.util.Date.from(((java.time.LocalDateTime) o)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (o instanceof java.time.LocalDate) {
            return java.util.Date.from(((java.time.LocalDate) o)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // StockHistory native INSERT
    // -----------------------------------------------------------------------

    private void insertStockHistory(long pbId, TransferIssueItemRowDto item, StockAggregateResult agg,
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
    // Finance details (JPA IDENTITY — mirrors TransferReceiveNativeSqlService)
    // -----------------------------------------------------------------------

    /**
     * Creates BillItemFinanceDetails and BillFinanceDetails rows via JPA.
     * Values are stored as positive for the issue bill (revenue direction).
     *
     * @return double[] {netTotal, totalCostValue, totalPurchaseValue, totalRetailSaleValue, totalWholesaleValue}
     */
    private double[] insertFinanceDetails(long billId, long[] biIds, long[] pbIds,
                                          List<TransferIssueItemRowDto> items) {
        BigDecimal totalCostValue       = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue   = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue  = BigDecimal.ZERO;
        BigDecimal totalQuantity        = BigDecimal.ZERO;
        BigDecimal billNetTotal         = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            TransferIssueItemRowDto item = items.get(i);

            BigDecimal packs      = item.getIssuingQty();
            BigDecimal upb        = BigDecimal.valueOf(item.getUnitsPerPack() > 0 ? item.getUnitsPerPack() : 1.0);
            BigDecimal qty        = packs;
            BigDecimal qtyByUnits = packs.multiply(upb);

            BigDecimal grossRate      = item.getGrossRate() != null ? item.getGrossRate() : BigDecimal.ZERO;
            BigDecimal lineGrossTotal = grossRate.multiply(packs); // positive for issue (revenue)

            BigDecimal batchRetail    = BigDecimal.valueOf(item.getBatchRetailRate());
            BigDecimal batchPurchase  = BigDecimal.valueOf(item.getBatchPurchaseRate());
            BigDecimal batchWholesale = BigDecimal.valueOf(item.getBatchWholesaleRate());
            BigDecimal costRate = (item.getBatchCostRate() != null && item.getBatchCostRate() > 0)
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

            // Issue direction: positive line totals (revenue)
            bifd.setLineGrossTotal(lineGrossTotal);
            bifd.setGrossTotal(lineGrossTotal);
            bifd.setLineNetTotal(lineGrossTotal);
            bifd.setNetTotal(lineGrossTotal);

            // Cost values (what the stock was worth)
            bifd.setLineCost(itemCostValue.negate()); // negative cost-of-goods for issue
            bifd.setBillCost(BigDecimal.ZERO);
            bifd.setTotalCost(itemCostValue.negate());

            bifd.setValueAtCostRate(costRate.multiply(qtyByUnits));
            bifd.setValueAtPurchaseRate(batchPurchase.multiply(qtyByUnits));
            bifd.setValueAtRetailRate(batchRetail.multiply(qtyByUnits));
            bifd.setValueAtWholesaleRate(batchWholesale.multiply(qtyByUnits));

            // Quantities — negative for issue (stock going out)
            bifd.setQuantity(qty.negate());
            bifd.setQuantityByUnits(qtyByUnits.negate());
            bifd.setTotalQuantity(qty.negate());
            bifd.setFreeQuantity(BigDecimal.ZERO);
            bifd.setFreeQuantityByUnits(BigDecimal.ZERO);
            bifd.setUnitsPerPack(upb);

            bifd.setLineDiscount(BigDecimal.ZERO);
            bifd.setLineExpense(BigDecimal.ZERO);
            bifd.setLineTax(BigDecimal.ZERO);
            bifd.setTotalDiscount(BigDecimal.ZERO);
            bifd.setTotalExpense(BigDecimal.ZERO);
            bifd.setTotalTax(BigDecimal.ZERO);
            bifd.setProfitMargin(BigDecimal.ZERO);

            em.persist(bifd);
            em.flush();

            em.createNativeQuery("UPDATE " + billItemTable()
                    + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bifd.getId())
                    .setParameter(2, biIds[i])
                    .executeUpdate();

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
            billNetTotal         = billNetTotal.add(grossRate.multiply(packs));
        }

        // BillFinanceDetails — netTotal positive (revenue); purchase/retail/cost/wholesale
        // negative (stock-out = loss of inventory value), matching the sign convention of
        // the existing JPA-based transfer issue controller.
        Bill billRef = em.getReference(Bill.class, billId);
        BillFinanceDetails bfd = new BillFinanceDetails();
        bfd.setBill(billRef);
        bfd.setCreatedAt(new Date());
        bfd.setNetTotal(billNetTotal);
        bfd.setGrossTotal(billNetTotal);
        bfd.setTotalCostValue(totalCostValue.negate());
        bfd.setTotalPurchaseValue(totalPurchaseValue.negate());
        bfd.setTotalRetailSaleValue(totalRetailSaleValue.negate());
        bfd.setTotalWholesaleValue(totalWholesaleValue.negate());
        bfd.setTotalQuantity(totalQuantity.negate());
        bfd.setTotalFreeQuantity(BigDecimal.ZERO);
        em.persist(bfd);
        em.flush();

        em.createNativeQuery("UPDATE " + billTable() + " SET BILLFINANCEDETAILS_ID=? WHERE ID=?")
                .setParameter(1, bfd.getId())
                .setParameter(2, billId)
                .executeUpdate();

        return new double[]{
            billNetTotal.doubleValue(),         // [0] netTotal
            totalCostValue.doubleValue(),       // [1] totalCostValue
            totalPurchaseValue.doubleValue(),   // [2] totalPurchaseValue
            totalRetailSaleValue.doubleValue(), // [3] totalRetailSaleValue
            totalWholesaleValue.doubleValue()   // [4] totalWholesaleValue
        };
    }

    // -----------------------------------------------------------------------
    // Transfer rate determination
    // -----------------------------------------------------------------------

    private BigDecimal computeTransferRate(double purchaseRate, double retailRate, double costRate,
                                           double packSize, boolean byPurchaseRate, boolean byCostRate) {
        double unitRate;
        if (byPurchaseRate) {
            unitRate = purchaseRate;
        } else if (byCostRate) {
            unitRate = costRate;
        } else {
            unitRate = retailRate;
        }
        // grossRate per pack = unitRate * unitsPerPack
        return BigDecimal.valueOf(unitRate * (packSize > 0 ? packSize : 1.0));
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
