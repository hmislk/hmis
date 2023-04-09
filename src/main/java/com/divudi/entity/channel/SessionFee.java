/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.channel;

import com.divudi.entity.Fee;
import com.divudi.entity.ServiceSession;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 *
 * @author buddhika
 */
@Entity
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
