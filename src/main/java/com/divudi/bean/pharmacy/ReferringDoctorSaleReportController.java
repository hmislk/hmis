package com.divudi.bean.pharmacy;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Institution;
import com.divudi.core.data.dto.ReferringDoctorSaleBillDTO;
import com.divudi.core.data.dto.ReferringDoctorSaleItemDTO;
import com.divudi.core.data.dto.ReferringDoctorSaleSummaryDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.facade.DoctorFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.pharmacy.ReferringDoctorSaleReportService;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * Referring Doctor Wise Sale Report for pharmacy retail sales (DTO based).
 * The legacy ReportController.processPresciptionSummeryReport() is untouched.
 */
@Named
@SessionScoped
public class ReferringDoctorSaleReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ReferringDoctorSaleReportService service;
    @EJB
    private DoctorFacade doctorFacade;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private PaymentMethod paymentMethod;
    private Department department;
    private Doctor referringDoctor;
    private boolean onlyWithoutDoctor;
    private List<ReferringDoctorSaleSummaryDTO> summaryRows;
    private List<ReferringDoctorSaleBillDTO> billRows;
    private List<ReferringDoctorSaleItemDTO> itemRows;
    private String selectedDoctorName;

    public String navigateToReferringDoctorSaleReport() {
        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay();
        institution = null;
        site = null;
        paymentMethod = null;
        department = null;
        referringDoctor = null;
        onlyWithoutDoctor = false;
        summaryRows = null;
        billRows = null;
        itemRows = null;
        selectedDoctorName = null;
        return "/pharmacy/reports/referring_doctor_sale_report?faces-redirect=true";
    }

    private boolean datesValid() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both the from and to dates");
            return false;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From date cannot be after to date");
            return false;
        }
        if (referringDoctor != null && onlyWithoutDoctor) {
            JsfUtil.addErrorMessage("Clear the Referring Doctor or untick Only bills without a referring doctor. Both cannot apply together.");
            return false;
        }
        return true;
    }

    /** The doctor wins when both a doctor and "only without doctor" are selected. */
    private boolean effectiveOnlyWithoutDoctor() {
        return referringDoctor == null && onlyWithoutDoctor;
    }

    public void processSummary() {
        if (!datesValid()) {
            return;
        }
        List<ReferringDoctorSaleBillDTO> bills = service.fetchBills(fromDate, toDate, institution, site,
                department, referringDoctor, effectiveOnlyWithoutDoctor(), paymentMethod);
        summaryRows = service.summarize(bills);
        billRows = null;
        itemRows = null;
        selectedDoctorName = null;
    }

    public void processBillList() {
        if (!datesValid()) {
            return;
        }
        billRows = service.fetchBills(fromDate, toDate, institution, site,
                department, referringDoctor, effectiveOnlyWithoutDoctor(), paymentMethod);
        itemRows = null;
        selectedDoctorName = currentDoctorLabel();
    }

    public void processItemList() {
        if (!datesValid()) {
            return;
        }
        itemRows = service.fetchItems(fromDate, toDate, institution, site,
                department, referringDoctor, effectiveOnlyWithoutDoctor(), paymentMethod);
        billRows = null;
        selectedDoctorName = currentDoctorLabel();
    }

    private String currentDoctorLabel() {
        if (referringDoctor != null) {
            return referringDoctor.getPerson() != null && referringDoctor.getPerson().getName() != null
                    ? referringDoctor.getPerson().getName() : "Doctor #" + referringDoctor.getId();
        }
        if (onlyWithoutDoctor) {
            return ReferringDoctorSaleBillDTO.NO_DOCTOR;
        }
        return "All doctors";
    }

    private void selectDoctor(Long doctorId) {
        if (doctorId == null) {
            onlyWithoutDoctor = true;
            referringDoctor = null;
        } else {
            referringDoctor = doctorFacade.find(doctorId);
            onlyWithoutDoctor = false;
        }
    }

    public void listBillsForDoctor(Long doctorId) {
        selectDoctor(doctorId);
        processBillList();
    }

    public void listItemsForDoctor(Long doctorId) {
        selectDoctor(doctorId);
        processItemList();
    }

    public void clearDoctorFilter() {
        referringDoctor = null;
        onlyWithoutDoctor = false;
    }

    // ---- totals ----
    public long getTotalSaleCount() {
        long t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getSaleCount();
            }
        }
        return t;
    }

    public double getTotalSaleGross() {
        double t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getSaleGross();
            }
        }
        return t;
    }

    public double getTotalSaleDiscount() {
        double t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getSaleDiscount();
            }
        }
        return t;
    }

    public double getTotalSaleNet() {
        double t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getSaleNet();
            }
        }
        return t;
    }

    public long getTotalCancelCount() {
        long t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getCancelCount();
            }
        }
        return t;
    }

    public double getTotalCancelValue() {
        double t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getCancelValue();
            }
        }
        return t;
    }

    public long getTotalReturnCount() {
        long t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getReturnCount();
            }
        }
        return t;
    }

    public double getTotalReturnValue() {
        double t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getReturnValue();
            }
        }
        return t;
    }

    public double getTotalNetValue() {
        double t = 0;
        if (summaryRows != null) {
            for (ReferringDoctorSaleSummaryDTO r : summaryRows) {
                t += r.getNetValue();
            }
        }
        return t;
    }

    public double getBillTotalGross() {
        double t = 0;
        if (billRows != null) {
            for (ReferringDoctorSaleBillDTO r : billRows) {
                t += r.getGrossTotal() == null ? 0 : r.getGrossTotal();
            }
        }
        return t;
    }

    public double getBillTotalDiscount() {
        double t = 0;
        if (billRows != null) {
            for (ReferringDoctorSaleBillDTO r : billRows) {
                t += r.getDiscount() == null ? 0 : r.getDiscount();
            }
        }
        return t;
    }

    public double getBillTotalNet() {
        double t = 0;
        if (billRows != null) {
            for (ReferringDoctorSaleBillDTO r : billRows) {
                t += r.getNetTotal() == null ? 0 : r.getNetTotal();
            }
        }
        return t;
    }

    public double getItemTotalQuantity() {
        double t = 0;
        if (itemRows != null) {
            for (ReferringDoctorSaleItemDTO r : itemRows) {
                t += r.getQuantity() == null ? 0 : r.getQuantity();
            }
        }
        return t;
    }

    public double getItemTotalNetValue() {
        double t = 0;
        if (itemRows != null) {
            for (ReferringDoctorSaleItemDTO r : itemRows) {
                t += r.getNetValue() == null ? 0 : r.getNetValue();
            }
        }
        return t;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public List<ReferringDoctorSaleSummaryDTO> getSummaryRows() {
        return summaryRows;
    }

    public void setSummaryRows(List<ReferringDoctorSaleSummaryDTO> summaryRows) {
        this.summaryRows = summaryRows;
    }

    public List<ReferringDoctorSaleBillDTO> getBillRows() {
        return billRows;
    }

    public void setBillRows(List<ReferringDoctorSaleBillDTO> billRows) {
        this.billRows = billRows;
    }

    public List<ReferringDoctorSaleItemDTO> getItemRows() {
        return itemRows;
    }

    public void setItemRows(List<ReferringDoctorSaleItemDTO> itemRows) {
        this.itemRows = itemRows;
    }

    public String getSelectedDoctorName() {
        return selectedDoctorName;
    }

    public void setSelectedDoctorName(String selectedDoctorName) {
        this.selectedDoctorName = selectedDoctorName;
    }
}
