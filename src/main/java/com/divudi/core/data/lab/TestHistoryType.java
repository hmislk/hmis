/*
 * By
 * Dr M H B Ariyaratne | <buddhika.ari@gmail.com>
 * H.K. Damitha Deshan | <hkddrajapaksha@gmail.com>
 */
package com.divudi.core.data.lab;

/**
 * Enum representing different stages of laboratory test history. ChatGPT
 * contributed.
 */
public enum TestHistoryType {
    ORDERED,
    BARCODE_PRINTED,
    SAMPLE_COLLECTED,
    SAMPLE_ACCEPTED,
    SAMPLE_SENT,
    SAMPLE_SENT_OUT_LAB,
    SAMPLE_REJECTED,
    SAMPLE_RECEIVED,
    SAMPLE_TRANSFERRED,
    SAMPLE_PUT_TO_ANALYZER,
    RESULT_RECEIVED_FROM_ANALYZER,
    REPORT_CREATED,
    DATA_ENTERED,
    REPORT_CORRECTED,
    REPORT_APPROVED,
    REPORT_VIEWED,
    REPORT_DISPATCHED;

    public String getLabel() {
        switch (this) {
            case ORDERED:
                return "Bill Created";
            case BARCODE_PRINTED:
                return "Barcode Printed";
            case SAMPLE_COLLECTED:
                return "Sample Collected";
            case SAMPLE_ACCEPTED:
                return "Sample Accepted";
            case SAMPLE_SENT:
                return "Sample Sent to Lab";
            case SAMPLE_SENT_OUT_LAB:
                return "Sample Sent to External Lab";
            case SAMPLE_REJECTED:
                return "Sample Rejected";
            case SAMPLE_RECEIVED:
                return "Sample Received";
            case SAMPLE_TRANSFERRED:
                return "Sample Transferred";
            case SAMPLE_PUT_TO_ANALYZER:
                return "Sample Put to Analyzer";
            case RESULT_RECEIVED_FROM_ANALYZER:
                return "Result Received from Analyzer";
            case REPORT_CREATED:
                return "Report Created";
            case REPORT_APPROVED:
                return "Report Approved";
            case REPORT_CORRECTED:
                return "Report Corrected";
            case REPORT_VIEWED:
                return "Report Viewed";
            case REPORT_DISPATCHED:
                return "Report Dispatched";
            default:
                return this.toString();
        }
    }
}
