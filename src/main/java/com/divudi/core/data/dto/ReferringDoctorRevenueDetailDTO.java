package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Referring Doctor Wise Revenue Detail Report.
 * Replaces entity-based BillItem loading for improved performance.
 */
public class ReferringDoctorRevenueDetailDTO implements Serializable {

    private Long billItemId;
    private Long billId;
    private String deptId;
    private Boolean cancelled;
    private Boolean refunded;
    private String referringDoctorName;
    private String itemName;
    private Date createdAt;
    private Double collectingCentreFee;
    private Double hospitalFee;
    private Double staffFee;
    private Double discount;
    private Double netValue;

    public ReferringDoctorRevenueDetailDTO() {
    }

    public ReferringDoctorRevenueDetailDTO(
            Long billItemId,
            Long billId,
            String deptId,
            Boolean cancelled,
            Boolean refunded,
            String referringDoctorName,
            String itemName,
            Date createdAt,
            Double collectingCentreFee,
            Double hospitalFee,
            Double staffFee,
            Double discount,
            Double netValue) {
        this.billItemId = billItemId;
        this.billId = billId;
        this.deptId = deptId;
        this.cancelled = cancelled != null ? cancelled : false;
        this.refunded = refunded != null ? refunded : false;
        this.referringDoctorName = referringDoctorName;
        this.itemName = itemName;
        this.createdAt = createdAt;
        this.collectingCentreFee = collectingCentreFee != null ? collectingCentreFee : 0.0;
        this.hospitalFee = hospitalFee != null ? hospitalFee : 0.0;
        this.staffFee = staffFee != null ? staffFee : 0.0;
        this.discount = discount != null ? discount : 0.0;
        this.netValue = netValue != null ? netValue : 0.0;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }

    public String getReferringDoctorName() {
        return referringDoctorName;
    }

    public void setReferringDoctorName(String referringDoctorName) {
        this.referringDoctorName = referringDoctorName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Double getCollectingCentreFee() {
        return collectingCentreFee;
    }

    public void setCollectingCentreFee(Double collectingCentreFee) {
        this.collectingCentreFee = collectingCentreFee;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getStaffFee() {
        return staffFee;
    }

    public void setStaffFee(Double staffFee) {
        this.staffFee = staffFee;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }
}
