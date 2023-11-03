/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.ServiceSessionLeaveFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ServiceSessionLeaveController implements Serializable {

    private ServiceSessionLeave current;
    private Staff currentStaff;
    private Speciality speciality;
    ServiceSession selectedServiceSession;
    boolean removeLeave;

    List<ServiceSessionLeave> serviceSessionLeaves;
    ///////////////////////
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionLeaveFacade facade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;

    @Inject
    SessionController sessionController;
    @Inject
    BookingController bookingController;

    public List<Staff> getConsultants() {
        List<Staff> suggestions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        m.put("sp", getSpeciality());
        if (getSessionController().getLoggedPreference().isShowOnlyMarkedDoctors()) {

            sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                    + " and pi.type=:typ "
                    + " and pi.institution=:ins "
                    + " and pi.staff.speciality=:sp "
                    + " order by pi.staff.person.name ";

            m.put("ins", getSessionController().getInstitution());
            m.put("typ", PersonInstitutionType.Channelling);

        } else {
            sql = "select p from Staff p where p.retired=false and p.speciality=:sp order by p.person.name";
        }

        suggestions = getStaffFacade().findByJpql(sql, m);


        return suggestions;
    }

    private boolean errorCheck() {
        if (getCurrentStaff() == null) {
            UtilityController.addErrorMessage("Please select Staff");
            return true;
        }

        if (getCurrent().getSessionDate() == null) {
            UtilityController.addErrorMessage("Please select Leave Date");
            return true;
        }
        return false;
    }

    private boolean errorCheckForServiceSessoinLeave() {
        if (getSelectedServiceSession() == null) {
            UtilityController.addErrorMessage("Please select Service Session");
            return true;
        }

        if (getCurrent().getDeactivateComment() == null || getCurrent().getDeactivateComment().isEmpty()) {
            UtilityController.addErrorMessage("Please Enter a Reson For Leave");
            return true;
        }
        if (getSelectedServiceSession().isDeactivated()) {
            UtilityController.addErrorMessage("This Service Session is Alredy Leave Added");
            return true;
        }
        return false;
    }

    private boolean errorCheckForServiceSessoinLeaveByDate() {

        if (getCurrent().getDeactivateComment() == null || getCurrent().getDeactivateComment().isEmpty()) {
            UtilityController.addErrorMessage("Please Enter a Reson For Leave");
            return true;
        }
        if (bookingController.getSessionStartingDate() == null) {
            UtilityController.addErrorMessage("Plaease Select Leave Date");
            return true;
        }
        return false;
    }

//    public void remove() {
//        getFacade().remove(getCurrent());
//        current = null;
//    }
    public void remove(ServiceSessionLeave ssl) {
        ssl.setRetired(true);
        ssl.setRetiredAt(new Date());
        ssl.setRetirer(getSessionController().getLoggedUser());
        getFacade().edit(ssl);
    }

    public void removeLeaveAndActiveServiceSession(ServiceSessionLeave ssl) {
        if (ssl.getRetireComments() == null || ssl.getRetireComments().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Remove Comment.");
            return;
        }
        ssl.getOriginatingSession().setDeactivated(false);
        getServiceSessionFacade().edit(ssl.getOriginatingSession());

        ssl.setRetired(true);
        ssl.setRetiredAt(new Date());
        ssl.setRetirer(getSessionController().getLoggedUser());
        getFacade().edit(ssl);
        fillLeaveItems();
    }

    public void removeLeaveAndActiveServiceSessionByDate() {
        if (bookingController.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff.");
            return;
        }
        if (current.getDeactivateComment() == null || current.getDeactivateComment().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Remove Comment.");
            return;
        }
        List<ServiceSessionLeave> serviceSessionLeaves = fetchCreatedLeaveServiceSession(bookingController.getSessionStartingDate(), bookingController.getStaff());
        if (serviceSessionLeaves.isEmpty()) {
            JsfUtil.addErrorMessage("Please Select Correct Date This Date hasn't Any Leave");
            return;
        }
        for (ServiceSessionLeave ssl : serviceSessionLeaves) {
            ssl.getOriginatingSession().setDeactivated(false);
            getServiceSessionFacade().edit(ssl.getOriginatingSession());

            ssl.setRetired(true);
            ssl.setRetiredAt(new Date());
            ssl.setRetirer(getSessionController().getLoggedUser());
            ssl.setRetireComments(getCurrent().getDeactivateComment());
            getFacade().edit(ssl);
        }
        current = null;
        fillLeaveItems();
    }

    public void fillLeaveItems() {
        setCurrentStaff(bookingController.getStaff());

        String slq = "Select s From ServiceSessionLeave s Where "
                + " s.sessionDate>= :dt "
                + " and s.staff=:st "
                + " order by s.sessionDate";
        HashMap hm = new HashMap();
        hm.put("dt", bookingController.getSessionStartingDate());
        hm.put("st", getCurrentStaff());

        serviceSessionLeaves = getFacade().findByJpql(slq, hm, TemporalType.DATE);
//        //// // System.out.println("hm = " + hm);
//        //// // System.out.println("slq = " + slq);
//        //// // System.out.println("serviceSessionLeaves.size() = " + serviceSessionLeaves.size());
        bookingController.generateSessionsOnlyId();
    }

    public void addLeave() {
        if (errorCheck()) {
            return;
        }

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setStaff(getCurrentStaff());
        getFacade().create(getCurrent());
        current = null;

    }

    public void addLeaveForServiceSession() {
        if (errorCheckForServiceSessoinLeave()) {
            return;
        }

        //deactive Service Session
        getSelectedServiceSession().setDeactivated(true);
        getServiceSessionFacade().edit(selectedServiceSession);

        //create servicesession Leave
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setStaff(getSelectedServiceSession().getStaff());
        getCurrent().setOriginatingSession(getSelectedServiceSession());
        getCurrent().setSessionDate(getSelectedServiceSession().getSessionDate());//leave date
        getFacade().create(getCurrent());

//        bookingController.setSelectedServiceSession(selectedServiceSession);
//        bookingController.fillBillSessions();
//        bookingController.sendSmsToinformLeave();

        current = null;
        selectedServiceSession = null;
        fillLeaveItems();

    }

    public void addLeaveForServiceSessionByDate() {
        if (errorCheckForServiceSessoinLeaveByDate()) {
            return;
        }
        List<ServiceSession> serviceSessions = fetchCreatedServiceSession(bookingController.getSessionStartingDate(), bookingController.getStaff());
        if (serviceSessions.isEmpty()) {
            UtilityController.addErrorMessage("Selected Date Haven't Sessions or This Date Already Added Leave");
            return;
        }
        for (ServiceSession s : serviceSessions) {
            //deactive Service Session
            s.setDeactivated(true);
            getServiceSessionFacade().edit(s);

            //create servicesession Leave
            ServiceSessionLeave ss = new ServiceSessionLeave();
            ss.setCreatedAt(new Date());
            ss.setCreater(getSessionController().getLoggedUser());
            ss.setStaff(s.getStaff());
            ss.setOriginatingSession(s);
            ss.setSessionDate(s.getSessionDate());//leave date
            ss.setDeactivateComment(getCurrent().getDeactivateComment());
            getFacade().create(ss);
            
//            bookingController.setSelectedServiceSession(s);
//            bookingController.fillBillSessions();
//            bookingController.sendSmsToinformLeave();

        }
        current = null;
        fillLeaveItems();

    }

    public List<ServiceSession> fetchCreatedServiceSession(Date d, Staff s) {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s where s.retired=false "
                + " and s.staff=:staff "
                + " and s.originatingSession is not null "
                + " and s.sessionDate=:d "
                + " and type(s)=:class"
                + " and s.deactivated=false "
                + " order by s.sessionWeekday,s.startingTime ";
        m.put("d", d);
        m.put("staff", s);
        m.put("class", ServiceSession.class);
        List<ServiceSession> tmp = getServiceSessionFacade().findByJpql(sql, m, TemporalType.DATE);
        return tmp;
    }

    public List<ServiceSessionLeave> fetchCreatedLeaveServiceSession(Date d, Staff s) {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSessionLeave s where s.retired=false "
                + " and s.staff=:staff "
                + " and s.sessionDate=:d "
                + " and type(s)=:class ";
        m.put("d", d);
        m.put("staff", s);
        m.put("class", ServiceSessionLeave.class);
        List<ServiceSessionLeave> tmp = getFacade().findByJpql(sql, m, TemporalType.DATE);
        return tmp;
    }

    /**
     * Creates a new instance of ServiceSessionLeaveController
     */
    public ServiceSessionLeaveController() {
    }

    public ServiceSessionLeave getCurrent() {
        if (current == null) {
            current = new ServiceSessionLeave();
        }
        return current;
    }

    public void setCurrent(ServiceSessionLeave current) {
        this.current = current;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        current = null;
        this.currentStaff = currentStaff;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        currentStaff = null;
        this.speciality = speciality;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public ServiceSessionLeaveFacade getFacade() {
        return facade;
    }

    public void setFacade(ServiceSessionLeaveFacade facade) {
        this.facade = facade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public List<ServiceSessionLeave> getServiceSessionLeaves() {
        if (serviceSessionLeaves == null) {
            serviceSessionLeaves = new ArrayList<>();
        }
        return serviceSessionLeaves;
    }

    public void setServiceSessionLeaves(List<ServiceSessionLeave> serviceSessionLeaves) {
        this.serviceSessionLeaves = serviceSessionLeaves;
    }

    public boolean isRemoveLeave() {
        return removeLeave;
    }

    public void setRemoveLeave(boolean removeLeave) {
        this.removeLeave = removeLeave;
    }

}
