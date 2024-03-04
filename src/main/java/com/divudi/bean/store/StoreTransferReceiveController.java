/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
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
public class StoreTransferReceiveController implements Serializable {

    private Bill issuedBill;
    private Bill receivedBill;
    private boolean printPreview;
    private Date fromDate;
    private Date toDate;
    ///////
    @Inject
    private SessionController sessionController;
    @Inject
    private PharmacyController pharmacyController;
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
    @EJB
    private BillNumberGenerator billNumberBean;

    private CommonFunctions commonFunctions;
    @Inject
    StoreCalculation storeCalculation;
    private List<BillItem> billItems;
    private List<Bill> bills;
    private SearchKeyword searchKeyword;

    public void onFocus(BillItem tmp) {
        getPharmacyController().setPharmacyItem(tmp.getItem());
    }

    public void makeNull() {
        issuedBill = null;
        receivedBill = null;
        printPreview = false;
        fromDate = null;
        toDate = null;
        billItems = null;
    }

    public StoreTransferReceiveController() {
    }

    public Bill getIssuedBill() {
        if (issuedBill == null) {
            issuedBill = new BilledBill();
        }
        return issuedBill;
    }

    public void setIssuedBill(Bill issuedBill) {
        makeNull();
        this.issuedBill = issuedBill;
        receivedBill = null;
        generateBillComponent();
    }

    public void generateBillComponent() {

        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getIssuedBill())) {

            BillItem bItem = new BillItem();
            bItem.setReferanceBillItem(i.getBillItem());
            bItem.copy(i.getBillItem());
            if (Math.abs(i.getQtyInUnit()) >= Math.abs(i.getStaffStock().getStock())) {
                bItem.setTmpQty(Math.abs(i.getQtyInUnit()));
            } else {
                bItem.setTmpQty(Math.abs(i.getStaffStock().getStock()));
            }

            bItem.setSearialNo(getBillItems().size());

            //       bItem.setTmpSuggession(getPharmacyCalculation().getSuggessionOnly(i.getBillItem().getItem()));
            PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
            phItem.setBillItem(bItem);
            phItem.copy(i);
            phItem.invertValue(i);

            bItem.setPharmaceuticalBillItem(phItem);

            getBillItems().add(bItem);

        }

    }

    @EJB
    private StockFacade stockFacade;

    public void settle() {

        saveBill();

        for (BillItem i : getBillItems()) {

//            i.getPharmaceuticalBillItem().setQtyInUnit(i.getQty());
            if (i.getPharmaceuticalBillItem().getQtyInUnit() == 0.0 || i.getItem() instanceof Vmpp || i.getItem() instanceof Vmp) {
                continue;
            }

            i.setBill(getReceivedBill());
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

            tmpPh.setItemBatch(tmpPh.getStaffStock().getItemBatch());

            double qty = Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit());

            //    Deduct Staff Stock
            boolean returnFlag = getStoreBean().deductFromStock(tmpPh, Math.abs(qty), getIssuedBill().getToStaff());

            if (returnFlag) {
                //     Add Stock To Department
                Stock addedStock = getStoreBean().addToStock(tmpPh, Math.abs(qty), getSessionController().getDepartment());

                tmpPh.setStock(addedStock);
            } else {
                i.setTmpQty(0);
                getBillItemFacade().edit(i);
            }

//            //Temprory Solution
//            Stock addedStock = getStoreBean().addToStock(tmpPh, Math.abs(qty), getSessionController().getDepartment());
//            tmpPh.setStock(addedStock);
            getPharmaceuticalBillItemFacade().edit(tmpPh);

            getReceivedBill().getBillItems().add(i);
        }

        getReceivedBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StoreTransferReceive, BillClassType.BilledBill, BillNumberSuffix.STTR));
        getReceivedBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StoreTransferReceive, BillClassType.BilledBill, BillNumberSuffix.STTR));

        getReceivedBill().setInstitution(getSessionController().getInstitution());
        getReceivedBill().setDepartment(getSessionController().getDepartment());

        getReceivedBill().setCreater(getSessionController().getLoggedUser());
        getReceivedBill().setCreatedAt(Calendar.getInstance().getTime());

        getReceivedBill().setBackwardReferenceBill(getIssuedBill());

        getReceivedBill().setNetTotal(calTotal());

        getBillFacade().edit(getReceivedBill());

        //Update Issue Bills Reference Bill
        getIssuedBill().getForwardReferenceBills().add(getReceivedBill());
        getBillFacade().edit(getIssuedBill());

//        getIssuedBill().setReferenceBill(getReceivedBill());
//        getBillFacade().edit(getIssuedBill());
        printPreview = true;

    }

    private double calTotal() {
        double value = 0;
        int serialNo = 0;
        for (BillItem b : getBillItems()) {
            value += (b.getPharmaceuticalBillItem().getPurchaseRate() * b.getPharmaceuticalBillItem().getQty());
            b.setSearialNo(serialNo++);
        }

        return value;

    }

//    public void onEdit(BillItem tmp) {
//        double availableStock = getStoreBean().getStockQty(tmp.getPharmaceuticalBillItem().getItemBatch(), getReceivedBill().getFromStaff());
//        double oldValue = getPharmaceuticalBillItemFacade().find(tmp.getPharmaceuticalBillItem().getId()).getQtyInUnit();
//        if (availableStock < tmp.getQty()) {
//            tmp.setQty(oldValue);
//            JsfUtil.addErrorMessage("You cant recieved over than Issued Qty setted Old Value");
//        }
//
//        //   getPharmacyController().setPharmacyItem(tmp.getItem());
//    }
    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
//        double availableStock = getStoreBean().getStockQty(tmp.getPharmaceuticalBillItem().getStaffStock(), getIssuedBill().getToStaff());
        //   double oldValue = getPharmaceuticalBillItemFacade().find(tmp.getPharmaceuticalBillItem().getId()).getQtyInUnit();
        if (tmp.getPharmaceuticalBillItem().getStaffStock().getStock() < tmp.getQty()) {
            tmp.setTmpQty(0.0);
            JsfUtil.addErrorMessage("You cant recieved over than Issued Qty setted Old Value");
        }

        //   getPharmacyController().setPharmacyItem(tmp.getItem());
    }

    public void saveBill() {
        getReceivedBill().setBillType(BillType.StoreTransferReceive);
        getReceivedBill().setBackwardReferenceBill(getIssuedBill());
        getReceivedBill().setFromStaff(getIssuedBill().getToStaff());
        getReceivedBill().setFromInstitution(getIssuedBill().getInstitution());
        getReceivedBill().setFromDepartment(getIssuedBill().getDepartment());

        if (getReceivedBill().getId() == null) {
            getBillFacade().create(getReceivedBill());
        }
    }

    public Bill getReceivedBill() {
        if (receivedBill == null) {
            receivedBill = new BilledBill();
        }
        return receivedBill;
    }

    public void setReceivedBill(Bill receivedBill) {
        this.receivedBill = receivedBill;
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

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
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

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

}
