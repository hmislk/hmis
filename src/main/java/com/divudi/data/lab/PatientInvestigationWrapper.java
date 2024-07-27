package com.divudi.data.lab;

import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.java.CommonFunctions;

/**
 *
 * @author Buddhika
 */
public class PatientInvestigationWrapper {

    private PatientInvestigation patientInvestigation;
    private boolean selected;
    private String uuid;

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
        return "PatientInvestigationWrapper{" +
                "uuid='" + getUuid() + '\'' +
                ", patientInvestigation=" + patientInvestigation +
                ", selected=" + selected +
                '}';
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PatientInvestigationWrapper that = (PatientInvestigationWrapper) obj;
        return getUuid().equals(that.getUuid());
    }
}
