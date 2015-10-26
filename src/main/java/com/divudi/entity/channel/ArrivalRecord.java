/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.channel;

import com.divudi.entity.BillSession;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.hr.FingerPrintRecord;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ArrivalRecord extends FingerPrintRecord implements Serializable {

    @ManyToOne
    ServiceSession serviceSession;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date sessionDate;

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
    
    

}
