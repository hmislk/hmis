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

    private Bill requestedBill;
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
    public boolean isFullyIssued(Bill bill) {
        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            return false; // Null or empty bills are not considered fully issued
        }

//        TODO: Create a Logic. Old one is NOT working
//        for (BillItem originalItem : billItems) {
//
//            if (originalItem.getPharmaceuticalBillItem().getQty() > 0) {
//                return false;
//            } else if (originalItem.getPharmaceuticalBillItem().getItemBatch() == null) {
//                return false;
//            }
//        }
        return false; // All items are fully issued
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

    public void generateBillComponent() {
        // Save the user stock container if this is a new bill
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        List<BillItem> billItemsOfRequest = billController.billItemsOfBill(getRequestedBill());

        // Setup department details
        getIssuedBill().setDepartment(requestedBill.getDepartment());
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        getIssuedBill().setToDepartment(requestedBill.getDepartment());

        for (BillItem billItemInRequest : billItemsOfRequest) {
            boolean flagStockFound = false;

            // Calculate pending quantity to issue
            double requestedQty = getPharmacyCalculation().getBilledIssuedByRequestedItem(billItemInRequest, BillType.PharmacyTransferIssue);
            double cancelledIssued = getPharmacyCalculation().getCancelledIssuedByRequestedItem(billItemInRequest, BillType.PharmacyTransferIssue);

            double alreadyIssuedQty = Math.abs(requestedQty) - Math.abs(cancelledIssued);
            billItemInRequest.setIssuedPhamaceuticalItemQty(alreadyIssuedQty);
            double quantityToIssue = billItemInRequest.getQty() - alreadyIssuedQty;

            Item stockItem = billItemInRequest.getItem();
            double packSize = 1.0;

            if (stockItem instanceof Ampp) {
                Ampp ampp = (Ampp) stockItem;
                stockItem = ampp.getAmp();
                packSize = stockItem.getDblValue();

            }
            if (stockItem instanceof Vmpp) {
                packSize = stockItem.getDblValue();
                //TODO: Add Suppoer to VMPP as a new issue
                continue;
            }

            Double quantityToIssueInUnits = quantityToIssue * packSize;

            List<Stock> availableStocks = pharmacyBean.getStockByQty(stockItem, getSessionController().getDepartment());

            if (availableStocks == null || availableStocks.isEmpty()) {
            } else {
            }

            Double totalIssuedQtyInUnits = 0.0;

            for (Stock issuingStock : availableStocks) {
                if (totalIssuedQtyInUnits >= quantityToIssueInUnits) {
                    break;
                }

                Double thisTimeIssuingQtyInUnits = issuingStock.getStock();

                String batchNo = issuingStock.getItemBatch().getBatchNo();
                Date doe = issuingStock.getItemBatch().getDateOfExpire();

                if (totalIssuedQtyInUnits + thisTimeIssuingQtyInUnits > quantityToIssueInUnits) {
                    thisTimeIssuingQtyInUnits = quantityToIssueInUnits - totalIssuedQtyInUnits;
                }

                if (thisTimeIssuingQtyInUnits <= 0) {
                    break;
                }

                if (!userStockController.isStockAvailable(issuingStock, thisTimeIssuingQtyInUnits, getSessionController().getLoggedUser())) {
                    continue;
                }

                totalIssuedQtyInUnits += thisTimeIssuingQtyInUnits;

                BillItem newlyCreatedBillItemInIssueBill = new BillItem();
                newlyCreatedBillItemInIssueBill.setSearialNo(getBillItems().size());
                newlyCreatedBillItemInIssueBill.setItem(billItemInRequest.getItem());
                newlyCreatedBillItemInIssueBill.setReferanceBillItem(billItemInRequest);
                newlyCreatedBillItemInIssueBill.setQty(thisTimeIssuingQtyInUnits / packSize);

                PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
                phItem.setBillItem(newlyCreatedBillItemInIssueBill);
                phItem.setQty(thisTimeIssuingQtyInUnits);
                phItem.setPurchaseRate(issuingStock.getItemBatch().getPurcahseRate());
                phItem.setRetailRateInUnit(issuingStock.getItemBatch().getRetailsaleRate());
                phItem.setStock(issuingStock);
                phItem.setDoe(issuingStock.getItemBatch().getDateOfExpire());
                phItem.setItemBatch(issuingStock.getItemBatch());

                if (packSize != 1.0) {
                    phItem.setQtyPacks(thisTimeIssuingQtyInUnits / packSize);
                }

                newlyCreatedBillItemInIssueBill.setPharmaceuticalBillItem(phItem);

                // Financials - Store all rates and values during bill creation
                BigDecimal qty = BigDecimal.valueOf(phItem.getQty() / packSize);
                BigDecimal transferRate = determineTransferRate(phItem.getItemBatch());
                BigDecimal purchaseRate = BigDecimal.valueOf(phItem.getItemBatch().getPurcahseRate());
                BigDecimal retailRate = BigDecimal.valueOf(phItem.getItemBatch().getRetailsaleRate());
                BigDecimal costRate = BigDecimal.valueOf(phItem.getItemBatch().getCostRate());
                BigDecimal packSizeBD = BigDecimal.valueOf(packSize);

                // Quantities
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setQuantity(qty);
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setTotalQuantity(qty);

                // Transfer rates and values (net values - what the transfer is priced at)
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setLineGrossRate(transferRate.multiply(packSizeBD));
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setLineNetRate(transferRate.multiply(packSizeBD));
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setLineGrossTotal(transferRate.multiply(qty).multiply(packSizeBD));
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setLineNetTotal(transferRate.multiply(qty).multiply(packSizeBD));
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setNetTotal(transferRate.multiply(qty).multiply(packSizeBD));

                // Purchase rate and value (for purchase value reporting)
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setValueAtPurchaseRate(purchaseRate.multiply(qty).multiply(packSizeBD));

                // Retail rate and value (for sale value reporting)
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setRetailSaleRate(retailRate.multiply(packSizeBD));
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setValueAtRetailRate(retailRate.multiply(qty).multiply(packSizeBD));

                // Cost rate and value
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setLineCostRate(costRate);
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setLineCost(costRate.multiply(qty).multiply(packSizeBD));
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setTotalCost(costRate.multiply(qty).multiply(packSizeBD));
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setTotalCostRate(costRate);
                newlyCreatedBillItemInIssueBill.getBillItemFinanceDetails().setValueAtCostRate(costRate.multiply(qty).multiply(packSizeBD));

                // Link stock issuance
                UserStock us = userStockController.saveUserStock(newlyCreatedBillItemInIssueBill, getSessionController().getLoggedUser(), usc);
                newlyCreatedBillItemInIssueBill.setTransUserStock(us);

                getBillItems().add(newlyCreatedBillItemInIssueBill);
                flagStockFound = true;

            }

            if (!flagStockFound) {
                BillItem bItem = new BillItem();
                bItem.setSearialNo(getBillItems().size());
                bItem.setItem(billItemInRequest.getItem());
                bItem.setReferanceBillItem(billItemInRequest);
                getBillItems().add(bItem);

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

            double alreadyIssued = bi.getReferanceBillItem().getIssuedPhamaceuticalItemQty();
            if (bi.getReferanceBillItem().getQty() < (bi.getQty() + alreadyIssued)) {
                JsfUtil.addErrorMessage("Issued quantity is higher than requested quantity in " + bi.getItem().getName());
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

}
