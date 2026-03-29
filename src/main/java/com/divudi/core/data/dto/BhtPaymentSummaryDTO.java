package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * DTO for BHT Deposit and Credit Settlement Summary Report (Issue #19345).
 * One instance per PatientEncounter (BHT).
 *
 * Deposit payment breakdown is keyed by PaymentMethod.
 * Credit settlement is the sum of all INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED
 * BillItems linked to this encounter.
 */
public class BhtPaymentSummaryDTO implements Serializable {

    private Long encounterDatabaseId;
    private String bhtNo;
    private String patientName;
    private Date dateOfAdmission;
    private Date dateOfDischarge;
    private AdmissionType admissionType;

    /** Sum of deposit bill payments, keyed by payment method. */
    private Map<PaymentMethod, Double> depositsByMethod = new EnumMap<>(PaymentMethod.class);

    /** Sum of INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED bill items for this BHT. */
    private double creditSettlementTotal;

    /** Slash-separated names of credit companies from the settlement bills. */
    private String creditCompanyNames;

    public BhtPaymentSummaryDTO() {
    }

    // --- computed helpers ---

    public double getTotalDeposits() {
        return depositsByMethod.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getDepositForMethod(PaymentMethod method) {
        return depositsByMethod.getOrDefault(method, 0.0);
    }

    public void addDeposit(PaymentMethod method, double amount) {
        if (method == null) {
            return;
        }
        depositsByMethod.merge(method, amount, Double::sum);
    }

    public void addCreditSettlement(double amount, String companyName) {
        creditSettlementTotal += amount;
        if (companyName != null && !companyName.isBlank()) {
            if (creditCompanyNames == null || creditCompanyNames.isBlank()) {
                creditCompanyNames = companyName;
            } else if (!creditCompanyNames.contains(companyName)) {
                creditCompanyNames = creditCompanyNames + " / " + companyName;
            }
        }
    }

    // --- getters / setters ---

    public Long getEncounterDatabaseId() {
        return encounterDatabaseId;
    }

    public void setEncounterDatabaseId(Long encounterDatabaseId) {
        this.encounterDatabaseId = encounterDatabaseId;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public void setDateOfAdmission(Date dateOfAdmission) {
        this.dateOfAdmission = dateOfAdmission;
    }

    public Date getDateOfDischarge() {
        return dateOfDischarge;
    }

    public void setDateOfDischarge(Date dateOfDischarge) {
        this.dateOfDischarge = dateOfDischarge;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Map<PaymentMethod, Double> getDepositsByMethod() {
        return depositsByMethod;
    }

    public double getCreditSettlementTotal() {
        return creditSettlementTotal;
    }

    public void setCreditSettlementTotal(double creditSettlementTotal) {
        this.creditSettlementTotal = creditSettlementTotal;
    }

    public String getCreditCompanyNames() {
        return creditCompanyNames != null ? creditCompanyNames : "";
    }

    public void setCreditCompanyNames(String creditCompanyNames) {
        this.creditCompanyNames = creditCompanyNames;
    }
}
