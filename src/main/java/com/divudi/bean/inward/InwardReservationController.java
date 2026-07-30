package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.AppointmentStatus;
import com.divudi.core.data.DepartmentType;
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
import com.divudi.core.entity.Appointment;

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
    private com.divudi.core.facade.AppointmentFacade appointmentFacade;
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

    private ScheduleModel theatreScheduleModel;
    private List<Reservation> theatreReservations;
    
            private ReservationDTO convertToReservationDTO(Appointment apt) {
        Appointment reloadAppointment = appointmentFacade.find(apt.getId());
        if (reloadAppointment == null) {
            return null;
        }
        Reservation res = findReservationForAppointment(reloadAppointment);
        
        // 1. Resolve start and end dates first
        Date start = res != null ? res.getReservedFrom() : combineDateAndTime(reloadAppointment.getAppointmentDate(), reloadAppointment.getAppointmentTimeFrom());
        Date end = res != null ? res.getReservedTo() : combineDateAndTime(reloadAppointment.getAppointmentDate(), reloadAppointment.getAppointmentTimeTo());

        // 2. Instantiate ReservationDTO with the normalized end date
        currentReservationDTO = new ReservationDTO(
                reloadAppointment.getId(), 
                start, 
                normalizeEndDate(start, end), // Safely checks for nulls/invalids
                reloadAppointment.getAppointmentNumber(), 
                reloadAppointment.getCreatedAt(), 
                res != null && res.getRoom() != null ? res.getRoom().getName() : "N/A", 
                reloadAppointment.getPatient().getPerson().getTitle(), 
                reloadAppointment.getPatient().getPerson().getName(), 
                reloadAppointment.getPatient().getPerson().getDob(), 
                reloadAppointment.getPatient().getPerson().getSex() != null ? reloadAppointment.getPatient().getPerson().getSex().getLabel() : "", 
                reloadAppointment.getPatient().getPerson().getMobile(),
                reloadAppointment.getBill() != null && reloadAppointment.getBill().getReferredBy() != null && reloadAppointment.getBill().getReferredBy().getPerson() != null ? reloadAppointment.getBill().getReferredBy().getPerson().getTitle() : null,
                reloadAppointment.getBill() != null && reloadAppointment.getBill().getReferredBy() != null && reloadAppointment.getBill().getReferredBy().getPerson() != null ? reloadAppointment.getBill().getReferredBy().getPerson().getName() : "",
                reloadAppointment.getStatus()
        );
        return currentReservationDTO;
    }
        
        private Date normalizeEndDate(Date startDate, Date endDate) {
        if (startDate == null) {
            return endDate;
        }
        if (endDate == null || !endDate.after(startDate)) {
            return new Date(startDate.getTime() + 3600000L); // 1-hour fallback duration
        }
        return endDate;
    }   
        
 
     private Reservation findReservationForAppointment(Appointment apt) {
        if (apt == null) {
            return null;
        }
        String jpql = "SELECT r FROM Reservation r WHERE r.retired = :ret AND r.appointment = :apt";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("apt", apt);
        return reservationFacade.findFirstByJpql(jpql, m);
    }
     
    private Date combineDateAndTime(Date date, Date time) {
        if (date == null) {
            return time;
        }
        if (time == null) {
            return date;
        }
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        calendar.setTime(time);
        calendar.set(java.util.Calendar.YEAR, year);
        calendar.set(java.util.Calendar.MONTH, month);
        calendar.set(java.util.Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    } 
    
    public String navigateToReservationCalendarFromMenu() {
        currentReservationDTO = null;
        fromDate = CommonFunctions.getStartOfDay();
        Long noOfMonths = configOptionApplicationController.getLongValueByKey("Number of Months to Load During Reservation Calendar", 6L);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);
        calendar.add(Calendar.MONTH, noOfMonths.intValue());
        toDate = calendar.getTime();
        calanderStatus = AppointmentStatus.PENDING;
        findReservations();
        return "/inward/inward_reservations_schedule_calendar?faces-redirect=true";
    }

    /**
     *
     * @param selectEvent
     */
        public void onEventSelectCal(SelectEvent<ScheduleEvent<?>> selectEvent) {
            currentReservationDTO = null; // Clear the previous selection first
            sEvent = selectEvent.getObject();
            Object data = sEvent.getData();
                if (data instanceof Appointment) {
                    convertToReservationDTO((Appointment) data);
                }
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

            jpql = "Select apt from Appointment apt "
                    + " where apt.retired=:ret"
                    + " and apt.appointmentType=:type";
            m.put("ret", false);
            m.put("type", com.divudi.core.data.AppointmentType.IP_APPOINTMENT);

            if (calanderStatus != null) {
                jpql += " and apt.status = :status";
                m.put("status", calanderStatus);
            }

            if (fromDate != null && toDate != null) {
                jpql += " and (apt.appointmentDate between :fd and :td"
                    + " or exists (select r.id from Reservation r"
                    + " where r.retired = :ret"
                    + " and r.appointment = apt"
                    + " and r.reservedFrom <= :td"
                    + " and (r.reservedTo is null or r.reservedTo >= :fd)))";
                    m.put("fd", fromDate);
                    m.put("td", toDate);
            }

            List<Appointment> appointments = appointmentFacade.findByJpql(jpql, m, javax.persistence.TemporalType.TIMESTAMP);
            generateReservationsEvents(appointments);
    }

            public void generateReservationsEvents(List<Appointment> appointments) {
        reservationModel = new DefaultScheduleModel();
        for (Appointment apt : appointments) {
            // Null safety check
            if (apt.getPatient() == null || apt.getPatient().getPerson() == null) {
                continue;
            }

            Reservation res = findReservationForAppointment(apt);
            Date startDate;
            Date endDate;
            String title;
            String roomName;

            // 1. Safely resolve patientName (never null for generateColor)
            String patientName = apt.getPatient().getPerson().getName() != null 
                    ? apt.getPatient().getPerson().getName() 
                    : "Patient";

            if (res != null) {
                // If a Room Reservation exists (ROOM_ADMISSION type)
                startDate = res.getReservedFrom();
                endDate = normalizeEndDate(startDate, res.getReservedTo()); // Normalized via suggestion #2 helper
                
                // Safely resolve roomName
                roomName = res.getRoom() != null && res.getRoom().getName() != null 
                        ? res.getRoom().getName() 
                        : "Room";
                title = roomName + " - " + patientName;
            } else {
                // Non-room Appointment (Procedure, Consultant, etc.)
                startDate = combineDateAndTime(apt.getAppointmentDate(), apt.getAppointmentTimeFrom());
                endDate = normalizeEndDate(startDate, combineDateAndTime(apt.getAppointmentDate(), apt.getAppointmentTimeTo())); // Normalized via suggestion #2 helper
                
                String categoryLabel = apt.getItem() != null && apt.getItem().getName() != null 
                        ? apt.getItem().getName() 
                        : "Appointment";
                roomName = "Procedure";
                title = categoryLabel + " - " + patientName;
            }

            if (startDate == null) {
                continue;
            }

            // 2. Generate colors using sanitized, non-null values
            String uniqueBorderColor = generateColor(roomName);
            String uniqueBackgroundColor = generateColor(patientName);

            // Build the schedule calendar event using the Appointment object as data binding
            DefaultScheduleEvent tempEvent = new DefaultScheduleEvent<Appointment>()
                    .builder()
                    .title(title)
                    .borderColor(uniqueBorderColor)
                    .backgroundColor(uniqueBackgroundColor)
                    .startDate(CommonFunctions.convertDateToLocalDateTime(startDate))
                    .endDate(CommonFunctions.convertDateToLocalDateTime(endDate))
                    .data(apt)
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

    // -----------------------------------------------------------------------
    // Theatre Schedule Calendar — shows only Theatre-room reservations
    // -----------------------------------------------------------------------

    public String navigateToTheatreScheduleCalendar() {
        currentReservationDTO = null;
        fromDate = CommonFunctions.getStartOfDay();
        Long noOfMonths = configOptionApplicationController.getLongValueByKey(
                "Number of Months to Load During Reservation Calendar", 6L);
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(Calendar.MONTH, noOfMonths.intValue());
        toDate = cal.getTime();
        calanderStatus = null;
        findTheatreReservations();
        return "/theater/theatre_schedule_calendar?faces-redirect=true";
    }

    public void findTheatreReservations() {
        Map m = new HashMap();
        m.put("ret", false);
        m.put("deptType", DepartmentType.Theatre);
        m.put("institution", sessionController.getInstitution());
        String jpql = "SELECT r FROM Reservation r "
                + "WHERE r.retired = :ret "
                + "AND r.room.department.departmentType = :deptType "
                + "AND r.room.department.institution = :institution";
        if (calanderStatus != null) {
            jpql += " AND r.appointment.status = :status";
            m.put("status", calanderStatus);
        }
        if (fromDate != null && toDate != null) {
            jpql += " AND r.reservedFrom <= :td"
                  + " AND (r.reservedTo IS NULL OR r.reservedTo >= :fd)";
            m.put("fd", fromDate);
            m.put("td", toDate);
        }
        jpql += " ORDER BY r.reservedFrom";
        theatreReservations = reservationFacade.findByJpqlWithoutCache(jpql, m, TemporalType.TIMESTAMP);
        if (theatreReservations == null) {
            theatreReservations = new java.util.ArrayList<>();
        }
        generateTheatreScheduleEvents(theatreReservations);
    }

    private void generateTheatreScheduleEvents(List<Reservation> reservations) {
        theatreScheduleModel = new DefaultScheduleModel();
        for (Reservation r : reservations) {
            if (r.getReservedFrom() == null || r.getRoom() == null || r.getPatient() == null) {
                continue;
            }
            Date end = r.getReservedTo() != null
                    ? r.getReservedTo()
                    : new Date(r.getReservedFrom().getTime() + 3_600_000L); // 1h default
            String roomName = r.getRoom().getName() != null ? r.getRoom().getName() : "Theatre";
            String patientName = r.getPatient().getPerson() != null
                    ? r.getPatient().getPerson().getName() : "Patient";
            DefaultScheduleEvent tempEvent = new DefaultScheduleEvent<SessionInstance>()
                    .builder()
                    .title(roomName + " — " + patientName)
                    .borderColor(generateColor(roomName))
                    .backgroundColor(generateColor(roomName))
                    .startDate(CommonFunctions.convertDateToLocalDateTime(r.getReservedFrom()))
                    .endDate(CommonFunctions.convertDateToLocalDateTime(end))
                    .data(r)
                    .build();
            theatreScheduleModel.addEvent(tempEvent);
        }
    }

    public ScheduleModel getTheatreScheduleModel() {
        if (theatreScheduleModel == null) {
            theatreScheduleModel = new DefaultScheduleModel();
        }
        return theatreScheduleModel;
    }

    public List<Reservation> getTheatreReservations() {
        if (theatreReservations == null) {
            theatreReservations = new java.util.ArrayList<>();
        }
        return theatreReservations;
    }
}
