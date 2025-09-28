/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.lab;

/**
 * Enum representing various analyzers with a label for each.
 */
public enum Analyzer {
    Sysmex_XS_Series("Sysmex XS Series"),
    Dimension_Clinical_Chemistry_System("Dimension Clinical Chemistry System"),
    Gallery_Indiko("Gallery Indiko"),
    Celltac_MEK("Celltac MEK"),
    BA400("BA 400"),
    IndikoPlus("Indiko Plus"),
    BioRadD10("Bio-Rad D10"),
    MindrayBC5150("Mindray BC 5150"),
    SmartLytePlus("SmartLyte Plus"),
    SwelabLumi("Swelab Lumi"),
    HumaCount5D("HumaCount5D"),
    HumaLyte("HumaLyte"),
    HumaStar600("HumaStar600"),
    MaglumiX3HL7("Maglumi X3 HL7"),
    MindrayCL1000i("MindrayCL1000i"),
    XL_200("XL 200"),
    AIA_360("AIA 360");

    private final String label;

    // Constructor to set the enum labels
    Analyzer(String label) {
        this.label = label;
    }

    // Getter for the label
    public String getLabel() {
        return this.label;
    }

    /**
     * Converts a string identifier to an Analyzer enum.
     *
     * @param text the string representation of the analyzer
     * @return the corresponding Analyzer enum
     */
    public static Analyzer fromString(String text) {
        if (text != null) {
            for (Analyzer b : Analyzer.values()) {
                if (text.equalsIgnoreCase(b.label)) {
                    return b;
                }
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
