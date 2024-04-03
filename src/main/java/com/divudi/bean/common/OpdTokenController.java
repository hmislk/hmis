/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.BillType;
import com.divudi.data.TokenType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.Token;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.TokenFacade;
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
 * @author Damiya
 */
@Named
@SessionScoped
public class OpdTokenController implements Serializable, ControllerWithPatient {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    TokenFacade tokenFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    // </editor-fold> 

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    PatientController patientController;
    @Inject
    private PharmacyBillSearch pharmacyBillSearch;
    @Inject
    OpdPreBillController opdPreBillController;
    @Inject
    OpdPreSettleController opdPreSettleController;

    // </editor-fold> 
    private Token currentToken;
    private Token removeingToken;
    private List<Token> currentTokens;
    private Patient patient;

    private Department department;
    private Institution institution;
    private Department counter;
    private Department selectedCounter;

    private boolean patientDetailsEditable;

    private boolean printPreview;
    
    public OpdTokenController() {
    }

    private void resetClassVariables() {
        currentToken = null;
        removeingToken = null;
        currentTokens = null;
        patient = null;
        printPreview = false;
    }

    public void saveToken(Token t) {
        if (t == null) {
            return;
        }
        if (t.getId() == null) {
            t.setCreatedAt(new Date());
            t.setCreatedBy(sessionController.getLoggedUser());
            tokenFacade.create(t);
        } else {
            tokenFacade.edit(t);
        }
    }

    public String navigateToCreateNewOpdToken() {
        resetClassVariables();
        currentToken = new Token();
        currentToken.setTokenType(TokenType.OPD_TOKEN);
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
        return "/opd/token/opd_token?faces-redirect=true";
    }

    public String settleOpdToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token");
            return "";
        }
        if (currentToken.getTokenType() == null) {
            JsfUtil.addErrorMessage("Wrong Token");
            return "";
        }
        if (getPatient().getId() == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return "";
        } else if (getPatient().getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return "";
        } else if (getPatient().getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please select a patient");
            return "";
        } else {
            patientController.save(patient);
            currentToken.setPatient(getPatient());
        }
        if (currentToken.getToDepartment() == null) {
            currentToken.setToDepartment(sessionController.getDepartment());
        }
        if (currentToken.getToInstitution() == null) {
            currentToken.setToInstitution(sessionController.getInstitution());
        }
        if (currentToken.getId() == null) {
            currentToken.setCreatedAt(new Date());
            currentToken.setCreatedBy(sessionController.getLoggedUser());
            tokenFacade.create(currentToken);
        } else {
            tokenFacade.edit(currentToken);
        }
        currentToken.setTokenNumber(billNumberGenerator.generateDailyTokenNumber(currentToken.getFromDepartment(), null, null, TokenType.OPD_TOKEN));
        currentToken.setCounter(counter);
        currentToken.setTokenDate(new Date());
        currentToken.setTokenAt(new Date());
        tokenFacade.edit(currentToken);
        printPreview = true;
        return "/opd/token/opd_token_print?faces-redirect=true";
    }

    public String navigateToManageOpdTokens() {
        fillOpdTokens();
        return "/opd/token/maage_opd_tokens?faces-redirect=true";
    }
    
    public String navigateToSettleOpdPreBill() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }
        if (currentToken.getBill() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        if (currentToken.getBill().getBillType() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return "";
        }
        
        findPreBill(currentToken.getBill());
        opdPreSettleController.setBillPreview(false);
        opdPreSettleController.setToken(currentToken);
        return "/opd_bill_pre_settle?faces-redirect=true";
    }
    
    public void findPreBill(Bill args) {
        Bill tmp;
        String sql = "Select b from BilledBill b"
                + " where b.referenceBill=:bil"
                + " and b.retired=false "
                + " and b.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("bil", args);
        tmp = billFacade.findFirstByJpql(sql, hm);

        if (tmp != null) {
            JsfUtil.addErrorMessage("Allready Paid");
            return;
        }    
        else{
            opdPreSettleController.setPreBill(args);
        }
    }

    public String navigateToNewOpdBillForCashier() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }

        opdPreBillController.makeNull();
        opdPreBillController.setPatient(currentToken.getPatient());
        opdPreBillController.setToken(currentToken);
        return "/opd/opd_pre_bill?faces-redirect=true";
    }

    public void fillOpdTokens() {
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.tokenType=:ty"
                + " and t.completed=:com";
        Map m = new HashMap();
        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("ty", TokenType.OPD_TOKEN);
        m.put("com", false);
        if (counter != null) {
            j += " and t.counter =:ct";
            m.put("ct", counter);
        }
        j += " order by t.id DESC";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
        System.out.println("currentTokens " + currentTokens);
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

    public String navigateToTokenIndex() {
        resetClassVariables();
        return "/opd/token/index?faces-redirect=true";
    }

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

    public PharmacyBillSearch getPharmacyBillSearch() {
        return pharmacyBillSearch;
    }

    public void setPharmacyBillSearch(PharmacyBillSearch pharmacyBillSearch) {
        this.pharmacyBillSearch = pharmacyBillSearch;
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

    public Department getSelectedCounter() {
        return selectedCounter;
    }

    public void setSelectedCounter(Department selectedCounter) {
        this.selectedCounter = selectedCounter;
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

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

}
