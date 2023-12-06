/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 *
 * @author buddhika
 */
public enum PaymentMethod {
    Cash,
    Credit,
    OnCall,
    Staff,
    //   @Deprecated
    Agent,
    Card,
    Cheque,
    Slip,
    ewallet,
    PatientDeposit,
    OnlineSettlement,
    MultiplePaymentMethods,
    YouOweMe;

    public String getLabel() {
        switch (this) {
            case Agent:
                return "Agent Payment";
            case Card:
                return "Credit Card";
            case Cash:
                return "Cash";
            case Cheque:
                return "Cheque";
            case Credit:
                return "Credit";
            case OnCall:
                return "On Call (Credit)";
            case OnlineSettlement:
                return "Online Settlement";
            case Slip:
                return "Slip Payment";
            case Staff:
                return "Staff Payment";
            case ewallet:
                return "e-Wallet Payment";
            case PatientDeposit:
                return "Patient Deposit";
            case MultiplePaymentMethods:
                return "Multiple Payment Methods";
            case YouOweMe:
                return "You Owe Me";
            default:
                return this.toString();

        }
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

}
