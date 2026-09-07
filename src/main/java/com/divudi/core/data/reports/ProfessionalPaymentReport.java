package com.divudi.core.data.reports;

public enum ProfessionalPaymentReport implements IReportType {
    OPD_PROFESSIONAL_FEE_PAYMENTS_REPORT("OPD Professional Fee Payments Report"),
    OPD_PROFESSIONAL_PAYMENTS_REPORT("OPD Professional Payments Report"),;


    private final String displayName;

    ProfessionalPaymentReport(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getReportType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getReportName() {
        return this.name();
    }
}
