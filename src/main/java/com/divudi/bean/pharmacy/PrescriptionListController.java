package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.UserSettingsController;
import com.divudi.core.data.dto.ReferringDoctorSaleBillDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.pharmacy.ReferringDoctorSaleReportService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Pharmacy Analytics > Retail Sale Reports > Prescription List: the retail
 * sale bill list by referring doctor, with cancellations and returns shown
 * as their own (negative) rows so the totals are net. The rows come from
 * {@link ReferringDoctorSaleReportService}, the same source as the Referring
 * Doctor Wise Sale Report, so the two reports always agree.
 */
@Named
@SessionScoped
public class PrescriptionListController implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String PAGE_ID = "pharmacy_prescription_list";

    @EJB
    private ReferringDoctorSaleReportService service;
    @Inject
    private UserSettingsController userSettingsController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private Doctor referringDoctor;
    private boolean onlyWithoutDoctor;
    private List<ReferringDoctorSaleBillDTO> rows;
    private List<ColumnSetting> columnSettings;

    private void clearFilters() {
        fromDate = CommonFunctions.getStartOfDay();
        toDate = CommonFunctions.getEndOfDay();
        institution = null;
        site = null;
        department = null;
        referringDoctor = null;
        onlyWithoutDoctor = false;
        rows = null;
    }

    public String navigateToPrescriptionList() {
        clearFilters();
        return "/pharmacy/prescription_list?faces-redirect=true";
    }

    /**
     * Drill-down from the legacy Prescription Summary: keep its date range
     * and department, list that doctor's bills straight away.
     */
    public String navigateToPrescriptionListForDoctor(Doctor doctor, Date from, Date to, Department dept) {
        clearFilters();
        if (from != null) {
            fromDate = from;
        }
        if (to != null) {
            toDate = to;
        }
        department = dept;
        referringDoctor = doctor;
        process();
        return "/pharmacy/prescription_list?faces-redirect=true";
    }

    public void process() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both the from and to dates");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From date cannot be after to date");
            return;
        }
        if (referringDoctor != null && onlyWithoutDoctor) {
            JsfUtil.addErrorMessage("Clear the Referring Doctor or untick Only bills without a referring doctor. Both cannot apply together.");
            return;
        }
        rows = service.fetchBills(fromDate, toDate, institution, site, department,
                referringDoctor, onlyWithoutDoctor, null);
    }

    // ---- totals ----
    public int getBillCount() {
        return rows == null ? 0 : rows.size();
    }

    public double getTotalGross() {
        double t = 0;
        if (rows != null) {
            for (ReferringDoctorSaleBillDTO r : rows) {
                t += r.getGrossTotal() == null ? 0 : r.getGrossTotal();
            }
        }
        return t;
    }

    public double getTotalDiscount() {
        double t = 0;
        if (rows != null) {
            for (ReferringDoctorSaleBillDTO r : rows) {
                t += r.getDiscount() == null ? 0 : r.getDiscount();
            }
        }
        return t;
    }

    public double getTotalNet() {
        double t = 0;
        if (rows != null) {
            for (ReferringDoctorSaleBillDTO r : rows) {
                t += r.getNetTotal() == null ? 0 : r.getNetTotal();
            }
        }
        return t;
    }

    // ---- screen settings ----
    public Long getRowsPerPage() {
        return configOptionApplicationController.getLongValueByKey("Pharmacy Analytics - Rows per Page for Screen", 20L);
    }

    public String getFromDateFormatted() {
        return CommonFunctions.formatDate(fromDate, "yyyyMMdd_HHmm");
    }

    public String getToDateFormatted() {
        return CommonFunctions.formatDate(toDate, "yyyyMMdd_HHmm");
    }

    /** Per-user column visibility, saved through UserSettingsController. */
    public boolean isColumnVisible(String columnId) {
        return userSettingsController.isColumnVisible(PAGE_ID, columnId);
    }

    public List<ColumnSetting> getColumnSettings() {
        if (columnSettings == null) {
            columnSettings = new ArrayList<>();
            columnSettings.add(new ColumnSetting("billType", "Bill Type"));
            columnSettings.add(new ColumnSetting("doctor", "Referring Doctor"));
            columnSettings.add(new ColumnSetting("patient", "Patient"));
            columnSettings.add(new ColumnSetting("department", "Department"));
            columnSettings.add(new ColumnSetting("paymentMethod", "Payment Method"));
            columnSettings.add(new ColumnSetting("gross", "Gross"));
            columnSettings.add(new ColumnSetting("discount", "Discount"));
        }
        return columnSettings;
    }

    public class ColumnSetting implements Serializable {

        private static final long serialVersionUID = 1L;
        private final String id;
        private final String label;

        ColumnSetting(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public boolean isVisible() {
            return userSettingsController.isColumnVisible(PAGE_ID, id);
        }

        public void setVisible(boolean visible) {
            if (visible != isVisible()) {
                userSettingsController.toggleColumnVisibility(PAGE_ID, id);
            }
        }
    }

    // ---- getters / setters ----
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

    public Doctor getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Doctor referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public boolean isOnlyWithoutDoctor() {
        return onlyWithoutDoctor;
    }

    public void setOnlyWithoutDoctor(boolean onlyWithoutDoctor) {
        this.onlyWithoutDoctor = onlyWithoutDoctor;
    }

    public List<ReferringDoctorSaleBillDTO> getRows() {
        return rows;
    }

    public void setRows(List<ReferringDoctorSaleBillDTO> rows) {
        this.rows = rows;
    }
}
