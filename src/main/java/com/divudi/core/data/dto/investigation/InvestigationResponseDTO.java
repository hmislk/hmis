package com.divudi.core.data.dto.investigation;

public class InvestigationResponseDTO extends InvestigationSearchResultDTO {
    private String message;

    public InvestigationResponseDTO() {}

    public InvestigationResponseDTO(Long id, String name, String code, String printName, Boolean inactive, String reportType, Boolean bypassSampleWorkflow, String message) {
        super(id, name, code, printName, inactive, reportType, bypassSampleWorkflow);
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
