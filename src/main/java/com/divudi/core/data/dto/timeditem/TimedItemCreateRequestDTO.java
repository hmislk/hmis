/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

/**
 * DTO for creating a new TimedItem.
 *
 * @author Buddhika
 */
public class TimedItemCreateRequestDTO {

    private String name; // required
    private String code; // optional, auto-generated from name if omitted
    private String printName;
    private String fullName;
    private String departmentType; // required (e.g. "Inward", "Theatre")
    private String inwardChargeType; // required
    private Long departmentId; // optional
    private Long institutionId; // optional
    private boolean inactive = false;

    public TimedItemCreateRequestDTO() {
    }

    public boolean isValid() {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (departmentType == null || departmentType.trim().isEmpty()) {
            return false;
        }
        if (inwardChargeType == null || inwardChargeType.trim().isEmpty()) {
            return false;
        }
        return true;
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

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }
}
