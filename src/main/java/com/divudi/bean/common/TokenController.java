package com.divudi.bean.common;

import com.divudi.data.TokenType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.Staff;
import com.divudi.entity.Token;
import com.divudi.entity.WebUser;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;

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

        currentToken.setInstitution(sessionController.getInstitution());
        currentToken.setFromInstitution(sessionController.getInstitution());

        currentToken.setCounter(counter);
        currentToken.setToDepartment(counter.getSuperDepartment());
        currentToken.setToInstitution(counter.getSuperDepartment().getInstitution());

        return "/token/pharmacy_token";
    }

    public String settlePharmacyToken(){
        if(currentToken==null){
            JsfUtil.addErrorMessage("No token");
            return "";
        }
        if(currentToken.getTokenType()==null){
            JsfUtil.addErrorMessage("Wrong Token");
            return "";
        }
        if(getPatient()==null){
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }else{
            currentToken.setPatient(patient);
        }
        if(currentToken.getToDepartment()==null){
            currentToken.setToDepartment(sessionController.getDepartment());
        }
        if(currentToken.getToInstitution()==null){
            currentToken.setToInstitution(sessionController.getInstitution());
        }
        currentToken.setTokenNumber(billNumberGenerator.generateDailyBillNumberForOpd(department));
        return "";
    }
    
    

    public void listTokens() {

    }

    public void callToken() {

    }

    public void startTokenService() {

    }

    public void completeTokenService() {

    }

    public void reverseCallToken() {

    }

    public void recallToken() {

    }

    public void restartTokenService() {

    }

    public void reverseCompleteTokenService() {

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
