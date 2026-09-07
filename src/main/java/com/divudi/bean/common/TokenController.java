package com.divudi.bean.common;

import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.TokenType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Token;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.PaymentMethod;
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
    PharmacyBillSearch pharmacyBillSearch;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class variables">
    private Token currentToken;

    private PaymentMethod paymentMethod;

    private Token removeingToken;
    private List<Token> inprogressTokensSaleForCashier;
     private List<Token> currentTokensSaleForCashier;
    private List<Token> currentTokens;
    private List<Token> currentTokensCounterWise;
    private Department department;
    private Institution institution;
    private Department counter;
    private Patient patient;
    private Department selectedCounter;

    private boolean patientDetailsEditable;

    // </editor-fold>
    public TokenController() {

    }

    private void resetClassVariables() {
        currentToken = null;
        removeingToken = null;
        currentTokens = null;
        patient = null;
    }

    public String navigateToTokenIndex() {
        resetClassVariables();
        return "/token/index?faces-redirect?";
    }

    public String navigateToCreateNewPharmacyToken() {
        resetClassVariables();
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

    public void logoutFromTvScreen() {
        selectedCounter = null;
    }

    public void fillPharmacyTokens() {
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.tokenType=:ty"
                + " and t.completed=:com";
        Map m = new HashMap();
        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("ty", TokenType.PHARMACY_TOKEN);
        m.put("com", false);
        if (counter != null) {
            j += " and t.counter =:ct";
            m.put("ct", counter);
        }
        j += " order by t.id DESC";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public String navigateToManagePharmacyTokensCompleted() {
        fillPharmacyTokensCompleted();
        return "/token/pharmacy_tokens_completed";
    }

    public void fillPharmacyTokensCompleted() {
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.tokenType=:ty"
                + " and t.completed=:com";
        Map m = new HashMap();

        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("ty", TokenType.PHARMACY_TOKEN);
        m.put("com", true);
        if (counter != null) {
            j += " and t.counter =:ct";
            m.put("ct", counter);
        }
        j += " order by t.id";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public String navigateToManagePharmacyTokensCalled() {
        fillPharmacyTokensCalled();
        return "/token/pharmacy_tokens_called"; // Adjust the navigation string as per your page structure
    }

    public String navigateToManagePharmacyTokensCalledByCounter() {
        return "/token/pharmacy_tokens_called_counter_wise"; // Adjust the navigation string as per your page structure
    }

    public void fillSaleForCashierCalledBillsTokens(){
        Map m = new HashMap();
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.bill.cancelled = false"
                + " and t.bill.retired = false"
                + " and t.bill.billTypeAtomic = :bta"
                + " and t.called=:cal "
                + " and t.tokenType=:ty"
                + " and t.completed=:com"
                + " and t.displayToken = true"
                + " and t.retired = false";
        
        Bill b = new Bill();
        b.getBillTypeAtomic();
        m.put("dep", sessionController.getDepartment());
        m.put("bta", BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        m.put("cal", true); // Tokens that are called
        m.put("com", false); // Tokens that are not completed
        m.put("ty", TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
        j += " order by t.id";
        currentTokensSaleForCashier = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }
    
    public void fillInprogressSaleForCashierBillsTokens(){
        Map m = new HashMap();
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.bill.cancelled = false"
                + " and t.bill.retired = false"
                + " and t.bill.billTypeAtomic = :bta"
                + " and t.called=:cal "
                + " and t.tokenType=:ty"
                + " and t.inProgress=:prog "
                + " and t.completed=:com "
                + " and t.displayToken = true"
                + " and t.retired = false";
        
        Bill b = new Bill();
        b.getBillTypeAtomic();
        m.put("dep", sessionController.getDepartment());
        m.put("bta", BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        m.put("cal", false); // Tokens that are called
        m.put("prog", true); // Tokens that are not in progress
        m.put("com", false); // Tokens that are not completed
        m.put("ty", TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
        j += " order by t.id";
        inprogressTokensSaleForCashier = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public void fillPharmacyTokensCalled() {
        Map<String, Object> m = new HashMap<>();
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.called=:cal "
                + " and t.tokenType=:ty"
                + " and t.inProgress=:prog "
                + " and t.completed=:com"; // Add conditions to filter out tokens that are in progress or completed
        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("cal", true); // Tokens that are called
        m.put("prog", false); // Tokens that are not in progress
        m.put("com", false); // Tokens that are not completed
        m.put("ty", TokenType.PHARMACY_TOKEN);
        j += " order by t.id";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public void fillPharmacyTokensCalledCounterWise() {
        Map<String, Object> m = new HashMap<>();
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.tokenType=:ty"
                + " and t.called=:cal "
                + " and t.inProgress=:prog "
                + " and t.completed=:com"; // Add conditions to filter out tokens that are in progress or completed
        if (selectedCounter != null) {
            j += " and t.counter =:ct";
            m.put("ct", selectedCounter);
        }
        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("cal", true); // Tokens that are called
        m.put("prog", false); // Tokens that are not in progress
        m.put("ty", TokenType.PHARMACY_TOKEN);
        m.put("com", false); // Tokens that are not completed
        j += " order by t.id";
        currentTokensCounterWise = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }
    
    public Token findPharmacyTokenSaleForCashier(Bill bill, TokenType tokenType) {
        if (bill == null) {
            return new Token();
        }
        String j = "Select t "
                + " from Token t"
                + " where t.bill=:bill"
                + " and t.tokenType=:ty"; // Add conditions to filter out tokens that are in progress or completed
        Map<String, Object> m = new HashMap<>();
        m.put("bill", bill);
        m.put("ty", tokenType);
        return tokenFacade.findFirstByJpql(j, m);
    }

    public Token findPharmacyTokens(Bill bill) {
        if (bill == null) {
            return new Token();
        }
        String j = "Select t "
                + " from Token t"
                + " where t.bill=:bill"
                + " and t.tokenType=:ty"; // Add conditions to filter out tokens that are in progress or completed
        Map<String, Object> m = new HashMap<>();
        m.put("bill", bill);
        m.put("ty", TokenType.PHARMACY_TOKEN);
        return tokenFacade.findFirstByJpql(j, m);
    }

    public Token findToken(Long id) {
        if (id == null) {
            return null;
        }
        String j = "Select t "
                + " from Token t "
                + " where t.id=:id"
                + " and t.tokenType=:ty";
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("ty", TokenType.PHARMACY_TOKEN);
        Token st = tokenFacade.findFirstByJpql(j, m);
        return st;
    }

    public String navigateToNewPharmacyBillForCashier() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }

        pharmacySaleController.resetAll();
        pharmacySaleController.setPatient(currentToken.getPatient());
        pharmacySaleController.setToken(currentToken);
        return pharmacySaleController.navigateToPharmacyBillForCashier();
    }

    public String navigateToNewPharmacyBillForCashierWholeSale() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }

        pharmacySaleController.resetAll();
        pharmacySaleController.setPatient(currentToken.getPatient());
        pharmacySaleController.setToken(currentToken);
        return pharmacySaleController.navigateToPharmacyBillForCashierWholeSale();
    }
    
    public String navigateToSaleForCashier() {
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

        if (!currentToken.getBill().getBillType().equals(BillType.PharmacyPre)) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        Bill tmp = billFacade.find(currentToken.getBill().getId());
        if (tmp.getBillItems() == null) {
            tmp.setBillItems(loadPharmacyBillItems(tmp));
        }

        pharmacyPreSettleController.setPreBill(tmp);
        pharmacyPreSettleController.setBillPreview(false);
        pharmacyPreSettleController.setToken(currentToken);
        pharmacySaleController.setBillSettlingStarted(false);
        tmp.setComments("Editing pharmacy token bill");
        pharmacyBillSearch.pharmacyRetailCancelBillWithStock(tmp);
        pharmacySaleController.resetAll();
        pharmacySaleController.setPatient(currentToken.getPatient());
        pharmacySaleController.setPreBill((PreBill) tmp);
        currentToken.getBill().setCancelled(true);
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier";

    }

    public String navigateToSaleForCashierToEditBillWithToken(Token token) {
        if (token == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }
        if (token.getBill() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        if (token.getBill().getBillType() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return "";
        }

        if (!token.getBill().getBillType().equals(BillType.PharmacyPre)) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        Bill tmp = billFacade.find(token.getBill().getId());
        if (tmp.getBillItems() == null) {
            tmp.setBillItems(loadPharmacyBillItems(tmp));
        }

        pharmacyPreSettleController.setPreBill(tmp);
        pharmacyPreSettleController.setBillPreview(false);
        pharmacyPreSettleController.setToken(token);
        pharmacySaleController.setBillSettlingStarted(false);
        tmp.setComments("Editing pharmacy token bill");
        pharmacyBillSearch.pharmacyRetailCancelBillWithStock(tmp);
        pharmacySaleController.resetAll();
        pharmacySaleController.setPatient(token.getPatient());
        pharmacySaleController.setPreBill((PreBill) tmp);
        token.getBill().setCancelled(true);
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";

    }

    public String navigateToSettlePharmacyPreBill() {
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
        if (!currentToken.getBill().getBillType().equals(BillType.PharmacyPre)) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        pharmacyPreSettleController.setPreBill(currentToken.getBill());
        pharmacyPreSettleController.setBillPreview(false);
        pharmacyPreSettleController.setToken(currentToken);
        pharmacyPreSettleController.setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_pre_settle";
    }

    public List<BillItem> loadPharmacyBillItems(Bill bill) {
        Map m = new HashMap();
        String jpql = "select bi from BillItem bi"
                + " where bi.retired=:ret "
                + " and bi.bill=:b";
        m.put("b", bill);
        m.put("ret", false);
        return billItemFacade.findByJpql(jpql, m);

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
//        if (getPatient().getId() == null) {
//            JsfUtil.addErrorMessage("Please select a patient");
//            return "";
        if (getPatient().getPerson().getName() == null) {
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
        tokenFacade.create(currentToken);
        currentToken.setTokenNumber(billNumberGenerator.generateDailyTokenNumber(currentToken.getFromDepartment(), null, null, TokenType.PHARMACY_TOKEN));
        currentToken.setCounter(counter);
        currentToken.setTokenDate(new Date());
        currentToken.setTokenAt(new Date());
        tokenFacade.edit(currentToken);
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

    public void toggleCalledStatus() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCalled(!currentToken.isCalled());
        currentToken.setCalledAt(currentToken.isCalled() ? new Date() : null);
        tokenFacade.edit(currentToken);
    }

    public void toggleCompletedStatus() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCompleted(!currentToken.isCompleted());
        Date now = new Date();
        currentToken.setCompletedAt(currentToken.isCompleted() ? now : null);
        currentToken.setStartedAt(currentToken.isCompleted() ? (currentToken.getStartedAt() == null ? now : currentToken.getStartedAt()) : null);
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

    public Department getSelectedCounter() {
        return selectedCounter;
    }

    public void setSelectedCounter(Department selectedCounter) {
        this.selectedCounter = selectedCounter;
    }

    public List<Token> getCurrentTokensCounterWise() {
        return currentTokensCounterWise;
    }

    public void setCurrentTokensCounterWise(List<Token> currentTokensCounterWise) {
        this.currentTokensCounterWise = currentTokensCounterWise;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public void listnerForPaymentMethodChange() {
        // ToDo: Add Logic
    }

    public List<Token> getInprogressTokensSaleForCashier() {
        return inprogressTokensSaleForCashier;
    }

    public void setInprogressTokensSaleForCashier(List<Token> inprogressTokensSaleForCashier) {
        this.inprogressTokensSaleForCashier = inprogressTokensSaleForCashier;
    }

    public List<Token> getCurrentTokensSaleForCashier() {
        return currentTokensSaleForCashier;
    }

    public void setCurrentTokensSaleForCashier(List<Token> currentTokensSaleForCashier) {
        this.currentTokensSaleForCashier = currentTokensSaleForCashier;
    }

}
