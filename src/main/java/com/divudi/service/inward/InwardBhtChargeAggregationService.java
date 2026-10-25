package com.divudi.service.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.inward.InwardMedicineBillTypes;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.PatientRoomTimedItemCharge;
import com.divudi.core.entity.inward.TheatreRoom;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.facade.PatientRoomFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
 *
 * Cancelled bills are deliberately NOT filtered out (no {@code cancelled = false}).
 * Cancelling a bill sets {@code cancelled=true} on the original and writes a
 * separate CancelledBill (same billType, negated values, {@code cancelled=false}).
 * Summing both nets the cancelled charge to zero, which is what the interim
 * bill (InwardBeanController) does. Filtering on {@code cancelled = false} kept
 * only the negative half and showed a cancelled charge as a deduction (#24548).
 * <p>
 * Read only. NOT_SUPPORTED keeps every entity it loads detached: a still-admitted
 * patient's rooms are recalculated in memory, and that must never be saved.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InwardBhtChargeAggregationService implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private PatientItemFacade patientItemFacade;
    @EJB
    private TimedItemPricingService timedItemPricingService;
    @EJB
    private InwardProfessionalFeeClassificationService professionalFeeClassificationService;
    @EJB
    private InwardRoomChargeCalculationService roomChargeCalculationService;

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
                "select distinct c from PatientEncounter c where c.retired = false"
                // Child encounters are reported inside their mother admission's row.
                + " and c.parentEncounter is null");

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
    // Charge totals per InwardChargeType
    // -------------------------------------------------------------------------

    /**
     * Gross charge per InwardChargeType for each given admission, built the
     * way the interim bill (BhtSummeryController) builds it: child encounters
     * (surgery procedure encounters, baby admissions) are folded into their
     * mother admission's figures, and a still-admitted patient's rooms are
     * priced up to now (issue #24717). Margins and discounts are returned
     * separately by {@link #fetchDiscountAndMarginByEncounter}.
     */
    public Map<Long, Map<InwardChargeType, Double>> fetchChargesByEncounter(
            List<PatientEncounter> encounters) {

        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();
        if (encounters == null || encounters.isEmpty()) {
            return result;
        }
        Family family = family(encounters);
        List<PatientEncounter> all = family.all;

        mergeChargeMaps(result, fetchInwardBillItemCharges(all));
        mergeChargeMaps(result, fetchOutsideBillItemCharges(all));
        mergeChargeMaps(result, fetchUnbilledTimedItemCharges(all));
        mergeChargeMaps(result, fetchRunningTimedItemTopUps(all));
        mergeChargeMaps(result, fetchAdmissionFeeCharges(all));
        mergeChargeMaps(result, fetchPatientRoomCharges(family));
        mergeChargeMaps(result, fetchProfessionalFeeCharges(all));
        mergeChargeMaps(result, fetchAssistingFeeCharges(all));
        mergeChargeMaps(result, fetchTechnicianFeeCharges(all));
        mergeChargeMaps(result, fetchIssueBillGross(all, InwardMedicineBillTypes.ALL, null, InwardChargeType.Medicine));
        mergeChargeMaps(result, fetchIssueBillGross(all, null, BillType.StoreBhtPre, InwardChargeType.GeneralIssuing));

        return family.foldCharges(result);
    }

    /**
     * Net interim-bill charge per encounter: gross (all InwardChargeTypes from
     * fetchChargesByEncounter) - discount + service charge (fetchDiscountAndMarginByEncounter).
     * Same figure as InwardInvoiceJournalRowDto.getGrandTotal().
     */
    public Map<Long, Double> fetchNetChargesByEncounter(List<PatientEncounter> encounters) {
        Map<Long, Double> result = new HashMap<>();
        if (encounters == null || encounters.isEmpty()) {
            return result;
        }

        Map<Long, Map<InwardChargeType, Double>> charges = fetchChargesByEncounter(encounters);
        Map<Long, double[]> discountMargin = fetchDiscountAndMarginByEncounter(encounters);

        for (Map.Entry<Long, Map<InwardChargeType, Double>> e : charges.entrySet()) {
            double gross = 0.0;
            if (e.getValue() != null) {
                for (Double v : e.getValue().values()) {
                    gross += v == null ? 0.0 : v;
                }
            }
            result.put(e.getKey(), gross);
        }
        for (Map.Entry<Long, double[]> e : discountMargin.entrySet()) {
            double[] dm = e.getValue();
            double discount = dm == null || dm.length < 1 ? 0.0 : dm[0];
            double serviceCharge = dm == null || dm.length < 2 ? 0.0 : dm[1];
            result.merge(e.getKey(), serviceCharge - discount, Double::sum);
        }
        return result;
    }

    /**
     * The given admissions plus their child encounters, and which mother
     * admission each child's charges belong to. The interim bill adds the
     * charges of direct children (InwardBeanController.fetchChildPatientEncounter)
     * to the mother's bill, so the reports fold them the same way.
     */
    private static final class Family {

        private final List<PatientEncounter> all = new ArrayList<>();
        private final Map<Long, Long> parentOf = new HashMap<>();
        private final Map<Long, PatientEncounter> byId = new HashMap<>();

        private Long billingIdOf(Long encounterId) {
            return parentOf.getOrDefault(encounterId, encounterId);
        }

        private Map<Long, Map<InwardChargeType, Double>> foldCharges(Map<Long, Map<InwardChargeType, Double>> source) {
            Map<Long, Map<InwardChargeType, Double>> folded = new HashMap<>();
            for (Map.Entry<Long, Map<InwardChargeType, Double>> e : source.entrySet()) {
                Map<InwardChargeType, Double> inner = folded.computeIfAbsent(
                        billingIdOf(e.getKey()), k -> new EnumMap<>(InwardChargeType.class));
                for (Map.Entry<InwardChargeType, Double> ie : e.getValue().entrySet()) {
                    inner.merge(ie.getKey(), ie.getValue(), Double::sum);
                }
            }
            return folded;
        }

        private Map<Long, double[]> foldArrays(Map<Long, double[]> source) {
            Map<Long, double[]> folded = new HashMap<>();
            for (Map.Entry<Long, double[]> e : source.entrySet()) {
                double[] v = e.getValue();
                double[] target = folded.computeIfAbsent(billingIdOf(e.getKey()), k -> new double[v.length]);
                for (int i = 0; i < v.length; i++) {
                    target[i] += v[i];
                }
            }
            return folded;
        }
    }

    private Family family(List<PatientEncounter> encounters) {
        Family family = new Family();
        for (PatientEncounter pe : encounters) {
            if (pe != null && pe.getId() != null && !family.byId.containsKey(pe.getId())) {
                family.all.add(pe);
                family.byId.put(pe.getId(), pe);
            }
        }
        if (family.all.isEmpty()) {
            return family;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("encs", new ArrayList<>(family.all));
        List<PatientEncounter> children = patientEncounterFacade.findByJpql(
                "select c from PatientEncounter c where c.retired = false and c.parentEncounter in :encs",
                params, TemporalType.TIMESTAMP);
        if (children != null) {
            for (PatientEncounter child : children) {
                if (child.getId() == null || child.getParentEncounter() == null) {
                    continue;
                }
                family.parentOf.put(child.getId(), child.getParentEncounter().getId());
                if (!family.byId.containsKey(child.getId())) {
                    family.all.add(child);
                    family.byId.put(child.getId(), child);
                }
            }
        }
        return family;
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
     * Service and investigation items on InwardBill, grouped by the item's own
     * charge type as the interim bill does
     * (InwardBeanController.calServiceBillItemsTotalByInwardChargeTypeBulk).
     * Many saved bill items have no charge type of their own, so grouping by
     * BillItem.inwardChargeType dropped them. Gross only: the margin and
     * discount come from fetchDiscountAndMarginByEncounter. The whitelist
     * leaves out the discharge-snapshot final bills (bug #6 fix).
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchInwardBillItemCharges(List<PatientEncounter> encounters) {
        String jpql = "select enc.id, i.inwardChargeType, sum(bi.grossValue)"
                + " from BillItem bi join bi.bill b join b.patientEncounter enc join bi.item i"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.billType = :btp"
                + " and enc in :encs"
                + " and i.inwardChargeType is not null"
                + " group by enc.id, i.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardBill);
        params.put("encs", encounters);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        return collectChargeTypeRows(rows);
    }

    /**
     * Outside Charge items, by the charge type chosen on the item, at their
     * net value, as InwardBeanController.caltValueFromAdditionalChargeBulk.
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchOutsideBillItemCharges(List<PatientEncounter> encounters) {
        String jpql = "select enc.id, bi.inwardChargeType, sum(bi.netValue)"
                + " from BillItem bi join bi.bill b join b.patientEncounter enc"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.billType = :btp"
                + " and enc in :encs"
                + " and bi.inwardChargeType is not null"
                + " group by enc.id, bi.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardOutSideBill);
        params.put("encs", encounters);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        return collectChargeTypeRows(rows);
    }

    /**
     * Timed services that have no BillItem yet (PatientItem.billItem is null).
     * New timed services get an InwardBill BillItem when they are added, which
     * fetchInwardBillItemCharges already counts; older ones may only exist as a
     * PatientItem. Mirrors InwardBeanController.calTimedPatientItemByInwardChargeType,
     * which the interim bill adds for the same rows (#19760).
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchUnbilledTimedItemCharges(List<PatientEncounter> encounters) {
        String jpql = "select enc.id, i.item.inwardChargeType, sum(i.serviceValue)"
                + " from PatientItem i join i.patientEncounter enc"
                + " where i.retired = false"
                + " and type(i.item) = :cls"
                + " and i.billItem is null"
                + " and i.item.inwardChargeType is not null"
                + " and enc in :encs"
                + " group by enc.id, i.item.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("cls", TimedItem.class);
        params.put("encs", encounters);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        return collectChargeTypeRows(rows);
    }

    /**
     * Live accrual of timed services that are still running (toTime is null),
     * billed or not. Their saved value (PatientItem.serviceValue, mirrored on the
     * BillItem) is priced once when the service is added and does not grow, so
     * each running service is re-priced as of now and only the positive
     * difference is added. Mirrors BhtSummeryController.addRunningTimedServiceLiveTopUp,
     * so the journal matches the interim bill while a service runs.
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchRunningTimedItemTopUps(List<PatientEncounter> encounters) {
        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();

        // Skip a service whose bill was cancelled or retired: its bill rows net to
        // zero, so it must not accrue. Left joins keep unbilled services.
        String jpql = "select i from PatientItem i"
                + " left join i.billItem bi"
                + " left join bi.bill b"
                + " where i.retired = false"
                + " and type(i.item) = :cls"
                + " and i.toTime is null"
                + " and i.item.inwardChargeType is not null"
                + " and i.patientEncounter in :encs"
                + " and (bi is null or (bi.retired = false and (b is null or (b.retired = false and b.cancelled = false))))";

        Map<String, Object> params = new HashMap<>();
        params.put("cls", TimedItem.class);
        params.put("encs", encounters);

        List<PatientItem> running = patientItemFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (running == null) {
            return result;
        }
        Date now = new Date();
        for (PatientItem pi : running) {
            PatientEncounter enc = pi.getPatientEncounter();
            if (enc == null || enc.getId() == null || !(pi.getItem() instanceof TimedItem)) {
                continue;
            }
            // A start time in the future has not accrued anything yet.
            if (pi.getFromTime() == null || now.before(pi.getFromTime())) {
                continue;
            }
            TimedItem item = (TimedItem) pi.getItem();
            // No configured fee would price at zero; keep the saved value.
            if (timedItemPricingService.getAllTimedItemFees(item).isEmpty()) {
                continue;
            }
            double liveValue = timedItemPricingService.calTotalTimedChargeForItem(
                    item, pi.getFromTime(), now, enc.isForiegner());
            double savedValue = pi.getServiceValue() != null ? pi.getServiceValue() : 0.0;
            double delta = liveValue - savedValue;
            if (delta <= 0.0) {
                continue;
            }
            result.computeIfAbsent(enc.getId(), k -> new EnumMap<>(InwardChargeType.class))
                  .merge(item.getInwardChargeType(), delta, Double::sum);
        }
        return result;
    }

    /**
     * ADMISSION_FEE: a flat AdmissionType.admissionFee value per encounter, not
     * persisted as a BillItem, as InwardBeanController.getAdmissionCharge. Services
     * filed under Admission Fee are added by fetchInwardBillItemCharges.
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

    /**
     * Room stays and their linked time-based charges, net of the room discount,
     * as InwardBeanController.getPatientRoomChargeSumsBulk adds them to the
     * interim bill.
     * <p>
     * The stored PatientRoom figures are only refreshed when someone opens the
     * interim bill, so for a patient still admitted they are recalculated up to
     * now here with the interim bill's own calculator, in memory only. A
     * discharged admission's rooms are closed, so the stored figures stand.
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchPatientRoomCharges(Family family) {
        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();

        Map<String, Object> params = new HashMap<>();
        params.put("encs", family.all);
        List<PatientRoom> rooms = patientRoomFacade.findByJpql(
                "select pr from PatientRoom pr where pr.retired = false and pr.patientEncounter in :encs order by pr.createdAt",
                params, TemporalType.TIMESTAMP);
        if (rooms == null || rooms.isEmpty()) {
            return result;
        }

        Map<PatientEncounter, List<PatientRoom>> liveRooms = new LinkedHashMap<>();
        Set<Long> storedEncounterIds = new HashSet<>();
        for (PatientRoom pr : rooms) {
            if (pr.getPatientEncounter() == null || pr.getPatientEncounter().getId() == null) {
                continue;
            }
            Long encId = pr.getPatientEncounter().getId();
            PatientEncounter billing = family.byId.get(family.billingIdOf(encId));
            if (billing != null && !billing.isDischarged()) {
                liveRooms.computeIfAbsent(billing, k -> new ArrayList<>()).add(pr);
            } else {
                storedEncounterIds.add(encId);
            }
        }
        List<PatientRoomTimedItemCharge> liveTimedCharges = roomChargeCalculationService.recalculateAll(liveRooms);

        boolean theatreAsOt = roomChargeCalculationService.isTheatreTimeBilledAsOt();
        for (PatientRoom pr : rooms) {
            if (pr.getPatientEncounter() == null || pr.getPatientEncounter().getId() == null) {
                continue;
            }
            Map<InwardChargeType, Double> inner = result.computeIfAbsent(
                    pr.getPatientEncounter().getId(), k -> new EnumMap<>(InwardChargeType.class));
            double room = pr.getCalculatedRoomCharge() - pr.getDiscountRoomCharge();
            double mo = pr.getCalculatedMoCharge() - pr.getDiscountMoCharge();
            double nursing = pr.getCalculatedNursingCharge() - pr.getDiscountNursingCharge();
            double maintain = pr.getCalculatedMaintainCharge() - pr.getDiscountMaintainCharge();
            double medicalCare = pr.getCalculatedMedicalCareCharge() - pr.getDiscountMedicalCareCharge();
            double administration = pr.getCalculatedAdministrationCharge() - pr.getDiscountAdministrationCharge();
            double linen = pr.getCalculatedLinenCharge() - pr.getDiscountLinenCharge();
            if (theatreAsOt && pr instanceof TheatreRoom) {
                // Theatre time billed under Operation Theatre Charges (#24723).
                mergeIfNonZero(inner, InwardChargeType.OperationTheatreCharges,
                        room + mo + nursing + maintain + medicalCare + administration + linen);
                continue;
            }
            mergeIfNonZero(inner, InwardChargeType.RoomCharges, room);
            mergeIfNonZero(inner, InwardChargeType.MOCharges, mo);
            mergeIfNonZero(inner, InwardChargeType.NursingCharges, nursing);
            mergeIfNonZero(inner, InwardChargeType.MaintainCharges, maintain);
            mergeIfNonZero(inner, InwardChargeType.MedicalCareICU, medicalCare);
            mergeIfNonZero(inner, InwardChargeType.AdministrationCharge, administration);
            mergeIfNonZero(inner, InwardChargeType.LinenCharges, linen);
        }

        for (PatientRoomTimedItemCharge tc : liveTimedCharges) {
            if (tc.getPatientRoom() == null || tc.getPatientRoom().getPatientEncounter() == null
                    || tc.getTimedItem() == null || tc.getTimedItem().getInwardChargeType() == null) {
                continue;
            }
            result.computeIfAbsent(tc.getPatientRoom().getPatientEncounter().getId(), k -> new EnumMap<>(InwardChargeType.class))
                  .merge(tc.getTimedItem().getInwardChargeType(), tc.getCalculatedCharge() - tc.getDiscountCharge(), Double::sum);
        }
        if (!storedEncounterIds.isEmpty()) {
            List<PatientEncounter> storedEncounters = new ArrayList<>();
            for (Long id : storedEncounterIds) {
                storedEncounters.add(family.byId.get(id));
            }
            Map<String, Object> tp = new HashMap<>();
            tp.put("encs", storedEncounters);
            List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(
                    "select pr.patientEncounter.id, t.timedItem.inwardChargeType, sum(t.calculatedCharge - t.discountCharge)"
                    + " from PatientRoomTimedItemCharge t join t.patientRoom pr"
                    + " where pr.retired = false"
                    + " and pr.patientEncounter in :encs"
                    + " group by pr.patientEncounter.id, t.timedItem.inwardChargeType",
                    tp, TemporalType.TIMESTAMP);
            mergeChargeMaps(result, collectChargeTypeRows(rows));
        }
        return result;
    }

    private void mergeIfNonZero(Map<InwardChargeType, Double> map, InwardChargeType type, double value) {
        if (value != 0.0) {
            map.merge(type, value, Double::sum);
        }
    }

    /** BILL_FEE — professional fees. Covers assisting fees too when the hospital merges them. */
    private Map<Long, Map<InwardChargeType, Double>> fetchProfessionalFeeCharges(List<PatientEncounter> encounters) {
        return fetchBillFeeCharges(encounters, InwardChargeType.ProfessionalCharge);
    }

    /** BILL_FEE — assisting fees (non-Consultant staff). Empty for a merged hospital. */
    private Map<Long, Map<InwardChargeType, Double>> fetchAssistingFeeCharges(List<PatientEncounter> encounters) {
        if (professionalFeeClassificationService.isSuppressed(InwardChargeType.DoctorAndNurses)) {
            return new HashMap<>();
        }
        return fetchBillFeeCharges(encounters, InwardChargeType.DoctorAndNurses);
    }

    /** BILL_FEE — technician/paramedical fees. Their own bucket whether or not the hospital merges (issue #23982). */
    private Map<Long, Map<InwardChargeType, Double>> fetchTechnicianFeeCharges(List<PatientEncounter> encounters) {
        return fetchBillFeeCharges(encounters, InwardChargeType.TechnicianAndParamedicalCharge);
    }

    private Map<Long, Map<InwardChargeType, Double>> fetchBillFeeCharges(
            List<PatientEncounter> encounters, InwardChargeType targetType) {

        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();

        Map<String, Object> params = new HashMap<>();

        String jpql = "select bf.bill.patientEncounter.id, sum(coalesce(bf.feeGrossValue, bf.feeValue))"
                + " from BillFee bf"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.billType = :btp"
                + " and bf.fee.feeType = :ftp"
                + professionalFeeClassificationService.staffCondition("bf", targetType, params)
                + " and bf.bill.patientEncounter in :encs"
                + " group by bf.bill.patientEncounter.id";

        params.put("btp", BillType.InwardProfessional);
        params.put("ftp", FeeType.Staff);
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

    /**
     * Medicine and store issues to the patient: gross ({@code Bill.total}) per
     * encounter under the given charge type. The interim bill takes these from
     * the bills, not the items ({@code calCostOfIssueByBill},
     * {@code calNetCostOfIssue}): a return item's value is stored positive, so
     * summing items counted returns as charges. The margin
     * ({@code netTotal - total}) is returned by fetchIssueBillMargins.
     *
     * @param btas     bill type atomics to include, or null to use billType
     * @param billType bill type to include when btas is null
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchIssueBillGross(List<PatientEncounter> encounters,
            List<BillTypeAtomic> btas, BillType billType, InwardChargeType chargeType) {
        Map<Long, Map<InwardChargeType, Double>> result = new HashMap<>();
        for (Map.Entry<Long, double[]> e : fetchIssueBillTotals(encounters, btas, billType).entrySet()) {
            mergeIfNonZero(result.computeIfAbsent(e.getKey(), k -> new EnumMap<>(InwardChargeType.class)),
                    chargeType, e.getValue()[0]);
        }
        return result;
    }

    /** Per encounter: {sum(Bill.total), sum(Bill.netTotal)} of the given issue bills. */
    private Map<Long, double[]> fetchIssueBillTotals(List<PatientEncounter> encounters,
            List<BillTypeAtomic> btas, BillType billType) {
        Map<Long, double[]> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        String jpql = "select b.patientEncounter.id, sum(b.total), sum(b.netTotal)"
                + " from Bill b"
                + " where b.retired = false"
                + " and b.patientEncounter in :encs";
        if (btas != null) {
            jpql += " and b.billTypeAtomic in :btas";
            params.put("btas", btas);
        } else {
            jpql += " and b.billType = :btp";
            params.put("btp", billType);
        }
        jpql += " group by b.patientEncounter.id";
        params.put("encs", encounters);

        List<Object[]> rows = patientEncounterFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                double total = r[1] == null ? 0.0 : ((Number) r[1]).doubleValue();
                double net = r[2] == null ? 0.0 : ((Number) r[2]).doubleValue();
                result.put((Long) r[0], new double[]{total, net});
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Discount / service-charge totals
    // -------------------------------------------------------------------------

    /**
     * Returns Map&lt;encounterId, double[]{discount, serviceCharge}&gt;, child
     * encounters folded into their mother admission. The sum of the
     * professional and other buckets of
     * {@link #fetchDiscountAndMarginSplitByProfessional}.
     */
    public Map<Long, double[]> fetchDiscountAndMarginByEncounter(List<PatientEncounter> encounters) {
        Map<Long, double[]> result = new HashMap<>();
        for (Map.Entry<Long, double[]> e : fetchDiscountAndMarginSplitByProfessional(encounters).entrySet()) {
            double[] v = e.getValue();
            result.put(e.getKey(), new double[]{v[0] + v[2], v[1] + v[3]});
        }
        return result;
    }

    /**
     * Discount and service charge (margin) per encounter, split into the
     * professional-fee bucket and everything else, from the same sources the
     * interim bill uses:
     * <ul>
     * <li>services on InwardBill: {@code BillItem.discount} and
     * {@code BillItem.marginValue}, for items with a charge type, matching
     * fetchInwardBillItemCharges (other);</li>
     * <li>medicine and store issues: {@code Bill.netTotal - Bill.total}
     * (other);</li>
     * <li>staff fees on InwardProfessional bills: {@code BillFee.feeDiscount}
     * and {@code BillFee.feeMargin}, split by the SAME criterion the gross
     * charge split uses (fetchProfessionalFeeCharges → fetchBillFeeCharges(ProfessionalCharge)):
     * a fee counts as "professional" only if its fee is Staff-type AND its
     * saved fee category is Consultant Fee (or Consultant/Assistant when
     * merged), i.e. exactly InwardProfessionalFeeClassificationService.staffCondition
     * for ProfessionalCharge. The staff subtype is not used: since #23874 the
     * category is persisted per fee, and a technician is often a Consultant
     * record (issue #23982).</li>
     * </ul>
     * The BillFee rows of InwardBill services carry the same margin as their
     * BillItems, so they are not read again here.
     * <p>
     * otherJpql uses explicit LEFT JOINs on bf.fee/bf.staff, not the implicit
     * inner-join path navigation professionalJpql uses (BillFee.fee and
     * BillFee.staff are both nullable @ManyToOne). Writing bf.fee.feeType or
     * type(bf.staff) anywhere in a query — even inside NOT(...) — forces an
     * implicit inner join, which structurally drops any row with a null fee/staff
     * from the result set before the WHERE predicate is evaluated (CodeRabbit
     * review, PR #22964). Its category test is notProfessionalChargeCategory(),
     * the exact complement of the professional side's, so the two buckets never
     * overlap or leak.
     * <p>
     * Child encounters are folded into their mother admission. Returns
     * Map&lt;encounterId, double[]{professionalDiscount, professionalServiceCharge,
     * otherDiscount, otherServiceCharge}&gt;.
     */
    public Map<Long, double[]> fetchDiscountAndMarginSplitByProfessional(List<PatientEncounter> encounters) {
        Map<Long, double[]> result = new HashMap<>();
        if (encounters == null || encounters.isEmpty()) {
            return result;
        }
        Family family = family(encounters);

        // The merge toggle is handled inside the classification service: when
        // merged, "professional" widens to every staffed non-technician fee and
        // "other" narrows by exactly the same rows, so the two buckets stay
        // complementary in both configurations.
        Map<String, Object> params = new HashMap<>();

        String professionalJpql = "select bf.bill.patientEncounter.id, sum(bf.feeDiscount), sum(bf.feeMargin)"
                + " from BillFee bf"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.billType = :btp"
                + " and bf.fee.feeType = :ftp"
                + professionalFeeClassificationService.staffCondition("bf", InwardChargeType.ProfessionalCharge, params)
                + " and bf.bill.patientEncounter in :encs"
                + " group by bf.bill.patientEncounter.id";

        String otherJpql = "select bf.bill.patientEncounter.id, sum(bf.feeDiscount), sum(bf.feeMargin)"
                + " from BillFee bf left join bf.fee f left join bf.staff s"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.billType = :btp"
                + " and (f.id is null or f.feeType != :ftp or s.id is null"
                + " or" + professionalFeeClassificationService.notProfessionalChargeCategory("bf") + ")"
                + " and bf.bill.patientEncounter in :encs"
                + " group by bf.bill.patientEncounter.id";

        params.put("encs", family.all);
        params.put("btp", BillType.InwardProfessional);
        params.put("ftp", FeeType.Staff);

        addDiscountMarginRows(result, patientEncounterFacade.findObjectArrayByJpql(professionalJpql, params, TemporalType.TIMESTAMP), 0);
        addDiscountMarginRows(result, patientEncounterFacade.findObjectArrayByJpql(otherJpql, params, TemporalType.TIMESTAMP), 2);

        String serviceJpql = "select enc.id, sum(bi.discount), sum(bi.marginValue)"
                + " from BillItem bi join bi.bill b join b.patientEncounter enc join bi.item i"
                + " where bi.retired = false"
                + " and b.retired = false"
                + " and b.billType = :btp"
                + " and enc in :encs"
                + " and i.inwardChargeType is not null"
                + " group by enc.id";
        Map<String, Object> serviceParams = new HashMap<>();
        serviceParams.put("btp", BillType.InwardBill);
        serviceParams.put("encs", family.all);
        addDiscountMarginRows(result, patientEncounterFacade.findObjectArrayByJpql(serviceJpql, serviceParams, TemporalType.TIMESTAMP), 2);

        addIssueMargins(result, fetchIssueBillTotals(family.all, InwardMedicineBillTypes.ALL, null));
        addIssueMargins(result, fetchIssueBillTotals(family.all, null, BillType.StoreBhtPre));

        return family.foldArrays(result);
    }

    private void addDiscountMarginRows(Map<Long, double[]> result, List<Object[]> rows, int offset) {
        if (rows == null) {
            return;
        }
        for (Object[] r : rows) {
            double disc = r[1] == null ? 0.0 : ((Number) r[1]).doubleValue();
            double marg = r[2] == null ? 0.0 : ((Number) r[2]).doubleValue();
            double[] bucket = result.computeIfAbsent((Long) r[0], k -> new double[4]);
            bucket[offset] += disc;
            bucket[offset + 1] += marg;
        }
    }

    private void addIssueMargins(Map<Long, double[]> result, Map<Long, double[]> issueTotals) {
        for (Map.Entry<Long, double[]> e : issueTotals.entrySet()) {
            double margin = e.getValue()[1] - e.getValue()[0];
            if (margin != 0.0) {
                result.computeIfAbsent(e.getKey(), k -> new double[4])[3] += margin;
            }
        }
    }
}
