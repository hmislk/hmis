package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillSearch;
import com.divudi.bean.pharmacy.PharmacyDailyStockDrillDownLevel1Controller.TransactionType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.light.common.BillLight;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.PharmacyService;

import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * F15 drill-down Level 2 controller — lists the individual bills behind a single
 * Level 1 row.
 *
 * For SALES rows the L1 grouping key is BillTypeAtomic + AdmissionType + PaymentScheme;
 * for the other transaction types it is BillTypeAtomic only. Filters (date range,
 * institution, site, department) are inherited from L1.
 *
 * Parent epic: #20236. This controller: #20240.
 */
@Named
@SessionScoped
public class PharmacyDailyStockDrillDownLevel2Controller implements Serializable {

    @EJB
    private PharmacyService pharmacyService;
    @Inject
    private BillSearch billSearch;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;

    private TransactionType transactionType;
    private BillTypeAtomic billTypeAtomic;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;

    private List<BillLight> bills = new ArrayList<>();

    private double totalGross;
    private double totalDiscount;
    private double totalServiceCharge;
    private double totalNet;
    private BigDecimal totalRetail = BigDecimal.ZERO;
    private BigDecimal totalPurchase = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;

    public PharmacyDailyStockDrillDownLevel2Controller() {
    }

    /**
     * Entry point invoked by Level 1's row-drill action. Copies the L1 row's
     * filter context (date range, institution/site/department, transaction type
     * and the row's selection key — atomic + admissionType + paymentScheme),
     * runs the fetch, then redirects to the L2 page.
     */
    public String navigateToLevel2FromLevel1(Date fromDate, Date toDate,
                                             Institution institution, Institution site, Department department,
                                             TransactionType transactionType, BillTypeAtomic billTypeAtomic,
                                             AdmissionType admissionType, PaymentScheme paymentScheme) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.institution = institution;
        this.site = site;
        this.department = department;
        this.transactionType = transactionType;
        this.billTypeAtomic = billTypeAtomic;
        this.admissionType = admissionType;
        this.paymentScheme = paymentScheme;
        loadBills();
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level2?faces-redirect=true";
    }

    private void loadBills() {
        bills = new ArrayList<>();
        resetTotals();

        if (transactionType == null || billTypeAtomic == null) {
            JsfUtil.addErrorMessage("Missing transaction type or bill type — cannot load Level 2 bill list");
            return;
        }

        Date startOfDay = CommonFunctions.getStartOfDay(fromDate);
        Date endOfDay = CommonFunctions.getEndOfDay(toDate);

        switch (transactionType) {
            case SALES:
                bills = pharmacyService.fetchPharmacyIncomeBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
            case PURCHASES:
                bills = pharmacyService.fetchPharmacyPurchaseBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
            case TRANSFERS:
                bills = pharmacyService.fetchPharmacyTransferBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
            case ADJUSTMENTS:
                bills = pharmacyService.fetchPharmacyAdjustmentBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }

        // Sort bills oldest first by bill date (per #20240 acceptance criteria).
        bills.sort(Comparator.comparing(
                BillLight::getBillDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        computeTotals();
    }

    private void resetTotals() {
        totalGross = 0;
        totalDiscount = 0;
        totalServiceCharge = 0;
        totalNet = 0;
        totalRetail = BigDecimal.ZERO;
        totalPurchase = BigDecimal.ZERO;
        totalCost = BigDecimal.ZERO;
    }

    private void computeTotals() {
        for (BillLight b : bills) {
            if (b.getTotal() != null) {
                totalGross += b.getTotal();
            }
            if (b.getDiscount() != null) {
                totalDiscount += b.getDiscount();
            }
            if (b.getServiceCharge() != null) {
                totalServiceCharge += b.getServiceCharge();
            }
            if (b.getNetTotal() != null) {
                totalNet += b.getNetTotal();
            }
            if (b.getTotalRetailSaleValue() != null) {
                totalRetail = totalRetail.add(b.getTotalRetailSaleValue());
            }
            if (b.getTotalPurchaseValue() != null) {
                totalPurchase = totalPurchase.add(b.getTotalPurchaseValue());
            }
            if (b.getTotalCostValue() != null) {
                totalCost = totalCost.add(b.getTotalCostValue());
            }
        }
    }

    /**
     * Back-to-Level-1 navigation. Level 1 is @SessionScoped so its previously
     * selected filters persist automatically — no explicit hand-back needed.
     */
    public String navigateBackToLevel1() {
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level1?faces-redirect=true";
    }

    /**
     * Open the existing per-bill view page for the given Level 2 row — issue #20241.
     * Delegates to BillSearch which loads the full Bill by id and routes to the
     * correct view page for the bill's BillTypeAtomic (refund / cancellation /
     * wholesale / GRN / transfer / etc.).
     */
    public String viewBill(BillLight bill) {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("Cannot open bill view — bill is missing or has no id");
            return null;
        }
        return billSearch.navigateToViewBillByAtomicBillTypeByBillId(bill.getId());
    }

    /**
     * Navigate to the Level 2 print page — issue #20245. The print page reads
     * the bill list and selection context straight from this @SessionScoped
     * controller, so no data hand-off is needed.
     */
    public String navigateToPrintPage() {
        if (bills == null || bills.isEmpty()) {
            JsfUtil.addErrorMessage("Please drill into a Level 1 row first before printing");
            return null;
        }
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level2_print?faces-redirect=true";
    }

    /**
     * Download the Level 2 bill list as an .xlsx file — issue #20244.
     * Header band carries the L2 selection context, followed by a 9-column bill
     * table and a bold totals row matching the on-screen view.
     */
    public void downloadExcel() {
        if (bills == null || bills.isEmpty()) {
            JsfUtil.addErrorMessage("Please drill into a Level 1 row first before downloading");
            return;
        }
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String atomicLabel = billTypeAtomic != null ? billTypeAtomic.name() : "AllAtomics";
            String fileName = "Pharmacy_BillList_"
                    + atomicLabel + "_"
                    + dateFormat.format(fromDate) + "_to_"
                    + dateFormat.format(toDate) + ".xlsx";

            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            Workbook workbook = createExcelReport();
            OutputStream out = response.getOutputStream();
            workbook.write(out);
            workbook.close();
            out.flush();
            out.close();

            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
            JsfUtil.addErrorMessage("Error generating Excel report: " + e.getMessage());
        }
    }

    private Workbook createExcelReport() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("L2 Bill List");

        short numFmt = workbook.createDataFormat().getFormat("#,##0.00");
        short dateFmt = workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm");

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle labelStyle = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);

        CellStyle numStyle = workbook.createCellStyle();
        numStyle.setDataFormat(numFmt);
        numStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(dateFmt);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

        int rowNum = 0;
        int totalCols = 9;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("F15 Drill-Down — Level 2 (Bill List)");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, totalCols - 1));

        rowNum++; // blank

        rowNum = writeKeyValueRow(sheet, rowNum, "From Date", dateFormat.format(fromDate), labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "To Date", dateFormat.format(toDate), labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Institution", institution != null ? institution.getName() : "", labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Site", site != null ? site.getName() : "", labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Department", department != null ? department.getName() : "", labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Transaction Type", transactionType != null ? transactionType.name() : "", labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Bill Type Atomic", billTypeAtomic != null ? billTypeAtomic.getLabel() : "", labelStyle);
        if (transactionType == TransactionType.SALES) {
            rowNum = writeKeyValueRow(sheet, rowNum, "Admission Type", admissionType != null ? admissionType.getName() : "", labelStyle);
            rowNum = writeKeyValueRow(sheet, rowNum, "Payment Scheme", paymentScheme != null ? paymentScheme.getName() : "", labelStyle);
        }

        rowNum++; // blank

        // Section header (single coloured band, blue, since L2 has no per-section colour like L1)
        byte[] blue = {(byte) 0x0D, (byte) 0x6E, (byte) 0xFD};
        String[] cols = {"Date / Time", "Bill Type Atomic", "Gross Total", "Discount", "Service Charge",
                         "Net Total", "Retail Value", "Purchase Value", "Cost Value"};
        rowNum = writeSectionHeader(sheet, workbook, rowNum, "Bills (" + bills.size() + ")", cols, blue, totalCols);

        for (BillLight b : bills) {
            Row r = sheet.createRow(rowNum++);
            if (b.getBillDate() != null) {
                Cell dc = r.createCell(0);
                dc.setCellValue(b.getBillDate());
                dc.setCellStyle(dateStyle);
            } else {
                r.createCell(0).setCellValue("");
            }
            r.createCell(1).setCellValue(b.getBillTypeAtomic() != null ? b.getBillTypeAtomic().getLabel() : "");
            setCellNum(r, 2, b.getTotal() != null ? b.getTotal() : 0.0, numStyle);
            setCellNum(r, 3, b.getDiscount() != null ? b.getDiscount() : 0.0, numStyle);
            setCellNum(r, 4, b.getServiceCharge() != null ? b.getServiceCharge() : 0.0, numStyle);
            setCellNum(r, 5, b.getNetTotal() != null ? b.getNetTotal() : 0.0, numStyle);
            setCellNum(r, 6, b.getTotalRetailSaleValue() != null ? b.getTotalRetailSaleValue().doubleValue() : 0.0, numStyle);
            setCellNum(r, 7, b.getTotalPurchaseValue() != null ? b.getTotalPurchaseValue().doubleValue() : 0.0, numStyle);
            setCellNum(r, 8, b.getTotalCostValue() != null ? b.getTotalCostValue().doubleValue() : 0.0, numStyle);
        }

        // Total row
        CellStyle totalStyle = makeSolidStyle(workbook, blue, numFmt, true, true);
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("Total");
        totalLabel.setCellStyle(totalStyle);
        Cell emptyAtomic = totalRow.createCell(1);
        emptyAtomic.setCellStyle(totalStyle);
        setCellNum(totalRow, 2, totalGross, totalStyle);
        setCellNum(totalRow, 3, totalDiscount, totalStyle);
        setCellNum(totalRow, 4, totalServiceCharge, totalStyle);
        setCellNum(totalRow, 5, totalNet, totalStyle);
        setCellNum(totalRow, 6, totalRetail.doubleValue(), totalStyle);
        setCellNum(totalRow, 7, totalPurchase.doubleValue(), totalStyle);
        setCellNum(totalRow, 8, totalCost.doubleValue(), totalStyle);

        for (int c = 0; c < totalCols; c++) {
            sheet.autoSizeColumn(c);
        }
        return workbook;
    }

    private int writeKeyValueRow(Sheet sheet, int rowNum, String key, String value, CellStyle keyStyle) {
        Row r = sheet.createRow(rowNum++);
        Cell k = r.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(keyStyle);
        r.createCell(1).setCellValue(value != null ? value : "");
        return rowNum;
    }

    private int writeSectionHeader(Sheet sheet, XSSFWorkbook wb, int rowNum,
                                   String sectionTitle, String[] colHeaders, byte[] rgb, int totalCols) {
        CellStyle sectionStyle = makeSolidStyle(wb, rgb, (short) 0, true, true);
        Row sectionRow = sheet.createRow(rowNum++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue(sectionTitle);
        sectionCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, totalCols - 1));

        byte[] lightRgb = lighten(rgb);
        CellStyle colStyle = makeSolidStyle(wb, lightRgb, (short) 0, true, false);
        Row colRow = sheet.createRow(rowNum++);
        for (int i = 0; i < colHeaders.length; i++) {
            Cell c = colRow.createCell(i);
            c.setCellValue(colHeaders[i]);
            c.setCellStyle(colStyle);
        }
        return rowNum;
    }

    private void setCellNum(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private XSSFCellStyle makeSolidStyle(XSSFWorkbook wb, byte[] rgb, short numFmt, boolean bold, boolean whiteText) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        if (numFmt != 0) {
            style.setDataFormat(numFmt);
        }
        style.setAlignment(HorizontalAlignment.LEFT);
        Font font = wb.createFont();
        font.setBold(bold);
        if (whiteText) {
            font.setColor(IndexedColors.WHITE.getIndex());
        }
        style.setFont(font);
        return style;
    }

    private byte[] lighten(byte[] rgb) {
        byte[] out = new byte[3];
        for (int i = 0; i < 3; i++) {
            int v = (rgb[i] & 0xFF);
            out[i] = (byte) Math.min(255, v + 80);
        }
        return out;
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public List<BillLight> getBills() {
        return bills;
    }

    public void setBills(List<BillLight> bills) {
        this.bills = bills;
    }

    public double getTotalGross() {
        return totalGross;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public double getTotalServiceCharge() {
        return totalServiceCharge;
    }

    public double getTotalNet() {
        return totalNet;
    }

    public BigDecimal getTotalRetail() {
        return totalRetail;
    }

    public BigDecimal getTotalPurchase() {
        return totalPurchase;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }
}
