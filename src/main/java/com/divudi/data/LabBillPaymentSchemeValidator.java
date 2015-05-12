/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 *
 * @author Buddhika
 */
@FacesValidator("labBillPsValidator")
public class LabBillPaymentSchemeValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        ////System.out.println("value of validator is going to be checked ");
        if (value == null) {
            ////System.out.println("value is null");
            FacesMessage msg =
                    new FacesMessage("Payment Scheme Error",
                    "Please select a payment method");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        } else {
            ////System.out.println("value of validator is " + value.toString());
        }
    }
}
