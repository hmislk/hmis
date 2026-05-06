package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Pharmacy Purchase Order list display optimization
 * @author Claude Code
 */
public class PharmacyPurchaseOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Basic bill information
    private Long billId;
    private String billNumber;
    private String deptId;
    private Date createdAt;
    private Double netTotal;
    private String paymentMethod;
    private Boolean cancelled;
    private Boolean consignment;

    // Creator information
    private String creatorName;
    private Long creatorId;

    // Supplier information
    private String supplierName;
    private Long supplierId;

    // Department information
    private String departmentName;
    private Long departmentId;

    // Reference bill (approval) information
    private Long referenceBillId;
    private String referenceBillDeptId;
    private Date referenceBillCreatedAt;
    private Double referenceBillNetTotal;
    private String referenceBillPaymentMethod;
    private String approverName;
    private Long approverId;
    private Boolean referenceBillCancelled;

    // Status information
    private Long checkedById;
    private String checkedByName;
    private Date checkedAt;

    // Cancellation information
    private Long cancelledBillId;
    private String cancellerName;
    private Date cancelledAt;

    // Bill status fields (for GRN page actions)
    private Boolean billClosed;
    private Boolean fullyIssued;

    // Default constructor required for JPA
    public PharmacyPurchaseOrderDTO() {
    }

    // Minimal constructor for testing (4 parameters)
    public PharmacyPurchaseOrderDTO(Long billId, String deptId, Date createdAt, Double netTotal) {
        this.billId = billId;
        this.deptId = deptId;
        this.billNumber = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
    }

    // Constructor for GRN PO listing page (11 parameters - without cancelledBill fields)
    // Used for pharmacy_purchase_order_list_for_recieve_dto.xhtml - optimized for approved POs awaiting GRN
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String deptId,
            Date createdAt,
            Double netTotal,
            String creatorName,
            String supplierName,
            String departmentName,
            Boolean consignment,
            Boolean cancelled,
            Boolean billClosed,
            Boolean fullyIssued) {
        this.billId = billId;
        this.billNumber = deptId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.creatorName = creatorName;
        this.supplierName = supplierName;
        this.departmentName = departmentName;
        this.consignment = consignment != null ? consignment : false;
        this.cancelled = cancelled != null ? cancelled : false;
        this.billClosed = billClosed != null ? billClosed : false;
        this.fullyIssued = fullyIssued != null ? fullyIssued : false;
    }

    // Constructor for basic bill information (for requested and approved search)
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String billNumber,
            String deptId,
            Date createdAt,
            Double netTotal,
            String paymentMethod,
            Boolean cancelled,
            Boolean consignment,
            String creatorName,
            String supplierName,
            String departmentName) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.cancelled = cancelled;
        this.consignment = consignment;
        this.creatorName = creatorName;
        this.supplierName = supplierName;
        this.departmentName = departmentName;
    }

    // Constructor for full information including approval details
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String billNumber,
            String deptId,
            Date createdAt,
            Double netTotal,
            String paymentMethod,
            Boolean cancelled,
            Boolean consignment,
            String creatorName,
            String supplierName,
            String departmentName,
            Long referenceBillId,
            String referenceBillDeptId,
            Date referenceBillCreatedAt,
            Double referenceBillNetTotal,
            String referenceBillPaymentMethod,
            String approverName,
            Boolean referenceBillCancelled,
            Long checkedById,
            String checkedByName) {
        this(billId, billNumber, deptId, createdAt, netTotal, paymentMethod, 
             cancelled, consignment, creatorName, supplierName, departmentName);
        this.referenceBillId = referenceBillId;
        this.referenceBillDeptId = referenceBillDeptId;
        this.referenceBillCreatedAt = referenceBillCreatedAt;
        this.referenceBillNetTotal = referenceBillNetTotal;
        this.referenceBillPaymentMethod = referenceBillPaymentMethod;
        this.approverName = approverName;
        this.referenceBillCancelled = referenceBillCancelled;
        this.checkedById = checkedById;
        this.checkedByName = checkedByName;
    }

    // Constructor for finalized purchase orders (without approval details)
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String billNumber,
            String deptId,
            Date createdAt,
            Double netTotal,
            String paymentMethod,
            Boolean cancelled,
            Boolean consignment,
            String creatorName,
            String supplierName,
            String departmentName,
            Long checkedById,
            String checkedByName) {
        this(billId, billNumber, deptId, createdAt, netTotal, paymentMethod, 
             cancelled, consignment, creatorName, supplierName, departmentName);
        this.checkedById = checkedById;
        this.checkedByName = checkedByName;
    }

    // Constructor for finalized purchase orders (12 parameters - JPQL compatible)
    // Note: JPQL returns enum as String, primitives as wrappers, nullable fields as Object
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String deptId,
            Date createdAt,
            Double netTotal,
            Object paymentMethod,
            Object cancelled,
            Object consignment,
            String creatorName,
            String supplierName,
            String departmentName,
            Long checkedById,
            String checkedByName) {
        this.billId = billId;
        this.billNumber = deptId; // Use deptId as billNumber for display
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.cancelled = cancelled != null ? (Boolean) cancelled : false;
        this.consignment = consignment != null ? (Boolean) consignment : false;
        this.creatorName = creatorName;
        this.supplierName = supplierName;
        this.departmentName = departmentName;
        this.checkedById = checkedById;
        this.checkedByName = checkedByName;
    }

    // Constructor for pending purchase orders (to finalize) - 12 parameters with Object types for JPQL compatibility
    // Used for fillOnlySavedPharmacyPo() where checkedBy fields will be null
    public PharmacyPurchaseOrderDTO(
            Long billId,
            Object deptId,
            Date createdAt,
            Object netTotal,
            Object paymentMethod,
            Object cancelled,
            Object consignment,
            Object creatorName,
            Object supplierName,
            Object departmentName,
            Object checkedById,
            Object checkedByName) {
        this.billId = billId;
        this.billNumber = deptId != null ? deptId.toString() : "";
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.netTotal = netTotal != null ? (Double) netTotal : 0.0;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.cancelled = cancelled != null ? (Boolean) cancelled : false;
        this.consignment = consignment != null ? (Boolean) consignment : false;
        this.creatorName = creatorName != null ? creatorName.toString() : "";
        this.supplierName = supplierName != null ? supplierName.toString() : "";
        this.departmentName = departmentName != null ? departmentName.toString() : "";
        this.checkedById = checkedById != null ? (Long) checkedById : null;
        this.checkedByName = checkedByName != null ? checkedByName.toString() : null;
    }

    // Constructor for pending purchase orders (to finalize) - 10 parameters without checkedBy fields
    // Used for fillOnlySavedPharmacyPo() JPQL query
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String deptId,
            Date createdAt,
            Double netTotal,
            Object paymentMethod,
            Object cancelled,
            Object consignment,
            String creatorName,
            String supplierName,
            String departmentName) {
        this.billId = billId;
        this.billNumber = deptId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.cancelled = cancelled != null ? (Boolean) cancelled : false;
        this.consignment = consignment != null ? (Boolean) consignment : false;
        this.creatorName = creatorName;
        this.supplierName = supplierName;
        this.departmentName = departmentName;
        this.checkedById = null; // Always null for pending orders
        this.checkedByName = null; // Always null for pending orders
    }

    // Constructor for requested and approved search (19 parameters - JPQL compatible)
    // Note: JPQL returns enum as String, primitives as wrappers, nullable fields as Object
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String deptId,
            Date createdAt,
            Double netTotal,
            Object paymentMethod,
            Object cancelled,
            Object consignment,
            String creatorName,
            String supplierName,
            String departmentName,
            Long referenceBillId,
            String referenceBillDeptId,
            Date referenceBillCreatedAt,
            Double referenceBillNetTotal,
            Object referenceBillPaymentMethod,
            String approverName,
            Object referenceBillCancelled,
            Long checkedById,
            String checkedByName) {
        this.billId = billId;
        this.billNumber = deptId; // Use deptId as billNumber for display
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.cancelled = cancelled != null ? (Boolean) cancelled : false;
        this.consignment = consignment != null ? (Boolean) consignment : false;
        this.creatorName = creatorName;
        this.supplierName = supplierName;
        this.departmentName = departmentName;
        this.referenceBillId = referenceBillId;
        this.referenceBillDeptId = referenceBillDeptId;
        this.referenceBillCreatedAt = referenceBillCreatedAt;
        this.referenceBillNetTotal = referenceBillNetTotal;
        this.referenceBillPaymentMethod = referenceBillPaymentMethod != null ? referenceBillPaymentMethod.toString() : null;
        this.approverName = approverName;
        this.referenceBillCancelled = referenceBillCancelled != null ? (Boolean) referenceBillCancelled : false;
        this.checkedById = checkedById;
        this.checkedByName = checkedByName;
    }

    // Constructor for GRN PO listing page (13 parameters - JPQL compatible)
    // Used for pharmacy_purchase_order_list_for_recieve_dto.xhtml - optimized for approved POs awaiting GRN
    // Note: Uses Boolean wrapper types for JPQL compatibility (primitives cannot be auto-boxed to Object in JPQL)
    public PharmacyPurchaseOrderDTO(
            Long billId,
            String deptId,
            Date createdAt,
            Double netTotal,
            String creatorName,
            String supplierName,
            String departmentName,
            Boolean consignment,
            Boolean cancelled,
            Boolean billClosed,
            Boolean fullyIssued,
            Date cancelledBillCreatedAt,
            String cancellerName) {
        this.billId = billId;
        this.billNumber = deptId; // Use deptId as billNumber for display
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.creatorName = creatorName;
        this.supplierName = supplierName;
        this.departmentName = departmentName;
        this.consignment = consignment != null ? consignment : false;
        this.cancelled = cancelled != null ? cancelled : false;
        this.billClosed = billClosed != null ? billClosed : false;
        this.fullyIssued = fullyIssued != null ? fullyIssued : false;
        this.cancelledAt = cancelledBillCreatedAt;
        this.cancellerName = cancellerName;
    }

    // Status check methods
    public boolean isApproved() {
        return referenceBillId != null && approverName != null;
    }

    public boolean isPending() {
        return checkedById != null && !isApproved();
    }

    public boolean isDraft() {
        return checkedById == null;
    }

    public boolean isReferenceBillExists() {
        return referenceBillId != null;
    }

    // Getters and Setters
    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
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

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Boolean getConsignment() {
        return consignment;
    }

    public void setConsignment(Boolean consignment) {
        this.consignment = consignment;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getReferenceBillId() {
        return referenceBillId;
    }

    public void setReferenceBillId(Long referenceBillId) {
        this.referenceBillId = referenceBillId;
    }

    public String getReferenceBillDeptId() {
        return referenceBillDeptId;
    }

    public void setReferenceBillDeptId(String referenceBillDeptId) {
        this.referenceBillDeptId = referenceBillDeptId;
    }

    public Date getReferenceBillCreatedAt() {
        return referenceBillCreatedAt;
    }

    public void setReferenceBillCreatedAt(Date referenceBillCreatedAt) {
        this.referenceBillCreatedAt = referenceBillCreatedAt;
    }

    public Double getReferenceBillNetTotal() {
        return referenceBillNetTotal;
    }

    public void setReferenceBillNetTotal(Double referenceBillNetTotal) {
        this.referenceBillNetTotal = referenceBillNetTotal;
    }

    public String getReferenceBillPaymentMethod() {
        return referenceBillPaymentMethod;
    }

    public void setReferenceBillPaymentMethod(String referenceBillPaymentMethod) {
        this.referenceBillPaymentMethod = referenceBillPaymentMethod;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public Boolean getReferenceBillCancelled() {
        return referenceBillCancelled;
    }

    public void setReferenceBillCancelled(Boolean referenceBillCancelled) {
        this.referenceBillCancelled = referenceBillCancelled;
    }

    public Long getCheckedById() {
        return checkedById;
    }

    public void setCheckedById(Long checkedById) {
        this.checkedById = checkedById;
    }

    public String getCheckedByName() {
        return checkedByName;
    }

    public void setCheckedByName(String checkedByName) {
        this.checkedByName = checkedByName;
    }

    public Date getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }

    public Long getCancelledBillId() {
        return cancelledBillId;
    }

    public void setCancelledBillId(Long cancelledBillId) {
        this.cancelledBillId = cancelledBillId;
    }

    public String getCancellerName() {
        return cancellerName;
    }

    public void setCancellerName(String cancellerName) {
        this.cancellerName = cancellerName;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Boolean getBillClosed() {
        return billClosed;
    }

    public void setBillClosed(Boolean billClosed) {
        this.billClosed = billClosed;
    }

    public Boolean getFullyIssued() {
        return fullyIssued;
    }

    public void setFullyIssued(Boolean fullyIssued) {
        this.fullyIssued = fullyIssued;
    }

    @Override
    public String toString() {
        return "PharmacyPurchaseOrderDTO{" +
                "billId=" + billId +
                ", billNumber='" + billNumber + '\'' +
                ", creatorName='" + creatorName + '\'' +
                ", supplierName='" + supplierName + '\'' +
                ", departmentName='" + departmentName + '\'' +
                ", netTotal=" + netTotal +
                ", approved=" + isApproved() +
                '}';
    }
}