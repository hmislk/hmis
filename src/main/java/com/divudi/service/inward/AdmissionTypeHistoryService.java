package com.divudi.service.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.inward.AdmissionTypeTimeline;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.AdmissionTypeHistory;
import com.divudi.core.facade.AdmissionTypeHistoryFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.service.AuditService;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * The dated history of an admission's type, and the change itself (#25814).
 *
 * <p>A change takes effect at its exact time for dated charges; per-day charges (linen) switch from the next day
 * (see {@link AdmissionTypeTimeline}).
 *
 * <p>Bill-time calculations ask {@link #timelineFor} for the type in force at
 * a date. An admission with no history rows answers with its current type for
 * every date, so nothing changes for admissions that were never converted.
 */
@Stateless
public class AdmissionTypeHistoryService {

    @EJB
    private AdmissionTypeHistoryFacade admissionTypeHistoryFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private AuditService auditService;

    public List<AdmissionTypeHistory> fetchHistory(PatientEncounter encounter) {
        if (encounter == null || encounter.getId() == null) {
            return new java.util.ArrayList<>();
        }
        String jpql = "select h from AdmissionTypeHistory h "
                + " where h.patientEncounter=:pe "
                + " order by h.fromDate, h.id";
        Map<String, Object> m = new HashMap<>();
        m.put("pe", encounter);
        // the TemporalType overload lets a query failure propagate, so a failed
        // lookup is not read as "no history"
        return admissionTypeHistoryFacade.findByJpql(jpql, m, javax.persistence.TemporalType.TIMESTAMP);
    }

    /** The timeline for an admission: its current type plus any history rows. */
    public AdmissionTypeTimeline timelineFor(PatientEncounter encounter) {
        if (encounter == null) {
            return new AdmissionTypeTimeline(null, null);
        }
        return new AdmissionTypeTimeline(encounter.getAdmissionType(), fetchHistory(encounter));
    }

    /**
     * Timelines for many admissions with one query per 500 admissions, keyed by
     * admission id. Admissions with no history get a timeline of their current type.
     */
    public Map<Long, AdmissionTypeTimeline> timelinesFor(java.util.Collection<PatientEncounter> encounters) {
        Map<Long, AdmissionTypeTimeline> result = new HashMap<>();
        if (encounters == null || encounters.isEmpty()) {
            return result;
        }
        Map<Long, List<AdmissionTypeHistory>> rowsById = new HashMap<>();
        List<Long> ids = new java.util.ArrayList<>();
        for (PatientEncounter e : encounters) {
            if (e != null && e.getId() != null) {
                ids.add(e.getId());
            }
        }
        for (int i = 0; i < ids.size(); i += 500) {
            List<Long> chunk = ids.subList(i, Math.min(i + 500, ids.size()));
            String jpql = "select h from AdmissionTypeHistory h "
                    + " where h.patientEncounter.id in :ids "
                    + " order by h.fromDate, h.id";
            Map<String, Object> m = new HashMap<>();
            m.put("ids", new java.util.ArrayList<>(chunk));
            List<AdmissionTypeHistory> rows = admissionTypeHistoryFacade.findByJpql(jpql, m, javax.persistence.TemporalType.TIMESTAMP);
            if (rows != null) {
                for (AdmissionTypeHistory h : rows) {
                    rowsById.computeIfAbsent(h.getPatientEncounter().getId(), k -> new java.util.ArrayList<>()).add(h);
                }
            }
        }
        for (PatientEncounter e : encounters) {
            if (e != null && e.getId() != null) {
                result.put(e.getId(), new AdmissionTypeTimeline(e.getAdmissionType(), rowsById.get(e.getId())));
            }
        }
        return result;
    }

    /** The type in force at the date; the current type when there is no history. */
    public AdmissionType typeAt(PatientEncounter encounter, Date date) {
        return timelineFor(encounter).typeAt(date);
    }

    /** The type the admission started with; the admission fee follows it. */
    public AdmissionType firstType(PatientEncounter encounter) {
        return timelineFor(encounter).firstType();
    }

    /** Drops the milliseconds so a DATETIME column never rounds the value up to the next second or day. */
    static Date wholeSeconds(Date d) {
        return new Date((d.getTime() / 1000L) * 1000L);
    }

    /** Interim bills still standing on the admission (a warning, not a block). */
    public long countInterimBills(PatientEncounter encounter) {
        if (encounter == null) {
            return 0;
        }
        String jpql = "select count(b) from Bill b "
                + " where b.retired=false and b.cancelled=false "
                + " and b.billType=:bt and b.patientEncounter=:pe";
        Map<String, Object> m = new HashMap<>();
        m.put("bt", BillType.InwardIntrimBill);
        m.put("pe", encounter);
        return billFacade.findLongByJpql(jpql, m);
    }

    /**
     * Changes the admission type of an admitted, not yet discharged patient.
     * Charges already added keep their stored values; calculations done at bill
     * time use the old type before the change and the new type from it.
     *
     * @throws AdmissionTypeChangeException when the change is refused
     */
    public void changeAdmissionType(PatientEncounter encounterRef, AdmissionType newType, String reason, WebUser user) {
        changeAdmissionType(encounterRef, newType, reason, user, null);
    }

    /**
     * As above. The admission row is locked for the transaction, so two saves at
     * once run one after the other; when the page showed a type
     * ({@code expectedCurrentType}) and the admission now has another, the change
     * is refused instead of writing a second history row on stale data.
     */
    public void changeAdmissionType(PatientEncounter encounterRef, AdmissionType newType, String reason, WebUser user, AdmissionType expectedCurrentType) {
        if (encounterRef == null || encounterRef.getId() == null) {
            throw new AdmissionTypeChangeException("No admission selected.");
        }
        PatientEncounter encounter = patientEncounterFacade.findWithLock(encounterRef.getId());
        if (encounter == null) {
            throw new AdmissionTypeChangeException("Admission not found.");
        }
        if (newType == null) {
            throw new AdmissionTypeChangeException("Select the new admission type.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new AdmissionTypeChangeException("Enter the reason for the change.");
        }
        AdmissionType oldType = encounter.getAdmissionType();
        if (expectedCurrentType != null && expectedCurrentType.getId() != null
                && (oldType == null || !expectedCurrentType.getId().equals(oldType.getId()))) {
            throw new AdmissionTypeChangeException("The admission type was changed by someone else. Open the Change Admission Type page again.");
        }
        if (oldType != null && oldType.getId() != null && oldType.getId().equals(newType.getId())) {
            throw new AdmissionTypeChangeException("The admission already has this admission type.");
        }
        if (newType.isRetired()) {
            throw new AdmissionTypeChangeException("The selected admission type is not active.");
        }
        if (Boolean.TRUE.equals(encounter.isDischarged())) {
            throw new AdmissionTypeChangeException("The patient is already discharged. The admission type cannot be changed.");
        }
        if (encounter.isPaymentFinalized()) {
            throw new AdmissionTypeChangeException("The final bill is already settled. The admission type cannot be changed.");
        }

        Date now = wholeSeconds(new Date());
        if (encounter.getDateOfAdmission() != null && encounter.getDateOfAdmission().after(now)) {
            throw new AdmissionTypeChangeException("The admission date is in the future. The admission type cannot be changed yet.");
        }
        String cleanReason = reason.trim();
        List<AdmissionTypeHistory> rows;
        try {
            rows = fetchHistory(encounter);
        } catch (RuntimeException e) {
            if (e instanceof AdmissionTypeChangeException) {
                throw e;
            }
            throw new AdmissionTypeChangeException("The admission type history could not be read. Nothing was changed. Try again.");
        }
        if (rows.isEmpty()) {
            AdmissionTypeHistory first = new AdmissionTypeHistory();
            first.setPatientEncounter(encounter);
            first.setAdmissionType(oldType);
            Date firstFrom = encounter.getDateOfAdmission() != null ? encounter.getDateOfAdmission() : encounter.getCreatedAt();
            first.setFromDate(firstFrom != null ? wholeSeconds(firstFrom) : null);
            first.setToDate(now);
            first.setCreatedAt(now);
            first.setCreater(user);
            first.setReason("Type the admission started with");
            admissionTypeHistoryFacade.create(first);
        } else {
            AdmissionTypeHistory open = rows.get(rows.size() - 1);
            open.setToDate(now);
            admissionTypeHistoryFacade.edit(open);
        }
        AdmissionTypeHistory next = new AdmissionTypeHistory();
        next.setPatientEncounter(encounter);
        next.setAdmissionType(newType);
        next.setFromDate(now);
        next.setToDate(null);
        next.setCreatedAt(now);
        next.setCreater(user);
        next.setReason(cleanReason);
        admissionTypeHistoryFacade.create(next);

        encounter.setAdmissionType(newType);
        patientEncounterFacade.edit(encounter);

        Map<String, Object> before = new HashMap<>();
        before.put("admissionType", oldType != null ? oldType.getName() : null);
        Map<String, Object> after = new HashMap<>();
        after.put("admissionType", newType.getName());
        after.put("reason", cleanReason);
        after.put("changedAt", now.toString());
        String trigger = "Admission type changed from "
                + (oldType != null ? oldType.getName() : "(none)") + " to " + newType.getName()
                + ". Reason: " + cleanReason;
        auditService.logEncounterAudit(encounter, trigger, before, after, user);
    }
}
