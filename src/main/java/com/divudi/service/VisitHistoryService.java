package com.divudi.service;

import com.divudi.core.data.VisitHistoryType;
import com.divudi.core.data.dto.VisitHistoryRowDTO;
import com.divudi.core.facade.BillFacade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Visit History of a patient (issue #25846): every bill and admission of the
 * patient, worked out at query time by patient id. Nothing is stored, so
 * merging or splitting patients is always correct. Cancellations, returns and
 * refunds are their own rows; nothing is netted out or hidden for staff.
 *
 * See developer_docs/billing/visit-history-business-rule.md.
 */
@Stateless
public class VisitHistoryService {

    /**
     * Per-source row cap, so a very busy patient cannot return an unbounded
     * list. The newest rows are kept.
     */
    public static final int MAX_ROWS_PER_SOURCE = 5000;

    @EJB
    private BillFacade billFacade;

    /**
     * Visit history rows, newest first.
     *
     * @param patientId id of the patient (never an entity)
     * @param fromDate optional start (inclusive); null for no lower bound
     * @param toDate optional end (inclusive); null for no upper bound
     * @param visitType optional visit type filter; null for all types
     * @param portalView true for the patient portal: internal bill types are
     * excluded. Staff views pass false.
     * @return the rows, never null
     * @throws RuntimeException when any source query fails, so a partial
     * history is never shown as a complete one
     */
    @SuppressWarnings("unchecked")
    public List<VisitHistoryRowDTO> findVisitHistory(Long patientId, Date fromDate, Date toDate,
            VisitHistoryType visitType, boolean portalView) {
        List<VisitHistoryRowDTO> rows = new ArrayList<>();
        if (patientId == null) {
            return rows;
        }

        // 1. Admissions of the patient.
        Map<String, Object> pa = new HashMap<>();
        pa.put("pid", patientId);
        String admissionDate = "coalesce(a.dateOfAdmission, a.createdAt)";
        String ja = "select new com.divudi.core.data.dto.VisitHistoryRowDTO("
                + " a.id, a.bhtNo, a.dateOfAdmission, a.createdAt, a.dateOfDischarge, d.name, sp.name) "
                + " from Admission a "
                + " left join a.department d "
                + " left join a.referringConsultant s "
                + " left join s.person sp "
                + " where a.retired=false and a.patient.id=:pid ";
        ja += dateBounds(admissionDate, fromDate, toDate, pa);
        ja += " order by " + admissionDate + " desc, a.id desc";
        List<VisitHistoryRowDTO> admissionRows = (List<VisitHistoryRowDTO>) billFacade.findLightsByJpqlStrict(
                ja, pa, TemporalType.TIMESTAMP, MAX_ROWS_PER_SOURCE);

        // 2. Bills of the patient.
        String billDate = "coalesce(b.createdAt, b.billTime)";
        String selectBill = "select new com.divudi.core.data.dto.VisitHistoryRowDTO("
                + " b.id, b.deptId, b.insId, b.createdAt, b.billTime, b.billTypeAtomic, d.departmentType, d.name, sp.name,"
                + " b.cancelled, b.refunded) "
                + " from Bill b "
                + " left join b.department d "
                + " left join b.staff s "
                + " left join s.person sp "
                + " where b.retired=false ";
        Map<String, Object> pb = new HashMap<>();
        pb.put("pid", patientId);
        String jb = selectBill + " and b.patient.id=:pid ";
        jb += dateBounds(billDate, fromDate, toDate, pb);
        jb += " order by " + billDate + " desc, b.id desc";
        List<VisitHistoryRowDTO> billRows = (List<VisitHistoryRowDTO>) billFacade.findLightsByJpqlStrict(
                jb, pb, TemporalType.TIMESTAMP, MAX_ROWS_PER_SOURCE);

        // 3. Inpatient bills raised against the patient's admissions but not
        // carrying the patient id themselves. Membership is a subquery on the
        // patient's admissions, so no admission is dropped by a row cap.
        List<VisitHistoryRowDTO> encounterBillRows = new ArrayList<>();
        {
            Map<String, Object> pe = new HashMap<>();
            pe.put("pid", patientId);
            String je = selectBill + " and b.patientEncounter.id in "
                    + "(select ea.id from Admission ea where ea.retired=false and ea.patient.id=:pid) "
                    + " and (b.patient is null or b.patient.id=:pid) ";
            je += dateBounds(billDate, fromDate, toDate, pe);
            je += " order by " + billDate + " desc, b.id desc";
            List<VisitHistoryRowDTO> r = (List<VisitHistoryRowDTO>) billFacade.findLightsByJpqlStrict(
                    je, pe, TemporalType.TIMESTAMP, MAX_ROWS_PER_SOURCE);
            if (r != null) {
                encounterBillRows = r;
            }
        }

        // 4. Merge, drop duplicates (the same bill found by patient and by admission), filter.
        Set<Long> seenBills = new HashSet<>();
        List<VisitHistoryRowDTO> merged = new ArrayList<>();
        if (admissionRows != null) {
            merged.addAll(admissionRows);
        }
        List<VisitHistoryRowDTO> allBills = new ArrayList<>();
        if (billRows != null) {
            allBills.addAll(billRows);
        }
        allBills.addAll(encounterBillRows);
        for (VisitHistoryRowDTO r : allBills) {
            if (r.getId() != null && seenBills.add(r.getId())) {
                merged.add(r);
            }
        }
        List<VisitHistoryRowDTO> result = new ArrayList<>();
        for (VisitHistoryRowDTO r : merged) {
            if (visitType != null && r.getVisitType() != visitType) {
                continue;
            }
            if (portalView && r.isInternalForPortal()) {
                continue;
            }
            result.add(r);
        }
        result.sort(Comparator.comparing(VisitHistoryRowDTO::getEventDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private String dateBounds(String dateExpr, Date fromDate, Date toDate, Map<String, Object> params) {
        String clause = "";
        if (fromDate != null) {
            clause += " and " + dateExpr + " >= :fromDate ";
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            clause += " and " + dateExpr + " <= :toDate ";
            params.put("toDate", toDate);
        }
        return clause;
    }

    /**
     * True when a source hit the per-source cap, so older rows may be missing.
     */
    public boolean mayBeTruncated(List<VisitHistoryRowDTO> unfilteredOrFilteredRows) {
        if (unfilteredOrFilteredRows == null) {
            return false;
        }
        int admissions = 0;
        int bills = 0;
        for (VisitHistoryRowDTO r : unfilteredOrFilteredRows) {
            if (r.isAdmission()) {
                admissions++;
            } else {
                bills++;
            }
        }
        return admissions >= MAX_ROWS_PER_SOURCE || bills >= MAX_ROWS_PER_SOURCE;
    }
}
