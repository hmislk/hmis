/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.inward.InwardChargeType;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;


/**
 *
 * @author buddhika
 */
@Entity
public class Speciality extends Category implements Serializable {

    /**
     * The inpatient professional Fee Category to preselect for staff of this
     * speciality (e.g. TECHNICIAN → Technician Fee). Null means "not set" and
     * the entry form falls back to the staff record's subtype (issue #23983).
     */
    @Enumerated(EnumType.STRING)
    private InwardChargeType defaultProfessionalFeeCategory;

    public InwardChargeType getDefaultProfessionalFeeCategory() {
        return defaultProfessionalFeeCategory;
    }

    public void setDefaultProfessionalFeeCategory(InwardChargeType defaultProfessionalFeeCategory) {
        this.defaultProfessionalFeeCategory = defaultProfessionalFeeCategory;
    }
}
