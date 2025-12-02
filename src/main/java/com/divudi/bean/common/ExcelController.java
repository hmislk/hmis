package com.divudi.bean.common;

import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.divudi.core.util.CommonFunctions;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Named
@RequestScoped
public class ExcelController {

    @Inject
    SearchController searchController;

    @Inject
    SessionController sessionController;

    /**
     * Creates a new instance of ExcelController
     */
    public ExcelController() {
    }
    public StreamedContent createExcelForBundle(ReportTemplateRowBundle rootBundle) throws IOException{
        return createExcelForBundle(rootBundle,searchController.getFromDate(),searchController.getToDate());
    }
    public StreamedContent createExcelForBundle(ReportTemplateRowBundle rootBundle, Date fromDate, Date toDate) throws IOException {
        if (rootBundle == null) {
            return null;
        }
        StreamedContent excelSc;

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet dataSheet = workbook.createSheet(rootBundle.getName());

        // Create cell styles for headers
        CellStyle centerBoldStyle = workbook.createCellStyle();
        centerBoldStyle.setAlignment(HorizontalAlignment.CENTER);
        centerBoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 14);
        centerBoldStyle.setFont(boldFont);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font normalFont = workbook.createFont();
        normalFont.setFontHeightInPoints((short) 12);
        centerStyle.setFont(normalFont);

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a");

        int currentRow = 0;

        // Row 0: Institution Name
        Row institutionRow = dataSheet.createRow(currentRow++);
        Cell institutionCell = institutionRow.createCell(0);
        String institutionName = sessionController.getInstitution() != null
                ? sessionController.getInstitution().getName()
                : "Institution";
        institutionCell.setCellValue(institutionName);
        institutionCell.setCellStyle(centerBoldStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // Row 1: Report Title (dynamic based on bundle name)
        Row reportTitleRow = dataSheet.createRow(currentRow++);
        Cell reportTitleCell = reportTitleRow.createCell(0);
        String reportTitle = "Cashier Report";
        if (rootBundle.getName() != null) {
            if (rootBundle.getName().toLowerCase().contains("summary")) {
                reportTitle = "Cashier Summary Report";
            } else if (rootBundle.getName().toLowerCase().contains("detail")) {
                reportTitle = "Cashier Details Report";
            }
        }
        reportTitleCell.setCellValue(reportTitle);
        reportTitleCell.setCellStyle(centerStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        // Row 2: Date Range
        Row dateRangeRow = dataSheet.createRow(currentRow++);
        Cell dateRangeCell = dateRangeRow.createCell(0);
        String dateRangeText = "From Date - " + dateTimeFormat.format(fromDate)
                + "     To Date - " + dateTimeFormat.format(toDate);
        dateRangeCell.setCellValue(dateRangeText);
        dateRangeCell.setCellStyle(centerStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 6));

        // Leave row 3 blank
        currentRow++;

        if (rootBundle.getBundles() == null || rootBundle.getBundles().isEmpty()) {
            currentRow = addDataToExcel(dataSheet, currentRow, rootBundle, rootBundle.getBundleType());
        } else {
            // Process all child bundles EXCEPT netCash
            for (ReportTemplateRowBundle childBundle : rootBundle.getBundles()) {
                if (childBundle.getBundleType() != null && !childBundle.getBundleType().equals("netCash")) {
                    currentRow = addDataToExcel(dataSheet, currentRow, childBundle, childBundle.getBundleType());
                    currentRow++;
                }
            }

            // Add Net Collection summary at the end using the ROOT bundle
            currentRow = addDataToExcelForTitleBundle(dataSheet, currentRow, rootBundle);
        }

        // Write the output to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Set the downloading file with a meaningful filename
        String filename = generateExcelFilename(rootBundle.getName(), fromDate, toDate);
        excelSc = DefaultStreamedContent.builder()
                .name(filename)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .stream(() -> inputStream)
                .build();

        return excelSc;
    }

    /**
     * Generates a meaningful filename for Excel export
     * Format: ReportName_FromDate_to_ToDate.xlsx
     * Example: CashierSummary_2024-01-01_to_2024-01-31.xlsx
     */
    private String generateExcelFilename(String bundleName, Date fromDate, Date toDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Clean up the bundle name by removing UUID patterns
        String cleanName = bundleName;
        if (bundleName != null) {
            // Remove UUID pattern (8-4-4-4-12 hex digits)
            cleanName = bundleName.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "").trim();
            // Remove any trailing/leading spaces or special characters
            cleanName = cleanName.replaceAll("[\\s]+", "_");
            // Remove trailing underscores
            cleanName = cleanName.replaceAll("_+$", "");
        }

        // If clean name is empty, use a default
        if (cleanName == null || cleanName.trim().isEmpty()) {
            cleanName = "Report";
        }

        // Format the dates
        String fromDateStr = fromDate != null ? dateFormat.format(fromDate) : "NoStartDate";
        String toDateStr = toDate != null ? dateFormat.format(toDate) : "NoEndDate";

        // Construct the filename
        return cleanName + "_" + fromDateStr + "_to_" + toDateStr + ".xlsx";
    }

    private int addDataToExcel(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle, String type) {
        if (type == null || type.isEmpty()) {
            type = "BillList";
        }
        switch (type) {
            case "opdServiceCollection":
                return addDataToExcelForItemSummaryGroupedByCategory(dataSheet, startRow, addingBundle);
            case "pharmacyCollection":
                return addDataToExcelForDepartmentCollection(dataSheet, startRow, addingBundle);
            case "ccCollection":
                return addDataToExcelForCcDeposits(dataSheet, startRow, addingBundle);
            case "companyPaymentBillOpd":
            case "companyPaymentBillInward":
            case "companyPaymentBillPharmacy":
            case "companyPaymentBillChannelling":
                return addDataToExcelForCompanyCollection(dataSheet, startRow, addingBundle);
            case "patientDepositPayments":
                return addDataToExcelForPatientDeposits(dataSheet, startRow, addingBundle);
            case "collectionForTheDay":
                return addDataToExcelForTitleBundle(dataSheet, startRow, addingBundle);
            case "netCash":
                // Net Cash is handled separately at the end of createExcelForBundle using root bundle
                return startRow;
            case "pettyCashPayments":
                return addDataToExcelForPettyCashPayments(dataSheet, startRow, addingBundle);
            case "ProfessionalPaymentBillReportOpd":
            case "ProfessionalPaymentBillReportChannelling":
            case "ProfessionalPaymentBillReportInward":
                return addDataToExcelForProfessionalPayments(dataSheet, startRow, addingBundle);
            case "paymentReportCards":
                return addDataToExcelForCreditCards(dataSheet, startRow, addingBundle);
            case "paymentReportStaffWelfare":
            case "paymentReportVoucher":
            case "paymentReportCheque":
            case "paymentReportEwallet":
            case "paymentReportSlip":
                //TODO: Change per the payment method
                return addDataToExcelForCreditCards(dataSheet, startRow, addingBundle);
            case "opdServiceCollectionCredit":
                return addDataToExcelForCreditItemSummaryGroupedByCategory(dataSheet, startRow, addingBundle);
            case "netCashPlusCredit":
                return addDataToExcelForNetCashPlusCreditBundle(dataSheet, startRow, addingBundle);
            case "opdServiceBilled":
            case "opdServiceCancellations":
            case "opdServiceRefunds":
            case "cashierSummaryOpdCredit":
            case "opdServiceCancellationsCredit":
            case "opdServiceRefundsCredit":
            case "pharmacyNonCreditBills":
            case "ProfessionalPaymentsOPDCancel":
            case "ProfessionalPaymentsInward":
            case "ProfessionalPaymentsInwardCancel":
            case "PettyCashPayment":
            case "PettyCashPaymentCancel":
            case "InwardPayments":
            case "InwardPaymentsCancel":
            case "InwardPaymentsRefund":
            case "OpdCredit":
            case "OpdCreditCancelled":
            case "OpdCreditRefund":
                return addDataToExcelForopdServiceBilled(dataSheet, startRow, addingBundle);
            case "pharmacyServiceCancellations":
            case "pharmacyServiceRefunds":
                return addDataToExcelForpharmacyServiceCancellations(dataSheet, startRow, addingBundle);
            case "ProfessionalPaymentsOPD":
                return addDataToExcelForProfessionalPaymentsOPD(dataSheet, startRow, addingBundle);
            case "CreditCompanyPaymentOPReceive":
                return addDataToExcelForCreditCompanyPaymentOPReceive(dataSheet, startRow, addingBundle);
            case "CreditCompanyPaymentOPCancel":
            case "CreditCompanyPaymentIPReceive":
            case "CreditCompanyPaymentIPCancellation":
            case "PatientDeposit":
            case "PatientDepositCancel":
            case "PatientDepositRefund":
            case "PharmacyCreditBills":
            case "PharmacyCreditCancel":
            case "PharmacyCreditRefund":
            case "AgencyDeposit":
            case "ChannelBookingsWithPayment":
                return addDataToExcelForCreditCompanyPaymentOPCancel(dataSheet, startRow, addingBundle);
            case "CollectingCentrePaymentReceive":
            case "CollectingCentrePaymentCancel":
                return addDataToExcelForCollectingCentrePaymentReceive(dataSheet, startRow, addingBundle);
            case "ChannelBookingsCancellation":
                return addDataToExcelForChannelBookingsCancellation(dataSheet, startRow, addingBundle);
            case "cashierSummaryOpd":
            case "OutpatientCreditSettling":
            case "OutpatientCreditSettlingCancel":
            case "OutpatientCreditSettlingAdjustments":
                return addDataToExcelForcashierSummaryOpd(dataSheet, startRow, addingBundle);
        }
        return startRow++;
    }

    private int addDataToExcelForcashierSummaryOpd(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with bundle name
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());

            // Calculate total columns needed for bill-level details
            int totalColumns = 6; // Serial, Date & Time, Bill No, Bill Type, Patient, Net Total
            if (addingBundle.isHasCashTransaction()) totalColumns++;
            if (addingBundle.isHasCardTransaction()) totalColumns++;
            if (addingBundle.isHasMultiplePaymentMethodsTransaction()) totalColumns++;
            if (addingBundle.isHasCreditTransaction()) totalColumns++;
            if (addingBundle.isHasStaffWelfareTransaction()) totalColumns++;
            if (addingBundle.isHasStaffTransaction()) totalColumns++;
            if (addingBundle.isHasVoucherTransaction()) totalColumns++;
            if (addingBundle.isHasIouTransaction()) totalColumns++;
            if (addingBundle.isHasAgentTransaction()) totalColumns++;
            if (addingBundle.isHasChequeTransaction()) totalColumns++;
            if (addingBundle.isHasSlipTransaction()) totalColumns++;
            if (addingBundle.isHasEWalletTransaction()) totalColumns++;
            if (addingBundle.isHasPatientDepositTransaction()) totalColumns++;
            if (addingBundle.isHasPatientPointsTransaction()) totalColumns++;
            if (addingBundle.isHasOnCallTransaction()) totalColumns++;

            // Merge title across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, totalColumns - 1));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);
            int colIndex = 0;

            // Base columns that are always present (bill-level details)
            headerRow.createCell(colIndex++).setCellValue("Serial");
            headerRow.createCell(colIndex++).setCellValue("Date & Time");
            headerRow.createCell(colIndex++).setCellValue("Bill No");
            headerRow.createCell(colIndex++).setCellValue("Bill Type");
            headerRow.createCell(colIndex++).setCellValue("Patient");
            headerRow.createCell(colIndex++).setCellValue("Net Total");

            // Add conditional headers based on bundle properties
            if (addingBundle.isHasCashTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cash");
            }
            if (addingBundle.isHasCardTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Card");
            }
            if (addingBundle.isHasMultiplePaymentMethodsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Multiple Payment Methods");
            }
            if (addingBundle.isHasCreditTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Credit");
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff Welfare");
            }
            if (addingBundle.isHasStaffTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff");
            }
            if (addingBundle.isHasVoucherTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Voucher");
            }
            if (addingBundle.isHasIouTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("IOU");
            }
            if (addingBundle.isHasAgentTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Agent");
            }
            if (addingBundle.isHasChequeTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cheque");
            }
            if (addingBundle.isHasSlipTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Slip");
            }
            if (addingBundle.isHasEWalletTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("eWallet");
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Deposit");
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Points");
            }
            if (addingBundle.isHasOnCallTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Online Settlement");
            }

            // Create date-time format for Excel
            CellStyle dateTimeStyle = dataSheet.getWorkbook().createCellStyle();
            CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
            dateTimeStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-mmm-yy hh:mm"));

            // Create number format for currency values
            CellStyle currencyStyle = dataSheet.getWorkbook().createCellStyle();
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Serial Number
                excelRow.createCell(colIndex++).setCellValue(serialNumber++);

                // Date & Time (from bill.createdAt)
                if (row.getBill() != null && row.getBill().getCreatedAt() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getBill().getCreatedAt());
                    dateCell.setCellStyle(dateTimeStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill No (from bill.deptId)
                if (row.getBill() != null && row.getBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill Type (from bill.billTypeAtomic)
                if (row.getBill() != null && row.getBill().getBillTypeAtomic() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getBillTypeAtomic().toString());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Patient Name
                if (row.getBill() != null && row.getBill().getPatient() != null &&
                    row.getBill().getPatient().getPerson() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getPatient().getPerson().getNameWithTitle());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Net Total (from bill.netTotal)
                if (row.getBill() != null) {
                    Cell netTotalCell = excelRow.createCell(colIndex++);
                    netTotalCell.setCellValue(row.getBill().getNetTotal());
                    netTotalCell.setCellStyle(currencyStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue(0.0);
                }

                // Add conditional payment method columns with currency formatting
                if (addingBundle.isHasCashTransaction()) {
                    Cell cashCell = excelRow.createCell(colIndex++);
                    cashCell.setCellValue(row.getCashValue());
                    cashCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasCardTransaction()) {
                    Cell cardCell = excelRow.createCell(colIndex++);
                    cardCell.setCellValue(row.getCardValue());
                    cardCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasMultiplePaymentMethodsTransaction()) {
                    Cell multipleCell = excelRow.createCell(colIndex++);
                    multipleCell.setCellValue(row.getMultiplePaymentMethodsValue());
                    multipleCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasCreditTransaction()) {
                    Cell creditCell = excelRow.createCell(colIndex++);
                    creditCell.setCellValue(row.getCreditValue());
                    creditCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasStaffWelfareTransaction()) {
                    Cell staffWelfareCell = excelRow.createCell(colIndex++);
                    staffWelfareCell.setCellValue(row.getStaffWelfareValue());
                    staffWelfareCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasStaffTransaction()) {
                    Cell staffCell = excelRow.createCell(colIndex++);
                    staffCell.setCellValue(row.getStaffValue());
                    staffCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasVoucherTransaction()) {
                    Cell voucherCell = excelRow.createCell(colIndex++);
                    voucherCell.setCellValue(row.getVoucherValue());
                    voucherCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasIouTransaction()) {
                    Cell iouCell = excelRow.createCell(colIndex++);
                    iouCell.setCellValue(row.getIouValue());
                    iouCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasAgentTransaction()) {
                    Cell agentCell = excelRow.createCell(colIndex++);
                    agentCell.setCellValue(row.getAgentValue());
                    agentCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasChequeTransaction()) {
                    Cell chequeCell = excelRow.createCell(colIndex++);
                    chequeCell.setCellValue(row.getChequeValue());
                    chequeCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasSlipTransaction()) {
                    Cell slipCell = excelRow.createCell(colIndex++);
                    slipCell.setCellValue(row.getSlipValue());
                    slipCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasEWalletTransaction()) {
                    Cell ewalletCell = excelRow.createCell(colIndex++);
                    ewalletCell.setCellValue(row.getEwalletValue());
                    ewalletCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasPatientDepositTransaction()) {
                    Cell depositCell = excelRow.createCell(colIndex++);
                    depositCell.setCellValue(row.getPatientDepositValue());
                    depositCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasPatientPointsTransaction()) {
                    Cell pointsCell = excelRow.createCell(colIndex++);
                    pointsCell.setCellValue(row.getPatientPointsValue());
                    pointsCell.setCellStyle(currencyStyle);
                }
                if (addingBundle.isHasOnCallTransaction()) {
                    Cell onlineCell = excelRow.createCell(colIndex++);
                    onlineCell.setCellValue(row.getOnlineSettlementValue());
                    onlineCell.setCellStyle(currencyStyle);
                }
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Serial, Date & Time, Bill No, Bill Type, Patient
            for (int i = 0; i < 5; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }

            // Net Total footer
            Cell netTotalFooterCell = footerRow.createCell(colIndex++);
            netTotalFooterCell.setCellValue(addingBundle.getTotal());
            netTotalFooterCell.setCellStyle(currencyStyle);

            // Add conditional payment method totals with currency formatting
            if (addingBundle.isHasCashTransaction()) {
                Cell cashTotalCell = footerRow.createCell(colIndex++);
                cashTotalCell.setCellValue(addingBundle.getCashValue());
                cashTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasCardTransaction()) {
                Cell cardTotalCell = footerRow.createCell(colIndex++);
                cardTotalCell.setCellValue(addingBundle.getCardValue());
                cardTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasMultiplePaymentMethodsTransaction()) {
                Cell multipleTotalCell = footerRow.createCell(colIndex++);
                multipleTotalCell.setCellValue(addingBundle.getMultiplePaymentMethodsValue());
                multipleTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasCreditTransaction()) {
                Cell creditTotalCell = footerRow.createCell(colIndex++);
                creditTotalCell.setCellValue(addingBundle.getCreditValue());
                creditTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                Cell staffWelfareTotalCell = footerRow.createCell(colIndex++);
                staffWelfareTotalCell.setCellValue(addingBundle.getStaffWelfareValue());
                staffWelfareTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasStaffTransaction()) {
                Cell staffTotalCell = footerRow.createCell(colIndex++);
                staffTotalCell.setCellValue(addingBundle.getStaffValue());
                staffTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasVoucherTransaction()) {
                Cell voucherTotalCell = footerRow.createCell(colIndex++);
                voucherTotalCell.setCellValue(addingBundle.getVoucherValue());
                voucherTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasIouTransaction()) {
                Cell iouTotalCell = footerRow.createCell(colIndex++);
                iouTotalCell.setCellValue(addingBundle.getIouValue());
                iouTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasAgentTransaction()) {
                Cell agentTotalCell = footerRow.createCell(colIndex++);
                agentTotalCell.setCellValue(addingBundle.getAgentValue());
                agentTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasChequeTransaction()) {
                Cell chequeTotalCell = footerRow.createCell(colIndex++);
                chequeTotalCell.setCellValue(addingBundle.getChequeValue());
                chequeTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasSlipTransaction()) {
                Cell slipTotalCell = footerRow.createCell(colIndex++);
                slipTotalCell.setCellValue(addingBundle.getSlipValue());
                slipTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasEWalletTransaction()) {
                Cell ewalletTotalCell = footerRow.createCell(colIndex++);
                ewalletTotalCell.setCellValue(addingBundle.getEwalletValue());
                ewalletTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                Cell depositTotalCell = footerRow.createCell(colIndex++);
                depositTotalCell.setCellValue(addingBundle.getPatientDepositValue());
                depositTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                Cell pointsTotalCell = footerRow.createCell(colIndex++);
                pointsTotalCell.setCellValue(addingBundle.getPatientPointsValue());
                pointsTotalCell.setCellStyle(currencyStyle);
            }
            if (addingBundle.isHasOnCallTransaction()) {
                Cell onlineTotalCell = footerRow.createCell(colIndex++);
                onlineTotalCell.setCellValue(addingBundle.getOnlineSettlementValue());
                onlineTotalCell.setCellStyle(currencyStyle);
            }

            // Auto-size columns for better readability
            for (int i = 0; i < totalColumns; i++) {
                dataSheet.autoSizeColumn(i);
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across multiple columns
            dataSheet.addMergedRegion(new CellRangeAddress(
            startRow - 1,
            startRow - 1,
            0,
            10));
        }

        return startRow;
    }

    private int addDataToExcelForChannelBookingsCancellation(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with bundle name
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());

            // Merge title across all columns (adjust column count as needed)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 20));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);

            // Base columns - Serial, Date & Time, Bill No, Original Bill No are always present
            String[] baseHeaders = {"Serial", "Date & Time", "Bill No", "Original Bill No"};
            int colIndex = 0;

            // Add base headers
            for (String header : baseHeaders) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
            }



            // Add Patient and Net Total columns
            headerRow.createCell(colIndex++).setCellValue("Patient");
            headerRow.createCell(colIndex++).setCellValue("Net Total");

            // Add conditional headers based on bundle properties (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cash");
            }
            if (addingBundle.isHasCardTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Card");
            }
            if (addingBundle.isHasCreditTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Credit");
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff Welfare");
            }
            if (addingBundle.isHasVoucherTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Voucher");
            }
            if (addingBundle.isHasIouTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("IOU");
            }
            if (addingBundle.isHasAgentTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Agent");
            }
            if (addingBundle.isHasChequeTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cheque");
            }
            if (addingBundle.isHasSlipTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Slip");
            }
            if (addingBundle.isHasEWalletTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("eWallet");
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Deposit");
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Points");
            }
            if (addingBundle.isHasOnlineSettlementTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Online Settlement");
            }

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Serial number
                excelRow.createCell(colIndex++).setCellValue(serialNumber++);

                // Date & Time
                if (row.getBill() != null && row.getBill().getCreatedAt() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getBill().getCreatedAt());
                    CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
                        dateStyle.setDataFormat(dataSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
                        dateCell.setCellStyle(dateStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill No
                if (row.getBill() != null && row.getBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Original Bill No
                if (row.getBill() != null && row.getBill().getBilledBill() != null &&
                        row.getBill().getBilledBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getBilledBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }


                // Patient
                if (row.getBill() != null && row.getBill().getPatient() != null &&
                        row.getBill().getPatient().getPerson() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getPatient().getPerson().getNameWithTitle());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Net Total
                if (row.getBill() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getNetTotal());
                } else {
                    excelRow.createCell(colIndex++).setCellValue(0.0);
                }

                // Add conditional payment method columns (matching XHTML order)
                if (addingBundle.isHasCashTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCashValue());
                }
                if (addingBundle.isHasCardTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCardValue());
                }
                if (addingBundle.isHasCreditTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCreditValue());
                }
                if (addingBundle.isHasStaffWelfareTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getStaffWelfareValue());
                }
                if (addingBundle.isHasVoucherTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getVoucherValue());
                }
                if (addingBundle.isHasIouTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getIouValue());
                }
                if (addingBundle.isHasAgentTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getAgentValue());
                }
                if (addingBundle.isHasChequeTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getChequeValue());
                }
                if (addingBundle.isHasSlipTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getSlipValue());
                }
                if (addingBundle.isHasEWalletTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getEwalletValue());
                }
                if (addingBundle.isHasPatientDepositTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientDepositValue());
                }
                if (addingBundle.isHasPatientPointsTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientPointsValue());
                }
                if (addingBundle.isHasOnlineSettlementTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getOnlineSettlementValue());
                }
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Serial, Date, Bill No, Original Bill No
            for (int i = 0; i < 4; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }



            // Empty cell for Patient
            footerRow.createCell(colIndex++).setCellValue("");

            // Net Total footer
            footerRow.createCell(colIndex++).setCellValue(addingBundle.getTotal());

            // Add conditional payment method totals (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCashValue());
            }
            if (addingBundle.isHasCardTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCardValue());
            }
            if (addingBundle.isHasCreditTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCreditValue());
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getStaffWelfareValue());
            }
            if (addingBundle.isHasVoucherTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getVoucherValue());
            }
            if (addingBundle.isHasIouTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getIouValue());
            }
            if (addingBundle.isHasAgentTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getAgentValue());
            }
            if (addingBundle.isHasChequeTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getChequeValue());
            }
            if (addingBundle.isHasSlipTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getSlipValue());
            }
            if (addingBundle.isHasEWalletTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getEwalletValue());
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientDepositValue());
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientPointsValue());
            }
            if (addingBundle.isHasOnlineSettlementTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getOnlineSettlementValue());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across multiple columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 10));
        }

        return startRow;
    }

    private int addDataToExcelForCollectingCentrePaymentReceive(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with bundle name
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());

            // Merge title across all columns (adjust column count as needed)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 18));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);

            // Base columns that are always present (matching XHTML template)
            String[] baseHeaders = {"Serial", "Date & Time", "Bill No", "Bill Type", "Collecting Center", "Net Total"};
            int colIndex = 0;

            // Add base headers
            for (String header : baseHeaders) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
            }

            // Add conditional headers based on bundle properties (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cash");
            }
            if (addingBundle.isHasCardTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Card");
            }
            if (addingBundle.isHasCreditTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Credit");
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff Welfare");
            }
            if (addingBundle.isHasVoucherTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Voucher");
            }
            if (addingBundle.isHasIouTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("IOU");
            }
            if (addingBundle.isHasAgentTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Agent");
            }
            if (addingBundle.isHasChequeTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cheque");
            }
            if (addingBundle.isHasSlipTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Slip");
            }
            if (addingBundle.isHasEWalletTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("eWallet");
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Deposit");
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Points");
            }
            if (addingBundle.isHasOnCallTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Online Settlement");
            }

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Serial number
                excelRow.createCell(colIndex++).setCellValue(serialNumber++);

                // Date & Time
                if (row.getBill() != null && row.getBill().getCreatedAt() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getBill().getCreatedAt());
                    CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
                    dateStyle.setDataFormat(dataSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill No
                if (row.getBill() != null && row.getBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill Type
                if (row.getBill() != null && row.getBill().getBillTypeAtomic() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getBillTypeAtomic().toString());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Collecting Center
                if (row.getBill() != null && row.getBill().getFromInstitution() != null) {
                    String code = row.getBill().getFromInstitution().getCode();
                    String name = row.getBill().getFromInstitution().getName();
                    String value = (code != null ? code : "") + " - " + (name != null ? name : "");
                    excelRow.createCell(colIndex++).setCellValue(value);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }


                // Net Total
                if (row.getBill() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getNetTotal());
                } else {
                    excelRow.createCell(colIndex++).setCellValue(0.0);
                }

                // Add conditional payment method columns (matching XHTML order)
                if (addingBundle.isHasCashTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCashValue());
                }
                if (addingBundle.isHasCardTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCardValue());
                }
                if (addingBundle.isHasCreditTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCreditValue());
                }
                if (addingBundle.isHasStaffWelfareTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getStaffWelfareValue());
                }
                if (addingBundle.isHasVoucherTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getVoucherValue());
                }
                if (addingBundle.isHasIouTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getIouValue());
                }
                if (addingBundle.isHasAgentTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getAgentValue());
                }
                if (addingBundle.isHasChequeTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getChequeValue());
                }
                if (addingBundle.isHasSlipTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getSlipValue());
                }
                if (addingBundle.isHasEWalletTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getEwalletValue());
                }
                if (addingBundle.isHasPatientDepositTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientDepositValue());
                }
                if (addingBundle.isHasPatientPointsTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientPointsValue());
                }
                if (addingBundle.isHasOnCallTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getOnlineSettlementValue());
                }
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Serial, Date, Bill No, Bill Type, Patient
            for (int i = 0; i < 5; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }

            // Net Total footer
            footerRow.createCell(colIndex++).setCellValue(addingBundle.getTotal());

            // Add conditional payment method totals (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCashValue());
            }
            if (addingBundle.isHasCardTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCardValue());
            }
            if (addingBundle.isHasCreditTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCreditValue());
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getStaffWelfareValue());
            }
            if (addingBundle.isHasVoucherTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getVoucherValue());
            }
            if (addingBundle.isHasIouTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getIouValue());
            }
            if (addingBundle.isHasAgentTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getAgentValue());
            }
            if (addingBundle.isHasChequeTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getChequeValue());
            }
            if (addingBundle.isHasSlipTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getSlipValue());
            }
            if (addingBundle.isHasEWalletTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getEwalletValue());
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientDepositValue());
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientPointsValue());
            }
            if (addingBundle.isHasOnCallTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getOnlineSettlementValue());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across multiple columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 10));
        }

        return startRow;
    }

    private int addDataToExcelForCreditCompanyPaymentOPCancel(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with bundle name
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());

            // Merge title across all columns (adjust column count as needed)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 18));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);

            // Base columns that are always present (matching XHTML template)
            String[] baseHeaders = {"Serial", "Date & Time", "Bill No", "Bill Type", "Patient", "Net Total"};
            int colIndex = 0;

            // Add base headers
            for (String header : baseHeaders) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
            }

            // Add conditional headers based on bundle properties (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cash");
            }
            if (addingBundle.isHasCardTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Card");
            }
            if (addingBundle.isHasCreditTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Credit");
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff Welfare");
            }
            if (addingBundle.isHasVoucherTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Voucher");
            }
            if (addingBundle.isHasIouTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("IOU");
            }
            if (addingBundle.isHasAgentTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Agent");
            }
            if (addingBundle.isHasChequeTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cheque");
            }
            if (addingBundle.isHasSlipTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Slip");
            }
            if (addingBundle.isHasEWalletTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("eWallet");
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Deposit");
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Points");
            }
            if (addingBundle.isHasOnCallTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Online Settlement");
            }

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Serial number
                excelRow.createCell(colIndex++).setCellValue(serialNumber++);

                // Date & Time
                if (row.getBill() != null && row.getBill().getCreatedAt() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getBill().getCreatedAt());
                    CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
                    dateStyle.setDataFormat(dataSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill No
                if (row.getBill() != null && row.getBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill Type
                if (row.getBill() != null && row.getBill().getBillTypeAtomic() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getBillTypeAtomic().toString());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Patient
                if (row.getBill() != null && row.getBill().getPatient() != null &&
                        row.getBill().getPatient().getPerson() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getPatient().getPerson().getNameWithTitle());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Net Total
                if (row.getBill() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getNetTotal());
                } else {
                    excelRow.createCell(colIndex++).setCellValue(0.0);
                }

                // Add conditional payment method columns (matching XHTML order)
                if (addingBundle.isHasCashTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCashValue());
                }
                if (addingBundle.isHasCardTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCardValue());
                }
                if (addingBundle.isHasCreditTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCreditValue());
                }
                if (addingBundle.isHasStaffWelfareTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getStaffWelfareValue());
                }
                if (addingBundle.isHasVoucherTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getVoucherValue());
                }
                if (addingBundle.isHasIouTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getIouValue());
                }
                if (addingBundle.isHasAgentTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getAgentValue());
                }
                if (addingBundle.isHasChequeTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getChequeValue());
                }
                if (addingBundle.isHasSlipTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getSlipValue());
                }
                if (addingBundle.isHasEWalletTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getEwalletValue());
                }
                if (addingBundle.isHasPatientDepositTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientDepositValue());
                }
                if (addingBundle.isHasPatientPointsTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientPointsValue());
                }
                if (addingBundle.isHasOnCallTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getOnlineSettlementValue());
                }
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Serial, Date, Bill No, Bill Type, Patient
            for (int i = 0; i < 5; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }

            // Net Total footer
            footerRow.createCell(colIndex++).setCellValue(addingBundle.getTotal());

            // Add conditional payment method totals (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCashValue());
            }
            if (addingBundle.isHasCardTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCardValue());
            }
            if (addingBundle.isHasCreditTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCreditValue());
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getStaffWelfareValue());
            }
            if (addingBundle.isHasVoucherTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getVoucherValue());
            }
            if (addingBundle.isHasIouTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getIouValue());
            }
            if (addingBundle.isHasAgentTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getAgentValue());
            }
            if (addingBundle.isHasChequeTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getChequeValue());
            }
            if (addingBundle.isHasSlipTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getSlipValue());
            }
            if (addingBundle.isHasEWalletTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getEwalletValue());
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientDepositValue());
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientPointsValue());
            }
            if (addingBundle.isHasOnCallTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getOnlineSettlementValue());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across multiple columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 10));
        }

        return startRow;
    }

    private int addDataToExcelForCreditCompanyPaymentOPReceive(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with bundle name
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());

            // Merge title across all columns (adjust column count as needed)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 18));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);

            // Base columns that are always present (matching XHTML template)
            String[] baseHeaders = {"Serial", "Date & Time", "Bill No*", "Bill Type", "Net Total"};
            int colIndex = 0;

            // Add base headers
            for (String header : baseHeaders) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
            }

            // Add conditional headers based on bundle properties (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cash");
            }
            if (addingBundle.isHasCardTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Card");
            }
            if (addingBundle.isHasCreditTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Credit");
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff Welfare");
            }
            if (addingBundle.isHasVoucherTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Voucher");
            }
            if (addingBundle.isHasIouTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("IOU");
            }
            if (addingBundle.isHasAgentTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Agent");
            }
            if (addingBundle.isHasChequeTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cheque");
            }
            if (addingBundle.isHasSlipTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Slip");
            }
            if (addingBundle.isHasEWalletTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("eWallet");
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Deposit");
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Points");
            }
            if (addingBundle.isHasOnCallTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Online Settlement");
            }

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Serial number
                excelRow.createCell(colIndex++).setCellValue(serialNumber++);

                // Date & Time
                if (row.getBill() != null && row.getBill().getCreatedAt() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getBill().getCreatedAt());
                    CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
                    dateStyle.setDataFormat(dataSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill No*
                if (row.getBill() != null && row.getBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill Type
                if (row.getBill() != null && row.getBill().getBillTypeAtomic() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getBillTypeAtomic().toString());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Net Total
                if (row.getBill() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getNetTotal());
                } else {
                    excelRow.createCell(colIndex++).setCellValue(0.0);
                }

                // Add conditional payment method columns (matching XHTML order)
                if (addingBundle.isHasCashTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCashValue());
                }
                if (addingBundle.isHasCardTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCardValue());
                }
                if (addingBundle.isHasCreditTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCreditValue());
                }
                if (addingBundle.isHasStaffWelfareTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getStaffWelfareValue());
                }
                if (addingBundle.isHasVoucherTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getVoucherValue());
                }
                if (addingBundle.isHasIouTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getIouValue());
                }
                if (addingBundle.isHasAgentTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getAgentValue());
                }
                if (addingBundle.isHasChequeTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getChequeValue());
                }
                if (addingBundle.isHasSlipTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getSlipValue());
                }
                if (addingBundle.isHasEWalletTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getEwalletValue());
                }
                if (addingBundle.isHasPatientDepositTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientDepositValue());
                }
                if (addingBundle.isHasPatientPointsTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientPointsValue());
                }
                if (addingBundle.isHasOnCallTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getOnlineSettlementValue());
                }
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Serial, Date, Bill No*, Bill Type
            for (int i = 0; i < 4; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }

            // Net Total footer
            footerRow.createCell(colIndex++).setCellValue(addingBundle.getTotal());

            // Add conditional payment method totals (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCashValue());
            }
            if (addingBundle.isHasCardTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCardValue());
            }
            if (addingBundle.isHasCreditTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCreditValue());
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getStaffWelfareValue());
            }
            if (addingBundle.isHasVoucherTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getVoucherValue());
            }
            if (addingBundle.isHasIouTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getIouValue());
            }
            if (addingBundle.isHasAgentTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getAgentValue());
            }
            if (addingBundle.isHasChequeTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getChequeValue());
            }
            if (addingBundle.isHasSlipTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getSlipValue());
            }
            if (addingBundle.isHasEWalletTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getEwalletValue());
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientDepositValue());
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientPointsValue());
            }
            if (addingBundle.isHasOnCallTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getOnlineSettlementValue());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across multiple columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 10));
        }

        return startRow;
    }

    private int addDataToExcelForProfessionalPaymentsOPD(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with bundle name
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());

            // Merge title across all columns (adjust column count as needed)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 18));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);

            // Base columns that are always present (matching XHTML template)
            String[] baseHeaders = {"Serial", "Date & Time", "Bill No", "Doctor", "Net Total"};
            int colIndex = 0;

            // Add base headers
            for (String header : baseHeaders) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
            }

            // Add conditional headers based on bundle properties (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cash");
            }
            if (addingBundle.isHasCardTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Card");
            }
            if (addingBundle.isHasCreditTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Credit");
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff Welfare");
            }
            if (addingBundle.isHasVoucherTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Voucher");
            }
            if (addingBundle.isHasIouTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("IOU");
            }
            if (addingBundle.isHasAgentTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Agent");
            }
            if (addingBundle.isHasChequeTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cheque");
            }
            if (addingBundle.isHasSlipTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Slip");
            }
            if (addingBundle.isHasEWalletTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("eWallet");
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Deposit");
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Points");
            }
            if (addingBundle.isHasOnCallTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Online Settlement");
            }

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Serial number
                excelRow.createCell(colIndex++).setCellValue(serialNumber++);

                // Date & Time
                if (row.getBill() != null && row.getBill().getCreatedAt() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getBill().getCreatedAt());
                    CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
                    dateStyle.setDataFormat(dataSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill No
                if (row.getBill() != null && row.getBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Doctor (instead of Patient, and instead of Bill Type which is hidden)
                if (row.getBill() != null && row.getBill().getStaff() != null &&
                        row.getBill().getStaff().getPerson() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getStaff().getPerson().getNameWithTitle());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Net Total
                if (row.getBill() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getNetTotal());
                } else {
                    excelRow.createCell(colIndex++).setCellValue(0.0);
                }

                // Add conditional payment method columns (matching XHTML order)
                if (addingBundle.isHasCashTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCashValue());
                }
                if (addingBundle.isHasCardTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCardValue());
                }
                if (addingBundle.isHasCreditTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCreditValue());
                }
                if (addingBundle.isHasStaffWelfareTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getStaffWelfareValue());
                }
                if (addingBundle.isHasVoucherTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getVoucherValue());
                }
                if (addingBundle.isHasIouTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getIouValue());
                }
                if (addingBundle.isHasAgentTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getAgentValue());
                }
                if (addingBundle.isHasChequeTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getChequeValue());
                }
                if (addingBundle.isHasSlipTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getSlipValue());
                }
                if (addingBundle.isHasEWalletTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getEwalletValue());
                }
                if (addingBundle.isHasPatientDepositTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientDepositValue());
                }
                if (addingBundle.isHasPatientPointsTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientPointsValue());
                }
                if (addingBundle.isHasOnCallTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getOnlineSettlementValue());
                }
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Serial, Date, Bill No, Doctor
            for (int i = 0; i < 4; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }

            // Net Total footer
            footerRow.createCell(colIndex++).setCellValue(addingBundle.getTotal());

            // Add conditional payment method totals (matching XHTML order)
            if (addingBundle.isHasCashTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCashValue());
            }
            if (addingBundle.isHasCardTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCardValue());
            }
            if (addingBundle.isHasCreditTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCreditValue());
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getStaffWelfareValue());
            }
            if (addingBundle.isHasVoucherTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getVoucherValue());
            }
            if (addingBundle.isHasIouTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getIouValue());
            }
            if (addingBundle.isHasAgentTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getAgentValue());
            }
            if (addingBundle.isHasChequeTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getChequeValue());
            }
            if (addingBundle.isHasSlipTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getSlipValue());
            }
            if (addingBundle.isHasEWalletTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getEwalletValue());
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientDepositValue());
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientPointsValue());
            }
            if (addingBundle.isHasOnCallTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getOnlineSettlementValue());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across multiple columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 10));
        }

        return startRow;
    }

    private int addDataToExcelForpharmacyServiceCancellations(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(5); // Assuming 6th column is for total
            totalCell.setCellValue(addingBundle.getTotal()); // Assuming getTotal() returns formatted string

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 4));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {
                    "Serial", "Date / Time", "Bill No", "Bill Type",
                    "Patient", "Net Total"
            };
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnHeaders[i]);
            }

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;


            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                String categoryOrItem = row.getCategory() != null ? row.getCategory().getName() : "";
                String itemName = row.getItem() != null ? row.getItem().getName() : "";

                // Serial number
                excelRow.createCell(0).setCellValue(serialNumber++);
                Cell dateCell = excelRow.createCell(1);
                dateCell.setCellValue(row.getBill().getCreatedAt());
                CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
                dateStyle.setDataFormat(dataSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
                dateCell.setCellStyle(dateStyle);
                excelRow.createCell(2).setCellValue(row.getBill().getInsId());
                excelRow.createCell(3).setCellValue(row.getBill().getBillTypeAtomic().toString());
                excelRow.createCell(4).setCellValue(row.getBill().getPatient().getPerson().getNameWithTitle());
                excelRow.createCell(5).setCellValue(row.getBill().getNetTotal());
            }
            // Add footer row with total (only for Net Total column)
            Row footerRow = dataSheet.createRow(startRow++);

            // Empty cells for Serial, Date, Bill No, Bill Type, Patient
            for (int i = 0; i < 5; i++) {
                footerRow.createCell(i).setCellValue("");
            }

            // Net Total footer
            footerRow.createCell(5).setCellValue(addingBundle.getTotal());


        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 6));
        }

        return startRow;
    }

    private int addDataToExcelForopdServiceBilled(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with bundle name
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());

            // Merge title across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 18));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);

            // Base columns that are always present
            String[] baseHeaders = {"Serial", "Date & Time", "Bill No", "Bill Type", "Patient", "Net Total"};
            int colIndex = 0;

            // Add base headers
            for (String header : baseHeaders) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
            }

            // Add conditional headers based on bundle properties
            if (addingBundle.isHasCashTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cash");
            }
            if (addingBundle.isHasCardTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Card");
            }
            if (addingBundle.isHasCreditTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Credit");
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Staff Welfare");
            }
            if (addingBundle.isHasVoucherTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Voucher");
            }
            if (addingBundle.isHasIouTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("IOU");
            }
            if (addingBundle.isHasAgentTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Agent");
            }
            if (addingBundle.isHasChequeTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Cheque");
            }
            if (addingBundle.isHasSlipTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Slip");
            }
            if (addingBundle.isHasEWalletTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("eWallet");
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Deposit");
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Patient Points");
            }
            if (addingBundle.isHasOnCallTransaction()) {
                headerRow.createCell(colIndex++).setCellValue("Online Settlement");
            }

            // Iterate through each row of the data and add to Excel
            int serialNumber = 1;
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Serial number
                excelRow.createCell(colIndex++).setCellValue(serialNumber++);

                // Date & Time
                if (row.getBill() != null && row.getBill().getCreatedAt() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getBill().getCreatedAt());
                    CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
                    dateStyle.setDataFormat(dataSheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill No
                if (row.getBill() != null && row.getBill().getDeptId() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getDeptId());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Bill Type
                if (row.getBill() != null && row.getBill().getBillTypeAtomic() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getBillTypeAtomic().toString());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Patient
                if (row.getBill() != null && row.getBill().getPatient() != null &&
                        row.getBill().getPatient().getPerson() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getPatient().getPerson().getNameWithTitle());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Net Total
                if (row.getBill() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getBill().getNetTotal());
                } else {
                    excelRow.createCell(colIndex++).setCellValue(0.0);
                }

                // Add conditional payment method columns
                if (addingBundle.isHasCashTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCashValue());
                }
                if (addingBundle.isHasCardTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCardValue());
                }
                if (addingBundle.isHasCreditTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getCreditValue());
                }
                if (addingBundle.isHasStaffWelfareTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getStaffWelfareValue());
                }
                if (addingBundle.isHasVoucherTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getVoucherValue());
                }
                if (addingBundle.isHasIouTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getIouValue());
                }
                if (addingBundle.isHasAgentTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getAgentValue());
                }
                if (addingBundle.isHasChequeTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getChequeValue());
                }
                if (addingBundle.isHasSlipTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getSlipValue());
                }
                if (addingBundle.isHasEWalletTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getEwalletValue());
                }
                if (addingBundle.isHasPatientDepositTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientDepositValue());
                }
                if (addingBundle.isHasPatientPointsTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getPatientPointsValue());
                }
                if (addingBundle.isHasOnCallTransaction()) {
                    excelRow.createCell(colIndex++).setCellValue(row.getOnlineSettlementValue());
                }
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Serial, Date, Bill No, Bill Type, Patient
            for (int i = 0; i < 5; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }

            // Net Total footer
            footerRow.createCell(colIndex++).setCellValue(addingBundle.getTotal());

            // Add conditional payment method totals
            if (addingBundle.isHasCashTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCashValue());
            }
            if (addingBundle.isHasCardTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCardValue());
            }
            if (addingBundle.isHasCreditTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getCreditValue());
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getStaffWelfareValue());
            }
            if (addingBundle.isHasVoucherTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getVoucherValue());
            }
            if (addingBundle.isHasIouTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getIouValue());
            }
            if (addingBundle.isHasAgentTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getAgentValue());
            }
            if (addingBundle.isHasChequeTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getChequeValue());
            }
            if (addingBundle.isHasSlipTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getSlipValue());
            }
            if (addingBundle.isHasEWalletTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getEwalletValue());
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientDepositValue());
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getPatientPointsValue());
            }
            if (addingBundle.isHasOnCallTransaction()) {
                footerRow.createCell(colIndex++).setCellValue(addingBundle.getOnlineSettlementValue());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across multiple columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 10));
        }

        return startRow;
    }

    private int addDataToExcelForNetCashPlusCreditBundle(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Row above for visual separation (mimicking <hr/>)
        Row upperSeparatorRow = dataSheet.createRow(startRow++);
        upperSeparatorRow.createCell(0).setCellValue(""); // Empty cell, you can optionally format it as a visual separator
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));

        // Create a title row for the report name and total
        Row titleRow = dataSheet.createRow(startRow++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(addingBundle.getName());  // Bundle name

        Cell totalCell = titleRow.createCell(5); // Total in the sixth column
        totalCell.setCellValue(addingBundle.getTotal());  // Total value
        // Merge cells for the title, leaving the last one for the total
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 4));

        // Row below for visual separation (mimicking <hr/>)
        Row lowerSeparatorRow = dataSheet.createRow(startRow++);
        lowerSeparatorRow.createCell(0).setCellValue(""); // Empty cell, can be formatted as a visual separator
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));

        // Adjust column widths to maintain uniform appearance
        for (int i = 0; i < 6; i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForTitleBundle(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create number format style
        CellStyle numberStyle = dataSheet.getWorkbook().createCellStyle();
        CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
        numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

        // Create bold style for headers
        CellStyle boldStyle = dataSheet.getWorkbook().createCellStyle();
        org.apache.poi.ss.usermodel.Font boldFont = dataSheet.getWorkbook().createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        // Row above for visual separation
        Row upperSeparatorRow = dataSheet.createRow(startRow++);
        upperSeparatorRow.createCell(0).setCellValue("");
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 1));

        // Header row: "Net Collection"
        Row headerRow = dataSheet.createRow(startRow++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Net Collection");
        headerCell.setCellStyle(boldStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 1));

        // Net Collection Total
        Row netCollectionRow = dataSheet.createRow(startRow++);
        netCollectionRow.createCell(0).setCellValue("Net Collection Total");
        Cell netCollectionValueCell = netCollectionRow.createCell(1);
        double netCollectionTotal = searchController.bundleCashierGrandTotal(addingBundle);
        netCollectionValueCell.setCellValue(netCollectionTotal);
        netCollectionValueCell.setCellStyle(numberStyle);

        // "Included in Collection Total" header
        Row includedHeaderRow = dataSheet.createRow(startRow++);
        Cell includedHeaderCell = includedHeaderRow.createCell(0);
        includedHeaderCell.setCellValue("Included in Collection Total");
        includedHeaderCell.setCellStyle(boldStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 1));

        // Calculate aggregated payment method values from all child bundles
        double cashTotal = 0, cardTotal = 0, creditTotal = 0, multipleTotal = 0, staffTotal = 0;
        double staffWelfareTotal = 0, voucherTotal = 0, iouTotal = 0, agentTotal = 0, chequeTotal = 0;
        double slipTotal = 0, ewalletTotal = 0, patientPointsTotal = 0, onlineSettlementTotal = 0;

        if (addingBundle.getBundles() != null && !addingBundle.getBundles().isEmpty()) {
            for (ReportTemplateRowBundle childBundle : addingBundle.getBundles()) {
                cashTotal += childBundle.getCashValue();
                cardTotal += childBundle.getCardValue();
                creditTotal += childBundle.getCreditValue();
                multipleTotal += childBundle.getMultiplePaymentMethodsValue();
                staffTotal += childBundle.getStaffValue();
                staffWelfareTotal += childBundle.getStaffWelfareValue();
                voucherTotal += childBundle.getVoucherValue();
                iouTotal += childBundle.getIouValue();
                agentTotal += childBundle.getAgentValue();
                chequeTotal += childBundle.getChequeValue();
                slipTotal += childBundle.getSlipValue();
                ewalletTotal += childBundle.getEWalletValue();
                patientPointsTotal += childBundle.getPatientPointsValue();
                onlineSettlementTotal += childBundle.getOnlineSettlementValue();
            }
        }

        // Add payment methods included in collection (only if non-zero)
        if (cashTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Cash");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(cashTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (cardTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Credit Card");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(cardTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (creditTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Credit");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(creditTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (multipleTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Multiple Payment Methods");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(multipleTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (staffTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Staff");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(staffTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (staffWelfareTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Staff Welfare");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(staffWelfareTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (voucherTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Voucher");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(voucherTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (iouTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("IOU");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(iouTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (agentTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Agent");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(agentTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (chequeTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Cheque");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(chequeTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (slipTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Slip");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(slipTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (ewalletTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("eWallet");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(ewalletTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (patientPointsTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Patient Points");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(patientPointsTotal);
            valueCell.setCellStyle(numberStyle);
        }
        if (onlineSettlementTotal != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Online Settlement");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(onlineSettlementTotal);
            valueCell.setCellStyle(numberStyle);
        }

        // Collection Total
        Row collectionTotalRow = dataSheet.createRow(startRow++);
        Cell collectionTotalLabelCell = collectionTotalRow.createCell(0);
        collectionTotalLabelCell.setCellValue("Collection Total");
        collectionTotalLabelCell.setCellStyle(boldStyle);
        Cell collectionTotalValueCell = collectionTotalRow.createCell(1);
        double collectionTotal = searchController.bundleCashierCollectionTotal(addingBundle);
        collectionTotalValueCell.setCellValue(collectionTotal);
        collectionTotalValueCell.setCellStyle(numberStyle);

        // "Not Included in Collection Total" header
        Row excludedHeaderRow = dataSheet.createRow(startRow++);
        Cell excludedHeaderCell = excludedHeaderRow.createCell(0);
        excludedHeaderCell.setCellValue("Not Included in Collection Total");
        excludedHeaderCell.setCellStyle(boldStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 1));

        // Add payment methods excluded from collection
        if (addingBundle.getPatientDepositValue() != 0) {
            Row row = dataSheet.createRow(startRow++);
            row.createCell(0).setCellValue("Patient Deposit");
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(addingBundle.getPatientDepositValue());
            valueCell.setCellStyle(numberStyle);
        }

        // Total NOT included in Collection Total
        Row excludedTotalRow = dataSheet.createRow(startRow++);
        Cell excludedTotalLabelCell = excludedTotalRow.createCell(0);
        excludedTotalLabelCell.setCellValue("Total NOT included in Collection Total");
        excludedTotalLabelCell.setCellStyle(boldStyle);
        Cell excludedTotalValueCell = excludedTotalRow.createCell(1);
        double excludedTotal = searchController.bundleCashierExcludedTotal(addingBundle);
        excludedTotalValueCell.setCellValue(excludedTotal);
        excludedTotalValueCell.setCellStyle(numberStyle);

        // Grand Total
        Row grandTotalRow = dataSheet.createRow(startRow++);
        Cell grandTotalLabelCell = grandTotalRow.createCell(0);
        grandTotalLabelCell.setCellValue("Grand Total");
        grandTotalLabelCell.setCellStyle(boldStyle);
        Cell grandTotalValueCell = grandTotalRow.createCell(1);
        double grandTotal = searchController.bundleCashierGrandTotal(addingBundle);
        grandTotalValueCell.setCellValue(grandTotal);
        grandTotalValueCell.setCellStyle(numberStyle);

        // Row below for visual separation
        Row lowerSeparatorRow = dataSheet.createRow(startRow++);
        lowerSeparatorRow.createCell(0).setCellValue("");
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 1));

        // Adjust column widths
        dataSheet.autoSizeColumn(0);
        dataSheet.autoSizeColumn(1);

        return startRow;
    }

    private int addDataToExcelForCreditCards(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 6));
        } else {
            // Create a title row for the report name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(6); // Total is in the seventh column
            totalCell.setCellValue(addingBundle.getTotal());

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));

            // Create header row for Excel only when there is data
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {"Bill No", "Bill Class", "Bill Type", "Card Ref. Number", "Bank", "Reference Bill", "Fee"};
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // First column: Bill No
                Cell billNoCell = excelRow.createCell(0);
                billNoCell.setCellValue(row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getDeptId() : "N/A");

                // Second column: Bill Class
                Cell billClassCell = excelRow.createCell(1);
                billClassCell.setCellValue(row.getPayment() != null && row.getPayment().getBill() != null && row.getPayment().getBill().getBillClassType() != null ? row.getPayment().getBill().getBillClassType().toString() : "N/A");

                // Third column: Bill Type
                Cell billTypeCell = excelRow.createCell(2);
                billTypeCell.setCellValue(row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getCreatedAt().toString() : "N/A");  // Assuming createdAt is a date

                // Fourth column: Card Ref. Number
                Cell cardRefCell = excelRow.createCell(3);
                cardRefCell.setCellValue(row.getPayment() != null ? row.getPayment().getCreditCardRefNo() : "N/A");

                // Fifth column: Bank
                Cell bankCell = excelRow.createCell(4);
                bankCell.setCellValue(row.getPayment() != null && row.getPayment().getBank() != null ? row.getPayment().getBank().getName() : "N/A");

                // Sixth column: Reference Bill
                Cell refBillCell = excelRow.createCell(5);
                refBillCell.setCellValue(row.getPayment() != null && row.getPayment().getBill() != null && row.getPayment().getBill().getBackwardReferenceBill() != null ? row.getPayment().getBill().getBackwardReferenceBill().getDeptId() : "N/A");

                // Seventh column: Fee
                Cell feeCell = excelRow.createCell(6);
                feeCell.setCellValue(row.getPayment() != null ? row.getPayment().getPaidValue() : 0.0);
                CellStyle feeStyle = dataSheet.getWorkbook().createCellStyle();
                CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
                feeStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                feeCell.setCellStyle(feeStyle);
            }
        }

        // Adjust column widths to fit the content
        for (int i = 0; i < 7; i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForProfessionalPayments(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 2));
        } else {
            // Create a title row for the report name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(2); // Total is in the third column
            totalCell.setCellValue(addingBundle.getTotal());

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 1));

            // Create header row for Excel only when there is data
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {"Bill No", "Professional", "Fee"};
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // First column: Bill No
                Cell billNoCell = excelRow.createCell(0);
                billNoCell.setCellValue(row.getBill() != null ? row.getBill().getDeptId() : "N/A");

                // Second column: Professional
                Cell professionalCell = excelRow.createCell(1);
                professionalCell.setCellValue(row.getBill() != null && row.getBill().getStaff() != null && row.getBill().getStaff().getPerson() != null ? row.getBill().getStaff().getPerson().getNameWithTitle() : "N/A");

                // Third column: Fee
                Cell feeCell = excelRow.createCell(2);
                feeCell.setCellValue(row.getBill() != null ? row.getBill().getNetTotal() : 0.0);
                CellStyle feeStyle = dataSheet.getWorkbook().createCellStyle();
                CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
                feeStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                feeCell.setCellStyle(feeStyle);
            }
        }

        // Adjust column widths to fit the content
        for (int i = 0; i < 3; i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForPettyCashPayments(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));
        } else {
            // Create a title row for the report name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(5); // Total is in the sixth column
            totalCell.setCellValue(addingBundle.getTotal());

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 4));

            // Create header row for Excel only when there is data
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {"Bill No", "Bill Type", "Fee", "Reference Bills"};
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
            }
            // Merge columns for Reference Bills
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 3, 5));

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // First column: Bill No
                Cell billNoCell = excelRow.createCell(0);
                billNoCell.setCellValue(row.getBill() != null ? row.getBill().getDeptId() : "N/A");

                // Second column: Bill Type
                Cell billTypeCell = excelRow.createCell(1);
                billTypeCell.setCellValue(row.getBill() != null ? row.getBill().getBillTypeAtomic().getLabel() : "N/A");

                // Third column: Fee
                Cell feeCell = excelRow.createCell(2);
                feeCell.setCellValue(row.getBill() != null ? row.getBill().getNetTotal() : 0.0);
                CellStyle feeStyle = dataSheet.getWorkbook().createCellStyle();
                CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
                feeStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                feeCell.setCellStyle(feeStyle);

                // Fourth to sixth columns: Reference Bills
                Cell refBillCell = excelRow.createCell(3);
                String refBills = Stream.of(
                        row.getBill().getBackwardReferenceBill(),
                        row.getBill().getForwardReferenceBill(),
                        row.getBill().getReferenceBill(),
                        row.getBill().getRefundedBill(),
                        row.getBill().getCancelledBill(),
                        row.getBill().getBilledBill())
                        .filter(Objects::nonNull)
                        .map(b -> Optional.ofNullable(b.getDeptId()).orElse("N/A"))
                        .collect(Collectors.joining(", "));
                refBillCell.setCellValue(refBills);
                excelRow.createCell(4); // For merged cell handling
                excelRow.createCell(5); // For merged cell handling
            }
            // Merge cells for Reference Bills in each data row
            for (int row = startRow - addingBundle.getReportTemplateRows().size(); row < startRow; row++) {
                dataSheet.addMergedRegion(new CellRangeAddress(row, row, 3, 5));
            }
        }

        // Adjust column widths to fit the content
        for (int i = 0; i < 6; i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForPatientDeposits(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 3));
        } else {
            // Create a title row for the report name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(3); // Total is in the fourth column
            totalCell.setCellValue(addingBundle.getTotal());

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 2));

            // Create header row for Excel only when there is data
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {"Bill No", "Patient", "Payment Method", "Value"};
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // First column: Bill No
                Cell billNoCell = excelRow.createCell(0);
                billNoCell.setCellValue(row.getBill() != null ? row.getBill().getDeptId() : "N/A");

                // Second column: Patient
                Cell patientCell = excelRow.createCell(1);
                patientCell.setCellValue(row.getBill() != null && row.getBill().getPatient() != null && row.getBill().getPatient().getPerson() != null ? row.getBill().getPatient().getPerson().getNameWithTitle() : "N/A");

                // Third column: Payment Method
                Cell paymentMethodCell = excelRow.createCell(2);
                paymentMethodCell.setCellValue(row.getBill() != null && row.getBill().getPaymentMethod() != null ? row.getBill().getPaymentMethod().getLabel() : "N/A");

                // Fourth column: Value, formatted as a number
                Cell valueCell = excelRow.createCell(3);
                valueCell.setCellValue(row.getBill() != null ? row.getBill().getNetTotal() : 0.0);
                CellStyle cellStyle = dataSheet.getWorkbook().createCellStyle();
                CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
                cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                valueCell.setCellStyle(cellStyle);
            }
        }

        // Adjust column widths to fit the content
        for (int i = 0; i < 4; i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForCompanyCollection(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 3));
        } else {
            // Create a title row for the report name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(3); // Total is in the fourth column
            totalCell.setCellValue(addingBundle.getTotal());

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 2));

            // Create header row for Excel only when there is data
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {"Bill No", "Company", "Payment Method", "Value"};
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // First column: Bill No
                Cell billNoCell = excelRow.createCell(0);
                billNoCell.setCellValue(row.getBill() != null ? row.getBill().getDeptId() : "Not availbale");

                // Second column: Company
                Cell companyCell = excelRow.createCell(1);
                companyCell.setCellValue(row.getBill() != null && row.getBill().getFromInstitution() != null ? row.getBill().getFromInstitution().getName() : "Not availbale");

                // Third column: Payment Method
                Cell paymentMethodCell = excelRow.createCell(2);
                paymentMethodCell.setCellValue(row.getBill() != null && row.getBill().getPaymentMethod() != null ? row.getBill().getPaymentMethod().getLabel() : "Not availbale");

                // Fourth column: Value, formatted as a number
                Cell valueCell = excelRow.createCell(3);
                valueCell.setCellValue(row.getBill() != null ? row.getBill().getNetTotal() : 0.0);
                CellStyle cellStyle = dataSheet.getWorkbook().createCellStyle();
                CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
                cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                valueCell.setCellStyle(cellStyle);
            }
        }

        // Adjust column widths to fit the content
        for (int i = 0; i < 4; i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForCcDeposits(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 4));
        } else {
            // Create a title row for the report name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(4); // Total is in the fifth column
            totalCell.setCellValue(addingBundle.getTotal());

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 3));

            // Create header row for Excel only when there is data
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {"Bill No", "Collecting Centre Name", "Collecting Centre Code", "Reference Bill", "Fee"};
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // First column: Bill No
                Cell billNoCell = excelRow.createCell(0);
                billNoCell.setCellValue(row.getBill() != null ? row.getBill().getDeptId() : "Not availbale");

                // Second column: Collecting Centre Name
                Cell ccNameCell = excelRow.createCell(1);
                ccNameCell.setCellValue(row.getBill() != null && row.getBill().getFromInstitution() != null ? row.getBill().getFromInstitution().getName() : "Not availbale");

                // Third column: Collecting Centre Code
                Cell ccCodeCell = excelRow.createCell(2);
                ccCodeCell.setCellValue(row.getBill() != null && row.getBill().getFromInstitution() != null ? row.getBill().getFromInstitution().getCode() : "Not availbale");

                // Fourth column: Reference Bill
                Cell refBillCell = excelRow.createCell(3);
                if (row.getBill() != null) {
                    // Concatenating all possible reference bills into a single cell
                    String refBills = Stream.of(
                            row.getBill().getBackwardReferenceBill(),
                            row.getBill().getForwardReferenceBill(),
                            row.getBill().getReferenceBill(),
                            row.getBill().getRefundedBill(),
                            row.getBill().getCancelledBill(),
                            row.getBill().getBilledBill())
                            .filter(Objects::nonNull)
                            .map(b -> b.getDeptId())
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "));
                    refBillCell.setCellValue(refBills);
                } else {
                    refBillCell.setCellValue("Not availbale");
                }

                // Fifth column: Fee
                Cell feeCell = excelRow.createCell(4);
                feeCell.setCellValue(row.getBill() != null ? row.getBill().getNetTotal() : 0.0);
                CellStyle cellStyle = dataSheet.getWorkbook().createCellStyle();
                CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
                cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                feeCell.setCellStyle(cellStyle);
            }
        }

        // Adjust column widths to fit the content
        for (int i = 0; i < 5; i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForDepartmentCollection(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Check if there are any data rows
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Pharmacy Collection");
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));
            return startRow;
        }

        // Create number format style
        CellStyle numberStyle = dataSheet.getWorkbook().createCellStyle();
        CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
        numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

        // Create title row with bundle name
        Row titleRow = dataSheet.createRow(startRow++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(addingBundle.getName());

        // Build dynamic column list based on which payment methods have values
        java.util.List<String> columns = new java.util.ArrayList<>();
        columns.add("Department");

        if (addingBundle.getCashValue() != 0) columns.add("Cash");
        if (addingBundle.getCardValue() != 0) columns.add("Card");
        if (addingBundle.getMultiplePaymentMethodsValue() != 0) columns.add("Multiple");
        if (addingBundle.getStaffValue() != 0) columns.add("Staff");
        if (addingBundle.getCreditValue() != 0) columns.add("Credit");
        if (addingBundle.getStaffWelfareValue() != 0) columns.add("Staff Welfare");
        if (addingBundle.getVoucherValue() != 0) columns.add("Voucher");
        if (addingBundle.getIouValue() != 0) columns.add("IOU");
        if (addingBundle.getAgentValue() != 0) columns.add("Agent");
        if (addingBundle.getChequeValue() != 0) columns.add("Cheque");
        if (addingBundle.getSlipValue() != 0) columns.add("Slip");
        if (addingBundle.getEWalletValue() != 0) columns.add("eWallet");
        if (addingBundle.getPatientDepositValue() != 0) columns.add("Patient Deposit");
        if (addingBundle.getPatientPointsValue() != 0) columns.add("Patient Points");
        if (addingBundle.getOnlineSettlementValue() != 0) columns.add("Online Settlement");

        columns.add("Grand Total");
        columns.add("Collection Total");
        columns.add("Total NOT included in Collection Total");

        // Create header row
        Row headerRow = dataSheet.createRow(startRow++);
        for (int i = 0; i < columns.size(); i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(columns.get(i));
        }

        // Populate data rows
        for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
            Row excelRow = dataSheet.createRow(startRow++);
            int colIndex = 0;

            // Department
            excelRow.createCell(colIndex++).setCellValue(
                row.getDepartment() != null ? row.getDepartment().getName() : "Not available"
            );

            // Dynamic payment method columns
            if (addingBundle.getCashValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getCashValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getCardValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getCardValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getMultiplePaymentMethodsValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getMultiplePaymentMethodsValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getStaffValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getStaffValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getCreditValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getCreditValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getStaffWelfareValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getStaffWelfareValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getVoucherValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getVoucherValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getIouValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getIouValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getAgentValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getAgentValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getChequeValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getChequeValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getSlipValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getSlipValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getEWalletValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getEwalletValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getPatientDepositValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getPatientDepositValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getPatientPointsValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getPatientPointsValue());
                cell.setCellStyle(numberStyle);
            }
            if (addingBundle.getOnlineSettlementValue() != 0) {
                Cell cell = excelRow.createCell(colIndex++);
                cell.setCellValue(row.getOnlineSettlementValue());
                cell.setCellStyle(numberStyle);
            }

            // Always include these columns
            Cell grandTotalCell = excelRow.createCell(colIndex++);
            grandTotalCell.setCellValue(row.getCashierGrandTotal());
            grandTotalCell.setCellStyle(numberStyle);

            Cell collectionTotalCell = excelRow.createCell(colIndex++);
            collectionTotalCell.setCellValue(row.getCashierCollectionTotal());
            collectionTotalCell.setCellStyle(numberStyle);

            Cell excludedTotalCell = excelRow.createCell(colIndex++);
            excludedTotalCell.setCellValue(row.getCashierExcludedTotal());
            excludedTotalCell.setCellStyle(numberStyle);
        }

        // Add footer row with totals
        Row footerRow = dataSheet.createRow(startRow++);
        int colIndex = 0;

        footerRow.createCell(colIndex++).setCellValue(""); // Empty for Department column

        // Dynamic payment method totals
        if (addingBundle.getCashValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getCashValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getCardValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getCardValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getMultiplePaymentMethodsValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getMultiplePaymentMethodsValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getStaffValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getStaffValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getCreditValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getCreditValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getStaffWelfareValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getStaffWelfareValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getVoucherValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getVoucherValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getIouValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getIouValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getAgentValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getAgentValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getChequeValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getChequeValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getSlipValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getSlipValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getEWalletValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getEWalletValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getPatientDepositValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getPatientDepositValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getPatientPointsValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getPatientPointsValue());
            cell.setCellStyle(numberStyle);
        }
        if (addingBundle.getOnlineSettlementValue() != 0) {
            Cell cell = footerRow.createCell(colIndex++);
            cell.setCellValue(addingBundle.getOnlineSettlementValue());
            cell.setCellStyle(numberStyle);
        }

        // Always include footer totals
        Cell grandTotalFooter = footerRow.createCell(colIndex++);
        grandTotalFooter.setCellValue(addingBundle.getCashierGrandTotal());
        grandTotalFooter.setCellStyle(numberStyle);

        Cell collectionTotalFooter = footerRow.createCell(colIndex++);
        collectionTotalFooter.setCellValue(addingBundle.getCashierCollectionTotal());
        collectionTotalFooter.setCellStyle(numberStyle);

        Cell excludedTotalFooter = footerRow.createCell(colIndex++);
        excludedTotalFooter.setCellValue(addingBundle.getCashierExcludedTotal());
        excludedTotalFooter.setCellStyle(numberStyle);

        // Add notes
        startRow++; // Blank row
        Row noteRow1 = dataSheet.createRow(startRow++);
        noteRow1.createCell(0).setCellValue("Note: Credit bills are NOT listed in this section. See 'Pharmacy Collection Bills - Credit' section below for credit transactions.");
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, columns.size() - 1));

        // Adjust column widths to fit the content
        for (int i = 0; i < columns.size(); i++) {
            dataSheet.autoSizeColumn(i);
        }

        return startRow;
    }

    private int addDataToExcelForItemSummaryGroupedByCategory(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(6); // Assuming 7th column is for total
            totalCell.setCellValue(addingBundle.getTotal()); // Assuming getTotal() returns formatted string

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {
                "Category", "Item / Service", "Count", "Hospital Fee",
                "Professional Fee", "Discount", "Net Amount"
            };
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnHeaders[i]);
            }

            // Iterate through each row of the data and add to Excel
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                String categoryOrItem = row.getCategory() != null ? row.getCategory().getName() : "";
                String itemName = row.getItem() != null ? row.getItem().getName() : "";

                // Create cells for each column
                excelRow.createCell(0).setCellValue(categoryOrItem);
                excelRow.createCell(1).setCellValue(itemName);
                excelRow.createCell(2).setCellValue(row.getItemCount());
                excelRow.createCell(3).setCellValue(row.getItemHospitalFee());
                excelRow.createCell(4).setCellValue(row.getItemProfessionalFee());
                excelRow.createCell(5).setCellValue(row.getItemDiscountAmount());
                excelRow.createCell(6).setCellValue(row.getItemNetTotal());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 6));
        }

        return startRow;
    }

    private int addDataToExcelForCreditItemSummaryGroupedByCategory(XSSFSheet dataSheet, int startRow, ReportTemplateRowBundle addingBundle) {
        // Create title row only if there is data
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create title row with name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(6); // Assuming 7th column is for total
            totalCell.setCellValue(addingBundle.getTotal()); // Assuming getTotal() returns formatted string

            // Merge title across all columns except the last (for total)
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {
                "Category", "Item / Service", "Count", "Hospital Fee",
                "Professional Fee", "Discount", "Net Amount"
            };
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnHeaders[i]);
            }

            // Iterate through each row of the data and add to Excel
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                String categoryOrItem = row.getCategory() != null ? row.getCategory().getName() : "";
                String itemName = row.getItem() != null ? row.getItem().getName() : "";

                // Create cells for each column
                excelRow.createCell(0).setCellValue(categoryOrItem);
                excelRow.createCell(1).setCellValue(itemName);
                excelRow.createCell(2).setCellValue(row.getItemCount());
                excelRow.createCell(3).setCellValue(row.getItemHospitalFee());
                excelRow.createCell(4).setCellValue(row.getItemProfessionalFee());
                excelRow.createCell(5).setCellValue(row.getItemDiscountAmount());
                excelRow.createCell(6).setCellValue(row.getItemNetTotal());
            }

        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 6));
        }

        return startRow;
    }

    // New method for DTO-based bundles
    public StreamedContent createExcelForDtoBundle(com.divudi.core.data.dto.DailyReturnBundleDTO rootBundle) throws IOException {
        return createExcelForDtoBundle(rootBundle, new Date(), new Date());
    }
    
    public StreamedContent createExcelForDtoBundle(com.divudi.core.data.dto.DailyReturnBundleDTO rootBundle, Date fromDate, Date toDate) throws IOException {
        if (rootBundle == null) {
            return null;
        }
        StreamedContent excelSc;

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet dataSheet = workbook.createSheet(rootBundle.getName());

        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));

        Row titleRow = dataSheet.createRow(0);
        Cell headerCell = titleRow.createCell(0);
        headerCell.setCellValue(rootBundle.getName() + "  -  " + CommonFunctions.getDateTimeFormat24(fromDate) + " to " + CommonFunctions.getDateTimeFormat24(toDate));
        headerCell.setCellStyle(style);
        dataSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // Add description row if available
        if (rootBundle.getDescription() != null && !rootBundle.getDescription().trim().isEmpty()) {
            Row descRow = dataSheet.createRow(1);
            Cell descCell = descRow.createCell(0);
            descCell.setCellValue(rootBundle.getDescription());
            descCell.setCellStyle(style);
            dataSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));
        }

        int currentRow = 3;

        if (rootBundle.getBundles() == null || rootBundle.getBundles().isEmpty()) {
            currentRow = addDtoDataToExcel(dataSheet, currentRow, rootBundle, rootBundle.getBundleType());
        } else {
            for (com.divudi.core.data.dto.DailyReturnBundleDTO childBundle : rootBundle.getBundles()) {
                currentRow = addDtoDataToExcel(dataSheet, currentRow, childBundle, childBundle.getBundleType());
                currentRow++;
            }
        }

        // Write the output to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        // Create a ByteArrayInputStream from the byte array
        byte[] bytes = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);

        // Create and return StreamedContent
        excelSc = DefaultStreamedContent.builder()
                .name("daily_return_dto_" + CommonFunctions.getDateTimeFormat24(fromDate) + "_to_" + CommonFunctions.getDateTimeFormat24(toDate) + ".xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .stream(() -> inputStream)
                .build();

        return excelSc;
    }
    
    private int addDtoDataToExcel(XSSFSheet dataSheet, int startRow, com.divudi.core.data.dto.DailyReturnBundleDTO addingBundle, String type) {
        if (addingBundle == null) {
            return startRow;
        }

        // Create title row with bundle name and total
        Row titleRow = dataSheet.createRow(startRow++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(addingBundle.getName());
        Cell totalCell = titleRow.createCell(6); // Assuming 7th column is for total
        totalCell.setCellValue(addingBundle.getTotal() != null ? addingBundle.getTotal() : 0.0);

        // Merge title across all columns except the last (for total)
        dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 5));

        // Only add data rows if there are rows to display
        if (addingBundle.getRows() != null && !addingBundle.getRows().isEmpty()) {
            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {
                "Category", "Item / Service", "Count", "Hospital Fee",
                "Professional Fee", "Discount", "Net Amount"
            };
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnHeaders[i]);
            }

            // Iterate through each row of the DTO data and add to Excel
            for (com.divudi.core.data.dto.DailyReturnRowDTO row : addingBundle.getRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // Create cells for each column
                excelRow.createCell(0).setCellValue(row.getCategoryName() != null ? row.getCategoryName() : "");
                excelRow.createCell(1).setCellValue(row.getItemName() != null ? row.getItemName() : "");
                excelRow.createCell(2).setCellValue(row.getItemCount() != null ? row.getItemCount() : 0L);
                excelRow.createCell(3).setCellValue(row.getItemHospitalFee() != null ? row.getItemHospitalFee() : 0.0);
                excelRow.createCell(4).setCellValue(row.getItemProfessionalFee() != null ? row.getItemProfessionalFee() : 0.0);
                excelRow.createCell(5).setCellValue(row.getItemDiscountAmount() != null ? row.getItemDiscountAmount() : 0.0);
                excelRow.createCell(6).setCellValue(row.getItemNetTotal() != null ? row.getItemNetTotal() : 0.0);
            }
        } else {
            // If no data, create a single row stating this
            Row noDataRow = dataSheet.createRow(startRow++);
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 6));
        }

        return startRow;
    }




}
