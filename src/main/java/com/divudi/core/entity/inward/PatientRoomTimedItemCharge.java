/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Snapshot row created at room assignment: records which TimedItem was active
 * at admission time and tracks the calculated charge for that item's duration.
 */
@Entity
public class PatientRoomTimedItemCharge implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PatientRoom patientRoom;

    @ManyToOne
    private TimedItem timedItem;

    private double calculatedCharge;
    private double discountCharge;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientRoom getPatientRoom() {
        return patientRoom;
    }

    public void setPatientRoom(PatientRoom patientRoom) {
        this.patientRoom = patientRoom;
    }

    public TimedItem getTimedItem() {
        return timedItem;
    }

    public void setTimedItem(TimedItem timedItem) {
        this.timedItem = timedItem;
    }

    public double getCalculatedCharge() {
        return calculatedCharge;
    }

    public void setCalculatedCharge(double calculatedCharge) {
        this.calculatedCharge = calculatedCharge;
    }

    public double getDiscountCharge() {
        return discountCharge;
    }

    public void setDiscountCharge(double discountCharge) {
        this.discountCharge = discountCharge;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PatientRoomTimedItemCharge)) {
            return false;
        }
        PatientRoomTimedItemCharge other = (PatientRoomTimedItemCharge) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "PatientRoomTimedItemCharge[id=" + id + "]";
    }
}
