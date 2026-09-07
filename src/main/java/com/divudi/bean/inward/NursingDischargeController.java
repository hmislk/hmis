package com.divudi.bean.inward;

import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.PendingPharmacyItemDTO;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Handles Nursing Discharge (stage 4) and Physical Discharge (stage 5) of the
 * inpatient workflow.
 *
 * Flow: Clinical → Room → Nursing → Administrative → Physical
 */
@Named
@SessionScoped
public class NursingDischargeController implements Serializable {

    @EJB
    private com.divudi.service.AuditService auditService;

    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    @EJB
    private BillFacade billFacade;

    @EJB
    private PatientRoomFacade patientRoomFacade;

    @Inject
    private SessionController sessionController;

    @Inject
    private NotificationController notificationController;

    private PatientEncounter currentEncounter;
    private List<PendingPharmacyItemDTO> pendingPharmacyItems = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    public String navigateToNursingDischarge(PatientEncounter pe) {
        this.currentEncounter = pe;
        loadPendingPharmacyItems();
        return "/inward/inward_nursing_discharge?faces-redirect=true";
    }

    public String navigateToPhysicalDischarge(PatientEncounter pe) {
        this.currentEncounter = pe;
        return "/inward/inward_physical_discharge?faces-redirect=true";
    }

    // -------------------------------------------------------------------------
    // Pending pharmacy validation
    // -------------------------------------------------------------------------

    /**
     * Rebuilds the list of pharmacy transactions that block nursing discharge.
     * <p>
     * Every query below uses explicit {@code LEFT JOIN}s for {@code creater} and
     * {@code toDepartment} rather than implicit path navigation
     * ({@code b.toDepartment.name}). Implicit navigation through a nullable
     * relationship generates an INNER JOIN, which silently drops every row whose
     * FK is NULL - and {@code PharmacySaleBhtController.savePreBillFinally()}
     * explicitly sets {@code toDepartment = null} on inward issue bills. That made
     * the whole pending check return an empty list, so nursing discharge went
     * through with medicines still awaiting ward receipt (issue #23222).
     */
    public void loadPendingPharmacyItems() {
        pendingPharmacyItems = new ArrayList<>();
        if (currentEncounter == null) {
            return;
        }
        pendingPharmacyItems.addAll(fetchPendingRequests());
        pendingPharmacyItems.addAll(fetchUnacceptedIssues());
        pendingPharmacyItems.addAll(fetchUnacceptedWardReturns());
        pendingPharmacyItems.addAll(fetchUnprocessedDirectIssueReturns());
    }

    @SuppressWarnings("unchecked")
    private List<PendingPharmacyItemDTO> fetchPendingRequests() {
        String jpql = "SELECT new com.divudi.core.data.dto.PendingPharmacyItemDTO(b.id, b.deptId, b.billDate, b.billTypeAtomic,"
                + " COALESCE(createrPerson.name, ''), COALESCE(toDept.name, ''))"
                + " FROM Bill b"
                + " LEFT JOIN b.creater creater"
                + " LEFT JOIN creater.webUserPerson createrPerson"
                + " LEFT JOIN b.toDepartment toDept"
                + " WHERE b.patientEncounter = :pe"
                + " AND b.billTypeAtomic = :bta"
                + " AND b.cancelled = false"
                + " AND b.completed = false"
                + " AND b.fullyIssued = false";
        Map<String, Object> params = new HashMap<>();
        params.put("pe", currentEncounter);
        params.put("bta", BillTypeAtomic.REQUEST_MEDICINE_INWARD);
        return (List<PendingPharmacyItemDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.DATE);
    }

    @SuppressWarnings("unchecked")
    private List<PendingPharmacyItemDTO> fetchUnacceptedIssues() {
        String jpql = "SELECT new com.divudi.core.data.dto.PendingPharmacyItemDTO(b.id, b.deptId, b.billDate, b.billTypeAtomic,"
                + " COALESCE(createrPerson.name, ''), COALESCE(toDept.name, ''))"
                + " FROM Bill b"
                + " LEFT JOIN b.creater creater"
                + " LEFT JOIN creater.webUserPerson createrPerson"
                + " LEFT JOIN b.toDepartment toDept"
                + " WHERE b.patientEncounter = :pe"
                + " AND b.billTypeAtomic IN :btas"
                + " AND b.cancelled = false"
                + " AND NOT EXISTS ("
                + "   SELECT r FROM Bill r"
                + "   WHERE r.backwardReferenceBill = b"
                + "   AND r.billTypeAtomic = :acceptBta"
                + "   AND r.cancelled = false"
                + " )";
        Map<String, Object> params = new HashMap<>();
        params.put("pe", currentEncounter);
        params.put("btas", Arrays.asList(
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD));
        params.put("acceptBta", BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD);
        return (List<PendingPharmacyItemDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.DATE);
    }

    @SuppressWarnings("unchecked")
    private List<PendingPharmacyItemDTO> fetchUnacceptedWardReturns() {
        String jpql = "SELECT new com.divudi.core.data.dto.PendingPharmacyItemDTO(b.id, b.deptId, b.billDate, b.billTypeAtomic,"
                + " COALESCE(createrPerson.name, ''), COALESCE(toDept.name, ''))"
                + " FROM Bill b"
                + " LEFT JOIN b.creater creater"
                + " LEFT JOIN creater.webUserPerson createrPerson"
                + " LEFT JOIN b.toDepartment toDept"
                + " WHERE b.patientEncounter = :pe"
                + " AND b.billTypeAtomic = :bta"
                + " AND b.cancelled = false"
                + " AND b.completed = false"
                + " AND b.fullyIssued = false";
        Map<String, Object> params = new HashMap<>();
        params.put("pe", currentEncounter);
        params.put("bta", BillTypeAtomic.RETURN_MEDICINE_INWARD);
        return (List<PendingPharmacyItemDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.DATE);
    }

    @SuppressWarnings("unchecked")
    private List<PendingPharmacyItemDTO> fetchUnprocessedDirectIssueReturns() {
        String jpql = "SELECT new com.divudi.core.data.dto.PendingPharmacyItemDTO(b.id, b.deptId, b.billDate, b.billTypeAtomic,"
                + " COALESCE(createrPerson.name, ''), COALESCE(toDept.name, ''))"
                + " FROM Bill b"
                + " LEFT JOIN b.creater creater"
                + " LEFT JOIN creater.webUserPerson createrPerson"
                + " LEFT JOIN b.toDepartment toDept"
                + " WHERE b.patientEncounter = :pe"
                + " AND b.billTypeAtomic = :bta"
                + " AND b.cancelled = false"
                + " AND b.checkedBy IS NULL";
        Map<String, Object> params = new HashMap<>();
        params.put("pe", currentEncounter);
        params.put("bta", BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
        return (List<PendingPharmacyItemDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.DATE);
    }

    public boolean hasPendingPharmacyItems() {
        return !pendingPharmacyItems.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Nursing Discharge
    // -------------------------------------------------------------------------

    /**
     * Returns true when the encounter still has at least one active (non-retired,
     * non-discharged) room stay.
     * <p>
     * This is the source of truth for "room discharge completed" instead of the
     * cached {@code PatientEncounter.roomDischargeDateTime} field, which is only
     * stamped by {@code RoomChangeController} when the discharged room happens to
     * be the tail of the room-stay linked list ({@code nextRoom == null}). In
     * multi-room-change scenarios that field can stay {@code null} even though
     * every room stay has been discharged, permanently blocking nursing
     * discharge (issue #21935).
     */
    private boolean hasActiveRoom() {
        String jpql = "SELECT COUNT(pr) FROM PatientRoom pr"
                + " WHERE pr.patientEncounter = :pe"
                + " AND pr.retired = false"
                + " AND pr.discharged = false";
        Map<String, Object> params = new HashMap<>();
        params.put("pe", currentEncounter);
        return patientRoomFacade.findLongByJpql(jpql, params) > 0;
    }

    public void confirmNursingDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (currentEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Nursing discharge already confirmed.");
            return;
        }
        if (hasActiveRoom()) {
            JsfUtil.addErrorMessage("Cannot confirm nursing discharge: room discharge has not been completed.");
            return;
        }
        loadPendingPharmacyItems();
        if (!pendingPharmacyItems.isEmpty()) {
            JsfUtil.addErrorMessage("Cannot confirm nursing discharge: "
                    + pendingPharmacyItems.size()
                    + " pending pharmacy item(s) must be resolved first.");
            return;
        }
        Map<String, Object> before = nursingDischargeStateMap();
        currentEncounter.setNursingDischarged(Boolean.TRUE);
        currentEncounter.setNursingDischargeDateTime(new Date());
        currentEncounter.setNursingDischargedBy(sessionController.getLoggedUser());
        patientEncounterFacade.edit(currentEncounter);
        auditService.logEncounterAudit(currentEncounter, "Nursing Discharge",
                before, nursingDischargeStateMap(), sessionController.getLoggedUser());
        notificationController.createNotification(currentEncounter, "NursingDischarge");
        JsfUtil.addSuccessMessage("Nursing discharge confirmed.");
    }

    /**
     * Snapshot of the nursing/physical discharge flags for audit events (#22236).
     */
    private Map<String, Object> nursingDischargeStateMap() {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        if (currentEncounter == null) {
            return m;
        }
        m.put("nursingDischarged", currentEncounter.isNursingDischarged());
        m.put("nursingDischargeDateTime", currentEncounter.getNursingDischargeDateTime());
        m.put("nursingDischargedBy", currentEncounter.getNursingDischargedBy() != null
                ? currentEncounter.getNursingDischargedBy().getName() : null);
        m.put("physicalDischarged", currentEncounter.isPhysicalDischarged());
        m.put("physicalDischargeDateTime", currentEncounter.getPhysicalDischargeDateTime());
        m.put("physicalDischargedBy", currentEncounter.getPhysicalDischargedBy() != null
                ? currentEncounter.getPhysicalDischargedBy().getName() : null);
        return m;
    }

    public void cancelNursingDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (!currentEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Nursing discharge has not been confirmed.");
            return;
        }
        if (currentEncounter.isPhysicalDischarged()) {
            JsfUtil.addErrorMessage("Cannot cancel nursing discharge: physical discharge has already been confirmed. Cancel physical discharge first.");
            return;
        }
        Map<String, Object> before = nursingDischargeStateMap();
        currentEncounter.setNursingDischarged(Boolean.FALSE);
        currentEncounter.setNursingDischargeDateTime(null);
        currentEncounter.setNursingDischargedBy(null);
        patientEncounterFacade.edit(currentEncounter);
        auditService.logEncounterAudit(currentEncounter, "Nursing Discharge Cancelled",
                before, nursingDischargeStateMap(), sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage("Nursing discharge cancelled.");
    }

    // -------------------------------------------------------------------------
    // Physical Discharge
    // -------------------------------------------------------------------------

    public void confirmPhysicalDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (currentEncounter.isPhysicalDischarged()) {
            JsfUtil.addErrorMessage("Physical discharge already confirmed.");
            return;
        }
        if (!currentEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot confirm physical discharge: nursing discharge has not been completed.");
            return;
        }
        boolean administrativelyDischarged = Boolean.TRUE.equals(currentEncounter.getDischarged())
                || (currentEncounter.getParentEncounter() != null
                    && Boolean.TRUE.equals(currentEncounter.getParentEncounter().getDischarged()));
        if (!administrativelyDischarged) {
            JsfUtil.addErrorMessage("Cannot confirm physical discharge: administrative discharge (final bill) has not been completed.");
            return;
        }
        Map<String, Object> before = nursingDischargeStateMap();
        currentEncounter.setPhysicalDischarged(Boolean.TRUE);
        currentEncounter.setPhysicalDischargeDateTime(new Date());
        currentEncounter.setPhysicalDischargedBy(sessionController.getLoggedUser());
        patientEncounterFacade.edit(currentEncounter);
        auditService.logEncounterAudit(currentEncounter, "Physical Discharge",
                before, nursingDischargeStateMap(), sessionController.getLoggedUser());
        notificationController.createNotification(currentEncounter, "PhysicalDischarge");
        JsfUtil.addSuccessMessage("Physical discharge confirmed. Patient has left the hospital.");
    }

    public void cancelPhysicalDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (!currentEncounter.isPhysicalDischarged()) {
            JsfUtil.addErrorMessage("Physical discharge has not been confirmed.");
            return;
        }
        Map<String, Object> before = nursingDischargeStateMap();
        currentEncounter.setPhysicalDischarged(Boolean.FALSE);
        currentEncounter.setPhysicalDischargeDateTime(null);
        currentEncounter.setPhysicalDischargedBy(null);
        patientEncounterFacade.edit(currentEncounter);
        auditService.logEncounterAudit(currentEncounter, "Physical Discharge Cancelled",
                before, nursingDischargeStateMap(), sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage("Physical discharge cancelled.");
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public PatientEncounter getCurrentEncounter() {
        return currentEncounter;
    }

    public void setCurrentEncounter(PatientEncounter currentEncounter) {
        this.currentEncounter = currentEncounter;
    }

    public List<PendingPharmacyItemDTO> getPendingPharmacyItems() {
        return pendingPharmacyItems;
    }
}
