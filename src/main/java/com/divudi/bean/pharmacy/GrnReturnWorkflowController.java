/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
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
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Payment;
import com.divudi.service.PaymentService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @EJB
    BillService billService;
    @EJB
    PaymentService paymentService;

    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    EnumController enumController;
    @Inject
    private WebUserController webUserController;
    @Inject
    PharmacyController pharmacyController;
    @Inject
    private SearchController searchController;

    // Main properties
    private Bill currentBill;
    private Bill requestedBill;  // For approval process
    private Bill approvedBill;   // For approval process
    private BillItem currentBillItem;
    private List<BillItem> selectedBillItems;
    private List<BillItem> billItems;
    private List<BillItem> selectedItems;  // For approval process
    private boolean printPreview;
    private PaymentMethodData paymentMethodData;

    // GRN Return specific properties
    private Bill selectedGrn;
    private Bill originalGrn;
    private List<Item> dealorItems;

    // Bill lists for workflow steps
    private List<Bill> grnReturnsToFinalize;
    private List<Bill> grnReturnsToApprove;
    private List<Bill> filteredGrnReturnsToFinalize;
    private List<Bill> filteredGrnReturnsToApprove;

    private Integer activeIndex;
    private Integer activeIndexForReturnsAndCancellations;

    @Inject
    PharmacyCalculation pharmacyBillBean;

    // Navigation methods
    public String navigateToCreateGrnReturn() {
        activeIndex = 0;
        resetBillValues();
        makeListNull();
        if (searchController != null) {
            searchController.makeListNull();
        }
        printPreview = false;  // Ensure no print preview when navigating
        return "/pharmacy/pharmacy_grn_return_request?faces-redirect=true";
    }

    public String navigateToCreateGrnReturnFromGrn() {
        if (selectedGrn == null) {
            JsfUtil.addErrorMessage("No GRN selected");
            return "";
        }

        // Check for existing unapproved GRN returns
        if (hasUnapprovedGrnReturns()) {
            JsfUtil.addErrorMessage("Cannot create new return. Please approve pending GRN returns first.");
            return "";
        }

        // Follow legacy pattern - create return bill from selected GRN
        createReturnBillFromGrn(selectedGrn);
        printPreview = false;  // Ensure no print preview when creating new return
        return "/pharmacy/pharmacy_grn_return_form?faces-redirect=true";
    }

    public String navigateToUpdateGrnReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No GRN Return selected");
            return "";
        }
        generateBillItemsForUpdate();
        printPreview = false;  // Ensure no print preview when editing
        return "/pharmacy/pharmacy_grn_return_form?faces-redirect=true";
    }

    public String navigateToFinalizeGrnReturn() {
        activeIndex = 0;
        makeListNull();
        grnReturnsToFinalize = null;
        filteredGrnReturnsToFinalize = null;
        printPreview = false;  // Ensure no print preview when navigating
        return "/pharmacy/pharmacy_grn_return_list_to_finalize?faces-redirect=true";
    }

    public String navigateToApproveGrnReturn() {
        activeIndex = 0;
        makeListNull();
        grnReturnsToApprove = null;
        filteredGrnReturnsToApprove = null;
        printPreview = false;  // Ensure no print preview when navigating
        return "/pharmacy/pharmacy_grn_return_list_to_approve?faces-redirect=true";
    }

    public String navigateToGrnReturnApproval() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No GRN Return selected for approval");
            return "";
        }

        // Load the bill items for approval review
        generateBillItemsForUpdate();
        printPreview = false;  // Ensure no print preview when navigating

        // Use the same form page for approval - no need for separate page
        return "/pharmacy/pharmacy_grn_return_form?faces-redirect=true";
    }

    public String navigateToViewGrnReturn() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No GRN Return selected for viewing");
            return "";
        }

        // Load the bill with all its items using billService
        try {
            currentBill = billService.reloadBill(currentBill);
            if (currentBill != null && currentBill.getBillItems() != null) {
                // Ensure bill items are loaded for preview
                ensureBillItemsForPreview();
                printPreview = true;
                return "/pharmacy/pharmacy_reprint_grn_return?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Could not load GRN Return details");
                return "";
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error loading GRN Return: " + e.getMessage());
            return "";
        }
    }

    // Core workflow methods
    public void saveRequest() {
        if (!isAuthorized("CREATE", "CreateGrnReturn")) {
            return;
        }

        if (currentBill == null) {
            JsfUtil.addErrorMessage("No bill selected to save");
            return;
        }

//        if (billItems == null || billItems.isEmpty()) {
//            JsfUtil.addErrorMessage("No items to save");
//            return;
//        }
//        
//        // Validate stock availability before saving
//        if (!validateAllItemsStockAvailability(true)) {
//            JsfUtil.addErrorMessage("Cannot save: Stock validation failed. Please correct the quantities and try again.");
//            return;
//        }
        saveBill(false);
        // Ensure bill items are properly associated for any subsequent operations
        ensureBillItemsForPreview();
        JsfUtil.addSuccessMessage("GRN Return Request Saved Successfully");
    }

    public void finalizeRequest() {
        if (!isAuthorized("FINALIZE", "FinalizeGrnReturn")) {
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

        // Validate payment method
        if (!validatePaymentMethod()) {
            return;
        }

        // Pre-validate payment creation to prevent finalization with payment issues
        if (!validatePaymentCreation()) {
            return;
        }

        // Validate stock availability before finalizing
        if (!validateAllItemsStockAvailability(true)) {
            JsfUtil.addErrorMessage("Cannot finalize: Stock validation failed. Please correct the quantities and try again.");
            return;
        }

        if (validateGrnReturn()) {
            // Process zero quantity items before saving
            processZeroQuantityItems();

            saveBill(true);
            // Ensure the bill items are properly associated with the current bill for print preview
            ensureBillItemsForPreview();
            printPreview = true;
            JsfUtil.addSuccessMessage("GRN Return Request Finalized Successfully");
        }
    }

    public void approve() {
        if (!isAuthorized("APPROVE", "ApproveGrnReturn")) {
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

        // Validate payment method
        if (!validatePaymentMethod()) {
            return;
        }

        // Pre-validate payment creation to prevent approval with payment issues
        if (!validatePaymentCreation()) {
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

            // Create payment for the return - ALL payment methods require payment records for healthcare compliance
            // Payment validation was performed at method start, so we can proceed with confidence
            if (currentBill.getPaymentMethod() != null) {
                try {
                    List<Payment> returnPayments = paymentService.createPayment(currentBill, getPaymentMethodData());
                    if (returnPayments != null && !returnPayments.isEmpty()) {
                        JsfUtil.addSuccessMessage("Payment created successfully for GRN return.");
                    } else {
                        // This should not happen since validation was done upfront, but handle defensively
                        String errorMsg = "Unexpected payment creation failure - no payments were created for "
                                + currentBill.getPaymentMethod().getLabel();
                        JsfUtil.addErrorMessage(errorMsg);
                        LOGGER.log(Level.SEVERE, errorMsg + " for bill: " + currentBill.getInsId()
                                + " - This should not occur after successful validation");
                    }
                } catch (Exception e) {
                    // This should not happen since validation was done upfront, but handle defensively
                    String errorMsg = "Unexpected error creating payment for GRN return: " + e.getMessage();
                    JsfUtil.addErrorMessage(errorMsg);
                    LOGGER.log(Level.SEVERE, errorMsg + " - This should not occur after successful validation", e);
                }
            }

            // Check if the original GRN is fully returned and mark it as fullReturned
            Bill originalGrnBill = currentBill.getReferenceBill();
            if (originalGrnBill != null && isGrnFullyReturned(originalGrnBill)) {
                originalGrnBill.setFullReturned(true);
                originalGrnBill.setFullReturnedBy(sessionController.getLoggedUser());
                originalGrnBill.setFullReturnedAt(new Date());
                billFacade.edit(originalGrnBill);
                JsfUtil.addSuccessMessage("Original GRN has been fully returned and marked as complete.");
            }

            // Ensure bill items are properly associated for print preview
            ensureBillItemsForPreview();
            printPreview = true;
            JsfUtil.addSuccessMessage("GRN Return Approved Successfully");
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
                double qty = fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0;
                double freeQty = fd.getFreeQuantity() != null ? Math.abs(fd.getFreeQuantity().doubleValue()) : 0.0;
                totalReturnQty = qty + freeQty;
            } else {
                // Separate quantity mode - check individual quantities
                double qty = fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0;
                double freeQty = fd.getFreeQuantity() != null ? Math.abs(fd.getFreeQuantity().doubleValue()) : 0.0;
                totalReturnQty = qty + freeQty;
            }

            // If no return quantity, mark for retirement (use == 0.0 since we use absolute values)
            if (totalReturnQty == 0.0) {
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
            // Set default suffix for GRN Return if not already set
            String billNumberSuffix = configOptionApplicationController.getShortTextValueByKey("Bill Number Suffix for GRN Return", "GRNR");
            if (billNumberSuffix == null || billNumberSuffix.trim().isEmpty()) {
                billNumberSuffix = "GRNR";
            }

            currentBill.setBillType(BillType.PharmacyGrnReturn);
            currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_RETURN);
            currentBill.setInstitution(sessionController.getInstitution());
            currentBill.setDepartment(sessionController.getDepartment());
            currentBill.setCreater(sessionController.getLoggedUser());
            currentBill.setCreatedAt(new Date());

            // Get configuration options for bill numbering strategies
            boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Dept Ins Year Count", false);
            boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Ins Year Count", false);
            boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution ID is Prefix Ins Year Count", false);

            String billId;

            // Independent department ID generation
            if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN_RETURN);
                currentBill.setDeptId(billId);
            } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN_RETURN);
                currentBill.setDeptId(billId);
            } else {
                String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
                        sessionController.getDepartment(),
                        BillTypeAtomic.PHARMACY_GRN_RETURN);
                currentBill.setDeptId(billNumber);
            }

            // Independent institution ID generation
            if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount) {
                billId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN_RETURN);
                currentBill.setInsId(billId);
            } else {
                String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
                        sessionController.getDepartment(),
                        BillTypeAtomic.PHARMACY_GRN_RETURN);
                currentBill.setInsId(billNumber);
            }
        }

        if (finalize) {
            currentBill.setCheckedBy(sessionController.getLoggedUser());
            currentBill.setCheckeAt(new Date());
        }

        calculateTotal();

        // Use create() for new bills, edit() for existing bills
        if (currentBill.getId() == null) {
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

            // For returns: make quantities negative before saving, use absolute value for stock deduction
            double absQty = Math.abs(totalQty);
            phi.setQty(-Math.abs(phi.getQty()));
            phi.setFreeQty(-Math.abs(phi.getFreeQty()));

            // Save the pharmaceutical bill item with negative quantities
            pharmaceuticalBillItemFacade.edit(phi);

            // Deduct from stock for return (use absolute value)
            boolean returnFlag = pharmacyBean.deductFromStock(
                    phi.getStock(),
                    absQty,
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

        // After stock update, negate the bill-level finance details for returns (stock moving out)
        if (currentBill != null && currentBill.getBillFinanceDetails() != null) {
            BillFinanceDetails bfd = currentBill.getBillFinanceDetails();

            // Negate the purchase, cost, and retail values at bill level
            if (bfd.getTotalPurchaseValue() != null) {
                bfd.setTotalPurchaseValue(bfd.getTotalPurchaseValue().abs().negate());
            }
            if (bfd.getTotalCostValue() != null) {
                bfd.setTotalCostValue(bfd.getTotalCostValue().abs().negate());
            }
            if (bfd.getTotalRetailSaleValue() != null) {
                bfd.setTotalRetailSaleValue(bfd.getTotalRetailSaleValue().abs().negate());
            }

            // Save the updated bill with corrected finance details
            billFacade.edit(currentBill);
        }
    }

    // Validation methods
    private boolean validatePaymentMethod() {
        if (currentBill == null) {
            return false;
        }

        // Check if payment method is selected
        if (currentBill.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return false;
        }

        // If payment method is not Cash, validate payment method details
        if (!currentBill.getPaymentMethod().equals(PaymentMethod.Cash)) {
            PaymentMethodData pmd = getPaymentMethodData();

            if (currentBill.getPaymentMethod().equals(PaymentMethod.Card)) {
                if (pmd.getCreditCard() == null
                        || pmd.getCreditCard().getNo() == null || pmd.getCreditCard().getNo().trim().isEmpty()) {
                    JsfUtil.addErrorMessage("Please enter credit card details");
                    return false;
                }
            } else if (currentBill.getPaymentMethod().equals(PaymentMethod.Cheque)) {
                if (pmd.getCheque() == null
                        || pmd.getCheque().getNo() == null || pmd.getCheque().getNo().trim().isEmpty()) {
                    JsfUtil.addErrorMessage("Please enter cheque details");
                    return false;
                }
            } else if (currentBill.getPaymentMethod().equals(PaymentMethod.Slip)) {
                if (pmd.getSlip() == null
                        || pmd.getSlip().getNo() == null || pmd.getSlip().getNo().trim().isEmpty()) {
                    JsfUtil.addErrorMessage("Please enter slip details");
                    return false;
                }
            } else if (currentBill.getPaymentMethod().equals(PaymentMethod.ewallet)) {
                if (pmd.getEwallet() == null
                        || pmd.getEwallet().getNo() == null || pmd.getEwallet().getNo().trim().isEmpty()) {
                    JsfUtil.addErrorMessage("Please enter e-wallet details");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Pre-validates payment creation to prevent approval/finalization with
     * payment issues This ensures healthcare financial integrity by catching
     * payment problems early
     */
    private boolean validatePaymentCreation() {
        if (currentBill == null || currentBill.getPaymentMethod() == null) {
            return true; // If no payment method, no payment creation needed
        }

        try {
            // Test payment creation without actually creating the payment
            // This validates that payment service can handle the current payment method and data
            PaymentMethodData pmd = getPaymentMethodData();
            if (pmd == null) {
                JsfUtil.addErrorMessage("Payment method data not available for validation");
                return false;
            }

            // For healthcare compliance, all payment methods including Cash must have valid payment data
            // This early validation prevents partial transactions that could affect audit trails
            if (paymentService == null) {
                JsfUtil.addErrorMessage("Payment service not available - cannot proceed with financial transaction");
                return false;
            }

            // Additional validation for payment method data completeness
            if (currentBill.getNetTotal() <= 0) {
                JsfUtil.addErrorMessage("Invalid bill total for payment creation");
                return false;
            }

            return true;
        } catch (Exception e) {
            String errorMsg = "Payment validation failed: " + e.getMessage();
            JsfUtil.addErrorMessage(errorMsg);
            LOGGER.log(Level.SEVERE, "Pre-validation of payment creation failed for bill: " + currentBill.getInsId(), e);
            return false;
        }
    }

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
                bi.setBill(null);
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
                // Remove from Bill's bill item list
                currentBill.getBillItems().remove(bi);
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

    // Calculation methods - Dedicated methods for GRN Return calculations
    private void calculateTotal() {
        if (currentBill == null) {
            return;
        }

        // Use the comprehensive calculation method that properly handles finance details
        calculateGrnReturnTotal();
    }

    /**
     * Dedicated GRN Return total calculation using
     * BillItemFinanceDetails.lineGrossTotal This ensures consistency with the
     * line-by-line calculations shown in the UI
     */
    private void calculateGrnReturnTotal() {
        if (currentBill == null) {
            return;
        }

        BigDecimal returnTotal = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalRetailValue = BigDecimal.ZERO;
        int itemCount = 0;

        for (BillItem bi : billItems) {
            if (bi == null || bi.isRetired()) {
                continue;
            }

            BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
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

            // Calculate purchase, cost, and retail values from PharmaceuticalBillItem
            if (phi != null) {
                // Sum up the purchase, cost, and retail values
                totalPurchaseValue = totalPurchaseValue.add(BigDecimal.valueOf(phi.getPurchaseValue()));
                totalCostValue = totalCostValue.add(BigDecimal.valueOf(phi.getCostValue()));
                totalRetailValue = totalRetailValue.add(BigDecimal.valueOf(phi.getRetailValue()));
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

        // Set purchase, cost, and retail sale values
        currentBill.getBillFinanceDetails().setTotalPurchaseValue(totalPurchaseValue);
        currentBill.getBillFinanceDetails().setTotalCostValue(totalCostValue);
        currentBill.getBillFinanceDetails().setTotalRetailSaleValue(totalRetailValue);

        // Also set the legacy total fields for backward compatibility
        currentBill.setTotal(returnTotal.doubleValue());
        currentBill.setNetTotal(returnTotal.doubleValue());

        // Calculate net value adjustment after updating net total
        calculateNetValueAdjustment();
    }

    // Event handlers (with comprehensive validation)
    public void onEdit(BillItem bi) {
        // Validate integer-only quantity if configuration is enabled
        if (configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            // Guard against null billItemFinanceDetails
            if (bi.getBillItemFinanceDetails() == null) {
                validateReturnQuantities(bi);
                calculateLineTotal(bi);
                calculateTotal();
                return;
            }

            BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
            BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();

            // Check quantity for decimal values
            if (qty != null && qty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setQuantity(BigDecimal.ZERO);
                calculateLineTotal(bi);
                calculateTotal();
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
                return;
            }

            // Check free quantity for decimal values
            if (freeQty != null && freeQty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setFreeQuantity(BigDecimal.ZERO);
                calculateLineTotal(bi);
                calculateTotal();
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
                return;
            }
        }

        validateReturnQuantities(bi);
        calculateLineTotal(bi);
        calculateTotal();
    }

    public void onFocus(BillItem bi) {
        // Set current editing item for context
        currentBillItem = bi;
    }

    private void prepareBillItems(Bill originalGrnBill, Bill newGrnReturnBill) {
        if (originalGrnBill == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        if (originalGrnBill.getId() == null) {
            JsfUtil.addErrorMessage("There is a system error. Please contact Developers");
            return;
        }
        String jpql = "Select p from PharmaceuticalBillItem p where p.billItem.bill.id = :billId";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", originalGrnBill.getId());
        List<PharmaceuticalBillItem> pbisOfOgirinalGrnBill = pharmaceuticalBillItemFacade.findByJpql(jpql, params);
        for (PharmaceuticalBillItem pbiOfBilledBill : pbisOfOgirinalGrnBill) {
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
                newBillItemInReturnBill.setBill(newGrnReturnBill);

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
        calculateGrnReturnTotal();
        try {
            BigDecimal netTotal = newGrnReturnBill != null && newGrnReturnBill.getBillFinanceDetails() != null
                    ? Optional.ofNullable(newGrnReturnBill.getBillFinanceDetails().getNetTotal()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            BigDecimal grossTotal = newGrnReturnBill != null && newGrnReturnBill.getBillFinanceDetails() != null
                    ? Optional.ofNullable(newGrnReturnBill.getBillFinanceDetails().getGrossTotal()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
        } catch (Exception ignore) {
            // Keep silent on totals extraction errors, already traced elsewhere
        }
    }

    private void calculateTotalReturnByLineNetTotals(Bill newGrnReturnBill) {
        // Use the dedicated calculation method for consistency
        if (newGrnReturnBill != null && newGrnReturnBill.equals(currentBill)) {
            calculateGrnReturnTotal();
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

            if (newGrnReturnBill == null) {
                return;
            }

            if (newGrnReturnBill.getBillFinanceDetails() == null) {
                newGrnReturnBill.setBillFinanceDetails(new BillFinanceDetails());
                newGrnReturnBill.getBillFinanceDetails().setBill(newGrnReturnBill);
            }

            newGrnReturnBill.getBillFinanceDetails().setNetTotal(returnTotal);
            newGrnReturnBill.getBillFinanceDetails().setGrossTotal(returnTotal);

            // Also set legacy fields for backward compatibility
            newGrnReturnBill.setTotal(returnTotal.doubleValue());
            newGrnReturnBill.setNetTotal(returnTotal.doubleValue());

            // Calculate net value adjustment after updating net total
            if (newGrnReturnBill.equals(currentBill)) {
                calculateNetValueAdjustment();
            }
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
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

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
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

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
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);

        Object result = billItemFacade.findSingleScalar(sql, params);
        BigDecimal returnValue = BigDecimal.ZERO;

        if (result != null) {
            returnValue = safeToBigDecimal(result).abs(); // Use absolute value since returns are negative
        } else {
        }

        return returnValue;
    }

    private void generateItemsFromGrn() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        billItems.clear();

        if (originalGrn == null) {
            JsfUtil.addErrorMessage("No GRN Bill is selected.");
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

        // Use same logic as DirectPurchaseReturnController.getReturnRate()
        BigDecimal rate = originalFd.getGrossRate();
        boolean usingCostRate = false;

        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Line Cost Rate", false)
                && originalFd.getLineCostRate() != null) {
            rate = originalFd.getLineCostRate();
            usingCostRate = true;  // Cost rates are already per unit
        } else if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Total Cost Rate", false)
                && originalFd.getTotalCostRate() != null) {
            rate = originalFd.getTotalCostRate();
            usingCostRate = true;  // Cost rates are already per unit
        }

        // Convert to per unit rate ONLY if using GrossRate (which is per pack for AMPP)
        // Cost rates (LineCostRate, TotalCostRate) are ALREADY per unit, so don't divide again
        if (!usingCostRate && rate != null && rate.compareTo(BigDecimal.ZERO) > 0
                && originalFd.getUnitsPerPack() != null && originalFd.getUnitsPerPack().compareTo(BigDecimal.ZERO) > 0) {
            return rate.divide(originalFd.getUnitsPerPack(), 4, BigDecimal.ROUND_HALF_UP);
        } else if (usingCostRate && rate != null) {
            return rate;  // Already per unit
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

        originalGrn = currentBill.getReferenceBill();
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

        // Set bill creation metadata
        currentBill.setCreatedAt(new Date());
        currentBill.setCreater(sessionController.getLoggedUser());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setDepartment(sessionController.getDepartment());

        //Copy Payment Method Details from GRN to GRN Return
        currentBill.setPaymentMethod(originalGrn.getPaymentMethod());

        // Generate items from GRN (similar to legacy prepareReturnBill)
        // generateItemsFromGrn(); commendted out as this method is NOT working correctl
        prepareBillItems(grn, currentBill);

        // Save the return bill immediately to generate IDs for all items
        // This will make item deletion much more reliable
        try {
            saveBill(false); // Save but don't finalize
            // Ensure bill items are properly associated for any subsequent operations
            ensureBillItemsForPreview();
            JsfUtil.addSuccessMessage("GRN Return created successfully. You can now modify quantities and remove items as needed.");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error creating GRN Return: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // GRN Return specific methods (matching legacy pattern)
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
        params.put("bt", BillType.PharmacyGrnReturn);
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
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("can", false);
        params.put("comp", true);
        params.put("ret", false);

        return billItemFacade.findDoubleByJpql(sql, params);
    }

    // Comprehensive validation methods based on legacy GrnReturnWithCostingController pattern
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
        BigDecimal preservedLineGrossRate = fd.getLineGrossRate(); // keep whatever rate the UI last set
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
            double currentTotalQty = (fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0);

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
            double currentQty = (fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0);
            double currentFreeQty = (fd.getFreeQuantity() != null ? Math.abs(fd.getFreeQuantity().doubleValue()) : 0.0);

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

        if (preservedLineGrossRate != null) {
            fd.setLineGrossRate(preservedLineGrossRate);
        }

        return isValid;
    }

    /**
     * Comprehensive stock validation for GRN return items Handles multiple rows
     * using the same stock scenario
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
        double returnQty = fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0;
        double returnFreeQty = fd.getFreeQuantity() != null ? Math.abs(fd.getFreeQuantity().doubleValue()) : 0.0;

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

            double returnQty = fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0;
            double returnFreeQty = fd.getFreeQuantity() != null ? Math.abs(fd.getFreeQuantity().doubleValue()) : 0.0;

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
            double returnQty = fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0;
            double returnFreeQty = fd.getFreeQuantity() != null ? Math.abs(fd.getFreeQuantity().doubleValue()) : 0.0;

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
                double qty = fd.getQuantity() != null ? Math.abs(fd.getQuantity().doubleValue()) : 0.0;
                double freeQty = fd.getFreeQuantity() != null ? Math.abs(fd.getFreeQuantity().doubleValue()) : 0.0;

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

            // Check stock availability for return processing
            if (bi.getPharmaceuticalBillItem() != null && bi.getPharmaceuticalBillItem().getStock() != null) {
                double currentStock = bi.getPharmaceuticalBillItem().getStock().getStock();
                double returningQty = bi.getPharmaceuticalBillItem().getQty();

                // Stock should be adjusted, but we need to ensure we don't go into impossible negative values
                // This is a business rule check - some institutions may want to prevent returns that would cause negative stock
                boolean allowNegativeStock = configOptionApplicationController.getBooleanValueByKey("Allow Negative Stock in Returns", true);
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
        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Line Cost Rate", false)) {
            return "Line Cost Rate";
        }
        if (configOptionApplicationController.getBooleanValueByKey("Purchase Return Based On Total Cost Rate", false)) {
            return "Total Cost Rate";
        }
        return "Purchase Rate";
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

            // Calculate unit rate from pack rate (HMIS STANDARD: PBI rates ALWAYS per unit)
            BigDecimal ratePerUnit = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                    ? userEnteredRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Sync pharmaceutical bill item with calculated unit-based values
            phi.setQty(quantityByUnits.doubleValue());        // ALWAYS in units
            phi.setFreeQty(freeQuantityByUnits.doubleValue()); // ALWAYS in units
            phi.setPurchaseRateInUnit(ratePerUnit.doubleValue());
            phi.setPurchaseRatePack(userEnteredRate.doubleValue());
            phi.setPurchaseRate(ratePerUnit.doubleValue());    // CRITICAL: ALWAYS per unit, not pack rate!

        } else {
            // For AMP items: User entered rate is per unit
            BigDecimal quantity = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQuantity = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;

            // Set unit quantities (same as pack quantities for AMP)
            fd.setQuantityByUnits(quantity);
            fd.setFreeQuantityByUnits(freeQuantity);

            // Sync pharmaceutical bill item with unit-based values
            phi.setQty(quantity.doubleValue());               // In units (same as packs for AMP)
            phi.setFreeQty(freeQuantity.doubleValue());       // In units (same as packs for AMP)
            phi.setPurchaseRateInUnit(userEnteredRate.doubleValue());
            phi.setPurchaseRate(userEnteredRate.doubleValue()); // Per unit
            phi.setPurchaseRatePack(userEnteredRate.doubleValue()); // Same as unit for AMP
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
     * Calculates the net value adjustment based on actual net value entered by user
     * Adjustment = Net Total (calculated) - Actual Net Value
     * Positive adjustment means calculated is higher than actual
     * Negative adjustment means calculated is lower than actual
     */
    public void calculateNetValueAdjustment() {
        if (currentBill == null || currentBill.getBillFinanceDetails() == null) {
            return;
        }

        BillFinanceDetails bfd = currentBill.getBillFinanceDetails();

        BigDecimal actualNetValue = bfd.getActualNetValue();
        BigDecimal netTotal = bfd.getNetTotal();

        if (actualNetValue != null && netTotal != null) {
            BigDecimal adjustment = netTotal.subtract(actualNetValue);
            bfd.setNetValueAdjustment(adjustment);
        } else {
            bfd.setNetValueAdjustment(null);
        }
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

            // Calculate unit rate from existing pack rate (HMIS STANDARD: PBI rates ALWAYS per unit)
            BigDecimal ratePerUnit = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                    ? existingRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Sync pharmaceutical bill item with calculated values
            phi.setQty(quantityByUnits.doubleValue());        // ALWAYS in units
            phi.setFreeQty(freeQuantityByUnits.doubleValue()); // ALWAYS in units
            phi.setPurchaseRateInUnit(ratePerUnit.doubleValue());
            phi.setPurchaseRatePack(existingRate.doubleValue());
            phi.setPurchaseRate(ratePerUnit.doubleValue());    // CRITICAL: ALWAYS per unit, not pack rate!

        } else {
            // For AMP items: Rate is per unit
            BigDecimal quantity = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
            BigDecimal freeQuantity = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;

            // Update unit quantities (same as pack quantities for AMP)
            fd.setQuantityByUnits(quantity);
            fd.setFreeQuantityByUnits(freeQuantity);

            // Sync pharmaceutical bill item with values
            phi.setQty(quantity.doubleValue());               // In units (same as packs for AMP)
            phi.setFreeQty(freeQuantity.doubleValue());       // In units (same as packs for AMP)
            phi.setPurchaseRateInUnit(existingRate.doubleValue());
            phi.setPurchaseRate(existingRate.doubleValue());  // Per unit
            phi.setPurchaseRatePack(existingRate.doubleValue()); // Same as unit for AMP
        }

        // CRITICAL: Restore the original rate to prevent external service interference
        fd.setLineGrossRate(existingRate);
    }

    /**
     * Dedicated line total calculation for GRN Return items Uses proper
     * quantity and rate calculations without external service interference
     */
    private void calculateLineTotal(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null || bi.getPharmaceuticalBillItem() == null) {
            return;
        }

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();
        Item item = bi.getItem();
        boolean isAmpp = item instanceof Ampp;

        // Null-safe BigDecimal values
        BigDecimal qty = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal rate = fd.getLineGrossRate() != null ? fd.getLineGrossRate() : BigDecimal.ZERO;
        BigDecimal unitsPerPack = fd.getUnitsPerPack() != null ? fd.getUnitsPerPack() : BigDecimal.ONE;

        // For GRN returns, line total = quantity × rate (paid quantity only, not including free)
        BigDecimal lineGross = qty.multiply(rate);
        BigDecimal lineNet = lineGross; // No discounts for GRN returns

        // Set the calculated line totals in BillItemFinanceDetails
        fd.setLineGrossTotal(lineGross);
        fd.setLineNetTotal(lineNet);

        // Calculate and set net rate (net total / quantity)
        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            fd.setLineNetRate(lineNet.divide(qty, 4, RoundingMode.HALF_UP));
        } else {
            fd.setLineNetRate(BigDecimal.ZERO);
        }

        // Calculate unit-based quantities for PBI (ALWAYS in units per HMIS standard)
        BigDecimal qtyByUnits;
        BigDecimal freeQtyByUnits;

        if (isAmpp) {
            // AMPP: Convert packs to units
            qtyByUnits = qty.multiply(unitsPerPack);
            freeQtyByUnits = freeQty.multiply(unitsPerPack);
        } else {
            // AMP: Already in units
            qtyByUnits = qty;
            freeQtyByUnits = freeQty;
        }

        // Update BIFD quantities - make negative for returns (stock moving out)
        fd.setQuantity(qty.abs().negate());
        fd.setQuantityByUnits(qtyByUnits.abs().negate());
        fd.setFreeQuantityByUnits(freeQtyByUnits.abs().negate());
        fd.setTotalQuantityByUnits(qtyByUnits.add(freeQtyByUnits).abs().negate());
        fd.setTotalQuantity(qty.add(freeQty).abs().negate()); // Total quantity in packs (for AMPP) or units (for AMP)

        // Set BillItem fields (as user entered)
        bi.setRate(rate.doubleValue());
        bi.setQty(qty.doubleValue());
        bi.setNetRate(fd.getLineNetRate() != null ? fd.getLineNetRate().doubleValue() : 0.0);
        bi.setGrossValue(lineGross.doubleValue());
        bi.setNetValue(lineNet.doubleValue());

        // CRITICAL: Set PharmaceuticalBillItem rates following HMIS standards
        if (isAmpp) {
            // AMPP: Convert pack rate to unit rate for PBI.purchaseRate
            BigDecimal ratePerUnit = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                    ? rate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            phi.setPurchaseRate(ratePerUnit.doubleValue());  // ALWAYS per unit
            phi.setPurchaseRatePack(rate.doubleValue());     // Pack rate only for AMPP
        } else {
            // AMP: Rate is already per unit
            phi.setPurchaseRate(rate.doubleValue());         // Per unit
            phi.setPurchaseRatePack(rate.doubleValue());     // Same as unit rate for AMP
        }

        // Set PBI quantities (ALWAYS in units per HMIS standard)
        phi.setQty(qtyByUnits.doubleValue());
        phi.setFreeQty(freeQtyByUnits.doubleValue());

        // Calculate and set rates and values from original GRN item
        if (bi.getReferanceBillItem() != null) {
            PharmaceuticalBillItem originalPhi = bi.getReferanceBillItem().getPharmaceuticalBillItem();
            if (originalPhi != null) {
                // Get the original rates (ALWAYS per unit per HMIS standard)
                double purchaseRatePerUnit = originalPhi.getPurchaseRate();
                double costRatePerUnit = originalPhi.getCostRate();
                double retailRatePerUnit = originalPhi.getRetailRate();

                // Set cost rate in PBI (ALWAYS per unit)
                phi.setCostRate(costRatePerUnit);

                // Calculate total quantity in units for value calculations
                double totalReturningQtyInUnits = qtyByUnits.doubleValue() + freeQtyByUnits.doubleValue();

                // Calculate values (quantity in units × rate per unit)
                phi.setPurchaseValue(totalReturningQtyInUnits * purchaseRatePerUnit);
                phi.setCostValue(totalReturningQtyInUnits * costRatePerUnit);
                phi.setRetailValue(totalReturningQtyInUnits * retailRatePerUnit);

                // Set retail rate from original
                phi.setRetailRate(retailRatePerUnit);

                // Set in BillItemFinanceDetails for consistency - make negative for returns (stock moving out)
                fd.setValueAtPurchaseRate(BigDecimal.valueOf(totalReturningQtyInUnits * purchaseRatePerUnit).abs().negate());
                fd.setValueAtCostRate(BigDecimal.valueOf(totalReturningQtyInUnits * costRatePerUnit).abs().negate());
                fd.setValueAtRetailRate(BigDecimal.valueOf(totalReturningQtyInUnits * retailRatePerUnit).abs().negate());

                // Set rates in BillItemFinanceDetails (in units for AMPs, in packs for AMPPs)
                if (isAmpp) {
                    // For AMPPs: Calculate rates per pack
                    BigDecimal purchaseRatePerPack = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                            ? BigDecimal.valueOf(purchaseRatePerUnit).multiply(unitsPerPack)
                            : BigDecimal.ZERO;
                    BigDecimal costRatePerPack = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                            ? BigDecimal.valueOf(costRatePerUnit).multiply(unitsPerPack)
                            : BigDecimal.ZERO;
                    BigDecimal retailRatePerPack = unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                            ? BigDecimal.valueOf(retailRatePerUnit).multiply(unitsPerPack)
                            : BigDecimal.ZERO;

                    fd.setPurchaseRate(purchaseRatePerPack);
                    fd.setCostRate(costRatePerPack);
                    fd.setRetailSaleRate(retailRatePerPack);
                } else {
                    // For AMPs: Rates are already per unit
                    fd.setPurchaseRate(BigDecimal.valueOf(purchaseRatePerUnit));
                    fd.setCostRate(BigDecimal.valueOf(costRatePerUnit));
                    fd.setRetailSaleRate(BigDecimal.valueOf(retailRatePerUnit));
                }

                // Set cost rate fields (lineCostRate, billCostRate, totalCostRate) - always per unit
                BigDecimal costRatePerUnitBD = BigDecimal.valueOf(costRatePerUnit);
                fd.setLineCostRate(costRatePerUnitBD);
                fd.setBillCostRate(costRatePerUnitBD);
                fd.setTotalCostRate(costRatePerUnitBD);

                // Calculate cost values (costRate × quantity in units) - negative for returns
                BigDecimal totalCostValue = costRatePerUnitBD.multiply(qtyByUnits.abs());
                fd.setLineCost(totalCostValue.negate());
                fd.setBillCost(totalCostValue.negate());
                fd.setTotalCost(totalCostValue.negate());
            }
        }

        // Set bill-level rates and totals (should match line-level for single-item returns)
        fd.setGrossRate(fd.getLineGrossRate());
        fd.setNetRate(fd.getLineNetRate());
        fd.setGrossTotal(fd.getLineGrossTotal());
        fd.setNetTotal(fd.getLineNetTotal());

        // Set zero-value fields (no discounts, taxes, or expenses on GRN returns)
        fd.setLineDiscount(BigDecimal.ZERO);
        fd.setLineTax(BigDecimal.ZERO);
        fd.setLineExpense(BigDecimal.ZERO);
        fd.setBillDiscount(BigDecimal.ZERO);
        fd.setBillTax(BigDecimal.ZERO);
        fd.setBillExpense(BigDecimal.ZERO);
        fd.setTotalDiscount(BigDecimal.ZERO);
        fd.setTotalTax(BigDecimal.ZERO);
        fd.setTotalExpense(BigDecimal.ZERO);
        fd.setLineDiscountRate(BigDecimal.ZERO);
        fd.setBillDiscountRate(BigDecimal.ZERO);
        fd.setTotalDiscountRate(BigDecimal.ZERO);
        fd.setLineTaxRate(BigDecimal.ZERO);
        fd.setBillTaxRate(BigDecimal.ZERO);
        fd.setTotalTaxRate(BigDecimal.ZERO);
        fd.setLineExpenseRate(BigDecimal.ZERO);
        fd.setBillExpenseRate(BigDecimal.ZERO);
        fd.setTotalExpenseRate(BigDecimal.ZERO);
    }

    // Utility methods
    private boolean hasUnapprovedGrnReturns() {
        if (selectedGrn == null) {
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
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);
        params.put("refBill", selectedGrn);

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
        if (originalGrn == null) {
            return 0.0;
        }

        PharmaceuticalBillItem originalPhi = findItemInGrn(item, originalGrn);
        return originalPhi != null ? originalPhi.getPurchaseRateInUnit() : 0.0;
    }

    /**
     * Get the original pack rate for AMPP items from the original GRN
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
    public void fillGrnReturnsToFinalize() {
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
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);
        params.put("dept", sessionController.getDepartment());

        grnReturnsToFinalize = billFacade.findByJpql(jpql, params);
        if (grnReturnsToFinalize == null) {
            grnReturnsToFinalize = new ArrayList<>();
        }
    }

    public void fillGrnReturnsToApprove() {
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
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);
        params.put("dept", sessionController.getDepartment());

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
        paymentMethodData = new PaymentMethodData();
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

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
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

    /**
     * Checks if a GRN is fully returned by comparing original quantities with
     * returned quantities for all items in the GRN. Only considers completed
     * (approved) returns.
     *
     * @param grnBill The original GRN bill to check
     * @return true if all items are fully returned, false otherwise
     */
    private boolean isGrnFullyReturned(Bill grnBill) {
        if (grnBill == null) {
            return false;
        }

        // Get all bill items from the original GRN
        String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId AND bi.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", grnBill.getId());
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
     * Authorization helper method to check GRN Return privileges and audit
     * denied access
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

            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized GRN Return access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});

            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " GRN return requests.");
            return false;
        }

        return true;
    }

    public Integer getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(Integer activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Integer getActiveIndexForReturnsAndCancellations() {
        return activeIndexForReturnsAndCancellations;
    }

    public void setActiveIndexForReturnsAndCancellations(Integer activeIndexForReturnsAndCancellations) {
        this.activeIndexForReturnsAndCancellations = activeIndexForReturnsAndCancellations;
    }

}
