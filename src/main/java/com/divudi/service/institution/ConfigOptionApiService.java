/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.institution;

import com.divudi.core.data.dto.config.DepartmentConfigDTO;
import com.divudi.core.data.dto.config.DepartmentConfigUpdateDTO;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ConfigOptionFacade;
import com.divudi.core.facade.DepartmentFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for ConfigOption API operations
 * Provides business logic for department configuration management
 * Only allows updating existing config options (predefined keys only)
 * Follows patterns from PharmacySearchApiService and PharmacyAdjustmentApiService
 *
 * @author Buddhika
 */
@Stateless
public class ConfigOptionApiService implements Serializable {

    @EJB
    private ConfigOptionFacade configOptionFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    /**
     * Get all configuration options for a department
     * Uses DTO constructor queries for optimal performance
     */
    public List<DepartmentConfigDTO> getDepartmentConfigs(Long departmentId) throws Exception {
        if (departmentId == null) {
            throw new Exception("Department ID is required");
        }

        // Validate department exists
        Department department = loadAndValidateDepartment(departmentId);

        Map<String, Object> params = new HashMap<>();
        params.put("departmentId", departmentId);

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.config.DepartmentConfigDTO(")
            .append("c.id, c.department.id, c.department.name, c.optionKey, c.optionValue) ")
            .append("FROM ConfigOption c ")
            .append("WHERE c.retired = false ")
            .append("AND c.department.id = :departmentId ")
            .append("ORDER BY c.optionKey");

        @SuppressWarnings("unchecked")
        List<DepartmentConfigDTO> results = (List<DepartmentConfigDTO>) configOptionFacade.findLightsByJpql(
            jpql.toString(), params, TemporalType.TIMESTAMP);

        return results;
    }

    /**
     * Update department configuration option value
     * Only updates existing config options (predefined keys only)
     * Does not create new config options
     */
    public DepartmentConfigDTO updateDepartmentConfig(DepartmentConfigUpdateDTO request, WebUser user) throws Exception {
        validateUpdateRequest(request);

        if (user == null) {
            throw new Exception("User is required for updating department config");
        }

        // Validate department exists
        Department department = loadAndValidateDepartment(request.getDepartmentId());

        // Find existing config option for this department and key
        ConfigOption configOption = findConfigOption(request.getDepartmentId(), request.getConfigKey());

        if (configOption == null) {
            throw new Exception("Configuration option '" + request.getConfigKey() +
                              "' not found for department. Only predefined config keys can be updated.");
        }

        // Update config value
        String oldValue = configOption.getOptionValue();
        configOption.setOptionValue(request.getConfigValue());

        // Save updated config option
        configOptionFacade.edit(configOption);

        // Build and return response DTO
        DepartmentConfigDTO response = new DepartmentConfigDTO();
        response.setId(configOption.getId());
        response.setDepartmentId(department.getId());
        response.setDepartmentName(department.getName());
        response.setConfigKey(configOption.getOptionKey());
        response.setConfigValue(configOption.getOptionValue());
        response.setConfigDescription("Updated from '" + oldValue + "' to '" + request.getConfigValue() + "'");

        return response;
    }

    // Private helper methods

    /**
     * Validate update request
     */
    private void validateUpdateRequest(DepartmentConfigUpdateDTO request) throws Exception {
        if (request == null || !request.isValid()) {
            throw new Exception("Valid update request is required with departmentId, configKey, and configValue");
        }

        if (request.getConfigKey().trim().isEmpty()) {
            throw new Exception("Config key cannot be empty");
        }
    }

    /**
     * Load and validate department
     */
    private Department loadAndValidateDepartment(Long departmentId) throws Exception {
        Department department = departmentFacade.find(departmentId);
        if (department == null) {
            throw new Exception("Department not found with ID: " + departmentId);
        }
        if (department.isRetired()) {
            throw new Exception("Department is retired");
        }
        return department;
    }

    /**
     * Find config option by department and key
     */
    private ConfigOption findConfigOption(Long departmentId, String configKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("departmentId", departmentId);
        params.put("configKey", configKey.trim());

        String jpql = "SELECT c FROM ConfigOption c " +
                     "WHERE c.retired = false " +
                     "AND c.department.id = :departmentId " +
                     "AND c.optionKey = :configKey";

        List<ConfigOption> results = configOptionFacade.findByJpql(jpql, params, 1);
        return results.isEmpty() ? null : results.get(0);
    }
}
