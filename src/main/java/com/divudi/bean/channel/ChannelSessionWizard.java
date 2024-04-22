/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.FeeType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import com.divudi.facade.StaffFacade;
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
import org.primefaces.event.FlowEvent;

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
    private ItemFee hospitalFee;
    private ItemFee staffFee;
    private ItemFee scanFee;
    private List<ItemFee> fees;
    List<Department> departments = new ArrayList<>();
    List<Staff> doctors = new ArrayList<>();
    @EJB
    StaffFacade staffFacade;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @Inject
    SessionController sessionController;

    public ChannelSessionWizard() {
    }

    public void makeNullAll() {
        current = null;
        speciality = null;
        currentStaff = null;
    }

    public void prepareAdd() {
        current = new ServiceSession();
        createHospitalFee();
        createScanFee();
        createStaffFee();
//        speciality = null;
//        currentStaff = null;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getServiceSessionFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getServiceSessionFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
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
            JsfUtil.addErrorMessage("Starting time Must be Filled");
            return true;
        }

        if (getCurrent().getSessionWeekday() == null && getCurrent().getSessionDate() == null) {
            JsfUtil.addErrorMessage("Set Weekday or Date");
            return true;
        }

        if (speciality == null) {
            JsfUtil.addErrorMessage("Plaese Select Specility");
            return true;
        }

        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Plaese Select Doctor");
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
                sql = "select p from ServiceSession p where p.retired=false and p.name like '%" + query.toUpperCase() + "%' and p.staff.id = " + getCurrentStaff().getId() + " order by p.name";
                suggestions = getServiceSessionFacade().findByJpql(sql);
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
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //////// // System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public List<Staff> completeDoctors(String query) {
        String sql;
        Map m = new HashMap();
        if (query == null) {
            doctors = new ArrayList<>();
        } else {
            m.put("qry", "%" + query.toUpperCase() + "%");
            if (speciality == null) {
                sql = "select p from Staff p "
                        + " where p.retired=false "
                        + " and ((p.person.name) like :qry "
                        + " or (p.code) like :qry ) "
                        + " order by p.person.name";
            } else {
                sql = "select p from Staff p "
                        + " where p.speciality=:spe "
                        + " and p.retired=false "
                        + " and ((p.person.name) like :qry "
                        + " or  (p.code) like :qry) "
                        + " order by p.person.name";
                m.put("spe", speciality);
            }
            doctors = getStaffFacade().findByJpql(sql, m);
        }
        return doctors;
    }

    public List<Department> getInstitutionDepatrments(Institution institution) {
        if (institution == null) {
            departments = new ArrayList<>();
            return departments;
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution=:ins order by d.name";
            Map m = new HashMap();
            m.put("ins", institution);
            departments = getDepartmentFacade().findByJpql(sql, m);
        }
        return departments;
    }

    public List<ServiceSession> getItems() {
        List<ServiceSession> items;
        String sql;
        HashMap hm = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and s.staff=:stf ";
        hm.put("stf", currentStaff);
        items = getServiceSessionFacade().findByJpql(sql, hm);
        return items;
    }

    public void createStaffFee() {
        staffFee = new ItemFee();
        staffFee.setName("Staff Fee");
        staffFee.setFeeType(FeeType.Staff);
        staffFee.setFee(0.0);
        staffFee.setFfee(0.0);
        staffFee.setInstitution(getSessionController().getLoggedUser().getInstitution());
        staffFee.setSpeciality(speciality);
        staffFee.setStaff(currentStaff);
        staffFee.setServiceSession(current);
        getItemFeeFacade().create(staffFee);
    }

    public void createHospitalFee() {
        hospitalFee = new ItemFee();
        hospitalFee.setName("Hospital Fee");
        hospitalFee.setFeeType(FeeType.OwnInstitution);
        hospitalFee.setFee(0.0);
        hospitalFee.setFfee(0.0);
        hospitalFee.setInstitution(getSessionController().getLoggedUser().getInstitution());
        if (current != null && current.getId() != null) {
            hospitalFee.setServiceSession(current);
        }
        getItemFeeFacade().create(hospitalFee);
    }

    public void createScanFee() {
        scanFee = new ItemFee();
        scanFee.setName("Scan Fee");
        scanFee.setFee(0.0);
        scanFee.setFfee(0.0);
        scanFee.setFeeType(FeeType.Service);
        scanFee.setInstitution(getSessionController().getLoggedUser().getInstitution());
        scanFee.setServiceSession(current);
        getItemFeeFacade().create(scanFee);
    }

    public void fillFees() {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false and "
                + " f.serviceSession=:ses "
                + " order by f.id";
        m.put("ses", current);
        fees = getItemFeeFacade().findByJpql(sql, m);
    }

    public void sessionListner() {
        if (createNewSession == true) {
            prepareAdd();
        }
    }

    public ServiceSession getCurrent() {
        return current;
    }

    public void scanFeeListner() {
        if (current.isScanFee()) {
            String sql;
            Map m = new HashMap();
            sql = "Select f from ItemFee f "
                    + " where f.retired=false "
                    + " and f.serviceSession=:ses "
                    + " and f.serviceSession.scanFee=true "
                    + " order by f.id";
            m.put("ses", current);
            fees = getItemFeeFacade().findByJpql(sql, m);

            if (fees.isEmpty()) {
                createScanFee();
                ////// // System.out.println("Scan Fee Created");
            }
        }
    }

    public void feesListner() {

//        ////// // System.out.println("view");
//        fillFees();
//        ////// // System.out.println("fees = " + fees);
//        if (fees.isEmpty()) {
//            ////// // System.out.println("Create New Fees");
        createHospitalFee();
        createStaffFee();

        if (current.isScanFee()) {
            createScanFee();
        }
//        }

        fillFees();
    }

    public void updateItemFee(ItemFee itemFee) {
        getItemFeeFacade().edit(itemFee);
        JsfUtil.addSuccessMessage("Sucessfull Updated");
    }

    private void createNewSession() {
        current = new ServiceSession();
        current.setCreatedAt(new Date());
        current.setCreater(sessionController.getLoggedUser());
        current.setSpeciality(speciality);
        current.setStaff(currentStaff);
        serviceSessionFacade.create(current);
    }

    public String onFlowProcess(FlowEvent event) {

        String phase = event.getPhaseId().getName();

        switch (event.getNewStep()) {
            case "speciality":
                break;
            case "doctor":
                break;
            case "check":
                if (createNewSession && current != null) {
                    createNewSession();
                    feesListner();
                    break;
                }
            default:
                break;

        }

        return event.getNewStep();

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

    public List<ItemFee> getFees() {
        if (fees == null) {
            fees = new ArrayList<>();
        }

        return fees;
    }

    public void setFees(List<ItemFee> fees) {
        this.fees = fees;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public ItemFee getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(ItemFee hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public ItemFee getStaffFee() {
        return staffFee;
    }

    public void setStaffFee(ItemFee staffFee) {
        this.staffFee = staffFee;
    }

    public ItemFee getScanFee() {
        return scanFee;
    }

    public void setScanFee(ItemFee scanFee) {
        this.scanFee = scanFee;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

}
