package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Pharmacy Transfer Issue Bill Items
 */
public class PharmacyTransferIssueBillItemDTO implements Serializable {
    private String billClassSimpleName; // Simple name of Bill class for row styling
    private String deptId;
    private Date createdAt;
    private String itemName;
    private String itemCode;
    private Double qty;
    private Double costRate;
    private Double costValue;
    private Double retailRate;
    private Double retailValue;
    private Double purchaseRate;
    private Double purchaseValue;
    private Double transferRate;
    private Double transferValue;

    public PharmacyTransferIssueBillItemDTO() {
    }

    public PharmacyTransferIssueBillItemDTO(String deptId, Date createdAt, String itemName, String itemCode,
                                            Double qty, Double costRate, Double costValue,
                                            Double retailRate, Double retailValue,
                                            Double purchaseRate, Double purchaseValue,
                                            Double transferRate, Double transferValue) {
        this.billClassSimpleName = null;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.costRate = costRate;
        this.costValue = costValue;
        this.retailRate = retailRate;
        this.retailValue = retailValue;
        this.purchaseRate = purchaseRate;
        this.purchaseValue = purchaseValue;
        this.transferRate = transferRate;
        this.transferValue = transferValue;
    }

    // Constructor matching JPQL query types: (String, Date, String, String, Double, Double, BigDecimal, Double, BigDecimal, Double, BigDecimal, BigDecimal, BigDecimal)
    public PharmacyTransferIssueBillItemDTO(String deptId, Date createdAt, String itemName, String itemCode,
                                            Double qty, Double costRate, java.math.BigDecimal costValue,
                                            Double retailRate, java.math.BigDecimal retailValue,
                                            Double purchaseRate, java.math.BigDecimal purchaseValue,
                                            java.math.BigDecimal transferRate, java.math.BigDecimal transferValue) {
        this.billClassSimpleName = null;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.costRate = costRate;
        this.costValue = costValue != null ? costValue.doubleValue() : null;
        this.retailRate = retailRate;
        this.retailValue = retailValue != null ? retailValue.doubleValue() : null;
        this.purchaseRate = purchaseRate;
        this.purchaseValue = purchaseValue != null ? purchaseValue.doubleValue() : null;
        this.transferRate = transferRate != null ? transferRate.doubleValue() : null;
        this.transferValue = transferValue != null ? transferValue.doubleValue() : null;
    }

    // New constructor including Bill class for conditional row styling
    // Signature order (to be used with TYPE(b) in JPQL):
    // TYPE(b), b.deptId, b.createdAt, it.name, it.code, bi.qty, ib.costRate, bfd.valueAtCostRate,
    // p.retailRate, bfd.valueAtRetailRate, p.purchaseRate, bfd.valueAtPurchaseRate, bfd.lineGrossRate, bfd.lineGrossTotal
    public PharmacyTransferIssueBillItemDTO(Object billClass,
                                            String deptId, Date createdAt, String itemName, String itemCode,
                                            Double qty, Double costRate, java.math.BigDecimal costValue,
                                            Double retailRate, java.math.BigDecimal retailValue,
                                            Double purchaseRate, java.math.BigDecimal purchaseValue,
                                            java.math.BigDecimal transferRate, java.math.BigDecimal transferValue) {
        this.billClassSimpleName = extractSimpleClassName(billClass);
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.costRate = costRate;
        this.costValue = costValue != null ? costValue.doubleValue() : null;
        this.retailRate = retailRate;
        this.retailValue = retailValue != null ? retailValue.doubleValue() : null;
        this.purchaseRate = purchaseRate;
        this.purchaseValue = purchaseValue != null ? purchaseValue.doubleValue() : null;
        this.transferRate = transferRate != null ? transferRate.doubleValue() : null;
        this.transferValue = transferValue != null ? transferValue.doubleValue() : null;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
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

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }

    public Double getCostValue() {
        return costValue;
    }

    public void setCostValue(Double costValue) {
        this.costValue = costValue;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(Double retailValue) {
        this.retailValue = retailValue;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getTransferRate() {
        return transferRate;
    }

    public void setTransferRate(Double transferRate) {
        this.transferRate = transferRate;
    }

    public Double getTransferValue() {
        return transferValue;
    }

    public void setTransferValue(Double transferValue) {
        this.transferValue = transferValue;
    }

    public String getBillClassSimpleName() {
        return billClassSimpleName;
    }

    private String extractSimpleClassName(Object billClass) {
        if (billClass == null) {
            return "";
        }
        if (billClass instanceof Class<?>) {
            return ((Class<?>) billClass).getSimpleName();
        }
        String s = billClass.toString();
        if (s.startsWith("class ")) {
            s = s.substring(6);
        }
        int lastDot = s.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < s.length() - 1) {
            return s.substring(lastDot + 1);
        }
        return s;
    }
}
