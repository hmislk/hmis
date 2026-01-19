package com.divudi.bean.report;

import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.dto.PatientDepositHistoryDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.facade.PatientDepositHistoryFacade;
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
 * Controller for Patient Deposit History.
 * Provides DTO-based search with mandatory patient and optional filters for
 * institution, site, and department.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PatientDepositHistoryController implements Serializable {

    @EJB
    private PatientDepositHistoryFacade patientDepositHistoryFacade;

    @Inject
    private SessionController sessionController;

    @Inject
    private PatientController patientController;

    // Filter fields
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private Patient patient;

    // Results
    private List<PatientDepositHistoryDto> patientDepositHistoryDtos;

    // Totals
    private double totalCredit;
    private double totalDebit;

    public PatientDepositHistoryController() {
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
     * Search for patient deposit history using DTO-based query.
     */
    public void searchPatientDepositHistory() {
        // Validate mandatory patient field
        if (patient == null) {
            JsfUtil.addErrorMessage("Please select a patient first");
            patientDepositHistoryDtos = null;
            return;
        }

        initializeDates();
        patientDepositHistoryDtos = fetchPatientDepositHistoryDtos();
        calculateTotals();

        if (patientDepositHistoryDtos == null || patientDepositHistoryDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No data found for the selected criteria");
        }
    }

    /**
     * Fetch patient deposit history as DTOs with filters applied.
     */
    private List<PatientDepositHistoryDto> fetchPatientDepositHistoryDtos() {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT NEW com.divudi.core.data.dto.PatientDepositHistoryDto(");
        jpql.append("pdh.id, ");
        jpql.append("b.id, ");
        jpql.append("b.deptId, ");
        jpql.append("b.billTypeAtomic, ");
        jpql.append("pdh.createdAt, ");
        jpql.append("pdh.historyType, ");
        jpql.append("p.id, ");
        jpql.append("per.name, ");
        jpql.append("p.phn, ");
        jpql.append("pdh.balanceBeforeTransaction, ");
        jpql.append("pdh.balanceAfterTransaction, ");
        jpql.append("pdh.transactionValue, ");
        jpql.append("d.id, ");
        jpql.append("d.name, ");
        jpql.append("s.id, ");
        jpql.append("s.name, ");
        jpql.append("i.id, ");
        jpql.append("i.name, ");
        jpql.append("b.cancelled, ");
        jpql.append("b.refunded, ");
        jpql.append("c.name");
        jpql.append(") ");
        jpql.append("FROM PatientDepositHistory pdh ");
        jpql.append("LEFT JOIN pdh.patientDeposit pd ");
        jpql.append("LEFT JOIN pd.patient p ");
        jpql.append("LEFT JOIN p.person per ");
        jpql.append("LEFT JOIN pdh.bill b ");
        jpql.append("LEFT JOIN b.creater c ");
        jpql.append("LEFT JOIN pdh.department d ");
        jpql.append("LEFT JOIN pdh.site s ");
        jpql.append("LEFT JOIN pdh.institution i ");
        jpql.append("WHERE pdh.retired = :ret ");
        jpql.append("AND pd.patient = :patient ");
        jpql.append("AND pdh.createdAt BETWEEN :fromDate AND :toDate ");

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("patient", patient);
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);

        // Optional filters
        if (institution != null) {
            jpql.append("AND pdh.institution = :institution ");
            m.put("institution", institution);
        }

        if (site != null) {
            jpql.append("AND pdh.site = :site ");
            m.put("site", site);
        }

        if (department != null) {
            jpql.append("AND pdh.department = :department ");
            m.put("department", department);
        }

        jpql.append("ORDER BY pdh.createdAt DESC");

        List<?> results = patientDepositHistoryFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        List<PatientDepositHistoryDto> dtos = new ArrayList<>();
        for (Object obj : results) {
            if (obj instanceof PatientDepositHistoryDto) {
                dtos.add((PatientDepositHistoryDto) obj);
            }
        }
        return dtos;
    }

    /**
     * Calculate total credit and debit from the DTO list.
     */
    private void calculateTotals() {
        totalCredit = 0.0;
        totalDebit = 0.0;

        if (patientDepositHistoryDtos != null) {
            for (PatientDepositHistoryDto dto : patientDepositHistoryDtos) {
                if (dto.getTransactionValue() != null) {
                    double transactionValue = dto.getTransactionValue();
                    if (transactionValue > 0) {
                        totalCredit += transactionValue;
                    } else if (transactionValue < 0) {
                        totalDebit += transactionValue;
                    }
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
        patient = null;
        patientDepositHistoryDtos = null;
        totalCredit = 0.0;
        totalDebit = 0.0;
    }

    /**
     * Navigate to the patient deposit history page.
     */
    public String navigateToPatientDepositHistory() {
        clearSearch();
        return "/patient_deposit/patient_deposit_history?faces-redirect=true";
    }

    /**
     * Set patient from selection and clear previous results.
     */
    public void selectPatient(Patient selectedPatient) {
        this.patient = selectedPatient;
        patientDepositHistoryDtos = null;
    }

    /**
     * Clear patient selection.
     */
    public void clearPatientSelection() {
        this.patient = null;
        patientDepositHistoryDtos = null;
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

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<PatientDepositHistoryDto> getPatientDepositHistoryDtos() {
        return patientDepositHistoryDtos;
    }

    public void setPatientDepositHistoryDtos(List<PatientDepositHistoryDto> patientDepositHistoryDtos) {
        this.patientDepositHistoryDtos = patientDepositHistoryDtos;
    }

    public double getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(double totalCredit) {
        this.totalCredit = totalCredit;
    }

    public double getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(double totalDebit) {
        this.totalDebit = totalDebit;
    }
}
