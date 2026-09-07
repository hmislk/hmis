package com.divudi.bean.report;

import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.dto.PatientDepositBalanceDto;
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
 * Controller for Patient-Specific Deposit Balance Report.
 * Shows deposit balances for a selected patient across all departments as of a specific date.
 * Uses the last PatientDepositHistory record before end of selected date.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PatientSpecificDepositBalanceReportController implements Serializable {

    @EJB
    private PatientDepositHistoryFacade patientDepositHistoryFacade;

    @Inject
    private SessionController sessionController;

    @Inject
    private PatientController patientController;

    // Filter fields
    private Date reportDate;

    // Results
    private List<PatientDepositBalanceDto> patientDepositBalanceDtos;

    // Totals
    private double totalBalance;

    public PatientSpecificDepositBalanceReportController() {
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
     * Generate the Patient-Specific Deposit Balance report.
     * Finds the last history record for each department for the selected patient
     * as of end of selected date, where balance > 0.
     */
    public void processPatientDepositBalanceReport() {
        if (patientController.getPatient() == null) {
            JsfUtil.addErrorMessage("Please select a patient first");
            return;
        }

        initializeDate();
        patientDepositBalanceDtos = fetchPatientSpecificDepositBalances();
        calculateTotals();

        if (patientDepositBalanceDtos == null || patientDepositBalanceDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No deposit balance found for the selected patient");
        }
    }

    /**
     * Fetch patient deposit balances for a specific patient as DTOs.
     * Strategy: Get the last PatientDepositHistory record for each department
     * for the selected patient where createdAt <= end of reportDate and balanceAfterTransaction > 0.
     */
    private List<PatientDepositBalanceDto> fetchPatientSpecificDepositBalances() {
        Date endOfDay = CommonFunctions.getEndOfDay(reportDate);

        // Build a subquery to get the MAX(id) for each department for this patient
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
        jpql.append("AND pdh.patientDeposit.patient = :patient ");

        // Subquery to get max ID for each department for this patient
        jpql.append("AND pdh.id = (");
        jpql.append("SELECT MAX(pdh2.id) FROM PatientDepositHistory pdh2 ");
        jpql.append("WHERE pdh2.retired = :ret ");
        jpql.append("AND pdh2.createdAt <= :endDate ");
        jpql.append("AND pdh2.patientDeposit.patient = :patient ");
        jpql.append("AND pdh2.department = pdh.department");
        jpql.append(") ");

        jpql.append("ORDER BY pdh.department.name");

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("endDate", endOfDay);
        m.put("patient", patientController.getPatient());

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
    public void clearPatientDepositBalanceReport() {
        reportDate = new Date();
        patientDepositBalanceDtos = null;
        totalBalance = 0.0;
    }

    /**
     * Navigate to the patient-specific report page.
     */
    public String navigateToPatientSpecificDepositBalanceReport() {
        clearPatientDepositBalanceReport();
        return "/patient_deposit/patient_specific_deposit_balance_report?faces-redirect=true";
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
