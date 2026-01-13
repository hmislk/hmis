package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for Referring Doctor Wise Revenue Summary Report.
 * Replaces entity-based TestWiseCountReport with BillItem for improved performance.
 * Aggregates data at database level to avoid Java-side loops.
 */
public class ReferringDoctorRevenueSummaryDTO implements Serializable {

    private Long doctorId;
    private String doctorNameWithTitle;
    private String collectingCentreName;
    private Long count;
    private Double hosFee;
    private Double ccFee;
    private Double proFee;
    private Double discount;
    private Double netTotal;

    public ReferringDoctorRevenueSummaryDTO() {
    }

    public ReferringDoctorRevenueSummaryDTO(
            Long doctorId,
            String doctorNameWithTitle,
            String collectingCentreName,
            Long count,
            Double hosFee,
            Double ccFee,
            Double proFee,
            Double discount,
            Double netTotal) {
        this.doctorId = doctorId;
        this.doctorNameWithTitle = doctorNameWithTitle;
        this.collectingCentreName = collectingCentreName;
        this.count = count != null ? count : 0L;
        this.hosFee = hosFee != null ? hosFee : 0.0;
        this.ccFee = ccFee != null ? ccFee : 0.0;
        this.proFee = proFee != null ? proFee : 0.0;
        this.discount = discount != null ? discount : 0.0;
        this.netTotal = netTotal != null ? netTotal : 0.0;
    }

    // Convenience method for gross amount calculation
    public Double getGrossAmount() {
        return (netTotal != null ? netTotal : 0.0) + (discount != null ? discount : 0.0);
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorNameWithTitle() {
        return doctorNameWithTitle;
    }

    public void setDoctorNameWithTitle(String doctorNameWithTitle) {
        this.doctorNameWithTitle = doctorNameWithTitle;
    }

    public String getCollectingCentreName() {
        return collectingCentreName;
    }

    public void setCollectingCentreName(String collectingCentreName) {
        this.collectingCentreName = collectingCentreName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getHosFee() {
        return hosFee;
    }

    public void setHosFee(Double hosFee) {
        this.hosFee = hosFee;
    }

    public Double getCcFee() {
        return ccFee;
    }

    public void setCcFee(Double ccFee) {
        this.ccFee = ccFee;
    }

    public Double getProFee() {
        return proFee;
    }

    public void setProFee(Double proFee) {
        this.proFee = proFee;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }
}
