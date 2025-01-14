package com.divudi.data;

/**
- *
- * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
- *
*/

public enum ReportType {
    GENARATE("Genarate"),
    UPLOAD("Upload"),
    OTHER("Other");

    private final String label;

    private ReportType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
