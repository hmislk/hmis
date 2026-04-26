package com.divudi.bean.pharmacy;

import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.PharmacyService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * F15 drill-down Level 1 controller — date-range summary parameterised by
 * transaction type and a multi-selected list of BillTypeAtomic.
 *
 * Reuses the F15 column layout but allows the user to scope the report to
 * a custom date range and a custom subset of bill type atomics. Calls into
 * the *ForAtomics PharmacyService methods added in issue #20237.
 *
 * Parent epic: #20236. This controller: #20238.
 */
@Named
@SessionScoped
public class PharmacyDailyStockDrillDownLevel1Controller implements Serializable {

    public enum TransactionType {
        SALES,
        PURCHASES,
        TRANSFERS,
        ADJUSTMENTS
    }

    @EJB
    private PharmacyService pharmacyService;
    @Inject
    private SessionController sessionController;
    @Inject
    private InstitutionController institutionController;
    @Inject
    private DepartmentController departmentController;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;

    private TransactionType transactionType = TransactionType.SALES;
    private List<BillTypeAtomic> availableAtomics = new ArrayList<>();
    private List<BillTypeAtomic> selectedAtomics = new ArrayList<>();

    private PharmacyBundle bundle;

    public PharmacyDailyStockDrillDownLevel1Controller() {
    }

    /**
     * Entry point used by the menu / direct navigation. Defaults filters to
     * session institution / site / department and today's date if not yet set,
     * then redirects to the L1 page.
     */
    public String navigateToLevel1() {
        if (institution == null) {
            institution = sessionController.getInstitution();
        }
        if (site == null) {
            site = sessionController.getLoggedSite();
        }
        if (department == null) {
            department = sessionController.getDepartment();
        }
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        rebuildAtomicOptions();
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level1?faces-redirect=true";
    }

    /**
     * Entry point reserved for the F15 → L1 drill-down (issue #20239).
     * Copies F15 filters and the chosen transaction type / initial atomic list
     * into L1 state, then redirects. Not yet wired from F15 — kept for #20239.
     */
    public String navigateToLevel1FromF15(Date f15FromDate, Date f15ToDate,
                                          Institution f15Institution, Institution f15Site, Department f15Department,
                                          TransactionType type, List<BillTypeAtomic> initialAtomics) {
        this.fromDate = f15FromDate;
        this.toDate = f15ToDate;
        this.institution = f15Institution;
        this.site = f15Site;
        this.department = f15Department;
        this.transactionType = type != null ? type : TransactionType.SALES;
        rebuildAtomicOptions();
        if (initialAtomics != null && !initialAtomics.isEmpty()) {
            this.selectedAtomics = new ArrayList<>(initialAtomics);
        }
        processReport();
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level1?faces-redirect=true";
    }

    /**
     * AJAX-invoked when the transaction-type radio changes. Repopulates
     * availableAtomics from the matching getPharmacy*BillTypes() and ticks all
     * by default.
     */
    public void onTransactionTypeChange() {
        rebuildAtomicOptions();
    }

    private void rebuildAtomicOptions() {
        availableAtomics = atomicsForCurrentType();
        selectedAtomics = new ArrayList<>(availableAtomics);
    }

    private List<BillTypeAtomic> atomicsForCurrentType() {
        if (transactionType == null) {
            return Collections.emptyList();
        }
        switch (transactionType) {
            case SALES:
                return pharmacyService.getPharmacyIncomeBillTypes();
            case PURCHASES:
                return pharmacyService.getPharmacyPurchaseBillTypes();
            case TRANSFERS:
                return pharmacyService.getPharmacyInternalTransferBillTypes();
            case ADJUSTMENTS:
                return pharmacyService.getPharmacyAdjustmentBillTypes();
            default:
                return Collections.emptyList();
        }
    }

    public void processReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates");
            return;
        }
        if (transactionType == null) {
            JsfUtil.addErrorMessage("Please select a transaction type");
            return;
        }
        Date startOfDay = CommonFunctions.getStartOfDay(fromDate);
        Date endOfDay = CommonFunctions.getEndOfDay(toDate);
        List<BillTypeAtomic> atomics = (selectedAtomics == null || selectedAtomics.isEmpty())
                ? atomicsForCurrentType()
                : selectedAtomics;

        switch (transactionType) {
            case SALES:
                bundle = pharmacyService.fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionTypeDtoForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
            case PURCHASES:
                bundle = pharmacyService.fetchPharmacyStockPurchaseValueByBillTypeDtoCompletedForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
            case TRANSFERS:
                bundle = pharmacyService.fetchPharmacyTransferValueByBillTypeDtoForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
            case ADJUSTMENTS:
                bundle = pharmacyService.fetchPharmacyAdjustmentValueByBillTypeDtoForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
        }
    }

    /**
     * Back-to-F15 navigation. F15's controller is @SessionScoped so its
     * filter values persist automatically — no explicit hand-back needed.
     */
    public String navigateBackToF15() {
        return "/pharmacy/reports/summary_reports/daily_stock_values_report_optimized?faces-redirect=true";
    }

    /**
     * Stub for Level 2 drill-down (issue #20240). Replaced when L2 lands.
     */
    public void drillToLevel2(PharmacyRow row) {
        JsfUtil.addInfoMessage("Level 2 drill-down (bill list) is coming in a future update");
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public List<BillTypeAtomic> getAvailableAtomics() {
        return availableAtomics;
    }

    public void setAvailableAtomics(List<BillTypeAtomic> availableAtomics) {
        this.availableAtomics = availableAtomics;
    }

    public List<BillTypeAtomic> getSelectedAtomics() {
        return selectedAtomics;
    }

    public void setSelectedAtomics(List<BillTypeAtomic> selectedAtomics) {
        this.selectedAtomics = selectedAtomics;
    }

    public PharmacyBundle getBundle() {
        return bundle;
    }

    public void setBundle(PharmacyBundle bundle) {
        this.bundle = bundle;
    }

    public TransactionType[] getTransactionTypes() {
        return TransactionType.values();
    }
}
