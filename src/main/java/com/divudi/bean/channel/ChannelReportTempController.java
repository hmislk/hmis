/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

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
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
    AgentReferenceBookFacade agentReferenceBookFacade;
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
    //
    List<Bill> bills;
    List<AgentReferenceBook> agentReferenceBooks;
    //
    Date fromDate;
    Date toDate;
    SearchKeyword searchKeyword;

    /**
     * Creates a new instance of ChannelReportTempController
     */
    public ChannelReportTempController() {
    }

    public List<Bill> fetchBills(BillType[] billTypes, Class[] bills, BillType bt, Date fd, Date td, Institution billedInstitution) {

        String sql;
        Map m = new HashMap();

        sql = "select b from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (billTypes != null) {
            sql += " and b.billType in :bt ";
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bt", bts);
        }
        if (billTypes != null) {
            sql += " and b.billType in :bts ";
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bts", bts);
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
        if (billedInstitution != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", billedInstitution);
        }

        sql += " order by b.createdAt ";

        m.put("fromDate", fd);
        m.put("toDate", td);

        System.err.println("Sql " + sql);
        System.out.println("m = " + m);
        return getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void createAgentPaymentTable() {
        bills = new ArrayList<>();
        BillType[] bts = {BillType.AgentPaymentReceiveBill};
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class};
        bills = fetchBills(bts, classes, null, fromDate, toDate, getSessionController().getLoggedUser().getInstitution());
        System.out.println("bills.size() = " + bills.size());

    }

    public void createAgentReferenceBooks() {
        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a where "
                + " a.createdAt between :fd and :td ";
        
        if (!getSearchKeyword().isWithRetiered()) {
            sql+= " and a.retired=false ";
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

}
