package com.divudi.bean.clientportal;

import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.ClientAccountCreationChannel;
import com.divudi.core.entity.ClientAccount;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.ClientAccountFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ViewScoped
public class ClientPortalStaffRegistrationController implements Serializable {

    private static final int MAX_SEARCH_RESULTS = 50;

    @EJB
    private PatientFacade patientFacade;
    @EJB
    private ClientAccountFacade clientAccountFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    private SecurityController securityController;
    @Inject
    private WebUserController webUserController;

    private String searchPhone;
    private String searchPhn;
    private String searchName;
    private List<Patient> searchResults;
    private boolean searchAttempted;

    private Patient selectedPatient;
    private boolean duplicateAccountBlocked;
    private ClientAccount existingAccount;

    private String password;
    private String passwordConfirm;
    private boolean registrationComplete;

    private void clearMessages() {
        java.util.Iterator<javax.faces.application.FacesMessage> iter = javax.faces.context.FacesContext.getCurrentInstance().getMessages();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    public String navigateToStaffAssistedRegistration() {
        if (!webUserController.hasPrivilege("ClientPortalCreateAccount")) {
            JsfUtil.addErrorMessage("You do not have permission to create client portal accounts.");
            return null;
        }
        resetAll();
        return "/admin/client_portal/staff_assisted_registration?faces-redirect=true";
    }

    private void resetAll() {
        searchPhone = null;
        searchPhn = null;
        searchName = null;
        searchResults = null;
        searchAttempted = false;
        selectedPatient = null;
        duplicateAccountBlocked = false;
        existingAccount = null;
        password = null;
        passwordConfirm = null;
        registrationComplete = false;
    }

    public void search() {
        clearMessages();
        selectedPatient = null;
        duplicateAccountBlocked = false;
        existingAccount = null;
        registrationComplete = false;
        searchAttempted = true;

        boolean hasPhone = searchPhone != null && !searchPhone.trim().isEmpty();
        boolean hasPhn = searchPhn != null && !searchPhn.trim().isEmpty();
        boolean hasName = searchName != null && !searchName.trim().isEmpty();
        if (!hasPhone && !hasPhn && !hasName) {
            JsfUtil.addErrorMessage("Enter a phone number, PHN, or name to search.");
            searchResults = new ArrayList<>();
            return;
        }

        StringBuilder jpql = new StringBuilder("select p from Patient p where p.retired=false");
        Map<String, Object> params = new HashMap<>();
        if (hasPhone) {
            jpql.append(" and p.patientPhoneNumber=:pp");
            params.put("pp", CommonFunctions.convertStringToLongOrZero(searchPhone.trim()));
        }
        if (hasPhn) {
            jpql.append(" and upper(p.phn)=:phn");
            params.put("phn", searchPhn.trim().toUpperCase());
        }
        if (hasName) {
            jpql.append(" and upper(p.person.name) like :name");
            params.put("name", "%" + searchName.trim().toUpperCase() + "%");
        }
        jpql.append(" order by p.id desc");

        searchResults = patientFacade.findByJpql(jpql.toString(), params, MAX_SEARCH_RESULTS);
    }

    public void selectPatient(Patient patient) {
        clearMessages();
        registrationComplete = false;
        password = null;
        passwordConfirm = null;
        if (patient == null || patient.getPerson() == null) {
            JsfUtil.addErrorMessage("Selected patient has no linked person record.");
            selectedPatient = null;
            return;
        }
        selectedPatient = patient;
        // UX-only fast check; the authoritative check is the row-locked one inside
        // clientAccountFacade.createIfNoActiveAccount() called from register().
        existingAccount = clientAccountFacade.findByPerson(patient.getPerson().getId());
        duplicateAccountBlocked = existingAccount != null;
    }

    public void register() {
        clearMessages();
        if (!webUserController.hasPrivilege("ClientPortalCreateAccount")) {
            JsfUtil.addErrorMessage("You do not have permission to create client portal accounts.");
            return;
        }
        if (selectedPatient == null || selectedPatient.getPerson() == null) {
            JsfUtil.addErrorMessage("Select a patient first.");
            return;
        }
        if (password == null || password.length() < 6) {
            JsfUtil.addErrorMessage("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(passwordConfirm)) {
            JsfUtil.addErrorMessage("Password and confirmation do not match.");
            return;
        }

        Person person = selectedPatient.getPerson();

        ClientAccount account = new ClientAccount();
        account.setPerson(person);
        account.setPasswordHash(securityController.hashAndCheck(password));
        account.setVerifiedPhone(selectedPatient.getPatientPhoneNumber() == null
                ? null : String.valueOf(selectedPatient.getPatientPhoneNumber()));
        account.setPhoneVerified(false);
        account.setEmailVerified(false);
        account.setCreatedVia(ClientAccountCreationChannel.STAFF_ASSISTED);
        account.setCreatedByWebUser(sessionController.getLoggedUser());
        account.setCreatedAt(new Date());
        account.setRetired(false);

        ClientAccount created = clientAccountFacade.createIfNoActiveAccount(person.getId(), account);
        if (created == null) {
            duplicateAccountBlocked = true;
            existingAccount = clientAccountFacade.findByPerson(person.getId());
            JsfUtil.addErrorMessage("A portal account already exists for this person. Use login or password-reset instead.");
            return;
        }

        registrationComplete = true;
    }

    public boolean isSearchResultsEmpty() {
        return searchResults == null || searchResults.isEmpty();
    }

    public String getSearchPhone() {
        return searchPhone;
    }

    public void setSearchPhone(String searchPhone) {
        this.searchPhone = searchPhone;
    }

    public String getSearchPhn() {
        return searchPhn;
    }

    public void setSearchPhn(String searchPhn) {
        this.searchPhn = searchPhn;
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public List<Patient> getSearchResults() {
        if (searchResults == null) {
            return new ArrayList<>();
        }
        return searchResults;
    }

    public void setSearchResults(List<Patient> searchResults) {
        this.searchResults = searchResults;
    }

    public boolean isSearchAttempted() {
        return searchAttempted;
    }

    public void setSearchAttempted(boolean searchAttempted) {
        this.searchAttempted = searchAttempted;
    }

    public Patient getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatient(Patient selectedPatient) {
        this.selectedPatient = selectedPatient;
    }

    public boolean isDuplicateAccountBlocked() {
        return duplicateAccountBlocked;
    }

    public void setDuplicateAccountBlocked(boolean duplicateAccountBlocked) {
        this.duplicateAccountBlocked = duplicateAccountBlocked;
    }

    public ClientAccount getExistingAccount() {
        return existingAccount;
    }

    public void setExistingAccount(ClientAccount existingAccount) {
        this.existingAccount = existingAccount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public boolean isRegistrationComplete() {
        return registrationComplete;
    }

    public void setRegistrationComplete(boolean registrationComplete) {
        this.registrationComplete = registrationComplete;
    }
}
