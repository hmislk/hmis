/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.RoomFacade;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.eclipse.persistence.jpa.JpaHelper;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class RoomOccupancyController implements Serializable {

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    RoomFacade roomFacade;
    private List<PatientRoom> patientRooms;

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

    public void createPatientRoom() {
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.admissionType.roomChargesAllowed=true "
                + " and pr.discharged=false "
                + " order by pr.roomFacilityCharge.name";

        patientRooms = getPatientRoomFacade().findBySQL(sql);

    }
    
    public void createPatientRoomVacant() {
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.discharged=true "
                + " order by pr.roomFacilityCharge.name";

        patientRooms = getPatientRoomFacade().findBySQL(sql);

    }
    
      public void createPatientRoomAll() {
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.discharged=false "
                + " order by pr.roomFacilityCharge.name";

        patientRooms = getPatientRoomFacade().findBySQL(sql);

    }

    public List<PatientRoom> getPatientRooms() {

        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }
}
