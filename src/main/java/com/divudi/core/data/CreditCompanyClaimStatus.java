package com.divudi.core.data;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Life cycle of an OPD credit company claim (#25847).
 */
public enum CreditCompanyClaimStatus {

    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    REJECTED("Rejected"),
    RESUBMITTED("Resubmitted"),
    PARTIALLY_SETTLED("Partially Settled"),
    SETTLED("Settled"),
    CLOSED("Closed"),
    CANCELLED("Cancelled");

    private final String label;

    CreditCompanyClaimStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Statuses a claim may move to from this one.
     */
    public Set<CreditCompanyClaimStatus> getAllowedNext() {
        switch (this) {
            case DRAFT:
                return EnumSet.of(SUBMITTED, CANCELLED);
            case SUBMITTED:
            case RESUBMITTED:
                return EnumSet.of(REJECTED, PARTIALLY_SETTLED, SETTLED, CLOSED);
            case REJECTED:
                return EnumSet.of(RESUBMITTED, CLOSED, CANCELLED);
            case PARTIALLY_SETTLED:
                return EnumSet.of(SETTLED, CLOSED);
            case SETTLED:
            case CLOSED:
            case CANCELLED:
            default:
                return EnumSet.noneOf(CreditCompanyClaimStatus.class);
        }
    }

    /**
     * Statuses the system may move a claim to when settlement bills are saved or
     * cancelled (#25847). Separate from {@link #getAllowedNext()}, which is what a
     * user may do. Settled can reopen when a settlement is cancelled.
     */
    public Set<CreditCompanyClaimStatus> getSystemNext() {
        switch (this) {
            case SUBMITTED:
            case RESUBMITTED:
                return EnumSet.of(PARTIALLY_SETTLED, SETTLED);
            case PARTIALLY_SETTLED:
                return EnumSet.of(SETTLED, SUBMITTED, RESUBMITTED);
            case SETTLED:
                return EnumSet.of(PARTIALLY_SETTLED, SUBMITTED, RESUBMITTED);
            default:
                return EnumSet.noneOf(CreditCompanyClaimStatus.class);
        }
    }

    public boolean canSystemMoveTo(CreditCompanyClaimStatus next) {
        return next != null && getSystemNext().contains(next);
    }

    /** True when settlement changes can change this status (a claim already sent to the company). */
    public boolean followsSettlement() {
        return this == SUBMITTED || this == RESUBMITTED || this == PARTIALLY_SETTLED || this == SETTLED;
    }

    /**
     * The one place that says which claims can be cancelled. A draft, or a
     * rejected claim; one waiting for the company is not cancelled here.
     */
    public static boolean canCancel(CreditCompanyClaimStatus status) {
        return status == DRAFT || status == REJECTED;
    }

    public boolean canMoveTo(CreditCompanyClaimStatus next) {
        return next != null && getAllowedNext().contains(next);
    }

    /**
     * A claim in one of these statuses still holds its bills, so a bill cannot
     * be put into another claim.
     */
    public boolean isOpen() {
        return this != CANCELLED && this != CLOSED && this != SETTLED;
    }

    public static CreditCompanyClaimStatus[] openStatuses() {
        return Arrays.stream(values()).filter(CreditCompanyClaimStatus::isOpen)
                .toArray(CreditCompanyClaimStatus[]::new);
    }
}
