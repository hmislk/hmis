/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.inward;

import com.divudi.entity.Fee;
import java.io.Serializable;
import javax.persistence.Entity;


/**
 *
 * @author buddhika
 */
@Entity
public class TimedItemFee extends Fee implements Serializable {

    private double durationHours = 0;
    private double overShootHours = 0;
    private long durationDaysForMoCharge;

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
}
