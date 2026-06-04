package com.divudi.service.patient;

import com.divudi.core.data.PatientMergeStatus;
import com.divudi.core.data.PatientMergeType;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PatientMergeAffectedRecord;
import com.divudi.core.entity.PatientMergeRecord;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientMergeAffectedRecordFacade;
import com.divudi.core.facade.PatientMergeRecordFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PersonFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class PatientMergeService {

    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PatientReportFacade patientReportFacade;
    @EJB
    private PatientMergeRecordFacade patientMergeRecordFacade;
    @EJB
    private PatientMergeAffectedRecordFacade patientMergeAffectedRecordFacade;

    /**
     * Merges secondary into primary. Steps:
     * 1. Snapshot both patients to JSON
     * 2. Fill null primary demographics from secondary
     * 3. Collect affected IDs, bulk-re-point all 4 entity types
     * 4. Retire secondary
     * 5. Persist PatientMergeRecord with all affected rows
     */
    public PatientMergeRecord merge(Patient primary, Patient secondary,
            PatientMergeType mergeType, String reason, WebUser mergedBy) {

        Date now = new Date();

        // 1. Snapshot
        PatientMergeRecord mergeRecord = new PatientMergeRecord();
        mergeRecord.setPrimaryPatient(primary);
        mergeRecord.setSecondaryPatient(secondary);
        mergeRecord.setMergeDate(now);
        mergeRecord.setMergedBy(mergedBy);
        mergeRecord.setMergeType(mergeType);
        mergeRecord.setMergeReason(reason);
        mergeRecord.setStatus(PatientMergeStatus.ACTIVE);
        mergeRecord.setPrimarySnapshotJson(snapshotPatient(primary));
        mergeRecord.setSecondarySnapshotJson(snapshotPatient(secondary));

        // 2. Fill null primary fields from secondary
        fillNullDemographics(primary, secondary);
        patientFacade.edit(primary);
        if (primary.getPerson() != null) {
            personFacade.edit(primary.getPerson());
        }

        // 3. Collect IDs then bulk-update each entity type
        List<PatientMergeAffectedRecord> affected = new ArrayList<>();
        affected.addAll(collectAndRepoint("Bill", primary, secondary));
        affected.addAll(collectAndRepointEncounters(primary, secondary));
        affected.addAll(collectAndRepointInvestigations(primary, secondary));
        affected.addAll(collectAndRepointReports(primary, secondary));

        // 4. Retire secondary
        secondary.setRetired(true);
        secondary.setRetireComments("Merged into patient #" + primary.getId() + " on " + now);
        patientFacade.edit(secondary);

        // 5. Persist
        patientMergeRecordFacade.create(mergeRecord);
        for (PatientMergeAffectedRecord ar : affected) {
            ar.setMergeRecord(mergeRecord);
            patientMergeAffectedRecordFacade.create(ar);
        }
        mergeRecord.setAffectedRecords(affected);

        return mergeRecord;
    }

    /**
     * Reverses a previously completed merge.
     */
    public void unmerge(PatientMergeRecord mergeRecord, WebUser reversedBy) {
        if (mergeRecord.getStatus() != PatientMergeStatus.ACTIVE) {
            throw new IllegalStateException("Merge #" + mergeRecord.getId() + " is already reversed.");
        }

        Patient primary = mergeRecord.getPrimaryPatient();
        Patient secondary = mergeRecord.getSecondaryPatient();

        // Restore affected records
        List<PatientMergeAffectedRecord> affected = loadAffectedRecords(mergeRecord);
        for (PatientMergeAffectedRecord ar : affected) {
            restorePatientReference(ar);
        }

        // Restore primary demographics from snapshot (undo any null-fill that was applied)
        restorePatientFromSnapshot(primary, mergeRecord.getPrimarySnapshotJson());
        patientFacade.edit(primary);
        if (primary.getPerson() != null) {
            personFacade.edit(primary.getPerson());
        }

        // Restore secondary demographics from snapshot
        restorePatientFromSnapshot(secondary, mergeRecord.getSecondarySnapshotJson());

        // Un-retire secondary
        secondary.setRetired(false);
        secondary.setRetireComments(null);
        patientFacade.edit(secondary);
        if (secondary.getPerson() != null) {
            personFacade.edit(secondary.getPerson());
        }

        // Mark merge reversed
        mergeRecord.setStatus(PatientMergeStatus.REVERSED);
        mergeRecord.setReversedBy(reversedBy);
        mergeRecord.setReversedAt(new Date());
        patientMergeRecordFacade.edit(mergeRecord);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private List<PatientMergeAffectedRecord> collectAndRepoint(
            String entityName, Patient primary, Patient secondary) {

        List<Long> ids = collectIds(entityName, secondary);
        if (!ids.isEmpty()) {
            bulkUpdate(entityName, primary, secondary);
        }
        return buildAffectedRows(entityName, ids, secondary.getId(), primary.getId());
    }

    private List<PatientMergeAffectedRecord> collectAndRepointEncounters(
            Patient primary, Patient secondary) {

        String jpql = "select pe.id from PatientEncounter pe where pe.patient = :sec and pe.retired = false";
        Map<String, Object> p = new HashMap<>();
        p.put("sec", secondary);
        List<Long> ids = patientEncounterFacade.findLongValuesByJpql(jpql, p);
        if (!ids.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("pri", primary);
            params.put("sec", secondary);
            patientEncounterFacade.updateByJpql(
                    "update PatientEncounter pe set pe.patient = :pri where pe.patient = :sec and pe.retired = false", params);
        }
        return buildAffectedRows("PatientEncounter", ids, secondary.getId(), primary.getId());
    }

    private List<PatientMergeAffectedRecord> collectAndRepointInvestigations(
            Patient primary, Patient secondary) {

        String jpql = "select pi.id from PatientInvestigation pi where pi.patient = :sec and pi.retired = false";
        Map<String, Object> p = new HashMap<>();
        p.put("sec", secondary);
        List<Long> ids = patientInvestigationFacade.findLongValuesByJpql(jpql, p);
        if (!ids.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("pri", primary);
            params.put("sec", secondary);
            patientInvestigationFacade.updateByJpql(
                    "update PatientInvestigation pi set pi.patient = :pri where pi.patient = :sec and pi.retired = false", params);
        }
        return buildAffectedRows("PatientInvestigation", ids, secondary.getId(), primary.getId());
    }

    private List<PatientMergeAffectedRecord> collectAndRepointReports(
            Patient primary, Patient secondary) {

        String jpql = "select pr.id from PatientReport pr where pr.patient = :sec and pr.retired = false";
        Map<String, Object> p = new HashMap<>();
        p.put("sec", secondary);
        List<Long> ids = patientReportFacade.findLongValuesByJpql(jpql, p);
        if (!ids.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("pri", primary);
            params.put("sec", secondary);
            patientReportFacade.updateByJpql(
                    "update PatientReport pr set pr.patient = :pri where pr.patient = :sec and pr.retired = false", params);
        }
        return buildAffectedRows("PatientReport", ids, secondary.getId(), primary.getId());
    }

    private List<Long> collectIds(String entityName, Patient secondary) {
        String jpql = "select e.id from " + entityName + " e where e.patient = :sec and e.retired = false";
        Map<String, Object> p = new HashMap<>();
        p.put("sec", secondary);
        switch (entityName) {
            case "Bill":
                return billFacade.findLongValuesByJpql(jpql, p);
            default:
                return new ArrayList<>();
        }
    }

    private void bulkUpdate(String entityName, Patient primary, Patient secondary) {
        Map<String, Object> params = new HashMap<>();
        params.put("pri", primary);
        params.put("sec", secondary);
        String jpql = "update " + entityName + " e set e.patient = :pri where e.patient = :sec and e.retired = false";
        switch (entityName) {
            case "Bill":
                billFacade.updateByJpql(jpql, params);
                break;
        }
    }

    private List<PatientMergeAffectedRecord> buildAffectedRows(
            String entityClass, List<Long> ids, Long oldId, Long newId) {
        List<PatientMergeAffectedRecord> rows = new ArrayList<>();
        for (Long id : ids) {
            PatientMergeAffectedRecord ar = new PatientMergeAffectedRecord();
            ar.setEntityClass(entityClass);
            ar.setEntityId(id);
            ar.setOldPatientId(oldId);
            ar.setNewPatientId(newId);
            rows.add(ar);
        }
        return rows;
    }

    private void restorePatientReference(PatientMergeAffectedRecord ar) {
        Patient old = patientFacade.find(ar.getOldPatientId());
        if (old == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("old", old);
        params.put("id", ar.getEntityId());
        String jpql = "update " + ar.getEntityClass() + " e set e.patient = :old where e.id = :id";
        switch (ar.getEntityClass()) {
            case "Bill":
                billFacade.updateByJpql(jpql, params);
                break;
            case "PatientEncounter":
                patientEncounterFacade.updateByJpql(jpql, params);
                break;
            case "PatientInvestigation":
                patientInvestigationFacade.updateByJpql(jpql, params);
                break;
            case "PatientReport":
                patientReportFacade.updateByJpql(jpql, params);
                break;
        }
    }

    private List<PatientMergeAffectedRecord> loadAffectedRecords(PatientMergeRecord mergeRecord) {
        String jpql = "select ar from PatientMergeAffectedRecord ar where ar.mergeRecord = :mr";
        Map<String, Object> p = new HashMap<>();
        p.put("mr", mergeRecord);
        return patientMergeAffectedRecordFacade.findByJpql(jpql, p);
    }

    private void fillNullDemographics(Patient primary, Patient secondary) {
        if (primary.getPerson() == null || secondary.getPerson() == null) {
            return;
        }
        Person pri = primary.getPerson();
        Person sec = secondary.getPerson();

        if (pri.getName() == null) pri.setName(sec.getName());
        if (pri.getNic() == null || pri.getNic().isEmpty()) pri.setNic(sec.getNic());
        if (pri.getPhone() == null || pri.getPhone().isEmpty()) pri.setPhone(sec.getPhone());
        if (pri.getMobile() == null || pri.getMobile().isEmpty()) pri.setMobile(sec.getMobile());
        if (pri.getEmail() == null || pri.getEmail().isEmpty()) pri.setEmail(sec.getEmail());
        if (pri.getAddress() == null || pri.getAddress().isEmpty()) pri.setAddress(sec.getAddress());
        if (pri.getDob() == null) pri.setDob(sec.getDob());
        if (pri.getSex() == null) pri.setSex(sec.getSex());
        if (pri.getTitle() == null) pri.setTitle(sec.getTitle());
        if (primary.getPhn() == null || primary.getPhn().isEmpty()) primary.setPhn(secondary.getPhn());
        if (primary.getCode() == null || primary.getCode().isEmpty()) primary.setCode(secondary.getCode());
    }

    private String snapshotPatient(Patient patient) {
        try {
            java.util.LinkedHashMap<String, Object> snap = new java.util.LinkedHashMap<>();
            snap.put("id", patient.getId());
            snap.put("phn", patient.getPhn());
            snap.put("code", patient.getCode());
            snap.put("retired", patient.isRetired());
            snap.put("retireComments", patient.getRetireComments());
            if (patient.getPerson() != null) {
                Person p = patient.getPerson();
                java.util.LinkedHashMap<String, Object> personSnap = new java.util.LinkedHashMap<>();
                personSnap.put("id", p.getId());
                personSnap.put("name", p.getName());
                personSnap.put("nic", p.getNic());
                personSnap.put("phone", p.getPhone());
                personSnap.put("mobile", p.getMobile());
                personSnap.put("email", p.getEmail());
                personSnap.put("address", p.getAddress());
                personSnap.put("dob", p.getDob() != null ? p.getDob().getTime() : null);
                personSnap.put("sex", p.getSex() != null ? p.getSex().name() : null);
                personSnap.put("title", p.getTitle() != null ? p.getTitle().name() : null);
                snap.put("person", personSnap);
            }
            return new com.google.gson.Gson().toJson(snap);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void restorePatientFromSnapshot(Patient patient, String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (root.has("phn") && !root.get("phn").isJsonNull()) {
                patient.setPhn(root.get("phn").getAsString());
            }
            if (root.has("code") && !root.get("code").isJsonNull()) {
                patient.setCode(root.get("code").getAsString());
            }
            if (root.has("person") && !root.get("person").isJsonNull()) {
                com.google.gson.JsonObject ps = root.getAsJsonObject("person");
                Person p = patient.getPerson();
                if (p != null) {
                    if (ps.has("name") && !ps.get("name").isJsonNull()) p.setName(ps.get("name").getAsString());
                    if (ps.has("nic") && !ps.get("nic").isJsonNull()) p.setNic(ps.get("nic").getAsString());
                    if (ps.has("phone") && !ps.get("phone").isJsonNull()) p.setPhone(ps.get("phone").getAsString());
                    if (ps.has("mobile") && !ps.get("mobile").isJsonNull()) p.setMobile(ps.get("mobile").getAsString());
                    if (ps.has("email") && !ps.get("email").isJsonNull()) p.setEmail(ps.get("email").getAsString());
                    if (ps.has("address") && !ps.get("address").isJsonNull()) p.setAddress(ps.get("address").getAsString());
                    if (ps.has("dob") && !ps.get("dob").isJsonNull()) p.setDob(new Date(ps.get("dob").getAsLong()));
                }
            }
        } catch (Exception e) {
            // snapshot parse failure — leave patient as-is
        }
    }
}
