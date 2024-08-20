package com.divudi.data;

public enum BillItemStatus {

    ORDERING("Ordering"),
    ORDER_COMPLETED("Order Completed"),
    SAMPLE_COLLECTED("Sample Collected"),
    SAMPLE_RECEIVED_AT_LAB("Sample Received at Lab"),
    SAMPLE_UNDER_MANUAL_PROCESSING("Sample Under Manual Processing"),
    UPLOADED_TO_ANALYZER("Uploaded to Analyzer"),
    ANALYZER_RESULTS_AVAILABLE("Analyzer Results Available"),
    AWAITING_APPROVAL("Awaiting Approval"),
    AWAITING_AUTHORIZATION("Awaiting Authorization"),
    REPORT_PRINTED("Report Printed"),
    REPORT_COLLECTED_BY_PORTER("Report Collected by Porter"),
    REPORT_COLLECTED_BY_PATIENT("Report Collected by Patient"),
    OTHER("Other");

    private final String label;

    BillItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
