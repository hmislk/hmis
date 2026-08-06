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
    private String fromDepartmentName;
    private String staffName;
    private String comments;
    private String insId;

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

    public PharmacyTransferReceivedListDTO(Long billId, String deptId,
            Date createdAt, Boolean cancelled, String createrName, Double netTotal,
            String fromDepartmentName, String staffName, String comments) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.cancelled = cancelled != null ? cancelled : false;
        this.createrName = createrName;
        this.netTotal = netTotal != null ? netTotal : 0.0;
        this.fromDepartmentName = fromDepartmentName;
        this.staffName = staffName;
        this.comments = comments;
    }

    /**
     * Adds {@code insId} and cancelled-by/cancelled-at reporting (issue #20523)
     * for the {@code pharmacy_transfer_recieve.xhtml} composite, which the
     * previous 9-arg constructor above did not carry (it always left
     * cancelledByName/cancelledAt null and sourced comments from the bill's
     * own {@code comments} rather than the cancelled/refunded bill's). This
     * is a new constructor per CLAUDE.md rules — the old one is left as-is
     * for any other caller.
     */
    public PharmacyTransferReceivedListDTO(Long billId, String deptId, String insId,
            Date createdAt, Boolean cancelled, String createrName, Double netTotal,
            String cancelledByName, Date cancelledAt,
            String fromDepartmentName, String staffName, String comments) {
        this.billId = billId;
        this.deptId = deptId;
        this.insId = insId;
        this.createdAt = createdAt;
        this.cancelled = cancelled != null ? cancelled : false;
        this.createrName = createrName;
        this.netTotal = netTotal != null ? netTotal : 0.0;
        this.cancelledByName = cancelledByName;
        this.cancelledAt = cancelledAt;
        this.fromDepartmentName = fromDepartmentName;
        this.staffName = staffName;
        this.comments = comments;
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

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getInsId() {
        return insId;
    }

    public void setInsId(String insId) {
        this.insId = insId;
    }
}
