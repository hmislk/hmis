package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for Pharmacy Transfer Receive Bill Reports
 * Aggregates financial data directly from database to avoid iterative calculations
 *
 * CRITICAL FIX for Issue #15797: Added billClassSimpleName, billedBillDeptId, and billedBillId
 * to distinguish cancelled receive bills and link them to original issue bills for proper reporting.
 */
public class PharmacyTransferReceiveDTO implements Serializable {

    private Long billId;
    private String deptId;
    private Date createdAt;
    private String departmentName; // Receiving department
    private String fromDepartmentName; // Department that sent the transfer
    private String transporterName;
    private Boolean cancelled;
    private Boolean refunded;
    private String cancelledBillDeptId;
    private String comments;
    private BigDecimal costValue;     // Sum of totalCostValue from BillFinanceDetails
    private BigDecimal purchaseValue; // Sum of totalPurchaseValue from BillFinanceDetails
    private BigDecimal transferValue; // Sum of lineNetTotal from BillItemFinanceDetails
    private BigDecimal saleValue;    // Sum of valueAtRetailRate from BillItemFinanceDetails

    // ADDED for Issue #15797: Fields to distinguish cancelled bills
    private String billClassSimpleName; // Simple class name (e.g., "Bill", "CancelledBill")
    private String billedBillDeptId;    // Original issue bill's deptId (for cancelled receives)
    private Long billedBillId;          // Original issue bill's id (for cancelled receives)
    
    // Default constructor
    public PharmacyTransferReceiveDTO() {
    }
    
    // Constructor for direct JPQL query with aggregated values
    public PharmacyTransferReceiveDTO(Long billId, String deptId, Date createdAt,
                                    String departmentName, String fromDepartmentName,
                                    String transporterName, Boolean cancelled, Boolean refunded,
                                    String comments, BigDecimal costValue, BigDecimal purchaseValue, BigDecimal saleValue) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.departmentName = departmentName;
        this.fromDepartmentName = fromDepartmentName;
        this.transporterName = transporterName;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.comments = comments;
        this.costValue = costValue;
        this.purchaseValue = purchaseValue;
        this.saleValue = saleValue;
    }

    // Constructor for JPQL queries with all financial values
    public PharmacyTransferReceiveDTO(Long billId, String deptId, Date createdAt,
                                    String departmentName, String fromDepartmentName,
                                    String transporterName, Boolean cancelled, Boolean refunded,
                                    String comments, BigDecimal costValue, BigDecimal purchaseValue,
                                    BigDecimal transferValue, BigDecimal saleValue) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.departmentName = departmentName;
        this.fromDepartmentName = fromDepartmentName;
        this.transporterName = transporterName;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.comments = comments;
        this.costValue = costValue;
        this.purchaseValue = purchaseValue;
        this.transferValue = transferValue;
        this.saleValue = saleValue;
    }

    // Constructor for JPQL queries with COALESCE (handles Object types from COALESCE)
    public PharmacyTransferReceiveDTO(Long billId, Object deptId, Date createdAt,
                                    Object departmentName, Object fromDepartmentName,
                                    Object transporterName, Object cancelled, Object refunded,
                                    Object comments, Object costValue, Object purchaseValue, Object transferValue, Object saleValue) {
        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.departmentName = departmentName != null ? departmentName.toString() : "";
        this.fromDepartmentName = fromDepartmentName != null ? fromDepartmentName.toString() : "";
        this.transporterName = transporterName != null ? transporterName.toString() : "";
        // Handle both Boolean and numeric (0/1) boolean values from database
        if (cancelled instanceof Boolean) {
            this.cancelled = (Boolean) cancelled;
        } else if (cancelled instanceof Number) {
            this.cancelled = ((Number) cancelled).intValue() != 0;
        } else {
            this.cancelled = false;
        }

        if (refunded instanceof Boolean) {
            this.refunded = (Boolean) refunded;
        } else if (refunded instanceof Number) {
            this.refunded = ((Number) refunded).intValue() != 0;
        } else {
            this.refunded = false;
        }
        this.comments = comments != null ? comments.toString() : "";

        // Handle BigDecimal conversion for financial values
        if (costValue instanceof BigDecimal) {
            this.costValue = (BigDecimal) costValue;
        } else if (costValue instanceof Double) {
            this.costValue = BigDecimal.valueOf((Double) costValue);
        } else {
            this.costValue = BigDecimal.ZERO;
        }

        if (purchaseValue instanceof BigDecimal) {
            this.purchaseValue = (BigDecimal) purchaseValue;
        } else if (purchaseValue instanceof Double) {
            this.purchaseValue = BigDecimal.valueOf((Double) purchaseValue);
        } else {
            this.purchaseValue = BigDecimal.ZERO;
        }

        if (transferValue instanceof BigDecimal) {
            this.transferValue = (BigDecimal) transferValue;
        } else if (transferValue instanceof Double) {
            this.transferValue = BigDecimal.valueOf((Double) transferValue);
        } else {
            this.transferValue = BigDecimal.ZERO;
        }

        if (saleValue instanceof BigDecimal) {
            this.saleValue = (BigDecimal) saleValue;
        } else if (saleValue instanceof Double) {
            this.saleValue = BigDecimal.valueOf((Double) saleValue);
        } else {
            this.saleValue = BigDecimal.ZERO;
        }
    }

    /**
     * NEW Constructor for Issue #15797: Includes TYPE(b) and billedBill fields
     * to distinguish cancelled receive bills in reports.
     *
     * @param billClass The bill class from TYPE(b) - used to identify CancelledBill
     * @param billId The bill ID
     * @param deptId The department bill number
     * @param createdAt The bill creation date
     * @param departmentName The receiving department name
     * @param fromDepartmentName The sending department name
     * @param transporterName The transporter/staff name
     * @param cancelled The cancelled flag
     * @param refunded The refunded flag
     * @param comments The bill comments (includes cancellation reason)
     * @param costValue The total cost value
     * @param purchaseValue The total purchase value
     * @param transferValue The line net total
     * @param saleValue The total retail sale value
     * @param billedBillDeptId The original issue bill's deptId (for cancelled receives)
     * @param billedBillId The original issue bill's id (for cancelled receives)
     */
    public PharmacyTransferReceiveDTO(Object billClass, Long billId, Object deptId, Date createdAt,
                                    Object departmentName, Object fromDepartmentName,
                                    Object transporterName, Object cancelled, Object refunded,
                                    Object comments, Object costValue, Object purchaseValue,
                                    Object transferValue, Object saleValue,
                                    Object billedBillDeptId, Object billedBillId) {
        // Extract simple class name from TYPE(b)
        this.billClassSimpleName = extractSimpleClassName(billClass);

        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.departmentName = departmentName != null ? departmentName.toString() : "";
        this.fromDepartmentName = fromDepartmentName != null ? fromDepartmentName.toString() : "";
        this.transporterName = transporterName != null ? transporterName.toString() : "";

        // Handle both Boolean and numeric (0/1) boolean values from database
        if (cancelled instanceof Boolean) {
            this.cancelled = (Boolean) cancelled;
        } else if (cancelled instanceof Number) {
            this.cancelled = ((Number) cancelled).intValue() != 0;
        } else {
            this.cancelled = false;
        }

        if (refunded instanceof Boolean) {
            this.refunded = (Boolean) refunded;
        } else if (refunded instanceof Number) {
            this.refunded = ((Number) refunded).intValue() != 0;
        } else {
            this.refunded = false;
        }
        this.comments = comments != null ? comments.toString() : "";

        // Handle BigDecimal conversion for financial values
        if (costValue instanceof BigDecimal) {
            this.costValue = (BigDecimal) costValue;
        } else if (costValue instanceof Double) {
            this.costValue = BigDecimal.valueOf((Double) costValue);
        } else {
            this.costValue = BigDecimal.ZERO;
        }

        if (purchaseValue instanceof BigDecimal) {
            this.purchaseValue = (BigDecimal) purchaseValue;
        } else if (purchaseValue instanceof Double) {
            this.purchaseValue = BigDecimal.valueOf((Double) purchaseValue);
        } else {
            this.purchaseValue = BigDecimal.ZERO;
        }

        if (transferValue instanceof BigDecimal) {
            this.transferValue = (BigDecimal) transferValue;
        } else if (transferValue instanceof Double) {
            this.transferValue = BigDecimal.valueOf((Double) transferValue);
        } else {
            this.transferValue = BigDecimal.ZERO;
        }

        if (saleValue instanceof BigDecimal) {
            this.saleValue = (BigDecimal) saleValue;
        } else if (saleValue instanceof Double) {
            this.saleValue = BigDecimal.valueOf((Double) saleValue);
        } else {
            this.saleValue = BigDecimal.ZERO;
        }

        // Handle billedBill fields (for cancelled receives linking to original issue)
        this.billedBillDeptId = billedBillDeptId != null ? billedBillDeptId.toString() : null;
        this.billedBillId = billedBillId instanceof Number ? ((Number) billedBillId).longValue() : null;
    }

    /**
     * Helper method to extract simple class name from TYPE(b) result.
     * TYPE(b) returns the fully qualified class name, we extract just the class name.
     *
     * @param billClass The class object from TYPE(b)
     * @return Simple class name (e.g., "Bill", "CancelledBill", "BilledBill")
     */
    private String extractSimpleClassName(Object billClass) {
        if (billClass == null) {
            return "Bill";
        }

        String fullClassName;
        if (billClass instanceof Class) {
            fullClassName = ((Class<?>) billClass).getSimpleName();
        } else {
            fullClassName = billClass.toString();
            // Extract simple name if it's a full class path
            if (fullClassName.contains(".")) {
                fullClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
            }
        }

        return fullClassName;
    }


    // Getters and Setters
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getTransporterName() {
        return transporterName;
    }

    public void setTransporterName(String transporterName) {
        this.transporterName = transporterName;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }

    public String getCancelledBillDeptId() {
        return cancelledBillDeptId;
    }

    public void setCancelledBillDeptId(String cancelledBillDeptId) {
        this.cancelledBillDeptId = cancelledBillDeptId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public BigDecimal getCostValue() {
        return costValue;
    }

    public void setCostValue(BigDecimal costValue) {
        this.costValue = costValue;
    }

    public BigDecimal getPurchaseValueBigDecimal() {
        return purchaseValue;
    }

    public void setPurchaseValue(BigDecimal purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public BigDecimal getTransferValue() {
        return transferValue;
    }

    public void setTransferValue(BigDecimal transferValue) {
        this.transferValue = transferValue;
    }

    public BigDecimal getSaleValueBigDecimal() {
        return saleValue;
    }

    public void setSaleValue(BigDecimal saleValue) {
        this.saleValue = saleValue;
    }

    // NEW Getters/Setters for Issue #15797 fields
    public String getBillClassSimpleName() {
        return billClassSimpleName;
    }

    public void setBillClassSimpleName(String billClassSimpleName) {
        this.billClassSimpleName = billClassSimpleName;
    }

    public String getBilledBillDeptId() {
        return billedBillDeptId;
    }

    public void setBilledBillDeptId(String billedBillDeptId) {
        this.billedBillDeptId = billedBillDeptId;
    }

    public Long getBilledBillId() {
        return billedBillId;
    }

    public void setBilledBillId(Long billedBillId) {
        this.billedBillId = billedBillId;
    }

    /**
     * Helper method to check if this is a cancelled receive bill.
     * Used in XHTML for conditional styling.
     */
    public boolean isCancelledReceive() {
        return "CancelledBill".equals(billClassSimpleName);
    }

    // Convenience methods for XHTML compatibility
    public Double getNetTotal() {
        return transferValue != null ? transferValue.doubleValue() : 0.0;
    }
    
    // Returns the cost value for "Cost Value" column - Double for XHTML display
    public Double getCostValueDouble() {
        return costValue != null ? costValue.doubleValue() : 0.0;
    }
    
    // IMPORTANT: This returns the actual purchase value for "Purchase Value" column
    // The purchaseValue field contains totalPurchaseValue from BillFinanceDetails
    public Double getPurchaseValue() {
        return purchaseValue != null ? purchaseValue.doubleValue() : 0.0;
    }
    
    // Returns the purchase value for "Purchase Value" column - Double for XHTML display
    public Double getPurchaseValueDouble() {
        return purchaseValue != null ? purchaseValue.doubleValue() : 0.0;
    }

    // Returns the transfer value for "Transfer Value" column - Double for XHTML display
    public Double getTransferValueDouble() {
        return transferValue != null ? transferValue.doubleValue() : 0.0;
    }

    // The XHTML expects this method name specifically - returns Double for display
    public Double getSaleValue() {
        return saleValue != null ? saleValue.doubleValue() : 0.0;
    }
    
    // For nested object access compatibility
    public Department getDepartment() {
        return new Department(departmentName);
    }
    
    public FromDepartment getFromDepartment() {
        return new FromDepartment(fromDepartmentName);
    }
    
    public FromStaff getFromStaff() {
        return new FromStaff(transporterName);
    }
    
    public CancelledBill getCancelledBill() {
        return new CancelledBill(cancelledBillDeptId);
    }
    
    // Nested classes for XHTML compatibility
    public static class Department {
        private String name;
        public Department(String name) { this.name = name; }
        public String getName() { return name; }
    }
    
    public static class FromDepartment {
        private String name;
        public FromDepartment(String name) { this.name = name; }
        public String getName() { return name; }
    }
    
    public static class FromStaff {
        private String personName;
        public FromStaff(String personName) { this.personName = personName; }
        public Person getPerson() { return new Person(personName); }
        
        public static class Person {
            private String name;
            public Person(String name) { this.name = name; }
            public String getName() { return name; }
            public String getNameWithTitle() { return name; } // For backward compatibility
        }
    }
    
    public static class CancelledBill {
        private String deptId;
        public CancelledBill(String deptId) { this.deptId = deptId; }
        public String getDeptId() { return deptId; }
    }
}