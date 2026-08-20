package com.divudi.service.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.CalculationMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientRoomFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Shared bulk-query helper for per-admission (BHT) charge/discount aggregation.
 * Used by InwardInvoiceJournalController and InwardProfessionalFeeSummaryController
 * so this JPQL — and its bug-fix history (see "bug #5"/"bug #6" references in the
 * method comments below) — lives in exactly one place. Extracted 2026-08-15 from
 * InwardInvoiceJournalController (issue #19321) without changing any query logic.
 *
 * All methods (except fetchEncounters) take an already-fetched
 * List&lt;PatientEncounter&gt; and return bulk-fetched Maps keyed by
 * PatientEncounter.id — no N+1 per-encounter queries.
 */
@Stateless
public class InwardBhtChargeAggregationService implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;

    /** Filter parameters for fetchEncounters(), mirroring the report filter panels. */
    public static class EncounterFilter {
        private Date fromDate;
        private Date toDate;
        /** "admissionDate" or "dischargeDate" */
        private String dateBasis = "dischargeDate";
        private AdmissionStatus admissionStatus;
        private AdmissionType admissionType;
        private PaymentMethod paymentMethod;
        private Institution institution;
        private Institution site;
        private Department department;

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
    }

    // -------------------------------------------------------------------------
    // Encounter search (moved from InwardInvoiceJournalController.fetchEncounters())
    // -------------------------------------------------------------------------
    public List<PatientEncounter> fetchEncounters(EncounterFilter filter) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select distinct c from PatientEncounter c where c.retired = false");

        boolean forceAdmissionDate = filter.getAdmissionStatus() == AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;

        if (filter.getFromDate() != null && filter.getToDate() != null) {
            if ("admissionDate".equals(filter.getDateBasis()) || forceAdmissionDate) {
                jpql.append(" and c.dateOfAdmission between :fromDate and :toDate");
            } else {
                jpql.append(" and c.dateOfDischarge between :fromDate and :toDate");
            }
            params.put("fromDate", filter.getFromDate());
            params.put("toDate", filter.getToDate());
        }

        if (filter.getAdmissionStatus() != null && filter.getAdmissionStatus() != AdmissionStatus.ANY_STATUS) {
            switch (filter.getAdmissionStatus()) {
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

        if (filter.getAdmissionType() != null) {
            jpql.append(" and c.admissionType = :admType");
            params.put("admType", filter.getAdmissionType());
        }

        if (filter.getInstitution() != null) {
            jpql.append(" and c.institution = :ins");
            params.put("ins", filter.getInstitution());
        }

        if (filter.getSite() != null) {
            jpql.append(" and c.department.site = :site");
            params.put("site", filter.getSite());
        }

        if (filter.getDepartment() != null) {
            jpql.append(" and c.department = :dept");
            params.put("dept", filter.getDepartment());
        }

        if (filter.getPaymentMethod() != null) {
            jpql.append(" and c.paymentMethod = :pm");
            params.put("pm", filter.getPaymentMethod());
        }

        jpql.append(" order by c.bhtNo");
        return patientEncounterFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    // -------------------------------------------------------------------------
    // Charge totals per InwardChargeType (moved from InwardInvoiceJournalController)
    // -------------------------------------------------------------------------
    public Map<Long, Map<InwardChargeType, Double>> fetchChargesByEncounter(
            List<PatientEncounter> encounters) {

        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();

        mergeChargeMaps(result, fetchBillItemCharges(encounters));
        mergeChargeMaps(result, fetchAdmissionFeeCharges(encounters));
        mergeChargeMaps(result, fetchPatientRoomCalculatedCharges(encounters));
        mergeChargeMaps(result, fetchPatientRoomServiceItemCharges(encounters));
        mergeChargeMaps(result, fetchProfessionalFeeCharges(encounters));
        mergeChargeMaps(result, fetchAssistingFeeCharges(encounters));
        mergeChargeMaps(result, fetchPharmacyBillCharges(encounters));
        mergeChargeMaps(result, fetchStoreBillCharges(encounters));

        return result;
    }

    private List<InwardChargeType> chargeTypesByCalculationMethod(CalculationMethod method) {
        List<InwardChargeType> types = new ArrayList<>();
        for (InwardChargeType ct : InwardChargeType.values()) {
            if (ct.getCalculationMethod() == method) {
                types.add(ct);
            }
        }
        return types;
    }

    private void mergeChargeMaps(Map<Long, Map<InwardChargeType, Double>> target,
            Map<Long, Map<InwardChargeType, Double>> source) {
        for (Map.Entry<Long, Map<InwardChargeType, Double>> e : source.entrySet()) {
            Map<InwardChargeType, Double> inner = target.computeIfAbsent(
                    e.getKey(), k -> new EnumMap<>(InwardChargeType.class));
            for (Map.Entry<InwardChargeType, Double> ie : e.getValue().entrySet()) {
                inner.merge(ie.getKey(), ie.getValue(), Double::sum);
            }
        }
    }

    private Map<Long, Map<InwardChargeType, Double>> collectChargeTypeRows(List<Object[]> rows) {
        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Object[] r : rows) {
            Long              encId = (Long) r[0];
            InwardChargeType  type  = (InwardChargeType) r[1];
            double            total = r[2] == null ? 0.0 : ((Number) r[2]).doubleValue();
            result.computeIfAbsent(encId, k -> new EnumMap<>(InwardChargeType.class))
                  .merge(type, total, Double::sum);
        }
        return result;
    }

    /**
     * BILL_ITEM (default) charge types: BillItem on InwardBill/InwardOutSideBill
     * only — a whitelist, which excludes both INWARD_FINAL_BILL and
     * INWARD_ORIGINAL_FINAL_BILL discharge-snapshot bills (bug #6 fix).
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchBillItemCharges(List<PatientEncounter> encounters) {
        List<InwardChargeType> types = chargeTypesByCalculationMethod(CalculationMethod.BILL_ITEM);
        if (types.isEmpty()) {
            return new HashMap<>();
        }

        String jpql = "select enc.id, bi.inwardChargeType, sum(bi.grossValue)"
                + " from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType in :btps"
                + " and enc in :encs"
                + " and bi.inwardChargeType in :types"
                + " group by enc.id, bi.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("btps", Arrays.asList(BillType.InwardBill, BillType.InwardOutSideBill));
        params.put("encs", encounters);
        params.put("types", types);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        return collectChargeTypeRows(rows);
    }

    /**
     * ADMISSION_FEE: a flat AdmissionType.admissionFee value, never persisted as
     * a BillItem — read straight off the already-loaded encounter list.
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchAdmissionFeeCharges(List<PatientEncounter> encounters) {
        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();
        for (PatientEncounter enc : encounters) {
            if (enc.getAdmissionType() == null || enc.getId() == null) {
                continue;
            }
            double fee = enc.getAdmissionType().getAdmissionFee();
            if (fee == 0.0) {
                continue;
            }
            result.computeIfAbsent(enc.getId(), k -> new EnumMap<>(InwardChargeType.class))
                  .merge(InwardChargeType.AdmissionFee, fee, Double::sum);
        }
        return result;
    }

    /** PATIENT_ROOM — time-based half: PatientRoom.getCalculatedXxxCharge() per encounter. */
    private Map<Long, Map<InwardChargeType, Double>> fetchPatientRoomCalculatedCharges(List<PatientEncounter> encounters) {
        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();

        String jpql = "select pr from PatientRoom pr join pr.patientEncounter enc"
                + " where pr.retired = false"
                + " and enc in :encs";
        Map<String, Object> params = new HashMap<>();
        params.put("encs", encounters);

        List<PatientRoom> rooms = patientRoomFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rooms == null) {
            return result;
        }
        for (PatientRoom pr : rooms) {
            PatientEncounter enc = pr.getPatientEncounter();
            if (enc == null || enc.getId() == null) {
                continue;
            }
            Map<InwardChargeType, Double> inner = result.computeIfAbsent(
                    enc.getId(), k -> new EnumMap<>(InwardChargeType.class));
            mergeIfNonZero(inner, InwardChargeType.RoomCharges, pr.getCalculatedRoomCharge());
            mergeIfNonZero(inner, InwardChargeType.MOCharges, pr.getCalculatedMoCharge());
            mergeIfNonZero(inner, InwardChargeType.NursingCharges, pr.getCalculatedNursingCharge());
            mergeIfNonZero(inner, InwardChargeType.LinenCharges, pr.getCalculatedLinenCharge());
            mergeIfNonZero(inner, InwardChargeType.AdministrationCharge, pr.getCalculatedAdministrationCharge());
            mergeIfNonZero(inner, InwardChargeType.MedicalCareICU, pr.getCalculatedMedicalCareCharge());
            mergeIfNonZero(inner, InwardChargeType.MaintainCharges, pr.getCalculatedMaintainCharge());
        }
        return result;
    }

    private void mergeIfNonZero(Map<InwardChargeType, Double> map, InwardChargeType type, double value) {
        if (value != 0.0) {
            map.merge(type, value, Double::sum);
        }
    }

    /** PATIENT_ROOM — service-item half: BillItems filed directly under a PATIENT_ROOM charge type on InwardBill. */
    private Map<Long, Map<InwardChargeType, Double>> fetchPatientRoomServiceItemCharges(List<PatientEncounter> encounters) {
        List<InwardChargeType> types = chargeTypesByCalculationMethod(CalculationMethod.PATIENT_ROOM);
        if (types.isEmpty()) {
            return new HashMap<>();
        }

        String jpql = "select enc.id, bi.inwardChargeType, sum(bi.grossValue)"
                + " from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType = :btp"
                + " and enc in :encs"
                + " and bi.inwardChargeType in :types"
                + " group by enc.id, bi.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardBill);
        params.put("encs", encounters);
        params.put("types", types);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        return collectChargeTypeRows(rows);
    }

    /** BILL_FEE — Consultant staff -> ProfessionalCharge. */
    private Map<Long, Map<InwardChargeType, Double>> fetchProfessionalFeeCharges(List<PatientEncounter> encounters) {
        return fetchBillFeeCharges(encounters, true, InwardChargeType.ProfessionalCharge);
    }

    /** BILL_FEE — non-Consultant staff (assistants/nurses) -> DoctorAndNurses. */
    private Map<Long, Map<InwardChargeType, Double>> fetchAssistingFeeCharges(List<PatientEncounter> encounters) {
        return fetchBillFeeCharges(encounters, false, InwardChargeType.DoctorAndNurses);
    }

    private Map<Long, Map<InwardChargeType, Double>> fetchBillFeeCharges(
            List<PatientEncounter> encounters, boolean consultantOnly, InwardChargeType targetType) {

        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();

        String jpql = "select bf.bill.patientEncounter.id, sum(coalesce(bf.feeGrossValue, bf.feeValue))"
                + " from BillFee bf"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.cancelled = false"
                + " and bf.bill.billType = :btp"
                + " and bf.fee.feeType = :ftp"
                + (consultantOnly ? " and type(bf.staff) = :staffClass" : " and type(bf.staff) != :staffClass")
                + " and bf.bill.patientEncounter in :encs"
                + " group by bf.bill.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardProfessional);
        params.put("ftp", FeeType.Staff);
        params.put("staffClass", Consultant.class);
        params.put("encs", encounters);

        List<Object[]> rows = billFeeFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                Long   encId = (Long) r[0];
                double total = r[1] == null ? 0.0 : ((Number) r[1]).doubleValue();
                result.computeIfAbsent(encId, k -> new EnumMap<>(InwardChargeType.class))
                      .merge(targetType, total, Double::sum);
            }
        }
        return result;
    }

    /** PHARMACY_BILL (Medicine): same BillTypeAtomic whitelist used elsewhere in the app for BHT medicine issues. */
    private Map<Long, Map<InwardChargeType, Double>> fetchPharmacyBillCharges(List<PatientEncounter> encounters) {
        List<InwardChargeType> types = chargeTypesByCalculationMethod(CalculationMethod.PHARMACY_BILL);
        if (types.isEmpty()) {
            return new HashMap<>();
        }

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);

        String jpql = "select enc.id, bi.inwardChargeType, sum(bi.grossValue)"
                + " from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billTypeAtomic in :btas"
                + " and enc in :encs"
                + " and bi.inwardChargeType in :types"
                + " group by enc.id, bi.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("btas", btas);
        params.put("encs", encounters);
        params.put("types", types);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        return collectChargeTypeRows(rows);
    }

    /** STORE_BILL (GeneralIssuing): same BillType.StoreBhtPre filter used elsewhere in the app. */
    private Map<Long, Map<InwardChargeType, Double>> fetchStoreBillCharges(List<PatientEncounter> encounters) {
        List<InwardChargeType> types = chargeTypesByCalculationMethod(CalculationMethod.STORE_BILL);
        if (types.isEmpty()) {
            return new HashMap<>();
        }

        String jpql = "select enc.id, bi.inwardChargeType, sum(bi.grossValue)"
                + " from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.cancelled = false"
                + " and b.billType = :btp"
                + " and enc in :encs"
                + " and bi.inwardChargeType in :types"
                + " group by enc.id, bi.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.StoreBhtPre);
        params.put("encs", encounters);
        params.put("types", types);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        return collectChargeTypeRows(rows);
    }

    // -------------------------------------------------------------------------
    // Discount / service-charge totals (moved from InwardInvoiceJournalController)
    // -------------------------------------------------------------------------

    /**
     * Returns Map&lt;encounterId, double[]{discount, serviceCharge}&gt; — combined
     * total across all bill types, used by the Invoice Journal report. Excludes
     * both discharge-snapshot bill types so post-discharge totals aren't doubled
     * (bug #6).
     */
    public Map<Long, double[]> fetchDiscountAndMarginByEncounter(List<PatientEncounter> encounters) {
        Map<Long, double[]> result = new HashMap<>();

        String jpql = "select bf.bill.patientEncounter.id, sum(bf.feeDiscount), sum(bf.feeMargin)"
                + " from BillFee bf"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.cancelled = false"
                + " and (bf.bill.billTypeAtomic is null or bf.bill.billTypeAtomic not in :excludedTypes)"
                + " and bf.bill.patientEncounter in :encs"
                + " group by bf.bill.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("encs", encounters);
        params.put("excludedTypes", Arrays.asList(
                BillTypeAtomic.INWARD_FINAL_BILL, BillTypeAtomic.INWARD_ORIGINAL_FINAL_BILL));

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                Long encId = (Long) r[0];
                double disc = r[1] == null ? 0.0 : ((Number) r[1]).doubleValue();
                double marg = r[2] == null ? 0.0 : ((Number) r[2]).doubleValue();
                result.put(encId, new double[]{disc, marg});
            }
        }
        return result;
    }

    /**
     * Same filter as fetchDiscountAndMarginByEncounter(), but split by the SAME
     * criterion the gross charge split uses (fetchProfessionalFeeCharges /
     * fetchBillFeeCharges(consultantOnly=true)): a BillFee counts as "professional"
     * only if its bill is InwardProfessional AND its fee is Staff-type AND its
     * staff is a Consultant — i.e. exactly the ProfessionalCharge criterion, not
     * "any BillFee on an InwardProfessional bill" (which would also catch
     * DoctorAndNurses/assisting-staff fees and misroute their discount). Used by
     * the Professional & Other Fee Summary report (2026-08-15) to net each bucket
     * independently, consistently with how the gross totals are bucketed.
     *
     * otherJpql uses explicit LEFT JOINs on bf.fee/bf.staff, not the implicit
     * inner-join path navigation professionalJpql uses (BillFee.fee and
     * BillFee.staff are both nullable @ManyToOne). Writing bf.fee.feeType or
     * type(bf.staff) anywhere in a query — even inside NOT(...) — forces an
     * implicit inner join, which structurally drops any row with a null fee/staff
     * from the result set before the WHERE predicate is evaluated. That's correct
     * for professionalJpql (a null-staff row genuinely isn't professional), but
     * would have been wrong here: it silently dropped those rows from BOTH
     * buckets instead of counting them as "other" (CodeRabbit review, PR #22964).
     * Returns Map&lt;encounterId, double[]{professionalDiscount, professionalServiceCharge,
     * otherDiscount, otherServiceCharge}&gt;.
     */
    public Map<Long, double[]> fetchDiscountAndMarginSplitByProfessional(List<PatientEncounter> encounters) {
        Map<Long, double[]> result = new HashMap<>();

        String professionalJpql = "select bf.bill.patientEncounter.id, sum(bf.feeDiscount), sum(bf.feeMargin)"
                + " from BillFee bf"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.cancelled = false"
                + " and (bf.bill.billTypeAtomic is null or bf.bill.billTypeAtomic not in :excludedTypes)"
                + " and bf.bill.billType = :btp"
                + " and bf.fee.feeType = :ftp"
                + " and type(bf.staff) = :staffClass"
                + " and bf.bill.patientEncounter in :encs"
                + " group by bf.bill.patientEncounter.id";

        String otherJpql = "select bf.bill.patientEncounter.id, sum(bf.feeDiscount), sum(bf.feeMargin)"
                + " from BillFee bf left join bf.fee f left join bf.staff s"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.cancelled = false"
                + " and (bf.bill.billTypeAtomic is null or bf.bill.billTypeAtomic not in :excludedTypes)"
                + " and (bf.bill.billType != :btp or f.id is null or f.feeType != :ftp or s.id is null or type(s) != :staffClass)"
                + " and bf.bill.patientEncounter in :encs"
                + " group by bf.bill.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("encs", encounters);
        params.put("excludedTypes", Arrays.asList(
                BillTypeAtomic.INWARD_FINAL_BILL, BillTypeAtomic.INWARD_ORIGINAL_FINAL_BILL));
        params.put("btp", BillType.InwardProfessional);
        params.put("ftp", FeeType.Staff);
        params.put("staffClass", Consultant.class);

        List<Object[]> professionalRows = patientEncounterFacade.findObjectArrayByJpql(professionalJpql, params, TemporalType.TIMESTAMP);
        if (professionalRows != null) {
            for (Object[] r : professionalRows) {
                Long encId = (Long) r[0];
                double disc = r[1] == null ? 0.0 : ((Number) r[1]).doubleValue();
                double marg = r[2] == null ? 0.0 : ((Number) r[2]).doubleValue();
                double[] bucket = result.computeIfAbsent(encId, k -> new double[4]);
                bucket[0] += disc;
                bucket[1] += marg;
            }
        }

        List<Object[]> otherRows = patientEncounterFacade.findObjectArrayByJpql(otherJpql, params, TemporalType.TIMESTAMP);
        if (otherRows != null) {
            for (Object[] r : otherRows) {
                Long encId = (Long) r[0];
                double disc = r[1] == null ? 0.0 : ((Number) r[1]).doubleValue();
                double marg = r[2] == null ? 0.0 : ((Number) r[2]).doubleValue();
                double[] bucket = result.computeIfAbsent(encId, k -> new double[4]);
                bucket[2] += disc;
                bucket[3] += marg;
            }
        }

        return result;
    }
}
