/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.StockQty;
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
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.CommonFunctionsProxy;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.service.BillService;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.StockBill;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.service.pharmacy.PharmacyCostingService;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class TransferIssueController implements Serializable {

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
    private BillService billService;
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
    private NotificationController notificationController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private CommonFunctionsProxy commonFunctionsProxy;
    @Inject
    private VmpController vmpController;

    private Bill requestedBill;
    // Bill id used when navigating from DTO tables
    private Long requestedBillId;
    private Bill issuedBill;
    private boolean printPreview;
    private boolean showAllBillFormats = false;
    private Date fromDate;
    private Date toDate;

    private List<BillItem> billItems;
    private BillItem billItem;
    private BillItem selectedBillItem;
    private Double qty;
    private Stock tmpStock;
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

//    public boolean isFullyIssued() {
//        for (BillItem originalItem : billItems) {
//
//            if (originalItem.getIssuedPhamaceuticalItemQty() == originalItem.getQty()) {
//                if (originalItem.getPharmaceuticalBillItem().getItemBatch() == null) {
//                    continue;
//                }
//                return true;
//            }
//        }
//
//        return false;
//    }
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

    public String navigateToPharmacyDirectIssueForRequests() {
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        createRequestIssueBillItems(requestedBill);
        return "/pharmacy/pharmacy_transfer_issue_direct_department?faces-redirect=true";
    }

    public String navigateToPharmacyIssueFromGrn() {
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        pharmacyController.clearItemHistory();
        createGrnIssueBillItems(requestedBill);
        return "/pharmacy/pharmacy_transfer_issue?faces-redirect=true";
    }

    public String navigateToListPharmacyIssueRequests() {
        return "/pharmacy/pharmacy_transfer_request_list?faces-redirect=true";
    }

    public String navigateToDirectPharmacyIssue() {
        createDirectIssueBillItems();
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());

        return "/pharmacy/pharmacy_transfer_issue_direct_department?faces-redirect=true";
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

        getBillItems().remove(billItem.getSearialNo());
        int serialNo = 0;
        for (BillItem b : getBillItems()) {
            b.setSearialNo(serialNo++);
        }
    }

    public void removeBillItem(BillItem billItem) {
        getBillItems().remove(billItem);
        int serialNo = 0;
        for (BillItem b : getBillItems()) {
            b.setSearialNo(serialNo++);
        }
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getIssuedBill(), getBillItems());
    }

    public void makeNull() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        requestedBill = null;
        issuedBill = null;
        printPreview = false;
        fromDate = null;
        toDate = null;
        billItems = null;
        userStockContainer = null;
        tmpStock = null;
        selectedBillItem = null;
    }

    public TransferIssueController() {
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
        generateBillComponent();
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getIssuedBill(), getBillItems());
    }

    public void createGrnIssueBillItems(Bill grn) {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        makeNull();
        requestedBill = grn;
        issuedBill = null;
        generateBillComponentsForIssueBillFromGrn(requestedBill);
    }

    public void createDirectIssueBillItems() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        makeNull();
    }

    /**
     * PERFORMANCE OPTIMIZED: Generates bill components using bulk DTO queries 
     * instead of individual N+1 queries for each bill item.
     * 
     * This method replaces individual calls to:
     * - getBilledIssuedByRequestedItem() for each item (N queries)
     * - getCancelledIssuedByRequestedItem() for each item (N queries)  
     * - getStockByQty() for each item (N queries)
     * 
     * With just 2 bulk queries total, regardless of item count.
     */
    public void generateBillComponent() {
        // Save the user stock container if this is a new bill
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        List<BillItem> billItemsOfRequest = billController.billItemsOfBill(getRequestedBill());

        // Setup department details
        getIssuedBill().setDepartment(requestedBill.getDepartment());
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        getIssuedBill().setToDepartment(requestedBill.getDepartment());

        // OPTIMIZATION 1: Bulk fetch all calculations for all bill items (replaces 2N individual queries)
        java.util.Map<Long, com.divudi.core.data.dto.BillItemCalculationDTO> calculationsMap = 
            getPharmacyCalculation().getBulkCalculationsForBillItems(billItemsOfRequest, BillType.PharmacyTransferIssue);

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

        java.util.Map<Long, java.util.List<com.divudi.core.data.dto.StockAvailabilityDTO>> stockAvailabilityMap = 
            pharmacyBean.getBulkStockAvailability(uniqueItems, getSessionController().getDepartment());

        // Process each bill item using pre-fetched data (no additional database queries)
        for (BillItem billItemInRequest : billItemsOfRequest) {
            boolean flagStockFound = false;

            // Get calculations from pre-fetched data instead of individual queries
            com.divudi.core.data.dto.BillItemCalculationDTO calculations = calculationsMap.get(billItemInRequest.getId());
            if (calculations == null) {
                // Create default calculation if not found
                calculations = new com.divudi.core.data.dto.BillItemCalculationDTO(
                    billItemInRequest.getId(), billItemInRequest.getQty(), 0.0, 0.0);
            }

            billItemInRequest.setIssuedPhamaceuticalItemQty(calculations.getNetIssuedQty());
            double quantityToIssue = calculations.getRemainingQty();

            if (quantityToIssue <= 0.001) { // Use small tolerance for floating point precision
                continue; // Skip if nothing left to issue
            }

            Item stockItem = billItemInRequest.getItem();
            double packSize = 1.0;

            if (stockItem instanceof Ampp) {
                Ampp ampp = (Ampp) stockItem;
                stockItem = ampp.getAmp();
                packSize = stockItem.getDblValue();
            }
            if (stockItem instanceof Vmpp) {
                packSize = stockItem.getDblValue();
                //TODO: Add Support to VMPP as a new issue
                continue;
            }

            Double quantityToIssueInUnits = quantityToIssue * packSize;

            // Get stock availability from pre-fetched data instead of individual query
            java.util.List<com.divudi.core.data.dto.StockAvailabilityDTO> availableStocks = 
                stockAvailabilityMap.get(stockItem.getId());
            
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

    public void generateBillComponentsForIssueBillFromGrn(Bill grn) {
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        List<BillItem> grnBillItems = billController.billItemsOfBill(grn);
        getIssuedBill().setDepartment(null);
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        getIssuedBill().setToDepartment(null);

        for (BillItem grnBillItem : grnBillItems) {
            BillItem newTransferBillItem = new BillItem();
            if (grnBillItem.getPharmaceuticalBillItem() == null) {
                continue;
            }
            PharmaceuticalBillItem transferIssueBillItem = new PharmaceuticalBillItem();

            transferIssueBillItem.setItemBatch(grnBillItem.getPharmaceuticalBillItem().getItemBatch());
            transferIssueBillItem.setStock(grnBillItem.getPharmaceuticalBillItem().getStock());
            transferIssueBillItem.setStockHistory(grnBillItem.getPharmaceuticalBillItem().getStockHistory());
            transferIssueBillItem.setDoe(grnBillItem.getPharmaceuticalBillItem().getDoe());
            transferIssueBillItem.setStringValue(grnBillItem.getPharmaceuticalBillItem().getStringValue());

            if (grnBillItem.getPharmaceuticalBillItem().getQty() != 0.0 && grnBillItem.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {
                transferIssueBillItem.setQty(grnBillItem.getPharmaceuticalBillItem().getQty());
                transferIssueBillItem.setQtyInUnit(grnBillItem.getPharmaceuticalBillItem().getQtyInUnit());
            } else if (grnBillItem.getPharmaceuticalBillItem().getQty() != 0.0) {
                transferIssueBillItem.setQty(grnBillItem.getPharmaceuticalBillItem().getQty());
                transferIssueBillItem.setQtyInUnit(grnBillItem.getPharmaceuticalBillItem().getQty());
            } else if (grnBillItem.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {
                transferIssueBillItem.setQty(grnBillItem.getPharmaceuticalBillItem().getQtyInUnit());
                transferIssueBillItem.setQtyInUnit(grnBillItem.getPharmaceuticalBillItem().getQtyInUnit());
            }

            if (grnBillItem.getPharmaceuticalBillItem().getFreeQty() != 0.0 && grnBillItem.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {

                transferIssueBillItem.setQty(transferIssueBillItem.getQty() + grnBillItem.getPharmaceuticalBillItem().getFreeQty());

                double totalQty = transferIssueBillItem.getQty() + grnBillItem.getPharmaceuticalBillItem().getFreeQty();

                transferIssueBillItem.setQtyInUnit(transferIssueBillItem.getQtyInUnit() + grnBillItem.getPharmaceuticalBillItem().getFreeQtyInUnit());

            } else if (grnBillItem.getPharmaceuticalBillItem().getFreeQty() != 0.0) {
                transferIssueBillItem.setQty(transferIssueBillItem.getQty() + grnBillItem.getPharmaceuticalBillItem().getFreeQty());
                transferIssueBillItem.setQtyInUnit(transferIssueBillItem.getQtyInUnit() + grnBillItem.getPharmaceuticalBillItem().getFreeQty());
            } else if (grnBillItem.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {
                transferIssueBillItem.setQty(transferIssueBillItem.getQty() + grnBillItem.getPharmaceuticalBillItem().getFreeQtyInUnit());
                transferIssueBillItem.setQtyInUnit(transferIssueBillItem.getQtyInUnit() + grnBillItem.getPharmaceuticalBillItem().getFreeQtyInUnit());
            }

            transferIssueBillItem.setPurchaseRate(grnBillItem.getPharmaceuticalBillItem().getPurchaseRate());
            transferIssueBillItem.setLastPurchaseRate(grnBillItem.getPharmaceuticalBillItem().getLastPurchaseRate());
            transferIssueBillItem.setRetailRate(grnBillItem.getPharmaceuticalBillItem().getRetailRate());
            transferIssueBillItem.setWholesaleRate(grnBillItem.getPharmaceuticalBillItem().getWholesaleRate());
            transferIssueBillItem.setStock(grnBillItem.getPharmaceuticalBillItem().getStock());
            transferIssueBillItem.setStaffStock(grnBillItem.getPharmaceuticalBillItem().getStaffStock());
            transferIssueBillItem.setBillItem(newTransferBillItem);

            newTransferBillItem.setItem(grnBillItem.getItem());
            newTransferBillItem.setItemId(grnBillItem.getItemId());
            newTransferBillItem.setNetRate(grnBillItem.getNetRate());

            newTransferBillItem.setQty(grnBillItem.getQty());

            newTransferBillItem.setGrossValue(grnBillItem.getGrossValue());
            newTransferBillItem.setNetValue(grnBillItem.getNetValue());

            newTransferBillItem.setPharmaceuticalBillItem(transferIssueBillItem);

            newTransferBillItem.setSearialNo(getBillItems().size());
            newTransferBillItem.setItem(grnBillItem.getItem());
            newTransferBillItem.setReferanceBillItem(grnBillItem);
//            ni.setTmpQty(0);
            getBillItems().add(newTransferBillItem);

        }

    }

    public void settleDirectIssue() {
        if (getIssuedBill().getToDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Department to Issue");
            return;
        }
        if (Objects.equals(getIssuedBill().getFromDepartment().getId(),
                getIssuedBill().getToDepartment().getId())) {
            JsfUtil.addErrorMessage("You can't issue to the same department");
            return;
        }
        if (getIssuedBill().getToStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return;
        }
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items");
            return;
        }
        boolean pharmacyTransferIsByPurchaseRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean pharmacyTransferIsByCostRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
        boolean pharmacyTransferIsByRetailRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate", true);
        if (!pharmacyTransferIsByPurchaseRate && !pharmacyTransferIsByCostRate && !pharmacyTransferIsByRetailRate) {
            pharmacyTransferIsByRetailRate = true;
        }
        for (BillItem bi : getBillItems()) {
            if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getPharmaceuticalBillItem().getQty(), getSessionController().getLoggedUser())) {
                JsfUtil.addErrorMessage("No adequate stocks for " + bi.getItem().getName());
                return;
            }
        }

        if (getIssuedBill().getId() == null) {
            getBillFacade().create(getIssuedBill());
        } else {
            getBillFacade().edit(getIssuedBill());
        }
        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please Add Bill Items");
            return;
        }
        boolean stockWasNotSufficientToIssueFound = false;
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQty(0 - i.getPharmaceuticalBillItem().getQty());
            if (i.getQty() == 0.0 || i.getItem() instanceof Vmpp || i.getItem() instanceof Vmp) {
                continue;
            }
            i.setBill(getIssuedBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            //Checking User Stock Entity
            if (!userStockController.isStockAvailable(i.getPharmaceuticalBillItem().getStock(), i.getPharmaceuticalBillItem().getQty(), getSessionController().getLoggedUser())) {
                i.setQty(0.0);
                getBillItemFacade().edit(i);
                getIssuedBill().getBillItems().add(i);
                continue;
            }

            boolean returnFlag = pharmacyBean.deductFromStock(i.getPharmaceuticalBillItem().getStock(),
                    Math.abs(i.getPharmaceuticalBillItem().getQty()),
                    i.getPharmaceuticalBillItem(),
                    getSessionController().getDepartment());
            if (returnFlag) {
                Stock staffStock = pharmacyBean.addToStock(i.getPharmaceuticalBillItem(),
                        Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), getIssuedBill().getToStaff());
                i.getPharmaceuticalBillItem().setStaffStock(staffStock);
            } else {
                i.setQty(0.0);

            }
            getBillItemFacade().edit(i);
            getIssuedBill().getBillItems().add(i);
        }

        if (stockWasNotSufficientToIssueFound) {
            JsfUtil.addErrorMessage("The Current Stock was not sufficient to issue some items. Other items issued except those items. Please check the bill and issue the migging items seperately in a new issue.");
            pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getIssuedBill(), getBillItems());
        }

        getIssuedBill().getBillItems().forEach(this::updateBillItemRateAndValueAndSaveForDirectIssue);

        getIssuedBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        if (getSessionController().getApplicationPreference().isDepNumGenFromToDepartment()) {
            getIssuedBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getIssuedBill().getToDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        } else {
            getIssuedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        }

        getIssuedBill().setDepartment(getIssuedBill().getFromDepartment());
        getIssuedBill().setToInstitution(getIssuedBill().getToDepartment().getInstitution());
        getIssuedBill().setCreater(getSessionController().getLoggedUser());
        getIssuedBill().setCreatedAt(Calendar.getInstance().getTime());
        getIssuedBill().setNetTotal(calculateBillNetTotal());
        getIssuedBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        getBillFacade().edit(getIssuedBill());
        billService.createBillFinancialDetailsForPharmacyDirectIssueBill(getIssuedBill(), getBillItems());
//        updateStockBillValues();
        notificationController.createNotification(issuedBill);

        Bill b = getBillFacade().find(getIssuedBill().getId());
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        issuedBill = b;
        printPreview = true;

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
                originalOrderItem.setIssuedPhamaceuticalItemQty(originalOrderItem.getIssuedPhamaceuticalItemQty() + billItemsInIssue.getQty());
                // Update remaining quantity to track what's left to issue
                Double remainingQty = originalOrderItem.getRemainingQty();
                double currentRemaining = (remainingQty != null) ? remainingQty : originalOrderItem.getQty();
                originalOrderItem.setRemainingQty(currentRemaining - billItemsInIssue.getQty());
                billItemFacade.editAndCommit(originalOrderItem);

                getBillItemFacade().edit(billItemsInIssue);
                getBillItemFacade().edit(originalOrderItem);
            } else {
                getBillItemFacade().edit(billItemsInIssue);
            }

            getPharmaceuticalBillItemFacade().edit(billItemsInIssue.getPharmaceuticalBillItem());

            getIssuedBill().getBillItems().add(billItemsInIssue);
        }

        getIssuedBill().getBillItems().forEach(this::updateBillItemRateAndValueAndSave);

        getIssuedBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));

        if (getSessionController().getApplicationPreference().isDepNumGenFromToDepartment()) {
            getIssuedBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getIssuedBill().getToDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        } else {
            getIssuedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        }

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

        getBillFacade().edit(getIssuedBill());
        billService.createBillFinancialDetailsForPharmacyBill(getIssuedBill());
        updateStockBillValues();

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

        b.setRate(rate);
        b.setNetRate(rate); // Fix: Set NETRATE
        b.setNetValue(rate * b.getQty()); // Use BillItem.qty for rate calculations
        b.setGrossValue(rate * b.getQty()); // Use BillItem.qty for rate calculations

        BigDecimal qty = BigDecimal.valueOf(b.getPharmaceuticalBillItem().getQty());
        BigDecimal rateBig = BigDecimal.valueOf(rate);
        BigDecimal total = rateBig.multiply(qty);

        f.setQuantity(qty);
        f.setTotalQuantity(qty);
        f.setLineGrossRate(rateBig);
        f.setLineNetRate(rateBig);
        f.setLineGrossTotal(total);
        f.setLineNetTotal(total);

        // Fix: Set LINECOSTRATE to actual cost rate from ItemBatch
        if (b.getPharmaceuticalBillItem() != null && b.getPharmaceuticalBillItem().getItemBatch() != null) {
            BigDecimal costRate = BigDecimal.valueOf(b.getPharmaceuticalBillItem().getItemBatch().getCostRate());
            f.setLineCostRate(costRate);
            f.setLineCost(costRate.multiply(qty));
        } else {
            f.setLineCost(total);
            f.setLineCostRate(rateBig);
        }

        // Fix: Add missing BillItemFinanceDetails fields
        f.setGrossRate(rateBig); // GROSSRATE

        // Calculate quantity by units (quantity * units per pack)
        BigDecimal unitsPerPack = f.getUnitsPerPack() != null ? f.getUnitsPerPack() : BigDecimal.ONE;
        f.setQuantityByUnits(qty.multiply(unitsPerPack)); // QUANTITYBYUNITS

        // Calculate value at different rates
        if (b.getPharmaceuticalBillItem() != null && b.getPharmaceuticalBillItem().getItemBatch() != null) {
            ItemBatch batch = b.getPharmaceuticalBillItem().getItemBatch();
            PharmaceuticalBillItem phItem = b.getPharmaceuticalBillItem();

            f.setValueAtCostRate(BigDecimal.valueOf(batch.getCostRate()).multiply(qty)); // VALUEATCOSTRATE
            f.setValueAtPurchaseRate(BigDecimal.valueOf(batch.getPurcahseRate()).multiply(qty)); // VALUEATPURCHASERATE
            f.setValueAtRetailRate(BigDecimal.valueOf(batch.getRetailsaleRate()).multiply(qty)); // VALUEATRETAILRATE

            // Fix: Add missing PharmaceuticalBillItem fields
            BigDecimal packQty = BigDecimal.valueOf(b.getQty()); // Quantity in packs

            phItem.setPurchaseRatePack(batch.getPurcahseRate() * unitsPerPack.doubleValue()); // PURCHASERATEPACK
            phItem.setRetailRatePack(batch.getRetailsaleRate() * unitsPerPack.doubleValue()); // RETAILRATEPACK
            phItem.setPurchaseValue(batch.getPurcahseRate() * phItem.getQty()); // PURCHASEVALUE (rate * qty in units)
            phItem.setRetailValue(batch.getRetailsaleRate() * phItem.getQty()); // RETAILVALUE (rate * qty in units)
        }

        getBillItemFacade().edit(b);
    }

    private void updateBillItemRateAndValueForDirectIssue(BillItem b) {
        BillItemFinanceDetails f = b.getBillItemFinanceDetails();
        double rate = b.getBillItemFinanceDetails().getLineGrossRate().doubleValue();

        b.setRate(rate);
        b.setNetRate(rate); // Fix: Set NETRATE
        b.setNetValue(rate * b.getQty()); // Use BillItem.qty for rate calculations
        b.setGrossValue(rate * b.getQty()); // Use BillItem.qty for rate calculations

        BigDecimal qty = BigDecimal.valueOf(b.getPharmaceuticalBillItem().getQty());
        BigDecimal rateBig = BigDecimal.valueOf(rate);
        BigDecimal total = rateBig.multiply(qty);

        f.setQuantity(qty);
        f.setTotalQuantity(qty);
        f.setLineGrossRate(rateBig);
        f.setLineNetRate(rateBig);
        f.setLineGrossTotal(total);
        f.setLineNetTotal(total);

        // Fix: Set LINECOSTRATE to actual cost rate from ItemBatch
        if (b.getPharmaceuticalBillItem() != null && b.getPharmaceuticalBillItem().getItemBatch() != null) {
            BigDecimal costRate = BigDecimal.valueOf(b.getPharmaceuticalBillItem().getItemBatch().getCostRate());
            f.setLineCostRate(costRate);
            f.setLineCost(costRate.multiply(qty));
        } else {
            f.setLineCost(total);
            f.setLineCostRate(rateBig);
        }

        // Fix: Add missing BillItemFinanceDetails fields
        f.setGrossRate(rateBig); // GROSSRATE

        // Calculate quantity by units (quantity * units per pack)
        BigDecimal unitsPerPack = f.getUnitsPerPack() != null ? f.getUnitsPerPack() : BigDecimal.ONE;
        f.setQuantityByUnits(qty.multiply(unitsPerPack)); // QUANTITYBYUNITS

        // Calculate value at different rates
        if (b.getPharmaceuticalBillItem() != null && b.getPharmaceuticalBillItem().getItemBatch() != null) {
            ItemBatch batch = b.getPharmaceuticalBillItem().getItemBatch();
            PharmaceuticalBillItem phItem = b.getPharmaceuticalBillItem();

            f.setValueAtCostRate(BigDecimal.valueOf(batch.getCostRate()).multiply(qty)); // VALUEATCOSTRATE
            f.setValueAtPurchaseRate(BigDecimal.valueOf(batch.getPurcahseRate()).multiply(qty)); // VALUEATPURCHASERATE
            f.setValueAtRetailRate(BigDecimal.valueOf(batch.getRetailsaleRate()).multiply(qty)); // VALUEATRETAILRATE

            // Fix: Add missing PharmaceuticalBillItem fields
            BigDecimal packQty = BigDecimal.valueOf(b.getQty()); // Quantity in packs

            phItem.setPurchaseRatePack(batch.getPurcahseRate() * unitsPerPack.doubleValue()); // PURCHASERATEPACK
            phItem.setRetailRatePack(batch.getRetailsaleRate() * unitsPerPack.doubleValue()); // RETAILRATEPACK
            phItem.setCostRatePack(batch.getCostRate() * unitsPerPack.doubleValue()); // COSTRATEPACK
            phItem.setPurchaseValue(batch.getPurcahseRate() * phItem.getQty()); // PURCHASEVALUE (rate * qty in units)
            phItem.setRetailValue(batch.getRetailsaleRate() * phItem.getQty()); // RETAILVALUE (rate * qty in units)
            phItem.setCostValue(batch.getCostRate() * phItem.getQty()); // COSTVALUE (rate * qty in units)
        }

        getBillItemFacade().edit(b);
    }

    private void updateBillItemRateAndValueAndSave(BillItem b) {
        updateBillItemRateAndValue(b);
        getBillItemFacade().edit(b);
    }

    private void updateBillItemRateAndValueAndSaveForDirectIssue(BillItem b) {
        updateBillItemRateAndValueForDirectIssue(b);
        getBillItemFacade().edit(b);
    }

    private double calculateBillNetTotal() {
        double value = 0;
        int serialNo = 0;
        for (BillItem b : getIssuedBill().getBillItems()) {
            double rate = b.getBillItemFinanceDetails().getLineGrossRate().doubleValue();
            value += rate * b.getPharmaceuticalBillItem().getQty();
            b.setSearialNo(serialNo++);
        }
        return value;
    }

    @Deprecated // Data Already stored in BillFinanceDetails
    private void updateStockBillValues() {
        double retailValue = 0.0;
        double purchaseValue = 0.0;
        double costValue = 0.0;

        for (BillItem bi : getIssuedBill().getBillItems()) {
            if (bi == null || bi.getPharmaceuticalBillItem() == null) {
                continue;
            }
            ItemBatch batch = bi.getPharmaceuticalBillItem().getItemBatch();
            if (batch == null) {
                continue;
            }
            double qty = bi.getPharmaceuticalBillItem().getQty();
            retailValue += batch.getRetailsaleRate() * qty;
            purchaseValue += batch.getPurcahseRate() * qty;
            costValue += batch.getCostRate() * qty;
        }

        StockBill sb = getIssuedBill().getStockBill();
        sb.setStockValueAsSaleRate(retailValue);
        sb.setStockValueAtPurchaseRates(purchaseValue);
        sb.setStockValueAsCostRate(costValue);
    }

    public void addNewBillItem() {
        billItem = new BillItem();
        if (getTmpStock() == null) {
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        if (getQty() == null) {
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }
        if (getQty() == 0.0) {
            JsfUtil.addErrorMessage("Quentity Zero?");
            return;
        }
        if (getQty() > getTmpStock().getStock()) {
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }
        if (!userStockController.isStockAvailable(getTmpStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }

        if (billItem.getBillItemFinanceDetails().getUnitsPerPack() == null || billItem.getBillItemFinanceDetails().getUnitsPerPack() == BigDecimal.ZERO) {
            if (billItem.getItem() instanceof Ampp) {
                billItem.getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.valueOf(billItem.getItem().getDblValue()));
            } else {
                billItem.getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.ONE);
            }
        }

        billItem.getPharmaceuticalBillItem().setQty(qty);
        billItem.getPharmaceuticalBillItem().setStock(getTmpStock());
        billItem.getPharmaceuticalBillItem().setItemBatch(getTmpStock().getItemBatch());

        billItem.getPharmaceuticalBillItem().setPurchaseRate(getTmpStock().getItemBatch().getPurcahseRate());
        billItem.getPharmaceuticalBillItem().setPurchaseRatePack(getTmpStock().getItemBatch().getPurcahseRate() * billItem.getBillItemFinanceDetails().getUnitsPerPack().doubleValue());

        billItem.getPharmaceuticalBillItem().setRetailRate(getTmpStock().getItemBatch().getRetailsaleRate());
        billItem.getPharmaceuticalBillItem().setRetailRatePack(getTmpStock().getItemBatch().getRetailsaleRate() * billItem.getBillItemFinanceDetails().getUnitsPerPack().doubleValue());

        billItem.getPharmaceuticalBillItem().setCostRate(getTmpStock().getItemBatch().getCostRate());
        billItem.getPharmaceuticalBillItem().setCostRatePack(getTmpStock().getItemBatch().getCostRate() * billItem.getBillItemFinanceDetails().getUnitsPerPack().doubleValue());

        billItem.getPharmaceuticalBillItem().setCostValue(billItem.getPharmaceuticalBillItem().getCostRate() * billItem.getPharmaceuticalBillItem().getQty());
        billItem.getPharmaceuticalBillItem().setRetailValue(billItem.getPharmaceuticalBillItem().getRetailRate() * billItem.getPharmaceuticalBillItem().getQty());
        billItem.getPharmaceuticalBillItem().setPurchaseValue(billItem.getPharmaceuticalBillItem().getPurchaseRate() * billItem.getPharmaceuticalBillItem().getQty());
        
        billItem.setItem(getTmpStock().getItemBatch().getItem());
        billItem.setQty(qty);

        billItem.getBillItemFinanceDetails().setLineGrossRate(determineTransferRate(billItem.getPharmaceuticalBillItem().getItemBatch()));

        billItem.getBillItemFinanceDetails().setQuantity(BigDecimal.valueOf(billItem.getQty()));
        billItem.getBillItemFinanceDetails().setQuantityByUnits(BigDecimal.valueOf(billItem.getQty()).multiply(billItem.getBillItemFinanceDetails().getUnitsPerPack()));

        billItem.getBillItemFinanceDetails().setLineGrossTotal(billItem.getBillItemFinanceDetails().getLineGrossRate().multiply(billItem.getBillItemFinanceDetails().getQuantity()));

        billItem.getBillItemFinanceDetails().setLineNetRate(billItem.getBillItemFinanceDetails().getLineGrossRate());
        billItem.getBillItemFinanceDetails().setGrossRate(billItem.getBillItemFinanceDetails().getLineGrossRate());
        billItem.getBillItemFinanceDetails().setLineNetTotal(billItem.getBillItemFinanceDetails().getLineGrossTotal());
        billItem.getBillItemFinanceDetails().setNetTotal(billItem.getBillItemFinanceDetails().getLineGrossTotal());

        billItem.getBillItemFinanceDetails().setTotalQuantity(BigDecimal.valueOf(billItem.getQty()));

        billItem.setRate(billItem.getBillItemFinanceDetails().getLineGrossRate().doubleValue());
        billItem.setNetRate(billItem.getBillItemFinanceDetails().getLineGrossRate().doubleValue());
        billItem.setNetValue(billItem.getBillItemFinanceDetails().getLineGrossTotal().doubleValue());
        billItem.setGrossValue(billItem.getBillItemFinanceDetails().getLineGrossTotal().doubleValue());

        BigDecimal costRate = BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getItemBatch().getCostRate());
        BigDecimal retailRate = BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate()).multiply(billItem.getBillItemFinanceDetails().getUnitsPerPack());
        BigDecimal purchaseRate = BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate()).multiply(billItem.getBillItemFinanceDetails().getUnitsPerPack());

        billItem.getBillItemFinanceDetails().setLineCostRate(costRate);
        billItem.getBillItemFinanceDetails().setLineCost(costRate.multiply(billItem.getBillItemFinanceDetails().getQuantity()));
        billItem.getBillItemFinanceDetails().setTotalCost(costRate.multiply(billItem.getBillItemFinanceDetails().getQuantity()));
        billItem.getBillItemFinanceDetails().setTotalCostRate(costRate);

        billItem.getBillItemFinanceDetails().setRetailSaleRate(BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate()));

        billItem.getBillItemFinanceDetails().setValueAtCostRate(costRate.multiply(billItem.getBillItemFinanceDetails().getQuantity()));
        billItem.getBillItemFinanceDetails().setValueAtRetailRate(retailRate.multiply(billItem.getBillItemFinanceDetails().getQuantity()));
        billItem.getBillItemFinanceDetails().setValueAtPurchaseRate(purchaseRate.multiply(billItem.getBillItemFinanceDetails().getQuantity()));

        billItem.setSearialNo(getBillItems().size() + 1);
        getBillItems().add(billItem);

        qty = null;
        tmpStock = null;

        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getIssuedBill(), getBillItems());
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
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getIssuedBill(), getBillItems());
    }

    public void onLineGrossRateChangeForTransferIssue(BillItem bi) {
        if (bi == null) {
            return;
        }
        updateFinancialsForTransferIssue(bi.getBillItemFinanceDetails());
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getIssuedBill(), getBillItems());
    }

    public void onEditDepartmentTransfer(BillItem billItem) {
        double availableStock = pharmacyBean.getStockQty(billItem.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());

        if (availableStock < billItem.getPharmaceuticalBillItem().getQtyInUnit()) {
            billItem.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(billItem.getPharmaceuticalBillItem().getStock(), billItem.getQty(), getSessionController().getLoggedUser())) {
            billItem.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        userStockController.updateUserStock(billItem.getTransUserStock(), billItem.getQty());

    }

    public void onFocus(BillItem tmp) {
        getPharmacyController().fillItemDetails(tmp.getItem());
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
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getIssuedBill(), getBillItems());
        
        JsfUtil.addSuccessMessage("Stock replaced successfully.");
    }

    public void removeAll() {
        if (billItems == null) {
            return;
        }

        for (BillItem b : billItems) {
            getBillItems().remove(b);
        }

        billItems = null;
    }

    private void saveBill() {
        getIssuedBill().setReferenceBill(getRequestedBill());
        if (getIssuedBill().getId() == null) {
            getBillFacade().create(getIssuedBill());
        }
    }

    public Bill getIssuedBill() {
        if (issuedBill == null) {
            issuedBill = new BilledBill();
            issuedBill.setBillType(BillType.PharmacyTransferIssue);
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
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

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Stock getTmpStock() {
        return tmpStock;
    }

    public void setTmpStock(Stock tmpStock) {
        this.tmpStock = tmpStock;
    }

    public BillItem getSelectedBillItem() {
        return selectedBillItem;
    }

    public void setSelectedBillItem(BillItem selectedBillItem) {
        this.selectedBillItem = selectedBillItem;
    }

    public boolean isShowAllBillFormats() {
        return showAllBillFormats;
    }

    public void setShowAllBillFormats(boolean showAllBillFormats) {
        this.showAllBillFormats = showAllBillFormats;
    }

    public String toggleShowAllBillFormats() {
        this.showAllBillFormats = !this.showAllBillFormats;
        return "";
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

    public String getStockColourClass(Stock stock) {
        if (!configOptionApplicationController.getBooleanValueByKey("Display Colours for Stock Autocomplete Items", true)) {
            return "";
        }

        if (stock == null || stock.getItemBatch() == null || stock.getItemBatch().getDateOfExpire() == null) {
            return "";
        }

        Date expiry = stock.getItemBatch().getDateOfExpire();
        Date now = commonFunctionsProxy.getCurrentDateTime();

        if (now.after(expiry)) {
            return "ui-messages-fatal";
        }

        if (commonFunctionsProxy.getDateAfterThreeMonthsCurrentDateTime().after(expiry)) {
            return "ui-messages-warn";
        }

        return "";
    }

    /**
     * Gets the remaining quantity that can still be issued for a given requested item.
     * @param referenceItem The original requested bill item
     * @return The remaining quantity that can be issued
     */
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
     * Creates a complete BillItem from stock DTO data, avoiding entity traversals.
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

        // Quantities
        financeDetails.setQuantity(qty);
        financeDetails.setTotalQuantity(qty);

        // Transfer rates and values (net values - what the transfer is priced at)
        financeDetails.setLineGrossRate(transferRate.multiply(packSizeBD));
        financeDetails.setLineNetRate(transferRate.multiply(packSizeBD));
        financeDetails.setLineGrossTotal(transferRate.multiply(qty).multiply(packSizeBD));
        financeDetails.setLineNetTotal(transferRate.multiply(qty).multiply(packSizeBD));
        financeDetails.setNetTotal(transferRate.multiply(qty).multiply(packSizeBD));

        // Purchase rate and value (for purchase value reporting)
        financeDetails.setValueAtPurchaseRate(purchaseRate.multiply(qty).multiply(packSizeBD));

        // Retail rate and value (for sale value reporting)
        financeDetails.setRetailSaleRate(retailRate.multiply(packSizeBD));
        financeDetails.setValueAtRetailRate(retailRate.multiply(qty).multiply(packSizeBD));

        // Cost rate and value
        financeDetails.setLineCostRate(costRate);
        financeDetails.setLineCost(costRate.multiply(qty).multiply(packSizeBD));
        financeDetails.setTotalCost(costRate.multiply(qty).multiply(packSizeBD));
        financeDetails.setTotalCostRate(costRate);
        financeDetails.setValueAtCostRate(costRate.multiply(qty).multiply(packSizeBD));
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
