package com.divudi.ws.report;

import com.divudi.core.entity.lab.Investigation;

public class ViewInvestigationResponseDTO {
    private String investigationName;
    private String investigationFullName;
    private String sampleName;
    private String reportType;
    private String category;
    private String reportFormat;

    public ViewInvestigationResponseDTO(final Investigation investigation) {
        if (investigation == null) {
            this.investigationName = null;
            this.investigationFullName = null;
            this.sampleName = null;
            this.reportType = null;
            this.category = null;
            this.reportFormat = null;
            return;
        }

        this.investigationName = investigation.getName();
        this.investigationFullName = investigation.getFullName();
        this.sampleName = investigation.getSample() == null
                ? null
                : investigation.getSample().getName();
        this.reportType = investigation.getReportType() == null
                ? null
                : investigation.getReportType().name();
        this.category = investigation.getCategory() == null
                ? null
                : investigation.getCategory().getName();
        this.reportFormat = investigation.getReportFormat() == null
                ? null
                : investigation.getReportFormat().getName();
    }

    public String getInvestigationName() {
        return investigationName;
    }

    public void setInvestigationName(String investigationName) {
        this.investigationName = investigationName;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    public String getInvestigationFullName() {
        return investigationFullName;
    }

    public void setInvestigationFullName(String investigationFullName) {
        this.investigationFullName = investigationFullName;
    }
}
