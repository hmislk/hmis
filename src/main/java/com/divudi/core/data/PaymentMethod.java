package com.divudi.core.data;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.lang.reflect.Field;

public enum PaymentMethod {
    OnCall("On Call", PaymentType.NONE, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Cash("Cash", PaymentType.NON_CREDIT, PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.CREDIT_SETTLEMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Card("Credit Card", PaymentType.NON_CREDIT, PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    MultiplePaymentMethods("Multiple Payment Methods", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS),
    Staff("Staff Credit", PaymentType.CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Credit("Credit", PaymentType.CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.PURCHASES),
    Staff_Welfare("Staff Welfare", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Voucher("Voucher", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING), // Assuming deprecated
    IOU("IOU", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Agent("Agent Payment", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Cheque("Cheque", PaymentType.NON_CREDIT, PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Slip("Slip Payment", PaymentType.NON_CREDIT, PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.CREDIT_SETTLEMENTS),
    ewallet("e-Wallet Payment", PaymentType.NON_CREDIT, PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.CREDIT_SETTLEMENTS),
    PatientDeposit("Patient Deposit", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    PatientPoints("Patient Points", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.CREDIT_SETTLEMENTS),
    OnlineSettlement("Online Settlement", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS),
    None("None", PaymentType.NONE, PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.CREDIT_SETTLEMENTS),
    OnlineBookingAgent("OnlineBooking Agent", PaymentType.NONE, PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.CREDIT_SETTLEMENTS),
    @Deprecated
    YouOweMe("You Owe Me", PaymentType.NON_CREDIT, PaymentContext.ACCEPTING_PAYMENTS); // Assuming deprecated

    private final String label;
    private final PaymentType paymentType;
    private final List<PaymentContext> contexts;

    PaymentMethod(String label, PaymentType paymentType, PaymentContext... contexts) {
        this.label = label;
        this.paymentType = paymentType;
        this.contexts = Arrays.asList(contexts);
    }

    public String getLabel() {
        return label;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public List<PaymentContext> getContexts() {
        return contexts;
    }

    public static List<PaymentMethod> getMethodsByContext(PaymentContext context) {
        return Arrays.stream(PaymentMethod.values())
                .filter(pm -> pm.contexts.contains(context) && !isDeprecated(pm))
                .collect(Collectors.toList());
    }

    public static List<PaymentMethod> getMethodsByType(PaymentType type) {
        return Arrays.stream(PaymentMethod.values())
                .filter(pm -> pm.paymentType == type)
                .collect(Collectors.toList());
    }

    private static boolean isDeprecated(PaymentMethod method) {
        try {
            Field field = PaymentMethod.class.getField(method.name());
            return field.isAnnotationPresent(Deprecated.class);
        } catch (NoSuchFieldException e) {
            return false; // Should not happen unless enum name is incorrect
        }
    }

    public static List<PaymentMethod> getActivePaymentMethods() {
        return Arrays.stream(PaymentMethod.values())
                .filter(pm -> !isDeprecated(pm))
                .collect(Collectors.toList());
    }

    /**
     * Returns all non-credit payment methods (excludes Credit and Staff payment methods)
     * @return List of PaymentMethod with PaymentType.NON_CREDIT
     */
    public static List<PaymentMethod> getNonCreditPaymentMethods() {
        return getMethodsByType(PaymentType.NON_CREDIT);
    }

    public String getInHandLabel() {
        switch (this) {
            case Agent:
                return "Agent Payment Received";
            case Card:
                return "Credit Card Received";
            case Cash:
                return "Cash in hand";
            case Cheque:
                return "Cheque Received";
            case Credit:
                return "Credit Received";
            case OnCall:
                return "On Call (Credit) Received";
            case OnlineSettlement:
                return "Online Settlement Received";
            case Slip:
                return "Slip Payment Received";
            case Staff:
                return "Staff Payment Received";
            case ewallet:
                return "e-Wallet Payment Received";
            default:
                return this.toString();
        }
    }

    public static List<PaymentMethod> asList() {
        return Arrays.asList(PaymentMethod.values());
    }

}
