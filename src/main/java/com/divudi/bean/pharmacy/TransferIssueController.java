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
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.service.BillService;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.service.pharmacy.PharmacyCostingService;
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
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class TransferIssueController implements Serializable {

    private Bill requestedBill;
    private Bill issuedBill;
    private boolean printPreview;
    private Date fromDate;
    private Date toDate;
    ///////
    @Inject
    UserStockController userStockController;
    @Inject
    private SessionController sessionController;
    @Inject
    BillController billController;
    @Inject
    NotificationController notificationController;
    ////
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    ////
    @EJB
    private PharmacyBean pharmacyBean;
    @Inject
    private PharmacyCalculation pharmacyCalculation;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @EJB
    private BillService billService;
    @EJB
    PharmacyCostingService pharmacyCostingService;

    private List<BillItem> billItems;
    private BillItem billItem;
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
//                System.out.println(originalItem.getIssuedPhamaceuticalItemQty() + " originalItem.getIssuedPhamaceuticalItemQty " + originalItem.getQty() + " originalItem.getQty()");
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

        for (BillItem originalItem : billItems) {

            if (originalItem.getPharmaceuticalBillItem().getQty() > 0) {
                return false;
            } else if (originalItem.getPharmaceuticalBillItem().getItemBatch() == null) {
                return false;
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

        //User Stock Container Save if New Bill
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        List<BillItem> bis = billController.billItemsOfBill(getRequestedBill());
        getIssuedBill().setDepartment(requestedBill.getDepartment());
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        getIssuedBill().setToDepartment(requestedBill.getDepartment());

        for (BillItem i : bis) {

            boolean flagStockFound = false;

            double billedIssue = getPharmacyCalculation().getBilledIssuedByRequestedItem(i, BillType.PharmacyTransferIssue);
            double cancelledIssue = getPharmacyCalculation().getCancelledIssuedByRequestedItem(i, BillType.PharmacyTransferIssue);

            double issuableQty = i.getQty() - (Math.abs(billedIssue) - Math.abs(cancelledIssue));

            //i.setIssuedPhamaceuticalItemQty(i.getQty() - issuableQty);
            List<StockQty> stockQtys = pharmacyBean.getStockByQty(i.getItem(), issuableQty, getSessionController().getDepartment());

            for (StockQty sq : stockQtys) {

//                if (sq.getQty() == 0) {
//                    continue;
//                }
                //Checking User Stock Entity
                if (!userStockController.isStockAvailable(sq.getStock(), sq.getQty(), getSessionController().getLoggedUser())) {
                    JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
                    continue;
                }

                BillItem bItem = new BillItem();
                bItem.setSearialNo(getBillItems().size());
                bItem.setItem(i.getItem());
                bItem.setReferanceBillItem(i);
                bItem.setQty(i.getQty());
                bItem.setTmpQty(sq.getQty());
                bItem.setIssuedPhamaceuticalItemQty(i.getQty() - sq.getQty());

                if (stockQtys.size() > 1) {
                    double billedIssueByItemBatch = getPharmacyCalculation().getBilledIssuedByRequestedItemBatch(i, BillType.PharmacyTransferIssue, sq.getStock().getItemBatch());
                    double cancelledIssueByItemBatch = getPharmacyCalculation().getCancelledIssuedByRequestedItemBatch(i, BillType.PharmacyTransferIssue, sq.getStock().getItemBatch());
                    bItem.setIssuedPhamaceuticalItemQty(Math.abs(billedIssueByItemBatch) + Math.abs(cancelledIssueByItemBatch));
                }

//               s bItem.setTmpSuggession(getSuggession(i.getBillItem().getItem()));
                //     //System.err.println("List "+bItem.getTmpSuggession());
                PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
                phItem.setBillItem(bItem);
                phItem.setQtyInUnit(sq.getQty());
                phItem.setPurchaseRateInUnit(sq.getStock().getItemBatch().getPurcahseRate());
                phItem.setRetailRateInUnit(sq.getStock().getItemBatch().getRetailsaleRate());
                phItem.setStock(sq.getStock());
                phItem.setDoe(sq.getStock().getItemBatch().getDateOfExpire());
                phItem.setItemBatch(sq.getStock().getItemBatch());
                phItem.setItemBatch(sq.getStock().getItemBatch());
                phItem.setQty(sq.getQty());
                bItem.setPharmaceuticalBillItem(phItem);

                //USER STOCK
                UserStock us = userStockController.saveUserStock(bItem, getSessionController().getLoggedUser(), usc);
                bItem.setTransUserStock(us);

//                double totalIssueAmount = 0;
//
//                if (stockQtys.size() > 1) {
//                    for (StockQty s : stockQtys) {
//                        totalIssueAmount += s.getQty();
//                    }
//                    bItem.setIssuedPhamaceuticalItemQty(totalIssueAmount);
//
//                }
                getBillItems().add(bItem);
                flagStockFound = true;

            }

            if (!flagStockFound) {
                BillItem bItem = new BillItem();
                bItem.setSearialNo(getBillItems().size());
                bItem.setItem(i.getItem());
                bItem.setReferanceBillItem(i);
                bItem.setTmpQty(0);
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
        if (getIssuedBill().getToStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return;
        }
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items");
            return;
        }
        boolean pharmacyTransferIsByPurchaseRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean pharmacyTransferIsByCostRate    = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate",    false);
        boolean pharmacyTransferIsByRetailRate  = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate",  true);

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
        for (BillItem i : getBillItems()) {

            i.getPharmaceuticalBillItem().setQty(0 - i.getPharmaceuticalBillItem().getQty());

            if (i.getQty() == 0.0 || i.getItem() instanceof Vmpp || i.getItem() instanceof Vmp) {
                continue;
            }

            i.setBill(getIssuedBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setPharmaceuticalBillItem(i.getPharmaceuticalBillItem());

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            //Checking User Stock Entity
            if (!userStockController.isStockAvailable(tmpPh.getStock(), tmpPh.getQty(), getSessionController().getLoggedUser())) {
                i.setQty(0.0);
                getBillItemFacade().edit(i);
                getIssuedBill().getBillItems().add(i);
                continue;
            }

//            System.out.println("//Remove Department Stock = ");
            //Remove Department Stock
            boolean returnFlag = pharmacyBean.deductFromStock(i.getPharmaceuticalBillItem().getStock(),
                    Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()),
                    i.getPharmaceuticalBillItem(),
                    getSessionController().getDepartment());
//            System.out.println("returnFlag = " + returnFlag);
            if (returnFlag) {

//Addinng Staff
//                System.out.println("//Addinng Staff = ");
                                Stock staffStock = pharmacyBean.addToStock(i.getPharmaceuticalBillItem(),
                        Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), getIssuedBill().getToStaff());

                i.getPharmaceuticalBillItem().setStaffStock(staffStock);

            } else {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
            }

            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());

            getIssuedBill().getBillItems().add(i);
        }

        getIssuedBill().getBillItems().forEach(this::updateBillItemRateAndValueAndSave);

        getIssuedBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));

        if (getSessionController().getApplicationPreference().isDepNumGenFromToDepartment()) {
            getIssuedBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getIssuedBill().getToDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        } else {
            getIssuedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        }

//        getIssuedBill().setInstitution(getSessionController().getInstitution());
        getIssuedBill().setDepartment(getIssuedBill().getFromDepartment());
//
        getIssuedBill().setToInstitution(getIssuedBill().getToDepartment().getInstitution());

        getIssuedBill().setCreater(getSessionController().getLoggedUser());
        getIssuedBill().setCreatedAt(Calendar.getInstance().getTime());

        getIssuedBill().setNetTotal(calculateBillNetTotal());

//        getIssuedBill().setBackwardReferenceBill(getRequestedBill());
        getIssuedBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        getBillFacade().edit(getIssuedBill());
        billService.createBillFinancialDetailsForPharmacyBill(getIssuedBill());
        notificationController.createNotification(issuedBill);

        //Update ReferenceBill
        //     getRequestedBill().setReferenceBill(getIssuedBill());
//        getRequestedBill().getForwardReferenceBills().add(getIssuedBill());
//        getBillFacade().edit(getRequestedBill());
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

            if (bi.getReferanceBillItem().getQty() < (bi.getPharmaceuticalBillItem().getQty() + bi.getIssuedPhamaceuticalItemQty())) {
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
        for (BillItem i : getBillItems()) {

            i.getPharmaceuticalBillItem().setQtyInUnit(0 - i.getPharmaceuticalBillItem().getQtyInUnit());

            if (i.getQty() == 0.0 || i.getItem() instanceof Vmpp || i.getItem() instanceof Vmp) {
                continue;
            }

            i.setBill(getIssuedBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setPharmaceuticalBillItem(i.getPharmaceuticalBillItem());

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            //Checking User Stock Entity
            if (!userStockController.isStockAvailable(tmpPh.getStock(), tmpPh.getQtyInUnit(), getSessionController().getLoggedUser())) {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
                getIssuedBill().getBillItems().add(i);
                continue;
            }

            //Remove Department Stock
            boolean returnFlag = pharmacyBean.deductFromStock(i.getPharmaceuticalBillItem().getStock(),
                    Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()),
                    i.getPharmaceuticalBillItem(),
                    getSessionController().getDepartment());
            if (returnFlag) {

                //Addinng Staff
                Stock staffStock = pharmacyBean.addToStock(i.getPharmaceuticalBillItem(),
                        Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), getIssuedBill().getToStaff());

                i.getPharmaceuticalBillItem().setStaffStock(staffStock);

            } else {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
            }

            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());

            getIssuedBill().getBillItems().add(i);
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
        b.setQty(b.getPharmaceuticalBillItem().getQty());
        b.setNetValue(rate * b.getPharmaceuticalBillItem().getQty());

        BigDecimal qty = BigDecimal.valueOf(b.getPharmaceuticalBillItem().getQty());
        BigDecimal rateBig = BigDecimal.valueOf(rate);
        BigDecimal total = rateBig.multiply(qty);

        f.setQuantity(qty);
        f.setTotalQuantity(qty);
        f.setLineGrossRate(rateBig);
        f.setLineNetRate(rateBig);
        f.setLineGrossTotal(total);
        f.setLineNetTotal(total);
        f.setLineCost(total);
        f.setLineCostRate(rateBig);

        getBillItemFacade().edit(b);
    }

    private void updateBillItemRateAndValueAndSave(BillItem b) {
        updateBillItemRateAndValue(b);
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
        billItem.getPharmaceuticalBillItem().setQty(qty);
        billItem.getPharmaceuticalBillItem().setStock(getTmpStock());
        billItem.getPharmaceuticalBillItem().setItemBatch(getTmpStock().getItemBatch());

        billItem.setItem(getTmpStock().getItemBatch().getItem());
        billItem.setQty(qty);

        billItem.getBillItemFinanceDetails().setLineGrossRate(determineTransferRate(billItem.getPharmaceuticalBillItem().getItemBatch()));
        
        billItem.getBillItemFinanceDetails().setQuantity(BigDecimal.valueOf(billItem.getQty()));

        billItem.getBillItemFinanceDetails().setLineGrossTotal(billItem.getBillItemFinanceDetails().getLineGrossRate().multiply(billItem.getBillItemFinanceDetails().getQuantity()));

        billItem.getBillItemFinanceDetails().setLineNetRate(billItem.getBillItemFinanceDetails().getLineGrossRate());
        billItem.getBillItemFinanceDetails().setLineNetTotal(billItem.getBillItemFinanceDetails().getLineGrossTotal());
        billItem.getBillItemFinanceDetails().setNetTotal(billItem.getBillItemFinanceDetails().getLineGrossTotal());

        billItem.getBillItemFinanceDetails().setTotalQuantity(BigDecimal.valueOf(billItem.getQty()));

        billItem.getBillItemFinanceDetails().setLineCostRate(BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getItemBatch().getCostRate()));
        billItem.getBillItemFinanceDetails().setLineCost(billItem.getBillItemFinanceDetails().getLineCostRate().multiply(billItem.getBillItemFinanceDetails().getQuantity()));
        billItem.getBillItemFinanceDetails().setTotalCost(billItem.getBillItemFinanceDetails().getLineCostRate().multiply(billItem.getBillItemFinanceDetails().getQuantity()));

        billItem.getBillItemFinanceDetails().setRetailSaleRate(BigDecimal.valueOf(billItem.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate()));

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

    @Inject
    private PharmacyController pharmacyController;

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
//        getIssuedBill().setToInstitution(getRequestedBill().getInstitution());
//        getIssuedBill().setToDepartment(getRequestedBill().getDepartment());

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

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
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

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
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

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
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

}
