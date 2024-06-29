/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemsDistributorsFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
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
public class StorePurchaseOrderRequestController implements Serializable {

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
    StoreBean storeBeen;
    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;
    private Bill currentBill;
    private BillItem currentBillItem;
    private List<BillItem> selectedBillItems;
    private List<BillItem> billItems;
    //private List<PharmaceuticalBillItem> pharmaceuticalBillItems;   
    @Inject
    StoreCalculation storeCalculation;
    private boolean printPreview;

    public void removeSelected() {
        //  //System.err.println("1");
        if (selectedBillItems == null) {
            //   //System.err.println("2");
            return;
        }

        for (BillItem b : selectedBillItems) {
            //System.err.println("SerialNO " + b.getSearialNo());
            //System.err.println("Item " + b.getItem().getName());
            BillItem tmp = getBillItems().remove(b.getSearialNo());
            tmp.setRetired(true);
            billItemFacade.edit(tmp);
            //billFacade.edit(currentBill);
            //System.err.println("Removed Item " + tmp.getItem().getName());
            calTotal();
        }

        selectedBillItems = null;
    }

    public void recreate() {
        currentBill = null;
        currentBillItem = null;
        billItems = null;
        printPreview = false;
    }

    public void addItem() {
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select and item from the list");
            return;
        }

        for (BillItem bi : getBillItems()) {
            if (getCurrentBillItem().getItem().equals(bi.getItem())) {
                JsfUtil.addErrorMessage("Already added this item");
                return;
            }
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());
        getCurrentBillItem().getPharmaceuticalBillItem().
                setPurchaseRateInUnit(getStoreBeen().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRateInUnit(getStoreBeen().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));

        getBillItems().add(getCurrentBillItem());

        calTotal();

        currentBillItem = null;
    }

    public void removeItem(BillItem bi) {
        //System.err.println("5 " + bi.getItem().getName());
        //System.err.println("6 " + bi.getSearialNo());
        ////// // System.out.println("bi = " + bi);
        if (bi != null) {
            getBillItems().remove(bi.getSearialNo());
            if (bi.getId() != null) {
                bi.setRetired(true);
                bi.setCreater(getSessionController().getLoggedUser());
                bi.setCreatedAt(new Date());
                billItemFacade.edit(bi);
            }
            calTotal();
            billFacade.edit(currentBill);
        }

    }

    @Inject
    private StoreController1 storeController1;

    public void onFocus(BillItem bi) {

        getStoreController1().setPharmacyItem(bi.getItem());
    }

    public void onEdit(BillItem bi) {
//        if (bi.getItem().getDepartmentType() == DepartmentType.Inventry) {
//            if (bi.getTmpQty() != 1) {
//                bi.setTmpQty(1);
//                JsfUtil.addErrorMessage("Asset Item Count Reset to 1");
//            }
//        }

        bi.setNetValue(bi.getPharmaceuticalBillItem().getQty() * bi.getPharmaceuticalBillItem().getPurchaseRate());

        getStoreController1().setPharmacyItem(bi.getItem());

        calTotal();
    }

    public void saveBill() {

        getCurrentBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StoreOrder, BillClassType.BilledBill, BillNumberSuffix.POR));
        getCurrentBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StoreOrder, BillClassType.BilledBill, BillNumberSuffix.POR));

        getCurrentBill().setCreater(getSessionController().getLoggedUser());
        getCurrentBill().setCreatedAt(Calendar.getInstance().getTime());

        getCurrentBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrentBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrentBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrentBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrentBill().setBillTypeAtomic(BillTypeAtomic.STORE_ORDER_PRE);
        if (getCurrentBill().getId() == null) {
            getBillFacade().create(getCurrentBill());
        } else {
            billFacade.edit(getCurrentBill());
        }

    }

    public void generateBillComponent() {
        // int serialNo = 0;
        setBillItems(new ArrayList<BillItem>());
        for (Item i : storeCalculation.getItemsForDealor(getCurrentBill().getToInstitution())) {
            BillItem bi = new BillItem();
            bi.setItem(i);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            tmp.setQty(getStoreBeen().getOrderingQty(bi.getItem(), getSessionController().getDepartment()));
            tmp.setPurchaseRateInUnit(getStoreBeen().getLastPurchaseRate(bi.getItem(), getSessionController().getDepartment()));
            tmp.setRetailRateInUnit(getStoreBeen().getLastRetailRate(bi.getItem(), getSessionController().getDepartment()));

            bi.setTmpQty(tmp.getQty());
            bi.setPharmaceuticalBillItem(tmp);

            getBillItems().add(bi);

        }

        calTotal();

    }

    public void saveBillComponent() {
        for (BillItem b : getBillItems()) {
            b.setRate(b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            b.setNetValue(b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            b.setBill(getCurrentBill());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

//            PharmaceuticalBillItem tmpPh = b.getPharmaceuticalBillItem();
//            b.setPharmaceuticalBillItem(null);

            if (b.getId() == null) {
                getBillItemFacade().create(b);
            } else {
                getBillItemFacade().edit(b);
            }

            if (b.getPharmaceuticalBillItem().getId() == null) {
                getPharmaceuticalBillItemFacade().create(b.getPharmaceuticalBillItem());
            } else {
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }
        }
    }

    public void createOrderWithItems() {
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Dealor");

        }

        generateBillComponent();

    }

    public void request() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getCurrentBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select Paymntmethod");
            return;
        }

        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Distributor ?");
            return;
        }
        
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please Select Item or Items");
            return;
        }

//
//        if (checkItemPrice()) {
//            JsfUtil.addErrorMessage("Please enter purchase price for all");
//            return;
//        }
        calTotal();

        saveBill();
        saveBillComponent();

        JsfUtil.addSuccessMessage("Request Succesfully Created");

        printPreview = true;
        
        

    }

    public void calTotal() {
        double tmp = 0;
        int serialNo = 0;
        for (BillItem b : getBillItems()) {
            ////// // System.out.println("b = " + b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getPurchaseRate());
            tmp += b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getPurchaseRate();
            b.setSearialNo(serialNo++);
        }

        getCurrentBill().setTotal(tmp);
        getCurrentBill().setNetTotal(tmp);
        ////// // System.out.println("serialNo = " + tmp);

    }

    public StorePurchaseOrderRequestController() {
    }

    @Inject
    private ItemController itemController;

    public void setInsListener() {
        getItemController().setInstitution(getCurrentBill().getToInstitution());
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

    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new BilledBill();
            currentBill.setBillType(BillType.StoreOrder);
        }
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
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

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            currentBillItem.setPharmaceuticalBillItem(ph);
            ph.setBillItem(currentBillItem);
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
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

    public StoreCalculation getStoreCalculation() {
        return storeCalculation;
    }

    public void setStoreCalculation(StoreCalculation storeCalculation) {
        this.storeCalculation = storeCalculation;
    }

    public StoreController1 getStoreController1() {
        return storeController1;
    }

    public void setStoreController1(StoreController1 storeController1) {
        this.storeController1 = storeController1;
    }

    public StoreBean getStoreBeen() {
        return storeBeen;
    }

    public void setStoreBeen(StoreBean storeBeen) {
        this.storeBeen = storeBeen;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

}
