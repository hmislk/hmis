/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemsDistributorsFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
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
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.util.BigDecimalUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class GrnCostingController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int PRICE_SCALE = 6;

    @Inject
    private SessionController sessionController;
    private BilledBill bill;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;

    @EJB
    @Deprecated
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
//    @EJB
//    PharmacyCostingService pharmacyCostingService;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    public boolean isShowProfitInGrnBill() {
        return configOptionApplicationController.getBooleanValueByKey("Show Profit Percentage in GRN", true);
    }

    /**
     * Wrapper for PharmacyCostingService.calculateProfitMarginForPurchases to
     * be used in JSF.
     *
     * @param bi
     * @return
     */
    public double calculateProfitMargin(BillItem bi) {
        return calculateProfitMarginForPurchases(bi);
    }

//    public double calculateProfitMargin(BillItem bi) {
//        return pharmacyCostingService.calculateProfitMarginForPurchases(bi);
//    }
    /////////////////
    private Bill approveBill;
    // private Bill grnBill; // Removed in favor of single accessor getGrnBill()
    private Bill currentGrnBillPre;
    private boolean printPreview;
    //////////////
    // Removed billItems field - using bill's collection directly
    private List<BillItem> selectedBillItems;
    private SearchKeyword searchKeyword;
    private double insTotal;
    private double difference;
    private Institution fromInstitution;
    private Institution referenceInstitution;
    BillItem currentExpense;

    public double calDifference() {
        double netTotal = getGrnBill().getNetTotal();
        difference = Math.abs(insTotal) - Math.abs(netTotal);
        return difference;
    }

    public String navigateToResiveCosting() {
        // Check if there are existing unapproved GRNs for this purchase order
        if (getApproveBill() != null && getApproveBill().getListOfBill() != null) {
            for (Bill existingGrn : getApproveBill().getListOfBill()) {
                if (existingGrn != null
                        && existingGrn.getBillTypeAtomic() != null
                        && existingGrn.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_GRN_PRE
                        && !existingGrn.isRetired()
                        && !existingGrn.isCancelled()) {
                    JsfUtil.addErrorMessage("There is already an unapproved GRN for this purchase order. Please approve or delete the existing GRN before creating a new one.");
                    return "";
                }
            }
        }

        clear();
        createGrn(); // This now includes discount copying and distribution
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setCreditDuration(getApproveBill().getCreditDuration());
        return "/pharmacy/pharmacy_grn_costing?faces-redirect=true";
    }

    public void clear() {
        // grnBill = null; // Field removed
        currentGrnBillPre = null; // Clear both bills since they should be the same object
        printPreview = false;
        // billItems removed - using bill's collection directly
        currentExpense = null; // Clear current expense to prevent duplication
        difference = 0;
        insTotal = 0;
    }

    public void removeItem(BillItem bi) {
        getBillItems().remove(bi.getSearialNo());
        calGrossTotal();
    }

    public List<BillItem> findAllBillItemsRefernceToOriginalItem(BillItem referenceBillItem) {
        List<BillItem> tmpBillItems = new ArrayList<>();
        for (BillItem i : getBillItems()) {
            if (i.getReferanceBillItem() == referenceBillItem) {
                tmpBillItems.add(i);
            }
        }
        return tmpBillItems;
    }

    public void duplicateItem(BillItem originalBillItemToDuplicate) {
        if (originalBillItemToDuplicate == null) {
            return;
        }
        BigDecimal totalQuantityOfBillItemsRefernceToOriginalItem = BigDecimal.ZERO;
        BigDecimal totalFreeQuantityOfBillItemsRefernceToOriginalItem = BigDecimal.ZERO;

        BigDecimal remainFreeQty = BigDecimal.ZERO;
        BigDecimal remainQty = BigDecimal.ZERO;

        BillItem newBillItemCreatedByDuplication = new BillItem();
        newBillItemCreatedByDuplication.copy(originalBillItemToDuplicate);
        newBillItemCreatedByDuplication.setId(null);

        BillItemFinanceDetails newBifd = originalBillItemToDuplicate.getBillItemFinanceDetails().clone();
        newBifd.setId(null);
        newBifd.setBillItem(newBillItemCreatedByDuplication);

        PharmaceuticalBillItem newPharmaceuticalBillItemCreatedByDuplication = new PharmaceuticalBillItem();
        newPharmaceuticalBillItemCreatedByDuplication.copy(originalBillItemToDuplicate.getPharmaceuticalBillItem());
        newPharmaceuticalBillItemCreatedByDuplication.setId(null);
        newPharmaceuticalBillItemCreatedByDuplication.setBillItem(newBillItemCreatedByDuplication);

        newBillItemCreatedByDuplication.setItem(originalBillItemToDuplicate.getItem());
        newBillItemCreatedByDuplication.setReferanceBillItem(originalBillItemToDuplicate.getReferanceBillItem());
        newBillItemCreatedByDuplication.setPharmaceuticalBillItem(newPharmaceuticalBillItemCreatedByDuplication);
        newBillItemCreatedByDuplication.setBillItemFinanceDetails(newBifd);

        List<BillItem> tmpBillItems = findAllBillItemsRefernceToOriginalItem(originalBillItemToDuplicate.getReferanceBillItem());

        for (BillItem bi : tmpBillItems) {
            totalQuantityOfBillItemsRefernceToOriginalItem = totalQuantityOfBillItemsRefernceToOriginalItem.add(bi.getBillItemFinanceDetails().getQuantity());
            totalFreeQuantityOfBillItemsRefernceToOriginalItem = totalFreeQuantityOfBillItemsRefernceToOriginalItem.add(bi.getBillItemFinanceDetails().getFreeQuantity());
        }
        remainQty = BigDecimal.valueOf(originalBillItemToDuplicate.getPreviousRecieveQtyInUnit()).subtract(totalQuantityOfBillItemsRefernceToOriginalItem);
        remainFreeQty = BigDecimal.valueOf(originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit()).subtract(totalFreeQuantityOfBillItemsRefernceToOriginalItem);

        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQty(remainQty.doubleValue());
        newBifd.setQuantity(remainQty);
        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQtyInUnit(remainQty.doubleValue());
        newBifd.setFreeQuantity(remainFreeQty);

        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQty(remainFreeQty.doubleValue());
        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQtyInUnit(remainFreeQty.doubleValue());

        newBillItemCreatedByDuplication.setTmpQty(remainQty.doubleValue());
        newBillItemCreatedByDuplication.setTmpFreeQty(remainFreeQty.doubleValue());

        newBillItemCreatedByDuplication.setPreviousRecieveQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveQtyInUnit());
        newBillItemCreatedByDuplication.setPreviousRecieveFreeQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit());
        getBillItems().add(newBillItemCreatedByDuplication);
        recalculateFinancialsBeforeAddingBillItem(newBillItemCreatedByDuplication.getBillItemFinanceDetails());
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        recalculateProfitMarginsForAllItems();
        calDifference();
    }

    public void removeSelected() {
        //  //System.err.println("1");
        if (selectedBillItems == null) {
            //   //System.err.println("2");
            return;
        }

        //   //System.err.println("3");
        for (BillItem b : selectedBillItems) {
            //  //System.err.println("4");
            getBillItems().remove(b.getSearialNo());
            calGrossTotal();
        }

        selectedBillItems = null;
    }

    public void clearList() {
        //   pharmacyItems = null;
        //  billItems = null;
    }

    public void setBatch(BillItem pid) {
        if (pid.getPharmaceuticalBillItem().getDoe() == null) {
            return;
        }

        if (pid.getPharmaceuticalBillItem().getDoe() != null) {
            if (pid.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
                pid.getPharmaceuticalBillItem().setStringValue(null);
                return;
                //    return;
            }
        }

        if (pid.getPharmaceuticalBillItem().getStringValue().trim().isEmpty()) {
            Date date = pid.getPharmaceuticalBillItem().getDoe();
            DateFormat df = new SimpleDateFormat("ddMMyyyy");
            String reportDate = df.format(date);
// Print what date is today!
            //       //System.err.println("Report Date: " + reportDate);
            pid.getPharmaceuticalBillItem().setStringValue(reportDate);
        }
    }

    public void request() {
//        if (Math.abs(difference) > 1) {
//            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
//            return;
//        }
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        if (getCurrentGrnBillPre().getReferenceInstitution() == null) {
            getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        }
        // Invoice details are already bound on currentGrnBillPre via UI
        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());

        if (getCurrentGrnBillPre().getInvoiceDate() == null) {
            getCurrentGrnBillPre().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        String msg = errorCheck(getCurrentGrnBillPre(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        saveGrnPreBill();
        if (!getCurrentGrnBillPre().getBillItems().isEmpty()) {
            getCurrentGrnBillPre().setBillItems(null);
        }

//        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());
        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            applyFinanceDetailsToPharmaceutical(i);

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getCurrentGrnBillPre());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            } else {
                getPharmaceuticalBillItemFacade().edit(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

//                 updatePoItemQty(i);
//            System.err.println("1 " + i);
            ItemBatch itemBatch = saveItemBatch(i);
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
//
//            Stock stock = getPharmacyBean().addToStock(
//                    i.getPharmaceuticalBillItem(),
//                    Math.abs(addingQty),
//                    getSessionController().getDepartment());
//
//            i.getPharmaceuticalBillItem().setStock(stock);
            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
//            saveBillFee(i, p);
            getCurrentGrnBillPre().getBillItems().add(i);
        }
        for (BillItem bi : getCurrentGrnBillPre().getBillItems()) {
        }

        calGrossTotal();

        calculateRetailSaleValueAndFreeValueAtPurchaseRate(getCurrentGrnBillPre());
//        updateBalanceForGrn(getCurrentGrnBillPre());

        getBillFacade().edit(getCurrentGrnBillPre());

        //  getPharmacyBillBean().editBill(, , getSessionController());
//        printPreview = true;
        JsfUtil.addSuccessMessage("Request Saved");

    }

    public void calculateRetailSaleValueAndFreeValueAtPurchaseRate(Bill b) {
        double sale = 0.0;
        double free = 0.0;

        for (BillItem i : b.getBillItems()) {
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            if (ph == null) {
                continue;
            }
            sale += (ph.getQty() + ph.getFreeQty()) * ph.getRetailRate();
            free += ph.getFreeQty() * ph.getPurchaseRate();
        }
        if (b.getBillType() == BillType.PharmacyGrnReturn || b.getBillType() == BillType.PurchaseReturn || b.getClass().equals(CancelledBill.class) || b.getClass().equals(RefundBill.class)) {
            b.setSaleValue(0.0 - Math.abs(sale));
            b.setFreeValue(0.0 - Math.abs(free));
        } else {
            b.setSaleValue(Math.abs(sale));
            b.setFreeValue(Math.abs(free));
        }
    }

    public void requestFinalize() {
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        if (getCurrentGrnBillPre().getReferenceInstitution() == null) {
            getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        }
        // Invoice details are already bound on currentGrnBillPre via UI
        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());

        String msg = errorCheck(getCurrentGrnBillPre(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }
        if (!getCurrentGrnBillPre().getBillItems().isEmpty()) {
            getCurrentGrnBillPre().setBillItems(null);
        }
        finalizeBill();
        if (!getCurrentGrnBillPre().getBillItems().isEmpty()) {
            getCurrentGrnBillPre().setBillItems(null);
        }

//        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());
        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            applyFinanceDetailsToPharmaceutical(i);

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getCurrentGrnBillPre());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            } else {
                getPharmaceuticalBillItemFacade().edit(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

//                 updatePoItemQty(i);
//            System.err.println("1 " + i);
            ItemBatch itemBatch = saveItemBatch(i);
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
//
//            Stock stock = getPharmacyBean().addToStock(
//                    i.getPharmaceuticalBillItem(),
//                    Math.abs(addingQty),
//                    getSessionController().getDepartment());
//
//            i.getPharmaceuticalBillItem().setStock(stock);
            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
//            saveBillFee(i, p);
            getCurrentGrnBillPre().getBillItems().add(i);
        }
        getCurrentGrnBillPre().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getCurrentGrnBillPre().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getCurrentGrnBillPre().setToInstitution(getApproveBill().getFromInstitution());
        getCurrentGrnBillPre().setToDepartment(getApproveBill().getFromDepartment());

        getCurrentGrnBillPre().setInstitution(getSessionController().getInstitution());
        getCurrentGrnBillPre().setDepartment(getSessionController().getDepartment());

        getCurrentGrnBillPre().setCreater(getSessionController().getLoggedUser());
        getCurrentGrnBillPre().setCreatedAt(Calendar.getInstance().getTime());

        calGrossTotal();

//        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getCurrentGrnBillPre());
//        updateBalanceForGrn(getCurrentGrnBillPre());
        getBillFacade().edit(getCurrentGrnBillPre());

        //  getPharmacyBillBean().editBill(, , getSessionController());
//        printPreview = true;
        JsfUtil.addSuccessMessage("Request Finalized");

    }

    public void settle() {
        if (!validateInputs()) {
            return;
        }
        saveGrnBill();
        distributeValuesToItems();
        fillData(getGrnBill());
        processBillItems();
        finalizeSettle();
        createAndPersistPayment();
    }

    private void fillData(Bill inputBill) {
        double billTotalAtCostRate = 0.0;

        double purchaseFree = 0.0;
        double purchaseNonFree = 0.0;

        double retailFree = 0.0;
        double retailNonFree = 0.0;

        double wholesaleFree = 0.0;
        double wholesaleNonFree = 0.0;

        double costFree = 0.0;
        double costNonFree = 0.0;

        for (BillItem bi : getBillItems()) {

            BillItemFinanceDetails bifd = bi.getBillItemFinanceDetails();
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();

            if (bifd == null) {
                continue;
            }

            double purchaseRate = bifd.getLineNetRate() != null ? bifd.getLineNetRate().doubleValue() : 0.0;
            double retailRate = bifd.getRetailSaleRate() != null ? bifd.getRetailSaleRate().doubleValue() : 0.0;
            double wholesaleRate = bifd.getWholesaleRate() != null ? bifd.getWholesaleRate().doubleValue() : 0.0;
            double costRate = bifd.getTotalCostRate() != null ? bifd.getTotalCostRate().doubleValue() : 0.0;

            double totalCostValue = bifd.getTotalCost() != null ? bifd.getTotalCost().doubleValue() : 0.0;
            billTotalAtCostRate += totalCostValue;

            double freeQty = bifd.getFreeQuantityByUnits() != null ? bifd.getFreeQuantityByUnits().doubleValue() : 0.0;
            double paidQty = bifd.getQuantityByUnits() != null ? bifd.getQuantityByUnits().doubleValue() : 0.0;

            double tmp;

            tmp = freeQty * purchaseRate;
            purchaseFree += tmp;

            tmp = paidQty * purchaseRate;
            purchaseNonFree += tmp;

            tmp = freeQty * retailRate;
            retailFree += tmp;

            tmp = paidQty * retailRate;
            retailNonFree += tmp;

            tmp = freeQty * wholesaleRate;
            wholesaleFree += tmp;

            tmp = paidQty * wholesaleRate;
            wholesaleNonFree += tmp;

            tmp = freeQty * costRate;
            costFree += tmp;

            tmp = paidQty * costRate;
            costNonFree += tmp;

            BigDecimal grossTotal = bifd.getGrossTotal();
            if (grossTotal != null) {
                bifd.setGrossRate(grossTotal.abs().negate());
            } else {
                bifd.setGrossRate(BigDecimal.ZERO);
            }

            BigDecimal netTotal = bifd.getNetTotal();
            if (netTotal != null) {
                bifd.setNetRate(netTotal.abs().negate());
            } else {
                bifd.setNetRate(BigDecimal.ZERO);
            }

            // IMPORTANT: Do NOT recalculate valueAt* fields here
            // These are already calculated correctly in recalculateFinancialsBeforeAddingBillItem()
            // Previous code here was using INCORRECT formulas that overwrote correct values:
            // - Used GROSS rate instead of NET rate for valueAtPurchaseRate
            // - Used incorrect quantity multiplication for valueAtCostRate
            // These lines were removed to prevent data corruption

        }

        inputBill.getBillFinanceDetails().setTotalCostValue(BigDecimal.valueOf(costFree + costNonFree));
        inputBill.getBillFinanceDetails().setTotalCostValueFree(BigDecimal.valueOf(costFree));
        inputBill.getBillFinanceDetails().setTotalCostValueNonFree(BigDecimal.valueOf(costNonFree));

        inputBill.getBillFinanceDetails().setTotalPurchaseValue(BigDecimal.valueOf(purchaseFree + purchaseNonFree));
        inputBill.getBillFinanceDetails().setTotalPurchaseValueFree(BigDecimal.valueOf(purchaseFree));
        inputBill.getBillFinanceDetails().setTotalPurchaseValueNonFree(BigDecimal.valueOf(purchaseNonFree));

        inputBill.getBillFinanceDetails().setTotalRetailSaleValue(BigDecimal.valueOf(retailFree + retailNonFree));
        inputBill.getBillFinanceDetails().setTotalRetailSaleValueFree(BigDecimal.valueOf(retailFree));
        inputBill.getBillFinanceDetails().setTotalRetailSaleValueNonFree(BigDecimal.valueOf(retailNonFree));

        inputBill.getBillFinanceDetails().setTotalWholesaleValue(BigDecimal.valueOf(wholesaleFree + wholesaleNonFree));
        inputBill.getBillFinanceDetails().setTotalWholesaleValueFree(BigDecimal.valueOf(wholesaleFree));
        inputBill.getBillFinanceDetails().setTotalWholesaleValueNonFree(BigDecimal.valueOf(wholesaleNonFree));

        inputBill.setSaleValue(retailFree + retailNonFree);
        inputBill.setFreeValue(retailFree);

    }

    private boolean validateInputs() {
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return false;
        }
        if (getGrnBill().getFromInstitution() == null) {
            getGrnBill().setFromInstitution(getFromInstitution());
        }
        if (getGrnBill().getReferenceInstitution() == null) {
            getGrnBill().setReferenceInstitution(getReferenceInstitution());
        }
        if (getGrnBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return false;
        }

        // Invoice details are already bound on grnBill/currentGrnBillPre via UI
        String msg = errorCheck(getGrnBill(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return false;
        }
        calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        if (getGrnBill().getInvoiceDate() == null) {
            getGrnBill().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No items found in the GRN.");
            return false;
        }

        for (BillItem bi : getBillItems()) {
            if (bi == null || bi.getBillItemFinanceDetails() == null || bi.getPharmaceuticalBillItem() == null) {
                JsfUtil.addErrorMessage("Invalid item data found.");
                return false;
            }

            BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
            BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();
            BigDecimal grossRate = bi.getBillItemFinanceDetails().getLineGrossRate();
            BigDecimal retailRate = bi.getBillItemFinanceDetails().getRetailSaleRate();
            Date doe = bi.getPharmaceuticalBillItem().getDoe();
            String batch = bi.getPharmaceuticalBillItem().getStringValue();

            if ((qty == null ? BigDecimal.ZERO : qty)
                    .add(freeQty == null ? BigDecimal.ZERO : freeQty)
                    .compareTo(BigDecimal.ZERO) <= 0) {
                JsfUtil.addErrorMessage("Item " + bi.getItem().getName() + " has zero or negative quantity.");
                return false;
            }

            if (grossRate == null || grossRate.compareTo(BigDecimal.ZERO) <= 0) {
                JsfUtil.addErrorMessage("Item " + bi.getItem().getName() + " has zero or missing purchase rate.");
                return false;
            }

            if (retailRate == null || retailRate.compareTo(BigDecimal.ZERO) <= 0) {
                JsfUtil.addErrorMessage("Item " + bi.getItem().getName() + " has zero or missing retail rate.");
                return false;
            }

            if (doe == null) {
                JsfUtil.addErrorMessage("Item " + bi.getItem().getName() + " is missing expiry date.");
                return false;
            }

            if (batch == null || batch.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Item " + bi.getItem().getName() + " is missing batch number.");
                return false;
            }
        }

        return true;
    }

    private void saveGrnBill() {
        saveBill();
    }

    private void distributeValuesToItems() {
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
    }

    private void processBillItems() {
        for (BillItem i : getBillItems()) {
            applyFinanceDetailsToPharmaceutical(i);
            PharmaceuticalBillItem pbi = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getGrnBill());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }
            if (pbi.getId() == null) {
                getPharmaceuticalBillItemFacade().create(pbi);
            }
            i.setPharmaceuticalBillItem(pbi);
            getBillItemFacade().edit(i);
            updateStockAndBatches(i);
            editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
            saveBillFee(i);
            getGrnBill().getBillItems().add(i);
        }
    }

    private void updateStockAndBatches(BillItem i) {
        ItemBatch itemBatch;
        if (configOptionApplicationController.getBooleanValueByKey("Manage Costing", true)) {
            itemBatch = saveItemBatchWithCosting(i);
        } else {
            itemBatch = saveItemBatch(i);
        }
        double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit()
                + i.getPharmaceuticalBillItem().getFreeQtyInUnit();
        i.getPharmaceuticalBillItem().setItemBatch(itemBatch);
        Stock stock = getPharmacyBean().addToStock(
                i.getPharmaceuticalBillItem(),
                Math.abs(addingQty),
                getSessionController().getDepartment());
        i.getPharmaceuticalBillItem().setStock(stock);
        getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
    }

    private void createAndPersistPayment() {
        createPayment(getGrnBill(), getGrnBill().getPaymentMethod());
    }

    private void finalizeSettle() {
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(
                getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_GRN);
        getGrnBill().setDeptId(deptId);
        getGrnBill().setInsId(deptId);
        getGrnBill().setToInstitution(getApproveBill().getFromInstitution());
        getGrnBill().setToDepartment(getApproveBill().getFromDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setCreater(getSessionController().getLoggedUser());
        getGrnBill().setCreatedAt(Calendar.getInstance().getTime());

        // Recalculate expense totals using the new categorization method
        recalculateExpenseTotals();

        // Note: NetTotal is already correctly calculated by the service and includes expenses
        // Removed expense doubling line: getGrnBill().setNetTotal(getGrnBill().getNetTotal() - calExpenses());
        calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        updateBalanceForGrn(getGrnBill());
        getBillFacade().edit(getGrnBill());

        // Check if Purchase Order is fully received and update fullyIssued status
        if (getApproveBill() != null && !getApproveBill().isFullyIssued()) {
            if (isPurchaseOrderFullyReceived(getApproveBill())) {
                getApproveBill().setFullyIssued(true);
                getApproveBill().setFullyIssuedAt(new Date());
                getApproveBill().setFullyIssuedBy(getSessionController().getLoggedUser());
                getBillFacade().edit(getApproveBill());
            }
        }

        printPreview = true;
    }

    private boolean isPurchaseOrderFullyReceived(Bill purchaseOrderBill) {
        if (purchaseOrderBill == null) {
            return false;
        }

        List<PharmaceuticalBillItem> orderItems = getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(purchaseOrderBill);

        if (orderItems == null || orderItems.isEmpty()) {
            return true;
        }

        for (PharmaceuticalBillItem orderItem : orderItems) {
            double calculatedReturns = calculateRemainigQtyFromOrder(orderItem);
            double remainingQty = Math.abs(orderItem.getQtyInUnit()) - Math.abs(calculatedReturns);
            double remainingFreeQty = orderItem.getFreeQty() - calculateRemainingFreeQtyFromOrder(orderItem);

            if (remainingQty > 0 || remainingFreeQty > 0) {
                return false;
            }
        }

        return true;
    }

    //TODO: FIX then when wholesale is handled
    public void settleWholesale() {
        if (insTotal == 0 && difference != 0) {
            JsfUtil.addErrorMessage("Fill the invoice Total");
            return;
        }
        if (difference != 0) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }

        String msg = errorCheck(getGrnBill(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }
        calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        if (getGrnBill().getInvoiceDate() == null) {
            getGrnBill().setInvoiceDate(getApproveBill().getCreatedAt());
        }

        saveWholesaleBill();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());

        Payment p = createPayment(getGrnBill(), getGrnBill().getPaymentMethod());

        for (BillItem i : getBillItems()) {
            if (i.getTmpQty() == 0.0 && i.getTmpFreeQty() == 0.0) {
                continue;
            }

            applyFinanceDetailsToPharmaceutical(i);

            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getGrnBill());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            i.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(i);

            //     updatePoItemQty(i);
            //System.err.println("1 " + i);
            ItemBatch itemBatch;
            if (configOptionApplicationController.getBooleanValueByKey("Manage Costing", true)) {
                itemBatch = saveItemBatchWithCosting(i);
            } else {
                itemBatch = saveItemBatch(i);
            }
            // getPharmacyBillBean().preCalForAddToStock(i, itemBatch, getSessionController().getDepartment());

            double addingQty = i.getPharmaceuticalBillItem().getQtyInUnit() + i.getPharmaceuticalBillItem().getFreeQtyInUnit();

            i.getPharmaceuticalBillItem().setItemBatch(itemBatch);

            Stock stock = getPharmacyBean().addToStock(
                    i.getPharmaceuticalBillItem(),
                    Math.abs(addingQty),
                    getSessionController().getDepartment());

            i.getPharmaceuticalBillItem().setStock(stock);

            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            editBillItem(i.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());
            saveBillFee(i, p);
            getGrnBill().getBillItems().add(i);
        }

        getGrnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getGrnBill().setToInstitution(getApproveBill().getFromInstitution());
        getGrnBill().setToDepartment(getApproveBill().getFromDepartment());

        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());

        getGrnBill().setCreater(getSessionController().getLoggedUser());
        getGrnBill().setCreatedAt(Calendar.getInstance().getTime());

        calGrossTotal();

        calculateRetailSaleValueAndFreeValueAtPurchaseRate(getGrnBill());
        updateBalanceForGrn(getGrnBill());

        getBillFacade().edit(getGrnBill());
        //  getPharmacyBillBean().editBill(, , getSessionController());

        // Check if Purchase Order is fully received and update fullyIssued status
        if (getApproveBill() != null && !getApproveBill().isFullyIssued()) {
            if (isPurchaseOrderFullyReceived(getApproveBill())) {
                getApproveBill().setFullyIssued(true);
                getApproveBill().setFullyIssuedAt(new Date());
                getApproveBill().setFullyIssuedBy(getSessionController().getLoggedUser());
                getBillFacade().edit(getApproveBill());
            }
        }

        printPreview = true;

    }

    public double calFreeQuantityPurchaseValue(Bill b) {
        double freeTotal = 0.0;
        for (BillItem bi : b.getBillItems()) {
            freeTotal = freeTotal + (bi.getPharmaceuticalBillItem().getFreeQty() * bi.getPharmaceuticalBillItem().getPurchaseRate());
        }
        return freeTotal;
    }

    public double calFreeQuantitySaleValue(Bill b) {
        double freeTotal = 0.0;
        for (BillItem bi : b.getBillItems()) {
            freeTotal = freeTotal + (bi.getPharmaceuticalBillItem().getFreeQty() * bi.getPharmaceuticalBillItem().getRetailRate());
        }
        return freeTotal;
    }

    private void updateBalanceForGrn(Bill grn) {
        if (grn == null) {
            return;
        }
        switch (grn.getPaymentMethod()) {
            case Agent:
            case Card:
            case Cash:
            case Cheque:
            case MultiplePaymentMethods:
            case OnCall:
            case OnlineSettlement:
            case PatientDeposit:
            case Slip:
            case Staff:
            case YouOweMe:
            case ewallet:
                grn.setBalance(0.0);
                break;
            case Credit:
                grn.setBalance(Math.abs(grn.getNetTotal()));
            default:
        }
    }

    public void viewPoList() {
        clearList();

    }

    public GrnCostingController() {
    }

    private String txtSearch;

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Bill getApproveBill() {
        if (approveBill == null) {
            approveBill = new BilledBill();
        }
        return approveBill;
    }

    public void finalizeBill() {
        if (currentGrnBillPre == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (currentGrnBillPre.getId() == null) {
            request();
        }
        getCurrentGrnBillPre().setEditedAt(new Date());
        getCurrentGrnBillPre().setEditor(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setCheckeAt(new Date());
        getCurrentGrnBillPre().setCheckedBy(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER);
        getBillFacade().edit(getCurrentGrnBillPre());

    }

    public void saveGrnPreBill() {
        getCurrentGrnBillPre().setBillDate(new Date());
        getCurrentGrnBillPre().setBillTime(new Date());
        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());
        getCurrentGrnBillPre().setReferenceBill(getApproveBill());
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        getCurrentGrnBillPre().setDepartment(getSessionController().getDepartment());
        getCurrentGrnBillPre().setInstitution(getSessionController().getInstitution());

        getCurrentGrnBillPre().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));
        getCurrentGrnBillPre().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN));

        getCurrentGrnBillPre().setToInstitution(getApproveBill().getFromInstitution());
        getCurrentGrnBillPre().setToDepartment(getApproveBill().getFromDepartment());

        getCurrentGrnBillPre().setInstitution(getSessionController().getInstitution());
        getCurrentGrnBillPre().setDepartment(getSessionController().getDepartment());

        getCurrentGrnBillPre().setCreater(getSessionController().getLoggedUser());
        getCurrentGrnBillPre().setCreatedAt(Calendar.getInstance().getTime());
        //   getGrnBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        //   getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getGrnBill(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
//        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);
        if (getCurrentGrnBillPre().getId() == null) {
            getBillFacade().create(getCurrentGrnBillPre());
        } else {
            getBillFacade().edit(getCurrentGrnBillPre());
        }

        getApproveBill().setReferenceBill(getCurrentGrnBillPre());
        getBillFacade().edit(getApproveBill());
    }

    public void saveBill() {
        getGrnBill().setBillDate(new Date());
        getGrnBill().setBillTime(new Date());
//        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setReferenceBill(getApproveBill());
        getGrnBill().setReferenceInstitution(getReferenceInstitution());
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);
        getGrnBill().setBillType(BillType.PharmacyGrnBill);

        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
        } else {
            getBillFacade().edit(getGrnBill());
        }

        if (getCurrentGrnBillPre() != null) {

            if (getCurrentGrnBillPre().getId() == null) {
                billFacade.create(getCurrentGrnBillPre());
            } else {
                billFacade.edit(getCurrentGrnBillPre());
            }
            getCurrentGrnBillPre().setForwardReferenceBill(getGrnBill());
            getBillFacade().edit(getCurrentGrnBillPre());
        }

    }

    public void saveWholesaleBill() {
        getGrnBill().setBillDate(new Date());
        getGrnBill().setBillTime(new Date());
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setReferenceBill(getApproveBill());
        if (getGrnBill().getFromInstitution() == null) {
            getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        }
        getGrnBill().setDepartment(getSessionController().getDepartment());
        getGrnBill().setInstitution(getSessionController().getInstitution());
        //   getGrnBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        //   getGrnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getGrnBill(), BillType.PharmacyGrnBill, BillNumberSuffix.GRN));
        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
        }
    }

    public void generateBillComponent() {

        for (PharmaceuticalBillItem pbiInApprovedOrder : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {

            if (pbiInApprovedOrder.getBillItem() == null) {
                continue;
            }

            double calculatedReturns = calculateRemainigQtyFromOrder(pbiInApprovedOrder);
            double remains = Math.abs(pbiInApprovedOrder.getQty()) - Math.abs(calculatedReturns);
            double remainFreeQty = pbiInApprovedOrder.getFreeQty() - calculateRemainingFreeQtyFromOrder(pbiInApprovedOrder);

            if (remains > 0 || remainFreeQty > 0) {
                BillItem newlyCreatedBillItemForGrn = new BillItem();
                newlyCreatedBillItemForGrn.setSearialNo(getBillItems().size());
                newlyCreatedBillItemForGrn.setItem(pbiInApprovedOrder.getBillItem().getItem());
                newlyCreatedBillItemForGrn.setReferanceBillItem(pbiInApprovedOrder.getBillItem());

                // Since pbis are always in units, we need to convert appropriately for BillItem
                if (pbiInApprovedOrder.getBillItem().getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                    // For Ampp items, BillItem quantity should be in packs, so convert units to packs
                    double unitsPerPack = pbiInApprovedOrder.getBillItem().getItem().getDblValue();
                    unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                    newlyCreatedBillItemForGrn.setQty(remains / unitsPerPack); // Convert units to packs
                    newlyCreatedBillItemForGrn.setTmpQty(remains / unitsPerPack);
                    newlyCreatedBillItemForGrn.setTmpFreeQty(remainFreeQty / unitsPerPack);
                } else {
                    // For Amp items, BillItem quantity should also be in units
                    newlyCreatedBillItemForGrn.setQty(remains);
                    newlyCreatedBillItemForGrn.setTmpFreeQty(remainFreeQty);
                }

                PharmaceuticalBillItem newlyCreatedPbiForGrn = new PharmaceuticalBillItem();
                newlyCreatedPbiForGrn.setBillItem(newlyCreatedBillItemForGrn);
                double tmpQty = newlyCreatedBillItemForGrn.getQty();
                double tmpFreeQty = newlyCreatedBillItemForGrn.getTmpFreeQty();

                // Previous received quantities should always be stored in units (for tracking)
                newlyCreatedBillItemForGrn.setPreviousRecieveQtyInUnit(remains);
                newlyCreatedBillItemForGrn.setPreviousRecieveFreeQtyInUnit(remainFreeQty);

                // PharmaceuticalBillItem quantities are always in units
                newlyCreatedPbiForGrn.setQty(remains);
                newlyCreatedPbiForGrn.setFreeQty(remainFreeQty);

                double pr = pbiInApprovedOrder.getPurchaseRate(); // This is per unit rate
                double rr = pbiInApprovedOrder.getRetailRate(); // This is per unit rate

                if (pr == 0.0) {
                    double fallbackPr = getPharmacyBean().getLastPurchaseRate(newlyCreatedBillItemForGrn.getItem(), sessionController.getDepartment());
                    if (fallbackPr > 0.0) {
                        pr = fallbackPr;
                    }
                }

                if (rr == 0.0) {
                    double fallbackRr = getPharmacyBean().getLastRetailRateByBillItemFinanceDetails(newlyCreatedBillItemForGrn.getItem(), sessionController.getDepartment());
                    if (fallbackRr > 0.0) {
                        rr = fallbackRr;
                    }
                }

                // Set rates - for pbi they are always per unit rates
                newlyCreatedPbiForGrn.setPurchaseRate(pr);
                newlyCreatedPbiForGrn.setRetailRate(rr);

                // For BillItemFinanceDetails, we need the rate per BillItem unit
                double lineGrossRateForBillItem = pr;
                double retailRateForBillItem = rr;

                if (pbiInApprovedOrder.getBillItem().getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                    // For Ampp, BillItem is in packs, so we need pack rates
                    double unitsPerPack = pbiInApprovedOrder.getBillItem().getItem().getDblValue();
                    unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                    lineGrossRateForBillItem = pr * unitsPerPack; // Convert unit rate to pack rate

                    // For retail rate, check if we have a pack rate from PO
                    double retailRatePack = pbiInApprovedOrder.getRetailRatePack();
                    if (retailRatePack > 0) {
                        // Use the pack rate directly from PO
                        retailRateForBillItem = retailRatePack;
                    } else if (pbiInApprovedOrder.getRetailRate() > 0) {
                        // PO has unit rate, convert to pack rate
                        retailRateForBillItem = pbiInApprovedOrder.getRetailRate() * unitsPerPack;
                    } else {
                        // Use fallback rate directly (getLastRetailRateByBillItemFinanceDetails likely returns pack rate for AMPP)
                        retailRateForBillItem = rr;
                    }
                } else {
                }

                newlyCreatedBillItemForGrn.setPharmaceuticalBillItem(newlyCreatedPbiForGrn);

                BillItemFinanceDetails fd = new BillItemFinanceDetails(newlyCreatedBillItemForGrn);

                // Set quantities in BillItemFinanceDetails to match the BillItem quantities
                fd.setQuantity(java.math.BigDecimal.valueOf(newlyCreatedBillItemForGrn.getQty()));
                fd.setFreeQuantity(java.math.BigDecimal.valueOf(newlyCreatedBillItemForGrn.getTmpFreeQty()));
                fd.setLineGrossRate(java.math.BigDecimal.valueOf(lineGrossRateForBillItem));
                fd.setLineDiscountRate(java.math.BigDecimal.ZERO);
                fd.setRetailSaleRate(java.math.BigDecimal.valueOf(retailRateForBillItem));
                fd.setLineNetRate(BigDecimal.valueOf(lineGrossRateForBillItem));

                newlyCreatedBillItemForGrn.setBillItemFinanceDetails(fd);
                recalculateFinancialsBeforeAddingBillItem(fd);

                newlyCreatedBillItemForGrn.getPharmaceuticalBillItem().setLastPurchaseRate(pr);
                newlyCreatedBillItemForGrn.getPharmaceuticalBillItem().setLastPurchaseRateInUnit(pr);
                newlyCreatedBillItemForGrn.getPharmaceuticalBillItem().setLastPurchaseRatePack(pr * fd.getUnitsPerPack().doubleValue());

                getBillItems().add(newlyCreatedBillItemForGrn);
            }

        }

    }

    public void generateBillComponent(Bill importGrnBill) {

        for (BillItem importBi : importGrnBill.getBillItems()) {
            PharmaceuticalBillItem i = importBi.getPharmaceuticalBillItem();
            double remains = i.getQty() - calculateRemainigQtyFromOrder(i);
            double remainFreeQty = i.getFreeQty() - calculateRemainingFreeQtyFromOrder(i);

            if (remains > 0 || remainFreeQty > 0) {
                BillItem bi = new BillItem();
                bi.setSearialNo(getBillItems().size());
                bi.setItem(i.getBillItem().getItem());
                bi.setReferanceBillItem(i.getBillItem());

                // Since PBI quantities are always in units, convert appropriately for BillItem
                if (i.getBillItem().getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                    // For Ampp items, BillItem quantity should be in packs
                    double unitsPerPack = i.getBillItem().getItem().getDblValue();
                    unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                    bi.setQty(remains / unitsPerPack);
                    bi.setTmpQty(remains / unitsPerPack);
                    bi.setTmpFreeQty(remainFreeQty / unitsPerPack);
                } else {
                    // For Amp items, quantities are already in units
                    bi.setQty(remains);
                    bi.setTmpQty(remains);
                    bi.setTmpFreeQty(remainFreeQty);
                }
                //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
                ph.setBillItem(bi);
                double tmpQty = bi.getQty();
                double tmpFreeQty = bi.getTmpFreeQty();

                // Previous received quantities are always stored in units
                bi.setPreviousRecieveQtyInUnit(remains);
                bi.setPreviousRecieveFreeQtyInUnit(remainFreeQty);

                // PharmaceuticalBillItem quantities are always in units
                ph.setQty(remains);
                ph.setQtyInUnit(remains);

                ph.setFreeQtyInUnit(remainFreeQty);
                ph.setFreeQty(remainFreeQty);

                ph.setPurchaseRate(i.getPurchaseRate());
                ph.setRetailRate(i.getRetailRate());

                ph.setDoe(i.getDoe());
                ph.setStringValue(i.getStringValue());

                double wr = getWholesaleRate(
                        ph.getPurchaseRate(), ph.getQtyInUnit(), ph.getFreeQtyInUnit());
                ph.setWholesaleRate(wr);

                ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));

                bi.setPharmaceuticalBillItem(ph);

                BillItemFinanceDetails fd = new BillItemFinanceDetails(bi);
                // Set quantities to match BillItem quantities (not PBI quantities)
                fd.setQuantity(java.math.BigDecimal.valueOf(tmpQty));
                fd.setFreeQuantity(java.math.BigDecimal.valueOf(tmpFreeQty));

                // Set rates appropriate for BillItem unit type
                if (bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                    // For Ampp, BillItem is in packs, so use pack rates
                    double unitsPerPack = bi.getItem().getDblValue();
                    unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                    fd.setLineGrossRate(java.math.BigDecimal.valueOf(ph.getPurchaseRate() * unitsPerPack));
                    fd.setRetailSaleRate(java.math.BigDecimal.valueOf(ph.getRetailRate() * unitsPerPack));
                } else {
                    // For Amp, BillItem is in units, so use unit rates
                    fd.setLineGrossRate(java.math.BigDecimal.valueOf(ph.getPurchaseRate()));
                    fd.setRetailSaleRate(java.math.BigDecimal.valueOf(ph.getRetailRate()));
                }
                fd.setWholesaleRate(BigDecimal.valueOf(wr));
                bi.setBillItemFinanceDetails(fd);
                recalculateFinancialsBeforeAddingBillItem(fd);

                getBillItems().add(bi);
                //  getBillItems().r
            }

        }
    }

    public void generateBillComponentAll() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getApproveBill())) {
            double remains = i.getQtyInUnit() - calculateRemainigQtyFromOrder(i);

            BillItem bi = new BillItem();
            bi.setSearialNo(getBillItems().size());
            bi.setItem(i.getBillItem().getItem());
            bi.setReferanceBillItem(i.getBillItem());

            // Since PBI quantities are always in units, convert appropriately for BillItem
            if (i.getBillItem().getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                // For Ampp items, BillItem quantity should be in packs
                double unitsPerPack = i.getBillItem().getItem().getDblValue();
                unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                bi.setQty(remains / unitsPerPack);
                bi.setTmpQty(remains / unitsPerPack);
            } else {
                // For Amp items, quantities are already in units
                bi.setQty(remains);
                bi.setTmpQty(remains);
            }
            //Set Suggession
//                bi.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(bi.getItem()));

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.setBillItem(bi);
            double tmpQty = bi.getQty();

            // PharmaceuticalBillItem quantities are always in units
            ph.setQty(remains);
            ph.setQtyInUnit(remains);

            ph.setFreeQty(i.getFreeQty());
            ph.setFreeQtyInUnit(i.getFreeQty());

            ph.setPurchaseRate(i.getPurchaseRate());
            ph.setRetailRate(i.getRetailRate());

            double wr = getWholesaleRate(
                    ph.getPurchaseRate(), ph.getQtyInUnit(), ph.getFreeQtyInUnit());
            ph.setWholesaleRate(wr);

            ph.setLastPurchaseRate(getPharmacyBean().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));
            ph.setFreeQty(i.getFreeQty());

            bi.setPharmaceuticalBillItem(ph);

            BillItemFinanceDetails fd = new BillItemFinanceDetails(bi);
            // Set quantities to match BillItem quantities (not PBI quantities)
            fd.setQuantity(java.math.BigDecimal.valueOf(tmpQty));
            fd.setFreeQuantity(java.math.BigDecimal.valueOf(i.getFreeQty()));

            // Set rates appropriate for BillItem unit type
            if (bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
                // For Ampp, BillItem is in packs, so use pack rates
                double unitsPerPack = bi.getItem().getDblValue();
                unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                fd.setLineGrossRate(java.math.BigDecimal.valueOf(ph.getPurchaseRate() * unitsPerPack));
                fd.setRetailSaleRate(java.math.BigDecimal.valueOf(ph.getRetailRate() * unitsPerPack));
            } else {
                // For Amp, BillItem is in units, so use unit rates
                fd.setLineGrossRate(java.math.BigDecimal.valueOf(ph.getPurchaseRate()));
                fd.setRetailSaleRate(java.math.BigDecimal.valueOf(ph.getRetailRate()));
            }
            fd.setWholesaleRate(BigDecimal.valueOf(wr));
            bi.setBillItemFinanceDetails(fd);
            recalculateFinancialsBeforeAddingBillItem(fd);

            getBillItems().add(bi);
            //  getBillItems().r

        }
    }

    public void createGrn() {
        setFromInstitution(getApproveBill().getToInstitution());
        setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponent();

        // Copy discount from the approved purchase order to GRN
        if (getApproveBill() != null) {
            getGrnBill().setDiscount(getApproveBill().getDiscount());
        }

        // Ensure bill discount synchronization before distribution
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        recalculateProfitMarginsForAllItems();

    }

    public void calculateBillTotalsFromItems() {
        calculateBillTotalsFromItemsForPurchases(getGrnBill(), getBillItems());
    }

    // Keep the old method for backward compatibility if needed
    @Deprecated
    public void calculateBillTotalsFromItemsOld() {
        int serialNo = 0;

        // Bill-level inputs: do not calculate here
        BigDecimal billDiscount = BigDecimal.valueOf(getGrnBill().getDiscount());
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
            // Use net rate, not gross rate for netValue
            double netValue = bi.getQty() * bi.getNetRate();
            bi.setNetValue(0 - netValue);

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
                totalPurchase = totalPurchase.add(Optional.ofNullable(f.getValueAtPurchaseRate()).orElse(BigDecimal.ZERO));
                totalRetail = totalRetail.add(retailValue);
                totalWholesale = totalWholesale.add(wholesaleValue);

                totalQty = totalQty.add(qty);
                totalFreeQty = totalFreeQty.add(freeQty);
                totalQtyAtomic = totalQtyAtomic.add(Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO));
                totalFreeQtyAtomic = totalFreeQtyAtomic.add(Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO));

                grossTotal = grossTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
                lineGrossTotal = lineGrossTotal.add(Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO));

                netTotal = netTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                BigDecimal itemLineNetTotal = Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO);
                lineNetTotal = lineNetTotal.add(itemLineNetTotal);
            }

        }

        // Assign legacy totals
        getGrnBill().setTotal(grossTotal.doubleValue());
        getGrnBill().setNetTotal(netTotal.doubleValue());
        getGrnBill().setSaleValue(totalRetail.doubleValue());

        // Assign to BillFinanceDetails
        BillFinanceDetails bfd = getGrnBill().getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(getGrnBill());
            getGrnBill().setBillFinanceDetails(bfd);
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

    public void createGrn(Bill importGrn) {
        setFromInstitution(importGrn.getToInstitution());
        setReferenceInstitution(importGrn.getDepartment().getInstitution());
        generateBillComponent(importGrn);
        calGrossTotal();
    }

    public void createGrnAll() {
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponentAll();
        calGrossTotal();
    }

    public void createGrnWholesale() {
        getGrnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_WHOLESALE);
        getGrnBill().setPaymentMethod(getApproveBill().getPaymentMethod());
        getGrnBill().setFromInstitution(getApproveBill().getToInstitution());
        getGrnBill().setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());
        generateBillComponent();
        calGrossTotal();
    }

    private double getRetailPrice(BillItem billItem) {
        String sql = "select (p.retailRate) from PharmaceuticalBillItem p where p.billItem=:b";
        HashMap hm = new HashMap();
        hm.put("b", billItem.getReferanceBillItem());
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    private double getWholesaleRate(double purchaseRate, double qtyInUnit, double freeQtyInUnit) {
        double wholesaleFactor
                = configOptionApplicationController.getDoubleValueByKey("Wholesale Rate Factor", 1.08);
        return (purchaseRate * wholesaleFactor) * qtyInUnit / (freeQtyInUnit + qtyInUnit);
    }

    private void applyFinanceDetailsToPharmaceutical(BillItem bi) {
        if (bi == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();

        if (f == null || pbi == null) {
            return;
        }

        recalculateFinancialsBeforeAddingBillItem(f);

        if (bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp) {
            BigDecimal unitsPerPack = Optional.ofNullable(f.getUnitsPerPack())
                    .orElse(BigDecimal.ONE);
            BigDecimal qtyPacks = Optional.ofNullable(f.getQuantity())
                    .orElse(BigDecimal.ZERO);
            BigDecimal qtyUnits = qtyPacks.multiply(unitsPerPack);
            BigDecimal freeQtyPacks = Optional.ofNullable(f.getFreeQuantity())
                    .orElse(BigDecimal.ZERO);
            BigDecimal freeQtyUnits = freeQtyPacks.multiply(unitsPerPack);

            pbi.setQty(qtyUnits.doubleValue());
            pbi.setQtyInUnit(pbi.getQty());
            pbi.setQtyPacks(qtyPacks.doubleValue());

            pbi.setFreeQty(freeQtyUnits.doubleValue());
            pbi.setFreeQtyInUnit(pbi.getFreeQty());
            pbi.setFreeQtyPacks(freeQtyPacks.doubleValue());

            // CRITICAL FIX: Use grossRate (not netRate) and convert pack rate to unit rate for AMPP
            // recalculateFinancialsBeforeAddingBillItem already set correct values, don't overwrite
            // pbi.purchaseRate and purchaseRatePack already set correctly by recalculate method

            // Set cost rate (ALWAYS per unit per HMIS standard)
            BigDecimal lineCostRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
            pbi.setCostRate(lineCostRate.doubleValue());

            pbi.setRetailRate(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());
            pbi.setRetailRatePack(Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO).doubleValue());
            pbi.setRetailRateInUnit(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());

            // Update BillItem quantity and rate in packs
            bi.setQty(qtyPacks.doubleValue());
            bi.setRate(pbi.getPurchaseRatePack());
        } else {
            BigDecimal qty = Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO);
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO);

            pbi.setQty(qty.doubleValue());
            pbi.setQtyInUnit(pbi.getQty());
            pbi.setQtyPacks(pbi.getQty());

            pbi.setFreeQty(freeQty.doubleValue());
            pbi.setFreeQtyInUnit(pbi.getFreeQty());
            pbi.setFreeQtyPacks(pbi.getFreeQty());

            // CRITICAL FIX: recalculateFinancialsBeforeAddingBillItem already set correct values, don't overwrite
            // pbi.purchaseRate and purchaseRatePack already set correctly by recalculate method

            // Set cost rate (ALWAYS per unit per HMIS standard)
            BigDecimal lineCostRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
            pbi.setCostRate(lineCostRate.doubleValue());

            double r = Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue();
            pbi.setRetailRate(r);
            pbi.setRetailRatePack(r);
            pbi.setRetailRateInUnit(r);

            // Update BillItem quantity and rate in units
            bi.setQty(qty.doubleValue());
            bi.setRate(pbi.getPurchaseRate());
        }
    }

    public void onEdit(RowEditEvent event) {
        BillItem editingBillItem = (BillItem) event.getObject();
        setBatch(editingBillItem);
        onEdit(editingBillItem);
    }

    public void checkQty(BillItem bi) {

        if (bi.getTmpQty() < 0.0) {
            bi.setTmpQty(0.0);
        }

        if (bi.getTmpFreeQty() < 0.0) {
            bi.setTmpFreeQty(0.0);
        }

        onEdit(bi);
    }

    public void onEdit(BillItem tmp) {
        setBatch(tmp);
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        double remains = getRemainingQty(tmp.getPharmaceuticalBillItem());
        if (remains < f.getQuantity().doubleValue()) {
            f.setQuantity(java.math.BigDecimal.valueOf(remains));
            tmp.setTmpQty(remains);
            JsfUtil.addErrorMessage("You cant Change Qty than Remaining qty");
        }

        if (f.getLineGrossRate().compareTo(f.getRetailSaleRatePerUnit()) > 0) {
            f.setRetailSaleRatePerUnit(f.getLineGrossRate());
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }
        recalculateFinancialsBeforeAddingBillItem(f);
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());

        calDifference();
        recalculateProfitMarginsForAllItems();
    }

    public void lineDiscountRateChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        recalculateFinancialsBeforeAddingBillItem(f);
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        // Recompute margins as free quantity changes affect potential income
        recalculateProfitMarginsForAllItems();
    }

    public void retailRateChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        recalculateFinancialsBeforeAddingBillItem(f);

        // Redistribute bill discount after retail rate changes (even if discount is 0 to clear previous distributions)
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        recalculateProfitMarginsForAllItems();
        calDifference();
    }

    public void freeQtyChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        recalculateFinancialsBeforeAddingBillItem(f);

        // Ensure discount synchronization after free quantity changes
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        // Recompute margins as free quantity changes affect potential income
        recalculateProfitMarginsForAllItems();
    }

    public void qtyChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        recalculateFinancialsBeforeAddingBillItem(f);

        // Redistribute bill discount after quantity changes (even if discount is 0 to clear previous distributions)
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
    }

    public void onEditPurchaseRate(BillItem tmp) {
        double retail = tmp.getPharmaceuticalBillItem().getPurchaseRate() + (tmp.getPharmaceuticalBillItem().getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
        tmp.getPharmaceuticalBillItem().setRetailRate(retail);
    }

    @Deprecated
    public void calGrossTotal() {
        double tmp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            BillItemFinanceDetails f = p.getBillItemFinanceDetails();
            if (f != null) {
                java.math.BigDecimal rate = java.util.Optional.ofNullable(f.getLineGrossRate()).orElse(java.math.BigDecimal.ZERO);
                java.math.BigDecimal qty = java.util.Optional.ofNullable(f.getQuantity()).orElse(java.math.BigDecimal.ZERO);
                tmp += rate.multiply(qty).doubleValue();
            }
            p.setSearialNo(serialNo++);
        }
        discountChangedLitener();
    }

    public void calGrossTotalForGrnPreBill() {
        double tmp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            BillItemFinanceDetails f = p.getBillItemFinanceDetails();
            if (f != null) {
                java.math.BigDecimal rate = java.util.Optional.ofNullable(f.getLineGrossRate()).orElse(java.math.BigDecimal.ZERO);
                java.math.BigDecimal qty = java.util.Optional.ofNullable(f.getQuantity()).orElse(java.math.BigDecimal.ZERO);
                tmp += rate.multiply(qty).doubleValue();
            }
            p.setSearialNo(serialNo++);
        }

        getGrnBill().setTotal(0 - tmp);
        discountChangedLitener();
    }

    public void discountChangedLitener() {
        ensureBillDiscountSynchronization();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        // Recalculate profit margins for all items after discount distribution
        recalculateProfitMarginsForAllItems();
        // The distribution method handles final bill totals via aggregateBillTotalsFromDistributedItems
        calDifference();
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

    /**
     * Ensures that both bill.discount and bill.billFinanceDetails.billDiscount
     * are synchronized The service method reads from
     * billFinanceDetails.billDiscount, but UI may store in bill.discount
     */
    private void ensureBillDiscountSynchronization() {
        if (getGrnBill() == null) {
            return;
        }

        // Ensure BillFinanceDetails exists
        if (getGrnBill().getBillFinanceDetails() == null) {
            getGrnBill().setBillFinanceDetails(new BillFinanceDetails(getGrnBill()));
        }

        // Synchronize discount and tax from bill to billFinanceDetails
        getGrnBill().getBillFinanceDetails().setBillDiscount(BigDecimal.valueOf(getGrnBill().getDiscount()));
        getGrnBill().getBillFinanceDetails().setBillTaxValue(BigDecimal.valueOf(getGrnBill().getTax()));

    }

    public void saveBillFee(BillItem bi) {
        saveBillFee(bi, null);
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
        // Bill Fee Payment Concept is no loger used. 
        // createBillFeePaymentAndPayment(bf, p);
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
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

    @Deprecated // THis is NO longer Needed
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

    public BillItem getCurrentExpense() {
        if (currentExpense == null) {
            currentExpense = new BillItem();
            currentExpense.setQty(1.0);
            // Explicitly set default value for consideredForCosting
            currentExpense.setConsideredForCosting(true);
        }
        return currentExpense;
    }

    public void setCurrentExpense(BillItem currentExpense) {
        this.currentExpense = currentExpense;
    }

    /**
     * Clear the current expense form completely
     */
    public void clearCurrentExpense() {
        this.currentExpense = null;
    }

    // Method called when expense item is selected from autocomplete
    public void onExpenseItemSelect() {
        // This method is called when an expense item is selected from the autocomplete
    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.StorePurchase);
            bill.setReferenceInstitution(getSessionController().getInstitution());
        }
        return bill;
    }

    public void addExpense() {
        if (getGrnBill().getId() == null) {
            getBillFacade().create(getGrnBill());
            if (getGrnBill().getBillFinanceDetails() == null) {
                getGrnBill().setBillFinanceDetails(new BillFinanceDetails(getGrnBill()));
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

        getCurrentExpense().setSearialNo(getGrnBill().getBillExpenses().size());

        // CRITICAL: Set the expenseBill reference for JPA relationship
        getCurrentExpense().setExpenseBill(getGrnBill());

        getGrnBill().getBillExpenses().add(currentExpense);

        // Recalculate expense totals after adding new expense
        recalculateExpenseTotals();

        // Recalculate entire bill totals with updated expense categorization
        calculateBillTotalsFromItems();

        if (getGrnBill().getId() != null) {
            if (currentExpense.getId() == null) {
                getBillItemFacade().create(currentExpense);
            } else {
                getBillItemFacade().edit(currentExpense);
            }
        }

        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        recalculateProfitMarginsForAllItems();

        getBillFacade().edit(getGrnBill());

        currentExpense = null;
    }

    // Method to recalculate expense totals based on costing categorization
    public void recalculateExpenseTotals() {
        double totalExpenses = 0.0;
        double expensesForCosting = 0.0;
        double expensesNotForCosting = 0.0;

        if (getGrnBill().getBillExpenses() != null) {
            for (BillItem expense : getGrnBill().getBillExpenses()) {
                // Skip retired expenses
                if (expense.isRetired()) {
                    continue;
                }
                double expenseValue = expense.getNetValue();
                totalExpenses += expenseValue;

                if (expense.isConsideredForCosting()) {
                    expensesForCosting += expenseValue;
                } else {
                    expensesNotForCosting += expenseValue;
                }
            }
        }

        getGrnBill().setExpenseTotal(totalExpenses);
        getGrnBill().setExpensesTotalConsideredForCosting(expensesForCosting);
        getGrnBill().setExpensesTotalNotConsideredForCosting(expensesNotForCosting);
    }

    // Method to handle expense costing checkbox changes
    public void updateExpenseCosting(BillItem expense) {
        recalculateExpenseTotals();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        if (getGrnBill().getId() != null) {
            billFacade.edit(getGrnBill());
        }
    }

    // Method to get expenses considered for costing total
    public double getExpensesTotalConsideredForCosting() {
        if (getGrnBill() == null || getGrnBill().getBillExpenses() == null || getGrnBill().getBillExpenses().isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (BillItem expense : getGrnBill().getBillExpenses()) {
            // Skip retired expenses
            if (!expense.isRetired() && expense.isConsideredForCosting()) {
                total += expense.getNetValue();
            }
        }
        return total;
    }

    // Method to get expenses not considered for costing total
    public double getExpensesTotalNotConsideredForCosting() {
        if (getGrnBill() == null || getGrnBill().getBillExpenses() == null || getGrnBill().getBillExpenses().isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (BillItem expense : getGrnBill().getBillExpenses()) {
            // Skip retired expenses
            if (!expense.isRetired() && !expense.isConsideredForCosting()) {
                total += expense.getNetValue();
            }
        }
        return total;
    }

    public void removeExpense(BillItem expense) {
        if (expense == null) {
            return;
        }

        // Mark expense as retired instead of deleting
        expense.setRetired(true);
        expense.setRetiredAt(new Date());
        expense.setRetirer(getSessionController().getLoggedUser());

        // If expense was already persisted, update it in database
        if (expense.getId() != null) {
            getBillItemFacade().edit(expense);
        }

        // Reload bill expenses to exclude retired ones
        if (getGrnBill().getId() != null) {
            reloadBillExpenses();
        }

        recalculateExpenseTotals();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());

        calDifference();
        if (getGrnBill().getId() != null) {
            billFacade.edit(getGrnBill());
        }
    }

    /**
     * Reload bill expenses from database excluding retired ones
     */
    private void reloadBillExpenses() {
        if (getGrnBill().getId() != null) {
            String jpql = "SELECT be FROM BillItem be WHERE be.expenseBill.id = :billId AND be.retired = false ORDER BY be.searialNo";
            Map<String, Object> params = new HashMap<>();
            params.put("billId", getGrnBill().getId());
            List<BillItem> activeExpenses = getBillItemFacade().findByJpql(jpql, params);
            getGrnBill().setBillExpenses(activeExpenses);

            // Reindex serial numbers for remaining expenses
            int index = 0;
            for (BillItem be : activeExpenses) {
                be.setSearialNo(index++);
            }
        }
    }

    public double calExpenses() {
        double tot = 0.0;
        if (getGrnBill().getBillExpenses() != null) {
            for (BillItem be : getGrnBill().getBillExpenses()) {
                // Skip retired expenses
                if (!be.isRetired()) {
                    tot = tot + be.getNetValue();
                }
            }
        }
        return tot;
    }

    public void calTotal() {
        double tot = 0.0;
        double exp = 0.0;
        int serialNo = 0;
        for (BillItem p : getBillItems()) {
            p.setQty(p.getPharmaceuticalBillItem().getQtyInUnit());
            p.setRate(p.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            serialNo++;
            p.setSearialNo(serialNo);
            if (p.getParentBillItem() == null) {
                double netValue = p.getQty() * p.getRate();
                p.setNetValue(0 - netValue);
                tot += p.getNetValue();
            }
        }

        for (BillItem e : getGrnBill().getBillExpenses()) {
            // Skip retired expenses
            if (!e.isRetired()) {
                double nv = e.getNetRate() * e.getQty();
                e.setNetValue(0 - nv);
                exp += e.getNetValue();
            }
        }

        getBill().setExpenseTotal(exp);
        getBill().setTotal(tot);
        getBill().setNetTotal(tot + exp);

    }

//    public double getNetTotal() {
//
//        double tmp = getGrnBill().getTotal() + getGrnBill().getTax() - getGrnBill().getDiscount();
//        getGrnBill().setNetTotal(tmp);
//
//        return tmp;
//    }
    public void setApproveBill(Bill approveBill) {
        this.approveBill = approveBill;
//        grnBill = null;
//        dealor = null;
//        pos = null;
//        printPreview = false;
//        billItems = null;
//        createGrn();
    }

    public Bill getGrnBill() {
        // Use the same object as currentGrnBillPre for one-bill approach
        Bill bill = getCurrentGrnBillPre();

        // Ensure it has BillFinanceDetails for calculations
        if (bill.getBillFinanceDetails() == null) {
            bill.setBillFinanceDetails(new BillFinanceDetails(bill));
        }

        return bill;
    }

    @Deprecated // Use getCurrentGrnBillPre() directly if needed for setting
    public void setGrnBill(Bill grnBill) {
        // this.grnBill = grnBill; // Field removed
        this.currentGrnBillPre = grnBill;
    }

    public SessionController getSessionController() {
        if (sessionController == null) {
            throw new IllegalStateException("SessionController is not properly injected");
        }
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
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

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

//    public List<BillItem> getBillItems() {
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//        }
//        return billItems;
//    }
//
//    public void setBillItems(List<BillItem> billItems) {
//        this.billItems = billItems;
//    }
    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
    }

    public List<BillItem> getBillItems() {
        // Always return the bill's collection directly - single source of truth
        if (getCurrentGrnBillPre().getBillItems() == null) {
            getCurrentGrnBillPre().setBillItems(new ArrayList<>());
        }
        return getCurrentGrnBillPre().getBillItems();
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
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

    public double getInsTotal() {
        return insTotal;
    }

    public void setInsTotal(double insTotal) {
        this.insTotal = insTotal;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public Bill getCurrentGrnBillPre() {
        if (currentGrnBillPre == null) {
            currentGrnBillPre = new BilledBill();
            currentGrnBillPre.setBillType(BillType.PharmacyGrnBill);
            currentGrnBillPre.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_PRE);
            if (getApproveBill() != null) {
                currentGrnBillPre.setConsignment(getApproveBill().isConsignment());
            }
        }
        return currentGrnBillPre;
    }

    public void setCurrentGrnBillPre(Bill currentGrnBillPre) {
        this.currentGrnBillPre = currentGrnBillPre;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getReferenceInstitution() {
        return referenceInstitution;
    }

    public void setReferenceInstitution(Institution referenceInstitution) {
        this.referenceInstitution = referenceInstitution;
    }

    // Removed controller-level invoiceDate/invoiceNumber; use currentGrnBillPre's fields directly
    private void saveImportBill(Bill importGrn) {
        importGrn.setBillType(BillType.PharmacyGrnBillImport);
        importGrn.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_PRE);
        if (importGrn.getId() == null) {
            billFacade.create(importGrn);
        } else {
            billFacade.edit(importGrn);
        }
        for (BillItem bi : importGrn.getBillItems()) {
            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }
    }

    public String navigateToResiveCostingWithSaveApprove() {
        // Check if there are existing unapproved GRNs for this purchase order
        if (getApproveBill() != null && getApproveBill().getListOfBill() != null) {
            for (Bill existingGrn : getApproveBill().getListOfBill()) {
                if (existingGrn != null
                        && existingGrn.getBillTypeAtomic() != null
                        && existingGrn.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_GRN_PRE
                        && !existingGrn.isRetired()
                        && !existingGrn.isCancelled()) {
                    JsfUtil.addErrorMessage("There is already an unapproved GRN for this purchase order. Please approve or delete the existing GRN before creating a new one.");
                    return "";
                }
            }
        }

        clear();

        // Ensure current expense is cleared for fresh start
        setCurrentExpense(null);

        // Prepare bill and items without saving - like createGrn() but without persistence
        setFromInstitution(getApproveBill().getToInstitution());
        setReferenceInstitution(getSessionController().getLoggedUser().getInstitution());

        // Set the institution fields directly on the bill so they show up in the UI immediately
        getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());

        generateBillComponent(); // This creates bill items but doesn't save

        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());
        getCurrentGrnBillPre().setCreditDuration(getApproveBill().getCreditDuration());

        // Copy discount from the approved purchase order to GRN
        if (getApproveBill() != null) {
            getCurrentGrnBillPre().setDiscount(getApproveBill().getDiscount());
        }

        // Ensure calculations are done after setup
        if (getBillItems() != null && !getBillItems().isEmpty()) {
            // Ensure bill discount synchronization before distribution
            ensureBillDiscountSynchronization();
            calculateBillTotalsFromItems();
            distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
            recalculateProfitMarginsForAllItems();

            calDifference();
        }

        return "/pharmacy/pharmacy_grn_costing_with_save_approve?faces-redirect=true";
    }

    public String navigateToEditGrnCosting() {
        // Load the saved GRN for editing
        // Save the bill reference before clearing
        Bill savedBill = getCurrentGrnBillPre();
        clear();
        // Restore the bill reference after clearing
        setCurrentGrnBillPre(savedBill);

        // Ensure current expense is cleared when loading existing bill
        setCurrentExpense(null);

        // Explicitly fetch bill items from database to avoid lazy loading issues
        if (getCurrentGrnBillPre().getId() != null) {
            String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId ORDER BY bi.searialNo";
            Map<String, Object> params = new HashMap<>();
            params.put("billId", getCurrentGrnBillPre().getId());
            List<BillItem> loadedItems = getBillItemFacade().findByJpql(jpql, params);

            // Set the loaded items to the bill's collection directly
            getCurrentGrnBillPre().setBillItems(loadedItems);

            // Also load bill expenses to preserve them when editing (exclude retired ones)
            String expenseJpql = "SELECT be FROM BillItem be WHERE be.expenseBill.id = :billId AND be.retired = false ORDER BY be.searialNo";
            List<BillItem> loadedExpenses = getBillItemFacade().findByJpql(expenseJpql, params);
            getCurrentGrnBillPre().setBillExpenses(loadedExpenses);
        }

        // Invoice details already on currentGrnBillPre
        setFromInstitution(getCurrentGrnBillPre().getFromInstitution());

        // Recalculate totals when loading saved bill
        if (getBillItems() != null && !getBillItems().isEmpty()) {
            // Defensive: de-duplicate any repeated expense rows after loading
            deduplicateBillExpensesInMemory();

            // Ensure each bill item's finance details are properly calculated
            for (BillItem bi : getBillItems()) {
                if (bi.getBillItemFinanceDetails() != null) {
                    recalculateFinancialsBeforeAddingBillItem(bi.getBillItemFinanceDetails());
                }
            }

            recalculateExpenseTotals();
            calculateBillTotalsFromItems();
            ensureBillDiscountSynchronization();
            distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
            calDifference();
        }

        return "/pharmacy/pharmacy_grn_costing_with_save_approve?faces-redirect=true";
    }

    /**
     * Remove duplicate expense entries from the in-memory list before persist.
     * Two expenses are considered duplicates if they share the same: - Item id
     * (or both null), - Net rate, - Considered-for-costing flag, - Description
     * (trimmed, case-insensitive), and - Quantity. Keeps the first occurrence
     * and retires subsequent ones if already persisted; otherwise removes from
     * the list.
     */
    private void deduplicateBillExpensesInMemory() {
        if (getCurrentGrnBillPre() == null) {
            return;
        }
        List<BillItem> expenses = getCurrentGrnBillPre().getBillExpenses();
        if (expenses == null || expenses.isEmpty()) {
            return;
        }

        Map<String, BillItem> seen = new HashMap<>();
        List<BillItem> toRemove = new ArrayList<>();

        for (BillItem e : expenses) {
            if (e == null || e.isRetired()) {
                continue;
            }
            Long itemId = e.getItem() != null ? e.getItem().getId() : null;
            String desc = e.getDescreption() != null ? e.getDescreption().trim().toLowerCase() : "";
            String key = (itemId == null ? "_null" : itemId.toString())
                    + "|" + String.format(java.util.Locale.ROOT, "%.6f", e.getNetRate())
                    + "|" + (e.isConsideredForCosting() ? "1" : "0")
                    + "|" + desc
                    + "|" + String.format(java.util.Locale.ROOT, "%.6f", e.getQty() == null ? 0.0 : e.getQty());

            if (!seen.containsKey(key)) {
                seen.put(key, e);
            } else {
                // duplicate
                if (e.getId() != null) {
                    // Persisted duplicate: retire it so it won't load next time
                    e.setRetired(true);
                    e.setBill(null);
                    e.setExpenseBill(null);
                    e.setRetiredAt(new Date());
                    e.setRetirer(getSessionController().getLoggedUser());
                    getBillItemFacade().edit(e);
                } else {
                    // Not persisted yet: just drop it from the list
                    toRemove.add(e);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            expenses.removeAll(toRemove);
        }
    }

    public void requestWithSaveApprove() {
        // Simple save method for costing save/approve workflow
        // Allow saving with incomplete data - no validation required

        // Set basic bill information
        getCurrentGrnBillPre().setBillDate(new Date());
        getCurrentGrnBillPre().setBillTime(new Date());

        // Only set reference bill if not already set (for new GRNs, not edited ones)
        if (getCurrentGrnBillPre().getReferenceBill() == null) {
            getCurrentGrnBillPre().setReferenceBill(getApproveBill());
        }

        // Invoice details already on currentGrnBillPre via UI binding
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        if (getCurrentGrnBillPre().getReferenceInstitution() == null) {
            getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        }

        getCurrentGrnBillPre().setDepartment(getSessionController().getDepartment());
        getCurrentGrnBillPre().setInstitution(getSessionController().getInstitution());
        if (getCurrentGrnBillPre().getCreater() == null) {
            getCurrentGrnBillPre().setCreater(getSessionController().getLoggedUser());
        }
        if (getCurrentGrnBillPre().getCreatedAt()==null) {
            getCurrentGrnBillPre().setCreatedAt(Calendar.getInstance().getTime());
        }

        // Initialize bill items collection if null (getBillItems() handles this automatically)
        getBillItems(); // This will initialize if null

        // Create or update the main bill
        if (getCurrentGrnBillPre().getId() == null) {
            getBillFacade().create(getCurrentGrnBillPre());
        } else {
            getBillFacade().edit(getCurrentGrnBillPre());
        }

        // Defensive: de-duplicate any repeated expense rows before persisting
        // This prevents automatic duplication across save/edit cycles
        deduplicateBillExpensesInMemory();

        // Save bill items - work directly with bill's collection
        for (BillItem i : getCurrentGrnBillPre().getBillItems()) {
            BillItemFinanceDetails f = i.getBillItemFinanceDetails();
            if (f == null || ((f.getQuantity() == null || f.getQuantity().compareTo(BigDecimal.ZERO) == 0)
                    && (f.getFreeQuantity() == null || f.getFreeQuantity().compareTo(BigDecimal.ZERO) == 0))) {
                continue;
            }

            i.setBill(getCurrentGrnBillPre());
            i.setCreatedAt(new Date());
            i.setCreater(getSessionController().getLoggedUser());

            // Create or update BillItem
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            } else {
                getBillItemFacade().edit(i);
            }

            // Handle BillItemFinanceDetails
            if (i.getBillItemFinanceDetails() != null) {
                i.getBillItemFinanceDetails().setBillItem(i);
            }

            // Handle PharmaceuticalBillItem
            if (i.getPharmaceuticalBillItem() != null) {
                if (i.getPharmaceuticalBillItem().getId() == null) {
                    getPharmaceuticalBillItemFacade().create(i.getPharmaceuticalBillItem());
                } else {
                    getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                }
            }
        }

        // Do not manually create/edit expense items here to avoid double-persisting.
        // They will be persisted via cascade when editing the bill at the end.
        // Ensure the relationship is set for all current expenses.
        if (getCurrentGrnBillPre().getBillExpenses() != null) {
            for (BillItem expense : getCurrentGrnBillPre().getBillExpenses()) {
                if (expense == null) {
                    continue;
                }
                expense.setExpenseBill(getCurrentGrnBillPre());
                if (expense.getCreatedAt() == null) {
                    expense.setCreatedAt(new Date());
                }
                if (expense.getCreater() == null) {
                    expense.setCreater(getSessionController().getLoggedUser());
                }
            }
        }

        // Final bill update to persist the collection
        getBillFacade().edit(getCurrentGrnBillPre());

        // Ensure bill discount distribution before saving (even if 0 to clear previous distributions)
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();

        getBillFacade().edit(getCurrentGrnBillPre());

        JsfUtil.addSuccessMessage("GRN Saved");
    }

    public void finalizeGrnWithSaveApprove() {
        // Apply same validations as authorize button
        if (getCurrentGrnBillPre().getInvoiceNumber() == null || getCurrentGrnBillPre().getInvoiceNumber().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please fill invoice number");
            return;
        }

        // Validate required fields for finalization
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }

        if (getCurrentGrnBillPre().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return;
        }

        // Validate batch details and sale prices for finalization
        String msg = errorCheck(getCurrentGrnBillPre(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        // First perform the save operation
        requestWithSaveApprove();

        // Mark the bill as completed
        getCurrentGrnBillPre().setCompleted(true);
        getCurrentGrnBillPre().setCompletedBy(getSessionController().getLoggedUser());
        getCurrentGrnBillPre().setCompletedAt(new Date());

        // Save the completed state
        getBillFacade().edit(getCurrentGrnBillPre());

        printPreview = true;

        JsfUtil.addSuccessMessage("GRN Finalized");
    }

    public String errorCheck(Bill b, List<BillItem> billItems) {
        String msg = "";

        if (b.getInvoiceNumber() == null || b.getInvoiceNumber().trim().isEmpty()) {
            msg = "Please Fill invoice number";
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Cheque) {
            if (b.getBank() == null || b.getBank().getId() == null || b.getChequeRefNo() == null) {
                msg = "Please select Cheque Number and Bank";
            }
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Slip) {
            if (b.getBank() == null || b.getBank().getId() == null || b.getComments() == null) {
                msg = "Please Fill Memo and Bank";
            }
        }

        if (billItems.isEmpty()) {
            msg = "There is no Item to receive";
        }

        if (checkItemBatch(billItems)) {
            msg = "Please Fill Batch deatail and Sale Price to All Item";
        }

        if (b.getReferenceInstitution() == null) {
            msg = "Please Fill Reference Institution";
        }

        // Validate that GRN quantities do not exceed ordered quantities
        String quantityValidationMsg = validateGrnQuantities(billItems);
        if (!quantityValidationMsg.isEmpty()) {
            msg = quantityValidationMsg;
        }

        return msg;
    }

    private String validateGrnQuantities(List<BillItem> billItems) {
        if (getApproveBill() == null) {
            return "";
        }

        for (BillItem grnItem : billItems) {
            if (grnItem.getReferanceBillItem() == null || grnItem.getPharmaceuticalBillItem() == null) {
                continue;
            }

            BillItem purchaseOrderItem = grnItem.getReferanceBillItem();
            PharmaceuticalBillItem poItem = purchaseOrderItem.getPharmaceuticalBillItem();

            if (poItem == null) {
                continue;
            }

            PharmaceuticalBillItem currentGrnPbi = grnItem.getPharmaceuticalBillItem();

            double orderedQty = poItem.getQty();
            double orderedFreeQty = poItem.getFreeQty();
            double currentGrnQty = currentGrnPbi.getQty();
            double currentGrnFreeQty = currentGrnPbi.getFreeQty();

            System.out.println("Item: " + grnItem.getItem().getName() + " - Ordered: " + orderedQty + ", Current GRN: " + currentGrnQty);
            System.out.println("Item: " + grnItem.getItem().getName() + " - Ordered Free: " + orderedFreeQty + ", Current GRN Free: " + currentGrnFreeQty);

            double totalReceivedFromAllGrns = calculateRemainigQtyFromOrder(poItem);
            System.out.println("totalReceivedFromAllGrns = " + totalReceivedFromAllGrns);
            double totalFreeReceivedFromAllGrns = calculateRemainingFreeQtyFromOrder(poItem);
            System.out.println("totalFreeReceivedFromAllGrns = " + totalFreeReceivedFromAllGrns);

            double previouslyReceivedQty = totalReceivedFromAllGrns;
            double previouslyReceivedFreeQty = totalFreeReceivedFromAllGrns;

            System.out.println("Item: " + grnItem.getItem().getName() + " - Previously received: " + previouslyReceivedQty + ", Total to receive: " + (previouslyReceivedQty + currentGrnQty));

            if (orderedQty < previouslyReceivedQty + currentGrnQty) {
                return "Item " + grnItem.getItem().getName() + " cannot receive " + currentGrnQty
                        + " as it exceeds ordered quantity. Ordered: " + orderedQty + ", Already received: " + previouslyReceivedQty
                        + ", Remaining: " + (orderedQty - previouslyReceivedQty);
            }

            // Feature flag controlled free quantity validation
            boolean enableFreeQtyValidation = configOptionApplicationController.getBooleanValueByKey("Enable Free Quantity Validation in GRN", false);
            if (enableFreeQtyValidation && orderedFreeQty < previouslyReceivedFreeQty + currentGrnFreeQty) {
                return "Item " + grnItem.getItem().getName() + " cannot receive " + currentGrnFreeQty
                        + " free quantity as it exceeds ordered free quantity. Ordered free: " + orderedFreeQty
                        + ", Already received free: " + previouslyReceivedFreeQty
                        + ", Remaining free: " + (orderedFreeQty - previouslyReceivedFreeQty);
            }
        }

        return "";
    }

    public boolean checkItemBatch(List<BillItem> list) {

        for (BillItem i : list) {
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            if (ph == null) {
                continue;
            }
            if (ph.getQty() != 0.0) {
                if (ph.getDoe() == null || ph.getStringValue().trim().isEmpty()) {
                    return true;
                }
                if (ph.getPurchaseRate() > ph.getRetailRate()) {
                    return true;
                }

            }

        }
        return false;
    }

    public void requestFinalizeWithSaveApprove() {
        // Always use bill's invoice number, ignore controller reference
        if (getCurrentGrnBillPre().getInvoiceNumber() == null || getCurrentGrnBillPre().getInvoiceNumber().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please fill invoice number");
            return;
        }

        // Validate required fields for finalization
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }

        if (getCurrentGrnBillPre().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return;
        }

        // Validate batch details and sale prices for finalization
        String msg = errorCheck(getCurrentGrnBillPre(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        // First ensure the bill is saved
        if (getCurrentGrnBillPre().getId() == null) {
            requestWithSaveApprove(); // Save first if not already saved
        }

        // Process bill items for finalization with full stock management
        for (BillItem grnBillItem : getBillItems()) {
            BillItemFinanceDetails f = grnBillItem.getBillItemFinanceDetails();
            if (f == null || ((f.getQuantity() == null || f.getQuantity().compareTo(BigDecimal.ZERO) == 0)
                    && (f.getFreeQuantity() == null || f.getFreeQuantity().compareTo(BigDecimal.ZERO) == 0))) {
                continue;
            }

            // Apply standardized finance details conversion
            applyFinanceDetailsToPharmaceutical(grnBillItem);

            // Create/update item batch for stock management with costing respect
            ItemBatch itemBatch;
            if (configOptionApplicationController.getBooleanValueByKey("Manage Costing", true)) {
                itemBatch = saveItemBatchWithCosting(grnBillItem);
            } else {
                itemBatch = saveItemBatch(grnBillItem);
            }
            grnBillItem.getPharmaceuticalBillItem().setItemBatch(itemBatch);

            // Create stock record
            double addingQty = grnBillItem.getPharmaceuticalBillItem().getQty() + grnBillItem.getPharmaceuticalBillItem().getFreeQty();
            Stock stock = getPharmacyBean().addToStock(
                    grnBillItem.getPharmaceuticalBillItem(),
                    Math.abs(addingQty),
                    getSessionController().getDepartment());

            grnBillItem.getPharmaceuticalBillItem().setStock(stock);

            // Update pharmaceutical bill item with stock calculations
            getPharmaceuticalBillItemFacade().edit(grnBillItem.getPharmaceuticalBillItem());
            editBillItem(grnBillItem.getPharmaceuticalBillItem(), getSessionController().getLoggedUser());

            BillItem poBillItem = grnBillItem.getReferanceBillItem();
            System.out.println("grnBillItem = " + grnBillItem);
            if (poBillItem != null) {
                if (poBillItem.getId() != null) {
                    poBillItem = billItemFacade.findWithoutCache(poBillItem.getId());
                    if (poBillItem != null) {
                        PharmaceuticalBillItem poPbi = poBillItem.getPharmaceuticalBillItem();
                        if (poPbi != null) {
                            //getRemainingQty() is double, can NOT be null
                            //At this point grnBillItem.getPharmaceuticalBillItem() is NOT null as it is used above. its getQty is double, so can not be null
                            poPbi.setRemainingQty(Math.abs(poPbi.getRemainingQty()) - Math.abs(grnBillItem.getPharmaceuticalBillItem().getQty()));
                            poPbi.setRemainingFreeQty(Math.abs(poPbi.getRemainingFreeQty()) - Math.abs(grnBillItem.getPharmaceuticalBillItem().getFreeQty()));
                            poPbi.setCompletedQty(Math.abs(poPbi.getCompletedQty()) + Math.abs(grnBillItem.getPharmaceuticalBillItem().getQty()));
                            poPbi.setCompletedFreeQty(Math.abs(poPbi.getCompletedFreeQty()) + Math.abs(grnBillItem.getPharmaceuticalBillItem().getFreeQty()));
                            billItemFacade.editAndCommit(poBillItem);
                        }
                    }
                }
            }

        }

        // Check if bill number suffix is configured, if not set default "GRN" for Pharmacy GRN
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_GRN, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            // Set default suffix for Pharmacy GRN if not configured
            configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_GRN, "GRN");
        }

        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

        // Handle Department ID generation
        String deptId;
        if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(),
                    BillTypeAtomic.PHARMACY_GRN
            );
        } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(),
                    BillTypeAtomic.PHARMACY_GRN
            );
        } else {
            // Default behavior - use the original method
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN);
        }

        // Handle Institution ID generation separately
        String insId;
        if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(),
                    BillTypeAtomic.PHARMACY_GRN
            );
        } else {
            // Default behavior - use the department ID for institution ID or original method
            if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount || billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
                insId = deptId;
            } else {
                insId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN);
            }
        }

        getCurrentGrnBillPre().setDeptId(deptId);
        getCurrentGrnBillPre().setInsId(insId);

        // Set finalization timestamps and user
        getCurrentGrnBillPre().setEditedAt(new Date());
        getCurrentGrnBillPre().setEditor(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setChecked(true);
        getCurrentGrnBillPre().setCheckeAt(new Date());
        getCurrentGrnBillPre().setCheckedBy(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setApproveUser(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setApproveAt(new Date());
        // Change bill type from PRE to final GRN
        getCurrentGrnBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);

        // Ensure bill discount distribution before final calculations (even if 0 to clear previous distributions)
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        // Recompute totals/margins before first render
        calDifference();
        calculateRetailSaleValueAndFreeValueAtPurchaseRate(getCurrentGrnBillPre());

        // Update financial balances
        updateBalanceForGrn(getCurrentGrnBillPre());

        getBillFacade().edit(getCurrentGrnBillPre());

        // Create payment record
        Payment p = createPayment(getCurrentGrnBillPre(), getCurrentGrnBillPre().getPaymentMethod());

        // Check if Purchase Order is fully received and update fullyIssued status
        if (getApproveBill() != null && !getApproveBill().isFullyIssued()) {
            if (isPurchaseOrderFullyReceived(getApproveBill())) {
                getApproveBill().setFullyIssued(true);
                getApproveBill().setFullyIssuedAt(new Date());
                getApproveBill().setFullyIssuedBy(getSessionController().getLoggedUser());
                getBillFacade().edit(getApproveBill());
            }
        }

        JsfUtil.addSuccessMessage("GRN Finalized");
        printPreview = true;
    }

    /**
     * Distribute bill-level values (discounts, expenses, taxes) proportionally
     * to items.
     *
     * @param billItems
     * @param bill
     */
    public void distributeProportionalBillValuesToItems(List<BillItem> billItems, Bill bill) {
        if (bill == null) {
            return;
        }

        if (bill.getBillFinanceDetails() == null) {
            bill.setBillFinanceDetails(new BillFinanceDetails(bill));
        }

        // Reset and recalculate expense totals from actual bill expense items
        double expenseTotal = 0.0;
        double expensesTotalConsideredForCosting = 0.0;
        double expensesTotalNotConsideredForCosting = 0.0;

        if (bill.getBillExpenses() != null && !bill.getBillExpenses().isEmpty()) {
            for (com.divudi.core.entity.BillItem expense : bill.getBillExpenses()) {
                // Skip retired expenses for consistency with other methods
                if (expense.isRetired()) {
                    continue;
                }

                double expenseValue = expense.getNetValue();
                boolean isConsidered = expense.isConsideredForCosting();

                expenseTotal += expenseValue;
                if (isConsidered) {
                    expensesTotalConsideredForCosting += expenseValue;
                } else {
                    expensesTotalNotConsideredForCosting += expenseValue;
                }
            }
        }

        // Set the recalculated expense totals
        bill.setExpenseTotal(expenseTotal);
        bill.setExpensesTotalConsideredForCosting(expensesTotalConsideredForCosting);
        bill.setExpensesTotalNotConsideredForCosting(expensesTotalNotConsideredForCosting);

        bill.getBillFinanceDetails().setBillDiscount(BigDecimal.valueOf(bill.getDiscount()));
        bill.getBillFinanceDetails().setBillTaxValue(BigDecimal.valueOf(bill.getTax()));
        bill.getBillFinanceDetails().setBillExpense(BigDecimal.valueOf(expensesTotalConsideredForCosting));

        if (billItems == null || billItems.isEmpty()) {
            return;
        }

        // Note: Reset logic moved to calculateBillTotalsFromItemsForPurchases() method
        BigDecimal totalBasis = BigDecimal.ZERO;
        Map<BillItem, BigDecimal> itemBases = new HashMap<>();
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }
            BigDecimal qty = BigDecimalUtil.valueOrZero(f.getQuantity());
            BigDecimal freeQty = BigDecimalUtil.valueOrZero(f.getFreeQuantity());
            BigDecimal lineNetTotal = BigDecimalUtil.valueOrZero(f.getLineNetTotal());
            // Use line net total (after discounts) for proportional distribution basis
            BigDecimal basis = lineNetTotal;

            itemBases.put(bi, basis);
            totalBasis = totalBasis.add(basis);
        }

        if (BigDecimalUtil.isNullOrZero(totalBasis)) {
            return;
        }

        BigDecimal billDiscountTotal = BigDecimalUtil.valueOrZero(bill.getBillFinanceDetails().getBillDiscount());
        BigDecimal billExpenseTotal = BigDecimalUtil.valueOrZero(bill.getBillFinanceDetails().getBillExpense());
        BigDecimal billTaxTotal = BigDecimalUtil.valueOrZero(bill.getBillFinanceDetails().getBillTaxValue());

        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }
            BigDecimal basis = itemBases.get(bi);
            BigDecimal ratio = basis.divide(totalBasis, 12, RoundingMode.HALF_UP);

            BigDecimal lineDiscount = BigDecimalUtil.valueOrZero(f.getLineDiscount());
            BigDecimal lineExpense = BigDecimalUtil.valueOrZero(f.getLineExpense());
            BigDecimal lineTax = BigDecimalUtil.valueOrZero(f.getLineTax());
            BigDecimal lineNetTotal = BigDecimalUtil.valueOrZero(f.getLineNetTotal());
            BigDecimal lineGrossTotal = BigDecimalUtil.valueOrZero(f.getLineGrossTotal());
            BigDecimal lineGrossRate = BigDecimalUtil.valueOrZero(f.getLineGrossRate());

            BigDecimal billDiscount = billDiscountTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billExpense = billExpenseTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billTax = billTaxTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            f.setBillDiscount(billDiscount);
            f.setBillExpense(billExpense);
            f.setBillTax(billTax);

            BigDecimal totalDiscount = lineDiscount.add(billDiscount);
            BigDecimal totalExpense = lineExpense.add(billExpense);
            BigDecimal totalTax = lineTax.add(billTax);

            f.setTotalDiscount(totalDiscount);
            f.setTotalExpense(totalExpense);
            f.setTotalTax(totalTax);

            BigDecimal quantity = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal totalQty = quantity.add(freeQty);
            f.setTotalQuantity(totalQty);

            BigDecimal billDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? billDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillDiscountRate(billDiscountRate);

            BigDecimal totalDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setTotalDiscountRate(totalDiscountRate);

            BigDecimal netTotal = lineGrossTotal.subtract(totalDiscount).add(totalTax).add(totalExpense);
            f.setNetTotal(netTotal);
            f.setTotalCost(netTotal);

            BigDecimal billCost = netTotal.subtract(lineNetTotal);
            f.setBillCost(billCost);

            BigDecimal qtyUnits = Optional.ofNullable(f.getTotalQuantityByUnits())
                    .orElse(totalQty);

            BigDecimal lineCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? lineNetTotal.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal billCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal totalCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            f.setLineCostRate(lineCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setBillCostRate(billCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setTotalCostRate(totalCostRate.setScale(4, RoundingMode.HALF_UP));

            f.setLineGrossRate(lineGrossRate);
            f.setBillGrossRate(BigDecimal.ZERO);
            f.setGrossRate(lineGrossRate);

            f.setLineGrossTotal(lineGrossTotal);
            f.setBillGrossTotal(BigDecimal.ZERO);
            f.setGrossTotal(lineGrossTotal);

            if (f.getLineNetRate() == null || f.getLineNetRate().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal lineNetRate = quantity.compareTo(BigDecimal.ZERO) > 0
                        ? lineNetTotal.divide(quantity, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                f.setLineNetRate(lineNetRate);
            }

            BigDecimal billNetRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillNetRate(billNetRate);

            BigDecimal netRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setNetRate(netRate);
        }

        // After distribution, update bill-level totals by aggregating from distributed line items
        aggregateBillTotalsFromDistributedItems(bill, billItems);
    }

    /**
     * Recalculate line-level financial values before adding a BillItem to a
     * bill.
     *
     * @param billItemFinanceDetails
     */
    public void recalculateFinancialsBeforeAddingBillItem(BillItemFinanceDetails billItemFinanceDetails) {
        if (billItemFinanceDetails == null || billItemFinanceDetails.getBillItem() == null) {
            return;
        }
        BillItem billItem = billItemFinanceDetails.getBillItem();
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();

        Double prPerUnit;
        Double rrPerUnit;
        BigDecimal qty = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getQuantity());
        BigDecimal freeQty = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getFreeQuantity());
        BigDecimal lineGrossRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineGrossRate());
        BigDecimal lineDiscountRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineDiscountRate());
        BigDecimal retailRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getRetailSaleRate());

        Item item = billItemFinanceDetails.getBillItem().getItem();
        BigDecimal totalQty = qty.add(freeQty);

        BigDecimal unitsPerPack;
        BigDecimal qtyInUnits;
        BigDecimal freeQtyInUnits;
        BigDecimal totalQtyInUnits;
        if (item instanceof Ampp) {
            double dblVal = item.getDblValue();
            unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
            qtyInUnits = qty.multiply(unitsPerPack);
            freeQtyInUnits = freeQty.multiply(unitsPerPack);
            totalQtyInUnits = totalQty.multiply(unitsPerPack);
            prPerUnit = lineGrossRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP).doubleValue();
            rrPerUnit = retailRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP).doubleValue();
        } else {
            unitsPerPack = BigDecimal.ONE;
            qtyInUnits = qty;
            freeQtyInUnits = freeQty;
            totalQtyInUnits = totalQty;
            prPerUnit = lineGrossRate.doubleValue();
            rrPerUnit = retailRate.doubleValue();
        }

        billItemFinanceDetails.setUnitsPerPack(unitsPerPack);
        billItemFinanceDetails.setQuantityByUnits(qtyInUnits);
        billItemFinanceDetails.setFreeQuantityByUnits(freeQtyInUnits);
        billItemFinanceDetails.setTotalQuantityByUnits(totalQtyInUnits);

        BigDecimal lineGrossTotal = lineGrossRate.multiply(qty);
        // lineDiscountRate is amount per unit, not percentage
        BigDecimal lineDiscountValue = lineDiscountRate.multiply(qty);
        BigDecimal lineNetTotal = lineGrossTotal.subtract(lineDiscountValue);
        BigDecimal lineCostRate = BigDecimalUtil.isPositive(totalQtyInUnits)
                ? lineNetTotal.divide(totalQtyInUnits, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // CRITICAL FIX: Calculate values including BOTH paid and free quantities
        // retailValue = retailRatePerUnit × totalQtyInUnits (includes free qty)
        BigDecimal retailValue = BigDecimal.valueOf(rrPerUnit).multiply(totalQtyInUnits);

        // purchaseValue = purchaseRatePerUnit × totalQtyInUnits (includes free qty)
        BigDecimal purchaseValue = BigDecimal.valueOf(prPerUnit).multiply(totalQtyInUnits);

        billItemFinanceDetails.setLineGrossRate(lineGrossRate);
        billItemFinanceDetails.setLineNetRate(BigDecimalUtil.isPositive(qty)
                ? lineNetTotal.divide(qty, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        billItemFinanceDetails.setRetailSaleRatePerUnit(
                BigDecimalUtil.isPositive(unitsPerPack)
                ? retailRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        );

        billItemFinanceDetails.setLineDiscount(lineDiscountValue);
        billItemFinanceDetails.setLineGrossTotal(lineGrossTotal);
        billItemFinanceDetails.setLineNetTotal(lineNetTotal);
        billItemFinanceDetails.setLineCost(lineNetTotal);
        billItemFinanceDetails.setLineCostRate(lineCostRate);
        billItemFinanceDetails.setTotalQuantity(totalQty);

        // Set costRate (as user enters - pack rate for AMPP, unit rate for AMP)
        billItemFinanceDetails.setCostRate(lineCostRate.multiply(unitsPerPack));

        // Set purchaseRate (line net rate - purchase rate after discount, as user enters)
        billItemFinanceDetails.setPurchaseRate(
                BigDecimalUtil.isPositive(qty)
                        ? lineNetTotal.divide(qty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO
        );

        // Calculate valueAtCostRate = lineCostRate × totalQtyInUnits (cost rate is in units)
        billItemFinanceDetails.setValueAtCostRate(
                lineCostRate.multiply(totalQtyInUnits)
        );

        // Calculate valueAtPurchaseRate = lineNetRate × qty
        billItemFinanceDetails.setValueAtPurchaseRate(
                billItemFinanceDetails.getLineNetRate().multiply(qty)
        );

        // CRITICAL FIX: Calculate value fields for all rate types using total quantity by units
        // Following Direct Purchase pattern (lines 1202-1227)
        if (BigDecimalUtil.isPositive(totalQtyInUnits)) {
            // Value at retail rate (total units × retail rate per unit)
            BigDecimal retailRatePerUnit = BigDecimal.valueOf(rrPerUnit);
            billItemFinanceDetails.setValueAtRetailRate(
                    totalQtyInUnits.multiply(retailRatePerUnit)
            );

            // Value at wholesale rate (if wholesale rate is set)
            BigDecimal wholesaleRatePerUnit = billItemFinanceDetails.getWholesaleRatePerUnit();
            if (wholesaleRatePerUnit != null && wholesaleRatePerUnit.compareTo(BigDecimal.ZERO) > 0) {
                billItemFinanceDetails.setValueAtWholesaleRate(
                        totalQtyInUnits.multiply(wholesaleRatePerUnit)
                );
            }
        } else {
            // Set zero values if no quantity
            billItemFinanceDetails.setValueAtRetailRate(BigDecimal.ZERO);
            billItemFinanceDetails.setValueAtWholesaleRate(BigDecimal.ZERO);
        }

        billItemFinanceDetails.setProfitMargin(calculateProfitMarginForPurchasesBigDecimal(billItemFinanceDetails.getBillItem()));

        // CRITICAL FIX: Set BillItem fields (BI table)
        BillItem bi = billItemFinanceDetails.getBillItem();
        if (bi != null) {
            // Set rate (gross rate as user entered - pack rate for AMPP, unit rate for AMP)
            bi.setRate(lineGrossRate.doubleValue());

            // Set quantity (as user entered - packs for AMPP, units for AMP)
            bi.setQty(qty.doubleValue());

            // Set net rate (from BIFD.lineNetRate - already calculated correctly)
            BigDecimal lineNetRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineNetRate());
            bi.setNetRate(lineNetRate.doubleValue());

            // Set gross value (line gross total)
            bi.setGrossValue(lineGrossTotal.doubleValue());

            // Set net value (lineNetRate × qty) - negative for purchases
            BigDecimal biNetValue = lineNetRate.multiply(qty);
            bi.setNetValue(0 - biNetValue.doubleValue());
        }

        pbi.setRetailRate(rrPerUnit);
        pbi.setRetailRateInUnit(rrPerUnit);
        pbi.setRetailRatePack(retailRate.doubleValue());

        // CRITICAL FIX: Use calculated retail value (includes free qty)
        pbi.setRetailPackValue(retailValue.doubleValue());
        pbi.setRetailValue(retailValue.doubleValue());

        pbi.setPurchaseRate(prPerUnit);
        pbi.setPurchaseRatePack(lineGrossRate.doubleValue());

        // CRITICAL FIX: Use calculated purchase value (includes free qty)
        pbi.setPurchaseRatePackValue(purchaseValue.doubleValue());
        pbi.setPurchaseValue(purchaseValue.doubleValue());

        // CRITICAL FIX: Set cost rate (ALWAYS per unit per HMIS standard)
        pbi.setCostRate(lineCostRate.doubleValue());

        pbi.setQty(qtyInUnits.doubleValue());
        pbi.setFreeQty(freeQtyInUnits.doubleValue());
    }

    public double calculateProfitMarginForPurchases(BillItem bi) {
        return calculateProfitMarginForPurchasesBigDecimal(bi).doubleValue();
    }

    public BigDecimal calculateProfitMarginForPurchasesBigDecimal(BillItem bi) {
        if (bi == null) {
            return BigDecimal.ZERO;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        if (f == null) {
            return BigDecimal.ZERO;
        }

        // Prefer distributed total cost; fall back to item net totals if not yet distributed
        BigDecimal effectiveCost = BigDecimalUtil.valueOrZero(f.getTotalCost());
        if (effectiveCost.compareTo(BigDecimal.ZERO) == 0) {
            effectiveCost = BigDecimalUtil.valueOrZero(
                    f.getNetTotal() != null ? f.getNetTotal() : f.getLineNetTotal()
            );
        }
        if (effectiveCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Use unit-based revenue for consistency across AMP/AMPP
        BigDecimal totalUnits = BigDecimalUtil.valueOrZero(f.getTotalQuantityByUnits());
        BigDecimal retailPerUnit = BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit());
        BigDecimal totalPotentialIncome = totalUnits.multiply(retailPerUnit);

        return totalPotentialIncome.subtract(effectiveCost)
                .divide(effectiveCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Aggregate bill totals from items after bill-level distribution has
     * occurred. This preserves line-level values while updating bill totals
     * from distributed values.
     *
     * Fixed: Now ensures that expenses "considered for costing" are properly
     * reflected in bill totals. - Line items already contain distributed
     * portions of expenses "considered for costing" in their netTotal - Bill
     * total should include both: line items (with distributed expenses) +
     * expenses "not considered for costing"
     */
    private void aggregateBillTotalsFromDistributedItems(Bill bill, List<BillItem> billItems) {
        BigDecimal totalNetTotal = BigDecimal.ZERO;
        BigDecimal totalLineNetTotal = BigDecimal.ZERO; // Sum of line net totals for "Gross Total" display

        // Sum up all line item totals (these already include distributed expenses "considered for costing")
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f != null) {
                totalNetTotal = totalNetTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                totalLineNetTotal = totalLineNetTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
            }
        }

        // Note: Expenses "considered for costing" are already included in line item netTotals
        // from the distribution process, so they don't need to be added again here.
        // Expenses "NOT considered for costing" should NOT be included in net total.
        bill.setNetTotal(totalNetTotal.doubleValue());
        bill.setTotal(totalLineNetTotal.doubleValue()); // Total should be sum of line net totals for "Gross Total" display
    }

    public void calculateBillTotalsFromItemsForPurchases(Bill bill, List<BillItem> billItems) {

        // First, ensure expenses are properly calculated
        recalculateExpenseTotals();

        // Reset distributed bill-level values in all line items before calculating totals
        // Note: We only reset the DISTRIBUTED portions, not user-entered line-level values
        if (billItems != null && !billItems.isEmpty()) {
            for (BillItem bi : billItems) {
                BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
                if (f != null) {
                    // Reset only the distributed bill-level values (not user input)
                    f.setBillExpense(BigDecimal.ZERO);
                    f.setBillDiscount(BigDecimal.ZERO);  // This will be recalculated from bill.getDiscount()
                    f.setBillTax(BigDecimal.ZERO);       // This will be recalculated from bill.getTax()

                    // Reset totals to only line-level values (preserving user inputs)
                    f.setTotalExpense(BigDecimalUtil.valueOrZero(f.getLineExpense()));
                    f.setTotalDiscount(BigDecimalUtil.valueOrZero(f.getLineDiscount()));
                    f.setTotalTax(BigDecimalUtil.valueOrZero(f.getLineTax()));

                    // Reset NetTotal to LineNetTotal (no bill-level distributions)
                    f.setNetTotal(BigDecimalUtil.valueOrZero(f.getLineNetTotal()));
                    f.setTotalCost(BigDecimalUtil.valueOrZero(f.getLineNetTotal()));
                }
            }
        }

        int serialNo = 0;

        // Only bill-level values provided by user
        BigDecimal billDiscount = BigDecimal.valueOf(bill.getDiscount());
        BigDecimal billExpense = BigDecimal.valueOf(bill.getExpensesTotalConsideredForCosting());
        BigDecimal billTax = BigDecimal.valueOf(bill.getTax());
        BigDecimal billCost = billDiscount.subtract(billExpense.add(billTax));

        // Initialize totals
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalLineExpenses = BigDecimal.ZERO;
        BigDecimal totalLineCosts = BigDecimal.ZERO;
        BigDecimal totalTaxLines = BigDecimal.ZERO;

        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;

        // Initialize free item value totals
        BigDecimal totalPurchaseValueFree = BigDecimal.ZERO;
        BigDecimal totalCostValueFree = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValueFree = BigDecimal.ZERO;

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (BillItem bi : billItems) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

            // Don't override user-entered quantities during GRN costing
            // Only set quantities from PharmaceuticalBillItem if BillItem qty is not already set by user
            if (bi.getQty() == null || bi.getQty() == 0.0) {
                if (bi.getItem() instanceof Ampp) {
                    bi.setQty(pbi.getQtyPacks());
                    bi.setRate(pbi.getPurchaseRatePack());
                } else if (bi.getItem() instanceof Amp) {
                    bi.setQty(pbi.getQty());
                    bi.setRate(pbi.getPurchaseRate());
                }
            } else {
                // Preserve user-entered quantity but update rate if needed
                if (bi.getItem() instanceof Ampp) {
                    bi.setRate(pbi.getPurchaseRatePack());
                } else if (bi.getItem() instanceof Amp) {
                    bi.setRate(pbi.getPurchaseRate());
                }
            }

            bi.setSearialNo(serialNo++);
            // Use net rate, not gross rate for netValue
            double netValue = bi.getQty() * bi.getNetRate();
            bi.setNetValue(-netValue);

            if (f != null) {
                BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal qtyTotal = qty.add(freeQty);

                BigDecimal costRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
                BigDecimal purchaseRate = Optional.ofNullable(f.getLineGrossRate()).orElse(BigDecimal.ZERO);
                BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO);
                BigDecimal wholesaleRate = Optional.ofNullable(f.getWholesaleRate()).orElse(BigDecimal.ZERO);

                BigDecimal retailValue = retailRate.multiply(qtyTotal);
                BigDecimal wholesaleValue = wholesaleRate.multiply(qtyTotal);
                BigDecimal freeItemValue = costRate.multiply(freeQty);

                // Calculate free item values
                BigDecimal freeItemPurchaseValue = purchaseRate.multiply(freeQty);
                BigDecimal freeItemCostValue = costRate.multiply(freeQty);
                BigDecimal freeItemRetailValue = retailRate.multiply(freeQty);

                totalLineDiscounts = totalLineDiscounts.add(Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO));
                totalLineExpenses = totalLineExpenses.add(Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO));
                totalTaxLines = totalTaxLines.add(Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO));
                totalLineCosts = totalLineCosts.add(Optional.ofNullable(f.getLineCost()).orElse(BigDecimal.ZERO));

                totalFreeItemValue = totalFreeItemValue.add(freeItemValue);
                totalPurchase = totalPurchase.add(Optional.ofNullable(f.getValueAtPurchaseRate()).orElse(BigDecimal.ZERO));
                totalRetail = totalRetail.add(retailValue);
                totalWholesale = totalWholesale.add(wholesaleValue);

                // Accumulate free item values
                totalPurchaseValueFree = totalPurchaseValueFree.add(freeItemPurchaseValue);
                totalCostValueFree = totalCostValueFree.add(freeItemCostValue);
                totalRetailSaleValueFree = totalRetailSaleValueFree.add(freeItemRetailValue);

                totalQty = totalQty.add(qty);
                totalFreeQty = totalFreeQty.add(freeQty);
                totalQtyAtomic = totalQtyAtomic.add(Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO));
                totalFreeQtyAtomic = totalFreeQtyAtomic.add(Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO));

                grossTotal = grossTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
                lineGrossTotal = lineGrossTotal.add(Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO));
                netTotal = netTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                lineNetTotal = lineNetTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));

                totalDiscount = totalDiscount.add(Optional.ofNullable(f.getTotalDiscount()).orElse(BigDecimal.ZERO));
                totalExpense = totalExpense.add(Optional.ofNullable(f.getTotalExpense()).orElse(BigDecimal.ZERO));
                totalCost = totalCost.add(Optional.ofNullable(f.getTotalCost()).orElse(BigDecimal.ZERO));
                totalTax = totalTax.add(Optional.ofNullable(f.getTotalTax()).orElse(BigDecimal.ZERO));
            }
        }

        // Calculate bill expenses total ONLY for expenses considered for costing
        double currentBillExpensesConsideredForCosting = 0.0;
        if (bill.getBillExpenses() != null && !bill.getBillExpenses().isEmpty()) {
            for (com.divudi.core.entity.BillItem expense : bill.getBillExpenses()) {
                // Skip retired expenses for consistency
                if (expense.isRetired()) {
                    continue;
                }
                // Only include expenses that are considered for costing in net total
                if (expense.isConsideredForCosting()) {
                    currentBillExpensesConsideredForCosting += expense.getNetValue();
                } else {
                }
            }
        }

        netTotal = lineNetTotal.abs().add(billTax.abs()).add(BigDecimal.valueOf(currentBillExpensesConsideredForCosting).abs()).subtract(billDiscount.abs());

        bill.setTotal(lineNetTotal.doubleValue());
        bill.setNetTotal(netTotal.doubleValue());
        bill.setSaleValue(totalRetail.doubleValue());

        // Ensure BillFinanceDetails is present
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }

        // Set calculated values
        bfd.setBillDiscount(billDiscount);
        bfd.setBillExpense(billExpense);
        bfd.setBillTaxValue(billTax);
        bfd.setBillCostValue(billCost);

        bfd.setLineDiscount(totalLineDiscounts);
        bfd.setLineExpense(totalLineExpenses);
        bfd.setItemTaxValue(totalTaxLines);
        bfd.setLineCostValue(totalLineCosts);

        bfd.setTotalDiscount(totalLineDiscounts.add(billDiscount));
        bfd.setTotalExpense(totalLineExpenses.add(billExpense));
        bfd.setTotalTaxValue(totalTaxLines.add(billTax));
        bfd.setTotalCostValue(totalLineCosts);

        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalPurchaseValue(totalPurchase);
        bfd.setTotalRetailSaleValue(totalRetail);
        bfd.setTotalWholesaleValue(totalWholesale);

        // Set free item values
        bfd.setTotalPurchaseValueFree(totalPurchaseValueFree);
        bfd.setTotalCostValueFree(totalCostValueFree);
        bfd.setTotalRetailSaleValueFree(totalRetailSaleValueFree);

        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);

        bfd.setGrossTotal(lineGrossTotal);
        bfd.setLineGrossTotal(lineGrossTotal);
        bfd.setNetTotal(netTotal);
        bfd.setLineNetTotal(lineNetTotal);

    }

    // ================= Methods copied from PharmacyCalculation =================
    public List<Item> getSuggessionOnly(Item item) {
        List<Item> suggessions = new ArrayList<>();

        if (item instanceof Amp) {
            suggessions = findPack((Amp) item);
            suggessions.add(item);
        } else if (item instanceof Ampp) {
            Amp amp = ((Ampp) item).getAmp();
            suggessions = findPack(amp);
            suggessions.add(amp);
        }

        return suggessions;
    }

    public List<Item> findPack(Amp amp) {
        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT i from Ampp i where i.retired=false and "
                + " i.amp=:am";

        hm.put("am", amp);

        return getItemFacade().findByJpql(sql, hm);
    }

    /**
     * Calculates the remaining quantity of items from a purchase order. Initial
     * quantity is equal to the ordered quantity. GRNs (Goods Received Notes)
     * reduce the remaining quantity. Cancelled GRNs increase the remaining
     * quantity. GRN Returns and GRN Return Cancellations are not considered.
     *
     * @param po The pharmaceutical bill item linked to the purchase order.
     * @return Remaining quantity from the original order.
     */
    public double calculateRemainigQtyFromOrder(PharmaceuticalBillItem po) {
        double billed = getTotalQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN);
        double cancelled = getTotalQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN_CANCELLED);
        double recieveNet = Math.abs(billed) - Math.abs(cancelled);
        return Math.abs(recieveNet);
    }

    /**
     * Calculates the remaining free quantity of items from a purchase order.
     * Initial quantity is equal to the free quantity in the order. GRNs reduce
     * the remaining free quantity. Cancelled GRNs increase the remaining free
     * quantity. GRN Returns and GRN Return Cancellations are not considered.
     *
     * @param po The pharmaceutical bill item linked to the purchase order.
     * @return Remaining free quantity from the original order.
     */
    public double calculateRemainingFreeQtyFromOrder(PharmaceuticalBillItem po) {
        double billed = getTotalFreeQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN);
        double cancelled = getTotalFreeQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN_CANCELLED);
        double recieveNet = Math.abs(billed) - Math.abs(cancelled);
        return (Math.abs(recieveNet));
    }

    public double getTotalQty(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  type(p.bill)=:class and p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    // ChatGPT contribution (2025-09-10): Treat null retired as active; make SUM null-safe.
    public double getTotalQty(BillItem pobi, BillTypeAtomic billTypeAtomic) {
        String sql = "Select COALESCE(SUM(COALESCE(bi.pharmaceuticalBillItem.qty,0)),0) "
                + " from BillItem bi "
                + " where (bi.retired=false or bi.retired is null) "
                + " and (bi.bill.retired=false or bi.bill.retired is null) "
                + " and bi.referanceBillItem=:pobi "
                + " and bi.bill.billTypeAtomic=:bta";
        Map<String, Object> hm = new HashMap<>();
        hm.put("pobi", pobi);
        hm.put("bta", billTypeAtomic);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getTotalFreeQty(BillItem pobi, BillTypeAtomic billTypeAtomic) {
        String sql = "Select COALESCE(SUM(COALESCE(bi.pharmaceuticalBillItem.freeQty,0)),0) "
                + " from BillItem bi "
                + " where (bi.retired=false or bi.retired is null) "
                + " and (bi.bill.retired=false or bi.bill.retired is null) "
                + " and bi.referanceBillItem=:pobi "
                + " and bi.bill.billTypeAtomic=:bta";
        Map<String, Object> hm = new HashMap<>();
        hm.put("pobi", pobi);
        hm.put("bta", billTypeAtomic);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getTotalFreeQty(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "  type(p.bill)=:class and p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getRemainingQty(PharmaceuticalBillItem ph) {
        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id = " + ph.getBillItem().getReferanceBillItem().getId();
        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);

        double poQty, remainsFree;
        poQty = po.getQtyInUnit();
        remainsFree = poQty - calculateRemainingFreeQtyFromOrder(po);

        return remainsFree;
    }

    public ItemBatch saveItemBatch(BillItem tmp) {
        ItemBatch itemBatch = new ItemBatch();
        Item itm = tmp.getItem();

        if (itm instanceof Ampp) {
            itm = ((Ampp) itm).getAmp();
        }

        double purchase = tmp.getPharmaceuticalBillItem().getPurchaseRateInUnit();
        double retail = tmp.getPharmaceuticalBillItem().getRetailRateInUnit();
        double wholesale = tmp.getPharmaceuticalBillItem().getWholesaleRate();

        itemBatch.setDateOfExpire(tmp.getPharmaceuticalBillItem().getDoe());
        itemBatch.setBatchNo(tmp.getPharmaceuticalBillItem().getStringValue());
        itemBatch.setPurcahseRate(purchase);
        itemBatch.setRetailsaleRate(retail);
        itemBatch.setWholesaleRate(wholesale);
        itemBatch.setLastPurchaseBillItem(tmp);
        HashMap hash = new HashMap();
        String sql;

        itemBatch.setItem(itm);
        sql = "Select p from ItemBatch p where  p.item=:itm "
                + " and p.dateOfExpire= :doe and p.retailsaleRate=:ret "
                + " and p.purcahseRate=:pur";

        hash.put("doe", itemBatch.getDateOfExpire());
        hash.put("itm", itemBatch.getItem());
        hash.put("ret", itemBatch.getRetailsaleRate());
        hash.put("pur", itemBatch.getPurcahseRate());
        List<ItemBatch> i = getItemBatchFacade().findByJpql(sql, hash, TemporalType.TIMESTAMP);

        if (!i.isEmpty()) {
            itemBatch.setMake(tmp.getPharmaceuticalBillItem().getMake());
            itemBatch.setModal(tmp.getPharmaceuticalBillItem().getModel());
            ItemBatch ib = i.get(0);
            ib.setWholesaleRate(wholesale);
            getItemBatchFacade().edit(ib);
            return ib;
        } else {
            itemBatch.setMake(tmp.getPharmaceuticalBillItem().getMake());
            itemBatch.setModal(tmp.getPharmaceuticalBillItem().getModel());
            getItemBatchFacade().create(itemBatch);
        }

        return itemBatch;
    }

    private ItemBatch fetchItemBatchWithCosting(Item item, double purchaseRate, double retailRate, double costRate, Date dateOfExpiry) {
        String jpql = "SELECT p FROM ItemBatch p "
                + "WHERE p.retired = false "
                + "AND p.item = :itm "
                + "AND p.dateOfExpire = :doe "
                + "AND p.retailsaleRate = :ret "
                + "AND p.costRate = :cr "
                + "AND p.purcahseRate = :pur";

        Map<String, Object> params = new HashMap<>();
        params.put("itm", item);
        params.put("doe", dateOfExpiry);
        params.put("ret", retailRate);
        params.put("cr", costRate);
        params.put("pur", purchaseRate);

        return getItemBatchFacade().findFirstByJpql(jpql, params, TemporalType.DATE);
    }

    private ItemBatch fetchItemBatchWithoutCosting(Item item, double purchaseRate, double retailRate, Date dateOfExpiry) {
        String jpql = "SELECT p FROM ItemBatch p "
                + "WHERE p.retired = false "
                + "AND p.item = :itm "
                + "AND p.dateOfExpire = :doe "
                + "AND p.retailsaleRate = :ret "
                + "AND p.purcahseRate = :pur";

        Map<String, Object> params = new HashMap<>();
        params.put("itm", item);
        params.put("doe", dateOfExpiry);
        params.put("ret", retailRate);
        params.put("pur", purchaseRate);

        return getItemBatchFacade().findFirstByJpql(jpql, params, TemporalType.DATE);
    }

    /**
     * Creates or fetches an existing ItemBatch based on costing and expiry
     * logic.Ensures uniqueness based on AMP, purchaseRate, retailRate,
     * costRate, and expiry.Additional fields like wholesaleRate, make, etc.,
     * are set but not used for uniqueness.
     *
     * @param inputBillItem
     * @return
     */
    public ItemBatch saveItemBatchWithCosting(BillItem inputBillItem) {
        if (inputBillItem == null || inputBillItem.getItem() == null || inputBillItem.getPharmaceuticalBillItem() == null) {
            return null;
        }

        Item originalItem = inputBillItem.getItem();
        Item amp = originalItem;
        if (amp instanceof Ampp) {
            amp = ((Ampp) amp).getAmp();
        } else {
        }

        Date expiryDate = inputBillItem.getPharmaceuticalBillItem().getDoe();
        if (expiryDate == null || amp == null) {
            return null;
        }

        ItemBatch itemBatch = null;

        double purchaseRatePerUnit;
        double retailRatePerUnit;
        double wholesaleRate = 0.0;
        double costRatePerUnit = 0.0;

        boolean manageCosting = configOptionApplicationController.getBooleanValueByKey("Manage Costing", true);

        if (manageCosting) {
            if (inputBillItem.getBillItemFinanceDetails() == null) {
                return null;
            }

            BigDecimal prGiven = inputBillItem.getBillItemFinanceDetails().getLineGrossRate();

            BigDecimal unitsPerPack = inputBillItem.getBillItemFinanceDetails().getUnitsPerPack();
            if (unitsPerPack.compareTo(BigDecimal.ZERO) <= 0) {
                unitsPerPack = BigDecimal.ONE;
            }

            BigDecimal prPerUnit = prGiven.divide(
                    unitsPerPack,
                    PRICE_SCALE,
                    RoundingMode.HALF_EVEN
            );

            purchaseRatePerUnit = prPerUnit.doubleValue();
            retailRatePerUnit = inputBillItem.getBillItemFinanceDetails().getRetailSaleRatePerUnit().doubleValue();
            costRatePerUnit = inputBillItem.getBillItemFinanceDetails().getTotalCostRate().doubleValue();

            itemBatch = fetchItemBatchWithCosting(amp, purchaseRatePerUnit, retailRatePerUnit, costRatePerUnit, expiryDate);
        } else {
            purchaseRatePerUnit = inputBillItem.getPharmaceuticalBillItem().getPurchaseRate();
            retailRatePerUnit = inputBillItem.getPharmaceuticalBillItem().getRetailRateInUnit();
            wholesaleRate = inputBillItem.getPharmaceuticalBillItem().getWholesaleRate();

            itemBatch = fetchItemBatchWithoutCosting(amp, purchaseRatePerUnit, retailRatePerUnit, expiryDate);
        }

        if (itemBatch == null) {
            itemBatch = new ItemBatch();
            itemBatch.setItem(amp);
            itemBatch.setDateOfExpire(expiryDate);
            itemBatch.setBatchNo(inputBillItem.getPharmaceuticalBillItem().getStringValue());
            itemBatch.setPurcahseRate(purchaseRatePerUnit);
            itemBatch.setRetailsaleRate(retailRatePerUnit);
            itemBatch.setWholesaleRate(wholesaleRate);
            itemBatch.setCostRate(costRatePerUnit);
            itemBatch.setLastPurchaseBillItem(inputBillItem);
            itemBatch.setMake(inputBillItem.getPharmaceuticalBillItem().getMake());
            itemBatch.setModal(inputBillItem.getPharmaceuticalBillItem().getModel());

            getItemBatchFacade().create(itemBatch);
        } else {
        }

        return itemBatch;
    }

    public void editBillItem(PharmaceuticalBillItem i, WebUser w) {
        i.getBillItem().setNetValue(0 - (i.getQty() * i.getPurchaseRate()));

        i.getBillItem().setCreatedAt(Calendar.getInstance().getTime());
        i.getBillItem().setCreater(w);
        i.getBillItem().setPharmaceuticalBillItem(i);

        getBillItemFacade().edit(i.getBillItem());
    }

}
