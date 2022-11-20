/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Staff;
import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author buddhika_ari
 */
public class WeekdayDisplay {

    private Staff staff;
    private Institution institution;
    private Department department;
    private Item item;
    private Date date;
    private Date startDate;
    private Date endDate;
    private Integer weekdayYear;
    private Integer weekdayMonth;

    private String[] dayNames;
    private DayOfWeek[] weekdays;
    private int[] daysOfMonth;
    private Date[] dates;
    private Long[] counts;
    private Double[] values;

    public WeekdayDisplay() {
    }

    public WeekdayDisplay(Date date) {
        this.date = date;
        configureDates();
        fillData();
    }

    public WeekdayDisplay(Integer weekdayYear, Integer weekdayMonth) {
        this.weekdayYear = weekdayYear;
        this.weekdayMonth = weekdayMonth;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, weekdayYear);
        c.set(Calendar.MONTH, weekdayMonth);
        date = c.getTime();
        configureDates();
        fillData();
    }

    private void resetCalcualtedDates() {
        startDate = null;
        endDate = null;
        weekdayYear = null;
        weekdayMonth = null;
    }

    private void configureDates() {
        resetCalcualtedDates();
        if (date == null) {
            return;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.DATE, 1);
        startDate = c.getTime();
        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
        endDate = c.getTime();
        weekdayYear = c.get(Calendar.YEAR);
        weekdayMonth = c.get(Calendar.MONTH);
    }

    private void fillData() {
        dayNames = new String[37];
        weekdays = new DayOfWeek[37];
        daysOfMonth = new int[37];
        dates = new Date[37];
        counts = new Long[37];
        values = new Double[37];
        Calendar tc = Calendar.getInstance();
        tc.setTime(startDate);
        boolean startFilling = false;

        for (int i = 0; i < 37; i++) {
            int m = i % 7;
            switch (m) {
                case 0:
                    weekdays[i] = DayOfWeek.MONDAY;
                    break;
                case 1:
                    weekdays[i] = DayOfWeek.TUESDAY;
                    break;
                case 2:
                    weekdays[i] = DayOfWeek.WEDNESDAY;
                    break;
                case 3:
                    weekdays[i] = DayOfWeek.THURSDAY;
                    break;
                case 4:
                    weekdays[i] = DayOfWeek.SUNDAY;
                    break;
                case 5:
                    weekdays[i] = DayOfWeek.SATURDAY;
                    break;
                case 6:
                    weekdays[i] = DayOfWeek.MONDAY;
            }
        }

        for (int i = 0; i < 37; i++) {
            if (!startFilling) {
                if (weekdays[i].getValue() - 1 == tc.get(Calendar.DAY_OF_WEEK)) {
                    startFilling = true;
                }
            }
            if (startFilling) {
                dayNames[i] = weekdays[i].toString();
                daysOfMonth[i] = tc.get(Calendar.DATE);
                dates[i] = tc.getTime();
                tc.add(Calendar.DATE, 1);
            }

        }
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        configureDates();
        fillData();
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String[] getDayNames() {
        return dayNames;
    }

    public void setDayNames(String[] dayNames) {
        this.dayNames = dayNames;
    }

    public DayOfWeek[] getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(DayOfWeek[] weekdays) {
        this.weekdays = weekdays;
    }

    public int[] getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(int[] daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public Date[] getDates() {
        return dates;
    }

    public void setDates(Date[] dates) {
        this.dates = dates;
    }

    public Long[] getCounts() {
        return counts;
    }

    public void setCounts(Long[] counts) {
        this.counts = counts;
    }

    public Double[] getValues() {
        return values;
    }

    public void setValues(Double[] values) {
        this.values = values;
    }

    public Integer getWeekdayYear() {
        return weekdayYear;
    }

    public void setWeekdayYear(Integer weekdayYear) {
        this.weekdayYear = weekdayYear;
    }

    public Integer getWeekdayMonth() {
        return weekdayMonth;
    }

    public void setWeekdayMonth(Integer weekdayMonth) {
        this.weekdayMonth = weekdayMonth;
    }

}
