package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.OtRoomWiseSurgeryCountDTO;
import com.divudi.core.data.dto.SurgeryReportDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.util.stream.Collectors;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Legend;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.options.elements.Fill;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearTickOptions;

@Named
@SessionScoped
public class SurgeryReportController implements Serializable {

    public SurgeryReportController() {
    }

    @EJB
    BillFacade billFacade;

    private Institution institution;
    private Institution site;
    private Department department;

    private Date fromDate;
    private Date toDate;

    private Item procedure;
    private RoomFacilityCharge operationTheatreRoom;
    private List<SurgeryReportDTO> reportList;

    private Date fromYearDate;
    private Date toYearDate;
    private String otRoomChartType;
    private List<OtRoomWiseSurgeryCountDTO> otRoomWiseList;
    private Map<Integer, Long> otRoomMonthlyTotals;
    private long otRoomGrandTotal;
    private String otRoomBarChartModel;
    private String otRoomLineChartModel;
    private String otRoomBarChartImage;
    private String otRoomLineChartImage;
    private int selectedYear;

    public void processSurgeryStatusReport() {
        reportList = new ArrayList<>();
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From Date cannot be after To Date.");
            return;
        }

        StringBuilder queryBody = new StringBuilder();
        queryBody.append(" from Bill b ")
                .append(" join b.procedure p ")
                .append(" join p.item i ")
                .append(" join b.patientEncounter pe ")
                .append(" join pe.patient pat ")
                .append(" join pat.person pp ")
                .append(" left join pe.currentPatientRoom cpr ")
                .append(" left join cpr.roomFacilityCharge rfc ")
                .append(" left join b.staff st ")
                .append(" left join st.person stp ")
                .append(" left join pe.referringConsultant rc ")
                .append(" left join rc.person rcp ")
                .append(" join b.department d ")
                .append(" where b.retired = false ")
                .append(" and b.cancelled = false ")
                .append(" and b.billType = :bt ")
                .append(" and b.createdAt between :fd and :td ");

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.SurgeryBill);
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (institution != null) {
            queryBody.append(" and b.institution = :inst ");
            params.put("inst", institution);
        }
        if (department != null) {
            queryBody.append(" and d = :dept ");
            params.put("dept", department);
        }
        if (site != null) {
            queryBody.append(" and d.site = :site ");
            params.put("site", site);
        }
        if (procedure != null) {
            queryBody.append(" and i = :proc ");
            params.put("proc", procedure);
        }
        if (operationTheatreRoom != null) {
            queryBody.append(" and exists ( ")
                    .append("   select 1 from PatientTransferRequest otr ")
                    .append("   where otr.retired = false ")
                    .append("   and otr.toRoomFacilityCharge = :otRoom ")
                    .append("   and (otr.admission.id = pe.id or otr.surgeryBill.id = b.id) ")
                    .append(" ) ");
            params.put("otRoom", operationTheatreRoom);
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.SurgeryReportDTO( ")
                .append("   b.id, pat.phn, pp.name, pe.dateOfAdmission, i.name, ")
                .append("   rfc.department.name, rfc.name, stp.name, rcp.title, rcp.name, pe.id, p.id ")
                .append(" ) ")
                .append(queryBody)
                .append(" order by i.name ");

        reportList = (List<SurgeryReportDTO>) billFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP, 1000);

        if (reportList.size() == 1000) {
            JsfUtil.addErrorMessage("Report may be incomplete because results were limited to 1,000 records.");
        }

        attachOtStatuses(reportList, queryBody.toString(), params);
        attachSurgeons(reportList, queryBody.toString(), params);
    }

    private void attachSurgeons(List<SurgeryReportDTO> rows, String queryBody, Map<String, Object> params) {
        if (rows.isEmpty()) {
            return;
        }

        String subquery = " (SELECT p.id " + queryBody + ") ";

        String jpql = "SELECT pe.id, ce.id, ec.patientEncounterComponentType, "
                + " stp.title, stp.name, bfstp.title, bfstp.name "
                + " FROM EncounterComponent ec "
                + " LEFT JOIN ec.patientEncounter pe "
                + " LEFT JOIN ec.childEncounter ce "
                + " LEFT JOIN ec.staff st "
                + " LEFT JOIN st.person stp "
                + " LEFT JOIN ec.billFee bf "
                + " LEFT JOIN bf.staff bfst "
                + " LEFT JOIN bfst.person bfstp "
                + " WHERE (pe.id IN " + subquery + " OR ce.id IN " + subquery + ") "
                + " AND ec.retired = false "
                + " AND (ec.patientEncounterComponentType = :type1 "
                + "      OR ec.patientEncounterComponentType = :type2) "
                + " ORDER BY ec.orderNo ";

        Map<String, Object> p = new HashMap<>(params);
        p.put("type1", com.divudi.core.data.inward.PatientEncounterComponentType.Performed_By);
        p.put("type2", com.divudi.core.data.inward.PatientEncounterComponentType.Assisted_by);

        List<Object[]> docRows = billFacade.findAggregates(jpql, p, TemporalType.TIMESTAMP);

        Map<Long, List<String>> doctorsMap = new HashMap<>();

        for (Object[] row : docRows) {
            Long peId = (Long) row[0];
            Long ceId = (Long) row[1];
            com.divudi.core.data.inward.PatientEncounterComponentType type
                    = (com.divudi.core.data.inward.PatientEncounterComponentType) row[2];

            com.divudi.core.data.Title staffTitle1 = (com.divudi.core.data.Title) row[3];
            String staffName1 = (String) row[4];
            com.divudi.core.data.Title staffTitle2 = (com.divudi.core.data.Title) row[5];
            String staffName2 = (String) row[6];

            String nameToUse = (staffName1 != null && !staffName1.trim().isEmpty())
                    ? staffName1.trim()
                    : (staffName2 != null ? staffName2.trim() : null);

            com.divudi.core.data.Title titleToUse = staffTitle1 != null ? staffTitle1 : staffTitle2;

            if (nameToUse == null || nameToUse.isEmpty()) {
                continue;
            }

            StringBuilder fullName = new StringBuilder();

            if (titleToUse != null) {
                fullName.append(titleToUse.toString()).append(" ");
            }

            fullName.append(nameToUse);

            if (type == com.divudi.core.data.inward.PatientEncounterComponentType.Assisted_by) {
                fullName.append(" (Assisted)");
            }

            String finalName = fullName.toString().trim();

            if (peId != null) {
                doctorsMap.computeIfAbsent(peId, k -> new ArrayList<>()).add(finalName);
            }
            if (ceId != null && (peId == null || !peId.equals(ceId))) {
                doctorsMap.computeIfAbsent(ceId, k -> new ArrayList<>()).add(finalName);
            }
        }

        for (SurgeryReportDTO r : rows) {
            if (r.getProcedureId() != null) {
                List<String> docs = doctorsMap.get(r.getProcedureId());
                if (docs != null && !docs.isEmpty()) {
                    List<String> uniqueDocs = docs.stream()
                            .distinct()
                            .collect(Collectors.toList());
                    r.setSurgeonName(String.join(", ", uniqueDocs));
                }
            }
        }
    }

    private void attachOtStatuses(List<SurgeryReportDTO> rows, String queryBody, Map<String, Object> params) {
        if (rows.isEmpty()) {
            return;
        }

        String encSubquery = " (SELECT pe.id " + queryBody + ") ";
        String billSubquery = " (SELECT b.id " + queryBody + ") ";

        String jpql = " select str.admission.id, str.surgeryBill.id, str.theatreOccupancyStatus , str.toRoomFacilityCharge.name "
                + " from PatientTransferRequest str "
                + " where str.retired = false "
                + " and (str.admission.id in " + encSubquery + " OR str.surgeryBill.id in " + billSubquery + ") "
                + " and str.theatreOccupancyStatus is not null "
                + " order by str.createdAt ASC";

        Map<String, Object> p = new HashMap<>(params);
        List<Object[]> statusRows = billFacade.findAggregates(jpql, p, TemporalType.TIMESTAMP);

        Map<Long, String> statusByEncounter = new HashMap<>();
        Map<Long, String> statusByBill = new HashMap<>();
        Map<Long, String> otRooms = new HashMap<>();

        for (Object[] row : statusRows) {
            Long encounterId = (Long) row[0];
            Long billId = (Long) row[1];
            Object statusObj = row[2];
            String otRoom = (String) row[3];
            String status = statusObj != null ? statusObj.toString() : "";

            if (billId != null) {
                statusByBill.put(billId, status);
            }
            if (encounterId != null) {
                statusByEncounter.put(encounterId, status);
            }
            if (otRoom != null && billId != null) {
                otRooms.put(billId, otRoom);
            }
            if (otRoom != null && encounterId != null) {
                otRooms.put(encounterId, otRoom);
            }
        }

        for (SurgeryReportDTO r : rows) {
            if (statusByBill.containsKey(r.getBillId())) {
                r.setOtStatus(statusByBill.get(r.getBillId()));
            } else if (statusByEncounter.containsKey(r.getPatientEncounterId())) {
                r.setOtStatus(statusByEncounter.get(r.getPatientEncounterId()));
            } else {
                r.setOtStatus("-");
            }

            if (otRooms.containsKey(r.getBillId())) {
                r.setOtRoomName(otRooms.get(r.getBillId()));
            } else if (otRooms.containsKey(r.getPatientEncounterId())) {
                r.setOtRoomName(otRooms.get(r.getPatientEncounterId()));
            } else {
                r.setOtRoomName("-");
            }
        }
    }

    private static final String[] REPORT_HEADERS = {
        "SL No.", "MRN", "Patient Name", "Admission Date", "Proposed Surgery",
        "OT Room", "Ward", "Surgeon", "OT Status", "Consultant"
    };

    public void downloadExcelReport() {
        if (reportList == null || reportList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please run the report first.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Surgery Report");

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dateStyle = workbook.createCellStyle();
            short dateFmt = workbook.getCreationHelper().createDataFormat().getFormat("dd/MM/yyyy HH:mm");
            dateStyle.setDataFormat(dateFmt);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            int rowIdx = 0;

            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Surgery Status Report");
            titleCell.setCellStyle(titleStyle);
            rowIdx++; // blank row

            Row dateRow = sheet.createRow(rowIdx++);
            dateRow.createCell(0).setCellValue("Date Range: " + sdf.format(fromDate) + " - " + sdf.format(toDate));

            if (operationTheatreRoom != null) {
                Row otRow = sheet.createRow(rowIdx++);
                otRow.createCell(0).setCellValue("OT Room: " + operationTheatreRoom.getName());
            }

            if (procedure != null) {
                Row procRow = sheet.createRow(rowIdx++);
                procRow.createCell(0).setCellValue("Proposed Surgery: " + procedure.getName());
            }
            rowIdx++; // blank row

            Row headerRow = sheet.createRow(rowIdx++);
            for (int c = 0; c < REPORT_HEADERS.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(REPORT_HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            int sl = 1;
            for (SurgeryReportDTO dto : reportList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sl++);
                row.createCell(1).setCellValue(nullSafe(dto.getMrn()));
                row.createCell(2).setCellValue(nullSafe(dto.getPatientName()));

                Cell admissionCell = row.createCell(3);
                if (dto.getAdmissionDate() != null) {
                    admissionCell.setCellValue(dto.getAdmissionDate());
                    admissionCell.setCellStyle(dateStyle);
                }

                row.createCell(4).setCellValue(nullSafe(dto.getProcedureName()));
                row.createCell(5).setCellValue(nullSafe(dto.getOtRoomName()));
                row.createCell(6).setCellValue(nullSafe(dto.getWardName()));
                row.createCell(7).setCellValue(nullSafe(dto.getSurgeonName()));
                row.createCell(8).setCellValue(nullSafe(dto.getOtStatus()));
                row.createCell(9).setCellValue(nullSafe(dto.getConsultantName()));
            }

            for (int c = 0; c < REPORT_HEADERS.length; c++) {
                sheet.autoSizeColumn(c);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            streamToResponse(bos.toByteArray(), "Surgery_Report.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to generate Excel report: " + e.getMessage());
        }
    }

    public void downloadPdfReport() {
        if (reportList == null || reportList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please run the report first.");
            return;
        }

        try {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, bos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Paragraph title = new Paragraph("Surgery Status Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Font filterFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph filters = new Paragraph();
            filters.add(new Phrase("Date Range: " + sdf.format(fromDate) + " - " + sdf.format(toDate) + "\n", filterFont));
            if (operationTheatreRoom != null) {
                filters.add(new Phrase("OT Room: " + operationTheatreRoom.getName() + "\n", filterFont));
            }
            if (procedure != null) {
                filters.add(new Phrase("Proposed Surgery: " + procedure.getName() + "\n", filterFont));
            }
            filters.setSpacingAfter(10);
            document.add(filters);

            PdfPTable table = new PdfPTable(REPORT_HEADERS.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5, 8, 12, 10, 12, 8, 8, 10, 8, 10});

            for (String h : REPORT_HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new java.awt.Color(220, 220, 220));
                cell.setPadding(4);
                table.addCell(cell);
            }

            SimpleDateFormat rowSdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            int sl = 1;
            for (SurgeryReportDTO dto : reportList) {
                addCell(table, String.valueOf(sl++), cellFont);
                addCell(table, nullSafe(dto.getMrn()), cellFont);
                addCell(table, nullSafe(dto.getPatientName()), cellFont);
                addCell(table, dto.getAdmissionDate() != null ? rowSdf.format(dto.getAdmissionDate()) : "", cellFont);
                addCell(table, nullSafe(dto.getProcedureName()), cellFont);
                addCell(table, nullSafe(dto.getOtRoomName()), cellFont);
                addCell(table, nullSafe(dto.getWardName()), cellFont);
                addCell(table, nullSafe(dto.getSurgeonName()), cellFont);
                addCell(table, nullSafe(dto.getOtStatus()), cellFont);
                addCell(table, nullSafe(dto.getConsultantName()), cellFont);
            }

            document.add(table);
            document.close();

            streamToResponse(bos.toByteArray(), "Surgery_Report.pdf", "application/pdf");

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to generate PDF report: " + e.getMessage());
        }
    }

    private void addCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setPadding(3);
        table.addCell(cell);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private void streamToResponse(byte[] data, String fileName, String contentType) throws java.io.IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType(contentType);
        response.setContentLength(data.length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        try (OutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
        }
        fc.responseComplete();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OT Room Wise Surgery Count Report
    // ═══════════════════════════════════════════════════════════════════════
    private static final String[] MONTH_SHORT_LABELS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private static final String[] CHART_COLORS = {
        "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
        "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
        "255, 99, 255", "99, 255, 132", "220, 20, 60", "65, 105, 225"
    };

    public void processOtRoomWiseSurgeryCountReport() {
        otRoomWiseList = new ArrayList<>();
        otRoomMonthlyTotals = new HashMap<>();
        otRoomGrandTotal = 0;
        otRoomBarChartModel = null;
        otRoomLineChartModel = null;

        if (fromYearDate == null || toYearDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return;
        }
        if (fromYearDate.after(toYearDate)) {
            JsfUtil.addErrorMessage("From Date cannot be after To Date.");
            return;
        }

        Calendar from = Calendar.getInstance();
        from.setTime(fromYearDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toYearDate);

        if (from.get(Calendar.YEAR) != to.get(Calendar.YEAR)) {
            JsfUtil.addErrorMessage(
                    "Please select a date range within a single calendar year. "
                    + "Monthly totals are grouped by month only, so a "
                    + "multi-year range would merge counts from different years.");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = buildOtRoomWiseJpql(params);

        List<Object[]> results = billFacade.findObjectArrayByJpql(
                jpql, params, TemporalType.TIMESTAMP);

        if (results == null || results.isEmpty()) {
            JsfUtil.addErrorMessage("No surgery records found for the selected period.");
            return;
        }

        aggregateOtRoomResults(results);

        if (otRoomChartType != null && !otRoomChartType.isEmpty()) {
            createOtRoomChartModels();
        }
    }

    private String buildOtRoomWiseJpql(Map<String, Object> params) {
        StringBuilder jpql = new StringBuilder();
        jpql.append(" select rfc.name, rfc.id, ")
                .append(" function('MONTH', ptr.createdAt), ")
                .append(" count(ptr) ")
                .append(" from PatientTransferRequest ptr ")
                .append(" join ptr.toRoomFacilityCharge rfc ")
                .append(" where ptr.retired = false ")
                .append(" and rfc.department.departmentType = :deptType ")
                .append(" and ptr.createdAt between :fd and :td ");

        params.put("deptType", DepartmentType.Theatre);
        params.put("fd", fromYearDate);
        params.put("td", toYearDate);

        jpql.append(" group by rfc.name, rfc.id, ")
                .append(" function('MONTH', ptr.createdAt) ")
                .append(" order by rfc.name ");

        return jpql.toString();
    }

    private void aggregateOtRoomResults(List<Object[]> results) {
        // Use LinkedHashMap to preserve insertion order (alphabetical from ORDER BY)
        Map<Long, OtRoomWiseSurgeryCountDTO> roomMap = new LinkedHashMap<>();

        for (Object[] row : results) {
            String roomName = (String) row[0];
            Long roomId = ((Number) row[1]).longValue();
            int month = ((Number) row[2]).intValue();
            long count = ((Number) row[3]).longValue();
            int monthIndex = month - 1; // JPQL MONTH() returns 1-12

            if (monthIndex < 0 || monthIndex >= 12) {
                continue;
            }

            OtRoomWiseSurgeryCountDTO dto = roomMap.get(roomId);
            if (dto == null) {
                dto = new OtRoomWiseSurgeryCountDTO(roomName, roomId);
                roomMap.put(roomId, dto);
            }
            dto.addCount(monthIndex, count);

            otRoomMonthlyTotals.merge(monthIndex, count, Long::sum);
            otRoomGrandTotal += count;
        }

        otRoomWiseList = new ArrayList<>(roomMap.values());
    }

    // ── Chart generation ─────────────────────────────────────────────────
    private void createOtRoomChartModels() {
        if (otRoomWiseList == null || otRoomWiseList.isEmpty()) {
            otRoomBarChartModel = null;
            otRoomLineChartModel = null;
            return;
        }
        createOtRoomBarChart();
        createOtRoomLineChart();
    }

    private void createOtRoomBarChart() {
        if (otRoomWiseList == null || otRoomWiseList.isEmpty()) {
            otRoomBarChartModel = null;
            return;
        }

        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels(MONTH_SHORT_LABELS);

        int colorIndex = 0;
        for (OtRoomWiseSurgeryCountDTO dto : otRoomWiseList) {
            int[] rgb = parseRgb(CHART_COLORS[colorIndex % CHART_COLORS.length]);

            BarDataset dataset = new BarDataset()
                    .setLabel(dto.getRoomName())
                    .setBackgroundColor(toRgba(rgb, 0.7))
                    .setBorderColor(toRgba(rgb, 1))
                    .setBorderWidth(1);

            for (int m = 0; m < 12; m++) {
                dataset.addData(dto.getCount(m));
            }
            barData.addDataset(dataset);
            colorIndex++;
        }

        barChart.setData(barData);

        BarOptions barOptionsObj = new BarOptions();
        barOptionsObj.setPlugins(buildOtRoomChartPlugins());
        barOptionsObj.setScales(buildOtRoomChartScales());
        barChart.setOptions(barOptionsObj);

        otRoomBarChartModel = barChart.toJson();
    }

    private void createOtRoomLineChart() {
        if (otRoomWiseList == null || otRoomWiseList.isEmpty()) {
            otRoomLineChartModel = null;
            return;
        }

        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels(MONTH_SHORT_LABELS);

        int colorIndex = 0;
        for (OtRoomWiseSurgeryCountDTO dto : otRoomWiseList) {
            int[] rgb = parseRgb(CHART_COLORS[colorIndex % CHART_COLORS.length]);

            LineDataset dataset = new LineDataset()
                    .setLabel(dto.getRoomName())
                    .setBorderColor(toRgba(rgb, 1))
                    .setFill(new Fill(false))
                    .setTension(0.4f);

            for (int m = 0; m < 12; m++) {
                dataset.addData(dto.getCount(m));
            }
            lineData.addDataset(dataset);
            colorIndex++;
        }

        lineChart.setData(lineData);

        LineOptions lineOptionsObj = new LineOptions();
        lineOptionsObj.setPlugins(buildOtRoomChartPlugins());
        lineOptionsObj.setScales(buildOtRoomChartScales());
        lineChart.setOptions(lineOptionsObj);

        otRoomLineChartModel = lineChart.toJson();
    }

    private Plugins buildOtRoomChartPlugins() {
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true)
                .setText("OT Room Wise Surgery Count - Year " + getOtRoomSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        return plugins;
    }

    private Scales buildOtRoomChartScales() {
        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions()
                .setBeginAtZero(true)
                .setTicks(new LinearTickOptions().setStepSize(1)));
        return scales;
    }

    public int getOtRoomSelectedYear() {
        Calendar cal = Calendar.getInstance();
        if (fromYearDate != null) {
            cal.setTime(fromYearDate);
        }
        return cal.get(Calendar.YEAR);
    }

    private int[] parseRgb(String csv) {
        String[] parts = csv.split(",");
        return new int[]{
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        };
    }

    private RGBAColor toRgba(int[] rgb, double alpha) {
        return new RGBAColor(rgb[0], rgb[1], rgb[2], alpha);
    }

    // ── Excel export ─────────────────────────────────────────────────────
    public void downloadOtRoomWiseExcel() {
        if (otRoomWiseList == null || otRoomWiseList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please run the report first.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("OT Room Surgery Count");

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle footerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font footerFont = workbook.createFont();
            footerFont.setBold(true);
            footerStyle.setFont(footerFont);
            footerStyle.setAlignment(HorizontalAlignment.RIGHT);

            int rowIdx = 0;

            // Title row
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("OT Room Wise Surgery Count - " + getOtRoomSelectedYear());
            titleCell.setCellStyle(titleStyle);
            rowIdx++; // blank row

            // Year header row (merged)
            Row yearRow = sheet.createRow(rowIdx++);
            yearRow.createCell(0); // empty for room name column
            Cell yearCell = yearRow.createCell(1);
            yearCell.setCellValue(String.valueOf(getOtRoomSelectedYear()));
            yearCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 13));

            // Month header row
            Row monthRow = sheet.createRow(rowIdx++);
            Cell roomHeader = monthRow.createCell(0);
            roomHeader.setCellValue("OT Room");
            roomHeader.setCellStyle(headerStyle);
            for (int m = 0; m < 12; m++) {
                Cell cell = monthRow.createCell(m + 1);
                cell.setCellValue(MONTH_SHORT_LABELS[m]);
                cell.setCellStyle(headerStyle);
            }
            Cell totalHeader = monthRow.createCell(13);
            totalHeader.setCellValue("Total");
            totalHeader.setCellStyle(headerStyle);

            // Data rows
            for (OtRoomWiseSurgeryCountDTO dto : otRoomWiseList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullSafe(dto.getRoomName()));
                for (int m = 0; m < 12; m++) {
                    Cell cell = row.createCell(m + 1);
                    cell.setCellValue(dto.getCount(m));
                    cell.setCellStyle(numberStyle);
                }
                Cell totalCell = row.createCell(13);
                totalCell.setCellValue(dto.getTotalCount());
                totalCell.setCellStyle(footerStyle);
            }

            // Footer row
            Row footerRow = sheet.createRow(rowIdx++);
            Cell footerLabel = footerRow.createCell(0);
            footerLabel.setCellValue("Total Count");
            footerLabel.setCellStyle(footerStyle);
            for (int m = 0; m < 12; m++) {
                Cell cell = footerRow.createCell(m + 1);
                cell.setCellValue(otRoomMonthlyTotals.getOrDefault(m, 0L));
                cell.setCellStyle(footerStyle);
            }
            Cell grandTotalCell = footerRow.createCell(13);
            grandTotalCell.setCellValue(otRoomGrandTotal);
            grandTotalCell.setCellStyle(footerStyle);

            // Auto-size columns
            for (int c = 0; c <= 13; c++) {
                sheet.autoSizeColumn(c);
            }

            // Append Chart Image if available
            String b64Image = null;
            if ("bar".equals(otRoomChartType) && otRoomBarChartImage != null && otRoomBarChartImage.startsWith("data:image/png;base64,")) {
                b64Image = otRoomBarChartImage.substring("data:image/png;base64,".length());
            } else if ("line".equals(otRoomChartType) && otRoomLineChartImage != null && otRoomLineChartImage.startsWith("data:image/png;base64,")) {
                b64Image = otRoomLineChartImage.substring("data:image/png;base64,".length());
            }

            if (b64Image != null) {
                try {
                    byte[] imgBytes = java.util.Base64.getDecoder().decode(b64Image);
                    int picIdx = workbook.addPicture(imgBytes, Workbook.PICTURE_TYPE_PNG);
                    CreationHelper helper = workbook.getCreationHelper();
                    Drawing drawing = sheet.createDrawingPatriarch();
                    ClientAnchor anchor = helper.createClientAnchor();
                    anchor.setCol1(0);
                    anchor.setRow1(rowIdx + 2);
                    anchor.setCol2(10);
                    anchor.setRow2(rowIdx + 22);
                    drawing.createPicture(anchor, picIdx);
                } catch (Exception ex) {
                    System.err.println("Failed to embed chart image in Excel: " + ex.getMessage());
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            streamToResponse(bos.toByteArray(),
                    "OT_Room_Surgery_Count_" + getOtRoomSelectedYear() + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to generate Excel report: " + e.getMessage());
        }
    }

    // ── PDF export ───────────────────────────────────────────────────────
    public void downloadOtRoomWisePdf() {
        if (otRoomWiseList == null || otRoomWiseList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please run the report first.");
            return;
        }

        try {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, bos);
            document.open();

            Font pdfTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font pdfHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font pdfCellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font pdfFooterFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            Paragraph title = new Paragraph(
                    "OT Room Wise Surgery Count - " + getOtRoomSelectedYear(),
                    pdfTitleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // 14 columns: Room Name + Jan-Dec + Total
            PdfPTable table = new PdfPTable(14);
            table.setWidthPercentage(100);
            float[] widths = new float[14];
            widths[0] = 14; // room name
            for (int i = 1; i <= 12; i++) {
                widths[i] = 6;
            }
            widths[13] = 7; // total
            table.setWidths(widths);

            // Year header row
            PdfPCell emptyCell = new PdfPCell(new Phrase("", pdfHeaderFont));
            emptyCell.setBackgroundColor(new java.awt.Color(220, 220, 220));
            emptyCell.setPadding(4);
            table.addCell(emptyCell);

            PdfPCell yearCell = new PdfPCell(new Phrase(String.valueOf(getOtRoomSelectedYear()), pdfHeaderFont));
            yearCell.setColspan(13);
            yearCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            yearCell.setBackgroundColor(new java.awt.Color(220, 220, 220));
            yearCell.setPadding(4);
            table.addCell(yearCell);

            // Month header row
            addCell(table, "OT Room", pdfHeaderFont, new java.awt.Color(220, 220, 220));
            for (String month : MONTH_SHORT_LABELS) {
                addCell(table, month, pdfHeaderFont, new java.awt.Color(220, 220, 220));
            }
            addCell(table, "Total", pdfHeaderFont, new java.awt.Color(220, 220, 220));

            // Data rows
            for (OtRoomWiseSurgeryCountDTO dto : otRoomWiseList) {
                addCell(table, nullSafe(dto.getRoomName()), pdfCellFont);
                for (int m = 0; m < 12; m++) {
                    addCell(table, String.valueOf(dto.getCount(m)), pdfCellFont);
                }
                addCell(table, String.valueOf(dto.getTotalCount()), pdfFooterFont);
            }

            // Footer row
            addCell(table, "Total Count", pdfFooterFont, new java.awt.Color(230, 230, 230));
            for (int m = 0; m < 12; m++) {
                addCell(table, String.valueOf(otRoomMonthlyTotals.getOrDefault(m, 0L)),
                        pdfFooterFont, new java.awt.Color(230, 230, 230));
            }
            addCell(table, String.valueOf(otRoomGrandTotal),
                    pdfFooterFont, new java.awt.Color(230, 230, 230));

            document.add(table);

            // Append Chart Image if available
            String b64Image = null;
            if ("bar".equals(otRoomChartType) && otRoomBarChartImage != null && otRoomBarChartImage.startsWith("data:image/png;base64,")) {
                b64Image = otRoomBarChartImage.substring("data:image/png;base64,".length());
            } else if ("line".equals(otRoomChartType) && otRoomLineChartImage != null && otRoomLineChartImage.startsWith("data:image/png;base64,")) {
                b64Image = otRoomLineChartImage.substring("data:image/png;base64,".length());
            }

            if (b64Image != null) {
                try {
                    byte[] imgBytes = java.util.Base64.getDecoder().decode(b64Image);
                    com.lowagie.text.Image chartImg = com.lowagie.text.Image.getInstance(imgBytes);
                    chartImg.scaleToFit(PageSize.A4.rotate().getWidth() - 40, 300);
                    chartImg.setAlignment(Element.ALIGN_CENTER);
                    chartImg.setSpacingBefore(20);
                    document.add(chartImg);
                } catch (Exception ex) {
                    System.err.println("Failed to embed chart image in PDF: " + ex.getMessage());
                }
            }

            document.close();

            streamToResponse(bos.toByteArray(),
                    "OT_Room_Surgery_Count_" + getOtRoomSelectedYear() + ".pdf",
                    "application/pdf");

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to generate PDF report: " + e.getMessage());
        }
    }

    private void addCell(PdfPTable table, String value, Font font, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setPadding(3);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        table.addCell(cell);
    }

    public List<SurgeryReportDTO> getReportList() {
        return reportList;
    }

    public void setReportList(List<SurgeryReportDTO> reportList) {
        this.reportList = reportList;
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.core.util.CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.core.util.CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public com.divudi.core.entity.Item getProcedure() {
        return procedure;
    }

    public void setProcedure(Item procedure) {
        this.procedure = procedure;
    }

    public RoomFacilityCharge getOperationTheatreRoom() {
        return operationTheatreRoom;
    }

    public void setOperationTheatreRoom(RoomFacilityCharge operationTheatreRoom) {
        this.operationTheatreRoom = operationTheatreRoom;
    }

    // ── OT Room Wise getters/setters ─────────────────────────────────────
    public Date getFromYearDate() {
        if (fromYearDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            fromYearDate = cal.getTime();
        }
        return fromYearDate;
    }

    public void setFromYearDate(Date fromYearDate) {
        this.fromYearDate = fromYearDate;
    }

    public Date getToYearDate() {
        if (toYearDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            toYearDate = cal.getTime();
        }
        return toYearDate;
    }

    public void setToYearDate(Date toYearDate) {
        this.toYearDate = toYearDate;
    }

    public String getOtRoomChartType() {
        return otRoomChartType;
    }

    public void setOtRoomChartType(String otRoomChartType) {
        this.otRoomChartType = otRoomChartType;
    }

    public List<OtRoomWiseSurgeryCountDTO> getOtRoomWiseList() {
        return otRoomWiseList;
    }

    public void setOtRoomWiseList(List<OtRoomWiseSurgeryCountDTO> otRoomWiseList) {
        this.otRoomWiseList = otRoomWiseList;
    }

    public Map<Integer, Long> getOtRoomMonthlyTotals() {
        return otRoomMonthlyTotals;
    }

    public void setOtRoomMonthlyTotals(Map<Integer, Long> otRoomMonthlyTotals) {
        this.otRoomMonthlyTotals = otRoomMonthlyTotals;
    }

    public long getOtRoomGrandTotal() {
        return otRoomGrandTotal;
    }

    public void setOtRoomGrandTotal(long otRoomGrandTotal) {
        this.otRoomGrandTotal = otRoomGrandTotal;
    }

    public String getOtRoomBarChartModel() {
        return otRoomBarChartModel;
    }

    public void setOtRoomBarChartModel(String otRoomBarChartModel) {
        this.otRoomBarChartModel = otRoomBarChartModel;
    }

    public String getOtRoomLineChartModel() {
        return otRoomLineChartModel;
    }

    public void setOtRoomLineChartModel(String otRoomLineChartModel) {
        this.otRoomLineChartModel = otRoomLineChartModel;
    }

    public long getOtRoomMonthlyTotal(int monthIndex) {
        if (otRoomMonthlyTotals == null) {
            return 0;
        }
        return otRoomMonthlyTotals.getOrDefault(monthIndex, 0L);
    }

    public String getOtRoomBarChartImage() {
        return otRoomBarChartImage;
    }

    public void setOtRoomBarChartImage(String otRoomBarChartImage) {
        this.otRoomBarChartImage = otRoomBarChartImage;
    }

    public String getOtRoomLineChartImage() {
        return otRoomLineChartImage;
    }

    public void setOtRoomLineChartImage(String otRoomLineChartImage) {
        this.otRoomLineChartImage = otRoomLineChartImage;
    }

    public int getSelectedYear() {

        Calendar cal = Calendar.getInstance();
        if (fromYearDate != null) {
            cal.setTime(fromYearDate);
        }
        return cal.get(Calendar.YEAR);
    }

    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
    }

}
