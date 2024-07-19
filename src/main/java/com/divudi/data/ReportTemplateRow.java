package com.divudi.data;

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

    public String getFeeName() {
        return feeName;
    }

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

    public ReportTemplateRow() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    
    
}
