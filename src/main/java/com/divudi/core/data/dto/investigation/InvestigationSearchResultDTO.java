package com.divudi.core.data.dto.investigation;

public class InvestigationSearchResultDTO {
    private Long id;
    private String name;
    private String code;
    private String printName;
    private Boolean inactive;
    private String reportType;
    private Boolean bypassSampleWorkflow;
    private Boolean vatable;
    private Double vatPercentage;

    public InvestigationSearchResultDTO() {}

    public InvestigationSearchResultDTO(Long id, String name, String code, String printName, Boolean inactive, String reportType, Boolean bypassSampleWorkflow) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.printName = printName;
        this.inactive = inactive;
        this.reportType = reportType;
        this.bypassSampleWorkflow = bypassSampleWorkflow;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Boolean getVatable() { return vatable; }
    public void setVatable(Boolean vatable) { this.vatable = vatable; }
    public Double getVatPercentage() { return vatPercentage; }
    public void setVatPercentage(Double vatPercentage) { this.vatPercentage = vatPercentage; }
}
