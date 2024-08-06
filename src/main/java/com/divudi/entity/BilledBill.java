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
public class BilledBill extends Bill implements Serializable {
//     static final long serialVersionUID = 1L;
//    
//    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @OrderBy("inwardChargeType, searialNo")
//    List<BillItem> billItems;

    public BilledBill() {
        billClassType = BillClassType.BilledBill;
        qty = 1;
    }

}
