package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One row per referring doctor. cancelValue and returnValue are the (negative)
 * net totals of the cancellation and return bills, so netValue is a plain sum.
 */
public class ReferringDoctorSaleSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long doctorId;
    private String doctorName;
    private long saleCount;
    private double saleGross;
    private double saleDiscount;
    private double saleNet;
    private long cancelCount;
    private double cancelValue;
    private long returnCount;
    private double returnValue;
    private double netValue;

    public ReferringDoctorSaleSummaryDTO() {
    }

    public ReferringDoctorSaleSummaryDTO(Long doctorId, String doctorName) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public long getSaleCount() {
        return saleCount;
    }

    public void setSaleCount(long saleCount) {
        this.saleCount = saleCount;
    }

    public double getSaleGross() {
        return saleGross;
    }

    public void setSaleGross(double saleGross) {
        this.saleGross = saleGross;
    }

    public double getSaleDiscount() {
        return saleDiscount;
    }

    public void setSaleDiscount(double saleDiscount) {
        this.saleDiscount = saleDiscount;
    }

    public double getSaleNet() {
        return saleNet;
    }

    public void setSaleNet(double saleNet) {
        this.saleNet = saleNet;
    }

    public long getCancelCount() {
        return cancelCount;
    }

    public void setCancelCount(long cancelCount) {
        this.cancelCount = cancelCount;
    }

    public double getCancelValue() {
        return cancelValue;
    }

    public void setCancelValue(double cancelValue) {
        this.cancelValue = cancelValue;
    }

    public long getReturnCount() {
        return returnCount;
    }

    public void setReturnCount(long returnCount) {
        this.returnCount = returnCount;
    }

    public double getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(double returnValue) {
        this.returnValue = returnValue;
    }

    public double getNetValue() {
        return netValue;
    }

    public void setNetValue(double netValue) {
        this.netValue = netValue;
    }
}
