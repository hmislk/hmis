package com.divudi.core.data.dto;

import com.divudi.core.data.CreditCompanyClaimStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * An open, submitted claim as read for the Claim Ageing report (#25847).
 */
public class CreditCompanyClaimAgeingClaimDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long companyId;
    private final String companyName;
    private final Date submittedAt;
    private final CreditCompanyClaimStatus status;

    public CreditCompanyClaimAgeingClaimDto(Long id, Long companyId, String companyName, Date submittedAt,
            CreditCompanyClaimStatus status) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public CreditCompanyClaimStatus getStatus() {
        return status;
    }
}
