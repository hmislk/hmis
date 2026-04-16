/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.clinical;

import java.io.Serializable;

/**
 * DTO for updating favourite medicine configurations
 * Used for API requests to update existing favourite medicines
 * All fields are optional - only provided fields will be updated
 *
 * @author Buddhika
 */
public class FavouriteMedicineUpdateRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Age range updates
    private Double fromYears;           // New minimum age in years
    private Double toYears;             // New maximum age in years

    // Medicine details updates
    private String categoryName;        // New medicine category

    // Dosage information updates
    private Double dose;                // New dose amount
    private String doseUnitName;        // New dose unit

    // Frequency and duration updates
    private String frequencyUnitName;   // New frequency
    private Double duration;            // New duration amount
    private String durationUnitName;    // New duration unit

    // Optional fields updates
    private Double issue;               // New issue quantity
    private String issueUnitName;       // New issue unit
    private Boolean indoor;             // New indoor flag
    private String sex;                 // New sex restriction
    private Double orderNo;             // New display order

    /**
     * Default constructor
     */
    public FavouriteMedicineUpdateRequestDTO() {
    }

    // Getters and setters
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

    public Boolean getIndoor() {
        return indoor;
    }

    public void setIndoor(Boolean indoor) {
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

    /**
     * Validate age range if both values are provided
     */
    public boolean isValidAgeRange() {
        if (fromYears != null && toYears != null) {
            return fromYears >= 0 && toYears >= 0 && fromYears < toYears;
        }
        if (fromYears != null) {
            return fromYears >= 0;
        }
        if (toYears != null) {
            return toYears >= 0;
        }
        return true;
    }

    /**
     * Check if any field is provided for update
     */
    public boolean hasUpdates() {
        return fromYears != null ||
               toYears != null ||
               (categoryName != null && !categoryName.trim().isEmpty()) ||
               dose != null ||
               (doseUnitName != null && !doseUnitName.trim().isEmpty()) ||
               (frequencyUnitName != null && !frequencyUnitName.trim().isEmpty()) ||
               duration != null ||
               (durationUnitName != null && !durationUnitName.trim().isEmpty()) ||
               issue != null ||
               (issueUnitName != null && !issueUnitName.trim().isEmpty()) ||
               indoor != null ||
               (sex != null && !sex.trim().isEmpty()) ||
               orderNo != null;
    }

    @Override
    public String toString() {
        return "FavouriteMedicineUpdateRequestDTO{" +
                "fromYears=" + fromYears +
                ", toYears=" + toYears +
                ", categoryName='" + categoryName + '\'' +
                ", dose=" + dose +
                ", doseUnitName='" + doseUnitName + '\'' +
                ", frequencyUnitName='" + frequencyUnitName + '\'' +
                ", duration=" + duration +
                ", durationUnitName='" + durationUnitName + '\'' +
                ", indoor=" + indoor +
                ", sex='" + sex + '\'' +
                ", orderNo=" + orderNo +
                '}';
    }
}