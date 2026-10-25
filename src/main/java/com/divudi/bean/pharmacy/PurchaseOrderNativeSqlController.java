/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderPrintDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;
import com.divudi.service.pharmacy.PurchaseOrderNativeSqlService;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * SessionScoped controller for the native-SQL Purchase Order print preview.
 *
 * Entry point: viewByBillId(Long billId) — called by BillSearch routing.
 * Stores the loaded DTO and navigates to pharmacy_reprint_po_native.xhtml.
 * Related issue: #20923
 */
@Named
@SessionScoped
public class PurchaseOrderNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    private PurchaseOrderPrintDto printDto;
    private boolean printPreview;
    private Long currentBillId;

    @Inject
    private SessionController sessionController;

    @EJB
    private PurchaseOrderNativeSqlService purchaseOrderNativeSqlService;

    @EJB
    private BillService billService;

    @Inject
    private PharmacyBillSearch pharmacyBillSearch;

    // -----------------------------------------------------------------------
    // Navigation entry point (called by BillSearch)
    // -----------------------------------------------------------------------

    public String viewByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        makeNull();
        currentBillId = billId;
        printDto = purchaseOrderNativeSqlService.loadPrintDtoByBillId(billId);
        if (printDto == null) {
            JsfUtil.addErrorMessage("Purchase Order not found");
            return null;
        }
        printPreview = true;
        return "/pharmacy/pharmacy_reprint_po_native?faces-redirect=true";
    }

    /**
     * Cancel button on the native print page (issue #23941). This controller
     * only holds a flat, read-only {@link PurchaseOrderPrintDto} - it has no
     * entity {@code Bill} to hand to {@code pharmacyBillSearch.pharmacyPoCancel()}.
     * Loads the entity via {@code billService.reloadBill(...)} (same call
     * {@code BillSearch.navigateToViewBillByAtomicBillTypeByBillIdEntityBased}
     * uses), populates it into {@code pharmacyBillSearch}, then navigates
     * straight to the existing comment-entry/confirm page, which already
     * re-checks the privilege and calls {@code pharmacyPoCancel()}.
     *
     * <p>Uses {@code printDto.getApprovalBillId()}, not {@code currentBillId}:
     * {@code viewByBillId(Long)} accepts either a PHARMACY_ORDER *request* or
     * a PHARMACY_ORDER_APPROVAL id and always displays the approval's data, so
     * {@code currentBillId} can be the request's id. Cancelling must always
     * target the actual approval bill regardless of which id was used to view
     * this page (#23988 review).</p>
     */
    public String navigateToCancelPo() {
        if (printDto == null || printDto.getApprovalBillId() == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill foundBill = billService.reloadBill(printDto.getApprovalBillId());
        if (foundBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        pharmacyBillSearch.setBill(foundBill);
        // Clear any stale reason left over from a previous cancellation in
        // this session - pharmacy_cancel_po.xhtml's comment field is
        // session-scoped and not reset by setBill() (#23988 review).
        pharmacyBillSearch.setComment(null);
        return "/pharmacy/pharmacy_cancel_po?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    public void makeNull() {
        printDto = null;
        printPreview = false;
        currentBillId = null;
    }

    // -----------------------------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------------------------

    public PurchaseOrderPrintDto getPrintDto() { return printDto; }
    public void setPrintDto(PurchaseOrderPrintDto printDto) { this.printDto = printDto; }

    public boolean isPrintPreview() { return printPreview; }
    public void setPrintPreview(boolean printPreview) { this.printPreview = printPreview; }

    public Long getCurrentBillId() { return currentBillId; }
    public void setCurrentBillId(Long currentBillId) { this.currentBillId = currentBillId; }
}
