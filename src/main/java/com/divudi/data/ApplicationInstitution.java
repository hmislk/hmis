package com.divudi.data;

public enum ApplicationInstitution {

    Ruhuna("Ruhuna Hospital"),
    Cooperative("Cooperative Clinic"),
    Arogya("Arogya Health Center"),
    Reliable("Reliable Medical Center"),
    KML("KML Hospital"),
    Roseth("Roseth Clinic"),
    Suwani("Suwani Hospital"),
    BMS("BMS Clinic"),
    Digasiri("Digasiri Health Center"),
    Probhodha("Probhodha Medical Center"),
    Sethma("Sethma Hospital"),
    Rmh("RMH Hospital");

    private final String label;

    ApplicationInstitution(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
