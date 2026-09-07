package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.inward.PatientEncounterType;
import com.divudi.core.entity.inward.RoomCategory;

public class ProfitMatrixRowDTO {

    private String invoiceNo;
    private String admissionNo;
    private String mrn;
    private String patientName;
    private String visitType;
    private String referringDoctorName;
    private Title title;
    private Double invoiceAmount;
    private Double finalAmount;
    private Double profitMargin;
    private RoomCategory roomCategory;

    private String serviceName;
    private InwardChargeType inwardChargeType;
    private String serviceDepartment;
    private Double serviceValue;
    private Double matrixPercentage;

    public ProfitMatrixRowDTO(String invoiceNo,
            String admissionNo,
            String mrn,
            String patientName,
            PatientEncounterType visitType,
            String referringDoctorName,
            double invoiceAmount,
            double finalAmount) {
        this(
                invoiceNo,
                admissionNo,
                mrn,
                patientName,
                visitType != null ? visitType.toString() : null,
                referringDoctorName,
                invoiceAmount,
                finalAmount
        );
    }

    public ProfitMatrixRowDTO(
            String invoiceNo,
            String admissionNo,
            String mrn,
            String patientName,
            PatientEncounterType visitType,
            String referringDoctorName,
            Double invoiceAmount,
            RoomCategory roomCategory,
            Double finalAmount
    ) {
        this.invoiceNo = invoiceNo;
        this.admissionNo = admissionNo;
        this.mrn = mrn;
        this.patientName = patientName;
        this.visitType = visitType != null ? visitType.toString() : null;
        this.referringDoctorName = referringDoctorName;
        this.invoiceAmount = invoiceAmount;
        this.roomCategory = roomCategory;
        this.finalAmount = finalAmount;

        if (invoiceAmount != null && finalAmount != null) {
            this.profitMargin = invoiceAmount - finalAmount;
            this.matrixPercentage = invoiceAmount != 0.0
                    ? (this.profitMargin * 100.0 / invoiceAmount)
                    : null;
        }
    }

    public ProfitMatrixRowDTO(String invoiceNo,
            String admissionNo,
            String mrn,
            String patientName,
            PatientEncounterType visitType,
            String referringDoctorName,
            String serviceName,
            String serviceDepartment,
            Double invoiceAmount,
            Double serviceValue,
            Double finalAmount) {

        this.invoiceNo = invoiceNo;
        this.admissionNo = admissionNo;
        this.mrn = mrn;
        this.patientName = patientName;
        this.visitType = visitType != null ? visitType.toString() : null;
        this.referringDoctorName = referringDoctorName;
        this.serviceName = serviceName;
        this.serviceDepartment = serviceDepartment;
        this.invoiceAmount = invoiceAmount;
        this.serviceValue = serviceValue;
        this.finalAmount = finalAmount;

        if (invoiceAmount != null && finalAmount != null) {
            this.profitMargin = invoiceAmount - finalAmount;
            this.matrixPercentage = invoiceAmount != 0.0
                    ? (this.profitMargin * 100.0 / invoiceAmount)
                    : null;
        } else {
            this.profitMargin = null;
            this.matrixPercentage = null;
        }
    }

    public ProfitMatrixRowDTO(String invoiceNo,
            String admissionNo,
            String mrn,
            String patientName,
            String visitType,
            String referringDoctorName,
            Double invoiceAmount,
            Double finalAmount) {
        this.invoiceNo = invoiceNo;
        this.admissionNo = admissionNo;
        this.mrn = mrn;
        this.patientName = patientName;
        this.visitType = visitType;
        this.referringDoctorName = referringDoctorName;
        this.invoiceAmount = invoiceAmount;
        this.finalAmount = finalAmount;
        this.profitMargin = (invoiceAmount != null && finalAmount != null)
                ? invoiceAmount - finalAmount
                : null;
    }

    public ProfitMatrixRowDTO(
            String invoiceNo,
            String admissionNo,
            String mrn,
            String patientName,
            String visitType,
            String referringDoctorName,
            Title title,
            String serviceName,
            InwardChargeType inwardChargeType,
            String serviceDepartment,
            Double invoiceAmount,
            Double serviceValue,
            Double finalAmount,
            Double profitMargin
    ) {
        this.invoiceNo = invoiceNo;
        this.admissionNo = admissionNo;
        this.mrn = mrn;
        this.patientName = patientName;
        this.visitType = visitType;
        this.referringDoctorName = referringDoctorName;
        this.title = title;
        this.serviceName = serviceName;
        this.inwardChargeType = inwardChargeType;
        this.serviceDepartment = serviceDepartment;
        this.invoiceAmount = invoiceAmount;
        this.serviceValue = serviceValue;
        this.finalAmount = finalAmount;
        this.profitMargin = profitMargin;

        if (this.serviceValue != null && this.profitMargin != null) {
            this.matrixPercentage = (this.profitMargin/ this.serviceValue) * 100;
        } else {
            this.matrixPercentage = 0.0;
        }
    }

    public ProfitMatrixRowDTO(
            String invoiceNo,
            String admissionNo,
            String mrn,
            String patientName,
            String visitType,
            String referringDoctorName,
            Title title,
            Double invoiceAmount,
            RoomCategory roomCategory,
            Double finalAmount
    ) {
        this.invoiceNo = invoiceNo;
        this.admissionNo = admissionNo;
        this.mrn = mrn;
        this.patientName = patientName;
        this.visitType = visitType;
        this.referringDoctorName = referringDoctorName;
        this.title = title;
        this.invoiceAmount = invoiceAmount;
        this.roomCategory = roomCategory;
        this.finalAmount = finalAmount;

        if (this.invoiceAmount != null && this.finalAmount != null) {
            this.profitMargin = this.finalAmount - this.invoiceAmount;
        } else {
            this.profitMargin = 0.0;
        }
    }



    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getAdmissionNo() {
        return admissionNo;
    }

    public String getMrn() {
        return mrn;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getVisitType() {
        return visitType;
    }

    public String getReferringDoctorName() {
        return referringDoctorName;
    }

    public Double getInvoiceAmount() {
        return invoiceAmount;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public Double getProfitMargin() {
        return profitMargin;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceDepartment() {
        return serviceDepartment;
    }

    public Double getServiceValue() {
        return serviceValue;
    }

    public Double getMatrixPercentage() {
        return matrixPercentage;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
    }

    public Title getTitle() {
        return title;
    }

    public void setInvoiceAmount(Double invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public void setProfitMargin(Double profitMargin) {
        this.profitMargin = profitMargin;
    }

    public void setMatrixPercentage(Double matrixPercentage) {
        this.matrixPercentage = matrixPercentage;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }
}
