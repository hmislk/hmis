package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for Pharmacy Transfer Receive Bill Reports
 * Aggregates financial data directly from database to avoid iterative calculations
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