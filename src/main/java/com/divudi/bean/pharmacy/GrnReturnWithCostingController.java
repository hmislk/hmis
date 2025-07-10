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
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.itextpdf.text.pdf.BidiOrder;
import java.math.BigDecimal;
import java.io.Serializable;
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

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class GrnReturnWithCostingController implements Serializable {

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
            returnBill.setBillType(BillType.PharmacyGrnReturn);
            returnBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
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

    private double getRemainingTotalQtyToReturnByUnits(BillItem bilItem) {
        double originalQty = 0.0;
        double originalFreeQty = 0.0;
        double returnedTotal = 0.0;
        if (bilItem.getPharmaceuticalBillItem() != null) {
            originalQty = bilItem.getPharmaceuticalBillItem().getQty();
            originalFreeQty = bilItem.getPharmaceuticalBillItem().getFreeQty();
        }
        returnedTotal = Math.abs(getPharmacyRecieveBean().getQtyPlusFreeQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN));
        returnedTotal -= Math.abs(getPharmacyRecieveBean().getQtyPlusFreeQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN_CANCELLATION));
        return Math.abs(originalQty) + Math.abs(originalFreeQty) - Math.abs(returnedTotal);
    }

    private double getRemainingQtyToReturnByUnits(BillItem bilItem) {
        double originalQty = 0.0;
        double returnedTotal = 0.0;
        if (bilItem.getPharmaceuticalBillItem() != null) {
            originalQty = bilItem.getPharmaceuticalBillItem().getQty();
        }
        returnedTotal = Math.abs(getPharmacyRecieveBean().getQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN));
        returnedTotal -= Math.abs(getPharmacyRecieveBean().getQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN_CANCELLATION));
        return Math.abs(originalQty) - Math.abs(returnedTotal);
    }

    private double getRemainingFreeQtyToReturnByUnits(BillItem bilItem) {
        double originalQty = 0.0;
        double originalFreeQty = 0.0;
        double returnedTotal = 0.0;
        if (bilItem.getPharmaceuticalBillItem() != null) {
            originalQty = bilItem.getPharmaceuticalBillItem().getQty();
            originalFreeQty = bilItem.getPharmaceuticalBillItem().getFreeQty();
        }
        returnedTotal = Math.abs(getPharmacyRecieveBean().getFreeQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN));
        returnedTotal -= Math.abs(getPharmacyRecieveBean().getFreeQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN_CANCELLATION));
        return Math.abs(originalQty) + Math.abs(originalFreeQty) - Math.abs(returnedTotal);
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
        PharmaceuticalBillItem pbiOriginal = originalBillItem.getPharmaceuticalBillItem();
        BillItemFinanceDetails bifdReturning = returningBillItem.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbiReturning = returningBillItem.getPharmaceuticalBillItem();

        if (bifdOriginal == null || bifdReturning == null || pbiOriginal == null || pbiReturning == null) {
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
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

        Object[] returnedValues = getBillItemFacade().findSingleAggregate(sql, params);

        if (returnedValues != null) {
            alreadyReturnQuentity = safeToBigDecimal(returnedValues[0]);
            alreadyReturnedFreeQuentity = safeToBigDecimal(returnedValues[1]);
            allreadyReturnedTotalQuentity = alreadyReturnQuentity.add(alreadyReturnedFreeQuentity);
        }

        bifdOriginal.setReturnQuantity(alreadyReturnQuentity);
        bifdOriginal.setReturnFreeQuantity(alreadyReturnedFreeQuentity);

        BigDecimal originalQty = safeToBigDecimal(bifdOriginal.getQuantity());
        BigDecimal originalFreeQty = safeToBigDecimal(bifdOriginal.getFreeQuantity());

        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
            BigDecimal originalTotal = originalQty.add(originalFreeQty);
            BigDecimal returnedTotal = alreadyReturnQuentity.add(alreadyReturnedFreeQuentity);
            BigDecimal remaining = originalTotal.subtract(returnedTotal);
            bifdReturning.setQuantity(remaining);
            bifdReturning.setFreeQuantity(BigDecimal.ZERO);
        } else {
            bifdReturning.setQuantity(originalQty.subtract(alreadyReturnQuentity));
            bifdReturning.setFreeQuantity(originalFreeQty.subtract(alreadyReturnedFreeQuentity));
        }

        returningRate = getReturnRateForUnits(originalBillItem);
        if (returningRate != null) {
            bifdReturning.setLineGrossRate(returningRate);
        }

        billItemFacade.edit(originalBillItem);
    }

    private BigDecimal safeToBigDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private double getRemainingFreeQty(BillItem bilItem) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "   p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", bilItem);
        hm.put("btp", BillType.PharmacyGrnReturn);

        double originalFreeQty = 0.0;
        if (bilItem.getPharmaceuticalBillItem() != null) {
            originalFreeQty = bilItem.getPharmaceuticalBillItem().getFreeQty();
        }

        return originalFreeQty + getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public void onEdit(BillItem editingBillItem) {
        editingBillItem.getBillItemFinanceDetails().setLineGrossTotal(editingBillItem.getBillItemFinanceDetails().getTotalQuantity().multiply(editingBillItem.getBillItemFinanceDetails().getLineGrossRate()));
    }

    public void onEditOld(BillItem editingBillItem) {
        System.out.println("onEdit");
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();

        double remainngTotalQty = getRemainingTotalQtyToReturnByUnits(editingBillItem.getReferanceBillItem());
        double remainngQty = getRemainingQtyToReturnByUnits(editingBillItem.getReferanceBillItem());
        double remainngFreeQty = getRemainingFreeQtyToReturnByUnits(editingBillItem.getReferanceBillItem());

        pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(editingBillItem.getPharmaceuticalBillItem(), editingBillItem.getBillItemFinanceDetails());
        editingBillItem.setQty(editingBillItem.getBillItemFinanceDetails().getQuantity().doubleValue());
        System.out.println("remainngTotalQty = " + remainngTotalQty);
        System.out.println("remainngQty = " + remainngQty);
        System.out.println("remainngFreeQty = " + remainngFreeQty);
        System.out.println("tmp.getQty() = " + editingBillItem.getQty());

        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
            if (editingBillItem.getQty() > remainngTotalQty) {
                editingBillItem.setQty(remainngTotalQty);
                JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
            }
        } else if (!configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
            if (editingBillItem.getPharmaceuticalBillItem().getFreeQty() > remainngFreeQty && editingBillItem.getPharmaceuticalBillItem().getQty() > remainngQty) {
                editingBillItem.getPharmaceuticalBillItem().setFreeQty(remainngFreeQty);
                editingBillItem.getPharmaceuticalBillItem().setQty(remainngQty);
                JsfUtil.addErrorMessage("You cant return over than ballanced Free Qty ");
            } else if (editingBillItem.getPharmaceuticalBillItem().getQty() > remainngQty) {
                editingBillItem.getPharmaceuticalBillItem().setQty(remainngQty);
                JsfUtil.addErrorMessage("You cant return over than ballanced Free Qty ");
            } else if (editingBillItem.getPharmaceuticalBillItem().getFreeQty() > remainngFreeQty) {
                editingBillItem.getPharmaceuticalBillItem().setFreeQty(remainngFreeQty);
                JsfUtil.addErrorMessage("You cant return over than ballanced Free Qty ");
            }
        }

        pharmacyCostingService.addBillItemFinanceDetailQuantitiesFromPharmaceuticalBillItem(editingBillItem.getPharmaceuticalBillItem(), editingBillItem.getBillItemFinanceDetails());
        editingBillItem.setQty(editingBillItem.getBillItemFinanceDetails().getQuantity().doubleValue());

        System.out.println("editingBillItem.getBillItemFinanceDetails().getQuantity() = " + editingBillItem.getBillItemFinanceDetails().getQuantity());
        System.out.println("editingBillItem.getBillItemFinanceDetails().getQuantityByUnits() = " + editingBillItem.getBillItemFinanceDetails().getQuantityByUnits());
        System.out.println("editingBillItem.getBillItemFinanceDetails().getFreeQuantity() = " + editingBillItem.getBillItemFinanceDetails().getFreeQuantity());
        System.out.println("editingBillItem.getBillItemFinanceDetails().getFreeQuantityByUnits() = " + editingBillItem.getBillItemFinanceDetails().getFreeQuantityByUnits());

        calculateBillItemDetails(editingBillItem);
        callculateBillDetails();
        calTotal();
        getPharmacyController().setPharmacyItem(editingBillItem.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    public void resetValuesForReturn() {
        bill = null;
        returnBill = null;
        printPreview = false;
        billItems = null;
    }

    private void saveReturnBill() {
        getReturnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
        getReturnBill().setInvoiceDate(getBill().getInvoiceDate());
        getReturnBill().setReferenceBill(getBill());
        getReturnBill().setToInstitution(getBill().getFromInstitution());
        getReturnBill().setToDepartment(getBill().getFromDepartment());
        getReturnBill().setFromInstitution(getBill().getToInstitution());
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN_RETURN);
        getReturnBill().setDeptId(deptId);
        getReturnBill().setInsId(deptId); // for insId also dept Id is used intentionally

        getReturnBill().setInstitution(getSessionController().getInstitution());
        getReturnBill().setDepartment(getSessionController().getDepartment());
        // getReturnBill().setReferenceBill(getBill());
        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    public BigDecimal getReturnRateForUnits(BillItem originalBillItem) {
        BillItemFinanceDetails fd = originalBillItem.getBillItemFinanceDetails();
        if (fd == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = fd.getGrossRate();
        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Line Cost Rate", false)
                && fd.getLineCostRate() != null) {
            rate = fd.getLineCostRate();
        } else if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Total Cost Rate", false)
                && fd.getTotalCostRate() != null) {
            rate = fd.getTotalCostRate();
        } else if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Purchase Rate", false)
                && fd.getLineGrossRate() != null) {
            if (originalBillItem.getItem() instanceof Ampp) {
                rate = fd.getLineGrossRate().divide(fd.getUnitsPerPack());
            }
            if (originalBillItem.getItem() instanceof Vmpp) {
                rate = fd.getLineGrossRate().divide(fd.getUnitsPerPack());
            }
            if (originalBillItem.getItem() instanceof Amp) {
                rate = fd.getLineGrossRate();
            }
            if (originalBillItem.getItem() instanceof Vmp) {
                rate = fd.getLineGrossRate();
            }

        }

        if (originalBillItem.getItem() instanceof Ampp) {
            BigDecimal upp = Optional.ofNullable(fd.getUnitsPerPack()).orElse(BigDecimal.ONE);
            if (upp.compareTo(BigDecimal.ZERO) > 0) {
                rate = rate.multiply(upp);
            }
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

//    private void saveComponent() {
//        for (BillItem i : getBillItems()) {
//            i.getPharmaceuticalBillItem().setQtyInUnit(0 - i.getQty());
//
//            if (i.getPharmaceuticalBillItem().getQtyInUnit() == 0.0) {
//                continue;
//            }
//
//            double rate = getReturnRateForUnits(i).doubleValue();
//            i.setNetValue(i.getPharmaceuticalBillItem().getQtyInUnit() * rate);
//            i.setCreatedAt(Calendar.getInstance().getTime());
//            i.setCreater(getSessionController().getLoggedUser());
//
//            PharmaceuticalBillItem tmpPharmaceuticalBillItem = i.getPharmaceuticalBillItem();
//            i.setPharmaceuticalBillItem(null);
//
//            if (i.getId() == null) {
//                getBillItemFacade().create(i);
//            }
//
//            tmpPharmaceuticalBillItem.setBillItem(i);
//
//            if (tmpPharmaceuticalBillItem.getId() == null) {
//                getPharmaceuticalBillItemFacade().create(tmpPharmaceuticalBillItem);
//            }
//
//            i.setPharmaceuticalBillItem(tmpPharmaceuticalBillItem);
//            getBillItemFacade().edit(i);
//
//            boolean returnFlag = getPharmacyBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());
//
//            if (!returnFlag) {
//                i.setTmpQty(0);
//                getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
//                getBillItemFacade().edit(i);
//            }
//
//            getReturnBill().getBillItems().add(i);
//        }
//
//    }
    // ChatGPT Contribution
    private void saveBillItems() {
        for (BillItem i : getBillItems()) {
            BillItemFinanceDetails fd = i.getBillItemFinanceDetails();
            BillItem ref = i.getReferanceBillItem();
            BillItemFinanceDetails refFd = ref != null ? ref.getBillItemFinanceDetails() : null;

            if (fd == null || refFd == null) {
                continue; // Skip if finance details are missing
            }

            PharmaceuticalBillItem pbi = i.getPharmaceuticalBillItem();

            double qty = fd.getQuantity().doubleValue();
            double freeQty = fd.getFreeQuantity().doubleValue();
            double unitsPerPack = fd.getUnitsPerPack().doubleValue();
            double rate = fd.getLineGrossRate().doubleValue();

            if (i.getItem() instanceof Ampp) {
                pbi.setQty(-qty * unitsPerPack);
                pbi.setFreeQty(-freeQty * unitsPerPack);
            } else {
                pbi.setQty(-qty);
                pbi.setFreeQty(-freeQty);
            }

            i.setNetValue(pbi.getQty() * rate);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());

            i.setPharmaceuticalBillItem(null);

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            pbi.setBillItem(i);

            if (pbi.getId() == null) {
                getPharmaceuticalBillItemFacade().create(pbi);
            }

            i.setPharmaceuticalBillItem(pbi);
            getBillItemFacade().edit(i);

            boolean returnFlag = getPharmacyBean().deductFromStock(
                    pbi.getStock(),
                    Math.abs(fd.getTotalQuantityByUnits().doubleValue()),
                    pbi,
                    getSessionController().getDepartment()
            );

            if (!returnFlag) {
                getPharmaceuticalBillItemFacade().edit(pbi);
                getBillItemFacade().edit(i);
                // TODO: Log error
            }

            saveBillFee(i);
            getBillItemFacade().editAndCommit(ref);

            BillItemFinanceDetails savedFd = ref.getBillItemFinanceDetails();

            getReturnBill().getBillItems().add(i);
        }
    }

    private void fillData() {
        double billReturnTotal = 0.0;
        double billTotalAtCostRate = 0.0;
        double billReturnTotalAtPurchaseRate = 0.0;
        double billReturnTotalAtRetailRate = 0.0;
        double billReturnTotalAtWholesaleRate = 0.0;
        double billTotalFreeValue = 0.0;

        for (BillItem bi : getBillItems()) {

            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();

            double purchaseRateOfAUnit = pbi.getItemBatch().getPurcahseRate();
            double retailRateOfAUnit = pbi.getItemBatch().getRetailsaleRate();
            double wholesaleRateOfAUnit = pbi.getItemBatch().getWholesaleRate();

            pharmacyCostingService.calculateUnitsPerPack(fd);
            pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(pbi, fd);

            fd.setLineNetRate(fd.getLineGrossRate());
            fd.setLineCostRate(BigDecimal.valueOf(pbi.getItemBatch().getCostRate()));

            fd.setLineGrossTotal(fd.getLineGrossRate().multiply(fd.getTotalQuantity()));
            fd.setLineNetTotal(fd.getLineGrossTotal());
            fd.setLineCost(fd.getLineCostRate().multiply(fd.getTotalQuantityByUnits()));
            fd.setTotalCost(fd.getLineCostRate().multiply(fd.getTotalQuantityByUnits()));

            billReturnTotal += fd.getLineGrossTotal().doubleValue();

            billTotalAtCostRate += fd.getTotalCost().doubleValue();

            billReturnTotalAtPurchaseRate += fd.getTotalQuantityByUnits().doubleValue() * purchaseRateOfAUnit;
            billReturnTotalAtRetailRate += fd.getTotalQuantityByUnits().doubleValue() * retailRateOfAUnit;
            billReturnTotalAtWholesaleRate += fd.getTotalQuantityByUnits().doubleValue() * wholesaleRateOfAUnit;
            
//            fd.getTotalQuantityByUnits();
//            fd.getFreeQuantityByUnits();
//            fd.getQuantityByUnits();

        }

        returnBill.setNetTotal(billReturnTotal);
        returnBill.setTotal(billReturnTotal);

        returnBill.getBillFinanceDetails().setLineCostValue(BigDecimal.valueOf(billTotalAtCostRate));
        returnBill.getBillFinanceDetails().setBillCostValue(BigDecimal.ZERO);
        returnBill.getBillFinanceDetails().setTotalCostValue(BigDecimal.valueOf(billTotalAtCostRate));

        returnBill.getBillFinanceDetails().setLineGrossTotal(BigDecimal.valueOf(billReturnTotal));
        returnBill.getBillFinanceDetails().setBillGrossTotal(BigDecimal.ZERO);
        returnBill.getBillFinanceDetails().setGrossTotal(BigDecimal.valueOf(billReturnTotal));

        returnBill.getBillFinanceDetails().setLineNetTotal(BigDecimal.valueOf(billReturnTotal));
        returnBill.getBillFinanceDetails().setBillNetTotal(BigDecimal.ZERO);
        returnBill.getBillFinanceDetails().setNetTotal(BigDecimal.valueOf(billReturnTotal));

        returnBill.getBillFinanceDetails().setTotalPurchaseValue(BigDecimal.valueOf(billReturnTotalAtPurchaseRate));
        returnBill.getBillFinanceDetails().setTotalRetailSaleValue(BigDecimal.valueOf(billReturnTotalAtRetailRate));
        returnBill.getBillFinanceDetails().setTotalWholesaleValue(BigDecimal.valueOf(billReturnTotalAtWholesaleRate));
        returnBill.getBillFinanceDetails().setTotalOfFreeItemValues(BigDecimal.valueOf(0.0)); // update if computed

        returnBill.getBillFinanceDetails().setTotalOfFreeItemValues(BigDecimal.ONE);

    }

    private void applyPendingReturnTotals() {
        for (BillItem i : getBillItems()) {
            BillItemFinanceDetails fd = i.getBillItemFinanceDetails();
            BillItem ref = i.getReferanceBillItem();
            BillItemFinanceDetails refFd = ref != null ? ref.getBillItemFinanceDetails() : null;

            if (fd == null || refFd == null) {
                continue;
            }

            refFd.setReturnQuantity(refFd.getReturnQuantity().add(fd.getQuantity()));
            refFd.setReturnFreeQuantity(refFd.getReturnFreeQuantity().add(fd.getFreeQuantity()));
        }
    }

    private void revertPendingReturnTotals() {
        for (BillItem i : getBillItems()) {
            BillItemFinanceDetails fd = i.getBillItemFinanceDetails();
            BillItem ref = i.getReferanceBillItem();
            BillItemFinanceDetails refFd = ref != null ? ref.getBillItemFinanceDetails() : null;

            if (fd == null || refFd == null) {
                continue;
            }

            refFd.setReturnQuantity(refFd.getReturnQuantity().subtract(fd.getQuantity()));
            refFd.setReturnFreeQuantity(refFd.getReturnFreeQuantity().subtract(fd.getFreeQuantity()));
        }
    }

    public void settleGrnReturn() {
        if (returnBill == null) {
            JsfUtil.addErrorMessage("No return bill");
            return;
        }
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (billItems == null) {
            JsfUtil.addErrorMessage("No Bill Items");
            return;
        }
        if (billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return;
        }
        if (returnBill.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a payment method please");
            return;
        }
        boolean checkByTotalQty = configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity");
        boolean checkByQtyAndFree = configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Quantity and Free Quantity");

        boolean allZero = true;

        for (BillItem returningBillItem : billItems) {
            if (returningBillItem == null || returningBillItem.getBillItemFinanceDetails() == null) {
                continue;
            }

            BigDecimal qty = Optional.ofNullable(returningBillItem.getBillItemFinanceDetails().getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal freeQty = Optional.ofNullable(returningBillItem.getBillItemFinanceDetails().getFreeQuantity()).orElse(BigDecimal.ZERO);

            if (checkByTotalQty && qty.compareTo(BigDecimal.ZERO) > 0) {
                allZero = false;
                break;
            }

            if (checkByQtyAndFree && (qty.compareTo(BigDecimal.ZERO) > 0 || freeQty.compareTo(BigDecimal.ZERO) > 0)) {
                allZero = false;
                break;
            }
        }

        if (allZero) {
            JsfUtil.addErrorMessage("No return quantities entered.");
            return;
        }

        fillData();

        applyPendingReturnTotals();
        if (getPharmacyBean().isInsufficientStockForReturn(getBillItems())) {
            revertPendingReturnTotals();
            JsfUtil.addErrorMessage("Insufficient stock available to return these items.");
            return;
        }
        if (getPharmacyBean().isReturingMoreThanPurchased(getBillItems())) {
            revertPendingReturnTotals();
            JsfUtil.addErrorMessage("Returning more than purchased.");
            return;
        }
        pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
        saveReturnBill();
        saveBillItems();
        Payment p = createPayment(getReturnBill(), getReturnBill().getPaymentMethod());

        getBillFacade().edit(getReturnBill());
        getBillFacade().edit(getBill());

        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");

    }

    private void calTotal() {
        double grossTotal = 0.0;

        for (BillItem p : getBillItems()) {
            double rate = getReturnRateForUnits(p).doubleValue();
            grossTotal += rate * p.getQty();

        }

        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(grossTotal);

        //  return grossTotal;
    }

    public void prepareReturnBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No GRN is selected to return");
            return;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type for the selected Bill to return. Its a system error. Please contact Developers");
            return;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_GRN) {
            JsfUtil.addErrorMessage("Wrong Bill Type for the selected Bill to return. Its a system error. Please contact Developers");
            return;
        }
        returnBill = null;
        getReturnBill();
        getReturnBill().setBilledBill(bill);
        prepareBillItems(bill);
    }

    private void prepareBillItems(Bill bill) {
        if (bill == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        if (bill.getId() == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        String jpql = "Select p from PharmaceuticalBillItem p where p.billItem.bill.id = :billId";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", bill.getId());
        List<PharmaceuticalBillItem> pbisOfBilledBill = getPharmaceuticalBillItemFacade().findByJpql(jpql, params);
        for (PharmaceuticalBillItem pbiOfBilledBill : pbisOfBilledBill) {
            BillItem newBillItemInReturnBill = new BillItem();
            newBillItemInReturnBill.setQty(0.0);
            newBillItemInReturnBill.setBill(getReturnBill());
            newBillItemInReturnBill.setItem(pbiOfBilledBill.getBillItem().getItem());
            newBillItemInReturnBill.setReferanceBillItem(pbiOfBilledBill.getBillItem());
            newBillItemInReturnBill.setBill(returnBill);

            PharmaceuticalBillItem newPharmaceuticalBillItemInReturnBill = new PharmaceuticalBillItem();
            newPharmaceuticalBillItemInReturnBill.setBillItem(newBillItemInReturnBill);
            newPharmaceuticalBillItemInReturnBill.setStock(pbiOfBilledBill.getStock());
            newBillItemInReturnBill.setPharmaceuticalBillItem(newPharmaceuticalBillItemInReturnBill);

            double originalQtyInUnits = pbiOfBilledBill.getQty();
            double originalFreeQtyInUnits = pbiOfBilledBill.getFreeQty();

            if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false)) {
                double returnedTotal = getPharmacyRecieveBean().getQtyPlusFreeQtyInUnits(pbiOfBilledBill.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
                newPharmaceuticalBillItemInReturnBill.setQty(Math.abs(originalQtyInUnits) + Math.abs(originalFreeQtyInUnits) - Math.abs(returnedTotal));
                newPharmaceuticalBillItemInReturnBill.setFreeQty(0.0);
            } else {
                double returnedQty = getPharmacyRecieveBean().getTotalQty(pbiOfBilledBill.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
                double returnedFreeQty = getPharmacyRecieveBean().getTotalFreeQty(pbiOfBilledBill.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
                newPharmaceuticalBillItemInReturnBill.setQty(Math.abs(originalQtyInUnits) - Math.abs(returnedQty));
                newPharmaceuticalBillItemInReturnBill.setFreeQty(Math.abs(originalFreeQtyInUnits) - Math.abs(returnedFreeQty));
            }
            BillItemFinanceDetails newBillItemFinanceDetailsInReturnBill = new BillItemFinanceDetails();
            newBillItemFinanceDetailsInReturnBill.setBillItem(newBillItemInReturnBill);
            newBillItemInReturnBill.setBillItemFinanceDetails(newBillItemFinanceDetailsInReturnBill);
            pharmacyCostingService.calculateUnitsPerPack(newBillItemFinanceDetailsInReturnBill);
            pharmacyCostingService.addBillItemFinanceDetailQuantitiesFromPharmaceuticalBillItem(newPharmaceuticalBillItemInReturnBill, newBillItemFinanceDetailsInReturnBill);
            newBillItemFinanceDetailsInReturnBill.setLineGrossRate(getReturnRateForUnits(pbiOfBilledBill.getBillItem()).multiply(newBillItemFinanceDetailsInReturnBill.getUnitsPerPack()));
            calculateLineTotalByLineGrossRate(newBillItemInReturnBill);
            getBillItems().add(newBillItemInReturnBill);
        }
        calculateTotalReturnByLineNetTotals();
    }

    private void calculateBillItemDetails(BillItem returningBillItem) {
        System.out.println("=== calculateBillItemDetails() called ===");

        if (returningBillItem == null) {
            System.out.println("returningBillItem is null");
            return;
        }

        BillItemFinanceDetails f = returningBillItem.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = returningBillItem.getPharmaceuticalBillItem();

        if (f == null) {
            System.out.println("BillItemFinanceDetails is null");
        }
        if (pbi == null) {
            System.out.println("PharmaceuticalBillItem is null");
        }
        if (f == null || pbi == null) {
            return;
        }

        if (pharmacyCostingService != null) {
            System.out.println("Calling pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem");
            pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
        } else {
            System.out.println("pharmacyCostingService is null");
        }

        BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
        System.out.println("Quantity (qty) = " + qty + ", Free Quantity (freeQty) = " + freeQty);

        if (returningBillItem.getItem() instanceof Ampp) {
            System.out.println("Item is an instance of Ampp");

            double qtyPacks = qty.doubleValue();
            pbi.setQtyPacks(qtyPacks);
            System.out.println("pbi.setQtyPacks = " + qtyPacks);

            double qtyUnits = f.getQuantityByUnits().doubleValue();
            pbi.setQty(qtyUnits);
            System.out.println("pbi.setQty = " + qtyUnits);

            double freeQtyPacks = freeQty.doubleValue();
            pbi.setFreeQtyPacks(freeQtyPacks);
            System.out.println("pbi.setFreeQtyPacks = " + freeQtyPacks);

            double freeQtyUnits = f.getFreeQuantityByUnits().doubleValue();
            pbi.setFreeQty(freeQtyUnits);
            System.out.println("pbi.setFreeQty = " + freeQtyUnits);

            double purchaseRatePack = f.getLineGrossRate().doubleValue();
            pbi.setPurchaseRatePack(purchaseRatePack);
            pbi.setPurchaseRate(purchaseRatePack);
            System.out.println("pbi.setPurchaseRatePack & setPurchaseRate = " + purchaseRatePack);

            double retailRate = f.getRetailSaleRate().doubleValue();
            pbi.setRetailRate(retailRate);
            pbi.setRetailRatePack(retailRate);
            System.out.println("pbi.setRetailRate & setRetailRatePack = " + retailRate);

            double retailRateUnit = f.getRetailSaleRatePerUnit().doubleValue();
            pbi.setRetailRateInUnit(retailRateUnit);
            System.out.println("pbi.setRetailRateInUnit = " + retailRateUnit);

        } else {
            System.out.println("Item is NOT an instance of Ampp");

            double qtyVal = qty.doubleValue();
            pbi.setQty(qtyVal);
            pbi.setQtyPacks(qtyVal);
            System.out.println("pbi.setQty & setQtyPacks = " + qtyVal);

            double freeQtyVal = freeQty.doubleValue();
            pbi.setFreeQty(freeQtyVal);
            pbi.setFreeQtyPacks(freeQtyVal);
            System.out.println("pbi.setFreeQty & setFreeQtyPacks = " + freeQtyVal);

            double purchaseRate = f.getLineGrossRate().doubleValue();
            pbi.setPurchaseRate(purchaseRate);
            pbi.setPurchaseRatePack(purchaseRate);
            System.out.println("pbi.setPurchaseRate & setPurchaseRatePack = " + purchaseRate);

            f.setRetailSaleRate(f.getRetailSaleRatePerUnit());
            double retailRate = Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue();
            pbi.setRetailRate(retailRate);
            pbi.setRetailRatePack(retailRate);
            pbi.setRetailRateInUnit(retailRate);
            System.out.println("pbi.setRetailRate, setRetailRatePack, setRetailRateInUnit = " + retailRate);
        }

        double finalQty = f.getQuantityByUnits().doubleValue();
        double finalRate = f.getLineGrossRate().doubleValue();
        returningBillItem.setQty(finalQty);
        returningBillItem.setRate(finalRate);
        System.out.println("returningBillItem.setQty = " + finalQty);
        System.out.println("returningBillItem.setRate = " + finalRate);

        System.out.println("=== End of calculateBillItemDetails() ===");
    }

    private void callculateBillDetails() {
        if (returnBill == null) {
            return;
        }

        if (billItems != null) {
            for (BillItem bi : billItems) {
                calculateBillItemDetails(bi);
            }
        }

        if (pharmacyCostingService != null) {
            pharmacyCostingService.distributeProportionalBillValuesToItems(getBillItems(), getReturnBill());
            pharmacyCostingService.calculateBillTotalsFromItemsForPurchases(getReturnBill(), getBillItems());
        }

        if (pharmacyCalculation != null) {
            pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getReturnBill());
        }
    }

    public void onReturnRateChange(BillItem bi) {
        calculateBillItemDetails(bi);
        callculateBillDetails();
    }

    public void onReturningTotalQtyChange(BillItem editingBillItem) {
        calculateLineTotalByLineGrossRate(editingBillItem);
        calculateTotalReturnByLineNetTotals();
    }

    private void calculateLineTotalByLineGrossRate(BillItem inputBillItem) {
        System.out.println("onReturningTotalQtyChange");

        if (inputBillItem == null) {
            System.out.println("editingBillItem is null");
            return;
        }

        BillItemFinanceDetails f = inputBillItem.getBillItemFinanceDetails();
        if (f == null) {
            System.out.println("BillItemFinanceDetails is null for BillItem id: " + inputBillItem.getId());
            return;
        }

        BigDecimal qty = f.getQuantity();
        BigDecimal grossRate = f.getLineGrossRate();

        System.out.println("Total Quantity: " + qty);
        System.out.println("Line Gross Rate: " + grossRate);

        if (qty == null || grossRate == null) {
            System.out.println("Cannot calculate LineGrossTotal as quantity or rate is null");
            return;
        }

        BigDecimal grossTotal = qty.multiply(grossRate);
        f.setLineGrossTotal(grossTotal);
        System.out.println("Line Gross Total set to: " + grossTotal);
    }

    private void calculateTotalReturnByLineNetTotals() {
        System.out.println("calculateTotalReturnByLineNetTotals");

        BigDecimal returnTotal = BigDecimal.ZERO;
        for (BillItem bi : billItems) {
            if (bi == null) {
                System.out.println("Encountered null BillItem in billItems list");
                continue;
            }

            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                System.out.println("BillItemFinanceDetails is null for BillItem id: " + bi.getId());
                continue;
            }

            BigDecimal lineGrossTotal = f.getLineGrossTotal();
            System.out.println("Line Gross Total for BillItem id " + bi.getId() + ": " + lineGrossTotal);

            if (lineGrossTotal != null) {
                returnTotal = returnTotal.add(lineGrossTotal);
            }
        }

        if (returnBill == null || returnBill.getBillFinanceDetails() == null) {
            System.out.println("Bill or BillFinanceDetails is null. Cannot set Net Total.");
            return;
        }

        returnBill.getBillFinanceDetails().setNetTotal(returnTotal);
        System.out.println("Net Total set to: " + returnTotal);
    }

    public void onReturningQtyChange(BillItem bi) {
        onEdit(bi);
    }

    public void onReturningFreeQtyChange(BillItem bi) {
        onEdit(bi);
    }

    // Have to have onedit methods for the following components in the  page direct_purchase_return
    // txtReturnRate, txtReturningTotalQty, txtReturningQty, txtReturningFreeQty
    // These should update bill item lelvel txtLineReturnValue and Bill Level panelReturnBillDetails
    // have to prefil 
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
//        createBillFeePaymentAndPayment(bf, p); // Retired Concept. No Loger Used
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
