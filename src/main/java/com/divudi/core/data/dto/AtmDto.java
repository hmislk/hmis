package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for ATM (Anatomical Therapeutic Material) management operations. Provides
 * lightweight representation for display and management operations.
 *
 * Note: Uses 'descreption' field name for backward compatibility with existing
 * database schema.
 *
 * @author Claude Code
 */
public class AtmDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core display fields
    private Long id;
    private String name;
    private String code;
    private String descreption; // Note: intentional spelling for backward compatibility
    private Boolean retired;
    private Boolean inactive;

    // VTM relationship fields
    private Long vtmId;
    private String vtmName;

    // Navigation/Department fields
    private String departmentTypeName;

    /**
     * Default constructor
     */
    public AtmDto() {
    }

    /**
     * Constructor for JPQL query - comprehensive ATM management use case
     * Includes core fields needed for display and management operations
     * with VTM relationship data
     *
     * @param id ATM ID
     * @param name ATM name
     * @param code ATM code (can be null)
     * @param descreption ATM description (note: spelling preserved for backward
     * compatibility)
     * @param retired Whether ATM is retired/inactive
     * @param vtmId VTM ID (can be null)
     * @param vtmName VTM name for display (can be null)
     * @param departmentTypeName Department type name (for display)
     *
     * @deprecated Use the 7-argument constructor instead. Department type should be
     * filtered in WHERE clause, not included in DTO for consistency across all
     * DTO creation methods (getAtmDtos, completeAtmDto, createAtmDto).
     */
    @Deprecated
    public AtmDto(Long id, String name, String code, String descreption,
            Boolean retired, Long vtmId, String vtmName, String departmentTypeName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = retired != null ? retired : false;
        this.vtmId = vtmId;
        this.vtmName = vtmName;
        this.departmentTypeName = departmentTypeName;
    }

    /**
     * Constructor for JPQL query - core ATM management use case without VTM
     * Includes essential fields needed for basic ATM operations
     *
     * @param id ATM ID
     * @param name ATM name
     * @param code ATM code (can be null)
     * @param descreption ATM description (note: spelling preserved for backward
     * compatibility)
     * @param retired Whether ATM is retired/inactive
     */
    public AtmDto(Long id, String name, String code, String descreption,
            Boolean retired) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = retired != null ? retired : false;
        this.vtmId = null;
        this.vtmName = null;
    }

    /**
     * Constructor for JPQL query - ATM management with inactive status
     *
     * @param id ATM ID
     * @param name ATM name
     * @param code ATM code (can be null)
     * @param descreption ATM description
     * @param retired Whether ATM is retired (soft deleted)
     * @param inactive Whether ATM is inactive (temporarily disabled)
     */
    public AtmDto(Long id, String name, String code, String descreption,
            Boolean retired, Boolean inactive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
        this.vtmId = null;
        this.vtmName = null;
    }

    /**
     * Constructor for JPQL query - basic ATM management use case
     * Includes core fields needed for display and management operations
     * with VTM relationship data
     *
     * @param id ATM ID
     * @param name ATM name
     * @param code ATM code (can be null)
     * @param descreption ATM description (note: spelling preserved for backward
     * compatibility)
     * @param retired Whether ATM is retired/inactive
     * @param vtmId VTM ID (can be null)
     * @param vtmName VTM name for display (can be null)
     */
    public AtmDto(Long id, String name, String code, String descreption,
            Boolean retired, Long vtmId, String vtmName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = retired != null ? retired : false;
        this.vtmId = vtmId;
        this.vtmName = vtmName;
    }

    // Getters and Setters
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

    public String getDescreption() {
        return descreption;
    }

    public void setDescreption(String descreption) {
        this.descreption = descreption;
    }

    public Boolean getRetired() {
        return retired;
    }

    // Prefer boolean accessor for EL friendliness
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

    public Long getVtmId() {
        return vtmId;
    }

    public void setVtmId(Long vtmId) {
        this.vtmId = vtmId;
    }

    public String getVtmName() {
        return vtmName;
    }

    public void setVtmName(String vtmName) {
        this.vtmName = vtmName;
    }

    public String getDepartmentTypeName() {
        return departmentTypeName;
    }

    public void setDepartmentTypeName(String departmentTypeName) {
        this.departmentTypeName = departmentTypeName;
    }

    // Utility methods for display
    /**
     * Returns display status for UI - Active/Inactive based on inactive attribute
     */
    public String getStatusDisplay() {
        return isInactive() ? "Inactive" : "Active";
    }

    /**
     * Returns CSS class for status display - for styling based on inactive attribute
     */
    public String getStatusCssClass() {
        return isInactive() ? "badge-danger" : "badge-success";
    }

    /**
     * Returns VTM display string for UI
     */
    public String getVtmDisplay() {
        if (vtmName != null && !vtmName.trim().isEmpty()) {
            return vtmName;
        }
        return "No VTM";
    }

    @Override
    public String toString() {
        return "AtmDto{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", code='" + code + '\''
                + ", retired=" + retired
                + ", vtmId=" + vtmId
                + ", vtmName='" + vtmName + '\''
                + ", departmentTypeName='" + departmentTypeName + '\''
                + '}';
    }
}