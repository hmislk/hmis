package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class OtRoomWiseSurgeryCountDTO implements Serializable {

    private String roomName;
    private Long roomFacilityChargeId;
    private Map<Integer, Long> monthlyCounts = new HashMap<>();
    private long totalCount = 0;

    public OtRoomWiseSurgeryCountDTO() {
    }

    public OtRoomWiseSurgeryCountDTO(String roomName, Long roomFacilityChargeId) {
        this.roomName = roomName;
        this.roomFacilityChargeId = roomFacilityChargeId;
    }

    public long getCount(int monthIndex) {
        return monthlyCounts.getOrDefault(monthIndex, 0L);
    }

    public void addCount(int monthIndex, long count) {
        monthlyCounts.merge(monthIndex, count, Long::sum);
        totalCount += count;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Long getRoomFacilityChargeId() {
        return roomFacilityChargeId;
    }

    public void setRoomFacilityChargeId(Long roomFacilityChargeId) {
        this.roomFacilityChargeId = roomFacilityChargeId;
    }

    public Map<Integer, Long> getMonthlyCounts() {
        return monthlyCounts;
    }

    public void setMonthlyCounts(Map<Integer, Long> monthlyCounts) {
        this.monthlyCounts = monthlyCounts;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}
