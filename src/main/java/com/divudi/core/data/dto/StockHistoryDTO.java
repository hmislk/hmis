package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.HistoryType;
import java.io.Serializable;
import java.util.Date;

/**
 * Flat projection for StockHistory API responses.
 */
public class StockHistoryDTO implements Serializable {

    private Long id;
    private Date createdAt;
    private Date stockAt;
    private HistoryType historyType;

    private Long itemId;
    private String itemName;
    private String batchNo;
    private Date expiryDate;

    private Long departmentId;
    private String departmentName;

    private Long billId;
    private String billNumber;
    private BillTypeAtomic billTypeAtomic;
    private Double transactionQty;

    private Double stockQty;
    private Double institutionBatchQty;
    private Double totalBatchQty;

    private Double itemStock;
    private Double institutionItemStock;
    private Double totalItemStock;

    private Double retailRate;
    private Double purchaseRate;
    private Double costRate;

    private Double stockSaleValue;
    private Double stockPurchaseValue;
    private Double stockCostValue;

    private Double itemStockValueAtSaleRate;
    private Double itemStockValueAtPurchaseRate;
    private Double itemStockValueAtCostRate;

    private Double institutionBatchStockValueAtSaleRate;
    private Double institutionBatchStockValueAtPurchaseRate;
    private Double institutionBatchStockValueAtCostRate;

    private Double totalBatchStockValueAtSaleRate;
    private Double totalBatchStockValueAtPurchaseRate;
    private Double totalBatchStockValueAtCostRate;

    public StockHistoryDTO(Long id, Date createdAt, Date stockAt, HistoryType historyType,
            Long itemId, String itemName, String batchNo, Date expiryDate,
            Long departmentId, String departmentName,
            Long billId, String billNumber, BillTypeAtomic billTypeAtomic, Double transactionQty,
            Double stockQty, Double institutionBatchQty, Double totalBatchQty,
            Double itemStock, Double institutionItemStock, Double totalItemStock,
            Double retailRate, Double purchaseRate, Double costRate,
            Double stockSaleValue, Double stockPurchaseValue, Double stockCostValue,
            Double itemStockValueAtSaleRate, Double itemStockValueAtPurchaseRate, Double itemStockValueAtCostRate,
            Double institutionBatchStockValueAtSaleRate, Double institutionBatchStockValueAtPurchaseRate, Double institutionBatchStockValueAtCostRate,
            Double totalBatchStockValueAtSaleRate, Double totalBatchStockValueAtPurchaseRate, Double totalBatchStockValueAtCostRate) {
        this.id = id;
        this.createdAt = createdAt;
        this.stockAt = stockAt;
        this.historyType = historyType;
        this.itemId = itemId;
        this.itemName = itemName;
        this.batchNo = batchNo;
        this.expiryDate = expiryDate;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.billId = billId;
        this.billNumber = billNumber;
        this.billTypeAtomic = billTypeAtomic;
        this.transactionQty = transactionQty;
        this.stockQty = stockQty;
        this.institutionBatchQty = institutionBatchQty;
        this.totalBatchQty = totalBatchQty;
        this.itemStock = itemStock;
        this.institutionItemStock = institutionItemStock;
        this.totalItemStock = totalItemStock;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.stockSaleValue = stockSaleValue;
        this.stockPurchaseValue = stockPurchaseValue;
        this.stockCostValue = stockCostValue;
        this.itemStockValueAtSaleRate = itemStockValueAtSaleRate;
        this.itemStockValueAtPurchaseRate = itemStockValueAtPurchaseRate;
        this.itemStockValueAtCostRate = itemStockValueAtCostRate;
        this.institutionBatchStockValueAtSaleRate = institutionBatchStockValueAtSaleRate;
        this.institutionBatchStockValueAtPurchaseRate = institutionBatchStockValueAtPurchaseRate;
        this.institutionBatchStockValueAtCostRate = institutionBatchStockValueAtCostRate;
        this.totalBatchStockValueAtSaleRate = totalBatchStockValueAtSaleRate;
        this.totalBatchStockValueAtPurchaseRate = totalBatchStockValueAtPurchaseRate;
        this.totalBatchStockValueAtCostRate = totalBatchStockValueAtCostRate;
    }

    public Long getId() {
        return id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getStockAt() {
        return stockAt;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Long getBillId() {
        return billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public Double getTransactionQty() {
        return transactionQty;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public Double getInstitutionBatchQty() {
        return institutionBatchQty;
    }

    public Double getTotalBatchQty() {
        return totalBatchQty;
    }

    public Double getItemStock() {
        return itemStock;
    }

    public Double getInstitutionItemStock() {
        return institutionItemStock;
    }

    public Double getTotalItemStock() {
        return totalItemStock;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public Double getCostRate() {
        return costRate;
    }

    public Double getStockSaleValue() {
        return stockSaleValue;
    }

    public Double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public Double getStockCostValue() {
        return stockCostValue;
    }

    public Double getItemStockValueAtSaleRate() {
        return itemStockValueAtSaleRate;
    }

    public Double getItemStockValueAtPurchaseRate() {
        return itemStockValueAtPurchaseRate;
    }

    public Double getItemStockValueAtCostRate() {
        return itemStockValueAtCostRate;
    }

    public Double getInstitutionBatchStockValueAtSaleRate() {
        return institutionBatchStockValueAtSaleRate;
    }

    public Double getInstitutionBatchStockValueAtPurchaseRate() {
        return institutionBatchStockValueAtPurchaseRate;
    }

    public Double getInstitutionBatchStockValueAtCostRate() {
        return institutionBatchStockValueAtCostRate;
    }

    public Double getTotalBatchStockValueAtSaleRate() {
        return totalBatchStockValueAtSaleRate;
    }

    public Double getTotalBatchStockValueAtPurchaseRate() {
        return totalBatchStockValueAtPurchaseRate;
    }

    public Double getTotalBatchStockValueAtCostRate() {
        return totalBatchStockValueAtCostRate;
    }
}
