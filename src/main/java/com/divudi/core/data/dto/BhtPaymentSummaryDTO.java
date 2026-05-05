package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DTO for BHT Deposit and Credit Settlement Summary Report (Issue #19345).
 * One instance per PatientEncounter (BHT).
 *
 * Deposit payment breakdown is keyed by PaymentMethod.
 * Credit settlement is the sum of all INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED
 * BillItems linked to this encounter.
 */
public class BhtPaymentSummaryDTO implements Serializable {

    private static final Logger LOG = Logger.getLogger(BhtPaymentSummaryDTO.class.getName());

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
    private final Set<String> creditCompanyNamesSet = new HashSet<>();

    /** Final bill department ID (deptId) for this BHT — null if no final bill yet. */
    private String finalBillNumber;

    /** Net total of the final bill. */
    private double finalBillTotal;

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
            LOG.log(Level.WARNING, "BHT {0}: deposit of {1} has null PaymentMethod — amount dropped from totals",
                    new Object[]{bhtNo, amount});
            return;
        }
        depositsByMethod.merge(method, amount, Double::sum);
    }

    public void addCreditSettlement(double amount, String companyName) {
        creditSettlementTotal += amount;
        if (companyName != null && !companyName.isBlank()) {
            if (creditCompanyNamesSet.add(companyName)) {
                if (creditCompanyNames == null || creditCompanyNames.isBlank()) {
                    creditCompanyNames = companyName;
                } else {
                    creditCompanyNames = creditCompanyNames + " / " + companyName;
                }
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

    public String getFinalBillNumber() {
        return finalBillNumber != null ? finalBillNumber : "";
    }

    public void setFinalBillNumber(String finalBillNumber) {
        this.finalBillNumber = finalBillNumber;
    }

    public double getFinalBillTotal() {
        return finalBillTotal;
    }

    public void setFinalBillTotal(double finalBillTotal) {
        this.finalBillTotal = finalBillTotal;
    }

    /**
     * Balance = final bill net total − (deposits + credit settlements collected).
     * Positive means amount still due; negative means overpayment / refund due.
     * Returns 0 when no final bill exists.
     */
    public double getBalance() {
        if (finalBillTotal == 0.0) {
            return 0.0;
        }
        return finalBillTotal - getTotalDeposits() - creditSettlementTotal;
    }
}
