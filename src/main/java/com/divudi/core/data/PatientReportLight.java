package com.divudi.core.data;

import com.divudi.core.entity.lab.PatientReport;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public class PatientReportLight {

    private Long id;
    private String name;
    private boolean approved;
    private boolean printed;
    private ReportType reportType;
    private String qrCodeContentsLink;

    public PatientReportLight() {
    }
    
    public PatientReportLight(Long id) {
        this.id = id;
    }

    public PatientReportLight(Long id, boolean approved, boolean printed, ReportType reportType, String qrCodeContentsLink) {
        this.id = id;
        this.approved = approved;
        this.printed = printed;
        this.reportType = reportType;
        this.qrCodeContentsLink = qrCodeContentsLink;
    }
    
    public PatientReportLight(PatientReport patientReport) {
        this.id = patientReport.getId();
        this.name = patientReport.getPatientInvestigation().getBillItem().getItem().getName();
        this.approved = patientReport.getApproved();
        this.printed = patientReport.getPrinted();
        this.reportType = patientReport.getReportType();
        this.qrCodeContentsLink = patientReport.getQrCodeContentsLink();
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof PatientReportLight)) {
            return false;
        }
        PatientReportLight other = (PatientReportLight) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        // Return a unique string representation for each PatientReportLight object
        return "ItemLight{" + "id=" + id + ", name=" + name + '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getQrCodeContentsLink() {
        return qrCodeContentsLink;
    }

    public void setQrCodeContentsLink(String qrCodeContentsLink) {
        this.qrCodeContentsLink = qrCodeContentsLink;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isPrinted() {
        return printed;
    }

    public void setPrinted(boolean printed) {
        this.printed = printed;
    }

}
