/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.ServiceSessionLeaveFacade;
import com.divudi.facade.StaffFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
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
    ///////////////////////
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionLeaveFacade facade;
    
    @Inject
    SessionController sessionController;

    public List<Staff> getConsultants() {
        List<Staff> suggestions=new ArrayList<>();
        String sql;
        Map m = new HashMap();
        if (getSpeciality() != null) {

            m.put("sp", getSpeciality());
            if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

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
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
        }
        suggestions = getStaffFacade().findBySQL(sql, m);

        System.out.println("m = " + m);
        System.out.println("sql = " + sql);
        System.out.println("suggestions.size() = " + suggestions.size());
        
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

    public void remove() {
        getFacade().remove(getCurrent());
        current = null;
    }

    public List<ServiceSessionLeave> getItems() {

        String slq = "Select s From ServiceSessionLeave s Where s.sessionDate> :dt and s.staff=:st";
        HashMap hm = new HashMap();
        hm.put("dt", Calendar.getInstance().getTime());
        hm.put("st", getCurrentStaff());
        return getFacade().findBySQL(slq, hm, TemporalType.DATE);
    }

    public void addLeave() {
        if (errorCheck()) {
            return;
        }

        getCurrent().setStaff(getCurrentStaff());
        getFacade().create(getCurrent());
        current = null;

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

}
