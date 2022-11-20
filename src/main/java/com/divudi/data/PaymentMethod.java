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
    OnlineSettlement;

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
            default:
                return this.toString();

        }
    }
}
