/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.FeeType;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.Department;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SheduleController implements Serializable {

    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade facade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;
    @Inject
    private SessionController sessionController;
    private Speciality speciality;
    ServiceSession current;
    private Staff currentStaff;
    private List<ServiceSession> filteredValue;
    List<SessionNumberGenerator> lstSessionNumberGenerator;
    List<ItemFee> itemFees;

    public List<ItemFee> getItemFees() {
        if (itemFees == null) {
            itemFees = new ArrayList<>();
        }
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    @EJB
    ItemFeeFacade itemFeeFacade;

    public void fillFees() {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and (f.serviceSession=:ses "
                + " or f.item=:ses )"
                + " order by f.id";
        m.put("ses", current);
        System.err.println("Fill  Fees");
        itemFees = itemFeeFacade.findBySQL(sql, m);
    }

    public ItemFee createStaffFee() {
        ItemFee stf = new ItemFee();
        stf.setName("Doctor Fee");
        stf.setFeeType(FeeType.Staff);
        stf.setFee(0.0);
        stf.setFfee(0.0);
        stf.setInstitution(getCurrent().getInstitution());
        stf.setSpeciality(speciality);
        stf.setStaff(currentStaff);
        stf.setServiceSession(current);
        return stf;
    }

    public ItemFee createHospitalFee() {
        ItemFee hos = new ItemFee();
        hos.setName("Hospital Fee");
        hos.setFeeType(FeeType.OwnInstitution);
        hos.setFee(0.0);
        hos.setFfee(0.0);
        hos.setInstitution(getCurrent().getInstitution());
        hos.setServiceSession(current);

        return hos;
    }

    public ItemFee createAgencyFee() {
        ItemFee agency = new ItemFee();
        agency.setName("Agency Fee");
        agency.setFeeType(FeeType.OtherInstitution);
        agency.setFee(0.0);
        agency.setFfee(0.0);
        agency.setServiceSession(current);
        return agency;
    }

    public ItemFee createScanFee() {
        ItemFee scn = new ItemFee();
        scn.setName("Scan Fee");
        scn.setFee(0.0);
        scn.setFfee(0.0);
        scn.setFeeType(FeeType.Service);
        scn.setInstitution(getCurrent().getInstitution());
        scn.setServiceSession(current);
        return scn;
    }

    private void createFees() {
        getItemFees().add(createStaffFee());
        getItemFees().add(createHospitalFee());
        getItemFees().add(createAgencyFee());
        getItemFees().add(createScanFee());
    }

    public void makeNull() {
        speciality = null;
        current = null;
        currentStaff = null;
        filteredValue = null;
        itemFees = null;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        Map m = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getSpeciality() != null) {
                if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

                    sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                            + " and pi.type=:typ "
                            + " and pi.institution=:ins "
                            + " and (upper(pi.staff.person.name) like '%" + query.toUpperCase() + "%'or  upper(pi.staff.code) like '%" + query.toUpperCase() + "%' )"
                            + " and pi.staff.speciality=:spe "
                            + " order by pi.staff.person.name ";

                    m.put("ins", getSessionController().getInstitution());
                    m.put("spe", getSpeciality());
                    m.put("typ", PersonInstitutionType.Channelling);
                } else {
                    sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
                }
            } else {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            ////System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql,m);
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
        ////System.out.println(sql);
        suggestions = getStaffFacade().findBySQL(sql);

        return suggestions;
    }

    public List<ServiceSession> completeSession(String query) {
        List<ServiceSession> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getCurrentStaff() != null) {
                sql = "select p from ServiceSession p where p.retired=false and upper(p.name) like '%" + query.toUpperCase() + "%' and p.staff.id = " + getCurrentStaff().getId() + " order by p.name";
                suggestions = getFacade().findBySQL(sql);
            } else {
                suggestions = new ArrayList<>();
            }

        }
        return suggestions;
    }

    public List<SessionNumberGenerator> getLstSessionNumberGenerator() {
        return lstSessionNumberGenerator;
    }

    public void setLstSessionNumberGenerator(List<SessionNumberGenerator> lstSessionNumberGenerator) {
        this.lstSessionNumberGenerator = lstSessionNumberGenerator;
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

    @EJB
    DepartmentFacade departmentFacade;

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrent().getInstitution().getId();
            d = departmentFacade.findBySQL(sql);
        }

        return d;
    }

    public ServiceSession getCurrent() {
        if (current == null) {
            current = new ServiceSession();
            current.setInstitution(sessionController.getInstitution());
            current.setDepartment(sessionController.getDepartment());
//            createFees();
        }
        return current;
    }

    public SessionNumberGeneratorFacade getSessionNumberGeneratorFacade() {
        return sessionNumberGeneratorFacade;
    }

    public void setSessionNumberGeneratorFacade(SessionNumberGeneratorFacade sessionNumberGeneratorFacade) {
        this.sessionNumberGeneratorFacade = sessionNumberGeneratorFacade;
    }

    public void setCurrent(ServiceSession current) {
        this.current = current;
    }

    public List<ServiceSession> getItems() {
        List<ServiceSession> items;
        String sql;
        HashMap hm = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and s.staff=:stf "
                + " order by s.sessionWeekday,s.startingTime ";
        hm.put("stf", currentStaff);
        //hm.put("class", ServiceSessionLeave.class);
        items = getFacade().findBySQL(sql, hm);
//        }

        return items;
    }

    public void prepareAdd() {
        current = null;
        itemFees = null;
        createFees();
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
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }

        getItems();
        current = null;
        getCurrent();
    }

    private boolean checkError() {
        if (current.getStartingTime() == null) {
            UtilityController.addErrorMessage("Starting time Must be Filled");
            return true;
        }
        if (current.getName() == null || current.getName().trim().equals("")) {
            UtilityController.addErrorMessage("Please Select Session Name");
            return true;
        }

        if (current.getSessionWeekday() == null && getCurrent().getSessionDate() == null) {
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

    public SessionNumberGenerator saveSessionNumber() {
        SessionNumberGenerator sessionNumberGenerator = new SessionNumberGenerator();
        sessionNumberGenerator.setSpeciality(speciality);
        sessionNumberGenerator.setStaff(currentStaff);
        sessionNumberGenerator.setName(currentStaff.getPerson().getName() + " " + current.getName());
        sessionNumberGeneratorFacade.create(sessionNumberGenerator);
        return sessionNumberGenerator;
    }

    public void resetSessionNumbers() {

        String sql;
        sql = " SELECT sg FROM ServiceSession sg WHERE sg.retired=false";
        List<ServiceSession> list = facade.findBySQL(sql);

        for (ServiceSession sng : list) {
            if (sng.getSessionNumberGenerator() != null) {
                continue;
            }
            SessionNumberGenerator sessionNumberGenerator = new SessionNumberGenerator();
            sessionNumberGenerator.setSpeciality(sng.getStaff().getSpeciality());
            sessionNumberGenerator.setStaff(sng.getStaff());
            sessionNumberGenerator.setName(sng.getStaff().getPerson().getName() + " " + sng.getName());
            sessionNumberGeneratorFacade.create(sessionNumberGenerator);

            sng.setSessionNumberGenerator(sessionNumberGenerator);
            facade.edit(sng);
        }

    }

    private void saveFees(ServiceSession serviceSession) {
        if (getItemFees() == null) {
            System.err.println("null");
            return;
        }

        System.err.println("size " + getItemFees().size());

        for (ItemFee i : getItemFees()) {
            i.setServiceSession(serviceSession);
            i.setItem(serviceSession);
            i.setInstitution(serviceSession.getInstitution());
            if (i.getId() == null) {
                i.setCreatedAt(new Date());
                i.setCreater(sessionController.getLoggedUser());
                System.err.println("cRE");
                itemFeeFacade.edit(i);
            } else {
                System.err.println("Edit");
                itemFeeFacade.edit(i);
            }

        }
    }

    public void saveSelected() {
        System.err.println("1 " + getItemFees().size());
        //System.out.println("session name"+current.getName());
        if (checkError()) {
            return;
        }

        System.err.println("1 " + getItemFees().size());
        if (getCurrent().getSessionNumberGenerator() == null) {
            SessionNumberGenerator ss = saveSessionNumber();
            current.setSessionNumberGenerator(ss);
        }

        System.err.println("1 " + getItemFees().size());

        getCurrent().setStaff(currentStaff);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            System.out.println("getCurrent().getId() = " + getCurrent().getId());
            System.err.println("edit Ses");
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            System.out.println("getCurrent().getId() = " + getCurrent().getId());
            System.err.println("cre Ses");
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        System.err.println("1 " + getItemFees().size());

        saveFees(getCurrent());

        getCurrent().setTotal(calTot());
        getCurrent().setTotalFfee(calFTot());

        facade.edit(getCurrent());

        prepareAdd();
        getItems();
    }

    private double calTot() {
        double tot = 0.0;
        for (ItemFee i : getItemFees()) {
            tot += i.getFee();
        }
        return tot;
    }

    private double calFTot() {
        double tot = 0.0;
        for (ItemFee i : getItemFees()) {
            tot += i.getFfee();
        }
        return tot;
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

}
