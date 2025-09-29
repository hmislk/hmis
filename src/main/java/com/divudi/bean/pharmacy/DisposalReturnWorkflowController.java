/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for Disposal Return Workflow - handles Create, Finalize, and
 * Approve steps
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class DisposalReturnWorkflowController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(DisposalReturnWorkflowController.class.getName());

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillService billService;

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private IssueReturnController issueReturnController;

    // Lists for workflow management
    private List<Bill> disposalReturnsToFinalize;
    private List<Bill> disposalReturnsToApprove;
    private List<Bill> filteredDisposalReturnsToFinalize;
    private List<Bill> filteredDisposalReturnsToApprove;

    // Currently selected bills for processing
    private Bill selectedBillToFinalize;
    private Bill selectedBillToApprove;

    // Accordion panel management
    private String activeIndexForReturnsAndCancellations = "0";

    /**
     * Navigation Methods - Each page shows a list of bills with navigation to
     * unified pharmacy_bill_return_issue page
     */
    public String navigateToCreateDisposalReturn() {
        // Navigate to disposal bill search page to select bills for return
        return "/pharmacy/pharmacy_search_issue_bill_for_return?faces-redirect=true";
    }

    public String navigateToFinalizeDisposalReturn() {
        // Navigate to list of disposal returns that need finalization
        fillDisposalReturnsToFinalize();
        return "/pharmacy/pharmacy_disposal_return_finalize?faces-redirect=true";
    }

    public String navigateToApproveDisposalReturn() {
        // Navigate to list of disposal returns that need approval
        fillDisposalReturnsToApprove();
        return "/pharmacy/pharmacy_disposal_return_approve?faces-redirect=true";
    }

    /**
     * Check if there are disposal returns pending approval for current
     * department
     */
    public boolean hasPendingDisposalReturnForApproval() {
        if (sessionController.getDepartment() == null) {
            return false;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT COUNT(b) FROM Bill b "
                + "WHERE b.retired = :retired "
                + "AND b.billTypeAtomic = :billTypeAtomic "
                + "AND b.department = :department "
                + "AND b.checked = :checked "
                + "AND (b.cancelled = :cancelled OR b.cancelled IS NULL) ";

        params.put("retired", false);
        params.put("billTypeAtomic", BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        params.put("department", sessionController.getDepartment());
        params.put("checked", true);
        params.put("cancelled", false);

        Long count = billFacade.countByJpql(jpql, params);
        return count != null && count > 0;
    }

    /**
     * Check if there are pending disposal returns for a specific disposal issue
     * bill This prevents creating multiple returns for the same disposal bill
     * when one is already pending approval Similar to GRN return workflow logic
     *
     * @param disposalIssueBill The original disposal issue bill to check
     * @return true if there are pending returns for this specific bill
     */
    public boolean hasPendingDisposalReturnForSpecificBill(Bill disposalIssueBill) {
        if (disposalIssueBill == null || sessionController.getDepartment() == null) {
            return false;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT COUNT(b) FROM Bill b "
                + "WHERE b.retired = :retired "
                + "AND b.billTypeAtomic = :billTypeAtomic "
                + "AND b.department = :department "
                + "AND b.referenceBill = :referenceBill "
                + "AND b.checked = :checked "
                + "AND (b.cancelled = :cancelled OR b.cancelled IS NULL) "
                + "AND (b.completed = :completed OR b.completed IS NULL) ";

        params.put("retired", false);
        params.put("billTypeAtomic", BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        params.put("department", sessionController.getDepartment());
        params.put("referenceBill", disposalIssueBill);
        params.put("checked", true); // Finalized but not yet approved
        params.put("cancelled", false);
        params.put("completed", false); // Not yet completed/approved

        Long count = billFacade.countByJpql(jpql, params);
        return count != null && count > 0;
    }

    /**
     * Fill disposal returns that need to be finalized
     */
    public void fillDisposalReturnsToFinalize() {
        if (sessionController.getDepartment() == null) {
            disposalReturnsToFinalize = new ArrayList<>();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT b FROM Bill b "
                + "WHERE b.retired = :retired "
                + "AND b.billTypeAtomic = :billTypeAtomic "
                + "AND b.department = :department "
                + "AND (b.checked = :checked OR b.checked IS NULL) "
                + "AND (b.cancelled = :cancelled OR b.cancelled IS NULL) "
                + "ORDER BY b.createdAt DESC";

        params.put("retired", false);
        params.put("billTypeAtomic", BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        params.put("department", sessionController.getDepartment());
        params.put("checked", false);
        params.put("cancelled", false);

        disposalReturnsToFinalize = billFacade.findByJpql(jpql, params);
        if (disposalReturnsToFinalize == null) {
            disposalReturnsToFinalize = new ArrayList<>();
        }
    }

    /**
     * Fill disposal returns that need to be approved
     */
    public void fillDisposalReturnsToApprove() {
        if (sessionController.getDepartment() == null) {
            disposalReturnsToApprove = new ArrayList<>();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT b FROM Bill b "
                + "WHERE b.retired = :retired "
                + "AND b.billTypeAtomic = :billTypeAtomic "
                + "AND b.department = :department "
                + "AND b.checked = :checked "
                + "AND (b.cancelled = :cancelled OR b.cancelled IS NULL) "
                + "AND (b.completed = :completed OR b.completed IS NULL) "
                + "ORDER BY b.checkeAt DESC";

        params.put("retired", false);
        params.put("billTypeAtomic", BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        params.put("department", sessionController.getDepartment());
        params.put("checked", true);
        params.put("cancelled", false);
        params.put("completed", false);

        disposalReturnsToApprove = billFacade.findByJpql(jpql, params);
        if (disposalReturnsToApprove == null) {
            disposalReturnsToApprove = new ArrayList<>();
        }
    }

    /**
     * Navigate to the unified disposal return page for a specific return bill
     * This will be called from the list pages when user wants to process a
     * specific return
     */
    public String navigateToProcessDisposalReturn(Bill disposalReturnBill) {
        if (disposalReturnBill == null) {
            JsfUtil.addErrorMessage("No disposal return bill selected");
            return null;
        }
        if (disposalReturnBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("No bill Error");
            return null;
        }

        // Set the bill in the IssueReturnController
        issueReturnController.setReturnBill(billService.reloadBill(disposalReturnBill));
        issueReturnController.setOriginalBill(billService.reloadBill(disposalReturnBill.getReferenceBill()));
        issueReturnController.setReturnBillItems(disposalReturnBill.getBillItems());
        issueReturnController.setOriginalBillItems(disposalReturnBill.getReferenceBill().getBillItems());
        // Navigate to the unified return processing page
        return "/pharmacy/pharmacy_bill_return_issue?faces-redirect=true";
    }

    /**
     * Reset all workflow data
     */
    public void makeNull() {
        disposalReturnsToFinalize = null;
        disposalReturnsToApprove = null;
        filteredDisposalReturnsToFinalize = null;
        filteredDisposalReturnsToApprove = null;
        selectedBillToFinalize = null;
        selectedBillToApprove = null;
    }

    // Getters and Setters
    public List<Bill> getDisposalReturnsToFinalize() {
        return disposalReturnsToFinalize;
    }

    public void setDisposalReturnsToFinalize(List<Bill> disposalReturnsToFinalize) {
        this.disposalReturnsToFinalize = disposalReturnsToFinalize;
    }

    public List<Bill> getDisposalReturnsToApprove() {
        return disposalReturnsToApprove;
    }

    public void setDisposalReturnsToApprove(List<Bill> disposalReturnsToApprove) {
        this.disposalReturnsToApprove = disposalReturnsToApprove;
    }

    public List<Bill> getFilteredDisposalReturnsToFinalize() {
        return filteredDisposalReturnsToFinalize;
    }

    public void setFilteredDisposalReturnsToFinalize(List<Bill> filteredDisposalReturnsToFinalize) {
        this.filteredDisposalReturnsToFinalize = filteredDisposalReturnsToFinalize;
    }

    public List<Bill> getFilteredDisposalReturnsToApprove() {
        return filteredDisposalReturnsToApprove;
    }

    public void setFilteredDisposalReturnsToApprove(List<Bill> filteredDisposalReturnsToApprove) {
        this.filteredDisposalReturnsToApprove = filteredDisposalReturnsToApprove;
    }

    public Bill getSelectedBillToFinalize() {
        return selectedBillToFinalize;
    }

    public void setSelectedBillToFinalize(Bill selectedBillToFinalize) {
        this.selectedBillToFinalize = selectedBillToFinalize;
    }

    public Bill getSelectedBillToApprove() {
        return selectedBillToApprove;
    }

    public void setSelectedBillToApprove(Bill selectedBillToApprove) {
        this.selectedBillToApprove = selectedBillToApprove;
    }

    public String getActiveIndexForReturnsAndCancellations() {
        return activeIndexForReturnsAndCancellations;
    }

    public void setActiveIndexForReturnsAndCancellations(String activeIndexForReturnsAndCancellations) {
        this.activeIndexForReturnsAndCancellations = activeIndexForReturnsAndCancellations;
    }
}
