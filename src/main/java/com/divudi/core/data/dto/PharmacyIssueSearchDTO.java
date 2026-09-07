package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy issue bill search (issue #21015 / #20299).
 * Eliminates N+1 lazy-load storms from the old Bill-entity approach.
 */
public class PharmacyIssueSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private String insId;
    private String toDepartmentName;
    private Date createdAt;
    private String creatorName;
    private boolean cancelled;
    private Date cancelledAt;
    private String cancelledBy;
    private String cancelComments;
    private Double netTotal;

    public PharmacyIssueSearchDTO() {
    }

    public PharmacyIssueSearchDTO(
            Long id,
            String deptId,
            String insId,
            String toDepartmentName,
            Date createdAt,
            String creatorName,
            Boolean cancelled,
            Date cancelledAt,
            String cancelledBy,
            String cancelComments,
            Double netTotal) {
        this.id = id;
        this.deptId = deptId;
        this.insId = insId;
        this.toDepartmentName = toDepartmentName;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.cancelled = cancelled != null ? cancelled : false;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancelComments = cancelComments;
        this.netTotal = netTotal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public String getInsId() { return insId; }
    public void setInsId(String insId) { this.insId = insId; }

    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancelComments() { return cancelComments; }
    public void setCancelComments(String cancelComments) { this.cancelComments = cancelComments; }

    public Double getNetTotal() { return netTotal; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }
}
