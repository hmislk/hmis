/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Pharmacy Return Without Trasing Bill Item-level report
 * Designed for JPQL projection to avoid N+1 query problems
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class PharmacyReturnWithoutTrasingBillItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Bill context
    private Long billId;
    private String billDeptId;
    private Date billCreatedAt;
    private String supplierName;
    private String paymentMethod;

    // Item details
    private Long itemId;
    private String itemName;
    private String itemCode;
    private String itemBarcode;

    // Batch details
    private String batchNo;
    private Date expiryDate;

    // Quantities (negative for returns)
    private Double quantity;
    private Double quantityInUnits;

    // Rates (positive)
    private Double costRate;
    private Double purchaseRate;
    private Double retailRate;

    // Values (negative stock impact, positive revenue)
    private Double costValue;
    private Double purchaseValue;
    private Double retailValue;
    private Double lineTotal;

    // Default constructor
    public PharmacyReturnWithoutTrasingBillItemDTO() {
    }

    // JPQL constructor for projection
    public PharmacyReturnWithoutTrasingBillItemDTO(
            Long billId,
            String billDeptId,
            Date billCreatedAt,
            String supplierName,
            String paymentMethod,
            Long itemId,
            String itemName,
            String itemCode,
            String itemBarcode,
            String batchNo,
            Date expiryDate,
            Double quantity,
            Double quantityInUnits,
            Double costRate,
            Double purchaseRate,
            Double retailRate,
            Double costValue,
            Double purchaseValue,
            Double retailValue,
            Double lineTotal) {

        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.supplierName = supplierName;
        this.paymentMethod = paymentMethod;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.itemBarcode = itemBarcode;
        this.batchNo = batchNo;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.quantityInUnits = quantityInUnits;
        this.costRate = costRate;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.costValue = costValue;
        this.purchaseValue = purchaseValue;
        this.retailValue = retailValue;
        this.lineTotal = lineTotal;
    }

    // Getters and Setters

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillDeptId() {
        return billDeptId;
    }

    public void setBillDeptId(String billDeptId) {
        this.billDeptId = billDeptId;
    }

    public Date getBillCreatedAt() {
        return billCreatedAt;
    }

    public void setBillCreatedAt(Date billCreatedAt) {
        this.billCreatedAt = billCreatedAt;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
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

    public String getItemBarcode() {
        return itemBarcode;
    }

    public void setItemBarcode(String itemBarcode) {
        this.itemBarcode = itemBarcode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getQuantityInUnits() {
        return quantityInUnits;
    }

    public void setQuantityInUnits(Double quantityInUnits) {
        this.quantityInUnits = quantityInUnits;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getCostValue() {
        return costValue;
    }

    public void setCostValue(Double costValue) {
        this.costValue = costValue;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(Double retailValue) {
        this.retailValue = retailValue;
    }

    public Double getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(Double lineTotal) {
        this.lineTotal = lineTotal;
    }

    @Override
    public String toString() {
        return "PharmacyReturnWithoutTrasingBillItemDTO{"
                + "billId=" + billId
                + ", billDeptId=" + billDeptId
                + ", itemName=" + itemName
                + ", quantity=" + quantity
                + ", lineTotal=" + lineTotal + '}';
    }
}