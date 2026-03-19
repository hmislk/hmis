package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.inward.TransferRequestStatus;
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
import java.util.stream.Collectors;
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

    public String navigateToInitiateTransfer() {
        current = null;
        targetRoomFacilityCharge = null;
        notes = null;
        return "/inward/inward_transfer_initiate?faces-redirect=true";
    }

    public String navigateToPatientAccept() {
        loadPendingForDepartment();
        return "/inward/inward_patient_accept?faces-redirect=true";
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

        JsfUtil.addSuccessMessage("Transfer initiated successfully.");
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

    public List<PatientTransferRequest> getPendingTransfersForCurrentBht() {
        if (current == null || pendingRequests == null) {
            return new ArrayList<>();
        }
        return pendingRequests.stream()
                .filter(r -> r.getAdmission() != null && r.getAdmission().equals(current))
                .collect(Collectors.toList());
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
}
