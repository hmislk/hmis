/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.PatientDepositController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.TokenController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyService;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.Token;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.UserStock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ConfigOptionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.PrescriptionFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.facade.UserStockContainerFacade;
import com.divudi.core.facade.UserStockFacade;
import com.divudi.ejb.OptimizedPharmacyBean;
import com.divudi.service.BillService;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PaymentService;
import com.divudi.service.pharmacy.PaymentProcessingService;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.service.pharmacy.StockSearchService;
import com.divudi.service.pharmacy.TokenService;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

/**
 * @author Buddhika
 */
/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyFastRetailSaleController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyFastRetailSaleController() {
    }

    public String navigateToPharmacyFastRetailSale() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_fast_retail_sale?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Please start the shift first");
                return "";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_fast_retail_sale?faces-redirect=true";
        }
    }

    @Inject
    PaymentSchemeController PaymentSchemeController;
    @Inject
    StockController stockController;
    @Inject
    AmpController ampController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    SessionController sessionController;
    @Inject
    SearchController searchController;
    @Inject
    PatientDepositController patientDepositController;

    @Inject
    TokenController tokenController;
    @Inject
    DrawerController drawerController;
    @EJB
    private PaymentProcessingService paymentProcessingService;
    @EJB
    private StockSearchService stockSearchService;
    @EJB
    private TokenService tokenService;

    ////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    DiscountSchemeValidationService discountSchemeValidationService;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    OptimizedPharmacyBean optimizedPharmacyBean;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    StaffService staffBean;
    @EJB
    PaymentService paymentService;
    @EJB
    private UserStockContainerFacade userStockContainerFacade;
    @EJB
    private UserStockFacade userStockFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    TokenFacade tokenFacade;
    @EJB
    private PharmacyService pharmacyService;
    @EJB
    BillService billService;
    @EJB
    private PharmacyCostingService pharmacyCostingService;
    /////////////////////////
    private PreBill preBill;
    private Bill saleBill;

    // Request-scoped caching for performance optimization
    private PreBill cachedPreBill;
    private Patient cachedPatient;
    private boolean preBillCacheValid = false;
    private boolean patientCacheValid = false;
    Bill printBill;
    Bill bill;
    BillItem billItem;
    private List<BillItem> selectedBillItems;
    //BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Integer intQty;
    Stock stock;

    /**
     * Lightweight stock DTO used for autocompletes.
     */
    private StockDTO selectedStockDto;
    private Long selectedStockId;

    /**
     * AMP (Active Medicinal Product) for new multi-batch approach
     */
    private Amp selectedAmp;
    private List<Stock> availableStocks;
    /**
     * Cached results from the most recent stock autocomplete query. Used by
     * {@link StockDtoConverter} to resolve objects on postback.
     */
    private List<StockDTO> cachedStockDtos;
    /**
     * DTO list for available stocks - used for efficient search and calculations
     */
    private List<StockDTO> availableStockDtos;
    private List<ClinicalFindingValue> allergyListOfPatient;
    private boolean billSettlingStarted;

    private PaymentScheme paymentScheme;

    int activeIndex;

    private Token token;
    private Patient patient;
    private YearMonthDay yearMonthDay;
    private String patientTabId = "tabPt";
    private String strTenderedValue = "";
    boolean billPreview = false;
    boolean fromOpdEncounter = false;
    String opdEncounterComments = "";
    int patientSearchTab = 0;

    Staff toStaff;
    Institution toInstitution;
    String errorMessage = "";

    double cashPaid;
    double netTotal;
    double balance;
    Double editingQty;
    String cashPaidStr;
    String comment;
    ///////////////////
    private UserStockContainer userStockContainer;
    PaymentMethodData paymentMethodData;
    private boolean patientDetailsEditable;
    private Department counter;
    Token currentToken;

    PaymentMethod paymentMethod;

    public Token getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(Token currentToken) {
        this.currentToken = currentToken;
    }

    //    public String navigateToPharmacySaleWithoutStocks() {
//        prepareForPharmacySaleWithoutStock();
//        return "/pharmacy/pharmacy_sale_without_stock?faces-redirect=true";
//    }
    private void prepareForPharmacySaleWithoutStock() {
        clearBill();
        clearBillItem();
        searchController.createPreBillsNotPaid();
        billPreview = false;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCashPaidStr() {
        if (cashPaid == 0.0) {
            cashPaidStr = "";
        } else {
            cashPaidStr = String.format("%1$,.2f", cashPaid);
        }
        return cashPaidStr;
    }

    public void setCashPaidStr(String cashPaidStr) {
        try {
            setCashPaid(Double.valueOf(cashPaidStr));
        } catch (Exception e) {
            setCashPaid(0);
        }
        this.cashPaidStr = cashPaidStr;
    }

    public Double getEditingQty() {
        return editingQty;
    }

    public void setEditingQty(Double editingQty) {
        this.editingQty = editingQty;
    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();

            }
            return getPreBill().getNetTotal() - multiplePaymentMethodTotalValue;
        }
        return getPreBill().getTotal();
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            }

        }
    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null ||
            paymentMethodData == null ||
            paymentMethodData.getPaymentMethodMultiple() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
    }

    public double getOldQty(BillItem bItem) {
        String sql = "Select b.qty From BillItem b where b.retired=false and b.bill=:b and b=:itm";
        HashMap hm = new HashMap();
        hm.put("b", getPreBill());
        hm.put("itm", bItem);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    @Inject
    UserStockController userStockController;

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        if (tmp == null) {
            return;
        }
        onEdit(tmp);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);

        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    //Check when edititng Qty
    //
    public boolean onEdit(BillItem tmp) {
        //Cheking Minus Value && Null
        if (tmp.getQty() <= 0 || tmp.getQty() == null) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return true;
        }

        if (tmp.getQty() > tmp.getPharmaceuticalBillItem().getStock().getStock()) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return true;
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {

            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
            return true;
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

        onEditCalculation(tmp);

        return false;
    }

    private Prescription prescription;
    private boolean enableLabelPrintFromSaleView = false;

    public void enableLabelPrint(Prescription p) {
        enableLabelPrintFromSaleView = true;
        this.prescription = p;
    }

    public void addPrescriptionToBillitem(BillItem billItem) {
        if (prescription == null) {
            prescription = new Prescription();
        }

        if (billItem.getInstructions() != null && !billItem.getInstructions().isEmpty()) {
            if (billItem.getPrescription().getComment() == null || billItem.getPrescription().getComment().isEmpty()) {
                billItem.getPrescription().setComment(billItem.getInstructions());
            } else if (billItem.getInstructions().equalsIgnoreCase(billItem.getPrescription().getComment())) {
                billItem.getPrescription().setComment(billItem.getInstructions());
            }
        }

    }

    private void onEditCalculation(BillItem tmp) {
        if (tmp == null) {
            return;
        }

        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());

        calculateBillItemForEditing(tmp);

        calTotal();

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

        pat.setCreater(getSessionController().getLoggedUser());
        pat.setCreatedAt(new Date());
        pat.getPerson().setCreater(getSessionController().getLoggedUser());
        pat.getPerson().setCreatedAt(new Date());

        if (pat.getPerson().getId() == null) {
            getPersonFacade().create(pat.getPerson());
        }
        if (pat.getId() == null) {
            getPatientFacade().create(pat);
        }
        return pat;
    }

    //    private Patient savePatient() {
//        switch (getPatientTabId()) {
//            case "tabPt":
//                if (!getSearchedPatient().getPerson().getName().trim().equals("")) {
//                    getSearchedPatient().setCreater(getSessionController().getLoggedUser());
//                    getSearchedPatient().setCreatedAt(new Date());
//                    getSearchedPatient().getPerson().setCreater(getSessionController().getLoggedUser());
//                    getSearchedPatient().getPerson().setCreatedAt(new Date());
//                    if (getSearchedPatient().getPerson().getId() == null) {
//                        getPersonFacade().create(getSearchedPatient().getPerson());
//                    }
//                    if (getSearchedPatient().getId() == null) {
//                        getPatientFacade().create(getSearchedPatient());
//                    }
//                    return getSearchedPatient();
//                } else {
//                    return null;
//                }
//            case "tabSearchPt":
//                return getSearchedPatient();
//        }
//        return null;
//    }
    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public Integer getIntQty() {
        if (qty == null) {
            return null;
        }
        return qty.intValue();
    }

    public void setIntQty(Integer intQty) {
        this.intQty = intQty;
        if (intQty == null) {
            setQty(null);
        } else {
            setQty(intQty.doubleValue());
        }
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Stock getStock() {
        // Lazy loading: Load stock entity only when actually needed
        if (stock == null && selectedStockId != null) {
            stock = getStockFacade().find(selectedStockId);
        }
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    // AMP-related getters and setters
    public Amp getSelectedAmp() {
        return selectedAmp;
    }

    public void setSelectedAmp(Amp selectedAmp) {
        this.selectedAmp = selectedAmp;
    }

    public List<Stock> getAvailableStocks() {
        return availableStocks;
    }

    public void setAvailableStocks(List<Stock> availableStocks) {
        this.availableStocks = availableStocks;
    }

    public List<StockDTO> getAvailableStockDtos() {
        return availableStockDtos;
    }

    public void setAvailableStockDtos(List<StockDTO> availableStockDtos) {
        this.availableStockDtos = availableStockDtos;
    }

    private String navigateToPharmacyRetailSaleAfterCashierCheck(Patient pt, PaymentScheme ps) {
        if (pt == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        if (ps == null) {
            JsfUtil.addErrorMessage("No Membership");
            return "";
        }
        if (patient == null) {
            JsfUtil.addErrorMessage("No patient selected");
            patient = new Patient();
            patientDetailsEditable = true;
        }
        resetAll();
        patient = pt;
        paymentScheme = ps;
        setPatient(getPatient());
        setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
    }

    private String navigateToPharmacyRetailSaleAfterCashierCheckForCashier(Patient pt, PaymentScheme ps) {
        if (pt == null) {
            JsfUtil.addErrorMessage("No Patient Selected");
            return "";
        }
        if (ps == null) {
            JsfUtil.addErrorMessage("No Membership");
            return "";
        }
        if (patient == null) {
            JsfUtil.addErrorMessage("No patient selected");
            patient = new Patient();
            patientDetailsEditable = true;
        }
        resetAll();
        patient = pt;
        paymentScheme = ps;
        setPatient(getPatient());
        setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
    }

    public void resetAll() {
        // Performance optimization: Minimize expensive operations and cache invalidations
        setBillSettlingStarted(false);

        // Only retire user stock if there are active containers
        if (userStockContainer != null && userStockContainer.getId() != null) {
            userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        }

        // Clear all data and invalidate caches
        clearBill();
        clearBillItem();
        billPreview = false;

        // Invalidate all caches for performance optimization
        invalidateAllCaches();
    }

    /**
     * Initialization fallback for preRenderView to ensure controller state is
     * ready
     */
    public void initIfNeeded() {
        if (billItem == null) {
            getBillItem(); // Force initialization of BillItem and nested objects
        }
    }

    public List<StockDTO> completeStockDtos(String qry) {
        // Performance optimization: Only search if query is meaningful
        if (qry == null || qry.trim().length() < 3) {
            cachedStockDtos = new ArrayList<>();
            return cachedStockDtos;
        }
        cachedStockDtos = stockSearchService.findStockDtos(qry, sessionController.getLoggedUser().getDepartment());
        return cachedStockDtos;
    }

    /**
     * Autocomplete method for AMP (Active Medicinal Product) selection
     */
    public List<Amp> completeAmp(String qry) {
        if (qry == null || qry.trim().length() < 3) {
            return new ArrayList<>();
        }
        return ampController.completeAmp(qry);
    }

    /**
     * Handler for AMP selection - loads available stocks and focuses quantity
     */
    public void handleAmpSelect(SelectEvent event) {
        Amp selectedAmpObj = (Amp) event.getObject();
        if (selectedAmpObj == null) {
            return;
        }
        this.selectedAmp = selectedAmpObj;
        if (intQty == null || intQty == 0) {
            setIntQty(1);
        }
    }

    /**
     * Load available non-expired stocks for the selected AMP
     * Also loads DTO version for efficient calculations
     */
    public void loadAvailableStocks() {
        if (selectedAmp == null) {
            availableStocks = new ArrayList<>();
            availableStockDtos = new ArrayList<>();
            return;
        }
        
        // Load DTOs first for efficient calculations
        loadAvailableStockDtos();
        
        // Load entities for backward compatibility if needed
        String jpql = "SELECT s FROM Stock s WHERE s.itemBatch.item = :amp "
                + "AND s.stock > 0 "
                + "AND (s.itemBatch.dateOfExpire IS NULL OR s.itemBatch.dateOfExpire > :currentDate) "
                + "AND s.department = :department "
                + "ORDER BY s.itemBatch.dateOfExpire ASC, s.itemBatch.purcahseRate ASC";

        Map<String, Object> params = new HashMap<>();
        params.put("amp", selectedAmp);
        params.put("currentDate", new Date());
        params.put("department", sessionController.getLoggedUser().getDepartment());
        try {
            availableStocks = stockFacade.findByJpql(jpql, params);
        } catch (Exception e) {
            availableStocks = new ArrayList<>();
            JsfUtil.addErrorMessage("Error loading available stocks: " + e.getMessage());
        }
    }

    /**
     * Load available non-expired stocks as DTOs for efficient calculations
     */
    public void loadAvailableStockDtos() {
        if (selectedAmp == null) {
            availableStockDtos = new ArrayList<>();
            return;
        }
        
        String jpql = "SELECT new com.divudi.core.data.dto.StockDTO("
                + "s.id, "
                + "s.id, "  // stockId
                + "s.itemBatch.id, "  // itemBatchId 
                + "s.itemBatch.item.name, "
                + "s.itemBatch.item.code, "
                + "s.itemBatch.retailsaleRate, "
                + "s.stock, "
                + "s.itemBatch.dateOfExpire, "
                + "s.itemBatch.batchNo, "
                + "s.itemBatch.purcahseRate, "
                + "s.itemBatch.wholesaleRate, "
                + "s.itemBatch.item.allowFractions) "
                + "FROM Stock s WHERE s.itemBatch.item = :amp "
                + "AND s.stock > 0 "
                + "AND (s.itemBatch.dateOfExpire IS NULL OR s.itemBatch.dateOfExpire > :currentDate) "
                + "AND s.department = :department "
                + "ORDER BY s.itemBatch.dateOfExpire ASC, s.itemBatch.purcahseRate ASC";

        Map<String, Object> params = new HashMap<>();
        params.put("amp", selectedAmp);
        params.put("currentDate", new Date());
        params.put("department", sessionController.getLoggedUser().getDepartment());
        
        try {
            availableStockDtos = (List<StockDTO>) stockFacade.findLightsByJpql(jpql, params);
        } catch (Exception e) {
            availableStockDtos = new ArrayList<>();
            JsfUtil.addErrorMessage("Error loading available stock DTOs: " + e.getMessage());
        }
    }

    public void handleSelect(SelectEvent event) {
        // Get the selected item directly from the event (before JSF updates the bound property)
        StockDTO selectedDto = (StockDTO) event.getObject();
        if (selectedDto == null || selectedDto.getId() == null) {
            return;
        }

        // Update the bound properties with the selected item
        this.selectedStockDto = selectedDto;
        this.selectedStockId = selectedDto.getId();

        if (getBillItem() == null || getBillItem().getPharmaceuticalBillItem() == null) {
            return;
        }

        // Set stock using lazy loading (will be loaded when getStock() is called)
        getBillItem().getPharmaceuticalBillItem().setStock(getStock());

        // Initialize quantity to 1 if not set
        if (intQty == null || intQty == 0) {
            setIntQty(1);
        }

        // Only perform heavy operations if stock was successfully loaded
        if (stock != null) {
            calculateRates(billItem);
            calculateBillItem();
            pharmacyService.addBillItemInstructions(billItem);
        }
    }

    //    public void calculateRates(BillItem bi) {
//        ////////System.out.println("calculating rates");
//        if (bi.getPharmaceuticalBillItem().getStock() == null) {
//            ////////System.out.println("stock is null");
//            return;
//        }
//        getBillItem();
//        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
//        if (pharmBillItem != null) {
//            Stock stock = pharmBillItem.getStock();
//            if (stock != null) {
//                ItemBatch itemBatch = stock.getItemBatch();
//                if (itemBatch != null) {
//                    // Ensure that each step in the chain is not null before accessing further.
//                    bi.setRate(itemBatch.getRetailsaleRate());
//                } else {
//                }
//            } else {
//            }
//        } else {
//        }
//
////        bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//        bi.setDiscount(calculateBillItemDiscountRate(bi));
//        //  ////System.err.println("Discount "+bi.getDiscount());
//        bi.setNetRate(bi.getRate() - bi.getDiscount());
//        //  ////System.err.println("Net "+bi.getNetRate());
//    }
    public void calculateBillItemListner(AjaxBehaviorEvent event) {
        calculateBillItem();
    }

    public void calculateBillItem() {
        if (stock == null) {
            return;
        }
        if (getPreBill() == null) {
            return;
        }
        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }
        if (getQty() > getStock().getStock()) {
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        //Bill Item
//        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);

        //Rates
        //Values
        billItem.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
        billItem.setNetValue(qty * billItem.getNetRate());
        billItem.setDiscount(billItem.getGrossValue() - billItem.getNetValue());

    }

    public void addBillItem() {
        if (configOptionApplicationController.getBooleanValueByKey("Add quantity from multiple batches in pharmacy retail billing")) {
            addBillItemMultipleBatches();
        } else {
            addBillItemSingleItem();
        }
        processBillItems();
        setActiveIndex(1);
    }

    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
//            calculateBillItemForEditing(tbi);
        }
        calculateTotals();
    }

    public void calculateRates(BillItem bi) {
        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
        if (pharmBillItem != null && pharmBillItem.getStock() != null) {
            ItemBatch itemBatch = pharmBillItem.getStock().getItemBatch();
            if (itemBatch != null) {
                bi.setRate(itemBatch.getRetailsaleRate());
            }
            bi.setDiscountRate(calculateBillItemDiscountRate(bi));
            bi.setNetRate(bi.getRate() - bi.getDiscountRate());

            bi.setGrossValue(bi.getRate() * bi.getQty());
            bi.setDiscount(bi.getDiscountRate() * bi.getQty());
            bi.setNetValue(bi.getGrossValue() - bi.getDiscount());

        }
    }

    public void calculateTotals() {
        getPreBill().setTotal(0);
        double netTotal = 0.0, grossTotal = 0.0, discountTotal = 0.0;
        int index = 0;

        for (BillItem b : getPreBill().getBillItems()) {
            if (!b.isRetired()) {
                b.setSearialNo(index++);
                netTotal += b.getNetValue();
                grossTotal += b.getGrossValue();
                discountTotal += b.getDiscount();
//                getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
            }
        }

        getPreBill().setNetTotal(netTotal);
        getPreBill().setTotal(grossTotal);
        getPreBill().setGrantTotal(grossTotal);
        getPreBill().setDiscount(discountTotal);
        setNetTotal(getPreBill().getNetTotal());
    }

    public double addBillItemSingleItem() {
        editingQty = null;
        errorMessage = null;
        double addedQty = 0.0;
        if (billItem == null) {
            return addedQty;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return addedQty;
        }
        if (getStock() == null) {
            errorMessage = "Item ??";
            JsfUtil.addErrorMessage("Please select an Item Batch to Dispense ??");
            return addedQty;
        }
        if (getStock().getItemBatch().getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("Please not select Expired Items");
            return addedQty;
        }
        if (getQty() == null) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Quantity?");
            return addedQty;
        }
        if (getQty() == 0.0) {
            errorMessage = "Quantity Zero?";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return addedQty;
        }
        if (getQty() > getStock().getStock()) {
            errorMessage = "No sufficient stocks.";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return addedQty;
        }

        if (checkItemBatch()) {
            errorMessage = "This batch is already there in the bill.";
            JsfUtil.addErrorMessage("Already added this item batch");
            return addedQty;
        }
//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            JsfUtil.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return addedQty;
        }
        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            if (patient != null && getBillItem() != null) {
                // Performance optimization: Lazy load allergy list only when patient is valid
                if (allergyListOfPatient == null && patient.getId() != null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
                }
                String allergyMsg = "";
                if (allergyListOfPatient != null) {
                    allergyMsg = pharmacyService.getAllergyMessageForPatient(patient, billItem, allergyListOfPatient);
                }

                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return addedQty;
                }
            }
        }

        addedQty = qty;
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();
        ////System.out.println("Rate*****" + billItem.getRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());

        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        if (getUserStockContainer().getId() == null) {
            saveUserStockContainer();
        }

        UserStock us = saveUserStock(billItem);
        billItem.setTransUserStock(us);

        pharmacyService.addBillItemInstructions(billItem);

        clearBillItem();
        getBillItem();
        return addedQty;
    }

    private void addSingleStock() {
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);
        if (getUserStockContainer().getId() == null) {
            saveUserStockContainer();
        }
        UserStock us = saveUserStock(billItem);
        billItem.setTransUserStock(us);
    }

    private void addMultipleStock() {
        Double remainingQty = Math.abs(qty) - Math.abs(getStock().getStock());
        addSingleStock();
        List<Stock> availableStocks = stockController.findNextAvailableStocks(getStock());

    }

    private void saveUserStockContainer() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        getUserStockContainer().setCreater(getSessionController().getLoggedUser());
        getUserStockContainer().setCreatedAt(new Date());

        getUserStockContainerFacade().create(getUserStockContainer());

    }

    private UserStock saveUserStock(BillItem tbi) {
        UserStock us = new UserStock();
        us.setStock(tbi.getPharmaceuticalBillItem().getStock());
        us.setUpdationQty(tbi.getQty());
        us.setCreater(getSessionController().getLoggedUser());
        us.setCreatedAt(new Date());
        us.setUserStockContainer(getUserStockContainer());
        getUserStockFacade().create(us);

        getUserStockContainer().getUserStocks().add(us);

        return us;
    }

    //    public void calculateAllRatesNew() {
//        ////////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateRates(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calTotal();
//    }
    //    Checked
    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(pbi);
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    //    private boolean errorCheckForPreBill() {
//        if (getPreBill().getBillItems().isEmpty()) {
//            JsfUtil.addErrorMessage("No Items added to bill to sale");
//            return true;
//        }
//        return false;
//    }
//    private boolean checkPaymentScheme(PaymentScheme paymentScheme) {
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Cheque) {
//            if (getSaleBill().getBank() == null || getSaleBill().getChequeRefNo() == null || getSaleBill().getChequeDate() == null) {
//                JsfUtil.addErrorMessage("Please select Cheque Number,Bank and Cheque Date");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Slip) {
//            if (getSaleBill().getBank() == null || getSaleBill().getComments() == null || getSaleBill().getChequeDate() == null) {
//                JsfUtil.addErrorMessage("Please Fill Memo,Bank and Slip Date ");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Card) {
//            if (getSaleBill().getBank() == null || getSaleBill().getCreditCardRefNo() == null) {
//                JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank");
//                return true;
//            }
//
////            if (getCreditCardRefNo().trim().length() < 16) {
////                JsfUtil.addErrorMessage("Enter 16 Digit");
////                return true;
////            }
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Credit) {
//            if (getSaleBill().getCreditCompany() == null) {
//                JsfUtil.addErrorMessage("Please Select Credit Company");
//                return true;
//            }
//
//        }
//
//        if (paymentScheme != null && paymentScheme.getPaymentMethod() != null && paymentScheme.getPaymentMethod() == PaymentMethod.Cash) {
//            if (getPreBill().getCashPaid() == 0.0) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//            if (getPreBill().getCashPaid() < getPreBill().getNetTotal()) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//        }
//
//        return false;
//
//    }
    @Override
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    private boolean errorCheckForSaleBill() {

        if (getPaymentSchemeController().checkPaymentMethodError(getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need to Enter the Cash Tendered Amount to Settle Pharmacy Retail Bill", true)) {
            if (paymentMethod == PaymentMethod.Cash) {
                if (cashPaid == 0.0) {
                    JsfUtil.addErrorMessage("Please enter the paid amount");
                    return true;
                }
                if (cashPaid < getPreBill().getNetTotal()) {
                    JsfUtil.addErrorMessage("Please select tendered amount correctly");
                    return true;
                }
            }
        }

        return false;
    }

    private void savePreBillFinallyForRetailSale(Patient pt) {
        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }
        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
//        getPreBill().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(pt, getSessionController().getApplicationPreference().isMembershipExpires()));
        getPreBill().setToStaff(toStaff);
        getPreBill().setToInstitution(toInstitution);

        getPreBill().setComments(comment);

        getPreBill().setCashPaid(cashPaid);
        getPreBill().setBalance(balance);

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        String insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setInsId(insId);
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setDeptId(deptId);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

    }

    private void savePreBillFinallyForRetailSaleForCashier(Patient pt) {
        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }
        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
//        getPreBill().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(pt, getSessionController().getApplicationPreference().isMembershipExpires()));
        getPreBill().setToStaff(toStaff);
        getPreBill().setToInstitution(toInstitution);

        getPreBill().setComments(comment);

        getPreBill().setCashPaid(cashPaid);
        getPreBill().setBalance(balance);

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        String insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setInsId(insId);
        String deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        getPreBill().setDeptId(deptId);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

    }

    @Inject
    private BillBeanController billBean;

    private void saveSaleBill() {
        //  calculateAllRates();

        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());

        getSaleBill().setBillType(BillType.PharmacySale);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getInstitution());
        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());
        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());
        getSaleBill().setInvoiceNumber(getPreBill().getInvoiceNumber());
        getSaleBill().setComments(comment);

        getSaleBill().setCashPaid(cashPaid);
        getSaleBill().setBalance(balance);

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), getPaymentMethodData());

        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        }

        updatePreBill();
    }
//

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());
    }

    @EJB
    PrescriptionFacade prescriptionFacade;

    private void savePreBillItemsFinally(List<BillItem> list) {
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {
//If any issue in Stock Bill Item will not save & not include for total
//                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());

            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            if (tbi.getPrescription() != null) {
                if (tbi.getPrescription().getId() == null) {
                    prescriptionFacade.create(tbi.getPrescription());
                } else {
                    prescriptionFacade.edit(tbi.getPrescription());
                }
            }

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);

            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();

            //Deduct Stock
            boolean returnFlag;
            if (useOptimizedStockDeduction()) {
                returnFlag = optimizedPharmacyBean.deductFromStockOptimized(
                        tbi.getPharmaceuticalBillItem().getStock(),
                        Math.abs(qtyL),
                        tbi.getPharmaceuticalBillItem(),
                        getPreBill().getDepartment());
            } else {
                returnFlag = getPharmacyBean().deductFromStock(
                        tbi.getPharmaceuticalBillItem().getStock(),
                        Math.abs(qtyL),
                        tbi.getPharmaceuticalBillItem(),
                        getPreBill().getDepartment());
            }

            if (!returnFlag) {
                tbi.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
                getBillItemFacade().edit(tbi);
            }

            getPreBill().getBillItems().add(tbi);
        }

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    private void saveSaleBillItems(List<BillItem> list) {
        for (BillItem tbi : list) {

            BillItem newBil = new BillItem();

            newBil.copy(tbi);
            newBil.setReferanceBillItem(tbi);
            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            if (tbi.getPrescription() != null) {
                if (tbi.getPrescription().getId() == null) {
                    prescriptionFacade.create(tbi.getPrescription());
                } else {
                    prescriptionFacade.edit(tbi.getPrescription());
                }

                newBil.setPrescription(tbi.getPrescription());
                tbi.getPrescription().setPatient(patient);
                tbi.getPrescription().setCreatedAt(new Date());
                tbi.getPrescription().setCreater(sessionController.getWebUser());
                tbi.getPrescription().setInstitution(sessionController.getInstitution());
                tbi.getPrescription().setDepartment(sessionController.getDepartment());
                prescriptionFacade.edit(tbi.getPrescription());
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            getSaleBill().getBillItems().add(newBil);

            tbi.setReferanceBillItem(newBil);
            getBillItemFacade().edit(tbi);
        }

        getBillFacade().edit(getSaleBill());
    }

    private void saveSaleBillItems(List<BillItem> list, Payment p) {
        for (BillItem tbi : list) {

            BillItem newBil = new BillItem();

            newBil.copy(tbi);
            newBil.setReferanceBillItem(tbi);
            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            getSaleBill().getBillItems().add(newBil);

            tbi.setReferanceBillItem(newBil);
            getBillItemFacade().edit(tbi);
        }

        getBillFacade().edit(getSaleBill());
    }

    private boolean checkAllBillItem() {
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

    @EJB
    private ConfigOptionFacade configOptionFacade;

    @EJB
    private CashTransactionBean cashTransactionBean;

    public void settleBillWithPay() {
        editingQty = null;

        if (billSettlingStarted) {
            return;
        }

        billSettlingStarted = true;
        if (sessionController.getApplicationPreference().isCheckPaymentSchemeValidation()) {
            if (getPaymentScheme() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select Payment Scheme");
                return;
            }
        }

        if (getPaymentMethod() == null) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        if (getPreBill().getBillItems().isEmpty()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }
        if ((getPatient().getMobileNumberStringTransient() == null
                || getPatient().getMobileNumberStringTransient().trim().isEmpty() || getPatient().getPerson().getName().trim().isEmpty())
                && configOptionApplicationController.getBooleanValueByKey("Patient details are required for retail sale")) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please enter patient name and mobile number.");
            return;
        }
        if (getPaymentMethod() == PaymentMethod.Card) {
            String cardNumber = getPaymentMethodData().getCreditCard().getNo();
            if ((cardNumber == null || cardNumber.trim().isEmpty()
                    || cardNumber.trim().length() != 4)
                    && configOptionApplicationController.getBooleanValueByKey("Pharmacy retail sale CreditCard last digits is Mandatory")) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter a Credit Card last 4 digits");
                return;
            }
        }

        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme, getPaymentMethodData());
        if (!discountSchemeValidation.isFlag()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return;
        }

        boolean patientRequiredForPharmacySale = configOptionApplicationController.getBooleanValueByKey(
                "Patient is required in Pharmacy Retail Sale Bill for " + sessionController.getDepartment().getName(),
                false
        );

        if (patientRequiredForPharmacySale) {
            if (getPatient() == null
                    || getPatient().getPerson() == null
                    || getPatient().getPerson().getName() == null
                    || getPatient().getPerson().getName().trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please Select a Patient");
                billSettlingStarted = false;
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            // Performance optimization: Only check allergies if patient is valid and has bill items
            if (patient != null && patient.getId() != null && !getPreBill().getBillItems().isEmpty()) {
                if (allergyListOfPatient == null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
                }
                String allergyMsg = null;
                if (allergyListOfPatient != null) {
                    allergyMsg = pharmacyService.isAllergyForPatient(patient, getPreBill().getBillItems(), allergyListOfPatient);
                }
                if (allergyMsg != null && !allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    billSettlingStarted = false;
                    return;
                }
            }
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                    setZeroToQty(bi);
//                    onEditCalculation(bi);
                    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                    billSettlingStarted = false;
                    return;
                }
                ////System.out.println("bi.getItem().getName() = " + bi.getItem().getName());
                ////System.out.println("bi.getQty() = " + bi.getQty());
                if (bi.getQty() <= 0.0) {
                    ////System.out.println("bi.getQty() = " + bi.getQty());
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    billSettlingStarted = false;
                    return;
                }
            }
        }

        Patient pt = savePatient();

        if (errorCheckForSaleBill()) {
            billSettlingStarted = false;
            return;
        }

        calculateAllRates();

        getPreBill().setPaidAmount(getPreBill().getTotal());

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinallyForRetailSale(pt);
        savePreBillItemsFinally(tmpBillItems);

        saveSaleBill();
        List<Payment> payments = createPaymentsForBill(getSaleBill());
        drawerController.updateDrawerForIns(payments);
        saveSaleBillItems(tmpBillItems);

        getBillFacade().edit(getPreBill());

        setPrintBill(getBillFacade().find(getSaleBill().getId()));

        // Update BillFinanceDetails for retail sale to ensure proper report data
        if (getSaleBill() != null) {
            pharmacyCostingService.updateBillFinanceDetailsForRetailSale(getSaleBill());
            getBillFacade().edit(getSaleBill());

            // Calculate and record comprehensive financial details for retail sale
            System.out.println("=== Calculating comprehensive financial details for retail sale ===");
            calculateAndRecordCostingValues(getSaleBill());
        }

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
            getStaffBean().updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("User Credit Updated");
        } else if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            double runningBalance;
            if (pt != null) {
                if (pt.getRunningBalance() != null) {
                    runningBalance = pt.getRunningBalance();
                } else {
                    runningBalance = 0.0;
                }
                runningBalance += netTotal;
                pt.setRunningBalance(runningBalance);
            }

        }

//        if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff_Welfare) {
//            staffBean.updateStaffWelfare(toStaff, netTotal);
//            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
//        } else if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff) {
//            staffBean.updateStaffCredit(toStaff, netTotal);
//            JsfUtil.addSuccessMessage("Staff Credit Updated");
//        }
//
//        if (paymentMethod == PaymentMethod.PatientDeposit) {
//            if (getPatient().getRunningBalance() != null) {
//                getPatient().setRunningBalance(getPatient().getRunningBalance() - netTotal);
//            } else {
//                getPatient().setRunningBalance(0.0 - netTotal);
//            }
//            getPatientFacade().edit(getPatient());
//            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());
//            patientDepositController.updateBalance(getSaleBill(), pd);
//        }
        paymentService.updateBalances(payments);

        resetAll();
        billSettlingStarted = false;
        billPreview = true;

    }

    //    checked
    private boolean checkItemBatch() {
        for (BillItem bItem : getPreBill().getBillItems()) {
            if (bItem.getPharmaceuticalBillItem().getStock().equals(getBillItem().getPharmaceuticalBillItem().getStock())) {
                return true;
            }
        }
        return false;
    }

    public void calTotal() {
        getPreBill().setTotal(0);
        double netTot = 0.0;
        double discount = 0.0;
        double grossTot = 0.0;
        int index = 0;
        for (BillItem b : getPreBill().getBillItems()) {
            if (b.isRetired()) {
                continue;
            }
            b.setSearialNo(index++);

            netTot = netTot + b.getNetValue();
            grossTot = grossTot + b.getGrossValue();
            discount = discount + b.getDiscount();
            getPreBill().setTotal(getPreBill().getTotal() + b.getGrossValue());
        }
        ////System.out.println("1.discount = " + discount);
        netTot = netTot + getPreBill().getServiceCharge();

        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        setNetTotal(getPreBill().getNetTotal());

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void removeBillItem(BillItem b) {
        userStockController.removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
        getPreBill().getBillItems().remove(b.getSearialNo());

        calTotal();
    }

    /**
     * Create the Payment entities associated with the given bill using the
     * configured {@link PaymentProcessingService}.
     *
     * @param b the bill to create payments for
     * @return list of persisted payments
     */
    public List<Payment> createPaymentsForBill(Bill b) {
        return paymentProcessingService.createPaymentsForBill(
                b,
                b.getPaymentMethod(),
                paymentMethodData,
                sessionController.getInstitution(),
                sessionController.getDepartment(),
                sessionController.getLoggedUser()
        );
    }

    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    MembershipSchemeController membershipSchemeController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    //    TO check the functionality
    public double calculateBillItemDiscountRate(BillItem bi) {
        if (bi == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return 0.0;
        }
        bi.setItem(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getItem());
        double retailRate = bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate();
        double discountRate = 0;
        boolean discountAllowed = bi.getItem().isDiscountAllowed();
//        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        //MEMBERSHIPSCHEME DISCOUNT
//        if (membershipScheme != null && discountAllowed) {
//            PaymentMethod tpm = getPaymentMethod();
//            if (tpm == null) {
//                tpm = PaymentMethod.Cash;
//            }
//            PriceMatrix priceMatrix = getPriceMatrixController().getPharmacyMemberDisCount(tpm, membershipScheme, getSessionController().getDepartment(), bi.getItem().getCategory());
//            if (priceMatrix == null) {
//                return 0;
//            } else {
//                bi.setPriceMatrix(priceMatrix);
//                return (retailRate * priceMatrix.getDiscountPercent()) / 100;
//            }
//        }
//
        //PAYMENTSCHEME DISCOUNT
        if (getPaymentScheme() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //PAYMENTMETHOD DISCOUNT
        if (getPaymentMethod() != null && discountAllowed) {
            PriceMatrix priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());

            if (priceMatrix != null) {
                bi.setPriceMatrix(priceMatrix);
                discountRate = priceMatrix.getDiscountPercent();
            }

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //CREDIT COMPANY DISCOUNT
        if (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null) {
            discountRate = toInstitution.getPharmacyDiscount();

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;
        }
        return 0;

    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }

    }

    private void clearBill() {
        preBill = null;
        saleBill = null;
        patient = null;
        // Invalidate caches when clearing bill data
        preBillCacheValid = false;
        cachedPreBill = null;
        patientCacheValid = false;
        cachedPatient = null;
        toInstitution = null;
        toStaff = null;
//        billItems = null;
        patientTabId = "tabPt";
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        paymentScheme = null;
        paymentMethod = PaymentMethod.Cash;
        userStockContainer = null;
        fromOpdEncounter = false;
        opdEncounterComments = null;
        patientSearchTab = 0;
        cashPaid = 0.0;
        errorMessage = "";
        comment = null;
        token = null;
        currentToken = null;

    }

    private void clearBillItem() {
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        editingQty = null;
        errorMessage = "";
        paymentMethodData = null;
        selectedStockDto = null;
        selectedAmp = null;
        availableStocks = null;
        allergyListOfPatient = null;
    }

    /**
     * Performance optimization: Invalidate all request-scoped caches This
     * method should be called after major state changes
     */
    private void invalidateAllCaches() {
        preBillCacheValid = false;
        cachedPreBill = null;
        patientCacheValid = false;
        cachedPatient = null;
    }

    public boolean CheckDateAfterOneMonthCurrentDateTime(Date date) {
        Calendar calDateOfExpiry = Calendar.getInstance();
        calDateOfExpiry.setTime(CommonFunctions.getEndOfDay(date));
        Calendar cal = Calendar.getInstance();
        cal.setTime(CommonFunctions.getEndOfDay(new Date()));
        cal.add(Calendar.DATE, 31);
        if (cal.getTimeInMillis() <= calDateOfExpiry.getTimeInMillis()) {
            return false;
        } else {
            return true;
        }
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillItem getEditingBillItem() {
        return editingBillItem;
    }

    public void setEditingBillItem(BillItem editingBillItem) {
        this.editingBillItem = editingBillItem;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public OptimizedPharmacyBean getOptimizedPharmacyBean() {
        return optimizedPharmacyBean;
    }

    public void setOptimizedPharmacyBean(OptimizedPharmacyBean optimizedPharmacyBean) {
        this.optimizedPharmacyBean = optimizedPharmacyBean;
    }

    private boolean useOptimizedStockDeduction() {
        return configOptionApplicationController.getBooleanValueByKey(
                "Enable Optimized Pharmacy Fast Sale Stock Deduction",
                false
        );
    }

    @Override
    public Patient getPatient() {
        // Performance optimization: Use request-scoped caching to prevent excessive getter calls
        if (patientCacheValid && cachedPatient != null) {
            return cachedPatient;
        }

        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable = true;
        }

        // Cache the result for this request
        cachedPatient = patient;
        patientCacheValid = true;
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
        // Invalidate cache when patient is set
        patientCacheValid = false;
        cachedPatient = null;
        selectPaymentSchemeAsPerPatientMembership();
    }

    private void selectPaymentSchemeAsPerPatientMembership() {
        if (patient == null) {
            return;
        }
        if (patient.getPerson().getMembershipScheme() == null) {
            paymentScheme = null;
        } else {
            setPaymentScheme(patient.getPerson().getMembershipScheme().getPaymentScheme());
        }
        listnerForPaymentMethodChange();
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public PreBill getPreBill() {
        // Performance optimization: Use request-scoped caching to prevent excessive getter calls
        if (preBillCacheValid && cachedPreBill != null) {
            return cachedPreBill;
        }

        if (preBill == null) {
            preBill = new PreBill();
            preBill.setBillType(BillType.PharmacyPre);
            preBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
            //   preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }

        // Cache the result for this request
        cachedPreBill = preBill;
        preBillCacheValid = true;
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
        // Invalidate cache when preBill is set
        preBillCacheValid = false;
        cachedPreBill = null;
    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
            //   saleBill.setBillType(BillType.PharmacySale);
        }
        return saleBill;
    }

    public void setSaleBill(Bill saleBill) {
        this.saleBill = saleBill;
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public void setStrTenderedValue(String strTenderedValue) {
        this.strTenderedValue = strTenderedValue;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        balance = cashPaid - netTotal;
        this.cashPaid = cashPaid;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        balance = cashPaid - netTotal;
        this.netTotal = netTotal;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isBillPreview() {
        return billPreview;
    }

    public void setBillPreview(boolean billPreview) {
        this.billPreview = billPreview;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Bill getPrintBill() {
        return printBill;
    }

    public void setPrintBill(Bill printBill) {
        this.printBill = printBill;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return PaymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController PaymentSchemeController) {
        this.PaymentSchemeController = PaymentSchemeController;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public UserStockContainer getUserStockContainer() {
        if (userStockContainer == null) {
            userStockContainer = new UserStockContainer();
        }
        return userStockContainer;
    }

    public void setUserStockContainer(UserStockContainer userStockContainer) {
        this.userStockContainer = userStockContainer;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public StaffService getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffService staffBean) {
        this.staffBean = staffBean;
    }

    public boolean isFromOpdEncounter() {
        return fromOpdEncounter;
    }

    public void setFromOpdEncounter(boolean fromOpdEncounter) {
        this.fromOpdEncounter = fromOpdEncounter;
    }

    public String getOpdEncounterComments() {
        return opdEncounterComments;
    }

    public void setOpdEncounterComments(String opdEncounterComments) {
        this.opdEncounterComments = opdEncounterComments;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UserStockContainerFacade getUserStockContainerFacade() {
        return userStockContainerFacade;
    }

    public void setUserStockContainerFacade(UserStockContainerFacade userStockContainerFacade) {
        this.userStockContainerFacade = userStockContainerFacade;
    }

    public UserStockFacade getUserStockFacade() {
        return userStockFacade;
    }

    public void setUserStockFacade(UserStockFacade userStockFacade) {
        this.userStockFacade = userStockFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public Department getCounter() {
        return counter;
    }

    public void setCounter(Department counter) {
        this.counter = counter;
    }

    @Override
    public void listnerForPaymentMethodChange() {
        if (paymentMethod == PaymentMethod.Cash) {
            getPaymentMethodData().getCash().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.Credit) {
            getPaymentMethodData().getCredit().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.Cheque) {
            getPaymentMethodData().getCheque().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.Slip) {
            getPaymentMethodData().getSlip().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.ewallet) {
            getPaymentMethodData().getEwallet().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.Staff) {
            getPaymentMethodData().getStaffCredit().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.OnlineSettlement) {
            getPaymentMethodData().getOnlineSettlement().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
        // Recalculate all bill item discounts when payment method or scheme changes
        processBillItems();
    }

    public Prescription getPrescription() {
        if (prescription == null) {
            prescription = new Prescription();
        }
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public boolean isEnableLabelPrintFromSaleView() {
        return enableLabelPrintFromSaleView;
    }

    public void setEnableLabelPrintFromSaleView(boolean enableLabelPrintFromSaleView) {
        this.enableLabelPrintFromSaleView = enableLabelPrintFromSaleView;
    }

    public boolean isBillSettlingStarted() {
        return billSettlingStarted;
    }

    public void setBillSettlingStarted(boolean billSettlingStarted) {
        this.billSettlingStarted = billSettlingStarted;
    }

    public StockDTO getSelectedStockDto() {
        return selectedStockDto;
    }

    public void setSelectedStockDto(StockDTO stockDto) {
        this.selectedStockDto = stockDto;
        // Set the ID for lazy loading but don't load the full entity yet
        if (stockDto != null && stockDto.getId() != null) {
            this.selectedStockId = stockDto.getId();
            // Clear any existing stock to force lazy loading when needed
            this.stock = null;
        } else {
            this.selectedStockId = null;
            this.stock = null;
        }
    }

    public List<StockDTO> getCachedStockDtos() {
        return cachedStockDtos;
    }

    public Long getSelectedStockId() {
        return selectedStockId;
    }

    public void setSelectedStockId(Long selectedStockId) {
        this.selectedStockId = selectedStockId;
        // When the ID is set, load the corresponding stock
        if (selectedStockId != null) {
            stock = getStockFacade().find(selectedStockId);
            // Also set selectedStockDto for compatibility
            if (stock != null && stock.getItemBatch() != null && stock.getItemBatch().getItem() != null) {
                selectedStockDto = new StockDTO(stock.getId(),
                        stock.getItemBatch().getItem().getName(),
                        stock.getItemBatch().getItem().getCode(),
                        stock.getItemBatch().getItem().getVmp() != null ? stock.getItemBatch().getItem().getVmp().getName() : "",
                        stock.getItemBatch().getRetailsaleRate(),
                        stock.getStock(),
                        stock.getItemBatch().getDateOfExpire());
            }
        }
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public void processBillItems() {
        calculateAllRates();
        calculateTotals();
    }

    public void calculateBillItemForEditing(BillItem bi) {
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }

        Stock stock = bi.getPharmaceuticalBillItem().getStock();

        if (stock.getItemBatch() == null) {
            return;
        }

        bi.setRate(stock.getItemBatch().getRetailsaleRate());
        bi.setGrossValue(bi.getQty() * bi.getRate());

        // Calculate discounts and other values as needed
        calculateTotals();
    }

    public void addBillItemMultipleBatches() {
        editingQty = null;
        errorMessage = null;

        if (selectedAmp == null) {
            JsfUtil.addErrorMessage("Please select a medicine first");
            return;
        }

        if (getQty() == null || getQty() <= 0) {
            JsfUtil.addErrorMessage("Please enter a valid quantity");
            return;
        }
        // Check if the same AMP is already in the bill
        double existingTotalQty = 0.0;
        for (BillItem existingItem : getPreBill().getBillItems()) {
            if (existingItem.getItem() != null && existingItem.getItem().getId().equals(selectedAmp.getId())) {
                existingTotalQty += existingItem.getQty();
            }
        }

        if (existingTotalQty > 0) {
            JsfUtil.addErrorMessage("'" + selectedAmp.getName() + "' is already in the bill with quantity " + existingTotalQty + ". Please edit the existing item quantity instead of adding duplicate items.");
            return;
        }

        // Load available stocks as DTOs for efficient calculations
        loadAvailableStockDtos();

        if (availableStockDtos == null || availableStockDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No stock available for " + selectedAmp.getName());
            return;
        }

        // Check if requested quantity exceeds available stock using DTOs
        double totalAvailableStock = 0.0;
        for (StockDTO stockDto : availableStockDtos) {
            totalAvailableStock += stockDto.getStockQty();
        }

        if (getQty() > totalAvailableStock) {
            JsfUtil.addErrorMessage("Requested quantity (" + getQty() + ") exceeds available stock (" + totalAvailableStock + ") for " + selectedAmp.getName());
            return;
        }

        // Add items from available batches to fulfill the requested quantity using DTOs
        double remainingQty = getQty();

        for (StockDTO stockDto : availableStockDtos) {
            if (remainingQty <= 0) {
                break;
            }

            double stockAvailable = stockDto.getStockQty();
            double qtyToAdd = Math.min(remainingQty, stockAvailable);

            if (qtyToAdd > 0) {
                // Load full Stock entity only when needed for business operations
                Stock availableStock = stockFacade.find(stockDto.getStockId());
                if (availableStock == null) {
                    JsfUtil.addErrorMessage("Stock not found for ID: " + stockDto.getStockId());
                    continue;
                }
                
                // Create a new bill item for this batch
                BillItem newBillItem = new BillItem();
                newBillItem.setBill(getPreBill());
                newBillItem.setItem(selectedAmp);
                newBillItem.setQty(qtyToAdd);
                newBillItem.setInwardChargeType(InwardChargeType.Medicine);
                newBillItem.setSearialNo(getPreBill().getBillItems().size() + 1);

                // Create pharmaceutical bill item
                PharmaceuticalBillItem pharmBillItem = new PharmaceuticalBillItem();
                pharmBillItem.setBillItem(newBillItem);
                pharmBillItem.setStock(availableStock);
                pharmBillItem.setItemBatch(availableStock.getItemBatch());
                pharmBillItem.setQtyInUnit(0 - qtyToAdd);
                newBillItem.setPharmaceuticalBillItem(pharmBillItem);

                // Calculate rates
                calculateRates(newBillItem);

                // Add to bill
                getPreBill().getBillItems().add(newBillItem);

                remainingQty -= qtyToAdd;
            }
        }

        if (remainingQty > 0) {
            JsfUtil.addErrorMessage("Only " + (getQty() - remainingQty) + " items could be added due to insufficient stock");
        } else {
            JsfUtil.addSuccessMessage("Added " + getQty() + " of " + selectedAmp.getName());
        }

        // Clear selection for next item
        selectedAmp = null;
        setIntQty(1);

        // Update totals
        processBillItems();
        setActiveIndex(1);
    }

    /**
     * Calculates and records comprehensive financial details for retail sale bills.
     * This method ensures BillItemFinanceDetails are properly populated for pharmacy retail sales,
     * maintaining consistency with return processing and providing complete financial data for reporting.
     *
     * Key financial details calculated:
     * - Stock valuations (negative - stock going out)
     * - Costs (positive - following net total direction)
     * - Rates and quantities
     * - PharmaceuticalBillItem values
     *
     * This ensures pharmacy income reports have complete financial data at both bill and line-item levels.
     *
     * @param bill The retail sale bill to calculate financial details for
     */
    private void calculateAndRecordCostingValues(Bill bill) {
        System.out.println("=== calculateAndRecordCostingValues START (Retail Sale) ===");
        System.out.println("Bill ID: " + (bill != null ? bill.getId() : "null"));
        System.out.println("Bill Type: " + (bill != null ? bill.getBillTypeAtomic() : "null"));

        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            System.out.println("Early return - no bill or bill items");
            return;
        }

        System.out.println("Number of bill items: " + bill.getBillItems().size());

        // Initialize bill finance details if not present (should already exist from service call)
        if (bill.getBillFinanceDetails() == null) {
            BillFinanceDetails billFinanceDetails = new BillFinanceDetails();
            billFinanceDetails.setBill(bill);
            bill.setBillFinanceDetails(billFinanceDetails);
            System.out.println("Created new BillFinanceDetails for bill");
        } else {
            System.out.println("BillFinanceDetails already exists for bill");
        }

        // Initialize bill-level totals
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFreeQuantity = BigDecimal.ZERO;

        // Process each bill item
        int itemIndex = 0;
        for (BillItem billItem : bill.getBillItems()) {
            itemIndex++;
            System.out.println("--- Processing Bill Item " + itemIndex + " ---");
            System.out.println("BillItem ID: " + (billItem != null ? billItem.getId() : "null"));
            System.out.println("BillItem retired: " + (billItem != null ? billItem.isRetired() : "null"));
            System.out.println("BillItem qty: " + (billItem != null ? billItem.getQty() : "null"));

            if (billItem == null || billItem.isRetired()) {
                System.out.println("Skipping retired or null bill item");
                continue;
            }

            // Initialize bill item finance details if not present
            if (billItem.getBillItemFinanceDetails() == null) {
                BillItemFinanceDetails itemFinanceDetails = new BillItemFinanceDetails();
                itemFinanceDetails.setBillItem(billItem);
                billItem.setBillItemFinanceDetails(itemFinanceDetails);
                System.out.println("Created new BillItemFinanceDetails for item");
            } else {
                System.out.println("BillItemFinanceDetails already exists for item");
            }

            // Get pharmaceutical bill item for rate information
            PharmaceuticalBillItem pharmaItem = billItem.getPharmaceuticalBillItem();
            System.out.println("PharmaceuticalBillItem: " + (pharmaItem != null ? "exists" : "null"));
            if (pharmaItem == null) {
                System.out.println("Skipping - no pharmaceutical bill item");
                continue;
            }

            // Get quantities - for sales these will be positive
            BigDecimal qty = BigDecimal.valueOf(billItem.getQty());
            BigDecimal freeQty = BigDecimal.valueOf(pharmaItem.getFreeQty());
            BigDecimal totalQty = qty.add(freeQty);
            System.out.println("Quantities - qty: " + qty + ", freeQty: " + freeQty + ", totalQty: " + totalQty);

            // Get rates from pharmaceutical bill item
            BigDecimal retailRate = BigDecimal.valueOf(pharmaItem.getRetailRate());
            BigDecimal purchaseRate = BigDecimal.valueOf(pharmaItem.getPurchaseRate());
            BigDecimal wholesaleRate = BigDecimal.valueOf(pharmaItem.getWholesaleRate());

            System.out.println("Pharma rates - retail: " + retailRate + ", purchase: " + purchaseRate + ", wholesale: " + wholesaleRate);

            // Get cost rate from item batch (which is the actual cost for sales)
            BigDecimal costRate = purchaseRate; // default fallback
            if (pharmaItem.getItemBatch() != null) {
                double batchPurchaseRate = pharmaItem.getItemBatch().getPurcahseRate();
                if (batchPurchaseRate > 0) {
                    costRate = BigDecimal.valueOf(batchPurchaseRate);
                    System.out.println("Got costRate from itemBatch.purcahseRate: " + costRate);

                    // Also update the pharmaceutical bill item with this cost rate
                    pharmaItem.setCostRate(costRate.doubleValue());
                    pharmaItem.setPurchaseRate(costRate.doubleValue());
                    System.out.println("Updated PharmaceuticalBillItem costRate to: " + costRate);
                } else {
                    System.out.println("ItemBatch purcahseRate is zero, using purchaseRate as fallback");
                }
            } else {
                System.out.println("No itemBatch available, using purchaseRate as costRate fallback");
            }

            // Calculate values based on total quantity (including free quantities)
            BigDecimal itemRetailValue = retailRate.multiply(totalQty);
            BigDecimal itemPurchaseValue = purchaseRate.multiply(totalQty);
            BigDecimal itemCostValue = costRate.multiply(totalQty);
            BigDecimal itemWholesaleValue = wholesaleRate.multiply(totalQty);

            System.out.println("Calculated values - retail: " + itemRetailValue + ", purchase: " + itemPurchaseValue +
                             ", cost: " + itemCostValue + ", wholesale: " + itemWholesaleValue);

            // Set item-level finance details - enhanced with more comprehensive data
            BillItemFinanceDetails bifd = billItem.getBillItemFinanceDetails();
            System.out.println("Setting values in BillItemFinanceDetails...");

            // RATES (no signs - always positive rates)
            bifd.setLineNetRate(BigDecimal.valueOf(Math.abs(billItem.getNetRate())));
            bifd.setLineGrossRate(BigDecimal.valueOf(Math.abs(billItem.getRate())));
            bifd.setGrossRate(BigDecimal.valueOf(Math.abs(billItem.getRate())));
            bifd.setLineCostRate(costRate.abs()); // costRate from itemBatch (no sign)
            bifd.setCostRate(costRate.abs());
            bifd.setPurchaseRate(purchaseRate.abs());
            bifd.setRetailSaleRate(retailRate.abs());

            // BILL-LEVEL RATES (always 0 for now)
            bifd.setBillCostRate(BigDecimal.ZERO);

            // TOTAL RATES (lineCostRate + billCostRate)
            bifd.setTotalCostRate(bifd.getLineCostRate()); // since billCostRate = 0

            // TOTALS
            bifd.setGrossTotal(BigDecimal.valueOf(billItem.getGrossValue()));
            bifd.setLineGrossTotal(bifd.getGrossTotal()); // no bill-level discounts
            bifd.setLineNetTotal(BigDecimal.valueOf(billItem.getNetValue()));

            // COSTS (positive - following net total direction for sales)
            BigDecimal lineCost = costRate.multiply(qty.abs()); // Always positive for sales
            bifd.setLineCost(lineCost);
            bifd.setBillCost(BigDecimal.ZERO);
            bifd.setTotalCost(lineCost); // totalCost = lineCost + billCost

            // QUANTITIES
            bifd.setQuantity(qty);
            bifd.setFreeQuantity(freeQty);
            bifd.setTotalQuantity(totalQty);
            bifd.setQuantityByUnits(qty.abs()); // no packs, same as quantity but positive

            // STOCK VALUATIONS (negative - stock going out for sales)
            BigDecimal absQty = qty.abs(); // absolute quantity for valuation
            bifd.setValueAtCostRate(costRate.multiply(absQty).negate()); // Negative - stock going out
            bifd.setValueAtPurchaseRate(purchaseRate.multiply(absQty).negate()); // Negative - stock going out
            bifd.setValueAtRetailRate(retailRate.multiply(absQty).negate()); // Negative - stock going out
            bifd.setValueAtWholesaleRate(wholesaleRate.multiply(absQty).negate()); // Negative - stock going out

            System.out.println("Set all BillItemFinanceDetails fields successfully");
            System.out.println("Rates - lineNet: " + bifd.getLineNetRate() + ", lineGross: " + bifd.getLineGrossRate() +
                             ", lineCost: " + bifd.getLineCostRate());
            System.out.println("Costs - line: " + lineCost + ", bill: " + bifd.getBillCost() + ", total: " + bifd.getTotalCost());
            System.out.println("Stock Values - cost: " + bifd.getValueAtCostRate() + ", purchase: " + bifd.getValueAtPurchaseRate() +
                             ", retail: " + bifd.getValueAtRetailRate());

            // Set PharmaceuticalBillItem values (negative - stock going out)
            System.out.println("Setting PharmaceuticalBillItem values...");
            BigDecimal absQtyForPBI = qty.abs(); // absolute quantity for PBI valuations
            pharmaItem.setCostValue(costRate.multiply(absQtyForPBI).negate().doubleValue()); // Negative - stock going out
            pharmaItem.setPurchaseValue(purchaseRate.multiply(absQtyForPBI).negate().doubleValue()); // Negative - stock going out
            pharmaItem.setRetailValue(retailRate.multiply(absQtyForPBI).negate().doubleValue()); // Negative - stock going out

            System.out.println("PBI values - cost: " + pharmaItem.getCostValue() +
                             ", purchase: " + pharmaItem.getPurchaseValue() +
                             ", retail: " + pharmaItem.getRetailValue());

            // Save PharmaceuticalBillItem to ensure values are persisted
            if (pharmaItem.getId() == null) {
                System.out.println("PharmaceuticalBillItem is new - will be saved via cascade");
            } else {
                System.out.println("PharmaceuticalBillItem exists, saving explicitly");
                pharmaceuticalBillItemFacade.edit(pharmaItem);
            }

            // Aggregate values for bill level (negative for stock going out)
            totalCostValue = totalCostValue.add(itemCostValue.negate());
            totalPurchaseValue = totalPurchaseValue.add(itemPurchaseValue.negate());
            totalRetailSaleValue = totalRetailSaleValue.add(itemRetailValue.negate());
            totalWholesaleValue = totalWholesaleValue.add(itemWholesaleValue.negate());
            totalQuantity = totalQuantity.add(qty);
            totalFreeQuantity = totalFreeQuantity.add(freeQty);

            System.out.println("Aggregated totals - cost: " + totalCostValue + ", purchase: " + totalPurchaseValue +
                             ", retail: " + totalRetailSaleValue + ", quantity: " + totalQuantity);

            // Save bill item finance details using JPA cascade persistence
            if (billItem.getBillItemFinanceDetails().getId() == null) {
                System.out.println("BillItemFinanceDetails is new (id == null) - will be saved via cascade");
            } else {
                System.out.println("BillItemFinanceDetails exists, calling billItemFacade.edit()");
                billItemFacade.edit(billItem);
            }
        }

        System.out.println("=== Finished processing all bill items ===");

        // Update bill-level finance details with negative stock valuations
        System.out.println("Updating BillFinanceDetails with negative stock valuations...");
        BillFinanceDetails bfd = bill.getBillFinanceDetails();

        // Override the existing positive values with negative ones (stock going out)
        bfd.setTotalCostValue(totalCostValue); // Negative
        bfd.setTotalPurchaseValue(totalPurchaseValue); // Negative
        bfd.setTotalRetailSaleValue(totalRetailSaleValue); // Negative
        bfd.setTotalWholesaleValue(totalWholesaleValue); // Negative

        // Set missing quantity totals needed for pharmacy income reports
        bfd.setTotalQuantity(totalQuantity);
        bfd.setTotalFreeQuantity(totalFreeQuantity);

        // Keep existing net and gross totals (these should remain positive)
        System.out.println("Final BillFinanceDetails totals - cost: " + totalCostValue +
                         ", purchase: " + totalPurchaseValue + ", retail: " + totalRetailSaleValue +
                         ", quantity: " + totalQuantity + ", netTotal: " + bill.getNetTotal());

        // Save bill finance details
        if (bill.getBillFinanceDetails().getId() == null) {
            System.out.println("BillFinanceDetails is new (id == null) - will be saved via cascade");
        } else {
            System.out.println("BillFinanceDetails exists, calling billFacade.edit()");
            billFacade.edit(bill);
        }

        System.out.println("=== calculateAndRecordCostingValues COMPLETE (Retail Sale) ===");
    }

    @FacesConverter("stockDtoConverter")
    public static class StockDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                Long id = Long.valueOf(value);
                PharmacyFastRetailSaleController controller = (PharmacyFastRetailSaleController) facesContext.getApplication().getELResolver()
                        .getValue(facesContext.getELContext(), null, "pharmacyFastRetailSaleController");
                if (controller != null && controller.getCachedStockDtos() != null) {
                    for (StockDTO dto : controller.getCachedStockDtos()) {
                        if (dto != null && id.equals(dto.getId())) {
                            return dto;
                        }
                    }
                }
                StockDTO dto = new StockDTO();
                dto.setId(id);
                return dto;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
            if (value == null) {
                return "";
            }
            if (value instanceof StockDTO) {
                StockDTO stockDto = (StockDTO) value;
                return stockDto.getId() != null ? stockDto.getId().toString() : "";
            }
            return "";
        }
    }

}
