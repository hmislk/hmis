package com.divudi.core.data.dto;

import java.util.Date;

public class SurgeryCountSurgeryWiseDTO {

    private Long surgeryId;
    private String surgeryName;
    private String surgeryCategory;
    private Date createdAt;

    // Monthly counts
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

    private int yearTotal;

    private boolean grandTotal;

    public SurgeryCountSurgeryWiseDTO() {
    }

    // Constructor for JPQL raw row fetch
    public SurgeryCountSurgeryWiseDTO(Long surgeryId, String surgeryName, String surgeryCategory, Date createdAt) {
        this.surgeryId = surgeryId;
        this.surgeryName = surgeryName;
        this.surgeryCategory = surgeryCategory;
        this.createdAt = createdAt;
    }

    public void addMonthCount(int monthIndex, int count) {
        switch (monthIndex) {
            case 0:
                january += count;
                break;
            case 1:
                february += count;
                break;
            case 2:
                march += count;
                break;
            case 3:
                april += count;
                break;
            case 4:
                may += count;
                break;
            case 5:
                june += count;
                break;
            case 6:
                july += count;
                break;
            case 7:
                august += count;
                break;
            case 8:
                september += count;
                break;
            case 9:
                october += count;
                break;
            case 10:
                november += count;
                break;
            case 11:
                december += count;
                break;
        }
    }

    public void calculateYearTotal() {
        yearTotal = january + february + march + april + may + june
                + july + august + september + october + november + december;
    }

    public void addAllCounts(SurgeryCountSurgeryWiseDTO other) {
        this.january += other.getJanuary();
        this.february += other.getFebruary();
        this.march += other.getMarch();
        this.april += other.getApril();
        this.may += other.getMay();
        this.june += other.getJune();
        this.july += other.getJuly();
        this.august += other.getAugust();
        this.september += other.getSeptember();
        this.october += other.getOctober();
        this.november += other.getNovember();
        this.december += other.getDecember();
        this.yearTotal += other.getYearTotal();
    }

    // Getters and Setters

    public Long getSurgeryId() {
        return surgeryId;
    }

    public void setSurgeryId(Long surgeryId) {
        this.surgeryId = surgeryId;
    }

    public String getSurgeryName() {
        return surgeryName;
    }

    public void setSurgeryName(String surgeryName) {
        this.surgeryName = surgeryName;
    }

    public String getSurgeryCategory() {
        return surgeryCategory;
    }

    public void setSurgeryCategory(String surgeryCategory) {
        this.surgeryCategory = surgeryCategory;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public int getYearTotal() {
        return yearTotal;
    }

    public void setYearTotal(int yearTotal) {
        this.yearTotal = yearTotal;
    }

    public boolean isGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(boolean grandTotal) {
        this.grandTotal = grandTotal;
    }
}