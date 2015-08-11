/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PersonInstitutionType;
import com.divudi.data.channel.ChannelScheduleEvent;
import com.divudi.ejb.ChannelBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class BookingController implements Serializable {

    private Speciality speciality;
    private Staff staff;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date channelDay;
    private ServiceSession selectedServiceSession;
    private BillSession selectedBillSession;
    ////////////////////
    private List<ServiceSession> serviceSessions;
    private List<BillSession> billSessions;
    ////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    ///////////////////
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    ItemFeeFacade ItemFeeFacade;
    /////////////////////////
    @EJB
    private ChannelBean channelBean;

    List<Staff> consultants;
    List<BillSession> getSelectedBillSession;
    boolean printPreview;
    double absentCount;
    int serealNo;
    Date date;

    private ScheduleModel eventModel;

    private ChannelScheduleEvent event = new ChannelScheduleEvent();

    public String nurse() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "channel_nurse_view";
        } else {
            return "";
        }
    }

    public String doctor() {
        if (preSet()) {
            getChannelReportController().fillDoctorView();
            return "channel_doctor_view";
        } else {
            return "";
        }
    }

    public String session() {
        if (preSet()) {
            return "channel_session_view";
        } else {
            return "";
        }
    }

    public String phone() {
        if (preSet()) {
            return "channel_phone_view";
        } else {
            return "";
        }
    }

    public String user() {
        if (preSet()) {
            return "channel_user_view";
        } else {
            return "";
        }
    }

    public List<BillSession> getGetSelectedBillSession() {
        return getSelectedBillSession;
    }

    public void setGetSelectedBillSession(List<BillSession> getSelectedBillSession) {
        this.getSelectedBillSession = getSelectedBillSession;
    }

    public boolean errorCheckForSerial() {
        boolean alreadyExists = false;
        for (BillSession bs : billSessions) {
            //System.out.println("billSessions" + bs.getId());

            if (selectedBillSession.equals(bs)) {

            } else {
                if (bs.getSerialNo() == selectedBillSession.getSerialNo()) {
                    alreadyExists = true;
                }
            }
            System.out.println("bs = " + bs);
            System.out.println("selectedBillSession = " + selectedBillSession);
            if (!bs.equals(selectedBillSession)) {
                for (BillItem bi : bs.getBill().getBillItems()) {
                    System.out.println("bi.getBillSession().getSerialNo() = " + bi.getBillSession().getSerialNo());
                    System.out.println("serealNo = " + serealNo);
                    if (serealNo == bi.getBillSession().getSerialNo()) {
                        System.err.println("Equals");
                        alreadyExists = true;
                        UtilityController.addErrorMessage("This Number Is Alredy Exsist");
                    }
                }
            }

        }

        return alreadyExists;
    }

    public boolean errorCheck() {
        boolean flag = false;
        if (serealNo == 0) {
            UtilityController.addErrorMessage("Cant Add This Number");
            return true;
        }
        for (BillSession bs : billSessions) {
            System.out.println("bs = " + bs);
            System.out.println("selectedBillSession = " + selectedBillSession);
            if (!bs.equals(selectedBillSession)) {
                for (BillItem bi : bs.getBill().getBillItems()) {
                    System.out.println("bi.getBillSession().getSerialNo() = " + bi.getBillSession().getSerialNo());
                    System.out.println("serealNo = " + serealNo);
                    if (serealNo == bi.getBillSession().getSerialNo()) {
                        System.err.println("Equals");
                        UtilityController.addErrorMessage("This Number Is Alredy Exsist");
                        flag = true;
                    }
                }
            }

        }

        return flag;
    }

    public double getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(double absentCount) {
        this.absentCount = absentCount;
    }

//    public void errorCheckChannelNumber() {
//
//        for (BillSession bs : billSessions) {
//            //System.out.println("billSessions" + bs.getName());
//            for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
//                //System.out.println("billitem" + bi.getId());
//                if (bs.getSerialNo() == bi.getBillSession().getSerialNo()) {
//                    UtilityController.addErrorMessage("Number you entered already exist");
//                    setSelectedBillSession(bs);
//
//                }
//
//            }
//        }
//
//    }
    public void updatePatient() {
        getPersonFacade().edit(getSelectedBillSession().getBill().getPatient().getPerson());
        UtilityController.addSuccessMessage("Patient Updated");
    }

    public void updateSerial() {
        if (errorCheckForSerial()) {
            return;
        }
        if (errorCheck()) {
            return;
        }

        for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
            bi.getBillSession().setSerialNo(serealNo);
            System.out.println("bi = " + bi.getBillSession().getSerialNo());
            System.out.println("serealNo = " + serealNo);
            getBillItemFacade().edit(bi);
        }

        getBillSessionFacade().edit(getSelectedBillSession());
        //System.out.println(getSelectedBillSession().getBill().getPatient());
        UtilityController.addSuccessMessage("Serial Updated");
    }

    public void makeNull() {
        speciality = null;
        staff = null;
        selectedServiceSession = null;
        /////////////////////
        serviceSessions = null;
        billSessions = null;
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
            ////System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public void fillConsultants() {
        String sql;
        Map m = new HashMap();
        m.put("sp", getSpeciality());
        if (getSpeciality() != null) {
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

            consultants = getStaffFacade().findBySQL(sql, m);
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
            consultants = getStaffFacade().findBySQL(sql);
        }
//        //System.out.println("consultants = " + consultants);
        setStaff(null);
    }

    public List<Staff> getConsultants() {
        return consultants;
    }

    public void setConsultants(List<Staff> consultants) {
        this.consultants = consultants;
    }

    /**
     * Creates a new instance of BookingController
     */
    public BookingController() {
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
        fillConsultants();
        setStaff(null);
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        System.err.println("CLIKED");
        this.staff = staff;
        //generateSessions();
        setSelectedServiceSession(null);
    }

    public Date getDate() {
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

    private Double[] fetchFee(Item item, FeeType feeType) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select sum(f.fee),sum(f.ffee) "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses "
                + " and f.feeType=:ftp";
        m.put("ses", item);
        m.put("ftp", feeType);
        Object[] obj = getItemFeeFacade().findAggregateModified(jpql, m, TemporalType.TIMESTAMP);

        if (obj == null) {
            Double[] dbl = new Double[2];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            return dbl;
        }

        Double[] dbl = Arrays.copyOf(obj, obj.length, Double[].class);
        System.err.println("Fetch Fee Values " + dbl);
        return dbl;
    }

    private double fetchLocalFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m, TemporalType.TIMESTAMP);

        return obj;
    }

    private double fetchForiegnFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m, TemporalType.TIMESTAMP);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private List<ItemFee> fetchFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        List<ItemFee> list = getItemFeeFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
        System.err.println("Fetch Fess " + list);
        return list;
    }

    public void calculateFee(List<ServiceSession> lstSs) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss, FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            dbl = fetchFee(ss, FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            System.err.println("1111");
            dbl = fetchFee(ss, FeeType.Tax);
            System.err.println("2222");
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            ss.setTotalFee(fetchLocalFee(ss));
            ss.setTotalFfee(fetchForiegnFee(ss));
            ss.setItemFees(fetchFee(ss));
        }
    }

    public void generateSessions() {
        serviceSessions = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());

        if (staff != null) {
            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " order by s.sessionWeekday,s.startingTime ";
            List<ServiceSession> tmp = getServiceSessionFacade().findBySQL(sql, m);
            System.err.println("Fetch Sessions " + tmp);
            calculateFee(tmp);
            System.err.println("Calling Start");
            serviceSessions = getChannelBean().generateDailyServiceSessionsFromWeekdaySessions(tmp);
            generateSessionEvents(serviceSessions);
            System.err.println("Calling End");
        }
    }

    public void generateSessionEvents(List<ServiceSession> sss) {
        eventModel = new DefaultScheduleModel();
        for (ServiceSession s : sss) {
            ChannelScheduleEvent e = new ChannelScheduleEvent();
            e.setServiceSession(s);
            e.setTitle(s.getName());
            e.setStartDate(s.getTransStartTime());
            e.setEndDate(s.getTransEndTime());
            System.out.println("e = " + e);
            eventModel.addEvent(e);
        }
        System.out.println("eventModel = " + eventModel);
    }

    public void onEventSelect(SelectEvent selectEvent) {
        event = (ChannelScheduleEvent) selectEvent.getObject();
        selectedServiceSession = event.getServiceSession();
        fillBillSessions();
    }

    public void generateSessionsFutureBooking(SelectEvent event) {
        date = null;
        date = ((Date) event.getObject());
        serviceSessions = new ArrayList<>();
        Map m = new HashMap();

        Date currenDate = new Date();
        if (getDate().before(currenDate)) {
            UtilityController.addErrorMessage("Please Select Future Date");
            return;
        }

        String sql = "";

        if (staff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);

            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.sessionWeekday=:wd ";

            m.put("staff", getStaff());
            m.put("wd", wd);
            List<ServiceSession> tmp = getServiceSessionFacade().findBySQL(sql, m);
            calculateFee(tmp);
            serviceSessions = getChannelBean().generateServiceSessionsForSelectedDate(tmp, date);
        }

        billSessions = new ArrayList<>();
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<ServiceSession> getServiceSessions() {
        return serviceSessions;
    }

    public void setServiceSessions(List<ServiceSession> serviceSessions) {
        this.serviceSessions = serviceSessions;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void fillBillSessions(SelectEvent event) {
        selectedBillSession = null;
        selectedServiceSession = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        System.out.println("billSessions" + billSessions);

    }

    public void fillBillSessions() {
        selectedBillSession = null;
//        selectedServiceSession = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        System.out.println("billSessions" + billSessions);

    }

    public void fillAbsentBillSessions(SelectEvent event) {
        selectedBillSession = null;
        selectedServiceSession = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt "
                + " and bs.absent=true "
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);
        //absentCount=billSessions.size();

    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;

    }

    public void makeBillSessionNull() {
        billSessions = null;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public BillSession getSelectedBillSession() {
        if (selectedBillSession == null) {
            selectedBillSession = new BillSession();
            Bill b = new BilledBill();
            Patient p = new Patient();
            p.setPerson(new Person());
            b.setPatient(p);
            selectedBillSession.setBill(b);
        }
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
        getChannelCancelController().makeNull();
        getChannelCancelController().setBillSession(selectedBillSession);
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public ChannelBillController getChannelCancelController() {
        return channelCancelController;
    }

    public void setChannelCancelController(ChannelBillController channelCancelController) {
        this.channelCancelController = channelCancelController;
    }

    public ChannelReportController getChannelReportController() {
        return channelReportController;
    }

    public void setChannelReportController(ChannelReportController channelReportController) {
        this.channelReportController = channelReportController;
    }

    public Boolean preSet() {
        if (getSelectedServiceSession() == null) {
            UtilityController.addErrorMessage("Please select Service Session");
            return false;
        }
        getChannelReportController().setServiceSession(selectedServiceSession);

        return true;
    }

    public ChannelSearchController getChannelSearchController() {
        return channelSearchController;
    }

    public void setChannelSearchController(ChannelSearchController channelSearchController) {
        this.channelSearchController = channelSearchController;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return ItemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade ItemFeeFacade) {
        this.ItemFeeFacade = ItemFeeFacade;
    }

    public Date getChannelDay() {
        return channelDay;
    }

    public void setChannelDay(Date channelDay) {
        this.channelDay = channelDay;
    }

    public int getSerealNo() {
        return serealNo;
    }

    public void setSerealNo(int serealNo) {
        this.serealNo = serealNo;
    }

    public ScheduleModel getEventModel() {
        if (eventModel == null) {
            eventModel = new DefaultScheduleModel();
        }
        return eventModel;
    }

    public void setEventModel(ScheduleModel eventModel) {
        this.eventModel = eventModel;
    }

    public ChannelScheduleEvent getEvent() {
        if (event == null) {
            event = new ChannelScheduleEvent();
        }
        return event;
    }

    public void setEvent(ChannelScheduleEvent event) {
        this.event = event;
    }

}
