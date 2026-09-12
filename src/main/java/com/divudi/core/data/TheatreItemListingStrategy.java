/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data;

/**
 * Controls which items the theatre surgery service bill
 * (theater/inward_bill_surgery_service.xhtml) offers in its item autocomplete.
 *
 * Held per department on {@link com.divudi.core.entity.UserPreference}, so one
 * hospital can bill theatre consumables from its existing OPD/Inward service
 * master while another keeps a dedicated Theatre Service master.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum TheatreItemListingStrategy {

    THEATRE_SERVICES("Theatre Services Only"),
    ALL_SERVICES("All Services"),
    ITEMS_MAPPED_TO_LOGGED_DEPARTMENT("Services Mapped to the Logged Department");

    private final String label;

    TheatreItemListingStrategy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
