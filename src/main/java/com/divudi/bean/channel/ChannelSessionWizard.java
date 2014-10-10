/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Fee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import com.divudi.facade.StaffFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Sniper 619
 */
@Named(value = "channelSessionWizard")
@SessionScoped
public class ChannelSessionWizard implements Serializable {

    ServiceSession current;
    Speciality speciality;
    Staff currentStaff;
    Boolean createNewSession;
    private Fee hospitalFee;
    private Fee doctorFee;
    private Fee other;
    @EJB
    StaffFacade staffFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;
    @Inject
    SessionController sessionController;

    public ChannelSessionWizard() {
    }
    
    public void prepareAdd() {
        current = new ServiceSession();
        hospitalFee = null;
        doctorFee = null;
        other = null;
//        speciality = null;
//        currentStaff = null;
    }
    
    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getServiceSessionFacade().edit(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }

        getItems();
        current = null;
        getCurrent();
    }
    
    public void saveSelected() {
        if (getCurrent().getSessionNumberGenerator() == null) {
            SessionNumberGenerator ss = saveSessionNumber();
            current.setSessionNumberGenerator(ss);
        }
        if (checkError()) {
            return;
        }

        current.setStaff(currentStaff);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getServiceSessionFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getServiceSessionFacade().create(getCurrent());
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
        prepareAdd();
        getItems();
    }
    
    public SessionNumberGenerator saveSessionNumber() {
        SessionNumberGenerator sessionNumberGenerator = new SessionNumberGenerator();
        sessionNumberGenerator.setSpeciality(speciality);
        sessionNumberGenerator.setStaff(currentStaff);
        sessionNumberGenerator.setName(currentStaff.getPerson().getName() + " " + current.getName());
        sessionNumberGeneratorFacade.create(sessionNumberGenerator);
        return sessionNumberGenerator;
    }
    
    private boolean checkError() {
        if (getCurrent().getStartingTime() == null) {
            UtilityController.addErrorMessage("Starting time Must be Filled");
            return true;
        }

        if (getCurrent().getSessionWeekday() == null && getCurrent().getSessionDate() == null) {
            UtilityController.addErrorMessage("Set Weekday or Date");
            return true;
        }

        if (speciality == null) {
            UtilityController.addErrorMessage("Plaese Select Specility");
            return true;
        }

        if (currentStaff == null) {
            UtilityController.addErrorMessage("Plaese Select Doctor");
            return true;
        }

        return false;
    }
    
    public List<ServiceSession> completeSession(String query) {
        List<ServiceSession> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<ServiceSession>();
        } else {
            if (getCurrentStaff() != null) {
                sql = "select p from ServiceSession p where p.retired=false and upper(p.name) like '%" + query.toUpperCase() + "%' and p.staff.id = " + getCurrentStaff().getId() + " order by p.name";
                suggestions = getServiceSessionFacade().findBySQL(sql);
            } else {
                suggestions = new ArrayList<ServiceSession>();
            }

        }
        return suggestions;
    }
    
    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getSpeciality() != null) {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql);
        }
        return suggestions;
    }
    
    public List<ServiceSession> getItems() {
        List<ServiceSession> items;
        String sql;
        HashMap hm = new HashMap();
//        if (currentStaff == null) {
//            // items = getFacade().findAll("name", true);
//            items = new ArrayList<>();
//        } else {
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and s.staff=:stf ";
        hm.put("stf", currentStaff);
//        hm.put("class", ServiceSessionLeave.class);
        items = getServiceSessionFacade().findBySQL(sql, hm);
//        }

        return items;
    }
    
    public void sessionListner(){
        if (createNewSession==true) {
            prepareAdd();
        }
    }

    public ServiceSession getCurrent() {
        if (current == null) {
            current = new ServiceSession();
        }
        return current;
    }

    public void setCurrent(ServiceSession current) {
        this.current = current;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public Boolean getCreateNewSession() {
        return createNewSession;
    }

    public void setCreateNewSession(Boolean createNewSession) {
        this.createNewSession = createNewSession;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public Fee getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Fee hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Fee getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(Fee doctorFee) {
        this.doctorFee = doctorFee;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SessionNumberGeneratorFacade getSessionNumberGeneratorFacade() {
        return sessionNumberGeneratorFacade;
    }

    public void setSessionNumberGeneratorFacade(SessionNumberGeneratorFacade sessionNumberGeneratorFacade) {
        this.sessionNumberGeneratorFacade = sessionNumberGeneratorFacade;
    }

    public Fee getOther() {
        return other;
    }

    public void setOther(Fee other) {
        this.other = other;
    }

}
