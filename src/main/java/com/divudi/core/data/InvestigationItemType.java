package com.divudi.core.data;

/**
 * @author Buddhika
 */
public enum InvestigationItemType {

    Label,
    Value,
    Calculation,
    Flag,
    DynamicLabel,
    Css,
    Barcode,
    BarcodeVertical,
    QrCode,
    Investigation,
    Template,
    AntibioticList,
    Antibiotic,
    Image,
    ExternalImage,
    Html,
    MeasurementUnit,
    ReportImage,
    WarningFlag,
    /**
     * Free-text comment for one organism (antibiotic group) in a
     * Microbiology report. One value is created per organism when the
     * organism is added.
     */
    OrganismComment,
    @Deprecated
    List,
    @Deprecated
    Speciality,
    @Deprecated
    Staff,
    @Deprecated
    Person,
    @Deprecated
    User,
    @Deprecated
    Patient,
    @Deprecated
    ItemsCatetgory,
}
