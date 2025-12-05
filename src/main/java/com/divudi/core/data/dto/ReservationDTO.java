package com.divudi.core.data.dto;

import com.divudi.bean.lab.LaboratoryCommonController;
import com.divudi.core.data.AppointmentStatus;
import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

public class ReservationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Date reservedFrom;
    private Date reservedTo;
    private String appointmentNumber;
    private Date createdAt;
    private String roomNo;
    private String patientNameWithTitle;
    private Title patientTitle;
    private String patientName;
    private Date patientDob;
    private String patientAge;
    private String patientGender;
    private String patientMobile;
    private AppointmentStatus status;
    
    public ReservationDTO() {
        
    }
    
    // Usinng searchAppointments() in AppointmentController
    public ReservationDTO(Long id, Date reservedFrom, Date reservedTo,String appointmentNumber, Date createdAt,String roomNo, Title patientTitle, String patientName,Date patientDob, String patientGender, String patientMobile, AppointmentStatus status) {
        this.id = id;
        this.reservedFrom = reservedFrom;
        this.reservedTo = reservedTo;
        this.appointmentNumber = appointmentNumber;
        this.createdAt = createdAt;
        this.roomNo = roomNo;
        this.patientTitle = patientTitle;
        this.patientName = patientName;
        this.patientDob = patientDob;
        this.patientGender = patientGender;
        this.patientMobile = patientMobile;
        this.status = status;
        
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getReservedFrom() {
        return reservedFrom;
    }

    public void setReservedFrom(Date reservedFrom) {
        this.reservedFrom = reservedFrom;
    }

    public Date getReservedTo() {
        return reservedTo;
    }

    public void setReservedTo(Date reservedTo) {
        this.reservedTo = reservedTo;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getPatientDob() {
        return patientDob;
    }

    public void setPatientDob(Date patientDob) {
        this.patientDob = patientDob;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public void setPatientTitle(Title patientTitle) {
        this.patientTitle = patientTitle;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAge() {
        patientAge = LaboratoryCommonController.calculateAge(patientDob,createdAt);
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientNameWithTitle() {
        String temT;
        Title t = getPatientTitle();
        if (t != null) {
            temT = t.getLabel();
        } else {
            temT = "";
        }
        patientNameWithTitle = temT + " " + getPatientName();
        return patientNameWithTitle;
    }

    public void setPatientNameWithTitle(String patientNameWithTitle) {
        this.patientNameWithTitle = patientNameWithTitle;
    }

    public String getPatientMobile() {
        return patientMobile;
    }

    public void setPatientMobile(String patientMobile) {
        this.patientMobile = patientMobile;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

}

