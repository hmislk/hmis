package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for Pharmacy Transfer Issue Bill Reports
 * Aggregates financial data directly from database to avoid iterative calculations
 */
public class PharmacyTransferIssueDTO implements Serializable {
    
    private Long billId;
    private String deptId;
    private Date createdAt;
    private String fromDepartmentName;
    private String toDepartmentName;
    private String transporterName;
    private Boolean cancelled;
    private Boolean refunded;
    private String cancelledBillDeptId;
    private String comments;
    private BigDecimal transferValue; // Sum of lineNetTotal from BillItemFinanceDetails 
    private BigDecimal saleValue;    // Sum of valueAtRetailRate from BillItemFinanceDetails
    
    // Default constructor
    public PharmacyTransferIssueDTO() {
    }
    
    // Constructor for direct JPQL query with aggregated values
    public PharmacyTransferIssueDTO(Long billId, String deptId, Date createdAt, 
                                    String fromDepartmentName, String toDepartmentName, 
                                    String transporterName, Boolean cancelled, Boolean refunded,
                                    String cancelledBillDeptId, String comments,
                                    BigDecimal transferValue, BigDecimal saleValue) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.fromDepartmentName = fromDepartmentName;
        this.toDepartmentName = toDepartmentName;
        this.transporterName = transporterName;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.cancelledBillDeptId = cancelledBillDeptId;
        this.comments = comments;
        this.transferValue = transferValue;
        this.saleValue = saleValue;
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
    
    // The XHTML expects this method name specifically - returns Double for display
    public Double getSaleValue() {
        return saleValue != null ? saleValue.doubleValue() : 0.0;
    }
    
    // Note: getFromDepartmentName(), getToDepartmentName(), getTransporterName() 
    // are already defined above in the regular getters section
    
    // For nested object access compatibility
    public FromDepartment getFromDepartment() {
        return new FromDepartment(fromDepartmentName);
    }
    
    public ToDepartment getToDepartment() {
        return new ToDepartment(toDepartmentName);
    }
    
    public ToStaff getToStaff() {
        return new ToStaff(transporterName);
    }
    
    public CancelledBill getCancelledBill() {
        return new CancelledBill(cancelledBillDeptId);
    }
    
    // Nested classes for XHTML compatibility
    public static class FromDepartment {
        private String name;
        public FromDepartment(String name) { this.name = name; }
        public String getName() { return name; }
    }
    
    public static class ToDepartment {
        private String name;
        public ToDepartment(String name) { this.name = name; }
        public String getName() { return name; }
    }
    
    public static class ToStaff {
        private String personNameWithTitle;
        public ToStaff(String personNameWithTitle) { this.personNameWithTitle = personNameWithTitle; }
        public Person getPerson() { return new Person(personNameWithTitle); }
        
        public static class Person {
            private String nameWithTitle;
            public Person(String nameWithTitle) { this.nameWithTitle = nameWithTitle; }
            public String getNameWithTitle() { return nameWithTitle; }
        }
    }
    
    public static class CancelledBill {
        private String deptId;
        public CancelledBill(String deptId) { this.deptId = deptId; }
        public String getDeptId() { return deptId; }
    }
}