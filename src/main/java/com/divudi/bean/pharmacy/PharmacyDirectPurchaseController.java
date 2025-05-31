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
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BillNumber;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
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

        if (f.getQuantity() == null || f.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            JsfUtil.addErrorMessage("Please enter quantity");
            return;
        }

        if (f.getLineGrossRate() == null || f.getLineGrossRate().compareTo(BigDecimal.ZERO) <= 0) {
            JsfUtil.addErrorMessage("Please enter the purchase rate");
            return;
        }

        if (f.getRetailSaleRatePerUnit() == null || f.getRetailSaleRatePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
            JsfUtil.addErrorMessage("Please enter the sale rate");
            return;
        }

        if (pbi.getDoe() == null) {
            JsfUtil.addErrorMessage("Please set the date of expiry");
            return;
        }

        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
        }

        recalculateFinancialsBeforeAddingBillItem(f);

        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;

        if (item instanceof Ampp) {
            pbi.setQty(f.getQuantityByUnits().doubleValue());
            pbi.setQtyPacks(f.getQuantity().doubleValue());

            pbi.setFreeQty(f.getFreeQuantityByUnits().doubleValue());
            pbi.setFreeQtyPacks(f.getFreeQuantity().doubleValue());

            pbi.setPurchaseRate(f.getNetRate().divide(f.getUnitsPerPack(), 4, RoundingMode.HALF_UP).doubleValue());
            pbi.setPurchaseRatePack(f.getNetRate().doubleValue());

            pbi.setRetailRate(f.getRetailSaleRatePerUnit().doubleValue());
            pbi.setRetailRatePack(f.getRetailSaleRate().doubleValue());
            pbi.setRetailRateInUnit(f.getRetailSaleRatePerUnit().doubleValue());

        } else {
            // AMP: no packs; assign both units and packs as same
            pbi.setQty(f.getQuantityByUnits().doubleValue());
            pbi.setQtyPacks(f.getQuantityByUnits().doubleValue());

            pbi.setFreeQty(f.getFreeQuantityByUnits().doubleValue());
            pbi.setFreeQtyPacks(f.getFreeQuantityByUnits().doubleValue());

            pbi.setPurchaseRate(f.getNetRate().doubleValue());
            pbi.setPurchaseRatePack(f.getNetRate().doubleValue());

            // User Records f.getRetailSaleRatePerUnit(). It will be same same for all retail rates
            f.setRetailSaleRate(f.getRetailSaleRatePerUnit()); // User gives 

            pbi.setRetailRate(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());
            pbi.setRetailRatePack(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());
            pbi.setRetailRateInUnit(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());

        }

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getBillItems().add(currentBillItem);

        currentBillItem = null;
        distributeProportionalBillValuesToItems(getBillItems(), getBill());
        calculateBillTotalsFromItems();
//        calulateTotalsWhenAddingItemsOldMethod();
    }

// ChatGPT contributed - Recalculates line-level financial values before adding BillItem to bill
    private void recalculateFinancialsBeforeAddingBillItem(BillItemFinanceDetails f) {
        // 1. Retrieve user-entered input values, ensuring non-null defaults
        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;                   // actual quantity
        BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;       // free quantity
        BigDecimal lineGrossRate = f.getLineGrossRate() != null ? f.getLineGrossRate() : BigDecimal.ZERO; // rate per item
        BigDecimal lineDiscountRate = f.getLineDiscountRate() != null ? f.getLineDiscountRate() : BigDecimal.ZERO; // discount per unit
        BigDecimal retailRate = f.getRetailSaleRatePerUnit() != null ? f.getRetailSaleRatePerUnit() : BigDecimal.ZERO; // retail sale rate

        Item item = f.getBillItem().getItem();

        // 2. Calculate total quantity
        BigDecimal totalQty = qty.add(freeQty); // total quantity in packs

        // 3. Declare variables for unit-based calculations
        BigDecimal unitsPerPack = null;         // units per pack
        BigDecimal totalQtyInUnits = null;      // total quantity in units
        BigDecimal qtyInUnits = null;           // quantity in units
        BigDecimal freeQtyInUnits = null;       // free quantity in units

        // 4. Handle conversion to units if item is Ampp (e.g., 1 pack = 10 tablets)
        if (item instanceof Ampp) {
            double dblVal = item.getDblValue(); // e.g., 10 units per pack
            if (dblVal > 0.0) {
                unitsPerPack = BigDecimal.valueOf(dblVal);
            } else {
                unitsPerPack = BigDecimal.ONE; // fallback if unitsPerPack not defined
            }
            qtyInUnits = qty.multiply(unitsPerPack);
            freeQtyInUnits = freeQty.multiply(unitsPerPack);
            totalQtyInUnits = totalQty.multiply(unitsPerPack);
        } else {
            // If not Ampp, assume quantity is already in units
            unitsPerPack = BigDecimal.ONE;
            qtyInUnits = qty;
            freeQtyInUnits = freeQty;
            totalQtyInUnits = totalQty;
        }

        f.setUnitsPerPack(unitsPerPack);

        // 5. Record unit-based quantities in finance details
        f.setQuantityByUnits(qtyInUnits);
        f.setFreeQuantityByUnits(freeQtyInUnits);
        f.setTotalQuantityByUnits(totalQtyInUnits);

        // 6. Financial Calculations (all line-level)
        BigDecimal lineGrossTotal = lineGrossRate.multiply(qty);               // gross = rate × quantity (only actual qty)
        BigDecimal lineDiscountValue = lineDiscountRate.multiply(qty);         // discount = rate × quantity (only actual qty)
        BigDecimal lineNetTotal = lineGrossTotal.subtract(lineDiscountValue);  // net = gross - discount

        // 7. Cost rate based on quantity in units
        BigDecimal lineCostRate = totalQtyInUnits.compareTo(BigDecimal.ZERO) > 0
                ? lineNetTotal.divide(totalQtyInUnits, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 8. Retail value = retail rate × total quantity in units
        BigDecimal retailValue = retailRate.multiply(totalQtyInUnits);

        // 9. Assign computed values to line-level fields only (no bill-level values touched)
        f.setLineGrossRate(lineGrossRate);  // retained
        f.setLineNetRate(
                totalQty.compareTo(BigDecimal.ZERO) > 0
                ? lineNetTotal.divide(totalQty, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        ); // average net rate per pack

        f.setRetailSaleRate(retailRate.multiply(unitsPerPack));
        f.setLineDiscount(lineDiscountValue);     // calculated
        f.setLineGrossTotal(lineGrossTotal);      // calculated
        f.setLineNetTotal(lineNetTotal);          // calculated
        f.setLineCost(lineNetTotal);              // cost = net total
        f.setLineCostRate(lineCostRate);          // cost per unit
        f.setTotalQuantity(totalQty);             // pack-based total (with free)
        // 10. Comments:
        // - No bill-level values (billDiscount, billCost, etc.) are changed here
        // - Only line-specific fields are calculated based on user inputs
        // - Units-per-pack logic is safely handled with fallback
    }

// ChatGPT contributed - Distributes user-entered bill-level values proportionally to items and calculates derived totals and rates
    public void distributeProportionalBillValuesToItems(List<BillItem> billItems, Bill bill) {
        if (bill == null || bill.getBillFinanceDetails() == null || billItems == null || billItems.isEmpty()) {
            return;
        }

        // Step 1: Calculate proportional basis per item (based on quantity + freeQuantity)
        BigDecimal totalBasis = BigDecimal.ZERO;
        Map<BillItem, BigDecimal> itemBases = new HashMap<>();

        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }

            BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);             // user-entered
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);     // user-entered
            BigDecimal lineGrossRate = Optional.ofNullable(f.getLineGrossRate()).orElse(BigDecimal.ZERO); // user-entered

            BigDecimal basis = lineGrossRate.multiply(qty.add(freeQty)); // calculated
            itemBases.put(bi, basis);
            totalBasis = totalBasis.add(basis);
        }

        if (totalBasis.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // Step 2: Convert and assign user-entered bill-level values to finance details
        bill.getBillFinanceDetails().setBillDiscount(BigDecimal.valueOf(bill.getDiscount()));
        bill.getBillFinanceDetails().setBillTaxValue(BigDecimal.valueOf(bill.getTax()));
        bill.getBillFinanceDetails().setBillExpense(BigDecimal.valueOf(bill.getExpenseTotal()));

        BigDecimal billDiscountTotal = Optional.ofNullable(bill.getBillFinanceDetails().getBillDiscount()).orElse(BigDecimal.ZERO);
        BigDecimal billExpenseTotal = Optional.ofNullable(bill.getBillFinanceDetails().getBillExpense()).orElse(BigDecimal.ZERO);
        BigDecimal billTaxTotal = Optional.ofNullable(bill.getBillFinanceDetails().getBillTaxValue()).orElse(BigDecimal.ZERO);

        // Step 3: Distribute and calculate item-level derived values
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }

            BigDecimal basis = itemBases.get(bi);
            BigDecimal ratio = basis.divide(totalBasis, 12, RoundingMode.HALF_UP); // calculated

            // User-entered line values
            BigDecimal lineDiscount = Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO);
            BigDecimal lineExpense = Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO);
            BigDecimal lineTax = Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO);
            BigDecimal lineNetTotal = Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO);
            BigDecimal lineGrossTotal = Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO);
            BigDecimal lineGrossRate = Optional.ofNullable(f.getLineGrossRate()).orElse(BigDecimal.ZERO);

            // Distributed bill-level values
            BigDecimal billDiscount = billDiscountTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billExpense = billExpenseTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billTax = billTaxTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            f.setBillDiscount(billDiscount);
            f.setBillExpense(billExpense);
            f.setBillTax(billTax);

            // Totals = line + bill
            BigDecimal totalDiscount = lineDiscount.add(billDiscount);
            BigDecimal totalExpense = lineExpense.add(billExpense);
            BigDecimal totalTax = lineTax.add(billTax);

            f.setTotalDiscount(totalDiscount);
            f.setTotalExpense(totalExpense);
            f.setTotalTax(totalTax);

            // Quantities
            BigDecimal quantity = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);         // used for rates
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);      // used for cost
            BigDecimal totalQty = quantity.add(freeQty);                                                 // full total
            f.setTotalQuantity(totalQty); // recorded

            // Rates using only quantity (excluding free)
            BigDecimal billDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? billDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillDiscountRate(billDiscountRate);

            BigDecimal totalDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setTotalDiscountRate(totalDiscountRate);

            // Net and cost values
            BigDecimal netTotal = lineGrossTotal.subtract(totalDiscount).add(totalTax).add(totalExpense);
            f.setNetTotal(netTotal);
            f.setTotalCost(netTotal);

            BigDecimal billCost = netTotal.subtract(lineNetTotal);
            f.setBillCost(billCost);

            // Cost-related rates use totalQty
            BigDecimal lineCostRate = totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? lineNetTotal.divide(totalQty, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal billCostRate = billCost.compareTo(BigDecimal.ZERO) > 0 && totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(totalQty, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal totalCostRate = totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(totalQty, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            f.setLineCostRate(lineCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setBillCostRate(billCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setTotalCostRate(totalCostRate.setScale(4, RoundingMode.HALF_UP));

            // Reapply or reset gross and net rates
            f.setLineGrossRate(lineGrossRate);       // retained
            f.setBillGrossRate(BigDecimal.ZERO);     // reset
            f.setGrossRate(lineGrossRate);           // derived

            f.setLineGrossTotal(lineGrossTotal);     // retained
            f.setBillGrossTotal(BigDecimal.ZERO);    // reset
            f.setGrossTotal(lineGrossTotal);         // sum of line + bill

            if (f.getLineNetRate() == null || f.getLineNetRate().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal lineNetRate = totalQty.compareTo(BigDecimal.ZERO) > 0
                        ? lineNetTotal.divide(totalQty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                f.setLineNetRate(lineNetRate);
            }

            f.setBillNetRate(BigDecimal.ZERO);       // reset
            f.setNetRate(f.getLineNetRate());        // final assigned
        }
    }

    public void onQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }
        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        recalculateFinancialsBeforeAddingBillItem(f);
    }

    public void onFreeQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        recalculateFinancialsBeforeAddingBillItem(f);
    }

    public void onLineGrossRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        recalculateFinancialsBeforeAddingBillItem(f); // Rate drives discount value

    }

    public void onLineDiscountRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        recalculateFinancialsBeforeAddingBillItem(f);
    }

    public void onRetailSaleRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        recalculateFinancialsBeforeAddingBillItem(f);

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
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillEjb billEjb;
    @EJB
    PaymentService paymentService;

    /**
     * Controllers
     */
    @Inject
    private SessionController sessionController;
    @Inject
    PharmacyCalculation pharmacyBillBean;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
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

    public void onItemSelect(SelectEvent event) {
        if (getCurrentBillItem().getItem() == null) {
            return;
        }
        Item item = getCurrentBillItem().getItem();
        double pr = getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment());
        double rr = getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment());
        // Keep these for backword compatibility - Start
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pr);
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(rr);
        // Keep these for backword compatibility - End        

        if (item instanceof Ampp) {
            getCurrentBillItem().getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.valueOf(item.getDblValue()));
            getCurrentBillItem().getBillItemFinanceDetails().setLineGrossRate(BigDecimal.valueOf(pr).multiply(getCurrentBillItem().getBillItemFinanceDetails().getUnitsPerPack()));
            getCurrentBillItem().getBillItemFinanceDetails().setRetailSaleRatePerUnit(BigDecimal.valueOf(rr));
        } else {
            getCurrentBillItem().getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.ONE);
            getCurrentBillItem().getBillItemFinanceDetails().setLineGrossRate(BigDecimal.valueOf(pr));
            getCurrentBillItem().getBillItemFinanceDetails().setRetailSaleRatePerUnit(BigDecimal.valueOf(rr));

        }

        recalculateFinancialsBeforeAddingBillItem(getCurrentBillItem().getBillItemFinanceDetails());
    }

    public void createGrnAndPurchaseBillsWithCancellsAndReturnsOfSingleDepartment() {
        Date startTime = new Date();

        BillType[] bts = new BillType[]{BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.PharmacyGrnReturn, BillType.PurchaseReturn,};
        Class[] bcs = new Class[]{BilledBill.class, CancelledBill.class, RefundBill.class};
        billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, bcs, department, null, null);

    }

    public void createOnlyPurchaseBillsWithCancellsAndReturnsOfSingleDepartment() {
        Date startTime = new Date();

        BillType[] bts = new BillType[]{BillType.PharmacyPurchaseBill, BillType.PurchaseReturn,};
        Class[] bcs = new Class[]{BilledBill.class, CancelledBill.class, RefundBill.class};
        billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, bcs, department, null, null);

    }

    public void createOnlyGrnBillsWithCancellsAndReturnsOfSingleDepartment() {
        Date startTime = new Date();

        BillType[] bts = new BillType[]{BillType.PharmacyGrnBill, BillType.PurchaseReturn,};
        Class[] bcs = new Class[]{BilledBill.class, CancelledBill.class, RefundBill.class};
        billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, bcs, department, null, null);

    }

    public void fillItemVicePurchaseAndGoodReceive() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql;
        BillItem bi = new BillItem();
        List<BillType> bts = new ArrayList<>();

        bts.add(BillType.PharmacyGrnBill);
        bts.add(BillType.PharmacyGrnReturn);
        bts.add(BillType.PharmacyPurchaseBill);

        sql = "select new com.divudi.core.data.dataStructure.PharmacyStockRow"
                + " (bi.item.name, "
                + " sum(bi.qty), "
                + " sum(bi.pharmaceuticalBillItem.freeQty)) "
                + " from BillItem bi "
                + " where bi.bill.billType in :bts "
                + " and bi.bill.createdAt between :fd and :td ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", bts);

        if (department != null) {
            sql = sql + " and bi.bill.department=:dept ";
            m.put("dept", department);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.institution=:ins ";
            m.put("ins", institution);
        }

        sql = sql + "group by bi.item "
                + "order by bi.item.name";

        List<PharmacyStockRow> lsts = (List) billFacade.findObjects(sql, m, TemporalType.TIMESTAMP);

        rows = lsts;

    }

    public void calculatePurchaseRateAndWholesaleRateFromRetailRate() {
        if (currentBillItem == null || currentBillItem.getPharmaceuticalBillItem() == null || currentBillItem.getPharmaceuticalBillItem().getRetailRate() == 0) {
            return;
        }
        currentBillItem.getPharmaceuticalBillItem().setPurchaseRate(currentBillItem.getPharmaceuticalBillItem().getRetailRate() / 1.15);
        currentBillItem.getPharmaceuticalBillItem().setWholesaleRate(currentBillItem.getPharmaceuticalBillItem().getPurchaseRate() * 1.08);
    }

// ChatGPT contributed - Calculates true profit margin (%) based on unit sale and cost rates
    // ChatGPT contributed - Calculates profit margin (%) correctly based on item type (Amp or Ampp)
    public double calcProfitMargin(BillItem ph) {
        if (ph == null || ph.getBillItemFinanceDetails() == null) {
            return 0.0;
        }

        Item item = ph.getItem();
        BillItemFinanceDetails f = ph.getBillItemFinanceDetails();

        BigDecimal qtyInUnits = Optional.ofNullable(f.getTotalQuantityByUnits()).orElse(BigDecimal.ZERO);
        if (qtyInUnits.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        // Retail rate is always per unit
        BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO);

        // Determine correct cost rate
        BigDecimal costRatePerUnit;
        if (item instanceof Ampp) {
            double unitsPerPackDouble = item.getDblValue();
            BigDecimal unitsPerPack = unitsPerPackDouble > 0 ? BigDecimal.valueOf(unitsPerPackDouble) : BigDecimal.ONE;

            // Line cost rate in this case is cost per pack, so convert to per-unit
            BigDecimal packCostRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
            costRatePerUnit = packCostRate.divide(unitsPerPack, 6, RoundingMode.HALF_UP);
        } else {
            // For Amp or unit-based items, cost rate is already per unit
            costRatePerUnit = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
        }

        // Calculate totals
        BigDecimal retailTotal = retailRate.multiply(qtyInUnits);
        BigDecimal costTotal = costRatePerUnit.multiply(qtyInUnits);

        if (retailTotal.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        // Margin = (retail − cost) / retail × 100
        return retailTotal.subtract(costTotal)
                .divide(retailTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public boolean isProfitMarginExcessive(BillItem ph) {
        if (ph == null || ph.getItem() == null || ph.getItem().getCategory() == null) {
            return false;
        }
        double margin = calcProfitMargin(ph);
        return ph.getItem().getCategory().getProfitMargin() > margin;
    }

    public List<PharmacyStockRow> getRows() {
        return rows;
    }

    public void setRows(List<PharmacyStockRow> rows) {
        this.rows = rows;
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

    public void remove(BillItem b) {
        getBillItems().remove(b);
        int i = 0;
        for (BillItem bi : getBillItems()) {
            bi.setSearialNo(i++);
        }
    }

    public PharmacyCalculation getPharmacyBillBean() {
        return pharmacyBillBean;
    }

    public void setPharmacyBillBean(PharmacyCalculation pharmacyBillBean) {
        this.pharmacyBillBean = pharmacyBillBean;
    }

    public PharmacyDirectPurchaseController() {
    }

    public void onEditPurchaseRate(BillItem tmp) {

        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        tmp.getPharmaceuticalBillItem().setRetailRate(retail);

        onEdit(tmp);
    }

    public void onEditPurchaseRate() {

        double retail = getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() + (getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(retail);

        onEdit(getCurrentBillItem());
    }

    public void onEdit(BillItem tmp) {

        if (tmp.getPharmaceuticalBillItem().getPurchaseRate() > tmp.getPharmaceuticalBillItem().getRetailRate()) {
            tmp.getPharmaceuticalBillItem().setRetailRate(0);
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }

        if (tmp.getPharmaceuticalBillItem().getDoe() != null) {
            if (tmp.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                tmp.getPharmaceuticalBillItem().setDoe(null);
                JsfUtil.addErrorMessage("Check Date of Expiry");
                //    return;
            }
        }

        wsRate = (tmp.getPharmaceuticalBillItem().getPurchaseRate() * 1.08) * (tmp.getTmpQty()) / (tmp.getTmpQty() + tmp.getPharmaceuticalBillItem().getFreeQty());
        wsRate = CommonFunctions.round(wsRate);
        tmp.getPharmaceuticalBillItem().setWholesaleRate(wsRate);
        calTotal();
    }

    public void setBatch(BillItem pid) {
        if (pid.getPharmaceuticalBillItem().getStringValue().trim().isEmpty()) {
            Date date = pid.getPharmaceuticalBillItem().getDoe();
            DateFormat df = new SimpleDateFormat("ddMMyyyy");
            String reportDate = df.format(date);
// Print what date is today!
            //       //System.err.println("Report Date: " + reportDate);
            pid.getPharmaceuticalBillItem().setStringValue(reportDate);
        }
        onEdit(pid);
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

    public void calSaleRte() {
        saleRate = 0.0;
        double categoryMarginPercentage = 0;
        if (getCurrentBillItem() == null || getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Bill Item is Null");
        } else {

            Object item = getCurrentBillItem().getItem();

            if (item instanceof Ampp) {
                Ampp tmpAmpp = (Ampp) item;
                if (tmpAmpp.getAmp() != null
                        && tmpAmpp.getCategory() != null
                        && tmpAmpp.getCategory().getSaleMargin() != null) {
                    categoryMarginPercentage = tmpAmpp.getCategory().getSaleMargin() + 100;
                }
            } else if (item instanceof Amp) {
                Amp tmpAmp = (Amp) item;
                if (tmpAmp.getCategory() != null
                        && tmpAmp.getCategory().getSaleMargin() != null) {
                    categoryMarginPercentage = tmpAmp.getCategory().getSaleMargin() + 100;
                }
            }
        }

        double tmpPurchaseRate;
        if (getCurrentBillItem().getItem() instanceof Ampp) {
            saleRate = (categoryMarginPercentage * getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRatePack() / getCurrentBillItem().getItem().getDblValue()) / 100;
            tmpPurchaseRate = getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRatePack() / getCurrentBillItem().getItem().getDblValue();
            getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(tmpPurchaseRate);
        } else if (getCurrentBillItem().getItem() instanceof Amp) {
            saleRate = (categoryMarginPercentage * getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate()) / 100;
        }

        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(saleRate);

        categoryMarginPercentage = 108;
        wsRate = (categoryMarginPercentage * getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate()) / 100;
        if (getCurrentBillItem().getTmpQty() + getCurrentBillItem().getPharmaceuticalBillItem().getFreeQty() != 0) {
            wsRate = wsRate * getCurrentBillItem().getTmpQty() / (getCurrentBillItem().getTmpQty() + getCurrentBillItem().getPharmaceuticalBillItem().getFreeQty());
        }
        wsRate = CommonFunctions.round(wsRate);
        getCurrentBillItem().getPharmaceuticalBillItem().setWholesaleRate(wsRate);

    }

    public void calNetTotal() {
        double grossTotal;
        if (getBill().getDiscount() > 0 || getBill().getTax() > 0) {
            grossTotal = getBill().getTotal() + getBill().getDiscount() - getBill().getTax();
            ////// // System.out.println("gross" + grossTotal);
            ////// // System.out.println("net1" + getBill().getNetTotal());
            getBill().setNetTotal(grossTotal);
            ////// // System.out.println("net2" + getBill().getNetTotal());
        }
        distributeProportionalBillValuesToItems(billItems, bill);
    }

    public void billDiscountChangedByUser() {
        distributeProportionalBillValuesToItems(getBillItems(), getBill());
        calculateBillTotalsFromItems();
    }

    public void billTaxChangedByUser() {
        distributeProportionalBillValuesToItems(getBillItems(), getBill());
        calculateBillTotalsFromItems();
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
        currentExpense = null;

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
        List<Payment> ps = paymentService.createPayment(getBill(), getPaymentMethodData());

        billItemsTotalQty = 0;

        for (BillItem i : getBillItems()) {

            System.out.println("\n--- Processing BillItem ---");
            System.out.println("BillItem ID: " + i.getId());
            System.out.println("Item: " + (i.getItem() != null ? i.getItem().getName() : "NULL"));
            System.out.println("Item Class: " + (i.getItem() != null ? i.getItem().getClass().getSimpleName() : "NULL"));
            System.out.println("Qty: " + i.getPharmaceuticalBillItem().getQty());
            System.out.println("Free Qty: " + i.getPharmaceuticalBillItem().getFreeQty());

            double lastPurchaseRate = 0.0;
            lastPurchaseRate = getPharmacyBean().getLastPurchaseRate(i.getItem());

            if (i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty() == 0.0) {
                System.out.println("Skipping item due to zero total quantity.");
                continue;
            }

            billItemsTotalQty = billItemsTotalQty + i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty();

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());

            if (i.getId() == null) {
                getBillItemFacade().create(i);
                System.out.println("Created BillItem");
            } else {
                getBillItemFacade().edit(i);
                System.out.println("Edited BillItem");
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
                System.out.println("Created PharmaceuticalBillItem");
            } else {
                getPharmaceuticalBillItemFacade().edit(tmpPh);
                System.out.println("Edited PharmaceuticalBillItem");
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            saveBillFee(i);

            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatchWithCosting(i);
            System.out.println("ItemBatch ID: " + (itemBatch != null ? itemBatch.getId() : "NULL"));

            double addingQty = i.getBillItemFinanceDetails().getTotalQuantityByUnits().doubleValue();
            System.out.println("Adding to stock. Quantity (in units): " + addingQty);

            tmpPh.setItemBatch(itemBatch);

            Stock stock = getPharmacyBean().addToStock(tmpPh, Math.abs(addingQty), getSessionController().getDepartment());
            System.out.println("Stock object returned: " + (stock != null ? "NOT NULL" : "NULL"));

            tmpPh.setLastPurchaseRate(lastPurchaseRate);
            tmpPh.setStock(stock);

            getPharmaceuticalBillItemFacade().edit(tmpPh);

            getBill().getBillItems().add(i);
        }

        //check and calculate expenses separately
        if (billExpenses != null && !billExpenses.isEmpty()) {
            getBill().setBillExpenses(billExpenses);

            double totalForExpenses = 0;
            for (BillItem expense : getBillExpenses()) {
                totalForExpenses += expense.getNetValue();
            }

            getBill().setExpenseTotal(-Math.abs(totalForExpenses));
            getBill().setNetTotal(getBill().getNetTotal() + totalForExpenses);
        }

        getPharmacyBillBean().calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
        getBillFacade().edit(getBill());

        JsfUtil.addSuccessMessage("Direct Purchase Successfully Completed.");
        printPreview = true;
    }

    public void removeItem(BillItem bi) {
        getBillItems().remove(bi);

        int i = 0;
        for (BillItem it : getBillItems()) {
            it.setSearialNo(i++);
        }

        calTotal();
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
            getPaymentFacade().create(p);
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

    @Deprecated
    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }
//
//    public void recreate() {
//
////        cashPaid = 0.0;
//        currentPharmacyItemData = null;
//        pharmacyItemDatas = null;
//    }

    public void addItemWithLastRate() {
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select and item from the list");
            return;
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRateInUnit(getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRateInUnit(getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));

        getBillItems().add(getCurrentBillItem());

        calTotal();

        currentBillItem = null;
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

    public BillItem getBillItem(Item i) {
        BillItem tmp = new BillItem();
        tmp.setBill(getBill());
        tmp.setItem(i);

        //   getBillItemFacade().create(tmp);
        return tmp;
    }

    public PharmaceuticalBillItem getPharmacyBillItem(BillItem b) {
        PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
        tmp.setBillItem(b);
        //   tmp.setQty(getPharmacyBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        //     tmp.setPurchaseRate(getPharmacyBean().getPurchaseRate(b.getItem(), getSessionController().getDepartment()));
        tmp.setRetailRate(getPharmacyBillBean().calRetailRate(tmp));
//        if (b.getId() == null || b.getId() == 0) {
//            getPharmaceuticalBillItemFacade().create(tmp);
//        } else {
//            getPharmaceuticalBillItemFacade().edit(tmp);
//        }
        return tmp;
    }

    public double getNetTotal() {

        double tmp = getBill().getTotal() + getBill().getTax() - getBill().getDiscount();
        getBill().setNetTotal(0 - tmp);

        return tmp;
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

    public void calculateBillTotalsFromItems() {
        double tot = 0.0;
        double saleValue = 0.0;
        int serialNo = 0;

        // Bill-level inputs: do not calculate here
        BigDecimal billDiscount = BigDecimal.ZERO;
        BigDecimal billExpense = BigDecimal.ZERO;
        BigDecimal billTax = BigDecimal.ZERO;
        BigDecimal billCost = BigDecimal.ZERO;

        // Totals from bill items
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalLineExpenses = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalCostLines = BigDecimal.ZERO;

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalTaxLines = BigDecimal.ZERO;

        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        for (BillItem bi : getBillItems()) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

            if (bi.getItem() instanceof Ampp) {
                bi.setQty(pbi.getQtyPacks());
                bi.setRate(pbi.getPurchaseRatePack());
            } else if (bi.getItem() instanceof Amp) {
                bi.setQty(pbi.getQty());
                bi.setRate(pbi.getPurchaseRate());
            }

            bi.setSearialNo(serialNo++);
            double netValue = bi.getQty() * bi.getRate();
            bi.setNetValue(0 - netValue);
            tot += bi.getNetValue();

            saleValue += (pbi.getQty() + pbi.getFreeQty()) * pbi.getRetailRate();

            if (f != null) {
                BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal costRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
                BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO);
                BigDecimal wholesaleRate = Optional.ofNullable(f.getWholesaleRate()).orElse(BigDecimal.ZERO);

                BigDecimal qtyTotal = qty.add(freeQty);
                BigDecimal retailValue = retailRate.multiply(qtyTotal);
                BigDecimal wholesaleValue = wholesaleRate.multiply(qtyTotal);
                BigDecimal freeItemValue = costRate.multiply(freeQty);

                totalLineDiscounts = totalLineDiscounts.add(Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO));
                totalLineExpenses = totalLineExpenses.add(Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO));
                totalTaxLines = totalTaxLines.add(Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO));
                totalCostLines = totalCostLines.add(Optional.ofNullable(f.getLineCost()).orElse(BigDecimal.ZERO));

                totalDiscount = totalDiscount.add(Optional.ofNullable(f.getTotalDiscount()).orElse(BigDecimal.ZERO));
                totalExpense = totalExpense.add(Optional.ofNullable(f.getTotalExpense()).orElse(BigDecimal.ZERO));
                totalCost = totalCost.add(Optional.ofNullable(f.getTotalCost()).orElse(BigDecimal.ZERO));
                totalTax = totalTax.add(Optional.ofNullable(f.getTotalTax()).orElse(BigDecimal.ZERO));

                totalFreeItemValue = totalFreeItemValue.add(freeItemValue);
                totalPurchase = totalPurchase.add(Optional.ofNullable(f.getGrossTotal()).orElse(BigDecimal.ZERO));
                totalRetail = totalRetail.add(retailValue);
                totalWholesale = totalWholesale.add(wholesaleValue);

                totalQty = totalQty.add(qty);
                totalFreeQty = totalFreeQty.add(freeQty);
                totalQtyAtomic = totalQtyAtomic.add(Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO));
                totalFreeQtyAtomic = totalFreeQtyAtomic.add(Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO));

                grossTotal = grossTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
                lineGrossTotal = lineGrossTotal.add(Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO));

                netTotal = netTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                lineNetTotal = lineNetTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
            }

        }

        // Assign legacy totals
        getBill().setTotal(grossTotal.doubleValue());
        getBill().setNetTotal(netTotal.doubleValue());
        getBill().setSaleValue(saleValue);

        // Assign to BillFinanceDetails
        BillFinanceDetails bfd = getBill().getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(getBill());
            getBill().setBillFinanceDetails(bfd);
        }

        // Inputs from user or UI – left unchanged if already set
        bfd.setBillDiscount(bfd.getBillDiscount() != null ? bfd.getBillDiscount() : billDiscount);
        bfd.setBillExpense(bfd.getBillExpense() != null ? bfd.getBillExpense() : billExpense);
        bfd.setBillTaxValue(bfd.getBillTaxValue() != null ? bfd.getBillTaxValue() : billTax);
        bfd.setBillCostValue(bfd.getBillCostValue() != null ? bfd.getBillCostValue() : billCost);

        // Assign calculated from items
        bfd.setLineDiscount(totalLineDiscounts);
        bfd.setLineExpense(totalLineExpenses);
        bfd.setItemTaxValue(totalTaxLines);
        bfd.setLineCostValue(totalCostLines);

        bfd.setTotalDiscount(totalDiscount);
        bfd.setTotalExpense(totalExpense);
        bfd.setTotalCostValue(totalCost);
        bfd.setTotalTaxValue(totalTax);

        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalPurchaseValue(totalPurchase);
        bfd.setTotalRetailSaleValue(totalRetail);
        bfd.setTotalWholesaleValue(totalWholesale);

        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);

        bfd.setGrossTotal(grossTotal);
        bfd.setLineGrossTotal(lineGrossTotal);
        bfd.setNetTotal(netTotal);
        bfd.setLineNetTotal(lineNetTotal);
    }

    @Deprecated // use 
    public void calulateTotalsWhenAddingItemsOldMethod() {
        double tot = 0.0;
        double saleValue = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            if (p.getItem() instanceof Ampp) {
                p.setQty(p.getPharmaceuticalBillItem().getQtyPacks());
                p.setRate(p.getPharmaceuticalBillItem().getPurchaseRatePack());
            } else if (p.getItem() instanceof Amp) {
                p.setQty(p.getPharmaceuticalBillItem().getQty());
                p.setRate(p.getPharmaceuticalBillItem().getPurchaseRate());
            }
            p.setSearialNo(serialNo++);
            double netValue = p.getQty() * p.getRate();
            p.setNetValue(0 - netValue);
            tot += p.getNetValue();
            saleValue += (p.getPharmaceuticalBillItem().getQty() + p.getPharmaceuticalBillItem().getFreeQty()) * p.getPharmaceuticalBillItem().getRetailRate();
        }
        getBill().setTotal(tot);
        getBill().setNetTotal(tot);
        getBill().setSaleValue(saleValue);
    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.PharmacyPurchaseBill);
            bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
            bill.setReferenceInstitution(getSessionController().getInstitution());
        }
        return bill;
    }

    public double findLastRetailRate(Amp amp) {
        return getPharmacyBean().getLastRetailRate(amp, getSessionController().getDepartment());
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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
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

    public AmpController getAmpController() {
        return ampController;
    }

    public void setAmpController(AmpController ampController) {
        this.ampController = ampController;
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

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
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

}
