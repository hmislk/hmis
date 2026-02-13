package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for AMP (Actual Medicinal Product) bulk editing operations.
 * Provides lightweight representation for display and bulk updates.
 *
 * @author Claude Code
 */
public class AmpDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Boolean discountAllowed;
    private Boolean allowFractions;
    private Boolean consumptionAllowed;
    private Boolean refundsAllowed;

    /**
     * Default constructor
     */
    public AmpDTO() {
    }

    /**
     * Constructor for JPQL query - bulk editing use case
     * Includes all fields needed for display and filtering
     *
     * @param id AMP ID
     * @param name AMP name
     * @param categoryId Category ID (for navigation)
     * @param categoryName Category name (for display)
     * @param discountAllowed Whether discount is allowed
     * @param allowFractions Whether fractions are allowed
     * @param consumptionAllowed Whether consumption is allowed
     * @param refundsAllowed Whether refunds are allowed
     */
    public AmpDTO(Long id, String name, Long categoryId, String categoryName,
                  Boolean discountAllowed, Boolean allowFractions,
                  Boolean consumptionAllowed, Boolean refundsAllowed) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.discountAllowed = discountAllowed;
        this.allowFractions = allowFractions;
        this.consumptionAllowed = consumptionAllowed;
        this.refundsAllowed = refundsAllowed;
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

    public Boolean getDiscountAllowed() {
        return discountAllowed;
    }

    // Prefer boolean accessor for EL friendliness
    public boolean isDiscountAllowed() {
        return discountAllowed != null && discountAllowed;
    }

    public void setDiscountAllowed(Boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    public Boolean getAllowFractions() {
        return allowFractions;
    }

    // Prefer boolean accessor for EL friendliness
    public boolean isAllowFractions() {
        return allowFractions != null && allowFractions;
    }

    public void setAllowFractions(Boolean allowFractions) {
        this.allowFractions = allowFractions;
    }

    public Boolean getConsumptionAllowed() {
        return consumptionAllowed;
    }

    // Prefer boolean accessor for EL friendliness
    public boolean isConsumptionAllowed() {
        return consumptionAllowed != null && consumptionAllowed;
    }

    public void setConsumptionAllowed(Boolean consumptionAllowed) {
        this.consumptionAllowed = consumptionAllowed;
    }

    public Boolean getRefundsAllowed() {
        return refundsAllowed;
    }

    // Prefer boolean accessor for EL friendliness
    public boolean isRefundsAllowed() {
        return refundsAllowed != null && refundsAllowed;
    }

    public void setRefundsAllowed(Boolean refundsAllowed) {
        this.refundsAllowed = refundsAllowed;
    }
}
