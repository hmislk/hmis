/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import java.math.BigDecimal;

/**
 * Validator to ensure the input value is a positive number (greater than zero)
 * @author Buddhika
 */
@FacesValidator("positiveNumberValidator")
public class PositiveNumberValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value == null) {
            return; // Let required validator handle null values
        }

        BigDecimal numberValue;
        try {
            if (value instanceof BigDecimal) {
                numberValue = (BigDecimal) value;
            } else if (value instanceof Number) {
                numberValue = new BigDecimal(value.toString());
            } else {
                numberValue = new BigDecimal(value.toString());
            }
        } catch (NumberFormatException e) {
            FacesMessage msg = new FacesMessage("Invalid Number Format", 
                "Please enter a valid number");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }

        if (numberValue.compareTo(BigDecimal.ZERO) <= 0) {
            FacesMessage msg = new FacesMessage("Invalid Quantity", 
                "Quantity must be greater than zero");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }
}