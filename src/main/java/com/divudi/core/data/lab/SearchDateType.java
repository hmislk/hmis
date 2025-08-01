package com.divudi.core.data.lab;

/**
 * Enum representing the various search date types.
 *
 * Author: M H B Ariyaratne
 */
public enum SearchDateType {

    ORDERED_DATE("Ordered Date"),
    SAMPLE_GENERATED_DATE("Sample Generated Date"),
    SAMPLE_COLLECTED_DATE("Sample Collected Date"),
    SAMPLE_SENT_DATE("Sample Sent Date"),
    SAMPLE_ACCEPTED_DATE("Sample Accepted Date"),
    REPORT_AUTHORIZED("Report Authorized"),
    REPORT_PRINTED("Report Printed");

    private final String label;

    SearchDateType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
