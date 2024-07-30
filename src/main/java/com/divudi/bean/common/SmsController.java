/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.MessageType;
import com.divudi.data.SmsSentResponse;
import com.divudi.data.hr.ReportKeyWord;

import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.Sms;
import com.divudi.entity.UserPreference;
import com.divudi.facade.SmsFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;

import java.util.ArrayList;
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

    CommonFunctions commonFunctions;
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

    public List<Sms> getFaildsms() {
        return faildsms;
    }

//    public void sentCheckSms() {
//        if (smsMessage == null) {
//            JsfUtil.addErrorMessage("Message?");
//            return;
//        }
//        if (smsNumber == null) {
//            JsfUtil.addErrorMessage("Message?");
//            return;
//        }
//        smsOutput = smsManager.sendSmsByApplicationPreferenceReturnString(smsNumber, smsMessage, sessionController.getApplicationPreference());
//
//        UserPreference pf = sessionController.getApplicationPreference();
//        smsOutput += "\n" + "Username Parameter : " + pf.getSmsUsernameParameterName();
//        smsOutput += "\n" + "Username : " + pf.getSmsUsername();
//        smsOutput += "\n" + "Passwprd Parameter : " + pf.getSmsPasswordParameterName();
//        smsOutput += "\n" + "Password : " + pf.getSmsPassword();
//        smsOutput += "\n" + "Alias Parameter : " + pf.getSmsUserAliasParameterName();
//        smsOutput += "\n" + "Alias : " + pf.getSmsUserAlias();
//        smsOutput += "\n" + "Number Parameter : " + pf.getSmsPhoneNumberParameterName();
//        smsOutput += "\n" + "Text Parameter : " + pf.getSmsMessageParameterName();
//
//    }

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

//    public void sendSmsAwaitingToSendInDatabase() {
//        String j = "Select e from Sms e where e.sentSuccessfully=false and e.retired=false";
//        List<Sms> smses = getSmsFacade().findByJpql(j);
//        for (Sms e : smses) {
//            e.setSentSuccessfully(Boolean.TRUE);
//            getSmsFacade().edit(e);
//            Boolean sentSuccessfully = smsManager.sendSms(e);
//        }
//    }

    public Boolean sendSms(String number, String message, String username, String password, String sendingAlias) {
        Sms s = new Sms();
        s.setReceipientNumber(number);
        s.setSendingMessage(message);
        return smsManager.sendSms(s);
    }

    public void sendSms() {
        Sms s = new Sms();
        s.setReceipientNumber(smsNumber);
        s.setSendingMessage(smsMessage);
        save(s);
        boolean b= smsManager.sendSms(s);
        selectedSms = s;
    }

    
//    public Boolean sendSmsPromo(String number, String message, String username, String password, String sendingAlias) {
//        Sms s = new Sms();
//        s.setReceipientNumber(number);
//        s.setSendingMessage(message);
//        return smsManager.sendSms(s);
//    }
    public Boolean sendSms(String number, String message) {
        Sms s = new Sms();
        s.setReceipientNumber(number);
        s.setSendingMessage(message);
        return smsManager.sendSms(s);
    }

//    public Boolean sendSmsPromo(String number, String message) {
//        Sms s = new Sms();
//        s.setReceipientNumber(number);
//        s.setSendingMessage(message);
//        return smsManager.sendSms(s);
//    }
//    public void sendSmsToNumberList(String sendingNo, ApplicationInstitution ai, String msg, Bill b, MessageType smsType) {
//        if (sendingNo.contains("077") || sendingNo.contains("076")
//                || sendingNo.contains("071") || sendingNo.contains("070")
//                || sendingNo.contains("072")
//                || sendingNo.contains("075")
//                || sendingNo.contains("078")) {
//        } else {
//            return;
//        }
//        Sms e = new Sms();
//        e.setSentSuccessfully(Boolean.TRUE);
//        SmsSentResponse sent = sendSmsPromo(sendingNo, msg);
//
//        if (sent.isSentSuccefully()) {
//            e.setSentSuccessfully(true);
//            e.setSentAt(new Date());
//            e.setCreatedAt(new Date());
//            e.setCreater(getSessionController().getLoggedUser());
//            e.setBill(b);
//            e.setReceivedMessage(sent.getReceivedMessage());
//            e.setSmsType(smsType);
//            e.setSendingMessage(msg);
//            getSmsFacade().create(e);
//        }
//
//    }
//    public void sendSmsForPatientReport() {
//        Date startTime = new Date();
//        Sms e = new Sms();
//        e.setCreatedAt(new Date());
//        e.setCreater(sessionController.getLoggedUser());
//        e.setBill(null);
//        e.setPatientReport(null);
//        e.setPatientInvestigation(null);
//        e.setCreatedAt(new Date());
//        e.setCreater(sessionController.getLoggedUser());
//        e.setReceipientNumber(number);
//        e.setSendingMessage(message);
//        e.setDepartment(getSessionController().getLoggedUser().getDepartment());
//        e.setInstitution(getSessionController().getInstitution());
//        e.setSentSuccessfully(false);
//        getSmsFacade().create(e);
//        smsManager.sendSmsByApplicationPreference(e.getReceipientNumber(), e.getSendingMessage(), sessionController.getApplicationPreference());
//        JsfUtil.addSuccessMessage("SMS Sent");
//    }
//    
//    public void createSmsTable() {
//        long lng = getCommonFunctions().getDayCount(getReportKeyWord().getFromDate(), getReportKeyWord().getToDate());
//        if (Math.abs(lng) > 2 && !getReportKeyWord().isAdditionalDetails()) {
//            JsfUtil.addErrorMessage("Date Range is too Long");
//            return;
//        }
//        String sql;
//        Map m = new HashMap();
//        smsSummeryRows = new ArrayList<>();
//        smses = new ArrayList<>();
//
//        if (getReportKeyWord().isAdditionalDetails()) {
//            sql = " select s.smsType, count(s) ";
//        } else {
//            sql = " select s ";
//        }
//        sql += " from Sms s where s.retired=false "
//                + " and s.createdAt between :fd and :td ";
//
//        if (getReportKeyWord().getSmsType() != null) {
//            sql += " and s.smsType=:st ";
//            m.put("st", getReportKeyWord().getSmsType());
//        }
//
//        if (getReportKeyWord().isAdditionalDetails()) {
//            sql += " group by s.smsType ";
//        } else {
//            sql += " order by s.id ";
//        }
//
//        m.put("fd", getReportKeyWord().getFromDate());
//        m.put("td", getReportKeyWord().getToDate());
//
//        if (getReportKeyWord().isAdditionalDetails()) {
//            List<Object[]> objects = getSmsFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
//            long l = 0l;
//            for (Object[] ob : objects) {
//                SmsSummeryRow row = new SmsSummeryRow();
//                MessageType smsType = (MessageType) ob[0];
//                long count = (long) ob[1];
//                row.setSmsType(smsType);
//                row.setCount(count);
//                l += count;
//                smsSummeryRows.add(row);
//            }
//            SmsSummeryRow row = new SmsSummeryRow();
//            row.setSmsType(null);
//            row.setCount(l);
//            smsSummeryRows.add(row);
//        } else {
//            smses = getSmsFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        }
//
//    }
    public void fillAllSms() {
        String j = "select s "
                + " from Sms s "
                + " where s.createdAt between :fd and :td ";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        smses = smsFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

//    
//    public void sendSms() {
//        selectedSms = new Sms();
//        selectedSms.setSendingMessage(smsMessage);
//        selectedSms.setReceipientNumber(smsNumber);
//        smsManager.sendSms(selectedSms);
//    }
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
            JsfUtil.addErrorMessage("no sms");
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
