/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.FeeType;
import com.divudi.entity.Fee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named(value = "sheduleController")
@SessionScoped
public class SheduleController implements Serializable {

    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade facade;
    @EJB
    private FeeFacade feeFacade;
    @Inject
    private SessionController sessionController;
    private Speciality speciality;
    ServiceSession current;  
    private Staff currentStaff;
    private List<ServiceSession> filteredValue;
    private Fee hospitalFee;
    private Fee doctorFee;
    private Fee tax;

   

    public void makeNull() {
        speciality = null;
        current = null;
        currentStaff = null;
        filteredValue = null;
        hospitalFee = null;
        doctorFee = null;
        tax = null;     
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

    public List<Staff> getConsultants() {
        List<Staff> suggestions;
        String sql;

        if (getSpeciality() != null) {
            sql = "select p from Staff p where p.retired=false and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
        }
        //System.out.println(sql);
        suggestions = getStaffFacade().findBySQL(sql);

        return suggestions;
    }

    public List<ServiceSession> completeSession(String query) {
        List<ServiceSession> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<ServiceSession>();
        } else {
            if (getCurrentStaff() != null) {
                sql = "select p from ServiceSession p where p.retired=false and upper(p.name) like '%" + query.toUpperCase() + "%' and p.staff.id = " + getCurrentStaff().getId() + " order by p.name";
                suggestions = getFacade().findBySQL(sql);
            } else {
                suggestions = new ArrayList<ServiceSession>();
            }

        }
        return suggestions;
    }

    public SheduleController() {
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

    public ServiceSession getCurrent() {
        if (current == null) {
            current = new ServiceSession();
        }
        return current;
    }

    public void setCurrent(ServiceSession current) {
        this.current = current;

        List<Fee> tmp = new ArrayList<Fee>();

        if (getCurrent().getId() != null) {
            tmp = getFeeFacade().findBySQL("Select i from Fee i where i.retired=false and i.serviceSession.id=" + getCurrent().getId());
        }

        for (Fee f : tmp) {
            if (f.getFeeType() == FeeType.Staff) {
                doctorFee = f;
            } else if (f.getFeeType() == FeeType.OwnInstitution) {
                hospitalFee = f;
            } else if (f.getFeeType() == FeeType.Tax) {
                tax = f;
            }
        }

    }

    public List<ServiceSession> getItems() {
        List<ServiceSession> items;
        String sql;
        if (currentStaff == null) {
            // items = getFacade().findAll("name", true);
            items = new ArrayList<ServiceSession>();
        } else {
            sql = "Select s From ServiceSession s where s.retired=false and s.staff.id=" + currentStaff.getId();
            items = getFacade().findBySQL(sql);
        }

        return items;
    }

    public void prepareAdd() {
        current = new ServiceSession();
        hospitalFee = null;
        doctorFee = null;
        tax = null;
    }

    public ServiceSessionFacade getFacade() {
        return facade;
    }

    public void setFacade(ServiceSessionFacade facade) {
        this.facade = facade;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }

        getItems();
        current = null;
        getCurrent();
    }

    
    private boolean checkError() {
        if (getCurrent().getStartingTime() == null) {
            UtilityController.addErrorMessage("Starting time Must be Filled");
            return true;
        }

        if (getCurrent().getSessionWeekday() == 0 && getCurrent().getSessionDate() == null) {
            UtilityController.addErrorMessage("Set Weekday or Date");
            return true;
        }

        return false;
    }

    public void saveSelected() {
        if (checkError()) {
            return;
        }
        current.setStaff(currentStaff);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
        prepareAdd();
        getItems();
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public List<ServiceSession> getAllSession() {
        String sql = "Select s From ServiceSession s where s.retired=false order by s.staff.speciality.name,s.staff.person.name,s.sessionWeekday,s.startingTime ";
        List<ServiceSession> tmp = getFacade().findBySQL(sql);

        return tmp;
    }

    public List<ServiceSession> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<ServiceSession> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public Fee getHospitalFee() {

        if (hospitalFee == null) {
            hospitalFee = new Fee();
            hospitalFee.setName("Hospital Fee");
            hospitalFee.setFeeType(FeeType.OwnInstitution);
        }

        return hospitalFee;
    }

    public void setHospitalFee(Fee hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Fee getDoctorFee() {
        if (doctorFee == null) {
            doctorFee = new Fee();
            doctorFee.setName("Staff Fee");
            doctorFee.setFeeType(FeeType.Staff);
        }
        return doctorFee;
    }

    public void setDoctorFee(Fee doctorFee) {
        this.doctorFee = doctorFee;
    }

    public Fee getTax() {
        if (tax == null) {
            tax = new Fee();
            tax.setName("Tax");
            tax.setFeeType(FeeType.Tax);
        }
        return tax;
    }

    public void setTax(Fee tax) {
        this.tax = tax;
    }

   
}
