/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PharmacyItemData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.service.pharmacy.PharmacyCostingService;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Optional;
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

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class DirectPurchaseReturnController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    PharmacyCostingService pharmacyCostingService;

    /**
     * Controllers
     */
    @Inject
    PharmacyCalculation pharmacyCalculation;
    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    /**
     * Properties
     *
     */

    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
    private List<BillItem> billItems;

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new BilledBill();
            returnBill.setBillType(BillType.PurchaseReturn);
            returnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
        }
        return returnBill;
    }

    public void setReturnBill(Bill returnBill) {
        this.returnBill = returnBill;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    @Inject
    private PharmacyCalculation pharmacyRecieveBean;

    private double getRemainingQty(BillItem bilItem) {
        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
            double originalQty = bilItem.getQty();
            double originalFreeQty = 0.0;
            if (bilItem.getPharmaceuticalBillItem() != null) {
                originalFreeQty = bilItem.getPharmaceuticalBillItem().getFreeQty();
            }
            double returnedTotal = getPharmacyRecieveBean().getTotalQtyWithFreeQty(bilItem, BillType.PurchaseReturn, new BilledBill());
            return originalQty + originalFreeQty - Math.abs(returnedTotal);
        } else {
            String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                    + "   p.creater is not null and"
                    + " p.referanceBillItem=:bt and p.bill.billType=:btp";

            HashMap hm = new HashMap();
            hm.put("bt", bilItem);
            hm.put("btp", BillType.PurchaseReturn);

            return bilItem.getQty() + getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
        }
    }

    private void addDataToReturningBillItem(BillItem returningBillItem) {
        if (returningBillItem == null) {
            return;
        }
        BillItem originalBillItem = returningBillItem.getReferanceBillItem();
        if (originalBillItem == null) {
            return;
        }
        BillItemFinanceDetails bifdOriginal = originalBillItem.getBillItemFinanceDetails();
        if (bifdOriginal == null) {
            return;
        }
        PharmaceuticalBillItem pbiOriginal = originalBillItem.getPharmaceuticalBillItem();
        if (pbiOriginal == null) {
            return;
        }
        BillItemFinanceDetails bifdReturning = returningBillItem.getBillItemFinanceDetails();
        if (bifdReturning == null) {
            return;
        }
        PharmaceuticalBillItem pbiReturning = returningBillItem.getPharmaceuticalBillItem();
        if (pbiReturning == null) {
            return;
        }

        BigDecimal alreadyReturnQuentity = BigDecimal.ZERO;
        BigDecimal alreadyReturnedFreeQuentity = BigDecimal.ZERO;
        BigDecimal allreadyReturnedTotalQuentity = BigDecimal.ZERO;

        BigDecimal returningRate = BigDecimal.ZERO;

        String sql = "Select sum(b.billItemFinanceDetails.quantity), sum(b.billItemFinanceDetails.freeQuantity) "
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.bill.retired=false "
                + " and b.referanceBillItem=:obi "
                + " and b.bill.billTypeAtomic=:bta";

        Map<String, Object> params = new HashMap<>();
        params.put("obi", originalBillItem);
        params.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);

        Object[] returnedValues = getBillItemFacade().findSingleAggregate(sql, params);

        if (returnedValues != null) {
            alreadyReturnQuentity = returnedValues[0] == null ? BigDecimal.ZERO : new BigDecimal(returnedValues[0].toString());
            alreadyReturnedFreeQuentity = returnedValues[1] == null ? BigDecimal.ZERO : new BigDecimal(returnedValues[1].toString());
            allreadyReturnedTotalQuentity = alreadyReturnQuentity.add(alreadyReturnedFreeQuentity);
        }

        bifdOriginal.setReturnQuantity(alreadyReturnQuentity);
        bifdOriginal.setReturnFreeQuantity(alreadyReturnedFreeQuentity);

        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
            // Returning free quantity is not tracked separately.
            // The total return covers both original quantity and free quantity.
            // Here, free quantity being returned is considered zero.
            // Need to subtract already returned total (qty + free) from original total (qty + free)
            BigDecimal originalTotal = bifdOriginal.getQuantity().add(bifdOriginal.getFreeQuantity());
            BigDecimal returnedTotal = alreadyReturnQuentity.add(alreadyReturnedFreeQuentity);
            BigDecimal remaining = originalTotal.subtract(returnedTotal);
            bifdReturning.setQuantity(remaining);
            bifdReturning.setFreeQuantity(BigDecimal.ZERO);
        } else {
            // Returning quantity and free quantity are managed separately.
            // Subtract already returned quantity and free quantity from respective originals.
            bifdReturning.setQuantity(bifdOriginal.getQuantity().subtract(alreadyReturnQuentity));
            bifdReturning.setFreeQuantity(bifdOriginal.getFreeQuantity().subtract(alreadyReturnedFreeQuentity));
        }

        returningRate = getReturnRate(originalBillItem);
        bifdReturning.setGrossRate(returningRate);

        billItemFacade.edit(originalBillItem);
    }

    private double getRemainingFreeQty(BillItem bilItem) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "   p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", bilItem);
        hm.put("btp", BillType.PurchaseReturn);

        double originalFreeQty = 0.0;
        if (bilItem.getPharmaceuticalBillItem() != null) {
            originalFreeQty = bilItem.getPharmaceuticalBillItem().getFreeQty();
        }

        return originalFreeQty + getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public void onEdit(BillItem tmp) {
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();

        double remain = getRemainingQty(tmp.getReferanceBillItem());
        if (tmp.getQty() > remain) {
            tmp.setQty(remain);
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
        }

        if (!configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
            double remainFree = getRemainingFreeQty(tmp.getReferanceBillItem());
            if (tmp.getPharmaceuticalBillItem().getFreeQty() > remainFree) {
                tmp.getPharmaceuticalBillItem().setFreeQty(remainFree);
                JsfUtil.addErrorMessage("You cant return over than ballanced Free Qty ");
            }
        }

        calTotal();
        getPharmacyController().setPharmacyItem(tmp.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    public void resetValuesForReturn() {
        bill = null;
        returnBill = null;
        printPreview = false;
        billItems = null;
    }

    private void saveReturnBill() {
        getReturnBill().setInvoiceDate(getBill().getInvoiceDate());
        getReturnBill().setReferenceBill(getBill());
        getReturnBill().setToInstitution(getBill().getFromInstitution());
        getReturnBill().setToDepartment(getBill().getFromDepartment());
        getReturnBill().setFromInstitution(getBill().getToInstitution());
        getReturnBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PurchaseReturn, BillClassType.BilledBill, BillNumberSuffix.PURRET));
        getReturnBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PurchaseReturn, BillClassType.BilledBill, BillNumberSuffix.PURRET));

        getReturnBill().setInstitution(getSessionController().getInstitution());
        getReturnBill().setDepartment(getSessionController().getDepartment());
        // getReturnBill().setReferenceBill(getBill());
        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    public BigDecimal getReturnRate(BillItem originalBillItem) {
        BigDecimal rate = originalBillItem.getBillItemFinanceDetails().getGrossRate();
        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Line Cost Rate", false)
                && originalBillItem.getBillItemFinanceDetails() != null
                && originalBillItem.getBillItemFinanceDetails().getLineCostRate() != null) {
            rate = originalBillItem.getBillItemFinanceDetails().getLineCostRate();
        } else if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Total Cost Rate", false)
                && originalBillItem.getBillItemFinanceDetails() != null
                && originalBillItem.getBillItemFinanceDetails().getTotalCostRate() != null) {
            rate = originalBillItem.getBillItemFinanceDetails().getTotalCostRate();
        }
        return rate;
    }

    public String getReturnRateLabel() {
        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Line Cost Rate", false)) {
            return "Line Cost Rate";
        }
        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Total Cost Rate", false)) {
            return "Total Cost Rate";
        }
        return "Purchase Rate";
    }

    private void saveComponent() {
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQtyInUnit(0 - i.getQty());

            if (i.getPharmaceuticalBillItem().getQtyInUnit() == 0.0) {
                continue;
            }

            double rate = getReturnRate(i).doubleValue();
            i.setNetValue(i.getPharmaceuticalBillItem().getQtyInUnit() * rate);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPharmaceuticalBillItem = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            tmpPharmaceuticalBillItem.setBillItem(i);

            if (tmpPharmaceuticalBillItem.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPharmaceuticalBillItem);
            }

            i.setPharmaceuticalBillItem(tmpPharmaceuticalBillItem);
            getBillItemFacade().edit(i);

            boolean returnFlag = getPharmacyBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            if (!returnFlag) {
                i.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                getBillItemFacade().edit(i);
            }

            getReturnBill().getBillItems().add(i);
        }

    }

    private void saveComponent(Payment p) {
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQtyInUnit(0 - i.getQty());
            i.getPharmaceuticalBillItem().setFreeQtyInUnit(0 - i.getPharmaceuticalBillItem().getFreeQty());

            if (i.getPharmaceuticalBillItem().getQtyInUnit() == 0.0) {
                continue;
            }

            double rate = getReturnRate(i).doubleValue();
            i.setNetValue(i.getPharmaceuticalBillItem().getQtyInUnit() * rate);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPharmaceuticalBillItem = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            tmpPharmaceuticalBillItem.setBillItem(i);

            if (tmpPharmaceuticalBillItem.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPharmaceuticalBillItem);
            }

            i.setPharmaceuticalBillItem(tmpPharmaceuticalBillItem);
            getBillItemFacade().edit(i);

            boolean returnFlag = getPharmacyBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            if (!returnFlag) {
                i.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                getBillItemFacade().edit(i);
            }

            saveBillFee(i, p);

            getReturnBill().getBillItems().add(i);
        }

    }

    private boolean checkStock(PharmaceuticalBillItem pharmaceuticalBillItem) {
        double stockQty = getPharmacyBean().getStockQty(pharmaceuticalBillItem.getItemBatch(), getBill().getDepartment());

        if (pharmaceuticalBillItem.getQtyInUnit() > stockQty) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkGrnItems() {
        for (BillItem bi : getBillItems()) {
            bi.getPharmaceuticalBillItem().setQty(bi.getQty());
            if (checkStock(bi.getPharmaceuticalBillItem())) {
                return true;
            }
        }

        return false;
    }

    public void settle() {
        if (checkGrnItems()) {
            JsfUtil.addErrorMessage("Items for this GRN Already issued so you can't cancel ");
            return;

        }
        saveReturnBill();
        Payment p = createPayment(getReturnBill(), getReturnBill().getPaymentMethod());
        saveComponent(p);

        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
        returnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
        getBillFacade().edit(getReturnBill());
        getBillFacade().edit(getBill());

        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");

    }

    private void calTotal() {
        double grossTotal = 0.0;

        for (BillItem p : getBillItems()) {
            double rate = getReturnRate(p).doubleValue();
            grossTotal += rate * p.getQty();

        }

        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(grossTotal);

        //  return grossTotal;
    }

    public void prepareReturnBill() {
        System.out.println("prepareReturnBill");
        if (bill == null) {
            JsfUtil.addErrorMessage("No Direct Purcchase is selected to return");
            return;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type for the selected Bill to return. Its a system error. Please contact Developers");
            return;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_DIRECT_PURCHASE) {
            JsfUtil.addErrorMessage("Wrong Bill Type for the selected Bill to return. Its a system error. Please contact Developers");
            return;
        }
        getReturnBill();
        getReturnBill().setBilledBill(bill);
        prepareBillItems(bill);
    }

    private void prepareBillItems(Bill bill) {
        System.out.println("prepareBillItems");
        System.out.println("getBill() = " + bill);
        if (bill == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        if (bill.getId() == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        String jpql = "Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + bill.getId();
        List<PharmaceuticalBillItem> pbisOfBilledBill = getPharmaceuticalBillItemFacade().findByJpql(jpql);
        System.out.println("pbisOfBilledBill = " + pbisOfBilledBill);
        for (PharmaceuticalBillItem pbiOfBilledBill : pbisOfBilledBill) {
            System.out.println("i = " + pbiOfBilledBill);
            BillItem newBillItemInReturnBill = new BillItem();
            newBillItemInReturnBill.copy(pbiOfBilledBill.getBillItem());
            newBillItemInReturnBill.setQty(0.0);
            newBillItemInReturnBill.setBill(getReturnBill());
            newBillItemInReturnBill.setItem(pbiOfBilledBill.getBillItem().getItem());
            newBillItemInReturnBill.setReferanceBillItem(pbiOfBilledBill.getBillItem());
            newBillItemInReturnBill.setBill(returnBill);

            BigDecimal alreadyReturnQuentity = BigDecimal.ZERO;
            BigDecimal alreadyReturnedFreeQuentity = BigDecimal.ZERO;
            BigDecimal allreadyReturnedTotalQuentity = BigDecimal.ZERO;

            BigDecimal availableToReturnQuentity = BigDecimal.ZERO;
            BigDecimal availableToReturnFreeQuentity = BigDecimal.ZERO;
            BigDecimal availableToReturnTotalQuentity = BigDecimal.ZERO;

            BigDecimal returningRate = BigDecimal.ZERO;
            BigDecimal returningValue = BigDecimal.ZERO;
            BigDecimal returningFreeValue = BigDecimal.ZERO;
            BigDecimal returningTotalValue = BigDecimal.ZERO;

            PharmaceuticalBillItem newPharmaceuticalBillItemInReturnBill = new PharmaceuticalBillItem();
            newPharmaceuticalBillItemInReturnBill.copy(pbiOfBilledBill);
            newPharmaceuticalBillItemInReturnBill.setBillItem(newBillItemInReturnBill);

            double originalQty = pbiOfBilledBill.getQty();
            double originalFreeQty = pbiOfBilledBill.getFreeQty();

            if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
                double returnedTotal = getPharmacyRecieveBean().getTotalQtyWithFreeQty(pbiOfBilledBill.getBillItem(), BillType.PurchaseReturn, new BilledBill());
                newPharmaceuticalBillItemInReturnBill.setQty(originalQty + originalFreeQty - Math.abs(returnedTotal));
                newPharmaceuticalBillItemInReturnBill.setFreeQty(0.0);
            } else {
                double returnedQty = getPharmacyRecieveBean().getTotalQty(pbiOfBilledBill.getBillItem(), BillType.PurchaseReturn, new BilledBill());
                double returnedFreeQty = getPharmacyRecieveBean().getTotalFreeQty(pbiOfBilledBill.getBillItem(), BillType.PurchaseReturn, new BilledBill());
                newPharmaceuticalBillItemInReturnBill.setQty(originalQty - Math.abs(returnedQty));
                newPharmaceuticalBillItemInReturnBill.setFreeQty(originalFreeQty - Math.abs(returnedFreeQty));
            }

            newBillItemInReturnBill.setPharmaceuticalBillItem(newPharmaceuticalBillItemInReturnBill);

            BillItemFinanceDetails fd = null;
            BillItemFinanceDetails originalFd = pbiOfBilledBill.getBillItem().getBillItemFinanceDetails();
            if (originalFd != null) {
                fd = originalFd.clone();
                fd.setBillItem(newBillItemInReturnBill);
            } else {
                fd = new BillItemFinanceDetails(newBillItemInReturnBill);
            }
            fd.setQuantity(BigDecimal.valueOf(newPharmaceuticalBillItemInReturnBill.getQty()));
            fd.setFreeQuantity(BigDecimal.valueOf(newPharmaceuticalBillItemInReturnBill.getFreeQty()));
            newBillItemInReturnBill.setBillItemFinanceDetails(fd);
            if (pharmacyCostingService != null) {
                pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);
            }

            addDataToReturningBillItem(newBillItemInReturnBill);

            getBillItems().add(newBillItemInReturnBill);
            calculateBillItemDetails(newBillItemInReturnBill);
        }
        System.out.println("getBillItems = " + getBillItems());
    }

    private void calculateBillItemDetails(BillItem returningBillItem){
        // user Input, must not changed
        if (returningBillItem == null) {
            return;
        }

        BillItemFinanceDetails f = returningBillItem.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = returningBillItem.getPharmaceuticalBillItem();

        if (f == null || pbi == null) {
            return;
        }

        // Values provided by the user
        BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal grossRate = Optional.ofNullable(f.getGrossRate()).orElse(BigDecimal.ZERO);

        Item item = returningBillItem.getItem();

        BigDecimal unitsPerPack;
        if (item instanceof Ampp) {
            double units = item.getDblValue();
            unitsPerPack = units > 0.0 ? BigDecimal.valueOf(units) : BigDecimal.ONE;
        } else {
            unitsPerPack = BigDecimal.ONE;
        }

        f.setUnitsPerPack(unitsPerPack);

        // Calculate derived quantities
        BigDecimal qtyUnits = qty.multiply(unitsPerPack);
        BigDecimal freeQtyUnits = freeQty.multiply(unitsPerPack);

        f.setQuantityByUnits(qtyUnits);
        f.setFreeQuantityByUnits(freeQtyUnits);
        f.setTotalQuantity(qty.add(freeQty));
        f.setTotalQuantityByUnits(qtyUnits.add(freeQtyUnits));

        // Basic rates - net rate equals gross rate in return scenario
        f.setNetRate(grossRate);
        f.setLineGrossRate(grossRate);
        f.setLineNetRate(grossRate);
        f.setRetailSaleRatePerUnit(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO));
        f.setRetailSaleRate(Optional.ofNullable(f.getRetailSaleRate()).orElse(f.getRetailSaleRatePerUnit()));

        // Fill PharmaceuticalBillItem for backward compatibility
        if (item instanceof Ampp) {
            pbi.setQty(qtyUnits.doubleValue());
            pbi.setQtyPacks(qty.doubleValue());

            pbi.setFreeQty(freeQtyUnits.doubleValue());
            pbi.setFreeQtyPacks(freeQty.doubleValue());

            pbi.setPurchaseRate(grossRate.doubleValue());
            pbi.setPurchaseRatePack(grossRate.doubleValue());

            double retailRate = f.getRetailSaleRate().doubleValue();
            pbi.setRetailRate(retailRate);
            pbi.setRetailRatePack(retailRate);
            pbi.setRetailRateInUnit(f.getRetailSaleRatePerUnit().doubleValue());
        } else {
            pbi.setQty(qtyUnits.doubleValue());
            pbi.setQtyPacks(qtyUnits.doubleValue());

            pbi.setFreeQty(freeQtyUnits.doubleValue());
            pbi.setFreeQtyPacks(freeQtyUnits.doubleValue());

            pbi.setPurchaseRate(grossRate.doubleValue());
            pbi.setPurchaseRatePack(grossRate.doubleValue());

            double retailRate = f.getRetailSaleRatePerUnit().doubleValue();
            pbi.setRetailRate(retailRate);
            pbi.setRetailRatePack(retailRate);
            pbi.setRetailRateInUnit(retailRate);

            f.setRetailSaleRate(f.getRetailSaleRatePerUnit());
        }

    }
    
    public void onEditItem(PharmacyItemData tmp) {
        double pur = getPharmacyBean().getLastPurchaseRate(tmp.getPharmaceuticalBillItem().getBillItem().getItem(), tmp.getPharmaceuticalBillItem().getBillItem().getReferanceBillItem().getBill().getDepartment());
        double ret = getPharmacyBean().getLastRetailRate(tmp.getPharmaceuticalBillItem().getBillItem().getItem(), tmp.getPharmaceuticalBillItem().getBillItem().getReferanceBillItem().getBill().getDepartment());

        tmp.getPharmaceuticalBillItem().setPurchaseRateInUnit(pur);
        tmp.getPharmaceuticalBillItem().setRetailRateInUnit(ret);
        tmp.getPharmaceuticalBillItem().setLastPurchaseRateInUnit(pur);

        // onEdit(tmp);
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
        createBillFeePaymentAndPayment(bf, p);
    }

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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmaceuticalItemController getPharmaceuticalItemController() {
        return pharmaceuticalItemController;
    }

    public void setPharmaceuticalItemController(PharmaceuticalItemController pharmaceuticalItemController) {
        this.pharmaceuticalItemController = pharmaceuticalItemController;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
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

    public PharmacyCalculation getPharmacyRecieveBean() {
        return pharmacyRecieveBean;
    }

    public void setPharmacyRecieveBean(PharmacyCalculation pharmacyRecieveBean) {
        this.pharmacyRecieveBean = pharmacyRecieveBean;
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

}
