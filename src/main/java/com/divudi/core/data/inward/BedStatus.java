/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.inward;

/**
 * HL7 v2 Table 0116 bed status values.
 * Reference: https://terminology.hl7.org/CodeSystem-v2-0116.html
 */
public enum BedStatus {

    Available("A", "Available", "bed-status-available"),
    Occupied("O", "Occupied", "bed-status-occupied"),
    Housekeeping("H", "Housekeeping", "bed-status-housekeeping"),
    Contaminated("C", "Contaminated", "bed-status-contaminated"),
    Isolated("I", "Isolated", "bed-status-isolated"),
    OutOfOrder("U", "Out of Order", "bed-status-outoforder");

    private final String hl7Code;
    private final String displayLabel;
    private final String cssClass;

    BedStatus(String hl7Code, String displayLabel, String cssClass) {
        this.hl7Code = hl7Code;
        this.displayLabel = displayLabel;
        this.cssClass = cssClass;
    }

    public String getHl7Code() {
        return hl7Code;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getCssClass() {
        return cssClass;
    }
}
