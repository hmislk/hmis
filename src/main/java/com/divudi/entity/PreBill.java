package com.divudi.entity;

import com.divudi.data.BillClassType;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 *
 * @author buddhika
 */
@Entity
public class PreBill extends Bill implements Serializable {

    public PreBill() {
        billClassType = BillClassType.PreBill;
        qty = 1;
    }
}
