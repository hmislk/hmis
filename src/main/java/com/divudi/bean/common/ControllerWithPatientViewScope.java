package com.divudi.bean.common;

import com.divudi.core.entity.Patient;

import java.util.List;

public interface ControllerWithPatientViewScope {

    Patient getPatient();

    void setPatient(Patient patient);

    boolean isPatientDetailsEditable();

    void setPatientDetailsEditable(boolean patientDetailsEditable);

    void toggalePatientEditable();

    List<Patient> getQuickSearchPatientList();

    void setQuickSearchPatientList(List<Patient> quickSearchPatientList);

    void selectQuickOneFromQuickSearchPatient();

    void saveSelected(Patient p);

    void saveSelectedPatient();

    String getQuickSearchPhoneNumber();

    void setQuickSearchPhoneNumber(String quickSearchPhoneNumber);

    void quickSearchPatientLongPhoneNumber();

    void quickSearchNewPatient();
}
