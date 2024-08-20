/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.AuditEventApplicationController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PaymentMethodValue;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.dataStructure.CashierSummeryData;
import com.divudi.data.dataStructure.WebUserBillsTotal;
import com.divudi.data.table.String1Value1;
import com.divudi.data.table.String1Value5;

import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Buddhika
 */
@Named
@javax.enterprise.context.RequestScoped
public class CashierReportController implements Serializable {

    @Inject
    private SessionController sessionController;
    @Inject
    private CommonController commonController;

    private CommonFunctions commonFunction;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private BillFacade billFacade;
    private WebUser currentCashier;
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;
    String fromReciptNo;
    String toReciptNo;
    private List<WebUser> cashiers;
    private List<CashierSummeryData> cashierDatas;
    private double finalCashTot;
    private double finalCreditTot;
    private double finalCardTot;
    private int reportCashierIndex;
    private CashierSummeryData current;
    double finalChequeTot;
    private List<String1Value1> dataTableDatas;
    @Inject
    private EnumController enumController;
    @Inject
    CommonReport commonReport;

    @Inject
    AuditEventApplicationController auditEventApplicationController;

    String header = "";

    /**
     *
     * @return
     */
    public String navigateToOpdSummeries() {
        String fileUrl = "/analytics/opd/index?faces-redirect=true;";
        return fileUrl;
    }

    public void recreteModal() {
        finalCashTot = 0.0;
        finalChequeTot = 0.0;
        finalCardTot = 0.0;
        finalCreditTot = 0.0;
        current = null;
        cashierDatas = null;
        cashiers = null;
        currentCashier = null;
        dataTableDatas = null;
    }

    public void recreteModal2() {
        fromReciptNo = null;
        toReciptNo = null;
        recreteModal();
    }

    public String navigateToDepartmentAllCashierReport() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        auditEvent.setEventTrigger("navigateToDepartmentAllCashierReport()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

        return "/reportCashier/report_cashier_summery_all?faces-redirect=true";
    }


    public String navigateToCashierReport() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToCashierReport()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_cashier_detailed_by_user_by_reciptno.xhtml";
    }

    public String navigateToDayEndSummary() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToCashierSummary()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/day_end_summery.xhtml?faces-redirect=true";
    }

    public String navigateToShiftEndSummary() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToCashierSummary()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/shift_end_summery.xhtml?faces-redirect=true";
    }


    public String navigateToAllCashierReportUsingReciptNo() {
        return "/reportCashier/report_cashier_summery_by_user_by_reciptno.xhtml";
    }

    public String navigateToAllCashierSummary() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToAllCashierSummary()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_cashier_summery_all_total_only.xhtml?faces-redirect=true";
    }

    public String navigateToAllCashierSummaryOnlyByReciptNo() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToAllCashierSummaryOnlyByReciptNo()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_cashier_summery_all_total_only_by_reciptno.xhtml?faces-redirect=true";
    }

    public String navigateToReportCashierSummeryAllTotalOnlyWithoutPro() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportCashierSummeryAllTotalOnlyWithoutPro()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_cashier_summery_all_total_only_without_pro.xhtml?faces-redirect=true";
    }

    public String navigateToReportIncomeWithoutCreditByInstitution() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportIncomeWithoutCreditByInstitution()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_income_without_credit_by_institution.xhtml?faces-redirect=true";
    }

    public String navigateToOpdSearchUserBills() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToOpdSearchUserBills()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/opd_search_user_bills.xhtml?faces-redirect=true";
    }

    public String navigateToBillTypeSummery() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToBillTypeSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportIncome/bill_type_summery.xhtml?faces-redirect=true";
    }

    public String navigateToReportCashierDetailedUserByBillType() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportIncomeWithoutCreditByInstitution()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_cashier_detailed_user_by_billType.xhtml?faces-redirect=true";
    }

    public String navigateToOpdBillReport() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateTOopdBillReport()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/opd_bill_report.xhtml?faces-redirect=true";
    }

    public String navigateToReportOpdBillsForVat() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportOpdBillsForVat()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_opd_bills_for_vat.xhtml?faces-redirect=true";
    }

    public String navigateToReportOpdBillPaymentSheame() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportOpdBillPaymentSheame()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_opd_bill_payment_sheame.xhtml?faces-redirect=true";
    }

    public String navigateToReportCashierDetailedByUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportCashierDetailedByUser()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_cashier_detailed_by_user.xhtml?faces-redirect=true";
    }

    public String navigateToReportCashierItemCountByUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportCashierItemCountByUser()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_cashier_item_count_by_user.xhtml?faces-redirect=true";
    }

    public CashierReportController() {
    }

    public List<CashierSummeryData> getCashierDatasOwn() {
        cashierDatas = new ArrayList<>();
        for (WebUser w : getCashiers()) {
            CashierSummeryData temp = new CashierSummeryData();
            temp.setCasheir(w);
            findSummeryOwn(temp, w);
            setDataTable(temp);
            cashierDatas.add(temp);

        }

        return cashierDatas;
    }

    private BillsTotals calculateBillTotalsByPaymentMethod(BillType billType, String suffix, Bill bill, WebUser webUser) {
        BillsTotals newB = new BillsTotals();
        newB.setName(billType.getLabel() + " " + suffix);
        for (Object o : userBillTotalsWithPaymentMethods(webUser, bill, billType)) {
            PaymentMethodValue pmv = (PaymentMethodValue) o;
            switch (pmv.getPaymentMethod()) {
                case Card:
                    newB.setCard(pmv.getValue());
                    finalCardTot += pmv.getValue();
                    break;
                case Cash:
                    newB.setCash(pmv.getValue());
                    finalCashTot += pmv.getValue();
                    break;

                case Cheque:
                    newB.setCheque(pmv.getValue());
                    finalChequeTot += pmv.getValue();
                    break;
                case Credit:
                    newB.setCredit(pmv.getValue());
                    finalCreditTot += pmv.getValue();
                    break;
                case Slip:
                    newB.setSlip(pmv.getValue());
                    finalSlipTot += pmv.getValue();
                    break;
            }
        }
        return newB;
    }

//    New Method is calculateBillTotalsByPaymentMethod
    @Deprecated
    private BillsTotals createRow(BillType billType, String suffix, Bill bill, WebUser webUser) {
        BillsTotals newB = new BillsTotals();
        newB.setName(billType.getLabel() + " " + suffix);
        newB.setCard(calTotalValueForLoggedDepartment(webUser, bill, PaymentMethod.Card, billType));
        finalCardTot += newB.getCard();
        newB.setCash(calTotalValueForLoggedDepartment(webUser, bill, PaymentMethod.Cash, billType));
        finalCashTot += newB.getCash();
        newB.setCheque(calTotalValueForLoggedDepartment(webUser, bill, PaymentMethod.Cheque, billType));
        finalChequeTot += newB.getCheque();
        newB.setCredit(calTotalValueForLoggedDepartment(webUser, bill, PaymentMethod.Credit, billType));
        finalCreditTot += newB.getCredit();
        newB.setSlip(calTotalValueForLoggedDepartment(webUser, bill, PaymentMethod.Slip, billType));
        finalSlipTot += newB.getSlip();

        return newB;

    }

    private BillsTotals createRowAgent(BillType billType, String suffix, Bill bill, WebUser webUser) {
        BillsTotals newB = new BillsTotals();
        newB.setName(billType.getLabel() + " " + suffix);
        newB.setCash(calTotalValueOwn(webUser, bill, PaymentMethod.Cash, billType));
        finalCashTot += newB.getCash();

        return newB;

    }

    private BillsTotals createRowWithoutPro(BillType billType, String suffix, Bill bill, WebUser webUser) {
        BillsTotals newB = new BillsTotals();
        newB.setName(billType.getLabel() + " " + suffix);
        newB.setCard(calTotalValueOwnWithoutPro(webUser, bill, PaymentMethod.Card, billType));
        finalCardTot += newB.getCard();
        newB.setCash(calTotalValueOwnWithoutPro(webUser, bill, PaymentMethod.Cash, billType));
        finalCashTot += newB.getCash();
        newB.setCheque(calTotalValueOwnWithoutPro(webUser, bill, PaymentMethod.Cheque, billType));
        finalChequeTot += newB.getCheque();
        newB.setCredit(calTotalValueOwnWithoutPro(webUser, bill, PaymentMethod.Credit, billType));
        finalCreditTot += newB.getCredit();
        newB.setSlip(calTotalValueOwnWithoutPro(webUser, bill, PaymentMethod.Slip, billType));
        finalSlipTot += newB.getSlip();

        return newB;

    }

    private BillsTotals createRowInOut(BillType billType, String suffix, Bill bill, WebUser webUser) {
        BillsTotals newB = new BillsTotals();
        newB.setName(billType.getLabel() + " " + suffix);
        newB.setCard(calTotalValueOwnInOutCreditCard(webUser, bill, billType));
        finalCardTot += newB.getCard();
        newB.setCash(calTotalValueOwnInOutCash(webUser, bill, billType));
        finalCashTot += newB.getCash();
        newB.setCheque(calTotalValueOwnInOutCheque(webUser, bill, billType));
        finalChequeTot += newB.getCheque();
        newB.setSlip(calTotalValueOwnInOutSlip(webUser, bill, billType));
        finalSlipTot += newB.getSlip();

        return newB;

    }

    public void calCashierData() {
        String ipAddress;
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startedTime = new Date();
        auditEvent.setEventDataTime(startedTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setEventTrigger("calCashierData()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiers()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypes()) {

                BillsTotals newB = createRow(btp, "Billed", new BilledBill(), webUser);

                if (newB.getCard() != 0 || newB.getCash() != 0 || newB.getCheque() != 0 || newB.getCredit() != 0 || newB.getSlip() != 0) {
                    billls.add(newB);
                }

                BillsTotals newC = createRow(btp, "Cancelled", new CancelledBill(), webUser);

                if (newC.getCard() != 0 || newC.getCash() != 0 || newC.getCheque() != 0 || newC.getCredit() != 0 || newC.getSlip() != 0) {
                    billls.add(newC);
                }

                BillsTotals newR = createRow(btp, "Refunded", new RefundBill(), webUser);

                if (newR.getCard() != 0 || newR.getCash() != 0 || newR.getCheque() != 0 || newR.getCredit() != 0 || newR.getSlip() != 0) {
                    billls.add(newR);
                }

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }
            //channel agent bill cash cancel refund
            BillsTotals newC = createRowAgent(BillType.ChannelAgent, "Cancelled", new CancelledBill(), webUser);

            if (newC.getCard() != 0 || newC.getCash() != 0 || newC.getCheque() != 0 || newC.getCredit() != 0 || newC.getSlip() != 0) {
                billls.add(newC);
            }

            BillsTotals newR = createRow(BillType.ChannelAgent, "Refunded", new RefundBill(), webUser);

            if (newR.getCard() != 0 || newR.getCash() != 0 || newR.getCheque() != 0 || newR.getCredit() != 0 || newR.getSlip() != 0) {
                billls.add(newR);
            }

            uCash += (newC.getCash() + newR.getCash());

            //
            //Cash In
            BillsTotals newIn = createRowInOut(BillType.CashIn, "Billed", new BilledBill(), webUser);

            if (newIn.getCard() != 0 || newIn.getCash() != 0
                    || newIn.getCheque() != 0 || newIn.getCredit() != 0
                    || newIn.getSlip() != 0) {
                billls.add(newIn);
            }

            uCard += (newIn.getCard());
            uCash += (newIn.getCash());
            uCheque += (newIn.getCheque());
            uCredit += (newIn.getCredit());
            uSlip += (newIn.getSlip());

            BillsTotals newInCan = createRowInOut(BillType.CashIn, "Cancelled", new CancelledBill(), webUser);
            uCard += (newInCan.getCard());
            uCash += (newInCan.getCash());
            uCheque += (newInCan.getCheque());
            uCredit += (newInCan.getCredit());
            uSlip += (newInCan.getSlip());

            if (newInCan.getCard() != 0 || newInCan.getCash() != 0
                    || newInCan.getCheque() != 0 || newInCan.getCredit() != 0
                    || newInCan.getSlip() != 0) {
                billls.add(newInCan);
            }

            //Cash Out
            BillsTotals newOut = createRowInOut(BillType.CashOut, "Billed", new BilledBill(), webUser);

            if (newOut.getCard() != 0 || newOut.getCash() != 0 || newOut.getCheque() != 0 || newOut.getCredit() != 0 || newOut.getSlip() != 0) {
                billls.add(newOut);
            }

            uCard += (newOut.getCard());
            uCash += (newOut.getCash());
            uCheque += (newOut.getCheque());
            uCredit += (newOut.getCredit());
            uSlip += (newOut.getSlip());

            if (newOut.getCard() != 0 || newOut.getCash() != 0
                    || newOut.getCheque() != 0 || newOut.getCredit() != 0
                    || newOut.getSlip() != 0) {
                billls.add(newOut);
            }

            BillsTotals newOutCan = createRowInOut(BillType.CashOut, "Cancelled", new CancelledBill(), webUser);
            uCard += (newOutCan.getCard());
            uCash += (newOutCan.getCash());
            uCheque += (newOutCan.getCheque());
            uCredit += (newOutCan.getCredit());
            uSlip += (newOutCan.getSlip());

            if (newOutCan.getCard() != 0 || newOutCan.getCash() != 0
                    || newOutCan.getCheque() != 0 || newOutCan.getCredit() != 0
                    || newOutCan.getSlip() != 0) {
                billls.add(newOutCan);
            }

            //Adjustment
            BillsTotals adj = createRowInOut(BillType.DrawerAdjustment, "Adjusment", new BilledBill(), webUser);
            uCard += (adj.getCard());
            uCash += (adj.getCash());
            uCheque += (adj.getCheque());
            uCredit += (adj.getCredit());
            uSlip += (adj.getSlip());

            if (adj.getCard() != 0 || adj.getCash() != 0
                    || adj.getCheque() != 0 || adj.getCredit() != 0
                    || adj.getSlip() != 0) {
                billls.add(adj);
            }

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }

        Date startTime = new Date();
        

        Date endTime = new Date();
        duration = endTime.getTime() - startedTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
    }

    public void calCashierDataChannel() {

        String ipAddress;
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startedTime = new Date();
        auditEvent.setEventDataTime(startedTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("calCashierDataChannel()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        header = "Channel";
        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiersChannel()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypesChannel()) {
                BillsTotals newB = createRow(btp, "Billed", new BilledBill(), webUser);

                if (newB.getCard() != 0 || newB.getCash() != 0 || newB.getCheque() != 0 || newB.getCredit() != 0 || newB.getSlip() != 0) {
                    billls.add(newB);
                }

                BillsTotals newC = createRow(btp, "Cancelled", new CancelledBill(), webUser);

                if (newC.getCard() != 0 || newC.getCash() != 0 || newC.getCheque() != 0 || newC.getCredit() != 0 || newC.getSlip() != 0) {
                    billls.add(newC);
                }

                BillsTotals newR = createRow(btp, "Refunded", new RefundBill(), webUser);

                if (newR.getCard() != 0 || newR.getCash() != 0 || newR.getCheque() != 0 || newR.getCredit() != 0 || newR.getSlip() != 0) {
                    billls.add(newR);
                }

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }
            //channel agent bill cash cancel refund
            BillsTotals newC = createRowAgent(BillType.ChannelAgent, "Cancelled", new CancelledBill(), webUser);

            if (newC.getCard() != 0 || newC.getCash() != 0 || newC.getCheque() != 0 || newC.getCredit() != 0 || newC.getSlip() != 0) {
                billls.add(newC);
            }

            BillsTotals newR = createRow(BillType.ChannelAgent, "Refunded", new RefundBill(), webUser);

            if (newR.getCard() != 0 || newR.getCash() != 0 || newR.getCheque() != 0 || newR.getCredit() != 0 || newR.getSlip() != 0) {
                billls.add(newR);
            }

            uCash += (newC.getCash() + newR.getCash());

            //
            //Cash In
            BillsTotals newIn = createRowInOut(BillType.CashIn, "Billed", new BilledBill(), webUser);

            if (newIn.getCard() != 0 || newIn.getCash() != 0
                    || newIn.getCheque() != 0 || newIn.getCredit() != 0
                    || newIn.getSlip() != 0) {
                billls.add(newIn);
            }

            uCard += (newIn.getCard());
            uCash += (newIn.getCash());
            uCheque += (newIn.getCheque());
            uCredit += (newIn.getCredit());
            uSlip += (newIn.getSlip());

            BillsTotals newInCan = createRowInOut(BillType.CashIn, "Cancelled", new CancelledBill(), webUser);
            uCard += (newInCan.getCard());
            uCash += (newInCan.getCash());
            uCheque += (newInCan.getCheque());
            uCredit += (newInCan.getCredit());
            uSlip += (newInCan.getSlip());

            if (newInCan.getCard() != 0 || newInCan.getCash() != 0
                    || newInCan.getCheque() != 0 || newInCan.getCredit() != 0
                    || newInCan.getSlip() != 0) {
                billls.add(newInCan);
            }

            //Cash Out
            BillsTotals newOut = createRowInOut(BillType.CashOut, "Billed", new BilledBill(), webUser);

            if (newOut.getCard() != 0 || newOut.getCash() != 0 || newOut.getCheque() != 0 || newOut.getCredit() != 0 || newOut.getSlip() != 0) {
                billls.add(newOut);
            }

            uCard += (newOut.getCard());
            uCash += (newOut.getCash());
            uCheque += (newOut.getCheque());
            uCredit += (newOut.getCredit());
            uSlip += (newOut.getSlip());

            if (newOut.getCard() != 0 || newOut.getCash() != 0
                    || newOut.getCheque() != 0 || newOut.getCredit() != 0
                    || newOut.getSlip() != 0) {
                billls.add(newOut);
            }

            BillsTotals newOutCan = createRowInOut(BillType.CashOut, "Cancelled", new CancelledBill(), webUser);
            uCard += (newOutCan.getCard());
            uCash += (newOutCan.getCash());
            uCheque += (newOutCan.getCheque());
            uCredit += (newOutCan.getCredit());
            uSlip += (newOutCan.getSlip());

            if (newOutCan.getCard() != 0 || newOutCan.getCash() != 0
                    || newOutCan.getCheque() != 0 || newOutCan.getCredit() != 0
                    || newOutCan.getSlip() != 0) {
                billls.add(newOutCan);
            }

            //Adjustment
            BillsTotals adj = createRowInOut(BillType.DrawerAdjustment, "Adjusment", new BilledBill(), webUser);
            uCard += (adj.getCard());
            uCash += (adj.getCash());
            uCheque += (adj.getCheque());
            uCredit += (adj.getCredit());
            uSlip += (adj.getSlip());

            if (adj.getCard() != 0 || adj.getCash() != 0
                    || adj.getCheque() != 0 || adj.getCredit() != 0
                    || adj.getSlip() != 0) {
                billls.add(adj);
            }

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);
            Date endTime = new Date();
            duration = endTime.getTime() - startedTime.getTime();
            auditEvent.setEventDuration(duration);
            auditEvent.setEventStatus("Completed");
            auditEventApplicationController.logAuditEvent(auditEvent);

        }

        Date startTime = new Date();
        

    }

    public void calCashierDataCashier() {
        String ipAddress;
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startedTime = new Date();
        auditEvent.setEventDataTime(startedTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("calCashierDataCashier()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        header = "Cashier";
        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiersCashier()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypesCashier()) {
                BillsTotals newB = createRow(btp, "Billed", new BilledBill(), webUser);

                if (newB.getCard() != 0 || newB.getCash() != 0 || newB.getCheque() != 0 || newB.getCredit() != 0 || newB.getSlip() != 0) {
                    billls.add(newB);
                }

                BillsTotals newC = createRow(btp, "Cancelled", new CancelledBill(), webUser);

                if (newC.getCard() != 0 || newC.getCash() != 0 || newC.getCheque() != 0 || newC.getCredit() != 0 || newC.getSlip() != 0) {
                    billls.add(newC);
                }

                BillsTotals newR = createRow(btp, "Refunded", new RefundBill(), webUser);

                if (newR.getCard() != 0 || newR.getCash() != 0 || newR.getCheque() != 0 || newR.getCredit() != 0 || newR.getSlip() != 0) {
                    billls.add(newR);
                }

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }
            //channel agent bill cash cancel refund
//            BillsTotals newC = createRowAgent(BillType.ChannelAgent, "Cancelled", new CancelledBill(), webUser);
//
//            if (newC.getCard() != 0 || newC.getCash() != 0 || newC.getCheque() != 0 || newC.getCredit() != 0 || newC.getSlip() != 0) {
//                billls.add(newC);
//            }
//
//            BillsTotals newR = createRow(BillType.ChannelAgent, "Refunded", new RefundBill(), webUser);
//
//            if (newR.getCard() != 0 || newR.getCash() != 0 || newR.getCheque() != 0 || newR.getCredit() != 0 || newR.getSlip() != 0) {
//                billls.add(newR);
//            }
//
//            uCash += (newC.getCash() + newR.getCash());

            //
            //Cash In
            BillsTotals newIn = createRowInOut(BillType.CashIn, "Billed", new BilledBill(), webUser);

            if (newIn.getCard() != 0 || newIn.getCash() != 0
                    || newIn.getCheque() != 0 || newIn.getCredit() != 0
                    || newIn.getSlip() != 0) {
                billls.add(newIn);
            }

            uCard += (newIn.getCard());
            uCash += (newIn.getCash());
            uCheque += (newIn.getCheque());
            uCredit += (newIn.getCredit());
            uSlip += (newIn.getSlip());

            BillsTotals newInCan = createRowInOut(BillType.CashIn, "Cancelled", new CancelledBill(), webUser);
            uCard += (newInCan.getCard());
            uCash += (newInCan.getCash());
            uCheque += (newInCan.getCheque());
            uCredit += (newInCan.getCredit());
            uSlip += (newInCan.getSlip());

            if (newInCan.getCard() != 0 || newInCan.getCash() != 0
                    || newInCan.getCheque() != 0 || newInCan.getCredit() != 0
                    || newInCan.getSlip() != 0) {
                billls.add(newInCan);
            }

            //Cash Out
            BillsTotals newOut = createRowInOut(BillType.CashOut, "Billed", new BilledBill(), webUser);

            if (newOut.getCard() != 0 || newOut.getCash() != 0 || newOut.getCheque() != 0 || newOut.getCredit() != 0 || newOut.getSlip() != 0) {
                billls.add(newOut);
            }

            uCard += (newOut.getCard());
            uCash += (newOut.getCash());
            uCheque += (newOut.getCheque());
            uCredit += (newOut.getCredit());
            uSlip += (newOut.getSlip());

            if (newOut.getCard() != 0 || newOut.getCash() != 0
                    || newOut.getCheque() != 0 || newOut.getCredit() != 0
                    || newOut.getSlip() != 0) {
                billls.add(newOut);
            }

            BillsTotals newOutCan = createRowInOut(BillType.CashOut, "Cancelled", new CancelledBill(), webUser);
            uCard += (newOutCan.getCard());
            uCash += (newOutCan.getCash());
            uCheque += (newOutCan.getCheque());
            uCredit += (newOutCan.getCredit());
            uSlip += (newOutCan.getSlip());

            if (newOutCan.getCard() != 0 || newOutCan.getCash() != 0
                    || newOutCan.getCheque() != 0 || newOutCan.getCredit() != 0
                    || newOutCan.getSlip() != 0) {
                billls.add(newOutCan);
            }

            //Adjustment
            BillsTotals adj = createRowInOut(BillType.DrawerAdjustment, "Adjusment", new BilledBill(), webUser);
            uCard += (adj.getCard());
            uCash += (adj.getCash());
            uCheque += (adj.getCheque());
            uCredit += (adj.getCredit());
            uSlip += (adj.getSlip());

            if (adj.getCard() != 0 || adj.getCash() != 0
                    || adj.getCheque() != 0 || adj.getCredit() != 0
                    || adj.getSlip() != 0) {
                billls.add(adj);
            }

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }

        Date startTime = new Date();
        
        Date endTime = new Date();
        duration = endTime.getTime() - startedTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void calCashierDataUsingReciptNo() {
        Date startTime = new Date();
        fromDate = null;
        toDate = null;
        if (fromReciptNo == null) {
            JsfUtil.addErrorMessage("Please Enter Check Bill No");
            return;
        }
        fromDate = commonReport.fetchDate(fromReciptNo);
        if (fromDate == null) {
            JsfUtil.addErrorMessage("Please Enter Correct From Bill No");
            return;
        }
        if (toReciptNo == null) {
            JsfUtil.addErrorMessage("Please Enter Check Bill No");
            return;
        }
        toDate = commonReport.fetchDate(toReciptNo);
        if (toDate == null) {
            JsfUtil.addErrorMessage("Please Enter Correct To Bill No");
            return;
        }
        calCashierData();

        
    }

    public void calculateCashierSummeryTotals() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("calculateCashierSummeryTotals()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiers()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypes()) {
                BillsTotals newB = calculateBillTotalsByPaymentMethod(btp, "Billed", new BilledBill(), webUser);
                BillsTotals newC = calculateBillTotalsByPaymentMethod(btp, "Cancelled", new CancelledBill(), webUser);
                BillsTotals newR = calculateBillTotalsByPaymentMethod(btp, "Refunded", new RefundBill(), webUser);

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
    }

    public void calCashierDataTotalOnly() {
        Date startTime = new Date();
        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiers()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypes()) {
                BillsTotals newB = calculateBillTotalsByPaymentMethod(btp, "Billed", new BilledBill(), webUser);
                BillsTotals newC = calculateBillTotalsByPaymentMethod(btp, "Cancelled", new CancelledBill(), webUser);
                BillsTotals newR = calculateBillTotalsByPaymentMethod(btp, "Refunded", new RefundBill(), webUser);

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }

            //Cash IN
            BillsTotals newIn = createRowInOut(BillType.CashIn, "Billed", new BilledBill(), webUser);
            uCard += (newIn.getCard());
            uCash += (newIn.getCash());
            uCheque += (newIn.getCheque());
            uCredit += (newIn.getCredit());
            uSlip += (newIn.getSlip());

            BillsTotals newInCan = createRowInOut(BillType.CashIn, "Cancelled", new CancelledBill(), webUser);
            uCard += (newInCan.getCard());
            uCash += (newInCan.getCash());
            uCheque += (newInCan.getCheque());
            uCredit += (newInCan.getCredit());
            uSlip += (newInCan.getSlip());

            //Cahs Out
            BillsTotals newOut = createRowInOut(BillType.CashOut, "Billed", new BilledBill(), webUser);
            uCard += (newOut.getCard());
            uCash += (newOut.getCash());
            uCheque += (newOut.getCheque());
            uCredit += (newOut.getCredit());
            uSlip += (newOut.getSlip());

            BillsTotals newOutCan = createRowInOut(BillType.CashOut, "Cancelled", new CancelledBill(), webUser);
            uCard += (newOutCan.getCard());
            uCash += (newOutCan.getCash());
            uCheque += (newOutCan.getCheque());
            uCredit += (newOutCan.getCredit());
            uSlip += (newOutCan.getSlip());

            //Drawer Adjustment
            BillsTotals adj = createRowInOut(BillType.DrawerAdjustment, "Adjusment", new BilledBill(), webUser);
            uCard += (adj.getCard());
            uCash += (adj.getCash());
            uCheque += (adj.getCheque());
            uCredit += (adj.getCredit());
            uSlip += (adj.getSlip());

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }
        

    }

    public void calCashierDataTotalOnlyChannel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("calCashierDataTotalOnlyChannel()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        header = "Channel";
        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiersChannel()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypesChannel()) {
                BillsTotals newB = createRow(btp, "Billed", new BilledBill(), webUser);
                BillsTotals newC = createRow(btp, "Cancelled", new CancelledBill(), webUser);
                BillsTotals newR = createRow(btp, "Refunded", new RefundBill(), webUser);

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }

            //channel agent bill cash cancel refund
            BillsTotals newC = createRow(BillType.ChannelAgent, "Cancelled", new CancelledBill(), webUser);
            BillsTotals newR = createRow(BillType.ChannelAgent, "Refunded", new RefundBill(), webUser);

            uCard += (newC.getCard() + newR.getCard());
            uCash += (newC.getCash() + newR.getCash());
            uCheque += (newC.getCheque() + newR.getCheque());
            uCredit += (newC.getCredit() + newR.getCredit());
            uSlip += (newC.getSlip() + newR.getSlip());
            //channel agent bill cash cancel refund

            //Cash IN
            BillsTotals newIn = createRowInOut(BillType.CashIn, "Billed", new BilledBill(), webUser);
            uCard += (newIn.getCard());
            uCash += (newIn.getCash());
            uCheque += (newIn.getCheque());
            uCredit += (newIn.getCredit());
            uSlip += (newIn.getSlip());

            BillsTotals newInCan = createRowInOut(BillType.CashIn, "Cancelled", new CancelledBill(), webUser);
            uCard += (newInCan.getCard());
            uCash += (newInCan.getCash());
            uCheque += (newInCan.getCheque());
            uCredit += (newInCan.getCredit());
            uSlip += (newInCan.getSlip());

            //Cahs Out
            BillsTotals newOut = createRowInOut(BillType.CashOut, "Billed", new BilledBill(), webUser);
            uCard += (newOut.getCard());
            uCash += (newOut.getCash());
            uCheque += (newOut.getCheque());
            uCredit += (newOut.getCredit());
            uSlip += (newOut.getSlip());

            BillsTotals newOutCan = createRowInOut(BillType.CashOut, "Cancelled", new CancelledBill(), webUser);
            uCard += (newOutCan.getCard());
            uCash += (newOutCan.getCash());
            uCheque += (newOutCan.getCheque());
            uCredit += (newOutCan.getCredit());
            uSlip += (newOutCan.getSlip());

            //Drawer Adjustment
            BillsTotals adj = createRowInOut(BillType.DrawerAdjustment, "Adjusment", new BilledBill(), webUser);
            uCard += (adj.getCard());
            uCash += (adj.getCash());
            uCheque += (adj.getCheque());
            uCredit += (adj.getCredit());
            uSlip += (adj.getSlip());

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void calCashierDataTotalOnlyCashier() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("calCashierDataTotalOnlyCashier()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        header = "Cashier";
        startTime = new Date();
        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiersCashier()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypesCashier()) {
                BillsTotals newB = createRow(btp, "Billed", new BilledBill(), webUser);
                BillsTotals newC = createRow(btp, "Cancelled", new CancelledBill(), webUser);
                BillsTotals newR = createRow(btp, "Refunded", new RefundBill(), webUser);

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }

            //Cash IN
            BillsTotals newIn = createRowInOut(BillType.CashIn, "Billed", new BilledBill(), webUser);
            uCard += (newIn.getCard());
            uCash += (newIn.getCash());
            uCheque += (newIn.getCheque());
            uCredit += (newIn.getCredit());
            uSlip += (newIn.getSlip());

            BillsTotals newInCan = createRowInOut(BillType.CashIn, "Cancelled", new CancelledBill(), webUser);
            uCard += (newInCan.getCard());
            uCash += (newInCan.getCash());
            uCheque += (newInCan.getCheque());
            uCredit += (newInCan.getCredit());
            uSlip += (newInCan.getSlip());

            //Cahs Out
            BillsTotals newOut = createRowInOut(BillType.CashOut, "Billed", new BilledBill(), webUser);
            uCard += (newOut.getCard());
            uCash += (newOut.getCash());
            uCheque += (newOut.getCheque());
            uCredit += (newOut.getCredit());
            uSlip += (newOut.getSlip());

            BillsTotals newOutCan = createRowInOut(BillType.CashOut, "Cancelled", new CancelledBill(), webUser);
            uCard += (newOutCan.getCard());
            uCash += (newOutCan.getCash());
            uCheque += (newOutCan.getCheque());
            uCredit += (newOutCan.getCredit());
            uSlip += (newOutCan.getSlip());

            //Drawer Adjustment
            BillsTotals adj = createRowInOut(BillType.DrawerAdjustment, "Adjusment", new BilledBill(), webUser);
            uCard += (adj.getCard());
            uCash += (adj.getCash());
            uCheque += (adj.getCheque());
            uCredit += (adj.getCredit());
            uSlip += (adj.getSlip());

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
    }

    public void calCashierDataTotalOnlyUsingReciptNo() {
        fromDate = null;
        toDate = null;
        if (fromReciptNo == null) {
            JsfUtil.addErrorMessage("Please Enter Check Bill No");
            return;
        }
        fromDate = commonReport.fetchDate(fromReciptNo);
        if (fromDate == null) {
            JsfUtil.addErrorMessage("Please Enter Correct From Bill No");
            return;
        }
        if (toReciptNo == null) {
            JsfUtil.addErrorMessage("Please Enter Check Bill No");
            return;
        }
        toDate = commonReport.fetchDate(toReciptNo);
        if (toDate == null) {
            JsfUtil.addErrorMessage("Please Enter Correct To Bill No");
            return;
        }
        calCashierDataTotalOnly();
    }

    public void calCashierDataTotalOnlyWithoutPro() {
        Date startTime = new Date();
        finalCashTot = finalChequeTot = finalCardTot = finalCreditTot = finalSlipTot = 0;
        webUserBillsTotals = new ArrayList<>();
        for (WebUser webUser : getCashiersWithoutPro()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(webUser);
            List<BillsTotals> billls = new ArrayList<>();

            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getEnumController().getCashFlowBillTypes()) {
                BillsTotals newB = createRowWithoutPro(btp, "Billed", new BilledBill(), webUser);
                BillsTotals newC = createRowWithoutPro(btp, "Cancelled", new CancelledBill(), webUser);
                BillsTotals newR = createRowWithoutPro(btp, "Refunded", new RefundBill(), webUser);

                uCard += (newB.getCard() + newC.getCard() + newR.getCard());
                uCash += (newB.getCash() + newC.getCash() + newR.getCash());
                uCheque += (newB.getCheque() + newC.getCheque() + newR.getCheque());
                uCredit += (newB.getCredit() + newC.getCredit() + newR.getCredit());
                uSlip += (newB.getSlip() + newC.getSlip() + newR.getSlip());

            }

            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }
            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);

        }

        

    }

    private List<WebUserBillsTotal> webUserBillsTotals;

    private List<Object> userBillTotalsWithPaymentMethods(WebUser w, Bill billClass, BillType billType) {
        String sql;
        Map temMap = new HashMap();
        sql = "select new com.divudi.data.PaymentMethodValue(b.paymentMethod, sum(b.netTotal+b.vat))"
                + " from Bill b "
                + " where type(b)=:bill "
                + " and b.creater=:cret "
                + " and b.institution=:ins"
                + " and b.retired=false "
                + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate "
                + " group by b.paymentMethod";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillFacade().findObjectByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

//  New One  userBillTotalsWithPaymentMethods
    @Deprecated
    private double calTotalValueOwn(WebUser w, Bill billClass, PaymentMethod pM, BillType billType) {
////        int day= Calendar.HOUR_OF_DAY(getToDate())- Calendar.DATE(getFromDate()) ;
//        Date a;
//        a = Calendar.Date.getToDate()-Date.getFromDate();
//        int day2;
//        day2 = Calendar.DAY_OF_YEAR(getToDate());
//        if(day2>=2){
//                    
//            JsfUtil.addErrorMessage("Please Enter Blow 2 Days");
//            return 0;
//        }
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal+b.vat) from Bill b where type(b)=:bill and b.creater=:cret and "
                + " b.paymentMethod= :payMethod  and b.institution=:ins"
                + " and b.retired=false "
                + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("payMethod", pM);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calTotalValueForLoggedDepartment(WebUser w, Bill billClass, PaymentMethod pM, BillType billType) {
////        int day= Calendar.HOUR_OF_DAY(getToDate())- Calendar.DATE(getFromDate()) ;
//        Date a;
//        a = Calendar.Date.getToDate()-Date.getFromDate();
//        int day2;
//        day2 = Calendar.DAY_OF_YEAR(getToDate());
//        if(day2>=2){
//                    
//            JsfUtil.addErrorMessage("Please Enter Blow 2 Days");
//            return 0;
//        }
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal+b.vat) from Bill b where type(b)=:bill and b.creater=:cret and "
                + " b.paymentMethod= :payMethod  and b.department=:dep"
                + " and b.retired=false "
                + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("payMethod", pM);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("dep", getSessionController().getDepartment());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calTotalValueOwnWithoutPro(WebUser w, Bill billClass, PaymentMethod pM, BillType billType) {
////        int day= Calendar.HOUR_OF_DAY(getToDate())- Calendar.DATE(getFromDate()) ;
//        Date a;
//        a = Calendar.Date.getToDate()-Date.getFromDate();
//        int day2;
//        day2 = Calendar.DAY_OF_YEAR(getToDate());
//        if(day2>=2){
//                    
//            JsfUtil.addErrorMessage("Please Enter Blow 2 Days");
//            return 0;
//        }
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal-b.staffFee) from Bill b where type(b)=:bill and b.creater=:cret and "
                + " b.paymentMethod= :payMethod  and b.institution=:ins"
                + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("payMethod", pM);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calTotalValueOwnInOutCash(WebUser w, Bill billClass, BillType billType) {

        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.cashTransaction.cashValue) from Bill b"
                + "  where type(b)=:bill "
                + " and b.creater=:cret "
                + " and b.institution=:ins"
                + " and b.billType= :billTp "
                + "and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        double dbl = getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return dbl;

    }

    private double calTotalValueOwnInOutCreditCard(WebUser w, Bill billClass, BillType billType) {

        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.cashTransaction.creditCardValue) from Bill b"
                + "  where type(b)=:bill "
                + " and b.creater=:cret "
                + " and b.institution=:ins"
                + " and b.billType= :billTp "
                + "and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calTotalValueOwnInOutCheque(WebUser w, Bill billClass, BillType billType) {

        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.cashTransaction.chequeValue) from Bill b"
                + "  where type(b)=:bill "
                + " and b.creater=:cret "
                + " and b.institution=:ins"
                + " and b.billType= :billTp "
                + "and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calTotalValueOwnInOutSlip(WebUser w, Bill billClass, BillType billType) {

        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.cashTransaction.slipValue) from Bill b"
                + "  where type(b)=:bill "
                + " and b.creater=:cret "
                + " and b.institution=:ins"
                + " and b.billType= :billTp "
                + "and b.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private void setDataTable(CashierSummeryData c) {
        List<String1Value5> dataTable5Values = new ArrayList<>();

        if (c.getBilledCash() != 0 || c.getBilledCheque() != 0 || c.getBilledCredit() != 0
                || c.getBilledCreditCard() != 0 || c.getBilledSlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Billed");
            tmp.setValue1(c.getBilledCash());
            tmp.setValue2(c.getBilledCredit());
            tmp.setValue3(c.getBilledCreditCard());
            tmp.setValue4(c.getBilledCheque());
            tmp.setValue5(c.getBilledSlip());
            dataTable5Values.add(tmp);
        }

        if (c.getCancelledCash() != 0 || c.getCancelledCheque() != 0 || c.getCancelledCredit() != 0
                || c.getCancelledCreditCard() != 0 || c.getCancelledSlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Cancelled");
            tmp.setValue1(c.getCancelledCash());
            tmp.setValue2(c.getCancelledCredit());
            tmp.setValue3(c.getCancelledCreditCard());
            tmp.setValue4(c.getCancelledCheque());
            tmp.setValue5(c.getCancelledSlip());
            dataTable5Values.add(tmp);
        }

        if (c.getRefundCheque() != 0 || c.getRefundSlip() != 0 || c.getRefundedCash() != 0
                || c.getRefundedCredit() != 0 || c.getRefundedCreditCard() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Refunded");
            tmp.setValue1(c.getRefundedCash());
            tmp.setValue2(c.getRefundedCredit());
            tmp.setValue3(c.getRefundedCreditCard());
            tmp.setValue4(c.getRefundCheque());
            tmp.setValue5(c.getRefundSlip());
            dataTable5Values.add(tmp);
        }

        if (c.getPaymentCash() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Payment");
            tmp.setValue1(c.getPaymentCash());
            dataTable5Values.add(tmp);
        }

        if (c.getPaymentCashCancel() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Payment Cancel");
            tmp.setValue1(c.getPaymentCashCancel());
            dataTable5Values.add(tmp);
        }

        if (c.getPettyCash() != 0 || c.getPettyCheque() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Petty Cash");
            tmp.setValue1(c.getPettyCash());
            tmp.setValue4(c.getPettyCheque());
            dataTable5Values.add(tmp);
        }

        if (c.getPettyCancelCash() != 0 || c.getPettyCancelCheque() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Petty Cash Cancel");
            tmp.setValue1(c.getPettyCancelCash());
            tmp.setValue4(c.getPettyCancelCheque());
            dataTable5Values.add(tmp);
        }

        if (c.getCompanyCash() != 0 || c.getCompanyCheque() != 0 || c.getCompanySlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Credit Company Payment");
            tmp.setValue1(c.getCompanyCash());
            tmp.setValue4(c.getCompanyCheque());
            tmp.setValue5(c.getCompanySlip());
            dataTable5Values.add(tmp);
        }

        if (c.getCompanyCancelCash() != 0 || c.getCompanyCancelCheque() != 0 || c.getCompanyCancelSlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Credit Company Payment Cancel");
            tmp.setValue1(c.getCompanyCancelCash());
            tmp.setValue4(c.getCompanyCancelCheque());
            tmp.setValue5(c.getCompanyCancelSlip());
            dataTable5Values.add(tmp);
        }

        if (c.getAgentCash() != 0 || c.getAgentCheque() != 0 || c.getAgentSlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Agent Payment");
            tmp.setValue1(c.getAgentCash());
            tmp.setValue4(c.getAgentCheque());
            tmp.setValue5(c.getAgentSlip());
            dataTable5Values.add(tmp);
        }

        if (c.getAgentCancelCash() != 0 || c.getAgentCancelCheque() != 0 || c.getAgentCancelSlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Agent Payment Cancel");
            tmp.setValue1(c.getAgentCancelCash());
            tmp.setValue4(c.getAgentCancelCheque());
            tmp.setValue5(c.getAgentCancelSlip());
            dataTable5Values.add(tmp);
        }

        if (c.getInwardPaymentCash() != 0 || c.getInwardPaymentCheque() != 0 || c.getInwardPaymentSlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Inward Payment");
            tmp.setValue1(c.getInwardPaymentCash());
            tmp.setValue4(c.getInwardPaymentCheque());
            tmp.setValue5(c.getInwardPaymentSlip());
            dataTable5Values.add(tmp);
        }

        if (c.getInwardCancelCash() != 0 || c.getInwardCancelCheque() != 0 || c.getInwardCancelSlip() != 0) {
            String1Value5 tmp = new String1Value5();
            tmp.setString("Inward Payment Cancel");
            tmp.setValue1(c.getInwardCancelCash());
            tmp.setValue4(c.getInwardCancelCheque());
            tmp.setValue5(c.getInwardCancelSlip());
            dataTable5Values.add(tmp);
        }

        String1Value5 tmp = new String1Value5();
        tmp.setString("Net Total");
        tmp.setValue1(c.getNetCash());
        tmp.setValue2(c.getNetCredit());
        tmp.setValue3(c.getNetCreditCard());
        tmp.setValue4(c.getNetCheque());
        tmp.setValue5(c.getNetSlip());
        dataTable5Values.add(tmp);

        c.setDataTable5Value(dataTable5Values);
    }
    private double finalSlipTot = 0.0;

    public double getFinalChequeTot() {

        return finalChequeTot;
    }

    public double getFinalChequeTotOwn() {
        finalChequeTot = 0.0;
        for (CashierSummeryData s : getCashierDatasOwn()) {
            finalChequeTot += s.getNetCheque();
        }
        return finalChequeTot;
    }

    void findSummeryOwn(CashierSummeryData c, WebUser w) {
        c.setBilledCash(calTotOwn(w, new BilledBill(), PaymentMethod.Cash, BillType.OpdBill));
        c.setBilledCredit(calTotOwn(w, new BilledBill(), PaymentMethod.Credit, BillType.OpdBill));
        c.setBilledCreditCard(calTotOwn(w, new BilledBill(), PaymentMethod.Card, BillType.OpdBill));
        c.setBilledCheque(calTotOwn(w, new BilledBill(), PaymentMethod.Cheque, BillType.OpdBill));
        c.setBilledSlip(calTotOwn(w, new BilledBill(), PaymentMethod.Slip, BillType.OpdBill));

        c.setCancelledCash(calTotOwn(w, new CancelledBill(), PaymentMethod.Cash, BillType.OpdBill));
        c.setCancelledCredit(calTotOwn(w, new CancelledBill(), PaymentMethod.Credit, BillType.OpdBill));
        c.setCancelledCreditCard(calTotOwn(w, new CancelledBill(), PaymentMethod.Card, BillType.OpdBill));
        c.setCancelledCheque(calTotOwn(w, new CancelledBill(), PaymentMethod.Cheque, BillType.OpdBill));
        c.setCancelledSlip(calTotOwn(w, new CancelledBill(), PaymentMethod.Slip, BillType.OpdBill));

        c.setRefundedCash(calTotOwn(w, new RefundBill(), PaymentMethod.Cash, BillType.OpdBill));
        c.setRefundedCredit(calTotOwn(w, new RefundBill(), PaymentMethod.Credit, BillType.OpdBill));
        c.setRefundedCreditCard(calTotOwn(w, new RefundBill(), PaymentMethod.Card, BillType.OpdBill));
        c.setRefundCheque(calTotOwn(w, new RefundBill(), PaymentMethod.Cheque, BillType.OpdBill));
        c.setRefundSlip(calTotOwn(w, new RefundBill(), PaymentMethod.Slip, BillType.OpdBill));

        c.setPaymentCash(calTotOwn(w, new BilledBill(), PaymentMethod.Cash, BillType.PaymentBill));
        c.setPaymentCashCancel(calTotOwn(w, new CancelledBill(), PaymentMethod.Cash, BillType.PaymentBill));

        c.setPettyCash(calTotOwn(w, new BilledBill(), PaymentMethod.Cash, BillType.PettyCash));
        c.setPettyCheque(calTotOwn(w, new CancelledBill(), PaymentMethod.Cheque, BillType.PettyCash));

        c.setPettyCancelCash(calTotOwn(w, new CancelledBill(), PaymentMethod.Cash, BillType.PettyCash));
        c.setPettyCancelCheque(calTotOwn(w, new CancelledBill(), PaymentMethod.Cheque, BillType.PettyCash));

        c.setCompanyCash(calTotOwn(w, new BilledBill(), PaymentMethod.Cash, BillType.CashRecieveBill));
        c.setCompanyCheque(calTotOwn(w, new BilledBill(), PaymentMethod.Cheque, BillType.CashRecieveBill));
        c.setCompanySlip(calTotOwn(w, new BilledBill(), PaymentMethod.Slip, BillType.CashRecieveBill));

        c.setCompanyCancelCash(calTotOwn(w, new CancelledBill(), PaymentMethod.Cash, BillType.CashRecieveBill));
        c.setCompanyCancelCheque(calTotOwn(w, new CancelledBill(), PaymentMethod.Cheque, BillType.CashRecieveBill));
        c.setCompanyCancelSlip(calTotOwn(w, new CancelledBill(), PaymentMethod.Slip, BillType.CashRecieveBill));

        c.setAgentCash(calTotOwn(w, new BilledBill(), PaymentMethod.Cash, BillType.AgentPaymentReceiveBill));
        c.setAgentCheque(calTotOwn(w, new BilledBill(), PaymentMethod.Cheque, BillType.AgentPaymentReceiveBill));
        c.setAgentSlip(calTotOwn(w, new BilledBill(), PaymentMethod.Slip, BillType.AgentPaymentReceiveBill));

        c.setAgentCancelCash(calTotOwn(w, new CancelledBill(), PaymentMethod.Cash, BillType.AgentPaymentReceiveBill));
        c.setAgentCancelCheque(calTotOwn(w, new CancelledBill(), PaymentMethod.Cheque, BillType.AgentPaymentReceiveBill));
        c.setAgentCancelSlip(calTotOwn(w, new CancelledBill(), PaymentMethod.Slip, BillType.AgentPaymentReceiveBill));

        c.setInwardPaymentCash(calTotOwn(w, new BilledBill(), PaymentMethod.Cash, BillType.InwardPaymentBill));
        c.setInwardPaymentCheque(calTotOwn(w, new BilledBill(), PaymentMethod.Cheque, BillType.InwardPaymentBill));
        c.setInwardPaymentSlip(calTotOwn(w, new BilledBill(), PaymentMethod.Slip, BillType.InwardPaymentBill));

        c.setInwardCancelCash(calTotOwn(w, new CancelledBill(), PaymentMethod.Cash, BillType.InwardPaymentBill));
        c.setInwardCancelCheque(calTotOwn(w, new CancelledBill(), PaymentMethod.Cheque, BillType.InwardPaymentBill));
        c.setInwardCancelSlip(calTotOwn(w, new CancelledBill(), PaymentMethod.Slip, BillType.InwardPaymentBill));
    }

    double calTotOwn(WebUser w, Bill billClass, PaymentMethod pM, BillType billType) {
        String sql;
        Map temMap = new HashMap();

        if (getSessionController().getInstitution() == null) {
            return 0.0;
        }

        if (billType == BillType.InwardPaymentBill) {
            sql = "select b from Bill b where type(b)=:bill and b.creater=:cret and "
                    + " b.paymentMethod=:payMethod  and b.institution=:ins"
                    + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate ";
        } else {
            sql = "select b from Bill b where type(b)=:bill and b.creater=:cret and "
                    + " b.paymentMethod= :payMethod  and b.institution=:ins"
                    + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("payMethod", pM);
        temMap.put("bill", billClass.getClass());
        temMap.put("cret", w);
        temMap.put("ins", getSessionController().getInstitution());

        List<Bill> bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        double tot = 0;
        for (Bill b : bills) {
            tot += b.getNetTotal();
        }
        return tot;

    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunction().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        recreteModal();

    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunction().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        recreteModal();
    }

    public CommonFunctions getCommonFunction() {
        return commonFunction;
    }

    public void setCommonFunction(CommonFunctions commonFunction) {
        this.commonFunction = commonFunction;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public List<WebUser> getCashiers() {
        String sql;
        Map temMap = new HashMap();
        BillType[] btpArr = enumController.getCashFlowBillTypes();
        List<BillType> btpList = Arrays.asList(btpArr);
        sql = "select DISTINCT(b.creater) from "
                + " Bill b "
                + " where b.institution=:ins "
                + " and b.billType in :btp "
                + " and b.createdAt between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", btpList);
        temMap.put("ins", sessionController.getInstitution());
        cashiers = getWebUserFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    public List<WebUser> getCashiersCashier() {
        String sql;
        Map temMap = new HashMap();
        BillType[] btpArr = enumController.getCashFlowBillTypesCashier();
        List<BillType> btpList = Arrays.asList(btpArr);
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
        cashiers = getWebUserFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    public List<WebUser> getCashiersChannel() {
        String sql;
        Map temMap = new HashMap();
        BillType[] btpArr = enumController.getCashFlowBillTypesChannel();
        List<BillType> btpListTmp = Arrays.asList(btpArr);
        List<BillType> btpList = new ArrayList<>();
        btpList.addAll(btpListTmp);
        btpList.add(BillType.ChannelAgent);//Get Only Agency cash Cancel users
        sql = "select us from "
                + " Bill b "
                + " join b.creater us "
                + " where b.retired=false "
                + " and b.institution=:ins "
                + " and b.paymentMethod!=:pm "
                + " and b.billType in :btp "
                + " and b.createdAt between :fromDate and :toDate "
                + " group by us "
                + " having sum(b.netTotal+b.vat)!=0 ";
        temMap.put("pm", PaymentMethod.Agent);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", btpList);
        temMap.put("ins", sessionController.getInstitution());
        cashiers = getWebUserFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    public List<WebUser> getCashiersWithoutPro() {
        String sql;
        Map temMap = new HashMap();
        BillType[] btpArr = enumController.getCashFlowBillTypes();
        List<BillType> btpList = Arrays.asList(btpArr);
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
        cashiers = getWebUserFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    public void setCashiers(List<WebUser> cashiers) {
        this.cashiers = cashiers;

    }

    public List<CashierSummeryData> getCashierDatas() {
        cashierDatas = new ArrayList<>();
        for (WebUser w : getCashiers()) {
            CashierSummeryData temp = new CashierSummeryData();
            temp.setCasheir(w);
            findSummery(temp, w);
            setDataTable(temp);
            cashierDatas.add(temp);

        }

        return cashierDatas;
    }

    void findSummery(CashierSummeryData c, WebUser w) {
        c.setBilledCash(calTot(w, new BilledBill(), PaymentMethod.Cash, BillType.OpdBill));
        c.setBilledCredit(calTot(w, new BilledBill(), PaymentMethod.Credit, BillType.OpdBill));
        c.setBilledCreditCard(calTot(w, new BilledBill(), PaymentMethod.Card, BillType.OpdBill));
        c.setBilledCheque(calTot(w, new BilledBill(), PaymentMethod.Cheque, BillType.OpdBill));
        c.setBilledSlip(calTot(w, new BilledBill(), PaymentMethod.Slip, BillType.OpdBill));

        c.setCancelledCash(calTot(w, new CancelledBill(), PaymentMethod.Cash, BillType.OpdBill));
        c.setCancelledCredit(calTot(w, new CancelledBill(), PaymentMethod.Credit, BillType.OpdBill));
        c.setCancelledCreditCard(calTot(w, new CancelledBill(), PaymentMethod.Card, BillType.OpdBill));
        c.setCancelledCheque(calTot(w, new CancelledBill(), PaymentMethod.Cheque, BillType.OpdBill));
        c.setCancelledSlip(calTot(w, new CancelledBill(), PaymentMethod.Slip, BillType.OpdBill));

        c.setRefundedCash(calTot(w, new RefundBill(), PaymentMethod.Cash, BillType.OpdBill));
        c.setRefundedCredit(calTot(w, new RefundBill(), PaymentMethod.Credit, BillType.OpdBill));
        c.setRefundedCreditCard(calTot(w, new RefundBill(), PaymentMethod.Card, BillType.OpdBill));
        c.setRefundCheque(calTot(w, new RefundBill(), PaymentMethod.Cheque, BillType.OpdBill));
        c.setRefundSlip(calTot(w, new RefundBill(), PaymentMethod.Slip, BillType.OpdBill));

        c.setPaymentCash(calTot(w, new BilledBill(), PaymentMethod.Cash, BillType.PaymentBill));
        c.setPaymentCashCancel(calTot(w, new CancelledBill(), PaymentMethod.Cash, BillType.PaymentBill));

        c.setPettyCash(calTot(w, new BilledBill(), PaymentMethod.Cash, BillType.PettyCash));
        c.setPettyCheque(calTot(w, new BilledBill(), PaymentMethod.Cheque, BillType.PettyCash));

        c.setPettyCancelCash(calTot(w, new CancelledBill(), PaymentMethod.Cash, BillType.PettyCash));
        c.setPettyCancelCheque(calTot(w, new CancelledBill(), PaymentMethod.Cheque, BillType.PettyCash));

        c.setCompanyCash(calTot(w, new BilledBill(), PaymentMethod.Cash, BillType.CashRecieveBill));
        c.setCompanyCheque(calTot(w, new BilledBill(), PaymentMethod.Cheque, BillType.CashRecieveBill));
        c.setCompanySlip(calTot(w, new BilledBill(), PaymentMethod.Slip, BillType.CashRecieveBill));

        c.setCompanyCancelCash(calTot(w, new CancelledBill(), PaymentMethod.Cash, BillType.CashRecieveBill));
        c.setCompanyCancelCheque(calTot(w, new CancelledBill(), PaymentMethod.Cheque, BillType.CashRecieveBill));
        c.setCompanyCancelSlip(calTot(w, new CancelledBill(), PaymentMethod.Slip, BillType.CashRecieveBill));

        c.setAgentCash(calTot(w, new BilledBill(), PaymentMethod.Cash, BillType.AgentPaymentReceiveBill));
        c.setAgentCheque(calTot(w, new BilledBill(), PaymentMethod.Cheque, BillType.AgentPaymentReceiveBill));
        c.setAgentSlip(calTot(w, new BilledBill(), PaymentMethod.Slip, BillType.AgentPaymentReceiveBill));

        c.setAgentCancelCash(calTot(w, new CancelledBill(), PaymentMethod.Cash, BillType.AgentPaymentReceiveBill));
        c.setAgentCancelCheque(calTot(w, new CancelledBill(), PaymentMethod.Cheque, BillType.AgentPaymentReceiveBill));
        c.setAgentCancelSlip(calTot(w, new CancelledBill(), PaymentMethod.Slip, BillType.AgentPaymentReceiveBill));

    }

    double calTot(WebUser w, Bill bill, PaymentMethod paymentMethod, BillType billType) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b  where type(b)=:bill and b.creater=:web and "
                + "b.paymentMethod= :pm "
                + " and b.billType= :billTp and b.createdAt between :fromDate and :toDate";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billTp", billType);
        temMap.put("pm", paymentMethod);
        temMap.put("bill", bill.getClass());
        temMap.put("web", w);
        List<Bill> bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        double tot = 0;
        for (Bill b : bills) {
            tot += b.getNetTotal();
        }
        return tot;
    }

    public void setCashierDatas(List<CashierSummeryData> cashierDatas) {
        this.cashierDatas = cashierDatas;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public double getFinalCashTot() {
        return finalCashTot;
    }

    public double getFinalCashTotOwn() {
        finalCashTot = 0.0;
        for (CashierSummeryData s : getCashierDatasOwn()) {
            finalCashTot += s.getNetCash();
        }
        return finalCashTot;
    }

    public void setFinalCashTot(double finalCashTot) {
        this.finalCashTot = finalCashTot;
    }

    public double getFinalCreditTot() {

        return finalCreditTot;
    }

    public double getFinalCreditTotOwn() {
        finalCreditTot = 0.0;
        for (CashierSummeryData s : getCashierDatasOwn()) {
            finalCreditTot += s.getNetCredit();
        }
        return finalCreditTot;
    }

    public void setFinalCreditTot(double finalCreditTot) {
        this.finalCreditTot = finalCreditTot;
    }

    public double getFinalCardTot() {

        return finalCardTot;
    }

    public double getFinalCreditCardTotOwn() {
        finalCardTot = 0.0;
        for (CashierSummeryData s : getCashierDatasOwn()) {
            finalCardTot += s.getNetCreditCard();
        }

        return finalCardTot;
    }

    public void setFinalCardTot(double finalCardTot) {
        this.finalCardTot = finalCardTot;
    }

    public CashierSummeryData getCurrent() {
        if (current == null) {
            current = new CashierSummeryData();
        }

        return current;
    }

    public void setCurrent(CashierSummeryData current) {
        this.current = current;

    }

    public WebUser getCurrentCashier() {
        if (currentCashier == null) {
            currentCashier = new WebUser();
        }

        return currentCashier;

    }

    private void setCashierData() {
        current = null;

        getCurrent().setCasheir(getCurrentCashier());
        findSummery(getCurrent(), getCurrentCashier());

    }

    public void setCurrentCashier(WebUser currentCashier) {
        this.currentCashier = currentCashier;
        setCashierData();
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public double getFinalSlipTot() {

        return finalSlipTot;
    }

    public double getFinalSlipTotOwn() {
        finalSlipTot = 0.0;
        for (CashierSummeryData s : getCashierDatasOwn()) {
            finalSlipTot += s.getNetSlip();
        }
        return finalSlipTot;
    }

    public void setFinalSlipTot(double finalSlipTot) {
        this.finalSlipTot = finalSlipTot;
    }

    public List<String1Value1> getDataTableDatas() {
        dataTableDatas = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotOwn());

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Credit Card Total");
        tmp2.setValue(getFinalCreditCardTotOwn());

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cheque Total");
        tmp3.setValue(getFinalChequeTotOwn());

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Final Slip Total");
        tmp4.setValue(getFinalSlipTotOwn());

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotOwn());

        dataTableDatas.add(tmp1);
        dataTableDatas.add(tmp2);
        dataTableDatas.add(tmp3);
        dataTableDatas.add(tmp4);
        dataTableDatas.add(tmp5);

        return dataTableDatas;
    }

    public void excelLisner() {
        calCashierData();
        calCashierDataChannel();
        calCashierDataCashier();
    }

    public void downloadAsCashierSummeryExcel() throws ParseException {
        getWebUserBillsTotals();

        try {

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("All Cashier Summary");

            Font font = workbook.createFont();
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);

            CellStyle style = workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setFont(font);

            CellRangeAddress mergedInsCell = new CellRangeAddress(0, 0, 0, 6);
            sheet.addMergedRegion(mergedInsCell);

            Row ins = sheet.createRow(0);
            ins.createCell(0).setCellValue(sessionController.getLoggedUser().getInstitution().getName());
            ins.getCell(0).setCellStyle(style);

            CellRangeAddress mergedCellTitle = new CellRangeAddress(1, 1, 0, 6);
            sheet.addMergedRegion(mergedCellTitle);

            Row title = sheet.createRow(1);
            title.createCell(0).setCellValue("All Cashier Report - " + sessionController.getLoggedUser().getDepartment().getName());
            title.getCell(0).setCellStyle(style);

            CellRangeAddress mergedFromDateCell = new CellRangeAddress(2, 2, 0, 6);
            sheet.addMergedRegion(mergedFromDateCell);

            CellRangeAddress mergedToDateCell = new CellRangeAddress(3, 3, 0, 6);
            sheet.addMergedRegion(mergedToDateCell);

            DateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            DateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH);

            TimeZone timeZoneSL = TimeZone.getTimeZone("Asia/Colombo");
            inputFormat.setTimeZone(timeZoneSL);

            Row fDate = sheet.createRow(2);
            fDate.createCell(0).setCellValue("From Date -  " + outputFormat.format(inputFormat.parse(fromDate.toString())));
            
            
            Row tDate = sheet.createRow(3);
            tDate.createCell(0).setCellValue("To Date -  " + outputFormat.format(inputFormat.parse(toDate.toString())));

            Row headerRow = sheet.createRow(5);
            headerRow.createCell(0).setCellValue("User");
            headerRow.createCell(1).setCellValue("Bill type");
            headerRow.createCell(2).setCellValue("Cash");
            headerRow.createCell(3).setCellValue("Credit");
            headerRow.createCell(4).setCellValue("Credit Card");
            headerRow.createCell(5).setCellValue("Cheque");
            headerRow.createCell(6).setCellValue("Slip");
            // Add more columns as needed
            // Populate the data rows
            int rowNum = 6;

            for (WebUserBillsTotal userbill : webUserBillsTotals) {
                if (userbill.getBillsTotals().isEmpty()) {
                    continue;
                }
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(userbill.getWebUser().getName());

                for (BillsTotals bt : userbill.getBillsTotals()) {

                    row.createCell(1).setCellValue(bt.getName());
                    row.createCell(2).setCellValue(bt.getCash());
                    row.createCell(3).setCellValue(bt.getCredit());
                    row.createCell(4).setCellValue(bt.getCard());
                    row.createCell(5).setCellValue(bt.getCheque());
                    row.createCell(6).setCellValue(bt.getSlip());
                    row = sheet.createRow(rowNum++);
                }
            }

            // Set the response headers to initiate the download
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"all_cashier_summery.xlsx\"");

            // Write the workbook to the response output stream
            workbook.write(response.getOutputStream());
            workbook.close();
            context.responseComplete();

            JsfUtil.addSuccessMessage("Download Successfully");

        } catch (IOException e) {
            // Handle any exceptions
        }
    }

    public void setDataTableDatas(List<String1Value1> dataTableDatas) {
        this.dataTableDatas = dataTableDatas;
    }

    public List<WebUserBillsTotal> getWebUserBillsTotals() {
        return webUserBillsTotals;
    }

    public void setWebUserBillsTotals(List<WebUserBillsTotal> webUserBillsTotals) {
        this.webUserBillsTotals = webUserBillsTotals;
    }

    public EnumController getEnumController() {
        return enumController;
    }

    public void setEnumController(EnumController enumController) {
        this.enumController = enumController;
    }

    public String getFromReciptNo() {
        return fromReciptNo;
    }

    public void setFromReciptNo(String fromReciptNo) {
        this.fromReciptNo = fromReciptNo;
    }

    public String getToReciptNo() {
        return toReciptNo;
    }

    public void setToReciptNo(String toReciptNo) {
        this.toReciptNo = toReciptNo;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public int getReportCashierIndex() {
        return reportCashierIndex;
    }

    public void setReportCashierIndex(int reportCashierIndex) {
        this.reportCashierIndex = reportCashierIndex;
    }
}
