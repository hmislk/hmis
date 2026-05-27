package com.divudi.core.data;

/**
 * Logical groups for organizing user icons on the home page and in the admin
 * icon tree.
 *
 * @author Dr M H B Ariyaratne
 */
public enum IconGroup {

    PATIENT_MANAGEMENT("Patient Management", 1),
    OPD_BILLING("OPD Billing", 2),
    PAYMENTS_AND_REFUNDS("Payments and Refunds", 3),
    DOCTOR_AND_CHANNEL("Doctor and Channel", 4),
    LAB_AND_REPORTS("Lab and Reports", 5),
    OPD_SUMMARIES("OPD Summaries", 6),
    PHARMACY_SALES("Pharmacy Sales", 7),
    PHARMACY_RETURNS("Pharmacy Returns", 8),
    PHARMACY_STOCK("Pharmacy Stock", 9),
    PHARMACY_REPORTS("Pharmacy Reports", 10),
    WHOLESALE("Wholesale", 11),
    INPATIENT_DIRECT_ISSUES("Inpatient Direct Issues", 12),
    PURCHASING("Purchasing", 13),
    GOODS_RECEIPT("Goods Receipt", 14),
    STOCK_ADJUSTMENTS("Stock Adjustments", 15),
    SUPPLIER_PAYMENTS("Supplier Payments", 16),
    CASHIER("Cashier", 17),
    INPATIENT_ADMISSIONS("Inpatient Admissions", 18),
    INPATIENT_ROOMS("Inpatient Rooms", 19),
    INPATIENT_SERVICES("Inpatient Services", 20),
    INPATIENT_BILLING("Inpatient Billing", 21),
    INPATIENT_ANALYTICS("Inpatient Analytics", 22),
    OPTICIAN("Optician", 23);

    private final String label;
    private final int orderNumber;

    IconGroup(String label, int orderNumber) {
        this.label = label;
        this.orderNumber = orderNumber;
    }

    public String getLabel() {
        return label;
    }

    public int getOrderNumber() {
        return orderNumber;
    }
}
