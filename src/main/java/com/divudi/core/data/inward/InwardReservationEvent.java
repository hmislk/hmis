/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.inward;

import com.divudi.core.entity.inward.Reservation;
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
