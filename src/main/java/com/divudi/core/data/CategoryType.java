/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 * 
 * Enum contributed by ChatGPT to support transition from subclass-based Category model.
 */
package com.divudi.core.data;

public enum CategoryType {
    ROUTE_OF_ADMINISTRATION("Route of Administration"),
    DESIGNATION("Designation"),
    GRADE("Grade"),
    STAFF_CATEGORY("Staff Category"),
    FINANCIAL_CATEGORY("Financial Category"),
    ADMISSION_TYPE("Admission Type"),
    ROOM("Room"),
    ROOM_CATEGORY("Room Category"),
    TIMED_ITEM_CATEGORY("Timed Item Category"),
    INVESTIGATION_CATEGORY("Investigation Category"),
    INVESTIGATION_ITEM_VALUE_CATEGORY("Investigation Item Value Category"),
    INVESTIGATION_TUBE("Investigation Tube"),
    MACHINE("Machine"),
    REPORT_FORMAT("Report Format"),
    SAMPLE("Sample"),
    WORKSHEET_FORMAT("Worksheet Format"),
    MEMBERSHIP_SCHEME("Membership Scheme"),
    ADJUSTMENT_CATEGORY("Adjustment Category"),
    DISCARD_CATEGORY("Discard Category"),
    FREQUENCY_UNIT("Frequency Unit"),
    MAKE("Make"),
    MEASUREMENT_UNIT("Measurement Unit"),
    PHARMACEUTICAL_CATEGORY("Pharmaceutical Category"),
    PHARMACEUTICAL_ITEM_CATEGORY("Pharmaceutical Item Category"),
    PHARMACEUTICAL_ITEM_TYPE("Pharmaceutical Item Type"),
    STORE_ITEM_CATEGORY("Store Item Category"),
    DOSAGE_FORM("Dosage Form"),
    FORM_FORMAT("Form Format"),
    INVENTORY_CATEGORY("Inventory Category"),
    METADATA_CATEGORY("Metadata Category"),
    METADATA_SUPER_CATEGORY("Metadata Super Category"),
    NATIONALITY("Nationality"),
    RELATION("Relation"),
    RELIGION("Religion"),
    SERVICE_CATEGORY("Service Category"),
    SERVICE_SUB_CATEGORY("Service Sub Category"),
    SESSION_NUMBER_GENERATOR("Session Number Generator"),
    SPECIALITY("Speciality"),
    VOCABULARY("Vocabulary");

    private final String label;

    private CategoryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
