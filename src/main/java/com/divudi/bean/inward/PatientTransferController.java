package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.inward.TheatreOccupancyStatus;
import com.divudi.core.data.inward.TheatreTransferType;
import com.divudi.core.data.inward.TransferRequestStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.PatientTransferRequestFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PatientTransferController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;
    @Inject
    private RoomFacilityChargeController roomFacilityChargeController;
    @Inject
    private InwardBeanController inwardBean;
    @Inject
    private AdmissionController admissionController;

    @EJB
    private AdmissionFacade admissionFacade;
    @EJB
    private PatientTransferRequestFacade patientTransferRequestFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private BillFacade billFacade;

    private Admission current;
    private Institution institution;
    private RoomFacilityCharge targetRoomFacilityCharge;
    private String notes;
    private List<PatientTransferRequest> pendingRequests;
    private Date acceptedAt;
    private PatientTransferRequest lastInitiatedRequest;

    // All transfer requests for the currently selected patient
    private List<PatientTransferRequest> currentAdmissionRequests;

    // Theatre workflow fields
    private Bill selectedSurgeryBill;
    private List<Bill> surgeryBillsForCurrentAdmission;
    private List<RoomFacilityCharge> theatreRoomsForCurrentInstitution;
    private List<PatientTransferRequest> pendingTheatreRequests;
    private List<PatientTransferRequest> inTheatreRequests;
    private List<PatientTransferRequest> pendingReturnRequests;

    public List<Admission> completePatientByInstitution(String query) {
        return admissionController.completePatientNotFinalizedByInstitution(query, getInstitution());
    }

    public void onInstitutionChange() {
        current = null;
        targetRoomFacilityCharge = null;
        notes = null;
        lastInitiatedRequest = null;
        currentAdmissionRequests = null;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String navigateToInitiateTransfer() {
        current = null;
        targetRoomFacilityCharge = null;
        notes = null;
        lastInitiatedRequest = null;
        currentAdmissionRequests = null;
        institution = sessionController.getInstitution();
        return "/inward/inward_transfer_initiate?faces-redirect=true";
    }

    public String navigateToInitiateTransferForAdmission(Admission admission) {
        current = admission;
        targetRoomFacilityCharge = null;
        notes = null;
        lastInitiatedRequest = null;
        currentAdmissionRequests = null;
        onPatientSelected();
        return "/inward/inward_transfer_initiate?faces-redirect=true";
    }

    public String navigateToPatientAccept() {
        loadPendingForDepartment();
        loadPendingReturnsForWard();
        return "/inward/inward_patient_accept?faces-redirect=true";
    }

    /**
     * Called when the user selects a patient. Loads all transfer requests for
     * that admission so the page can show history and evaluate blocking rules.
     */
    public void onPatientSelected() {
        lastInitiatedRequest = null;
        targetRoomFacilityCharge = null;
        notes = null;
        loadCurrentAdmissionRequests();
    }

    private void loadCurrentAdmissionRequests() {
        if (current == null) {
            currentAdmissionRequests = new ArrayList<>();
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("admission", current);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.admission = :admission "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt DESC";
        currentAdmissionRequests = patientTransferRequestFacade.findByJpql(jpql, params);
        if (currentAdmissionRequests == null) {
            currentAdmissionRequests = new ArrayList<>();
        }
    }

    /**
     * True when there is a PENDING transfer request for the current patient.
     * In this state the user must cancel it before creating a new one.
     */
    public boolean isHasPendingRequest() {
        if (currentAdmissionRequests == null) {
            return false;
        }
        return currentAdmissionRequests.stream()
                .anyMatch(r -> r.getStatus() == TransferRequestStatus.PENDING);
    }

    /**
     * Returns the PENDING request for the current patient, or null.
     */
    public PatientTransferRequest getPendingRequestForCurrent() {
        if (currentAdmissionRequests == null) {
            return null;
        }
        return currentAdmissionRequests.stream()
                .filter(r -> r.getStatus() == TransferRequestStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    /**
     * True when the patient's current room belongs to a DIFFERENT department
     * than the logged-in user — meaning the patient has already moved away and
     * this ward cannot initiate another transfer.
     */
    public boolean isPatientInDifferentDepartment() {
        if (current == null) {
            return false;
        }
        PatientRoom currentRoom = current.getCurrentPatientRoom();
        if (currentRoom == null || currentRoom.getRoomFacilityCharge() == null) {
            return false;
        }
        Department patientDept = currentRoom.getRoomFacilityCharge().getDepartment();
        if (patientDept == null || patientDept.getId() == null) {
            return false;
        }
        Department userDept = sessionController.getDepartment();
        if (userDept == null || userDept.getId() == null) {
            // Cannot determine user's department — allow transfer
            return false;
        }
        return !patientDept.getId().equals(userDept.getId());
    }

    /**
     * True when the initiate-transfer form should be shown and usable.
     * Blocked when: patient is in another department, or there is already a
     * PENDING request that must be resolved first.
     */
    public boolean isCanInitiateTransfer() {
        if (current == null) {
            return false;
        }
        if (isPatientInDifferentDepartment()) {
            return false;
        }
        if (isHasPendingRequest()) {
            return false;
        }
        return true;
    }

    public void initiateTransfer() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select a patient first.");
            return;
        }
        if (targetRoomFacilityCharge == null) {
            JsfUtil.addErrorMessage("Please select a target room.");
            return;
        }
        // Re-query DB to prevent race conditions with concurrent sessions
        loadCurrentAdmissionRequests();
        if (!isCanInitiateTransfer()) {
            JsfUtil.addErrorMessage("Transfer cannot be initiated for this patient.");
            return;
        }
        // Reject same-room transfers
        PatientRoom currentRoom = current.getCurrentPatientRoom();
        if (currentRoom != null
                && currentRoom.getRoomFacilityCharge() != null
                && currentRoom.getRoomFacilityCharge().getId() != null
                && targetRoomFacilityCharge.getId() != null
                && currentRoom.getRoomFacilityCharge().getId().equals(targetRoomFacilityCharge.getId())) {
            JsfUtil.addErrorMessage("Please select a different target room.");
            return;
        }

        PatientTransferRequest req = new PatientTransferRequest();
        req.setAdmission(current);
        req.setFromPatientRoom(current.getCurrentPatientRoom());
        req.setToRoomFacilityCharge(targetRoomFacilityCharge);
        req.setStatus(TransferRequestStatus.PENDING);
        req.setInitiatedAt(new Date());
        req.setInitiatedBy(sessionController.getLoggedUser());
        req.setCreatedAt(new Date());
        req.setCreater(sessionController.getLoggedUser());
        req.setNotes(notes);
        patientTransferRequestFacade.create(req);
        lastInitiatedRequest = req;

        JsfUtil.addSuccessMessage("Transfer initiated successfully.");
        targetRoomFacilityCharge = null;
        notes = null;
        loadCurrentAdmissionRequests();
    }

    /**
     * Cancel a pending request from the initiate page (so the user can then
     * choose a different room and re-initiate).
     */
    public void cancelPendingAndReopen(PatientTransferRequest req) {
        if (req == null || req.getId() == null) {
            return;
        }
        // Re-fetch from DB to avoid acting on a stale session copy
        PatientTransferRequest persisted = patientTransferRequestFacade.find(req.getId());
        if (persisted == null || persisted.getStatus() != TransferRequestStatus.PENDING) {
            lastInitiatedRequest = null;
            loadCurrentAdmissionRequests();
            JsfUtil.addErrorMessage("This transfer request is no longer pending and cannot be cancelled.");
            return;
        }
        persisted.setStatus(TransferRequestStatus.CANCELLED);
        persisted.setCancelledAt(new Date());
        persisted.setCancelledBy(sessionController.getLoggedUser());
        patientTransferRequestFacade.edit(persisted);
        lastInitiatedRequest = null;
        loadCurrentAdmissionRequests();
        JsfUtil.addSuccessMessage("Transfer request cancelled. You may now initiate a new one.");
    }

    public void startNewTransfer() {
        lastInitiatedRequest = null;
        targetRoomFacilityCharge = null;
        notes = null;
    }

    public void acceptTransfer(PatientTransferRequest req) {
        if (req == null) {
            return;
        }
        if (req.getTheatreTransferType() == TheatreTransferType.SEND_TO_THEATRE) {
            JsfUtil.addErrorMessage("This is a theatre transfer request. Use \"Accept Theatre Patient\" instead.");
            loadPendingForDepartment();
            return;
        }

        Date effectiveAt = req.getAcceptedAt() != null ? req.getAcceptedAt() : new Date();
        req.setAcceptedAt(effectiveAt);

        if (req.getFromPatientRoom() == null) {
            // Admission handover — mark room as admitted
            req.getAdmission().setRoomAdmitted(true);
            admissionFacade.edit(req.getAdmission());
        } else {
            // Ward-to-ward transfer
            PatientRoom fromPatientRoom = req.getFromPatientRoom();
            fromPatientRoom.setDischarged(true);
            fromPatientRoom.setDischargedAt(effectiveAt);
            fromPatientRoom.setDischargedBy(sessionController.getLoggedUser());
            patientRoomFacade.edit(fromPatientRoom);

            PatientRoom newPatientRoom = new PatientRoom();
            newPatientRoom = inwardBean.savePatientRoom(
                    newPatientRoom,
                    fromPatientRoom,
                    req.getToRoomFacilityCharge(),
                    req.getAdmission(),
                    effectiveAt,
                    sessionController.getLoggedUser());

            fromPatientRoom.setNextRoom(newPatientRoom);
            patientRoomFacade.edit(fromPatientRoom);

            req.getAdmission().setCurrentPatientRoom(newPatientRoom);
            admissionFacade.edit(req.getAdmission());
        }

        req.setStatus(TransferRequestStatus.ACCEPTED);
        req.setAcceptedBy(sessionController.getLoggedUser());
        patientTransferRequestFacade.edit(req);

        loadPendingForDepartment();
        JsfUtil.addSuccessMessage("Patient accepted successfully.");
    }

    public void cancelTransfer(PatientTransferRequest req) {
        if (req == null) {
            return;
        }
        req.setStatus(TransferRequestStatus.CANCELLED);
        req.setCancelledAt(new Date());
        req.setCancelledBy(sessionController.getLoggedUser());
        patientTransferRequestFacade.edit(req);
        loadPendingForDepartment();
        JsfUtil.addSuccessMessage("Transfer request cancelled.");
    }

    /**
     * True if there are any PENDING transfer requests targeting the logged-in
     * user's department. Used to enable/disable the Accept Patients button.
     */
    public boolean isHasPendingRequestsForDepartment() {
        Department userDept = sessionController.getDepartment();
        if (userDept == null) {
            return false;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("status", TransferRequestStatus.PENDING);
        params.put("department", userDept);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :status "
                + "AND r.toRoomFacilityCharge.department = :department "
                + "AND r.theatreTransferType IS NULL "
                + "AND r.retired = false";
        List<PatientTransferRequest> result = patientTransferRequestFacade.findByJpql(jpql, params, 1);
        return result != null && !result.isEmpty();
    }

    /**
     * True if there are any PENDING theatre-bound (SEND_TO_THEATRE) transfer
     * requests targeting the logged-in user's department. Used to enable/
     * disable the Accept Theatre Patient button.
     */
    public boolean isHasPendingTheatreRequestsForDepartment() {
        Department userDept = sessionController.getDepartment();
        if (userDept == null) {
            return false;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("status", TransferRequestStatus.PENDING);
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("department", userDept);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :status "
                + "AND r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department = :department "
                + "AND r.retired = false";
        List<PatientTransferRequest> result = patientTransferRequestFacade.findByJpql(jpql, params, 1);
        return result != null && !result.isEmpty();
    }

    public void loadPendingForDepartment() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("status", TransferRequestStatus.PENDING);
        params.put("department", sessionController.getDepartment());
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :status "
                + "AND r.toRoomFacilityCharge.department = :department "
                + "AND r.theatreTransferType IS NULL "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt";
        pendingRequests = patientTransferRequestFacade.findByJpql(jpql, params);
        if (pendingRequests == null) {
            pendingRequests = new ArrayList<>();
        }
    }

    public Date getAcceptedAt() {
        if (acceptedAt == null) {
            acceptedAt = new Date();
        }
        return acceptedAt;
    }

    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Admission getCurrent() {
        return current;
    }

    public void setCurrent(Admission current) {
        this.current = current;
    }

    public RoomFacilityCharge getTargetRoomFacilityCharge() {
        return targetRoomFacilityCharge;
    }

    public void setTargetRoomFacilityCharge(RoomFacilityCharge targetRoomFacilityCharge) {
        this.targetRoomFacilityCharge = targetRoomFacilityCharge;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PatientTransferRequest> getPendingRequests() {
        if (pendingRequests == null) {
            pendingRequests = new ArrayList<>();
        }
        return pendingRequests;
    }

    public void setPendingRequests(List<PatientTransferRequest> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public PatientTransferRequest getLastInitiatedRequest() {
        return lastInitiatedRequest;
    }

    public void setLastInitiatedRequest(PatientTransferRequest lastInitiatedRequest) {
        this.lastInitiatedRequest = lastInitiatedRequest;
    }

    public List<PatientTransferRequest> getCurrentAdmissionRequests() {
        if (currentAdmissionRequests == null) {
            currentAdmissionRequests = new ArrayList<>();
        }
        return currentAdmissionRequests;
    }

    /**
     * Returns the single PENDING request as a one-element list for datatable binding,
     * or empty list if none.
     */
    public List<PatientTransferRequest> getPendingRequestForCurrentAsList() {
        PatientTransferRequest req = getPendingRequestForCurrent();
        List<PatientTransferRequest> list = new ArrayList<>();
        if (req != null) {
            list.add(req);
        }
        return list;
    }

    // =========================================================================
    // Theatre Workflow Methods
    // =========================================================================

    public String navigateToSendToTheatre(Admission admission) {
        current = admission;
        targetRoomFacilityCharge = null;
        selectedSurgeryBill = null;
        notes = null;
        surgeryBillsForCurrentAdmission = loadSurgeryBillsForAdmission(admission);
        theatreRoomsForCurrentInstitution = roomFacilityChargeController.findTheatreRoomsForInstitution(sessionController.getInstitution());
        return "/inward/inward_send_to_theatre?faces-redirect=true";
    }

    public String navigateToTheatreAccept() {
        loadPendingForTheatre();
        loadInTheatreRequests();
        return "/theater/theatre_patient_accept?faces-redirect=true";
    }

    public void sendToTheatre() {
        if (current == null) {
            JsfUtil.addErrorMessage("No patient selected.");
            return;
        }
        if (targetRoomFacilityCharge == null) {
            JsfUtil.addErrorMessage("Please select a theatre room.");
            return;
        }
        PatientTransferRequest existing = findActiveSendToTheatreRequest(current);
        if (existing != null) {
            JsfUtil.addErrorMessage("This patient already has an active theatre transfer in progress.");
            return;
        }
        PatientTransferRequest req = new PatientTransferRequest();
        req.setAdmission(current);
        req.setFromPatientRoom(current.getCurrentPatientRoom());
        req.setToRoomFacilityCharge(targetRoomFacilityCharge);
        req.setTheatreTransferType(TheatreTransferType.SEND_TO_THEATRE);
        req.setTheatreOccupancyStatus(TheatreOccupancyStatus.SENT_TO_THEATRE);
        req.setSurgeryBill(selectedSurgeryBill);
        req.setStatus(TransferRequestStatus.PENDING);
        req.setNotes(notes);
        req.setInitiatedAt(new Date());
        req.setInitiatedBy(sessionController.getLoggedUser());
        req.setCreatedAt(new Date());
        req.setCreater(sessionController.getLoggedUser());
        patientTransferRequestFacade.create(req);
        lastInitiatedRequest = req;
        JsfUtil.addSuccessMessage("Patient sent to theatre successfully.");
        targetRoomFacilityCharge = null;
        selectedSurgeryBill = null;
        notes = null;
    }

    public void acceptInTheatre(PatientTransferRequest req) {
        if (req == null || req.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(req.getId());
        if (persisted == null || persisted.getStatus() != TransferRequestStatus.PENDING) {
            JsfUtil.addErrorMessage("This request is no longer pending.");
            loadPendingForTheatre();
            return;
        }
        persisted.setStatus(TransferRequestStatus.ACCEPTED);
        persisted.setAcceptedAt(new Date());
        persisted.setAcceptedBy(sessionController.getLoggedUser());
        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.RECEIVED_IN_THEATRE);
        patientTransferRequestFacade.edit(persisted);
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Patient accepted in theatre.");
    }

    public void markInTheatre(PatientTransferRequest req) {
        if (req == null || req.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(req.getId());
        if (persisted == null) {
            return;
        }
        if (persisted.getTheatreOccupancyStatus() != TheatreOccupancyStatus.RECEIVED_IN_THEATRE) {
            JsfUtil.addErrorMessage("Patient must be in RECEIVED_IN_THEATRE status to mark as In Theatre.");
            return;
        }
        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.IN_THEATRE);
        if (persisted.getProcedureStartAt() == null) {
            persisted.setProcedureStartAt(new Date());
        }
        patientTransferRequestFacade.edit(persisted);
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Patient marked as In Theatre.");
    }

    public void markProcedureCompleted(PatientTransferRequest req) {
        if (req == null || req.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(req.getId());
        if (persisted == null) {
            return;
        }
        if (persisted.getTheatreOccupancyStatus() != TheatreOccupancyStatus.IN_THEATRE) {
            JsfUtil.addErrorMessage("Patient must be IN_THEATRE status to mark procedure as completed.");
            return;
        }
        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.PROCEDURE_COMPLETED);
        if (persisted.getProcedureEndAt() == null) {
            persisted.setProcedureEndAt(new Date());
        }
        patientTransferRequestFacade.edit(persisted);
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Procedure marked as completed.");
    }

    public void returnToWard(PatientTransferRequest theatreReq) {
        if (theatreReq == null || theatreReq.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(theatreReq.getId());
        if (persisted == null) {
            return;
        }
        TheatreOccupancyStatus currentStatus = persisted.getTheatreOccupancyStatus();
        if (currentStatus != TheatreOccupancyStatus.PROCEDURE_COMPLETED
                && currentStatus != TheatreOccupancyStatus.IN_RECOVERY) {
            JsfUtil.addErrorMessage("Patient must have completed the procedure before returning to ward.");
            return;
        }
        if (persisted.getFromPatientRoom() == null || persisted.getFromPatientRoom().getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Cannot determine the ward room to return patient to.");
            return;
        }
        PatientTransferRequest returnReq = new PatientTransferRequest();
        returnReq.setAdmission(persisted.getAdmission());
        returnReq.setFromPatientRoom(persisted.getFromPatientRoom());
        returnReq.setToRoomFacilityCharge(persisted.getFromPatientRoom().getRoomFacilityCharge());
        returnReq.setTheatreTransferType(TheatreTransferType.RETURN_TO_WARD);
        returnReq.setSurgeryBill(persisted.getSurgeryBill());
        returnReq.setStatus(TransferRequestStatus.PENDING);
        returnReq.setNotes(notes);
        returnReq.setInitiatedAt(new Date());
        returnReq.setInitiatedBy(sessionController.getLoggedUser());
        returnReq.setCreatedAt(new Date());
        returnReq.setCreater(sessionController.getLoggedUser());
        patientTransferRequestFacade.create(returnReq);

        persisted.setTheatreOccupancyStatus(TheatreOccupancyStatus.RETURNED_TO_WARD);
        if (persisted.getReturnedToWardAt() == null) {
            persisted.setReturnedToWardAt(new Date());
        }
        patientTransferRequestFacade.edit(persisted);

        notes = null;
        loadPendingForTheatre();
        loadInTheatreRequests();
        JsfUtil.addSuccessMessage("Patient return to ward initiated.");
    }

    public void acceptReturnToWard(PatientTransferRequest returnReq) {
        if (returnReq == null || returnReq.getId() == null) {
            return;
        }
        PatientTransferRequest persisted = patientTransferRequestFacade.find(returnReq.getId());
        if (persisted == null || persisted.getStatus() != TransferRequestStatus.PENDING) {
            JsfUtil.addErrorMessage("This return request is no longer pending.");
            loadPendingReturnsForWard();
            return;
        }
        persisted.setStatus(TransferRequestStatus.ACCEPTED);
        persisted.setAcceptedAt(new Date());
        persisted.setAcceptedBy(sessionController.getLoggedUser());
        patientTransferRequestFacade.edit(persisted);

        // Update the master SEND_TO_THEATRE record's returnedToWardAt if not already set
        PatientTransferRequest theatreReq = findActiveSendToTheatreRequestForReturn(persisted);
        if (theatreReq != null && theatreReq.getReturnedToWardAt() == null) {
            theatreReq.setReturnedToWardAt(new Date());
            theatreReq.setTheatreOccupancyStatus(TheatreOccupancyStatus.RETURNED_TO_WARD);
            patientTransferRequestFacade.edit(theatreReq);
        }

        loadPendingReturnsForWard();
        JsfUtil.addSuccessMessage("Patient accepted back from theatre.");
    }

    public void loadPendingForTheatre() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("status", TransferRequestStatus.PENDING);
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("department", sessionController.getDepartment());
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :status "
                + "AND r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department = :department "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt";
        pendingTheatreRequests = patientTransferRequestFacade.findByJpql(jpql, params);
        if (pendingTheatreRequests == null) {
            pendingTheatreRequests = new ArrayList<>();
        }
    }

    public void loadInTheatreRequests() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("accepted", TransferRequestStatus.ACCEPTED);
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("department", sessionController.getDepartment());
        params.put("returned", TheatreOccupancyStatus.RETURNED_TO_WARD);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :accepted "
                + "AND r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department = :department "
                + "AND (r.theatreOccupancyStatus IS NULL OR r.theatreOccupancyStatus <> :returned) "
                + "AND r.retired = false "
                + "ORDER BY r.acceptedAt";
        inTheatreRequests = patientTransferRequestFacade.findByJpql(jpql, params);
        if (inTheatreRequests == null) {
            inTheatreRequests = new ArrayList<>();
        }
    }

    public void loadPendingReturnsForWard() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("status", TransferRequestStatus.PENDING);
        params.put("type", TheatreTransferType.RETURN_TO_WARD);
        params.put("department", sessionController.getDepartment());
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :status "
                + "AND r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department = :department "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt";
        pendingReturnRequests = patientTransferRequestFacade.findByJpql(jpql, params);
        if (pendingReturnRequests == null) {
            pendingReturnRequests = new ArrayList<>();
        }
    }

    public PatientTransferRequest findActiveSendToTheatreRequest(Admission admission) {
        if (admission == null) {
            return null;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("admission", admission);
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("returned", TheatreOccupancyStatus.RETURNED_TO_WARD);
        params.put("cancelled", TransferRequestStatus.CANCELLED);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.admission = :admission "
                + "AND r.theatreTransferType = :type "
                + "AND r.status <> :cancelled "
                + "AND (r.theatreOccupancyStatus IS NULL OR r.theatreOccupancyStatus <> :returned) "
                + "AND r.retired = false "
                + "ORDER BY r.createdAt DESC";
        List<PatientTransferRequest> results = patientTransferRequestFacade.findByJpql(jpql, params, 1);
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    private PatientTransferRequest findActiveSendToTheatreRequestForReturn(PatientTransferRequest returnReq) {
        if (returnReq == null || returnReq.getAdmission() == null) {
            return null;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("admission", returnReq.getAdmission());
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("accepted", TransferRequestStatus.ACCEPTED);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.admission = :admission "
                + "AND r.theatreTransferType = :type "
                + "AND r.status = :accepted "
                + "AND r.retired = false "
                + "ORDER BY r.createdAt DESC";
        List<PatientTransferRequest> results = patientTransferRequestFacade.findByJpql(jpql, params, 1);
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    private List<Bill> loadSurgeryBillsForAdmission(Admission admission) {
        if (admission == null) {
            return new ArrayList<>();
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("admission", admission);
        params.put("billType", BillType.SurgeryBill);
        String jpql = "SELECT b FROM Bill b "
                + "WHERE b.patientEncounter = :admission "
                + "AND b.billType = :billType "
                + "AND b.cancelled = false "
                + "AND b.retired = false "
                + "ORDER BY b.createdAt";
        List<Bill> result = billFacade.findByJpql(jpql, params);
        return result != null ? result : new ArrayList<>();
    }

    public List<Bill> getSurgeryBillsForCurrentAdmission() {
        if (surgeryBillsForCurrentAdmission == null) {
            surgeryBillsForCurrentAdmission = new ArrayList<>();
        }
        return surgeryBillsForCurrentAdmission;
    }

    public List<RoomFacilityCharge> getTheatreRoomsForCurrentInstitution() {
        if (theatreRoomsForCurrentInstitution == null) {
            theatreRoomsForCurrentInstitution = new ArrayList<>();
        }
        return theatreRoomsForCurrentInstitution;
    }

    public Bill getSelectedSurgeryBill() {
        return selectedSurgeryBill;
    }

    public void setSelectedSurgeryBill(Bill selectedSurgeryBill) {
        this.selectedSurgeryBill = selectedSurgeryBill;
    }

    public List<PatientTransferRequest> getPendingTheatreRequests() {
        if (pendingTheatreRequests == null) {
            pendingTheatreRequests = new ArrayList<>();
        }
        return pendingTheatreRequests;
    }

    public List<PatientTransferRequest> getPendingReturnRequests() {
        if (pendingReturnRequests == null) {
            pendingReturnRequests = new ArrayList<>();
        }
        return pendingReturnRequests;
    }

    public List<PatientTransferRequest> getInTheatreRequests() {
        if (inTheatreRequests == null) {
            inTheatreRequests = new ArrayList<>();
        }
        return inTheatreRequests;
    }

    // -----------------------------------------------------------------------
    // Milestone stepper helpers — called from XHTML to colour each step circle
    // Visual steps: 1=Sent, 2=Received, 3=In OT, 4=Proc Done, 5=Returned
    // -----------------------------------------------------------------------

    public String theatreStepBg(TheatreOccupancyStatus status, int visualStep) {
        if (status == null) {
            return "#dee2e6";
        }
        int s = status.getStepNumber();
        int needed = theatreVisualToEnumStep(visualStep);
        if (needed == 0) {
            return "#dee2e6";
        }
        if (s > needed) {
            return "#198754"; // completed: green
        }
        if (s == needed) {
            return "#fd7e14"; // current: amber
        }
        return "#dee2e6";     // future: light grey
    }

    public String theatreStepColor(TheatreOccupancyStatus status, int visualStep) {
        if (status == null) {
            return "#9ca3af";
        }
        int s = status.getStepNumber();
        int needed = theatreVisualToEnumStep(visualStep);
        return (needed > 0 && s >= needed) ? "#fff" : "#9ca3af";
    }

    public String theatreLineBg(TheatreOccupancyStatus status, int afterVisualStep) {
        if (status == null) {
            return "#dee2e6";
        }
        int s = status.getStepNumber();
        int needed = theatreVisualToEnumStep(afterVisualStep);
        return (needed > 0 && s > needed) ? "#198754" : "#dee2e6";
    }

    private int theatreVisualToEnumStep(int visualStep) {
        switch (visualStep) {
            case 1: return 2; // SENT_TO_THEATRE
            case 2: return 3; // RECEIVED_IN_THEATRE
            case 3: return 4; // IN_THEATRE
            case 4: return 5; // PROCEDURE_COMPLETED
            case 5: return 7; // RETURNED_TO_WARD
            default: return 0;
        }
    }

    // -----------------------------------------------------------------------
    // OT duration: formatted as "1h 35m" from procedureStartAt to procedureEndAt
    // (or to now when still in progress)
    // -----------------------------------------------------------------------

    public String formatOtDuration(PatientTransferRequest req) {
        if (req == null || req.getProcedureStartAt() == null) {
            return "—";
        }
        java.util.Date end = req.getProcedureEndAt() != null ? req.getProcedureEndAt() : new java.util.Date();
        long totalMinutes = (end.getTime() - req.getProcedureStartAt().getTime()) / 60_000L;
        if (totalMinutes < 0) {
            totalMinutes = 0;
        }
        return String.format("%dh %02dm", totalMinutes / 60, totalMinutes % 60);
    }
}
