/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.inward;

import com.divudi.entity.ServiceSession;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.entity.inward.Reservation;
import org.primefaces.model.DefaultScheduleEvent;
/**
 *
 * @author buddhika
 */
public class InwardReservationEvent extends DefaultScheduleEvent{
    private Reservation reservation;

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

   
    
    
    
    
}
