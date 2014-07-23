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
public class Amp extends PharmaceuticalItem implements Serializable {

    private static final long serialVersionUID = 1L;
    @ManyToOne
    private Vmp vmp;

    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }
}
