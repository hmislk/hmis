package com.divudi.bean.report;


import com.divudi.bean.common.ControllerWithReportFilters;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.data.dto.PatientDepositHistoryDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
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
 * Controller for Patient Deposit History Reports.
 * Provides filtering by institution, site, department, and date range.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PatientDepositHistoryReportController implements Serializable, ControllerWithReportFilters {

    @EJB
    private PatientDepositHistoryFacade patientDepositHistoryFacade;

    @Inject
    private SessionController sessionController;

    // Filter fields
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;
    private ReportViewType reportViewType;

    // Results
    private List<PatientDepositHistoryDto> patientDepositHistoryDtos;

    // Totals
    private double totalCredit;
    private double totalDebit;
    private double netTotal;

    public PatientDepositHistoryReportController() {
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
     * Generate the Patient Deposit History report using DTOs.
     */
    public void generateReport() {
        initializeDates();
        patientDepositHistoryDtos = fetchPatientDepositHistoryDtos();
        calculateTotals();
        if (patientDepositHistoryDtos == null || patientDepositHistoryDtos.isEmpty()) {
            JsfUtil.addErrorMessage("No data found for the selected criteria");
        }
    }

    /**
     * Fetch patient deposit histories as DTOs with filters applied.
     * Includes aggregate balance fields for contextual display.
     */
    private List<PatientDepositHistoryDto> fetchPatientDepositHistoryDtos() {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT NEW com.divudi.core.data.dto.PatientDepositHistoryDto(");
        jpql.append("pdh.id, ");
        jpql.append("pdh.bill.id, ");
        jpql.append("pdh.bill.deptId, ");
        jpql.append("pdh.bill.billTypeAtomic, ");
        jpql.append("pdh.createdAt, ");
        jpql.append("pdh.historyType, ");
        jpql.append("pdh.patientDeposit.patient.id, ");
        jpql.append("pdh.patientDeposit.patient.person.name, ");
        jpql.append("pdh.patientDeposit.patient.phn, ");
        jpql.append("pdh.balanceBeforeTransaction, ");
        jpql.append("pdh.balanceAfterTransaction, ");
        jpql.append("pdh.transactionValue, ");
        jpql.append("pdh.department.id, ");
        jpql.append("pdh.department.name, ");
        jpql.append("pdh.site.id, ");
        jpql.append("pdh.site.name, ");
        jpql.append("pdh.institution.id, ");
        jpql.append("pdh.institution.name, ");
        // Aggregate balances for this patient
        jpql.append("pdh.patientDepositBalanceForSite, ");
        jpql.append("pdh.patientDepositBalanceForInstitution, ");
        jpql.append("pdh.patientDepositBalanceForAllInstitutions, ");
        // Aggregate balances for all patients
        jpql.append("pdh.allPatientsDepositBalanceForDepartment, ");
        jpql.append("pdh.allPatientsDepositBalanceForSite, ");
        jpql.append("pdh.allPatientsDepositBalanceForInstitution, ");
        jpql.append("pdh.allPatientsDepositBalanceForAllInstitutions, ");
        // Bill status and creator
        jpql.append("pdh.bill.cancelled, ");
        jpql.append("pdh.bill.refunded, ");
        jpql.append("pdh.creater.name");
        jpql.append(") ");
        jpql.append("FROM PatientDepositHistory pdh ");
        jpql.append("WHERE pdh.retired = :ret ");
        jpql.append("AND pdh.createdAt BETWEEN :fromDate AND :toDate ");

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);

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
     * Get contextual patient balance based on filter selection.
     * Department → balanceAfterTransaction (department level)
     * Site (no dept) → patientDepositBalanceForSite
     * Institution (no site/dept) → patientDepositBalanceForInstitution
     * Nothing → patientDepositBalanceForAllInstitutions
     */
    public Double getContextualPatientBalance(PatientDepositHistoryDto dto) {
        if (dto == null) {
            return 0.0;
        }
        if (department != null) {
            return dto.getBalanceAfterTransaction();
        } else if (site != null) {
            return dto.getPatientDepositBalanceForSite();
        } else if (institution != null) {
            return dto.getPatientDepositBalanceForInstitution();
        } else {
            return dto.getPatientDepositBalanceForAllInstitutions();
        }
    }

    /**
     * Get contextual all-patients balance based on filter selection.
     * Department → allPatientsDepositBalanceForDepartment
     * Site (no dept) → allPatientsDepositBalanceForSite
     * Institution (no site/dept) → allPatientsDepositBalanceForInstitution
     * Nothing → allPatientsDepositBalanceForAllInstitutions
     */
    public Double getContextualAllPatientsBalance(PatientDepositHistoryDto dto) {
        if (dto == null) {
            return 0.0;
        }
        if (department != null) {
            return dto.getAllPatientsDepositBalanceForDepartment();
        } else if (site != null) {
            return dto.getAllPatientsDepositBalanceForSite();
        } else if (institution != null) {
            return dto.getAllPatientsDepositBalanceForInstitution();
        } else {
            return dto.getAllPatientsDepositBalanceForAllInstitutions();
        }
    }

    /**
     * Get the label for the contextual balance column header.
     */
    public String getContextualBalanceLabel() {
        if (department != null) {
            return "Department Balance";
        } else if (site != null) {
            return "Site Balance";
        } else if (institution != null) {
            return "Institution Balance";
        } else {
            return "Global Balance";
        }
    }

    /**
     * Calculate totals from the DTO list.
     */
    private void calculateTotals() {
        totalCredit = 0.0;
        totalDebit = 0.0;

        if (patientDepositHistoryDtos != null) {
            for (PatientDepositHistoryDto dto : patientDepositHistoryDtos) {
                if (dto.getTransactionValue() != null) {
                    if (dto.getTransactionValue() > 0) {
                        totalCredit += dto.getTransactionValue();
                    } else {
                        totalDebit += dto.getTransactionValue();
                    }
                }
            }
        }
        netTotal = totalCredit + totalDebit;
    }

    /**
     * Clear all filters and results.
     */
    public void clearReport() {
        fromDate = CommonFunctions.getStartOfDay(new Date());
        toDate = CommonFunctions.getEndOfDay(new Date());
        institution = null;
        site = null;
        department = null;
        patientDepositHistoryDtos = null;
        totalCredit = 0.0;
        totalDebit = 0.0;
        netTotal = 0.0;
    }

    /**
     * Navigate to the report page.
     */
    public String navigateToPatientDepositHistoriesReport() {
        clearReport();
        return "/patient_deposit/patient_deposit_histories_report?faces-redirect=true";
    }

    // Getters and Setters

    @Override
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    @Override
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    @Override
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @Override
    public Institution getInstitution() {
        return institution;
    }

    @Override
    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @Override
    public Institution getSite() {
        return site;
    }

    @Override
    public void setSite(Institution site) {
        this.site = site;
    }

    @Override
    public Department getDepartment() {
        return department;
    }

    @Override
    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    @Override
    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Override
    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    @Override
    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    @Override
    public ReportViewType getReportViewType() {
        return reportViewType;
    }

    @Override
    public void setReportViewType(ReportViewType reportViewType) {
        this.reportViewType = reportViewType;
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

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }
}
