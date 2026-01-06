package com.divudi.bean.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.TokenType;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Token;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.Department;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.ejb.OptimizedPharmacyBean;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.PaymentService;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.service.pharmacy.TokenService;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.StaffService;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.TokenController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.service.pharmacy.StockSearchService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Fast retail sale optimized for settlement at cashier. Stocks are deducted but
 * payments are collected later at the cashier.
 */
@Named
@SessionScoped
public class PharmacyFastRetailSaleForCashierController extends PharmacyFastRetailSaleController implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    DrawerController drawerController;
    @Inject
    SearchController searchController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    TokenController tokenController;
    @EJB
    TokenService tokenService;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    TokenFacade tokenFacade;
    @EJB
    PaymentService paymentService;
    @EJB
    OptimizedPharmacyBean optimizedPharmacyBean;
    @EJB
    StaffService staffBean;
    @EJB
    PharmacyService pharmacyService;
    @EJB
    PharmacyCostingService pharmacyCostingService;
    @EJB
    StockSearchService stockSearchService;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @Inject
    UserStockController userStockController;
    @Inject
    BillBeanController billBeanController;

    private boolean billSettlingStarted;
    private UserStockContainer userStockContainer;
    private Staff toStaff;
    private Institution toInstitution;
    private Department counter;
    private double cashPaid;
    private double balance;
    private double netTotal;
    private PaymentMethod paymentMethod;
    private PaymentScheme paymentScheme;
    private PaymentMethodData paymentMethodData;
    private Token currentToken;
    private String comment;

    public PharmacyFastRetailSaleForCashierController() {
    }

    /**
     * Navigate to fast sale for cashier page with shift checks.
     */
    public String navigateToPharmacyFastRetailSaleForCashier() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_fast_retail_sale_for_cashier?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Please start the shift first");
                return "";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_fast_retail_sale_for_cashier?faces-redirect=true";
        }
    }

    /**
     * Finalize pre-bill, deduct stock and generate token. Payment will be
     * collected later at cashier.
     */
    public void settlePreBill() {
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return;
        }
        
        if(getPatient() != null && getPatient().getId() != null && configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management in the system", false) 
                && configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management for Pharmacy from the system", false)){
            if(getPatient().isBlacklisted()){
                JsfUtil.addErrorMessage("This patient is blacklisted from the system. Can't Bill.");
                return;
            }
        }
        
        for (BillItem bi : getPreBill().getBillItems()) {
            if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), sessionController.getLoggedUser())) {
                setZeroToQty(bi);
                JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                return;
            }
            if (bi.getQty() <= 0.0) {
                JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                return;
            }
        }
        Patient pt = savePatient();
        calculateAllRates();
        List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());
        getPreBill().setBillItems(null);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        savePreBillFinallyForRetailSaleForCashier(pt);
        savePreBillItemsFinally(tmpBillItems);
        setPrintBill(billFacade.find(getPreBill().getId()));
        if (configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false)) {
            if (getPatient() != null) {
                Token t = tokenController.findPharmacyTokens(getPreBill());
                if (t == null) {
                    settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                    markInprogress();
                } else {
                    markToken();
                }
            }
        }
        if (currentToken != null) {
            currentToken.setBill(getPreBill());
            tokenFacade.edit(currentToken);
        }
        resetAll();
        billSettlingStarted = false;
        billPreview = true;
    }

    private void savePreBillFinallyForRetailSaleForCashier(Patient pt) {
        if (getPreBill().getId() == null) {
            billFacade.create(getPreBill());
        }
        getPreBill().setDepartment(sessionController.getLoggedUser().getDepartment());
        getPreBill().setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(sessionController.getLoggedUser());
        getPreBill().setPatient(pt);
        getPreBill().setToStaff(toStaff);
        getPreBill().setToInstitution(toInstitution);
        getPreBill().setComments(comment);
        getPreBill().setCashPaid(cashPaid);
        getPreBill().setBalance(balance);
        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(sessionController.getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(paymentMethod);
        getPreBill().setPaymentScheme(getPaymentScheme());
        getBillBean().setPaymentMethodData(getPreBill(), paymentMethod, getPaymentMethodData());

        // Handle Department ID generation (independent)
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else {
            // Use existing method for backward compatibility
            deptId = billNumberBean.departmentBillNumberGenerator(
                    getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else {
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
                configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false) ||
                configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department
            } else {
                // Preserve old behavior: use existing institution method for backward compatibility
                insId = billNumberBean.institutionBillNumberGenerator(
                        getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
            }
        }

        getPreBill().setDeptId(deptId);
        getPreBill().setInsId(insId);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));
    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {
                continue;
            }
            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(sessionController.getLoggedUser());
            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);
            if (tbi.getId() == null) {
                billItemFacade.create(tbi);
            }
            if (tmpPh.getId() == null) {
                pharmaceuticalBillItemFacade.create(tmpPh);
            }
            tbi.setPharmaceuticalBillItem(tmpPh);
            billItemFacade.edit(tbi);
            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();
            boolean returnFlag;
            if (useOptimizedStockDeduction()) {
                returnFlag = optimizedPharmacyBean.deductFromStockOptimized(tbi.getPharmaceuticalBillItem().getStock(), Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());
            } else {
                returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(), Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());
            }
            if (!returnFlag) {
                tbi.setTmpQty(0);
                pharmaceuticalBillItemFacade.edit(tbi.getPharmaceuticalBillItem());
                billItemFacade.edit(tbi);
            }
            getPreBill().getBillItems().add(tbi);
        }
        userStockController.retiredAllUserStockContainer(sessionController.getLoggedUser());
        calculateAllRates();
        billFacade.edit(getPreBill());
    }

    private boolean useOptimizedStockDeduction() {
        return configOptionApplicationController.getBooleanValueByKey("Enable Optimized Pharmacy Fast Sale Stock Deduction", false);
    }

    public void markInprogress() {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(getPreBill());
        t.setCalled(false);
        t.setCalledAt(null);
        t.setInProgress(true);
        t.setCompleted(false);
        tokenController.save(t);
    }

    public void markToken() {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(getPreBill());
        t.setCalled(true);
        t.setCalledAt(new Date());
        t.setInProgress(false);
        t.setCompleted(false);
        tokenController.save(t);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);
        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    private Patient savePatient() {
        Patient pat = getPatient();
        // Check for null references and empty name
        if (pat == null
                || pat.getPerson() == null
                || pat.getPerson().getName() == null
                || pat.getPerson().getName().trim().isEmpty()) {
            return null;
        }

        pat.setCreater(sessionController.getLoggedUser());
        pat.setCreatedAt(new Date());
        pat.getPerson().setCreater(sessionController.getLoggedUser());
        pat.getPerson().setCreatedAt(new Date());

        if (pat.getPerson().getId() == null) {
            personFacade.create(pat.getPerson());
        }
        if (pat.getId() == null) {
            patientFacade.create(pat);
        }
        return pat;
    }

    public void settlePharmacyToken(TokenType tokenType) {
        currentToken = new Token();
        currentToken.setTokenType(tokenType);
        currentToken.setDepartment(sessionController.getLoggedUser().getDepartment());
        currentToken.setFromDepartment(sessionController.getLoggedUser().getDepartment());
        currentToken.setPatient(getPatient());
        currentToken.setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        currentToken.setFromInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
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
        tokenFacade.create(currentToken);
        currentToken.setTokenNumber(billNumberBean.generateDailyTokenNumber(currentToken.getFromDepartment(), null, null, tokenType));
        currentToken.setTokenDate(new Date());
        currentToken.setTokenAt(new Date());
        currentToken.setCreatedAt(new Date());
        currentToken.setCreatedBy(sessionController.getLoggedUser());
        tokenFacade.edit(currentToken);
    }

    public Token getToken() {
        return currentToken;
    }

    public void setToken(Token token) {
        this.currentToken = token;
    }

    public Department getCounter() {
        return counter;
    }

    public void setCounter(Department counter) {
        this.counter = counter;
    }

    /**
     * Initialize controller state on first access to prevent NPEs. This method
     * is called by the preRenderView event in the XHTML.
     */
    public void initIfNeeded() {
        // Ensure all necessary objects are initialized
        if (getPreBill() == null) {
            // Initialization will be handled by the parent class getter
        }
        if (getBillItem() == null) {
            // Initialization will be handled by the parent class getter  
        }
        if (getPaymentMethodData() == null) {
            // Initialization will be handled by the parent class getter
        }
    }

    /**
     * Payment method handling for sale for cashier - minimal implementation.
     * Note: As per issue #14268, no payment data is preserved in sale for
     * cashier.
     */
    public PaymentMethod getPaymentMethod() {
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.Cash; // Default to cash for display purposes
        }
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public BillBeanController getBillBean() {
        return billBeanController;
    }

    /**
     * Payment method change listener - minimal implementation for sale for
     * cashier. Note: As per issue #14268, no actual payment data is preserved.
     */
    public void listnerForPaymentMethodChange() {
        // Minimal implementation to prevent NPEs
        // No actual payment data processing needed for sale for cashier
        if (getPaymentMethodData() != null && getPreBill() != null) {
            // Set minimal data to prevent NPEs during form rendering
            double netTotal = getPreBill().getNetTotal();
            if (paymentMethod == PaymentMethod.Cash) {
                getPaymentMethodData().getCash().setTotalValue(netTotal);
            }
        }
    }
}
