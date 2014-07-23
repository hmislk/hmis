/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
