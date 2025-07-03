/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemsDistributorsFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class TransferRequestController implements Serializable {

    @Inject
    private SessionController sessionController;
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
    private PharmacyBean pharmacyBean;
    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;
    private Bill bill;
    private Bill transerRequestBillPre;
    private Institution dealor;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    @Inject
    private PharmacyCalculation pharmacyBillBean;
    private boolean printPreview;
    @Inject
    NotificationController notificationController;
    @Inject
    private PharmacyController pharmacyController;

    private Department toDepartment;

    public void recreate() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        toDepartment = null;
        bill = null;
        currentBillItem = null;
        dealor = null;
        billItems = null;
        printPreview = false;
        transerRequestBillPre = null;

    }

    public void changeDepartment() {
        billItems = null;
        setToDepartment(null);
    }

    private boolean checkItems(Item item) {
        for (BillItem b : getBillItems()) {
            if (Objects.equals(b.getItem().getId(), item.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean errorCheck() {
        if (getToDepartment() == null) {
            JsfUtil.addErrorMessage("Select Department");
            return true;
        }

        if (Objects.equals(getToDepartment().getId(), getSessionController().getDepartment().getId())) {
            JsfUtil.addErrorMessage("U can't request same department");
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

        if (getBillItems().size() >= 50) {
            JsfUtil.addErrorMessage("You Can Only Add 50 Items For this Request.");
            return true;
        }

        return false;

    }

    @EJB
    StockFacade stockFacade;

    public void addAllItem() {

        if (getBill().getToDepartment() == null) {
            JsfUtil.addErrorMessage("Dept ?");
            return;
        }
        String jpql = "select s from Stock s where s.department=:dept";
        Map m = new HashMap();
        m.put("dept", getBill().getToDepartment());
        List<Stock> allAvailableStocks = stockFacade.findByJpql(jpql, m);
        for (Stock s : allAvailableStocks) {
            currentBillItem = null;
            getCurrentBillItem().setItem(s.getItemBatch().getItem());
            getCurrentBillItem().setTmpQty(s.getStock());
            addItem();
        }
        if (errorCheck()) {
            currentBillItem = null;
            return;
        }

        getCurrentBillItem().setSearialNo(getBillItems().size());

        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRateInUnit(getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRateInUnit(getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));

        getBillItems().add(getCurrentBillItem());

        currentBillItem = null;
    }

    public void addItem() {
        if (errorCheck()) {
            currentBillItem = null;
            return;
        }
        
        // User Input is getCurrentBillItem().getQty() > We should NOT change this programmitically
        // This user input needed to be recorded in pharmaceutical bill item and bill item Finance Details
        // pharmaceutical bill item qty will always be in units
        // If Ampp or Vmpp > have to multiply by pack size and write the qty in units in pharmaceutical bill item
        // have to add all quantity related data for bill Item Financial Details - No pricing related data is required
        
        getCurrentBillItem().setSearialNo(getBillItems().size());
        getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(getPharmacyBean().getLastPurchaseRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().getPharmaceuticalBillItem().setRetailRateInUnit(getPharmacyBean().getLastRetailRate(getCurrentBillItem().getItem(), getSessionController().getDepartment()));
        getCurrentBillItem().setQty(getCurrentBillItem().getTmpQty());
        getCurrentBillItem().getBillItemFinanceDetails();
        
        getBillItems().add(getCurrentBillItem());
        
        
        
        currentBillItem = null;
    }

    public void onEdit(BillItem tmp) {
        getPharmacyController().setPharmacyItem(tmp.getItem());
    }

    public void onEdit() {
        getPharmacyController().setPharmacyItem(getCurrentBillItem().getItem());
    }

    public void displayItemDetails(BillItem bi) {
        getPharmacyController().fillItemDetails(bi.getItem());
    }

    public void saveBill() {
        if (getBill().getId() == null) {

            getBill().setInstitution(getSessionController().getInstitution());
            getBill().setDepartment(getSessionController().getDepartment());

            getBill().setToInstitution(getBill().getToDepartment().getInstitution());

            getBillFacade().create(getBill());
        }

    }

    public void approveTransferRequestBill() {
        transerRequestBillPre.setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
        transerRequestBillPre.setApproveAt(new Date());
        transerRequestBillPre.setCheckedBy(sessionController.getLoggedUser());
        transerRequestBillPre.setCheckeAt(new Date());
        transerRequestBillPre.setApproveUser(sessionController.getLoggedUser());
        billFacade.edit(transerRequestBillPre);
        JsfUtil.addSuccessMessage("Approval done. Send the request to " + transerRequestBillPre.getToDepartment());

        bill = transerRequestBillPre;
        printPreview = true;

    }

    public void request() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getBillItems() == null) {
            JsfUtil.addErrorMessage("No Item Selected to Request");
            return;
        }

        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Item Selected to Request");
            return;
        }

        if (getToDepartment() == null) {
            JsfUtil.addErrorMessage("Select Requested Department");
            return;
        }
        getBill().setToDepartment(toDepartment);
        getBill().setToInstitution(getBill().getToDepartment().getInstitution());

        getBill().setFromDepartment(getSessionController().getDepartment());
        getBill().setFromInstitution(getSessionController().getInstitution());

        if (getBill().getToDepartment().equals(getBill().getFromDepartment())) {
            JsfUtil.addErrorMessage("You cant request from you own department.");
            return;
        }

        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items Requested");
            return;
        }

        for (BillItem bi : getBillItems()) {
            if (bi.getQty() == 0.0) {
                JsfUtil.addErrorMessage("Some Items Have Zero Quantities");
                return;
            }
        }

        saveBill();
        if (transerRequestBillPre != null) {
            transerRequestBillPre.setForwardReferenceBill(getBill());
            getBill().setReferenceBill(transerRequestBillPre);
            getBillFacade().edit(getTranserRequestBillPre());
        }

        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ));
        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ));

        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setCreatedAt(Calendar.getInstance().getTime());

        getBillFacade().edit(getBill());

        for (BillItem b : getBillItems()) {
            b.setBill(getBill());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = b.getPharmaceuticalBillItem();
            b.setPharmaceuticalBillItem(null);

            if (b.getId() == null) {
                getBillItemFacade().create(b);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            b.setPharmaceuticalBillItem(tmpPh);
            getPharmaceuticalBillItemFacade().edit(tmpPh);
            getBillItemFacade().edit(b);

            getBill().getBillItems().add(b);
        }
        getBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
        getBillFacade().edit(getBill());
        JsfUtil.addSuccessMessage("Transfer Request Succesfully Created");
        printPreview = true;
        notificationController.createNotification(getBill());

    }

    public void saveTranserRequest() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getBillItems() == null) {
            JsfUtil.addErrorMessage("No Item Selected to Request");
            return;
        }

        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Item Selected to Request");
            return;
        }

        if (getToDepartment() == null) {
            JsfUtil.addErrorMessage("Select Requested Department");
            return;
        }
        getTranserRequestBillPre().setToDepartment(toDepartment);
        getTranserRequestBillPre().setToInstitution(getTranserRequestBillPre().getToDepartment().getInstitution());
        getTranserRequestBillPre().setFromDepartment(getSessionController().getDepartment());
        getTranserRequestBillPre().setFromInstitution(getSessionController().getInstitution());

        if (getToDepartment().equals(getTranserRequestBillPre().getFromDepartment())) {
            JsfUtil.addErrorMessage("You cant request from you own department.");
            return;
        }

        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items Requested");
            return;
        }

        for (BillItem bi : getBillItems()) {
            if (bi.getQty() == 0.0) {
                JsfUtil.addErrorMessage("Some Items Have Zero Quantities");
                return;
            }
        }

        getTranserRequestBillPre().setInstitution(getSessionController().getInstitution());
        getTranserRequestBillPre().setDepartment(getSessionController().getDepartment());
        if (getTranserRequestBillPre().getId() == null) {
            getBillFacade().create(getTranserRequestBillPre());
        }
        getTranserRequestBillPre().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ));
        getTranserRequestBillPre().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ));

        getTranserRequestBillPre().setCreater(getSessionController().getLoggedUser());
        getTranserRequestBillPre().setCreatedAt(Calendar.getInstance().getTime());

        getBillFacade().edit(getTranserRequestBillPre());
        if (getTranserRequestBillPre().getBillItems().size() != 0) {
            getTranserRequestBillPre().setBillItems(new ArrayList<>());
        }
        for (BillItem b : getBillItems()) {
            b.setBill(getTranserRequestBillPre());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = b.getPharmaceuticalBillItem();
            b.setPharmaceuticalBillItem(null);

            if (b.getId() == null) {
                getBillItemFacade().create(b);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            b.setPharmaceuticalBillItem(tmpPh);
            getPharmaceuticalBillItemFacade().edit(tmpPh);
            getBillItemFacade().edit(b);
            getTranserRequestBillPre().getBillItems().add(b);
        }
        getTranserRequestBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
        getBillFacade().edit(getTranserRequestBillPre());
        JsfUtil.addSuccessMessage("Transfer Request Succesfully Created");
    }

    public String navigateToEditRequest() {
        Bill tranferRequestBillTemp = transerRequestBillPre;
        recreate();
        transerRequestBillPre = tranferRequestBillTemp;
        if (transerRequestBillPre == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }

        billItems = new ArrayList<>();
        billItems.addAll(getTranserRequestBillPre().getBillItems());
        System.out.println("line 401");
        System.out.println(billItems.size());
        for (BillItem bi : billItems) {
            bi.setTmpQty(bi.getQty());
            billItemFacade.edit(bi);
        }
        setToDepartment(getTranserRequestBillPre().getToDepartment());
        return "/pharmacy/pharmacy_transfer_request_save?faces-redirect=true";
    }

    @Inject
    private SearchController searchController;

    public String navigateToApproveRequest() {
        Bill tranferRequestBillTemp = transerRequestBillPre;
        recreate();
        transerRequestBillPre = tranferRequestBillTemp;
        if (transerRequestBillPre == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        billItems = new ArrayList<>();
        billItems.addAll(getTranserRequestBillPre().getBillItems());
        for (BillItem bi : billItems) {
            bi.setTmpQty(bi.getQty());
        }
        setToDepartment(getTranserRequestBillPre().getToDepartment());
        return "/pharmacy/pharmacy_transfer_request_approval?faces-redirect=true";
    }

    public String finalizeTranserRequest() {
        if (transerRequestBillPre == null) {
            JsfUtil.addErrorMessage("No Bill! Save the Bill First");
            return "";
        }
        if (transerRequestBillPre.getId() == null) {
            saveTranserRequest();
        }
        if (getTranserRequestBillPre().getBillItems().size() != 0) {
            getTranserRequestBillPre().setBillItems(new ArrayList<>());
        }
        getTranserRequestBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
        System.out.println("line 436");
        System.out.println(transerRequestBillPre.getBillItems().size());

        for (BillItem b : getBillItems()) {
            //System.out.println(b.getPharmaceuticalBillItem().getItemBatch().getItem().getName());
            b.setBill(getTranserRequestBillPre());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = b.getPharmaceuticalBillItem();
            b.setPharmaceuticalBillItem(null);

            if (b.getId() == null) {
                getBillItemFacade().create(b);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            b.setPharmaceuticalBillItem(tmpPh);
            getPharmaceuticalBillItemFacade().edit(tmpPh);
            getBillItemFacade().edit(b);
            getTranserRequestBillPre().getBillItems().add(b);
        }
        System.out.println("line 461");
        getTranserRequestBillPre().setEditedAt(new Date());
        getTranserRequestBillPre().setEditor(sessionController.getLoggedUser());
        getTranserRequestBillPre().setCheckeAt(new Date());
        getTranserRequestBillPre().setCheckedBy(sessionController.getLoggedUser());
        getBillFacade().edit(getTranserRequestBillPre());
        JsfUtil.addSuccessMessage("Transfer Request Succesfully Finalized");

        searchController.fillSavedTranserRequestBills();
        return "/pharmacy/pharmacy_transfer_request_list_search_for_approval?faces-redirect=true";
    }

    public String processTransferRequest() {
        if (toDepartment == null) {
            JsfUtil.addErrorMessage("Please Select a Department");
            return "";
        }
        if (Objects.equals(toDepartment, sessionController.getLoggedUser().getDepartment())) {
            JsfUtil.addErrorMessage("Cannot Make a Request with the Same Department");
            return "";
        }
        return "/pharmacy/pharmacy_transfer_request";
    }

    public void remove(BillItem billItem) {
        getBillItems().remove(billItem.getSearialNo());
        int serialNo = 0;
        for (BillItem bi : getBillItems()) {
            bi.setSearialNo(serialNo++);
        }

    }

    public TransferRequestController() {
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
            bill.setBillType(BillType.PharmacyTransferRequest);
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

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

    public PharmacyCalculation getPharmacyBillBean() {
        return pharmacyBillBean;
    }

    public void setPharmacyBillBean(PharmacyCalculation pharmacyBillBean) {
        this.pharmacyBillBean = pharmacyBillBean;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
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
            getPharmacyController().setPharmacyItem(currentBillItem.getItem());
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

    public Bill getTranserRequestBillPre() {
        if (transerRequestBillPre == null) {
            transerRequestBillPre = new BilledBill();
            transerRequestBillPre.setBillType(BillType.PharmacyTransferRequest);
        }

        return transerRequestBillPre;
    }

    public void setTranserRequestBillPre(Bill transerRequestBillPre) {
        this.transerRequestBillPre = transerRequestBillPre;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

}
