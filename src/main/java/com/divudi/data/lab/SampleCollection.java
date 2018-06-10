/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillItem;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientSample;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author buddhika_ari
 */
public class SampleCollection {

    private Patient patient;
    private Set<SampleBill> sampleBills;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Set<SampleBill> getSampleBills() {
        if(sampleBills==null){
            sampleBills = new HashSet<>();
        }
        return sampleBills;
    }

    public void setSampleBills(Set<SampleBill> sampleBills) {
        this.sampleBills = sampleBills;
    }

}
