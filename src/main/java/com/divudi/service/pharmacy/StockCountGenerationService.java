package com.divudi.service.pharmacy;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.StockFacade;
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
    public void generateStockCountBillAsync(String jobId, Department department,
                                            boolean includeZeroStock, int zeroStockLimit,
                                            WebUser user) {
        try {
            LOGGER.log(Level.INFO, "[StockCountGen] Starting async generation. JobId: {0}, Dept: {1}",
                      new Object[]{jobId, department.getName()});

            // Step 1: Fetch stock DTOs with optimized query
            progressTracker.updateProgress(jobId, 0, "Fetching stock data...");

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

            @SuppressWarnings("unchecked")
            List<StockDTO> stockDTOs = (List<StockDTO>) (List<?>) stockFacade.findByJpql(jpql, params);

            if (stockDTOs == null || stockDTOs.isEmpty()) {
                progressTracker.fail(jobId, "No stock available in the selected department");
                return;
            }

            int totalItems = stockDTOs.size();
            progressTracker.start(jobId, totalItems, "Processing stock items...");

            // Step 2: Fetch Stock entities in bulk
            progressTracker.updateProgress(jobId, 0, "Loading stock entities...");

            List<Long> stockIds = new java.util.ArrayList<>();
            for (StockDTO dto : stockDTOs) {
                if (dto != null && dto.getStockId() != null) {
                    stockIds.add(dto.getStockId());
                }
            }

            String entityJpql = "select s from Stock s "
                    + "join fetch s.itemBatch ib "
                    + "join fetch ib.item "
                    + "where s.id in :ids";
            HashMap<String, Object> entityParams = new HashMap<>();
            entityParams.put("ids", stockIds);
            List<Stock> stocks = stockFacade.findByJpql(entityJpql, entityParams);

            Map<Long, Stock> stockMap = new HashMap<>();
            for (Stock s : stocks) {
                if (s != null) {
                    stockMap.put(s.getId(), s);
                }
            }

            // Step 3: Create snapshot bill
            progressTracker.updateProgress(jobId, 0, "Creating snapshot bill...");

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
                if (dto == null || dto.getStockId() == null) {
                    continue;
                }

                Stock s = stockMap.get(dto.getStockId());
                if (s == null || s.getItemBatch() == null || s.getItemBatch().getItem() == null) {
                    LOGGER.log(Level.WARNING, "Stock entity not found or incomplete. Stock ID: {0}", dto.getStockId());
                    continue;
                }

                ItemBatch itemBatch = s.getItemBatch();

                // Create bill item
                BillItem bi = new BillItem();
                bi.setBill(snapshotBill);
                bi.setItem(itemBatch.getItem());
                bi.setDescreption(dto.getItemName() != null ? dto.getItemName() : "");
                bi.setQty(dto.getStockQty() != null ? dto.getStockQty() : 0.0);
                bi.setCreatedAt(new Date());
                bi.setCreater(user);

                // Create pharmaceutical bill item
                PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
                pbi.setBillItem(bi);
                pbi.setItemBatch(itemBatch);
                pbi.setQty(dto.getStockQty() != null ? dto.getStockQty() : 0.0);
                pbi.setStock(s);

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
                }
            }

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
                    // Fetch zero-stock entities
                    List<Long> zeroStockIds = new java.util.ArrayList<>();
                    for (StockDTO dto : zeroStockDTOs) {
                        if (dto != null && dto.getStockId() != null) {
                            zeroStockIds.add(dto.getStockId());
                        }
                    }

                    String zeroEntityJpql = "select s from Stock s "
                            + "join fetch s.itemBatch ib "
                            + "join fetch ib.item "
                            + "where s.id in :ids";
                    HashMap<String, Object> zeroEntityParams = new HashMap<>();
                    zeroEntityParams.put("ids", zeroStockIds);
                    List<Stock> zeroStocks = stockFacade.findByJpql(zeroEntityJpql, zeroEntityParams);

                    Map<Long, Stock> zeroStockMap = new HashMap<>();
                    for (Stock zs : zeroStocks) {
                        if (zs != null) {
                            zeroStockMap.put(zs.getId(), zs);
                        }
                    }

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
                            Stock zs = zeroStockMap.get(dto.getStockId());

                            if (zs == null || zs.getItemBatch() == null || zs.getItemBatch().getItem() == null) {
                                continue;
                            }

                            ItemBatch itemBatch = zs.getItemBatch();

                            BillItem bi = new BillItem();
                            bi.setBill(snapshotBill);
                            bi.setItem(itemBatch.getItem());
                            bi.setDescreption(dto.getItemName() != null ? dto.getItemName() : "");
                            bi.setQty(0.0);
                            bi.setCreatedAt(new Date());
                            bi.setCreater(user);

                            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
                            pbi.setBillItem(bi);
                            pbi.setItemBatch(itemBatch);
                            pbi.setQty(0.0);
                            pbi.setStock(zs);

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

            // Step 6: Persist the bill
            progressTracker.updateProgress(jobId, totalItems, "Saving snapshot bill...");

            snapshotBill.setNetTotal(total);
            billFacade.create(snapshotBill);

            // Step 7: Complete
            progressTracker.complete(jobId, snapshotBill.getId());

            LOGGER.log(Level.INFO, "[StockCountGen] Completed successfully. JobId: {0}, BillId: {1}, Items: {2}",
                      new Object[]{jobId, snapshotBill.getId(), snapshotBill.getBillItems().size()});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[StockCountGen] Failed. JobId: " + jobId, e);
            progressTracker.fail(jobId, "Error: " + e.getMessage());
        }
    }
}
