package com.divudi.data;

import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;

/**
 *
 * @author buddhika
 */
public class ReportTemplateRow {

    private String feeName;
    private String categoryName;
    private String toDepartmentName;
    private String itemName;
    private String paymentName;
    private Double rowValue;
    private Long rowCount;
    private Long id;

    private Category category;
    private ServiceType serviceType;
    private BillTypeAtomic billTypeAtomic;
    private Institution creditCompany;
    private Department toDepartment;

    public void setFeeName(String feeName) {
        this.feeName = feeName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getPaymentName() {
        return paymentName;
    }

    public void setPaymentName(String paymentName) {
        this.paymentName = paymentName;
    }

    public Double getRowValue() {
        return rowValue;
    }

    public void setRowValue(Double rowValue) {
        this.rowValue = rowValue;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public ReportTemplateRow(String feeName, String categoryName, String toDepartmentName, String itemName, String paymentName, Double rowValue, Long rowCount) {
        this.feeName = feeName;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.itemName = itemName;
        this.paymentName = paymentName;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }
    
    public ReportTemplateRow(String categoryName, Double rowValue) {
        this.categoryName = categoryName;
        this.rowValue = rowValue;
    }
    
    public ReportTemplateRow(BillTypeAtomic billTypeAtomic, String categoryName, String toDepartmentName, Double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.rowValue = rowValue;
    }

    public ReportTemplateRow(BillTypeAtomic billTypeAtomic, Double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.rowValue = rowValue;
    }

    
    public ReportTemplateRow(Double rowValue) {
        this.rowValue = rowValue;
    }

    public ReportTemplateRow() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    // Custom method to generate a unique key string, handling null values
    public String getCustomKey() {
        return (category != null ? category.getName() : "") + "|"
                + (creditCompany != null ? creditCompany.getName() : "") + "|"
                + (toDepartment != null ? toDepartment.getName() : "") + "|"
                + (serviceType != null ? serviceType.getLabel() : "") + "|"
                + (billTypeAtomic != null ? billTypeAtomic.getLabel() : "");
    }

    public String getFeeName() {
        return feeName;
    }

}
