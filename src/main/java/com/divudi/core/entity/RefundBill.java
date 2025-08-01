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
public class RefundBill extends Bill implements Serializable {

    public RefundBill() {
        super();
        billClassType = BillClassType.RefundBill;
        qty = 0 - 1;
    }

}
