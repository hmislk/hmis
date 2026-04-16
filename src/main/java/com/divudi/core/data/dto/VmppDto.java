package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for VMPP (Virtual Medicinal Product Pack) management operations. Provides
 * lightweight representation for display and management operations.
 *
 * Note: Uses 'descreption' field name for backward compatibility with existing
 * database schema.
 *
 * @author Claude Code
 */
public class VmppDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core display fields
    private Long id;
    private String name;
    private String code;
    private Boolean retired;
    private Boolean inactive;

    // VMP relationship fields
    private Long vmpId;
    private String vmpName;

    /**
     * Default constructor
     */
    public VmppDto() {
    }

    /**
     * Constructor for JPQL query - core VMPP management use case
     *
     * @param id VMPP ID
     * @param name VMPP name
     * @param code VMPP code (can be null)
     * @param retired Whether VMPP is retired/deleted
     * @param inactive Whether VMPP is inactive
     */
    public VmppDto(Long id, String name, String code,
            Boolean retired, Boolean inactive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
        this.vmpId = null;
        this.vmpName = null;
    }

    /**
     * Constructor for JPQL query - extended VMPP management with VMP
     * relationship
     *
     * @param id VMPP ID
     * @param name VMPP name
     * @param code VMPP code (can be null)
     * @param retired Whether VMPP is retired/deleted
     * @param inactive Whether VMPP is inactive
     * @param vmpId VMP ID (can be null)
     * @param vmpName VMP name for display (can be null)
     */
    public VmppDto(Long id, String name, String code,
            Boolean retired, Boolean inactive, Long vmpId, String vmpName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.retired = retired != null ? retired : false;
        this.inactive = inactive != null ? inactive : false;
        this.vmpId = vmpId;
        this.vmpName = vmpName;
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

    public Boolean getRetired() {
        return retired;
    }

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
     * Returns VMP display string for UI
     */
    public String getVmpDisplay() {
        if (vmpName != null && !vmpName.trim().isEmpty()) {
            return vmpName;
        }
        return "No VMP";
    }

    @Override
    public String toString() {
        return "VmppDto{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", code='" + code + '\''
                + ", retired=" + retired
                + ", inactive=" + inactive
                + ", vmpId=" + vmpId
                + ", vmpName='" + vmpName + '\''
                + '}';
    }
}
