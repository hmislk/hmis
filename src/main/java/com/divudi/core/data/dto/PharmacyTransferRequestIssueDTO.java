package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO representing issued bills for a pharmacy transfer request. Contains only
 * the minimal fields required for listing and navigation within JSF views. Also
 * exposes nested getter classes for backward compatibility with existing
 * expressions.
 */
public class PharmacyTransferRequestIssueDTO implements Serializable {

    private Long billId;
    private String deptId;
    private Date createdAt;
    private String creatorName;
    private Boolean cancelled;
    private Date cancelledAt;
    private String cancellerName;
    private String toStaffName;
    private BigDecimal netTotal;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    public PharmacyTransferRequestIssueDTO() {
    }

    public PharmacyTransferRequestIssueDTO(Long billId, String deptId, Date createdAt,
            String creatorName, Boolean cancelled, Date cancelledAt,
            String cancellerName, String toStaffName, BigDecimal netTotal) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.cancelled = cancelled;
        this.cancelledAt = cancelledAt;
        this.cancellerName = cancellerName;
        this.toStaffName = toStaffName;
        this.netTotal = netTotal;
    }

    /**
     * Constructor matching JPQL query without cancelled field.
     * Used when cancelled status is not directly queried.
     */
    public PharmacyTransferRequestIssueDTO(Long billId, String deptId, Date createdAt,
            String creatorName, Date cancelledAt, String cancellerName,
            String toStaffName, BigDecimal netTotal) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.cancelled = (cancelledAt != null); // Derive cancelled status from cancelledAt
        this.cancelledAt = cancelledAt;
        this.cancellerName = cancellerName;
        this.toStaffName = toStaffName;
        this.netTotal = netTotal;
    }

    /**
     * Simplified constructor for non-cancelled bills only.
     * Used when query filters out all cancelled bills.
     * JPA provides double for BigDecimal fields in constructor expressions.
     */
    public PharmacyTransferRequestIssueDTO(Long billId, String deptId, Date createdAt,
            String creatorName, String toStaffName, double netTotal) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.cancelled = false; // Query filters out cancelled bills
        this.cancelledAt = null;
        this.cancellerName = null;
        this.toStaffName = toStaffName;
        this.netTotal = BigDecimal.valueOf(netTotal);
    }

    // Flexible constructor accepting Objects
    public PharmacyTransferRequestIssueDTO(Long billId, Object deptId, Date createdAt,
            Object creatorName, Object cancelled, Date cancelledAt,
            Object cancellerName, Object toStaffName, Object netTotal) {
        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
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
        this.toStaffName = toStaffName != null ? toStaffName.toString() : "";
        if (netTotal instanceof BigDecimal) {
            this.netTotal = (BigDecimal) netTotal;
        } else if (netTotal instanceof Number) {
            this.netTotal = BigDecimal.valueOf(((Number) netTotal).doubleValue());
        } else {
            this.netTotal = BigDecimal.ZERO;
        }
    }

    // Simple constructor without cancellation details and toStaff
    public PharmacyTransferRequestIssueDTO(Long billId, Object deptId, Date createdAt,
            Object creatorName, Object netTotal) {
        System.out.println("=== Basic Issue DTO Constructor called with: billId=" + billId + 
            ", deptId=" + deptId + ", creatorName=" + creatorName + " ===");
        
        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.creatorName = creatorName != null ? creatorName.toString() : "";
        this.cancelled = false;  // Default to not cancelled
        this.cancelledAt = null;
        this.cancellerName = "";
        this.toStaffName = "";  // No toStaff data available
        if (netTotal instanceof BigDecimal) {
            this.netTotal = (BigDecimal) netTotal;
        } else if (netTotal instanceof Number) {
            this.netTotal = BigDecimal.valueOf(((Number) netTotal).doubleValue());
        } else {
            this.netTotal = BigDecimal.ZERO;
        }
    }

    // ---------------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------------

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

    public String getToStaffName() {
        return toStaffName;
    }

    public void setToStaffName(String toStaffName) {
        this.toStaffName = toStaffName;
    }

    public BigDecimal getNetTotalBigDecimal() {
        return netTotal;
    }

    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }

    // Convenience getter for JSF number formatting
    public Double getNetTotal() {
        return netTotal != null ? netTotal.doubleValue() : 0.0;
    }

    // ---------------------------------------------------------------
    // Nested accessors for backwards compatibility
    // ---------------------------------------------------------------

    public Creater getCreater() {
        return new Creater(creatorName);
    }

    public CancelledBill getCancelledBill() {
        return new CancelledBill(cancelledAt, cancellerName);
    }

    public ToStaff getToStaff() {
        return new ToStaff(toStaffName);
    }

    // Nested classes --------------------------------------------------

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

    public static class ToStaff {
        private Person person;

        public ToStaff(String name) {
            this.person = new Person(name);
        }

        public Person getPerson() {
            return person;
        }

        public static class Person {
            private String name;

            public Person(String name) {
                this.name = name;
            }

            public String getNameWithTitle() {
                return name;
            }

            public String getName() {
                return name;
            }
        }
    }
}
