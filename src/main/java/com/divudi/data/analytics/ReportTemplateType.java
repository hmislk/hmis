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
    ENCOUNTER_LIST("Encounter List"),
    BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS("Bill Type Summary by using Bills"),
    BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_USING_BILLS("Bill Type Summary by using Bills"),
    PAYMENT_TYPE_SUMMARY_USING_BILLS("Payment Method Summary by using Bills"),
    BILL_TYPE_ATOMIC_SUMMARY_USING_PAYMENTS("Bill Type Summary by using Payments"),
    BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_PAYMENTS("Bill Type Summary by using Payments"),
    PAYMENT_METHOD_SUMMARY_USING_PAYMENTS("Bill Type Summary by using Payments"),
    PAYMENT_METHOD_SUMMARY_USING_BILLS("Bill Type Summary by using Payments"),
    PAYMENT_TYPE_SUMMARY_PAYMENTS("Payment Method Summary by using Payments"),;

    private final String label;

    ReportTemplateType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
