package com.divudi.data.analytics;

/**
 * Enum for defining columns of a report template.
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum ReportTemplateColumn {
    DATE("Date"),
    TIME("Time"),
    BILLTYPE("Bill Type"),
    INSTITUTION("Institution"),
    DEPARTMENT("Department"),
    BILLED_VALUE("Billed Value"),
    RETURNED_VALUE("Returned Value"),
    CANCELLED_VALUE("Cancelled Value");

    private final String label;

    ReportTemplateColumn(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
