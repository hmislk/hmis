package com.divudi.core.data.dto;

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
    private Boolean retired;

    // VMP relationship fields
    private Long vmpId;
    private String vmpName;

    // Category relationship fields
    private Long categoryId;
    private String categoryName;

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
     * @param retired Whether AMP is retired/inactive
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean retired) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.retired = retired;
    }

    /**
     * Extended constructor - with VMP relationship for display
     * Used when VMP information is needed for display purposes
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param retired Whether AMP is retired/inactive
     * @param vmpId VMP ID
     * @param vmpName VMP name
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean retired,
                  Long vmpId, String vmpName) {
        this(id, name, code, barcode, retired);
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
     * @param retired Whether AMP is retired/inactive
     * @param vmpId VMP ID
     * @param vmpName VMP name
     * @param categoryId Category ID
     * @param categoryName Category name
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean retired,
                  Long vmpId, String vmpName, Long categoryId, String categoryName) {
        this(id, name, code, barcode, retired, vmpId, vmpName);
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    /**
     * Business rules constructor - includes business rule flags
     * Used for bulk editing and business rule management operations
     *
     * @param id AMP ID
     * @param name AMP name
     * @param code AMP code
     * @param barcode AMP barcode
     * @param retired Whether AMP is retired/inactive
     * @param discountAllowed Whether discount is allowed
     * @param allowFractions Whether fractions are allowed
     * @param consumptionAllowed Whether consumption is allowed
     * @param refundsAllowed Whether refunds are allowed
     */
    public AmpDto(Long id, String name, String code, String barcode, Boolean retired,
                  Boolean discountAllowed, Boolean allowFractions,
                  Boolean consumptionAllowed, Boolean refundsAllowed) {
        this(id, name, code, barcode, retired);
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
        return (retired != null && retired) ? "Inactive" : "Active";
    }

    /**
     * Get CSS class for status badge
     * @return CSS class for status styling
     */
    public String getStatusCssClass() {
        return (retired != null && retired) ? "badge-danger" : "badge-success";
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
        return retired == null || !retired;
    }

    /**
     * Check if AMP is retired/inactive
     * @return true if retired, false if active
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
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
                ", retired=" + retired +
                ", vmpName='" + vmpName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}