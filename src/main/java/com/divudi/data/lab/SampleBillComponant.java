/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import com.divudi.entity.BillComponent;
import com.divudi.entity.lab.PatientInvestigation;
import java.util.List;

/**
 *
 * @author buddhika_ari
 */
public class SampleBillComponant {

    private BillComponent billComponent;
    private PatientInvestigation patientInvestigation;
    private List<SampleTest> sampleTests;

    public BillComponent getBillComponent() {
        return billComponent;
    }

    public void setBillComponent(BillComponent billComponent) {
        this.billComponent = billComponent;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public List<SampleTest> getSampleTests() {
        return sampleTests;
    }

    public void setSampleTests(List<SampleTest> sampleTests) {
        this.sampleTests = sampleTests;
    }
}
