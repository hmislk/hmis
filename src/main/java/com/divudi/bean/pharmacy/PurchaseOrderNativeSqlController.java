/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderPrintDto;
import com.divudi.core.util.JsfUtil;
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
