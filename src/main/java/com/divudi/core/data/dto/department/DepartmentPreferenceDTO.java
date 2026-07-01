/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.department;

import java.io.Serializable;

/**
 * DTO for department-scoped {@code UserPreference} settings exposed over REST.
 *
 * Used both as the GET response body and the PUT request body for
 * {@code /api/departments/{id}/preferences}. Item-listing strategy fields are
 * carried as {@code String} enum names so a PUT can distinguish "field not
 * supplied" (null) from an explicit value, enabling partial updates.
 *
 * @author Buddhika
 */
public class DepartmentPreferenceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long departmentId;
    private String departmentName;

    /**
     * False when the department has no persisted UserPreference yet and the
     * returned strategy values are the entity defaults.
     */
    private Boolean preferenceExists;

    private String opdItemListingStrategy;
    private String ccItemListingStrategy;
    private String inwardItemListingStrategy;

    public DepartmentPreferenceDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getPreferenceExists() {
        return preferenceExists;
    }

    public void setPreferenceExists(Boolean preferenceExists) {
        this.preferenceExists = preferenceExists;
    }

    public String getOpdItemListingStrategy() {
        return opdItemListingStrategy;
    }

    public void setOpdItemListingStrategy(String opdItemListingStrategy) {
        this.opdItemListingStrategy = opdItemListingStrategy;
    }

    public String getCcItemListingStrategy() {
        return ccItemListingStrategy;
    }

    public void setCcItemListingStrategy(String ccItemListingStrategy) {
        this.ccItemListingStrategy = ccItemListingStrategy;
    }

    public String getInwardItemListingStrategy() {
        return inwardItemListingStrategy;
    }

    public void setInwardItemListingStrategy(String inwardItemListingStrategy) {
        this.inwardItemListingStrategy = inwardItemListingStrategy;
    }
}
