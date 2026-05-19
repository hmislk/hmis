package com.divudi.bean.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.AdmissionReportDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import com.divudi.core.util.JsfUtil;

/**
 * Controller for the enhanced Admission Report (Issue #19640) and Admission by
 * Consultant Report (Issue #19642).
 *
 * Replaces the entity-based query in MdInwardReportController with a DTO-based
 * JPQL SELECT NEW approach so that all columns are populated in one query and
 * there are no lazy-load round-trips during rendering.
 */
@Named
@SessionScoped
public class AdmissionReportController implements Serializable {

    @EJB
    private AdmissionFacade admissionFacade;

    @EJB
    private StaffFacade staffFacade;

    // -------------------------------------------------------------------------
    // Filter fields — shared by both report pages
    // -------------------------------------------------------------------------
    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();

    /**
     * "createdAt" (default) or "admissionDate"
     */
    private String dateBasis = "createdAt";

    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private Institution site;
    private Department department;

    // --- Admission-by-Consultant specific filters ---
    private Speciality speciality;
    private Staff consultant;

    // -------------------------------------------------------------------------
    // Report output
    // -------------------------------------------------------------------------
    private List<AdmissionReportDTO> admissions;
    private List<AdmissionReportDTO> admissionsByConsultant;

    // -------------------------------------------------------------------------
    // Main generate methods
    // -------------------------------------------------------------------------
    public void fillAdmissions() {
        admissions = executeAdmissionQuery(null, null);
    }

    public void fillAdmissionsByConsultant() {
        admissionsByConsultant = executeAdmissionQuery(speciality, consultant);
    }

    private List<AdmissionReportDTO> executeAdmissionQuery(Speciality filterSpeciality, Staff filterConsultant) {
        Map<String, Object> params = new HashMap<>();

        StringBuilder jpql = new StringBuilder(
                "select new com.divudi.core.data.dto.AdmissionReportDTO("
                + "ad.id,"
                + "ad.bhtNo,"
                + "ad.patient.person.name,"
                + "ad.patient.person.mobile,"
                + "ad.patient.person.address,"
                + "ad.dateOfAdmission,"
                + "ad.dateOfDischarge,"
                + "ad.createdAt,"
                + "ad.paymentMethod,"
                + "ad.creditCompany,"
                + "ad.referringConsultant,"
                + "ad.opdDoctor,"
                + "rfc.name,"
                + "ad.discharged,"
                + "ad.paymentFinalized,"
                + "ad.admissionType"
                + ") from Admission ad"
                + " left join ad.currentPatientRoom pr"
                + " left join pr.roomFacilityCharge rfc"
                + " where ad.retired = false");

        // --- date basis ---
        if (fromDate != null && toDate != null) {
            if ("admissionDate".equals(dateBasis)) {
                jpql.append(" and ad.dateOfAdmission between :fromDate and :toDate");
            } else {
                jpql.append(" and ad.createdAt between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }

        // --- admission type ---
        if (admissionType != null) {
            jpql.append(" and ad.admissionType = :admType");
            params.put("admType", admissionType);
        }

        // --- payment method ---
        if (paymentMethod != null) {
            jpql.append(" and ad.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }

        // --- institution ---
        if (institution != null) {
            jpql.append(" and ad.institution = :ins");
            params.put("ins", institution);
        }

        // --- site ---
        if (site != null) {
            jpql.append(" and ad.department.site = :site");
            params.put("site", site);
        }

        // --- department ---
        if (department != null) {
            jpql.append(" and ad.department = :dept");
            params.put("dept", department);
        }

        // --- speciality (consultant report only) ---
        if (filterSpeciality != null) {
            jpql.append(" and ad.referringConsultant.speciality = :sp");
            params.put("sp", filterSpeciality);
        }

        // --- consultant/staff (consultant report only) ---
        if (filterConsultant != null) {
            jpql.append(" and ad.referringConsultant = :cs");
            params.put("cs", filterConsultant);
        }

        jpql.append(" order by ad.createdAt asc");

        List<Object> results = admissionFacade.findObjectByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        List<AdmissionReportDTO> list = new ArrayList<>();
        for (Object o : results) {
            list.add((AdmissionReportDTO) o);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // AutoComplete for consultant filter
    // -------------------------------------------------------------------------
    public List<Staff> completeConsultant(String query) {
        Map<String, Object> params = new HashMap<>();
        String jpql;

        if (speciality != null) {
            jpql = "select p from Staff p"
                    + " where p.retired = false"
                    + " and (upper(p.person.name) like :q or p.code like :q)"
                    + " and p.speciality = :sp"
                    + " order by p.person.name";
            params.put("sp", speciality);
        } else {
            jpql = "select p from Staff p"
                    + " where p.retired = false"
                    + " and p.speciality is not null"
                    + " and (upper(p.person.name) like :q or p.code like :q)"
                    + " order by p.person.name";
        }
        params.put("q", "%" + query.toUpperCase() + "%");
        return staffFacade.findByJpql(jpql, params, 20);
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------
    /**
     * Called when Institution or Site selection changes to prevent stale
     * department.
     */
    public void clearDepartment() {
        department = null;
    }

    /**
     * Called when Speciality changes to clear the consultant so the list
     * refreshes.
     */
    public void clearConsultant() {
        consultant = null;
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------
    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        dateBasis = "createdAt";
        admissionType = null;
        paymentMethod = null;
        institution = null;
        site = null;
        department = null;
        speciality = null;
        consultant = null;
        admissions = null;
        admissionsByConsultant = null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // -------------------------------------------------------------------------
    // Download admission report by consultant as PDF with professional
    // -------------------------------------------------------------------------
    public void downloadAdmissionsByConsultantPdf() {
        if (admissionsByConsultant == null || admissionsByConsultant.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please generate the report first.");
            return;
        }

        Document document = null;
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");

            String fileName = "Admission_Report_by_Consultant_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();
            document = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
            PdfWriter.getInstance(document, out);
            document.open();

            // ── Fonts ──────────────────────────────────────────────────────
            com.lowagie.text.Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 16);
            com.lowagie.text.Font headerFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 8,
                    com.lowagie.text.Font.NORMAL, java.awt.Color.WHITE);
            com.lowagie.text.Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 7);
            com.lowagie.text.Font boldFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 8);

            // ── Colors ─────────────────────────────────────────────────────
            java.awt.Color headerBg = new java.awt.Color(41, 128, 185);
            java.awt.Color altRowBg = new java.awt.Color(240, 248, 255);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

            // ── Title ──────────────────────────────────────────────────────
            Paragraph title = new Paragraph(
                    "Admission Report by Consultant", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // ── Filter Info Table ──────────────────────────────────────────
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.2f, 2.3f, 1.2f, 2.3f});
            infoTable.setSpacingAfter(12);

            addInfoCell(infoTable, "From Date:",
                    fromDate != null ? dateFormat.format(fromDate) : "N/A",
                    boldFont, normalFont);
            addInfoCell(infoTable, "To Date:",
                    toDate != null ? dateFormat.format(toDate) : "N/A",
                    boldFont, normalFont);

            addInfoCell(infoTable, "Consultant:",
                    consultant != null ? consultant.getPerson().getNameWithTitle() : "All",
                    boldFont, normalFont);
            addInfoCell(infoTable, "Speciality:",
                    speciality != null ? speciality.getName() : "All",
                    boldFont, normalFont);

            addInfoCell(infoTable, "Date Basis:", dateBasis.equals("admissionDate") ? "Admission Date" : "Created Date",
                    boldFont, normalFont);
            addInfoCell(infoTable, "Generated:", sdf.format(new Date()), boldFont, normalFont);

            String insName = institution != null ? institution.getName() : "All";
            String deptName = department != null ? department.getName() : "All";
            addInfoCell(infoTable, "Institution/Dept:", insName + " / " + deptName,
                    boldFont, normalFont);
            addInfoCell(infoTable, "Records:", String.valueOf(admissionsByConsultant.size()),
                    boldFont, normalFont);

            document.add(infoTable);

            // ── Main Data Table ────────────────────────────────────────────
            String[] headers = {"BHT No", "Patient Name", "Phone", "Consultant", "Room",
                "Admission Date", "Discharge Date", "Adm Type", "Status", "Payment"};
            float[] colWidths = {6f, 14f, 10f, 13f, 8f, 10f, 10f, 8f, 9f, 8f};

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(colWidths);
            table.setHeaderRows(1);
            table.setSpacingBefore(5);

            // Header row
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Data rows
            int rowIdx = 0;
            for (AdmissionReportDTO ad : admissionsByConsultant) {
                java.awt.Color bg = (rowIdx % 2 == 0) ? altRowBg : null;

                addTableCell(table, ad.getBhtNo(), normalFont, bg, Element.ALIGN_CENTER);
                addTableCell(table, ad.getPatientName(), normalFont, bg, Element.ALIGN_LEFT);
                addTableCell(table, ad.getPhone(), normalFont, bg, Element.ALIGN_LEFT);
                addTableCell(table, ad.getConsultantName(), normalFont, bg, Element.ALIGN_LEFT);
                addTableCell(table, ad.getRoomName(), normalFont, bg, Element.ALIGN_CENTER);
                addTableCell(table, ad.getDateOfAdmission() != null
                        ? dateFormat.format(ad.getDateOfAdmission()) : "N/A",
                        normalFont, bg, Element.ALIGN_CENTER);
                addTableCell(table, ad.getDateOfDischarge() != null
                        ? dateFormat.format(ad.getDateOfDischarge()) : "N/A",
                        normalFont, bg, Element.ALIGN_CENTER);
                addTableCell(table, ad.getAdmissionType() != null
                        ? ad.getAdmissionType().getName() : "",
                        normalFont, bg, Element.ALIGN_LEFT);
                addTableCell(table, ad.getDischargeStatusText(), normalFont, bg, Element.ALIGN_CENTER);
                addTableCell(table, ad.getPaymentStatusText(), normalFont, bg, Element.ALIGN_CENTER);

                rowIdx++;
            }

            document.add(table);
            document.close();
            facesContext.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper: Add label-value pair to info table
    // -------------------------------------------------------------------------
    private void addInfoCell(PdfPTable table, String label, String value,
            com.lowagie.text.Font labelFont, com.lowagie.text.Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new com.lowagie.text.Phrase(label, labelFont));
        labelCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        labelCell.setPadding(2);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new com.lowagie.text.Phrase(value, valueFont));
        valueCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        valueCell.setPadding(2);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(valueCell);
    }

    // -------------------------------------------------------------------------
    // Helper: Add cell to data table with styling
    // -------------------------------------------------------------------------
    private void addTableCell(PdfPTable table, String value,
            com.lowagie.text.Font font, java.awt.Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(
                value != null && !value.isEmpty() ? value : "-", font));
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        table.addCell(cell);
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------
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

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Staff getConsultant() {
        return consultant;
    }

    public void setConsultant(Staff consultant) {
        this.consultant = consultant;
    }

    public List<AdmissionReportDTO> getAdmissions() {
        return admissions;
    }

    public List<AdmissionReportDTO> getAdmissionsByConsultant() {
        return admissionsByConsultant;
    }
}
