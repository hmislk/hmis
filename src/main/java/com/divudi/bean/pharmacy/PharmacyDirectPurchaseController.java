/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.dataStructure.BillListWithTotals;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.PharmacyStockRow;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BillNumber;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.PaymentService;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.core.util.BigDecimalUtil;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.math.RoundingMode;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyDirectPurchaseController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private BilledBill bill;
    private List<BillItem> billItems;
    private BillItem currentBillItem;
    private BillItem editingBillItem;
    private boolean printPreview;
    private boolean showAllBillFormats = false;
    private BillItem currentExpense;
    private List<BillItem> billExpenses;
    private String warningMessage;
    private boolean draftMode;

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToStartNewDirectPurchaseBill() {
        prepareForNewDIrectPurchaseBill();
        draftMode = false;
        return "/pharmacy/direct_purchase?faces-redirect=true";
    }

    public String navigateToStartNewDirectPurchaseDraft() {
        prepareForNewDIrectPurchaseBill();
        draftMode = true;
        return "/pharmacy/direct_purchase?faces-redirect=true";
    }

    public String loadDraftForEditing(com.divudi.core.entity.Bill draft) {
        if (draft == null) {
            com.divudi.core.util.JsfUtil.addErrorMessage("No draft selected");
            return null;
        }
        com.divudi.core.entity.Bill freshBill = billService.reloadBill(draft);
        if (freshBill == null) {
            com.divudi.core.util.JsfUtil.addErrorMessage("Draft bill could not be loaded");
            return null;
        }
        prepareForNewDIrectPurchaseBill();
        bill = (com.divudi.core.entity.BilledBill) freshBill;
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("billId", freshBill.getId());
        billItems = billItemFacade.findByJpql(
            "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId AND bi.retired = false ORDER BY bi.searialNo",
            params);
        String expJpql = "SELECT be FROM BillItem be WHERE be.expenseBill.id = :billId AND be.retired = false ORDER BY be.searialNo";
        billExpenses = billItemFacade.findByJpql(expJpql, params);
        draftMode = true;
        printPreview = false;
        return "/pharmacy/direct_purchase?faces-redirect=true";
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Functions">
    public void prepareForNewDIrectPurchaseBill() {
        printPreview = false;
        currentBillItem = null;
        if (bill != null) {
            bill.setDepartmentType(null);
        }
        bill = null;
        billItems = null;
        billExpenses = null;
        currentExpense = null;
        warningMessage = null;
    }

    /**
     * Recalculates profit margins for all bill items after discount
     * distribution
     */
    private void recalculateProfitMarginsForAllItems() {
        if (getBillItems() == null || getBillItems().isEmpty()) {
            return;
        }

        for (BillItem item : getBillItems()) {
            if (item != null && item.getBillItemFinanceDetails() != null) {
                // Recalculate profit margin using the updated total cost (which includes distributed discount)
                BigDecimal profitMargin = calculateProfitMarginForPurchasesBigDecimal(item);
                item.getBillItemFinanceDetails().setProfitMargin(profitMargin);
            }
        }
    }

    public void addItem() {
        Item item = getCurrentBillItem().getItem();
        BillItemFinanceDetails f = getCurrentBillItem().getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = getCurrentBillItem().getPharmaceuticalBillItem();

        if (item == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        // Auto-set department type if not already set
        if (getBill().getDepartmentType() == null) {
            if (item.getDepartmentType() != null) {
                getBill().setDepartmentType(item.getDepartmentType());
            } else {
                getBill().setDepartmentType(DepartmentType.Pharmacy);
            }
        }

        // Validate item's department type matches bill's department type
        if (getBill().getDepartmentType() != null) {
            DepartmentType itemDepartmentType = item.getDepartmentType();

            if (itemDepartmentType != null && !itemDepartmentType.equals(getBill().getDepartmentType())) {
                JsfUtil.addErrorMessage("Cannot add items from different department types. "
                        + "Bill is set for " + getBill().getDepartmentType().getLabel()
                        + " items, but you are trying to add a " + itemDepartmentType.getLabel() + " item.");
                return;
            }

            // Verify department type is allowed
            List<DepartmentType> allowedTypes = sessionController.getAvailableDepartmentTypesForPharmacyTransactions();
            if (allowedTypes == null || !allowedTypes.contains(getBill().getDepartmentType())) {
                JsfUtil.addErrorMessage("Items are not allowed for the selected department type: " + getBill().getDepartmentType().getLabel());
                return;
            }
        }

        // ChatGPT contributed
        boolean allowAddingDirectPurchaseItemsWhenNormalQuantityIsZeroAndFreeQuantityIsPresent
                = configOptionApplicationController.getBooleanValueByKey(
                        "Allow Adding Direct Purchase Items When Normal Quantity Is Zero And Free Quantity Is Present",
                        false
                );

        if (f == null || pbi == null) {
            JsfUtil.addErrorMessage("Invalid internal structure. Cannot proceed.");
            return;
        }

        // Quantity validation according to configuration
        // Common: disallow negative quantities
        if (BigDecimalUtil.isNegative(f.getQuantity())) {
            JsfUtil.addErrorMessage("Quantity cannot be negative");
            return;
        }
        if (BigDecimalUtil.isNegative(f.getFreeQuantity())) {
            JsfUtil.addErrorMessage("Free quantity cannot be negative");
            return;
        }
        if (allowAddingDirectPurchaseItemsWhenNormalQuantityIsZeroAndFreeQuantityIsPresent) {
            // Option true: require either quantity or free quantity to be present
            boolean hasQty = BigDecimalUtil.isPositive(f.getQuantity());
            boolean hasFree = BigDecimalUtil.isPositive(f.getFreeQuantity());
            if (!(hasQty || hasFree)) {
                JsfUtil.addErrorMessage("Please enter quantity or free quantity");
                return;
            }
        } else {
            // Option false: require quantity > 0; ignore free quantity
            if (!BigDecimalUtil.isPositive(f.getQuantity())) {
                JsfUtil.addErrorMessage("Please enter quantity");
                return;
            }
        }

        if (f.getLineGrossRate() == null || BigDecimalUtil.isNegative(f.getLineGrossRate())) {
            JsfUtil.addErrorMessage("Please enter a valid purchase rate (negative values not allowed)");
            return;
        }

        if (BigDecimalUtil.isNullOrZero(f.getRetailSaleRatePerUnit()) || BigDecimalUtil.isNegative(f.getRetailSaleRatePerUnit())) {
            JsfUtil.addErrorMessage("Please enter the sale rate");
            return;
        }

        // Issue #21635 / #13103 / #21837: block a retail rate below the purchase rate (selling
        // at a loss) unless config allows it (clearance / loss-leader pricing). Normalize AMPP
        // pack rates to per-unit before comparing.
        if (!isAllowRetailRateBelowPurchaseRate() && isRetailRateBelowPurchaseRate(item, f)) {
            JsfUtil.addErrorMessage("Retail rate is below the purchase rate. Enable 'Allow Retail Rate Below Purchase Rate in Pharmacy Purchasing' to proceed.");
            return;
        }

        if (pbi.getDoe() == null) {
            JsfUtil.addErrorMessage("Please set the date of expiry");
            return;
        }

        // Check if expired items are allowed (for stock upload scenarios)
        boolean allowExpiredItems = configOptionApplicationController.getBooleanValueByKey(
                "Allow Expired Items in Direct Purchase Stock Upload", false);

        if (!allowExpiredItems && pbi.getDoe() != null) {
            if (pbi.getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                JsfUtil.addErrorMessage("Check Date of Expiry");
                return;
            }
        }
        // Setup basic quantity and rate fields for AMP/AMPP handling
        BigDecimal qty = BigDecimalUtil.valueOrZero(f.getQuantity());

        // Ensure free quantity is properly initialized when left blank
        if (f.getFreeQuantity() == null) {
            f.setFreeQuantity(BigDecimal.ZERO);
        }
        BigDecimal freeQty = BigDecimalUtil.valueOrZero(f.getFreeQuantity());

        if (item instanceof Ampp) {
            // AMPP: User enters packs, need to set units per pack and quantity by units
            BigDecimal unitsPerPack = Optional.ofNullable(f.getUnitsPerPack()).orElse(BigDecimal.ONE);
            if (unitsPerPack.compareTo(BigDecimal.ZERO) <= 0) {
                unitsPerPack = BigDecimal.valueOf(item.getDblValue() > 0 ? item.getDblValue() : 1);
                f.setUnitsPerPack(unitsPerPack);
            }

            f.setQuantityByUnits(BigDecimalUtil.multiply(qty, unitsPerPack));
            f.setFreeQuantityByUnits(BigDecimalUtil.multiply(freeQty, unitsPerPack));
            f.setTotalQuantityByUnits(BigDecimalUtil.add(f.getQuantityByUnits(), f.getFreeQuantityByUnits()));

            // For AMPP, only set grossRate from lineGrossRate if grossRate is null or zero
            // grossRate will be calculated elsewhere from bill components
            BigDecimal existingGrossRate = BigDecimalUtil.valueOrZero(f.getGrossRate());
            if (existingGrossRate.compareTo(BigDecimal.ZERO) == 0) {
                f.setGrossRate(f.getLineGrossRate());
            }

            // Set netRate from lineNetRate if netRate is null or zero
            BigDecimal existingNetRate = BigDecimalUtil.valueOrZero(f.getNetRate());
            if (existingNetRate.compareTo(BigDecimal.ZERO) == 0) {
                f.setNetRate(f.getLineNetRate());
            }

            // Set retail rates
            if (f.getRetailSaleRatePerUnit() != null) {
                f.setRetailSaleRate(BigDecimalUtil.multiply(f.getRetailSaleRatePerUnit(), unitsPerPack));
            }
        } else {
            // AMP: User enters units, everything is unit-based
            f.setUnitsPerPack(BigDecimal.ONE);
            f.setQuantityByUnits(qty);
            f.setFreeQuantityByUnits(freeQty);
            f.setTotalQuantityByUnits(BigDecimalUtil.add(qty, freeQty));

            // For AMP, only set grossRate from lineGrossRate if grossRate is null or zero
            // grossRate will be calculated elsewhere from bill components
            BigDecimal existingGrossRate = BigDecimalUtil.valueOrZero(f.getGrossRate());
            if (existingGrossRate.compareTo(BigDecimal.ZERO) == 0) {
                f.setGrossRate(f.getLineGrossRate());
            }

            // Set netRate from lineNetRate if netRate is null or zero
            BigDecimal existingNetRate = BigDecimalUtil.valueOrZero(f.getNetRate());
            if (existingNetRate.compareTo(BigDecimal.ZERO) == 0) {
                f.setNetRate(f.getLineNetRate());
            }

            f.setRetailSaleRate(f.getRetailSaleRatePerUnit());
        }

        // Set PharmaceuticalBillItem basic values - calculations will be done by calculateItemTotals()
        pbi.setQty(BigDecimalUtil.valueOrZero(f.getQuantityByUnits()).doubleValue());
        pbi.setFreeQty(BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits()).doubleValue());

        if (item instanceof Ampp) {
            pbi.setQtyPacks(BigDecimalUtil.valueOrZero(f.getQuantity()).doubleValue());
            // Use null-safe free quantity to avoid NPEs
            pbi.setFreeQtyPacks(BigDecimalUtil.valueOrZero(f.getFreeQuantity()).doubleValue());
            pbi.setPurchaseRatePack(BigDecimalUtil.valueOrZero(f.getLineNetRate()).doubleValue());
            pbi.setRetailRatePack(BigDecimalUtil.valueOrZero(f.getRetailSaleRate()).doubleValue());
        } else {
            pbi.setQtyPacks(BigDecimalUtil.valueOrZero(f.getQuantityByUnits()).doubleValue());
            pbi.setFreeQtyPacks(BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits()).doubleValue());
            pbi.setPurchaseRatePack(BigDecimalUtil.valueOrZero(f.getLineNetRate()).doubleValue());
            pbi.setRetailRatePack(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());
        }

        // Set basic rates - will be recalculated by calculateItemTotals()
        if (item instanceof Ampp) {
            // For AMPP: netRate is pack price, but purchaseRate should be unit price
            BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
            if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
                unitsPerPack = BigDecimal.ONE; // Avoid division by zero
            }
            BigDecimal unitPurchaseRate = BigDecimalUtil.valueOrZero(f.getNetRate()).divide(unitsPerPack, 4, RoundingMode.HALF_UP);
            pbi.setPurchaseRate(unitPurchaseRate.doubleValue());
        } else {
            // For AMP: netRate is already unit price
            pbi.setPurchaseRate(BigDecimalUtil.valueOrZero(f.getNetRate()).doubleValue());
        }
        pbi.setRetailRate(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());
        pbi.setRetailRateInUnit(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());

        // Set BillItem basic rate fields - calculations will be done by calculateItemTotals()
        getCurrentBillItem().setRate(BigDecimalUtil.valueOrZero(f.getLineGrossRate()).doubleValue());
        getCurrentBillItem().setQty(BigDecimalUtil.valueOrZero(f.getQuantity()).doubleValue());

        // Calculate item totals using internal logic
        calculateItemTotals(getCurrentBillItem());

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getBillItems().add(currentBillItem);

        currentBillItem = null;
        // Calculate bill totals using internal methods
        calculateBillTotalsFromItems();

        // Distribute bill-level adjustments proportionally to line items
        distributeProportionalBillValuesToItems();

        // Recalculate profit margins after distributions have been applied
        recalculateProfitMarginsForAllItems();

//        calulateTotalsWhenAddingItemsOldMethod();
    }

// ChatGPT contributed - Recalculates line-level financial values before adding BillItem to bill
    public void onQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        // Validate integer-only quantity if configuration is enabled
        if (configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
            if (qty != null && qty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setQuantity(BigDecimal.ZERO);
                calculateItemTotals(bi);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
                return;
            }
        }

        // Recalculate item totals when quantity changes
        calculateItemTotals(bi);
    }

    public void onFreeQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        // Ensure free quantity is properly initialized when left blank
        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        if (f.getFreeQuantity() == null) {
            f.setFreeQuantity(BigDecimal.ZERO);
        }

        // Validate integer-only free quantity if configuration is enabled
        if (configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            BigDecimal freeQty = f.getFreeQuantity();
            if (freeQty != null && freeQty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                f.setFreeQuantity(BigDecimal.ZERO);
                calculateItemTotals(bi);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
                return;
            }
        }

        // Recalculate item totals when free quantity changes
        calculateItemTotals(bi);
    }

    public void onLineGrossRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }
        // Recalculate item totals when purchase rate changes
        calculateItemTotals(bi);
    }

    public void onLineDiscountRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }
        // Recalculate item totals when discount rate changes
        calculateItemTotals(bi);
    }

    // ChatGPT contributed: Optimized for null-safety and readability
    public void onRetailSaleRateChange() {
        if (currentBillItem == null) {
            return;
        }

        BillItemFinanceDetails f = currentBillItem.getBillItemFinanceDetails();
        if (f == null || f.getRetailSaleRate() == null) {
            return;
        }

        Item item = currentBillItem.getItem();
        if (item instanceof Ampp) {
            double dblVal = item.getDblValue();
            BigDecimal unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
            f.setRetailSaleRatePerUnit(f.getRetailSaleRate().divide(unitsPerPack, MathContext.DECIMAL64));
        } else {
            f.setRetailSaleRatePerUnit(f.getRetailSaleRate());
        }

        // Recalculate item totals when retail rate changes
        calculateItemTotals(currentBillItem);
    }

    public void onWholesaleRateChange() {
        if (currentBillItem == null) {
            return;
        }

        BillItemFinanceDetails f = currentBillItem.getBillItemFinanceDetails();
        if (f == null || f.getWholesaleRate() == null) {
            return;
        }

        Item item = currentBillItem.getItem();
        if (item instanceof Ampp) {
            double dblVal = item.getDblValue();
            BigDecimal unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
            f.setWholesaleRatePerUnit(f.getWholesaleRate().divide(unitsPerPack, MathContext.DECIMAL64));
        } else {
            f.setWholesaleRatePerUnit(f.getWholesaleRate());
        }

        // Recalculate item totals when wholesale rate changes
        calculateItemTotals(currentBillItem);
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Inner Classes Static Converter">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Inner Classes">
    // </editor-fold>  
    /**
     * EJBs
     */
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillEjb billEjb;
    @EJB
    PaymentService paymentService;
    @EJB
    PharmacyCostingService pharmacyCostingService;
    @EJB
    com.divudi.service.BillService billService;
    @EJB
    com.divudi.service.pharmacy.DirectPurchaseApprovingNativeSqlService directPurchaseApprovingService;

    /**
     * Controllers
     */
    @Inject
    private SessionController sessionController;
    @Inject
    PharmacyCalculation pharmacyBillBean;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    PageMetadataRegistry pageMetadataRegistry;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private ItemController itemController;
    /**
     * Properties
     */

    double saleRate;
    double wsRate;
    AmpController ampController;

    Institution institution;
    Department department;
    Date fromDate;
    Date toDate;
    List<PharmacyStockRow> rows;

    BillListWithTotals billListWithTotals;
    private double billItemsTotalQty;

    private PaymentMethodData paymentMethodData;
    private Institution site;
    private Institution toInstitution;
    private PaymentMethod paymentMethod;

    public List<BillItem> getBillExpenses() {
        if (billExpenses == null) {
            billExpenses = new ArrayList<>();
        }
        return billExpenses;
    }

    public void setBillExpenses(List<BillItem> billExpenses) {
        this.billExpenses = billExpenses;
    }

    public BillItem getCurrentExpense() {
        if (currentExpense == null) {
            currentExpense = new BillItem();
        }
        return currentExpense;
    }

    public void setCurrentExpense(BillItem currentExpense) {
        this.currentExpense = currentExpense;
    }
// ChatGPT contributed: Null-safe and debug-augmented version

    public void onItemSelect(SelectEvent event) {
        BillItem current = getCurrentBillItem();
        if (current == null || current.getItem() == null) {
            return;
        }

        Item item = current.getItem();
        Department dept = getSessionController().getDepartment();
        if (dept == null) {
            return;
        }

        double pr = 0.0;
        double rr = 0.0;
        BigDecimal packRate = BigDecimal.ZERO;

        BillItem lastPurchasedBillItem = getPharmacyBean().getLastPurchaseItem(item, dept);
        if (lastPurchasedBillItem != null) {
            BillItemFinanceDetails lastDetails = lastPurchasedBillItem.getBillItemFinanceDetails();
            if (lastDetails != null) {
                BigDecimal lineGrossRate = lastDetails.getLineGrossRate();
                BigDecimal lastRetailRate = lastDetails.getRetailSaleRate();

                pr = (lineGrossRate != null) ? lineGrossRate.doubleValue() : 0.0;
                rr = (lastRetailRate != null) ? lastRetailRate.doubleValue() : 0.0;
                packRate = BigDecimalUtil.valueOrZero(lastRetailRate);

            }
        }

        // Fallback logic
        if (pr == 0.0 || rr == 0.0) {
            double fallbackPr = getPharmacyBean().getLastPurchaseRate(item, dept);
            double fallbackRr = getPharmacyBean().getLastRetailRateByBillItemFinanceDetails(item, dept);
            pr = fallbackPr > 0.0 ? fallbackPr : pr;
            rr = fallbackRr > 0.0 ? fallbackRr : rr;
            packRate = BigDecimal.valueOf(rr);
        }

        PharmaceuticalBillItem pbi = current.getPharmaceuticalBillItem();
        //pbi will never be null as it is initialized in the getter
        pbi.setPurchaseRate(pr);
        pbi.setRetailRate(rr);

        BillItemFinanceDetails f = current.getBillItemFinanceDetails();
        //f will never be null as it is created in the getter

        f.setLineGrossRate(BigDecimal.valueOf(pr));

        if (item instanceof Ampp) {
            double units = item.getDblValue();
            BigDecimal unitsPerPack = (units > 0.0) ? BigDecimal.valueOf(units) : BigDecimal.ONE;
            f.setUnitsPerPack(unitsPerPack);
            f.setRetailSaleRate(packRate);
            f.setRetailSaleRatePerUnit(packRate.divide(unitsPerPack, MathContext.DECIMAL64));
        } else {
            f.setUnitsPerPack(BigDecimal.ONE);
            f.setRetailSaleRate(packRate);
            f.setRetailSaleRatePerUnit(packRate);
        }

        // Recalculate item totals after setting initial rates
        calculateItemTotals(current);
    }

    public void onExpenseItemSelect(SelectEvent event) {
        BillItem current = getCurrentExpense();
        if (current == null || current.getItem() == null) {
            return;
        }

        // Set the consideredForCosting based on the selected expense item's default setting
        current.setConsideredForCosting(current.getItem().isConsideredForCosting());
    }

    public void updateExpenseCosting(BillItem expense) {
        if (expense == null) {
            return;
        }

        // Use transactional EJB method to ensure atomicity
        billEjb.updateExpenseCosting(expense, getBill(), getBillItems(), pharmacyCostingService);
    }

    private void recalculateExpenseTotals() {
        Bill bill = getBill(); // Cache the bill reference
        if (bill == null) {
            return;
        }

        double billExpensesConsideredTotal = 0.0;
        double billExpensesNotConsideredTotal = 0.0;
        double billExpensesTotal = 0.0;

        // Calculate totals from bill-level expense BillItems (use Bill entity's list)
        if (bill.getBillExpenses() != null && !bill.getBillExpenses().isEmpty()) {
            for (BillItem expense : bill.getBillExpenses()) {
                billExpensesTotal += expense.getNetValue();
                if (expense.isConsideredForCosting()) {
                    billExpensesConsideredTotal += expense.getNetValue();
                } else {
                    billExpensesNotConsideredTotal += expense.getNetValue();
                }
            }
        }

        // Update the bill's expense totals
        bill.setExpenseTotal(billExpensesTotal);
        bill.setExpensesTotalConsideredForCosting(billExpensesConsideredTotal);
        bill.setExpensesTotalNotConsideredForCosting(billExpensesNotConsideredTotal);

        // Also update BillFinanceDetails if it exists
        if (bill.getBillFinanceDetails() != null) {
            bill.getBillFinanceDetails().setBillExpense(BigDecimal.valueOf(billExpensesTotal));
            bill.getBillFinanceDetails().setBillExpensesConsideredForCosting(BigDecimal.valueOf(billExpensesConsideredTotal));
            bill.getBillFinanceDetails().setBillExpensesNotConsideredForCosting(BigDecimal.valueOf(billExpensesNotConsideredTotal));
        }

        // Recalculate bill totals and distribute adjustments to line items
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems();

    }

// ChatGPT contributed - Calculates true profit margin (%) based on unit sale and cost rates
    // ChatGPT contributed - Calculates profit margin (%) correctly based on item type (Amp or Ampp)
    public double calculateProfitMargin(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return 0.0;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        BigDecimal purchaseRatePerUnit;
        BigDecimal retailRatePerUnit = BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit());

        // For AMPP items, convert pack rates to unit rates for profit calculation
        if (bi.getItem() instanceof Ampp) {
            BigDecimal packPurchaseRate = BigDecimalUtil.valueOrZero(f.getNetRate());
            BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());

            if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
                unitsPerPack = BigDecimal.ONE; // Avoid division by zero
            }

            // Convert pack rate to unit rate for profit calculation
            purchaseRatePerUnit = packPurchaseRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP);
            // DEBUG: Log the values to identify AMPP calculation issue
            // DEBUG: Log the values to identify AMPP calculation issue
            // DEBUG: Log the values to identify AMPP calculation issue
            // DEBUG: Log the values to identify AMPP calculation issue
            // DEBUG: Log the values to identify AMPP calculation issue
        } else {
            // For AMP items, netRate is already per unit
            purchaseRatePerUnit = BigDecimalUtil.valueOrZero(f.getNetRate());

        }

        if (purchaseRatePerUnit.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        // Profit Margin = ((Retail Rate - Purchase Rate) / Purchase Rate) * 100
        BigDecimal profit = retailRatePerUnit.subtract(purchaseRatePerUnit);
        BigDecimal margin = profit.divide(purchaseRatePerUnit, 4, RoundingMode.HALF_UP);
        double result = margin.multiply(BigDecimal.valueOf(100)).doubleValue();


        return result;
    }

    public boolean isProfitMarginExcessive(BillItem ph) {
        if (ph == null || ph.getItem() == null || ph.getItem().getCategory() == null) {
            return false;
        }
        double margin = calculateProfitMargin(ph);
        return ph.getItem().getCategory().getProfitMargin() > margin;
    }

    public void displayItemDetails(BillItem bi) {
        if (bi == null || bi.getItem() == null) {
            return;
        }
        pharmacyController.fillItemDetails(bi.getItem());
    }

    public List<PharmacyStockRow> getRows() {
        return rows;
    }

    public Institution getInstitution() {
        if (institution == null) {
            institution = getSessionController().getInstitution();
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();

    }

    public PharmacyCalculation getPharmacyBillBean() {
        return pharmacyBillBean;
    }

    public void setPharmacyBillBean(PharmacyCalculation pharmacyBillBean) {
        this.pharmacyBillBean = pharmacyBillBean;
    }

    public PharmacyDirectPurchaseController() {
    }

    public void setBatch() {
        if (getCurrentBillItem() != null) {
            PharmaceuticalBillItem pharmaceuticalBillItem = getCurrentBillItem().getPharmaceuticalBillItem();

            if (pharmaceuticalBillItem != null) {
                String stringValue = pharmaceuticalBillItem.getStringValue();

                if (stringValue != null && stringValue.trim().isEmpty()) {
                    Date date = pharmaceuticalBillItem.getDoe();

                    if (date != null) {
                        DateFormat df = new SimpleDateFormat("ddMMyyyy");
                        String reportDate = df.format(date);
                        pharmaceuticalBillItem.setStringValue(reportDate);
                    }
                }
            }
        }
    }

    public String errorCheck() {
        String msg = "";

        if (getBill().getFromInstitution() == null) {
            msg = "Please select Dealor";
            return msg;
        }

        if (getBillItems().isEmpty()) {
            msg = "Empty Items";
            return msg;
        }

        return msg;
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void billDiscountChangedByUser() {
        ensureBillDiscountAndTaxSynchronization();
        // Recalculate bill totals when bill discount changes
        calculateBillTotalsFromItems();
        // Distribute bill-level adjustments proportionally to line items
        distributeProportionalBillValuesToItems();
        recalculateProfitMarginsForAllItems();
    }

    public void billTaxChangedByUser() {
        ensureBillDiscountAndTaxSynchronization();
        // Recalculate bill totals when bill tax changes
        calculateBillTotalsFromItems();
        // Distribute bill-level adjustments proportionally to line items
        distributeProportionalBillValuesToItems();
        recalculateProfitMarginsForAllItems();
    }

    /**
     * Ensures that both bill.discount/tax and
     * bill.billFinanceDetails.billDiscount/billTaxValue are synchronized The
     * service method reads from billFinanceDetails, but UI may store in bill
     * directly
     */
    private void ensureBillDiscountAndTaxSynchronization() {
        if (getBill() == null) {
            return;
        }

        // Ensure BillFinanceDetails exists
        if (getBill().getBillFinanceDetails() == null) {
            getBill().setBillFinanceDetails(new BillFinanceDetails(getBill()));
        }

        // Synchronize discount and tax from bill to billFinanceDetails
        getBill().getBillFinanceDetails().setBillDiscount(BigDecimal.valueOf(getBill().getDiscount()));
        getBill().getBillFinanceDetails().setBillTaxValue(BigDecimal.valueOf(getBill().getTax()));
    }

    public void addExpense() {
        if (getBill().isCompleted()) {
            JsfUtil.addErrorMessage("This bill is completed and cannot be edited.");
            return;
        }
        if (getCurrentExpense().getItem() == null) {
            JsfUtil.addErrorMessage("Expense ?");
            return;
        }
        if (currentExpense.getQty() == null || currentExpense.getQty().equals(0.0)) {
            currentExpense.setQty(1.0);
        }
        if (currentExpense.getNetRate() == 0.0) {
            currentExpense.setNetRate(currentExpense.getRate());
        }

        currentExpense.setNetValue(currentExpense.getNetRate() * currentExpense.getQty());
        currentExpense.setGrossValue(currentExpense.getRate() * currentExpense.getQty());

        // Owning-side FK: without this, Bill.billExpenses' cascade persists
        // nothing back to this bill (issue #21856)
        currentExpense.setExpenseBill(getBill());

        getCurrentExpense().setSearialNo(getBillExpenses().size());
        getBillExpenses().add(currentExpense);

        // IMPORTANT: Also add to the Bill entity's expense list
        getBill().getBillExpenses().add(currentExpense);

        // Recalculate expense totals after adding new expense
        recalculateExpenseTotals();
        recalculateProfitMarginsForAllItems();

        // Deliberately NOT persisted here (issue #23005): like items added via
        // addItem(), the expense stays in the in-memory billExpenses list until
        // persistDraftDirectPurchase() or settleDirectPurchaseBillFinally() runs
        // - both already loop over billExpenses and create()/edit() each row
        // with expenseBill set to the (by-then persisted) Bill, so an early
        // persist here is redundant and was the source of orphan bare Bill rows
        // when a user added an expense before ever saving/settling.
        currentExpense = null;

    }

    public void removeExpense(BillItem expense) {
        if (expense == null) {
            return;
        }
        if (getBill().isCompleted()) {
            JsfUtil.addErrorMessage("This bill is completed and cannot be edited.");
            return;
        }

        if (billExpenses != null) {
            billExpenses.remove(expense);
            int index = 0;
            for (BillItem be : billExpenses) {
                be.setSearialNo(index++);
            }
        }

        if (getBill().getBillExpenses() != null) {
            getBill().getBillExpenses().remove(expense);
        }

        // Retire the persisted row - removing it from the in-memory list alone
        // does not delete it (Bill.billExpenses has no orphanRemoval), so an
        // "un-retired" removal would silently reappear on reload (issue #21856).
        if (expense.getId() != null) {
            expense.setRetired(true);
            expense.setRetireComments("Removed during draft edit");
            getBillItemFacade().edit(expense);
        }

        recalculateExpenseTotals();
        recalculateProfitMarginsForAllItems();

        if (getBill().getId() != null) {
            billFacade.edit(getBill());
        }
    }

    /**
     * Shared by settle/finalize/approve so the rule lives in one place instead of an
     * inline copy at each call site. Shows an error message and returns false if invalid.
     */
    private boolean isPaymentMethodValid(com.divudi.core.entity.Bill b) {
        if (b.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return false;
        }
        if (b.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            JsfUtil.addErrorMessage("MultiplePayments Not Allowed.");
            return false;
        }
        return true;
    }

    public void settleDirectPurchaseBillFinally() {
        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items");
            return;
        }
        if (getBill().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Dealer");
            return;
        }
        if (getBill().getReferenceInstitution() == null) {
            JsfUtil.addErrorMessage("Select Reference Institution");
        }
        if (getBill().getInvoiceNumber() == null || getBill().getInvoiceNumber().trim().isEmpty()) {
            boolean autogenerateInvoiceNumber = configOptionApplicationController.getBooleanValueByKey("Autogenerate Invoice Number for Pharmacy Direct Purchase", false);
            if (autogenerateInvoiceNumber) {
                BillNumber bn = billNumberBean.fetchLastBillNumberForYear(sessionController.getInstitution(), sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
                String invoiceNumber = configOptionApplicationController.getShortTextValueByKey("Invoice Number Prefix for Pharmacy Direct Purchase", "") + bn.getLastBillNumber();
                getBill().setInvoiceNumber(invoiceNumber);
            } else {
                JsfUtil.addErrorMessage("Please Enter Invoice Number");
                return;
            }
        }
        if (getBill().getInvoiceDate() == null) {
            boolean useCurrentDataIfInvoiceDataIsNotProvided = configOptionApplicationController.getBooleanValueByKey("If Invoice Number is not provided for Pharmacy Direct Purchase, use the current date", false);
            if (useCurrentDataIfInvoiceDataIsNotProvided) {
                getBill().setInvoiceDate(new Date());
            } else {
                JsfUtil.addErrorMessage("Please Fill Invoice Date");
                return;
            }
        }
        if (!isPaymentMethodValid(getBill())) {
            return;
        }

        // Validate department type consistency
        if (getBill().getDepartmentType() != null && !getBillItems().isEmpty()) {
            for (BillItem bi : getBillItems()) {
                if (bi.getItem() != null && bi.getItem().getDepartmentType() != null) {
                    if (!bi.getItem().getDepartmentType().equals(getBill().getDepartmentType())) {
                        JsfUtil.addErrorMessage("Inconsistent department types detected. All items must belong to the same department type.");
                        return;
                    }
                }
            }
        }

        saveBill();
        //   saveBillComponent();

//        Payment p = createPayment(getBill());
        billItemsTotalQty = 0;

        // Calculate bill-level totals and distribute bill-level adjustments (discount,
        // tax, expenses) into each item's cost rate BEFORE ItemBatch/Stock are created
        // below. saveItemBatchWithCosting() reads BillItemFinanceDetails.totalCostRate
        // to set ItemBatch.costRate (which StockHistory then snapshots) - if this ran
        // after item batches were created, the batch/stock would be left with the
        // pre-distribution cost rate forever while the report's cost totals reflect
        // the post-distribution one, producing a permanent COGS cost variance.
        calculateBillTotalsFromItems();
        if (getBill().getDiscount() != 0.0 || getBill().getTax() != 0.0 || getBill().getExpensesTotalConsideredForCosting() != 0.0) {
            distributeProportionalBillValuesToItems();
            // Persist the distributed values for previously-saved items here (a zero-qty/
            // zero-freeQty item is skipped by the loop below's `continue` before it ever
            // reaches create/edit(i) or getBill().getBillItems(), so it would otherwise
            // silently lose whatever distribute() computed for it). Brand-new items (id
            // still null - the common case for a Direct Purchase settled directly without
            // going through the Save Draft flow first) are deliberately skipped here: edit()
            // is em.merge(), which on a transient entity inserts a row but does NOT populate
            // this object's id - the loop below would then see getId()==null and create() a
            // second, duplicate row for the same item. Those items already carry the
            // distributed values in memory and get persisted correctly by the loop's own
            // create(i) call.
            for (BillItem item : getBillItems()) {
                if (item.getId() != null) {
                    getBillItemFacade().edit(item);
                }
            }
        }

        for (BillItem i : getBillItems()) {
            double lastPurchaseRate = 0.0;
            lastPurchaseRate = getPharmacyBean().getLastPurchaseRate(i.getItem());

            if (i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty() == 0.0) {
                continue;
            }

            billItemsTotalQty = billItemsTotalQty + i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty();

            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            getBillItemFacade().edit(i);

            saveBillFee(i);


            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatchWithCosting(i);


            double addingQty = BigDecimalUtil.valueOrZero(i.getBillItemFinanceDetails().getTotalQuantityByUnits()).doubleValue();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);

            Stock stock = getPharmacyBean().addToStockForCosting(i, Math.abs(addingQty), getSessionController().getDepartment());

            i.getPharmaceuticalBillItem().setLastPurchaseRate(lastPurchaseRate);
            i.getPharmaceuticalBillItem().setStock(stock);

            getBill().getBillItems().add(i);
        }

        // Recalculate BillFinanceDetails cost aggregates after distribution
        // This ensures bill-level totals reflect the updated cost rates with expenses
        if (getBill().getDiscount() != 0.0 || getBill().getTax() != 0.0 || getBill().getExpensesTotalConsideredForCosting() != 0.0) {
            recalculateBillFinanceDetailsCostAggregates();
        }

        //check and calculate expenses separately
        if (billExpenses != null && !billExpenses.isEmpty()) {
            // Persist each expense explicitly and set the owning-side expenseBill
            // FK - relying on Bill.billExpenses' cascade alone leaves this FK
            // NULL, since the mappedBy side (Bill.billExpenses) is not the
            // owning side of the relationship (issue #21856).
            int expenseSerial = 0;
            double totalForExpenses = 0;
            for (BillItem expense : billExpenses) {
                expense.setSearialNo(expenseSerial++);
                expense.setExpenseBill(getBill());
                expense.setCreatedAt(new Date());
                expense.setCreater(getSessionController().getLoggedUser());
                if (expense.getId() == null) {
                    getBillItemFacade().create(expense);
                } else {
                    getBillItemFacade().edit(expense);
                }
                totalForExpenses += expense.getNetValue();
            }

            getBill().setExpenseTotal(-Math.abs(totalForExpenses));
            // Note: NetTotal is already correctly calculated by the service and includes expenses
            // Removed: getBill().setNetTotal(getBill().getNetTotal() + totalForExpenses);
        }

//        getPharmacyBillBean().calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
        getBillFacade().edit(getBill());
        finalizeBill();
        approveBill();

        boolean generatePayments = configOptionApplicationController.getBooleanValueByKey(
            "Generate Payments for GRN, GRN Returns, Direct Purchase, and Direct Purchase Returns", false);
        if (generatePayments) {
            List<Payment> ps = paymentService.createPayment(getBill(), getPaymentMethodData());
        }

        JsfUtil.addSuccessMessage("Direct Purchase Successfully Completed.");
        printPreview = true;
    }

    public void removeItem(BillItem bi) {
        if (getBill().isCompleted()) {
            JsfUtil.addErrorMessage("This bill is completed and cannot be edited.");
            return;
        }
        getBillItems().remove(bi);

        int i = 0;
        for (BillItem it : getBillItems()) {
            it.setSearialNo(i++);
        }

        // Clear department type if all items are removed
        if (getBillItems().isEmpty()) {
            getBill().setDepartmentType(null);
        }

        calculateBillTotalsFromItems();
        currentBillItem = null;
    }

    public void prepareEditBillItem(BillItem bi) {
        this.editingBillItem = bi;
    }

    public void updateBillItem() {
        if (getBill().isCompleted()) {
            JsfUtil.addErrorMessage("This bill is completed and cannot be edited.");
            return;
        }
        if (editingBillItem == null) {
            JsfUtil.addErrorMessage("No item selected for editing");
            return;
        }
        BillItemFinanceDetails f = editingBillItem.getBillItemFinanceDetails();
        if (f != null) {
            Item item = editingBillItem.getItem();
            PharmaceuticalBillItem pbi = editingBillItem.getPharmaceuticalBillItem();

            // Sync retailSaleRatePerUnit from retailSaleRate (same logic as onRetailSaleRateChange)
            if (f.getRetailSaleRate() != null) {
                if (item instanceof Ampp) {
                    double dblVal = item.getDblValue();
                    BigDecimal unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
                    f.setRetailSaleRatePerUnit(f.getRetailSaleRate().divide(unitsPerPack, MathContext.DECIMAL64));
                } else {
                    f.setRetailSaleRatePerUnit(f.getRetailSaleRate());
                }
            }

            if (!isAllowRetailRateBelowPurchaseRate() && isRetailRateBelowPurchaseRate(item, f)) {
                JsfUtil.addErrorMessage("Retail rate is below the purchase rate. Enable 'Allow Retail Rate Below Purchase Rate in Pharmacy Purchasing' to proceed.");
                return;
            }

            // Sync wholesaleRatePerUnit from wholesaleRate (same logic as onWholesaleRateChange)
            if (f.getWholesaleRate() != null) {
                if (item instanceof Ampp) {
                    double dblVal = item.getDblValue();
                    BigDecimal unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
                    f.setWholesaleRatePerUnit(f.getWholesaleRate().divide(unitsPerPack, MathContext.DECIMAL64));
                } else {
                    f.setWholesaleRatePerUnit(f.getWholesaleRate());
                }
            }

            // Sync billItem.qty (pack-level quantity) - mirrors addItem() line 333
            editingBillItem.setQty(BigDecimalUtil.valueOrZero(f.getQuantity()).doubleValue());

            // Sync pack-level fields on PharmaceuticalBillItem - mirrors addItem() lines 302-313
            if (pbi != null) {
                if (item instanceof Ampp) {
                    pbi.setQtyPacks(BigDecimalUtil.valueOrZero(f.getQuantity()).doubleValue());
                    pbi.setFreeQtyPacks(BigDecimalUtil.valueOrZero(f.getFreeQuantity()).doubleValue());
                    pbi.setPurchaseRatePack(BigDecimalUtil.valueOrZero(f.getLineNetRate()).doubleValue());
                    pbi.setRetailRatePack(BigDecimalUtil.valueOrZero(f.getRetailSaleRate()).doubleValue());
                } else {
                    pbi.setQtyPacks(BigDecimalUtil.valueOrZero(f.getQuantityByUnits()).doubleValue());
                    pbi.setFreeQtyPacks(BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits()).doubleValue());
                    pbi.setPurchaseRatePack(BigDecimalUtil.valueOrZero(f.getLineNetRate()).doubleValue());
                    pbi.setRetailRatePack(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());
                }
            }
        }
        calculateItemTotals(editingBillItem);
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems();
        recalculateProfitMarginsForAllItems();
        editingBillItem = null;
    }

    private boolean isRetailRateBelowPurchaseRate(Item item, BillItemFinanceDetails f) {
        BigDecimal purchaseRatePerUnit = BigDecimalUtil.valueOrZero(f.getLineGrossRate());
        if (item instanceof Ampp) {
            BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
            if (unitsPerPack.compareTo(BigDecimal.ZERO) <= 0) {
                double dblVal = item.getDblValue();
                unitsPerPack = BigDecimal.valueOf(dblVal > 0 ? dblVal : 1);
            }
            purchaseRatePerUnit = purchaseRatePerUnit.divide(unitsPerPack, 6, RoundingMode.HALF_UP);
        }
        return purchaseRatePerUnit.compareTo(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit())) > 0;
    }

    /**
     * Autocomplete method for items filtered by department type
     * When department type is set on the bill, only items of that type are returned
     * Otherwise, items from all allowed department types are returned
     */
    public List<Item> completeItemsFilteredByDepartmentType(String query) {
        DepartmentType filterType = null;
        if (getBill() != null && getBill().getDepartmentType() != null) {
            filterType = getBill().getDepartmentType();
        }
        return itemController.completeAmpAndAmppItemForLoggedDepartment(query, filterType);
    }

    public Payment createPayment(Bill bill) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, bill.getPaymentMethod());
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            paymentFacade.create(p);
        }

    }

    public void saveBillFee(BillItem bi) {
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

    public void saveBill() {

        // Handle Department ID generation (independent)
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else {
            // Smart fallback logic
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)
                    || configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
                insId = deptId; // Use same number as department
            } else {
                // Use existing institution method for backward compatibility
                insId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
            }
        }

        getBill().setDeptId(deptId);
        getBill().setInsId(insId);
        getBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);

        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setDepartment(getSessionController().getDepartment());

        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());

        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        } else {
            getBillFacade().edit(getBill());
        }

    }

    public void finalizeBill() {
        getBill().setChecked(true);
        getBill().setCheckeAt(new Date());
        getBill().setCheckedBy(getSessionController().getLoggedUser());
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        } else {
            getBillFacade().edit(getBill());
        }
    }

    public void approveBill() {
        getBill().setCompleted(true);
        getBill().setCompletedAt(new Date());
        getBill().setCompletedBy(getSessionController().getLoggedUser());

        // Add missing approval tracking variables to match GRN approve process
        getBill().setApproveUser(getSessionController().getLoggedUser());
        getBill().setApproveAt(new Date());
        getBill().setEditor(getSessionController().getLoggedUser());
        getBill().setEditedAt(new Date());

        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        } else {
            getBillFacade().edit(getBill());
        }
    }


    // <editor-fold defaultstate="collapsed" desc="Draft Workflow Methods">

    /**
     * Persists the current bill and items as a draft (PRE type, not completed).
     * Shared by the explicit Save Draft action and by Finalize, which
     * transparently saves first when no draft has been saved yet.
     *
     * @return true if the draft was persisted, false if validation failed (an
     * error message has already been added to the growl in that case)
     */
    private boolean persistDraftDirectPurchase() {
        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items before saving");
            return false;
        }
        if (getBill().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Please select a Supplier");
            return false;
        }

        // Guard against a stale session (this bean's in-memory `bill` was loaded
        // before another user finalized/approved it) blindly reopening an
        // already-completed bill: the writes below unconditionally reset
        // billTypeAtomic/checked/completed, which would silently un-approve it
        // if this check weren't here first. Only relevant for a bill that's
        // already persisted (getId() != null) — a brand-new bill can't be
        // completed by anyone yet.
        if (getBill().getId() != null) {
            com.divudi.core.entity.Bill freshCheck = billService.reloadBill(getBill());
            if (freshCheck != null && freshCheck.isCompleted()) {
                JsfUtil.addErrorMessage("This bill has already been finalized/approved by another user and cannot be edited. Please refresh.");
                return false;
            }
        }

        // Save bill header as PRE type — no bill number yet, no stock
        getBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_PRE);
        getBill().setBillType(com.divudi.core.data.BillType.PharmacyPurchaseBill);
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setChecked(false);
        getBill().setCompleted(false);

        // For a brand-new bill, create it now so items/expenses below have a
        // real Bill FK to attach to. For an already-persisted draft, do NOT
        // edit(bill) here yet: at this point Bill.billExpenses (CascadeType.ALL)
        // may still hold a newly-added, not-yet-persisted expense (added via
        // addExpense(), which also appends to Bill.billExpenses for live total
        // calculations). An early edit()/merge() here would cascade-persist
        // that transient expense as a phantom row (with a generated ID this
        // in-memory session never learns about), which the explicit
        // create()-vs-edit() check in the expense-persist loop below can't
        // see -- causing a duplicate BillItem row (caught and soft-retired by
        // the "retire removed expenses" cleanup, but still wasteful, same
        // flavor of bug as the #23005 orphan-bill issue). The header field
        // changes made above are flushed later by this method's final
        // syncBillItemsCollectionFromDatabase()+edit(bill) call, once every
        // item/expense already has a real ID and cascade-merge can no longer
        // duplicate anything.
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }

        // Retire any previously persisted items that were removed from the session list
        if (getBill().getId() != null) {
            java.util.Map<String, Object> retireParams = new java.util.HashMap<>();
            retireParams.put("billId", getBill().getId());
            List<BillItem> persistedItems = getBillItemFacade().findByJpql(
                "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId AND bi.retired = false",
                retireParams);
            java.util.Set<Long> sessionIds = new java.util.HashSet<>();
            for (BillItem bi : getBillItems()) {
                if (bi.getId() != null) {
                    sessionIds.add(bi.getId());
                }
            }
            for (BillItem persisted : persistedItems) {
                if (!sessionIds.contains(persisted.getId())) {
                    persisted.setRetired(true);
                    persisted.setRetireComments("Removed during draft edit");
                    getBillItemFacade().edit(persisted);
                }
            }
        }

        // Save each bill item (PharmaceuticalBillItem cascades automatically)
        int serial = 0;
        for (BillItem bi : getBillItems()) {
            bi.setSearialNo(serial++);
            bi.setBill(getBill());
            bi.setCreatedAt(new Date());
            bi.setCreater(getSessionController().getLoggedUser());
            if (bi.getId() == null) {
                getBillItemFacade().create(bi);
            } else {
                getBillItemFacade().edit(bi);
            }
        }

        // Retire any previously persisted expenses that were removed from the session list
        java.util.Map<String, Object> retireExpenseParams = new java.util.HashMap<>();
        retireExpenseParams.put("billId", getBill().getId());
        List<BillItem> persistedExpenses = getBillItemFacade().findByJpql(
            "SELECT be FROM BillItem be WHERE be.expenseBill.id = :billId AND be.retired = false",
            retireExpenseParams);
        java.util.Set<Long> sessionExpenseIds = new java.util.HashSet<>();
        for (BillItem be : getBillExpenses()) {
            if (be.getId() != null) {
                sessionExpenseIds.add(be.getId());
            }
        }
        for (BillItem persisted : persistedExpenses) {
            if (!sessionExpenseIds.contains(persisted.getId())) {
                persisted.setRetired(true);
                persisted.setRetireComments("Removed during draft edit");
                getBillItemFacade().edit(persisted);
            }
        }

        // Save each bill expense explicitly - do not rely on Bill.billExpenses'
        // cascade alone, since the owning-side expenseBill FK must be set on
        // each child for the cascade-insert to actually link back to this bill
        int expenseSerial = 0;
        double totalForExpenses = 0.0;
        for (BillItem expense : getBillExpenses()) {
            expense.setSearialNo(expenseSerial++);
            expense.setExpenseBill(getBill());
            expense.setCreatedAt(new Date());
            expense.setCreater(getSessionController().getLoggedUser());
            if (expense.getId() == null) {
                getBillItemFacade().create(expense);
            } else {
                getBillItemFacade().edit(expense);
            }
            totalForExpenses += expense.getNetValue();
        }
        getBill().setExpenseTotal(-Math.abs(totalForExpenses));

        syncBillItemsCollectionFromDatabase();
        getBillFacade().edit(getBill());
        draftMode = true;
        return true;
    }

    /**
     * Bill.billItems has orphanRemoval=true, but this method persists each BillItem
     * directly via BillItemFacade rather than through that collection, so the in-memory
     * bill's billItems field is otherwise left null/stale across repeated calls. If
     * something has since refreshed this bill's shared cache entry (e.g. a Finalize
     * attempt blocked by validation, which reloads the bill before returning), a later
     * edit(bill) can treat that stale/empty collection as authoritative and orphan-delete
     * the BillItem rows this method just persisted (issue #21900). Re-fetching the
     * collection immediately before each edit(bill) keeps it accurate.
     */
    private void syncBillItemsCollectionFromDatabase() {
        if (getBill().getId() == null) {
            return;
        }
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("billId", getBill().getId());
        List<BillItem> currentlyPersistedItems = getBillItemFacade().findByJpql(
            "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId", params);
        getBill().getBillItems().clear();
        getBill().getBillItems().addAll(currentlyPersistedItems);
    }

    public void saveDraftDirectPurchase() {
        if (persistDraftDirectPurchase()) {
            JsfUtil.addSuccessMessage("Direct Purchase draft saved successfully.");
        }
    }

    public void finalizeDraftDirectPurchase() {
        // Always call persistDraftDirectPurchase() first: it is the method that
        // actually creates the bill (addItem() no longer creates a bare row),
        // and it also handles the create-vs-edit branching for a draft that is
        // being resumed and re-saved.
        if (!persistDraftDirectPurchase()) {
            return;
        }

        // Fresh DB read for early, friendly messaging (payment method validation
        // needs a real read anyway) — the actual concurrency guard is the atomic
        // claim below, not this check by itself, since a second session could
        // still pass this same read between here and the claim.
        com.divudi.core.entity.Bill freshBill = billService.reloadBill(bill);
        if (freshBill == null) {
            JsfUtil.addErrorMessage("Draft bill not found in database.");
            return;
        }
        if (freshBill.isCompleted()) {
            JsfUtil.addErrorMessage("This draft was already finalized by another user. Please refresh the list.");
            return;
        }
        if (!isPaymentMethodValid(freshBill)) {
            return;
        }

        // Atomic claim: only one concurrent finalize() call on this bill can
        // win (COMPLETED=0 -> 1 in a single UPDATE). A losing call gets 0 rows
        // affected here rather than silently overwriting the winner's write.
        boolean claimed = directPurchaseApprovingService.claimForFinalize(
                freshBill.getId(), getSessionController().getLoggedUser().getId());
        if (!claimed) {
            JsfUtil.addErrorMessage("This draft was already finalized by another user. Please refresh the list.");
            return;
        }
        bill = (com.divudi.core.entity.BilledBill) billService.reloadBill(freshBill);

        JsfUtil.addSuccessMessage("Direct Purchase finalized. It is now pending approval.");
        printPreview = true;
    }

    public void approveDirectPurchaseDraft() {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("No draft loaded.");
            return;
        }

        // Fresh DB read to guard against concurrent approval
        com.divudi.core.entity.Bill freshBill = billService.reloadBill(bill);
        if (freshBill == null) {
            JsfUtil.addErrorMessage("Draft bill not found in database.");
            return;
        }
        if (!freshBill.isCompleted()) {
            JsfUtil.addErrorMessage("Bill must be finalized before it can be approved.");
            return;
        }
        if (freshBill.isChecked()) {
            JsfUtil.addErrorMessage("This bill was already approved by another user. Please refresh the list.");
            return;
        }
        // The Payment Method dropdown on direct_purchase.xhtml stays editable on this screen
        // (unlike items/expenses), but nothing else on this page persists it, and unlike
        // finalize (which calls persistDraftDirectPurchase() first) nothing saves `bill`
        // before this method runs either - so a selection made here would otherwise be
        // silently discarded by the DB reload above on every Approve click. Validate the
        // pending in-memory selection itself (not the possibly-stale freshBill copy) before
        // persisting it, so an invalid choice is rejected without ever being written.
        if (!isPaymentMethodValid(bill)) {
            return;
        }
        PaymentMethod pendingPaymentMethod = bill.getPaymentMethod();
        if (freshBill.getPaymentMethod() != pendingPaymentMethod) {
            freshBill.setPaymentMethod(pendingPaymentMethod);
            getBillFacade().edit(freshBill);
        }

        // Switch session bill to the fresh DB copy so finalizeBill()/approveBill() operate on it
        bill = (com.divudi.core.entity.BilledBill) freshBill;

        // Atomic claim BEFORE generating a bill number or touching stock,
        // replacing the old in-memory setChecked(true)+edit(): only one
        // concurrent approve() call on this bill can win (BILLTYPEATOMIC
        // PRE->final and CHECKED 0->1 in a single UPDATE gated on the current
        // state). A losing call gets 0 rows affected and must not generate a
        // number or touch stock - generating the number only after a
        // successful claim avoids permanently burning a sequence value on a
        // losing/failed attempt.
        boolean claimed = directPurchaseApprovingService.claimForApproval(
                getBill().getId(), getSessionController().getLoggedUser().getId());
        if (!claimed) {
            JsfUtil.addErrorMessage("This bill was already approved by another user. Please refresh the list.");
            return;
        }
        // Reload to pick up the natively-claimed BILLTYPEATOMIC/CHECKED/CHECKEDBY/
        // CHECKEAT before any further edit(), so those aren't overwritten with
        // stale in-memory values.
        bill = (com.divudi.core.entity.BilledBill) billService.reloadBill(getBill());

        // Generate real bill number (mirrors saveBill()) - only reached once
        // this session has already won the claim above.
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        }

        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        } else {
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)
                    || configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
                insId = deptId;
            } else {
                insId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
            }
        }

        getBill().setDeptId(deptId);
        getBill().setInsId(insId);
        getBillFacade().edit(getBill());

        // Reload bill items from DB to ensure we have the persisted state
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("billId", getBill().getId());
        billItems = billItemFacade.findByJpql(
            "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId AND bi.retired = false ORDER BY bi.searialNo",
            params);
        String expJpql = "SELECT be FROM BillItem be WHERE be.expenseBill.id = :billId AND be.retired = false ORDER BY be.searialNo";
        billExpenses = billItemFacade.findByJpql(expJpql, params);

        // Calculate bill-level totals and distribute bill-level adjustments (discount,
        // tax, expenses) into each item's cost rate BEFORE ItemBatch/Stock are created
        // below. saveItemBatchWithCosting() reads BillItemFinanceDetails.totalCostRate
        // to set ItemBatch.costRate (which StockHistory then snapshots) - if this ran
        // after item batches were created, the batch/stock would be left with the
        // pre-distribution cost rate forever while the report's cost totals reflect
        // the post-distribution one, producing a permanent COGS cost variance (mirrors
        // the same fix in settleDirectPurchaseBillFinally()).
        calculateBillTotalsFromItems();
        if (getBill().getDiscount() != 0.0 || getBill().getTax() != 0.0 || getBill().getExpensesTotalConsideredForCosting() != 0.0) {
            distributeProportionalBillValuesToItems();
            // Unlike settle (fully in-memory graph, cascaded on the final bill edit),
            // these items were reloaded from DB above, so they need an explicit edit
            // here for the distributed values to persist.
            for (BillItem item : getBillItems()) {
                getBillItemFacade().edit(item);
            }
        }

        // Add stock for each item (mirrors settleDirectPurchaseBillFinally())
        billItemsTotalQty = 0;
        for (BillItem i : getBillItems()) {
            if (i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty() == 0.0) {
                continue;
            }
            double lastPurchaseRate = getPharmacyBean().getLastPurchaseRate(i.getItem());
            billItemsTotalQty += i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty();
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());
            getBillItemFacade().edit(i);
            saveBillFee(i);

            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatchWithCosting(i);
            double addingQty = BigDecimalUtil.valueOrZero(i.getBillItemFinanceDetails().getTotalQuantityByUnits()).doubleValue();
            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
            Stock stock = getPharmacyBean().addToStockForCosting(i, Math.abs(addingQty), getSessionController().getDepartment());
            i.getPharmaceuticalBillItem().setLastPurchaseRate(lastPurchaseRate);
            i.getPharmaceuticalBillItem().setStock(stock);
            // Persist the stock link explicitly. Unlike the settle path (where
            // the bill and items are an in-memory graph fully merged at the end),
            // here the items were reloaded from DB and the detached bill's lazy
            // billItems collection does not carry this change through the final
            // bill merge - without this edit, phi.stock stays NULL in the DB and
            // a later Direct Purchase Return fails with "Stock information not
            // available for item".
            getBillItemFacade().edit(i);
            getBill().getBillItems().add(i);
        }

        if (getBill().getDiscount() != 0.0 || getBill().getTax() != 0.0 || getBill().getExpensesTotalConsideredForCosting() != 0.0) {
            recalculateBillFinanceDetailsCostAggregates();
        }

        if (billExpenses != null && !billExpenses.isEmpty()) {
            getBill().setBillExpenses(billExpenses);
            double totalForExpenses = 0;
            for (BillItem expense : getBillExpenses()) {
                totalForExpenses += expense.getNetValue();
            }
            getBill().setExpenseTotal(-Math.abs(totalForExpenses));
        }

        getBillFacade().edit(getBill());
        finalizeBill();
        approveBill();

        boolean generatePayments = configOptionApplicationController.getBooleanValueByKey(
            "Generate Payments for GRN, GRN Returns, Direct Purchase, and Direct Purchase Returns", false);
        if (generatePayments) {
            paymentService.createPayment(getBill(), getPaymentMethodData());
        }

        JsfUtil.addSuccessMessage("Direct Purchase approved. Bill number: " + deptId + ". Stock updated.");
        printPreview = true;
    }

    public boolean isDraftMode() {
        return draftMode;
    }

    public void setDraftMode(boolean draftMode) {
        this.draftMode = draftMode;
    }

    // </editor-fold>

    public double getNetTotal() {
        // If NetTotal has already been calculated by the service (includes expenses), return it as-is
        if (getBill().getNetTotal() != 0.0) {
            return getBill().getNetTotal(); // Return the calculated value (negative for purchases)
        }

        // Fallback calculation for cases where service hasn't calculated yet
        double tmp = getBill().getTotal() + getBill().getTax() - getBill().getDiscount();
        getBill().setNetTotal(0 - tmp);

        return 0 - tmp; // Return negative value for purchase transactions
    }

    public void calTotal() {
        double tot = 0.0;
        double saleValue = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            p.setQty(p.getPharmaceuticalBillItem().getQtyInUnit());
            p.setRate(p.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            p.setSearialNo(serialNo++);
            double netValue = p.getQty() * p.getRate();
            p.setNetValue(0 - netValue);
            tot += p.getNetValue();
            saleValue += (p.getPharmaceuticalBillItem().getQtyInUnit() + p.getPharmaceuticalBillItem().getFreeQtyInUnit()) * p.getPharmaceuticalBillItem().getRetailRate();
        }
        getBill().setTotal(tot);
        getBill().setNetTotal(tot);
        getBill().setSaleValue(saleValue);
    }

    /**
     * Calculates individual item totals (gross, discount, net)
     *
     * @param billItem the bill item to calculate
     */
    private void calculateItemTotals(BillItem billItem) {
        if (billItem == null || billItem.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = billItem.getBillItemFinanceDetails();

        // Safe null handling for all finance detail fields
        BigDecimal qty = BigDecimalUtil.valueOrZero(f.getQuantity());
        BigDecimal purchaseRate = BigDecimalUtil.valueOrZero(f.getLineGrossRate());
        BigDecimal discountRate = BigDecimalUtil.valueOrZero(f.getLineDiscountRate());

        // Item Gross = Quantity × Purchase Rate
        BigDecimal itemGross = BigDecimalUtil.multiply(qty, purchaseRate);
        f.setLineGrossTotal(itemGross);

        // Item Discount = Quantity × Discount Rate
        BigDecimal itemDiscount = BigDecimalUtil.multiply(qty, discountRate);
        f.setLineDiscount(itemDiscount);

        // Item Net = Gross - Discount
        BigDecimal itemNet = BigDecimalUtil.subtract(itemGross, itemDiscount);
        f.setLineNetTotal(itemNet);

        // Set line net rate (safe division)
        if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
            f.setLineNetRate(itemNet.divide(qty, 4, RoundingMode.HALF_UP));
        } else {
            // When quantity is zero, preserve the user's entered purchase rate as line net rate
            // This ensures the purchase rate is retained for free items
            f.setLineNetRate(BigDecimalUtil.valueOrZero(f.getLineGrossRate()));
        }

        // Calculate cost value (line cost = net total for purchases)
        f.setLineCost(itemNet);

        // Ensure unit-based calculations are updated for UI display
        // Ensure free quantity is properly initialized when left blank
        if (f.getFreeQuantity() == null) {
            f.setFreeQuantity(BigDecimal.ZERO);
        }
        BigDecimal freeQty = BigDecimalUtil.valueOrZero(f.getFreeQuantity());
        BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());

        // Recalculate quantities by units based on current quantities and units per pack
        BigDecimal qtyByUnits = BigDecimalUtil.multiply(qty, unitsPerPack);
        BigDecimal freeQtyByUnits = BigDecimalUtil.multiply(freeQty, unitsPerPack);
        BigDecimal totalQtyByUnits = BigDecimalUtil.add(qtyByUnits, freeQtyByUnits);

        // Update all unit-based quantities
        f.setQuantityByUnits(qtyByUnits);
        f.setFreeQuantityByUnits(freeQtyByUnits);
        f.setTotalQuantityByUnits(totalQtyByUnits);

        // Calculate total cost rate per unit (needed for ItemBatch costing)
        BigDecimal totalCostRate = totalQtyByUnits.compareTo(BigDecimal.ZERO) > 0
                ? itemNet.divide(totalQtyByUnits, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        f.setTotalCostRate(totalCostRate);


        // CRITICAL: Set missing BIFD fields identified by code reviewer
        // 1. Set gross total and net total (main fields, not just line totals)
        f.setGrossTotal(itemGross);
        f.setNetTotal(itemNet);

        // 2. Calculate and set gross rate (grossTotal divided by quantity)
        if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
            f.setGrossRate(itemGross.divide(qty, 4, RoundingMode.HALF_UP));
        } else {
            // When quantity is zero, preserve the user's entered purchase rate (lineGrossRate)
            // This is needed for free items where user enters purchase rate but quantity is zero
            f.setGrossRate(BigDecimalUtil.valueOrZero(f.getLineGrossRate()));
        }

        // 2b. Calculate and set net rate (netTotal divided by quantity)
        if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
            f.setNetRate(itemNet.divide(qty, 4, RoundingMode.HALF_UP));
        } else {
            // When quantity is zero, preserve the user's entered purchase rate as net rate
            // This ensures consistency for free items
            f.setNetRate(BigDecimalUtil.valueOrZero(f.getLineGrossRate()));
        }

        // 3. Calculate and set line cost rate per unit
        if (totalQtyByUnits != null && totalQtyByUnits.compareTo(BigDecimal.ZERO) > 0) {
            f.setLineCostRate(itemNet.divide(totalQtyByUnits, 4, RoundingMode.HALF_UP));
        } else {
            f.setLineCostRate(BigDecimal.ZERO);
        }

        // Set costRate (as user enters - pack rate for AMPP, unit rate for AMP)
        f.setCostRate(BigDecimalUtil.multiply(BigDecimalUtil.valueOrZero(f.getLineCostRate()), unitsPerPack));

        // Set purchaseRate (line net rate - purchase rate after discount, as user enters)
        f.setPurchaseRate(BigDecimalUtil.valueOrZero(f.getLineNetRate()));

        // Normalize purchase rate to per-unit; AMPP items enter pack rate so we divide by pack size
        // Regression note: prevents AMPP purchases from multiplying pack size twice (Oct 2025 change)
        BigDecimal netRateAtEntry = BigDecimalUtil.valueOrZero(f.getNetRate());
        BigDecimal purchaseRatePerUnit = netRateAtEntry;
        if (billItem.getItem() instanceof Ampp) {
            // Guard against null/zero pack sizes to avoid divide-by-zero
            BigDecimal safeUnitsPerPack = unitsPerPack.compareTo(BigDecimal.ZERO) > 0 ? unitsPerPack : BigDecimal.ONE;
            // Convert the pack-level net rate to a unit-level rate for downstream value calculations
            purchaseRatePerUnit = netRateAtEntry.divide(safeUnitsPerPack, 6, RoundingMode.HALF_UP);
        }

        // 4. Calculate value fields for all rate types using total quantity by units
        BigDecimal totalUnits = BigDecimalUtil.valueOrZero(f.getTotalQuantityByUnits());
        if (totalUnits.compareTo(BigDecimal.ZERO) > 0) {
            // Value at retail rate
            f.setValueAtRetailRate(BigDecimalUtil.multiply(totalUnits,
                    BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit())));

            // Value at cost rate (using the calculated line cost rate)
            f.setValueAtCostRate(BigDecimalUtil.multiply(totalUnits,
                    BigDecimalUtil.valueOrZero(f.getLineCostRate())));

            // Value at wholesale rate (if wholesale rate is set)
            BigDecimal wholesaleRate = f.getWholesaleRatePerUnit();
            if (wholesaleRate != null) {
                f.setValueAtWholesaleRate(BigDecimalUtil.multiply(totalUnits, wholesaleRate));
            }
        } else {
            // Set zero values if no quantity
            f.setValueAtRetailRate(BigDecimal.ZERO);
            f.setValueAtCostRate(BigDecimal.ZERO);
            f.setValueAtWholesaleRate(BigDecimal.ZERO);
        }

        // Calculate valueAtPurchaseRate based on configuration
        if (configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true)) {
            // Net Rate × Total Quantity (includes free items)
            // Value at purchase becomes unit rate × total units, keeping AMPP aligned with AMP
            f.setValueAtPurchaseRate(
                    BigDecimalUtil.multiply(totalUnits, purchaseRatePerUnit)
            );
        } else {
            // Net Rate × Paid Quantity (actual money spent, excludes free items)
            f.setValueAtPurchaseRate(
                    BigDecimalUtil.multiply(BigDecimalUtil.valueOrZero(f.getLineNetRate()), qty)
            );
        }

        // Update BillItem values with safe null handling
        billItem.setGrossValue(itemGross != null ? itemGross.doubleValue() : 0.0);
        billItem.setNetValue(itemNet != null ? itemNet.doubleValue() : 0.0);
        billItem.setRate(purchaseRate != null ? purchaseRate.doubleValue() : 0.0);
        billItem.setNetRate(f.getLineNetRate() != null ? f.getLineNetRate().doubleValue() : 0.0);

        // Update PharmaceuticalBillItem with calculated values in units
        PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();
        // pbi will never be null as it is created in the getter
        BigDecimal retailRatePerUnit = BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit());
        BigDecimal costRatePerUnit = BigDecimalUtil.valueOrZero(f.getLineCostRate());
        // Update quantities in units (important for stock calculations)
        pbi.setQty(qtyByUnits.doubleValue()); // Paid quantity in units
        pbi.setFreeQty(freeQtyByUnits.doubleValue()); // Free quantity in units

        // Update rates per unit using normalized purchase rate (AMPP-aware)
        pbi.setPurchaseRate(purchaseRatePerUnit.doubleValue()); // Purchase rate per unit
        pbi.setCostRate(costRatePerUnit.doubleValue()); // Cost rate per unit
        pbi.setRetailRate(retailRatePerUnit.doubleValue()); // Retail rate per unit

        // Calculate values (quantity × rate) so unit-level totals remain consistent
        BigDecimal pbiPurchaseValue = BigDecimalUtil.multiply(qtyByUnits, purchaseRatePerUnit);
        BigDecimal pbiCostValue = BigDecimalUtil.multiply(qtyByUnits, costRatePerUnit);
        BigDecimal pbiRetailValue = BigDecimalUtil.multiply(qtyByUnits, retailRatePerUnit);

        pbi.setPurchaseValue(BigDecimalUtil.valueOrZero(pbiPurchaseValue).doubleValue());
        pbi.setCostValue(BigDecimalUtil.valueOrZero(pbiCostValue).doubleValue());
        pbi.setRetailValue(BigDecimalUtil.valueOrZero(pbiRetailValue).doubleValue());

    }

    /**
     * Calculates sale value for an item: (Paid Qty + Free Qty) × Retail Rate
     *
     * @param billItem the bill item to calculate
     * @return the calculated sale value
     */
    private BigDecimal calculateSaleValue(BillItem billItem) {
        if (billItem == null || billItem.getBillItemFinanceDetails() == null) {
            return BigDecimal.ZERO;
        }

        BillItemFinanceDetails f = billItem.getBillItemFinanceDetails();

        // Use total quantity by units (includes both paid and free quantities in unit form)
        BigDecimal totalQtyByUnits = BigDecimalUtil.valueOrZero(f.getTotalQuantityByUnits());
        BigDecimal retailRatePerUnit = BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit());

        // Sale Value = Total Qty (in units) × Retail Rate Per Unit
        return BigDecimalUtil.multiply(totalQtyByUnits, retailRatePerUnit);
    }

    /**
     * Calculates bill-level totals from all items for Direct Purchase workflow.
     * Populates BillFinanceDetails (purchase/retail/cost/gross/net, quantities,
     * discounts, taxes, expenses).
     */
    public void calculateBillTotalsFromItems() {
        if (getBill() == null || getBillItems() == null || getBillItems().isEmpty()) {
            return;
        }

        // Read config once before the loop for consistency
        boolean purchaseValueIncludesFreeItems = configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true);

        // Initialize aggregates
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalLineExpenses = BigDecimal.ZERO;
        BigDecimal totalLineCosts = BigDecimal.ZERO;
        BigDecimal totalTaxLines = BigDecimal.ZERO;

        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;

        // Free/Non-free breakdowns
        BigDecimal purchaseValueFree = BigDecimal.ZERO;
        BigDecimal purchaseValueNonFree = BigDecimal.ZERO;
        BigDecimal costValueFree = BigDecimal.ZERO;
        BigDecimal costValueNonFree = BigDecimal.ZERO;
        BigDecimal retailValueFree = BigDecimal.ZERO;
        BigDecimal retailValueNonFree = BigDecimal.ZERO;
        BigDecimal wholesaleValueFree = BigDecimal.ZERO;
        BigDecimal wholesaleValueNonFree = BigDecimal.ZERO;

        BigDecimal grossTotalLines = BigDecimal.ZERO;      // Sum of line gross totals
        BigDecimal netTotalLines = BigDecimal.ZERO;        // Sum of line net totals

        // Walk through items and aggregate
        for (BillItem bi : getBillItems()) {
            BillItemFinanceDetails f = (bi != null) ? bi.getBillItemFinanceDetails() : null;
            if (f == null) {
                continue;
            }

            BigDecimal qty = BigDecimalUtil.valueOrZero(f.getQuantity());
            BigDecimal freeQty = BigDecimalUtil.valueOrZero(f.getFreeQuantity());
            BigDecimal totalQtyPacks = qty.add(freeQty);

            // Fallbacks for missing computed fields
            BigDecimal lineGrossRate = BigDecimalUtil.valueOrZero(f.getLineGrossRate());
            if (f.getLineGrossTotal() == null) {
                f.setLineGrossTotal(lineGrossRate.multiply(qty));
            }
            if (f.getGrossTotal() == null || f.getGrossTotal().compareTo(BigDecimal.ZERO) == 0) {
                f.setGrossTotal(lineGrossRate.multiply(qty));
            }
            if (f.getLineNetTotal() == null) {
                BigDecimal lineDiscount = BigDecimalUtil.valueOrZero(f.getLineDiscount());
                f.setLineNetTotal(BigDecimalUtil.subtract(f.getLineGrossTotal(), lineDiscount));
            }
            if (f.getNetTotal() == null) {
                f.setNetTotal(f.getLineNetTotal());
            }

            // Use unit-quantities for value-at-rate fields when available
            BigDecimal qtyUnits = BigDecimalUtil.valueOrZero(f.getQuantityByUnits());
            BigDecimal freeUnits = BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits());
            BigDecimal totalUnits = BigDecimalUtil.add(qtyUnits, freeUnits);
            BigDecimal retailPerUnit = BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit());
            BigDecimal netRateAtEntry = BigDecimalUtil.valueOrZero(f.getNetRate());
            BigDecimal unitsPerPackForEntry = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
            BigDecimal netPerUnit = netRateAtEntry;
            if (bi != null && bi.getItem() instanceof Ampp) {
                // Bill items store AMPP net rate per pack; convert to per unit so aggregations stay consistent
                BigDecimal safeUnitsPerPack = unitsPerPackForEntry.compareTo(BigDecimal.ZERO) > 0 ? unitsPerPackForEntry : BigDecimal.ONE;
                // Division uses HALF_UP to retain accuracy without extra rounding drift across the bill
                netPerUnit = netRateAtEntry.divide(safeUnitsPerPack, 6, RoundingMode.HALF_UP);
            }
            BigDecimal costPerUnit = BigDecimalUtil.valueOrZero(f.getLineCostRate());
            if (BigDecimalUtil.isNullOrZero(costPerUnit) && totalUnits.compareTo(BigDecimal.ZERO) > 0 && f.getValueAtCostRate() != null) {
                // Derive if not set
                costPerUnit = f.getValueAtCostRate().divide(totalUnits, 4, RoundingMode.HALF_UP);
                f.setLineCostRate(costPerUnit);
            }

            // Compute value-at-rate fields if missing
            if (f.getValueAtRetailRate() == null) {
                f.setValueAtRetailRate(totalUnits.multiply(retailPerUnit));
            }
            if (f.getValueAtPurchaseRate() == null) {
                if (configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true)) {
                    // Net Rate × Total Quantity (includes free items)
                    f.setValueAtPurchaseRate(totalUnits.multiply(netPerUnit));
                } else {
                    // Net Rate × Paid Quantity (excludes free items)
                    BigDecimal lineNetRate = BigDecimalUtil.valueOrZero(f.getLineNetRate());
                    f.setValueAtPurchaseRate(lineNetRate.multiply(qty));
                }
            }
            if (f.getValueAtCostRate() == null) {
                f.setValueAtCostRate(totalUnits.multiply(costPerUnit));
            }

            // Compute free/non-free breakdowns per item based on config
            if (purchaseValueIncludesFreeItems) {
                // Use net rate for both free and non-free
                purchaseValueNonFree = purchaseValueNonFree.add(netPerUnit.multiply(qtyUnits));
                purchaseValueFree = purchaseValueFree.add(netPerUnit.multiply(freeUnits));
            } else {
                // Use actual paid value (valueAtPurchaseRate) for non-free, zero for free
                purchaseValueNonFree = purchaseValueNonFree.add(BigDecimalUtil.valueOrZero(f.getValueAtPurchaseRate()));
                purchaseValueFree = purchaseValueFree.add(BigDecimal.ZERO);
            }

            costValueNonFree = costValueNonFree.add(costPerUnit.multiply(qtyUnits));
            costValueFree = costValueFree.add(costPerUnit.multiply(freeUnits));
            retailValueNonFree = retailValueNonFree.add(retailPerUnit.multiply(qtyUnits));
            retailValueFree = retailValueFree.add(retailPerUnit.multiply(freeUnits));
            BigDecimal wholesalePerUnit = BigDecimalUtil.valueOrZero(f.getWholesaleRatePerUnit());
            wholesaleValueNonFree = wholesaleValueNonFree.add(wholesalePerUnit.multiply(qtyUnits));
            wholesaleValueFree = wholesaleValueFree.add(wholesalePerUnit.multiply(freeUnits));

            // Aggregate line-level components
            totalLineDiscounts = totalLineDiscounts.add(BigDecimalUtil.valueOrZero(f.getLineDiscount()));
            totalLineExpenses = totalLineExpenses.add(BigDecimalUtil.valueOrZero(f.getLineExpense()));
            totalTaxLines = totalTaxLines.add(BigDecimalUtil.valueOrZero(f.getLineTax()));
            totalLineCosts = totalLineCosts.add(BigDecimalUtil.valueOrZero(f.getLineCost()));

            BigDecimal freeItemValue = costPerUnit.multiply(freeUnits);
            totalFreeItemValue = totalFreeItemValue.add(freeItemValue);

            // Totals at different rates
            totalPurchase = totalPurchase.add(BigDecimalUtil.valueOrZero(f.getValueAtPurchaseRate()));
            totalRetail = totalRetail.add(BigDecimalUtil.valueOrZero(f.getValueAtRetailRate()));
            totalWholesale = totalWholesale.add(BigDecimalUtil.valueOrZero(f.getValueAtWholesaleRate()));

            // Quantities
            totalQty = totalQty.add(qty);
            totalFreeQty = totalFreeQty.add(freeQty);
            totalQtyAtomic = totalQtyAtomic.add(BigDecimalUtil.valueOrZero(f.getQuantityByUnits()));
            totalFreeQtyAtomic = totalFreeQtyAtomic.add(BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits()));

            // Line totals
            grossTotalLines = grossTotalLines.add(BigDecimalUtil.valueOrZero(f.getLineGrossTotal()));
            netTotalLines = netTotalLines.add(BigDecimalUtil.valueOrZero(f.getLineNetTotal()));
        }

        // Sum current bill-level expenses (from expense bill items)
        double billExpensesTotal = 0.0;
        if (getBill().getBillExpenses() != null) {
            for (BillItem expense : getBill().getBillExpenses()) {
                if (expense != null && !expense.isRetired()) {
                    billExpensesTotal += expense.getNetValue();
                }
            }
        }

        // Bill-level values directly entered by user
        BigDecimal billDiscount = BigDecimal.valueOf(getBill().getDiscount());
        BigDecimal billTax = BigDecimal.valueOf(getBill().getTax());
        BigDecimal billExpenseConsidered = BigDecimal.valueOf(getBill().getExpensesTotalConsideredForCosting());
        BigDecimal billCost = billDiscount.subtract(billExpenseConsidered.add(billTax));

        // For purchase bills, legacy controller logic keeps totals negative.
        // Compute final net as line net + tax - discount + bill expenses considered for costing, then set negative on Bill.
        BigDecimal finalNet = netTotalLines.add(billTax).subtract(billDiscount).add(billExpenseConsidered);
        getBill().setTotal(-BigDecimalUtil.valueOrZero(netTotalLines).doubleValue());
        getBill().setNetTotal(-BigDecimalUtil.valueOrZero(finalNet).doubleValue());
        getBill().setSaleValue(BigDecimalUtil.valueOrZero(totalRetail).doubleValue());

        // Ensure and populate BillFinanceDetails
        BillFinanceDetails bfd = getBill().getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(getBill());
            getBill().setBillFinanceDetails(bfd);
        }

        bfd.setBillDiscount(billDiscount);
        bfd.setBillExpense(billExpenseConsidered);
        bfd.setBillTaxValue(billTax);
        bfd.setBillCostValue(billCost);

        bfd.setLineDiscount(totalLineDiscounts);
        bfd.setLineExpense(totalLineExpenses);
        bfd.setItemTaxValue(totalTaxLines);
        bfd.setLineCostValue(totalLineCosts);

        // Totals (line totals + bill-level where applicable)
        bfd.setTotalDiscount(totalLineDiscounts.add(billDiscount));
        bfd.setTotalExpense(totalLineExpenses.add(billExpenseConsidered));
        // Total Tax should include both line-level tax and bill-level tax
        bfd.setTotalTaxValue(totalTaxLines.add(billTax));
        bfd.setTotalCostValue(totalLineCosts);

        // Values at purchase/retail/cost
        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalPurchaseValue(totalPurchase);
        bfd.setTotalRetailSaleValue(totalRetail);
        bfd.setTotalWholesaleValue(totalWholesale);
        // Set free/non-free breakdowns used by UI panels
        bfd.setTotalPurchaseValueFree(purchaseValueFree);
        bfd.setTotalPurchaseValueNonFree(purchaseValueNonFree);
        bfd.setTotalCostValueFree(costValueFree);
        bfd.setTotalCostValueNonFree(costValueNonFree);
        bfd.setTotalRetailSaleValueFree(retailValueFree);
        bfd.setTotalRetailSaleValueNonFree(retailValueNonFree);
        bfd.setTotalWholesaleValueFree(wholesaleValueFree);
        bfd.setTotalWholesaleValueNonFree(wholesaleValueNonFree);

        // Quantities
        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);

        // Gross/Net totals snapshot (positive numbers inside BFD)
        bfd.setGrossTotal(grossTotalLines);
        bfd.setLineGrossTotal(grossTotalLines);
        bfd.setNetTotal(finalNet);
        bfd.setLineNetTotal(netTotalLines);
        BigDecimal expensesNotForCosting = BigDecimal.valueOf(getBill().getExpensesTotalNotConsideredForCosting());
        bfd.setBillExpensesConsideredForCosting(BigDecimal.valueOf(getBill().getExpensesTotalConsideredForCosting()));
        bfd.setBillExpensesNotConsideredForCosting(expensesNotForCosting);
        bfd.setTotalBillValue(finalNet.add(expensesNotForCosting));
    }

    /**
     * Distributes bill-level adjustments (tax, discount, expenses)
     * proportionally to line items based on their lineNetTotal values. This
     * ensures that bill-level changes are allocated to individual bill items
     * without affecting the line-level calculations.
     *
     * Copied and modified from PharmacyCostingService to avoid external
     * dependencies.
     */
    private void distributeProportionalBillValuesToItems() {
        BilledBill bill = getBill();
        List<BillItem> billItems = getBillItems();

        if (bill == null || billItems == null || billItems.isEmpty()) {
            return;
        }

        if (bill.getBillFinanceDetails() == null) {
            bill.setBillFinanceDetails(new BillFinanceDetails(bill));
        }

        // Get bill-level adjustments
        BigDecimal billDiscount = BigDecimal.valueOf(bill.getDiscount());
        BigDecimal billTax = BigDecimal.valueOf(bill.getTax());
        BigDecimal billExpensesConsidered = BigDecimal.valueOf(bill.getExpensesTotalConsideredForCosting());

        // Calculate total basis for proportional distribution (sum of all line net totals)
        BigDecimal totalBasis = BigDecimal.ZERO;
        Map<BillItem, BigDecimal> itemBases = new HashMap<>();

        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }

            BigDecimal lineNetTotal = BigDecimalUtil.valueOrZero(f.getLineNetTotal());
            itemBases.put(bi, lineNetTotal);
            totalBasis = totalBasis.add(lineNetTotal);
        }

        if (BigDecimalUtil.isNullOrZero(totalBasis)) {
            return;
        }

        // Distribute bill-level values proportionally to each item
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }

            BigDecimal basis = itemBases.get(bi);
            BigDecimal ratio = basis.divide(totalBasis, 12, RoundingMode.HALF_UP);

            // Calculate distributed amounts
            BigDecimal distributedDiscount = billDiscount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal distributedTax = billTax.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal distributedExpense = billExpensesConsidered.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            // Set bill-level distribution values (these are additional to line values)
            f.setBillDiscount(distributedDiscount);
            f.setBillTax(distributedTax);
            f.setBillExpense(distributedExpense);

            // Calculate totals including both line and bill-level values
            BigDecimal lineDiscount = BigDecimalUtil.valueOrZero(f.getLineDiscount());
            BigDecimal lineTax = BigDecimalUtil.valueOrZero(f.getLineTax());
            BigDecimal lineExpense = BigDecimalUtil.valueOrZero(f.getLineExpense());

            BigDecimal totalDiscount = lineDiscount.add(distributedDiscount);
            BigDecimal totalTax = lineTax.add(distributedTax);
            BigDecimal totalExpense = lineExpense.add(distributedExpense);

            f.setTotalDiscount(totalDiscount);
            f.setTotalTax(totalTax);
            f.setTotalExpense(totalExpense);

            // Calculate final net total for the item
            // Net Total = Line Gross - Total Discount + Total Tax + Total Expense
            BigDecimal lineGrossTotal = BigDecimalUtil.valueOrZero(f.getLineGrossTotal());
            BigDecimal finalNetTotal = lineGrossTotal.subtract(totalDiscount).add(totalTax).add(totalExpense);

            f.setNetTotal(finalNetTotal);
            f.setTotalCost(finalNetTotal);

            // Recalculate totalCostRate to include distributed bill-level expenses
            // This ensures that ItemBatch gets the correct cost rate when settled
            BigDecimal totalQtyByUnits = BigDecimalUtil.valueOrZero(f.getTotalQuantityByUnits());
            BigDecimal oldTotalCostRate = f.getTotalCostRate();
            BigDecimal oldCostRate = f.getCostRate();
            BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
            if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
                unitsPerPack = BigDecimal.ONE;
            }

            if (totalQtyByUnits.compareTo(BigDecimal.ZERO) > 0) {
                // Update totalCostRate (per unit)
                BigDecimal updatedTotalCostRate = finalNetTotal.divide(totalQtyByUnits, 6, RoundingMode.HALF_UP);
                f.setTotalCostRate(updatedTotalCostRate);

                // Update costRate (per pack for AMPP, per unit for AMP)
                BigDecimal updatedCostRate = BigDecimalUtil.multiply(updatedTotalCostRate, unitsPerPack);
                f.setCostRate(updatedCostRate);

                // Update valueAtCostRate (totalQtyByUnits × updatedTotalCostRate)
                BigDecimal updatedValueAtCostRate = BigDecimalUtil.multiply(totalQtyByUnits, updatedTotalCostRate);
                f.setValueAtCostRate(updatedValueAtCostRate);

                // Also update PharmaceuticalBillItem with the correct costRate
                if (bi.getPharmaceuticalBillItem() != null) {
                    double oldPbiCostRate = bi.getPharmaceuticalBillItem().getCostRate();
                    bi.getPharmaceuticalBillItem().setCostRate(updatedTotalCostRate.doubleValue());

                    // Update costValue as well (qty × costRate)
                    BigDecimal qtyByUnits = BigDecimalUtil.valueOrZero(f.getQuantityByUnits());
                    BigDecimal updatedCostValue = BigDecimalUtil.multiply(qtyByUnits, updatedTotalCostRate);
                    bi.getPharmaceuticalBillItem().setCostValue(updatedCostValue.doubleValue());


                }
            } else {
                f.setTotalCostRate(BigDecimal.ZERO);
                f.setCostRate(BigDecimal.ZERO);
                f.setValueAtCostRate(BigDecimal.ZERO);
                if (bi.getPharmaceuticalBillItem() != null) {
                    bi.getPharmaceuticalBillItem().setCostRate(0.0);
                    bi.getPharmaceuticalBillItem().setCostValue(0.0);
                }
            }

            // Calculate bill cost (the additional cost from bill-level adjustments)
            BigDecimal lineNetTotal = BigDecimalUtil.valueOrZero(f.getLineNetTotal());
            BigDecimal billCost = finalNetTotal.subtract(lineNetTotal);
            f.setBillCost(billCost);
        }
    }

    /**
     * Recalculates BillFinanceDetails cost aggregates after expense distribution.
     * This ensures bill-level totals reflect the updated cost rates with expenses included.
     */
    private void recalculateBillFinanceDetailsCostAggregates() {
        if (getBill() == null || getBill().getBillFinanceDetails() == null || getBillItems() == null || getBillItems().isEmpty()) {
            return;
        }

        BillFinanceDetails bfd = getBill().getBillFinanceDetails();

        // Recalculate cost aggregates from updated line items
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalCostValueFree = BigDecimal.ZERO;
        BigDecimal totalCostValueNonFree = BigDecimal.ZERO;

        for (BillItem bi : getBillItems()) {
            BillItemFinanceDetails f = (bi != null) ? bi.getBillItemFinanceDetails() : null;
            if (f == null) {
                continue;
            }

            // Sum up the updated valueAtCostRate (which now includes distributed expenses)
            BigDecimal itemCostValue = BigDecimalUtil.valueOrZero(f.getValueAtCostRate());
            totalCostValue = totalCostValue.add(itemCostValue);

            // Calculate free/non-free breakdown
            BigDecimal qtyByUnits = BigDecimalUtil.valueOrZero(f.getQuantityByUnits());
            BigDecimal freeQtyByUnits = BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits());
            BigDecimal costRatePerUnit = BigDecimalUtil.valueOrZero(f.getTotalCostRate());

            BigDecimal costValueNonFree = BigDecimalUtil.multiply(qtyByUnits, costRatePerUnit);
            BigDecimal costValueFree = BigDecimalUtil.multiply(freeQtyByUnits, costRatePerUnit);

            totalCostValueNonFree = totalCostValueNonFree.add(costValueNonFree);
            totalCostValueFree = totalCostValueFree.add(costValueFree);
        }

        // Update BillFinanceDetails with recalculated values
        BigDecimal oldTotalCostValue = bfd.getTotalCostValue();
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalCostValueFree(totalCostValueFree);
        bfd.setTotalCostValueNonFree(totalCostValueNonFree);

    }

    /**
     * Calculate profit margin for purchases based on PharmacyCostingService
     */
    public BigDecimal calculateProfitMarginForPurchasesBigDecimal(BillItem bi) {
        if (bi == null) {
            return BigDecimal.ZERO;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        if (f == null) {
            return BigDecimal.ZERO;
        }

        // Use total cost as specified in comments
        BigDecimal totalCost = f.getTotalCost();
        BigDecimal retailRate = f.getRetailSaleRate();
        BigDecimal qty = f.getQuantity();
        BigDecimal freeQty = f.getFreeQuantity();

        if (totalCost == null || retailRate == null || qty == null || freeQty == null) {
            return BigDecimal.ZERO;
        }

        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Ensure free quantity is properly initialized when left blank
        if (freeQty == null) {
            freeQty = BigDecimal.ZERO;
        }

        // Total Potential Income from qty + free qty multiplied by retail rate
        BigDecimal totalQty = qty.add(freeQty);
        BigDecimal totalPotentialIncome = retailRate.multiply(totalQty);

        return totalPotentialIncome.subtract(totalCost)
                .divide(totalCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public double calculateProfitMarginForPurchases(BillItem bi) {
        return calculateProfitMarginForPurchasesBigDecimal(bi).doubleValue();
    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.PharmacyPurchaseBill);
            bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_PRE);
            bill.setReferenceInstitution(getSessionController().getInstitution());
            boolean consignmentEnabled = configOptionApplicationController.getBooleanValueByKey("Consignment Option is checked in new Pharmacy Purchasing Bills", true);
            bill.setConsignment(consignmentEnabled);
        }
        return bill;
    }

    public void setBill(BilledBill bill) {
        this.bill = bill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    /**
     * Issue #21635 / #13103: whether saving a retail rate below the purchase rate
     * (clearance / loss-leader pricing) is allowed. Policy decision, config-driven
     * rather than a free per-transaction user toggle.
     */
    public boolean isAllowRetailRateBelowPurchaseRate() {
        return configOptionApplicationController.getBooleanValueByKey(
                "Allow Retail Rate Below Purchase Rate in Pharmacy Purchasing", false);
    }

    /**
     * Issue #21837: gates the Wholesale Rate field/columns on this page.
     */
    public boolean isWholesaleTransactionsAllowed() {
        return configOptionApplicationController.getBooleanValueByKey(
                "Allow Wholesale Transactions in Pharmacy Purchasing", false);
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            PharmaceuticalBillItem cuPharmaceuticalBillItem = new PharmaceuticalBillItem();
            currentBillItem.setPharmaceuticalBillItem(cuPharmaceuticalBillItem);
            cuPharmaceuticalBillItem.setBillItem(currentBillItem);
            BillItemFinanceDetails fd = new BillItemFinanceDetails(currentBillItem);
            currentBillItem.setBillItemFinanceDetails(fd);
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public BillItem getEditingBillItem() {
        return editingBillItem;
    }

    public void setEditingBillItem(BillItem editingBillItem) {
        this.editingBillItem = editingBillItem;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public double getSaleRate() {
        return saleRate;
    }

    public void setSaleRate(double saleRate) {
        this.saleRate = saleRate;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillListWithTotals getBillListWithTotals() {
        return billListWithTotals;
    }

    public void setBillListWithTotals(BillListWithTotals billListWithTotals) {
        this.billListWithTotals = billListWithTotals;
    }

    public double getBillItemsTotalQty() {
        return billItemsTotalQty;
    }

    public void setBillItemsTotalQty(double billItemsTotalQty) {
        this.billItemsTotalQty = billItemsTotalQty;
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

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isShowAllBillFormats() {
        return showAllBillFormats;
    }

    public void setShowAllBillFormats(boolean showAllBillFormats) {
        this.showAllBillFormats = showAllBillFormats;
    }

    public String toggleShowAllBillFormats() {
        this.showAllBillFormats = !this.showAllBillFormats;
        return "";
    }

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata(
                "pharmacy/direct_purchase",
                "Pharmacy Direct Purchase",
                "Create and manage direct purchase bills for pharmacy stock",
                "PharmacyDirectPurchaseController"
        );

        // Configuration Options - APPLICATION scope
        metadata.addConfigOption(new ConfigOptionInfo(
                "Allow Wholesale Transactions in Pharmacy Purchasing",
                "Shows the Wholesale Rate field on the Add New Item row and item edit dialog",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Allow Retail Rate Below Purchase Rate in Pharmacy Purchasing",
                "Allows saving a retail rate below the purchase rate (clearance / loss-leader pricing)",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Allow Adding Direct Purchase Items When Normal Quantity Is Zero And Free Quantity Is Present",
                "Allows adding a direct purchase item with zero normal quantity as long as free quantity is entered, "
                + "for fully-free supplier items. When off, quantity must be greater than zero to add an item.",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Use Save Finalize Approve Workflow for Direct Purchase",
                "Switches the page from single-step Settle to a Save Draft / Finalize / Approve workflow",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Enable Consignment in Pharmacy Purchasing",
                "Shows or hides the consignment checkbox option in the purchasing details panel",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Direct Purchase Bill Print - A4",
                "Renders the standard A4 print format for direct purchase bills",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Direct Purchase Bill Print - A4 (Custom 1)",
                "Renders the A4 (Custom 1) print format for direct purchase bills",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Direct Purchase Bill Print - A4 Details",
                "Renders the A4 format with costing details for direct purchase bills",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Direct Purchase Bill Print - Custom 1",
                "Renders custom print format 1 with costing details for direct purchase bills",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Direct Purchase Bill Print - Custom 2",
                "Renders custom print format 2 with costing details for direct purchase bills",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Direct Purchase Bill Print - Custom Letter Format",
                "Renders the custom letter format with costing details for direct purchase bills",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Show Profit % in Direct Purchase Bill",
                "Shows the profit percentage column on direct purchase bill printouts",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Show Retail Value in Direct Purchase Bill",
                "Shows the retail value column on direct purchase bill printouts",
                OptionScope.APPLICATION
        ));

        // Privileges
        metadata.addPrivilege(new PrivilegeInfo(
                "Admin",
                "Administrative access to page configuration",
                "Config button visibility"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PharmacyDirectPurchaseSave",
                "Permission to save a direct purchase draft",
                "Save Draft button visibility"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PharmacyDirectPurchaseFinalize",
                "Permission to finalize a direct purchase draft",
                "Finalize button visibility"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PharmacyDirectPurchaseApprove",
                "Permission to approve a finalized direct purchase draft",
                "Controls access to the Approve Direct Purchase list page"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "ChangeReceiptPrintingPaperTypes",
                "Access to receipt printing configuration settings",
                "Controls visibility of the Settings button in print preview"
        ));

        pageMetadataRegistry.registerPage(metadata);
    }

}
