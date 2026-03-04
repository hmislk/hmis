package com.divudi.core.data.dto;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;

/**
 * Comprehensive DTO for AMP (Actual Medicinal Product) management operations.
 * Provides lightweight representation for autocomplete, display, and management operations.
 * Supports multiple constructor patterns for different query scenarios.
 *
 * @author Claude Code
 */
public class AmpDto implements Serializable {
    private static final long serialVersionUID = 1L;

    // Core AMP fields
    private Long id;
    private String name;
    private String code;
    private String barcode;
    private Boolean inactive;

    // VMP relationship fields
    private Long vmpId;
    private String vmpName;

    // Category relationship fields
    private Long categoryId;
    private String categoryName;

    // Dosage form relationship fields
    private Long dosageFormId;
    private String dosageFormName;

    // Department type
    private DepartmentType departmentType;

    // Business rule fields
    private Boolean discountAllowed;
    private Boolean allowFractions;
    private Boolean consumptionAllowed;
    private Boolean refundsAllowed;

    /**
     * Default constructor
     */
    public AmpDto() {
    }

    /**
     * Basic constructor - core AMP management (primary for JPQL)
     * Used for essential AMP operations, autocomplete, and status management
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param inactive Whether AMP is inactive
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean inactive) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.inactive = inactive;
    }

    /**
     * Constructor with department type - for autocomplete with department type display
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param inactive Whether AMP is inactive
     * @param departmentType Department type of the AMP
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean inactive, DepartmentType departmentType) {
        this(id, name, code, barcode, inactive);
        this.departmentType = departmentType;
    }

    /**
     * Extended constructor - with VMP relationship for display
     * Used when VMP information is needed for display purposes
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param inactive Whether AMP is inactive
     * @param vmpId VMP ID
     * @param vmpName VMP name
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean inactive,
                  Long vmpId, String vmpName) {
        this(id, name, code, barcode, inactive);
        this.vmpId = vmpId;
        this.vmpName = vmpName;
    }

    /**
     * Comprehensive constructor - with VMP and category information
     * Used for complete AMP information display and management
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param inactive Whether AMP is inactive
     * @param vmpId VMP ID
     * @param vmpName VMP name
     * @param categoryId Category ID
     * @param categoryName Category name
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean inactive,
                  Long vmpId, String vmpName, Long categoryId, String categoryName) {
        this(id, name, code, barcode, inactive, vmpId, vmpName);
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    /**
     * Comprehensive constructor with dosage form - includes VMP, category, and dosage form
     * Used for complete AMP information display including dosage form data
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param inactive Whether AMP is inactive
     * @param vmpId VMP ID
     * @param vmpName VMP name
     * @param categoryId Category ID
     * @param categoryName Category name
     * @param dosageFormId Dosage form ID
     * @param dosageFormName Dosage form name
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean inactive,
                  Long vmpId, String vmpName, Long categoryId, String categoryName,
                  Long dosageFormId, String dosageFormName) {
        this(id, name, code, barcode, inactive, vmpId, vmpName, categoryId, categoryName);
        this.dosageFormId = dosageFormId;
        this.dosageFormName = dosageFormName;
    }

    /**
     * Business rules constructor - includes business rule flags
     * Used for bulk editing and business rule management operations
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param inactive Whether AMP is inactive
     * @param discountAllowed Whether discount is allowed
     * @param allowFractions Whether fractions are allowed
     * @param consumptionAllowed Whether consumption is allowed
     * @param refundsAllowed Whether refunds are allowed
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean inactive,
                  Boolean discountAllowed, Boolean allowFractions,
                  Boolean consumptionAllowed, Boolean refundsAllowed) {
        this(id, name, code, barcode, inactive);
        this.discountAllowed = discountAllowed;
        this.allowFractions = allowFractions;
        this.consumptionAllowed = consumptionAllowed;
        this.refundsAllowed = refundsAllowed;
    }

    // Display utility methods

    /**
     * Get display text for status
     * @return "Active" or "Inactive"
     */
    public String getStatusDisplay() {
        return (inactive != null && inactive) ? "Inactive" : "Active";
    }

    /**
     * Get CSS class for status badge
     * @return CSS class for status styling
     */
    public String getStatusCssClass() {
        return (inactive != null && inactive) ? "badge-danger" : "badge-success";
    }

    /**
     * Get display text for VMP relationship
     * @return VMP name or "Not linked" if no VMP
     */
    public String getVmpDisplay() {
        return (vmpName != null && !vmpName.trim().isEmpty()) ? vmpName : "Not linked";
    }

    /**
     * Get display text for code, handling null values
     * @return Code or "Not assigned" if null
     */
    public String getCodeDisplay() {
        return (code != null && !code.trim().isEmpty()) ? code : "Not assigned";
    }

    /**
     * Get display text for barcode, handling null values
     * @return Barcode or "Not assigned" if null
     */
    public String getBarcodeDisplay() {
        return (barcode != null && !barcode.trim().isEmpty()) ? barcode : "Not assigned";
    }

    /**
     * Get display text for category
     * @return Category name or "Uncategorized" if no category
     */
    public String getCategoryDisplay() {
        return (categoryName != null && !categoryName.trim().isEmpty()) ? categoryName : "Uncategorized";
    }

    /**
     * Check if AMP is active (not retired)
     * @return true if active, false if inactive/retired
     */
    public boolean isActive() {
        return inactive == null || !inactive;
    }

    /**
     * Check if AMP is inactive
     * @return true if inactive, false if active
     */
    public boolean isInactive() {
        return inactive != null && inactive;
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
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

    // Category relationship getters and setters

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    // Dosage form relationship getters and setters

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

    // Business rule getters and setters

    public Boolean getDiscountAllowed() {
        return discountAllowed;
    }

    // EL-friendly boolean accessor
    public boolean isDiscountAllowed() {
        return discountAllowed != null && discountAllowed;
    }

    public void setDiscountAllowed(Boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    public Boolean getAllowFractions() {
        return allowFractions;
    }

    // EL-friendly boolean accessor
    public boolean isAllowFractions() {
        return allowFractions != null && allowFractions;
    }

    public void setAllowFractions(Boolean allowFractions) {
        this.allowFractions = allowFractions;
    }

    public Boolean getConsumptionAllowed() {
        return consumptionAllowed;
    }

    // EL-friendly boolean accessor
    public boolean isConsumptionAllowed() {
        return consumptionAllowed != null && consumptionAllowed;
    }

    public void setConsumptionAllowed(Boolean consumptionAllowed) {
        this.consumptionAllowed = consumptionAllowed;
    }

    public Boolean getRefundsAllowed() {
        return refundsAllowed;
    }

    // EL-friendly boolean accessor
    public boolean isRefundsAllowed() {
        return refundsAllowed != null && refundsAllowed;
    }

    public void setRefundsAllowed(Boolean refundsAllowed) {
        this.refundsAllowed = refundsAllowed;
    }

    // Department type getter and setter

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public String getDepartmentTypeLabel() {
        return departmentType != null ? departmentType.getShortLabel() : "";
    }

    // Utility methods for consistency

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AmpDto ampDto = (AmpDto) obj;
        return id != null ? id.equals(ampDto.id) : ampDto.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AmpDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", barcode='" + barcode + '\'' +
                ", inactive=" + inactive +
                ", vmpName='" + vmpName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", dosageFormName='" + dosageFormName + '\'' +
                '}';
    }
}