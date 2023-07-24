/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.Category;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 *
 * @author buddhika
 */
@Entity
public class MeasurementUnit extends Category implements Serializable {

    private boolean strengthUnit;
    private boolean packUnit;
    private boolean issueUnit;
    private boolean durationUnit;
    private boolean frequencyUnit;
    
    private Double durationInHours;
    private Double frequencyInHours;
    
    

    public boolean isStrengthUnit() {
        return strengthUnit;
    }

    public void setStrengthUnit(boolean strengthUnit) {
        this.strengthUnit = strengthUnit;
    }

    public boolean isPackUnit() {
        return packUnit;
    }

    public void setPackUnit(boolean packUnit) {
        this.packUnit = packUnit;
    }

    public boolean isIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(boolean issueUnit) {
        this.issueUnit = issueUnit;
    }

    public boolean isDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(boolean durationUnit) {
        this.durationUnit = durationUnit;
    }

    public boolean isFrequencyUnit() {
        return frequencyUnit;
    }

    public void setFrequencyUnit(boolean frequencyUnit) {
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
