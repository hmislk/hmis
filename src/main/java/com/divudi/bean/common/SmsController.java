/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.MessageType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.Sms;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.util.JsfUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Dushan
 */
@Named
@SessionScoped
public class SmsController implements Serializable {

    /*
    EJB
     */
    @EJB
    SmsFacade smsFacade;
    @EJB
    private SmsManagerEjb smsManager;
    /*
    Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    CommonFunctions commonFunctions;
    /*
    Class Variables
     */
    List<Sms> smses;
    List<Sms> faildsms;
    private Sms selectedSms;
    private Boolean bool;

    public List<Sms> getFaildsms() {
        return faildsms;
    }

    public void setFaildsms(List<Sms> faildsms) {
        this.faildsms = faildsms;
    }
    List<SmsSummeryRow> smsSummeryRows;
    ReportKeyWord reportKeyWord;
    private String number;
    private String message;
    private String messageOutput;
    private Date fromDate;
    private Date toDate;

    /**
     * Creates a new instance of SmsController
     */
    public SmsController() {
    }

    public void sendSmsAwaitingToSendInDatabase() {
        String j = "Select e from Sms e where e.sentSuccessfully=false and e.retired=false";
        List<Sms> smses = getSmsFacade().findByJpql(j);
        for (Sms e : smses) {
            e.setSentSuccessfully(Boolean.TRUE);
            getSmsFacade().edit(e);
            boolean sentSuccessfully = smsManager.sendSmsByApplicationPreference(e.getReceipientNumber(), e.getSendingMessage(), sessionController.getApplicationPreference());
            e.setSentSuccessfully(sentSuccessfully);
            e.setSentAt(new Date());
            getSmsFacade().edit(e);
        }
    }

    public boolean sendSms(String number, String message, String username, String password, String sendingAlias) {
        return smsManager.sendSmsByApplicationPreference(number, message, sessionController.getApplicationPreference());
    }

    public boolean sendSmsPromo(String number, String message, String username, String password, String sendingAlias) {
        return smsManager.sendSmsByApplicationPreference(number, message, sessionController.getApplicationPreference());
    }

    public boolean sendSms(String number, String message) {
        return smsManager.sendSmsByApplicationPreference(number, message, sessionController.getApplicationPreference());
    }

    public boolean sendSmsPromo(String number, String message) {
        return smsManager.sendSmsByApplicationPreference(number, message, sessionController.getApplicationPreference());
    }

    public void sendSmsToNumberList(String sendingNo, ApplicationInstitution ai, String msg, Bill b, MessageType smsType) {
        if (sendingNo.contains("077") || sendingNo.contains("076")
                || sendingNo.contains("071") || sendingNo.contains("070")
                || sendingNo.contains("072")
                || sendingNo.contains("075")
                || sendingNo.contains("078")) {
        } else {
            return;
        }
        Sms e = new Sms();
        e.setSentSuccessfully(Boolean.TRUE);
        boolean sent = sendSmsPromo(sendingNo, msg);

        if (sent) {
            e.setSentSuccessfully(true);
            e.setSentAt(new Date());
            e.setCreatedAt(new Date());
            e.setCreater(getSessionController().getLoggedUser());
            e.setBill(b);
            e.setSmsType(smsType);
            e.setSendingMessage(msg);
            getSmsFacade().create(e);
        }

    }

    public void sendSmsForPatientReport() {
        Date startTime = new Date();
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setBill(null);
        e.setPatientReport(null);
        e.setPatientInvestigation(null);
        e.setCreatedAt(new Date());
        e.setCreater(sessionController.getLoggedUser());
        e.setReceipientNumber(number);
        e.setSendingMessage(message);
        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
        e.setInstitution(getSessionController().getInstitution());
        e.setSentSuccessfully(false);
        getSmsFacade().create(e);
        smsManager.sendSmsByApplicationPreference(e.getReceipientNumber(), e.getSendingMessage(), sessionController.getApplicationPreference());
        UtilityController.addSuccessMessage("SMS Sent");
    }

    public void createSmsTable() {
        long lng = getCommonFunctions().getDayCount(getReportKeyWord().getFromDate(), getReportKeyWord().getToDate());
        if (Math.abs(lng) > 2 && !getReportKeyWord().isAdditionalDetails()) {
            UtilityController.addErrorMessage("Date Range is too Long");
            return;
        }
        String sql;
        Map m = new HashMap();
        smsSummeryRows = new ArrayList<>();
        smses = new ArrayList<>();

        if (getReportKeyWord().isAdditionalDetails()) {
            sql = " select s.smsType, count(s) ";
        } else {
            sql = " select s ";
        }
        sql += " from Sms s where s.retired=false "
                + " and s.createdAt between :fd and :td ";

        if (getReportKeyWord().getSmsType() != null) {
            sql += " and s.smsType=:st ";
            m.put("st", getReportKeyWord().getSmsType());
        }

        if (getReportKeyWord().isAdditionalDetails()) {
            sql += " group by s.smsType ";
        } else {
            sql += " order by s.id ";
        }

        m.put("fd", getReportKeyWord().getFromDate());
        m.put("td", getReportKeyWord().getToDate());

        if (getReportKeyWord().isAdditionalDetails()) {
            List<Object[]> objects = getSmsFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
            long l = 0l;
            for (Object[] ob : objects) {
                SmsSummeryRow row = new SmsSummeryRow();
                MessageType smsType = (MessageType) ob[0];
                long count = (long) ob[1];
                row.setSmsType(smsType);
                row.setCount(count);
                l += count;
                smsSummeryRows.add(row);
            }
            SmsSummeryRow row = new SmsSummeryRow();
            row.setSmsType(null);
            row.setCount(l);
            smsSummeryRows.add(row);
        } else {
            smses = getSmsFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public void fillAllSms() {
        System.out.println("fillAllSms");
        String j = "select s "
                + " from Sms s "
                + " where s.createdAt between :fd and :td ";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        System.out.println("m = " + m);
        System.out.println("j = " + j);
        smses = smsFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void fillAllFaildSms() {
        String j = "select s "
                + "from Sms s "
                + "where s.sentSuccessfully !=:suc "
                + "AND s.createdAt between :fd and :td";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("suc", true);
        System.out.println("m = " + m);
        System.out.println("j = " + j);
        faildsms = smsFacade.findByJpql(j, m);
    }

    public void sentUnsentSms() {
        if (selectedSms == null) {
            JsfUtil.addErrorMessage("No SMS selected");
            return;
        }
        boolean sendSms = smsManager.sendSmsByApplicationPreference(selectedSms.getReceipientNumber(), selectedSms.getSendingMessage(), sessionController.getApplicationPreference());
        if (sendSms == true) {
            getSmsFacade().edit(selectedSms);
        }
    }

    public List<Sms> allsms() {
        return getSmsFacade().findAll();
    }

    public List<SmsSummeryRow> getSmsSummeryRows() {
        return smsSummeryRows;
    }

    public void setSmsSummeryRows(List<SmsSummeryRow> smsSummeryRows) {
        this.smsSummeryRows = smsSummeryRows;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageOutput() {
        return messageOutput;
    }

    public void setMessageOutput(String messageOutput) {
        this.messageOutput = messageOutput;
    }

    public SmsManagerEjb getSmsManager() {
        return smsManager;
    }

    public void setSmsManager(SmsManagerEjb smsManager) {
        this.smsManager = smsManager;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Sms getSelectedSms() {
        return selectedSms;
    }

    public void setSelectedSms(Sms selectedSms) {
        this.selectedSms = selectedSms;
    }

    public class SmsSummeryRow {

        MessageType smsType;
        long count;

        public MessageType getSmsType() {
            return smsType;
        }

        public void setSmsType(MessageType smsType) {
            this.smsType = smsType;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    //---------Getters and Setters
    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
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

    public List<Sms> getSmses() {
        return smses;
    }

    public void setSmses(List<Sms> smses) {
        this.smses = smses;
    }

}
