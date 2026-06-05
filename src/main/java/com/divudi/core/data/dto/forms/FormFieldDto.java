package com.divudi.core.data.dto.forms;

public class FormFieldDto {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String componentPresentationType;
    private String componentDataType;
    private Integer orderNo;
    private boolean required;
    private String placeholder;
    private Double minValue;
    private Double maxValue;
    private Double stepSize;
    private Integer maxRating;
    private String onLabel;
    private String offLabel;
    private String editHtml;
    private String viewHtml;

    public FormFieldDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getComponentPresentationType() { return componentPresentationType; }
    public void setComponentPresentationType(String componentPresentationType) { this.componentPresentationType = componentPresentationType; }
    public String getComponentDataType() { return componentDataType; }
    public void setComponentDataType(String componentDataType) { this.componentDataType = componentDataType; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public Double getMinValue() { return minValue; }
    public void setMinValue(Double minValue) { this.minValue = minValue; }
    public Double getMaxValue() { return maxValue; }
    public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }
    public Double getStepSize() { return stepSize; }
    public void setStepSize(Double stepSize) { this.stepSize = stepSize; }
    public Integer getMaxRating() { return maxRating; }
    public void setMaxRating(Integer maxRating) { this.maxRating = maxRating; }
    public String getOnLabel() { return onLabel; }
    public void setOnLabel(String onLabel) { this.onLabel = onLabel; }
    public String getOffLabel() { return offLabel; }
    public void setOffLabel(String offLabel) { this.offLabel = offLabel; }
    public String getEditHtml() { return editHtml; }
    public void setEditHtml(String editHtml) { this.editHtml = editHtml; }
    public String getViewHtml() { return viewHtml; }
    public void setViewHtml(String viewHtml) { this.viewHtml = viewHtml; }
}
