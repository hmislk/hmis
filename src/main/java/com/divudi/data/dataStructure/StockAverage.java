/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Item;
import java.util.List;

/**
 *
 * @author safrin
 */
public class StockAverage {

    private Item item;
    private List<InstitutionStock> institutionStocks;
    private double itemStockTotal;
    private double itemAverageTotal;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<InstitutionStock> getInstitutionStocks() {
        return institutionStocks;
    }

    public void setInstitutionStocks(List<InstitutionStock> institutionStocks) {
        this.institutionStocks = institutionStocks;
    }

    public double getItemStockTotal() {
        return itemStockTotal;
    }

    public void setItemStockTotal(double itemStockTotal) {
        this.itemStockTotal = itemStockTotal;
    }

    public double getItemAverageTotal() {
        return itemAverageTotal;
    }

    public void setItemAverageTotal(double itemAverageTotal) {
        this.itemAverageTotal = itemAverageTotal;
    }
}
