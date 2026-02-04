package com.divudi.core.util;

import java.time.Month;
import java.util.Date;

/**
 *
 * @author buddhika
 */
public class MonthInfo {

    private Date date;
    private Month month;

    MonthInfo(Date date, Month month) {
        this.date = date;
        this.month = month;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    
}
