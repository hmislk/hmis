/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum CollectingCentrePaymentMethod {
    FULL_PAYMENT_WITH_COMMISSION,
    DEDUCTED_PAYMENT;

    public String getLabel(){
        return this.toString();
    }
    
}