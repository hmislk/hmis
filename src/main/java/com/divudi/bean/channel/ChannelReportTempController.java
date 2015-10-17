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
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSessionLeave;
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
import com.divudi.facade.StaffFacade;
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
    //
    List<Bill> bills;
    List<AgentReferenceBook> agentReferenceBooks;
    List<ServiceSessionLeave> serviceSessionLeaves;
    List<ChannelSummeryDateRangeBillTotalTable> channelSummeryDateRangeBillTotalTables;
    //
    Date fromDate;
    Date toDate;
    SearchKeyword searchKeyword;
    ReportKeyWord reportKeyWord;
    boolean count;
    boolean billedAgencys;
    boolean withOutDocPayment;
    boolean byDate;

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

    public double fetchBillsTotal(BillType[] billTypes, BillType bt, Class[] bills, Class[] nbills, Bill b, Date fd, Date td, Institution billedInstitution, Institution creditCompany, boolean withOutDocFee, boolean count, Staff staff) {

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

    public List<Staff> fetchBillsStaffs() {

        Date fd = commonFunctions.getStartOfMonth(fromDate);
        Date td = commonFunctions.getEndOfMonth(commonFunctions.getStartOfMonth(toDate));
        System.err.println("td = " + td);
        System.err.println("fd = " + fd);

        String sql;
        Map m = new HashMap();

        sql = " select distinct(b.staff) from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.staff is not null "
                + " order by b.staff.person.name ";

        m.put("fromDate", fd);
        m.put("toDate", td);
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);

        return getStaffFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void createAgentPaymentTable() {
        bills = new ArrayList<>();
        BillType[] bts = {BillType.AgentPaymentReceiveBill};
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class};
        bills = fetchBills(bts, classes, fromDate, toDate, getSessionController().getLoggedUser().getInstitution(), null);
        System.out.println("bills.size() = " + bills.size());

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

        agentReferenceBooks = getAgentReferenceBookFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
    }

    public void updateDecactivateAgentBook(AgentReferenceBook a) {

        a.setEditor(getSessionController().getLoggedUser());
        a.setEditedAt(new Date());
        getAgentReferenceBookFacade().edit(a);

        UtilityController.addSuccessMessage("Updated");
        createAgentReferenceBooks();
    }

    public void createConsultantLeaves() {
        String sql;
        HashMap m = new HashMap();

        sql = "Select s From ServiceSessionLeave s Where s.retired=false "
                + " and s.sessionDate between :fd and :td ";

        if (getReportKeyWord().getStaff() != null) {
            sql += "  and s.staff=:st";
            m.put("st", getReportKeyWord().getStaff());
        }

        sql += " order by s.sessionDate ";

        m.put("fd", fromDate);
        m.put("td", toDate);

        serviceSessionLeaves = getServiceSessionLeaveFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
    }

    public List<ChannelSummeryDateRangeBillTotalRow> fetchChannelSummeryRows(Institution i, BillType bt, BillType[] bts, boolean withOutDoc, boolean count, Staff s, boolean byDate) {
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

            acsr.setCanceledTotal(fetchBillsTotal(bts, bt, null, null, new CancelledBill(), fd, td, null, i, withOutDoc, count, s));
            ctot += acsr.getCanceledTotal();
            acsr.setRefundTotal(fetchBillsTotal(bts, bt, null, null, new RefundBill(), fd, td, null, i, withOutDoc, count, s));
            rtot += acsr.getRefundTotal();
            acsr.setBillTotal(fetchBillsTotal(bts, bt, null, null, new BilledBill(), fd, td, null, i, withOutDoc, count, s));
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
            aws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(a, BillType.ChannelAgent, null, withOutDocPayment, count, null, byDate));
            channelSummeryDateRangeBillTotalTables.add(aws);
        }
    }

    public void fetchStaffWiseChannelTotalOrCount() {
        channelSummeryDateRangeBillTotalTables = new ArrayList<>();
        for (Staff s : fetchBillsStaffs()) {
            ChannelSummeryDateRangeBillTotalTable sws = new ChannelSummeryDateRangeBillTotalTable();
            sws.setStaff(s);
            sws.setChannelSummeryDateRangeBillTotalRows(fetchChannelSummeryRows(null, null, new BillType[]{BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall,BillType.ChannelStaff}, withOutDocPayment, count, s, byDate));
            channelSummeryDateRangeBillTotalTables.add(sws);
        }
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

    //inner Classes
    public class ChannelSummeryDateRangeBillTotalTable {

        Institution Agency;
        Staff staff;
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

    }

    public class ChannelSummeryDateRangeBillTotalRow {

        String date;
        double billTotal;
        double canceledTotal;
        double refundTotal;
        boolean bold;

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

}
