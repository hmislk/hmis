/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
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
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.PaymentService;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.core.util.BigDecimalUtil;
import com.divudi.bean.pharmacy.PharmacyController;
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
    private boolean printPreview;
    private boolean showAllBillFormats = false;
    private BillItem currentExpense;
    private List<BillItem> billExpenses;
    private String warningMessage;

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToStartNewDirectPurchaseBill() {
        prepareForNewDIrectPurchaseBill();
        return "/pharmacy/direct_purchase?faces-redirect=true";
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Functions">
    public void prepareForNewDIrectPurchaseBill() {
        printPreview = false;
        currentBillItem = null;
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

        if (f == null || pbi == null) {
            JsfUtil.addErrorMessage("Invalid internal structure. Cannot proceed.");
            return;
        }

        if (BigDecimalUtil.isNullOrZero(f.getQuantity()) || BigDecimalUtil.isNegative(f.getQuantity())) {
            JsfUtil.addErrorMessage("Please enter quantity");
            return;
        }

        if (BigDecimalUtil.isNullOrZero(f.getLineGrossRate()) || BigDecimalUtil.isNegative(f.getLineGrossRate())) {
            JsfUtil.addErrorMessage("Please enter the purchase rate");
            return;
        }

        if (BigDecimalUtil.isNullOrZero(f.getRetailSaleRatePerUnit()) || BigDecimalUtil.isNegative(f.getRetailSaleRatePerUnit())) {
            JsfUtil.addErrorMessage("Please enter the sale rate");
            return;
        }

        if (pbi.getDoe() == null) {
            JsfUtil.addErrorMessage("Please set the date of expiry");
            return;
        }

        if (pbi.getDoe() != null) {
            if (pbi.getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                JsfUtil.addErrorMessage("Check Date of Expiry");
                return;
            }
        }
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }

        // Setup basic quantity and rate fields for AMP/AMPP handling
        BigDecimal qty = BigDecimalUtil.valueOrZero(f.getQuantity());
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
            f.setRetailSaleRate(f.getRetailSaleRatePerUnit());
        }

        // Set PharmaceuticalBillItem basic values - calculations will be done by calculateItemTotals()
        pbi.setQty(f.getQuantityByUnits().doubleValue());
        pbi.setFreeQty(f.getFreeQuantityByUnits().doubleValue());

        if (item instanceof Ampp) {
            pbi.setQtyPacks(f.getQuantity().doubleValue());
            pbi.setFreeQtyPacks(f.getFreeQuantity().doubleValue());
            pbi.setPurchaseRatePack(f.getLineGrossRate().doubleValue());
            pbi.setRetailRatePack(BigDecimalUtil.valueOrZero(f.getRetailSaleRate()).doubleValue());
        } else {
            pbi.setQtyPacks(f.getQuantityByUnits().doubleValue());
            pbi.setFreeQtyPacks(f.getFreeQuantityByUnits().doubleValue());
            pbi.setPurchaseRatePack(f.getLineGrossRate().doubleValue());
            pbi.setRetailRatePack(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());
        }

        // Set basic rates - will be recalculated by calculateItemTotals()
        if (item instanceof Ampp) {
            // For AMPP: grossRate is pack price, but purchaseRate should be unit price
            BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
            if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
                unitsPerPack = BigDecimal.ONE; // Avoid division by zero
            }
            BigDecimal unitPurchaseRate = f.getGrossRate().divide(unitsPerPack, 4, RoundingMode.HALF_UP);
            pbi.setPurchaseRate(unitPurchaseRate.doubleValue());
        } else {
            // For AMP: grossRate is already unit price
            pbi.setPurchaseRate(f.getGrossRate().doubleValue());
        }
        pbi.setRetailRate(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());
        pbi.setRetailRateInUnit(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());

        // Set BillItem basic rate fields - calculations will be done by calculateItemTotals()
        getCurrentBillItem().setRate(f.getLineGrossRate().doubleValue());
        getCurrentBillItem().setQty(f.getQuantity().doubleValue());

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
        // Recalculate item totals when quantity changes
        calculateItemTotals(bi);
    }

    public void onFreeQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
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
    private PharmacyController pharmacyController;
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
            BigDecimal packPurchaseRate = BigDecimalUtil.valueOrZero(f.getGrossRate());
            BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());

            if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
                unitsPerPack = BigDecimal.ONE; // Avoid division by zero
            }

            // Convert pack rate to unit rate for profit calculation
            purchaseRatePerUnit = packPurchaseRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP);

            // DEBUG: Log the values to identify AMPP calculation issue
            System.out.println("DEBUG - Item: " + bi.getItem().getName() + " (AMPP)");
            System.out.println("DEBUG - Pack Purchase Rate (stored): " + packPurchaseRate);
            System.out.println("DEBUG - Units Per Pack: " + unitsPerPack);
            System.out.println("DEBUG - Purchase Rate Per Unit (calculated): " + purchaseRatePerUnit);
            System.out.println("DEBUG - Retail Rate Per Unit: " + retailRatePerUnit);
        } else {
            // For AMP items, grossRate is already per unit
            purchaseRatePerUnit = BigDecimalUtil.valueOrZero(f.getGrossRate());

            System.out.println("DEBUG - Item: " + bi.getItem().getName() + " (AMP)");
            System.out.println("DEBUG - Purchase Rate Per Unit (stored): " + purchaseRatePerUnit);
            System.out.println("DEBUG - Retail Rate Per Unit: " + retailRatePerUnit);
        }

        if (purchaseRatePerUnit.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        // Profit Margin = ((Retail Rate - Purchase Rate) / Purchase Rate) * 100
        BigDecimal profit = retailRatePerUnit.subtract(purchaseRatePerUnit);
        BigDecimal margin = profit.divide(purchaseRatePerUnit, 4, RoundingMode.HALF_UP);
        double result = margin.multiply(BigDecimal.valueOf(100)).doubleValue();

        System.out.println("DEBUG - Calculated Profit Margin: " + result + "%");
        System.out.println("DEBUG - =====================================");

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
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
            if (getBill().getBillFinanceDetails() == null) {
                getBill().setBillFinanceDetails(new BillFinanceDetails(getBill()));
            }
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

        getCurrentExpense().setSearialNo(getBillExpenses().size());
        getBillExpenses().add(currentExpense);

        // IMPORTANT: Also add to the Bill entity's expense list
        getBill().getBillExpenses().add(currentExpense);

        // Recalculate expense totals after adding new expense
        recalculateExpenseTotals();
        recalculateProfitMarginsForAllItems();

        // Persist the updated bill
        if (getBill().getId() != null) {
            getBillFacade().edit(getBill());
        }

        currentExpense = null;

    }

    public void removeExpense(BillItem expense) {
        if (expense == null) {
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

        recalculateExpenseTotals();
        recalculateProfitMarginsForAllItems();

        if (getBill().getId() != null) {
            billFacade.edit(getBill());
        }
    }

    public void settleDirectPurchaseBillFinally() {
        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
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
        if (getBill().getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            JsfUtil.addErrorMessage("MultiplePayments Not Allowed.");
            return;
        }

        //Need to Add History
        String msg = errorCheck();
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        saveBill();
        //   saveBillComponent();

//        Payment p = createPayment(getBill());
        billItemsTotalQty = 0;

        for (BillItem i : getBillItems()) {
            double lastPurchaseRate = 0.0;
            lastPurchaseRate = getPharmacyBean().getLastPurchaseRate(i.getItem());

            if (i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty() == 0.0) {
                continue;
            }

            System.out.println("DirectPurchase: Processing item '" + i.getItem().getName() + "' (Type: " + i.getItem().getClass().getSimpleName() + ")");

            billItemsTotalQty = billItemsTotalQty + i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty();

            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            // CRITICAL: Calculate all item totals before final persistence
            calculateItemTotals(i);

            // Save updated BillItem and PharmaceuticalBillItem with calculated values
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

        // CRITICAL: Calculate bill-level totals and update BillFinanceDetails after all items are processed
        calculateBillTotalsFromItems();

        // Distribute bill-level adjustments proportionally to items if needed
        if (getBill().getDiscount() != 0.0 || getBill().getTax() != 0.0 || getBill().getExpensesTotalConsideredForCosting() != 0.0) {
            distributeProportionalBillValuesToItems();

            for (BillItem item : getBillItems()) {
                getBillItemFacade().edit(item);
            }
        }

        //check and calculate expenses separately
        if (billExpenses != null && !billExpenses.isEmpty()) {
            getBill().setBillExpenses(billExpenses);

            double totalForExpenses = 0;
            for (BillItem expense : getBillExpenses()) {
                totalForExpenses += expense.getNetValue();
            }

            getBill().setExpenseTotal(-Math.abs(totalForExpenses));
            // Note: NetTotal is already correctly calculated by the service and includes expenses
            // Removed: getBill().setNetTotal(getBill().getNetTotal() + totalForExpenses);
        }

//        getPharmacyBillBean().calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
        getBillFacade().edit(getBill());
        List<Payment> ps = paymentService.createPayment(getBill(), getPaymentMethodData());

        JsfUtil.addSuccessMessage("Direct Purchase Successfully Completed.");
        printPreview = true;
    }

    public void removeItem(BillItem bi) {
        getBillItems().remove(bi);

        int i = 0;
        for (BillItem it : getBillItems()) {
            it.setSearialNo(i++);
        }

        calculateBillTotalsFromItems();
        currentBillItem = null;
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

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);

        getBill().setDeptId(deptId);
        getBill().setInsId(deptId);
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
            f.setLineNetRate(BigDecimal.ZERO);
        }

        // Calculate cost value (line cost = net total for purchases)
        f.setLineCost(itemNet);

        // Ensure unit-based calculations are updated for UI display
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
        // 1. Set gross total (main field, not just lineGrossTotal)
        f.setGrossTotal(itemGross);

        // 2. Calculate and set gross rate (grossTotal divided by quantity)
        if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
            f.setGrossRate(itemGross.divide(qty, 4, RoundingMode.HALF_UP));
        } else {
            f.setGrossRate(BigDecimal.ZERO);
        }

        // 3. Calculate and set line cost rate per unit
        if (totalQtyByUnits != null && totalQtyByUnits.compareTo(BigDecimal.ZERO) > 0) {
            f.setLineCostRate(itemNet.divide(totalQtyByUnits, 4, RoundingMode.HALF_UP));
        } else {
            f.setLineCostRate(BigDecimal.ZERO);
        }

        // 4. Calculate value fields for all rate types using total quantity by units
        BigDecimal totalUnits = BigDecimalUtil.valueOrZero(f.getTotalQuantityByUnits());
        if (totalUnits.compareTo(BigDecimal.ZERO) > 0) {
            // Value at retail rate
            f.setValueAtRetailRate(BigDecimalUtil.multiply(totalUnits,
                    BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit())));

            // Value at purchase rate (using gross rate)
            f.setValueAtPurchaseRate(BigDecimalUtil.multiply(totalUnits,
                    BigDecimalUtil.valueOrZero(f.getGrossRate())));

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
            f.setValueAtPurchaseRate(BigDecimal.ZERO);
            f.setValueAtCostRate(BigDecimal.ZERO);
            f.setValueAtWholesaleRate(BigDecimal.ZERO);
        }

        // Update BillItem values with safe null handling
        billItem.setGrossValue(itemGross != null ? itemGross.doubleValue() : 0.0);
        billItem.setNetValue(itemNet != null ? itemNet.doubleValue() : 0.0);
        billItem.setRate(purchaseRate != null ? purchaseRate.doubleValue() : 0.0);
        billItem.setNetRate(f.getLineNetRate() != null ? f.getLineNetRate().doubleValue() : 0.0);

        // Update PharmaceuticalBillItem with calculated values in units
        PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();
        // pbi will never be null as it is created in the getter
        BigDecimal grossRatePerUnit = BigDecimalUtil.valueOrZero(f.getGrossRate());
        BigDecimal retailRatePerUnit = BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit());

        // Update quantities in units (important for stock calculations)
        pbi.setQty(qtyByUnits.doubleValue()); // Paid quantity in units
        pbi.setFreeQty(freeQtyByUnits.doubleValue()); // Free quantity in units

        // Update rates per unit
        pbi.setPurchaseRate(grossRatePerUnit.doubleValue()); // Purchase rate per unit
        pbi.setRetailRate(retailRatePerUnit.doubleValue()); // Retail rate per unit

        // Calculate values (quantity × rate)
        BigDecimal pbiPurchaseValue = BigDecimalUtil.multiply(qtyByUnits, grossRatePerUnit);
        BigDecimal pbiRetailValue = BigDecimalUtil.multiply(qtyByUnits, retailRatePerUnit);

        pbi.setPurchaseValue(pbiPurchaseValue.doubleValue());
        pbi.setRetailValue(pbiRetailValue.doubleValue());

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
            BigDecimal grossPerUnit = BigDecimalUtil.valueOrZero(f.getGrossRate());
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
                f.setValueAtPurchaseRate(totalUnits.multiply(grossPerUnit));
            }
            if (f.getValueAtCostRate() == null) {
                f.setValueAtCostRate(totalUnits.multiply(costPerUnit));
            }

            // Compute free/non-free breakdowns per item
            purchaseValueNonFree = purchaseValueNonFree.add(grossPerUnit.multiply(qtyUnits));
            purchaseValueFree = purchaseValueFree.add(grossPerUnit.multiply(freeUnits));
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
        getBill().setTotal(-netTotalLines.doubleValue());
        getBill().setNetTotal(-finalNet.doubleValue());
        getBill().setSaleValue(totalRetail.doubleValue());

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

            // Calculate bill cost (the additional cost from bill-level adjustments)
            BigDecimal lineNetTotal = BigDecimalUtil.valueOrZero(f.getLineNetTotal());
            BigDecimal billCost = finalNetTotal.subtract(lineNetTotal);
            f.setBillCost(billCost);
        }
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
            bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
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

}
