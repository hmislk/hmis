/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.DoctorSpecialityController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.SmsController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.data.MessageType;
import com.divudi.data.channel.ChannelScheduleEvent;
import com.divudi.ejb.ChannelBean;

import com.divudi.ejb.FinalVariables;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionInstance;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.ArrivalRecord;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.util.JsfUtil;
import com.divudi.java.CommonFunctions;
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
import org.primefaces.event.RowEditEvent;
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

    /**
     * EJBs
     */
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
    @EJB
    private ChannelBean channelBean;
    @EJB
    FingerPrintRecordFacade fpFacade;
    @EJB
    FinalVariables finalVariables;

    CommonFunctions commonFunctions;
    /**
     * Controllers
     */
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    @Inject
    ServiceSessionLeaveController serviceSessionLeaveController;
    @Inject
    ChannelBillController channelBillController;
    @Inject
    DoctorSpecialityController doctorSpecialityController;
    @Inject
    ChannelStaffPaymentBillController channelStaffPaymentBillController;
    @Inject
    SmsController smsController;

    /**
     * Properties
     */
    private Speciality speciality;
    private Staff staff;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date channelDay;
    private ServiceSessionInstance selectedServiceSessionInstance;
    private BillSession selectedBillSession;
    private List<ServiceSessionInstance> serviceSessionInstances;
    private List<BillSession> billSessions;
    List<Staff> consultants;
    List<BillSession> getSelectedBillSession;
    boolean printPreview;
    double absentCount;
    int serealNo;
    Date date;
    Date sessionStartingDate;
    String selectTextSpeciality = "";
    String selectTextConsultant = "";
    String selectTextSession = "";
    ArrivalRecord arrivalRecord;
    PaymentMethod canPayMetTmp;


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
            ////// // System.out.println("billSessions" + bs.getId());

            if (selectedBillSession.equals(bs)) {

            } else {
                if (bs.getSerialNo() == selectedBillSession.getSerialNo()) {
                    alreadyExists = true;
                }
            }
            if (!bs.equals(selectedBillSession)) {
                for (BillItem bi : bs.getBill().getBillItems()) {
                    if (serealNo == bi.getBillSession().getSerialNo()) {
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
            if (!bs.equals(selectedBillSession)) {
                for (BillItem bi : bs.getBill().getBillItems()) {
                    if (serealNo == bi.getBillSession().getSerialNo()) {
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
//            ////// // System.out.println("billSessions" + bs.getName());
//            for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
//                ////// // System.out.println("billitem" + bi.getId());
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
            getBillItemFacade().edit(bi);
        }

        getBillSessionFacade().edit(getSelectedBillSession());
        ////// // System.out.println(getSelectedBillSession().getBill().getPatient());
        UtilityController.addSuccessMessage("Serial Updated");
    }

    public void listnerMarkAbsent() {
        if (getSelectedBillSession().isAbsent()) {
            getSelectedBillSession().setAbsentMarkedAt(new Date());
            getSelectedBillSession().setAbsentMarkedUser(getSessionController().getLoggedUser());
        } else {
            getSelectedBillSession().setAbsentUnmarkedAt(new Date());
            getSelectedBillSession().setAbsentUnmarkedUser(getSessionController().getLoggedUser());
        }

        getBillSessionFacade().edit(getSelectedBillSession());
        ////// // System.out.println(getSelectedBillSession().getBill().getPatient());
        if (getSelectedBillSession().isAbsent()) {
            UtilityController.addSuccessMessage("Mark As Absent");
            if (getSelectedBillSession().getBill().getPaidBill() != null) {
                getSelectedBillSession().getBill().getPaidBill().getSingleBillSession().setAbsent(true);
                getBillSessionFacade().edit(getSelectedBillSession().getBill().getPaidBill().getSingleBillSession());
            }
        } else {
            UtilityController.addSuccessMessage("Mark As Present");
            if (getSelectedBillSession().getBill().getPaidBill() != null) {
                getSelectedBillSession().getBill().getPaidBill().getSingleBillSession().setAbsent(false);
                getBillSessionFacade().edit(getSelectedBillSession().getBill().getPaidBill().getSingleBillSession());
            }
        }
    }

    public void makeNull() {
        speciality = null;
        staff = null;
        selectedServiceSessionInstance = null;
        /////////////////////
        serviceSessionInstances = null;
        billSessions = null;
        sessionStartingDate = null;
        consultants = null;
        channelBillController.clearForNewSearch();
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getSpeciality() != null) {
                sql = "select p from Staff p where p.retired=false and (p.person.name like '%" + query.toUpperCase() + "%'or  p.code like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and (p.person.name like '%" + query.toUpperCase() + "%'or  p.code like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //////// // System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void fillConsultants() {
        String sql;
        Map m = new HashMap();
        m.put("sp", getSpeciality());
        if (getSpeciality() != null) {
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

            consultants = getStaffFacade().findByJpql(sql, m);
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
            consultants = getStaffFacade().findByJpql(sql);
        }
//        ////// // System.out.println("consultants = " + consultants);
        setStaff(null);
    }

    public List<Staff> getSelectedConsultants() {
//        //// // System.out.println("selectText.length() = " + selectTextConsultant.length());
        String sql;
        Map m = new HashMap();

//        ////// // System.out.println("consultants = " + consultants);
        if (selectTextConsultant == null || selectTextConsultant.trim().equals("")) {
            m.put("sp", getSpeciality());
            if (getSpeciality() != null) {
                if (getSessionController().getLoggedPreference().isShowOnlyMarkedDoctors()) {

                    sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                            + " and pi.type=:typ "
                            + " and pi.institution=:ins "
                            + " and pi.staff.speciality=:sp ";
                    if (getSessionController().getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                        sql += " order by pi.staff.codeInterger , pi.staff.person.name ";
                    } else {
                        sql += " order by pi.staff.person.name ";
                    }
                    m.put("ins", getSessionController().getInstitution());
                    m.put("typ", PersonInstitutionType.Channelling);

                } else {
                    sql = "select p from Staff p where p.retired=false and p.speciality=:sp order by p.person.name";
                }
//                //// // System.out.println("m = " + m);
//                //// // System.out.println("sql = " + sql);
                consultants = getStaffFacade().findByJpql(sql, m);
            }
        } else {
            if (selectTextConsultant.length() > 4) {
                doctorSpecialityController.setSelectText("");
                if (getSessionController().getLoggedPreference().isShowOnlyMarkedDoctors()) {

                    sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                            + " and pi.type=:typ "
                            + " and pi.institution=:ins "
                            + " and pi.staff.person.name like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                            + " order by pi.staff.person.name ";

                    m.put("ins", getSessionController().getInstitution());
                    m.put("typ", PersonInstitutionType.Channelling);
//                    //// // System.out.println("m = " + m);
//                    //// // System.out.println("sql = " + sql);
                    consultants = getStaffFacade().findByJpql(sql, m);

                } else {
                    sql = "select p from Staff p where p.retired=false "
                            + " and p.person.name like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                            + " order by p.person.name";
                    consultants = getStaffFacade().findByJpql(sql);
                }

            } else {
                m.put("sp", getSpeciality());
                if (getSpeciality() != null) {
                    if (getSessionController().getLoggedPreference().isShowOnlyMarkedDoctors()) {

                        sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                                + " and pi.type=:typ "
                                + " and pi.institution=:ins "
                                + " and pi.staff.speciality=:sp "
                                + " and pi.staff.person.name like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                                + " order by pi.staff.person.name ";

                        m.put("ins", getSessionController().getInstitution());
                        m.put("typ", PersonInstitutionType.Channelling);

                    } else {
                        sql = "select p from Staff p where p.retired=false and p.speciality=:sp"
                                + " and p.person.name like '%" + getSelectTextConsultant().toUpperCase() + "%' "
                                + " order by p.person.name";
                    }
                    consultants = getStaffFacade().findByJpql(sql, m);
                }
            }
        }
        if (consultants == null) {
            consultants = new ArrayList<>();
        }

//        if (consultants.size() > 0) {
//            //// // System.out.println("consultants.size() = " + consultants.size());
//            setStaff(consultants.get(0));
//            setSpeciality(getStaff().getSpeciality());
////            generateSessions();
//            generateSessionsOnlyId();
//        } else {
//
//            setStaff(null);
//        }
        return consultants;
    }

    public List<Staff> getConsultants() {
        if (consultants == null) {
            consultants = new ArrayList<>();
        }
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
    }

//    public void setSpeciality(Speciality speciality) {
//        this.speciality = speciality;
//        fillConsultants();
//        setStaff(null);
//    }
    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

//    public void setStaff(Staff staff) {
////        System.err.println("CLIKED");
//        this.staff = staff;
//        //generateSessions();
//        setSelectedServiceSession(null);
//        serviceSessionLeaveController.setSelectedServiceSession(null);
//        serviceSessionLeaveController.setCurrentStaff(staff);
//    }
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
//        System.err.println("Fetch Fee Values " + dbl);
        return dbl;
    }

    private List<Object[]> fetchFeeById(Long l) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f.feeType,sum(f.fee),sum(f.ffee) "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses "
                + " group by f.feeType ";
        m.put("ses", l);
        List<Object[]> objs = getItemFeeFacade().findAggregates(jpql, m);
        return objs;
    }

    private double fetchLocalFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        //// // System.out.println("paymentMethod = " + paymentMethod);
//        //// // System.out.println("feeTypes = " + feeTypes);
//        //// // System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchLocalFeeOnlyStaffVat(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        List<FeeType> feeTypes = Arrays.asList(new FeeType[]{FeeType.Service, FeeType.OwnInstitution});
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        feeTypes = Arrays.asList(new FeeType[]{FeeType.Staff});
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Staff};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);

        Double obj2 = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            obj = 0.0;
        }
        if (obj2 == null) {
            obj2 = 0.0;
        }
        double d = 0.0;
        // who wat to get vat for selected sessions add institution institution to this methord and settle methord
        if (getSessionController().getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
            if (item.isVatable()) {
                d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
            } else {
                d = obj + obj2;
            }
        } else {
            d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
        }

        return d;
    }

    private Object[] fetchLocalForiegnFeeById(Long l, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee),sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", l);
//        //// // System.out.println("paymentMethod = " + paymentMethod);
//        //// // System.out.println("feeTypes = " + feeTypes);
//        //// // System.out.println("m = " + m);
        Object[] obj = getBillFacade().findAggregateModified(jpql, m, TemporalType.DATE);

        return obj;
    }

    private double fetchForiegnFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
//        //// // System.out.println("paymentMethod = " + paymentMethod);
//        //// // System.out.println("feeTypes = " + feeTypes);
//        //// // System.out.println("m = " + m);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchForiegnFeeOnlyStaffVat(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        List<FeeType> feeTypes = Arrays.asList(new FeeType[]{FeeType.Service, FeeType.OwnInstitution});
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        feeTypes = Arrays.asList(new FeeType[]{FeeType.Staff});
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Staff};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);

        Double obj2 = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            obj = 0.0;
        }
        if (obj2 == null) {
            obj2 = 0.0;
        }
        double d = 0.0;
        // who wat to get vat for selected sessions add institution institution to this methord and settle methord
        if (getSessionController().getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
            if (item.isVatable()) {
                d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
            } else {
                d = obj + obj2;
            }
        } else {
            d = obj + (obj2 * finalVariables.getVATPercentageWithAmount());
        }

        return d;
    }

    private List<ItemFee> fetchFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        List<ItemFee> list = getItemFeeFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    private List<ItemFee> fetchItemFeeById(Long l) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item.id=:ses ";
        m.put("ses", l);
        List<ItemFee> list = getItemFeeFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    public void calculateFee(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss, FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            dbl = fetchFee(ss, FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
//            System.err.println("1111");
            dbl = fetchFee(ss, FeeType.Tax);
//            System.err.println("2222");
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            ss.setTotalFee(fetchLocalFee(ss, paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss, paymentMethod));
            ss.setItemFees(fetchFee(ss));
        }
    }

    public void calculateFeeBySessionIdList(List<Long> lstSs, PaymentMethod paymentMethod) {
        for (Long ss : lstSs) {
//            System.err.println("Cal Fee  in Time  = " + new Date() + " SS id=" + ss);
            ServiceSession session = getServiceSessionFacade().find(ss);
            List<Object[]> objs = fetchFeeById(ss);
            for (Object[] o : objs) {
                FeeType ft = (FeeType) o[0];
                if (ft == FeeType.OwnInstitution) {
                    session.setHospitalFee((double) o[1]);
                    session.setHospitalFfee((double) o[2]);
                    continue;
                }
                if (ft == FeeType.Staff) {
                    session.setProfessionalFee((double) o[1]);
                    session.setProfessionalFfee((double) o[2]);
                    continue;
                }
                if (ft == FeeType.Staff) {
                    session.setTaxFee((double) o[1]);
                    session.setTaxFfee((double) o[2]);
                }
            }
            Object[] ob = fetchLocalForiegnFeeById(ss, paymentMethod);
            session.setTotalFee((double) ob[0]);
            session.setTotalFfee((double) ob[1]);
            session.setItemFees(fetchItemFeeById(ss));
        }
    }

    public void calculateFeeBooking(List<ServiceSessionInstance> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSessionInstance ss : lstSs) {
            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void calculateFeeBookingNew(List<ServiceSessionInstance> lstSs, PaymentMethod paymentMethod) {
        int rowIndex = 0;
        for (ServiceSessionInstance ss : lstSs) {
            ss.setDisplayCount(channelBean.getBillSessionsCount(ss, ss.getSessionDate()));
            ss.setTransDisplayCountWithoutCancelRefund(channelBean.getBillSessionsCountWithOutCancelRefund(ss, ss.getSessionDate()));
            ss.setTransCreditBillCount(channelBean.getBillSessionsCountCrditBill(ss, ss.getSessionDate()));
            ss.setTransRowNumber(rowIndex++);
//            System.err.println("Time D.A. in = " + new Date());
//            checkDoctorArival(ss);
//            System.err.println("Time D.A. Out = " + new Date());

            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            if (getSessionController().getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Arogya) {
                ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
                ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
                ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
                ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));

                if (ss.getOriginatingSession().isVatable()) {
                    //all Bill
//                    ss.setTotalFee(ss.getTotalFee() * finalVariables.getVATPercentageWithAmount());
//                    ss.getOriginatingSession().setTotalFee(ss.getOriginatingSession().getTotalFfee() * finalVariables.getVATPercentageWithAmount());

                    //only Doc Fee
                    ss.setTotalFee(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
                    ss.getOriginatingSession().setTotalFee(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod));
                }
            } else {
                ss.setTotalFee(Math.round(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod)));
                ss.setTotalFfee(Math.round(fetchForiegnFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod)));
                ss.getOriginatingSession().setTotalFee(Math.round(fetchLocalFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod)));
                ss.getOriginatingSession().setTotalFfee(Math.round(fetchForiegnFeeOnlyStaffVat(ss.getOriginatingSession(), paymentMethod)));
//                //// // System.out.println("Math.round(ss.getTotalFee()) = " + Math.round(ss.getTotalFee()));
            }
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void generateSessions() {
        serviceSessionInstances = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        m.put("class", ServiceSession.class);

        if (staff != null) {
//            No Change Needed
            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.originatingSession is null "
                    + " and type(s)=:class "
                    + " order by s.sessionWeekday,s.startingTime ";
            List<ServiceSession> originatingServiceSessions = getServiceSessionFacade().findByJpql(sql, m);

            for (ServiceSession ss : originatingServiceSessions) {
                ss.getStaff();
                ss.getDepartment();

            }

            calculateFee(originatingServiceSessions, channelBillController.getPaymentMethod());
            serviceSessionInstances = getChannelBean().generateDailyServiceSessionsFromWeekdaySessionsNew(originatingServiceSessions, getSessionStartingDate());
//            generateSessionEvents(serviceSessionInstances);
            checkDoctorArival(originatingServiceSessions);
        }
    }

    public void generateSessionsOnlyId() {
        serviceSessionInstances = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        m.put("class", ServiceSession.class);
        m.put("nd", new Date());
        if (staff != null) {
            sql = "Select s.id From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.originatingSession is null"
                    + " and ((s.sessionWeekday is null and s.sessionDate >=:nd)or(s.sessionWeekday is not null and s.sessionDate is null)) "
                    + " and type(s)=:class "
                    + " order by s.sessionWeekday,s.startingTime ";
            List<Long> tmp = new ArrayList<>();
            tmp = getServiceSessionFacade().findLongList(sql, m, TemporalType.DATE);
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());
//            calculateFeeBySessionIdList(tmp, channelBillController.getPaymentMethod());


            serviceSessionInstances = getChannelBean().generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionId(tmp, getSessionStartingDate());
//            generateSessionEvents(serviceSessionInstances);
//            generateSessionEvents(serviceSessionInstances);
//            generateSessionEvents(serviceSessionInstances);
//            generateSessionEvents(serviceSessionInstances);
//            generateSessionEvents(serviceSessionInstances);
//            generateSessionEvents(serviceSessionInstances);
//            generateSessionEvents(serviceSessionInstances);
//            generateSessionEvents(serviceSessionInstances);
        }
    }

    public void generateSessionsOnlyIdNew() {
        serviceSessionInstances = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        m.put("class", ServiceSession.class);
        m.put("nd", new Date());
        if (staff != null) {
            serviceSessionInstances = getChannelBean().generateDailyServiceSessionsFromWeekdaySessionsNewByServiceSessionIdNew(staff, getSessionStartingDate());
        }
        if (getSessionController().getLoggedUser().getWebUserPerson() != null) {
        }
    }

//    public void generateSessionEvents(List<ServiceSessionInstance> sss) {
//        eventModel = new DefaultScheduleModel();
//        for (ServiceSessionInstance s : sss) {
//            ChannelScheduleEvent e = new ChannelScheduleEvent();
//            e.setServiceSession(s);
//            e.setTitle(s.getName());
//            e.setStartDate(CommonFunctions.getLocalDateTime(s.getTransStartTime()));
//            e.setEndDate(CommonFunctions.getLocalDateTime(s.getTransEndTime()));
//            eventModel.addEvent(e);
//            checkDoctorArival(s);
//        }
//    }

    public void checkDoctorArival(ServiceSession s) {
        s.setArival(findArrivals(s));
    }

    public void checkDoctorArival(List<ServiceSession> sss) {
        for (ServiceSession s : sss) {
            s.setArival(findArrivals(s));
        }
    }

//    public void onEventSelect(SelectEvent selectEvent) {
//        event = (ChannelScheduleEvent) selectEvent.getObject();
//        selectedServiceSessionInstance = event.getServiceSession();
//        fillBillSessions();
//    }

    public void generateSessionsFutureBooking(SelectEvent event) {
        date = null;
        date = ((Date) event.getObject());
        serviceSessionInstances = new ArrayList<>();
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
            List<ServiceSession> tmp = getServiceSessionFacade().findByJpql(sql, m);
            calculateFee(tmp, channelBillController.getPaymentMethod());//check work future bokking
            serviceSessionInstances = getChannelBean().generateServiceSessionsForSelectedDate(tmp, date);
        }

        billSessions = new ArrayList<>();
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<ServiceSessionInstance> getServiceSessionInstances() {
        return serviceSessionInstances;
    }

    public void setServiceSessionInstances(List<ServiceSessionInstance> serviceSessionInstances) {
        this.serviceSessionInstances = serviceSessionInstances;
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

//    public void fillBillSessions(SelectEvent event) {
//        selectedBillSession = null;
//        selectedServiceSessionInstance = ((ServiceSession) event.getObject());
//
//        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
//        List<BillType> bts = Arrays.asList(billTypes);
//
//        String sql = "Select bs From BillSession bs "
//                + " where bs.retired=false"
//                + " and bs.serviceSession=:ss "
//                + " and bs.bill.billType in :bt"
//                + " and type(bs.bill)=:class "
//                + " and bs.sessionDate= :ssDate "
//                + " order by bs.serialNo ";
//        HashMap hh = new HashMap();
//        hh.put("bt", bts);
//        hh.put("class", BilledBill.class);
//        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
//        hh.put("ss", getSelectedServiceSession());
//        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
//        //// // System.out.println("hh = " + hh);
//        //// // System.out.println("getSelectedServiceSession().isTransLeave() = " + getSelectedServiceSession().isTransLeave());
//        if (getSelectedServiceSession().isTransLeave()) {
//            billSessions=null;
//        }
//        //// // System.out.println("billSessions" + billSessions);
//
//    }
    public void findArrivals() {

        String sql = "Select bs From ArrivalRecord bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.sessionDate= :ssDate ";
        HashMap hh = new HashMap();
        hh.put("ssDate", getSelectedServiceSessionInstance().getSessionDate());
        hh.put("ss", getSelectedServiceSessionInstance());
        arrivalRecord = (ArrivalRecord) fpFacade.findFirstByJpql(sql, hh);
    }

    public Boolean findArrivals(ServiceSession ss) {
        String sql = "Select bs From ArrivalRecord bs "
                + " where bs.retired=false"
                + " and bs.serviceSession.id=:ss "
                + " and bs.sessionDate=:ssDate ";
        HashMap hh = new HashMap();
        hh.put("ssDate", ss.getSessionDate());
        hh.put("ss", ss.getId());
        arrivalRecord = (ArrivalRecord) fpFacade.findFirstByJpql(sql, hh);

        if (arrivalRecord != null) {
            if (arrivalRecord.isApproved()) {
                return true;
            } else {
                return false;
            }
        }
        return null;
    }

    public void sendSmsDoctorArived(ServiceSession ss) {
        //// // System.out.println("ss.getSessionAt() = " + ss.getSessionAt());
        //// // System.out.println("ss.getSessionDate() = " + ss.getSessionDate());
        //// // System.out.println("ss.getSessionTime() = " + ss.getSessionTime());
        //// // System.out.println("ss.getStartingTime() = " + ss.getStartingTime());

        Calendar cal = Calendar.getInstance();
        cal.setTime(ss.getStartingTime());
        cal.add(Calendar.HOUR, 3);

        List<ServiceSession> list = fetchServiceSessionsForTimeRange(ss.getStaff(), ss.getSessionDate(), ss.getStartingTime(), cal.getTime());
        List<BillSession> bSessions = new ArrayList<>();
        for (ServiceSession s : list) {
            bSessions.addAll(fillBillSessions(s));
        }

        String msg = "Dear Sir/Madam,\n"
                + ss.getStaff().getPerson().getName() + " has arrived.\n"
                //                + "** Now you can channel your doctor online on www.ruhunuhospital.lk **";
                + "** Now you can channel your doctor online on https://goo.gl/aEbnDD **";
//        fillBillSessions();
        for (BillSession bs : bSessions) {
            if (bs.getBill().isCancelled() || bs.getBill().isRefunded()) {
                continue;
            }
            smsController.sendSmsToNumberList(bs.getBill().getPatient().getPerson().getPhone(), getSessionController().getUserPreference().getApplicationInstitution(), msg, bs.getBill(), MessageType.ChannelDoctorAraival);
        }
    }

    public void sendSmsToinformLeave() {
        String msg = "Dear Sir/Madam,\n"
                + selectedServiceSessionInstance.getStaff().getPerson().getName() + " is Leave Today."
                + "Thank you for using Ruhunu Hospital services.";
        //fillBillSessions();
        for (BillSession bs : billSessions) {
            if (bs.getBill().isCancelled() || bs.getBill().isRefunded()) {
                continue;
            }
//            smsController.sendSmsToNumberList(bs.getBill().getPatient().getPerson().getPhone(), getSessionController().getUserPreference().getApplicationInstitution(), msg,bs.getBill());
        }
    }

    public List<ServiceSession> fetchServiceSessionsForTimeRange(Staff s, Date date, Date ft, Date tt) {
        String sql;
        Map m = new HashMap();
        List<ServiceSession> tmp = new ArrayList<>();
        sql = "Select s From ServiceSession s where s.retired=false "
                + " and s.staff=:staff "
                + " and s.originatingSession is not null "
                + " and s.sessionDate=:d "
                + " and s.startingTime between :ft and :tt "
                + " and type(s)=:class "
                + " order by s.sessionDate,s.startingTime ";
        m.put("d", date);
        m.put("ft", ft);
        m.put("tt", tt);
        m.put("staff", s);
        m.put("class", ServiceSession.class);
        try {
            tmp = getServiceSessionFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public void markAsArrived() {
        if (selectedServiceSessionInstance == null) {
            return;
        }
        if (selectedServiceSessionInstance.getSessionDate() == null) {
            return;
        }
        if (commonFunctions.getEndOfDay(selectedServiceSessionInstance.getSessionDate()).getTime() != commonFunctions.getEndOfDay(new Date()).getTime()) {
            JsfUtil.addErrorMessage("You Can Mark Only Today Arrivals Only");
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedServiceSessionInstance.getSessionDate());
            arrivalRecord.setServiceSession(selectedServiceSessionInstance.getOriginatingSession());
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
            //
            if (getSessionController().getLoggedPreference().isChannelDoctorArivalMsgSend()) {
                sendSmsDoctorArived(selectedServiceSessionInstance.getOriginatingSession());
            }
        }
        arrivalRecord.setRecordTimeStamp(new Date());
        arrivalRecord.setApproved(false);
        fpFacade.edit(arrivalRecord);
    }

    public void markAsLeft() {
        if (selectedServiceSessionInstance == null) {
            return;
        }
        if (selectedServiceSessionInstance.getSessionDate() == null) {
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedServiceSessionInstance.getSessionDate());
            arrivalRecord.setServiceSession(selectedServiceSessionInstance.getOriginatingSession());
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
        }

        arrivalRecord.setApproved(true);
        arrivalRecord.setApprovedAt(new Date());
        arrivalRecord.setApprover(sessionController.getLoggedUser());
        fpFacade.edit(arrivalRecord);
    }

    public void fillBillSessions() {
        selectedBillSession = null;
//        selectedServiceSessionInstance = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.bill.retired=false "
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSessionInstance().getSessionDate());
        hh.put("ss", getSelectedServiceSessionInstance().getOriginatingSession());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

    }

    public List<BillSession> fillBillSessions(ServiceSession ss) {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false "
                + " and bs.bill.retired=false "
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", ss.getSessionDate());
        hh.put("ss", ss);
        return getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

    }

    public void fillAbsentBillSessions(SelectEvent event) {
        selectedBillSession = null;
        selectedServiceSessionInstance = ((ServiceSessionInstance) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.bill.retired=false "
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt "
                + " and bs.absent=true "
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSessionInstance().getSessionAt());
        hh.put("ss", getSelectedServiceSessionInstance().getOriginatingSession());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
        //absentCount=billSessions.size();

    }

    public String paySelectedDoctor() {
        if (getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Specility And Staff");
            return "";
        }
        if (getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return "";
        }
        channelStaffPaymentBillController.makenull();
        channelStaffPaymentBillController.setSpeciality(getSpeciality());
        channelStaffPaymentBillController.setCurrentStaff(getStaff());
        channelStaffPaymentBillController.fillSessions();
        channelStaffPaymentBillController.setConsiderDate(true);
        channelStaffPaymentBillController.calculateDueFees();

        return "/channel/channel_payment_staff_bill";

    }

    public void onEditItem(RowEditEvent event) {
        ServiceSession tmp = (ServiceSession) event.getObject();
        ServiceSession ss = getServiceSessionFacade().find(tmp.getId());
        if (ss.getMaxNo() != tmp.getMaxNo() || ss.getStartingNo() != tmp.getStartingNo() || ss.getSessionTime() != tmp.getStartingTime() || ss.isRetired() != tmp.isRetired()) {
            tmp.setEditedAt(new Date());
            tmp.setEditer(getSessionController().getLoggedUser());
        }
        getServiceSessionFacade().edit(tmp);
    }

    public void listnerStaffListForRowSelect() {
        getSelectedConsultants();
    }

    public void listnerStaffListForRowSelectNew() {
        serviceSessionInstances = new ArrayList<>();
        listnerStaffListForRowSelect();
        listnerClearSelectedServiceSession();
    }

    public void clearServiceSessions() {
        serviceSessionInstances = new ArrayList<>();
    }

    public void listnerServiceSessionListForRowSelectNew() {
        generateSessionsOnlyIdNew();
//        generateSessionsOnlyId(); before Optimize
        listnerClearSelectedServiceSession();
    }

    public void listnerBillSessionListForRowSelectNew() {
        fillBillSessions();
        listnerClearSelectedBillSession();
        if (getSessionController().getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative
                && getSelectedServiceSessionInstance().getOriginatingSession().getForBillType() == BillType.XrayScan) {
            getSelectedServiceSessionInstance().getOriginatingSession().setItemFees(fetchFee(getSelectedServiceSessionInstance().getOriginatingSession()));
        }
    }

    public void listnerStaffRowSelect() {
        getSelectedConsultants();
        setSelectedServiceSessionInstance(null);
        serviceSessionLeaveController.setSelectedServiceSession(null);
        serviceSessionLeaveController.setCurrentStaff(staff);
    }

    public void listnerSessionRowSelect() {
        for (ServiceSessionInstance ss : serviceSessionInstances) {
            if (ss.getOriginatingSession().getSessionText().toLowerCase().contains(selectTextSession.toLowerCase())) {
                selectedServiceSessionInstance = ss;
            }
        }
    }

    public void listnerStaffListForSpecilitySelectedText() {
        if (doctorSpecialityController.getSelectedItems().size() > 0) {
            setSpeciality(doctorSpecialityController.getSelectedItems().get(0));
            listnerStaffListForRowSelect();
        }
    }

    public void listnerClearSelectedServiceSession() {
        selectedServiceSessionInstance = null;
        billSessions = null;
        selectedBillSession = null;
        getChannelCancelController().clearForNewBill();
        getChannelBillController().setBillSession(null);
    }

    public void listnerClearSelectedBillSession() {
        selectedBillSession = null;
        getChannelBillController().setBillSession(null);
    }

    public void viewBill(BillSession bs) {
//        setSpeciality(bs.getServiceSession().getStaff().getSpeciality());
//        //// // System.out.println("++++getSpeciality().getName() = " + getSpeciality().getName());

//        getSelectedConsultants();
//        setSpeciality(bs.getServiceSession().getStaff().getSpeciality());
//        setStaff(bs.getServiceSession().getStaff());
//        //// // System.out.println("++++bs.getServiceSession().getStaff().getName() = " + bs.getServiceSession().getStaff().getPerson().getName());
//        //// // System.out.println("++++getStaff().getPerson().getName() = " + getStaff().getPerson().getName());
//        generateSessionsOnlyId();
//        setSelectedServiceSession(bs.getServiceSession());
//        //// // System.out.println("++++bs.getServiceSession() = " + bs.getServiceSession());
//        //// // System.out.println("++++getSelectedServiceSession() = " + getSelectedServiceSession());
//        fillBillSessions();
        //// // System.out.println("++++channelBillController.getBillSession() = " + channelBillController.getBillSession());
        //// // System.out.println("++++channelBillController.getBillSessionTmp() = " + channelBillController.getBillSessionTmp());
        setSelectedBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
//        getChannelBillController().setBillSession(bs);
        //// // System.out.println("++++bs = " + bs);
        //// // System.out.println("++++getSelectedBillSession() = " + getSelectedBillSession());
        channelBillController.listnerSetBillSession(bs);
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public ServiceSessionInstance getSelectedServiceSessionInstance() {
        return selectedServiceSessionInstance;
    }

    public void setSelectedServiceSessionInstance(ServiceSessionInstance selectedServiceSessionInstance) {
        this.selectedServiceSessionInstance = selectedServiceSessionInstance;

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
        getChannelBillController().clearForNewBill();
        getChannelBillController().setBillSession(selectedBillSession);
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
        if (getSelectedServiceSessionInstance() == null) {
            UtilityController.addErrorMessage("Please select Service Session");
            return false;
        }
        getChannelReportController().setServiceSession(selectedServiceSessionInstance.getOriginatingSession());

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


    public ChannelScheduleEvent getEvent() {
        if (event == null) {
            event = new ChannelScheduleEvent();
        }
        return event;
    }

    public void setEvent(ChannelScheduleEvent event) {
        this.event = event;
    }

    public Date getSessionStartingDate() {
        if (sessionStartingDate == null) {
            sessionStartingDate = new Date();
        }
        return sessionStartingDate;
    }

    public void setSessionStartingDate(Date sessionStartingDate) {
        this.sessionStartingDate = sessionStartingDate;
    }

    public String getSelectTextSpeciality() {
        return selectTextSpeciality;
    }

    public void setSelectTextSpeciality(String selectTextSpeciality) {
        this.selectTextSpeciality = selectTextSpeciality;
    }

    public String getSelectTextConsultant() {
        return selectTextConsultant;
    }

    public void setSelectTextConsultant(String selectTextConsultant) {
        this.selectTextConsultant = selectTextConsultant;
    }

    public String getSelectTextSession() {
        return selectTextSession;
    }

    public void setSelectTextSession(String selectTextSession) {
        this.selectTextSession = selectTextSession;
    }

    public ArrivalRecord getArrivalRecord() {
        return arrivalRecord;
    }

    public void setArrivalRecord(ArrivalRecord arrivalRecord) {
        this.arrivalRecord = arrivalRecord;
    }

    public FingerPrintRecordFacade getFpFacade() {
        return fpFacade;
    }

    public ServiceSessionLeaveController getServiceSessionLeaveController() {
        return serviceSessionLeaveController;
    }

    public ChannelBillController getChannelBillController() {
        return channelBillController;
    }

    public DoctorSpecialityController getDoctorSpecialityController() {
        return doctorSpecialityController;
    }

    public ChannelStaffPaymentBillController getChannelStaffPaymentBillController() {
        return channelStaffPaymentBillController;
    }

    public PaymentMethod getCanPayMetTmp() {
        return canPayMetTmp;
    }

    public void setCanPayMetTmp(PaymentMethod canPayMetTmp) {
        this.canPayMetTmp = canPayMetTmp;
    }

}
