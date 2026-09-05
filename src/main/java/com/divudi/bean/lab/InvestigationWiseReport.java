package com.divudi.bean.lab;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Person;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

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

    /**
     * Upper bound on the rows one Process run may load. PatientInvestigation
     * maps 56 to-one associations and none of them declare LAZY, so every row
     * drags its whole entity graph into the persistence context — and this bean
     * is @SessionScoped, so whatever is loaded stays resident. An unbounded
     * range (a year is ~143,000 rows locally) is enough to exhaust the heap.
     */
    private static final int MAX_REPORT_ROWS = 10000;

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

        if (fromDate == null || toDate == null) {
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

        // Only the columns the report shows are selected. Loading whole
        // PatientInvestigation entities pulls 56 to-one associations per row
        // (none of them declare LAZY), which made a month-wide range take
        // minutes; the projection reads the nine displayed values instead.
        jpql.append("select pi.id, pi.createdAt, patient.phn, ")
                .append("person.title, person.name, person.sex, person.dob, ")
                .append("inv.name, b.ipOpOrCc, itemDept.name ")
                .append("from PatientInvestigation pi ")
                .append("join pi.billItem bi ")
                .append("join bi.bill b ")
                .append("left join pi.investigation inv ")
                .append("left join pi.patient patient ")
                .append("left join patient.person person ")
                .append("left join bi.item item ")
                .append("left join item.department itemDept ")
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
            jpql.append("and itemDept = :laboratory ");
            parameters.put("laboratory", laboratory);
        }

        if (investigation != null) {
            jpql.append("and pi.investigation = :investigation ");
            parameters.put("investigation", investigation);
        }

        Sex selectedSex = resolveSex(gender);

        if (selectedSex != null) {
            jpql.append("and person.sex = :sex ");
            parameters.put("sex", selectedSex);
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
            // One row beyond the cap tells truncation apart from a result that
            // happens to be exactly MAX_REPORT_ROWS long.
            List<Object[]> rows
                    = patientInvestigationFacade.findObjectsArrayByJpql(
                            jpql.toString(),
                            parameters,
                            TemporalType.TIMESTAMP,
                            MAX_REPORT_ROWS + 1
                    );

            if (rows == null) {
                rows = new ArrayList<>();
            }

            boolean truncated = rows.size() > MAX_REPORT_ROWS;

            if (truncated) {
                rows = rows.subList(0, MAX_REPORT_ROWS);
            }

            for (Object[] columns : rows) {
                reportData.add(createReportRow(columns));
            }

            if (reportData.isEmpty()) {
                JsfUtil.addErrorMessage(
                        "No investigation records were found."
                );
            } else if (truncated) {
                JsfUtil.addErrorMessage(
                        "Showing the first " + MAX_REPORT_ROWS
                        + " records only. Narrow the date range or filters"
                        + " to see the rest."
                );
            } else {
                JsfUtil.addSuccessMessage(
                        reportData.size() + " records found."
                );
            }

        } catch (Exception e) {
            // The detail stays in the server log; a query or schema fragment in
            // the growl would leak infrastructure information to the user.
            e.printStackTrace();

            reportData = new ArrayList<>();

            JsfUtil.addErrorMessage(
                    "Error generating the report. Please check the server log."
            );
        }
    }

    /**
     * A department or laboratory chosen under the previous institution is no
     * longer in the refreshed lists, but would otherwise stay set on this
     * session-scoped bean and silently filter the report down to nothing.
     */
    public void institutionChanged() {
        site = null;
        department = null;
        laboratory = null;
    }

    public void siteChanged() {
        department = null;
        laboratory = null;
    }

    /**
     * Builds a report row from one projected result tuple. The column order
     * must match the select clause in {@link #process()}.
     */
    private InvestigationWiseRow createReportRow(Object[] columns) {

        InvestigationWiseRow row = new InvestigationWiseRow();

        if (columns == null) {
            return row;
        }

        row.setId((Long) columns[0]);
        row.setCreatedAt((Date) columns[1]);
        row.setMrn(safeString((String) columns[2]));

        Title title = (Title) columns[3];
        String name = safeString((String) columns[4]);

        row.setPatientName(
                title == null ? name : (title.getLabel() + " " + name).trim()
        );

        Sex sex = (Sex) columns[5];

        if (sex != null) {
            row.setGender(safeString(sex.getLabel()));
        }

        row.setAge(formatAge((Date) columns[6]));
        row.setInvestigationName(safeString((String) columns[7]));
        row.setVisitType(safeString((String) columns[8]));
        row.setLaboratoryName(safeString((String) columns[9]));

        return row;
    }

    /**
     * Formats a date of birth the same way the patient screens do, by reusing
     * Person's own age calculation rather than duplicating it here.
     */
    private String formatAge(Date dateOfBirth) {
        if (dateOfBirth == null) {
            return "";
        }

        Person person = new Person();
        person.setDob(dateOfBirth);

        return safeString(person.getAgeAsString());
    }

    /**
     * Maps the Gender dropdown value onto the Sex enum so the filter can run in
     * the query. Returns null for "All" or anything unrecognised, which leaves
     * the filter off rather than silently matching nothing.
     */
    private Sex resolveSex(String selectedGender) {
        if (isBlank(selectedGender)) {
            return null;
        }

        for (Sex sex : Sex.values()) {
            if (sex.getLabel().equalsIgnoreCase(selectedGender.trim())) {
                return sex;
            }
        }

        return null;
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
