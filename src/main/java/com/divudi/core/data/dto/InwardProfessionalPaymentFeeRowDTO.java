package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Flat, JPQL-projectable row for one charge-side {@code BillFee} on an inpatient
 * professional-fee bill (issue #22800). Grouping by admission and by
 * doctor+speciality happens in Java, not here — see
 * {@code InwardReportControllerBht.groupIntoDetailRows}.
 */
public class InwardProfessionalPaymentFeeRowDTO implements Serializable {

    private Long patientEncounterId;
    private Long staffId;
    private String staffName;
    private Long specialityId;
    private String specialityName;
    private Double feeValue;
    private Double paidValue;
    private String referenceBillDeptId;
    private Date referenceBillCreatedAt;

    public InwardProfessionalPaymentFeeRowDTO() {
    }

    public InwardProfessionalPaymentFeeRowDTO(Long patientEncounterId, Long staffId, String staffName,
            Long specialityId, String specialityName, Double feeValue, Double paidValue,
            String referenceBillDeptId, Date referenceBillCreatedAt) {
        this.patientEncounterId = patientEncounterId;
        this.staffId = staffId;
        this.staffName = staffName;
        this.specialityId = specialityId;
        this.specialityName = specialityName;
        this.feeValue = feeValue;
        this.paidValue = paidValue;
        this.referenceBillDeptId = referenceBillDeptId;
        this.referenceBillCreatedAt = referenceBillCreatedAt;
    }

    public Long getPatientEncounterId() {
        return patientEncounterId;
    }

    public void setPatientEncounterId(Long patientEncounterId) {
        this.patientEncounterId = patientEncounterId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Long getSpecialityId() {
        return specialityId;
    }

    public void setSpecialityId(Long specialityId) {
        this.specialityId = specialityId;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }

    public Double getFeeValue() {
        return feeValue;
    }

    public void setFeeValue(Double feeValue) {
        this.feeValue = feeValue;
    }

    public Double getPaidValue() {
        return paidValue;
    }

    public void setPaidValue(Double paidValue) {
        this.paidValue = paidValue;
    }

    public String getReferenceBillDeptId() {
        return referenceBillDeptId;
    }

    public void setReferenceBillDeptId(String referenceBillDeptId) {
        this.referenceBillDeptId = referenceBillDeptId;
    }

    public Date getReferenceBillCreatedAt() {
        return referenceBillCreatedAt;
    }

    public void setReferenceBillCreatedAt(Date referenceBillCreatedAt) {
        this.referenceBillCreatedAt = referenceBillCreatedAt;
    }
}
