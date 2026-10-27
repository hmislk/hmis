package com.divudi.service.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.GuardianRoom;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.PatientRoomTimedItemCharge;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.facade.EncounterCreditCompanyFacade;
import com.divudi.core.facade.PatientRoomTimedItemChargeFacade;
import com.divudi.core.facade.PriceMatrixFacade;
import com.divudi.core.util.CommonFunctions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
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
import javax.inject.Inject;

/**
 * Room charge calculation for an admission's PatientRooms, shared by the
 * interim bill (BhtSummeryController) and the inward charge aggregation
 * service behind the Invoice Journal and debtor reports (issue #24717), so the
 * two cannot drift apart.
 * <p>
 * Each room's seven charge components are priced up to its discharge time
 * (or now while it is still occupied), the room-category price-matrix margin
 * is folded in, and the room-category discount is applied. The results are
 * written onto the given PatientRoom and PatientRoomTimedItemCharge objects
 * only: nothing is persisted here. The interim bill saves them afterwards;
 * the reports work on detached copies and never save.
 * <p>
 * SUPPORTS: called without a transaction (JSF beans, the read-only
 * aggregation service), every facade read commits on its own, so the objects
 * changed here are detached and no change reaches the database by itself.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class InwardRoomChargeCalculationService {

    /** When true, theatre stays (TheatreRoom) are billed under Operation Theatre Charges (#24723). */
    public static final String THEATRE_TIME_AS_OT_KEY = "Inward Bill - Bill Theatre Room Time Under Operation Theatre Charges";

    @EJB
    private TimedItemPricingService timedItemPricingService;
    @EJB
    private EncounterCreditCompanyFacade encounterCreditCompanyFacade;
    @EJB
    private PatientRoomTimedItemChargeFacade patientRoomTimedItemChargeFacade;
    @EJB
    private PriceMatrixFacade priceMatrixFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private SessionController sessionController;

    /**
     * Recalculates charges, margins and discounts of the given rooms, all
     * belonging to one billing (mother) admission, and the time-based charges
     * linked to each room.
     *
     * @param rooms            the admission's rooms, including those of its child encounters
     * @param billingEncounter the mother admission the bill is raised on; its
     *                         payment method, scheme, admission type, credit
     *                         company and current room drive the calculation
     * @return the rooms' PatientRoomTimedItemCharges with calculatedCharge updated
     */
    public List<PatientRoomTimedItemCharge> recalculate(List<PatientRoom> rooms, PatientEncounter billingEncounter) {
        if (rooms == null || rooms.isEmpty() || billingEncounter == null) {
            return new ArrayList<>();
        }
        Map<PatientEncounter, List<PatientRoom>> one = new LinkedHashMap<>();
        one.put(billingEncounter, rooms);
        return recalculateAll(one);
    }

    /**
     * {@link #recalculate} for many admissions at once, as the reports need:
     * timed charges and credit companies are read in one query each, and the
     * discount and price-matrix lookups are shared across admissions.
     *
     * @param roomsByBillingEncounter each mother admission with its rooms
     *                                (its own and its child encounters')
     */
    public List<PatientRoomTimedItemCharge> recalculateAll(Map<PatientEncounter, List<PatientRoom>> roomsByBillingEncounter) {
        List<PatientRoomTimedItemCharge> timedCharges = new ArrayList<>();
        if (roomsByBillingEncounter == null || roomsByBillingEncounter.isEmpty()) {
            return timedCharges;
        }
        Context ctx = new Context();
        Map<Long, Institution> creditCompanies = resolveSingleCreditCompanies(roomsByBillingEncounter.keySet());
        Map<Long, List<PatientRoomTimedItemCharge>> timedByRoom = fetchTimedItemCharges(roomsByBillingEncounter.values());

        for (Map.Entry<PatientEncounter, List<PatientRoom>> e : roomsByBillingEncounter.entrySet()) {
            PatientEncounter billingEncounter = e.getKey();
            if (billingEncounter == null || e.getValue() == null) {
                continue;
            }
            PaymentMethod pm = billingEncounter.getPaymentMethod();
            PaymentScheme scheme = billingEncounter.getPaymentScheme();
            AdmissionType admType = billingEncounter.getAdmissionType();
            Institution creditCompany = creditCompanies.get(billingEncounter.getId());

            for (PatientRoom p : e.getValue()) {
                if (p.getAdmittedAt() == null) {
                    p.setAdmittedAt(new Date());
                }
                calculateComponents(p, billingEncounter, creditCompany, ctx);
                timedCharges.addAll(calculateTimedItemCharges(p, timedByRoom.get(p.getId())));

                // Discount percentages depend on the room's category (issue #24011).
                RoomCategory rc = p.getRoomFacilityCharge() != null ? p.getRoomFacilityCharge().getRoomCategory() : null;
                double[] pct = ctx.discountPercents.computeIfAbsent(
                        Arrays.asList(pm, idOf(scheme), idOf(admType), idOf(creditCompany), idOf(rc)),
                        k -> roomChargeDiscountPercents(pm, scheme, admType, creditCompany, rc));
                applyRoomChargeDiscounts(p, pct[0], pct[1], pct[2], pct[3], pct[4], pct[5], pct[6]);
            }
        }
        return timedCharges;
    }

    /**
     * Recalculates only the calculated and margin fields of each room. Never
     * touches discount or adjusted fields, so a discount the user has just
     * edited on the interim bill survives.
     */
    public void recalculateMargins(List<PatientRoom> rooms, PatientEncounter billingEncounter) {
        if (rooms == null || rooms.isEmpty() || billingEncounter == null) {
            return;
        }
        Context ctx = new Context();
        Institution creditCompany = resolveSingleCreditCompany(billingEncounter);
        for (PatientRoom p : rooms) {
            calculateComponents(p, billingEncounter, creditCompany, ctx);
        }
    }

    /** Lookups shared across the rooms of one calculation pass. */
    private static class Context {

        private final Map<List<Object>, double[]> discountPercents = new HashMap<>();
        /** Departments that have any room-charge price-matrix row; loaded on first use. */
        private Set<Long> roomMatrixDepartmentIds;
    }

    private static Long idOf(Object entity) {
        if (entity instanceof Institution) {
            return ((Institution) entity).getId();
        }
        if (entity instanceof PaymentScheme) {
            return ((PaymentScheme) entity).getId();
        }
        if (entity instanceof AdmissionType) {
            return ((AdmissionType) entity).getId();
        }
        if (entity instanceof RoomCategory) {
            return ((RoomCategory) entity).getId();
        }
        return null;
    }

    private Map<Long, Institution> resolveSingleCreditCompanies(Collection<PatientEncounter> encounters) {
        Map<Long, Institution> result = new HashMap<>();
        List<PatientEncounter> withId = new ArrayList<>();
        for (PatientEncounter pe : encounters) {
            if (pe != null && pe.getId() != null) {
                withId.add(pe);
            }
        }
        if (withId.isEmpty()) {
            return result;
        }
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("encs", withId);
        List<EncounterCreditCompany> list = encounterCreditCompanyFacade.findByJpql(
                "select e from EncounterCreditCompany e where e.retired = false and e.patientEncounter in :encs", hm);
        Map<Long, Integer> counts = new HashMap<>();
        if (list != null) {
            for (EncounterCreditCompany ecc : list) {
                Long encId = ecc.getPatientEncounter().getId();
                counts.merge(encId, 1, Integer::sum);
                result.put(encId, ecc.getInstitution());
            }
        }
        // Only an admission with exactly one credit company has one to use.
        counts.forEach((encId, n) -> {
            if (n != 1) {
                result.remove(encId);
            }
        });
        return result;
    }

    private Map<Long, List<PatientRoomTimedItemCharge>> fetchTimedItemCharges(Collection<List<PatientRoom>> roomLists) {
        Map<Long, List<PatientRoomTimedItemCharge>> result = new HashMap<>();
        List<PatientRoom> rooms = new ArrayList<>();
        for (List<PatientRoom> l : roomLists) {
            if (l == null) {
                continue;
            }
            for (PatientRoom p : l) {
                if (p != null && p.getId() != null) {
                    rooms.add(p);
                }
            }
        }
        if (rooms.isEmpty()) {
            return result;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("prs", rooms);
        List<PatientRoomTimedItemCharge> charges = patientRoomTimedItemChargeFacade.findByJpql(
                "SELECT t FROM PatientRoomTimedItemCharge t WHERE t.patientRoom in :prs", params);
        if (charges != null) {
            for (PatientRoomTimedItemCharge tc : charges) {
                result.computeIfAbsent(tc.getPatientRoom().getId(), k -> new ArrayList<>()).add(tc);
            }
        }
        return result;
    }

    public boolean isTheatreTimeBilledAsOt() {
        return configOptionApplicationController.getBooleanValueByKey(THEATRE_TIME_AS_OT_KEY, false);
    }

    /**
     * The credit company of the admission when it has exactly one, else null.
     */
    public Institution resolveSingleCreditCompany(PatientEncounter encounter) {
        if (encounter == null) {
            return null;
        }
        String jpql = "select e from EncounterCreditCompany e where e.retired = false and e.patientEncounter = :enc";
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("enc", encounter);
        List<EncounterCreditCompany> list = encounterCreditCompanyFacade.findByJpql(jpql, hm, 2);
        if (list != null && list.size() == 1) {
            return list.get(0).getInstitution();
        }
        return null;
    }

    private void calculateComponents(PatientRoom p, PatientEncounter billingEncounter, Institution creditCompany, Context ctx) {
        calculateRoomCharge(p, billingEncounter, creditCompany, ctx);
        calculateMaintananceCharge(p, billingEncounter, creditCompany, ctx);
        calculateLinenCharge(p, billingEncounter, creditCompany, ctx);
        if (!(p instanceof GuardianRoom)) {
            calculateNursingCharge(p, billingEncounter, creditCompany, ctx);
            calculateMoCharge(p, billingEncounter, creditCompany, ctx);
            calculateAdministrationCharge(p, billingEncounter, creditCompany, ctx);
            calculateMedicalCareCharge(p, billingEncounter, creditCompany, ctx);
        }
    }

    /**
     * Discount % for each room charge type, in the order applyRoomChargeDiscounts
     * takes them: room, maintenance, linen, nursing, MO, administration,
     * medical care. A row for the given room category wins over an all-rooms row.
     */
    private double[] roomChargeDiscountPercents(PaymentMethod pm, PaymentScheme scheme, AdmissionType admType,
            Institution creditCompany, RoomCategory rc) {
        InwardChargeType[] types = {InwardChargeType.RoomCharges, InwardChargeType.MaintainCharges,
            InwardChargeType.LinenCharges, InwardChargeType.NursingCharges, InwardChargeType.MOCharges,
            InwardChargeType.AdministrationCharge, InwardChargeType.MedicalCareICU};
        double[] r = new double[types.length];
        for (int i = 0; i < types.length; i++) {
            r[i] = priceMatrixController.getInwardDiscountPercentForChargeType(pm, scheme, admType, types[i], creditCompany, rc);
        }
        return r;
    }

    private void applyRoomChargeDiscounts(PatientRoom p,
            double roomPct, double maintainPct, double linenPct, double nursingPct,
            double moPct, double adminPct, double medicalCarePct) {
        double roomDisc = (roomPct / 100.0) * p.getCalculatedRoomCharge();
        double maintainDisc = (maintainPct / 100.0) * p.getCalculatedMaintainCharge();
        double linenDisc = (linenPct / 100.0) * p.getCalculatedLinenCharge();
        double nursingDisc = (nursingPct / 100.0) * p.getCalculatedNursingCharge();
        double moDisc = (moPct / 100.0) * p.getCalculatedMoCharge();
        double adminDisc = (adminPct / 100.0) * p.getCalculatedAdministrationCharge();
        double medicalCareDisc = (medicalCarePct / 100.0) * p.getCalculatedMedicalCareCharge();

        p.setDiscountRoomCharge(roomDisc);
        p.setDiscountMaintainCharge(maintainDisc);
        p.setDiscountLinenCharge(linenDisc);
        p.setDiscountNursingCharge(nursingDisc);
        p.setDiscountMoCharge(moDisc);
        p.setDiscountAdministrationCharge(adminDisc);
        p.setDiscountMedicalCareCharge(medicalCareDisc);

        p.setAdjustedRoomCharge(p.getCalculatedRoomCharge() - roomDisc);
        p.setAdjustedMaintainCharge(p.getCalculatedMaintainCharge() - maintainDisc);
        p.setAjdustedLinenCharge(p.getCalculatedLinenCharge() - linenDisc);
        p.setAjdustedNursingCharge(p.getCalculatedNursingCharge() - nursingDisc);
        p.setAdjustedMoCharge(p.getCalculatedMoCharge() - moDisc);
        p.setAjdustedAdministrationCharge(p.getCalculatedAdministrationCharge() - adminDisc);
        p.setAjdustedMedicalCareCharge(p.getCalculatedMedicalCareCharge() - medicalCareDisc);
    }

    /**
     * Margin (positive or negative) from the room-category price-adjustment
     * matrix (InwardPriceAdjustment rows configured on the room facility
     * category price-matrix admin page) for a single room-related charge amount
     * of a given PatientRoom. Returns 0 when the room has no
     * department/room-category context or no matching matrix row exists.
     */
    private double roomChargeMatrixMargin(PatientRoom p, double chargeValue, PatientEncounter encounter, Institution creditCompany, Context ctx) {
        if (p.getRoomFacilityCharge() == null || chargeValue == 0.0) {
            return 0.0;
        }
        Department department = p.getRoomFacilityCharge().getDepartment();
        if (department == null) {
            return 0.0;
        }
        // A department with no room-charge price-matrix row can never match
        // one; skipping its lookups keeps a report over many admissions fast.
        if (ctx.roomMatrixDepartmentIds == null) {
            ctx.roomMatrixDepartmentIds = new HashSet<>(priceMatrixFacade.findLongList(
                    "select distinct a.department.id from InwardPriceAdjustment a"
                    + " where a.retired = false and a.category is null and a.department is not null",
                    new HashMap<>()));
        }
        if (!ctx.roomMatrixDepartmentIds.contains(department.getId())) {
            return 0.0;
        }
        RoomCategory roomCategory = p.getRoomFacilityCharge().getRoomCategory();
        PaymentMethod paymentMethod = encounter == null ? null : encounter.getPaymentMethod();
        AdmissionType admissionType = encounter == null ? null : encounter.getAdmissionType();
        PriceMatrix priceMatrix = priceMatrixController.fetchRoomChargeMargin(department, chargeValue, paymentMethod, creditCompany, admissionType, roomCategory);
        if (priceMatrix == null) {
            return 0.0;
        }
        return (chargeValue * priceMatrix.getMargin()) / 100.0;
    }

    private boolean excluded(String component) {
        return configOptionApplicationController.getBooleanValueByKey("Inward Room Price Matrix - Exclude " + component + " Charge", false);
    }

    private void calculateLinenCharge(PatientRoom p, PatientEncounter encounter, Institution creditCompany, Context ctx) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentLinenCharge() == 0.0) {
            p.setCalculatedLinenCharge(0);
            p.setMarginLinenCharge(0.0);
            return;
        }

        double linen = p.getCurrentLinenCharge();
        Date dischargedAt = p.getDischargedAt();
        if (dischargedAt == null) {
            dischargedAt = new Date();
        }

        double extra = p.getAddedLinenCharge();
        double calculated;
        if (CommonFunctions.checkToDateAreInSameDay(p.getAdmittedAt(), dischargedAt)) {
            if (p.getAdmittedAt().equals(dischargedAt)) {
                calculated = 0 + extra;
            } else {
                calculated = linen + extra;
            }
        } else {
            calculated = (linen * CommonFunctions.getDayCount(p.getAdmittedAt(), dischargedAt)) + extra;
        }

        double linenMargin = excluded("Linen") ? 0.0 : roomChargeMatrixMargin(p, calculated, encounter, creditCompany, ctx);
        p.setMarginLinenCharge(linenMargin);
        p.setCalculatedLinenCharge(calculated + linenMargin);
    }

    private void calculateMoCharge(PatientRoom p, PatientEncounter encounter, Institution creditCompany, Context ctx) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentMoCharge() == 0.0) {
            p.setCalculatedMoCharge(0);
            p.setMarginMoCharge(0.0);
            return;
        }

        double calculated;
        if (!sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            double mo = p.getCurrentMoCharge();
            calculated = getCharge(p, mo, encounter) + p.getAddedMoCharge();
        } else {
            Date dischargedAt = p.getDischargedAt();
            long dCount = CommonFunctions.getDayCount(p.getAdmittedAt(), dischargedAt);

            if (dCount <= p.getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge()) {
                calculated = p.getCurrentMoCharge() + p.getAddedMoCharge();
            } else {
                long extra = dCount - p.getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge();
                calculated = (p.getCurrentMoChargeForAfterDuration() * extra) + p.getCurrentMoCharge();
            }
        }

        double moMargin = excluded("MO") ? 0.0 : roomChargeMatrixMargin(p, calculated, encounter, creditCompany, ctx);
        p.setMarginMoCharge(moMargin);
        p.setCalculatedMoCharge(calculated + moMargin);
    }

    private void calculateAdministrationCharge(PatientRoom p, PatientEncounter encounter, Institution creditCompany, Context ctx) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentAdministrationCharge() == 0.0) {
            p.setCalculatedAdministrationCharge(0);
            p.setMarginAdministrationCharge(0.0);
            return;
        }

        double adm = p.getCurrentAdministrationCharge();
        double calculated = getCharge(p, adm, encounter) + p.getAddedAdministrationCharge();
        double adminMargin = excluded("Administration") ? 0.0 : roomChargeMatrixMargin(p, calculated, encounter, creditCompany, ctx);
        p.setMarginAdministrationCharge(adminMargin);
        p.setCalculatedAdministrationCharge(calculated + adminMargin);
    }

    private void calculateMedicalCareCharge(PatientRoom p, PatientEncounter encounter, Institution creditCompany, Context ctx) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentMedicalCareCharge() == 0.0) {
            p.setCalculatedMedicalCareCharge(0);
            p.setMarginMedicalCareCharge(0.0);
            return;
        }

        double med = p.getCurrentMedicalCareCharge();
        double calculated = getCharge(p, med, encounter) + p.getAddedMedicalCareCharge();
        double medicalCareMargin = excluded("Medical Care") ? 0.0 : roomChargeMatrixMargin(p, calculated, encounter, creditCompany, ctx);
        p.setMarginMedicalCareCharge(medicalCareMargin);
        p.setCalculatedMedicalCareCharge(calculated + medicalCareMargin);
    }

    private void calculateNursingCharge(PatientRoom p, PatientEncounter encounter, Institution creditCompany, Context ctx) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentNursingCharge() == 0) {
            p.setCalculatedNursingCharge(0);
            p.setMarginNursingCharge(0.0);
            return;
        }

        double nursing = p.getCurrentNursingCharge();
        double calculated = getCharge(p, nursing, encounter) + p.getAddedNursingCharge();
        double nursingMargin = excluded("Nursing") ? 0.0 : roomChargeMatrixMargin(p, calculated, encounter, creditCompany, ctx);
        p.setMarginNursingCharge(nursingMargin);
        p.setCalculatedNursingCharge(calculated + nursingMargin);
    }

    private void calculateRoomCharge(PatientRoom p, PatientEncounter encounter, Institution creditCompany, Context ctx) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentRoomCharge() == 0) {
            p.setCalculatedRoomCharge(0);
            p.setMarginRoomCharge(0.0);
            return;
        }

        double roomCharge = p.getCurrentRoomCharge();
        double calculated = getCharge(p, roomCharge, encounter) + p.getAddedRoomCharge();

        double roomMargin = roomChargeMatrixMargin(p, calculated, encounter, creditCompany, ctx);
        p.setMarginRoomCharge(roomMargin);

        p.setCalculatedRoomCharge(calculated + roomMargin);
    }

    private double getCharge(PatientRoom patientRoom, double value, PatientEncounter encounter) {
        TimedItemFee timedFee = patientRoom.getRoomFacilityCharge().getTimedItemFee();
        Date dischargeAt = patientRoom.getDischargedAt();

        if (dischargeAt == null) {
            dischargeAt = new Date();
        }

        if (encounter.getCurrentPatientRoom() == null) {
            return 0;
        }

        return value * timedItemPricingService.calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt);
    }

    private void calculateMaintananceCharge(PatientRoom p, PatientEncounter encounter, Institution creditCompany, Context ctx) {
        if (p.getRoomFacilityCharge() == null || p.getCurrentMaintananceCharge() == 0) {
            p.setCalculatedMaintainCharge(0);
            p.setMarginMaintainCharge(0.0);
            return;
        }
        double maintanance = p.getCurrentMaintananceCharge();
        double calculated = getCharge(p, maintanance, encounter) + p.getAddedMaintainCharge();
        double maintainMargin = excluded("Maintenance") ? 0.0 : roomChargeMatrixMargin(p, calculated, encounter, creditCompany, ctx);
        p.setMarginMaintainCharge(maintainMargin);
        p.setCalculatedMaintainCharge(calculated + maintainMargin);
    }

    private List<PatientRoomTimedItemCharge> calculateTimedItemCharges(PatientRoom p, List<PatientRoomTimedItemCharge> charges) {
        List<PatientRoomTimedItemCharge> result = new ArrayList<>();
        if (charges == null || charges.isEmpty()) {
            return result;
        }
        Date to = p.getDischargedAt() != null ? p.getDischargedAt() : new Date();
        boolean foreigner = p.getPatientEncounter() != null && p.getPatientEncounter().isForiegner();
        for (PatientRoomTimedItemCharge tc : charges) {
            if (tc.getTimedItem() == null) {
                continue;
            }
            double total = timedItemPricingService.calTotalTimedChargeForItem(tc.getTimedItem(), p.getAdmittedAt(), to, foreigner);
            tc.setCalculatedCharge(total);
            result.add(tc);
        }
        return result;
    }
}
