package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.InpatientPharmacyItemMovementDTO;
import com.divudi.core.data.inward.InwardMedicineBillTypes;
import com.divudi.core.data.dto.InpatientPharmacyNetSummaryDTO;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Per-item pharmacy movement of one inpatient encounter (issue #24097):
 * issues, returns and cancellations of every batch of an item, on the same
 * basis as the Medicine lines of the final bill.
 * <p>
 * Issue bills that were later cancelled are counted as issued, and their
 * cancellation bills are counted separately - cancelling only flags the
 * original bill, so excluding it as well would deduct the cancellation twice.
 * Quantities and values are summed as magnitudes and the sign is applied per
 * bill type, because return line net values are not stored with a consistent
 * sign.
 */
@Stateless
public class InpatientPharmacySummaryService {

    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    private static final List<BillTypeAtomic> DIRECT_ISSUE_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE
    );

    private static final List<BillTypeAtomic> REQUEST_ISSUE_TYPES = Arrays.asList(
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD
    );

    private static final List<BillTypeAtomic> RETURN_TYPES = Arrays.asList(
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
            // Porter-flow return: counted when the pharmacy accepts it (#25509).
            BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN
    );

    private static final List<BillTypeAtomic> CANCELLATION_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION
    );

    /**
     * Cancelling a BHT issue return: the patient keeps the medicines, so it adds back
     * what the return deducted (#26105).
     */
    private static final List<BillTypeAtomic> RETURN_CANCELLATION_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION
    );

    /**
     * One row per item with Direct Issue / Issued on Request / Returned /
     * Cancelled quantities and net values, ordered by item name.
     */
    public List<InpatientPharmacyItemMovementDTO> fetchItemMovements(PatientEncounter patientEncounter) {
        Map<Long, InpatientPharmacyItemMovementDTO> byItem = new LinkedHashMap<>();
        for (Object[] row : fetchRows(patientEncounter)) {
            Long itemId = (Long) row[0];
            BillTypeAtomic bta = (BillTypeAtomic) row[2];
            double qty = toDouble(row[3]);
            double value = toDouble(row[7]);
            InpatientPharmacyItemMovementDTO dto = byItem.get(itemId);
            if (dto == null) {
                dto = new InpatientPharmacyItemMovementDTO(itemId, (String) row[1]);
                byItem.put(itemId, dto);
            }
            if (DIRECT_ISSUE_TYPES.contains(bta)) {
                dto.addDirectIssue(qty, value);
            } else if (REQUEST_ISSUE_TYPES.contains(bta)) {
                dto.addRequestIssue(qty, value);
            } else if (RETURN_TYPES.contains(bta)) {
                dto.addReturn(qty, value);
            } else if (CANCELLATION_TYPES.contains(bta)) {
                dto.addCancelled(qty, value);
            } else if (RETURN_CANCELLATION_TYPES.contains(bta)) {
                dto.addReturn(-qty, -value);
            }
        }
        return new ArrayList<>(byItem.values());
    }

    /**
     * Net quantity, gross, discount, service charge and net value per item
     * (issues minus returns minus cancellations), ordered by item name.
     */
    public List<InpatientPharmacyNetSummaryDTO> fetchNetSummary(PatientEncounter patientEncounter) {
        Map<Long, InpatientPharmacyNetSummaryDTO> byItem = new LinkedHashMap<>();
        for (Object[] row : fetchRows(patientEncounter)) {
            Long itemId = (Long) row[0];
            double sign = signOf((BillTypeAtomic) row[2]);
            InpatientPharmacyNetSummaryDTO dto = byItem.get(itemId);
            if (dto == null) {
                dto = new InpatientPharmacyNetSummaryDTO(itemId, (String) row[1], 0.0, 0.0, 0.0, 0.0, 0.0);
                byItem.put(itemId, dto);
            }
            dto.setNetQty(dto.getNetQty() + sign * toDouble(row[3]));
            dto.setNetGrossValue(dto.getNetGrossValue() + sign * toDouble(row[4]));
            dto.setNetDiscount(dto.getNetDiscount() + sign * toDouble(row[5]));
            dto.setNetServiceCharge(dto.getNetServiceCharge() + sign * toDouble(row[6]));
            dto.setNetValue(dto.getNetValue() + sign * toDouble(row[7]));
        }
        return new ArrayList<>(byItem.values());
    }

    private double signOf(BillTypeAtomic bta) {
        if (DIRECT_ISSUE_TYPES.contains(bta) || REQUEST_ISSUE_TYPES.contains(bta)
                || RETURN_CANCELLATION_TYPES.contains(bta)) {
            return 1.0;
        }
        return -1.0;
    }

    /**
     * Rows of [itemId, itemName, billTypeAtomic, |qty|, |gross|, |discount|,
     * |margin|, |net|] grouped by item and bill type.
     */
    private List<Object[]> fetchRows(PatientEncounter patientEncounter) {
        if (patientEncounter == null) {
            return new ArrayList<>();
        }
        List<BillTypeAtomic> billTypes = new ArrayList<>();
        billTypes.addAll(DIRECT_ISSUE_TYPES);
        billTypes.addAll(REQUEST_ISSUE_TYPES);
        billTypes.addAll(RETURN_TYPES);
        billTypes.addAll(CANCELLATION_TYPES);
        billTypes.addAll(RETURN_CANCELLATION_TYPES);

        List<Long> peIds = encounterIdsWithChildren(patientEncounter);

        String jpql = "SELECT bi.item.id, bi.item.name, bi.bill.billTypeAtomic, "
                + "SUM(ABS(pbi.qty)), "
                + "SUM(ABS(bi.grossValue)), "
                + "SUM(ABS(bi.discount)), "
                + "SUM(ABS(bi.marginValue)), "
                + "SUM(ABS(bi.netValue)) "
                + "FROM BillItem bi LEFT JOIN bi.pharmaceuticalBillItem pbi "
                + "WHERE bi.bill.patientEncounter.id IN :peIds "
                + "AND bi.bill.billTypeAtomic IN :billTypes "
                + "AND bi.retired = FALSE "
                + "AND bi.bill.retired = FALSE "
                // The data-admin reversal of an issue (same issue type, negative values) and
                // the original it cancelled net to 0 in the interim bill; drop both (#26107).
                + "AND " + InwardMedicineBillTypes.JPQL_NOT_REVERSAL_BILL
                + "AND " + InwardMedicineBillTypes.JPQL_NOT_REVERSED_ORIGINAL
                + "GROUP BY bi.item.id, bi.item.name, bi.bill.billTypeAtomic "
                + "ORDER BY bi.item.name";

        Map<String, Object> params = new HashMap<>();
        params.put("peIds", peIds);
        params.put("billTypes", billTypes);

        List<Object[]> rows = billItemFacade.findObjectArrayByJpql(jpql, params, null);
        return rows != null ? rows : new ArrayList<>();
    }

    /**
     * The admission and its child admissions (such as a baby BHT): the interim
     * bill sums them with the parent, so this summary does too (#26100).
     */
    private List<Long> encounterIdsWithChildren(PatientEncounter patientEncounter) {
        Map<String, Object> params = new HashMap<>();
        params.put("pe", patientEncounter);
        List<PatientEncounter> children = patientEncounterFacade.findByJpql(
                "SELECT pe FROM PatientEncounter pe WHERE pe.parentEncounter = :pe AND pe.retired = false", params);
        return idsOf(patientEncounter, children);
    }

    static List<Long> idsOf(PatientEncounter patientEncounter, List<PatientEncounter> children) {
        List<Long> ids = new ArrayList<>();
        ids.add(patientEncounter.getId());
        if (children != null) {
            for (PatientEncounter child : children) {
                ids.add(child.getId());
            }
        }
        return ids;
    }

    private double toDouble(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }
}
