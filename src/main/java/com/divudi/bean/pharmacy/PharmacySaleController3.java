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
import com.divudi.bean.common.ConfigOptionController;
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
import com.divudi.core.data.TokenType;
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
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
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
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.data.dto.StockValidationResult;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ConfigOptionFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.service.AuditService;
import com.divudi.service.BillService;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PaymentService;
import com.divudi.service.StockLockingService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.annotation.PostConstruct;

import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;

import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.PrimeFaces;

/**
 *
 * @author Buddhika
 */
@Named("pharmacySaleController3")
@SessionScoped
public class PharmacySaleController3 implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    private static final Logger logger = Logger.getLogger(PharmacySaleController3.class.getName());

    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private PaymentSchemeController PaymentSchemeController;
    @Inject
    private StockController stockController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private FinancialTransactionController financialTransactionController;
    @Inject
    private SessionController sessionController;
    @Inject
    private SearchController searchController;
    @Inject
    private PatientDepositController patientDepositController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private TokenController tokenController;
    @Inject
    private DrawerController drawerController;
    @Inject
    private PageMetadataRegistry pageMetadataRegistry;
    @EJB
    private ConfigOptionFacade configOptionFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;
    @EJB
    private StockHistoryFacade stockHistoryFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private DiscountSchemeValidationService discountSchemeValidationService;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private StaffService staffBean;
    @EJB
    private PaymentService paymentService;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private TokenFacade tokenFacade;
    @EJB
    private PharmacyService pharmacyService;
    @EJB
    private BillService billService;
    @EJB
    private AuditService auditService;
    @EJB
    private StockLockingService stockLockingService;
    /////////////////////////
    private PreBill preBill;
    private Bill saleBill;
    Bill printBill;
    Bill bill;
    BillItem billItem;
    private List<BillItem> selectedBillItems;
    //BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Integer intQty;
    Stock stock;
    StockDTO stockDto;
    /**
     * Temporary cache of the most recent autocomplete results. Used by
     * StockDtoConverter to preserve full DTO data during JSF lifecycle. NOT a
     * persistent cache - cleared on each new search.
     */
    private List<StockDTO> lastAutocompleteResults;

    // === PERFORMANCE OPTIMIZATION CACHES ===
    /**
     * Cache for converter operations during single add button action.
     * Cleared at start of each addBillItem() call to prevent stale data.
     */
    private transient Map<Long, StockDTO> converterCache;
    private transient int converterCallCount = 0;

    /**
     * Cache for discount calculations during single add button action.
     * Key format: "billItemId_paymentSchemeId_paymentMethod"
     */
    private transient Map<String, Double> discountCache;
    private transient int discountCalculationCount = 0;

    /**
     * Cache for item metadata only (names, codes, batch info) - NO STOCK QUANTITIES.
     * Stock quantities are always fetched fresh to ensure multi-user accuracy.
     * Key format: "department_id|query|search_config_hash"
     * TTL: 5 minutes for item metadata (changes rarely)
     */
    private transient Map<String, List<Long>> itemMetadataCache; // Cache stock IDs only
    private transient Map<String, Long> metadataCacheTimestamps;
    private static final long METADATA_CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes for metadata

    /**
     * Session-level configuration cache (refreshed only on browser restart)
     * This is safe because config changes require session restart anyway
     */
    private transient Boolean sessionCachedSearchByItemCode;
    private transient Boolean sessionCachedSearchByBarcode;
    private transient Boolean sessionCachedSearchByGeneric;
    private transient boolean configCacheInitialized = false;

    /**
     * Flag to track JPA warm-up completion status
     */
    private volatile boolean jpaWarmUpCompleted = false;

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
    PaymentMethodData paymentMethodData;
    private boolean patientDetailsEditable;
    private Department counter;
    Token currentToken;

    PaymentMethod paymentMethod;

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacySaleController3() {
    }

    @PostConstruct
    public void init() {
        registerPageMetadata();
        // Initialize JPA facades in background to prevent first-user delays
        warmUpJpaFacades();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("pharmacy/pharmacy_bill_retail_sale_for_cashier");
        metadata.setPageName("Pharmacy Retail Sale For Cashier");
        metadata.setDescription("Pharmacy retail sale interface for cashiers with token system support");
        metadata.setControllerClass("PharmacySaleController3");

        // 🔧 UI DISPLAY CONFIGURATIONS
        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable token system in sale for cashier",
            "Enables token-based queue management system for pharmacy sales",
            "Line 25: Token system controls and counter selection",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Analytics - Show Single Items Summary",
            "Shows button to view detailed analytics for individual pharmacy items",
            "Line 92: Item details button for analytics",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Allow Editing Quantity of Added Items in Pharmacy Retail Sale for Cashier",
            "Enables editing quantity of already added items in the bill items table",
            "Line 314: Quantity input field in bill items table",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Allow Tendered Amount for pharmacy sale for cashier",
            "Shows tendered amount and balance calculation fields for cash transactions",
            "Line 565: Tendered amount and balance display panel",
            OptionScope.APPLICATION
        ));

        // 🔧 PRINTING CONFIGURATIONS
        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Bill Support for Native Printers",
            "Enables native printer support for bill printing instead of browser printing",
            "Line 619: Print button rendering condition",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Sale for Cashier Token Bill is Pos paper",
            "Uses POS paper format for token bill printing",
            "Line 704: Token bill format rendering",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Sale for Cashier Bill is Pos paper",
            "Uses POS paper format for main bill printing",
            "Line 722: Main bill format rendering",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Retail Sale Bill is PosHeaderPaper",
            "Uses POS header paper format for bill printing",
            "Line 738: POS header format rendering",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Sale for cashier Bill is FiveFiveCustom3",
            "Uses FiveFiveCustom3 format for bill printing",
            "Line 748: FiveFiveCustom3 format rendering",
            OptionScope.APPLICATION
        ));

        // 🔧 SEARCH AND AUTOCOMPLETE CONFIGURATIONS
        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable search medicines by item code",
            "Enables searching medicines using item codes in autocomplete",
            "Controller: Medicine search functionality",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable search medicines by barcode",
            "Enables searching medicines using barcodes in autocomplete",
            "Controller: Medicine search functionality",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable search medicines by generic name(VMP)",
            "Enables searching medicines by generic names (Virtual Medicinal Products)",
            "Controller: Medicine search functionality",
            OptionScope.APPLICATION
        ));

        // 🔧 BILL VALIDATION CONFIGURATIONS
        metadata.addConfigOption(new ConfigOptionInfo(
            "Add bill item instructions in pharmacy cashier sale",
            "Includes special instructions when adding items to pharmacy bills",
            "Controller: Bill item processing logic",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Check for Allergies during Dispensing",
            "Validates patient allergies against prescribed medications during dispensing",
            "Controller: Patient safety validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Add quantity from multiple batches in pharmacy retail billing",
            "Allows combining quantities from multiple batches for the same item",
            "Controller: Quantity handling logic",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need to Enter the Cash Tendered Amount to Settle Pharmacy Retail Bill",
            "Requires cash tendered amount input before settling retail pharmacy bills",
            "Controller: Bill settlement validation",
            OptionScope.APPLICATION
        ));

        // 🔧 BILL NUMBER GENERATION STRATEGIES (CRITICAL)
        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number",
            "Generates bill numbers with format: Prefix-DEPT-INST-YEAR-NUMBER for pharmacy sale pre bills",
            "Controller: Bill number generation in savePreBillForCashier()",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number",
            "Generates bill numbers with format: Prefix-INST-DEPT-YEAR-NUMBER for pharmacy sale pre bills",
            "Controller: Bill number generation in savePreBillForCashier()",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number",
            "Generates bill numbers with format: Prefix-INST-YEAR-NUMBER for pharmacy sale pre bills",
            "Controller: Bill number generation in savePreBillForCashier()",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number",
            "Generates bill numbers with format: Prefix-DEPT-INST-YEAR-NUMBER for pharmacy sale cashier pre bills",
            "Controller: Bill number generation in savePreBillTOSettleInCashier()",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number",
            "Generates bill numbers with format: Prefix-INST-DEPT-YEAR-NUMBER for pharmacy sale cashier pre bills",
            "Controller: Bill number generation in savePreBillTOSettleInCashier()",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number",
            "Generates bill numbers with format: Prefix-INST-YEAR-NUMBER for pharmacy sale cashier pre bills",
            "Controller: Bill number generation in savePreBillTOSettleInCashier()",
            OptionScope.APPLICATION
        ));

        // 🔧 BILL NUMBER SUFFIXES (Required for BillNumberGenerator methods)
        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Suffix for PHARMACY_RETAIL_SALE_PRE",
            "Custom suffix to append to pharmacy retail sale pre bill numbers (used by BillNumberGenerator methods)",
            "Controller: BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE bill generation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Suffix for PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER",
            "Custom suffix to append to pharmacy retail sale pre bills to settle at cashier (used by BillNumberGenerator methods)",
            "Controller: BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER bill generation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Suffix for PHARMACY_RETAIL_SALE",
            "Custom suffix to append to pharmacy retail sale bill numbers (used by BillNumberGenerator methods)",
            "Controller: BillTypeAtomic.PHARMACY_RETAIL_SALE bill generation",
            OptionScope.APPLICATION
        ));

        // 🔧 PATIENT VALIDATION CONFIGURATIONS
        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient is required in Pharmacy Retail Sale",
            "Requires patient selection/creation for all pharmacy retail sales",
            "Controller: Patient validation in multiple methods",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient Name is required in Pharmacy Retail Sale",
            "Validates that patient name is provided for pharmacy retail sales",
            "Controller: Patient data validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient Phone is required in Pharmacy Retail Sale",
            "Validates that patient phone number is provided for pharmacy retail sales",
            "Controller: Patient contact validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient Gender is required in Pharmacy Retail Sale",
            "Validates that patient gender is provided for pharmacy retail sales",
            "Controller: Patient demographic validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient Address is required in Pharmacy Retail Sale",
            "Validates that patient address is provided for pharmacy retail sales",
            "Controller: Patient address validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient Area is required in Pharmacy Retail Sale",
            "Validates that patient area/location is provided for pharmacy retail sales",
            "Controller: Patient location validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable blacklist patient management in the system",
            "Enables system-wide blacklist patient management functionality",
            "Controller: Patient blacklist checking",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable blacklist patient management for Pharmacy from the system",
            "Enables blacklist patient management specifically for pharmacy module",
            "Controller: Pharmacy-specific patient blacklist checking",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Referring Doctor is required in Pharmacy Retail Sale",
            "Validates that referring doctor is specified for pharmacy retail sales",
            "Controller: Doctor referral validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient Phone number is mandotary in sale for cashier",
            "Makes patient phone number mandatory specifically for cashier sales",
            "Controller: Cashier-specific patient validation",
            OptionScope.APPLICATION
        ));

        // 🔧 DETAILED PATIENT CREATION REQUIREMENTS
        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient Title And Gender To Save Patient in Pharmacy Sale",
            "Requires patient title and gender when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient Name to save Patient in Pharmacy Sale",
            "Requires patient name when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient Age to Save Patient in Pharmacy Sale",
            "Requires patient age when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient Phone Number to save Patient in Pharmacy Sale",
            "Requires patient phone number when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient Address to save Patient in Pharmacy Sale",
            "Requires patient address when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient Mail to save Patient in Pharmacy Sale",
            "Requires patient email address when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient NIC to save Patient in Pharmacy Sale",
            "Requires patient NIC (National Identity Card) when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Patient Area to save Patient in Pharmacy Sale",
            "Requires patient area/location when creating new patients in pharmacy sales",
            "Controller: New patient creation validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Need Referring Doctor to settlle bill in Pharmacy Sale",
            "Requires referring doctor selection before settling pharmacy bills",
            "Controller: Bill settlement validation",
            OptionScope.APPLICATION
        ));

        // 🔧 PAYMENT AND SETTLEMENT CONFIGURATIONS
        metadata.addConfigOption(new ConfigOptionInfo(
            "Patient details are required for retail sale",
            "Requires patient details to be completed for retail sales",
            "Controller: Retail sale validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy retail sale CreditCard last digits is Mandatory",
            "Requires last 4 digits of credit card for credit card payments in pharmacy retail sales",
            "Controller: Credit card payment validation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy discount should be staff when select Staff_welfare as payment method",
            "Automatically applies staff discount when staff welfare payment method is selected",
            "Controller: Staff welfare payment validation",
            OptionScope.APPLICATION
        ));

        // 🔧 PRIVILEGES
        metadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to system configuration and page management",
            "Line 39: Config button visibility"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
            "ChangeReceiptPrintingPaperTypes",
            "Permission to change receipt printing paper types and formats",
            "Line 603: Paper type selection controls"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
            "PharmacySale",
            "Basic permission to access pharmacy sale functionalities",
            "Lines 657-660: Sale navigation buttons"
        ));

        // 🔧 REGISTER THE METADATA
        pageMetadataRegistry.registerPage(metadata);
    }

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
    public String navigateToPharmacyBillForCashier() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
            } else {
                setBillSettlingStarted(false);
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
        }
    }

    public String navigateToPharmacyBillForCashierWholeSale() {
        setBillSettlingStarted(false);
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
    }

    public String navigateToBillCancellationView() {
        return "pharmacy_cancel_bill_retail?faces-redirect=true";
    }

    private void prepareForPharmacySaleWithoutStock() {
        clearBill();
        clearBillItem();
        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public void searchPatientListener() {
        //  createPaymentSchemeItems();
        calculateRatesForAllBillItemsInPreBill();
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

    public void prepareNewPharmacyBillForMembers() {
        clearNewBillForMembers();
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public void clearForNewBill() {
        preBill = null;
        saleBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        paymentScheme = null;
        paymentMethod = null;
        activeIndex = 0;
        patient = null;
        yearMonthDay = null;
        patientTabId = "tabPt";
        strTenderedValue = "";
        billPreview = false;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;
        cashPaidStr = null;
    }

    public void clearNewBillForMembers() {
        preBill = null;
        saleBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        paymentScheme = null;
        paymentMethod = null;
        activeIndex = 0;
        patient = null;
        yearMonthDay = null;
        patientTabId = "tabSearchPt";
        patientSearchTab = 1;
        strTenderedValue = "";
        billPreview = false;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;
        cashPaidStr = null;
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

    public void onTabChange(TabChangeEvent event) {
        if (event != null && event.getTab() != null) {
            setPatientTabId(event.getTab().getId());
        }

        if (!getPatientTabId().equals("tabSearchPt")) {
            if (fromOpdEncounter == false) {
                setPatient(null);
            }
        }

//        createPaymentSchemeItems();
        calculateRatesForAllBillItemsInPreBill();

    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd == null) {
                    continue;
                }
                if (cd.getPaymentMethodData() != null && cd.getPaymentMethod() != null) {
                    // Only add the value from the selected payment method for this ComponentDetail
                    switch (cd.getPaymentMethod()) {
                        case Cash:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                            break;
                        case Card:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                            break;
                        case Cheque:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                            break;
                        case ewallet:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                            break;
                        case PatientDeposit:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                            break;
                        case Slip:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                            break;
                        case Staff:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                            break;
                        case Staff_Welfare:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                            break;
                        case Credit:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCredit().getTotalValue();
                            break;
                        case OnlineSettlement:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
                            break;
                        default:
                            break;
                    }
                }
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
            if (arrSize == 0) {
                return; // No payment methods added yet
            }
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethodData() == null) {
                return; // Payment method data not initialized
            }
            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                // Only set value automatically if not already set by user
                if (pm.getPaymentMethodData().getCash().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                if (pm.getPaymentMethodData().getCreditCard().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                if (pm.getPaymentMethodData().getCheque().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                if (pm.getPaymentMethodData().getSlip().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                if (pm.getPaymentMethodData().getEwallet().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                if (patient == null || patient.getId() == null) {
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                    return; // Patient not selected yet, ignore
                }
                // Initialize patient deposit data for UI component
                pm.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                PatientDeposit pd = patientDepositController.getDepositOfThePatient(patient, sessionController.getDepartment());
                if (pd != null && pd.getId() != null) {
                    pm.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                    pm.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                    // Set total value to remain amount only if there's sufficient balance, otherwise set to available balance
                    double availableBalance = pd.getBalance();
                    if (availableBalance >= remainAmount) {
                        pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
                    } else {
                        pm.getPaymentMethodData().getPatient_deposit().setTotalValue(availableBalance);
                    }
                } else {
                    pm.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(false);
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                if (pm.getPaymentMethodData().getCredit().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff) {
                if (pm.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                if (pm.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getStaffWelfare().setTotalValue(remainAmount);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.OnlineSettlement) {
                if (pm.getPaymentMethodData().getOnlineSettlement().getTotalValue() == 0.0) {
                    pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(remainAmount);
                }
            }

        }
    }

    private void cleanupInvalidPaymentDetails() {
        if (paymentMethodData == null
                || paymentMethodData.getPaymentMethodMultiple() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
            return;
        }

        // Remove ComponentDetails with null paymentMethodData or null paymentMethod
        paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()
                .removeIf(cd -> cd.getPaymentMethodData() == null || cd.getPaymentMethod() == null);
    }

    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null
                || paymentMethodData == null
                || paymentMethodData.getPaymentMethodMultiple() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
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

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        if (tmp == null) {
            return;
        }
        execureOnEditActions(tmp);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);

        //userStockController.updateUserStock(tmp.getTransUserStock(), 0); // Commented out for performance
    }

    //Check when edititng Qty
    //
    public boolean execureOnEditActions(BillItem tmp) {
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

        //Check Is There Any Other User using same Stock - COMMENTED OUT FOR PERFORMANCE
        //if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {
        //
        //    setZeroToQty(tmp);
        //    onEditCalculation(tmp);
        //    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
        //    return true;
        //}
        //userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty()); // Commented out for performance
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
        tmp.getPharmaceuticalBillItem().setQty(0 - tmp.getQty());

        calculateBillItemForEditing(tmp);

        calculateBillItemsAndBillTotalsOfPreBill();

    }

    public void quantityInTableChangeEvent(BillItem tmp) {
        if (tmp == null) {
            return;
        }

        // Validate pharmaceutical bill item and stock
        if (tmp.getPharmaceuticalBillItem() == null) {
            JsfUtil.addErrorMessage("Invalid bill item - pharmaceutical information missing");
            return;
        }

        if (tmp.getPharmaceuticalBillItem().getStock() == null) {
            JsfUtil.addErrorMessage("Invalid bill item - stock information missing");
            return;
        }

        // Validate quantity
        if (tmp.getQty() == null || tmp.getQty() <= 0) {
            JsfUtil.addErrorMessage("Please enter a valid quantity greater than 0");
            return;
        }

        try {
            // Fetch latest stock information to get current available quantity
            Stock currentStock = stockFacade.find(tmp.getPharmaceuticalBillItem().getStock().getId());

            if (currentStock == null) {
                JsfUtil.addErrorMessage("Stock information not found. Please refresh and try again.");
                return;
            }

            // Check if entered quantity exceeds available stock
            if (tmp.getQty() > currentStock.getStock()) {
                JsfUtil.addErrorMessage("Insufficient stock. Available: " +
                    String.format("%.0f", currentStock.getStock()) +
                    ", Requested: " + String.format("%.0f", tmp.getQty()));

                // Reset quantity to maximum available stock
                tmp.setQty(currentStock.getStock());

                // Recalculate with the adjusted quantity
                tmp.setGrossValue(tmp.getQty() * tmp.getRate());
                tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());
                calculateBillItemForEditing(tmp);
                calculateBillItemsAndBillTotalsOfPreBill();
                return;
            }

            // If validation passes, proceed with normal calculation
            tmp.setGrossValue(tmp.getQty() * tmp.getRate());
            tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());

            calculateBillItemForEditing(tmp);

            calculateBillItemsAndBillTotalsOfPreBill();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error validating stock quantity: " + e.getMessage());
            System.out.println("Error in quantityInTableChangeEvent: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void editQty(BillItem bi) {
        if (bi == null) {
            //////System.out.println("No Bill Item to Edit Qty");
            return;
        }
        if (editingQty == null) {
            //////System.out.println("Editing qty is null");
            return;
        }

        bi.setQty(editingQty);
        bi.getPharmaceuticalBillItem().setQtyInUnit(0 - editingQty);
        calculateBillItemForEditing(bi);

        calculateBillItemsAndBillTotalsOfPreBill();
        editingQty = null;
    }

    /**
     * Divides the quantity of a single bill item by half.
     * Minimum quantity is 1 - never goes below 1.
     * Uses existing quantityInTableChangeEvent for validation and calculations.
     */
    public void divideQuantityByHalf(BillItem billItem) {
        if (billItem == null) {
            JsfUtil.addErrorMessage("No item selected");
            return;
        }

        if (billItem.getQty() == null || billItem.getQty() <= 0) {
            JsfUtil.addErrorMessage("Invalid quantity");
            return;
        }

        // Calculate new quantity (divide by 2, minimum 1)
        double newQty = Math.max(1.0, billItem.getQty() / 2.0);

        // Set the new quantity
        billItem.setQty(newQty);

        // Use existing method for validation and recalculation
        quantityInTableChangeEvent(billItem);

        JsfUtil.addSuccessMessage("Quantity reduced to " + String.format("%.0f", newQty));
    }

    /**
     * Multiplies the quantity of a single bill item by two.
     * Validates against available stock before applying changes.
     * Uses existing quantityInTableChangeEvent for validation and calculations.
     */
    public void multiplyQuantityByTwo(BillItem billItem) {
        if (billItem == null) {
            JsfUtil.addErrorMessage("No item selected");
            return;
        }

        if (billItem.getQty() == null || billItem.getQty() <= 0) {
            JsfUtil.addErrorMessage("Invalid quantity");
            return;
        }

        // Validate pharmaceutical bill item and stock
        if (billItem.getPharmaceuticalBillItem() == null) {
            JsfUtil.addErrorMessage("Invalid bill item - pharmaceutical information missing");
            return;
        }

        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            JsfUtil.addErrorMessage("Invalid bill item - stock information missing");
            return;
        }

        try {
            // Fetch latest stock information
            Stock currentStock = stockFacade.find(billItem.getPharmaceuticalBillItem().getStock().getId());

            if (currentStock == null) {
                JsfUtil.addErrorMessage("Stock information not found. Please refresh and try again.");
                return;
            }

            // Calculate new quantity (multiply by 2)
            double newQty = billItem.getQty() * 2.0;

            // Check if new quantity exceeds available stock
            if (newQty > currentStock.getStock()) {
                JsfUtil.addErrorMessage("Cannot double quantity. Available: " +
                    String.format("%.0f", currentStock.getStock()) +
                    ", Required: " + String.format("%.0f", newQty));
                return;
            }

            // Set the new quantity
            billItem.setQty(newQty);

            // Use existing method for validation and recalculation
            quantityInTableChangeEvent(billItem);

            JsfUtil.addSuccessMessage("Quantity increased to " + String.format("%.0f", newQty));

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error validating stock quantity: " + e.getMessage());
            System.out.println("Error in multiplyQuantityByTwo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Divides all bill item quantities by half.
     * Minimum quantity is 1 for each item - never goes below 1.
     * Processes all items and recalculates bill totals.
     */
    public void divideAllQuantitiesByHalf() {
        if (getPreBill() == null || getPreBill().getBillItems() == null || getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No items in the bill");
            return;
        }

        int processedCount = 0;
        int errorCount = 0;

        for (BillItem billItem : getPreBill().getBillItems()) {
            try {
                if (billItem != null && billItem.getQty() != null && billItem.getQty() > 0) {
                    // Calculate new quantity (divide by 2, minimum 1)
                    double newQty = Math.max(1.0, billItem.getQty() / 2.0);
                    billItem.setQty(newQty);

                    // Update pharmaceutical bill item quantity
                    if (billItem.getPharmaceuticalBillItem() != null) {
                        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - newQty);
                    }

                    // Recalculate gross value
                    billItem.setGrossValue(billItem.getQty() * billItem.getRate());

                    processedCount++;
                }
            } catch (Exception e) {
                errorCount++;
                System.out.println("Error processing item in divideAllQuantitiesByHalf: " + e.getMessage());
            }
        }

        // Recalculate all bill totals
        calculateBillItemsAndBillTotalsOfPreBill();

        if (errorCount > 0) {
            JsfUtil.addErrorMessage(processedCount + " items reduced, " + errorCount + " items failed");
        } else {
            JsfUtil.addSuccessMessage("All quantities reduced by half (" + processedCount + " items processed)");
        }
    }

    /**
     * Multiplies all bill item quantities by two.
     * Validates each item against available stock before applying changes.
     * Items that exceed stock limits are skipped with error messages.
     * Recalculates bill totals after processing.
     */
    public void multiplyAllQuantitiesByTwo() {
        if (getPreBill() == null || getPreBill().getBillItems() == null || getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No items in the bill");
            return;
        }

        int processedCount = 0;
        int skippedCount = 0;
        StringBuilder skippedItems = new StringBuilder();

        for (BillItem billItem : getPreBill().getBillItems()) {
            try {
                if (billItem == null || billItem.getQty() == null || billItem.getQty() <= 0) {
                    skippedCount++;
                    continue;
                }

                // Validate pharmaceutical bill item and stock
                if (billItem.getPharmaceuticalBillItem() == null ||
                    billItem.getPharmaceuticalBillItem().getStock() == null) {
                    skippedCount++;
                    continue;
                }

                // Fetch latest stock information
                Stock currentStock = stockFacade.find(billItem.getPharmaceuticalBillItem().getStock().getId());

                if (currentStock == null) {
                    skippedCount++;
                    continue;
                }

                // Calculate new quantity (multiply by 2)
                double newQty = billItem.getQty() * 2.0;

                // Check if new quantity exceeds available stock
                if (newQty > currentStock.getStock()) {
                    skippedCount++;
                    String itemName = billItem.getPharmaceuticalBillItem().getStock().getItemBatch().getItem().getName();
                    if (skippedItems.length() > 0) {
                        skippedItems.append(", ");
                    }
                    skippedItems.append(itemName);
                    continue;
                }

                // Set the new quantity
                billItem.setQty(newQty);

                // Update pharmaceutical bill item quantity
                if (billItem.getPharmaceuticalBillItem() != null) {
                    billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - newQty);
                }

                // Recalculate gross value
                billItem.setGrossValue(billItem.getQty() * billItem.getRate());

                processedCount++;

            } catch (Exception e) {
                skippedCount++;
                System.out.println("Error processing item in multiplyAllQuantitiesByTwo: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Recalculate all bill totals
        calculateBillItemsAndBillTotalsOfPreBill();

        // Show results
        if (skippedCount > 0 && processedCount > 0) {
            JsfUtil.addSuccessMessage(processedCount + " items doubled successfully");
            JsfUtil.addErrorMessage(skippedCount + " items skipped due to insufficient stock: " + skippedItems.toString());
        } else if (skippedCount > 0 && processedCount == 0) {
            JsfUtil.addErrorMessage("No items could be doubled. All items have insufficient stock.");
        } else {
            JsfUtil.addSuccessMessage("All quantities doubled (" + processedCount + " items processed)");
        }
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
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Stock convertStockDtoToEntity(StockDTO stockDto) {
        long startTime = System.currentTimeMillis();
        System.out.println("=== convertStockDtoToEntity START - StockDTO ID: " + (stockDto != null ? stockDto.getId() : "null") + " ===");

        if (stockDto == null || stockDto.getId() == null) {
            System.out.println("convertStockDtoToEntity: stockDto or ID is null, returning null");
            return null;
        }

        long beforeGetReference = System.currentTimeMillis();
        System.out.println("convertStockDtoToEntity: Getting stock reference for ID: " + stockDto.getId());

        // Use EntityManager.getReference() to get JPA proxy WITHOUT database query
        // Returns a proxy that only loads data when you access entity properties
        // This is instantaneous (~0ms) compared to find() which queries database
        // Perfect for setting entity references for persistence without needing the actual data
        try {
            Stock result = stockFacade.getReference(stockDto.getId());
            long afterGetReference = System.currentTimeMillis();
            long totalTime = afterGetReference - startTime;
            long getRefTime = afterGetReference - beforeGetReference;
            System.out.println("convertStockDtoToEntity: stockFacade.getReference took " + getRefTime + "ms");
            System.out.println("=== convertStockDtoToEntity END - Total time: " + totalTime + "ms ===");
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            System.out.println("convertStockDtoToEntity: Exception occurred - " + e.getMessage());
            System.out.println("=== convertStockDtoToEntity END (EXCEPTION) - Total time: " + (endTime - startTime) + "ms ===");
            return null;
        }
    }

    public StockDTO getStockDto() {
        return stockDto;
    }

    public void setStockDto(StockDTO stockDto) {
        this.stockDto = stockDto;
    }

    public List<StockDTO> getLastAutocompleteResults() {
        return lastAutocompleteResults;
    }

    public void setLastAutocompleteResults(List<StockDTO> lastAutocompleteResults) {
        this.lastAutocompleteResults = lastAutocompleteResults;
    }

    public String newSaleBillWithoutReduceStock() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale";
    }

    public String newSaleBillWithoutReduceStockForCashier() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale_for_cashier";
    }

    public String navigateToPharmacyRetailSale() {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetAll();
                setBillSettlingStarted(false);
                return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            resetAll();
            setBillSettlingStarted(false);
            setBillSettlingStarted(false);
            return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
        }
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

    public String navigateToPharmacyRetailSale(Patient pt, PaymentScheme ps) {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                return navigateToPharmacyRetailSaleAfterCashierCheck(pt, ps);
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            return navigateToPharmacyRetailSaleAfterCashierCheck(pt, ps);
        }
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

        // Clear all existing data first
        resetAll();

        // Then set the patient and payment scheme after clearing
        patient = pt;
        paymentScheme = ps;
        setPatient(patient);
        setBillSettlingStarted(false);

        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier?faces-redirect=true";
    }

    public String navigateToPharmacyRetailSaleForCashier(Patient pt, PaymentScheme ps) {
        if (sessionController.getPharmacyBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                return navigateToPharmacyRetailSaleAfterCashierCheckForCashier(pt, ps);
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            return navigateToPharmacyRetailSaleAfterCashierCheckForCashier(pt, ps);
        }
    }

    public void resetAll() {
        setBillSettlingStarted(false);
        //userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser()); // Commented out for performance
        clearBill();
        clearBillItem();
//        searchController.createPreBillsNotPaid();
        billPreview = false;

        // Clear temporary cache
        lastAutocompleteResults = null;
        System.out.println("=== CASHIER resetAll: Cleared cache and reset all fields ===");
    }

    public void prepareForNewPharmacyRetailBill() {
        //userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser()); // Commented out for performance
        clearBill();
        clearBillItem();
//        searchController.createPreBillsNotPaid();
        billPreview = false;
    }

    public String pharmacyRetailSale() {
        setBillSettlingStarted(false);
        return "/pharmacy_wholesale/pharmacy_bill_retail_sale?faces-redirect=true";
    }

    public String toPharmacyRetailSale() {
        setBillSettlingStarted(false);
        return "/pharmacy/pharmacy_bill_retail_sale?faces-redirect=true";
    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i where i.retired=false "
                + " and (i.name) like :n and type(i)=:t "
                + " and i.id not in(select ibs.id from Stock ibs where ibs.stock >:s and ibs.department=:d and (ibs.itemBatch.item.name) like :n ) order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }






    public List<Stock> completeAvailableStockOptimized(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }

        qry = qry.replaceAll("[\\n\\r]", "").trim();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", getSessionController().getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");

        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);

        StringBuilder sql = new StringBuilder("SELECT i FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department ")
                .append("AND (");

        sql.append("i.itemBatch.item.name LIKE :query ");

        if (searchByItemCode) {
            sql.append("OR i.itemBatch.item.code LIKE :query ");
        }

        if (searchByBarcode) {
            sql.append("OR i.itemBatch.item.barcode = :query ");
        }

        if (searchByGeneric) {
            sql.append("OR i.itemBatch.item.vmp.vtm.name LIKE :query ");
        }

        sql.append(") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire");

        return getStockFacade().findByJpql(sql.toString(), parameters, 20);
    }

    public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
        long startTime = System.nanoTime();

        try {
            // Input validation
            if (qry == null || qry.trim().isEmpty()) {
                lastAutocompleteResults = Collections.emptyList();
                logger.log(Level.FINE, "Autocomplete query is empty, returning empty list");
                return lastAutocompleteResults;
            }

            qry = qry.replaceAll("[\\n\\r]", "").trim().toUpperCase();

            // Initialize metadata cache if needed
            if (itemMetadataCache == null) {
                itemMetadataCache = new ConcurrentHashMap<>();
                metadataCacheTimestamps = new ConcurrentHashMap<>();
            }

            // Get cached search configuration
            SearchConfig searchConfig = getCachedSearchConfig(qry.length() > 6);

            // Build cache key for metadata
            String cacheKey = buildAutocompleteCacheKey(qry, searchConfig);

            // Check metadata cache first
            List<Long> cachedStockIds = getCachedMetadata(cacheKey);
            List<StockDTO> results;

            if (cachedStockIds != null) {
                // Cache hit: Get fresh stock data for cached stock IDs with preserved relevance ordering
                results = getFreshStockDataForIds(cachedStockIds);
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                logger.log(Level.FINE, "Metadata cache hit for query ''{0}'' in {1}ms - fetched fresh stock for {2} items with preserved ordering",
                    new Object[]{qry, durationMs, results.size()});
            } else {
                // Cache miss: Execute full search with EclipseLink-compatible relevance ordering and cache the stock IDs
                results = executeFullSearchAndCacheMetadata(qry, searchConfig, cacheKey);
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                if (durationMs > 500) {
                    logger.log(Level.WARNING, "Slow autocomplete query: {0}ms for query ''{1}'' returned {2} results",
                        new Object[]{durationMs, qry, results.size()});
                } else {
                    logger.log(Level.FINE, "Autocomplete query completed in {0}ms for query ''{1}'' returned {2} results with relevance ordering",
                        new Object[]{durationMs, qry, results.size()});
                }
            }

            // Store results for JSF converter
            lastAutocompleteResults = results != null ? results : Collections.emptyList();
            return lastAutocompleteResults;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during autocomplete query for: " + qry, e);
            lastAutocompleteResults = Collections.emptyList();
            return lastAutocompleteResults;
        }
    }

    /**
     * Inner class to hold search configuration for caching
     */
    private static class SearchConfig {
        boolean searchByItemCode;
        boolean searchByBarcode;
        boolean searchByGeneric;

        SearchConfig(boolean searchByItemCode, boolean searchByBarcode, boolean searchByGeneric) {
            this.searchByItemCode = searchByItemCode;
            this.searchByBarcode = searchByBarcode;
            this.searchByGeneric = searchByGeneric;
        }

        @Override
        public String toString() {
            return searchByItemCode + "|" + searchByBarcode + "|" + searchByGeneric;
        }
    }

    /**
     * Get session-level cached search configuration (initialized once per session)
     */
    private SearchConfig getCachedSearchConfig(boolean longQuery) {
        // Initialize session-level config cache once per session
        if (!configCacheInitialized) {
            sessionCachedSearchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                    "Enable search medicines by item code", true);
            sessionCachedSearchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                    "Enable search medicines by generic name(VMP)", false);
            configCacheInitialized = true;

            logger.log(Level.INFO, "Search configuration cached for session: ItemCode={0}, Generic={1}",
                new Object[]{sessionCachedSearchByItemCode, sessionCachedSearchByGeneric});
        }

        // Barcode search still depends on query length but uses session cache
        boolean searchByBarcode;
        if (longQuery) {
            if (sessionCachedSearchByBarcode == null) {
                sessionCachedSearchByBarcode = configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true);
            }
            searchByBarcode = sessionCachedSearchByBarcode;
        } else {
            searchByBarcode = false; // Don't search barcode for short queries
        }

        return new SearchConfig(sessionCachedSearchByItemCode, searchByBarcode, sessionCachedSearchByGeneric);
    }

    /**
     * Build cache key for autocomplete results
     */
    private String buildAutocompleteCacheKey(String query, SearchConfig searchConfig) {
        Department dept = getSessionController().getLoggedUser().getDepartment();
        return dept.getId() + "|" + query + "|" + searchConfig.toString();
    }

    /**
     * Get cached stock IDs for metadata-only caching (multi-user safe)
     */
    private List<Long> getCachedMetadata(String cacheKey) {
        if (itemMetadataCache == null || metadataCacheTimestamps == null) {
            return null;
        }

        Long timestamp = metadataCacheTimestamps.get(cacheKey);
        if (timestamp == null) {
            return null;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - timestamp > METADATA_CACHE_TTL_MS) {
            // Cache expired, remove entries
            itemMetadataCache.remove(cacheKey);
            metadataCacheTimestamps.remove(cacheKey);
            return null;
        }

        return itemMetadataCache.get(cacheKey);
    }

    /**
     * Get fresh stock data for cached stock IDs with preserved ordering (ensures multi-user accuracy)
     */
    private List<StockDTO> getFreshStockDataForIds(List<Long> stockIds) {
        if (stockIds == null || stockIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Build query to get current stock data for specific stock IDs
            // Note: We use the original order from stockIds list to preserve relevance ranking
            StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
                    .append("s.id, s.itemBatch.id, s.itemBatch.item.id, s.itemBatch.item.name, s.itemBatch.item.code, ")
                    .append("COALESCE(s.itemBatch.item.vmp.name, ''), s.itemBatch.batchNo, ")
                    .append("s.itemBatch.retailsaleRate, s.stock, s.itemBatch.dateOfExpire, s.itemBatch.item.discountAllowed) ")
                    .append("FROM Stock s ")
                    .append("WHERE s.stock > :stockMin ")
                    .append("AND s.department = :department ")
                    .append("AND s.id IN :stockIds ");

            // Preserve the original CASE-based ordering from the cached metadata
            // This ensures consistent result ordering between cache misses and hits
            sql.append("ORDER BY ")
                .append("CASE ");

            // Add specific ordering for each cached stock ID to preserve relevance ranking
            for (int i = 0; i < stockIds.size() && i < 20; i++) {
                sql.append("WHEN s.id = ").append(stockIds.get(i)).append(" THEN ").append(i).append(" ");
            }

            sql.append("ELSE 999 END, s.itemBatch.dateOfExpire");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("department", getSessionController().getLoggedUser().getDepartment());
            parameters.put("stockMin", 0.0);
            parameters.put("stockIds", stockIds);

            List<StockDTO> results = (List<StockDTO>) getStockFacade().findLightsByJpql(
                    sql.toString(), parameters, TemporalType.TIMESTAMP, 20);

            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching fresh stock data for cached IDs", e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute optimized search query with EclipseLink-compatible relevance ordering and cache the stock IDs
     */
    private List<StockDTO> executeFullSearchAndCacheMetadata(String qry, SearchConfig searchConfig, String cacheKey) {
        try {
            // Use LinkedHashSet to maintain ordering and eliminate duplicates
            Set<StockDTO> orderedResults = new LinkedHashSet<>();

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("department", getSessionController().getLoggedUser().getDepartment());
            parameters.put("stockMin", 0.0);
            parameters.put("searchStart", qry + "%");      // Exact prefix matches
            parameters.put("exactQuery", qry);             // For barcode exact match

            // Step 1: Get prefix matches first (highest relevance)
            List<StockDTO> prefixResults = executeSearchQuery(qry, searchConfig, parameters, true);
            if (prefixResults != null) {
                orderedResults.addAll(prefixResults);
            }

            // Step 2: Get contains matches if we need more results (lower relevance)
            if (orderedResults.size() < 20) {
                parameters.put("searchContains", "%" + qry + "%");
                List<StockDTO> containsResults = executeSearchQuery(qry, searchConfig, parameters, false);
                if (containsResults != null) {
                    orderedResults.addAll(containsResults); // Set automatically handles duplicates
                }
            }

            // Convert to list maintaining the insertion order (relevance order)
            List<StockDTO> finalResults = new ArrayList<>(orderedResults);

            // Limit to 20 results
            if (finalResults.size() > 20) {
                finalResults = finalResults.subList(0, 20);
            }

            if (!finalResults.isEmpty()) {
                // Cache the stock IDs for future metadata lookups (in relevance order)
                List<Long> stockIds = new ArrayList<>();
                for (StockDTO dto : finalResults) {
                    stockIds.add(dto.getId());
                }
                cacheMetadata(cacheKey, stockIds);

                logger.log(Level.FINE, "Search completed with relevance ordering - {0} results (prefix matches first, then contains matches)",
                    finalResults.size());
            }

            return finalResults;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing autocomplete search query with relevance ordering", e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute search query for either prefix or contains pattern
     */
    private List<StockDTO> executeSearchQuery(String qry, SearchConfig searchConfig, Map<String, Object> parameters, boolean prefixSearch) {
        try {
            StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
                    .append("s.id, s.itemBatch.id, s.itemBatch.item.id, s.itemBatch.item.name, s.itemBatch.item.code, ")
                    .append("COALESCE(s.itemBatch.item.vmp.name, ''), s.itemBatch.batchNo, ")
                    .append("s.itemBatch.retailsaleRate, s.stock, s.itemBatch.dateOfExpire, s.itemBatch.item.discountAllowed) ")
                    .append("FROM Stock s ")
                    .append("WHERE s.stock > :stockMin ")
                    .append("AND s.department = :department ");

            // Create a copy of parameters and only include the ones we actually use
            Map<String, Object> queryParameters = new HashMap<>();
            queryParameters.put("stockMin", parameters.get("stockMin"));
            queryParameters.put("department", parameters.get("department"));

            // Build search conditions based on search type
            if (prefixSearch) {
                sql.append("AND (");
                boolean firstCondition = true;
                boolean usesExactQuery = false;

                // Item name prefix search (highest priority)
                sql.append("UPPER(s.itemBatch.item.name) LIKE UPPER(:searchStart)");
                firstCondition = false;
                queryParameters.put("searchStart", parameters.get("searchStart"));

                // Item code prefix search (if enabled)
                if (searchConfig.searchByItemCode) {
                    if (!firstCondition) sql.append(" OR ");
                    sql.append("UPPER(s.itemBatch.item.code) LIKE UPPER(:searchStart)");
                    firstCondition = false;
                    // searchStart already added above
                }

                // Barcode exact match (if enabled and query is long enough)
                if (searchConfig.searchByBarcode && qry.length() >= 6) {
                    if (!firstCondition) sql.append(" OR ");
                    sql.append("s.itemBatch.item.barcode = :exactQuery");
                    firstCondition = false;
                    usesExactQuery = true;
                }

                // Generic name prefix search (if enabled)
                if (searchConfig.searchByGeneric) {
                    if (!firstCondition) sql.append(" OR ");
                    sql.append("UPPER(s.itemBatch.item.vmp.vtm.name) LIKE UPPER(:searchStart)");
                    // searchStart already added above
                }

                sql.append(") ORDER BY s.itemBatch.item.name, s.itemBatch.dateOfExpire");

                // Only add exactQuery parameter if it's actually used
                if (usesExactQuery) {
                    queryParameters.put("exactQuery", parameters.get("exactQuery"));
                }

            } else {
                // Contains search (excludes items already found in prefix search)
                sql.append("AND (");
                boolean firstCondition = true;

                // Add the parameters we'll use for contains search
                queryParameters.put("searchContains", parameters.get("searchContains"));
                queryParameters.put("searchStart", parameters.get("searchStart"));

                // Item name contains (but not prefix to avoid duplicates)
                sql.append("UPPER(s.itemBatch.item.name) LIKE UPPER(:searchContains) ")
                   .append("AND NOT UPPER(s.itemBatch.item.name) LIKE UPPER(:searchStart)");
                firstCondition = false;

                // Item code contains (if enabled)
                if (searchConfig.searchByItemCode) {
                    if (!firstCondition) sql.append(" OR ");
                    sql.append("(UPPER(s.itemBatch.item.code) LIKE UPPER(:searchContains) ")
                       .append("AND NOT UPPER(s.itemBatch.item.code) LIKE UPPER(:searchStart))");
                    firstCondition = false;
                }

                // Generic name contains (if enabled)
                if (searchConfig.searchByGeneric) {
                    if (!firstCondition) sql.append(" OR ");
                    sql.append("(UPPER(s.itemBatch.item.vmp.vtm.name) LIKE UPPER(:searchContains) ")
                       .append("AND NOT UPPER(s.itemBatch.item.vmp.vtm.name) LIKE UPPER(:searchStart))");
                }

                sql.append(") ORDER BY s.itemBatch.item.name, s.itemBatch.dateOfExpire");
            }

            // Execute query with only the parameters that are actually used
            List<StockDTO> results = (List<StockDTO>) getStockFacade().findLightsByJpql(
                    sql.toString(), queryParameters, TemporalType.TIMESTAMP, 20);

            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing " + (prefixSearch ? "prefix" : "contains") + " search query", e);
            return Collections.emptyList();
        }
    }

    /**
     * Cache stock IDs for metadata caching
     */
    private void cacheMetadata(String cacheKey, List<Long> stockIds) {
        if (itemMetadataCache == null) {
            itemMetadataCache = new ConcurrentHashMap<>();
        }
        if (metadataCacheTimestamps == null) {
            metadataCacheTimestamps = new ConcurrentHashMap<>();
        }

        // Limit cache size to prevent memory issues
        if (itemMetadataCache.size() > 100) {
            // Simple cache eviction: remove oldest entries
            itemMetadataCache.clear();
            metadataCacheTimestamps.clear();
        }

        itemMetadataCache.put(cacheKey, new ArrayList<>(stockIds)); // Defensive copy
        metadataCacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }

    public void handleSelectAction() {
        long startTime = System.currentTimeMillis();

        if (stockDto == null) {
            return;
        }

        long beforeCalculateRates = System.currentTimeMillis();

        // Entity conversion removed from here - will happen in calculateBillItem or addBillItemSingleItem when needed
        // This eliminates 2000ms+ delay during item selection
        calculateRatesOfSelectedBillItemBeforeAddingToTheList(billItem);

        long beforeAddInstructions = System.currentTimeMillis();

        // Add instructions only if enabled (default: false for performance)
        if (configOptionApplicationController.getBooleanValueByKey("Add bill item instructions in pharmacy cashier sale", false)) {
            pharmacyService.addBillItemInstructions(billItem);
        }

        long endTime = System.currentTimeMillis();
    }

    public void handleSelect(SelectEvent event) {
        long startTime = System.nanoTime();

        try {
            // Get the selected StockDTO from the event to ensure we have the full DTO with all fields
            if (event != null && event.getObject() != null && event.getObject() instanceof StockDTO) {
                StockDTO selectedDto = (StockDTO) event.getObject();
                logger.log(Level.FINE, "Item selected - ID: {0}, StockQty: {1}, ItemName: {2}",
                    new Object[]{selectedDto.getId(), selectedDto.getStockQty(), selectedDto.getItemName()});
                this.stockDto = selectedDto;
            } else {
                logger.log(Level.WARNING, "handleSelect called with null or invalid event object");
                return;
            }

            handleSelectAction();

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            if (durationMs > 1000) {
                logger.log(Level.WARNING, "Slow item selection processing: {0}ms for item ID {1}",
                    new Object[]{durationMs, stockDto != null ? stockDto.getId() : "null"});
            } else {
                logger.log(Level.FINE, "Item selection completed in {0}ms for item ID {1}",
                    new Object[]{durationMs, stockDto != null ? stockDto.getId() : "null"});
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during item selection", e);
        }
    }

    public void showItemDetailsForSelectedStock() {
        try {
            if (stockDto == null) {
                JsfUtil.addErrorMessage("Please select a stock first");
                return;
            }

            Stock selectedStock = convertStockDtoToEntity(stockDto);
            if (selectedStock == null || selectedStock.getItemBatch() == null || selectedStock.getItemBatch().getItem() == null) {
                JsfUtil.addErrorMessage("Selected stock does not have valid item information");
                return;
            }

            Item selectedItem = selectedStock.getItemBatch().getItem();
            Long itemId = selectedItem.getId();

            // Construct the URL with the item ID parameter
            String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
            String popupUrl = contextPath + "/faces/pharmacy/pharmacy_item_transactions_popup.xhtml?itemId=" + itemId;

            // Execute JavaScript to open the popup
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Opening item details for: " + selectedItem.getName(), null));

            PrimeFaces.current().executeScript("window.open('" + popupUrl + "', '_blank');");

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error opening item details: " + e.getMessage());
        }
    }

    //    public void calculateRatesOfSelectedBillItemBeforeAddingToTheList(BillItem bi) {
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
        long calculateBillItemStart = System.currentTimeMillis();
        System.out.println("=== CASHIER calculateBillItem START - Time: " + calculateBillItemStart + " ===");
        System.out.println("calculateBillItem: JPA warm-up status: " + (jpaWarmUpCompleted ? "COMPLETED" : "PENDING/IN-PROGRESS"));

        if (stockDto == null) {
            System.out.println("calculateBillItem: stockDto is null, returning early");
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

        // Convert StockDTO to Stock entity if not already set
        long beforeStockConvert = System.currentTimeMillis();
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            System.out.println("calculateBillItem: Converting stockDto to entity...");
            Stock stockEntity = convertStockDtoToEntity(stockDto);
            if (stockEntity != null) {
                getBillItem().getPharmaceuticalBillItem().setStock(stockEntity);
            }
            long afterStockConvert = System.currentTimeMillis();
            System.out.println("calculateBillItem: Stock conversion took " + (afterStockConvert - beforeStockConvert) + "ms");
        } else {
            System.out.println("calculateBillItem: Stock already set, skipping conversion");
        }

        if (getQty() == null) {
            qty = 0.0;
        }
        if (stockDto.getStockQty() != null && getQty() > stockDto.getStockQty()) {
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        //Bill Item
//        billItem.setInwardChargeType(InwardChargeType.Medicine);
        // PERFORMANCE FIX: Use DTO fields and entity proxies to avoid database query
        // Previously: stockEntity.getItemBatch() triggered database load defeating getReference() optimization
        // Now: Use proxy references and DTO data directly - zero database queries
        long beforeEntityRefs = System.currentTimeMillis();

        if (stockDto.getItemId() != null) {
            System.out.println("calculateBillItem: Getting item reference for ID: " + stockDto.getItemId());
            long beforeItem = System.currentTimeMillis();
            billItem.setItem(itemFacade.getReference(stockDto.getItemId()));
            long afterItem = System.currentTimeMillis();
            System.out.println("calculateBillItem: Item reference took " + (afterItem - beforeItem) + "ms");
        }

        if (stockDto.getDateOfExpire() != null) {
            billItem.getPharmaceuticalBillItem().setDoe(stockDto.getDateOfExpire());
        }

        if (stockDto.getItemBatchId() != null) {
            System.out.println("calculateBillItem: Getting itemBatch reference for ID: " + stockDto.getItemBatchId());
            System.out.println("calculateBillItem: This is the KNOWN BOTTLENECK operation. Warm-up status: " + (jpaWarmUpCompleted ? "COMPLETED - Should be fast" : "PENDING - May be slow"));
            long beforeBatch = System.currentTimeMillis();
            billItem.getPharmaceuticalBillItem().setItemBatch(itemBatchFacade.getReference(stockDto.getItemBatchId()));
            long afterBatch = System.currentTimeMillis();
            long batchTime = afterBatch - beforeBatch;
            System.out.println("calculateBillItem: ItemBatch reference took " + batchTime + "ms" +
                (batchTime > 1000 ? " *** SLOW - JPA not warmed up ***" : " *** FAST - JPA warmed up successfully ***"));
        }

        long afterEntityRefs = System.currentTimeMillis();
        System.out.println("calculateBillItem: Total entity references took " + (afterEntityRefs - beforeEntityRefs) + "ms");

        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);

        //Rates - Calculate rate and discount based on current payment scheme
        if (stockDto.getRetailRate() != null) {
            billItem.setRate(stockDto.getRetailRate());
        }

        // Calculate discount rate based on current payment scheme
        long beforeDiscount = System.currentTimeMillis();
        System.out.println("calculateBillItem: Calculating discount rate...");
        billItem.setDiscountRate(calculateBillItemDiscountRate(billItem));
        long afterDiscount = System.currentTimeMillis();
        System.out.println("calculateBillItem: Discount calculation took " + (afterDiscount - beforeDiscount) + "ms");

        // Calculate net rate after applying discount
        billItem.setNetRate(billItem.getRate() - billItem.getDiscountRate());

        //Values
        billItem.setGrossValue(billItem.getRate() * qty);
        billItem.setDiscount(billItem.getDiscountRate() * qty);
        billItem.setNetValue(billItem.getGrossValue() - billItem.getDiscount());

        long calculateBillItemEnd = System.currentTimeMillis();
        long calculateBillItemTime = calculateBillItemEnd - calculateBillItemStart;
        System.out.println("=== CASHIER calculateBillItem END - Time: " + calculateBillItemTime + "ms ===");

    }

    public void addBillItem() {
        long addBillItemStartTime = System.currentTimeMillis();
        System.out.println("=== CASHIER addBillItem CALLED ===");
        System.out.println("Add Button Click - Start Time: " + addBillItemStartTime);

        // Clear performance caches at start of new add operation
        clearPerformanceCaches();

        System.out.println("Stack trace to identify caller:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
            System.out.println("  " + stackTrace[i]);
        }
        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            if (patient != null && getBillItem() != null) {
                if (allergyListOfPatient == null) {
                    allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
                }
                String allergyMsg = pharmacyService.getAllergyMessageForPatient(patient, billItem, allergyListOfPatient);
                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    return;
                }
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Add quantity from multiple batches in pharmacy retail billing")) {
            addBillItemMultipleBatches();
        } else {
            addBillItemSingleItem();
        }
        calculateBillItemsAndBillTotalsOfPreBill();
        setActiveIndex(1);

        long addBillItemEndTime = System.currentTimeMillis();
        long totalTime = addBillItemEndTime - addBillItemStartTime;

        // Print performance summary
        System.out.println("=== CASHIER ADD BUTTON PERFORMANCE SUMMARY ===");
        System.out.println("Total Add Button Time: " + totalTime + "ms");
        System.out.println("Converter Calls: " + converterCallCount);
        System.out.println("Converter Cache Size: " + (converterCache != null ? converterCache.size() : 0));
        System.out.println("Discount Calculations: " + discountCalculationCount);
        System.out.println("Discount Cache Size: " + (discountCache != null ? discountCache.size() : 0));
        System.out.println("Add Button Click - End Time: " + addBillItemEndTime);
        System.out.println("=== END PERFORMANCE SUMMARY ===");
    }

    public void calculateBillItemsAndBillTotalsOfPreBill() {
        long calculateStartTime = System.currentTimeMillis();
        System.out.println("=== CASHIER calculateBillItemsAndBillTotalsOfPreBill START ===");
        System.out.println("Calculate totals - Start Time: " + calculateStartTime);

        calculateRatesForAllBillItemsInPreBill();
        calculatePreBillTotals();

        long calculateEndTime = System.currentTimeMillis();
        long calculateTotalTime = calculateEndTime - calculateStartTime;
        System.out.println("=== CASHIER calculateBillItemsAndBillTotalsOfPreBill TOTAL TIME: " + calculateTotalTime + "ms ===");
        System.out.println("Calculate totals - End Time: " + calculateEndTime);
    }

    public void calculateRatesForAllBillItemsInPreBill() {
        long ratesCalculationStart = System.currentTimeMillis();
        System.out.println("=== CASHIER calculateRatesForAllBillItemsInPreBill START - Time: " + ratesCalculationStart + " ===");
        System.out.println("Bill items to process: " + (getPreBill() != null && getPreBill().getBillItems() != null ? getPreBill().getBillItems().size() : 0));

        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRatesOfSelectedBillItemBeforeAddingToTheList(tbi);
//            calculateBillItemForEditing(tbi);
        }
        calculatePreBillTotals();

        long ratesCalculationEnd = System.currentTimeMillis();
        long ratesCalculationTime = ratesCalculationEnd - ratesCalculationStart;
        System.out.println("=== CASHIER calculateRatesForAllBillItemsInPreBill END - Time: " + ratesCalculationTime + "ms ===");
    }

    public void calculateRatesOfSelectedBillItemBeforeAddingToTheList(BillItem bi) {
        long startTime = System.currentTimeMillis();

        PharmaceuticalBillItem pharmBillItem = bi.getPharmaceuticalBillItem();
        if (pharmBillItem != null && pharmBillItem.getStock() != null) {
            ItemBatch itemBatch = pharmBillItem.getStock().getItemBatch();
            if (itemBatch != null) {
                bi.setRate(itemBatch.getRetailsaleRate());
            }

            long beforeDiscount = System.currentTimeMillis();
            bi.setDiscountRate(calculateBillItemDiscountRate(bi));

            long afterDiscount = System.currentTimeMillis();

            bi.setNetRate(bi.getRate() - bi.getDiscountRate());

            bi.setGrossValue(bi.getRate() * bi.getQty());
            bi.setDiscount(bi.getDiscountRate() * bi.getQty());
            bi.setNetValue(bi.getGrossValue() - bi.getDiscount());

        }

        long endTime = System.currentTimeMillis();
    }

    public void calculatePreBillTotals() {
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
        long singleItemStartTime = System.currentTimeMillis();
        System.out.println("=== CASHIER addBillItemSingleItem START ===");
        System.out.println("Single item processing - Start Time: " + singleItemStartTime);

        editingQty = null;
        errorMessage = null;
        double addedQty = 0.0;

        if (billItem == null) {
            System.out.println("Validation FAILED: billItem is null");
            return addedQty;
        }

        if (billItem.getPharmaceuticalBillItem() == null) {
            System.out.println("Validation FAILED: pharmaceuticalBillItem is null");
            return addedQty;
        }

        System.out.println("Checking stockDto - Current value: " + (stockDto != null ? "ID=" + stockDto.getId() : "NULL"));

        if (getStockDto() == null) {
            errorMessage = "Item ??";
            System.out.println("Validation FAILED: stockDto is NULL");
            JsfUtil.addErrorMessage("Please select an Item Batch to Dispense ??");
            return addedQty;
        }

        System.out.println("stockDto ID: " + getStockDto().getId());
        System.out.println("stockDto ItemName: " + getStockDto().getItemName());
        System.out.println("stockDto StockQty: " + getStockDto().getStockQty());
        System.out.println("stockDto RetailRate: " + getStockDto().getRetailRate());
        System.out.println("stockDto DateOfExpire: " + getStockDto().getDateOfExpire());

        if (getStockDto().getDateOfExpire() != null && getStockDto().getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
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
        if (getStockDto().getStockQty() == null) {
            errorMessage = "Stock quantity not available.";
            System.out.println("Validation FAILED: stockDto.stockQty is NULL");
            System.out.println("This indicates converter failed to preserve full DTO");
            JsfUtil.addErrorMessage("Stock quantity not available. Please select a valid stock.");
            return addedQty;
        }

        System.out.println("Validation: stockQty = " + getStockDto().getStockQty());
        System.out.println("Validation: requested qty = " + getQty());

        if (getQty() > getStockDto().getStockQty()) {
            errorMessage = "No sufficient stocks.";
            System.out.println("Validation FAILED: Insufficient stock. Available: " + getStockDto().getStockQty() + ", Requested: " + getQty());
            JsfUtil.addErrorMessage("Insufficient stock. Available: " + String.format("%.0f", getStockDto().getStockQty()) + ", Requested: " + String.format("%.0f", getQty()));
            return addedQty;
        }

        System.out.println("Validation PASSED: Stock quantity check successful");

        System.out.println("Checking if item batch already exists in bill...");
        boolean batchExists = checkItemBatch();
        System.out.println("checkItemBatch() returned: " + batchExists);

        if (batchExists) {
            errorMessage = "This batch is already there in the bill.";
            System.out.println("ERROR: Item batch already in bill - stockDto ID: " + getStockDto().getId());
            System.out.println("Current bill items count: " + (getPreBill() != null && getPreBill().getBillItems() != null ? getPreBill().getBillItems().size() : 0));
            JsfUtil.addErrorMessage("Already added this item batch");
            clearBillItem(); // Clear stale state to prevent confusion
            return addedQty;
        }

        System.out.println("Item batch check passed, proceeding to add item...");
//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            JsfUtil.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        //Checking User Stock Entity - COMMENTED OUT FOR PERFORMANCE
//        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
//            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
//            return addedQty;
//        }


        addedQty = qty;
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);

        // Convert StockDTO to Stock entity for persistence
        Stock stockEntity = convertStockDtoToEntity(stockDto);
        if (stockEntity != null) {
            billItem.getPharmaceuticalBillItem().setStock(stockEntity);
            billItem.getPharmaceuticalBillItem().setItemBatch(stockEntity.getItemBatch());
            billItem.setItem(stockEntity.getItemBatch().getItem());
        }

        calculateBillItem();
        ////System.out.println("Rate*****" + billItem.getRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setBill(getPreBill());

        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        System.out.println("SUCCESS: Adding item to bill - ID: " + getStockDto().getId() + ", Qty: " + qty);
        getPreBill().getBillItems().add(billItem);
        System.out.println("Total items in bill now: " + getPreBill().getBillItems().size());

        // UserStock operations commented out for performance
        //if (getUserStockContainer().getId() == null) {
        //    saveUserStockContainer();
        //}
        //UserStock us = saveUserStock(billItem);
        //billItem.setTransUserStock(us);
        // Add instructions only if enabled (default: false for performance)
        if (configOptionApplicationController.getBooleanValueByKey("Add bill item instructions in pharmacy cashier sale", false)) {
            pharmacyService.addBillItemInstructions(billItem);
        }

        clearBillItem();
        getBillItem();

        long singleItemEndTime = System.currentTimeMillis();
        long singleItemTotalTime = singleItemEndTime - singleItemStartTime;
        System.out.println("=== CASHIER addBillItemSingleItem TOTAL TIME: " + singleItemTotalTime + "ms ===");
        System.out.println("Single item processing - End Time: " + singleItemEndTime);

        return addedQty;
    }

    public void addBillItemMultipleBatches() {
        System.out.println("=== CASHIER addBillItemMultipleBatches START ===");
        System.out.println("WARNING: Multiple batches mode is active!");
        editingQty = null;
        errorMessage = null;

        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getStockDto() == null) {
            errorMessage = "Please select an Item Batch to Dispense?";
            JsfUtil.addErrorMessage("Please select an Item Batch to Dispense?");
            return;
        }
        StockDTO userSelectedStockDto = stockDto;
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
//        if (getStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
//            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
//            return;
//        }
        if (getQty() == null) {
            errorMessage = "Please enter a Quantity";
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }
        if (getQty() == 0.0) {
            errorMessage = "Please enter a Quantity";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return;
        }

        double requestedQty = getQty();
        double addedQty = 0.0;
        double remainingQty = getQty();

        if (getStockDto().getStockQty() == null) {
            errorMessage = "Stock quantity not available.";
            JsfUtil.addErrorMessage("Stock quantity not available. Please select a valid stock.");
            return;
        }

        if (getQty() <= getStockDto().getStockQty()) {
            double thisTimeAddingQty = addBillItemSingleItem();
            if (thisTimeAddingQty >= requestedQty) {
                return;
            } else {
                addedQty += thisTimeAddingQty;
                remainingQty = remainingQty - thisTimeAddingQty;
            }
        } else {
            qty = getStockDto().getStockQty();
            double thisTimeAddingQty = addBillItemSingleItem();
            addedQty += thisTimeAddingQty;
            remainingQty = remainingQty - thisTimeAddingQty;
        }

//        addedQty = addBillItemSingleItem();
//        System.out.println("stock = " + userSelectedStockDto);
//        System.out.println("stock item batch = " + userSelectedStockDto.getItemBatch());
//        System.out.println("stock item batch item= " + userSelectedStockDto.getItemBatch().getItem());
        // Convert DTO to entity for finding next available stocks (multiple batches feature)
        Stock userSelectedStock = convertStockDtoToEntity(userSelectedStockDto);
        if (userSelectedStock == null) {
            JsfUtil.addErrorMessage("Unable to process stock information");
            return;
        }

        List<Stock> availableStocks = stockController.findNextAvailableStocks(userSelectedStock);
        for (Stock s : availableStocks) {
            stock = s;
            if (remainingQty < s.getStock()) {
                qty = remainingQty;
            } else {
                qty = s.getStock();
            }
            double thisTimeAddingQty = addBillItemSingleItem();
            addedQty += thisTimeAddingQty;
            remainingQty = remainingQty - thisTimeAddingQty;

            if (remainingQty <= 0) {
                return;
            }
        }
        if (addedQty < requestedQty) {
            errorMessage = "Quantity is not Enough...!";
            System.out.println("=== MULTIPLE BATCHES: Insufficient quantity ===");
            System.out.println("Requested: " + requestedQty + ", Added: " + addedQty);
            System.out.println("This error should only appear when using multiple batches mode");
            JsfUtil.addErrorMessage("Only " + String.format("%.0f", addedQty) + " is Available form the Requested Quantity");
        }

        System.out.println("=== CASHIER addBillItemMultipleBatches END ===");
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
        // UserStock operations commented out for performance
        //if (getUserStockContainer().getId() == null) {
        //    saveUserStockContainer();
        //}
        //UserStock us = saveUserStock(billItem);
        //billItem.setTransUserStock(us);
    }

    private void addMultipleStock() {
        Double remainingQty = Math.abs(qty) - Math.abs(getStock().getStock());
        addSingleStock();
        List<Stock> availableStocks = stockController.findNextAvailableStocks(getStock());

    }

    // COMMENTED OUT FOR PERFORMANCE
    //private void saveUserStockContainer() {
    //    userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
    //
    //    getUserStockContainer().setCreater(getSessionController().getLoggedUser());
    //    getUserStockContainer().setCreatedAt(new Date());
    //
    //    getUserStockContainerFacade().create(getUserStockContainer());
    //
    //}
    // COMMENTED OUT FOR PERFORMANCE
    //private UserStock saveUserStock(BillItem tbi) {
    //    UserStock us = new UserStock();
    //    us.setStock(tbi.getPharmaceuticalBillItem().getStock());
    //    us.setUpdationQty(tbi.getQty());
    //    us.setCreater(getSessionController().getLoggedUser());
    //    us.setCreatedAt(new Date());
    //    us.setUserStockContainer(getUserStockContainer());
    //    getUserStockFacade().create(us);
    //
    //    getUserStockContainer().getUserStocks().add(us);
    //
    //    return us;
    //}
    //    public void calculateAllRatesNew() {
//        ////////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateRatesOfSelectedBillItemBeforeAddingToTheList(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calculateBillItemsAndBillTotalsOfPreBill();
//    }
    //    Checked
    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
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

    private void savePreBill(Patient pt) {
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

        // Set balance and paidAmount based on payment method
        // For Credit and Staff credit payment methods, balance is netTotal and paidAmount is zero
        // For all other payment methods including MultiplePaymentMethods, balance is zero and paidAmount is netTotal
        if (getPaymentMethod() == PaymentMethod.Credit || getPaymentMethod() == PaymentMethod.Staff) {
            getPreBill().setBalance(getPreBill().getNetTotal());
            getPreBill().setPaidAmount(0.0);
        } else {
            getPreBill().setBalance(0.0);
            getPreBill().setPaidAmount(getPreBill().getNetTotal());
        }

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
        } else {
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department to avoid consuming counter twice
            } else {
                // Use existing method for backward compatibility
                insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
            }
        }
        getPreBill().setInsId(insId);
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

        // Set balance and paidAmount based on payment method
        // For Credit and Staff credit payment methods, balance is netTotal and paidAmount is zero
        // For all other payment methods including MultiplePaymentMethods, balance is zero and paidAmount is netTotal
        if (getPaymentMethod() == PaymentMethod.Credit || getPaymentMethod() == PaymentMethod.Staff) {
            getPreBill().setBalance(getPreBill().getNetTotal());
            getPreBill().setPaidAmount(0.0);
        } else {
            getPreBill().setBalance(0.0);
            getPreBill().setPaidAmount(getPreBill().getNetTotal());
        }

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setPaymentMethod(getPaymentMethod());
        getPreBill().setPaymentScheme(getPaymentScheme());

        getBillBean().setPaymentMethodData(getPreBill(), getPaymentMethod(), getPaymentMethodData());

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().departmentBillNumberGenerator(getPreBill().getDepartment(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
        }

        // Handle Institution ID generation
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else {
            // Check if department strategy is enabled
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department to avoid consuming counter twice
            } else {
                // Use existing method for backward compatibility
                insId = getBillNumberBean().institutionBillNumberGenerator(getPreBill().getInstitution(), getPreBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.SALE);
            }
        }
        getPreBill().setInsId(insId);
        getPreBill().setDeptId(deptId);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        getPreBill().setInvoiceNumber(billNumberBean.fetchPaymentSchemeCount(getPreBill().getPaymentScheme(), getPreBill().getBillType(), getPreBill().getInstitution()));

        getBillFacade().edit(getPreBill());
    }

    private void saveSaleBill() {
        //  calculateRatesForAllBillItemsInPreBill();

        getSaleBill().copy(getPreBill());
        getSaleBill().copyValue(getPreBill());

        getSaleBill().setBillType(BillType.PharmacySale);
        getSaleBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getInstitution());
        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setReferenceBill(getPreBill());

        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());
        getSaleBill().setInvoiceNumber(getPreBill().getInvoiceNumber());
        getSaleBill().setComments(comment);

        getSaleBill().setCashPaid(cashPaid);

        // Set balance and paidAmount based on payment method
        // For Credit and Staff credit payment methods, balance is netTotal and paidAmount is zero
        // For all other payment methods including MultiplePaymentMethods, balance is zero and paidAmount is netTotal
        if (getPaymentMethod() == PaymentMethod.Credit || getPaymentMethod() == PaymentMethod.Staff) {
            getSaleBill().setBalance(getSaleBill().getNetTotal());
            getSaleBill().setPaidAmount(0.0);
        } else {
            getSaleBill().setBalance(0.0);
            getSaleBill().setPaidAmount(getSaleBill().getNetTotal());
        }

        getBillBean().setPaymentMethodData(getSaleBill(), getSaleBill().getPaymentMethod(), getPaymentMethodData());
        if (getSaleBill().getId() == null) {
            getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
            getSaleBill().setCreater(getSessionController().getLoggedUser());
            getBillFacade().create(getSaleBill());
        } else {
            getBillFacade().edit(getSaleBill());
        }

        updatePreBill();
    }
//

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());
    }

    private void savePreBillItems() {
        for (BillItem tbi : getPreBill().getBillItems()) {
//            if (execureOnEditActions(tbi)) {
////If any issue in Stock Bill Item will not save & not include for total
////                continue;
//            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            if (tbi.getId() == null) {
                tbi.setCreatedAt(new Date());
                tbi.setCreater(sessionController.getLoggedUser());
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }
            double qtyL = tbi.getPharmaceuticalBillItem().getQty() + tbi.getPharmaceuticalBillItem().getFreeQty();
            //Deduct Stock
            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());
            if (!returnFlag) {
                // Store original item data before modification for audit
                BillItem originalBillItem = new BillItem();
                originalBillItem.setQty(tbi.getQty());
                originalBillItem.setItem(tbi.getItem());
                originalBillItem.setRate(tbi.getRate());
                originalBillItem.setNetValue(tbi.getNetValue());

                tbi.setQty(0.0);
                // Keep PharmaceuticalBillItem quantities in sync for consistency
                if (tbi.getPharmaceuticalBillItem() != null) {
                    tbi.getPharmaceuticalBillItem().setQty(0.0);
                    tbi.getPharmaceuticalBillItem().setFreeQty(0.0f);
                }
                JsfUtil.addErrorMessage("Error. Please check the bill again manually. When stock was recording, there was no sufficient stocks for the item : " + tbi.getItem().getName());

                // Record audit event for insufficient stock
                auditService.logAudit(
                        originalBillItem,
                        tbi,
                        sessionController.getLoggedUser(),
                        "BillItem",
                        "INSUFFICIENT_STOCK_QUANTITY_RESET"
                );

//                getBillItemFacade().edit(tbi);
            }
//            getPreBill().getBillItems().add(tbi);
        }

        //userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser()); // Commented out for performance
        calculateRatesForAllBillItemsInPreBill();

    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        // Initialize the billItems collection if it was set to null
        if (getPreBill().getBillItems() == null) {
            getPreBill().setBillItems(new ArrayList<>());
        }

        for (BillItem tbi : list) {
            if (execureOnEditActions(tbi)) {
//If any issue in Stock Bill Item will not save & not include for total
//                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            if (tbi.getId() == null) {
                tbi.setCreatedAt(new Date());
                tbi.setCreater(sessionController.getLoggedUser());
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }
            double qtyL = tbi.getPharmaceuticalBillItem().getQty() + tbi.getPharmaceuticalBillItem().getFreeQty();
            //Deduct Stock
            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());
            if (!returnFlag) {
                // Store original item data before modification for audit
                BillItem originalBillItem = new BillItem();
                originalBillItem.setQty(tbi.getQty());
                originalBillItem.setItem(tbi.getItem());
                originalBillItem.setRate(tbi.getRate());
                originalBillItem.setNetValue(tbi.getNetValue());

                tbi.setQty(0.0);
                // Keep PharmaceuticalBillItem quantities in sync for consistency
                if (tbi.getPharmaceuticalBillItem() != null) {
                    tbi.getPharmaceuticalBillItem().setQty(0.0);
                    tbi.getPharmaceuticalBillItem().setFreeQty(0.0f);
                }
                JsfUtil.addErrorMessage("Error. Please check the bill again manually. When stock was recording, there was no sufficient stocks for the item : " + tbi.getItem().getName());

                // Record audit event for insufficient stock
                auditService.logAudit(
                        originalBillItem,
                        tbi,
                        sessionController.getLoggedUser(),
                        "BillItem",
                        "INSUFFICIENT_STOCK_QUANTITY_RESET"
                );

                getBillItemFacade().edit(tbi);
            }
            getPreBill().getBillItems().add(tbi);
        }

        //userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser()); // Commented out for performance
        calculateRatesForAllBillItemsInPreBill();

    }

    /**
     * Save Pre-Bill Items Finally with Locked Stocks
     *
     * Enhanced version of savePreBillItemsFinally() that uses pre-locked and validated stocks
     * from StockLockingService. This prevents stock deduction failures since stocks were
     * already validated before settlement began.
     *
     * Key Differences from savePreBillItemsFinally():
     * - Uses pre-locked stocks from stockValidation result
     * - Calls stockLockingService.deductAndReleaseLock() instead of pharmacyBean.deductFromStock()
     * - Should not encounter insufficient stock errors (since pre-validated)
     * - Releases each lock immediately after deduction
     *
     * @param list List of bill items to save
     * @param lockedStocks Map of locked stocks (stockId -> Stock entity) from validation
     */
    private void savePreBillItemsFinallyWithLockedStocks(List<BillItem> list, Map<Long, Stock> lockedStocks) {
        // Initialize the billItems collection if it was set to null
        if (getPreBill().getBillItems() == null) {
            getPreBill().setBillItems(new ArrayList<>());
        }

        for (BillItem tbi : list) {
            if (execureOnEditActions(tbi)) {
                // If any issue in Stock Bill Item will not save & not include for total
                // continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            if (tbi.getId() == null) {
                tbi.setCreatedAt(new Date());
                tbi.setCreater(sessionController.getLoggedUser());
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }

            // Get the pre-locked stock for this item
            PharmaceuticalBillItem pbi = tbi.getPharmaceuticalBillItem();
            if (pbi == null || pbi.getStock() == null || pbi.getStock().getId() == null) {
                logger.log(Level.WARNING, "Bill item has no valid stock reference: {0}", tbi.getId());
                continue;
            }

            Stock lockedStock = lockedStocks.get(pbi.getStock().getId());
            if (lockedStock == null) {
                logger.log(Level.SEVERE, "No locked stock found for bill item {0}, stock ID {1}",
                        new Object[]{tbi.getId(), pbi.getStock().getId()});
                // This should not happen since we pre-validated all stocks
                throw new RuntimeException("Internal error: No locked stock found for pre-validated item: " + tbi.getItem().getName());
            }

            double qtyL = pbi.getQty() + pbi.getFreeQty();

            // Deduct from locked stock and release lock
            boolean returnFlag = stockLockingService.deductAndReleaseLock(
                    lockedStock,
                    Math.abs(qtyL),
                    pbi,
                    getPreBill().getDepartment()
            );

            if (!returnFlag) {
                // This should be extremely rare since we pre-validated stocks
                // But handle it gracefully with audit trail
                BillItem originalBillItem = new BillItem();
                originalBillItem.setQty(tbi.getQty());
                originalBillItem.setItem(tbi.getItem());
                originalBillItem.setRate(tbi.getRate());
                originalBillItem.setNetValue(tbi.getNetValue());

                tbi.setQty(0.0);
                // Keep PharmaceuticalBillItem quantities in sync for consistency
                if (tbi.getPharmaceuticalBillItem() != null) {
                    tbi.getPharmaceuticalBillItem().setQty(0.0);
                    tbi.getPharmaceuticalBillItem().setFreeQty(0.0f);
                }

                String errorMsg = "Critical error: Stock deduction failed for pre-validated item: " + tbi.getItem().getName();
                JsfUtil.addErrorMessage(errorMsg);
                logger.log(Level.SEVERE, errorMsg);

                // Record audit event for this critical failure
                auditService.logAudit(
                        originalBillItem,
                        tbi,
                        sessionController.getLoggedUser(),
                        "BillItem",
                        "PRE_VALIDATED_STOCK_DEDUCTION_FAILED"
                );

                getBillItemFacade().edit(tbi);
            }

            getPreBill().getBillItems().add(tbi);
        }

        //userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser()); // Commented out for performance
        calculateRatesForAllBillItemsInPreBill();
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
        if (paymentMethodData != null || toStaff != null) {
            getPaymentMethodData().getStaffCredit().setToStaff(toStaff);
            getPaymentMethodData().getStaffWelfare().setToStaff(toStaff);
        }
    }

    private void syncStaffSelectionFromPaymentDetails(PaymentMethod method) {
        if (method != PaymentMethod.Staff && method != PaymentMethod.Staff_Welfare) {
            return;
        }
        if (paymentMethodData == null) {
            return;
        }
        if (toStaff != null) {
            return;
        }
        ComponentDetail staffComponent = method == PaymentMethod.Staff
                ? paymentMethodData.getStaffCredit()
                : paymentMethodData.getStaffWelfare();
        if (staffComponent != null && staffComponent.getToStaff() != null) {
            setToStaff(staffComponent.getToStaff());
        }
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    private void saveSaleBillItems() {
        for (BillItem preBillItem : getPreBill().getBillItems()) {

            BillItem newlyCreatedBillItemForSaleBill = new BillItem();

            newlyCreatedBillItemForSaleBill.copy(preBillItem);
            newlyCreatedBillItemForSaleBill.setReferanceBillItem(preBillItem);
            newlyCreatedBillItemForSaleBill.setBill(getSaleBill());
            newlyCreatedBillItemForSaleBill.setInwardChargeType(InwardChargeType.Medicine);
            //      newBil.setBill(getSaleBill());
            newlyCreatedBillItemForSaleBill.setCreatedAt(Calendar.getInstance().getTime());
            newlyCreatedBillItemForSaleBill.setCreater(getSessionController().getLoggedUser());

            if (preBillItem.getPrescription() != null) {
                Prescription newlyCreatedPrescreptionForSaleBillItem = preBillItem.getPrescription().cloneForNewEntity();
                newlyCreatedBillItemForSaleBill.setPrescription(newlyCreatedPrescreptionForSaleBillItem);
                newlyCreatedPrescreptionForSaleBillItem.setPatient(patient);
                newlyCreatedPrescreptionForSaleBillItem.setCreatedAt(new Date());
                newlyCreatedPrescreptionForSaleBillItem.setCreater(sessionController.getWebUser());
                newlyCreatedPrescreptionForSaleBillItem.setInstitution(sessionController.getInstitution());
                newlyCreatedPrescreptionForSaleBillItem.setDepartment(sessionController.getDepartment());
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(preBillItem.getPharmaceuticalBillItem());
            newPhar.setBillItem(newlyCreatedBillItemForSaleBill);
            newlyCreatedBillItemForSaleBill.setPharmaceuticalBillItem(newPhar);

            getBillItemFacade().create(newlyCreatedBillItemForSaleBill);
            getSaleBill().getBillItems().add(newlyCreatedBillItemForSaleBill);

            preBillItem.setReferanceBillItem(newlyCreatedBillItemForSaleBill);
        }
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
                newBil.setPrescription(tbi.getPrescription());
                tbi.getPrescription().setPatient(patient);
                tbi.getPrescription().setCreatedAt(new Date());
                tbi.getPrescription().setCreater(sessionController.getWebUser());
                tbi.getPrescription().setInstitution(sessionController.getInstitution());
                tbi.getPrescription().setDepartment(sessionController.getDepartment());
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

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

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);

            getSaleBill().getBillItems().add(newBil);

            tbi.setReferanceBillItem(newBil);
            getBillItemFacade().edit(tbi);
            saveBillFee(newBil, p);
        }

        getBillFacade().edit(getSaleBill());
    }

    private boolean checkAllBillItem() {
        for (BillItem b : getPreBill().getBillItems()) {

            if (execureOnEditActions(b)) {
                return true;
            }
        }

        return false;

    }

    public void settlePharmacyToken(TokenType tokenType) {
        currentToken = new Token();
        currentToken.setTokenType(tokenType);
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
        if (getPatient().getId() == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        } else if (getPatient().getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        } else if (getPatient().getPerson().getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a patient");
            return;
        } else {
            Patient pt = savePatient();
            currentToken.setPatient(pt);
        }
        if (currentToken.getToDepartment() == null) {
            currentToken.setToDepartment(sessionController.getDepartment());
        }
        if (currentToken.getToInstitution() == null) {
            currentToken.setToInstitution(sessionController.getInstitution());
        }
        tokenFacade.create(currentToken);
        currentToken.setTokenNumber(billNumberBean.generateDailyTokenNumber(currentToken.getFromDepartment(), null, null, tokenType));
        currentToken.setCounter(counter);
        currentToken.setTokenDate(new Date());
        currentToken.setTokenAt(new Date());
        currentToken.setBill(getPreBill());
        tokenFacade.edit(currentToken);
        setToken(currentToken);
    }

    public String settlePreBillAndNavigateToPrint() {
        editingQty = null;
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return null;
        }
        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                // COMMENTED OUT FOR PERFORMANCE
                //if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                //    setZeroToQty(bi);
                //    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                //    return null;
                //}
                if (bi.getQty() <= 0.0) {
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    return null;
                }
            }
        }
        if (getPreBill().isCancelled() == true) {
            getPreBill().setCancelled(false);
        }
        if (getPaymentMethod() == null) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please select Payment Method");
            return null;
        }

        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme);
        if (!discountSchemeValidation.isFlag()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return null;
        }
        // Pharmacy Sale Validation - Patient and Patient Details
        boolean patientRequired = configOptionApplicationController.getBooleanValueByKey("Patient is required in Pharmacy Retail Sale", false);
        boolean patientRequiredForPharmacySale = patientRequired; // Keep for backward compatibility with code below

        if (patientRequired) {
            if (getPatient() == null || getPatient().getPerson() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Patient is required.");
                return null;
            }
        }

        // Only validate patient details if patient is required OR if patient exists
        boolean hasPatient = getPatient() != null && getPatient().getPerson() != null;

        if (hasPatient || patientRequired) {
            // Patient Name validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Name is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getName() == null
                        || getPatient().getPerson().getName().trim().isEmpty()) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient name is required.");
                    return null;
                }
            }

            // Patient Phone validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Phone is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient is required.");
                    return null;
                }
                // Check both phone and mobile - at least one should be present
                boolean hasPhone = getPatient().getPerson().getPhone() != null
                        && !getPatient().getPerson().getPhone().trim().isEmpty();
                boolean hasMobile = getPatient().getPerson().getMobile() != null
                        && !getPatient().getPerson().getMobile().trim().isEmpty();

                if (!hasPhone && !hasMobile) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient phone number is required.");
                    return null;
                }
            }

            // Patient Gender validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Gender is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getSex() == null) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient gender is required.");
                    return null;
                }
            }

            // Patient Address validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Address is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getAddress() == null
                        || getPatient().getPerson().getAddress().trim().isEmpty()) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient address is required.");
                    return null;
                }
            }

            // Patient Area validation
            if (configOptionApplicationController.getBooleanValueByKey("Patient Area is required in Pharmacy Retail Sale", false)) {
                if (getPatient() == null || getPatient().getPerson() == null
                        || getPatient().getPerson().getArea() == null) {
                    billSettlingStarted = false;
                    JsfUtil.addErrorMessage("Patient area is required.");
                    return null;
                }
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management in the system", false)
                && configOptionApplicationController.getBooleanValueByKey("Enable blacklist patient management for Pharmacy from the system", false)) {
            if (getPatient().isBlacklisted()) {
                JsfUtil.addErrorMessage("This patient is blacklisted from the system. Can't Bill.");
                billSettlingStarted = false;
                return null;
            }
        }

        // Referring Doctor validation
        if (configOptionApplicationController.getBooleanValueByKey("Referring Doctor is required in Pharmacy Retail Sale", false)) {
            if (getPreBill() == null || getPreBill().getReferredBy() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Referring doctor is required.");
                return null;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Patient Phone number is mandotary in sale for cashier", true)) {
            if (getPatient().getPatientPhoneNumber() == null && getPatient().getPatientMobileNumber() == null) {
                JsfUtil.addErrorMessage("Please enter phone number of the patient");
                return null;
            } else if (getPatient().getId() == null) {
                if (getPatient().getPatientPhoneNumber() != null && !(String.valueOf(getPatient().getPatientPhoneNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid phone number with more than or equal 10 digits of the patient");
                    return null;
                } else if (getPatient().getPatientMobileNumber() != null && !(String.valueOf(getPatient().getPatientMobileNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid mobile number with more than or equal 10 digits of the patient");
                    return null;
                }
            }
        }

        // STOCK-FIRST SETTLEMENT: Validate and lock all stocks BEFORE any entity saves
        StockValidationResult stockValidation = stockLockingService.lockAndValidateStocks(
            getPreBill().getBillItems(),
            getSessionController().getLoggedUser(),
            getPreBill().getDepartment()
        );

        if (!stockValidation.isValid()) {
            // Display comprehensive error messages showing ALL stock shortfalls
            JsfUtil.addErrorMessage(stockValidation.getFormattedErrorMessage());
            billSettlingStarted = false;
            return null;
        }

        // Stock validation successful - proceed with settlement using locked stocks
        try {
            Patient pt = null;
            if (getPatient() != null && getPatient().getPerson() != null) {
                String name = getPatient().getPerson().getName();
                boolean hasValidName = name != null && !name.trim().isEmpty();

                if (patientRequiredForPharmacySale) {
                    if (!hasValidName) {
                        JsfUtil.addErrorMessage("Please Select a Patient");
                        billSettlingStarted = false;
                        return null;
                    } else {
                        pt = savePatient();
                    }
                } else {
                    if (hasValidName) {
                        pt = savePatient();
                    }
                }
            } else if (patientRequiredForPharmacySale) {
                JsfUtil.addErrorMessage("Please Select a Patient");
                billSettlingStarted = false;
                return null;
            }

            if (billPreview) {

            }

            calculateRatesForAllBillItemsInPreBill();

            List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());
            getPreBill().setBillItems(null);
            getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);

            savePreBillFinallyForRetailSaleForCashier(pt);
            // Use locked stocks for settlement instead of re-validating
            savePreBillItemsFinallyWithLockedStocks(tmpBillItems, stockValidation.getLockedStocks());
            setPrintBill(getPreBill());
            // Calculate and record costing values for stock valuation after persistence
            // Using current bill directly instead of reloading to avoid transaction timing issues
            // Calculate and record costing values for stock valuation after persistence
            // Using current bill directly instead of reloading to avoid transaction timing issues

            if (getPreBill().getBillItems() != null && !getPreBill().getBillItems().isEmpty()) {
                calculateAndRecordCostingValues(getPreBill());
            } else {
                Bill managedBill = loadBillWithPharmaceuticalItems(getPreBill().getId());
                if (managedBill != null) {
                    calculateAndRecordCostingValues(managedBill);
                    setPreBill((PreBill) managedBill);
                } else {
                }
            }

            if (configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false)) {
                if (getPatient() != null) {
                    Token t = tokenController.findPharmacyTokens(getPreBill());
                    if (t == null) {
                        Token saleForCashierToken = tokenController.findPharmacyTokenSaleForCashier(getPreBill(), TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                        if (saleForCashierToken == null) {
                            settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                        }
                        markInprogress();

                    } else if (t != null) {
                        markToken();
                    }
                }

            }

            if (getCurrentToken() != null) {
                getCurrentToken().setBill(getPreBill());
                tokenFacade.edit(getCurrentToken());
            }

            resetAll();
            billPreview = true;
            return navigateToSaleBillForCashierPrint();
        } catch (Exception e) {
            // Release all locks on any settlement failure
            stockLockingService.releaseLocks(stockValidation.getLockedStocks(),
                    "Settlement failed: " + e.getMessage());
            JsfUtil.addErrorMessage("Settlement failed. Please try again.");
            billSettlingStarted = false;
            logger.log(Level.SEVERE, "Error during bill settlement", e);
            return null;
        }
    }

    public String navigateToSaleBillForCashierPrint() {
        return "/pharmacy/printing/retail_sale_for_cashier?faces-redirect=true";
    }

    @Deprecated // Plse use settlePreBillAndNavigateToPrint
    public void settlePreBill() {
        editingQty = null;

        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return;
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                // COMMENTED OUT FOR PERFORMANCE
                //if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                //    setZeroToQty(bi);
                //    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                //    return;
                //}
                if (bi.getQty() <= 0.0) {
                    JsfUtil.addErrorMessage("Some BillItem Quntity is Zero or less than Zero");
                    return;
                }
            }
        }
        if (getPreBill().isCancelled() == true) {
            getPreBill().setCancelled(false);
        }

        if (getPaymentMethod() == null) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme);
        if (!discountSchemeValidation.isFlag()) {
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return;
        }

        boolean patientRequiredForPharmacySale = configOptionApplicationController.getBooleanValueByKey(
                "Patient is required in Pharmacy Retail Sale Bill for " + sessionController.getDepartment().getName(),
                false
        );

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select title.");
                return;
            }
            if (getPatient().getPerson().getSex() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select gender.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Name to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter name.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getDob() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient date of birth.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Phone Number to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getPhone() == null || getPatient().getPerson().getPhone().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter phone number.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Address to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getAddress() == null || getPatient().getPerson().getAddress().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient address.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Mail to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getEmail() == null || getPatient().getPerson().getEmail().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient email.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient NIC to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getNic() == null || getPatient().getPerson().getNic().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient NIC.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Area to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getArea() == null || getPatient().getPerson().getArea().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select patient area.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Referring Doctor to settlle bill in Pharmacy Sale", false)) {
            if (getPreBill().getReferredBy() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select referring doctor.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Patient Phone number is mandotary in sale for cashier", true)) {
            if (getPatient().getPatientPhoneNumber() == null && getPatient().getPatientMobileNumber() == null) {
                JsfUtil.addErrorMessage("Please enter phone number of the patient");
                return;
            } else if (getPatient().getId() == null) {
                if (getPatient().getPatientPhoneNumber() != null && !(String.valueOf(getPatient().getPatientPhoneNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid phone number with more than or equal 10 digits of the patient");
                    return;
                } else if (getPatient().getPatientMobileNumber() != null && !(String.valueOf(getPatient().getPatientMobileNumber()).length() >= 9)) {
                    JsfUtil.addErrorMessage("Please enter valid mobile number with more than or equal 10 digits of the patient");
                    return;
                }
            }
        }

        Patient pt = null;
        if (getPatient() != null && getPatient().getPerson() != null) {
            String name = getPatient().getPerson().getName();
            boolean hasValidName = name != null && !name.trim().isEmpty();

            if (patientRequiredForPharmacySale) {
                if (!hasValidName) {
                    JsfUtil.addErrorMessage("Please Select a Patient");
                    billSettlingStarted = false;
                    return;
                } else {
                    pt = savePatient();
                }
            } else {
                if (hasValidName) {
                    pt = savePatient();
                }
            }
        } else if (patientRequiredForPharmacySale) {
            JsfUtil.addErrorMessage("Please Select a Patient");
            billSettlingStarted = false;
            return;
        }

        if (billPreview) {

        }

        calculateRatesForAllBillItemsInPreBill();

        List<BillItem> tmpBillItems = new ArrayList<>(getPreBill().getBillItems());
        getPreBill().setBillItems(null);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);

        savePreBillFinallyForRetailSaleForCashier(pt);
        savePreBillItemsFinally(tmpBillItems);
        Long id = getPreBill().getId();
        if (id == null) {
            JsfUtil.addErrorMessage("Pre-bill is not persisted; cannot load for printing");
            return;
        }
//        setPrintBill(getBillFacade().find(id));

        if (configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false)) {

            if (getPatient() != null) {
                Token t = tokenController.findPharmacyTokens(getPreBill());
                if (t == null) {
                    Token saleForCashierToken = tokenController.findPharmacyTokenSaleForCashier(getPreBill(), TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                    if (saleForCashierToken == null) {
                        settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
                    }
                    markInprogress();

                } else if (t != null) {
                    markToken();
                }
            }

        }

        if (getCurrentToken() != null) {
            getCurrentToken().setBill(getPreBill());
            tokenFacade.edit(getCurrentToken());
        }

        resetAll();
        setPrintBill(billService.reloadBill(id));
        billPreview = true;
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

    public boolean errorCheckOnPaymentMethod() {
        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }

        syncStaffSelectionFromPaymentDetails(paymentMethod);

        if (paymentMethod == PaymentMethod.PatientDeposit) {
//            if (!getPatient().getHasAnAccount()) {
//                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
//                return true;
//            }
            double creditLimitAbsolute = 0.0;
//            if (getPatient().getCreditLimit() == null) {
//                creditLimitAbsolute = 0.0;
//            } else {
//                creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
//            }
//
            double runningBalance;
//            if (getPatient().getRunningBalance() != null) {
//                runningBalance = getPatient().getRunningBalance();
//            } else {
//                runningBalance = 0.0;
//            }
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());
            runningBalance = pd.getBalance();
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

//        if (paymentMethod == PaymentMethod.Credit) {
//            if (creditCompany == null && collectingCentre == null) {
//                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company or Collecting centre.");
//                return true;
//            }
//        }
        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }

            if (toStaff.getCurrentCreditValue() + netTotal > toStaff.getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + netTotal > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }

            // Clean up invalid ComponentDetails before validation
            cleanupInvalidPaymentDetails();

            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd.getPaymentMethod() != null && cd.getPaymentMethodData() != null) {
                    if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
                        double creditLimitAbsolute = 0.0;
                        PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());

                        if (pd == null) {
                            JsfUtil.addErrorMessage("No Patient Deposit.");
                            return true;
                        }

                        double runningBalance = pd.getBalance();
                        double availableForPurchase = runningBalance + creditLimitAbsolute;

                        if (cd.getPaymentMethodData().getPatient_deposit().getTotalValue() > availableForPurchase) {
                            JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                            return true;
                        }
                    }
                    if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                        if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                            JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                            return true;
                        }
                        Staff selectedStaff = cd.getPaymentMethodData().getStaffCredit().getToStaff();
                        if (selectedStaff.getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > selectedStaff.getCreditLimitQualified()) {
                            JsfUtil.addErrorMessage("No enough Credit.");
                            return true;
                        }
                    } else if (cd.getPaymentMethod().equals(PaymentMethod.Staff_Welfare)) {
                        if (cd.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffWelfare().getToStaff() == null) {
                            JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                            return true;
                        }
                        Staff welfareStaff = cd.getPaymentMethodData().getStaffWelfare().getToStaff();
                        double utilized = Math.abs(welfareStaff.getAnnualWelfareUtilized());
                        if (utilized + cd.getPaymentMethodData().getStaffWelfare().getTotalValue() > welfareStaff.getAnnualWelfareQualified()) {
                            JsfUtil.addErrorMessage("No enough credit.");
                            return true;
                        }
                    }
                    // Aggregate only the relevant payment method total
                    switch (cd.getPaymentMethod()) {
                        case Cash:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                            break;
                        case Card:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                            break;
                        case Cheque:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                            break;
                        case ewallet:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                            break;
                        case PatientDeposit:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                            break;
                        case Slip:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                            break;
                        case Staff:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                            break;
                        case Staff_Welfare:
                            multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                            break;
                        default:
                            break;
                    }
                }
            }
            double differenceOfBillTotalAndPaymentValue = netTotal - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
            if (cashPaid == 0.0) {
                setCashPaid(multiplePaymentMethodTotalValue);
            }

        }
        return false;
    }

    public List<Payment> createPaymentsForBill(Bill b) {
        return createMultiplePayments(b, b.getPaymentMethod());
    }

    public List<Payment> createMultiplePayments(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd.getPaymentMethodData() == null || cd.getPaymentMethod() == null) {
                    continue;
                }
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCreditCard().getComment());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setBank(cd.getPaymentMethodData().getCheque().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCheque().getComment());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setPolicyNo(cd.getPaymentMethodData().getEwallet().getReferralNo());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        p.setReferenceNo(cd.getPaymentMethodData().getEwallet().getReferenceNo());
                        p.setBank(cd.getPaymentMethodData().getEwallet().getInstitution());
                        break;
                    case Agent:
                    case PatientDeposit:
                        p.setPaidValue(cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        break;
                    case Credit:
                        p.setPolicyNo(cd.getPaymentMethodData().getCredit().getReferralNo());
                        p.setReferenceNo(cd.getPaymentMethodData().getCredit().getReferenceNo());
                        p.setCreditCompany(cd.getPaymentMethodData().getCredit().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getCredit().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCredit().getComment());
                        break;
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        p.setPaymentDate(cd.getPaymentMethodData().getSlip().getDate());
                        p.setComments(cd.getPaymentMethodData().getSlip().getComment());
                        p.setReferenceNo(cd.getPaymentMethodData().getSlip().getReferenceNo());
                        break;
                    case OnCall:
                        break;
                    case OnlineSettlement:
                        p.setPaidValue(cd.getPaymentMethodData().getOnlineSettlement().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getOnlineSettlement().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getOnlineSettlement().getDate());
                        p.setPaymentDate(cd.getPaymentMethodData().getOnlineSettlement().getDate());
                        p.setReferenceNo(cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo());
                        p.setComments(cd.getPaymentMethodData().getOnlineSettlement().getComment());
                        break;
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            p.setToStaff(cd.getPaymentMethodData().getStaffCredit().getToStaff());
                            // Set bill.toStaff from the first Staff payment component
                            if (bill.getToStaff() == null) {
                                bill.setToStaff(cd.getPaymentMethodData().getStaffCredit().getToStaff());
                            }
                        }
                        break;
                    case Staff_Welfare:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() != null) {
                            p.setToStaff(cd.getPaymentMethodData().getStaffWelfare().getToStaff());
                            // Set bill.toStaff from the first Staff_Welfare payment component
                            if (bill.getToStaff() == null) {
                                bill.setToStaff(cd.getPaymentMethodData().getStaffWelfare().getToStaff());
                            }
                        }
                        break;
                    case YouOweMe:
                    case MultiplePaymentMethods:
                        break;
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                    p.setComments(paymentMethodData.getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setBank(paymentMethodData.getCheque().getInstitution());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    p.setComments(paymentMethodData.getCheque().getComment());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:
                    p.setPaidValue(paymentMethodData.getEwallet().getTotalValue());
                    p.setPolicyNo(paymentMethodData.getEwallet().getReferralNo());
                    p.setComments(paymentMethodData.getEwallet().getComment());
                    p.setReferenceNo(paymentMethodData.getEwallet().getReferenceNo());
                    p.setBank(paymentMethodData.getEwallet().getInstitution());
                    break;
                case Credit:
                    p.setPolicyNo(paymentMethodData.getCredit().getReferralNo());
                    p.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getCredit().getInstitution());
                    p.setPaidValue(paymentMethodData.getCredit().getTotalValue());
                    p.setComments(paymentMethodData.getCredit().getComment());
                    getSaleBill().setToInstitution(paymentMethodData.getCredit().getInstitution());
                    getSaleBill().setCreditCompany(paymentMethodData.getCredit().getInstitution());
                    break;
                case Agent:
                case PatientDeposit:
                    p.setPaidValue(paymentMethodData.getPatient_deposit().getTotalValue());
                    break;
                case Slip:
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    p.setPaymentDate(paymentMethodData.getSlip().getDate());
                    p.setComments(paymentMethodData.getSlip().getComment());
                    p.setReferenceNo(paymentMethodData.getSlip().getReferenceNo());
                    break;
                case OnCall:
                    break;
                case OnlineSettlement:
                    p.setPaidValue(paymentMethodData.getOnlineSettlement().getTotalValue());
                    p.setBank(paymentMethodData.getOnlineSettlement().getInstitution());
                    p.setRealizedAt(paymentMethodData.getOnlineSettlement().getDate());
                    p.setPaymentDate(paymentMethodData.getOnlineSettlement().getDate());
                    p.setReferenceNo(paymentMethodData.getOnlineSettlement().getReferenceNo());
                    p.setComments(paymentMethodData.getOnlineSettlement().getComment());
                    break;
                case Staff:
                    p.setToStaff(paymentMethodData.getStaffCredit().getToStaff());
                    p.setPaidValue(paymentMethodData.getStaffCredit().getTotalValue());
                    break;
                case Staff_Welfare:
                    p.setToStaff(paymentMethodData.getStaffWelfare().getToStaff());
                    p.setPaidValue(paymentMethodData.getStaffWelfare().getTotalValue());
                    break;
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

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

        if (getPaymentMethod() == PaymentMethod.Staff_Welfare && configOptionApplicationController.getBooleanValueByKey("Pharmacy discount should be staff when select Staff_welfare as payment method", false)) {
            if (paymentScheme == null || !paymentScheme.getName().equalsIgnoreCase("staff")) {
                JsfUtil.addErrorMessage("Saff Welfare need to set staff discount scheme.");
                billSettlingStarted = false;
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

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select title.");
                return;
            }
            if (getPatient().getPerson().getSex() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select gender.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Name to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter name.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getDob() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient date of birth.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Phone Number to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getPhone() == null || getPatient().getPerson().getPhone().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter phone number.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Address to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getAddress() == null || getPatient().getPerson().getAddress().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient address.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Mail to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getEmail() == null || getPatient().getPerson().getEmail().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient email.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient NIC to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getNic() == null || getPatient().getPerson().getNic().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please enter patient NIC.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Area to save Patient in Pharmacy Sale", false)) {
            if (getPatient().getPerson().getArea() == null || getPatient().getPerson().getArea().getName().trim().isEmpty()) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select patient area.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Referring Doctor to settlle bill in Pharmacy Sale", false)) {
            if (getPreBill().getReferredBy() == null) {
                billSettlingStarted = false;
                JsfUtil.addErrorMessage("Please select referring doctor.");
                return;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            if (allergyListOfPatient == null) {
                allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
            }
            String allergyMsg = pharmacyService.isAllergyForPatient(patient, getPreBill().getBillItems(), allergyListOfPatient);
            if (!allergyMsg.isEmpty()) {
                JsfUtil.addErrorMessage(allergyMsg);
                billSettlingStarted = false;
                return;
            }
        }

        if (!getPreBill().getBillItems().isEmpty()) {
            for (BillItem bi : getPreBill().getBillItems()) {
                // COMMENTED OUT FOR PERFORMANCE
                //if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getQty(), getSessionController().getLoggedUser())) {
                //    setZeroToQty(bi);
////                    onEditCalculation(bi);
                //    JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
                //    billSettlingStarted = false;
                //    return;
                //}
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

        if (errorCheckForSaleBill()) {
            billSettlingStarted = false;
            return;
        }
        if (errorCheckOnPaymentMethod()) {
            billSettlingStarted = false;
            return;
        }

        Patient pt = savePatient();

        calculateRatesForAllBillItemsInPreBill();

        savePreBill(pt);
        savePreBillItems(); // Stocks are Updated here

        saveSaleBill();
        saveSaleBillItems();

        List<Payment> payments = createPaymentsForBill(getSaleBill());
        drawerController.updateDrawerForIns(payments);

        updateRetailSaleFinanceDetails(getSaleBill());

        updateAll(); // REQUIRED - finance detail entities may be detached

        setPrintBill(getBillFacade().find(getSaleBill().getId()));

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
            getStaffBean().updateStaffCredit(toStaff, netTotal);
            JsfUtil.addSuccessMessage("User Credit Updated");
        }

        paymentService.updateBalances(payments);

        resetAll();
        billSettlingStarted = false;
        billPreview = true;

    }

    /**
     * Updates BillFinanceDetails for retail sales by calculating from existing
     * bill items. This method does not alter existing calculation logic but
     * creates new specific calculation for retail sales.
     *
     * @param bill The retail sale bill to update
     */
    /**
     * Comprehensive method to update retail sale finance details for bill and
     * bill items. Combines functionality from
     * updateBillFinanceDetailsForRetailSale and calculateAndRecordCostingValues
     * with simplified retail-only calculations and proper cost rate handling.
     */
    public void updateRetailSaleFinanceDetails(Bill bill) {

        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            return;
        }

        // Initialize bill-level totals
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFreeQuantity = BigDecimal.ZERO;

        // Process each bill item
        int itemIndex = 0;
        for (BillItem billItem : bill.getBillItems()) {
            itemIndex++;

            if (billItem == null || billItem.isRetired()) {
                continue;
            }

            // Get pharmaceutical bill item for rate information
            PharmaceuticalBillItem pharmaItem = billItem.getPharmaceuticalBillItem();
            if (pharmaItem == null) {
                continue;
            }

            // Get quantities
            BigDecimal qty = BigDecimal.valueOf(billItem.getQty());
            BigDecimal freeQty = BigDecimal.valueOf(pharmaItem.getFreeQty());
            BigDecimal totalQty = qty.add(freeQty);

            // Get rates from pharmaceutical bill item
            BigDecimal retailRate = BigDecimal.valueOf(pharmaItem.getRetailRate());
            BigDecimal purchaseRate = BigDecimal.valueOf(pharmaItem.getPurchaseRate());
            BigDecimal wholesaleRate = BigDecimal.valueOf(pharmaItem.getWholesaleRate());

            // Get cost rate from item batch (correct approach) with fallback to purchase rate
            BigDecimal costRate = purchaseRate; // default fallback
            if (pharmaItem.getItemBatch() != null) {
                Double batchCostRate = pharmaItem.getItemBatch().getCostRate();
                if (batchCostRate != null && batchCostRate > 0) {
                    costRate = BigDecimal.valueOf(batchCostRate);
                } else {
                }
            } else {
            }

            // Get BillItemFinanceDetails (note: getBillItemFinanceDetails() auto-creates if null)
            BillItemFinanceDetails bifd = billItem.getBillItemFinanceDetails();

            // Calculate absolute quantity for calculations
            BigDecimal absQty = qty.abs();

            // UPDATE RATE FIELDS in BillItemFinanceDetails
            bifd.setLineNetRate(BigDecimal.valueOf(billItem.getNetRate()));
            bifd.setGrossRate(BigDecimal.valueOf(billItem.getRate()));
            bifd.setLineGrossRate(BigDecimal.valueOf(billItem.getRate()));
            bifd.setBillCostRate(BigDecimal.ZERO); // Set to 0 as per user requirement
            bifd.setTotalCostRate(costRate);
            bifd.setLineCostRate(costRate);
            bifd.setCostRate(costRate);
            bifd.setPurchaseRate(purchaseRate);
            bifd.setRetailSaleRate(retailRate);

            // UPDATE TOTAL FIELDS in BillItemFinanceDetails
            bifd.setLineGrossTotal(BigDecimal.valueOf(billItem.getGrossValue()));
            bifd.setGrossTotal(BigDecimal.valueOf(billItem.getGrossValue()));

            // SIMPLIFIED RETAIL SALE CALCULATIONS (positive values for retail sales)
            BigDecimal itemCostValue = costRate.multiply(absQty);
            BigDecimal itemRetailValue = retailRate.multiply(totalQty); // Include free quantity
            BigDecimal itemPurchaseValue = purchaseRate.multiply(totalQty); // Include free quantity

            // UPDATE COST VALUES in BillItemFinanceDetails
            bifd.setLineCost(itemCostValue);
            bifd.setBillCost(BigDecimal.ZERO); // Set to 0 as per user requirement
            bifd.setTotalCost(itemCostValue);

            // UPDATE VALUE FIELDS in BillItemFinanceDetails (for retail sales, use totalQty including free)
            bifd.setValueAtCostRate(costRate.multiply(totalQty));
            bifd.setValueAtPurchaseRate(purchaseRate.multiply(totalQty));
            bifd.setValueAtRetailRate(retailRate.multiply(totalQty));

            // UPDATE QUANTITIES in BillItemFinanceDetails
            bifd.setQuantity(qty);
            bifd.setQuantityByUnits(qty);

            // UPDATE PHARMACEUTICAL BILL ITEM VALUES
            pharmaItem.setCostRate(costRate.doubleValue());
            pharmaItem.setCostValue(itemCostValue.doubleValue());
            pharmaItem.setRetailValue(itemRetailValue.doubleValue());
            pharmaItem.setPurchaseValue(itemPurchaseValue.doubleValue());

            // Accumulate bill-level totals
            totalCostValue = totalCostValue.add(itemCostValue);
            totalPurchaseValue = totalPurchaseValue.add(itemPurchaseValue);
            totalRetailSaleValue = totalRetailSaleValue.add(itemRetailValue);
            totalQuantity = totalQuantity.add(qty);
            totalFreeQuantity = totalFreeQuantity.add(freeQty);

        }

        // UPDATE BILL-LEVEL FINANCE DETAILS (check if auto-creation happens here too)
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails();
            bfd.setBill(bill);
            bill.setBillFinanceDetails(bfd);
        } else {
        }

        // Set basic totals from bill
        bfd.setNetTotal(BigDecimal.valueOf(bill.getNetTotal()));
        bfd.setGrossTotal(BigDecimal.valueOf(bill.getTotal()));

        // Set calculated totals
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalRetailSaleValue(totalRetailSaleValue);
        bfd.setTotalQuantity(totalQuantity);
        bfd.setTotalFreeQuantity(totalFreeQuantity);

    }

    private void updateAll() {
        if (saleBill.getBillFinanceDetails() != null) {
        }

        for (BillItem pbi : preBill.getBillItems()) {
            billItemFacade.edit(pbi);
        }
        billFacade.edit(preBill);
        for (BillItem sbi : saleBill.getBillItems()) {
            billItemFacade.edit(sbi);
        }
        billFacade.edit(saleBill);

        if (saleBill.getBillFinanceDetails() != null) {
        }
    }

    public String newPharmacyRetailSale() {
        clearBill();
        clearBillItem();
        billPreview = false;
        setBillSettlingStarted(false);
        return "pharmacy_bill_retail_sale";
    }

    //    checked
    private boolean checkItemBatch() {
        // Null safety: if stockDto is not available, cannot perform check
        if (stockDto == null || stockDto.getId() == null) {
            System.out.println("WARNING: checkItemBatch called with null stockDto");
            return false; // Cannot determine duplication, assume not duplicate
        }

        System.out.println("Checking for duplicate batch - stockDto ID: " + stockDto.getId());

        // Compare stockDto ID (user's current selection) against existing bill items
        for (BillItem bItem : getPreBill().getBillItems()) {
            if (bItem.getPharmaceuticalBillItem() != null) {
                Stock existingStock = bItem.getPharmaceuticalBillItem().getStock();
                // Compare IDs directly - safe because Stock.equals() only compares IDs anyway
                if (existingStock != null && stockDto.getId().equals(existingStock.getId())) {
                    System.out.println("DUPLICATE FOUND: Stock ID " + stockDto.getId() + " already in bill");
                    return true; // Duplicate found
                }
            }
        }

        System.out.println("No duplicate found for Stock ID: " + stockDto.getId());
        return false; // No duplicate
    }

    public void addBillItemOld() {
        editingQty = null;

        if (billItem == null) {
            errorMessage = "No Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            errorMessage = "No Pharmaceutical Bill Item";
            return;
        }
        if (getStock() == null) {
            errorMessage = "Please select item";
            return;
        }

        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            errorMessage = "Pls Select Stock";
            return;
        }

        if (getQty() == null) {
            errorMessage = "Quentity?";
            return;
        }

        if (getQty() > getStock().getStock()) {
            errorMessage = "No Sufficient Stocks";
            return;
        }

        if (checkItemBatch()) {
            errorMessage = "Already added this item batch";
            return;
        }

        //Checking User Stock Entity - COMMENTED OUT FOR PERFORMANCE
        //if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
        //    errorMessage = "Sorry Already Other User Try to Billing This Stock You Cant Add";
        //    return;
        //}
        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        //User Stock Container Save if New Bill - COMMENTED OUT FOR PERFORMANCE
        //userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        //UserStock us = userStockController.saveUserStock(billItem, getSessionController().getLoggedUser(), getUserStockContainer());
        //billItem.setTransUserStock(us);
        calculateRatesForAllBillItemsInPreBill();

        calculateBillItemsAndBillTotalsOfPreBill();

        clearBillItem();
        setActiveIndex(1);
    }

    public void removeBillItem(BillItem b) {
        // UserStock operation commented out for performance
        //userStockController.removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
        getPreBill().getBillItems().remove(b.getSearialNo());

        calculateBillItemsAndBillTotalsOfPreBill();
    }

    public void removeSelectedBillItems() {
        if (selectedBillItems == null || selectedBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please select items to delete");
            return;
        }

        Iterator<BillItem> iterator = selectedBillItems.iterator();
        while (iterator.hasNext()) {
            BillItem billItem = iterator.next();
            // UserStock operation commented out for performance
            //userStockController.removeUserStock(billItem.getTransUserStock(), getSessionController().getLoggedUser());
            getPreBill().getBillItems().remove(billItem);
            iterator.remove();
        }

        calculateBillItemsAndBillTotalsOfPreBill();
    }

    //    Checked
    public void handleQuentityEntryListner(AjaxBehaviorEvent event) {
        if (stock == null) {
            errorMessage = "No stock";
            return;
        }
        if (getPreBill() == null) {
            errorMessage = "No Pre Bill";
            return;
        }
        if (billItem == null) {
            errorMessage = "No Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            errorMessage = "No Pharmaceutical Bill Item";
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            errorMessage = "No Stock. Stock Assigned.";
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }
        billItem.setQty(qty);
        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        //Values
        billItem.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
    }

    public void calculateBillItemForEditing(BillItem bi) {
        //////System.out.println("calculateBillItemForEditing");
        //////System.out.println("bi = " + bi);
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            //////System.out.println("calculateItemForEditingFailedBecause of null");
            return;
        }
        //////System.out.println("bi.getQty() = " + bi.getQty());
        //////System.out.println("bi.getRate() = " + bi.getRate());
        bi.setGrossValue(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() * bi.getQty());
        bi.setNetValue(bi.getQty() * bi.getNetRate());
        bi.setDiscount(bi.getGrossValue() - bi.getNetValue());
        //////System.out.println("bi.getNetValue() = " + bi.getNetValue());

    }

    //    Checked
    public void handleStockSelect(SelectEvent event) {
        if (stock == null) {
            JsfUtil.addErrorMessage("Empty Stock");
            return;
        }
        getBillItem();
        billItem.getPharmaceuticalBillItem().setStock(stock);

        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            JsfUtil.addErrorMessage("Null Stock");
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            JsfUtil.addErrorMessage("Null Batch Stock");
            return;
        }
//        getBillItem();
        billItem.setRate(billItem.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateRatesForAllBillItemsInPreBill();
    }

    @Deprecated // Use listnerForPaymentMethodChange
    public void changeListener() {
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
        }
        calculateRatesForAllBillItemsInPreBill();
    }

    //    public void calculateRatesForAllBillItemsInPreBill() {
//        //////System.out.println("calculating all rates");
//        for (BillItem tbi : getPreBill().getBillItems()) {
//            calculateDiscountRates(tbi);
//            calculateBillItemForEditing(tbi);
//        }
//        calculateBillItemsAndBillTotalsOfPreBill();
//    }
    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    //    Checked
    public void calculateDiscountRates(BillItem bi) {
        bi.setDiscountRate(calculateBillItemDiscountRate(bi));
        bi.setDiscount(bi.getDiscountRate() * bi.getQty());
        bi.setNetRate(bi.getRate() - bi.getDiscountRate());
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    //    TO check the functionality
    public double calculateBillItemDiscountRate(BillItem bi) {
        long startTime = System.currentTimeMillis();
        discountCalculationCount++;

        // Generate cache key for this discount calculation
        String cacheKey = generateDiscountCacheKey(bi);

        // PERFORMANCE OPTIMIZATION: Check discount cache first
        if (discountCache != null && cacheKey != null && discountCache.containsKey(cacheKey)) {
            Double cachedDiscount = discountCache.get(cacheKey);
            System.out.println("=== DISCOUNT CALCULATION CACHE HIT (#" + discountCalculationCount + ") ===");
            System.out.println("Cached discount for key '" + cacheKey + "': " + cachedDiscount);
            long endTime = System.currentTimeMillis();
            System.out.println("=== DISCOUNT CALCULATION CACHED - Time: " + (endTime - startTime) + "ms ===");
            return cachedDiscount;
        }

        // DEBUG: Log payment scheme status and stack trace for actual calculation
        System.out.println("=== DISCOUNT CALCULATION DEBUG (#" + discountCalculationCount + ") ===");
        System.out.println("PaymentScheme from controller: " + (getPaymentScheme() != null ? getPaymentScheme().getName() : "NULL"));
        System.out.println("PaymentMethod from controller: " + (getPaymentMethod() != null ? getPaymentMethod().toString() : "NULL"));
        System.out.println("Cache key: " + cacheKey);
        System.out.println("Called from:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < Math.min(6, stackTrace.length); i++) {
            System.out.println("  " + stackTrace[i]);
        }

        if (bi == null) {
            System.out.println("Returning 0.0 - BillItem is NULL");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem() == null) {
            System.out.println("Returning 0.0 - PharmaceuticalBillItem is NULL");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            System.out.println("Returning 0.0 - Stock is NULL");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            System.out.println("Returning 0.0 - ItemBatch is NULL");
            return 0.0;
        }

        // Skip ALL discount calculation if no payment scheme is selected
        if (getPaymentScheme() == null) {
            System.out.println("Returning 0.0 - PaymentScheme is NULL");
            // Cache this NULL payment scheme result
            if (cacheKey != null && discountCache != null) {
                discountCache.put(cacheKey, 0.0);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("=== DISCOUNT CALCULATION COMPLETE (NULL SCHEME) - Time: " + (endTime - startTime) + "ms ===");
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
            long beforePriceMatrix = System.currentTimeMillis();
            discountRate = getPriceMatrixController().getPaymentSchemeDiscountPercent(getPaymentMethod(), getPaymentScheme(), getSessionController().getDepartment(), bi.getItem());
            long afterPriceMatrix = System.currentTimeMillis();

            double dr;
            dr = (retailRate * discountRate) / 100;
            return dr;

        }

        //PAYMENTMETHOD DISCOUNT
        if (getPaymentMethod() != null && discountAllowed) {
            long beforePriceMatrix = System.currentTimeMillis();
            discountRate = getPriceMatrixController().getPaymentSchemeDiscountPercent(getPaymentMethod(), getSessionController().getDepartment(), bi.getItem());
            long afterPriceMatrix = System.currentTimeMillis();

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
        // Cache the final discount result
        double finalDiscount = 0;
        if (cacheKey != null && discountCache != null) {
            discountCache.put(cacheKey, finalDiscount);
            System.out.println("Cached discount result: " + finalDiscount + " for key: " + cacheKey);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("=== DISCOUNT CALCULATION COMPLETE - Time: " + (endTime - startTime) + "ms ===");

        return finalDiscount;

    }

    /**
     * Generate cache key for discount calculation based on billItem context.
     * Returns null for uncacheable scenarios (incomplete data).
     */
    private String generateDiscountCacheKey(BillItem bi) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null
            || bi.getPharmaceuticalBillItem().getStock() == null
            || bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return null;
        }

        // Create cache key based on payment context and item
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("item_");
        keyBuilder.append(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getItem().getId());
        keyBuilder.append("_scheme_");
        keyBuilder.append(getPaymentScheme() != null ? getPaymentScheme().getId() : "null");
        keyBuilder.append("_method_");
        keyBuilder.append(getPaymentMethod() != null ? getPaymentMethod().toString() : "null");

        return keyBuilder.toString();
    }

    public void saveBillFee(BillItem bi, Payment p) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(bi.getNetValue());
        bf.setFeeGrossValue(bi.getGrossValue());
        bf.setSettleValue(bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
//        createBillFeePaymentAndPayment(bf, p);
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }

    public List<Payment> createPaymentForRetailSaleCancellation(Bill cancellationBill, PaymentMethod inputPaymentMethod) {
        List<Payment> ps = new ArrayList<>();
        if (inputPaymentMethod == null) {
            List<Payment> originalBillPayments = billService.fetchBillPayments(cancellationBill.getBilledBill());
            if (originalBillPayments != null) {
                for (Payment originalBillPayment : originalBillPayments) {
                    Payment p = originalBillPayment.clonePaymentForNewBill();
                    p.invertValues();
                    p.setReferancePayment(originalBillPayment);
                    p.setBill(cancellationBill);
                    p.setInstitution(getSessionController().getInstitution());
                    p.setDepartment(getSessionController().getDepartment());
                    p.setCreatedAt(new Date());
                    p.setCreater(getSessionController().getLoggedUser());
                    paymentFacade.create(p);
                    ps.add(p);
                }
            }
        } else if (inputPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (cd.getPaymentMethodData() == null || cd.getPaymentMethod() == null) {
                    continue;
                }
                Payment p = new Payment();
                p.setBill(cancellationBill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCreditCard().getComment());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCheque().getComment());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCash().getComment());
                        break;
                    case ewallet:
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
                    case Agent:
                    case Credit:
                    case PatientDeposit:
                    case Slip:
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }
                p.setPaidValue(0 - Math.abs(p.getPaidValue()));
                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(cancellationBill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(inputPaymentMethod);
            p.setPaidValue(cancellationBill.getNetTotal());

            switch (inputPaymentMethod) {
                case Card:
                    p.setBank(getPaymentMethodData().getCreditCard().getInstitution());
                    p.setCreditCardRefNo(getPaymentMethodData().getCreditCard().getNo());
                    p.setComments(getPaymentMethodData().getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(getPaymentMethodData().getCheque().getDate());
                    p.setChequeRefNo(getPaymentMethodData().getCheque().getNo());
                    p.setComments(getPaymentMethodData().getCheque().getComment());
                    break;
                case Cash:
                    p.setComments(getPaymentMethodData().getCash().getComment());
                    break;
                case ewallet:
                    p.setComments(getPaymentMethodData().getEwallet().getComment());
                    break;

                case Agent:
                case Credit:
                case PatientDeposit:
                case Slip:
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
            }

            p.setPaidValue(0 - Math.abs(p.getBill().getNetTotal()));
            paymentFacade.create(p);
            ps.add(p);
        }
        return ps;
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

//    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
//        BillFeePayment bfp = new BillFeePayment();
//        bfp.setBillFee(bf);
//        bfp.setAmount(bf.getSettleValue());
//        bfp.setInstitution(getSessionController().getInstitution());
//        bfp.setDepartment(getSessionController().getDepartment());
//        bfp.setCreater(getSessionController().getLoggedUser());
//        bfp.setCreatedAt(new Date());
//        bfp.setPayment(p);
//        getBillFeePaymentFacade().create(bfp);
//    }
    private void clearBill() {
        preBill = null;
        saleBill = null;
        patient = null;
        toInstitution = null;
        toStaff = null;
//        billItems = null;
        patientTabId = "tabPt";
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        paymentScheme = null;
        paymentMethod = PaymentMethod.Cash;
        //userStockContainer = null; // Removed for performance
        fromOpdEncounter = false;
        opdEncounterComments = null;
        patientSearchTab = 0;
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
        stockDto = null;
        editingQty = null;
        errorMessage = "";
        // paymentMethod = PaymentMethod.Cash; // Never do this. It shold be done in clear bill item
        paymentMethodData = null;
        setCashPaid(0.0);
        allergyListOfPatient = null;
    }

    /**
     * Clear performance optimization caches at the start of each add button action.
     * This prevents stale cached data from affecting new operations while allowing
     * optimization during a single add button workflow.
     */
    private void clearPerformanceCaches() {
        long startTime = System.currentTimeMillis();

        // Reset cache objects
        converterCache = new java.util.concurrent.ConcurrentHashMap<>();
        discountCache = new java.util.concurrent.ConcurrentHashMap<>();

        // Reset counters for performance tracking
        converterCallCount = 0;
        discountCalculationCount = 0;

        long endTime = System.currentTimeMillis();
        System.out.println("=== PERFORMANCE CACHES CLEARED - Time: " + (endTime - startTime) + "ms ===");
    }

    /**
     * Warm up JPA facades by triggering initialization during controller startup.
     * This prevents the 8+ second delay on first user interaction by initializing
     * Hibernate EntityManagerFactory, connection pools, and metadata caches early.
     * Called from init() @PostConstruct method.
     */
    private void warmUpJpaFacades() {
        // Run warm-up in background thread to not block UI
        new Thread(() -> {
            try {
                long warmUpStart = System.currentTimeMillis();
                System.out.println("=== CASHIER JPA WARM-UP START ===");

                // Simple and robust warm-up - just trigger JPA initialization
                int successCount = 0;

                // Warm up Item facade
                try {
                    System.out.println("WARM-UP: Initializing Item facade...");
                    long itemStart = System.currentTimeMillis();
                    itemFacade.count(); // Simple count query to warm up
                    long itemEnd = System.currentTimeMillis();
                    System.out.println("WARM-UP: Item facade initialized in " + (itemEnd - itemStart) + "ms");
                    successCount++;
                } catch (Exception e) {
                    System.out.println("WARM-UP: Item facade failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }

                // Warm up ItemBatch facade - MOST IMPORTANT
                try {
                    System.out.println("WARM-UP: Initializing ItemBatch facade...");
                    long batchStart = System.currentTimeMillis();

                    // Start with simple operations
                    long count = itemBatchFacade.count();
                    System.out.println("WARM-UP: ItemBatch count = " + count);

                    // Try to load some entities if count is reasonable
                    if (count > 0 && count < 1000000) {
                        System.out.println("WARM-UP: Loading sample ItemBatch entities...");
                        java.util.List<ItemBatch> batches = itemBatchFacade.findRange(new int[]{0, Math.min(50, (int)count)});
                        System.out.println("WARM-UP: Loaded " + batches.size() + " ItemBatch entities");

                        // Access properties for first few entities only
                        int accessed = 0;
                        for (ItemBatch batch : batches) {
                            if (batch != null && accessed < 5) {
                                try {
                                    Long id = batch.getId();
                                    if (id != null) {
                                        System.out.println("WARM-UP: Accessed ItemBatch ID " + id);
                                        accessed++;
                                    }
                                } catch (Exception e2) {
                                    System.out.println("WARM-UP: Error accessing batch properties: " + e2.getMessage());
                                }
                            }
                        }
                        System.out.println("WARM-UP: Successfully accessed " + accessed + " ItemBatch entities");
                    }

                    long batchEnd = System.currentTimeMillis();
                    System.out.println("WARM-UP: ItemBatch facade initialized in " + (batchEnd - batchStart) + "ms");
                    successCount++;
                } catch (Exception e) {
                    System.out.println("WARM-UP: ItemBatch facade failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                }

                // Warm up Stock facade
                try {
                    System.out.println("WARM-UP: Initializing Stock facade...");
                    long stockStart = System.currentTimeMillis();
                    stockFacade.count(); // Simple count query
                    long stockEnd = System.currentTimeMillis();
                    System.out.println("WARM-UP: Stock facade initialized in " + (stockEnd - stockStart) + "ms");
                    successCount++;
                } catch (Exception e) {
                    System.out.println("WARM-UP: Stock facade failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }

                long warmUpEnd = System.currentTimeMillis();
                long totalWarmUpTime = warmUpEnd - warmUpStart;

                // Mark warm-up as completed
                jpaWarmUpCompleted = true;

                System.out.println("=== CASHIER JPA WARM-UP COMPLETE - Total time: " + totalWarmUpTime + "ms ===");
                System.out.println("WARM-UP: First user interaction should now be fast!");
                System.out.println("WARM-UP: jpaWarmUpCompleted flag set to true");

            } catch (Exception e) {
                System.out.println("WARM-UP: Warm-up thread failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "PharmacyCashier-JPA-WarmUp").start();
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

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
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

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable = true;
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
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
        if (preBill == null) {
            preBill = new PreBill();
            preBill.setBillType(BillType.PharmacyPre);
            preBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
            //   preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
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
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (patient == null || patient.getId() == null) {
                return; // Patient not selected yet, ignore
            }
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(patient, sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                // Set total value to net total only if there's sufficient balance, otherwise set to available balance
                double availableBalance = pd.getBalance();
                if (availableBalance >= netTotal) {
                    getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
                } else {
                    getPaymentMethodData().getPatient_deposit().setTotalValue(availableBalance);
                }
            } else {
                getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
            }
        } else if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (patient == null || patient.getId() == null) {
                return; // Patient not selected yet, ignore
            }
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(patient, sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit && cd.getPaymentMethodData() != null) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                        cd.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
        calculateBillItemsAndBillTotalsOfPreBill();
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

    public void calculateDobFromAge() {
        if (patient == null) {
            return;
        }
        if (patient.getPerson() == null) {
            return;
        }
        patient.getPerson().calDobFromAge();
    }

    /**
     * Calculates and records comprehensive financial details for retail sale
     * bill items. Populates BillItemFinanceDetails (BIFD) and
     * BillFinanceDetails (BFD) with complete cost, purchase, retail, and
     * wholesale rate information required for pharmacy income reports.
     *
     * @param bill The retail sale bill to calculate financial details for
     */
    // Getter method for JSF to access the converter
    public StockDtoConverter getStockDtoConverter() {
        return new StockDtoConverter();
    }

    // StockDTO Converter for JSF
    public static class StockDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            long converterStartTime = System.currentTimeMillis();

            PharmacySaleController3 controller = (PharmacySaleController3) facesContext.getApplication().getELResolver()
                    .getValue(facesContext.getELContext(), null, "pharmacySaleController3");

            if (controller != null) {
                controller.converterCallCount++;
            }

            System.out.println("=== CASHIER StockDtoConverter.getAsObject START (#" + (controller != null ? controller.converterCallCount : "?") + ") ===");
            System.out.println("Converting value: " + value);

            if (value == null || value.trim().isEmpty()) {
                System.out.println("Converter: value is null or empty, returning null");
                return null;
            }

            try {
                Long id = Long.valueOf(value);
                System.out.println("Converter: Parsed ID = " + id);

                if (controller == null) {
                    System.out.println("Converter ERROR: controller is null");
                    return null;
                }

                // PERFORMANCE OPTIMIZATION: Check converter cache first
                if (controller.converterCache != null && controller.converterCache.containsKey(id)) {
                    StockDTO cachedDto = controller.converterCache.get(id);
                    System.out.println("Converter: CACHE HIT! Returning cached DTO - ID: " + id + ", StockQty: " + cachedDto.getStockQty());
                    long converterEndTime = System.currentTimeMillis();
                    System.out.println("=== CASHIER StockDtoConverter.getAsObject END (CACHED) - Time: " + (converterEndTime - converterStartTime) + "ms ===");
                    return cachedDto;
                }

                StockDTO foundDto = null;

                // First check: Does current stockDto match?
                if (controller.getStockDto() != null && id.equals(controller.getStockDto().getId())) {
                    System.out.println("Converter: Found match in current stockDto");
                    System.out.println("Converter: Returning DTO with StockQty = " + controller.getStockDto().getStockQty());
                    foundDto = controller.getStockDto();
                }

                // Second check: Search in lastAutocompleteResults
                if (foundDto == null && controller.getLastAutocompleteResults() != null) {
                    System.out.println("Converter: Searching in lastAutocompleteResults ("
                            + controller.getLastAutocompleteResults().size() + " items)");
                    for (StockDTO dto : controller.getLastAutocompleteResults()) {
                        if (dto != null && id.equals(dto.getId())) {
                            System.out.println("Converter: Found match in autocomplete cache - ID: " + dto.getId()
                                    + ", StockQty: " + dto.getStockQty());
                            foundDto = dto;
                            break;
                        }
                    }
                    if (foundDto == null) {
                        System.out.println("Converter: No match found in autocomplete cache");
                    }
                } else if (foundDto == null) {
                    System.out.println("Converter: lastAutocompleteResults is null");
                }

                // Cache the result for future converter calls during this add operation
                if (foundDto != null && controller.converterCache != null) {
                    controller.converterCache.put(id, foundDto);
                    System.out.println("Converter: Cached DTO for ID " + id + " for future use");
                }

                if (foundDto == null) {
                    System.out.println("Converter: No match found anywhere, returning null");
                }

                long converterEndTime = System.currentTimeMillis();
                System.out.println("=== CASHIER StockDtoConverter.getAsObject END - Time: " + (converterEndTime - converterStartTime) + "ms ===");
                return foundDto;

            } catch (NumberFormatException e) {
                System.out.println("Converter ERROR: NumberFormatException - " + e.getMessage());
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

    /**
     * Load bill with all pharmaceutical item associations to avoid lazy loading
     * issues. This is essential for financial calculations that require
     * pharmaceutical item data.
     *
     * @param billId The ID of the bill to load
     * @return Bill with all pharmaceutical associations loaded
     */
    private Bill loadBillWithPharmaceuticalItems(Long billId) {

        // First try to get the basic bill
        Bill bill = getBillFacade().find(billId);

        if (bill != null) {
        }

        // Now try with JPQL to fetch associations
        String jpql = "SELECT b FROM Bill b "
                + "JOIN FETCH b.billItems bi "
                + "LEFT JOIN FETCH bi.pharmaceuticalBillItem pbi "
                + "LEFT JOIN FETCH pbi.itemBatch "
                + "WHERE b.id = :billId";

        Map<String, Object> params = new HashMap<>();
        params.put("billId", billId);

        try {
            Bill loadedBill = getBillFacade().findFirstByJpql(jpql, params);

            if (loadedBill != null && loadedBill.getBillItems() != null) {
                for (BillItem bi : loadedBill.getBillItems()) {
                }
            } else {
            }

            return loadedBill;
        } catch (Exception e) {
            e.printStackTrace();

            // Check if original bill exists before attempting fallback
            if (bill == null) {
                return null;
            }

            // Fallback: Force load collections manually
            if (bill.getBillItems() != null) {
                for (BillItem bi : bill.getBillItems()) {
                    // Force lazy loading
                    if (bi.getPharmaceuticalBillItem() != null) {
                        // Force load item batch if needed
                        if (bi.getPharmaceuticalBillItem().getItemBatch() != null) {
                        }
                    }
                }
            } else {
            }
            return bill;
        }
    }

    /**
     * Calculate and record costing values for stock valuation. Creates
     * BillFinanceDetails and BillItemFinanceDetails with proper financial
     * calculations.
     *
     * @param bill The bill for which to calculate costing values
     */
    private void calculateAndRecordCostingValues(Bill bill) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 1; i <= Math.min(4, stack.length - 1); i++) {
        }

        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            return;
        }

        // Initialize bill finance details if not present
        if (bill.getBillFinanceDetails() == null) {
            BillFinanceDetails billFinanceDetails = new BillFinanceDetails();
            billFinanceDetails.setBill(bill);
            bill.setBillFinanceDetails(billFinanceDetails);
        } else {
            BillFinanceDetails existingBfd = bill.getBillFinanceDetails();

            // Check if calculations are already done
            if (existingBfd.getTotalCostValue() != null
                    || existingBfd.getTotalPurchaseValue() != null
                    || existingBfd.getTotalRetailSaleValue() != null) {
                return;
            }
        }

        // Initialize aggregated values
        java.math.BigDecimal totalCostValue = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPurchaseValue = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalRetailSaleValue = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalWholesaleValue = java.math.BigDecimal.ZERO;

        // Iterate through bill items and calculate stock valuations
        int itemIndex = 0;
        for (BillItem billItem : bill.getBillItems()) {
            itemIndex++;

            if (billItem == null) {
                continue;
            }

            if (!billItem.isConsideredForCosting()) {
                continue;
            }

            // Check if BillItem already has calculated finance details
            BillItemFinanceDetails existingDetails = billItem.getBillItemFinanceDetails();
            if (existingDetails != null) {

                // Check if values are already set - if so, skip calculation to prevent duplicates
                if (existingDetails.getValueAtCostRate() != null
                        || existingDetails.getValueAtPurchaseRate() != null
                        || existingDetails.getValueAtRetailRate() != null) {
                    continue;
                }
            }

            // Initialize BillItemFinanceDetails explicitly (prevents auto-creation duplicates)
            // NOTE: This is the PREFERRED approach - gradually migrate all callers to this pattern
            billItem.initializeBillItemFinanceDetails();
            BillItemFinanceDetails itemFinanceDetails = billItem.getBillItemFinanceDetails();

            // Get quantity - default to 0 if null
            // NOTE: Consistent with updateRetailSaleFinanceDetails - only billItem.getQty() is used for valuations
            // Free quantities are tracked separately but NOT included in stock valuation calculations
            // This is correct behavior - free quantities don't affect cost/purchase/retail valuations
            java.math.BigDecimal quantity = billItem.getQty() != null
                    ? java.math.BigDecimal.valueOf(billItem.getQty()) : java.math.BigDecimal.ZERO;

            // Calculate stock valuations for this item based on pharmaceutical bill item rates
            PharmaceuticalBillItem pharmaItem = billItem.getPharmaceuticalBillItem();

            if (pharmaItem != null) {
                if (pharmaItem.getItemBatch() != null) {
                }
                // Calculate value at cost rate - use actual cost rate from ItemBatch
                Double costRateValue = null;
                if (pharmaItem.getItemBatch() != null) {
                    costRateValue = pharmaItem.getItemBatch().getCostRate();
                }

                if (costRateValue == null || costRateValue <= 0) {
                    costRateValue = pharmaItem.getPurchaseRate(); // fallback
                }

                if (costRateValue > 0) {
                    java.math.BigDecimal costRate = java.math.BigDecimal.valueOf(costRateValue);
                    java.math.BigDecimal valueAtCostRate = quantity.multiply(costRate).negate();
                    itemFinanceDetails.setValueAtCostRate(valueAtCostRate);
                    totalCostValue = totalCostValue.add(valueAtCostRate);
                } else {
                }

                // Calculate value at purchase rate (same as cost rate for now)
                if (pharmaItem.getPurchaseRate() > 0) {
                    java.math.BigDecimal purchaseRate = java.math.BigDecimal.valueOf(pharmaItem.getPurchaseRate());
                    java.math.BigDecimal valueAtPurchaseRate = quantity.multiply(purchaseRate).negate();
                    itemFinanceDetails.setValueAtPurchaseRate(valueAtPurchaseRate);
                    totalPurchaseValue = totalPurchaseValue.add(valueAtPurchaseRate);
                } else {
                }

                // Calculate value at retail rate (based on retail rate)
                if (pharmaItem.getRetailRate() > 0) {
                    java.math.BigDecimal retailRate = java.math.BigDecimal.valueOf(pharmaItem.getRetailRate());
                    java.math.BigDecimal valueAtRetailRate = quantity.multiply(retailRate).negate();
                    itemFinanceDetails.setValueAtRetailRate(valueAtRetailRate);
                    totalRetailSaleValue = totalRetailSaleValue.add(valueAtRetailRate);
                } else {
                }

                // Calculate value at wholesale rate (use retail rate if wholesale rate not available)
                double wholesaleRate = pharmaItem.getWholesaleRate() > 0
                        ? pharmaItem.getWholesaleRate()
                        : (pharmaItem.getRetailRate() > 0 ? pharmaItem.getRetailRate() : 0.0);

                if (wholesaleRate > 0) {
                    java.math.BigDecimal wholsaleRateBd = java.math.BigDecimal.valueOf(wholesaleRate);
                    java.math.BigDecimal valueAtWholesaleRate = quantity.multiply(wholsaleRateBd).negate();
                    itemFinanceDetails.setValueAtWholesaleRate(valueAtWholesaleRate);
                    totalWholesaleValue = totalWholesaleValue.add(valueAtWholesaleRate);
                }

                // Set rates in the finance details - use the SAME costRateValue used for calculations
                if (costRateValue != null && costRateValue > 0) {
                    // CRITICAL FIX: Store the actual cost rate used in calculations (not always purchaseRate)
                    itemFinanceDetails.setCostRate(java.math.BigDecimal.valueOf(costRateValue));
                    itemFinanceDetails.setTotalCostRate(java.math.BigDecimal.valueOf(costRateValue));
                    itemFinanceDetails.setLineCostRate(java.math.BigDecimal.valueOf(costRateValue));
                }
                if (pharmaItem.getPurchaseRate() > 0) {
                    itemFinanceDetails.setPurchaseRate(java.math.BigDecimal.valueOf(pharmaItem.getPurchaseRate()));
                }
                if (pharmaItem.getRetailRate() > 0) {
                    itemFinanceDetails.setRetailSaleRate(java.math.BigDecimal.valueOf(pharmaItem.getRetailRate()));
                }
                if (wholesaleRate > 0) {
                    itemFinanceDetails.setWholesaleRate(java.math.BigDecimal.valueOf(wholesaleRate));
                }
            }

            // Set quantity in finance details
            itemFinanceDetails.setQuantity(quantity);
            itemFinanceDetails.setTotalQuantity(quantity);

        }

        // Update bill level aggregated values
        BillFinanceDetails billFinanceDetails = bill.getBillFinanceDetails();
        billFinanceDetails.setTotalCostValue(totalCostValue);
        billFinanceDetails.setTotalPurchaseValue(totalPurchaseValue);
        billFinanceDetails.setTotalRetailSaleValue(totalRetailSaleValue);
        billFinanceDetails.setTotalWholesaleValue(totalWholesaleValue);

        // NOTE: Removed redundant billItemFacade.edit() calls to prevent duplicate cascades
        // The bill.save() below will cascade to all billItems and their financeDetails automatically
        // Save the bill with its finance details
        billFacade.edit(bill);
    }

}
