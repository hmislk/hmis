/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
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

    public double dateDifferenceInMinutes(Date fromDate, Date toDate) {
        long timeInMs = toDate.getTime() - fromDate.getTime();
        return timeInMs / (1000 * 60);
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
        s += "\n ***************";

        System.err.println(s);

    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}
