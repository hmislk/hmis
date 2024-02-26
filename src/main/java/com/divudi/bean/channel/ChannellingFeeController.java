/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;

import com.divudi.data.FeeType;
import com.divudi.entity.Department;
import com.divudi.entity.Fee;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ChannellingFeeController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;

    @EJB
    private ItemFacade itemFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;

    private Speciality speciality;
    private Staff doctor;
    private Item session;
    private ItemFee fee;
    private ItemFee removingFee;

    ServiceSession serviceSession;
    Fee doctorFee;
    Fee hospitalFee;
    Fee scanFee;
    boolean includeScanning;

    List<ServiceSession> sessions;
    private List<Staff> doctors;
    private List<ItemFee> fees;
    List<Department> departments = new ArrayList<>();
    List<Staff> staffSuggestions;

    public void prepareEditSessionForAnyDay() {
        if (serviceSession == null) {
            JsfUtil.addErrorMessage("Session not selected");
            return;
        }
        if (serviceSession.getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("Error in setting originating session. Contact Developers.");
            return;
        }
        if (serviceSession.getOriginatingSession().getStaff() == null) {
            JsfUtil.addErrorMessage("Consultant not set in session. Contact developer");
            return;
        } else {
            doctor = serviceSession.getOriginatingSession().getStaff();
            speciality = doctor.getSpeciality();
        }
        if (serviceSession.getItemFees() == null || serviceSession.getItemFees().isEmpty()) {
            createAllFeesForServiceSession();
        } else {

        }
    }

//    public SessionNumberGenerator saveSessionNumber() {
//        SessionNumberGenerator sessionNumberGenerator = new SessionNumberGenerator();
//        sessionNumberGenerator.setSpeciality(speciality);
//        sessionNumberGenerator.setStaff(doctor);
//        sessionNumberGenerator.setName(doctor.getPerson().getName() + " " + serviceSession.getOriginatingSession().getName());
//        sessionNumberGeneratorFacade.create(sessionNumberGenerator);
//        return sessionNumberGenerator;
//    }

    public void createAllFeesForServiceSession() {
        createAllFeesForServiceSessionWithoutScanning();
        createAllFeesForServiceSessionWithScanning();
    }

    public void createAllFeesForServiceSessionWithoutScanning() {
        createConsultantFee();
        createHospitalFee();
    }

    public void createAllFeesForServiceSessionWithScanning() {
        createScanningConsultationFee();
        createScanningHospitalFee();
    }

    public void createConsultantFee() {
        Fee f = new Fee();
        f.setCreatedAt(new Date());
        f.setCreater(getSessionController().getLoggedUser());
        f.setFee(0.0);
        f.setFfee(0.0);
        f.setSpeciality(speciality);
        f.setStaff(doctor);
        f.setFeeType(FeeType.Staff);
        f.setName("Consultant Fee");
    }

    public void createHospitalFee() {
        Fee f = new Fee();
        f.setCreatedAt(new Date());
        f.setCreater(getSessionController().getLoggedUser());
        f.setFee(0.0);
        f.setFfee(0.0);
        f.setInstitution(getSessionController().getInstitution());
        f.setDepartment(getSessionController().getDepartment());
        f.setFeeType(FeeType.OwnInstitution);
        f.setName("Hospital Fee");
    }

    public void createScanningConsultationFee() {
        Fee f = new Fee();
        f.setCreatedAt(new Date());
        f.setCreater(getSessionController().getLoggedUser());
        f.setFee(0.0);
        f.setFfee(0.0);
        f.setSpeciality(speciality);
        f.setStaff(doctor);
        f.setFeeType(FeeType.Staff);
        f.setName("Scanning Consultant Fee");
    }

    public void createScanningHospitalFee() {
        Fee f = new Fee();
        f.setCreatedAt(new Date());
        f.setCreater(getSessionController().getLoggedUser());
        f.setFee(0.0);
        f.setFfee(0.0);
        f.setInstitution(getSessionController().getInstitution());
        f.setDepartment(getSessionController().getDepartment());
        f.setFeeType(FeeType.OwnInstitution);
        f.setName("Scanning Hospital Fee");

    }

    public List<Department> getInstitutionDepatrments() {
        if (getFee().getInstitution() == null) {
            departments = new ArrayList<>();
            return departments;
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution=:ins order by d.name";
            Map m = new HashMap();
            m.put("ins", getFee().getInstitution());
            departments = getDepartmentFacade().findByJpql(sql, m);
        }
        return departments;
    }

    public List<Staff> completeStaff(String query) {
        String sql;
        if (query == null) {
            staffSuggestions = new ArrayList<>();
        } else {
            Map m = new HashMap();
            m.put("qry", "%" + query.toUpperCase() + "%");
            if (getFee().getSpeciality() == null) {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like :qry or (p.code) like :qry ) order by p.person.name";
            } else {
                sql = "select p from Staff p where p.speciality=:spe and p.retired=false and ((p.person.name) like :qry or  (p.code) like :qry) order by p.person.name";
                m.put("spe", getFee().getSpeciality());
            }
            staffSuggestions = getStaffFacade().findByJpql(sql, m);

        }
        return staffSuggestions;
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

//    public void prepareAdd() {
//        ////// // System.out.println("prepairing to add");
//        fee = new ItemFee();
//        ////// // System.out.println("fee = " + fee);
//    }

    public void fillSessions() {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and s.staff=:doc "
                + " order by s.sessionWeekday, s.sessionAt";
        m.put("doc", doctor);
        sessions = getServiceSessionFacade().findByJpql(sql, m);
    }

    public void fillFees() {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false and "
                + " f.item=:ses "
                + " order by f.id";
        m.put("ses", session);
        fees = getItemFeeFacade().findByJpql(sql, m);
    }

    public void saveCharge() {
        if (session == null) {
            JsfUtil.addErrorMessage("Please select a session");
            return;
        }
        if (fee == null) {
            JsfUtil.addErrorMessage("Please select a fee");
            return;
        }
        fee.setItem(session);
        if (fee.getId() == null || fee.getId() == 0) {
            fee.setCreatedAt(new Date());
            fee.setCreater(getSessionController().getLoggedUser());
            getItemFeeFacade().create(fee);
            JsfUtil.addSuccessMessage("Fee Added");
        } else {
            getItemFeeFacade().edit(fee);
            JsfUtil.addSuccessMessage("Fee Saved");
        }
        fillFees();
        session.setTotal(calTot());
        getItemFacade().edit(session);

    }

    private double calTot() {
        double tot = 0.0;
        for (ItemFee i : getFees()) {
            tot += i.getFee();
        }
        return tot;
    }

    public ChannellingFeeController() {
    }

    public void delete() {
        if (removingFee != null) {
            removingFee.setRetired(true);
            removingFee.setRetiredAt(new Date());
            removingFee.setRetirer(getSessionController().getLoggedUser());
            getItemFeeFacade().edit(removingFee);
            JsfUtil.addSuccessMessage("Deleted Successfull");
        } else {
            JsfUtil.addSuccessMessage("Nothing To Delete");
        }
        fillFees();
        session.setTotal(calTot());
        setRemovingFee(null);
        setFee(null);
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
        setDoctors(null);
    }

    public Staff getDoctor() {
        return doctor;
    }

    public void setDoctor(Staff doctor) {
        this.doctor = doctor;
        setSessions(null);
    }

    public List<Staff> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<Staff> doctors) {
        this.doctors = doctors;
        setDoctor(null);
    }

    public List<ServiceSession> getSessions() {
        if (sessions == null) {
            fillSessions();
        }
        return sessions;
    }

    public void setSessions(List<ServiceSession> sessions) {
        this.sessions = sessions;
        setSession(null);
    }

    public Item getSession() {
        return session;
    }

    public void setSession(Item session) {
        this.session = session;
        setFees(null);
    }

    public ItemFee getFee() {
        if (fee == null) {
            fee = new ItemFee();
        }
        return fee;
    }

    public void setFee(ItemFee fee) {
        this.fee = fee;
    }

    public ItemFee getRemovingFee() {
        return removingFee;
    }

    public void setRemovingFee(ItemFee removingFee) {
        this.removingFee = removingFee;
    }

    public List<ItemFee> getFees() {
        if (fees == null) {
            fillFees();
        }
        return fees;
    }

    public void setFees(List<ItemFee> fees) {
        this.fees = fees;
        setFee(null);
        setRemovingFee(null);
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public ServiceSession getServiceSession() {
        return serviceSession;
    }

    public void setServiceSession(ServiceSession serviceSession) {
        this.serviceSession = serviceSession;
    }

    public Fee getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(Fee doctorFee) {
        this.doctorFee = doctorFee;
    }

    public Fee getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Fee hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Fee getScanFee() {
        return scanFee;
    }

    public void setScanFee(Fee scanFee) {
        this.scanFee = scanFee;
    }

    public List<Staff> getStaffSuggestions() {
        return staffSuggestions;
    }

    public void setStaffSuggestions(List<Staff> staffSuggestions) {
        this.staffSuggestions = staffSuggestions;
    }

}
