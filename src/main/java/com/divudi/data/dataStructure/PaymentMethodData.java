package com.divudi.data.dataStructure;

import com.divudi.data.PaymentMethod;

/**
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
public class PaymentMethodData {
    private ComponentDetail cash;
    private ComponentDetail creditCard;
    private ComponentDetail cheque;
    private ComponentDetail slip;
    private ComponentDetail ewallet;
    private ComponentDetail patient_deposit;
    private ComponentDetail paymentMethodMultiple;
    private ComponentDetail credit;
    
    private PaymentMethod paymentMethod;

    public ComponentDetail getCreditCard() {
        if (creditCard == null) {
            creditCard = new ComponentDetail();
            creditCard.setPaymentMethod(PaymentMethod.Card);
        }
        return creditCard;
    }

    public void setCreditCard(ComponentDetail creditCard) {
        this.creditCard = creditCard;
    }

    
    
    
    public ComponentDetail getCheque() {
        if (cheque == null) {
            cheque = new ComponentDetail();
            cheque.setPaymentMethod(PaymentMethod.Cheque);
        }
        return cheque;
    }

    public void setCheque(ComponentDetail cheque) {
        this.cheque = cheque;
    }

    public ComponentDetail getSlip() {
        if (slip == null) {
            slip = new ComponentDetail();
            slip.setPaymentMethod(PaymentMethod.Slip);
        }
        return slip;
    }

    public void setSlip(ComponentDetail slip) {
        this.slip = slip;
    }

    public ComponentDetail getEwallet() {
        if (ewallet == null) {
            ewallet = new ComponentDetail();
            ewallet.setPaymentMethod(PaymentMethod.ewallet);
        }
        return ewallet;
    }

    public void setEwallet(ComponentDetail ewallet) {
        this.ewallet = ewallet;
    }

    public ComponentDetail getPatient_deposit() {
        if (patient_deposit == null) {
            patient_deposit = new ComponentDetail();
            patient_deposit.setPaymentMethod(PaymentMethod.PatientDeposit);
        }
        return patient_deposit;
    }

    public void setPatient_deposit(ComponentDetail patient_deposit) {
        this.patient_deposit = patient_deposit;
    }

    

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public ComponentDetail getPaymentMethodMultiple() {
        if(paymentMethodMultiple==null){
            paymentMethodMultiple = new ComponentDetail();
            paymentMethodMultiple.getMultiplePaymentMethodComponentDetails();
            paymentMethodMultiple.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);
        }
        return paymentMethodMultiple;
    }

    public void setPaymentMethodMultiple(ComponentDetail paymentMethodMultiple) {
        this.paymentMethodMultiple = paymentMethodMultiple;
    }

    public ComponentDetail getCash() {
        if(cash==null){
            cash = new ComponentDetail();
            cash.setPaymentMethod(PaymentMethod.Cash);
        }
        return cash;
    }

    public void setCash(ComponentDetail cash) {
        this.cash = cash;
    }

    public ComponentDetail getCredit() {
        if (credit == null) {
            credit = new ComponentDetail();
            credit.setPaymentMethod(PaymentMethod.Credit);
        }
        return credit;
    }

    public void setCredit(ComponentDetail credit) {
        this.credit = credit;
    }

    
    
    
    
}
