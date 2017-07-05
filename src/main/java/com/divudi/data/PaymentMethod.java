/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
   OnlineSettlement;
   
   public String getLabel() {
        switch (this) {
            case OnlineSettlement:
                return "Online Settlement";
            default:
                return this.toString();

        }
    }
}
