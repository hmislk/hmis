/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author buddhika
 */
public enum CreditDuration {

    D30,
    D60,
    D90;

    public String getLabel() {
        switch (this) {
            case D30:
                return "30 days";
            case D60:
                return "60 days";
            case D90:
                return "90 days";

            default:
                return this.toString();
        }
    }
}




