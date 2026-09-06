package com.divudi.core.data.dto.investigation;

public class InvestigationComponentDTO {

    private Long id;
    private Long investigationId;
    private String componentName;
    private String message;

    public InvestigationComponentDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInvestigationId() { return investigationId; }
    public void setInvestigationId(Long investigationId) { this.investigationId = investigationId; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
