/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.hr.StaffController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.channel.ReferenceBookEnum;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.RefundBill;
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
import com.divudi.facade.ServiceSessionLeaveFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
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
    StaffFacade staffFacade;
    @EJB
    SpecialityFacade specialityFacade;
    @EJB
    WebUserFacade webUserFacade;
    //
    @EJB
    ChannelBean channelBean;
    @EJB
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
    //
    List<Bill> bills;
    List<AgentReferenceBook> agentReferenceBooks;
    List<ServiceSessionLeave> serviceSessionLeaves;
    List<ChannelSummeryDateRangeBillTotalTable> channelSummeryDateRangeBillTotalTables;
    List<ItemFee> itemFees;
    List<Institution> agencies;
    List<ChannelDateDetailRow> channelDateDetailRows;
    List<ChannelSummeryDateRangeOrUserRow> channelSummeryDateRangeOrUserRows = new ArrayList<>();
    //
    Date fromDate;
    Date toDate;
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

    /**
     * Creates a new instance of ChannelReportTempController
     */
    public ChannelReportTempController() {
    }

    public List<Bill> fetchBills(BillType[] billTypes, Class[] bills, Date fd, Date td, Institution billedInstitution, Institution creditCompany) {

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
        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            m.put("cc", creditCompany);
        }

        sql += " order by b.createdAt ";

        m.put("fromDate", fd);
        m.put("toDate", td);

        System.err.println("Sql " + sql);
        System.out.println("m = " + m);
        return getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

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
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

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

        m.put("fromDate", fd);
        m.put("toDate", td);
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);
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

        m.put("fromDate", fd);
        m.put("toDate", td);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);
        if (count) {
            return getBillFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public List<Institution> fetchBillsAgencys() {

        Date fd = commonFunctions.getStartOfMonth(fromDate);
        Date td = commonFunctions.getEndOfMonth(commonFunctions.getStartOfMonth(toDate));
        System.err.println("td = " + td);
        System.err.println("fd = " + fd);

        String sql;
        Map m = new HashMap();

        sql = " select distinct(b.creditCompany) from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.creditCompany is not null "
                + " order by b.creditCompany.name ";

        m.put("fromDate", fd);
        m.put("toDate", td);
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);

        return getInstitutionFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

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
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);

        return getStaffFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

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
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);

        return getStaffFacade().findLongByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Speciality> fetchBillsSpecilitys() {

        Date fd = commonFunctions.getStartOfMonth(fromDate);
        Date td = commonFunctions.getEndOfMonth(commonFunctions.getStartOfMonth(toDate));
        System.err.println("td = " + td);
        System.err.println("fd = " + fd);

        String sql;
        Map m = new HashMap();

        sql = " select distinct(b.staff.speciality) from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.staff is not null "
                + " order by b.staff.person.name ";

        m.put("fromDate", fd);
        m.put("toDate", td);
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);

        return getSpecialityFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

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

        return getSpecialityFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
    }

    public Double[] fetchDocCountAndfees(Speciality sp, List<BillType> bts, Staff s) {

        String sql;
        Map m = new HashMap();

        sql = " select distinct(bf.bill) from BillFee  bf where "
                + " bf.bill.retired=false"
                + " and bf.bill.refunded=false "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.billType in :bt "
                + " and bf.bill.staff.speciality=:sp "
                + " and bf.bill.staff=:s "
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

        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("class", BilledBill.class);
        m.put("bt", bts);
        m.put("sp", sp);
        m.put("s", s);

        List<Bill> bs = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        Double[] d = new Double[3];
        d[0] = 0.0;
        d[1] = 0.0;
        d[2] = 0.0;
        if (!bs.isEmpty()) {
            d[0] = (double) bs.size();
            for (Bill b : bs) {
                d[1] += b.getStaffFee();
                d[2] += (b.getNetTotal() - b.getStaffFee());
            }
        }

        return d;
    }

    public void createAgentPaymentTable() {
        bills = new ArrayList<>();
        BillType[] bts = {BillType.AgentPaymentReceiveBill};
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class};
        bills = fetchBills(bts, classes, fromDate, toDate, getSessionController().getLoggedUser().getInstitution(), null);
        System.out.println("bills.size() = " + bills.size());

    }

    public void createCollectingCenterPaymentTable() {
        bills = new ArrayList<>();
        BillType[] bts = {BillType.CollectingCentrePaymentReceiveBill};
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class};
        bills = fetchBills(bts, classes, fromDate, toDate, getSessionController().getLoggedUser().getInstitution(), null);
        System.out.println("bills.size() = " + bills.size());

    }

    public void createChannelAgentReferenceBooks() {
        createAgentReferenceBooks(ReferenceBookEnum.ChannelBook);
    }

    public void createCollectingCenterReferenceBooks() {
        createAgentReferenceBooks(ReferenceBookEnum.LabBook);
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

        agentReferenceBooks = getAgentReferenceBookFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
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

        temMap.put("fd", fd);
        temMap.put("td", td);
        temMap.put("fdc", commonFunctions.getStartOfDay(getFromDate()));
        temMap.put("tdc", commonFunctions.getEndOfDay(getFromDate()));
        temMap.put("btp", billType);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";
        System.out.println("temMap = " + temMap);
        System.out.println("sql = " + sql);
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

        temMap.put("fd", fd);
        temMap.put("td", td);
        temMap.put("btp", billType);
        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";
        System.out.println("temMap = " + temMap);
        System.out.println("sql = " + sql);
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createUsercollectionByDate() {
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
            System.out.println("td = " + td);
            System.out.println("fd = " + fd);
            System.out.println("nowDate = " + nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);
            System.out.println("formatedDate = " + formatedDate);
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
            System.out.println("nowDate = " + nowDate);
        }
    }

    public void createUsercollectionByDateCreated() {
        if (getReportKeyWord().getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select User.");
            return;
        }
        Date todayLastBillDate=fetchTodaybill(Arrays.asList(new BillType[]{BillType.ChannelCash,BillType.ChannelAgent,BillType.ChannelPaid}), reportKeyWord.getWebUser(), false);
        if (todayLastBillDate==null) {
            JsfUtil.addErrorMessage("This User Has not Bill any Bill Selected Day");
            return;
        }
        System.out.println("todayLastBillDate = " + todayLastBillDate);
        channelDateDetailRows = new ArrayList<>();
        channelTotal = new ChannelTotal();
        Date nowDate = getFromDate();
//        while (nowDate.before(getToDate())) {
        while (nowDate.before(todayLastBillDate)) {
            ChannelDateDetailRow row = new ChannelDateDetailRow();
            String formatedDate;
            Date fd;
            Date td;
            fd = commonFunctions.getStartOfDay(nowDate);
            td = commonFunctions.getEndOfDay(nowDate);
            System.out.println("td = " + td);
            System.out.println("fd = " + fd);
            System.out.println("nowDate = " + nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
            formatedDate = df.format(fd);
            System.out.println("formatedDate = " + formatedDate);
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
            System.out.println("nowDate = " + nowDate);
        }
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

        serviceSessionLeaves = getServiceSessionLeaveFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
    }

    public void createChannelCountByUserOrDate() {
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
                System.out.println("formatedDate = " + formatedDate);
                row.setDate(formatedDate);
                row.setUserRows(fetchUserRows(fd, td, bts));
                System.out.println("row.getUserRows().size() = " + row.getUserRows().size());
                if (row.getUserRows().size() > 1) {
                    channelSummeryDateRangeOrUserRows.add(row);
                }

                Calendar cal = Calendar.getInstance();
                cal.setTime(nowDate);
                cal.add(Calendar.DATE, 1);
                nowDate = cal.getTime();
                System.out.println("nowDate = " + nowDate);
            }

        } else {
            for (WebUser webUser : fetchCashiers(bts)) {
                ChannelSummeryDateRangeOrUserRow row = new ChannelSummeryDateRangeOrUserRow();
                row.setUser(webUser);
                row.setDateRangeRows(fetchDateRangeRows(getFromDate(), getToDate(), webUser, bts));
                System.out.println("row.getDateRangeRows().size() = " + row.getDateRangeRows().size());
                if (row.getDateRangeRows().size() > 1) {
                    channelSummeryDateRangeOrUserRows.add(row);
                }
            }
        }
        System.out.println("channelSummeryDateRangeOrUserRows.size() = " + channelSummeryDateRangeOrUserRows.size());

    }

    public void createChannelCountByUserOrDate2() {
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
            System.err.println("Sql " + sql);
            System.out.println("m = " + m);
            List<Bill> bills = new ArrayList<>();
            bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
            Date fd = getFromDate();
            for (Bill b : bills) {
                System.out.println("1.b.getCreatedAt() = " + b.getCreatedAt());
                System.out.println("1.fd = " + fd);
                if (b.getCreatedAt().getTime() < fd.getTime()) {
                    fd = b.getCreatedAt();
                }
                System.out.println("2.b.getCreatedAt() = " + b.getCreatedAt());
                System.out.println("2.fd = " + fd);
            }

            //
            row.setDateRangeRows(fetchDateRangeRowsSession(fd, commonFunctions.getEndOfDay(new Date()), webUser, bts));
            System.out.println("row.getDateRangeRows().size() = " + row.getDateRangeRows().size());
            if (row.getDateRangeRows().size() > 1) {
                channelSummeryDateRangeOrUserRows.add(row);
            }
        }
        System.out.println("channelSummeryDateRangeOrUserRows.size() = " + channelSummeryDateRangeOrUserRows.size());

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
                System.out.println("td = " + td);
                System.out.println("fd = " + fd);
                System.out.println("nowDate = " + nowDate);

                DateFormat df = new SimpleDateFormat("yyyy MMMM dd");
                formatedDate = df.format(fd);
                System.out.println("formatedDate = " + formatedDate);

            } else {
                fd = commonFunctions.getStartOfMonth(nowDate);
                td = commonFunctions.getEndOfMonth(nowDate);
                System.out.println("td = " + td);
                System.out.println("fd = " + fd);
                System.out.println("nowDate = " + nowDate);

                DateFormat df = new SimpleDateFormat("yyyy MMMM");
                formatedDate = df.format(fd);
                System.out.println("formatedDate = " + formatedDate);
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
            System.out.println("nowDate = " + nowDate);
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
            acsrs.add(acsr);

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
        System.out.println("institutions.size() = " + institutions.size());
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

    public void fetchSpecilityWiseDoctorAppoinmentCount() {
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
        cashiers = getWebUserFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
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
        cashiers = getWebUserFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
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
            double netTotal = fetchBillsTotal(bts, null, null, null, null, fDate, tDate, null, null, false, false, null, null, webUser);
            double hosTotal = fetchBillsTotal(bts, null, null, null, new BilledBill(), fDate, tDate, null, null, true, false, null, null, webUser);
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
            System.out.println("formatedDate = " + formatedDate);
            row.setDate(formatedDate);
            row.setBillCount(fetchBillsTotal(bts, null, null, null, new BilledBill(), fd, td, null, null, false, true, null, null, webUser));
            row.setCanceledCount(fetchBillsTotal(bts, null, null, null, new CancelledBill(), fd, td, null, null, false, true, null, null, webUser));
            row.setRefundCount(fetchBillsTotal(bts, null, null, null, new RefundBill(), fd, td, null, null, false, true, null, null, webUser));
            double netTotal = fetchBillsTotal(bts, null, null, null, null, fd, td, null, null, false, false, null, null, webUser);
            double hosTotal = fetchBillsTotal(bts, null, null, null, new BilledBill(), fd, td, null, null, true, false, null, null, webUser);
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
            System.out.println("nowDate = " + nowDate);
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
            System.out.println("formatedDate = " + formatedDate);
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
            System.out.println("nowDate = " + nowDate);
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

        temMap.put("fdc", commonFunctions.getStartOfDay(new Date()));
        temMap.put("tdc", commonFunctions.getEndOfDay(new Date()));
        temMap.put("btps", billTypes);
        temMap.put("ins", getSessionController().getInstitution());

        
        System.out.println("temMap = " + temMap);
        System.out.println("sql = " + sql);
        return getBillFacade().findDateByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createAgentWiseAppoinmentCount() {
        fetchAgentWiseChannelTotal();
    }

    public void createAgentWiseAppoinmentTotal() {
        fetchAgentWiseChannelTotal();
    }

    public void createStaffWiseAppoinmentCount() {
        fetchStaffWiseChannelTotalOrCount();
    }

    public void createStaffWiseAppoinmentTotal() {
        fetchStaffWiseChannelTotalOrCount();
    }

    public void createSpecilityWiseAppoinmentCount() {
        fetchSpecilityWiseChannelTotalOrCount();
    }

    public void createSpecilityWiseAppoinmentTotal() {
        fetchSpecilityWiseChannelTotalOrCount();
    }

    public void createSpecilityWiseDoctorAppoinmentCount() {
        fetchSpecilityWiseDoctorAppoinmentCount();
    }

    public void createStaffShedules() {
        itemFees = getSheduleController().fetchStaffServiceSessions();
    }

    public void createAgencyBalanceTable() {
        String sql;
        HashMap m = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType=:typ ";

        m.put("typ", InstitutionType.Agency);

        agencies = getInstitutionFacade().findBySQL(sql, m);
    }

    public void createCollectingcenterBalanceTable() {
        String sql;
        HashMap m = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType=:typ ";

        m.put("typ", InstitutionType.CollectingCentre);

        agencies = getInstitutionFacade().findBySQL(sql, m);
    }

    public void createStaffWiseChannelBillTypeCountTable() {
        fetchStaffWiseChannelBillTypeCount();
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

    //inner Classes(Data Structures)
    public class ChannelSummeryDateRangeBillTotalTable {

        Institution Agency;
        Staff staff;
        Speciality speciality;
        double[] count;

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

}
