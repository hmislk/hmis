package com.divudi.java;

import com.divudi.entity.channel.SessionInstance;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Date;
import java.util.List;

@Named
@ApplicationScoped
public class CommonFunctionsProxy {

    public String convertToWordJSF(double number) {
        return CommonFunctions.convertToWord(number);
    }

    public String formatToLongDate(Date date, String dateFormat) {
        return CommonFunctions.formatToLongDate(date, dateFormat);
    }

    public Date getCurrentDateTime() {
        return CommonFunctions.getCurrentDateTime();
    }

    public Date getDateBeforeThreeMonthsCurrentDateTime() {
        return CommonFunctions.getDateBeforeThreeMonthsCurrentDateTime();
    }

    public Date getEndOfDayOnCurrentDate() {
        return CommonFunctions.getEndOfDay(new Date());
    }

    public boolean renderPaginator(List<Object> list, int count) {
        return CommonFunctions.renderPaginator(list, count);
    }

    public Date getDateAfterThreeMonthsCurrentDateTime() {
        return CommonFunctions.getDateAfterThreeMonthsCurrentDateTime();
    }

    public boolean sameDate(Date date1, Date date2) {
        return CommonFunctions.sameDate(date1, date2);
    }

    public Date getEndOfDay() {
        return CommonFunctions.getEndOfDay(new Date());
    }

    public SessionInstance convertToSessionInstance(Object ob) {
        if (ob instanceof SessionInstance) {
            return (SessionInstance) ob;
        }
        return null;
    }

    public Date retiermentDate(Date dob) {
        return CommonFunctions.retiermentDate(dob);
    }

    public double dateDifferenceInMinutes(Date fromDate, Date toDate) {
        return CommonFunctions.dateDifferenceInMinutes(fromDate, toDate);
    }
}
