package com.divudi.core.data.dto;

import com.divudi.core.data.inward.PatientEncounterType;

public class ProfitMatrixRowDTO {

    private String invoiceNo;
    private String admissionNo;
    private String mrn;
    private String patientName;
    private String visitType;
    private String referringDoctorName;
    private Double invoiceAmount;
    private Double finalAmount;
    private Double profitMargin;

    private String serviceName;
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
}
