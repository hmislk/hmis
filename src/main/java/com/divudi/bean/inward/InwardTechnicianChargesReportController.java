package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.InwardTechnicianChargeGroupDTO;
import com.divudi.core.data.dto.InwardTechnicianChargeRowDTO;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.inward.InwardProfessionalFeeClassificationService;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Inward Technician Charges Report (issue #25818): every inpatient professional
 * fee whose Fee Category is Technician Fee, grouped by technician with Added and
 * Paid Fee Value subtotals and a grand total. Excel, PDF (A4) and Print.
 */
@Named
@SessionScoped
public class InwardTechnicianChargesReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DATE_BASIS_ADMISSION = "admission";
    private static final String DATE_BASIS_DISCHARGE = "discharge";
    private static final String DATE_BASIS_FINAL_BILL = "finalBillCreated";
    private static final String DATE_BASIS_FEE_ADDED = "feeAdded";

    @EJB
    private BillFacade billFacade;
    @EJB
    private InwardProfessionalFeeClassificationService professionalFeeClassificationService;
    @Inject
    private SessionController sessionController;

    private Date fromDate;
    private Date toDate;
    private String dateBasis = DATE_BASIS_ADMISSION;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private Department department;
    private Staff technician;
    private boolean scopeInitialized;

    private List<InwardTechnicianChargeGroupDTO> groups = new ArrayList<>();
    private double grandAddedTotal;
    private double grandPaidTotal;
    private boolean processed;
    // Filters as they were when the report was last processed, for the print/export header.
    private String filterSummary = "";
    private boolean showGrandTotal;

    public String navigateToTechnicianChargesReport() {
        if (!scopeInitialized) {
            institution = sessionController.getInstitution();
            scopeInitialized = true;
        }
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        dateBasis = DATE_BASIS_ADMISSION;
        admissionType = null;
        paymentMethod = null;
        department = null;
        technician = null;
        filterSummary = "";
        showGrandTotal = false;
        groups = new ArrayList<>();
        grandAddedTotal = 0;
        grandPaidTotal = 0;
        processed = false;
        return "/inward/reports/inward_technician_charges_report?faces-redirect=true";
    }

    public void clearTechnician() {
        technician = null;
    }

    public void clearDepartment() {
        department = null;
    }

    public void processReport() {
        groups = new ArrayList<>();
        grandAddedTotal = 0;
        grandPaidTotal = 0;
        processed = false;
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Select From Date and To Date");
            return;
        }
        try {
            List<InwardTechnicianChargeRowDTO> rows = fetchRows();
            Map<Long, InwardTechnicianChargeGroupDTO> byStaff = new LinkedHashMap<>();
            for (InwardTechnicianChargeRowDTO row : rows) {
                InwardTechnicianChargeGroupDTO g = byStaff.get(row.getStaffId());
                if (g == null) {
                    g = new InwardTechnicianChargeGroupDTO(row.getTechnicianName());
                    byStaff.put(row.getStaffId(), g);
                }
                g.add(row);
                grandAddedTotal += row.getAddedFeeValue();
                grandPaidTotal += row.getPaidFeeValue();
            }
            groups = new ArrayList<>(byStaff.values());
            filterSummary = buildFilterSummary();
            showGrandTotal = technician == null;
            processed = true;
        } catch (Exception e) {
            Logger.getLogger(InwardTechnicianChargesReportController.class.getName()).log(Level.SEVERE, "Error loading technician charges report", e);
            JsfUtil.addErrorMessage("Error loading technician charges report data");
        }
    }

    private List<InwardTechnicianChargeRowDTO> fetchRows() {
        boolean byFinalBill = DATE_BASIS_FINAL_BILL.equals(dateBasis);
        Map<String, Object> params = new HashMap<>();
        params.put("billType", BillType.InwardProfessional);
        List<BillTypeAtomic> excludedAtomics = new ArrayList<>();
        excludedAtomics.add(BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL_CANCELLATION);
        excludedAtomics.add(BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL_CANCELLATION);
        params.put("excludedAtomics", excludedAtomics);

        String jpql = "SELECT new com.divudi.core.data.dto.InwardTechnicianChargeRowDTO("
                + "COALESCE(pe.bhtNo, ''), "
                + "st.id, "
                + "COALESCE(per.name, ''), "
                + "per.title, "
                + "bf.createdAt, "
                + "bf.feeValue, "
                + "bf.paidValue) "
                + "FROM BillFee bf "
                + "JOIN bf.bill b "
                + "JOIN b.patientEncounter pe "
                + (byFinalBill ? "JOIN pe.finalBill fb " : "")
                + "JOIN bf.staff st "
                + "LEFT JOIN st.person per "
                + "WHERE bf.retired = false "
                + "AND b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billType = :billType "
                // Cancelling a fee bill also writes a negative contra bill (not flagged cancelled); leave it out
                // so a cancelled fee disappears from the report.
                + "AND b.billTypeAtomic NOT IN :excludedAtomics "
                // Fee Category = Technician Fee (never affected by the merged-fees toggle).
                + professionalFeeClassificationService.staffCondition("bf", InwardChargeType.TechnicianAndParamedicalCharge, params);

        jpql = appendScopePredicate(jpql, params);
        jpql = appendAdmissionTypePredicate(jpql, params);
        jpql = appendPaymentMethodPredicate(jpql, params);
        if (technician != null) {
            params.put("technician", technician);
            jpql += "AND bf.staff = :technician ";
        }
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        jpql += "AND " + resolveDatePath() + " BETWEEN :fromDate AND :toDate ";
        jpql += "ORDER BY per.name, st.id, bf.createdAt, pe.bhtNo";

        List<InwardTechnicianChargeRowDTO> result = (List<InwardTechnicianChargeRowDTO>) billFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    private String resolveDatePath() {
        if (DATE_BASIS_DISCHARGE.equals(dateBasis)) {
            return "pe.dateOfDischarge";
        } else if (DATE_BASIS_FINAL_BILL.equals(dateBasis)) {
            return "fb.createdAt";
        } else if (DATE_BASIS_FEE_ADDED.equals(dateBasis)) {
            return "bf.createdAt";
        }
        return "pe.dateOfAdmission";
    }

    private String appendScopePredicate(String jpql, Map<String, Object> params) {
        if (institution != null) {
            params.put("tcInstitution", institution);
            jpql += "AND pe.institution = :tcInstitution ";
        }
        if (department != null) {
            params.put("tcDepartment", department);
            jpql += "AND pe.department = :tcDepartment ";
        }
        return jpql;
    }

    private String appendAdmissionTypePredicate(String jpql, Map<String, Object> params) {
        if (admissionType == null) {
            return jpql;
        }
        params.put("tcAdmissionType", admissionType);
        return jpql + "AND pe.admissionType = :tcAdmissionType ";
    }

    private String appendPaymentMethodPredicate(String jpql, Map<String, Object> params) {
        if (paymentMethod == null) {
            return jpql;
        }
        params.put("tcPaymentMethod", paymentMethod);
        return jpql + "AND pe.paymentMethod = :tcPaymentMethod ";
    }

    private String dateBasisLabel() {
        if (DATE_BASIS_DISCHARGE.equals(dateBasis)) {
            return "Discharge Date";
        } else if (DATE_BASIS_FINAL_BILL.equals(dateBasis)) {
            return "Final Bill Created Date";
        } else if (DATE_BASIS_FEE_ADDED.equals(dateBasis)) {
            return "Professional Fee Added Date";
        }
        return "Admission Date";
    }

    private String buildFilterSummary() {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append(dateBasisLabel()).append(": ").append(f.format(fromDate)).append(" to ").append(f.format(toDate));
        sb.append(" | Admission Type: ").append(admissionType != null ? admissionType.getName() : "All");
        sb.append(" | Payment Method: ").append(paymentMethod != null ? paymentMethod.getLabel() : "All");
        sb.append(" | Institution: ").append(institution != null ? institution.getName() : "All");
        sb.append(" | Department: ").append(department != null ? department.getName() : "All");
        sb.append(" | Technician: ").append(technician != null && technician.getPerson() != null
                ? technician.getPerson().getName() : "All Technicians");
        return sb.toString();
    }

    public void downloadExcel() {
        if (groups == null || groups.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Technician Charges");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            org.apache.poi.ss.usermodel.Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(bold);
            CellStyle boldMoneyStyle = workbook.createCellStyle();
            boldMoneyStyle.setFont(bold);
            boldMoneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            int r = 0;
            sheet.createRow(r++).createCell(0).setCellValue("Inward Technician Charges Report");
            sheet.createRow(r++).createCell(0).setCellValue(filterSummary);
            r++;

            String[] headers = {"BHT No", "Technician Name", "Fee Added Date", "Added Fee Value", "Paid Fee Value"};
            Row headerRow = sheet.createRow(r++);
            for (int c = 0; c < headers.length; c++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(headerStyle);
            }

            for (InwardTechnicianChargeGroupDTO g : groups) {
                for (InwardTechnicianChargeRowDTO row : g.getRows()) {
                    Row x = sheet.createRow(r++);
                    x.createCell(0).setCellValue(row.getBhtNo());
                    x.createCell(1).setCellValue(row.getTechnicianName());
                    x.createCell(2).setCellValue(row.getFeeAddedDate() != null ? dateFormat.format(row.getFeeAddedDate()) : "");
                    org.apache.poi.ss.usermodel.Cell a = x.createCell(3);
                    a.setCellValue(row.getAddedFeeValue());
                    a.setCellStyle(moneyStyle);
                    org.apache.poi.ss.usermodel.Cell p = x.createCell(4);
                    p.setCellValue(row.getPaidFeeValue());
                    p.setCellStyle(moneyStyle);
                }
                writeTotalRow(sheet.createRow(r++), "Subtotal - " + g.getTechnicianName(),
                        g.getAddedFeeTotal(), g.getPaidFeeTotal(), boldStyle, boldMoneyStyle);
            }
            if (showGrandTotal) {
                writeTotalRow(sheet.createRow(r++), "Grand Total", grandAddedTotal, grandPaidTotal, boldStyle, boldMoneyStyle);
            }
            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();
            String filename = "Inward_Technician_Charges_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentLength(bytes.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
            }
            facesContext.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(InwardTechnicianChargesReportController.class.getName()).log(Level.SEVERE, "Error exporting technician charges to Excel", e);
            JsfUtil.addErrorMessage("Failed to generate Excel: " + e.getMessage());
        }
    }

    private void writeTotalRow(Row row, String label, double added, double paid, CellStyle boldStyle, CellStyle boldMoneyStyle) {
        org.apache.poi.ss.usermodel.Cell lbl = row.createCell(0);
        lbl.setCellValue(label);
        lbl.setCellStyle(boldStyle);
        org.apache.poi.ss.usermodel.Cell a = row.createCell(3);
        a.setCellValue(added);
        a.setCellStyle(boldMoneyStyle);
        org.apache.poi.ss.usermodel.Cell p = row.createCell(4);
        p.setCellValue(paid);
        p.setCellStyle(boldMoneyStyle);
    }

    public void downloadPdf() {
        if (groups == null || groups.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            String fileName = "Inward_Technician_Charges_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";

            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 20, 20, 30, 25);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Inward Technician Charges Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);
            com.lowagie.text.Paragraph filters = new com.lowagie.text.Paragraph(filterSummary, normalFont);
            filters.setSpacingAfter(8);
            document.add(filters);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            DecimalFormat df = new DecimalFormat("#,##0.00");

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 5f, 3.5f, 3f, 3f});
            String[] headers = {"BHT No", "Technician Name", "Fee Added Date", "Added Fee Value", "Paid Fee Value"};
            for (int c = 0; c < headers.length; c++) {
                PdfPCell cell = new PdfPCell(new Phrase(headers[c], headerFont));
                cell.setHorizontalAlignment(c >= 3 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                table.addCell(cell);
            }
            // Header row repeats on each page.
            table.setHeaderRows(1);

            for (InwardTechnicianChargeGroupDTO g : groups) {
                for (InwardTechnicianChargeRowDTO row : g.getRows()) {
                    table.addCell(new PdfPCell(new Phrase(row.getBhtNo(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(row.getTechnicianName(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(row.getFeeAddedDate() != null ? sdf.format(row.getFeeAddedDate()) : "", normalFont)));
                    table.addCell(rightCell(df.format(row.getAddedFeeValue()), normalFont));
                    table.addCell(rightCell(df.format(row.getPaidFeeValue()), normalFont));
                }
                addTotalRow(table, "Subtotal - " + g.getTechnicianName(), g.getAddedFeeTotal(), g.getPaidFeeTotal(), boldFont, df);
            }
            if (showGrandTotal) {
                addTotalRow(table, "Grand Total", grandAddedTotal, grandPaidTotal, boldFont, df);
            }

            document.add(table);
            document.close();

            byte[] pdfBytes = baos.toByteArray();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseContentLength(pdfBytes.length);
            externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            OutputStream out = externalContext.getResponseOutputStream();
            out.write(pdfBytes);
            out.flush();
            facesContext.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(InwardTechnicianChargesReportController.class.getName()).log(Level.SEVERE, "Error exporting technician charges to PDF", e);
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        }
    }

    private PdfPCell rightCell(String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, double added, double paid,
            com.lowagie.text.Font boldFont, DecimalFormat df) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, boldFont));
        lbl.setColspan(3);
        table.addCell(lbl);
        table.addCell(rightCell(df.format(added), boldFont));
        table.addCell(rightCell(df.format(paid), boldFont));
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

    public String getDateBasis() {
        return dateBasis;
    }

    public void setDateBasis(String dateBasis) {
        this.dateBasis = dateBasis;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Staff getTechnician() {
        return technician;
    }

    public void setTechnician(Staff technician) {
        this.technician = technician;
    }

    public List<InwardTechnicianChargeGroupDTO> getGroups() {
        return groups;
    }

    public double getGrandAddedTotal() {
        return grandAddedTotal;
    }

    public double getGrandPaidTotal() {
        return grandPaidTotal;
    }

    public boolean isProcessed() {
        return processed;
    }

    public boolean isShowGrandTotal() {
        return showGrandTotal;
    }

    public String getFilterSummary() {
        return filterSummary;
    }
}
