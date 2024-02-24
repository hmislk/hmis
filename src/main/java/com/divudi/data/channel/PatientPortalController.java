package com.divudi.data.channel;

import com.divudi.bean.common.DoctorController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.Bill;
import com.divudi.entity.Consultant;
import com.divudi.entity.Doctor;
import com.divudi.entity.Payment;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author acer
 */
@Named(value = "patientPortalController")
@ApplicationScoped
public class PatientPortalController {

    private String PatientphoneNumber;
    private boolean bookDoctor;
    private Staff selectedDoctor;
    private List<Bill> patientBills;
    private List<Payment> PatientPayments;
    private List<Payment> channelPayments;
    private List<ServiceSession> docotrSessions;
    private List<Staff> doctors;
    private List<ServiceSession> channelSessions;
    private Speciality selectedSpeciality;
    private ServiceSession selectedChannelSession;
    private Date date;
    Staff staff;
    ServiceSession serviceSession;

    @EJB
    StaffFacade staffFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    DoctorController doctorController;

    public void docotrBooking() {
        bookDoctor = true;
        channelDoctors();

    }

    public void channelDoctors() {
        doctors = null;
        Map m = new HashMap();
        String sql = "select p from Staff p where p.retired=false and type(p)=:stype";
        m.put("stype", Consultant.class);
        doctors = staffFacade.findByJpql(sql, m);
    }

    public void fillChannelBookings() {
        Calendar c = Calendar.getInstance();
        c.setTime(getDate());
        int wd = c.get(Calendar.DAY_OF_WEEK);
        if (selectedDoctor != null) {
            Map m = new HashMap();
            String sql = "select s from ServiceSession s where s.retired=false and s.staff=:sd ";
            m.put("sd", selectedDoctor);
            // m.put("wd", 10);
            channelSessions = serviceSessionFacade.findByJpql(sql, m);
            System.out.println("channelSessions = " + channelSessions.size());
        }

    }

    public String getPatientphoneNumber() {
        return PatientphoneNumber;
    }

    public void setPatientphoneNumber(String PatientphoneNumber) {
        this.PatientphoneNumber = PatientphoneNumber;
    }

    public boolean isBookDoctor() {
        return bookDoctor;
    }

    public void setBookDoctor(boolean bookDoctor) {
        this.bookDoctor = bookDoctor;
    }

    public List<Bill> getPatientBills() {
        return patientBills;
    }

    public void setPatientBills(List<Bill> patientBills) {
        this.patientBills = patientBills;
    }

    public List<Payment> getPatientPayments() {
        return PatientPayments;
    }

    public void setPatientPayments(List<Payment> PatientPayments) {
        this.PatientPayments = PatientPayments;
    }

    public List<Payment> getChannelPayments() {
        return channelPayments;
    }

    public void setChannelPayments(List<Payment> channelPayments) {
        this.channelPayments = channelPayments;
    }

    public List<ServiceSession> getDocotrSessions() {
        return docotrSessions;
    }

    public void setDocotrSessions(List<ServiceSession> channels) {
        this.docotrSessions = channels;
    }

    public Staff getSelectedDoctor() {
        return selectedDoctor;
    }

    public void setSelectedDoctor(Staff selectedDoctor) {
        this.selectedDoctor = selectedDoctor;
    }

    public List<Staff> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<Staff> doctors) {
        this.doctors = doctors;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Speciality getSelectedSpeciality() {
        return selectedSpeciality;
    }

    public void setSelectedSpeciality(Speciality selectedSpeciality) {
        this.selectedSpeciality = selectedSpeciality;
    }

    public List<ServiceSession> getChannelSessions() {
        return channelSessions;
    }

    public void setChannelSessions(List<ServiceSession> channelSessions) {
        this.channelSessions = channelSessions;
    }

    public ServiceSession getSelectedChannelSession() {
        return selectedChannelSession;
    }

    public void setSelectedChannelSession(ServiceSession selectedChannelSession) {
        this.selectedChannelSession = selectedChannelSession;
    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
