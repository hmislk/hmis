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
