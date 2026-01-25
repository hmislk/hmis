/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.PharmacyBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import javax.inject.Named;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.TemporalType;

/**
 * Service for Direct Issue Batch Operations
 * Provides optimized batch processing for inpatient direct issue workflow
 * Reduces database queries and improves performance through batch operations
 *
 * @author Claude AI Assistant
 */
@Named
@Stateless
@Dependent
@Transactional
public class DirectIssueBatchService implements Serializable {

    @EJB
    private StockFacade stockFacade;

    @EJB
    private PharmacyBean pharmacyBean;

    /**
     * Performs proper stock deduction for multiple bill items
     * Uses PharmacyBean.deductFromStock to ensure stock histories are created
     *
     * @param billItems List of bill items to process
     */
    public void batchStockDeduction(List<BillItem> billItems) {
        if (billItems == null || billItems.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int successCount = 0;

        // Process each item individually to ensure proper stock history creation
        for (BillItem item : billItems) {
            if (item.getPharmaceuticalBillItem() != null &&
                item.getPharmaceuticalBillItem().getStock() != null &&
                item.getBill() != null &&
                item.getBill().getDepartment() != null) {

                Stock stock = item.getPharmaceuticalBillItem().getStock();
                double qty = Math.abs(item.getQty());
                PharmaceuticalBillItem pbi = item.getPharmaceuticalBillItem();

                // CRITICAL: Use PharmacyBean.deductFromStock to ensure stock histories are created
                boolean success = pharmacyBean.deductFromStock(stock, qty, pbi, item.getBill().getDepartment());

                if (!success) {
                    throw new RuntimeException("Failed to deduct stock for item: " +
                        (item.getItem() != null ? item.getItem().getName() : "Unknown") +
                        ". Insufficient stock available.");
                }

                successCount++;
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("=== Stock Deduction Performance ===");
        System.out.println("Items processed: " + successCount + "/" + billItems.size());
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        System.out.println("Average per item: " + ((endTime - startTime) / Math.max(1, successCount)) + "ms");
    }

    /**
     * Validates stock availability for multiple bill items in batch
     * Returns list of stock availability information to catch issues before settlement
     *
     * @param billItems List of bill items to validate
     * @return List of StockDTO with current availability information
     */
    public List<StockDTO> validateBatchStockAvailability(List<BillItem> billItems) {
        if (billItems == null || billItems.isEmpty()) {
            return new ArrayList<>();
        }

        // Collect all stock IDs
        List<Long> stockIds = billItems.stream()
            .filter(bi -> bi.getPharmaceuticalBillItem() != null &&
                         bi.getPharmaceuticalBillItem().getStock() != null)
            .map(bi -> bi.getPharmaceuticalBillItem().getStock().getId())
            .distinct()
            .collect(Collectors.toList());

        if (stockIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Single query to check all stock levels
        return findCurrentStockLevels(stockIds);
    }

    /**
     * Fetches current stock levels for given stock IDs in a single query
     *
     * @param stockIds List of stock IDs to check
     * @return List of StockDTO with current stock levels
     */
    private List<StockDTO> findCurrentStockLevels(List<Long> stockIds) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stockIds", stockIds);
        parameters.put("stockMin", 0.0);

        String sql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "s.id, s.itemBatch.item.name, s.itemBatch.item.code, s.itemBatch.item.vmp.name, "
                + "s.itemBatch.retailsaleRate, s.stock, s.itemBatch.dateOfExpire) "
                + "FROM Stock s "
                + "WHERE s.id IN :stockIds AND s.stock >= :stockMin";

        return (List<StockDTO>) stockFacade.findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP, 100);
    }

    /**
     * Validates if requested quantities are available for all bill items
     *
     * @param billItems List of bill items to validate
     * @return Map of stock ID to availability status (true = sufficient stock)
     */
    public Map<Long, Boolean> validateStockSufficiency(List<BillItem> billItems) {
        Map<Long, Boolean> availabilityMap = new HashMap<>();

        if (billItems == null || billItems.isEmpty()) {
            return availabilityMap;
        }

        // Get current stock levels
        List<StockDTO> currentStocks = validateBatchStockAvailability(billItems);
        Map<Long, Double> stockLevels = new HashMap<>();

        for (StockDTO stockDto : currentStocks) {
            stockLevels.put(stockDto.getId(), stockDto.getStockQty());
        }

        // Calculate required quantities per stock
        Map<Long, Double> requiredQuantities = new HashMap<>();
        for (BillItem item : billItems) {
            if (item.getPharmaceuticalBillItem() != null &&
                item.getPharmaceuticalBillItem().getStock() != null) {

                Long stockId = item.getPharmaceuticalBillItem().getStock().getId();
                Double qty = Math.abs(item.getQty());
                requiredQuantities.merge(stockId, qty, Double::sum);
            }
        }

        // Compare required vs available
        for (Map.Entry<Long, Double> entry : requiredQuantities.entrySet()) {
            Long stockId = entry.getKey();
            Double required = entry.getValue();
            Double available = stockLevels.getOrDefault(stockId, 0.0);

            availabilityMap.put(stockId, available >= required);
        }

        return availabilityMap;
    }

    /**
     * Optimized settlement validation before processing
     * Checks all constraints in a single batch operation
     *
     * @param bill The bill to validate for settlement
     * @return true if all validations pass, false otherwise
     */
    public boolean validateBillForSettlement(Bill bill) {
        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            return false;
        }

        // Batch validate stock sufficiency
        Map<Long, Boolean> stockSufficiency = validateStockSufficiency(bill.getBillItems());

        // Check if any stock is insufficient
        return stockSufficiency.values().stream().allMatch(Boolean::booleanValue);
    }
}