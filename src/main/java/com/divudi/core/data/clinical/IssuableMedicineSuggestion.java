package com.divudi.core.data.clinical;

import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.MeasurementUnit;

/**
 * Transient view model for displaying issuable medicine suggestions during
 * prescription. Not a JPA entity.
 *
 * @author Dr M H B Ariyaratne
 */
public class IssuableMedicineSuggestion {

    private Amp amp;
    private double calculatedIssueQty;
    private MeasurementUnit issueUnit;
    private double availableStock;
    private boolean selected;
    private boolean inStock;
    private boolean sameDosageForm;
    private String displayText;
    private ClinicalFindingValue sourceCfv;

    public IssuableMedicineSuggestion() {
    }

    public Amp getAmp() {
        return amp;
    }

    public void setAmp(Amp amp) {
        this.amp = amp;
    }

    public double getCalculatedIssueQty() {
        return calculatedIssueQty;
    }

    public void setCalculatedIssueQty(double calculatedIssueQty) {
        this.calculatedIssueQty = calculatedIssueQty;
    }

    public MeasurementUnit getIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(MeasurementUnit issueUnit) {
        this.issueUnit = issueUnit;
    }

    public double getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(double availableStock) {
        this.availableStock = availableStock;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public boolean isSameDosageForm() {
        return sameDosageForm;
    }

    public void setSameDosageForm(boolean sameDosageForm) {
        this.sameDosageForm = sameDosageForm;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public ClinicalFindingValue getSourceCfv() {
        return sourceCfv;
    }

    public void setSourceCfv(ClinicalFindingValue sourceCfv) {
        this.sourceCfv = sourceCfv;
    }
}
