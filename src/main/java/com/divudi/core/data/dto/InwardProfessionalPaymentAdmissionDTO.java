package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Flat, JPQL-projectable row for one admission, used by the Inpatient
 * Professional Payment Report (issue #22800). Independent of whether the
 * admission has any professional fee entries, so admissions with none still
 * surface in the report.
 */
public class InwardProfessionalPaymentAdmissionDTO implements Serializable {

    private Long patientEncounterId;
    private String bhtNo;
    private Date dateOfAdmission;
    private Date dateOfDischarge;
    private String confirmedFinalBillDeptId;

    public InwardProfessionalPaymentAdmissionDTO() {
    }

    public InwardProfessionalPaymentAdmissionDTO(Long patientEncounterId, String bhtNo, Date dateOfAdmission,
            Date dateOfDischarge, String confirmedFinalBillDeptId) {
        this.patientEncounterId = patientEncounterId;
        this.bhtNo = bhtNo;
        this.dateOfAdmission = dateOfAdmission;
        this.dateOfDischarge = dateOfDischarge;
        this.confirmedFinalBillDeptId = confirmedFinalBillDeptId;
    }

    public Long getPatientEncounterId() {
        return patientEncounterId;
    }

    public void setPatientEncounterId(Long patientEncounterId) {
        this.patientEncounterId = patientEncounterId;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public void setDateOfAdmission(Date dateOfAdmission) {
        this.dateOfAdmission = dateOfAdmission;
    }

    public Date getDateOfDischarge() {
        return dateOfDischarge;
    }

    public void setDateOfDischarge(Date dateOfDischarge) {
        this.dateOfDischarge = dateOfDischarge;
    }

    public String getConfirmedFinalBillDeptId() {
        return confirmedFinalBillDeptId;
    }

    public void setConfirmedFinalBillDeptId(String confirmedFinalBillDeptId) {
        this.confirmedFinalBillDeptId = confirmedFinalBillDeptId;
    }
}
