/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author safrin
 */
public class SystemTimeStamp {

    private Integer year = null;
    private Integer month = null;
    private Integer day = null;
    private Integer hour = null;
    private Integer minute = null;
    private Integer second = null;
    private Date time = null;

    public void clear() {
        year = null;
        month = null;
        day = null;
        hour = null;
        minute = null;
        second = null;
        time = null;
    }

    public boolean checkDate() {
        if (year == null || month == null || day == null) {
            return true;
        }

        return false;
    }

    public boolean checkHour() {
        if (hour == null) {
            return true;
        }

        return false;
    }

    private void processTime() {
        System.err.println("Start Process Time");
        if (checkDate()) {
            return;
        }

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, (month - 1));
            calendar.set(Calendar.DAY_OF_MONTH, day);

            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, second);

            setTime(calendar.getTime());
        } catch (Exception e) {
        }

        System.err.println("End Process Time");
    }

    public void processTime(Date date) {
        System.err.println("Start Process Time with Date");
        if (date == null) {
            return;
        }

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            setYear(calendar.get(Calendar.YEAR));
            setMonth(1 + calendar.get(Calendar.MONTH));
            setDay(calendar.get(Calendar.DAY_OF_MONTH));
            setHour(calendar.get(Calendar.HOUR_OF_DAY));
            setMinute(calendar.get(Calendar.MINUTE));
            setSecond(calendar.get(Calendar.SECOND));
        } catch (Exception e) {
        }

        System.err.println("End Process Time with Date");
    }

    public Date getTime() {
        processTime();
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public Integer getSecond() {
        return second;
    }

    public void setSecond(Integer second) {
        this.second = second;
    }

}
