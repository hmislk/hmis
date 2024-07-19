package com.divudi.data.analytics;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum ReportTemplateFilter {
    DATE("Date"),
    FROM_DATE("From Date"),
    TO_DATE("To Date"),
    INSTITUTION("Institution"),

    DEPARTMENT("Department"),
    FROM_INSTITUTION("From Institution"),
    FROM_DEPARTMENT("From Department"),
    TO_INSTITUTION("To Institution"),
    TO_DEPARTMENT("To Department"),
    
    ;

    private final String label;

    ReportTemplateFilter(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
