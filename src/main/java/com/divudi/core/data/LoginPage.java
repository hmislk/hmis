package com.divudi.core.data;

/**
 * Enum representing various page types
 *
 * Author: Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 * Contribution by OpenAI's ChatGPT for additional labels and method implementation.
 */
public enum LoginPage {
    HOME("Home"),
    CHANNELLING_QUEUE_PAGE("Channelling Queue Page"),
    CHANNELLING_TV_DISPLAY("Channelling TV Display"),
    OPD_QUEUE_PAGE("OPD Queue Page"),
    OPD_TOKEN_DISPLAY("OPD Token Display"),
    PHARMACY_TOKEN_DISPLAY("Pharmacy Token Display"),
    COURIER_LANDING_PAGE("Courier Landing Page"),
    LABORATORY_DOCTER_DASHBOARD("Laboratory Docter Dashboard");

    private final String label;

    LoginPage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
