package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Holds occupancy metrics for a single room category
 * (e.g. Normal Room, A/C Room, Super Deluxe(Old), etc.)
 */
public class RoomCategoryOccupancyDTO implements Serializable {

    private Long numberOfRooms;   // distinct rooms occupied (or available, depending on context)
    private Long numberOfDays;    // total occupied days
    private Long totalAvailable;  // total available rooms in this category (for ratio calc)
    private Double ratio;         // occupied days / (available rooms * days in period)
    private Double avg;           // occupied days / number of rooms occupied

    public RoomCategoryOccupancyDTO() {
        this.numberOfRooms = 0L;
        this.numberOfDays = 0L;
        this.totalAvailable = 0L;
        this.ratio = 0.0;
        this.avg = 0.0;
    }

    public void addRoomCount(long count) {
        if (numberOfRooms == null) {
            numberOfRooms = 0L;
        }
        numberOfRooms += count;
    }

    public void addDayCount(long count) {
        if (numberOfDays == null) {
            numberOfDays = 0L;
        }
        numberOfDays += count;
    }

    public void merge(RoomCategoryOccupancyDTO other) {
        if (other == null) {
            return;
        }
        addRoomCount(other.numberOfRooms != null ? other.numberOfRooms : 0L);
        addDayCount(other.numberOfDays != null ? other.numberOfDays : 0L);
        if (other.totalAvailable != null) {
            this.totalAvailable = (this.totalAvailable == null ? 0L : this.totalAvailable) + other.totalAvailable;
        }
    }

    /**
     * Calculates ratio and average once all aggregation is complete.
     * ratio  = occupied days / (total available rooms * days in period)
     * avg    = occupied days / number of rooms occupied
     *
     * Note: ratio computation that depends on "days in period" should be
     * performed by the caller (controller) by passing daysInPeriod via
     * calculateDerived(daysInPeriod) below — this no-arg version is kept
     * for cases where totalAvailable already encodes the room-days denominator.
     */
    public void calculateDerived() {
        long days = numberOfDays != null ? numberOfDays : 0L;
        long rooms = numberOfRooms != null ? numberOfRooms : 0L;
        long available = totalAvailable != null ? totalAvailable : 0L;

        avg = rooms > 0 ? (double) days / rooms : 0.0;
        ratio = available > 0 ? (double) days / available : 0.0;
    }

    /**
     * Preferred derivation when the caller knows the number of calendar
     * days in the reporting period (e.g. days in month).
     *
     * ratio = occupied days / (totalAvailable rooms * daysInPeriod)
     */
    public void calculateDerived(long daysInPeriod) {
        long days = numberOfDays != null ? numberOfDays : 0L;
        long rooms = numberOfRooms != null ? numberOfRooms : 0L;
        long available = totalAvailable != null ? totalAvailable : 0L;

        avg = rooms > 0 ? (double) days / rooms : 0.0;
        double denom = (double) available * daysInPeriod;
        ratio = denom > 0 ? days / denom : 0.0;
    }

    // --- Getters / Setters ---

    public Long getNumberOfRooms() { return numberOfRooms; }
    public void setNumberOfRooms(Long numberOfRooms) { this.numberOfRooms = numberOfRooms; }

    public Long getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(Long numberOfDays) { this.numberOfDays = numberOfDays; }

    public Long getTotalAvailable() { return totalAvailable; }
    public void setTotalAvailable(Long totalAvailable) { this.totalAvailable = totalAvailable; }

    public Double getRatio() { return ratio; }
    public void setRatio(Double ratio) { this.ratio = ratio; }

    public Double getAvg() { return avg; }
    public void setAvg(Double avg) { this.avg = avg; }
}