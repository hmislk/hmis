/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data;

public enum Sex {
    Male("Male", "M"),
    Female("Female", "F"),
    Unknown("Unknown", "U"),
    Other("Other", "O");

    private final String label;
    private final String shortLabel;

    Sex(String label, String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public static Sex getByLabelOrShortLabel(String input) {
        if (input != null) {
            for (Sex sex : Sex.values()) {
                if (sex.label.equalsIgnoreCase(input) || sex.shortLabel.equalsIgnoreCase(input)) {
                    return sex;
                }
            }
            if (input.toLowerCase().contains("f")) {
                return Sex.Female;
            }
        }
        return Sex.Male; // Default return value if no exact match or "f" found
    }

}
