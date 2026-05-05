package com.divudi.core.data.dto.investigation;

public class InvestigationCreateRequestDTO {
    private String name;
    private String code;
    private String printName;
    private Boolean inactive;
    private String reportType;
    private Boolean bypassSampleWorkflow;

    public boolean isValid() { return name != null && !name.trim().isEmpty(); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPrintName() { return printName; }
    public void setPrintName(String printName) { this.printName = printName; }
    public Boolean getInactive() { return inactive; }
    public void setInactive(Boolean inactive) { this.inactive = inactive; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public Boolean getBypassSampleWorkflow() { return bypassSampleWorkflow; }
    public void setBypassSampleWorkflow(Boolean bypassSampleWorkflow) { this.bypassSampleWorkflow = bypassSampleWorkflow; }
}
