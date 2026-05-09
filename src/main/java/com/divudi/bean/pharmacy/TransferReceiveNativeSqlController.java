/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.TransferReceiveItemRowDto;
import com.divudi.core.data.dto.TransferReceivePrintDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.BillService;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.service.pharmacy.TransferReceiveNativeSqlService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.TemporalType;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * SessionScoped controller for the native-SQL transfer receive workflow.
 *
 * Mirrors TransferReceiveController in its methods and navigation targets, but:
 *  - Uses List&lt;TransferReceiveItemRowDto&gt; instead of entity BillItem lists
 *  - Uses TransferReceivePrintDto for print display instead of entity Bill
 *  - Uses TransferReceiveNativeSqlService for data loading and settlement
 *
 * Bean name: transferReceiveNativeSqlController (matches @Named annotation and XHTML EL)
 */
@Named
@SessionScoped
public class TransferReceiveNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(TransferReceiveNativeSqlController.class.getName());

    // ---- State ----
    private Bill issuedBill;
    private Long receivedBillId;
    private List<TransferReceiveItemRowDto> itemRowList;
    private TransferReceivePrintDto printDto;
    private boolean printPreview;
    private String comments;
    private Date fromDate;
    private Date toDate;
    private TransferReceiveItemRowDto selectedItemRow;

    // ---- Injected ----
    @Inject
    private SessionController sessionController;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @Inject
    private PageMetadataRegistry pageMetadataRegistry;

    @EJB
    private BillFacade billFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private BillNumberGenerator billNumberBean;

    @EJB
    private TransferReceiveNativeSqlService transferReceiveNativeSqlService;

    @EJB
    BillService billService;

    @EJB
    private PharmacyCostingService pharmacyCostingService;

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
     * Loads issued items via native service and navigates to the native receive page.
     * Mirrors TransferReceiveController.navigateToRecieveRequest().
     */
    public String navigateToReceiveRequestNative() {
        if (issuedBill == null || issuedBill.getId() == null) {
            JsfUtil.addErrorMessage("Nothing to receive");
            return null;
        }

        // Check already received via native SQL
        if (isAlreadyReceivedNative(issuedBill.getId())) {
            JsfUtil.addErrorMessage("Already Received!");
            return null;
        }

        printPreview = false;
        printDto = null;
        comments = null;

        boolean byPurchaseRate = configOptionApplicationController.getBooleanValueByKey(
                "Pharmacy Transfer is by Purchase Rate", false);
        boolean byCostRate = configOptionApplicationController.getBooleanValueByKey(
                "Pharmacy Transfer is by Cost Rate", false);

        itemRowList = transferReceiveNativeSqlService.loadIssuedItemsForReceive(
                issuedBill.getId(), byPurchaseRate, byCostRate);

        if (itemRowList == null || itemRowList.isEmpty()) {
            JsfUtil.addErrorMessage("No items remaining to receive.");
            return null;
        }

        resolveAmpItemIds(itemRowList);

        return "/pharmacy/pharmacy_transfer_receive_native?faces-redirect=true";
    }

    public String navigateBackToReceiveList() {
        return "/pharmacy/pharmacy_transfer_issued_list?faces-redirect=true";
    }

    public String navigateToReprintReceiveIssue() {
        return "/pharmacy/pharmacy_reprint_transfer_receive?faces-redirect=true";
    }

    public String navigateToEditReceiveIssue() {
        return "/pharmacy/pharmacy_transfer_receive_native_with_approval?faces-redirect=true";
    }

    public String navigateToApproveReceiveIssue() {
        return "/pharmacy/pharmacy_transfer_receive_native_approval?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // Core settlement
    // -----------------------------------------------------------------------

    /**
     * Builds the bill header and calls the native service to settle.
     * Mirrors TransferReceiveController.settle().
     */
    public void settle() {
        if (itemRowList == null || itemRowList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to Receive, Please check Received Quantity");
            return;
        }

        if (isAlreadyReceivedNative(issuedBill.getId())) {
            JsfUtil.addErrorMessage("Cannot receive — already fully received!");
            return;
        }

        if (wouldCauseOverReceiving()) {
            JsfUtil.addErrorMessage("Cannot receive — quantities exceed remaining amounts!");
            return;
        }

        List<String> stockErrors = transferReceiveNativeSqlService.checkSourceStockSufficiency(itemRowList);
        if (!stockErrors.isEmpty()) {
            for (String msg : stockErrors) {
                JsfUtil.addErrorMessage("Insufficient source stock — " + msg);
            }
            return;
        }

        Bill bill = buildReceiveBillHeader(BillTypeAtomic.PHARMACY_RECEIVE);
        applyBillNumbers(bill);

        try {
            printDto = transferReceiveNativeSqlService.settle(
                    bill,
                    itemRowList,
                    sessionController.getDepartment().getId(),
                    sessionController.getInstitution().getId());
        } catch (RuntimeException ex) {
            JsfUtil.addErrorMessage("Settlement failed: " + ex.getMessage());
            return;
        }

        receivedBillId = bill.getId();
        enrichPrintDto(printDto, bill);
        printPreview = true;
    }

    /**
     * Approval variant: settles a pre-saved receive request.
     * Mirrors TransferReceiveController.settleApprove().
     */
    public void settleApprove() {
        if (receivedBillId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return;
        }

        if (isAlreadyReceivedNative(issuedBill.getId())) {
            JsfUtil.addErrorMessage("Cannot receive — already fully received!");
            return;
        }

        if (wouldCauseOverReceiving()) {
            JsfUtil.addErrorMessage("Cannot receive — quantities exceed remaining amounts!");
            return;
        }

        List<String> stockErrors = transferReceiveNativeSqlService.checkSourceStockSufficiency(itemRowList);
        if (!stockErrors.isEmpty()) {
            for (String msg : stockErrors) {
                JsfUtil.addErrorMessage("Insufficient source stock — " + msg);
            }
            return;
        }

        long preBillId = receivedBillId; // capture before overwrite
        Bill bill = buildReceiveBillHeader(BillTypeAtomic.PHARMACY_RECEIVE);
        bill.setApproveAt(new Date());
        bill.setApproveUser(sessionController.getLoggedUser());
        applyBillNumbers(bill);

        try {
            printDto = transferReceiveNativeSqlService.settle(
                    bill,
                    itemRowList,
                    sessionController.getDepartment().getId(),
                    sessionController.getInstitution().getId());
        } catch (RuntimeException ex) {
            JsfUtil.addErrorMessage("Settlement failed: " + ex.getMessage());
            return;
        }

        // Retire the pre-saved PHARMACY_RECEIVE_PRE bill — it has been superseded
        // by the new PHARMACY_RECEIVE bill created above. Without this, the PRE bill
        // remains as an orphan and would appear in list queries.
        try {
            transferReceiveNativeSqlService.retirePreBill(preBillId, sessionController.getLoggedUser().getId());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not retire pre-bill id={0}: {1}",
                    new Object[]{preBillId, ex.getMessage()});
        }

        receivedBillId = bill.getId();
        enrichPrintDto(printDto, bill);
        printPreview = true;
    }

    /**
     * Saves a pre-approval receive request (BillTypeAtomic.PHARMACY_RECEIVE_PRE).
     * Does not do stock operations — just persists the header via JPA.
     * Mirrors TransferReceiveController.saveRequest().
     */
    public void saveRequest() {
        if (issuedBill == null || issuedBill.getId() == null) {
            JsfUtil.addErrorMessage("No issued bill selected");
            return;
        }

        Bill bill = buildReceiveBillHeader(BillTypeAtomic.PHARMACY_RECEIVE_PRE);
        String billNo = billNumberBean.institutionBillNumberGenerator(
                sessionController.getDepartment(), BillType.PharmacyTransferReceive,
                BillClassType.BilledBill, BillNumberSuffix.PHTI);
        bill.setDeptId(billNo);
        bill.setInsId(billNo);
        bill.setBillItems(null);

        billFacade.create(bill);
        receivedBillId = bill.getId();

        // Link to issued bill's forwardReferenceBills via JPA
        issuedBill.getForwardReferenceBills().add(bill);
        billFacade.edit(issuedBill);

        JsfUtil.addSuccessMessage("Request Saved Successfully");
    }

    /**
     * Finalizes a pre-saved receive request (marks as checked).
     * Mirrors TransferReceiveController.finalizeRequest().
     */
    public void finalizeRequest() {
        if (receivedBillId == null) {
            JsfUtil.addErrorMessage("No saved request found. Please save first.");
            return;
        }

        Bill savedBill = billFacade.find(receivedBillId);
        if (savedBill == null) {
            JsfUtil.addErrorMessage("Saved bill not found.");
            return;
        }

        String receiveId = billNumberBean.departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(
                issuedBill.getDepartment(), sessionController.getDepartment(),
                BillTypeAtomic.PHARMACY_RECEIVE);
        savedBill.setDeptId(receiveId);
        savedBill.setInsId(receiveId);

        savedBill.setEditor(sessionController.getLoggedUser());
        savedBill.setEditedAt(new Date());
        savedBill.setCheckeAt(new Date());
        savedBill.setCheckedBy(sessionController.getLoggedUser());
        billFacade.edit(savedBill);

        JsfUtil.addSuccessMessage("Request Finalized Successfully");
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    /**
     * Recalculates item.value when the user changes receivingQty in the DataTable.
     * Updates the running display total.
     */
    public void onQuantityChangeForTransferReceive(TransferReceiveItemRowDto item) {
        if (item == null || item.getReceivingQty() == null || item.getGrossRate() == null) {
            return;
        }
        double value = item.getGrossRate()
                .multiply(item.getReceivingQty())
                .abs()
                .doubleValue();
        item.setValue(value);
    }

    /**
     * Returns the sum of abs values for all items in the current row list.
     * Displayed as "Receiving Value" in the panel.
     */
    public double getDisplayReceivedNetTotal() {
        if (itemRowList == null) {
            return 0.0;
        }
        double tot = 0.0;
        for (TransferReceiveItemRowDto item : itemRowList) {
            tot += Math.abs(item.getValue());
        }
        return tot;
    }

    /**
     * Fills template header placeholders from a Bill entity.
     * Mirrors TransferReceiveController.fillHeaderDataOfTransferReceiveNote(String, Bill).
     * Used by transfer_receive_custom_1 components that still call this helper.
     */
    public String fillHeaderDataOfTransferReceiveNote(String s, Bill b) {
        if (s == null) {
            return "";
        }
        if (b == null) {
            return s;
        }

        String institutionName = safeStr(b.getCreater() != null && b.getCreater().getDepartment() != null
                ? b.getCreater().getDepartment().getPrintingName() : null);
        String institutionAddress = safeStr(b.getCreater() != null && b.getCreater().getDepartment() != null
                ? b.getCreater().getDepartment().getAddress() : null);
        String phone1 = safeStr(b.getCreater() != null && b.getCreater().getDepartment() != null
                ? b.getCreater().getDepartment().getTelephone1() : null);
        String phone2 = safeStr(b.getCreater() != null && b.getCreater().getDepartment() != null
                ? b.getCreater().getDepartment().getTelephone2() : null);
        String institutionPhones = phone1;
        if (!phone2.isEmpty()) {
            institutionPhones += " / " + phone2;
        }
        String institutionFax = "";
        if (b.getCreater() != null && b.getCreater().getDepartment() != null
                && b.getCreater().getDepartment().getFax() != null
                && !b.getCreater().getDepartment().getFax().trim().isEmpty()) {
            institutionFax = "Fax: " + b.getCreater().getDepartment().getFax();
        }
        String institutionEmail = "";
        if (b.getCreater() != null && b.getCreater().getDepartment() != null
                && b.getCreater().getDepartment().getEmail() != null
                && !b.getCreater().getDepartment().getEmail().trim().isEmpty()) {
            institutionEmail = "Email: " + b.getCreater().getDepartment().getEmail();
        }
        String cancelledStatus = "";
        if (b.getBilledBill() != null && Boolean.TRUE.equals(b.getBilledBill().isCancelled())) {
            cancelledStatus = " <span class=\"receipt-cancelled\">**Cancelled**</span>";
        }
        String locationFrom = b.getFromDepartment() != null ? safeStr(b.getFromDepartment().getName()) : "";
        String locationTo = b.getDepartment() != null ? safeStr(b.getDepartment().getName()) : "";
        String receivedPerson = (b.getCreater() != null && b.getCreater().getWebUserPerson() != null)
                ? safeStr(b.getCreater().getWebUserPerson().getName()) : "";
        String issuedPerson = (b.getBackwardReferenceBill() != null
                && b.getBackwardReferenceBill().getCreater() != null
                && b.getBackwardReferenceBill().getCreater().getWebUserPerson() != null)
                ? safeStr(b.getBackwardReferenceBill().getCreater().getWebUserPerson().getName()) : "";
        String receiveNo = safeStr(b.getDeptId());
        String issueNo = (b.getBackwardReferenceBill() != null)
                ? safeStr(b.getBackwardReferenceBill().getDeptId()) : "";
        String receivedTime = "";
        if (b.getCreatedAt() != null && sessionController.getApplicationPreference() != null
                && sessionController.getApplicationPreference().getLongDateTimeFormat() != null) {
            String fmt = CommonFunctions.getDateFormat(b.getCreatedAt(),
                    sessionController.getApplicationPreference().getLongDateTimeFormat());
            receivedTime = fmt != null ? fmt : "";
        }
        String issueTime = "";
        if (b.getBackwardReferenceBill() != null && b.getBackwardReferenceBill().getCreatedAt() != null
                && sessionController.getApplicationPreference() != null
                && sessionController.getApplicationPreference().getLongDateTimeFormat() != null) {
            String fmt = CommonFunctions.getDateFormat(b.getBackwardReferenceBill().getCreatedAt(),
                    sessionController.getApplicationPreference().getLongDateTimeFormat());
            issueTime = fmt != null ? fmt : "";
        }

        return s.replace("{{institution_name}}", institutionName)
                .replace("{{institution_address}}", institutionAddress)
                .replace("{{institution_phones}}", institutionPhones)
                .replace("{{institution_fax}}", institutionFax)
                .replace("{{institution_email}}", institutionEmail)
                .replace("{{cancelled_status}}", cancelledStatus)
                .replace("{{location_from}}", locationFrom)
                .replace("{{location_to}}", locationTo)
                .replace("{{received_person}}", receivedPerson)
                .replace("{{issued_person}}", issuedPerson)
                .replace("{{receive_no}}", receiveNo)
                .replace("{{issue_no}}", issueNo)
                .replace("{{received_time}}", receivedTime)
                .replace("{{issue_time}}", issueTime);
    }

    /**
     * Overload that fills template header from the current printDto (no Bill entity needed).
     */
    public String fillHeaderDataFromPrintDto(String template) {
        if (template == null || template.isEmpty() || printDto == null) {
            return template != null ? template : "";
        }

        String institutionPhones = safeStr(printDto.getDepartmentPhone1());
        String phone2 = safeStr(printDto.getDepartmentPhone2());
        if (!phone2.isEmpty()) {
            institutionPhones += " / " + phone2;
        }

        String institutionFax = printDto.getDepartmentFax() != null && !printDto.getDepartmentFax().trim().isEmpty()
                ? "Fax: " + printDto.getDepartmentFax() : "";
        String institutionEmail = printDto.getDepartmentEmail() != null && !printDto.getDepartmentEmail().trim().isEmpty()
                ? "Email: " + printDto.getDepartmentEmail() : "";
        String cancelledStatus = printDto.isCancelled()
                ? " <span class=\"receipt-cancelled\">**Cancelled**</span>" : "";

        String receivedTime = "";
        if (printDto.getReceivedAt() != null && sessionController.getApplicationPreference() != null
                && sessionController.getApplicationPreference().getLongDateTimeFormat() != null) {
            String fmt = CommonFunctions.getDateFormat(printDto.getReceivedAt(),
                    sessionController.getApplicationPreference().getLongDateTimeFormat());
            receivedTime = fmt != null ? fmt : "";
        }
        String issueTime = "";
        if (printDto.getIssuedAt() != null && sessionController.getApplicationPreference() != null
                && sessionController.getApplicationPreference().getLongDateTimeFormat() != null) {
            String fmt = CommonFunctions.getDateFormat(printDto.getIssuedAt(),
                    sessionController.getApplicationPreference().getLongDateTimeFormat());
            issueTime = fmt != null ? fmt : "";
        }

        return template
                .replace("{{institution_name}}", safeStr(printDto.getDepartmentPrintingName()))
                .replace("{{institution_address}}", safeStr(printDto.getDepartmentAddress()))
                .replace("{{institution_phones}}", institutionPhones)
                .replace("{{institution_fax}}", institutionFax)
                .replace("{{institution_email}}", institutionEmail)
                .replace("{{cancelled_status}}", cancelledStatus)
                .replace("{{location_from}}", safeStr(printDto.getFromDepartmentName()))
                .replace("{{location_to}}", safeStr(printDto.getToDepartmentName()))
                .replace("{{received_person}}", safeStr(printDto.getReceivedByName()))
                .replace("{{issued_person}}", safeStr(printDto.getIssuedByName()))
                .replace("{{receive_no}}", safeStr(printDto.getReceiveNo()))
                .replace("{{issue_no}}", safeStr(printDto.getIssueNo()))
                .replace("{{received_time}}", receivedTime)
                .replace("{{issue_time}}", issueTime);
    }

    /**
     * Returns config option list for the page admin configuration interface.
     * Mirrors TransferReceiveController.getConfigOptionsForDevelopers().
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
                "Pharmacy Transfer Receive Bill Footer CSS", "CSS for the footer area", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Pharmacy Transfer Receive Bill Footer Text", "Footer text (HTML supported)", OptionScope.APPLICATION));
        list.add(new com.divudi.core.data.admin.ConfigOptionInfo(
                "Transfer Receive Note Header", "Template header with {{placeholders}}", OptionScope.APPLICATION));
        return list;
    }

    /**
     * Resets all session state.
     * Mirrors TransferReceiveController.makeNull().
     */
    public void makeNull() {
        issuedBill = null;
        receivedBillId = null;
        itemRowList = null;
        printDto = null;
        printPreview = false;
        comments = null;
        fromDate = null;
        toDate = null;
        selectedItemRow = null;
    }

    // -----------------------------------------------------------------------
    // Validation helpers
    // -----------------------------------------------------------------------

    /**
     * Native SQL check: returns true if all issued items have been fully received
     * (no remaining qty greater than tolerance).
     * Delegates to the stateless service which holds a PersistenceContext.
     */
    public boolean isAlreadyReceivedNative(long issuedBillId) {
        return transferReceiveNativeSqlService.isAlreadyReceived(issuedBillId);
    }

    /**
     * Validates that no item's receivingQty exceeds the remaining available qty.
     */
    public boolean wouldCauseOverReceiving() {
        if (itemRowList == null || issuedBill == null) {
            return false;
        }
        for (TransferReceiveItemRowDto item : itemRowList) {
            if (item.getReceivingQty() == null) {
                continue;
            }
            double receivingUnits = item.getReceivingQty().doubleValue() * item.getUnitsPerPack();
            // remainingUnits = issuedUnits minus all prior receives (computed at page-load time)
            if (receivingUnits > item.getRemainingUnits() + 0.001) {
                return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private Bill buildReceiveBillHeader(BillTypeAtomic atomicType) {
        BilledBill bill = new BilledBill();
        bill.setBillType(BillType.PharmacyTransferReceive);
        bill.setBillTypeAtomic(atomicType);
        bill.setBackwardReferenceBill(issuedBill);
        bill.setFromStaff(issuedBill.getToStaff());
        bill.setFromInstitution(issuedBill.getFromInstitution() != null
                ? issuedBill.getFromInstitution() : issuedBill.getInstitution());
        bill.setFromDepartment(issuedBill.getFromDepartment() != null
                ? issuedBill.getFromDepartment() : issuedBill.getDepartment());
        bill.setToInstitution(sessionController.getInstitution());
        bill.setToDepartment(sessionController.getDepartment());
        bill.setInstitution(sessionController.getInstitution());
        bill.setDepartment(sessionController.getDepartment());
        bill.setCreater(sessionController.getLoggedUser());
        bill.setCreatedAt(new Date());
        bill.setComments(comments);
        return bill;
    }

    private void applyBillNumbers(Bill bill) {
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RECEIVE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RECEIVE);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RECEIVE);
        } else {
            deptId = billNumberBean.institutionBillNumberGenerator(
                    sessionController.getDepartment(), BillType.PharmacyTransferReceive,
                    BillClassType.BilledBill, BillNumberSuffix.PHTI);
        }

        String insId;
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Transfer Receive - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RECEIVE);
        } else {
            insId = deptId;
        }

        bill.setDeptId(deptId);
        bill.setInsId(insId);
    }

    /**
     * Populates the print DTO with department, institution, and issue bill information
     * from the current session and the issuedBill after settlement.
     */
    private void enrichPrintDto(TransferReceivePrintDto dto, Bill bill) {
        if (dto == null) {
            return;
        }

        // Issued bill details
        if (issuedBill != null) {
            dto.setIssueNo(issuedBill.getDeptId());
            dto.setIssuedAt(issuedBill.getCreatedAt());
            dto.setFromDepartmentName(issuedBill.getFromDepartment() != null
                    ? safeStr(issuedBill.getFromDepartment().getName())
                    : (issuedBill.getDepartment() != null ? safeStr(issuedBill.getDepartment().getName()) : ""));
            if (issuedBill.getCreater() != null) {
                dto.setIssuedByName(issuedBill.getCreater().getWebUserPerson() != null
                        ? safeStr(issuedBill.getCreater().getWebUserPerson().getName()) : "");
                dto.setIssuedByStaffCode(issuedBill.getCreater().getStaff() != null
                        ? safeStr(issuedBill.getCreater().getStaff().getCode()) : "");
            }
            if (issuedBill.getToStaff() != null && issuedBill.getToStaff().getPerson() != null) {
                dto.setTransporterName(safeStr(issuedBill.getToStaff().getPerson().getNameWithTitle()));
            }
            dto.setIssuedNetTotal(Math.abs(issuedBill.getNetTotal()));
        }

        // Receiving department details
        Department dept = sessionController.getDepartment();
        if (dept != null) {
            dto.setToDepartmentName(safeStr(dept.getName()));
            dto.setDepartmentPrintingName(dept.getPrintingName() != null
                    ? dept.getPrintingName() : safeStr(dept.getName()));
            dto.setDepartmentAddress(safeStr(dept.getAddress()));
            dto.setDepartmentPhone1(safeStr(dept.getTelephone1()));
            dto.setDepartmentPhone2(safeStr(dept.getTelephone2()));
            dto.setDepartmentFax(safeStr(dept.getFax()));
            dto.setDepartmentEmail(safeStr(dept.getEmail()));

            // Combined phone for institution phone field
            String phones = safeStr(dept.getTelephone1());
            String p2 = safeStr(dept.getTelephone2());
            if (!p2.isEmpty()) {
                phones += " / " + p2;
            }
            dto.setInstitutionPhone(phones);
        }

        // Institution details
        Institution inst = sessionController.getInstitution();
        if (inst != null) {
            dto.setInstitutionName(safeStr(inst.getName()));
            dto.setInstitutionAddress(safeStr(inst.getAddress()));
            dto.setInstitutionFax(safeStr(inst.getFax()));
            dto.setInstitutionEmail(safeStr(inst.getEmail()));
        }

        // Received by details
        WebUser loggedUser = sessionController.getLoggedUser();
        if (loggedUser != null) {
            dto.setReceivedByName(loggedUser.getWebUserPerson() != null
                    ? safeStr(loggedUser.getWebUserPerson().getName()) : "");
            dto.setReceivedByStaffCode(loggedUser.getStaff() != null
                    ? safeStr(loggedUser.getStaff().getCode()) : "");
        }

        // Approved by details
        if (bill.getApproveUser() != null) {
            dto.setApprovedByName(bill.getApproveUser().getWebUserPerson() != null
                    ? safeStr(bill.getApproveUser().getWebUserPerson().getName()) : "");
            dto.setApprovedAt(bill.getApproveAt());
        }

        // Footer and template from configuration
        dto.setFooterCss(configOptionApplicationController
                .getLongTextValueByKey("Pharmacy Transfer Receive Bill Footer CSS"));
        dto.setFooterText(configOptionApplicationController
                .getLongTextValueByKey("Pharmacy Transfer Receive Bill Footer Text"));

        String headerTemplate = configOptionApplicationController
                .getLongTextValueByKey("Transfer Receive Note Header");
        dto.setTemplateHeader(fillHeaderDataFromPrintDto(headerTemplate));
    }

    private void resolveAmpItemIds(List<TransferReceiveItemRowDto> items) {
        if (items == null) {
            return;
        }
        for (TransferReceiveItemRowDto item : items) {
            if (item.getItemId() == null) {
                continue;
            }
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("id", item.getItemId());
                List<?> result = itemFacade.findLightsByJpql(
                        "SELECT i.amp.id FROM Item i WHERE i.id = :id AND TYPE(i) = Ampp",
                        params, TemporalType.DATE, 1);
                if (result != null && !result.isEmpty() && result.get(0) != null) {
                    item.setAmpItemId(((Number) result.get(0)).longValue());
                }
                // non-Ampp items: ampItemId already equals itemId — no change needed
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not resolve AMP for itemId={0}: {1}",
                        new Object[]{item.getItemId(), e.getMessage()});
            }
        }
    }

    private static String safeStr(String s) {
        return s != null ? s : "";
    }

    // -----------------------------------------------------------------------
    // Getters / Setters
    // -----------------------------------------------------------------------

    public Bill getIssuedBill() {
        if (issuedBill == null) {
            issuedBill = new BilledBill();
        }
        return issuedBill;
    }

    public void setIssuedBill(Bill issuedBill) {
        this.issuedBill = issuedBill;
    }

    public Long getReceivedBillId() {
        return receivedBillId;
    }

    public void setReceivedBillId(Long receivedBillId) {
        this.receivedBillId = receivedBillId;
    }

    public List<TransferReceiveItemRowDto> getItemRowList() {
        if (itemRowList == null) {
            itemRowList = new ArrayList<>();
        }
        return itemRowList;
    }

    public void setItemRowList(List<TransferReceiveItemRowDto> itemRowList) {
        this.itemRowList = itemRowList;
    }

    public TransferReceivePrintDto getPrintDto() {
        return printDto;
    }

    public void setPrintDto(TransferReceivePrintDto printDto) {
        this.printDto = printDto;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public TransferReceiveItemRowDto getSelectedItemRow() {
        return selectedItemRow;
    }

    public void setSelectedItemRow(TransferReceiveItemRowDto selectedItemRow) {
        this.selectedItemRow = selectedItemRow;
    }
}
