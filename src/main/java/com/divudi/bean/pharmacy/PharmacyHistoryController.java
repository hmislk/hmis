/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.bean.pharmacy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

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
    
//    @Schedule(dayOfWeek = "Sat",hour = "0")
    public void createWeeklyHistory(){
        
    }
    
}
