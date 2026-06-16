package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.inward.TheatreOccupancyStatus;
import com.divudi.core.data.inward.TheatreTransferType;
import com.divudi.core.data.inward.TransferRequestStatus;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.PatientTransferRequestFacade;
import com.divudi.core.facade.RoomFacilityChargeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class TheatreDashboardController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @Inject
    private PatientTransferController patientTransferController;

    @EJB
    private PatientTransferRequestFacade patientTransferRequestFacade;
    @EJB
    private RoomFacilityChargeFacade roomFacilityChargeFacade;

    private List<TheatreRoomCard> roomCards;
    private List<PatientTransferRequest> pendingAcceptance;
    private List<PatientTransferRequest> inTheatre;
    private List<PatientTransferRequest> pendingReturns;
    private List<PatientTransferRequest> todaysList;

    public String navigateToTheatreDashboard() {
        loadAll();
        return "/theater/theatre_dashboard?faces-redirect=true";
    }

    public void loadAll() {
        loadPendingAcceptance();
        loadInTheatre();
        loadPendingReturns();
        loadTodaysList();
        buildRoomCards();
    }

    private void loadPendingAcceptance() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("status", TransferRequestStatus.PENDING);
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("institution", sessionController.getInstitution());
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :status "
                + "AND r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department.institution = :institution "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt";
        pendingAcceptance = patientTransferRequestFacade.findByJpql(jpql, params);
        if (pendingAcceptance == null) {
            pendingAcceptance = new ArrayList<>();
        }
    }

    private void loadInTheatre() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("accepted", TransferRequestStatus.ACCEPTED);
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("institution", sessionController.getInstitution());
        params.put("returned", TheatreOccupancyStatus.RETURNED_TO_WARD);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :accepted "
                + "AND r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department.institution = :institution "
                + "AND (r.theatreOccupancyStatus IS NULL OR r.theatreOccupancyStatus <> :returned) "
                + "AND r.retired = false "
                + "ORDER BY r.acceptedAt";
        inTheatre = patientTransferRequestFacade.findByJpql(jpql, params);
        if (inTheatre == null) {
            inTheatre = new ArrayList<>();
        }
    }

    private void loadPendingReturns() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("status", TransferRequestStatus.PENDING);
        params.put("type", TheatreTransferType.RETURN_TO_WARD);
        params.put("institution", sessionController.getInstitution());
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.status = :status "
                + "AND r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department.institution = :institution "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt";
        pendingReturns = patientTransferRequestFacade.findByJpql(jpql, params);
        if (pendingReturns == null) {
            pendingReturns = new ArrayList<>();
        }
    }

    private void loadTodaysList() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date endOfDay = cal.getTime();

        HashMap<String, Object> params = new HashMap<>();
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        params.put("institution", sessionController.getInstitution());
        params.put("start", startOfDay);
        params.put("end", endOfDay);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.theatreTransferType = :type "
                + "AND r.toRoomFacilityCharge.department.institution = :institution "
                + "AND r.initiatedAt BETWEEN :start AND :end "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt";
        todaysList = patientTransferRequestFacade.findByJpql(jpql, params);
        if (todaysList == null) {
            todaysList = new ArrayList<>();
        }
    }

    private void buildRoomCards() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deptType", DepartmentType.Theatre);
        params.put("institution", sessionController.getInstitution());
        String jpql = "SELECT r FROM RoomFacilityCharge r "
                + "WHERE r.retired = false "
                + "AND r.department.departmentType = :deptType "
                + "AND r.department.institution = :institution "
                + "ORDER BY r.name";
        List<RoomFacilityCharge> theatreRooms = roomFacilityChargeFacade.findByJpql(jpql, params);
        if (theatreRooms == null) {
            theatreRooms = new ArrayList<>();
        }

        roomCards = new ArrayList<>();
        for (RoomFacilityCharge room : theatreRooms) {
            PatientTransferRequest active = findActiveRequestForRoom(room);
            roomCards.add(new TheatreRoomCard(room, active));
        }
    }

    private PatientTransferRequest findActiveRequestForRoom(RoomFacilityCharge room) {
        for (PatientTransferRequest req : inTheatre) {
            if (req.getToRoomFacilityCharge() != null
                    && room.getId() != null
                    && room.getId().equals(req.getToRoomFacilityCharge().getId())) {
                return req;
            }
        }
        return null;
    }

    // Delegate milestone actions to PatientTransferController and reload
    public void acceptInTheatre(PatientTransferRequest req) {
        patientTransferController.acceptInTheatre(req);
        loadAll();
    }

    public void markInTheatre(PatientTransferRequest req) {
        patientTransferController.markInTheatre(req);
        loadAll();
    }

    public void markProcedureCompleted(PatientTransferRequest req) {
        patientTransferController.markProcedureCompleted(req);
        loadAll();
    }

    public void returnToWard(PatientTransferRequest req) {
        patientTransferController.returnToWard(req);
        loadAll();
    }

    public void acceptReturnToWard(PatientTransferRequest req) {
        patientTransferController.acceptReturnToWard(req);
        loadAll();
    }

    public List<TheatreRoomCard> getRoomCards() {
        if (roomCards == null) {
            roomCards = new ArrayList<>();
        }
        return roomCards;
    }

    public List<PatientTransferRequest> getPendingAcceptance() {
        if (pendingAcceptance == null) {
            pendingAcceptance = new ArrayList<>();
        }
        return pendingAcceptance;
    }

    public List<PatientTransferRequest> getInTheatre() {
        if (inTheatre == null) {
            inTheatre = new ArrayList<>();
        }
        return inTheatre;
    }

    public List<PatientTransferRequest> getPendingReturns() {
        if (pendingReturns == null) {
            pendingReturns = new ArrayList<>();
        }
        return pendingReturns;
    }

    public List<PatientTransferRequest> getTodaysList() {
        if (todaysList == null) {
            todaysList = new ArrayList<>();
        }
        return todaysList;
    }
}
