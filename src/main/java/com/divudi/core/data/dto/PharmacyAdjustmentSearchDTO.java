package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy adjustment bill search (issue #21016 / #20299).
 * Eliminates N+1 lazy-load storms from the old Bill-entity approach.
 */
public class PharmacyAdjustmentSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private BillTypeAtomic billTypeAtomic;
    private String departmentName;
    private Date createdAt;
    private String creatorName;
    private String comments;
    private Double netTotal;
    private boolean cancelled;
    private Date cancelledAt;
    private String cancelledBy;
    private String cancelComments;

    public PharmacyAdjustmentSearchDTO() {
    }

    public PharmacyAdjustmentSearchDTO(
            Long id,
            String deptId,
            BillTypeAtomic billTypeAtomic,
            String departmentName,
            Date createdAt,
            String creatorName,
            String comments,
            Double netTotal,
            Boolean cancelled,
            Date cancelledAt,
            String cancelledBy,
            String cancelComments) {
        this.id = id;
        this.deptId = deptId;
        this.billTypeAtomic = billTypeAtomic;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.comments = comments;
        this.netTotal = netTotal;
        this.cancelled = cancelled != null ? cancelled : false;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancelComments = cancelComments;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public BillTypeAtomic getBillTypeAtomic() { return billTypeAtomic; }
    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) { this.billTypeAtomic = billTypeAtomic; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Double getNetTotal() { return netTotal; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancelComments() { return cancelComments; }
    public void setCancelComments(String cancelComments) { this.cancelComments = cancelComments; }
}
