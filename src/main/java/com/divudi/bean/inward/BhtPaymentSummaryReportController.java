package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.BhtPaymentSummaryDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for BHT Deposit and Credit Settlement Summary Report.
 * Issue #19345
 *
 * One row per PatientEncounter (BHT). Columns show deposit totals broken down
 * by PaymentMethod plus a combined credit-settlement column.
 */
@Named
@SessionScoped
public class BhtPaymentSummaryReportController implements Serializable {

    // -------------------------------------------------------------------------
    // EJBs
    // -------------------------------------------------------------------------
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFacade billFacade;

    // -------------------------------------------------------------------------
    // Filter fields
    // -------------------------------------------------------------------------
    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();

    /** "admissionDate" or "dischargeDate" */
    private String dateBasis = "dischargeDate";

    private AdmissionStatus admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private Institution site;
    private Department department;

    // -------------------------------------------------------------------------
    // Report output
    // -------------------------------------------------------------------------
    private List<BhtPaymentSummaryDTO> reportRows;

    private double grandTotalDeposits;
    private double grandTotalDepositCash;
    private double grandTotalDepositCard;
    private double grandTotalDepositOther;
    private double grandTotalPostFinalPayments;
    private double grandTotalPostFinalCash;
    private double grandTotalPostFinalCard;
    private double grandTotalPostFinalOther;
    private double grandTotalCreditBilled;
    private double grandTotalCreditSettlement;
    private double grandTotalCreditBalance;
    private double grandTotalFinalBills;
    private double grandTotalBalance;

    // -------------------------------------------------------------------------
    // Main generate method
    // -------------------------------------------------------------------------

    public void generateReport() {
        reportRows = new ArrayList<>();
        grandTotalDeposits = 0;
        grandTotalDepositCash = 0;
        grandTotalDepositCard = 0;
        grandTotalDepositOther = 0;
        grandTotalPostFinalPayments = 0;
        grandTotalPostFinalCash = 0;
        grandTotalPostFinalCard = 0;
        grandTotalPostFinalOther = 0;
        grandTotalCreditBilled = 0;
        grandTotalCreditSettlement = 0;
        grandTotalCreditBalance = 0;
        grandTotalFinalBills = 0;
        grandTotalBalance = 0;

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        for (PatientEncounter enc : encounters) {
            BhtPaymentSummaryDTO row = buildRow(enc);
            reportRows.add(row);

            grandTotalDeposits += row.getTotalDeposits();
            grandTotalDepositCash += row.getDepositCash();
            grandTotalDepositCard += row.getDepositCard();
            grandTotalDepositOther += row.getDepositOther();
            grandTotalPostFinalPayments += row.getTotalPostFinalPayments();
            grandTotalPostFinalCash += row.getPostFinalCash();
            grandTotalPostFinalCard += row.getPostFinalCard();
            grandTotalPostFinalOther += row.getPostFinalOther();
            grandTotalCreditBilled += row.getCreditBilledTotal();
            grandTotalCreditSettlement += row.getCreditSettlementTotal();
            grandTotalCreditBalance += row.getCreditBalance();
            grandTotalFinalBills += row.getFinalBillTotal();
            grandTotalBalance += row.getTotalBalance();
        }
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    private List<PatientEncounter> fetchEncounters() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select distinct c from PatientEncounter c where c.retired = false");

        // --- date basis ---
        if (fromDate != null && toDate != null) {
            if ("admissionDate".equals(dateBasis)) {
                jpql.append(" and c.dateOfAdmission between :fromDate and :toDate");
            } else {
                // default: dischargeDate
                jpql.append(" and c.dateOfDischarge between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }

        // --- admission status ---
        if (admissionStatus != null && admissionStatus != AdmissionStatus.ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append(" and c.discharged = :dis");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", false);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", true);
                    break;
                default:
                    break;
            }
        }

        // --- admission type ---
        if (admissionType != null) {
            jpql.append(" and c.admissionType = :admType");
            params.put("admType", admissionType);
        }

        // --- institution ---
        if (institution != null) {
            jpql.append(" and c.institution = :ins");
            params.put("ins", institution);
        }

        // --- site (department.site) ---
        if (site != null) {
            jpql.append(" and c.department.site = :site");
            params.put("site", site);
        }

        // --- department ---
        if (department != null) {
            jpql.append(" and c.department = :dept");
            params.put("dept", department);
        }

        // --- payment method ---
        if (paymentMethod != null) {
            jpql.append(" and c.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }

        jpql.append(" order by c.bhtNo");

        return patientEncounterFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    private BhtPaymentSummaryDTO buildRow(PatientEncounter enc) {
        BhtPaymentSummaryDTO row = new BhtPaymentSummaryDTO();
        row.setEncounterDatabaseId(enc.getId());
        row.setBhtNo(enc.getBhtNo());
        row.setPatientName(enc.getPatient() != null && enc.getPatient().getPerson() != null
                ? enc.getPatient().getPerson().getNameWithTitle() : "");
        row.setDateOfAdmission(enc.getDateOfAdmission());
        row.setDateOfDischarge(enc.getDateOfDischarge());
        row.setAdmissionType(enc.getAdmissionType());

        // --- deposit payments ---
        List<Payment> depositPayments = fetchDepositPayments(enc);
        for (Payment p : depositPayments) {
            row.addDeposit(p.getPaymentMethod(), Math.abs(p.getPaidValue()));
        }

        // --- post-final-bill payments ---
        // No Math.abs() here — see fetchPostFinalPayments() javadoc: cancellations
        // arrive as separate negative-amount rows that must net out.
        List<Payment> postFinalPayments = fetchPostFinalPayments(enc);
        for (Payment p : postFinalPayments) {
            row.addPostFinalPayment(p.getPaymentMethod(), p.getPaidValue());
        }

        // --- credit company bills ---
        List<Bill> creditCompanyBills = fetchCreditCompanyBills(enc);
        for (Bill b : creditCompanyBills) {
            String companyName = b.getCreditCompany() != null ? b.getCreditCompany().getName() : "";
            row.addCreditCompanyBill(b.getNetTotal(), b.getPaidAmount(), companyName);
        }

        // --- final bill (latest non-cancelled InwardFinalBill for this encounter) ---
        Bill finalBill = fetchFinalBill(enc);
        if (finalBill != null) {
            row.setFinalBillNumber(finalBill.getDeptId());
            row.setFinalBillTotal(finalBill.getNetTotal());
        }

        return row;
    }

    /**
     * Fetch the latest non-cancelled InwardFinalBill for the given encounter.
     */
    private Bill fetchFinalBill(PatientEncounter enc) {
        String jpql = "select b from BilledBill b"
                + " where b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType = :bt"
                + " and b.confirmedFinalBill = true"
                + " and b.patientEncounter = :enc"
                + " order by b.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.InwardFinalBill);
        params.put("enc", enc);
        return billFacade.findFirstByJpql(jpql, params);
    }

    /**
     * Fetch all Payment records linked to INWARD_DEPOSIT bills for this encounter.
     * Deposit bills link to the encounter via bill.patientEncounter directly.
     */
    private List<Payment> fetchDepositPayments(PatientEncounter enc) {
        String jpql = "select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.cancelled = false"
                + " and p.bill.billTypeAtomic = :bta"
                + " and p.bill.patientEncounter = :enc";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_DEPOSIT);
        params.put("enc", enc);
        return paymentFacade.findByJpql(jpql, params);
    }

    /**
     * Fetch post-final-bill payments for this encounter.
     *
     * Deliberately does NOT filter on {@code bill.cancelled=false}: cancellation
     * and refund of a post-final-bill payment are represented as a separate
     * negative-amount row of the same bill type, rather than by flagging the
     * original row, so summing every row's paid value with its natural sign
     * (no {@code Math.abs()}) nets out correctly. This mirrors
     * {@link PostFinalBillInwardPaymentController#getPostFinalPaymentTotal},
     * which uses the same unfiltered, sign-preserving sum pattern.
     */
    private List<Payment> fetchPostFinalPayments(PatientEncounter enc) {
        String jpql = "select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.billType = :bt"
                + " and p.bill.patientEncounter = :enc";
        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PostFinalBillInwardPayment);
        params.put("enc", enc);
        return paymentFacade.findByJpql(jpql, params);
    }

    /**
     * Fetch credit company bills (INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY)
     * that reference this encounter directly, excluding cancelled/refunded bills.
     */
    private List<Bill> fetchCreditCompanyBills(PatientEncounter enc) {
        String jpql = "select b from Bill b"
                + " where b.retired = false"
                + " and b.billTypeAtomic = :bta"
                + " and b.paymentMethod = :pm"
                + " and b.cancelledBill is null"
                + " and b.refundedBill is null"
                + " and b.patientEncounter = :enc";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        params.put("pm", PaymentMethod.Credit);
        params.put("enc", enc);
        return billFacade.findByJpql(jpql, params);
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        dateBasis = "dischargeDate";
        admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
        admissionType = null;
        paymentMethod = null;
        institution = null;
        site = null;
        department = null;
        reportRows = null;
        grandTotalDeposits = 0;
        grandTotalDepositCash = 0;
        grandTotalDepositCard = 0;
        grandTotalDepositOther = 0;
        grandTotalPostFinalPayments = 0;
        grandTotalPostFinalCash = 0;
        grandTotalPostFinalCard = 0;
        grandTotalPostFinalOther = 0;
        grandTotalCreditBilled = 0;
        grandTotalCreditSettlement = 0;
        grandTotalCreditBalance = 0;
        grandTotalFinalBills = 0;
        grandTotalBalance = 0;
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public String getDateBasis() { return dateBasis; }
    public void setDateBasis(String dateBasis) { this.dateBasis = dateBasis; }

    public AdmissionStatus getAdmissionStatus() { return admissionStatus; }
    public void setAdmissionStatus(AdmissionStatus admissionStatus) { this.admissionStatus = admissionStatus; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Institution getSite() { return site; }
    public void setSite(Institution site) { this.site = site; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public List<BhtPaymentSummaryDTO> getReportRows() { return reportRows; }

    public double getGrandTotalDeposits() { return grandTotalDeposits; }

    public double getGrandTotalDepositCash() { return grandTotalDepositCash; }

    public double getGrandTotalDepositCard() { return grandTotalDepositCard; }

    public double getGrandTotalDepositOther() { return grandTotalDepositOther; }

    public double getGrandTotalPostFinalPayments() { return grandTotalPostFinalPayments; }

    public double getGrandTotalPostFinalCash() { return grandTotalPostFinalCash; }

    public double getGrandTotalPostFinalCard() { return grandTotalPostFinalCard; }

    public double getGrandTotalPostFinalOther() { return grandTotalPostFinalOther; }

    public double getGrandTotalCreditBilled() { return grandTotalCreditBilled; }

    public double getGrandTotalCreditSettlement() { return grandTotalCreditSettlement; }

    public double getGrandTotalCreditBalance() { return grandTotalCreditBalance; }

    public double getGrandTotalFinalBills() { return grandTotalFinalBills; }

    public double getGrandTotalBalance() { return grandTotalBalance; }
}
