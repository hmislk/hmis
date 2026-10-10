package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Pharmacy Analytics "Purchase Order Status" report (#24280).
 *
 * Filled by a JPQL constructor query over pharmacy purchase orders, joined to
 * their approval bill (the PO's referenceBill) when one exists.
 */
public class PurchaseOrderStatusRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billId;
    private String deptId;
    private Date createdAt;
    private String creatorName;
    private Date finalizedAt;
    private String finalizedByName;
    private String supplierName;
    private String departmentName;
    private Double netTotal;
    private String approvalDeptId;
    private Date approvedAt;
    private String approvedByName;

    public PurchaseOrderStatusRowDTO() {
    }

    public PurchaseOrderStatusRowDTO(
            Long billId,
            String deptId,
            Date createdAt,
            String creatorName,
            Date finalizedAt,
            String finalizedByName,
            String supplierName,
            String departmentName,
            Double netTotal,
            String approvalDeptId,
            Date approvedAt,
            String approvedByName) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.finalizedAt = finalizedAt;
        this.finalizedByName = finalizedByName;
        this.supplierName = supplierName;
        this.departmentName = departmentName;
        this.netTotal = netTotal;
        this.approvalDeptId = approvalDeptId;
        this.approvedAt = approvedAt;
        this.approvedByName = approvedByName;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
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

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Date getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(Date finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public String getFinalizedByName() {
        return finalizedByName;
    }

    public void setFinalizedByName(String finalizedByName) {
        this.finalizedByName = finalizedByName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public String getApprovalDeptId() {
        return approvalDeptId;
    }

    public void setApprovalDeptId(String approvalDeptId) {
        this.approvalDeptId = approvalDeptId;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }
}
