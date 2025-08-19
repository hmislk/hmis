package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * DTO for Daily Return Report Performance Optimization
 * 
 * This DTO replaces entity-based queries with optimized JPQL queries
 * for the most critical financial report in the HMIS system.
 * 
 * @author Dr M H B Ariyaratne
 * @author Kabi10 (Performance Optimization Implementation)
 */
public class DailyReturnDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Report Metadata
    private String reportName;
    private String reportDescription;
    private Date fromDate;
    private Date toDate;
    private String institutionName;
    private String siteName;
    private String departmentName;
    
    // Financial Totals
    private double collectionForTheDay;
    private double netCashCollection;
    private double netCollectionPlusCredits;
    
    // Collection Categories
    private List<DailyReturnItemDTO> opdServiceCollections;
    private List<DailyReturnItemDTO> pharmacyCollections;
    private List<DailyReturnItemDTO> collectingCentreCollections;
    private List<DailyReturnItemDTO> creditCompanyCollections;
    private List<DailyReturnItemDTO> patientDepositCollections;
    
    // Payment Categories
    private List<DailyReturnItemDTO> pettyCashPayments;
    private List<DailyReturnItemDTO> professionalPayments;
    private List<DailyReturnItemDTO> cardPayments;
    private List<DailyReturnItemDTO> staffPayments;
    private List<DailyReturnItemDTO> voucherPayments;
    private List<DailyReturnItemDTO> chequePayments;
    private List<DailyReturnItemDTO> ewalletPayments;
    private List<DailyReturnItemDTO> slipPayments;
    
    // Credit Collections
    private List<DailyReturnItemDTO> creditBills;
    private List<DailyReturnItemDTO> opdServiceCollectionCredit;
    
    // Constructors
    public DailyReturnDTO() {
        initializeLists();
    }
    
    public DailyReturnDTO(String reportName, Date fromDate, Date toDate) {
        this.reportName = reportName;
        this.fromDate = fromDate;
        this.toDate = toDate;
        initializeLists();
    }
    
    private void initializeLists() {
        this.opdServiceCollections = new ArrayList<>();
        this.pharmacyCollections = new ArrayList<>();
        this.collectingCentreCollections = new ArrayList<>();
        this.creditCompanyCollections = new ArrayList<>();
        this.patientDepositCollections = new ArrayList<>();
        this.pettyCashPayments = new ArrayList<>();
        this.professionalPayments = new ArrayList<>();
        this.cardPayments = new ArrayList<>();
        this.staffPayments = new ArrayList<>();
        this.voucherPayments = new ArrayList<>();
        this.chequePayments = new ArrayList<>();
        this.ewalletPayments = new ArrayList<>();
        this.slipPayments = new ArrayList<>();
        this.creditBills = new ArrayList<>();
        this.opdServiceCollectionCredit = new ArrayList<>();
    }
    
    // Calculation Methods
    public void calculateTotals() {
        // Calculate collection for the day
        collectionForTheDay = 0.0;
        collectionForTheDay += sumItemDTOs(opdServiceCollections);
        collectionForTheDay += sumItemDTOs(pharmacyCollections);
        collectionForTheDay += sumItemDTOs(collectingCentreCollections);
        collectionForTheDay += sumItemDTOs(creditCompanyCollections);
        collectionForTheDay += sumItemDTOs(patientDepositCollections);
        
        // Calculate net cash collection
        netCashCollection = collectionForTheDay;
        netCashCollection -= Math.abs(sumItemDTOs(pettyCashPayments));
        netCashCollection -= Math.abs(sumItemDTOs(professionalPayments));
        netCashCollection -= Math.abs(sumItemDTOs(cardPayments));
        netCashCollection -= Math.abs(sumItemDTOs(staffPayments));
        netCashCollection -= Math.abs(sumItemDTOs(voucherPayments));
        netCashCollection -= Math.abs(sumItemDTOs(chequePayments));
        netCashCollection -= Math.abs(sumItemDTOs(ewalletPayments));
        netCashCollection -= Math.abs(sumItemDTOs(slipPayments));
        
        // Calculate net collection plus credits
        netCollectionPlusCredits = netCashCollection + Math.abs(sumItemDTOs(opdServiceCollectionCredit));
    }
    
    private double sumItemDTOs(List<DailyReturnItemDTO> items) {
        return items.stream().mapToDouble(DailyReturnItemDTO::getTotal).sum();
    }
    
    // Getters and Setters
    public String getReportName() {
        return reportName;
    }
    
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }
    
    public String getReportDescription() {
        return reportDescription;
    }
    
    public void setReportDescription(String reportDescription) {
        this.reportDescription = reportDescription;
    }
    
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
    
    public String getInstitutionName() {
        return institutionName;
    }
    
    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }
    
    public String getSiteName() {
        return siteName;
    }
    
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
    
    public String getDepartmentName() {
        return departmentName;
    }
    
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
    
    public double getCollectionForTheDay() {
        return collectionForTheDay;
    }
    
    public void setCollectionForTheDay(double collectionForTheDay) {
        this.collectionForTheDay = collectionForTheDay;
    }
    
    public double getNetCashCollection() {
        return netCashCollection;
    }
    
    public void setNetCashCollection(double netCashCollection) {
        this.netCashCollection = netCashCollection;
    }
    
    public double getNetCollectionPlusCredits() {
        return netCollectionPlusCredits;
    }
    
    public void setNetCollectionPlusCredits(double netCollectionPlusCredits) {
        this.netCollectionPlusCredits = netCollectionPlusCredits;
    }
    
    public List<DailyReturnItemDTO> getOpdServiceCollections() {
        return opdServiceCollections;
    }
    
    public void setOpdServiceCollections(List<DailyReturnItemDTO> opdServiceCollections) {
        this.opdServiceCollections = opdServiceCollections;
    }
    
    public List<DailyReturnItemDTO> getPharmacyCollections() {
        return pharmacyCollections;
    }
    
    public void setPharmacyCollections(List<DailyReturnItemDTO> pharmacyCollections) {
        this.pharmacyCollections = pharmacyCollections;
    }
    
    public List<DailyReturnItemDTO> getCollectingCentreCollections() {
        return collectingCentreCollections;
    }
    
    public void setCollectingCentreCollections(List<DailyReturnItemDTO> collectingCentreCollections) {
        this.collectingCentreCollections = collectingCentreCollections;
    }
    
    public List<DailyReturnItemDTO> getCreditCompanyCollections() {
        return creditCompanyCollections;
    }
    
    public void setCreditCompanyCollections(List<DailyReturnItemDTO> creditCompanyCollections) {
        this.creditCompanyCollections = creditCompanyCollections;
    }
    
    public List<DailyReturnItemDTO> getPatientDepositCollections() {
        return patientDepositCollections;
    }
    
    public void setPatientDepositCollections(List<DailyReturnItemDTO> patientDepositCollections) {
        this.patientDepositCollections = patientDepositCollections;
    }
    
    public List<DailyReturnItemDTO> getPettyCashPayments() {
        return pettyCashPayments;
    }
    
    public void setPettyCashPayments(List<DailyReturnItemDTO> pettyCashPayments) {
        this.pettyCashPayments = pettyCashPayments;
    }
    
    public List<DailyReturnItemDTO> getProfessionalPayments() {
        return professionalPayments;
    }
    
    public void setProfessionalPayments(List<DailyReturnItemDTO> professionalPayments) {
        this.professionalPayments = professionalPayments;
    }
    
    public List<DailyReturnItemDTO> getCardPayments() {
        return cardPayments;
    }
    
    public void setCardPayments(List<DailyReturnItemDTO> cardPayments) {
        this.cardPayments = cardPayments;
    }
    
    public List<DailyReturnItemDTO> getStaffPayments() {
        return staffPayments;
    }
    
    public void setStaffPayments(List<DailyReturnItemDTO> staffPayments) {
        this.staffPayments = staffPayments;
    }
    
    public List<DailyReturnItemDTO> getVoucherPayments() {
        return voucherPayments;
    }
    
    public void setVoucherPayments(List<DailyReturnItemDTO> voucherPayments) {
        this.voucherPayments = voucherPayments;
    }
    
    public List<DailyReturnItemDTO> getChequePayments() {
        return chequePayments;
    }
    
    public void setChequePayments(List<DailyReturnItemDTO> chequePayments) {
        this.chequePayments = chequePayments;
    }
    
    public List<DailyReturnItemDTO> getEwalletPayments() {
        return ewalletPayments;
    }
    
    public void setEwalletPayments(List<DailyReturnItemDTO> ewalletPayments) {
        this.ewalletPayments = ewalletPayments;
    }
    
    public List<DailyReturnItemDTO> getSlipPayments() {
        return slipPayments;
    }
    
    public void setSlipPayments(List<DailyReturnItemDTO> slipPayments) {
        this.slipPayments = slipPayments;
    }
    
    public List<DailyReturnItemDTO> getCreditBills() {
        return creditBills;
    }
    
    public void setCreditBills(List<DailyReturnItemDTO> creditBills) {
        this.creditBills = creditBills;
    }
    
    public List<DailyReturnItemDTO> getOpdServiceCollectionCredit() {
        return opdServiceCollectionCredit;
    }
    
    public void setOpdServiceCollectionCredit(List<DailyReturnItemDTO> opdServiceCollectionCredit) {
        this.opdServiceCollectionCredit = opdServiceCollectionCredit;
    }
}
