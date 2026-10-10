/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.pharmacy;

import com.divudi.core.entity.Bill;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 * Frozen snapshot of one {@code TransferIssueItemRowDto} row selected by the user when a
 * native-SQL Fast Issue draft (PHARMACY_ISSUE_PRE) is saved via
 * {@code TransferIssueNativeSqlController.saveDraftNativeIssue()}.
 *
 * The native Fast Issue draft persists only the Bill header at Save time — no BillItem rows
 * exist until settlement. Without this snapshot, reopening the draft later (a different
 * session Finalizing or Approving it) had to recompute item rows and quantities from the
 * request's *current* remaining quantities, silently discarding whatever the user actually
 * selected/entered at Save time. Every field here mirrors a field
 * {@code TransferIssueNativeSqlService.settle(...)} reads off the DTO to write BillItem,
 * PharmaceuticalBillItem, StockHistory and finance-detail rows, so restoring these rows
 * exactly reproduces the DTO that was on screen when the draft was saved (#23608).
 */
@Entity
public class PharmacyTransferIssueDraftItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Bill draftBill;

    private int serialNo;
    private String itemName;
    private String itemCode;
    private String batchNo;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date dateOfExpire;

    private Long requestedBillItemId;
    private Long itemId;
    private Long ampItemId;
    private String itemDtype;
    private String departmentType;
    private double unitsPerPack = 1.0;

    private Long deptStockId;
    private Long itemBatchId;
    private double availableStock;

    private double requestedQty;
    private double alreadyIssuedQty;
    private double remainingQty;

    /** decimal(18,4), matching this project's other rate/qty BigDecimal columns (e.g. BillItemFinanceDetails) — the JPA default of decimal(38,0) truncates all fractional quantities. */
    @Column(precision = 18, scale = 4)
    private BigDecimal issuingQty;

    @Column(precision = 18, scale = 4)
    private BigDecimal grossRate;
    private double lineTotal;

    private double purchaseRate;
    private double retailRate;
    private double wholesaleRate;
    private double costRate;

    private double batchRetailRate;
    private double batchPurchaseRate;
    private double batchWholesaleRate;
    private Double batchCostRate;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bill getDraftBill() {
        return draftBill;
    }

    public void setDraftBill(Bill draftBill) {
        this.draftBill = draftBill;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
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

    public Long getRequestedBillItemId() {
        return requestedBillItemId;
    }

    public void setRequestedBillItemId(Long requestedBillItemId) {
        this.requestedBillItemId = requestedBillItemId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAmpItemId() {
        return ampItemId;
    }

    public void setAmpItemId(Long ampItemId) {
        this.ampItemId = ampItemId;
    }

    public String getItemDtype() {
        return itemDtype;
    }

    public void setItemDtype(String itemDtype) {
        this.itemDtype = itemDtype;
    }

    public String getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(String departmentType) {
        this.departmentType = departmentType;
    }

    public double getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(double unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public Long getDeptStockId() {
        return deptStockId;
    }

    public void setDeptStockId(Long deptStockId) {
        this.deptStockId = deptStockId;
    }

    public Long getItemBatchId() {
        return itemBatchId;
    }

    public void setItemBatchId(Long itemBatchId) {
        this.itemBatchId = itemBatchId;
    }

    public double getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(double availableStock) {
        this.availableStock = availableStock;
    }

    public double getRequestedQty() {
        return requestedQty;
    }

    public void setRequestedQty(double requestedQty) {
        this.requestedQty = requestedQty;
    }

    public double getAlreadyIssuedQty() {
        return alreadyIssuedQty;
    }

    public void setAlreadyIssuedQty(double alreadyIssuedQty) {
        this.alreadyIssuedQty = alreadyIssuedQty;
    }

    public double getRemainingQty() {
        return remainingQty;
    }

    public void setRemainingQty(double remainingQty) {
        this.remainingQty = remainingQty;
    }

    public BigDecimal getIssuingQty() {
        return issuingQty;
    }

    public void setIssuingQty(BigDecimal issuingQty) {
        this.issuingQty = issuingQty;
    }

    public BigDecimal getGrossRate() {
        return grossRate;
    }

    public void setGrossRate(BigDecimal grossRate) {
        this.grossRate = grossRate;
    }

    public double getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(double lineTotal) {
        this.lineTotal = lineTotal;
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
        this.retailRate = retailRate;
    }

    public double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
    }

    public double getBatchRetailRate() {
        return batchRetailRate;
    }

    public void setBatchRetailRate(double batchRetailRate) {
        this.batchRetailRate = batchRetailRate;
    }

    public double getBatchPurchaseRate() {
        return batchPurchaseRate;
    }

    public void setBatchPurchaseRate(double batchPurchaseRate) {
        this.batchPurchaseRate = batchPurchaseRate;
    }

    public double getBatchWholesaleRate() {
        return batchWholesaleRate;
    }

    public void setBatchWholesaleRate(double batchWholesaleRate) {
        this.batchWholesaleRate = batchWholesaleRate;
    }

    public Double getBatchCostRate() {
        return batchCostRate;
    }

    public void setBatchCostRate(Double batchCostRate) {
        this.batchCostRate = batchCostRate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PharmacyTransferIssueDraftItem)) {
            return false;
        }
        PharmacyTransferIssueDraftItem other = (PharmacyTransferIssueDraftItem) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.pharmacy.PharmacyTransferIssueDraftItem[ id=" + id + " ]";
    }
}
