/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

/**
 * DTO for updating an existing TimedItem. All fields optional — only non-null values are applied.
 *
 * @author Buddhika
 */
public class TimedItemUpdateRequestDTO {

    private String name;
    private String code;
    private String printName;
    private String fullName;
    private String departmentType;
    private String inwardChargeType;
    private Long departmentId;
    private Long institutionId;
    private Boolean inactive;

    public TimedItemUpdateRequestDTO() {
    }

    public boolean isValid() {
        return name != null || code != null || printName != null || fullName != null
                || departmentType != null || inwardChargeType != null
                || departmentId != null || institutionId != null || inactive != null;
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

    public String getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(String departmentType) {
        this.departmentType = departmentType;
    }

    public String getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(String inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }
}
