/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.StockQty;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.UserStock;
import com.divudi.entity.pharmacy.UserStockContainer;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.java.CommonFunctions;
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
    CommonController commonController;
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

    private CommonFunctions commonFunctions;
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
        return "/pharmacy/pharmacy_transfer_issue";
    }

    public String navigateToPharmacyDirectIssueForRequests() {
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        createRequestIssueBillItems(requestedBill);
        return "/pharmacy/pharmacy_transfer_issue_direct_department";
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

        return "/pharmacy/pharmacy_transfer_issue_direct_department";
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
        if (billItem.getTransUserStock().isRetired()) {
            JsfUtil.addErrorMessage("This Item Already removed");
            return;
        }

        userStockController.removeUserStock(billItem.getTransUserStock(), getSessionController().getLoggedUser());

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

            List<StockQty> stockQtys = pharmacyBean.getStockByQty(i.getItem(), issuableQty, getSessionController().getDepartment());

            for (StockQty sq : stockQtys) {

                if (sq.getQty() == 0) {
                    continue;
                }

                //Checking User Stock Entity
                if (!userStockController.isStockAvailable(sq.getStock(), sq.getQty(), getSessionController().getLoggedUser())) {
                    JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
                    continue;
                }

                BillItem bItem = new BillItem();
                bItem.setSearialNo(getBillItems().size());
                bItem.setItem(i.getItem());
                bItem.setReferanceBillItem(i);

                bItem.setTmpQty(sq.getQty());

//               s bItem.setTmpSuggession(getSuggession(i.getBillItem().getItem()));
                //     //System.err.println("List "+bItem.getTmpSuggession());
                PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
                phItem.setBillItem(bItem);
                phItem.setQtyInUnit((double) sq.getQty());
                phItem.setPurchaseRateInUnit((double) sq.getStock().getItemBatch().getPurcahseRate());
                phItem.setRetailRateInUnit((double) sq.getStock().getItemBatch().getRetailsaleRate());
                phItem.setStock(sq.getStock());
                phItem.setDoe(sq.getStock().getItemBatch().getDateOfExpire());
                phItem.setItemBatch(sq.getStock().getItemBatch());
                phItem.setItemBatch(sq.getStock().getItemBatch());
                phItem.setQty(sq.getQty());
                bItem.setPharmaceuticalBillItem(phItem);

                //USER STOCK
                UserStock us = userStockController.saveUserStock(bItem, getSessionController().getLoggedUser(), usc);
                bItem.setTransUserStock(us);

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

        List<BillItem> bis = billController.billItemsOfBill(grn);
        getIssuedBill().setDepartment(null);
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        getIssuedBill().setToDepartment(null);

        for (BillItem oi : bis) {

            System.out.println("oi = " + oi);

            BillItem ni = new BillItem();

            if (oi.getPharmaceuticalBillItem() == null) {
                continue;
            }

            System.out.println("oi.getPharmaceuticalBillItem() = " + oi.getPharmaceuticalBillItem());

            PharmaceuticalBillItem npi = new PharmaceuticalBillItem();
            npi.setItemBatch(oi.getPharmaceuticalBillItem().getItemBatch());
            npi.setItemBatch(oi.getPharmaceuticalBillItem().getItemBatch());
            npi.setStockHistory(oi.getPharmaceuticalBillItem().getStockHistory());
            npi.setDoe(oi.getPharmaceuticalBillItem().getDoe());
            npi.setStringValue(oi.getPharmaceuticalBillItem().getStringValue());
            System.out.println("oi.getPharmaceuticalBillItem().getQty() = " + oi.getPharmaceuticalBillItem().getQty());
            if (oi.getPharmaceuticalBillItem().getQty() != 0.0 && oi.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {
                npi.setQty(oi.getPharmaceuticalBillItem().getQty());
                npi.setQtyInUnit(oi.getPharmaceuticalBillItem().getQtyInUnit());
            } else if (oi.getPharmaceuticalBillItem().getQty() != 0.0) {
                npi.setQty(oi.getPharmaceuticalBillItem().getQty());
                npi.setQtyInUnit(oi.getPharmaceuticalBillItem().getQty());
            } else if (oi.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {
                npi.setQty(oi.getPharmaceuticalBillItem().getQtyInUnit());
                npi.setQtyInUnit(oi.getPharmaceuticalBillItem().getQtyInUnit());
            }

            if (oi.getPharmaceuticalBillItem().getFreeQty() != 0.0 && oi.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {
                npi.setFreeQty(oi.getPharmaceuticalBillItem().getFreeQty());
                npi.setFreeQtyInUnit(oi.getPharmaceuticalBillItem().getFreeQtyInUnit());
            } else if (oi.getPharmaceuticalBillItem().getFreeQty() != 0.0) {
                npi.setFreeQty(oi.getPharmaceuticalBillItem().getFreeQty());
                npi.setFreeQtyInUnit(oi.getPharmaceuticalBillItem().getFreeQty());
            } else if (oi.getPharmaceuticalBillItem().getQtyInUnit() != 0.0) {
                npi.setFreeQty(oi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                npi.setFreeQtyInUnit(oi.getPharmaceuticalBillItem().getFreeQtyInUnit());
            }

            npi.setPurchaseRate(oi.getPharmaceuticalBillItem().getPurchaseRate());
            npi.setLastPurchaseRate(oi.getPharmaceuticalBillItem().getLastPurchaseRate());
            npi.setRetailRate(oi.getPharmaceuticalBillItem().getRetailRate());
            npi.setWholesaleRate(oi.getPharmaceuticalBillItem().getWholesaleRate());
            npi.setStock(oi.getPharmaceuticalBillItem().getStock());
            npi.setStaffStock(oi.getPharmaceuticalBillItem().getStaffStock());
            npi.setBillItem(ni);

            ni.setItem(oi.getItem());
            ni.setItemId(oi.getItemId());
            ni.setNetRate(oi.getNetRate());

            ni.setQty(oi.getQty());

            ni.setGrossValue(oi.getGrossValue());
            ni.setNetValue(oi.getNetValue());

            ni.setPharmaceuticalBillItem(npi);

            ni.setSearialNo(getBillItems().size());
            ni.setItem(oi.getItem());
            ni.setReferanceBillItem(oi);
//            ni.setTmpQty(0);
            getBillItems().add(ni);

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

        if (getIssuedBill().getId() == null) {
            getBillFacade().create(getIssuedBill());
        } else {
            getBillFacade().edit(getIssuedBill());
        }
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

        getIssuedBill().setNetTotal(calTotal());

        getIssuedBill().setBackwardReferenceBill(getRequestedBill());

        getBillFacade().edit(getIssuedBill());

        //Update ReferenceBill
        //     getRequestedBill().setReferenceBill(getIssuedBill());
        getRequestedBill().getForwardReferenceBills().add(getIssuedBill());
        getBillFacade().edit(getRequestedBill());

        Bill b = getBillFacade().find(getIssuedBill().getId());
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        issuedBill = null;
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

        getIssuedBill().setNetTotal(calTotal());

        getIssuedBill().setBackwardReferenceBill(getRequestedBill());

        getBillFacade().edit(getIssuedBill());

        //Update ReferenceBill
        //     getRequestedBill().setReferenceBill(getIssuedBill());
        getRequestedBill().getForwardReferenceBills().add(getIssuedBill());
        getBillFacade().edit(getRequestedBill());

        Bill b = getBillFacade().find(getIssuedBill().getId());
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        issuedBill = null;
        issuedBill = b;

        printPreview = true;

    }

    private double calTotal() {
        double value = 0;
        int serialNo = 0;

        if (sessionController.getApplicationPreference().isTranferNetTotalbyRetailRate()) {
            for (BillItem b : getIssuedBill().getBillItems()) {
                value += (b.getPharmaceuticalBillItem().getRetailRate() * b.getPharmaceuticalBillItem().getQty());
                b.setSearialNo(serialNo++);
            }
        } else {
            for (BillItem b : getIssuedBill().getBillItems()) {
                value += (b.getPharmaceuticalBillItem().getPurchaseRate() * b.getPharmaceuticalBillItem().getQty());
                b.setSearialNo(serialNo++);
            }

        }

        return value;

    }

    public void addBillItemNew() {

        billItem = new BillItem();
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getTmpStock() == null) {
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        if (getTmpStock().getItemBatch().getDateOfExpire().before(commonController.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("Please not select Expired Items");
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

//        if (checkItemBatch()) {
//            errorMessage = "This batch is already there in the bill.";
//            UtilityController.addErrorMessage("Already added this item batch");
//            return;
//        }
//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            UtilityController.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getTmpStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (qty));
        billItem.getPharmaceuticalBillItem().setStock(getTmpStock());
        billItem.getPharmaceuticalBillItem().setItemBatch(getTmpStock().getItemBatch());

//        calculateBillItem();
        ////System.out.println("Rate*****" + billItem.getRate());
//        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getTmpStock().getItemBatch().getItem());
        billItem.setQty(qty);
//        billItem.setBill(getPreBill());

        billItem.setSearialNo(getBillItems().size() + 1);
        getBillItems().add(billItem);

        qty = null;
        tmpStock = null;
    }

    @Inject
    private PharmacyController pharmacyController;
    
    public void onEditDepartmentTransfer(BillItem billItem) {
        System.out.println("billItem = " + billItem);
        double availableStock = pharmacyBean.getStockQty(billItem.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());

        System.out.println("availableStock = " + availableStock);

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

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        double availableStock = pharmacyBean.getStockQty(tmp.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());

        if (availableStock < tmp.getPharmaceuticalBillItem().getQtyInUnit()) {
            tmp.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {
            tmp.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

    }

    public void onFocus(BillItem tmp) {
        getPharmacyController().setPharmacyItem(tmp.getItem());
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
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
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
