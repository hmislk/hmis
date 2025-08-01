package com.divudi.core.data.web;

public enum ComponentMappingType {
    SHORT_TEXT("Short Text", ComponentDataType.Short_Text, ComponentPresentationType.Input_text),
    LONG_TEXT("Long Text", ComponentDataType.Long_Text, ComponentPresentationType.Input_text_Area),
    SHORT_INTEGER("Short Integer", ComponentDataType.Short, ComponentPresentationType.Input_Number),
    LONG_INTEGER("Long Integer", ComponentDataType.Long, ComponentPresentationType.Input_Number),
    DOUBLE_NUMBER("Double Number", ComponentDataType.Double, ComponentPresentationType.Input_Number),
    INSTITUTION("Institution", ComponentDataType.Coded_Item, ComponentPresentationType.SelectOneMenu),
    DEPARTMENT("Department", ComponentDataType.Coded_Item, ComponentPresentationType.SelectOneMenu),
    SITE("Site", ComponentDataType.Coded_Item, ComponentPresentationType.SelectOneMenu),
    SPECIALITY("Speciality", ComponentDataType.Coded_Item, ComponentPresentationType.SelectOneMenu);

    private final String label;
    private final ComponentDataType dataType;
    private final ComponentPresentationType presentationType;

    ComponentMappingType(String label, ComponentDataType dataType, ComponentPresentationType presentationType) {
        this.label = label;
        this.dataType = dataType;
        this.presentationType = presentationType;
    }

    public String getLabel() {
        return label;
    }

    public ComponentDataType getDataType() {
        return dataType;
    }

    public ComponentPresentationType getPresentationType() {
        return presentationType;
    }
}
