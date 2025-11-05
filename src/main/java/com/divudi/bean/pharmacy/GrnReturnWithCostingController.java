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
            
            // Initialize BillFinanceDetails
            if (returnBill.getBillFinanceDetails() == null) {
                returnBill.setBillFinanceDetails(new BillFinanceDetails());
                returnBill.getBillFinanceDetails().setBill(returnBill);
            }
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

        // Note: During preparation, we don't modify the original bill item
        // The already returned quantities are used only for calculation purposes
        BigDecimal originalQty = safeToBigDecimal(bifdOriginal.getQuantity());
        BigDecimal originalFreeQty = safeToBigDecimal(bifdOriginal.getFreeQuantity());

        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false)) {
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
    }

    public BigDecimal getAlreadyReturnedQuantity(BillItem originalBillItem) {
        if (originalBillItem == null) {
            return BigDecimal.ZERO;
        }

        // For consistency with original quantities which are in units, 
        // sum quantityByUnits instead of quantity (which might be in packs for Ampp items)
        String sql = "Select sum(b.billItemFinanceDetails.quantityByUnits) "
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.bill.retired=false "
                + " and b.referanceBillItem=:obi "
                + " and b.bill.billTypeAtomic=:bta";

        Map<String, Object> params = new HashMap<>();
        params.put("obi", originalBillItem);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

        Object result = getBillItemFacade().findSingleScalar(sql, params);
        BigDecimal returnValue = BigDecimal.ZERO;

        if (result != null) {
            returnValue = safeToBigDecimal(result).abs(); // Use absolute value since returns are negative
        }

        return returnValue;
    }
    
    public BigDecimal getAlreadyReturnedQuantityWhenApproval(BillItem originalBillItem) {
        System.out.println("DEBUG getAlreadyReturnedQuantityWhenApproval: START");
        if (originalBillItem == null) {
            return BigDecimal.ZERO;
        }

        String itemName = originalBillItem.getItem() != null ? originalBillItem.getItem().getName() : "Unknown Item";
        System.out.println("DEBUG getAlreadyReturnedQuantityWhenApproval: Item=" + itemName);

        // For consistency with original quantities which are in units, 
        // sum quantityByUnits instead of quantity (which might be in packs for Ampp items)
        String sql = "Select sum(b.billItemFinanceDetails.quantityByUnits) "
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.bill.retired=false "
                + " and b.referanceBillItem=:obi "
                + " and b.bill.completed=true "
                + " and b.bill.billTypeAtomic=:bta";

        Map<String, Object> params = new HashMap<>();
        params.put("obi", originalBillItem);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

        System.out.println("DEBUG getAlreadyReturnedQuantityWhenApproval: SQL=" + sql);
        System.out.println("DEBUG getAlreadyReturnedQuantityWhenApproval: Looking for completed=true returns");

        Object result = getBillItemFacade().findSingleScalar(sql, params);
        BigDecimal returnValue = BigDecimal.ZERO;

        if (result != null) {
            returnValue = safeToBigDecimal(result).abs(); // Use absolute value since returns are negative
        } else {
        }

        return returnValue;
    }

    public BigDecimal getAlreadyReturnedFreeQuantity(BillItem originalBillItem) {
        if (originalBillItem == null) {
            return BigDecimal.ZERO;
        }

        // For consistency with original quantities which are in units, 
        // sum freeQuantityByUnits instead of freeQuantity (which might be in packs for Ampp items)
        String sql = "Select sum(b.billItemFinanceDetails.freeQuantityByUnits) "
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.bill.retired=false "
                + " and b.referanceBillItem=:obi "
                + " and b.bill.billTypeAtomic=:bta";

        Map<String, Object> params = new HashMap<>();
        params.put("obi", originalBillItem);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

        Object result = getBillItemFacade().findSingleScalar(sql, params);
        BigDecimal returnValue = BigDecimal.ZERO;

        if (result != null) {
            returnValue = safeToBigDecimal(result).abs(); // Use absolute value since returns are negative
        }

        return returnValue;
    }
    
    public BigDecimal getAlreadyReturnedFreeQuantityWhenApproval(BillItem originalBillItem) {
        System.out.println("DEBUG getAlreadyReturnedFreeQuantityWhenApproval: START");
        if (originalBillItem == null) {
            return BigDecimal.ZERO;
        }

        String itemName = originalBillItem.getItem() != null ? originalBillItem.getItem().getName() : "Unknown Item";
        System.out.println("DEBUG getAlreadyReturnedFreeQuantityWhenApproval: Item=" + itemName);

        // For consistency with original quantities which are in units, 
        // sum freeQuantityByUnits instead of freeQuantity (which might be in packs for Ampp items)
        String sql = "Select sum(b.billItemFinanceDetails.freeQuantityByUnits) "
                + " from BillItem b "
                + " where b.retired=false "
                + " and b.bill.retired=false "
                + " and b.referanceBillItem=:obi "
                + " and b.bill.completed=true "
                + " and b.bill.billTypeAtomic=:bta";

        Map<String, Object> params = new HashMap<>();
        params.put("obi", originalBillItem);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

        System.out.println("DEBUG getAlreadyReturnedFreeQuantityWhenApproval: Looking for completed=true free quantity returns");

        Object result = getBillItemFacade().findSingleScalar(sql, params);
        BigDecimal returnValue = BigDecimal.ZERO;

        if (result != null) {
            returnValue = safeToBigDecimal(result).abs(); // Use absolute value since returns are negative
        } else {
        }

        return returnValue;
    }

    public boolean isReturnQuantityValid() {
        boolean checkTotalQuantity = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);

        for (BillItem returningBillItem : getBillItems()) {
            if (returningBillItem == null) {
                continue;
            }

            BillItem originalBillItem = returningBillItem.getReferanceBillItem();
            if (originalBillItem == null) {
                continue;
            }

            BillItemFinanceDetails originalFd = originalBillItem.getBillItemFinanceDetails();
            BillItemFinanceDetails returningFd = returningBillItem.getBillItemFinanceDetails();

            if (originalFd == null || returningFd == null) {
                continue;
            }

            String itemName = originalBillItem.getItem() != null ? originalBillItem.getItem().getName() : "Unknown Item";

            // Get original purchased quantities in units for consistency with returned quantities
            BigDecimal originalQty = safeToBigDecimal(originalFd.getQuantityByUnits());
            BigDecimal originalFreeQty = safeToBigDecimal(originalFd.getFreeQuantityByUnits());

            // Get already returned quantities (from database, excluding this transaction)
            BigDecimal alreadyReturnedQty = getAlreadyReturnedQuantityWhenApproval(originalBillItem);
            BigDecimal alreadyReturnedFreeQty = getAlreadyReturnedFreeQuantityWhenApproval(originalBillItem);

            // Get quantities being returned now (current user input)
            BigDecimal currentReturnQty = safeToBigDecimal(returningFd.getQuantity());
            BigDecimal currentReturnFreeQty = safeToBigDecimal(returningFd.getFreeQuantity());
            // Debug output to understand the calculation

            // Debug output to understand the calculation
            System.out.println("=== VALIDATION DEBUG for " + itemName + " ===");
            System.out.println("Original qty: " + originalQty + ", Original free qty: " + originalFreeQty);
            System.out.println("Already returned qty: " + alreadyReturnedQty + ", Already returned free qty: " + alreadyReturnedFreeQty);
            System.out.println("Current return qty: " + currentReturnQty + ", Current return free qty: " + currentReturnFreeQty);

            // FIXED: Use absolute values for current return quantities and compare directly
            // Don't add to already returned quantities - just validate against remaining balance
            BigDecimal remainingQty = originalQty.subtract(alreadyReturnedQty);
            BigDecimal remainingFreeQty = originalFreeQty.subtract(alreadyReturnedFreeQty);
            BigDecimal remainingTotal = remainingQty.add(remainingFreeQty);
            

            if (checkTotalQuantity) {
                // Check total quantity (qty + free qty combined)
                BigDecimal currentReturnTotal = currentReturnQty.add(currentReturnFreeQty);
                
                if (currentReturnTotal.compareTo(remainingTotal) > 0) {
                    JsfUtil.addErrorMessage("Cannot return " + currentReturnTotal + " total quantity for item '"
                            + itemName + "'. Maximum returnable: " + remainingTotal);
                    return false;
                }
            } else {
                // Check quantities separately
                if (currentReturnQty.compareTo(remainingQty) > 0) {
                    JsfUtil.addErrorMessage("Cannot return " + currentReturnQty + " quantity for item '"
                            + itemName + "'. Maximum returnable: " + remainingQty);
                    return false;
                }

                if (currentReturnFreeQty.compareTo(remainingFreeQty) > 0) {
                    JsfUtil.addErrorMessage("Cannot return " + currentReturnFreeQty + " free quantity for item '"
                            + itemName + "'. Maximum returnable: " + remainingFreeQty);
                    return false;
                }
            }
        }

        return true;
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

    /**
     * Checks if a GRN is fully returned by comparing original quantities with returned quantities
     * for all items in the GRN.
     * @param grnBill The original GRN bill to check
     * @return true if all items are fully returned, false otherwise
     */
    private boolean isGrnFullyReturned(Bill grnBill) {
        System.out.println("=== isGrnFullyReturned DEBUG START ===");
        if (grnBill == null) {
            return false;
        }

        System.out.println("DEBUG: Checking GRN ID=" + grnBill.getId() + ", DeptId=" + grnBill.getDeptId());

        // Get all bill items from the original GRN
        String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId AND bi.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", grnBill.getId());
        List<BillItem> originalBillItems = getBillItemFacade().findByJpql(jpql, params);

        if (originalBillItems == null || originalBillItems.isEmpty()) {
            return false;
        }

        System.out.println("DEBUG: Found " + originalBillItems.size() + " original bill items");

        // Check each item to see if it's fully returned
        int itemIndex = 0;
        for (BillItem originalBillItem : originalBillItems) {
            itemIndex++;
            String itemName = originalBillItem.getItem() != null ? originalBillItem.getItem().getName() : "Unknown Item";
            System.out.println("DEBUG: Checking item " + itemIndex + "/" + originalBillItems.size() + ": " + itemName);
            
            BillItemFinanceDetails originalFd = originalBillItem.getBillItemFinanceDetails();
            if (originalFd == null) {
                continue;
            }

            // Get original quantities in units for consistency with returned quantities
            // Use quantityByUnits and freeQuantityByUnits to match the return calculation logic
            BigDecimal originalQty = safeToBigDecimal(originalFd.getQuantityByUnits());
            BigDecimal originalFreeQty = safeToBigDecimal(originalFd.getFreeQuantityByUnits());
            System.out.println("DEBUG: Item " + itemIndex + " original qty=" + originalQty + " (units), originalFreeQty=" + originalFreeQty + " (units)");

            // Get already returned quantities (using approved/completed returns only)
            // Note: This includes the current transaction since return bill is already saved as completed
            BigDecimal returnedQty = getAlreadyReturnedQuantityWhenApproval(originalBillItem);
            BigDecimal returnedFreeQty = getAlreadyReturnedFreeQuantityWhenApproval(originalBillItem);
            
            System.out.println("DEBUG: Item " + itemIndex + " total returned qty=" + returnedQty + ", returnedFreeQty=" + returnedFreeQty);

            // Calculate remaining quantities
            BigDecimal remainingQty = originalQty.subtract(returnedQty);
            BigDecimal remainingFreeQty = originalFreeQty.subtract(returnedFreeQty);
            
            // For total quantity mode, check total remaining instead of individual qty/free qty
            BigDecimal originalTotal = originalQty.add(originalFreeQty);
            BigDecimal returnedTotal = returnedQty.add(returnedFreeQty);
            BigDecimal remainingTotal = originalTotal.subtract(returnedTotal);
            
            System.out.println("DEBUG: Item " + itemIndex + " remaining qty=" + remainingQty + ", remainingFreeQty=" + remainingFreeQty);
            System.out.println("DEBUG: Item " + itemIndex + " originalTotal=" + originalTotal + ", returnedTotal=" + returnedTotal + ", remainingTotal=" + remainingTotal);

            // Check if item is fully returned - use total quantity comparison for more accurate results
            boolean isItemFullyReturned = remainingTotal.compareTo(BigDecimal.ZERO) <= 0;
            System.out.println("DEBUG: Item " + itemIndex + " isItemFullyReturned=" + isItemFullyReturned);

            if (!isItemFullyReturned) {
                System.out.println("DEBUG: Item " + itemIndex + " still has remaining quantities, GRN is NOT fully returned");
                return false;
            }
        }

        // All items are fully returned
        System.out.println("DEBUG: ALL items are fully returned!");
        return true;
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
        System.out.println("=== onEdit START ===");
        if (editingBillItem == null || editingBillItem.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails fd = editingBillItem.getBillItemFinanceDetails();
        System.out.println("onEdit: Input qty=" + fd.getQuantity() + ", freeQty=" + fd.getFreeQuantity() + ", rate=" + fd.getLineGrossRate());
        
        // FIXED: Use consistent validation logic with the main validation method
        if (editingBillItem.getReferanceBillItem() != null) {
            BillItem originalBillItem = editingBillItem.getReferanceBillItem();
            BillItemFinanceDetails originalFd = originalBillItem.getBillItemFinanceDetails();
            
            if (originalFd != null) {
                // Get original and already returned quantities (all in units for consistency)
                BigDecimal originalQty = safeToBigDecimal(originalFd.getQuantityByUnits());
                BigDecimal originalFreeQty = safeToBigDecimal(originalFd.getFreeQuantityByUnits());
                BigDecimal alreadyReturnedQty = getAlreadyReturnedQuantity(originalBillItem);
                BigDecimal alreadyReturnedFreeQty = getAlreadyReturnedFreeQuantity(originalBillItem);
                
                // Calculate remaining quantities
                BigDecimal remainingQty = originalQty.subtract(alreadyReturnedQty);
                BigDecimal remainingFreeQty = originalFreeQty.subtract(alreadyReturnedFreeQty);
                BigDecimal remainingTotal = remainingQty.add(remainingFreeQty);
                
                boolean returnByTotalQty = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);
                
                if (returnByTotalQty) {
                    double currentTotalQty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
                    if (currentTotalQty > remainingTotal.doubleValue()) {
                        fd.setQuantity(BigDecimal.valueOf(Math.max(0, remainingTotal.doubleValue())));
                        fd.setFreeQuantity(BigDecimal.ZERO);
                        JsfUtil.addErrorMessage("Cannot return more than remaining quantity. Remaining: " + remainingTotal);
                    }
                } else {
                    double currentQty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
                    double currentFreeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;
                    
                    if (currentQty > remainingQty.doubleValue()) {
                        fd.setQuantity(BigDecimal.valueOf(Math.max(0, remainingQty.doubleValue())));
                        JsfUtil.addErrorMessage("Cannot return more than remaining quantity. Remaining: " + remainingQty);
                    }
                    if (currentFreeQty > remainingFreeQty.doubleValue()) {
                        fd.setFreeQuantity(BigDecimal.valueOf(Math.max(0, remainingFreeQty.doubleValue())));
                        JsfUtil.addErrorMessage("Cannot return more than remaining free quantity. Remaining: " + remainingFreeQty);
                    }
                }
            }
        }

        System.out.println("onEdit: After validation qty=" + fd.getQuantity() + ", freeQty=" + fd.getFreeQuantity());

        // Sync pharmaceutical bill item quantities with finance details (without resetting user input)
        pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(editingBillItem.getPharmaceuticalBillItem(), fd);
        
        // Calculate line total based on user input quantities and rate
        calculateLineTotalByLineGrossRate(editingBillItem);
        System.out.println("onEdit: After line calc, lineGrossTotal=" + fd.getLineGrossTotal());
        
        // Update bill-level totals
        calculateTotalReturnByLineNetTotals();
        System.out.println("onEdit: Bill total=" + (returnBill != null && returnBill.getBillFinanceDetails() != null ? returnBill.getBillFinanceDetails().getNetTotal() : "null"));
        
        // Set pharmacy item context
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
        
        getReturnBill().setCompleted(true);

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }else{
            getBillFacade().edit(getReturnBill());
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

        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Line Cost Rate", false)
                && fd.getLineCostRate() != null) {
            rate = fd.getLineCostRate();
        } else if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Total Cost Rate", false)
                && fd.getTotalCostRate() != null) {
            rate = fd.getTotalCostRate();
        } else if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Purchase Rate", false)
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
        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Line Cost Rate", false)
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
        } else if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Total Cost Rate", false)
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
        } else if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Purchase Rate", false)
                && fd.getLineGrossRate() != null) {
            rate = fd.getLineGrossRate();
        }
        return rate;
    }

    public String getReturnRateLabel() {
        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Line Cost Rate", false)) {
            return "Line Cost Rate";
        }
        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Total Cost Rate", false)) {
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
        List<BillItem> zeroQuantityItems = new ArrayList<>();

        for (Iterator<BillItem> iterator = getBillItems().iterator(); iterator.hasNext();) {
            BillItem i = iterator.next();

            BillItemFinanceDetails fd = i.getBillItemFinanceDetails();
            BillItem referanceBillItem = i.getReferanceBillItem();
            BillItemFinanceDetails refFd = referanceBillItem != null ? referanceBillItem.getBillItemFinanceDetails() : null;

            if (fd == null || refFd == null) {
                continue; // Skip if finance details are missing
            }

            // Check if item has zero return quantities
            BigDecimal returnQty = Optional.ofNullable(fd.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal returnFreeQty = Optional.ofNullable(fd.getFreeQuantity()).orElse(BigDecimal.ZERO);
            boolean hasZeroQuantities = returnQty.compareTo(BigDecimal.ZERO) == 0 && returnFreeQty.compareTo(BigDecimal.ZERO) == 0;

            if (hasZeroQuantities) {
                // If item already exists in database, retire it
                if (i.getId() != null) {
                    i.setRetired(true);
                    i.setRetiredAt(new Date());
                    i.setRetirer(sessionController.getLoggedUser());
                    billItemFacade.edit(i);
                    zeroQuantityItems.add(i);
                }
                // Remove from current list
                iterator.remove();
                continue;
            }

            PharmaceuticalBillItem pbi = i.getPharmaceuticalBillItem();
            pharmacyCostingService.makeAllQuantityValuesNegative(pbi);
            if (i.getId() == null) {
                i.setCreatedAt(new Date());
                i.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(i);
            } else {
                billItemFacade.edit(i);
            }

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

        // Log information about zero quantity items that were retired
        if (!zeroQuantityItems.isEmpty()) {
            StringBuilder infoMessage = new StringBuilder("Items with zero return quantities were excluded from the return:<br/>");
            for (BillItem zeroItem : zeroQuantityItems) {
                if (zeroItem != null && zeroItem.getItem() != null) {
                    infoMessage.append("- ").append(zeroItem.getItem().getName()).append("<br/>");
                }
            }
            JsfUtil.addSuccessMessage(infoMessage.toString());
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

            // Skip items with zero return quantities
            BigDecimal returnQty = Optional.ofNullable(fd.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal returnFreeQty = Optional.ofNullable(fd.getFreeQuantity()).orElse(BigDecimal.ZERO);
            boolean hasZeroQuantities = returnQty.compareTo(BigDecimal.ZERO) == 0 && returnFreeQty.compareTo(BigDecimal.ZERO) == 0;

            if (hasZeroQuantities) {
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

            BigDecimal currentReturnQty = refFd.getReturnQuantity() == null ? BigDecimal.ZERO : refFd.getReturnQuantity();
            BigDecimal currentReturnFreeQty = refFd.getReturnFreeQuantity() == null ? BigDecimal.ZERO : refFd.getReturnFreeQuantity();
            BigDecimal addQty = fd.getQuantity() == null ? BigDecimal.ZERO : fd.getQuantity();
            BigDecimal addFreeQty = fd.getFreeQuantity() == null ? BigDecimal.ZERO : fd.getFreeQuantity();

            refFd.setReturnQuantity(currentReturnQty.add(addQty));
            refFd.setReturnFreeQuantity(currentReturnFreeQty.add(addFreeQty));

            refPbi.setRemainingQty(refPbi.getRemainingQty() - pbi.getQty());
            refPbi.setRemainingQtyPack(refPbi.getRemainingQtyPack() - pbi.getQtyPacks());

            refPbi.setRemainingFreeQty(refPbi.getRemainingFreeQty() - pbi.getFreeQty());
            refPbi.setRemainingFreeQtyPack(refPbi.getRemainingFreeQtyPack() - pbi.getFreeQtyPacks());

            billItemFacade.edit(ref);

        }
    }

    private void applyPendingReturnTotals() {
        for (BillItem i : getBillItems()) {
            BillItemFinanceDetails fd = i.getBillItemFinanceDetails();
            BillItem ref = i.getReferanceBillItem();
            BillItemFinanceDetails refFd = ref != null ? ref.getBillItemFinanceDetails() : null;

            if (fd == null || refFd == null) {
                continue;
            }

            BigDecimal qty = fd.getQuantity() == null ? BigDecimal.ZERO : fd.getQuantity().abs();
            BigDecimal freeQty = fd.getFreeQuantity() == null ? BigDecimal.ZERO : fd.getFreeQuantity().abs();

            BigDecimal currentReturnQty = refFd.getReturnQuantity() == null ? BigDecimal.ZERO : refFd.getReturnQuantity();
            BigDecimal currentReturnFreeQty = refFd.getReturnFreeQuantity() == null ? BigDecimal.ZERO : refFd.getReturnFreeQuantity();

            refFd.setReturnQuantity(currentReturnQty.add(qty));
            refFd.setReturnFreeQuantity(currentReturnFreeQty.add(freeQty));
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

            BigDecimal qty = fd.getQuantity() == null ? BigDecimal.ZERO : fd.getQuantity().abs();
            BigDecimal freeQty = fd.getFreeQuantity() == null ? BigDecimal.ZERO : fd.getFreeQuantity().abs();

            BigDecimal currentReturnQty = refFd.getReturnQuantity() == null ? BigDecimal.ZERO : refFd.getReturnQuantity();
            BigDecimal currentReturnFreeQty = refFd.getReturnFreeQuantity() == null ? BigDecimal.ZERO : refFd.getReturnFreeQuantity();

            refFd.setReturnQuantity(currentReturnQty.subtract(qty));
            refFd.setReturnFreeQuantity(currentReturnFreeQty.subtract(freeQty));
        }
    }

    public void settleGrnReturn() {
        if (returnBill == null) {
            JsfUtil.addErrorMessage("No GRN Return bill");
            return;
        }
        if (bill == null) {
            JsfUtil.addErrorMessage("No GRN");
            return;
        }
        if (billItems == null) {
            JsfUtil.addErrorMessage("No GRN Return Bill Items");
            return;
        }
        if (billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No GRN Return Bill Items");
            return;
        }
        if (returnBill.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a payment method please");
            return;
        }
        boolean checkByTotalQty = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity");
        boolean checkByQtyAndFree = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Quantity and Free Quantity");

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

        // Validate return quantities before processing
        if (!isReturnQuantityValid()) {
            return;
        }

        fillData();
        applyPendingReturnTotals();

        if (getPharmacyBean().isInsufficientStockForReturn(getBillItems())) {
            revertPendingReturnTotals();
            JsfUtil.addErrorMessage("Insufficient stock available to return these items.");
            return;
        }

        revertPendingReturnTotals();

        saveReturnBill();
        saveBillItems();
        applyRemainingValuesInOriginalBill(returnBill.getBillItems());

        boolean generatePayments = configOptionApplicationController.getBooleanValueByKey(
            "Generate Payments for GRN, GRN Returns, Direct Purchase, and Direct Purchase Returns", false);
        Payment p = null;
        if (generatePayments) {
            p = createPayment(getReturnBill(), getReturnBill().getPaymentMethod());
        }

        getBillFacade().edit(getReturnBill());
        
        System.out.println("=== SETTLE GRN RETURN DEBUG ===");
        System.out.println("DEBUG: About to check if GRN is fully returned");
        System.out.println("DEBUG: Original GRN ID=" + (getBill() != null ? getBill().getId() : "null"));
        System.out.println("DEBUG: Original GRN DeptId=" + (getBill() != null ? getBill().getDeptId() : "null"));
        
        // Check if GRN is fully returned and mark as fullReturned
        boolean isFullyReturned = isGrnFullyReturned(getBill());
        
        if (isFullyReturned) {
            getBill().setFullReturned(true);
            getBill().setFullReturnedBy(getSessionController().getLoggedUser());
            getBill().setFullReturnedAt(new Date());
            JsfUtil.addSuccessMessage("GRN has been fully returned and marked as complete.");
        } else {
        }
        
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
        System.out.println("=== prepareBillItems START ===");
        if (bill == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        System.out.println("prepareBillItems: billId=" + bill.getId());
        if (bill.getId() == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        String jpql = "Select p from PharmaceuticalBillItem p where p.billItem.bill.id = :billId";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", bill.getId());
        List<PharmaceuticalBillItem> pbisOfBilledBill = getPharmaceuticalBillItemFacade().findByJpql(jpql, params);
        System.out.println("prepareBillItems: fetched pharmaceutical items count=" + (pbisOfBilledBill != null ? pbisOfBilledBill.size() : 0));
        for (PharmaceuticalBillItem pbiOfBilledBill : pbisOfBilledBill) {
            try {
                String itemName = null;
                Long itemId = null;
                if (pbiOfBilledBill != null && pbiOfBilledBill.getBillItem() != null && pbiOfBilledBill.getBillItem().getItem() != null) {
                    itemName = pbiOfBilledBill.getBillItem().getItem().getName();
                    itemId = pbiOfBilledBill.getBillItem().getItem().getId();
                }
                System.out.println("prepareBillItems: processing item id=" + itemId + ", name=" + itemName);
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
            System.out.println(
                    "prepareBillItems: originalQtyInUnits=" + originalQtyInUnits
                    + ", originalFreeQtyInUnits=" + originalFreeQtyInUnits);

            boolean returnByTotalQuantity = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);
            System.out.println("prepareBillItems: returnByTotalQuantity=" + returnByTotalQuantity);

            if (returnByTotalQuantity) {
                // Use approval-scoped database query methods
                BigDecimal alreadyReturnedQty = getAlreadyReturnedQuantityWhenApproval(pbiOfBilledBill.getBillItem());
                BigDecimal alreadyReturnedFreeQty = getAlreadyReturnedFreeQuantityWhenApproval(pbiOfBilledBill.getBillItem());
                BigDecimal totalAlreadyReturned = alreadyReturnedQty.add(alreadyReturnedFreeQty);
                
                double originalTotal = Math.abs(originalQtyInUnits) + Math.abs(originalFreeQtyInUnits);
                double availableToReturn = originalTotal - totalAlreadyReturned.doubleValue();
                
                // Ensure we don't show negative quantities
                availableToReturn = Math.max(0.0, availableToReturn);
                newPharmaceuticalBillItemInReturnBill.setQty(availableToReturn);
                newPharmaceuticalBillItemInReturnBill.setFreeQty(0.0);
            } else {
                // Use approval-scoped database query methods
                BigDecimal alreadyReturnedQty = getAlreadyReturnedQuantityWhenApproval(pbiOfBilledBill.getBillItem());
                BigDecimal alreadyReturnedFreeQty = getAlreadyReturnedFreeQuantityWhenApproval(pbiOfBilledBill.getBillItem());
                
                double availableQty = Math.abs(originalQtyInUnits) - alreadyReturnedQty.doubleValue();
                double availableFreeQty = Math.abs(originalFreeQtyInUnits) - alreadyReturnedFreeQty.doubleValue();
                
                // Ensure we don't show negative quantities
                availableQty = Math.max(0.0, availableQty);
                availableFreeQty = Math.max(0.0, availableFreeQty);
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
            System.out.println(
                    "prepareBillItems: unitsPerPack=" + unitsPerPack
                    + ", lineGrossRateForAUnit=" + lineGrossRateForAUnit
                    + ", lineGrossRateAsEntered=" + lineGrossRateAsEntered);
            newBillItemFinanceDetailsInReturnBill.setLineGrossRate(lineGrossRateAsEntered);
            calculateLineTotalByLineGrossRate(newBillItemInReturnBill);
            getBillItems().add(newBillItemInReturnBill);
            } catch (Exception e) {
                System.out.println("prepareBillItems: ERROR while preparing an item - " + e.getMessage());
            }
        }
        calculateTotalReturnByLineNetTotals();
        try {
            BigDecimal netTotal = getReturnBill() != null && getReturnBill().getBillFinanceDetails() != null
                    ? Optional.ofNullable(getReturnBill().getBillFinanceDetails().getNetTotal()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            BigDecimal grossTotal = getReturnBill() != null && getReturnBill().getBillFinanceDetails() != null
                    ? Optional.ofNullable(getReturnBill().getBillFinanceDetails().getGrossTotal()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
        } catch (Exception ignore) {
            // Keep silent on totals extraction errors, already traced elsewhere
        }
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

        pharmacyCostingService.recalculateFinancialsForBillItemForGrnReturn(f);

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

            double retailRate = f.getRetailSaleRate() != null ? f.getRetailSaleRate().doubleValue() : 0.0;
            pbi.setRetailRate(retailRate);
            pbi.setRetailRatePack(retailRate);

            double retailRateUnit = f.getRetailSaleRatePerUnit() != null ? f.getRetailSaleRatePerUnit().doubleValue() : 0.0;
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
        pharmacyCostingService.calculateBillTotalsFromItemsForGrnReturns(getReturnBill(), getBillItems());
    }

    public void onReturnRateChange(BillItem bi) {
        calculateBillItemDetails(bi);
        calculateLineTotalByLineGrossRate(bi);
        callculateBillDetails();
        calculateTotalReturnByLineNetTotals();
    }

    public void onReturningTotalQtyChange(BillItem editingBillItem) {
        onEdit(editingBillItem);
    }

    private void calculateLineTotalByLineGrossRate(BillItem inputBillItem) {

        if (inputBillItem == null) {
            return;
        }

        BillItemFinanceDetails f = inputBillItem.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }

        BigDecimal qty = f.getQuantity() != null ? f.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = f.getFreeQuantity() != null ? f.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal totalQty = qty.add(freeQty);
        BigDecimal grossRate = f.getLineGrossRate();

        if (grossRate == null) {
            grossRate = BigDecimal.ZERO;
        }

        // For GRN returns, line total = total quantity (qty + free qty) × rate
        BigDecimal grossTotal = totalQty.multiply(grossRate);
        f.setLineGrossTotal(grossTotal);
    }

    private void calculateTotalReturnByLineNetTotals() {
        System.out.println("calculateTotalReturnByLineNetTotals: START");

        BigDecimal returnTotal = BigDecimal.ZERO;
        int itemCount = 0;
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
                itemCount++;
            }
        }

        if (returnBill == null) {
            return;
        }
        
        if (returnBill.getBillFinanceDetails() == null) {
            returnBill.setBillFinanceDetails(new BillFinanceDetails());
            returnBill.getBillFinanceDetails().setBill(returnBill);
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
