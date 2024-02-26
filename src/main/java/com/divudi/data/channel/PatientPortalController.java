package com.divudi.data.channel;

import com.divudi.bean.common.DoctorController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.PersonInstitutionType;
import com.divudi.ejb.ChannelBean;
import com.divudi.entity.Bill;
import com.divudi.entity.Consultant;
import com.divudi.entity.Doctor;
import com.divudi.entity.Payment;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionInstanceFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.java.CommonFunctions;
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
import javax.persistence.TemporalType;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;

/**
 *
 * @author acer
 */
@Named(value = "patientPortalController")
@ApplicationScoped
public class PatientPortalController {

    private String PatientphoneNumber;
    private boolean bookDoctor;
    private Staff selectedConsultant;
    private List<Bill> patientBills;
    private List<Payment> PatientPayments;
    private List<Payment> channelPayments;
    private List<ServiceSession> docotrSessions;
    private List<Staff> consultants;
    private List<ServiceSession> channelSessions;
    private Speciality selectedSpeciality;
    private ServiceSession selectedChannelSession;
    private Date date;
    private List<SessionInstance> sessionInstances;
    private Date sessionStartingDate;
    ScheduleModel eventModel;

    Staff staff;
    ServiceSession serviceSession;

    @EJB
    private StaffFacade staffFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    SessionInstanceFacade sessionInstanceFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    DoctorController doctorController;

    private ChannelBean channelBean;

    public void docotrBooking() {
        bookDoctor = true;
        fillConsultants();

    }

    public void fillConsultants() {
        consultants = null;
        Map m = new HashMap();
        String sql = "select p from Staff p where p.retired=false and type(p)=:stype";
        m.put("stype", Consultant.class);
        consultants = staffFacade.findByJpql(sql, m);
    }

    public void fillChannelSessions() {
        if (selectedConsultant != null) {
            Map m = new HashMap();
            String sql = "select s from ServiceSession s where s.retired=false and s.staff=:sd ";
            m.put("sd", selectedConsultant);
            // m.put("wd", 10);
            channelSessions = serviceSessionFacade.findByJpql(sql, m);
            System.out.println("channelSessions = " + channelSessions.size());
        }

    }

    public void fillSessionInstance() {
        if (channelSessions != null) {
            sessionInstances = new ArrayList<>();
            sessionStartingDate = new Date();
            String jpql = "select i "
                    + " from SessionInstance i "
                    + " where i.originatingSession=:os "
                    + " and i.retired=:ret ";

            Map m = new HashMap();
            m.put("ret", false);
            m.put("os", selectedChannelSession);

            sessionInstances = sessionInstanceFacade.findByJpql(jpql, m, TemporalType.DATE);
            System.out.println("sessionInstances = " + sessionInstances.size());
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

    public Staff getSelectedConsultant() {
        return selectedConsultant;
    }

    public void setSelectedConsultant(Staff selectedConsultant) {
        this.selectedConsultant = selectedConsultant;
    }

    public List<Staff> getConsultants() {
        return consultants;
    }

    public void setConsultants(List<Staff> consultants) {
        this.consultants = consultants;
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

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public List<SessionInstance> getSessionInstances() {
        return sessionInstances;
    }

    public void setSessionInstances(List<SessionInstance> sessionInstances) {
        this.sessionInstances = sessionInstances;
    }

    public Date getSessionStartingDate() {
        if (sessionStartingDate == null) {
            sessionStartingDate = new Date();
        }
        return sessionStartingDate;
    }

    public void setSessionStartingDate(Date sessionStartingDate) {
        this.sessionStartingDate = sessionStartingDate;
    }

}
