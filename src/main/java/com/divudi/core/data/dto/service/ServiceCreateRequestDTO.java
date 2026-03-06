/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.service;

/**
 * DTO for creating a new Service or InwardService
 *
 * @author Buddhika
 */
public class ServiceCreateRequestDTO {

    private String serviceType; // "OPD" or "Inward" (required)
    private String name; // required
    private String code; // optional, auto-generated if not provided
    private String printName;
    private String fullName;
    private Long categoryId;
    private Long institutionId;
    private Long departmentId;
    private String inwardChargeType; // required when serviceType=Inward
    private boolean inactive = false;
    private boolean discountAllowed;
    private boolean userChangable;
    private boolean chargesVisibleForInward;
    private boolean marginNotAllowed;
    private boolean requestForQuentity;
    private boolean patientNotRequired;

    public ServiceCreateRequestDTO() {
    }

    public boolean isValid() {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return false;
        }
        if (!serviceType.equalsIgnoreCase("OPD") && !serviceType.equalsIgnoreCase("Inward")) {
            return false;
        }
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (serviceType.equalsIgnoreCase("Inward")
                && (inwardChargeType == null || inwardChargeType.trim().isEmpty())) {
            return false;
        }
        return true;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
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

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public boolean isDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    public boolean isUserChangable() {
        return userChangable;
    }

    public void setUserChangable(boolean userChangable) {
        this.userChangable = userChangable;
    }

    public boolean isChargesVisibleForInward() {
        return chargesVisibleForInward;
    }

    public void setChargesVisibleForInward(boolean chargesVisibleForInward) {
        this.chargesVisibleForInward = chargesVisibleForInward;
    }

    public boolean isMarginNotAllowed() {
        return marginNotAllowed;
    }

    public void setMarginNotAllowed(boolean marginNotAllowed) {
        this.marginNotAllowed = marginNotAllowed;
    }

    public boolean isRequestForQuentity() {
        return requestForQuentity;
    }

    public void setRequestForQuentity(boolean requestForQuentity) {
        this.requestForQuentity = requestForQuentity;
    }

    public boolean isPatientNotRequired() {
        return patientNotRequired;
    }

    public void setPatientNotRequired(boolean patientNotRequired) {
        this.patientNotRequired = patientNotRequired;
    }
}
