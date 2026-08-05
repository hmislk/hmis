package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.InwardInvoiceJournalRowDto;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.CalculationMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.EncounterCreditCompanyFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.PaymentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private EncounterCreditCompanyFacade encounterCreditCompanyFacade;
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
    private PaymentMethod   paymentMethod;
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

    private double grandTotalGross;
    private double grandTotalDiscount;
    private double grandTotalServiceCharge;
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
        grandTotalGross          = 0;
        grandTotalDiscount       = 0;
        grandTotalServiceCharge  = 0;
        grandTotalCharges        = 0;
        grandTotalDeposits       = 0;
        grandTotalCreditSettlement = 0;

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        // --- bulk fetch gross charges per InwardChargeType for all encounters ---
        Map<Long, Map<InwardChargeType, Double>> chargeMap = fetchChargesByEncounter(encounters);

        // --- bulk fetch discount / service-charge totals per encounter ---
        Map<Long, double[]> discountMarginMap = fetchDiscountAndMarginByEncounter(encounters);

        // --- bulk fetch deposit totals for all encounters at once ---
        Map<Long, Double> depositMap = fetchDepositTotalsByEncounter(encounters);

        // --- bulk fetch credit settlement totals ---
        Map<Long, double[]> creditMap = fetchCreditSettlementByEncounter(encounters);

        // --- bulk fetch credit company names (an encounter may have more than one) ---
        Map<Long, List<String>> creditCompanyNamesMap = fetchCreditCompanyNamesByEncounter(encounters);

        // --- build one row per encounter ---
        for (PatientEncounter enc : encounters) {
            InwardInvoiceJournalRowDto row = buildRow(enc, chargeMap, discountMarginMap, depositMap, creditMap,
                    creditCompanyNamesMap);
            reportRows.add(row);

            // accumulate column totals and active types
            for (InwardChargeType ct : allChargeTypes) {
                double v = row.getChargeForType(ct);
                if (v != 0) {
                    activeChargeTypes.add(ct);
                    columnTotals.merge(ct, v, Double::sum);
                }
            }
            grandTotalGross            += row.getGrossTotal();
            grandTotalDiscount         += row.getTotalDiscount();
            grandTotalServiceCharge    += row.getTotalServiceCharge();
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
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf",  false);
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

        if (paymentMethod != null) {
            jpql.append(" and c.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }

        jpql.append(" order by c.bhtNo");
        return patientEncounterFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    /**
     * Dispatcher that combines per-CalculationMethod bulk (GROUP BY) queries into
     * the same Map< encounterId, Map<InwardChargeType, total> > shape the caller
     * expects. Each sub-fetch stays a single bulk query for the whole encounter
     * set (no N+1 per-encounter queries) — see class docblock.
     *
     * Per InwardChargeType.getCalculationMethod():
     *  - BILL_ITEM (default)  : BillItem on InwardBill/InwardOutSideBill (whitelist —
     *                            naturally excludes both INWARD_FINAL_BILL and
     *                            INWARD_ORIGINAL_FINAL_BILL snapshot bills, fixing
     *                            the post-discharge double count).
     *  - ADMISSION_FEE         : flat AdmissionType.admissionFee, read straight off
     *                            the already-loaded encounters (no BillItem exists).
     *  - PATIENT_ROOM           : PatientRoom.getCalculatedXxxCharge() (time-based)
     *                            plus any service BillItems filed under the same
     *                            charge type on InwardBill.
     *  - BILL_FEE               : BillFee.feeValue on InwardProfessional bills,
     *                            split Consultant (ProfessionalCharge) vs
     *                            non-Consultant (DoctorAndNurses).
     *  - PHARMACY_BILL/STORE_BILL: BillItem on the pharmacy-issue BillTypeAtomic
     *                            list / StoreBhtPre, same as InwardChargeTypeDetailController.
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchChargesByEncounter(
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

    /**
     * Returns the InwardChargeType values whose CalculationMethod matches, e.g.
     * the default BILL_ITEM set (everything without an explicit CalculationMethod)
     * or the 7 PATIENT_ROOM types.
     */
    private List<InwardChargeType> chargeTypesByCalculationMethod(CalculationMethod method) {
        List<InwardChargeType> types = new ArrayList<>();
        for (InwardChargeType ct : InwardChargeType.values()) {
            if (ct.getCalculationMethod() == method) {
                types.add(ct);
            }
        }
        return types;
    }

    /** Merges a fragment charge map into the accumulator, summing on collision. */
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

    /** Reads {encId, InwardChargeType, sum} triples from an Object[] result set into a charge map. */
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
     * only — a whitelist, which is the fix for bug #6 (this naturally excludes
     * both INWARD_FINAL_BILL and INWARD_ORIGINAL_FINAL_BILL discharge-snapshot
     * bills, unlike the old blacklist that only excluded the latter).
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

    /**
     * PATIENT_ROOM — time-based half: PatientRoom.getCalculatedXxxCharge() per
     * encounter, one bulk query then a Java-side pivot (see
     * InwardChargeTypeBreakdownController.buildFromPatientRoom()/extractPatientRoomCharge()).
     */
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

    /**
     * PATIENT_ROOM — service-item half: BillItems filed directly under a
     * PATIENT_ROOM charge type on InwardBill (e.g. an ad-hoc room-charge line),
     * summed alongside the calculated time-based charge for the same type.
     */
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

    /**
     * BILL_FEE — Consultant staff → ProfessionalCharge. Same filter as
     * InwardBeanController.createProfesionallFee().
     */
    private Map<Long, Map<InwardChargeType, Double>> fetchProfessionalFeeCharges(List<PatientEncounter> encounters) {
        return fetchBillFeeCharges(encounters, true, InwardChargeType.ProfessionalCharge);
    }

    /**
     * BILL_FEE — non-Consultant staff (assistants/nurses) → DoctorAndNurses.
     * Same filter as InwardBeanController.createDoctorAndNurseFee().
     */
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

    /**
     * PHARMACY_BILL (Medicine): same BillTypeAtomic whitelist as
     * InwardChargeTypeDetailController.fetchPharmacyBillItemRows(), aggregated.
     */
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

    /**
     * STORE_BILL (GeneralIssuing): same BillType.StoreBhtPre filter as
     * InwardChargeTypeDetailController.fetchStoreBillItemRows(), aggregated.
     */
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

    /**
     * Single bulk query: sum(feeDiscount) and sum(feeMargin) from BillFee
     * grouped by encounter, matching the same filter used for gross charges.
     * Returns Map< encounterId, double[]{ discount, serviceCharge } >.
     *
     * Reads from BillFee (not BillItem) because BillItem.discount is not
     * populated for inpatient bills — discount lives per-fee.
     *
     * Kept as a single query spanning all bill sources (not narrowed to the
     * InwardBill/InwardOutSideBill whitelist used for gross charges) since fees
     * can be filed against professional/pharmacy/store bills too. Excludes BOTH
     * discharge-snapshot bill types — INWARD_FINAL_BILL and
     * INWARD_ORIGINAL_FINAL_BILL — so post-discharge discount/service-charge
     * totals aren't doubled (bug #6).
     */
    private Map<Long, double[]> fetchDiscountAndMarginByEncounter(List<PatientEncounter> encounters) {
        Map<Long, double[]> result = new HashMap<>();

        String jpql = "select bf.bill.patientEncounter.id, sum(bf.feeDiscount), sum(bf.feeMargin)"
                + " from BillFee bf"
                + " where bf.retired = false"
                + " and bf.bill.retired = false"
                + " and bf.bill.cancelled = false"
                + " and bf.bill.billTypeAtomic not in :excludedTypes"
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
                double total = ((Number) r[1]).doubleValue();
                result.put(encId, total);
            }
        }
        return result;
    }

    /**
     * Single bulk query: credit settlement totals grouped by encounter.
     * Returns Map< encounterId, double[]{total} >.
     *
     * Groups/filters on bi.patientEncounter (the per-BillItem encounter link),
     * NOT bi.bill.patientEncounter — the credit settlement Bill created by
     * CashRecieveBillController.settleCreditForInwardCreditCompanyPaymentBills()
     * is never given a patientEncounter (it can span multiple BHTs); only each
     * BillItem carries its own patientEncounter. Filtering on bi.bill.patientEncounter
     * silently dropped every settlement row (bug #5).
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

    /**
     * Single bulk query: names of all non-retired EncounterCreditCompany rows
     * grouped by encounter. A BHT can have more than one active credit company
     * (the legacy PatientEncounter.creditCompany field only ever held one) —
     * see buildRow() for how this is merged with the legacy field.
     * Returns Map< encounterId, List<companyName> >.
     */
    private Map<Long, List<String>> fetchCreditCompanyNamesByEncounter(List<PatientEncounter> encounters) {
        Map<Long, List<String>> result = new HashMap<>();

        String jpql = "select ecc.patientEncounter.id, ecc.institution.name"
                + " from EncounterCreditCompany ecc"
                + " where ecc.retired = false"
                + " and ecc.patientEncounter in :encs";

        Map<String, Object> params = new HashMap<>();
        params.put("encs", encounters);

        List<Object[]> rows = encounterCreditCompanyFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                Long   encId = (Long) r[0];
                String name  = (String) r[1];
                if (name == null) {
                    continue;
                }
                result.computeIfAbsent(encId, k -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    private InwardInvoiceJournalRowDto buildRow(
            PatientEncounter enc,
            Map<Long, Map<InwardChargeType, Double>> chargeMap,
            Map<Long, double[]> discountMarginMap,
            Map<Long, Double> depositMap,
            Map<Long, double[]> creditMap,
            Map<Long, List<String>> creditCompanyNamesMap) {

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

        // charges (gross per InwardChargeType)
        Map<InwardChargeType, Double> charges = chargeMap.get(enc.getId());
        if (charges != null) {
            charges.forEach(row::addCharge);
        }

        // discount & service-charge totals
        double[] dm = discountMarginMap.get(enc.getId());
        if (dm != null) {
            row.setTotalDiscount(dm[0]);
            row.setTotalServiceCharge(dm[1]);
        }

        // deposits
        row.setTotalDeposits(depositMap.getOrDefault(enc.getId(), 0.0));

        // credit settlement
        double[] credit = creditMap.get(enc.getId());
        if (credit != null) {
            row.setCreditSettlementTotal(credit[0]);
        }
        // credit company name(s) — a BHT may have more than one active company
        // via EncounterCreditCompany; the legacy single-valued creditCompany
        // field is kept for backward compat and merged in first.
        Set<String> creditCompanyNames = new LinkedHashSet<>();
        if (enc.getCreditCompany() != null && enc.getCreditCompany().getName() != null) {
            creditCompanyNames.add(enc.getCreditCompany().getName());
        }
        List<String> extraNames = creditCompanyNamesMap.get(enc.getId());
        if (extraNames != null) {
            creditCompanyNames.addAll(extraNames);
        }
        if (!creditCompanyNames.isEmpty()) {
            row.setCreditCompanyName(String.join(", ", creditCompanyNames));
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
        paymentMethod    = null;
        institution      = null;
        site             = null;
        department       = null;
        reportRows       = null;
        columnTotals     = new EnumMap<>(InwardChargeType.class);
        activeChargeTypes = EnumSet.noneOf(InwardChargeType.class);
        grandTotalGross = grandTotalDiscount = grandTotalServiceCharge = 0;
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

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Institution getSite() { return site; }
    public void setSite(Institution site) { this.site = site; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public List<InwardInvoiceJournalRowDto> getReportRows() { return reportRows; }

    public List<InwardChargeType> getAllChargeTypes() { return allChargeTypes; }

    public Map<InwardChargeType, Double> getColumnTotals() { return columnTotals; }

    public double getGrandTotalGross() { return grandTotalGross; }
    public double getGrandTotalDiscount() { return grandTotalDiscount; }
    public double getGrandTotalServiceCharge() { return grandTotalServiceCharge; }
    public double getGrandTotalCharges() { return grandTotalCharges; }
    public double getGrandTotalDeposits() { return grandTotalDeposits; }
    public double getGrandTotalCreditSettlement() { return grandTotalCreditSettlement; }
}
