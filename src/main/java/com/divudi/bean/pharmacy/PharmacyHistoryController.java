/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.bean.pharmacy;

import javax.ejb.Schedule;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;

/**
 *
 * @author Buddhika
 */
@Named(value = "pharmacyHistoryController")
@ApplicationScoped
public class PharmacyHistoryController {

    /**
     * Creates a new instance of PharmacyHistoryController
     */
    public PharmacyHistoryController() {
    }
    
    @Schedule(dayOfWeek = "Sat",hour = "0")
    public void createWeeklyHistory(){
        
    }
    
}
