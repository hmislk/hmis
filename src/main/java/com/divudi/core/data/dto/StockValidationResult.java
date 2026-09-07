package com.divudi.core.data.dto;

import com.divudi.core.entity.pharmacy.Stock;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stock Validation Result DTO
 *
 * Encapsulates the result of stock locking and validation operations.
 * Contains locked stocks, validation status, errors, and audit event reference.
 *
 * Usage Pattern:
 * 1. StockLockingService creates this result during lockAndValidateStocks()
 * 2. Controller checks isValid() to determine if settlement can proceed
 * 3. If invalid, controller displays errors to user and releases locks
 * 4. If valid, controller proceeds with settlement and deducts from locked stocks
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 */
public class StockValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean valid;
    private Map<Long, Stock> lockedStocks; // stockId -> locked Stock entity
    private List<StockValidationError> errors;
    private String auditEventId;

    public StockValidationResult() {
        this.valid = false;
        this.lockedStocks = new HashMap<>();
        this.errors = new ArrayList<>();
    }

    public StockValidationResult(boolean valid) {
        this();
        this.valid = valid;
    }

    /**
     * Adds a validation error to the result
     */
    public void addError(StockValidationError error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.valid = false; // Any error invalidates the result
    }

    /**
     * Adds a locked stock to the map
     */
    public void addLockedStock(Long stockId, Stock stock) {
        if (this.lockedStocks == null) {
            this.lockedStocks = new HashMap<>();
        }
        this.lockedStocks.put(stockId, stock);
    }

    /**
     * Checks if there are any validation errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Gets count of validation errors
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    /**
     * Gets count of locked stocks
     */
    public int getLockedStockCount() {
        return lockedStocks != null ? lockedStocks.size() : 0;
    }

    /**
     * Formats all errors into a single user-friendly message
     */
    public String getFormattedErrorMessage() {
        if (!hasErrors()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Insufficient stock for the following items:\n");
        for (StockValidationError error : errors) {
            sb.append("- ").append(error.getErrorMessage()).append("\n");
        }
        return sb.toString();
    }

    // Getters and setters

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Map<Long, Stock> getLockedStocks() {
        return lockedStocks;
    }

    public void setLockedStocks(Map<Long, Stock> lockedStocks) {
        this.lockedStocks = lockedStocks;
    }

    public List<StockValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<StockValidationError> errors) {
        this.errors = errors;
    }

    public String getAuditEventId() {
        return auditEventId;
    }

    public void setAuditEventId(String auditEventId) {
        this.auditEventId = auditEventId;
    }

    @Override
    public String toString() {
        return "StockValidationResult{" +
                "valid=" + valid +
                ", lockedStockCount=" + getLockedStockCount() +
                ", errorCount=" + getErrorCount() +
                ", auditEventId='" + auditEventId + '\'' +
                '}';
    }
}
