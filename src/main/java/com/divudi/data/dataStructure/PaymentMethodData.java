package com.divudi.data.dataStructure;

import com.divudi.data.PaymentMethod;
import java.util.ArrayList;
import java.util.List;

/**
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
public class PaymentMethodData {
    private ComponentDetail creditCard;
    private ComponentDetail cheque;
    private ComponentDetail slip;
    private ComponentDetail ewallet;
    private ComponentDetail patient_deposit;
    private ComponentDetail paymentMethodMultiple;
    
    private PaymentMethod paymentMethod;

    public ComponentDetail getCreditCard() {
        if (creditCard == null) {
            creditCard = new ComponentDetail();
        }
        return creditCard;
    }

    public void setCreditCard(ComponentDetail creditCard) {
        this.creditCard = creditCard;
    }

    
    
    
    public ComponentDetail getCheque() {
        if (cheque == null) {
            cheque = new ComponentDetail();
        }
        return cheque;
    }

    public void setCheque(ComponentDetail cheque) {
        this.cheque = cheque;
    }

    public ComponentDetail getSlip() {
        if (slip == null) {
            slip = new ComponentDetail();
        }
        return slip;
    }

    public void setSlip(ComponentDetail slip) {
        this.slip = slip;
    }

    public ComponentDetail getEwallet() {
        if (ewallet == null) {
            ewallet = new ComponentDetail();
        }
        return ewallet;
    }

    public void setEwallet(ComponentDetail ewallet) {
        this.ewallet = ewallet;
    }

    public ComponentDetail getPatient_deposit() {
        if (patient_deposit == null) {
            patient_deposit = new ComponentDetail();
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
        }
        return paymentMethodMultiple;
    }

    public void setPaymentMethodMultiple(ComponentDetail paymentMethodMultiple) {
        this.paymentMethodMultiple = paymentMethodMultiple;
    }

    
    
    
}
