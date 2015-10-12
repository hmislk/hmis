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
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ServiceSessionLeaveFacade;
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
import org.apache.log4j.helpers.DateTimeDateFormat;

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
    AgentReferenceBookFacade agentReferenceBookFacade;
    @EJB
    ServiceSessionLeaveFacade serviceSessionLeaveFacade;
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
    List<AgentWithSummery> agentWithSummerys;
    //
    Date fromDate;
    Date toDate;
    SearchKeyword searchKeyword;
    ReportKeyWord reportKeyWord;

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

    public double fetchBillsTotal(BillType[] billTypes,BillType bt, Class[] bills,Bill b, Date fd, Date td, Institution billedInstitution, Institution creditCompany, boolean withOutDocFee) {

        String sql;
        Map m = new HashMap();

        if (withOutDocFee) {
            sql = " select (b.netTotal-b.staffFee) ";
        } else {
            sql = " select (b.netTotal) ";
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

        sql += " order by b.createdAt ";

        m.put("fromDate", fd);
        m.put("toDate", td);
        System.out.println("creditCompany.getName() = " + creditCompany.getName());
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);
        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }
    
    public double fetchBillsTotal(BillType bt, Bill b, Date fd, Date td,  Institution creditCompany, boolean withOutDocFee) {

        String sql;
        Map m = new HashMap();

        if (withOutDocFee) {
            sql = " select (b.netTotal-b.staffFee) ";
        } else {
            sql = " select (b.netTotal) ";
        }

        sql += " from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (bt != null) {
            sql += " and b.billType=:bt ";
            m.put("bt", bt);
        }
        if (b != null) {
            sql += " and type(b)=:class ";
            m.put("class", b.getClass());
        }
        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            m.put("cc", creditCompany);
        }

        sql += " order by b.createdAt ";

        m.put("fromDate", fd);
        m.put("toDate", td);
        System.out.println("creditCompany.getName() = " + creditCompany.getName());
        System.err.println("Sql " + sql);
        System.out.println("m = " + m);
        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

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

    public List<AgentChannelSummeryRow> fetchAgentChannelSummeryRows(Institution i, BillType bt, boolean withOutDoc) {
        List<AgentChannelSummeryRow> acsrs = new ArrayList<>();
        Date nowDate = getFromDate();
        double btot = 0.0;
        double ctot = 0.0;
        double rtot = 0.0;
        while (nowDate.before(getToDate())) {
            Date fd = commonFunctions.getStartOfMonth(nowDate);
            Date td = commonFunctions.getEndOfMonth(nowDate);
            System.out.println("td = " + td);
            System.out.println("fd = " + fd);
            System.out.println("nowDate = " + nowDate);

            DateFormat df = new SimpleDateFormat("yyyy MMMM");
            String formatedDate = df.format(fd);
            System.out.println("formatedDate = " + formatedDate);

            AgentChannelSummeryRow acsr = new AgentChannelSummeryRow();
            acsr.setDate(formatedDate);
            acsr.setBilledTotal(fetchBillsTotal(bt,new BilledBill(), fd, td, i, withOutDoc));
            btot += acsr.getBilledTotal();
            System.out.println("fetchBillsTotal(bt,new BilledBill(), fd, td, i, withOutDoc) = " + fetchBillsTotal(bt,new BilledBill(), fd, td, i, withOutDoc));
            System.out.println("acsr.getBilledTotal() = " + acsr.getBilledTotal());
            acsr.setCanceledTotal(fetchBillsTotal(null,bt, null,new CancelledBill(), fd, td, null, i, withOutDoc));
            ctot += acsr.getCanceledTotal();
            acsr.setRefundTotal(fetchBillsTotal(null,bt, null,new RefundBill(), fd, td, null, i, withOutDoc));
            rtot += acsr.getRefundTotal();
            acsrs.add(acsr);

            Calendar cal = Calendar.getInstance();
            cal.setTime(nowDate);
            cal.add(Calendar.MONTH, 1);
            nowDate = cal.getTime();
            System.out.println("nowDate = " + nowDate);
        }
        AgentChannelSummeryRow acsr = new AgentChannelSummeryRow();
        acsr.setDate("Total");
        acsr.setBilledTotal(btot);
        acsr.setCanceledTotal(ctot);
        acsr.setRefundTotal(rtot);
        acsr.setBold(true);
        acsrs.add(acsr);
        return acsrs;
    }

    public void fetchAgentWiseChannelTotal(boolean withOutDocPayment) {
        agentWithSummerys = new ArrayList<>();
        for (Institution a : getInstitutionController().getAgencies()) {
            AgentWithSummery aws = new AgentWithSummery();
            aws.setAgency(a);
            aws.setAgentChannelSummeryRows(fetchAgentChannelSummeryRows(a,BillType.ChannelAgent, withOutDocPayment));
            agentWithSummerys.add(aws);
        }
    }
    
    public void createAgentWiseAppoinmentTotalWithoutDocFee(){
        fetchAgentWiseChannelTotal(true);
    }
    
    public void createAgentWiseAppoinmentTotalWithDocFee(){
        fetchAgentWiseChannelTotal(false);
    }

    //inner Classes
    public class AgentWithSummery {

        Institution Agency;
        List<AgentChannelSummeryRow> agentChannelSummeryRows;

        public Institution getAgency() {
            return Agency;
        }

        public void setAgency(Institution Agency) {
            this.Agency = Agency;
        }

        public List<AgentChannelSummeryRow> getAgentChannelSummeryRows() {
            return agentChannelSummeryRows;
        }

        public void setAgentChannelSummeryRows(List<AgentChannelSummeryRow> agentChannelSummeryRows) {
            this.agentChannelSummeryRows = agentChannelSummeryRows;
        }
    }

    public class AgentChannelSummeryRow {

        String date;
        double billedTotal;
        double canceledTotal;
        double refundTotal;
        boolean bold;

        public double getBilledTotal() {
            return billedTotal;
        }

        public void setBilledTotal(double billedTotal) {
            this.billedTotal = billedTotal;
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

    public List<AgentWithSummery> getAgentWithSummerys() {
        return agentWithSummerys;
    }

    public void setAgentWithSummerys(List<AgentWithSummery> agentWithSummerys) {
        this.agentWithSummerys = agentWithSummerys;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

}
