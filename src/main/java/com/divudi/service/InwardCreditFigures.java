package com.divudi.service;

import com.divudi.core.data.BillCategory;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CountedServiceType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.InstitutionBillEncounter;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.InstitutionFacade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-admission credit company figures (#25601), shared by every inpatient
 * credit report so they all read the same numbers.
 *
 * Company receipts and cancels recalculate each commitment bill
 * (INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY, referenceBill = the admission's
 * current final bill) from the database. finalBill.paidAmount is not used
 * here because it holds patient and company money together (recounted by
 * InwardBeanController.recountFinalBillPaid, #26152); these figures need them
 * apart. Patient money is read from the InwardPaymentBill bills instead, and
 * the encounter's creditPaidAmount (no longer written) must not be read.
 *
 * A plain class (not a bean): each caller keeps its own instance, which caches
 * what it has loaded. Not thread safe, like the session beans that hold it.
 */
public class InwardCreditFigures implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BillFacade billFacade;
    private final InstitutionFacade institutionFacade;

    /** Admission id -> patient money paid (non-Credit InwardPaymentBill bills, signed). */
    private final Map<Long, Double> patientPaidByEncounterId = new HashMap<>();

    /** Final bill id -> {committed, paid by company}; an empty array means the final bill has no commitment bills. */
    private final Map<Long, double[]> ccFiguresByFinalBillId = new HashMap<>();

    /** Final bill id -> commitment company id (null = bill has none) -> {committed, paid}. */
    private final Map<Long, Map<Long, double[]>> ccCompanyRawByFinalBillId = new HashMap<>();

    /** Final bill id -> per-company figures after the company fallback (cached). */
    private final Map<Long, List<CompanyFigures>> ccCompanyMergedByFinalBillId = new HashMap<>();

    private final Map<Long, Institution> institutionsById = new HashMap<>();

    /** Final bills whose commitment query failed; they have no figures (not legacy ones) until the next load(). */
    private final Set<Long> failedFinalBillIds = new HashSet<>();

    public InwardCreditFigures(BillFacade billFacade, InstitutionFacade institutionFacade) {
        this.billFacade = billFacade;
        this.institutionFacade = institutionFacade;
    }

    /** One company's share of one admission: committed and paid from its commitment bills. */
    public static class CompanyFigures implements Serializable {

        private static final long serialVersionUID = 1L;

        public final Institution company;
        public double committed;
        public double paid;

        public CompanyFigures(Institution company, double committed, double paid) {
            this.company = company;
            this.committed = committed;
            this.paid = paid;
        }

        public double due() {
            return Math.max(0.0, committed - paid);
        }
    }

    public static boolean isSameCompany(Institution a, Institution b) {
        Long ai = a == null ? null : a.getId();
        Long bi = b == null ? null : b.getId();
        return ai == null ? bi == null : ai.equals(bi);
    }

    /**
     * Loads the commitment bill totals of these admissions' final bills in a
     * few grouped queries, replacing what was loaded before.
     */
    public void load(List<PatientEncounter> encounters) {
        ccFiguresByFinalBillId.clear();
        ccCompanyRawByFinalBillId.clear();
        ccCompanyMergedByFinalBillId.clear();
        patientPaidByEncounterId.clear();
        failedFinalBillIds.clear();
        add(encounters);
    }

    /** Same as {@link #load(List)} but keeps what is already loaded (searches that gather admissions in several steps). */
    public void add(List<PatientEncounter> encounters) {
        if (encounters == null || encounters.isEmpty()) {
            return;
        }
        List<Long> encounterIds = new ArrayList<>();
        Set<Long> seenEncounters = new HashSet<>();
        for (PatientEncounter pe : encounters) {
            if (pe != null && pe.getId() != null && !patientPaidByEncounterId.containsKey(pe.getId())
                    && seenEncounters.add(pe.getId())) {
                encounterIds.add(pe.getId());
            }
        }
        for (int from = 0; from < encounterIds.size(); from += 500) {
            putPatientPaid(encounterIds.subList(from, Math.min(from + 500, encounterIds.size())));
        }
        List<Long> finalBillIds = new ArrayList<>();
        Set<Long> seenFinalBills = new HashSet<>();
        for (PatientEncounter pe : encounters) {
            if (pe != null && pe.getFinalBill() != null && pe.getFinalBill().getId() != null
                    && !ccFiguresByFinalBillId.containsKey(pe.getFinalBill().getId())
                    && seenFinalBills.add(pe.getFinalBill().getId())) {
                finalBillIds.add(pe.getFinalBill().getId());
            }
        }
        for (int from = 0; from < finalBillIds.size(); from += 500) {
            List<Long> chunk = finalBillIds.subList(from, Math.min(from + 500, finalBillIds.size()));
            for (Long id : chunk) {
                ccFiguresByFinalBillId.put(id, new double[0]);
            }
            if (!putCcFigures(chunk)) {
                failedFinalBillIds.addAll(chunk);
            }
        }
    }

    /**
     * Same filter as InwardBeanController.getPaidByPatientValue (used at
     * finalization): non-retired InwardPaymentBill bills, leaving out the
     * Credit method, which old flows used for company money. Cancellation and
     * refund bills carry a negative net total, so the sum is net of them.
     */
    private void putPatientPaid(List<Long> encounterIds) {
        for (Long id : encounterIds) {
            patientPaidByEncounterId.put(id, 0.0);
        }
        String jpql = "SELECT b.patientEncounter.id, SUM(b.netTotal) FROM Bill b"
                + " WHERE b.retired = false"
                + " AND b.billType = :btp"
                + " AND b.paymentMethod <> :pm"
                + " AND b.patientEncounter.id IN :ids"
                + " GROUP BY b.patientEncounter.id";
        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardPaymentBill);
        params.put("pm", PaymentMethod.Credit);
        params.put("ids", new ArrayList<>(encounterIds));
        List<Object[]> rows = billFacade.findAggregates(jpql, params);
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            patientPaidByEncounterId.put(id, row[1] == null ? 0.0 : ((Number) row[1]).doubleValue());
        }
    }

    /** @return false when a query failed (findAggregates returns null then), so the caller must not treat the bills as having no commitments */
    private boolean putCcFigures(List<Long> finalBillIds) {
        String jpql = "SELECT c.referenceBill.id, SUM(c.netTotal), SUM(c.settledAmountBySponsor) FROM Bill c"
                + " WHERE c.billTypeAtomic = :bta"
                + " AND c.referenceBill.id IN :ids"
                + " AND c.cancelled = false"
                + " AND c.retired = false"
                + " GROUP BY c.referenceBill.id";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        params.put("ids", new ArrayList<>(finalBillIds));
        List<Object[]> rows = billFacade.findAggregates(jpql, params);
        if (rows == null) {
            return false;
        }
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            double committed = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            double paid = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();
            ccFiguresByFinalBillId.put(id, new double[]{committed, paid});
        }
        return putCcCompanyFigures(finalBillIds);
    }

    /**
     * Per-company split of the same commitment bills (same filters as the
     * admission totals above): one grouped query per chunk, grouped by final
     * bill and commitment bill company. A bill with no company is kept under a
     * null company id so the split always adds up to the admission total.
     */
    private boolean putCcCompanyFigures(List<Long> finalBillIds) {
        String jpql = "SELECT c.referenceBill.id, cc.id, SUM(c.netTotal), SUM(c.settledAmountBySponsor) FROM Bill c"
                + " LEFT JOIN c.creditCompany cc"
                + " WHERE c.billTypeAtomic = :bta"
                + " AND c.referenceBill.id IN :ids"
                + " AND c.cancelled = false"
                + " AND c.retired = false"
                + " GROUP BY c.referenceBill.id, cc.id";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        params.put("ids", new ArrayList<>(finalBillIds));
        List<Object[]> rows = billFacade.findAggregates(jpql, params);
        if (rows == null) {
            return false;
        }
        for (Object[] row : rows) {
            Long finalBillId = (Long) row[0];
            Long companyId = row[1] == null ? null : ((Number) row[1]).longValue();
            double committed = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();
            double paid = row[3] == null ? 0.0 : ((Number) row[3]).doubleValue();
            ccCompanyRawByFinalBillId.computeIfAbsent(finalBillId, k -> new LinkedHashMap<>())
                    .put(companyId, new double[]{committed, paid});
        }
        return true;
    }

    private Institution institutionById(Long id) {
        Institution i = institutionsById.get(id);
        if (i == null) {
            i = institutionFacade.find(id);
            if (i != null) {
                institutionsById.put(id, i);
            }
        }
        return i;
    }

    /**
     * The admission's commitment figures split by commitment company. A bill
     * with no company falls back to the admission's own company; if that is
     * missing too it stays under a null company. An admission with no
     * commitment bills (older admissions) gets one row from the legacy
     * figures, so the rows always add up to getCcCommitted / getCcPaid.
     */
    public List<CompanyFigures> companyFigures(PatientEncounter pe) {
        if (pe == null || pe.getFinalBill() == null || pe.getFinalBill().getId() == null) {
            return Collections.emptyList();
        }
        double[] admissionFigures = ccFigures(pe); // loads the admission and its split when not yet loaded
        Long fbId = pe.getFinalBill().getId();
        if (failedFinalBillIds.contains(fbId)) {
            // The lookup failed: neither legacy figures nor a cached answer; it is retried on the next load().
            return Collections.emptyList();
        }
        List<CompanyFigures> merged = ccCompanyMergedByFinalBillId.get(fbId);
        if (merged != null) {
            return merged;
        }
        merged = new ArrayList<>();
        Map<Long, double[]> raw = ccCompanyRawByFinalBillId.get(fbId);
        if (admissionFigures == null || raw == null || raw.isEmpty()) {
            // Legacy figures only for a Credit admission with a company; a cash admission
            // with a stale company and no commitment bills has no company share (#25601).
            if (usesLegacyCompanyFigures(pe)) {
                double used = pe.getCreditUsedAmount();
                double settled = pe.getFinalBill().getSettledAmountBySponsor();
                merged.add(new CompanyFigures(pe.getCreditCompany(), used, settled));
            }
        } else {
            Map<Long, CompanyFigures> byCompany = new LinkedHashMap<>();
            for (Map.Entry<Long, double[]> e : raw.entrySet()) {
                Institution company = e.getKey() == null ? pe.getCreditCompany() : institutionById(e.getKey());
                Long key = company == null ? null : company.getId();
                CompanyFigures f = byCompany.get(key);
                if (f == null) {
                    byCompany.put(key, new CompanyFigures(company, e.getValue()[0], e.getValue()[1]));
                } else {
                    f.committed += e.getValue()[0];
                    f.paid += e.getValue()[1];
                }
            }
            merged.addAll(byCompany.values());
        }
        ccCompanyMergedByFinalBillId.put(fbId, merged);
        return merged;
    }

    /** Commitment bill totals of the admission's final bill, or null when it has none (older admissions). */
    private double[] ccFigures(PatientEncounter pe) {
        if (pe == null || pe.getFinalBill() == null || pe.getFinalBill().getId() == null) {
            return null;
        }
        Long id = pe.getFinalBill().getId();
        if (!ccFiguresByFinalBillId.containsKey(id)) {
            ccFiguresByFinalBillId.put(id, new double[0]);
            if (!putCcFigures(Collections.singletonList(id))) {
                failedFinalBillIds.add(id);
            }
        }
        double[] figures = ccFiguresByFinalBillId.get(id);
        return figures.length == 0 ? null : figures;
    }

    /**
     * An admission with no commitment bills falls back to its own credit figures
     * only when it is a Credit admission with a credit company; otherwise it has
     * no company share.
     */
    private static boolean usesLegacyCompanyFigures(PatientEncounter pe) {
        return pe != null && pe.getPaymentMethod() == PaymentMethod.Credit && pe.getCreditCompany() != null;
    }

    /** Amount the credit companies took on for this admission. */
    public double committed(PatientEncounter pe) {
        double[] f = ccFigures(pe);
        if (f != null) {
            return f[0];
        }
        return pe != null && usesLegacyCompanyFigures(pe) ? pe.getCreditUsedAmount() : 0.0;
    }

    /** Amount the credit companies have paid for this admission (receipts less cancels). */
    public double companyPaid(PatientEncounter pe) {
        double[] f = ccFigures(pe);
        if (f != null) {
            return f[1];
        }
        return pe == null || pe.getFinalBill() == null ? 0.0 : pe.getFinalBill().getSettledAmountBySponsor();
    }

    /** Patient money paid for this admission (payments less cancels and refunds), never below 0. */
    public double patientPaid(PatientEncounter pe) {
        if (pe == null || pe.getId() == null) {
            return 0.0;
        }
        if (!patientPaidByEncounterId.containsKey(pe.getId())) {
            putPatientPaid(Collections.singletonList(pe.getId()));
        }
        return Math.max(0.0, patientPaidByEncounterId.get(pe.getId()));
    }

    /**
     * Commitment bills of these final bills that have a non-cancelled company
     * payment (receipt) bill paid by the given method, as "finalBillId:companyId" keys (companyId "none" when the bill
     * has no company). Used by the report payment-method filter.
     */
    public Set<String> commitmentKeysPaidBy(List<PatientEncounter> encounters, PaymentMethod method) {
        Set<String> keys = new HashSet<>();
        if (method == null || encounters == null) {
            return keys;
        }
        List<Long> finalBillIds = new ArrayList<>();
        for (PatientEncounter pe : encounters) {
            if (pe != null && pe.getFinalBill() != null && pe.getFinalBill().getId() != null
                    && !finalBillIds.contains(pe.getFinalBill().getId())) {
                finalBillIds.add(pe.getFinalBill().getId());
            }
        }
        for (int from = 0; from < finalBillIds.size(); from += 500) {
            List<Long> chunk = finalBillIds.subList(from, Math.min(from + 500, finalBillIds.size()));
            String jpql = "SELECT DISTINCT c.referenceBill.id, cc.id FROM BillItem bi"
                    + " JOIN bi.referenceBill c"
                    + " LEFT JOIN c.creditCompany cc"
                    + " WHERE c.billTypeAtomic = :bta"
                    + " AND c.referenceBill.id IN :ids"
                    + " AND c.cancelled = false"
                    + " AND c.retired = false"
                    + " AND bi.retired = false"
                    + " AND bi.bill.billTypeAtomic IN :receiptTypes"
                    + " AND bi.bill.cancelled = false"
                    + " AND bi.bill.retired = false"
                    + " AND bi.bill.paymentMethod = :pm";
            Map<String, Object> params = new HashMap<>();
            params.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
            params.put("ids", new ArrayList<>(chunk));
            params.put("receiptTypes", companyReceiptTypes());
            params.put("pm", method);
            List<Object[]> rows = billFacade.findAggregates(jpql, params);
            if (rows == null) {
                continue;
            }
            for (Object[] row : rows) {
                keys.add(commitmentKey((Long) row[0], row[1] == null ? null : ((Number) row[1]).longValue()));
            }
        }
        return keys;
    }

    /**
     * The receipt bill types the company paid figure is built from (the
     * CREDIT_SETTLE_BY_COMPANY types, see CreditBean.getSettledAmountByCompany),
     * leaving out the cancellation and refund types.
     */
    private static List<BillTypeAtomic> companyReceiptTypes() {
        List<BillTypeAtomic> types = new ArrayList<>();
        for (BillTypeAtomic t : BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_COMPANY)) {
            if (t.getBillCategory() == BillCategory.BILL) {
                types.add(t);
            }
        }
        return types;
    }

    private static String commitmentKey(Long finalBillId, Long companyId) {
        return finalBillId + ":" + (companyId == null ? "none" : companyId);
    }

    private static double cents(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Rows for the inpatient debtor balance and opd-and-inward due reports:
     * one row per commitment company of each admission, from the commitment
     * and payment bills (the same figures as the Credit Company Due pages).
     * The patient's paid, due and excess are carried on the admission's first
     * listed row only, so an admission with two companies does not count the
     * patient twice; which rows are listed does not depend on company order. Excess
     * is negative (balance sign), as these reports have always shown it.
     *
     * @param dueType "due", "excess", "settled" or anything else for all rows
     * @param filterBy "patient", "company", "any" or "all"
     * @param companyFilter only this company's rows when not null
     * @param receiptMethod only companies with a receipt paid by this method when not null
     */
    public List<InstitutionBillEncounter> buildReportRows(List<PatientEncounter> encounters, String dueType,
            String filterBy, Institution companyFilter, PaymentMethod receiptMethod) {
        List<InstitutionBillEncounter> rows = new ArrayList<>();
        if (encounters == null || encounters.isEmpty()) {
            return rows;
        }
        load(encounters);
        Set<String> methodKeys = receiptMethod == null ? null : commitmentKeysPaidBy(encounters, receiptMethod);
        for (PatientEncounter pe : encounters) {
            if (pe == null || pe.getFinalBill() == null) {
                continue;
            }
            double netTotal = pe.getFinalBill().getNetTotal();
            double patientPaid = patientPaid(pe);
            double totalCommitted = committed(pe);
            double totalCompanyPaid = companyPaid(pe);
            // Round to cents before the sign tests: the final bill net can carry
            // more decimals than the bills, which would list a 0.00 balance row.
            double patientBalance = cents(netTotal - totalCommitted - patientPaid);
            List<InstitutionBillEncounter> admissionRows = new ArrayList<>();
            for (CompanyFigures f : companyFigures(pe)) {
                if (f.company == null) {
                    continue;
                }
                if (companyFilter != null && !isSameCompany(f.company, companyFilter)) {
                    continue;
                }
                if (methodKeys != null && !paidByMethod(methodKeys, pe, f.company)) {
                    continue;
                }
                double companyBalance = cents(f.committed - f.paid);
                InstitutionBillEncounter row = new InstitutionBillEncounter();
                row.setInstitution(f.company);
                row.setBhtNo(pe.getBhtNo());
                row.setDateOfDischarge(pe.getDateOfDischarge());
                row.setPatient(pe.getPatient());
                row.setNetTotal(netTotal);
                row.setGopAmount(f.committed);
                row.setPaidByPatient(0.0);
                row.setPatientDue(0.0);
                row.setPatientExcess(0.0);
                row.setPaidByCompany(f.paid);
                row.setCompanyDue(companyBalance > 0 ? companyBalance : 0.0);
                row.setCompanyExcess(companyBalance < 0 ? companyBalance : 0.0);
                row.setPatientEncounter(pe);
                row.setTotalPaidByCompanies(totalCompanyPaid);
                row.setTotalDue(cents(netTotal - (patientPaid + totalCompanyPaid)));
                row.setPatientGopAmount(netTotal - totalCommitted);
                // Whether a row is listed depends on its own company balance and the
                // admission's patient balance only, never on the order of the companies.
                if (matches(row, patientBalance, dueType, filterBy)) {
                    admissionRows.add(row);
                }
            }
            // The patient's figures are shown once per admission, on its first listed row.
            if (!admissionRows.isEmpty()) {
                InstitutionBillEncounter first = admissionRows.get(0);
                first.setPaidByPatient(patientPaid);
                first.setPatientDue(patientBalance > 0 ? patientBalance : 0.0);
                first.setPatientExcess(patientBalance < 0 ? patientBalance : 0.0);
            }
            rows.addAll(admissionRows);
        }
        return rows;
    }

    /**
     * For a report filtered to one credit company: Paid by Companies shows that
     * company's own paid figure instead of the admission's total paid by all
     * companies (#26071). Total due is not touched.
     */
    public static void showSelectedCompanyPaid(List<InstitutionBillEncounter> rows) {
        if (rows == null) {
            return;
        }
        for (InstitutionBillEncounter r : rows) {
            r.setTotalPaidByCompanies(r.getPaidByCompany());
        }
    }

    private static boolean paidByMethod(Set<String> methodKeys, PatientEncounter pe, Institution company) {
        Long fbId = pe.getFinalBill().getId();
        if (methodKeys.contains(commitmentKey(fbId, company.getId()))) {
            return true;
        }
        // A commitment bill with no company counts for the admission's own company.
        return methodKeys.contains(commitmentKey(fbId, null)) && isSameCompany(company, pe.getCreditCompany());
    }

    /** Same due / excess / settled and patient / company / any / all tests the old row builder applied. */
    private static boolean matches(InstitutionBillEncounter r, double admissionPatientBalance, String dueType, String filterBy) {
        String type = dueType == null ? "any" : dueType.toLowerCase();
        String by = filterBy == null ? "any" : filterBy.toLowerCase();
        boolean patientDue = admissionPatientBalance > 0;
        boolean companyDue = r.getCompanyDue() > 0;
        boolean patientExcess = admissionPatientBalance < 0;
        boolean companyExcess = r.getCompanyExcess() < 0;
        boolean patientSettled = admissionPatientBalance == 0;
        boolean companySettled = r.getCompanyDue() == 0 && r.getCompanyExcess() == 0;
        switch (type) {
            case "due":
                return test(by, patientDue, companyDue);
            case "excess":
                return test(by, patientExcess, companyExcess);
            case "settled":
                return test(by, patientSettled, companySettled);
            default:
                return true;
        }
    }

    private static boolean test(String filterBy, boolean patient, boolean company) {
        switch (filterBy) {
            case "patient":
                return patient;
            case "company":
                return company;
            case "all":
                return patient && company;
            default:
                return patient || company;
        }
    }
}
