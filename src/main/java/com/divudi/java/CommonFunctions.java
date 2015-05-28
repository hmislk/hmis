/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.java;

import com.divudi.data.Sex;
import com.divudi.data.Title;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author buddhika
 */
public class CommonFunctions {

    public static  double round(double numberToRound, int decimalPlaces){
        if(numberToRound==0){
            return 0.0;
        }
        Double m = Math.pow(10, decimalPlaces);
        Double n = m * numberToRound ;
        Long l=Math.round(n);
        return l/m;
    }
    
    public static double round(double numberToRound){
        return round(numberToRound, 2);
    }
    
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

    public static Boolean checkAgeSex(Date dob, Sex sex, Title title) {
        Boolean result = true;
        Date toDate = Calendar.getInstance().getTime();

        long age;

        if ((toDate.getTime() - dob.getTime()) / (1000 * 60 * 60 * 24) == 0) {
            return false;
        }

        age = ((toDate.getTime() - dob.getTime()) / (1000 * 60 * 60 * 24)) / 365;

        if (title == Title.Baby || title == Title.Baby_Of) {
            if (age > 6) {
                result = false;
            }
        } else if ((title == Title.Master)) {
            if (age > 13) {
                result = false;
            }
        }

        if (title == Title.Mrs
                || title == Title.Mrs
                || title == Title.Ms
                || title == Title.Miss
                || title == Title.DrMrs
                || title == Title.DrMiss) {

            if (sex == Sex.Male) {
                result = false;
            }
        }

        if (title == Title.Mr
                || title == Title.Master
                || title == Title.Dr) {
            if (sex == Sex.Female) {
                result = false;
            }
        }

        return result;
    }
    
    
    
    
}
