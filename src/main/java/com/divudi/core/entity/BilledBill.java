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
public class BilledBill extends Bill implements Serializable {

    public BilledBill() {
        super();
        billClassType = BillClassType.BilledBill;
        qty = 1;
    }

}
