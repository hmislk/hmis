/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

/**
 * Lightweight search result DTO for TimedItem.
 *
 * @author Buddhika
 */
public class TimedItemSearchResultDTO {

    private Long id;
    private String name;
    private String code;
    private String printName;
    private String departmentType;
    private String inwardChargeType;
    private Double total;
    private Double totalForForeigner;
    private boolean inactive;
    /**
     * Always false unless the caller asked for retired rows with includeRetired=true —
     * the search excludes them otherwise. Exposed so an agent that retired the wrong
     * service can list what it retired and pick the id to restore (issue #23236 §2).
     */
    private boolean retired;
    private Long categoryId;
    private String categoryName;

    public TimedItemSearchResultDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPrintName() {
        return printName;
    }

    public void setPrintName(String printName) {
        this.printName = printName;
    }

    public String getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(String departmentType) {
        this.departmentType = departmentType;
    }

    public String getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(String inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getTotalForForeigner() {
        return totalForForeigner;
    }

    public void setTotalForForeigner(Double totalForForeigner) {
        this.totalForForeigner = totalForForeigner;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
