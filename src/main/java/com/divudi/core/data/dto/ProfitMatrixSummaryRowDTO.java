package com.divudi.core.data.dto;

import com.divudi.core.data.inward.PatientEncounterType;

public class ProfitMatrixSummaryRowDTO {

    private String invoiceNo;
    private String admissionNo;
    private String mrn;
    private String patientName;
    private String visitType;
    private String referringDoctorName;
    private Double invoiceAmount;
    private Double finalAmount;
    private Double profitMargin;

    public ProfitMatrixSummaryRowDTO(String invoiceNo,
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

    public ProfitMatrixSummaryRowDTO(String invoiceNo,
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
}
