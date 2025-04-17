/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.channel;

import com.divudi.core.entity.Fee;
import com.divudi.core.entity.ServiceSession;
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
