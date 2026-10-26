package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Flat row for the Stock Transfer report (Detail view and its exports),
 * loaded by a direct JPQL constructor query instead of BillItem entities.
 */
public class StockTransferDetailRowDTO implements Serializable {

    private Long billId;
    private String billDeptId;
    private Date billCreatedAt;
    private Long refBillId;
    private String refBillDeptId;
    private Date refBillCreatedAt;
    private String itemName;
    private String itemCode;
    private String categoryName;
    private String dosageFormName;
    private String strengthUnitName;
    private Double qty;
    private BigDecimal purchaseRate;
    private BigDecimal valueAtPurchaseRate;
    private BigDecimal costRate;
    private BigDecimal valueAtCostRate;
    private BigDecimal retailSaleRate;
    private BigDecimal valueAtRetailRate;
    private String createrName;
    private String toDepartmentName;

    public StockTransferDetailRowDTO() {
    }

    public StockTransferDetailRowDTO(Long billId, String billDeptId, Date billCreatedAt,
            Long refBillId, String refBillDeptId, Date refBillCreatedAt,
            String itemName, String itemCode, String categoryName, String dosageFormName, String strengthUnitName,
            Double qty,
            BigDecimal purchaseRate, BigDecimal valueAtPurchaseRate,
            BigDecimal costRate, BigDecimal valueAtCostRate,
            BigDecimal retailSaleRate, BigDecimal valueAtRetailRate,
            String createrName, String toDepartmentName) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.refBillId = refBillId;
        this.refBillDeptId = refBillDeptId;
        this.refBillCreatedAt = refBillCreatedAt;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.categoryName = categoryName;
        this.dosageFormName = dosageFormName;
        this.strengthUnitName = strengthUnitName;
        this.qty = qty;
        this.purchaseRate = purchaseRate;
        this.valueAtPurchaseRate = valueAtPurchaseRate;
        this.costRate = costRate;
        this.valueAtCostRate = valueAtCostRate;
        this.retailSaleRate = retailSaleRate;
        this.valueAtRetailRate = valueAtRetailRate;
        this.createrName = createrName;
        this.toDepartmentName = toDepartmentName;
    }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getBillDeptId() { return billDeptId; }
    public void setBillDeptId(String billDeptId) { this.billDeptId = billDeptId; }

    public Date getBillCreatedAt() { return billCreatedAt; }
    public void setBillCreatedAt(Date billCreatedAt) { this.billCreatedAt = billCreatedAt; }

    public Long getRefBillId() { return refBillId; }
    public void setRefBillId(Long refBillId) { this.refBillId = refBillId; }

    public String getRefBillDeptId() { return refBillDeptId; }
    public void setRefBillDeptId(String refBillDeptId) { this.refBillDeptId = refBillDeptId; }

    public Date getRefBillCreatedAt() { return refBillCreatedAt; }
    public void setRefBillCreatedAt(Date refBillCreatedAt) { this.refBillCreatedAt = refBillCreatedAt; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDosageFormName() { return dosageFormName; }
    public void setDosageFormName(String dosageFormName) { this.dosageFormName = dosageFormName; }

    public String getStrengthUnitName() { return strengthUnitName; }
    public void setStrengthUnitName(String strengthUnitName) { this.strengthUnitName = strengthUnitName; }

    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    public BigDecimal getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(BigDecimal purchaseRate) { this.purchaseRate = purchaseRate; }

    public BigDecimal getValueAtPurchaseRate() { return valueAtPurchaseRate; }
    public void setValueAtPurchaseRate(BigDecimal valueAtPurchaseRate) { this.valueAtPurchaseRate = valueAtPurchaseRate; }

    public BigDecimal getCostRate() { return costRate; }
    public void setCostRate(BigDecimal costRate) { this.costRate = costRate; }

    public BigDecimal getValueAtCostRate() { return valueAtCostRate; }
    public void setValueAtCostRate(BigDecimal valueAtCostRate) { this.valueAtCostRate = valueAtCostRate; }

    public BigDecimal getRetailSaleRate() { return retailSaleRate; }
    public void setRetailSaleRate(BigDecimal retailSaleRate) { this.retailSaleRate = retailSaleRate; }

    public BigDecimal getValueAtRetailRate() { return valueAtRetailRate; }
    public void setValueAtRetailRate(BigDecimal valueAtRetailRate) { this.valueAtRetailRate = valueAtRetailRate; }

    public String getCreaterName() { return createrName; }
    public void setCreaterName(String createrName) { this.createrName = createrName; }

    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }
}
