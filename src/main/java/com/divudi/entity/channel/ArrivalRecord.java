/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.channel;

import com.divudi.entity.ServiceSession;
import com.divudi.entity.hr.FingerPrintRecord;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika
 */
@Entity
public class ArrivalRecord extends FingerPrintRecord implements Serializable {

    @ManyToOne
    ServiceSession serviceSession;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date sessionDate;
    
    private SessionInstance sessionInstance;

    public ServiceSession getServiceSession() {
        return serviceSession;
    }

    public void setServiceSession(ServiceSession serviceSession) {
        this.serviceSession = serviceSession;
    }


    
    public Date getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }
    
    

}
