/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Pharmacy Return Without Trasing Bill-level report
 * Designed for JPQL projection to avoid N+1 query problems
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class PharmacyReturnWithoutTrasingBillDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Bill identification
    private Long billId;
    private String deptId;
    private String invoiceNumber;
    private Date createdAt;
    private Date billDate;

    // Entity references
    private String supplierName;        // toInstitution.name
    private Long supplierId;
    private String departmentName;
    private Long departmentId;
    private String creatorName;
    private String comments;

    // Financial summary
    private String paymentMethod;
    private Double grossTotal;
    private Double discount;
    private Double netTotal;

    // Aggregated valuation (from BillFinanceDetails)
    private Double totalCostValue;      // Sum negative stock impact
    private Double totalPurchaseValue;  // Sum negative stock impact
    private Double totalRetailValue;    // Sum negative stock impact

    // Default constructor
    public PharmacyReturnWithoutTrasingBillDTO() {
    }

    // JPQL constructor for projection
    public PharmacyReturnWithoutTrasingBillDTO(
            Long billId,
            String deptId,
            String invoiceNumber,
            Date createdAt,
            Date billDate,
            String supplierName,
            Long supplierId,
            String departmentName,
            Long departmentId,
            String creatorName,
            String comments,
            String paymentMethod,
            Double grossTotal,
            Double discount,
            Double netTotal,
            Double totalCostValue,
            Double totalPurchaseValue,
            Double totalRetailValue) {

        this.billId = billId;
        this.deptId = deptId;
        this.invoiceNumber = invoiceNumber;
        this.createdAt = createdAt;
        this.billDate = billDate;
        this.supplierName = supplierName;
        this.supplierId = supplierId;
        this.departmentName = departmentName;
        this.departmentId = departmentId;
        this.creatorName = creatorName;
        this.comments = comments;
        this.paymentMethod = paymentMethod;
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.totalCostValue = totalCostValue;
        this.totalPurchaseValue = totalPurchaseValue;
        this.totalRetailValue = totalRetailValue;
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
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

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(Double totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public Double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(Double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public Double getTotalRetailValue() {
        return totalRetailValue;
    }

    public void setTotalRetailValue(Double totalRetailValue) {
        this.totalRetailValue = totalRetailValue;
    }

    @Override
    public String toString() {
        return "PharmacyReturnWithoutTrasingBillDTO{"
                + "billId=" + billId
                + ", deptId=" + deptId
                + ", supplierName=" + supplierName
                + ", netTotal=" + netTotal + '}';
    }
}