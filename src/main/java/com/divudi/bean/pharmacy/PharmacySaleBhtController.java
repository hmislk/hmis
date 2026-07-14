/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UserNotificationController;
import com.divudi.bean.common.WebUserController;

import com.divudi.core.util.JsfUtil;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.Sex;
import com.divudi.core.data.StockQty;
import com.divudi.core.data.Title;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.ejb.PharmacyService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.UserStock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.service.pharmacy.DirectIssueBatchService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import com.divudi.service.pharmacy.PharmacyCostingService;
import java.math.BigDecimal;
import java.util.Optional;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import java.util.logging.Logger;

@Named
@SessionScoped
public class PharmacySaleBhtController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PharmacySaleBhtController.class.getName());

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacySaleBhtController() {
    }

    @PostConstruct
    public void init() {
        registerPageMetadata();
        // Ensure clean state when page is accessed
        clearBillItem();
        makeNull();
    }

    /**
     * Register page metadata for the admin interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("inward/pharmacy_bill_issue_bht");
        metadata.setPageName("Pharmacy BHT Direct Issue");
        metadata.setDescription("Direct issue of medicines to inpatients from pharmacy");
        metadata.setControllerClass("PharmacySaleBhtController");

        // Register configuration options used on this page
        metadata.addConfigOption(new ConfigOptionInfo(
            "Medicine Identification Codes Used",
            "Shows medicine identification codes in the autocomplete dropdown",
            "Autocomplete column: Medicine code visibility",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Bill Support for Native Printers",
            "Enables native printer support for pharmacy bills",
            "Bill preview section: Native printer button rendering",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Inward Direct Issue Bill is FiveFiveCustom3",
            "Displays bill in FiveFiveCustom3 paper format",
            "Bill preview section: 5.5 custom paper format rendering",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Inward Direct Issue Bill is PosHeaderPaper",
            "Displays bill in POS header paper format",
            "Bill preview section: POS header paper format rendering",
            OptionScope.APPLICATION
        ));

        // Register privileges used on this page
        metadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Access to page configuration management interface",
            "Page header: Config button visibility"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
            "NursingWorkBench",
            "Access from nursing workbench interface - shows back to workbench button",
            "Page header and actions: Back to workbench navigation"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
            "ShowDrugCharges",
            "View drug prices and financial charges in the billing interface",
            "Item autocomplete and bill table: Rate and value columns visibility"
        ));

        // Register the page metadata
        pageMetadataRegistry.registerPage(metadata);
    }

    @Inject
    UserStockController userStockController;
    @Inject
    PaymentSchemeController PaymentSchemeController;

    @Inject
    SessionController sessionController;
    @Inject
    PharmacyCalculation pharmacyCalculation;
    @Inject
    UserNotificationController userNotificationController;
    @Inject
    WebUserController webUserController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    ItemBatchFacade itemBatchFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    private com.divudi.ejb.PrescriptionToItemService prescriptionToItemService;
    @EJB
    private DirectIssueBatchService directIssueBatchService;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    BillService billService;
    @EJB
    private PharmacyService pharmacyService;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    PageMetadataRegistry pageMetadataRegistry;
/////////////////////////
    Item selectedAlternative;
    private PreBill preBill;
    Bill printBill;
    Bill bill;
    private Bill bhtRequestBill;
    BillItem billItem;
    Stock replacableStock;
    Item selectedAvailableAmp;
    //BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Stock stock;
    StockDTO stockDto;

    // Performance optimization fields
    private StockDTO selectedStockDto;
    private Long selectedStockId;
    private List<StockDTO> lastAutocompleteResults;

    // Metadata caching for autocomplete performance
    private ConcurrentHashMap<String, List<Long>> searchMetadataCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30000; // 30 seconds

    // Cached price-matrix config flag (resolved lazily, cleared on resetAll)
    private Boolean cachedMatrixByAdmissionDepartment;

    private Item item;
    private PatientEncounter patientEncounter;
    int activeIndex;
    boolean billPreview = false;
    // When true, medicines are issued at retail rate with NO inward price-matrix
    // service charge (used by the Issue Discharge Medicines page).
    boolean dischargeIssueMode = false;
    // Per-prescription conversion report shown on the discharge issue page so the
    // pharmacist sees the original prescription against what was actually resolved
    // (and any low/no-stock shortfall) — prevents silent omissions. Issue #21334.
    private List<DischargeConversionRow> dischargeConversionReport = new ArrayList<>();
    Department department;
    String errorMessage = "";
    /////////////////
    List<Stock> replaceableStocks;
    //List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    List<BillItem> billItems;
    /////////////////////////
    private UserStockContainer userStockContainer;
    private List<ClinicalFindingValue> allergyListOfPatient;

    private Bill batchBill;
    @Inject
    private BillBeanController billBean;
    private Stock tmpStock;
    private Double billItemTotal;

    public void selectSurgeryBillListener() {
        patientEncounter = getBatchBill().getPatientEncounter();
    }

    public void settleSurgeryBhtIssue() {
        if (getBatchBill() == null) {
            return;
        }
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("There are No Medicines/Devices to Bill!!!");
            return;
        }
        if (getBatchBill().getProcedure() == null) {
            return;
        }
        if (patientEncounter == null) {
            patientEncounter = getBatchBill().getPatientEncounter();
        }
        if (patientEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot issue medicines: nursing discharge has already been confirmed for this patient.");
            return;
        }
        if (patientEncounter.isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return;
        }
        if (hasAllergyConflicts(getPreBill().getBillItems())) {
            return;
        }
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE;
        BillType bt = BillType.PharmacyBhtPre;
        settleBhtIssue(bt, bta, getBatchBill().getFromDepartment());
        getBillBean().saveEncounterComponents(getPrintBill(), getBatchBill(), getSessionController().getLoggedUser());
        getBillBean().updateBatchBill(getBatchBill());
    }

//    public void settleSurgeryBhtIssueStore() {
//        if (getBatchBill() == null) {
//            return;
//        }
//
//        if (getBatchBill().getProcedure() == null) {
//            return;
//        }
//
//        settleBhtIssue(BillType.StoreBhtPre, getBatchBill().getFromDepartment(), BillNumberSuffix.PHISSUE);
//
//        getBillBean().saveEncounterComponents(getPrintBill(), getBatchBill(), getSessionController().getLoggedUser());
//        getBillBean().updateBatchBill(getBatchBill());
//
//    }
    public void makeNull() {
        selectedAlternative = null;
        preBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        stockDto = null;
        // Clear DTO-related fields
        selectedStockDto = null;
        selectedStockId = null;
        lastAutocompleteResults = null;
        activeIndex = 0;
        billPreview = false;
        replaceableStocks = null;
        itemsWithoutStocks = null;
        patientEncounter = null;
        batchBill = null;
        allergyListOfPatient = null;
    }

    public void makeNullWithFill() {
        makeNull();
//        searchController.createInwardBHTForIssueTable();
    }

    public double getOldQty(BillItem bItem) {
        String sql = "Select b.qty From BillItem b"
                + " where b.retired=false and b.bill=:b and b=:itm";
        HashMap hm = new HashMap();
        hm.put("b", getPreBill());
        hm.put("itm", bItem);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public String navigateToCancelBhtRequest() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }

        return "/inward/bht_bill_cancel?faces-redirect=true";
    }

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        onEdit(tmp);
    }

    public void onEditing(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();

        tmp.setQty(tmp.getPharmaceuticalBillItem().getQtyInUnit());
        if (tmp.getPharmaceuticalBillItem().getQtyInUnit() <= 0) {
            setZeroToQty(tmp);
            recalculateEditedIssueRow(tmp);
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return;
        }
        Stock fetchedStock = tmp.getPharmaceuticalBillItem().getStock();
        if (fetchedStock != null && tmp.getPharmaceuticalBillItem().getQtyInUnit() > fetchedStock.getStock()) {
            setZeroToQty(tmp);
            recalculateEditedIssueRow(tmp);
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        if (tmp.getReferanceBillItem() != null) {
            double remaining = getRemainingQuantityForItem(tmp.getReferanceBillItem());
            if (tmp.getPharmaceuticalBillItem().getQtyInUnit() > remaining) {
                JsfUtil.addErrorMessage("Cannot issue " + tmp.getPharmaceuticalBillItem().getQtyInUnit()
                        + " units of " + tmp.getItem().getName() + ". Only " + remaining + " units remaining to be issued.");
                tmp.setQty(remaining);
                tmp.getPharmaceuticalBillItem().setQtyInUnit((float) remaining);
                userStockController.updateUserStock(tmp.getTransUserStock(), remaining);
            }
        }

        recalculateEditedIssueRow(tmp);
    }

    /**
     * After a row-edit on the BHT issue page, restore the issue sign convention
     * (unit qty is stored NEGATIVE for issues, exactly as
     * generateIssueBillComponentsForBhtRequest builds the rows) and recalculate
     * this row's financials (rate/margin/gross/net) plus the running bill total
     * for the edited quantity.
     *
     * Without this, a partially-issued row settled after a qty edit keeps the
     * ORIGINAL requested-quantity gross/margin/net values while stock moves by
     * the edited quantity, and the positive unit qty flips the sign of the
     * movement in the Cost Of Goods Sold report (found via COGS E2E
     * verification: request 20, issue 10 → bill said 458.70, stock moved 10,
     * COGS saw +10 instead of -10).
     */
    private void recalculateEditedIssueRow(BillItem tmp) {
        if (tmp == null || tmp.getPharmaceuticalBillItem() == null) {
            return;
        }
        double editedQty = Math.abs(tmp.getPharmaceuticalBillItem().getQtyInUnit());
        tmp.setQty(editedQty);
        tmp.getPharmaceuticalBillItem().setQtyInUnit((float) (0 - editedQty));
        tmp.getPharmaceuticalBillItem().setQty(0 - editedQty);
        calculateRates(tmp);
        calCurrentBillItemTotal(getBillItems());
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);
        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    private void setQtyToMatchAvailability(BillItem tmp, Double availableQty) {
        tmp.setQty(availableQty);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(availableQty);
        tmp.getPharmaceuticalBillItem().setQty(availableQty);
        userStockController.updateUserStock(tmp.getTransUserStock(), availableQty);
    }

    /**
     * Validates if decimal quantities are allowed based on three-tier configuration hierarchy.
     *
     * Priority 1: Universal decimal allowance (application-wide setting)
     * Priority 2: Item-specific configuration (Item.allowFractions field)
     * Priority 3: Integer-only enforcement (existing behavior)
     *
     * @param qty The quantity to validate
     * @param item The item being validated
     * @return true if the quantity contains decimals and decimals are not allowed, false otherwise
     */
    private boolean isDecimalQuantityNotAllowed(Double qty, Item item) {
        // If quantity is null or is already an integer, no validation needed
        if (qty == null || qty % 1 == 0) {
            return false;
        }

        // Priority 1: Check if decimals are allowed universally
        boolean allowDecimalsUniversally = configOptionApplicationController.getBooleanValueByKey(
            "Pharmacy Direct Issue to BHT - Allow Decimals Universally", false);
        if (allowDecimalsUniversally) {
            return false; // Decimals allowed universally
        }

        // Priority 2: Check if the specific item allows fractions
        boolean itemAllowsFractions = (item != null && item.isAllowFractions());
        if (itemAllowsFractions) {
            return false; // Item-specific setting allows decimals
        }

        // Priority 3: Integer-only enforcement (existing behavior)
        boolean mustBeInteger = configOptionApplicationController.getBooleanValueByKey(
            "Pharmacy Direct Issue to BHT - Quantity Must Be Integer", true);
        return mustBeInteger; // Decimals not allowed if integer-only is enforced
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

        // Validate quantity based on three-tier configuration hierarchy
        if (isDecimalQuantityNotAllowed(tmp.getQty(), tmp.getItem())) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Please enter only whole numbers (integers). Decimal values are not allowed for this item.");
            return true;
        }

        Stock fetchedStock = getStockFacade().find(tmp.getPharmaceuticalBillItem().getStock().getId());

        if (tmp.getQty() > fetchedStock.getStock()) {
            setQtyToMatchAvailability(tmp, fetchedStock.getStock());
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("There are no sufficient stocks. Please adjust quantity");
            return true;
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {

            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("Another User On Change Bill Item "
                    + " Qty value is resetted");
            return true;
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

        onEditCalculation(tmp);

        return false;
    }

    private void onEditCalculation(BillItem tmp) {
        if (tmp == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return;
        }
        if (tmp.getPharmaceuticalBillItem().getStock().getItemBatch().getItem() == null) {
            return;
        }
        calculateRates(tmp);
        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());
//        calculateBillItemForEditing(tmp);
        calTotal();
    }

    public void changeBillItem(BillItem bi, Stock tempStock) {
        if (bi == null) {
            JsfUtil.addErrorMessage("Bill item is required");
            return;
        }
        if (tempStock == null) {
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        if (tempStock.getItemBatch() == null) {
            JsfUtil.addErrorMessage("Invalid stock - missing batch information");
            return;
        }
        if (tempStock.getItemBatch().getDateOfExpire() != null
                && tempStock.getItemBatch().getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("Please not select Expired Items");
            return;
        }
        if (tempStock.getStock() <= 0) {
            JsfUtil.addErrorMessage("No sufficient stock available");
            return;
        }
        bi.getPharmaceuticalBillItem().setItemBatch(tempStock.getItemBatch());
        bi.getPharmaceuticalBillItem().setStock(tempStock);
        bi.setItem(tempStock.getItemBatch().getItem());
        calculateRates(bi);
        calCurrentBillItemTotal(getBillItems());
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Stock> getReplaceableStocks() {
        return replaceableStocks;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        if (qty != null && qty <= 0) {
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return;
        }
        this.qty = qty;
    }

    public Stock getStock() {
        // Implement lazy loading pattern - only fetch when needed
        if (stock == null && selectedStockId != null) {
            stock = getStockFacade().find(selectedStockId);
        }
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
        // Update selectedStockId when stock is set directly
        this.selectedStockId = stock != null ? stock.getId() : null;
    }

    // New DTO-based getters and setters
    public StockDTO getSelectedStockDto() {
        return selectedStockDto;
    }

    public void setSelectedStockDto(StockDTO selectedStockDto) {
        this.selectedStockDto = selectedStockDto;
        this.selectedStockId = selectedStockDto != null ? selectedStockDto.getId() : null;
        this.stock = null; // Clear cached entity to force lazy loading
    }

    public Long getSelectedStockId() {
        return selectedStockId;
    }

    public void setSelectedStockId(Long selectedStockId) {
        this.selectedStockId = selectedStockId;
        this.stock = null; // Clear cached entity to force lazy loading
    }

    public List<StockDTO> getLastAutocompleteResults() {
        return lastAutocompleteResults;
    }

    public void setLastAutocompleteResults(List<StockDTO> lastAutocompleteResults) {
        this.lastAutocompleteResults = lastAutocompleteResults;
    }

    /**
     * Handles stock selection from autocomplete component
     * Sets up billItem with selected stock and calculates rates/values
     * @param event SelectEvent containing the selected StockDTO
     */
    public void handleStockSelect(SelectEvent event) {
        try {
            StockDTO selectedDto = (StockDTO) event.getObject();
            this.selectedStockDto = selectedDto;
            this.selectedStockId = selectedDto != null ? selectedDto.getId() : null;
            this.stock = null;

            if (selectedDto != null) {
                if (getBillItem() == null) {
                    setBillItem(new BillItem());
                }
                if (getBillItem().getPharmaceuticalBillItem() == null) {
                    getBillItem().setPharmaceuticalBillItem(new PharmaceuticalBillItem());
                }

                // Stock/ItemBatch proxies are deferred to addBillItem(). Loading them here
                // was causing 20-26s first-touch lag per batch because EclipseLink weaves
                // the descriptor and Payara's JDBC pool validates a connection on first
                // borrow per entity class. We only need DTO values for preliminary display.
                calculateRatesFromDto(getBillItem(), selectedDto);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in handleStockSelect", e);
        }
    }

    public void setReplaceableStocks(List<Stock> replaceableStocks) {
        this.replaceableStocks = replaceableStocks;
    }

    public Item getSelectedAlternative() {
        return selectedAlternative;
    }

    public void setSelectedAlternative(Item selectedAlternative) {
        this.selectedAlternative = selectedAlternative;
    }

    public void selectReplaceableStocks() {
        if (selectedAlternative == null || !(selectedAlternative instanceof Amp)) {
            replaceableStocks = new ArrayList<>();
            return;
        }
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        Amp amp = (Amp) selectedAlternative;
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("s", d);
        m.put("vmp", amp.getVmp());
        sql = "select i from Stock i join treat(i.itemBatch.item as Amp) amp where i.stock >:s and i.department=:d and amp.vmp=:vmp order by i.itemBatch.item.name";
        replaceableStocks = getStockFacade().findByJpql(sql, m);
    }

    public List<Item> getItemsWithoutStocks() {
        return itemsWithoutStocks;
    }

    public void setItemsWithoutStocks(List<Item> itemsWithoutStocks) {
        this.itemsWithoutStocks = itemsWithoutStocks;
    }

    public void resetAll() {
        // PERFORMANCE OPTIMIZATION: UserStock cleanup removed to match cashier workflow
        // No longer needed since UserStock operations are eliminated
        clearBill();
        clearBillItem();
        billPreview = false;
        makeNull();
        department = null;
        replaceableStocks = new ArrayList<>();
        itemsWithoutStocks = new ArrayList<>();
        errorMessage = "";
        cachedMatrixByAdmissionDepartment = null;
        dischargeIssueMode = false;
        dischargeConversionReport = new ArrayList<>();
    }

    /**
     * Reset for the Issue Discharge Medicines page. Same as resetAll() but turns
     * on discharge mode so medicines are issued at retail rate with no inward
     * price-matrix service charge.
     */
    public void resetAllForDischargeIssue() {
        resetAll();
        dischargeIssueMode = true;
    }

    /**
     * Prepares a discharge-medicine issue bill from a set of discharge-medicine
     * prescriptions (selected on the inpatient assessment) for the given
     * admission, then turns on discharge mode. Each prescription is converted to
     * a bill item: the dispensable item and quantity are resolved via
     * {@link com.divudi.ejb.PrescriptionToItemService}, and a FEFO (earliest
     * expiry) stock batch with sufficient quantity is auto-picked from the
     * logged-in pharmacy ({@code sessionController.getDepartment()}). The
     * pharmacist can still change batch/quantity on the issue page before
     * settling. Issue #21334.
     *
     * <p>Mirrors {@link PharmacyRequestForBhtController#addBillItemFromPrescription},
     * but resolves a concrete stock + batch (a discharge issue dispenses
     * immediately, unlike a ward request which is fulfilled later).</p>
     *
     * @param admission     the patient encounter the discharge medicines belong to
     * @param prescriptions the selected discharge-medicine prescriptions
     * @return navigation outcome to the discharge issue page, or "" to stay
     */
    public String prepareDischargeIssueFromPrescriptions(PatientEncounter admission,
            List<com.divudi.core.entity.clinical.Prescription> prescriptions) {
        if (admission == null || admission.getId() == null) {
            JsfUtil.addErrorMessage("No admission selected.");
            return "";
        }
        if (prescriptions == null || prescriptions.isEmpty()) {
            JsfUtil.addErrorMessage("Select at least one discharge medicine.");
            return "";
        }
        Department dispensingDepartment = getSessionController().getLoggedUser().getDepartment();
        if (dispensingDepartment == null) {
            JsfUtil.addErrorMessage("No dispensing pharmacy (logged-in department) found.");
            return "";
        }

        resetAllForDischargeIssue();
        setPatientEncounter(admission);
        dischargeConversionReport = new ArrayList<>();

        int added = 0;
        int notAvailable = 0;
        for (com.divudi.core.entity.clinical.Prescription sourcePrescription : prescriptions) {
            DischargeConversionRow row = addDischargeBillItemFromPrescription(sourcePrescription, dispensingDepartment);
            if (row != null) {
                dischargeConversionReport.add(row);
                if (row.getStatus() != DischargeConversionRow.Status.NOT_AVAILABLE) {
                    added++;
                } else {
                    notAvailable++;
                }
            }
        }

        if (added == 0) {
            // Still navigate to the issue page so the conversion report (which the
            // message tells the pharmacist to review) is visible — every selected
            // medicine failed (no stock or conversion failure), so nothing is on
            // the bill, but the per-medicine outcomes must not be hidden.
            JsfUtil.addWarningMessage("None of the selected medicines could be issued from "
                    + dispensingDepartment.getName() + ". Review the conversion report and add them manually if needed.");
            calTotal();
            return "/inward/pharmacy_discharge_medicine_issue?faces-redirect=true";
        }
        if (notAvailable > 0) {
            JsfUtil.addWarningMessage(notAvailable + " medicine(s) could not be issued from "
                    + dispensingDepartment.getName() + " (no stock or conversion failed). See the conversion report and add them manually if needed.");
        }

        calTotal();
        return "/inward/pharmacy_discharge_medicine_issue?faces-redirect=true";
    }

    /**
     * Converts one discharge-medicine prescription to a discharge issue bill item
     * dispensed from {@code dispensingDepartment}, auto-picking the earliest-expiry
     * (FEFO) stock batch with sufficient quantity. Falls back to qty=1 when the
     * prescription is incomplete (so the pharmacist sets the real quantity on the
     * issue page).
     *
     * <p>Always returns a {@link DischargeConversionRow} describing the original
     * prescription against what was resolved/issued (including no-/low-stock
     * shortfalls) so the pharmacist can see and act on any gap rather than a
     * medicine being silently omitted. A bill item is added only when stock is
     * available. Issue #21334.</p>
     */
    private DischargeConversionRow addDischargeBillItemFromPrescription(
            com.divudi.core.entity.clinical.Prescription sourcePrescription, Department dispensingDepartment) {
        // Never silently drop a selected prescription: a prescription with no
        // medicine still gets a visible report row so the omission is surfaced.
        DischargeConversionRow row = new DischargeConversionRow();
        if (sourcePrescription == null || sourcePrescription.getItem() == null) {
            row.setPrescribedText(sourcePrescription != null
                    ? sourcePrescription.getFormattedPrescription() : "Unknown prescription");
            row.setResolvedItemName("");
            row.setRequiredQty(0.0);
            row.setIssuedQty(0.0);
            row.setStatus(DischargeConversionRow.Status.NOT_AVAILABLE);
            row.setMessage("Prescription has no medicine selected. Review and add manually.");
            return row;
        }
        row.setPrescribedText(sourcePrescription.getFormattedPrescription());

        Item dispensableItem = sourcePrescription.getItem();
        Double calculatedQty = null;
        boolean conversionFailed = false;
        String conversionError = null;
        try {
            com.divudi.ejb.PrescriptionToItemService.PrescriptionToItemResult result
                    = prescriptionToItemService.calculateItemAndQuantity(sourcePrescription);
            if (result != null && result.isSuccess()) {
                if (result.getItem() != null) {
                    dispensableItem = result.getItem();
                }
                if (result.getQuantity() != null) {
                    calculatedQty = result.getQuantity();
                }
            } else {
                // The conversion did NOT succeed. Distinguish a genuine failure
                // (e.g. no suitable AMP for a VTM/VMP, unit-conversion error) from
                // a merely incomplete prescription. Only incomplete prescriptions
                // get the qty=1 fallback; a real failure is flagged and skipped so
                // the bill is never settled with an arbitrary under-dose.
                if (prescriptionToItemService.isCalculationPossible(sourcePrescription)) {
                    conversionFailed = true;
                    conversionError = (result != null && result.getErrorMessage() != null)
                            ? result.getErrorMessage()
                            : "could not be converted to a dispensable item";
                }
            }
        } catch (Exception e) {
            conversionFailed = true;
            conversionError = e.getMessage();
            LOGGER.log(Level.FINE, "Discharge prescription conversion failed for {0}: {1}",
                    new Object[]{dispensableItem.getName(), e.getMessage()});
        }

        row.setResolvedItemName(dispensableItem.getName());

        if (conversionFailed) {
            row.setRequiredQty(0.0);
            row.setIssuedQty(0.0);
            row.setStatus(DischargeConversionRow.Status.NOT_AVAILABLE);
            row.setMessage(dispensableItem.getName() + ": " + conversionError
                    + ". Not added — please add manually or omit.");
            return row;
        }

        boolean qtyDefaulted = false;
        if (calculatedQty == null || calculatedQty <= 0) {
            // Incomplete-prescription path: default to 1 and let the pharmacist
            // adjust on the issue page before settling (mirrors the ward request).
            calculatedQty = 1.0;
            qtyDefaulted = true;
        }

        // Round up to whole units so we never request a fractional dispense.
        double requiredQty = Math.ceil(calculatedQty);
        row.setRequiredQty(requiredQty);

        // FEFO via a lightweight DTO projection (NO entity graph loading — avoids
        // the ~46-query Stock/ItemBatch/Item cascade noted in issue #20138 that
        // made this slow). Earliest-expiry, in-date stock in the dispensing pharmacy.
        List<StockDTO> availableStocks = findFefoStockDtosForItem(dispensableItem, dispensingDepartment);
        if (availableStocks == null || availableStocks.isEmpty()) {
            row.setIssuedQty(0.0);
            row.setStatus(DischargeConversionRow.Status.NOT_AVAILABLE);
            row.setMessage("No stock of " + dispensableItem.getName() + " in " + dispensingDepartment.getName()
                    + ". Add manually from another batch/pharmacy or omit.");
            return row;
        }

        double requestQty = requiredQty;
        StockDTO chosenStock = null;
        for (StockDTO s : availableStocks) {
            if (s.getStockQty() != null && s.getStockQty() >= requestQty) {
                chosenStock = s;
                break;
            }
        }
        boolean partial = false;
        if (chosenStock == null) {
            // No single batch covers the full quantity; take the earliest-expiry
            // batch (the list is already FEFO-ordered) and cap qty to its stock.
            chosenStock = availableStocks.get(0);
            requestQty = Math.min(requestQty, chosenStock.getStockQty());
            if (requestQty <= 0) {
                row.setIssuedQty(0.0);
                row.setStatus(DischargeConversionRow.Status.NOT_AVAILABLE);
                row.setMessage("No usable stock of " + dispensableItem.getName() + " in "
                        + dispensingDepartment.getName() + ".");
                return row;
            }
            partial = true;
        }

        double retailRate = chosenStock.getRetailRate() != null ? chosenStock.getRetailRate() : 0.0;

        // Build the bill item from the DTO using getReference() proxies — never
        // find() — so no eager cascade fires (mirrors addBillItem(), issue #20138).
        Item itemRef = getItemFacade().getReference(chosenStock.getItemId());
        Stock stockRef = getStockFacade().getReference(chosenStock.getId());
        ItemBatch itemBatchRef = getItemBatchFacade().getReference(chosenStock.getItemBatchId());

        BillItem newBillItem = new BillItem();
        newBillItem.setItem(itemRef);
        newBillItem.setQty(requestQty);
        newBillItem.setInwardChargeType(InwardChargeType.Medicine);
        newBillItem.setBill(getPreBill());

        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setBillItem(newBillItem);
        pbi.setStock(stockRef);
        pbi.setItemBatchWithRates(itemBatchRef, retailRate,
                chosenStock.getPurchaseRate() != null ? chosenStock.getPurchaseRate() : 0.0);
        pbi.setDoe(chosenStock.getDateOfExpire());
        pbi.setTransDisplayItemName(chosenStock.getItemName());
        pbi.setFreeQty(0.0f);
        pbi.setQty(0 - Math.abs(requestQty));
        pbi.setQtyInUnit(0 - Math.abs(requestQty));
        newBillItem.setPharmaceuticalBillItem(pbi);

        // Carry the prescription details onto a detached in-memory prescription
        // for the description (persisted later during settle).
        com.divudi.core.entity.clinical.Prescription inMemoryPrescription
                = new com.divudi.core.entity.clinical.Prescription();
        inMemoryPrescription.setItem(itemRef);
        inMemoryPrescription.setDose(sourcePrescription.getDose());
        inMemoryPrescription.setDoseUnit(sourcePrescription.getDoseUnit());
        inMemoryPrescription.setFrequencyUnit(sourcePrescription.getFrequencyUnit());
        inMemoryPrescription.setDuration(sourcePrescription.getDuration());
        inMemoryPrescription.setDurationUnit(sourcePrescription.getDurationUnit());
        inMemoryPrescription.setComment(sourcePrescription.getComment());
        inMemoryPrescription.setPatient(getPatientEncounter().getPatient());
        inMemoryPrescription.setEncounter(getPatientEncounter());
        inMemoryPrescription.setIndoor(true);
        newBillItem.setPrescription(inMemoryPrescription);

        // Build the description from the DTO name (avoids touching the Item proxy).
        StringBuilder desc = new StringBuilder(chosenStock.getItemName());
        if (sourcePrescription.getDose() != null) {
            desc.append(" ").append(sourcePrescription.getDose());
            if (sourcePrescription.getDoseUnit() != null) {
                desc.append(" ").append(sourcePrescription.getDoseUnit().getName());
            }
        }
        if (sourcePrescription.getFrequencyUnit() != null) {
            desc.append(" ").append(sourcePrescription.getFrequencyUnit().getName());
        }
        if (sourcePrescription.getDuration() != null) {
            desc.append(" for ").append(sourcePrescription.getDuration());
            if (sourcePrescription.getDurationUnit() != null) {
                desc.append(" ").append(sourcePrescription.getDurationUnit().getName());
            }
        }
        if (sourcePrescription.getComment() != null && !sourcePrescription.getComment().trim().isEmpty()) {
            desc.append(" - ").append(sourcePrescription.getComment());
        }
        newBillItem.setDescreption(desc.toString());

        // Discharge: retail rate with ZERO inward margin (no service charge).
        // Price directly from the DTO rate — no entity dereference, no price-matrix
        // lookup — so the whole conversion stays free of heavy entity loading.
        double grossValue = retailRate * requestQty;
        newBillItem.setRate(retailRate);
        newBillItem.setGrossValue(grossValue);
        newBillItem.setMarginValue(0.0);
        newBillItem.setMarginRate(0.0);
        newBillItem.setNetValue(grossValue);
        newBillItem.setNetRate(retailRate);
        newBillItem.setAdjustedValue(grossValue);
        newBillItem.setDiscount(0.0);

        if (getPreBill().getBillItems() == null) {
            getPreBill().setBillItems(new ArrayList<>());
        }
        newBillItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(newBillItem);

        // Record the conversion outcome for the on-screen report.
        row.setIssuedQty(requestQty);
        if (partial) {
            row.setStatus(DischargeConversionRow.Status.PARTIAL_LOW_STOCK);
            row.setMessage("Only " + requestQty + " of " + requiredQty + " in stock at "
                    + dispensingDepartment.getName() + ". Issued the available quantity from the earliest-expiry batch — split the rest manually or omit.");
        } else if (qtyDefaulted) {
            row.setStatus(DischargeConversionRow.Status.QTY_DEFAULTED);
            row.setMessage("Quantity could not be calculated (incomplete prescription). Defaulted to "
                    + requestQty + " — verify before settling.");
        } else {
            row.setStatus(DischargeConversionRow.Status.ISSUED_FULL);
            row.setMessage("");
        }
        return row;
    }

    /**
     * FEFO stock lookup for a prescribed item as lightweight {@link StockDTO}
     * projections (stockId, itemBatchId, itemId, name, code, generic, retailRate,
     * stockQty, doe) — NO entity loading. In-date, positive-stock rows in the
     * dispensing pharmacy, earliest expiry first. For AMP/Ampp items the query is
     * by the AMP itself; for VMP/VTM it resolves the candidate AMPs first.
     * Issue #21334.
     */
    private List<StockDTO> findFefoStockDtosForItem(Item dispensableItem, Department dispensingDepartment) {
        List<Amp> amps = pharmacyBean.resolveAmps(dispensableItem);
        if (amps == null || amps.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("amps", amps);
        params.put("d", dispensingDepartment);
        params.put("s", 0.0);
        params.put("doe", new Date());

        String jpql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "s.id, s.itemBatch.id, s.itemBatch.item.id, s.itemBatch.item.name, s.itemBatch.item.code, "
                + "s.itemBatch.item.vmp.name, s.itemBatch.retailsaleRate, s.itemBatch.purcahseRate, "
                + "s.stock, s.itemBatch.dateOfExpire) "
                + "FROM Stock s "
                + "WHERE s.itemBatch.item IN :amps "
                + "AND s.department = :d "
                + "AND s.stock > :s "
                + "AND s.itemBatch.dateOfExpire > :doe "
                + "ORDER BY s.itemBatch.dateOfExpire";

        @SuppressWarnings("unchecked")
        List<StockDTO> dtos = (List<StockDTO>) getStockFacade()
                .findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return dtos != null ? dtos : new ArrayList<>();
    }

    public List<DischargeConversionRow> getDischargeConversionReport() {
        if (dischargeConversionReport == null) {
            dischargeConversionReport = new ArrayList<>();
        }
        return dischargeConversionReport;
    }

    /** True when any converted line was not issued in full (needs pharmacist attention). */
    public boolean isDischargeConversionHasAttentionItems() {
        for (DischargeConversionRow r : getDischargeConversionReport()) {
            if (r.isNeedsAttention()) {
                return true;
            }
        }
        return false;
    }

    public void selectReplaceableStocksNew() {
        if (selectedAvailableAmp == null || !(selectedAvailableAmp instanceof Amp)) {
            replaceableStocks = new ArrayList<>();
            return;
        }
        fillReplaceableStocksForAmp((Amp) selectedAvailableAmp);
    }

    public void makeStockAsBillItemStock() {
        ////// // System.out.println("replacableStock = " + replacableStock);
        setStock(replacableStock);
        getBillItem().getPharmaceuticalBillItem().setStock(getStock());
        calculateRates(billItem);
        ////// // System.out.println("getStock() = " + getStock());
    }

    public void fillReplaceableStocksForAmp(Amp ampIn) {
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        Amp amp = ampIn;
        m.put("d", getDepartment());
        m.put("s", d);
        m.put("vmp", amp.getVmp());
        m.put("a", amp);
        sql = "select i from Stock i join treat(i.itemBatch.item as Amp) amp "
                + "where i.stock >:s and "
                + "i.department=:d and "
                + "amp.vmp=:vmp "
                + "and amp<>:a "
                + "order by i.itemBatch.item.name";
        replaceableStocks = getStockFacade().findByJpql(sql, m);
    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i"
                + " where i.retired=false "
                + " and (i.name) like :n "
                + " and type(i)=:t and i.id not "
                + " in(select ibs.id from Stock ibs "
                + " where ibs.stock >:s "
                + " and ibs.department=:d "
                + " and (ibs.itemBatch.item.name) like :n )"
                + " order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    public List<Item> completeRetailSaleItems(String qry, Department d) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i"
                + " where i.retired=false "
                + " and (i.name) like :n "
                + " and i.vmp is not null "
                + " and type(i)=:t and i.id not "
                + " in(select ibs.id from Stock ibs "
                + " where ibs.stock >:s "
                + " and ibs.department=:d "
                + " and ibs.itemBatch.item.vmp is not null "
                + " and (ibs.itemBatch.item.name) like :n )"
                + " order by i.name ";
        m.put("t", Amp.class);
        m.put("d", d);
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    public List<Stock> completeAvailableStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("depTp", DepartmentType.Store);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i"
                    + " where i.stock >:s"
                    + " and i.department=:d "
                    + " and i.itemBatch.item.departmentType is null "
                    + " or i.itemBatch.item.departmentType!=:depTp "
                    + " and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n "
                    + " or (i.itemBatch.item.barcode) like :n )  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i "
                    + " where i.stock >:s "
                    + " and i.department=:d"
                    + " and i.itemBatch.item.departmentType is null "
                    + " or i.itemBatch.item.departmentType!=:depTp "
                    + "  and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n)  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        items = getStockFacade().findByJpql(sql, m, 20);
        itemsWithoutStocks = completeRetailSaleItems(qry);
        //////// // System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return items;
    }

    public List<Stock> completeAvailableStocksStore(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("depTp", DepartmentType.Store);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i"
                    + " where i.stock >:s"
                    + " and i.department=:d "
                    + " and i.itemBatch.item.departmentType=:depTp "
                    + " and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n "
                    + " or (i.itemBatch.item.barcode) like :n )  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i "
                    + " where i.stock >:s "
                    + " and i.department=:d"
                    + " and i.itemBatch.item.departmentType=:depTp "
                    + "  and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n)  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        items = getStockFacade().findByJpql(sql, m, 20);
        //  itemsWithoutStocks = completeRetailSaleItems(qry);
        //////// // System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return items;
    }

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

    private void savePreBillFinally(Patient pt, Department matrixDepartment, BillType billType, BillTypeAtomic billTypeAtomic) {
        getPreBill().setBillType(billType);
        getPreBill().setBillTypeAtomic(billTypeAtomic);
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getLoggedUser().getDepartment(), billTypeAtomic);
        getPreBill().setInsId(deptId);
        getPreBill().setDeptId(deptId);

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setPatient(pt);
        getPreBill().setPatientEncounter(getPatientEncounter());
        getPreBill().setToDepartment(null);
        getPreBill().setToInstitution(null);
        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());

        getPreBill().setFromDepartment(matrixDepartment);
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        //TODO: What is this doing here. Need to investigate
        getBillBean().setSurgeryData(getPreBill(), getBatchBill(), SurgeryBillType.PharmacyItem);

        if (getPreBill().getId() == null) {
            getPreBill().setCreatedAt(Calendar.getInstance().getTime());
            getPreBill().setCreater(getSessionController().getLoggedUser());
            getBillFacade().create(getPreBill());
        } else {
            getBillFacade().edit(getPreBill());
        }

    }

    private void savePreBillFinallyRequest(Patient pt, Department matrixDepartment, BillType billType, BillNumberSuffix billNumberSuffix) {
        getPreBill().setBillType(billType);
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.PreBill, billNumberSuffix));
        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.PreBill, billNumberSuffix));

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
        getPreBill().setPatientEncounter(getPatientEncounter());

        getPreBill().setToDepartment(getDepartment());
        getPreBill().setToInstitution(getDepartment().getInstitution());
        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());

        getPreBill().setFromDepartment(matrixDepartment);
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setSurgeryData(getPreBill(), getBatchBill(), SurgeryBillType.PharmacyItem);

        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }

    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        if (getPreBill().getBillItems() == null) {
            getPreBill().setBillItems(new ArrayList<>());
        }

        for (BillItem tbi : list) {
            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());
            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());
            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            } else {
                getBillItemFacade().edit(tbi);
            }
        }

        if (!directIssueBatchService.validateBillForSettlement(getPreBill())) {
            String errorMsg = "One or more items have insufficient stock. Please refresh and try again.";
            LOGGER.log(Level.SEVERE, "Batch stock validation failed during BHT settlement for Bill ID: {0}",
                    getPreBill().getId());
            JsfUtil.addErrorMessage(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        try {
            directIssueBatchService.batchStockDeduction(list);
            LOGGER.log(Level.INFO, "Successfully processed batch stock deduction for {0} items in Bill ID: {1}",
                    new Object[]{list.size(), getPreBill().getId()});
        } catch (Exception e) {
            String errorMsg = "Failed to process stock deductions. " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Batch stock deduction failed during BHT settlement: {0}", errorMsg);
            LOGGER.log(Level.SEVERE, "Bill ID: {0}, Department: {1}, User: {2}",
                    new Object[]{
                        getPreBill().getId(),
                        getPreBill().getDepartment() != null ? getPreBill().getDepartment().getName() : "unknown",
                        getSessionController().getLoggedUser() != null
                            ? getSessionController().getLoggedUser().getName() : "unknown"
                    });
            JsfUtil.addErrorMessage(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        getBillFacade().edit(getPreBill());
    }

    /**
     * After stock is deducted from the issuing pharmacy, credit the same
     * quantities to the porter's staff stock (with stock history), so the
     * medicines are tracked as carried by the porter on the way to the ward.
     */
    private void transferIssuedStockToPorter(List<BillItem> list, Staff porter) {
        if (porter == null) {
            return;
        }
        for (BillItem tbi : list) {
            PharmaceuticalBillItem pbi = tbi.getPharmaceuticalBillItem();
            double qty = Math.abs(pbi.getQty());
            Stock staffStock = pharmacyBean.addToStock(pbi, qty, porter);
            pbi.setStaffStock(staffStock);
            getPharmaceuticalBillItemFacade().edit(pbi);
        }
    }

    private void savePreBillItemsFinallyRequest(List<BillItem> list) {
        // Initialize bill items list if null
        if (getPreBill().getBillItems() == null) {
            getPreBill().setBillItems(new ArrayList<>());
        }

        // PERFORMANCE OPTIMIZATION: Apply batch processing to request settlement (matching main settlement)
        List<BillItem> validItems = new ArrayList<>();

        // Step 1: Save all bill items first (with validation)
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {//If any issue in Stock Bill Item will not save & not include for total
                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());

            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);
            tbi.getPharmaceuticalBillItem().setBillItem(tbi);
            getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());

            getPreBill().getBillItems().add(tbi);
            validItems.add(tbi);
        }

        // Step 2: Batch validate stock availability before processing
        if (!directIssueBatchService.validateBillForSettlement(getPreBill())) {
            String errorMsg = "One or more items have insufficient stock for request settlement. Please refresh and try again.";
            LOGGER.log(Level.SEVERE, "Batch stock validation failed during BHT request settlement for Bill ID: {0}",
                    getPreBill().getId());
            JsfUtil.addErrorMessage(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // Step 3: Execute batch stock deduction (replaces individual deductFromStock calls)
        try {
            directIssueBatchService.batchStockDeduction(validItems);
            LOGGER.log(Level.INFO, "Successfully processed batch stock deduction for {0} items in Request Bill ID: {1}",
                    new Object[]{validItems.size(), getPreBill().getId()});
        } catch (Exception e) {
            String errorMsg = "Failed to process stock deductions for request settlement. " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Batch stock deduction failed during BHT request settlement: {0}", errorMsg);
            LOGGER.log(Level.SEVERE, "Request Bill ID: {0}, Department: {1}, User: {2}",
                    new Object[]{
                        getPreBill().getId(),
                        getPreBill().getDepartment() != null ? getPreBill().getDepartment().getName() : "unknown",
                        getSessionController().getLoggedUser() != null
                            ? getSessionController().getLoggedUser().getName() : "unknown"
                    });
            JsfUtil.addErrorMessage(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // PERFORMANCE OPTIMIZATION: UserStock cleanup removed to match cashier workflow
        // No longer needed since UserStock operations are eliminated

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    private boolean checkAllBillItem() {
        if (getPreBill().getBillItems() == null) {
            return true;
        }
        if (getPreBill().getBillItems().isEmpty()) {
            return true;
        }
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

    public void settlePharmacyBhtIssue() {
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }
        if (errorCheck()) {
            return;
        }
        if (hasAllergyConflicts(getPreBill().getBillItems())) {
            return;
        }
        // Guarantee the inward price matrix IS applied even if a previous discharge
        // bill left dischargeIssueMode on in this @SessionScoped controller. Force
        // matrix mode and re-price every line before saving. (PR #21330 review)
        dischargeIssueMode = false;
        for (BillItem bi : getPreBill().getBillItems()) {
            calculateRates(bi);
        }
        calTotal();
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE;
        BillType bt = BillType.PharmacyBhtPre;
        Department matrixDept = null;
        boolean matrixByAdmissionDepartment;
        boolean matrixByIssuingDepartment;
        matrixByAdmissionDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Inpatient Department for " + sessionController.getDepartment().getName(), true);
        matrixByIssuingDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Issuing Department for " + sessionController.getDepartment().getName(), true);

        if (matrixByAdmissionDepartment) {
            if (getPatientEncounter() == null) {
                matrixDept = getSessionController().getDepartment();
            } else if (getPatientEncounter().getCurrentPatientRoom() == null) {
                matrixDept = getSessionController().getDepartment();
            } else if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                matrixDept = getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
            }

        } else if (matrixByIssuingDepartment) {
            matrixDept = getSessionController().getDepartment();
        } else {
            matrixDept = getSessionController().getDepartment();
        }

        settleBhtIssue(bt, bta, matrixDept);

    }

    /**
     * Settle an Issue Discharge Medicines bill. Identical to settlePharmacyBhtIssue()
     * except it uses the DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE atomic. The inward
     * price-matrix service charge is already suppressed at add-item time because
     * dischargeIssueMode is on (see calculateRates), so each item is issued at
     * retail rate with zero margin. The bill is still counted as an inward medicine
     * issue in all reports (the new atomic is listed alongside DIRECT_ISSUE_INWARD_MEDICINE).
     */
    public void settlePharmacyDischargeIssue() {
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }
        if (errorCheck()) {
            return;
        }
        if (hasAllergyConflicts(getPreBill().getBillItems())) {
            return;
        }
        // Guarantee no inward service charge regardless of how the page was reached
        // (e.g. opened/bookmarked directly without resetAllForDischargeIssue()).
        // Force discharge mode on and re-price every line at bare retail before saving.
        dischargeIssueMode = true;
        for (BillItem bi : getPreBill().getBillItems()) {
            calculateRates(bi);
        }
        calTotal();
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE;
        BillType bt = BillType.PharmacyBhtPre;
        // Matrix department is irrelevant for margin here (no matrix is applied),
        // but settleBhtIssue requires a non-null department for bill metadata.
        Department matrixDept = determineMatrixDepartment();
        settleBhtIssue(bt, bta, matrixDept);
    }

    private Department determineMatrixDepartment() {
        Department matrixDept = null;
        boolean matrixByAdmissionDepartment;
        boolean matrixByIssuingDepartment;
        matrixByAdmissionDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Inpatient Department for " + sessionController.getDepartment().getName(), true);
        matrixByIssuingDepartment = configOptionApplicationController.getBooleanValueByKey("Price Matrix is calculated from Issuing Department for " + sessionController.getDepartment().getName(), true);

        if (matrixByAdmissionDepartment) {
            if (getPatientEncounter() == null) {
                matrixDept = getSessionController().getDepartment();
            } else if (getPatientEncounter().getCurrentPatientRoom() == null) {
                matrixDept = getPatientEncounter().getDepartment();
            } else if (getPatientEncounter().getCurrentPatientRoom() != null) {
                if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                    matrixDept = getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
                }
            }
        } else if (matrixByIssuingDepartment) {
            matrixDept = getSessionController().getDepartment();
        } else {
            matrixDept = getSessionController().getDepartment();
        }
        return matrixDept;
    }

    public void settlePharmacyBhtIssueAccept() {
        if (errorCheck()) {
            return;
        }
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing To Settle.");
            return;
        }
        
        if (bhtRequestBill == null) {
            JsfUtil.addErrorMessage("No BHT request selected.");
            return;
        }

        if( bhtRequestBill.isCompleted()){
            JsfUtil.addErrorMessage("This request has already been completed..");
            return;
        }

        if (isFullyIssued(bhtRequestBill)) {
            JsfUtil.addErrorMessage("This request has already been fully issued.");
            return;
        }

        if (hasAllergyConflicts(getBillItems())) {
            return;
        }

        if (getPreBill().getToStaff() == null) {
            JsfUtil.addErrorMessage("Please select the staff member (porter) who will carry the medicines to the ward.");
            return;
        }

        BillTypeAtomic bta = BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD;
        BillType bt = BillType.PharmacyBhtPre;

        Department matrixDept = determineMatrixDepartment();

        List<BillItem> issuedBillItems = new ArrayList<>(getBillItems());

        if (!settleBhtIssueRequestAccept(bt, bta, matrixDept, BillNumberSuffix.PHISSUE)) {
            return;
        }

        // Update remainingQty on the original request items using DB-derived issued total
        for (BillItem tbi : issuedBillItems) {
            BillItem refItem = tbi.getReferanceBillItem();
            if (refItem == null || refItem.getId() == null) {
                continue;
            }
            BillItem freshRefItem = billItemFacade.findWithoutCache(refItem.getId());
            freshRefItem.setRemainingQty(getRemainingQuantityForItem(freshRefItem));
            billItemFacade.editAndCommit(freshRefItem);
        }

        // Auto-complete the request once everything has been issued
        if (!bhtRequestBill.isFullyIssued() && isFullyIssued(bhtRequestBill)) {
            Bill freshRequestBill = getBillFacade().findWithoutCache(bhtRequestBill.getId());
            freshRequestBill.setFullyIssued(true);
            freshRequestBill.setFullyIssuedAt(new Date());
            freshRequestBill.setFullyIssuedBy(sessionController.getLoggedUser());
            getBillFacade().edit(freshRequestBill);
            bhtRequestBill.setFullyIssued(true);
            bhtRequestBill.setFullyIssuedAt(freshRequestBill.getFullyIssuedAt());
            bhtRequestBill.setFullyIssuedBy(freshRequestBill.getFullyIssuedBy());
        }

        //update Bill
        if (completed && webUserController.hasPrivilege(Privileges.PharmacyBhtRequestForceComplete.toString())) {
            bhtRequestBill.setCompleted(true);
            bhtRequestBill.setCompletedAt(new Date());
            bhtRequestBill.setCompletedBy(sessionController.getLoggedUser());

            billFacade.edit(bhtRequestBill);
        }
        completed = false;
        userNotificationController.userNotificationRequestComplete();

    }

    public void settlePharmacyBhtIssueRequest() {
        if (errorCheck()) {
            return;
        }
        if (hasAllergyConflicts(getPreBill().getBillItems())) {
            return;
        }
        settleBhtIssueRequest(BillType.InwardPharmacyRequest, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), BillNumberSuffix.PHISSUEREQ);

    }

    public void settleStoreBhtIssue() {
        if (errorCheck()) {
            return;
        }
        if (hasAllergyConflicts(getPreBill().getBillItems())) {
            return;
        }
        BillTypeAtomic bta = BillTypeAtomic.DIRECT_ISSUE_STORE_INWARD;
        BillType bt = BillType.StoreBhtPre;
        settleBhtIssue(bt, bta, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment());
    }

    private boolean errorCheck() {
        if (getPatientEncounter() == null || getPatientEncounter().getPatient() == null) {
            JsfUtil.addErrorMessage("Please Select a BHT");
            return true;
        }

        if (getPatientEncounter().getAdmissionType().isRoomChargesAllowed() || getPatientEncounter().getCurrentPatientRoom() != null) {

            if (getPatientEncounter().getCurrentPatientRoom() == null) {
                JsfUtil.addErrorMessage("Please Select Patient Room");
                return true;
            }

            if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() == null) {
                JsfUtil.addErrorMessage("Please Set Room");
                return true;
            }

        }

        if (getPatientEncounter().isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot issue medicines: nursing discharge has already been confirmed for this patient.");
            return true;
        }

        if (getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return true;
        }

        if (getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Sorry this BHT was Settled !!!");
            return true;
        }
        for (BillItem bi : getPreBill().getBillItems()) {
            if (bi.getItem() == null) {
                JsfUtil.addErrorMessage("Requested item could not empty" + bi.getItem().getName());
                return true;
            }
            if (bi.getPharmaceuticalBillItem() == null) {
                JsfUtil.addErrorMessage("Requested item not found" + bi.getItem().getName());
                return true;
            }
            if (bi.getPharmaceuticalBillItem().getStock() == null) {
                JsfUtil.addErrorMessage("Requested item not found" + bi.getItem().getName());
                return true;
            }
            if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
                JsfUtil.addErrorMessage("Please edit the item quantity to save" + bi.getItem().getName());
                return true;
            }

        }

//        if (checkAllBillItem()) {
//            //  UtilityController.addErrorMessage("Please Set Room 33");
//            return true;
//        }
        return false;
    }

    private boolean hasAllergyConflicts(List<BillItem> items) {
        if (!configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            return false;
        }
        if (items == null || items.isEmpty()) {
            return false;
        }
        PatientEncounter encounter = getPatientEncounter();
        Patient patient = encounter != null ? encounter.getPatient() : null;
        if (patient == null) {
            return false;
        }
        if (pharmacyService == null) {
            return false;
        }
        if (allergyListOfPatient == null) {
            allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
        }
        String allergyMsg = pharmacyService.isAllergyForPatient(patient, items, allergyListOfPatient);
        if (!allergyMsg.isEmpty()) {
            JsfUtil.addErrorMessage(allergyMsg);
            return true;
        }
        return false;
    }

    private void settleBhtIssue(BillType btp, BillTypeAtomic bta, Department matrixDepartment) {
        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        List<BillItem> itemsToIssue = getPreBill().getBillItems();
        if (hasAllergyConflicts(itemsToIssue)) {
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = new ArrayList<>(itemsToIssue);

        try {
            savePreBillFinally(pt, matrixDepartment, btp, bta);
            savePreBillItemsFinally(tmpBillItems);
            billService.createBillFinancialDetailsForInpatientDirectIssueBill(getPreBill());
//        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());
            setPrintBill(getBillFacade().find(getPreBill().getId()));
            clearBill();
            clearBillItem();
            billPreview = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during BHT settlement for patient encounter: {0}",
                    new Object[]{getPatientEncounter() != null ? getPatientEncounter().getId() : "unknown"});
            LOGGER.log(Level.SEVERE, "Settlement failure details", e);
            JsfUtil.addErrorMessage("Failed to settle bill. Please try again. Error: " + e.getMessage());
            // DO NOT clear the bill - keep items visible so user doesn't lose their work
        }

    }

    private void settleBhtIssueRequest(BillType btp, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {

        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return;
        }

        List<BillItem> itemsToIssue = getPreBill().getBillItems();
        if (hasAllergyConflicts(itemsToIssue)) {
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        // Create a proper copy of the list to avoid reference issues
        List<BillItem> tmpBillItems = new ArrayList<>(itemsToIssue);

        savePreBillFinallyRequest(pt, matrixDepartment, btp, billNumberSuffix);
        savePreBillItemsFinallyRequest(tmpBillItems);
        billService.createBillFinancialDetailsForInpatientDirectIssueBill(getPreBill());

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    public void removeBillItem(BillItem b) {
        // PERFORMANCE OPTIMIZATION: UserStock cleanup removed to match cashier workflow
        // No longer needed since UserStock operations are eliminated
        getPreBill().getBillItems().remove(b.getSearialNo());

        calTotal();
    }
    
    private BillItem itemForSubstitution;
    private Stock selectedSubstituteStock;
    private List<Stock> substituteStocks;
    
    private boolean completed;
    
    @Inject
    VmpController vmpController;
    @EJB
    PharmacyCostingService pharmacyCostingService;
    
    public void prepareSubstitute(BillItem bi) {
        itemForSubstitution = bi;
        selectedSubstituteStock = null;
        substituteStocks = new ArrayList<>();
        if (bi == null || bi.getItem() == null) {
            return;
        }
        List<Amp> amps = pharmacyBean.resolveAmps(bi.getItem());
        Date currentDate = new Date();
        for (Amp substituteAmp : amps) {
            List<Stock> stocks = pharmacyBean.getStockByQty(substituteAmp, sessionController.getDepartment());
            if (stocks != null) {
                for (Stock stock : stocks) {
                    if (stock.getStock() > 0
                            && stock.getItemBatch() != null
                            && stock.getItemBatch().getDateOfExpire() != null
                            && stock.getItemBatch().getDateOfExpire().after(currentDate)) {
                        substituteStocks.add(stock);
                    }
                }
            }
        }
    }
    
    public void replaceSelectedSubstitute() {
        if (itemForSubstitution == null || selectedSubstituteStock == null) {
            JsfUtil.addErrorMessage("Please select a substitute stock.");
            return;
        }

        // Update the bill item with selected stock details
        itemForSubstitution.setItem(selectedSubstituteStock.getItemBatch().getItem());

        PharmaceuticalBillItem phItem = itemForSubstitution.getPharmaceuticalBillItem();
        if (phItem == null) {
            phItem = new PharmaceuticalBillItem();
            phItem.setBillItem(itemForSubstitution);
            itemForSubstitution.setPharmaceuticalBillItem(phItem);
        }

        // Set stock and batch details
        phItem.setStock(selectedSubstituteStock);
        phItem.setItemBatch(selectedSubstituteStock.getItemBatch());
        phItem.setDoe(selectedSubstituteStock.getItemBatch().getDateOfExpire());
        phItem.setPurchaseRate(selectedSubstituteStock.getItemBatch().getPurcahseRate());
        phItem.setRetailRateInUnit(selectedSubstituteStock.getItemBatch().getRetailsaleRate());

        // Update rates in pharmaceutical bill item
        phItem.setPurchaseRatePack(selectedSubstituteStock.getItemBatch().getPurcahseRate());
        phItem.setRetailRatePack(selectedSubstituteStock.getItemBatch().getRetailsaleRate());
        phItem.setCostRate(selectedSubstituteStock.getItemBatch().getCostRate());
        phItem.setCostRatePack(selectedSubstituteStock.getItemBatch().getCostRate());

        // Update financials
        BillItemFinanceDetails financeDetails = itemForSubstitution.getBillItemFinanceDetails();
        if (financeDetails != null) {
            BigDecimal transferRate = determineTransferRate(selectedSubstituteStock.getItemBatch());
            financeDetails.setLineGrossRate(transferRate);
            financeDetails.setLineNetRate(transferRate);

            // Update cost and retail rates
            financeDetails.setLineCostRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getCostRate()));
            financeDetails.setRetailSaleRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getRetailsaleRate()));

            // Update values at different rates
            BigDecimal qty = financeDetails.getQuantity() != null ? financeDetails.getQuantity() : BigDecimal.ONE;
            financeDetails.setValueAtCostRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getCostRate()).multiply(qty));
            financeDetails.setValueAtPurchaseRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getPurcahseRate()).multiply(qty));
            financeDetails.setValueAtRetailRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getRetailsaleRate()).multiply(qty));
        }

        calculateBillTotalsForTransferIssue(getPreBill());

        JsfUtil.addSuccessMessage("Stock replaced successfully.");
    }
    
    private void calculateBillTotalsForTransferIssue(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return;
        }

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        int serialNo = 1;

        for (BillItem bi : bill.getBillItems()) {
            if (bi.isRetired()) {
                continue;
            }

            bi.setSearialNo(serialNo++);

            // For transfer issue: stock goes out so qty is negative
            double absQty = Math.abs(bi.getQty());
            bi.setQty(-absQty);

            // Revenue is positive (we receive money/value for stock going out)
            double netValue = absQty * bi.getRate();
            bi.setNetValue(netValue);

            grossTotal = grossTotal.add(BigDecimal.valueOf(netValue));
            netTotal = netTotal.add(BigDecimal.valueOf(netValue));
            lineGrossTotal = lineGrossTotal.add(BigDecimal.valueOf(netValue));
            lineNetTotal = lineNetTotal.add(BigDecimal.valueOf(netValue));
        }

        // Set bill totals as positive (revenue)
        bill.setTotal(grossTotal.doubleValue());
        bill.setNetTotal(netTotal.doubleValue());

        // Set bill finance details totals as positive (revenue)
        if (bill.getBillFinanceDetails() != null) {
            bill.getBillFinanceDetails().setGrossTotal(grossTotal);
            bill.getBillFinanceDetails().setLineGrossTotal(lineGrossTotal);
            bill.getBillFinanceDetails().setNetTotal(netTotal);
            bill.getBillFinanceDetails().setLineNetTotal(lineNetTotal);
        }

//        getBillFacade().edit(bill);
    }
    
    public double getRemainingQuantityForItem(BillItem referenceItem) {
        if (referenceItem == null || referenceItem.getId() == null) {
            return 0.0;
        }
        // Issue bill items reference the request item directly. Cancellation/return
        // bill items reference the issue bill item (one level further), so their
        // qty is netted off via that second reference hop. See BhtIssueReturnController
        // and PharmacyBillSearch#cancelPharmacyRequestIssueToBht for the chains.
        String jpql = "SELECT SUM("
                + "CASE WHEN bi.bill.billTypeAtomic = :issueBta THEN ABS(bi.qty) "
                + "ELSE -ABS(bi.qty) END) FROM BillItem bi "
                + "WHERE ((bi.bill.billTypeAtomic = :issueBta AND bi.referanceBillItem.id = :refId) "
                + "OR (bi.bill.billTypeAtomic IN :reverseBtas AND bi.referanceBillItem.referanceBillItem.id = :refId)) "
                + "AND (bi.bill.retired = false OR bi.bill.retired IS NULL)";
        Map<String, Object> params = new HashMap<>();
        params.put("refId", referenceItem.getId());
        params.put("issueBta", BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
        params.put("reverseBtas", Arrays.asList(
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN));
        double alreadyIssued = getBillItemFacade().findDoubleByJpql(jpql, params);
        return Math.max(0.0, referenceItem.getQty() - alreadyIssued);
    }

    public boolean isFullyIssued(Bill requestBill) {
        if (requestBill == null) {
            return false;
        }
        Bill freshBill = getBillFacade().findWithoutCache(requestBill.getId());
        if (freshBill == null || freshBill.getBillItems() == null || freshBill.getBillItems().isEmpty()) {
            return false;
        }
        for (BillItem item : freshBill.getBillItems()) {
            if (getRemainingQuantityForItem(item) > 0.001) {
                return false;
            }
        }
        return true;
    }


    private BigDecimal determineTransferRate(ItemBatch itemBatch) {
        if (itemBatch == null) {
            return BigDecimal.ZERO;
        }

        boolean pharmacyTransferIsByPurchaseRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean pharmacyTransferIsByCostRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
        boolean pharmacyTransferIsByRetailRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate", true);

        if (pharmacyTransferIsByPurchaseRate) {
            return BigDecimal.valueOf(itemBatch.getPurcahseRate());
        } else if (pharmacyTransferIsByCostRate) {
            return BigDecimal.valueOf(itemBatch.getCostRate());
        } else {
            return BigDecimal.valueOf(itemBatch.getRetailsaleRate());
        }
    }
    
    private boolean settleBhtIssueRequestAccept(BillType btp, BillTypeAtomic bta, Department matrixDepartment, BillNumberSuffix billNumberSuffix) {

        if (matrixDepartment == null) {
            JsfUtil.addErrorMessage("This Bht can't issue as this Surgery Has No Department");
            return false;
        }
        List<BillItem> tmpBillItems = getBillItems();

        if (hasAllergyConflicts(tmpBillItems)) {
            return false;
        }

        if (!getBillItems().isEmpty()) {
            getPreBill().setReferenceBill(getBillItems().get(0).getReferanceBillItem().getBill());
        }

        for (BillItem tbi : tmpBillItems) {
            if (tbi.getPharmaceuticalBillItem().getQty() == 0.0) {
                JsfUtil.addErrorMessage("Item Qty is Zero " + tbi.getItem().getName());
                return false;
            }
            Stock tbiStock = tbi.getPharmaceuticalBillItem().getStock();
            if (tbiStock == null) {
                JsfUtil.addErrorMessage("No stock available for " + tbi.getItem().getName() + ". Please check pharmacy stock.");
                return false;
            }
            if (Math.abs(tbi.getPharmaceuticalBillItem().getQty()) > tbiStock.getStock()) {
                JsfUtil.addErrorMessage("Not Enough Stock " + tbi.getItem().getName());
                return false;
            }
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        // No need to clear billItems - let savePreBillItemsFinally handle it properly

        savePreBillFinally(pt, matrixDepartment, btp, bta);
        savePreBillItemsFinally(tmpBillItems);
        transferIssuedStockToPorter(tmpBillItems, getPreBill().getToStaff());
        billService.createBillFinancialDetailsForInpatientDirectIssueBill(getPreBill());

        // Calculation Margin
        updateMargin(getPreBill().getBillItems(), getPreBill(), getPreBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());
        //pdateBillTotals(getPreBill().getBillItems(),  getPreBill());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

        return true;
    }

    /**
     * The room category of the patient's current room, or null when the patient
     * is not in a room (or the room has no facility charge / category). Drives the
     * room-category dimension of the inward pharmacy-margin matrix (issue #21981);
     * null means "wildcard row only", preserving legacy behaviour.
     */
    private RoomCategory resolveCurrentRoomCategory(PatientEncounter encounter) {
        if (encounter == null
                || encounter.getCurrentPatientRoom() == null
                || encounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            return null;
        }
        return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getRoomCategory();
    }

    public void updateMargin(BillItem bi, Department matrixDepartment, PaymentMethod paymentMethod) {
        double rate = Math.abs(bi.getRate());
        double margin;
        PatientEncounter encounter = bi.getBill() != null ? bi.getBill().getPatientEncounter() : null;
        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod, null,
                encounter != null ? encounter.getAdmissionType() : null, resolveCurrentRoomCategory(encounter));
        if (priceMatrix != null) {
            margin = ((bi.getGrossValue() * priceMatrix.getMargin()) / 100);
            bi.setMarginRate((bi.getRate() * (priceMatrix.getMargin() + 100)) / 100);
        } else {
            margin = 0.0;
            bi.setMarginRate(0.0);
        }

        bi.setMarginValue(margin);
        bi.setNetValue(bi.getGrossValue() + bi.getMarginValue());
        bi.setAdjustedValue(bi.getNetValue());
    }

    @Deprecated
    public void updateMargin(List<BillItem> billItems, Bill bill, Department matrixDepartment, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
        double marginTotal = 0;
        PatientEncounter encounter = bill != null ? bill.getPatientEncounter() : null;
        for (BillItem bi : billItems) {

            double rate = Math.abs(bi.getRate());
            double margin;

            PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod, null,
                    encounter != null ? encounter.getAdmissionType() : null, resolveCurrentRoomCategory(encounter));

            if (priceMatrix != null) {
                margin = ((bi.getGrossValue() * priceMatrix.getMargin()) / 100);
                bi.setMarginRate((bi.getRate() * (priceMatrix.getMargin() + 100)) / 100);
            } else {
                margin = 0.0;
                bi.setMarginRate(0.0);
            }

            bi.setMarginValue(margin);

            bi.setNetValue(bi.getGrossValue() + bi.getMarginValue());
            bi.setAdjustedValue(bi.getNetValue());
            getBillItemFacade().edit(bi);

            total += bi.getGrossValue();
            netTotal += bi.getNetValue();
            marginTotal += bi.getMarginValue();
        }

        bill.setTotal(total);
        bill.setNetTotal(netTotal);
        bill.setMargin(marginTotal);
        getBillFacade().edit(bill);

    }

    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private InwardBeanController inwardBean;

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    private boolean checkItemBatch() {
        // PERFORMANCE OPTIMIZATION: Use selectedStockId to avoid entity loading
        if (selectedStockId == null) {
            return false;
        }

        for (BillItem bItem : getPreBill().getBillItems()) {
            if (bItem.getPharmaceuticalBillItem() != null &&
                bItem.getPharmaceuticalBillItem().getStock() != null &&
                Objects.equals(bItem.getPharmaceuticalBillItem().getStock().getId(), selectedStockId)) {
                return true;
            }
        }

        return false;
    }

    public void addBillItem() {

        if (getPreBill() == null) {
            JsfUtil.addErrorMessage("No Prebill");
            return;
        }
        if (getBillItem() == null) {
            JsfUtil.addErrorMessage("No Bill Item");
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem() == null) {
            JsfUtil.addErrorMessage("No Pharmaceutical Bill Item");
            return;
        }
        if (selectedStockDto == null || selectedStockId == null) {
            JsfUtil.addErrorMessage("No Stock Selected");
            return;
        }
        if (getQty() == null || getQty() <= 0.0) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Please enter a Quantity?");
            return;
        }
        if (selectedStockDto.getDateOfExpire() != null
                && selectedStockDto.getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("You are NOT allowed to select Expired Items");
            return;
        }
        if (getQty() > selectedStockDto.getStockQty()) {
            errorMessage = "No Sufficient Stocks?";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        // Use getReference() — a thin, uninitialised proxy — instead of find().
        // find(ItemBatch/Stock) triggers a cascading eager load (~46 SQL queries,
        // ~20s first time). We only need the ID here; rates come from the DTO.
        // Issue #20138.
        Stock stockRef = getStockFacade().getReference(selectedStockId);
        ItemBatch itemBatchRef = getItemBatchFacade().getReference(selectedStockDto.getItemBatchId());

        billItem.getPharmaceuticalBillItem().setStock(stockRef);

        double dtoRetailRate = selectedStockDto.getRetailRate() != null ? selectedStockDto.getRetailRate() : 0.0;
        double dtoPurchaseRate = selectedStockDto.getPurchaseRate() != null ? selectedStockDto.getPurchaseRate() : 0.0;
        billItem.getPharmaceuticalBillItem().setItemBatchWithRates(itemBatchRef, dtoRetailRate, dtoPurchaseRate);

        Item currentItem = billItem.getItem();
        if (currentItem == null && selectedStockDto.getItemId() != null) {
            // Use getReference — a proxy — instead of find. BillItem.setItem()
            // has no side effects, so the cascading eager load of Item never fires.
            billItem.setItem(getItemFacade().getReference(selectedStockDto.getItemId()));
        }

        if (checkItemBatch()) {
            errorMessage = "Already added this item batch";
            JsfUtil.addErrorMessage("Already added this item batch");
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
            List<ClinicalFindingValue> allergyListOfPatient = pharmacyService.getAllergyListForPatient(patientEncounter.getPatient());
            List<BillItem> billItems = new ArrayList<>();
            billItems.add(billItem);
            if (allergyListOfPatient != null && !allergyListOfPatient.isEmpty()) {
                String allergyMsg = pharmacyService.isAllergyForPatient(patientEncounter.getPatient(), billItems, allergyListOfPatient);
                if (!allergyMsg.isEmpty()) {
                    JsfUtil.addErrorMessage(allergyMsg);
                    clearBillItem();
                    return;
                }
            }
        }

        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - Math.abs(qty));
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));
        billItem.setQty(qty);
        billItem.getPharmaceuticalBillItem().setDoe(selectedStockDto.getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setTransDisplayItemName(selectedStockDto.getItemName());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));

        calculateRates(billItem, selectedStockDto.getRetailRate());

        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setBill(getPreBill());
        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        calTotal();
        clearBillItem();
        errorMessage = "";
    }

    private void calTotal() {
        if (getPreBill().getBillItems() == null) {
            return;
        }
        getPreBill().setTotal(0);
        double netTot = 0.0;
        double discount = 0.0;
        double grossTot = 0.0;
        double marginTotal = 0.0;
        int index = 0;
        for (BillItem b : getPreBill().getBillItems()) {
            if (b.isRetired()) {
                continue;
            }
            b.setSearialNo(index++);
            netTot += b.getNetValue();
            grossTot += b.getGrossValue();
            discount += b.getDiscount();
            marginTotal += b.getMarginValue();
        }
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        getPreBill().setMargin(marginTotal);
    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void addBillItemNew() {
        errorMessage = null;

        billItem = new BillItem();
        PharmaceuticalBillItem pharmaceuticalBillItem = new PharmaceuticalBillItem();
        pharmaceuticalBillItem.setBillItem(billItem);
        billItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);
        if (getTmpStock() == null) {
            errorMessage = "Item?";
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        Stock loadedTmpStock = stockFacade.findWithItemBatch(tmpStock.getId());
        if (loadedTmpStock == null) {
            errorMessage = "Selected stock is no longer available.";
            JsfUtil.addErrorMessage("Selected stock is no longer available.");
            return;
        }
        tmpStock = loadedTmpStock;
        if (tmpStock.getItemBatch() != null
                && tmpStock.getItemBatch().getDateOfExpire() != null
                && tmpStock.getItemBatch().getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("Please not select Expired Items");
            return;
        }
        if (getQty() == null) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }
        if (getQty() == 0.0) {
            errorMessage = "Quantity Zero?";
            JsfUtil.addErrorMessage("Quentity Zero?");
            return;
        }
        if (getQty() > getTmpStock().getStock()) {
            errorMessage = "No sufficient stocks.";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

//        if (checkItemBatch()) {
//            errorMessage = "This batch is already there in the bill.";
//            UtilityController.addErrorMessage("Already added this item batch");
//            return;
//        }
//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            UtilityController.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getTmpStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }

        billItem.getPharmaceuticalBillItem().setQtyInUnit(qty);
        billItem.getPharmaceuticalBillItem().setStock(getTmpStock());
        billItem.getPharmaceuticalBillItem().setItemBatch(getTmpStock().getItemBatch());

//        calculateBillItem();
        ////System.out.println("Rate*****" + billItem.getRate());
        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getTmpStock().getItemBatch().getItem());
        billItem.setQty(qty);
//        billItem.setBill(getPreBill());
        billItem.setSearialNo(getBillItems().size() + 1);
        calculateRates(billItem);
        getBillItems().add(billItem);

        calCurrentBillItemTotal(getBillItems());

        qty = null;
        tmpStock = null;
        setActiveIndex(1);
    }

    public void removeBillItemFromBhtRequest(BillItem b) {
        if (b == null) {
            JsfUtil.addErrorMessage("Please selct item");
            return;
        }
        if (getBillItems() == null) {
            JsfUtil.addErrorMessage("No items in the bill");
            return;
        }
        getBillItems().remove(b);

        calTotal();
    }

    public void calculateBillItemListner(AjaxBehaviorEvent event) {
        // PERFORMANCE OPTIMIZATION: Use DTO-based calculation for quantity changes
        if (selectedStockDto != null) {
            calculateBillItemFromDto();
        } else {
            // Fallback to entity-based calculation if DTO not available
            calculateBillItem();
        }
    }

    /**
     * DTO-based calculation for quantity changes - fast, no entity loading
     */
    public void calculateBillItemFromDto() {
        if (selectedStockDto == null || getBillItem() == null || getQty() == null) {
            return;
        }

        // Quick DTO-based calculation (no entity loading)
        double rate = selectedStockDto.getRetailRate();
        double quantity = getQty();
        double grossValue = rate * quantity;

        // Update BillItem with basic values for immediate display
        getBillItem().setQty(quantity);
        getBillItem().setRate(rate);
        getBillItem().setGrossValue(grossValue);
        getBillItem().setNetValue(grossValue); // Will be recalculated with margins in addBillItem
        getBillItem().setMarginValue(0.0); // Simplified for quick display
        getBillItem().setNetRate(rate);
        getBillItem().setDiscount(0.0);

    }

    public void calculateBillItem() {
        // Use lazy loading getStock() method which handles both direct stock and DTO-based stock
        Stock stockEntity = getStock();
        if (stockEntity == null) {
            return;
        }
        if (getPreBill() == null) {
            return;
        }
        if (getBillItem() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem().getStock() == null) {
            getBillItem().getPharmaceuticalBillItem().setStock(stockEntity);
        }
        if (getQty() == null) {
            qty = 0.0;
        }

        //Bill Item
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));

        calculateRates(billItem);

    }

    public void calculateBillItemForEditing(BillItem bi) {
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        calculateRates(bi);
    }

    public void handleSelect(AjaxBehaviorEvent event) {
        handleSelect();
    }

    public void handleSelect(SelectEvent event) {
        handleSelect();
    }

    public void handleSelect() {
        if (getBillItem() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem() == null) {
            return;
        }

        // Use lazy loading getStock() method which handles both direct stock and DTO-based stock
        Stock stockEntity = getStock();
        if (stockEntity == null) {
            return;
        }

        getBillItem().getPharmaceuticalBillItem().setStock(stockEntity);
        if (getBillItem().getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return;
        }
        if (getBillItem().getPharmaceuticalBillItem().getStock().getItemBatch().getItem() == null) {
            return;
        }
        getBillItem().setItem(getBillItem().getPharmaceuticalBillItem().getStock().getItemBatch().getItem());
        calculateRates(getBillItem());
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

    @Deprecated
    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
            calculateBillItemForEditing(tbi);
        }
        calTotal();
    }

    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    private Department resolveMatrixDepartment() {
        // Read config once per prebill lifecycle (cleared on resetAll) rather than on every rate calc
        if (cachedMatrixByAdmissionDepartment == null) {
            cachedMatrixByAdmissionDepartment = configOptionApplicationController.getBooleanValueByKey(
                    "Price Matrix is calculated from Inpatient Department for "
                    + sessionController.getDepartment().getName(), true);
        }

        if (cachedMatrixByAdmissionDepartment) {
            if (getPatientEncounter() == null) {
                return getSessionController().getDepartment();
            }
            if (getPatientEncounter().getCurrentPatientRoom() == null) {
                return getPatientEncounter().getDepartment();
            }
            if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                return getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
            }
            return null;
        }
        return getSessionController().getDepartment();
    }

    public void calculateRates(BillItem bi) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null || bi.getItem() == null) {
            return;
        }

        if (bi.isFromPackage() && bi.getOverriddenRate() != null) {
            double packageRate = bi.getOverriddenRate();
            double quantity = bi.getQty() != null ? bi.getQty() : 0.0;
            bi.setRate(packageRate);
            bi.setGrossValue(packageRate * quantity);
            bi.setMarginValue(0.0);
            bi.setNetValue(packageRate * quantity);
            bi.setMarginRate(0.0);
            bi.setNetRate(packageRate);
            bi.setAdjustedValue(packageRate * quantity);
            bi.setDiscount(0);
            return;
        }

        if (selectedStockDto != null
                && bi.getPharmaceuticalBillItem().getStock() != null
                && Objects.equals(selectedStockDto.getId(), bi.getPharmaceuticalBillItem().getStock().getId())) {
            calculateRates(bi, selectedStockDto.getRetailRate());
            return;
        }

        if (bi.getPharmaceuticalBillItem().getStock() == null
                || bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            return;
        }

        long calcStartTime = LOGGER.isLoggable(Level.FINE) ? System.currentTimeMillis() : 0L;

        double originalRate;
        double estimatedValueBeforeAddingMarginToCalculateMatrix;
        double marginPercentage;
        double marginRate;
        double marginValue;
        double quantity;
        double grossValue;
        double netValue;

        Department matrixDept = resolveMatrixDepartment();

        quantity = bi.getQty();
        originalRate = bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate();
        estimatedValueBeforeAddingMarginToCalculateMatrix = originalRate * quantity;

        PaymentMethod paymentMethod = null;
        if (getPatientEncounter() != null) {
            paymentMethod = getPatientEncounter().getPaymentMethod();
        }

        PriceMatrix priceMatrix = null;
        // Discharge medicines are issued without the inward price matrix (no service charge).
        if (!dischargeIssueMode && bi.getItem() != null) {
            priceMatrix = getPriceMatrixController().fetchInwardMargin(
                    bi,
                    estimatedValueBeforeAddingMarginToCalculateMatrix,
                    matrixDept,
                    paymentMethod,
                    null,
                    getPatientEncounter() != null ? getPatientEncounter().getAdmissionType() : null,
                    resolveCurrentRoomCategory(getPatientEncounter())
            );
        }

        if (priceMatrix != null) {
            marginPercentage = priceMatrix.getMargin() / 100;
        } else {
            marginPercentage = 0.0;
        }

        marginRate = marginPercentage * originalRate;
        marginValue = marginRate * quantity;
        grossValue = originalRate * quantity;
        netValue = grossValue + marginValue;

        bi.setRate(originalRate);
        bi.setGrossValue(grossValue);
        bi.setMarginValue(marginValue);
        bi.setNetValue(netValue);
        bi.setMarginRate(marginRate);
        bi.setNetRate(originalRate + marginRate);
        bi.setAdjustedValue(netValue);
        bi.setDiscount(0);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "[calculateRates] TOTAL: {0}ms",
                    (System.currentTimeMillis() - calcStartTime));
        }
    }

    /**
     * Rate-aware overload: accepts retailRate from DTO to avoid loading the Stock/ItemBatch entity.
     */
    public void calculateRates(BillItem bi, double retailRate) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null) {
            return;
        }

        Department matrixDept = resolveMatrixDepartment();
        double quantity = bi.getQty();
        double estimatedValue = retailRate * quantity;
        PaymentMethod paymentMethod = getPatientEncounter() != null ? getPatientEncounter().getPaymentMethod() : null;

        PriceMatrix priceMatrix = null;
        // Discharge medicines are issued without the inward price matrix (no service charge).
        if (!dischargeIssueMode && bi.getItem() != null) {
            priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, estimatedValue, matrixDept, paymentMethod, null,
                    getPatientEncounter() != null ? getPatientEncounter().getAdmissionType() : null, resolveCurrentRoomCategory(getPatientEncounter()));
        }

        double marginPercentage = priceMatrix != null ? priceMatrix.getMargin() / 100 : 0.0;
        double marginRate = marginPercentage * retailRate;
        double grossValue = retailRate * quantity;
        double netValue = grossValue + marginRate * quantity;

        bi.setRate(retailRate);
        bi.setGrossValue(grossValue);
        bi.setMarginValue(marginRate * quantity);
        bi.setNetValue(netValue);
        bi.setMarginRate(marginRate);
        bi.setNetRate(retailRate + marginRate);
        bi.setAdjustedValue(netValue);
        bi.setDiscount(0);
    }

    /**
     * PHASE 2 OPTIMIZATION: Calculate rates using DTO data directly
     * Avoids heavy entity loading that was causing 14+ second delays
     */
    public void calculateRatesFromDto(BillItem bi, StockDTO stockDto) {
        if (bi == null || stockDto == null) {
            return;
        }

        double quantity = bi.getQty() != null ? bi.getQty() : 1.0;
        double originalRate = stockDto.getRetailRate();
        double grossValue = originalRate * quantity;

        // Display the base retail rate immediately; full margin is recalculated in addBillItem
        bi.setRate(originalRate);
        bi.setGrossValue(grossValue);
        bi.setMarginValue(0);
        bi.setNetValue(grossValue);
        bi.setMarginRate(0);
        bi.setNetRate(originalRate);
        bi.setAdjustedValue(grossValue);
        bi.setDiscount(0);
    }

    public List<Stock> completeAvailableStocksSelectedPharmacy(String qry) {
        if (department == null) {
            JsfUtil.addErrorMessage("Please Select Depatment");
            return new ArrayList<>();
        }

        String sql;
        Map m = new HashMap();
        m.put("d", department);
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n or (i.itemBatch.item.vmp.name) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.vmp.name) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }

        List<Stock> items = getStockFacade().findByJpql(sql, m, 20);

        if (qry.length() > 5 && items.size() == 1) {
            stock = items.get(0);
            replaceableStocks = new ArrayList<>();
            itemsWithoutStocks = new ArrayList<>();
            handleSelectAction();
        } else if (!qry.trim().isEmpty() && qry.length() > 4) {
            itemsWithoutStocks = completeRetailSaleItems(qry, department);
            if (itemsWithoutStocks != null && !itemsWithoutStocks.isEmpty()) {
                fillReplaceableStocksForAmp((Amp) itemsWithoutStocks.get(0));
            }
        }

        return items;
    }

    public String navigateToIssueMedicinesDirectlyForBhtRequest() {
        if (bhtRequestBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        if (isFullyIssued(bhtRequestBill)) {
            JsfUtil.addErrorMessage("This request has already been fully issued.");
            return "";
        }
        setCompleted(false);
        // The search-list render already initialized patientEncounter (and its
        // nested patient/person/room associations) on the session-stored entity.
        // Preserve it here because loadBillWithItemsFresh() does not join-fetch
        // patientEncounter, so the returned detached bill has an uninitialized proxy.
        PatientEncounter preservedEncounter = bhtRequestBill.getPatientEncounter();
        // Eager-fetch the bill with items, item details, and stock in one JOIN FETCH
        // query to avoid lazy-loading on the session-stored (detached) entity.
        Bill freshBill = getBillItemFacade().loadBillWithItemsFresh(bhtRequestBill.getId());
        if (freshBill == null) {
            JsfUtil.addErrorMessage("Request bill not found.");
            return "";
        }
        freshBill.setPatientEncounter(preservedEncounter);
        bhtRequestBill = freshBill;
        generateIssueBillComponentsForBhtRequest(freshBill);
        return "/ward/ward_pharmacy_bht_issue?faces-redirect=true";
    }

    public void generateIssueBillComponentsForBhtRequest(Bill b) {
        // This is a normal (matrix-priced) inward issue flow. Clear any sticky
        // discharge-issue mode left over from a previous discharge bill in the
        // same @SessionScoped controller so items are priced with the inward
        // price matrix, not at bare retail. (PR #21330 review)
        dischargeIssueMode = false;
        if (b == null) {
            JsfUtil.addErrorMessage("No bill");
            return;
        }
        if (b.getBillItems() == null) {
            JsfUtil.addErrorMessage("Bill Items Null");
            return;
        }
        if (b.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return;
        }

        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        setPatientEncounter(b.getPatientEncounter());
        billItems = new ArrayList<>();

        for (BillItem i : b.getBillItems()) {
            if (i.getItem() == null) {
                continue;
            }
            if (i.getQty() == null) {
                continue;
            }
            Item requestedItem = i.getItem();

            double billedIssue = getPharmacyCalculation().getBilledInwardPharmacyRequest(i, BillType.PharmacyBhtPre);
            double cancelledIssue = getPharmacyCalculation().getCancelledInwardPharmacyRequest(i, BillType.PharmacyBhtPre);
            double refundedIssue = getPharmacyCalculation().getRefundedInwardPharmacyRequest(i, BillType.PharmacyBhtPre);
            double issuableQty = Math.abs(i.getQty()) - (Math.abs(billedIssue) - (Math.abs(cancelledIssue) + Math.abs(refundedIssue)));

            // Resolve VTM/VMP/AMP/ATM to concrete AMP candidates with stock priority:
            // 1. Exact requested AMP  2. Same-strength sibling AMP  3. Any available AMP
            // For AMP requests also include VMP siblings so substitution can fire when
            // the exact brand is out of stock.
            List<Amp> candidateAmps = new ArrayList<>(pharmacyBean.resolveAmps(requestedItem));
            if (requestedItem instanceof Amp) {
                Vmp vmp = ((Amp) requestedItem).getVmp();
                if (vmp != null) {
                    List<Amp> siblings = pharmacyBean.findAmpsForVmp(vmp);
                    if (siblings != null) {
                        for (Amp sibling : siblings) {
                            if (!sibling.getId().equals(requestedItem.getId())) {
                                candidateAmps.add(sibling);
                            }
                        }
                    }
                }
            }
            Double requestedStrength = requestedItem.getStrengthOfAnIssueUnit();

            Amp exactAmp = null;
            List<StockQty> exactStockQtys = null;

            Amp sameStrengthAmp = null;
            List<StockQty> sameStrengthStockQtys = null;
            Date sameStrengthEarliestExpiry = null;

            Amp fallbackAmp = null;
            List<StockQty> fallbackStockQtys = null;
            Date fallbackEarliestExpiry = null;

            for (Amp candidate : candidateAmps) {
                Double ampStrength = candidate.getStrengthOfAnIssueUnit();
                double candidateQty;
                if (requestedStrength != null && requestedStrength > 0
                        && ampStrength != null && ampStrength > 0) {
                    candidateQty = Math.ceil(issuableQty * requestedStrength / ampStrength);
                } else {
                    candidateQty = issuableQty;
                }

                List<StockQty> stockQtys = pharmacyBean.getStockByQty((Item) candidate, candidateQty, getSessionController().getDepartment());
                if (stockQtys == null || stockQtys.isEmpty()) {
                    continue;
                }

                // getStockByQty returns batches ORDER BY dateOfExpire, so first entry is earliest
                Date candidateEarliestExpiry = null;
                StockQty first = stockQtys.get(0);
                if (first.getStock() != null && first.getStock().getItemBatch() != null) {
                    candidateEarliestExpiry = first.getStock().getItemBatch().getDateOfExpire();
                }

                boolean isExact = (requestedItem instanceof Amp)
                        && requestedItem.getId() != null
                        && requestedItem.getId().equals(candidate.getId());
                boolean isSameStrength = (requestedStrength == null || ampStrength == null)
                        || (requestedStrength.doubleValue() == ampStrength.doubleValue());

                if (isExact) {
                    exactAmp = candidate;
                    exactStockQtys = stockQtys;
                    break; // exact match is optimal
                } else if (isSameStrength
                        && (sameStrengthAmp == null
                        || (candidateEarliestExpiry != null && (sameStrengthEarliestExpiry == null
                        || candidateEarliestExpiry.before(sameStrengthEarliestExpiry))))) {
                    sameStrengthAmp = candidate;
                    sameStrengthStockQtys = stockQtys;
                    sameStrengthEarliestExpiry = candidateEarliestExpiry;
                } else if (!isSameStrength
                        && (fallbackAmp == null
                        || (candidateEarliestExpiry != null && (fallbackEarliestExpiry == null
                        || candidateEarliestExpiry.before(fallbackEarliestExpiry))))) {
                    fallbackAmp = candidate;
                    fallbackStockQtys = stockQtys;
                    fallbackEarliestExpiry = candidateEarliestExpiry;
                }
            }

            // Pick best available candidate
            final List<StockQty> selectedStockQtys;
            final boolean isSubstitute;

            if (exactAmp != null) {
                selectedStockQtys = exactStockQtys;
                isSubstitute = false;
            } else if (sameStrengthAmp != null) {
                selectedStockQtys = sameStrengthStockQtys;
                isSubstitute = true;
            } else if (fallbackAmp != null) {
                selectedStockQtys = fallbackStockQtys;
                isSubstitute = true;
            } else {
                selectedStockQtys = null;
                isSubstitute = false;
            }

            if (selectedStockQtys != null && !selectedStockQtys.isEmpty()) {
                for (StockQty sq : selectedStockQtys) {
                    if (sq.getQty() == 0) {
                        continue;
                    }
                    if (!userStockController.isStockAvailable(sq.getStock(), sq.getQty(), getSessionController().getLoggedUser())) {
                        JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
                        continue;
                    }
                    billItem = new BillItem();
                    billItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
                    billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - sq.getQty());
                    billItem.getPharmaceuticalBillItem().setQty(0 - sq.getQty());
                    billItem.getPharmaceuticalBillItem().setStock(sq.getStock());
                    billItem.getPharmaceuticalBillItem().setItemBatch(sq.getStock().getItemBatch());
                    billItem.setItem(sq.getStock().getItemBatch().getItem());
                    billItem.setQty(sq.getQty());
                    billItem.setDescreption(i.getDescreption());
                    billItem.getPharmaceuticalBillItem().setDoe(sq.getStock().getItemBatch().getDateOfExpire());
                    billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
                    billItem.getPharmaceuticalBillItem().setItemBatch(sq.getStock().getItemBatch());
                    billItem.setGrossValue(sq.getStock().getItemBatch().getRetailsaleRate() * sq.getQty());
                    billItem.setNetValue(sq.getQty() * sq.getStock().getItemBatch().getRetailsaleRate());
                    billItem.setInwardChargeType(InwardChargeType.Medicine);
                    billItem.getPharmaceuticalBillItem().setBillItem(billItem);
                    billItem.setReferanceBillItem(i);
                    if (i.isFromPackage()) {
                        billItem.setFromPackage(true);
                        billItem.setOverriddenRate(i.getOverriddenRate());
                        billItem.setSourcePackageItem(i.getSourcePackageItem());
                    }
                    billItem.setSearialNo(getBillItems().size() + 1);
                    if (isSubstitute) {
                        billItem.setAutoSubstituted(true);
                        billItem.setRequestedItemName(requestedItem.getName());
                    }
                    calculateRates(billItem);
                    billItems.add(billItem);
                }
            } else {
                // No stock found for any AMP — add placeholder for manual resolution
                billItem = new BillItem();
                billItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
                billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - issuableQty);
                billItem.getPharmaceuticalBillItem().setQty(0 - issuableQty);
                billItem.getPharmaceuticalBillItem().setStock(null);
                billItem.getPharmaceuticalBillItem().setItemBatch(null);
                billItem.setItem(requestedItem);
                billItem.setQty(issuableQty);
                billItem.setDescreption(i.getDescreption());
                billItem.setInwardChargeType(InwardChargeType.Medicine);
                billItem.setReferanceBillItem(i);
                if (i.isFromPackage()) {
                    billItem.setFromPackage(true);
                    billItem.setOverriddenRate(i.getOverriddenRate());
                    billItem.setSourcePackageItem(i.getSourcePackageItem());
                }
                billItem.setSearialNo(getBillItems().size() + 1);
                billItem.getPharmaceuticalBillItem().setBillItem(billItem);
                calculateRates(billItem);
                billItems.add(billItem);
            }
        }

        calCurrentBillItemTotal(billItems);
        getPreBill().setBillItems(billItems);

//        boolean flag = false;
//        for (BillItem bi : getBillItems()) {
//            if (Objects.equals(bi.getPharmaceuticalBillItem().getStock().getId(), stock.getId())) {
//                flag = true;
//                break;
//            }
//            stock = bi.getPharmaceuticalBillItem().getStock();
//        }
//
//        if (flag) {
//            billItems = null;
//            JsfUtil.addErrorMessage("There is Some Item in request that are added Multiple Time in Transfer request!!! please check request you can't issue errornus transfer request");
//        }
    }

    public void calCurrentBillItemTotal(List<BillItem> billItems) {
        billItemTotal = 0.0;
        for (BillItem bi : billItems) {
            billItemTotal += bi.getNetValue();
        }
    }

    public boolean checkBillComponent(Bill b) {

        boolean flag = false;
        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(b)) {
//            //// // System.out.println("i.getQtyInUnit() = " + i.getQtyInUnit());
            double billedIssue = getPharmacyCalculation().getBilledInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
//            //// // System.out.println("billedIssue = " + billedIssue);
            double cancelledIssue = getPharmacyCalculation().getCancelledInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
//            //// // System.out.println("cancelledIssue = " + cancelledIssue);
            double refundedIssue = getPharmacyCalculation().getRefundedInwardPharmacyRequest(i.getBillItem(), BillType.PharmacyBhtPre);
//            //// // System.out.println("refundedIssue = " + refundedIssue);

            double issuableQty = Math.abs(i.getQtyInUnit()) - (Math.abs(billedIssue) - (Math.abs(cancelledIssue) + Math.abs(refundedIssue)));
            if (issuableQty > 0) {
                flag = true;
            }

        }

        return flag;

    }

    public void handleSelectAction() {
        if (stock == null) {
            //////// // System.out.println("Stock NOT selected.");
        }
        if (getBillItem() == null || getBillItem().getPharmaceuticalBillItem() == null) {
            //////// // System.out.println("Internal Error at PharmacySaleController.java > handleSelectAction");
        }

        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        calculateRates(billItem);
        if (stock != null && stock.getItemBatch() != null) {
            fillReplaceableStocksForAmp((Amp) stock.getItemBatch().getItem());
        }
    }

    private void clearBill() {
        // Properly clear all PreBill data including items and totals
        if (preBill != null) {
            if (preBill.getBillItems() != null) {
                preBill.getBillItems().clear();
            }
            preBill.setTotal(0);
            preBill.setNetTotal(0);
            preBill.setGrantTotal(0);
            preBill.setDiscount(0);
            preBill.setMargin(0);
        }
        preBill = null;
        userStockContainer = null;
    }

    private void clearBillItem() {
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        stockDto = null;
        // Clear DTO-related fields
        selectedStockDto = null;
        selectedStockId = null;
        lastAutocompleteResults = null;
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

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
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

    public PreBill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
            // preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public boolean isBillPreview() {
        return billPreview;
    }

    public void setBillPreview(boolean billPreview) {
        this.billPreview = billPreview;
    }

    public boolean isDischargeIssueMode() {
        return dischargeIssueMode;
    }

    public void setDischargeIssueMode(boolean dischargeIssueMode) {
        this.dischargeIssueMode = dischargeIssueMode;
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

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
        this.allergyListOfPatient = null;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public PharmacyCalculation getPharmacyCalculation() {
        return pharmacyCalculation;
    }

    public void setPharmacyCalculation(PharmacyCalculation pharmacyCalculation) {
        this.pharmacyCalculation = pharmacyCalculation;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Stock getReplacableStock() {
        return replacableStock;
    }

    public void setReplacableStock(Stock replacableStock) {
        this.replacableStock = replacableStock;
    }

    public Item getSelectedAvailableAmp() {
        return selectedAvailableAmp;
    }

    public void setSelectedAvailableAmp(Item selectedAvailableAmp) {
        this.selectedAvailableAmp = selectedAvailableAmp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Bill getBhtRequestBill() {
        return bhtRequestBill;
    }

    public void setBhtRequestBill(Bill bhtRequestBill) {
        this.bhtRequestBill = bhtRequestBill;
    }

    public Stock getTmpStock() {
        return tmpStock;
    }

    public void setTmpStock(Stock tmpStock) {
        this.tmpStock = tmpStock;
    }

    public Double getBillItemTotal() {
        return billItemTotal;
    }

    public void setBillItemTotal(Double billItemTotal) {
        this.billItemTotal = billItemTotal;
    }

    // DTO-based properties and methods for improved performance
    public StockDTO getStockDto() {
        return stockDto;
    }

    public void setStockDto(StockDTO stockDto) {
        this.stockDto = stockDto;
        if (stockDto != null) {
            this.stock = convertStockDtoToEntity(stockDto);
        }
    }

    public Stock convertStockDtoToEntity(StockDTO stockDto) {
        if (stockDto == null || stockDto.getId() == null) {
            return null;
        }
        return stockFacade.find(stockDto.getId());
    }

    /**
     * Gets fresh stock data for cached stock IDs while preserving order
     */
    private List<StockDTO> getFreshStockDataForIds(List<Long> stockIds) {
        if (stockIds == null || stockIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stockIds", stockIds);
        parameters.put("stockMin", 0.0);

        String sql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "s.id, s.itemBatch.id, s.itemBatch.item.id, s.itemBatch.item.name, s.itemBatch.item.code, "
                + "s.itemBatch.item.vmp.name, s.itemBatch.retailsaleRate, s.stock, s.itemBatch.dateOfExpire) "
                + "FROM Stock s "
                + "WHERE s.id IN :stockIds AND s.stock > :stockMin";

        List<StockDTO> freshResults = (List<StockDTO>) getStockFacade().findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP, 50);

        // Preserve order from cached metadata
        List<StockDTO> orderedResults = new ArrayList<>();
        Map<Long, StockDTO> resultMap = new HashMap<>();
        for (StockDTO dto : freshResults) {
            resultMap.put(dto.getId(), dto);
        }

        for (Long id : stockIds) {
            StockDTO dto = resultMap.get(id);
            if (dto != null) {
                orderedResults.add(dto);
            }
        }

        return orderedResults;
    }

    /**
     * Executes full search and caches metadata
     */
    private List<StockDTO> executeFullSearchAndCacheMetadata(String qry, String cacheKey) {
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

        StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
                .append("i.id, i.itemBatch.id, i.itemBatch.item.id, i.itemBatch.item.name, i.itemBatch.item.code, ")
                .append("i.itemBatch.item.vmp.name, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) ")
                .append("FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department ")
                .append("AND (");

        sql.append("i.itemBatch.item.name LIKE :query ");

        if (searchByItemCode) {
            sql.append("OR i.itemBatch.item.code LIKE :query ");
        }

        if (searchByBarcode) {
            parameters.put("barcodeQuery", qry);
            sql.append("OR i.itemBatch.item.barcode = :barcodeQuery ");
        }

        if (searchByGeneric) {
            sql.append("OR i.itemBatch.item.vmp.vtm.name LIKE :query ");
        }

        sql.append(") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire");

        List<StockDTO> results = (List<StockDTO>) getStockFacade().findLightsByJpql(sql.toString(), parameters, TemporalType.TIMESTAMP, 20);

        // Cache metadata (stock IDs) for future use
        List<Long> stockIds = new ArrayList<>();
        for (StockDTO dto : results) {
            stockIds.add(dto.getId());
        }
        searchMetadataCache.put(cacheKey, stockIds);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());

        return results;
    }

    /**
     * Gets cached metadata if still valid
     */
    private List<Long> getCachedMetadata(String cacheKey) {
        Long cacheTime = cacheTimestamps.get(cacheKey);
        if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < CACHE_TTL_MS) {
            return searchMetadataCache.get(cacheKey);
        }
        // Cache expired, remove entries
        searchMetadataCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
        return null;
    }

    public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            lastAutocompleteResults = new ArrayList<>();
            return lastAutocompleteResults;
        }

        qry = qry.replaceAll("[\\n\\r]", "").trim();

        String cacheKey = qry.toLowerCase().trim() + "_" +
            getSessionController().getLoggedUser().getDepartment().getId();

        List<Long> cachedStockIds = getCachedMetadata(cacheKey);
        List<StockDTO> results;

        if (cachedStockIds != null) {
            results = getFreshStockDataForIds(cachedStockIds);
        } else {
            results = executeFullSearchAndCacheMetadata(qry, cacheKey);
        }

        lastAutocompleteResults = results != null ? results : new ArrayList<>();
        return lastAutocompleteResults;
    }

    // Getter method for JSF to access the converter
    public StockDtoConverter getStockDtoConverter() {
        return new StockDtoConverter();
    }

    public BillItem getItemForSubstitution() {
        return itemForSubstitution;
    }

    public void setItemForSubstitution(BillItem itemForSubstitution) {
        this.itemForSubstitution = itemForSubstitution;
    }

    public Stock getSelectedSubstituteStock() {
        return selectedSubstituteStock;
    }

    public void setSelectedSubstituteStock(Stock selectedSubstituteStock) {
        this.selectedSubstituteStock = selectedSubstituteStock;
    }

    public List<Stock> getSubstituteStocks() {
        return substituteStocks;
    }

    public void setSubstituteStocks(List<Stock> substituteStocks) {
        this.substituteStocks = substituteStocks;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // StockDTO Converter for JSF
    public static class StockDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            try {
                Long id = Long.valueOf(value);
                PharmacySaleBhtController controller = (PharmacySaleBhtController)
                    facesContext.getApplication().getELResolver()
                    .getValue(facesContext.getELContext(), null, "pharmacySaleBhtController");

                if (controller == null) {
                    // Fallback: Create minimal DTO
                    StockDTO dto = new StockDTO();
                    dto.setId(id);
                    return dto;
                }

                // PERFORMANCE OPTIMIZATION: Search in cached results (ZERO DATABASE QUERIES)

                // First check: Does current selectedStockDto match?
                if (controller.getSelectedStockDto() != null && id.equals(controller.getSelectedStockDto().getId())) {
                    return controller.getSelectedStockDto();
                }

                // Second check: Search in lastAutocompleteResults
                if (controller.getLastAutocompleteResults() != null) {
                    for (StockDTO dto : controller.getLastAutocompleteResults()) {
                        if (dto != null && id.equals(dto.getId())) {
                            return dto;
                        }
                    }
                }

                // Fallback: Create minimal DTO (avoids database query during postback)
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
            return value.toString();
        }
    }

    /**
     * One row of the prescription→dispensing conversion report shown on the
     * discharge issue page. Captures the original prescription and what was
     * actually resolved/issued, plus a human-readable status and severity so the
     * pharmacist can spot low-/no-stock shortfalls and avoid silent omissions.
     * Issue #21334.
     */
    public static class DischargeConversionRow implements Serializable {

        private static final long serialVersionUID = 1L;

        public enum Status {
            ISSUED_FULL,
            QTY_DEFAULTED,
            PARTIAL_LOW_STOCK,
            NOT_AVAILABLE
        }

        private String prescribedText;
        private String resolvedItemName;
        private Double requiredQty;
        private Double issuedQty;
        private Status status;
        private String message;

        public DischargeConversionRow() {
        }

        public String getPrescribedText() {
            return prescribedText;
        }

        public void setPrescribedText(String prescribedText) {
            this.prescribedText = prescribedText;
        }

        public String getResolvedItemName() {
            return resolvedItemName;
        }

        public void setResolvedItemName(String resolvedItemName) {
            this.resolvedItemName = resolvedItemName;
        }

        public Double getRequiredQty() {
            return requiredQty;
        }

        public void setRequiredQty(Double requiredQty) {
            this.requiredQty = requiredQty;
        }

        public Double getIssuedQty() {
            return issuedQty;
        }

        public void setIssuedQty(Double issuedQty) {
            this.issuedQty = issuedQty;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        /** True when this row needs the pharmacist's attention (not fully issued). */
        public boolean isNeedsAttention() {
            return status != Status.ISSUED_FULL;
        }

        /** Bootstrap/PrimeFaces severity keyword for styling the row. */
        public String getSeverity() {
            if (status == null) {
                return "info";
            }
            switch (status) {
                case ISSUED_FULL:
                    return "success";
                case QTY_DEFAULTED:
                case PARTIAL_LOW_STOCK:
                    return "warning";
                case NOT_AVAILABLE:
                    return "danger";
                default:
                    return "info";
            }
        }

        public String getStatusLabel() {
            if (status == null) {
                return "";
            }
            switch (status) {
                case ISSUED_FULL:
                    return "Issued in full";
                case QTY_DEFAULTED:
                    return "Qty defaulted — verify";
                case PARTIAL_LOW_STOCK:
                    return "Partial — low stock";
                case NOT_AVAILABLE:
                    return "Not available";
                default:
                    return "";
            }
        }
    }

}
