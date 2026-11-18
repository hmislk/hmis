package com.divudi.service.otp;

import java.io.Serializable;
import java.util.Date;

/**
 * Outcome of sending a one-time code. Never contains the code.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public class OtpIssueResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        SENT,
        SEND_FAILED,
        RATE_LIMITED,
        INVALID_MOBILE
    }

    private final Status status;
    private final long retryAfterSeconds;
    private final Date expiresAt;

    private OtpIssueResult(Status status, long retryAfterSeconds, Date expiresAt) {
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
        this.expiresAt = expiresAt;
    }

    public static OtpIssueResult sent(Date expiresAt, long cooldownSeconds) {
        return new OtpIssueResult(Status.SENT, cooldownSeconds, expiresAt);
    }

    public static OtpIssueResult sendFailed() {
        return new OtpIssueResult(Status.SEND_FAILED, 0, null);
    }

    public static OtpIssueResult rateLimited(long retryAfterSeconds) {
        return new OtpIssueResult(Status.RATE_LIMITED, Math.max(1, retryAfterSeconds), null);
    }

    public static OtpIssueResult invalidMobile() {
        return new OtpIssueResult(Status.INVALID_MOBILE, 0, null);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSent() {
        return status == Status.SENT;
    }

    /**
     * Seconds until another code may be requested.
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }
}
