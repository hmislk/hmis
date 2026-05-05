package com.divudi.core.entity.hr;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "SHIFTSTAFFREQUIREMENT")
public class ShiftStaffRequirement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", unique = true)
    private Shift shift;

    private Integer mondayCount;
    private Integer tuesdayCount;
    private Integer wednesdayCount;
    private Integer thursdayCount;
    private Integer fridayCount;
    private Integer saturdayCount;
    private Integer sundayCount;
    private Integer poyaDayCount;
    private Integer publicHolidayCount;
    private Integer mercantileHolidayCount;

    public ShiftStaffRequirement() {
        this.mondayCount = 0;
        this.tuesdayCount = 0;
        this.wednesdayCount = 0;
        this.thursdayCount = 0;
        this.fridayCount = 0;
        this.saturdayCount = 0;
        this.sundayCount = 0;
        this.poyaDayCount = 0;
        this.publicHolidayCount = 0;
        this.mercantileHolidayCount = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public Integer getMondayCount() {
        return mondayCount;
    }

    public void setMondayCount(Integer mondayCount) {
        this.mondayCount = mondayCount;
    }

    public Integer getTuesdayCount() {
        return tuesdayCount;
    }

    public void setTuesdayCount(Integer tuesdayCount) {
        this.tuesdayCount = tuesdayCount;
    }

    public Integer getWednesdayCount() {
        return wednesdayCount;
    }

    public void setWednesdayCount(Integer wednesdayCount) {
        this.wednesdayCount = wednesdayCount;
    }

    public Integer getThursdayCount() {
        return thursdayCount;
    }

    public void setThursdayCount(Integer thursdayCount) {
        this.thursdayCount = thursdayCount;
    }

    public Integer getFridayCount() {
        return fridayCount;
    }

    public void setFridayCount(Integer fridayCount) {
        this.fridayCount = fridayCount;
    }

    public Integer getSaturdayCount() {
        return saturdayCount;
    }

    public void setSaturdayCount(Integer saturdayCount) {
        this.saturdayCount = saturdayCount;
    }

    public Integer getSundayCount() {
        return sundayCount;
    }

    public void setSundayCount(Integer sundayCount) {
        this.sundayCount = sundayCount;
    }

    public Integer getPoyaDayCount() {
        return poyaDayCount;
    }

    public void setPoyaDayCount(Integer poyaDayCount) {
        this.poyaDayCount = poyaDayCount;
    }

    public Integer getPublicHolidayCount() {
        return publicHolidayCount;
    }

    public void setPublicHolidayCount(Integer publicHolidayCount) {
        this.publicHolidayCount = publicHolidayCount;
    }

    public Integer getMercantileHolidayCount() {
        return mercantileHolidayCount;
    }

    public void setMercantileHolidayCount(Integer mercantileHolidayCount) {
        this.mercantileHolidayCount = mercantileHolidayCount;
    }
}