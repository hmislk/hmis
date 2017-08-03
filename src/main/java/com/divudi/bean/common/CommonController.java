/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 *
 * @author buddhika
 */
@Named(value = "commonController")
@SessionScoped
public class CommonController implements Serializable {

    @Inject
    private SessionController sessionController;

    /**
     * Creates a new instance of CommonController
     */
    public CommonController() {
    }

    public Date getCurrentDateTime() {
        return new Date();
    }

    public boolean sameDate(Date date1, Date date2) {
        Calendar d1 = Calendar.getInstance();
        d1.setTime(date1);
        DateTime first = new DateTime(date1);
        DateTime second = new DateTime(date2);
        LocalDate firstDate = first.toLocalDate();
        LocalDate secondDate = second.toLocalDate();
        return firstDate.equals(secondDate);
    }

    public Date retiermentDate(Date dob) {
        if (dob == null) {
            dob = new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(dob);
        cal.add(Calendar.YEAR, 50);
        System.out.println("cal = " + cal.getTime());
        return cal.getTime();
    }

    public double dateDifferenceInMinutes(Date fromDate, Date toDate) {
        long timeInMs = toDate.getTime() - fromDate.getTime();
        return timeInMs / (1000 * 60);
    }

    public double dateDifferenceInSeconds(Date fromDate, Date toDate) {
        long timeInMs = toDate.getTime() - fromDate.getTime();
        return timeInMs / 1000;
    }

    public void printReportDetails(Date fromDate, Date toDate, Date startTime, String url) {

        String s;
        s = "***************";
        s += "\n Report User :" + getSessionController().getLoggedUser().getWebUserPerson().getName();
        s += "\n Report Description : " + url;
        if (fromDate != null) {
            s += "\n Report Form :" + fromDate;
        }
        if (toDate != null) {
            s += " To :" + toDate;
        }
        s += "\n Report Start Time : " + startTime + " End Time :" + new Date();
        if (fromDate != null && toDate != null) {
            s += "\n Time Defferent : " + dateDifferenceInMinutes(fromDate, toDate);
        }
        if (startTime != null) {
            s += "\n Report Time Defferent(Miniuts) : " + dateDifferenceInMinutes(startTime, new Date());
            s += "\n Report Time Defferent(Seconds) : " + dateDifferenceInSeconds(startTime, new Date());
        }
        s += "\n ***************";

        System.err.println(s);

    }

    //----------Date Time Formats
    public String getDateFormat(Date date) {
        String s = "";
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd");
        s = d.format(date);
        return s;
    }

    public String getDateFormat2(Date date) {
        String s = "";
        DateFormat d = new SimpleDateFormat("YYYY-MMM-dd");
        s = d.format(date);
        return s;
    }

    public String getTimeFormat12(Date date) {
        String s = "";
        DateFormat d = new SimpleDateFormat("hh:mm:ss a");
        s = d.format(date);
        return s;
    }

    public String getTimeFormat24(Date date) {
        String s = "";
        DateFormat d = new SimpleDateFormat("HH:mm:ss");
        s = d.format(date);
        return s;
    }

    public String getDateTimeFormat12(Date date) {
        String s = "";
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss a");
        s = d.format(date);
        return s;
    }

    public String getDateTimeFormat24(Date date) {
        String s = "";
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        s = d.format(date);
        return s;
    }

    public Date getConvertDateTimeFormat24(String dateString) throws ParseException {
        DateFormat d = new SimpleDateFormat("yyyy-MM-dd");
        Date date = d.parse(dateString);
        System.out.println("date = " + date);
        System.out.println("dateString = " + dateString);
        return date;
    }

    public String getDouble(double d) {
        String s = "";
        NumberFormat myFormatter = new DecimalFormat("##0.00");
        s = myFormatter.format(d);
        System.out.println("s = " + s);
        return s;
    }

    //----------Date Time Formats
    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}
