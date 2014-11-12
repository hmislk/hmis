/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.BillClassType;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class BilledBill extends Bill implements Serializable {
//     static final long serialVersionUID = 1L;

    public BilledBill() {
        billClassType = BillClassType.BilledBill;
        qty = 1;
    }

}
