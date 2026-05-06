package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for AMP (Actual Medicinal Product) create/update operations.
 * Extends base with vmpId, atmId, categoryId, and business rule fields.
 *
 * @author Buddhika
 */
public class AmpRequestDTO extends PharmaceuticalItemBaseRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long vmpId;
    private Long atmId;
    private Long categoryId;
    private Long dosageFormId;
    private String barcode;
    private Boolean discountAllowed;
    private Boolean allowFractions;
    private Boolean consumptionAllowed;
    private Boolean refundsAllowed;

    public AmpRequestDTO() {
    }

    public Long getVmpId() {
        return vmpId;
    }

    public void setVmpId(Long vmpId) {
        this.vmpId = vmpId;
    }

    public Long getAtmId() {
        return atmId;
    }

    public void setAtmId(Long atmId) {
        this.atmId = atmId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getDosageFormId() {
        return dosageFormId;
    }

    public void setDosageFormId(Long dosageFormId) {
        this.dosageFormId = dosageFormId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Boolean getDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(Boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    public Boolean getAllowFractions() {
        return allowFractions;
    }

    public void setAllowFractions(Boolean allowFractions) {
        this.allowFractions = allowFractions;
    }

    public Boolean getConsumptionAllowed() {
        return consumptionAllowed;
    }

    public void setConsumptionAllowed(Boolean consumptionAllowed) {
        this.consumptionAllowed = consumptionAllowed;
    }

    public Boolean getRefundsAllowed() {
        return refundsAllowed;
    }

    public void setRefundsAllowed(Boolean refundsAllowed) {
        this.refundsAllowed = refundsAllowed;
    }
}
