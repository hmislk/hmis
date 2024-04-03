package com.divudi.data;

public enum PaymentMethod {
    Cash("Cash"),
    Credit("Credit"),
    OnCall("On Call (Credit)"),
    Staff("Staff Payment"),
    Voucher("Voucher"),
    Agent("Agent Payment"),
    Card("Credit Card"),
    Cheque("Cheque"),
    Slip("Slip Payment"),
    ewallet("e-Wallet Payment"),
    PatientDeposit("Patient Deposit"),
    OnlineSettlement("Online Settlement"),
    MultiplePaymentMethods("Multiple Payment Methods"),
    YouOweMe("You Owe Me");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getInHandLabel() {
        // Modified to use the label field directly, appending "Received" as needed.
        // You can customize this part as per your requirement or remove if not needed.
        switch (this) {
            case Cash:
            case Agent:
            case Card:
            case Cheque:
            case Credit:
            case OnCall:
            case OnlineSettlement:
            case Slip:
            case Staff:
            case ewallet:
                return label + " Received";
            default:
                return label;
        }
    }
}
