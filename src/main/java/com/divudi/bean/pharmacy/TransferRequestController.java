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
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.service.BillService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.io.Serializable;
import java.math.BigInteger;
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
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    BillService billService;
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
    private boolean showAllBillFormats = false;
    private Department toDepartment;
    private List<Department> recentToDepartments;
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
        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return;
        }
        if (bill == null) {
            bill = new BilledBill();
        }
        bill.setDepartment(sessionController.getDepartment());
        bill.setFromDepartment(sessionController.getDepartment());
        bill = createNewApprovedTransferRequestBill(transferRequestBillPre, billItems, bill);
        printPreview = true;
    }

    public Bill createNewApprovedTransferRequestBill(Bill preBillToCreateApprovedBill, List<BillItem> transferRequestPreBillItems, Bill newApprovedBill) {
        if (transferRequestPreBillItems == null || transferRequestPreBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return null;
        }

        newApprovedBill.copy(preBillToCreateApprovedBill);

        newApprovedBill.setDepartment(sessionController.getDepartment());
        newApprovedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
        newApprovedBill.setBillType(BillType.PharmacyTransferRequest);

        newApprovedBill.setFromDepartment(preBillToCreateApprovedBill.getFromDepartment());
        newApprovedBill.setFromInstitution(preBillToCreateApprovedBill.getFromInstitution());
        newApprovedBill.setToDepartment(preBillToCreateApprovedBill.getToDepartment());
        newApprovedBill.setToInstitution(preBillToCreateApprovedBill.getToInstitution());

        newApprovedBill.setCreatedAt(new Date());
        newApprovedBill.setCreater(sessionController.getLoggedUser());

        String requestId = billNumberBean.departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(
                newApprovedBill.getDepartment(),
                newApprovedBill.getToDepartment(),
                BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);

        newApprovedBill.setBillDate(new Date());
        newApprovedBill.setBillTime(new Date());
        newApprovedBill.setDeptId(requestId);
        newApprovedBill.setInsId(requestId);
        newApprovedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
        newApprovedBill.setBillType(BillType.PharmacyTransferRequest);
        newApprovedBill.setApproveAt(new Date());
        newApprovedBill.setCheckedBy(sessionController.getLoggedUser());
        newApprovedBill.setCheckeAt(new Date());
        newApprovedBill.setApproveUser(sessionController.getLoggedUser());

        if (newApprovedBill.getId() == null) {
            newApprovedBill.setCreater(sessionController.getLoggedUser());
            newApprovedBill.setCreatedAt(new Date());
            billFacade.create(newApprovedBill);
        } else {
            billFacade.edit(newApprovedBill);
        }

        for (BillItem newBillItem : transferRequestPreBillItems) {
            newBillItem.setBill(newApprovedBill);
            if (newBillItem.getId() == null) {
                billItemFacade.create(newBillItem);
            } else {
                billItemFacade.edit(newBillItem);
            }
            newApprovedBill.getBillItems().add(newBillItem);
        }

        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(newApprovedBill, newApprovedBill.getBillItems());
        billFacade.edit(newApprovedBill);

        if (preBillToCreateApprovedBill != null) {
            preBillToCreateApprovedBill.setForwardReferenceBill(newApprovedBill);
            preBillToCreateApprovedBill.setApproveUser(sessionController.getLoggedUser());
            preBillToCreateApprovedBill.setApproveAt(new Date());
            preBillToCreateApprovedBill.setReferenceBill(newApprovedBill);
            billFacade.edit(preBillToCreateApprovedBill);
            newApprovedBill.setReferenceBill(preBillToCreateApprovedBill);
        }

        billFacade.edit(newApprovedBill);
        return newApprovedBill;
    }

    public void request() {
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
        if (getTransferRequestBillPre() == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return true;
        }
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
        String requestPreBillId = billNumberBean.departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(
                getSessionController().getDepartment(),
                getToDepartment(),
                BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
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
        getTransferRequestBillPre().setDeptId(requestPreBillId);
        getTransferRequestBillPre().setInsId(requestPreBillId);
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
        if (getTransferRequestBillPre().getBillItems() == null || getTransferRequestBillPre().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items in the request");
            return "";
        }
        bill = new BilledBill();
        bill.copy(transferRequestBillPre);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
        bill.setBillType(BillType.PharmacyTransferRequest);
        billItems = new ArrayList<>();
        for (BillItem requestItemInPreBill : getTransferRequestBillPre().getBillItems()) {
            BillItem newBillItemInApprovedRequest = new BillItem();
            newBillItemInApprovedRequest.copy(requestItemInPreBill);
            newBillItemInApprovedRequest.setBill(bill);
            billItems.add(newBillItemInApprovedRequest);
        }
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(bill, billItems);
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

        boolean approvalIsNeeded = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transer Request With Approval", false);
        if (!approvalIsNeeded) {
            bill = createNewApprovedTransferRequestBill(
                    getTransferRequestBillPre(),
                    getTransferRequestBillPre().getBillItems(),
                    new BilledBill()
            );
        } else {
            bill = getTransferRequestBillPre();
        }

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
        getTransferRequestBillPre().setFromInstitution(sessionController.getInstitution());
        getTransferRequestBillPre().setFromDepartment(sessionController.getDepartment());
        getTransferRequestBillPre().setToDepartment(toDepartment);
        getTransferRequestBillPre().setToInstitution(toDepartment.getInstitution());
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

        // Quantity entered by user
        BigDecimal qty = BigDecimal.valueOf(bi.getQty());

        // Determine units per pack for Ampp or Vmpp
        BigDecimal unitsPerPack = BigDecimal.ONE;
        if (item instanceof Ampp || item instanceof Vmpp) {
            unitsPerPack = item.getDblValue() > 0 ? BigDecimal.valueOf(item.getDblValue()) : BigDecimal.ONE;
        }

        fd.setUnitsPerPack(unitsPerPack);
        fd.setQuantity(qty);
        fd.setTotalQuantity(qty);

        // Line Gross Rate is expected to be entered manually or by caller
        BigDecimal grossRate = fd.getLineGrossRate();
        if (grossRate == null || grossRate.equals(BigDecimal.ZERO)) {
            BigDecimal tmpGrossRate = determineTransferRate(item);
            grossRate = tmpGrossRate.multiply(unitsPerPack);
            fd.setLineGrossRate(grossRate);
        }

        // Compute base values
        BigDecimal lineGrossTotal = grossRate.multiply(qty);
        fd.setLineGrossTotal(lineGrossTotal);
        fd.setGrossTotal(lineGrossTotal);

        // Since no discounts/expenses/taxes, Net = Gross
        fd.setLineNetRate(grossRate);
        fd.setLineNetTotal(lineGrossTotal);
        fd.setNetTotal(lineGrossTotal);

        // Quantity in units
        BigDecimal qtyByUnits = qty.multiply(unitsPerPack);
        fd.setQuantityByUnits(qtyByUnits);
        fd.setTotalQuantityByUnits(qtyByUnits);

        // Retail sale rate in unit is defined by the user via PBI
        fd.setRetailSaleRate(BigDecimal.valueOf(ph.getRetailRateInUnit()));

        // Optional zero fields to avoid nulls
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

        // Call final adjustment logic
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);

        // Update PBI and BI fields
        ph.setQty(qtyByUnits.doubleValue());
        ph.setQtyPacks(qty.doubleValue());
    }

    // ChatGPT contributed - Populate default rates when an item is selected
    public void populateRatesOnItemSelect() {
        BillItem bi = getCurrentBillItem();
        if (bi == null || bi.getItem() == null) {
            return;
        }

        PharmaceuticalBillItem ph = bi.getPharmaceuticalBillItem();
        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();

        if (bi.getItem() instanceof Ampp) {
            Ampp ampp = (Ampp) bi.getItem();
            Amp amp = ampp.getAmp();

            fd.setUnitsPerPack(BigDecimal.valueOf(bi.getItem().getDblValue()));

            Double retailRatePerUnit = pharmacyBean.getLastRetailRate(amp, sessionController.getDepartment());
            Double purchaseRatePerUnit = pharmacyBean.getLastPurchaseRate(amp, sessionController.getDepartment());
            Double costRatePerUnit = pharmacyBean.getLastCostRate(amp, sessionController.getDepartment());
            BigDecimal transferRate = determineTransferRate(amp);

            ph.setPurchaseRate(purchaseRatePerUnit * fd.getUnitsPerPack().doubleValue());
            ph.setPurchaseRatePack(purchaseRatePerUnit * fd.getUnitsPerPack().doubleValue());

            ph.setRetailRate(retailRatePerUnit * fd.getUnitsPerPack().doubleValue());
            ph.setRetailRatePack(retailRatePerUnit * fd.getUnitsPerPack().doubleValue());

            ph.setRetailPackValue(0);

            fd.setLineCostRate(BigDecimal.valueOf(costRatePerUnit).multiply(fd.getUnitsPerPack()));
            fd.setLineGrossRate(transferRate.multiply(fd.getUnitsPerPack()));

        } else if (bi.getItem() instanceof Vmpp) {
            Vmpp vmpp = (Vmpp) bi.getItem();
            Vmp vmp = vmpp.getVmp();

            fd.setUnitsPerPack(BigDecimal.valueOf(bi.getItem().getDblValue()));

            Double retailRatePerUnit = pharmacyBean.getLastRetailRate(vmp, sessionController.getDepartment());
            Double purchaseRatePerUnit = pharmacyBean.getLastPurchaseRate(vmp, sessionController.getDepartment());
            Double costRatePerUnit = pharmacyBean.getLastCostRate(vmp, sessionController.getDepartment());
            BigDecimal transferRate = determineTransferRate(vmp);

            ph.setPurchaseRate(purchaseRatePerUnit * fd.getUnitsPerPack().doubleValue());
            ph.setPurchaseRatePack(purchaseRatePerUnit * fd.getUnitsPerPack().doubleValue());

            ph.setRetailRate(retailRatePerUnit * fd.getUnitsPerPack().doubleValue());
            ph.setRetailRatePack(retailRatePerUnit * fd.getUnitsPerPack().doubleValue());

            ph.setRetailPackValue(0);

            fd.setLineCostRate(BigDecimal.valueOf(costRatePerUnit).multiply(fd.getUnitsPerPack()));
            fd.setLineGrossRate(transferRate.multiply(fd.getUnitsPerPack()));

        } else if (bi.getItem() instanceof Amp) {
            Amp amp = (Amp) bi.getItem();

            fd.setUnitsPerPack(BigDecimal.ONE);

            Double retailRatePerUnit = pharmacyBean.getLastRetailRate(amp, sessionController.getDepartment());
            Double purchaseRatePerUnit = pharmacyBean.getLastPurchaseRate(amp, sessionController.getDepartment());
            Double costRatePerUnit = pharmacyBean.getLastCostRate(amp, sessionController.getDepartment());
            BigDecimal transferRate = determineTransferRate(amp);

            ph.setPurchaseRate(purchaseRatePerUnit);
            ph.setPurchaseRatePack(purchaseRatePerUnit);

            ph.setRetailRate(retailRatePerUnit);
            ph.setRetailRatePack(retailRatePerUnit);

            ph.setRetailPackValue(0);

            fd.setLineCostRate(BigDecimal.valueOf(costRatePerUnit));
            fd.setLineGrossRate(transferRate);

        } else if (bi.getItem() instanceof Vmp) {
            Vmp vmp = (Vmp) bi.getItem();

            fd.setUnitsPerPack(BigDecimal.ONE);

            Double retailRatePerUnit = pharmacyBean.getLastRetailRate(vmp, sessionController.getDepartment());
            Double purchaseRatePerUnit = pharmacyBean.getLastPurchaseRate(vmp, sessionController.getDepartment());
            Double costRatePerUnit = pharmacyBean.getLastCostRate(vmp, sessionController.getDepartment());
            BigDecimal transferRate = determineTransferRate(vmp);

            ph.setPurchaseRate(purchaseRatePerUnit);
            ph.setPurchaseRatePack(purchaseRatePerUnit);

            ph.setRetailRate(retailRatePerUnit);
            ph.setRetailRatePack(retailRatePerUnit);

            ph.setRetailPackValue(0);

            fd.setLineCostRate(BigDecimal.valueOf(costRatePerUnit));
            fd.setLineGrossRate(transferRate);
        }

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

    // ************************************
    // Newly added helper methods
    // ************************************
    public void onCurrentQtyChange() {
        if (currentBillItem == null) {
            return;
        }

        BillItemFinanceDetails fd = currentBillItem.getBillItemFinanceDetails();
        if (fd == null) {
            return;
        }
        updateFinancials(fd);
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);
        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(getTransferRequestBillPre(), getBillItems());
    }

    public void onCurrentLineGrossRateChange() {
        if (currentBillItem == null) {
            return;
        }

        BillItemFinanceDetails fd = currentBillItem.getBillItemFinanceDetails();
        if (fd == null) {
            return;
        }
        updateFinancials(fd);
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

    public List<Department> getRecentToDepartments() {
        if (recentToDepartments == null) {
            String jpql = "select distinct b.toDepartment from Bill b "
                    + " where b.retired=false "
                    + " and b.billTypeAtomic=:bt "
                    + " and b.fromDepartment=:fd "
                    + " order by b.id desc";
            Map<String, Object> m = new HashMap<>();
            m.put("bt", BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
            m.put("fd", sessionController.getDepartment());
            recentToDepartments = departmentFacade.findByJpql(jpql, m, 10);
        }
        return recentToDepartments;
    }

    public String selectFromRecentDepartment(Department d) {
        if (d == null) {
            return "";
        }
        setToDepartment(d);
        return processTransferRequest();
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

}
