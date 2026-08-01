package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class SurgeryCostEstimationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Internal keys
    private Long surgeryBillId;
    private Long procedureId;

    // Patient
    private String phn;
    private String patientName;

    // Admission
    private String admissionNo;
    private Date admissionDate;
    private String bedNo;

    // Surgery
    private String surgeonName;
    private String assistantSurgeonName;
    private String surgeryTypeName;
    private String serviceName;
    private String otRoomName;
    private String surgeryStatusLabel;

    // Amounts
    private Double roomCharges;
    private Double drugCharges;
    private Double totalHospitalCharge;
    private Double professionalCharge;
    private Double totalAmount;
    private Double billDiscount;
    private Double netAmount;

    private Long admissionId;
    
    // New constructor matching optimized JPQL projection
    public SurgeryCostEstimationDTO(Long surgeryBillId, Long procedureId, String phn, String patientName,
                                    String admissionNo, Date admissionDate, String bedNo,
                                    String serviceName, String surgeryTypeName, Long admissionId,
                                    com.divudi.core.data.Title billStaffTitle, String billStaffName) {
        this.surgeryBillId = surgeryBillId;
        this.procedureId = procedureId;
        this.phn = phn;
        this.patientName = patientName;
        this.admissionNo = admissionNo;
        this.admissionDate = admissionDate;
        this.bedNo = bedNo;
        this.serviceName = serviceName;
        this.surgeryTypeName = surgeryTypeName;
        this.admissionId = admissionId;
        this.surgeonName = billStaffTitle != null ? billStaffTitle.toString() : billStaffName;
    }

    // Constructor matching JPQL projection
    public SurgeryCostEstimationDTO(Long surgeryBillId, Long procedureId, String phn, String patientName,
                                    String admissionNo, Date admissionDate, String bedNo,
                                    String serviceName, String surgeryTypeName) {
        this.surgeryBillId = surgeryBillId;
        this.procedureId = procedureId;
        this.phn = phn;
        this.patientName = patientName;
        this.admissionNo = admissionNo;
        this.admissionDate = admissionDate;
        this.bedNo = bedNo;
        this.serviceName = serviceName;
        this.surgeryTypeName = surgeryTypeName;
    }

    public Long getSurgeryBillId() {
        return surgeryBillId;
    }

    public void setSurgeryBillId(Long surgeryBillId) {
        this.surgeryBillId = surgeryBillId;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public Long getProcedureId() {
        return procedureId;
    }

    public void setProcedureId(Long procedureId) {
        this.procedureId = procedureId;
    }

    public String getPhn() {
        return phn;
    }

    public void setPhn(String phn) {
        this.phn = phn;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getAdmissionNo() {
        return admissionNo;
    }

    public void setAdmissionNo(String admissionNo) {
        this.admissionNo = admissionNo;
    }

    public Date getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(Date admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getBedNo() {
        return bedNo;
    }

    public void setBedNo(String bedNo) {
        this.bedNo = bedNo;
    }

    public String getSurgeonName() {
        return surgeonName;
    }

    public void setSurgeonName(String surgeonName) {
        this.surgeonName = surgeonName;
    }

    public String getAssistantSurgeonName() {
        return assistantSurgeonName;
    }

    public void setAssistantSurgeonName(String assistantSurgeonName) {
        this.assistantSurgeonName = assistantSurgeonName;
    }

    public String getSurgeryTypeName() {
        return surgeryTypeName;
    }

    public void setSurgeryTypeName(String surgeryTypeName) {
        this.surgeryTypeName = surgeryTypeName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getOtRoomName() {
        return otRoomName;
    }

    public void setOtRoomName(String otRoomName) {
        this.otRoomName = otRoomName;
    }

    public String getSurgeryStatusLabel() {
        return surgeryStatusLabel;
    }

    public void setSurgeryStatusLabel(String surgeryStatusLabel) {
        this.surgeryStatusLabel = surgeryStatusLabel;
    }

    public Double getRoomCharges() {
        return roomCharges;
    }

    public void setRoomCharges(Double roomCharges) {
        this.roomCharges = roomCharges;
    }

    public Double getDrugCharges() {
        return drugCharges;
    }

    public void setDrugCharges(Double drugCharges) {
        this.drugCharges = drugCharges;
    }

    public Double getTotalHospitalCharge() {
        return totalHospitalCharge;
    }

    public void setTotalHospitalCharge(Double totalHospitalCharge) {
        this.totalHospitalCharge = totalHospitalCharge;
    }

    public Double getProfessionalCharge() {
        return professionalCharge;
    }

    public void setProfessionalCharge(Double professionalCharge) {
        this.professionalCharge = professionalCharge;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getBillDiscount() {
        return billDiscount;
    }

    public void setBillDiscount(Double billDiscount) {
        this.billDiscount = billDiscount;
    }

    public Double getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Double netAmount) {
        this.netAmount = netAmount;
    }
}
