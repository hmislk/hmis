/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.data.dto.PharmacyReturnWithoutTrasingBillDTO;
import com.divudi.core.data.dto.PharmacyReturnWithoutTrasingBillItemDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Session-scoped controller for Pharmacy Return Without Trasing Report
 * Supports both "By Bill" and "By Bill Items" report views with modern filtering
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Named
@SessionScoped
public class PharmacyReturnWithoutTrasingReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    // Filter properties (matching procurement report pattern)
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private ReportViewType reportViewType;

    // Report data containers
    private List<PharmacyReturnWithoutTrasingBillDTO> billReportData;
    private List<PharmacyReturnWithoutTrasingBillItemDTO> billItemReportData;
    private PharmacyReturnWithoutTrasingBillDTO summaryRow;
    private int totalRecordCount;

    // Display configuration
    private String fontSizeForScreen = "12px";
    private Integer rowsPerPageForScreen = 25;

    // Injected services
    @EJB
    private BillService billService;
    @Inject
    private SessionController sessionController;
    @Inject
    private InstitutionController institutionController;
    @Inject
    private DepartmentController departmentController;

    // Main processing method
    public void processReport() {
        if (reportViewType == null) {
            JsfUtil.addErrorMessage("Please select a report view type.");
            return;
        }

        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both from and to dates.");
            return;
        }

        try {
            switch (reportViewType) {
                case BY_BILL:
                    processReportByBill();
                    break;
                case BY_BILL_ITEM:
                    processReportByBillItem();
                    break;
                default:
                    JsfUtil.addErrorMessage("Unsupported report view type: " + reportViewType.getLabel());
                    break;
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error processing report: " + e.getMessage());
        }
    }

    private void processReportByBill() {
        // Clear previous data
        billItemReportData = null;
        summaryRow = null;

        // Fetch bill-level data
        billReportData = billService.fetchPharmacyReturnWithoutTrasingBillDTOs(
                fromDate, toDate, institution, site, department, sessionController.getLoggedUser());

        calculateBillSummary();

        if (billReportData != null && !billReportData.isEmpty()) {
            JsfUtil.addSuccessMessage("Report generated successfully. Found " + totalRecordCount + " bills.");
        } else {
            JsfUtil.addInfoMessage("No records found for the selected criteria.");
        }
    }

    private void processReportByBillItem() {
        // Clear previous data
        billReportData = null;
        summaryRow = null;

        // Fetch item-level data
        billItemReportData = billService.fetchPharmacyReturnWithoutTrasingBillItemDTOs(
                fromDate, toDate, institution, site, department, sessionController.getLoggedUser());

        calculateBillItemSummary();

        if (billItemReportData != null && !billItemReportData.isEmpty()) {
            JsfUtil.addSuccessMessage("Report generated successfully. Found " + billItemReportData.size() + " bill items.");
        } else {
            JsfUtil.addInfoMessage("No records found for the selected criteria.");
        }
    }

    private void calculateBillSummary() {
        // Initialize totals
        double totalGross = 0.0;
        double totalNet = 0.0;
        double totalDiscount = 0.0;
        double totalCost = 0.0;
        double totalPurchase = 0.0;
        double totalRetail = 0.0;

        totalRecordCount = 0;

        if (billReportData != null) {
            for (PharmacyReturnWithoutTrasingBillDTO dto : billReportData) {
                totalGross += (dto.getGrossTotal() != null ? dto.getGrossTotal() : 0.0);
                totalNet += (dto.getNetTotal() != null ? dto.getNetTotal() : 0.0);
                totalDiscount += (dto.getDiscount() != null ? dto.getDiscount() : 0.0);
                totalCost += (dto.getTotalCostValue() != null ? dto.getTotalCostValue() : 0.0);
                totalPurchase += (dto.getTotalPurchaseValue() != null ? dto.getTotalPurchaseValue() : 0.0);
                totalRetail += (dto.getTotalRetailValue() != null ? dto.getTotalRetailValue() : 0.0);
            }
            totalRecordCount = billReportData.size();
        }

        // Create summary row
        summaryRow = new PharmacyReturnWithoutTrasingBillDTO();
        summaryRow.setGrossTotal(totalGross);
        summaryRow.setNetTotal(totalNet);
        summaryRow.setDiscount(totalDiscount);
        summaryRow.setTotalCostValue(totalCost);
        summaryRow.setTotalPurchaseValue(totalPurchase);
        summaryRow.setTotalRetailValue(totalRetail);
    }

    private void calculateBillItemSummary() {
        // For bill items, we could also calculate summary if needed
        // For now, just set the count
        totalRecordCount = (billItemReportData != null) ? billItemReportData.size() : 0;
    }

    // Reset methods
    public void resetFilters() {
        fromDate = null;
        toDate = null;
        institution = null;
        site = null;
        department = null;
        reportViewType = null;
        clearReportData();
    }

    public void clearReportData() {
        billReportData = null;
        billItemReportData = null;
        summaryRow = null;
        totalRecordCount = 0;
    }

    // Lazy initialization getters with defaults
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

    public ReportViewType getReportViewType() {
        return reportViewType;
    }

    public void setReportViewType(ReportViewType reportViewType) {
        this.reportViewType = reportViewType;
    }

    public List<PharmacyReturnWithoutTrasingBillDTO> getBillReportData() {
        return billReportData;
    }

    public void setBillReportData(List<PharmacyReturnWithoutTrasingBillDTO> billReportData) {
        this.billReportData = billReportData;
    }

    public List<PharmacyReturnWithoutTrasingBillItemDTO> getBillItemReportData() {
        return billItemReportData;
    }

    public void setBillItemReportData(List<PharmacyReturnWithoutTrasingBillItemDTO> billItemReportData) {
        this.billItemReportData = billItemReportData;
    }

    public PharmacyReturnWithoutTrasingBillDTO getSummaryRow() {
        return summaryRow;
    }

    public void setSummaryRow(PharmacyReturnWithoutTrasingBillDTO summaryRow) {
        this.summaryRow = summaryRow;
    }

    public int getTotalRecordCount() {
        return totalRecordCount;
    }

    public void setTotalRecordCount(int totalRecordCount) {
        this.totalRecordCount = totalRecordCount;
    }

    public String getFontSizeForScreen() {
        return fontSizeForScreen;
    }

    public void setFontSizeForScreen(String fontSizeForScreen) {
        this.fontSizeForScreen = fontSizeForScreen;
    }

    public Integer getRowsPerPageForScreen() {
        return rowsPerPageForScreen;
    }

    public void setRowsPerPageForScreen(Integer rowsPerPageForScreen) {
        this.rowsPerPageForScreen = rowsPerPageForScreen;
    }
}