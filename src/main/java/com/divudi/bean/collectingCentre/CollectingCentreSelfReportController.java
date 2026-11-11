package com.divudi.bean.collectingCentre;

import com.divudi.bean.common.ReportTimerController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.PharmacyController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.reports.CollectionCenterReport;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Backs the "Reports" section of the Collecting Centre self-service pages.
 * Every report here is always restricted to the logged-in Collecting Centre,
 * unlike the admin reports it is based on.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class CollectingCentreSelfReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final List<BillTypeAtomic> RECEIPT_BILL_TYPES = Arrays.asList(
            BillTypeAtomic.CC_PAYMENT_MADE_BILL,
            BillTypeAtomic.CC_PAYMENT_MADE_CANCELLATION_BILL,
            BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL,
            BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);

    @EJB
    private BillFacade billFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    private CollectingCentreSelfCommonController collectingCentreSelfCommonController;
    @Inject
    private ReportTimerController reportTimerController;
    @Inject
    private PharmacyController pharmacyController;

    private Date fromDate;
    private Date toDate;
    private ReportTemplateRowBundle receiptBundle;
    /**
     * The collecting centre the receiptBundle was queried for, so a bundle is
     * never shown after the session moves to another centre.
     */
    private Institution receiptCollectingCentre;

    // <editor-fold defaultstate="collapsed" desc="Privileges">
    public boolean isCanOpenReports() {
        return collectingCentreSelfCommonController.hasSelfPrivilege(Privileges.CcSelfReports);
    }

    /**
     * The Receipt Report sits inside the Reports section, so it needs both
     * the Reports and the Receipt Report privileges.
     */
    public boolean isCanOpenReceiptReport() {
        return isCanOpenReports()
                && collectingCentreSelfCommonController.hasSelfPrivilege(Privileges.CcSelfReceiptReport);
    }

    private Privileges missingReceiptReportPrivilege() {
        return isCanOpenReports() ? Privileges.CcSelfReceiptReport : Privileges.CcSelfReports;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation">
    public String navigateToReports() {
        if (!isCanOpenReports()) {
            return collectingCentreSelfCommonController.navigateToSelfUnauthorized(Privileges.CcSelfReports);
        }
        return "/collecting_centre/cc_self_reports?faces-redirect=true";
    }

    public String navigateToReceiptReport() {
        if (!isCanOpenReceiptReport()) {
            return collectingCentreSelfCommonController.navigateToSelfUnauthorized(missingReceiptReportPrivilege());
        }
        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay();
        clearReceiptBundle();
        return "/collecting_centre/cc_self_receipt_report?faces-redirect=true";
    }

    public String navigateBackToReceiptReport() {
        if (!isCanOpenReceiptReport()) {
            return collectingCentreSelfCommonController.navigateToSelfUnauthorized(missingReceiptReportPrivilege());
        }
        return "/collecting_centre/cc_self_receipt_report?faces-redirect=true";
    }

    /**
     * Re-processes the current criteria before printing, so the print view
     * never shows rows from an earlier Process run.
     */
    public String navigateToReceiptReportPrint() {
        processReceiptReport();
        if (getReceiptBundle() == null) {
            return null;
        }
        return "/collecting_centre/cc_self_receipt_report_print?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Receipt Report">
    /**
     * Same bills as the admin Collection Centre Receipt Report, but always
     * pinned to the logged-in Collecting Centre (bill.fromInstitution).
     */
    public void processReceiptReport() {
        clearReceiptBundle();
        if (!collectingCentreSelfCommonController.checkSelfPrivilege(Privileges.CcSelfReports)
                || !collectingCentreSelfCommonController.checkSelfPrivilege(Privileges.CcSelfReceiptReport)) {
            return;
        }
        Institution collectingCentre = sessionController.getInstitution();
        if (collectingCentre == null || !collectingCentreSelfCommonController.isCollectingCentreDepartment()) {
            JsfUtil.addErrorMessage("Collecting centre not found for the logged department");
            return;
        }
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select From and To dates");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From date must be before To date");
            return;
        }
        reportTimerController.trackReportExecution(() -> {
            String jpql = "select new com.divudi.core.data.ReportTemplateRow(bill) "
                    + " from Bill bill "
                    + " where bill.retired=:ret "
                    + " and bill.billDate between :fd and :td "
                    + " and bill.billTypeAtomic in :bTypes "
                    + " and bill.fromInstitution=:cc "
                    + " order by bill.createdAt";
            Map<String, Object> m = new HashMap<>();
            m.put("ret", false);
            m.put("fd", fromDate);
            m.put("td", toDate);
            m.put("bTypes", RECEIPT_BILL_TYPES);
            m.put("cc", collectingCentre);

            ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
            bundle.setReportTemplateRows((List<ReportTemplateRow>) billFacade.findLightsByJpql(jpql, m));
            bundle.calculateTotalByBills();
            receiptBundle = bundle;
            receiptCollectingCentre = collectingCentre;
        }, CollectionCenterReport.COLLECTION_CENTER_RECEIPT_REPORT, sessionController.getLoggedUser());
    }

    private void clearReceiptBundle() {
        receiptBundle = null;
        receiptCollectingCentre = null;
    }

    private boolean isReceiptBundleForCurrentCentre() {
        Institution current = sessionController.getInstitution();
        return receiptCollectingCentre != null
                && current != null
                && receiptCollectingCentre.getId() != null
                && receiptCollectingCentre.getId().equals(current.getId());
    }

    public Map<String, Object> getReceiptReportFilters() {
        SimpleDateFormat sdf = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateTimeFormat());
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("From Date", fromDate == null ? "-" : sdf.format(fromDate));
        filters.put("To Date", toDate == null ? "-" : sdf.format(toDate));
        Institution collectingCentre = sessionController.getInstitution();
        filters.put("Collecting Centre", collectingCentre == null ? "-" : collectingCentre.getCode() + " - " + collectingCentre.getName());
        return filters;
    }

    public void postProcessReceiptReportExcel(Object document) {
        if (!(document instanceof XSSFWorkbook)) {
            Logger.getLogger(CollectingCentreSelfReportController.class.getName()).log(Level.SEVERE, "Expected an XSSFWorkbook for the CC Self Receipt Report Excel export");
            return;
        }
        XSSFWorkbook workbook = (XSSFWorkbook) document;
        XSSFSheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            return;
        }
        workbook.setSheetName(0, "Receipt Report");
        sheet.shiftRows(0, sheet.getLastRowNum(), 7);
        pharmacyController.addMetaDataToExcelSheet(workbook, sheet, 0, "Collection Centre Receipt Report", getReceiptReportFilters());
    }

    /**
     * Re-processes the current criteria before exporting, so the PDF never
     * carries rows from an earlier Process run under the new criteria.
     */
    public void exportReceiptReportToPdf() {
        processReceiptReport();
        ReportTemplateRowBundle receiptBundle = getReceiptBundle();
        if (receiptBundle == null) {
            return;
        }
        if (receiptBundle.getReportTemplateRows() == null || receiptBundle.getReportTemplateRows().isEmpty()) {
            JsfUtil.addErrorMessage("No data to export for the selected dates");
            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/pdf");
        String dates = CommonFunctions.dateRangeForFileName(fromDate, toDate, sessionController.getApplicationPreference().getLongDateFormat());
        String fileName = (dates == null || dates.isEmpty()) ? "CC_receipt_report.pdf" : "CC_receipt_report_" + dates + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        SimpleDateFormat sdf = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateTimeFormat());
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Institution collectingCentre = sessionController.getInstitution();

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            if (collectingCentre != null) {
                document.add(new Paragraph(collectingCentre.getName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            }
            document.add(new Paragraph("Collection Centre Receipt Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Printed: " + sdf.format(new Date()), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            document.add(new Paragraph(" "));

            PdfPTable infoTable = pharmacyController.createInfoTablePdfExport(sdf, getReceiptReportFilters());
            if (infoTable != null) {
                document.add(infoTable);
            }

            String[] headers = {"S. No.", "Bill No.", "Billed At", "Billed By", "Payment Method", "Amount", "Comments"};
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.6f, 3f, 2.5f, 2.5f, 1.8f, 1.8f, 4f});
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            int index = 1;
            for (ReportTemplateRow row : receiptBundle.getReportTemplateRows()) {
                Bill b = row.getBill();
                table.addCell(textCell(String.valueOf(index++), bodyFont));
                table.addCell(textCell(b.getDeptId(), bodyFont));
                table.addCell(textCell(b.getCreatedAt() == null ? null : sdf.format(b.getCreatedAt()), bodyFont));
                table.addCell(textCell(b.getCreater() == null ? null : b.getCreater().getName(), bodyFont));
                table.addCell(textCell(b.getPaymentMethod() == null ? null : b.getPaymentMethod().getLabel(), bodyFont));
                table.addCell(numCell(b.getTotal(), bodyFont));
                table.addCell(textCell(b.getComments(), bodyFont));
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            totalLabel.setColspan(5);
            totalLabel.setBackgroundColor(BaseColor.LIGHT_GRAY);
            totalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(totalLabel);
            table.addCell(numCell(receiptBundle.getTotal(), headerFont));
            table.addCell(textCell(null, bodyFont));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CollectingCentreSelfReportController.class.getName()).log(Level.SEVERE, "Error exporting CC Self Receipt Report to PDF", e);
        }
    }

    private PdfPCell textCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null || text.isEmpty() ? "-" : text, font));
        cell.setPadding(2f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell numCell(double value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(String.format("%,.2f", value), font));
        cell.setPadding(2f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
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

    /**
     * Returns the bundle only while the session is still on the collecting
     * centre it was queried for, and only to a user who may open the report.
     */
    public ReportTemplateRowBundle getReceiptBundle() {
        if (receiptBundle == null) {
            return null;
        }
        if (!isReceiptBundleForCurrentCentre() || !isCanOpenReceiptReport()) {
            clearReceiptBundle();
            return null;
        }
        return receiptBundle;
    }
    // </editor-fold>
}
