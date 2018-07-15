/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author buddhika_ari
 */
public class PaymentMethodValue {
    PaymentMethod paymentMethod;
    Double value;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getValue() {
        if(value==null){
            value =0d;
        }
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public PaymentMethodValue() {
    }

    public PaymentMethodValue(PaymentMethod paymentMethod, Double value) {
        this.paymentMethod = paymentMethod;
        this.value = value;
    }
    
    
    
}
