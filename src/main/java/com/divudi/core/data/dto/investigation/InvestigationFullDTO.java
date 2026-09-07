package com.divudi.core.data.dto.investigation;

import com.divudi.core.data.dto.service.ItemFeeDTO;

import java.util.List;

public class InvestigationFullDTO {

    private InvestigationResponseDTO investigation;
    private List<InvestigationComponentDTO> components;
    private List<InvestigationItemDTO> formatItems;
    private List<InvestigationItemValueDTO> itemValues;
    private List<IxCalDTO> calculations;
    private List<TestFlagDTO> flags;
    private List<DynamicLabelDTO> dynamicLabels;
    private List<InvestigationValidatorDTO> validators;
    private List<ItemFeeDTO> fees;
    private String message;

    public InvestigationFullDTO() {
    }

    public InvestigationResponseDTO getInvestigation() { return investigation; }
    public void setInvestigation(InvestigationResponseDTO investigation) { this.investigation = investigation; }
    public List<InvestigationComponentDTO> getComponents() { return components; }
    public void setComponents(List<InvestigationComponentDTO> components) { this.components = components; }
    public List<InvestigationItemDTO> getFormatItems() { return formatItems; }
    public void setFormatItems(List<InvestigationItemDTO> formatItems) { this.formatItems = formatItems; }
    public List<InvestigationItemValueDTO> getItemValues() { return itemValues; }
    public void setItemValues(List<InvestigationItemValueDTO> itemValues) { this.itemValues = itemValues; }
    public List<IxCalDTO> getCalculations() { return calculations; }
    public void setCalculations(List<IxCalDTO> calculations) { this.calculations = calculations; }
    public List<TestFlagDTO> getFlags() { return flags; }
    public void setFlags(List<TestFlagDTO> flags) { this.flags = flags; }
    public List<DynamicLabelDTO> getDynamicLabels() { return dynamicLabels; }
    public void setDynamicLabels(List<DynamicLabelDTO> dynamicLabels) { this.dynamicLabels = dynamicLabels; }
    public List<InvestigationValidatorDTO> getValidators() { return validators; }
    public void setValidators(List<InvestigationValidatorDTO> validators) { this.validators = validators; }
    public List<ItemFeeDTO> getFees() { return fees; }
    public void setFees(List<ItemFeeDTO> fees) { this.fees = fees; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
