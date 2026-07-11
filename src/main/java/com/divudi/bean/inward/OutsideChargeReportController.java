package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.PharmacyController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.OutsidePaymentReportDto;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.Serializable;
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
import javax.persistence.TemporalType;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Thisara Samuditha | github - thisarasamuditha |  thellamburavithanagethisarasam@gmail.com
 */
@Named(value = "outsideChargeReportController")
@SessionScoped
public class OutsideChargeReportController implements Serializable {

    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFacade billFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;
    @Inject
    private PharmacyController pharmacyController;

    private Date fromDate;
    private Date toDate;
    private Date dischargeFromDate;
    private Date dischargeToDate;
    private Date invoiceApprovedFromDate;
    private Date invoiceApprovedToDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private Doctor referringDoctor;
    private Institution creditCompany;
    private List<AdmissionType> admissionType;

    /** "Paid", "Not Paid", or null/"" for All. Bound to the Paid Type filter. */
    private String paidType;

    private List<OutsidePaymentReportDto> reportRows;

    public OutsideChargeReportController() {
    }
 
    public void processOutsidePaymentReport() {
        reportRows = null;

        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both Admission From and To dates.");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("Admission From date must not be after Admission To date.");
            return;
        }
        if (dischargeFromDate != null && dischargeToDate != null && dischargeFromDate.after(dischargeToDate)) {
            JsfUtil.addErrorMessage("Discharge From date must not be after Discharge To date.");
            return;
        }
        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null
                && invoiceApprovedFromDate.after(invoiceApprovedToDate)) {
            JsfUtil.addErrorMessage("Invoice Approved From date must not be after Invoice Approved To date.");
            return;
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT new com.divudi.core.data.dto.OutsidePaymentReportDto("
                + "bi.id, "
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "COALESCE(enc.patient.phn, ''), "
                + "COALESCE(enc.patient.person.name, ''), "
                + "COALESCE(enc.bhtNo, ''), "
                + "COALESCE(item.name, ''), "
                + "enc.dateOfDischarge, "
                + "b.cancelled, "
                + "b.refunded, "
                + "bi.inwardChargeType, "
                + "COALESCE(createrPerson.name, ''), "
                + "bi.createdAt, "
                + "bi.netValue, "
                + "b.paidAt, "
                + "b.paidAmount, "
                + "b.netTotal, "
                + "COALESCE(bi.descreption, ''), "
                + "b.paid) "
                + "FROM BillItem bi "
                + "JOIN bi.bill b "
                + "JOIN b.patientEncounter enc "
                + "LEFT JOIN bi.item item "
                + "LEFT JOIN bi.creater creater "
                + "LEFT JOIN creater.webUserPerson createrPerson "
                + "WHERE bi.retired = false "
                + "AND b.retired = false "
                + "AND b.billType = :bt "
                + "AND enc.dateOfAdmission BETWEEN :fd AND :td ");

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.InwardOutSideBill);
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (dischargeFromDate != null && dischargeToDate != null) {
            jpql.append("AND enc.dateOfDischarge BETWEEN :dfd AND :dtd ");
            params.put("dfd", dischargeFromDate);
            params.put("dtd", dischargeToDate);
        }

        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null) {
            jpql.append("AND b.createdAt BETWEEN :iafd AND :iatd ");
            params.put("iafd", invoiceApprovedFromDate);
            params.put("iatd", invoiceApprovedToDate);
        }

        if (institution != null) {
            jpql.append("AND b.institution = :ins ");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append("AND b.department.site = :site ");
            params.put("site", site);
        }

        if (department != null) {
            jpql.append("AND b.department = :dept ");
            params.put("dept", department);
        }

        if (referringDoctor != null) {
            jpql.append("AND enc.referringDoctor = :doc ");
            params.put("doc", referringDoctor);
        }

        if (creditCompany != null) {
            jpql.append("AND b.creditCompany = :cc ");
            params.put("cc", creditCompany);
        }

        if (admissionType != null && !admissionType.isEmpty()) {
            jpql.append("AND enc.admissionType IN :ats ");
            params.put("ats", admissionType);
        }

        if ("Paid".equals(paidType)) {
            jpql.append("AND b.paid = true ");
        } else if ("Not Paid".equals(paidType)) {
            jpql.append("AND b.paid = false ");
        }

        jpql.append("ORDER BY enc.bhtNo, bi.createdAt");

        List<OutsidePaymentReportDto> result = (List<OutsidePaymentReportDto>) billItemFacade.findLightsByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP);
        reportRows = result != null ? result : new ArrayList<>();

        if (reportRows.isEmpty()) {
            JsfUtil.addErrorMessage("No records found for the selected criteria.");
        }
    }

    public String getChargeTypeLabel(InwardChargeType type) {
        if (type == null) {
            return "";
        }
        return configOptionApplicationController.getInwardChargeTypeLabel(type);
    }

    public void updateOutsidePaymentRow(OutsidePaymentReportDto row) {
        if (row == null || row.getBillId() == null) {
            JsfUtil.addErrorMessage("Unable to update this row.");
            return;
        }
        try {
            Bill bill = billFacade.find(row.getBillId());
            if (bill == null) {
                JsfUtil.addErrorMessage("Bill not found for this row.");
                return;
            }
            bill.setPaidAmount(row.getPaidAmount() != null ? row.getPaidAmount() : 0.0);
            bill.setPaid(row.getPaid() != null && row.getPaid());
            billFacade.edit(bill);

            if (row.getBillItemId() != null) {
                BillItem billItem = billItemFacade.find(row.getBillItemId());
                if (billItem != null) {
                    billItem.setDescreption(row.getMemo());
                    billItemFacade.edit(billItem);
                }
            }

            JsfUtil.addSuccessMessage("Row updated successfully.");
        } catch (Exception e) {
            Logger.getLogger(OutsideChargeReportController.class.getName())
                    .log(Level.SEVERE, "Error updating outside payment row", e);
            JsfUtil.addErrorMessage("Error updating row.");
        }
    }

    public String getOutsidePaymentReportFileName() {
        StringBuilder fileName = new StringBuilder("Outside_Payment_Report");
        String dates = CommonFunctions.dateRangeForFileName(
                fromDate, toDate, sessionController.getApplicationPreference().getLongDateFormat());
        if (dates != null && !dates.isEmpty()) {
            fileName.append("_").append(dates);
        }
        return fileName.toString();
    }

    public void postProcessOutsidePaymentReportExcel(Object document) {
        if (document == null) {
            Logger.getLogger(OutsideChargeReportController.class.getName())
                    .log(Level.SEVERE, "Document is null in postProcessOutsidePaymentReportExcel");
            return;
        }
        if (!(document instanceof XSSFWorkbook)) {
            Logger.getLogger(OutsideChargeReportController.class.getName())
                    .log(Level.SEVERE, "Expected document to be an instance of XSSFWorkbook, but got: {0}", document.getClass().getName());
            return;
        }
        XSSFWorkbook workbook = (XSSFWorkbook) document;
        XSSFSheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            return;
        }

        workbook.setSheetName(0, "Outside Payment Report");
        sheet.shiftRows(0, sheet.getLastRowNum(), 7);

        Map<String, Object> filters = getFiltersForOutsidePaymentReport();
        if (filters != null && !filters.isEmpty()) {
            pharmacyController.addMetaDataToExcelSheet(workbook, sheet, 0, "Outside Payment Report", filters);
        }
    }

    private Map<String, Object> getFiltersForOutsidePaymentReport() {
        SimpleDateFormat sdf = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateTimeFormat());
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("Admission From Date", fromDate != null ? sdf.format(fromDate) : "N/A");
        filters.put("Admission To Date", toDate != null ? sdf.format(toDate) : "N/A");
        filters.put("Institution", institution != null ? institution.getName() : "All Institutions");
        filters.put("Site", site != null ? site.getName() : "All Sites");
        filters.put("Department", department != null ? department.getName() : "All Departments");
        filters.put("Credit Company", creditCompany != null ? creditCompany.getName() : "All");
        filters.put("Paid Type", paidType != null && !paidType.isEmpty() ? paidType : "All");
        return filters;
    }

    public void exportOutsidePaymentReportToPDF() {
        if (reportRows == null || reportRows.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        response.reset();

        String dates = CommonFunctions.dateRangeForFileName(
                fromDate, toDate, sessionController.getApplicationPreference().getLongDateFormat());

        response.setContentType("application/pdf");
        if (dates != null && !dates.isEmpty()) {
            response.setHeader("Content-Disposition", "attachment; filename=Outside_Payment_Report_" + dates + ".pdf");
        } else {
            response.setHeader("Content-Disposition", "attachment; filename=Outside_Payment_Report.pdf");
        }

        com.itextpdf.text.Font bodyFontSmall = com.itextpdf.text.FontFactory.getFont(
                com.itextpdf.text.FontFactory.HELVETICA, 7);
        com.itextpdf.text.Font headerFont = com.itextpdf.text.FontFactory.getFont(
                com.itextpdf.text.FontFactory.HELVETICA_BOLD, 7);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a");
        SimpleDateFormat dateFmt = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateFormat());
        String institutionName = sessionController.getInstitution() != null
                ? sessionController.getInstitution().getName() : "";

        try (OutputStream out = response.getOutputStream()) {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4.rotate());
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            if (!institutionName.isEmpty()) {
                document.add(new com.itextpdf.text.Paragraph(institutionName,
                        com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 18)));
            }
            document.add(new com.itextpdf.text.Paragraph("Outside Payment Report",
                    com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 16)));
            document.add(new com.itextpdf.text.Paragraph("Generated On: " + sdf.format(new Date()),
                    com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10)));
            document.add(new com.itextpdf.text.Paragraph(" "));

            com.itextpdf.text.BaseColor lightGray = new com.itextpdf.text.BaseColor(245, 245, 245);

            int columnCount = 12;
            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(columnCount);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1.5f, 2.5f, 1.5f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1f});

            String[] headers = {"Invoice No", "MRN", "Patient Name", "BHT No", "Description",
                "Discharged On", "Charge Type", "Invoice Added Amt", "Paid Amount", "Due Amount", "Memo", "Paid"};

            for (String header : headers) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(header, headerFont));
                cell.setBackgroundColor(lightGray);
                table.addCell(cell);
            }

            for (OutsidePaymentReportDto row : reportRows) {
                table.addCell(pdfTextCell(row.getInvoiceNo(), bodyFontSmall));
                table.addCell(pdfTextCell(row.getMrn(), bodyFontSmall));
                table.addCell(pdfTextCell(row.getPatientName(), bodyFontSmall));
                table.addCell(pdfTextCell(row.getBhtNo(), bodyFontSmall));
                table.addCell(pdfTextCell(row.getDescription(), bodyFontSmall));
                table.addCell(pdfTextCell(row.getDischargedOn() != null ? dateFmt.format(row.getDischargedOn()) : "-", bodyFontSmall));
                table.addCell(pdfTextCell(getChargeTypeLabel(row.getInwardChargeType()), bodyFontSmall));
                table.addCell(pdfNumCell(row.getInvoiceAddedAmount(), bodyFontSmall));
                table.addCell(pdfNumCell(row.getPaidAmount(), bodyFontSmall));
                table.addCell(pdfNumCell(row.getDueAmount(), bodyFontSmall));
                table.addCell(pdfTextCell(row.getMemo(), bodyFontSmall));
                table.addCell(pdfTextCell(Boolean.TRUE.equals(row.getPaid()) ? "Yes" : "No", bodyFontSmall));
            }

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(OutsideChargeReportController.class.getName())
                    .log(Level.SEVERE, "Error exporting Outside Payment Report to PDF", e);
            JsfUtil.addErrorMessage("Error exporting report to PDF.");
        }
    }

    private com.itextpdf.text.pdf.PdfPCell pdfTextCell(String text, com.itextpdf.text.Font font) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(text == null || text.isEmpty() ? "-" : text, font));
        cell.setPadding(2f);
        return cell;
    }

    private com.itextpdf.text.pdf.PdfPCell pdfNumCell(Double val, com.itextpdf.text.Font font) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(val != null ? String.format("%,.2f", val) : "-", font));
        cell.setPadding(2f);
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        return cell;
    }

    // getters and setters
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getDischargeFromDate() {
        return dischargeFromDate;
    }

    public void setDischargeFromDate(Date dischargeFromDate) {
        this.dischargeFromDate = dischargeFromDate;
    }

    public Date getDischargeToDate() {
        return dischargeToDate;
    }

    public void setDischargeToDate(Date dischargeToDate) {
        this.dischargeToDate = dischargeToDate;
    }

    public Date getInvoiceApprovedFromDate() {
        return invoiceApprovedFromDate;
    }

    public void setInvoiceApprovedFromDate(Date InvoiceApprovedFromDate) {
        this.invoiceApprovedFromDate = InvoiceApprovedFromDate;
    }

    public Date getInvoiceApprovedToDate() {
        return invoiceApprovedToDate;
    }

    public void setInvoiceApprovedToDate(Date InvoiceApprovedToDate) {
        this.invoiceApprovedToDate = InvoiceApprovedToDate;
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

    public Doctor getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Doctor referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public List<AdmissionType> getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(List<AdmissionType> admissionType) {
        this.admissionType = admissionType;
    }

    public String getPaidType() {
        return paidType;
    }

    public void setPaidType(String paidType) {
        this.paidType = paidType;
    }

    public List<OutsidePaymentReportDto> getReportRows() {
        return reportRows;
    }
}
