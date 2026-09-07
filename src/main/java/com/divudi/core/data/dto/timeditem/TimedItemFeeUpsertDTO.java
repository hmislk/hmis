/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

import com.divudi.core.data.inward.TimedItemDurationUnit;

/**
 * One slot in a bulk fee write ({@code PUT /timed-items/{id}/fees}).
 *
 * <p>{@code id} is what distinguishes an update from a create: present means "this is
 * the existing fee with that id", absent means "add a new slot". Any live fee whose id
 * is missing from the submitted list is retired by the bulk write.
 *
 * @author Buddhika
 */
public class TimedItemFeeUpsertDTO {

    /** Existing fee to update. Null/absent creates a new fee. */
    private Long id;
    private String name;
    private double fee;
    private double ffee;
    private double durationHours;
    private double overShootHours;
    private long durationDaysForMoCharge;
    private int sortOrder;
    private boolean repeating;
    private TimedItemDurationUnit durationUnit;

    public TimedItemFeeUpsertDTO() {
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
