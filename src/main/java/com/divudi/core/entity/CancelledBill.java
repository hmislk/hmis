/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.BillClassType;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 *
 * @author buddhika
 */
@Entity
public class CancelledBill extends Bill implements Serializable {

    public CancelledBill() {
        super();
        billClassType = BillClassType.CancelledBill;
        qty = 0 - 1;
    }

}
