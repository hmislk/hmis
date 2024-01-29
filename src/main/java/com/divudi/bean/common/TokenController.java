package com.divudi.bean.common;

import com.divudi.data.TokenType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.Staff;
import com.divudi.entity.Token;
import com.divudi.entity.WebUser;
import com.divudi.facade.TokenFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author acer
 */
@Named
@SessionScoped
public class TokenController implements Serializable, ControllerWithPatient {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    TokenFacade tokenFacade;
    // </editor-fold> 

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    // </editor-fold> 

    // <editor-fold defaultstate="collapsed" desc="Class variables">
    private Token currentToken;
    private Token removeingToken;
    private List<Token> currentTokens;
    private Department department;
    private Institution institution;
    private Department counter;
    private Patient patient;

    private boolean patientDetailsEditable;

    // </editor-fold> 
    public TokenController() {

    }

    private void resetClassVariables() {
        currentToken = null;
        removeingToken = null;
        currentTokens = null;
    }

    public String navigateToTokenIndex() {
        resetClassVariables();
        return "/token/index?faces-redirect?";
    }

    public String navigateToCreateNewPharmacyToken() {
        currentToken = new Token();

        currentToken.setTokenType(TokenType.PHARMACY_TOKEN);

        currentToken.setDepartment(sessionController.getDepartment());
        currentToken.setFromDepartment(sessionController.getDepartment());
        currentToken.setPatient(getPatient());
        currentToken.setInstitution(sessionController.getInstitution());
        currentToken.setFromInstitution(sessionController.getInstitution());

        if (getCounter() == null) {
            if (sessionController.getLoggableSubDepartments() != null
                    && !sessionController.getLoggableSubDepartments().isEmpty()) {
                counter = sessionController.getLoggableSubDepartments().get(0);
            }
        }
        currentToken.setCounter(getCounter());
        if (counter != null) {
            currentToken.setToDepartment(counter.getSuperDepartment());
            if (counter.getSuperDepartment() != null) {
                currentToken.setToInstitution(counter.getSuperDepartment().getInstitution());
            }
        }

        return "/token/pharmacy_token";
    }

    public String navigateToManagePharmacyTokens() {
        fillPharmacyTokens();
        return "/token/pharmacy_tokens";
    }

    public void fillPharmacyTokens() {
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.completed=:com";
        Map m = new HashMap();
        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("com", false);
        j += " order by t.id";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public String settlePharmacyToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token");
            return "";
        }
        if (currentToken.getTokenType() == null) {
            JsfUtil.addErrorMessage("Wrong Token");
            return "";
        }
        if (getPatient() == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        } else {
            currentToken.setPatient(patient);
        }
        if (currentToken.getToDepartment() == null) {
            currentToken.setToDepartment(sessionController.getDepartment());
        }
        if (currentToken.getToInstitution() == null) {
            currentToken.setToInstitution(sessionController.getInstitution());
        }
        currentToken.setTokenNumber(billNumberGenerator.generateDailyTokenNumber(currentToken.getFromDepartment(), null, null, TokenType.PHARMACY_TOKEN));
        currentToken.setTokenDate(new Date());
        tokenFacade.create(currentToken);
        return "/token/pharmacy_token_print";
    }

    public void listTokens() {

    }

    public void save(Token st) {
        if (st == null) {
            return;
        }
        if (st.getId() == null) {
            st.setCreatedAt(new Date());
            st.setCreatedBy(sessionController.getLoggedUser());
            tokenFacade.create(st);
        } else {
            tokenFacade.edit(st);
        }
    }

    public void callToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCalled(true);
        currentToken.setCalledAt(new Date());
        tokenFacade.edit(currentToken);
    }

    public String getTokenStatus(Token token) {
        if (token.isRetired()) {
            return "Retired";
        } else if (token.isCompleted()) {
            return "Completed";
        } else if (token.isInProgress()) {
            return "In Progress";
        } else if (token.isCalled()) {
            return "Called";
        } else {
            return "Pending";
        }
    }

    public void startTokenService() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setInProgress(true);
        currentToken.setStartedAt(new Date());
        tokenFacade.edit(currentToken);
    }

    public void completeTokenService() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        if (!currentToken.isCalled() || !currentToken.isInProgress()) {
            JsfUtil.addErrorMessage("Token needs to be called and in progress to complete");
            return;
        }
        currentToken.setCompleted(true);
        currentToken.setCompletedAt(new Date());
        tokenFacade.edit(currentToken);
    }

    public void reverseCallToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCalled(false);
        currentToken.setCalledAt(null);
        tokenFacade.edit(currentToken);
    }

    public void recallToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        // Set called to true, but keep the original call time
        currentToken.setCalled(true);
        tokenFacade.edit(currentToken);
    }

    public void restartTokenService() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setInProgress(false);
        currentToken.setStartedAt(null);
        tokenFacade.edit(currentToken);
    }

    public void reverseCompleteTokenService() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCompleted(false);
        currentToken.setCompletedAt(null);
        tokenFacade.edit(currentToken);
    }

    public void fetchNextToken() {

    }

    public void fetchPreviousToken() {

    }

    public void searchToken() {

    }

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    // </editor-fold> 
    public Token getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(Token currentToken) {
        this.currentToken = currentToken;
    }

    public Token getRemoveingToken() {
        return removeingToken;
    }

    public void setRemoveingToken(Token removeingToken) {
        this.removeingToken = removeingToken;
    }

    public List<Token> getCurrentTokens() {
        return currentTokens;
    }

    public void setCurrentTokens(List<Token> currentTokens) {
        this.currentTokens = currentTokens;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getCounter() {
        return counter;
    }

    public void setCounter(Department counter) {
        this.counter = counter;
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            Person p = new Person();
            patientDetailsEditable = true;

            patient.setPerson(p);
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

}
