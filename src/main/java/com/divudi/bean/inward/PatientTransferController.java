package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.inward.TransferRequestStatus;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.AdmissionFacade;
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

    @EJB
    private AdmissionFacade admissionFacade;
    @EJB
    private PatientTransferRequestFacade patientTransferRequestFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;

    private Admission current;
    private RoomFacilityCharge targetRoomFacilityCharge;
    private String notes;
    private List<PatientTransferRequest> pendingRequests;
    private Date acceptedAt;
    private PatientTransferRequest lastInitiatedRequest;

    // All transfer requests for the currently selected patient
    private List<PatientTransferRequest> currentAdmissionRequests;

    public String navigateToInitiateTransfer() {
        current = null;
        targetRoomFacilityCharge = null;
        notes = null;
        lastInitiatedRequest = null;
        currentAdmissionRequests = null;
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
        if (!isCanInitiateTransfer()) {
            JsfUtil.addErrorMessage("Transfer cannot be initiated for this patient.");
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
        if (req == null) {
            return;
        }
        req.setStatus(TransferRequestStatus.CANCELLED);
        req.setCancelledAt(new Date());
        req.setCancelledBy(sessionController.getLoggedUser());
        patientTransferRequestFacade.edit(req);
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
}
