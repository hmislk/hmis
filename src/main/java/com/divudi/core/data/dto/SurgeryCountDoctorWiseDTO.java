package com.divudi.core.data.dto;

import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import java.util.Date;
import java.util.List;

public class SurgeryCountDoctorWiseDTO {

    private List<BillFee> billfees;
    private Date createdAt;
    private Staff staff;

    private String doctorName;
    private Speciality speciality;
    private String specialityName;

    private double january;
    private double february;
    private double march;
    private double april;
    private double may;
    private double june;
    private double july;
    private double august;
    private double september;
    private double october;
    private double november;
    private double december;

    public SurgeryCountDoctorWiseDTO() {
    }

    public SurgeryCountDoctorWiseDTO(Staff staff,
            String doctorName,
            String speciality,
            Date createdAt) {

        this.staff = staff;
        this.doctorName = doctorName;
        this.specialityName = speciality;
        this.createdAt = createdAt;
    }

    public SurgeryCountDoctorWiseDTO(String doctorName,
            Speciality speciality,
            double january,
            double february,
            double march,
            double april,
            double may,
            double june,
            double july,
            double august,
            double september,
            double october,
            double november,
            double december) {

        this.doctorName = doctorName;
        this.speciality = speciality;
        this.january = january;
        this.february = february;
        this.march = march;
        this.april = april;
        this.may = may;
        this.june = june;
        this.july = july;
        this.august = august;
        this.september = september;
        this.october = october;
        this.november = november;
        this.december = december;
    }

    // Getters and Setters
    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public double getJanuary() {
        return january;
    }

    public void setJanuary(double january) {
        this.january = january;
    }

    public double getFebruary() {
        return february;
    }

    public void setFebruary(double february) {
        this.february = february;
    }

    public double getMarch() {
        return march;
    }

    public void setMarch(double march) {
        this.march = march;
    }

    public double getApril() {
        return april;
    }

    public void setApril(double april) {
        this.april = april;
    }

    public double getMay() {
        return may;
    }

    public void setMay(double may) {
        this.may = may;
    }

    public double getJune() {
        return june;
    }

    public void setJune(double june) {
        this.june = june;
    }

    public double getJuly() {
        return july;
    }

    public void setJuly(double july) {
        this.july = july;
    }

    public double getAugust() {
        return august;
    }

    public void setAugust(double august) {
        this.august = august;
    }

    public double getSeptember() {
        return september;
    }

    public void setSeptember(double september) {
        this.september = september;
    }

    public double getOctober() {
        return october;
    }

    public void setOctober(double october) {
        this.october = october;
    }

    public double getNovember() {
        return november;
    }

    public void setNovember(double november) {
        this.november = november;
    }

    public double getDecember() {
        return december;
    }

    public void setDecember(double december) {
        this.december = december;
    }

    public List<BillFee> getBillfees() {
        return billfees;
    }

    public void setBillfees(List<BillFee> billfees) {
        this.billfees = billfees;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }
}
