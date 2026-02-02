/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.clinical;

import java.io.Serializable;

/**
 * DTO for creating favourite medicine configurations
 * Used for API requests to create new favourite medicines
 * Supports automatic entity creation and validation
 *
 * @author Buddhika
 */
public class FavouriteMedicineCreateRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Required fields
    private String itemName;            // Medicine name (VTM, ATM, VMP, or AMP)
    private String itemType;            // "Vtm", "Atm", "Vmp", "Amp"
    private Double fromYears;           // Minimum age in years
    private Double toYears;             // Maximum age in years

    // Medicine details
    private String categoryName;        // Medicine category (e.g., "suspension", "tablet")

    // Dosage information
    private Double dose;                // Dose amount
    private String doseUnitName;        // Dose unit (e.g., "ml", "mg")

    // Frequency and duration
    private String frequencyUnitName;   // Frequency (e.g., "8 hourly", "twice daily")
    private Double duration;            // Duration amount
    private String durationUnitName;    // Duration unit (e.g., "days", "weeks")

    // Optional fields
    private Double issue;               // Issue quantity
    private String issueUnitName;       // Issue unit
    private boolean indoor = false;     // For indoor patients only
    private String sex;                 // "Male", "Female", or null for both
    private Double orderNo;             // Display order (auto-generated if not provided)
    private String forItemName;         // The item this is a favourite for (optional)

    // Control flags
    private boolean createMissingEntities = false; // Auto-create missing entities

    /**
     * Default constructor
     */
    public FavouriteMedicineCreateRequestDTO() {
    }

    // Getters and setters
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

    public String getFrequencyUnitName() {
        return frequencyUnitName;
    }

    public void setFrequencyUnitName(String frequencyUnitName) {
        this.frequencyUnitName = frequencyUnitName;
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

    public Double getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Double orderNo) {
        this.orderNo = orderNo;
    }

    public String getForItemName() {
        return forItemName;
    }

    public void setForItemName(String forItemName) {
        this.forItemName = forItemName;
    }

    public boolean isCreateMissingEntities() {
        return createMissingEntities;
    }

    public void setCreateMissingEntities(boolean createMissingEntities) {
        this.createMissingEntities = createMissingEntities;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return itemName != null && !itemName.trim().isEmpty() &&
               itemType != null && !itemType.trim().isEmpty() &&
               fromYears != null && fromYears >= 0 &&
               toYears != null && toYears >= 0 &&
               fromYears < toYears;
    }

    @Override
    public String toString() {
        return "FavouriteMedicineCreateRequestDTO{" +
                "itemName='" + itemName + '\'' +
                ", itemType='" + itemType + '\'' +
                ", fromYears=" + fromYears +
                ", toYears=" + toYears +
                ", categoryName='" + categoryName + '\'' +
                ", dose=" + dose +
                ", doseUnitName='" + doseUnitName + '\'' +
                ", frequencyUnitName='" + frequencyUnitName + '\'' +
                ", duration=" + duration +
                ", durationUnitName='" + durationUnitName + '\'' +
                ", createMissingEntities=" + createMissingEntities +
                '}';
    }
}