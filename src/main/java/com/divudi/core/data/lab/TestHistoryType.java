/*
 * By
 * Dr M H B Ariyaratne | <buddhika.ari@gmail.com>
 * H.K. Damitha Deshan | <hkddrajapaksha@gmail.com>
 */
package com.divudi.core.data.lab;

/**
 * Enum representing different stages of laboratory test history. ChatGPT contributed.
 */
public enum TestHistoryType {
    ORDERED,
    BARCODE_PRINTED,
    SAMPLE_COLLECTED,
    SAMPLE_ACCEPTED,
    SAMPLE_REJECTED,
    SAMPLE_SENT,
    SAMPLE_SENT_OUT_LAB,
    SAMPLE_RECEIVED,
    SAMPLE_TRANSFERRED,
    SAMPLE_PUT_TO_ANALYZER,
    RESULT_RECEIVED_FROM_ANALYZER,
    REPORT_CREATED,
    REPORT_APPROVED,
    REPORT_CORRECTED,
    REPORT_VIEWED,
    REPORT_DISPATCHED;
    
    public String getLabel() {
        switch (this) {
            case ORDERED:
                return "Bill Created";
            default:
                return "";
        }
    }
}
