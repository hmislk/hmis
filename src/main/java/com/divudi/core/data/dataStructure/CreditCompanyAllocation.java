/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Institution;
import java.io.Serializable;

/**
 * Transient (in-memory) allocation of the inward final bill net due across
 * credit companies. Populated when Process is clicked; consumed by Settle.
 *
 * @author Dr M H B Ariyaratne
 */
public class CreditCompanyAllocation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Source record; null for the single-company fallback row. */
    private EncounterCreditCompany encounterCreditCompany;

    /** The credit company institution. */
    private Institution creditCompany;

    /** Amount allocated to this company; editable by the cashier. */
    private double allocatedAmount;

    /**
     * Constructor for normal rows built from an {@link EncounterCreditCompany}.
     */
    public CreditCompanyAllocation(EncounterCreditCompany ecc, double allocatedAmount) {
        this(ecc.getInstitution(), allocatedAmount);
        this.encounterCreditCompany = ecc;
    }

    /**
     * Constructor for the fallback row when no {@link EncounterCreditCompany}
     * records exist but {@code PatientEncounter.creditCompany} is set.
     */
    public CreditCompanyAllocation(Institution creditCompany, double allocatedAmount) {
        this.creditCompany = creditCompany;
        this.allocatedAmount = allocatedAmount;
    }

    /** Display name derived from the institution. */
    public String getCompanyName() {
        return creditCompany != null ? creditCompany.getName() : "";
    }

    public EncounterCreditCompany getEncounterCreditCompany() {
        return encounterCreditCompany;
    }

    public void setEncounterCreditCompany(EncounterCreditCompany encounterCreditCompany) {
        this.encounterCreditCompany = encounterCreditCompany;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public double getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(double allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }
}
