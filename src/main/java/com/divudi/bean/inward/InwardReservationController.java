package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.AppointmentStatus;
import com.divudi.core.data.dto.ReservationDTO;
import com.divudi.core.data.inward.InwardReservationEvent;

import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.entity.inward.Reservation;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.ReservationFacade;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import javax.persistence.TemporalType;
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
    @EJB
    ReservationFacade reservationFacade;
    ////////////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    ////////////////////////
    private PatientEncounter patientEncounter;

    Date fromDate;
    Date toDate;
    private ScheduleModel reservationModel;
    private String serverTimeZone = ZoneId.systemDefault().toString();
    private List<Reservation> selectedReservations;
    private ScheduleEvent<?> sEvent = new DefaultScheduleEvent<>();
    private InwardReservationEvent event = new InwardReservationEvent();
    
    private ReservationDTO currentReservationDTO;
    
    private ReservationDTO convertToReservationDTO(Reservation r){
        Reservation reloadReservation = reservationFacade.find(r.getId());
        if(reloadReservation == null){
            return null;
        }
        currentReservationDTO = new ReservationDTO(
                reloadReservation.getId(), 
                reloadReservation.getReservedFrom(), 
                reloadReservation.getReservedTo(), 
                reloadReservation.getAppointment().getAppointmentNumber(), 
                reloadReservation.getCreatedAt(), 
                reloadReservation.getRoom().getName(), 
                reloadReservation.getAppointment().getPatient().getPerson().getTitle(), 
                reloadReservation.getAppointment().getPatient().getPerson().getName(), 
                reloadReservation.getAppointment().getPatient().getPerson().getDob(), 
                reloadReservation.getAppointment().getPatient().getPerson().getSex().getLabel(), 
                reloadReservation.getAppointment().getPatient().getPerson().getMobile(),
                reloadReservation.getAppointment().getBill().getReferredBy().getPerson().getTitle(),
                reloadReservation.getAppointment().getBill().getReferredBy().getPerson().getName(),
                reloadReservation.getAppointment().getStatus()
        );
        return currentReservationDTO;
    }

    public String navigateToReservationCalendarFromMenu() {
        currentReservationDTO = null;
        fromDate = CommonFunctions.getStartOfDay();
        Long noOfMonths = configOptionApplicationController.getLongValueByKey("Number of Months to Load During Reservation Calendar", 6L);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);
        calendar.add(Calendar.MONTH, noOfMonths.intValue());
        toDate = calendar.getTime();
        findReservations();
        return "/inward/inward_reservations_schedule_calendar?faces-redirect=true";
    }

    /**
     *
     * @param selectEvent
     */
    public void onEventSelectCal(SelectEvent<ScheduleEvent<?>> selectEvent) {
        sEvent = selectEvent.getObject();
        convertToReservationDTO((Reservation) sEvent.getData());
    }

    @Deprecated
    public void onDateSelect(SelectEvent<LocalDateTime> selectEvent) {
        event = (InwardReservationEvent) DefaultScheduleEvent.builder()
                .startDate(selectEvent.getObject().plusHours(0))
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
    
    private AppointmentStatus calanderStatus = null ;

    public void findReservations() {
        String jpql;
        Map m = new HashMap();

        jpql = "Select r from Reservation r "
                + " where r.retired=:ret";
        m.put("ret", false);
        
        if(calanderStatus != null){
            jpql += " and r.appointment.status =:ststus";
            m.put("ststus", calanderStatus);
        }else{
            jpql += " and r.appointment.status =:ststus";
            m.put("ststus", AppointmentStatus.PENDING);
        }
        
        if (fromDate != null) {
            jpql += " and (r.reservedFrom between :fd and :td or r.reservedTo between :fd and :td )";
            m.put("fd", fromDate);
            m.put("td", toDate);
        }
        
        selectedReservations = reservationFacade.findByJpqlWithoutCache(jpql, m, TemporalType.TIMESTAMP);
        generateReservationsEvents(selectedReservations);
    }

    public void generateReservationsEvents(List<Reservation> lsi) {
        reservationModel = new DefaultScheduleModel();
        for (Reservation si : lsi) {

            // Dates
            Date startDate = si.getReservedFrom();
            Date endDate = si.getReservedTo();

            // Generate unique colors for each event
            String uniqueBorderColor = generateColor(si.getRoom().getName());
            String uniqueBackgroundColor = generateColor(si.getPatient().getPerson().getName());

            DefaultScheduleEvent tempEvent;
            tempEvent = new DefaultScheduleEvent<SessionInstance>()
                    .builder()
                    .title(si.getRoom().getName() + " - " + si.getPatient().getPerson().getName())
                    .borderColor(uniqueBorderColor)
                    .backgroundColor(uniqueBackgroundColor)
                    .startDate(CommonFunctions.convertDateToLocalDateTime(startDate))
                    .endDate(CommonFunctions.convertDateToLocalDateTime(endDate))
                    .data(si)
                    .build();

            reservationModel.addEvent(tempEvent);
        }
    }

    private String generateColor(String seed) {
        int hash = seed.hashCode(); // Create a hash from the seed (e.g., room name)
        int hue = Math.abs(hash) % 360; // Map hash to a hue value (0-359)
        int saturation = 70 + (Math.abs(hash) % 30); // Ensure high saturation (70-100)
        int lightness = 50; // Set a fixed lightness for vibrancy

        // Return HSL color in CSS format
        return "hsl(" + hue + ", " + saturation + "%, " + lightness + "%)";
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
        if (reservationModel == null) {
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

    public ReservationDTO getCurrentReservationDTO() {
        return currentReservationDTO;
    }

    public void setCurrentReservationDTO(ReservationDTO currentReservationDTO) {
        this.currentReservationDTO = currentReservationDTO;
    }

    public AppointmentStatus getCalanderStatus() {
        return calanderStatus;
    }

    public void setCalanderStatus(AppointmentStatus calanderStatus) {
        this.calanderStatus = calanderStatus;
    }
}
