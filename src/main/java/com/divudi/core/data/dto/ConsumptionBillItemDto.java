package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class ConsumptionBillItemDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long billItemId;
    private Long billId;
    private String billDeptId;
    private String invoiceNumber;
    private String toDepartmentName;
    private String itemName;
    private String categoryName;
    private String dosageFormName;
    private double consumptionQty;
    private boolean billCancelled;
    private boolean billFullReturned;

    private Long cancelledBillId;
    private String cancelledBillDeptId;
    private Long refundedBillId;
    private String refundedBillDeptId;
    private Long billedBillId;
    private String billedBillDeptId;

    private double purchaseRate;
    private double consumptionPurchaseValue;
    private double retailRate;
    private double consumptionRetailValue;
    private double costRate;
    private double consumptionCostValue;

    private Date createdAt;
    private String creatorName;
    private String comments;

    public ConsumptionBillItemDto() {}

    public ConsumptionBillItemDto(
            Long billItemId, Long billId, String billDeptId, String invoiceNumber,
            String toDepartmentName, String itemName, String categoryName, String dosageFormName,
            double consumptionQty, boolean billCancelled, boolean billFullReturned,
            Long cancelledBillId, String cancelledBillDeptId,
            Long refundedBillId, String refundedBillDeptId,
            Long billedBillId, String billedBillDeptId,
            double purchaseRate, double consumptionPurchaseValue,
            double retailRate, double consumptionRetailValue,
            double costRate, double consumptionCostValue,
            Date createdAt, String creatorName, String comments) {
        this.billItemId = billItemId;
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.invoiceNumber = invoiceNumber;
        this.toDepartmentName = toDepartmentName;
        this.itemName = itemName;
        this.categoryName = categoryName;
        this.dosageFormName = dosageFormName;
        this.consumptionQty = consumptionQty;
        this.billCancelled = billCancelled;
        this.billFullReturned = billFullReturned;
        this.cancelledBillId = cancelledBillId;
        this.cancelledBillDeptId = cancelledBillDeptId;
        this.refundedBillId = refundedBillId;
        this.refundedBillDeptId = refundedBillDeptId;
        this.billedBillId = billedBillId;
        this.billedBillDeptId = billedBillDeptId;
        this.purchaseRate = purchaseRate;
        this.consumptionPurchaseValue = consumptionPurchaseValue;
        this.retailRate = retailRate;
        this.consumptionRetailValue = consumptionRetailValue;
        this.costRate = costRate;
        this.consumptionCostValue = consumptionCostValue;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.comments = comments;
    }

    // All getters and setters
    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getBillDeptId() { return billDeptId; }
    public void setBillDeptId(String billDeptId) { this.billDeptId = billDeptId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDosageFormName() { return dosageFormName; }
    public void setDosageFormName(String dosageFormName) { this.dosageFormName = dosageFormName; }
    public double getConsumptionQty() { return consumptionQty; }
    public void setConsumptionQty(double consumptionQty) { this.consumptionQty = consumptionQty; }
    public boolean isBillCancelled() { return billCancelled; }
    public void setBillCancelled(boolean billCancelled) { this.billCancelled = billCancelled; }
    public boolean isBillFullReturned() { return billFullReturned; }
    public void setBillFullReturned(boolean billFullReturned) { this.billFullReturned = billFullReturned; }
    public Long getCancelledBillId() { return cancelledBillId; }
    public void setCancelledBillId(Long cancelledBillId) { this.cancelledBillId = cancelledBillId; }
    public String getCancelledBillDeptId() { return cancelledBillDeptId; }
    public void setCancelledBillDeptId(String cancelledBillDeptId) { this.cancelledBillDeptId = cancelledBillDeptId; }
    public Long getRefundedBillId() { return refundedBillId; }
    public void setRefundedBillId(Long refundedBillId) { this.refundedBillId = refundedBillId; }
    public String getRefundedBillDeptId() { return refundedBillDeptId; }
    public void setRefundedBillDeptId(String refundedBillDeptId) { this.refundedBillDeptId = refundedBillDeptId; }
    public Long getBilledBillId() { return billedBillId; }
    public void setBilledBillId(Long billedBillId) { this.billedBillId = billedBillId; }
    public String getBilledBillDeptId() { return billedBillDeptId; }
    public void setBilledBillDeptId(String billedBillDeptId) { this.billedBillDeptId = billedBillDeptId; }
    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }
    public double getConsumptionPurchaseValue() { return consumptionPurchaseValue; }
    public void setConsumptionPurchaseValue(double consumptionPurchaseValue) { this.consumptionPurchaseValue = consumptionPurchaseValue; }
    public double getRetailRate() { return retailRate; }
    public void setRetailRate(double retailRate) { this.retailRate = retailRate; }
    public double getConsumptionRetailValue() { return consumptionRetailValue; }
    public void setConsumptionRetailValue(double consumptionRetailValue) { this.consumptionRetailValue = consumptionRetailValue; }
    public double getCostRate() { return costRate; }
    public void setCostRate(double costRate) { this.costRate = costRate; }
    public double getConsumptionCostValue() { return consumptionCostValue; }
    public void setConsumptionCostValue(double consumptionCostValue) { this.consumptionCostValue = consumptionCostValue; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
