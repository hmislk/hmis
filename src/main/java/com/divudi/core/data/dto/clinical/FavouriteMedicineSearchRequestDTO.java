/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.clinical;

import java.io.Serializable;

/**
 * DTO for searching favourite medicine configurations
 * Used for API requests to search and filter favourite medicines
 * Supports various filtering criteria including age ranges, medicine types, and categories
 *
 * @author Buddhika
 */
public class FavouriteMedicineSearchRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Search filters
    private String query;               // Search by medicine name
    private String itemType;            // Filter by medicine type: "Vtm", "Atm", "Vmp", "Amp"
    private String categoryName;        // Filter by category name

    // Age range filters
    private Double ageYears;            // Find favourites for specific age
    private Double fromYears;           // Min age range
    private Double toYears;             // Max age range

    // Additional filters
    private String sex;                 // Filter by sex: "Male", "Female"
    private Boolean indoor;             // Filter by indoor/outdoor
    private String forItemName;         // Filter by the item this is a favourite for

    // Pagination and ordering
    private Integer limit;              // Limit results (default: no limit)
    private Integer offset;             // Skip results (default: 0)
    private String orderBy;             // Order by field: "orderNo", "itemName", "fromYears"
    private String orderDirection;      // Order direction: "ASC", "DESC" (default: ASC)

    /**
     * Default constructor
     */
    public FavouriteMedicineSearchRequestDTO() {
        this.limit = null;
        this.offset = 0;
        this.orderBy = "orderNo";
        this.orderDirection = "ASC";
    }

    // Getters and setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getAgeYears() {
        return ageYears;
    }

    public void setAgeYears(Double ageYears) {
        this.ageYears = ageYears;
    }

    public Double getFromYears() {
        return fromYears;
    }

    public void setFromYears(Double fromYears) {
        this.fromYears = fromYears;
    }

    public Double getToYears() {
        return toYears;
    }

    public void setToYears(Double toYears) {
        this.toYears = toYears;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Boolean getIndoor() {
        return indoor;
    }

    public void setIndoor(Boolean indoor) {
        this.indoor = indoor;
    }

    public String getForItemName() {
        return forItemName;
    }

    public void setForItemName(String forItemName) {
        this.forItemName = forItemName;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(String orderDirection) {
        this.orderDirection = orderDirection;
    }

    /**
     * Check if any search criteria are provided
     */
    public boolean hasFilters() {
        return (query != null && !query.trim().isEmpty()) ||
               (itemType != null && !itemType.trim().isEmpty()) ||
               (categoryName != null && !categoryName.trim().isEmpty()) ||
               ageYears != null ||
               (fromYears != null || toYears != null) ||
               (sex != null && !sex.trim().isEmpty()) ||
               indoor != null ||
               (forItemName != null && !forItemName.trim().isEmpty());
    }

    /**
     * Get valid order by fields
     */
    public static String[] getValidOrderByFields() {
        return new String[]{"orderNo", "itemName", "fromYears", "toYears", "categoryName"};
    }

    /**
     * Validate order direction
     */
    public boolean isValidOrderDirection() {
        return "ASC".equalsIgnoreCase(orderDirection) || "DESC".equalsIgnoreCase(orderDirection);
    }

    @Override
    public String toString() {
        return "FavouriteMedicineSearchRequestDTO{" +
                "query='" + query + '\'' +
                ", itemType='" + itemType + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", ageYears=" + ageYears +
                ", fromYears=" + fromYears +
                ", toYears=" + toYears +
                ", sex='" + sex + '\'' +
                ", indoor=" + indoor +
                ", limit=" + limit +
                ", offset=" + offset +
                ", orderBy='" + orderBy + '\'' +
                ", orderDirection='" + orderDirection + '\'' +
                '}';
    }
}