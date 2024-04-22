/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import com.divudi.data.MeasurementType;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 *
 * @author buddhika
 */
@Entity
public class Vmp extends PharmaceuticalItem implements Serializable {

    @ManyToOne
    private Category dosageForm;
    private Item vtm;
    @ManyToOne
    private MeasurementUnit strengthUnit;
    @ManyToOne
    private MeasurementUnit issueUnit;
    @ManyToOne
    private MeasurementUnit packUnit;
    @ManyToOne
    private MeasurementUnit issueMultipliesUnit;
    @ManyToOne
    private MeasurementUnit minimumIssueQuantityUnit;
    private Double strengthOfAnIssueUnit;
    private Double issueUnitsPerPackUnit;
    private Double issueMultipliesQuantity;
    private Double minimumIssueQuantity;
    

    public Vmp() {
    }

    private MeasurementType issueType;

    public MeasurementUnit getStrengthUnit() {
        return strengthUnit;
    }

    public void setStrengthUnit(MeasurementUnit strengthUnit) {
        this.strengthUnit = strengthUnit;
    }

    public MeasurementUnit getIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(MeasurementUnit issueUnit) {
        this.issueUnit = issueUnit;
    }

    public MeasurementUnit getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(MeasurementUnit packUnit) {
        this.packUnit = packUnit;
    }

    public MeasurementUnit getIssueMultipliesUnit() {
        return issueMultipliesUnit;
    }

    public void setIssueMultipliesUnit(MeasurementUnit issueMultipliesUnit) {
        this.issueMultipliesUnit = issueMultipliesUnit;
    }

    public MeasurementUnit getMinimumIssueQuantityUnit() {
        return minimumIssueQuantityUnit;
    }

    public void setMinimumIssueQuantityUnit(MeasurementUnit minimumIssueQuantityUnit) {
        this.minimumIssueQuantityUnit = minimumIssueQuantityUnit;
    }

    public Double getStrengthOfAnIssueUnit() {
        return strengthOfAnIssueUnit;
    }

    public void setStrengthOfAnIssueUnit(Double strengthOfAnIssueUnit) {
        this.strengthOfAnIssueUnit = strengthOfAnIssueUnit;
    }

    public Double getIssueUnitsPerPackUnit() {
        return issueUnitsPerPackUnit;
    }

    public void setIssueUnitsPerPackUnit(Double issueUnitsPerPackUnit) {
        this.issueUnitsPerPackUnit = issueUnitsPerPackUnit;
    }

    public Double getIssueMultipliesQuantity() {
        return issueMultipliesQuantity;
    }

    public void setIssueMultipliesQuantity(Double issueMultipliesQuantity) {
        this.issueMultipliesQuantity = issueMultipliesQuantity;
    }

    public Double getMinimumIssueQuantity() {
        return minimumIssueQuantity;
    }

    public void setMinimumIssueQuantity(Double minimumIssueQuantity) {
        this.minimumIssueQuantity = minimumIssueQuantity;
    }

    public MeasurementType getIssueType() {
        return issueType;
    }

    public void setIssueType(MeasurementType issueType) {
        this.issueType = issueType;
    }

    public Category getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(Category dosageForm) {
        this.dosageForm = dosageForm;
    }

    public Item getVtm() {
        return vtm;
    }
    
    public void setVtm(Item vtm) {
        this.vtm = vtm;
    }
    
    
}
