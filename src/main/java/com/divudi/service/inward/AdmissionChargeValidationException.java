/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

/**
 * Raised when an {@code AdmissionChargeItem} payload breaks one of the rules
 * enforced by {@link AdmissionChargeApiService} (issue #23594).
 *
 * <p>The message is written for the person configuring the charge, not for a
 * log: the REST API returns it verbatim as the body of a 400 - one rule, one
 * wording.</p>
 *
 * @author Buddhika
 */
public class AdmissionChargeValidationException extends Exception {

    public AdmissionChargeValidationException(String message) {
        super(message);
    }
}
