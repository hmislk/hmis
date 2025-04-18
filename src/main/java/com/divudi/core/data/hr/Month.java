/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.data.hr;

/**
 *
 * @author User
 */
public enum Month {
    January(0),
    February(1),
    March(2),
    April(3),
    May(4),
    June(5),
    July(6),
    August(7),
    September(8),
    October(9),
    November(10),
    December(11)
    ;

    private final int monthValue;

    public int getMonthValue(){
        return monthValue;
    }

    Month(int value) {
        this.monthValue = value;
    }
}
