package com.divudi.core.data.dto;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

public class StockLedgerDTO implements Serializable {

    private Long id;
    private String departmentName;
    private String categoryName;
    private String dosageFormName;
    private String itemCode;
    private String itemName;
    private String measurementUnitName;
    private Boolean transThisIsStockIn;
    private Boolean transThisIsStockOut;
    private String billDeptId;
    private Long billId;
    private String documentTypeLabel;
    private Date createdAt;
    private String fromDepartmentName;
    private String toDepartmentName;
    private String billDepartmentName;
    private Double qty;
    private Double freeQty;
    private Double purchaseRate;
    private Double retailRate;

    // Batch-level stock fields
    private Double stockQty;
    private Double instituionBatchQty;
    private Double totalBatchQty;
    private Double stockPurchaseValue;
    private Double stockSaleValue;
    private Double stockCostValue;
    private Double institutionBatchStockValueAtPurchaseRate;
    private Double institutionBatchStockValueAtSaleRate;
    private Double institutionBatchStockValueAtCostRate;
    private Double totalBatchStockValueAtPurchaseRate;
    private Double totalBatchStockValueAtSaleRate;
    private Double totalBatchStockValueAtCostRate;

    // Item-level stock fields
    private Double itemStock;
    private Double institutionItemStock;
    private Double totalItemStock;
    private Double itemStockValueAtPurchaseRate;
    private Double itemStockValueAtSaleRate;
    private Double itemStockValueAtCostRate;
    private Double institutionItemStockValueAtPurchaseRate;
    private Double institutionItemStockValueAtSaleRate;
    private Double institutionItemStockValueAtCostRate;
    private Double totalItemStockValueAtPurchaseRate;
    private Double totalItemStockValueAtSaleRate;
    private Double totalItemStockValueAtCostRate;

    // Batch-specific fields
    private String batchNo;
    private Date dateOfExpire;
    private Double batchCostRate;

    /**
     * Constructor for byBatch report type.
     * Includes batch-specific fields (batchNo, dateOfExpire, batchCostRate).
     * Note: transThisIsStockIn/Out are derived from qty+freeQty since they are
     * @Transient on PharmaceuticalBillItem and cannot be used in JPQL.
     */
    public StockLedgerDTO(
            Long id,
            String departmentName,
            String categoryName,
            String dosageFormName,
            String itemCode,
            String itemName,
            String measurementUnitName,
            String billDeptId,
            Long billId,
            BillTypeAtomic billTypeAtomic,
            BillType billType,
            Date createdAt,
            String fromDepartmentName,
            String toDepartmentName,
            String billDepartmentName,
            Double qty,
            Double freeQty,
            Double purchaseRate,
            Double retailRate,
            Double stockQty,
            Double instituionBatchQty,
            Double totalBatchQty,
            Double stockPurchaseValue,
            Double stockSaleValue,
            Double stockCostValue,
            Double institutionBatchStockValueAtPurchaseRate,
            Double institutionBatchStockValueAtSaleRate,
            Double institutionBatchStockValueAtCostRate,
            Double totalBatchStockValueAtPurchaseRate,
            Double totalBatchStockValueAtSaleRate,
            Double totalBatchStockValueAtCostRate,
            Double itemStock,
            Double institutionItemStock,
            Double totalItemStock,
            Double itemStockValueAtPurchaseRate,
            Double itemStockValueAtSaleRate,
            Double itemStockValueAtCostRate,
            Double institutionItemStockValueAtPurchaseRate,
            Double institutionItemStockValueAtSaleRate,
            Double institutionItemStockValueAtCostRate,
            Double totalItemStockValueAtPurchaseRate,
            Double totalItemStockValueAtSaleRate,
            Double totalItemStockValueAtCostRate,
            String batchNo,
            Date dateOfExpire,
            Double batchCostRate) {
        this.id = id;
        this.departmentName = departmentName;
        this.categoryName = categoryName;
        this.dosageFormName = dosageFormName;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.measurementUnitName = measurementUnitName;
        this.billDeptId = billDeptId;
        this.billId = billId;
        this.documentTypeLabel = billTypeAtomic != null ? billTypeAtomic.getLabel()
                : billType != null ? billType.getLabel() : "";
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.toDepartmentName = toDepartmentName;
        this.billDepartmentName = billDepartmentName;
        this.qty = qty;
        this.freeQty = freeQty;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        // Derive transThisIsStockIn/Out from qty+freeQty (matches PharmaceuticalBillItem logic)
        double totalQty = (qty != null ? qty : 0.0) + (freeQty != null ? freeQty : 0.0);
        this.transThisIsStockIn = totalQty > 0;
        this.transThisIsStockOut = totalQty < 0;
        this.stockQty = stockQty;
        this.instituionBatchQty = instituionBatchQty;
        this.totalBatchQty = totalBatchQty;
        this.stockPurchaseValue = stockPurchaseValue;
        this.stockSaleValue = stockSaleValue;
        this.stockCostValue = stockCostValue;
        this.institutionBatchStockValueAtPurchaseRate = institutionBatchStockValueAtPurchaseRate;
        this.institutionBatchStockValueAtSaleRate = institutionBatchStockValueAtSaleRate;
        this.institutionBatchStockValueAtCostRate = institutionBatchStockValueAtCostRate;
        this.totalBatchStockValueAtPurchaseRate = totalBatchStockValueAtPurchaseRate;
        this.totalBatchStockValueAtSaleRate = totalBatchStockValueAtSaleRate;
        this.totalBatchStockValueAtCostRate = totalBatchStockValueAtCostRate;
        this.itemStock = itemStock;
        this.institutionItemStock = institutionItemStock;
        this.totalItemStock = totalItemStock;
        this.itemStockValueAtPurchaseRate = itemStockValueAtPurchaseRate;
        this.itemStockValueAtSaleRate = itemStockValueAtSaleRate;
        this.itemStockValueAtCostRate = itemStockValueAtCostRate;
        this.institutionItemStockValueAtPurchaseRate = institutionItemStockValueAtPurchaseRate;
        this.institutionItemStockValueAtSaleRate = institutionItemStockValueAtSaleRate;
        this.institutionItemStockValueAtCostRate = institutionItemStockValueAtCostRate;
        this.totalItemStockValueAtPurchaseRate = totalItemStockValueAtPurchaseRate;
        this.totalItemStockValueAtSaleRate = totalItemStockValueAtSaleRate;
        this.totalItemStockValueAtCostRate = totalItemStockValueAtCostRate;
        this.batchNo = batchNo;
        this.dateOfExpire = dateOfExpire;
        this.batchCostRate = batchCostRate;
    }

    /**
     * Constructor for byItem report type.
     * Excludes batch-specific fields (batchNo, dateOfExpire, batchCostRate).
     * Note: transThisIsStockIn/Out are derived from qty+freeQty since they are
     * @Transient on PharmaceuticalBillItem and cannot be used in JPQL.
     */
    public StockLedgerDTO(
            Long id,
            String departmentName,
            String categoryName,
            String dosageFormName,
            String itemCode,
            String itemName,
            String measurementUnitName,
            String billDeptId,
            Long billId,
            BillTypeAtomic billTypeAtomic,
            BillType billType,
            Date createdAt,
            String fromDepartmentName,
            String toDepartmentName,
            String billDepartmentName,
            Double qty,
            Double freeQty,
            Double purchaseRate,
            Double retailRate,
            Double stockQty,
            Double instituionBatchQty,
            Double totalBatchQty,
            Double stockPurchaseValue,
            Double stockSaleValue,
            Double stockCostValue,
            Double institutionBatchStockValueAtPurchaseRate,
            Double institutionBatchStockValueAtSaleRate,
            Double institutionBatchStockValueAtCostRate,
            Double totalBatchStockValueAtPurchaseRate,
            Double totalBatchStockValueAtSaleRate,
            Double totalBatchStockValueAtCostRate,
            Double itemStock,
            Double institutionItemStock,
            Double totalItemStock,
            Double itemStockValueAtPurchaseRate,
            Double itemStockValueAtSaleRate,
            Double itemStockValueAtCostRate,
            Double institutionItemStockValueAtPurchaseRate,
            Double institutionItemStockValueAtSaleRate,
            Double institutionItemStockValueAtCostRate,
            Double totalItemStockValueAtPurchaseRate,
            Double totalItemStockValueAtSaleRate,
            Double totalItemStockValueAtCostRate) {
        this.id = id;
        this.departmentName = departmentName;
        this.categoryName = categoryName;
        this.dosageFormName = dosageFormName;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.measurementUnitName = measurementUnitName;
        this.billDeptId = billDeptId;
        this.billId = billId;
        this.documentTypeLabel = billTypeAtomic != null ? billTypeAtomic.getLabel()
                : billType != null ? billType.getLabel() : "";
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.toDepartmentName = toDepartmentName;
        this.billDepartmentName = billDepartmentName;
        this.qty = qty;
        this.freeQty = freeQty;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        // Derive transThisIsStockIn/Out from qty+freeQty (matches PharmaceuticalBillItem logic)
        double totalQty = (qty != null ? qty : 0.0) + (freeQty != null ? freeQty : 0.0);
        this.transThisIsStockIn = totalQty > 0;
        this.transThisIsStockOut = totalQty < 0;
        this.stockQty = stockQty;
        this.instituionBatchQty = instituionBatchQty;
        this.totalBatchQty = totalBatchQty;
        this.stockPurchaseValue = stockPurchaseValue;
        this.stockSaleValue = stockSaleValue;
        this.stockCostValue = stockCostValue;
        this.institutionBatchStockValueAtPurchaseRate = institutionBatchStockValueAtPurchaseRate;
        this.institutionBatchStockValueAtSaleRate = institutionBatchStockValueAtSaleRate;
        this.institutionBatchStockValueAtCostRate = institutionBatchStockValueAtCostRate;
        this.totalBatchStockValueAtPurchaseRate = totalBatchStockValueAtPurchaseRate;
        this.totalBatchStockValueAtSaleRate = totalBatchStockValueAtSaleRate;
        this.totalBatchStockValueAtCostRate = totalBatchStockValueAtCostRate;
        this.itemStock = itemStock;
        this.institutionItemStock = institutionItemStock;
        this.totalItemStock = totalItemStock;
        this.itemStockValueAtPurchaseRate = itemStockValueAtPurchaseRate;
        this.itemStockValueAtSaleRate = itemStockValueAtSaleRate;
        this.itemStockValueAtCostRate = itemStockValueAtCostRate;
        this.institutionItemStockValueAtPurchaseRate = institutionItemStockValueAtPurchaseRate;
        this.institutionItemStockValueAtSaleRate = institutionItemStockValueAtSaleRate;
        this.institutionItemStockValueAtCostRate = institutionItemStockValueAtCostRate;
        this.totalItemStockValueAtPurchaseRate = totalItemStockValueAtPurchaseRate;
        this.totalItemStockValueAtSaleRate = totalItemStockValueAtSaleRate;
        this.totalItemStockValueAtCostRate = totalItemStockValueAtCostRate;
    }

    // Derived helper methods

    public Double getStockInQty() {
        if (transThisIsStockIn != null && transThisIsStockIn) {
            double q = qty != null ? qty : 0.0;
            double f = freeQty != null ? freeQty : 0.0;
            return q + f;
        }
        return null;
    }

    public Double getStockOutQty() {
        if (transThisIsStockOut != null && transThisIsStockOut) {
            double q = qty != null ? qty : 0.0;
            double f = freeQty != null ? freeQty : 0.0;
            return q + f;
        }
        return null;
    }

    public String getToStoreOrBillDepartment() {
        return toDepartmentName != null ? toDepartmentName : billDepartmentName;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDosageFormName() {
        return dosageFormName;
    }

    public void setDosageFormName(String dosageFormName) {
        this.dosageFormName = dosageFormName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getMeasurementUnitName() {
        return measurementUnitName;
    }

    public void setMeasurementUnitName(String measurementUnitName) {
        this.measurementUnitName = measurementUnitName;
    }

    public Boolean getTransThisIsStockIn() {
        return transThisIsStockIn;
    }

    public void setTransThisIsStockIn(Boolean transThisIsStockIn) {
        this.transThisIsStockIn = transThisIsStockIn;
    }

    public Boolean getTransThisIsStockOut() {
        return transThisIsStockOut;
    }

    public void setTransThisIsStockOut(Boolean transThisIsStockOut) {
        this.transThisIsStockOut = transThisIsStockOut;
    }

    public String getBillDeptId() {
        return billDeptId;
    }

    public void setBillDeptId(String billDeptId) {
        this.billDeptId = billDeptId;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getDocumentTypeLabel() {
        return documentTypeLabel;
    }

    public void setDocumentTypeLabel(String documentTypeLabel) {
        this.documentTypeLabel = documentTypeLabel;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getBillDepartmentName() {
        return billDepartmentName;
    }

    public void setBillDepartmentName(String billDepartmentName) {
        this.billDepartmentName = billDepartmentName;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(Double freeQty) {
        this.freeQty = freeQty;
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

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Double getInstituionBatchQty() {
        return instituionBatchQty;
    }

    public void setInstituionBatchQty(Double instituionBatchQty) {
        this.instituionBatchQty = instituionBatchQty;
    }

    public Double getTotalBatchQty() {
        return totalBatchQty;
    }

    public void setTotalBatchQty(Double totalBatchQty) {
        this.totalBatchQty = totalBatchQty;
    }

    public Double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(Double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public Double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(Double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public Double getStockCostValue() {
        return stockCostValue;
    }

    public void setStockCostValue(Double stockCostValue) {
        this.stockCostValue = stockCostValue;
    }

    public Double getInstitutionBatchStockValueAtPurchaseRate() {
        return institutionBatchStockValueAtPurchaseRate;
    }

    public void setInstitutionBatchStockValueAtPurchaseRate(Double institutionBatchStockValueAtPurchaseRate) {
        this.institutionBatchStockValueAtPurchaseRate = institutionBatchStockValueAtPurchaseRate;
    }

    public Double getInstitutionBatchStockValueAtSaleRate() {
        return institutionBatchStockValueAtSaleRate;
    }

    public void setInstitutionBatchStockValueAtSaleRate(Double institutionBatchStockValueAtSaleRate) {
        this.institutionBatchStockValueAtSaleRate = institutionBatchStockValueAtSaleRate;
    }

    public Double getInstitutionBatchStockValueAtCostRate() {
        return institutionBatchStockValueAtCostRate;
    }

    public void setInstitutionBatchStockValueAtCostRate(Double institutionBatchStockValueAtCostRate) {
        this.institutionBatchStockValueAtCostRate = institutionBatchStockValueAtCostRate;
    }

    public Double getTotalBatchStockValueAtPurchaseRate() {
        return totalBatchStockValueAtPurchaseRate;
    }

    public void setTotalBatchStockValueAtPurchaseRate(Double totalBatchStockValueAtPurchaseRate) {
        this.totalBatchStockValueAtPurchaseRate = totalBatchStockValueAtPurchaseRate;
    }

    public Double getTotalBatchStockValueAtSaleRate() {
        return totalBatchStockValueAtSaleRate;
    }

    public void setTotalBatchStockValueAtSaleRate(Double totalBatchStockValueAtSaleRate) {
        this.totalBatchStockValueAtSaleRate = totalBatchStockValueAtSaleRate;
    }

    public Double getTotalBatchStockValueAtCostRate() {
        return totalBatchStockValueAtCostRate;
    }

    public void setTotalBatchStockValueAtCostRate(Double totalBatchStockValueAtCostRate) {
        this.totalBatchStockValueAtCostRate = totalBatchStockValueAtCostRate;
    }

    public Double getItemStock() {
        return itemStock;
    }

    public void setItemStock(Double itemStock) {
        this.itemStock = itemStock;
    }

    public Double getInstitutionItemStock() {
        return institutionItemStock;
    }

    public void setInstitutionItemStock(Double institutionItemStock) {
        this.institutionItemStock = institutionItemStock;
    }

    public Double getTotalItemStock() {
        return totalItemStock;
    }

    public void setTotalItemStock(Double totalItemStock) {
        this.totalItemStock = totalItemStock;
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

    public Double getItemStockValueAtCostRate() {
        return itemStockValueAtCostRate;
    }

    public void setItemStockValueAtCostRate(Double itemStockValueAtCostRate) {
        this.itemStockValueAtCostRate = itemStockValueAtCostRate;
    }

    public Double getInstitutionItemStockValueAtPurchaseRate() {
        return institutionItemStockValueAtPurchaseRate;
    }

    public void setInstitutionItemStockValueAtPurchaseRate(Double institutionItemStockValueAtPurchaseRate) {
        this.institutionItemStockValueAtPurchaseRate = institutionItemStockValueAtPurchaseRate;
    }

    public Double getInstitutionItemStockValueAtSaleRate() {
        return institutionItemStockValueAtSaleRate;
    }

    public void setInstitutionItemStockValueAtSaleRate(Double institutionItemStockValueAtSaleRate) {
        this.institutionItemStockValueAtSaleRate = institutionItemStockValueAtSaleRate;
    }

    public Double getInstitutionItemStockValueAtCostRate() {
        return institutionItemStockValueAtCostRate;
    }

    public void setInstitutionItemStockValueAtCostRate(Double institutionItemStockValueAtCostRate) {
        this.institutionItemStockValueAtCostRate = institutionItemStockValueAtCostRate;
    }

    public Double getTotalItemStockValueAtPurchaseRate() {
        return totalItemStockValueAtPurchaseRate;
    }

    public void setTotalItemStockValueAtPurchaseRate(Double totalItemStockValueAtPurchaseRate) {
        this.totalItemStockValueAtPurchaseRate = totalItemStockValueAtPurchaseRate;
    }

    public Double getTotalItemStockValueAtSaleRate() {
        return totalItemStockValueAtSaleRate;
    }

    public void setTotalItemStockValueAtSaleRate(Double totalItemStockValueAtSaleRate) {
        this.totalItemStockValueAtSaleRate = totalItemStockValueAtSaleRate;
    }

    public Double getTotalItemStockValueAtCostRate() {
        return totalItemStockValueAtCostRate;
    }

    public void setTotalItemStockValueAtCostRate(Double totalItemStockValueAtCostRate) {
        this.totalItemStockValueAtCostRate = totalItemStockValueAtCostRate;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }

    public Double getBatchCostRate() {
        return batchCostRate;
    }

    public void setBatchCostRate(Double batchCostRate) {
        this.batchCostRate = batchCostRate;
    }
}
