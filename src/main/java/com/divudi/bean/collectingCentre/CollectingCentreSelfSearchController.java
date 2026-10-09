package com.divudi.bean.collectingCentre;

import com.divudi.bean.common.BillSearch;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backs the "Search Bills" section of the Collecting Centre self-service pages.
 * Unlike the admin-side collecting_centre_search_bill_own.xhtml, the search is
 * always restricted to the logged-in Collecting Centre's own bills.
 */
@Named
@SessionScoped
public class CollectingCentreSelfSearchController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillService billService;

    @Inject
    private SessionController sessionController;
    @Inject
    private BillSearch billSearch;

    private static final String SUBMITTED_BILL_ID_PARAM = "ccSelfBillId";

    private Date fromDate;
    private Date toDate;
    private SearchKeyword searchKeyword;
    private List<Bill> bills;
    private Bill viewingBill;

    public String navigateToSearchBills() {
        fromDate = CommonFunctions.getStartOfDay();
        toDate = CommonFunctions.getEndOfDay();
        searchKeyword = new SearchKeyword();
        bills = null;
        return "/collecting_centre/cc_self_search_bills?faces-redirect=true";
    }

    public String navigateBackToSearchBills() {
        viewingBill = null;
        return "/collecting_centre/cc_self_search_bills?faces-redirect=true";
    }

    public String navigateToReprintBill(Bill bill) {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return null;
        }
        Bill fetched = billFacade.find(bill.getId());
        if (fetched == null || !isOwnBill(fetched)) {
            JsfUtil.addErrorMessage("This bill does not belong to your collecting centre");
            return null;
        }
        fetched.setBillItems(billService.fetchBillItems(fetched));
        viewingBill = fetched;
        return "/collecting_centre/cc_self_bill_reprint?faces-redirect=true";
    }

    public String navigateBackToReprintBill() {
        if (viewingBill == null) {
            return navigateBackToSearchBills();
        }
        return navigateToReprintBill(viewingBill);
    }

    public String navigateToOriginalBillPrint() {
        if (!isViewingOwnBill()) {
            return null;
        }
        return "/collecting_centre/cc_self_bill_original_print?faces-redirect=true";
    }

    /**
     * Reuses BillSearch's collecting centre cancellation flow (privileges and
     * cancel-request approval included); only the normal cancel page is swapped
     * for the self-service one.
     */
    public String navigateToCancelBill() {
        if (!isViewingOwnBill() || !isSelfBillOrError(viewingBill)) {
            return null;
        }
        billSearch.setBill(viewingBill);
        billSearch.setComment(null);
        String outcome = billSearch.navigateToCancelCollectingCentreBill();
        if (outcome != null && outcome.startsWith("/collecting_centre/bill_cancel")) {
            return "/collecting_centre/cc_self_bill_cancel?faces-redirect=true";
        }
        return outcome;
    }

    public String navigateToRefundBill() {
        if (!isViewingOwnBill() || !isSelfBillOrError(viewingBill)) {
            return null;
        }
        billSearch.setBill(viewingBill);
        billSearch.setComment(null);
        billSearch.setRefundingItems(new ArrayList<>());
        billSearch.setRefundAmount(0.0);
        String outcome = billSearch.navigateToRefundCollectingCentreBill();
        if (outcome == null || outcome.isEmpty()) {
            return outcome;
        }
        return "/collecting_centre/cc_self_bill_refund?faces-redirect=true";
    }

    /**
     * Self-service submit for cancel: re-checks the bill held by the
     * session-scoped BillSearch before delegating, since the page can be
     * reached without navigateToCancelBill().
     */
    public void cancelBill() {
        Bill fetched = fetchSubmittedOwnBill();
        if (fetched == null || !isSelfBillOrError(fetched)) {
            return;
        }
        if (fetched.isCancelled()) {
            JsfUtil.addErrorMessage("This bill is already cancelled");
            return;
        }
        if (fetched.isRefunded()) {
            JsfUtil.addErrorMessage("This bill has refunds and can not be cancelled");
            return;
        }
        billSearch.cancelCollectingCentreBill();
    }

    /**
     * Self-service submit for refund: same checks as cancelBill(). Already
     * refunded bills are allowed, as remaining items can still be refunded.
     */
    public String refundBill() {
        Bill fetched = fetchSubmittedOwnBill();
        if (fetched == null || !isSelfBillOrError(fetched)) {
            return "";
        }
        return billSearch.refundCollectingCenterBill();
    }

    /**
     * Returns the persisted bill if the bill ID submitted with the form (the
     * bill the page was rendered for) still matches BillSearch's session bill
     * and belongs to this collecting centre; otherwise adds an error and
     * returns null. Guards against another tab replacing the session bill.
     */
    private Bill fetchSubmittedOwnBill() {
        Bill current = billSearch.getBill();
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return null;
        }
        String submittedId = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get(SUBMITTED_BILL_ID_PARAM);
        if (submittedId == null || !submittedId.equals(String.valueOf(current.getId()))) {
            JsfUtil.addErrorMessage("The selected bill was changed in another tab. Please reopen the bill and try again.");
            return null;
        }
        Bill fetched = billFacade.find(current.getId());
        if (fetched == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return null;
        }
        if (!isOwnBill(fetched)) {
            JsfUtil.addErrorMessage("This bill does not belong to your collecting centre");
            return null;
        }
        return fetched;
    }

    private boolean isViewingOwnBill() {
        if (viewingBill == null || viewingBill.getId() == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return false;
        }
        if (!isOwnBill(viewingBill)) {
            JsfUtil.addErrorMessage("This bill does not belong to your collecting centre");
            return false;
        }
        return true;
    }

    /**
     * Only bills raised by the centre itself (Self Billing) can be cancelled
     * or refunded here; bills raised at the hospital for the centre
     * (Hospital Billing) are view only.
     */
    public boolean isViewingSelfBill() {
        return isSelfBill(viewingBill);
    }

    private boolean isSelfBillOrError(Bill bill) {
        if (!isSelfBill(bill)) {
            JsfUtil.addErrorMessage("This is a Hospital Billing bill and cannot be cancelled or refunded here");
            return false;
        }
        return true;
    }

    private boolean isSelfBill(Bill bill) {
        return bill != null
                && bill.getDepartment() != null
                && bill.getDepartment().getDepartmentType() == DepartmentType.CollectingCentre;
    }

    public void searchBills() {
        bills = null;
        Institution collectingCentre = sessionController.getInstitution();
        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Collecting centre not found for the logged department");
            return;
        }
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select From and To dates");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = "select bill "
                + " from BilledBill bill "
                + " where bill.billType = :billType "
                + " and bill.fromInstitution = :cc "
                + " and bill.createdAt between :fromDate and :toDate "
                + " and bill.retired = false ";

        SearchKeyword sk = getSearchKeyword();

        if (hasText(sk.getBillNo())) {
            jpql += " and (bill.insId like :billNo or bill.deptId like :billNo) ";
            params.put("billNo", "%" + sk.getBillNo().trim().toUpperCase() + "%");
        }

        if (hasText(sk.getPatientName())) {
            jpql += " and bill.patient.person.name like :patientName ";
            params.put("patientName", "%" + sk.getPatientName().trim().toUpperCase() + "%");
        }

        if (hasText(sk.getPatientPhone())) {
            jpql += " and bill.patient.person.phone like :patientPhone ";
            params.put("patientPhone", "%" + sk.getPatientPhone().trim() + "%");
        }

        jpql += " order by bill.createdAt desc ";

        params.put("billType", BillType.CollectingCentreBill);
        params.put("cc", collectingCentre);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        bills = billFacade.findByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
    }

    private boolean isOwnBill(Bill bill) {
        Institution collectingCentre = sessionController.getInstitution();
        return collectingCentre != null
                && bill.getFromInstitution() != null
                && collectingCentre.equals(bill.getFromInstitution());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Double parseAmount(String value) {
        try {
            return Double.valueOf(value.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
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

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public Bill getViewingBill() {
        return viewingBill;
    }

    public void setViewingBill(Bill viewingBill) {
        this.viewingBill = viewingBill;
    }
}
