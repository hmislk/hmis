package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class StockConsumptionItemDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long billItemId;
    private Long billId;
    private String deptId;
    private Long referenceBillId;
    private String referenceBillDeptId;
    private Date createdAt;
    private String itemName;
    private String itemCode;
    private String batchNo;
    private double qty;
    private double purchaseRate;
    private double purchaseValue;
    private double costRate;
    private double costValue;
    private double retailRate;
    private double retailValue;
    private String toDepartmentName;
    private String comments;
    private String measurementUnitName;

    public StockConsumptionItemDto() {}

    public StockConsumptionItemDto(
            Long billItemId, Long billId, String deptId,
            Long referenceBillId, String referenceBillDeptId,
            Date createdAt, String itemName, String itemCode, String batchNo,
            double qty,
            double purchaseRate, double purchaseValue,
            double costRate, double costValue,
            double retailRate, double retailValue,
            String toDepartmentName, String comments, String measurementUnitName) {
        this.billItemId = billItemId;
        this.billId = billId;
        this.deptId = deptId;
        this.referenceBillId = referenceBillId;
        this.referenceBillDeptId = referenceBillDeptId;
        this.createdAt = createdAt;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.batchNo = batchNo;
        this.qty = qty;
        this.purchaseRate = purchaseRate;
        this.purchaseValue = purchaseValue;
        this.costRate = costRate;
        this.costValue = costValue;
        this.retailRate = retailRate;
        this.retailValue = retailValue;
        this.toDepartmentName = toDepartmentName;
        this.comments = comments;
        this.measurementUnitName = measurementUnitName;
    }

    public double getNetTotal() {
        return purchaseValue;
    }

    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }
    public Long getReferenceBillId() { return referenceBillId; }
    public void setReferenceBillId(Long referenceBillId) { this.referenceBillId = referenceBillId; }
    public String getReferenceBillDeptId() { return referenceBillDeptId; }
    public void setReferenceBillDeptId(String referenceBillDeptId) { this.referenceBillDeptId = referenceBillDeptId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public double getQty() { return qty; }
    public void setQty(double qty) { this.qty = qty; }
    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }
    public double getPurchaseValue() { return purchaseValue; }
    public void setPurchaseValue(double purchaseValue) { this.purchaseValue = purchaseValue; }
    public double getCostRate() { return costRate; }
    public void setCostRate(double costRate) { this.costRate = costRate; }
    public double getCostValue() { return costValue; }
    public void setCostValue(double costValue) { this.costValue = costValue; }
    public double getRetailRate() { return retailRate; }
    public void setRetailRate(double retailRate) { this.retailRate = retailRate; }
    public double getRetailValue() { return retailValue; }
    public void setRetailValue(double retailValue) { this.retailValue = retailValue; }
    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getMeasurementUnitName() { return measurementUnitName; }
    public void setMeasurementUnitName(String measurementUnitName) { this.measurementUnitName = measurementUnitName; }
}
