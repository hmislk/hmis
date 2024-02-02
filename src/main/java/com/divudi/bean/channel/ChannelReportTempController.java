/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.hr.StaffController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.channel.ReferenceBookEnum;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.ChannelBean;

import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.ServiceSessionLeaveFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.joda.time.LocalDate;

/**
 *
 * @author Sniper619
 */
@Named(value = "channelReportTempController")
@SessionScoped
public class ChannelReportTempController implements Serializable {

    @EJB
    BillSessionFacade billSessionFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    @EJB
    ServiceSessionLeaveFacade serviceSessionLeaveFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    StaffFacade staffFacade;
    @EJB
    SpecialityFacade specialityFacade;
    @EJB
    WebUserFacade webUserFacade;
    //
    @EJB
    ChannelBean channelBean;

    CommonFunctions commonFunctions;
    //
    @Inject
    SessionController sessionController;
    @Inject
    StaffController staffController;
    @Inject
    InstitutionController institutionController;
    @Inject
    SheduleController sheduleController;
    @Inject
    CommonController commonController;
    @Inject
    ChannelReportController channelReportController;
    //
    List<Bill> bills;
    List<AgentReferenceBook> agentReferenceBooks;
    List<ServiceSessionLeave> serviceSessionLeaves;
    List<ChannelSummeryDateRangeBillTotalTable> channelSummeryDateRangeBillTotalTables;
    List<ItemFee> itemFees;
    List<Institution> agencies;
    List<ChannelDateDetailRow> channelDateDetailRows;
    List<ChannelSummeryDateRangeOrUserRow> channelSummeryDateRangeOrUserRows = new ArrayList<>();
    List<AgentHistory> agentHistorys;
    List<ChannelReferenceBookRow> channelReferenceBookRows;
    List<ChannelSheduleSummeryRow> channelSheduleSummeryRows;
    List<String> headers;
    List<ChannelDoctorCountsRow> channelDoctorCountsRows;
    private List<ChannelReportSpecialityWiseSummeryRow> channelReportSpecialityWiseSummeryRows;
    private List<ChannelVatReportPaymentSchemeWiseRow> channelVatReportPaymentSchemeWiseRows;
    private List<ColumnModel> columns;
    private List<String> dates;
    private List<Long> countsList;
    List<ColumnModel> columnModels;
    private List<PaymentMethod> paymentMethods;
    List<ChannelUserSummeryRow> channelUserSummeryRows;
    private String dept = "1";
    private String billtype = "1";
    //
    Date fromDate;
    Date toDate;
    private Institution institution;
    SearchKeyword searchKeyword;
    ReportKeyWord reportKeyWord;
    boolean count;
    boolean billedAgencys;
    boolean withOutDocPayment;
    boolean withDocPayment;
    boolean byDate;
    boolean sessoinDate;
    boolean paid;
    boolean agency;
    boolean scan;
    ChannelTotal channelTotal;
    private double grantTot = 0.0;

    /**
     * Creates a new instance of ChannelReportTempController
     */
    public ChannelReportTempController() {
    }

    public List<Bill> fetchBills(BillType[] billTypes, Class[] bills, Date fd, Date td, Institution billedInstitution, Institution creditCompany, Institution fromInstitution) {

        String sql;
        Map m = new HashMap();

        sql = " select b from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (billTypes != null) {
            sql += " and b.billType in :bt ";
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bt", bts);
        }
        if (bills != null) {
            sql += " and type(b) in :class ";
            List<Class> cs = Arrays.asList(bills);
            m.put("class", cs);
        }
        if (billedInstitution != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", billedInstitution);
        }
        if (fromInstitution != null) {
            sql += " and b.fromInstitution=:fins ";
            m.put("fins", fromInstitution);
        }
        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            m.put("cc", creditCompany);
        }

        sql += " order by b.createdAt ";

        m.put("fromDate", fd);
        m.put("toDate", td);

        return getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchBillsNetTotal(BillType[] billTypes, Class[] bills, Date fd, Date td, Institution billedInstitution, Institution creditCompany, Institution fromInstitution) {

        String sql;
        Map m = new HashMap();

        sql = " select sum(b.netTotal) from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (billTypes != null) {
            sql += " and b.billType in :bt ";
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bt", bts);
        }
        if (bills != null) {
            sql += " and type(b) in :class ";
            List<Class> cs = Arrays.asList(bills);
            m.put("class", cs);
        }
        if (billedInstitution != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", billedInstitution);
        }
        if (fromInstitution != null) {
            sql += " and b.fromInstitution=:fins ";
            m.put("fins", fromInstitution);
        }
        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            m.put("cc", creditCompany);
        }

        sql += " order by b.createdAt ";

        m.put("fromDate", fd);
        m.put("toDate", td);

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchBillsTotal(BillType[] billTypes, BillType bt, Class[] bills, Class[] nbills, Bill b, Date fd, Date td, Institution billedInstitution, Institution creditCompany, boolean withOutDocFee, boolean count, Staff staff, Speciality sp, WebUser webUser) {

        String sql;
        Map m = new HashMap();
        if (count) {
            sql = " select count(b) ";
        } else if (withOutDocFee) {
            sql = " select sum(b.netTotal-b.staffFee) ";
        } else {
            sql = " select sum(b.netTotal) ";
        }

        sql += " from Bill b "
                + " where b.retired=false ";

        if (b.getClass().equals(BilledBill.class)) {
            sql += " and b.singleBillSession.sessionDate between :fromDate and :toDate ";
        }
        if (b.getClass().equals(CancelledBill.class)) {
            sql += " and b.createdAt between :fromDate and :toDate ";
        }
        if (b.getClass().equals(RefundBill.class)) {
            sql += " and b.createdAt between :fromDate and :toDate ";
        }

        if (billTypes != null) {
            sql += " and b.billType in :bt ";
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bt", bts);
        }
        if (bt != null) {
            sql += " and b.billType=:bt ";
            m.put("bt", bt);
        }
        if (bills != null) {
            sql += " and type(b) in :class ";
            List<Class> cs = Arrays.asList(bills);
            m.put("class", cs);
        }
        if (nbills != null) {
            sql += " and type(b) not in :nclass ";
            List<Class> ncs = Arrays.asList(nbills);
            m.put("nclass", ncs);
        }
        if (b != null) {
            sql += " and type(b)=:class ";
            m.put("class", b.getClass());
        }
        if (billedInstitution != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", billedInstitution);
        }
        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            m.put("cc", creditCompany);
        }
        if (staff != null) {
            sql += " and b.staff=:s ";
            m.put("s", staff);
        }
        if (webUser != null) {
            sql += " and b.creater=:wu ";
            m.put("wu", webUser);
        }
        if (sp != null) {
            sql += " and b.staff.speciality=:sp ";
            m.put("sp", sp);
        }
        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:fbt ";
            m.put("fbt", getReportKeyWord().getBillType());
        }

        m.put("fromDate", fd);
        m.put("toDate", td);
        if (count) {
            return getBillFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public double fetchBillsTotalSessoin(BillType[] billTypes, BillType bt, Class[] bills, Class[] nbills, Bill b, Date fd, Date td, Institution billedInstitution, Institution creditCompany, boolean withOutDocFee, boolean count, Staff staff, Speciality sp, WebUser webUser) {

        String sql;
        Map m = new HashMap();
        if (count) {
            sql = " select count(b) ";
        } else if (withOutDocFee) {
            sql = " select sum(b.netTotal-b.staffFee) ";
        } else {
            sql = " select sum(b.netTotal) ";
        }

        sql += " from Bill b "
                + " where b.retired=false"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.singleBillSession.sessionDate between :fd and :td ";

        if (billTypes != null) {
            sql += " and b.billType in :bt ";
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bt", bts);
        }
        if (bt != null) {
            sql += " and b.billType=:bt ";
            m.put("bt", bt);
        }
        if (bills != null) {
            sql += " and type(b) in :class ";
            List<Class> cs = Arrays.asList(bills);
            m.put("class", cs);
        }
        if (nbills != null) {
            sql += " and type(b) not in :nclass ";
            List<Class> ncs = Arrays.asList(nbills);
            m.put("nclass", ncs);
        }
        if (b != null) {
            sql += " and type(b)=:class ";
            m.put("class", b.getClass());
        }
        if (billedInstitution != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", billedInstitution);
        }
        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            m.put("cc", creditCompany);
        }
        if (staff != null) {
            sql += " and b.staff=:s ";
            m.put("s", staff);
        }
        if (webUser != null) {
            sql += " and b.creater=:wu ";
            m.put("wu", webUser);
        }
        if (sp != null) {
            sql += " and b.staff.speciality=:sp ";
            m.put("sp", sp);
        }
        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:fbt ";
            m.put("fbt", getReportKeyWord().getBillType());
        }

        m.put("fromDate", fd);
        m.put("toDate", td);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        if (count) {
            return getBillFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public List<Institution> fetchBillsAgencys() {

        Date fd = commonFunctions.getStartOfMonth(fromDate);
        Date td = commonFunctions.getEndOfMonth(commonFunctions.getStartOfMonth(toDate));

        String sql;
        Map m = new HashMap();

        sql = " select distinct(b.creditCompany) from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.creditCompany is not null "
                + " order by b.creditCompany.name ";

        m.put("fromDate", fd);
        m.put("toDate", td);

        return getInstitutionFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Staff> fetchBillsStaffs(Speciality s, List<BillType> billTypes) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(b.staff) from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.staff is not null ";

        if (s != null) {
            sql += " and b.staff.speciality=:sp ";
            m.put("sp", s);
        }
        if (billTypes != null) {
            sql += " and b.billType in :bts ";
            m.put("bts", billTypes);
        }
        sql += " order by b.staff.person.name ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        return getStaffFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Speciality> fetchBillsSpecilities(List<BillType> billTypes) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(b.staff.speciality) from Bill b "
                + " where b.retired=false "
                + " and ((b.createdAt between :fromDate and :toDate) "
                + " or (b.singleBillSession.sessionDate between :fromDate and :toDate)) "
                + " and b.staff is not null ";

        if (billTypes != null) {
            sql += " and b.billType in :bts ";
            m.put("bts", billTypes);
        }
        sql += " order by b.staff.speciality.name ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        return getSpecialityFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchBillsCount(Staff s, BillType bt) {

        String sql;
        Map m = new HashMap();

        sql = " select count(b) from BilledBill b "
                + " where b.retired=false "
                + " and b.refunded=false "
                + " and b.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (s != null) {
            sql += " and b.staff=:s ";
            m.put("s", s);
        }
        if (bt != null) {
            sql += " and b.billType=:bt ";
            m.put("bt", bt);
        }
        sql += " order by b.staff.person.name ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        return getStaffFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Speciality> fetchBillsSpecilitys() {

        Date fd = commonFunctions.getStartOfMonth(fromDate);
        Date td = commonFunctions.getEndOfMonth(commonFunctions.getStartOfMonth(toDate));

        String sql;
        Map m = new HashMap();

        sql = " select distinct(b.staff.speciality) from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.staff is not null "
                + " order by b.staff.person.name ";

        m.put("fromDate", fd);
        m.put("toDate", td);

        return getSpecialityFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }
    
    
    public List<Speciality> fetchBillFeesSpecilityAndInstitution(List<BillType> bts) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(bf.bill.staff.speciality) from BillFee  bf where "
                + " bf.bill.retired=false"
                + " and bf.bill.refunded=false "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.feeValue>0 ";

        if (scan) {
            sql += " and bf.fee.feeType =:fts ";
            m.put("fts", FeeType.Service);
        } else {
            sql += " and bf.fee.feeType =:fth ";
            m.put("fth", FeeType.OwnInstitution);
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and bf.bill.createdAt between :fd and :td ";
        }
         if (institution!=null) {
            sql += " and bf.bill.singleBillSession.institution = :ins ";
            m.put("ins", institution);
        } 

        sql += " order by bf.bill.staff.speciality.name ";

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("bt", bts);

        return getSpecialityFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<Speciality> fetchBillFeesSpecility(List<BillType> bts) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(bf.bill.staff.speciality) from BillFee  bf where "
                + " bf.bill.retired=false"
                + " and bf.bill.refunded=false "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.billType in :bt "
                + " and type(bf.bill)=:class "
                + " and bf.feeValue>0 ";

        if (scan) {
            sql += " and bf.fee.feeType =:fts ";
            m.put("fts", FeeType.Service);
        } else {
            sql += " and bf.fee.feeType =:fth ";
            m.put("fth", FeeType.OwnInstitution);
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and bf.bill.createdAt between :fd and :td ";
        }

        sql += " order by bf.bill.staff.speciality.name ";

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("bt", bts);

        return getSpecialityFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public Double[] fetchDocCountAndfees(Speciality sp, List<BillType> bts, Staff s) {

        List<Bill> bbills = new ArrayList<>();
        List<Bill> cbills = new ArrayList<>();
        List<Bill> rbills = new ArrayList<>();
        List<Bill> crbills = new ArrayList<>();

        if (scan) {
            bbills = countBillByBillTypeAndFeeType(new BilledBill(), FeeType.Service, bts, sp, s);
            cbills = countBillByBillTypeAndFeeType(new CancelledBill(), FeeType.Service, bts, sp, s);
            rbills = countBillByBillTypeAndFeeType(new RefundBill(), FeeType.Service, bts, sp, s);
        } else {
            bbills = countBillByBillTypeAndFeeType(new BilledBill(), FeeType.OwnInstitution, bts, sp, s);
            cbills = countBillByBillTypeAndFeeType(new CancelledBill(), FeeType.OwnInstitution, bts, sp, s);
            rbills = countBillByBillTypeAndFeeType(new RefundBill(), FeeType.OwnInstitution, bts, sp, s);
        }
        crbills.addAll(cbills);
        crbills.addAll(rbills);

        Double[] d = new Double[3];
        d[0] = 0.0;
        d[1] = 0.0;
        d[2] = 0.0;
        if (!bbills.isEmpty() || !cbills.isEmpty() || !rbills.isEmpty()) {
            d[0] = (double) (bbills.size() - (cbills.size() + rbills.size()));
            for (Bill b : bbills) {
                d[1] += b.getStaffFee();
                d[2] += (b.getNetTotal() - b.getStaffFee());
            }
            for (Bill b : crbills) {
                d[1] -= b.getStaffFee();
                d[2] -= (b.getNetTotal() - b.getStaffFee());
            }
        }

        return d;
    }
    
    
    public Double[] fetchDocCountAndfees(Institution ins, Speciality sp, List<BillType> bts, Staff s) {

        List<Bill> bbills = new ArrayList<>();
        List<Bill> cbills = new ArrayList<>();
        List<Bill> rbills = new ArrayList<>();
        List<Bill> crbills = new ArrayList<>();

        if (scan) {
            bbills = countBillByBillTypeAndFeeType(ins ,new BilledBill(), FeeType.Service, bts, sp, s);
            cbills = countBillByBillTypeAndFeeType(ins ,new CancelledBill(), FeeType.Service, bts, sp, s);
            rbills = countBillByBillTypeAndFeeType(ins ,new RefundBill(), FeeType.Service, bts, sp, s);
        } else {
            bbills = countBillByBillTypeAndFeeType(ins ,new BilledBill(), FeeType.OwnInstitution, bts, sp, s);
            cbills = countBillByBillTypeAndFeeType(ins ,new CancelledBill(), FeeType.OwnInstitution, bts, sp, s);
            rbills = countBillByBillTypeAndFeeType(ins ,new RefundBill(), FeeType.OwnInstitution, bts, sp, s);
        }
        crbills.addAll(cbills);
        crbills.addAll(rbills);

        Double[] d = new Double[3];
        d[0] = 0.0;
        d[1] = 0.0;
        d[2] = 0.0;
        if (!bbills.isEmpty() || !cbills.isEmpty() || !rbills.isEmpty()) {
            d[0] = (double) (bbills.size() - (cbills.size() + rbills.size()));
            for (Bill b : bbills) {
                d[1] += b.getStaffFee();
                d[2] += (b.getNetTotal() - b.getStaffFee());
            }
            for (Bill b : crbills) {
                d[1] -= b.getStaffFee();
                d[2] -= (b.getNetTotal() - b.getStaffFee());
            }
        }

        return d;
    }

//    public Double[] fetchDocCountAndfees(Speciality sp, List<BillType> bts, Staff s) {
//
//        String sql;
//        Map m = new HashMap();
//
//        sql = " select distinct(bf.bill) from BillFee  bf where "
//                + " bf.bill.retired=false"
//                + " and bf.bill.refunded=false "
//                + " and bf.bill.cancelled=false "
//                + " and bf.bill.billType in :bt "
//                + " and bf.bill.staff.speciality=:sp "
//                + " and bf.bill.staff=:s "
//                + " and type(bf.bill)=:class "
//                + " and bf.feeValue>0 ";
//
//        if (scan) {
//            sql += " and bf.fee.feeType =:fts ";
//            m.put("fts", FeeType.Service);
//        } else {
//            sql += " and bf.fee.feeType =:fth ";
//            m.put("fth", FeeType.OwnInstitution);
//        }
//
//        if (paid) {
//            sql += " and bf.bill.paidBill is not null "
//                    + " and bf.bill.paidAmount!=0 ";
//        }
//        if (sessoinDate) {
//            sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
//        } else {
//            sql += " and bf.bill.createdAt between :fd and :td ";
//        }
//
//        m.put("fd", getFromDate());
//        m.put("td", getToDate());
//        m.put("class", BilledBill.class);
//        m.put("bt", bts);
//        m.put("sp", sp);
//        m.put("s", s);
//
//        List<Bill> bs = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//
//        Double[] d = new Double[3];
//        d[0] = 0.0;
//        d[1] = 0.0;
//        d[2] = 0.0;
//        if (!bs.isEmpty()) {
//            d[0] = (double) bs.size();
//            for (Bill b : bs) {
//                d[1] += b.getStaffFee();
//                d[2] += (b.getNetTotal() - b.getStaffFee());
//            }
//        }
//
//        return d;
//    }
    public List<Bill> countBillByBillTypeAndFeeType(Bill bill, FeeType ft, List<BillType> bts, Speciality sp, Staff s) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(bf.bill) from BillFee bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType in :bts "
                + " and bf.bill.staff.speciality=:sp "
                + " and bf.bill.staff=:s "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft "
                + " and bf.feeValue>0 ";

        if (bill.getClass().equals(CancelledBill.class)) {
            sql += " and bf.bill.cancelled=true";
        }
        if (bill.getClass().equals(RefundBill.class)) {
            sql += " and bf.bill.refunded=true";
        }

        if (ft == FeeType.OwnInstitution) {
            sql += " and bf.fee.name =:fn ";
            m.put("fn", "Hospital Fee");
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

        }

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("ft", ft);
        m.put("bts", bts);
        m.put("sp", sp);
        m.put("s", s);
//        m.put("fn", "Scan Fee");
        List<Bill> bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

//        //// // System.out.println("sql = " + sql);
//        //// // System.out.println("m = " + m);
//        //// // System.out.println("bills.size() = " + bills.size());
        return bills;
    }

    
    public List<Bill> countBillByBillTypeAndFeeType(Institution ins, Bill bill, FeeType ft, List<BillType> bts, Speciality sp, Staff s) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(bf.bill) from BillFee bf where "
                + " bf.bill.retired=false "
                + " and bf.bill.billType in :bts "
                + " and bf.bill.staff.speciality=:sp "
                + " and bf.bill.staff=:s "
                + " and type(bf.bill)=:class "
                + " and bf.fee.feeType =:ft "
                + " and bf.feeValue>0 ";

        if (bill.getClass().equals(CancelledBill.class)) {
            sql += " and bf.bill.cancelled=true";
        }
        if (bill.getClass().equals(RefundBill.class)) {
            sql += " and bf.bill.refunded=true";
        }

        if (ft == FeeType.OwnInstitution) {
            sql += " and bf.fee.name =:fn ";
            m.put("fn", "Hospital Fee");
        }
        
         if (ins != null) {
            sql += " and bf.bill.institution =:ins ";
            m.put("ins", ins);
        }

        if (paid) {
            sql += " and bf.bill.paidBill is not null "
                    + " and bf.bill.paidAmount!=0 ";
        }
        if (sessoinDate) {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.singleBillSession.sessionDate between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }
        } else {
            if (bill.getClass().equals(BilledBill.class)) {
                sql += " and bf.bill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(CancelledBill.class)) {
                sql += " and bf.bill.cancelledBill.createdAt between :fd and :td ";
            }
            if (bill.getClass().equals(RefundBill.class)) {
                sql += " and bf.bill.refundedBill.createdAt between :fd and :td ";
            }

        }

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("ft", ft);
        m.put("bts", bts);
        m.put("sp", sp);
        m.put("s", s);
//        m.put("fn", "Scan Fee");
        List<Bill> bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

//        //// // System.out.println("sql = " + sql);
//        //// // System.out.println("m = " + m);
//        //// // System.out.println("bills.size() = " + bills.size());
        return bills;
    }

    
    
    public void createAgentPaymentTable() {
        Date startTime = new Date();

        bills = new ArrayList<>();
        BillType[] bts = {BillType.AgentPaymentReceiveBill};
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class};
        bills = fetchBills(bts, classes, fromDate, toDate, getSessionController().getLoggedUser().getInstitution(), null, getReportKeyWord().getInstitution());
        channelTotal.setNetTotal(fetchBillsNetTotal(bts, classes, fromDate, toDate, getSessionController().getInstitution(), null, getReportKeyWord().getInstitution()));

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Income report/Agent Reports/Agent Deposite(/faces/channel/channel_report_agent_payment_bill.xhtml)");

    }

    public void createCollectingCenterPaymentTable() {
        Date startTime = new Date();

        bills = new ArrayList<>();
        BillType[] bts = {BillType.CollectingCentrePaymentReceiveBill};
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class};
        bills = fetchBills(bts, classes, fromDate, toDate, getSessionController().getLoggedUser().getInstitution(), null, getReportKeyWord().getInstitution());
        channelTotal.setNetTotal(fetchBillsNetTotal(bts, classes, fromDate, toDate, getSessionController().getInstitution(), null, getReportKeyWord().getInstitution()));

        commonController.printReportDetails(fromDate, toDate, startTime, "Payments/Book issuing/Collecting center booki issuing/Collecting center deposists(/faces/reportLab/report_collecting_center_payment_bill.xhtml)");

    }

    public void createChannelAgentReferenceBooks() {
        Date startTime = new Date();
        createAgentReferenceBooks(ReferenceBookEnum.ChannelBook);
        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Income report/Agent Reports/Agent channel book report(/faces/channel/channel_report_agent_referece_book.xhtml)");
    }

    public void createCollectingCenterReferenceBooks() {
        Date startTime = new Date();

        createAgentReferenceBooks(ReferenceBookEnum.LabBook);

        commonController.printReportDetails(fromDate, toDate, startTime, "Payments/Book issuing/Collecting center booki issuing/ Collecting center book report(/faces/reportLab/lab_report_collecting_center_referece_book.xhtml)");
    }

    public void createChannelAgentReferenceBookIssuedBillList() {
        if (!getSearchKeyword().isActiveAdvanceOption()) {
            if (getSearchKeyword().getVal1() == null || "".equals(getSearchKeyword().getVal1())) {
                JsfUtil.addErrorMessage("Please Enter Channel Book Number");
                return;
            }
        } else {
            if (getSearchKeyword().getIns() == null || "".equals(getSearchKeyword().getIns())) {
                JsfUtil.addErrorMessage("Please Enter Agency");
                return;
            }
        }

        createReferenceBookIssuedBillList(ReferenceBookEnum.ChannelBook);
    }

    private void createReferenceBookIssuedBillList(ReferenceBookEnum bookEnum) {
        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a where a.referenceBookEnum=:rb ";
//                + " a.createdAt between :fd and :td ";

        if (!getSearchKeyword().isWithRetiered()) {
            sql += " and a.retired=false ";
        }

        if (getSearchKeyword().getIns() != null) {
            sql += " and a.institution=:ins ";
            m.put("ins", getSearchKeyword().getIns());
        }

        if (getSearchKeyword().getVal1() != null && !getSearchKeyword().getVal1().trim().equals("")) {
            Double dbl = null;
            try {
                dbl = Double.parseDouble(getSearchKeyword().getVal1());
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Please Enter A Number");
                e.printStackTrace();
            }
            sql += " and a.bookNumber=:bn ";
            m.put("bn", dbl);
        }

        m.put("rb", bookEnum);
//        m.put("fd", fromDate);
//        m.put("td", toDate);

        if (!getSearchKeyword().isActiveAdvanceOption()) {
            sql += " order by a.bookNumber ";
            agentReferenceBooks = getAgentReferenceBookFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            sql += " order by a.bookNumber desc ";
            agentReferenceBooks = getAgentReferenceBookFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 5);
        }
        channelReferenceBookRows = new ArrayList<>();
        if (agentReferenceBooks.isEmpty()) {
            JsfUtil.addErrorMessage("This Book Is Not Issued");
        } else {
            for (AgentReferenceBook arb : agentReferenceBooks) {
                channelReferenceBookRows.add(createBookRow(arb));
            }
        }
    }

    public ChannelReferenceBookRow createBookRow(AgentReferenceBook arb) {
        ChannelReferenceBookRow bookRow = new ChannelReferenceBookRow();
        int srn = (int) arb.getStartingReferenceNumber();
        int ern = (int) arb.getEndingReferenceNumber();
        bookRow.setBn((int) arb.getBookNumber());
        bookRow.setRefRange(" " + srn + " - " + ern);
        bookRow.setIns(arb.getInstitution());
        bookRow.setAgentHistorys(channelReportController.createAgentHistoryByBook(arb.getCreatedAt(), new Date(), arb.getInstitution(), Arrays.asList(new HistoryType[]{HistoryType.ChannelBooking}), srn, ern));
        bookRow.setAgentHistoryTotal(channelReportController.createAgentHistoryByBookTotal(arb.getCreatedAt(), new Date(), arb.getInstitution(), Arrays.asList(new HistoryType[]{HistoryType.ChannelBooking}), srn, ern));
        return bookRow;
    }

    public void createAgentReferenceBooks(ReferenceBookEnum bookEnum) {
        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a where "
                + " a.createdAt between :fd and :td ";

        if (!getSearchKeyword().isWithRetiered()) {
            sql += " and a.retired=false ";
        }

        if (getSearchKeyword().getIns() != null) {
            sql += " and a.institution=:ins ";
            m.put("ins", getSearchKeyword().getIns());
        }

        if (getSearchKeyword().isActiveAdvanceOption()) {
            if (getSearchKeyword().getVal1() != null && !getSearchKeyword().getVal1().trim().equals("")) {
                Double dbl = null;
                try {
                    dbl = Double.parseDouble(getSearchKeyword().getVal1());
                } catch (Exception e) {
                    JsfUtil.addErrorMessage("Please Enter A Number");
                    e.printStackTrace();
                }
                sql += " and a.bookNumber=:bn ";
                m.put("bn", dbl);
            }
            if (getSearchKeyword().getVal2() != null && !getSearchKeyword().getVal2().trim().equals("")) {
                Double dbl = null;
                try {
                    dbl = Double.parseDouble(getSearchKeyword().getVal2());
                } catch (Exception e) {
                    JsfUtil.addErrorMessage("Please Enter A Number");
                    e.printStackTrace();
                }
                sql += " and a.startingReferenceNumber=:srn ";
                m.put("srn", dbl);
            }
            if (getSearchKeyword().getVal3() != null && !getSearchKeyword().getVal3().trim().equals("")) {
                Double dbl = null;
                try {
                    dbl = Double.parseDouble(getSearchKeyword().getVal3());
                } catch (Exception e) {
                    JsfUtil.addErrorMessage("Please Enter A Number");
                    e.printStackTrace();
                }
                sql += " and a.endingReferenceNumber=:ern ";
                m.put("ern", dbl);
            }
        }

        sql += " and a.referenceBookEnum=:rb "
                + " order by a.bookNumber ";

        m.put("rb", bookEnum);
        m.put("fd", fromDate);
        m.put("td", toDate);

        agentReferenceBooks = getAgentReferenceBookFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private double calValue(Bill billClass, BillType billType, PaymentMethod paymentMethod, WebUser wUser, Date fd, Date td, boolean df, boolean hf) {
        String sql;
        Map temMap = new HashMap();

        sql = " SELECT ";

        if (!hf && !df) {
            sql += " sum(b.netTotal) ";
        }
        if (hf) {
            sql += " sum(b.netTotal-b.staffFee) ";
        }
        if (df) {
            sql += " sum(b.staffFee) ";
        }

        sql += " FROM Bill b WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.institution=:ins "
                + " and b.singleBillSession.sessionDate between :fd and :td "
                + " and b.createdAt between :fdc and :tdc ";

        if (billClass != null) {
            sql += " and type(b)=:bill ";
            temMap.put("bill", billClass.getClass());
        }

        if (wUser != null) {
            sql += " and b.creater=:w ";
            temMap.put("w", wUser);
        }

        if (paymentMethod == PaymentMethod.OnCall || paymentMethod == PaymentMethod.Staff) {
            if (billClass instanceof BilledBill) {
                sql += " and b.referenceBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            } else {
                sql += " and b.billedBill.referenceBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            }

        } else {
            sql += " and b.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:bt ";
            temMap.put("bt", getReportKeyWord().getBillType());
        }

        temMap.put("fd", fd);
        temMap.put("td", td);
        temMap.put("fdc", commonFunctions.getStartOfDay(getFromDate()));
        temMap.put("tdc", commonFunctions.getEndOfDay(getFromDate()));
        temMap.put("btp", billType);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calVatValue(Bill billClass, BillType billType, PaymentMethod paymentMethod, WebUser wUser) {
        String sql;
        Map temMap = new HashMap();

        sql = " SELECT sum(b.vat) "
                + " FROM Bill b WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.institution=:ins "
                + " and b.createdAt between :fdc and :tdc ";

        if (billClass != null) {
            sql += " and type(b)=:bill ";
            temMap.put("bill", billClass.getClass());
        }

        if (wUser != null) {
            sql += " and b.creater=:w ";
            temMap.put("w", wUser);
        }

        if (paymentMethod == PaymentMethod.OnCall || paymentMethod == PaymentMethod.Staff) {
            if (billClass instanceof BilledBill) {
                sql += " and b.referenceBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            } else {
                sql += " and b.billedBill.referenceBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            }

        } else {
            sql += " and b.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:bt ";
            temMap.put("bt", getReportKeyWord().getBillType());
        }

        temMap.put("fdc", commonFunctions.getStartOfDay(getFromDate()));
        temMap.put("tdc", commonFunctions.getEndOfDay(getFromDate()));
        temMap.put("btp", billType);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValue(Bill billClass, BillType billType, PaymentMethod paymentMethod, WebUser wUser, Date fd, Date td, boolean sd, boolean df, boolean hf) {
        String sql;
        Map temMap = new HashMap();

        sql = " SELECT ";

        if (!hf && !df) {
            sql += " sum(b.netTotal) ";
        }
        if (hf) {
            sql += " sum(b.netTotal-b.staffFee) ";
        }
        if (df) {
            sql += " sum(b.staffFee) ";
        }

        sql += " FROM Bill b WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.institution=:ins ";

        if (billClass != null) {
            sql += " and type(b)=:bill ";
            temMap.put("bill", billClass.getClass());
        }

        if (sd) {
            sql += " and b.singleBillSession.sessionDate between :fd and :td ";
        } else {
            sql += " and b.createdAt between :fd and :td ";
        }

        if (wUser != null) {
            sql += " and b.creater=:w ";
            temMap.put("w", wUser);
        }

        if (paymentMethod == PaymentMethod.OnCall || paymentMethod == PaymentMethod.Staff) {
            if (billClass instanceof BilledBill) {
                sql += " and b.referenceBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            } else {
                sql += " and b.billedBill.referenceBill.paymentMethod=:pm ";
                temMap.put("pm", paymentMethod);
            }

        } else {
            sql += " and b.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (getReportKeyWord().getBillType() != null) {
            sql += " and b.singleBillSession.serviceSession.originatingSession.forBillType=:bt ";
            temMap.put("bt", getReportKeyWord().getBillType());
        }

        temMap.put("fd", fd);
        temMap.put("td", td);
        temMap.put("btp", billType);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createUsercollectionByDate() {
        Date startTime = new Date();

        if (getReportKeyWord().getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select User.");
            return;
        }
        channelDateDetailRows = new ArrayList<>();
        channelTotal = new ChannelTotal();
        Date nowDate = getFromDate();
        while (nowDate.before(getToDate())) {
            ChannelDateDetailRow row = new ChannelDateDetailRow();
            String formatedDate;
            Date fd;
            Date td;
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);
            //// // System.out.println("td = " + td);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);
            row.setDate(formatedDate);
            row.setCash(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Cash, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Cash, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Cash, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment));
            channelTotal.cashTotal += row.getCash();

            row.setAgent(calValue(new BilledBill(), BillType.ChannelAgent, PaymentMethod.Agent, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelAgent, PaymentMethod.Agent, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelAgent, PaymentMethod.Agent, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment));
            channelTotal.agentTotal += row.getAgent();

            row.setCard(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Card, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Card, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Card, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment));
            channelTotal.cardTotal += row.getCard();

            row.setCheque(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Cheque, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Cheque, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Cheque, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment));
            channelTotal.chequeTotal += row.getCheque();

            row.setOnCall(calValue(new BilledBill(), BillType.ChannelPaid, PaymentMethod.OnCall, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelPaid, PaymentMethod.OnCall, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelPaid, PaymentMethod.OnCall, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment));
            channelTotal.onCallTotal += row.getOnCall();

            row.setSlip(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Slip, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Slip, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Slip, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment));
            channelTotal.slipTotal += row.getSlip();

            row.setStaff(calValue(new BilledBill(), BillType.ChannelPaid, PaymentMethod.Staff, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelPaid, PaymentMethod.Staff, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelPaid, PaymentMethod.Staff, reportKeyWord.getWebUser(), fd, td, sessoinDate, withDocPayment, withOutDocPayment));
            channelTotal.staffTotal += row.getStaff();

            channelDateDetailRows.add(row);
            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            cal.add(Calendar.DATE, 1);
            nowDate = cal.getTime();
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/New Channel report/Today all my booking summery(/faces/channel/channel_report_cashier_summery_date.xhtml)");
    }

    public void createUsercollectionByDateCreated() {
        Date startTime = new Date();

        if (getReportKeyWord().getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select User.");
            return;
        }
        Date todayLastBillDate = fetchTodaybill(Arrays.asList(new BillType[]{BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelPaid}), reportKeyWord.getWebUser(), false);
        if (todayLastBillDate == null) {
            JsfUtil.addErrorMessage("This User Has not Bill any Bill Selected Day");
            return;
        }
        channelDateDetailRows = new ArrayList<>();
        channelTotal = new ChannelTotal();
        Date nowDate = getFromDate();
//        while (nowDate.before(getToDate())) {
        while (nowDate.before(todayLastBillDate) || nowDate.equals(todayLastBillDate)) {
            ChannelDateDetailRow row = new ChannelDateDetailRow();
            String formatedDate;
            Date fd;
            Date td;
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);
            //// // System.out.println("td = " + td);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);
            row.setDate(formatedDate);
            row.setCash(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Cash, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Cash, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Cash, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment));
            channelTotal.cashTotal += row.getCash();

            row.setAgent(calValue(new BilledBill(), BillType.ChannelAgent, PaymentMethod.Agent, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelAgent, PaymentMethod.Agent, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelAgent, PaymentMethod.Agent, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment));
            channelTotal.agentTotal += row.getAgent();

            row.setCard(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Card, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Card, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Card, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment));
            channelTotal.cardTotal += row.getCard();

            row.setCheque(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Cheque, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Cheque, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Cheque, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment));
            channelTotal.chequeTotal += row.getCheque();

            row.setOnCall(calValue(new BilledBill(), BillType.ChannelPaid, PaymentMethod.OnCall, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelPaid, PaymentMethod.OnCall, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelPaid, PaymentMethod.OnCall, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment));
            channelTotal.onCallTotal += row.getOnCall();

            row.setSlip(calValue(new BilledBill(), BillType.ChannelCash, PaymentMethod.Slip, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Slip, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Slip, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment));
            channelTotal.slipTotal += row.getSlip();

            row.setStaff(calValue(new BilledBill(), BillType.ChannelPaid, PaymentMethod.Staff, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new CancelledBill(), BillType.ChannelPaid, PaymentMethod.Staff, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment)
                    + calValue(new RefundBill(), BillType.ChannelPaid, PaymentMethod.Staff, reportKeyWord.getWebUser(), fd, td, withDocPayment, withOutDocPayment));
            channelTotal.staffTotal += row.getStaff();

            if (row.getCash() != 0 || row.getAgent() != 0 || row.getCard() != 0 || row.getCheque() != 0 || row.getOnCall() != 0 || row.getSlip() != 0 || row.getStaff() != 0) {
                channelDateDetailRows.add(row);
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            cal.add(Calendar.DATE, 1);
            nowDate = cal.getTime();
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/New Channel report/Today all my booking summery(credit date)(/faces/channel/channel_report_cashier_summery_date_created.xhtml)");
    }

    public void createUsercollectionByDateCreatedSummery() {
        Date startTime = new Date();
        channelUserSummeryRows = new ArrayList<>();

        for (WebUser wu : getCashiers()) {
            Date todayLastBillDate = fetchTodaybill(Arrays.asList(new BillType[]{BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelPaid}), wu, false);
            if (todayLastBillDate == null) {
                JsfUtil.addErrorMessage("This User Has not Bill any Bill Selected Day");
                return;
            }
            ChannelUserSummeryRow row = new ChannelUserSummeryRow();
            row.setUser(wu);
            row.setDateDetailRows(fetchUserSummeryRows(wu, todayLastBillDate));
            channelUserSummeryRows.add(row);

        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/New Channel report/Today all my booking summery(credit date)(/faces/channel/channel_report_cashier_summery_date_created.xhtml)");
    }

    public List<ChannelSummeryDateRangeRow> fetchUserSummeryRows(WebUser user, Date todayLastBillDate) {
        Date nowDate = getFromDate();
        double total = 0.0;
        List<ChannelSummeryDateRangeRow> rows = new ArrayList<>();
        ChannelSummeryDateRangeRow row = new ChannelSummeryDateRangeRow();

        row.setDate("Hospital Fees");
        row.setTotalHosFee(fetchTotalUserRow(nowDate, todayLastBillDate, user, true, false));
        row.setBold(false);
        rows.add(row);
        total = row.getTotalHosFee();

//        while (nowDate.before(getToDate())) {
        while (nowDate.before(todayLastBillDate) || nowDate.equals(todayLastBillDate)) {
            ChannelSummeryDateRangeRow ro = new ChannelSummeryDateRangeRow();
            String formatedDate;
            Date fd;
            Date td;
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);
            //// // System.out.println("td = " + td);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);

            ro.setDate(formatedDate + " Professional Payment");
            ro.setTotalHosFee(fetchTotalUserRow(fd, td, user, false, true));
            ro.setBold(false);
            if (ro.getTotalHosFee() != 0) {
                rows.add(ro);
                total += ro.getTotalHosFee();
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            cal.add(Calendar.DATE, 1);
            nowDate = cal.getTime();
        }
        row = new ChannelSummeryDateRangeRow();
        row.setDate("VAT 15%");
        row.setTotalHosFee(fetchTotalUserRowVat(user));
        row.setBold(false);
        rows.add(row);
        total += row.getTotalHosFee();

        row = new ChannelSummeryDateRangeRow();
        row.setDate("Net Total");
        row.setTotalHosFee(total);
        row.setBold(true);
        rows.add(row);

        return rows;
    }

    public double fetchTotalUserRow(Date fd, Date td, WebUser wu, boolean hf, boolean sf) {
        double d = 0;
        d = fetchTotal(BillType.ChannelPaid, PaymentMethod.Staff, fd, td, wu, hf, sf)
                + fetchTotal(BillType.ChannelCash, PaymentMethod.Slip, fd, td, wu, hf, sf)
                + fetchTotal(BillType.ChannelPaid, PaymentMethod.OnCall, fd, td, wu, hf, sf)
                + fetchTotal(BillType.ChannelCash, PaymentMethod.Card, fd, td, wu, hf, sf)
                + fetchTotal(BillType.ChannelCash, PaymentMethod.Cheque, fd, td, wu, hf, sf)
                + fetchTotal(BillType.ChannelAgent, PaymentMethod.Agent, fd, td, wu, hf, sf)
                + fetchTotal(BillType.ChannelCash, PaymentMethod.Cash, fd, td, wu, hf, sf);
        return d;
    }

    public double fetchTotalUserRowVat(WebUser wu) {
        double d = 0;
        d = fetchTotalVat(BillType.ChannelPaid, PaymentMethod.Staff, wu)
                + fetchTotalVat(BillType.ChannelCash, PaymentMethod.Slip, wu)
                + fetchTotalVat(BillType.ChannelPaid, PaymentMethod.OnCall, wu)
                + fetchTotalVat(BillType.ChannelCash, PaymentMethod.Card, wu)
                + fetchTotalVat(BillType.ChannelCash, PaymentMethod.Cheque, wu)
                + fetchTotalVat(BillType.ChannelAgent, PaymentMethod.Agent, wu)
                + fetchTotalVat(BillType.ChannelCash, PaymentMethod.Cash, wu);
        return d;
    }

    public double fetchTotal(BillType bt, PaymentMethod pm, Date fd, Date td, WebUser wu, boolean hf, boolean sf) {
        double d = 0;

        d = calValue(new BilledBill(), bt, pm, wu, fd, td, sf, hf)
                + calValue(new CancelledBill(), bt, pm, wu, fd, td, sf, hf)
                + calValue(new RefundBill(), bt, pm, wu, fd, td, sf, hf);

        return d;
    }

    public double fetchTotalVat(BillType bt, PaymentMethod pm, WebUser wu) {
        double d = 0;

        d = calVatValue(new BilledBill(), bt, pm, wu)
                + calVatValue(new CancelledBill(), bt, pm, wu)
                + calVatValue(new RefundBill(), bt, pm, wu);

        return d;
    }

    public void updateDecactivateAgentBook(AgentReferenceBook a) {

        a.setEditor(getSessionController().getLoggedUser());
        a.setEditedAt(new Date());
        getAgentReferenceBookFacade().edit(a);

        UtilityController.addSuccessMessage("Updated");
        createAgentReferenceBooks(ReferenceBookEnum.ChannelBook);
    }

    public void updateDecactivateCCBook(AgentReferenceBook a) {

        a.setEditor(getSessionController().getLoggedUser());
        a.setEditedAt(new Date());
        getAgentReferenceBookFacade().edit(a);

        UtilityController.addSuccessMessage("Updated");
        createAgentReferenceBooks(ReferenceBookEnum.LabBook);
    }

    public void createConsultantLeaves() {
        String sql;
        HashMap m = new HashMap();

        sql = "Select s From ServiceSessionLeave s Where s.sessionDate between :fd and :td ";

        if (getReportKeyWord().getStaff() != null) {
            sql += "  and s.staff=:st";
            m.put("st", getReportKeyWord().getStaff());
        }

        sql += " order by s.sessionDate ";

        m.put("fd", fromDate);
        m.put("td", toDate);

        serviceSessionLeaves = getServiceSessionLeaveFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void createChannelCountByUserOrDate() {
        Date startTime = new Date();

        channelSummeryDateRangeOrUserRows = new ArrayList<>();
        channelTotal = new ChannelTotal();
        BillType[] bts;
        if (agency) {
            bts = new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent,};
        } else {
            bts = new BillType[]{BillType.ChannelCash, BillType.ChannelPaid,};
        }

        if (byDate) {
            Date nowDate = getFromDate();
            while (nowDate.before(getToDate())) {
                ChannelSummeryDateRangeOrUserRow row = new ChannelSummeryDateRangeOrUserRow();
                String formatedDate;
                Date fd;
                Date td;
                fd = commonFunctions.getStartOfDay(nowDate);
                td = commonFunctions.getEndOfDay(nowDate);

                DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
                formatedDate = df.format(fd);
                row.setDate(formatedDate);
                row.setUserRows(fetchUserRows(fd, td, bts));
                if (row.getUserRows().size() > 1) {
                    channelSummeryDateRangeOrUserRows.add(row);
                }

                Calendar cal = Calendar.getInstance();
                cal.setTime(nowDate);
                cal.add(Calendar.DATE, 1);
                nowDate = cal.getTime();
            }

        } else {
            for (WebUser webUser : fetchCashiers(bts)) {
                ChannelSummeryDateRangeOrUserRow row = new ChannelSummeryDateRangeOrUserRow();
                row.setUser(webUser);
                row.setDateRangeRows(fetchDateRangeRows(getFromDate(), getToDate(), webUser, bts));
                if (row.getDateRangeRows().size() > 1) {
                    channelSummeryDateRangeOrUserRows.add(row);
                }
            }
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/New Channel report/Channel count by users(/faces/channel/report_cashier_vise_count.xhtml)");

    }

    public void createChannelCountByUserOrDate2() {
        Date startTime = new Date();

        long lng = getCommonFunctions().getDayCount(getFromDate(), getToDate());

        if (Math.abs(lng) > 2) {
            UtilityController.addErrorMessage("Date Range is too Long");
            return;
        }
        channelSummeryDateRangeOrUserRows = new ArrayList<>();
        channelTotal = new ChannelTotal();
        BillType[] bts;
        bts = new BillType[]{BillType.ChannelCash, BillType.ChannelPaid,};

        for (WebUser webUser : fetchCashiersSession(bts)) {
            ChannelSummeryDateRangeOrUserRow row = new ChannelSummeryDateRangeOrUserRow();
            row.setUser(webUser);
            //
            String sql;
            Map m = new HashMap();

            sql = "select b from Bill b "
                    + " where b.retired=false "
                    + " and b.singleBillSession.sessionDate between :fromDate and :toDate"
                    + " and b.billType in :bt "
                    + " and b.creater=:wu";

            m.put("bt", Arrays.asList(bts));
            m.put("wu", webUser);

            m.put("fromDate", getFromDate());
            m.put("toDate", getToDate());
            List<Bill> bills = new ArrayList<>();
            bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
            Date fd = getFromDate();
            for (Bill b : bills) {
                if (b.getCreatedAt().getTime() < fd.getTime()) {
                    fd = b.getCreatedAt();
                }
            }

            //
            row.setDateRangeRows(fetchDateRangeRowsSession(fd, commonFunctions.getEndOfDay(new Date()), webUser, bts));
            if (row.getDateRangeRows().size() > 1) {
                channelSummeryDateRangeOrUserRows.add(row);
            }
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/New Channel report/Channel count by users(by appoinment date)(/faces/channel/report_cashier_vise_count_1.xhtml)");

    }

    public List<ChannelSummeryDateRangeBillTotalRow> fetchChannelSummeryRows(Institution i, BillType bt, BillType[] bts, boolean withOutDoc, boolean count, Staff s, boolean byDate, Speciality sp) {
        List<ChannelSummeryDateRangeBillTotalRow> acsrs = new ArrayList<>();
        Date nowDate = getFromDate();
        double btot = 0.0;
        double ctot = 0.0;
        double rtot = 0.0;
        while (nowDate.before(getToDate())) {
            String formatedDate;
            Date fd;
            Date td;
            if (byDate) {
                fd = commonFunctions.getStartOfDay(nowDate);
                td = commonFunctions.getEndOfDay(nowDate);

                DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
                formatedDate = df.format(fd);

            } else {
                fd = commonFunctions.getStartOfMonth(nowDate);
                td = commonFunctions.getEndOfMonth(nowDate);

                DateFormat df = new SimpleDateFormat("yyyy MMMM");
                formatedDate = df.format(fd);
            }

            ChannelSummeryDateRangeBillTotalRow acsr = new ChannelSummeryDateRangeBillTotalRow();
            acsr.setDate(formatedDate);

            acsr.setCanceledTotal(fetchBillsTotal(bts, bt, null, null, new CancelledBill(), fd, td, null, i, withOutDoc, count, s, sp, null));
            ctot += acsr.getCanceledTotal();
            acsr.setRefundTotal(fetchBillsTotal(bts, bt, null, null, new RefundBill(), fd, td, null, i, withOutDoc, count, s, sp, null));
            rtot += acsr.getRefundTotal();
            acsr.setBillTotal(fetchBillsTotal(bts, bt, null, null, new BilledBill(), fd, td, null, i, withOutDoc, count, s, sp, null));
            btot += acsr.getBillTotal();

            acsrs.add(acsr);

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            if (byDate) {
                cal.add(Calendar.DATE, 1);
            } else {
                cal.add(Calendar.MONTH, 1);
            }
            nowDate = cal.getTime();
        }
        ChannelSummeryDateRangeBillTotalRow acsr = new ChannelSummeryDateRangeBillTotalRow();
        acsr.setDate("Total");
        acsr.setBillTotal(btot);
        acsr.setCanceledTotal(ctot);
        acsr.setRefundTotal(rtot);
        acsr.setBold(true);
        acsrs.add(acsr);
        return acsrs;
    }

    public List<Long> fetchChannelDocCountsRows(Institution i, BillType bt, BillType[] bts, boolean withOutDoc, boolean count, Staff s, boolean byDate, Speciality sp) {
        List<Long> ls = new ArrayList<>();
        Date nowDate = getFromDate();
        double netTot = 0.0;
        while (nowDate.before(getToDate())) {
            String formatedDate;
            Date fd;
            Date td;
            if (byDate) {
                fd = commonFunctions.getStartOfDay(nowDate);
                td = commonFunctions.getEndOfDay(nowDate);

                DateFormat df = new SimpleDateFormat("yy MM dd");
                formatedDate = df.format(fd);

            } else {
                fd = commonFunctions.getStartOfMonth(nowDate);
                td = commonFunctions.getEndOfMonth(commonFunctions.getStartOfMonth(nowDate));

                DateFormat df = new SimpleDateFormat("yy MM");
                formatedDate = df.format(fd);
            }
            double tmpTot = fetchBillsTotal(bts, bt, null, null, new BilledBill(), fd, td, null, i, withOutDoc, count, s, sp, null)
                    - (fetchBillsTotal(bts, bt, null, null, new CancelledBill(), fd, td, null, i, withOutDoc, count, s, sp, null)
                    + fetchBillsTotal(bts, bt, null, null, new RefundBill(), fd, td, null, i, withOutDoc, count, s, sp, null));

            ls.add((long) tmpTot);
            netTot += tmpTot;

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            if (byDate) {
                cal.add(Calendar.DATE, 1);
            } else {
                cal.add(Calendar.MONTH, 1);
            }
            nowDate = cal.getTime();
        }
        ls.add((long) netTot);
        return ls;
    }

    public List<String> fetchChannelHeaders() {
        headers = new ArrayList<>();
        Date nowDate = getFromDate();
        double netTot = 0.0;
        while (nowDate.before(getToDate())) {
            String formatedDate;
            Date fd;
            Date td;
            if (byDate) {
                fd = commonFunctions.getStartOfDay(nowDate);
                td = commonFunctions.getEndOfDay(nowDate);

                DateFormat df = new SimpleDateFormat(" yy MM dd ");
                formatedDate = df.format(fd);

            } else {
                fd = commonFunctions.getStartOfMonth(nowDate);
                td = commonFunctions.getEndOfMonth(nowDate);

                DateFormat df = new SimpleDateFormat(" yyyy MMM ");
                formatedDate = df.format(fd);
            }
            headers.add(formatedDate);

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            if (byDate) {
                cal.add(Calendar.DATE, 1);
            } else {
                cal.add(Calendar.MONTH, 1);
            }
            nowDate = cal.getTime();
        }
        headers.add("Total");

        return headers;
    }

    public List<ChannelSummeryDateRangeBillTotalRow> fetchChannelSummeryRows(Speciality sp, List<BillType> bts) {
        List<ChannelSummeryDateRangeBillTotalRow> acsrs = new ArrayList<>();

        double totalCount = 0;
        double totalDocfee = 0;
        double totalHosfee = 0;

        for (Staff s : fetchBillsStaffs(sp, null)) {

            ChannelSummeryDateRangeBillTotalRow acsr = new ChannelSummeryDateRangeBillTotalRow();
            Double[] d = new Double[3];
            d = fetchDocCountAndfees(sp, bts, s);

            acsr.setStaff(s);
            acsr.setBillTotal(d[0]);
            acsr.setTotalDocFee(d[1]);
            acsr.setTotalHosFee(d[2]);
            if (acsr.getBillTotal() != 0.0 || acsr.getTotalDocFee() != 0.0 || acsr.getTotalHosFee() != 0.0) {
                acsrs.add(acsr);
            }

            totalCount += d[0];
            totalDocfee += d[1];
            totalHosfee += d[2];
        }

        ChannelSummeryDateRangeBillTotalRow acsr = new ChannelSummeryDateRangeBillTotalRow();
        acsr.setDate("Total");
        acsr.setBillTotal(totalCount);
        acsr.setTotalDocFee(totalDocfee);
        acsr.setTotalHosFee(totalHosfee);
        acsr.setBold(true);
        acsrs.add(acsr);
        return acsrs;
    }

    
    public List<ChannelSummeryDateRangeBillTotalRow> fetchChannelSummeryRows(Institution ins,Speciality sp, List<BillType> bts) {
        List<ChannelSummeryDateRangeBillTotalRow> acsrs = new ArrayList<>();

        double totalCount = 0;
        double totalDocfee = 0;
        double totalHosfee = 0;

        for (Staff s : fetchBillsStaffs(sp, null)) {

            ChannelSummeryDateRangeBillTotalRow acsr = new ChannelSummeryDateRangeBillTotalRow();
            Double[] d = new Double[3];
            d = fetchDocCountAndfees(ins, sp, bts, s);

            acsr.setStaff(s);
            acsr.setBillTotal(d[0]);
            acsr.setTotalDocFee(d[1]);
            acsr.setTotalHosFee(d[2]);
            if (acsr.getBillTotal() != 0.0 || acsr.getTotalDocFee() != 0.0 || acsr.getTotalHosFee() != 0.0) {
                acsrs.add(acsr);
            }

            totalCount += d[0];
            totalDocfee += d[1];
            totalHosfee += d[2];
        }

        ChannelSummeryDateRangeBillTotalRow acsr = new ChannelSummeryDateRangeBillTotalRow();
        acsr.setDate("Total");
        acsr.setBillTotal(totalCount);
        acsr.setTotalDocFee(totalDocfee);
        acsr.setTotalHosFee(totalHosfee);
        acsr.setBold(true);
        acsrs.add(acsr);
        return acsrs;
    }

    
    
    public double[] fetchChannelBillTypeBilCounts(Staff s, List<BillType> bts) {
        double[] d = new double[4];
        int i = 0;
        for (BillType bt : bts) {
            d[i] = fetchBillsCount(s, bt);
            i++;
        }

        return d;
    }

    public void fetchAgentWiseChannelTotal() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        List<Institution> institutions = new ArrayList<>();
        if (billedAgencys) {
            institutions.addAll(fetchBillsAgencys());
        } else {
            institutions.addAll(getInstitutionController().getAgencies());
        }
        for (Institution a : institutions) {
            ChannelSummeryDateRangeBillTotalTable aws = new ChannelSummeryDateRangeBillTotalTable();
            aws.setAgency(a);
            aws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(a, BillType.ChannelAgent, null, withOutDocPayment, count, null, byDate, null));
            channelSummeryDateRangeBillTotalTables.add(aws);
        }
    }

    public void fetchStaffWiseChannelTotalOrCount() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        for (Staff s : fetchBillsStaffs(null, null)) {
            ChannelSummeryDateRangeBillTotalTable sws = new ChannelSummeryDateRangeBillTotalTable();
            sws.setStaff(s);
            sws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(null, null, new BillType[]{BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff}, withOutDocPayment, count, s, byDate, null));
            channelSummeryDateRangeBillTotalTables.add(sws);
        }
    }

    public void fetchSpecilityWiseChannelTotalOrCount() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        for (Speciality sp : fetchBillsSpecilitys()) {
            ChannelSummeryDateRangeBillTotalTable sws = new ChannelSummeryDateRangeBillTotalTable();
            sws.setSpeciality(sp);
            sws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(null, null, new BillType[]{BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff}, withOutDocPayment, count, null, byDate, sp));
            channelSummeryDateRangeBillTotalTables.add(sws);
        }
    }

    public void fetchStaffWiseChannelCount() {

        channelDoctorCountsRows = new ArrayList<>();
        if (getReportKeyWord().getStaff() != null) {
            ChannelDoctorCountsRow row = new ChannelDoctorCountsRow();
            row.setStaff(getReportKeyWord().getStaff());
            row.setCounts(fetchChannelDocCountsRows(null, null, new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, false, true, getReportKeyWord().getStaff(), byDate, null));
            channelDoctorCountsRows.add(row);
        } else {
            for (Staff s : fetchBillsStaffs(getReportKeyWord().getSpeciality(), Arrays.asList(new BillType[]{BillType.ChannelPaid, BillType.ChannelCash, BillType.ChannelAgent}))) {
                ChannelDoctorCountsRow row = new ChannelDoctorCountsRow();
                row.setStaff(s);
                row.setCounts(fetchChannelDocCountsRows(null, null, new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, false, true, s, byDate, null));
                channelDoctorCountsRows.add(row);
            }
        }

    }

    public void fetchSpecilityWiseChannelCount() {

        channelDoctorCountsRows = new ArrayList<>();

        for (Speciality s : fetchBillsSpecilities(Arrays.asList(new BillType[]{BillType.ChannelPaid, BillType.ChannelCash, BillType.ChannelAgent}))) {
            ChannelDoctorCountsRow row = new ChannelDoctorCountsRow();
            row.setSpeciality(s);
            row.setCounts(fetchChannelDocCountsRows(null, null, new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, false, true, null, byDate, s));
            channelDoctorCountsRows.add(row);
        }

    }
    
    public void fetchSpecilityWiseDoctorAppoinmentCount() {
        if(institution==null){
            fetchSpecilityWiseDoctorAppoinmentCountWithoutInstitution();
        }else{
            fetchSpecilityWiseDoctorAppoinmentCountWithInstitution();
        }
    }

    public void fetchSpecilityWiseDoctorAppoinmentCountWithInstitution() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        BillType[] bts = new BillType[]{BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> billTypes = Arrays.asList(bts);
        for (Speciality sp : fetchBillFeesSpecilityAndInstitution(billTypes)) {
            ChannelSummeryDateRangeBillTotalTable sws = new ChannelSummeryDateRangeBillTotalTable();
            sws.setSpeciality(sp);
            sws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(institution, sp, billTypes));
            channelSummeryDateRangeBillTotalTables.add(sws);
        }
    }
    
    public void fetchSpecilityWiseDoctorAppoinmentCountWithoutInstitution() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        BillType[] bts = new BillType[]{BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> billTypes = Arrays.asList(bts);
        for (Speciality sp : fetchBillFeesSpecility(billTypes)) {
            ChannelSummeryDateRangeBillTotalTable sws = new ChannelSummeryDateRangeBillTotalTable();
            sws.setSpeciality(sp);
            sws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(sp, billTypes));
            channelSummeryDateRangeBillTotalTables.add(sws);
        }
    }
    

    public void fetchStaffWiseChannelBillTypeCount() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        double[] d = new double[4];
        BillType[] bts = new BillType[]{BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> billTypes = Arrays.asList(bts);
        for (Staff s : fetchBillsStaffs(null, billTypes)) {
            ChannelSummeryDateRangeBillTotalTable sws = new ChannelSummeryDateRangeBillTotalTable();
            sws.setStaff(s);

            sws.setCount(fetchChannelBillTypeBilCounts(s, billTypes));
            for (int i = 0; i < 4; i++) {
                d[i] += sws.count[i];
            }
            channelSummeryDateRangeBillTotalTables.add(sws);
        }

        ChannelSummeryDateRangeBillTotalTable sws = new ChannelSummeryDateRangeBillTotalTable();
        sws.setStaff(null);
        sws.setCount(d);
        channelSummeryDateRangeBillTotalTables.add(sws);
    }

    public List<WebUser> fetchCashiers(BillType[] bts) {
        List<WebUser> cashiers = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        List<BillType> btys = Arrays.asList(bts);
        sql = "select us from "
                + " Bill b "
                + " join b.creater us "
                + " where b.retired=false "
                + " and b.institution=:ins "
                + " and b.billType in :btp "
                + " and b.createdAt between :fromDate and :toDate "
                + " group by us "
                + " having sum(b.netTotal)!=0 ";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("btp", btys);
        m.put("ins", sessionController.getInstitution());
        cashiers = getWebUserFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    public List<WebUser> fetchCashiersSession(BillType[] bts) {
        List<WebUser> cashiers = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        List<BillType> btys = Arrays.asList(bts);
        sql = "select us from "
                + " Bill b "
                + " join b.creater us "
                + " where b.retired=false "
                + " and b.institution=:ins "
                + " and b.billType in :btp "
                + " and b.singleBillSession.sessionDate between :fromDate and :toDate "
                + " group by us "
                + " having sum(b.netTotal)!=0 ";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("btp", btys);
        m.put("ins", sessionController.getInstitution());
        cashiers = getWebUserFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    public List<ChannelSummeryUserRow> fetchUserRows(Date fDate, Date tDate, BillType[] bts) {
        List<ChannelSummeryUserRow> userRows = new ArrayList<>();
        double tbc = 0.0;
        double tcc = 0.0;
        double trc = 0.0;
        double tht = 0.0;
        double tst = 0.0;
        for (WebUser webUser : fetchCashiers(bts)) {
            ChannelSummeryUserRow row = new ChannelSummeryUserRow();
            row.setUser(webUser);
            row.setBillCount(fetchBillsTotal(bts, null, null, null, new BilledBill(), fDate, tDate, null, null, false, true, null, null, webUser));
            row.setCanceledCount(fetchBillsTotal(bts, null, null, null, new CancelledBill(), fDate, tDate, null, null, false, true, null, null, webUser));
            row.setRefundCount(fetchBillsTotal(bts, null, null, null, new RefundBill(), fDate, tDate, null, null, false, true, null, null, webUser));
            double netTotal = fetchBillsTotal(bts, null, null, null, new BilledBill(), fDate, tDate, null, null, false, false, null, null, webUser)
                    + (fetchBillsTotal(bts, null, null, null, new CancelledBill(), fDate, tDate, null, null, false, false, null, null, webUser)
                    + fetchBillsTotal(bts, null, null, null, new RefundBill(), fDate, tDate, null, null, false, false, null, null, webUser));
            double hosTotal = fetchBillsTotal(bts, null, null, null, new BilledBill(), fDate, tDate, null, null, true, false, null, null, webUser)
                    + (fetchBillsTotal(bts, null, null, null, new CancelledBill(), fDate, tDate, null, null, true, false, null, null, webUser)
                    + fetchBillsTotal(bts, null, null, null, new RefundBill(), fDate, tDate, null, null, true, false, null, null, webUser));
            row.setTotalHosFee(hosTotal);
            row.setTotalDocFee(netTotal - hosTotal);
            row.setBold(false);
            if (row.getBillCount() != 0.0 || row.getCanceledCount() != 0.0 || row.getRefundCount() != 0.0) {
                userRows.add(row);
            }

            tbc += row.getBillCount();
            tcc += row.getCanceledCount();
            trc += row.getRefundCount();
            tht += row.getTotalHosFee();
            tst += row.getTotalDocFee();
        }

        ChannelSummeryUserRow row = new ChannelSummeryUserRow();
        row.setBillCount(tbc);
        row.setCanceledCount(tcc);
        row.setRefundCount(trc);
        row.setTotalHosFee(tht);
        row.setTotalDocFee(tst);
        row.setBold(true);
        userRows.add(row);

        channelTotal.setTotalBillCount(channelTotal.getTotalBillCount() + tbc);
        channelTotal.setTotalCanceledCount(channelTotal.getTotalCanceledCount() + tcc);
        channelTotal.setTotalRefundCount(channelTotal.getTotalRefundCount() + trc);
        channelTotal.setTotalDocFee(channelTotal.getTotalDocFee() + tst);
        channelTotal.setTotalHosFee(channelTotal.getTotalHosFee() + tht);

        return userRows;
    }

    public List<ChannelSummeryDateRangeRow> fetchDateRangeRows(Date fDate, Date tDate, WebUser webUser, BillType[] bts) {
        List<ChannelSummeryDateRangeRow> dateRangeRows = new ArrayList<>();
        Date nowDate = fDate;
        double tbc = 0.0;
        double tcc = 0.0;
        double trc = 0.0;
        double tht = 0.0;
        double tst = 0.0;
        while (nowDate.before(tDate)) {
            ChannelSummeryDateRangeRow row = new ChannelSummeryDateRangeRow();
            String formatedDate;
            Date fd;
            Date td;
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);
            row.setDate(formatedDate);
            row.setBillCount(fetchBillsTotal(bts, null, null, null, new BilledBill(), fd, td, null, null, false, true, null, null, webUser));
            row.setCanceledCount(fetchBillsTotal(bts, null, null, null, new CancelledBill(), fd, td, null, null, false, true, null, null, webUser));
            row.setRefundCount(fetchBillsTotal(bts, null, null, null, new RefundBill(), fd, td, null, null, false, true, null, null, webUser));
            double netTotal = fetchBillsTotal(bts, null, null, null, new BilledBill(), fd, td, null, null, false, false, null, null, webUser)
                    + (fetchBillsTotal(bts, null, null, null, new CancelledBill(), fd, td, null, null, false, false, null, null, webUser)
                    + fetchBillsTotal(bts, null, null, null, new RefundBill(), fd, td, null, null, false, false, null, null, webUser));
            double hosTotal = fetchBillsTotal(bts, null, null, null, new BilledBill(), fd, td, null, null, true, false, null, null, webUser)
                    + (fetchBillsTotal(bts, null, null, null, new CancelledBill(), fd, td, null, null, true, false, null, null, webUser)
                    + fetchBillsTotal(bts, null, null, null, new RefundBill(), fd, td, null, null, true, false, null, null, webUser));
            row.setTotalHosFee(hosTotal);
            row.setTotalDocFee(netTotal - hosTotal);
            row.setBold(false);

            if (row.getBillCount() != 0.0 || row.getCanceledCount() != 0.0 || row.getRefundCount() != 0.0) {
                dateRangeRows.add(row);
            }

            tbc += row.getBillCount();
            tcc += row.getCanceledCount();
            trc += row.getRefundCount();
            tht += row.getTotalHosFee();
            tst += row.getTotalDocFee();

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            cal.add(Calendar.DATE, 1);
            nowDate = cal.getTime();
        }
        ChannelSummeryDateRangeRow row = new ChannelSummeryDateRangeRow();
//        row.setDate("Total");
        row.setBillCount(tbc);
        row.setCanceledCount(tcc);
        row.setRefundCount(trc);
        row.setTotalHosFee(tht);
        row.setTotalDocFee(tst);
        row.setBold(true);
        dateRangeRows.add(row);

        channelTotal.setTotalBillCount(channelTotal.getTotalBillCount() + tbc);
        channelTotal.setTotalCanceledCount(channelTotal.getTotalCanceledCount() + tcc);
        channelTotal.setTotalRefundCount(channelTotal.getTotalRefundCount() + trc);
        channelTotal.setTotalDocFee(channelTotal.getTotalDocFee() + tst);
        channelTotal.setTotalHosFee(channelTotal.getTotalHosFee() + tht);

        return dateRangeRows;
    }

    public List<ChannelSummeryDateRangeRow> fetchDateRangeRowsSession(Date fDate, Date tDate, WebUser webUser, BillType[] bts) {
        List<ChannelSummeryDateRangeRow> dateRangeRows = new ArrayList<>();
        Date nowDate = fDate;
        double tbc = 0.0;
        double tcc = 0.0;
        double trc = 0.0;
        double tht = 0.0;
        double tst = 0.0;
        while (nowDate.before(tDate)) {
            ChannelSummeryDateRangeRow row = new ChannelSummeryDateRangeRow();
            String formatedDate;
            Date fd;
            Date td;
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);
            row.setDate(formatedDate);
            row.setBillCount(fetchBillsTotalSessoin(bts, null, null, null, new BilledBill(), fd, td, null, null, false, true, null, null, webUser));
            row.setCanceledCount(fetchBillsTotalSessoin(bts, null, null, null, new CancelledBill(), fd, td, null, null, false, true, null, null, webUser));
            row.setRefundCount(fetchBillsTotalSessoin(bts, null, null, null, new RefundBill(), fd, td, null, null, false, true, null, null, webUser));
            double netTotal = fetchBillsTotalSessoin(bts, null, null, null, null, fd, td, null, null, false, false, null, null, webUser);
            double hosTotal = fetchBillsTotalSessoin(bts, null, null, null, null, fd, td, null, null, true, false, null, null, webUser);
            row.setTotalHosFee(hosTotal);
            row.setTotalDocFee(netTotal - hosTotal);
            row.setBold(false);

            if (row.getBillCount() != 0.0 || row.getCanceledCount() != 0.0 || row.getRefundCount() != 0.0) {
                dateRangeRows.add(row);
            }

            tbc += row.getBillCount();
            tcc += row.getCanceledCount();
            trc += row.getRefundCount();
            tht += row.getTotalHosFee();
            tst += row.getTotalDocFee();

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            cal.add(Calendar.DATE, 1);
            nowDate = cal.getTime();
        }
        ChannelSummeryDateRangeRow row = new ChannelSummeryDateRangeRow();
//        row.setDate("Total");
        row.setBillCount(tbc);
        row.setCanceledCount(tcc);
        row.setRefundCount(trc);
        row.setTotalHosFee(tht);
        row.setTotalDocFee(tst);
        row.setBold(true);
        dateRangeRows.add(row);

        channelTotal.setTotalBillCount(channelTotal.getTotalBillCount() + tbc);
        channelTotal.setTotalCanceledCount(channelTotal.getTotalCanceledCount() + tcc);
        channelTotal.setTotalRefundCount(channelTotal.getTotalRefundCount() + trc);
        channelTotal.setTotalDocFee(channelTotal.getTotalDocFee() + tst);
        channelTotal.setTotalHosFee(channelTotal.getTotalHosFee() + tht);

        return dateRangeRows;
    }

    private Date fetchTodaybill(List<BillType> billTypes, WebUser wUser, boolean fristBill) {
        String sql;
        Map temMap = new HashMap();

        sql = " SELECT ";

        if (fristBill) {
            sql += " min(b.singleBillSession.sessionDate) ";
        } else {
            sql += " max(b.singleBillSession.sessionDate) ";
        }

        sql += " FROM Bill b WHERE b.retired=false "
                + " and b.billType in :btps "
                + " and b.institution=:ins "
                + " and b.createdAt between :fdc and :tdc ";

        if (wUser != null) {
            sql += " and b.creater=:w ";
            temMap.put("w", wUser);
        }

        temMap.put("fdc", commonFunctions.getStartOfDay(getFromDate()));
        temMap.put("tdc", commonFunctions.getEndOfDay(getFromDate()));
        temMap.put("btps", billTypes);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findDateByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createAgentWiseAppoinmentCount() {
        Date startTime = new Date();
        fetchAgentWiseChannelTotal();

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Income report/Agent Reports/Agent vise channel appoinment  total and count(/faces/channel/channel_report_agent_wise_channel_total.xhtml)");
    }

    public void createAgentWiseAppoinmentTotal() {
        fetchAgentWiseChannelTotal();
    }

    public void createAgentWiseAppoinmentCountSummery() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        List<Institution> institutions = new ArrayList<>();
        institutions.addAll(fetchBillsAgencys());
        channelTotal = new ChannelTotal();
        for (Institution a : institutions) {
            ChannelSummeryDateRangeBillTotalTable aws = new ChannelSummeryDateRangeBillTotalTable();
            aws.setAgency(a);
            aws.setNetCount(fetchBillsTotal(null, BillType.ChannelAgent, null, null, new BilledBill(), getFromDate(), getToDate(), null, a, false, true, null, null, null)
                    - (fetchBillsTotal(null, BillType.ChannelAgent, null, null, new CancelledBill(), getFromDate(), getToDate(), null, a, false, true, null, null, null)
                    + fetchBillsTotal(null, BillType.ChannelAgent, null, null, new RefundBill(), getFromDate(), getToDate(), null, a, false, true, null, null, null)));
            if (aws.getNetCount() > 0) {
                channelSummeryDateRangeBillTotalTables.add(aws);
                channelTotal.setNetTotal(channelTotal.getNetTotal() + aws.getNetCount());
            }

        }
    }

    public void createStaffWiseAppoinmentCount() {
        fetchStaffWiseChannelTotalOrCount();
    }

    public void createStaffWiseAppoinmentCountNew() {
        columnModels = new ArrayList<>();
        fetchChannelHeaders();
        fetchStaffWiseChannelCount();
        ChannelDoctorCountsRow row = new ChannelDoctorCountsRow();
        int i = channelDoctorCountsRows.size();
        int j = headers.size();
//        row.setCategoryName("Total");
        List<Long> list = new ArrayList<>();
        for (int k = 0; k < j; k++) {
            double total = 0.0;
            for (int l = 0; l < i; l++) {
                total += channelDoctorCountsRows.get(l).getCounts().get(k);
            }
            list.add((long) total);
        }
        row.setCounts(list);
        channelDoctorCountsRows.add(row);
        Long l = 0l;
        for (String h : headers) {
            ColumnModel c = new ColumnModel();
            c.setHeader(h);
            c.setProperty(l.toString());
            columnModels.add(c);
            l++;
        }
    }

    public void createSpecilityWiseAppoinmentCountNew() {
        columnModels = new ArrayList<>();
        fetchChannelHeaders();
        fetchSpecilityWiseChannelCount();
        ChannelDoctorCountsRow row = new ChannelDoctorCountsRow();
        int i = channelDoctorCountsRows.size();
        int j = headers.size();
//        row.setCategoryName("Total");
        List<Long> list = new ArrayList<>();
        for (int k = 0; k < j; k++) {
            double total = 0.0;
            for (int l = 0; l < i; l++) {
                total += channelDoctorCountsRows.get(l).getCounts().get(k);
            }
            list.add((long) total);
        }
        row.setCounts(list);
        channelDoctorCountsRows.add(row);
        Long l = 0l;
        for (String h : headers) {
            ColumnModel c = new ColumnModel();
            c.setHeader(h);
            c.setProperty(l.toString());
            columnModels.add(c);
            l++;
        }
    }

    public void createStaffWiseAppoinmentTotal() {
        Date startTime = new Date();
        fetchStaffWiseChannelTotalOrCount();

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Consultant report/Agent vise channel appoinment total and count(/faces/channel/channel_report_consultant_month_count.xhtml)");
    }

    public void createSpecilityWiseAppoinmentCount() {
        fetchSpecilityWiseChannelTotalOrCount();
    }

    public List<ChannelReportSpecialityWiseSummeryRow> createChannelReportDoctorWise() {
        columns = new ArrayList<>();
        channelReportSpecialityWiseSummeryRows = new ArrayList<>();
        ChannelReportSpecialityWiseSummeryRow row = new ChannelReportSpecialityWiseSummeryRow();;
        ColumnModel c = new ColumnModel();
        List<Staff> doctors = fetchBillsStaffs(null, Arrays.asList(new BillType[]{BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelPaid}));

        for (Staff s : doctors) {

            row = new ChannelReportSpecialityWiseSummeryRow();
            //ChannelReportSpecialityWiseSummeryRow row = new ChannelReportSpecialityWiseSummeryRow();
            row.setDoctor(s);
            row.setCounts(countsBetweenDates(s, fromDate, toDate));

            channelReportSpecialityWiseSummeryRows.add(row);

        }

        Long l = 0l;
        for (String d : datesBetween(fromDate, toDate)) {
            c = new ColumnModel();
            c.setHeader(d.toUpperCase());
            c.setProperty(l.toString());
//           c.setProperty(d);
            columns.add(c);
            l++;
        }
        c = new ColumnModel();
        c.setHeader("Total");
        c.setProperty(l.toString());
        columns.add(c);
//        //// // System.out.println("channelReportSpecialityWiseSummeryRows.indefOf() = " + channelReportSpecialityWiseSummeryRows.get(0).doctor.getPerson().getNameWithTitle());
//        //// // System.out.println("channelReportSpecialityWiseSummeryRows.indefOf() = " + channelReportSpecialityWiseSummeryRows.get(0).counts.get(0).longValue());


        return channelReportSpecialityWiseSummeryRows;
    }

    public List<String> datesBetween(Date fd, Date td) {
        ColumnModel c = new ColumnModel();
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd");

        String startDate = sm.format(fd);
        String endDate = sm.format(td);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(String.valueOf(date.getYear()) + " - " + String.valueOf(date.getMonthOfYear()) + " - " + String.valueOf(date.getDayOfMonth()));
        }

        return dates;

    }

    public List<Long> countsBetweenDates(Staff s, Date fd, Date td) {
        ChannelReportSpecialityWiseSummeryRow row = new ChannelReportSpecialityWiseSummeryRow();
        List<Long> countsList = new ArrayList<>();
        //  List<ChannelSummeryDateRangeBillTotalRow> acsrs = new ArrayList<>();
        Date nowDate = getFromDate();
        double btot = 0.0;
        double ctot = 0.0;
        double rtot = 0.0;
        double netTot = 0.0;
        while (nowDate.before(getToDate())) {
            String formatedDate;

//            if (byDate) {
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);
//            //// // System.out.println("td = " + td);
//            //// // System.out.println("fd = " + fd);
//            //// // System.out.println("nowDate = " + nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);

//           
            double ctot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new CancelledBill(), fd, td, null, null, false, true, s, null, null);
            double rtot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new RefundBill(), fd, td, null, null, false, true, s, null, null);
            double btot1 = fetchBillsTotal(new BillType[]{BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent}, null, null, null, new BilledBill(), fd, td, null, null, false, true, s, null, null);
            ctot += ctot1;
            rtot += rtot1;
            btot += btot1;
            netTot = btot1 - (ctot1 + rtot1);
            countsList.add((long) netTot);

            //acsrs.add(acsr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
//            if (byDate) {
            cal.add(Calendar.DATE, 1);
//            } else {
//                cal.add(Calendar.MONTH, 1);
//            }
            nowDate = cal.getTime();
        }
        //row.setSum((long) netTot);
        countsList.add((long) (btot - (ctot + rtot)));
        return countsList;
    }

    public List<Double> totalsBetweenDates(PaymentMethod pm, Date fd, Date td) {
        ChannelVatReportPaymentSchemeWiseRow row = new ChannelVatReportPaymentSchemeWiseRow();
        List<Double> totsList = new ArrayList<>();
        Date nowDate = getFromDate();
//        double btot = 0.0;
//        double ctot = 0.0;
//        double rtot = 0.0;
        double netTot = 0.0;
        double netTot1;
        while (nowDate.before(getToDate())) {
            String formatedDate;

//            if (byDate) {
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);
//            //// // System.out.println("td = " + td);
//            //// // System.out.println("fd = " + fd);
//            //// // System.out.println("nowDate = " + nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);

            netTot1 = (fetchBillsVatTotal(pm, fd, td)) * 45 / 100;
            totsList.add(netTot1);
            netTot += netTot1;
            //acsrs.add(acsr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
//            if (byDate) {
            cal.add(Calendar.DATE, 1);
//            } else {
//                cal.add(Calendar.MONTH, 1);
//            }
            nowDate = cal.getTime();
        }
        //row.setSum((long) netTot);
        totsList.add(netTot);
        return totsList;

    }

    public void createCanceledAndRefund() {
        grantTot = 0.0;
        bills = fetchCancelledOrRefundBills();
        for (Bill bill : bills) {
            grantTot += (bill.getVat() + bill.getNetTotal());
        }
    }

    public List<Bill> fetchCancelledOrRefundBills() {
        String sql = "";
        Map m = new HashMap();

        sql = "select b from Bill b where b.retired=false";
        if (dept.equals("1")) {
            sql += " and b.billType in :bt ";
            ArrayList<BillType> bts = new ArrayList<>(
                    Arrays.asList(BillType.ChannelCash, BillType.ChannelPaid, BillType.ChannelAgent));
            m.put("bt", bts);
        }
        if (dept.equals("2")) {
            sql += " and b.billType =:bt ";
            m.put("bt", BillType.OpdBill);
        }
        if (dept.equals("3")) {
            sql += " and b.billType in :bt ";
            ArrayList<BillType> bts = new ArrayList<>(
                    Arrays.asList(BillType.PharmacySale));
            m.put("bt", bts);
        }
        if (billtype.equals("1")) {
            sql += " and type(b)=:class ";
            m.put("class", CancelledBill.class);
        }
        if (billtype.equals("2")) {
            sql += " and type(b)=:class ";
            m.put("class", RefundBill.class);
        }
        sql += " and b.createdAt between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);

        return billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchBillsVatTotal(PaymentMethod pm, Date fd, Date td) {
        String sql = "";
        Map m = new HashMap();
        double total = 0.0;

        sql += " select sum(b.vat) from Bill b "
                + " where b.retired=false "
                + " and b.paymentMethod=:pm "
                + " and b.createdAt between :fromDate and :toDate  ";
        m.put("pm", pm);
        m.put("fromDate", fd);
        m.put("toDate", td);
        total = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        return total;
    }

    public List<PaymentMethod> fetchPaymentMethods() {

        paymentMethods = Arrays.asList(PaymentMethod.values());
        return paymentMethods;
    }

    public List<ChannelVatReportPaymentSchemeWiseRow> createChannelVatReportPaymentSchemeWiseRows() {
        channelVatReportPaymentSchemeWiseRows = new ArrayList<>();
        columns = new ArrayList<>();
        ChannelVatReportPaymentSchemeWiseRow row;
        ColumnModel c;
        for (PaymentMethod pm : fetchPaymentMethods()) {
            row = new ChannelVatReportPaymentSchemeWiseRow();
            row.setPm(pm);
            row.setTot(totalsBetweenDates(pm, toDate, toDate));
            channelVatReportPaymentSchemeWiseRows.add(row);
        }
        Long l = 0l;
        for (String d : datesBetween(fromDate, toDate)) {
            c = new ColumnModel();

            c.setHeader(d.toUpperCase());

            c.setProperty(l.toString());
//           c.setProperty(d);
            columns.add(c);
            l++;
        }
        c = new ColumnModel();
        c.setHeader("Total");
        c.setProperty(l.toString());
        columns.add(c);
        return channelVatReportPaymentSchemeWiseRows;
    }

    public void createSpecilityWiseAppoinmentTotal() {
        Date startTime = new Date();
        fetchSpecilityWiseChannelTotalOrCount();

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Consultant report/Speciality vise channel appoinment total and count(/faces/channel/channel_report_specility_month_count.xhtml)");
    }

    public void createSpecilityWiseDoctorAppoinmentCount() {
        Date startTime = new Date();
        fetchSpecilityWiseDoctorAppoinmentCount();
        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Consultant report/Channel and scan count(/faces/channel/channel_report_scan_channel_count.xhtml)");
    }

    public void createStaffShedules() {
        Date startTime = new Date();
        itemFees = getSheduleController().fetchStaffServiceSessions();

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Session report/Entered schedule report(/faces/channel/channel_report_channel_shedule.xhtml)");
    }

    public void createStaffShedulesSpecial() {
        channelSheduleSummeryRows = new ArrayList<>();
        if (getReportKeyWord().getStaff() == null) {
            for (Staff s : fetchStaffs()) {
                ChannelSheduleSummeryRow row = new ChannelSheduleSummeryRow();
                row.setStaff(s);
                row.setServiceSessions(fetchServiceSessions(s));
                channelSheduleSummeryRows.add(row);
            }
        } else {
            ChannelSheduleSummeryRow row = new ChannelSheduleSummeryRow();
            row.setStaff(getReportKeyWord().getStaff());
            row.setServiceSessions(fetchServiceSessions(getReportKeyWord().getStaff()));
            channelSheduleSummeryRows.add(row);
        }
    }

    public void createAgencyBalanceTable() {
        Date startTime = new Date();

        String sql;
        HashMap m = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType=:typ ";

        m.put("typ", InstitutionType.Agency);
        if (getReportKeyWord()!=null) {
            if (getReportKeyWord().isAdditionalDetails()) {
                sql+= " and c.inactive!=true ";   
            }
        }
        agencies = getInstitutionFacade().findByJpql(sql, m);
        getChannelTotal().setNetTotal(0);
        for (Institution a : agencies) {
            getChannelTotal().setNetTotal(channelTotal.getNetTotal() + a.getBallance());
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Income report/Agent Reports/Agent balance(/faces/channel/channel_report_agent_balance.xhtml)");
    }

    public void createCollectingcenterBalanceTable() {
        Date startTime = new Date();

        String sql;
        HashMap m = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType=:typ ";

        m.put("typ", InstitutionType.CollectingCentre);

        agencies = getInstitutionFacade().findByJpql(sql, m);

        commonController.printReportDetails(fromDate, toDate, startTime, "Payments/Book issuing/Collecting center booki issuing/Collecting center current balance(/faces/reportLab/report_collecting_center_balance.xhtml)");
    }

    public void createStaffWiseChannelBillTypeCountTable() {
        Date startTime = new Date();
        fetchStaffWiseChannelBillTypeCount();

        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Reports/Consultant report/Doctor vise payment count(/faces/channel/channel_report_staff_wise_patient_count.xhtml)");
    }

    public void clearAllReportData() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
    }

    //listners
    public void listnerDoc() {
        if (withDocPayment) {
            withOutDocPayment = false;
        }
    }

    public void listnerHos() {
        if (withOutDocPayment) {
            withDocPayment = false;
        }
    }

    public List<Staff> fetchStaffs() {
        String sql;
        HashMap m = new HashMap();
        List<Staff> list = new ArrayList<>();

        sql = "Select distinct(s.staff) From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.originatingSession is null "
                + " order by s.staff.speciality.name,s.staff.person.name ";

        m.put("class", ServiceSession.class);

        list = getStaffFacade().findByJpql(sql, m);

        return list;
    }

    public List<ServiceSession> fetchServiceSessions(Staff s) {
        String sql;
        HashMap m = new HashMap();
        List<ServiceSession> list = new ArrayList<>();

        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.staff=:s "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime ";

        m.put("s", s);
        m.put("class", ServiceSession.class);

        list = getServiceSessionFacade().findByJpql(sql, m);

        return list;
    }

    public List<WebUser> getCashiers() {
        String sql;
        Map temMap = new HashMap();
        List<WebUser> webUsers = new ArrayList<>();
        List<BillType> btpList = Arrays.asList(new BillType[]{BillType.ChannelCash, BillType.ChannelAgent, BillType.ChannelPaid});
        sql = "select us from "
                + " Bill b "
                + " join b.creater us "
                + " where b.retired=false "
                + " and b.institution=:ins "
                + " and b.billType in :btp "
                + " and b.createdAt between :fromDate and :toDate "
                + " group by us "
                + " having sum(b.netTotal)!=0 ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", btpList);
        temMap.put("ins", sessionController.getInstitution());
        webUsers = getWebUserFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return webUsers;
    }

    public ChannelReportController getChannelReportController() {
        return channelReportController;
    }

    public void setChannelReportController(ChannelReportController channelReportController) {
        this.channelReportController = channelReportController;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<ChannelReportSpecialityWiseSummeryRow> getChannelReportSpecialityWiseSummeryRows() {
        return channelReportSpecialityWiseSummeryRows;
    }

    public void setChannelReportSpecialityWiseSummeryRows(List<ChannelReportSpecialityWiseSummeryRow> channelReportSpecialityWiseSummeryRows) {
        this.channelReportSpecialityWiseSummeryRows = channelReportSpecialityWiseSummeryRows;
    }

    public List<ColumnModel> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnModel> columns) {
        this.columns = columns;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public List<Long> getCountsList() {
        return countsList;
    }

    public void setCountsList(List<Long> countsList) {
        this.countsList = countsList;
    }

    public List<ChannelVatReportPaymentSchemeWiseRow> getChannelVatReportPaymentSchemeWiseRows() {
        return channelVatReportPaymentSchemeWiseRows;
    }

    public void setChannelVatReportPaymentSchemeWiseRows(List<ChannelVatReportPaymentSchemeWiseRow> channelVatReportPaymentSchemeWiseRows) {
        this.channelVatReportPaymentSchemeWiseRows = channelVatReportPaymentSchemeWiseRows;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getBilltype() {
        return billtype;
    }

    public void setBilltype(String billtype) {
        this.billtype = billtype;
    }

    public double getGrantTot() {
        return grantTot;
    }

    public void setGrantTot(double grantTot) {
        this.grantTot = grantTot;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    //inner Classes(Data Structures)
    public class ChannelSummeryDateRangeBillTotalTable {

        Institution Agency;
        Staff staff;
        Speciality speciality;
        double[] count;
        String[] dates;//for 2d 
        double netCount;

        List<ChannelSummeryDateRangeBillTotalRow> channelSummeryDateRangeBillTotalRows;

        public Institution getAgency() {
            return Agency;
        }

        public void setAgency(Institution Agency) {
            this.Agency = Agency;
        }

        public List<ChannelSummeryDateRangeBillTotalRow> getChannelSummeryDateRangeBillTotalRows() {
            return channelSummeryDateRangeBillTotalRows;
        }

        public void setChannelSummeryDateRangeBillTotalRows(List<ChannelSummeryDateRangeBillTotalRow> channelSummeryDateRangeBillTotalRows) {
            this.channelSummeryDateRangeBillTotalRows = channelSummeryDateRangeBillTotalRows;
        }

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public Speciality getSpeciality() {
            return speciality;
        }

        public void setSpeciality(Speciality speciality) {
            this.speciality = speciality;
        }

        public double[] getCount() {
            return count;
        }

        public void setCount(double[] count) {
            this.count = count;
        }

        public double getNetCount() {
            return netCount;
        }

        public void setNetCount(double netCount) {
            this.netCount = netCount;
        }

        public String[] getDates() {
            return dates;
        }

        public void setDates(String[] dates) {
            this.dates = dates;
        }

    }

    public class ChannelSummeryDateRangeBillTotalRow {

        String date;
        double billTotal;
        double canceledTotal;
        double refundTotal;
        boolean bold;

        Staff staff;
        double totalHosFee;
        double totalDocFee;

        public ChannelSummeryDateRangeBillTotalRow() {
        }

        public double getCanceledTotal() {
            return canceledTotal;
        }

        public void setCanceledTotal(double canceledTotal) {
            this.canceledTotal = canceledTotal;
        }

        public double getRefundTotal() {
            return refundTotal;
        }

        public void setRefundTotal(double refundTotal) {
            this.refundTotal = refundTotal;
        }

        public boolean isBold() {
            return bold;
        }

        public void setBold(boolean bold) {
            this.bold = bold;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public double getBillTotal() {
            return billTotal;
        }

        public void setBillTotal(double billTotal) {
            this.billTotal = billTotal;
        }

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public double getTotalHosFee() {
            return totalHosFee;
        }

        public void setTotalHosFee(double totalHosFee) {
            this.totalHosFee = totalHosFee;
        }

        public double getTotalDocFee() {
            return totalDocFee;
        }

        public void setTotalDocFee(double totalDocFee) {
            this.totalDocFee = totalDocFee;
        }
    }

    public class ChannelDateDetailRow {

        String date;
        double cash;
        double card;
        double slip;
        double cheque;
        double agent;
        double staff;
        double onCall;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public double getCash() {
            return cash;
        }

        public void setCash(double cash) {
            this.cash = cash;
        }

        public double getCard() {
            return card;
        }

        public void setCard(double card) {
            this.card = card;
        }

        public double getSlip() {
            return slip;
        }

        public void setSlip(double slip) {
            this.slip = slip;
        }

        public double getCheque() {
            return cheque;
        }

        public void setCheque(double cheque) {
            this.cheque = cheque;
        }

        public double getAgent() {
            return agent;
        }

        public void setAgent(double agent) {
            this.agent = agent;
        }

        public double getStaff() {
            return staff;
        }

        public void setStaff(double staff) {
            this.staff = staff;
        }

        public double getOnCall() {
            return onCall;
        }

        public void setOnCall(double onCall) {
            this.onCall = onCall;
        }

    }

    public class ChannelTotal {

        double cashTotal;
        double cardTotal;
        double slipTotal;
        double chequeTotal;
        double agentTotal;
        double staffTotal;
        double onCallTotal;

        double totalBillCount;
        double totalCanceledCount;
        double totalRefundCount;
        double totalHosFee;
        double totalDocFee;

        double netTotal;

        public double getCashTotal() {
            return cashTotal;
        }

        public void setCashTotal(double cashTotal) {
            this.cashTotal = cashTotal;
        }

        public double getCardTotal() {
            return cardTotal;
        }

        public void setCardTotal(double cardTotal) {
            this.cardTotal = cardTotal;
        }

        public double getSlipTotal() {
            return slipTotal;
        }

        public void setSlipTotal(double slipTotal) {
            this.slipTotal = slipTotal;
        }

        public double getChequeTotal() {
            return chequeTotal;
        }

        public void setChequeTotal(double chequeTotal) {
            this.chequeTotal = chequeTotal;
        }

        public double getAgentTotal() {
            return agentTotal;
        }

        public void setAgentTotal(double agentTotal) {
            this.agentTotal = agentTotal;
        }

        public double getStaffTotal() {
            return staffTotal;
        }

        public void setStaffTotal(double staffTotal) {
            this.staffTotal = staffTotal;
        }

        public double getOnCallTotal() {
            return onCallTotal;
        }

        public void setOnCallTotal(double onCallTotal) {
            this.onCallTotal = onCallTotal;
        }

        public double getTotalBillCount() {
            return totalBillCount;
        }

        public void setTotalBillCount(double totalBillCount) {
            this.totalBillCount = totalBillCount;
        }

        public double getTotalCanceledCount() {
            return totalCanceledCount;
        }

        public void setTotalCanceledCount(double totalCanceledCount) {
            this.totalCanceledCount = totalCanceledCount;
        }

        public double getTotalRefundCount() {
            return totalRefundCount;
        }

        public void setTotalRefundCount(double totalRefundCount) {
            this.totalRefundCount = totalRefundCount;
        }

        public double getTotalHosFee() {
            return totalHosFee;
        }

        public void setTotalHosFee(double totalHosFee) {
            this.totalHosFee = totalHosFee;
        }

        public double getTotalDocFee() {
            return totalDocFee;
        }

        public void setTotalDocFee(double totalDocFee) {
            this.totalDocFee = totalDocFee;
        }

        public double getNetTotal() {
            return netTotal;
        }

        public void setNetTotal(double netTotal) {
            this.netTotal = netTotal;
        }

    }

    public class ChannelSummeryDateRangeOrUserRow {

        String date;
        WebUser user;
        List<ChannelSummeryUserRow> userRows;
        List<ChannelSummeryDateRangeRow> dateRangeRows;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public WebUser getUser() {
            return user;
        }

        public void setUser(WebUser user) {
            this.user = user;
        }

        public List<ChannelSummeryUserRow> getUserRows() {
            return userRows;
        }

        public void setUserRows(List<ChannelSummeryUserRow> userRows) {
            this.userRows = userRows;
        }

        public List<ChannelSummeryDateRangeRow> getDateRangeRows() {
            return dateRangeRows;
        }

        public void setDateRangeRows(List<ChannelSummeryDateRangeRow> dateRangeRows) {
            this.dateRangeRows = dateRangeRows;
        }

    }

    public class ChannelSummeryUserRow {

        WebUser user;
        double billCount;
        double canceledCount;
        double refundCount;
        boolean bold;
        double totalHosFee;
        double totalDocFee;

        public double getBillCount() {
            return billCount;
        }

        public void setBillCount(double billCount) {
            this.billCount = billCount;
        }

        public double getCanceledCount() {
            return canceledCount;
        }

        public void setCanceledCount(double canceledCount) {
            this.canceledCount = canceledCount;
        }

        public double getRefundCount() {
            return refundCount;
        }

        public void setRefundCount(double refundCount) {
            this.refundCount = refundCount;
        }

        public boolean isBold() {
            return bold;
        }

        public void setBold(boolean bold) {
            this.bold = bold;
        }

        public double getTotalHosFee() {
            return totalHosFee;
        }

        public void setTotalHosFee(double totalHosFee) {
            this.totalHosFee = totalHosFee;
        }

        public double getTotalDocFee() {
            return totalDocFee;
        }

        public void setTotalDocFee(double totalDocFee) {
            this.totalDocFee = totalDocFee;
        }

        public WebUser getUser() {
            return user;
        }

        public void setUser(WebUser user) {
            this.user = user;
        }

    }

    public class ChannelSummeryDateRangeRow {

        String date;
        double billCount;
        double canceledCount;
        double refundCount;
        boolean bold;
        double totalHosFee;
        double totalDocFee;

        public double getBillCount() {
            return billCount;
        }

        public void setBillCount(double billCount) {
            this.billCount = billCount;
        }

        public double getCanceledCount() {
            return canceledCount;
        }

        public void setCanceledCount(double canceledCount) {
            this.canceledCount = canceledCount;
        }

        public double getRefundCount() {
            return refundCount;
        }

        public void setRefundCount(double refundCount) {
            this.refundCount = refundCount;
        }

        public boolean isBold() {
            return bold;
        }

        public void setBold(boolean bold) {
            this.bold = bold;
        }

        public double getTotalHosFee() {
            return totalHosFee;
        }

        public void setTotalHosFee(double totalHosFee) {
            this.totalHosFee = totalHosFee;
        }

        public double getTotalDocFee() {
            return totalDocFee;
        }

        public void setTotalDocFee(double totalDocFee) {
            this.totalDocFee = totalDocFee;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

    }

    public class ChannelReferenceBookRow {

        int bn;
        String refRange;
        Institution ins;
        List<AgentHistory> agentHistorys;
        double agentHistoryTotal;

        public int getBn() {
            return bn;
        }

        public void setBn(int bn) {
            this.bn = bn;
        }

        public Institution getIns() {
            return ins;
        }

        public void setIns(Institution ins) {
            this.ins = ins;
        }

        public List<AgentHistory> getAgentHistorys() {
            return agentHistorys;
        }

        public void setAgentHistorys(List<AgentHistory> agentHistorys) {
            this.agentHistorys = agentHistorys;
        }

        public String getRefRange() {
            return refRange;
        }

        public void setRefRange(String refRange) {
            this.refRange = refRange;
        }

        public double getAgentHistoryTotal() {
            return agentHistoryTotal;
        }

        public void setAgentHistoryTotal(double agentHistoryTotal) {
            this.agentHistoryTotal = agentHistoryTotal;
        }
    }

    public class ChannelSheduleSummeryRow {

        Staff staff;
        List<ServiceSession> serviceSessions;

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public List<ServiceSession> getServiceSessions() {
            return serviceSessions;
        }

        public void setServiceSessions(List<ServiceSession> serviceSessions) {
            this.serviceSessions = serviceSessions;
        }

    }

    public class ChannelDoctorCountsRow {

        Staff staff;
        Speciality speciality;
        List<Long> counts;

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public List<Long> getCounts() {
            return counts;
        }

        public void setCounts(List<Long> counts) {
            this.counts = counts;
        }

        public Speciality getSpeciality() {
            return speciality;
        }

        public void setSpeciality(Speciality speciality) {
            this.speciality = speciality;
        }
    }

    public class ChannelReportSpecialityWiseSummeryRow {

        private Staff doctor;
        private List<Long> counts;
        private long sum = 0l;

        public Staff getDoctor() {
            return doctor;
        }

        public void setDoctor(Staff doctor) {
            this.doctor = doctor;
        }

        public List<Long> getCounts() {
            return counts;
        }

        public void setCounts(List<Long> counts) {
            this.counts = counts;
        }

        public long getSum() {

            return sum;
        }

        public void setSum(long sum) {
            this.sum = sum;
        }

    }

    public class ChannelVatReportPaymentSchemeWiseRow {

        private PaymentMethod pm;
        private List<Double> tot;

        public PaymentMethod getPm() {
            return pm;
        }

        public void setPm(PaymentMethod pm) {
            this.pm = pm;
        }

        public List<Double> getTot() {
            return tot;
        }

        public void setTot(List<Double> tot) {
            this.tot = tot;
        }

    }

    public class ColumnModel {

        private String header;
        private String property;

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    public class ChannelUserSummeryRow {

        WebUser user;
        List<ChannelSummeryDateRangeRow> dateDetailRows;

        public WebUser getUser() {
            return user;
        }

        public void setUser(WebUser user) {
            this.user = user;
        }

        public List<ChannelSummeryDateRangeRow> getDateDetailRows() {
            return dateDetailRows;
        }

        public void setDateDetailRows(List<ChannelSummeryDateRangeRow> dateDetailRows) {
            this.dateDetailRows = dateDetailRows;
        }

    }

    //Getters and Setters
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StaffController getStaffController() {
        return staffController;
    }

    public void setStaffController(StaffController staffController) {
        this.staffController = staffController;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public AgentReferenceBookFacade getAgentReferenceBookFacade() {
        return agentReferenceBookFacade;
    }

    public void setAgentReferenceBookFacade(AgentReferenceBookFacade agentReferenceBookFacade) {
        this.agentReferenceBookFacade = agentReferenceBookFacade;
    }

    public List<AgentReferenceBook> getAgentReferenceBooks() {
        return agentReferenceBooks;
    }

    public void setAgentReferenceBooks(List<AgentReferenceBook> agentReferenceBooks) {
        this.agentReferenceBooks = agentReferenceBooks;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public ServiceSessionLeaveFacade getServiceSessionLeaveFacade() {
        return serviceSessionLeaveFacade;
    }

    public void setServiceSessionLeaveFacade(ServiceSessionLeaveFacade serviceSessionLeaveFacade) {
        this.serviceSessionLeaveFacade = serviceSessionLeaveFacade;
    }

    public List<ServiceSessionLeave> getServiceSessionLeaves() {
        return serviceSessionLeaves;
    }

    public void setServiceSessionLeaves(List<ServiceSessionLeave> serviceSessionLeaves) {
        this.serviceSessionLeaves = serviceSessionLeaves;
    }

    public List<ChannelSummeryDateRangeBillTotalTable> getChannelSummeryDateRangeBillTotalTables() {
        return channelSummeryDateRangeBillTotalTables;
    }

    public void setChannelSummeryDateRangeBillTotalTables(List<ChannelSummeryDateRangeBillTotalTable> channelSummeryDateRangeBillTotalTables) {
        this.channelSummeryDateRangeBillTotalTables = channelSummeryDateRangeBillTotalTables;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public boolean isCount() {
        return count;
    }

    public void setCount(boolean count) {
        this.count = count;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public boolean isBilledAgencys() {
        return billedAgencys;
    }

    public void setBilledAgencys(boolean billedAgencys) {
        this.billedAgencys = billedAgencys;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public boolean isWithOutDocPayment() {
        return withOutDocPayment;
    }

    public void setWithOutDocPayment(boolean withOutDocPayment) {
        this.withOutDocPayment = withOutDocPayment;
    }

    public boolean isByDate() {
        return byDate;
    }

    public void setByDate(boolean byDate) {
        this.byDate = byDate;
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    public boolean isSessoinDate() {
        return sessoinDate;
    }

    public void setSessoinDate(boolean sessoinDate) {
        this.sessoinDate = sessoinDate;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isScan() {
        return scan;
    }

    public void setScan(boolean scan) {
        this.scan = scan;
    }

    public SheduleController getSheduleController() {
        return sheduleController;
    }

    public void setSheduleController(SheduleController sheduleController) {
        this.sheduleController = sheduleController;
    }

    public List<ItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    public List<Institution> getAgencies() {
        if (agencies == null) {
            agencies = new ArrayList<>();
        }
        return agencies;
    }

    public void setAgencies(List<Institution> agencies) {
        this.agencies = agencies;
    }

    public List<ChannelDateDetailRow> getChannelDateDetailRows() {
        return channelDateDetailRows;
    }

    public void setChannelDateDetailRows(List<ChannelDateDetailRow> channelDateDetailRows) {
        this.channelDateDetailRows = channelDateDetailRows;
    }

    public ChannelTotal getChannelTotal() {
        if (channelTotal == null) {
            channelTotal = new ChannelTotal();
        }
        return channelTotal;
    }

    public void setChannelTotal(ChannelTotal channelTotal) {
        this.channelTotal = channelTotal;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public List<ChannelSummeryDateRangeOrUserRow> getChannelSummeryDateRangeOrUserRows() {
        return channelSummeryDateRangeOrUserRows;
    }

    public void setChannelSummeryDateRangeOrUserRows(List<ChannelSummeryDateRangeOrUserRow> channelSummeryDateRangeOrUserRows) {
        this.channelSummeryDateRangeOrUserRows = channelSummeryDateRangeOrUserRows;
    }

    public boolean isAgency() {
        return agency;
    }

    public void setAgency(boolean agency) {
        this.agency = agency;
    }

    public boolean isWithDocPayment() {
        return withDocPayment;
    }

    public void setWithDocPayment(boolean withDocPayment) {
        this.withDocPayment = withDocPayment;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<AgentHistory> getAgentHistorys() {
        return agentHistorys;
    }

    public void setAgentHistorys(List<AgentHistory> agentHistorys) {
        this.agentHistorys = agentHistorys;
    }

    public List<ChannelReferenceBookRow> getChannelReferenceBookRows() {
        return channelReferenceBookRows;
    }

    public void setChannelReferenceBookRows(List<ChannelReferenceBookRow> channelReferenceBookRows) {
        this.channelReferenceBookRows = channelReferenceBookRows;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public List<ChannelSheduleSummeryRow> getChannelSheduleSummeryRows() {
        return channelSheduleSummeryRows;
    }

    public void setChannelSheduleSummeryRows(List<ChannelSheduleSummeryRow> channelSheduleSummeryRows) {
        this.channelSheduleSummeryRows = channelSheduleSummeryRows;
    }

    public List<ChannelDoctorCountsRow> getChannelDoctorCountsRows() {
        return channelDoctorCountsRows;
    }

    public void setChannelDoctorCountsRows(List<ChannelDoctorCountsRow> channelDoctorCountsRows) {
        this.channelDoctorCountsRows = channelDoctorCountsRows;
    }

    public List<ColumnModel> getColumnModels() {
        return columnModels;
    }

    public void setColumnModels(List<ColumnModel> columnModels) {
        this.columnModels = columnModels;
    }

    public List<ChannelUserSummeryRow> getChannelUserSummeryRows() {
        return channelUserSummeryRows;
    }

    public void setChannelUserSummeryRows(List<ChannelUserSummeryRow> channelUserSummeryRows) {
        this.channelUserSummeryRows = channelUserSummeryRows;
    }

    
    
}
