package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for Category Day End Report - optimized for performance
 * Used to replace entity-based queries that were causing timeouts
 *
 * @author buddhika.ari@gmail.com
 */
public class CategoryDayEndReportDto implements Serializable {

    private String categoryName;
    private String subCategoryName;
    private double cashValue;
    private double creditValue;
    private double totalValue;
    private String billType;
    private String paymentMethod;
    private long billCount;

    public CategoryDayEndReportDto() {
    }

    /**
     * Constructor for JPQL queries with aggregated data
     * Used in native/JPQL queries that aggregate bill/fee data by category
     *
     * @param categoryName Name of the category
     * @param subCategoryName Name of the subcategory (optional)
     * @param cashValue Total cash amount
     * @param creditValue Total credit amount
     * @param billCount Number of bills
     */
    public CategoryDayEndReportDto(String categoryName, String subCategoryName,
                                   double cashValue, double creditValue, long billCount) {
        this.categoryName = categoryName;
        this.subCategoryName = subCategoryName;
        this.cashValue = cashValue;
        this.creditValue = creditValue;
        this.totalValue = cashValue + creditValue;
        this.billCount = billCount;
    }

    /**
     * Constructor for simplified queries
     *
     * @param categoryName Name of the category
     * @param totalValue Total value (all payment methods combined)
     * @param billCount Number of bills
     */
    public CategoryDayEndReportDto(String categoryName, double totalValue, long billCount) {
        this.categoryName = categoryName;
        this.totalValue = totalValue;
        this.billCount = billCount;
    }

    /**
     * Constructor for detailed payment method breakdown
     *
     * @param categoryName Name of the category
     * @param billType Type of bill
     * @param paymentMethod Payment method used
     * @param totalValue Total value
     * @param billCount Number of bills
     */
    public CategoryDayEndReportDto(String categoryName, String billType,
                                   String paymentMethod, double totalValue, long billCount) {
        this.categoryName = categoryName;
        this.billType = billType;
        this.paymentMethod = paymentMethod;
        this.totalValue = totalValue;
        this.billCount = billCount;
    }

    // Getters and Setters
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public void setSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public double getCashValue() {
        return cashValue;
    }

    public void setCashValue(double cashValue) {
        this.cashValue = cashValue;
    }

    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getBillCount() {
        return billCount;
    }

    public void setBillCount(long billCount) {
        this.billCount = billCount;
    }
}
