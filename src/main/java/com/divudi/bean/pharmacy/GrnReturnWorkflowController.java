/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for GRN Return Workflow - handles Create, Finalize, and Approve
 * steps
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class GrnReturnWorkflowController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(GrnReturnWorkflowController.class.getName());

    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private PharmacyCostingService pharmacyCostingService;

    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    EnumController enumController;

    // Main properties
    private Bill currentBill;
    private Bill requestedBill;  // For approval process
    private Bill approvedBill;   // For approval process
    private BillItem currentBillItem;
    private List<BillItem> selectedBillItems;
    private List<BillItem> billItems;
    private List<BillItem> selectedItems;  // For approval process
    private boolean printPreview;

    // GRN Return specific properties
    private Bill selectedGrn;
    private Bill originalGrn;
    private List<Item> dealorItems;

    // Bill lists for workflow steps
    private List<Bill> grnReturnsToFinalize;
    private List<Bill> grnReturnsToApprove;
    private List<Bill> filteredGrnReturnsToFinalize;
    private List<Bill> filteredGrnReturnsToApprove;

    @Inject
    PharmacyCalculation pharmacyBillBean;

    // Navigation methods
    public String navigateToCreateGrnReturn() {
        resetBillValues();
        makeListNull();
        return "/pharmacy/pharmacy_grn_return_request?faces-redirect=true";
    }

    public String navigateToCreateGrnReturnFromGrn() {
        if (selectedGrn == null) {
            JsfUtil.addErrorMessage("No GRN selected");
            return "";
        }
        // Follow legacy pattern - create return bill from selected GRN
        createReturnBillFromGrn(selectedGrn);
        return "/pharmacy/pharmacy_grn_return_form?faces-redirect=true";
    }

    public String navigateToUpdateGrnReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No GRN Return selected");
            return "";
        }
        generateBillItemsForUpdate();
        return "/pharmacy/pharmacy_grn_return_form?faces-redirect=true";
    }

    public String navigateToFinalizeGrnReturn() {
        makeListNull();
        return "/pharmacy/pharmacy_grn_return_list_to_finalize?faces-redirect=true";
    }

    public String navigateToApproveGrnReturn() {
        makeListNull();
        return "/pharmacy/pharmacy_grn_return_list_to_approve?faces-redirect=true";
    }

    public String navigateToGrnReturnApproval() {
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No GRN Return selected for approval");
            return "";
        }

        // Create approved bill based on requested bill
        approvedBill = new RefundBill();
        approvedBill.copy(requestedBill);
        approvedBill.setBillType(BillType.PharmacyGrnReturn);
        approvedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
        approvedBill.setReferenceBill(requestedBill.getReferenceBill());
        approvedBill.setBilledBill(requestedBill);

        generateBillItemsForApproval();

        return "/pharmacy/pharmacy_grn_return_approving?faces-redirect=true";
    }

    // Core workflow methods
    public void saveRequest() {
        saveBill(false);
        JsfUtil.addSuccessMessage("GRN Return Request Saved Successfully");
    }

    public void finalizeRequest() {
        if (validateGrnReturn()) {
            saveBill(true);
            printPreview = true;
            JsfUtil.addSuccessMessage("GRN Return Request Finalized Successfully");
        }
    }

    public void approve() {
        if (validateApproval()) {
            saveApprovedBill();
            updateStock();  // Stock handling happens only at approval stage
            printPreview = true;
            JsfUtil.addSuccessMessage("GRN Return Approved Successfully");
        }
    }

    // Bill management methods
    private void saveBill(boolean finalize) {
        if (currentBill.getId() == null) {
            currentBill.setBillType(BillType.PharmacyGrnReturn);
            currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
            currentBill.setInstitution(sessionController.getInstitution());
            currentBill.setDepartment(sessionController.getDepartment());
            currentBill.setCreater(sessionController.getLoggedUser());
            currentBill.setCreatedAt(new Date());

            String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
                    sessionController.getDepartment(),
                    BillTypeAtomic.PHARMACY_GRN_RETURN);
            currentBill.setDeptId(billNumber);
            currentBill.setInsId(billNumber);
        }

        if (finalize) {
            currentBill.setCheckedBy(sessionController.getLoggedUser());
            currentBill.setCheckeAt(new Date());
        }

        calculateTotal();
        billFacade.edit(currentBill);
        saveBillItems();
    }

    private void saveApprovedBill() {
        approvedBill.setInstitution(sessionController.getInstitution());
        approvedBill.setDepartment(sessionController.getDepartment());
        approvedBill.setCreater(sessionController.getLoggedUser());
        approvedBill.setCreatedAt(new Date());
        approvedBill.setCheckedBy(sessionController.getLoggedUser());
        approvedBill.setCheckeAt(new Date());

        String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(),
                BillTypeAtomic.PHARMACY_GRN_RETURN);
        approvedBill.setDeptId(billNumber);
        approvedBill.setInsId(billNumber);

        calculateApprovedTotal();
        billFacade.edit(approvedBill);
        saveApprovedBillItems();

        // Link the approved bill to the requested bill
        requestedBill.setReferenceBill(approvedBill);
        billFacade.edit(requestedBill);
    }

    private void saveBillItems() {
        for (BillItem bi : billItems) {
            if (bi.getPharmaceuticalBillItem().getQty() == 0
                    && bi.getPharmaceuticalBillItem().getFreeQty() == 0) {
                continue;
            }

            bi.setBill(currentBill);
            bi.setCreatedAt(new Date());
            bi.setCreater(sessionController.getLoggedUser());

            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            phi.setBillItem(bi);

            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }

            if (phi.getId() == null) {
                pharmaceuticalBillItemFacade.create(phi);
            } else {
                pharmaceuticalBillItemFacade.edit(phi);
            }
        }
    }

    private void saveApprovedBillItems() {
        for (BillItem bi : billItems) {
            if (bi.getPharmaceuticalBillItem().getQty() == 0
                    && bi.getPharmaceuticalBillItem().getFreeQty() == 0) {
                continue;
            }

            BillItem approvedBi = new BillItem();
            approvedBi.copy(bi);
            approvedBi.setBill(approvedBill);
            approvedBi.setCreatedAt(new Date());
            approvedBi.setCreater(sessionController.getLoggedUser());

            PharmaceuticalBillItem approvedPhi = new PharmaceuticalBillItem();
            approvedPhi.copy(bi.getPharmaceuticalBillItem());
            approvedPhi.setBillItem(approvedBi);

            billItemFacade.create(approvedBi);
            pharmaceuticalBillItemFacade.create(approvedPhi);
        }
    }

    // Stock handling - only at approval stage
    private void updateStock() {
        for (BillItem bi : billItems) {
            if (bi.getPharmaceuticalBillItem().getQty() == 0
                    && bi.getPharmaceuticalBillItem().getFreeQty() == 0) {
                continue;
            }

            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            double totalQty = phi.getQty() + phi.getFreeQty();

            // Deduct from stock for return
            boolean returnFlag = pharmacyBean.deductFromStock(
                    phi.getStock(),
                    Math.abs(totalQty),
                    phi,
                    sessionController.getDepartment()
            );

            if (!returnFlag) {
                LOGGER.log(Level.WARNING, "Unable to deduct stock for item: {0}", bi.getItem().getName());
                // Reset quantities if stock deduction failed
                phi.setQty(0);
                phi.setFreeQty(0);
                pharmaceuticalBillItemFacade.edit(phi);
            }
        }
    }

    // Validation methods
    private boolean validateGrnReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No GRN Return Bill");
            return false;
        }

        if (originalGrn == null) {
            JsfUtil.addErrorMessage("Please select a GRN to return items from");
            return false;
        }

        // Use comprehensive validation
        return validateFinalization();
    }

    // Item management methods
    public void addItem() {
        if (currentBillItem.getItem() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        BillItem newBi = createBillItemFromGrnItem(currentBillItem.getItem());
        if (newBi != null) {
            billItems.add(newBi);
            calculateTotal();
        }

        currentBillItem = new BillItem();
    }

    private BillItem createBillItemFromGrnItem(Item item) {
        // Find the item in the original GRN
        PharmaceuticalBillItem originalPhi = findItemInGrn(item, originalGrn);
        if (originalPhi == null) {
            JsfUtil.addErrorMessage("Item not found in selected GRN");
            return null;
        }

        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setSearialNo(billItems.size());

        PharmaceuticalBillItem phi = new PharmaceuticalBillItem();
        phi.copy(originalPhi);
        phi.setBillItem(bi);
        phi.setQty(0.0);  // Default return quantity
        phi.setFreeQty(0.0);  // Default free return quantity

        bi.setPharmaceuticalBillItem(phi);

        return bi;
    }

    private PharmaceuticalBillItem findItemInGrn(Item item, Bill grn) {
        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                + "WHERE phi.billItem.bill = :grn "
                + "AND phi.billItem.item = :item "
                + "AND phi.billItem.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("grn", grn);
        params.put("item", item);
        params.put("ret", false);

        List<PharmaceuticalBillItem> phis = pharmaceuticalBillItemFacade.findByJpql(sql, params);
        return phis.isEmpty() ? null : phis.get(0);
    }

    public void removeItem(BillItem bi) {
        billItems.remove(bi);
        calculateTotal();
    }

    public void removeSelected() {
        if (selectedBillItems != null) {
            billItems.removeAll(selectedBillItems);
            calculateTotal();
        }
    }

    // Calculation methods
    private void calculateTotal() {
        if (currentBill == null) {
            return;
        }

        double total = 0.0;
        for (BillItem bi : billItems) {
            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            double itemTotal = (phi.getQty() + phi.getFreeQty()) * phi.getPurchaseRateInUnit();
            bi.setNetValue(itemTotal);
            total += itemTotal;
        }

        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateApprovedTotal() {
        if (approvedBill == null) {
            return;
        }

        double total = 0.0;
        for (BillItem bi : billItems) {
            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            double itemTotal = (phi.getQty() + phi.getFreeQty()) * phi.getPurchaseRateInUnit();
            bi.setNetValue(itemTotal);
            total += itemTotal;
        }

        approvedBill.setTotal(total);
        approvedBill.setNetTotal(total);
    }

    // Event handlers (with comprehensive validation)
    public void onEdit(BillItem bi) {
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    public void onFocus(BillItem bi) {
        // Set current editing item for context
        currentBillItem = bi;
    }

    public void onGrnSelect() {
        if (selectedGrn != null) {
            originalGrn = selectedGrn;
            currentBill.setToInstitution(originalGrn.getFromInstitution());
            currentBill.setReferenceBill(originalGrn);
            generateItemsFromGrn();
        }
    }

    private void generateItemsFromGrn() {
        billItems.clear();

        if (originalGrn == null) {
            return;
        }

        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                + "WHERE phi.billItem.bill = :grn "
                + "AND phi.billItem.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("grn", originalGrn);
        params.put("ret", false);

        List<PharmaceuticalBillItem> grnItems = pharmaceuticalBillItemFacade.findByJpql(sql, params);

        // Check configuration options for quantity loading (following legacy pattern)
        boolean returnByTotalQty = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);

        for (PharmaceuticalBillItem grnPhi : grnItems) {
            BillItem originalBillItem = grnPhi.getBillItem();

            // Create new return bill item (following legacy pattern at lines 1102-1107)
            BillItem returnBillItem = new BillItem();
            returnBillItem.setQty(0.0);  // Will be set later based on units calculation
            returnBillItem.setBill(currentBill);
            returnBillItem.setItem(originalBillItem.getItem());
            returnBillItem.setReferanceBillItem(originalBillItem);
            returnBillItem.setSearialNo(billItems.size());

            // Create pharmaceutical bill item for return (following legacy pattern at lines 1108-1119)
            PharmaceuticalBillItem returnPhi = new PharmaceuticalBillItem();
            returnPhi.setBillItem(returnBillItem);
            returnPhi.setItemBatch(grnPhi.getItemBatch());
            returnPhi.setStock(grnPhi.getStock());
            
            // Copy rate properties from original GRN
            returnPhi.setPurchaseRate(grnPhi.getPurchaseRate());
            returnPhi.setPurchaseRatePack(grnPhi.getPurchaseRatePack());
            returnPhi.setPurchaseRateInUnit(grnPhi.getPurchaseRateInUnit());
            returnPhi.setRetailRate(grnPhi.getRetailRate());
            returnPhi.setRetailRatePack(grnPhi.getRetailRatePack());
            returnPhi.setRetailRateInUnit(grnPhi.getRetailRateInUnit());

            // Get original quantities in units for calculation (following legacy pattern)
            double originalQtyInUnits = Math.abs(grnPhi.getQty());
            double originalFreeQtyInUnits = Math.abs(grnPhi.getFreeQty());

            // Calculate remaining quantities based on configuration (following legacy pattern at lines 1120-1137)
            if (returnByTotalQty) {
                // Total quantity mode - calculate combined remaining quantity
                double remainingTotal = getRemainingTotalQtyToReturn(originalBillItem);
                remainingTotal = Math.max(0.0, remainingTotal);
                returnPhi.setQty(remainingTotal);
                returnPhi.setFreeQty(0.0);
            } else {
                // Separate quantity mode - calculate quantities separately
                double remainingQty = getRemainingQtyToReturn(originalBillItem);
                double remainingFreeQty = getRemainingFreeQtyToReturn(originalBillItem);
                remainingQty = Math.max(0.0, remainingQty);
                remainingFreeQty = Math.max(0.0, remainingFreeQty);
                returnPhi.setQty(remainingQty);
                returnPhi.setFreeQty(remainingFreeQty);
            }

            // Create finance details for the return item (following legacy pattern at lines 1138-1141)
            BillItemFinanceDetails fd = new BillItemFinanceDetails();
            fd.setBillItem(returnBillItem);
            returnBillItem.setBillItemFinanceDetails(fd);

            // Calculate units per pack and set up quantities properly (following legacy pattern at lines 1142-1143)
            pharmacyCostingService.calculateUnitsPerPack(fd);
            pharmacyCostingService.addBillItemFinanceDetailQuantitiesFromPharmaceuticalBillItem(returnPhi, fd);

            // Set the line gross rate (following legacy pattern at lines 1144-1156)
            BigDecimal lineGrossRateForAUnit = getReturnRateForUnits(originalBillItem);
            BigDecimal unitsPerPack = fd.getUnitsPerPack();

            // Ensure both values are not null before multiplication
            if (lineGrossRateForAUnit == null) {
                lineGrossRateForAUnit = BigDecimal.ZERO;
            }
            if (unitsPerPack == null) {
                unitsPerPack = BigDecimal.ONE;
            }

            BigDecimal lineGrossRateAsEntered = lineGrossRateForAUnit.multiply(unitsPerPack);
            fd.setLineGrossRate(lineGrossRateAsEntered);

            // Calculate line total
            calculateLineTotal(returnBillItem);

            // Set relationships
            returnBillItem.setPharmaceuticalBillItem(returnPhi);

            // Only add items that have remaining quantities to return
            if (returnPhi.getQty() > 0 || returnPhi.getFreeQty() > 0) {
                billItems.add(returnBillItem);
            }
        }

        // Calculate total after adding all items
        calculateTotal();
    }

    // Helper method to get return rate for units (following legacy pattern)
    private BigDecimal getReturnRateForUnits(BillItem originalBillItem) {
        if (originalBillItem == null || originalBillItem.getBillItemFinanceDetails() == null) {
            return BigDecimal.ZERO;
        }

        BillItemFinanceDetails originalFd = originalBillItem.getBillItemFinanceDetails();
        
        // Return the purchase rate per unit
        if (originalFd.getLineGrossRate() != null && originalFd.getUnitsPerPack() != null) {
            return originalFd.getLineGrossRate().divide(originalFd.getUnitsPerPack(), 4, BigDecimal.ROUND_HALF_UP);
        } else if (originalBillItem.getPharmaceuticalBillItem() != null) {
            return BigDecimal.valueOf(originalBillItem.getPharmaceuticalBillItem().getPurchaseRateInUnit());
        }

        return BigDecimal.ZERO;
    }

    private void generateBillItemsForUpdate() {
        billItems.clear();

        if (currentBill == null) {
            return;
        }

        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                + "WHERE phi.billItem.bill = :bill "
                + "AND phi.billItem.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("bill", currentBill);
        params.put("ret", false);

        List<PharmaceuticalBillItem> existingItems = pharmaceuticalBillItemFacade.findByJpql(sql, params);

        for (PharmaceuticalBillItem phi : existingItems) {
            BillItem bi = phi.getBillItem();
            bi.setSearialNo(billItems.size());
            billItems.add(bi);
        }

        originalGrn = currentBill.getReferenceBill();
    }

    private void generateBillItemsForApproval() {
        billItems.clear();

        if (requestedBill == null) {
            return;
        }

        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                + "WHERE phi.billItem.bill = :bill "
                + "AND phi.billItem.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("bill", requestedBill);
        params.put("ret", false);

        List<PharmaceuticalBillItem> requestedItems = pharmaceuticalBillItemFacade.findByJpql(sql, params);

        for (PharmaceuticalBillItem requestedPhi : requestedItems) {
            BillItem bi = new BillItem();
            bi.copy(requestedPhi.getBillItem());

            PharmaceuticalBillItem phi = new PharmaceuticalBillItem();
            phi.copy(requestedPhi);
            phi.setBillItem(bi);

            bi.setPharmaceuticalBillItem(phi);
            bi.setSearialNo(billItems.size());
            billItems.add(bi);
        }
    }

    // Auto-complete methods
    public List<Bill> completeGrn(String query) {
        String sql = "SELECT b FROM Bill b "
                + "WHERE b.retired = :ret "
                + "AND b.cancelled = :can "
                + "AND b.billType = :bt "
                + "AND b.institution = :ins "
                + "AND (b.deptId LIKE :q OR b.fromInstitution.name LIKE :q) "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("can", false);
        params.put("bt", BillType.PharmacyGrnBill);
        params.put("ins", sessionController.getInstitution());
        params.put("q", "%" + query + "%");

        return billFacade.findByJpql(sql, params, 50);
    }

    // GRN Return Creation - following legacy pattern
    private void createReturnBillFromGrn(Bill grn) {
        resetBillValues();
        originalGrn = grn;

        // Create return bill
        currentBill = new RefundBill();
        currentBill.setBillType(BillType.PharmacyGrnReturn);
        currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
        currentBill.setReferenceBill(grn);
        currentBill.setToInstitution(grn.getFromInstitution());

        // Generate items from GRN (similar to legacy prepareReturnBill)
        generateItemsFromGrn();
    }

    // GRN Return specific methods (matching legacy pattern)
    public double getAlreadyReturnedQuantity(BillItem referanceBillItem) {
        // Calculate total already returned quantity for this item
        if (referanceBillItem == null) {
            return 0.0;
        }

        String sql = "SELECT SUM(bi.pharmaceuticalBillItem.qty) FROM BillItem bi "
                + "WHERE bi.referanceBillItem = :refBi "
                + "AND bi.bill.billType = :bt "
                + "AND bi.bill.cancelled = :can "
                + "AND bi.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("refBi", referanceBillItem);
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("can", false);
        params.put("ret", false);

        return billItemFacade.findDoubleByJpql(sql, params);
    }

    public double getAlreadyReturnedFreeQuantity(BillItem referanceBillItem) {
        // Calculate total already returned free quantity for this item
        if (referanceBillItem == null) {
            return 0.0;
        }

        String sql = "SELECT SUM(bi.pharmaceuticalBillItem.freeQty) FROM BillItem bi "
                + "WHERE bi.referanceBillItem = :refBi "
                + "AND bi.bill.billType = :bt "
                + "AND bi.bill.cancelled = :can "
                + "AND bi.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("refBi", referanceBillItem);
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("can", false);
        params.put("ret", false);

        return billItemFacade.findDoubleByJpql(sql, params);
    }

    // Comprehensive validation methods based on legacy GrnReturnWithCostingController pattern
    public double getRemainingTotalQtyToReturn(BillItem referanceBillItem) {
        if (referanceBillItem == null || referanceBillItem.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }

        double originalQty = Math.abs(referanceBillItem.getPharmaceuticalBillItem().getQty());
        double originalFreeQty = Math.abs(referanceBillItem.getPharmaceuticalBillItem().getFreeQty());
        double alreadyReturned = getAlreadyReturnedQuantity(referanceBillItem) + getAlreadyReturnedFreeQuantity(referanceBillItem);

        return (originalQty + originalFreeQty) - Math.abs(alreadyReturned);
    }

    public double getRemainingQtyToReturn(BillItem referanceBillItem) {
        if (referanceBillItem == null || referanceBillItem.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }

        double originalQty = Math.abs(referanceBillItem.getPharmaceuticalBillItem().getQty());
        double alreadyReturned = getAlreadyReturnedQuantity(referanceBillItem);

        return originalQty - Math.abs(alreadyReturned);
    }

    public double getRemainingFreeQtyToReturn(BillItem referanceBillItem) {
        if (referanceBillItem == null || referanceBillItem.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }

        double originalFreeQty = Math.abs(referanceBillItem.getPharmaceuticalBillItem().getFreeQty());
        double alreadyReturned = getAlreadyReturnedFreeQuantity(referanceBillItem);

        return originalFreeQty - Math.abs(alreadyReturned);
    }

    // Validation method called during quantity changes (similar to legacy onEdit pattern)
    public boolean validateReturnQuantities(BillItem billItem) {
        if (billItem == null || billItem.getBillItemFinanceDetails() == null || billItem.getReferanceBillItem() == null) {
            return false;
        }

        BillItemFinanceDetails fd = billItem.getBillItemFinanceDetails();
        boolean isValid = true;

        // Get remaining quantities
        double remainingTotalQty = getRemainingTotalQtyToReturn(billItem.getReferanceBillItem());
        double remainingQty = getRemainingQtyToReturn(billItem.getReferanceBillItem());
        double remainingFreeQty = getRemainingFreeQtyToReturn(billItem.getReferanceBillItem());

        // Check configuration options
        boolean returnByTotalQty = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);
        boolean returnByQtyAndFree = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Quantity and Free Quantity", false);

        if (returnByTotalQty) {
            // Total quantity mode - qty + free qty combined
            double currentTotalQty = (fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0);
            if (currentTotalQty > remainingTotalQty) {
                fd.setQuantity(BigDecimal.valueOf(Math.max(0, remainingTotalQty)));
                fd.setFreeQuantity(BigDecimal.ZERO);
                JsfUtil.addErrorMessage("Cannot return more than remaining quantity. Remaining: " + remainingTotalQty);
                isValid = false;
            }
        } else if (returnByQtyAndFree) {
            // Separate quantity and free quantity mode
            double currentQty = (fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0);
            double currentFreeQty = (fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0);

            if (currentQty > remainingQty) {
                fd.setQuantity(BigDecimal.valueOf(Math.max(0, remainingQty)));
                JsfUtil.addErrorMessage("Cannot return more than remaining quantity. Remaining: " + remainingQty);
                isValid = false;
            }
            if (currentFreeQty > remainingFreeQty) {
                fd.setFreeQuantity(BigDecimal.valueOf(Math.max(0, remainingFreeQty)));
                JsfUtil.addErrorMessage("Cannot return more than remaining free quantity. Remaining: " + remainingFreeQty);
                isValid = false;
            }
        }

        // Check if rate modification is allowed
        boolean rateChangeAllowed = configOptionApplicationController.getBooleanValueByKey("Purchase Return - Changing Return Rate is allowed", false);
        if (!rateChangeAllowed && billItem.getReferanceBillItem() != null) {
            // Reset rate to original GRN rate if modification is not allowed
            BigDecimal originalRate = BigDecimal.valueOf(getOriginalPurchaseRate(billItem.getItem()));
            fd.setLineGrossRate(originalRate);
        }

        return isValid;
    }

    // Comprehensive finalization validation
    public boolean validateFinalization() {
        if (currentBill == null || billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items selected for return");
            return false;
        }

        boolean isValid = true;

        // Validate each item
        for (BillItem bi : billItems) {
            if (bi.isRetired()) {
                continue;
            }

            if (!validateReturnQuantities(bi)) {
                isValid = false;
            }

            // Check if item has positive return quantity
            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd != null) {
                double qty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
                double freeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;
                
                if (qty <= 0 && freeQty <= 0) {
                    JsfUtil.addErrorMessage("Item " + bi.getItem().getName() + " must have return quantity greater than zero");
                    isValid = false;
                }
            }
        }

        // Validate return reason is provided
        if (currentBill.getComments() == null || currentBill.getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Return reason is required for finalization");
            isValid = false;
        }

        return isValid;
    }

    // Comprehensive approval validation
    public boolean validateApproval() {
        if (requestedBill == null || billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No return request found for approval");
            return false;
        }

        boolean isValid = true;

        // Check if request is already finalized
        if (requestedBill.getCheckedBy() == null) {
            JsfUtil.addErrorMessage("Return request must be finalized before approval");
            return false;
        }

        // Check if already approved
        if (requestedBill.getReferenceBill() != null && requestedBill.getReferenceBill().getCreater() != null) {
            JsfUtil.addErrorMessage("Return request is already approved");
            return false;
        }

        // Validate each approved item quantities
        for (BillItem bi : billItems) {
            if (bi.isRetired()) {
                continue;
            }

            // Re-validate quantities at approval stage (additional security check)
            if (!validateReturnQuantities(bi)) {
                isValid = false;
            }

            // Check stock availability for return processing
            if (bi.getPharmaceuticalBillItem() != null && bi.getPharmaceuticalBillItem().getStock() != null) {
                double currentStock = bi.getPharmaceuticalBillItem().getStock().getStock();
                double returningQty = bi.getPharmaceuticalBillItem().getQty();
                
                // Stock should be adjusted, but we need to ensure we don't go into impossible negative values
                // This is a business rule check - some institutions may want to prevent returns that would cause negative stock
                boolean allowNegativeStock = configOptionApplicationController.getBooleanValueByKey("Allow Negative Stock in Returns", true);
                if (!allowNegativeStock && (currentStock + returningQty < 0)) {
                    JsfUtil.addErrorMessage("Insufficient stock for item: " + bi.getItem().getName() + 
                                          ". Current: " + currentStock + ", Returning: " + returningQty);
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    public String getReturnRateLabel() {
        return "per Unit";
    }

    // Event handlers for quantity changes (with validation) - following legacy pattern
    public void onReturnRateChange(BillItem bi) {
        // Sync pharmaceutical quantities with finance details
        if (bi.getBillItemFinanceDetails() != null && bi.getPharmaceuticalBillItem() != null) {
            pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(
                bi.getPharmaceuticalBillItem(), bi.getBillItemFinanceDetails());
        }
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    public void onReturningTotalQtyChange(BillItem bi) {
        // Sync pharmaceutical quantities with finance details
        if (bi.getBillItemFinanceDetails() != null && bi.getPharmaceuticalBillItem() != null) {
            pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(
                bi.getPharmaceuticalBillItem(), bi.getBillItemFinanceDetails());
        }
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    public void onReturningQtyChange(BillItem bi) {
        // Sync pharmaceutical quantities with finance details
        if (bi.getBillItemFinanceDetails() != null && bi.getPharmaceuticalBillItem() != null) {
            pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(
                bi.getPharmaceuticalBillItem(), bi.getBillItemFinanceDetails());
        }
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    public void onReturningFreeQtyChange(BillItem bi) {
        // Sync pharmaceutical quantities with finance details
        if (bi.getBillItemFinanceDetails() != null && bi.getPharmaceuticalBillItem() != null) {
            pharmacyCostingService.addPharmaceuticalBillItemQuantitiesFromBillItemFinanceDetailQuantities(
                bi.getPharmaceuticalBillItem(), bi.getBillItemFinanceDetails());
        }
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    private void calculateLineTotal(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
        BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();
        BigDecimal rate = bi.getBillItemFinanceDetails().getLineGrossRate();

        if (qty == null) {
            qty = BigDecimal.ZERO;
        }
        if (freeQty == null) {
            freeQty = BigDecimal.ZERO;
        }
        if (rate == null) {
            rate = BigDecimal.ZERO;
        }

        BigDecimal total = qty.add(freeQty).multiply(rate);
        bi.getBillItemFinanceDetails().setLineGrossTotal(total);
        bi.setNetValue(total.doubleValue());
    }

    // Utility methods
    public void displayItemDetails(BillItem bi) {
        // Implementation for displaying item details
    }

    public double getOriginalPurchaseRate(Item item) {
        if (originalGrn == null) {
            return 0.0;
        }

        PharmaceuticalBillItem originalPhi = findItemInGrn(item, originalGrn);
        return originalPhi != null ? originalPhi.getPurchaseRateInUnit() : 0.0;
    }

    // Methods to populate bill lists for workflow steps
    public void fillGrnReturnsToFinalize() {
        String jpql = "SELECT b FROM RefundBill b "
                + "WHERE b.billType = :bt "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.billedBill IS NULL "
                + "AND b.cancelled = false "
                + "AND b.retired = false "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN_REQUEST);

        grnReturnsToFinalize = billFacade.findByJpql(jpql, params);
        if (grnReturnsToFinalize == null) {
            grnReturnsToFinalize = new ArrayList<>();
        }
    }

    public void fillGrnReturnsToApprove() {
        String jpql = "SELECT b FROM RefundBill b "
                + "WHERE b.billType = :bt "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.billedBill IS NULL "
                + "AND b.cancelled = false "
                + "AND b.retired = false "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN_FINALIZED);

        grnReturnsToApprove = billFacade.findByJpql(jpql, params);
        if (grnReturnsToApprove == null) {
            grnReturnsToApprove = new ArrayList<>();
        }
    }

    public void resetBillValues() {
        currentBill = new RefundBill();
        currentBillItem = new BillItem();
        billItems = new ArrayList<>();
        selectedBillItems = new ArrayList<>();
        selectedItems = new ArrayList<>();
        printPreview = false;
        selectedGrn = null;
        originalGrn = null;
        requestedBill = null;
        approvedBill = null;
    }

    public void makeListNull() {
        // Reset search lists - implementation depends on search requirements
    }

    // Getters and Setters
    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new RefundBill();
        }
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public Bill getRequestedBill() {
        return requestedBill;
    }

    public void setRequestedBill(Bill requestedBill) {
        this.requestedBill = requestedBill;
    }

    public Bill getApprovedBill() {
        return approvedBill;
    }

    public void setApprovedBill(Bill approvedBill) {
        this.approvedBill = approvedBill;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public List<BillItem> getSelectedBillItems() {
        if (selectedBillItems == null) {
            selectedBillItems = new ArrayList<>();
        }
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
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

    public List<BillItem> getSelectedItems() {
        if (selectedItems == null) {
            selectedItems = new ArrayList<>();
        }
        return selectedItems;
    }

    public void setSelectedItems(List<BillItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Bill getSelectedGrn() {
        return selectedGrn;
    }

    public void setSelectedGrn(Bill selectedGrn) {
        this.selectedGrn = selectedGrn;
    }

    public Bill getOriginalGrn() {
        return originalGrn;
    }

    public void setOriginalGrn(Bill originalGrn) {
        this.originalGrn = originalGrn;
    }

    public List<Item> getDealorItems() {
        if (dealorItems == null) {
            dealorItems = new ArrayList<>();
        }
        return dealorItems;
    }

    public void setDealorItems(List<Item> dealorItems) {
        this.dealorItems = dealorItems;
    }

    public List<Bill> getGrnReturnsToFinalize() {
        if (grnReturnsToFinalize == null) {
            grnReturnsToFinalize = new ArrayList<>();
        }
        return grnReturnsToFinalize;
    }

    public void setGrnReturnsToFinalize(List<Bill> grnReturnsToFinalize) {
        this.grnReturnsToFinalize = grnReturnsToFinalize;
    }

    public List<Bill> getGrnReturnsToApprove() {
        if (grnReturnsToApprove == null) {
            grnReturnsToApprove = new ArrayList<>();
        }
        return grnReturnsToApprove;
    }

    public void setGrnReturnsToApprove(List<Bill> grnReturnsToApprove) {
        this.grnReturnsToApprove = grnReturnsToApprove;
    }

    public List<Bill> getFilteredGrnReturnsToFinalize() {
        return filteredGrnReturnsToFinalize;
    }

    public void setFilteredGrnReturnsToFinalize(List<Bill> filteredGrnReturnsToFinalize) {
        this.filteredGrnReturnsToFinalize = filteredGrnReturnsToFinalize;
    }

    public List<Bill> getFilteredGrnReturnsToApprove() {
        return filteredGrnReturnsToApprove;
    }

    public void setFilteredGrnReturnsToApprove(List<Bill> filteredGrnReturnsToApprove) {
        this.filteredGrnReturnsToApprove = filteredGrnReturnsToApprove;
    }
}
