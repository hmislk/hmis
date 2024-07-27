package com.divudi.data.lab;

import com.divudi.entity.lab.PatientInvestigation;

/**
 *
 * @author Buddhika
 */
public class PatientInvestigationWrapper {
    private PatientInvestigation patientInvestigation;
    private boolean selected;

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public PatientInvestigationWrapper() {
    }

    public PatientInvestigationWrapper(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    
    
    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
}
