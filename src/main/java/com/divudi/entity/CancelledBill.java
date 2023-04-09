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
public class CancelledBill extends Bill implements Serializable {

    public CancelledBill() {
        billClassType = BillClassType.CancelledBill;
        qty = 0 - 1;
    }

}
