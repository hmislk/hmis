/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyController;
import com.divudi.bean.pharmacy.UserStockController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.StockQty;
import com.divudi.ejb.BillNumberGenerator;

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
public class StoreTransferIssueController implements Serializable {

    private Bill requestedBill;
    private Bill issuedBill;
    private boolean printPreview;
    private Date fromDate;
    private Date toDate;
    ///////
    @Inject
    private SessionController sessionController;
    @Inject
    UserStockController userStockController;
    @Inject
    BillController billController;
    ////
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    ////
    @EJB
    StoreBean storeBean;
    @Inject
    StoreCalculation storeCalculation;
    @EJB
    private BillNumberGenerator billNumberBean;

    private CommonFunctions commonFunctions;
    private List<BillItem> billItems;
    UserStockContainer userStockContainer;

    private double totalItemQuantity;

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

    public void makeNull() {
        getStoreBean().retiredAllUserStockContainer(getSessionController().getLoggedUser());
        requestedBill = null;
        issuedBill = null;
        printPreview = false;
        fromDate = null;
        toDate = null;
        billItems = null;
        userStockContainer = null;
    }

    public StoreTransferIssueController() {
    }

    public String navigateToStoreIssueForRequests() {
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        createRequestIssueBillItems(requestedBill);
        return "/store/store_transfer_issue";
    }

    public void createRequestIssueBillItems(Bill requestedBill) {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        makeNull();
        this.requestedBill = requestedBill;
        issuedBill = null;
        generateBillComponent();
    }

    public Bill getRequestedBill() {
        if (requestedBill == null) {
            requestedBill = new BilledBill();
        }
        return requestedBill;
    }

    public void setRequestedBill(Bill requestedBill) {
        getStoreBean().retiredAllUserStockContainer(getSessionController().getLoggedUser());
        makeNull();
        this.requestedBill = requestedBill;
        issuedBill = null;
        generateBillComponent();
    }

    public void generateBillComponent() {

        //User Stock Container Save if New Bill
        UserStockContainer usc = getStoreBean().saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        List<BillItem> bis = billController.billItemsOfBill(getRequestedBill());
        getIssuedBill().setDepartment(requestedBill.getFromDepartment());
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        getIssuedBill().setToDepartment(requestedBill.getDepartment());

        for (BillItem i : bis) {

            boolean flagStockFound = false;

            double billedIssue = storeCalculation.getBilledIssuedByRequestedItem(i, BillType.StoreTransferIssue);
            double cancelledIssue = storeCalculation.getCancelledIssuedByRequestedItem(i, BillType.StoreTransferIssue);

            double issuableQty = i.getQty() - (Math.abs(billedIssue) - Math.abs(cancelledIssue));

            List<StockQty> stockQtys = storeBean.getStockByQty(i.getItem(), issuableQty, getSessionController().getDepartment());

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

//        Stock stock = new Stock();
//        boolean flag = false;
//        for (BillItem b : getBillItems()) {
//            if (b.getPharmaceuticalBillItem().getStock().getId() == stock.getId()) {
//                flag = true;
//                break;
//            }
//            stock = b.getPharmaceuticalBillItem().getStock();
//        }
//
//        if (flag) {
//            billItems = null;
//            JsfUtil.addErrorMessage("There is Some Item in request that are added Multiple Time in Transfer request!!! please check request you can't issue errornus transfer request");
//        }
    }

    public void settle() {
        if (getIssuedBill().getToStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return;
        }
        totalItemQuantity = 0;
        for (BillItem i : getBillItems()) {
            if (i.getPharmaceuticalBillItem().getStock() == null) {
                JsfUtil.addErrorMessage("Please Select Item Batch for " + i.getItem().getName());
                continue;
            }
            if (i.getQty() == 0.0 || i.getItem() instanceof Vmpp || i.getItem() instanceof Vmp) {
                continue;
            } else {
                totalItemQuantity += i.getQty();
            }

        }
        if (totalItemQuantity == 0) {
            JsfUtil.addErrorMessage("No Items Available for Issuing");
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
            if (!getStoreBean().isStockAvailable(tmpPh.getStock(), tmpPh.getQtyInUnit(), getSessionController().getLoggedUser())) {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
                getIssuedBill().getBillItems().add(i);
                continue;
            }

            //Remove Department Stock
            boolean returnFlag = getStoreBean().deductFromStock(i.getPharmaceuticalBillItem().getStock(),
                    Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()),
                    i.getPharmaceuticalBillItem(),
                    getSessionController().getDepartment());

            if (returnFlag) {

                //Addinng Staff
                Stock staffStock = getStoreBean().addToStock(i.getPharmaceuticalBillItem(),
                        Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), getIssuedBill().getToStaff());

                i.getPharmaceuticalBillItem().setStaffStock(staffStock);

            } else {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
            }

            getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());

            getIssuedBill().getBillItems().add(i);
        }

        getIssuedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StoreTransferIssue, BillClassType.BilledBill, BillNumberSuffix.STTI));
        getIssuedBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StoreTransferIssue, BillClassType.BilledBill, BillNumberSuffix.STTI));

        getIssuedBill().setInstitution(getSessionController().getInstitution());
        getIssuedBill().setDepartment(getSessionController().getDepartment());

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

        getStoreBean().retiredAllUserStockContainer(getSessionController().getLoggedUser());

        issuedBill = null;
        issuedBill = b;

        printPreview = true;

    }

    private double calTotal() {
        double value = 0;
        int serialNo = 0;
        for (BillItem b : getIssuedBill().getBillItems()) {
            value += (b.getPharmaceuticalBillItem().getPurchaseRate() * b.getPharmaceuticalBillItem().getQty());
            b.setSearialNo(serialNo++);
        }

        return value;

    }

    @Inject
    private PharmacyController pharmacyController;

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        double availableStock = getStoreBean().getStockQty(tmp.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());

        if (availableStock < tmp.getPharmaceuticalBillItem().getQtyInUnit()) {
            tmp.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        //Check Is There Any Other User using same Stock
        if (!getStoreBean().isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {
            tmp.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        getStoreBean().updateUserStock(tmp.getTransUserStock(), tmp.getQty());

    }

    public void onFocus(BillItem tmp) {
        getPharmacyController().setPharmacyItem(tmp.getItem());
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
            issuedBill.setBillType(BillType.StoreTransferIssue);
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

    public StoreBean getStoreBean() {
        return storeBean;
    }

    public void setStoreBean(StoreBean storeBean) {
        this.storeBean = storeBean;
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

    public double getTotalItemQuantity() {
        return totalItemQuantity;
    }

    public void setTotalItemQuantity(double totalItemQuantity) {
        this.totalItemQuantity = totalItemQuantity;
    }

}
