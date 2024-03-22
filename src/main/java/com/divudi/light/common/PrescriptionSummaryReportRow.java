/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.light.common;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class PrescriptionSummaryReportRow {
    Integer id;
    String doctorName;
    Long doctorId;
    Long prescriptionCount;
    Double prescriptionValue;

    public PrescriptionSummaryReportRow() {
    }

    public PrescriptionSummaryReportRow(String doctorName, Long doctorId, Long prescriptionCount, Double prescriptionValue) {
        this.doctorName = doctorName;
        this.doctorId = doctorId;
        this.prescriptionCount = prescriptionCount;
        this.prescriptionValue = prescriptionValue;
    }
    
    

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getPrescriptionCount() {
        return prescriptionCount;
    }

    public void setPrescriptionCount(Long prescriptionCount) {
        this.prescriptionCount = prescriptionCount;
    }

    public Double getPrescriptionValue() {
        return prescriptionValue;
    }

    public void setPrescriptionValue(Double prescriptionValue) {
        this.prescriptionValue = prescriptionValue;
    }
    
    
    
}
