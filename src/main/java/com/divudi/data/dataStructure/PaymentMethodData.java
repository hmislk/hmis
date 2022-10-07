/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.data.dataStructure;

/**
 *
 * @author safrin
 */
public class PaymentMethodData {
    private ComponentDetail creditCard=new ComponentDetail();
    private ComponentDetail cheque=new ComponentDetail();
    private ComponentDetail slip=new ComponentDetail();
    
    
    

    public ComponentDetail getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(ComponentDetail creditCard) {
        this.creditCard = creditCard;
    }

    public ComponentDetail getCheque() {
        return cheque;
    }

    public void setCheque(ComponentDetail cheque) {
        this.cheque = cheque;
    }

    public ComponentDetail getSlip() {
        return slip;
    }

    public void setSlip(ComponentDetail slip) {
        this.slip = slip;
    }

    
}
