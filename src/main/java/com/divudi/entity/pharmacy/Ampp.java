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
public class Ampp extends PharmaceuticalItem implements Serializable {

    private static final long serialVersionUID = 1L;
    @ManyToOne
     Amp amp;
    @ManyToOne
     Vmpp vmpp;

    public Amp getAmp() {
        return amp;
    }

    public void setAmp(Amp amp) {
        this.amp = amp;
    }

    public Vmpp getVmpp() {
        return vmpp;
    }

    public void setVmpp(Vmpp vmpp) {
        this.vmpp = vmpp;
    }
}
