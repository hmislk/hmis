package com.divudi.core.data.dto.investigation;

public class InvestigationCreateRequestDTO {
    private String name;
    private String code;
    private String printName;
    private Boolean inactive;
    private String reportType;
    private Boolean bypassSampleWorkflow;
    private Boolean vatable;
    private Double vatPercentage;
    private Long categoryId;
    private String categoryName;
    private Long sampleId;
    private String sampleName;
    private Long containerId;
    private String containerName;
    private Long analyzerId;
    private String analyzerName;

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
    public Boolean getVatable() { return vatable; }
    public void setVatable(Boolean vatable) { this.vatable = vatable; }
    public Double getVatPercentage() { return vatPercentage; }
    public void setVatPercentage(Double vatPercentage) { this.vatPercentage = vatPercentage; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getSampleId() { return sampleId; }
    public void setSampleId(Long sampleId) { this.sampleId = sampleId; }
    public String getSampleName() { return sampleName; }
    public void setSampleName(String sampleName) { this.sampleName = sampleName; }
    public Long getContainerId() { return containerId; }
    public void setContainerId(Long containerId) { this.containerId = containerId; }
    public String getContainerName() { return containerName; }
    public void setContainerName(String containerName) { this.containerName = containerName; }
    public Long getAnalyzerId() { return analyzerId; }
    public void setAnalyzerId(Long analyzerId) { this.analyzerId = analyzerId; }
    public String getAnalyzerName() { return analyzerName; }
    public void setAnalyzerName(String analyzerName) { this.analyzerName = analyzerName; }
}
