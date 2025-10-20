package com.divudi.core.util;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * JSF Validator to ensure input is an integer (no decimal places)
 *
 * Usage in XHTML:
 * <p:inputText value="#{bean.qty}">
 *     <f:validator validatorId="integerValidator" />
 * </p:inputText>
 *
 * @author HMIS Team
 */
@FacesValidator("integerValidator")
public class IntegerValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        if (value == null) {
            return; // Allow null values - use required attribute for null checking
        }

        // Check if it's a Double with decimal places
        if (value instanceof Double) {
            Double doubleValue = (Double) value;
            if (doubleValue % 1 != 0) {
                FacesMessage msg = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Validation Error",
                    "Please enter only whole numbers (integers). Decimal values are not allowed."
                );
                throw new ValidatorException(msg);
            }
        } else if (value instanceof Float) {
            Float floatValue = (Float) value;
            if (floatValue % 1 != 0) {
                FacesMessage msg = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Validation Error",
                    "Please enter only whole numbers (integers). Decimal values are not allowed."
                );
                throw new ValidatorException(msg);
            }
        } else if (value instanceof String) {
            String stringValue = (String) value;
            try {
                Double doubleValue = Double.parseDouble(stringValue);
                if (doubleValue % 1 != 0) {
                    FacesMessage msg = new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Validation Error",
                        "Please enter only whole numbers (integers). Decimal values are not allowed."
                    );
                    throw new ValidatorException(msg);
                }
            } catch (NumberFormatException e) {
                FacesMessage msg = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Validation Error",
                    "Please enter a valid number."
                );
                throw new ValidatorException(msg);
            }
        }
    }
}
