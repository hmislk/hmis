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
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.itextpdf.text.pdf.BidiOrder;
import java.math.BigDecimal;
import java.io.Serializable;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
        double originalFreeQty = 0.0;
        double returnedTotal = 0.0;
        if (bilItem.getPharmaceuticalBillItem() != null) {
            originalFreeQty = bilItem.getPharmaceuticalBillItem().getFreeQty();
        }
        returnedTotal = Math.abs(getPharmacyRecieveBean().getFreeQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN));
        returnedTotal -= Math.abs(getPharmacyRecieveBean().getFreeQtyInUnits(bilItem, BillTypeAtomic.PHARMACY_GRN_RETURN_CANCELLATION));
        return Math.abs(originalFreeQty) - Math.abs(returnedTotal);
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

    public void onEditOld(BillItem editingBillItem) { //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();

        double remainngTotalQty = getRemainingTotalQtyToReturnByUnits(editingBillItem.getReferanceBillItem());
        double remainngQty = getRemainingQtyToReturnByUnits(editingBillItem.getReferanceBillItem());
        double remainngFreeQty = getRemainingFreeQtyToReturnByUnits(editingBillItem.getReferanceBillItem());

        pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(editingBillItem.getPharmaceuticalBillItem(), editingBillItem.getBillItemFinanceDetails());
        editingBillItem.setQty(editingBillItem.getBillItemFinanceDetails().getQuantity().doubleValue());

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
        if (rate == null) {
            rate = BigDecimal.ZERO;
        }
        
        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Line Cost Rate", false)
                && fd.getLineCostRate() != null) {
            rate = fd.getLineCostRate();
        } else if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Total Cost Rate", false)
                && fd.getTotalCostRate() != null) {
            rate = fd.getTotalCostRate();
        } else if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Purchase Rate", false)
                && fd.getLineGrossRate() != null) {
            if (originalBillItem.getItem() instanceof Ampp) {
                if (fd.getUnitsPerPack() != null && fd.getUnitsPerPack().compareTo(BigDecimal.ZERO) != 0) {
                    rate = fd.getLineGrossRate().divide(fd.getUnitsPerPack());
                } else {
                    rate = fd.getLineGrossRate();
                }
            } else if (originalBillItem.getItem() instanceof Vmpp) {
                if (fd.getUnitsPerPack() != null && fd.getUnitsPerPack().compareTo(BigDecimal.ZERO) != 0) {
                    rate = fd.getLineGrossRate().divide(fd.getUnitsPerPack());
                } else {
                    rate = fd.getLineGrossRate();
                }
            } else if (originalBillItem.getItem() instanceof Amp) {
                rate = fd.getLineGrossRate();
            } else if (originalBillItem.getItem() instanceof Vmp) {
                rate = fd.getLineGrossRate();
            }
        }
        return rate != null ? rate : BigDecimal.ZERO;
    }

    public BigDecimal getReturnRate(BillItem originalBillItem) {
        BillItemFinanceDetails fd = originalBillItem.getBillItemFinanceDetails();
        if (fd == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = fd.getGrossRate();
        if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Line Cost Rate", false)
                && fd.getLineCostRate() != null) {
            if (originalBillItem.getItem() instanceof Ampp) {
                rate = fd.getLineCostRate().multiply(fd.getUnitsPerPack());
            } else if (originalBillItem.getItem() instanceof Vmpp) {
                rate = fd.getLineCostRate().multiply(fd.getUnitsPerPack());
            } else if (originalBillItem.getItem() instanceof Amp) {
                rate = fd.getLineCostRate();
            } else if (originalBillItem.getItem() instanceof Vmp) {
                rate = fd.getLineCostRate();
            }
        } else if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Total Cost Rate", false)
                && fd.getTotalCostRate() != null) {
            if (originalBillItem.getItem() instanceof Ampp) {
                rate = fd.getTotalCostRate().multiply(fd.getUnitsPerPack());
            } else if (originalBillItem.getItem() instanceof Vmpp) {
                rate = fd.getTotalCostRate().multiply(fd.getUnitsPerPack());
            } else if (originalBillItem.getItem() instanceof Amp) {
                rate = fd.getTotalCostRate();
            } else if (originalBillItem.getItem() instanceof Vmp) {
                rate = fd.getTotalCostRate();
            }
        } else if (configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return Based On Purchase Rate", false)
                && fd.getLineGrossRate() != null) {
            rate = fd.getLineGrossRate();
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
// ChatGPT contributed
    private void saveBillItems() {
        List<BillItem> failedItems = new ArrayList<>();

        for (Iterator<BillItem> iterator = getBillItems().iterator(); iterator.hasNext();) {
            BillItem i = iterator.next();

            BillItemFinanceDetails fd = i.getBillItemFinanceDetails();
            BillItem referanceBillItem = i.getReferanceBillItem();
            BillItemFinanceDetails refFd = referanceBillItem != null ? referanceBillItem.getBillItemFinanceDetails() : null;

            if (fd == null || refFd == null) {
                continue; // Skip if finance details are missing
            }

            PharmaceuticalBillItem pbi = i.getPharmaceuticalBillItem();
            System.out.println("pbi.getQty() = " + pbi.getQty());
            pharmacyCostingService.makeAllQuantityValuesNegative(pbi);
            if (i.getId() == null) {
                i.setCreatedAt(new Date());
                i.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(i);
            } else {
                billItemFacade.edit(i);
            }

            System.out.println("fd.getTotalQuantityByUnits().doubleValue() = " + fd.getTotalQuantityByUnits().doubleValue());

            boolean stockUpdatedSuccessfully = getPharmacyBean().deductFromStock(
                    pbi.getStock(),
                    Math.abs(fd.getTotalQuantityByUnits().doubleValue()),
                    pbi,
                    getSessionController().getDepartment()
            );

            if (stockUpdatedSuccessfully) {
                billItemFacade.edit(i);
                saveBillFee(i);
                billItemFacade.editAndCommit(referanceBillItem);
                getReturnBill().getBillItems().add(i);
            } else {
                if (i.getId() != null) {
                    i.setRetired(true);
                    i.setRetiredAt(new Date());
                    i.setRetirer(sessionController.getLoggedUser());
                    billItemFacade.edit(i);
                } else {
                    iterator.remove(); // Remove from list if not persisted
                }
                failedItems.add(i); // Collect for logging or notification
            }
        }

        if (!failedItems.isEmpty()) {
            fillData(); // Recalculate after removing or retiring items
            billFacade.edit(returnBill);

            StringBuilder errorMessage = new StringBuilder("Stock Update Error in the following items:<br/>");
            for (BillItem failed : failedItems) {
                if (failed != null && failed.getItem() != null) {
                    errorMessage.append("- ").append(failed.getItem().getName()).append("<br/>");
                }
            }
            JsfUtil.addErrorMessage(errorMessage.toString());
        }

    }

    private static class ReturnFinanceTotals {

        double billTotalAtCostRate;
        double purchaseFree;
        double purchaseNonFree;
        double retailFree;
        double retailNonFree;
        double wholesaleFree;
        double wholesaleNonFree;
        double costFree;
        double costNonFree;
        double lineGrossTotal;
        double lineNetTotal;
    }

    private void accumulateTotals(ReturnFinanceTotals totals, BillItemFinanceDetails fd) {
        if (fd == null) {
            return;
        }

        double purchaseRate = Optional.ofNullable(fd.getLineNetRate()).orElse(BigDecimal.ZERO).doubleValue();
        double retailRate = Optional.ofNullable(fd.getRetailSaleRate()).orElse(BigDecimal.ZERO).doubleValue();
        double wholesaleRate = Optional.ofNullable(fd.getWholesaleRate()).orElse(BigDecimal.ZERO).doubleValue();
        double costRate = Optional.ofNullable(fd.getLineCostRate()).orElse(BigDecimal.ZERO).doubleValue();

        BigDecimal qtyByUnits = Optional.ofNullable(fd.getQuantityByUnits()).orElse(BigDecimal.ZERO);
        BigDecimal freeQtyByUnits = Optional.ofNullable(fd.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO);
        BigDecimal totalQtyByUnits = Optional.ofNullable(fd.getTotalQuantityByUnits()).orElse(BigDecimal.ZERO);

        totals.lineGrossTotal += Optional.ofNullable(fd.getLineGrossTotal()).orElse(BigDecimal.ZERO).doubleValue();
        totals.lineNetTotal += Optional.ofNullable(fd.getLineNetTotal()).orElse(BigDecimal.ZERO).doubleValue();

        totals.billTotalAtCostRate += costRate * totalQtyByUnits.doubleValue();

        totals.purchaseFree += freeQtyByUnits.doubleValue() * purchaseRate;
        totals.purchaseNonFree += qtyByUnits.doubleValue() * purchaseRate;

        totals.retailFree += freeQtyByUnits.doubleValue() * retailRate;
        totals.retailNonFree += qtyByUnits.doubleValue() * retailRate;

        totals.wholesaleFree += freeQtyByUnits.doubleValue() * wholesaleRate;
        totals.wholesaleNonFree += qtyByUnits.doubleValue() * wholesaleRate;

        totals.costFree += freeQtyByUnits.doubleValue() * costRate;
        totals.costNonFree += qtyByUnits.doubleValue() * costRate;
    }

    private void fillData() {
        ReturnFinanceTotals totals = new ReturnFinanceTotals();
        int serialNo = 0;

        for (BillItem bi : getBillItems()) {
            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (fd == null || pbi == null) {
                continue;
            }

            BillItem ref = bi.getReferanceBillItem();
            PharmaceuticalBillItem refPbi = ref != null ? ref.getPharmaceuticalBillItem() : null;

            Item item = bi.getItem();
            BigDecimal upp = Optional.ofNullable(fd.getUnitsPerPack()).orElse(BigDecimal.ONE);

            double purchaseRatePack = pbi.getItemBatch().getPurcahseRate();
            double retailRatePack = pbi.getItemBatch().getRetailsaleRate();
            double wholesaleRatePack = pbi.getItemBatch().getWholesaleRate();
            double costRateUnit = pbi.getItemBatch().getCostRate(); // always unit

            pharmacyCostingService.calculateUnitsPerPack(fd);
            fd.setTotalQuantity(fd.getQuantity().add(fd.getFreeQuantity()));
            pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(pbi, fd);

            fd.setLineNetRate(fd.getLineGrossRate());
            fd.setLineCostRate(BigDecimal.valueOf(costRateUnit));
            fd.setLineGrossTotal(fd.getLineGrossRate().multiply(fd.getTotalQuantity()));
            fd.setLineNetTotal(fd.getLineGrossTotal());

            fd.setGrossRate(fd.getLineGrossRate());
            fd.setNetRate(fd.getLineNetRate());
            fd.setGrossTotal(fd.getLineGrossTotal());
            fd.setNetTotal(fd.getLineNetTotal());

            if (refPbi != null && refPbi.getItemBatch() != null) {
                double refRetail = refPbi.getItemBatch().getRetailsaleRate();
                fd.setRetailSaleRate(BigDecimal.valueOf(refRetail));
                fd.setRetailSaleRatePerUnit(BigDecimal.valueOf(refRetail).divide(upp, 4, RoundingMode.HALF_UP));
                double retailValue = refRetail * fd.getTotalQuantityByUnits().doubleValue();
                pbi.setRetailValue(-Math.abs(retailValue));
            }

            accumulateTotals(totals, fd);

            double qtyInUnits = fd.getQuantityByUnits().doubleValue();
            double qtyPacks = fd.getQuantity().doubleValue();
            double lineRate = fd.getLineGrossRate().doubleValue();

            boolean isAmpp = item instanceof Ampp;
            double purchaseRateUnit = isAmpp ? purchaseRatePack / upp.doubleValue() : purchaseRatePack;
            double retailRateUnit = isAmpp ? retailRatePack / upp.doubleValue() : retailRatePack;
            double wholesaleRateUnit = isAmpp ? wholesaleRatePack / upp.doubleValue() : wholesaleRatePack;

            pbi.setPurchaseRate(purchaseRateUnit);
            pbi.setPurchaseRatePack(purchaseRatePack);
            pbi.setRetailRate(retailRateUnit);
            pbi.setRetailRatePack(retailRatePack);
            pbi.setWholesaleRate(wholesaleRateUnit);
            pbi.setWholesaleRatePack(wholesaleRatePack);

            pbi.setQty(-qtyInUnits);
            pbi.setQtyPacks(-qtyPacks);
            pbi.setPurchaseValue(pbi.getQty() * pbi.getPurchaseRate());
            pbi.setRetailPackValue(pbi.getQtyPacks() * pbi.getRetailRatePack());

            fd.setQuantity(fd.getQuantity().negate());
            fd.setFreeQuantity(fd.getFreeQuantity().negate());
            fd.setTotalQuantity(fd.getTotalQuantity().negate());
            fd.setQuantityByUnits(fd.getQuantityByUnits().negate());
            fd.setFreeQuantityByUnits(fd.getFreeQuantityByUnits().negate());
            fd.setTotalQuantityByUnits(fd.getTotalQuantityByUnits().negate());

            fd.setLineCost(fd.getLineCostRate().multiply(fd.getTotalQuantityByUnits()));
            fd.setTotalCost(fd.getLineCost());

            BigDecimal qtyAbs = fd.getTotalQuantityByUnits().abs();

            BigDecimal purchaseVal = BigDecimal.valueOf(purchaseRateUnit).multiply(qtyAbs);
            fd.setValueAtPurchaseRate(purchaseVal.negate());

            BigDecimal retailVal = BigDecimal.valueOf(retailRateUnit).multiply(qtyAbs);
            fd.setValueAtRetailRate(retailVal.negate());

            bi.setQty(pbi.getQty());
            bi.setRate(lineRate);
            bi.setNetValue(Math.abs(qtyInUnits * (isAmpp ? lineRate / upp.doubleValue() : lineRate)));
            bi.setSearialNo(serialNo++);

            if (pbi.getSerialNo() == null && pbi.getItemBatch() != null) {
                pbi.setSerialNo(pbi.getItemBatch().getSerialNo());
            }
        }

        returnBill.setNetTotal(totals.lineNetTotal);
        returnBill.setTotal(totals.lineNetTotal);

        BillFinanceDetails bfd = returnBill.getBillFinanceDetails();

        bfd.setLineCostValue(BigDecimal.valueOf(totals.billTotalAtCostRate));
        returnBill.getBillFinanceDetails().setBillCostValue(BigDecimal.ZERO);
        bfd.setTotalCostValue(BigDecimal.valueOf(totals.costFree + totals.costNonFree));
        bfd.setTotalCostValueFree(BigDecimal.valueOf(totals.costFree));
        bfd.setTotalCostValueNonFree(BigDecimal.valueOf(totals.costNonFree));

        bfd.setLineGrossTotal(BigDecimal.valueOf(totals.lineGrossTotal));
        returnBill.getBillFinanceDetails().setBillGrossTotal(BigDecimal.ZERO);
        bfd.setGrossTotal(BigDecimal.valueOf(totals.lineGrossTotal));

        bfd.setLineNetTotal(BigDecimal.valueOf(totals.lineNetTotal));
        returnBill.getBillFinanceDetails().setBillNetTotal(BigDecimal.ZERO);
        bfd.setNetTotal(BigDecimal.valueOf(totals.lineNetTotal));

        bfd.setTotalPurchaseValue(BigDecimal.valueOf(totals.purchaseFree + totals.purchaseNonFree));
        bfd.setTotalPurchaseValueFree(BigDecimal.valueOf(totals.purchaseFree));
        bfd.setTotalPurchaseValueNonFree(BigDecimal.valueOf(totals.purchaseNonFree));

        bfd.setTotalRetailSaleValue(BigDecimal.valueOf(totals.retailFree + totals.retailNonFree));
        bfd.setTotalRetailSaleValueFree(BigDecimal.valueOf(totals.retailFree));
        bfd.setTotalRetailSaleValueNonFree(BigDecimal.valueOf(totals.retailNonFree));

        bfd.setTotalWholesaleValue(BigDecimal.valueOf(totals.wholesaleFree + totals.wholesaleNonFree));
        bfd.setTotalWholesaleValueFree(BigDecimal.valueOf(totals.wholesaleFree));
        bfd.setTotalWholesaleValueNonFree(BigDecimal.valueOf(totals.wholesaleNonFree));

        returnBill.setSaleValue(totals.retailFree + totals.retailNonFree);
        returnBill.setFreeValue(totals.retailFree);
    }

    private void applyRemainingValuesInOriginalBill(List<BillItem> inputBillItems) {
        if (inputBillItems == null) {
            return;
        }
        if (inputBillItems.isEmpty()) {
            return;
        }
        for (BillItem i : inputBillItems) {
            BillItemFinanceDetails fd = i.getBillItemFinanceDetails();
            PharmaceuticalBillItem pbi = i.getPharmaceuticalBillItem();
            BillItem ref = i.getReferanceBillItem();

            if (ref == null) {
                continue;
            }

            BillItemFinanceDetails refFd = ref.getBillItemFinanceDetails();
            PharmaceuticalBillItem refPbi = ref.getPharmaceuticalBillItem();

            if (fd == null || refFd == null || pbi == null || refPbi == null) {
                continue;
            }

            refFd.setReturnQuantity(refFd.getReturnQuantity().add(fd.getQuantity()));
            refFd.setReturnFreeQuantity(refFd.getReturnFreeQuantity().add(fd.getFreeQuantity()));

            refPbi.setRemainingQty(refPbi.getRemainingQty() - pbi.getQty());
            refPbi.setRemainingQtyPack(refPbi.getRemainingQtyPack() - pbi.getQtyPacks());

            refPbi.setRemainingFreeQty(refPbi.getRemainingFreeQty() - pbi.getFreeQty());
            refPbi.setRemainingFreeQtyPack(refPbi.getRemainingFreeQtyPack() - pbi.getFreeQtyPacks());

            billItemFacade.edit(i);

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

        saveReturnBill();
        saveBillItems();
        applyRemainingValuesInOriginalBill(returnBill.getBillItems());

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
            newPharmaceuticalBillItemInReturnBill.setItemBatch(pbiOfBilledBill.getItemBatch());
            newPharmaceuticalBillItemInReturnBill.setStock(pbiOfBilledBill.getStock());
            newBillItemInReturnBill.setPharmaceuticalBillItem(newPharmaceuticalBillItemInReturnBill);

            double originalQtyInUnits = pbiOfBilledBill.getQty();
            double originalFreeQtyInUnits = pbiOfBilledBill.getFreeQty();

            boolean returnByTotalQuantity = configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return by Total Quantity", false);
            
            if (returnByTotalQuantity) {
                double returnedTotal = getPharmacyRecieveBean().getQtyPlusFreeQtyInUnits(pbiOfBilledBill.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
                double availableToReturn = Math.abs(originalQtyInUnits) + Math.abs(originalFreeQtyInUnits) - Math.abs(returnedTotal);
                newPharmaceuticalBillItemInReturnBill.setQty(availableToReturn);
                newPharmaceuticalBillItemInReturnBill.setFreeQty(0.0);
            } else {
                double returnedQty = getPharmacyRecieveBean().getTotalQty(pbiOfBilledBill.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
                double returnedFreeQty = getPharmacyRecieveBean().getTotalFreeQty(pbiOfBilledBill.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
                double availableQty = Math.abs(originalQtyInUnits) - Math.abs(returnedQty);
                double availableFreeQty = Math.abs(originalFreeQtyInUnits) - Math.abs(returnedFreeQty);
                newPharmaceuticalBillItemInReturnBill.setQty(availableQty);
                newPharmaceuticalBillItemInReturnBill.setFreeQty(availableFreeQty);
            }
            BillItemFinanceDetails newBillItemFinanceDetailsInReturnBill = new BillItemFinanceDetails();
            newBillItemFinanceDetailsInReturnBill.setBillItem(newBillItemInReturnBill);
            newBillItemInReturnBill.setBillItemFinanceDetails(newBillItemFinanceDetailsInReturnBill);
            addDataToReturningBillItem(newBillItemInReturnBill);
            pharmacyCostingService.calculateUnitsPerPack(newBillItemFinanceDetailsInReturnBill);
            pharmacyCostingService.addBillItemFinanceDetailQuantitiesFromPharmaceuticalBillItem(newPharmaceuticalBillItemInReturnBill, newBillItemFinanceDetailsInReturnBill);
            BigDecimal lineGrossRateForAUnit = getReturnRateForUnits(pbiOfBilledBill.getBillItem());
            BigDecimal unitsPerPack = newBillItemFinanceDetailsInReturnBill.getUnitsPerPack();
            
            // Ensure both values are not null before multiplication
            if (lineGrossRateForAUnit == null) {
                lineGrossRateForAUnit = BigDecimal.ZERO;
            }
            if (unitsPerPack == null) {
                unitsPerPack = BigDecimal.ONE;
            }
            
            BigDecimal lineGrossRateAsEntered = lineGrossRateForAUnit.multiply(unitsPerPack);
            newBillItemFinanceDetailsInReturnBill.setLineGrossRate(lineGrossRateAsEntered);
            calculateLineTotalByLineGrossRate(newBillItemInReturnBill);
            getBillItems().add(newBillItemInReturnBill);
        }
        calculateTotalReturnByLineNetTotals();
    }

    private void calculateBillItemDetails(BillItem returningBillItem) {

        if (returningBillItem == null) {
            return;
        }

        BillItemFinanceDetails f = returningBillItem.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = returningBillItem.getPharmaceuticalBillItem();

        if (f == null) {
        }
        if (pbi == null) {
        }
        if (f == null || pbi == null) {
            return;
        }

        if (pharmacyCostingService != null) {
            pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(f);
        } else {
        }

        BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);

        if (returningBillItem.getItem() instanceof Ampp) {

            double qtyPacks = qty.doubleValue();
            pbi.setQtyPacks(qtyPacks);

            double qtyUnits = f.getQuantityByUnits().doubleValue();
            pbi.setQty(qtyUnits);

            double freeQtyPacks = freeQty.doubleValue();
            pbi.setFreeQtyPacks(freeQtyPacks);

            double freeQtyUnits = f.getFreeQuantityByUnits().doubleValue();
            pbi.setFreeQty(freeQtyUnits);

            double purchaseRatePack = f.getLineGrossRate().doubleValue();
            pbi.setPurchaseRatePack(purchaseRatePack);
            pbi.setPurchaseRate(purchaseRatePack);

            double retailRate = f.getRetailSaleRate().doubleValue();
            pbi.setRetailRate(retailRate);
            pbi.setRetailRatePack(retailRate);

            double retailRateUnit = f.getRetailSaleRatePerUnit().doubleValue();
            pbi.setRetailRateInUnit(retailRateUnit);

        } else {

            double qtyVal = qty.doubleValue();
            pbi.setQty(qtyVal);
            pbi.setQtyPacks(qtyVal);

            double freeQtyVal = freeQty.doubleValue();
            pbi.setFreeQty(freeQtyVal);
            pbi.setFreeQtyPacks(freeQtyVal);

            double purchaseRate = f.getLineGrossRate().doubleValue();
            pbi.setPurchaseRate(purchaseRate);
            pbi.setPurchaseRatePack(purchaseRate);

            f.setRetailSaleRate(f.getRetailSaleRatePerUnit());
            double retailRate = Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue();
            pbi.setRetailRate(retailRate);
            pbi.setRetailRatePack(retailRate);
            pbi.setRetailRateInUnit(retailRate);
        }

        double finalQty = f.getQuantityByUnits().doubleValue();
        double finalRate = f.getLineGrossRate().doubleValue();
        returningBillItem.setQty(finalQty);
        returningBillItem.setRate(finalRate);
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

        if (inputBillItem == null) {
            return;
        }

        BillItemFinanceDetails f = inputBillItem.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }

        BigDecimal qty = f.getQuantity();
        BigDecimal grossRate = f.getLineGrossRate();

        if (qty == null || grossRate == null) {
            return;
        }

        BigDecimal grossTotal = qty.multiply(grossRate);
        f.setLineGrossTotal(grossTotal);
    }

    private void calculateTotalReturnByLineNetTotals() {

        BigDecimal returnTotal = BigDecimal.ZERO;
        for (BillItem bi : billItems) {
            if (bi == null) {
                continue;
            }

            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }

            BigDecimal lineGrossTotal = f.getLineGrossTotal();

            if (lineGrossTotal != null) {
                returnTotal = returnTotal.add(lineGrossTotal);
            }
        }

        if (returnBill == null || returnBill.getBillFinanceDetails() == null) {
            return;
        }

        returnBill.getBillFinanceDetails().setNetTotal(returnTotal);
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
