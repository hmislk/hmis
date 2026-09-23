/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.institution;

import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
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
 * Provides business logic for department configuration management.
 * Updates an existing department-scoped config option, or creates one
 * (OptionScope.DEPARTMENT) if it doesn't exist yet — matching the lazy-create
 * behavior of ConfigOptionApplicationController.getBooleanValueByKeyForDepartment
 * and the app-scoped setX endpoints in ConfigResource.
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
     * Update department configuration option value. Creates the option
     * (OptionScope.DEPARTMENT) if it doesn't exist yet for this department —
     * many department-scoped keys (e.g. "Pharmacy - Allow Issue to Same
     * Department") are only ever lazily created by the JSF page that reads
     * them, so a department nobody has opened that page for has no row at
     * all; this endpoint previously required that row to pre-exist, making
     * it impossible to set such a key purely via API (issue #23945 follow-up).
     * The value type for a newly-created key comes from
     * {@code configValueType} if given, otherwise is inferred (BOOLEAN for
     * "true"/"false", SHORT_TEXT otherwise). Ignored when updating an
     * existing option — its stored type is left as-is.
     */
    public DepartmentConfigDTO updateDepartmentConfig(DepartmentConfigUpdateDTO request, WebUser user) throws Exception {
        validateUpdateRequest(request);

        if (user == null) {
            throw new Exception("User is required for updating department config");
        }

        // Validate department exists
        Department department = loadAndValidateDepartment(request.getDepartmentId());

        // Find existing config option for this department and key, creating it if absent
        ConfigOption configOption = findConfigOption(request.getDepartmentId(), request.getConfigKey());
        boolean created = configOption == null;
        if (created) {
            OptionValueType valueType = resolveValueType(request);
            String typeError = validateValueForType(valueType, request.getConfigValue());
            if (typeError != null) {
                throw new Exception(typeError);
            }
            configOption = configOptionFacade.createOptionIfNotExists(request.getConfigKey(), OptionScope.DEPARTMENT,
                    null, department, null, valueType, request.getConfigValue());
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
        response.setConfigDescription(created
                ? "Created with value '" + request.getConfigValue() + "'"
                : "Updated from '" + oldValue + "' to '" + request.getConfigValue() + "'");

        return response;
    }

    /**
     * Determine the OptionValueType for a newly-created department config
     * option: explicit {@code configValueType} if given, else inferred from
     * the value (BOOLEAN for "true"/"false", SHORT_TEXT otherwise).
     */
    private OptionValueType resolveValueType(DepartmentConfigUpdateDTO request) throws Exception {
        String explicit = request.getConfigValueType();
        if (explicit != null && !explicit.trim().isEmpty()) {
            try {
                return OptionValueType.valueOf(explicit.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new Exception("Invalid configValueType '" + explicit
                        + "'. Valid values: " + java.util.Arrays.toString(OptionValueType.values()));
            }
        }
        String value = request.getConfigValue();
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return OptionValueType.BOOLEAN;
        }
        return OptionValueType.SHORT_TEXT;
    }

    /**
     * Validate that {@code value} actually parses as {@code valueType} before
     * it is persisted on create. Without this, a caller-supplied
     * configValueType (e.g. INTEGER) with an unparseable configValue would
     * still get stored, and the typed department readers
     * (e.g. getLongValueByKeyForDepartment) silently fall back to their
     * default on every subsequent read instead of surfacing the bad data.
     * Mirrors AnthropicApiService.validateTypedValue's parsing rules and
     * scope (BOOLEAN/INTEGER/LONG/DOUBLE only), but returns an "Invalid "
     * prefixed message so DepartmentApi maps it to HTTP 400.
     */
    private String validateValueForType(OptionValueType valueType, String value) {
        if (valueType == null) {
            return null;
        }
        switch (valueType) {
            case BOOLEAN:
                if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
                    return "Invalid configValue '" + value + "' for configValueType BOOLEAN (must be true or false)";
                }
                break;
            case INTEGER:
                try {
                    Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    return "Invalid configValue '" + value + "' for configValueType INTEGER";
                }
                break;
            case LONG:
                try {
                    Long.parseLong(value.trim());
                } catch (NumberFormatException e) {
                    return "Invalid configValue '" + value + "' for configValueType LONG";
                }
                break;
            case DOUBLE:
                try {
                    Double.parseDouble(value.trim());
                } catch (NumberFormatException e) {
                    return "Invalid configValue '" + value + "' for configValueType DOUBLE";
                }
                break;
            default:
                break;
        }
        return null;
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
