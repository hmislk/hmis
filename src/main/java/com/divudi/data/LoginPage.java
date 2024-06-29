package com.divudi.data;

/**
 * Enum representing various page types
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 * Contribution by OpenAI's ChatGPT for additional labels and method implementation.
 */
public enum LoginPage {
    HOME("Home"),
    CHANNELLING_QUEUE_PAGE("Channelling Queue Page"),
    CHANNELLING_TV_DISPLAY("Channelling TV Display"),
    OPD_QUEUE_PAGE("OPD Queue Page"),
    OPD_TOKEN_DISPLAY("OPD Token Display"),
    PHARMACY_TOKEN_DISPLAY("Pharmacy Token Display");

    private final String label;

    /**
     * Constructor for LoginPage enum to set the user-friendly label.
     *
     * @param label the user-friendly name of the page type
     */
    LoginPage(String label) {
        this.label = label;
    }

    /**
     * Returns the user-friendly name of the page type.
     *
     * @return the label of the enum constant
     */
    public String getLabel() {
        return label;
    }
}
