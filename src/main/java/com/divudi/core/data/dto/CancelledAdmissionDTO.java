package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Cancelled Admissions report (#25745). Built by a JPQL
 * SELECT NEW in CancelledAdmissionReportController. Admissions cancelled before
 * the cancellation fields existed have blank reason and decision makers.
 */
public class CancelledAdmissionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long admissionId;
    private Date cancelledAt;
    private String bhtNo;
    private String patientName;
    private String patientPhn;
    private String admissionTypeName;
    private Date dateOfAdmission;
    private String admissionDecidedBy;
    private String cancellationReason;
    private String notes;
    private String cancellationDecidedBy;
    private String cancelledBy;

    public CancelledAdmissionDTO() {
    }

    public CancelledAdmissionDTO(
            Long admissionId,
            Date cancelledAt,
            String bhtNo,
            String patientName,
            String patientPhn,
            String admissionTypeName,
            Date dateOfAdmission,
            String admissionDecidedBy,
            String cancellationReason,
            String notes,
            String cancellationDecidedBy,
            String cancelledBy) {
        this.admissionId = admissionId;
        this.cancelledAt = cancelledAt;
        this.bhtNo = bhtNo;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.admissionTypeName = admissionTypeName;
        this.dateOfAdmission = dateOfAdmission;
        this.admissionDecidedBy = admissionDecidedBy;
        this.cancellationReason = cancellationReason;
        this.notes = notes;
        this.cancellationDecidedBy = cancellationDecidedBy;
        this.cancelledBy = cancelledBy;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientPhn() {
        return patientPhn;
    }

    public String getAdmissionTypeName() {
        return admissionTypeName == null ? "" : admissionTypeName;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public String getAdmissionDecidedBy() {
        return admissionDecidedBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getNotes() {
        return notes;
    }

    public String getCancellationDecidedBy() {
        return cancellationDecidedBy;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }
}
