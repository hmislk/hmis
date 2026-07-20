package com.divudi.bean.lab;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Investigation Wise Laboratory Report.
 *
 * @author Rashmika
 */
@Named(value = "investigationWiseReport")
@SessionScoped
public class InvestigationWiseReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

    private Date fromDate;
    private Date toDate;

    private Institution institution;
    private Institution site;
    private Department department;
    private Department laboratory;
    private Investigation investigation;

    private String visitType;
    private String gender;

    private List<InvestigationWiseRow> reportData;

    public InvestigationWiseReport() {
    }

    public void process() {
        reportData = new ArrayList<>();

        if (getFromDate() == null || getToDate() == null) {
            JsfUtil.addErrorMessage(
                    "Please select both From Date and To Date."
            );
            return;
        }

        if (getFromDate().after(getToDate())) {
            JsfUtil.addErrorMessage(
                    "From Date cannot be later than To Date."
            );
            return;
        }

        StringBuilder jpql = new StringBuilder();

        jpql.append("select pi ")
                .append("from PatientInvestigation pi ")
                .append("join pi.billItem bi ")
                .append("join bi.bill b ")
                .append("where pi.retired = false ")
                .append("and bi.retired = false ")
                .append("and b.retired = false ")
                .append("and pi.createdAt between :fromDate and :toDate ");

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("fromDate", getFromDate());
        parameters.put("toDate", getToDate());

        if (institution != null) {
            jpql.append("and b.institution = :institution ");
            parameters.put("institution", institution);
        }

        if (site != null) {
            jpql.append("and b.site = :site ");
            parameters.put("site", site);
        }

        if (department != null) {
            jpql.append("and b.department = :department ");
            parameters.put("department", department);
        }

        /*
         * The laboratory department belongs to the investigation Item.
         */
        if (laboratory != null) {
            jpql.append("and bi.item.department = :laboratory ");
            parameters.put("laboratory", laboratory);
        }

        if (investigation != null) {
            jpql.append("and pi.investigation = :investigation ");
            parameters.put("investigation", investigation);
        }

        if (!isBlank(visitType)) {
            jpql.append("and upper(b.ipOpOrCc) = :visitType ");
            parameters.put(
                    "visitType",
                    visitType.trim().toUpperCase()
            );
        }

        jpql.append("order by pi.createdAt desc");

        try {
            List<PatientInvestigation> investigations
                    = patientInvestigationFacade.findByJpql(
                            jpql.toString(),
                            parameters,
                            TemporalType.TIMESTAMP
                    );

            if (investigations == null) {
                investigations = new ArrayList<>();
            }

            for (PatientInvestigation patientInvestigation
                    : investigations) {

                InvestigationWiseRow row
                        = createReportRow(patientInvestigation);

                /*
                 * Gender is filtered after querying because Person.sex is an
                 * enum and its exact enum type is not required in this bean.
                 */
                if (!isBlank(gender)
                        && !genderMatches(row.getGender(), gender)) {
                    continue;
                }

                reportData.add(row);
            }

            if (reportData.isEmpty()) {
                JsfUtil.addErrorMessage(
                        "No investigation records were found."
                );
            } else {
                JsfUtil.addSuccessMessage(
                        reportData.size() + " records found."
                );
            }

        } catch (Exception e) {
            reportData = new ArrayList<>();

            JsfUtil.addErrorMessage(
                    "Error generating report: " + e.getMessage()
            );
        }
    }

    private InvestigationWiseRow createReportRow(
            PatientInvestigation patientInvestigation) {

        InvestigationWiseRow row = new InvestigationWiseRow();

        if (patientInvestigation == null) {
            return row;
        }

        row.setId(patientInvestigation.getId());
        row.setCreatedAt(patientInvestigation.getCreatedAt());

        Patient patient = patientInvestigation.getPatient();

        if (patient != null) {
            row.setMrn(safeString(patient.getPhn()));

            Person person = patient.getPerson();

            if (person != null) {
                row.setPatientName(
                        safeString(person.getNameWithTitle())
                );

                if (person.getSex() != null) {
                    row.setGender(
                            safeString(person.getSex().getLabel())
                    );
                }

                row.setAge(
                        safeString(person.getAgeAsString())
                );
            }
        }

        Investigation selectedInvestigation
                = patientInvestigation.getInvestigation();

        if (selectedInvestigation != null) {
            row.setInvestigationName(
                    safeString(selectedInvestigation.getName())
            );
        }

        BillItem billItem = patientInvestigation.getBillItem();

        if (billItem != null) {
            Bill bill = billItem.getBill();

            if (bill != null) {
                row.setVisitType(
                        safeString(bill.getIpOpOrCc())
                );
            }

            Item item = billItem.getItem();

            if (item != null && item.getDepartment() != null) {
                row.setLaboratoryName(
                        safeString(item.getDepartment().getName())
                );
            }
        }

        return row;
    }

    private boolean genderMatches(
            String reportGender,
            String selectedGender) {

        if (isBlank(selectedGender)) {
            return true;
        }

        if (isBlank(reportGender)) {
            return false;
        }

        return reportGender.trim().equalsIgnoreCase(
                selectedGender.trim()
        );
    }

    public void clear() {
        institution = null;
        site = null;
        department = null;
        laboratory = null;
        investigation = null;

        visitType = null;
        gender = null;

        reportData = new ArrayList<>();

        fromDate = CommonFunctions.getStartOfDay(new Date());
        toDate = CommonFunctions.getEndOfDay(new Date());
    }

    public void exportExcel() {

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        HttpServletResponse response
                = (HttpServletResponse) externalContext.getResponse();

        response.reset();
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"investigation_wise_report.xlsx\""
        );
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Investigation Wise Report");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);

            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex);
            Cell titleCell = titleRow.createCell(0);

            titleCell.setCellValue("Investigation Wise Report");
            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                    new CellRangeAddress(rowIndex, rowIndex, 0, 8)
            );

            rowIndex += 2;

            rowIndex = createFilterRow(sheet, rowIndex, "From Date", formatDate(getFromDate()));
            rowIndex = createFilterRow(sheet, rowIndex, "To Date", formatDate(getToDate()));
            rowIndex = createFilterRow(sheet, rowIndex, "Institution",
                    institution == null ? "All" : safeString(institution.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Site",
                    site == null ? "All" : safeString(site.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Department",
                    department == null ? "All" : safeString(department.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Laboratory",
                    laboratory == null ? "All" : safeString(laboratory.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Investigation",
                    investigation == null ? "All" : safeString(investigation.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Gender",
                    isBlank(gender) ? "All" : gender);
            rowIndex = createFilterRow(sheet, rowIndex, "Visit Type",
                    isBlank(visitType) ? "All" : visitType);

            rowIndex++;

            String[] headers = {
                "No.", "MRN", "Visit Type", "Laboratory", "Gender",
                "Name", "Age", "Investigation", "Created Date"
            };

            Row headerRow = sheet.createRow(rowIndex++);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (getReportData().isEmpty()) {
                // NOTE: export the header-only sheet with an explicit
                // "no records" row instead of blocking the export.
                Row emptyRow = sheet.createRow(rowIndex++);
                Cell emptyCell = emptyRow.createCell(0);
                emptyCell.setCellValue("No investigation records found for the selected filters.");
                sheet.addMergedRegion(
                        new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 8)
                );
            } else {
                int number = 1;

                for (InvestigationWiseRow reportRow : getReportData()) {

                    Row excelRow = sheet.createRow(rowIndex++);

                    excelRow.createCell(0).setCellValue(number++);
                    excelRow.createCell(1).setCellValue(safeString(reportRow.getMrn()));
                    excelRow.createCell(2).setCellValue(safeString(reportRow.getVisitType()));
                    excelRow.createCell(3).setCellValue(safeString(reportRow.getLaboratoryName()));
                    excelRow.createCell(4).setCellValue(safeString(reportRow.getGender()));
                    excelRow.createCell(5).setCellValue(safeString(reportRow.getPatientName()));
                    excelRow.createCell(6).setCellValue(safeString(reportRow.getAge()));
                    excelRow.createCell(7).setCellValue(safeString(reportRow.getInvestigationName()));
                    excelRow.createCell(8).setCellValue(formatDate(reportRow.getCreatedAt()));
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);

                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();

            facesContext.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();

            if (!facesContext.getResponseComplete()) {
                JsfUtil.addErrorMessage("Excel export failed: " + e.getMessage());
            }
        }
    }

    public void exportPdf() {

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        try {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);

            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"investigation_wise_report.pdf\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            document.add(new Paragraph("Investigation Wise Report"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("From Date: " + formatDate(getFromDate())));
            document.add(new Paragraph("To Date: " + formatDate(getToDate())));
            document.add(new Paragraph("Institution: "
                    + (institution == null ? "All" : safeString(institution.getName()))));
            document.add(new Paragraph("Site: "
                    + (site == null ? "All" : safeString(site.getName()))));
            document.add(new Paragraph("Department: "
                    + (department == null ? "All" : safeString(department.getName()))));
            document.add(new Paragraph("Laboratory: "
                    + (laboratory == null ? "All" : safeString(laboratory.getName()))));
            document.add(new Paragraph("Investigation: "
                    + (investigation == null ? "All" : safeString(investigation.getName()))));
            document.add(new Paragraph("Gender: " + (isBlank(gender) ? "All" : gender)));
            document.add(new Paragraph("Visit Type: " + (isBlank(visitType) ? "All" : visitType)));
            document.add(new Paragraph(" "));

            if (getReportData().isEmpty()) {
                // NOTE: export a document with just the filters and a
                // "no records" note instead of blocking export.
                document.add(new Paragraph("No investigation records found for the selected filters."));
            } else {
                PdfPTable table = new PdfPTable(9);
                table.setWidthPercentage(100);

                String[] headers = {
                    "No.", "MRN", "Visit Type", "Laboratory", "Gender",
                    "Name", "Age", "Investigation", "Created Date"
                };

                for (String h : headers) {
                    table.addCell(new Phrase(h));
                }

                int number = 1;

                for (InvestigationWiseRow row : getReportData()) {
                    table.addCell(String.valueOf(number++));
                    table.addCell(safeString(row.getMrn()));
                    table.addCell(safeString(row.getVisitType()));
                    table.addCell(safeString(row.getLaboratoryName()));
                    table.addCell(safeString(row.getGender()));
                    table.addCell(safeString(row.getPatientName()));
                    table.addCell(safeString(row.getAge()));
                    table.addCell(safeString(row.getInvestigationName()));
                    table.addCell(formatDate(row.getCreatedAt()));
                }

                document.add(table);
            }

            document.close();
            facesContext.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();

            if (!facesContext.getResponseComplete()) {
                JsfUtil.addErrorMessage("Error exporting PDF file: " + e.getMessage());
            }
        }
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);

        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);

        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    private int createFilterRow(
            Sheet sheet,
            int rowIndex,
            String label,
            String value) {

        Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(safeString(value));

        return rowIndex + 1;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }

        SimpleDateFormat dateFormat
                = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        return dateFormat.format(date);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public boolean isReportEmpty() {
        return getReportData().isEmpty();
    }

    public Date getCurrentDate() {
        return new Date();
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(
                    new Date()
            );
        }

        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(
                    new Date()
            );
        }

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

    public Department getLaboratory() {
        return laboratory;
    }

    public void setLaboratory(Department laboratory) {
        this.laboratory = laboratory;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(
            Investigation investigation) {

        this.investigation = investigation;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<InvestigationWiseRow> getReportData() {
        if (reportData == null) {
            reportData = new ArrayList<>();
        }

        return reportData;
    }

    public void setReportData(
            List<InvestigationWiseRow> reportData) {

        this.reportData = reportData;
    }

    public static class InvestigationWiseRow
            implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private String mrn = "";
        private String visitType = "";
        private String laboratoryName = "";
        private String gender = "";
        private String patientName = "";
        private String age = "";
        private String investigationName = "";
        private Date createdAt;

        public InvestigationWiseRow() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getMrn() {
            return mrn;
        }

        public void setMrn(String mrn) {
            this.mrn = mrn;
        }

        public String getVisitType() {
            return visitType;
        }

        public void setVisitType(String visitType) {
            this.visitType = visitType;
        }

        public String getLaboratoryName() {
            return laboratoryName;
        }

        public void setLaboratoryName(
                String laboratoryName) {

            this.laboratoryName = laboratoryName;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(
                String patientName) {

            this.patientName = patientName;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }

        public String getInvestigationName() {
            return investigationName;
        }

        public void setInvestigationName(
                String investigationName) {

            this.investigationName
                    = investigationName;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }
}
