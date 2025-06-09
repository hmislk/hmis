/*
 * By
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 * amd
 * Damitha Deshan
 */
package com.divudi.core.data.lab;

/**
 * Enum representing different stages of laboratory test history.
 * ChatGPT contributed.
 */
public enum TestHistoryType {
    ORDERED,
    BARCODE_PRINTED,
    SAMPLE_COLLECTED,
    SAMPLE_ACCEPTED,
    SAMPLE_REJECTED,
    SAMPLE_SENT,
    SAMPLE_RECEIVED,
    SAMPLE_TRANSFERRED,
    SAMPLE_PUT_TO_ANALYZER,
    RESULT_RECEIVED_FROM_ANALYZER,
    REPORT_CREATED,
    REPORT_APPROVED,
    REPORT_CORRECTED,
    REPORT_VIEWED,
    REPORT_DISPATCHED
}
