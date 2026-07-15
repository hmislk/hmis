/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.dto.TransferIssueItemRowDto;
import com.divudi.core.data.dto.TransferIssuePrintDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.pharmacy.TransferIssueNativeSqlService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * SessionScoped controller for the native-SQL transfer issue workflow.
 *
 * Mirrors TransferIssueForRequestsController in its responsibility but:
 *  - Uses List&lt;TransferIssueItemRowDto&gt; instead of entity BillItem lists
 *  - Uses TransferIssuePrintDto for print display instead of entity Bill
 *  - Uses TransferIssueNativeSqlService for data loading and settlement
 *  - Does NOT involve UserStockController (stock checked directly before settle)
 *
 * Bean name: transferIssueNativeSqlController (matches @Named and XHTML EL)
 * Related issue: #20583
 */
@Named
@SessionScoped
public class TransferIssueNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(TransferIssueNativeSqlController.class.getName());

    // ---- State ----
    private Bill requestedBill;
    private Long requestedBillId;
    private Bill issuedBill;
    private List<TransferIssueItemRowDto> issueItems;
    private TransferIssuePrintDto printDto;
    private boolean printPreview;
    private boolean draftMode;

    // ---- Injected ----
    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @Inject
    private PageMetadataRegistry pageMetadataRegistry;

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillNumberGenerator billNumberBean;

    @EJB
    private TransferIssueNativeSqlService transferIssueNativeSqlService;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PostConstruct
    public void init() {
        // No heavy initialization — list is loaded on navigation
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    /**
     * Loads requested items via native service and navigates to the native issue page.
     * Mirrors TransferIssueForRequestsController.navigateToPharmacyIssueForRequestsById().
     */
    public String navigateToIssueRequestNative() {
        if (requestedBillId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }

        requestedBill = billFacade.find(requestedBillId);
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("Request bill not found.");
            return null;
        }

        if (configOptionApplicationController.getBooleanValueByKey(
                "Use Save Finalize Approve Workflow for Issue for Requests", false)
                && hasPendingNativeIssueForDepartment()) {
            JsfUtil.addErrorMessage("There is already a pending fast issue for this department. Please finalize or cancel it first.");
            return null;
        }

        printPreview = false;
        printDto = null;
        draftMode = false;
        issuedBill = new BilledBill();

        boolean byPurchaseRate = configOptionApplicationController.getBooleanValueByKey(
                "Pharmacy Transfer is by Purchase Rate", false);
        boolean byCostRate = configOptionApplicationController.getBooleanValueByKey(
                "Pharmacy Transfer is by Cost Rate", false);

        issueItems = transferIssueNativeSqlService.loadRequestedItemsForIssue(
                requestedBillId,
                sessionController.getDepartment().getId(),
                byPurchaseRate,
                byCostRate);

        if (issueItems == null || issueItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items available to issue for this request.");
            return null;
        }

        return "/pharmacy/pharmacy_transfer_issue_native?faces-redirect=true";
    }

    public String navigateBackToRequestList() {
        return "/pharmacy/pharmacy_transfer_request_list?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // Save → Finalize → Approve draft workflow (native / fast issue)
    // -----------------------------------------------------------------------

    private boolean hasPendingNativeIssueForDepartment() {
        String jpql = "Select count(b) From Bill b "
                + " where b.retired=false "
                + " and b.billTypeAtomic = :bTp "
                + " and b.checked = :checked "
                + " and b.department = :dept";
        Map<String, Object> params = new HashMap<>();
        params.put("bTp", BillTypeAtomic.PHARMACY_ISSUE_PRE);
        params.put("checked", false);
        params.put("dept", sessionController.getDepartment());
        long count = billFacade.findLongByJpql(jpql, params);
        return count > 0;
    }

    public void saveDraftNativeIssue() {
        if (!isAuthorized("SAVE_DRAFT_NATIVE_ISSUE", "PharmacyIssueForRequestSave")) {
            return;
        }
        if (issueItems == null || issueItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items to save. Please check the request.");
            return;
        }
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No request bill selected.");
            return;
        }
        BilledBill draft = new BilledBill();
        draft.setBillType(BillType.PharmacyTransferIssue);
        draft.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE_PRE);
        draft.setBackwardReferenceBill(requestedBill);
        draft.setReferenceBill(requestedBill);
        draft.setFromInstitution(sessionController.getInstitution());
        draft.setFromDepartment(sessionController.getDepartment());
        draft.setToInstitution(requestedBill.getFromInstitution() != null
                ? requestedBill.getFromInstitution() : requestedBill.getInstitution());
        draft.setToDepartment(requestedBill.getFromDepartment() != null
                ? requestedBill.getFromDepartment() : requestedBill.getDepartment());
        draft.setInstitution(sessionController.getInstitution());
        draft.setDepartment(sessionController.getDepartment());
        draft.setCreater(sessionController.getLoggedUser());
        draft.setCreatedAt(new Date());
        draft.setCompleted(false);
        draft.setChecked(false);
        if (issuedBill != null) {
            draft.setToStaff(issuedBill.getToStaff());
        }
        issuedBill = draft;
        stampDepartmentTypeIfMissing();
        billFacade.create(draft);
        draftMode = true;
        JsfUtil.addSuccessMessage("Draft fast issue saved. Please proceed to Finalize.");
    }

    public String loadDraftNativeIssueForEditing(Bill draft) {
        makeNull();
        if (draft == null || draft.getId() == null) {
            JsfUtil.addErrorMessage("Invalid draft bill.");
            return null;
        }
        issuedBill = billFacade.find(draft.getId());
        if (issuedBill == null || issuedBill.isRetired()) {
            JsfUtil.addErrorMessage("Draft bill not found or already retired.");
            return null;
        }
        requestedBill = issuedBill.getReferenceBill();
        if (requestedBill == null) {
            JsfUtil.addErrorMessage("Request bill reference missing from draft.");
            return null;
        }
        boolean byPurchaseRate = configOptionApplicationController.getBooleanValueByKey(
                "Pharmacy Transfer is by Purchase Rate", false);
        boolean byCostRate = configOptionApplicationController.getBooleanValueByKey(
                "Pharmacy Transfer is by Cost Rate", false);
        issueItems = transferIssueNativeSqlService.loadRequestedItemsForIssue(
                requestedBill.getId(),
                sessionController.getDepartment().getId(),
                byPurchaseRate,
                byCostRate);
        draftMode = true;
        printPreview = false;
        return "/pharmacy/pharmacy_transfer_issue_native?faces-redirect=true";
    }

    public void finalizeDraftNativeIssue() {
        if (!isAuthorized("FINALIZE_DRAFT_NATIVE_ISSUE", "PharmacyIssueForRequestFinalize")) {
            return;
        }
        if (issuedBill == null || issuedBill.getId() == null) {
            JsfUtil.addErrorMessage("No draft to finalize.");
            return;
        }
        Bill fresh = billFacade.find(issuedBill.getId());
        if (fresh == null || fresh.isRetired()) {
            JsfUtil.addErrorMessage("Draft not found.");
            return;
        }
        if (fresh.isCompleted()) {
            JsfUtil.addErrorMessage("Draft already finalized.");
            return;
        }
        fresh.setCompleted(true);
        fresh.setCompletedAt(new Date());
        fresh.setCompletedBy(sessionController.getLoggedUser());
        billFacade.edit(fresh);
        issuedBill = fresh;
        JsfUtil.addSuccessMessage("Fast issue finalized. It is now pending approval.");
    }

    public synchronized String approveDraftNativeIssue() {
        if (!isAuthorized("APPROVE_DRAFT_NATIVE_ISSUE", "PharmacyIssueForRequestApprove")) {
            return null;
        }
        if (issueItems == null || issueItems.isEmpty()) {
            JsfUtil.addErrorMessage("No items to issue. Reload the draft and check quantities.");
            return null;
        }
        if (issuedBill == null || issuedBill.getId() == null) {
            JsfUtil.addErrorMessage("No draft bill to approve.");
            return null;
        }
        Bill fresh = billFacade.findWithoutCache(issuedBill.getId());
        if (fresh == null || fresh.isRetired()) {
            JsfUtil.addErrorMessage("Draft bill not found or retired.");
            return null;
        }
        if (!fresh.isCompleted()) {
            JsfUtil.addErrorMessage("This fast issue must be finalized before it can be approved.");
            return null;
        }
        if (fresh.isChecked()) {
            JsfUtil.addErrorMessage("This fast issue has already been approved.");
            makeNull();
            return null;
        }
        fresh.setChecked(true);
        fresh.setCheckeAt(new Date());
        fresh.setApproveUser(sessionController.getLoggedUser());
        billFacade.edit(fresh);

        applyBillNumbers(fresh);

        Long staffId = null;
        if (fresh.getToStaff() != null) {
            staffId = fresh.getToStaff().getId();
        }
        try {
            printDto = transferIssueNativeSqlService.settle(
                    fresh,
                    issueItems,
                    sessionController.getDepartment().getId(),
                    sessionController.getInstitution().getId(),
                    staffId);
        } catch (RuntimeException ex) {
            fresh.setChecked(false);
            fresh.setCheckeAt(null);
            fresh.setApproveUser(null);
            billFacade.edit(fresh);
            JsfUtil.addErrorMessage("Approval failed: " + ex.getMessage() + ". The draft has been reset — please try again.");
            return null;
        }
        enrichPrintDto(printDto, fresh);
        printPreview = true;
        draftMode = false;
        return null;
    }

    public String cancelPendingNativeIssue() {
        if (!isAuthorized("CANCEL_PENDING_NATIVE_ISSUE", "PharmacyTransferIssueCancel")) {
            return "";
        }
        if (issuedBill == null || issuedBill.getId() == null) {
            JsfUtil.addErrorMessage("No draft to cancel.");
            return null;
        }
        Bill fresh = billFacade.find(issuedBill.getId());
        if (fresh != null && !fresh.isRetired()) {
            fresh.setRetired(true);
            fresh.setRetiredAt(new Date());
            fresh.setRetirer(sessionController.getLoggedUser());
            billFacade.edit(fresh);
        }
        makeNull();
        JsfUtil.addSuccessMessage("Draft fast issue cancelled.");
        return "/pharmacy/pharmacy_transfer_request_list?faces-redirect=true";
    }

    public String viewByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        makeNull();
        printDto = transferIssueNativeSqlService.loadPrintDtoByBillId(billId);
        if (printDto == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        printDto.setFooterCss(configOptionApplicationController
                .getLongTextValueByKey("Pharmacy Transfer Issue Bill Footer CSS"));
        printDto.setFooterText(configOptionApplicationController
                .getLongTextValueByKey("Pharmacy Transfer Issue Bill Footer Text"));
        printPreview = true;
        return "/pharmacy/pharmacy_transfer_issue_native?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // Core settlement
    // -----------------------------------------------------------------------

    /**
     * Validates quantities, checks stock, builds the bill header, and delegates to
     * the native service to settle.
     * Mirrors TransferIssueForRequestsController.settle().
     */
    public void settle() {
        if (!isAuthorized("SETTLE_NATIVE_ISSUE", "PharmacyIssueForRequestFinalize")) {
            return;
        }
        if (issueItems == null || issueItems.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to issue. Please check quantities.");
            return;
        }

        if (requestedBill == null) {
            JsfUtil.addErrorMessage("No request bill selected.");
            return;
        }

        // Validate that the total issuing qty per request bill item does not exceed
        // the remaining requested qty (requestedQty - alreadyIssuedQty).
        // Negative quantities are explicitly excluded from the sum. If they were included,
        // a user could enter qty=+100 on one batch row and qty=-60 on another row of the
        // same item, keeping the sum at 40 (under the limit) while settlement still issues
        // 100 units from the first row. Skipping negatives prevents that bypass.
        // The service's itemsToProcess filter (issuingQty > 0) is a second safety layer.
        Map<Long, Double> issuingByReqItem = new LinkedHashMap<>();
        Map<Long, String> nameByReqItem    = new LinkedHashMap<>();
        Map<Long, Double> maxByReqItem     = new LinkedHashMap<>();
        for (TransferIssueItemRowDto item : issueItems) {
            long reqId   = item.getRequestedBillItemId();
            double qty   = item.getIssuingQty() != null ? item.getIssuingQty().doubleValue() : 0.0;
            if (qty <= 0) continue; // negative/zero qty excluded — see comment above
            double max   = item.getRequestedQty() - item.getAlreadyIssuedQty();
            issuingByReqItem.merge(reqId, qty, Double::sum);
            nameByReqItem.putIfAbsent(reqId, item.getItemName());
            maxByReqItem.putIfAbsent(reqId, max);
        }
        boolean overIssue = false;
        for (Map.Entry<Long, Double> e : issuingByReqItem.entrySet()) {
            double total = e.getValue();
            double max   = maxByReqItem.getOrDefault(e.getKey(), 0.0);
            if (total > max + 0.001) {
                JsfUtil.addErrorMessage(nameByReqItem.get(e.getKey())
                        + ": issue qty " + String.format("%.2f", total)
                        + " exceeds remaining requested qty " + String.format("%.2f", max));
                overIssue = true;
            }
        }
        if (overIssue) return;

        List<String> stockErrors = transferIssueNativeSqlService.checkStockSufficiency(issueItems);
        if (!stockErrors.isEmpty()) {
            for (String msg : stockErrors) {
                JsfUtil.addErrorMessage("Insufficient stock — " + msg);
            }
            return;
        }

        buildIssueBillHeader();
        applyBillNumbers(issuedBill);

        // Staff is intentionally optional. Departments may issue to a ward or location
        // rather than a named individual. When staffId is null the service still deducts
        // dept stock and records StockHistory but skips the staff-stock credit step.
        // If staff enforcement is required in future, add a guard here:
        //   if (issuedBill.getToStaff() == null) { JsfUtil.addErrorMessage("..."); return; }
        Long staffId = null;
        if (issuedBill.getToStaff() != null) {
            staffId = issuedBill.getToStaff().getId();
        }

        try {
            printDto = transferIssueNativeSqlService.settle(
                    issuedBill,
                    issueItems,
                    sessionController.getDepartment().getId(),
                    sessionController.getInstitution().getId(),
                    staffId);
        } catch (RuntimeException ex) {
            // Reinitialise the bill so a retry gets a fresh entity rather than a
            // detached one with a stale JPA-generated ID from the failed flush.
            com.divudi.core.entity.Staff prevStaff = issuedBill.getToStaff();
            issuedBill = new com.divudi.core.entity.BilledBill();
            issuedBill.setToStaff(prevStaff);
            JsfUtil.addErrorMessage("Settlement failed: " + ex.getMessage());
            return;
        }

        enrichPrintDto(printDto, issuedBill);
        printPreview = true;
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    /**
     * Recalculates item.lineTotal when the user changes issuingQty in the DataTable.
     * Mirrors TransferIssueForRequestsController.onQuantityChangeForTransferIssue().
     */
    public void onQuantityChangeForTransferIssue(TransferIssueItemRowDto item) {
        if (item == null || item.getIssuingQty() == null || item.getGrossRate() == null) {
            return;
        }
        double lineTotal = item.getGrossRate()
                .multiply(item.getIssuingQty())
                .doubleValue();
        item.setLineTotal(lineTotal);
    }

    /**
     * Removes an item from the issue list (user opt-out before settle).
     * Mirrors TransferIssueForRequestsController.remove().
     */
    public void remove(TransferIssueItemRowDto item) {
        if (issueItems != null) {
            issueItems.remove(item);
        }
    }

    /**
     * Returns the sum of line totals for all items currently queued for issue.
     * Displayed as "Issuing Value" in the panel.
     */
    public double getDisplayIssuedNetTotal() {
        if (issueItems == null) {
            return 0.0;
        }
        double tot = 0.0;
        for (TransferIssueItemRowDto item : issueItems) {
            tot += item.getLineTotal();
        }
        return tot;
    }

    /**
     * Resets all session state.
     * Mirrors TransferIssueForRequestsController.makeNull().
     */
    public void makeNull() {
        requestedBill = null;
        requestedBillId = null;
        issuedBill = null;
        issueItems = null;
        printDto = null;
        printPreview = false;
        draftMode = false;
    }

    /**
     * Returns config option list for the page admin configuration interface.
     * Mirrors TransferIssueForRequestsController.getConfigOptionsForDevelopers().
     */
    public List<com.divudi.core.data.admin.ConfigOptionInfo> getConfigOptionsForDevelopers() {
        List<com.divudi.core.data.admin.ConfigOptionInfo> list = new ArrayList<>();
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Transfer is by Purchase Rate", "Use purchase rate as transfer rate (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Transfer is by Cost Rate", "Use cost rate as transfer rate (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Transfer is by Retail Rate", "Use retail rate as transfer rate (default true)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Department Code + Institution Code + Year + Yearly Number",
                "Yearly dept+inst number strategy (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Department Code + Year + Yearly Number",
                "Yearly inst+dept number strategy (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Year + Yearly Number",
                "Institution-wide yearly number strategy (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Transfer Issue A4 Paper", "Enable A4 print format for transfer issue (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Transfer Issue Bill Footer CSS", "CSS for the footer area", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Transfer Issue Bill Footer Text", "Footer text (HTML supported)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Report Font Size of Item List in Pharmacy Disbursement Reports", "Font size for item list (default 10pt)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Serial Number", "Show serial number column (default true)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Date of Expiary", "Show date of expiry column (default true)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Code", "Show item code column (default true)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Purchase Rate", "Show purchase rate column (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Purchase Value", "Show purchase value column (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Retail Sale Rate", "Show retail rate column (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Retail Sale Value", "Show retail value column (default false)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Disbursement Reports - Display Cost Value", "Show cost value column (default false)", OptionScope.APPLICATION));
        return list;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void buildIssueBillHeader() {
        issuedBill.setBillType(BillType.PharmacyTransferIssue);
        issuedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE);
        issuedBill.setBackwardReferenceBill(requestedBill);
        issuedBill.setReferenceBill(requestedBill);
        issuedBill.setFromInstitution(sessionController.getInstitution());
        issuedBill.setFromDepartment(sessionController.getDepartment());
        issuedBill.setToInstitution(requestedBill.getFromInstitution() != null
                ? requestedBill.getFromInstitution() : requestedBill.getInstitution());
        issuedBill.setToDepartment(requestedBill.getFromDepartment() != null
                ? requestedBill.getFromDepartment() : requestedBill.getDepartment());
        issuedBill.setInstitution(sessionController.getInstitution());
        issuedBill.setDepartment(sessionController.getDepartment());
        issuedBill.setCreater(sessionController.getLoggedUser());
        issuedBill.setCreatedAt(new Date());
        stampDepartmentTypeIfMissing();
    }

    /**
     * Department-type-filtered reports drop bills left NULL (#22056). The request bill
     * normally already carries a departmentType (TransferRequestController stamps it on
     * first item add); this is the fallback for legacy requests that predate that stamp —
     * take the first issued item's type, defaulting to Pharmacy (#22146).
     */
    private void stampDepartmentTypeIfMissing() {
        if (issuedBill.getDepartmentType() != null) {
            return;
        }
        if (requestedBill != null && requestedBill.getDepartmentType() != null) {
            issuedBill.setDepartmentType(requestedBill.getDepartmentType());
            return;
        }
        if (issueItems != null && !issueItems.isEmpty()) {
            String dt = issueItems.get(0).getDepartmentType();
            issuedBill.setDepartmentType(dt != null ? DepartmentType.valueOf(dt) : DepartmentType.Pharmacy);
        }
    }

    private void applyBillNumbers(Bill bill) {
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else {
            if (sessionController.getApplicationPreference().isDepNumGenFromToDepartment()) {
                deptId = billNumberBean.departmentBillNumberGenerator(
                        sessionController.getDepartment(), bill.getToDepartment(),
                        BillType.PharmacyTransferIssue, BillClassType.BilledBill, BillNumberSuffix.PHTI);
            } else {
                deptId = billNumberBean.institutionBillNumberGenerator(
                        sessionController.getDepartment(), BillType.PharmacyTransferIssue,
                        BillClassType.BilledBill, BillNumberSuffix.PHTI);
            }
        }

        String insId;
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            insId = deptId;
        } else {
            insId = billNumberBean.institutionBillNumberGenerator(
                    sessionController.getInstitution(), BillType.PharmacyTransferIssue,
                    BillClassType.BilledBill, BillNumberSuffix.PHTI);
        }

        bill.setDeptId(deptId);
        bill.setInsId(insId);
    }

    /**
     * Populates print DTO with session/config data after settlement.
     */
    private void enrichPrintDto(TransferIssuePrintDto dto, Bill bill) {
        if (dto == null) {
            return;
        }

        // From department (issuing dept = logged-in dept)
        Department fromDept = sessionController.getDepartment();
        if (fromDept != null) {
            dto.setFromDepartmentName(safeStr(fromDept.getName()));
            dto.setDepartmentPrintingName(fromDept.getPrintingName() != null
                    ? fromDept.getPrintingName() : safeStr(fromDept.getName()));
            dto.setDepartmentAddress(safeStr(fromDept.getAddress()));
            dto.setDepartmentPhone1(safeStr(fromDept.getTelephone1()));
            dto.setDepartmentPhone2(safeStr(fromDept.getTelephone2()));
            dto.setDepartmentFax(safeStr(fromDept.getFax()));
            dto.setDepartmentEmail(safeStr(fromDept.getEmail()));
        }

        // Institution
        Institution inst = sessionController.getInstitution();
        if (inst != null) {
            dto.setInstitutionName(safeStr(inst.getName()));
            dto.setInstitutionAddress(safeStr(inst.getAddress()));
            dto.setInstitutionFax(safeStr(inst.getFax()));
            dto.setInstitutionEmail(safeStr(inst.getEmail()));
            dto.setInstitutionPhone(safeStr(inst.getPhone()));
        }

        // Issued-by details
        WebUser loggedUser = sessionController.getLoggedUser();
        if (loggedUser != null) {
            dto.setIssuedByName(loggedUser.getWebUserPerson() != null
                    ? safeStr(loggedUser.getWebUserPerson().getName()) : "");
            dto.setIssuedByStaffCode(loggedUser.getStaff() != null
                    ? safeStr(loggedUser.getStaff().getCode()) : "");
        }

        // To-department (receiving dept = request bill's from-department)
        if (requestedBill != null) {
            Department toDept = requestedBill.getFromDepartment() != null
                    ? requestedBill.getFromDepartment() : requestedBill.getDepartment();
            if (toDept != null) {
                dto.setToDepartmentName(safeStr(toDept.getName()));
            }
            dto.setRequestedAt(requestedBill.getCreatedAt());
            dto.setRequestNo(safeStr(requestedBill.getDeptId()));
        }

        // To-staff details
        if (bill.getToStaff() != null && bill.getToStaff().getPerson() != null) {
            dto.setToStaffName(safeStr(bill.getToStaff().getPerson().getNameWithTitle()));
            dto.setToStaffCode(bill.getToStaff().getCode() != null ? bill.getToStaff().getCode() : "");
        }

        // Issue bill details
        dto.setIssueNo(safeStr(bill.getDeptId()));
        dto.setIssuedAt(bill.getCreatedAt());

        // Footer / template config
        dto.setFooterCss(configOptionApplicationController
                .getLongTextValueByKey("Pharmacy Transfer Issue Bill Footer CSS"));
        dto.setFooterText(configOptionApplicationController
                .getLongTextValueByKey("Pharmacy Transfer Issue Bill Footer Text"));
    }

    /** Safe getter for the to-department name: falls back to bill.department if fromDepartment is null. */
    public String getRequestedBillToDepartmentName() {
        if (requestedBill == null) return "";
        com.divudi.core.entity.Department dept = requestedBill.getFromDepartment() != null
                ? requestedBill.getFromDepartment()
                : requestedBill.getDepartment();
        return dept != null ? safeStr(dept.getName()) : "";
    }

    private static String safeStr(String s) {
        return s != null ? s : "";
    }

    // -----------------------------------------------------------------------
    // Getters / Setters
    // -----------------------------------------------------------------------

    public Bill getRequestedBill() {
        if (requestedBill == null) {
            requestedBill = new BilledBill();
        }
        return requestedBill;
    }

    public void setRequestedBill(Bill requestedBill) {
        this.requestedBill = requestedBill;
    }

    public Long getRequestedBillId() {
        return requestedBillId;
    }

    public void setRequestedBillId(Long requestedBillId) {
        this.requestedBillId = requestedBillId;
    }

    public Bill getIssuedBill() {
        if (issuedBill == null) {
            issuedBill = new BilledBill();
        }
        return issuedBill;
    }

    public void setIssuedBill(Bill issuedBill) {
        this.issuedBill = issuedBill;
    }

    public List<TransferIssueItemRowDto> getIssueItems() {
        if (issueItems == null) {
            issueItems = new ArrayList<>();
        }
        return issueItems;
    }

    public void setIssueItems(List<TransferIssueItemRowDto> issueItems) {
        this.issueItems = issueItems;
    }

    public TransferIssuePrintDto getPrintDto() {
        return printDto;
    }

    public void setPrintDto(TransferIssuePrintDto printDto) {
        this.printDto = printDto;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public boolean isDraftMode() {
        return draftMode;
    }

    public void setDraftMode(boolean draftMode) {
        this.draftMode = draftMode;
    }

    /**
     * Authorization helper method to check Pharmacy Transfer Issue (native
     * SQL / fast issue) privileges and audit denied access
     *
     * @param action The action being attempted (e.g. SAVE_DRAFT_NATIVE_ISSUE, FINALIZE_DRAFT_NATIVE_ISSUE)
     * @param requiredPrivilege The specific privilege required
     * @return true if authorized, false if not
     */
    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null, billId={1}",
                    new Object[]{action, issuedBill != null ? issuedBill.getId() : "null"});
            return false;
        }

        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            // Audit denied access attempt
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billId = issuedBill != null ? issuedBill.getId() : null;

            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized Pharmacy Transfer Issue (native) access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});

            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " fast transfer issues.");
            return false;
        }

        return true;
    }
}
