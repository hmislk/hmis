package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
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
    private Date addedFeeDate;
    private Title staffTitle;

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

    // Issue #22803 - Detailed report needs the charge-side BillFee.createdAt
    // (when the professional fee was added) alongside everything the
    // existing constructor already carries. New constructor only - the one
    // above stays for backward compatibility.
    public InwardProfessionalPaymentFeeRowDTO(Long patientEncounterId, Long staffId, String staffName,
            Long specialityId, String specialityName, Double feeValue, Double paidValue,
            String referenceBillDeptId, Date referenceBillCreatedAt, Date addedFeeDate) {
        this(patientEncounterId, staffId, staffName, specialityId, specialityName, feeValue, paidValue,
                referenceBillDeptId, referenceBillCreatedAt);
        this.addedFeeDate = addedFeeDate;
    }

    // Carries the doctor's Person.title (Dr, Prof, ...) so the report can show
    // it before the consultant name. New constructor only - the ones above
    // stay for backward compatibility.
    public InwardProfessionalPaymentFeeRowDTO(Long patientEncounterId, Long staffId, String staffName,
            Long specialityId, String specialityName, Double feeValue, Double paidValue,
            String referenceBillDeptId, Date referenceBillCreatedAt, Date addedFeeDate, Title staffTitle) {
        this(patientEncounterId, staffId, staffName, specialityId, specialityName, feeValue, paidValue,
                referenceBillDeptId, referenceBillCreatedAt, addedFeeDate);
        this.staffTitle = staffTitle;
    }

    // Consultant name prefixed with the title, e.g. "Dr. A Perera".
    public String getStaffNameWithTitle() {
        if (staffName == null || staffName.trim().isEmpty()) {
            return "";
        }
        return staffTitle != null ? staffTitle.getLabel().trim() + " " + staffName.trim() : staffName.trim();
    }

    public Title getStaffTitle() {
        return staffTitle;
    }

    public void setStaffTitle(Title staffTitle) {
        this.staffTitle = staffTitle;
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

    public Date getAddedFeeDate() {
        return addedFeeDate;
    }

    public void setAddedFeeDate(Date addedFeeDate) {
        this.addedFeeDate = addedFeeDate;
    }
}
