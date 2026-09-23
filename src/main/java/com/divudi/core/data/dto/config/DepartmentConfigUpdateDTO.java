/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.config;

import java.io.Serializable;

/**
 * DTO for updating Department Configuration Option values
 * Used to update department config option values
 *
 * @author Buddhika
 */
public class DepartmentConfigUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long departmentId; // Required
    private String configKey; // Required
    private String configValue; // Required - new value to set
    private String configValueType; // Optional - OptionValueType name, only used when creating a new key

    public DepartmentConfigUpdateDTO() {
    }

    public DepartmentConfigUpdateDTO(Long departmentId, String configKey, String configValue) {
        this.departmentId = departmentId;
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public DepartmentConfigUpdateDTO(Long departmentId, String configKey, String configValue, String configValueType) {
        this(departmentId, configKey, configValue);
        this.configValueType = configValueType;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return departmentId != null &&
               configKey != null && !configKey.trim().isEmpty() &&
               configValue != null;
    }

    // Getters and Setters

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
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

    public String getConfigValueType() {
        return configValueType;
    }

    public void setConfigValueType(String configValueType) {
        this.configValueType = configValueType;
    }

    @Override
    public String toString() {
        return "DepartmentConfigUpdateDTO{" +
                "departmentId=" + departmentId +
                ", configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", configValueType='" + configValueType + '\'' +
                '}';
    }
}
