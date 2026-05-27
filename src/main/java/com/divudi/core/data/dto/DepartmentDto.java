package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for Department management operations. Provides lightweight representation
 * for autocomplete selection and management pages.
 *
 * @author Dr M H B Ariyaratne
 */
public class DepartmentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String departmentCode;
    private Boolean retired;
    private Boolean inactive;

    public DepartmentDto() {
    }

    /**
     * Constructor for JPQL queries
     */
    public DepartmentDto(Long id, String name, String code, String departmentCode,
            Boolean retired, Boolean inactive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.departmentCode = departmentCode;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
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

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public Boolean getRetired() {
        return retired;
    }

    public boolean isRetired() {
        return retired != null && retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public boolean isInactive() {
        return inactive != null && inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }

    public String getStatusDisplay() {
        return isInactive() ? "Inactive" : "Active";
    }

    public String getStatusCssClass() {
        return isInactive() ? "badge-danger" : "badge-success";
    }

    @Override
    public String toString() {
        return "DepartmentDto{id=" + id + ", name='" + name + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentDto that = (DepartmentDto) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
