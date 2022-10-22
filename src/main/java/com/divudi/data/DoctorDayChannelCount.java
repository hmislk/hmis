/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import com.divudi.entity.Staff;
import java.util.Date;

/**
 *
 * @author buddhika_ari
 */
public class DoctorDayChannelCount {

    private Staff staff;
    private Date appointmentDate;
    private Long booked;
    private Long cancelled;
    private Long refunded;
    
    public DoctorDayChannelCount() {
    }

    public DoctorDayChannelCount(Staff staff, Date appointmentDate, Long booked, Long cancelled, Long refunded) {
        this.staff = staff;
        this.appointmentDate = appointmentDate;
        this.booked = booked;
        this.cancelled = cancelled;
        this.refunded = refunded;
    }

    public DoctorDayChannelCount(Staff staff, Date appointmentDate, Long booked) {
        this.staff = staff;
        this.appointmentDate = appointmentDate;
        this.booked = booked;
    }
    
    
    
    

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public Long getBooked() {
        return booked;
    }

    public void setBooked(Long booked) {
        this.booked = booked;
    }

    public Long getCancelled() {
        return cancelled;
    }

    public void setCancelled(Long cancelled) {
        this.cancelled = cancelled;
    }

    public Long getRefunded() {
        return refunded;
    }

    public void setRefunded(Long refunded) {
        this.refunded = refunded;
    }

    
    
    
    
    
}
