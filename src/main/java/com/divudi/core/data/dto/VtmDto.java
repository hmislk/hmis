package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for VTM (Virtual Therapeutic Moiety) management operations. Provides
 * lightweight representation for display and management operations.
 *
 * Note: Uses 'descreption' field name for backward compatibility with existing
 * database schema.
 *
 * @author Claude Code
 */
public class VtmDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core display fields
    private Long id;
    private String name;
    private String code;
    private String descreption; // Note: intentional spelling for backward compatibility
    private String instructions;
    private Boolean retired;

    // Navigation/Department fields
    private String departmentTypeName;

    /**
     * Default constructor
     */
    public VtmDto() {
    }

    /**
     * Constructor for JPQL query - basic VTM management use case Includes core
     * fields needed for display and management operations
     *
     * @param id VTM ID
     * @param name VTM name
     * @param code VTM code (can be null)
     * @param descreption VTM description (note: spelling preserved for backward
     * compatibility)
     * @param instructions Usage instructions (can be null)
     * @param retired Whether VTM is retired/inactive
     * @param departmentTypeName Department type name (for display)
     */
    public VtmDto(Long id, String name, String code, String descreption,
            String instructions, Boolean retired, String departmentTypeName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.instructions = instructions;
        this.retired = retired != null ? retired : false;
        this.departmentTypeName = departmentTypeName;
    }

    /**
     * Constructor for JPQL query - basic VTM management use case Includes core
     * fields needed for display and management operations
     *
     * @param id VTM ID
     * @param name VTM name
     * @param code VTM code (can be null)
     * @param descreption VTM description (note: spelling preserved for backward
     * compatibility)
     * @param instructions Usage instructions (can be null)
     * @param retired Whether VTM is retired/inactive
     */
    public VtmDto(Long id, String name, String code, String descreption,
            String instructions, Boolean retired) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.instructions = instructions;
        this.retired = retired != null ? retired : false;
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

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
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

    public String getDepartmentTypeName() {
        return departmentTypeName;
    }

    public void setDepartmentTypeName(String departmentTypeName) {
        this.departmentTypeName = departmentTypeName;
    }

    // Utility methods for display
    /**
     * Returns display status for UI - Active/Inactive
     */
    public String getStatusDisplay() {
        return isRetired() ? "Inactive" : "Active";
    }

    /**
     * Returns CSS class for status display - for styling
     */
    public String getStatusCssClass() {
        return isRetired() ? "badge-danger" : "badge-success";
    }

    @Override
    public String toString() {
        return "VtmDto{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", code='" + code + '\''
                + ", retired=" + retired
                + ", departmentTypeName='" + departmentTypeName + '\''
                + '}';
    }
}
