package com.divudi.service.pharmacy;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.ejb.BillNumberGenerator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Async service for generating stock count bills in the background.
 * Uses DTO projection queries for optimal performance.
 */
@Stateless
public class StockCountGenerationService {
    private static final Logger LOGGER = Logger.getLogger(StockCountGenerationService.class.getName());

    @EJB
    private StockFacade stockFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private StockCountGenerationTracker progressTracker;

    /**
     * Generate stock count bill asynchronously.
     *
     * @param jobId Unique job identifier for progress tracking
     * @param department Department to generate stock count for
     * @param includeZeroStock Whether to include zero-stock batches
     * @param zeroStockLimit Limit of zero-stock batches per item
     * @param user User who initiated the generation
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void generateStockCountBillAsync(String jobId, Department department,
                                            boolean includeZeroStock, int zeroStockLimit,
                                            WebUser user) {
        try {
            LOGGER.log(Level.INFO, "[StockCountGen] Starting async generation. JobId: {0}, Dept: {1}",
                      new Object[]{jobId, department.getName()});

            // Step 1: Fetch stock DTOs with optimized query
            progressTracker.updateProgress(jobId, 0, "Fetching stock data...");
            LOGGER.log(Level.INFO, "[StockCountGen] Updated progress to: Fetching stock data...");

            String jpql = "select new com.divudi.core.data.dto.StockDTO("
                    + "s.id, ib.id, i.id, "
                    + "c.name, i.name, ib.batchNo, "
                    + "ib.dateOfExpire, s.stock, ib.costRate) "
                    + "from Stock s "
                    + "join s.itemBatch ib "
                    + "join ib.item i "
                    + "left join i.category c "
                    + "where s.department=:d and s.stock>0 "
                    + "order by coalesce(c.name, '') asc, "
                    + "coalesce(i.name, '') asc, "
                    + "coalesce(ib.dateOfExpire, current_date) asc";

            HashMap<String, Object> params = new HashMap<>();
            params.put("d", department);

            LOGGER.log(Level.INFO, "[StockCountGen] Executing DTO query...");

            // Give time for log to flush
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            @SuppressWarnings("unchecked")
            List<StockDTO> stockDTOs = (List<StockDTO>) (List<?>) stockFacade.findByJpql(jpql, params);
            LOGGER.log(Level.INFO, "[StockCountGen] DTO query completed. Found {0} stocks", stockDTOs != null ? stockDTOs.size() : 0);

            // Give time for log to flush
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (stockDTOs == null || stockDTOs.isEmpty()) {
                LOGGER.log(Level.WARNING, "[StockCountGen] No stocks found for department: {0}", department.getName());
                progressTracker.fail(jobId, "No stock available in the selected department");
                return;
            }

            int totalItems = stockDTOs.size();
            progressTracker.start(jobId, totalItems, "Processing stock items...");
            LOGGER.log(Level.INFO, "[StockCountGen] Started progress tracking with {0} total items", totalItems);

            // Give time for progress update to flush
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Step 2: Create entity references (proxies) - NO database queries!
            progressTracker.updateProgress(jobId, 0, "Creating entity references...");
            LOGGER.log(Level.INFO, "[StockCountGen] Creating entity proxies using getReference()...");

            // No entity fetching needed - we have all data in DTOs!
            // We only need entity references for JPA relationships

            // Step 3: Create snapshot bill
            progressTracker.updateProgress(jobId, 0, "Creating snapshot bill...");
            LOGGER.log(Level.INFO, "[StockCountGen] Creating snapshot bill...");

            Bill snapshotBill = new Bill();
            snapshotBill.setBillType(BillType.PharmacySnapshotBill);
            snapshotBill.setBillClassType(BillClassType.BilledBill);
            snapshotBill.setDepartment(department);

            if (department.getInstitution() != null) {
                snapshotBill.setInstitution(department.getInstitution());
            }

            snapshotBill.setCreatedAt(new Date());
            snapshotBill.setCreater(user);
            snapshotBill.setBillItems(new java.util.ArrayList<>());

            // Step 4: Process regular stocks
            double total = 0.0;
            int processed = 0;

            for (StockDTO dto : stockDTOs) {
                if (dto == null || dto.getStockId() == null || dto.getItemBatchId() == null || dto.getId() == null) {
                    continue;
                }

                // Use getReference() to create proxies - NO database queries!
                Stock stockProxy = stockFacade.getReference(dto.getStockId());
                ItemBatch itemBatchProxy = itemBatchFacade.getReference(dto.getItemBatchId());
                Item itemProxy = itemFacade.getReference(dto.getId());

                // Create bill item
                BillItem bi = new BillItem();
                bi.setBill(snapshotBill);
                bi.setItem(itemProxy);  // Use proxy reference
                bi.setDescreption(dto.getItemName() != null ? dto.getItemName() : "");
                bi.setQty(dto.getStockQty() != null ? dto.getStockQty() : 0.0);
                bi.setCreatedAt(new Date());
                bi.setCreater(user);

                // Create pharmaceutical bill item
                PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
                pbi.setBillItem(bi);
                pbi.setItemBatch(itemBatchProxy);  // Use proxy reference
                pbi.setQty(dto.getStockQty() != null ? dto.getStockQty() : 0.0);
                pbi.setStock(stockProxy);  // Use proxy reference

                if (dto.getBatchNo() != null) {
                    pbi.setStringValue(dto.getBatchNo());
                }

                double safeCostRate = (dto.getCostRate() != null) ? dto.getCostRate() : 0.0;
                pbi.setCostRate(safeCostRate);

                double safeQty = (bi.getQty() != null) ? bi.getQty() : 0.0;
                double lineValue = safeCostRate * safeQty;

                bi.setNetValue(lineValue);
                total += lineValue;
                bi.setPharmaceuticalBillItem(pbi);

                snapshotBill.getBillItems().add(bi);

                // Update progress every 50 items
                processed++;
                if (processed % 50 == 0) {
                    progressTracker.updateProgress(jobId, processed,
                        String.format("Processed %d of %d items...", processed, totalItems));

                    // Give time for progress update to flush
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            LOGGER.log(Level.INFO, "[StockCountGen] Finished processing {0} stock items", processed);

            // Step 5: Handle zero-stock batches if configured
            if (includeZeroStock && zeroStockLimit > 0) {
                progressTracker.updateProgress(jobId, processed, "Processing zero-stock batches...");

                String zeroStockJpql = "select new com.divudi.core.data.dto.StockDTO("
                        + "s.id, ib.id, i.id, "
                        + "c.name, i.name, ib.batchNo, "
                        + "ib.dateOfExpire, s.stock, ib.costRate) "
                        + "from Stock s "
                        + "join s.itemBatch ib "
                        + "join ib.item i "
                        + "left join i.category c "
                        + "where s.department=:d and (s.stock is null or s.stock = 0) "
                        + "order by coalesce(c.name, '') asc, "
                        + "coalesce(i.name, '') asc, "
                        + "coalesce(ib.dateOfExpire, current_date) desc";

                @SuppressWarnings("unchecked")
                List<StockDTO> zeroStockDTOs = (List<StockDTO>) (List<?>) stockFacade.findByJpql(zeroStockJpql, params);

                if (zeroStockDTOs != null && !zeroStockDTOs.isEmpty()) {
                    LOGGER.log(Level.INFO, "[StockCountGen] Processing {0} zero-stock items (using proxy references)...", zeroStockDTOs.size());

                    // Group by item ID and apply limit
                    Map<Long, List<StockDTO>> zeroStocksByItemId = new java.util.LinkedHashMap<>();
                    for (StockDTO dto : zeroStockDTOs) {
                        if (dto == null || dto.getId() == null) {
                            continue;
                        }
                        Long itemId = dto.getId();
                        zeroStocksByItemId.computeIfAbsent(itemId, k -> new java.util.ArrayList<>()).add(dto);
                    }

                    // Process with limit per item
                    for (List<StockDTO> itemZeroStockDTOs : zeroStocksByItemId.values()) {
                        int limit = Math.min(zeroStockLimit, itemZeroStockDTOs.size());

                        for (int i = 0; i < limit; i++) {
                            StockDTO dto = itemZeroStockDTOs.get(i);

                            if (dto == null || dto.getStockId() == null || dto.getItemBatchId() == null || dto.getId() == null) {
                                continue;
                            }

                            // Use getReference() to create proxies - NO database queries!
                            Stock stockProxy = stockFacade.getReference(dto.getStockId());
                            ItemBatch itemBatchProxy = itemBatchFacade.getReference(dto.getItemBatchId());
                            Item itemProxy = itemFacade.getReference(dto.getId());

                            BillItem bi = new BillItem();
                            bi.setBill(snapshotBill);
                            bi.setItem(itemProxy);  // Use proxy reference
                            bi.setDescreption(dto.getItemName() != null ? dto.getItemName() : "");
                            bi.setQty(0.0);
                            bi.setCreatedAt(new Date());
                            bi.setCreater(user);

                            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
                            pbi.setBillItem(bi);
                            pbi.setItemBatch(itemBatchProxy);  // Use proxy reference
                            pbi.setQty(0.0);
                            pbi.setStock(stockProxy);  // Use proxy reference

                            if (dto.getBatchNo() != null) {
                                pbi.setStringValue(dto.getBatchNo());
                            }

                            double safeCostRate = (dto.getCostRate() != null) ? dto.getCostRate() : 0.0;
                            pbi.setCostRate(safeCostRate);

                            bi.setNetValue(0.0);
                            bi.setPharmaceuticalBillItem(pbi);

                            snapshotBill.getBillItems().add(bi);
                        }
                    }
                }
            }

            // Step 6: Finalize the bill (NOT persisted yet - user will review and record)
            progressTracker.updateProgress(jobId, totalItems, "Finalizing snapshot bill...");
            LOGGER.log(Level.INFO, "[StockCountGen] Finalizing snapshot bill with {0} items...", snapshotBill.getBillItems().size());

            // Give time for progress update to flush
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            snapshotBill.setNetTotal(total);
            // NOTE: Bill is NOT persisted here - stored in memory like sync method
            // User will review and click "Record/Settle Stock Count" to persist

            LOGGER.log(Level.INFO, "[StockCountGen] Bill generated successfully in memory (not persisted yet). Items: {0}", snapshotBill.getBillItems().size());

            // Give time for log to flush
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Step 7: Complete - pass the in-memory bill to tracker
            progressTracker.complete(jobId, snapshotBill);

            LOGGER.log(Level.INFO, "[StockCountGen] Completed successfully. JobId: {0}, Items: {1}",
                      new Object[]{jobId, snapshotBill.getBillItems().size()});

            // Final sleep to ensure completion is visible
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOGGER.log(Level.SEVERE, "[StockCountGen] Failed. JobId: " + jobId + ", Error: " + errorMsg, e);
            progressTracker.fail(jobId, "Error: " + errorMsg);

            // Print stack trace to help debugging
            e.printStackTrace();
        }
    }
}
