/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.FeeChangeType;
import com.divudi.data.FeeType;
import com.divudi.data.PersonInstitutionType;
import com.divudi.ejb.ChannelBean;

import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.StockHistoryRecorder;
import com.divudi.entity.Department;
import com.divudi.entity.FeeChange;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionInstance;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FeeChangeFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class SheduleController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade facade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    FeeChangeFacade feeChangeFacade;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    StockHistoryRecorder stockHistoryRecorder;
    @EJB
    ChannelBean channelBean;
    @EJB
    FinalVariables finalVariables;
    /**
     * Controllers
     */
    @Inject
    private SessionController sessionController;

    CommonFunctions commonFunctions;
    /**
     * Class Variables
     */
    private Speciality speciality;
    ServiceSession current;
    private Staff currentStaff;
    private List<ServiceSession> filteredValue;
    List<SessionNumberGenerator> lstSessionNumberGenerator;
    List<ItemFee> itemFees;
    //change Fee
    Date effectiveDate;
    List<FeeChange> feeChanges;
    List<FeeChange> feeChangesList;
    boolean feeChangeStaff;

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
        itemFees = itemFeeFacade.findByJpql(sql, m);
    }

    public ItemFee createStaffFee() {
        ItemFee stf = new ItemFee();
        stf.setName("Doctor Fee");
        stf.setFeeType(FeeType.Staff);
        stf.setFee(0.0);
        stf.setFfee(0.0);
        stf.setDiscountAllowed(false);
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
        hos.setDiscountAllowed(true);
        hos.setFee(0.0);
        hos.setFfee(0.0);
        hos.setInstitution(getCurrent().getInstitution());
        hos.setDepartment(current.getDepartment());
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

    public ItemFee createOnCallFee() {
        ItemFee onc = new ItemFee();
        onc.setName("On-Call Fee");
        onc.setFeeType(FeeType.OwnInstitution);
        onc.setFee(0.0);
        onc.setFfee(0.0);
        onc.setInstitution(getCurrent().getInstitution());
        onc.setServiceSession(current);
        return onc;
    }

    private void createFees() {
        getItemFees().add(createStaffFee());
        getItemFees().add(createHospitalFee());
        getItemFees().add(createAgencyFee());
        getItemFees().add(createScanFee());
        getItemFees().add(createOnCallFee());
    }

    public void makeNull() {
        speciality = null;
        current = null;
        currentStaff = null;
        filteredValue = null;
        itemFees = null;
        prepareAdd();
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        Map m = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getSpeciality() != null) {
                if (getSessionController().getLoggedPreference().isShowOnlyMarkedDoctors()) {

                    sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                            + " and pi.type=:typ "
                            + " and pi.institution=:ins "
                            + " and ((pi.staff.person.name) like '%" + query.toUpperCase() + "%'or  (pi.staff.code) like '%" + query.toUpperCase() + "%' )"
                            + " and pi.staff.speciality=:spe "
                            + " order by pi.staff.person.name ";

                    m.put("ins", getSessionController().getInstitution());
                    m.put("spe", getSpeciality());
                    m.put("typ", PersonInstitutionType.Channelling);
                } else {
                    sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
                }
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //////// // System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql, m);
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
        //////// // System.out.println(sql);
        suggestions = getStaffFacade().findByJpql(sql);

        return suggestions;
    }

    public List<ServiceSession> completeSession(String query) {
        List<ServiceSession> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getCurrentStaff() != null) {
                sql = "select p from ServiceSession p where p.retired=false and (p.name) like '%" + query.toUpperCase() + "%' and p.staff.id = " + getCurrentStaff().getId() + " order by p.name";
                suggestions = getFacade().findByJpql(sql);
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
            d = departmentFacade.findByJpql(sql);
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
                + " and type(s)=:class "
                + " and s.staff=:stf "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime ";
        hm.put("stf", currentStaff);
        hm.put("class", ServiceSession.class);
        items = getFacade().findByJpql(sql, hm);
        List<ServiceSession> tmp = new ArrayList<>();
        for (ServiceSession i : items) {
            if (i.getSessionDate() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(i.getSessionDate());
                Calendar calNow = Calendar.getInstance();
                if (cal.get(Calendar.YEAR) <= calNow.get(Calendar.YEAR)) {
                    if (cal.get(Calendar.MONTH) <= calNow.get(Calendar.MONTH)) {
                        if (cal.get(Calendar.DATE) < calNow.get(Calendar.DATE)) {
                            tmp.add(i);
                        }
                    }
                }

            }
        }
        items.removeAll(tmp);

        return items;
    }

    public List<ItemFee> fetchStaffServiceSessions() {
        String sql;
        HashMap m = new HashMap();
        sql = "Select i From ItemFee i join i.serviceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.originatingSession is null ";
        if (currentStaff != null) {
            sql += " and s.staff=:stf ";
            m.put("stf", currentStaff);
        }
        sql += " order by s.staff.person.name,s.sessionWeekday,s.startingTime ";

        m.put("class", ServiceSession.class);

        return itemFeeFacade.findByJpql(sql, m);
    }

    public void prepareAdd() {
        current = new ServiceSession();
        current.setInstitution(getSessionController().getInstitution());
        current.setDepartment(getSessionController().getDepartment());
        itemFees = new ArrayList<>();
        createFees();
    }

    public void prepareAddFeeChange() {
        prepareAdd();
        createChangeFees();
    }

    public ServiceSessionFacade getFacade() {
        return facade;
    }

    public void setFacade(ServiceSessionFacade facade) {
        this.facade = facade;
    }

    public void delete() {

        if (checkError(current)) {
            JsfUtil.addErrorMessage("you can't Remove This Shedule.Because This Shedule has Channeling Bills");
            return;
        }

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            if (checkSessionNumberGenerater(current)) {
                current.getSessionNumberGenerator().setRetired(true);
                current.getSessionNumberGenerator().setRetiredAt(new Date());
                current.getSessionNumberGenerator().setRetirer(getSessionController().getLoggedUser());
                sessionNumberGeneratorFacade.edit(current.getSessionNumberGenerator());
            }
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

        if (current.getSessionWeekday() != null && getCurrent().getSessionDate() != null) {
            UtilityController.addErrorMessage("Ycan Select Only Weekday or Date");
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

        if (getSessionController().getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative
                && current.getForBillType() == null) {
            UtilityController.addErrorMessage("Plaese Select Channel Type");
            return true;
        }

        return false;
    }

    private boolean checkError(ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        sql = " select bs.serviceSession from BillSession bs where "
                + " bs.retired=false "
                + " and bs.serviceSession.originatingSession=:ss "
                + " and bs.serviceSession.sessionDate>=:nd";
        m.put("ss", ss);
        m.put("nd", new Date());
        List<ServiceSession> sss = getFacade().findByJpql(sql, m, TemporalType.DATE);
//        double d=getFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);
        return sss.size() > 0;
    }

    public boolean checkSessionNumberGenerater(ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.sessionNumberGenerator=:sg "
                + " and s!=:ss "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime ";
        m.put("sg", ss.getSessionNumberGenerator());
        m.put("ss", ss);
        m.put("class", ServiceSession.class);
        List<ServiceSession> sss = getFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        sss = getFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        return sss.isEmpty();
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
        List<ServiceSession> list = facade.findByJpql(sql);

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
            return;
        }

        for (ItemFee i : getItemFees()) {
            i.setServiceSession(serviceSession);
            i.setItem(serviceSession);
            i.setInstitution(serviceSession.getInstitution());
            if (i.getId() == null) {
                i.setCreatedAt(new Date());
                i.setCreater(sessionController.getLoggedUser());
                itemFeeFacade.create(i);
            } else {
                itemFeeFacade.edit(i);
            }

        }
    }

    public void saveSelected() {
        ////// // System.out.println("session name"+current.getName());
        if (checkError()) {
            return;
        }

        if (getCurrent().getSessionNumberGenerator() == null) {
            SessionNumberGenerator ss = saveSessionNumber();
            current.setSessionNumberGenerator(ss);
        }

        getCurrent().setStaff(currentStaff);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getCurrent().setEditer(getSessionController().getLoggedUser());
            getCurrent().setEditedAt(new Date());
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            generateSessions(currentStaff);
            UtilityController.addSuccessMessage("Saved Successfully");
        }

        saveFees(getCurrent());

        getCurrent().setTotal(calTot());
        getCurrent().setTotalFfee(calFTot());

        facade.edit(getCurrent());
        updateCreatedServicesesions(getCurrent());
        prepareAdd();
        getItems();
    }

    public void createFutureSessionsManually() {
        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Pease Select Doctor");
            return;
        }
        generateSessions(currentStaff);
    }

    public void generateSessions(Staff staff) {
        String sql;
        Map m = new HashMap();
        m.put("staff", staff);
        m.put("class", ServiceSession.class);
        if (staff != null) {
            sql = "Select s.id From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.originatingSession is null "
                    + " and type(s)=:class "
                    + " order by s.sessionWeekday,s.startingTime ";
            List<Long> serviceSessionIds = new ArrayList<>();
            serviceSessionIds = serviceSessionFacade.findLongList(sql, m);

            if (serviceSessionIds.isEmpty()) {
                return;
            }
            generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionId(serviceSessionIds, null);
        }
    }

    public void generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionId(List<Long> inputSessionIds, Date d) {
        int sessionDayCount = 0;
        List<ServiceSessionInstance> createdSessions = new ArrayList<>();

        if (inputSessionIds == null || inputSessionIds.isEmpty()) {
            return;
        }
        Date nowDate;
        if (d == null) {
            nowDate = Calendar.getInstance().getTime();
        } else {
            nowDate = d;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(nowDate);
        c.add(Calendar.MONTH, 2);
        Date toDate = c.getTime();
        Integer tmp = 0;
        int rowIndex = 0;
        List<ServiceSession> sessions = new ArrayList<>();
        int finalSessionDayCount = 10;
        while (toDate.after(nowDate) && sessionDayCount < finalSessionDayCount) {
            if (sessions.isEmpty()) {
                for (Long s : inputSessionIds) {
                    ServiceSession ss = serviceSessionFacade.find(s);
                    sessions.add(ss);
                    if (ss.getSessionDate() != null) {
                        Calendar sessionDate = Calendar.getInstance();
                        sessionDate.setTime(ss.getSessionDate());
                        Calendar nDate = Calendar.getInstance();
                        nDate.setTime(nowDate);
                        if (sessionDate.get(Calendar.DATE) == nDate.get(Calendar.DATE) && sessionDate.get(Calendar.MONTH) == nDate.get(Calendar.MONTH) && sessionDate.get(Calendar.YEAR) == nDate.get(Calendar.YEAR)) {
                            ServiceSessionInstance newSs = new ServiceSessionInstance();
                            newSs = channelBean.fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = channelBean.createServiceSessionForChannelShedule(ss, nowDate);
                            }
                            //Temprory
//                            newSs.setDisplayCount(channelBean.getBillSessionsCount(ss, nowDate));
//                            newSs.setTransDisplayCountWithoutCancelRefund(channelBean.getBillSessionsCountWithOutCancelRefund(ss, nowDate));
//                            newSs.setTransCreditBillCount(channelBean.getBillSessionsCountCrditBill(ss, nowDate));
                            newSs.setStaff(ss.getStaff());
//                            newSs.setTransRowNumber(rowIndex++);
                            //add to list

                            createdSessions.add(newSs);
//                            checkDoctorArival(newSs);
//                            ss.setServiceSessionCreateForOriginatingSession(true);
                            if (Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    } else {
                        Calendar wdc = Calendar.getInstance();
                        wdc.setTime(nowDate);
                        if (ss.getSessionWeekday() != null && (ss.getSessionWeekday() == wdc.get(Calendar.DAY_OF_WEEK))) {
                            ServiceSessionInstance newSs =  channelBean.fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = new ServiceSessionInstance();
//                            System.err.println("Cretate New");
                                newSs = channelBean.createServiceSessionForChannelShedule(ss, nowDate);
                            }
//                        //// // System.out.println("newSs = " + newSs);
                            //Temprory
//                            newSs.setDisplayCount(channelBean.getBillSessionsCount(newSs, nowDate));
//                            newSs.setTransDisplayCountWithoutCancelRefund(channelBean.getBillSessionsCountWithOutCancelRefund(newSs, nowDate));
//                            newSs.setTransCreditBillCount(channelBean.getBillSessionsCountCrditBill(newSs, nowDate));
                            newSs.setTransRowNumber(rowIndex++);
                            //add to list
                            createdSessions.add(newSs);
//                            checkDoctorArival(newSs);
//                            ss.setServiceSessionCreateForOriginatingSession(true);
                            if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    }
                }
            } else {
                for (ServiceSession ss : sessions) {

                    if (ss.getSessionDate() != null) {
                        Calendar sessionDate = Calendar.getInstance();
                        sessionDate.setTime(ss.getSessionDate());
                        Calendar nDate = Calendar.getInstance();
                        nDate.setTime(nowDate);
                        if (sessionDate.get(Calendar.DATE) == nDate.get(Calendar.DATE) && sessionDate.get(Calendar.MONTH) == nDate.get(Calendar.MONTH) && sessionDate.get(Calendar.YEAR) == nDate.get(Calendar.YEAR)) {
                            ServiceSessionInstance newSs = channelBean.fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = channelBean.createServiceSessionForChannelShedule(ss, nowDate);
                            }
                            //Temprory
//                            newSs.setDisplayCount(channelBean.getBillSessionsCount(ss, nowDate));
//                            newSs.setTransDisplayCountWithoutCancelRefund(channelBean.getBillSessionsCountWithOutCancelRefund(ss, nowDate));
//                            newSs.setTransCreditBillCount(channelBean.getBillSessionsCountCrditBill(ss, nowDate));
                            newSs.setStaff(ss.getStaff());
//                            newSs.setTransRowNumber(rowIndex++);
                            //add to list
                            createdSessions.add(newSs);
//                            checkDoctorArival(newSs);
//                            ss.setServiceSessionCreateForOriginatingSession(true);
                            if (Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    } else {
                        Calendar wdc = Calendar.getInstance();
                        wdc.setTime(nowDate);
                        if (ss.getSessionWeekday() != null && (ss.getSessionWeekday() == wdc.get(Calendar.DAY_OF_WEEK))) {
                            ServiceSessionInstance newSs = channelBean.fetchCreatedServiceSession(ss.getStaff(), nowDate, ss);
                            if (newSs == null) {
                                newSs = new ServiceSessionInstance();
//                            System.err.println("Cretate New");
                                newSs = channelBean.createServiceSessionForChannelShedule(ss, nowDate);
                            }
//                        //// // System.out.println("newSs = " + newSs);
                            //Temprory
//                            newSs.setDisplayCount(channelBean.getBillSessionsCount(newSs, nowDate));
//                            newSs.setTransDisplayCountWithoutCancelRefund(channelBean.getBillSessionsCountWithOutCancelRefund(newSs, nowDate));
//                            newSs.setTransCreditBillCount(channelBean.getBillSessionsCountCrditBill(newSs, nowDate));
//                            newSs.setTransRowNumber(rowIndex++);
                            //add to list
                            createdSessions.add(newSs);
////                            checkDoctorArival(newSs);
//                            ss.setServiceSessionCreateForOriginatingSession(true);
                            if (!Objects.equals(tmp, ss.getSessionWeekday())) {
                                sessionDayCount++;
                            }
                        }
                    }
                }
            }

            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);
            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();

        }

    }

    public void updateCreatedServicesesions(ServiceSession ss) {
        //// // System.out.println("ss.getName() = " + ss.getName());
        //// // System.out.println("ss.getInstitution() = " + ss.getInstitution());
        for (ServiceSession i : fetchCreatedServiceSessions(ss)) {
            //// // System.out.println("i.getName() = " + i.getName());
            //// // System.out.println("i.getInstitution() = " + i.getInstitution());
            //// // System.out.println("i.getDepartment() = " + i.getDepartment());
            //// // System.out.println("i.getStartingTime() = " + i.getStartingTime());

            i.setName(ss.getName());
            i.setInstitution(ss.getInstitution());
            i.setDepartment(ss.getDepartment());
            i.setStartingTime(ss.getStartingTime());
            i.setEndingTime(ss.getEndingTime());
            i.setMaxNo(ss.getMaxNo());
            i.setDuration(ss.getDuration());
            i.setRoomNo(ss.getRoomNo());
            i.setAfterSession(ss.getAfterSession());
            i.setBeforeSession(ss.getBeforeSession());
            i.setDisplayCount(ss.getDisplayCount());
            i.setDisplayPercent(ss.getDisplayPercent());
            i.setRefundable(ss.isRefundable());
            i.setCreditNumbers(ss.getCreditNumbers());
            i.setCashNumbers(ss.getCashNumbers());
            i.setAgencyNumbers(ss.getAgencyNumbers());
            i.setReserveNumbers(ss.getReserveNumbers());
            i.setReserveName(ss.getReserveName());
            i.setMaxTableRows(ss.getMaxTableRows());
            i.setSessionWeekday(ss.getSessionWeekday());

            getFacade().edit(i);
        }
    }

    public List<ServiceSession> fetchCreatedServiceSessions(ServiceSession ss) {
        List<ServiceSession> items;
        String sql;
        HashMap m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.originatingSession=:ss"
                + " and s.sessionDate>=:sd "
                + " order by s.sessionWeekday,s.startingTime ";
        m.put("ss", ss);
        m.put("class", ServiceSession.class);
        m.put("sd", commonFunctions.getStartOfDay());
        items = getFacade().findByJpql(sql, m);
        return items;
    }

    public void createOnCallFeeOldSession() {
        String sql;
        Map m = new HashMap();
        sql = "Select DISTINCT(f.serviceSession) from ItemFee f "
                + " where f.retired=false "
                + " and f.serviceSession is not null ";
        List<ServiceSession> serviceSessionsAll = serviceSessionFacade.findByJpql(sql);
        for (ServiceSession s : serviceSessionsAll) {

        }
        List<ServiceSession> tmpList = new ArrayList<>();
        tmpList.addAll(serviceSessionsAll);
        tmpList.removeAll(fetchSessionByFee("On-Call Fee", FeeType.OwnInstitution));
        createFeesForServiceSessionList(tmpList, "On-Call Fee", FeeType.OwnInstitution);

        tmpList.addAll(serviceSessionsAll);
        tmpList.removeAll(fetchSessionByFee("Scan Fee", FeeType.Service));
        createFeesForServiceSessionList(tmpList, "Scan Fee", FeeType.Service);

        tmpList.addAll(serviceSessionsAll);
        tmpList.removeAll(fetchSessionByFee("Agency Fee", FeeType.OtherInstitution));
        createFeesForServiceSessionList(tmpList, "Agency Fee", FeeType.OtherInstitution);

        tmpList.addAll(serviceSessionsAll);
        tmpList.removeAll(fetchSessionByFee("Hospital Fee", FeeType.OwnInstitution));
        createFeesForServiceSessionList(tmpList, "Hospital Fee", FeeType.OwnInstitution);

        tmpList.addAll(serviceSessionsAll);
        tmpList.removeAll(fetchSessionByFee("Doctor Fee", FeeType.Staff));
        createFeesForServiceSessionList(tmpList, "Doctor Fee", FeeType.Staff);

//        List<ServiceSession> serviceSessions = serviceSessionFacade.findByJpql(sql, m);
//        //// // System.out.println("serviceSessions.size() = " + serviceSessions.size());
//        serviceSessionsAll.removeAll(serviceSessions);
//        for (ServiceSession ss : serviceSessionsAll) {
//            ItemFee onc = new ItemFee();
//            onc.setName("On-Call Fee");
//            onc.setFeeType(FeeType.OwnInstitution);
//            onc.setFee(0.0);
//            onc.setFfee(0.0);
//            onc.setInstitution(ss.getInstitution());
//            onc.setServiceSession(ss);
//            onc.setItem(ss);
//            itemFeeFacade.create(onc);
//        }
    }

    public List<ServiceSession> fetchSessionByFee(String feeName, FeeType feeType) {
        List<ServiceSession> list = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select DISTINCT(f.serviceSession) from ItemFee f "
                + " where f.retired=false "
                + " and f.serviceSession is not null "
                + " and f.feeType=:fType "
                + " and f.name='" + feeName + "'"
                + " order by f.id";
        m.put("fType", feeType);
        list = serviceSessionFacade.findByJpql(sql, m);
        return list;

    }

    public void createFee(ServiceSession ss, String name, FeeType ft) {
        ItemFee itemFee = new ItemFee();
        itemFee.setName(name);
        itemFee.setFeeType(ft);
        itemFee.setFee(0.0);
        itemFee.setFfee(0.0);
        itemFee.setInstitution(getCurrent().getInstitution());
        itemFee.setServiceSession(ss);
        if (ft == FeeType.Staff) {
            try {
                if (ss.getStaff() != null && ss.getStaff().getSpeciality() != null) {
                    itemFee.setSpeciality(ss.getStaff().getSpeciality());
                    itemFee.setStaff(ss.getStaff());
                } else {
                    return;
                }
            } catch (Exception e) {
                return;
            }
        }
        itemFeeFacade.create(itemFee);
    }

    public void createFeesForServiceSessionList(List<ServiceSession> serviceSessions, String name, FeeType ft) {
        for (ServiceSession ss : serviceSessions) {
            createFee(ss, name, ft);
        }
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

    public void createChangeFees() {
        feeChanges = new ArrayList<>();
        for (ItemFee f : itemFees) {
            FeeChange fc = new FeeChange();
            fc.setFee(f);
            fc.getFee().setStaff(null);
            fc.getFee().setSpeciality(null);
            fc.getFee().setServiceSession(null);
            fc.setFeeChangeType(FeeChangeType.Channel);
            fc.setDone(false);
            feeChanges.add(fc);
        }
    }

    public void saveFeeChanges() {
        Date nowDate = getCommonFunctions().getEndOfDay(new Date());
        if (nowDate.before(effectiveDate)) {
            JsfUtil.addErrorMessage("Please Select Future Date");
            return;
        }
        if (feeChangeStaff) {
            if (speciality == null) {
                JsfUtil.addErrorMessage("Please Select Specility");
                return;
            }
            if (currentStaff == null) {
                JsfUtil.addErrorMessage("Please Select Staff");
                return;
            }
            for (FeeChange fc : feeChanges) {
                fc.getFee().setStaff(currentStaff);
                fc.getFee().setSpeciality(speciality);
            }
        }
        String sql;
        Map m = new HashMap();
        sql = " select fc from FeeChange fc where "
                + " fc.retired=false "
                + " and fc.validFrom=:ed ";
        if (feeChangeStaff) {
            sql += " and fc.fee.staff=:s "
                    + " and fc.fee.speciality=:sp ";
            m.put("s", currentStaff);
            m.put("sp", speciality);
        }
        m.put("ed", effectiveDate);
        List<FeeChange> changes = getFeeChangeFacade().findByJpql(sql, m, TemporalType.DATE);
        for (FeeChange fc : feeChanges) {
            if ((fc.getFee().getFee() == 0) && (fc.getFee().getFfee() == 0)) {
                continue;
            }
            fc.setValidFrom(effectiveDate);
            if (changes.size() > 0) {
                for (FeeChange c : changes) {
                    if ((fc.getFee().getFeeType() == c.getFee().getFeeType())
                            && (fc.getFee().getName().equals(c.getFee().getName()))
                            && (fc.getFee().getStaff().equals(c.getFee().getStaff()))
                            && (fc.getFee().getSpeciality().equals(c.getFee().getSpeciality()))
                            && (fc.getValidFrom().getTime() == c.getValidFrom().getTime())) {
                        JsfUtil.addErrorMessage("This Fee Already Add - " + c.getFee().getName() + " , " + c.getFee().getFeeType() + " , " + c.getValidFrom());
                    } else {
                        //// // System.out.println("fc.getFee().getName() = " + fc.getFee().getName());
                        //// // System.out.println("c.getFee().getName() = " + c.getFee().getName());
                        //// // System.out.println("fc.getFee().getFeeType() = " + fc.getFee().getFeeType());
                        //// // System.out.println("c.getFee().getFeeType() = " + c.getFee().getFeeType());
                        //// // System.out.println("fc.getValidFrom() = " + fc.getValidFrom());
                        //// // System.out.println("c.getValidFrom() = " + c.getValidFrom());
                        if ((fc.getFee().getFee() != 0 || fc.getFee().getFfee() != 0) && (fc.getFee().getFee() != c.getFee().getFee() || fc.getFee().getFfee() != fc.getFee().getFfee())) {
                            fc.setValidFrom(effectiveDate);
                            fc.setCreatedAt(new Date());
                            fc.setCreater(getSessionController().getLoggedUser());
                            getFeeFacade().create(fc.getFee());
                            getFeeChangeFacade().create(fc);
                            JsfUtil.addSuccessMessage("Fee Change Added - " + c.getFee().getName() + " , " + c.getFee().getFeeType() + " , " + c.getValidFrom());
                        } else {
                            JsfUtil.addErrorMessage("Already Added");
                        }

                    }

                }
            } else {
                if (fc.getFee().getFee() != 0 || fc.getFee().getFfee() != 0) {
                    fc.setValidFrom(effectiveDate);
                    fc.setCreatedAt(new Date());
                    fc.setCreater(getSessionController().getLoggedUser());
                    getFeeFacade().create(fc.getFee());
                    getFeeChangeFacade().create(fc);
                    JsfUtil.addSuccessMessage("Fee Change Added - " + fc.getFee().getName() + " , " + fc.getFee().getFeeType() + " , " + fc.getValidFrom());
                } else {
                    JsfUtil.addErrorMessage("Fees Zero");
                }
            }

        }
        prepareAdd();

    }

    public void createFeeChangeTable() {
        String sql;
        Map m = new HashMap();
        sql = " select fc from FeeChange fc where "
                + " fc.retired=false "
                + " and fc.validFrom>:ed ";
        m.put("ed", effectiveDate);
        feeChangesList = getFeeChangeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void removeAddFee(FeeChange fc) {
        fc.setRetired(true);
        fc.setRetiredAt(new Date());
        fc.setRetirer(getSessionController().getLoggedUser());
        getFeeChangeFacade().edit(fc);
        JsfUtil.addSuccessMessage("Removed");
    }

    public void feeChangeListner() {
        if (!feeChangeStaff) {
            speciality = null;
            currentStaff = null;
        }
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
        List<ServiceSession> tmp = getFacade().findByJpql(sql);

        return tmp;
    }

    public List<ServiceSession> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<ServiceSession> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public Date getEffectiveDate() {
        if (effectiveDate == null) {
            effectiveDate = getCommonFunctions().getEndOfDay(new Date());
        } else {
            effectiveDate = getCommonFunctions().getEndOfDay(effectiveDate);
        }
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public List<FeeChange> getFeeChanges() {
        if (feeChanges == null) {
            feeChanges = new ArrayList<>();
        }
        return feeChanges;
    }

    public void setFeeChanges(List<FeeChange> feeChanges) {
        this.feeChanges = feeChanges;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public FeeChangeFacade getFeeChangeFacade() {
        return feeChangeFacade;
    }

    public void setFeeChangeFacade(FeeChangeFacade feeChangeFacade) {
        this.feeChangeFacade = feeChangeFacade;
    }

    public List<FeeChange> getFeeChangesList() {
        return feeChangesList;
    }

    public void setFeeChangesList(List<FeeChange> feeChangesList) {
        this.feeChangesList = feeChangesList;
    }

    public boolean isFeeChangeStaff() {
        return feeChangeStaff;
    }

    public void setFeeChangeStaff(boolean feeChangeStaff) {
        this.feeChangeStaff = feeChangeStaff;
    }

}
