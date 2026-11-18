package com.divudi.service.otp;

import java.io.Serializable;

/**
 * Outcome of checking a one-time code.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public class OtpVerifyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        /** Correct code; it is now used up */
        VERIFIED,
        /** Wrong code (counts as a failed attempt on the account) */
        INVALID,
        /** The code has expired; request a new one */
        EXPIRED,
        /** No unused code exists; request one */
        NO_ACTIVE_CODE,
        /** This code had too many wrong entries and was closed; request a new one */
        TOO_MANY_ATTEMPTS
    }

    private final Status status;
    // Where the verified code was sent (only set when VERIFIED)
    private final String identifier;

    private OtpVerifyResult(Status status, String identifier) {
        this.status = status;
        this.identifier = identifier;
    }

    public static OtpVerifyResult of(Status status) {
        return new OtpVerifyResult(status, null);
    }

    public static OtpVerifyResult verified(String identifier) {
        return new OtpVerifyResult(Status.VERIFIED, identifier);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isVerified() {
        return status == Status.VERIFIED;
    }

    public String getIdentifier() {
        return identifier;
    }
}
