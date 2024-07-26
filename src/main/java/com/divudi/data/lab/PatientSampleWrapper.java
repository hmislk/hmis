package com.divudi.data.lab;

import com.divudi.entity.lab.PatientSample;

/**
 *
 * @author Buddhika
 */
public class PatientSampleWrapper {
    private PatientSample patientSample;
    private boolean selected;

    public PatientSample getPatientSample() {
        return patientSample;
    }

    public void setPatientSample(PatientSample patientSample) {
        this.patientSample = patientSample;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    
}
