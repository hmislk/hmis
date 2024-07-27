package com.divudi.bean.channel;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.FeeType;
import com.divudi.data.MessageType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.Sms;
import com.divudi.data.SmsSentResponse;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.StaffFacade;
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
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ChannelStaffPaymentBillController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private BillFacade billFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private PaymentFacade paymentFacade;
    /////////////////

    private CommonFunctions commonFunctions;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    BillSessionFacade billSessionFacade;
    @EJB
    private SmsFacade smsFacade;
    @EJB
    private SmsManagerEjb smsManager;
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    /////////////////////
    private List<BillItem> billItems;
    List<Bill> selectedItems;
    private List<Bill> items = null;
    private List<BillFee> dueBillFeeReport;
    private List<BillFee> filteredBillFee;
    List<BillFee> dueBillFees;
    List<BillFee> payingBillFees;
    private List<BillFee> billFees;
    private List<ServiceSession> serviceSessions;
    private List<ServiceSession> serviceSessionList;
    /////////////////////    
    private Date fromDate;
    private Date toDate;
    private Date date;
    private Bill current;
    Staff currentStaff;
    Institution institution;
    double totalDue;
    double totalPaying;
    private Boolean printPreview = false;
    PaymentMethod paymentMethod;
    Speciality speciality;
    private ServiceSession selectedServiceSession;
    private SessionInstance sessionInstance;
    boolean considerDate = false;
    BillFee billFee;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<BillFee> getBillFees() {
        if (getCurrent() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
                if (billFees == null) {
                    billFees = new ArrayList<BillFee>();
                }
            }
        }

        return billFees;
    }

    private void recreateModel() {
        billFees = null;
        billItems = null;
        printPreview = false;
        billItems = null;
        selectedItems = null;
        items = null;
        dueBillFeeReport = null;
        dueBillFees = null;
        payingBillFees = null;
        billFees = null;
        /////////////////////    
        fromDate = null;
        toDate = null;
        current = null;
        currentStaff = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        paymentMethod = null;
        speciality = null;
        serviceSessions = null;
    }

    public void makenull() {
        billFees = null;
        billItems = null;
        printPreview = false;
        selectedItems = null;
        items = null;
        dueBillFeeReport = null;
        dueBillFees = null;
        payingBillFees = null;
        billFees = null;
        /////////////////////    
        fromDate = null;
        toDate = null;
        current = null;
        currentStaff = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        //printPreview = false;
        paymentMethod = null;
        speciality = null;
        serviceSessions = null;
        serviceSessionList = null;
        sessionInstance = null;
        currentStaff = null;
        dueBillFees = new ArrayList<BillFee>();
        payingBillFees = new ArrayList<BillFee>();
        totalPaying = 0.0;
        totalDue = 0.0;
        recreateModel();
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
//        currentStaff = null;
//        dueBillFees = new ArrayList<BillFee>();
//        payingBillFees = new ArrayList<BillFee>();
//        totalPaying = 0.0;
//        totalDue = 0.0;
    }

//    public List<Staff> completeStaff(String query) {
//        List<Staff> suggestions;
//        String sql;
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            if (speciality != null) {
//                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
//            } else {
//                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
//            }
//            //////// // System.out.println(sql);
//            suggestions = getStaffFacade().findByJpql(sql);
//        }
//        return suggestions;
//    }
    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        if (getSpeciality() != null) {
            if (getSessionController().getApplicationPreference().isShowOnlyMarkedDoctors()) {

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
        suggestions = getStaffFacade().findByJpql(sql, m);

        return suggestions;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(double totalDue) {
        this.totalDue = totalDue;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public void calculateDueFeesOld() {
        if (currentStaff == null || currentStaff.getId() == null || selectedServiceSession == null) {
            dueBillFees = new ArrayList<>();
        } else {
            String sql;
            HashMap h = new HashMap();

            sql = "select b from BillFee b where b.retired=false and (b.bill.billType=:btp or b.bill.billType=:btp2)"
                    + "and b.bill.id in(Select bs.bill.id From BillSession bs where bs.retired=false and bs.serviceSession.id=" + getSelectedServiceSession().getId() + " and bs.sessionDate= :ssDate) "
                    + "and b.bill.cancelled=false and b.bill.refunded=false  and (b.feeValue - b.paidValue) > 0 and b.staff.id = " + currentStaff.getId();
            h.put("btp", BillType.ChannelPaid);
            h.put("btp2", BillType.ChannelCredit);
            h.put("ssDate", getDate());

            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql, h, TemporalType.DATE);

            for (BillFee bf : tmp) {
                if (bf.getBill().getBillType() == BillType.ChannelCredit) {
                    bf.setFeeValue(0.0);
                }
            }

            dueBillFees = tmp;

        }

    }

    public void debugDueFees() {
        Date startTime = new Date();

        if (getSpeciality() == null) {
            JsfUtil.addErrorMessage("Select Specility");
            return;
        }

        if (getCurrentStaff() == null) {
            JsfUtil.addErrorMessage("Select Doctor");
            return;
        }
        if (considerDate) {
            if (getToDate().getTime() > commonFunctions.getEndOfDay().getTime()) {
                JsfUtil.addErrorMessage("You Can't search after current Date");
                return;
            }
        }

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.staff=:stf ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null && considerDate) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        } else {
            sql += " and b.bill.appointmentAt <= :nd";
            hm.put("nd", commonFunctions.getEndOfDay());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.bill.singleBillSession.sessionInstance.originatingSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

//        sql += " and b.bill.singleBillSession.absent=false "
//                + " order by b.bill.singleBillSession.serviceSession.sessionDate,"
//                + " b.bill.singleBillSession.serviceSession.sessionTime,"
//                + " b.bill.singleBillSession.serialNo ";

        sql += " order by b.bill.singleBillSession.serviceSession.sessionDate,"
                + " b.bill.singleBillSession.serviceSession.sessionTime,"
                + " b.bill.singleBillSession.serialNo ";

        hm.put("stf", getCurrentStaff());
        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm.put("class", BilledBill.class);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        //// // System.out.println("dueBillFees.size() = " + dueBillFees.size());
        //// // System.out.println("hm = " + hm);
        //// // System.out.println("sql = " + sql);

        HashMap m = new HashMap();
        sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false  "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.staff=:stf ";

        if (getFromDate() != null && getToDate() != null && considerDate) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            m.put("frm", getFromDate());
            m.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.bill.singleBillSession.serviceSession.originatingSession=:ss";
            m.put("ss", getSelectedServiceSession());
        }

        sql += " and b.bill.singleBillSession.absent=true "
                + " and b.bill.singleBillSession.serviceSession.originatingSession.refundable=false "
                + " order by b.bill.singleBillSession.serviceSession.sessionDate,"
                + " b.bill.singleBillSession.serviceSession.sessionTime,"
                + " b.bill.singleBillSession.serialNo ";
        m.put("stf", getCurrentStaff());
        //hm.put("ins", sessionController.getInstitution());
        m.put("bt", bts);
        m.put("ftp", FeeType.Staff);
        m.put("class", BilledBill.class);
        List<BillFee> nonRefundableBillFees = new ArrayList<>();
        nonRefundableBillFees = billFeeFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        dueBillFees.addAll(nonRefundableBillFees);

    }

    public void calculateDueFees() {
        Date startTime = new Date();

        if (getSpeciality() == null) {
            JsfUtil.addErrorMessage("Select Specility");
            return;
        }

        if (getCurrentStaff() == null) {
            JsfUtil.addErrorMessage("Select Doctor");
            return;
        }
        if (considerDate) {
            if (getToDate().getTime() > commonFunctions.getEndOfDay().getTime()) {
                JsfUtil.addErrorMessage("You Can't search after current Date");
                return;
            }
        }

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false "
                + " and b.bill.cancelled=false "

//                + " and b.bill.singleBillSession.absent=false"

                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.staff=:stf ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null && considerDate) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        } else {
            sql += " and b.bill.appointmentAt <= :nd";
            hm.put("nd", commonFunctions.getEndOfDay());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.bill.singleBillSession.sessionInstance.originatingSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

//        sql += " and b.bill.singleBillSession.absent=false "
//                + " order by b.bill.singleBillSession.serviceSession.sessionDate,"
//                + " b.bill.singleBillSession.serviceSession.sessionTime,"
//                + " b.bill.singleBillSession.serialNo ";
        sql += " order by b.bill.singleBillSession.serviceSession.sessionDate,"
                + " b.bill.singleBillSession.serviceSession.sessionTime,"
                + " b.bill.singleBillSession.serialNo ";

        hm.put("stf", getCurrentStaff());
        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm.put("class", BilledBill.class);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        //// // System.out.println("dueBillFees.size() = " + dueBillFees.size());
        //// // System.out.println("hm = " + hm);
        //// // System.out.println("sql = " + sql);

        HashMap m = new HashMap();
        sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false  "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.staff=:stf ";

        if (getFromDate() != null && getToDate() != null && considerDate) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            m.put("frm", getFromDate());
            m.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.bill.singleBillSession.serviceSession.originatingSession=:ss";
            m.put("ss", getSelectedServiceSession());
        }

        sql += " and b.bill.singleBillSession.absent=true "
                + " and b.bill.singleBillSession.serviceSession.originatingSession.refundable=false "
                + " order by b.bill.singleBillSession.serviceSession.sessionDate,"
                + " b.bill.singleBillSession.serviceSession.sessionTime,"
                + " b.bill.singleBillSession.serialNo ";
        m.put("stf", getCurrentStaff());
        //hm.put("ins", sessionController.getInstitution());
        m.put("bt", bts);
        m.put("ftp", FeeType.Staff);
        m.put("class", BilledBill.class);
        List<BillFee> nonRefundableBillFees = new ArrayList<>();
        nonRefundableBillFees = billFeeFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        dueBillFees.addAll(nonRefundableBillFees);

    }

    public void calculateSessionDueFees() {
        Date startTime = new Date();
        if (getSessionInstance() == null) {
            JsfUtil.addErrorMessage("Select Specility");
            return;
        }

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();
        String sql = " SELECT b "
                + " FROM BillFee b "
                + " where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.bill.singleBillSession.sessionInstance=:si"
                + " and b.bill.singleBillSession.completed=:com";
        sql += " order by b.bill.singleBillSession.serialNo ";
        hm.put("si", getSessionInstance());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm.put("com", true);
        hm.put("class", BilledBill.class);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

        HashMap m = new HashMap();
        sql = " SELECT b "
                + " FROM BillFee b "
                + " where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false  "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.bill.singleBillSession.sessionInstance=:si "
                + " and b.bill.singleBillSession.absent=true "
                + " and b.bill.singleBillSession.serviceSession.originatingSession.refundable=false ";
        sql += " order by b.bill.singleBillSession.serialNo ";
        m.put("si", getSessionInstance());
        m.put("bt", bts);
        m.put("ftp", FeeType.Staff);
        m.put("class", BilledBill.class);
        List<BillFee> nonRefundableBillFees = billFeeFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        dueBillFees.addAll(nonRefundableBillFees);

    }

    public void calculateSessionDoneFees() {
        Date startTime = new Date();
        if (getSessionInstance() == null) {
            JsfUtil.addErrorMessage("Select Specility");
            return;
        }

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap hm = new HashMap();
        String sql = " SELECT b "
                + " FROM BillFee b "
                + " where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false "
                + " and b.bill.cancelled=false "
                + " and b.bill.singleBillSession.absent=false"
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.bill.singleBillSession.sessionInstance=:si"
                + " and b.bill.singleBillSession.completed=:com";
        sql += " order by b.bill.singleBillSession.serialNo ";
        hm.put("si", getSessionInstance());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm.put("com", true);
        hm.put("class", BilledBill.class);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

        HashMap m = new HashMap();
        sql = " SELECT b "
                + " FROM BillFee b "
                + " where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false  "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.bill.singleBillSession.sessionInstance=:si "
                + " and b.bill.singleBillSession.absent=true "
                + " and b.bill.singleBillSession.serviceSession.originatingSession.refundable=false ";
        sql += " order by b.bill.singleBillSession.serialNo ";
        m.put("si", getSessionInstance());
        m.put("bt", bts);
        m.put("ftp", FeeType.Staff);
        m.put("class", BilledBill.class);
        List<BillFee> nonRefundableBillFees = billFeeFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        dueBillFees.addAll(nonRefundableBillFees);

    }

    public void calculateSessionAllFees() {
        if (getSessionInstance() == null) {
            JsfUtil.addErrorMessage("Select Specility");
            return;
        }

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);
        HashMap<String, Object> hm = new HashMap<>();
        String sql = " SELECT b "
                + " FROM BillFee b "
                + " where type(b.bill) in :classes "
                + " and b.bill.retired=false "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.singleBillSession.sessionInstance=:si";
        sql += " order by b.bill.singleBillSession.serialNo ";
        List<Class<?>> classes = new ArrayList<>();
        classes.add(BilledBill.class);

        hm.put("si", getSessionInstance());
        hm.put("ftp", FeeType.Staff);
        hm.put("classes", classes);
        System.out.println("sql = " + sql);
        System.out.println("hm = " + hm);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public void calculateDueFeesAgency() {
        Date startTime = new Date();

        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType=:bt ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.serviceSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

        if (getInstitution() != null) {
            sql += " and b.institution=:ins";
            hm.put("ins", getInstitution());
        }

        //hm.put("ins", sessionController.getInstitution());
        //hm.put("bt", bts);
        hm.put("ftp", FeeType.OtherInstitution);
        hm.put("class", BilledBill.class);
        hm.put("bt", BillType.ChannelAgent);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void calculateTotalDue() {
        if (dueBillFees != null) {
            totalDue = 0;
            for (BillFee f : dueBillFees) {
                totalDue = totalDue + f.getFeeValue() - f.getPaidValue();
            }
        }
    }

    public void performCalculations() {
        calculateTotalDue();
        calculateTotalPay();
    }

    public void calculateTotalPay() {
        totalPaying = 0;
        for (BillFee f : payingBillFees) {
            totalPaying = totalPaying + (f.getFeeValue() - f.getPaidValue());
        }
    }

    public List<ServiceSession> getServiceSessions() {
        serviceSessions = new ArrayList<ServiceSession>();
        String sql;

        if (currentStaff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);
            sql = "Select s From ServiceSession s where s.retired=false and s.staff.id=" + getCurrentStaff().getId() + " and s.sessionWeekday=" + wd;
            serviceSessions = getServiceSessionFacade().findByJpql(sql);
        }

        return serviceSessions;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getDueBillFees() {
        return dueBillFees;
    }

    public void setDueBillFees(List<BillFee> dueBillFees) {
        this.dueBillFees = dueBillFees;
    }

    public List<BillFee> getPayingBillFees() {
        return payingBillFees;
    }

    public void setPayingBillFees(List<BillFee> payingBillFees) {
        this.payingBillFees = payingBillFees;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Staff getCurrentStaff() {
        return currentStaff;

    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;

    }

    public void prepareAdd() {
        current = new BilledBill();
    }

    public void setSelectedItems(List<Bill> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public void fillSessions() {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.staff=:doc "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime";
        m.put("doc", currentStaff);
        m.put("class", ServiceSession.class);
        serviceSessionList = getServiceSessionFacade().findByJpql(sql, m);
    }

    private Bill createPaymentBill() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.ChannelProPayment);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.ChannelProPayment, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));
        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.ChannelProPayment, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));
        tmp.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION);
        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setNetTotal(0 - totalPaying);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setStaff(currentStaff);
        tmp.setToStaff(currentStaff);
        tmp.setTotal(0 - totalPaying);

        return tmp;
    }

    public Payment createPaymentProPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        double valueToSet = 0 - Math.abs(bill.getNetTotal());
        p.setPaidValue(valueToSet);
        if (pm == null) {
            pm = bill.getPaymentMethod();
        }
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
        getPaymentFacade().edit(p);
    }

    private Bill createPaymentBillForSession() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.ChannelProPayment);
        tmp.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.ChannelProPayment, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));
        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.ChannelProPayment, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setNetTotal(0 - totalPaying);
        tmp.setPaymentMethod(paymentMethod);
        if (sessionInstance == null || sessionInstance.getStaff() == null) {
            if (currentStaff != null) {
                tmp.setStaff(currentStaff);
                tmp.setToStaff(currentStaff);
            } else {
                // Handle the case when both sessionInstance and currentStaff are null
                // Depending on your requirements, you might throw an exception or handle it differently
                throw new IllegalStateException("Both sessionInstance and currentStaff are null.");
            }
        } else {
            tmp.setStaff(sessionInstance.getStaff());
            tmp.setToStaff(sessionInstance.getStaff());
        }
        tmp.setTotal(0 - totalPaying);

        return tmp;
    }

    private Bill createPaymentBillAgent() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.ChannelAgencyCommission);
        tmp.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.ChannelAgencyCommission, BillClassType.BilledBill, BillNumberSuffix.AGNPAY));
        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.ChannelAgencyCommission, BillClassType.BilledBill, BillNumberSuffix.AGNPAY));

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setNetTotal(0 - totalPaying);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setToInstitution(institution);
//        tmp.setStaff(currentStaff);
        //tmp.setToStaff(currentStaff);
        tmp.setTotal(0 - totalPaying);

        return tmp;
    }

    private boolean checkBillFeeValue() {
        for (BillFee f : payingBillFees) {
            if (f.getFeeValue() == 0.0) {
                return true;
            }
        }
        return false;
    }

    private boolean errorCheck() {
        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Please select a Staff Memeber");
            return true;
        }

        if (checkBillFeeValue()) {
            JsfUtil.addErrorMessage("There is a Credit Bill");
            return true;
        }

        performCalculations();
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Total Paying Amount is zero. Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    private boolean errorCheckForSessionPayments() {
        if (sessionInstance == null) {
            JsfUtil.addErrorMessage("Error. No Session Instance");
            return true;
        }
        if (checkBillFeeValue()) {
            JsfUtil.addErrorMessage("There is a Credit Bill");
            return true;
        }

        performCalculations();
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Total Paying Amount is zero. Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    private boolean errorCheckForAgency() {
//        if (currentStaff == null) {
//            JsfUtil.addErrorMessage("Please select a Staff Memeber");
//            return true;
//        }

        if (checkBillFeeValue()) {
            JsfUtil.addErrorMessage("There is a Credit Bill");
            return true;
        }

        performCalculations();
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }
        calculateTotalPay();
        Bill b = createPaymentBillForSession();
        current = b;
        getBillFacade().create(b);
        createPaymentProPayment(b, paymentMethod);
        saveBillItemsAndFees(b);
        if (sessionController.getDepartmentPreference().isSendSmsOnChannelBookingDocterPayment()) {
            sendSmsAfterDocPayment();
        }
        printPreview = true;
        currentStaff = null;
        JsfUtil.addSuccessMessage("Successfully Paid");
        //////// // System.out.println("Paid");
    }

    public void settleSessionPaymentBill() {
        if (errorCheckForSessionPayments()) {
            return;
        }
        calculateTotalPay();
        Bill b = createPaymentBill();

        getBillFacade().create(b);
        createPaymentProPayment(b, paymentMethod);
        List<BillItem> bis = saveBillItemsAndFees(b);
        if (bis != null && !bis.isEmpty()) {
            BillSession bs = createBillSession(bis.get(0), sessionInstance);
            b.setSingleBillSession(bs);
            b.setSingleBillItem(bis.get(0));
        }
        current = b;
        if (sessionController.getDepartmentPreference().isSendSmsOnChannelBookingDocterPayment()) {
            sendSmsAfterSessionPayment();
        }
        printPreview = true;
        currentStaff = null;
        JsfUtil.addSuccessMessage("Successfully Paid");
    }

    private BillSession createBillSession(BillItem billItem, SessionInstance si) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(billItem.getBill());
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        bs.setDepartment(si.getDepartment());
        bs.setInstitution(si.getInstitution());
        bs.setSessionInstance(si);
        bs.setSessionDate(si.getSessionDate());
        bs.setSessionTime(si.getSessionTime());
        bs.setStaff(si.getOriginatingSession().getStaff());
        billSessionFacade.create(bs);
        return bs;
    }

    public void sendSmsAfterDocPayment() {
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setBill(current);
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setReceipientNumber(current.getStaff().getPerson().getPhone());
        e.setSendingMessage(generateDoctorPaymentSms(current));
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getLoggedUser().getInstitution());
        e.setPending(false);
        e.setSmsType(MessageType.DoctorPayment);
        getSmsFacade().create(e);
        Boolean sent = smsManager.sendSms(e);
        if (sent) {
            JsfUtil.addSuccessMessage("SMS Sent");
        } else {
            JsfUtil.addSuccessMessage("SMS Failed");
        }
        e.setSentSuccessfully(sent);
        getSmsFacade().edit(e);
    }

    public void sendSmsAfterSessionPayment() {
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setBill(current);
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setReceipientNumber(sessionInstance.getStaff().getPerson().getMobile());
        e.setSendingMessage(generateSessionPaymentSms(current, sessionInstance));
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getLoggedUser().getInstitution());
        e.setPending(false);
        e.setSmsType(MessageType.DoctorPayment);
        getSmsFacade().create(e);
        Boolean sent = smsManager.sendSms(e);
        if (sent) {
            JsfUtil.addSuccessMessage("SMS Sent");
        } else {
            JsfUtil.addSuccessMessage("SMS Failed");
        }
        e.setSentSuccessfully(sent);
        getSmsFacade().edit(e);

    }

    private String generateDoctorPaymentSms(Bill b) {
        String s;
        String template;
        String date = CommonController.getDateFormat(b.getBillDate(),
                "dd MMM");
        //System.out.println("date = " + date);
        String time = CommonController.getDateFormat(
                b.getBillTime(),
                "hh:mm a");
        //System.out.println("time = " + time);
        ServiceSession ss = null;
        if (b != null && b.getSingleBillSession() != null && b.getSingleBillSession().getSessionInstance() != null
                && b.getSingleBillSession().getSessionInstance().getOriginatingSession() != null) {
            ss = b.getSingleBillSession().getSessionInstance().getOriginatingSession();
        }
        if (ss != null && ss.getStartingTime() != null) {
            time = CommonController.getDateFormat(
                    ss.getStartingTime(),
                    "hh:mm a");
        } else {
            //System.out.println("Null Error");
        }
        if (sessionController.getDepartmentPreference().getSmsTemplateForChannelBookingDoctorPayment() == null) {
            String doc = b.getStaff().getPerson().getNameWithTitle();
            s = "Dear "
                    + "{doctor}"
                    + "{dept_id}"
                    + "Your Payment of the "
                    + ""
                    + "{session_name}"
                    + " on "
                    + "{date} "
                    + ""
                    + "Patient Count - "
                    + "{patient_count}"
                    + " and the total is "
                    + "{net_total}"
                    + ". Thank you";
            sessionController.getDepartmentPreference().setSmsTemplateForChannelBookingDoctorPayment(doc);
            template = doc;
        } else {
            template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBookingDoctorPayment();
        }
        s = genarateTemplateForSms(b, template);

        return s;
    }

    private String generateSessionPaymentSms(Bill b, SessionInstance si) {
        String s;
        String template;
        String date = CommonController.getDateFormat(si.getSessionDate(),
                "dd MMM");
        //System.out.println("date = " + date);
        String time = "";
        if (si.getSessionTime() != null) {
            time = CommonController.getDateFormat(
                    si.getSessionTime(),
                    "hh:mm a");
        } else if (si.getOriginatingSession().getStartingTime() != null) {
            time = CommonController.getDateFormat(
                    si.getOriginatingSession().getStartingTime(),
                    "hh:mm a");
        }
        template = sessionController.getDepartmentPreference().getSmsTemplateForChannelBookingDoctorPayment();
        s = genarateTemplateForSms(b, sessionInstance, template);
        return s;
    }

    public String genarateTemplateForSms(Bill b, String input) {
        String s;
        if (b == null) {
            s = "error in bill";
            return s;
        }
        if (b.getSingleBillSession() == null) {
            s = "error in bill session";
            return s;
        }
        if (b.getSingleBillSession().getSessionInstance() == null) {
            s = "error in session Instance";
            return s;
        }
        if (b.getSingleBillSession().getSessionInstance().getOriginatingSession() == null) {
            s = "error in Originating Session";
            return s;
        }

        SessionInstance si = b.getSingleBillSession().getSessionInstance();
        ServiceSession oss = si.getOriginatingSession();

        String time = CommonController.getDateFormat(
                oss.getStartingTime(),
                sessionController.getApplicationPreference().getShortTimeFormat());

        String date = CommonController.getDateFormat(si.getSessionDate(),
                "dd MMM");

        String doc = b.getStaff().getPerson().getNameWithTitle();
        int no = b.getBillItems().size();
        double total = b.getTotal();
        String sessionName = oss.getName();

//        String input = sessionController.getDepartmentPreference().getDocterPaymentSMSTemplate();
        s = input.replace("{doctor}", doc)
                .replace("{patient_count}", String.valueOf(no))
                .replace("{doc}", doc)
                .replace("{time}", time)
                .replace("{date}", date)
                .replace("{No}", String.valueOf(no))
                .replace("{ins_id}", b.getInsId())
                .replace("{dept_id}", b.getDeptId())
                .replace("{net_total}", String.valueOf(-total))
                .replace("{session_name}", sessionName);

        return s;
    }

    public String genarateTemplateForSms(Bill b, SessionInstance sii, String template) {
        String s;
        if (b == null) {
            s = "error in bill";
            return s;
        }

        SessionInstance si = sii;
        ServiceSession oss = si.getOriginatingSession();

        String time = CommonController.getDateFormat(
                oss.getStartingTime(),
                sessionController.getApplicationPreference().getShortTimeFormat());

        String date = CommonController.getDateFormat(si.getSessionDate(),
                "dd MMM");

        String doc = b.getStaff().getPerson().getNameWithTitle();
        int no = b.getBillItems().size();
        double total = b.getTotal();
        String sessionName = oss.getName();

//        String input = sessionController.getDepartmentPreference().getDocterPaymentSMSTemplate();
        s = template.replace("{doctor}", doc)
                .replace("{patient_count}", String.valueOf(no))
                .replace("{doc}", doc)
                .replace("{time}", time)
                .replace("{date}", date)
                .replace("{No}", String.valueOf(no))
                .replace("{ins_id}", b.getInsId())
                .replace("{dept_id}", b.getDeptId())
                .replace("{net_total}", String.valueOf(-total))
                .replace("{session_name}", sessionName);

        return s;
    }

    public void settleBillAgent() {
        if (errorCheckForAgency()) {
            return;
        }
        calculateTotalPay();
        Bill b = createPaymentBillAgent();
        current = b;
        getBillFacade().create(b);
        saveBillItemsAndFees(b);
        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Paid");
        //////// // System.out.println("Paid");
    }

    private List<BillItem> saveBillItemsAndFees(Bill b) {
        List<BillItem> bis = new ArrayList<>();
        for (BillFee bf : getPayingBillFees()) {
            BillItem i = saveBillItemForPaymentBill(b, bf);
            bf.setPaidValue(bf.getFeeValue());
            getBillFeeFacade().edit(bf);
            BillFee nbf = new BillFee();
            nbf.setBillItem(i);
            nbf.setBill(b);
            nbf.setReferenceBillFee(bf);
            nbf.setFeeValue(bf.getFeeValue());
            billFeeFacade.create(nbf);
            bis.add(i);
        }
        return bis;
    }

    private BillItem saveBillItemForPaymentBill(Bill b, BillFee bf) {
        BillItem i = new BillItem();
        i.setReferanceBillItem(bf.getBillItem());
        i.setReferenceBill(bf.getBill());
        i.setPaidForBillFee(bf);
        i.setBill(b);
        i.setCreatedAt(Calendar.getInstance().getTime());
        i.setCreater(getSessionController().getLoggedUser());
        i.setDiscount(0.0);
        i.setGrossValue(bf.getFeeValue());
        i.setNetValue(bf.getFeeValue());
        i.setQty(1.0);
        i.setRate(bf.getFeeValue());
        getBillItemFacade().create(i);
        b.getBillItems().add(i);
        return i;
    }

    public BillFacade getEjbFacade() {
        return billFacade;
    }

    public void setEjbFacade(BillFacade ejbFacade) {
        this.billFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ChannelStaffPaymentBillController() {
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
//        currentStaff = null;
//        dueBillFees = new ArrayList<BillFee>();
//        payingBillFees = new ArrayList<BillFee>();
//        totalPaying = 0.0;
//        totalDue = 0.0;
//        recreateModel();
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
//        getItems();
        current = null;
        getCurrent();
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public Boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(Boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Date getToDate() {
        //Dont Remove Comments if u want ask Safrin
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        //  resetLists();
    }

    public Date getFromDate() {
        //Dont Remove Comments if u want ask Safrin
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        //  resetLists();
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<BillFee> getDueBillFeeReport() {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillFee b where b.retired=false and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and b.bill.institution.id=" + getSessionController().getInstitution().getId() + " and b.bill.billDate between :fromDate and :toDate order by b.staff.id  ";
        //////// // System.out.println("sql is " + sql);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        dueBillFeeReport = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        //////// // System.out.println(dueBillFeeReport.size());

        if (dueBillFeeReport == null) {
            dueBillFeeReport = new ArrayList<>();
        }

        return dueBillFeeReport;
    }

//     sql = "select b from BillFee b where b.retired=false and (b.bill.billType=:btp or b.bill.billType=:btp2)"
//                    + "and b.bill.id in(Select bs.bill.id From BillSession bs where bs.retired=false and bs.serviceSession.id=" + getSelectedServiceSession().getId() + " and bs.sessionDate= :ssDate) "
//                    + "and b.bill.cancelled=false and b.bill.refunded=false  and (b.feeValue - b.paidValue) > 0 and b.staff.id = " + currentStaff.getId();
    public void setDueBillFeeReport(List<BillFee> dueBillFeeReport) {
        this.dueBillFeeReport = dueBillFeeReport;
    }

    public List<BillItem> getBillItems() {
        if (getCurrent() != null) {
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id = " + current.getId();
            billItems = getBillItemFacade().findByJpql(sql);
            if (billItems == null) {
                billItems = new ArrayList<BillItem>();
            }
        }

        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setItems(List<Bill> items) {
        this.items = items;

    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<ServiceSession> getServiceSession() {
        return serviceSessions;
    }

    public void setServiceSession(List<ServiceSession> serviceSession) {
        this.serviceSessions = serviceSession;
    }

    public List<ServiceSession> getServiceSessionList() {
        return serviceSessionList;
    }

    public void setServiceSessionList(List<ServiceSession> serviceSessionList) {
        this.serviceSessionList = serviceSessionList;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;

    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;
    }

    public List<BillFee> getFilteredBillFee() {
        return filteredBillFee;
    }

    public void setFilteredBillFee(List<BillFee> filteredBillFee) {
        this.filteredBillFee = filteredBillFee;
    }

    public boolean isConsiderDate() {
        return considerDate;
    }

    public void setConsiderDate(boolean considerDate) {
        this.considerDate = considerDate;
    }

    public BillFee getBillFee() {
        return billFee;
    }

    public void setBillFee(BillFee billFee) {
        if (billFee != null) {
            setSpeciality(billFee.getSpeciality());
            setCurrentStaff(billFee.getStaff());
            calculateDueFees();
        }
        this.billFee = billFee;
    }

    public SmsManagerEjb getSmsManager() {
        return smsManager;
    }

    public void setSmsManager(SmsManagerEjb smsManager) {
        this.smsManager = smsManager;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
    }

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    /**
     *
     */
    @FacesConverter(forClass = BilledBill.class)
    public static class StaffPaymentBillControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ChannelStaffPaymentBillController controller = (ChannelStaffPaymentBillController) facesContext.getApplication().getELResolver().
                    //getValue(facesContext.getELContext(), null, "billController");
                    getValue(facesContext.getELContext(), null, "staffPaymentBillController");
            return controller.billFacade.find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof BilledBill) {
                BilledBill o = (BilledBill) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ChannelStaffPaymentBillController.class.getName());
            }
        }
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
