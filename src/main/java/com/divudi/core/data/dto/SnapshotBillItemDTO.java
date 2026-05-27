package com.divudi.core.data.dto;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for displaying pharmacy snapshot bill items on the
 * print and settle pages. Contains only the fields actually rendered —
 * no JPA entity lifecycle, no EAGER relationship triggers.
 */
public class SnapshotBillItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billItemId;
    private Double qty;
    private String itemName;
    private String categoryName;
    private Double netValue;
    private Double costRate;
    private Double purchaseRate;
    private Double retailRate;
    private Date expiryDate;
    private String batchNo;
    private String dosageForm;
    private DepartmentType departmentType;

    public SnapshotBillItemDTO() {
    }

    public SnapshotBillItemDTO(Long billItemId, Double qty, String itemName, String categoryName,
                                Double netValue, Double costRate, Double purchaseRate, Double retailRate,
                                Date expiryDate, String batchNo, String dosageForm) {
        this.billItemId = billItemId;
        this.qty = qty;
        this.itemName = itemName;
        this.categoryName = categoryName;
        this.netValue = netValue;
        this.costRate = costRate;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.expiryDate = expiryDate;
        this.batchNo = batchNo;
        this.dosageForm = dosageForm;
    }

    public SnapshotBillItemDTO(Long billItemId, Double qty, String itemName, String categoryName,
                                Double netValue, Double costRate, Double purchaseRate, Double retailRate,
                                Date expiryDate, String batchNo, String dosageForm, DepartmentType departmentType) {
        this(billItemId, qty, itemName, categoryName, netValue, costRate, purchaseRate, retailRate,
                expiryDate, batchNo, dosageForm);
        this.departmentType = departmentType;
    }

    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }

    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Double getNetValue() { return netValue; }
    public void setNetValue(Double netValue) { this.netValue = netValue; }

    public Double getCostRate() { return costRate != null ? costRate : 0.0; }
    public void setCostRate(Double costRate) { this.costRate = costRate; }

    public Double getPurchaseRate() { return purchaseRate != null ? purchaseRate : 0.0; }
    public void setPurchaseRate(Double purchaseRate) { this.purchaseRate = purchaseRate; }

    public Double getRetailRate() { return retailRate != null ? retailRate : 0.0; }
    public void setRetailRate(Double retailRate) { this.retailRate = retailRate; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public double getCostValue() { return getCostRate() * (qty != null ? qty : 0.0); }
    public double getPurchaseValue() { return getPurchaseRate() * (qty != null ? qty : 0.0); }
    public double getRetailValue() { return getRetailRate() * (qty != null ? qty : 0.0); }
}
