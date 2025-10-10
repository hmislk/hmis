/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.Item;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;

/**
 * Controller for handling pharmacy transfer receive cancellation operations.
 *
 * <h2>Purpose</h2>
 * This controller manages the cancellation of pharmacy transfer receive bills
 * that were created via the TransferReceiveController. When a transfer receive
 * needs to be cancelled, this controller creates a reversing bill that:
 * <ul>
 *   <li>Reverses stock movements (deducts from receiving department, adds back to issuer)</li>
 *   <li>Reverses cost accounting (negative cost values)</li>
 *   <li>Maintains audit trail via Bill.cancelledBill relationship</li>
 *   <li>Updates the original Transfer Issue bill's fullyIssued status</li>
 *   <li>Creates complete BillFinanceDetails and BillItemFinanceDetails for analytics</li>
 * </ul>
 *
 * <h2>Critical Sign Convention for Cancellation</h2>
 *
 * <h3>Original Transfer Receive:</h3>
 * <pre>
 * - Quantity: POSITIVE (stock comes in)
 * - Cost values (totalCostValue, etc.): POSITIVE (cost burden added)
 * - Inventory values (valueAtCostRate, etc.): POSITIVE (follow quantity)
 * - Revenue/Gross Total/Net Total: NEGATIVE (money goes out to pay issuing dept)
 * </pre>
 *
 * <h3>Cancellation Bill (Reversal):</h3>
 * <pre>
 * - Quantity: NEGATIVE (stock goes out)
 * - Cost values (totalCostValue, etc.): NEGATIVE (cost burden removed)
 * - Inventory values (valueAtCostRate, etc.): NEGATIVE (follow quantity)
 * - Revenue/Gross Total/Net Total: POSITIVE (money comes back in)
 * </pre>
 *
 * <h3>Stock Reversal Logic:</h3>
 * <pre>
 * Original Receive: Staff Stock += qty, Dept Stock += qty
 * Cancellation:     Dept Stock -= qty, Staff Stock += qty (reversal)
 * </pre>
 *
 * <h3>Issue #15797 Fix:</h3>
 * <p>This controller creates complete BillFinanceDetails and BillItemFinanceDetails
 * with proper TYPE(b) support in DTOs, ensuring cancelled receive amounts are correctly
 * deducted from transfer receive summary reports.</p>
 *
 * <h3>Related Documentation</h3>
 * <ul>
 *   <li>See: developer_docs/pharmacy/cost-accounting-sign-conventions.md</li>
 *   <li>See: {@link TransferIssueCancellationController} for issue cancellation pattern</li>
 * </ul>
 *
 * @author HMIS Development Team
 * @since 2025-10-10 Issue #15797 - Transfer receive cancellation completion
 */
@Named
@SessionScoped
public class TransferReceiveCancellationController implements Serializable {

    private static final long serialVersionUID = 1L;

    // EJB Dependencies
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;

    // CDI Dependencies
    @Inject
    private SessionController sessionController;
    @Inject
    private BillController billController;
    @Inject
    private PharmacyController pharmacyController;

    // Properties
    private Bill originalReceiveBill;
    private Long originalReceiveBillId;
    private Bill cancellationBill;
    private List<BillItem> cancellationBillItems;
    private String cancellationReason;
    private boolean printPreview;

    /**
     * Creates a new instance of TransferReceiveCancellationController
     */
    public TransferReceiveCancellationController() {
    }

    /**
     * Navigates to the cancellation preview page by loading the original receive bill
     * using its ID. Performs all necessary validations before allowing cancellation.
     *
     * @return navigation outcome - empty string on error, page path on success
     */
    public String navigateToCancellationPreviewById() {
        // Validation 1: Check if bill ID is provided
        if (originalReceiveBillId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }

        // Load the original receive bill
        originalReceiveBill = billFacade.find(originalReceiveBillId);

        // Validation 2: Check if bill exists
        if (originalReceiveBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }

        // Validation 3: Check if already cancelled
        if (!validateNotAlreadyCancelled()) {
            return "";
        }

        // Validation 4: Check if items have sufficient stock for reversal
        if (!validateSufficientStockForReversal()) {
            return "";
        }

        // Create cancellation bill preview
        createCancellationBillPreview();

        // Navigate to cancellation page
        return "/pharmacy/pharmacy_transfer_receive_cancel?faces-redirect=true";
    }

    /**
     * Validates that the bill has not already been cancelled.
     *
     * @return true if validation passes, false otherwise
     */
    private boolean validateNotAlreadyCancelled() {
        if (originalReceiveBill.isCancelled()) {
            JsfUtil.addErrorMessage("This bill has already been cancelled");
            return false;
        }

        // Check if there's a cancellation bill already created
        if (originalReceiveBill.getCancelledBill() != null) {
            JsfUtil.addErrorMessage("A cancellation bill already exists for this transfer receive");
            return false;
        }

        return true;
    }

    /**
     * Validates that there is sufficient stock to reverse the receive operation.
     * Prevents negative stock situations.
     *
     * @return true if validation passes, false otherwise
     */
    private boolean validateSufficientStockForReversal() {
        if (originalReceiveBill.getBillItems() == null || originalReceiveBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No items found in the original bill");
            return false;
        }

        // Note: Stock validation is performed during actual deduction in reverseStockMovements()
        // If stock is insufficient, the transaction will rollback with clear error message
        // This is the safest approach to prevent race conditions

        return true;
    }

    /**
     * Creates a preview of the cancellation bill without saving to database.
     * This allows the user to review before confirming.
     */
    private void createCancellationBillPreview() {
        // Create new cancellation bill structure
        createCancellationBillStructure();

        // Create cancellation bill items
        createCancellationBillItems();

        // Calculate totals
        calculateCancellationTotals();
    }

    /**
     * Creates the cancellation bill structure with metadata.
     */
    private void createCancellationBillStructure() {
        cancellationBill = new CancelledBill();
        cancellationBill.setBillType(BillType.PharmacyTransferReceive);
        cancellationBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RECEIVE_CANCELLED);
        cancellationBill.setBillClassType(BillClassType.CancelledBill);

        // Set departments (same as original receive)
        cancellationBill.setDepartment(originalReceiveBill.getDepartment());
        cancellationBill.setInstitution(originalReceiveBill.getInstitution());
        cancellationBill.setFromDepartment(originalReceiveBill.getFromDepartment());
        cancellationBill.setFromInstitution(originalReceiveBill.getFromInstitution());
        cancellationBill.setToDepartment(originalReceiveBill.getToDepartment());
        cancellationBill.setToInstitution(originalReceiveBill.getToInstitution());

        // Set audit fields
        cancellationBill.setCreater(sessionController.getLoggedUser());
        cancellationBill.setCreatedAt(Calendar.getInstance().getTime());

        // Link to original receive bill
        cancellationBill.setBackwardReferenceBill(originalReceiveBill);

        // Link to the original issue bill (for traceability)
        if (originalReceiveBill.getBackwardReferenceBill() != null) {
            cancellationBill.setBilledBill(originalReceiveBill.getBackwardReferenceBill());
        }

        // Set cancellation flag
        cancellationBill.setCancelled(false); // The cancellation bill itself is not cancelled

        // Initialize collections
        cancellationBill.setBillItems(new ArrayList<>());
    }

    /**
     * Creates cancellation bill items by reversing the original receive bill items.
     *
     * <h3>CRITICAL: Sign Reversal for Cancellation</h3>
     * <pre>
     * Original Receive:              Cancellation:
     * - Quantity: +10 packs          - Quantity: -10 packs
     * - Cost: +27.273                - Cost: -27.273
     * - Revenue: 0                   - Revenue: 0
     * </pre>
     */
    private void createCancellationBillItems() {
        cancellationBillItems = new ArrayList<>();

        if (originalReceiveBill.getBillItems() == null || originalReceiveBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No items found in the original bill");
            return;
        }

        for (BillItem originalItem : originalReceiveBill.getBillItems()) {
            if (originalItem.isRetired() || originalItem.getPharmaceuticalBillItem() == null) {
                continue;
            }

            // Create new bill item for cancellation
            BillItem cancellationItem = new BillItem();
            cancellationItem.setBill(cancellationBill);
            cancellationItem.setItem(originalItem.getItem());
            cancellationItem.setRetired(false);
            cancellationItem.setCreatedAt(new Date());
            cancellationItem.setCreater(sessionController.getLoggedUser());

            // Reverse quantity - original was positive, cancellation is negative
            double originalQty = originalItem.getQty();
            cancellationItem.setQty(-1 * Math.abs(originalQty)); // Make negative for cancellation

            // Create reversed pharmaceutical bill item
            createReversedPharmaceuticalBillItem(cancellationItem, originalItem);

            // Create reversed finance details (CRITICAL for Issue #15797)
            createReversedFinanceDetails(cancellationItem, originalItem);

            // Set revenue values - POSITIVE for cancellation (money comes back in)
            double rate = originalItem.getRate();
            cancellationItem.setRate(rate);
            cancellationItem.setNetRate(rate);
            // POSITIVE revenue for cancellation (money comes back in)
            cancellationItem.setNetValue(rate * Math.abs(originalQty)); // POSITIVE
            cancellationItem.setGrossValue(rate * Math.abs(originalQty)); // POSITIVE

            cancellationBillItems.add(cancellationItem);
        }

        cancellationBill.setBillItems(cancellationBillItems);
    }

    /**
     * Creates reversed pharmaceutical bill item for cancellation.
     * Quantities are reversed (negative for cancellation).
     */
    private void createReversedPharmaceuticalBillItem(BillItem cancellationItem, BillItem originalItem) {
        PharmaceuticalBillItem originalPhItem = originalItem.getPharmaceuticalBillItem();
        PharmaceuticalBillItem cancellationPhItem = new PharmaceuticalBillItem();

        cancellationPhItem.setBillItem(cancellationItem);
        cancellationPhItem.setItemBatch(originalPhItem.getItemBatch());

        // Reverse quantity - original was positive, cancellation is negative
        double originalQtyInUnits = originalPhItem.getQty();
        cancellationPhItem.setQty(-1 * Math.abs(originalQtyInUnits)); // Negative for stock reversal

        // Copy rate values
        ItemBatch batch = originalPhItem.getItemBatch();
        if (batch != null) {
            double unitsPerPack = (originalItem.getItem() != null) ? originalItem.getItem().getDblValue() : 1.0;
            cancellationPhItem.setPurchaseRatePack(batch.getPurcahseRate() * unitsPerPack);
            cancellationPhItem.setRetailRatePack(batch.getRetailsaleRate() * unitsPerPack);
            cancellationPhItem.setCostRatePack(batch.getCostRate() * unitsPerPack);

            // Negative values for cancellation (cost burden removed)
            cancellationPhItem.setPurchaseValue(-1 * batch.getPurcahseRate() * Math.abs(originalQtyInUnits));
            cancellationPhItem.setRetailValue(-1 * batch.getRetailsaleRate() * Math.abs(originalQtyInUnits));
            cancellationPhItem.setCostValue(-1 * batch.getCostRate() * Math.abs(originalQtyInUnits));
        }

        // Set stock references (for tracking which stock is affected by this cancellation)
        // These point to the same stock entities as the original receive for audit trail
        cancellationPhItem.setStock(originalPhItem.getStock());
        cancellationPhItem.setStaffStock(originalPhItem.getStaffStock());

        cancellationItem.setPharmaceuticalBillItem(cancellationPhItem);
    }

    /**
     * Creates reversed finance details for cancellation bill item.
     *
     * <h3>CRITICAL FIX for Issue #15797: Reversed Sign Convention</h3>
     * <p>Transfer Receive Cancellation reverses the original receive:</p>
     * <ul>
     *   <li>Quantity: NEGATIVE (stock goes out from receiving dept)</li>
     *   <li>Cost: NEGATIVE (cost burden removed)</li>
     *   <li>Revenue/Gross Total: POSITIVE (money comes back in)</li>
     *   <li>Inventory valuations: NEGATIVE (follow quantity sign)</li>
     * </ul>
     *
     * <h3>Example:</h3>
     * <pre>
     * Original Receive BIFD:         Cancellation BIFD:
     * - quantity: +10 packs          - quantity: -10 packs
     * - lineCost: +27.273            - lineCost: -27.273
     * - totalCost: +27.273           - totalCost: -27.273
     * - valueAtCostRate: +27.273     - valueAtCostRate: -27.273
     * - lineGrossTotal: -50.0        - lineGrossTotal: +50.0 (money comes in)
     * - lineNetTotal: -50.0          - lineNetTotal: +50.0 (money comes in)
     * </pre>
     *
     * @param cancellationItem The cancellation bill item
     * @param originalItem The original receive bill item
     */
    private void createReversedFinanceDetails(BillItem cancellationItem, BillItem originalItem) {
        BillItemFinanceDetails originalBifd = originalItem.getBillItemFinanceDetails();
        BillItemFinanceDetails cancellationBifd = new BillItemFinanceDetails();

        // Get values from original
        ItemBatch batch = originalItem.getPharmaceuticalBillItem().getItemBatch();
        double unitsPerPack = (originalBifd != null && originalBifd.getUnitsPerPack() != null)
            ? originalBifd.getUnitsPerPack().doubleValue()
            : 1.0;

        // Set units per pack
        cancellationBifd.setUnitsPerPack(BigDecimal.valueOf(unitsPerPack));

        // Quantities - NEGATIVE for cancellation (stock goes out)
        double qtyInPacks = Math.abs(cancellationItem.getQty());
        double qtyInUnits = Math.abs(cancellationItem.getPharmaceuticalBillItem().getQty());

        cancellationBifd.setQuantity(BigDecimal.valueOf(-1 * qtyInPacks)); // NEGATIVE
        cancellationBifd.setTotalQuantity(BigDecimal.valueOf(-1 * qtyInPacks)); // NEGATIVE
        cancellationBifd.setQuantityByUnits(BigDecimal.valueOf(-1 * qtyInUnits)); // NEGATIVE

        // Rates - always positive (intrinsic properties)
        BigDecimal rate = BigDecimal.valueOf(originalItem.getRate());
        cancellationBifd.setLineGrossRate(rate);
        cancellationBifd.setLineNetRate(rate);
        cancellationBifd.setGrossRate(rate);

        // Gross totals - POSITIVE for cancellation (money comes back in)
        cancellationBifd.setLineGrossTotal(rate.multiply(BigDecimal.valueOf(qtyInPacks))); // POSITIVE
        cancellationBifd.setLineNetTotal(rate.multiply(BigDecimal.valueOf(qtyInPacks))); // POSITIVE
        cancellationBifd.setGrossTotal(rate.multiply(BigDecimal.valueOf(qtyInPacks))); // POSITIVE

        if (batch != null) {
            // Cost rates - always positive (intrinsic properties)
            BigDecimal costRate = BigDecimal.valueOf(batch.getCostRate());
            BigDecimal purchaseRate = BigDecimal.valueOf(batch.getPurcahseRate());
            BigDecimal retailRate = BigDecimal.valueOf(batch.getRetailsaleRate());

            cancellationBifd.setCostRate(costRate);
            cancellationBifd.setLineCostRate(costRate);
            cancellationBifd.setBillCostRate(BigDecimal.ZERO);
            cancellationBifd.setTotalCostRate(costRate);
            cancellationBifd.setPurchaseRate(purchaseRate);
            cancellationBifd.setRetailSaleRate(retailRate);

            // Cost values - NEGATIVE for cancellation (cost burden removed)
            BigDecimal qtyInUnitsBD = BigDecimal.valueOf(qtyInUnits);
            cancellationBifd.setLineCost(costRate.multiply(qtyInUnitsBD).negate()); // NEGATIVE
            cancellationBifd.setBillCost(BigDecimal.ZERO);
            cancellationBifd.setTotalCost(costRate.multiply(qtyInUnitsBD).negate()); // NEGATIVE

            // Inventory valuations - NEGATIVE for cancellation (stock goes out, values decrease)
            cancellationBifd.setValueAtCostRate(costRate.multiply(qtyInUnitsBD).negate()); // NEGATIVE
            cancellationBifd.setValueAtPurchaseRate(purchaseRate.multiply(qtyInUnitsBD).negate()); // NEGATIVE
            cancellationBifd.setValueAtRetailRate(retailRate.multiply(qtyInUnitsBD).negate()); // NEGATIVE
        }

        cancellationItem.setBillItemFinanceDetails(cancellationBifd);
    }

    /**
     * Calculates totals for the cancellation bill.
     * Revenue values will be POSITIVE (money comes back in).
     */
    private void calculateCancellationTotals() {
        double netTotal = 0.0;

        if (cancellationBillItems != null) {
            for (BillItem item : cancellationBillItems) {
                netTotal += item.getNetValue(); // This will be POSITIVE for cancellation (money in)
            }
        }

        cancellationBill.setNetTotal(netTotal); // POSITIVE (money comes back in)
        cancellationBill.setTotal(netTotal);

        // Create and aggregate Bill-level finance details (CRITICAL for Issue #15797)
        createBillFinanceDetails();
    }

    /**
     * Creates Bill-level BillFinanceDetails by aggregating from BillItemFinanceDetails.
     * This is CRITICAL for Issue #15797 - ensures cancelled receive amounts appear correctly
     * in transfer receive summary reports with proper TYPE(b) discrimination.
     *
     * <h3>Sign Convention for Transfer Receive Cancellation:</h3>
     * <ul>
     *   <li>totalCostValue: NEGATIVE (cost burden removed)</li>
     *   <li>totalPurchaseValue: NEGATIVE (purchase value removed)</li>
     *   <li>totalRetailSaleValue: NEGATIVE (retail value removed)</li>
     *   <li>lineNetTotal: POSITIVE (money comes back in)</li>
     * </ul>
     */
    private void createBillFinanceDetails() {
        BillFinanceDetails bfd = new BillFinanceDetails();

        // Initialize aggregated values
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        // Aggregate from all bill item finance details
        if (cancellationBillItems != null) {
            for (BillItem item : cancellationBillItems) {
                BillItemFinanceDetails itemFd = item.getBillItemFinanceDetails();
                if (itemFd != null) {
                    // Aggregate cost values (NEGATIVE for cancellation - cost removed)
                    if (itemFd.getTotalCost() != null) {
                        totalCostValue = totalCostValue.add(itemFd.getTotalCost());
                    }

                    // Aggregate purchase values (NEGATIVE for cancellation - inventory value removed)
                    if (itemFd.getValueAtPurchaseRate() != null) {
                        totalPurchaseValue = totalPurchaseValue.add(itemFd.getValueAtPurchaseRate());
                    }

                    // Aggregate retail sale values (NEGATIVE for cancellation - inventory value removed)
                    if (itemFd.getValueAtRetailRate() != null) {
                        totalRetailSaleValue = totalRetailSaleValue.add(itemFd.getValueAtRetailRate());
                    }

                    // Aggregate line net totals (POSITIVE for cancellation - money comes back in)
                    if (itemFd.getLineNetTotal() != null) {
                        lineNetTotal = lineNetTotal.add(itemFd.getLineNetTotal());
                    }
                }
            }
        }

        // Set aggregated values
        bfd.setTotalCostValue(totalCostValue); // NEGATIVE
        bfd.setTotalPurchaseValue(totalPurchaseValue); // NEGATIVE
        bfd.setTotalRetailSaleValue(totalRetailSaleValue); // NEGATIVE
        bfd.setLineNetTotal(lineNetTotal); // POSITIVE

        // Link to cancellation bill (bi-directional)
        bfd.setBill(cancellationBill);
        cancellationBill.setBillFinanceDetails(bfd);
    }

    /**
     * Confirms and saves the cancellation bill to database.
     * This method is transactional and performs all database operations atomically.
     *
     * <h3>Operations Performed:</h3>
     * <ol>
     *   <li>Validates cancellation reason is provided</li>
     *   <li>Validates sufficient stock for reversal</li>
     *   <li>Saves cancellation bill structure with BillFinanceDetails</li>
     *   <li>Saves cancellation bill items with BillItemFinanceDetails</li>
     *   <li>Generates bill numbers</li>
     *   <li>Reverses stock movements (deducts from dept, adds to staff/issuer)</li>
     *   <li>Updates original receive bill as cancelled</li>
     *   <li>Updates original issue bill's fullyIssued status (CRITICAL)</li>
     *   <li>Creates bill-to-bill references</li>
     * </ol>
     */
    @Transactional
    public void confirmCancellation() {
        try {
            // Validation: Check cancellation reason
            if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Cancellation reason is required");
                return;
            }

            // Re-validate the original receive bill hasn't been cancelled in the meantime
            Bill freshOriginalReceiveBill = billFacade.find(originalReceiveBill.getId());
            if (freshOriginalReceiveBill.isCancelled()) {
                JsfUtil.addErrorMessage("This bill has been cancelled by another user");
                makeNull();
                return;
            }

            // Re-validate sufficient stock
            if (!validateSufficientStockForReversal()) {
                return;
            }

            // Save cancellation bill
            cancellationBill.setComments(cancellationReason);
            billFacade.create(cancellationBill);

            // Save all bill items (cascade will handle PharmaceuticalBillItem and BillItemFinanceDetails)
            for (BillItem item : cancellationBill.getBillItems()) {
                // Save bill item - cascade will persist PharmaceuticalBillItem
                billItemFacade.create(item);

                // Update finance details with bill item reference (for bi-directional link)
                if (item.getBillItemFinanceDetails() != null) {
                    item.getBillItemFinanceDetails().setBillItem(item);
                }
            }

            // Generate bill numbers
            String deptId = billNumberBean.institutionBillNumberGenerator(
                sessionController.getDepartment(),
                BillType.PharmacyTransferReceive,
                BillClassType.CancelledBill,
                BillNumberSuffix.PHTRCAN
            );
            String insId = billNumberBean.institutionBillNumberGenerator(
                sessionController.getInstitution(),
                BillType.PharmacyTransferReceive,
                BillClassType.CancelledBill,
                BillNumberSuffix.PHTRCAN
            );

            cancellationBill.setDeptId(deptId);
            cancellationBill.setInsId(insId);
            billFacade.edit(cancellationBill);

            // Reverse stock movements (deduct from receiving dept, add back to issuer)
            reverseStockMovements();

            // Update original receive bill as cancelled
            freshOriginalReceiveBill.setCancelled(true);
            freshOriginalReceiveBill.setCancelledBill(cancellationBill);

            // Initialize forwardReferenceBills if null to avoid NPE
            if (freshOriginalReceiveBill.getForwardReferenceBills() == null) {
                freshOriginalReceiveBill.setForwardReferenceBills(new ArrayList<>());
            }
            freshOriginalReceiveBill.getForwardReferenceBills().add(cancellationBill);

            billFacade.edit(freshOriginalReceiveBill);

            // CRITICAL: Update original Transfer Issue bill's fullyIssued status
            updateOriginalTransferIssueBill(freshOriginalReceiveBill);

            // Reload the cancellation bill to get all persisted data
            Bill savedCancellationBill = billFacade.find(cancellationBill.getId());
            cancellationBill = savedCancellationBill;

            // Show success message
            JsfUtil.addSuccessMessage("Transfer Receive Cancelled Successfully. Cancellation Bill: " + cancellationBill.getInsId());

            // Set print preview
            printPreview = true;

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error cancelling transfer receive: " + e.getMessage());
            e.printStackTrace();
            // Rethrow to ensure transaction rollback
            throw new RuntimeException("Cancellation failed - all changes rolled back", e);
        }
    }

    /**
     * Reverses stock movements by undoing what the receive did.
     *
     * <p>For each cancelled item, this method:
     * <ul>
     *   <li>Deducts quantity from receiving department stock</li>
     *   <li>Adds quantity back to issuing staff stock (reverses the original issue)</li>
     * </ul>
     * The stock is deducted/added with history tracking for audit purposes.</p>
     *
     * <h3>Stock Flow:</h3>
     * <pre>
     * Original Issue:   FROM dept → TO staff (staff stock decreased)
     * Original Receive: TO staff → TO dept (staff stock increased, dept stock increased)
     * Receive Cancel:   TO dept → TO staff (dept stock decreased, staff stock increased back)
     * </pre>
     *
     * <p>After receive cancellation, the original Transfer Issue can either be:
     * <ul>
     *   <li>Cancelled by issuing department (if they want to reverse the entire transfer)</li>
     *   <li>Re-received as another receive by receiving department (if it was received incorrectly)</li>
     * </ul>
     * </p>
     */
    private void reverseStockMovements() {
        for (BillItem item : cancellationBill.getBillItems()) {
            PharmaceuticalBillItem phItem = item.getPharmaceuticalBillItem();

            if (phItem != null && phItem.getItemBatch() != null) {
                double qtyToReverse = Math.abs(phItem.getQty());
                ItemBatch batch = phItem.getItemBatch();

                // 1. Deduct from receiving department stock
                boolean deductSuccess = pharmacyBean.deductFromStock(
                    batch,
                    qtyToReverse,
                    originalReceiveBill.getToDepartment()
                );

                if (!deductSuccess) {
                    // Critical failure - throw exception to rollback entire transaction
                    throw new RuntimeException("Failed to deduct stock for item: " +
                        item.getItem().getName() + ". Insufficient stock in receiving department. Transaction rolled back.");
                }

                // 2. Add back to issuing staff stock (the staff who originally issued the transfer)
                // This reverses the receive and puts the stock back where it was after the issue
                if (phItem.getStaffStock() != null) {
                    pharmacyBean.addToStock(
                        phItem,
                        qtyToReverse,
                        phItem.getStaffStock().getStaff()
                    );
                }
            }
        }
    }

    /**
     * Updates the original Transfer Issue bill's fullyIssued status after receive cancellation.
     *
     * <p>CRITICAL FIX: When a receive is cancelled, the original Transfer Issue bill
     * should have its fullyIssued status recalculated. If any issued quantity is now
     * not fully received (due to this cancellation), the issue bill should be marked
     * as NOT fully issued, allowing new receives to be created.</p>
     *
     * @param cancelledReceiveBill The receive bill that was just cancelled
     */
    private void updateOriginalTransferIssueBill(Bill cancelledReceiveBill) {
        // Get the original Transfer Issue bill
        Bill originalIssueBill = cancelledReceiveBill.getBackwardReferenceBill();

        if (originalIssueBill == null) {
            // No issue bill linked, nothing to update
            return;
        }

        // Recalculate if issue is still fully received
        boolean stillFullyReceived = calculateIfFullyReceived(originalIssueBill);

        if (!stillFullyReceived && originalIssueBill.isFullyIssued()) {
            // Reset fullyIssued flags
            originalIssueBill.setFullyIssued(false);
            originalIssueBill.setFullyIssuedAt(null);
            originalIssueBill.setFullyIssuedBy(null);
            billFacade.edit(originalIssueBill);
        }
    }

    /**
     * Calculates whether a Transfer Issue bill is still fully received.
     * Accounts for cancelled receives by excluding them from the calculation.
     *
     * @param issueBill The Transfer Issue bill to check
     * @return true if all issued quantities are fully received (excluding cancelled receives)
     */
    private boolean calculateIfFullyReceived(Bill issueBill) {
        if (issueBill == null || issueBill.getBillItems() == null || issueBill.getBillItems().isEmpty()) {
            return false;
        }

        // For each issue item, check if fully received
        for (BillItem issueItem : issueBill.getBillItems()) {
            if (issueItem.isRetired() || issueItem.getPharmaceuticalBillItem() == null) {
                continue;
            }

            double issuedQty = Math.abs(issueItem.getQty());

            // Calculate total received quantity (excluding cancelled receives)
            double receivedQty = calculateReceivedQuantityForItem(issueBill, issueItem.getItem());

            // If any item is not fully received, return false
            if (receivedQty < issuedQty) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the total received quantity for a specific item from an issue bill.
     * Excludes cancelled receive bills from the calculation.
     *
     * @param issueBill The Transfer Issue bill
     * @param item The item to calculate received quantity for
     * @return Total received quantity (excluding cancelled receives)
     */
    private double calculateReceivedQuantityForItem(Bill issueBill, Item item) {
        double totalReceived = 0.0;

        if (issueBill.getForwardReferenceBills() == null || issueBill.getForwardReferenceBills().isEmpty()) {
            return totalReceived;
        }

        // Sum up quantities from all non-cancelled receive bills
        for (Bill refBill : issueBill.getForwardReferenceBills()) {
            // Only consider non-cancelled receive bills
            if (refBill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RECEIVE
                && !refBill.isRetired() && !refBill.isCancelled()) {

                // Find the matching item in the receive bill
                if (refBill.getBillItems() != null) {
                    for (BillItem receiveItem : refBill.getBillItems()) {
                        if (receiveItem.getItem() != null && receiveItem.getItem().equals(item)
                            && !receiveItem.isRetired()) {
                            totalReceived += Math.abs(receiveItem.getQty());
                        }
                    }
                }
            }
        }

        return totalReceived;
    }

    /**
     * Cleans up all session-scoped properties.
     * Called after successful cancellation or when navigating away.
     */
    public void makeNull() {
        originalReceiveBill = null;
        originalReceiveBillId = null;
        cancellationBill = null;
        cancellationBillItems = null;
        cancellationReason = null;
        printPreview = false;
    }

    public void displayItemDetails(BillItem billItem) {
        if (billItem == null || billItem.getItem() == null) {
            JsfUtil.addErrorMessage("No item selected");
            return;
        }
        Item item = billItem.getItem();
        pharmacyController.setPharmacyItem(item);
        pharmacyController.fillDetails();
    }

    // Getters and Setters

    public Bill getOriginalReceiveBill() {
        return originalReceiveBill;
    }

    public void setOriginalReceiveBill(Bill originalReceiveBill) {
        this.originalReceiveBill = originalReceiveBill;
    }

    public Long getOriginalReceiveBillId() {
        return originalReceiveBillId;
    }

    public void setOriginalReceiveBillId(Long originalReceiveBillId) {
        this.originalReceiveBillId = originalReceiveBillId;
    }

    public Bill getCancellationBill() {
        return cancellationBill;
    }

    public void setCancellationBill(Bill cancellationBill) {
        this.cancellationBill = cancellationBill;
    }

    public List<BillItem> getCancellationBillItems() {
        return cancellationBillItems;
    }

    public void setCancellationBillItems(List<BillItem> cancellationBillItems) {
        this.cancellationBillItems = cancellationBillItems;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public BillController getBillController() {
        return billController;
    }
}
