package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.channel.InwardReservationEvent;

import com.divudi.entity.PatientEncounter;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.entity.inward.Reservation;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.java.CommonFunctions;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.schedule.ScheduleEntryMoveEvent;
import org.primefaces.event.schedule.ScheduleEntryResizeEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleEvent;
import org.primefaces.model.ScheduleModel;

/**
 *
 * @author L C J Samarasekara
 * 
 */
@Named
@SessionScoped
public class InwardReservationController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    
    ////////////////////////////
    @Inject
    private SessionController sessionController;
    
    ////////////////////////    
    
    private PatientEncounter patientEncounter;
  
    Date fromDate;
    Date toDate;
    private ScheduleModel reservationModel;
    private String serverTimeZone = ZoneId.systemDefault().toString();
    private List<Reservation> selectedReservations;
    private ScheduleEvent<?> sEvent = new DefaultScheduleEvent<>();
    private InwardReservationEvent event = new InwardReservationEvent();

    public String navigateToReservationCalendarFromMenu() {
        return "/inward/inward_reservations_schedule_calendar?faces-redirect=true";
    }
    
    /**
     *
     * @param selectEvent
     */
    public void onEventSelectCal(SelectEvent<ScheduleEvent<?>> selectEvent) {
        sEvent = selectEvent.getObject();
    }

    public void onDateSelect(SelectEvent<LocalDateTime> selectEvent) {
        event = (InwardReservationEvent) DefaultScheduleEvent.builder()
                .startDate(selectEvent.getObject())
                .endDate(selectEvent.getObject().plusHours(1))
                .build();
    }

    public void onEventMove(ScheduleEntryMoveEvent event) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Event moved",
                "Delta:" + event.getDeltaAsDuration());

        addMessage(message);
    }

    public void onEventResize(ScheduleEntryResizeEvent event) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Event resized",
                "Start-Delta:" + event.getDeltaStartAsDuration() + ", End-Delta: " + event.getDeltaEndAsDuration());

        addMessage(message);
    }

    private void addMessage(FacesMessage message) {
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void findSessions() {
        
        generateChaneelSessionEvents(selectedReservations);
    }

    public void generateChaneelSessionEvents(List<Reservation> lsi) {
        reservationModel = new DefaultScheduleModel();
        for (Reservation si : lsi) {
            System.out.println("Name = " + si.getRoom().getName());

            Calendar sdt = Calendar.getInstance();
            sdt.setTime(si.getReservedFrom());

            Calendar st = Calendar.getInstance();
            st.setTime(si.getReservedFrom());

            sdt.set(Calendar.HOUR, st.get(Calendar.HOUR));
            sdt.set(Calendar.MINUTE, st.get(Calendar.MINUTE));

            Calendar edt = Calendar.getInstance();
            sdt.setTime(si.getReservedTo());

            Calendar et = Calendar.getInstance();
            st.setTime(si.getReservedTo());

            edt.set(Calendar.HOUR, et.get(Calendar.HOUR));
            edt.set(Calendar.MINUTE, et.get(Calendar.MINUTE));

            DefaultScheduleEvent event;
            event = new DefaultScheduleEvent<SessionInstance>().builder()
                    .title(si.getRoom().getName()+ " - " + si.getPatient().getPerson().getName())
                    .borderColor("#27AE60")
                    .backgroundColor("#BDE8CA")
                    .startDate(CommonFunctions.convertDateToLocalDateTime(sdt.getTime()))
                    .endDate(CommonFunctions.convertDateToLocalDateTime(edt.getTime()))
                    .data(si)
                    .build();

            reservationModel.addEvent(event);
        }
    }
    
    

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public ScheduleModel getReservationModel() {
        if(reservationModel == null){
            reservationModel = new DefaultScheduleModel();
        }
        return reservationModel;
    }

    public void setReservationModel(ScheduleModel reservationModel) {
        this.reservationModel = reservationModel;
    }

    public String getServerTimeZone() {
        return serverTimeZone;
    }

    public void setServerTimeZone(String serverTimeZone) {
        this.serverTimeZone = serverTimeZone;
    }

    public List<Reservation> getSelectedReservations() {
        return selectedReservations;
    }

    public void setSelectedReservations(List<Reservation> selectedReservations) {
        this.selectedReservations = selectedReservations;
    }

    public ScheduleEvent<?> getsEvent() {
        return sEvent;
    }

    public void setsEvent(ScheduleEvent<?> sEvent) {
        this.sEvent = sEvent;
    }

    public InwardReservationEvent getEvent() {
        return event;
    }

    public void setEvent(InwardReservationEvent event) {
        this.event = event;
    }
}
