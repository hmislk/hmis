package com.divudi.service;

import com.divudi.bean.common.AuditEventController;
import com.divudi.core.data.dto.StockValidationError;
import com.divudi.core.data.dto.StockValidationResult;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.PharmacyBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.LockTimeoutException;
import javax.persistence.PessimisticLockException;

/**
 * Stock Locking Service for Concurrency-Safe Pharmacy Settlement
 *
 * Implements stock-first settlement pattern to prevent overselling in multi-user environments.
 * Uses pessimistic database locking to ensure atomic stock validation and deduction.
 *
 * Key Responsibilities:
 * - Lock ALL required stocks atomically before validation
 * - Validate stock quantities collectively (all-or-nothing)
 * - Deduct from locked stocks during settlement
 * - Release locks gracefully on success or failure
 * - Maintain comprehensive audit trail
 *
 * Integration Pattern:
 * 1. Controller calls lockAndValidateStocks() before settlement
 * 2. Service locks all stocks with 30-second timeout
 * 3. Service validates quantities against locked (fresh) stock values
 * 4. If valid: Controller settles bill, calls deductAndReleaseLock() for each item
 * 5. If invalid: Controller shows errors, calls releaseLocks() to clean up
 *
 * Thread Safety:
 * - Uses pessimistic locking (PESSIMISTIC_WRITE) for database-level synchronization
 * - Lock timeout prevents indefinite waits (30 seconds)
 * - Proper exception handling for lock contention scenarios
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 */
@Stateless
public class StockLockingService {

    private static final Logger LOGGER = Logger.getLogger(StockLockingService.class.getName());
    private static final int LOCK_TIMEOUT_SECONDS = 30;

    @EJB
    private StockFacade stockFacade;

    @EJB
    private PharmacyBean pharmacyBean;

    @Inject
    private AuditEventController auditEventController;

    /**
     * Lock and Validate Stocks (Step 1 of Stock-First Settlement)
     *
     * Locks all required stocks atomically and validates quantities collectively.
     * This is the critical entry point for preventing overselling.
     *
     * Algorithm:
     * 1. Create audit event for tracking
     * 2. Extract unique stock IDs from bill items
     * 3. Lock ALL stocks atomically (fails fast if any lock unavailable)
     * 4. Validate each locked stock's quantity
     * 5. Collect all validation errors
     * 6. Return result with locked stocks OR errors
     *
     * @param billItems List of bill items requiring stock
     * @param user Current user for audit trail
     * @param department Department context for audit trail
     * @return StockValidationResult containing locked stocks (if valid) or errors (if invalid)
     */
    public StockValidationResult lockAndValidateStocks(List<BillItem> billItems, WebUser user, Department department) {
        LOGGER.log(Level.INFO, "Starting stock lock and validation for {0} bill items", billItems != null ? billItems.size() : 0);

        StockValidationResult result = new StockValidationResult();

        // Create audit event
        AuditEvent auditEvent = auditEventController.createNewAuditEvent(
                "Stock Lock and Validation",
                String.format("Locking stocks for %d bill items", billItems != null ? billItems.size() : 0)
        );
        result.setAuditEventId(auditEvent.getUuid());

        // Validate input
        if (billItems == null || billItems.isEmpty()) {
            LOGGER.log(Level.WARNING, "No bill items provided for stock locking");
            auditEventController.failAuditEvent(auditEvent, "No bill items provided");
            result.setValid(false);
            return result;
        }

        try {
            // Step 1: Extract unique stock IDs and build stock-to-billItems map
            Map<Long, List<BillItem>> stockToBillItems = new HashMap<>();
            for (BillItem billItem : billItems) {
                PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();
                if (pbi == null || pbi.getStock() == null || pbi.getStock().getId() == null) {
                    LOGGER.log(Level.WARNING, "Bill item {0} has no valid stock reference", billItem.getId());
                    continue;
                }

                Long stockId = pbi.getStock().getId();
                stockToBillItems.computeIfAbsent(stockId, k -> new ArrayList<>()).add(billItem);
            }

            if (stockToBillItems.isEmpty()) {
                LOGGER.log(Level.WARNING, "No valid stock references found in bill items");
                auditEventController.failAuditEvent(auditEvent, "No valid stock references found");
                result.setValid(false);
                return result;
            }

            LOGGER.log(Level.INFO, "Attempting to lock {0} unique stocks", stockToBillItems.size());

            // Step 2: Lock ALL stocks atomically
            Map<Long, Stock> lockedStocks = new HashMap<>();
            for (Long stockId : stockToBillItems.keySet()) {
                try {
                    Stock lockedStock = stockFacade.findWithLock(stockId);
                    if (lockedStock == null) {
                        throw new IllegalStateException("Stock not found: " + stockId);
                    }
                    lockedStocks.put(stockId, lockedStock);
                    result.addLockedStock(stockId, lockedStock);
                    LOGGER.log(Level.FINE, "Successfully locked stock {0} with current quantity: {1}",
                            new Object[]{stockId, lockedStock.getStock()});
                } catch (LockTimeoutException | PessimisticLockException e) {
                    LOGGER.log(Level.SEVERE, "Failed to lock stock " + stockId + " - another transaction is using it", e);
                    // Release any locks we acquired so far
                    releaseLocks(lockedStocks, "Lock acquisition failed for stock " + stockId);
                    auditEventController.failAuditEvent(auditEvent,
                            "Lock timeout - stock " + stockId + " is locked by another user");
                    result.setValid(false);
                    StockValidationError error = new StockValidationError(
                            null,
                            null,
                            0,
                            0,
                            "Cannot lock stock - it is currently being used by another user. Please try again."
                    );
                    result.addError(error);
                    return result;
                }
            }

            LOGGER.log(Level.INFO, "Successfully locked {0} stocks, now validating quantities", lockedStocks.size());

            // Step 3: Validate quantities for all locked stocks
            List<StockValidationError> validationErrors = new ArrayList<>();
            for (Map.Entry<Long, List<BillItem>> entry : stockToBillItems.entrySet()) {
                Long stockId = entry.getKey();
                List<BillItem> itemsForThisStock = entry.getValue();
                Stock lockedStock = lockedStocks.get(stockId);

                // Calculate total quantity needed for this stock
                double totalRequiredQty = 0;
                for (BillItem billItem : itemsForThisStock) {
                    PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();
                    if (pbi != null && billItem.getQty() != null) {
                        totalRequiredQty += billItem.getQty();
                    }
                }

                // Validate against locked (fresh) stock quantity
                double availableQty = lockedStock.getStock() != null ? lockedStock.getStock() : 0.0;
                if (availableQty < totalRequiredQty) {
                    String itemName = getItemName(lockedStock);
                    String errorMsg = String.format(
                            "%s: Required %.2f, Available %.2f (Short by %.2f)",
                            itemName,
                            totalRequiredQty,
                            availableQty,
                            totalRequiredQty - availableQty
                    );

                    StockValidationError error = new StockValidationError(
                            itemsForThisStock.get(0), // Representative bill item
                            lockedStock,
                            totalRequiredQty,
                            availableQty,
                            errorMsg
                    );
                    validationErrors.add(error);

                    LOGGER.log(Level.WARNING, "Stock validation failed: {0}", errorMsg);
                }
            }

            // Step 4: Determine overall validation result
            if (validationErrors.isEmpty()) {
                // Success - all stocks validated
                result.setValid(true);
                auditEventController.completeAuditEvent(auditEvent,
                        String.format("Successfully locked and validated %d stocks", lockedStocks.size()));
                LOGGER.log(Level.INFO, "Stock validation successful for all {0} stocks", lockedStocks.size());
            } else {
                // Failure - release locks and return errors
                result.setValid(false);
                result.setErrors(validationErrors);
                releaseLocks(lockedStocks, "Validation failed - insufficient stock");
                auditEventController.failAuditEvent(auditEvent,
                        String.format("Validation failed: %d stock shortages", validationErrors.size()));
                LOGGER.log(Level.WARNING, "Stock validation failed with {0} errors", validationErrors.size());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during stock locking and validation", e);
            auditEventController.failAuditEvent(auditEvent, "Unexpected error: " + e.getMessage());
            result.setValid(false);
            StockValidationError error = new StockValidationError(
                    null,
                    null,
                    0,
                    0,
                    "System error during stock validation: " + e.getMessage()
            );
            result.addError(error);
        }

        return result;
    }

    /**
     * Deduct and Release Lock (Step 2 of Stock-First Settlement)
     *
     * Deducts quantity from a previously locked stock and releases the lock.
     * Called during bill settlement for each item after validation succeeded.
     *
     * IMPORTANT: This method assumes the stock is ALREADY LOCKED by lockAndValidateStocks()
     * and validation has passed. It performs the actual deduction.
     *
     * @param lockedStock The locked stock entity (from StockValidationResult)
     * @param qty Quantity to deduct
     * @param pbi Pharmaceutical bill item for audit trail
     * @param department Department for stock history
     * @return true if deduction successful, false otherwise
     */
    public boolean deductAndReleaseLock(Stock lockedStock, double qty, PharmaceuticalBillItem pbi, Department department) {
        if (lockedStock == null || lockedStock.getId() == null) {
            LOGGER.log(Level.WARNING, "Cannot deduct from null or unsaved stock");
            return false;
        }

        LOGGER.log(Level.FINE, "Deducting {0} from stock {1}", new Object[]{qty, lockedStock.getId()});

        try {
            // Use existing deductFromStock method which handles stock history
            boolean deducted = pharmacyBean.deductFromStock(lockedStock, qty, pbi, department);

            if (deducted) {
                LOGGER.log(Level.INFO, "Successfully deducted {0} from stock {1}",
                        new Object[]{qty, lockedStock.getId()});
            } else {
                LOGGER.log(Level.WARNING, "Failed to deduct {0} from stock {1}",
                        new Object[]{qty, lockedStock.getId()});
            }

            // Lock is automatically released when transaction commits
            return deducted;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deducting from stock " + lockedStock.getId(), e);
            return false;
        }
    }

    /**
     * Release Locks (Cleanup Method)
     *
     * Explicitly releases all locks without deducting stock.
     * Called when settlement is cancelled or validation fails.
     *
     * Note: In JPA/EJB, pessimistic locks are automatically released when the transaction
     * commits or rolls back. This method primarily serves for logging and audit purposes.
     *
     * @param lockedStocks Map of locked stocks to release
     * @param reason Reason for releasing locks (for audit trail)
     */
    public void releaseLocks(Map<Long, Stock> lockedStocks, String reason) {
        if (lockedStocks == null || lockedStocks.isEmpty()) {
            return;
        }

        LOGGER.log(Level.INFO, "Releasing {0} stock locks. Reason: {1}",
                new Object[]{lockedStocks.size(), reason});

        // Create audit event for lock release
        AuditEvent auditEvent = auditEventController.createNewAuditEvent(
                "Stock Locks Released",
                String.format("Releasing %d locks. Reason: %s", lockedStocks.size(), reason)
        );

        try {
            // Locks are automatically released by transaction boundary
            // This is just for logging and audit
            for (Map.Entry<Long, Stock> entry : lockedStocks.entrySet()) {
                LOGGER.log(Level.FINE, "Releasing lock on stock {0}", entry.getKey());
            }

            auditEventController.completeAuditEvent(auditEvent,
                    String.format("Released %d locks successfully", lockedStocks.size()));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error releasing locks", e);
            auditEventController.failAuditEvent(auditEvent, "Error releasing locks: " + e.getMessage());
        }
    }

    /**
     * Helper method to get item name from stock for error messages
     */
    private String getItemName(Stock stock) {
        if (stock == null) {
            return "Unknown Item";
        }

        try {
            if (stock.getItemBatch() != null && stock.getItemBatch().getItem() != null) {
                String name = stock.getItemBatch().getItem().getName();
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error getting item name from stock", e);
        }

        return "Item (Stock ID: " + stock.getId() + ")";
    }
}
