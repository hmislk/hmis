/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.java;

import com.divudi.data.Sex;
import com.divudi.data.Title;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 *
 * @author buddhika
 */
public class CommonFunctions {

    public static double round(double numberToRound, int decimalPlaces) {
        if (numberToRound == 0) {
            return 0.0;
        }
        Double m = Math.pow(10, decimalPlaces);
        Double n = m * numberToRound;
        Long l = Math.round(n);
        return l / m;
    }

    public static double round(double numberToRound) {
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

    public static LocalDateTime getLocalDateTime(Date dateTime){
        Date input = dateTime;
        LocalDateTime date = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return date;
    }
    
    public static Date getStartOfMonth(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("IST"));
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
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

    public static Date getStartOfDay() {
        return getStartOfDay(new Date());
    }

    public static Date getStartOfDay(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);
        ////// // System.out.println("calendar.getTime() = " + calendar.getTime());
        return calendar.getTime();
    }

    public static Date getStartOfMonth() {
        return getStartOfMonth(new Date());
    }

    

    public static Date getEndOfDay() {
        return getEndOfDay(new Date());
    }

    public static Date getEndOfDay(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        ////// // System.out.println("calendar.getTime() = " + calendar.getTime());
        return calendar.getTime();
    }

    public static String nameToCode(String name) {
        if(name==null){
            return "";
        }
        String code;
        code = name.replaceAll(" ", "_");
        code = code.toLowerCase();
        return code;
    }

    public Date getFirstDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        //////// // System.out.println("First : " + cal.getTime());
        return cal.getTime();
    }

    public Date getLastDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.DATE, 31);
        //////// // System.out.println("Last : " + cal.getTime());
        return cal.getTime();
    }

    public Date getFirstDayOfWeek(Date date) {
        // get today and clear time of day
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

// get start of this week in milliseconds
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        //      //////// // System.out.println("Start of this week:       " + cal.getTime());
        //       //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());

// start of the next week
//        cal.add(Calendar.WEEK_OF_YEAR, 1);
//        //////// // System.out.println("Start of the next week:   " + cal.getTime());
//        //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());
        return cal.getTime();
    }

    public Date getLastDayOfWeek(Date date) {
        // get today and clear time of day
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

// get start of this week in milliseconds
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        //   //////// // System.out.println("Start of this week:       " + cal.getTime());
        //     //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());

        cal.add(Calendar.DATE, 7);

// start of the next week
//        cal.add(Calendar.WEEK_OF_YEAR, 1);
//        //////// // System.out.println("Start of the next week:   " + cal.getTime());
//        //////// // System.out.println("... in milliseconds:      " + cal.getTimeInMillis());
        return cal.getTime();
    }

    public static Date removeTime(Date date) {
        Calendar cal;
        cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        //      System.err.println("Only Date " + cal.getTime());
        return cal.getTime();
    }

    public static double roundToTwoDecimalPlaces(double num) {
        return roundToTwoDecimalPlaces(num, 2);
    }

    public static double roundToTwoDecimalPlaces(double num, int decimalPlaces) {
        double mul = Math.pow(10, decimalPlaces);
        double roundOff = (double) Math.round(num * mul) / mul;
        return roundOff;
    }

}
