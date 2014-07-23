/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
