package com.divudi.data.analytics;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum ReportTemplateType {
    BILL_LIST("Bill List"),
    BILL_NET_TOTAL("Bill Net Total"),
    BILL_ITEM_LIST("Bill Item List"),
    BILL_FEE_LIST("Bill Fee List"),
    PATIENT_LIST("Patient List"),
    SESSION_INSTANCE_LIST("Channelling Session Instance List"),
    ENCOUNTER_LIST("Encounter List"),
    BILL_FEE_GROUPED_BY_TO_DEPARTMENT_AND_CATEGORY("Bill fees grouped by to departmetnt and category"),
    BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS("Bill Type Summary by using Bills"),
    BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_USING_BILLS("Bill Type Summary by using Bills"),
    PAYMENT_TYPE_SUMMARY_USING_BILLS("Payment Method Summary by using Bills"),
    BILL_TYPE_ATOMIC_SUMMARY_USING_PAYMENTS("Bill Type Summary by using Payments"),
    BILL_TYPE_ATOMIC_SUMMARY_USING_FEES("Bill Type Summary usinf Bill Fees"),
    BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_PAYMENTS("Bill Type Summary by using Payments"),
    PAYMENT_METHOD_SUMMARY_USING_PAYMENTS("Bill Type Summary by using Payments"),
    PAYMENT_METHOD_SUMMARY_USING_BILLS("Bill Type Summary by using Payments"),
    PAYMENT_TYPE_SUMMARY_PAYMENTS("Payment Method Summary by using Payments"),
    ITEM_CATEGORY_SUMMARY_BY_BILL_FEE("Item Category Summary by Bill Fee"),
    ITEM_CATEGORY_SUMMARY_BY_BILL_ITEM("Item Category Summary by Bill Item"),
    ITEM_DEPARTMENT_SUMMARY_BY_BILL_ITEM("Item Department Summary by Bill Item"),
    ITEM_CATEGORY_SUMMARY_BY_BILL("Item Category Summary by Bill"),
    ITEM_SUMMARY_BY_BILL("Item Summary by Bill"),
    TO_DEPARTMENT_SUMMARY_BY_BILL_FEE("To Department Summary by Bill Fee"),
    TO_DEPARTMENT_SUMMARY_BY_BILL_ITEM("To Department Summary by Bill Item"),
    TO_DEPARTMENT_SUMMARY_BY_BILL("To Department Summary by Bill");

    ;

    private final String label;

    ReportTemplateType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
