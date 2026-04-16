/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.department;

import java.io.Serializable;

/**
 * DTO for updating Department relationships
 * Used to change institution, site, or super department
 *
 * @author Buddhika
 */
public class DepartmentRelationshipUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long departmentId; // Required - the department to update
    private Long institutionId; // Optional - update if provided
    private Long siteId; // Optional - update if provided, null to remove
    private Long superDepartmentId; // Optional - update if provided, null to remove

    public DepartmentRelationshipUpdateDTO() {
    }

    public DepartmentRelationshipUpdateDTO(Long departmentId, Long institutionId,
                                          Long siteId, Long superDepartmentId) {
        this.departmentId = departmentId;
        this.institutionId = institutionId;
        this.siteId = siteId;
        this.superDepartmentId = superDepartmentId;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return departmentId != null;
    }

    // Getters and Setters

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

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public Long getSuperDepartmentId() {
        return superDepartmentId;
    }

    public void setSuperDepartmentId(Long superDepartmentId) {
        this.superDepartmentId = superDepartmentId;
    }

    @Override
    public String toString() {
        return "DepartmentRelationshipUpdateDTO{" +
                "departmentId=" + departmentId +
                ", institutionId=" + institutionId +
                ", siteId=" + siteId +
                ", superDepartmentId=" + superDepartmentId +
                '}';
    }
}
