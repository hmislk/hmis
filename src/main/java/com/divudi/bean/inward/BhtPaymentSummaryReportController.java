package com.divudi.bean.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.BhtPaymentSummaryDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillItemFacade;
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
    private BillItemFacade billItemFacade;
    @EJB
    private PaymentFacade paymentFacade;

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

    /** Payment methods that have at least one non-zero deposit in the current result set. */
    private List<PaymentMethod> allPaymentMethods = new ArrayList<>();

    /** Column totals, keyed by PaymentMethod ordinal. */
    private Map<PaymentMethod, Double> columnTotals = new HashMap<>();
    private double grandTotalDeposits;
    private double grandTotalCreditSettlement;

    // -------------------------------------------------------------------------
    // Main generate method
    // -------------------------------------------------------------------------

    public void generateReport() {
        reportRows = new ArrayList<>();
        columnTotals = new HashMap<>();
        grandTotalDeposits = 0;
        grandTotalCreditSettlement = 0;

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            allPaymentMethods = new ArrayList<>();
            return;
        }

        for (PatientEncounter enc : encounters) {
            BhtPaymentSummaryDTO row = buildRow(enc);
            reportRows.add(row);

            // accumulate column totals across all known methods
            for (PaymentMethod pm : PaymentMethod.values()) {
                columnTotals.merge(pm, row.getDepositForMethod(pm), Double::sum);
            }
            grandTotalDeposits += row.getTotalDeposits();
            grandTotalCreditSettlement += row.getCreditSettlementTotal();
        }

        // only show columns where at least one BHT had a non-zero deposit
        allPaymentMethods = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            if (columnTotals.getOrDefault(pm, 0.0) != 0.0) {
                allPaymentMethods.add(pm);
            }
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

        // --- credit settlements ---
        List<BillItem> creditItems = fetchCreditSettlementItems(enc);
        for (BillItem bi : creditItems) {
            String companyName = "";
            if (bi.getReferenceBill() != null && bi.getReferenceBill().getCreditCompany() != null) {
                companyName = bi.getReferenceBill().getCreditCompany().getName();
            }
            row.addCreditSettlement(bi.getNetValue(), companyName);
        }

        return row;
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
     * Fetch BillItems from INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED and
     * INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION bills that reference this encounter.
     * Including both types allows cancellation items (negative netValue) to naturally
     * offset the received items, so the net total is correct even when payments are cancelled.
     */
    private List<BillItem> fetchCreditSettlementItems(PatientEncounter enc) {
        String jpql = "select bi from BillItem bi"
                + " where bi.retired = false"
                + " and bi.bill.retired = false"
                + " and bi.bill.billTypeAtomic in :btas"
                + " and bi.patientEncounter = :enc";
        Map<String, Object> params = new HashMap<>();
        params.put("btas", java.util.Arrays.asList(
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION));
        params.put("enc", enc);
        return billItemFacade.findByJpql(jpql, params);
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    public double getColumnTotal(PaymentMethod pm) {
        return columnTotals.getOrDefault(pm, 0.0);
    }

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
        columnTotals = new HashMap<>();
        grandTotalDeposits = 0;
        grandTotalCreditSettlement = 0;
        allPaymentMethods = new ArrayList<>();
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

    public List<PaymentMethod> getAllPaymentMethods() { return allPaymentMethods; }

    public Map<PaymentMethod, Double> getColumnTotals() { return columnTotals; }

    public double getGrandTotalDeposits() { return grandTotalDeposits; }

    public double getGrandTotalCreditSettlement() { return grandTotalCreditSettlement; }
}
