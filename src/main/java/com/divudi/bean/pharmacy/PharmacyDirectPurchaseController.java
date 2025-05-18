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

        if (f.getGrossRate() == null || f.getGrossRate().compareTo(BigDecimal.ZERO) <= 0) {
            JsfUtil.addErrorMessage("Please enter the purchase rate");
            return;
        }

        if (f.getRetailSaleRate() == null || f.getRetailSaleRate().compareTo(BigDecimal.ZERO) <= 0) {
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

        BigDecimal unitsPerPack = BigDecimal.valueOf(item.getDblValue());
        if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
            unitsPerPack = BigDecimal.ONE;
        }

        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;

        if (item instanceof Amp) {
            // AMP: no packs; assign both units and packs as same
            pbi.setQty(qty.doubleValue());
            pbi.setQtyPacks(qty.doubleValue());
            pbi.setFreeQty(freeQty.doubleValue());
            pbi.setFreeQtyPacks(freeQty.doubleValue());

            // Assign purchase rate (unit rate)
            pbi.setPurchaseRate(f.getGrossRate().doubleValue());
            pbi.setRetailRate(f.getRetailSaleRate().doubleValue());

        } else if (item instanceof Ampp) {
            // AMPP: has packs; need conversion
            BigDecimal qtyPacks = qty;
            BigDecimal freeQtyPacks = freeQty;

            BigDecimal qtyUnits = qtyPacks.multiply(unitsPerPack);
            BigDecimal freeQtyUnits = freeQtyPacks.multiply(unitsPerPack);

            pbi.setQty(qtyUnits.doubleValue());
            pbi.setQtyPacks(qtyPacks.doubleValue());
            pbi.setFreeQty(freeQtyUnits.doubleValue());
            pbi.setFreeQtyPacks(freeQtyPacks.doubleValue());

            // Assign purchase rate per pack
            pbi.setPurchaseRatePack(f.getGrossRate().doubleValue());
            pbi.setRetailRate(f.getRetailSaleRate().doubleValue());
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getBillItems().add(currentBillItem);

        currentBillItem = null;
        distributeProportionalBillValuesToItems(getBillItems(), getBill());
        calculateBillTotalsFromItems();
//        calulateTotalsWhenAddingItemsOldMethod();
    }

    private void recalculateFinancialsBeforeAddingBillItem(BillItemFinanceDetails f, boolean fromDiscountRate) {
        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal grossRate = f.getGrossRate() != null ? f.getGrossRate() : BigDecimal.ZERO;
        BigDecimal lineDiscountRate = f.getLineDiscountRate() != null ? f.getLineDiscountRate() : BigDecimal.ZERO;
        BigDecimal lineDiscountValue = f.getLineDiscount() != null ? f.getLineDiscount() : BigDecimal.ZERO;

        BigDecimal grossTotal = grossRate.multiply(qty);
        f.setGrossTotal(grossTotal.setScale(2, RoundingMode.HALF_UP));

        if (fromDiscountRate) {
            lineDiscountValue = lineDiscountRate.multiply(qty);
            f.setLineDiscount(lineDiscountValue.setScale(2, RoundingMode.HALF_UP));
        } else {
            // Back-calculate rate from value
            BigDecimal rate = qty.compareTo(BigDecimal.ZERO) > 0
                    ? lineDiscountValue.divide(qty, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setLineDiscountRate(rate.setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal netTotal = grossTotal.subtract(lineDiscountValue);
        f.setNetTotal(netTotal.setScale(2, RoundingMode.HALF_UP));
        f.setValueAtRetailRate(netTotal.setScale(2, RoundingMode.HALF_UP));

        BigDecimal unit = qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : qty;
        BigDecimal perUnit = netTotal.divide(unit, 6, RoundingMode.HALF_UP);
        f.setValueAtPurchaseRate(perUnit.setScale(2, RoundingMode.HALF_UP));
    }

    public void distributeProportionalBillValuesToItems(List<BillItem> billItems, Bill bill) {
        if (bill == null || bill.getBillFinanceDetails() == null || billItems == null || billItems.isEmpty()) {
            return;
        }

        BigDecimal totalBasis = BigDecimal.ZERO;
        Map<BillItem, BigDecimal> itemBases = new HashMap<>();

        // Calculate total basis for proportional allocation
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }

            BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;
            BigDecimal grossRate = f.getGrossRate() != null ? f.getGrossRate() : BigDecimal.ZERO;

            BigDecimal basis = grossRate.multiply(qty.add(freeQty));
            itemBases.put(bi, basis);
            totalBasis = totalBasis.add(basis);
        }

        if (totalBasis.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // Get bill-level totals
        BigDecimal billDiscountTotal = bill.getBillFinanceDetails().getBillDiscount() != null
                ? bill.getBillFinanceDetails().getBillDiscount()
                : BigDecimal.ZERO;
        BigDecimal billExpenseTotal = bill.getBillFinanceDetails().getTotalExpense() != null
                ? bill.getBillFinanceDetails().getTotalExpense()
                : BigDecimal.ZERO;
        BigDecimal billTaxTotal = bill.getBillFinanceDetails().getTotalTaxValue() != null
                ? bill.getBillFinanceDetails().getTotalTaxValue()
                : BigDecimal.ZERO;

        // Distribute to bill items
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }

            BigDecimal basis = itemBases.getOrDefault(bi, BigDecimal.ZERO);
            BigDecimal ratio = basis.divide(totalBasis, 12, RoundingMode.HALF_UP);

            // Allocate values
            BigDecimal itemDiscount = billDiscountTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemExpense = billExpenseTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTax = billTaxTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            // Set allocated values
            f.setBillTaxRate(itemDiscount);
            f.setBillExpense(itemExpense);
            f.setBillTax(itemTax);

            // Totals
            f.setTotalTaxRate(f.getLineDiscount().add(itemDiscount));
            f.setTotalExpense(itemExpense);
            f.setTotalTax(itemTax);

            // Cost = grossTotal - totalDiscount + totalTax + expense
            BigDecimal cost = f.getGrossTotal()
                    .subtract(f.getTotalTaxRate())
                    .add(f.getTotalTax())
                    .add(f.getTotalExpense());
            f.setBillCost(cost.setScale(2, RoundingMode.HALF_UP));
            f.setTotalCost(cost.setScale(2, RoundingMode.HALF_UP));

            // Cost Rate = cost / (qty + free)
            BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;
            BigDecimal totalQty = qty.add(freeQty);

            BigDecimal costRate = totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? cost.divide(totalQty, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillCostRate(costRate.setScale(4, RoundingMode.HALF_UP));
            f.setTotalCostRate(costRate.setScale(4, RoundingMode.HALF_UP));
        }
    }

    public void onQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        recalculateFinancialsBeforeAddingBillItem(f, true); // Assume rate is the driver
    }

    public void onFreeQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal grossRate = f.getGrossRate() != null ? f.getGrossRate() : BigDecimal.ZERO;

        BigDecimal totalQty = qty.add(freeQty);
        f.setTotalQuantity(totalQty.setScale(2, RoundingMode.HALF_UP));

        BigDecimal freeValue = grossRate.multiply(freeQty);
        f.setFreeValueAtCostRate(freeValue.setScale(2, RoundingMode.HALF_UP));

        BigDecimal netTotal = f.getNetTotal() != null ? f.getNetTotal() : BigDecimal.ZERO;
        BigDecimal unit = qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : qty;
        BigDecimal perUnit = netTotal.divide(unit, 6, RoundingMode.HALF_UP);
        f.setValueAtPurchaseRate(perUnit.setScale(2, RoundingMode.HALF_UP));
    }

    public void onGrossRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        recalculateFinancialsBeforeAddingBillItem(f, true); // Rate drives discount value

        // Recalculate free value
        BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal grossRate = f.getGrossRate() != null ? f.getGrossRate() : BigDecimal.ZERO;
        BigDecimal freeValue = grossRate.multiply(freeQty);
        f.setFreeValueAtCostRate(freeValue.setScale(2, RoundingMode.HALF_UP));
    }

    public void onLineDiscountRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        recalculateFinancialsBeforeAddingBillItem(f, true); // Recalculate value from rate
    }

    public void onRetailSaleRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal fqty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal retailRate = f.getRetailSaleRate() != null ? f.getRetailSaleRate() : BigDecimal.ZERO;
        BigDecimal totalQty = qty.add(fqty);
        BigDecimal retailValue = retailRate.multiply(totalQty);
        f.setValueAtRetailRate(retailValue.setScale(2, RoundingMode.HALF_UP));

        BigDecimal unit = qty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : qty;
        BigDecimal perUnit = retailValue.divide(unit, 6, RoundingMode.HALF_UP);
        f.setValueAtPurchaseRate(perUnit.setScale(2, RoundingMode.HALF_UP));
    }

    public void onRetailSaleRatePerUnitChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal ratePerUnit = f.getRetailSaleRatePerUnit() != null ? f.getRetailSaleRatePerUnit() : BigDecimal.ZERO;

        BigDecimal retailValue = ratePerUnit.multiply(qty);
        f.setValueAtPurchaseRate(ratePerUnit.setScale(2, RoundingMode.HALF_UP));
        f.setValueAtRetailRate(retailValue.setScale(2, RoundingMode.HALF_UP));
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
//        Item selectedItem = (Item) event.getObject();
//        System.out.println("selectedItem = " + selectedItem);
//        System.out.println("getCurrentBillItem().getItem() = " + getCurrentBillItem().getItem());
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
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
        getBillItems().remove(b.getSearialNo());
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

    public void addExpense() {
        if (getBill().getId() == null) {
            getBillFacade().create(getBill());
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

            double lastPurchaseRate = 0.0;
            lastPurchaseRate = getPharmacyBean().getLastPurchaseRate(i.getItem());

            if (i.getPharmaceuticalBillItem().getQty() + i.getPharmaceuticalBillItem().getFreeQty() == 0.0) {
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
            } else {
                getBillItemFacade().edit(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            } else {
                getPharmaceuticalBillItemFacade().edit(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);
            saveBillFee(i);
            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatch(i);
            double addingQty = tmpPh.getQtyInUnit() + tmpPh.getFreeQtyInUnit();

            tmpPh.setItemBatch(itemBatch);
            Stock stock = getPharmacyBean().addToStock(tmpPh, Math.abs(addingQty), getSessionController().getDepartment());
            tmpPh.setLastPurchaseRate(lastPurchaseRate);
            tmpPh.setStock(stock);
            getPharmaceuticalBillItemFacade().edit(tmpPh);

            getBill().getBillItems().add(i);
        }
        if (billItemsTotalQty == 0.0) {
            JsfUtil.addErrorMessage("Please Add Item Quantities To Bill");
            return;
        }

        //check and calculate expenses separately
        if (billExpenses != null && !billExpenses.isEmpty()) {
            getBill().setBillExpenses(billExpenses);

            double totalForExpenses = 0;
            for (BillItem expense : getBillExpenses()) {
                totalForExpenses += expense.getNetValue();
            }

            getBill().setExpenseTotal(-totalForExpenses);
            getBill().setNetTotal(getBill().getNetTotal() - totalForExpenses);
        }

        getPharmacyBillBean().calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());

        getBillFacade().edit(getBill());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getBill(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Successfully Billed");
        printPreview = true;
        //   recreate();

    }

    public void removeItem(BillItem bi) {
        //System.err.println("5 " + bi.getItem().getName());
        //System.err.println("6 " + bi.getSearialNo());
        getBillItems().remove(bi.getSearialNo());

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

        BigDecimal billDiscount = BigDecimal.ZERO;
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;

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

            // Preserve legacy sale value
            saleValue += (pbi.getQty() + pbi.getFreeQty()) * pbi.getRetailRate();

            if (f != null) {
                billDiscount = billDiscount.add(f.getBillTaxRate());
                totalLineDiscounts = totalLineDiscounts.add(f.getLineDiscount());
                totalDiscount = totalDiscount.add(f.getTotalTaxRate());
                totalExpense = totalExpense.add(f.getTotalExpense());
                totalFreeItemValue = totalFreeItemValue.add(f.getFreeValueAtCostRate() != null ? f.getFreeValueAtCostRate() : BigDecimal.ZERO);
                totalCost = totalCost.add(f.getTotalCost());
                totalPurchase = totalPurchase.add(f.getGrossTotal());
                totalQty = totalQty.add(f.getQuantity());
                totalFreeQty = totalFreeQty.add(f.getFreeQuantity());
                totalTax = totalTax.add(f.getTotalTax());
                totalRetail = totalRetail.add(f.getValueAtRetailRate());
                totalWholesale = totalWholesale.add(f.getValueAtWholesaleRate() != null ? f.getValueAtWholesaleRate() : BigDecimal.ZERO);
                totalQtyAtomic = totalQtyAtomic.add(f.getQuantity()); // update if separate conversion logic applies
                totalFreeQtyAtomic = totalFreeQtyAtomic.add(f.getFreeQuantity());
            }
        }

        // Set legacy values
        getBill().setTotal(tot);
        getBill().setNetTotal(tot);
        getBill().setSaleValue(saleValue);

        // Set finance details
        BillFinanceDetails bfd = getBill().getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(getBill());
            getBill().setBillFinanceDetails(bfd);
        }

        bfd.setBillDiscount(billDiscount);
        bfd.setTotalOfBillLineDiscounts(totalLineDiscounts);
        bfd.setTotalDiscount(totalDiscount);
        bfd.setTotalExpense(totalExpense);
        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalCostValue(totalCost);
        bfd.setTotalPurchaseValue(totalPurchase);
        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalTaxValue(totalTax);
        bfd.setTotalRetailSaleValue(totalRetail);
        bfd.setTotalWholesaleValue(totalWholesale);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);
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
