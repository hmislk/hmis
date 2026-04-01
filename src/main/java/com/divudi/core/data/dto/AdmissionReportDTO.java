package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for the Admission Report (Issue #19640) and Admission by Consultant Report (Issue #19642).
 * One row per Admission (PatientEncounter subclass).
 *
 * Populated by a JPQL query in AdmissionReportController — avoids
 * lazy-load penalties that the old entity-based approach caused.
 */
public class AdmissionReportDTO implements Serializable {

    private Long admissionId;
    private String bhtNo;
    private String patientName;
    private String phone;
    private String address;
    private Date dateOfAdmission;
    private Date dateOfDischarge;
    private Date createdAt;
    private PaymentMethod paymentMethod;
    private String creditCompanyName;
    private String consultantName;
    private String medicalOfficerName;
    private String roomName;
    private boolean discharged;
    private boolean paymentFinalized;
    private AdmissionType admissionType;

    public AdmissionReportDTO() {
    }

    // -------------------------------------------------------------------------
    // Constructor used by JPQL SELECT NEW
    // paymentFinalized is Boolean (boxed) to survive NULL database values safely.
    // -------------------------------------------------------------------------

    public AdmissionReportDTO(
            Long admissionId,
            String bhtNo,
            String patientName,
            String phone,
            String address,
            Date dateOfAdmission,
            Date dateOfDischarge,
            Date createdAt,
            PaymentMethod paymentMethod,
            Institution creditCompany,
            Staff referringConsultant,
            Staff opdDoctor,
            String roomName,
            Boolean discharged,
            Boolean paymentFinalized,
            AdmissionType admissionType) {

        this.admissionId = admissionId;
        this.bhtNo = bhtNo;
        this.patientName = patientName != null ? patientName : "";
        this.phone = phone != null ? phone : "";
        this.address = address != null ? address : "";
        this.dateOfAdmission = dateOfAdmission;
        this.dateOfDischarge = dateOfDischarge;
        this.createdAt = createdAt;
        this.paymentMethod = paymentMethod;
        this.creditCompanyName = creditCompany != null ? creditCompany.getName() : "";
        this.consultantName = referringConsultant != null && referringConsultant.getPerson() != null
                ? referringConsultant.getPerson().getNameWithTitle() : "";
        this.medicalOfficerName = opdDoctor != null && opdDoctor.getPerson() != null
                ? opdDoctor.getPerson().getNameWithTitle() : "";
        this.roomName = roomName != null ? roomName : "";
        this.discharged = Boolean.TRUE.equals(discharged);
        this.paymentFinalized = Boolean.TRUE.equals(paymentFinalized);
        this.admissionType = admissionType;
    }

    // -------------------------------------------------------------------------
    // Derived helpers used by the XHTML
    // -------------------------------------------------------------------------

    /** Plain-text status string — used in print/export columns. */
    public String getDischargeStatusText() {
        return discharged ? "Discharged" : "Not Discharged";
    }

    /** Plain-text finalized status — used in print/export columns. */
    public String getPaymentStatusText() {
        return paymentFinalized ? "Finalized" : "Not Finalized";
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Date getDateOfAdmission() { return dateOfAdmission; }
    public void setDateOfAdmission(Date dateOfAdmission) { this.dateOfAdmission = dateOfAdmission; }

    public Date getDateOfDischarge() { return dateOfDischarge; }
    public void setDateOfDischarge(Date dateOfDischarge) { this.dateOfDischarge = dateOfDischarge; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCreditCompanyName() { return creditCompanyName != null ? creditCompanyName : ""; }
    public void setCreditCompanyName(String creditCompanyName) { this.creditCompanyName = creditCompanyName; }

    public String getConsultantName() { return consultantName != null ? consultantName : ""; }
    public void setConsultantName(String consultantName) { this.consultantName = consultantName; }

    public String getMedicalOfficerName() { return medicalOfficerName != null ? medicalOfficerName : ""; }
    public void setMedicalOfficerName(String medicalOfficerName) { this.medicalOfficerName = medicalOfficerName; }

    public String getRoomName() { return roomName != null ? roomName : ""; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public boolean isDischarged() { return discharged; }
    public void setDischarged(boolean discharged) { this.discharged = discharged; }

    public boolean isPaymentFinalized() { return paymentFinalized; }
    public void setPaymentFinalized(boolean paymentFinalized) { this.paymentFinalized = paymentFinalized; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }
}
