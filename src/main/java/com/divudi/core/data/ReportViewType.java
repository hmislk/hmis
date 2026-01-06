package com.divudi.core.data;

/**
 * Enum to define different report groupings for income reports in HIMS. ChatGPT
 * contributed version with UI-friendly labels.
 * 
 * @author buddhika
 */
public enum ReportViewType {
    BY_BILL("By Bill"),
    BY_BILL_ITEM("By Bill Item"),
    BY_ITEM("By Item"),
    BY_BILL_TYPE("By Bill Type"),
    BY_PAYMENT_METHOD("By Payment Method"),
    BY_INSTITUTION("By Institution"),
    BY_DEPARTMENT("By Department"),
    BY_DISCOUNT_TYPE_AND_ADMISSION_TYPE("By Discount Type and Admission Type"),
    BY_BILL_TYPE_AND_DISCOUNT_TYPE_AND_ADMISSION_TYPE("By Bill Type + Discount Type and Admission Type"), // ChatGPT contribution
    BY_STAFF("By Staff"),
    BY_PATIENT_CATEGORY("By Patient Category"),
    BY_ITEM_CATEGORY("By Item Category"),
    BY_DATE("By Date"),
    BY_USER("By User");

    private final String label;

    private ReportViewType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
