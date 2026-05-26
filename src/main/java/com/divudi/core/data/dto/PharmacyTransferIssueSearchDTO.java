package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for the pharmacy transfer issue search list.
 * Populated via JPQL constructor query to avoid full entity graph loading.
 * Cancellation details (canceller name/date) are omitted from the list view;
 * users open the bill via the View action to see full details.
 */
public class PharmacyTransferIssueSearchDTO implements Serializable {

    private Long billId;
    private String deptId;
    private Date createdAt;
    private String toDepartmentName;
    private String creatorName;
    private String transporterName;
    private Boolean cancelled;
    private Double netTotal;
    private String comments;

    public PharmacyTransferIssueSearchDTO() {
    }

    public PharmacyTransferIssueSearchDTO(Long billId, String deptId, Date createdAt,
            String toDepartmentName, String creatorName, String transporterName,
            Boolean cancelled, Double netTotal, String comments) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.toDepartmentName = toDepartmentName;
        this.creatorName = creatorName;
        this.transporterName = transporterName;
        this.cancelled = cancelled;
        this.netTotal = netTotal;
        this.comments = comments;
    }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getTransporterName() { return transporterName; }
    public void setTransporterName(String transporterName) { this.transporterName = transporterName; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public Double getNetTotal() { return netTotal != null ? netTotal : 0.0; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
