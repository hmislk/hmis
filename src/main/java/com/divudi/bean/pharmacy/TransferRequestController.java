/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.*;
import com.divudi.core.entity.*;
import com.divudi.core.util.BigDecimalUtil;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
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
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.service.BillService;

import java.text.DecimalFormat;
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
import java.util.Optional;
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

    public String navigateToCreateANewTransferRequest() {
        recreate();

        return "/pharmacy/pharmacy_transfer_request?faces-redirect=true";
    }

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

    public String fillHeaderDataOfTransferRequest(String s, Bill b) {
        if (b != null) {
            String filledHeader;

            String fromDepartment = b.getFromDepartment().getName();
            String fromInstitution = b.getFromDepartment().getInstitution().getName();
            String toDepartment = b.getToDepartment().getName();
            String toInstitution = b.getToDepartment().getInstitution().getName();
            String billId = b.getDeptId();
            String user = b.getCreater().getWebUserPerson().getName();
            String billDate = (b != null ? CommonFunctions.getDateFormat(b.getCreatedAt(), sessionController.getApplicationPreference().getLongDateTimeFormat()) : "");
            String billStatus = b.getStatus() == null ? "" : b.getStatus().toString();

            filledHeader = s.replace("{{from_dept}}", fromDepartment)
                    .replace("{{from_ins}}", fromInstitution)
                    .replace("{{to_dept}}", toDepartment)
                    .replace("{{to_ins}}", toInstitution)
                    .replace("{{bill_id}}", billId)
                    .replace("{{user}}", user)
                    .replace("{{bill_date}}", billDate)
                    .replace("{{bill_status}}", billStatus);

            return filledHeader;
        } else {
            return s;
        }
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
        recalculateTransferRequestBillTotals();

        currentBillItem = null;
    }

    public void onEdit(BillItem tmp) {
        updateFinancials(tmp.getBillItemFinanceDetails());
        recalculateTransferRequestBillTotals();
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
        if (preBillToCreateApprovedBill == null) {
            JsfUtil.addErrorMessage("No Pre Bill");
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

        newApprovedBill.setBillDate(new Date());
        newApprovedBill.setBillTime(new Date());
        //Always have to be the same as prebill
        newApprovedBill.setInsId(preBillToCreateApprovedBill.getInsId());
        newApprovedBill.setDeptId(preBillToCreateApprovedBill.getDeptId());

        newApprovedBill.setApproveAt(new Date());
        newApprovedBill.setApproveUser(sessionController.getLoggedUser());

        newApprovedBill.setChecked(true);
        newApprovedBill.setCheckedBy(sessionController.getLoggedUser());
        newApprovedBill.setCheckeAt(new Date());

        newApprovedBill.setCompleted(true);
        newApprovedBill.setCompletedBy(sessionController.getLoggedUser());
        newApprovedBill.setCompletedAt(new Date());

        if (newApprovedBill.getId() == null) {
            newApprovedBill.setCreater(sessionController.getLoggedUser());
            newApprovedBill.setCreatedAt(new Date());
            billFacade.create(newApprovedBill);
        } else {
            billFacade.edit(newApprovedBill);
        }

        for (BillItem newBillItem : transferRequestPreBillItems) {
            newBillItem.setBill(newApprovedBill);
            // Initialize remainingQty for new Transfer Requests
            newBillItem.setRemainingQty(newBillItem.getQty());
            if (newBillItem.getId() == null) {
                billItemFacade.create(newBillItem);
            } else {
                billItemFacade.edit(newBillItem);
            }
            newApprovedBill.getBillItems().add(newBillItem);
        }

        pharmacyCostingService.calculateBillTotalsFromItemsForTransferOuts(newApprovedBill, newApprovedBill.getBillItems());
        billFacade.edit(newApprovedBill);

        preBillToCreateApprovedBill.setForwardReferenceBill(newApprovedBill);

        preBillToCreateApprovedBill.setApproveUser(sessionController.getLoggedUser());
        preBillToCreateApprovedBill.setApproveAt(new Date());
        preBillToCreateApprovedBill.setReferenceBill(newApprovedBill);

        preBillToCreateApprovedBill.setCompleted(true);
        preBillToCreateApprovedBill.setCompletedAt(new Date());
        preBillToCreateApprovedBill.setCompletedBy(sessionController.getLoggedUser());

        billFacade.edit(preBillToCreateApprovedBill);
        newApprovedBill.setReferenceBill(preBillToCreateApprovedBill);

        billFacade.edit(newApprovedBill);
        return newApprovedBill;
    }

    @Deprecated
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
        } else {

            boolean useDeptInsFormat = configOptionApplicationController.getBooleanValueByKey(
                    "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
            boolean useInsFormat = configOptionApplicationController.getBooleanValueByKey(
                    "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Year + Yearly Number", false);

            String deptId;
            if (useDeptInsFormat) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
            } else if (configOptionApplicationController.getBooleanValueByKey(
                    "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
            } else if (useInsFormat) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
            } else {
                deptId = getBillNumberBean().institutionBillNumberGenerator(
                        getSessionController().getDepartment(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ);
            }

            String insId;
            if (useInsFormat) {
                insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST);
            } else if (useDeptInsFormat) {
                insId = deptId;
            } else {
                insId = getBillNumberBean().institutionBillNumberGenerator(
                        getSessionController().getInstitution(), BillType.PharmacyTransferRequest, BillClassType.BilledBill, BillNumberSuffix.PHTRQ);
            }

            getBill().setDeptId(deptId);
            getBill().setInsId(insId);
        }

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

        // Only generate bill numbers if they are blank
        if (getTransferRequestBillPre().getDeptId() == null || getTransferRequestBillPre().getDeptId().trim().isEmpty()
                || getTransferRequestBillPre().getInsId() == null || getTransferRequestBillPre().getInsId().trim().isEmpty()) {

            boolean useDeptInsFormat = configOptionApplicationController.getBooleanValueByKey(
                    "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
            boolean useInsFormat = configOptionApplicationController.getBooleanValueByKey(
                    "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Year + Yearly Number", false);

            String legacyRequestId = null;
            if (!useDeptInsFormat && !useInsFormat) {
                legacyRequestId = billNumberBean.departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(
                        getSessionController().getDepartment(),
                        getToDepartment(),
                        BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
            }

            String deptId;
            if (useDeptInsFormat) {
                deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
            } else if (configOptionApplicationController.getBooleanValueByKey(
                    "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
                deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
            } else if (useInsFormat) {
                deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
            } else {
                deptId = legacyRequestId;
            }

            String insId;
            if (useInsFormat) {
                insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
            } else if (useDeptInsFormat) {
                insId = deptId;
            } else {
                insId = legacyRequestId;
            }

            getTransferRequestBillPre().setDeptId(deptId);
            getTransferRequestBillPre().setInsId(insId);
        }
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
        calculateBillTotalsFromItemsForTransferRequests(getTransferRequestBillPre(), billItems);
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
        if (bill.getInstitution() == null) {
            bill.setInstitution(sessionController.getInstitution());
        }
        if (bill.getDepartment() == null) {
            bill.setDepartment(sessionController.getDepartment());
        }
        if (bill.getCreater() == null) {
            bill.setCreater(sessionController.getLoggedUser());
        }
        if (bill.getCreatedAt() == null) {
            bill.setCreatedAt(new Date());
        }
        billItems = new ArrayList<>();
        for (BillItem requestItemInPreBill : getTransferRequestBillPre().getBillItems()) {
            BillItem newBillItemInApprovedRequest = new BillItem();
            newBillItemInApprovedRequest.copy(requestItemInPreBill);
            newBillItemInApprovedRequest.setBill(bill);
            // Initialize remainingQty for new Transfer Requests
            newBillItemInApprovedRequest.setRemainingQty(newBillItemInApprovedRequest.getQty());
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

//        getTransferRequestBillPre().setApproveAt(new Date());
//        getTransferRequestBillPre().setApproveUser(sessionController.getLoggedUser());
//        getTransferRequestBillPre().setCompleted(true);
//        getTransferRequestBillPre().setCompletedAt(new Date());
//        getTransferRequestBillPre().setCompletedBy(sessionController.getLoggedUser());
        getBillFacade().edit(getTransferRequestBillPre());
        JsfUtil.addSuccessMessage("Transfer Request Succesfully Finalized");

        bill = getTransferRequestBillPre();

        printPreview = true;
    }

    // Commented out - No longer needed as approval is done via separate approval page
    // This method was called from pharmacy_transfer_request.xhtml Approve button which has been removed
//    public void approveTranserRequestPreBill() {
//        if (errorsPresent()) {
//            return;
//        }
//        saveTransferRequestPreBillAndBillItems();
//        getTransferRequestBillPre().setEditedAt(new Date());
//        getTransferRequestBillPre().setEditor(sessionController.getLoggedUser());
//
//        getTransferRequestBillPre().setCheckeAt(new Date());
//        getTransferRequestBillPre().setCheckedBy(sessionController.getLoggedUser());
//
//        getTransferRequestBillPre().setApproveAt(new Date());
//        getTransferRequestBillPre().setApproveUser(sessionController.getLoggedUser());
//
//        getTransferRequestBillPre().setCompleted(true);
//        getTransferRequestBillPre().setCompletedAt(new Date());
//        getTransferRequestBillPre().setCompletedBy(sessionController.getLoggedUser());
//
//        getBillFacade().edit(getTransferRequestBillPre());
//        JsfUtil.addSuccessMessage("Transfer Request Succesfully Finalized");
//
//        bill = createNewApprovedTransferRequestBill(
//                getTransferRequestBillPre(),
//                getTransferRequestBillPre().getBillItems(),
//                new BilledBill()
//        );
//
//        printPreview = true;
//    }

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
        recalculateTransferRequestBillTotals();

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
        recalculateTransferRequestBillTotals();
    }

    // ChatGPT contributed - Recalculate item totals when gross rate changes
    public void onLineGrossRateChange(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);
        recalculateTransferRequestBillTotals();
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
        recalculateTransferRequestBillTotals();
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
        recalculateTransferRequestBillTotals();
    }

    private void recalculateTransferRequestBillTotals() {
        if (transferRequestBillPre != null) {
            calculateBillTotalsFromItemsForTransferRequests(transferRequestBillPre, getBillItems());
        }
        if (bill != null) {
            calculateBillTotalsFromItemsForTransferRequests(bill, getBillItems());
        }
    }

    private void calculateBillTotalsFromItemsForTransferRequests(Bill billForRequest, List<BillItem> requestBillItems) {
        if (billForRequest == null) {
            return;
        }

        List<BillItem> itemsToProcess = requestBillItems != null ? requestBillItems : new ArrayList<>();

        int serialNo = 0;

        BigDecimal billDiscount = BigDecimal.valueOf(billForRequest.getDiscount());
        BigDecimal billExpense = BigDecimal.valueOf(billForRequest.getExpenseTotal());
        BigDecimal billTax = BigDecimal.valueOf(billForRequest.getTax());
        BigDecimal billCost = billDiscount.subtract(billExpense.add(billTax));

        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalLineExpenses = BigDecimal.ZERO;
        BigDecimal totalLineCosts = BigDecimal.ZERO;
        BigDecimal totalTaxLines = BigDecimal.ZERO;
        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;
        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotalSum = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotalSum = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (BillItem bi : itemsToProcess) {
            if (bi == null) {
                continue;
            }

            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

            if (pbi == null || f == null) {
                continue;
            }

            bi.setSearialNo(serialNo++);

            BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.valueOf(bi.getQty()));
            BigDecimal grossRate = Optional.ofNullable(f.getLineGrossRate()).orElse(BigDecimal.ZERO);
            BigDecimal netRate = Optional.ofNullable(f.getLineNetRate()).orElse(grossRate);

            if (f.getLineGrossTotal() == null || f.getLineGrossTotal().compareTo(BigDecimal.ZERO) == 0) {
                f.setLineGrossTotal(grossRate.multiply(qty));
            }

            if (f.getGrossTotal() == null || f.getGrossTotal().compareTo(BigDecimal.ZERO) == 0) {
                f.setGrossTotal(f.getLineGrossTotal());
            }

            if (f.getLineNetTotal() == null || f.getLineNetTotal().compareTo(BigDecimal.ZERO) == 0) {
                f.setLineNetTotal(f.getLineGrossTotal());
            }

            if (f.getNetTotal() == null || f.getNetTotal().compareTo(BigDecimal.ZERO) == 0) {
                f.setNetTotal(f.getLineNetTotal());
            }

            BigDecimal lineGrossTotal = Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO);
            BigDecimal lineNetTotal = Optional.ofNullable(f.getLineNetTotal()).orElse(lineGrossTotal);
            BigDecimal netTotalForItem = Optional.ofNullable(f.getNetTotal()).orElse(lineNetTotal);

            bi.setRate(netRate.doubleValue());
            bi.setNetRate(netRate.doubleValue());
            bi.setGrossValue(lineGrossTotal.doubleValue());
            bi.setNetValue(lineNetTotal.doubleValue());

            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal qtyTotal = qty.add(freeQty);

            BigDecimal costRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
            BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO);
            BigDecimal wholesaleRate = Optional.ofNullable(f.getWholesaleRate()).orElse(BigDecimal.ZERO);

            BigDecimal retailValue = retailRate.multiply(qtyTotal);
            BigDecimal wholesaleValue = wholesaleRate.multiply(qtyTotal);
            BigDecimal freeItemValue = costRate.multiply(freeQty);

            totalLineDiscounts = totalLineDiscounts.add(Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO));
            totalLineExpenses = totalLineExpenses.add(Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO));
            totalTaxLines = totalTaxLines.add(Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO));
            totalLineCosts = totalLineCosts.add(Optional.ofNullable(f.getLineCost()).orElse(BigDecimal.ZERO));
            totalFreeItemValue = totalFreeItemValue.add(freeItemValue);
            totalPurchase = totalPurchase.add(Optional.ofNullable(f.getGrossTotal()).orElse(lineGrossTotal));
            totalRetail = totalRetail.add(retailValue);
            totalWholesale = totalWholesale.add(wholesaleValue);
            totalQty = totalQty.add(qty);
            totalFreeQty = totalFreeQty.add(freeQty);
            totalQtyAtomic = totalQtyAtomic.add(Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO));
            totalFreeQtyAtomic = totalFreeQtyAtomic.add(Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO));
            grossTotal = grossTotal.add(lineNetTotal);
            lineGrossTotalSum = lineGrossTotalSum.add(lineGrossTotal);
            netTotal = netTotal.add(netTotalForItem);
            lineNetTotalSum = lineNetTotalSum.add(lineNetTotal);
            totalDiscount = totalDiscount.add(Optional.ofNullable(f.getTotalDiscount()).orElse(BigDecimal.ZERO));
            totalExpense = totalExpense.add(Optional.ofNullable(f.getTotalExpense()).orElse(BigDecimal.ZERO));
            totalCost = totalCost.add(Optional.ofNullable(f.getTotalCost()).orElse(BigDecimal.ZERO));
            totalTax = totalTax.add(Optional.ofNullable(f.getTotalTax()).orElse(BigDecimal.ZERO));

            if (pbi != null) {
                if (f.getValueAtPurchaseRate() != null) {
                    pbi.setPurchaseValue(f.getValueAtPurchaseRate().doubleValue());
                }
                if (f.getValueAtRetailRate() != null) {
                    pbi.setRetailValue(f.getValueAtRetailRate().doubleValue());
                }
            }
        }

        billForRequest.setTotal(BigDecimalUtil.valueOrZero(grossTotal).doubleValue());
        billForRequest.setNetTotal(BigDecimalUtil.valueOrZero(netTotal).doubleValue());
        billForRequest.setSaleValue(BigDecimalUtil.valueOrZero(totalRetail).doubleValue());

        BillFinanceDetails bfd = billForRequest.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(billForRequest);
            billForRequest.setBillFinanceDetails(bfd);
        }

        bfd.setBillDiscount(billDiscount);
        bfd.setBillExpense(billExpense);
        bfd.setBillTaxValue(billTax);
        bfd.setBillCostValue(billCost);
        bfd.setLineDiscount(totalLineDiscounts);
        bfd.setLineExpense(totalLineExpenses);
        bfd.setItemTaxValue(totalTaxLines);
        bfd.setLineCostValue(totalLineCosts);
        bfd.setTotalDiscount(totalDiscount);
        bfd.setTotalExpense(totalExpense);
        bfd.setTotalTaxValue(totalTax);

        BigDecimal sumCostFromItems = BigDecimal.ZERO;
        BigDecimal sumPurchaseFromItems = BigDecimal.ZERO;
        BigDecimal sumRetailFromItems = BigDecimal.ZERO;
        for (BillItem it : itemsToProcess) {
            if (it == null || it.getBillItemFinanceDetails() == null) {
                continue;
            }
            BillItemFinanceDetails fd = it.getBillItemFinanceDetails();
            sumCostFromItems = sumCostFromItems.add(Optional.ofNullable(fd.getValueAtCostRate()).orElse(BigDecimal.ZERO));
            sumPurchaseFromItems = sumPurchaseFromItems.add(Optional.ofNullable(fd.getValueAtPurchaseRate()).orElse(BigDecimal.ZERO));
            sumRetailFromItems = sumRetailFromItems.add(Optional.ofNullable(fd.getValueAtRetailRate()).orElse(BigDecimal.ZERO));

            if (it.getPharmaceuticalBillItem() != null) {
                if (fd.getValueAtPurchaseRate() != null) {
                    it.getPharmaceuticalBillItem().setPurchaseValue(fd.getValueAtPurchaseRate().doubleValue());
                }
                if (fd.getValueAtRetailRate() != null) {
                    it.getPharmaceuticalBillItem().setRetailValue(fd.getValueAtRetailRate().doubleValue());
                }
            }
        }

        bfd.setTotalCostValue(sumCostFromItems);
        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalPurchaseValue(sumPurchaseFromItems);
        bfd.setTotalRetailSaleValue(sumRetailFromItems);
        bfd.setTotalWholesaleValue(totalWholesale);
        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);
        bfd.setGrossTotal(grossTotal);
        bfd.setLineGrossTotal(lineGrossTotalSum);
        bfd.setNetTotal(netTotal);
        bfd.setLineNetTotal(lineNetTotalSum);
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
