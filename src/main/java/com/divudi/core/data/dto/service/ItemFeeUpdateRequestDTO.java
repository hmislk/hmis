/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.service;

/**
 * DTO for updating an existing ItemFee.
 * Only non-null fields are applied.
 *
 * @author Buddhika
 */
public class ItemFeeUpdateRequestDTO {

    private String name;
    private String feeType;
    private Double fee;
    private Double ffee;
    private Boolean discountAllowed;
    private Long institutionId;
    private Long departmentId;
    private Long specialityId;
    private Long staffId;

    public ItemFeeUpdateRequestDTO() {
    }

    public boolean isValid() {
        return name != null || feeType != null || fee != null || ffee != null
                || discountAllowed != null || institutionId != null || departmentId != null
                || specialityId != null || staffId != null;
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

    public Boolean getDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(Boolean discountAllowed) {
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
