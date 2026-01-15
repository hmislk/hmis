/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data.dto;

import com.divudi.core.entity.Staff;

/**
 *
 * @author OVS
 */
public class InwardIncomeDoctorSpecialtyDTO {
    
    private Long staffId;
    private String doctorName;
    private String specialtyName;
    
    private Double docFee;
    private Double hosFee;
    
    private Long billId;
    private Double billTotal;
    
    private Double docFeeTotal;
    private Double hosFeeTotal;
    private Double totalCharge;
    
    
    // constructor
    public InwardIncomeDoctorSpecialtyDTO(){
        this.docFee = 0.0;
        this.hosFee = 0.0;
        this.billTotal = 0.0;
    }
    
    public InwardIncomeDoctorSpecialtyDTO(Long staffId, String doctorName, String specialtyName, Double docFee, Double hosFee, Long billId, Double billTotal) {
        this.staffId = staffId;
        this.doctorName = doctorName;
        this.specialtyName = specialtyName;
        this.docFee = docFee;
        this.hosFee = hosFee;
        this.billId = billId;
        this.billTotal = billTotal;
        
        System.out.println("billId + billTotal = " + billId + billTotal);
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

    public Double getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(Double billTotal) {
        this.billTotal = billTotal;
    }
    
    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }
    
    public Double getDocFeeTotal() {
        return docFeeTotal;
    }

    public void setDocFeeTotal(Double docFeeTotal) {
        this.docFeeTotal = docFeeTotal;
    }

    public Double getHosFeeTotal() {
        return hosFeeTotal;
    }

    public void setHosFeeTotal(Double hosFeeTotal) {
        this.hosFeeTotal = hosFeeTotal;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }

}
