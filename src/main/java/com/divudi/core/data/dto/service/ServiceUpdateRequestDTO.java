/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.service;

/**
 * DTO for updating an existing Service or InwardService.
 * Only non-null fields are applied to the entity.
 *
 * @author Buddhika
 */
public class ServiceUpdateRequestDTO {

    private String name;
    private String code;
    private String printName;
    private String fullName;
    private Long categoryId;
    private Long institutionId;
    private Long departmentId;
    private String inwardChargeType;
    private Boolean inactive;
    private Boolean discountAllowed;
    private Boolean userChangable;
    private Boolean chargesVisibleForInward;
    private Boolean marginNotAllowed;
    private Boolean requestForQuentity;
    private Boolean patientNotRequired;

    public ServiceUpdateRequestDTO() {
    }

    public boolean isValid() {
        // At least one field must be set
        return name != null || code != null || printName != null || fullName != null
                || categoryId != null || institutionId != null || departmentId != null
                || inwardChargeType != null || inactive != null || discountAllowed != null
                || userChangable != null || chargesVisibleForInward != null
                || marginNotAllowed != null || requestForQuentity != null
                || patientNotRequired != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPrintName() {
        return printName;
    }

    public void setPrintName(String printName) {
        this.printName = printName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public String getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(String inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }

    public Boolean getDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(Boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    public Boolean getUserChangable() {
        return userChangable;
    }

    public void setUserChangable(Boolean userChangable) {
        this.userChangable = userChangable;
    }

    public Boolean getChargesVisibleForInward() {
        return chargesVisibleForInward;
    }

    public void setChargesVisibleForInward(Boolean chargesVisibleForInward) {
        this.chargesVisibleForInward = chargesVisibleForInward;
    }

    public Boolean getMarginNotAllowed() {
        return marginNotAllowed;
    }

    public void setMarginNotAllowed(Boolean marginNotAllowed) {
        this.marginNotAllowed = marginNotAllowed;
    }

    public Boolean getRequestForQuentity() {
        return requestForQuentity;
    }

    public void setRequestForQuentity(Boolean requestForQuentity) {
        this.requestForQuentity = requestForQuentity;
    }

    public Boolean getPatientNotRequired() {
        return patientNotRequired;
    }

    public void setPatientNotRequired(Boolean patientNotRequired) {
        this.patientNotRequired = patientNotRequired;
    }
}
