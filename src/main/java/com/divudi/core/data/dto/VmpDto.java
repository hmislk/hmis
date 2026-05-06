package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for VMP (Virtual Medicinal Product) management operations. Provides
 * lightweight representation for display and management operations.
 *
 * Note: Uses 'descreption' field name for backward compatibility with existing
 * database schema.
 *
 * @author Claude Code
 */
public class VmpDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core display fields
    private Long id;
    private String name;
    private String code;
    private String descreption; // Note: intentional spelling for backward compatibility
    private Boolean retired; // For delete functionality (permanent removal from lists)
    private Boolean inactive; // For active/inactive status (visible but marked inactive)

    // VTM relationship fields
    private Long vtmId;
    private String vtmName;

    // Dosage form relationship fields
    private Long dosageFormId;
    private String dosageFormName;

    /**
     * Default constructor
     */
    public VmpDto() {
    }

    /**
     * Backwards-compatible constructor for JPQL query - assumes not retired
     * @param id VMP ID
     * @param name VMP name
     * @param code VMP code (can be null)
     * @param descreption VMP description
     * @param inactive Whether VMP is inactive
     */
    public VmpDto(Long id, String name, String code, String descreption, Boolean inactive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = false; // Assume not retired for backwards compatibility
        this.inactive = inactive != null ? inactive : false;
    }

    /**
     * Constructor for JPQL query - core VMP management use case
     * Includes essential fields needed for basic VMP operations
     *
     * @param id VMP ID
     * @param name VMP name
     * @param code VMP code (can be null)
     * @param descreption VMP description (note: spelling preserved for backward
     * compatibility)
     * @param retired Whether VMP is retired/deleted
     * @param inactive Whether VMP is inactive
     */
    public VmpDto(Long id, String name, String code, String descreption,
            Boolean retired, Boolean inactive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
        // Relationship fields remain null
        this.vtmId = null;
        this.vtmName = null;
        this.dosageFormId = null;
        this.dosageFormName = null;
    }

    /**
     * Constructor for JPQL query - extended VMP management use case
     * Includes core fields needed for display and management operations
     * with VTM relationship data
     *
     * @param id VMP ID
     * @param name VMP name
     * @param code VMP code (can be null)
     * @param descreption VMP description (note: spelling preserved for backward
     * compatibility)
     * @param inactive Whether VMP is inactive
     * @param vtmId VTM ID (can be null)
     * @param vtmName VTM name for display (can be null)
     */
    public VmpDto(Long id, String name, String code, String descreption,
            Boolean retired, Boolean inactive, Long vtmId, String vtmName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
        this.vtmId = vtmId;
        this.vtmName = vtmName;
        this.dosageFormId = null;
        this.dosageFormName = null;
    }

    /**
     * Constructor for JPQL query - comprehensive VMP management use case
     * Includes core fields with both VTM and dosage form relationship data
     *
     * @param id VMP ID
     * @param name VMP name
     * @param code VMP code (can be null)
     * @param descreption VMP description (note: spelling preserved for backward
     * compatibility)
     * @param inactive Whether VMP is inactive
     * @param vtmId VTM ID (can be null)
     * @param vtmName VTM name for display (can be null)
     * @param dosageFormId Dosage form ID (can be null)
     * @param dosageFormName Dosage form name for display (can be null)
     */
    public VmpDto(Long id, String name, String code, String descreption,
            Boolean retired, Boolean inactive, Long vtmId, String vtmName, Long dosageFormId, String dosageFormName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.descreption = descreption;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
        this.vtmId = vtmId;
        this.vtmName = vtmName;
        this.dosageFormId = dosageFormId;
        this.dosageFormName = dosageFormName;
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

    public Boolean getInactive() {
        return inactive;
    }

    // Prefer boolean accessor for EL friendliness
    public boolean isInactive() {
        return inactive != null && inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
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

    public Long getDosageFormId() {
        return dosageFormId;
    }

    public void setDosageFormId(Long dosageFormId) {
        this.dosageFormId = dosageFormId;
    }

    public String getDosageFormName() {
        return dosageFormName;
    }

    public void setDosageFormName(String dosageFormName) {
        this.dosageFormName = dosageFormName;
    }

    // Utility methods for display
    /**
     * Returns display status for UI - Active/Inactive
     */
    public String getStatusDisplay() {
        return isInactive() ? "Inactive" : "Active";
    }

    /**
     * Returns CSS class for status display - for styling
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

    /**
     * Returns dosage form display string for UI
     */
    public String getDosageFormDisplay() {
        if (dosageFormName != null && !dosageFormName.trim().isEmpty()) {
            return dosageFormName;
        }
        return "No Dosage Form";
    }

    @Override
    public String toString() {
        return "VmpDto{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", code='" + code + '\''
                + ", retired=" + retired
                + ", vtmId=" + vtmId
                + ", vtmName='" + vtmName + '\''
                + ", dosageFormId=" + dosageFormId
                + ", dosageFormName='" + dosageFormName + '\''
                + '}';
    }
}