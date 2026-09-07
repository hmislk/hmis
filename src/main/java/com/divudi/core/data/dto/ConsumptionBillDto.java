package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class ConsumptionBillDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long billId;
    private String deptId;
    private String invoiceNumber;
    private String toDepartmentName;
    private boolean cancelled;
    private boolean fullReturned;

    private Long cancelledBillId;
    private String cancelledBillDeptId;
    private Long refundedBillId;
    private String refundedBillDeptId;
    private Long billedBillId;
    private String billedBillDeptId;

    private double consumptionPurchaseValue;
    private double consumptionCostValue;
    private double consumptionRetailValue;

    private Date createdAt;
    private String creatorName;
    private String comments;

    public ConsumptionBillDto() {}

    // Full constructor for building from Object[] row
    public ConsumptionBillDto(
            Long billId, String deptId, String invoiceNumber,
            String toDepartmentName, boolean cancelled, boolean fullReturned,
            Long cancelledBillId, String cancelledBillDeptId,
            Long refundedBillId, String refundedBillDeptId,
            Long billedBillId, String billedBillDeptId,
            double consumptionPurchaseValue, double consumptionCostValue, double consumptionRetailValue,
            Date createdAt, String creatorName, String comments) {
        this.billId = billId;
        this.deptId = deptId;
        this.invoiceNumber = invoiceNumber;
        this.toDepartmentName = toDepartmentName;
        this.cancelled = cancelled;
        this.fullReturned = fullReturned;
        this.cancelledBillId = cancelledBillId;
        this.cancelledBillDeptId = cancelledBillDeptId;
        this.refundedBillId = refundedBillId;
        this.refundedBillDeptId = refundedBillDeptId;
        this.billedBillId = billedBillId;
        this.billedBillDeptId = billedBillDeptId;
        this.consumptionPurchaseValue = consumptionPurchaseValue;
        this.consumptionCostValue = consumptionCostValue;
        this.consumptionRetailValue = consumptionRetailValue;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.comments = comments;
    }

    // Standard getters and setters for all fields
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public boolean isFullReturned() { return fullReturned; }
    public void setFullReturned(boolean fullReturned) { this.fullReturned = fullReturned; }
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
    public double getConsumptionPurchaseValue() { return consumptionPurchaseValue; }
    public void setConsumptionPurchaseValue(double consumptionPurchaseValue) { this.consumptionPurchaseValue = consumptionPurchaseValue; }
    public double getConsumptionCostValue() { return consumptionCostValue; }
    public void setConsumptionCostValue(double consumptionCostValue) { this.consumptionCostValue = consumptionCostValue; }
    public double getConsumptionRetailValue() { return consumptionRetailValue; }
    public void setConsumptionRetailValue(double consumptionRetailValue) { this.consumptionRetailValue = consumptionRetailValue; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
