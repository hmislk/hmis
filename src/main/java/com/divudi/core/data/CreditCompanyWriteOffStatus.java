package com.divudi.core.data;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life cycle of a write-off of an OPD credit bill's due (#25847). One user
 * requests, a different user with the approve privilege decides. Only an
 * APPROVED write-off reduces the bill's due.
 */
public enum CreditCompanyWriteOffStatus {

    REQUESTED("Requested"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String label;

    CreditCompanyWriteOffStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Statuses a write-off may move to from this one. */
    public Set<CreditCompanyWriteOffStatus> getAllowedNext() {
        switch (this) {
            case REQUESTED:
                return EnumSet.of(APPROVED, REJECTED);
            case APPROVED:
                return EnumSet.of(CANCELLED);
            case REJECTED:
            case CANCELLED:
            default:
                return EnumSet.noneOf(CreditCompanyWriteOffStatus.class);
        }
    }

    public boolean canMoveTo(CreditCompanyWriteOffStatus next) {
        return next != null && getAllowedNext().contains(next);
    }
}
