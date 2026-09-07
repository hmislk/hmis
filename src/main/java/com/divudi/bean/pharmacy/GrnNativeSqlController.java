/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillSearch;
import com.divudi.core.data.dto.pharmacy.GrnPrintHeaderDto;
import com.divudi.core.data.dto.pharmacy.GrnPrintItemDto;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.pharmacy.PharmacyGrnNativeSqlService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for the native SQL GRN view page (pharmacy_grn_native.xhtml).
 *
 * Provides:
 *   viewByBillId(Long)       — load data and navigate to the native view page
 *   navigateBack()           — return to the page the user came from
 *   navigateToManageBill()   — load the full entity and navigate to the full
 *                              GRN reprint/costing page via BillSearch manage switch
 *
 * returnPage defaults to pharmacy_search so existing callers through BillSearch
 * get a sensible back destination without needing changes.
 */
@Named
@SessionScoped
public class GrnNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String NATIVE_VIEW_PAGE =
            "/pharmacy/pharmacy_grn_native?faces-redirect=true";

    private static final String DEFAULT_RETURN_PAGE =
            "/pharmacy/pharmacy_search?faces-redirect=true";

    @EJB
    private PharmacyGrnNativeSqlService nativeSqlService;

    @Inject
    private BillSearch billSearch;

    // -----------------------------------------------------------------------
    // State (cleared on each viewByBillId call)
    // -----------------------------------------------------------------------

    private GrnPrintHeaderDto header;
    private List<GrnPrintItemDto> items = new ArrayList<>();
    private Long currentBillId;

    /**
     * The page to navigate to when the Back button is pressed.
     * Callers that know their origin page should set this before calling
     * viewByBillId(). The default is pharmacy_search, which is the most
     * common entry point for GRN lookups.
     */
    private String returnPage;

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    /**
     * Load GRN data from native SQL and navigate to the native view page.
     * Called from BillSearch.navigateToViewBillByAtomicBillTypeByBillId()
     * and from the pharmacy search composite.
     */
    public String viewByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No GRN bill selected");
            return null;
        }
        makeNull();
        currentBillId = billId;

        header = nativeSqlService.loadHeaderByBillId(billId);
        if (header == null) {
            JsfUtil.addErrorMessage("GRN bill not found (ID: " + billId + ")");
            return null;
        }

        items = nativeSqlService.loadItemsByBillId(billId);
        if (items == null) {
            items = new ArrayList<>();
        }

        // Apply the default return page if caller did not override it.
        if (returnPage == null) {
            returnPage = DEFAULT_RETURN_PAGE;
        }

        return NATIVE_VIEW_PAGE;
    }

    /**
     * Navigate back to the page the user came from.
     * The returnPage is set by the caller before viewByBillId(), or defaults
     * to pharmacy_search.
     */
    public String navigateBack() {
        String dest = returnPage != null ? returnPage : DEFAULT_RETURN_PAGE;
        returnPage = null;
        return dest;
    }

    /**
     * Load the full GRN entity (all bill items, pharmaceutical bill items,
     * finance details) and navigate to the GRN reprint/costing page.
     * Routing is done through BillSearch.navigateToManageBillByAtomicBillTypeByBillId
     * so that the manage-bill switch case is the single authoritative routing point.
     */
    public String navigateToManageBill() {
        if (currentBillId == null) {
            JsfUtil.addErrorMessage("No GRN bill selected");
            return null;
        }
        return billSearch.navigateToManageBillByAtomicBillTypeByBillId(currentBillId);
    }

    // -----------------------------------------------------------------------
    // State management
    // -----------------------------------------------------------------------

    public void makeNull() {
        header = null;
        items = new ArrayList<>();
        currentBillId = null;
        // intentionally do NOT reset returnPage here — the caller sets it
        // before viewByBillId(), and we want to honour that.
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public GrnPrintHeaderDto getHeader() { return header; }
    public void setHeader(GrnPrintHeaderDto header) { this.header = header; }

    public List<GrnPrintItemDto> getItems() { return items; }
    public void setItems(List<GrnPrintItemDto> items) { this.items = items; }

    public Long getCurrentBillId() { return currentBillId; }
    public void setCurrentBillId(Long currentBillId) { this.currentBillId = currentBillId; }

    public String getReturnPage() { return returnPage; }
    public void setReturnPage(String returnPage) { this.returnPage = returnPage; }
}
