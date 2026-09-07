/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.institution;

import java.io.Serializable;

/**
 * DTO for updating Institution parent relationship
 * Used to change or remove parent institution
 *
 * @author Buddhika
 */
public class InstitutionRelationshipUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long institutionId; // Required - the institution to update
    private Long parentInstitutionId; // Optional - null to remove parent relationship

    public InstitutionRelationshipUpdateDTO() {
    }

    public InstitutionRelationshipUpdateDTO(Long institutionId, Long parentInstitutionId) {
        this.institutionId = institutionId;
        this.parentInstitutionId = parentInstitutionId;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return institutionId != null;
    }

    // Getters and Setters

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Long getParentInstitutionId() {
        return parentInstitutionId;
    }

    public void setParentInstitutionId(Long parentInstitutionId) {
        this.parentInstitutionId = parentInstitutionId;
    }

    @Override
    public String toString() {
        return "InstitutionRelationshipUpdateDTO{" +
                "institutionId=" + institutionId +
                ", parentInstitutionId=" + parentInstitutionId +
                '}';
    }
}
