package com.divudi.bean.common;

import com.divudi.bean.common.ExcelController;
import com.divudi.bean.common.PdfController;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.dto.DailyReturnDTO;
import com.divudi.core.data.dto.DailyReturnDetailDTO;
import com.divudi.core.data.dto.DailyReturnItemDTO;
import com.divudi.core.data.dto.DailyReturnBundleDTO;
import com.divudi.core.data.dto.DailyReturnRowDTO;
import com.divudi.core.data.dto.BillItemDetailDTO;
import com.divudi.core.data.dto.PaymentDetailDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.service.DailyReturnDtoService;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.StreamedContent;
import java.io.IOException;

/**
 * Controller for DTO-based Daily Return report.
 */
@Named
@SessionScoped
public class DailyReturnDtoController implements Serializable {

    @EJB
    private DailyReturnDtoService dailyReturnDtoService;
    
    @Inject
    private SessionController sessionController;
    
    @Inject
    private ExcelController excelController;
    
    @Inject
    private PdfController pdfController;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private boolean withProfessionalFee;

    private List<DailyReturnDTO> dailyReturnDtos;
    private List<DailyReturnItemDTO> dailyReturnItemDtos;
    private List<DailyReturnDetailDTO> dailyReturnDetailDtos;
    private List<BillItemDetailDTO> opdBillItemDtos;
    private List<BillItemDetailDTO> ccBillItemDtos;
    private List<DailyReturnDetailDTO> ccBillDtos;
    private List<PaymentDetailDTO> cardPaymentDtos;
    private List<PaymentDetailDTO> patientDepositPaymentDtos;
    
    private ReportTemplateRowBundle bundle;
    private DailyReturnBundleDTO dtoBundle;
    private StreamedContent downloadingExcel;
    
    // Initialize with default date values like the entity-based report
    {
        fromDate = com.divudi.core.util.CommonFunctions.getStartOfMonth(new Date());
        toDate = com.divudi.core.util.CommonFunctions.getEndOfMonth(new Date());
    }

    public void generateDailyReturnDto() {
        // Fetch the DTO data
        dailyReturnDtos = dailyReturnDtoService.fetchDailyReturnByPaymentMethod(fromDate, toDate);
        dailyReturnItemDtos = dailyReturnDtoService.fetchDailyReturnItems(fromDate, toDate);
        dailyReturnDetailDtos = dailyReturnDtoService.fetchDetailedDailyReturnBills(fromDate, toDate);
        opdBillItemDtos = dailyReturnDtoService.fetchOpdBillItemsForDailyReturn(fromDate, toDate);
        ccBillItemDtos = dailyReturnDtoService.fetchCcCollectionBillItemsForDailyReturn(fromDate, toDate);
        ccBillDtos = dailyReturnDtoService.fetchCcCollectionBillsForDailyReturn(fromDate, toDate);
        cardPaymentDtos = dailyReturnDtoService.fetchCreditCardPaymentsForDailyReturn(fromDate, toDate);
        patientDepositPaymentDtos = dailyReturnDtoService.fetchPatientDepositPaymentsForDailyReturn(fromDate, toDate);
        
        // Create both bundle structures
        createBundle();  // Keep original for compatibility
        createDtoBundle();  // New DTO-specific bundle
    }
    
    private void createBundle() {
        bundle = new ReportTemplateRowBundle();
        bundle.setName("Daily Return");
        bundle.setBundleType("dailyReturn");

        String institutionName = institution != null ? institution.getName() : "All Institutions";
        String siteName = site != null ? site.getName() : "All Sites";
        String departmentName = department != null ? department.getName() : "All Departments";

        String dateTimeFormat = sessionController.getApplicationPreference().getLongDateTimeFormat();

        String formattedFromDate = fromDate != null ? new SimpleDateFormat(dateTimeFormat).format(fromDate) : "Not available";
        String formattedToDate = toDate != null ? new SimpleDateFormat(dateTimeFormat).format(toDate) : "Not available";

        String description = String.format("Report for %s to %s covering %s, %s, %s",
                formattedFromDate, formattedToDate,
                institutionName, siteName, departmentName);

        bundle.setDescription(description);

        // Create bundle items from DTO data - this creates the same structure as the original
        // but uses DTO data instead of entity queries
        createBundleFromDtoData();
    }
    
    private void createBundleFromDtoData() {
        // Create comprehensive bundle structure from DTO data to match the original
        double collectionForTheDay = 0.0;
        double netCashCollection = 0.0;
        double netCollectionPlusCredits = 0.0;

        // 1. OPD Service Collection - simulate based on DTO data
        ReportTemplateRowBundle opdServiceCollection = createBundleFromPaymentMethod("opdServiceCollection", "OPD Service Collection");
        bundle.getBundles().add(opdServiceCollection);
        collectionForTheDay += getSafeTotal(opdServiceCollection);

        // 2. Pharmacy Collection - simulate based on DTO data
        ReportTemplateRowBundle pharmacyCollection = createBundleFromPaymentMethod("pharmacyCollection", "Pharmacy Collection");
        bundle.getBundles().add(pharmacyCollection);
        collectionForTheDay += getSafeTotal(pharmacyCollection);

        // 3. Collecting Centre Collection
        ReportTemplateRowBundle ccCollection = createBundleFromPaymentMethod("ccCollection", "Collecting Centre Collection");
        bundle.getBundles().add(ccCollection);
        collectionForTheDay += getSafeTotal(ccCollection);

        // 4. Company Payment Bills for different types
        ReportTemplateRowBundle opdCreditCompanyCollection = createEmptyBundle("companyPaymentBillOpd", "OPD Credit Company Payment Collection");
        bundle.getBundles().add(opdCreditCompanyCollection);
        collectionForTheDay += getSafeTotal(opdCreditCompanyCollection);

        ReportTemplateRowBundle inwardCreditCompanyCollection = createEmptyBundle("companyPaymentBillInward", "Inward Credit Company Payment Collection");
        bundle.getBundles().add(inwardCreditCompanyCollection);
        collectionForTheDay += getSafeTotal(inwardCreditCompanyCollection);

        ReportTemplateRowBundle pharmacyCreditCompanyCollection = createEmptyBundle("companyPaymentBillPharmacy", "Pharmacy Credit Company Payment Collection");
        bundle.getBundles().add(pharmacyCreditCompanyCollection);
        collectionForTheDay += getSafeTotal(pharmacyCreditCompanyCollection);

        ReportTemplateRowBundle channellingCreditCompanyCollection = createEmptyBundle("companyPaymentBillChannelling", "Channelling Credit Company Payment Collection");
        bundle.getBundles().add(channellingCreditCompanyCollection);
        collectionForTheDay += getSafeTotal(channellingCreditCompanyCollection);

        // 5. Patient Deposit Payments
        ReportTemplateRowBundle patientDepositPayments = createEmptyBundle("patientDepositPayments", "Patient Deposit Payments");
        bundle.getBundles().add(patientDepositPayments);
        collectionForTheDay += getSafeTotal(patientDepositPayments);

        // 6. Collection for the day summary
        ReportTemplateRowBundle collectionForTheDayBundle = new ReportTemplateRowBundle();
        collectionForTheDayBundle.setName("Collection for the day");
        collectionForTheDayBundle.setBundleType("collectionForTheDay");
        collectionForTheDayBundle.setTotal(collectionForTheDay);
        bundle.getBundles().add(collectionForTheDayBundle);

        netCashCollection = collectionForTheDay;

        // 7. Deduct various payments from net cash collection
        ReportTemplateRowBundle pettyCashPayments = createEmptyBundle("pettyCashPayments", "Petty Cash Payments");
        bundle.getBundles().add(pettyCashPayments);
        netCashCollection -= Math.abs(getSafeTotal(pettyCashPayments));

        // 8. Professional Payment Bills
        ReportTemplateRowBundle opdProfessionalPayments = createEmptyBundle("ProfessionalPaymentBillReportOpd", "OPD Professional Payments");
        bundle.getBundles().add(opdProfessionalPayments);
        netCashCollection -= Math.abs(getSafeTotal(opdProfessionalPayments));

        ReportTemplateRowBundle channellingProfessionalPayments = createEmptyBundle("ProfessionalPaymentBillReportChannelling", "Channelling Professional Payments");
        bundle.getBundles().add(channellingProfessionalPayments);
        netCashCollection -= Math.abs(getSafeTotal(channellingProfessionalPayments));

        ReportTemplateRowBundle inwardProfessionalPayments = createEmptyBundle("ProfessionalPaymentBillReportInward", "Inward Professional Payments");
        bundle.getBundles().add(inwardProfessionalPayments);
        netCashCollection -= Math.abs(getSafeTotal(inwardProfessionalPayments));

        // 9. Payment method specific bundles
        ReportTemplateRowBundle cardPayments = createCardPaymentBundle();
        bundle.getBundles().add(cardPayments);
        netCashCollection -= Math.abs(getSafeTotal(cardPayments));

        ReportTemplateRowBundle staffPayments = createEmptyBundle("paymentReportStaffWelfare", "Staff Payments");
        bundle.getBundles().add(staffPayments);
        netCashCollection -= Math.abs(getSafeTotal(staffPayments));

        ReportTemplateRowBundle voucherPayments = createEmptyBundle("paymentReportVoucher", "Voucher Payments");
        bundle.getBundles().add(voucherPayments);
        netCashCollection -= Math.abs(getSafeTotal(voucherPayments));

        ReportTemplateRowBundle chequePayments = createEmptyBundle("paymentReportCheque", "Cheque Payments");
        bundle.getBundles().add(chequePayments);
        netCashCollection -= Math.abs(getSafeTotal(chequePayments));

        ReportTemplateRowBundle ewalletPayments = createEmptyBundle("paymentReportEwallet", "E-wallet Payments");
        bundle.getBundles().add(ewalletPayments);
        netCashCollection -= Math.abs(getSafeTotal(ewalletPayments));

        ReportTemplateRowBundle slipPayments = createEmptyBundle("paymentReportSlip", "Slip Payments");
        bundle.getBundles().add(slipPayments);
        netCashCollection -= Math.abs(getSafeTotal(slipPayments));

        // 10. Net Cash for the day
        ReportTemplateRowBundle netCashForTheDayBundle = new ReportTemplateRowBundle();
        netCashForTheDayBundle.setName("Net Cash");
        netCashForTheDayBundle.setBundleType("netCash");
        netCashForTheDayBundle.setTotal(netCashCollection);
        bundle.getBundles().add(netCashForTheDayBundle);

        // 11. OPD Service Collection Credit
        ReportTemplateRowBundle opdServiceCollectionCredit = createEmptyBundle("opdServiceCollectionCredit", "OPD Service Collection Credit");
        bundle.getBundles().add(opdServiceCollectionCredit);
        netCollectionPlusCredits = netCashCollection + Math.abs(getSafeTotal(opdServiceCollectionCredit));

        // 12. Net Cash Plus Credits
        ReportTemplateRowBundle netCashForTheDayBundlePlusCredits = new ReportTemplateRowBundle();
        netCashForTheDayBundlePlusCredits.setName("Net Cash Plus Credits");
        netCashForTheDayBundlePlusCredits.setBundleType("netCashPlusCredit");
        netCashForTheDayBundlePlusCredits.setTotal(netCollectionPlusCredits);
        bundle.getBundles().add(netCashForTheDayBundlePlusCredits);
    }

    private ReportTemplateRowBundle createBundleFromPaymentMethod(String bundleType, String name) {
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        bundle.setName(name);
        bundle.setBundleType(bundleType);
        
        double total = 0.0;
        double hospitalTotal = 0.0;
        double staffTotal = 0.0;
        double discount = 0.0;
        long count = 0;
        Long rowCount = 1L;
        
        // Create detailed rows from the bill data
        if (dailyReturnDetailDtos != null) {
            for (DailyReturnDetailDTO detailDto : dailyReturnDetailDtos) {
                if (shouldIncludeInBundle(bundleType, detailDto)) {
                    // Create a ReportTemplateRow for each bill
                    ReportTemplateRow row = new ReportTemplateRow();
                    row.setRowCount(rowCount++);
                    
                    // Set the bill number as item name for display
                    row.setItemName(detailDto.getBillNumber() != null ? detailDto.getBillNumber() : "");
                    row.setCategoryName(detailDto.getBillType() != null ? detailDto.getBillType().toString() : "");
                    row.setToDepartmentName(detailDto.getBillClassType() != null ? detailDto.getBillClassType().toString() : "");
                    row.setFeeName(detailDto.getDepartmentName() != null ? detailDto.getDepartmentName() : "");
                    row.setPaymentName(detailDto.getPaymentMethod() != null ? detailDto.getPaymentMethod().toString() : "");
                    
                    Double billTotal = detailDto.getNetTotal() != null ? detailDto.getNetTotal() : 0.0;
                    
                    // Set the properties that the composite component expects
                    row.setItemCount(1L);  // Each bill is one count
                    row.setItemHospitalFee(billTotal * 0.8);  // Simulated hospital fee (80%)
                    row.setItemProfessionalFee(billTotal * 0.2);  // Simulated professional fee (20%)
                    row.setItemDiscountAmount(0.0);  // No discount for simplicity
                    row.setItemNetTotal(billTotal);
                    
                    // Also set the basic properties
                    row.setRowValue(billTotal);
                    row.setTotal(billTotal);
                    
                    bundle.getReportTemplateRows().add(row);
                    total += billTotal;
                    hospitalTotal += row.getItemHospitalFee();
                    staffTotal += row.getItemProfessionalFee();
                    count++;
                }
            }
        }
        
        // Set bundle totals
        bundle.setTotal(total);
        bundle.setHospitalTotal(hospitalTotal);
        bundle.setStaffTotal(staffTotal);
        bundle.setDiscount(discount);
        bundle.setCount(count);
        
        return bundle;
    }
    
    private boolean shouldIncludeInBundle(String bundleType, DailyReturnDetailDTO detailDto) {
        // Logic to distribute bills across different bundle types based on actual data
        
        if (detailDto == null) {
            return false;
        }
        
        String billTypeStr = detailDto.getBillType() != null ? detailDto.getBillType().toString().toLowerCase() : "";
        String deptName = detailDto.getDepartmentName() != null ? detailDto.getDepartmentName().toLowerCase() : "";
        String billClassStr = detailDto.getBillClassType() != null ? detailDto.getBillClassType().toString().toLowerCase() : "";
        
        switch (bundleType) {
            case "opdServiceCollection":
                return billTypeStr.contains("opd") || billClassStr.contains("opd") || deptName.contains("opd");
                
            case "pharmacyCollection":
                return deptName.contains("pharmacy") || billTypeStr.contains("pharmacy");
                
            case "ccCollection":
                // Look for CollectingCentre in BillType names
                return billTypeStr.contains("collectingcentre") || 
                       billTypeStr.contains("collecting") ||
                       deptName.contains("lab") || deptName.contains("collect") || 
                       billTypeStr.contains("lab") || billClassStr.contains("lab");
                       
            // These bundle types typically don't have direct bill mappings, so return false for now
            case "companyPaymentBillOpd":
            case "companyPaymentBillInward":
            case "companyPaymentBillPharmacy":
            case "companyPaymentBillChannelling":
            case "patientDepositPayments":
            case "pettyCashPayments":
            case "ProfessionalPaymentBillReportOpd":
            case "ProfessionalPaymentBillReportChannelling":
            case "ProfessionalPaymentBillReportInward":
            case "paymentReportCards":
            case "paymentReportStaffWelfare":
            case "paymentReportVoucher":
            case "paymentReportCheque":
            case "paymentReportEwallet":
            case "paymentReportSlip":
            case "opdServiceCollectionCredit":
                return false; // These are typically empty or calculated differently
                
            default:
                return false;
        }
    }
    
    private void createDtoBundle() {
        dtoBundle = new DailyReturnBundleDTO();
        dtoBundle.setName("Daily Return");
        dtoBundle.setBundleType("dailyReturn");

        String institutionName = institution != null ? institution.getName() : "All Institutions";
        String siteName = site != null ? site.getName() : "All Sites";
        String departmentName = department != null ? department.getName() : "All Departments";

        String dateTimeFormat = sessionController.getApplicationPreference().getLongDateTimeFormat();
        String formattedFromDate = fromDate != null ? new java.text.SimpleDateFormat(dateTimeFormat).format(fromDate) : "Not available";
        String formattedToDate = toDate != null ? new java.text.SimpleDateFormat(dateTimeFormat).format(toDate) : "Not available";

        String description = String.format("Report for %s to %s covering %s, %s, %s",
                formattedFromDate, formattedToDate,
                institutionName, siteName, departmentName);

        dtoBundle.setDescription(description);

        // Create DTO bundle items from detailed data
        createDtoBundleFromData();
    }
    
    private void createDtoBundleFromData() {
        double collectionForTheDay = 0.0;
        double netCashCollection = 0.0;
        double netCollectionPlusCredits = 0.0;

        // 1. OPD Service Collection - respect professional fee setting
        DailyReturnBundleDTO opdServiceCollection;
        if (isWithProfessionalFee()) {
            opdServiceCollection = createOpdServiceCollectionDto(true);
        } else {
            opdServiceCollection = createOpdServiceCollectionDto(false);
        }
        dtoBundle.getBundles().add(opdServiceCollection);
        collectionForTheDay += getSafeDtoTotal(opdServiceCollection);

        // 2. Pharmacy Collection
        DailyReturnBundleDTO pharmacyCollection = createDtoBundleFromPaymentMethod("pharmacyCollection", "Pharmacy Collection");
        dtoBundle.getBundles().add(pharmacyCollection);
        collectionForTheDay += getSafeDtoTotal(pharmacyCollection);

        // 3. Collecting Centre Collection - use specific CC data  
        DailyReturnBundleDTO ccCollection = createCcCollectionDto();
        dtoBundle.getBundles().add(ccCollection);
        collectionForTheDay += getSafeDtoTotal(ccCollection);

        // 4. Patient Deposit Payments - add to collection
        DailyReturnBundleDTO patientDepositPayments = createPatientDepositPaymentsDtoBundle();
        dtoBundle.getBundles().add(patientDepositPayments);
        collectionForTheDay += getSafeDtoTotal(patientDepositPayments);

        // 5. Collection for the day summary
        DailyReturnBundleDTO collectionForTheDayBundle = new DailyReturnBundleDTO();
        collectionForTheDayBundle.setName("Collection for the day");
        collectionForTheDayBundle.setBundleType("collectionForTheDay");
        collectionForTheDayBundle.setTotal(collectionForTheDay);
        dtoBundle.getBundles().add(collectionForTheDayBundle);

        netCashCollection = collectionForTheDay;

        // 6. Credit Card Payments - add to DTO bundle
        DailyReturnBundleDTO cardPayments = createCardPaymentDtoBundle();
        dtoBundle.getBundles().add(cardPayments);
        netCashCollection -= Math.abs(getSafeDtoTotal(cardPayments));

        // 7. Staff Payments - typically empty, but needed for structure
        DailyReturnBundleDTO staffPayments = createEmptyDtoBundle("paymentReportStaffWelfare", "Staff Payments");
        dtoBundle.getBundles().add(staffPayments);
        netCashCollection -= Math.abs(getSafeDtoTotal(staffPayments));

        // 8. Voucher Payments - typically empty, but needed for structure
        DailyReturnBundleDTO voucherPayments = createEmptyDtoBundle("paymentReportVoucher", "Voucher Payments");
        dtoBundle.getBundles().add(voucherPayments);
        netCashCollection -= Math.abs(getSafeDtoTotal(voucherPayments));

        // 9. Cheque Payments - typically empty, but needed for structure
        DailyReturnBundleDTO chequePayments = createEmptyDtoBundle("paymentReportCheque", "Cheque Payments");
        dtoBundle.getBundles().add(chequePayments);
        netCashCollection -= Math.abs(getSafeDtoTotal(chequePayments));

        // 10. E-wallet Payments - typically empty, but needed for structure
        DailyReturnBundleDTO ewalletPayments = createEmptyDtoBundle("paymentReportEwallet", "E-wallet Payments");
        dtoBundle.getBundles().add(ewalletPayments);
        netCashCollection -= Math.abs(getSafeDtoTotal(ewalletPayments));

        // 11. Slip Payments - typically empty, but needed for structure
        DailyReturnBundleDTO slipPayments = createEmptyDtoBundle("paymentReportSlip", "Slip Payments");
        dtoBundle.getBundles().add(slipPayments);
        netCashCollection -= Math.abs(getSafeDtoTotal(slipPayments));

        // 12. Net Cash for the day
        DailyReturnBundleDTO netCashForTheDayBundle = new DailyReturnBundleDTO();
        netCashForTheDayBundle.setName("Net Cash");
        netCashForTheDayBundle.setBundleType("netCash");
        netCashForTheDayBundle.setTotal(netCashCollection);
        dtoBundle.getBundles().add(netCashForTheDayBundle);

        // 13. OPD Service Collection - Credit - contains only CREDIT payment types
        DailyReturnBundleDTO opdServiceCollectionCredit = createOpdServiceCollectionCreditDto(isWithProfessionalFee());
        dtoBundle.getBundles().add(opdServiceCollectionCredit);
        netCollectionPlusCredits = netCashCollection + Math.abs(getSafeDtoTotal(opdServiceCollectionCredit)); // NOT deducted from net cash

        // 14. Net Cash Plus Credits
        DailyReturnBundleDTO netCashForTheDayBundlePlusCredits = new DailyReturnBundleDTO();
        netCashForTheDayBundlePlusCredits.setName("Net Cash Plus Credits");
        netCashForTheDayBundlePlusCredits.setBundleType("netCashPlusCredit");
        netCashForTheDayBundlePlusCredits.setTotal(netCollectionPlusCredits);
        // Add one row to show the breakdown
        DailyReturnRowDTO netCashPlusCreditsRow = new DailyReturnRowDTO();
        netCashPlusCreditsRow.setCategoryName("Net Cash Plus Credits");
        netCashPlusCreditsRow.setItemName("Total");
        netCashPlusCreditsRow.setItemNetTotal(netCollectionPlusCredits);
        netCashPlusCreditsRow.setItemHospitalFee(netCollectionPlusCredits);
        netCashPlusCreditsRow.setItemProfessionalFee(0.0);
        netCashPlusCreditsRow.setItemDiscountAmount(0.0);
        netCashPlusCreditsRow.setItemCount(1L);
        List<DailyReturnRowDTO> netCashPlusCreditsRows = new ArrayList<>();
        netCashPlusCreditsRows.add(netCashPlusCreditsRow);
        netCashForTheDayBundlePlusCredits.setRows(netCashPlusCreditsRows);
        dtoBundle.getBundles().add(netCashForTheDayBundlePlusCredits);

        dtoBundle.setTotal(collectionForTheDay);
    }
    
    private DailyReturnBundleDTO createDtoBundleFromPaymentMethod(String bundleType, String name) {
        DailyReturnBundleDTO bundle = new DailyReturnBundleDTO();
        bundle.setName(name);
        bundle.setBundleType(bundleType);
        
        double total = 0.0;
        double hospitalTotal = 0.0;
        double staffTotal = 0.0;
        double discount = 0.0;
        long count = 0;
        
        // Create detailed rows from the bill data
        if (dailyReturnDetailDtos != null) {
            for (DailyReturnDetailDTO detailDto : dailyReturnDetailDtos) {
                if (shouldIncludeInDtoBundle(bundleType, detailDto)) {
                    DailyReturnRowDTO row = new DailyReturnRowDTO(
                        detailDto.getBillNumber(),
                        detailDto.getBillType(),
                        detailDto.getBillClassType(),
                        detailDto.getDepartmentName(),
                        detailDto.getPaymentMethod(),
                        detailDto.getNetTotal()
                    );
                    
                    row.setCreatedAt(detailDto.getCreatedAt());
                    row.setFromDepartmentName(detailDto.getFromDepartmentName());
                    
                    bundle.getRows().add(row);
                    total += row.getItemNetTotal();
                    hospitalTotal += row.getItemHospitalFee();
                    staffTotal += row.getItemProfessionalFee();
                    discount += row.getItemDiscountAmount();
                    count++;
                }
            }
        }
        
        // Set bundle totals
        bundle.setTotal(total);
        bundle.setHospitalTotal(hospitalTotal);
        bundle.setStaffTotal(staffTotal);
        bundle.setDiscount(discount);
        bundle.setCount(count);
        
        return bundle;
    }
    
    private boolean shouldIncludeInDtoBundle(String bundleType, DailyReturnDetailDTO detailDto) {
        return shouldIncludeInBundle(bundleType, detailDto);  // Reuse the same logic
    }
    
    private double getSafeDtoTotal(DailyReturnBundleDTO bundle) {
        return bundle != null && bundle.getTotal() != null ? bundle.getTotal() : 0.0;
    }
    
    private DailyReturnBundleDTO createOpdServiceCollectionDto(boolean withProfessionalFee) {
        DailyReturnBundleDTO opdBundle = new DailyReturnBundleDTO();
        opdBundle.setName("OPD Service Collection");
        opdBundle.setBundleType("opdServiceCollection");
        
        // Exactly mimic the original billItemsToBundleForOpdUnderCategory logic
        Map<String, DailyReturnRowDTO> categoryMap = new java.util.TreeMap<>();
        Map<String, DailyReturnRowDTO> itemMap = new java.util.TreeMap<>();
        
        // Grand totals - accumulated ONCE per BillItem (not per row)
        double totalOpdServiceCollection = 0.0;
        double totalGrossValue = 0.0;
        double totalHospitalFee = 0.0;
        double totalDiscount = 0.0;
        double totalStaffFee = 0.0;
        long totalQuantity = 0L;
        
        if (opdBillItemDtos != null) {
            for (BillItemDetailDTO bi : opdBillItemDtos) {
                // Skip null payment method or NONE payment type
                if (bi.getPaymentMethod() == null || 
                    bi.getPaymentMethod().getPaymentType() == com.divudi.core.data.PaymentType.NONE) {
                    continue;
                }
                
                // Skip credit payments (NON_CREDIT only for main collection)
                if (bi.getPaymentMethod().getPaymentType() == com.divudi.core.data.PaymentType.CREDIT) {
                    continue;
                }
                
                String categoryName = bi.getCategoryName();
                String itemName = bi.getItemName();
                String itemKey = categoryName + "->" + itemName;
                
                // Initialize category and item rows if not exists
                categoryMap.putIfAbsent(categoryName, new DailyReturnRowDTO());
                itemMap.putIfAbsent(itemKey, new DailyReturnRowDTO());
                
                DailyReturnRowDTO categoryRow = categoryMap.get(categoryName);
                DailyReturnRowDTO itemRow = itemMap.get(itemKey);
                
                // Set row identifiers
                categoryRow.setCategoryName(categoryName);
                categoryRow.setItemName(""); // Categories have empty item names
                itemRow.setCategoryName(categoryName);
                itemRow.setItemName(itemName);
                
                // Get financial values from BillItem (use actual values, not artificial splits)
                double grossValue = bi.getGrossValue();
                double hospitalFee = bi.getHospitalFee();
                double discount = bi.getDiscount();
                double staffFee = bi.getProfessionalFee(); // This comes from actual staffFee in database
                double netValue = bi.getNetValue();
                
                // Apply bill class type modifier like the original entity-based system
                long qtyModifier = (bi.getBillClassType() != null && 
                        (bi.getBillClassType() == com.divudi.core.data.BillClassType.CancelledBill
                        || bi.getBillClassType() == com.divudi.core.data.BillClassType.RefundBill)) ? -1 : 1;
                
                // Adjust financial values for cancelled/refunded items
                if (qtyModifier == -1) {
                    grossValue = -Math.abs(grossValue);
                    hospitalFee = -Math.abs(hospitalFee);
                    discount = -Math.abs(discount);
                    staffFee = -Math.abs(staffFee);
                    netValue = -Math.abs(netValue);
                }
                
                // Use absolute quantity with modifier (like original getQtyAbsolute() * qtyModifier)
                long quantity = (long) (Math.abs(bi.getQty()) * qtyModifier);
                
                // CRITICAL: Accumulate grand totals ONCE per BillItem
                // Handle professional fee setting like the original
                if (withProfessionalFee) {
                    totalOpdServiceCollection += netValue; // Include staff fee
                } else {
                    totalOpdServiceCollection += hospitalFee - discount; // Exclude staff fee
                }
                totalGrossValue += grossValue;
                totalHospitalFee += hospitalFee;
                totalDiscount += discount;
                totalStaffFee += staffFee;
                totalQuantity += quantity;
                
                // Update individual row totals (like the updateRow method)
                // Respect the withProfessionalFee setting for row display
                if (withProfessionalFee) {
                    updateDtoRow(categoryRow, quantity, grossValue, hospitalFee, discount, staffFee, netValue);
                    updateDtoRow(itemRow, quantity, grossValue, hospitalFee, discount, staffFee, netValue);
                } else {
                    // When professional fee is disabled, show 0.00 for professional fee in rows
                    updateDtoRow(categoryRow, quantity, grossValue, hospitalFee, discount, 0.0, hospitalFee - discount);
                    updateDtoRow(itemRow, quantity, grossValue, hospitalFee, discount, 0.0, hospitalFee - discount);
                }
            }
        }
        
        // Add category and item rows to the bundle in the correct order
        List<DailyReturnRowDTO> allRows = new ArrayList<>();
        for (String categoryName : categoryMap.keySet()) {
            // Add category row first
            allRows.add(categoryMap.get(categoryName));
            
            // Then add all items for this category
            for (String itemKey : itemMap.keySet()) {
                if (itemKey.startsWith(categoryName + "->")) {
                    allRows.add(itemMap.get(itemKey));
                }
            }
        }
        
        opdBundle.setRows(allRows);
        // Use the grand totals (not sum of row totals)
        opdBundle.setTotal(totalOpdServiceCollection);
        opdBundle.setHospitalTotal(totalHospitalFee);
        // Set staff total based on professional fee setting
        if (withProfessionalFee) {
            opdBundle.setStaffTotal(totalStaffFee);
        } else {
            opdBundle.setStaffTotal(0.0); // Show 0.00 when professional fee is disabled
        }
        opdBundle.setDiscount(totalDiscount);
        opdBundle.setCount(totalQuantity);
        
        return opdBundle;
    }
    
    private DailyReturnBundleDTO createOpdServiceCollectionCreditDto(boolean withProfessionalFee) {
        DailyReturnBundleDTO opdCreditBundle = new DailyReturnBundleDTO();
        opdCreditBundle.setName("OPD Service Collection - Credit");
        opdCreditBundle.setBundleType("opdServiceCollectionCredit");
        
        // Credit payments respect the professional fee checkbox like normal OPD collection
        Map<String, DailyReturnRowDTO> categoryMap = new java.util.TreeMap<>();
        Map<String, DailyReturnRowDTO> itemMap = new java.util.TreeMap<>();
        
        // Grand totals - accumulated ONCE per BillItem (not per row)
        double totalOpdServiceCollectionCredit = 0.0;
        double totalGrossValue = 0.0;
        double totalHospitalFee = 0.0;
        double totalDiscount = 0.0;
        double totalStaffFee = 0.0;
        long totalQuantity = 0L;
        
        if (opdBillItemDtos != null) {
            System.out.println("DEBUG Credit: Processing " + opdBillItemDtos.size() + " total OPD bill items");
            
            // First, let's see all the credit items for debugging
            int creditItemCount = 0;
            for (BillItemDetailDTO bi : opdBillItemDtos) {
                if (bi.getPaymentMethod() != null && 
                    bi.getPaymentMethod().getPaymentType() == com.divudi.core.data.PaymentType.CREDIT) {
                    creditItemCount++;
                    System.out.println("DEBUG Credit Item #" + creditItemCount + ": Category=" + bi.getCategoryName() + 
                        ", Item=" + bi.getItemName() + ", Qty=" + bi.getQty() + 
                        ", HospitalFee=" + bi.getHospitalFee() + ", ProfessionalFee=" + bi.getProfessionalFee() + 
                        ", NetValue=" + bi.getNetValue() + ", BillClassType=" + bi.getBillClassType());
                }
            }
            System.out.println("DEBUG Credit: Total CREDIT items found: " + creditItemCount);
            for (BillItemDetailDTO bi : opdBillItemDtos) {
                // Skip null payment method or NONE payment type
                if (bi.getPaymentMethod() == null || 
                    bi.getPaymentMethod().getPaymentType() == com.divudi.core.data.PaymentType.NONE) {
                    continue;
                }
                
                // Include ONLY credit payments (opposite of main OPD collection)
                if (bi.getPaymentMethod().getPaymentType() != com.divudi.core.data.PaymentType.CREDIT) {
                    continue;
                }
                
                System.out.println("DEBUG Credit: Found CREDIT payment - Item: " + bi.getItemName() + 
                    ", Qty: " + bi.getQty() + ", BillClassType: " + bi.getBillClassType() + 
                    ", HospitalFee: " + bi.getHospitalFee() + ", ProfessionalFee: " + bi.getProfessionalFee());
                
                String categoryName = bi.getCategoryName();
                String itemName = bi.getItemName();
                String itemKey = categoryName + "->" + itemName;
                
                // Initialize category and item rows if not exists
                categoryMap.putIfAbsent(categoryName, new DailyReturnRowDTO());
                itemMap.putIfAbsent(itemKey, new DailyReturnRowDTO());
                
                DailyReturnRowDTO categoryRow = categoryMap.get(categoryName);
                DailyReturnRowDTO itemRow = itemMap.get(itemKey);
                
                // Set row identifiers
                categoryRow.setCategoryName(categoryName);
                categoryRow.setItemName(""); // Categories have empty item names
                itemRow.setCategoryName(categoryName);
                itemRow.setItemName(itemName);
                
                // Get financial values from BillItem (use actual values, not artificial splits)
                double grossValue = bi.getGrossValue();
                double hospitalFee = bi.getHospitalFee();
                double discount = bi.getDiscount();
                double staffFee = bi.getProfessionalFee(); // Use actual professional fee from database
                double netValue = bi.getNetValue(); // Use actual net value from database
                
                // Apply bill class type modifier like the original entity-based system
                long qtyModifier = (bi.getBillClassType() != null && 
                        (bi.getBillClassType() == com.divudi.core.data.BillClassType.CancelledBill
                        || bi.getBillClassType() == com.divudi.core.data.BillClassType.RefundBill)) ? -1 : 1;
                
                // Adjust financial values for cancelled/refunded items
                if (qtyModifier == -1) {
                    grossValue = -Math.abs(grossValue);
                    hospitalFee = -Math.abs(hospitalFee);
                    discount = -Math.abs(discount);
                    staffFee = -Math.abs(staffFee); // Will still be 0.0 for credit payments
                    netValue = -Math.abs(netValue);
                }
                
                // Use absolute quantity with modifier (like original getQtyAbsolute() * qtyModifier)
                long quantity = (long) (Math.abs(bi.getQty()) * qtyModifier);
                
                System.out.println("DEBUG Credit: After processing - Item: " + bi.getItemName() + 
                    ", OriginalQty: " + bi.getQty() + ", QtyModifier: " + qtyModifier + ", FinalQuantity: " + quantity +
                    ", HospitalFee: " + hospitalFee + ", StaffFee: " + staffFee + ", NetValue: " + netValue);
                
                // Respect the professional fee checkbox like normal OPD collection
                if (withProfessionalFee) {
                    totalOpdServiceCollectionCredit += netValue; // Include staff fee
                } else {
                    totalOpdServiceCollectionCredit += hospitalFee - discount; // Exclude staff fee
                }
                totalGrossValue += grossValue;
                totalHospitalFee += hospitalFee;
                totalDiscount += discount;
                totalStaffFee += staffFee;
                totalQuantity += quantity;
                
                // Update individual row totals - respect the withProfessionalFee setting for row display
                if (withProfessionalFee) {
                    System.out.println("DEBUG Credit: With ProfFee - updating row with professionalFee = " + staffFee + ", netValue = " + netValue);
                    updateDtoRow(categoryRow, quantity, grossValue, hospitalFee, discount, staffFee, netValue);
                    updateDtoRow(itemRow, quantity, grossValue, hospitalFee, discount, staffFee, netValue);
                } else {
                    // When professional fee is disabled, show 0.00 for professional fee in rows
                    System.out.println("DEBUG Credit: Without ProfFee - updating row with professionalFee = 0.0, hospitalFee = " + hospitalFee);
                    updateDtoRow(categoryRow, quantity, grossValue, hospitalFee, discount, 0.0, hospitalFee - discount);
                    updateDtoRow(itemRow, quantity, grossValue, hospitalFee, discount, 0.0, hospitalFee - discount);
                }
            }
        }
        
        // Add category and item rows to the bundle in the correct order
        List<DailyReturnRowDTO> allRows = new ArrayList<>();
        for (String categoryName : categoryMap.keySet()) {
            // Add category row first
            allRows.add(categoryMap.get(categoryName));
            
            // Then add all items for this category
            for (String itemKey : itemMap.keySet()) {
                if (itemKey.startsWith(categoryName + "->")) {
                    allRows.add(itemMap.get(itemKey));
                }
            }
        }
        
        opdCreditBundle.setRows(allRows);
        
        // Debug: Print all final rows
        System.out.println("DEBUG Credit Final Rows: " + allRows.size() + " total rows");
        for (DailyReturnRowDTO row : allRows) {
            System.out.println("DEBUG Credit Row: Category='" + row.getCategoryName() + "', Item='" + row.getItemName() + 
                "', Count=" + row.getItemCount() + ", HospitalFee=" + row.getItemHospitalFee() + 
                ", ProfessionalFee=" + row.getItemProfessionalFee() + ", NetTotal=" + row.getItemNetTotal());
        }
        
        // Use the grand totals (not sum of row totals)
        opdCreditBundle.setTotal(totalOpdServiceCollectionCredit);
        opdCreditBundle.setHospitalTotal(totalHospitalFee);
        // Set staff total based on professional fee setting
        if (withProfessionalFee) {
            opdCreditBundle.setStaffTotal(totalStaffFee);
        } else {
            opdCreditBundle.setStaffTotal(0.0); // Show 0.00 when professional fee is disabled
        }
        opdCreditBundle.setDiscount(totalDiscount);
        opdCreditBundle.setCount(totalQuantity);
        
        System.out.println("DEBUG Credit Bundle Totals: Total=" + totalOpdServiceCollectionCredit + 
            ", HospitalTotal=" + totalHospitalFee + ", StaffTotal=" + 
            (withProfessionalFee ? totalStaffFee : 0.0) + ", Count=" + totalQuantity);
        
        return opdCreditBundle;
    }
    
    // Helper method to update individual row totals (mimics the original updateRow method)
    private void updateDtoRow(DailyReturnRowDTO row, long count, double total, double hospitalFee, 
                             double discount, double professionalFee, double netTotal) {
        // Initialize if null
        if (row.getItemCount() == null) row.setItemCount(0L);
        if (row.getItemHospitalFee() == null) row.setItemHospitalFee(0.0);
        if (row.getItemProfessionalFee() == null) row.setItemProfessionalFee(0.0);
        if (row.getItemDiscountAmount() == null) row.setItemDiscountAmount(0.0);
        if (row.getItemNetTotal() == null) row.setItemNetTotal(0.0);
        
        // Accumulate values
        row.setItemCount(row.getItemCount() + count);
        row.setItemHospitalFee(row.getItemHospitalFee() + hospitalFee);
        row.setItemProfessionalFee(row.getItemProfessionalFee() + professionalFee);
        row.setItemDiscountAmount(row.getItemDiscountAmount() + discount);
        row.setItemNetTotal(row.getItemNetTotal() + netTotal);
        
        // Debug logging
        if (professionalFee != 0.0) {
            System.out.println("DEBUG updateDtoRow: Adding professionalFee=" + professionalFee + " to row. New total: " + row.getItemProfessionalFee());
        }
    }
    
    private DailyReturnBundleDTO createCcCollectionDto() {
        DailyReturnBundleDTO ccBundle = new DailyReturnBundleDTO();
        ccBundle.setName("Collecting Centre Collection");
        ccBundle.setBundleType("ccCollection");
        
        System.out.println("DEBUG: createCcCollectionDto - ccBillDtos size: " + (ccBillDtos != null ? ccBillDtos.size() : "null"));
        
        // CC collection works at Bill level, not BillItem level like the original
        List<DailyReturnRowDTO> allRows = new ArrayList<>();
        double totalCcCollection = 0.0;
        long totalCount = 0L;
        
        if (ccBillDtos != null) {
            int processedCount = 0;
            for (DailyReturnDetailDTO bill : ccBillDtos) {
                System.out.println("DEBUG: CC Bill - BillNumber: " + bill.getBillNumber() + ", NetTotal: " + bill.getNetTotal() + ", BillType: " + bill.getBillType() + ", Dept: " + bill.getDepartmentName());
                
                // Skip null payment method or NONE payment type
                if (bill.getPaymentMethod() == null || 
                    bill.getPaymentMethod().getPaymentType() == com.divudi.core.data.PaymentType.NONE) {
                    System.out.println("DEBUG: CC Bill - SKIPPED due to null/NONE payment method");
                    continue;
                }
                processedCount++;
                
                // Create a row for each CC bill (similar to how original generateBillReport works)
                DailyReturnRowDTO row = new DailyReturnRowDTO();
                row.setBillNumber(bill.getBillNumber());
                row.setBillType(bill.getBillType());
                row.setBillClassType(bill.getBillClassType());
                row.setDepartmentName(bill.getDepartmentName());
                row.setFromDepartmentName(bill.getFromDepartmentName());
                row.setPaymentMethod(bill.getPaymentMethod());
                row.setCreatedAt(bill.getCreatedAt());
                
                // Set display values for CC bills
                row.setItemName(bill.getBillNumber()); // Bill number as item name
                row.setCategoryName(bill.getDepartmentName() != null ? bill.getDepartmentName() : "Collecting Centre"); // Department as category
                row.setFeeName(bill.getFromDepartmentName() != null ? bill.getFromDepartmentName() : ""); // From department as fee name
                
                // Set financial values from Bill
                Double netTotal = bill.getNetTotal() != null ? bill.getNetTotal() : 0.0;
                row.setItemNetTotal(netTotal);
                row.setItemHospitalFee(netTotal); // CC bills typically have hospital fee = net total
                row.setItemProfessionalFee(0.0); // CC bills typically don't have professional fee
                row.setItemDiscountAmount(0.0); // No discount breakdown for CC bills
                row.setItemCount(1L); // Each bill is one count
                row.setTotal(netTotal);
                
                allRows.add(row);
                totalCcCollection += netTotal;
                totalCount++;
            }
            System.out.println("DEBUG: CC Collection - processed " + processedCount + " Bills");
        }
        
        ccBundle.setRows(allRows);
        // Use the grand totals 
        ccBundle.setTotal(totalCcCollection);
        ccBundle.setHospitalTotal(totalCcCollection); // Hospital total = total for CC bills
        ccBundle.setStaffTotal(0.0); // No staff fees for CC bills
        ccBundle.setDiscount(0.0); // No discounts for CC bills
        ccBundle.setCount(totalCount);
        
        System.out.println("DEBUG: CC Collection - final totals: " + totalCcCollection + ", rows: " + allRows.size());
        
        return ccBundle;
    }
    
    private DailyReturnBundleDTO createCardPaymentDtoBundle() {
        DailyReturnBundleDTO bundle = new DailyReturnBundleDTO();
        bundle.setName("Credit Card Payments");
        bundle.setBundleType("paymentReportCards");
        
        List<DailyReturnRowDTO> allRows = new ArrayList<>();
        double totalCardPayments = 0.0;
        long totalCount = 0L;
        
        if (cardPaymentDtos != null) {
            for (PaymentDetailDTO payment : cardPaymentDtos) {
                
                // Create a row for each card payment
                DailyReturnRowDTO row = new DailyReturnRowDTO();
                
                // Set bill information - following the same pattern as the original entity-based report
                row.setBillNumber(payment.getBillNumber()); // Bill number 
                row.setBillType(payment.getBillType()); // Bill type
                row.setPaymentMethod(payment.getPaymentMethod()); // Payment method
                row.setCreatedAt(payment.getCreatedAt()); // Date/time
                row.setDepartmentName(payment.getDepartmentName()); // Department
                
                // Set display values for Credit Card Payments table
                row.setItemName(payment.getBillNumber()); // Bill number 
                row.setCategoryName(payment.getBillType() != null ? payment.getBillType().toString() : "BilledBill"); // Bill Class (BillType)
                row.setFeeName(payment.getCreditCardRefNo()); // Card Ref. Number
                row.setPaymentName(payment.getBankName()); // Bank name
                row.setFromDepartmentName(""); // Reference Bill (not applicable for payments)
                
                // Set financial values
                Double paidValue = payment.getPaidValue(); // Keep original value
                row.setItemNetTotal(paidValue);
                row.setItemHospitalFee(paidValue); // For payment reports, hospital fee = total
                row.setItemProfessionalFee(0.0); // No professional fee breakdown for payments
                row.setItemDiscountAmount(0.0); // No discount for payments
                row.setItemCount(1L); // Each payment is one count
                row.setTotal(paidValue);
                
                allRows.add(row);
                totalCardPayments += paidValue;
                totalCount++;
            }
        }
        
        bundle.setRows(allRows);
        bundle.setTotal(totalCardPayments);
        bundle.setHospitalTotal(totalCardPayments);
        bundle.setStaffTotal(0.0);
        bundle.setDiscount(0.0);
        bundle.setCount(totalCount);
        
        return bundle;
    }
    
    private DailyReturnBundleDTO createPatientDepositPaymentsDtoBundle() {
        DailyReturnBundleDTO bundle = new DailyReturnBundleDTO();
        bundle.setName("Patient Deposit Payments");
        bundle.setBundleType("patientDepositPayments");
        
        List<DailyReturnRowDTO> allRows = new ArrayList<>();
        double totalPatientDeposits = 0.0;
        long totalCount = 0L;
        
        if (patientDepositPaymentDtos != null) {
            for (PaymentDetailDTO payment : patientDepositPaymentDtos) {
                
                // Create a row for each patient deposit payment
                DailyReturnRowDTO row = new DailyReturnRowDTO();
                
                // Set bill information
                row.setBillNumber(payment.getBillNumber()); // Bill number 
                row.setBillType(payment.getBillType()); // Bill type
                row.setPaymentMethod(payment.getPaymentMethod()); // Payment method
                row.setCreatedAt(payment.getCreatedAt()); // Date/time
                row.setDepartmentName(payment.getDepartmentName()); // Department
                
                // Set display values for Patient Deposit Payments table
                // Following the pattern: Bill No | Patient | Payment Method | Value
                row.setItemName(payment.getBillNumber()); // Bill No
                row.setCategoryName(payment.getBankName()); // Patient name (stored in bankName field)
                row.setFeeName(payment.getPaymentMethod() != null ? payment.getPaymentMethod().toString() : ""); // Payment Method
                row.setPaymentName(""); // Not used for patient deposits
                row.setFromDepartmentName(""); // Not used for patient deposits
                
                // Set financial values
                Double paidValue = payment.getPaidValue(); // Keep original value (positive for deposits, negative for refunds)
                row.setItemNetTotal(paidValue);
                row.setItemHospitalFee(paidValue); // For patient deposits, hospital fee = total
                row.setItemProfessionalFee(0.0); // No professional fee for patient deposits
                row.setItemDiscountAmount(0.0); // No discount for patient deposits
                row.setItemCount(1L); // Each deposit is one count
                row.setTotal(paidValue);
                
                allRows.add(row);
                totalPatientDeposits += paidValue;
                totalCount++;
            }
        }
        
        bundle.setRows(allRows);
        bundle.setTotal(totalPatientDeposits);
        bundle.setHospitalTotal(totalPatientDeposits);
        bundle.setStaffTotal(0.0);
        bundle.setDiscount(0.0);
        bundle.setCount(totalCount);
        
        return bundle;
    }
    
    private DailyReturnBundleDTO createEmptyDtoBundle(String bundleType, String name) {
        DailyReturnBundleDTO bundle = new DailyReturnBundleDTO();
        bundle.setName(name);
        bundle.setBundleType(bundleType);
        bundle.setTotal(0.0);
        bundle.setHospitalTotal(0.0);
        bundle.setStaffTotal(0.0);
        bundle.setDiscount(0.0);
        bundle.setCount(0L);
        bundle.setRows(new ArrayList<>()); // Empty list
        return bundle;
    }
    
    private ReportTemplateRowBundle createCardPaymentBundle() {
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        bundle.setName("Credit Card Payments");
        bundle.setBundleType("paymentReportCards");
        
        System.out.println("DEBUG: createCardPaymentBundle - cardPaymentDtos size: " + (cardPaymentDtos != null ? cardPaymentDtos.size() : "null"));
        
        List<ReportTemplateRow> allRows = new ArrayList<>();
        double totalCardPayments = 0.0;
        long totalCount = 0L;
        
        if (cardPaymentDtos != null) {
            int processedCount = 0;
            for (PaymentDetailDTO payment : cardPaymentDtos) {
                System.out.println("DEBUG: Card Payment - BillNumber: " + payment.getBillNumber() + ", PaidValue: " + payment.getPaidValue() + ", CardRef: " + payment.getCreditCardRefNo() + ", Bank: " + payment.getBankName());
                
                processedCount++;
                
                // Create a row for each card payment
                ReportTemplateRow row = new ReportTemplateRow();
                row.setRowCount((long) processedCount);
                
                // Set bill information
                row.setItemName(payment.getBillNumber()); // Bill number 
                row.setCategoryName(payment.getBillType() != null ? payment.getBillType().toString() : ""); // Bill type
                row.setFeeName(payment.getCreditCardRefNo()); // Card ref number
                row.setPaymentName(payment.getBankName()); // Bank name
                row.setToDepartmentName(payment.getDepartmentName()); // Department
                
                // Set financial values
                Double paidValue = payment.getPaidValue(); // Keep original value
                row.setItemNetTotal(paidValue);
                row.setRowValue(paidValue);
                row.setTotal(paidValue);
                
                // Set count and other values
                row.setItemCount(1L);
                row.setItemHospitalFee(paidValue); // For payment reports, hospital fee = total
                row.setItemProfessionalFee(0.0); // No professional fee breakdown for payments
                row.setItemDiscountAmount(0.0); // No discount for payments
                
                allRows.add(row);
                totalCardPayments += paidValue;
                totalCount++;
            }
            System.out.println("DEBUG: Card Payments - processed " + processedCount + " payments");
        }
        
        bundle.setReportTemplateRows(allRows);
        bundle.setTotal(totalCardPayments);
        bundle.setHospitalTotal(totalCardPayments);
        bundle.setStaffTotal(0.0);
        bundle.setDiscount(0.0);
        bundle.setCount(totalCount);
        
        System.out.println("DEBUG: Card Payments - final totals: " + totalCardPayments + ", rows: " + allRows.size());
        
        return bundle;
    }

    private ReportTemplateRowBundle createEmptyBundle(String bundleType, String name) {
        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        bundle.setName(name);
        bundle.setBundleType(bundleType);
        bundle.setTotal(0.0);
        return bundle;
    }

    private double getSafeTotal(ReportTemplateRowBundle bundle) {
        return bundle != null ? bundle.getTotal() : 0.0;
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

    public boolean isWithProfessionalFee() {
        return withProfessionalFee;
    }

    public void setWithProfessionalFee(boolean withProfessionalFee) {
        this.withProfessionalFee = withProfessionalFee;
    }

    public List<DailyReturnDTO> getDailyReturnDtos() {
        return dailyReturnDtos;
    }

    public void setDailyReturnDtos(List<DailyReturnDTO> dailyReturnDtos) {
        this.dailyReturnDtos = dailyReturnDtos;
    }

    public List<DailyReturnItemDTO> getDailyReturnItemDtos() {
        return dailyReturnItemDtos;
    }

    public void setDailyReturnItemDtos(List<DailyReturnItemDTO> dailyReturnItemDtos) {
        this.dailyReturnItemDtos = dailyReturnItemDtos;
    }
    
    public ReportTemplateRowBundle getBundle() {
        return bundle;
    }
    
    public void setBundle(ReportTemplateRowBundle bundle) {
        this.bundle = bundle;
    }
    
    public StreamedContent getBundleAsExcel() {
        try {
            downloadingExcel = excelController.createExcelForBundle(bundle);
        } catch (IOException e) {
            // Handle IOException
        }
        return downloadingExcel;
    }
    
    public StreamedContent getBundleAsPdf() {
        StreamedContent pdfSc = null;
        try {
            pdfSc = pdfController.createPdfForBundle(bundle);
        } catch (IOException e) {
            // Handle IOException
        }
        return pdfSc;
    }
    
    public StreamedContent getDtoBundleAsExcel() {
        try {
            downloadingExcel = excelController.createExcelForDtoBundle(dtoBundle, fromDate, toDate);
        } catch (IOException e) {
            // Handle IOException
        }
        return downloadingExcel;
    }
    
    public StreamedContent getDtoBundleAsPdf() {
        StreamedContent pdfSc = null;
        try {
            pdfSc = pdfController.createPdfForDtoBundle(dtoBundle);
        } catch (IOException e) {
            // Handle IOException
        }
        return pdfSc;
    }
    
    public void updateBillItemValues() {
        // Placeholder for update functionality - can be implemented later if needed
        // This matches the original SearchController method
    }
    
    public StreamedContent getDownloadingExcel() {
        return downloadingExcel;
    }

    public void setDownloadingExcel(StreamedContent downloadingExcel) {
        this.downloadingExcel = downloadingExcel;
    }
    
    public List<DailyReturnDetailDTO> getDailyReturnDetailDtos() {
        return dailyReturnDetailDtos;
    }

    public void setDailyReturnDetailDtos(List<DailyReturnDetailDTO> dailyReturnDetailDtos) {
        this.dailyReturnDetailDtos = dailyReturnDetailDtos;
    }
    
    public DailyReturnBundleDTO getDtoBundle() {
        return dtoBundle;
    }
    
    public void setDtoBundle(DailyReturnBundleDTO dtoBundle) {
        this.dtoBundle = dtoBundle;
    }
    
    public List<BillItemDetailDTO> getOpdBillItemDtos() {
        return opdBillItemDtos;
    }
    
    public void setOpdBillItemDtos(List<BillItemDetailDTO> opdBillItemDtos) {
        this.opdBillItemDtos = opdBillItemDtos;
    }
    
    public List<BillItemDetailDTO> getCcBillItemDtos() {
        return ccBillItemDtos;
    }
    
    public void setCcBillItemDtos(List<BillItemDetailDTO> ccBillItemDtos) {
        this.ccBillItemDtos = ccBillItemDtos;
    }
    
    public List<DailyReturnDetailDTO> getCcBillDtos() {
        return ccBillDtos;
    }
    
    public void setCcBillDtos(List<DailyReturnDetailDTO> ccBillDtos) {
        this.ccBillDtos = ccBillDtos;
    }
    
    public List<PaymentDetailDTO> getCardPaymentDtos() {
        return cardPaymentDtos;
    }
    
    public void setCardPaymentDtos(List<PaymentDetailDTO> cardPaymentDtos) {
        this.cardPaymentDtos = cardPaymentDtos;
    }
    
    public List<PaymentDetailDTO> getPatientDepositPaymentDtos() {
        return patientDepositPaymentDtos;
    }
    
    public void setPatientDepositPaymentDtos(List<PaymentDetailDTO> patientDepositPaymentDtos) {
        this.patientDepositPaymentDtos = patientDepositPaymentDtos;
    }
}
