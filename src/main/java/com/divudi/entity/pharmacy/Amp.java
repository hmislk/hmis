/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 *
 * @author buddhika
 */
@Entity
public class Amp extends PharmaceuticalItem implements Serializable {
    @ManyToOne
    private Vmp vmp;
    
    @ManyToOne
    private Atm atm;
    

    @Override
    public Vmp getVmp() {
        return vmp;
    }

    @Override
    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }

    public Atm getAtm() {
        return atm;
    }

    public void setAtm(Atm atm) {
        this.atm = atm;
    }
    
    
}
