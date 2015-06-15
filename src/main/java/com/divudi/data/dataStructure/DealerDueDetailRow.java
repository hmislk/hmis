/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;

/**
 *
 * @author Buddhika
 */
public class DealerDueDetailRow {
    Institution dealer;
    Double zeroToThirty;
    PatientEncounter zeroToThirtyEncounter;
    
    Double thirtyToSixty;
    PatientEncounter thirtyToSixtyEncounter;
    Double sixtyToNinty;
    PatientEncounter sixtyToNintyEncounter;
    Double moreThanNinty;
    PatientEncounter moreThanNintyEncounter;

    public Institution getDealer() {
        return dealer;
    }

    public void setDealer(Institution dealer) {
        this.dealer = dealer;
    }

    public Double getZeroToThirty() {
        return zeroToThirty;
    }

    public void setZeroToThirty(Double zeroToThirty) {
        this.zeroToThirty = zeroToThirty;
    }

    public PatientEncounter getZeroToThirtyEncounter() {
        return zeroToThirtyEncounter;
    }

    public void setZeroToThirtyEncounter(PatientEncounter zeroToThirtyEncounter) {
        this.zeroToThirtyEncounter = zeroToThirtyEncounter;
    }

    public Double getThirtyToSixty() {
        return thirtyToSixty;
    }

    public void setThirtyToSixty(Double thirtyToSixty) {
        this.thirtyToSixty = thirtyToSixty;
    }

    public PatientEncounter getThirtyToSixtyEncounter() {
        return thirtyToSixtyEncounter;
    }

    public void setThirtyToSixtyEncounter(PatientEncounter thirtyToSixtyEncounter) {
        this.thirtyToSixtyEncounter = thirtyToSixtyEncounter;
    }

    public Double getSixtyToNinty() {
        return sixtyToNinty;
    }

    public void setSixtyToNinty(Double sixtyToNinty) {
        this.sixtyToNinty = sixtyToNinty;
    }

    public PatientEncounter getSixtyToNintyEncounter() {
        return sixtyToNintyEncounter;
    }

    public void setSixtyToNintyEncounter(PatientEncounter sixtyToNintyEncounter) {
        this.sixtyToNintyEncounter = sixtyToNintyEncounter;
    }

    public Double getMoreThanNinty() {
        return moreThanNinty;
    }

    public void setMoreThanNinty(Double moreThanNinty) {
        this.moreThanNinty = moreThanNinty;
    }

    public PatientEncounter getMoreThanNintyEncounter() {
        return moreThanNintyEncounter;
    }

    public void setMoreThanNintyEncounter(PatientEncounter moreThanNintyEncounter) {
        this.moreThanNintyEncounter = moreThanNintyEncounter;
    }
    
    
}
