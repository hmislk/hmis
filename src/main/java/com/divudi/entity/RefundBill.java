/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.BillClassType;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 *
 * @author buddhika
 */
@Entity
public class RefundBill extends Bill implements Serializable {

    public RefundBill() {
        billClassType = BillClassType.RefundBill;
        qty = 0 - 1;
    }

}
