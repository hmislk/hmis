package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Pharmacy Item-based Purchase Orders (PO) in item history. Keeps only
 * fields required by UI tables to improve performance.
 */
public class PharmacyItemPoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Bill header
    private Long billId;
    private String billDeptId;
    private Date billCreatedAt;

    // Party & user
    private String supplierName; // toInstitution.name
    private String creatorName;  // creater.webUserPerson.name

    // Item quantities
    private Double qty;         // Ordered quantity (PO)
    private Double freeQty;      // Ordered Free Qty

    public PharmacyItemPoDTO() {
    }

    public PharmacyItemPoDTO(Long billId,
            String billDeptId,
            Date billCreatedAt,
            String supplierName,
            String creatorName,
            Double qty,
            Double grnQty) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.supplierName = supplierName;
        this.creatorName = creatorName;
        this.qty = qty;
        this.freeQty = grnQty;
    }

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

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
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
}
