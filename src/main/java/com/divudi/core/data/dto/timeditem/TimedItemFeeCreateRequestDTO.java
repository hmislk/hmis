/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

import com.divudi.core.data.inward.TimedItemDurationUnit;

/**
 * DTO for adding a fee to a TimedItem.
 *
 * @author Buddhika
 */
public class TimedItemFeeCreateRequestDTO {

    private String name; // required
    private double fee; // required, >= 0
    private double ffee; // optional, defaults to fee if 0
    private double durationHours; // required for tiered billing, except for ONE_TIME
    private double overShootHours;
    private long durationDaysForMoCharge;
    private int sortOrder;
    private boolean repeating;
    private TimedItemDurationUnit durationUnit; // optional, defaults to HOUR

    public TimedItemFeeCreateRequestDTO() {
    }

    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
                && (durationHours > 0 || durationUnit == TimedItemDurationUnit.ONE_TIME)
                && fee >= 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public double getFfee() {
        return ffee;
    }

    public void setFfee(double ffee) {
        this.ffee = ffee;
    }

    public double getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(double durationHours) {
        this.durationHours = durationHours;
    }

    public double getOverShootHours() {
        return overShootHours;
    }

    public void setOverShootHours(double overShootHours) {
        this.overShootHours = overShootHours;
    }

    public long getDurationDaysForMoCharge() {
        return durationDaysForMoCharge;
    }

    public void setDurationDaysForMoCharge(long durationDaysForMoCharge) {
        this.durationDaysForMoCharge = durationDaysForMoCharge;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public TimedItemDurationUnit getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(TimedItemDurationUnit durationUnit) {
        this.durationUnit = durationUnit;
    }
}
