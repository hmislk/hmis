/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.bean.common;

import com.divudi.entity.Patient;
import com.divudi.entity.Staff;
import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 *
 * @author root
 */
@Named(value = "transferController")
@SessionScoped
public class TransferController implements Serializable {

    
    Patient patient;
    Staff staff;
    
    /**
     * Creates a new instance of TransferController
     */
    public TransferController() {
    }

    public Patient getPatient() {
//        //////// // System.out.println("getting patient = " + patient);
        return patient;
    }

    public void setPatient(Patient patient) {
//        //////// // System.out.println("setting patient = " + patient);
        this.patient = patient;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }
    
    
    
}
