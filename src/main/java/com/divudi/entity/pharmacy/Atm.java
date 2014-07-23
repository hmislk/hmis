/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.pharmacy;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Atm extends PharmaceuticalItem implements Serializable {

    private static final long serialVersionUID = 1L;
    @ManyToOne
    Vtm vtm;

    public Vtm getVtm() {
        return vtm;
    }

    public void setVtm(Vtm vtm) {
        this.vtm = vtm;
    }
}
