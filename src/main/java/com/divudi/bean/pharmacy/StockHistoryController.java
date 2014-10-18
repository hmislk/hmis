/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.entity.pharmacy.StockHistory;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class StockHistoryController implements Serializable {

    List<StockHistory> pharmacyStockHistories;
    List<Date> pharmacyStockHistoryDays;

    
    
    public List<Date> getPharmacyStockHistoryDays() {
        return pharmacyStockHistoryDays;
    }

    public void setPharmacyStockHistoryDays(List<Date> pharmacyStockHistoryDays) {
        this.pharmacyStockHistoryDays = pharmacyStockHistoryDays;
    }
    
    

    public List<StockHistory> getPharmacyStockHistories() {
        return pharmacyStockHistories;
    }

    public void setPharmacyStockHistories(List<StockHistory> pharmacyStockHistories) {
        this.pharmacyStockHistories = pharmacyStockHistories;
    }
    
    
    
    /**
     * Creates a new instance of StockHistoryController
     */
    public StockHistoryController() {
    }
    
}
