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
public class Vmpp extends PharmaceuticalItem implements Serializable {
    private static final long serialVersionUID = 1L;
   
    @ManyToOne
    Vmp vmp;

    @ManyToOne
    MeasurementUnit packUnit;

    public MeasurementUnit getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(MeasurementUnit packUnit) {
        this.packUnit = packUnit;
    }
    
    
    
    
    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }
    
    
}
