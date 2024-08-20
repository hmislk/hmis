package com.divudi.data;

import java.util.List;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

public enum PaymentMethod {

    // Enum constants with @Deprecated where necessary
    OnCall("On Call", PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Cash("Cash", PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.CREDIT_SETTLEMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Card("Credit Card", PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    MultiplePaymentMethods("Multiple Payment Methods", PaymentContext.ACCEPTING_PAYMENTS),
    Staff("Staff Credit", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Credit("Credit", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.PURCHASES),
    Staff_Welfare("Staff Welfare", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    @Deprecated
    Voucher("Voucher", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    IOU("IOU", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Agent("Agent Payment", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Cheque("Cheque", PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    Slip("Slip Payment", PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.CREDIT_SETTLEMENTS),
    ewallet("e-Wallet Payment", PaymentContext.PURCHASES, PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.CREDIT_SETTLEMENTS),
    PatientDeposit("Patient Deposit", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING),
    PatientPoints("Patient Points", PaymentContext.ACCEPTING_PAYMENTS, PaymentContext.ACCEPTING_PAYMENTS_FOR_CHANNELLING, PaymentContext.CREDIT_SETTLEMENTS),
    OnlineSettlement("Online Settlement", PaymentContext.ACCEPTING_PAYMENTS),
    
    @Deprecated
    YouOweMe("You Owe Me", PaymentContext.ACCEPTING_PAYMENTS);

    private final String label;
    private final List<PaymentContext> contexts;

    PaymentMethod(String label, PaymentContext... contexts) {
        this.label = label;
        this.contexts = Arrays.asList(contexts);
    }

    public String getLabel() {
        return label;
    }

    public static List<PaymentMethod> getMethodsByContext(PaymentContext context) {
        return Arrays.stream(PaymentMethod.values())
                .filter(pm -> !isDeprecated(pm)) // Filter out deprecated enums
                .filter(pm -> pm.contexts.contains(context))
                .collect(Collectors.toList());
    }


    private static boolean isDeprecated(PaymentMethod method) {
        try {
            Field field = PaymentMethod.class.getField(method.name());
            return field.isAnnotationPresent(Deprecated.class);
        } catch (NoSuchFieldException e) {
            return false;  // Should not happen unless enum name is incorrect
        }
    }

    public static List<PaymentMethod> getActivePaymentMethods() {
        return Arrays.stream(PaymentMethod.values())
                .filter(pm -> !isDeprecated(pm))
                .collect(Collectors.toList());
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
