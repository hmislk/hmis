/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.MessageType;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.core.entity.Sms;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;

import java.util.List;
import java.util.Map;

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

    /*
    Class Variables
     */
    List<Sms> smses;
    List<Sms> faildsms;
    private Sms selectedSms;
    private Boolean bool;

    private String smsMessage;
    private String smsNumber;
    private String smsOutput;

    // New variable to control SMS sending
    private static boolean doNotSendAnySms = false;

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

    public Boolean sendSms(String number, String message, String username, String password, String sendingAlias) {
        if (doNotSendAnySms) {
            return false;
        }
        selectedSms = new Sms();
        selectedSms.setReceipientNumber(number);
        selectedSms.setSendingMessage(message);
        return smsManager.sendSms(selectedSms);
    }

    public void sendSms() {
        if (doNotSendAnySms) {
            return;
        }
        Sms s = new Sms();
        s.setReceipientNumber(smsNumber);
        s.setSendingMessage(smsMessage);
        save(s);
        boolean b = smsManager.sendSms(s);
        selectedSms = s;
    }

    public void sendSmsFromWeb() {
        System.out.println("sendSmsFromWeb");
        System.out.println("doNotSendAnySms = " + doNotSendAnySms);
        if (doNotSendAnySms) {
            return;
        }
        Sms s = new Sms();
        s.setReceipientNumber(smsNumber);
        s.setSendingMessage(smsMessage);
        save(s);
        boolean b = smsManager.sendSms(s);
        selectedSms = s;
    }

    public Boolean sendSms(String number, String message) {
        if (doNotSendAnySms) {
            return false;
        }
        Sms s = new Sms();
        s.setReceipientNumber(number);
        s.setSendingMessage(message);
        return smsManager.sendSms(s);
    }

    public void fillAllSms() {
        String j = "select s "
                + " from Sms s "
                + " where s.createdAt between :fd and :td ";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        smses = smsFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void fillAllFaildSms() {
        // Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
        String j = "select s "
                + "from Sms s "
                + "where s.sentSuccessfully <> :suc "
                + "AND s.createdAt between :fd and :td";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("suc", true);
        faildsms = smsFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void sentUnsentSms() {
        if (doNotSendAnySms) {
            JsfUtil.addErrorMessage("SMS sending is disabled");
            return;
        }
        if (selectedSms == null) {
            JsfUtil.addErrorMessage("No SMS selected");
            return;
        }
        Boolean sendSms = smsManager.sendSms(selectedSms);
        if (sendSms) {
            JsfUtil.addSuccessMessage("Sent Successfully");
        } else {
            JsfUtil.addSuccessMessage("Sending failed");
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

    public String getSmsMessage() {
        return smsMessage;
    }

    public void setSmsMessage(String smsMessage) {
        this.smsMessage = smsMessage;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public String getSmsOutput() {
        return smsOutput;
    }

    public void setSmsOutput(String smsOutput) {
        this.smsOutput = smsOutput;
    }

    public Boolean getBool() {
        return bool;
    }

    public void setBool(Boolean bool) {
        this.bool = bool;
    }

    private void save(Sms s) {
        if(s==null){
            JsfUtil.addErrorMessage("No SMS");
            return;
        }
        if(s.getId()==null){
            s.setCreatedAt(new Date());
            s.setCreater(sessionController.getLoggedUser());
            smsFacade.create(s);
        }else{
            smsFacade.edit(s);
        }
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
