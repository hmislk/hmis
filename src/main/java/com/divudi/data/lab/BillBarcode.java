package com.divudi.data.lab;

import com.divudi.entity.Bill;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Buddhika
 */
public class BillBarcode {
    private Bill bill;    
    private List<PatientInvestigationWrapper> patientInvestigationWrappers;
    private List<PatientSampleWrapper> patientSampleWrappers;

    public Bill getBill() {
        return bill;
    }

    public BillBarcode() {
    }

    public BillBarcode(Bill bill) {
        this.bill = bill;
    }
    
    

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public List<PatientInvestigationWrapper> getPatientInvestigationWrappers() {
        if(patientInvestigationWrappers==null){
            patientInvestigationWrappers=new ArrayList<>();
        }
        return patientInvestigationWrappers;
    }

    public void setPatientInvestigationWrappers(List<PatientInvestigationWrapper> patientInvestigationWrappers) {
        this.patientInvestigationWrappers = patientInvestigationWrappers;
    }

    public List<PatientSampleWrapper> getPatientSampleWrappers() {
        return patientSampleWrappers;
    }

    public void setPatientSampleWrappers(List<PatientSampleWrapper> patientSampleWrappers) {
        this.patientSampleWrappers = patientSampleWrappers;
    }

   
    
    
}
