/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

/**
 * DTO for updating an existing TimedItemFee. All fields optional — only non-null/non-sentinel values are applied.
 *
 * @author Buddhika
 */
public class TimedItemFeeUpdateRequestDTO {

    private String name;
    private Double fee;
    private Double ffee;
    private Double durationHours;
    private Double overShootHours;
    private Long durationDaysForMoCharge;
    private Integer sortOrder;
    private Boolean repeating;

    public TimedItemFeeUpdateRequestDTO() {
    }

    public boolean isValid() {
        boolean hasValidName = name != null && !name.trim().isEmpty();
        return hasValidName || fee != null || ffee != null || durationHours != null
                || overShootHours != null || durationDaysForMoCharge != null
                || sortOrder != null || repeating != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }

    public Double getFfee() {
        return ffee;
    }

    public void setFfee(Double ffee) {
        this.ffee = ffee;
    }

    public Double getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Double durationHours) {
        this.durationHours = durationHours;
    }

    public Double getOverShootHours() {
        return overShootHours;
    }

    public void setOverShootHours(Double overShootHours) {
        this.overShootHours = overShootHours;
    }

    public Long getDurationDaysForMoCharge() {
        return durationDaysForMoCharge;
    }

    public void setDurationDaysForMoCharge(Long durationDaysForMoCharge) {
        this.durationDaysForMoCharge = durationDaysForMoCharge;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getRepeating() {
        return repeating;
    }

    public void setRepeating(Boolean repeating) {
        this.repeating = repeating;
    }
}
