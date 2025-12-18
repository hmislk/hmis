
package com.divudi.core.data.dto;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author CHINTHAKA
 * use this for server data for pharmacy movement stock report by item
 * Path - Pharmacy analytics -> movement reports -> Pharmacy Movement Out with Stock Report
 */
public class MovementOutStockReportByItemDto {
    
    private long pharmaceuticalBillItemId;
    private long billItemId;
    private long itemId;
    private long billId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    private String itemName;
    private double quantity;
    private double grossSaleValue;
    private double marginValue;
    private double discountValue;
    private double netSaleValue;
    private double currentStock;

    public MovementOutStockReportByItemDto(long pharmaceuticalBillItemId, long billItemId, long itemId, long billId, Date createdAt, String itemName, double quantity, double grossSaleValue, double marginValue, double discountValue, double netSaleValue) {
        this.pharmaceuticalBillItemId = pharmaceuticalBillItemId;
        this.billItemId = billItemId;
        this.itemId = itemId;
        this.billId = billId;
        this.createdAt = createdAt;
        this.itemName = itemName;
        this.quantity = quantity;
        this.grossSaleValue = grossSaleValue;
        this.marginValue = marginValue;
        this.discountValue = discountValue;
        this.netSaleValue = netSaleValue;
    }

    public double getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(double currentStock) {
        this.currentStock = currentStock;
    }

    public long getBillId() {
        return billId;
    }

    public void setBillId(long billId) {
        this.billId = billId;
    }

    public long getPharmaceuticalBillItemId() {
        return pharmaceuticalBillItemId;
    }

    public void setPharmaceuticalBillItemId(long pharmaceuticalBillItemId) {
        this.pharmaceuticalBillItemId = pharmaceuticalBillItemId;
    }

    public long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(long billItemId) {
        this.billItemId = billItemId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getGrossSaleValue() {
        return grossSaleValue;
    }

    public void setGrossSaleValue(double grossSaleValue) {
        this.grossSaleValue = grossSaleValue;
    }

    public double getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(double marginValue) {
        this.marginValue = marginValue;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public double getNetSaleValue() {
        return netSaleValue;
    }

    public void setNetSaleValue(double netSaleValue) {
        this.netSaleValue = netSaleValue;
    }
    
    
    
    
    
}
