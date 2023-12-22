package com.divudi.bean.common;

import com.divudi.entity.Patient;

public interface ControllerWithPatient {

    public Patient getPatient();

    public void setPatient(Patient patient);

    public boolean isPatientDetailsEditable();

    public void setPatientDetailsEditable(boolean patientDetailsEditable);

    public void toggalePatientEditable();
}
