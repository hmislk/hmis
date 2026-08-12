package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Data Transfer Object for Room Occupancy Report.
 */
public class RoomOccupancyReportDto implements Serializable {

    // Detail Fields
    private String mrn;
    private String patientName;
    private Date admissionDate;
    private Date dischargeDate;
    private String roomCategory;
    private String admissionType;
    private String wardBed;
    private double occupancyDays;
    private Date roomFromTime;
    private Date roomToTime;
    private String durationTime;
    
    // Summary Fields
    private String groupName;
    private double totalOccupancyDays;

    public RoomOccupancyReportDto() {
    }

    // Constructor for JPQL Detail query projection
    public RoomOccupancyReportDto(String mrn, String patientName, Date admissionDate, Date dischargeDate,
                                  String roomCategory, String admissionType, String wardName, String roomName,
                                  Date roomFromTime, Date roomToTime) {
        this.mrn = mrn;
        this.patientName = patientName;
        this.admissionDate = admissionDate;
        this.dischargeDate = dischargeDate;
        this.roomCategory = roomCategory;
        this.admissionType = admissionType;
        
        // Combine Ward and Room/Bed name
        if (wardName != null && roomName != null) {
            this.wardBed = wardName + " | " + roomName;
        } else if (wardName != null) {
            this.wardBed = wardName;
        } else if (roomName != null) {
            this.wardBed = roomName;
        } else {
            this.wardBed = "";
        }
        
        this.roomFromTime = roomFromTime;
        this.roomToTime = roomToTime;
        
        calculateDurationAndDays();
    }
    
    private void calculateDurationAndDays() {
        if (this.roomFromTime != null) {
            long fromMillis = this.roomFromTime.getTime();
            long toMillis;
            if (this.roomToTime != null) {
                toMillis = this.roomToTime.getTime();
            } else {
                // If not yet discharged from room, calculate up to now
                toMillis = new Date().getTime();
            }
            
            long durationMillis = toMillis - fromMillis;
            if (durationMillis > 0) {
                // Precise days calculation
                this.occupancyDays = (double) durationMillis / (1000 * 60 * 60 * 24);
                
                long totalSecs = durationMillis / 1000;
                long hours = totalSecs / 3600;
                long mins = (totalSecs % 3600) / 60;
                long secs = totalSecs % 60;
                this.durationTime = String.format("%02d:%02d:%02d", hours, mins, secs);
            } else {
                this.occupancyDays = 0.0;
                this.durationTime = "00:00:00";
            }
        } else {
            this.occupancyDays = 0.0;
            this.durationTime = "";
        }
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Date getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(Date admissionDate) {
        this.admissionDate = admissionDate;
    }

    public Date getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(Date dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public String getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(String roomCategory) {
        this.roomCategory = roomCategory;
    }

    public String getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(String admissionType) {
        this.admissionType = admissionType;
    }

    public String getWardBed() {
        return wardBed;
    }

    public void setWardBed(String wardBed) {
        this.wardBed = wardBed;
    }

    public double getOccupancyDays() {
        return occupancyDays;
    }

    public void setOccupancyDays(double occupancyDays) {
        this.occupancyDays = occupancyDays;
    }

    public Date getRoomFromTime() {
        return roomFromTime;
    }

    public void setRoomFromTime(Date roomFromTime) {
        this.roomFromTime = roomFromTime;
    }

    public Date getRoomToTime() {
        return roomToTime;
    }

    public void setRoomToTime(Date roomToTime) {
        this.roomToTime = roomToTime;
    }

    public String getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(String durationTime) {
        this.durationTime = durationTime;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public double getTotalOccupancyDays() {
        return totalOccupancyDays;
    }

    public void setTotalOccupancyDays(double totalOccupancyDays) {
        this.totalOccupancyDays = totalOccupancyDays;
    }
}
