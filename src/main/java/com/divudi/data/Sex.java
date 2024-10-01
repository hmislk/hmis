/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

public enum Sex {
    Male("Male", "M"),
    Female("Female", "F"),
    Unknown("Unknown", "U"),
    Other("Other", "O");

    private final String label;
    private final String shortLabel;

    private Sex(String label, String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }
}
