/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.channel;

import com.divudi.entity.*;
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
public class SessionFee extends Fee implements Serializable {

    @ManyToOne
    ServiceSession session;

    public ServiceSession getSession() {
        return session;
    }

    public void setSession(ServiceSession session) {
        this.session = session;
    }
    
    
    
    private static final long serialVersionUID = 1L;
}
