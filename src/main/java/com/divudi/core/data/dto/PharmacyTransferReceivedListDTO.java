package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class PharmacyTransferReceivedListDTO implements Serializable {

    private Long billId;
    private Long issuedBillId;
    private String deptId;
    private Date createdAt;
    private Boolean cancelled;
    private String createrName;
    private Double netTotal;
    private String cancelledByName;
    private Date cancelledAt;

    public PharmacyTransferReceivedListDTO() {
    }

    public PharmacyTransferReceivedListDTO(Long billId, Long issuedBillId, String deptId, Boolean cancelled) {
        this(billId, issuedBillId, deptId, null, cancelled, null, null, null, null);
    }

    public PharmacyTransferReceivedListDTO(Long billId, Long issuedBillId, String deptId,
            Date createdAt, Boolean cancelled, String createrName, Double netTotal,
            String cancelledByName, Date cancelledAt) {
        this.billId = billId;
        this.issuedBillId = issuedBillId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.cancelled = cancelled != null ? cancelled : false;
        this.createrName = createrName;
        this.netTotal = netTotal != null ? netTotal : 0.0;
        this.cancelledByName = cancelledByName;
        this.cancelledAt = cancelledAt;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Long getIssuedBillId() {
        return issuedBillId;
    }

    public void setIssuedBillId(Long issuedBillId) {
        this.issuedBillId = issuedBillId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getCreaterName() {
        return createrName;
    }

    public void setCreaterName(String createrName) {
        this.createrName = createrName;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public String getCancelledByName() {
        return cancelledByName;
    }

    public void setCancelledByName(String cancelledByName) {
        this.cancelledByName = cancelledByName;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
