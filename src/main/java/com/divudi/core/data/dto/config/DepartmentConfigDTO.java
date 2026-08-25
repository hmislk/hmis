/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.config;

import java.io.Serializable;

/**
 * DTO for Department Configuration Option
 * Used to read department config option values
 *
 * @author Buddhika
 */
public class DepartmentConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long departmentId;
    private String departmentName;
    private String configKey;
    private String configValue;
    private String configDescription;

    public DepartmentConfigDTO() {
    }

    /**
     * Constructor for JPQL queries
     */
    public DepartmentConfigDTO(Long id, Long departmentId, String departmentName,
                              String configKey, String configValue) {
        this.id = id;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.configKey = configKey;
        this.configValue = configValue;
    }

    /**
     * Full constructor with description
     */
    public DepartmentConfigDTO(Long id, Long departmentId, String departmentName,
                              String configKey, String configValue, String configDescription) {
        this.id = id;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configDescription = configDescription;
    }

    // Getters and Setters

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

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigDescription() {
        return configDescription;
    }

    public void setConfigDescription(String configDescription) {
        this.configDescription = configDescription;
    }

    @Override
    public String toString() {
        return "DepartmentConfigDTO{" +
                "id=" + id +
                ", departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                ", configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", configDescription='" + configDescription + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentConfigDTO that = (DepartmentConfigDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
