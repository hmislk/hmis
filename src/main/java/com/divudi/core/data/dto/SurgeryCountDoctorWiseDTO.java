package com.divudi.core.data.dto;

import com.divudi.core.entity.Staff;
import java.util.Calendar;
import java.util.Date;

public class SurgeryCountDoctorWiseDTO {

    private Staff staff;
    private String doctorName;
    private String specialityName;
    private Date createdAt;

    // Month-wise counters
    private int january;
    private int february;
    private int march;
    private int april;
    private int may;
    private int june;
    private int july;
    private int august;
    private int september;
    private int october;
    private int november;
    private int december;

    // Flag for subtotal rows
    private boolean isSubtotal;
    private boolean isGrandTotal;

    public SurgeryCountDoctorWiseDTO(Staff staff, String doctorName, String specialityName, Date createdAt) {
        this.staff = staff;
        this.doctorName = doctorName;
        this.specialityName = specialityName;
        this.createdAt = createdAt;
        this.isSubtotal = false;
        this.isGrandTotal = false;
    }

    // Constructor for subtotal rows
    public SurgeryCountDoctorWiseDTO(String specialityName) {
        this.specialityName = specialityName;
        this.doctorName = "Total";
        this.isSubtotal = true;
        this.isGrandTotal = false;
    }

    // Constructor for grand total row
    public SurgeryCountDoctorWiseDTO() {
        this.doctorName = "Grand Total";
        this.specialityName = "";
        this.isSubtotal = false;
        this.isGrandTotal = true;
    }

    // Getters and Setters
    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSubtotal() {
        return isSubtotal;
    }

    public void setSubtotal(boolean subtotal) {
        isSubtotal = subtotal;
    }

    public boolean isGrandTotal() {
        return isGrandTotal;
    }

    public void setGrandTotal(boolean grandTotal) {
        isGrandTotal = grandTotal;
    }

    public int getJanuary() {
        return january;
    }

    public void setJanuary(int january) {
        this.january = january;
    }

    public int getFebruary() {
        return february;
    }

    public void setFebruary(int february) {
        this.february = february;
    }

    public int getMarch() {
        return march;
    }

    public void setMarch(int march) {
        this.march = march;
    }

    public int getApril() {
        return april;
    }

    public void setApril(int april) {
        this.april = april;
    }

    public int getMay() {
        return may;
    }

    public void setMay(int may) {
        this.may = may;
    }

    public int getJune() {
        return june;
    }

    public void setJune(int june) {
        this.june = june;
    }

    public int getJuly() {
        return july;
    }

    public void setJuly(int july) {
        this.july = july;
    }

    public int getAugust() {
        return august;
    }

    public void setAugust(int august) {
        this.august = august;
    }

    public int getSeptember() {
        return september;
    }

    public void setSeptember(int september) {
        this.september = september;
    }

    public int getOctober() {
        return october;
    }

    public void setOctober(int october) {
        this.october = october;
    }

    public int getNovember() {
        return november;
    }

    public void setNovember(int november) {
        this.november = november;
    }

    public int getDecember() {
        return december;
    }

    public void setDecember(int december) {
        this.december = december;
    }

    public int getTotalSurgeries() {
        return january + february + march + april + may + june
                + july + august + september + october + november + december;
    }

    public void addMonthCount(int month, int count) {
        switch (month) {
            case Calendar.JANUARY:
                january += count;
                break;
            case Calendar.FEBRUARY:
                february += count;
                break;
            case Calendar.MARCH:
                march += count;
                break;
            case Calendar.APRIL:
                april += count;
                break;
            case Calendar.MAY:
                may += count;
                break;
            case Calendar.JUNE:
                june += count;
                break;
            case Calendar.JULY:
                july += count;
                break;
            case Calendar.AUGUST:
                august += count;
                break;
            case Calendar.SEPTEMBER:
                september += count;
                break;
            case Calendar.OCTOBER:
                october += count;
                break;
            case Calendar.NOVEMBER:
                november += count;
                break;
            case Calendar.DECEMBER:
                december += count;
                break;
        }
    }

    public void addAllCounts(SurgeryCountDoctorWiseDTO other) {
        this.january += other.january;
        this.february += other.february;
        this.march += other.march;
        this.april += other.april;
        this.may += other.may;
        this.june += other.june;
        this.july += other.july;
        this.august += other.august;
        this.september += other.september;
        this.october += other.october;
        this.november += other.november;
        this.december += other.december;
    }
}
