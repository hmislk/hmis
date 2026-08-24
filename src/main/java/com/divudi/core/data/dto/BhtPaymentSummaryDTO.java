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

    /** Sum of post-final-bill payments, keyed by payment method. */
    private Map<PaymentMethod, Double> postFinalPaymentsByMethod = new EnumMap<>(PaymentMethod.class);

    /** Sum of billed amounts (netTotal) of credit company bills for this BHT. */
    private double creditBilledTotal;

    /** Sum of settled (paid) amounts of credit company bills for this BHT. */
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

    public double getDepositCash() {
        return getDepositForMethod(PaymentMethod.Cash);
    }

    public double getDepositCard() {
        return getDepositForMethod(PaymentMethod.Card);
    }

    public double getDepositOther() {
        return getTotalDeposits() - getDepositCash() - getDepositCard();
    }

    public double getTotalPostFinalPayments() {
        return postFinalPaymentsByMethod.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getPostFinalPaymentForMethod(PaymentMethod method) {
        return postFinalPaymentsByMethod.getOrDefault(method, 0.0);
    }

    /**
     * Amounts are merged with their natural sign — callers must NOT pass
     * {@code Math.abs(...)} here. Unlike deposits, post-final-bill payment
     * cancellations/refunds arrive as separate negative-amount rows that
     * must net out against the original positive row.
     */
    public void addPostFinalPayment(PaymentMethod method, double amount) {
        if (method == null) {
            LOG.log(Level.WARNING, "BHT {0}: post-final payment of {1} has null PaymentMethod — amount dropped from totals",
                    new Object[]{bhtNo, amount});
            return;
        }
        postFinalPaymentsByMethod.merge(method, amount, Double::sum);
    }

    public double getPostFinalCash() {
        return getPostFinalPaymentForMethod(PaymentMethod.Cash);
    }

    public double getPostFinalCard() {
        return getPostFinalPaymentForMethod(PaymentMethod.Card);
    }

    /**
     * Post-final-bill payments made via {@link PaymentMethod#Credit} (settling
     * the final bill on credit) — broken out from {@link #getPostFinalOther()}
     * per issue #23262, so a distinct "Final Credit" column can be shown
     * alongside Final Cash / Final Other.
     */
    public double getPostFinalCredit() {
        return getPostFinalPaymentForMethod(PaymentMethod.Credit);
    }

    public double getPostFinalOther() {
        return getTotalPostFinalPayments() - getPostFinalCash() - getPostFinalCard() - getPostFinalCredit();
    }

    public void addCreditCompanyBill(double billedAmount, double settledAmount, String companyName) {
        creditBilledTotal += billedAmount;
        creditSettlementTotal += settledAmount;
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

    public double getCreditBalance() {
        return creditBilledTotal - creditSettlementTotal;
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

    public Map<PaymentMethod, Double> getPostFinalPaymentsByMethod() {
        return postFinalPaymentsByMethod;
    }

    public double getCreditBilledTotal() {
        return creditBilledTotal;
    }

    public void setCreditBilledTotal(double creditBilledTotal) {
        this.creditBilledTotal = creditBilledTotal;
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
     * Balance = final bill net total − (deposits + post-final payments + credit billed).
     * Positive means amount still due; negative means overpayment / refund due.
     * Returns 0 when no final bill exists — checked via finalBillNumber presence,
     * not finalBillTotal, so a genuine final bill with a zero net total still
     * nets out any deposits/post-final payments/credit billed instead of being
     * mistaken for "no final bill".
     */
    public double getTotalBalance() {
        if (finalBillNumber == null || finalBillNumber.isBlank()) {
            return 0.0;
        }
        return finalBillTotal - getTotalDeposits() - getTotalPostFinalPayments() - creditBilledTotal;
    }
}
