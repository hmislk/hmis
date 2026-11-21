package com.divudi.core.data.dto;

import com.divudi.core.data.CreditCompanyClaimStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Credit Company Claims list (#25847).
 */
public class CreditCompanyClaimRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String claimNo;
    private String companyName;
    private CreditCompanyClaimStatus status;
    private double claimedTotal;
    private Date createdAt;
    private Date submittedAt;
    private String companyReference;
    private int resubmitCount;

    public CreditCompanyClaimRowDto(Long id, String claimNo, String companyName, CreditCompanyClaimStatus status,
            double claimedTotal, Date createdAt, Date submittedAt, String companyReference, int resubmitCount) {
        this.id = id;
        this.claimNo = claimNo;
        this.companyName = companyName;
        this.status = status;
        this.claimedTotal = claimedTotal;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
        this.companyReference = companyReference;
        this.resubmitCount = resubmitCount;
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

    public CreditCompanyClaimStatus getStatus() {
        return status;
    }

    public double getClaimedTotal() {
        return claimedTotal;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public String getCompanyReference() {
        return companyReference;
    }

    public int getResubmitCount() {
        return resubmitCount;
    }
}
