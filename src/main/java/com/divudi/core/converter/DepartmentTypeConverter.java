package com.divudi.core.converter;

import com.divudi.core.data.DepartmentType;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class DepartmentTypeConverter implements AttributeConverter<DepartmentType, String> {

    @Override
    public String convertToDatabaseColumn(DepartmentType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DepartmentType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return DepartmentType.valueOf(dbData.trim());
        } catch (IllegalArgumentException e) {
            return null; // or log warning
        }
    }
}
