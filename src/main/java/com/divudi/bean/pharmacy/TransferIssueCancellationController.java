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
 * Controller for handling pharmacy transfer issue cancellation operations.
 *
 * <h2>Purpose</h2>
 * This controller manages the cancellation of pharmacy transfer issue bills
 * that were created via the TransferIssueController. When a transfer issue
 * needs to be cancelled, this controller creates a reversing bill that:
 * <ul>
 *   <li>Reverses stock movements (adds back to issuing department)</li>
 *   <li>Reverses cost accounting (opposite signs)</li>
 *   <li>Reverses revenue (negative revenue for cancellation)</li>
 *   <li>Deducts from user stocks if applicable</li>
 *   <li>Maintains audit trail via Bill.cancelledBill relationship</li>
 * </ul>
 *
 * <h2>Critical Sign Convention for Cancellation</h2>
 *
 * <h3>Original Transfer Issue:</h3>
 * <pre>
 * - Quantity: NEGATIVE (stock out)
 * - Cost: NEGATIVE (cost relieved)
 * - Revenue: POSITIVE (money in)
 * </pre>
 *
 * <h3>Cancellation Bill (Reversal):</h3>
 * <pre>
 * - Quantity: POSITIVE (stock returns)
 * - Cost: POSITIVE (cost burden restored)
 * - Revenue: NEGATIVE (money refunded)
 * </pre>
 *
 * <h3>Related Documentation</h3>
 * <ul>
 *   <li>See: developer_docs/pharmacy/cost-accounting-sign-conventions.md</li>
 *   <li>See: {@link TransferIssueController#updateBillItemRateAndValueForDirectIssue(BillItem)}</li>
 * </ul>
 *
 * @author HMIS Development Team
 * @since 2025-10-09 Issue #15696 - Transfer issue cancellation implementation
 */
@Named
@SessionScoped
public class TransferIssueCancellationController implements Serializable {

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
    private UserStockController userStockController;

    // Properties
    private Bill originalBill;
    private Long originalBillId;
    private Bill cancellationBill;
    private List<BillItem> cancellationBillItems;
    private String cancellationReason;
    private boolean printPreview;

    /**
     * Creates a new instance of TransferIssueCancellationController
     */
    public TransferIssueCancellationController() {
    }

    /**
     * Navigates to the cancellation preview page by loading the original bill
     * using its ID. Performs all necessary validations before allowing cancellation.
     *
     * @return navigation outcome - empty string on error, page path on success
     */
    public String navigateToCancellationPreviewById() {
        // Validation 1: Check if bill ID is provided
        if (originalBillId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }

        // Load the original bill
        originalBill = billFacade.find(originalBillId);

        // Validation 2: Check if bill exists
        if (originalBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }

        // Validation 3: Check if already cancelled
        if (!validateNotAlreadyCancelled()) {
            return "";
        }

        // Validation 4: Check if not already returned
        if (!validateNotAlreadyReturned()) {
            return "";
        }

        // Validation 5: Check if not accepted at receiving end
        if (!validateNotAcceptedAtReceivingEnd()) {
            return "";
        }

        // Create cancellation bill preview
        createCancellationBillPreview();

        // Navigate to cancellation page
        return "/pharmacy/pharmacy_transfer_issue_cancel?faces-redirect=true";
    }

    /**
     * Validates that the bill has not already been cancelled.
     *
     * @return true if validation passes, false otherwise
     */
    private boolean validateNotAlreadyCancelled() {
        if (originalBill.isCancelled()) {
            JsfUtil.addErrorMessage("This bill has already been cancelled");
            return false;
        }

        // Check if there's a cancellation bill already created
        if (originalBill.getCancelledBill() != null) {
            JsfUtil.addErrorMessage("A cancellation bill already exists for this transfer issue");
            return false;
        }

        return true;
    }

    /**
     * Validates that the bill has not already been returned.
     * Checks for return bills in the forwardReferenceBills collection.
     *
     * @return true if validation passes, false otherwise
     */
    private boolean validateNotAlreadyReturned() {
        if (originalBill.getForwardReferenceBills() != null && !originalBill.getForwardReferenceBills().isEmpty()) {
            for (Bill refBill : originalBill.getForwardReferenceBills()) {
                if (refBill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_ISSUE_RETURN
                    && !refBill.isRetired() && !refBill.isCancelled()) {
                    JsfUtil.addErrorMessage("This transfer issue has been returned and cannot be cancelled. Return Bill: " + refBill.getInsId());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validates that the items have not been accepted at the receiving end.
     * For transfer issues, we check if there's a receive bill that accepted the items.
     *
     * @return true if validation passes, false otherwise
     */
    private boolean validateNotAcceptedAtReceivingEnd() {
        if (originalBill.getForwardReferenceBills() != null && !originalBill.getForwardReferenceBills().isEmpty()) {
            for (Bill refBill : originalBill.getForwardReferenceBills()) {
                if (refBill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RECEIVE
                    && !refBill.isRetired() && !refBill.isCancelled()) {
                    JsfUtil.addErrorMessage("This transfer issue has been received at the destination and cannot be cancelled. Receive Bill: " + refBill.getInsId());
                    return false;
                }
            }
        }
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
        cancellationBill.setBillType(BillType.PharmacyTransferIssue);
        cancellationBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
        cancellationBill.setBillClassType(BillClassType.CancelledBill);

        // Set departments (same as original)
        cancellationBill.setDepartment(originalBill.getDepartment());
        cancellationBill.setInstitution(originalBill.getInstitution());
        cancellationBill.setFromDepartment(originalBill.getFromDepartment());
        cancellationBill.setFromInstitution(originalBill.getFromInstitution());
        cancellationBill.setToDepartment(originalBill.getToDepartment());
        cancellationBill.setToInstitution(originalBill.getToInstitution());
        cancellationBill.setToStaff(originalBill.getToStaff());

        // Set audit fields
        cancellationBill.setCreater(sessionController.getLoggedUser());
        cancellationBill.setCreatedAt(Calendar.getInstance().getTime());

        // Link to original bill
        cancellationBill.setBackwardReferenceBill(originalBill);

        // Set cancellation flag
        cancellationBill.setCancelled(false); // The cancellation bill itself is not cancelled
        cancellationBill.setCancelledBill(originalBill); // Points to the bill being cancelled

        // Initialize collections
        cancellationBill.setBillItems(new ArrayList<>());
    }

    /**
     * Creates cancellation bill items by reversing the original bill items.
     *
     * <h3>CRITICAL: Sign Reversal for Cancellation</h3>
     * <pre>
     * Original Issue:                Cancellation:
     * - Quantity: -10 packs         - Quantity: +10 packs
     * - Cost: -27.273               - Cost: +27.273
     * - Revenue: +50.0              - Revenue: -50.0
     * </pre>
     */
    private void createCancellationBillItems() {
        cancellationBillItems = new ArrayList<>();

        if (originalBill.getBillItems() == null || originalBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No items found in the original bill");
            return;
        }

        for (BillItem originalItem : originalBill.getBillItems()) {
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

            // Reverse quantity - original was negative, cancellation is positive
            double originalQty = originalItem.getQty();
            cancellationItem.setQty(Math.abs(originalQty)); // Make positive for cancellation

            // Create reversed pharmaceutical bill item
            createReversedPharmaceuticalBillItem(cancellationItem, originalItem);

            // Create reversed finance details
            createReversedFinanceDetails(cancellationItem, originalItem);

            // Set revenue values (will be negative for cancellation)
            double rate = originalItem.getRate();
            cancellationItem.setRate(rate);
            cancellationItem.setNetRate(rate);
            // Negative revenue for cancellation (refund)
            cancellationItem.setNetValue(-1 * rate * Math.abs(originalQty));
            cancellationItem.setGrossValue(-1 * rate * Math.abs(originalQty));

            cancellationBillItems.add(cancellationItem);
        }

        cancellationBill.setBillItems(cancellationBillItems);
    }

    /**
     * Creates reversed pharmaceutical bill item for cancellation.
     * Quantities are reversed (positive for cancellation).
     */
    private void createReversedPharmaceuticalBillItem(BillItem cancellationItem, BillItem originalItem) {
        PharmaceuticalBillItem originalPhItem = originalItem.getPharmaceuticalBillItem();
        PharmaceuticalBillItem cancellationPhItem = new PharmaceuticalBillItem();

        cancellationPhItem.setBillItem(cancellationItem);
        cancellationPhItem.setItemBatch(originalPhItem.getItemBatch());

        // Reverse quantity - original was negative, cancellation is positive
        double originalQtyInUnits = originalPhItem.getQty();
        cancellationPhItem.setQty(Math.abs(originalQtyInUnits)); // Positive for stock return

        // Copy rate values
        ItemBatch batch = originalPhItem.getItemBatch();
        if (batch != null) {
            double unitsPerPack = (originalItem.getItem() != null) ? originalItem.getItem().getDblValue() : 1.0;
            cancellationPhItem.setPurchaseRatePack(batch.getPurcahseRate() * unitsPerPack);
            cancellationPhItem.setRetailRatePack(batch.getRetailsaleRate() * unitsPerPack);
            cancellationPhItem.setCostRatePack(batch.getCostRate() * unitsPerPack);

            // Positive values for cancellation (cost restored)
            cancellationPhItem.setPurchaseValue(batch.getPurcahseRate() * Math.abs(originalQtyInUnits));
            cancellationPhItem.setRetailValue(batch.getRetailsaleRate() * Math.abs(originalQtyInUnits));
            cancellationPhItem.setCostValue(batch.getCostRate() * Math.abs(originalQtyInUnits));
        }

        cancellationItem.setPharmaceuticalBillItem(cancellationPhItem);
    }

    /**
     * Creates reversed finance details for cancellation bill item.
     *
     * <h3>CRITICAL: Reversed Sign Convention</h3>
     * <p>Since the original issue had negative quantities/costs and positive revenue,
     * the cancellation must reverse all signs:</p>
     * <ul>
     *   <li>Quantity: POSITIVE (stock returns to issuing dept)</li>
     *   <li>Cost: POSITIVE (cost burden restored)</li>
     *   <li>Revenue: NEGATIVE (refund/reversal)</li>
     * </ul>
     *
     * <h3>Example:</h3>
     * <pre>
     * Original Issue BIFD:           Cancellation BIFD:
     * - quantity: -10 packs          - quantity: +10 packs
     * - lineCost: -27.273            - lineCost: +27.273
     * - totalCost: -27.273           - totalCost: +27.273
     * - valueAtCostRate: -27.273     - valueAtCostRate: +27.273
     * </pre>
     *
     * @param cancellationItem The cancellation bill item
     * @param originalItem The original issue bill item
     */
    private void createReversedFinanceDetails(BillItem cancellationItem, BillItem originalItem) {
        BillItemFinanceDetails originalBifd = originalItem.getBillItemFinanceDetails();
        BillItemFinanceDetails cancellationBifd = new BillItemFinanceDetails();

        // Get values from original
        ItemBatch batch = originalItem.getPharmaceuticalBillItem().getItemBatch();
        double unitsPerPack = (originalBifd.getUnitsPerPack() != null)
            ? originalBifd.getUnitsPerPack().doubleValue()
            : 1.0;

        // Set units per pack
        cancellationBifd.setUnitsPerPack(BigDecimal.valueOf(unitsPerPack));

        // Quantities - POSITIVE for cancellation (stock returns)
        double qtyInPacks = Math.abs(cancellationItem.getQty());
        double qtyInUnits = Math.abs(cancellationItem.getPharmaceuticalBillItem().getQty());

        cancellationBifd.setQuantity(BigDecimal.valueOf(qtyInPacks)); // POSITIVE
        cancellationBifd.setTotalQuantity(BigDecimal.valueOf(qtyInPacks)); // POSITIVE
        cancellationBifd.setQuantityByUnits(BigDecimal.valueOf(qtyInUnits)); // POSITIVE

        // Rates - always positive (intrinsic properties) - get from original item
        BigDecimal rate = BigDecimal.valueOf(originalItem.getRate());
        cancellationBifd.setLineGrossRate(rate);
        cancellationBifd.setLineNetRate(rate);
        cancellationBifd.setGrossRate(rate);

        // Gross totals - negative for cancellation (refund)
        cancellationBifd.setLineGrossTotal(rate.multiply(BigDecimal.valueOf(-1 * qtyInPacks)));
        cancellationBifd.setLineNetTotal(rate.multiply(BigDecimal.valueOf(-1 * qtyInPacks)));
        cancellationBifd.setGrossTotal(rate.multiply(BigDecimal.valueOf(-1 * qtyInPacks)));

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

            // Cost values - POSITIVE for cancellation (cost burden restored)
            BigDecimal qtyInUnitsBD = BigDecimal.valueOf(qtyInUnits);
            cancellationBifd.setLineCost(costRate.multiply(qtyInUnitsBD)); // POSITIVE
            cancellationBifd.setBillCost(BigDecimal.ZERO);
            cancellationBifd.setTotalCost(costRate.multiply(qtyInUnitsBD)); // POSITIVE

            // Inventory valuations - POSITIVE for cancellation (inventory value restored)
            cancellationBifd.setValueAtCostRate(costRate.multiply(qtyInUnitsBD)); // POSITIVE
            cancellationBifd.setValueAtPurchaseRate(purchaseRate.multiply(qtyInUnitsBD)); // POSITIVE
            cancellationBifd.setValueAtRetailRate(retailRate.multiply(qtyInUnitsBD)); // POSITIVE
        }

        cancellationItem.setBillItemFinanceDetails(cancellationBifd);
    }

    /**
     * Calculates totals for the cancellation bill.
     * Revenue will be negative (refund).
     */
    private void calculateCancellationTotals() {
        double netTotal = 0.0;

        if (cancellationBillItems != null) {
            for (BillItem item : cancellationBillItems) {
                netTotal += item.getNetValue(); // This will be negative for cancellation
            }
        }

        cancellationBill.setNetTotal(netTotal); // Negative for refund
        cancellationBill.setTotal(netTotal);

        // Create and aggregate Bill-level finance details
        createBillFinanceDetails();
    }

    /**
     * Creates Bill-level BillFinanceDetails by aggregating from BillItemFinanceDetails.
     * This is critical for disbursement reports to show cancellations correctly.
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
                    // Aggregate cost values (POSITIVE for cancellation)
                    if (itemFd.getTotalCost() != null) {
                        totalCostValue = totalCostValue.add(itemFd.getTotalCost());
                    }

                    // Aggregate purchase values (POSITIVE for cancellation)
                    if (itemFd.getValueAtPurchaseRate() != null) {
                        totalPurchaseValue = totalPurchaseValue.add(itemFd.getValueAtPurchaseRate());
                    }

                    // Aggregate retail sale values (POSITIVE for cancellation)
                    if (itemFd.getValueAtRetailRate() != null) {
                        totalRetailSaleValue = totalRetailSaleValue.add(itemFd.getValueAtRetailRate());
                    }

                    // Aggregate line net totals (NEGATIVE for cancellation - revenue reversal)
                    if (itemFd.getLineNetTotal() != null) {
                        lineNetTotal = lineNetTotal.add(itemFd.getLineNetTotal());
                    }
                }
            }
        }

        // Set aggregated values
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalRetailSaleValue(totalRetailSaleValue);
        bfd.setLineNetTotal(lineNetTotal);

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
     *   <li>Saves cancellation bill structure</li>
     *   <li>Saves cancellation bill items with reversed finance details</li>
     *   <li>Generates bill numbers</li>
     *   <li>Reverses stock movements (adds stock back to issuing department)</li>
     *   <li>Deducts from user stocks if applicable</li>
     *   <li>Updates original bill as cancelled</li>
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

            // Re-validate the original bill hasn't been cancelled in the meantime
            Bill freshOriginalBill = billFacade.find(originalBill.getId());
            if (freshOriginalBill.isCancelled()) {
                JsfUtil.addErrorMessage("This bill has been cancelled by another user");
                makeNull();
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
                item.getBillItemFinanceDetails().setBillItem(item);
            }

            // Generate bill numbers
            String deptId = billNumberBean.institutionBillNumberGenerator(
                sessionController.getDepartment(),
                BillType.PharmacyTransferIssue,
                BillClassType.CancelledBill,
                BillNumberSuffix.PHTICAN
            );
            String insId = billNumberBean.institutionBillNumberGenerator(
                sessionController.getInstitution(),
                BillType.PharmacyTransferIssue,
                BillClassType.CancelledBill,
                BillNumberSuffix.PHTICAN
            );

            cancellationBill.setDeptId(deptId);
            cancellationBill.setInsId(insId);
            billFacade.edit(cancellationBill);

            // Reverse stock movements (add stock back to issuing department)
            reverseStockMovements();

            // Deduct from user stocks if original issue was to a staff member
            if (originalBill.getToStaff() != null) {
                deductUserStocks();
            }

            // Update original bill as cancelled
            freshOriginalBill.setCancelled(true);
            freshOriginalBill.setCancelledBill(cancellationBill);

            // Initialize forwardReferenceBills if null to avoid NPE
            if (freshOriginalBill.getForwardReferenceBills() == null) {
                freshOriginalBill.setForwardReferenceBills(new ArrayList<>());
            }
            freshOriginalBill.getForwardReferenceBills().add(cancellationBill);

            billFacade.edit(freshOriginalBill);

            // Reload the cancellation bill to get all persisted data
            Bill savedCancellationBill = billFacade.find(cancellationBill.getId());
            cancellationBill = savedCancellationBill;

            // Show success message
            JsfUtil.addSuccessMessage("Transfer Issue Cancelled Successfully. Cancellation Bill: " + cancellationBill.getInsId());

            // Set print preview
            printPreview = true;

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error cancelling transfer issue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reverses stock movements by adding stock back to the issuing department.
     *
     * <p>For each cancelled item, this method adds the quantity back to the
     * department stock using PharmacyBean methods. The stock is added with
     * positive quantity (opposite of the original negative quantity).</p>
     */
    private void reverseStockMovements() {
        for (BillItem item : cancellationBill.getBillItems()) {
            PharmaceuticalBillItem phItem = item.getPharmaceuticalBillItem();

            if (phItem != null && phItem.getItemBatch() != null) {
                // Add stock back to department
                double qtyToAdd = Math.abs(phItem.getQty());

                pharmacyBean.addToStock(
                    phItem,
                    qtyToAdd,
                    originalBill.getFromDepartment()
                );
            }
        }
    }

    /**
     * Deducts quantities from user stocks if the original issue was to a staff member.
     *
     * <p>If the original transfer issue was to a staff member, we need to deduct
     * the stock from that staff member's user stock since we're cancelling the issue.</p>
     */
    private void deductUserStocks() {
        for (BillItem item : cancellationBill.getBillItems()) {
            PharmaceuticalBillItem phItem = item.getPharmaceuticalBillItem();

            if (phItem != null && phItem.getItemBatch() != null) {
                double qtyToDeduct = Math.abs(phItem.getQty());

                // Deduct from staff stock
                pharmacyBean.deductFromStock(
                    phItem,
                    qtyToDeduct,
                    originalBill.getToStaff()
                );
            }
        }
    }

    /**
     * Cleans up all session-scoped properties.
     * Called after successful cancellation or when navigating away.
     */
    public void makeNull() {
        originalBill = null;
        originalBillId = null;
        cancellationBill = null;
        cancellationBillItems = null;
        cancellationReason = null;
        printPreview = false;
    }

    // Getters and Setters

    public Bill getOriginalBill() {
        return originalBill;
    }

    public void setOriginalBill(Bill originalBill) {
        this.originalBill = originalBill;
    }

    public Long getOriginalBillId() {
        return originalBillId;
    }

    public void setOriginalBillId(Long originalBillId) {
        this.originalBillId = originalBillId;
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

    public UserStockController getUserStockController() {
        return userStockController;
    }
}
