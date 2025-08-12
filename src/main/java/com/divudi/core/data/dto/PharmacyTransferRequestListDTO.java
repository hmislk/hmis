package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lightweight DTO representing a pharmacy transfer request with minimal
 * information required for listing and navigation. Designed to be populated
 * directly from JPQL queries using {@code findLightsByJpql}.
 *
 * <p>Includes nested accessor classes to keep backward compatibility with
 * existing JSF expressions. New fields can be added without modifying existing
 * constructors.</p>
 */
public class PharmacyTransferRequestListDTO implements Serializable {

    private Long billId;
    private String deptId;
    private Date createdAt;
    private String fromDepartmentName;
    private String creatorName;
    private Boolean cancelled;
    private Date cancelledAt;
    private String cancellerName;
    private List<PharmacyTransferRequestIssueDTO> issuedBills = new ArrayList<>();

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    public PharmacyTransferRequestListDTO() {
    }

    public PharmacyTransferRequestListDTO(Long billId, String deptId, Date createdAt,
            String fromDepartmentName, String creatorName, Boolean cancelled,
            Date cancelledAt, String cancellerName) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.creatorName = creatorName;
        this.cancelled = cancelled;
        this.cancelledAt = cancelledAt;
        this.cancellerName = cancellerName;
    }

    // Constructor accepting generic Objects to allow flexibility with JPQL
    // projections that use COALESCE or future field additions.
    public PharmacyTransferRequestListDTO(Long billId, Object deptId, Date createdAt,
            Object fromDepartmentName, Object creatorName, Object cancelled,
            Date cancelledAt, Object cancellerName) {
        System.out.println("=== DTO Constructor called with: billId=" + billId + 
            ", deptId=" + deptId + ", cancelled=" + cancelled + 
            " (type: " + (cancelled != null ? cancelled.getClass().getSimpleName() : "null") + ") ===");
        
        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName != null ? fromDepartmentName.toString() : "";
        this.creatorName = creatorName != null ? creatorName.toString() : "";
        if (cancelled instanceof Boolean) {
            this.cancelled = (Boolean) cancelled;
        } else if (cancelled instanceof Number) {
            this.cancelled = ((Number) cancelled).intValue() != 0;
        } else {
            this.cancelled = false;
        }
        this.cancelledAt = cancelledAt;
        this.cancellerName = cancellerName != null ? cancellerName.toString() : "";
    }

    // Basic constructor without cancellation details
    public PharmacyTransferRequestListDTO(Long billId, Object deptId, Date createdAt,
            Object fromDepartmentName, Object creatorName) {
        System.out.println("=== Basic DTO Constructor called with: billId=" + billId + 
            ", deptId=" + deptId + ", creatorName=" + creatorName + " ===");
        
        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName != null ? fromDepartmentName.toString() : "";
        this.creatorName = creatorName != null ? creatorName.toString() : "";
        this.cancelled = false;  // Default to not cancelled
        this.cancelledAt = null;
        this.cancellerName = "";
    }

    // ------------------------------------------------------------------
    // Getters & Setters
    // ------------------------------------------------------------------

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

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellerName() {
        return cancellerName;
    }

    public void setCancellerName(String cancellerName) {
        this.cancellerName = cancellerName;
    }

    public List<PharmacyTransferRequestIssueDTO> getIssuedBills() {
        return issuedBills;
    }

    public void setIssuedBills(List<PharmacyTransferRequestIssueDTO> issuedBills) {
        this.issuedBills = issuedBills;
    }

    // ------------------------------------------------------------------
    // Nested accessors for backwards compatibility with existing JSF
    // expressions such as p.department.name or p.creater.webUserPerson.name
    // ------------------------------------------------------------------

    public Department getDepartment() {
        return new Department(fromDepartmentName);
    }

    public Creater getCreater() {
        return new Creater(creatorName);
    }

    public CancelledBill getCancelledBill() {
        return new CancelledBill(cancelledAt, cancellerName);
    }

    // Nested classes ----------------------------------------------------

    public static class Department {
        private String name;

        public Department(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class Creater {
        private WebUserPerson webUserPerson;

        public Creater(String name) {
            this.webUserPerson = new WebUserPerson(name);
        }

        public WebUserPerson getWebUserPerson() {
            return webUserPerson;
        }
    }

    public static class WebUserPerson {
        private String name;

        public WebUserPerson(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class CancelledBill {
        private Date createdAt;
        private Creater creater;

        public CancelledBill(Date createdAt, String cancellerName) {
            this.createdAt = createdAt;
            this.creater = new Creater(cancellerName);
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Creater getCreater() {
            return creater;
        }
    }
}
