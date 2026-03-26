/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.service;

import java.util.Date;
import java.util.List;

/**
 * Full response DTO for a Service or InwardService
 *
 * @author Buddhika
 */
public class ServiceResponseDTO {

    private Long id;
    private String name;
    private String code;
    private String printName;
    private String fullName;
    private String serviceType; // OPD or Inward
    private Double total;
    private Double totalForForeigner;
    private boolean inactive;
    private boolean retired;
    private boolean discountAllowed;
    private boolean userChangable;
    private boolean chargesVisibleForInward;
    private boolean marginNotAllowed;
    private boolean requestForQuentity;
    private boolean patientNotRequired;
    private String inwardChargeType;
    private Long categoryId;
    private String categoryName;
    private Long institutionId;
    private String institutionName;
    private Long departmentId;
    private String departmentName;
    private Date createdAt;
    private List<ItemFeeDTO> fees;
    private String message;

    public ServiceResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getTotalForForeigner() {
        return totalForForeigner;
    }

    public void setTotalForForeigner(Double totalForForeigner) {
        this.totalForForeigner = totalForForeigner;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
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

    public String getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(String inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<ItemFeeDTO> getFees() {
        return fees;
    }

    public void setFees(List<ItemFeeDTO> fees) {
        this.fees = fees;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
