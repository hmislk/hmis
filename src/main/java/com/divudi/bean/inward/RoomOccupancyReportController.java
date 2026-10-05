package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.data.dto.RoomOccupancyReportDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.util.stream.Collectors;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Named
@SessionScoped
public class RoomOccupancyReportController implements Serializable {

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private DepartmentFacade departmentFacade;

    @Inject
    private SessionController sessionController;

    private Date fromDate;
    private Date toDate;

    private PatientEncounterDto selectedPatient;
    private Department ward;
    private RoomFacilityCharge roomFacilityCharge;
    private AdmissionType admissionType;
    private RoomCategory roomCategory;
    private Institution institution;
    private Institution site;

    private String reportType = "Detail"; // "Detail", "Summary By Room Category", "Summary By Admission Type"
    private String summaryType = "Category"; // Internal toggle for which summary to render

    private List<RoomOccupancyReportDto> detailDtos;
    private List<RoomOccupancyReportDto> summaryDtos;

    public RoomOccupancyReportController() {
    }

    public void processReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both discharge dates.");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("Discharge from date must not be after discharge to date.");
            return;
        }
        if ("Detail".equals(reportType)) {
            processDetailReport();
        } else {
            processSummaryReport();
        }
    }

    private void processDetailReport() {
        detailDtos = new ArrayList<>();
        summaryDtos = new ArrayList<>();

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.RoomOccupancyReportDto( ")
                .append("     pat.phn, ")
                .append("     per.name, ")
                .append("     adm.dateOfAdmission, ")
                .append("     adm.dateOfDischarge, ")
                .append("     rc.name, ")
                .append("     at.name, ")
                .append("     dept.name, ")
                .append("     r.name, ")
                .append("     pr.admittedAt, ")
                .append("     pr.dischargedAt ")
                .append(" ) ")
                .append(" from PatientRoom pr ")
                .append(" left join pr.patientEncounter adm ")
                .append(" left join adm.patient pat ")
                .append(" left join pat.person per ")
                .append(" left join pr.roomFacilityCharge rfc ")
                .append(" left join rfc.room r ")
                .append(" left join rfc.roomCategory rc ")
                .append(" left join rfc.department dept ")
                .append(" left join dept.institution inst ")
                .append(" left join adm.admissionType at ")
                .append(" where pr.retired = false ")
                .append(" and pr.discharged = true ");

        Map<String, Object> params = new HashMap<>();

        jpql.append(" and (pr.dischargedAt between :fd and :td) ");
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (selectedPatient != null && selectedPatient.getPatientEncounter() != null) {
            jpql.append(" and pat = :pt ");
            params.put("pt", selectedPatient.getPatientEncounter().getPatient());
        }

        if (ward != null) {
            jpql.append(" and dept = :wd ");
            params.put("wd", ward);
        }

        if (roomFacilityCharge != null) {
            jpql.append(" and rfc = :rfc ");
            params.put("rfc", roomFacilityCharge);
        }

        if (admissionType != null) {
            jpql.append(" and at = :adt ");
            params.put("adt", admissionType);
        }

        if (roomCategory != null) {
            jpql.append(" and rc = :rc ");
            params.put("rc", roomCategory);
        }

        if (site != null) {
            jpql.append(" and dept.institution = :st ");
            params.put("st", site);
        } else if (institution != null) {
            jpql.append(" and dept.institution = :ins ");
            params.put("ins", institution);
        }

        jpql.append(" order by pr.admittedAt desc ");
        detailDtos = (List<RoomOccupancyReportDto>) patientRoomFacade
                .findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        System.out.println("This is msg " + detailDtos.size());

    }

    private void processSummaryReport() {
        processDetailReport(); // Fetch all raw data first

        summaryDtos = new ArrayList<>();

        if (detailDtos == null || detailDtos.isEmpty()) {
            return;
        }

        if ("Summary By Room Category".equals(reportType)) {
            summaryType = "Category";
            Map<String, Double> map = detailDtos.stream()
                    .filter(dto -> dto.getRoomCategory() != null)
                    .collect(Collectors.groupingBy(RoomOccupancyReportDto::getRoomCategory, Collectors.summingDouble(RoomOccupancyReportDto::getOccupancyDays)));

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                RoomOccupancyReportDto dto = new RoomOccupancyReportDto();
                dto.setGroupName(entry.getKey());
                dto.setTotalOccupancyDays(entry.getValue());
                summaryDtos.add(dto);
            }
        } else if ("Summary By Admission Type".equals(reportType)) {
            summaryType = "AdmissionType";
            Map<String, Double> map = detailDtos.stream()
                    .filter(dto -> dto.getAdmissionType() != null)
                    .collect(Collectors.groupingBy(RoomOccupancyReportDto::getAdmissionType, Collectors.summingDouble(RoomOccupancyReportDto::getOccupancyDays)));

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                RoomOccupancyReportDto dto = new RoomOccupancyReportDto();
                dto.setGroupName(entry.getKey());
                dto.setTotalOccupancyDays(entry.getValue());
                summaryDtos.add(dto);
            }
        }

        // Sort
        summaryDtos.sort((d1, d2) -> d1.getGroupName().compareToIgnoreCase(d2.getGroupName()));
    }

    private static final SimpleDateFormat HEADER_DATE_FMT = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

    public void preProcessDetailPdf(Object document) throws DocumentException {
        Document pdf = (Document) document;
        pdf.setPageSize(PageSize.A4.rotate());
        pdf.setMargins(20f, 20f, 20f, 20f);
        pdf.open();
        addReportHeader(pdf, "Room Occupancy Report - Detail");
    }

    public void preProcessSummaryPdf(Object document) throws DocumentException {
        Document pdf = (Document) document;
        pdf.setPageSize(PageSize.A4);
        pdf.setMargins(30f, 30f, 30f, 30f);
        pdf.open();

        String title = "Summary By Admission Type".equals(reportType)
                ? "Room Occupancy Report - Summary By Admission Type"
                : "Room Occupancy Report - Summary By Room Category";
        addReportHeader(pdf, title);
    }

    private void addReportHeader(Document pdf, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
        // Darker, larger meta font than before (issue: filters were present but easy to miss in the PDF)
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(4f);
        pdf.add(titlePara);

        Paragraph metaPara = new Paragraph(buildFilterSummary(), metaFont);
        metaPara.setAlignment(Element.ALIGN_CENTER);
        metaPara.setSpacingAfter(12f);
        pdf.add(metaPara);
    }

    /**
     * The filter values currently applied to the report, keyed by label, in
     * display order. Shared by the PDF header, the Excel export header and the
     * on-screen/print "Applied Filters" line so the three stay consistent.
     */
    private Map<String, Object> collectActiveFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("Discharge From", fromDate != null ? HEADER_DATE_FMT.format(fromDate) : "N/A");
        filters.put("Discharge To", toDate != null ? HEADER_DATE_FMT.format(toDate) : "N/A");
        if (selectedPatient != null) {
            filters.put("Patient MRN", selectedPatient.getBhtNo());
        }
        if (ward != null) {
            filters.put("Ward", ward.getName());
        }
        if (roomFacilityCharge != null) {
            filters.put("Bed", roomFacilityCharge.getName());
        }
        if (roomCategory != null) {
            filters.put("Room Category", roomCategory.getName());
        }
        if (admissionType != null) {
            filters.put("Admission Type", admissionType.getName());
        }
        if (institution != null) {
            filters.put("Institution", institution.getName());
        }
        if (site != null) {
            filters.put("Site", site.getName());
        }
        return filters;
    }

    private String buildFilterSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Discharge Period: ")
                .append(fromDate != null ? HEADER_DATE_FMT.format(fromDate) : "N/A")
                .append(" - ")
                .append(toDate != null ? HEADER_DATE_FMT.format(toDate) : "N/A");

        if (selectedPatient != null) {
            sb.append("  |  Patient MRN: ").append(selectedPatient.getBhtNo());
        }
        if (ward != null) {
            sb.append("  |  Ward: ").append(ward.getName());
        }
        if (roomFacilityCharge != null) {
            sb.append("  |  Bed: ").append(roomFacilityCharge.getName());
        }
        if (roomCategory != null) {
            sb.append("  |  Room Category: ").append(roomCategory.getName());
        }
        if (admissionType != null) {
            sb.append("  |  Admission Type: ").append(admissionType.getName());
        }
        if (institution != null) {
            sb.append("  |  Institution: ").append(institution.getName());
        }
        if (site != null) {
            sb.append("  |  Site: ").append(site.getName());
        }
        sb.append("\nGenerated: ").append(HEADER_DATE_FMT.format(new Date()));
        return sb.toString();
    }

    /**
     * Plain-text "Applied Filters" line for the on-screen/print view (Issue:
     * downloaded PDF/Excel showed the applied filters but the printed page did
     * not carry any indication of what was filtered).
     */
    public String getAppliedFiltersSummary() {
        Map<String, Object> filters = collectActiveFilters();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (!first) {
                sb.append("   |   ");
            }
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    // PostProcessor for the Detail Excel export: inserts the applied filters
    // above the exported rows (Issue: downloaded Excel had no indication of
    // which filters produced the data).
    public void postProcessDetailExcel(Object document) {
        insertExcelFilterSummary(document, "Room Occupancy Report - Detail");
    }

    // PostProcessor for the Summary Excel export (same purpose as above).
    public void postProcessSummaryExcel(Object document) {
        String title = "Summary By Admission Type".equals(reportType)
                ? "Room Occupancy Report - Summary By Admission Type"
                : "Room Occupancy Report - Summary By Room Category";
        insertExcelFilterSummary(document, title);
    }

    private void insertExcelFilterSummary(Object document, String title) {
        if (!(document instanceof XSSFWorkbook)) {
            return;
        }
        XSSFWorkbook workbook = (XSSFWorkbook) document;
        XSSFSheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            return;
        }

        Map<String, Object> filters = collectActiveFilters();
        String institutionName = sessionController != null && sessionController.getInstitution() != null
                ? sessionController.getInstitution().getName() : null;

        int filterRows = Math.max(1, (int) Math.ceil(filters.size() / 3.0));
        int rowsNeeded = (institutionName != null ? 1 : 0) + 1 + filterRows + 1;

        int lastRowNum = sheet.getLastRowNum();
        if (sheet.getPhysicalNumberOfRows() > 0) {
            sheet.shiftRows(0, lastRowNum, rowsNeeded);
        }

        org.apache.poi.ss.usermodel.Font instFont = workbook.createFont();
        instFont.setBold(true);
        instFont.setFontHeightInPoints((short) 14);
        CellStyle instStyle = workbook.createCellStyle();
        instStyle.setFont(instFont);
        instStyle.setAlignment(HorizontalAlignment.CENTER);

        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        org.apache.poi.ss.usermodel.Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        CellStyle labelStyle = workbook.createCellStyle();
        labelStyle.setFont(labelFont);

        int lastCol = 7;
        int rowIndex = 0;

        if (institutionName != null) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastCol));
            Row instRow = sheet.createRow(rowIndex++);
            Cell instCell = instRow.createCell(0);
            instCell.setCellValue(institutionName);
            instCell.setCellStyle(instStyle);
        }

        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastCol));
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);

        int pairCounter = 0;
        Row row = sheet.createRow(rowIndex++);
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            Cell labelCell = row.createCell(pairCounter * 3);
            labelCell.setCellValue(entry.getKey() + ":");
            labelCell.setCellStyle(labelStyle);

            Cell valueCell = row.createCell(pairCounter * 3 + 1);
            valueCell.setCellValue(String.valueOf(entry.getValue()));

            pairCounter++;
            if (pairCounter == 3) {
                pairCounter = 0;
                row = sheet.createRow(rowIndex++);
            }
        }
    }

    /**
     * Ward autocomplete: the departments that actually have rooms/beds
     * (RoomFacilityCharge.department), which is exactly what the Ward filter
     * matches on. The shared Inward-type department list offered departments
     * that no room belongs to, so choosing one returned no data.
     */
    public List<Department> completeWard(String qry) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select distinct d from RoomFacilityCharge rfc "
                + " join rfc.department d "
                + " where rfc.retired = false "
                + " and d.retired = false "
                + " and upper(d.name) like :q "
                + " order by d.name";
        params.put("q", "%" + (qry == null ? "" : qry.toUpperCase()) + "%");
        return departmentFacade.findByJpql(jpql, params);
    }

    // Both discharge calendars default to the same day (start / end of today).
    // Left null, each picker filled in the current clock time on its own, so
    // Discharge From and Discharge To came out as different moments.
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

    public PatientEncounterDto getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatient(PatientEncounterDto selectedPatient) {
        this.selectedPatient = selectedPatient;
    }

    public Department getWard() {
        return ward;
    }

    public void setWard(Department ward) {
        this.ward = ward;
    }

    public RoomFacilityCharge getRoomFacilityCharge() {
        return roomFacilityCharge;
    }

    public void setRoomFacilityCharge(RoomFacilityCharge roomFacilityCharge) {
        this.roomFacilityCharge = roomFacilityCharge;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
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

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public List<RoomOccupancyReportDto> getDetailDtos() {
        return detailDtos;
    }

    public void setDetailDtos(List<RoomOccupancyReportDto> detailDtos) {
        this.detailDtos = detailDtos;
    }

    public List<RoomOccupancyReportDto> getSummaryDtos() {
        return summaryDtos;
    }

    public void setSummaryDtos(List<RoomOccupancyReportDto> summaryDtos) {
        this.summaryDtos = summaryDtos;
    }
}
