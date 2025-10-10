package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for Pharmacy Transfer Issue Bill Reports
 * Specifically designed for transfer issue bill reports to avoid conflicts with receipt reports
 * Aggregates financial data directly from database to avoid iterative calculations
 */
public class PharmacyTransferIssueBillDTO implements Serializable {
    
    private Long billId;
    private String deptId;
    private Date createdAt;
    private String billClassSimpleName; // Simple class name of Bill (e.g., CancelledBill)
    private String fromDepartmentName;
    private String toDepartmentName;
    private String transporterName;
    private Boolean cancelled;
    private Boolean refunded;
    private String comments;
    private BigDecimal costValue;     // Sum of totalCostValue from BillFinanceDetails
    private BigDecimal purchaseValue; // Sum of totalPurchaseValue from BillFinanceDetails
    private BigDecimal transferValue; // Sum of lineNetTotal from BillFinanceDetails 
    private BigDecimal saleValue;    // Sum of totalRetailSaleValue from BillFinanceDetails

    private String bhtNo;
    private String cancelledDeptId;
    private Double total;
    private Double margin;
    private Double discount;
    private Double netValue;

    // Default constructor
    public PharmacyTransferIssueBillDTO() {
    }

    public PharmacyTransferIssueBillDTO(Long billId, String deptId, Date createdAt, String bhtNo, String cancelledDeptId,
                                        Object purchaseValue, Double total, Double margin, Double discount, Double netValue) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.bhtNo = bhtNo;
        this.cancelledDeptId = cancelledDeptId;
        if (purchaseValue instanceof BigDecimal) {
            this.purchaseValue = (BigDecimal) purchaseValue;
        } else if (purchaseValue instanceof Double) {
            this.purchaseValue = BigDecimal.valueOf((Double) purchaseValue);
        } else {
            this.purchaseValue = BigDecimal.ZERO;
        }
        this.total = total;
        this.margin = margin;
        this.discount = discount;
        this.netValue = netValue;
    }

    // Constructor for JPQL queries with COALESCE (handles Object types from COALESCE)
    // Parameters match the exact order from ReportsTransfer.fillTransferIssueBillsDtoDirectly():
    // b.id, b.deptId, b.createdAt, b.department.name, b.toDepartment.name, p.name, 
    // b.cancelled, b.refunded, b.comments, bfd.totalCostValue, bfd.totalPurchaseValue, 
    // bfd.lineNetTotal, bfd.totalRetailSaleValue
    public PharmacyTransferIssueBillDTO(Long billId, Object deptId, Date createdAt, 
                                        Object fromDepartmentName, Object toDepartmentName, 
                                        Object transporterName, Object cancelled, Object refunded,
                                        Object comments, Object costValue, Object purchaseValue, 
                                        Object transferValue, Object saleValue) {
        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.billClassSimpleName = null; // not provided in this constructor
        this.fromDepartmentName = fromDepartmentName != null ? fromDepartmentName.toString() : "";
        this.toDepartmentName = toDepartmentName != null ? toDepartmentName.toString() : "";
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

    // New constructor including Bill class for row styling in XHTML
    // Parameters order matches JPQL in ReportsTransfer.fillTransferIssueBillsDtoDirectly():
    // TYPE(b), b.id, COALESCE(b.deptId,''), b.createdAt, COALESCE(b.department.name,''), COALESCE(b.toDepartment.name,''),
    // COALESCE(p.name,''), COALESCE(b.cancelled,false), COALESCE(b.refunded,false), COALESCE(b.comments,''),
    // COALESCE(bfd.totalCostValue,0.0), COALESCE(bfd.totalPurchaseValue,0.0), COALESCE(bfd.lineNetTotal,0.0), COALESCE(bfd.totalRetailSaleValue,0.0)
    public PharmacyTransferIssueBillDTO(Object billClass,
                                        Long billId, Object deptId, Date createdAt,
                                        Object fromDepartmentName, Object toDepartmentName,
                                        Object transporterName, Object cancelled, Object refunded,
                                        Object comments, Object costValue, Object purchaseValue,
                                        Object transferValue, Object saleValue) {
        this.billId = billId;
        this.deptId = deptId != null ? deptId.toString() : "";
        this.createdAt = createdAt;
        this.billClassSimpleName = extractSimpleClassName(billClass);
        this.fromDepartmentName = fromDepartmentName != null ? fromDepartmentName.toString() : "";
        this.toDepartmentName = toDepartmentName != null ? toDepartmentName.toString() : "";
        this.transporterName = transporterName != null ? transporterName.toString() : "";

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
    
    // Returns the transfer value for "Transfer Value" column - Double for XHTML display
    public Double getTransferValueDouble() {
        return transferValue != null ? transferValue.doubleValue() : 0.0;
    }
    
    // The XHTML expects this method name specifically - returns Double for display
    public Double getSaleValue() {
        return saleValue != null ? saleValue.doubleValue() : 0.0;
    }
    
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

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getCancelledDeptId() {
        return cancelledDeptId;
    }

    public void setCancelledDeptId(String cancelledDeptId) {
        this.cancelledDeptId = cancelledDeptId;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public String getBillClassSimpleName() {
        return billClassSimpleName;
    }

    private String extractSimpleClassName(Object billClass) {
        if (billClass == null) {
            return "";
        }
        if (billClass instanceof Class<?>) {
            return ((Class<?>) billClass).getSimpleName();
        }
        String s = billClass.toString();
        if (s.startsWith("class ")) {
            s = s.substring(6);
        }
        int lastDot = s.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < s.length() - 1) {
            return s.substring(lastDot + 1);
        }
        return s;
    }

    // Nested classes for XHTML compatibility
    public static class FromDepartment implements Serializable {
        private String name;
        public FromDepartment(String name) { this.name = name; }
        public String getName() { return name; }
    }
    
    public static class ToDepartment implements Serializable {
        private String name;
        public ToDepartment(String name) { this.name = name; }
        public String getName() { return name; }
    }
    
    public static class ToStaff implements Serializable {
        private String personName;
        public ToStaff(String personName) { this.personName = personName; }
        public Person getPerson() { return new Person(personName); }
        
        public static class Person implements Serializable {
            private String name;
            public Person(String name) { this.name = name; }
            public String getName() { return name; }
            public String getNameWithTitle() { return name; } // For backward compatibility
        }
    }
}
