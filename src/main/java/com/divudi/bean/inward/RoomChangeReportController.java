package com.divudi.bean.inward;

import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.data.dto.RoomChangeReportDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.PatientTransferRequestFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@SessionScoped
public class RoomChangeReportController implements Serializable {

    public RoomChangeReportController() {
    }

    @EJB
    private PatientTransferRequestFacade patientTransferRequestFacade;

    private Institution institution;
    private Institution site;
    private Department department;

    private RoomFacilityCharge fromWard;
    private RoomFacilityCharge toWard;

    private Date fromDate;
    private Date toDate;

    private PatientEncounterDto selectedPatient;
    private PatientEncounterDto selectedBhtNo;

    private List<RoomChangeReportDto> roomChangeReportDtoList;

    public void createRoomChangeReport() {
        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.RoomChangeReportDto( ")
                .append("     r.id, ")
                .append("     r.status, ")
                .append("     pat.phn, ")
                .append("     per.name, ")
                .append("     per.dob, ")
                .append("     per.sex, ")
                .append("     adm.bhtNo, ")
                .append("     adm.dateOfAdmission, ")
                .append("     fromInst.name, ")
                .append("     fromDept.name, ")
                .append("     rcPerson.name, ")
                .append("     fromRoomEntity.name, ")
                .append("     fromRfc.roomCategory.name, ")
                .append("     toInst.name, ")
                .append("     toDept.name, ")
                .append("     rcPerson.name, ")
                .append("     toRoomEntity.name, ")
                .append("     toRfc.roomCategory.name, ")
                .append("     r.acceptedAt, ")
                .append("     abPerson.name, ")
                .append("     r.notes ")
                .append(" ) ")
                .append(" from PatientTransferRequest r ")
                .append(" left join r.admission adm ")
                .append(" left join adm.patient pat ")
                .append(" left join pat.person per ")
                .append(" left join adm.referringConsultant rc ")
                .append(" left join rc.person rcPerson ")
                .append(" left join r.fromPatientRoom fromRoom ")
                .append(" left join fromRoom.roomFacilityCharge fromRfc ")
                .append(" left join fromRfc.room fromRoomEntity ")
                .append(" left join fromRfc.department fromDept ")
                .append(" left join fromDept.institution fromInst ")
                .append(" left join r.toRoomFacilityCharge toRfc ")
                .append(" left join toRfc.room toRoomEntity ")
                .append(" left join toRfc.department toDept ")
                .append(" left join toDept.institution toInst ")
                .append(" left join r.acceptedBy ab ")
                .append(" left join ab.webUserPerson abPerson ")
                .append(" where r.retired = false ")
                .append(" and r.createdAt between :fd and :td ");

        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (selectedPatient != null && selectedPatient.getPatientEncounter() != null) {
            jpql.append(" and pat = :pt ");
            params.put("pt", selectedPatient.getPatientEncounter().getPatient());
        }

        if (selectedBhtNo != null && selectedBhtNo.getPatientEncounter() != null) {
            jpql.append(" and adm = :pe ");
            params.put("pe", selectedBhtNo.getPatientEncounter());
        }

        if (fromWard != null) {
            jpql.append(" and fromRfc = :fw ");
            params.put("fw", fromWard);
        }

        if (toWard != null) {
            jpql.append(" and toRfc = :tw ");
            params.put("tw", toWard);
        }

        jpql.append(" order by r.createdAt desc ");

        roomChangeReportDtoList = (List<RoomChangeReportDto>) patientTransferRequestFacade
                .findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    public void downloadRoomChangeReportPdf() {
        if (roomChangeReportDtoList == null || roomChangeReportDtoList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A3.rotate(), 20, 20, 30, 20);
            PdfWriter.getInstance(document, baos);
            document.open();

            addReportHeader(document);
            addReportTable(document);

            document.close();

            streamPdfToBrowser(baos.toByteArray(), "Room_Change_Report.pdf");

        } catch (DocumentException e) {
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Failed to stream PDF: " + e.getMessage());
        }
    }

    private void addReportHeader(Document document) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font metaFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

        Paragraph title = new Paragraph("ROOM CHANGE REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        StringBuilder meta = new StringBuilder();
        meta.append("From Date: ").append(fromDate != null ? sdf.format(fromDate) : "-");
        meta.append("   |   To Date: ").append(toDate != null ? sdf.format(toDate) : "-");
        if (fromWard != null) {
            meta.append("   |   From Ward: ").append(fromWard.getName());
        }
        if (toWard != null) {
            meta.append("   |   To Ward: ").append(toWard.getName());
        }
        if (selectedPatient != null) {
            meta.append("   |   Patient: ").append(selectedPatient.getPatientName());
        }
        if (selectedBhtNo != null) {
            meta.append("   |   Visit No: ").append(selectedBhtNo.getBhtNo());
        }

        Paragraph metaPara = new Paragraph(meta.toString(), metaFont);
        metaPara.setAlignment(Element.ALIGN_CENTER);
        metaPara.setSpacingAfter(10f);
        document.add(metaPara);
    }

    private void addReportTable(Document document) throws DocumentException {
        int columnCount = 28;
        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        float[] widths = {
            3, 5, 8, 3, 3, 4, 5, 4, 4, 6, // S.No..Transfer Status
            6, 6, 6, 6, 5, 4, 5, // From: Hospital..Bed Type
            6, 6, 6, 6, 5, 4, 5, // To: Hospital..Bed Type
            5, 4, 6, 8 // Transfer Date..Reason
        };
        table.setWidths(widths);

        Font headerFont = new Font(Font.HELVETICA, 7, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 7, Font.NORMAL);
        Color headerBg = new Color(60, 60, 60);

        String[] headers = {
            "S.No", "MRNO", "Patient Name", "Age", "Gender", "Visit",
            "Admission Date", "Admission Time", "Transfer Req. No.", "Transfer Status",
            "From Hospital", "From Department", "From Primary Consultant", "From Consultant",
            "From Ward", "From Bed No", "From Bed Type",
            "To Hospital", "To Department", "To Primary Consultant", "To Consultant",
            "To Ward", "To Bed No", "To Bed Type",
            "Transfer Date", "Transfer Time", "Transfer By", "Reason"
        };

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(3f);
            table.addCell(cell);
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("M/d/yyyy");
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
        int n = 1;

        for (RoomChangeReportDto detail : roomChangeReportDtoList) {
            addCell(table, String.valueOf(n++), cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getPatientPhn(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getPatientName(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getPatientAge() != null ? String.valueOf(detail.getPatientAge()) : "", cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getPatientSex(), cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getBhtNo(), cellFont, Element.ALIGN_CENTER);
            addCell(table, formatDate(detail.getDateOfAdmission(), dateFmt), cellFont, Element.ALIGN_CENTER);
            addCell(table, formatDate(detail.getDateOfAdmission(), timeFmt), cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getTransferRequestId() != null ? String.valueOf(detail.getTransferRequestId()) : "", cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getStatus() != null ? detail.getStatus().toString() : "", cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getFromHospital(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getFromDepartment(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getFromPrimaryConsultant(), cellFont, Element.ALIGN_LEFT);
            addCell(table, "", cellFont, Element.ALIGN_LEFT); // from consultant: no source field yet
            addCell(table, detail.getFromDepartment(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getFromWardRoomName(), cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getFromBedType(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getToHospital(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getToDepartment(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getToPrimaryConsultant(), cellFont, Element.ALIGN_LEFT);
            addCell(table, "", cellFont, Element.ALIGN_LEFT); // to consultant: no source field yet
            addCell(table, detail.getToDepartment(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getToWardRoomName(), cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getToBedType(), cellFont, Element.ALIGN_LEFT);
            addCell(table, formatDate(detail.getAcceptedAt(), dateFmt), cellFont, Element.ALIGN_CENTER);
            addCell(table, formatDate(detail.getAcceptedAt(), timeFmt), cellFont, Element.ALIGN_CENTER);
            addCell(table, detail.getAcceptedByName(), cellFont, Element.ALIGN_LEFT);
            addCell(table, detail.getNotes(), cellFont, Element.ALIGN_LEFT);
        }

        document.add(table);
    }

    private void addCell(PdfPTable table, String value, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(2f);
        table.addCell(cell);
    }

    private String formatDate(Date date, SimpleDateFormat fmt) {
        return date != null ? fmt.format(date) : "";
    }

    private void streamPdfToBrowser(byte[] pdfBytes, String fileName) throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        ec.responseReset();
        ec.setResponseContentType("application/pdf");
        ec.setResponseContentLength(pdfBytes.length);
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        OutputStream out = ec.getResponseOutputStream();
        out.write(pdfBytes);
        out.flush();

        fc.responseComplete();
    }

    public void downloadRoomChangeReportExcel() {
        if (roomChangeReportDtoList == null || roomChangeReportDtoList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please run the report first.");
            return;
        }

        String[] headers = {
            "S.No", "MRNO", "Patient Name", "Age", "Gender", "Visit",
            "Admission Date", "Admission Time",
            "Transfer Request No.", "Transfer Request Status",
            "From Hospital", "From Department", "From Primary Consultant", "From Consultant",
            "From Ward", "From Bed No", "From Bed Type",
            "To Hospital", "To Department", "To Primary Consultant", "To Consultant",
            "To Ward", "To Bed No", "To Bed Type",
            "Transfer Date", "Transfer Time", "Transfer By", "Reason"
        };

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Room Change Report");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook, "M/d/yyyy");
            CellStyle timeStyle = createDateStyle(workbook, "HH:mm");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            int sNo = 1;
            for (RoomChangeReportDto d : roomChangeReportDtoList) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;

                row.createCell(col++).setCellValue(sNo++);
                row.createCell(col++).setCellValue(nz(d.getPatientPhn()));
                row.createCell(col++).setCellValue(nz(d.getPatientName()));
                row.createCell(col++).setCellValue(d.getPatientAge() != null ? d.getPatientAge() : 0);
                row.createCell(col++).setCellValue(nz(d.getPatientSex()));
                row.createCell(col++).setCellValue(nz(d.getBhtNo()));

                col = writeDateTimePair(row, col, d.getDateOfAdmission(), dateStyle, timeStyle);

                row.createCell(col++).setCellValue(d.getTransferRequestId() != null ? d.getTransferRequestId() : 0);
                row.createCell(col++).setCellValue(d.getStatus() != null ? d.getStatus().toString() : "");

                row.createCell(col++).setCellValue(nz(d.getFromHospital()));
                row.createCell(col++).setCellValue(nz(d.getFromDepartment()));
                row.createCell(col++).setCellValue(nz(d.getFromPrimaryConsultant()));
                row.createCell(col++).setCellValue(""); // From Consultant - no source field on DTO yet
                row.createCell(col++).setCellValue(nz(d.getFromWardRoomName()));
                row.createCell(col++).setCellValue(""); // From Bed No - no source field on DTO yet
                row.createCell(col++).setCellValue(nz(d.getFromBedType()));

                row.createCell(col++).setCellValue(nz(d.getToHospital()));
                row.createCell(col++).setCellValue(nz(d.getToDepartment()));
                row.createCell(col++).setCellValue(nz(d.getToPrimaryConsultant()));
                row.createCell(col++).setCellValue(""); // To Consultant
                row.createCell(col++).setCellValue(nz(d.getToWardRoomName()));
                row.createCell(col++).setCellValue(""); // To Bed No
                row.createCell(col++).setCellValue(nz(d.getToBedType()));

                col = writeDateTimePair(row, col, d.getAcceptedAt(), dateStyle, timeStyle);

                row.createCell(col++).setCellValue(nz(d.getAcceptedByName()));
                row.createCell(col++).setCellValue(nz(d.getNotes()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            FacesContext fc = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"Room_Change_Detail_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx\"");

            try (ServletOutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }

            fc.responseComplete();

        } catch (IOException e) {
            JsfUtil.addErrorMessage("Error generating Excel file: " + e.getMessage());
            Logger.getLogger(RoomChangeReportController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private int writeDateTimePair(Row row, int col, Date value, CellStyle dateStyle, CellStyle timeStyle) {
        Cell dateCell = row.createCell(col++);
        Cell timeCell = row.createCell(col++);
        if (value != null) {
            dateCell.setCellValue(value);
            dateCell.setCellStyle(dateStyle);
            timeCell.setCellValue(value);
            timeCell.setCellStyle(timeStyle);
        }
        return col;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    org.apache.poi.ss.usermodel.Font font = workbook.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
}

    private CellStyle createDateStyle(Workbook workbook, String pattern) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(pattern));
        return style;
    }

    private String nz(String value) {
        return value != null ? value : "";
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

    public RoomFacilityCharge getFromWard() {
        return fromWard;
    }

    public void setFromWard(RoomFacilityCharge fromWard) {
        this.fromWard = fromWard;
    }

    public RoomFacilityCharge getToWard() {
        return toWard;
    }

    public void setToWard(RoomFacilityCharge toWard) {
        this.toWard = toWard;
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

    public PatientEncounterDto getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatient(PatientEncounterDto selectedPatient) {
        this.selectedPatient = selectedPatient;
    }

    public PatientEncounterDto getSelectedBhtNo() {
        return selectedBhtNo;
    }

    public void setSelectedBhtNo(PatientEncounterDto selectedBhtNo) {
        this.selectedBhtNo = selectedBhtNo;
    }

    public PatientTransferRequestFacade getPatientTransferRequestFacade() {
        return patientTransferRequestFacade;
    }

    public List<RoomChangeReportDto> getRoomChangeReportDtoList() {
        return roomChangeReportDtoList;
    }

    public void setRoomChangeReportDtoList(List<RoomChangeReportDto> roomChangeReportDtoList) {
        this.roomChangeReportDtoList = roomChangeReportDtoList;
    }
}
