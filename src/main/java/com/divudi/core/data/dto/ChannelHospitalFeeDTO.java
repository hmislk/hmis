package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for bulk editing Hospital Fees (OwnInstitution fees) for Channel Sessions
 *
 * @author Claude Code
 */
public class ChannelHospitalFeeDTO implements Serializable {

    private Long itemFeeId;
    private Long serviceSessionId;
    private String sessionName;
    private String consultantName;
    private String specialityName;
    private String departmentName;
    private Integer sessionWeekday;
    private String weekdayName;
    private Double hospitalFee;
    private Double hospitalForeignerFee;
    private boolean selected;

    public ChannelHospitalFeeDTO() {
    }

    /**
     * Constructor for JPQL query results
     */
    public ChannelHospitalFeeDTO(Long itemFeeId, Long serviceSessionId, String sessionName,
                                  String consultantName, String specialityName,
                                  String departmentName, Integer sessionWeekday,
                                  Double hospitalFee, Double hospitalForeignerFee) {
        this.itemFeeId = itemFeeId;
        this.serviceSessionId = serviceSessionId;
        this.sessionName = sessionName;
        this.consultantName = consultantName;
        this.specialityName = specialityName;
        this.departmentName = departmentName;
        this.sessionWeekday = sessionWeekday;
        this.hospitalFee = hospitalFee;
        this.hospitalForeignerFee = hospitalForeignerFee;
        this.selected = false;
        this.weekdayName = getWeekdayNameFromNumber(sessionWeekday);
    }

    private String getWeekdayNameFromNumber(Integer weekday) {
        if (weekday == null) {
            return "";
        }
        switch (weekday) {
            case 1: return "Sunday";
            case 2: return "Monday";
            case 3: return "Tuesday";
            case 4: return "Wednesday";
            case 5: return "Thursday";
            case 6: return "Friday";
            case 7: return "Saturday";
            default: return "";
        }
    }

    // Getters and Setters
    public Long getItemFeeId() {
        return itemFeeId;
    }

    public void setItemFeeId(Long itemFeeId) {
        this.itemFeeId = itemFeeId;
    }

    public Long getServiceSessionId() {
        return serviceSessionId;
    }

    public void setServiceSessionId(Long serviceSessionId) {
        this.serviceSessionId = serviceSessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getSessionWeekday() {
        return sessionWeekday;
    }

    public void setSessionWeekday(Integer sessionWeekday) {
        this.sessionWeekday = sessionWeekday;
        this.weekdayName = getWeekdayNameFromNumber(sessionWeekday);
    }

    public String getWeekdayName() {
        return weekdayName;
    }

    public void setWeekdayName(String weekdayName) {
        this.weekdayName = weekdayName;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getHospitalForeignerFee() {
        return hospitalForeignerFee;
    }

    public void setHospitalForeignerFee(Double hospitalForeignerFee) {
        this.hospitalForeignerFee = hospitalForeignerFee;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
