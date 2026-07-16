package com.divudi.bean.kiosk;

import com.divudi.core.data.PatientRegistrationSource;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the kiosk self-registration page.
 * No authenticated user is required — kiosk terminals are pre-authenticated at
 * the device level. All patients registered here receive
 * {@link PatientRegistrationSource#KIOSK}.
 *
 * Issue: hmislk/hmis#21198
 */
@Named
@ViewScoped
public class KioskController implements Serializable {

    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PersonFacade personFacade;

    private Patient patient;
    private String searchPhone;
    private List<Patient> searchResults;

    private boolean showSearch = true;
    private boolean showResults = false;
    private boolean showNewForm = false;
    private boolean showDone = false;

    public void searchByPhone() {
        if (searchPhone == null || searchPhone.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a phone number to search.");
            return;
        }
        Long phone = CommonFunctions.removeSpecialCharsInPhonenumber(searchPhone.trim());
        if (phone == null) {
            JsfUtil.addErrorMessage("Please enter a valid phone number (digits only, e.g. 0771234567).");
            return;
        }
        searchPhone = phone.toString();
        Map<String, Object> params = new HashMap<>();
        params.put("ph", phone);
        searchResults = patientFacade.findByJpql(
                "select p from Patient p where p.retired=false and (p.patientPhoneNumber=:ph or p.patientMobileNumber=:ph) order by p.id desc",
                params);
        showSearch = false;
        showResults = true;
    }

    public void selectExistingPatient(Patient selectedPt) {
        this.patient = selectedPt;
        showResults = false;
        showDone = true;
    }

    public void startNewPatient() {
        patient = new Patient();
        patient.setPerson(new Person());
        patient.getPerson().setPhone(searchPhone);
        patient.getPerson().setMobile(searchPhone);
        showResults = false;
        showNewForm = true;
    }

    public void saveNewPatient() {
        if (patient == null || patient.getPerson() == null) {
            JsfUtil.addErrorMessage("Unexpected error. Please start over.");
            return;
        }
        if (patient.getPerson().getTitle() == null) {
            JsfUtil.addErrorMessage("Title is required.");
            return;
        }
        if (patient.getPerson().getName() == null || patient.getPerson().getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Full name is required.");
            return;
        }
        if (patient.getPerson().getSex() == null) {
            JsfUtil.addErrorMessage("Gender is required.");
            return;
        }
        if (patient.getPerson().getDob() == null) {
            JsfUtil.addErrorMessage("Date of birth is required.");
            return;
        }
        if (!Person.checkAgeSex(patient.getPerson().getDob(), patient.getPerson().getSex(), patient.getPerson().getTitle())) {
            JsfUtil.addErrorMessage("Title and Gender do not match. Please check Title, Age and Sex.");
            return;
        }

        Long phone = CommonFunctions.removeSpecialCharsInPhonenumber(searchPhone);
        if (phone == null) {
            JsfUtil.addErrorMessage("Invalid phone number. Please start over.");
            return;
        }
        patient.setPatientPhoneNumber(phone);
        patient.setPatientMobileNumber(phone);
        patient.getPerson().setPhone(phone.toString());
        patient.getPerson().setMobile(phone.toString());
        patient.setRegistrationSource(PatientRegistrationSource.KIOSK);
        patient.getPerson().setCreatedAt(new Date());
        personFacade.create(patient.getPerson());
        patient.setCreatedAt(new Date());
        patientFacade.create(patient);

        showNewForm = false;
        showDone = true;
    }

    public void reset() {
        patient = null;
        searchPhone = null;
        searchResults = null;
        showSearch = true;
        showResults = false;
        showNewForm = false;
        showDone = false;
    }

    public void updateSexByTitle() {
        if (patient == null || patient.getPerson() == null || patient.getPerson().getTitle() == null) {
            return;
        }
        Title title = patient.getPerson().getTitle();
        switch (title) {
            case Mrs:
            case Ms:
            case Miss:
            case DrMrs:
            case DrMs:
            case DrMiss:
            case ProfMrs:
                patient.getPerson().setSex(Sex.Female);
                break;
            case Mr:
            case Master:
                patient.getPerson().setSex(Sex.Male);
                break;
            default:
                break;
        }
    }

    public Title[] getTitles() {
        return Title.values();
    }

    public Sex[] getSexValues() {
        return Sex.values();
    }

    // Getters and setters

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getSearchPhone() {
        return searchPhone;
    }

    public void setSearchPhone(String searchPhone) {
        this.searchPhone = searchPhone;
    }

    public List<Patient> getSearchResults() {
        return searchResults;
    }

    public boolean isShowSearch() {
        return showSearch;
    }

    public boolean isShowResults() {
        return showResults;
    }

    public boolean isShowNewForm() {
        return showNewForm;
    }

    public boolean isShowDone() {
        return showDone;
    }
}
