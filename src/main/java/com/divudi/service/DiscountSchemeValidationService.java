package com.divudi.service;

import com.divudi.data.BooleanMessage;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.Bill;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class DiscountSchemeValidationService {

    public BooleanMessage validateDiscountScheme(Bill bill, PaymentMethodData pmd) {
        BooleanMessage validated = new BooleanMessage();
        if (bill == null) {
            validated.setFlag(false);
            validated.setMessage("Bill is missing.");
            return validated;
        }
        if (bill.getPaymentMethod() == null) {
            validated.setFlag(false);
            validated.setMessage("Payment Method is missing.");
            return validated;
        }
        if (bill.getPaymentScheme() == null) {
            validated.setFlag(true);
            validated.setMessage("No Discount Scheme. Therefore nothing to validate.");
            return validated;
        }
        return validateDiscountScheme(bill.getPaymentMethod(), bill.getPaymentScheme(), pmd);
    }

    public BooleanMessage validateDiscountScheme(PaymentMethod paymentMethod, PaymentScheme discountScheme, PaymentMethodData pmd) {
        BooleanMessage validated = new BooleanMessage();
        validated.setFlag(true);
        validated.setMessage("No error in validating discount scheme.");
        if (paymentMethod == null) {
            validated.setFlag(false);
            validated.setMessage("Payment Method is missing.");
            return validated;
        }
        if (discountScheme == null) {
            validated.setFlag(true);
            validated.setMessage("No Discount Scheme. Therefore nothing to validate.");
            return validated;
        }
        if (discountScheme.isStaffRequired()) {
            BooleanMessage validateStaffRequirement = validateDiscountSchemeForStaffRequired(paymentMethod, discountScheme, pmd);
            if (!validateStaffRequirement.isFlag()) {
                return validateStaffRequirement;
            }
        }

        return validated;
    }

    public BooleanMessage validateDiscountSchemeForStaffRequired(PaymentMethod paymentMethod, PaymentScheme discountScheme, PaymentMethodData pmd) {
        BooleanMessage validated = new BooleanMessage();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (pmd == null || pmd.getPaymentMethodMultiple() == null || pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                validated.setFlag(false);
                validated.setMessage("Invalid multiple payment method data.");
                return validated;
            }
            for (ComponentDetail cd : pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd.getPaymentMethod() == PaymentMethod.Staff || cd.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                    validated.setFlag(false);
                    validated.setMessage("Discount scheme can NOT be applied for Multiple Payment Methods.");
                    return validated;
                }
            }
        } else if (paymentMethod == PaymentMethod.Staff || paymentMethod == PaymentMethod.Staff_Welfare) {
            validated.setFlag(true);
            validated.setMessage("Discount scheme is validated against Payment Method.");
            return validated;
        } else {
            validated.setFlag(false);
            validated.setMessage("Discount scheme " + discountScheme.getName() + " can NOT be allowed with the payment method " + paymentMethod.getLabel());
            return validated;
        }
        return validated;
    }

}
