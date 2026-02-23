/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ControllerWithReportFilters;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.ReportTimerController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.report.CommonReport;
import com.divudi.core.data.dto.StockReportByItemDTO;
import com.divudi.core.data.dto.DepartmentViceStockDTO;
import com.divudi.core.data.reports.PharmacyReports;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PharmacyStockRow;
import com.divudi.core.data.dataStructure.StockReportRecord;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.DosageForm;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.StockHistory;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.util.Objects;
import javax.faces.context.ExternalContext;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Buddhika
 */
@Named(value = "reportsStock")
@SessionScoped
public class ReportsStock implements Serializable, ControllerWithReportFilters {

    /**
     * Bean Variables
     */
    Department department;
    Staff staff;
    Institution institution;
    Institution site;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;
    private ReportViewType reportViewType;
    private List<ReportViewType> reportViewTypes;
    private Category category;
    private DosageForm dosageForm;
    private DepartmentType departmentType;
    private List<DepartmentType> selectedDepartmentTypes;
    List<Stock> stocks;
    List<StockDTO> stockDtos;
    double stockSaleValue;
    double stockPurchaseValue;
    double stockCostValue;
    List<PharmacyStockRow> pharmacyStockRows;
    List<StockReportRecord> records;
    Date fromDate;
    Date toDate;
    Date fromDateE;
    Date toDateE;
    double totalQty;
    double totalPurchaseRate;
    double totalPurchaseValue;
    double totalRetailSaleRate;
    double totalRetailSaleValue;
    private double totalGrnCashPurchaseValue;
    private double totalGrnCashRetailValue;
    private double totalGrnCreditPurchaseValue;
    private double totalGrnCreditRetailValue;
    private double totalDirectPurchasePurchaseValue;
    private double totalDirectPurchaseRetailValue;
    private double totalGrnReturnPurchaseValue;
    private double totalGrnReturnRetailValue;
    private double totalGrnFreeQtyPurchaseValue;
    private double totalGrnFreeQtyRetailValue;
    private double totalRetailSaleCashValue;
    private double totalPurchaseSaleCashValue;
    private double totalRetailSaleCreditValue;
    private double totalPurchaseSaleCreditValue;
    private double startingPurchaseValue;
    private double startingSaleValue;
    private double purchaseValueAfterGrn;
    private double saleValueAfterGrn;
    private double purchaseValueAfterGrnReturnAndFreeQty;
    private double saleValueAfterGrnReturnAndFreeQty;
    private double purchaseValueAfterSale;
    private double saleValueAfterSale;
    private double purchaseValueAfterDisbursement;
    private double saleValueAfterDisbursement;
    private double finalTotalPurchaseValue;
    private double finalTotalSaleValue;
    private boolean includeZeroStock;

    private int fromRecord;
    private int toRecord;
    Vmp vmp;
    BillType[] billTypes;
    ReportKeyWord reportKeyWord;
    private List<BillItem> billItems;
    @Inject
    private ReportTimerController reportTimerController;
    @Inject
    ReportsTransfer reportsTransfer;
    @Inject
    CommonReport commonReport;
    @Inject
    PharmacySummaryReportController pharmacySummaryReportController;
    /**
     * Managed Beans
     */
    @Inject
    DealerController dealerController;
    @Inject
    SessionController sessionController;
    @Inject
    ItemController itemController;
    /**
     * EJBs
     */
    @EJB
    StockFacade stockFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade billFacade;

    public String navigateToPharmacyReportDepartmentStockByItem() {
        pharmacyStockRows = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_by_item?faces-redirect=true";
    }

    public String navigateToPharmacyReportDepartmentStockByItemDTO() {
        pharmacyStockRows = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_by_item_DTO?faces-redirect=true";
    }

    public String navigateToPharmacyReportDepartmentStockByItemOrderByVmp() {
        pharmacyStockRows = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_by_item_order_by_vmp?faces-redirect=true";
    }

    public String navigateToPharmacyReportDepartmentStockByZeroItem() {
        pharmacyStockRows = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_by_zero_item?faces-redirect=true";
    }

    public String navigateToStockReportByBatch() {
        stocks = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_by_batch?faces-redirect=true";
    }

    public String navigateToStockReportByBatchForExport() {
        stocks = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_by_batch_for_export?faces-redirect=true";
    }

    public String navigateToStockReportByBatchDto() {
        stockDtos = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_by_batch_dto?faces-redirect=true";
    }

    public String navigateToPharmacyStockOverviewReport() {
        pharmacyStockRows = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_stock_overview?faces-redirect=true";
    }

    public String navigateToDepartmentViceStockReport() {
        departmentViceStockDtos = new ArrayList<>();
        return "/pharmacy/pharmacy_report_department_vice_stock?faces-redirect=true";
    }

    /**
     * Methods
     */
    public void fillDepartmentStocks() {
        reportTimerController.trackReportExecution(() -> {
            Date startedAt = new Date();
            Map<String, Object> m = new HashMap<>();
            StringBuilder sql = new StringBuilder("select s from Stock s where s.stock > 0");

            if (department != null) {
                sql.append(" and s.department=:d");
                m.put("d", department);
            }
            if (site != null) {
                sql.append(" and s.department.site=:site");
                m.put("site", site);
            }
            if (institution != null) {
                sql.append(" and s.department.institution=:ins");
                m.put("ins", institution);
            }

            Date beforeJpql = new Date();
            stocks = getStockFacade().findByJpql(sql.toString(), m);

            Date afterJpql = new Date();
            stocks.sort(Comparator.comparing(s -> s.getItemBatch().getItem().getName(), String.CASE_INSENSITIVE_ORDER));

            Date beforeCal = new Date();
            stockPurchaseValue = stocks.stream()
                    .mapToDouble(s -> s.getItemBatch().getPurcahseRate() * s.getStock())
                    .sum();

            stockSaleValue = stocks.stream()
                    .mapToDouble(s -> s.getItemBatch().getRetailsaleRate() * s.getStock())
                    .sum();

            stockCostValue = stocks.stream()
                    .mapToDouble(s -> {
                        Double cr = s.getItemBatch().getCostRate();
                        return (cr == null ? 0.0 : cr) * s.getStock();
                    })
                    .sum();
            Date afterCal = new Date();
        }, PharmacyReports.STOCK_REPORT_BY_BATCH, sessionController.getLoggedUser());
    }

    public void fillDepartmentStockDtos() {
        reportTimerController.trackReportExecution(() -> {
            Map<String, Object> m = new HashMap<>();
            StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.dto.StockDTO(");
            jpql.append("s.id, ");
            jpql.append("cat.name, ");
            jpql.append("s.itemBatch.item.name, ");
            jpql.append("s.itemBatch.item.departmentType, ");
            jpql.append("s.itemBatch.item.code, ");
            jpql.append("s.itemBatch.dateOfExpire, ");
            jpql.append("s.itemBatch.batchNo, ");
            jpql.append("s.stock, ");
            jpql.append("s.itemBatch.purcahseRate, ");
            jpql.append("s.itemBatch.costRate, ");
            jpql.append("s.itemBatch.retailsaleRate, ");
            jpql.append("df.name) ");
            jpql.append("from Stock s ");
            jpql.append("left join s.itemBatch.item.category cat ");
            jpql.append("left join s.itemBatch.item.dosageForm df ");
            jpql.append("where s.stock > 0");

            if (department != null) {
                jpql.append(" and s.department=:d");
                m.put("d", department);
            }
            if (site != null) {
                jpql.append(" and s.department.site=:site");
                m.put("site", site);
            }
            if (institution != null) {
                jpql.append(" and s.department.institution=:ins");
                m.put("ins", institution);
            }

            if (selectedDepartmentTypes != null && !selectedDepartmentTypes.isEmpty()) {
                jpql.append(" and s.itemBatch.item.departmentType IN :departmentTypes");
                m.put("departmentTypes", selectedDepartmentTypes);
            }

            if (category != null) {
                jpql.append(" and s.itemBatch.item.category=:cat");
                m.put("cat", category);
            }

            if (dosageForm != null) {
                jpql.append(" and s.itemBatch.item.dosageForm=:df");
                m.put("df", dosageForm);
            }

            stockDtos = (List<StockDTO>) stockFacade.findLightsByJpql(jpql.toString(), m);
            stockDtos.sort(Comparator.comparing(StockDTO::getItemName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));

            stockPurchaseValue = stockDtos.stream()
                    .mapToDouble(s -> {
                        Double pr = s.getPurchaseRate();
                        Double qty = s.getStockQty();
                        return (pr == null ? 0.0 : pr) * (qty == null ? 0.0 : qty);
                    })
                    .sum();

            stockSaleValue = stockDtos.stream()
                    .mapToDouble(s -> {
                        Double rr = s.getRetailRate();
                        Double qty = s.getStockQty();
                        return (rr == null ? 0.0 : rr) * (qty == null ? 0.0 : qty);
                    })
                    .sum();

            stockCostValue = stockDtos.stream()
                    .mapToDouble(s -> {
                        Double cr = s.getCostRate();
                        Double qty = s.getStockQty();
                        return (cr == null ? 0.0 : cr) * (qty == null ? 0.0 : qty);
                    })
                    .sum();
        }, PharmacyReports.STOCK_REPORT_BY_BATCH, sessionController.getLoggedUser());
    }

    public void exportCurrentStockByBatchToExcel() {

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response
                = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=Current_Stock_By_Batch.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Current Stock By Batch");

            int rowIndex = 0;
            int totalColumns = 14;

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            // Title Style (Big + Bold + Center)
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            //filter Style
            CellStyle filterStyle = workbook.createCellStyle();
            filterStyle.setAlignment(HorizontalAlignment.CENTER);

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Data Style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            DataFormat dataFormat = workbook.createDataFormat();

            CellStyle formatStyle = workbook.createCellStyle();
            formatStyle.cloneStyleFrom(dataStyle);
            formatStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            // =========================
            // HEADER
            // =========================
            Row instRow = sheet.createRow(rowIndex++);
            Cell instCell = instRow.createCell(0);
            instCell.setCellValue(institution != null ? institution.getName() : "");
            instCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            Row siteRow = sheet.createRow(rowIndex++);
            Cell siteCell = siteRow.createCell(0);
            siteCell.setCellValue(site != null ? site.getName() : "");
            siteCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            Row deptRow = sheet.createRow(rowIndex++);
            Cell deptCell = deptRow.createCell(0);
            deptCell.setCellValue(department != null ? department.getName() : "");
            deptCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            rowIndex++;

            // Filters row
            Row filterRow = sheet.createRow(rowIndex++);
            Cell filterCell = filterRow.createCell(0);
            filterCell.setCellValue(
                    "Category: " + (category != null ? category.getName() : "All")
                    + " | Dosage Form: " + (dosageForm != null ? dosageForm.getName() : "All")
                    + " | Department Type: " + getSelectedDepartmentTypesPrintDisplay()
            );
            filterCell.setCellStyle(filterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            rowIndex++;

            // Report Title
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Current Stock By Batch Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            rowIndex++;

            // =========================
            // header
            // =========================
            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {
                "Category", "Dosage Form", "Item", "Type", "Code",
                "Expiry", "Batch No", "Stock",
                "Purchase Rate", "Purchase Value",
                "Cost Rate", "Cost Value",
                "Retail Rate", "Retail Value"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // =========================
            // data row
            // =========================
            for (StockDTO row : stockDtos) {

                Row dataRow = sheet.createRow(rowIndex++);
                int col = 0;

                dataRow.createCell(col++).setCellValue(row.getCategoryName());
                dataRow.createCell(col++).setCellValue(row.getDosageFormName());
                dataRow.createCell(col++).setCellValue(row.getItemName());
                dataRow.createCell(col++).setCellValue(row.getDepartmentType().name());
                dataRow.createCell(col++).setCellValue(row.getCode());

                dataRow.createCell(col++).setCellValue(
                        row.getDateOfExpire() != null
                        ? new SimpleDateFormat("dd/MM/yyyy")
                                .format(row.getDateOfExpire()) : "-"
                );

                dataRow.createCell(col++).setCellValue(row.getBatchNo());
                dataRow.createCell(col++).setCellValue(row.getStockQty());

                double purchaseRate = row.getPurchaseRate() != null ? row.getPurchaseRate() : 0.0;
                double costRate = row.getCostRate() != null ? row.getCostRate() : 0.0;
                double retailRate = row.getRetailRate() != null ? row.getRetailRate() : 0.0;

                dataRow.createCell(col++).setCellValue(purchaseRate);
                dataRow.createCell(col++).setCellValue(purchaseRate * row.getStockQty());
                dataRow.createCell(col++).setCellValue(costRate);
                dataRow.createCell(col++).setCellValue(costRate * row.getStockQty());
                dataRow.createCell(col++).setCellValue(retailRate);
                dataRow.createCell(col++).setCellValue(retailRate * row.getStockQty());

                for (int i = 0; i < totalColumns; i++) {
                    dataRow.getCell(i).setCellStyle(dataStyle);
                }
            }

            // =========================
            // total
            // =========================
            Row footerRow = sheet.createRow(rowIndex++);
            Cell totalLabel = footerRow.createCell(7);
            totalLabel.setCellValue("Total");
            totalLabel.setCellStyle(headerStyle);

            Cell totalPurchase = footerRow.createCell(9);
            totalPurchase.setCellValue(stockPurchaseValue);
            totalPurchase.setCellStyle(formatStyle);

            Cell totalCost = footerRow.createCell(11);
            totalCost.setCellValue(stockCostValue);
            totalCost.setCellStyle(formatStyle);

            Cell totalSale = footerRow.createCell(13);
            totalSale.setCellValue(stockSaleValue);
            totalSale.setCellStyle(formatStyle);

            // Auto size
            for (int i = 0; i < totalColumns; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            Logger.getLogger(ReportsStock.class.getName()).log(Level.SEVERE, e.getMessage());
        }
    }

    public void exportCurrentStockByBatchToPDF() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Current_Stock_By_Batch.pdf");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            com.itextpdf.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font filterFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // =========================
            // HEADER (Institution, Site, Department)
            // =========================
            Paragraph instPara = new Paragraph(institution != null ? institution.getName() : "", titleFont);
            instPara.setAlignment(Element.ALIGN_CENTER);
            document.add(instPara);

            Paragraph sitePara = new Paragraph(site != null ? site.getName() : "", titleFont);
            sitePara.setAlignment(Element.ALIGN_CENTER);
            document.add(sitePara);

            Paragraph deptPara = new Paragraph(department != null ? department.getName() : "", titleFont);
            deptPara.setAlignment(Element.ALIGN_CENTER);
            document.add(deptPara);

            document.add(new Paragraph(" ")); // empty line

            // Filters
            Paragraph filterPara = new Paragraph(
                    "Category: " + (category != null ? category.getName() : "All")
                    + " | Dosage Form: " + (dosageForm != null ? dosageForm.getName() : "All")
                    + " | Department Type: " + getSelectedDepartmentTypesPrintDisplay(),
                    filterFont
            );
            filterPara.setAlignment(Element.ALIGN_CENTER);
            document.add(filterPara);

            // Report Title
            Paragraph titlePara = new Paragraph("Current Stock By Batch Report", subTitleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);

            document.add(new Paragraph(" ")); // empty line

            // =========================
            // TABLE
            // =========================
            int columnCount = 14;
            PdfPTable table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            String[] headers = {
                "Category", "Dosage Form", "Item", "Type", "Code",
                "Expiry", "Batch No", "Stock",
                "Purchase Rate", "Purchase Value",
                "Cost Rate", "Cost Value",
                "Retail Rate", "Retail Value"
            };

            float[] widths = {3f, 3f, 4f, 3f, 3f, 3f, 3f, 2f, 3f, 3f, 3f, 3f, 3f, 3f};
            table.setWidths(widths);

            // Header row
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                
                table.addCell(cell);
            }

            // Data rows
            for (StockDTO row : stockDtos) {
                table.addCell(new PdfPCell(new Phrase(row.getCategoryName() != null ? row.getCategoryName() : "-", dataFont)));
                table.addCell(new PdfPCell(new Phrase(row.getDosageFormName() != null ? row.getDosageFormName() : "-", dataFont)));
                table.addCell(new PdfPCell(new Phrase(row.getItemName() != null ? row.getItemName() : "-", dataFont)));
                table.addCell(new PdfPCell(new Phrase(row.getDepartmentType() != null ? row.getDepartmentType().name() : "-", dataFont)));
                table.addCell(new PdfPCell(new Phrase(row.getCode() != null ? row.getCode() : "-", dataFont)));
                table.addCell(new PdfPCell(new Phrase(row.getDateOfExpire() != null ? sdf.format(row.getDateOfExpire()) : "-", dataFont)));
                table.addCell(new PdfPCell(new Phrase(row.getBatchNo() != null ? row.getBatchNo() : "-", dataFont)));

                PdfPCell stockCell = new PdfPCell(new Phrase(String.valueOf(Objects.requireNonNullElse(row.getStockQty(), 0.0)), dataFont));
                stockCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(stockCell);

                // Numeric columns
                double purchaseRate = Objects.requireNonNullElse(row.getPurchaseRate(), 0.0);
                double costRate = Objects.requireNonNullElse(row.getCostRate(), 0.0);
                double retailRate = Objects.requireNonNullElse(row.getRetailRate(), 0.0);
                double purchaseValue = purchaseRate * Objects.requireNonNullElse(row.getStockQty(), 0.0);
                double costValue = costRate * Objects.requireNonNullElse(row.getStockQty(), 0.0);
                double retailValue = retailRate * Objects.requireNonNullElse(row.getStockQty(), 0.0);

                PdfPCell prCell = new PdfPCell(new Phrase(String.format("%,.2f", purchaseRate), dataFont));
                prCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(prCell);

                PdfPCell pvCell = new PdfPCell(new Phrase(String.format("%,.2f", purchaseValue), dataFont));
                pvCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(pvCell);

                PdfPCell crCell = new PdfPCell(new Phrase(String.format("%,.2f", costRate), dataFont));
                crCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(crCell);

                PdfPCell cvCell = new PdfPCell(new Phrase(String.format("%,.2f", costValue), dataFont));
                cvCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cvCell);

                PdfPCell rrCell = new PdfPCell(new Phrase(String.format("%,.2f", retailRate), dataFont));
                rrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(rrCell);

                PdfPCell rvCell = new PdfPCell(new Phrase(String.format("%,.2f", retailValue), dataFont));
                rvCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(rvCell);
            }

            // Totals row
            PdfPCell totalLabel = new PdfPCell(new Phrase("Total", headerFont));
            totalLabel.setColspan(8); // First 8 columns
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalLabel);

            PdfPCell purchaseRateTotalCell = new PdfPCell(new Phrase("", headerFont));
            purchaseRateTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(purchaseRateTotalCell);

            PdfPCell purchaseValueTotalCell = new PdfPCell(new Phrase(String.format("%,.2f", stockPurchaseValue), headerFont));
            purchaseValueTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(purchaseValueTotalCell);

            PdfPCell costRateTotalCell = new PdfPCell(new Phrase("", headerFont));
            costRateTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(costRateTotalCell);

            PdfPCell costValueTotalCell = new PdfPCell(new Phrase(String.format("%,.2f", stockCostValue), headerFont));
            costValueTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(costValueTotalCell);

            PdfPCell retailRateTotalCell = new PdfPCell(new Phrase("", headerFont));
            retailRateTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(retailRateTotalCell);

            PdfPCell retailValueTotalCell = new PdfPCell(new Phrase(String.format("%,.2f", stockSaleValue), headerFont));
            retailValueTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(retailValueTotalCell);

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            Logger.getLogger(ReportsStock.class.getName()).log(Level.SEVERE, "Error exporting Current Stock By Batch PDF", e);
        }
    }

    public void toggleIncludeZeroStock() {
        includeZeroStock = !includeZeroStock;
    }

    public void fillDepartmentStocksForDownload() {
        reportTimerController.trackReportExecution(() -> {
            Date startedAt = new Date();

            Map<String, Object> parameters = new HashMap<>();
            StringBuilder jpql = new StringBuilder("SELECT s FROM Stock s WHERE 1=1");

            if (!includeZeroStock) {
                jpql.append(" AND s.stock > 0");
            }

            if (department != null) {
                jpql.append(" AND s.department = :dept");
                parameters.put("dept", department);
            }
            if (site != null) {
                jpql.append(" AND s.department.site = :site");
                parameters.put("site", site);
            }
            if (institution != null) {
                jpql.append(" AND s.department.institution = :ins");
                parameters.put("ins", institution);
            }

            jpql.append(" ORDER BY s.id");

            Date beforeJpql = new Date();

            stocks = getStockFacade().findByJpql(jpql.toString(), parameters);

        }, PharmacyReports.STOCK_REPORT_BY_BATCH, sessionController.getLoggedUser());
    }

    public void fillDepartmentStocksOfMedicines() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s "
                + " where s.department=:d "
                + " and s.stock>0 "
                + " and (s.itemBatch.item.departmentType is null or s.itemBatch.item.departmentType =:depty) "
                //                + " and s.itemBatch.item.departmentType!=:depty1 "
                //                + " and s.itemBatch.item.departmentType!=:depty2 "
                + " order by s.itemBatch.item.name";
        m.put("d", department);
        m.put("depty", DepartmentType.Pharmacy);
//        m.put("depty1", DepartmentType.Store);
//        m.put("depty2", DepartmentType.Inventry);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        stockCostValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
            stockCostValue = stockCostValue + ((ts.getItemBatch().getCostRate() == null ? 0 : ts.getItemBatch().getCostRate()) * ts.getStock());
        }

    }

    public String fillDepartmentNonEmptyStocksByVmp() {
        Map m = new HashMap();
        String sql;

        if (department == null) {
            sql = "select s from Stock s join TREAT(s.itemBatch.item as Amp) amp "
                    + "where s.stock>:z and amp.vmp=:vmp "
                    + "order by s.itemBatch.item.name";
        } else {
            sql = "select s from Stock s join TREAT(s.itemBatch.item as Amp) amp "
                    + "where s.stock>:z and s.department=:d and amp.vmp=:vmp "
                    + "order by s.itemBatch.item.name";
            m.put("d", department);
        }
        m.put("z", 0.0);
        m.put("vmp", vmp);
        //System.err.println("");
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        stockCostValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
            stockCostValue = stockCostValue + ((ts.getItemBatch().getCostRate() == null ? 0 : ts.getItemBatch().getCostRate()) * ts.getStock());
        }

        return "pharmacy_report_department_stock_by_single_product";

    }

    public void fillDepartmentNonEmptyProductStocks() {
        Map m = new HashMap();
        String sql;
        if (department == null) {
            sql = "select new com.divudi.core.data.dataStructure.PharmacyStockRow(vmp, sum(s.stock), "
                    + "sum(s.itemBatch.purcahseRate * s.stock), sum(s.itemBatch.retailsaleRate * s.stock))  "
                    + "from Stock s join s.itemBatch.item as amp join amp.vmp as vmp "
                    + "where s.stock>:z  "
                    + "group by vmp, vmp.name "
                    + "order by vmp.name";
            m.put("z", 0.0);
        } else {
            sql = "select new com.divudi.core.data.dataStructure.PharmacyStockRow(vmp, sum(s.stock), "
                    + "sum(s.itemBatch.purcahseRate * s.stock), sum(s.itemBatch.retailsaleRate * s.stock))  "
                    + "from Stock s join s.itemBatch.item as amp join amp.vmp as vmp "
                    + "where s.stock>:z and s.department=:d "
                    + "group by vmp, vmp.name "
                    + "order by vmp.name";
            m.put("d", department);
            m.put("z", 0.0);
        }
        List<PharmacyStockRow> lsts = (List) getStockFacade().findObjects(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (PharmacyStockRow r : lsts) {
            stockPurchaseValue += r.getPurchaseValue();
            stockSaleValue += r.getSaleValue();
        }
        pharmacyStockRows = lsts;

    }

    public void generateDepartmentStockOverviewReport() {

        startingPurchaseValue = 0.0;
        startingSaleValue = 0.0;

        //ALL GRN
        commonReport.setFromDate(findStartOfMonth(toDate));
        commonReport.setToDate(findEndofMonth(toDate));
        commonReport.setDepartment(department);
        commonReport.setDepartmentId("");
        commonReport.createGrnDetailTable();

        //GRN - Cash
        totalGrnCashPurchaseValue = 0.0;
        totalGrnCashRetailValue = 0.0;
        totalGrnCashPurchaseValue = commonReport.getGrnBilled().getCash() + commonReport.getGrnCancelled().getCash();
        totalGrnCashRetailValue = commonReport.getGrnBilled().getSaleCash() + commonReport.getGrnCancelled().getSaleCash();

        //GRN - Credit
        totalGrnCreditPurchaseValue = 0.0;
        totalGrnCreditRetailValue = 0.0;
        totalGrnCreditPurchaseValue = commonReport.getGrnBilled().getCredit() + commonReport.getGrnCancelled().getCredit();
        totalGrnCreditRetailValue = commonReport.getGrnBilled().getSaleCredit() + commonReport.getGrnCancelled().getSaleCredit();

        //Direct Purchase
        totalDirectPurchasePurchaseValue = 0.0;
        totalDirectPurchaseRetailValue = 0.0;
        calDirectPurchaseTotalsForOverViewReport();

        //Totals After GRN
        purchaseValueAfterGrn = Math.abs(totalGrnCashPurchaseValue) + Math.abs(totalGrnCreditPurchaseValue) + Math.abs(totalDirectPurchasePurchaseValue);
        saleValueAfterGrn = Math.abs(totalGrnCashRetailValue) + Math.abs(totalGrnCreditRetailValue) + Math.abs(totalDirectPurchaseRetailValue);

        //GRN Return
        totalGrnReturnPurchaseValue = 0.0;
        totalGrnReturnRetailValue = 0.0;
        totalGrnReturnPurchaseValue = (commonReport.getGrnReturn().getCash() + commonReport.getGrnReturn().getCredit()) + (commonReport.getGrnReturnCancel().getCash() + commonReport.getGrnReturnCancel().getCredit());
        totalGrnReturnRetailValue = (commonReport.getGrnReturn().getSaleCash() + commonReport.getGrnReturn().getSaleCredit()) + (commonReport.getGrnReturnCancel().getSaleCash() + commonReport.getGrnReturnCancel().getSaleCredit());

        //Free Qty
        totalGrnFreeQtyPurchaseValue = 0.0;
        totalGrnFreeQtyRetailValue = 0.0;
        calFreeQtyTotalsForOverViewReport();

        //Total After Free Qty and Returns
        purchaseValueAfterGrnReturnAndFreeQty = (0 - Math.abs(totalGrnReturnPurchaseValue)) + Math.abs(totalGrnFreeQtyPurchaseValue);
        saleValueAfterGrnReturnAndFreeQty = (0 - Math.abs(totalGrnReturnRetailValue)) + Math.abs(totalGrnFreeQtyRetailValue);

        //Sale
        saleTotalsForOverViewReport();

        //TotalAfterSales
        purchaseValueAfterSale = Math.abs(totalPurchaseSaleCashValue) + Math.abs(totalPurchaseSaleCreditValue);
        saleValueAfterSale = Math.abs(totalRetailSaleCashValue) + Math.abs(totalRetailSaleCreditValue);

        //Disbursment
        reportsTransfer.fetchBillTotalByToDepartment(findStartOfMonth(toDate), findEndofMonth(toDate), department, BillType.PharmacyTransferIssue);

        //TotalsAfterDisbursment
        purchaseValueAfterDisbursement = Math.abs(reportsTransfer.getNetTotalPurchaseValues());
        saleValueAfterDisbursement = Math.abs(reportsTransfer.getNetTotalSaleValues());

        //Final Count
        finalTotalPurchaseValue = startingPurchaseValue + purchaseValueAfterGrn + purchaseValueAfterGrnReturnAndFreeQty - (purchaseValueAfterSale + purchaseValueAfterDisbursement);
        finalTotalSaleValue = startingSaleValue + saleValueAfterGrn + saleValueAfterGrnReturnAndFreeQty - (saleValueAfterSale + saleValueAfterDisbursement);
    }

    public void calDirectPurchaseTotalsForOverViewReport() {
        String jpql;
        Map temMap = new HashMap();

        jpql = "select b"
                + " FROM Bill b "
                + " where b.department=:dept "
                + " and  b.billType= :bt "
                + " and (b.cancelled = false or b.refunded = false)"
                + " and  b.createdAt between :fromDate and :toDate ";
        temMap.put("toDate", findEndofMonth(toDate));
        temMap.put("fromDate", findStartOfMonth(toDate));
        temMap.put("dept", department);
        temMap.put("bt", BillType.PharmacyPurchaseBill);

        List<Bill> bills = billFacade.findByJpql(jpql, temMap, TemporalType.TIMESTAMP);

        if (!bills.isEmpty()) {
            for (Bill b : bills) {
                totalDirectPurchasePurchaseValue += b.getNetTotal();
                totalDirectPurchaseRetailValue += b.getSaleValue();
            }
        }

    }

    public void calFreeQtyTotalsForOverViewReport() {
        String jpql;
        Map temMap = new HashMap();

        jpql = "select bi"
                + " FROM BillItem bi "
                + " where bi.bill.department=:dept "
                + " and  bi.bill.billType in :bts "
                + " and (bi.bill.cancelled = false or bi.bill.refunded = false)"
                + " and  bi.bill.createdAt between :fromDate and :toDate ";
        temMap.put("toDate", findEndofMonth(toDate));
        temMap.put("fromDate", findStartOfMonth(toDate));
        temMap.put("dept", department);
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PharmacyGrnBill);
        bts.add(BillType.PharmacyPurchaseBill);
        temMap.put("bts", bts);

        List<BillItem> billitems = billItemFacade.findByJpql(jpql, temMap, TemporalType.TIMESTAMP);

        if (!billitems.isEmpty()) {
            for (BillItem bi : billitems) {
                totalGrnFreeQtyPurchaseValue += (bi.getPharmaceuticalBillItem().getFreeQty() * bi.getPharmaceuticalBillItem().getPurchaseRate());
                totalGrnFreeQtyRetailValue += (bi.getPharmaceuticalBillItem().getFreeQty() * bi.getPharmaceuticalBillItem().getRetailRate());
            }
        }

    }

    public void saleTotalsForOverViewReport() {
        String jpql;
        Map<String, Object> temMap = new HashMap<>();

        // Define the BillTypeAtomics you want to consider
        List<BillTypeAtomic> btas = Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_CANCELLED,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_REFUND,
                BillTypeAtomic.PHARMACY_WHOLESALE,
                BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED,
                BillTypeAtomic.PHARMACY_WHOLESALE_PRE,
                BillTypeAtomic.PHARMACY_WHOLESALE_REFUND,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_THEATRE
        );

        // Define the credit bill types
        List<BillTypeAtomic> creditBillTypeAtomics = Arrays.asList(
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_THEATRE
        );

        List<PaymentMethod> creditPaymentMethod = Arrays.asList(
                PaymentMethod.Credit,
                PaymentMethod.Staff);

        // The optimized JPQL using SUM and CASE WHEN
        jpql = "SELECT "
                + "SUM(CASE WHEN (b.paymentMethod IN :credit OR b.billTypeAtomic IN :creditTypes) "
                + "THEN (pbi.qty * pbi.purchaseRate) ELSE 0 END), "
                + "SUM(CASE WHEN (b.paymentMethod IN :credit OR b.billTypeAtomic IN :creditTypes) "
                + "THEN (pbi.qty * pbi.retailRate) ELSE 0 END), "
                + "SUM(CASE WHEN (b.paymentMethod NOT IN :credit AND b.billTypeAtomic NOT IN :creditTypes) "
                + "THEN (pbi.qty * pbi.purchaseRate) ELSE 0 END), "
                + "SUM(CASE WHEN (b.paymentMethod NOT IN :credit AND b.billTypeAtomic NOT IN :creditTypes) "
                + "THEN (pbi.qty * pbi.retailRate) ELSE 0 END) "
                + "FROM BillItem bi "
                + "JOIN bi.bill b "
                + "JOIN bi.pharmaceuticalBillItem pbi "
                + "WHERE b.department = :dept "
                + "AND b.billTypeAtomic IN :btas "
                + "AND (b.cancelled = false OR b.refunded = false) "
                + "AND b.createdAt BETWEEN :fromDate AND :toDate";

        // Set parameters
        temMap.put("toDate", findEndofMonth(toDate));
        temMap.put("fromDate", findStartOfMonth(toDate));
        temMap.put("dept", department);
        temMap.put("btas", btas);
        temMap.put("creditTypes", creditBillTypeAtomics);
        temMap.put("credit", creditPaymentMethod);

        // Run the query and retrieve results
        Object[] results = (Object[]) billItemFacade.findSingleAggregate(jpql, temMap, TemporalType.TIMESTAMP);

        // Assign results with null checks
        totalPurchaseSaleCreditValue = results[0] != null ? (Double) results[0] : 0.0;
        totalRetailSaleCreditValue = results[1] != null ? (Double) results[1] : 0.0;
        totalPurchaseSaleCashValue = results[2] != null ? (Double) results[2] : 0.0;
        totalRetailSaleCashValue = results[3] != null ? (Double) results[3] : 0.0;
    }

    public void fillDepartmentNonEmptyItemStocks() {
        reportTimerController.trackReportExecution(() -> {
            if (department == null) {
                JsfUtil.addErrorMessage("Please select a department");
                return;
            }
            Map m = new HashMap();
            String sql;
            sql = "select new com.divudi.core.data.dto.StockReportByItemDTO"
                    + "(s.itemBatch.item.code, "
                    + "s.itemBatch.item.name, "
                    + "sum(s.stock), "
                    + "sum(s.itemBatch.purcahseRate * s.stock), "
                    + "sum(s.itemBatch.retailsaleRate * s.stock))  "
                    + "from Stock s where s.stock>:z and s.department=:d "
                    + "group by s.itemBatch.item.name, s.itemBatch.item.code "
                    + "order by s.itemBatch.item.name";
            m.put("d", department);
            m.put("z", 0.0);
            List<StockReportByItemDTO> lsts = (List) getStockFacade().findLightsByJpql(sql, m);
            stockPurchaseValue = 0.0;
            stockSaleValue = 0.0;
            for (StockReportByItemDTO r : lsts) {
                stockPurchaseValue += r.getPurchaseValue();
                stockSaleValue += r.getSaleValue();

            }
            stockReportByItemDTOS = lsts;
        }, PharmacyReports.STOCK_REPORT_BY_ITEM, sessionController.getLoggedUser());
    }

    private List<StockReportByItemDTO> stockReportByItemDTOS;

    public List<StockReportByItemDTO> getStockReportByItemDTOS() {
        return stockReportByItemDTOS;
    }

    public void setStockReportByItemDTOS(List<StockReportByItemDTO> stockReportByItemDTOS) {
        this.stockReportByItemDTOS = stockReportByItemDTOS;
    }

    // Department Vice Stock Report fields
    private List<DepartmentViceStockDTO> departmentViceStockDtos;
    private double totalDepartmentViceStockQuantity;
    private double totalDepartmentViceStockPurchaseValue;
    private double totalDepartmentViceStockRetailValue;
    private double totalDepartmentViceStockCostValue;

    public List<DepartmentViceStockDTO> getDepartmentViceStockDtos() {
        return departmentViceStockDtos;
    }

    public void setDepartmentViceStockDtos(List<DepartmentViceStockDTO> departmentViceStockDtos) {
        this.departmentViceStockDtos = departmentViceStockDtos;
    }

    public double getTotalDepartmentViceStockQuantity() {
        return totalDepartmentViceStockQuantity;
    }

    public void setTotalDepartmentViceStockQuantity(double totalDepartmentViceStockQuantity) {
        this.totalDepartmentViceStockQuantity = totalDepartmentViceStockQuantity;
    }

    public double getTotalDepartmentViceStockPurchaseValue() {
        return totalDepartmentViceStockPurchaseValue;
    }

    public void setTotalDepartmentViceStockPurchaseValue(double totalDepartmentViceStockPurchaseValue) {
        this.totalDepartmentViceStockPurchaseValue = totalDepartmentViceStockPurchaseValue;
    }

    public double getTotalDepartmentViceStockRetailValue() {
        return totalDepartmentViceStockRetailValue;
    }

    public void setTotalDepartmentViceStockRetailValue(double totalDepartmentViceStockRetailValue) {
        this.totalDepartmentViceStockRetailValue = totalDepartmentViceStockRetailValue;
    }

    public double getTotalDepartmentViceStockCostValue() {
        return totalDepartmentViceStockCostValue;
    }

    public void setTotalDepartmentViceStockCostValue(double totalDepartmentViceStockCostValue) {
        this.totalDepartmentViceStockCostValue = totalDepartmentViceStockCostValue;
    }

    public void fillDepartmentZeroItemStocks() {
        reportTimerController.trackReportExecution(() -> {
            if (department == null && site == null && institution == null) {
                JsfUtil.addErrorMessage("Please select a department, site, or institution");
                return;
            }
            Map m = new HashMap();
            StringBuilder sql = new StringBuilder("select new com.divudi.core.data.dataStructure.PharmacyStockRow("
                    + "s.itemBatch.item.code, "
                    + "s.itemBatch.item.name, "
                    + "sum(s.stock), "
                    + "sum(s.itemBatch.purcahseRate * s.stock), "
                    + "sum(s.itemBatch.retailsaleRate * s.stock)) "
                    + "from Stock s where 1=1 ");
            if (department != null) {
                sql.append(" and s.department=:d");
                m.put("d", department);
            }
            if (site != null) {
                sql.append(" and s.department.site=:site");
                m.put("site", site);
            }
            if (institution != null) {
                sql.append(" and s.department.institution=:ins");
                m.put("ins", institution);
            }
            sql.append(" group by s.itemBatch.item.code, s.itemBatch.item.name having sum(s.stock) = 0 order by s.itemBatch.item.name");
            List<PharmacyStockRow> lsts = (List) getStockFacade().findObjects(sql.toString(), m);
            stockPurchaseValue = 0.0;
            stockSaleValue = 0.0;
            for (PharmacyStockRow r : lsts) {
                stockPurchaseValue += r.getPurchaseValue();
                stockSaleValue += r.getSaleValue();
            }
            pharmacyStockRows = lsts;
        }, PharmacyReports.STOCK_REPORT_BY_ITEM, sessionController.getLoggedUser());
    }

    public void fillDepartmentStockByItemOrderByVmp() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select new com.divudi.core.data.dataStructure.PharmacyStockRow"
                + "(s.itemBatch.item.vmp.name, "
                + "s.itemBatch.item.code, "
                + "s.itemBatch.item.name, "
                + "sum(s.stock), "
                + "sum(s.itemBatch.purcahseRate * s.stock), "
                + "sum(s.itemBatch.retailsaleRate * s.stock))  "
                + "from Stock s where s.stock>:z and s.department=:d "
                + "group by s.itemBatch.item "
                + "order by s.itemBatch.item.vmp.name, s.itemBatch.item.name";
        m.put("d", department);
        m.put("z", 0.0);
        List<PharmacyStockRow> lsts = (List) getStockFacade().findObjects(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (PharmacyStockRow r : lsts) {
            stockPurchaseValue += r.getPurchaseValue();
            stockSaleValue += r.getSaleValue();

        }
        pharmacyStockRows = lsts;

    }

    public void fillDepartmentViceStockDtos() {
        reportTimerController.trackReportExecution(() -> {
            Map<String, Object> m = new HashMap<>();
            StringBuilder jpql = new StringBuilder("SELECT new com.divudi.core.data.dto.DepartmentViceStockDTO(");
            jpql.append("d.institution.name, ");
            jpql.append("d.site.name, ");
            jpql.append("d.name, ");
            jpql.append("s.itemBatch.item.departmentType, ");
            jpql.append("SUM(s.stock), ");
            jpql.append("SUM(s.stock * COALESCE(s.itemBatch.purcahseRate, 0.0)), ");
            jpql.append("SUM(s.stock * COALESCE(s.itemBatch.retailsaleRate, 0.0)), ");
            jpql.append("SUM(s.stock * COALESCE(s.itemBatch.costRate, 0.0))) ");
            jpql.append("FROM Stock s JOIN s.department d ");
            jpql.append("WHERE s.stock > 0 ");
            jpql.append("GROUP BY d.institution.name, d.site.name, d.name, s.itemBatch.item.departmentType ");
            jpql.append("ORDER BY d.institution.name, d.site.name, d.name, s.itemBatch.item.departmentType");

            List<DepartmentViceStockDTO> dtos = (List<DepartmentViceStockDTO>) stockFacade.findLightsByJpql(jpql.toString(), m);

            // Set serial numbers and calculate totals
            int serialNo = 1;
            totalDepartmentViceStockQuantity = 0.0;
            totalDepartmentViceStockPurchaseValue = 0.0;
            totalDepartmentViceStockRetailValue = 0.0;
            totalDepartmentViceStockCostValue = 0.0;

            for (DepartmentViceStockDTO dto : dtos) {
                dto.setSerialNo(serialNo++);
                if (dto.getQuantity() != null) {
                    totalDepartmentViceStockQuantity += dto.getQuantity();
                }
                if (dto.getPurchaseValue() != null) {
                    totalDepartmentViceStockPurchaseValue += dto.getPurchaseValue();
                }
                if (dto.getRetailValue() != null) {
                    totalDepartmentViceStockRetailValue += dto.getRetailValue();
                }
                if (dto.getCostValue() != null) {
                    totalDepartmentViceStockCostValue += dto.getCostValue();
                }
            }

            departmentViceStockDtos = dtos;
        }, PharmacyReports.DEPARTMENT_VICE_STOCK_REPORT, sessionController.getLoggedUser());
    }

    public void fillDepartmentInventryStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s"
                + " where s.department=:d"
                + " and s.itemBatch.item.departmentType=:depty "
                + " order by s.itemBatch.item.name";

        m.put("depty", DepartmentType.Inventry);
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
//        for (Stock ts : stocks) {
//            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
//            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
//        }
    }

    public void fillDepartmentStocksMinus() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.stock<0 and s.department=:d order by s.itemBatch.item.name";
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        stockCostValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
            stockCostValue = stockCostValue + ((ts.getItemBatch().getCostRate() == null ? 0 : ts.getItemBatch().getCostRate()) * ts.getStock());
        }

    }

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private StockHistoryFacade stockHistoryFacade;

    private PharmaceuticalBillItem getPreviousPharmacuticalBillByBatch(ItemBatch itemBatch, Department department, Date date) {
        String sql = "Select sh from PharmaceuticalBillItem sh where "
                + " sh.itemBatch=:itmB and sh.billItem.bill.department=:dep "
                + " and (sh.billItem.bill.billType=:btp1 or sh.billItem.bill.billType=:btp2 )"
                + "  and sh.billItem.createdAt between :fd and :td "
                + " order by sh.billItem.createdAt desc";
        HashMap hm = new HashMap();
        hm.put("itmB", itemBatch);
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.MONTH, 1);
        cl.set(Calendar.DAY_OF_MONTH, 26);
        hm.put("td", date);
        hm.put("fd", cl.getTime());
        hm.put("dep", department);
        hm.put("btp1", BillType.PharmacyGrnBill);
        hm.put("btp2", BillType.PharmacyPurchaseBill);
        return getPharmaceuticalBillItemFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    private StockHistory getPreviousStockHistoryByBatch(ItemBatch itemBatch, Department department, Date date) {
        String sql = "Select sh from StockHistory sh where sh.retired=false and"
                + " sh.itemBatch=:itmB and sh.department=:dep and sh.pbItem.billItem.createdAt<:dt "
                + " order by sh.pbItem.billItem.createdAt desc";
        HashMap hm = new HashMap();
        hm.put("itmB", itemBatch);
        hm.put("dt", date);
        hm.put("dep", department);
        return getStockHistoryFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public void fillDepartmentStocksError() {
        Set<Stock> stockSet = new HashSet<>();
        String sql;
        Map temMap = new HashMap();

        sql = "select p from PharmaceuticalBillItem p where "
                + " p.billItem.bill.department=:dep "
                + " and p.billItem.createdAt>:date and "
                + "  p.stockHistory is not null order by p.stockHistory.id ";

        temMap.put("dep", department);
        temMap.put("date", date);

        List<PharmaceuticalBillItem> list = getPharmaceuticalBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (PharmaceuticalBillItem b : list) {
            StockHistory sh = getPreviousStockHistoryByBatch(b.getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());
            PharmaceuticalBillItem phi = getPreviousPharmacuticalBillByBatch(b.getStock().getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());

            double calculatedStk = 0;
            boolean flg = false;
            if (sh != null) {
                calculatedStk = (sh.getStockQty() + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit());
                flg = true;
            } else if (phi != null) {
                calculatedStk = phi.getQtyInUnit() + phi.getFreeQtyInUnit();
                flg = true;
            }

            if (flg && b.getStockHistory().getStockQty() != calculatedStk) {
                stockSet.add(b.getStock());
            }
        }

        stocks = new ArrayList<>(stockSet);
    }

    public void fillDepartmentStocksError2() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department=:d order by s.itemBatch.item.name";
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        Set<Stock> tmpStockList = new HashSet<>();

        for (Stock st : stocks) {
            sql = "Select ph from PharmaceuticalBillItem ph where ph.stock=:st "
                    + " and ph.billItem.createdAt>:date  "
                    + " and ph.stockHistory is not null  "
                    + " order by ph.stockHistory.id ";

            m.clear();
            m.put("st", st);
            m.put("date", date);

            List<PharmaceuticalBillItem> phList = getPharmaceuticalBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

            PharmaceuticalBillItem previousPh = null;
            double calculatedStock = 0;

            for (PharmaceuticalBillItem ph : phList) {
                if (previousPh == null) {
                    previousPh = ph;
                    calculatedStock = ph.getStockHistory().getStockQty();
                    continue;
                }
                double preHistoryQty = 0;
                double curHistory = 0;

                if (previousPh.getStockHistory() != null) {
                    preHistoryQty = previousPh.getStockHistory().getStockQty();
                }

                if (ph.getStockHistory() != null) {
                    curHistory = ph.getStockHistory().getStockQty();
                }

                double calcualtedQty = preHistoryQty + previousPh.getQtyInUnit() + previousPh.getFreeQtyInUnit();

                switch (ph.getBillItem().getBill().getBillType()) {
                    case PharmacyGrnBill:
                    case PharmacyPurchaseBill:
                    case PharmacyTransferReceive:
                        if (ph.getBillItem().getBill() instanceof BilledBill) {
                            calculatedStock += Math.abs(ph.getQtyInUnit());
                            calculatedStock += Math.abs(ph.getFreeQtyInUnit());
                        } else if (ph.getBillItem().getBill() instanceof CancelledBill || ph.getBillItem().getBill() instanceof RefundBill) {
                            calculatedStock -= Math.abs(ph.getQtyInUnit());
                            calculatedStock -= Math.abs(ph.getFreeQtyInUnit());
                        }
                        break;
                    case PharmacyGrnReturn:
                    case PurchaseReturn:
                    case PharmacyTransferIssue:
                        if (ph.getBillItem().getBill() instanceof BilledBill) {
                            calculatedStock -= Math.abs(ph.getQtyInUnit());
                            calculatedStock -= Math.abs(ph.getFreeQtyInUnit());
                        } else if (ph.getBillItem().getBill() instanceof CancelledBill || ph.getBillItem().getBill() instanceof RefundBill) {
                            calculatedStock += Math.abs(ph.getQtyInUnit());
                            calculatedStock += Math.abs(ph.getFreeQtyInUnit());
                        }
                        break;
                    case PharmacyPre:
                        if (ph.getBillItem().getBill() instanceof PreBill) {
                            if (ph.getBillItem().getBill().getReferenceBill() == null) {
                                break;
                            }
                            calculatedStock -= Math.abs(ph.getQtyInUnit());

                        } else if (ph.getBillItem().getBill() instanceof CancelledBill || ph.getBillItem().getBill() instanceof RefundBill) {
                            calculatedStock += Math.abs(ph.getQtyInUnit());
                        }
                        break;
                    default:

                }

                if (calcualtedQty != curHistory) {
                    st.setCalculated(calculatedStock);
                    tmpStockList.add(st);
                }

                previousPh = ph;
            }

        }

        stocks = new ArrayList<>(tmpStockList);
    }

    private Date date;

    public void fillDepartmentExpiaryStocks() {
        reportTimerController.trackReportExecution(() -> {
            if (department == null) {
                JsfUtil.addErrorMessage("Please select a department");
                return;
            }
            Map m = new HashMap();
            String sql;
            sql = "select s "
                    + " from Stock s "
                    + " where s.stock > :st "
                    + " and s.department=:d "
                    + " and s.itemBatch.dateOfExpire "
                    + " between :fd and :td "
                    + " order by s.itemBatch.dateOfExpire";
            m.put("d", department);
            m.put("fd", getFromDate());
            m.put("td", getToDate());
            m.put("st", 0.0);
            stocks = getStockFacade().findByJpql(sql, m);
            stockPurchaseValue = 0.0;
            stockSaleValue = 0.0;
            stockCostValue = 0.0;
            for (Stock ts : stocks) {
                stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
                stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
                stockCostValue = stockCostValue + ((ts.getItemBatch().getCostRate() == null ? 0 : ts.getItemBatch().getCostRate()) * ts.getStock());
            }
        }, PharmacyReports.STOCK_REPORT_BY_EXPIRY, sessionController.getLoggedUser());
    }

    public void addComment(Stock st) {
        if (st != null) {
            getStockFacade().edit(st);
            JsfUtil.addSuccessMessage("Edit Successful");
        }
    }

    @EJB
    ItemFacade itemFacade;

    List<Item> items;
    private Item item;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void fillDepartmentNonmovingStocks() {
        Date startTime = new Date();

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "SELECT bi.item "
                + " FROM BillItem bi "
                + " WHERE  "
                + " bi.bill.department=:d "
                + " AND bi.bill.billType in :bts "
                + " AND bi.bill.billDate between :fd and :td ";
        m.put("d", department);
        m.put("bts", Arrays.asList(billTypes));
        m.put("fd", getFromDateE());
        m.put("td", getToDateE());
        if (category != null) {
            sql += " AND bi.item.category=:cat ";
            m.put("cat", category);
        }
        sql += " GROUP BY bi.item";

        Set<Item> bis = new HashSet<>(itemFacade.findByJpql(sql, m));

        sql = "SELECT s.itemBatch.item "
                + " FROM Stock s "
                + " WHERE s.department=:d "
                + " AND s.stock > 0 ";
        m = new HashMap();
        m.put("d", department);
        if (category != null) {
            sql += " AND s.itemBatch.item.category=:cat ";
            m.put("cat", category);
        }
        sql = sql + " GROUP BY s.itemBatch.item "
                + " ORDER BY s.itemBatch.item.name";

        Set<Item> sis = new HashSet<>(itemFacade.findByJpql(sql, m));

        sis.removeAll(bis);
        items = new ArrayList<>(sis);

        Collections.sort(items);

        Map<String, Object> h = new HashMap<>();
        String jpql = "select s from Stock s "
                + "where s.itemBatch.item in :items "
                + " and s.stock > 0 ";
        h.put("items", items);
        stocks = getStockFacade().findByJpql(jpql, h);
    }

    public void fillStaffStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (staff == null) {
            JsfUtil.addErrorMessage("Please select a staff member");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.staff=:d order by s.itemBatch.item.name";
        m.put("d", staff);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

    }

    public void fillAllStaffStocks() {
        Map<String, Object> m = new HashMap<>();
        StringBuilder sql = new StringBuilder("select s from Stock s where s.stock!=:d");

        // Add staff filter if staff is selected
        if (staff != null) {
            sql.append(" and s.staff=:staff");
            m.put("staff", staff);
        }

        // Add item filter if item is selected
        if (item != null) {
            sql.append(" and s.itemBatch.item=:item");
            m.put("item", item);
        }

        sql.append(" order by s.staff.person.name, s.itemBatch.item.name");

        m.put("d", 0.0);
        stocks = getStockFacade().findByJpql(sql.toString(), m);

        // Calculate totals
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        stockCostValue = 0.0;

        for (Stock ts : stocks) {
            stockPurchaseValue += (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue += (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
            stockCostValue += ((ts.getItemBatch().getCostRate() == null ? 0 : ts.getItemBatch().getCostRate()) * ts.getStock());
        }
    }

    public void clearFilters() {
        staff = null;
        itemController.setCurrent(null);
        stocks = new ArrayList<>();
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        stockCostValue = 0.0;
        JsfUtil.addSuccessMessage("Filters cleared successfully");
    }

    public void fillAllStaffStockItems() {
        //TODO: Improve Logic. This Logic is wrong.

        Map<String, Object> m = new HashMap<>();
        String sql = "select bi from BillItem bi"
                + " where bi.bill.billType = :bt"
                + " and bi.retired = :ret"
                + " and bi.bill.toStaff is not null"
                + " and bi.bill.fromDepartment is not null"
                + " and bi.bill.forwardReferenceBills is empty"
                + " order by bi.bill.toStaff.person.name";
        m.put("bt", BillType.PharmacyTransferIssue);
        m.put("ret", false);
        billItems = billItemFacade.findByJpql(sql, m);
    }

    public void fillDistributorExpiryStocks() {

        if (department == null || institution == null) {
            JsfUtil.addErrorMessage("Please select a department and distributor");
            return;
        }

        Map<String, Object> m = new HashMap<>();
        String sql;

        sql = "select s "
                + "from Stock s "
                + "where s.stock > :st "
                + "and s.department = :dep "
                + "and s.itemBatch.dateOfExpire between :fd and :td "
                + "and s.itemBatch.item.id in ("
                + "    select item.id "
                + "    from ItemsDistributors id join id.item as item "
                + "    where id.retired = false "
                + "    and id.institution = :ins"
                + ") "
                + "order by s.itemBatch.dateOfExpire";

        m.put("dep", department);
        m.put("ins", institution);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("st", 0.0);

        stocks = getStockFacade().findByJpql(sql, m);

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue += ts.getItemBatch().getPurcahseRate() * ts.getStock();
            stockSaleValue += ts.getItemBatch().getPurcahseRate() * ts.getStock();
        }

    }

    public String fillDistributorStocks() {
        if (department == null || institution == null) {
            JsfUtil.addErrorMessage("Please select a department && Dealor");
            return "";
        }
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();
        m.put("ins", institution);
        m.put("dep", department);
        m.put("st", 0.0);
        sql = "select s "
                + " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " and s.itemBatch.item.id in "
                + "(select item.id "
                + " from ItemsDistributors id join id.item as item "
                + " where id.retired=false "
                + " and id.institution=:ins)";
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

        return "/pharmacy/pharmacy_report_supplier_stock_by_batch";
    }

    public String fillSupplierStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return "";
        }
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();

        m.put("dep", department);
        m.put("st", 0.0);
        sql = "select s "
                + " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st ";

        if (institution != null) {
            sql += "and s.itemBatch.lastPurchaseBillItem.bill.fromInstitution =:ins";
            m.put("ins", institution);
        }
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

        return "/pharmacy/pharmacy_report_stock_report_with_supplier?faces-redirect=true";
    }

    public void fillCategoryStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (department == null || category == null) {
            JsfUtil.addErrorMessage("Please select a department && Category");
            return;
        }
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();
        m.put("cat", category);
        m.put("dep", department);
        m.put("st", 0.0);
        sql = "select s "
                + " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " and s.itemBatch.item.category=:cat "
                + " order by s.itemBatch.item.name";
        stocks = getStockFacade().findByJpql(sql, m);
        totalQty = 0.0;
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;

        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
            totalQty += ts.getStock();
            totalPurchaseRate += ts.getItemBatch().getPurcahseRate();
            totalRetailSaleRate += ts.getItemBatch().getRetailsaleRate();
            totalPurchaseValue += ts.getItemBatch().getPurcahseRate() * ts.getStock();
            totalRetailSaleValue += ts.getItemBatch().getRetailsaleRate() * ts.getStock();
        }
        getReportKeyWord().setBool1(false);

    }

    public void fillCategoryStocksNew() {

        if (department == null || category == null) {
            JsfUtil.addErrorMessage("Please select a department && Category");
            return;
        }
        Map m;
        String sql = "";

        m = new HashMap();
        m.put("cat", category);
        m.put("dep", department);
        m.put("st", 0.0);
        if (getReportKeyWord().getString().equals("0")) {
            sql = "select s ";
        } else if (getReportKeyWord().getString().equals("1")) {
            sql = "select s.itemBatch.item, sum(s.stock) ";
        } else if (getReportKeyWord().getString().equals("2")) {
            sql = "select s.itemBatch.item, sum(s.stock), s.itemBatch.retailsaleRate ";
        }
        sql += " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " and s.itemBatch.item.category=:cat ";

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        totalQty = 0.0;
        totalPurchaseRate = 0.0;
        totalRetailSaleRate = 0.0;
        totalPurchaseValue = 0.0;
        totalRetailSaleValue = 0.0;

        if (getReportKeyWord().getString().equals("0")) {
            sql += " order by s.itemBatch.item.name";
            stocks = getStockFacade().findByJpql(sql, m);

            for (Stock ts : stocks) {
                stockPurchaseValue += (ts.getItemBatch().getPurcahseRate() * ts.getStock());
                stockSaleValue += (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
                totalQty += ts.getStock();
                totalPurchaseRate += ts.getItemBatch().getPurcahseRate();
                totalRetailSaleRate += ts.getItemBatch().getRetailsaleRate();
                totalPurchaseValue += ts.getItemBatch().getPurcahseRate() * ts.getStock();
                totalRetailSaleValue += ts.getItemBatch().getRetailsaleRate() * ts.getStock();
            }
            getReportKeyWord().setBool1(false);
        } else if (getReportKeyWord().getString().equals("1") || getReportKeyWord().getString().equals("2")) {
            if (getReportKeyWord().getString().equals("1")) {
                sql += " group by s.itemBatch.item ";
            }
            if (getReportKeyWord().getString().equals("2")) {
                sql += " group by s.itemBatch.item, s.itemBatch.retailsaleRate ";
            }
            sql += " order by s.itemBatch.item.name ";
            List<Object[]> objects = getStockFacade().findAggregates(sql, m);
            stocks = new ArrayList<>();
            for (Object[] ob : objects) {
                Item i = (Item) ob[0];
                double d = (double) ob[1];
                Stock s = new Stock();
                ItemBatch ib = new ItemBatch();
                if (getReportKeyWord().getString().equals("2")) {
                    double saleRate = (double) ob[2];
                    ib.setRetailsaleRate(saleRate);
                }
                ib.setItem(i);
                s.setItemBatch(ib);

                s.setStock(d);

                if (i != null) {
                    s.setItemName(i.getName() != null ? i.getName() : "UNKNOWN");
                    s.setBarcode(i.getBarcode() != null ? i.getBarcode() : "");
                    String code = i.getCode();
                    Long longCode = CommonFunctions.stringToLong(code);
                    s.setLongCode(longCode);
                    s.setDateOfExpire(ib.getDateOfExpire());
                    s.setRetailsaleRate(ib.getRetailsaleRate());
                } else {
                    s.setItemName("UNKNOWN");
                    s.setBarcode("");
                    s.setLongCode(0L);
                }

                stocks.add(s);
                totalQty += d;
            }
            getReportKeyWord().setBool1(true);
        } else if (getReportKeyWord().getString().equals("3")) {
            stocks = new ArrayList<>();
            for (int i = 0; i < 70; i++) {
                Stock s = new Stock();
                s.setStock(1.0);
                stocks.add(s);
            }
        } else if (getReportKeyWord().getString().equals("4")) {
            stocks = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                Stock s = new Stock();
                s.setStock(1.0);
                stocks.add(s);
            }
        }

    }

    List<StockReportRecord> stockRecords;

    public List<StockReportRecord> getStockRecords() {
        return stockRecords;
    }

    public void setStockRecords(List<StockReportRecord> stockRecords) {
        this.stockRecords = stockRecords;
    }

    public void fillCategoryStocksByCatagory() {
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();
        m.put("dep", department);
        m.put("st", 0.0);
        sql = "select sum(s.stock * s.itemBatch.purcahseRate), "
                + " s.itemBatch.item.category "
                + " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " group by s.itemBatch.item.category "
                + " order by s.itemBatch.item.category.name";
        List<Object[]> objs = getStockFacade().findAggregates(sql, m);
        totalPurchaseValue = 0.0;
        stockRecords = new ArrayList<>();

        for (Object[] obj : objs) {
            Double sv = (Double) obj[0];
            Category c = (Category) obj[1];
            StockReportRecord r = new StockReportRecord();
            r.setCategory(c);
            r.setStockOnHand(sv);
            stockRecords.add(r);
            totalPurchaseValue += sv;
        }

    }

    public void fillAllDistributorStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m;
        String sql;
        records = new ArrayList<>();
        List<Institution> dealers = getDealerController().getItems();
        stockSaleValue = 0.0;
        stockPurchaseValue = 0.0;
        for (Institution i : dealers) {
            m = new HashMap();
            m.put("ins", i);
            m.put("d", department);
            sql = "select sum(s.stock),sum(s.stock * s.itemBatch.purcahseRate),sum(s.stock * s.itemBatch.retailsaleRate)"
                    + " from Stock s where s.department=:d and s.itemBatch.item.id in (select item.id from ItemsDistributors id join id.item as item where id.retired=false and id.institution=:ins)"
                    + " ";
            Object[] objs = getStockFacade().findSingleAggregate(sql, m);

            if (objs[0] != null && (Double) objs[0] > 0) {
                StockReportRecord r = new StockReportRecord();
                r.setInstitution(i);
                r.setQty((Double) objs[0]);
                r.setPurchaseValue((Double) objs[1]);
                r.setRetailsaleValue((Double) objs[2]);
                records.add(r);
                stockPurchaseValue = stockPurchaseValue + r.getPurchaseValue();
                stockSaleValue = stockSaleValue + r.getRetailsaleValue();
            }
        }

    }

    public void fillAllSuppliersStocks() {

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }

        Map m;
        String sql;
        records = new ArrayList<>();
        List<Institution> dealers = getDealerController().getItems();
        stockSaleValue = 0.0;
        stockPurchaseValue = 0.0;
        for (Institution i : dealers) {
            m = new HashMap();
            m.put("ins", i);
            m.put("d", department);
            sql = "select sum(s.stock),sum(s.stock * s.itemBatch.purcahseRate),sum(s.stock * s.itemBatch.retailsaleRate)"
                    + " from Stock s "
                    + " where s.department=:d "
                    + " and s.itemBatch.lastPurchaseBillItem.bill.fromInstitution =:ins";
            Object[] objs = getStockFacade().findSingleAggregate(sql, m);

            if (objs[0] != null && (Double) objs[0] > 0) {
                StockReportRecord r = new StockReportRecord();
                r.setInstitution(i);
                r.setQty((Double) objs[0]);
                r.setPurchaseValue((Double) objs[1]);
                r.setRetailsaleValue((Double) objs[2]);
                records.add(r);
                stockPurchaseValue = stockPurchaseValue + r.getPurchaseValue();
                stockSaleValue = stockSaleValue + r.getRetailsaleValue();
            }
        }

    }

    /**
     * Getters & Setters
     *
     * @return
     */
    @Override
    public Department getDepartment() {
        return department;
    }

    @Override
    public void setDepartment(Department department) {
        this.department = department;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public List<DepartmentType> getSelectedDepartmentTypes() {
        if (selectedDepartmentTypes == null) {
            selectedDepartmentTypes = new ArrayList<>();
        }
        return selectedDepartmentTypes;
    }

    public void setSelectedDepartmentTypes(List<DepartmentType> selectedDepartmentTypes) {
        this.selectedDepartmentTypes = selectedDepartmentTypes;
    }

    public String getSelectedDepartmentTypesPrintDisplay() {
        List<DepartmentType> list = getSelectedDepartmentTypes();
        if (list.isEmpty()) {
            return "All";
        }

        return list.stream()
                .map(DepartmentType::getLabel)
                .collect(Collectors.joining(", "));
    }

    public List<DepartmentType> getAvailableDepartmentTypes() {
        return Arrays.asList(
                DepartmentType.Pharmacy,
                DepartmentType.Store,
                DepartmentType.Lab,
                DepartmentType.Kitchen
        );
    }

    @Override
    public Institution getInstitution() {
        return institution;
    }

    @Override
    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public List<StockDTO> getStockDtos() {
        return stockDtos;
    }

    public void setStockDtos(List<StockDTO> stockDtos) {
        this.stockDtos = stockDtos;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    /**
     * Constructor
     */
    public ReportsStock() {
    }

    public double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public double getStockCostValue() {
        return stockCostValue;
    }

    public void setStockCostValue(double stockCostValue) {
        this.stockCostValue = stockCostValue;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<StockReportRecord> getRecords() {
        return records;
    }

    public void setRecords(List<StockReportRecord> records) {
        this.records = records;
    }

    public DealerController getDealerController() {
        return dealerController;
    }

    public void setDealerController(DealerController dealerController) {
        this.dealerController = dealerController;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public DosageForm getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(DosageForm dosageForm) {
        this.dosageForm = dosageForm;
    }

    @Override
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = Calendar.getInstance().getTime();
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
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 3);
            toDate = c.getTime();
        }
        return toDate;
    }

    public void fillThreeMonthsExpiary() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 3);
        toDate = c.getTime();
        fillDepartmentExpiaryStocks();;
    }

    public void fillSixMonthsExpiary() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 6);
        toDate = c.getTime();
        fillDepartmentExpiaryStocks();;
    }

    public void fillOneYearExpiary() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
        toDate = c.getTime();
        fillDepartmentExpiaryStocks();;
    }

    public void fillThreeMonthsExpiaryOfSupplier() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 3);
        toDate = c.getTime();
        fillDistributorExpiryStocks();
    }

    public void fillSixMonthsExpiaryOfSupplier() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 6);
        toDate = c.getTime();
        fillDistributorExpiryStocks();
    }

    public void fillOneYearExpiaryOfSupplier() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
        toDate = c.getTime();
        fillDistributorExpiryStocks();
    }

    public void fillThreeMonthsNonmoving() {
        Date startTime = new Date();

        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 3);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();

    }

    public void fillSixMonthsNonmoving() {
        Date startTime = new Date();

        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 6);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();

    }

    public void fillOneYearNonmoving() {
        Date startTime = new Date();

        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 1);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();

    }

    public Date findStartOfMonth(Date date) {
        return CommonFunctions.getStartOfMonth(date);
    }

    public Date findEndofMonth(Date date) {
        return CommonFunctions.getEndOfMonth(date);
    }

    @Override
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDateE() {
        if (fromDateE == null) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 3);
            fromDateE = c.getTime();
        }
        return fromDateE;
    }

    public void setFromDateE(Date fromDateE) {
        this.fromDateE = fromDateE;
    }

    public Date getToDateE() {
        if (toDateE == null) {
            toDateE = Calendar.getInstance().getTime();
        }
        return toDateE;
    }

    public void setToDateE(Date toDateE) {
        this.toDateE = toDateE;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public double getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(double totalQty) {
        this.totalQty = totalQty;
    }

    public double getTotalPurchaseRate() {
        return totalPurchaseRate;
    }

    public void setTotalPurchaseRate(double totalPurchaseRate) {
        this.totalPurchaseRate = totalPurchaseRate;
    }

    public double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public double getTotalRetailSaleRate() {
        return totalRetailSaleRate;
    }

    public void setTotalRetailSaleRate(double totalRetailSaleRate) {
        this.totalRetailSaleRate = totalRetailSaleRate;
    }

    public double getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(double totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public List<PharmacyStockRow> getPharmacyStockRows() {
        return pharmacyStockRows;
    }

    public void setPharmacyStockRows(List<PharmacyStockRow> pharmacyStockRows) {
        this.pharmacyStockRows = pharmacyStockRows;
    }

    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    boolean paginator = true;
    int rows = 20;

    public boolean isPaginator() {
        return paginator;
    }

    public void setPaginator(boolean paginator) {
        this.paginator = paginator;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void prepareForPrint() {
        paginator = false;
        if (stocks != null && !stocks.isEmpty()) {
            rows = stocks.size();
        } else if (stockDtos != null && !stockDtos.isEmpty()) {
            rows = stockDtos.size();
        } else {
            rows = 0;
        }
    }

    public void prepareForView() {
        paginator = true;
        rows = 20;
    }

    public BillType[] getBillTypes() {
        if (billTypes == null) {
            billTypes = new BillType[]{BillType.PharmacySale, BillType.PharmacyIssue, BillType.PharmacyPre, BillType.PharmacyWholesalePre};
        }
        return billTypes;
    }

    public void setBillTypes(BillType[] billTypes) {
        this.billTypes = billTypes;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBills(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public int getFromRecord() {
        return fromRecord;
    }

    public void setFromRecord(int fromRecord) {
        this.fromRecord = fromRecord;
    }

    public int getToRecord() {
        return toRecord;
    }

    public void setToRecord(int toRecord) {
        this.toRecord = toRecord;
    }

    public double getTotalGrnCashPurchaseValue() {
        return totalGrnCashPurchaseValue;
    }

    public void setTotalGrnCashPurchaseValue(double totalGrnCashPurchaseValue) {
        this.totalGrnCashPurchaseValue = totalGrnCashPurchaseValue;
    }

    public double getTotalGrnCashRetailValue() {
        return totalGrnCashRetailValue;
    }

    public void setTotalGrnCashRetailValue(double totalGrnCashRetailValue) {
        this.totalGrnCashRetailValue = totalGrnCashRetailValue;
    }

    public double getTotalGrnCreditPurchaseValue() {
        return totalGrnCreditPurchaseValue;
    }

    public void setTotalGrnCreditPurchaseValue(double totalGrnCreditPurchaseValue) {
        this.totalGrnCreditPurchaseValue = totalGrnCreditPurchaseValue;
    }

    public double getTotalGrnCreditRetailValue() {
        return totalGrnCreditRetailValue;
    }

    public void setTotalGrnCreditRetailValue(double totalGrnCreditRetailValue) {
        this.totalGrnCreditRetailValue = totalGrnCreditRetailValue;
    }

    public double getTotalGrnReturnPurchaseValue() {
        return totalGrnReturnPurchaseValue;
    }

    public void setTotalGrnReturnPurchaseValue(double totalGrnReturnPurchaseValue) {
        this.totalGrnReturnPurchaseValue = totalGrnReturnPurchaseValue;
    }

    public double getTotalGrnReturnRetailValue() {
        return totalGrnReturnRetailValue;
    }

    public void setTotalGrnReturnRetailValue(double totalGrnReturnRetailValue) {
        this.totalGrnReturnRetailValue = totalGrnReturnRetailValue;
    }

    public double getTotalDirectPurchasePurchaseValue() {
        return totalDirectPurchasePurchaseValue;
    }

    public void setTotalDirectPurchasePurchaseValue(double totalDirectPurchasePurchaseValue) {
        this.totalDirectPurchasePurchaseValue = totalDirectPurchasePurchaseValue;
    }

    public double getTotalDirectPurchaseRetailValue() {
        return totalDirectPurchaseRetailValue;
    }

    public void setTotalDirectPurchaseRetailValue(double totalDirectPurchaseRetailValue) {
        this.totalDirectPurchaseRetailValue = totalDirectPurchaseRetailValue;
    }

    public double getTotalGrnFreeQtyPurchaseValue() {
        return totalGrnFreeQtyPurchaseValue;
    }

    public void setTotalGrnFreeQtyPurchaseValue(double totalGrnFreeQtyPurchaseValue) {
        this.totalGrnFreeQtyPurchaseValue = totalGrnFreeQtyPurchaseValue;
    }

    public double getTotalGrnFreeQtyRetailValue() {
        return totalGrnFreeQtyRetailValue;
    }

    public void setTotalGrnFreeQtyRetailValue(double totalGrnFreeQtyRetailValue) {
        this.totalGrnFreeQtyRetailValue = totalGrnFreeQtyRetailValue;
    }

    public double getTotalRetailSaleCashValue() {
        return totalRetailSaleCashValue;
    }

    public void setTotalRetailSaleCashValue(double totalRetailSaleCashValue) {
        this.totalRetailSaleCashValue = totalRetailSaleCashValue;
    }

    public double getTotalPurchaseSaleCashValue() {
        return totalPurchaseSaleCashValue;
    }

    public void setTotalPurchaseSaleCashValue(double totalPurchaseSaleCashValue) {
        this.totalPurchaseSaleCashValue = totalPurchaseSaleCashValue;
    }

    public double getTotalRetailSaleCreditValue() {
        return totalRetailSaleCreditValue;
    }

    public void setTotalRetailSaleCreditValue(double totalRetailSaleCreditValue) {
        this.totalRetailSaleCreditValue = totalRetailSaleCreditValue;
    }

    public double getTotalPurchaseSaleCreditValue() {
        return totalPurchaseSaleCreditValue;
    }

    public void setTotalPurchaseSaleCreditValue(double totalPurchaseSaleCreditValue) {
        this.totalPurchaseSaleCreditValue = totalPurchaseSaleCreditValue;
    }

    public double getStartingPurchaseValue() {
        return startingPurchaseValue;
    }

    public void setStartingPurchaseValue(double startingPurchaseValue) {
        this.startingPurchaseValue = startingPurchaseValue;
    }

    public double getStartingSaleValue() {
        return startingSaleValue;
    }

    public void setStartingSaleValue(double startingSaleValue) {
        this.startingSaleValue = startingSaleValue;
    }

    public double getPurchaseValueAfterGrn() {
        return purchaseValueAfterGrn;
    }

    public void setPurchaseValueAfterGrn(double purchaseValueAfterGrn) {
        this.purchaseValueAfterGrn = purchaseValueAfterGrn;
    }

    public double getSaleValueAfterGrn() {
        return saleValueAfterGrn;
    }

    public void setSaleValueAfterGrn(double saleValueAfterGrn) {
        this.saleValueAfterGrn = saleValueAfterGrn;
    }

    public double getPurchaseValueAfterGrnReturnAndFreeQty() {
        return purchaseValueAfterGrnReturnAndFreeQty;
    }

    public void setPurchaseValueAfterGrnReturnAndFreeQty(double purchaseValueAfterGrnReturnAndFreeQty) {
        this.purchaseValueAfterGrnReturnAndFreeQty = purchaseValueAfterGrnReturnAndFreeQty;
    }

    public double getSaleValueAfterGrnReturnAndFreeQty() {
        return saleValueAfterGrnReturnAndFreeQty;
    }

    public void setSaleValueAfterGrnReturnAndFreeQty(double saleValueAfterGrnReturnAndFreeQty) {
        this.saleValueAfterGrnReturnAndFreeQty = saleValueAfterGrnReturnAndFreeQty;
    }

    public double getPurchaseValueAfterSale() {
        return purchaseValueAfterSale;
    }

    public void setPurchaseValueAfterSale(double purchaseValueAfterSale) {
        this.purchaseValueAfterSale = purchaseValueAfterSale;
    }

    public double getSaleValueAfterSale() {
        return saleValueAfterSale;
    }

    public void setSaleValueAfterSale(double saleValueAfterSale) {
        this.saleValueAfterSale = saleValueAfterSale;
    }

    public double getPurchaseValueAfterDisbursement() {
        return purchaseValueAfterDisbursement;
    }

    public void setPurchaseValueAfterDisbursement(double purchaseValueAfterDisbursement) {
        this.purchaseValueAfterDisbursement = purchaseValueAfterDisbursement;
    }

    public double getSaleValueAfterDisbursement() {
        return saleValueAfterDisbursement;
    }

    public void setSaleValueAfterDisbursement(double saleValueAfterDisbursement) {
        this.saleValueAfterDisbursement = saleValueAfterDisbursement;
    }

    public double getFinalTotalPurchaseValue() {
        return finalTotalPurchaseValue;
    }

    public void setFinalTotalPurchaseValue(double finalTotalPurchaseValue) {
        this.finalTotalPurchaseValue = finalTotalPurchaseValue;
    }

    public double getFinalTotalSaleValue() {
        return finalTotalSaleValue;
    }

    public void setFinalTotalSaleValue(double finalTotalSaleValue) {
        this.finalTotalSaleValue = finalTotalSaleValue;
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

    public List<ReportViewType> getReportViewTypes() {
        return reportViewTypes;
    }

    public void setReportViewTypes(List<ReportViewType> reportViewTypes) {
        this.reportViewTypes = reportViewTypes;
    }

    public boolean isIncludeZeroStock() {
        return includeZeroStock;
    }

    public void setIncludeZeroStock(boolean includeZeroStock) {
        this.includeZeroStock = includeZeroStock;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

}
