/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.java;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author buddhika
 */
public class CommonFunctions {

    public static long calTimePeriod(Date frDate, Date tDate) {
        if (frDate == null || tDate == null) {
            return 0;
        }
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(tDate);
        cal2.setTime(frDate);
        Long minCount = (cal1.getTimeInMillis() - cal2.getTimeInMillis()) / (1000 * 60 * 60);
        return minCount;
    }
    
    
    public Date getBeginningOfMonth(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MINUTE, -1);
        return calendar.getTime();
    }

    
    public static Date getStartOfMonth(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("IST"));
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, 1, 0, 0, 0);
        return calendar.getTime();
    }
    
    public static Date getEndOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.SECOND, -1);
        return calendar.getTime();
    }
    
    
}
