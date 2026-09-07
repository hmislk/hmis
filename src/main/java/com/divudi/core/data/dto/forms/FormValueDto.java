package com.divudi.core.data.dto.forms;

import java.util.Date;
import java.util.List;

public class FormValueDto {
    private Long id;
    private Long fieldId;
    private String fieldName;
    private String componentPresentationType;
    private String shortTextValue;
    private String longTextValue;
    private Double doubleValue;
    private Integer intValue;
    private Boolean booleanValue;
    private Date dateValue;
    private Integer ratingIntValue;
    private List<String> selectedValues;

    public FormValueDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getComponentPresentationType() { return componentPresentationType; }
    public void setComponentPresentationType(String componentPresentationType) { this.componentPresentationType = componentPresentationType; }
    public String getShortTextValue() { return shortTextValue; }
    public void setShortTextValue(String shortTextValue) { this.shortTextValue = shortTextValue; }
    public String getLongTextValue() { return longTextValue; }
    public void setLongTextValue(String longTextValue) { this.longTextValue = longTextValue; }
    public Double getDoubleValue() { return doubleValue; }
    public void setDoubleValue(Double doubleValue) { this.doubleValue = doubleValue; }
    public Integer getIntValue() { return intValue; }
    public void setIntValue(Integer intValue) { this.intValue = intValue; }
    public Boolean getBooleanValue() { return booleanValue; }
    public void setBooleanValue(Boolean booleanValue) { this.booleanValue = booleanValue; }
    public Date getDateValue() { return dateValue; }
    public void setDateValue(Date dateValue) { this.dateValue = dateValue; }
    public Integer getRatingIntValue() { return ratingIntValue; }
    public void setRatingIntValue(Integer ratingIntValue) { this.ratingIntValue = ratingIntValue; }
    public List<String> getSelectedValues() { return selectedValues; }
    public void setSelectedValues(List<String> selectedValues) { this.selectedValues = selectedValues; }
}
