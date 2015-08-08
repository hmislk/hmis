/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
