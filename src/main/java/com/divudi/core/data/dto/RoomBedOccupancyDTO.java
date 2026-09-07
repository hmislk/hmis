package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Holds occupancy metrics for I.C.U / Ward Bed (Old) / Ward Bed (New) sections.
 * "numberOfUnits" represents rooms for I.C.U and beds for Ward Bed sections,
 * following the report's column semantics (#Rooms vs #Beds).
 */
public class RoomBedOccupancyDTO implements Serializable {

    private Long numberOfUnits;   // #Rooms (ICU) or #Beds (Ward Bed Old/New)
    private Long numberOfDays;    // total occupied days
    private Long totalAvailable;  // total available units (rooms/beds) for ratio calc
    private Double ratio;
    private Double avg;

    public RoomBedOccupancyDTO() {
        this.numberOfUnits = 0L;
        this.numberOfDays = 0L;
        this.totalAvailable = 0L;
        this.ratio = 0.0;
        this.avg = 0.0;
    }

    public void addUnitCount(long count) {
        if (numberOfUnits == null) {
            numberOfUnits = 0L;
        }
        numberOfUnits += count;
    }

    public void addDayCount(long count) {
        if (numberOfDays == null) {
            numberOfDays = 0L;
        }
        numberOfDays += count;
    }

    public void merge(RoomBedOccupancyDTO other) {
        if (other == null) {
            return;
        }
        addUnitCount(other.numberOfUnits != null ? other.numberOfUnits : 0L);
        addDayCount(other.numberOfDays != null ? other.numberOfDays : 0L);
        if (other.totalAvailable != null) {
            this.totalAvailable = (this.totalAvailable == null ? 0L : this.totalAvailable) + other.totalAvailable;
        }
    }

    public void calculateDerived() {
        long days = numberOfDays != null ? numberOfDays : 0L;
        long units = numberOfUnits != null ? numberOfUnits : 0L;
        long available = totalAvailable != null ? totalAvailable : 0L;

        avg = units > 0 ? (double) days / units : 0.0;
        ratio = available > 0 ? (double) days / available : 0.0;
    }

    public void calculateDerived(long daysInPeriod) {
        long days = numberOfDays != null ? numberOfDays : 0L;
        long units = numberOfUnits != null ? numberOfUnits : 0L;
        long available = totalAvailable != null ? totalAvailable : 0L;

        avg = units > 0 ? (double) days / units : 0.0;
        double denom = (double) available * daysInPeriod;
        ratio = denom > 0 ? days / denom : 0.0;
    }

    // --- Getters / Setters ---

    public Long getNumberOfUnits() { return numberOfUnits; }
    public void setNumberOfUnits(Long numberOfUnits) { this.numberOfUnits = numberOfUnits; }

    public Long getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(Long numberOfDays) { this.numberOfDays = numberOfDays; }

    public Long getTotalAvailable() { return totalAvailable; }
    public void setTotalAvailable(Long totalAvailable) { this.totalAvailable = totalAvailable; }

    public Double getRatio() { return ratio; }
    public void setRatio(Double ratio) { this.ratio = ratio; }

    public Double getAvg() { return avg; }
    public void setAvg(Double avg) { this.avg = avg; }
}