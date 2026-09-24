/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

/**
 * Thrown by {@link PharmacyPurchaseOrderApprovalCancellationService#cancelApproval}
 * when a Pharmacy Purchase Order Approval bill cannot be cancelled. Carries a
 * {@link Reason} so callers (the JSF UI and the REST API) can each map the
 * failure to their own presentation - error message text in one case, an
 * HTTP status code in the other - without parsing the message string.
 *
 * @author Buddhika
 */
public class PharmacyPoCancellationException extends Exception {

    public enum Reason {
        /**
         * The approval bill (or the request bill for a status lookup) could
         * not be found for the given id.
         */
        NOT_FOUND,
        /**
         * The bill resolved for the given id is not a Pharmacy Purchase
         * Order Approval bill (BillType.PharmacyOrderApprove).
         */
        NOT_APPROVAL_TYPE,
        /**
         * The approval bill has already been cancelled.
         */
        ALREADY_CANCELLED,
        /**
         * The 'Pharmacy Purchase Order Bill can be Cancelled' config option
         * is switched off.
         */
        CONFIG_DISABLED,
        /**
         * A GRN already exists against this approval - mirrors the guard in
         * {@code PharmacyBillSearch.checkGrn()}.
         */
        GRN_EXISTS
    }

    private final Reason reason;

    public PharmacyPoCancellationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
