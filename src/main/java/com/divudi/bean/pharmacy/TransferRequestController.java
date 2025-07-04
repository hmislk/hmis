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
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemsDistributorsFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
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

    private static final Logger LOGGER = Logger.getLogger(TransferRequestController.class.getName());

    // <editor-fold defaultstate="collapsed" desc="EJBs">
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
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmacyCostingService pharmacyCostingService;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    @Inject
    private PharmacyCalculation pharmacyBillBean;
    @Inject
    private NotificationController notificationController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private SearchController searchController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private Bill bill;
    private Bill transferRequestBillPre;
    private Institution dealor;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    private boolean printPreview;
    private Department toDepartment;
    // </editor-fold>

    public void recreate() {
        toDepartment = null;
        bill = null;
        currentBillItem = null;
        dealor = null;
        billItems = null;
        printPreview = false;
        transferRequestBillPre = null;
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
        if (getCurrentBillItem().getQty() == 0) {
            JsfUtil.addErrorMessage("Set Ordering Qty");
            return true;
        }
        if (checkItems(getCurrentBillItem().getItem())) {
            JsfUtil.addErrorMessage("Item is Already Added");
            return true;
        }
        return false;
    }

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
        BillItem bi = getCurrentBillItem();
        PharmaceuticalBillItem ph = bi.getPharmaceuticalBillItem();
        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        Item item = bi.getItem();

        bi.setSearialNo(getBillItems().size());
        ph.setPurchaseRate(getPharmacyBean().getLastPurchaseRate(item, getSessionController().getDepartment()));
        ph.setRetailRateInUnit(getPharmacyBean().getLastRetailRate(item, getSessionController().getDepartment()));

        updateFinancials(fd);
        getBillItems().add(bi);
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), getBillItems());

        currentBillItem = null;
    }

    public void onEdit(BillItem tmp) {
        updateFinancials(tmp.getBillItemFinanceDetails());
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), getBillItems());
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
        transferRequestBillPre.setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
        transferRequestBillPre.setApproveAt(new Date());
        transferRequestBillPre.setCheckedBy(sessionController.getLoggedUser());
        transferRequestBillPre.setCheckeAt(new Date());
        transferRequestBillPre.setApproveUser(sessionController.getLoggedUser());
        billFacade.edit(transferRequestBillPre);
        JsfUtil.addSuccessMessage("Approval done. Send the request to " + transferRequestBillPre.getToDepartment());

        bill = transferRequestBillPre;
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
        if (transferRequestBillPre != null) {
            transferRequestBillPre.setForwardReferenceBill(getBill());
            getBill().setReferenceBill(transferRequestBillPre);
            getBillFacade().edit(getTransferRequestBillPre());
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

    public boolean errorsPresent() {
        if (getBillItems() == null) {
            JsfUtil.addErrorMessage("No Item Selected to Request");
            return true;
        }
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Item Selected to Request");
            return true;
        }
        if (getToDepartment() == null) {
            JsfUtil.addErrorMessage("Select Requested Department");
            return true;
        }
        return false;
    }

    public void saveTransferRequestPreBillAndBillItems() {
        getTransferRequestBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
        getTransferRequestBillPre().setBillType(BillType.PharmacyTransferRequest);
        getTransferRequestBillPre().setToDepartment(getToDepartment());
        getTransferRequestBillPre().setToInstitution(getToDepartment().getInstitution());
        getTransferRequestBillPre().setFromDepartment(getSessionController().getDepartment());
        getTransferRequestBillPre().setFromInstitution(getSessionController().getInstitution());

        if (getToDepartment().equals(getTransferRequestBillPre().getFromDepartment())) {
            JsfUtil.addErrorMessage("You cant request from you own department.");
            return;
        }
        for (BillItem bi : getBillItems()) {
            if (bi.getQty() == 0.0) {
                JsfUtil.addErrorMessage("Some Items Have Zero Quantities");
                return;
            }
        }
        getTransferRequestBillPre().setInstitution(getSessionController().getInstitution());
        getTransferRequestBillPre().setDepartment(getSessionController().getDepartment());
        if (getTransferRequestBillPre().getId() == null) {
            getBillFacade().create(getTransferRequestBillPre());
        }
        getTransferRequestBillPre().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ));
        getTransferRequestBillPre().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ));
        getTransferRequestBillPre().setCreater(getSessionController().getLoggedUser());
        getTransferRequestBillPre().setCreatedAt(Calendar.getInstance().getTime());
        getBillFacade().edit(getTransferRequestBillPre());
        for (BillItem b : getBillItems()) {
            b.setBill(getTransferRequestBillPre());
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

            if (b.getId() == null || !getTransferRequestBillPre().getBillItems().contains(b)) {
                getTransferRequestBillPre().getBillItems().add(b);
            }
        }
        getTransferRequestBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
        LOGGER.log(Level.FINE, "Finalizing transfer request with {0} items", getBillItems().size());
        getBillFacade().edit(getTransferRequestBillPre());
    }

    public void saveTranserRequestPreBill() {
        if (errorsPresent()) {
            return;
        }
        saveTransferRequestPreBillAndBillItems();
        JsfUtil.addSuccessMessage("Transfer Request Succesfully Created");
    }

    public String navigateToEditRequest() {
        Bill transferRequestBillTemp = transferRequestBillPre;
        recreate();
        transferRequestBillPre = transferRequestBillTemp;
        if (transferRequestBillPre == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        billItems = fetchBillItems(getTransferRequestBillPre());
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), billItems);
        LOGGER.log(Level.FINE, "Editing transfer request with {0} items", billItems.size());
        setToDepartment(getTransferRequestBillPre().getToDepartment());
        return "/pharmacy/pharmacy_transfer_request?faces-redirect=true";
    }

    public String navigateToApproveRequest() {
        Bill transferRequestBillTemp = transferRequestBillPre;
        recreate();
        transferRequestBillPre = transferRequestBillTemp;
        if (transferRequestBillPre == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        billItems = new ArrayList<>();
        billItems.addAll(getTransferRequestBillPre().getBillItems());
        for (BillItem bi : billItems) {
            bi.setTmpQty(bi.getQty());
        }
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), billItems);
        setToDepartment(getTransferRequestBillPre().getToDepartment());
        return "/pharmacy/pharmacy_transfer_request_approval?faces-redirect=true";
    }

    public void finalizeTranserRequestPreBill() {
        if (errorsPresent()) {
            return;
        }
        saveTransferRequestPreBillAndBillItems();
        getTransferRequestBillPre().setEditedAt(new Date());
        getTransferRequestBillPre().setEditor(sessionController.getLoggedUser());
        getTransferRequestBillPre().setCheckeAt(new Date());
        getTransferRequestBillPre().setCheckedBy(sessionController.getLoggedUser());
        getBillFacade().edit(getTransferRequestBillPre());
        JsfUtil.addSuccessMessage("Transfer Request Succesfully Finalized");
        printPreview = true;
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
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), getBillItems());

    }

    private List<BillItem> fetchBillItems(Bill bill) {
        List<BillItem> items = new ArrayList<>();
        if (bill == null) {
            return items;
        }
        String jpql = "select bi from BillItem bi where bi.bill=:bill and bi.retired=false";
        Map m = new HashMap();
        m.put("bill", bill);
        items = billItemFacade.findByJpql(jpql, m);
        return items;
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
            BillItemFinanceDetails fd = new BillItemFinanceDetails(currentBillItem);
            currentBillItem.setBillItemFinanceDetails(fd);
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

    public Bill getTransferRequestBillPre() {
        if (transferRequestBillPre == null) {
            transferRequestBillPre = new BilledBill();
            transferRequestBillPre.setBillType(BillType.PharmacyTransferRequest);
        }

        return transferRequestBillPre;
    }

    public void setTransferRequestBillPre(Bill transferRequestBillPre) {
        this.transferRequestBillPre = transferRequestBillPre;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    private void updateFinancials(BillItemFinanceDetails fd) {
        if (fd == null || fd.getBillItem() == null) {
            return;
        }

        BillItem bi = fd.getBillItem();
        PharmaceuticalBillItem ph = bi.getPharmaceuticalBillItem();
        Item item = bi.getItem();

        BigDecimal qty = BigDecimal.valueOf(bi.getQty());
        BigDecimal unitsPerPack = BigDecimal.ONE;
        if (item instanceof Ampp || item instanceof Vmpp) {
            unitsPerPack = item.getDblValue() > 0 ? BigDecimal.valueOf(item.getDblValue()) : BigDecimal.ONE;
        }

        fd.setUnitsPerPack(unitsPerPack);
        fd.setQuantity(qty);
        fd.setTotalQuantity(qty);

        fd.setLineGrossRate(determineTransferRate(item));
        fd.setRetailSaleRate(BigDecimal.valueOf(ph.getRetailRateInUnit()));

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);

        ph.setQty(fd.getQuantityByUnits().doubleValue());
        ph.setQtyPacks(fd.getQuantity().doubleValue());
        bi.setTmpQty(fd.getQuantity().doubleValue());
    }

    // ChatGPT contributed - Populate default rates when an item is selected
    public void populateRatesOnItemSelect() {
        BillItem bi = getCurrentBillItem();
        if (bi == null || bi.getItem() == null) {
            return;
        }

        PharmaceuticalBillItem ph = bi.getPharmaceuticalBillItem();
        if (ph == null) {
            ph = new PharmaceuticalBillItem();
            ph.setBillItem(bi);
            bi.setPharmaceuticalBillItem(ph);
        }

        ph.setPurchaseRate(pharmacyBean.getLastPurchaseRate(bi.getItem(), sessionController.getDepartment()));
        ph.setRetailRateInUnit(pharmacyBean.getLastRetailRate(bi.getItem(), sessionController.getDepartment()));

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        if (fd == null) {
            fd = new BillItemFinanceDetails(bi);
            bi.setBillItemFinanceDetails(fd);
        }

        fd.setLineCostRate(BigDecimal.valueOf(ph.getPurchaseRate()));
        fd.setLineGrossRate(determineTransferRate(bi.getItem()));

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), getBillItems());
    }

    // ChatGPT contributed - Recalculate item totals when gross rate changes
    public void onLineGrossRateChange(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), getBillItems());
    }

    private BigDecimal determineTransferRate(Item item) {
        boolean byPurchase = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean byCost = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
        boolean byRetail = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate", true);

        if (byPurchase) {
            return BigDecimal.valueOf(pharmacyBean.getLastPurchaseRate(item, sessionController.getDepartment()));
        } else if (byCost) {
            return BigDecimal.valueOf(pharmacyBean.getLastCostRate(item, sessionController.getDepartment()));
        } else {
            return BigDecimal.valueOf(pharmacyBean.getLastRetailRate(item, sessionController.getDepartment()));
        }
    }

}
