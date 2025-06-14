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
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
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
import com.divudi.service.pharmacy.PharmacyCostingService;
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

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);

        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;

        if (item instanceof Ampp) {
            BigDecimal unitsPerPack = Optional.ofNullable(f.getUnitsPerPack())
                    .orElse(BigDecimal.ONE);
            BigDecimal qtyUnits = f.getQuantity().multiply(unitsPerPack);
            BigDecimal freeQtyUnits = f.getFreeQuantity().multiply(unitsPerPack);

            pbi.setQty(qtyUnits.doubleValue());
            pbi.setQtyPacks(f.getQuantity().doubleValue());

            pbi.setFreeQty(freeQtyUnits.doubleValue());
            pbi.setFreeQtyPacks(f.getFreeQuantity().doubleValue());

            pbi.setPurchaseRate(f.getNetRate().doubleValue());
            pbi.setPurchaseRatePack(f.getNetRate().doubleValue());

            pbi.setRetailRate(f.getRetailSaleRate().doubleValue());
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
        pharmacyCostingService.distributeProportionalBillValuesToItems(getBillItems(), getBill());
        calculateBillTotalsFromItems();
//        calulateTotalsWhenAddingItemsOldMethod();
    }

// ChatGPT contributed - Recalculates line-level financial values before adding BillItem to bill

    public void onQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }
        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
    }

    public void onFreeQuantityChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
    }

    public void onLineGrossRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f); // Rate drives discount value

    }

    public void onLineDiscountRateChange() {
        BillItem bi = currentBillItem;
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
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

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
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
    /**
     * Properties
     */

    private PaymentMethodData paymentMethodData;

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
                packRate = lastRetailRate != null ? lastRetailRate : BigDecimal.ZERO;

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
        if (pbi != null) {
            pbi.setPurchaseRate(pr);
            pbi.setRetailRate(rr);
        }

        BillItemFinanceDetails f = current.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }

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

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
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

    public PharmacyDirectPurchaseController() {
    }

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

        double billItemsTotalQty = 0;

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

            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatchWithCosting(i);

            double addingQty = i.getBillItemFinanceDetails().getTotalQuantityByUnits().doubleValue();

            tmpPh.setItemBatch(itemBatch);

            Stock stock = getPharmacyBean().addToStock(tmpPh, Math.abs(addingQty), getSessionController().getDepartment());

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

        pharmacyCostingService.distributeProportionalBillValuesToItems(billItems, bill);
        calculateBillTotalsFromItems();
        calTotal();
        currentBillItem = null;
    }

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


    public void saveBill() {

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);

        getBill().setDeptId(deptId);
        getBill().setInsId(deptId);
        getBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);


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
        getBill().setSaleValue(totalRetail.doubleValue());

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


}
