/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.service;

/**
 * DTO for adding a new fee to a service
 *
 * @author Buddhika
 */
public class ItemFeeCreateRequestDTO {

    private String name; // required
    private String feeType; // required - FeeType enum value
    private Double fee; // required
    private Double ffee; // optional, defaults to fee if 0
    private boolean discountAllowed;
    private Long institutionId; // required for OwnInstitution/OtherInstitution/Referral/CollectingCentre
    private Long departmentId;
    private Long specialityId; // for Staff fee type
    private Long staffId; // for Staff fee type

    public ItemFeeCreateRequestDTO() {
    }

    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
                && feeType != null && !feeType.trim().isEmpty()
                && fee != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }

    public Double getFfee() {
        return ffee;
    }

    public void setFfee(Double ffee) {
        this.ffee = ffee;
    }

    public boolean isDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getSpecialityId() {
        return specialityId;
    }

    public void setSpecialityId(Long specialityId) {
        this.specialityId = specialityId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
}
