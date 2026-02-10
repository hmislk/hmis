package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for AMPP (Actual Medicinal Product Package) management operations.
 * Provides lightweight representation for autocomplete, display, and management operations.
 *
 * @author Claude Code
 */
public class AmppDto implements Serializable {
    private static final long serialVersionUID = 1L;

    // Core AMPP fields
    private Long id;
    private String name;
    private String code;
    private Boolean retired;
    private Boolean inactive;

    // Pack information
    private Double dblValue; // Pack size
    private String packUnitName;

    // AMP relationship fields
    private Long ampId;
    private String ampName;

    // VMP relationship (through AMP)
    private Long vmpId;
    private String vmpName;

    /**
     * Default constructor
     */
    public AmppDto() {
    }

    /**
     * Basic constructor - core AMPP management (primary for JPQL)
     * Used for essential AMPP operations, autocomplete, and status management
     *
     * @param id AMPP ID
     * @param name AMPP name
     * @param code AMPP code
     * @param retired Whether AMPP is retired/deleted
     * @param inactive Whether AMPP is inactive
     */
    public AmppDto(Long id, String name, String code, Boolean retired, Boolean inactive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
    }

    /**
     * Extended constructor - with pack information
     * Used when pack size and unit information is needed
     *
     * @param id AMPP ID
     * @param name AMPP name
     * @param code AMPP code
     * @param retired Whether AMPP is retired/deleted
     * @param inactive Whether AMPP is inactive
     * @param dblValue Pack size value
     * @param packUnitName Pack unit name
     */
    public AmppDto(Long id, String name, String code, Boolean retired, Boolean inactive,
                   Double dblValue, String packUnitName) {
        this(id, name, code, retired, inactive);
        this.dblValue = dblValue;
        this.packUnitName = packUnitName;
    }

    /**
     * Extended constructor - with AMP relationship for display
     * Used when AMP information is needed for display purposes
     *
     * @param id AMPP ID
     * @param name AMPP name
     * @param code AMPP code
     * @param retired Whether AMPP is retired/deleted
     * @param inactive Whether AMPP is inactive
     * @param ampId AMP ID
     * @param ampName AMP name
     */
    public AmppDto(Long id, String name, String code, Boolean retired, Boolean inactive,
                   Long ampId, String ampName) {
        this(id, name, code, retired, inactive);
        this.ampId = ampId;
        this.ampName = ampName;
    }

    /**
     * Comprehensive constructor - with pack information and AMP relationship
     * Used for complete AMPP information display and management
     *
     * @param id AMPP ID
     * @param name AMPP name
     * @param code AMPP code
     * @param retired Whether AMPP is retired/deleted
     * @param inactive Whether AMPP is inactive
     * @param dblValue Pack size value
     * @param packUnitName Pack unit name
     * @param ampId AMP ID
     * @param ampName AMP name
     */
    public AmppDto(Long id, String name, String code, Boolean retired, Boolean inactive,
                   Double dblValue, String packUnitName, Long ampId, String ampName) {
        this(id, name, code, retired, inactive, dblValue, packUnitName);
        this.ampId = ampId;
        this.ampName = ampName;
    }

    /**
     * Full constructor - with pack information, AMP and VMP relationships
     * Used for complete AMPP hierarchy display
     *
     * @param id AMPP ID
     * @param name AMPP name
     * @param code AMPP code
     * @param retired Whether AMPP is retired/deleted
     * @param inactive Whether AMPP is inactive
     * @param dblValue Pack size value
     * @param packUnitName Pack unit name
     * @param ampId AMP ID
     * @param ampName AMP name
     * @param vmpId VMP ID
     * @param vmpName VMP name
     */
    public AmppDto(Long id, String name, String code, Boolean retired, Boolean inactive,
                   Double dblValue, String packUnitName, Long ampId, String ampName,
                   Long vmpId, String vmpName) {
        this(id, name, code, retired, inactive, dblValue, packUnitName, ampId, ampName);
        this.vmpId = vmpId;
        this.vmpName = vmpName;
    }

    // Display utility methods

    /**
     * Get display text for status
     * @return "Active" or "Inactive"
     */
    public String getStatusDisplay() {
        return isInactive() ? "Inactive" : "Active";
    }

    /**
     * Get CSS class for status badge
     * @return CSS class for status styling
     */
    public String getStatusCssClass() {
        return isInactive() ? "badge-danger" : "badge-success";
    }

    /**
     * Get display text for AMP relationship
     * @return AMP name or "Not linked" if no AMP
     */
    public String getAmpDisplay() {
        return (ampName != null && !ampName.trim().isEmpty()) ? ampName : "Not linked";
    }

    /**
     * Get display text for VMP relationship
     * @return VMP name or "Not linked" if no VMP
     */
    public String getVmpDisplay() {
        return (vmpName != null && !vmpName.trim().isEmpty()) ? vmpName : "Not linked";
    }

    /**
     * Get display text for pack size
     * @return Pack size with unit or "Not specified"
     */
    public String getPackSizeDisplay() {
        if (dblValue != null && packUnitName != null && !packUnitName.trim().isEmpty()) {
            return dblValue + " " + packUnitName;
        } else if (dblValue != null) {
            return dblValue.toString();
        }
        return "Not specified";
    }

    /**
     * Check if AMPP is active (not inactive)
     * @return true if active, false if inactive
     */
    public boolean isActive() {
        return !isInactive();
    }

    /**
     * Check if AMPP is inactive
     * @return true if inactive, false if active
     */
    public boolean isInactive() {
        return inactive != null && inactive;
    }

    /**
     * Check if AMPP is retired/deleted
     * @return true if retired, false if not
     */
    public boolean isRetired() {
        return retired != null && retired;
    }

    // Core getters and setters

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

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }

    // Pack information getters and setters

    public Double getDblValue() {
        return dblValue;
    }

    public void setDblValue(Double dblValue) {
        this.dblValue = dblValue;
    }

    public String getPackUnitName() {
        return packUnitName;
    }

    public void setPackUnitName(String packUnitName) {
        this.packUnitName = packUnitName;
    }

    // AMP relationship getters and setters

    public Long getAmpId() {
        return ampId;
    }

    public void setAmpId(Long ampId) {
        this.ampId = ampId;
    }

    public String getAmpName() {
        return ampName;
    }

    public void setAmpName(String ampName) {
        this.ampName = ampName;
    }

    // VMP relationship getters and setters

    public Long getVmpId() {
        return vmpId;
    }

    public void setVmpId(Long vmpId) {
        this.vmpId = vmpId;
    }

    public String getVmpName() {
        return vmpName;
    }

    public void setVmpName(String vmpName) {
        this.vmpName = vmpName;
    }

    // Utility methods for consistency

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AmppDto amppDto = (AmppDto) obj;
        return id != null ? id.equals(amppDto.id) : amppDto.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AmppDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", retired=" + retired +
                ", inactive=" + inactive +
                ", dblValue=" + dblValue +
                ", packUnitName='" + packUnitName + '\'' +
                ", ampName='" + ampName + '\'' +
                ", vmpName='" + vmpName + '\'' +
                '}';
    }
}
