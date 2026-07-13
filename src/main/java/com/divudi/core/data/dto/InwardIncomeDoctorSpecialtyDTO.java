/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import com.divudi.core.entity.Staff;

/**
 *
 * @author OVS
 */
public class InwardIncomeDoctorSpecialtyDTO {
    
    private Long staffId;
    private Title doctorTitle; 
    private String doctorName;
    private String specialtyName;
    
    private Double docFee;
    private Double hosFee;
    private Double totalCharge;
    
    
    // constructor
    public InwardIncomeDoctorSpecialtyDTO(){
        this.docFee = 0.0;
        this.hosFee = 0.0;
        this.totalCharge = 0.0;
    }
    
    // constructor for doctor wise
    public InwardIncomeDoctorSpecialtyDTO(Long staffId, Title doctorTitle, String doctorName, String specialtyName, Double docFee, Double hosFee) {
        this.staffId = staffId;
        this.doctorTitle = (doctorTitle != null ? doctorTitle : Title.Dr);
        this.doctorName = doctorName;
        this.specialtyName = specialtyName;
        this.docFee = docFee;
        this.hosFee = hosFee;
    }
    
    // constructor for speciality wise
    public InwardIncomeDoctorSpecialtyDTO(Long staffId, String specialtyName, Double docFee, Double hosFee) {
        this.staffId = staffId;
        this.specialtyName = specialtyName;
        this.docFee = docFee;
        this.hosFee = hosFee;
    }
    
    public InwardIncomeDoctorSpecialtyDTO(Long staffId, Double docFee, Double hosFee) {
        this.staffId = staffId;
        this.docFee = docFee;
        this.hosFee = hosFee;
    }
    
    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
    
    public Title getDoctorTitle() {
        return doctorTitle;
    }
    
    public void setDoctorTitle(Title title) {
        this.doctorTitle = title;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialtyName() {
        return specialtyName;
    }

    public void setSpecialtyName(String specialtyName) {
        this.specialtyName = specialtyName;
    }

    public Double getDocFee() {
        return docFee;
    }

    public void setDocFee(Double docFee) {
        this.docFee = docFee;
    }

    public Double getHosFee() {
        return hosFee;
    }

    public void setHosFee(Double hosFee) {
        this.hosFee = hosFee;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }

}
