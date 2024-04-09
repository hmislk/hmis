package com.divudi.bean.common;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("enumConverter")
public class EnumConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            Object attributes = component.getAttributes().get("enumType");
            if (attributes instanceof Class<?>) {
                Class<?> enumType = (Class<?>) attributes;
                return Enum.valueOf((Class<Enum>) enumType, value);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Conversion error for enum: " + value, e);
        }
        return null;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        } else {
            throw new RuntimeException("Value is not an enum: " + value);
        }
    }
}
