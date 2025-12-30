package com.divudi.ws.report;

import com.divudi.core.entity.lab.PatientReport;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ViewReportResponseDTO {
    private Long id;
    private Date createdAt;

    private Boolean dataEntered;
    private String dataEntryUser;
    private Date dataEntryAt;
    private String dataEntryComments;
    private String dataEntryDepartment;
    private String dataEntryInstitution;

    private Boolean approved;
    private String approveUser;
    private Date approveAt;
    private String approveComments;
    private String approveDepartment;
    private String approveInstitution;

    private Boolean printed;
    private String printingUser;
    private Date printingAt;
    private String printingComments;
    private String printingDepartment;
    private String printingInstitution;

    private String reportFormat;
    private String qrCodeContentsDetailed;
    private String qrCodeContentsLink;

    private List<ViewReportItemValueResponseDTO> reportItemValues;

    public static ViewReportResponseDTO fromPatientReport(final PatientReport patientReport) {
        ViewReportResponseDTO dto = new ViewReportResponseDTO();

        if (patientReport == null) {
            return dto;
        }

        dto.id = patientReport.getId();
        dto.createdAt = patientReport.getCreatedAt();

        dto.dataEntered = patientReport.getDataEntered();
        dto.dataEntryUser = patientReport.getDataEntryUser() != null
                ? patientReport.getDataEntryUser().getName()
                : null;
        dto.dataEntryAt = patientReport.getDataEntryAt();
        dto.dataEntryComments = patientReport.getDataEntryComments();
        dto.dataEntryDepartment = patientReport.getDataEntryDepartment() != null
                ? patientReport.getDataEntryDepartment().getName()
                : null;
        dto.dataEntryInstitution = patientReport.getDataEntryInstitution() != null
                ? patientReport.getDataEntryInstitution().getName()
                : null;

        dto.approved = patientReport.getApproved();
        dto.approveUser = patientReport.getApproveUser() != null
                ? patientReport.getApproveUser().getName()
                : null;
        dto.approveAt = patientReport.getApproveAt();
        dto.approveComments = patientReport.getApproveComments();
        dto.approveDepartment = patientReport.getApproveDepartment() != null
                ? patientReport.getApproveDepartment().getName()
                : null;
        dto.approveInstitution = patientReport.getApproveInstitution() != null
                ? patientReport.getApproveInstitution().getName()
                : null;

        dto.printed = patientReport.getPrinted();
        dto.printingUser = patientReport.getPrintingUser() != null
                ? patientReport.getPrintingUser().getName()
                : null;
        dto.printingAt = patientReport.getPrintingAt();
        dto.printingComments = patientReport.getPrintingComments();
        dto.printingDepartment = patientReport.getPrintingDepartment() != null
                ? patientReport.getPrintingDepartment().getName()
                : null;
        dto.printingInstitution = patientReport.getPrintingInstitution() != null
                ? patientReport.getPrintingInstitution().getName()
                : null;

        dto.reportFormat = patientReport.getReportFormat() != null
                ? patientReport.getReportFormat().getName()
                : null;

        dto.qrCodeContentsDetailed = patientReport.getQrCodeContentsDetailed();
        dto.qrCodeContentsLink = patientReport.getQrCodeContentsLink();

        dto.reportItemValues = patientReport.getPatientReportItemValues() != null
                ? ViewReportItemValueResponseDTO.fromPatientReportItemValues(
                        patientReport.getPatientReportItemValues()
                )
                : Collections.emptyList();

        return dto;
    }

    public static List<ViewReportResponseDTO> fromPatientReports(final List<PatientReport> patientReports) {
        if (patientReports == null) {
            return Collections.emptyList();
        }

        return patientReports.stream()
                .map(ViewReportResponseDTO::fromPatientReport)
                .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getDataEntered() {
        return dataEntered;
    }

    public void setDataEntered(Boolean dataEntered) {
        this.dataEntered = dataEntered;
    }

    public String getDataEntryUser() {
        return dataEntryUser;
    }

    public void setDataEntryUser(String dataEntryUser) {
        this.dataEntryUser = dataEntryUser;
    }

    public Date getDataEntryAt() {
        return dataEntryAt;
    }

    public void setDataEntryAt(Date dataEntryAt) {
        this.dataEntryAt = dataEntryAt;
    }

    public String getDataEntryComments() {
        return dataEntryComments;
    }

    public void setDataEntryComments(String dataEntryComments) {
        this.dataEntryComments = dataEntryComments;
    }

    public String getDataEntryDepartment() {
        return dataEntryDepartment;
    }

    public void setDataEntryDepartment(String dataEntryDepartment) {
        this.dataEntryDepartment = dataEntryDepartment;
    }

    public String getDataEntryInstitution() {
        return dataEntryInstitution;
    }

    public void setDataEntryInstitution(String dataEntryInstitution) {
        this.dataEntryInstitution = dataEntryInstitution;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getApproveUser() {
        return approveUser;
    }

    public void setApproveUser(String approveUser) {
        this.approveUser = approveUser;
    }

    public Date getApproveAt() {
        return approveAt;
    }

    public void setApproveAt(Date approveAt) {
        this.approveAt = approveAt;
    }

    public String getApproveComments() {
        return approveComments;
    }

    public void setApproveComments(String approveComments) {
        this.approveComments = approveComments;
    }

    public String getApproveDepartment() {
        return approveDepartment;
    }

    public void setApproveDepartment(String approveDepartment) {
        this.approveDepartment = approveDepartment;
    }

    public String getApproveInstitution() {
        return approveInstitution;
    }

    public void setApproveInstitution(String approveInstitution) {
        this.approveInstitution = approveInstitution;
    }

    public Boolean getPrinted() {
        return printed;
    }

    public void setPrinted(Boolean printed) {
        this.printed = printed;
    }

    public String getPrintingUser() {
        return printingUser;
    }

    public void setPrintingUser(String printingUser) {
        this.printingUser = printingUser;
    }

    public Date getPrintingAt() {
        return printingAt;
    }

    public void setPrintingAt(Date printingAt) {
        this.printingAt = printingAt;
    }

    public String getPrintingComments() {
        return printingComments;
    }

    public void setPrintingComments(String printingComments) {
        this.printingComments = printingComments;
    }

    public String getPrintingDepartment() {
        return printingDepartment;
    }

    public void setPrintingDepartment(String printingDepartment) {
        this.printingDepartment = printingDepartment;
    }

    public String getPrintingInstitution() {
        return printingInstitution;
    }

    public void setPrintingInstitution(String printingInstitution) {
        this.printingInstitution = printingInstitution;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    public String getQrCodeContentsDetailed() {
        return qrCodeContentsDetailed;
    }

    public void setQrCodeContentsDetailed(String qrCodeContentsDetailed) {
        this.qrCodeContentsDetailed = qrCodeContentsDetailed;
    }

    public String getQrCodeContentsLink() {
        return qrCodeContentsLink;
    }

    public void setQrCodeContentsLink(String qrCodeContentsLink) {
        this.qrCodeContentsLink = qrCodeContentsLink;
    }

    public List<ViewReportItemValueResponseDTO> getReportItemValues() {
        return reportItemValues;
    }

    public void setReportItemValues(List<ViewReportItemValueResponseDTO> reportItemValues) {
        this.reportItemValues = reportItemValues;
    }
}
