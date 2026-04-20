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

    /** Source record; null for the single-company fallback row and patient rows. */
    private EncounterCreditCompany encounterCreditCompany;

    /** The credit company institution; null for patient-portion rows. */
    private Institution creditCompany;

    /** Amount allocated to this company or to the patient; editable by the cashier. */
    private double allocatedAmount;

    /**
     * True when this row represents the patient's co-payment portion rather than
     * a credit company commitment. Patient rows are displayed in the allocation
     * table but are NOT saved as CC commitment bills — the patient settles via the
     * normal inward payment flow.
     */
    private boolean patientPortion;

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

    /**
     * Constructor for a patient co-payment row. The patient is responsible for
     * the given amount; no CC commitment bill is created for this row.
     */
    public CreditCompanyAllocation(double allocatedAmount, boolean patientPortion) {
        this.allocatedAmount = allocatedAmount;
        this.patientPortion = patientPortion;
    }

    /** Display name: "Patient" for patient rows, institution name otherwise. */
    public String getCompanyName() {
        if (patientPortion) {
            return "Patient";
        }
        return creditCompany != null ? creditCompany.getName() : "";
    }

    public boolean isPatientPortion() {
        return patientPortion;
    }

    public void setPatientPortion(boolean patientPortion) {
        this.patientPortion = patientPortion;
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
