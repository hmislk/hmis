package com.divudi.core.data.dto;

import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
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
    private AdmissionType admissionType;
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
            AdmissionType admissionType,
            Date dateOfAdmission,
            Staff admissionDecidedBy,
            String cancellationReason,
            String notes,
            Staff cancellationDecidedBy,
            WebUser cancelledBy) {
        this.admissionId = admissionId;
        this.cancelledAt = cancelledAt;
        this.bhtNo = bhtNo;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.admissionType = admissionType;
        this.dateOfAdmission = dateOfAdmission;
        this.admissionDecidedBy = staffName(admissionDecidedBy);
        this.cancellationReason = cancellationReason;
        this.notes = notes;
        this.cancellationDecidedBy = staffName(cancellationDecidedBy);
        this.cancelledBy = cancelledBy == null ? null
                : cancelledBy.getWebUserPerson() != null ? cancelledBy.getWebUserPerson().getName()
                : cancelledBy.getName();
    }

    private static String staffName(Staff staff) {
        return staff == null || staff.getPerson() == null ? null : staff.getPerson().getNameWithTitle();
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

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public String getAdmissionTypeName() {
        return admissionType == null ? "" : admissionType.getName();
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
