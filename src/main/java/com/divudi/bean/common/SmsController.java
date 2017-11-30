/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.SmsType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.Sms;
import com.divudi.facade.SmsFacade;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void sendSmsToNumberList(String sendingNo, ApplicationInstitution ai, String msg, Bill b, SmsType smsType) {

        if (sendingNo.contains("077") || sendingNo.contains("076")
                || sendingNo.contains("071") || sendingNo.contains("070")
                || sendingNo.contains("072")
                || sendingNo.contains("075")
                || sendingNo.contains("078")) {
        } else {
            return;
        }

        if (ai == ApplicationInstitution.Ruhuna) {
            StringBuilder sb = new StringBuilder(sendingNo);
            sb.deleteCharAt(3);
            sendingNo = sb.toString();

            String url = "https://cpsolutions.dialog.lk/index.php/cbs/sms/send?destination=94";
            HttpResponse<String> stringResponse;
            String pw = "&q=14488825498722";

            String messageBody2 = msg;

            System.out.println("messageBody2 = " + messageBody2.length());

            final StringBuilder request = new StringBuilder(url);
            request.append(sendingNo.substring(1, 10));
            request.append(pw);

            try {
                System.out.println("pw = " + pw);
                System.out.println("sendingNo = " + sendingNo);
                System.out.println("sendingNo.substring(1, 10) = " + sendingNo.substring(1, 10));
                System.out.println("text = " + messageBody2);

                stringResponse = Unirest.post(request.toString()).field("message", messageBody2).asString();

            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            Sms sms = new Sms();
            sms.setPassword(pw);
            sms.setCreatedAt(new Date());
            sms.setCreater(getSessionController().getLoggedUser());
            sms.setBill(b);
            sms.setSmsType(smsType);
            sms.setSendingUrl(url);
            sms.setSendingMessage(messageBody2);
            getSmsFacade().create(sms);
        }
    }

    public void createSmsTable() {
        long lng = getCommonFunctions().getDayCount(getReportKeyWord().getFromDate(), getReportKeyWord().getToDate());
        System.out.println("lng = " + lng);

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

        System.out.println("m = " + m);
        System.out.println("sql = " + sql);
        System.out.println("smses.size() = " + smses.size());

        if (getReportKeyWord().isAdditionalDetails()) {
            List<Object[]> objects = getSmsFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
            long l = 0l;
            for (Object[] ob : objects) {
                SmsSummeryRow row = new SmsSummeryRow();
                SmsType smsType = (SmsType) ob[0];
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

        SmsType smsType;
        long count;

        public SmsType getSmsType() {
            return smsType;
        }

        public void setSmsType(SmsType smsType) {
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
