/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author pdhs
 */
public class TimeUtils {

    public static String millisToYearsAndDates(long duration) {
        String res;
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        if (days == 365) {
            res = "One year.";
        } else if (days > 365) {
            long years = days / 365;
            if ((days - (years * 365)) == 0) {
                res = years + " years.";
            } else {
                res = years + " years and " + (days - (years * 365)) + " days";
            }
        } else if (days == 0) {
            res = "Less than a day.";
        } else {
            res = days + " days.";
        }
        return res;
    }

}
