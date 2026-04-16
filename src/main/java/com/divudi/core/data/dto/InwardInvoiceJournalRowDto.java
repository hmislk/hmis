package com.divudi.core.data.dto;

import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * DTO for the Inpatient Invoice Journal report (Issue #19321).
 * One instance per PatientEncounter / BHT.
 *
 * Charge totals are keyed by InwardChargeType — one column per type.
 * Credit and deposit settlement details are included for the reconciliation
 * section (CA–CI in the reference spreadsheet).
 */
public class InwardInvoiceJournalRowDto implements Serializable {

    // -------------------------------------------------------------------------
    // Patient / admission identity
    // -------------------------------------------------------------------------
    private Long   encounterDatabaseId;
    private String bhtNo;
    private String patientName;
    private Date   dateOfAdmission;
    private Date   dateOfDischarge;
    private String finalBillNo;
    private AdmissionType admissionType;

    // -------------------------------------------------------------------------
    // Charge totals — one entry per InwardChargeType
    // -------------------------------------------------------------------------
    private final Map<InwardChargeType, Double> chargesByType =
            new EnumMap<>(InwardChargeType.class);

    // -------------------------------------------------------------------------
    // Deposit / settlement summary
    // -------------------------------------------------------------------------
    private double totalDeposits;
    private double creditSettlementTotal;
    private String creditCompanyName;

    // -------------------------------------------------------------------------
    // Computed helpers
    // -------------------------------------------------------------------------

    public double getChargeForType(InwardChargeType type) {
        return chargesByType.getOrDefault(type, 0.0);
    }

    public void addCharge(InwardChargeType type, double amount) {
        if (type == null) {
            return;
        }
        chargesByType.merge(type, amount, Double::sum);
    }

    public double getGrandTotal() {
        return chargesByType.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public Long getEncounterDatabaseId() { return encounterDatabaseId; }
    public void setEncounterDatabaseId(Long encounterDatabaseId) { this.encounterDatabaseId = encounterDatabaseId; }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Date getDateOfAdmission() { return dateOfAdmission; }
    public void setDateOfAdmission(Date dateOfAdmission) { this.dateOfAdmission = dateOfAdmission; }

    public Date getDateOfDischarge() { return dateOfDischarge; }
    public void setDateOfDischarge(Date dateOfDischarge) { this.dateOfDischarge = dateOfDischarge; }

    public String getFinalBillNo() { return finalBillNo != null ? finalBillNo : ""; }
    public void setFinalBillNo(String finalBillNo) { this.finalBillNo = finalBillNo; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public Map<InwardChargeType, Double> getChargesByType() { return chargesByType; }

    public double getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(double totalDeposits) { this.totalDeposits = totalDeposits; }

    public double getCreditSettlementTotal() { return creditSettlementTotal; }
    public void setCreditSettlementTotal(double creditSettlementTotal) { this.creditSettlementTotal = creditSettlementTotal; }

    public String getCreditCompanyName() { return creditCompanyName != null ? creditCompanyName : ""; }
    public void setCreditCompanyName(String creditCompanyName) { this.creditCompanyName = creditCompanyName; }
}
