package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.SurgeryReportDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.SurgeryReportDTO( ")
                .append("   b.id, pat.phn, pp.name, pe.dateOfAdmission, i.name, ")
                .append("   d.name, rfc.name, stp.name, rcp.name, pe.id ")
                .append(" ) ")
                .append(" from Bill b ")
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
            jpql.append(" and b.institution = :inst ");
            params.put("inst", institution);
        }
        if (department != null) {
            jpql.append(" and d = :dept ");
            params.put("dept", department);
        }
        if (site != null) {
            jpql.append(" and d.site = :site ");
            params.put("site", site);
        }
        if (procedure != null) {
            jpql.append(" and i = :proc ");
            params.put("proc", procedure);
        }
        if (operationTheatreRoom != null) {
            jpql.append(" and cpr.roomFacilityCharge = :otRoom ");
            params.put("otRoom", operationTheatreRoom);
        }

        jpql.append(" order by i.name ");

        reportList = (List<SurgeryReportDTO>) billFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP, 1000);

        attachOtStatuses(reportList);
    }

    private void attachOtStatuses(List<SurgeryReportDTO> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> encounterIds = new ArrayList<>();
        for (SurgeryReportDTO r : rows) {
            encounterIds.add(r.getPatientEncounterId());
        }

        String jpql = " select str.admission.id, str.theatreOccupancyStatus "
                + " from PatientTransferRequest str "
                + " where str.retired = false "
                + " and str.admission.id in :ids "
                + " order by str.createdAt ASC";
        Map<String, Object> p = new HashMap<>();
        p.put("ids", encounterIds);

        List<Object[]> statusRows = billFacade.findAggregates(jpql, p, TemporalType.TIMESTAMP);

        Map<Long, String> statusByEncounter = new HashMap<>();
        for (Object[] row : statusRows) {
            Long encounterId = (Long) row[0];
            Object statusObj = row[1];
            String status = statusObj != null ? statusObj.toString() : "";
            statusByEncounter.put(encounterId, status);
        }

        for (SurgeryReportDTO r : rows) {
            r.setOtStatus(statusByEncounter.get(r.getPatientEncounterId()));
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
            titleCell.setCellValue("Surgery Status Report  (" + sdf.format(fromDate) + " - " + sdf.format(toDate) + ")");
            titleCell.setCellStyle(titleStyle);
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
            Paragraph title = new Paragraph(
                    "Surgery Status Report  (" + sdf.format(fromDate) + " - " + sdf.format(toDate) + ")",
                    titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

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

}
