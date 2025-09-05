/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.WebUserController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for Direct Purchase Return Workflow - handles Create, Finalize,
 * and Approve steps
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class DirectPurchaseReturnWorkflowController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(DirectPurchaseReturnWorkflowController.class.getName());

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
    @EJB
    BillService billService;

    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    EnumController enumController;
    @Inject
    private WebUserController webUserController;
    @Inject
    PharmacyController pharmacyController;
    @Inject
    private SearchController searchController;

    @Inject
    private GrnReturnWorkflowController grnReturnWorkflowController;

    // Main properties
    private Bill currentBill;
    private Bill requestedBill;  // For approval process
    private Bill approvedBill;   // For approval process
    private BillItem currentBillItem;
    private List<BillItem> selectedBillItems;
    private List<BillItem> billItems;
    private List<BillItem> selectedItems;  // For approval process
    private boolean printPreview;

    // Direct Purchase Return specific properties
    private Bill selectedDirectPurchase;
    private Bill originalDirectPurchase;
    private List<Item> dealorItems;

    // Bill lists for workflow steps
    private List<Bill> directPurchaseReturnsToFinalize;
    private List<Bill> directPurchaseReturnsToApprove;
    private List<Bill> filteredDirectPurchaseReturnsToFinalize;
    private List<Bill> filteredDirectPurchaseReturnsToApprove;

    @Inject
    PharmacyCalculation pharmacyBillBean;

    // Navigation methods
    public String navigateToCreateDirectPurchaseReturn() {
        grnReturnWorkflowController.setActiveIndex(1);
        resetBillValues();
        makeListNull();
        if (searchController != null) {
            searchController.makeListNull();
        }
        printPreview = false;  // Ensure no print preview when navigating
        return "/pharmacy/pharmacy_direct_purchase_return_request?faces-redirect=true";
    }


    public String navigateToCreateDirectPurchaseReturnFromPurchase() {
        if (selectedDirectPurchase == null) {
            JsfUtil.addErrorMessage("No Direct Purchase selected");
            return "";
        }
        
        // Check for existing unapproved Direct Purchase returns
        if (hasUnapprovedDirectPurchaseReturns()) {
            JsfUtil.addErrorMessage("Cannot create new return. Please approve pending Direct Purchase returns first.");
            return "";
        }
        
        // Follow legacy pattern - create return bill from selected Direct Purchase
        createReturnBillFromDirectPurchase(selectedDirectPurchase);
        printPreview = false;  // Ensure no print preview when creating new return
        return "/pharmacy/pharmacy_direct_purchase_return_form?faces-redirect=true";
    }

    public String navigateToUpdateDirectPurchaseReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Direct Purchase Return selected");
            return "";
        }
        generateBillItemsForUpdate();
        printPreview = false;  // Ensure no print preview when editing
        return "/pharmacy/pharmacy_direct_purchase_return_form?faces-redirect=true";
    }

    public String navigateToFinalizeDirectPurchaseReturn() {
        grnReturnWorkflowController.setActiveIndex(1);
        makeListNull();
        directPurchaseReturnsToFinalize = null;
        filteredDirectPurchaseReturnsToFinalize = null;
        printPreview = false;  // Ensure no print preview when navigating
        return "/pharmacy/pharmacy_direct_purchase_return_list_to_finalize?faces-redirect=true";
    }

    public String navigateToApproveDirectPurchaseReturn() {
        grnReturnWorkflowController.setActiveIndex(1);
        makeListNull();
        directPurchaseReturnsToApprove = null;
        filteredDirectPurchaseReturnsToApprove = null;
        printPreview = false;  // Ensure no print preview when navigating
        return "/pharmacy/pharmacy_direct_purchase_return_list_to_approve?faces-redirect=true";
    }

    public String navigateToDirectPurchaseReturnApproval() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Direct Purchase Return selected for approval");
            return "";
        }

        // Load the bill items for approval review
        generateBillItemsForUpdate();
        printPreview = false;  // Ensure no print preview when navigating

        // Use the same form page for approval - no need for separate page
        return "/pharmacy/pharmacy_direct_purchase_return_form?faces-redirect=true";
    }

    public String navigateToViewDirectPurchaseReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Direct Purchase Return selected for viewing");
            return "";
        }

        // Load the bill with all its items using billService
        try {
            currentBill = billService.reloadBill(currentBill);
            if (currentBill != null && currentBill.getBillItems() != null) {
                // Ensure bill items are loaded for preview
                ensureBillItemsForPreview();
                printPreview = true;
                return "/pharmacy/pharmacy_reprint_direct_purchase_return?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Could not load Direct Purchase Return details");
                return "";
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error loading Direct Purchase Return: " + e.getMessage());
            return "";
        }
    }

    // Core workflow methods
    public void saveRequest() {
        if (!isAuthorized("CREATE", "CreateDirectPurchaseReturn")) {
            return;
        }

        if (currentBill == null) {
            JsfUtil.addErrorMessage("No bill selected to save");
            return;
        }

        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items to save");
            return;
        }

        // Validate stock availability before saving
        if (!validateAllItemsStockAvailability(true)) {
            JsfUtil.addErrorMessage("Cannot save: Stock validation failed. Please correct the quantities and try again.");
            return;
        }

        saveBill(false);
        // Ensure bill items are properly associated for any subsequent operations
        ensureBillItemsForPreview();
        JsfUtil.addSuccessMessage("Direct Purchase Return Request Saved Successfully");
    }

    public void finalizeRequest() {
        if (!isAuthorized("FINALIZE", "FinalizeDirectPurchaseReturn")) {
            return;
        }

        if (currentBill == null) {
            JsfUtil.addErrorMessage("No bill selected to finalize");
            return;
        }

        // Check if comments are provided for finalization
        if (currentBill.getComments() == null || currentBill.getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a reason for return to finalize");
            return;
        }

        // Validate stock availability before finalizing
        if (!validateAllItemsStockAvailability(true)) {
            JsfUtil.addErrorMessage("Cannot finalize: Stock validation failed. Please correct the quantities and try again.");
            return;
        }

        if (validateDirectPurchaseReturn()) {
            // Process zero quantity items before saving
            processZeroQuantityItems();

            saveBill(true);
            // Ensure the bill items are properly associated with the current bill for print preview
            ensureBillItemsForPreview();
            printPreview = true;
            JsfUtil.addSuccessMessage("Direct Purchase Return Request Finalized Successfully");
        }
    }

    public void approve() {
        if (!isAuthorized("APPROVE", "ApproveDirectPurchaseReturn")) {
            return;
        }

        if (currentBill == null) {
            JsfUtil.addErrorMessage("No bill selected to approve");
            return;
        }

        if (sessionController == null || sessionController.getLoggedUser() == null) {
            JsfUtil.addErrorMessage("User session invalid");
            return;
        }

        // Validate stock availability before approving
        if (!validateAllItemsStockAvailability(true)) {
            JsfUtil.addErrorMessage("Cannot approve: Stock validation failed. Please correct the quantities and try again.");
            return;
        }

        if (validateApproval()) {
            // Process zero quantity items before approval
            processZeroQuantityItems();

            // Mark the current bill as completed (approved)
            currentBill.setCompleted(true);
            currentBill.setCompletedBy(sessionController.getLoggedUser());
            currentBill.setCompletedAt(new Date());

            // Save the bill with completed status
            try {
                billFacade.edit(currentBill);
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error saving bill: " + e.getMessage());
                return;
            }

            updateStock();  // Stock handling happens only at approval stage

            // Check if the original Direct Purchase is fully returned and mark it as fullReturned
            Bill originalDirectPurchaseBill = currentBill.getReferenceBill();
            if (originalDirectPurchaseBill != null && isDirectPurchaseFullyReturned(originalDirectPurchaseBill)) {
                originalDirectPurchaseBill.setFullReturned(true);
                originalDirectPurchaseBill.setFullReturnedBy(sessionController.getLoggedUser());
                originalDirectPurchaseBill.setFullReturnedAt(new Date());
                billFacade.edit(originalDirectPurchaseBill);
                JsfUtil.addSuccessMessage("Original Direct Purchase has been fully returned and marked as complete.");
            }

            // Ensure bill items are properly associated for print preview
            ensureBillItemsForPreview();
            printPreview = true;
            JsfUtil.addSuccessMessage("Direct Purchase Return Approved Successfully");
        }
    }

    /**
     * Process items with zero return quantities by retiring them and setting
     * bill reference to null. This ensures they are not displayed in the print
     * and not included in stock processing.
     */
    private void processZeroQuantityItems() {
        if (billItems == null || billItems.isEmpty()) {
            return;
        }

        List<BillItem> itemsToRetire = new ArrayList<>();
        boolean returnByTotalQuantity = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);

        for (BillItem bi : billItems) {
            if (bi == null || bi.isRetired()) {
                continue;
            }

            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd == null) {
                continue;
            }

            double totalReturnQty = 0.0;

            if (returnByTotalQuantity) {
                // Total quantity mode - check combined qty + free qty
                double qty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
                double freeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;
                totalReturnQty = qty + freeQty;
            } else {
                // Separate quantity mode - check individual quantities
                double qty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
                double freeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;
                totalReturnQty = qty + freeQty;
            }

            // If no return quantity, mark for retirement
            if (totalReturnQty <= 0.0) {
                itemsToRetire.add(bi);
            }
        }

        // Process items that need to be retired
        for (BillItem bi : itemsToRetire) {
            try {
                // Mark as retired
                bi.setRetired(true);
                bi.setRetirer(sessionController.getLoggedUser());
                bi.setRetiredAt(new Date());
                bi.setBill(null); // Set bill reference to null as requested

                // If the item already exists in database, update it
                if (bi.getId() != null) {
                    billItemFacade.edit(bi);

                    // Also retire the pharmaceutical bill item
                    if (bi.getPharmaceuticalBillItem() != null && bi.getPharmaceuticalBillItem().getId() != null) {
                        PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
                        phi.setRetired(true);
                        phi.setRetirer(sessionController.getLoggedUser());
                        phi.setRetiredAt(new Date());
                        pharmaceuticalBillItemFacade.edit(phi);
                    }
                }

                LOGGER.log(Level.INFO, "Retired zero quantity return item: {0}",
                        bi.getItem() != null ? bi.getItem().getName() : "Unknown Item");

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error retiring zero quantity item: {0}", e.getMessage());
            }
        }

        // Remove retired items from the current list so they don't appear in print
        billItems.removeAll(itemsToRetire);

        if (!itemsToRetire.isEmpty()) {
            LOGGER.log(Level.INFO, "Processed {0} zero quantity items for retirement", itemsToRetire.size());
        }
    }

    // Bill management methods
    private void saveBill(boolean finalize) {
        boolean isNewBill = (currentBill.getId() == null);

        if (isNewBill) {
            currentBill.setBillType(BillType.PurchaseReturn);
            currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
            currentBill.setInstitution(sessionController.getInstitution());
            currentBill.setDepartment(sessionController.getDepartment());
            currentBill.setCreater(sessionController.getLoggedUser());
            currentBill.setCreatedAt(new Date());

            String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
                    sessionController.getDepartment(),
                    BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
            currentBill.setDeptId(billNumber);
            currentBill.setInsId(billNumber);
        }

        if (finalize) {
            currentBill.setCheckedBy(sessionController.getLoggedUser());
            currentBill.setCheckeAt(new Date());
        }

        calculateTotal();

        // Use create() for new bills, edit() for existing bills
        if (isNewBill) {
            billFacade.create(currentBill);
        } else {
            billFacade.edit(currentBill);
        }

        saveBillItems();
    }

    private void saveBillItems() {
        if (billItems == null || billItems.isEmpty()) {
            return;
        }

        for (BillItem bi : billItems) {
            if (bi == null) {
                continue;
            }

            // Allow items with zero quantities - they may be intentionally set to zero
            // Skip only if item is retired or invalid
            if (bi.isRetired()) {
                continue;
            }

            // Ensure bill reference is set
            bi.setBill(currentBill);

            // Set up pharmaceutical bill item relationship
            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            if (phi != null) {
                phi.setBillItem(bi);
            }

            // Set up finance details relationship
            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd != null) {
                fd.setBillItem(bi);
            }

            // Save entities in correct order
            if (bi.getId() == null) {
                // Set audit fields only for new records
                bi.setCreatedAt(new Date());
                bi.setCreater(sessionController.getLoggedUser());
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

    // Stock handling - only at approval stage
    private void updateStock() {
        for (BillItem bi : billItems) {
            // Skip only retired items or items with truly zero quantities
            if (bi.isRetired()) {
                continue;
            }

            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            double totalQty = phi.getQty() + phi.getFreeQty();

            // Skip stock processing for zero quantity items (no stock impact)
            if (totalQty == 0) {
                continue;
            }

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
    private boolean validateDirectPurchaseReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Direct Purchase Return Bill");
            return false;
        }

        if (originalDirectPurchase == null) {
            JsfUtil.addErrorMessage("Please select a Direct Purchase to return items from");
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

        BillItem newBi = createBillItemFromDirectPurchaseItem(currentBillItem.getItem());
        if (newBi != null) {
            billItems.add(newBi);
            calculateTotal();
        }

        currentBillItem = new BillItem();
    }

    private BillItem createBillItemFromDirectPurchaseItem(Item item) {
        // Find the item in the original Direct Purchase
        PharmaceuticalBillItem originalPhi = findItemInDirectPurchase(item, originalDirectPurchase);
        if (originalPhi == null) {
            JsfUtil.addErrorMessage("Item not found in selected Direct Purchase");
            return null;
        }

        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setSearialNo(billItems.size());
        bi.setReferanceBillItem(originalPhi.getBillItem());  // Link to original bill item

        PharmaceuticalBillItem phi = new PharmaceuticalBillItem();
        phi.copy(originalPhi);
        phi.setBillItem(bi);
        phi.setQty(0.0);  // Default return quantity
        phi.setFreeQty(0.0);  // Default free return quantity

        bi.setPharmaceuticalBillItem(phi);

        return bi;
    }

    private PharmaceuticalBillItem findItemInDirectPurchase(Item item, Bill directPurchase) {
        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                + "WHERE phi.billItem.bill = :directPurchase "
                + "AND phi.billItem.item = :item "
                + "AND phi.billItem.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("directPurchase", directPurchase);
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

    public void deleteReturnItem(BillItem bi) {
        if (bi == null) {
            JsfUtil.addErrorMessage("No item selected for deletion");
            return;
        }

        try {
            String itemName = bi.getItem() != null ? bi.getItem().getName() : "Unknown";

            // If the item is not saved (no ID), simply remove it from the list
            if (bi.getId() == null) {
                billItems.remove(bi);
                JsfUtil.addSuccessMessage("Item removed from return list: " + itemName);
            } else {
                // If already saved, mark as retired and remove from database
                bi.setRetired(true);
                bi.setRetirer(sessionController.getLoggedUser());
                bi.setRetiredAt(new Date());
                bi.setBill(null); // Set bill as null as requested

                // Update the bill item in database
                billItemFacade.edit(bi);

                // Also retire the pharmaceutical bill item
                if (bi.getPharmaceuticalBillItem() != null && bi.getPharmaceuticalBillItem().getId() != null) {
                    PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
                    phi.setRetired(true);
                    phi.setRetirer(sessionController.getLoggedUser());
                    phi.setRetiredAt(new Date());
                    pharmaceuticalBillItemFacade.edit(phi);
                }

                // Remove from current list to refresh the display
                billItems.remove(bi);
                JsfUtil.addSuccessMessage("Item retired and removed from return: " + itemName);
            }

            // Recalculate total after removal
            calculateTotal();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error removing item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureBillItemsForPreview() {
        if (currentBill != null) {
            // Ensure the current bill has its billItems collection properly populated
            // The print preview component expects bill.billItems to be available
            try {
                if (currentBill.getBillItems() == null) {
                    // Initialize the collection if it's null
                    currentBill.setBillItems(new ArrayList<>());
                }

                // Clear and repopulate with current bill items
                currentBill.getBillItems().clear();

                // Add all current bill items that are not retired
                for (BillItem bi : billItems) {
                    if (bi != null && !bi.isRetired()) {
                        currentBill.getBillItems().add(bi);
                    }
                }
            } catch (Exception e) {
                // If there's an issue with the billItems collection, log it but don't fail
                JsfUtil.addErrorMessage("Warning: Could not properly associate items for preview: " + e.getMessage());
            }
        }
    }

    // Calculation methods - Dedicated methods for Direct Purchase Return calculations
    private void calculateTotal() {
        if (currentBill == null) {
            return;
        }

        // Use the comprehensive calculation method that properly handles finance details
        calculateDirectPurchaseReturnTotal();
    }

    /**
     * Dedicated Direct Purchase Return total calculation using
     * BillItemFinanceDetails.lineGrossTotal This ensures consistency with the
     * line-by-line calculations shown in the UI
     */
    private void calculateDirectPurchaseReturnTotal() {
        if (currentBill == null) {
            return;
        }

        BigDecimal returnTotal = BigDecimal.ZERO;
        int itemCount = 0;

        for (BillItem bi : billItems) {
            if (bi == null || bi.isRetired()) {
                continue;
            }

            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd == null) {
                continue;
            }

            BigDecimal lineGrossTotal = fd.getLineGrossTotal();
            if (lineGrossTotal != null) {
                returnTotal = returnTotal.add(lineGrossTotal);
                itemCount++;
                // Also update the individual bill item net value for consistency
                bi.setNetValue(lineGrossTotal.doubleValue());
            }
        }

        // Ensure the bill has finance details
        if (currentBill.getBillFinanceDetails() == null) {
            currentBill.setBillFinanceDetails(new BillFinanceDetails());
            currentBill.getBillFinanceDetails().setBill(currentBill);
        }

        // Set the calculated totals
        currentBill.getBillFinanceDetails().setNetTotal(returnTotal);
        currentBill.getBillFinanceDetails().setGrossTotal(returnTotal);

        // Also set the legacy total fields for backward compatibility
        currentBill.setTotal(returnTotal.doubleValue());
        currentBill.setNetTotal(returnTotal.doubleValue());
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

    private void prepareBillItems(Bill originalDirectPurchaseBill, Bill newDirectPurchaseReturnBill) {
        if (originalDirectPurchaseBill == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        if (originalDirectPurchaseBill.getId() == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        String jpql = "Select p from PharmaceuticalBillItem p where p.billItem.bill.id = :billId";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", originalDirectPurchaseBill.getId());
        List<PharmaceuticalBillItem> pbisOfOriginalDirectPurchaseBill = pharmaceuticalBillItemFacade.findByJpql(jpql, params);
        for (PharmaceuticalBillItem pbiOfBilledBill : pbisOfOriginalDirectPurchaseBill) {
            try {
                String itemName = null;
                Long itemId = null;
                if (pbiOfBilledBill != null && pbiOfBilledBill.getBillItem() != null && pbiOfBilledBill.getBillItem().getItem() != null) {
                    itemName = pbiOfBilledBill.getBillItem().getItem().getName();
                    itemId = pbiOfBilledBill.getBillItem().getItem().getId();
                }
                BillItem newBillItemInReturnBill = new BillItem();
                newBillItemInReturnBill.setQty(0.0);
                newBillItemInReturnBill.setItem(pbiOfBilledBill.getBillItem().getItem());
                newBillItemInReturnBill.setReferanceBillItem(pbiOfBilledBill.getBillItem());
                newBillItemInReturnBill.setBill(newDirectPurchaseReturnBill);

                PharmaceuticalBillItem newPharmaceuticalBillItemInReturnBill = new PharmaceuticalBillItem();
                newPharmaceuticalBillItemInReturnBill.setBillItem(newBillItemInReturnBill);
                newPharmaceuticalBillItemInReturnBill.setItemBatch(pbiOfBilledBill.getItemBatch());
                newPharmaceuticalBillItemInReturnBill.setStock(pbiOfBilledBill.getStock());
                newBillItemInReturnBill.setPharmaceuticalBillItem(newPharmaceuticalBillItemInReturnBill);

                double originalQtyInUnits = pbiOfBilledBill.getQty();
                double originalFreeQtyInUnits = pbiOfBilledBill.getFreeQty();

                boolean returnByTotalQuantity = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);

                if (returnByTotalQuantity) {
                    // Use consistent database query methods
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
                    // Use consistent database query methods
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
                newBillItemFinanceDetailsInReturnBill.setLineGrossRate(lineGrossRateAsEntered);
                calculateLineTotal(newBillItemInReturnBill);
                getBillItems().add(newBillItemInReturnBill);
            } catch (Exception e) {
            }
        }
        calculateDirectPurchaseReturnTotal();
        try {
            BigDecimal netTotal = newDirectPurchaseReturnBill != null && newDirectPurchaseReturnBill.getBillFinanceDetails() != null
                    ? Optional.ofNullable(newDirectPurchaseReturnBill.getBillFinanceDetails().getNetTotal()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            BigDecimal grossTotal = newDirectPurchaseReturnBill != null && newDirectPurchaseReturnBill.getBillFinanceDetails() != null
                    ? Optional.ofNullable(newDirectPurchaseReturnBill.getBillFinanceDetails().getGrossTotal()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
        } catch (Exception ignore) {
            // Keep silent on totals extraction errors, already traced elsewhere
        }
    }

    private void calculateTotalReturnByLineNetTotals(Bill newDirectPurchaseReturnBill) {
        // Use the dedicated calculation method for consistency
        if (newDirectPurchaseReturnBill != null && newDirectPurchaseReturnBill.equals(currentBill)) {
            calculateDirectPurchaseReturnTotal();
        } else {
            // Legacy fallback for different bill calculations
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

            if (newDirectPurchaseReturnBill == null) {
                return;
            }

            if (newDirectPurchaseReturnBill.getBillFinanceDetails() == null) {
                newDirectPurchaseReturnBill.setBillFinanceDetails(new BillFinanceDetails());
                newDirectPurchaseReturnBill.getBillFinanceDetails().setBill(newDirectPurchaseReturnBill);
            }

            newDirectPurchaseReturnBill.getBillFinanceDetails().setNetTotal(returnTotal);
            newDirectPurchaseReturnBill.getBillFinanceDetails().setGrossTotal(returnTotal);

            // Also set legacy fields for backward compatibility
            newDirectPurchaseReturnBill.setTotal(returnTotal.doubleValue());
            newDirectPurchaseReturnBill.setNetTotal(returnTotal.doubleValue());
        }
    }

    private void calculateLineTotalByLineGrossRate(BillItem inputBillItem) {
        // Use the dedicated line calculation method for consistency
        calculateLineTotal(inputBillItem);
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
        params.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);

        Object[] returnedValues = billItemFacade.findSingleAggregate(sql, params);

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

    public BigDecimal getAlreadyReturnedQuantityWhenApproval(BillItem originalBillItem) {
        if (originalBillItem == null) {
            return BigDecimal.ZERO;
        }

        String itemName = originalBillItem.getItem() != null ? originalBillItem.getItem().getName() : "Unknown Item";

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
        params.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);

        Object result = billItemFacade.findSingleScalar(sql, params);
        BigDecimal returnValue = BigDecimal.ZERO;

        if (result != null) {
            returnValue = safeToBigDecimal(result).abs(); // Use absolute value since returns are negative
        } else {
        }

        return returnValue;
    }

    public BigDecimal getAlreadyReturnedFreeQuantityWhenApproval(BillItem originalBillItem) {
        if (originalBillItem == null) {
            return BigDecimal.ZERO;
        }

        String itemName = originalBillItem.getItem() != null ? originalBillItem.getItem().getName() : "Unknown Item";

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
        params.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);

        Object result = billItemFacade.findSingleScalar(sql, params);
        BigDecimal returnValue = BigDecimal.ZERO;

        if (result != null) {
            returnValue = safeToBigDecimal(result).abs(); // Use absolute value since returns are negative
        } else {
        }

        return returnValue;
    }

    private void generateItemsFromDirectPurchase() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        billItems.clear();

        if (originalDirectPurchase == null) {
            JsfUtil.addErrorMessage("No Direct Purchase Bill is selected.");
            return;
        }

        String sql = "SELECT phi FROM PharmaceuticalBillItem phi "
                + "WHERE phi.billItem.bill = :directPurchase "
                + "AND phi.billItem.retired = :ret";

        Map<String, Object> params = new HashMap<>();
        params.put("directPurchase", originalDirectPurchase);
        params.put("ret", false);

        List<PharmaceuticalBillItem> directPurchaseItems = pharmaceuticalBillItemFacade.findByJpql(sql, params);

        // Check configuration options for quantity loading (following legacy pattern)
        boolean returnByTotalQty = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);

        for (PharmaceuticalBillItem directPurchasePhi : directPurchaseItems) {
            BillItem originalBillItem = directPurchasePhi.getBillItem();

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
            returnPhi.setItemBatch(directPurchasePhi.getItemBatch());
            returnPhi.setStock(directPurchasePhi.getStock());

            // Copy rate properties from original Direct Purchase
            returnPhi.setPurchaseRate(directPurchasePhi.getPurchaseRate());
            returnPhi.setPurchaseRatePack(directPurchasePhi.getPurchaseRatePack());
            returnPhi.setPurchaseRateInUnit(directPurchasePhi.getPurchaseRateInUnit());
            returnPhi.setRetailRate(directPurchasePhi.getRetailRate());
            returnPhi.setRetailRatePack(directPurchasePhi.getRetailRatePack());
            returnPhi.setRetailRateInUnit(directPurchasePhi.getRetailRateInUnit());

            // Get original quantities in units for calculation (following legacy pattern)
            double originalQtyInUnits = Math.abs(directPurchasePhi.getQty());
            double originalFreeQtyInUnits = Math.abs(directPurchasePhi.getFreeQty());

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
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
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

        originalDirectPurchase = currentBill.getReferenceBill();
    }

    private void generateBillItemsForApproval() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
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

            // Ensure the original referenceBillItem link is preserved after copy
            bi.setReferanceBillItem(requestedPhi.getBillItem().getReferanceBillItem());

            PharmaceuticalBillItem phi = new PharmaceuticalBillItem();
            phi.copy(requestedPhi);
            phi.setBillItem(bi);

            bi.setPharmaceuticalBillItem(phi);
            bi.setSearialNo(billItems.size());
            billItems.add(bi);
        }
    }

    // Auto-complete methods
    public List<Bill> completeDirectPurchase(String query) {
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
        params.put("bt", BillType.PharmacyPurchaseBill);
        params.put("ins", sessionController.getInstitution());
        params.put("q", "%" + query + "%");

        return billFacade.findByJpql(sql, params, 50);
    }

    // Direct Purchase Return Creation - following legacy pattern
    private void createReturnBillFromDirectPurchase(Bill directPurchase) {
        resetBillValues();
        originalDirectPurchase = directPurchase;

        // Create return bill
        currentBill = new RefundBill();
        currentBill.setBillType(BillType.PurchaseReturn);
        currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
        currentBill.setReferenceBill(directPurchase);
        currentBill.setToInstitution(directPurchase.getFromInstitution());

        // Set bill creation metadata
        currentBill.setCreatedAt(new Date());
        currentBill.setCreater(sessionController.getLoggedUser());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setDepartment(sessionController.getDepartment());

        // Generate items from Direct Purchase (similar to legacy prepareReturnBill)
        // generateItemsFromDirectPurchase(); commented out as this method is NOT working correctly
        prepareBillItems(directPurchase, currentBill);

        // Save the return bill immediately to generate IDs for all items
        // This will make item deletion much more reliable
        try {
            saveBill(false); // Save but don't finalize
            // Ensure bill items are properly associated for any subsequent operations
            ensureBillItemsForPreview();
            JsfUtil.addSuccessMessage("Direct Purchase Return created successfully. You can now modify quantities and remove items as needed.");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error creating Direct Purchase Return: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Direct Purchase Return specific methods (matching legacy pattern)
    public double getAlreadyReturnedQuantity(BillItem referanceBillItem) {
        // Calculate total already returned quantity for this item
        if (referanceBillItem == null) {
            return 0.0;
        }

        String sql = "Select sum(b.billItemFinanceDetails.quantityByUnits) "
                + "from BillItem b where b.referanceBillItem=:refBi "
                + "and b.bill.billType=:bt and b.bill.cancelled=:can "
                + "and b.retired=:ret and b.bill.completed=:comp";

        Map<String, Object> params = new HashMap<>();
        params.put("refBi", referanceBillItem);
        params.put("bt", BillType.PurchaseReturn);
        params.put("can", false);
        params.put("comp", true);
        params.put("ret", false);

        return billItemFacade.findDoubleByJpql(sql, params);
    }

    public double getAlreadyReturnedFreeQuantity(BillItem referanceBillItem) {
        // Calculate total already returned free quantity for this item
        if (referanceBillItem == null) {
            return 0.0;
        }

        String sql = "Select sum(b.billItemFinanceDetails.freeQuantityByUnits) "
                + "from BillItem b where b.referanceBillItem=:refBi "
                + "and b.bill.billType=:bt and b.bill.cancelled=:can "
                + "and b.retired=:ret and b.bill.completed=:comp";

        Map<String, Object> params = new HashMap<>();
        params.put("refBi", referanceBillItem);
        params.put("bt", BillType.PurchaseReturn);
        params.put("can", false);
        params.put("comp", true);
        params.put("ret", false);

        return billItemFacade.findDoubleByJpql(sql, params);
    }

    // Comprehensive validation methods based on legacy DirectPurchaseReturnWithCostingController pattern
    public double getRemainingTotalQtyToReturn(BillItem referanceBillItem) {
        if (referanceBillItem == null || referanceBillItem.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }

        BillItemFinanceDetails originalFd = referanceBillItem.getBillItemFinanceDetails();
        if (originalFd == null) {
            return 0.0;
        }

        BigDecimal originalQtyBD = originalFd.getQuantityByUnits();
        BigDecimal originalFreeQtyBD = originalFd.getFreeQuantityByUnits();

        double originalQty = originalQtyBD != null ? Math.abs(originalQtyBD.doubleValue()) : 0.0;
        double originalFreeQty = originalFreeQtyBD != null ? Math.abs(originalFreeQtyBD.doubleValue()) : 0.0;
        double alreadyReturned = getAlreadyReturnedQuantity(referanceBillItem) + getAlreadyReturnedFreeQuantity(referanceBillItem);

        return (originalQty + originalFreeQty) - Math.abs(alreadyReturned);
    }

    public double getRemainingQtyToReturn(BillItem referanceBillItem) {
        if (referanceBillItem == null || referanceBillItem.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }

        BillItemFinanceDetails originalFd = referanceBillItem.getBillItemFinanceDetails();
        if (originalFd == null) {
            return 0.0;
        }

        BigDecimal originalQtyBD = originalFd.getQuantityByUnits();
        double originalQty = originalQtyBD != null ? Math.abs(originalQtyBD.doubleValue()) : 0.0;
        double alreadyReturned = getAlreadyReturnedQuantity(referanceBillItem);

        return originalQty - Math.abs(alreadyReturned);
    }

    public double getRemainingFreeQtyToReturn(BillItem referanceBillItem) {
        if (referanceBillItem == null || referanceBillItem.getPharmaceuticalBillItem() == null) {
            return 0.0;
        }

        BillItemFinanceDetails originalFd = referanceBillItem.getBillItemFinanceDetails();
        if (originalFd == null) {
            return 0.0;
        }

        BigDecimal originalFreeQtyBD = originalFd.getFreeQuantityByUnits();
        double originalFreeQty = originalFreeQtyBD != null ? Math.abs(originalFreeQtyBD.doubleValue()) : 0.0;
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

        // Get remaining quantities (excluding current transaction to avoid double counting)
        double remainingTotalQty = getRemainingTotalQtyToReturn(billItem.getReferanceBillItem());
        double remainingQty = getRemainingQtyToReturn(billItem.getReferanceBillItem());
        double remainingFreeQty = getRemainingFreeQtyToReturn(billItem.getReferanceBillItem());

        // Validate stock availability - critical check
        if (!validateStockAvailability(billItem, true)) {
            isValid = false;
        }

        // Check configuration options
        boolean returnByTotalQty = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Total Quantity", false);
        boolean returnByQtyAndFree = configOptionApplicationController.getBooleanValueByKey("Purchase Return by Quantity and Free Quantity", false);

        // Check if this is an AMPP item (pack item) that needs unit conversion
        boolean isAmppItem = (billItem.getItem() instanceof Ampp);
        double unitsPerPack = 1.0;
        if (isAmppItem && fd.getUnitsPerPack() != null) {
            unitsPerPack = fd.getUnitsPerPack().doubleValue();
        }

        if (returnByTotalQty) {
            // Total quantity mode - qty + free qty combined
            double currentTotalQty = (fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0);

            // Convert pack quantity to units for AMPP items
            double currentTotalQtyInUnits = isAmppItem ? currentTotalQty * unitsPerPack : currentTotalQty;

            // Allow exact match (>=) instead of just (>)
            if (currentTotalQtyInUnits > remainingTotalQty) {
                // Convert back to packs for AMPP items when setting the corrected value
                double correctedQtyInPacks = isAmppItem ? Math.max(0, remainingTotalQty / unitsPerPack) : Math.max(0, remainingTotalQty);
                fd.setQuantity(BigDecimal.valueOf(correctedQtyInPacks));
                fd.setFreeQuantity(BigDecimal.ZERO);
                JsfUtil.addErrorMessage("Cannot return more than remaining quantity. Remaining: "
                        + (isAmppItem ? (remainingTotalQty / unitsPerPack) + " packs" : remainingTotalQty + " units"));
                isValid = false;
            }
        } else if (returnByQtyAndFree) {
            // Separate quantity and free quantity mode
            double currentQty = (fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0);
            double currentFreeQty = (fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0);

            // Convert pack quantities to units for AMPP items
            double currentQtyInUnits = isAmppItem ? currentQty * unitsPerPack : currentQty;
            double currentFreeQtyInUnits = isAmppItem ? currentFreeQty * unitsPerPack : currentFreeQty;

            // Allow exact match (>=) instead of just (>)
            if (currentQtyInUnits > remainingQty) {
                // Convert back to packs for AMPP items when setting the corrected value
                double correctedQtyInPacks = isAmppItem ? Math.max(0, remainingQty / unitsPerPack) : Math.max(0, remainingQty);
                fd.setQuantity(BigDecimal.valueOf(correctedQtyInPacks));
                JsfUtil.addErrorMessage("Cannot return more than remaining quantity. Remaining: "
                        + (isAmppItem ? (remainingQty / unitsPerPack) + " packs" : remainingQty + " units"));
                isValid = false;
            }
            if (currentFreeQtyInUnits > remainingFreeQty) {
                // Convert back to packs for AMPP items when setting the corrected value
                double correctedFreeQtyInPacks = isAmppItem ? Math.max(0, remainingFreeQty / unitsPerPack) : Math.max(0, remainingFreeQty);
                fd.setFreeQuantity(BigDecimal.valueOf(correctedFreeQtyInPacks));
                JsfUtil.addErrorMessage("Cannot return more than remaining free quantity. Remaining: "
                        + (isAmppItem ? (remainingFreeQty / unitsPerPack) + " packs" : remainingFreeQty + " units"));
                isValid = false;
            }
        }

        // Check if rate modification is allowed
        boolean rateChangeAllowed = configOptionApplicationController.getBooleanValueByKey("Purchase Return - Changing Return Rate is allowed", false);

        if (!rateChangeAllowed && billItem.getReferanceBillItem() != null) {
            // For AMPP items, we need to get the original pack rate, not unit rate
            if (billItem.getItem() instanceof Ampp) {
                // For AMPP: Get original pack rate from the original bill item
                BigDecimal originalPackRate = getOriginalPackRate(billItem.getReferanceBillItem());
                fd.setLineGrossRate(originalPackRate);
            } else {
                // For AMP: Use unit rate as before
                BigDecimal originalRate = BigDecimal.valueOf(getOriginalPurchaseRate(billItem.getItem()));
                fd.setLineGrossRate(originalRate);
            }
        }

        return isValid;
    }

    /**
     * Comprehensive stock validation for Direct Purchase return items Handles
     * multiple rows using the same stock scenario
     *
     * @param billItem The return bill item being validated
     * @param showMessages Whether to show error messages to user
     * @return true if stock is sufficient, false otherwise
     */
    public boolean validateStockAvailability(BillItem billItem, boolean showMessages) {
        if (billItem == null || billItem.getPharmaceuticalBillItem() == null
                || billItem.getPharmaceuticalBillItem().getStock() == null) {
            if (showMessages) {
                JsfUtil.addErrorMessage("Stock information not available for item: "
                        + (billItem != null && billItem.getItem() != null ? billItem.getItem().getName() : "Unknown"));
            }
            return false;
        }

        PharmaceuticalBillItem phi = billItem.getPharmaceuticalBillItem();
        BillItemFinanceDetails fd = billItem.getBillItemFinanceDetails();

        if (fd == null) {
            return true; // No quantities to validate
        }

        double currentStock = phi.getStock().getStock();
        double returnQty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
        double returnFreeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;

        // Convert to units if AMPP item
        boolean isAmppItem = billItem.getItem() instanceof Ampp;
        double unitsPerPack = 1.0;
        if (isAmppItem && fd.getUnitsPerPack() != null) {
            unitsPerPack = fd.getUnitsPerPack().doubleValue();
        }

        double returnQtyInUnits = isAmppItem ? returnQty * unitsPerPack : returnQty;
        double returnFreeQtyInUnits = isAmppItem ? returnFreeQty * unitsPerPack : returnFreeQty;
        double totalReturnQtyInUnits = returnQtyInUnits + returnFreeQtyInUnits;

        // Calculate total stock usage by this item AND other items in the same return using the same stock
        double totalStockUsageFromCurrentReturn = calculateTotalStockUsageFromCurrentReturn(phi.getStock(), billItem);

        // Check if total usage exceeds current stock
        if (totalStockUsageFromCurrentReturn > currentStock) {
            if (showMessages) {
                String itemName = billItem.getItem() != null ? billItem.getItem().getName() : "Unknown";
                String stockBatch = phi.getStock().getItemBatch() != null
                        ? phi.getStock().getItemBatch().getBatchNo() : "Unknown Batch";

                JsfUtil.addErrorMessage("Insufficient stock for " + itemName + " (Batch: " + stockBatch + "). "
                        + "Current stock: " + String.format("%.2f", currentStock)
                        + ", Total return quantity (including other items): " + String.format("%.2f", totalStockUsageFromCurrentReturn));

                // Reset quantity to available stock minus other usages
                double availableForThisItem = Math.max(0, currentStock - (totalStockUsageFromCurrentReturn - totalReturnQtyInUnits));

                if (isAmppItem) {
                    double availableInPacks = availableForThisItem / unitsPerPack;
                    fd.setQuantity(BigDecimal.valueOf(availableInPacks));
                    fd.setFreeQuantity(BigDecimal.ZERO);
                } else {
                    fd.setQuantity(BigDecimal.valueOf(availableForThisItem));
                    fd.setFreeQuantity(BigDecimal.ZERO);
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Calculate total stock usage from current return for the same stock This
     * handles the scenario where multiple items in the return use the same
     * stock
     */
    private double calculateTotalStockUsageFromCurrentReturn(com.divudi.core.entity.pharmacy.Stock stock, BillItem excludeItem) {
        if (billItems == null || billItems.isEmpty() || stock == null) {
            return 0.0;
        }

        double totalUsage = 0.0;

        for (BillItem bi : billItems) {
            if (bi == null || bi.isRetired() || bi.equals(excludeItem)) {
                continue;
            }

            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
            if (phi == null || phi.getStock() == null || !phi.getStock().equals(stock)) {
                continue; // Different stock
            }

            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd == null) {
                continue;
            }

            double returnQty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
            double returnFreeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;

            // Convert to units if AMPP item
            boolean isAmppItem = bi.getItem() instanceof Ampp;
            double unitsPerPack = 1.0;
            if (isAmppItem && fd.getUnitsPerPack() != null) {
                unitsPerPack = fd.getUnitsPerPack().doubleValue();
            }

            double returnQtyInUnits = isAmppItem ? returnQty * unitsPerPack : returnQty;
            double returnFreeQtyInUnits = isAmppItem ? returnFreeQty * unitsPerPack : returnFreeQty;

            totalUsage += (returnQtyInUnits + returnFreeQtyInUnits);
        }

        // Add the current item's usage
        if (excludeItem != null && excludeItem.getBillItemFinanceDetails() != null) {
            BillItemFinanceDetails fd = excludeItem.getBillItemFinanceDetails();
            double returnQty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
            double returnFreeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;

            boolean isAmppItem = excludeItem.getItem() instanceof Ampp;
            double unitsPerPack = 1.0;
            if (isAmppItem && fd.getUnitsPerPack() != null) {
                unitsPerPack = fd.getUnitsPerPack().doubleValue();
            }

            double returnQtyInUnits = isAmppItem ? returnQty * unitsPerPack : returnQty;
            double returnFreeQtyInUnits = isAmppItem ? returnFreeQty * unitsPerPack : returnFreeQty;

            totalUsage += (returnQtyInUnits + returnFreeQtyInUnits);
        }

        return totalUsage;
    }

    /**
     * Validate stock availability for all return items Called before Save,
     * Finalize, and Approve operations
     */
    public boolean validateAllItemsStockAvailability(boolean showMessages) {
        if (billItems == null || billItems.isEmpty()) {
            return true;
        }

        boolean allValid = true;

        for (BillItem bi : billItems) {
            if (bi == null || bi.isRetired()) {
                continue;
            }

            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd == null) {
                continue;
            }

            // Skip items with zero quantities
            double returnQty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
            double returnFreeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;

            if (returnQty <= 0 && returnFreeQty <= 0) {
                continue;
            }

            if (!validateStockAvailability(bi, showMessages)) {
                allValid = false;
            }
        }

        return allValid;
    }

    // Comprehensive finalization validation
    public boolean validateFinalization() {
        if (currentBill == null || billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items selected for return");
            return false;
        }

        boolean isValid = true;
        boolean hasItemsWithQuantities = false;

        // Validate stock availability first - this is critical
        if (!validateAllItemsStockAvailability(true)) {
            JsfUtil.addErrorMessage("Stock validation failed during finalization");
            isValid = false;
        }

        // Validate each item
        for (BillItem bi : billItems) {
            if (bi.isRetired()) {
                continue;
            }

            // Validate return quantities
            if (!validateReturnQuantities(bi)) {
                isValid = false;
            }

            // Check if at least one item has some return quantity
            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd != null) {
                double qty = fd.getQuantity() != null ? fd.getQuantity().doubleValue() : 0.0;
                double freeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity().doubleValue() : 0.0;

                // Accept any positive return quantities (including exact remaining quantities)
                if (qty > 0 || freeQty > 0) {
                    hasItemsWithQuantities = true;
                }
            }
        }

        // At least one item should have return quantities (prevent completely empty returns)
        if (!hasItemsWithQuantities) {
            JsfUtil.addErrorMessage("At least one item must have return quantities greater than zero");
            isValid = false;
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
        if (currentBill == null || billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No return items found for approval");
            return false;
        }

        boolean isValid = true;

        // Check if request is already finalized
        if (currentBill.getCheckedBy() == null) {
            JsfUtil.addErrorMessage("Return request must be finalized before approval");
            return false;
        }

        // Check if already approved
        if (currentBill.isCompleted()) {
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

            // HEALTHCARE INVENTORY VALIDATION: Check if return can be processed without creating impossible stock states
            if (bi.getPharmaceuticalBillItem() != null && bi.getPharmaceuticalBillItem().getStock() != null) {
                double currentStock = bi.getPharmaceuticalBillItem().getStock().getStock();
                double returningQty = bi.getPharmaceuticalBillItem().getQty();

                /*
                 * IMPORTANT BUSINESS LOGIC EXPLANATION:
                 * 
                 * In healthcare inventory systems, stock can legitimately go negative due to:
                 * 1. Emergency medication dispensing (patient safety priority)
                 * 2. Concurrent processes updating stock simultaneously
                 * 3. Data entry timing differences between departments
                 * 
                 * When processing returns:
                 * - Returns INCREASE available stock (items are added back to inventory)
                 * - We validate BEFORE processing to ensure the return won't create impossible states
                 * - The check (currentStock + returningQty < 0) means: "Is current stock so deeply 
                 *   negative that even adding this return quantity back won't make it non-negative?"
                 * 
                 * This prevents processing returns when stock is impossibly negative (e.g., -1000 units
                 * when returning 10 units would still leave -990), which could indicate data corruption.
                 * 
                 * NOTE: This is NOT checking if the return would make stock negative (which would be
                 * currentStock - returningQty < 0). That logic would be incorrect for return processing.
                 */
                boolean allowNegativeStock = configOptionApplicationController.getBooleanValueByKey("Allow Negative Stock in Returns", true);
                
                // Validate that processing this return won't result in an impossible negative stock state
                if (!allowNegativeStock && (currentStock + returningQty < 0)) {
                    JsfUtil.addErrorMessage("Insufficient stock for item: " + bi.getItem().getName()
                            + ". Current: " + currentStock + ", Returning: " + returningQty);
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
        // Use dedicated method for this controller to avoid external service interference
        syncQuantitiesAfterRateChange(bi);
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    /**
     * Dedicated method to sync quantities after rate change without interfering
     * with user-entered rates
     */
    private void syncQuantitiesAfterRateChange(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null || bi.getPharmaceuticalBillItem() == null) {
            return;
        }

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();

        // User has changed the lineGrossRate - this is what we need to preserve
        BigDecimal userEnteredRate = fd.getLineGrossRate();
        String itemName = bi.getItem() != null ? bi.getItem().getName() : "Unknown";
        boolean isAmpp = bi.getItem() instanceof Ampp;

        if (isAmpp) {
            // For AMPP items: User entered rate is per pack, we need to sync unit-based calculations
            BigDecimal quantity = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQuantity = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;
            BigDecimal unitsPerPack = fd.getUnitsPerPack() != null ? fd.getUnitsPerPack() : BigDecimal.ONE;

            // Calculate unit-based quantities
            BigDecimal quantityByUnits = quantity.multiply(unitsPerPack);
            BigDecimal freeQuantityByUnits = freeQuantity.multiply(unitsPerPack);

            // Set calculated unit quantities
            fd.setQuantityByUnits(quantityByUnits);
            fd.setFreeQuantityByUnits(freeQuantityByUnits);

            // Calculate unit rate from pack rate
            BigDecimal ratePerUnit = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                    ? userEnteredRate.divide(unitsPerPack, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;

            // Sync pharmaceutical bill item with calculated unit-based values
            phi.setQty(quantityByUnits.doubleValue());
            phi.setFreeQty(freeQuantityByUnits.doubleValue());
            phi.setPurchaseRateInUnit(ratePerUnit.doubleValue());
            phi.setPurchaseRatePack(userEnteredRate.doubleValue());
            phi.setPurchaseRate(userEnteredRate.doubleValue()); // Pack rate for AMPP

        } else {
            // For AMP items: User entered rate is per unit
            BigDecimal quantity = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQuantity = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;

            // Set unit quantities (same as pack quantities for AMP)
            fd.setQuantityByUnits(quantity);
            fd.setFreeQuantityByUnits(freeQuantity);

            // Sync pharmaceutical bill item with unit-based values
            phi.setQty(quantity.doubleValue());
            phi.setFreeQty(freeQuantity.doubleValue());
            phi.setPurchaseRateInUnit(userEnteredRate.doubleValue());
            phi.setPurchaseRate(userEnteredRate.doubleValue()); // Same for AMP
        }

        // Always preserve the user-entered rate
        fd.setLineGrossRate(userEnteredRate);
    }

    public void onReturningTotalQtyChange(BillItem bi) {
        // Use dedicated sync method to preserve rates
        syncQuantitiesAfterQuantityChange(bi, "TotalQty");
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    public void onReturningQtyChange(BillItem bi) {
        // Use dedicated sync method to preserve rates
        syncQuantitiesAfterQuantityChange(bi, "Qty");
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    public void onReturningFreeQtyChange(BillItem bi) {
        // Use dedicated sync method to preserve rates
        syncQuantitiesAfterQuantityChange(bi, "FreeQty");
        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    /**
     * Dedicated method to sync quantities after quantity change without
     * interfering with user-entered rates
     */
    private void syncQuantitiesAfterQuantityChange(BillItem bi, String changeType) {
        if (bi == null || bi.getBillItemFinanceDetails() == null || bi.getPharmaceuticalBillItem() == null) {
            return;
        }

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();

        // CRITICAL: Store the current rate BEFORE any processing
        BigDecimal existingRate = fd.getLineGrossRate();
        String itemName = bi.getItem() != null ? bi.getItem().getName() : "Unknown";
        boolean isAmpp = bi.getItem() instanceof Ampp;

        if (isAmpp) {
            // For AMPP items: Rate should remain as pack rate
            BigDecimal quantity = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQuantity = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;
            BigDecimal unitsPerPack = fd.getUnitsPerPack() != null ? fd.getUnitsPerPack() : BigDecimal.ONE;

            // Calculate unit-based quantities
            BigDecimal quantityByUnits = quantity.multiply(unitsPerPack);
            BigDecimal freeQuantityByUnits = freeQuantity.multiply(unitsPerPack);

            // Update unit quantities but preserve the rate
            fd.setQuantityByUnits(quantityByUnits);
            fd.setFreeQuantityByUnits(freeQuantityByUnits);

            // Calculate unit rate from existing pack rate
            BigDecimal ratePerUnit = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                    ? existingRate.divide(unitsPerPack, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;

            // Sync pharmaceutical bill item with calculated values
            phi.setQty(quantityByUnits.doubleValue());
            phi.setFreeQty(freeQuantityByUnits.doubleValue());
            phi.setPurchaseRateInUnit(ratePerUnit.doubleValue());
            phi.setPurchaseRatePack(existingRate.doubleValue());
            phi.setPurchaseRate(existingRate.doubleValue()); // Pack rate for AMPP

        } else {
            // For AMP items: Rate is per unit
            BigDecimal quantity = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQuantity = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;

            // Update unit quantities (same as pack quantities for AMP)
            fd.setQuantityByUnits(quantity);
            fd.setFreeQuantityByUnits(freeQuantity);

            // Sync pharmaceutical bill item with values
            phi.setQty(quantity.doubleValue());
            phi.setFreeQty(freeQuantity.doubleValue());
            phi.setPurchaseRateInUnit(existingRate.doubleValue());
            phi.setPurchaseRate(existingRate.doubleValue());
        }

        // CRITICAL: Restore the original rate to prevent external service interference
        fd.setLineGrossRate(existingRate);
    }

    /**
     * Dedicated line total calculation for Direct Purchase Return items Uses
     * proper quantity and rate calculations without external service
     * interference
     */
    private void calculateLineTotal(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        BigDecimal qty = fd.getQuantity();
        BigDecimal freeQty = fd.getFreeQuantity();
        BigDecimal rate = fd.getLineGrossRate();

        String itemName = bi.getItem() != null ? bi.getItem().getName() : "Unknown";
        boolean isAmpp = bi.getItem() instanceof Ampp;

        if (qty == null) {
            qty = BigDecimal.ZERO;
        }
        if (freeQty == null) {
            freeQty = BigDecimal.ZERO;
        }
        if (rate == null) {
            rate = BigDecimal.ZERO;
        }

        // For Direct Purchase returns, line total = (quantity + free quantity) × rate
        BigDecimal totalQty = qty.add(freeQty);
        BigDecimal lineTotal = totalQty.multiply(rate);

        // Set the calculated line total
        fd.setLineGrossTotal(lineTotal);
        fd.setLineNetTotal(lineTotal);
        bi.setNetValue(lineTotal.doubleValue());

    }

    // Utility methods
    private boolean hasUnapprovedDirectPurchaseReturns() {
        if (selectedDirectPurchase == null) {
            return false;
        }

        String jpql = "SELECT COUNT(b) FROM RefundBill b "
                + "WHERE b.billType = :bt "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.referenceBill = :refBill "
                + "AND b.checkedBy IS NOT NULL "
                + "AND (b.completed = false OR b.completed IS NULL) "
                + "AND b.cancelled = false "
                + "AND b.retired = false";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PurchaseReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
        params.put("refBill", selectedDirectPurchase);

        Long count = billFacade.findLongByJpql(jpql, params);
        return count != null && count > 0;
    }

    public void displayItemDetails(BillItem bi) {
        if (bi == null || bi.getItem() == null) {
            return;
        }
        pharmacyController.fillItemDetails(bi.getItem());
    }

    public double getOriginalPurchaseRate(Item item) {
        if (originalDirectPurchase == null) {
            return 0.0;
        }

        PharmaceuticalBillItem originalPhi = findItemInDirectPurchase(item, originalDirectPurchase);
        return originalPhi != null ? originalPhi.getPurchaseRateInUnit() : 0.0;
    }

    /**
     * Get the original pack rate for AMPP items from the original Direct
     * Purchase
     */
    private BigDecimal getOriginalPackRate(BillItem originalBillItem) {
        if (originalBillItem == null || originalBillItem.getBillItemFinanceDetails() == null) {
            return BigDecimal.ZERO;
        }

        BillItemFinanceDetails originalFd = originalBillItem.getBillItemFinanceDetails();

        // For AMPP items, the lineGrossRate in the original bill should be the pack rate
        if (originalFd.getLineGrossRate() != null) {
            return originalFd.getLineGrossRate();
        }

        // Fallback: Calculate pack rate from unit rate and units per pack
        PharmaceuticalBillItem originalPhi = originalBillItem.getPharmaceuticalBillItem();
        if (originalPhi != null && originalFd.getUnitsPerPack() != null) {
            BigDecimal unitRate = BigDecimal.valueOf(originalPhi.getPurchaseRateInUnit());
            BigDecimal unitsPerPack = originalFd.getUnitsPerPack();
            BigDecimal packRate = unitRate.multiply(unitsPerPack);
            return packRate;
        }

        return BigDecimal.ZERO;
    }

    // Methods to populate bill lists for workflow steps
    public void fillDirectPurchaseReturnsToFinalize() {
        // Find draft/saved bills that need to be finalized
        // Draft = no checkedBy (not yet finalized)
        // Need finalization = ready to be checked and finalized
        String jpql = "SELECT b FROM RefundBill b "
                + "WHERE b.billType = :bt "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.checkedBy IS NULL "
                + "AND b.cancelled = false "
                + "AND b.retired = false "
                + "AND b.department = :dept "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PurchaseReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
        params.put("dept", sessionController.getDepartment());

        directPurchaseReturnsToFinalize = billFacade.findByJpql(jpql, params);
        if (directPurchaseReturnsToFinalize == null) {
            directPurchaseReturnsToFinalize = new ArrayList<>();
        }
    }

    public void fillDirectPurchaseReturnsToApprove() {
        // Ready for approval = completed = false (not yet approved)
        String jpql = "SELECT b FROM RefundBill b "
                + "WHERE b.billType = :bt "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.checkedBy IS NOT NULL "
                + "AND (b.completed = false OR b.completed IS NULL) "
                + "AND b.cancelled = false "
                + "AND b.retired = false "
                + "AND b.department = :dept "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PurchaseReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
        params.put("dept", sessionController.getDepartment());

        directPurchaseReturnsToApprove = billFacade.findByJpql(jpql, params);
        if (directPurchaseReturnsToApprove == null) {
            directPurchaseReturnsToApprove = new ArrayList<>();
        }
    }

    // In DirectPurchaseReturnWorkflowController.java
    public void fillApprovedDirectPurchaseReturns() {
        String jpql = "SELECT b FROM RefundBill b "
                + "WHERE b.billType = :bt "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.completed = true "
                + "AND b.cancelled = false "
                + "AND b.retired = false "
                + "AND b.department = :dept "
                + "ORDER BY b.createdAt DESC";
        Map<String, Object> p = new HashMap<>();
        p.put("bt", BillType.PurchaseReturn);
        p.put("bta", BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
        p.put("dept", sessionController.getDepartment());
        directPurchaseReturnsToApprove = billFacade.findByJpql(jpql, p);
        if (directPurchaseReturnsToApprove == null) {
            directPurchaseReturnsToApprove = new ArrayList<>();
        }
    }

    public void resetBillValues() {
        currentBill = new RefundBill();
        currentBillItem = new BillItem();
        billItems = new ArrayList<>();
        selectedBillItems = new ArrayList<>();
        selectedItems = new ArrayList<>();
        printPreview = false;
        selectedDirectPurchase = null;
        originalDirectPurchase = null;
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

    public Bill getSelectedDirectPurchase() {
        return selectedDirectPurchase;
    }

    public void setSelectedDirectPurchase(Bill selectedDirectPurchase) {
        this.selectedDirectPurchase = selectedDirectPurchase;
    }

    public Bill getOriginalDirectPurchase() {
        return originalDirectPurchase;
    }

    public void setOriginalDirectPurchase(Bill originalDirectPurchase) {
        this.originalDirectPurchase = originalDirectPurchase;
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

    public List<Bill> getDirectPurchaseReturnsToFinalize() {
        if (directPurchaseReturnsToFinalize == null) {
            directPurchaseReturnsToFinalize = new ArrayList<>();
        }
        return directPurchaseReturnsToFinalize;
    }

    public void setDirectPurchaseReturnsToFinalize(List<Bill> directPurchaseReturnsToFinalize) {
        this.directPurchaseReturnsToFinalize = directPurchaseReturnsToFinalize;
    }

    public List<Bill> getDirectPurchaseReturnsToApprove() {
        if (directPurchaseReturnsToApprove == null) {
            directPurchaseReturnsToApprove = new ArrayList<>();
        }
        return directPurchaseReturnsToApprove;
    }

    public void setDirectPurchaseReturnsToApprove(List<Bill> directPurchaseReturnsToApprove) {
        this.directPurchaseReturnsToApprove = directPurchaseReturnsToApprove;
    }

    public List<Bill> getFilteredDirectPurchaseReturnsToFinalize() {
        return filteredDirectPurchaseReturnsToFinalize;
    }

    public void setFilteredDirectPurchaseReturnsToFinalize(List<Bill> filteredDirectPurchaseReturnsToFinalize) {
        this.filteredDirectPurchaseReturnsToFinalize = filteredDirectPurchaseReturnsToFinalize;
    }

    public List<Bill> getFilteredDirectPurchaseReturnsToApprove() {
        return filteredDirectPurchaseReturnsToApprove;
    }

    public void setFilteredDirectPurchaseReturnsToApprove(List<Bill> filteredDirectPurchaseReturnsToApprove) {
        this.filteredDirectPurchaseReturnsToApprove = filteredDirectPurchaseReturnsToApprove;
    }

    /**
     * Checks if a Direct Purchase is fully returned by comparing original
     * quantities with returned quantities for all items in the Direct Purchase.
     * Only considers completed (approved) returns.
     *
     * @param directPurchaseBill The original Direct Purchase bill to check
     * @return true if all items are fully returned, false otherwise
     */
    private boolean isDirectPurchaseFullyReturned(Bill directPurchaseBill) {
        if (directPurchaseBill == null) {
            return false;
        }

        // Get all bill items from the original Direct Purchase
        String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId AND bi.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", directPurchaseBill.getId());
        List<BillItem> originalBillItems = billItemFacade.findByJpql(jpql, params);

        if (originalBillItems == null || originalBillItems.isEmpty()) {
            return false;
        }

        // Check each item to see if it's fully returned
        for (BillItem originalBillItem : originalBillItems) {
            if (originalBillItem.getPharmaceuticalBillItem() == null) {
                continue;
            }

            // Get original quantities using unit-based values for consistency
            BillItemFinanceDetails originalFd = originalBillItem.getBillItemFinanceDetails();
            if (originalFd == null) {
                continue;
            }

            // Use BigDecimal for precise calculations like the legacy method
            BigDecimal originalQty = safeToBigDecimal(originalFd.getQuantityByUnits());
            BigDecimal originalFreeQty = safeToBigDecimal(originalFd.getFreeQuantityByUnits());

            // Get already returned quantities using the same methods as legacy
            // (includes current transaction since return bill is already saved as completed)
            BigDecimal returnedQty = getAlreadyReturnedQuantityWhenApproval(originalBillItem);
            BigDecimal returnedFreeQty = getAlreadyReturnedFreeQuantityWhenApproval(originalBillItem);

            // Calculate remaining quantities using BigDecimal for precision
            BigDecimal remainingQty = originalQty.subtract(returnedQty);
            BigDecimal remainingFreeQty = originalFreeQty.subtract(returnedFreeQty);

            // For total quantity mode, check total remaining instead of individual qty/free qty
            BigDecimal originalTotal = originalQty.add(originalFreeQty);
            BigDecimal returnedTotal = returnedQty.add(returnedFreeQty);
            BigDecimal remainingTotal = originalTotal.subtract(returnedTotal);

            // Check if item is fully returned - use total quantity comparison for more accurate results
            boolean isItemFullyReturned = remainingTotal.compareTo(BigDecimal.ZERO) <= 0;

            if (!isItemFullyReturned) {
                return false;
            }
        }

        // All items are fully returned
        return true;
    }

    /**
     * Authorization helper method to check Direct Purchase Return privileges
     * and audit denied access
     *
     * @param action The action being attempted (CREATE, FINALIZE, APPROVE)
     * @param requiredPrivilege The specific privilege required
     * @return true if authorized, false if not
     */
    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null, billId={1}",
                    new Object[]{action, currentBill != null ? currentBill.getId() : "null"});
            return false;
        }

        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            // Audit denied access attempt
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billId = currentBill != null ? currentBill.getId() : null;

            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized Direct Purchase Return access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});

            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " Direct Purchase return requests.");
            return false;
        }

        return true;
    }

}
