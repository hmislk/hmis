/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author Buddhika
 */
public enum CollectingCentrePaymentMethod {

    FULL_PAYMENT_WITH_COMMISSION,
    PAYMENT_WITHOUT_COMMISSION;

    public String getLabel() {
        return this.toString();
    }
}
