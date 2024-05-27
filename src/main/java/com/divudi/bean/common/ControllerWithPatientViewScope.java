package com.divudi.bean.common;

import com.divudi.entity.Patient;
import java.util.List;

public interface ControllerWithPatientViewScope {

    public Patient getPatient();

    public void setPatient(Patient patient);

    public boolean isPatientDetailsEditable();

    public void setPatientDetailsEditable(boolean patientDetailsEditable);

    public void toggalePatientEditable();

    public List<Patient> getQuickSearchPatientList();

    public void setQuickSearchPatientList(List<Patient> quickSearchPatientList);

    public void selectQuickOneFromQuickSearchPatient();

    public void saveSelected(Patient p);

    public void saveSelectedPatient();

    public String getQuickSearchPhoneNumber();
    
     public void setQuickSearchPhoneNumber(String quickSearchPhoneNumber);

    public void quickSearchPatientLongPhoneNumber();

    public void quickSearchNewPatient();
}
