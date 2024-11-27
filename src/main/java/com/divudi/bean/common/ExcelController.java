package com.divudi.bean.common;

import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
    CommonController commonController;

    /**
     * Creates a new instance of ExcelController
     */
    public ExcelController() {
    }

    public StreamedContent createExcelForBundle(ReportTemplateRowBundle rootBundle) throws IOException {
        if (rootBundle == null) {
            return null;
        }
        StreamedContent excelSc;

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet dataSheet = workbook.createSheet(rootBundle.getName());

        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Row titleRow = dataSheet.createRow(0);
        Cell headerCell = titleRow.createCell(0);
        headerCell.setCellValue("Daily Return  -  " + commonController.getDateTimeFormat24(searchController.getFromDate()) + " to " + commonController.getDateTimeFormat24(searchController.getToDate()));
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
            case "netCash":
                return addDataToExcelForTitleBundle(dataSheet, startRow, addingBundle);
            case "opdServiceCollectionCredit":
                return addDataToExcelForCreditItemSummaryGroupedByCategory(dataSheet, startRow, addingBundle);

        }
        return startRow++;
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

    
    
    
}
