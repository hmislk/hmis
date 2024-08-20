/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemsDistributorsFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StoreTransferRequestController implements Serializable {

    @Inject
    private SessionController sessionController;
    @Inject
    CommonController commonController;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    StoreBean storeBean;
    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;
    private Bill bill;
    private Institution dealor;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    @Inject
    StoreCalculation storeCalculation;
    private boolean printPreview;

    public void recreate() {
        bill = null;
        currentBillItem = null;
        dealor = null;
        billItems = null;
        printPreview = false;
    }
    
    public void changeDepartment() {
        billItems = null;
        bill.setToDepartment(null);
    }

    private boolean checkItems(Item item) {
        for (BillItem b : getBillItems()) {
            if (b.getItem().getId() == item.getId()) {
                return true;
            }
        }
        return false;
    }

    private boolean errorCheck() {
        if (getBill().getToDepartment() == null) {
            JsfUtil.addErrorMessage("Select Department");
            return true;
        }

        if (getBill().getToDepartment().getId() == getSessionController().getDepartment().getId()) {
            JsfUtil.addErrorMessage("You can't request same department");
            return true;
        }

        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Select Item");
            return true;
        }

        if (getCurrentBillItem().getTmpQty() == 0) {
            JsfUtil.addErrorMessage("Set Ordering Qty");
            return true;
        }

        if (checkItems(getCurrentBillItem().getItem())) {
            JsfUtil.addErrorMessage("Item is Already Added");
            return true;
        }

        return false;

    }

    public void addItem() {

        if (errorCheck()) {
            currentBillItem = null;
            return;
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());

        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRateInUnit(getStoreBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRateInUnit(getStoreBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));

        getBillItems().add(getCurrentBillItem());

        currentBillItem = null;
    }
//    @Inject
//    private PharmacyController pharmacyController;
    @Inject
    StoreController1 storeController1;

    public void onEdit(BillItem tmp) {
        storeController1.setPharmacyItem(tmp.getItem());
    }

    public void onEdit() {
        storeController1.setPharmacyItem(getCurrentBillItem().getItem());
    }

    public void saveBill() {
        if (getBill().getId() == null) {

            getBill().setInstitution(getSessionController().getInstitution());
            getBill().setDepartment(getSessionController().getDepartment());

            getBill().setToInstitution(getBill().getToDepartment().getInstitution());

            getBillFacade().create(getBill());
        }

    }

    public void request() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getBill().getToDepartment() == null) {
            JsfUtil.addErrorMessage("Select Requested Department");
            return;
        }

        if (getBill().getToDepartment() == getSessionController().getDepartment()) {
            JsfUtil.addErrorMessage("You cant request ur department itself");
            return;
        }

        for (BillItem bi : getBillItems()) {
            if (bi.getQty() == 0.0) {
                JsfUtil.addErrorMessage("Check Items Qty");
                return;
            }
        }

        saveBill();

        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StoreTransferRequest, BillClassType.BilledBill, BillNumberSuffix.STTRQ));
        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StoreTransferRequest, BillClassType.BilledBill, BillNumberSuffix.STTRQ));

        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setCreatedAt(Calendar.getInstance().getTime());

        getBillFacade().edit(getBill());

        for (BillItem b : getBillItems()) {
            b.setBill(getBill());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = b.getPharmaceuticalBillItem();
            b.setPharmaceuticalBillItem(null);
            getBillItemFacade().create(b);

            getPharmaceuticalBillItemFacade().create(tmpPh);

            b.setPharmaceuticalBillItem(tmpPh);
            getPharmaceuticalBillItemFacade().edit(tmpPh);
            getBillItemFacade().edit(b);

            getBill().getBillItems().add(b);
        }

        getBillFacade().edit(getBill());

        JsfUtil.addSuccessMessage("Transfer Request Succesfully Created");

        printPreview = true;
        
        

    }

    public void remove(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());
        int serialNo = 0;
        for (BillItem bi : getBillItems()) {
            bi.setSearialNo(serialNo++);
        }

    }

    public StoreTransferRequestController() {
    }

    public Institution getDealor() {

        return dealor;
    }

    public void setDealor(Institution dealor) {
        this.dealor = dealor;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.StoreTransferRequest);
        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
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

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

//    public boolean isPrintPreview() {
//        return printPreview;
//    }
//
//    public void setPrintPreview(boolean printPreview) {
//        this.printPreview = printPreview;
//    }
    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.setBillItem(currentBillItem);
            currentBillItem.setPharmaceuticalBillItem(ph);
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {

        this.currentBillItem = currentBillItem;
        if (currentBillItem != null && currentBillItem.getItem() != null) {
            storeController1.setPharmacyItem(currentBillItem.getItem());
        }
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

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
    
}
