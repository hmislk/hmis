package com.divudi.core.entity;

import com.divudi.core.data.BillClassType;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 *
 * @author buddhika
 */
@Entity
public class PreBill extends Bill implements Serializable {

    public PreBill() {
        super();
        billClassType = BillClassType.PreBill;
        qty = 1;
    }
}
