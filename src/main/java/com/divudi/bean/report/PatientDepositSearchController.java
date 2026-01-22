package com.divudi.bean.report;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.PatientDepositBillDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 * Controller for Patient Deposit Search.
 * Provides DTO-based search with optional filters for institution, site, and
 * department.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PatientDepositSearchController implements Serializable {

    @EJB
    private BillFacade billFacade;

    @Inject
    private SessionController sessionController;

    // Filter fields
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private String patientName;
    private String billNo;

    // Results
    private List<PatientDepositBillDto> patientDepositBillDtos;

    // Selected bill for reprint navigation
    private Bill selectedBill;

    // Totals
    private double totalNetValue;

    public PatientDepositSearchController() {
    }

    /**
     * Initialize default date range on first access
     */
    private void initializeDates() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
    }

    /**
     * Search for patient deposit receive bills.
     */
    public void searchPatientDeposits() {
        searchPatientDepositBills(BillType.PatientPaymentReceiveBill);
    }

    /**
     * Search for patient deposit return/refund bills.
     */
    public void searchPatientDepositReturns() {
        searchPatientDepositBills(BillType.PatientPaymentRefundBill);
    }

    /**
     * Search for patient deposit bills using DTO-based query.
     */
    private void searchPatientDepositBills(BillType billType) {
        initializeDates();
        patientDepositBillDtos = fetchPatientDepositBillDtos(billType);
        calculateTotals();
        if (patientDepositBillDtos == null || patientDepositBillDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No data found for the selected criteria");
        }
    }

    /**
     * Fetch patient deposit bills as DTOs with filters applied.
     */
    private List<PatientDepositBillDto> fetchPatientDepositBillDtos(BillType billType) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT NEW com.divudi.core.data.dto.PatientDepositBillDto(");
        jpql.append("b.id, ");
        jpql.append("b.deptId, ");
        jpql.append("b.createdAt, ");
        jpql.append("b.netTotal, ");
        jpql.append("b.comments, ");
        jpql.append("p.id, ");
        jpql.append("per.name, ");
        jpql.append("p.phn, ");
        jpql.append("c.id, ");
        jpql.append("c.name, ");
        jpql.append("d.id, ");
        jpql.append("d.name, ");
        jpql.append("s.id, ");
        jpql.append("s.name, ");
        jpql.append("i.id, ");
        jpql.append("i.name, ");
        jpql.append("b.cancelled, ");
        jpql.append("b.refunded, ");
        jpql.append("cb.id, ");
        jpql.append("cb.deptId, ");
        jpql.append("cb.createdAt, ");
        jpql.append("cbc.name, ");
        jpql.append("cb.comments, ");
        jpql.append("rb.comments");
        jpql.append(") ");
        jpql.append("FROM Bill b ");
        jpql.append("LEFT JOIN b.patient p ");
        jpql.append("LEFT JOIN p.person per ");
        jpql.append("LEFT JOIN b.creater c ");
        jpql.append("LEFT JOIN b.department d ");
        jpql.append("LEFT JOIN b.site s ");
        jpql.append("LEFT JOIN b.institution i ");
        jpql.append("LEFT JOIN b.cancelledBill cb ");
        jpql.append("LEFT JOIN cb.creater cbc ");
        jpql.append("LEFT JOIN b.refundedBill rb ");
        jpql.append("WHERE b.billType = :billType ");
        jpql.append("AND b.retired = :ret ");
        jpql.append("AND b.createdAt BETWEEN :fromDate AND :toDate ");

        Map<String, Object> m = new HashMap<>();
        m.put("billType", billType);
        m.put("ret", false);
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);

        // Optional filters
        if (institution != null) {
            jpql.append("AND b.institution = :institution ");
            m.put("institution", institution);
        }

        if (site != null) {
            jpql.append("AND b.site = :site ");
            m.put("site", site);
        }

        if (department != null) {
            jpql.append("AND b.department = :department ");
            m.put("department", department);
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql.append("AND UPPER(per.name) LIKE :patientName ");
            m.put("patientName", "%" + patientName.trim().toUpperCase() + "%");
        }

        if (billNo != null && !billNo.trim().isEmpty()) {
            jpql.append("AND UPPER(b.deptId) LIKE :billNo ");
            m.put("billNo", "%" + billNo.trim().toUpperCase() + "%");
        }

        jpql.append("ORDER BY b.createdAt DESC");

        List<?> results = billFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        List<PatientDepositBillDto> dtos = new ArrayList<>();
        for (Object obj : results) {
            if (obj instanceof PatientDepositBillDto) {
                dtos.add((PatientDepositBillDto) obj);
            }
        }
        return dtos;
    }

    /**
     * Calculate total net value from the DTO list.
     */
    private void calculateTotals() {
        totalNetValue = 0.0;

        if (patientDepositBillDtos != null) {
            for (PatientDepositBillDto dto : patientDepositBillDtos) {
                if (dto.getNetTotal() != null && !dto.isCancelled()) {
                    totalNetValue += dto.getNetTotal();
                }
            }
        }
    }

    /**
     * Clear all filters and results.
     */
    public void clearSearch() {
        fromDate = CommonFunctions.getStartOfDay(new Date());
        toDate = CommonFunctions.getEndOfDay(new Date());
        institution = null;
        site = null;
        department = null;
        patientName = null;
        billNo = null;
        patientDepositBillDtos = null;
        totalNetValue = 0.0;
    }

    /**
     * Navigate to the patient deposit search page.
     */
    public String navigateToPatientDepositSearch() {
        clearSearch();
        return "/patient_deposit/patient_deposit_search?faces-redirect=true";
    }

    /**
     * Navigate to the patient deposit return search page.
     */
    public String navigateToPatientDepositReturnSearch() {
        clearSearch();
        return "/patient_deposit/patient_deposit_return_search?faces-redirect=true";
    }

    /**
     * Find the Bill entity by ID for reprint navigation.
     */
    public Bill findBillById(Long billId) {
        if (billId == null) {
            return null;
        }
        return billFacade.find(billId);
    }

    /**
     * Set the selected bill by ID and navigate to reprint.
     */
    public String navigateToReprintBill(Long billId) {
        selectedBill = findBillById(billId);
        return "/patient_deposit/view/patient_deposit_bill_reprint?faces-redirect=true";
    }

    /**
     * Set the selected bill by ID and navigate to cancel reprint.
     */
    public String navigateToCancelReprintBill(Long billId) {
        selectedBill = findBillById(billId);
        return "/patient_deposit/view/patient_deposit_cancel_reprint?faces-redirect=true";
    }

    /**
     * Navigate to view the cancelled bill (for bills that have been cancelled).
     */
    public String navigateToViewCancelledBill(Long cancelledBillId) {
        selectedBill = findBillById(cancelledBillId);
        return "/patient_deposit/view/patient_deposit_cancel_reprint?faces-redirect=true";
    }

    // Getters and Setters

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public List<PatientDepositBillDto> getPatientDepositBillDtos() {
        return patientDepositBillDtos;
    }

    public void setPatientDepositBillDtos(List<PatientDepositBillDto> patientDepositBillDtos) {
        this.patientDepositBillDtos = patientDepositBillDtos;
    }

    public Bill getSelectedBill() {
        return selectedBill;
    }

    public void setSelectedBill(Bill selectedBill) {
        this.selectedBill = selectedBill;
    }

    public double getTotalNetValue() {
        return totalNetValue;
    }

    public void setTotalNetValue(double totalNetValue) {
        this.totalNetValue = totalNetValue;
    }
}
