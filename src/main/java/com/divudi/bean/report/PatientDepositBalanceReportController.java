package com.divudi.bean.report;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.dto.PatientDepositBalanceDto;
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
 * Controller for Patient Deposit Balance Report.
 * Shows the last balance for each patient/department combination as of a specific date.
 * Uses the last PatientDepositHistory record before end of selected date.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PatientDepositBalanceReportController implements Serializable {

    @EJB
    private PatientDepositHistoryFacade patientDepositHistoryFacade;

    @Inject
    private SessionController sessionController;

    // Filter fields
    private Date reportDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private Patient patient;

    // Results
    private List<PatientDepositBalanceDto> patientDepositBalanceDtos;

    // Totals
    private double totalBalance;

    public PatientDepositBalanceReportController() {
    }

    /**
     * Initialize default date on first access
     */
    private void initializeDate() {
        if (reportDate == null) {
            reportDate = new Date();
        }
    }

    /**
     * Generate the Patient Deposit Balance report.
     * Finds the last history record for each patient/department combination
     * as of end of selected date, where balance > 0.
     */
    public void generateReport() {
        initializeDate();
        patientDepositBalanceDtos = fetchPatientDepositBalances();
        calculateTotals();
        if (patientDepositBalanceDtos == null || patientDepositBalanceDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No data found for the selected criteria");
        }
    }

    /**
     * Fetch patient deposit balances as DTOs.
     * Strategy: Get the last PatientDepositHistory record for each patient+department
     * combination where createdAt <= end of reportDate and balanceAfterTransaction > 0.
     */
    private List<PatientDepositBalanceDto> fetchPatientDepositBalances() {
        Date endOfDay = CommonFunctions.getEndOfDay(reportDate);

        // Build a subquery to get the MAX(id) for each patient+department combination
        // Then join back to get the full record details
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT NEW com.divudi.core.data.dto.PatientDepositBalanceDto(");
        jpql.append("pdh.id, ");
        jpql.append("pdh.patientDeposit.patient.id, ");
        jpql.append("pdh.patientDeposit.patient.person.name, ");
        jpql.append("pdh.patientDeposit.patient.phn, ");
        jpql.append("pdh.department.id, ");
        jpql.append("pdh.department.name, ");
        jpql.append("pdh.site.id, ");
        jpql.append("pdh.site.name, ");
        jpql.append("pdh.institution.id, ");
        jpql.append("pdh.institution.name, ");
        jpql.append("pdh.balanceAfterTransaction");
        jpql.append(") ");
        jpql.append("FROM PatientDepositHistory pdh ");
        jpql.append("WHERE pdh.retired = :ret ");
        jpql.append("AND pdh.createdAt <= :endDate ");
        jpql.append("AND pdh.balanceAfterTransaction > 0 ");

        // Subquery to get max ID for each patient+department combination
        jpql.append("AND pdh.id = (");
        jpql.append("SELECT MAX(pdh2.id) FROM PatientDepositHistory pdh2 ");
        jpql.append("WHERE pdh2.retired = :ret ");
        jpql.append("AND pdh2.createdAt <= :endDate ");
        jpql.append("AND pdh2.patientDeposit.patient = pdh.patientDeposit.patient ");
        jpql.append("AND pdh2.department = pdh.department");
        jpql.append(") ");

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("endDate", endOfDay);

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

        jpql.append("ORDER BY pdh.patientDeposit.patient.phn, pdh.department.name");

        List<?> results = patientDepositHistoryFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        List<PatientDepositBalanceDto> dtos = new ArrayList<>();
        for (Object obj : results) {
            if (obj instanceof PatientDepositBalanceDto) {
                dtos.add((PatientDepositBalanceDto) obj);
            }
        }
        return dtos;
    }

    /**
     * Calculate total balance from the DTO list.
     */
    private void calculateTotals() {
        totalBalance = 0.0;

        if (patientDepositBalanceDtos != null) {
            for (PatientDepositBalanceDto dto : patientDepositBalanceDtos) {
                if (dto.getBalance() != null) {
                    totalBalance += dto.getBalance();
                }
            }
        }
    }

    /**
     * Clear all filters and results.
     */
    public void clearReport() {
        reportDate = new Date();
        institution = null;
        site = null;
        department = null;
        patientDepositBalanceDtos = null;
        totalBalance = 0.0;
    }

    /**
     * Navigate to the report page.
     */
    public String navigateToPatientDepositBalanceReport() {
        clearReport();
        return "/patient_deposit/patient_deposit_balance_report?faces-redirect=true";
    }

    // Getters and Setters

    public Date getReportDate() {
        if (reportDate == null) {
            reportDate = new Date();
        }
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
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

    public List<PatientDepositBalanceDto> getPatientDepositBalanceDtos() {
        return patientDepositBalanceDtos;
    }

    public void setPatientDepositBalanceDtos(List<PatientDepositBalanceDto> patientDepositBalanceDtos) {
        this.patientDepositBalanceDtos = patientDepositBalanceDtos;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
    }
}
