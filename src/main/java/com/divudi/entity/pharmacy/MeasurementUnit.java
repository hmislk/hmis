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

}
