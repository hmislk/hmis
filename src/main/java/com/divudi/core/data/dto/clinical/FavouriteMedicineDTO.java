/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.clinical;

import java.io.Serializable;

/**
 * DTO for Favourite Medicine by Age operations
 * Provides comprehensive representation of favourite medicine configurations
 * Used for API responses and data transfer
 *
 * @author Buddhika
 */
public class FavouriteMedicineDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Basic identification
    private Long id;
    private String itemName;
    private String itemType; // Vtm, Atm, Vmp, Amp
    private Long itemId;

    // Age range (in years for API, converted to/from days for database)
    private Double fromYears;
    private Double toYears;

    // Medicine details
    private String categoryName;
    private Long categoryId;

    // Dosage information
    private Double dose;
    private String doseUnitName;
    private Long doseUnitId;

    // Frequency and duration
    private String frequencyUnitName;
    private Long frequencyUnitId;
    private Double duration;
    private String durationUnitName;
    private Long durationUnitId;

    // Issue information
    private Double issue;
    private String issueUnitName;
    private Long issueUnitId;

    // Additional properties
    private Double orderNo;
    private boolean indoor;
    private String sex;

    // Audit information
    private String createdBy;
    private String createdAt;
    private String forItem; // The item this is a favourite for

    /**
     * Default constructor
     */
    public FavouriteMedicineDTO() {
    }

    /**
     * Constructor for JPQL queries - basic information
     */
    public FavouriteMedicineDTO(Long id, String itemName, String itemType,
                               Double fromYears, Double toYears, String categoryName,
                               Double dose, String doseUnitName, String frequencyUnitName,
                               Double duration, String durationUnitName, Double orderNo) {
        this.id = id;
        this.itemName = itemName;
        this.itemType = itemType;
        this.fromYears = fromYears;
        this.toYears = toYears;
        this.categoryName = categoryName;
        this.dose = dose;
        this.doseUnitName = doseUnitName;
        this.frequencyUnitName = frequencyUnitName;
        this.duration = duration;
        this.durationUnitName = durationUnitName;
        this.orderNo = orderNo;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Double getDose() {
        return dose;
    }

    public void setDose(Double dose) {
        this.dose = dose;
    }

    public String getDoseUnitName() {
        return doseUnitName;
    }

    public void setDoseUnitName(String doseUnitName) {
        this.doseUnitName = doseUnitName;
    }

    public Long getDoseUnitId() {
        return doseUnitId;
    }

    public void setDoseUnitId(Long doseUnitId) {
        this.doseUnitId = doseUnitId;
    }

    public String getFrequencyUnitName() {
        return frequencyUnitName;
    }

    public void setFrequencyUnitName(String frequencyUnitName) {
        this.frequencyUnitName = frequencyUnitName;
    }

    public Long getFrequencyUnitId() {
        return frequencyUnitId;
    }

    public void setFrequencyUnitId(Long frequencyUnitId) {
        this.frequencyUnitId = frequencyUnitId;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getDurationUnitName() {
        return durationUnitName;
    }

    public void setDurationUnitName(String durationUnitName) {
        this.durationUnitName = durationUnitName;
    }

    public Long getDurationUnitId() {
        return durationUnitId;
    }

    public void setDurationUnitId(Long durationUnitId) {
        this.durationUnitId = durationUnitId;
    }

    public Double getIssue() {
        return issue;
    }

    public void setIssue(Double issue) {
        this.issue = issue;
    }

    public String getIssueUnitName() {
        return issueUnitName;
    }

    public void setIssueUnitName(String issueUnitName) {
        this.issueUnitName = issueUnitName;
    }

    public Long getIssueUnitId() {
        return issueUnitId;
    }

    public void setIssueUnitId(Long issueUnitId) {
        this.issueUnitId = issueUnitId;
    }

    public Double getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Double orderNo) {
        this.orderNo = orderNo;
    }

    public boolean isIndoor() {
        return indoor;
    }

    public void setIndoor(boolean indoor) {
        this.indoor = indoor;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getForItem() {
        return forItem;
    }

    public void setForItem(String forItem) {
        this.forItem = forItem;
    }

    @Override
    public String toString() {
        return "FavouriteMedicineDTO{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", itemType='" + itemType + '\'' +
                ", fromYears=" + fromYears +
                ", toYears=" + toYears +
                ", categoryName='" + categoryName + '\'' +
                ", dose=" + dose +
                ", doseUnitName='" + doseUnitName + '\'' +
                ", frequencyUnitName='" + frequencyUnitName + '\'' +
                ", duration=" + duration +
                ", durationUnitName='" + durationUnitName + '\'' +
                '}';
    }
}