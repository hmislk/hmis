/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.StockQty;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CommonFunctions;
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
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
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
    @EJB
    private CommonFunctions commonFunctions;
    private List<BillItem> billItems;
    UserStockContainer userStockContainer;

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
            UtilityController.addErrorMessage("This Item Already removed");
            return;
        }

        userStockController.removeUserStock(billItem.getTransUserStock(), getSessionController().getLoggedUser());

        getBillItems().remove(billItem.getSearialNo());
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
    }

    public TransferIssueController() {
    }

    public Bill getRequestedBill() {
        if (requestedBill == null) {
            requestedBill = new BilledBill();

        }
        return requestedBill;
    }

    public void setRequestedBill(Bill requestedBill) {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        makeNull();
        this.requestedBill = requestedBill;
        issuedBill = null;
        generateBillComponent();
    }

    public void generateBillComponent() {

        //User Stock Container Save if New Bill
        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getRequestedBill())) {

            double billedIssue = getPharmacyCalculation().getBilledIssuedByRequestedItem(i.getBillItem(), BillType.PharmacyTransferIssue);
            double cancelledIssue = getPharmacyCalculation().getCancelledIssuedByRequestedItem(i.getBillItem(), BillType.PharmacyTransferIssue);

            double issuableQty = i.getQtyInUnit() - (Math.abs(billedIssue) - Math.abs(cancelledIssue));

            System.err.println("Issueable Qty " + issuableQty);

            List<StockQty> stockQtys = pharmacyBean.getStockByQty(i.getBillItem().getItem(), issuableQty, getSessionController().getDepartment());

            for (StockQty sq : stockQtys) {
                if (sq.getQty() == 0) {
                    continue;
                }

                //Checking User Stock Entity
                if (!userStockController.isStockAvailable(sq.getStock(), sq.getQty(), getSessionController().getLoggedUser())) {
                    UtilityController.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
                    continue;
                }

                System.err.println("Stock " + sq.getStock());
                System.err.println("QTY " + sq.getQty());
                BillItem bItem = new BillItem();
                bItem.setSearialNo(getBillItems().size());
                bItem.setItem(i.getBillItem().getItem());
                bItem.setReferanceBillItem(i.getBillItem());
                bItem.setTmpQty(sq.getQty());
                System.err.println("Bill Item QTY " + bItem.getQty());

//               s bItem.setTmpSuggession(getSuggession(i.getBillItem().getItem()));
                //     //System.err.println("List "+bItem.getTmpSuggession());
                PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
                phItem.setBillItem(bItem);
                phItem.setQtyInUnit((double) sq.getQty());
                System.err.println("Pharmac Item QTY " + phItem.getQtyInUnit());
                phItem.setFreeQtyInUnit(i.getFreeQtyInUnit());
                phItem.setPurchaseRateInUnit((double) sq.getStock().getItemBatch().getPurcahseRate());
                phItem.setRetailRateInUnit((double) sq.getStock().getItemBatch().getRetailsaleRate());
                phItem.setStock(sq.getStock());
                phItem.setDoe(sq.getStock().getItemBatch().getDateOfExpire());
                phItem.setItemBatch(sq.getStock().getItemBatch());
                bItem.setPharmaceuticalBillItem(phItem);

                //USER STOCK
                UserStock us = userStockController.saveUserStock(bItem, getSessionController().getLoggedUser(), usc);
                bItem.setTransUserStock(us);

                getBillItems().add(bItem);

            }

        }

        Stock stock = new Stock();
        boolean flag = false;
        for (BillItem b : getBillItems()) {
            if (b.getPharmaceuticalBillItem().getStock().getId() == stock.getId()) {
                flag = true;
                break;
            }
            stock = b.getPharmaceuticalBillItem().getStock();
        }

        if (flag) {
            billItems = null;
            UtilityController.addErrorMessage("There is Some Item in request that are added Multiple Time in Transfer request!!! please check request you can't issue errornus transfer request");
        }

    }

    public void settle() {
        if (getIssuedBill().getToStaff() == null) {
            UtilityController.addErrorMessage("Please Select Staff");
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

        if (getSessionController().getInstitutionPreference().isDepNumGenFromToDepartment()) {
            getIssuedBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getIssuedBill().getToDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        } else {
            getIssuedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI));
        }

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

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        issuedBill = null;
        issuedBill = b;

        printPreview = true;

    }

    private double calTotal() {
        double value = 0;
        int serialNo = 0;
        System.out.println("preference"+sessionController.getInstitutionPreference().isTranferNetTotalbyRetailRate());

        if (sessionController.getInstitutionPreference().isTranferNetTotalbyRetailRate()) {
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

    @Inject
    private PharmacyController pharmacyController;

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        double availableStock = pharmacyBean.getStockQty(tmp.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());

        if (availableStock < tmp.getPharmaceuticalBillItem().getQtyInUnit()) {
            tmp.setTmpQty(0.0);
            UtilityController.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {
            tmp.setTmpQty(0.0);
            UtilityController.addErrorMessage("You cant issue over than Stock Qty setted Old Value");
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

    }

    public void onFocus(BillItem tmp) {
        getPharmacyController().setPharmacyItem(tmp.getItem());
    }

    private void saveBill() {
        getIssuedBill().setReferenceBill(getRequestedBill());
        getIssuedBill().setToInstitution(getRequestedBill().getInstitution());
        getIssuedBill().setToDepartment(getRequestedBill().getDepartment());

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
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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

}
