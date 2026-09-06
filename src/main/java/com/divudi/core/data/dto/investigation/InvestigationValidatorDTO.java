package com.divudi.core.data.dto.investigation;

public class InvestigationValidatorDTO {

    private Long id;
    private Long investigationId;
    private String name;
    private Double maximumValue;
    private Double minimumValue;
    private String message;

    public InvestigationValidatorDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInvestigationId() { return investigationId; }
    public void setInvestigationId(Long investigationId) { this.investigationId = investigationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getMaximumValue() { return maximumValue; }
    public void setMaximumValue(Double maximumValue) { this.maximumValue = maximumValue; }
    public Double getMinimumValue() { return minimumValue; }
    public void setMinimumValue(Double minimumValue) { this.minimumValue = minimumValue; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
