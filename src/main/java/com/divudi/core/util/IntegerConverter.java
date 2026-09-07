package com.divudi.core.util;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * JSF Converter to ensure input is an integer (no decimal places)
 * This converter validates during the conversion phase, which works better with AJAX
 *
 * Usage in XHTML:
 * <p:inputText value="#{bean.qty}" converter="integerConverter" />
 *
 * @author HMIS Team
 */
@FacesConverter("integerConverter")
public class IntegerConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmedValue = value.trim();

            // Check if the string contains a decimal point
            if (trimmedValue.contains(".")) {
                double doubleValue = Double.parseDouble(trimmedValue);

                // Check if it has decimal places
                if (doubleValue % 1 != 0) {
                    FacesMessage msg = new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Validation Error",
                        "Please enter only whole numbers (integers). Decimal values are not allowed."
                    );
                    throw new ConverterException(msg);
                }

                // It's a whole number, return it as Double
                return doubleValue;
            }

            // No decimal point, parse as double
            return Double.parseDouble(trimmedValue);

        } catch (NumberFormatException e) {
            FacesMessage msg = new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Validation Error",
                "Please enter a valid number."
            );
            throw new ConverterException(msg);
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Number) {
            double num = ((Number) value).doubleValue();
            // Format without decimal places if it's a whole number
            if (num % 1 == 0) {
                return String.valueOf((long) num);
            }
            return String.valueOf(num);
        }

        return value.toString();
    }
}
