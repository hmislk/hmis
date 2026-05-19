/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Carries all pre-computed stock aggregate values needed to write one
 * StockHistory row without triggering JPA entity loading.
 *
 * Values are fetched by InpatientDirectIssueNativeSqlService via two native
 * SQL queries that compress the 9 JPQL aggregate calls in PharmacyBean.addToStockHistory().
 */
public class StockAggregateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    // Post-deduction stock qty for this stock record (dept-level batch qty)
    private double stockQty;

    // ---- Batch-level quantities ----
    private double institutionBatchQty;
    private double totalBatchQty;

    // ---- Item-level quantities (all batches of the AMP item) ----
    private double departmentItemStock;
    private double institutionItemStock;
    private double totalItemStock;

    // ---- Item-level values at purchase rate ----
    private double itemStockValueAtPurchaseRate;
    private double institutionItemStockValueAtPurchaseRate;
    private double totalItemStockValueAtPurchaseRate;

    // ---- Item-level values at cost rate ----
    private double itemStockValueAtCostRate;
    private double institutionItemStockValueAtCostRate;
    private double totalItemStockValueAtCostRate;

    // ---- Item-level values at retail/sale rate ----
    private double itemStockValueAtSaleRate;
    private double institutionItemStockValueAtSaleRate;
    private double totalItemStockValueAtSaleRate;

    // ---- Batch-level values ----
    private double institutionBatchStockValueAtPurchaseRate;
    private double totalBatchStockValueAtPurchaseRate;
    private double institutionBatchStockValueAtSaleRate;
    private double totalBatchStockValueAtSaleRate;
    private double institutionBatchStockValueAtCostRate;
    private double totalBatchStockValueAtCostRate;

    public StockAggregateResult() {
    }

    public double getStockQty() {
        return stockQty;
    }

    public void setStockQty(double stockQty) {
        this.stockQty = stockQty;
    }

    public double getInstitutionBatchQty() {
        return institutionBatchQty;
    }

    public void setInstitutionBatchQty(double institutionBatchQty) {
        this.institutionBatchQty = institutionBatchQty;
    }

    public double getTotalBatchQty() {
        return totalBatchQty;
    }

    public void setTotalBatchQty(double totalBatchQty) {
        this.totalBatchQty = totalBatchQty;
    }

    public double getDepartmentItemStock() {
        return departmentItemStock;
    }

    public void setDepartmentItemStock(double departmentItemStock) {
        this.departmentItemStock = departmentItemStock;
    }

    public double getInstitutionItemStock() {
        return institutionItemStock;
    }

    public void setInstitutionItemStock(double institutionItemStock) {
        this.institutionItemStock = institutionItemStock;
    }

    public double getTotalItemStock() {
        return totalItemStock;
    }

    public void setTotalItemStock(double totalItemStock) {
        this.totalItemStock = totalItemStock;
    }

    public double getItemStockValueAtPurchaseRate() {
        return itemStockValueAtPurchaseRate;
    }

    public void setItemStockValueAtPurchaseRate(double itemStockValueAtPurchaseRate) {
        this.itemStockValueAtPurchaseRate = itemStockValueAtPurchaseRate;
    }

    public double getInstitutionItemStockValueAtPurchaseRate() {
        return institutionItemStockValueAtPurchaseRate;
    }

    public void setInstitutionItemStockValueAtPurchaseRate(double institutionItemStockValueAtPurchaseRate) {
        this.institutionItemStockValueAtPurchaseRate = institutionItemStockValueAtPurchaseRate;
    }

    public double getTotalItemStockValueAtPurchaseRate() {
        return totalItemStockValueAtPurchaseRate;
    }

    public void setTotalItemStockValueAtPurchaseRate(double totalItemStockValueAtPurchaseRate) {
        this.totalItemStockValueAtPurchaseRate = totalItemStockValueAtPurchaseRate;
    }

    public double getItemStockValueAtCostRate() {
        return itemStockValueAtCostRate;
    }

    public void setItemStockValueAtCostRate(double itemStockValueAtCostRate) {
        this.itemStockValueAtCostRate = itemStockValueAtCostRate;
    }

    public double getInstitutionItemStockValueAtCostRate() {
        return institutionItemStockValueAtCostRate;
    }

    public void setInstitutionItemStockValueAtCostRate(double institutionItemStockValueAtCostRate) {
        this.institutionItemStockValueAtCostRate = institutionItemStockValueAtCostRate;
    }

    public double getTotalItemStockValueAtCostRate() {
        return totalItemStockValueAtCostRate;
    }

    public void setTotalItemStockValueAtCostRate(double totalItemStockValueAtCostRate) {
        this.totalItemStockValueAtCostRate = totalItemStockValueAtCostRate;
    }

    public double getItemStockValueAtSaleRate() {
        return itemStockValueAtSaleRate;
    }

    public void setItemStockValueAtSaleRate(double itemStockValueAtSaleRate) {
        this.itemStockValueAtSaleRate = itemStockValueAtSaleRate;
    }

    public double getInstitutionItemStockValueAtSaleRate() {
        return institutionItemStockValueAtSaleRate;
    }

    public void setInstitutionItemStockValueAtSaleRate(double institutionItemStockValueAtSaleRate) {
        this.institutionItemStockValueAtSaleRate = institutionItemStockValueAtSaleRate;
    }

    public double getTotalItemStockValueAtSaleRate() {
        return totalItemStockValueAtSaleRate;
    }

    public void setTotalItemStockValueAtSaleRate(double totalItemStockValueAtSaleRate) {
        this.totalItemStockValueAtSaleRate = totalItemStockValueAtSaleRate;
    }

    public double getInstitutionBatchStockValueAtPurchaseRate() {
        return institutionBatchStockValueAtPurchaseRate;
    }

    public void setInstitutionBatchStockValueAtPurchaseRate(double institutionBatchStockValueAtPurchaseRate) {
        this.institutionBatchStockValueAtPurchaseRate = institutionBatchStockValueAtPurchaseRate;
    }

    public double getTotalBatchStockValueAtPurchaseRate() {
        return totalBatchStockValueAtPurchaseRate;
    }

    public void setTotalBatchStockValueAtPurchaseRate(double totalBatchStockValueAtPurchaseRate) {
        this.totalBatchStockValueAtPurchaseRate = totalBatchStockValueAtPurchaseRate;
    }

    public double getInstitutionBatchStockValueAtSaleRate() {
        return institutionBatchStockValueAtSaleRate;
    }

    public void setInstitutionBatchStockValueAtSaleRate(double institutionBatchStockValueAtSaleRate) {
        this.institutionBatchStockValueAtSaleRate = institutionBatchStockValueAtSaleRate;
    }

    public double getTotalBatchStockValueAtSaleRate() {
        return totalBatchStockValueAtSaleRate;
    }

    public void setTotalBatchStockValueAtSaleRate(double totalBatchStockValueAtSaleRate) {
        this.totalBatchStockValueAtSaleRate = totalBatchStockValueAtSaleRate;
    }

    public double getInstitutionBatchStockValueAtCostRate() {
        return institutionBatchStockValueAtCostRate;
    }

    public void setInstitutionBatchStockValueAtCostRate(double institutionBatchStockValueAtCostRate) {
        this.institutionBatchStockValueAtCostRate = institutionBatchStockValueAtCostRate;
    }

    public double getTotalBatchStockValueAtCostRate() {
        return totalBatchStockValueAtCostRate;
    }

    public void setTotalBatchStockValueAtCostRate(double totalBatchStockValueAtCostRate) {
        this.totalBatchStockValueAtCostRate = totalBatchStockValueAtCostRate;
    }
}
