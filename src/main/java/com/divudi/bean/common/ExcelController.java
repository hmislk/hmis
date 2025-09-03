package com.divudi.bean.common;

import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd HH:mm"));

        Row titleRow = dataSheet.createRow(0);
        Cell headerCell = titleRow.createCell(0);
        headerCell.setCellValue(rootBundle.getName()+ "  -  " + CommonFunctions.getDateTimeFormat24(fromDate) + " to " + CommonFunctions.getDateTimeFormat24(toDate));
        headerCell.setCellStyle(style);
        dataSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        String indDataSet = "";
        if (searchController.getInstitution() != null) {
            indDataSet += searchController.getInstitution().getName();
        }
        if (searchController.getDepartment() != null) {
            indDataSet += "  ";
            indDataSet += searchController.getDepartment().getName();
            indDataSet += "(" + searchController.getSite().getName() + ")";
        }
        if (!indDataSet.trim().isEmpty() || indDataSet.trim() != null) {
            Row insData = dataSheet.createRow(1);
            Cell insCell = insData.createCell(0);
            insCell.setCellValue(indDataSet);
            insCell.setCellStyle(style);
            dataSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));
        }

        int currentRow = 3;

        if (rootBundle.getBundles() == null || rootBundle.getBundles().isEmpty()) {
            currentRow = addDataToExcel(dataSheet, currentRow, rootBundle, rootBundle.getBundleType());
        } else {
            for (ReportTemplateRowBundle childBundle : rootBundle.getBundles()) {
                currentRow = addDataToExcel(dataSheet, currentRow, childBundle, childBundle.getBundleType());
                currentRow++;
            }
        }

        // Write the output to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Set the downloading file
        excelSc = DefaultStreamedContent.builder()
                .name(rootBundle.getName() + ".xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .stream(() -> inputStream)
                .build();

        return excelSc;
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
            case "netCash":
                return addDataToExcelForTitleBundle(dataSheet, startRow, addingBundle);
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

            // Calculate total columns needed
            int totalColumns = 4; // Institution, Site, Department, Date
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
            totalColumns++; // Grand Total

            // Merge title across all columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, totalColumns - 1));

            // Create header row for Excel
            Row headerRow = dataSheet.createRow(startRow++);
            int colIndex = 0;

            // Base columns that are always present
            headerRow.createCell(colIndex++).setCellValue("Institution");
            headerRow.createCell(colIndex++).setCellValue("Site");
            headerRow.createCell(colIndex++).setCellValue("Department");
            headerRow.createCell(colIndex++).setCellValue("Date");

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

            // Grand Total column
            headerRow.createCell(colIndex++).setCellValue("Grand Total");

            // Create date format for Excel
            CellStyle dateStyle = dataSheet.getWorkbook().createCellStyle();
            CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            // Create number format for currency values
            CellStyle currencyStyle = dataSheet.getWorkbook().createCellStyle();
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

            // Iterate through each row of the data and add to Excel
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);
                colIndex = 0;

                // Institution
                if (row.getDepartment() != null && row.getDepartment().getInstitution() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getDepartment().getInstitution().getName());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Site
                if (row.getDepartment() != null && row.getDepartment().getSite() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getDepartment().getSite().getName());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Department
                if (row.getDepartment() != null) {
                    excelRow.createCell(colIndex++).setCellValue(row.getDepartment().getName());
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
                }

                // Date
                if (row.getDate() != null) {
                    Cell dateCell = excelRow.createCell(colIndex++);
                    dateCell.setCellValue(row.getDate());
                    dateCell.setCellStyle(dateStyle);
                } else {
                    excelRow.createCell(colIndex++).setCellValue("");
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

                // Grand Total
                Cell totalCell = excelRow.createCell(colIndex++);
                totalCell.setCellValue(row.getTotal());
                totalCell.setCellStyle(currencyStyle);
            }

            // Add footer row with totals
            Row footerRow = dataSheet.createRow(startRow++);
            colIndex = 0;

            // Empty cells for Institution, Site, Department, Date
            for (int i = 0; i < 4; i++) {
                footerRow.createCell(colIndex++).setCellValue("");
            }

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

            // Grand Total footer
            Cell grandTotalCell = footerRow.createCell(colIndex++);
            grandTotalCell.setCellValue(addingBundle.getTotal());
            grandTotalCell.setCellStyle(currencyStyle);

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
            noDataCell.setCellValue("No Data for " + addingBundle.getName());
            // Merge the cell across both columns
            dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 1));
        } else {
            // Create a title row for the report name and total
            Row titleRow = dataSheet.createRow(startRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(addingBundle.getName());
            Cell totalCell = titleRow.createCell(1); // Assuming 2nd column is for total
            totalCell.setCellValue(addingBundle.getTotal()); // Assuming getTotal() returns formatted string

            // Merge title across the first column (leaving second for total)
            try {
                // Merge title across the first column (leaving second for total)
                dataSheet.addMergedRegion(new CellRangeAddress(startRow - 1, startRow - 1, 0, 0));
            } catch (Exception e) {

            }

            // Create header row for Excel only when there is data
            Row headerRow = dataSheet.createRow(startRow++);
            String[] columnHeaders = {"Department", "Collection"};
            for (int i = 0; i < columnHeaders.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                Row excelRow = dataSheet.createRow(startRow++);

                // First column: Department Name
                Cell departmentCell = excelRow.createCell(0);
                departmentCell.setCellValue(row.getDepartment() != null ? row.getDepartment().getName() : "Not availbale");

                // Second column: Collection, formatted as a number
                Cell collectionCell = excelRow.createCell(1);
                collectionCell.setCellValue(row.getRowValue() != null ? row.getRowValue() : 0.0);

                // Optionally, you can apply a number format to the collection cell
                CellStyle cellStyle = dataSheet.getWorkbook().createCellStyle();
                CreationHelper createHelper = dataSheet.getWorkbook().getCreationHelper();
                cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
                collectionCell.setCellStyle(cellStyle);
            }
        }

        // Adjust column widths to fit the content
        dataSheet.autoSizeColumn(0);
        dataSheet.autoSizeColumn(1);

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
