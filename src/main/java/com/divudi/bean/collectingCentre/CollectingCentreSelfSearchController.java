package com.divudi.bean.collectingCentre;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.io.Serializable;
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

        bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
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
