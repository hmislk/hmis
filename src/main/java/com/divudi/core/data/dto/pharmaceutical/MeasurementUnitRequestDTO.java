package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for MeasurementUnit create/update operations.
 * Extends category fields with unit-specific boolean flags and duration/frequency.
 *
 * @author Buddhika
 */
public class MeasurementUnitRequestDTO extends CategoryConfigRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean strengthUnit;
    private Boolean packUnit;
    private Boolean issueUnit;
    private Boolean durationUnit;
    private Boolean frequencyUnit;
    private Double durationInHours;
    private Double frequencyInHours;

    public MeasurementUnitRequestDTO() {
    }

    public Boolean getStrengthUnit() {
        return strengthUnit;
    }

    public void setStrengthUnit(Boolean strengthUnit) {
        this.strengthUnit = strengthUnit;
    }

    public Boolean getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(Boolean packUnit) {
        this.packUnit = packUnit;
    }

    public Boolean getIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(Boolean issueUnit) {
        this.issueUnit = issueUnit;
    }

    public Boolean getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(Boolean durationUnit) {
        this.durationUnit = durationUnit;
    }

    public Boolean getFrequencyUnit() {
        return frequencyUnit;
    }

    public void setFrequencyUnit(Boolean frequencyUnit) {
        this.frequencyUnit = frequencyUnit;
    }

    public Double getDurationInHours() {
        return durationInHours;
    }

    public void setDurationInHours(Double durationInHours) {
        this.durationInHours = durationInHours;
    }

    public Double getFrequencyInHours() {
        return frequencyInHours;
    }

    public void setFrequencyInHours(Double frequencyInHours) {
        this.frequencyInHours = frequencyInHours;
    }
}
