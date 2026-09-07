package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.membership.RestrictedPaymentMethod;
import com.divudi.core.facade.RestrictedPaymentMethodFacade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class DiscountSchemeValidationService {

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    RestrictedPaymentMethodFacade restrictedPaymentMethodFacade;

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

        BooleanMessage restrictedCheck = validateNotRestrictedPaymentMethod(paymentMethod, discountScheme, pmd);
        if (!restrictedCheck.isFlag()) {
            return restrictedCheck;
        }

        if (discountScheme.isStaffRequired()) {
            BooleanMessage validateStaffRequirement = validateDiscountSchemeForStaffRequired(paymentMethod, discountScheme, pmd);
            if (!validateStaffRequirement.isFlag()) {
                return validateStaffRequirement;
            }
        }

        return validated;
    }

    public BooleanMessage validateDiscountScheme(PaymentMethod paymentMethod, PaymentScheme discountScheme) {
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

        BooleanMessage restrictedCheck = validateNotRestrictedPaymentMethod(paymentMethod, discountScheme, null);
        if (!restrictedCheck.isFlag()) {
            return restrictedCheck;
        }

        if (discountScheme.isStaffRequired()) {
            BooleanMessage validateStaffRequirement = validateDiscountSchemeForStaffRequired(paymentMethod, discountScheme);
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

                boolean staffPaymentMethodsAreAllowedInsideMultiplePayments = configOptionApplicationController.getBooleanValueByKey("Staff Payment Methods Are Allowed Inside Multiple Payments", true);
                if (!staffPaymentMethodsAreAllowedInsideMultiplePayments) {
                    if (cd.getPaymentMethod() == PaymentMethod.Staff || cd.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                        validated.setFlag(false);
                        validated.setMessage("Discount scheme can NOT be applied for Multiple Payment Methods.");
                        return validated;
                    }
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



    public BooleanMessage validateDiscountSchemeForStaffRequired(PaymentMethod paymentMethod, PaymentScheme discountScheme) {
        BooleanMessage validated = new BooleanMessage();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
                validated.setFlag(false);
                validated.setMessage("Invalid multiple payment method data.");
                return validated;
        } else if (paymentMethod == PaymentMethod.Staff || paymentMethod == PaymentMethod.Staff_Welfare) {
            validated.setFlag(true);
            validated.setMessage("Discount scheme is validated against Payment Method.");
            return validated;
        } else {
            validated.setFlag(false);
            validated.setMessage("Discount scheme " + discountScheme.getName() + " can NOT be allowed with the payment method " + paymentMethod.getLabel());
            return validated;
        }
    }

    public BooleanMessage validateNotRestrictedPaymentMethod(PaymentMethod paymentMethod, PaymentScheme discountScheme, PaymentMethodData pmd) {
        BooleanMessage validated = new BooleanMessage();
        validated.setFlag(true);
        validated.setMessage("No restricted payment method.");

        if (paymentMethod == null || discountScheme == null) {
            return validated;
        }

        List<PaymentMethod> restricted = loadRestrictedPaymentMethods(discountScheme);
        if (restricted == null || restricted.isEmpty()) {
            return validated;
        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (pmd != null
                    && pmd.getPaymentMethodMultiple() != null
                    && pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                for (ComponentDetail cd : pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd != null && cd.getPaymentMethod() != null && restricted.contains(cd.getPaymentMethod())) {
                        validated.setFlag(false);
                        validated.setMessage("Payment method " + cd.getPaymentMethod().getLabel()
                                + " is restricted for discount scheme " + discountScheme.getName() + ".");
                        return validated;
                    }
                }
            }
        } else if (restricted.contains(paymentMethod)) {
            validated.setFlag(false);
            validated.setMessage("Payment method " + paymentMethod.getLabel()
                    + " is restricted for discount scheme " + discountScheme.getName() + ".");
            return validated;
        }

        return validated;
    }

    private List<PaymentMethod> loadRestrictedPaymentMethods(PaymentScheme discountScheme) {
        if (discountScheme == null || discountScheme.getId() == null) {
            return null;
        }
        String jpql = "SELECT r FROM RestrictedPaymentMethod r "
                + " WHERE r.retired = false "
                + " AND r.paymentScheme = :ps";
        Map<String, Object> params = new HashMap<>();
        params.put("ps", discountScheme);
        List<RestrictedPaymentMethod> rows = restrictedPaymentMethodFacade.findByJpql(jpql, params);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        List<PaymentMethod> methods = new java.util.ArrayList<>();
        for (RestrictedPaymentMethod r : rows) {
            if (r.getPaymentMethod() != null) {
                methods.add(r.getPaymentMethod());
            }
        }
        return methods;
    }

}
