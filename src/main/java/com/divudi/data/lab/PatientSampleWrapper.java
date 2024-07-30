package com.divudi.data.lab;

import com.divudi.entity.lab.PatientSample;
import com.divudi.java.CommonFunctions;
import java.util.Objects;

/**
 * Represents a wrapper for PatientSample.
 * 
 * @version 1.0
 * @since 2024-07-27
 */
public class PatientSampleWrapper {
    private PatientSample patientSample;
    private boolean selected;
    private String uuid;

    public PatientSampleWrapper() {
    }

    public PatientSampleWrapper(PatientSample patientSample) {
        this.patientSample = patientSample;
    }

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

    public String getUuid() {
        if (uuid == null || uuid.trim().equals("")) {
            uuid = CommonFunctions.generateUuid();
        }
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "PatientSampleWrapper{" +
                "uuid='" + getUuid() + '\'' +
                ", patientSample=" + patientSample +
                ", selected=" + selected +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PatientSampleWrapper that = (PatientSampleWrapper) obj;
        return Objects.equals(getUuid(), that.getUuid());
    }
}
