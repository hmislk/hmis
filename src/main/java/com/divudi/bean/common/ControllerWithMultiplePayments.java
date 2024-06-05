/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.data.dataStructure.PaymentMethodData;

/**
 *
 * @author Senula Nanayakkara
 */
public interface ControllerWithMultiplePayments {
    public double calculatRemainForMultiplePaymentTotal();
    public void recieveRemainAmountAutomatically();
    
}
