/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.pricing;

/**
 * DTO representing a single ItemFee associated with a collecting centre.
 * Extends the data in ItemFeeDTO by including item and collecting centre
 * identification fields, since these fees are queried cross-item.
 */
public class CollectingCentreFeeDTO {

    private Long id;
    private String name;
    private String feeType;
    private Double fee;
    private Double ffee;
    private boolean discountAllowed;
    private boolean retired;

    // The collecting centre (forInstitution)
    private Long collectingCentreId;
    private String collectingCentreName;
    private String collectingCentreCode;

    // The item this fee belongs to
    private Long itemId;
    private String itemName;
    private String itemCode;

    // Contextual fee modifiers (institution, department, speciality, staff)
    private Long institutionId;
    private String institutionName;
    private Long departmentId;
    private String departmentName;
    private Long specialityId;
    private String specialityName;
    private Long staffId;
    private String staffName;

    public CollectingCentreFeeDTO() {
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

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public Long getCollectingCentreId() {
        return collectingCentreId;
    }

    public void setCollectingCentreId(Long collectingCentreId) {
        this.collectingCentreId = collectingCentreId;
    }

    public String getCollectingCentreName() {
        return collectingCentreName;
    }

    public void setCollectingCentreName(String collectingCentreName) {
        this.collectingCentreName = collectingCentreName;
    }

    public String getCollectingCentreCode() {
        return collectingCentreCode;
    }

    public void setCollectingCentreCode(String collectingCentreCode) {
        this.collectingCentreCode = collectingCentreCode;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
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
}
