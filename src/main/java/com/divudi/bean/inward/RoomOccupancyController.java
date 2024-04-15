/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.entity.inward.RoomFacilityCharge;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.RoomFacade;
import com.divudi.facade.RoomFacilityChargeFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
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
    CommonController commonController;

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    RoomFacade roomFacade;
    @EJB
    RoomFacilityChargeFacade roomFacilityChargeFacade;
    private List<PatientRoom> patientRooms;
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
        createPatientRoom();
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
        createPatientRoom();
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
        createPatientRoom();
    }

    public void createPatientRoom() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.admissionType.roomChargesAllowed=true "
                + " and pr.discharged=false "
                + " order by pr.roomFacilityCharge.name";

        patientRooms = getPatientRoomFacade().findByJpql(sql);

        

    }

    public void createPatientRoomVacant() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        String sql = "SELECT rf FROM RoomFacilityCharge rf "
                + " where rf.retired=false "
                + " and rf.room.filled=false"
                + " and rf.room.retired=false"
                + " order by rf.name";

        roomFacilityCharges = getRoomFacilityChargeFacade().findByJpql(sql);
        
    }

    public void createPatientRoomAll() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.discharged=false "
                + " order by pr.roomFacilityCharge.name";

        patientRooms = getPatientRoomFacade().findByJpql(sql);
        
        
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

    public void setRoomFacilityCharges(List<RoomFacilityCharge> roomFacilityCharges) {
        this.roomFacilityCharges = roomFacilityCharges;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
