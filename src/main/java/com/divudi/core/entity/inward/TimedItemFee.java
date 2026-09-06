/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.data.inward.TimedItemDurationUnit;
import com.divudi.core.entity.Fee;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;


/**
 *
 * @author buddhika
 */
@Entity
public class TimedItemFee extends Fee implements Serializable {

    private double durationHours = 0;
    private double overShootHours = 0;
    private long durationDaysForMoCharge;
    private int sortOrder = 0;
    private boolean repeating = false;
    /**
     * The unit durationHours/overShootHours are expressed in. Null on every row
     * created before this field existed, which is why {@link #getDurationUnit()}
     * reads it as HOUR — the behaviour those rows have always had.
     */
    @Enumerated(EnumType.STRING)
    private TimedItemDurationUnit durationUnit;

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public void setOverShootHours(Integer overShootHours) {
        this.overShootHours = overShootHours;
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

    /**
     * Never null: rows saved before this field existed are hour-based, so that
     * is what an unset unit means. Defaulting on read (rather than leaving null
     * to every caller) also keeps the configuration dropdowns from showing a
     * legacy fee as whatever option happens to be listed first.
     * <p>
     * The entity hierarchy uses field access (see the {@code @Id} on
     * {@code Fee.id}), so this default is not written back to the column until
     * something actually sets the unit.
     */
    public TimedItemDurationUnit getDurationUnit() {
        return durationUnit == null ? TimedItemDurationUnit.HOUR : durationUnit;
    }

    public void setDurationUnit(TimedItemDurationUnit durationUnit) {
        this.durationUnit = durationUnit;
    }

    /**
     * True when this fee is charged once for the whole service, regardless of
     * how long it ran.
     */
    @Transient
    public boolean isOneTime() {
        return getDurationUnit() == TimedItemDurationUnit.ONE_TIME;
    }

    /**
     * Length of one billable block in minutes. This is the single place elapsed
     * time is converted into block size, so every calculation (room charges,
     * interim bill, final bill, timed service consume) reads the same number.
     * Zero for a one-time fee, which is not time-based.
     */
    @Transient
    public double getDurationMinutes() {
        if (isOneTime()) {
            return 0;
        }
        return durationHours * getDurationUnit().getMinutesPerUnit();
    }

    /**
     * Length of one billable block in hours, for callers that reason in hours
     * (e.g. package room-duration variance).
     */
    @Transient
    public double getDurationInHours() {
        return getDurationMinutes() / 60.0;
    }

    /**
     * Grace period in minutes before an incomplete block is charged as a whole
     * block. Expressed in the same unit as the block length.
     */
    @Transient
    public double getOverShootMinutes() {
        if (isOneTime()) {
            return 0;
        }
        return overShootHours * getDurationUnit().getMinutesPerUnit();
    }
}
