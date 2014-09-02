/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import java.util.List;

/**
 *
 * @author safrin
 */
public class InstitutionEncounters {

    private Institution institution;
    private List<PatientEncounter> patientEncounters;
    private double returned;
    private double total;
    private double paidTotal;
    double paidTotalPatient;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

  

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getPaidTotal() {
        return paidTotal;
    }

    public void setPaidTotal(double paidTotal) {
        this.paidTotal = paidTotal;
    }

    public double getReturned() {
        return returned;
    }

    public void setReturned(double returned) {
        this.returned = returned;
    }

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }

    public double getPaidTotalPatient() {
        return paidTotalPatient;
    }

    public void setPaidTotalPatient(double paidTotalPatient) {
        this.paidTotalPatient = paidTotalPatient;
    }
    
    
}
