package com.divudi.core.data.dto;

import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.service.CreditCompanyClaimReportMath;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Claims by Status report (#25847). The outstanding amount is
 * filled in after the query from one batched bill-due lookup.
 */
public class CreditCompanyClaimStatusReportRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String claimNo;
    private final String companyName;
    private final String departmentName;
    private final Date createdAt;
    private final Date submittedAt;
    private final CreditCompanyClaimStatus status;
    private final int resubmitCount;
    private final double claimedTotal;
    private double outstanding;

    public CreditCompanyClaimStatusReportRowDto(Long id, String claimNo, String companyName, String departmentName,
            Date createdAt, Date submittedAt, CreditCompanyClaimStatus status, int resubmitCount, double claimedTotal) {
        this.id = id;
        this.claimNo = claimNo;
        this.companyName = companyName;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
        this.status = status;
        this.resubmitCount = resubmitCount;
        this.claimedTotal = claimedTotal;
    }

    public Long getId() {
        return id;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public CreditCompanyClaimStatus getStatus() {
        return status;
    }

    public int getResubmitCount() {
        return resubmitCount;
    }

    public double getClaimedTotal() {
        return claimedTotal;
    }

    public double getOutstanding() {
        return outstanding;
    }

    public void setOutstanding(double outstanding) {
        this.outstanding = outstanding;
    }

    /** Claimed less outstanding; 0 for a cancelled claim, whose bills were freed. */
    public double getReceivedOrClosed() {
        if (isCancelled()) {
            return 0.0;
        }
        return CreditCompanyClaimReportMath.receivedOrClosed(claimedTotal, outstanding);
    }

    /** A cancelled claim no longer holds its bills, so it has no outstanding and is left out of totals. */
    public boolean isCancelled() {
        return status == CreditCompanyClaimStatus.CANCELLED;
    }
}
