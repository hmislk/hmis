package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.InwardInvoiceJournalRowDto;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for the Inpatient Invoice Journal report (Issue #19321).
 *
 * One row per PatientEncounter. Columns show charge totals broken down by
 * InwardChargeType, followed by deposit and credit settlement totals.
 *
 * Performance: charge totals are fetched in a single bulk JPQL GROUP BY query
 * for the entire result set, then pivoted in Java — avoiding N+1 per encounter.
 */
@Named
@SessionScoped
public class InwardInvoiceJournalController implements Serializable {

    // -------------------------------------------------------------------------
    // EJBs / CDI
    // -------------------------------------------------------------------------
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    // -------------------------------------------------------------------------
    // Filter fields
    // -------------------------------------------------------------------------
    private Date fromDate = startOfCurrentMonth();
    private Date toDate   = new Date();

    /** "admissionDate" or "dischargeDate" */
    private String dateBasis = "dischargeDate";

    private AdmissionStatus admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType   admissionType;
    private Institution     institution;
    private Institution     site;
    private Department      department;

    // -------------------------------------------------------------------------
    // Report output
    // -------------------------------------------------------------------------
    private List<InwardInvoiceJournalRowDto> reportRows;

    /**
     * All InwardChargeType values — drives the dynamic columns in XHTML.
     * Columns where every row has 0 are hidden via rendered="#{...}".
     */
    private final List<InwardChargeType> allChargeTypes = Arrays.asList(InwardChargeType.values());

    /**
     * Set of charge types that have at least one non-zero value in the current
     * result set. Used by XHTML to suppress empty columns.
     */
    private Set<InwardChargeType> activeChargeTypes = EnumSet.noneOf(InwardChargeType.class);

    /** Column totals keyed by InwardChargeType. */
    private Map<InwardChargeType, Double> columnTotals = new EnumMap<>(InwardChargeType.class);

    private double grandTotalCharges;
    private double grandTotalDeposits;
    private double grandTotalCreditSettlement;

    // -------------------------------------------------------------------------
    // Main generate method
    // -------------------------------------------------------------------------

    public void generateReport() {
        reportRows               = new ArrayList<>();
        columnTotals             = new EnumMap<>(InwardChargeType.class);
        activeChargeTypes        = EnumSet.noneOf(InwardChargeType.class);
        grandTotalCharges        = 0;
        grandTotalDeposits       = 0;
        grandTotalCreditSettlement = 0;

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        // --- bulk fetch charge totals for all encounters at once ---
        Map<Long, Map<InwardChargeType, Double>> chargeMap = fetchChargesByEncounter(encounters);

        // --- bulk fetch deposit totals for all encounters at once ---
        Map<Long, Double> depositMap = fetchDepositTotalsByEncounter(encounters);

        // --- bulk fetch credit settlement totals ---
        Map<Long, double[]> creditMap = fetchCreditSettlementByEncounter(encounters);

        // --- build one row per encounter ---
        for (PatientEncounter enc : encounters) {
            InwardInvoiceJournalRowDto row = buildRow(enc, chargeMap, depositMap, creditMap);
            reportRows.add(row);

            // accumulate column totals and active types
            for (InwardChargeType ct : allChargeTypes) {
                double v = row.getChargeForType(ct);
                if (v != 0) {
                    activeChargeTypes.add(ct);
                    columnTotals.merge(ct, v, Double::sum);
                }
            }
            grandTotalCharges          += row.getGrandTotal();
            grandTotalDeposits         += row.getTotalDeposits();
            grandTotalCreditSettlement += row.getCreditSettlementTotal();
        }
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    private List<PatientEncounter> fetchEncounters() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select distinct c from PatientEncounter c where c.retired = false");

        // When filtering not-yet-discharged patients, discharge date is null —
        // always use admission date as the date basis for that status.
        boolean forceAdmissionDate = admissionStatus == AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;

        if (fromDate != null && toDate != null) {
            if ("admissionDate".equals(dateBasis) || forceAdmissionDate) {
                jpql.append(" and c.dateOfAdmission between :fromDate and :toDate");
            } else {
                jpql.append(" and c.dateOfDischarge between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate",   toDate);
        }

        if (admissionStatus != null && admissionStatus != AdmissionStatus.ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append(" and c.discharged = :dis");
                    params.put("dis", false);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf",  true);
                    break;
                default:
                    break;
            }
        }

        if (admissionType != null) {
            jpql.append(" and c.admissionType = :admType");
            params.put("admType", admissionType);
        }

        if (institution != null) {
            jpql.append(" and c.institution = :ins");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append(" and c.department.site = :site");
            params.put("site", site);
        }

        if (department != null) {
            jpql.append(" and c.department = :dept");
            params.put("dept", department);
        }

        jpql.append(" order by c.bhtNo");
        return patientEncounterFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    /**
     * Single bulk query: sum(netValue) grouped by (patientEncounter, inwardChargeType)
     * for all encounters in the set, covering all non-cancelled bill items.
     * Returns Map< encounterId, Map<InwardChargeType, total> >.
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchChargesByEncounter(
            List<PatientEncounter> encounters) {

        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();

        String jpql = "select bi.patientEncounter.id, bi.inwardChargeType, sum(bi.netValue)"
                + " from BillItem bi"
                + " where bi.retired = false"
                + " and bi.bill.retired = false"
                + " and bi.bill.cancelled = false"
                + " and bi.patientEncounter in :encs"
                + " and bi.inwardChargeType is not null"
                + " group by bi.patientEncounter.id, bi.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("encs", encounters);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                Long            encId  = (Long) r[0];
                InwardChargeType type  = (InwardChargeType) r[1];
                Double           total = ((Number) r[2]).doubleValue();
                result.computeIfAbsent(encId, k -> new EnumMap<>(InwardChargeType.class))
                      .put(type, total);
            }
        }
        return result;
    }

    /**
     * Single bulk query: sum of deposit payments grouped by encounter.
     * Returns Map< encounterId, totalDeposits >.
     */
    private Map<Long, Double> fetchDepositTotalsByEncounter(List<PatientEncounter> encounters) {
        Map<Long, Double> result = new HashMap<>();

        String jpql = "select p.bill.patientEncounter.id, sum(p.paidValue)"
                + " from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.cancelled = false"
                + " and p.bill.billTypeAtomic = :bta"
                + " and p.bill.patientEncounter in :encs"
                + " group by p.bill.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("bta",  BillTypeAtomic.INWARD_DEPOSIT);
        params.put("encs", encounters);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                Long   encId = (Long) r[0];
                double total = Math.abs(((Number) r[1]).doubleValue());
                result.put(encId, total);
            }
        }
        return result;
    }

    /**
     * Single bulk query: credit settlement totals grouped by encounter.
     * Returns Map< encounterId, double[]{total} >.
     */
    private Map<Long, double[]> fetchCreditSettlementByEncounter(List<PatientEncounter> encounters) {
        Map<Long, double[]> result = new HashMap<>();

        String jpql = "select bi.patientEncounter.id, sum(bi.netValue)"
                + " from BillItem bi"
                + " where bi.retired = false"
                + " and bi.bill.retired = false"
                + " and bi.bill.cancelled = false"
                + " and bi.bill.billTypeAtomic = :bta"
                + " and bi.patientEncounter in :encs"
                + " group by bi.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("bta",  BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
        params.put("encs", encounters);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                Long   encId = (Long) r[0];
                double total = Math.abs(((Number) r[1]).doubleValue());
                result.put(encId, new double[]{total});
            }
        }
        return result;
    }

    private InwardInvoiceJournalRowDto buildRow(
            PatientEncounter enc,
            Map<Long, Map<InwardChargeType, Double>> chargeMap,
            Map<Long, Double> depositMap,
            Map<Long, double[]> creditMap) {

        InwardInvoiceJournalRowDto row = new InwardInvoiceJournalRowDto();
        row.setEncounterDatabaseId(enc.getId());
        row.setBhtNo(enc.getBhtNo());
        row.setPatientName(enc.getPatient() != null && enc.getPatient().getPerson() != null
                ? enc.getPatient().getPerson().getNameWithTitle() : "");
        row.setDateOfAdmission(enc.getDateOfAdmission());
        row.setDateOfDischarge(enc.getDateOfDischarge());
        row.setAdmissionType(enc.getAdmissionType());

        if (enc.getFinalBill() != null) {
            row.setFinalBillNo(enc.getFinalBill().getIdStr());
        }

        // charges
        Map<InwardChargeType, Double> charges = chargeMap.get(enc.getId());
        if (charges != null) {
            charges.forEach(row::addCharge);
        }

        // deposits
        row.setTotalDeposits(depositMap.getOrDefault(enc.getId(), 0.0));

        // credit settlement
        double[] credit = creditMap.get(enc.getId());
        if (credit != null) {
            row.setCreditSettlementTotal(credit[0]);
        }
        if (enc.getCreditCompany() != null) {
            row.setCreditCompanyName(enc.getCreditCompany().getName());
        }

        return row;
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    public void onInstitutionChange() {
        site       = null;
        department = null;
    }

    public void onSiteChange() {
        department = null;
    }

    public boolean isChargeTypeActive(InwardChargeType type) {
        return activeChargeTypes.contains(type);
    }

    public double getColumnTotal(InwardChargeType type) {
        return columnTotals.getOrDefault(type, 0.0);
    }

    public String getChargeTypeLabel(InwardChargeType type) {
        return configOptionApplicationController.getInwardChargeTypeLabel(type);
    }

    public String navigateToReport() {
        makeNull();
        return "/inward/inward_report_invoice_journal?faces-redirect=true";
    }

    public void makeNull() {
        fromDate         = startOfCurrentMonth();
        toDate           = new Date();
        dateBasis        = "dischargeDate";
        admissionStatus  = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
        admissionType    = null;
        institution      = null;
        site             = null;
        department       = null;
        reportRows       = null;
        columnTotals     = new EnumMap<>(InwardChargeType.class);
        activeChargeTypes = EnumSet.noneOf(InwardChargeType.class);
        grandTotalCharges = grandTotalDeposits = grandTotalCreditSettlement = 0;
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

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Institution getSite() { return site; }
    public void setSite(Institution site) { this.site = site; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public List<InwardInvoiceJournalRowDto> getReportRows() { return reportRows; }

    public List<InwardChargeType> getAllChargeTypes() { return allChargeTypes; }

    public Map<InwardChargeType, Double> getColumnTotals() { return columnTotals; }

    public double getGrandTotalCharges() { return grandTotalCharges; }
    public double getGrandTotalDeposits() { return grandTotalDeposits; }
    public double getGrandTotalCreditSettlement() { return grandTotalCreditSettlement; }
}
