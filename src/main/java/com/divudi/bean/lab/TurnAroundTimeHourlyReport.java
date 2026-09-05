package com.divudi.bean.lab;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Route;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Turn Around Time (Hourly) Report.
 *
 * Groups certified/pending investigations by Investigation + Department and
 * buckets the elapsed time (order -&gt; result completion) into hourly bands.
 */
@Named(value = "tatHourlyReport")
@SessionScoped
public class TurnAroundTimeHourlyReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;

    // ---- Filters -----------------------------------------------------
    private Date fromDate;
    private Date toDate;

    private Institution institution;
    private Institution site;
    private Department department;
    private Department laboratoryDepartment;
    private Institution collectingCentre;
    private Route route;
    private Investigation investigation;
    private Staff referingDoctor;

    /**
     * "Results Pending" | "Result Certified" | null (=All)
     */
    private String resultStatus;

    /**
     * "OP" | "IP" | "CC" | "All" | null
     */
    private String visitType;

    // ---- Result ---------------------------------------------------
    private List<TatRow> reportData;

    public TurnAroundTimeHourlyReport() {
    }

    public void process() {
        reportData = new ArrayList<>();

        if (getFromDate() == null || getToDate() == null) {
            JsfUtil.addErrorMessage("Please select both From Date and To Date.");
            return;
        }

        if (getFromDate().after(getToDate())) {
            JsfUtil.addErrorMessage("From Date cannot be later than To Date.");
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
                // Filtered on the bill date, matching the "(Billed) Date" labels in the UI.
                .append("and b.createdAt between :fromDate and :toDate ");

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

        if (laboratoryDepartment != null) {
            jpql.append("and bi.item.department = :laboratoryDepartment ");
            parameters.put("laboratoryDepartment", laboratoryDepartment);
        }

        if (collectingCentre != null) {
            jpql.append("and b.collectingCentre = :collectingCentre ");
            parameters.put("collectingCentre", collectingCentre);
        }

        if (route != null) {
            jpql.append("and b.collectingCentre.route = :route ");
            parameters.put("route", route);
        }

        if (investigation != null) {
            jpql.append("and pi.investigation = :investigation ");
            parameters.put("investigation", investigation);
        }

        if (referingDoctor != null) {
            jpql.append("and b.referredBy = :referingDoctor ");
            parameters.put("referingDoctor", referingDoctor);
        }

        // Bill.getIpOpOrCc() derives the visit type from patientEncounter /
        // collectingCentre rather than reading the persisted column, which is
        // null on a large share of historical bills. Mirror that derivation here
        // so the filter agrees with what the rest of the application shows.
        if (!isBlank(visitType) && !"All".equalsIgnoreCase(visitType)) {
            String normalisedVisitType = visitType.trim().toUpperCase();

            if ("IP".equals(normalisedVisitType)) {
                jpql.append("and b.patientEncounter is not null ");
            } else if ("CC".equals(normalisedVisitType)) {
                jpql.append("and b.patientEncounter is null ")
                        .append("and b.collectingCentre is not null ");
            } else if ("OP".equals(normalisedVisitType)) {
                jpql.append("and b.patientEncounter is null ")
                        .append("and b.collectingCentre is null ");
            }
        }

        jpql.append("order by pi.investigation.name, bi.item.department.name");

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

            Map<String, TatRow> grouped = new LinkedHashMap<>();

            for (PatientInvestigation pi : investigations) {

                Date completionDate = resolveCompletionDate(pi);
                boolean isCompleted = completionDate != null;

                if ("Results Pending".equals(resultStatus) && isCompleted) {
                    continue;
                }

                if ("Result Certified".equals(resultStatus) && !isCompleted) {
                    continue;
                }

                String investigationName = pi.getInvestigation() != null
                        ? safeString(pi.getInvestigation().getName())
                        : "";

                Department labDept = pi.getBillItem() != null
                        && pi.getBillItem().getItem() != null
                        ? pi.getBillItem().getItem().getDepartment()
                        : null;

                String departmentName = labDept != null
                        ? safeString(labDept.getName())
                        : "";

                String key = investigationName + "|" + departmentName;

                TatRow row = grouped.get(key);

                if (row == null) {
                    row = new TatRow();
                    row.setInvestigationName(investigationName);
                    row.setDepartmentName(departmentName);
                    grouped.put(key, row);
                }

                if (!isCompleted) {
                    row.setPendingCount(row.getPendingCount() + 1);
                    continue;
                }

                row.setProcessedCount(row.getProcessedCount() + 1);

                long hours = elapsedHours(resolveStartDate(pi), completionDate);
                applyToBucket(row, hours);
            }

            reportData = new ArrayList<>(grouped.values());

            if (reportData.isEmpty()) {
                JsfUtil.addErrorMessage("No records were found for the selected filters.");
            } else {
                JsfUtil.addSuccessMessage(reportData.size() + " group(s) found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            reportData = new ArrayList<>();
            JsfUtil.addErrorMessage("Error generating report: " + e.getMessage());
        }
    }

    /**
     * The timestamp at which the result is considered certified. {@code approveAt}
     * is the point the result is authorised for release, which is what the
     * "Result Certified" filter means on this page.
     *
     * Returning null means "still pending" for this record.
     */
    private Date resolveCompletionDate(PatientInvestigation pi) {
        if (pi == null) {
            return null;
        }
        return pi.getApproveAt();
    }

    /**
     * The timestamp the turn around time is measured from. {@code orderedAt} is
     * when the investigation was requested; older records that predate that
     * field fall back to the row's creation time.
     */
    private Date resolveStartDate(PatientInvestigation pi) {
        if (pi == null) {
            return null;
        }
        return pi.getOrderedAt() != null ? pi.getOrderedAt() : pi.getCreatedAt();
    }

    private long elapsedHours(Date start, Date end) {
        if (start == null || end == null) {
            return 0;
        }

        long millis = end.getTime() - start.getTime();

        if (millis < 0) {
            return 0;
        }

        return millis / (1000L * 60L * 60L);
    }

    private void applyToBucket(TatRow row, long hours) {
        if (hours < 1) {
            row.setBucket0_1(row.getBucket0_1() + 1);
        } else if (hours < 2) {
            row.setBucket1_2(row.getBucket1_2() + 1);
        } else if (hours < 3) {
            row.setBucket2_3(row.getBucket2_3() + 1);
        } else if (hours < 4) {
            row.setBucket3_4(row.getBucket3_4() + 1);
        } else if (hours < 5) {
            row.setBucket4_5(row.getBucket4_5() + 1);
        } else if (hours < 6) {
            row.setBucket5_6(row.getBucket5_6() + 1);
        } else if (hours < 7) {
            row.setBucket6_7(row.getBucket6_7() + 1);
        } else if (hours < 8) {
            row.setBucket7_8(row.getBucket7_8() + 1);
        } else if (hours < 9) {
            row.setBucket8_9(row.getBucket8_9() + 1);
        } else if (hours < 10) {
            row.setBucket9_10(row.getBucket9_10() + 1);
        } else {
            row.setBucket10Plus(row.getBucket10Plus() + 1);
        }
    }

    public void clear() {
        institution = null;
        site = null;
        department = null;
        laboratoryDepartment = null;
        collectingCentre = null;
        route = null;
        investigation = null;
        referingDoctor = null;
        resultStatus = null;
        visitType = null;

        reportData = new ArrayList<>();

        fromDate = CommonFunctions.getStartOfDay(new Date());
        toDate = CommonFunctions.getEndOfDay(new Date());
    }

    // ---- Excel export -----------------------------------------------
    public void exportExcel() {

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.reset();
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"turn_around_time_hourly_report.xlsx\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("TAT Hourly Report");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);

            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Turn Around Time (Hourly) Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 14));

            rowIndex += 2;

            rowIndex = createFilterRow(sheet, rowIndex, "From Date", formatDate(getFromDate()));
            rowIndex = createFilterRow(sheet, rowIndex, "To Date", formatDate(getToDate()));
            rowIndex = createFilterRow(sheet, rowIndex, "Institution",
                    institution == null ? "All" : safeString(institution.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Site",
                    site == null ? "All" : safeString(site.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Department",
                    department == null ? "All" : safeString(department.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Investigation",
                    investigation == null ? "All" : safeString(investigation.getName()));
            rowIndex = createFilterRow(sheet, rowIndex, "Result Status",
                    isBlank(resultStatus) ? "All" : resultStatus);
            rowIndex = createFilterRow(sheet, rowIndex, "Visit Type",
                    isBlank(visitType) ? "All" : visitType);

            rowIndex++;

            String[] headers = {
                "Investigation", "Department",
                "0-1(hr)", "1-2(hrs)", "2-3(hrs)", "3-4(hrs)", "4-5(hrs)",
                "5-6(hrs)", "6-7(hrs)", "7-8(hrs)", "8-9(hrs)", "9-10(hrs)",
                ">=10(hrs)", "Processed", "Pending"
            };

            Row headerRow = sheet.createRow(rowIndex++);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (getReportData().isEmpty()) {
                Row emptyRow = sheet.createRow(rowIndex++);
                Cell emptyCell = emptyRow.createCell(0);
                emptyCell.setCellValue("No records found for the selected filters.");
                sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 14));
            } else {
                for (TatRow r : getReportData()) {
                    Row excelRow = sheet.createRow(rowIndex++);

                    excelRow.createCell(0).setCellValue(safeString(r.getInvestigationName()));
                    excelRow.createCell(1).setCellValue(safeString(r.getDepartmentName()));
                    excelRow.createCell(2).setCellValue(r.getBucket0_1());
                    excelRow.createCell(3).setCellValue(r.getBucket1_2());
                    excelRow.createCell(4).setCellValue(r.getBucket2_3());
                    excelRow.createCell(5).setCellValue(r.getBucket3_4());
                    excelRow.createCell(6).setCellValue(r.getBucket4_5());
                    excelRow.createCell(7).setCellValue(r.getBucket5_6());
                    excelRow.createCell(8).setCellValue(r.getBucket6_7());
                    excelRow.createCell(9).setCellValue(r.getBucket7_8());
                    excelRow.createCell(10).setCellValue(r.getBucket8_9());
                    excelRow.createCell(11).setCellValue(r.getBucket9_10());
                    excelRow.createCell(12).setCellValue(r.getBucket10Plus());
                    excelRow.createCell(13).setCellValue(r.getProcessedCount());
                    excelRow.createCell(14).setCellValue(r.getPendingCount());
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 8000) {
                    sheet.setColumnWidth(i, 8000);
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

    // ---- PDF export -----------------------------------------------
    public void exportPdf() {

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        try {
            Document document = new Document(PageSize.A4.rotate(), 15, 15, 15, 15);

            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"turn_around_time_hourly_report.pdf\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            document.add(new Paragraph("Turn Around Time (Hourly) Report"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("From Date: " + formatDate(getFromDate())));
            document.add(new Paragraph("To Date: " + formatDate(getToDate())));
            document.add(new Paragraph("Institution: "
                    + (institution == null ? "All" : safeString(institution.getName()))));
            document.add(new Paragraph("Site: "
                    + (site == null ? "All" : safeString(site.getName()))));
            document.add(new Paragraph("Department: "
                    + (department == null ? "All" : safeString(department.getName()))));
            document.add(new Paragraph("Investigation: "
                    + (investigation == null ? "All" : safeString(investigation.getName()))));
            document.add(new Paragraph("Result Status: "
                    + (isBlank(resultStatus) ? "All" : resultStatus)));
            document.add(new Paragraph("Visit Type: "
                    + (isBlank(visitType) ? "All" : visitType)));
            document.add(new Paragraph(" "));

            if (getReportData().isEmpty()) {
                document.add(new Paragraph("No records found for the selected filters."));
            } else {
                PdfPTable table = new PdfPTable(15);
                table.setWidthPercentage(100);

                String[] headers = {
                    "Investigation", "Department",
                    "0-1", "1-2", "2-3", "3-4", "4-5",
                    "5-6", "6-7", "7-8", "8-9", "9-10",
                    ">=10", "Processed", "Pending"
                };

                for (String h : headers) {
                    table.addCell(new Phrase(h));
                }

                for (TatRow r : getReportData()) {
                    table.addCell(safeString(r.getInvestigationName()));
                    table.addCell(safeString(r.getDepartmentName()));
                    table.addCell(String.valueOf(r.getBucket0_1()));
                    table.addCell(String.valueOf(r.getBucket1_2()));
                    table.addCell(String.valueOf(r.getBucket2_3()));
                    table.addCell(String.valueOf(r.getBucket3_4()));
                    table.addCell(String.valueOf(r.getBucket4_5()));
                    table.addCell(String.valueOf(r.getBucket5_6()));
                    table.addCell(String.valueOf(r.getBucket6_7()));
                    table.addCell(String.valueOf(r.getBucket7_8()));
                    table.addCell(String.valueOf(r.getBucket8_9()));
                    table.addCell(String.valueOf(r.getBucket9_10()));
                    table.addCell(String.valueOf(r.getBucket10Plus()));
                    table.addCell(String.valueOf(r.getProcessedCount()));
                    table.addCell(String.valueOf(r.getPendingCount()));
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

    // ---- Excel/PDF style helpers --------------------------------------
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

    private int createFilterRow(Sheet sheet, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(safeString(value));
        return rowIndex + 1;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormat.format(date);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ---- Getters / setters --------------------------------------------
    public boolean isReportEmpty() {
        return getReportData().isEmpty();
    }

    public Date getCurrentDate() {
        return new Date();
    }

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

    public Department getLaboratoryDepartment() {
        return laboratoryDepartment;
    }

    public void setLaboratoryDepartment(Department laboratoryDepartment) {
        this.laboratoryDepartment = laboratoryDepartment;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public Staff getReferingDoctor() {
        return referingDoctor;
    }

    public void setReferingDoctor(Staff referingDoctor) {
        this.referingDoctor = referingDoctor;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public List<TatRow> getReportData() {
        if (reportData == null) {
            reportData = new ArrayList<>();
        }
        return reportData;
    }

    public void setReportData(List<TatRow> reportData) {
        this.reportData = reportData;
    }

    // ---- Row model ------------------------------------------------
    public static class TatRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private String investigationName = "";
        private String departmentName = "";

        private int bucket0_1;
        private int bucket1_2;
        private int bucket2_3;
        private int bucket3_4;
        private int bucket4_5;
        private int bucket5_6;
        private int bucket6_7;
        private int bucket7_8;
        private int bucket8_9;
        private int bucket9_10;
        private int bucket10Plus;

        private int processedCount;
        private int pendingCount;

        public String getInvestigationName() {
            return investigationName;
        }

        public void setInvestigationName(String investigationName) {
            this.investigationName = investigationName;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public int getBucket0_1() {
            return bucket0_1;
        }

        public void setBucket0_1(int bucket0_1) {
            this.bucket0_1 = bucket0_1;
        }

        public int getBucket1_2() {
            return bucket1_2;
        }

        public void setBucket1_2(int bucket1_2) {
            this.bucket1_2 = bucket1_2;
        }

        public int getBucket2_3() {
            return bucket2_3;
        }

        public void setBucket2_3(int bucket2_3) {
            this.bucket2_3 = bucket2_3;
        }

        public int getBucket3_4() {
            return bucket3_4;
        }

        public void setBucket3_4(int bucket3_4) {
            this.bucket3_4 = bucket3_4;
        }

        public int getBucket4_5() {
            return bucket4_5;
        }

        public void setBucket4_5(int bucket4_5) {
            this.bucket4_5 = bucket4_5;
        }

        public int getBucket5_6() {
            return bucket5_6;
        }

        public void setBucket5_6(int bucket5_6) {
            this.bucket5_6 = bucket5_6;
        }

        public int getBucket6_7() {
            return bucket6_7;
        }

        public void setBucket6_7(int bucket6_7) {
            this.bucket6_7 = bucket6_7;
        }

        public int getBucket7_8() {
            return bucket7_8;
        }

        public void setBucket7_8(int bucket7_8) {
            this.bucket7_8 = bucket7_8;
        }

        public int getBucket8_9() {
            return bucket8_9;
        }

        public void setBucket8_9(int bucket8_9) {
            this.bucket8_9 = bucket8_9;
        }

        public int getBucket9_10() {
            return bucket9_10;
        }

        public void setBucket9_10(int bucket9_10) {
            this.bucket9_10 = bucket9_10;
        }

        public int getBucket10Plus() {
            return bucket10Plus;
        }

        public void setBucket10Plus(int bucket10Plus) {
            this.bucket10Plus = bucket10Plus;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public void setProcessedCount(int processedCount) {
            this.processedCount = processedCount;
        }

        public int getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(int pendingCount) {
            this.pendingCount = pendingCount;
        }
    }
}
