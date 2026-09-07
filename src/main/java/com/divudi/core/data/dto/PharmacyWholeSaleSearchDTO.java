package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy wholesale bill search list (issue #21010 / #20299).
 * Eliminates N+1 lazy-load storms from the old Bill-entity approach.
 */
public class PharmacyWholeSaleSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private Date createdAt;
    private String creatorName;
    private String comments;
    private boolean cancelled;

    public PharmacyWholeSaleSearchDTO() {
    }

    public PharmacyWholeSaleSearchDTO(
            Long id,
            String deptId,
            Date createdAt,
            String creatorName,
            String comments,
            Boolean cancelled) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.comments = comments;
        this.cancelled = cancelled != null ? cancelled : false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
