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
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.UserStock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.Item;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.service.pharmacy.PharmacyCostingService;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for handling Transfer Issue for Requests workflow.
 * This controller manages the pharmacy_transfer_issue.xhtml page (issue for transfer requests).
 *
 * Separated from TransferIssueController to focus only on request-based issues.
 *
 */
@Named
@SessionScoped
public class TransferIssueForRequestsController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @Inject
    private PharmacyCalculation pharmacyCalculation;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyCostingService pharmacyCostingService;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private UserStockController userStockController;
    @Inject
    private SessionController sessionController;
    @Inject
    private BillController billController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private VmpController vmpController;

    private Bill requestedBill;
    // Bill id used when navigating from DTO tables
    private Long requestedBillId;
    private Bill issuedBill;
    private boolean printPreview;

    private List<BillItem> billItems;
    private BillItem selectedBillItem;
    UserStockContainer userStockContainer;
    private List<Stock> substituteStocks;
    private Stock selectedSubstituteStock;
    private BillItem itemForSubstitution;

    /**
     * @deprecated Use {@link #navigateToPharmacyIssueForRequestsById()} which
     * loads the requested bill using its id from DTO tables.
     */
    @Deprecated
    public String navigateToPharmacyIssueForRequests() {
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        createRequestIssueBillItems(requestedBill);
        if (isFullyIssued(requestedBill)) {
            JsfUtil.addErrorMessage("All items have already been issued.");
            return "";
        }
        return "/pharmacy/pharmacy_transfer_issue?faces-redirect=true";
    }

    /**
     * Navigation helper when only the request bill id is available.
     */
    public String navigateToPharmacyIssueForRequestsById() {
        if (requestedBillId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        requestedBill = billFacade.find(requestedBillId);
        return navigateToPharmacyIssueForRequests();
    }

    public boolean isFullyIssued(Bill requestBill) {
        if (requestBill == null) {
            return false; // Null bills are not considered fully issued
        }

        // Fetch fresh bill items from database to ensure latest remainingQty values
        List<BillItem> freshBillItems = billController.billItemsOfBill(requestBill);
        if (freshBillItems == null || freshBillItems.isEmpty()) {
            return false; // Empty bills are not considered fully issued
        }

        for (BillItem requestItem : freshBillItems) {
            // Handle null remainingQty (legacy data) by using original quantity
            Double remainingQty = requestItem.getRemainingQty();
            if (remainingQty == null) {
                remainingQty = requestItem.getQty();
            }
            // Use remainingQty field from database - if > 0, still has items to issue
            if (remainingQty > 0.001) { // Add small tolerance for floating point precision
                return false; // Still has remaining quantity to issue
            }
        }

        return true; // All items are fully issued
    }

    public UserStockContainer getUserStockContainer() {
        if (userStockContainer == null) {
            userStockContainer = new UserStockContainer();
        }

        return userStockContainer;
    }

    public void setUserStockContainer(UserStockContainer userStockContainer) {
        this.userStockContainer = userStockContainer;
    }

    public void remove(BillItem billItem) {
        if (billItem.getTransUserStock() != null) {
            if (billItem.getTransUserStock().isRetired()) {
                JsfUtil.addErrorMessage("This Item Already removed");
                return;
            }

            userStockController.removeUserStock(billItem.getTransUserStock(), getSessionController().getLoggedUser());
        }

        // Remove by object instance instead of index to avoid removing wrong item
        getBillItems().remove(billItem);
        // Reindex all items sequentially after successful removal
        int serialNo = 0;
        for (BillItem b : getBillItems()) {
            b.setSearialNo(serialNo++);
        }
    }

    public void makeNull() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        requestedBill = null;
        issuedBill = null;
        printPreview = false;
        billItems = null;
        userStockContainer = null;
        selectedBillItem = null;
    }

    public TransferIssueForRequestsController() {
    }

    public Bill getRequestedBill() {
        if (requestedBill == null) {
            requestedBill = new BilledBill();

        }
        return requestedBill;
    }

    public Long getRequestedBillId() {
        return requestedBillId;
    }

    /**
     * Setter used by DTO based tables to pass only the bill id. The full
     * {@link Bill} entity is fetched here when required.
     */
    public void setRequestedBillId(Long requestedBillId) {
        this.requestedBillId = requestedBillId;
        if (requestedBillId != null) {
            this.requestedBill = billFacade.find(requestedBillId);
        } else {
            this.requestedBill = null;
        }
    }

    public void createRequestIssueBillItems(Bill requestedBill) {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        makeNull();
        this.requestedBill = requestedBill;
        issuedBill = null;
        generateBillComponent(requestedBill);
        calculateBillTotalsForTransferIssue(getIssuedBill());
    }

    /**
     * PERFORMANCE OPTIMIZED: Generates bill components using bulk DTO queries
     * instead of individual N+1 queries for each bill item.
     *
     * This method replaces individual calls to: -
     * getBilledIssuedByRequestedItem() for each item (N queries) -
     * getCancelledIssuedByRequestedItem() for each item (N queries) -
     * getStockByQty() for each item (N queries)
     *
     * With just 2 bulk queries total, regardless of item count.
     */
    public void generateBillComponent(Bill requestedBill) {
        // Save the user stock container if this is a new bill
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        List<BillItem> billItemsOfRequest = billController.billItemsOfBill(requestedBill);

        // Setup department details
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        getIssuedBill().setToDepartment(requestedBill.getFromDepartment());

        // OPTIMIZATION 1: Bulk fetch all calculations for all bill items (replaces 2N individual queries)
        java.util.Map<Long, com.divudi.core.data.dto.BillItemCalculationDTO> calculationsMap
                = getPharmacyCalculation().getBulkCalculationsForBillItems(billItemsOfRequest, BillTypeAtomic.PHARMACY_ISSUE);

        // OPTIMIZATION 2: Extract unique items and bulk fetch stock availability (replaces N individual queries)
        java.util.List<Item> uniqueItems = billItemsOfRequest.stream()
                .map(BillItem::getItem)
                .map(item -> {
                    if (item instanceof Ampp) {
                        Ampp ampp = (Ampp) item;
                        return ampp.getAmp() != null ? ampp.getAmp() : item;
                    } else {
                        return item;
                    }
                })
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<Long, java.util.List<com.divudi.core.data.dto.StockAvailabilityDTO>> stockAvailabilityMap
                = pharmacyBean.getBulkStockAvailability(uniqueItems, getSessionController().getDepartment());

        // Process each bill item using pre-fetched data (no additional database queries)
        for (BillItem billItemInRequest : billItemsOfRequest) {
            boolean flagStockFound = false;

            // Get calculations from pre-fetched data instead of individual queries
            com.divudi.core.data.dto.BillItemCalculationDTO calculations = calculationsMap.get(billItemInRequest.getId());
            if (calculations == null) {
                // Create default calculation if not found - use remaining quantity from database if available
                Double remainingFromDb = billItemInRequest.getRemainingQty();
                Double issuedQtyFromDb = billItemInRequest.getIssuedPhamaceuticalItemQty();
                double alreadyIssuedFromDb = (issuedQtyFromDb != null) ? issuedQtyFromDb : 0.0;
                calculations = new com.divudi.core.data.dto.BillItemCalculationDTO(
                        billItemInRequest.getId(), billItemInRequest.getQty(), alreadyIssuedFromDb, 0.0);
            } else {
            }

            billItemInRequest.setIssuedPhamaceuticalItemQty(calculations.getNetIssuedQty());
            // Update remaining quantity for consistency
            billItemInRequest.setRemainingQty(calculations.getRemainingQty());
            double quantityToIssue = getRemainingQuantityForItem(billItemInRequest);

            if (quantityToIssue <= 0.001) { // Use small tolerance for floating point precision
                continue; // Skip if nothing left to issue
            }

            Item stockItem = billItemInRequest.getItem();
            double packSize = 1.0;

            if (stockItem instanceof Ampp) {
                Ampp ampp = (Ampp) stockItem;
                packSize = ampp.getDblValue();  // Get pack size from AMPP before changing stockItem
                stockItem = ampp.getAmp();
            }
            if (stockItem instanceof Vmpp) {
                Vmpp vmpp = (Vmpp) stockItem;
                packSize = vmpp.getDblValue();  // Get pack size from VMPP
                // For now, skip VMPP items as mentioned in TODO
                // Future enhancement: stockItem = vmpp.getVmp();
                continue;
            }

            Double quantityToIssueInUnits = quantityToIssue * packSize;

            // Get stock availability from pre-fetched data instead of individual query
            java.util.List<com.divudi.core.data.dto.StockAvailabilityDTO> availableStocks
                    = stockAvailabilityMap.get(stockItem.getId());

            if (availableStocks == null || availableStocks.isEmpty()) {
                // Create empty bill item if no stock available
                createEmptyBillItem(billItemInRequest);
                continue;
            }

            Double totalIssuedQtyInUnits = 0.0;

            for (com.divudi.core.data.dto.StockAvailabilityDTO stockDTO : availableStocks) {
                if (totalIssuedQtyInUnits >= quantityToIssueInUnits) {
                    break;
                }

                Double thisTimeIssuingQtyInUnits = stockDTO.getAvailableStock();

                if (totalIssuedQtyInUnits + thisTimeIssuingQtyInUnits > quantityToIssueInUnits) {
                    thisTimeIssuingQtyInUnits = quantityToIssueInUnits - totalIssuedQtyInUnits;
                }

                if (thisTimeIssuingQtyInUnits <= 0) {
                    break;
                }

                // For AMPP items, ensure we only issue complete packs
                if (packSize > 1.0) {
                    // Check if we have enough units for at least one complete pack
                    if (thisTimeIssuingQtyInUnits < packSize) {
                        // Skip this stock as it doesn't have enough for a complete pack
                        continue;
                    }
                    // Round down to the nearest complete pack
                    double completePacksOnly = Math.floor(thisTimeIssuingQtyInUnits / packSize) * packSize;
                    thisTimeIssuingQtyInUnits = completePacksOnly;

                    // Check if after rounding we still have something to issue
                    if (thisTimeIssuingQtyInUnits <= 0) {
                        continue;
                    }
                }

                // Create minimal Stock entity from DTO for user stock validation only
                Stock stock = createStockFromDTO(stockDTO);

                if (!userStockController.isStockAvailable(stock, thisTimeIssuingQtyInUnits, getSessionController().getLoggedUser())) {
                    continue;
                }

                totalIssuedQtyInUnits += thisTimeIssuingQtyInUnits;

                // Create bill item using DTO data instead of entity traversal
                BillItem newBillItem = createBillItemFromStockDTO(
                        stockDTO, billItemInRequest, thisTimeIssuingQtyInUnits, packSize, getBillItems().size()
                );

                // Link user stock
                UserStock us = userStockController.saveUserStock(newBillItem, getSessionController().getLoggedUser(), usc);
                newBillItem.setTransUserStock(us);

                getBillItems().add(newBillItem);
                flagStockFound = true;
            }

            if (!flagStockFound) {
                createEmptyBillItem(billItemInRequest);
            }
        }
    }

    public void settle() {
        if (getIssuedBill().getToDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Department to Issue");
            return;
        }
        if (getIssuedBill().getToStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return;
        }
        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items are added to Transfer");
            return;
        }
        for (BillItem bi : getBillItems()) {
            if (bi.getPharmaceuticalBillItem().getItemBatch() != null) {
                if (bi.getPharmaceuticalBillItem().getStock().getStock() < bi.getPharmaceuticalBillItem().getQty()) {
                    JsfUtil.addErrorMessage("Available quantity is less than issued quantity in " + bi.getPharmaceuticalBillItem().getItemBatch().getItem().getName());
                    return;
                }
            } else if (bi.getPharmaceuticalBillItem().getItemBatch() == null) {
                if (bi.getPharmaceuticalBillItem().getQty() > 0) {
                    JsfUtil.addErrorMessage(bi.getItem().getName() + " is not available in the stock");
                    return;
                }
            }

            double remainingQty = getRemainingQuantityForItem(bi.getReferanceBillItem());
            double issuingQty = bi.getBillItemFinanceDetails().getQuantity() != null ? bi.getBillItemFinanceDetails().getQuantity().doubleValue() : 0.0;
            if (issuingQty > remainingQty) {
                JsfUtil.addErrorMessage("Issued quantity (" + issuingQty + ") is higher than remaining requested quantity (" + remainingQty + ") for " + bi.getItem().getName());
                return;
            }
        }

        //Remove Zero Qty Item
        List<BillItem> billItemList = new ArrayList<>();
        for (BillItem bi : getBillItems()) {
            if (bi.getPharmaceuticalBillItem().getQty() != 0.0) {
                billItemList.add(bi);
            }
        }

        //Replase the billItem with out Zero Qty Item
        setBillItems(billItemList);

        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items are added to Transfer");
            return;
        }

        saveBill();
        for (BillItem billItemsInIssue : getBillItems()) {
            BillItem originalOrderItem = billItemsInIssue.getReferanceBillItem();
            billItemsInIssue.getPharmaceuticalBillItem().setQty(0 - Math.abs(billItemsInIssue.getPharmaceuticalBillItem().getQty()));
            if (billItemsInIssue.getQty() == 0.0 || billItemsInIssue.getItem() instanceof Vmpp || billItemsInIssue.getItem() instanceof Vmp) {
                continue;
            }

            billItemsInIssue.setBill(getIssuedBill());
            billItemsInIssue.setCreatedAt(Calendar.getInstance().getTime());
            billItemsInIssue.setCreater(getSessionController().getLoggedUser());
            billItemsInIssue.setPharmaceuticalBillItem(billItemsInIssue.getPharmaceuticalBillItem());

            PharmaceuticalBillItem tmpPh = billItemsInIssue.getPharmaceuticalBillItem();
            billItemsInIssue.setPharmaceuticalBillItem(null);

            if (billItemsInIssue.getId() == null) {
                getBillItemFacade().create(billItemsInIssue);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            billItemsInIssue.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(billItemsInIssue);

            //Checking User Stock Entity
            if (!userStockController.isStockAvailable(tmpPh.getStock(), tmpPh.getQty(), getSessionController().getLoggedUser())) {
                billItemsInIssue.setTmpQty(0);
                getBillItemFacade().edit(billItemsInIssue);
                getIssuedBill().getBillItems().add(billItemsInIssue);
                continue;
            }

            //Remove Department Stock
            boolean returnFlag = pharmacyBean.deductFromStock(billItemsInIssue.getPharmaceuticalBillItem().getStock(),
                    Math.abs(billItemsInIssue.getPharmaceuticalBillItem().getQty()),
                    billItemsInIssue.getPharmaceuticalBillItem(),
                    getSessionController().getDepartment());
            if (returnFlag) {

                //Addinng Staff
                Stock staffStock = pharmacyBean.addToStock(billItemsInIssue.getPharmaceuticalBillItem(),
                        Math.abs(billItemsInIssue.getPharmaceuticalBillItem().getQty()), getIssuedBill().getToStaff());

                billItemsInIssue.getPharmaceuticalBillItem().setStaffStock(staffStock);

                originalOrderItem = billItemFacade.findWithoutCache(originalOrderItem.getId());

                // Null-safe handling for issuedPhamaceuticalItemQty to prevent NPE
                Double currentIssued = originalOrderItem.getIssuedPhamaceuticalItemQty();
                double currentIssuedValue = (currentIssued != null) ? currentIssued : 0.0;
                double issuedQtyThisTime = Math.abs(billItemsInIssue.getQty()); // Use absolute value since qty is negative for issues
                originalOrderItem.setIssuedPhamaceuticalItemQty(currentIssuedValue + issuedQtyThisTime);
                // Update remaining quantity to track what's left to issue
                Double remainingQty = originalOrderItem.getRemainingQty();
                double currentRemaining = (remainingQty != null) ? remainingQty : originalOrderItem.getQty();
                originalOrderItem.setRemainingQty(currentRemaining - issuedQtyThisTime);

                billItemFacade.editAndCommit(originalOrderItem);

                getBillItemFacade().edit(billItemsInIssue);
                getBillItemFacade().edit(originalOrderItem);
            } else {
                getBillItemFacade().edit(billItemsInIssue);
            }

            getPharmaceuticalBillItemFacade().edit(billItemsInIssue.getPharmaceuticalBillItem());

            getIssuedBill().getBillItems().add(billItemsInIssue);
        }

        // Calculate bill totals BEFORE persisting to ensure qty is negative
        calculateBillTotalsForTransferIssue(getIssuedBill());

        getIssuedBill().getBillItems().forEach(this::updateBillItemRateAndValueAndSave);

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else {
            // Use existing method for backward compatibility
            if (getSessionController().getApplicationPreference().isDepNumGenFromToDepartment()) {
                deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getIssuedBill().getToDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI);
            } else {
                deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI);
            }
        }

        // Handle Institution ID generation
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else {
            // Check if department strategy is enabled
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department to avoid consuming counter twice
            } else {
                // Use existing method for backward compatibility
                insId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI);
            }
        }
        getIssuedBill().setInsId(insId);
        getIssuedBill().setDeptId(deptId);

        getIssuedBill().setInstitution(getSessionController().getInstitution());
        getIssuedBill().setDepartment(getSessionController().getDepartment());

        getIssuedBill().setFromInstitution(getSessionController().getInstitution());
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());

        getIssuedBill().setToInstitution(getRequestedBill().getFromInstitution());
        getIssuedBill().setToDepartment(getRequestedBill().getFromDepartment());

        getIssuedBill().setCreater(getSessionController().getLoggedUser());
        getIssuedBill().setCreatedAt(Calendar.getInstance().getTime());

        getIssuedBill().setNetTotal(calculateBillNetTotal());

        getIssuedBill().setBackwardReferenceBill(getRequestedBill());
        getIssuedBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE);

        // Don't call edit() here - bill was already created in saveBill()
        // Calling edit/merge again can create a duplicate bill
        // getBillFacade().edit(getIssuedBill());
        createBillFinancialDetailsForPharmacyTransferIssueBill(getIssuedBill());
        // calculateBillTotalsForTransferIssue was already called before persistence at line 511

        //Update ReferenceBill
        //     getRequestedBill().setReferenceBill(getIssuedBill());
        getRequestedBill().getForwardReferenceBills().add(getIssuedBill());
        getBillFacade().edit(getRequestedBill());

        Bill b = getBillFacade().find(getIssuedBill().getId());
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        issuedBill = b;

        // Check if Transfer Request is fully issued and update fullyIssued status
        if (getRequestedBill() != null && !getRequestedBill().isFullyIssued()) {
            // Refresh the requested bill with latest data from database to ensure accurate remainingQty values
            Bill freshRequestedBill = getBillFacade().findWithoutCache(getRequestedBill().getId());
            if (isFullyIssued(freshRequestedBill)) {
                freshRequestedBill.setFullyIssued(true);
                freshRequestedBill.setFullyIssuedAt(new Date());
                freshRequestedBill.setFullyIssuedBy(getSessionController().getLoggedUser());
                getBillFacade().edit(freshRequestedBill);
                // Update the local reference as well
                getRequestedBill().setFullyIssued(true);
                getRequestedBill().setFullyIssuedAt(freshRequestedBill.getFullyIssuedAt());
                getRequestedBill().setFullyIssuedBy(freshRequestedBill.getFullyIssuedBy());
            }
        }

        printPreview = true;

    }

    private void updateBillItemRateAndValue(BillItem b) {
        BillItemFinanceDetails f = b.getBillItemFinanceDetails();
        double rate = b.getBillItemFinanceDetails().getLineGrossRate().doubleValue();

        // Use the user's input quantity from BillItemFinanceDetails instead of pre-calculated values
        BigDecimal userInputQtyInPacks = f.getQuantity() != null ? f.getQuantity().abs() : BigDecimal.ZERO;

        b.setRate(rate);
        b.setNetRate(rate);
        // Money movement (revenue) must be positive - use user input quantity
        b.setNetValue(rate * userInputQtyInPacks.doubleValue());
        b.setGrossValue(rate * userInputQtyInPacks.doubleValue());

        // Calculate units based on user input and pack size
        BigDecimal unitsPerPack = f.getUnitsPerPack() != null ? f.getUnitsPerPack() : BigDecimal.ONE;
        BigDecimal userInputQtyInUnits = userInputQtyInPacks.multiply(unitsPerPack);

        BigDecimal qtyInUnits = userInputQtyInUnits;
        BigDecimal qtyInPacks = userInputQtyInPacks;
        BigDecimal rateBig = BigDecimal.valueOf(rate);

        // Set unitsPerPack from the PharmaceuticalBillItem/DTO before computing pack rates
        if (f.getUnitsPerPack() == null || f.getUnitsPerPack().compareTo(BigDecimal.ZERO) == 0) {
            if (b.getItem() instanceof Ampp) {
                f.setUnitsPerPack(BigDecimal.valueOf(b.getItem().getDblValue()));
            } else {
                f.setUnitsPerPack(BigDecimal.ONE);
            }
        }

        // Don't override user input - preserve the quantity from BillItemFinanceDetails
        // The user input is already stored in qtyInPacks and qtyInUnits variables

        f.setLineGrossRate(rateBig);
        f.setLineNetRate(rateBig);
        // Money totals should be positive - use user input quantity
        f.setLineGrossTotal(rateBig.multiply(qtyInPacks));
        f.setLineNetTotal(rateBig.multiply(qtyInPacks));

        // Cost/purchase valuations should be negative for transfer out
        if (b.getPharmaceuticalBillItem() != null && b.getPharmaceuticalBillItem().getItemBatch() != null) {
            ItemBatch batch = b.getPharmaceuticalBillItem().getItemBatch();
            BigDecimal costRate = BigDecimal.valueOf(batch.getCostRate());
            f.setCostRate(costRate);
            f.setLineCostRate(costRate);
            f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(qtyInUnits)));
            f.setTotalCostRate(costRate);
            f.setTotalCost(BigDecimal.ZERO.subtract(costRate.multiply(qtyInUnits)));
            // Ensure purchaseRate is present on finance details
            if (f.getPurchaseRate() == null) {
                f.setPurchaseRate(BigDecimal.valueOf(batch.getPurcahseRate()));
            }
        } else {
            f.setLineCostRate(BigDecimal.ZERO);
            f.setLineCost(BigDecimal.ZERO);
            f.setTotalCostRate(BigDecimal.ZERO);
            f.setTotalCost(BigDecimal.ZERO);
        }

        // Ensure base fields exist
        f.setGrossRate(rateBig);

        // Quantity by units should be negative (stock out)
        f.setQuantityByUnits(BigDecimal.ZERO.subtract(qtyInUnits));
        f.setTotalQuantityByUnits(BigDecimal.ZERO.subtract(qtyInUnits));

        // Update PharmaceuticalBillItem quantities to match user input
        if (b.getPharmaceuticalBillItem() != null) {
            // Set negative quantities for issue (stock out)
            b.getPharmaceuticalBillItem().setQty(-qtyInUnits.doubleValue());
            b.getPharmaceuticalBillItem().setQtyPacks(-qtyInPacks.doubleValue());
        }

        // Update BillItem quantity to match user input (negative for issue)
        b.setQty(-qtyInPacks.doubleValue());
    }

    private void updateBillItemRateAndValueAndSave(BillItem b) {
        updateBillItemRateAndValue(b);
        billItemFacade.edit(b);
    }

    private double calculateBillNetTotal() {
        double value = 0;
        int serialNo = 0;
        for (BillItem b : getIssuedBill().getBillItems()) {
            double rate = b.getBillItemFinanceDetails().getLineGrossRate().doubleValue();
            // Use user-entered quantity from BillItemFinanceDetails instead of BillItem.qty
            // This ensures we use the actual issued quantity (e.g., 5) not the requested quantity (e.g., 10)
            BigDecimal userEnteredQty = b.getBillItemFinanceDetails().getQuantity();
            double qtyToUse = (userEnteredQty != null) ? Math.abs(userEnteredQty.doubleValue()) : Math.abs(b.getQty());
            value += rate * qtyToUse;
            b.setSearialNo(serialNo++);
        }
        return value;
    }

    /**
     * Creates bill financial details for pharmacy transfer issue bills.
     * Follows the same pattern as Direct Issue to ensure correct negative values for stock out.
     *
     * @param bill The transfer issue bill
     */
    private void createBillFinancialDetailsForPharmacyTransferIssueBill(Bill bill) {
        if (bill == null) {
            return;
        }

        if (bill.getBillFinanceDetails() == null) {
            bill.setBillFinanceDetails(new BillFinanceDetails());
        }

        Double billValueAtRetailRate = 0.0;
        Double billValueAtPurchaseRate = 0.0;
        Double billValueAtCostRate = 0.0;

        for (BillItem bi : bill.getBillItems()) {
            if (bi == null || bi.getPharmaceuticalBillItem() == null) {
                continue;
            }

            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (pbi.getItemBatch() == null) {
                continue;
            }

            Double quantityInUnits = pbi.getQty();
            Double retailRatePerUnit = pbi.getItemBatch().getRetailsaleRate();
            Double purchaseRatePerUnit = pbi.getItemBatch().getPurcahseRate();
            Double costRatePerUnit = pbi.getItemBatch().getCostRate();

            // Multiply by quantity (which is already negative for stock out)
            // This ensures values are negative
            double billItemRetailValue = retailRatePerUnit * quantityInUnits;
            double billItemPurchaseValue = purchaseRatePerUnit * quantityInUnits;
            double billItemCostValue = costRatePerUnit * quantityInUnits;

            billValueAtRetailRate += billItemRetailValue;
            billValueAtPurchaseRate += billItemPurchaseValue;
            billValueAtCostRate += billItemCostValue;
        }

        bill.getBillFinanceDetails().setTotalRetailSaleValue(BigDecimal.valueOf(billValueAtRetailRate));
        bill.getBillFinanceDetails().setTotalPurchaseValue(BigDecimal.valueOf(billValueAtPurchaseRate));
        bill.getBillFinanceDetails().setTotalCostValue(BigDecimal.valueOf(billValueAtCostRate));

        getBillFacade().edit(bill);
    }

    /**
     * Calculates bill totals for transfer issue.
     * For transfer issue (stock goes out):
     * - Bill revenue (netTotal, grossTotal) is POSITIVE (issuing dept receives money/value)
     * - BillItem qty is NEGATIVE (stock goes out)
     * - BillItem netValue is POSITIVE (revenue received)
     *
     * @param bill The transfer issue bill
     */
    private void calculateBillTotalsForTransferIssue(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return;
        }

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        int serialNo = 1;

        for (BillItem bi : bill.getBillItems()) {
            if (bi.isRetired()) {
                continue;
            }

            bi.setSearialNo(serialNo++);

            // For transfer issue: stock goes out so qty is negative
            double absQty = Math.abs(bi.getQty());
            bi.setQty(-absQty);

            // Revenue is positive (we receive money/value for stock going out)
            double netValue = absQty * bi.getRate();
            bi.setNetValue(netValue);

            grossTotal = grossTotal.add(BigDecimal.valueOf(netValue));
            netTotal = netTotal.add(BigDecimal.valueOf(netValue));
            lineGrossTotal = lineGrossTotal.add(BigDecimal.valueOf(netValue));
            lineNetTotal = lineNetTotal.add(BigDecimal.valueOf(netValue));
        }

        // Set bill totals as positive (revenue)
        bill.setTotal(grossTotal.doubleValue());
        bill.setNetTotal(netTotal.doubleValue());

        // Set bill finance details totals as positive (revenue)
        if (bill.getBillFinanceDetails() != null) {
            bill.getBillFinanceDetails().setGrossTotal(grossTotal);
            bill.getBillFinanceDetails().setLineGrossTotal(lineGrossTotal);
            bill.getBillFinanceDetails().setNetTotal(netTotal);
            bill.getBillFinanceDetails().setLineNetTotal(lineNetTotal);
        }

//        getBillFacade().edit(bill);
    }

    private BigDecimal determineTransferRate(ItemBatch itemBatch) {
        if (itemBatch == null) {
            return BigDecimal.ZERO;
        }

        boolean pharmacyTransferIsByPurchaseRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean pharmacyTransferIsByCostRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
        boolean pharmacyTransferIsByRetailRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate", true);

        if (pharmacyTransferIsByPurchaseRate) {
            return BigDecimal.valueOf(itemBatch.getPurcahseRate());
        } else if (pharmacyTransferIsByCostRate) {
            return BigDecimal.valueOf(itemBatch.getCostRate());
        } else {
            return BigDecimal.valueOf(itemBatch.getRetailsaleRate());
        }
    }

    private void updateFinancialsForTransferIssue(BillItemFinanceDetails fd) {
        if (fd == null || fd.getBillItem() == null) {
            return;
        }

        BillItem bi = fd.getBillItem();
        PharmaceuticalBillItem ph = bi.getPharmaceuticalBillItem();
        Item item = bi.getItem();

        BigDecimal qty = Optional.ofNullable(fd.getQuantity()).orElse(BigDecimal.ZERO);

        // Validate quantity doesn't exceed remaining requested quantity
        if (bi.getReferanceBillItem() != null) {
            double remainingQty = getRemainingQuantityForItem(bi.getReferanceBillItem());

            if (qty.doubleValue() > remainingQty) {
                // Cap the quantity to the maximum allowed
                qty = BigDecimal.valueOf(remainingQty);
                fd.setQuantity(qty);
            }
        }

        BigDecimal unitsPerPack = BigDecimal.ONE;
        if (item instanceof Ampp || item instanceof Vmpp) {
            unitsPerPack = item.getDblValue() > 0 ? BigDecimal.valueOf(item.getDblValue()) : BigDecimal.ONE;
        }

        fd.setUnitsPerPack(unitsPerPack);
        fd.setTotalQuantity(qty);
        fd.setQuantity(qty);

        BigDecimal grossRate = Optional.ofNullable(fd.getLineGrossRate()).orElse(determineTransferRate(ph.getItemBatch()).multiply(unitsPerPack));
        fd.setLineGrossRate(grossRate);

        BigDecimal lineGrossTotal = grossRate.multiply(qty);
        fd.setLineGrossTotal(lineGrossTotal);
        fd.setGrossTotal(lineGrossTotal);

        fd.setLineNetRate(grossRate);
        fd.setLineNetTotal(lineGrossTotal);
        fd.setNetTotal(lineGrossTotal);

        BigDecimal qtyByUnits = qty.multiply(unitsPerPack);
        fd.setQuantityByUnits(qtyByUnits);
        fd.setTotalQuantityByUnits(qtyByUnits);

        fd.setLineDiscount(BigDecimal.ZERO);
        fd.setLineExpense(BigDecimal.ZERO);
        fd.setLineTax(BigDecimal.ZERO);
        fd.setLineCost(BigDecimal.ZERO);
        fd.setTotalDiscount(BigDecimal.ZERO);
        fd.setTotalExpense(BigDecimal.ZERO);
        fd.setTotalTax(BigDecimal.ZERO);
        fd.setTotalCost(BigDecimal.ZERO);
        fd.setFreeQuantity(BigDecimal.ZERO);
        fd.setFreeQuantityByUnits(BigDecimal.ZERO);

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);

        if (ph != null) {
            ph.setQty(qtyByUnits.doubleValue());
            ph.setQtyPacks(qty.doubleValue());
        }

        bi.setQty(qty.doubleValue());
        bi.setRate(grossRate.doubleValue());
        bi.setNetValue(lineGrossTotal.doubleValue());
    }

    public void onQuantityChangeForTransferIssue(BillItem bi) {
        if (bi == null) {
            return;
        }

        // Validate quantity doesn't exceed remaining requested quantity
        if (bi.getReferanceBillItem() != null) {
            double remainingQty = getRemainingQuantityForItem(bi.getReferanceBillItem());
            double currentIssuingQty = bi.getBillItemFinanceDetails().getQuantity() != null ? bi.getBillItemFinanceDetails().getQuantity().doubleValue() : 0.0;

            if (currentIssuingQty > remainingQty) {
                JsfUtil.addErrorMessage("Cannot issue " + currentIssuingQty + " units of " + bi.getItem().getName() + ". Only " + remainingQty + " units remaining to be issued.");
                // Reset to maximum allowed quantity
                bi.getBillItemFinanceDetails().setQuantity(BigDecimal.valueOf(remainingQty));
            }
        }

        updateFinancialsForTransferIssue(bi.getBillItemFinanceDetails());
        calculateBillTotalsForTransferIssue(getIssuedBill());
    }

    public void onLineGrossRateChangeForTransferIssue(BillItem bi) {
        if (bi == null) {
            return;
        }
        updateFinancialsForTransferIssue(bi.getBillItemFinanceDetails());
        calculateBillTotalsForTransferIssue(getIssuedBill());
    }

    public void onEditDepartmentTransfer(BillItem billItem) {
        double availableStock = pharmacyBean.getBatchStockQty(billItem.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());

        if (availableStock < billItem.getPharmaceuticalBillItem().getQtyInUnit()) {
            billItem.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(billItem.getPharmaceuticalBillItem().getStock(), billItem.getPharmaceuticalBillItem().getQtyInUnit(), getSessionController().getLoggedUser())) {
            billItem.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        userStockController.updateUserStock(billItem.getTransUserStock(), billItem.getPharmaceuticalBillItem().getQtyInUnit());

    }

    public void displayItemDetails(BillItem tmp) {
        getPharmacyController().fillItemDetails(tmp.getItem());
    }

    public void prepareBatchDetails(BillItem bi) {
        selectedBillItem = bi;
    }

    public void prepareSubstitute(BillItem bi) {
        itemForSubstitution = bi;
        selectedSubstituteStock = null;
        substituteStocks = new ArrayList<>();
        if (bi != null && bi.getItem() instanceof Amp) {
            Amp amp = (Amp) bi.getItem();
            if (amp.getVmp() != null) {
                List<Amp> amps = vmpController.ampsOfVmp(amp.getVmp());
                for (Amp substituteAmp : amps) {
                    List<Stock> stocks = pharmacyBean.getStockByQty(substituteAmp, sessionController.getDepartment());
                    if (stocks != null) {
                        for (Stock stock : stocks) {
                            if (stock.getStock() > 0 && stock.getItemBatch() != null && stock.getItemBatch().getDateOfExpire() != null) {
                                Date currentDate = new Date();
                                if (stock.getItemBatch().getDateOfExpire().after(currentDate)) {
                                    substituteStocks.add(stock);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void replaceSelectedSubstitute() {
        if (itemForSubstitution == null || selectedSubstituteStock == null) {
            JsfUtil.addErrorMessage("Please select a substitute stock.");
            return;
        }

        // Update the bill item with selected stock details
        itemForSubstitution.setItem(selectedSubstituteStock.getItemBatch().getItem());

        PharmaceuticalBillItem phItem = itemForSubstitution.getPharmaceuticalBillItem();
        if (phItem == null) {
            phItem = new PharmaceuticalBillItem();
            phItem.setBillItem(itemForSubstitution);
            itemForSubstitution.setPharmaceuticalBillItem(phItem);
        }

        // Set stock and batch details
        phItem.setStock(selectedSubstituteStock);
        phItem.setItemBatch(selectedSubstituteStock.getItemBatch());
        phItem.setDoe(selectedSubstituteStock.getItemBatch().getDateOfExpire());
        phItem.setPurchaseRate(selectedSubstituteStock.getItemBatch().getPurcahseRate());
        phItem.setRetailRateInUnit(selectedSubstituteStock.getItemBatch().getRetailsaleRate());

        // Update rates in pharmaceutical bill item
        phItem.setPurchaseRatePack(selectedSubstituteStock.getItemBatch().getPurcahseRate());
        phItem.setRetailRatePack(selectedSubstituteStock.getItemBatch().getRetailsaleRate());
        phItem.setCostRate(selectedSubstituteStock.getItemBatch().getCostRate());
        phItem.setCostRatePack(selectedSubstituteStock.getItemBatch().getCostRate());

        // Update financials
        BillItemFinanceDetails financeDetails = itemForSubstitution.getBillItemFinanceDetails();
        if (financeDetails != null) {
            BigDecimal transferRate = determineTransferRate(selectedSubstituteStock.getItemBatch());
            financeDetails.setLineGrossRate(transferRate);
            financeDetails.setLineNetRate(transferRate);

            // Update cost and retail rates
            financeDetails.setLineCostRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getCostRate()));
            financeDetails.setRetailSaleRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getRetailsaleRate()));

            // Update values at different rates
            BigDecimal qty = financeDetails.getQuantity() != null ? financeDetails.getQuantity() : BigDecimal.ONE;
            financeDetails.setValueAtCostRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getCostRate()).multiply(qty));
            financeDetails.setValueAtPurchaseRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getPurcahseRate()).multiply(qty));
            financeDetails.setValueAtRetailRate(BigDecimal.valueOf(selectedSubstituteStock.getItemBatch().getRetailsaleRate()).multiply(qty));
        }

        updateFinancialsForTransferIssue(itemForSubstitution.getBillItemFinanceDetails());
        calculateBillTotalsForTransferIssue(getIssuedBill());

        JsfUtil.addSuccessMessage("Stock replaced successfully.");
    }

    private void saveBill() {
        getIssuedBill().setReferenceBill(getRequestedBill());
        if (getIssuedBill().getId() == null) {
            getBillFacade().create(getIssuedBill());
        } else {
            getBillFacade().edit(getIssuedBill());
        }
    }

    public Bill getIssuedBill() {
        if (issuedBill == null) {
            issuedBill = new BilledBill();
            issuedBill.setBillType(BillType.PharmacyTransferIssue);
            issuedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE);
        }
        return issuedBill;
    }

    public void setIssuedBill(Bill issuedBill) {
        this.issuedBill = issuedBill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
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

    public PharmacyCalculation getPharmacyCalculation() {
        return pharmacyCalculation;
    }

    public void setPharmacyCalculation(PharmacyCalculation pharmacyCalculation) {
        this.pharmacyCalculation = pharmacyCalculation;
    }

    public void setRequestedBill(Bill requestedBill) {
        this.requestedBill = requestedBill;
    }

    public BillItem getSelectedBillItem() {
        return selectedBillItem;
    }

    public void setSelectedBillItem(BillItem selectedBillItem) {
        this.selectedBillItem = selectedBillItem;
    }

    public List<Stock> getSubstituteStocks() {
        return substituteStocks;
    }

    public void setSubstituteStocks(List<Stock> substituteStocks) {
        this.substituteStocks = substituteStocks;
    }

    public Stock getSelectedSubstituteStock() {
        return selectedSubstituteStock;
    }

    public void setSelectedSubstituteStock(Stock selectedSubstituteStock) {
        this.selectedSubstituteStock = selectedSubstituteStock;
    }

    public BillItem getItemForSubstitution() {
        return itemForSubstitution;
    }

    public void setItemForSubstitution(BillItem itemForSubstitution) {
        this.itemForSubstitution = itemForSubstitution;
    }

    public double getRemainingQuantityForItem(BillItem referenceItem) {
        if (referenceItem == null) {
            return 0.0;
        }
        double requestedQty = referenceItem.getQty();
        double alreadyIssued = referenceItem.getIssuedPhamaceuticalItemQty();
        return Math.max(0.0, requestedQty - alreadyIssued);
    }

    // ------------------------------------------------------------------
    // Helper Methods for DTO-based Optimization
    // ------------------------------------------------------------------
    /**
     * Creates a minimal Stock entity from DTO data for user stock validation.
     * Only populates fields needed for isStockAvailable() check.
     */
    private Stock createStockFromDTO(com.divudi.core.data.dto.StockAvailabilityDTO dto) {
        Stock stock = new Stock();
        stock.setId(dto.getStockId());
        stock.setStock(dto.getAvailableStock());

        ItemBatch batch = new ItemBatch();
        batch.setId(dto.getItemBatchId());
        batch.setBatchNo(dto.getBatchNo());
        batch.setDateOfExpire(dto.getDateOfExpire());
        batch.setPurcahseRate(dto.getPurchaseRate());
        batch.setRetailsaleRate(dto.getRetailRate());
        batch.setCostRate(dto.getCostRate());

        stock.setItemBatch(batch);
        return stock;
    }

    /**
     * Creates a complete BillItem from stock DTO data, avoiding entity
     * traversals.
     */
    private BillItem createBillItemFromStockDTO(com.divudi.core.data.dto.StockAvailabilityDTO stockDTO,
            BillItem referenceItem, Double qtyInUnits,
            double packSize, int serialNo) {
        BillItem newBillItem = new BillItem();
        newBillItem.setSearialNo(serialNo);
        newBillItem.setItem(referenceItem.getItem());
        newBillItem.setReferanceBillItem(referenceItem);
        newBillItem.setQty(qtyInUnits / packSize);

        // Create pharmaceutical bill item using DTO data
        PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
        phItem.setBillItem(newBillItem);
        phItem.setQty(qtyInUnits);
        phItem.setPurchaseRate(stockDTO.getPurchaseRate());
        phItem.setRetailRateInUnit(stockDTO.getRetailRate());
        phItem.setDoe(stockDTO.getDateOfExpire());

        // Create minimal entities for required relationships using DTO data
        ItemBatch batch = new ItemBatch();
        batch.setId(stockDTO.getItemBatchId());
        batch.setBatchNo(stockDTO.getBatchNo());
        batch.setPurcahseRate(stockDTO.getPurchaseRate());
        batch.setRetailsaleRate(stockDTO.getRetailRate());
        batch.setCostRate(stockDTO.getCostRate());
        batch.setDateOfExpire(stockDTO.getDateOfExpire());
        phItem.setItemBatch(batch);

        Stock stock = new Stock();
        stock.setId(stockDTO.getStockId());
        stock.setStock(stockDTO.getAvailableStock());
        stock.setItemBatch(batch);
        phItem.setStock(stock);

        if (packSize != 1.0) {
            phItem.setQtyPacks(qtyInUnits / packSize);
        }

        newBillItem.setPharmaceuticalBillItem(phItem);

        // Set financial details using DTO data
        setFinancialDetailsFromDTO(newBillItem, stockDTO, packSize);

        return newBillItem;
    }

    /**
     * Sets financial details for a bill item using DTO data.
     */
    private void setFinancialDetailsFromDTO(BillItem billItem, com.divudi.core.data.dto.StockAvailabilityDTO stockDTO, double packSize) {
        BigDecimal qty = BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getQty() / packSize);
        BigDecimal transferRate = determineTransferRate(billItem.getPharmaceuticalBillItem().getItemBatch());
        BigDecimal purchaseRate = BigDecimal.valueOf(stockDTO.getPurchaseRate());
        BigDecimal retailRate = BigDecimal.valueOf(stockDTO.getRetailRate());
        BigDecimal costRate = BigDecimal.valueOf(stockDTO.getCostRate());
        BigDecimal packSizeBD = BigDecimal.valueOf(packSize);

        // Initialize finance details if null
        if (billItem.getBillItemFinanceDetails() == null) {
            billItem.setBillItemFinanceDetails(new BillItemFinanceDetails());
        }

        BillItemFinanceDetails financeDetails = billItem.getBillItemFinanceDetails();

        // Set unitsPerPack from the packSize parameter before computing pack rates
        financeDetails.setUnitsPerPack(packSizeBD);

        // Calculate unit quantities for consistency
        BigDecimal qtyInUnits = BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getQty());

        // Quantities
        financeDetails.setQuantity(qty);
        financeDetails.setTotalQuantity(qty);
        financeDetails.setQuantityByUnits(qtyInUnits);

        // Transfer rates and values (net values - what the transfer is priced at)
        BigDecimal packRate = transferRate.multiply(packSizeBD);
        financeDetails.setLineGrossRate(packRate);
        financeDetails.setLineNetRate(packRate);
        // Calculate totals using pack quantities and pack rates (not unit quantities)
        financeDetails.setLineGrossTotal(packRate.multiply(qty));
        financeDetails.setLineNetTotal(packRate.multiply(qty));
        financeDetails.setNetTotal(packRate.multiply(qty));

        // Purchase rate and value (for purchase value reporting) - use unit quantities
        financeDetails.setValueAtPurchaseRate(purchaseRate.multiply(qtyInUnits));

        // Retail rate and value (for sale value reporting) - use unit quantities
        financeDetails.setRetailSaleRate(retailRate);
        financeDetails.setValueAtRetailRate(retailRate.multiply(qtyInUnits));

        // Cost rate and value - use unit quantities for per-unit cost rates
        financeDetails.setLineCostRate(costRate);
        financeDetails.setLineCost(costRate.multiply(qtyInUnits));
        financeDetails.setTotalCost(costRate.multiply(qtyInUnits));
        financeDetails.setTotalCostRate(costRate);
        financeDetails.setValueAtCostRate(costRate.multiply(qtyInUnits));
    }

    /**
     * Creates an empty bill item when no stock is available for an item.
     */
    private void createEmptyBillItem(BillItem referenceItem) {
        BillItem bItem = new BillItem();
        bItem.setSearialNo(getBillItems().size());
        bItem.setItem(referenceItem.getItem());
        bItem.setReferanceBillItem(referenceItem);
        getBillItems().add(bItem);
    }

}
