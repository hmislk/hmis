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
    private Boolean completed;
    private Boolean fullyIssued;
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
        this.completed = false; // Default to not completed
    }

    /**
     * Constructor matching JPQL query without cancelled field.
     * Used when cancelled status is determined by filter conditions.
     */
    public PharmacyTransferRequestListDTO(Long billId, String deptId, Date createdAt,
            String fromDepartmentName, String creatorName,
            Date cancelledAt, String cancellerName) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.creatorName = creatorName;
        this.cancelled = false; // Set to false since query filters out cancelled bills
        this.cancelledAt = cancelledAt;
        this.cancellerName = cancellerName;
        this.completed = false; // Default to not completed
    }

    /**
     * Simplified constructor for non-cancelled bills only.
     * Used when query filters out all cancelled bills.
     */
    public PharmacyTransferRequestListDTO(Long billId, String deptId, Date createdAt,
            String fromDepartmentName, String creatorName) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.creatorName = creatorName;
        this.cancelled = false; // Query filters out cancelled bills
        this.cancelledAt = null;
        this.cancellerName = null;
        this.completed = false; // Default to not completed
    }

    /**
     * Constructor including completed status for non-cancelled bills.
     * Used for performance optimization to include completed flag.
     */
    public PharmacyTransferRequestListDTO(Long billId, String deptId, Date createdAt,
            String fromDepartmentName, String creatorName, Boolean completed) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.creatorName = creatorName;
        this.cancelled = false; // Query filters out cancelled bills
        this.cancelledAt = null;
        this.cancellerName = null;
        this.completed = completed;
        this.fullyIssued = false; // Default to not fully issued
    }

    /**
     * Constructor including both completed and fullyIssued status for non-cancelled bills.
     * Used for performance optimization to include both flags.
     */
    public PharmacyTransferRequestListDTO(Long billId, String deptId, Date createdAt,
            String fromDepartmentName, String creatorName, Boolean completed, Boolean fullyIssued) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.creatorName = creatorName;
        this.cancelled = false; // Query filters out cancelled bills
        this.cancelledAt = null;
        this.cancellerName = null;
        this.completed = completed;
        this.fullyIssued = fullyIssued;
    }

    // Basic constructor without cancellation details
    public PharmacyTransferRequestListDTO(Long billId, Object deptId, Date createdAt,
            Object fromDepartmentName, Object creatorName) {
        
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

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getFullyIssued() {
        return fullyIssued;
    }

    public void setFullyIssued(Boolean fullyIssued) {
        this.fullyIssued = fullyIssued;
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
