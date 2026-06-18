package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Holds occupancy metrics for a single room category
 * (e.g. Normal Room, A/C Room, Super Deluxe(Old), etc.)
 */
public class RoomCategoryOccupancyDTO implements Serializable {

    public static final String RATIO_MODE_AGGREGATED_ROOM_UTILIZATION = "AGGREGATED_ROOM_UTILIZATION";
    public static final String RATIO_MODE_PATIENT_CATEGORY_DURATION = "PATIENT_CATEGORY_DURATION";

    private Long numberOfRooms;   // distinct rooms occupied (or available, depending on context)
    private Long numberOfDays;    // total occupied days
    private Long totalAvailable;  // total available rooms in this category (for ratio calc)
    private Long patientCategoryDays; // numerator for patient category duration ratio
    private Long patientTotalLengthOfStayDays; // denominator for patient category duration ratio
    private Long availableRoomDays; // denominator for aggregated room utilization ratio
    private Double ratio;         // selected ratio, based on ratioCalculationMode
    private Double patientCategoryDurationRatio;
    private Double aggregatedRoomUtilizationRatio;
    private String ratioCalculationMode;
    private Double avg;           // occupied days / number of rooms occupied

    public RoomCategoryOccupancyDTO() {
        this.numberOfRooms = 0L;
        this.numberOfDays = 0L;
        this.totalAvailable = 0L;
        this.patientCategoryDays = 0L;
        this.patientTotalLengthOfStayDays = 0L;
        this.availableRoomDays = 0L;
        this.ratio = 0.0;
        this.patientCategoryDurationRatio = 0.0;
        this.aggregatedRoomUtilizationRatio = 0.0;
        this.ratioCalculationMode = RATIO_MODE_AGGREGATED_ROOM_UTILIZATION;
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

    public void addPatientRatioDays(long categoryDays, long totalLengthOfStayDays) {
        if (patientCategoryDays == null) {
            patientCategoryDays = 0L;
        }
        if (patientTotalLengthOfStayDays == null) {
            patientTotalLengthOfStayDays = 0L;
        }
        patientCategoryDays += categoryDays;
        patientTotalLengthOfStayDays += totalLengthOfStayDays;
    }

    public void merge(RoomCategoryOccupancyDTO other) {
        if (other == null) {
            return;
        }
        addRoomCount(other.numberOfRooms != null ? other.numberOfRooms : 0L);
        addDayCount(other.numberOfDays != null ? other.numberOfDays : 0L);
        addPatientRatioDays(
                other.patientCategoryDays != null ? other.patientCategoryDays : 0L,
                other.patientTotalLengthOfStayDays != null ? other.patientTotalLengthOfStayDays : 0L);
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
        availableRoomDays = available;
        aggregatedRoomUtilizationRatio = availableRoomDays > 0 ? (double) days / availableRoomDays : 0.0;
        calculatePatientCategoryDurationRatio();
        ratioCalculationMode = RATIO_MODE_AGGREGATED_ROOM_UTILIZATION;
        ratio = aggregatedRoomUtilizationRatio;
    }

    /**
     * Preferred derivation when the caller knows the number of calendar
     * days in the reporting period (e.g. days in month).
     *
     * ratio = occupied days / (totalAvailable rooms * daysInPeriod)
     */
    public void calculateDerived(long daysInPeriod) {
        calculateDerived(daysInPeriod, RATIO_MODE_AGGREGATED_ROOM_UTILIZATION);
    }

    /**
     * Calculates both supported ratio formulas and selects the display ratio.
     *
     * Patient category duration ratio:
     * patientCategoryDays / patientTotalLengthOfStayDays
     *
     * Aggregated room utilization ratio:
     * numberOfDays / (totalAvailable rooms * daysInPeriod)
     *
     * @param daysInPeriod
     * @param selectedRatioCalculationMode
     */
    public void calculateDerived(long daysInPeriod, String selectedRatioCalculationMode) {
        long days = numberOfDays != null ? numberOfDays : 0L;
        long rooms = numberOfRooms != null ? numberOfRooms : 0L;
        long available = totalAvailable != null ? totalAvailable : 0L;

        avg = rooms > 0 ? (double) days / rooms : 0.0;
        double denom = (double) available * daysInPeriod;
        availableRoomDays = denom > 0 ? (long) denom : 0L;
        aggregatedRoomUtilizationRatio = denom > 0 ? days / denom : 0.0;
        calculatePatientCategoryDurationRatio();
        ratioCalculationMode = selectedRatioCalculationMode != null
                ? selectedRatioCalculationMode : RATIO_MODE_AGGREGATED_ROOM_UTILIZATION;
        ratio = RATIO_MODE_PATIENT_CATEGORY_DURATION.equals(ratioCalculationMode)
                ? patientCategoryDurationRatio : aggregatedRoomUtilizationRatio;
    }

    private void calculatePatientCategoryDurationRatio() {
        long categoryDays = patientCategoryDays != null ? patientCategoryDays : 0L;
        long totalLengthOfStayDays = patientTotalLengthOfStayDays != null ? patientTotalLengthOfStayDays : 0L;
        patientCategoryDurationRatio = totalLengthOfStayDays > 0
                ? (double) categoryDays / totalLengthOfStayDays : 0.0;
    }

    // --- Getters / Setters ---

    public Long getNumberOfRooms() { return numberOfRooms; }
    public void setNumberOfRooms(Long numberOfRooms) { this.numberOfRooms = numberOfRooms; }

    public Long getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(Long numberOfDays) { this.numberOfDays = numberOfDays; }

    public Long getTotalAvailable() { return totalAvailable; }
    public void setTotalAvailable(Long totalAvailable) { this.totalAvailable = totalAvailable; }

    public Long getPatientCategoryDays() { return patientCategoryDays; }
    public void setPatientCategoryDays(Long patientCategoryDays) { this.patientCategoryDays = patientCategoryDays; }

    public Long getPatientTotalLengthOfStayDays() { return patientTotalLengthOfStayDays; }
    public void setPatientTotalLengthOfStayDays(Long patientTotalLengthOfStayDays) { this.patientTotalLengthOfStayDays = patientTotalLengthOfStayDays; }

    public Long getAvailableRoomDays() { return availableRoomDays; }
    public void setAvailableRoomDays(Long availableRoomDays) { this.availableRoomDays = availableRoomDays; }

    public Double getRatio() { return ratio; }
    public void setRatio(Double ratio) { this.ratio = ratio; }

    public Double getPatientCategoryDurationRatio() { return patientCategoryDurationRatio; }
    public void setPatientCategoryDurationRatio(Double patientCategoryDurationRatio) { this.patientCategoryDurationRatio = patientCategoryDurationRatio; }

    public Double getAggregatedRoomUtilizationRatio() { return aggregatedRoomUtilizationRatio; }
    public void setAggregatedRoomUtilizationRatio(Double aggregatedRoomUtilizationRatio) { this.aggregatedRoomUtilizationRatio = aggregatedRoomUtilizationRatio; }

    public String getRatioCalculationMode() { return ratioCalculationMode; }
    public void setRatioCalculationMode(String ratioCalculationMode) { this.ratioCalculationMode = ratioCalculationMode; }

    public Double getAvg() { return avg; }
    public void setAvg(Double avg) { this.avg = avg; }
}
