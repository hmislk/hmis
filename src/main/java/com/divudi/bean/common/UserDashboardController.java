/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.dto.UserDashboardDto;
import com.divudi.core.data.dto.UserDepartmentDto;
import com.divudi.core.data.dto.UserLoginDto;
import com.divudi.core.data.dto.UserTransactionDto;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.LoginsFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * User Dashboard Controller
 * Displays comprehensive user information including personal details, staff info,
 * department access, login history, and transaction chronology
 *
 * @author Claude Code
 */
@Named(value = "userDashboardController")
@SessionScoped
public class UserDashboardController implements Serializable {

    // Dependencies
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private LoginsFacade loginsFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private PatientFacade patientFacade;
    @Inject
    private SessionController sessionController;

    // Dashboard data
    private Long selectedUserId;
    private UserDashboardDto userDetails;
    private List<UserDepartmentDto> assignedDepartments;
    private List<UserDepartmentDto> accessedDepartments;
    private List<UserDepartmentDto> allDepartments; // Merged and highlighted
    private List<UserLoginDto> recentLogins;
    private List<UserTransactionDto> transactions;

    // Transaction filters
    private Date transactionFromDate;
    private Date transactionToDate;

    // Login history limit
    private int recentLoginLimit = 15;

    /**
     * Initialize controller
     * Gets userId from Flash scope passed from user_logins page
     */
    @PostConstruct
    public void init() {
        // Initialization happens here, but data loading happens via prepareUserDashboard()
    }

    /**
     * Prepare dashboard for a specific user
     * Called from LoginController before navigation
     */
    public void prepareUserDashboard(Long userId) {
        this.selectedUserId = userId;
        try {
            loadDashboardData();
        } catch (Exception e) {
            // Log error and set userDetails to null so page shows error message
            System.err.println("Error loading dashboard data for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            this.userDetails = null;
        }
    }

    /**
     * Load all dashboard data
     */
    public void loadDashboardData() {
        if (selectedUserId == null) {
            return;
        }

        loadUserDetails();
        loadDepartments();
        loadRecentLogins();
        loadTransactions();
    }

    /**
     * Load user personal and staff details
     */
    private void loadUserDetails() {
        String jpql = "SELECT new com.divudi.core.data.dto.UserDashboardDto("
                + "u.id, u.name, u.code, u.email, u.telNo, u.role.name, "
                + "u.webUserPerson.name, u.institution.name, u.department.name, "
                + "u.createdAt, u.activatedAt, u.lastPasswordResetAt, u.activated, "
                + "s.id, s.code, s.registration, s.qualification, "
                + "s.designation.name, s.grade.name, s.staffCategory.name, "
                + "s.speciality.name, s.dateJoined, s.dateLeft, s.employeeStatus) "
                + "FROM WebUser u LEFT JOIN u.staff s "
                + "WHERE u.id = :userId";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", selectedUserId);

        @SuppressWarnings("unchecked")
        List<UserDashboardDto> results = (List<UserDashboardDto>) webUserFacade.findLightsByJpql(jpql, params);
        if (results != null && !results.isEmpty()) {
            userDetails = results.get(0);
        }
    }

    /**
     * Load and merge departments from privileges and access history
     */
    private void loadDepartments() {
        // Load assigned departments (from privileges)
        String jpqlPrivileges = "SELECT new com.divudi.core.data.dto.UserDepartmentDto("
                + "d.id, d.name, d.institution.name) "
                + "FROM WebUserPrivilege p JOIN p.department d "
                + "WHERE p.webUser.id = :userId AND p.retired = false "
                + "AND d.retired = false "
                + "ORDER BY d.name";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", selectedUserId);
        @SuppressWarnings("unchecked")
        List<UserDepartmentDto> assigned = (List<UserDepartmentDto>) webUserFacade.findLightsByJpql(jpqlPrivileges, params);
        assignedDepartments = assigned != null ? assigned : new ArrayList<>();

        // Load accessed departments (from login history)
        String jpqlAccessed = "SELECT new com.divudi.core.data.dto.UserDepartmentDto("
                + "d.id, d.name, d.institution.name, MAX(l.logedAt), COUNT(l.id)) "
                + "FROM Logins l JOIN l.department d "
                + "WHERE l.webUser.id = :userId "
                + "GROUP BY d.id, d.name, d.institution.name "
                + "ORDER BY MAX(l.logedAt) DESC";

        @SuppressWarnings("unchecked")
        List<UserDepartmentDto> accessed = (List<UserDepartmentDto>) loginsFacade.findLightsByJpql(jpqlAccessed, params);
        accessedDepartments = accessed != null ? accessed : new ArrayList<>();

        // Merge both lists and highlight accessed departments
        mergeDepartments();
    }

    /**
     * Merge assigned and accessed departments, highlighting accessed ones
     */
    private void mergeDepartments() {
        allDepartments = new ArrayList<>();
        Map<Long, UserDepartmentDto> deptMap = new HashMap<>();

        // Add all assigned departments
        for (UserDepartmentDto dto : assignedDepartments) {
            deptMap.put(dto.getDepartmentId(), dto);
            allDepartments.add(dto);
        }

        // Update with access information or add new departments
        for (UserDepartmentDto accessedDto : accessedDepartments) {
            Long deptId = accessedDto.getDepartmentId();

            if (deptMap.containsKey(deptId)) {
                // Department is assigned - update with access info
                UserDepartmentDto existing = deptMap.get(deptId);
                existing.setAccessed(true);
                existing.setLastAccessedAt(accessedDto.getLastAccessedAt());
                existing.setAccessCount(accessedDto.getAccessCount());
            } else {
                // Department accessed but not assigned via privilege
                accessedDto.setAssignedViaPrivilege(false);
                allDepartments.add(accessedDto);
            }
        }

        // Sort: accessed first, then by name
        allDepartments.sort((d1, d2) -> {
            if (d1.isAccessed() && !d2.isAccessed()) {
                return -1;
            }
            if (!d1.isAccessed() && d2.isAccessed()) {
                return 1;
            }
            return d1.getDepartmentName().compareTo(d2.getDepartmentName());
        });
    }

    /**
     * Load recent login history
     */
    private void loadRecentLogins() {
        String jpql = "SELECT new com.divudi.core.data.dto.UserLoginDto("
                + "l.id, l.webUser.id, l.webUser.name, l.webUser.code, "
                + "l.webUser.webUserPerson.name, l.institution.name, "
                + "l.department.name, l.logedAt, l.logoutAt, l.ipaddress, "
                + "l.browser, l.operatingSystem, l.screenResolution, l.computerName) "
                + "FROM Logins l "
                + "WHERE l.webUser.id = :userId "
                + "ORDER BY l.logedAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", selectedUserId);

        @SuppressWarnings("unchecked")
        List<UserLoginDto> logins = (List<UserLoginDto>) loginsFacade.findLightsByJpql(jpql, params, null, recentLoginLimit);
        recentLogins = logins != null ? logins : new ArrayList<>();
    }

    /**
     * Load transactions with date range filter
     */
    public void loadTransactions() {
        if (selectedUserId == null) {
            return;
        }

        transactions = new ArrayList<>();

        // Fetch bills
        List<UserTransactionDto> billTransactions = fetchBillTransactions();
        if (billTransactions != null) {
            transactions.addAll(billTransactions);
        }

        // Fetch payments
        List<UserTransactionDto> paymentTransactions = fetchPaymentTransactions();
        if (paymentTransactions != null) {
            transactions.addAll(paymentTransactions);
        }

        // Fetch patient registrations
        List<UserTransactionDto> patientTransactions = fetchPatientRegistrations();
        if (patientTransactions != null) {
            transactions.addAll(patientTransactions);
        }

        // Sort chronologically (most recent first)
        transactions.sort((t1, t2)
                -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));
    }

    /**
     * Fetch bill transactions
     */
    private List<UserTransactionDto> fetchBillTransactions() {
        String jpql = "SELECT new com.divudi.core.data.dto.UserTransactionDto("
                + "'BILL', b.id, b.deptId, b.createdAt, "
                + "b.patient.person.name, b.billTypeAtomic, b.netTotal, "
                + "b.paymentMethod, b.cancelled, b.refunded) "
                + "FROM Bill b "
                + "WHERE b.creater.id = :userId "
                + "AND b.createdAt BETWEEN :fromDate AND :toDate "
                + "ORDER BY b.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", selectedUserId);
        params.put("fromDate", getTransactionFromDate());
        params.put("toDate", getTransactionToDate());

        @SuppressWarnings("unchecked")
        List<UserTransactionDto> bills = (List<UserTransactionDto>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        return bills != null ? bills : new ArrayList<>();
    }

    /**
     * Fetch payment transactions
     */
    private List<UserTransactionDto> fetchPaymentTransactions() {
        String jpql = "SELECT new com.divudi.core.data.dto.UserTransactionDto("
                + "'PAYMENT', p.id, p.referenceNo, p.paymentDate, "
                + "p.bill.patient.person.name, p.paymentMethod, "
                + "p.paidValue, p.retired) "
                + "FROM Payment p "
                + "WHERE p.creater.id = :userId "
                + "AND p.paymentDate BETWEEN :fromDate AND :toDate "
                + "ORDER BY p.paymentDate DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", selectedUserId);
        params.put("fromDate", getTransactionFromDate());
        params.put("toDate", getTransactionToDate());

        @SuppressWarnings("unchecked")
        List<UserTransactionDto> payments = (List<UserTransactionDto>) paymentFacade.findLightsByJpql(jpql, params, TemporalType.DATE);
        return payments != null ? payments : new ArrayList<>();
    }

    /**
     * Fetch patient registrations
     */
    private List<UserTransactionDto> fetchPatientRegistrations() {
        String jpql = "SELECT new com.divudi.core.data.dto.UserTransactionDto("
                + "'PATIENT_REG', p.id, p.code, p.createdAt, p.person.name) "
                + "FROM Patient p "
                + "WHERE p.creater.id = :userId "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "ORDER BY p.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", selectedUserId);
        params.put("fromDate", getTransactionFromDate());
        params.put("toDate", getTransactionToDate());

        @SuppressWarnings("unchecked")
        List<UserTransactionDto> patients = (List<UserTransactionDto>) patientFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        return patients != null ? patients : new ArrayList<>();
    }

    /**
     * Reset transaction filters to default (last 30 days)
     */
    public void resetTransactionFilters() {
        // Set to 30 days ago
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
        transactionFromDate = CommonFunctions.getStartOfDay(cal.getTime());
        transactionToDate = CommonFunctions.getEndOfDay();
        loadTransactions();
    }

    // Getters with defaults

    public Date getTransactionFromDate() {
        if (transactionFromDate == null) {
            // Set to 30 days ago
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
            transactionFromDate = CommonFunctions.getStartOfDay(cal.getTime());
        }
        return transactionFromDate;
    }

    public void setTransactionFromDate(Date transactionFromDate) {
        this.transactionFromDate = transactionFromDate;
    }

    public Date getTransactionToDate() {
        if (transactionToDate == null) {
            transactionToDate = CommonFunctions.getEndOfDay();
        }
        return transactionToDate;
    }

    public void setTransactionToDate(Date transactionToDate) {
        this.transactionToDate = transactionToDate;
    }

    public Long getSelectedUserId() {
        return selectedUserId;
    }

    public void setSelectedUserId(Long selectedUserId) {
        this.selectedUserId = selectedUserId;
    }

    public UserDashboardDto getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserDashboardDto userDetails) {
        this.userDetails = userDetails;
    }

    public List<UserDepartmentDto> getAssignedDepartments() {
        return assignedDepartments;
    }

    public void setAssignedDepartments(List<UserDepartmentDto> assignedDepartments) {
        this.assignedDepartments = assignedDepartments;
    }

    public List<UserDepartmentDto> getAccessedDepartments() {
        return accessedDepartments;
    }

    public void setAccessedDepartments(List<UserDepartmentDto> accessedDepartments) {
        this.accessedDepartments = accessedDepartments;
    }

    public List<UserDepartmentDto> getAllDepartments() {
        return allDepartments;
    }

    public void setAllDepartments(List<UserDepartmentDto> allDepartments) {
        this.allDepartments = allDepartments;
    }

    public List<UserLoginDto> getRecentLogins() {
        return recentLogins;
    }

    public void setRecentLogins(List<UserLoginDto> recentLogins) {
        this.recentLogins = recentLogins;
    }

    public List<UserTransactionDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<UserTransactionDto> transactions) {
        this.transactions = transactions;
    }

    public int getRecentLoginLimit() {
        return recentLoginLimit;
    }

    public void setRecentLoginLimit(int recentLoginLimit) {
        this.recentLoginLimit = recentLoginLimit;
    }
}
