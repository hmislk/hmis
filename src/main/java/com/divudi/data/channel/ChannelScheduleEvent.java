/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.channel;

import com.divudi.entity.ServiceSession;
import org.primefaces.model.DefaultScheduleEvent;
/**
 *
 * @author buddhika
 */
public class ChannelScheduleEvent extends DefaultScheduleEvent{
    ServiceSession serviceSession;

    public ServiceSession getServiceSession() {
        return serviceSession;
    }

    public void setServiceSession(ServiceSession serviceSession) {
        this.serviceSession = serviceSession;
    }
    
    
}
