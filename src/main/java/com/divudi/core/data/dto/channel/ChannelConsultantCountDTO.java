package com.divudi.core.data.dto.channel;

import java.util.Date;

import com.divudi.core.data.Title;

public class ChannelConsultantCountDTO {
    private String consultantName;
    private Title consultantTitle;
    private String consultantSpeciality;
    private Date sessionStartingTime;
    private Long sessionInstanceId;
    
    private long systemBookingCount;
    private long onlineBookingCount;
    private long agentBookingCount;
    private long onCallBookingCount;
    private long totalBookingCount;
    private long rescheduledBookingCount;

    private boolean holiday;

    private double doctorFee;
    private double refundDocFee;
    private double rescheduledDocFee;
    private double onCallDocFee;

    public ChannelConsultantCountDTO(Long sessionInstanceId, Long systemBookingCount, Long onlineBookingCount, Long agentBookingCount, Long onCallBookingCount, Long rescheduledBookingCount, Double doctorFee, Double refundDocFee, Double rescheduledDocFee, Double onCallDocFee) {
        this.sessionInstanceId = sessionInstanceId;
        this.systemBookingCount = systemBookingCount != null ? systemBookingCount : 0;
        this.onlineBookingCount = onlineBookingCount != null ? onlineBookingCount : 0;
        this.agentBookingCount = agentBookingCount != null ? agentBookingCount : 0;
        this.onCallBookingCount = onCallBookingCount != null ? onCallBookingCount : 0;
        this.rescheduledBookingCount = rescheduledBookingCount != null ? rescheduledBookingCount : 0;

        this.totalBookingCount = this.systemBookingCount + this.onlineBookingCount + this.agentBookingCount + this.onCallBookingCount + this.rescheduledBookingCount;

        this.doctorFee = doctorFee != null ? doctorFee.doubleValue() : 0.0;
        this.refundDocFee = refundDocFee != null ? refundDocFee.doubleValue() : 0.0;
        this.rescheduledDocFee = rescheduledDocFee != null ? rescheduledDocFee.doubleValue() : 0.0;
        this.onCallDocFee = onCallDocFee != null ? onCallDocFee.doubleValue() : 0.0;

        this.doctorFee = this.doctorFee + this.onCallDocFee + this.refundDocFee + this.rescheduledDocFee;
    }

    public ChannelConsultantCountDTO(Long id, Date sessionStartingTime, boolean holiday, String consultantName, Title consultantTitle, String consultantSpeciality) {
        this.sessionInstanceId = id;
        this.sessionStartingTime = sessionStartingTime;
        this.holiday = holiday;
        this.consultantName = consultantName;
        this.consultantTitle = consultantTitle;
        this.consultantSpeciality = consultantSpeciality;
        
        this.systemBookingCount = 0;
        this.onlineBookingCount = 0;
        this.agentBookingCount = 0;
        this.onCallBookingCount = 0;
        this.rescheduledBookingCount = 0;
        this.totalBookingCount = 0;
        
        this.doctorFee = 0.0;
    }

    public Long getSessionInstanceId() {
        return sessionInstanceId;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public Title getConsultantTitle() {
        return consultantTitle;
    }

    public void setConsultantTitle(Title consultantTitle) {
        this.consultantTitle = consultantTitle;
    }

    public String getConsultantSpeciality() {
        return consultantSpeciality;
    }

    public void setConsultantSpeciality(String consultantSpeciality) {
        this.consultantSpeciality = consultantSpeciality;
    }

    public Date getSessionStartingTime() {
        return sessionStartingTime;
    }

    public void setSessionStartingTime(Date sessionStartingTime) {
        this.sessionStartingTime = sessionStartingTime;
    }

    public long getSystemBookingCount() {
        return systemBookingCount;
    }

    public void setSystemBookingCount(long systemBookingCount) {
        this.systemBookingCount = systemBookingCount;
    }

    public long getOnlineBookingCount() {
        return onlineBookingCount;
    }

    public void setOnlineBookingCount(long onlineBookingCount) {
        this.onlineBookingCount = onlineBookingCount;
    }

    public long getAgentBookingCount() {
        return agentBookingCount;
    }

    public void setAgentBookingCount(long agentBookingCount) {
        this.agentBookingCount = agentBookingCount;
    }

    public long getOnCallBookingCount() {
        return onCallBookingCount;
    }

    public void setOnCallBookingCount(long onCallBookingCount) {
        this.onCallBookingCount = onCallBookingCount;
    }

    public long getRescheduledBookingCount() {
        return rescheduledBookingCount;
    }

    public void setRescheduledBookingCount(long rescheduledBookingCount) {
        this.rescheduledBookingCount = rescheduledBookingCount;
    }

    public long getTotalBookingCount() {
        return totalBookingCount;
    }

    public void setTotalBookingCount(long totalBookingCount) {
        this.totalBookingCount = totalBookingCount;
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    public double getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(double doctorFee) {
        this.doctorFee = doctorFee;
    }

    public String getConsultantNameWithTitle() {
        if (consultantTitle == null) {
            return consultantName;
        }
        return consultantTitle.getLabel() + " " + consultantName;
    }
    
}
