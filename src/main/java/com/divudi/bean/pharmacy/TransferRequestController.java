/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;

import com.divudi.core.data.*;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.dto.ItemRatesDTO;
import com.divudi.core.data.dto.search.ItemDTO;
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
import com.divudi.bean.common.ItemController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.service.BillService;
import com.divudi.service.StockService;

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
import java.util.WeakHashMap;
import javax.annotation.PostConstruct;
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
    @EJB
    private StockService stockService;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
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
    @Inject
    private PageMetadataRegistry pageMetadataRegistry;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private Bill bill;
    private Bill transferRequestBillPre;
    // Bill id used for navigation from DTO-driven tables (issue #22567) that
    // only have the bill id available, not the full Bill entity.
    private Long billId;
    private Institution dealor;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    private boolean printPreview;
    private boolean showAllBillFormats = false;
    private Department toDepartment;
    private List<Department> recentToDepartments;
    private ItemDTO currentItemDto;
    private ItemRatesDTO currentItemRates;
    private List<String> cachedAvailableDeptTypesForDisplay;
    private List<String> cachedLoggedDeptTypesForDisplay;
    private List<String> cachedToDeptTypesForDisplay;
    // </editor-fold>

    public String navigateToCreateANewTransferRequest() {
        recreate();

        return "/pharmacy/pharmacy_transfer_request?faces-redirect=true";
    }

    public void recreate() {
        toDepartment = null;
        bill = null;
        currentBillItem = null;
        currentItemDto = null;
        currentItemRates = null;
        cachedAvailableDeptTypesForDisplay = null;
        cachedLoggedDeptTypesForDisplay = null;
        cachedToDeptTypesForDisplay = null;
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
    
    @Inject
    ItemController itemController;
    
    public List<ItemDTO> completeAmpAmppVmpVmppItemsForRequestingDepartment(String query) {
        DepartmentType typeFilter = getBill().getDepartmentType();
        return itemController.completeAmpAmppVmpVmppItemDtosForRequestingDepartment(query, toDepartment, typeFilter);
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

        // Auto-set department type on first item addition
        if (getBill().getDepartmentType() == null) {
            DepartmentType itemDeptType = getCurrentBillItem().getItem().getDepartmentType();
            if (itemDeptType != null) {
                getBill().setDepartmentType(itemDeptType);
            } else {
                // Default to Pharmacy type for items without department type
                getBill().setDepartmentType(DepartmentType.Pharmacy);
            }
        }

        // Validate item department type matches bill department type
        if (!validateItemDepartmentType(getCurrentBillItem().getItem())) {
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

        if (currentItemRates != null) {
            ph.setPurchaseRate(currentItemRates.getPurchaseRate());
            ph.setRetailRateInUnit(currentItemRates.getRetailRate());
        } else {
            ph.setPurchaseRate(getPharmacyBean().getLastPurchaseRate(item, getSessionController().getDepartment()));
            ph.setRetailRateInUnit(getPharmacyBean().getLastRetailRate(item, getSessionController().getDepartment()));
        }

        updateFinancials(fd);
        getBillItems().add(bi);
        recalculateTransferRequestBillTotals();

        currentBillItem = null;
        currentItemDto = null;
        currentItemRates = null;
    }

    public void onEdit(BillItem tmp) {
        updateFinancials(tmp.getBillItemFinanceDetails());
        recalculateTransferRequestBillTotals();
    }

    public void displayItemDetails(BillItem bi) {
        getPharmacyController().fillItemDetails(bi.getItem());
    }

    private final Map<BillItem, Double> availableQtyAtOrderingStoreCache = new WeakHashMap<>();

    public double getAvailableQtyAtOrderingStore(BillItem bi) {
        if (bi == null || bi.getItem() == null || getToDepartment() == null) {
            return 0.0;
        }
        return availableQtyAtOrderingStoreCache.computeIfAbsent(bi, this::calculateAvailableQtyAtOrderingStore);
    }

    private double calculateAvailableQtyAtOrderingStore(BillItem bi) {
        Item item = bi.getItem();
        double stock = stockService.findDepartmentStock(getToDepartment(), item);
        if ((item instanceof Ampp || item instanceof Vmpp) && item.getDblValue() > 0) {
            return stock / item.getDblValue();
        }
        return stock;
    }

    public boolean isAvailableQtyShortAtOrderingStore(BillItem bi) {
        return getAvailableQtyAtOrderingStore(bi) < bi.getQty();
    }

    public void saveBill() {
        if (getBill().getId() == null) {

            getBill().setInstitution(getSessionController().getInstitution());
            getBill().setDepartment(getSessionController().getDepartment());

            getBill().setToInstitution(getBill().getToDepartment().getInstitution());

            getBillFacade().create(getBill());
        }

    }

    // synchronized: defense in depth alongside navigateToApproveRequest() — serializes
    // the final persist step on this session-scoped bean so a racing double-submit
    // can't write the (already duplicated) billItems list twice.
    public synchronized void approveTransferRequestBill() {
        if (!isAuthorized("APPROVE_REQUEST", "PharmacyDisbursementRequestApproval")) {
            return;
        }
        // Check if the pre-bill is already approved to prevent a queued double-submit
        // (blocked on the synchronized lock above) from creating a second approved bill
        // once the first call has finished.
        if (transferRequestBillPre != null && transferRequestBillPre.getReferenceBill() != null) {
            JsfUtil.addErrorMessage("This transfer request is already approved");
            return;
        }
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

    // synchronized: same re-entrancy guard as approveTransferRequestBill()/
    // navigateToApproveRequest() above, applied at the actual persist step.
    public synchronized Bill createNewApprovedTransferRequestBill(Bill preBillToCreateApprovedBill, List<BillItem> transferRequestPreBillItems, Bill newApprovedBill) {
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
        // Bill.copy() above does not carry departmentType — stamp it explicitly so the
        // approved request bill doesn't lose the type the PRE bill already had (#22146).
        if (preBillToCreateApprovedBill.getDepartmentType() != null) {
            newApprovedBill.setDepartmentType(preBillToCreateApprovedBill.getDepartmentType());
        } else if (!transferRequestPreBillItems.isEmpty() && transferRequestPreBillItems.get(0).getItem() != null) {
            DepartmentType firstItemType = transferRequestPreBillItems.get(0).getItem().getDepartmentType();
            newApprovedBill.setDepartmentType(firstItemType != null ? firstItemType : DepartmentType.Pharmacy);
        } else {
            newApprovedBill.setDepartmentType(DepartmentType.Pharmacy);
        }

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

            // Fixed: Use cascade relationship - save only BillItem, PBI will be saved automatically
            if (b.getId() == null) {
                getBillItemFacade().create(b);
            } else {
                getBillItemFacade().edit(b);
            }

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
        getTransferRequestBillPre().setDepartmentType(getBill().getDepartmentType());
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

            // Fixed: Use cascade relationship - save only BillItem, PBI will be saved automatically
            if (b.getId() == null) {
                getBillItemFacade().create(b);
            } else {
                getBillItemFacade().edit(b);
            }

            if (b.getId() == null || !getTransferRequestBillPre().getBillItems().contains(b)) {
                getTransferRequestBillPre().getBillItems().add(b);
            }
        }
        getTransferRequestBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_PRE);
        LOGGER.log(Level.FINE, "Finalizing transfer request with {0} items", getBillItems().size());
        getBillFacade().edit(getTransferRequestBillPre());
    }

    public void saveTranserRequestPreBill() {
        if (!isAuthorized("REQUEST", "PharmacyDisbursementRequest")) {
            return;
        }
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
        // Set the bill to the loaded bill so getBill() returns the existing bill with its department type
        bill = transferRequestBillPre;
        return "/pharmacy/pharmacy_transfer_request?faces-redirect=true";
    }
    
    

    // synchronized: the Approve Request button on the transfer-request-list-to-approve
    // page has no double-click guard. This method clears and repopulates the
    // session-scoped billItems field from the pre-bill's items; a double-click raced
    // two concurrent calls through this clear-then-repopulate step, leaving billItems
    // holding every line twice (issue: duplicate items on TREQ/RH/GRO/26/00074, same
    // bug class as #21417/#21815/PR #22101, tracked generically under #22102).
    public synchronized String navigateToApproveRequest() {
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

    /**
     * Navigation helper for DTO-driven tables that only have the bill id
     * (not the full entity) available.
     */
    public String navigateToEditRequestById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        transferRequestBillPre = billFacade.find(billId);
        return navigateToEditRequest();
    }

    // synchronized for the same double-click defense-in-depth reason as navigateToApproveRequest()
    public synchronized String navigateToApproveRequestById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        transferRequestBillPre = billFacade.find(billId);
        return navigateToApproveRequest();
    }

    public String navigateToViewApprovedRequestById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return "";
        }
        bill = billFacade.find(billId);
        printPreview = true;
        return "/pharmacy/pharmacy_transfer_request_approval?faces-redirect=true";
    }

    public void finalizeTranserRequestPreBill() {
        if (!isAuthorized("FINALIZE_REQUEST", "PharmacyDisbursementFinalizeRequest")) {
            return;
        }
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
        String jpql = "select bi from BillItem bi "
                + "join fetch bi.item "
                + "left join fetch bi.billItemFinanceDetails "
                + "where bi.bill=:bill and bi.retired=false";
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

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
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

    /**
     * Handles changes to the toDepartment selection.
     * Validates that the current department type selection is still valid
     * for the intersection of logged department and toDepartment.
     * Resets department type to null if it's no longer valid.
     */
    public void handleToDepartmentChange() {
        if (toDepartment == null) {
            return;
        }
        cachedAvailableDeptTypesForDisplay = null;
        cachedToDeptTypesForDisplay = null;

        if (bill != null && bill.getDepartmentType() != null) {
            List<DepartmentType> validTypes = getAvailableDepartmentTypesForTransfer();
            if (!validTypes.contains(bill.getDepartmentType())) {
                bill.setDepartmentType(null);
                JsfUtil.addErrorMessage("Department type reset. The previously selected department type is not supported by " +
                    toDepartment.getName() + ". Please select a valid department type.");
            }
        }
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

    public void populateRatesOnItemSelect() {
        if (currentItemDto == null || currentItemDto.getId() == null) {
            return;
        }

        // Load the full correctly-typed entity (Amp/Ampp/Vmp/Vmpp) once
        Item item = itemFacade.find(currentItemDto.getId());
        if (item == null) {
            return;
        }
        getCurrentBillItem().setItem(item);

        // Fetch all three rates in a single DB query using the rate-lookup item
        Item rateItem = itemFacade.getReference(currentItemDto.getRateItemId());
        currentItemRates = pharmacyBean.getLastRatesForItem(rateItem, sessionController.getDepartment());

        BillItem bi = getCurrentBillItem();
        PharmaceuticalBillItem ph = bi.getPharmaceuticalBillItem();
        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();

        // Pack size: use DTO value (>0) or default to 1 for unit items
        double rawDblValue = currentItemDto.getDblValue() != null ? currentItemDto.getDblValue() : 0.0;
        boolean isPack = "Ampp".equals(currentItemDto.getItemTypeName()) || "Vmpp".equals(currentItemDto.getItemTypeName());
        BigDecimal unitsPerPack = (isPack && rawDblValue > 0) ? BigDecimal.valueOf(rawDblValue) : BigDecimal.ONE;
        fd.setUnitsPerPack(unitsPerPack);

        BigDecimal transferRate = determineTransferRateFromRates(currentItemRates);

        if (isPack) {
            ph.setPurchaseRate(currentItemRates.getPurchaseRate() * unitsPerPack.doubleValue());
            ph.setPurchaseRatePack(currentItemRates.getPurchaseRate() * unitsPerPack.doubleValue());
            ph.setRetailRate(currentItemRates.getRetailRate() * unitsPerPack.doubleValue());
            ph.setRetailRatePack(currentItemRates.getRetailRate() * unitsPerPack.doubleValue());
            fd.setLineCostRate(BigDecimal.valueOf(currentItemRates.getCostRate()).multiply(unitsPerPack));
            fd.setLineGrossRate(transferRate.multiply(unitsPerPack));
        } else {
            ph.setPurchaseRate(currentItemRates.getPurchaseRate());
            ph.setPurchaseRatePack(currentItemRates.getPurchaseRate());
            ph.setRetailRate(currentItemRates.getRetailRate());
            ph.setRetailRatePack(currentItemRates.getRetailRate());
            fd.setLineCostRate(BigDecimal.valueOf(currentItemRates.getCostRate()));
            fd.setLineGrossRate(transferRate);
        }
        ph.setRetailPackValue(0);

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);
        recalculateTransferRequestBillTotals();
    }

    private BigDecimal determineTransferRateFromRates(ItemRatesDTO rates) {
        boolean byPurchase = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean byCost = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
        if (byPurchase) {
            return BigDecimal.valueOf(rates.getPurchaseRate());
        } else if (byCost) {
            return BigDecimal.valueOf(rates.getCostRate());
        } else {
            return BigDecimal.valueOf(rates.getRetailRate());
        }
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


    private boolean validateItemDepartmentType(Item item) {
        if (getBill().getDepartmentType() == null) return true;

        DepartmentType itemDeptType = item.getDepartmentType();
        DepartmentType billDeptType = getBill().getDepartmentType();

        // For items without department type, treat as Pharmacy
        if (itemDeptType == null) {
            itemDeptType = DepartmentType.Pharmacy;
        }

        // Check if item type matches bill type
        if (!itemDeptType.equals(billDeptType)) {
            JsfUtil.addErrorMessage("Cannot add items from different department types. " +
                "Transfer is set for " + billDeptType.getLabel() +
                " items, but you are trying to add a " + itemDeptType.getLabel() + " item.");
            return false;
        }

        return true;
    }

    public boolean isDepartmentTypeLocked() {
        return billItems != null && !billItems.isEmpty();
    }

    public void changeDepartmentType() {
        // Reset items if department type is changed manually
        if (billItems != null && !billItems.isEmpty()) {
            JsfUtil.addErrorMessage("Cannot change department type when items are already added");
            return;
        }

        // Clear existing items and reset
        billItems = new ArrayList<>();
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
            String jpql = "select distinct d from Bill b "
                    + "join b.toDepartment d "
                    + "left join fetch d.institution "
                    + "where b.retired=false "
                    + "and b.billTypeAtomic=:bt "
                    + "and b.fromDepartment=:fd "
                    + "and d.retired=false "
                    + "and d.inactive=false "
                    + "order by b.id desc";
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

    /**
     * Get available department types for the current target department for display purposes
     * @return List of department type names that are enabled for pharmacy transactions
     */
    /**
     * Gets the intersection of department types supported by BOTH:
     * 1. The logged user's department (from session)
     * 2. The toDepartment (requesting department)
     *
     * This ensures users can only select department types that are valid
     * for BOTH departments involved in the transfer.
     *
     * @return List of DepartmentType objects representing the intersection
     */
    public List<DepartmentType> getAvailableDepartmentTypesForTransfer() {
        List<DepartmentType> intersection = new ArrayList<>();

        if (toDepartment == null || sessionController.getDepartment() == null) {
            return intersection;
        }

        // Get logged department's supported types
        List<DepartmentType> loggedDeptTypes =
            sessionController.getAvailableDepartmentTypesForPharmacyTransactions();

        // Get toDepartment's supported types
        List<DepartmentType> toDeptTypes =
            getAvailableDepartmentTypesForToDepartment();

        // Calculate intersection
        for (DepartmentType dt : loggedDeptTypes) {
            if (toDeptTypes.contains(dt)) {
                intersection.add(dt);
            }
        }

        return intersection;
    }

    /**
     * Gets the department types supported by the toDepartment.
     * Similar to SessionController logic but for the target department.
     *
     * @return List of DepartmentType objects supported by toDepartment
     */
    private List<DepartmentType> getAvailableDepartmentTypesForToDepartment() {
        List<DepartmentType> types = new ArrayList<>();

        if (toDepartment == null) {
            return types;
        }

        // Check each department type
        for (DepartmentType depType : DepartmentType.values()) {
            if (isDepartmentTypeAllowedForToDepartment(depType)) {
                types.add(depType);
            }
        }

        return types;
    }

    /**
     * Checks if a specific department type is allowed for toDepartment.
     * Mirrors the logic in SessionController for consistency.
     *
     * @param departmentType The department type to check
     * @return true if allowed, false otherwise
     */
    private boolean isDepartmentTypeAllowedForToDepartment(DepartmentType departmentType) {
        if (toDepartment == null || departmentType == null) {
            return false;
        }

        String configKey = "Allow " + departmentType.getLabel() +
                           " Items In Pharmacy Transactions for " +
                           toDepartment.getName();

        // Default values (matching SessionController logic)
        // Pharmacy and Store default to true, others default to false
        boolean defaultValue = (departmentType == DepartmentType.Pharmacy ||
                               departmentType == DepartmentType.Store);

        return configOptionApplicationController.getBooleanValueByKey(configKey, defaultValue);
    }

    /**
     * Get available department types for the current target department for display purposes.
     * Now returns the intersection of logged department and toDepartment supported types.
     *
     * @return List of department type names that are enabled for pharmacy transactions
     */
    public List<String> getAvailableDepartmentTypesForDisplay() {
        if (toDepartment == null) {
            return new ArrayList<>();
        }
        if (cachedAvailableDeptTypesForDisplay == null) {
            cachedAvailableDeptTypesForDisplay = new ArrayList<>();
            for (DepartmentType dt : getAvailableDepartmentTypesForTransfer()) {
                cachedAvailableDeptTypesForDisplay.add(dt.getLabel());
            }
        }
        return cachedAvailableDeptTypesForDisplay;
    }

    public List<String> getLoggedDepartmentSupportedTypesForDisplay() {
        if (sessionController.getDepartment() == null) {
            return new ArrayList<>();
        }
        if (cachedLoggedDeptTypesForDisplay == null) {
            cachedLoggedDeptTypesForDisplay = new ArrayList<>();
            for (DepartmentType dt : sessionController.getAvailableDepartmentTypesForPharmacyTransactions()) {
                cachedLoggedDeptTypesForDisplay.add(dt.getLabel());
            }
        }
        return cachedLoggedDeptTypesForDisplay;
    }

    public List<String> getToDepartmentSupportedTypesForDisplay() {
        if (toDepartment == null) {
            return new ArrayList<>();
        }
        if (cachedToDeptTypesForDisplay == null) {
            cachedToDeptTypesForDisplay = new ArrayList<>();
            for (DepartmentType dt : getAvailableDepartmentTypesForToDepartment()) {
                cachedToDeptTypesForDisplay.add(dt.getLabel());
            }
        }
        return cachedToDeptTypesForDisplay;
    }

    public ItemDTO getCurrentItemDto() {
        return currentItemDto;
    }

    public void setCurrentItemDto(ItemDTO currentItemDto) {
        this.currentItemDto = currentItemDto;
    }

    public ItemRatesDTO getCurrentItemRates() {
        return currentItemRates;
    }

    public void setCurrentItemRates(ItemRatesDTO currentItemRates) {
        this.currentItemRates = currentItemRates;
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

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        // Register pharmacy_transfer_request.xhtml
        PageMetadata requestMetadata = new PageMetadata();
        requestMetadata.setPagePath("pharmacy/pharmacy_transfer_request");
        requestMetadata.setPageName("Pharmacy Transfer Request");
        requestMetadata.setDescription("Create and manage pharmacy transfer requests between departments");
        requestMetadata.setControllerClass("TransferRequestController");

        // Configuration Options
        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Stock Request - Show Rate and Value",
            "Controls visibility of rate and value fields in transfer request forms",
            "Lines 169, 179, 251, 258, 292-294 (XHTML): Rate/value input fields and display",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer Request Receipt is A4",
            "Uses A4 paper format for transfer request receipts",
            "Line 347 (XHTML): Receipt format selection",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer Request Receipt is Custom 1",
            "Uses Custom Format 1 for transfer request receipts",
            "Line 351 (XHTML): Receipt format selection",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer Request Receipt is Custom 2",
            "Uses Custom Format 2 for transfer request receipts",
            "Line 356 (XHTML): Receipt format selection",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Department Code + Institution Code + Year + Yearly Number",
            "Bill numbering format: Prefix-DeptCode-InstCode-Year-Number",
            "Lines 421, 531 (Controller): Bill number generation logic",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Year + Yearly Number",
            "Bill numbering format: Prefix-InstCode-Year-Number",
            "Lines 423, 533 (Controller): Bill number generation logic",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Department Code + Year + Yearly Number",
            "Bill numbering format: Prefix-InstCode-DeptCode-Year-Number",
            "Lines 430, 548 (Controller): Bill number generation logic",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Suffix for PHARMACY_TRANSFER_REQUEST",
            "Custom suffix to append to pharmacy transfer request bill numbers (used by BillNumberGenerator methods)",
            "Lines 436, 440, 443 (Controller): Bill number generation method calls",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer is by Purchase Rate",
            "Uses purchase rate for transfer pricing calculations",
            "Line 1328 (Controller): Transfer rate determination",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer is by Cost Rate",
            "Uses cost rate for transfer pricing calculations",
            "Line 1329 (Controller): Transfer rate determination",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer is by Retail Rate",
            "Uses retail rate for transfer pricing calculations",
            "Line 1330 (Controller): Transfer rate determination",
            OptionScope.APPLICATION
        ));

        // Department Type Configuration Options - Used by ItemController.getAvailableDepartmentTypesForPharmacyTransactions()
        // These are APPLICATION-scoped with department names embedded in keys
        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Allow Pharmacy Items In Pharmacy Transactions for [Department Name]",
            "Allows pharmacy items to be included in transfer requests for the specific department",
            "ItemController.getAvailableDepartmentTypesForPharmacyTransactions(): Department type filtering for item autocomplete",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Allow Lab Items In Pharmacy Transactions for [Department Name]",
            "Allows lab items to be included in transfer requests for the specific department",
            "ItemController.getAvailableDepartmentTypesForPharmacyTransactions(): Department type filtering for item autocomplete",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Allow Store Items In Pharmacy Transactions for [Department Name]",
            "Allows store items to be included in transfer requests for the specific department",
            "ItemController.getAvailableDepartmentTypesForPharmacyTransactions(): Department type filtering for item autocomplete",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Allow Etu Items In Pharmacy Transactions for [Department Name]",
            "Allows ETU (Emergency Treatment Unit) items to be included in transfer requests for the specific department",
            "ItemController.getAvailableDepartmentTypesForPharmacyTransactions(): Department type filtering for item autocomplete",
            OptionScope.APPLICATION
        ));

        requestMetadata.addConfigOption(new ConfigOptionInfo(
            "Allow Theatre Items In Pharmacy Transactions for [Department Name]",
            "Allows theatre/operating room items to be included in transfer requests for the specific department",
            "ItemController.getAvailableDepartmentTypesForPharmacyTransactions(): Department type filtering for item autocomplete",
            OptionScope.APPLICATION
        ));

        // Privileges
        requestMetadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to configuration interface",
            "Config button visibility"
        ));

        requestMetadata.addPrivilege(new PrivilegeInfo(
            "StockRequestViewRates",
            "View rate and value information in stock requests",
            "Lines 169, 179, 251, 258, 292-294 (XHTML): Rate and value fields visibility"
        ));

        requestMetadata.addPrivilege(new PrivilegeInfo(
            "ChangeReceiptPrintingPaperTypes",
            "Access to receipt printing configuration settings",
            "Line 319 (XHTML): Settings button visibility"
        ));

        pageMetadataRegistry.registerPage(requestMetadata);

        // Register pharmacy_transfer_request_approval.xhtml
        PageMetadata approvalMetadata = new PageMetadata();
        approvalMetadata.setPagePath("pharmacy/pharmacy_transfer_request_approval");
        approvalMetadata.setPageName("Pharmacy Transfer Request Approval");
        approvalMetadata.setDescription("Approve transfer requests from other departments");
        approvalMetadata.setControllerClass("TransferRequestController");

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer Request Receipt is A4",
            "Uses A4 paper format for transfer request receipts",
            "Line 174 (XHTML): Receipt format selection",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer Request Receipt is Custom 1",
            "Uses Custom Format 1 for transfer request receipts",
            "Line 178 (XHTML): Receipt format selection",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer Request Receipt is Custom 2",
            "Uses Custom Format 2 for transfer request receipts",
            "Line 183 (XHTML): Receipt format selection",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Department Code + Institution Code + Year + Yearly Number",
            "Bill numbering format: Prefix-DeptCode-InstCode-Year-Number (inherited from original request)",
            "Lines 328-329 (Controller): Bill number inherited from pre-bill created with this strategy",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Year + Yearly Number",
            "Bill numbering format: Prefix-InstCode-Year-Number (inherited from original request)",
            "Lines 328-329 (Controller): Bill number inherited from pre-bill created with this strategy",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Transfer Request - Prefix + Institution Code + Department Code + Year + Yearly Number",
            "Bill numbering format: Prefix-InstCode-DeptCode-Year-Number (inherited from original request)",
            "Lines 328-329 (Controller): Bill number inherited from pre-bill created with this strategy",
            OptionScope.APPLICATION
        ));

        // Note: Bill Number Suffix already registered in pharmacy_transfer_request metadata
        // approvalMetadata.addConfigOption(new ConfigOptionInfo(
        //     "Bill Number Suffix for PHARMACY_TRANSFER_REQUEST",
        //     "Custom suffix to append to pharmacy transfer request bill numbers (inherited from pre-bill)",
        //     "Lines 328-329 (Controller): Bill number copied from pre-bill",
        //     OptionScope.APPLICATION
        // ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer is by Purchase Rate",
            "Uses purchase rate for transfer pricing calculations when editing transfer rates",
            "Line 1335 (Controller): Transfer rate determination",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer is by Cost Rate",
            "Uses cost rate for transfer pricing calculations when editing transfer rates",
            "Line 1336 (Controller): Transfer rate determination",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Transfer is by Retail Rate",
            "Uses retail rate for transfer pricing calculations when editing transfer rates",
            "Line 1337 (Controller): Transfer rate determination",
            OptionScope.APPLICATION
        ));

        approvalMetadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to configuration interface",
            "Config button visibility"
        ));

        approvalMetadata.addPrivilege(new PrivilegeInfo(
            "ChangeReceiptPrintingPaperTypes",
            "Access to receipt printing configuration settings",
            "Line 150 (XHTML): Settings button visibility"
        ));

        pageMetadataRegistry.registerPage(approvalMetadata);

        // Register pharmacy_transfer_request_list_to_finalize.xhtml
        PageMetadata finalizeListMetadata = new PageMetadata();
        finalizeListMetadata.setPagePath("pharmacy/pharmacy_transfer_request_list_to_finalize");
        finalizeListMetadata.setPageName("Pharmacy Transfer Requests to Finalize");
        finalizeListMetadata.setDescription("List of saved transfer requests that need to be finalized");
        finalizeListMetadata.setControllerClass("SearchController");

        finalizeListMetadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to configuration interface",
            "Config button visibility"
        ));

        pageMetadataRegistry.registerPage(finalizeListMetadata);

        // Register pharmacy_transfer_request_list_to_approve.xhtml
        PageMetadata approveListMetadata = new PageMetadata();
        approveListMetadata.setPagePath("pharmacy/pharmacy_transfer_request_list_to_approve");
        approveListMetadata.setPageName("Pharmacy Transfer Requests to Approve");
        approveListMetadata.setDescription("List of finalized transfer requests awaiting approval");
        approveListMetadata.setControllerClass("SearchController");

        approveListMetadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to configuration interface",
            "Config button visibility"
        ));

        pageMetadataRegistry.registerPage(approveListMetadata);

        // Register pharmacy_transfer_request_list.xhtml
        PageMetadata requestListMetadata = new PageMetadata();
        requestListMetadata.setPagePath("pharmacy/pharmacy_transfer_request_list");
        requestListMetadata.setPageName("Pharmacy Transfer Request List");
        requestListMetadata.setDescription("List of approved transfer requests ready for issue");
        requestListMetadata.setControllerClass("SearchController");

        requestListMetadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to configuration interface",
            "Config button visibility"
        ));

        pageMetadataRegistry.registerPage(requestListMetadata);
    }

    /**
     * Authorization helper method to check Pharmacy Transfer Request
     * privileges and audit denied access
     *
     * @param action The action being attempted (e.g. REQUEST, FINALIZE_REQUEST, APPROVE_REQUEST)
     * @param requiredPrivilege The specific privilege required
     * @return true if authorized, false if not
     */
    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null, billId={1}",
                    new Object[]{action, bill != null ? bill.getId() : "null"});
            return false;
        }

        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            // Audit denied access attempt
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billId = bill != null ? bill.getId() : null;

            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized Pharmacy Transfer Request access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});

            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " transfer requests.");
            return false;
        }

        return true;
    }

}
