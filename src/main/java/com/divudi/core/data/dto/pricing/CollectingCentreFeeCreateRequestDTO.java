/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.pricing;

/**
 * DTO for creating a new ItemFee for a collecting centre.
 * Both collectingCentreId and itemId are required.
 * For feeType OwnInstitution, OtherInstitution, or Referral, departmentId is also required.
 */
public class CollectingCentreFeeCreateRequestDTO {

    private Long collectingCentreId; // required — Institution with institutionType=CollectingCentre
    private Long itemId;             // required — the Item (service/investigation) this fee covers
    private String name;             // required
    private String feeType;          // required — FeeType enum value
    private Double fee;              // required — local fee amount
    private Double ffee;             // optional, defaults to fee if null or 0
    private boolean discountAllowed;
    private Long institutionId;      // optional — contextual institution
    private Long departmentId;       // required for OwnInstitution/OtherInstitution/Referral fee types
    private Long specialityId;       // optional — for Staff fee type
    private Long staffId;            // optional — for Staff fee type

    public CollectingCentreFeeCreateRequestDTO() {
    }

    public boolean isValid() {
        return collectingCentreId != null
                && itemId != null
                && name != null && !name.trim().isEmpty()
                && feeType != null && !feeType.trim().isEmpty()
                && fee != null;
    }

    public Long getCollectingCentreId() {
        return collectingCentreId;
    }

    public void setCollectingCentreId(Long collectingCentreId) {
        this.collectingCentreId = collectingCentreId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
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
