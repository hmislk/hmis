package com.divudi.data.analytics;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum ReportTemplateType {
    BILL_LIST("Bill List"),
    BILL_ITEM_LIST("Bill Item List"),
    BILL_FEE_LIST("Bill Fee List"),
    PATIENT_LIST("Patient List"),
    ENCOUNTER_LIST("Encounter List");

    private final String label;

    ReportTemplateType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
