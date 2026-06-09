/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Flat projection of one stock-movement (one StockHistory transaction) for the
 * graphical Stock Movement Timeline.
 *
 * Loaded with a single JPQL constructor query that joins StockHistory ->
 * PharmaceuticalBillItem -> BillItem -> Bill (+ ItemBatch + BillItemFinanceDetails)
 * so the page never triggers lazy per-row loads (avoids the N+1 explosion that
 * made the entity-based Process action slow).
 *
 * @author buddhika
 */
public class StockMovementTimelineDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long stockHistoryId;
    private Date movementAt;

    // Batch identity
    private Long itemBatchId;
    private String batchNo;

    // Batch-level metrics
    private double stockQty;
    private double stockPurchaseValue;
    private double stockSaleValue;

    // Item-level (total) metrics
    private Double itemStock;
    private Double itemStockValueAtPurchaseRate;
    private Double itemStockValueAtSaleRate;

    // Triggering bill
    private Long billId;
    private String deptBillNo;
    private BillTypeAtomic billTypeAtomic;

    // Transaction quantities
    private double pbiQty;
    private double pbiFreeQty;
    private BigDecimal bifdQty;

    /**
     * Constructor matching the JPQL projection. Keep the argument order in sync
     * with the SELECT NEW clause in PharmacyStockMovementAnalyticsController.
     */
    public StockMovementTimelineDTO(
            Long stockHistoryId,
            Date movementAt,
            Long itemBatchId,
            String batchNo,
            double stockQty,
            double stockPurchaseValue,
            double stockSaleValue,
            Double itemStock,
            Double itemStockValueAtPurchaseRate,
            Double itemStockValueAtSaleRate,
            Long billId,
            String deptBillNo,
            BillTypeAtomic billTypeAtomic,
            double pbiQty,
            double pbiFreeQty,
            BigDecimal bifdQty) {
        this.stockHistoryId = stockHistoryId;
        this.movementAt = movementAt;
        this.itemBatchId = itemBatchId;
        this.batchNo = batchNo;
        this.stockQty = stockQty;
        this.stockPurchaseValue = stockPurchaseValue;
        this.stockSaleValue = stockSaleValue;
        this.itemStock = itemStock;
        this.itemStockValueAtPurchaseRate = itemStockValueAtPurchaseRate;
        this.itemStockValueAtSaleRate = itemStockValueAtSaleRate;
        this.billId = billId;
        this.deptBillNo = deptBillNo;
        this.billTypeAtomic = billTypeAtomic;
        this.pbiQty = pbiQty;
        this.pbiFreeQty = pbiFreeQty;
        this.bifdQty = bifdQty;
    }

    public StockMovementTimelineDTO() {
    }

    public Long getStockHistoryId() {
        return stockHistoryId;
    }

    public void setStockHistoryId(Long stockHistoryId) {
        this.stockHistoryId = stockHistoryId;
    }

    public Date getMovementAt() {
        return movementAt;
    }

    public void setMovementAt(Date movementAt) {
        this.movementAt = movementAt;
    }

    public Long getItemBatchId() {
        return itemBatchId;
    }

    public void setItemBatchId(Long itemBatchId) {
        this.itemBatchId = itemBatchId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public double getStockQty() {
        return stockQty;
    }

    public void setStockQty(double stockQty) {
        this.stockQty = stockQty;
    }

    public double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public Double getItemStock() {
        return itemStock;
    }

    public void setItemStock(Double itemStock) {
        this.itemStock = itemStock;
    }

    public Double getItemStockValueAtPurchaseRate() {
        return itemStockValueAtPurchaseRate;
    }

    public void setItemStockValueAtPurchaseRate(Double itemStockValueAtPurchaseRate) {
        this.itemStockValueAtPurchaseRate = itemStockValueAtPurchaseRate;
    }

    public Double getItemStockValueAtSaleRate() {
        return itemStockValueAtSaleRate;
    }

    public void setItemStockValueAtSaleRate(Double itemStockValueAtSaleRate) {
        this.itemStockValueAtSaleRate = itemStockValueAtSaleRate;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getDeptBillNo() {
        return deptBillNo;
    }

    public void setDeptBillNo(String deptBillNo) {
        this.deptBillNo = deptBillNo;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    /** Human-readable bill type label for the marker table. */
    public String getBillType() {
        return billTypeAtomic == null ? null : billTypeAtomic.getLabel();
    }

    public double getPbiQty() {
        return pbiQty;
    }

    public void setPbiQty(double pbiQty) {
        this.pbiQty = pbiQty;
    }

    public double getPbiFreeQty() {
        return pbiFreeQty;
    }

    public void setPbiFreeQty(double pbiFreeQty) {
        this.pbiFreeQty = pbiFreeQty;
    }

    public BigDecimal getBifdQty() {
        return bifdQty;
    }

    public void setBifdQty(BigDecimal bifdQty) {
        this.bifdQty = bifdQty;
    }
}
