package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PaymentType;
import com.divudi.core.data.ServiceType;
import com.divudi.core.data.dto.DailyReturnDTO;
import com.divudi.core.data.dto.DailyReturnItemDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Service for Daily Return DTO-based Performance Optimization
 * 
 * This service implements optimized JPQL queries using DTOs instead of
 * entity-based queries for the most critical financial report in HMIS.
 * 
 * Performance Benefits:
 * - Direct JPQL queries with minimal data transfer
 * - No entity loading overhead
 * - Optimized memory usage
 * - Faster query execution
 * 
 * @author Dr M H B Ariyaratne
 * @author Kabi10 (Performance Optimization Implementation)
 */
@Stateless
public class DailyReturnDtoService {
    
    @EJB
    private BillFacade billFacade;
    
    @EJB
    private BillItemFacade billItemFacade;
    
    @EJB
    private PaymentFacade paymentFacade;
    
    /**
     * Type-safe cast helper method to handle facade results.
     * Addresses compilation error with generic List<?> return types from facade methods.
     */
    @SuppressWarnings("unchecked")
    private List<DailyReturnItemDTO> castDtoList(List<?> list) {
        return (List<DailyReturnItemDTO>) list;
    }
    
    /**
     * Generate complete Daily Return report using DTO-based queries
     * 
     * @param fromDate Report start date
     * @param toDate Report end date
     * @param institution Filter by institution (null for all)
     * @param site Filter by site (null for all)
     * @param department Filter by department (null for all)
     * @param withProfessionalFee Include professional fees in calculations
     * @return Optimized DailyReturnDTO with all financial data
     */
    public DailyReturnDTO generateDailyReturnDto(Date fromDate, Date toDate, 
                                               Institution institution, Institution site, 
                                               Department department, boolean withProfessionalFee) {
        
        DailyReturnDTO dailyReturn = new DailyReturnDTO("Daily Return", fromDate, toDate);
        
        // Set report metadata
        setupReportMetadata(dailyReturn, institution, site, department);
        
        // Generate collections
        dailyReturn.setOpdServiceCollections(generateOpdServiceCollectionDto(fromDate, toDate, institution, site, department, withProfessionalFee));
        dailyReturn.setPharmacyCollections(generatePharmacyCollectionDto(fromDate, toDate, institution, site, department));
        dailyReturn.setCollectingCentreCollections(generateCcCollectionDto(fromDate, toDate, institution, site, department));
        dailyReturn.setCreditCompanyCollections(generateCreditCompanyCollectionDto(fromDate, toDate, institution, site, department));
        dailyReturn.setPatientDepositCollections(generatePatientDepositCollectionDto(fromDate, toDate, institution, site, department));
        
        // Generate payments
        dailyReturn.setPettyCashPayments(generatePettyCashPaymentsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setProfessionalPayments(generateProfessionalPaymentsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setCardPayments(generateCardPaymentsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setStaffPayments(generateStaffPaymentsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setVoucherPayments(generateVoucherPaymentsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setChequePayments(generateChequePaymentsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setEwalletPayments(generateEwalletPaymentsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setSlipPayments(generateSlipPaymentsDto(fromDate, toDate, institution, site, department));
        
        // Generate credit collections
        dailyReturn.setCreditBills(generateCreditBillsDto(fromDate, toDate, institution, site, department));
        dailyReturn.setOpdServiceCollectionCredit(generateOpdServiceCollectionCreditDto(fromDate, toDate, institution, site, department));
        
        // Calculate totals
        dailyReturn.calculateTotals();
        
        return dailyReturn;
    }
    
    private void setupReportMetadata(DailyReturnDTO dailyReturn, Institution institution, Institution site, Department department) {
        String institutionName = institution != null ? institution.getName() : "All Institutions";
        String siteName = site != null ? site.getName() : "All Sites";
        String departmentName = department != null ? department.getName() : "All Departments";
        
        dailyReturn.setInstitutionName(institutionName);
        dailyReturn.setSiteName(siteName);
        dailyReturn.setDepartmentName(departmentName);
        
        String dateTimeFormat = "dd/MM/yyyy HH:mm:ss"; // Default format
        String formattedFromDate = dailyReturn.getFromDate() != null ? new SimpleDateFormat(dateTimeFormat).format(dailyReturn.getFromDate()) : "Not available";
        String formattedToDate = dailyReturn.getToDate() != null ? new SimpleDateFormat(dateTimeFormat).format(dailyReturn.getToDate()) : "Not available";
        
        String description = String.format("Report for %s to %s covering %s, %s, %s",
                formattedFromDate, formattedToDate, institutionName, siteName, departmentName);
        
        dailyReturn.setReportDescription(description);
    }
    
    /**
     * Generate OPD Service Collection using optimized DTO query
     */
    public List<DailyReturnItemDTO> generateOpdServiceCollectionDto(Date fromDate, Date toDate, 
                                                                   Institution institution, Institution site, 
                                                                   Department department, boolean withProfessionalFee) {
        
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "bi.id, bi.item.name, bi.item.code, bi.item.category.name, "
                + "bi.bill.department.name, bi.bill.institution.name, bi.bill.id, "
                + "bi.bill.billNumber, bi.bill.billTypeAtomic, bi.bill.createdAt, "
                + "bi.qty, bi.rate, bi.grossValue, bi.discount, bi.netValue, "
                + "bi.hospitalFee, bi.professionalFee, bi.netValue) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.retired = :retired "
                + "AND bi.retired = :itemRetired "
                + "AND bi.bill.createdAt BETWEEN :fromDate AND :toDate ";
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("itemRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        
        // Add OPD bill types
        List<BillTypeAtomic> opdBillTypes = BillTypeAtomic.findByServiceType(ServiceType.OPD);
        if (!opdBillTypes.isEmpty()) {
            jpql += "AND bi.bill.billTypeAtomic IN :billTypes ";
            parameters.put("billTypes", opdBillTypes);
        }
        
        // Add filters
        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");
        
        // Add payment type filter for non-credit
        jpql += "AND (bi.bill.paymentScheme IS NULL OR bi.bill.paymentScheme.paymentType != :creditPaymentType) ";
        parameters.put("creditPaymentType", PaymentType.CREDIT);
        
        jpql += "ORDER BY bi.bill.department.name, bi.item.category.name, bi.item.name";
        
        return castDtoList(billItemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }
    
    /**
     * Generate Pharmacy Collection using optimized DTO query
     */
    public List<DailyReturnItemDTO> generatePharmacyCollectionDto(Date fromDate, Date toDate, 
                                                                 Institution institution, Institution site, 
                                                                 Department department) {
        
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "bi.id, bi.item.name, bi.item.code, bi.item.category.name, "
                + "bi.bill.department.name, bi.bill.institution.name, bi.bill.id, "
                + "bi.bill.billNumber, bi.bill.billTypeAtomic, bi.bill.createdAt, "
                + "bi.qty, bi.rate, bi.grossValue, bi.discount, bi.netValue, "
                + "0.0, 0.0, bi.netValue) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.retired = :retired "
                + "AND bi.retired = :itemRetired "
                + "AND bi.bill.createdAt BETWEEN :fromDate AND :toDate ";
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("itemRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        
        // Add Pharmacy bill types
        List<BillTypeAtomic> pharmacyBillTypes = BillTypeAtomic.findByServiceType(ServiceType.PHARMACY);
        if (!pharmacyBillTypes.isEmpty()) {
            jpql += "AND bi.bill.billTypeAtomic IN :billTypes ";
            parameters.put("billTypes", pharmacyBillTypes);
        }
        
        // Add filters
        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");
        
        jpql += "ORDER BY bi.bill.department.name, bi.item.name";
        
        return castDtoList(billItemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }
    
    /**
     * Generate Collecting Centre Collection using optimized DTO query
     */
    public List<DailyReturnItemDTO> generateCcCollectionDto(Date fromDate, Date toDate, 
                                                           Institution institution, Institution site, 
                                                           Department department) {
        
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "bi.id, bi.item.name, bi.item.code, bi.item.category.name, "
                + "bi.bill.department.name, bi.bill.institution.name, bi.bill.id, "
                + "bi.bill.billNumber, bi.bill.billTypeAtomic, bi.bill.createdAt, "
                + "bi.qty, bi.rate, bi.grossValue, bi.discount, bi.netValue, "
                + "bi.hospitalFee, bi.professionalFee, bi.netValue) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.retired = :retired "
                + "AND bi.retired = :itemRetired "
                + "AND bi.bill.createdAt BETWEEN :fromDate AND :toDate ";
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("itemRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        
        // Add Collecting Centre bill types
        List<BillTypeAtomic> ccBillTypes = BillTypeAtomic.findByServiceType(ServiceType.COLLECTING_CENTRE);
        if (!ccBillTypes.isEmpty()) {
            jpql += "AND bi.bill.billTypeAtomic IN :billTypes ";
            parameters.put("billTypes", ccBillTypes);
        }
        
        // Add filters
        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");
        
        jpql += "ORDER BY bi.bill.department.name, bi.item.name";
        
        return castDtoList(billItemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }
    
    /**
     * Generate Credit Company Collection using optimized DTO query
     */
    public List<DailyReturnItemDTO> generateCreditCompanyCollectionDto(Date fromDate, Date toDate, 
                                                                      Institution institution, Institution site, 
                                                                      Department department) {
        
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :creditPaymentMethod ";
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("creditPaymentMethod", PaymentMethod.Credit);
        
        // Add filters
        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "p");
        jpql = addSiteFilter(jpql, parameters, site, "site", "p");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "p");
        
        jpql += "ORDER BY p.bill.department.name, p.createdAt";
        
        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }
    
    // Additional methods for other collections and payments would follow the same pattern
    // Due to space constraints, I'll implement the key ones and add placeholders for others
    
    public List<DailyReturnItemDTO> generatePatientDepositCollectionDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :patientDepositPaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("patientDepositPaymentMethod", PaymentMethod.PatientDeposit);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generatePettyCashPaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :cashPaymentMethod "
                + "AND p.paidValue < 0 "; // Negative values for payments out

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("cashPaymentMethod", PaymentMethod.Cash);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateProfessionalPaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :staffPaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("staffPaymentMethod", PaymentMethod.Staff);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateCardPaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :cardPaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("cardPaymentMethod", PaymentMethod.Card);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateStaffPaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :staffWelfarePaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("staffWelfarePaymentMethod", PaymentMethod.Staff_Welfare);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateVoucherPaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :voucherPaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("voucherPaymentMethod", PaymentMethod.Voucher);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateChequePaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :chequePaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("chequePaymentMethod", PaymentMethod.Cheque);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateEwalletPaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :ewalletPaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("ewalletPaymentMethod", PaymentMethod.ewallet);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateSlipPaymentsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "p.id, p.paymentMethod, p.paidValue, p.creater.webUserPerson.name, "
                + "p.createdAt, p.bill.department.name) "
                + "FROM Payment p "
                + "WHERE p.retired = :retired "
                + "AND p.bill.retired = :billRetired "
                + "AND p.createdAt BETWEEN :fromDate AND :toDate "
                + "AND p.paymentMethod = :slipPaymentMethod ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("billRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("slipPaymentMethod", PaymentMethod.Slip);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY p.bill.department.name, p.createdAt";

        return castDtoList(paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateCreditBillsDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "bi.id, bi.item.name, bi.item.code, bi.item.category.name, "
                + "bi.bill.department.name, bi.bill.institution.name, bi.bill.id, "
                + "bi.bill.billNumber, bi.bill.billTypeAtomic, bi.bill.createdAt, "
                + "bi.qty, bi.rate, bi.grossValue, bi.discount, bi.netValue, "
                + "bi.hospitalFee, bi.professionalFee, bi.netValue) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.retired = :retired "
                + "AND bi.retired = :itemRetired "
                + "AND bi.bill.createdAt BETWEEN :fromDate AND :toDate "
                + "AND bi.bill.paymentScheme IS NOT NULL "
                + "AND bi.bill.paymentScheme.paymentType = :creditPaymentType ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("itemRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("creditPaymentType", PaymentType.CREDIT);

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY bi.bill.department.name, bi.item.category.name, bi.item.name";

        return castDtoList(billItemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    public List<DailyReturnItemDTO> generateOpdServiceCollectionCreditDto(Date fromDate, Date toDate, Institution institution, Institution site, Department department) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.DailyReturnItemDTO("
                + "bi.id, bi.item.name, bi.item.code, bi.item.category.name, "
                + "bi.bill.department.name, bi.bill.institution.name, bi.bill.id, "
                + "bi.bill.billNumber, bi.bill.billTypeAtomic, bi.bill.createdAt, "
                + "bi.qty, bi.rate, bi.grossValue, bi.discount, bi.netValue, "
                + "bi.hospitalFee, bi.professionalFee, bi.netValue) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.retired = :retired "
                + "AND bi.retired = :itemRetired "
                + "AND bi.bill.createdAt BETWEEN :fromDate AND :toDate "
                + "AND bi.bill.paymentScheme IS NOT NULL "
                + "AND bi.bill.paymentScheme.paymentType = :creditPaymentType ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("retired", false);
        parameters.put("itemRetired", false);
        parameters.put("fromDate", fromDate);
        parameters.put("toDate", toDate);
        parameters.put("creditPaymentType", PaymentType.CREDIT);

        // Add OPD bill types
        List<BillTypeAtomic> opdBillTypes = BillTypeAtomic.findByServiceType(ServiceType.OPD);
        if (!opdBillTypes.isEmpty()) {
            jpql += "AND bi.bill.billTypeAtomic IN :billTypes ";
            parameters.put("billTypes", opdBillTypes);
        }

        jpql = addInstitutionFilter(jpql, parameters, institution, "institution", "bi");
        jpql = addSiteFilter(jpql, parameters, site, "site", "bi");
        jpql = addDepartmentFilter(jpql, parameters, department, "department", "bi");

        jpql += "ORDER BY bi.bill.department.name, bi.item.category.name, bi.item.name";

        return castDtoList(billItemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP));
    }

    // Utility methods for adding filters - Fixed to accept alias parameter
    private String addInstitutionFilter(String jpql, Map<String, Object> parameters, Institution institution, String paramName, String alias) {
        if (institution != null) {
            jpql += "AND " + alias + ".bill.institution = :" + paramName + " ";
            parameters.put(paramName, institution);
        }
        return jpql;
    }

    private String addSiteFilter(String jpql, Map<String, Object> parameters, Institution site, String paramName, String alias) {
        if (site != null) {
            jpql += "AND " + alias + ".bill.site = :" + paramName + " ";
            parameters.put(paramName, site);
        }
        return jpql;
    }

    private String addDepartmentFilter(String jpql, Map<String, Object> parameters, Department department, String paramName, String alias) {
        if (department != null) {
            jpql += "AND " + alias + ".bill.department = :" + paramName + " ";
            parameters.put(paramName, department);
        }
        return jpql;
    }
}
