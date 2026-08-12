/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.dto.pharmacy.GrnApproveLineData;
import com.divudi.core.data.dto.pharmacy.GrnApproveRequest;
import com.divudi.core.data.dto.pharmacy.GrnLineData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.BigDecimalUtil;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.pharmacy.GrnApprovingNativeSqlService;
import com.divudi.service.pharmacy.GrnCostingNativeSqlService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;

/**
 * Native-SQL "Manage Costing = true" GRN (Goods Received Note) controller.
 * <p>
 * Owns the in-memory GRN bill exactly like {@link GrnCostingController}'s
 * "WithSaveApprove" flow (the only flow ported here -- the older two-bill
 * request()/settle()/saveBill() design is intentionally NOT touched or
 * referenced), but persists via the two native-SQL services instead of JPA
 * cascade saves: {@link GrnCostingNativeSqlService} for Create/Save/Finalize,
 * and {@link GrnApprovingNativeSqlService} for the single transactional
 * Approve call. All calculation/validation methods below are ported as close
 * to verbatim as possible from {@code GrnCostingController} -- same variable
 * names, same BigDecimal scales/rounding -- and operate purely on the
 * in-memory entity graph with JPQL reads only; no native SQL is issued from
 * this class directly.
 * <p>
 * Directly modeled on {@link PurchaseOrderRequestNativeSqlController} for CDI
 * conventions, the {@code synchronized} double-click guard on
 * finalize/approve (issue #22194 / PO issue #21417), and the lazy
 * {@code getCurrentBill()}-style rebuild pattern (here: {@link #getApproveBill()}
 * / {@link #getCurrentGrnBillPre()}).
 * Related issue: #22874
 */
@Named("grnCostingNativeSqlController")
@SessionScoped
public class GrnCostingNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(GrnCostingNativeSqlController.class.getName());

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private PageMetadataRegistry pageMetadataRegistry;
    /**
     * Used only by {@link #navigateToLegacyView()} to hand the in-memory bill
     * over to the original entity-based page's bean -- see that method's
     * Javadoc for why a direct bean-to-bean call is used instead of a
     * request-parameter round trip.
     */
    @Inject
    private GrnCostingController grnCostingController;

    @EJB
    private GrnCostingNativeSqlService grnCostingNativeSqlService;
    @EJB
    private GrnApprovingNativeSqlService grnApprovingNativeSqlService;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    // ------------------------------------------------------------------
    // Session state (mirrors GrnCostingController's field set)
    // ------------------------------------------------------------------
    private Bill approveBill;
    private Bill currentGrnBillPre;
    private boolean printPreview;
    private List<BillItem> selectedBillItems;
    private SearchKeyword searchKeyword;
    private String txtSearch;
    private double insTotal;
    private double difference;
    private Institution fromInstitution;
    private Institution referenceInstitution;
    private BillItem currentExpense;
    private Item freeItemToAdd;

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    // ==================================================================
    // Page metadata registration
    // ==================================================================
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata(
                "pharmacy/pharmacy_grn_costing_native",
                "Pharmacy GRN Costing (Native)",
                "Receive, cost, finalize and approve Goods Received Notes (GRN) against purchase orders using the native-SQL write path",
                "GrnCostingNativeSqlController"
        );

        metadata.addConfigOption(new ConfigOptionInfo(
                "Manage Costing",
                "Enables full landed-cost tracking (batch costRate/valueAtCostRate) when a GRN is approved",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Generate Payments for GRN, GRN Returns, Direct Purchase, and Direct Purchase Returns",
                "Automatically creates a Payment record when a GRN is approved",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Bill Number Suffix for PHARMACY_GRN",
                "Custom suffix for approved GRN bill numbers (default: GRN)",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Bill Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number",
                "Bill numbering format: Prefix-InstCode-DeptCode-Year-Number",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Bill Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Year + Yearly Number and Yearly Number",
                "Bill numbering format: Prefix-InstCode-Year-Number (institution-wide)",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Institution Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Year + Yearly Number and Yearly Number",
                "Controls separate institution-wide number generation for the insId field",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Purchase Value Includes Free Items",
                "Controls whether free-quantity items are included when computing purchase/cost values",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "GRN - Allow Free Items Not in Purchase Order",
                "Allows adding ad-hoc free-quantity lines to a GRN that were not on the source purchase order",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Enable Free Quantity Validation in GRN",
                "When enabled, blocks a GRN line's free quantity from exceeding the purchase order's ordered free quantity",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Show Profit Percentage in GRN",
                "Shows or hides the computed profit-margin percentage column on the GRN costing page",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Pharmacy Purchase - Quantity Must Be Integer",
                "Validates that quantity and free quantity values are whole numbers (no decimals)",
                OptionScope.INSTITUTION
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "GoodsRecipt",
                "Permission to receive goods against purchase orders",
                "Controls access to the GRN costing page and the Purchase Order receive list"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PharmacyGrnSave",
                "Permission to save a draft GRN",
                "Controls access to the Save button"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PharmacyGrnFinalize",
                "Permission to finalize a GRN",
                "Controls access to the Finalize button"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PharmacyGrnApprove",
                "Permission to approve a GRN (adds stock and creates the payment)",
                "Controls access to the Approve button"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "Admin",
                "Administrative access to configuration interface",
                "Controls visibility of the Config button"
        ));

        pageMetadataRegistry.registerPage(metadata);
    }

    // ==================================================================
    // Navigation
    // ==================================================================
    public void clear() {
        currentGrnBillPre = null;
        printPreview = false;
        currentExpense = null;
        difference = 0;
        insTotal = 0;
    }

    public String navigateToResiveCostingWithSaveApprove() {
        // Check for billId parameter (from DTO-based receive list)
        String billIdParam = JsfUtil.getRequestParameter("billId");
        if (billIdParam != null && !billIdParam.isEmpty()) {
            try {
                Long billId = Long.parseLong(billIdParam);
                Bill bill = billFacade.find(billId);
                if (bill != null) {
                    setApproveBill(bill);
                } else {
                    JsfUtil.addErrorMessage("Purchase Order not found");
                    return "";
                }
            } catch (NumberFormatException e) {
                JsfUtil.addErrorMessage("Invalid Bill ID");
                return "";
            }
        }

        // Guard against orphan PRE bills (issue #21579).
        if (getApproveBill() != null && getApproveBill().getId() != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("po", getApproveBill());
            params.put("type", BillTypeAtomic.PHARMACY_GRN_PRE);
            long orphanCount = billFacade.findLongByJpql(
                    "SELECT COUNT(b) FROM Bill b WHERE b.referenceBill = :po "
                    + "AND b.billTypeAtomic = :type AND b.retired = false AND b.cancelled = false",
                    params, TemporalType.TIMESTAMP);
            if (orphanCount > 0) {
                JsfUtil.addErrorMessage("There is already an unapproved GRN for this purchase order. Please approve or cancel the existing GRN before creating a new one.");
                return "";
            }
        }

        clear();
        setCurrentExpense(null);

        // Prepare bill and items without saving -- in-memory only, no persistence.
        setFromInstitution(getApproveBill().getToInstitution());
        setReferenceInstitution(sessionController.getLoggedUser().getInstitution());

        getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());

        generateBillComponent();

        getCurrentGrnBillPre().setPaymentMethod(getApproveBill().getPaymentMethod());
        getCurrentGrnBillPre().setCreditDuration(getApproveBill().getCreditDuration());

        if (getApproveBill() != null) {
            getCurrentGrnBillPre().setDiscount(getApproveBill().getDiscount());
            getCurrentGrnBillPre().setDepartmentType(getApproveBill().getDepartmentType());
        }

        if (getBillItems() != null && !getBillItems().isEmpty()) {
            ensureBillDiscountSynchronization();
            calculateBillTotalsFromItems();
            distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
            recalculateProfitMarginsForAllItems();
            calDifference();
        }

        return "/pharmacy/pharmacy_grn_costing_native?faces-redirect=true";
    }

    public String navigateToEditGrnCosting() {
        Bill savedBill = getCurrentGrnBillPre();
        clear();
        setCurrentGrnBillPre(savedBill);
        setCurrentExpense(null);

        if (getCurrentGrnBillPre().getId() != null) {
            String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId ORDER BY bi.searialNo";
            Map<String, Object> params = new HashMap<>();
            params.put("billId", getCurrentGrnBillPre().getId());
            List<BillItem> loadedItems = billItemFacade.findByJpql(jpql, params);
            getCurrentGrnBillPre().setBillItems(loadedItems);

            String expenseJpql = "SELECT be FROM BillItem be WHERE be.expenseBill.id = :billId AND be.retired = false ORDER BY be.searialNo";
            List<BillItem> loadedExpenses = billItemFacade.findByJpql(expenseJpql, params);
            getCurrentGrnBillPre().setBillExpenses(loadedExpenses);
        }

        setFromInstitution(getCurrentGrnBillPre().getFromInstitution());

        if (getBillItems() != null && !getBillItems().isEmpty()) {
            deduplicateBillExpensesInMemory();

            for (BillItem bi : getBillItems()) {
                if (bi.getBillItemFinanceDetails() != null) {
                    recalculateFinancialsBeforeAddingBillItem(bi.getBillItemFinanceDetails());
                }
            }

            recalculateExpenseTotals();
            calculateBillTotalsFromItems();
            ensureBillDiscountSynchronization();
            distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
            calDifference();
        }

        return "/pharmacy/pharmacy_grn_costing_native?faces-redirect=true";
    }

    /**
     * Convenience escape hatch to the original entity-based GRN costing page,
     * mirroring the "Legacy View" link on
     * {@code pharmacy_purhcase_order_request_native.xhtml}.
     * <p>
     * Unlike that page's Legacy View button -- a bare navigation string, since
     * the legacy {@code PurchaseOrderRequestController} page always starts a
     * fresh draft -- GRN's legacy page ({@code pharmacy_grn_costing_with_save_approve.xhtml})
     * is backed by a DIFFERENT session-scoped bean ({@link GrnCostingController})
     * that has no GET-param based route for loading an EXISTING saved GRN by
     * id: its own {@code navigateToResiveCostingWithSaveApprove()} reads a
     * {@code billId} request param that resolves to the SOURCE PURCHASE ORDER,
     * not to a GRN pre-bill, and passing this native draft's own bill id
     * through that param would incorrectly attempt to start a second GRN for
     * the same PO -- tripping that method's own orphan-PRE guard now that this
     * native draft already exists. The only working precedent in this codebase
     * for opening an existing saved GRN in the legacy bean is
     * {@code PharmacyBillSearch.navigateToEditSavedGrnCosting()}, which hands
     * the Bill over directly via {@code setCurrentGrnBillPre()} rather than a
     * request parameter -- that is the pattern mirrored here.
     */
    public String navigateToLegacyView() {
        if (currentGrnBillPre == null || currentGrnBillPre.getId() == null) {
            JsfUtil.addErrorMessage("Please save this GRN before switching to Legacy View");
            return "";
        }
        Bill reloaded = billFacade.find(currentGrnBillPre.getId());
        if (reloaded == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }
        grnCostingController.setCurrentGrnBillPre(reloaded);
        return grnCostingController.navigateToEditGrnCosting();
    }

    // ==================================================================
    // Save / Finalize / Approve
    // ==================================================================
    public void requestWithSaveApprove() {
        if (!isAuthorized("REQUEST_WITH_SAVE_APPROVE", "PharmacyGrnSave")) {
            return;
        }
        doRequestWithSaveApprove();
    }

    /**
     * Unguarded core of {@link #requestWithSaveApprove()}. Called directly
     * (bypassing the PharmacyGrnSave check) by {@link #finalizeGrnWithSaveApprove()}
     * and {@link #approveGrnWithSaveApprove()}, which auto-save a not-yet-
     * persisted draft as part of a Finalize/Approve action already authorized
     * under its own privilege -- mirrors legacy's
     * {@code doRequestWithSaveApprove()} exactly.
     */
    private void doRequestWithSaveApprove() {
        getCurrentGrnBillPre().setBillDate(new Date());
        getCurrentGrnBillPre().setBillTime(new Date());

        if (getCurrentGrnBillPre().getReferenceBill() == null) {
            getCurrentGrnBillPre().setReferenceBill(getApproveBill());
        }
        if (getCurrentGrnBillPre().getFromInstitution() == null) {
            getCurrentGrnBillPre().setFromInstitution(getFromInstitution());
        }
        if (getCurrentGrnBillPre().getReferenceInstitution() == null) {
            getCurrentGrnBillPre().setReferenceInstitution(getReferenceInstitution());
        }

        getBillItems(); // ensure initialized

        long editorId = sessionController.getLoggedUser().getId();
        long departmentId = sessionController.getDepartment().getId();
        long institutionId = sessionController.getInstitution().getId();

        if (getCurrentGrnBillPre().getId() == null) {
            Long referenceBillId = getApproveBill() != null ? getApproveBill().getId() : null;
            Institution fromInstForCreate = getCurrentGrnBillPre().getFromInstitution();
            Institution refInstForCreate = getCurrentGrnBillPre().getReferenceInstitution();
            // toInstitution is intentionally NOT set here: legacy's actual in-scope
            // navigateToResiveCostingWithSaveApprove() only sets fromInstitution/
            // referenceInstitution (verified against GrnCostingController.java) --
            // setToInstitution() calls elsewhere in that file belong to the older,
            // out-of-scope request()/settle()/createGrn(Bill) flow, not this one.
            long newBillId = grnCostingNativeSqlService.createDraftBill(
                    departmentId, institutionId, referenceBillId, editorId, null, null,
                    fromInstForCreate != null ? fromInstForCreate.getId() : null,
                    refInstForCreate != null ? refInstForCreate.getId() : null,
                    null);

            // createDraftBill() now also persists fromInstitution/referenceInstitution
            // (see above) -- re-apply the remaining header fields
            // already entered in memory (mirrors
            // PurchaseOrderRequestNativeSqlController.saveRequestWithoutMessage()).
            Institution fromInst = getCurrentGrnBillPre().getFromInstitution();
            Institution refInst = getCurrentGrnBillPre().getReferenceInstitution();
            PaymentMethod pm = getCurrentGrnBillPre().getPaymentMethod();
            int creditDuration = getCurrentGrnBillPre().getCreditDuration();
            double discount = getCurrentGrnBillPre().getDiscount();
            DepartmentType deptType = getCurrentGrnBillPre().getDepartmentType();
            boolean consignment = getCurrentGrnBillPre().isConsignment();
            String invoiceNumber = getCurrentGrnBillPre().getInvoiceNumber();
            Date invoiceDate = getCurrentGrnBillPre().getInvoiceDate();
            List<BillItem> pendingItems = getCurrentGrnBillPre().getBillItems();
            List<BillItem> pendingExpenses = getCurrentGrnBillPre().getBillExpenses();

            Bill reloaded = billFacade.find(newBillId);
            reloaded.setFromInstitution(fromInst);
            reloaded.setReferenceInstitution(refInst);
            reloaded.setPaymentMethod(pm);
            reloaded.setCreditDuration(creditDuration);
            reloaded.setDiscount(discount);
            reloaded.setDepartmentType(deptType);
            reloaded.setConsignment(consignment);
            reloaded.setInvoiceNumber(invoiceNumber);
            reloaded.setInvoiceDate(invoiceDate);
            reloaded.setBillItems(pendingItems);
            reloaded.setBillExpenses(pendingExpenses);
            currentGrnBillPre = reloaded;
        }

        long billId = getCurrentGrnBillPre().getId();

        deduplicateBillExpensesInMemory();

        for (BillItem i : new ArrayList<>(getCurrentGrnBillPre().getBillItems())) {
            BillItemFinanceDetails f = i.getBillItemFinanceDetails();
            if (f == null || ((f.getQuantity() == null || f.getQuantity().compareTo(BigDecimal.ZERO) == 0)
                    && (f.getFreeQuantity() == null || f.getFreeQuantity().compareTo(BigDecimal.ZERO) == 0))) {
                continue;
            }
            GrnLineData line = toLineData(i);
            GrnCostingNativeSqlService.SavedLineIds ids = grnCostingNativeSqlService.saveLine(billId, line);
            i.setId(ids.billItemId);
            // Native INSERT never reports its generated PK back onto a JPA
            // entity the way em.persist() does -- without this, a same-session
            // Save immediately followed by Approve (no DB reload in between)
            // would build GrnApproveLineData with pharmaceuticalBillItemId
            // null, silently skipping the stock/ItemBatch link and
            // StockHistory row at Approve time.
            if (i.getPharmaceuticalBillItem() != null) {
                i.getPharmaceuticalBillItem().setId(ids.pharmaceuticalBillItemId);
            }
        }

        if (getCurrentGrnBillPre().getBillExpenses() != null) {
            for (BillItem expense : getCurrentGrnBillPre().getBillExpenses()) {
                if (expense == null || expense.isRetired()) {
                    continue;
                }
                expense.setExpenseBill(getCurrentGrnBillPre());
                GrnLineData expenseLine = toExpenseLineData(expense);
                long expenseId = grnCostingNativeSqlService.saveExpenseLine(billId, expenseLine);
                expense.setId(expenseId);
            }
        }

        grnCostingNativeSqlService.updateDraftBillHeader(
                billId,
                getCurrentGrnBillPre().getInvoiceNumber(),
                getCurrentGrnBillPre().getPaymentMethod() != null ? getCurrentGrnBillPre().getPaymentMethod().name() : null,
                getCurrentGrnBillPre().getCreditDuration(),
                null,
                getCurrentGrnBillPre().getChequeRefNo(),
                getCurrentGrnBillPre().getComments(),
                getCurrentGrnBillPre().getDepartmentType() != null ? getCurrentGrnBillPre().getDepartmentType().name() : null,
                getCurrentGrnBillPre().getDiscount(),
                editorId);

        // Ensure bill discount distribution before saving (even if 0, to clear
        // previous distributions) -- matches legacy exactly.
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();

        grnCostingNativeSqlService.updateBillTotals(billId, getCurrentGrnBillPre().getNetTotal(), getCurrentGrnBillPre().getTotal());

        BillFinanceDetails bfd = getCurrentGrnBillPre().getBillFinanceDetails();
        double totalDiscount = bfd != null && bfd.getTotalDiscount() != null ? bfd.getTotalDiscount().doubleValue() : 0.0;
        double totalExpense = bfd != null && bfd.getTotalExpense() != null ? bfd.getTotalExpense().doubleValue() : 0.0;
        double totalTax = bfd != null && bfd.getTotalTaxValue() != null ? bfd.getTotalTaxValue().doubleValue() : 0.0;
        grnCostingNativeSqlService.saveBillFinanceDetails(billId, totalDiscount, totalExpense, totalTax);

        JsfUtil.addSuccessMessage("GRN Saved");
    }

    // synchronized: the Finalize button on pharmacy_grn_costing_native.xhtml has only a
    // confirm() dialog and a rendered="#{!completed}" button-hide that depends on a full
    // page re-render to take effect -- it does not stop a fast double-click before the
    // re-render lands. Same bug class as PurchaseOrderController.approve() /
    // TransferRequestController.approveTransferRequestBill() (issue #22194), and the same
    // guard legacy GrnCostingController.finalizeGrnWithSaveApprove() already applies.
    public synchronized void finalizeGrnWithSaveApprove() {
        if (!isAuthorized("FINALIZE_GRN_WITH_SAVE_APPROVE", "PharmacyGrnFinalize")) {
            return;
        }
        if (getCurrentGrnBillPre().isCompleted()) {
            JsfUtil.addErrorMessage("This GRN is already finalized");
            return;
        }
        if (getCurrentGrnBillPre().getInvoiceNumber() == null || getCurrentGrnBillPre().getInvoiceNumber().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please fill invoice number");
            return;
        }
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }
        if (getCurrentGrnBillPre().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return;
        }

        if (!getBillItems().isEmpty()) {
            DepartmentType billDeptType = getCurrentGrnBillPre().getDepartmentType();
            if (billDeptType == null && getBillItems().get(0).getItem() != null) {
                billDeptType = getBillItems().get(0).getItem().getDepartmentType();
                getCurrentGrnBillPre().setDepartmentType(billDeptType);
            }
            for (BillItem bi : getBillItems()) {
                if (bi.getItem() != null && bi.getItem().getDepartmentType() != null) {
                    if (!bi.getItem().getDepartmentType().equals(billDeptType)) {
                        JsfUtil.addErrorMessage("Items belong to more than one department type. GRN cannot be finalized.");
                        return;
                    }
                }
            }
        }

        String msg = errorCheck(getCurrentGrnBillPre(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        doRequestWithSaveApprove();

        grnCostingNativeSqlService.finalizeBill(getCurrentGrnBillPre().getId(), sessionController.getLoggedUser().getId());

        // Update the in-memory bill's completed flag too, so the page's
        // rendered="#{!completed}"/rendered="#{completed}" button toggle updates
        // immediately without a full reload.
        getCurrentGrnBillPre().setCompleted(true);
        getCurrentGrnBillPre().setCompletedBy(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setCompletedAt(new Date());

        printPreview = true;
        JsfUtil.addSuccessMessage("GRN Finalized");
    }

    // synchronized: the Approve button has only a confirm() dialog, no double-click guard.
    // Same bug class as PurchaseOrderController.approve() / TransferRequestController
    // .approveTransferRequestBill() (issue #22194), and the same guard legacy
    // GrnCostingController.approveGrnWithSaveApprove() already applies.
    public synchronized void approveGrnWithSaveApprove() {
        if (!isAuthorized("APPROVE_GRN_WITH_SAVE_APPROVE", "PharmacyGrnApprove")) {
            return;
        }
        if (getCurrentGrnBillPre().getBillTypeAtomic() == BillTypeAtomic.PHARMACY_GRN) {
            JsfUtil.addErrorMessage("This GRN is already approved");
            return;
        }
        if (getCurrentGrnBillPre().getInvoiceNumber() == null || getCurrentGrnBillPre().getInvoiceNumber().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please fill invoice number");
            return;
        }
        if (Math.abs(difference) > 1) {
            JsfUtil.addErrorMessage("The invoice does not match..! Check again");
            return;
        }
        if (getCurrentGrnBillPre().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return;
        }

        String msg = errorCheck(getCurrentGrnBillPre(), getBillItems());
        if (!msg.isEmpty()) {
            JsfUtil.addErrorMessage(msg);
            return;
        }

        if (getCurrentGrnBillPre().getId() == null) {
            doRequestWithSaveApprove();
        }

        // Ensure bill discount distribution and totals are current BEFORE processing
        // items -- so totalCostRate includes bill-level expenses when each line's
        // final approved values (and the ItemBatch costing key) are computed below.
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());

        String[] billNumbers = resolveGrnBillNumbers();
        String deptId = billNumbers[0];
        String insId = billNumbers[1];
        getCurrentGrnBillPre().setDeptId(deptId);
        getCurrentGrnBillPre().setInsId(insId);

        List<GrnApproveLineData> lines = new ArrayList<>();
        for (BillItem grnBillItem : getBillItems()) {
            BillItemFinanceDetails f = grnBillItem.getBillItemFinanceDetails();
            if (f == null || ((f.getQuantity() == null || f.getQuantity().compareTo(BigDecimal.ZERO) == 0)
                    && (f.getFreeQuantity() == null || f.getFreeQuantity().compareTo(BigDecimal.ZERO) == 0))) {
                continue;
            }
            lines.add(toApproveLineData(grnBillItem));
        }

        getCurrentGrnBillPre().setEditedAt(new Date());
        getCurrentGrnBillPre().setEditor(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setChecked(true);
        getCurrentGrnBillPre().setCheckeAt(new Date());
        getCurrentGrnBillPre().setCheckedBy(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setApproveUser(sessionController.getLoggedUser());
        getCurrentGrnBillPre().setApproveAt(new Date());
        getCurrentGrnBillPre().setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN);

        calDifference();
        // Ported, but only computed for in-memory display -- see class Javadoc on
        // doRequestWithSaveApprove(): neither native service writes bill.saleValue/
        // bill.freeValue, so these two columns are not persisted by native Approve.
        calculateRetailSaleValueAndFreeValueAtPurchaseRate(getCurrentGrnBillPre());
        double balanceValue = updateBalanceForGrn(getCurrentGrnBillPre());

        // Capture the POSITIVE net total for the payment BEFORE the sign flip below.
        double prePositiveNetTotalForPayment = getCurrentGrnBillPre().getNetTotal();
        double postNegationNetTotal = 0 - Math.abs(getCurrentGrnBillPre().getNetTotal());
        double postNegationTotal = 0 - Math.abs(getCurrentGrnBillPre().getTotal());

        boolean generatePayments = configOptionApplicationController.getBooleanValueByKey(
                "Generate Payments for GRN, GRN Returns, Direct Purchase, and Direct Purchase Returns", false);

        boolean poFullyReceived = getApproveBill() != null && getApproveBill().getId() != null
                && isPurchaseOrderFullyReceived(getApproveBill());

        GrnApproveRequest request = new GrnApproveRequest();
        request.setBillId(getCurrentGrnBillPre().getId());
        request.setPoBillId(getCurrentGrnBillPre().getReferenceBill() != null ? getCurrentGrnBillPre().getReferenceBill().getId() : null);
        request.setDeptId(deptId);
        request.setInsId(insId);
        request.setEditorId(sessionController.getLoggedUser().getId());
        request.setApproveUserId(sessionController.getLoggedUser().getId());
        request.setDepartmentId(sessionController.getDepartment().getId());
        request.setInstitutionId(sessionController.getInstitution().getId());
        request.setLines(lines);
        request.setPaymentMethod(getCurrentGrnBillPre().getPaymentMethod());
        request.setGeneratePayment(generatePayments);
        request.setPrePositiveNetTotalForPayment(prePositiveNetTotalForPayment);
        request.setPostNegationNetTotal(postNegationNetTotal);
        request.setPostNegationTotal(postNegationTotal);
        request.setBalanceValue(balanceValue);
        request.setPoFullyReceived(poFullyReceived);

        // Single transactional call -- does everything (line writes, ItemBatch/Stock/
        // StockHistory, bill-header promotion, payment, PO decrement/fully-issued,
        // orphan-PRE retirement) atomically. See GrnApprovingNativeSqlService's class
        // Javadoc for why this must be one EJB call rather than a per-item loop here.
        try {
            grnApprovingNativeSqlService.approveGrn(request);
        } catch (IllegalStateException ex) {
            // Last-resort race guard inside approveGrn() itself -- the
            // billTypeAtomic==PHARMACY_GRN check above already blocks the
            // normal double-click case, this only fires for a genuine
            // concurrent-session race.
            JsfUtil.addErrorMessage("This GRN was already approved by another session. Please refresh.");
            return;
        }

        reloadCurrentGrnBillPreAfterApprove();

        JsfUtil.addSuccessMessage("GRN Finalized");
        printPreview = true;
    }

    /**
     * Refreshes {@link #currentGrnBillPre} from the database after
     * {@link GrnApprovingNativeSqlService#approveGrn} commits, so the
     * print-preview/confirmation view reflects exactly what was persisted
     * (sign-flipped totals, promoted BILLTYPEATOMIC, new deptId/insId).
     */
    private void reloadCurrentGrnBillPreAfterApprove() {
        Long billId = currentGrnBillPre != null ? currentGrnBillPre.getId() : null;
        if (billId == null) {
            return;
        }
        Bill reloaded = billFacade.find(billId);
        if (reloaded == null) {
            return;
        }
        String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId AND bi.retired = false ORDER BY bi.searialNo";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", billId);
        List<BillItem> reloadedItems = billItemFacade.findByJpql(jpql, params);
        reloaded.setBillItems(reloadedItems);
        currentGrnBillPre = reloaded;
    }

    /**
     * Computes the department-scoped and institution-scoped bill number
     * strings for GRN Approve. Ported verbatim from
     * {@code GrnCostingController.approveGrnWithSaveApprove()} lines
     * 3341-3396 -- same config keys, same strategy branching, same
     * deptId-to-insId fallback relationship. No config key name differences
     * were found versus what the task description anticipated.
     */
    private String[] resolveGrnBillNumbers() {
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_GRN, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_GRN, "GRN");
        }

        boolean stratInsDeptYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
        boolean stratInsYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean stratInsIdInsYear = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Pharmacy GRN - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

        String deptId;
        if (stratInsDeptYear) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN);
        } else if (stratInsYear) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN);
        } else {
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN);
        }

        String insId;
        if (stratInsIdInsYear) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_GRN);
        } else {
            if (stratInsDeptYear || stratInsYear) {
                insId = deptId;
            } else {
                insId = billNumberBean.institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyGrnBill, BillClassType.BilledBill, BillNumberSuffix.GRN);
            }
        }

        return new String[]{deptId, insId};
    }

    // ==================================================================
    // GrnLineData / GrnApproveLineData DTO builders
    // ==================================================================
    private void populateLineData(GrnLineData line, BillItem bi) {
        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        line.setBillItemId(bi.getId());
        line.setPharmaceuticalBillItemId(bi.getPharmaceuticalBillItem() != null ? bi.getPharmaceuticalBillItem().getId() : null);
        line.setBillItemFinanceDetailsId(f != null ? f.getId() : null);
        line.setItemId(bi.getItem() != null ? bi.getItem().getId() : 0L);
        line.setAmpp(bi.getItem() instanceof Ampp);
        line.setReferenceBillItemId(bi.getReferanceBillItem() != null ? bi.getReferanceBillItem().getId() : null);
        line.setSerialNo(bi.getSearialNo());
        line.setCreaterId(sessionController.getLoggedUser().getId());

        if (f == null) {
            return;
        }
        line.setQuantity(BigDecimalUtil.valueOrZero(f.getQuantity()).doubleValue());
        line.setFreeQuantity(BigDecimalUtil.valueOrZero(f.getFreeQuantity()).doubleValue());
        line.setQuantityByUnits(BigDecimalUtil.valueOrZero(f.getQuantityByUnits()).doubleValue());
        line.setFreeQuantityByUnits(BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits()).doubleValue());
        line.setLineGrossRate(BigDecimalUtil.valueOrZero(f.getLineGrossRate()).doubleValue());
        line.setLineDiscountRate(BigDecimalUtil.valueOrZero(f.getLineDiscountRate()).doubleValue());
        line.setLineNetRate(BigDecimalUtil.valueOrZero(f.getLineNetRate()).doubleValue());
        // grossRate feeds the Save-stage pack/unit gross purchase rate on
        // pharmaceuticalbillitem -- see GrnCostingNativeSqlService.saveLine()'s
        // Javadoc; refined to the net-of-discount rate at Approve.
        line.setGrossRate(BigDecimalUtil.valueOrZero(f.getLineGrossRate()).doubleValue());
        line.setLineGrossTotal(BigDecimalUtil.valueOrZero(f.getLineGrossTotal()).doubleValue());
        line.setLineNetTotal(BigDecimalUtil.valueOrZero(f.getLineNetTotal()).doubleValue());
        line.setGrossTotal(BigDecimalUtil.valueOrZero(f.getGrossTotal()).doubleValue());
        line.setRetailSaleRate(BigDecimalUtil.valueOrZero(f.getRetailSaleRate()).doubleValue());
        line.setRetailSaleRatePerUnit(BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit()).doubleValue());
        line.setWholesaleRate(BigDecimalUtil.valueOrZero(f.getWholesaleRate()).doubleValue());
        line.setWholesaleRatePerUnit(BigDecimalUtil.valueOrZero(f.getWholesaleRatePerUnit()).doubleValue());
        line.setUnitsPerPack(BigDecimalUtil.valueOrZero(f.getUnitsPerPack()).doubleValue());
        line.setValueAtPurchaseRate(BigDecimalUtil.valueOrZero(f.getValueAtPurchaseRate()).doubleValue());
        line.setValueAtRetailRate(BigDecimalUtil.valueOrZero(f.getValueAtRetailRate()).doubleValue());
        line.setLineCost(BigDecimalUtil.valueOrZero(f.getLineCost()).doubleValue());
        line.setLineCostRate(BigDecimalUtil.valueOrZero(f.getLineCostRate()).doubleValue());
        line.setValueAtCostRate(BigDecimalUtil.valueOrZero(f.getValueAtCostRate()).doubleValue());
        line.setTotalCostRate(BigDecimalUtil.valueOrZero(f.getTotalCostRate()).doubleValue());
    }

    private GrnLineData toLineData(BillItem bi) {
        GrnLineData line = new GrnLineData();
        populateLineData(line, bi);
        return line;
    }

    /**
     * Expense rows are plain BillItem rows with no BillItemFinanceDetails/
     * PharmaceuticalBillItem (mirrors {@code GrnCostingController.addExpense()}),
     * so their line data is read directly off the BillItem's own qty/rate/
     * netRate/netValue/grossValue fields.
     */
    private GrnLineData toExpenseLineData(BillItem expense) {
        GrnLineData line = new GrnLineData();
        line.setBillItemId(expense.getId());
        line.setItemId(expense.getItem() != null ? expense.getItem().getId() : 0L);
        line.setSerialNo(expense.getSearialNo());
        line.setCreaterId(sessionController.getLoggedUser().getId());
        line.setQuantity(expense.getQty() != null ? expense.getQty() : 0.0);
        line.setLineGrossRate(expense.getRate());
        line.setLineNetRate(expense.getNetRate());
        line.setLineGrossTotal(expense.getGrossValue());
        line.setLineNetTotal(expense.getNetValue());
        line.setConsideredForCosting(expense.isConsideredForCosting());
        return line;
    }

    /**
     * Runs {@code applyFinanceDetailsToPharmaceutical(bi, true)} to compute
     * the final approved values (matching legacy immediately before
     * {@code saveItemBatchWithCosting()}), then reads them back off the
     * now-updated BillItemFinanceDetails/PharmaceuticalBillItem into a
     * GrnApproveLineData, including the PO linkage fields.
     */
    private GrnApproveLineData toApproveLineData(BillItem bi) {
        applyFinanceDetailsToPharmaceutical(bi, true);

        GrnApproveLineData line = new GrnApproveLineData();
        populateLineData(line, bi);

        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
        line.setExpiryDate(pbi != null ? pbi.getDoe() : null);
        line.setBatchNo(pbi != null ? pbi.getStringValue() : null);
        line.setPoBillItemId(bi.getReferanceBillItem() != null && bi.getReferanceBillItem().getId() != null
                ? bi.getReferanceBillItem().getId() : 0L);
        line.setPoReceivedQty(pbi != null ? Math.abs(pbi.getQty()) : 0.0);
        line.setPoReceivedFreeQty(pbi != null ? Math.abs(pbi.getFreeQty()) : 0.0);
        line.setCostRatePerUnit(pbi != null ? pbi.getCostRate() : 0.0);
        line.setPurchaseRatePerUnit(pbi != null ? pbi.getPurchaseRate() : 0.0);
        line.setRetailRatePerUnit(pbi != null ? pbi.getRetailRate() : 0.0);
        return line;
    }

    // ==================================================================
    // Line-editing action methods (in-memory only, matching legacy)
    // ==================================================================
    public List<BillItem> findAllBillItemsRefernceToOriginalItem(BillItem referenceBillItem) {
        List<BillItem> tmpBillItems = new ArrayList<>();
        for (BillItem i : getBillItems()) {
            if (i.getReferanceBillItem() == referenceBillItem) {
                tmpBillItems.add(i);
            }
        }
        return tmpBillItems;
    }

    public void duplicateItem(BillItem originalBillItemToDuplicate) {
        if (originalBillItemToDuplicate == null) {
            return;
        }
        if (originalBillItemToDuplicate.getReferanceBillItem() == null) {
            JsfUtil.addInfoMessage("Cannot duplicate an ad-hoc free item line");
            return;
        }
        BigDecimal totalQuantityOfBillItemsRefernceToOriginalItem = BigDecimal.ZERO;
        BigDecimal totalFreeQuantityOfBillItemsRefernceToOriginalItem = BigDecimal.ZERO;

        BigDecimal remainFreeQty;
        BigDecimal remainQty;

        BillItem newBillItemCreatedByDuplication = new BillItem();
        newBillItemCreatedByDuplication.copy(originalBillItemToDuplicate);
        newBillItemCreatedByDuplication.setId(null);

        BillItemFinanceDetails newBifd = originalBillItemToDuplicate.getBillItemFinanceDetails().clone();
        newBifd.setId(null);
        newBifd.setBillItem(newBillItemCreatedByDuplication);

        PharmaceuticalBillItem newPharmaceuticalBillItemCreatedByDuplication = new PharmaceuticalBillItem();
        newPharmaceuticalBillItemCreatedByDuplication.copy(originalBillItemToDuplicate.getPharmaceuticalBillItem());
        newPharmaceuticalBillItemCreatedByDuplication.setId(null);
        newPharmaceuticalBillItemCreatedByDuplication.setBillItem(newBillItemCreatedByDuplication);

        newBillItemCreatedByDuplication.setItem(originalBillItemToDuplicate.getItem());
        newBillItemCreatedByDuplication.setReferanceBillItem(originalBillItemToDuplicate.getReferanceBillItem());
        newBillItemCreatedByDuplication.setPharmaceuticalBillItem(newPharmaceuticalBillItemCreatedByDuplication);
        newBillItemCreatedByDuplication.setBillItemFinanceDetails(newBifd);

        List<BillItem> tmpBillItems = findAllBillItemsRefernceToOriginalItem(originalBillItemToDuplicate.getReferanceBillItem());

        for (BillItem bi : tmpBillItems) {
            totalQuantityOfBillItemsRefernceToOriginalItem = totalQuantityOfBillItemsRefernceToOriginalItem.add(bi.getBillItemFinanceDetails().getQuantity());
            totalFreeQuantityOfBillItemsRefernceToOriginalItem = totalFreeQuantityOfBillItemsRefernceToOriginalItem.add(bi.getBillItemFinanceDetails().getFreeQuantity());
        }
        remainQty = BigDecimal.valueOf(originalBillItemToDuplicate.getPreviousRecieveQtyInUnit()).subtract(totalQuantityOfBillItemsRefernceToOriginalItem);
        remainFreeQty = BigDecimal.valueOf(originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit()).subtract(totalFreeQuantityOfBillItemsRefernceToOriginalItem);

        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQty(remainQty.doubleValue());
        newBifd.setQuantity(remainQty);
        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setQtyInUnit(remainQty.doubleValue());
        newBifd.setFreeQuantity(remainFreeQty);

        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQty(remainFreeQty.doubleValue());
        newBillItemCreatedByDuplication.getPharmaceuticalBillItem().setFreeQtyInUnit(remainFreeQty.doubleValue());

        newBillItemCreatedByDuplication.setTmpQty(remainQty.doubleValue());
        newBillItemCreatedByDuplication.setTmpFreeQty(remainFreeQty.doubleValue());

        newBillItemCreatedByDuplication.setPreviousRecieveQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveQtyInUnit());
        newBillItemCreatedByDuplication.setPreviousRecieveFreeQtyInUnit(originalBillItemToDuplicate.getPreviousRecieveFreeQtyInUnit());
        getBillItems().add(newBillItemCreatedByDuplication);
        recalculateFinancialsBeforeAddingBillItem(newBillItemCreatedByDuplication.getBillItemFinanceDetails());
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        recalculateProfitMarginsForAllItems();
        calDifference();
    }

    public void addFreeItemNotInPo() {
        if (!configOptionApplicationController.getBooleanValueByKey("GRN - Allow Free Items Not in Purchase Order", false)) {
            JsfUtil.addErrorMessage("Adding free items not in the purchase order is not enabled.");
            return;
        }
        if (freeItemToAdd == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        List<Item> singleItemList = new ArrayList<>();
        singleItemList.add(freeItemToAdd);
        Map<Long, double[]> lastRatesMap = buildLastRatesMap(singleItemList);
        double[] rates = lastRatesMap.get(freeItemToAdd.getId());
        double lastPr = rates != null ? rates[0] : 0.0;
        double lastRr = rates != null ? rates[1] : 0.0;

        BillItem freeBillItem = new BillItem();
        freeBillItem.setSearialNo(getBillItems().size());
        freeBillItem.setItem(freeItemToAdd);
        freeBillItem.setReferanceBillItem(null);
        freeBillItem.setQty(0.0);
        freeBillItem.setTmpQty(0.0);
        freeBillItem.setTmpFreeQty(0.0);

        PharmaceuticalBillItem freePbi = new PharmaceuticalBillItem();
        freePbi.setBillItem(freeBillItem);
        freePbi.setQty(0.0);
        freePbi.setQtyInUnit(0.0);
        freePbi.setFreeQty(0.0);
        freePbi.setFreeQtyInUnit(0.0);
        freePbi.setPurchaseRate(0.0);
        freePbi.setRetailRate(lastRr);

        freeBillItem.setPharmaceuticalBillItem(freePbi);

        BillItemFinanceDetails fd = new BillItemFinanceDetails(freeBillItem);
        fd.setQuantity(BigDecimal.ZERO);
        fd.setFreeQuantity(BigDecimal.ZERO);
        fd.setLineGrossRate(BigDecimal.ZERO);
        fd.setLineDiscountRate(BigDecimal.ZERO);
        fd.setLineNetRate(BigDecimal.ZERO);
        fd.setRetailSaleRate(BigDecimal.valueOf(lastRr));

        freeBillItem.setBillItemFinanceDetails(fd);
        recalculateFinancialsBeforeAddingBillItem(fd);

        freePbi.setLastPurchaseRate(lastPr);
        freePbi.setLastPurchaseRateInUnit(lastPr);
        freePbi.setLastPurchaseRatePack(lastPr * fd.getUnitsPerPack().doubleValue());

        getBillItems().add(freeBillItem);

        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        recalculateProfitMarginsForAllItems();

        freeItemToAdd = null;
        JsfUtil.addSuccessMessage("Free item added. Enter the received free quantity.");
    }

    /**
     * Retires a previously-saved line's DB row immediately via
     * {@link GrnCostingNativeSqlService#retireLine} (rather than deferring
     * to the next Save, per {@link PurchaseOrderRequestNativeSqlController#removeItem}'s
     * precedent) -- {@code doRequestWithSaveApprove()}'s per-line loop only
     * iterates the SURVIVING in-memory list, so a persisted line simply
     * dropped from that list would never otherwise get its {@code retired}
     * flag written.
     */
    public void removeItem(BillItem bi) {
        if (bi == null) {
            return;
        }
        boolean removed;
        List<BillItem> items = getBillItems();
        removed = items.remove(bi);
        if (!removed) {
            BillItem toRemove = null;
            if (bi.getId() != null) {
                for (BillItem it : items) {
                    if (it != null && bi.getId().equals(it.getId())) {
                        toRemove = it;
                        break;
                    }
                }
            } else {
                int serial = bi.getSearialNo();
                for (BillItem it : items) {
                    if (it != null && (it.getId() == null || it.getId() == 0) && it.getSearialNo() == serial) {
                        toRemove = it;
                        break;
                    }
                }
            }
            if (toRemove != null) {
                removed = items.remove(toRemove);
                bi = toRemove;
            }
        }

        if (removed) {
            if (bi.getId() != null) {
                grnCostingNativeSqlService.retireLine(bi.getId(), sessionController.getLoggedUser().getId());
            }
            ensureBillDiscountSynchronization();
            calculateBillTotalsFromItems();
            distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
            recalculateProfitMarginsForAllItems();
            calDifference();
            if (getCurrentGrnBillPre().getId() != null) {
                grnCostingNativeSqlService.updateBillTotals(getCurrentGrnBillPre().getId(), getCurrentGrnBillPre().getNetTotal(), getCurrentGrnBillPre().getTotal());
            }
        }
    }

    public void removeSelected() {
        if (selectedBillItems == null) {
            return;
        }
        for (BillItem b : selectedBillItems) {
            getBillItems().remove(b);
            if (b.getId() != null) {
                grnCostingNativeSqlService.retireLine(b.getId(), sessionController.getLoggedUser().getId());
            }
        }
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        recalculateProfitMarginsForAllItems();
        calDifference();
        if (getCurrentGrnBillPre().getId() != null) {
            grnCostingNativeSqlService.updateBillTotals(getCurrentGrnBillPre().getId(), getCurrentGrnBillPre().getNetTotal(), getCurrentGrnBillPre().getTotal());
        }
        selectedBillItems = null;
    }

    public void setBatch(BillItem pid) {
        if (pid.getPharmaceuticalBillItem().getDoe() == null) {
            return;
        }
        if (pid.getPharmaceuticalBillItem().getDoe().getTime() < Calendar.getInstance().getTimeInMillis()) {
            pid.getPharmaceuticalBillItem().setStringValue(null);
            return;
        }
        if (pid.getPharmaceuticalBillItem().getStringValue().trim().isEmpty()) {
            Date date = pid.getPharmaceuticalBillItem().getDoe();
            java.text.DateFormat df = new java.text.SimpleDateFormat("ddMMyyyy");
            String reportDate = df.format(date);
            pid.getPharmaceuticalBillItem().setStringValue(reportDate);
        }
    }

    public void onEdit(RowEditEvent event) {
        BillItem editingBillItem = (BillItem) event.getObject();
        setBatch(editingBillItem);
        onEdit(editingBillItem);
    }

    public void checkQty(BillItem bi) {
        if (bi.getTmpQty() < 0.0) {
            bi.setTmpQty(0.0);
        }
        if (bi.getTmpFreeQty() < 0.0) {
            bi.setTmpFreeQty(0.0);
        }
        onEdit(bi);
    }

    /**
     * Clamps a bill item's quantity down to the PO's remaining orderable qty
     * and surfaces an error when it's over. Shared by {@link #onEdit(BillItem)}
     * and {@link #qtyChangedListner(BillItem)} (issue #22681).
     */
    private void clampQuantityToRemaining(BillItem tmp, BillItemFinanceDetails f) {
        if (tmp.getReferanceBillItem() != null) {
            double remains = getRemainingQty(tmp.getPharmaceuticalBillItem());
            if (remains < f.getQuantity().doubleValue()) {
                f.setQuantity(BigDecimal.valueOf(remains));
                tmp.setTmpQty(remains);
                JsfUtil.addErrorMessage("You cant Change Qty than Remaining qty");
            }
        }
    }

    public void onEdit(BillItem tmp) {
        setBatch(tmp);
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        clampQuantityToRemaining(tmp, f);

        if (f.getLineGrossRate().compareTo(f.getRetailSaleRatePerUnit()) > 0) {
            f.setRetailSaleRatePerUnit(f.getLineGrossRate());
            JsfUtil.addErrorMessage("You cant set retail price below purchase rate");
        }
        recalculateFinancialsBeforeAddingBillItem(f);
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());

        calDifference();
        recalculateProfitMarginsForAllItems();
    }

    public void lineDiscountRateChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        recalculateFinancialsBeforeAddingBillItem(f);
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        recalculateProfitMarginsForAllItems();
    }

    public void retailRateChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        recalculateFinancialsBeforeAddingBillItem(f);
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        recalculateProfitMarginsForAllItems();
        calDifference();
    }

    public void wholesaleRateChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        recalculateFinancialsBeforeAddingBillItem(f);
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        recalculateProfitMarginsForAllItems();
        calDifference();
    }

    public void freeQtyChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        if (configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            BigDecimal freeQty = f.getFreeQuantity();
            if (freeQty != null && freeQty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                f.setFreeQuantity(BigDecimal.ZERO);
                recalculateFinancialsBeforeAddingBillItem(f);
                ensureBillDiscountSynchronization();
                calculateBillTotalsFromItems();
                distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
                calDifference();
                recalculateProfitMarginsForAllItems();
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
                return;
            }
        }

        recalculateFinancialsBeforeAddingBillItem(f);
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        recalculateProfitMarginsForAllItems();
    }

    public void qtyChangedListner(BillItem tmp) {
        BillItemFinanceDetails f = tmp.getBillItemFinanceDetails();
        if (f == null) {
            return;
        }
        if (configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            BigDecimal qty = f.getQuantity();
            if (qty != null && qty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                f.setQuantity(BigDecimal.ZERO);
                recalculateFinancialsBeforeAddingBillItem(f);
                ensureBillDiscountSynchronization();
                calculateBillTotalsFromItems();
                distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
                calDifference();
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
                return;
            }
        }

        clampQuantityToRemaining(tmp, f);

        recalculateFinancialsBeforeAddingBillItem(f);
        ensureBillDiscountSynchronization();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
    }

    // ==================================================================
    // Expenses (in-memory only -- persisted on next Save/Finalize/Approve)
    // ==================================================================
    public BillItem getCurrentExpense() {
        if (currentExpense == null) {
            currentExpense = new BillItem();
            currentExpense.setQty(1.0);
            currentExpense.setConsideredForCosting(true);
        }
        return currentExpense;
    }

    public void setCurrentExpense(BillItem currentExpense) {
        this.currentExpense = currentExpense;
    }

    public void clearCurrentExpense() {
        this.currentExpense = null;
    }

    public void onExpenseItemSelect() {
        // No-op, mirrors legacy: called when an expense item is chosen from autocomplete.
    }

    public void addExpense() {
        if (getCurrentExpense().getItem() == null) {
            JsfUtil.addErrorMessage("Expense ?");
            return;
        }
        if (currentExpense.getQty() == null || currentExpense.getQty().equals(0.0)) {
            currentExpense.setQty(1.0);
        }
        if (currentExpense.getNetRate() == 0.0) {
            currentExpense.setNetRate(currentExpense.getRate());
        }

        currentExpense.setNetValue(currentExpense.getNetRate() * currentExpense.getQty());
        currentExpense.setGrossValue(currentExpense.getRate() * currentExpense.getQty());

        getCurrentExpense().setSearialNo(getGrnBill().getBillExpenses().size());
        getCurrentExpense().setExpenseBill(getGrnBill());

        getGrnBill().getBillExpenses().add(currentExpense);

        recalculateExpenseTotals();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
        recalculateProfitMarginsForAllItems();

        currentExpense = null;
    }

    /**
     * Retires a previously-saved expense row immediately (same reasoning as
     * {@link #removeItem(BillItem)}) rather than deferring to the next Save.
     */
    public void removeExpense(BillItem expense) {
        if (expense == null) {
            return;
        }
        expense.setRetired(true);
        expense.setRetiredAt(new Date());
        expense.setRetirer(sessionController.getLoggedUser());

        if (expense.getId() != null) {
            grnCostingNativeSqlService.retireLine(expense.getId(), sessionController.getLoggedUser().getId());
        }

        getGrnBill().getBillExpenses().remove(expense);

        recalculateExpenseTotals();
        calculateBillTotalsFromItems();
        distributeProportionalBillValuesToItems(getBillItems(), getGrnBill());
        calDifference();
    }

    public void recalculateExpenseTotals() {
        double totalExpenses = 0.0;
        double expensesForCosting = 0.0;
        double expensesNotForCosting = 0.0;

        if (getGrnBill().getBillExpenses() != null) {
            for (BillItem expense : getGrnBill().getBillExpenses()) {
                if (expense.isRetired()) {
                    continue;
                }
                double expenseValue = expense.getNetValue();
                totalExpenses += expenseValue;
                if (expense.isConsideredForCosting()) {
                    expensesForCosting += expenseValue;
                } else {
                    expensesNotForCosting += expenseValue;
                }
            }
        }

        getGrnBill().setExpenseTotal(totalExpenses);
        getGrnBill().setExpensesTotalConsideredForCosting(expensesForCosting);
        getGrnBill().setExpensesTotalNotConsideredForCosting(expensesNotForCosting);
    }

    public double getExpensesTotalConsideredForCosting() {
        if (getGrnBill() == null || getGrnBill().getBillExpenses() == null || getGrnBill().getBillExpenses().isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (BillItem expense : getGrnBill().getBillExpenses()) {
            if (!expense.isRetired() && expense.isConsideredForCosting()) {
                total += expense.getNetValue();
            }
        }
        return total;
    }

    public double getExpensesTotalNotConsideredForCosting() {
        if (getGrnBill() == null || getGrnBill().getBillExpenses() == null || getGrnBill().getBillExpenses().isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (BillItem expense : getGrnBill().getBillExpenses()) {
            if (!expense.isRetired() && !expense.isConsideredForCosting()) {
                total += expense.getNetValue();
            }
        }
        return total;
    }

    public double calExpenses() {
        double tot = 0.0;
        if (getGrnBill().getBillExpenses() != null) {
            for (BillItem be : getGrnBill().getBillExpenses()) {
                if (!be.isRetired()) {
                    tot = tot + be.getNetValue();
                }
            }
        }
        return tot;
    }

    /**
     * Remove duplicate expense entries from the in-memory list before
     * persist. Ported from {@code GrnCostingController.deduplicateBillExpensesInMemory()},
     * adapted to retire persisted duplicates via
     * {@link GrnCostingNativeSqlService#retireLine} instead of
     * {@code billItemFacade.edit()}.
     */
    private void deduplicateBillExpensesInMemory() {
        if (getCurrentGrnBillPre() == null) {
            return;
        }
        List<BillItem> expenses = getCurrentGrnBillPre().getBillExpenses();
        if (expenses == null || expenses.isEmpty()) {
            return;
        }

        Map<String, BillItem> seen = new HashMap<>();
        List<BillItem> toRemove = new ArrayList<>();

        for (BillItem e : expenses) {
            if (e == null || e.isRetired()) {
                continue;
            }
            Long itemId = e.getItem() != null ? e.getItem().getId() : null;
            String desc = e.getDescreption() != null ? e.getDescreption().trim().toLowerCase() : "";
            String key = (itemId == null ? "_null" : itemId.toString())
                    + "|" + String.format(Locale.ROOT, "%.6f", e.getNetRate())
                    + "|" + (e.isConsideredForCosting() ? "1" : "0")
                    + "|" + desc
                    + "|" + String.format(Locale.ROOT, "%.6f", e.getQty() == null ? 0.0 : e.getQty());

            if (!seen.containsKey(key)) {
                seen.put(key, e);
            } else {
                if (e.getId() != null) {
                    e.setRetired(true);
                    e.setBill(null);
                    e.setExpenseBill(null);
                    e.setRetiredAt(new Date());
                    e.setRetirer(sessionController.getLoggedUser());
                    grnCostingNativeSqlService.retireLine(e.getId(), sessionController.getLoggedUser().getId());
                } else {
                    toRemove.add(e);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            expenses.removeAll(toRemove);
        }
    }

    // ==================================================================
    // Calculation helpers (ported verbatim -- in-memory only, JPQL reads only)
    // ==================================================================
    public boolean isShowProfitInGrnBill() {
        return configOptionApplicationController.getBooleanValueByKey("Show Profit Percentage in GRN", true);
    }

    public double calculateProfitMargin(BillItem bi) {
        return calculateProfitMarginForPurchases(bi);
    }

    public double calDifference() {
        double netTotal = getGrnBill().getNetTotal();
        difference = Math.abs(insTotal) - Math.abs(netTotal);
        return difference;
    }

    public void calculateBillTotalsFromItems() {
        calculateBillTotalsFromItemsForPurchases(getGrnBill(), getBillItems());
    }

    public void calculateBillTotalsFromItemsForPurchases(Bill bill, List<BillItem> billItems) {
        boolean includeFreeItemsInPurchaseValue = configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true);

        recalculateExpenseTotals();

        if (billItems != null && !billItems.isEmpty()) {
            for (BillItem bi : billItems) {
                BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
                if (f != null) {
                    f.setBillExpense(BigDecimal.ZERO);
                    f.setBillDiscount(BigDecimal.ZERO);
                    f.setBillTax(BigDecimal.ZERO);

                    f.setTotalExpense(BigDecimalUtil.valueOrZero(f.getLineExpense()));
                    f.setTotalDiscount(BigDecimalUtil.valueOrZero(f.getLineDiscount()));
                    f.setTotalTax(BigDecimalUtil.valueOrZero(f.getLineTax()));

                    f.setNetTotal(BigDecimalUtil.valueOrZero(f.getLineNetTotal()));
                    f.setTotalCost(BigDecimalUtil.valueOrZero(f.getLineNetTotal()));
                }
            }
        }

        int serialNo = 0;

        BigDecimal billDiscount = BigDecimal.valueOf(bill.getDiscount());
        BigDecimal billExpense = BigDecimal.valueOf(bill.getExpensesTotalConsideredForCosting());
        BigDecimal billTax = BigDecimal.valueOf(bill.getTax());
        BigDecimal billCost = billDiscount.subtract(billExpense.add(billTax));

        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalLineExpenses = BigDecimal.ZERO;
        BigDecimal totalLineCosts = BigDecimal.ZERO;
        BigDecimal totalTaxLines = BigDecimal.ZERO;

        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;

        BigDecimal totalPurchaseValueFree = BigDecimal.ZERO;
        BigDecimal totalPurchaseValueNonFree = BigDecimal.ZERO;
        BigDecimal totalCostValueFree = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValueFree = BigDecimal.ZERO;

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (BillItem bi : billItems) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

            if (bi.getQty() == null || bi.getQty() == 0.0) {
                if (bi.getItem() instanceof Ampp) {
                    bi.setQty(pbi.getQtyPacks());
                    bi.setRate(pbi.getPurchaseRatePack());
                } else if (bi.getItem() instanceof Amp) {
                    bi.setQty(pbi.getQty());
                    bi.setRate(pbi.getPurchaseRate());
                }
            } else {
                if (bi.getItem() instanceof Ampp) {
                    bi.setRate(pbi.getPurchaseRatePack());
                } else if (bi.getItem() instanceof Amp) {
                    bi.setRate(pbi.getPurchaseRate());
                }
            }

            bi.setSearialNo(serialNo++);
            double netValue = bi.getQty() * bi.getNetRate();
            bi.setNetValue(-netValue);

            if (f != null) {
                BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal qtyTotal = qty.add(freeQty);

                BigDecimal costRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
                BigDecimal purchaseRate = Optional.ofNullable(f.getLineNetRate()).orElse(BigDecimal.ZERO);
                BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO);
                BigDecimal wholesaleRate = Optional.ofNullable(f.getWholesaleRate()).orElse(BigDecimal.ZERO);

                BigDecimal retailValue = retailRate.multiply(qtyTotal);
                BigDecimal wholesaleValue = wholesaleRate.multiply(qtyTotal);
                BigDecimal freeItemValue = costRate.multiply(freeQty);

                BigDecimal freeItemCostValue = costRate.multiply(freeQty);
                BigDecimal freeItemRetailValue = retailRate.multiply(freeQty);

                totalLineDiscounts = totalLineDiscounts.add(Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO));
                totalLineExpenses = totalLineExpenses.add(Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO));
                totalTaxLines = totalTaxLines.add(Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO));
                totalLineCosts = totalLineCosts.add(Optional.ofNullable(f.getLineCost()).orElse(BigDecimal.ZERO));

                totalFreeItemValue = totalFreeItemValue.add(freeItemValue);
                totalPurchase = totalPurchase.add(Optional.ofNullable(f.getValueAtPurchaseRate()).orElse(BigDecimal.ZERO));
                totalRetail = totalRetail.add(retailValue);
                totalWholesale = totalWholesale.add(wholesaleValue);

                if (includeFreeItemsInPurchaseValue) {
                    BigDecimal freeItemPurchaseValue = purchaseRate.multiply(freeQty);
                    BigDecimal paidItemPurchaseValue = purchaseRate.multiply(qty);
                    totalPurchaseValueFree = totalPurchaseValueFree.add(freeItemPurchaseValue);
                    totalPurchaseValueNonFree = totalPurchaseValueNonFree.add(paidItemPurchaseValue);
                } else {
                    totalPurchaseValueFree = totalPurchaseValueFree.add(BigDecimal.ZERO);
                    totalPurchaseValueNonFree = totalPurchaseValueNonFree.add(Optional.ofNullable(f.getValueAtPurchaseRate()).orElse(BigDecimal.ZERO));
                }

                totalCostValueFree = totalCostValueFree.add(freeItemCostValue);
                totalRetailSaleValueFree = totalRetailSaleValueFree.add(freeItemRetailValue);

                totalQty = totalQty.add(qty);
                totalFreeQty = totalFreeQty.add(freeQty);
                totalQtyAtomic = totalQtyAtomic.add(Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO));
                totalFreeQtyAtomic = totalFreeQtyAtomic.add(Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO));

                grossTotal = grossTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
                lineGrossTotal = lineGrossTotal.add(Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO));
                netTotal = netTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                lineNetTotal = lineNetTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));

                totalDiscount = totalDiscount.add(Optional.ofNullable(f.getTotalDiscount()).orElse(BigDecimal.ZERO));
                totalExpense = totalExpense.add(Optional.ofNullable(f.getTotalExpense()).orElse(BigDecimal.ZERO));
                totalCost = totalCost.add(Optional.ofNullable(f.getTotalCost()).orElse(BigDecimal.ZERO));
                totalTax = totalTax.add(Optional.ofNullable(f.getTotalTax()).orElse(BigDecimal.ZERO));
            }
        }

        double currentBillExpensesConsideredForCosting = 0.0;
        if (bill.getBillExpenses() != null && !bill.getBillExpenses().isEmpty()) {
            for (BillItem expense : bill.getBillExpenses()) {
                if (expense.isRetired()) {
                    continue;
                }
                if (expense.isConsideredForCosting()) {
                    currentBillExpensesConsideredForCosting += expense.getNetValue();
                }
            }
        }

        netTotal = lineNetTotal.abs().add(billTax.abs()).add(BigDecimal.valueOf(currentBillExpensesConsideredForCosting).abs()).subtract(billDiscount.abs());

        BigDecimal expensesNotForCosting = BigDecimal.valueOf(bill.getExpensesTotalNotConsideredForCosting());
        BigDecimal totalBillValue = netTotal.add(expensesNotForCosting);

        bill.setTotal(lineNetTotal.doubleValue());
        bill.setNetTotal(netTotal.doubleValue());
        bill.setSaleValue(totalRetail.doubleValue());

        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }

        bfd.setBillDiscount(billDiscount);
        bfd.setBillExpense(billExpense);
        bfd.setBillTaxValue(billTax);
        bfd.setBillCostValue(billCost);

        bfd.setLineDiscount(totalLineDiscounts);
        bfd.setLineExpense(totalLineExpenses);
        bfd.setItemTaxValue(totalTaxLines);
        bfd.setLineCostValue(totalLineCosts);

        bfd.setTotalDiscount(totalLineDiscounts.add(billDiscount));
        bfd.setTotalExpense(totalLineExpenses.add(billExpense));
        bfd.setTotalTaxValue(totalTaxLines.add(billTax));
        bfd.setTotalCostValue(totalLineCosts);

        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalPurchaseValue(totalPurchase);
        bfd.setTotalRetailSaleValue(totalRetail);
        bfd.setTotalWholesaleValue(totalWholesale);

        bfd.setTotalPurchaseValueFree(totalPurchaseValueFree);
        bfd.setTotalPurchaseValueNonFree(totalPurchaseValueNonFree);
        bfd.setTotalCostValueFree(totalCostValueFree);
        bfd.setTotalRetailSaleValueFree(totalRetailSaleValueFree);

        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);

        bfd.setGrossTotal(lineGrossTotal);
        bfd.setLineGrossTotal(lineGrossTotal);
        bfd.setNetTotal(netTotal);
        bfd.setLineNetTotal(lineNetTotal);

        bfd.setBillExpensesConsideredForCosting(BigDecimal.valueOf(bill.getExpensesTotalConsideredForCosting()));
        bfd.setBillExpensesNotConsideredForCosting(expensesNotForCosting);
        bfd.setTotalBillValue(totalBillValue);
    }

    public void distributeProportionalBillValuesToItems(List<BillItem> billItems, Bill bill) {
        if (bill == null) {
            return;
        }

        if (bill.getBillFinanceDetails() == null) {
            bill.setBillFinanceDetails(new BillFinanceDetails(bill));
        }

        double expenseTotal = 0.0;
        double expensesTotalConsideredForCosting = 0.0;
        double expensesTotalNotConsideredForCosting = 0.0;

        if (bill.getBillExpenses() != null && !bill.getBillExpenses().isEmpty()) {
            for (BillItem expense : bill.getBillExpenses()) {
                if (expense.isRetired()) {
                    continue;
                }
                double expenseValue = expense.getNetValue();
                boolean isConsidered = expense.isConsideredForCosting();
                expenseTotal += expenseValue;
                if (isConsidered) {
                    expensesTotalConsideredForCosting += expenseValue;
                } else {
                    expensesTotalNotConsideredForCosting += expenseValue;
                }
            }
        }

        bill.setExpenseTotal(expenseTotal);
        bill.setExpensesTotalConsideredForCosting(expensesTotalConsideredForCosting);
        bill.setExpensesTotalNotConsideredForCosting(expensesTotalNotConsideredForCosting);

        bill.getBillFinanceDetails().setBillDiscount(BigDecimal.valueOf(bill.getDiscount()));
        bill.getBillFinanceDetails().setBillTaxValue(BigDecimal.valueOf(bill.getTax()));
        bill.getBillFinanceDetails().setBillExpense(BigDecimal.valueOf(expensesTotalConsideredForCosting));

        if (billItems == null || billItems.isEmpty()) {
            return;
        }

        BigDecimal totalBasis = BigDecimal.ZERO;
        Map<BillItem, BigDecimal> itemBases = new HashMap<>();
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }
            BigDecimal lineNetTotal = BigDecimalUtil.valueOrZero(f.getLineNetTotal());
            BigDecimal basis = lineNetTotal;
            itemBases.put(bi, basis);
            totalBasis = totalBasis.add(basis);
        }

        if (BigDecimalUtil.isNullOrZero(totalBasis)) {
            return;
        }

        BigDecimal billDiscountTotal = BigDecimalUtil.valueOrZero(bill.getBillFinanceDetails().getBillDiscount());
        BigDecimal billExpenseTotal = BigDecimalUtil.valueOrZero(bill.getBillFinanceDetails().getBillExpense());
        BigDecimal billTaxTotal = BigDecimalUtil.valueOrZero(bill.getBillFinanceDetails().getBillTaxValue());

        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }
            BigDecimal basis = itemBases.get(bi);
            BigDecimal ratio = basis.divide(totalBasis, 12, RoundingMode.HALF_UP);

            BigDecimal lineDiscount = BigDecimalUtil.valueOrZero(f.getLineDiscount());
            BigDecimal lineExpense = BigDecimalUtil.valueOrZero(f.getLineExpense());
            BigDecimal lineTax = BigDecimalUtil.valueOrZero(f.getLineTax());
            BigDecimal lineNetTotal = BigDecimalUtil.valueOrZero(f.getLineNetTotal());
            BigDecimal lineGrossTotal = BigDecimalUtil.valueOrZero(f.getLineGrossTotal());
            BigDecimal lineGrossRate = BigDecimalUtil.valueOrZero(f.getLineGrossRate());

            BigDecimal billDiscount = billDiscountTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billExpense = billExpenseTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billTax = billTaxTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            f.setBillDiscount(billDiscount);
            f.setBillExpense(billExpense);
            f.setBillTax(billTax);

            BigDecimal totalDiscount = lineDiscount.add(billDiscount);
            BigDecimal totalExpense = lineExpense.add(billExpense);
            BigDecimal totalTax = lineTax.add(billTax);

            f.setTotalDiscount(totalDiscount);
            f.setTotalExpense(totalExpense);
            f.setTotalTax(totalTax);

            BigDecimal quantity = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal totalQty = quantity.add(freeQty);
            f.setTotalQuantity(totalQty);

            BigDecimal billDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? billDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillDiscountRate(billDiscountRate);

            BigDecimal totalDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setTotalDiscountRate(totalDiscountRate);

            BigDecimal netTotal = lineGrossTotal.subtract(totalDiscount).add(totalTax).add(totalExpense);
            f.setNetTotal(netTotal);
            f.setTotalCost(netTotal);

            BigDecimal billCost = netTotal.subtract(lineNetTotal);
            f.setBillCost(billCost);

            BigDecimal qtyUnits = Optional.ofNullable(f.getTotalQuantityByUnits()).orElse(totalQty);

            BigDecimal lineCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? lineNetTotal.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal billCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal totalCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            f.setLineCostRate(lineCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setBillCostRate(billCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setTotalCostRate(totalCostRate.setScale(4, RoundingMode.HALF_UP));

            BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
            if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
                unitsPerPack = BigDecimal.ONE;
            }
            BigDecimal costRate = BigDecimalUtil.multiply(totalCostRate, unitsPerPack);
            f.setCostRate(costRate);

            BigDecimal valueAtCostRate = BigDecimalUtil.multiply(qtyUnits, totalCostRate);
            f.setValueAtCostRate(valueAtCostRate);

            if (bi.getPharmaceuticalBillItem() != null) {
                bi.getPharmaceuticalBillItem().setCostRate(totalCostRate.doubleValue());
                BigDecimal qtyByUnits = BigDecimalUtil.valueOrZero(f.getQuantityByUnits());
                BigDecimal costValue = BigDecimalUtil.multiply(qtyByUnits, totalCostRate);
                bi.getPharmaceuticalBillItem().setCostValue(costValue.doubleValue());
            }

            f.setLineGrossRate(lineGrossRate);
            f.setBillGrossRate(BigDecimal.ZERO);
            f.setGrossRate(lineGrossRate);

            f.setLineGrossTotal(lineGrossTotal);
            f.setBillGrossTotal(BigDecimal.ZERO);
            f.setGrossTotal(lineGrossTotal);

            if (f.getLineNetRate() == null || f.getLineNetRate().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal lineNetRate = quantity.compareTo(BigDecimal.ZERO) > 0
                        ? lineNetTotal.divide(quantity, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                f.setLineNetRate(lineNetRate);
            }

            BigDecimal billNetRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillNetRate(billNetRate);

            BigDecimal netRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setNetRate(netRate);

            Item item = bi.getItem();
            BigDecimal freeQuantity = BigDecimalUtil.valueOrZero(f.getFreeQuantity());
            BigDecimal totalQuantity = quantity.add(freeQuantity);
            BigDecimal lineNetRateForCalc = BigDecimalUtil.valueOrZero(f.getLineNetRate());

            if (item instanceof Ampp) {
                unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
                if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
                    unitsPerPack = BigDecimal.ONE;
                }
                BigDecimal totalQuantityInUnits = totalQuantity.multiply(unitsPerPack);
                BigDecimal lineNetRatePerUnit = lineNetRateForCalc.divide(unitsPerPack, 4, RoundingMode.HALF_UP);

                if (configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true)) {
                    f.setValueAtPurchaseRate(totalQuantityInUnits.multiply(lineNetRatePerUnit));
                } else {
                    f.setValueAtPurchaseRate(lineNetRateForCalc.multiply(quantity));
                }
            } else {
                BigDecimal totalQuantityInUnits = totalQuantity;
                if (configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true)) {
                    f.setValueAtPurchaseRate(totalQuantityInUnits.multiply(lineNetRateForCalc));
                } else {
                    f.setValueAtPurchaseRate(lineNetRateForCalc.multiply(quantity));
                }
            }
        }

        aggregateBillTotalsFromDistributedItems(bill, billItems);
    }

    private void aggregateBillTotalsFromDistributedItems(Bill bill, List<BillItem> billItems) {
        BigDecimal totalNetTotal = BigDecimal.ZERO;
        BigDecimal totalLineNetTotal = BigDecimal.ZERO;

        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalCostValueFree = BigDecimal.ZERO;
        BigDecimal totalCostValueNonFree = BigDecimal.ZERO;

        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f != null) {
                totalNetTotal = totalNetTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                totalLineNetTotal = totalLineNetTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));

                BigDecimal itemCostValue = BigDecimalUtil.valueOrZero(f.getValueAtCostRate());
                totalCostValue = totalCostValue.add(itemCostValue);

                BigDecimal qtyByUnits = BigDecimalUtil.valueOrZero(f.getQuantityByUnits());
                BigDecimal freeQtyByUnits = BigDecimalUtil.valueOrZero(f.getFreeQuantityByUnits());
                BigDecimal costRatePerUnit = BigDecimalUtil.valueOrZero(f.getTotalCostRate());

                BigDecimal costValueNonFree = BigDecimalUtil.multiply(qtyByUnits, costRatePerUnit);
                BigDecimal costValueFree = BigDecimalUtil.multiply(freeQtyByUnits, costRatePerUnit);

                totalCostValueNonFree = totalCostValueNonFree.add(costValueNonFree);
                totalCostValueFree = totalCostValueFree.add(costValueFree);
            }
        }

        bill.setNetTotal(totalNetTotal.doubleValue());
        bill.setTotal(totalLineNetTotal.doubleValue());

        if (bill.getBillFinanceDetails() != null) {
            BillFinanceDetails bfd = bill.getBillFinanceDetails();
            bfd.setTotalCostValue(totalCostValue);
            bfd.setTotalCostValueFree(totalCostValueFree);
            bfd.setTotalCostValueNonFree(totalCostValueNonFree);
        }
    }

    public void recalculateFinancialsBeforeAddingBillItem(BillItemFinanceDetails billItemFinanceDetails) {
        if (billItemFinanceDetails == null || billItemFinanceDetails.getBillItem() == null) {
            return;
        }
        BillItem billItem = billItemFinanceDetails.getBillItem();
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();

        Double prPerUnit;
        Double rrPerUnit;
        BigDecimal qty = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getQuantity());
        BigDecimal freeQty = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getFreeQuantity());
        BigDecimal lineGrossRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineGrossRate());
        BigDecimal lineDiscountRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineDiscountRate());
        BigDecimal retailRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getRetailSaleRate());

        BigDecimal lineGrossTotal = lineGrossRate.multiply(qty);
        BigDecimal lineDiscountValue = lineDiscountRate.multiply(qty);
        BigDecimal lineNetTotal = lineGrossTotal.subtract(lineDiscountValue);
        BigDecimal lineNetRate = BigDecimalUtil.isPositive(qty)
                ? lineNetTotal.divide(qty, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Item item = billItemFinanceDetails.getBillItem().getItem();
        BigDecimal totalQty = qty.add(freeQty);

        BigDecimal unitsPerPack;
        BigDecimal qtyInUnits;
        BigDecimal freeQtyInUnits;
        BigDecimal totalQtyInUnits;
        if (item instanceof Ampp) {
            double dblVal = item.getDblValue();
            unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
            qtyInUnits = qty.multiply(unitsPerPack);
            freeQtyInUnits = freeQty.multiply(unitsPerPack);
            totalQtyInUnits = totalQty.multiply(unitsPerPack);
            prPerUnit = lineNetRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP).doubleValue();
            rrPerUnit = retailRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP).doubleValue();
        } else {
            unitsPerPack = BigDecimal.ONE;
            qtyInUnits = qty;
            freeQtyInUnits = freeQty;
            totalQtyInUnits = totalQty;
            prPerUnit = lineNetRate.doubleValue();
            rrPerUnit = retailRate.doubleValue();
        }

        billItemFinanceDetails.setUnitsPerPack(unitsPerPack);
        billItemFinanceDetails.setQuantityByUnits(qtyInUnits);
        billItemFinanceDetails.setFreeQuantityByUnits(freeQtyInUnits);
        billItemFinanceDetails.setTotalQuantityByUnits(totalQtyInUnits);

        BigDecimal lineCostRate = BigDecimalUtil.isPositive(totalQtyInUnits)
                ? lineNetTotal.divide(totalQtyInUnits, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal retailValue = BigDecimal.valueOf(rrPerUnit).multiply(totalQtyInUnits);
        BigDecimal purchaseValue = BigDecimal.valueOf(prPerUnit).multiply(totalQtyInUnits);

        billItemFinanceDetails.setLineGrossRate(lineGrossRate);
        billItemFinanceDetails.setLineNetRate(lineNetRate);

        billItemFinanceDetails.setRetailSaleRatePerUnit(
                BigDecimalUtil.isPositive(unitsPerPack)
                ? retailRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        );

        BigDecimal wholesaleRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getWholesaleRate());
        billItemFinanceDetails.setWholesaleRatePerUnit(
                BigDecimalUtil.isPositive(unitsPerPack)
                ? wholesaleRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        );

        billItemFinanceDetails.setLineDiscount(lineDiscountValue);
        billItemFinanceDetails.setLineGrossTotal(lineGrossTotal);
        billItemFinanceDetails.setLineNetTotal(lineNetTotal);
        billItemFinanceDetails.setLineCost(lineNetTotal);
        billItemFinanceDetails.setLineCostRate(lineCostRate);
        billItemFinanceDetails.setTotalQuantity(totalQty);

        billItemFinanceDetails.setCostRate(lineCostRate.multiply(unitsPerPack));

        billItemFinanceDetails.setPurchaseRate(
                BigDecimalUtil.isPositive(qty)
                ? lineNetTotal.divide(qty, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        );

        billItemFinanceDetails.setValueAtCostRate(
                lineCostRate.multiply(totalQtyInUnits)
        );

        if (configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true)) {
            BigDecimal purchaseRatePerUnit = BigDecimal.valueOf(prPerUnit);
            billItemFinanceDetails.setValueAtPurchaseRate(
                    totalQtyInUnits.multiply(purchaseRatePerUnit)
            );
        } else {
            billItemFinanceDetails.setValueAtPurchaseRate(
                    billItemFinanceDetails.getLineNetRate().multiply(qty)
            );
        }

        if (BigDecimalUtil.isPositive(totalQtyInUnits)) {
            BigDecimal retailRatePerUnit = BigDecimal.valueOf(rrPerUnit);
            billItemFinanceDetails.setValueAtRetailRate(
                    totalQtyInUnits.multiply(retailRatePerUnit)
            );

            BigDecimal wholesaleRatePerUnit = billItemFinanceDetails.getWholesaleRatePerUnit();
            if (wholesaleRatePerUnit != null && wholesaleRatePerUnit.compareTo(BigDecimal.ZERO) > 0) {
                billItemFinanceDetails.setValueAtWholesaleRate(
                        totalQtyInUnits.multiply(wholesaleRatePerUnit)
                );
            }
        } else {
            billItemFinanceDetails.setValueAtRetailRate(BigDecimal.ZERO);
            billItemFinanceDetails.setValueAtWholesaleRate(BigDecimal.ZERO);
        }

        billItemFinanceDetails.setProfitMargin(calculateProfitMarginForPurchasesBigDecimal(billItemFinanceDetails.getBillItem()));

        BillItem bi = billItemFinanceDetails.getBillItem();
        if (bi != null) {
            bi.setRate(lineGrossRate.doubleValue());
            bi.setQty(qty.doubleValue());
            bi.setNetRate(lineNetRate.doubleValue());
            bi.setGrossValue(lineGrossTotal.doubleValue());
            BigDecimal biNetValue = lineNetRate.multiply(qty);
            bi.setNetValue(0 - biNetValue.doubleValue());
        }

        pbi.setRetailRate(rrPerUnit);
        pbi.setRetailRateInUnit(rrPerUnit);
        pbi.setRetailRatePack(retailRate.doubleValue());

        pbi.setRetailPackValue(retailValue.doubleValue());
        pbi.setRetailValue(retailValue.doubleValue());

        pbi.setPurchaseRate(prPerUnit);
        pbi.setPurchaseRatePack(lineNetRate.doubleValue());

        pbi.setPurchaseRatePackValue(purchaseValue.doubleValue());
        pbi.setPurchaseValue(purchaseValue.doubleValue());

        pbi.setCostRate(lineCostRate.doubleValue());

        pbi.setQty(qtyInUnits.doubleValue());
        pbi.setFreeQty(freeQtyInUnits.doubleValue());
    }

    public void recalculateFinancialsBeforeAddingBillItemPreservingDistributedCosts(BillItemFinanceDetails billItemFinanceDetails) {
        BillItem billItem = billItemFinanceDetails.getBillItem();
        if (billItem == null || billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();
        Item item = billItem.getItem();

        BigDecimal lineNetRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineNetRate());
        BigDecimal quantity = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getQuantity());
        BigDecimal freeQuantity = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getFreeQuantity());
        BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getUnitsPerPack());
        if (unitsPerPack.compareTo(BigDecimal.ZERO) == 0) {
            unitsPerPack = BigDecimal.ONE;
        }

        BigDecimal totalQtyByUnits = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getTotalQuantityByUnits());
        BigDecimal totalCostRate = billItemFinanceDetails.getTotalCostRate();

        Double prPerUnit;
        if (item instanceof Ampp) {
            prPerUnit = lineNetRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP).doubleValue();
        } else {
            prPerUnit = lineNetRate.doubleValue();
        }

        pbi.setPurchaseRate(prPerUnit);
        pbi.setPurchaseRatePack(lineNetRate.doubleValue());

        BigDecimal purchaseValue;
        if (configOptionApplicationController.getBooleanValueByKey("Purchase Value Includes Free Items", true)) {
            purchaseValue = BigDecimal.valueOf(prPerUnit).multiply(totalQtyByUnits);
        } else {
            BigDecimal paidQtyInUnits = quantity.multiply(unitsPerPack);
            purchaseValue = BigDecimal.valueOf(prPerUnit).multiply(paidQtyInUnits);
        }
        pbi.setPurchaseRatePackValue(purchaseValue.doubleValue());
        pbi.setPurchaseValue(purchaseValue.doubleValue());

        if (totalCostRate != null && totalCostRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costRate = BigDecimalUtil.multiply(totalCostRate, unitsPerPack);
            billItemFinanceDetails.setCostRate(costRate);

            BigDecimal valueAtCostRate = BigDecimalUtil.multiply(totalQtyByUnits, totalCostRate);
            billItemFinanceDetails.setValueAtCostRate(valueAtCostRate);

            pbi.setCostRate(totalCostRate.doubleValue());
        }

        billItemFinanceDetails.setValueAtPurchaseRate(purchaseValue);
    }

    /**
     * Apply finance details to the pharmaceutical bill item.
     *
     * @param bi BillItem to process
     */
    private void applyFinanceDetailsToPharmaceutical(BillItem bi) {
        applyFinanceDetailsToPharmaceutical(bi, false);
    }

    /**
     * @param bi BillItem to process
     * @param preserveDistributedCosts If true, preserve costRate/valueAtCostRate that include bill-level costs
     */
    private void applyFinanceDetailsToPharmaceutical(BillItem bi, boolean preserveDistributedCosts) {
        if (bi == null) {
            return;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();

        if (f == null || pbi == null) {
            return;
        }

        if (preserveDistributedCosts) {
            recalculateFinancialsBeforeAddingBillItemPreservingDistributedCosts(f);
        } else {
            recalculateFinancialsBeforeAddingBillItem(f);
        }

        if (bi.getItem() instanceof Ampp) {
            BigDecimal unitsPerPack = Optional.ofNullable(f.getUnitsPerPack()).orElse(BigDecimal.ONE);
            BigDecimal qtyPacks = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal qtyUnits = qtyPacks.multiply(unitsPerPack);
            BigDecimal freeQtyPacks = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal freeQtyUnits = freeQtyPacks.multiply(unitsPerPack);

            pbi.setQty(qtyUnits.doubleValue());
            pbi.setQtyInUnit(pbi.getQty());
            pbi.setQtyPacks(qtyPacks.doubleValue());

            pbi.setFreeQty(freeQtyUnits.doubleValue());
            pbi.setFreeQtyInUnit(pbi.getFreeQty());
            pbi.setFreeQtyPacks(freeQtyPacks.doubleValue());

            BigDecimal totalCostRate = Optional.ofNullable(f.getTotalCostRate()).orElse(BigDecimal.ZERO);
            pbi.setCostRate(totalCostRate.doubleValue());

            pbi.setRetailRate(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());
            pbi.setRetailRatePack(Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO).doubleValue());
            pbi.setRetailRateInUnit(Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());

            pbi.setWholesaleRate(Optional.ofNullable(f.getWholesaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue());
            pbi.setWholesaleRatePack(Optional.ofNullable(f.getWholesaleRate()).orElse(BigDecimal.ZERO).doubleValue());

            bi.setQty(qtyPacks.doubleValue());
            bi.setRate(pbi.getPurchaseRatePack());
        } else {
            BigDecimal qty = Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO);
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO);

            pbi.setQty(qty.doubleValue());
            pbi.setQtyInUnit(pbi.getQty());
            pbi.setQtyPacks(pbi.getQty());

            pbi.setFreeQty(freeQty.doubleValue());
            pbi.setFreeQtyInUnit(pbi.getFreeQty());
            pbi.setFreeQtyPacks(pbi.getFreeQty());

            BigDecimal totalCostRate = Optional.ofNullable(f.getTotalCostRate()).orElse(BigDecimal.ZERO);
            pbi.setCostRate(totalCostRate.doubleValue());

            double r = Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue();
            pbi.setRetailRate(r);
            pbi.setRetailRatePack(r);
            pbi.setRetailRateInUnit(r);

            double wr = Optional.ofNullable(f.getWholesaleRatePerUnit()).orElse(BigDecimal.ZERO).doubleValue();
            pbi.setWholesaleRate(wr);
            pbi.setWholesaleRatePack(wr);

            bi.setQty(qty.doubleValue());
            bi.setRate(pbi.getPurchaseRate());
        }
    }

    public double calculateProfitMarginForPurchases(BillItem bi) {
        return calculateProfitMarginForPurchasesBigDecimal(bi).doubleValue();
    }

    public BigDecimal calculateProfitMarginForPurchasesBigDecimal(BillItem bi) {
        if (bi == null) {
            return BigDecimal.ZERO;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        if (f == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal effectiveCost = BigDecimalUtil.valueOrZero(f.getTotalCost());
        if (effectiveCost.compareTo(BigDecimal.ZERO) == 0) {
            effectiveCost = BigDecimalUtil.valueOrZero(
                    f.getNetTotal() != null ? f.getNetTotal() : f.getLineNetTotal()
            );
        }
        if (effectiveCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalUnits = BigDecimalUtil.valueOrZero(f.getTotalQuantityByUnits());
        BigDecimal retailPerUnit = BigDecimalUtil.valueOrZero(f.getRetailSaleRatePerUnit());
        BigDecimal totalPotentialIncome = totalUnits.multiply(retailPerUnit);

        return totalPotentialIncome.subtract(effectiveCost)
                .divide(effectiveCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private void recalculateProfitMarginsForAllItems() {
        if (getBillItems() == null || getBillItems().isEmpty()) {
            return;
        }
        for (BillItem item : getBillItems()) {
            if (item != null && item.getBillItemFinanceDetails() != null) {
                BigDecimal profitMargin = calculateProfitMarginForPurchasesBigDecimal(item);
                item.getBillItemFinanceDetails().setProfitMargin(profitMargin);
            }
        }
    }

    private void ensureBillDiscountSynchronization() {
        if (getGrnBill() == null) {
            return;
        }
        if (getGrnBill().getBillFinanceDetails() == null) {
            getGrnBill().setBillFinanceDetails(new BillFinanceDetails(getGrnBill()));
        }
        getGrnBill().getBillFinanceDetails().setBillDiscount(BigDecimal.valueOf(getGrnBill().getDiscount()));
        getGrnBill().getBillFinanceDetails().setBillTaxValue(BigDecimal.valueOf(getGrnBill().getTax()));
    }

    public void calculateRetailSaleValueAndFreeValueAtPurchaseRate(Bill b) {
        double sale = 0.0;
        double free = 0.0;

        for (BillItem i : b.getBillItems()) {
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            if (ph == null) {
                continue;
            }
            sale += (ph.getQty() + ph.getFreeQty()) * ph.getRetailRate();
            free += ph.getFreeQty() * ph.getPurchaseRate();
        }
        b.setSaleValue(Math.abs(sale));
        b.setFreeValue(Math.abs(free));
    }

    /**
     * Ported from {@code GrnCostingController.updateBalanceForGrn(Bill)},
     * changed from {@code void} to returning the computed value so it can be
     * carried on {@link GrnApproveRequest#setBalanceValue}. Still sets
     * {@code grn.setBalance(...)} in memory too, for print-preview display.
     */
    private double updateBalanceForGrn(Bill grn) {
        if (grn == null || grn.getPaymentMethod() == null) {
            return 0.0;
        }
        double balance;
        switch (grn.getPaymentMethod()) {
            case Agent:
            case Card:
            case Cash:
            case Cheque:
            case MultiplePaymentMethods:
            case OnCall:
            case OnlineSettlement:
            case PatientDeposit:
            case Slip:
            case Staff:
            case YouOweMe:
            case ewallet:
                balance = 0.0;
                break;
            case Credit:
                balance = Math.abs(grn.getNetTotal());
                break;
            default:
                balance = 0.0;
        }
        grn.setBalance(balance);
        return balance;
    }

    // ==================================================================
    // Validation
    // ==================================================================
    public String errorCheck(Bill b, List<BillItem> billItems) {
        String msg = "";

        if (b.getInvoiceNumber() == null || b.getInvoiceNumber().trim().isEmpty()) {
            msg = "Please Fill invoice number";
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Cheque) {
            if (b.getBank() == null || b.getBank().getId() == null || b.getChequeRefNo() == null) {
                msg = "Please select Cheque Number and Bank";
            }
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Slip) {
            if (b.getBank() == null || b.getBank().getId() == null || b.getComments() == null) {
                msg = "Please Fill Memo and Bank";
            }
        }

        if (billItems.isEmpty()) {
            msg = "There is no Item to receive";
        }

        if (checkItemBatch(billItems)) {
            msg = "Please Fill Batch deatail and Sale Price to All Item";
        }

        if (b.getReferenceInstitution() == null) {
            msg = "Please Fill Reference Institution";
        }

        String quantityValidationMsg = validateGrnQuantities(billItems);
        if (!quantityValidationMsg.isEmpty()) {
            msg = quantityValidationMsg;
        }

        String discountValidationMsg = validateLineDiscountRates(billItems);
        if (!discountValidationMsg.isEmpty()) {
            msg = discountValidationMsg;
        }

        return msg;
    }

    private String validateLineDiscountRates(List<BillItem> billItems) {
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }
            BigDecimal grossRate = f.getLineGrossRate();
            BigDecimal discountRate = f.getLineDiscountRate();
            if (grossRate == null || discountRate == null) {
                continue;
            }
            if (discountRate.compareTo(grossRate) > 0) {
                String itemName = bi.getItem() != null ? bi.getItem().getName() : "Unknown Item";
                return "Item " + itemName + ": Discount Rate cannot exceed Purchase Rate (results in a negative net rate)";
            }
        }
        return "";
    }

    private String validateGrnQuantities(List<BillItem> billItems) {
        if (getApproveBill() == null) {
            return "";
        }

        // Aggregate THIS GRN's own lines by PO item first. A single PO line can
        // be split/duplicated across multiple GRN lines for the same item, and
        // each line individually staying under the remaining qty does not
        // guarantee their SUM does -- checking line-by-line against
        // "previously received (other GRNs) + this one line" let an aggregate
        // over-receipt slip through silently (#22893: 20 received against 15
        // ordered, split across 2 duplicate lines, no validation error).
        Map<Long, Double> currentGrnQtyByPoItem = new HashMap<>();
        Map<Long, Double> currentGrnFreeQtyByPoItem = new HashMap<>();
        Map<Long, PharmaceuticalBillItem> poItemById = new HashMap<>();
        Map<Long, String> itemNameByPoItem = new HashMap<>();

        for (BillItem grnItem : billItems) {
            if (grnItem.getReferanceBillItem() == null || grnItem.getPharmaceuticalBillItem() == null) {
                continue;
            }

            BillItem purchaseOrderItem = grnItem.getReferanceBillItem();
            PharmaceuticalBillItem poItem = purchaseOrderItem.getPharmaceuticalBillItem();

            if (poItem == null || poItem.getId() == null) {
                continue;
            }

            PharmaceuticalBillItem currentGrnPbi = grnItem.getPharmaceuticalBillItem();

            Long poItemId = poItem.getId();
            currentGrnQtyByPoItem.merge(poItemId, currentGrnPbi.getQty(), Double::sum);
            currentGrnFreeQtyByPoItem.merge(poItemId, currentGrnPbi.getFreeQty(), Double::sum);
            poItemById.putIfAbsent(poItemId, poItem);
            itemNameByPoItem.putIfAbsent(poItemId, grnItem.getItem() != null ? grnItem.getItem().getName() : "Unknown Item");
        }

        boolean enableFreeQtyValidation = configOptionApplicationController.getBooleanValueByKey("Enable Free Quantity Validation in GRN", false);

        for (Map.Entry<Long, PharmaceuticalBillItem> e : poItemById.entrySet()) {
            Long poItemId = e.getKey();
            PharmaceuticalBillItem poItem = e.getValue();

            double orderedQty = poItem.getQty();
            double orderedFreeQty = poItem.getFreeQty();
            double currentGrnQty = currentGrnQtyByPoItem.getOrDefault(poItemId, 0.0);
            double currentGrnFreeQty = currentGrnFreeQtyByPoItem.getOrDefault(poItemId, 0.0);

            double previouslyReceivedQty = calculateRemainigQtyFromOrder(poItem);
            double previouslyReceivedFreeQty = calculateRemainingFreeQtyFromOrder(poItem);

            if (orderedQty < previouslyReceivedQty + currentGrnQty) {
                return "Item " + itemNameByPoItem.get(poItemId) + " cannot receive " + currentGrnQty
                        + " as it exceeds ordered quantity. Ordered: " + orderedQty + ", Already received: " + previouslyReceivedQty
                        + ", Remaining: " + (orderedQty - previouslyReceivedQty);
            }

            if (enableFreeQtyValidation && orderedFreeQty < previouslyReceivedFreeQty + currentGrnFreeQty) {
                return "Item " + itemNameByPoItem.get(poItemId) + " cannot receive " + currentGrnFreeQty
                        + " free quantity as it exceeds ordered free quantity. Ordered free: " + orderedFreeQty
                        + ", Already received free: " + previouslyReceivedFreeQty
                        + ", Remaining free: " + (orderedFreeQty - previouslyReceivedFreeQty);
            }
        }

        return "";
    }

    public boolean checkItemBatch(List<BillItem> list) {
        for (BillItem i : list) {
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            if (ph == null) {
                continue;
            }
            if (ph.getQty() != 0.0 || ph.getFreeQty() != 0.0) {
                if (ph.getDoe() == null || ph.getStringValue() == null || ph.getStringValue().trim().isEmpty()) {
                    return true;
                }
            }
            if (ph.getQty() != 0.0 && ph.getPurchaseRate() > ph.getRetailRate()) {
                return true;
            }
        }
        return false;
    }

    private boolean isPurchaseOrderFullyReceived(Bill purchaseOrderBill) {
        if (purchaseOrderBill == null) {
            return false;
        }

        List<PharmaceuticalBillItem> orderItems = pharmaceuticalBillItemFacade.getPharmaceuticalBillItems(purchaseOrderBill);

        if (orderItems == null || orderItems.isEmpty()) {
            return true;
        }

        for (PharmaceuticalBillItem orderItem : orderItems) {
            double calculatedReturns = calculateRemainigQtyFromOrder(orderItem);
            double remainingQty = Math.abs(orderItem.getQtyInUnit()) - Math.abs(calculatedReturns);
            double remainingFreeQty = orderItem.getFreeQty() - calculateRemainingFreeQtyFromOrder(orderItem);

            if (remainingQty > 0 || remainingFreeQty > 0) {
                return false;
            }
        }

        return true;
    }

    // ==================================================================
    // Purchase-order remaining-quantity helpers (JPQL reads only)
    // ==================================================================
    public double calculateRemainigQtyFromOrder(PharmaceuticalBillItem po) {
        double billed = getTotalQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN);
        double cancelled = getTotalQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN_CANCELLED);
        double recieveNet = Math.abs(billed) - Math.abs(cancelled);
        return Math.abs(recieveNet);
    }

    public double calculateRemainingFreeQtyFromOrder(PharmaceuticalBillItem po) {
        double billed = getTotalFreeQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN);
        double cancelled = getTotalFreeQty(po.getBillItem(), BillTypeAtomic.PHARMACY_GRN_CANCELLED);
        double recieveNet = Math.abs(billed) - Math.abs(cancelled);
        return Math.abs(recieveNet);
    }

    public double getTotalQty(BillItem pobi, BillTypeAtomic billTypeAtomic) {
        String sql = "Select COALESCE(SUM(COALESCE(bi.pharmaceuticalBillItem.qty,0)),0) "
                + " from BillItem bi "
                + " where (bi.retired=false or bi.retired is null) "
                + " and (bi.bill.retired=false or bi.bill.retired is null) "
                + " and bi.referanceBillItem=:pobi "
                + " and bi.bill.billTypeAtomic=:bta";
        Map<String, Object> hm = new HashMap<>();
        hm.put("pobi", pobi);
        hm.put("bta", billTypeAtomic);
        return pharmaceuticalBillItemFacade.findDoubleByJpql(sql, hm);
    }

    public double getTotalFreeQty(BillItem pobi, BillTypeAtomic billTypeAtomic) {
        String sql = "Select COALESCE(SUM(COALESCE(bi.pharmaceuticalBillItem.freeQty,0)),0) "
                + " from BillItem bi "
                + " where (bi.retired=false or bi.retired is null) "
                + " and (bi.bill.retired=false or bi.bill.retired is null) "
                + " and bi.referanceBillItem=:pobi "
                + " and bi.bill.billTypeAtomic=:bta";
        Map<String, Object> hm = new HashMap<>();
        hm.put("pobi", pobi);
        hm.put("bta", billTypeAtomic);
        return pharmaceuticalBillItemFacade.findDoubleByJpql(sql, hm);
    }

    public double getRemainingQty(PharmaceuticalBillItem ph) {
        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id = " + ph.getBillItem().getReferanceBillItem().getId();
        PharmaceuticalBillItem po = pharmaceuticalBillItemFacade.findFirstByJpql(sql);

        double poQty, remainsFree;
        poQty = po.getQtyInUnit();
        remainsFree = poQty - calculateRemainingFreeQtyFromOrder(po);

        return remainsFree;
    }

    // ==================================================================
    // Bulk lookup maps (used by generateBillComponent()/addFreeItemNotInPo())
    // ==================================================================
    private Map<Long, Double> buildReceivedQtyMap(Bill poBill, BillTypeAtomic billTypeAtomic) {
        String jpql = "SELECT bi.referanceBillItem.id,"
                + " COALESCE(SUM(COALESCE(bi.pharmaceuticalBillItem.qty, 0)), 0)"
                + " FROM BillItem bi"
                + " WHERE bi.referanceBillItem.bill = :poBill"
                + " AND (bi.retired = false OR bi.retired IS NULL)"
                + " AND (bi.bill.retired = false OR bi.bill.retired IS NULL)"
                + " AND bi.bill.billTypeAtomic = :bta"
                + " GROUP BY bi.referanceBillItem.id";
        Map<String, Object> params = new HashMap<>();
        params.put("poBill", poBill);
        params.put("bta", billTypeAtomic);
        Map<Long, Double> result = new HashMap<>();
        List<Object> rows = billItemFacade.findObjects(jpql, params);
        if (rows != null) {
            for (Object row : rows) {
                Object[] cols = (Object[]) row;
                Long billItemId = ((Number) cols[0]).longValue();
                double qty = cols[1] instanceof Number ? ((Number) cols[1]).doubleValue() : 0.0;
                result.put(billItemId, qty);
            }
        }
        return result;
    }

    private Map<Long, Double> buildReceivedFreeQtyMap(Bill poBill, BillTypeAtomic billTypeAtomic) {
        String jpql = "SELECT bi.referanceBillItem.id,"
                + " COALESCE(SUM(COALESCE(bi.pharmaceuticalBillItem.freeQty, 0)), 0)"
                + " FROM BillItem bi"
                + " WHERE bi.referanceBillItem.bill = :poBill"
                + " AND (bi.retired = false OR bi.retired IS NULL)"
                + " AND (bi.bill.retired = false OR bi.bill.retired IS NULL)"
                + " AND bi.bill.billTypeAtomic = :bta"
                + " GROUP BY bi.referanceBillItem.id";
        Map<String, Object> params = new HashMap<>();
        params.put("poBill", poBill);
        params.put("bta", billTypeAtomic);
        Map<Long, Double> result = new HashMap<>();
        List<Object> rows = billItemFacade.findObjects(jpql, params);
        if (rows != null) {
            for (Object row : rows) {
                Object[] cols = (Object[]) row;
                Long billItemId = ((Number) cols[0]).longValue();
                double qty = cols[1] instanceof Number ? ((Number) cols[1]).doubleValue() : 0.0;
                result.put(billItemId, qty);
            }
        }
        return result;
    }

    private Map<Long, double[]> buildLastRatesMap(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return new HashMap<>();
        }
        String jpql = "SELECT bi.item.id,"
                + " bi.billItemFinanceDetails.lineGrossRate,"
                + " bi.billItemFinanceDetails.retailSaleRate"
                + " FROM BillItem bi"
                + " WHERE bi.retired = false"
                + " AND bi.bill.cancelled = false"
                + " AND bi.item IN :items"
                + " AND (bi.bill.billType = :t OR bi.bill.billType = :t1)"
                + " ORDER BY bi.id DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("items", items);
        params.put("t", BillType.PharmacyGrnBill);
        params.put("t1", BillType.PharmacyPurchaseBill);
        Map<Long, double[]> result = new HashMap<>();
        List<Object> rows = billItemFacade.findObjects(jpql, params);
        if (rows != null) {
            for (Object row : rows) {
                Object[] cols = (Object[]) row;
                Long itemId = ((Number) cols[0]).longValue();
                if (!result.containsKey(itemId)) {
                    double pr = cols[1] instanceof Number ? ((Number) cols[1]).doubleValue() : 0.0;
                    double rr = cols[2] instanceof Number ? ((Number) cols[2]).doubleValue() : 0.0;
                    result.put(itemId, new double[]{pr, rr});
                }
            }
        }
        return result;
    }

    /**
     * Builds new in-memory GRN lines from the source purchase order's
     * remaining (ordered minus already-GRN'd) quantities. In-memory only, no
     * persistence. Ported verbatim from
     * {@code GrnCostingController.generateBillComponent()} (the no-arg
     * overload used by the WithSaveApprove flow -- the {@code Bill}-argument
     * overload and {@code generateBillComponentAll()} belong to the
     * out-of-scope import/legacy flows and are not ported).
     */
    private void generateBillComponent() {
        Bill poBill = getApproveBill();
        Map<Long, Double> grnQtyMap = buildReceivedQtyMap(poBill, BillTypeAtomic.PHARMACY_GRN);
        Map<Long, Double> grnCancelledQtyMap = buildReceivedQtyMap(poBill, BillTypeAtomic.PHARMACY_GRN_CANCELLED);
        Map<Long, Double> grnFreeQtyMap = buildReceivedFreeQtyMap(poBill, BillTypeAtomic.PHARMACY_GRN);
        Map<Long, Double> grnCancelledFreeQtyMap = buildReceivedFreeQtyMap(poBill, BillTypeAtomic.PHARMACY_GRN_CANCELLED);

        List<PharmaceuticalBillItem> poBillItems = pharmaceuticalBillItemFacade.getPharmaceuticalBillItemsWithItemAndCategory(poBill);

        List<Item> allPoItems = new ArrayList<>();
        for (PharmaceuticalBillItem pbi : poBillItems) {
            if (pbi.getBillItem() != null && pbi.getBillItem().getItem() != null) {
                allPoItems.add(pbi.getBillItem().getItem());
            }
        }
        Map<Long, double[]> lastRatesMap = buildLastRatesMap(allPoItems);

        for (PharmaceuticalBillItem pbiInApprovedOrder : poBillItems) {

            if (pbiInApprovedOrder.getBillItem() == null) {
                continue;
            }

            Long poItemId = pbiInApprovedOrder.getBillItem().getId();
            double receivedQty = Math.abs(grnQtyMap.getOrDefault(poItemId, 0.0))
                    - Math.abs(grnCancelledQtyMap.getOrDefault(poItemId, 0.0));
            double receivedFreeQty = Math.abs(grnFreeQtyMap.getOrDefault(poItemId, 0.0))
                    - Math.abs(grnCancelledFreeQtyMap.getOrDefault(poItemId, 0.0));

            double remains = Math.abs(pbiInApprovedOrder.getQty()) - Math.abs(receivedQty);
            double remainFreeQty = pbiInApprovedOrder.getFreeQty() - Math.abs(receivedFreeQty);

            if (remains > 0 || remainFreeQty > 0) {
                BillItem newlyCreatedBillItemForGrn = new BillItem();
                newlyCreatedBillItemForGrn.setSearialNo(getBillItems().size());
                newlyCreatedBillItemForGrn.setItem(pbiInApprovedOrder.getBillItem().getItem());
                newlyCreatedBillItemForGrn.setReferanceBillItem(pbiInApprovedOrder.getBillItem());

                if (pbiInApprovedOrder.getBillItem().getItem() instanceof Ampp) {
                    double unitsPerPack = pbiInApprovedOrder.getBillItem().getItem().getDblValue();
                    unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                    newlyCreatedBillItemForGrn.setQty(remains / unitsPerPack);
                    newlyCreatedBillItemForGrn.setTmpQty(remains / unitsPerPack);
                    newlyCreatedBillItemForGrn.setTmpFreeQty(remainFreeQty / unitsPerPack);
                } else {
                    newlyCreatedBillItemForGrn.setQty(remains);
                    newlyCreatedBillItemForGrn.setTmpFreeQty(remainFreeQty);
                }

                PharmaceuticalBillItem newlyCreatedPbiForGrn = new PharmaceuticalBillItem();
                newlyCreatedPbiForGrn.setBillItem(newlyCreatedBillItemForGrn);

                newlyCreatedBillItemForGrn.setPreviousRecieveQtyInUnit(remains);
                newlyCreatedBillItemForGrn.setPreviousRecieveFreeQtyInUnit(remainFreeQty);

                newlyCreatedPbiForGrn.setQty(remains);
                newlyCreatedPbiForGrn.setFreeQty(remainFreeQty);

                double pr = pbiInApprovedOrder.getPurchaseRate();
                double rr = pbiInApprovedOrder.getRetailRate();

                if (pr == 0.0) {
                    double[] rates = lastRatesMap.get(newlyCreatedBillItemForGrn.getItem().getId());
                    if (rates != null && rates[0] > 0.0) {
                        pr = rates[0];
                    }
                }

                if (rr == 0.0) {
                    double[] rates = lastRatesMap.get(newlyCreatedBillItemForGrn.getItem().getId());
                    if (rates != null && rates[1] > 0.0) {
                        rr = rates[1];
                    }
                }

                newlyCreatedPbiForGrn.setPurchaseRate(pr);
                newlyCreatedPbiForGrn.setRetailRate(rr);

                double lineGrossRateForBillItem = pr;
                double retailRateForBillItem = rr;

                if (pbiInApprovedOrder.getBillItem().getItem() instanceof Ampp) {
                    double unitsPerPack = pbiInApprovedOrder.getBillItem().getItem().getDblValue();
                    unitsPerPack = unitsPerPack > 0 ? unitsPerPack : 1.0;
                    lineGrossRateForBillItem = pr * unitsPerPack;

                    double retailRatePack = pbiInApprovedOrder.getRetailRatePack();
                    if (retailRatePack > 0) {
                        retailRateForBillItem = retailRatePack;
                    } else if (pbiInApprovedOrder.getRetailRate() > 0) {
                        retailRateForBillItem = pbiInApprovedOrder.getRetailRate() * unitsPerPack;
                    } else {
                        retailRateForBillItem = rr;
                    }
                }

                newlyCreatedBillItemForGrn.setPharmaceuticalBillItem(newlyCreatedPbiForGrn);

                BillItemFinanceDetails fd = new BillItemFinanceDetails(newlyCreatedBillItemForGrn);

                fd.setQuantity(BigDecimal.valueOf(newlyCreatedBillItemForGrn.getQty()));
                fd.setFreeQuantity(BigDecimal.valueOf(newlyCreatedBillItemForGrn.getTmpFreeQty()));
                fd.setLineGrossRate(BigDecimal.valueOf(lineGrossRateForBillItem));
                fd.setLineDiscountRate(BigDecimal.ZERO);
                fd.setRetailSaleRate(BigDecimal.valueOf(retailRateForBillItem));
                fd.setLineNetRate(BigDecimal.valueOf(lineGrossRateForBillItem));

                newlyCreatedBillItemForGrn.setBillItemFinanceDetails(fd);
                recalculateFinancialsBeforeAddingBillItem(fd);

                newlyCreatedBillItemForGrn.getPharmaceuticalBillItem().setLastPurchaseRate(pr);
                newlyCreatedBillItemForGrn.getPharmaceuticalBillItem().setLastPurchaseRateInUnit(pr);
                newlyCreatedBillItemForGrn.getPharmaceuticalBillItem().setLastPurchaseRatePack(pr * fd.getUnitsPerPack().doubleValue());

                getBillItems().add(newlyCreatedBillItemForGrn);
            }
        }
    }

    // ==================================================================
    // Authorization
    // ==================================================================
    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null",
                    action);
            return false;
        }

        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billId = null;
            if (currentGrnBillPre != null) {
                billId = currentGrnBillPre.getId();
            } else if (approveBill != null) {
                billId = approveBill.getId();
            }

            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized GRN Costing access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});

            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " GRN.");
            return false;
        }

        return true;
    }

    public String convertToWord(Double d) {
        return d == null ? "" : CommonFunctions.convertToWord(d);
    }

    // ==================================================================
    // Getters / setters
    // ==================================================================
    public SessionController getSessionController() {
        return sessionController;
    }

    public Bill getApproveBill() {
        if (approveBill == null) {
            approveBill = new BilledBill();
        }
        return approveBill;
    }

    public void setApproveBill(Bill approveBill) {
        this.approveBill = approveBill;
    }

    public Bill getCurrentGrnBillPre() {
        if (currentGrnBillPre == null) {
            currentGrnBillPre = new BilledBill();
            currentGrnBillPre.setBillType(BillType.PharmacyGrnBill);
            currentGrnBillPre.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_PRE);
            if (getApproveBill() != null) {
                currentGrnBillPre.setConsignment(getApproveBill().isConsignment());
            }
        }
        return currentGrnBillPre;
    }

    public void setCurrentGrnBillPre(Bill currentGrnBillPre) {
        this.currentGrnBillPre = currentGrnBillPre;
    }

    /**
     * Internal alias for {@link #getCurrentGrnBillPre()}, ensuring
     * BillFinanceDetails is present -- mirrors legacy
     * {@code GrnCostingController.getGrnBill()}, which the ported
     * calculation methods above call by that name.
     */
    private Bill getGrnBill() {
        Bill bill = getCurrentGrnBillPre();
        if (bill.getBillFinanceDetails() == null) {
            bill.setBillFinanceDetails(new BillFinanceDetails(bill));
        }
        return bill;
    }

    public List<BillItem> getBillItems() {
        if (getCurrentGrnBillPre().getBillItems() == null) {
            getCurrentGrnBillPre().setBillItems(new ArrayList<>());
        }
        return getCurrentGrnBillPre().getBillItems();
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public Item getFreeItemToAdd() {
        return freeItemToAdd;
    }

    public void setFreeItemToAdd(Item freeItemToAdd) {
        this.freeItemToAdd = freeItemToAdd;
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

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
    }

    public double getInsTotal() {
        return insTotal;
    }

    public void setInsTotal(double insTotal) {
        this.insTotal = insTotal;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getReferenceInstitution() {
        return referenceInstitution;
    }

    public void setReferenceInstitution(Institution referenceInstitution) {
        this.referenceInstitution = referenceInstitution;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public boolean isCompleted() {
        return currentGrnBillPre != null && currentGrnBillPre.isCompleted();
    }
}
