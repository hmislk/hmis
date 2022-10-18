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
import com.divudi.entity.Bill;
import com.divudi.entity.Sms;
import com.divudi.facade.SmsFacade;

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
@Named(value = "smsController")
@SessionScoped
public class SmsController implements Serializable {

    @EJB
    SmsFacade smsFacade;

    @Inject
    SessionController sessionController;
    @Inject
    CommonFunctions commonFunctions;

    List<Sms> smses;
    List<SmsSummeryRow> smsSummeryRows;

    ReportKeyWord reportKeyWord;

    /**
     * Creates a new instance of SmsController
     */
    public SmsController() {
    }

    public void sendSmsAwaitingToSendInDatabase() {
        String j = "Select e from Sms e where e.sentSuccessfully=false and e.retired=false";
        List<Sms> smses = getSmsFacade().findBySQL(j);
//        if (false) {
//            Sms e = new Sms();
//            e.getSentSuccessfully();
//            e.getInstitution();
//        }
        for (Sms e : smses) {
            e.setSentSuccessfully(Boolean.TRUE);
            getSmsFacade().edit(e);

            boolean sentSuccessfully = sendSms(e.getReceipientNumber(), e.getSendingMessage(),
                    e.getInstitution().getSmsSendingUsername(),
                    e.getInstitution().getSmsSendingPassword(),
                    e.getInstitution().getSmsSendingAlias());
            e.setSentSuccessfully(sentSuccessfully);
            e.setSentAt(new Date());
            getSmsFacade().edit(e);
        }

    }

    public static String executePost(String targetURL, Map<String, Object> parameters) {
        HttpURLConnection connection = null;
        if (parameters != null && !parameters.isEmpty()) {
            targetURL += "?";
        }
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            targetURL += pPara + "=" + pVal.toString() + "&";
        }
        if (parameters != null && !parameters.isEmpty()) {
            targetURL += "last=true";
        }
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);
            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(targetURL);
            wr.flush();
            wr.close();

            //Get Response  
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public boolean sendSms(String number, String message, String username, String password, String sendingAlias) {

        //// // System.out.println("number = " + number);
        //// // System.out.println("message = " + message);
        //// // System.out.println("username = " + username);

        Map m = new HashMap();
        m.put("userName", username);
        m.put("password", password);
        m.put("userAlias", sendingAlias);
        m.put("number", number);
        m.put("message", message);

        String res = executePost("http://localhost:8080/sms/faces/index.xhtml", m);
        if (res == null) {
            return false;
        } else if (res.toUpperCase().contains("200")) {
            return true;
        } else {
            return false;
        }

    }

    public boolean sendSmsPromo(String number, String message, String username, String password, String sendingAlias) {

        //// // System.out.println("number = " + number);
        //// // System.out.println("message = " + message);
        //// // System.out.println("username = " + username);

        Map m = new HashMap();
        m.put("userName", username);
        m.put("password", password);
        m.put("userAlias", sendingAlias);
        m.put("number", number);
        m.put("message", message);
        m.put("promo", "yes");

        String res = executePost("http://localhost:7070/sms/faces/index.xhtml", m);
        if (res == null) {
            return false;
        } else if (res.toUpperCase().contains("200")) {
            return true;
        } else {
            return false;
        }

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

        boolean sent = sendSmsPromo(sendingNo, msg, getSessionController().getInstitution().getSmsSendingUsername(),
                getSessionController().getInstitution().getSmsSendingPassword(),
                getSessionController().getInstitution().getSmsSendingAlias());

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
            smses = getSmsFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        }

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
