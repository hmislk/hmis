/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.data;

import com.divudi.entity.pharmacy.Stock;

/**
 *
 * @author safrin
 */
public class StockQty {
    private Stock stock;
    private double qty;

    public StockQty(Stock stock, double qty) {
        this.stock = stock;
        this.qty = qty;
    }
    
    

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }
}
