package com.divudi.core.data.dto;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.pharmacy.Stock;
import java.io.Serializable;

/**
 * Stock Validation Error DTO
 *
 * Represents a single stock validation failure during the stock-first settlement process.
 * Used to collect and report insufficient stock conditions before bill settlement.
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 */
public class StockValidationError implements Serializable {

    private static final long serialVersionUID = 1L;

    private BillItem billItem;
    private Stock stock;
    private double requestedQty;
    private double availableQty;
    private String errorMessage;

    public StockValidationError() {
    }

    public StockValidationError(BillItem billItem, Stock stock, double requestedQty, double availableQty, String errorMessage) {
        this.billItem = billItem;
        this.stock = stock;
        this.requestedQty = requestedQty;
        this.availableQty = availableQty;
        this.errorMessage = errorMessage;
    }

    // Getters and setters

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public double getRequestedQty() {
        return requestedQty;
    }

    public void setRequestedQty(double requestedQty) {
        this.requestedQty = requestedQty;
    }

    public double getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(double availableQty) {
        this.availableQty = availableQty;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "StockValidationError{" +
                "billItemId=" + (billItem != null ? billItem.getId() : null) +
                ", stockId=" + (stock != null ? stock.getId() : null) +
                ", requestedQty=" + requestedQty +
                ", availableQty=" + availableQty +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
