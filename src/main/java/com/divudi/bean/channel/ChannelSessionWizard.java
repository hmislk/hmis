/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.StaffFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;

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
    boolean createNewSession;
    @EJB
    StaffFacade staffFacade;

    public ChannelSessionWizard() {
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

    public boolean isCreateNewSession() {
        return createNewSession;
    }

    public void setCreateNewSession(boolean createNewSession) {
        this.createNewSession = createNewSession;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

}
