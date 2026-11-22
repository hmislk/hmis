/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.RoomFacade;
import com.divudi.core.facade.RoomFacilityChargeFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class RoomOccupancyController implements Serializable {

    @Inject
    NotificationController notificationController;

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    RoomFacade roomFacade;
    @EJB
    RoomFacilityChargeFacade roomFacilityChargeFacade;
    @Inject
    WebUserController webUserController;

    private List<PatientRoom> patientRooms;
    private Institution institution;
    private boolean roomChargeTypesOnly;
    List<RoomFacilityCharge> roomFacilityCharges;

    public RoomFacade getRoomFacade() {
        return roomFacade;
    }

    public void setRoomFacade(RoomFacade roomFacade) {
        this.roomFacade = roomFacade;
    }

    /**
     * Creates a new instance of RoomOccupancyController
     */
    public RoomOccupancyController() {
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    @Inject
    SessionController sessionController;

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void update(PatientRoom patientRoom) {
        patientRoom.setDischargedBy(getSessionController().getLoggedUser());
        getPatientRoomFacade().edit(patientRoom);
        refresh();
    }

    public void dischargeFromRoom(PatientRoom patientRoom) {
        if (patientRoom == null) {
            JsfUtil.addErrorMessage("No room");
            return;
        }
        if (patientRoom.isDischarged()) {
            JsfUtil.addErrorMessage("Already Discharged");
            return;
        }
        patientRoom.setDischarged(true);
        JsfUtil.addSuccessMessage("Successfully Discharged");
        patientRoom.setDischargedAt(new Date());
        patientRoom.setDischargedBy(getSessionController().getLoggedUser());
        getPatientRoomFacade().edit(patientRoom);
        refresh();
        notificationController.createNotification(patientRoom,"Discharge");
    }

    public void dischargeCancelFromRoom(PatientRoom patientRoom) {
        if (patientRoom == null) {
            JsfUtil.addErrorMessage("No room");
            return;
        }
        if (!patientRoom.isDischarged()) {
            JsfUtil.addErrorMessage("Already Discharged");
            return;
        }
        patientRoom.setDischarged(false);
        patientRoom.setDischargedAt(null);
        patientRoom.setDischargedBy(null);
        getPatientRoomFacade().edit(patientRoom);
        refresh();
    }

    /**
     * Called from the menu: resets the filter to the user's own institution and
     * loads every occupied room, so the page opens with data.
     */
    public void openRoomOccupancy() {
        institution = sessionController.getInstitution();
        roomChargeTypesOnly = false;
        loadPatientRooms();
    }

    /**
     * Reloads the list in the mode last chosen (used on filter change and after
     * a room discharge).
     */
    public void refresh() {
        loadPatientRooms();
    }

    public boolean isAnyInstitutionAllowed() {
        return webUserController.hasPrivilege("InwardRoomOccupancyAnyInstitute");
    }

    /**
     * The institution actually applied. A user without the Any Institute
     * privilege is always limited to their own institution, whatever the page
     * sends.
     */
    private Institution effectiveInstitution() {
        if (!isAnyInstitutionAllowed()) {
            return sessionController.getInstitution();
        }
        return institution;
    }

    public void showAllInstitutions() {
        institution = null;
        loadPatientRooms();
    }

    public void createPatientRoom() {
        roomChargeTypesOnly = true;
        loadPatientRooms();
    }

    public void createPatientRoomVacant() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        String sql = "SELECT rf FROM RoomFacilityCharge rf "
                + " where rf.retired=false "
                + " and rf.room.filled!=true"
                + " and rf.room.retired=false"
                + " and rf.room.id NOT IN ("
                + " SELECT pr.roomFacilityCharge.room.id"
                + " FROM PatientRoom pr"
                + " WHERE pr.retired=false"
                + " AND pr.discharged=false)"
                + " order by rf.name";

        roomFacilityCharges = getRoomFacilityChargeFacade().findByJpql(sql);

    }

    public void createPatientRoomAll() {
        roomChargeTypesOnly = false;
        loadPatientRooms();
    }

    /**
     * One row per patient in a room (a second patient placed in the same room
     * is listed as its own row; nothing here collapses rows by room).
     */
    private void loadPatientRooms() {
        Map<String, Object> m = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT pr FROM PatientRoom pr "
                + " join pr.patientEncounter pe "
                + " where pr.retired=false "
                + " and pr.discharged=false ");
        if (roomChargeTypesOnly) {
            sql.append(" and pe.admissionType.roomChargesAllowed=true ");
        }
        Institution ins = effectiveInstitution();
        if (ins != null) {
            sql.append(" and pe.institution=:ins ");
            m.put("ins", ins);
        } else if (!isAnyInstitutionAllowed()) {
            // No privilege and no login institution: show nothing, never everything.
            patientRooms = new ArrayList<>();
            JsfUtil.addErrorMessage("Your login has no institution; ask an administrator");
            return;
        }
        sql.append(" order by pr.roomFacilityCharge.name");
        patientRooms = getPatientRoomFacade().findByJpql(sql.toString(), m);
    }

    public int getRowCount() {
        return patientRooms == null ? 0 : patientRooms.size();
    }

    public boolean isAnyGuardian() {
        if (patientRooms == null) {
            return false;
        }
        for (PatientRoom pr : patientRooms) {
            // getGuardian() never returns null (it creates an empty Person), so test the content.
            if (pr.getPatientEncounter() != null) {
                Person g = pr.getPatientEncounter().getGuardian();
                if (g.getId() != null
                        || (g.getName() != null && !g.getName().trim().isEmpty())
                        || (g.getPhone() != null && !g.getPhone().trim().isEmpty())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAnyCreditCompany() {
        if (patientRooms == null) {
            return false;
        }
        for (PatientRoom pr : patientRooms) {
            if (pr.getPatientEncounter() != null && pr.getPatientEncounter().getCreditCompany() != null) {
                return true;
            }
        }
        return false;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public boolean isRoomChargeTypesOnly() {
        return roomChargeTypesOnly;
    }

    public List<PatientRoom> getPatientRooms() {

        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

    public RoomFacilityChargeFacade getRoomFacilityChargeFacade() {
        return roomFacilityChargeFacade;
    }

    public void setRoomFacilityChargeFacade(RoomFacilityChargeFacade roomFacilityChargeFacade) {
        this.roomFacilityChargeFacade = roomFacilityChargeFacade;
    }

    public List<RoomFacilityCharge> getRoomFacilityCharges() {
        return roomFacilityCharges;
    }

    /**
     * What a full day in this room costs, for the vacant-room list. The room
     * charge is per configured block, and the block can be defined in minutes,
     * hours or days — so it is scaled by however many blocks fit in 24 hours. A
     * one-time charge is not time-based and is shown as-is.
     */
    public double getDailyEquivalentRoomCharge(RoomFacilityCharge charge) {
        if (charge == null || charge.getTimedItemFee() == null || charge.getRoomCharge() == null) {
            return 0.0;
        }
        if (charge.getTimedItemFee().isOneTime()) {
            return charge.getRoomCharge();
        }
        double blockHours = charge.getTimedItemFee().getDurationInHours();
        if (blockHours <= 0) {
            return 0.0;
        }
        return charge.getRoomCharge() * (24.0 / blockHours);
    }

    public void setRoomFacilityCharges(List<RoomFacilityCharge> roomFacilityCharges) {
        this.roomFacilityCharges = roomFacilityCharges;
    }
}
