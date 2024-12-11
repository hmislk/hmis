package com.divudi.data;

import com.divudi.entity.pharmacy.StockHistory;

/**
 *
 * @author Dr M H B Ariyaratne
 * 
 */
public class PharmacyRow {
    private StockHistory stockHistory;
    private Long id;

    public PharmacyRow() {
    }

    public PharmacyRow(StockHistory stockHistory) {
        this.stockHistory = stockHistory;
    }

    public PharmacyRow(Long id) {
        this.id = id;
    }
    
    
    

    public StockHistory getStockHistory() {
        return stockHistory;
    }

    public void setStockHistory(StockHistory stockHistory) {
        this.stockHistory = stockHistory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
    
}
