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
                return EnumSet.of(RESUBMITTED, CLOSED);
            case PARTIALLY_SETTLED:
                return EnumSet.of(SETTLED, CLOSED);
            case SETTLED:
            case CLOSED:
            case CANCELLED:
            default:
                return EnumSet.noneOf(CreditCompanyClaimStatus.class);
        }
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
