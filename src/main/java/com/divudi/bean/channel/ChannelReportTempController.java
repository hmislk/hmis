/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.hr.StaffController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.InstitutionType;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.ChannelBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSessionLeave;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
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
import com.divudi.bean.common.util.JsfUtil;
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
    ChannelScheduleController sheduleController;
    //
    List<Bill> bills;
    List<AgentReferenceBook> agentReferenceBooks;
    List<ServiceSessionLeave> serviceSessionLeaves;
    List<ChannelSummeryDateRangeBillTotalTable> channelSummeryDateRangeBillTotalTables;
    List<ItemFee> itemFees;
    List<Institution> agencies;
    //
    Date fromDate;
    Date toDate;
    SearchKeyword searchKeyword;
    ReportKeyWord reportKeyWord;
    boolean count;
    boolean billedAgencys;
    boolean withOutDocPayment;
    boolean byDate;
    boolean sessoinDate;
    boolean paid;
    boolean scan;
    

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

        return getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchBillsTotal(BillType[] billTypes, BillType bt, Class[] bills, Class[] nbills, Bill b, Date fd, Date td, Institution billedInstitution, Institution creditCompany, boolean withOutDocFee, boolean count, Staff staff, Speciality sp) {

        String sql;
        Map m = new HashMap();
        if (count) {
            sql = " select count(b) ";
        } else {
            if (withOutDocFee) {
                sql = " select sum(b.netTotal-b.staffFee) ";
            } else {
                sql = " select sum(b.netTotal) ";
            }
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
        if (sp != null) {
            sql += " and b.staff.speciality=:sp ";
            m.put("sp", sp);
        }

        m.put("fromDate", fd);
        m.put("toDate", td);
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

    public List<Staff> fetchBillsStaffs(Speciality s,List<BillType> billTypes) {

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

        List<Bill> bs = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

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

    }
    
    public void createCollectingCenterPaymentTable() {
        bills = new ArrayList<>();
        BillType[] bts = {BillType.CollectingCentrePaymentReceiveBill};
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class};
        bills = fetchBills(bts, classes, fromDate, toDate, getSessionController().getLoggedUser().getInstitution(), null);

    }

    public void createAgentReferenceBooks() {
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

        sql += " order by a.bookNumber ";

        m.put("fd", fromDate);
        m.put("td", toDate);

        agentReferenceBooks = getAgentReferenceBookFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void updateDecactivateAgentBook(AgentReferenceBook a) {

        a.setEditor(getSessionController().getLoggedUser());
        a.setEditedAt(new Date());
        getAgentReferenceBookFacade().edit(a);

        JsfUtil.addSuccessMessage("Updated");
        createAgentReferenceBooks();
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

            acsr.setCanceledTotal(fetchBillsTotal(bts, bt, null, null, new CancelledBill(), fd, td, null, i, withOutDoc, count, s, sp));
            ctot += acsr.getCanceledTotal();
            acsr.setRefundTotal(fetchBillsTotal(bts, bt, null, null, new RefundBill(), fd, td, null, i, withOutDoc, count, s, sp));
            rtot += acsr.getRefundTotal();
            acsr.setBillTotal(fetchBillsTotal(bts, bt, null, null, new BilledBill(), fd, td, null, i, withOutDoc, count, s, sp));
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

    public List<ChannelSummeryDateRangeBillTotalRow> fetchChannelSummeryRows(Speciality sp, List<BillType> bts) {
        List<ChannelSummeryDateRangeBillTotalRow> acsrs = new ArrayList<>();

        double totalCount = 0;
        double totalDocfee = 0;
        double totalHosfee = 0;

        for (Staff s : fetchBillsStaffs(sp,null)) {

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
        for (Institution a : institutions) {
            ChannelSummeryDateRangeBillTotalTable aws = new ChannelSummeryDateRangeBillTotalTable();
            aws.setAgency(a);
            aws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(a, BillType.ChannelAgent, null, withOutDocPayment, count, null, byDate, null));
            channelSummeryDateRangeBillTotalTables.add(aws);
        }
    }

    public void fetchStaffWiseChannelTotalOrCount() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        for (Staff s : fetchBillsStaffs(null,null)) {
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
        for (Staff s : fetchBillsStaffs(null,billTypes)) {
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

        agencies = getInstitutionFacade().findByJpql(sql, m);
    }
    
    public void createCollectingcenterBalanceTable() {
        String sql;
        HashMap m = new HashMap();
        sql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType=:typ ";

        m.put("typ", InstitutionType.CollectingCentre);

        agencies = getInstitutionFacade().findByJpql(sql, m);
    }

    public void createStaffWiseChannelBillTypeCountTable() {
        fetchStaffWiseChannelBillTypeCount();
    }

    public void clearAllReportData() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
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

    //Getters and Setters
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
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

    public ChannelScheduleController getSheduleController() {
        return sheduleController;
    }

    public void setSheduleController(ChannelScheduleController sheduleController) {
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

}
