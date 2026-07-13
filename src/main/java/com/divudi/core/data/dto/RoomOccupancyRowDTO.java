package com.divudi.core.data.dto;

import com.divudi.core.entity.inward.RoomCategory;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoomOccupancyRowDTO implements Serializable {

    private Integer year;
    private Integer month;
    private Long numberOfAdmissions;

    // Keyed by RoomCategory entity — order preserved via LinkedHashMap
    private Map<RoomCategory, RoomCategoryOccupancyDTO> categoryMetrics;

    private RoomBedOccupancyDTO icuOccupancy;
    private RoomBedOccupancyDTO wardBedOldOccupancy;
    private RoomBedOccupancyDTO wardBedNewOccupancy;

    private Long indoorTotalDaysOccupied;
    private Long indoorTotalNumberOfRooms;
    private Double indoorTotalRatio;
    private Double indoorTotalAvg;

    private Long icuWardTotalDaysOccupied;
    private Long icuWardTotalNumberOfBeds;
    private Double icuWardRatio;
    private Double icuWardAvg;

    private boolean grandTotal;

    public RoomOccupancyRowDTO() {
        this.categoryMetrics = new LinkedHashMap<>();
        this.icuOccupancy = new RoomBedOccupancyDTO();
        this.wardBedOldOccupancy = new RoomBedOccupancyDTO();
        this.wardBedNewOccupancy = new RoomBedOccupancyDTO();
    }

    public RoomOccupancyRowDTO(Integer year, Integer month) {
        this();
        this.year = year;
        this.month = month;
    }

    public void addAdmissions(long count) {
        if (numberOfAdmissions == null) {
            numberOfAdmissions = 0L;
        }
        numberOfAdmissions += count;
    }

    public void addCategoryDays(RoomCategory category, long roomCount, long dayCount) {
        RoomCategoryOccupancyDTO dto = categoryMetrics.computeIfAbsent(category,
                k -> new RoomCategoryOccupancyDTO());
        dto.addRoomCount(roomCount);
        dto.addDayCount(dayCount);
    }

    public void addCategoryPatientRatioDays(RoomCategory category, long categoryDays, long totalLengthOfStayDays) {
        RoomCategoryOccupancyDTO dto = categoryMetrics.computeIfAbsent(category,
                k -> new RoomCategoryOccupancyDTO());
        dto.addPatientRatioDays(categoryDays, totalLengthOfStayDays);
    }

    /**
     * Ensure every expected category has an entry (zero-filled) so XHTML
     * iteration never encounters a null slot.
     *
     * @param allCategories
     */
    public void ensureCategories(List<RoomCategory> allCategories) {
        for (RoomCategory rc : allCategories) {
            categoryMetrics.computeIfAbsent(rc, k -> new RoomCategoryOccupancyDTO());
        }
    }

    public void merge(RoomOccupancyRowDTO other) {
        if (other.numberOfAdmissions != null) {
            addAdmissions(other.numberOfAdmissions);
        }

        for (Map.Entry<RoomCategory, RoomCategoryOccupancyDTO> e : other.categoryMetrics.entrySet()) {
            categoryMetrics.computeIfAbsent(e.getKey(), k -> new RoomCategoryOccupancyDTO())
                    .merge(e.getValue());
        }
        if (icuOccupancy != null && other.icuOccupancy != null) {
            icuOccupancy.merge(other.icuOccupancy);
        }
        if (wardBedOldOccupancy != null && other.wardBedOldOccupancy != null) {
            wardBedOldOccupancy.merge(other.wardBedOldOccupancy);
        }
        if (wardBedNewOccupancy != null && other.wardBedNewOccupancy != null) {
            wardBedNewOccupancy.merge(other.wardBedNewOccupancy);
        }
    }

    public void calculateDerivedMetrics() {
        long daysOcc = 0, rooms = 0;
        for (RoomCategoryOccupancyDTO dto : categoryMetrics.values()) {
            daysOcc += (dto.getNumberOfDays() != null ? dto.getNumberOfDays() : 0L);
            rooms += (dto.getNumberOfRooms() != null ? dto.getNumberOfRooms() : 0L);
        }
        indoorTotalDaysOccupied = daysOcc;
        indoorTotalNumberOfRooms = rooms;
        indoorTotalRatio = rooms > 0 ? (double) daysOcc / rooms : 0.0;
        indoorTotalAvg = rooms > 0 ? (double) daysOcc / rooms : 0.0;

        long icuWardDays
                = safeDay(icuOccupancy) + safeDay(wardBedOldOccupancy) + safeDay(wardBedNewOccupancy);
        long icuWardBeds
                = safeUnit(icuOccupancy) + safeUnit(wardBedOldOccupancy) + safeUnit(wardBedNewOccupancy);
        icuWardTotalDaysOccupied = icuWardDays;
        icuWardTotalNumberOfBeds = icuWardBeds;
        icuWardRatio = icuWardBeds > 0 ? (double) icuWardDays / icuWardBeds : 0.0;
        icuWardAvg = icuWardBeds > 0 ? (double) icuWardDays / icuWardBeds : 0.0;
    }

    private long safeDay(RoomBedOccupancyDTO d) {
        return d != null && d.getNumberOfDays() != null ? d.getNumberOfDays() : 0L;
    }

    private long safeUnit(RoomBedOccupancyDTO d) {
        return d != null && d.getNumberOfUnits() != null ? d.getNumberOfUnits() : 0L;
    }

    // --- Getters / Setters ---
    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getMonthName() {
        if (month == null) {
            return "";
        }
        String[] n = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return (month >= 1 && month <= 12) ? n[month - 1] : "";
    }

    public Long getNumberOfAdmissions() {
        return numberOfAdmissions;
    }

    public void setNumberOfAdmissions(Long v) {
        this.numberOfAdmissions = v;
    }

    public Map<RoomCategory, RoomCategoryOccupancyDTO> getCategoryMetrics() {
        if (categoryMetrics == null) {
            categoryMetrics = new LinkedHashMap<>();
        }
        return categoryMetrics;
    }

    public void setCategoryMetrics(Map<RoomCategory, RoomCategoryOccupancyDTO> v) {
        categoryMetrics = v;
    }

    public RoomBedOccupancyDTO getIcuOccupancy() {
        return icuOccupancy;
    }

    public void setIcuOccupancy(RoomBedOccupancyDTO v) {
        icuOccupancy = v;
    }

    public RoomBedOccupancyDTO getWardBedOldOccupancy() {
        return wardBedOldOccupancy;
    }

    public void setWardBedOldOccupancy(RoomBedOccupancyDTO v) {
        wardBedOldOccupancy = v;
    }

    public RoomBedOccupancyDTO getWardBedNewOccupancy() {
        return wardBedNewOccupancy;
    }

    public void setWardBedNewOccupancy(RoomBedOccupancyDTO v) {
        wardBedNewOccupancy = v;
    }

    public Long getIndoorTotalDaysOccupied() {
        return indoorTotalDaysOccupied;
    }

    public void setIndoorTotalDaysOccupied(Long v) {
        indoorTotalDaysOccupied = v;
    }

    public Long getIndoorTotalNumberOfRooms() {
        return indoorTotalNumberOfRooms;
    }

    public void setIndoorTotalNumberOfRooms(Long v) {
        indoorTotalNumberOfRooms = v;
    }

    public Double getIndoorTotalRatio() {
        return indoorTotalRatio;
    }

    public void setIndoorTotalRatio(Double v) {
        indoorTotalRatio = v;
    }

    public Double getIndoorTotalAvg() {
        return indoorTotalAvg;
    }

    public void setIndoorTotalAvg(Double v) {
        indoorTotalAvg = v;
    }

    public Long getIcuWardTotalDaysOccupied() {
        return icuWardTotalDaysOccupied;
    }

    public void setIcuWardTotalDaysOccupied(Long v) {
        icuWardTotalDaysOccupied = v;
    }

    public Long getIcuWardTotalNumberOfBeds() {
        return icuWardTotalNumberOfBeds;
    }

    public void setIcuWardTotalNumberOfBeds(Long v) {
        icuWardTotalNumberOfBeds = v;
    }

    public Double getIcuWardRatio() {
        return icuWardRatio;
    }

    public void setIcuWardRatio(Double v) {
        icuWardRatio = v;
    }

    public Double getIcuWardAvg() {
        return icuWardAvg;
    }

    public void setIcuWardAvg(Double v) {
        icuWardAvg = v;
    }

    public boolean isGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(boolean v) {
        grandTotal = v;
    }

    /**
     * Safe lookup used by XHTML dynamic room-category columns.
     *
     * @param rc
     * @return
     */
    public RoomCategoryOccupancyDTO metricFor(RoomCategory rc) {
        if (rc == null) {
            return new RoomCategoryOccupancyDTO();
        }
        return getCategoryMetrics().getOrDefault(rc, new RoomCategoryOccupancyDTO());
    }

    public RoomCategoryOccupancyDTO getMetricFor(RoomCategory rc) {
        return metricFor(rc);
    }
}
