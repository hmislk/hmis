package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Current due of all the bills of one claim (#25847), from a grouped query.
 */
public class CreditCompanyClaimDueDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long claimId;
    private final double due;

    public CreditCompanyClaimDueDto(Long claimId, Double due) {
        this.claimId = claimId;
        this.due = due == null ? 0.0 : due;
    }

    public Long getClaimId() {
        return claimId;
    }

    public double getDue() {
        return due;
    }
}
