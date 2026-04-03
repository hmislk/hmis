/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.EncounterType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.dto.InwardAdmissionDTO;
import com.divudi.core.data.dto.InwardAdmissionDemographicDataDTO;
import com.divudi.core.data.dto.InwardIncomeDoctorSpecialtyDTO;
import com.divudi.core.data.dto.MonthServiceCountDTO;
import com.divudi.core.data.dto.MonthlySurgeryCountDTO;
import com.divudi.core.data.dto.IpUnsettledInvoiceDTO;
import com.divudi.core.data.dto.PaymentTypeAdmissionDTO;
import com.divudi.core.data.dto.SurgeryCountDoctorWiseDTO;
import com.divudi.core.data.dto.SurgeryCountSurgeryWiseDTO;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.InwardChargeType;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.SurgeryType;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.line.LineChartModel;

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.io.OutputStream;
import javax.faces.context.ExternalContext;

import javax.faces.context.FacesContext;
import javax.persistence.TemporalType;

/**
 *
 * @author pdhs
 */
@Named
@SessionScoped
public class InwardReportController implements Serializable {

    /**
     * Creates a new instance of InwardReportController
     */
    public InwardReportController() {
    }

    @EJB
    PatientEncounterFacade peFacade;
    @EJB
    AdmissionTypeFacade admissionTypeFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;

    @Inject
    SessionController sessionController;
    @Inject
    InwardReportControllerBht inwardReportControllerBht;
    @Inject
    BhtSummeryController bhtSummeryController;

    PaymentMethod paymentMethod;
    AdmissionType admissionType;
    Institution institution;
    Institution site;
    Department department;
    Date fromDate;
    Date toDate;
    private Date fromYearStartDate;
    private Date toYearEndDate;

    Admission patientEncounter;
    double grossTotals;
    double discounts;
    double netTotals;
    boolean withFooter;
    String invoceNo;
    String vatRegNo;
    Bill bill;
    private String patientCode;

    List<IncomeByCategoryRecord> incomeByCategoryRecords;
    List<IndividualBhtIncomeByCategoryRecord> individualBhtIncomeByCategoryRecord;
    List<AdmissionType> admissionty;
    private List<AdmissionType> admissionTypes;
    List<PatientEncounter> patientEncounters;
    List<BillItem> billItems;

    List<BillItem> billedBill;
    List<BillItem> cancelledBill;
    List<BillItem> refundBill;
    List<PatientInvestigation> patientInvestigations;
    double totalBilledBill;
    double totalCancelledBill;
    double totalRefundBill;

    // for disscharge book
    boolean dischargeDate = true;
    boolean bhtNo = true;
    boolean paymentMethord = true;
    boolean creditCompany = true;
    boolean person = true;
    boolean guardian = true;
    boolean room = true;
    boolean refDoctor = true;
    boolean AddmitDetails = true;
    boolean billedBy = true;
    boolean finalBillTotal = true;
    boolean paidByPatient = true;
    boolean creditPaidAmount = true;
    boolean dueAmount = true;
    boolean calculatedAmount = true;
    boolean differentAmount = true;
    boolean developers = false;
    // for disscharge book
    boolean withoutCancelBHT = true;
    private Speciality currentSpeciality;

    // Surgery Survey Report
    private String reportType;
    private SurgeryType surgeryType;
    private List<MonthlySurgeryCountDTO> monthlySurgeryCountList;
    private List<String> surgeryHeaders;

    private Date dischargeFromDate;
    private Date dischargeToDate;
    private Date invoiceApprovedFromDate;
    private Date invoiceApprovedToDate;
    private Department serviceCenter;
    private Institution sponsor;
    private String dischargeType;
    private String patientCategory;
    private AdmissionStatus admissionStatus;
    private RoomCategory roomCategory;
    private Staff consultant;
    private List<IpUnsettledInvoiceDTO> unsettledInvoicesList;
    private Item surgeryItem;

    // for specialty/doctor wise income
    private List<InwardIncomeDoctorSpecialtyDTO> spcDocIncomeBillList;
    private InwardIncomeDoctorSpecialtyDTO totalValuesSpcDocIncome;
    private Doctor currentDoctor;
    private boolean byDoctor;

    // for specialty/doctor wise demographic data 
    private List<InwardAdmissionDemographicDataDTO> demographicDataList;
    private boolean demographicDataUnknownGender = false;
    private boolean demographicGeneratedByDoctor;

    private ReportKeyWord reportKeyWord;

    private String surgeryWiseLineChartModel;
    private String surgeryWiseBarChartModel;

    private String specialtyLineChartImage;
    private String specialtyBarChartImage;
    private String doctorLineChartImage;
    private String doctorBarChartImage;

    private Date admissionReportProcessedAt;
    private String admissionReportProcessedBy;

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }
    double netTotal;
    double netPaid;

    public void fillAdmissionBook() {
        Date startTime = new Date();

        fillAdmissions(null, null);

    }

    public void fillAdmissionBookNew() {
        Date startTime = new Date();
        if (getReportKeyWord().getString().isEmpty() || getReportKeyWord().getString() == null) {
            JsfUtil.addErrorMessage("Select a Selection Methord");
            return;
        }
        if (getReportKeyWord().getString().equals("0")) {
            fillAdmissions(null, null);
        } else if (getReportKeyWord().getString().equals("1")) {
            fillAdmissions(false, false);
        } else if (getReportKeyWord().getString().equals("2")) {
            fillAdmissions(true, false);
        } else if (getReportKeyWord().getString().equals("3")) {
            fillAdmissions(true, true);
        }

    }

    public void fillAdmissionBookOnlyInward() {
        Date startTime = new Date();

        fillAdmissions(false, null);

    }

    public void fillAdmissionBookOnlyDischarged() {
        Date startTime = new Date();
        fillAdmissions(true, null);

    }

    public void fillAdmissionBookOnlyDischargedNotFinalized() {
        Date startTime = new Date();
        fillAdmissions(true, false);

    }

    public void fillAdmissionBookOnlyDischargedFinalized() {
        Date startTime = new Date();
        fillAdmissions(true, true);

    }

    public List<SurgeryCountDoctorWiseDTO> getExportableBillList() {
        if (billList == null) {
            return new ArrayList<>();
        }
        return billList.stream()
                .filter(dto -> !dto.isGrandTotal())
                .collect(java.util.stream.Collectors.toList());
    }

    private List<SurgeryCountDoctorWiseDTO> billList;

    public void processSurgeryCountDoctorWiseReport() {
        billList = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append(" Select new com.divudi.core.data.dto.SurgeryCountDoctorWiseDTO(")
                .append(" b.staff, ")
                .append(" b.staff.person.name, ")
                .append(" b.staff.speciality.name, ")
                .append(" b.createdAt")
                .append(") ")
                .append(" from BillFee b ")
                .append(" Where b.retired = false ")
                .append(" And b.bill.billTypeAtomic = :bta ")
                .append(" And (type(b.staff) = :doctorClass OR type(b.staff) = :consultantClass) ")
                .append(" AND b.createdAt BETWEEN :fromDate AND :toDate ");

        params.put("bta", BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
        params.put("fromDate", fromYearStartDate);
        params.put("toDate", toYearEndDate);
        params.put("doctorClass", Doctor.class);
        params.put("consultantClass", Consultant.class);

        if (currentSpeciality != null) {
            jpql.append(" AND b.staff.speciality = :spe ");
            params.put("spe", currentSpeciality);
        }

        jpql.append(" ORDER BY b.staff.speciality.name, b.staff.person.name ");

        List<SurgeryCountDoctorWiseDTO> rawList = (List<SurgeryCountDoctorWiseDTO>) billFeeFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        // Group by specialty and doctor, count surgeries month-wise
        Map<String, Map<Long, SurgeryCountDoctorWiseDTO>> specialtyDoctorMap = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();

        for (SurgeryCountDoctorWiseDTO dto : rawList) {
            String speciality = dto.getSpecialityName();
            Long staffId = dto.getStaff().getId();

            // Get or create specialty map
            Map<Long, SurgeryCountDoctorWiseDTO> doctorMap = specialtyDoctorMap.get(speciality);
            if (doctorMap == null) {
                doctorMap = new LinkedHashMap<>();
                specialtyDoctorMap.put(speciality, doctorMap);
            }

            // Get or create doctor aggregation
            SurgeryCountDoctorWiseDTO aggregated = doctorMap.get(staffId);
            if (aggregated == null) {
                aggregated = new SurgeryCountDoctorWiseDTO(
                        dto.getStaff(),
                        dto.getDoctorName(),
                        dto.getSpecialityName(),
                        null
                );
                doctorMap.put(staffId, aggregated);
            }

            // Increment month counter
            cal.setTime(dto.getCreatedAt());
            int month = cal.get(Calendar.MONTH);
            aggregated.addMonthCount(month, 1);
        }

        // Build final list with subtotals and grand total
        billList = new ArrayList<>();
        SurgeryCountDoctorWiseDTO grandTotal = new SurgeryCountDoctorWiseDTO();

        for (Map.Entry<String, Map<Long, SurgeryCountDoctorWiseDTO>> specialtyEntry : specialtyDoctorMap.entrySet()) {
            String speciality = specialtyEntry.getKey();
            Map<Long, SurgeryCountDoctorWiseDTO> doctorMap = specialtyEntry.getValue();

            SurgeryCountDoctorWiseDTO subtotal = new SurgeryCountDoctorWiseDTO(speciality);

            // Add all doctors for this specialty
            for (SurgeryCountDoctorWiseDTO doctor : doctorMap.values()) {
                billList.add(doctor);
                subtotal.addAllCounts(doctor);
                grandTotal.addAllCounts(doctor);
            }

            // Add subtotal row
            billList.add(subtotal);
        }

        // Add grand total row
        billList.add(grandTotal);

        createChartModels();
    }

    public void downloadSurgeryCountDoctorWisePdf() {
        if (billList == null || billList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        com.lowagie.text.Document document = null;
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");

            String fileName = "Surgery_Count_Doctor_Wise_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();

            document = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A3.rotate(), 20, 20, 30, 20);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // ── Derive year from fromYearStartDate ─────────────────────────────────
            // fromYearStartDate is bound in XHTML — extract year from it safely
            int reportYear = Calendar.getInstance().get(Calendar.YEAR); // fallback
            if (fromYearStartDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(fromYearStartDate);
                reportYear = cal.get(Calendar.YEAR);
            }

            // ── Fonts ──────────────────────────────────────────────────────────────
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
                    com.lowagie.text.Font.NORMAL, new java.awt.Color(255, 255, 255));
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            com.lowagie.text.Font subtotalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            com.lowagie.text.Font grandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.lowagie.text.Font totalColFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
                    com.lowagie.text.Font.NORMAL, new java.awt.Color(255, 255, 255));

            // ── Colors ─────────────────────────────────────────────────────────────
            java.awt.Color headerBg = new java.awt.Color(41, 128, 185);
            java.awt.Color subtotalBg = new java.awt.Color(213, 232, 255);
            java.awt.Color grandTotalBg = new java.awt.Color(255, 200, 100);
            java.awt.Color totalColBg = new java.awt.Color(255, 165, 0);
            java.awt.Color evenRowBg = new java.awt.Color(255, 255, 255);
            java.awt.Color oddRowBg = new java.awt.Color(248, 249, 250);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            // ── Title ──────────────────────────────────────────────────────────────
            Paragraph title = new Paragraph(
                    "Surgery Count Report - Doctor Wise", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph yearLine = new Paragraph("Year: " + reportYear, subFont);
            yearLine.setAlignment(Element.ALIGN_CENTER);
            yearLine.setSpacingAfter(10);
            document.add(yearLine);

            // ── Info Table (inline helper — no external method needed) ─────────────
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(45);
            infoTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            infoTable.setWidths(new float[]{1.5f, 3f});
            infoTable.setSpacingAfter(12);

            // Inline addInfoRow — avoids dependency on missing helper method
            String[][] infoRows = {
                {"From Date:", fromYearStartDate != null ? sdf.format(fromYearStartDate) : ""},
                {"To Date:", toYearEndDate != null ? sdf.format(toYearEndDate) : ""},
                {"Speciality:", currentSpeciality != null ? currentSpeciality.getName() : "All"},
                {"Generated:", sdf.format(new Date())}
            };
            for (String[] row : infoRows) {
                // Label cell
                PdfPCell labelCell = new PdfPCell(new Phrase(row[0], boldFont));
                labelCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                labelCell.setPadding(3);
                infoTable.addCell(labelCell);
                // Value cell
                PdfPCell valueCell = new PdfPCell(new Phrase(row[1], normalFont));
                valueCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                valueCell.setPadding(3);
                infoTable.addCell(valueCell);
            }
            document.add(infoTable);

            // ── Column Headers & Widths ────────────────────────────────────────────
            String[] headers = {
                "Doctor Name", "Speciality",
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                "Total"
            };
            float[] colWidths = {
                3.5f, 2.5f,
                1f, 1f, 1f, 1f, 1f, 1f,
                1f, 1f, 1f, 1f, 1f, 1f,
                1.3f
            };

            // ── Main Data Table ────────────────────────────────────────────────────
            PdfPTable table = new PdfPTable(15);
            table.setWidthPercentage(100);
            table.setWidths(colWidths);
            table.setSpacingBefore(5);
            table.setSpacingAfter(10);
            table.setHeaderRows(1);

            // Header Row
            for (int i = 0; i < headers.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(headers[i], headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setHorizontalAlignment(i <= 1 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4);
                table.addCell(cell);
            }

            // Data Rows
            int rowIndex = 0;
            for (SurgeryCountDoctorWiseDTO item : billList) {

                boolean isSubtotal = item.isSubtotal();
                boolean isGrandTotal = item.isGrandTotal();
                boolean isDataRow = !isSubtotal && !isGrandTotal;

                java.awt.Color rowBg = isGrandTotal ? grandTotalBg
                        : isSubtotal ? subtotalBg
                                : (rowIndex % 2 == 0) ? evenRowBg : oddRowBg;

                com.lowagie.text.Font rowFont
                        = (isSubtotal || isGrandTotal) ? subtotalFont : normalFont;

                // Col 0 – Doctor Name
                // Inline nullSafe — avoids dependency on missing helper
                String doctorName = item.getDoctorName() != null ? item.getDoctorName() : "";
                addSurgeryPdfCell(table, doctorName,
                        isGrandTotal ? grandFont : rowFont,
                        rowBg, Element.ALIGN_LEFT, isGrandTotal);

                // Col 1 – Speciality
                String speciality = (isDataRow && item.getSpecialityName() != null)
                        ? item.getSpecialityName() : "";
                addSurgeryPdfCell(table, speciality,
                        rowFont, rowBg, Element.ALIGN_LEFT, false);

                // Cols 2-13 – Month values
                int[] monthValues = {
                    item.getJanuary(), item.getFebruary(), item.getMarch(),
                    item.getApril(), item.getMay(), item.getJune(),
                    item.getJuly(), item.getAugust(), item.getSeptember(),
                    item.getOctober(), item.getNovember(), item.getDecember()
                };
                for (int mv : monthValues) {
                    addSurgeryPdfCell(table,
                            mv > 0 ? String.valueOf(mv) : "",
                            rowFont, rowBg, Element.ALIGN_CENTER, false);
                }

                // Col 14 – Total (orange background, white text always)
                PdfPCell totalCell = new PdfPCell(
                        new Phrase(String.valueOf(item.getTotalSurgeries()), totalColFont));
                totalCell.setBackgroundColor(totalColBg);
                totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                totalCell.setPadding(3);
                if (isGrandTotal) {
                    totalCell.setBorderWidthTop(2f);
                }
                table.addCell(totalCell);

                if (isDataRow) {
                    rowIndex++;
                }
            }

            document.add(table);

            // ── Footer ─────────────────────────────────────────────────────────────
            Paragraph footer = new Paragraph(
                    "Generated on: " + sdf.format(new Date()), normalFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(6);
            document.add(footer);

            document.close();
            facesContext.responseComplete();

        } catch (DocumentException | IOException e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

// ── Helper: styled cell for surgery PDF table ─────────────────────────────────
    private void addSurgeryPdfCell(PdfPTable table,
            String value,
            com.lowagie.text.Font font,
            java.awt.Color bg,
            int hAlign,
            boolean topBorder) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        if (bg != null) {
            cell.setBackgroundColor(bg);
        }
        cell.setHorizontalAlignment(hAlign);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        if (topBorder) {
            cell.setBorderWidthTop(2f);
        }
        table.addCell(cell);
    }

    public void downloadSurgeryCountDoctorWiseExcel() {
        if (billList == null || billList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Surgery Count Doctor Wise");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            // ── Derive report year ─────────────────────────────────────────────────
            int reportYear = Calendar.getInstance().get(Calendar.YEAR);
            if (fromYearStartDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(fromYearStartDate);
                reportYear = cal.get(Calendar.YEAR);
            }

            // ── Title style ────────────────────────────────────────────────────────
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // ── Info label style ───────────────────────────────────────────────────
            XSSFCellStyle infoLabelStyle = workbook.createCellStyle();
            XSSFFont infoLabelFont = workbook.createFont();
            infoLabelFont.setBold(true);
            infoLabelFont.setFontHeightInPoints((short) 9);
            infoLabelStyle.setFont(infoLabelFont);

            // ── Info value style ───────────────────────────────────────────────────
            XSSFCellStyle infoValueStyle = workbook.createCellStyle();
            XSSFFont infoValueFont = workbook.createFont();
            infoValueFont.setFontHeightInPoints((short) 9);
            infoValueStyle.setFont(infoValueFont);

            // ── Column header style — blue bg, white bold ──────────────────────────
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 9);
            headerFont.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 41, (byte) 128, (byte) 185}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // ── Normal text style ──────────────────────────────────────────────────
            XSSFCellStyle normalStyle = workbook.createCellStyle();
            XSSFFont normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 8);
            normalStyle.setFont(normalFont);
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            // ── Normal number style — center aligned ───────────────────────────────
            XSSFCellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(normalStyle);
            numberStyle.setAlignment(HorizontalAlignment.CENTER);

            // ── Subtotal style — light blue bg, bold ───────────────────────────────
            XSSFCellStyle subtotalStyle = workbook.createCellStyle();
            XSSFFont subtotalFont = workbook.createFont();
            subtotalFont.setBold(true);
            subtotalFont.setFontHeightInPoints((short) 9);
            subtotalStyle.setFont(subtotalFont);
            subtotalStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 213, (byte) 232, (byte) 255}, null));
            subtotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subtotalStyle.setBorderBottom(BorderStyle.THIN);
            subtotalStyle.setBorderTop(BorderStyle.THIN);
            subtotalStyle.setBorderLeft(BorderStyle.THIN);
            subtotalStyle.setBorderRight(BorderStyle.THIN);

            // ── Subtotal number style ──────────────────────────────────────────────
            XSSFCellStyle subtotalNumberStyle = workbook.createCellStyle();
            subtotalNumberStyle.cloneStyleFrom(subtotalStyle);
            subtotalNumberStyle.setAlignment(HorizontalAlignment.CENTER);

            // ── Grand total style — orange bg, bold ────────────────────────────────
            XSSFCellStyle grandTotalStyle = workbook.createCellStyle();
            XSSFFont grandFont = workbook.createFont();
            grandFont.setBold(true);
            grandFont.setFontHeightInPoints((short) 10);
            grandTotalStyle.setFont(grandFont);
            grandTotalStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 255, (byte) 200, (byte) 100}, null));
            grandTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            grandTotalStyle.setBorderBottom(BorderStyle.MEDIUM);
            grandTotalStyle.setBorderTop(BorderStyle.MEDIUM);
            grandTotalStyle.setBorderLeft(BorderStyle.THIN);
            grandTotalStyle.setBorderRight(BorderStyle.THIN);

            // ── Grand total number style ───────────────────────────────────────────
            XSSFCellStyle grandTotalNumberStyle = workbook.createCellStyle();
            grandTotalNumberStyle.cloneStyleFrom(grandTotalStyle);
            grandTotalNumberStyle.setAlignment(HorizontalAlignment.CENTER);

            // ── Total column style — orange bg, white bold ─────────────────────────
            XSSFCellStyle totalColStyle = workbook.createCellStyle();
            XSSFFont totalColFont = workbook.createFont();
            totalColFont.setBold(true);
            totalColFont.setFontHeightInPoints((short) 9);
            totalColFont.setColor(
                    new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
            totalColStyle.setFont(totalColFont);
            totalColStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 255, (byte) 165, (byte) 0}, null));
            totalColStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalColStyle.setAlignment(HorizontalAlignment.CENTER);
            totalColStyle.setBorderBottom(BorderStyle.THIN);
            totalColStyle.setBorderTop(BorderStyle.THIN);
            totalColStyle.setBorderLeft(BorderStyle.THIN);
            totalColStyle.setBorderRight(BorderStyle.THIN);

            int rowIdx = 0;

            // ── Title row ──────────────────────────────────────────────────────────
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(22);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(
                    "Surgery Count Report - Doctor Wise  (Year: " + reportYear + ")");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 14));

            rowIdx++; // blank row

            // ── Info rows ──────────────────────────────────────────────────────────
            String[][] infoRows = {
                {"From Date:", fromYearStartDate != null ? sdf.format(fromYearStartDate) : ""},
                {"To Date:", toYearEndDate != null ? sdf.format(toYearEndDate) : ""},
                {"Speciality:", currentSpeciality != null ? currentSpeciality.getName() : "All"},
                {"Generated:", sdf.format(new Date())}
            };
            for (String[] info : infoRows) {
                Row infoRow = sheet.createRow(rowIdx++);
                org.apache.poi.ss.usermodel.Cell labelCell = infoRow.createCell(0);
                labelCell.setCellValue(info[0]);
                labelCell.setCellStyle(infoLabelStyle);
                org.apache.poi.ss.usermodel.Cell valueCell = infoRow.createCell(1);
                valueCell.setCellValue(info[1]);
                valueCell.setCellStyle(infoValueStyle);
            }

            rowIdx++; // blank row

            // ── Column header row ──────────────────────────────────────────────────
            String[] headers = {
                "Doctor Name", "Speciality",
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                "Total"
            };
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(18);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ──────────────────────────────────────────────────────────
            for (SurgeryCountDoctorWiseDTO item : billList) {

                boolean isSubtotal = item.isSubtotal();
                boolean isGrandTotal = item.isGrandTotal();
                boolean isDataRow = !isSubtotal && !isGrandTotal;

                XSSFCellStyle textStyle = isGrandTotal ? grandTotalStyle
                        : isSubtotal ? subtotalStyle
                                : normalStyle;
                XSSFCellStyle numStyle = isGrandTotal ? grandTotalNumberStyle
                        : isSubtotal ? subtotalNumberStyle
                                : numberStyle;

                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.setHeightInPoints(15);

                // Col 0 – Doctor Name
                org.apache.poi.ss.usermodel.Cell nameCell = dataRow.createCell(0);
                nameCell.setCellValue(item.getDoctorName() != null ? item.getDoctorName() : "");
                nameCell.setCellStyle(textStyle);

                // Col 1 – Speciality
                org.apache.poi.ss.usermodel.Cell specCell = dataRow.createCell(1);
                specCell.setCellValue(isDataRow && item.getSpecialityName() != null
                        ? item.getSpecialityName() : "");
                specCell.setCellStyle(textStyle);

                // Cols 2-13 – Month values
                int[] monthValues = {
                    item.getJanuary(), item.getFebruary(), item.getMarch(),
                    item.getApril(), item.getMay(), item.getJune(),
                    item.getJuly(), item.getAugust(), item.getSeptember(),
                    item.getOctober(), item.getNovember(), item.getDecember()
                };
                for (int m = 0; m < monthValues.length; m++) {
                    org.apache.poi.ss.usermodel.Cell monthCell = dataRow.createCell(2 + m);
                    if (monthValues[m] > 0) {
                        monthCell.setCellValue(monthValues[m]);
                    } else {
                        monthCell.setCellValue("");
                    }
                    monthCell.setCellStyle(numStyle);
                }

                // Col 14 – Total (always orange)
                org.apache.poi.ss.usermodel.Cell totalCell = dataRow.createCell(14);
                totalCell.setCellValue(item.getTotalSurgeries());
                totalCell.setCellStyle(totalColStyle);
            }

            // ── Column widths ──────────────────────────────────────────────────────
            int[] colWidths = {
                6000, 5000,
                1800, 1800, 1800, 1800, 1800, 1800,
                1800, 1800, 1800, 1800, 1800, 1800,
                2200
            };
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i]);
            }

            // ── Write workbook to byte array first, then stream ────────────────────
            // Avoids "IOException never thrown" by separating workbook.write()
            // from the JSF response stream handling
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] excelBytes = baos.toByteArray();

            // ── Write to HTTP response ─────────────────────────────────────────────
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            externalContext.setResponseContentLength(excelBytes.length);
            String fileName = "Surgery_Count_Doctor_Wise_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();
            out.write(excelBytes);
            out.flush();

            facesContext.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating Excel: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private List<SurgeryCountSurgeryWiseDTO> surgeryCountSurgeryWiseList;

    public void processSurgeryCountSurgeryWiseReport() {
        surgeryCountSurgeryWiseList = new ArrayList<>();
        if (fromYearStartDate == null || toYearEndDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return;
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select i.id, ")
                .append(" i.name, ")
                .append(" c.name, ")
                .append(" function('MONTH', b.createdAt) ")
                .append(" from BilledBill b ")
                .append(" join b.procedure p ")
                .append(" join p.item i ")
                .append(" left join i.category c ")
                .append(" where b.retired = false ")
                .append(" and b.cancelled = false ")
                .append(" and b.billType = :bt ")
                .append(" and b.createdAt between :fd and :td ")
                .append(" and p is not null ")
                .append(" and i is not null ");

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.SurgeryBill);
        params.put("fd", fromYearStartDate);
        params.put("td", toYearEndDate);

        if (institution != null) {
            jpql.append(" and b.institution = :inst ");
            params.put("inst", institution);
        }
        if (department != null) {
            jpql.append(" and b.department = :dept ");
            params.put("dept", department);
        }
        if (site != null) {
            jpql.append(" and b.department.site = :site ");
            params.put("site", site);
        }
        if (surgeryType != null) {
            jpql.append(" and c = :stype ");
            params.put("stype", surgeryType);
        }
        if (surgeryItem != null) {
            jpql.append(" and i = :sitem ");
            params.put("sitem", surgeryItem);
        }

        jpql.append(" order by i.name ");

        List<Object[]> results = billFacade.findObjectArrayByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP);

        if (results == null || results.isEmpty()) {
            JsfUtil.addErrorMessage("No surgery records found for the selected period.");
            return;
        }

        Map<Long, SurgeryCountSurgeryWiseDTO> surgeryMap = new LinkedHashMap<>();

        for (Object[] row : results) {
            Long itemId = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            String surgeryName = row[1] != null ? row[1].toString() : "Unknown";
            String categoryName = row[2] != null ? row[2].toString() : "N/A";
            int month = row[3] != null ? ((Number) row[3]).intValue() : 0;

            SurgeryCountSurgeryWiseDTO dto = surgeryMap.get(itemId);
            if (dto == null) {
                dto = new SurgeryCountSurgeryWiseDTO();
                dto.setSurgeryName(surgeryName);
                dto.setSurgeryCategory(categoryName);
                surgeryMap.put(itemId, dto);
            }

            int monthIndex = month - 1;
            if (monthIndex >= 0 && monthIndex < 12) {
                dto.addMonthCount(monthIndex, 1);
            }
        }

        SurgeryCountSurgeryWiseDTO grandTotal = new SurgeryCountSurgeryWiseDTO();
        grandTotal.setSurgeryName("Grand Total");
        grandTotal.setSurgeryCategory("");
        grandTotal.setGrandTotal(true);

        for (SurgeryCountSurgeryWiseDTO dto : surgeryMap.values()) {
            dto.calculateYearTotal();
            surgeryCountSurgeryWiseList.add(dto);
            grandTotal.addAllCounts(dto);
        }

        grandTotal.calculateYearTotal();
        surgeryCountSurgeryWiseList.add(grandTotal);
        createSurgeryWiseChartModels();

    }

    public List<SurgeryCountSurgeryWiseDTO> getExportableSurgeryCountSurgeryWiseList() {
        if (surgeryCountSurgeryWiseList == null) {
            return new ArrayList<>();
        }
        return surgeryCountSurgeryWiseList.stream()
                .filter(dto -> !dto.isGrandTotal())
                .collect(java.util.stream.Collectors.toList());
    }

    public void createSurgeryWiseChartModels() {
        createSurgeryWiseBarChart();
        createSurgeryWiseLineChart();
    }

    private void createSurgeryWiseBarChart() {
        if (surgeryCountSurgeryWiseList == null || surgeryCountSurgeryWiseList.isEmpty()) {
            surgeryWiseBarChartModel = null;
            return;
        }

        String[] colors = {
            "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
            "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
            "255, 99, 255", "99, 255, 132", "220, 20, 60", "65, 105, 225"
        };
        int colorIndex = 0;

        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

        for (SurgeryCountSurgeryWiseDTO dto : surgeryCountSurgeryWiseList) {
            if (dto.isGrandTotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor bgColor = new RGBAColor(
                    Integer.parseInt(rgb[0].trim()),
                    Integer.parseInt(rgb[1].trim()),
                    Integer.parseInt(rgb[2].trim()), 0.7);
            RGBAColor borderColor = new RGBAColor(
                    Integer.parseInt(rgb[0].trim()),
                    Integer.parseInt(rgb[1].trim()),
                    Integer.parseInt(rgb[2].trim()), 1);

            BarDataset dataset = new BarDataset()
                    .setLabel(dto.getSurgeryName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBackgroundColor(bgColor)
                    .setBorderColor(borderColor)
                    .setBorderWidth(1);
            barData.addDataset(dataset);
            colorIndex++;
        }

        barChart.setData(barData);

        BarOptions barOptionsObj = new BarOptions();
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true)
                .setText("Surgery Wise Count - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        barOptionsObj.setPlugins(plugins);

        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions()
                .setBeginAtZero(true)
                .setTicks(new LinearTickOptions().setStepSize(1)));
        barOptionsObj.setScales(scales);

        barChart.setOptions(barOptionsObj);
        surgeryWiseBarChartModel = barChart.toJson();
    }

    private void createSurgeryWiseLineChart() {
        if (surgeryCountSurgeryWiseList == null || surgeryCountSurgeryWiseList.isEmpty()) {
            surgeryWiseLineChartModel = null;
            return;
        }

        String[] colors = {
            "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
            "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
            "255, 99, 255", "99, 255, 132", "220, 20, 60", "65, 105, 225"
        };
        int colorIndex = 0;

        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

        for (SurgeryCountSurgeryWiseDTO dto : surgeryCountSurgeryWiseList) {
            if (dto.isGrandTotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor borderColor = new RGBAColor(
                    Integer.parseInt(rgb[0].trim()),
                    Integer.parseInt(rgb[1].trim()),
                    Integer.parseInt(rgb[2].trim()), 1);

            LineDataset dataset = new LineDataset()
                    .setLabel(dto.getSurgeryName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBorderColor(borderColor)
                    .setFill(new Fill(false))
                    .setTension(0.4f);
            lineData.addDataset(dataset);
            colorIndex++;
        }

        lineChart.setData(lineData);

        LineOptions lineOptionsObj = new LineOptions();
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true)
                .setText("Surgery Wise Count - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.RIGHT));
        lineOptionsObj.setPlugins(plugins);

        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions()
                .setBeginAtZero(true)
                .setTicks(new LinearTickOptions().setStepSize(1)));
        lineOptionsObj.setScales(scales);

        lineChart.setOptions(lineOptionsObj);
        surgeryWiseLineChartModel = lineChart.toJson();
    }

    public void processMonthlyWiseSurgerySurveyReport() {

        if (reportType == null) {
            return;
        }

        monthlySurgeryCountList = new ArrayList<>();

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        if (reportType.equals("SUMMARY")) {

            jpql.append(" Select new com.divudi.core.data.dto.MonthServiceCountDTO(")
                    .append(" FUNCTION('MONTH', a.dateOfDischarge), ")
                    .append(" s.item.category.name, ")
                    .append(" count(s) ")
                    .append(") ")
                    .append(" from PatientEncounter s ")
                    .append(" join s.parentEncounter a ")
                    .append(" Where s.retired = false ")
                    .append(" and a.discharged = true ")
                    .append(" and a.dateOfDischarge is not null ")
                    .append(" AND a.dateOfDischarge BETWEEN :fromDate AND :toDate ");

            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            if (surgeryType != null) {
                jpql.append(" and s.item.category = :stype ");
                params.put("stype", surgeryType);
            }

            if (institution != null) {
                jpql.append(" and a.institution = :inst");
                params.put("inst", institution);
            }

            if (department != null) {
                jpql.append(" and a.department = :dept");
                params.put("dept", department);
            }

            if (site != null) {
                jpql.append(" and a.department.site = :site");
                params.put("site", site);
            }

            jpql.append(" Group By FUNCTION('MONTH', a.dateOfDischarge), s.item.category.name ");

        } else if (reportType.equals("DETAIL")) {

            jpql.append(" Select new com.divudi.core.data.dto.MonthServiceCountDTO(")
                    .append(" FUNCTION('MONTH', a.dateOfDischarge), ")
                    .append(" s.item.name, ")
                    .append(" count(s) ")
                    .append(") ")
                    .append(" from PatientEncounter s ")
                    .append(" join s.parentEncounter a ")
                    .append(" Where s.retired = false ")
                    .append(" and a.discharged = true ")
                    .append(" and a.dateOfDischarge is not null ")
                    .append(" AND a.dateOfDischarge BETWEEN :fromDate AND :toDate ");

            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            if (surgeryType != null) {
                jpql.append(" and s.item.category = :stype ");
                params.put("stype", surgeryType);
            }

            if (institution != null) {
                jpql.append(" and a.institution = :inst");
                params.put("inst", institution);
            }

            if (department != null) {
                jpql.append(" and a.department = :dept");
                params.put("dept", department);
            }

            if (site != null) {
                jpql.append(" and a.department.site = :site");
                params.put("site", site);
            }

            jpql.append(" Group By FUNCTION('MONTH', a.dateOfDischarge), s.item.name ");

        }

        List<MonthServiceCountDTO> rawList
                = (List<MonthServiceCountDTO>) billFacade.findDTOsByJpql(
                        jpql.toString(), params, TemporalType.TIMESTAMP);

        if (rawList.isEmpty()) {
            monthlySurgeryCountList = null;
            return;
        }

        Set<String> surgeryHeaderSet = new HashSet<>();
        Map<Integer, MonthlySurgeryCountDTO> monthMap = new LinkedHashMap<>();

        for (MonthServiceCountDTO dto : rawList) {

            Integer month = dto.getMonth();

            MonthlySurgeryCountDTO aggregated = monthMap.get(month);
            if (aggregated == null) {

                aggregated = new MonthlySurgeryCountDTO();
                aggregated.setMonth(month);
                monthMap.put(month, aggregated);
            }

            aggregated.addServiceCount(dto.getServiceName(), dto.getServiceCount());
            surgeryHeaderSet.add(dto.getServiceName());
        }

        surgeryHeaders = new ArrayList<>(surgeryHeaderSet);
        Collections.sort(surgeryHeaders);

        MonthlySurgeryCountDTO grandTotal = new MonthlySurgeryCountDTO();
        grandTotal.setGrandTotal(true);

        for (int month = 1; month <= 12; month++) {

            MonthlySurgeryCountDTO dto = monthMap.get(month);

            if (dto == null) {
                continue;
            }

            dto.alignWithHeaders(surgeryHeaders);
            grandTotal.addAll(dto);
            monthlySurgeryCountList.add(dto);
        }

        if (!monthlySurgeryCountList.isEmpty()) {
            grandTotal.alignWithHeaders(surgeryHeaders);
            monthlySurgeryCountList.add(grandTotal);
        }

    }

    public void processIpUnsettledInvoicesReport() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        // Use PatientEncounter instead of Admission
        jpql.append("SELECT new com.divudi.core.data.dto.IpUnsettledInvoiceDTO(")
                .append("pe.id, ")
                .append("pe.patient.phn, ")
                .append("pe.patient.person.name, ")
                .append("pe.patient.person.title, ")
                .append("pe.patient.person.phone, ")
                .append("pe.patient.person.dob, ")
                .append("pe.currentPatientRoom, ")
                .append("pe.dateOfDischarge, ")
                .append("pe.paymentFinalized, ")
                .append("COALESCE(pe.netTotal, 0.0), ")
                .append("COALESCE(pe.creditPaidAmount, 0.0), ")
                .append("pe.creater")
                .append(") FROM PatientEncounter pe ");

        if (roomCategory != null) {
            jpql.append("LEFT JOIN pe.currentPatientRoom room ")
                    .append("LEFT JOIN room.roomFacilityCharge rfc ");
        }

        jpql.append("WHERE pe.retired = :ret ")
                .append("AND pe.dateOfAdmission BETWEEN :fd AND :td ")
                .append("AND pe.discharged = TRUE ")
                .append("AND pe.paymentFinalized = FALSE ");

        params.put("ret", false);
        params.put("fd", fromDate);
        params.put("td", toDate);

        // Discharge date filter
        if (dischargeFromDate != null && dischargeToDate != null) {
            jpql.append("AND pe.dateOfDischarge BETWEEN :dfd AND :dtd ");
            params.put("dfd", dischargeFromDate);
            params.put("dtd", dischargeToDate);
        }

        // Invoice approved date filter
        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null) {
            jpql.append("AND pe.finalBill IS NOT NULL ")
                    .append("AND pe.finalBill.createdAt BETWEEN :iafd AND :iatd ");
            params.put("iafd", invoiceApprovedFromDate);
            params.put("iatd", invoiceApprovedToDate);
        }

        // Institution filter
        if (institution != null) {
            jpql.append("AND pe.institution = :inst ");
            params.put("inst", institution);
        }

        // Site filter
        if (site != null) {
            jpql.append("AND pe.department.site = :site ");
            params.put("site", site);
        }

        // Department filter
        if (department != null) {
            jpql.append("AND pe.department = :dept ");
            params.put("dept", department);
        }

        // Consultant filter
        if (consultant != null) {
            jpql.append("AND pe.referringConsultant = :cons ");
            params.put("cons", consultant);
        }

        // Service Center filter (assuming this uses department)
        if (serviceCenter != null) {
            jpql.append("AND pe.department = :sc ");
            params.put("sc", serviceCenter);
        }

        if (sponsor != null) {
            jpql.append("AND pe.creditCompany = :sponsor ");
            params.put("sponsor", sponsor);
        }

        // Admission Status filter (if PatientEncounter has admissionStatus)
        if (admissionStatus != null) {
            jpql.append("AND pe.admissionStatus = :as ");
            params.put("as", admissionStatus);
        }

        // Admission Type filter
        if (admissionType != null) {
            jpql.append("AND pe.admissionType = :at ");
            params.put("at", admissionType);
        }

        // Payment Method filter
        if (paymentMethod != null) {
            jpql.append("AND pe.paymentMethod = :pm ");
            params.put("pm", paymentMethod);
        }

        // Room Category filter
        if (roomCategory != null) {
            jpql.append("AND rfc.roomCategory = :rc ");
            params.put("rc", roomCategory);
        }

        jpql.append("ORDER BY pe.dateOfAdmission ");

        try {
            unsettledInvoicesList = (List<IpUnsettledInvoiceDTO>) peFacade.findLightsByJpql(
                    jpql.toString(),
                    params,
                    TemporalType.TIMESTAMP
            );

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error loading unsettled invoices: " + e.getMessage());
            unsettledInvoicesList = new ArrayList<>();
        }

        if (unsettledInvoicesList == null) {
            unsettledInvoicesList = new ArrayList<>();
        }

    }

    public void processSpecialtyDoctorWiseIncomeReport() {
        spcDocIncomeBillList = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        btas.add(BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL);
        btas.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

        if (byDoctor) {
            jpql.append(" Select new com.divudi.core.data.dto.InwardIncomeDoctorSpecialtyDTO(")
                    .append(" bf.staff.id, ")
                    .append(" bf.staff.person.title,")
                    .append(" coalesce(bf.staff.person.name, 'N/A'), ")
                    .append(" coalesce(bf.staff.speciality.name, 'N/A'), ")
                    .append(" coalesce(bf.feeValue, 0.0), ")
                    .append(" coalesce(bf.billItem.hospitalFee, 0.0) ")
                    .append(") ");
        } else {
            jpql.append(" Select new com.divudi.core.data.dto.InwardIncomeDoctorSpecialtyDTO(")
                    .append(" bf.staff.speciality.id, ")
                    .append(" coalesce(bf.staff.speciality.name, 'N/A'), ")
                    .append(" coalesce(bf.feeValue, 0.0), ")
                    .append(" coalesce(bf.billItem.hospitalFee, 0.0) ")
                    .append(") ");
        }

        jpql.append(" from BillFee bf")
                .append(" Where bf.retired = false ")
                .append(" And bf.bill.retired=false ")
                .append(" And bf.billItem.retired=false ")
                .append(" And bf.bill.billTypeAtomic in :btas ")
                .append(" And (type(bf.staff) = :doctorClass OR type(bf.staff) = :consultantClass) ")
                .append(" AND bf.bill.createdAt BETWEEN :fromDate AND :toDate ");

        params.put("btas", btas);
        params.put("fromDate", fromYearStartDate);
        params.put("toDate", toYearEndDate);
        params.put("doctorClass", Doctor.class);
        params.put("consultantClass", Consultant.class);

        if (currentSpeciality != null) {
            jpql.append(" AND bf.staff.speciality = :spe ");
            params.put("spe", currentSpeciality);
        }

        if (currentDoctor != null) {
            jpql.append(" and bf.staff.id=:staffid ");
            params.put("staffid", currentDoctor.getId());
        }

        jpql.append(" ORDER BY bf.staff.speciality.name, bf.staff.person.name ");

        List<InwardIncomeDoctorSpecialtyDTO> rawList = (List<InwardIncomeDoctorSpecialtyDTO>) billFeeFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        if (byDoctor) {
            processDoctorWiseIncomeReport(rawList);
        } else {
            processSpecialtyWiseIncomeReport(rawList);
        }
    }

    public void calculateTotalValuesSpcDocIncome(Map<Long, InwardIncomeDoctorSpecialtyDTO> m) {
        if (spcDocIncomeBillList == null) {
            spcDocIncomeBillList = new ArrayList<>();
        }

        Double docChargeTotal = 0.0;
        Double hospitalChargeTotal = 0.0;
        Double totalCharge = 0.0;

        InwardIncomeDoctorSpecialtyDTO curr;

        for (Map.Entry<Long, InwardIncomeDoctorSpecialtyDTO> entry : m.entrySet()) {
            curr = entry.getValue();
            curr.setTotalCharge(curr.getDocFee() + curr.getHosFee());

            if (entry.getValue().getTotalCharge() == 0.0) {
                continue;
            }

            getSpcDocIncomeBillList().add(curr);
            docChargeTotal += curr.getDocFee();
            hospitalChargeTotal += curr.getHosFee();
            totalCharge += curr.getTotalCharge();
        }

        totalValuesSpcDocIncome = new InwardIncomeDoctorSpecialtyDTO();
        totalValuesSpcDocIncome.setDocFee(docChargeTotal);
        totalValuesSpcDocIncome.setHosFee(hospitalChargeTotal);
        totalValuesSpcDocIncome.setTotalCharge(totalCharge);
    }

    public void processSpecialtyWiseIncomeReport(List<InwardIncomeDoctorSpecialtyDTO> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return;
        }

        Map<Long, InwardIncomeDoctorSpecialtyDTO> specialtyMap = new LinkedHashMap<>();

        for (InwardIncomeDoctorSpecialtyDTO dto : rawList) {
            if (dto.getStaffId() == null) {
                continue;
            }

            Long sId = dto.getStaffId();
            InwardIncomeDoctorSpecialtyDTO currentSpc = specialtyMap.computeIfAbsent(sId, k -> {
                InwardIncomeDoctorSpecialtyDTO spc = new InwardIncomeDoctorSpecialtyDTO();
                spc.setStaffId(dto.getStaffId());
                spc.setSpecialtyName(dto.getSpecialtyName());

                return spc;
            });

            currentSpc.setDocFee(currentSpc.getDocFee() + dto.getDocFee());
            currentSpc.setHosFee(currentSpc.getHosFee() + dto.getHosFee());
        }

        calculateTotalValuesSpcDocIncome(specialtyMap);

    }

    public void processDoctorWiseIncomeReport(List<InwardIncomeDoctorSpecialtyDTO> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return;
        }

        Map<Long, InwardIncomeDoctorSpecialtyDTO> doctorMap = new LinkedHashMap<>();

        for (InwardIncomeDoctorSpecialtyDTO dto : rawList) {
            if (dto.getStaffId() == null) {
                continue;
            }

            Long sId = dto.getStaffId();
            InwardIncomeDoctorSpecialtyDTO currentDoc = doctorMap.computeIfAbsent(sId, k -> {
                InwardIncomeDoctorSpecialtyDTO doc = new InwardIncomeDoctorSpecialtyDTO();
                doc.setStaffId(dto.getStaffId());
                doc.setDoctorTitle(dto.getDoctorTitle());
                doc.setDoctorName(dto.getDoctorName());
                doc.setSpecialtyName(dto.getSpecialtyName());

                return doc;
            });

            currentDoc.setDocFee(currentDoc.getDocFee() + dto.getDocFee());
            currentDoc.setHosFee(currentDoc.getHosFee() + dto.getHosFee());
        }

        calculateTotalValuesSpcDocIncome(doctorMap);
    }

    public void processSpecialtyDoctorDemographicDataReport() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        demographicDataList = null;

        if (byDoctor) {
            jpql.append(" Select new com.divudi.core.data.dto.InwardAdmissionDemographicDataDTO(")
                    .append(" rc.id, ")
                    .append(" rcp.title, ")
                    .append(" coalesce(rcp.name, 'N/A'), ")
                    .append(" coalesce(rcs.name, 'N/A'), ")
                    .append(" pp.dob, ")
                    .append(" pp.sex ")
                    .append(") ");
        } else {
            jpql.append(" Select new com.divudi.core.data.dto.InwardAdmissionDemographicDataDTO(")
                    .append(" rcs.id, ")
                    .append(" coalesce(rcs.name, 'N/A'), ")
                    .append(" pp.dob, ")
                    .append(" pp.sex ")
                    .append(") ");
        }

        jpql.append(" from Admission a ")
                .append(" left join a.referringConsultant rc ")
                .append(" left join rc.person rcp ")
                .append(" left join rc.speciality rcs ")
                .append(" left join a.patient p ")
                .append(" left join p.person pp")
                .append(" Where a.retired = false ")
                .append(" And type(a.referringConsultant) = :consultantClass ")
                //            .append(" And a.dateOfAdmission <= :toDate ")
                //            .append(" And (a.dateOfDischarge >= :fromDate OR a.dateOfDischarge IS NULL) ");
                // date range considered for discharge date
                .append(" AND a.dateOfDischarge BETWEEN :fromDate AND :toDate ");

        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("consultantClass", Consultant.class);

        if (currentSpeciality != null) {
            jpql.append(" And rcs = :spe ");
            params.put("spe", currentSpeciality);
        }
        if (currentDoctor != null) {
            jpql.append(" And rc.id = :conId");
            params.put("conId", currentDoctor.getId());
        }
        if (institution != null) {
            jpql.append(" And a.institution = :inst");
            params.put("inst", institution);
        }
        if (department != null) {
            jpql.append(" And a.department = :dept");
            params.put("dept", department);
        }
        if (site != null) {
            jpql.append(" And a.department.site = :site");
            params.put("site", site);
        }

        jpql.append(" ORDER BY rcs.name, rcp.name ");

        List<InwardAdmissionDemographicDataDTO> rawList = (List<InwardAdmissionDemographicDataDTO>) peFacade.findLightsByJpqlWithoutCache(jpql.toString(), params, TemporalType.TIMESTAMP);
        demographicDataUnknownGender = false;

        if (byDoctor) {
            processDemographicDataDoctorWiseReport(rawList);
            demographicGeneratedByDoctor = true;
        } else {
            processDemographicDataSpecialtyWiseReport(rawList);
            demographicGeneratedByDoctor = false;
        }
    }

    public void processDemographicDataSpecialtyWiseReport(List<InwardAdmissionDemographicDataDTO> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return;
        }

        Map<Long, InwardAdmissionDemographicDataDTO> specialtyList = new LinkedHashMap<>();

        for (InwardAdmissionDemographicDataDTO dto : rawList) {
            if (dto.getId() == null) {
                continue;
            }

            InwardAdmissionDemographicDataDTO currentSpc = specialtyList.computeIfAbsent(dto.getId(), k -> {
                InwardAdmissionDemographicDataDTO newDto = new InwardAdmissionDemographicDataDTO(dto.getSpecialityName(), null, null);
                return newDto;
            }
            );

            currentSpc.incrementGenderCount(dto.getPatientSex());
            currentSpc.incrementAgeGroupCount(dto.getPatientAge());
            currentSpc.incrementTotalCount();

            if (dto.getPatientSex() == Sex.Unknown) {
                demographicDataUnknownGender = true;
            }
        }

        demographicDataList = new ArrayList<>(specialtyList.values());
    }

    public void processDemographicDataDoctorWiseReport(List<InwardAdmissionDemographicDataDTO> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return;
        }

        Map<Long, InwardAdmissionDemographicDataDTO> doctorList = new LinkedHashMap<>();

        for (InwardAdmissionDemographicDataDTO dto : rawList) {
            if (dto.getId() == null) {
                continue;
            }

            InwardAdmissionDemographicDataDTO currentSpc = doctorList.computeIfAbsent(dto.getId(), k -> {
                InwardAdmissionDemographicDataDTO newDto = new InwardAdmissionDemographicDataDTO(dto.getSpecialityName(), dto.getDoctorTitle(), dto.getDoctorName());
                return newDto;
            }
            );

            currentSpc.incrementGenderCount(dto.getPatientSex());
            currentSpc.incrementAgeGroupCount(dto.getPatientAge());
            currentSpc.incrementTotalCount();

            if (dto.getPatientSex() == Sex.Unknown) {
                demographicDataUnknownGender = true;
            }
        }

        demographicDataList = new ArrayList<>(doctorList.values());
    }

    private String lineChartModel;
    private String barChartModel;
    private String specialtyLineChartModel;
    private String specialtyBarChartModel;

    public void createChartModels() {
        createDoctorCharts();
        createSpecialtyCharts();
    }

    private void createDoctorCharts() {
        // Define color palette (RGB strings without alpha for now)
        String[] colors = {
            "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
            "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
            "255, 99, 255", "99, 255, 132"
        };
        int colorIndex = 0;

        // Line Chart
        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (dto.isSubtotal() || dto.isGrandTotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            LineDataset dataset = new LineDataset()
                    .setLabel(dto.getDoctorName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBorderColor(borderColor)
                    .setFill(new Fill(false))
                    .setTension(0.4f);
            lineData.addDataset(dataset);
            colorIndex++;
        }
        lineChart.setData(lineData);
        LineOptions lineOptionsObj = new LineOptions();
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Doctor Wise Surgery Count - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.RIGHT));
        lineOptionsObj.setPlugins(plugins);
        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(1)));
        lineOptionsObj.setScales(scales);
        lineChart.setOptions(lineOptionsObj);
        lineChartModel = lineChart.toJson();

        // Bar Chart (similar logic)
        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        colorIndex = 0;
        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (dto.isSubtotal() || dto.isGrandTotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor bgColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 0.7);
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            BarDataset dataset = new BarDataset()
                    .setLabel(dto.getDoctorName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBackgroundColor(bgColor)
                    .setBorderColor(borderColor)
                    .setBorderWidth(1);
            barData.addDataset(dataset);
            colorIndex++;
        }
        barChart.setData(barData);
        BarOptions barOptionsObj = new BarOptions();
        plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Doctor Wise Surgery Count - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        barOptionsObj.setPlugins(plugins);
        scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(1)));
        barOptionsObj.setScales(scales);
        barChart.setOptions(barOptionsObj);
        barChartModel = barChart.toJson();
    }

    private void createSpecialtyCharts() {
        // Similar to createDoctorCharts, but for specialties (use subtotals)
        String[] colors = {
            "220, 20, 60", "65, 105, 225", "255, 140, 0",
            "34, 139, 34", "138, 43, 226", "255, 215, 0"
        };
        int colorIndex = 0;

        // Line Chart
        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (!dto.isSubtotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            LineDataset dataset = new LineDataset()
                    .setLabel(dto.getSpecialityName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBorderColor(borderColor)
                    .setBorderWidth(3)
                    .setFill(new Fill(false))
                    .setTension(0.4f);
            lineData.addDataset(dataset);
            colorIndex++;
        }
        lineChart.setData(lineData);
        LineOptions lineOptionsObj = new LineOptions();
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Specialty Wise Surgery Count - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.RIGHT));
        lineOptionsObj.setPlugins(plugins);
        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(5)));
        lineOptionsObj.setScales(scales);
        lineChart.setOptions(lineOptionsObj);
        specialtyLineChartModel = lineChart.toJson();

        // Bar Chart (similar)
        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        colorIndex = 0;
        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (!dto.isSubtotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor bgColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 0.7);
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            BarDataset dataset = new BarDataset()
                    .setLabel(dto.getSpecialityName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBackgroundColor(bgColor)
                    .setBorderColor(borderColor)
                    .setBorderWidth(2);
            barData.addDataset(dataset);
            colorIndex++;
        }
        barChart.setData(barData);
        BarOptions barOptionsObj = new BarOptions();
        plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Specialty Wise Surgery Count - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        barOptionsObj.setPlugins(plugins);
        scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(5)));
        barOptionsObj.setScales(scales);
        barChart.setOptions(barOptionsObj);
        specialtyBarChartModel = barChart.toJson();
    }

    public int getSelectedYear() {
        Calendar cal = Calendar.getInstance();
        if (fromYearStartDate != null) {
            cal.setTime(fromYearStartDate);
        }
        return cal.get(Calendar.YEAR);
    }

    private String doctorLineData;
    private String doctorLineOptions;
    private String doctorBarData;
    private String doctorBarOptions;
    private String specialtyLineData;
    private String specialtyLineOptions;
    private String specialtyBarData;
    private String specialtyBarOptions;

    public String getDoctorLineData() {
        return doctorLineData;
    }

    public String getDoctorLineOptions() {
        return doctorLineOptions;
    }

    public String getDoctorBarData() {
        return doctorBarData;
    }

    public String getDoctorBarOptions() {
        return doctorBarOptions;
    }

    public String getSpecialtyLineData() {
        return specialtyLineData;
    }

    public String getSpecialtyLineOptions() {
        return specialtyLineOptions;
    }

    public String getSpecialtyBarData() {
        return specialtyBarData;
    }

    public String getSpecialtyBarOptions() {
        return specialtyBarOptions;
    }

    private List<InwardAdmissionDTO> list;

    public void clearAdmissionCountConsultantWiseReport() {
        list = null;
        specialtyLineChartImage = null;
        specialtyBarChartImage = null;
        doctorLineChartImage = null;
        doctorBarChartImage = null;
        admissionReportProcessedAt = null;
        admissionReportProcessedBy = null;
        specialtyLineChartModel = null;
        specialtyBarChartModel = null;
        lineChartModel = null;
        barChartModel = null;
        fromYearStartDate = null;
        toYearEndDate = null;
        currentSpeciality = null;
    }

    public void processAdmissionCountConsultantWiseReport() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append(" Select new com.divudi.core.data.dto.InwardAdmissionDTO(")
                .append(" e.referringConsultant.id, ")
                .append(" e.referringConsultant.person.title, ")
                .append(" e.referringConsultant.person.name, ")
                .append(" e.referringConsultant.speciality.name, ")
                .append(" e.dateOfDischarge ")
                .append(") ")
                .append(" from PatientEncounter e ")
                .append(" Where e.retired = false ")
                .append(" And type(e.referringConsultant) = :consultantClass ")
                .append(" AND e.dateOfDischarge BETWEEN :fromDate AND :toDate ");

        params.put("fromDate", fromYearStartDate);
        params.put("toDate", toYearEndDate);
        params.put("consultantClass", Consultant.class);

        if (currentSpeciality != null) {
            jpql.append(" AND e.referringConsultant.speciality = :spe ");
            params.put("spe", currentSpeciality);
        }

        jpql.append(" ORDER BY e.referringConsultant.speciality.name, e.referringConsultant.person.name ");

        List<InwardAdmissionDTO> rawList = (List<InwardAdmissionDTO>) peFacade.findLightsByJpqlWithoutCache(jpql.toString(), params, TemporalType.TIMESTAMP);

        // Group by specialty and doctor, count surgeries month-wise
        Map<String, Map<Long, InwardAdmissionDTO>> specialtyDoctorMap = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();

        for (InwardAdmissionDTO dto : rawList) {
            String speciality = dto.getSpecialityName();
            Long staffId = dto.getStaffId();

            // Get or create specialty map
            Map<Long, InwardAdmissionDTO> doctorMap = specialtyDoctorMap.get(speciality);
            if (doctorMap == null) {
                doctorMap = new LinkedHashMap<>();
                specialtyDoctorMap.put(speciality, doctorMap);
            }

            // Get or create doctor aggregation
            InwardAdmissionDTO aggregated = doctorMap.get(staffId);
            if (aggregated == null) {
                aggregated = new InwardAdmissionDTO(
                        dto.getStaffId(),
                        dto.getDoctorTitle(),
                        dto.getDoctorName(),
                        dto.getSpecialityName(),
                        null
                );
                doctorMap.put(staffId, aggregated);
            }

            // Increment month counter
            if (dto.getDateOfDischarge() != null) {
                cal.setTime(dto.getDateOfDischarge());
                int month = cal.get(Calendar.MONTH);
                aggregated.addMonthCount(month, 1);
            }
        }

        // Build final list with subtotals and grand total
        list = new ArrayList<>();
        InwardAdmissionDTO grandTotal = new InwardAdmissionDTO();

        for (Map.Entry<String, Map<Long, InwardAdmissionDTO>> specialtyEntry : specialtyDoctorMap.entrySet()) {
            String speciality = specialtyEntry.getKey();
            Map<Long, InwardAdmissionDTO> doctorMap = specialtyEntry.getValue();

            InwardAdmissionDTO subtotal = new InwardAdmissionDTO(speciality);

            // Add all doctors for this specialty
            for (InwardAdmissionDTO doctor : doctorMap.values()) {
                list.add(doctor);
                subtotal.addAllCounts(doctor);
                grandTotal.addAllCounts(doctor);
            }

            // Add subtotal row
            list.add(subtotal);
        }

        // Add grand total row
        list.add(grandTotal);

        createAdmissionCountCharts();
        admissionReportProcessedAt = new Date();
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            com.divudi.core.entity.WebUser u = sessionController.getLoggedUser();
            String personName = (u.getWebUserPerson() != null && u.getWebUserPerson().getName() != null)
                    ? u.getWebUserPerson().getName() : null;
            admissionReportProcessedBy = (personName != null && !personName.isBlank()) ? personName : u.getName();
        }
    }

    public void createAdmissionCountCharts() {
        createConsultantWiseCharts();
        createSpecialtyWiseCharts();
    }

    private void createConsultantWiseCharts() {
        // Define color palette (RGB strings without alpha for now)
        String[] colors = {
            "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
            "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
            "255, 99, 255", "99, 255, 132"
        };
        int colorIndex = 0;

        // Line Chart
        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        for (InwardAdmissionDTO dto : list) {
            if (dto.isSubtotal() || dto.isGrandTotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            LineDataset dataset = new LineDataset()
                    .setLabel(dto.getDoctorName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBorderColor(borderColor)
                    .setFill(new Fill(false))
                    .setTension(0.4f);
            lineData.addDataset(dataset);
            colorIndex++;
        }
        lineChart.setData(lineData);
        LineOptions lineOptionsObj = new LineOptions();
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Doctor Wise Count"));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        lineOptionsObj.setPlugins(plugins);
        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(1)));
        lineOptionsObj.setScales(scales);
        lineChart.setOptions(lineOptionsObj);
        lineChartModel = lineChart.toJson();

        // Bar Chart (similar logic)
        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        colorIndex = 0;
        for (InwardAdmissionDTO dto : list) {
            if (dto.isSubtotal() || dto.isGrandTotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor bgColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 0.7);
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            BarDataset dataset = new BarDataset()
                    .setLabel(dto.getDoctorName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBackgroundColor(bgColor)
                    .setBorderColor(borderColor)
                    .setBorderWidth(1);
            barData.addDataset(dataset);
            colorIndex++;
        }
        barChart.setData(barData);
        BarOptions barOptionsObj = new BarOptions();
        plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Doctor Wise Count"));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        barOptionsObj.setPlugins(plugins);
        scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(1)));
        barOptionsObj.setScales(scales);
        barChart.setOptions(barOptionsObj);
        barChartModel = barChart.toJson();
    }

    private void createSpecialtyWiseCharts() {
        // Similar to createDoctorCharts, but for specialties (use subtotals)
        String[] colors = {
            "220, 20, 60", "65, 105, 225", "255, 140, 0",
            "34, 139, 34", "138, 43, 226", "255, 215, 0"
        };
        int colorIndex = 0;

        // Line Chart
        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        for (InwardAdmissionDTO dto : list) {
            if (!dto.isSubtotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            LineDataset dataset = new LineDataset()
                    .setLabel(dto.getSpecialityName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBorderColor(borderColor)
                    .setBorderWidth(3)
                    .setFill(new Fill(false))
                    .setTension(0.4f);
            lineData.addDataset(dataset);
            colorIndex++;
        }
        lineChart.setData(lineData);
        LineOptions lineOptionsObj = new LineOptions();
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Specialty Wise Count"));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        lineOptionsObj.setPlugins(plugins);
        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(5)));
        lineOptionsObj.setScales(scales);
        lineChart.setOptions(lineOptionsObj);
        specialtyLineChartModel = lineChart.toJson();

        // Bar Chart (similar)
        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        colorIndex = 0;
        for (InwardAdmissionDTO dto : list) {
            if (!dto.isSubtotal()) {
                continue;
            }
            String[] rgb = colors[colorIndex % colors.length].split(",");
            RGBAColor bgColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 0.7);
            RGBAColor borderColor = new RGBAColor(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()), 1);
            BarDataset dataset = new BarDataset()
                    .setLabel(dto.getSpecialityName())
                    .addData(dto.getJanuary()).addData(dto.getFebruary()).addData(dto.getMarch())
                    .addData(dto.getApril()).addData(dto.getMay()).addData(dto.getJune())
                    .addData(dto.getJuly()).addData(dto.getAugust()).addData(dto.getSeptember())
                    .addData(dto.getOctober()).addData(dto.getNovember()).addData(dto.getDecember())
                    .setBackgroundColor(bgColor)
                    .setBorderColor(borderColor)
                    .setBorderWidth(2);
            barData.addDataset(dataset);
            colorIndex++;
        }
        barChart.setData(barData);
        BarOptions barOptionsObj = new BarOptions();
        plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true).setText("Specialty Wise Count"));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        barOptionsObj.setPlugins(plugins);
        scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(5)));
        barOptionsObj.setScales(scales);
        barChart.setOptions(barOptionsObj);
        specialtyBarChartModel = barChart.toJson();
    }

    private String paymentTypeLineChartModel;
    private String paymentTypeBarChartModel;

    private List<PaymentTypeAdmissionDTO> paymentTypeAdmissionCountList;

    public void processPaymentTypeWiseAdmissionCountReport() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append(" SELECT new com.divudi.core.data.dto.PaymentTypeAdmissionDTO(")
                .append(" e.dateOfAdmission, ")
                .append(" e.paymentMethod, ")
                .append(" e.claimable ")
                .append(") ")
                .append(" FROM PatientEncounter e ")
                .append(" WHERE e.retired = false ")
                .append(" AND e.paymentMethod IN :methods ")
                .append(" AND e.dateOfAdmission BETWEEN :fromDate AND :toDate ");

        params.put("methods", Arrays.asList(
                PaymentMethod.Cash,
                PaymentMethod.Credit
        ));

        params.put("fromDate", fromYearStartDate);
        params.put("toDate", toYearEndDate);

        List<PaymentTypeAdmissionDTO> rawList
                = (List<PaymentTypeAdmissionDTO>) peFacade.findLightsByJpqlWithoutCache(
                        jpql.toString(),
                        params,
                        TemporalType.TIMESTAMP
                );

        // Month-wise aggregation
        Map<Integer, PaymentTypeAdmissionDTO> monthMap = new LinkedHashMap<>();

        for (PaymentTypeAdmissionDTO dto : rawList) {

            int month = dto.getMonth();

            PaymentTypeAdmissionDTO aggregated = monthMap.get(month);
            if (aggregated == null) {
                aggregated = new PaymentTypeAdmissionDTO();
                aggregated.setMonth(month);
                monthMap.put(month, aggregated);
            }

            aggregated.add(dto);

        }

        // Final list + grand total
        paymentTypeAdmissionCountList = new ArrayList<>();
        PaymentTypeAdmissionDTO grandTotal = new PaymentTypeAdmissionDTO();

//        for (PaymentTypeAdmissionDTO row : monthMap.values()) {
//            System.out.println(
//                    "Month=" + row.getMonth()
//                    + " Cash=" + row.getCash()
//                    + " Claim=" + row.getCashToBeClaim()
//                    + " Credit=" + row.getCredit()
//            );
//            paymentTypeAdmissionCountList.add(row);
//            grandTotal.addAll(row);
//        }
        for (int month = 0; month < 12; month++) {
            PaymentTypeAdmissionDTO row = monthMap.get(month);
            if (row != null) {
                paymentTypeAdmissionCountList.add(row);
                grandTotal.addAll(row);
            }
        }

        grandTotal.setIsGrandTotal(true);
        paymentTypeAdmissionCountList.add(grandTotal);

        createPaymentTypeWiseAdmissionCountCharts();

    }

    public void createPaymentTypeWiseAdmissionCountCharts() {
        createPaymentTypeLineChart();
        createPaymentTypeBarChart();
    }

    private void createPaymentTypeLineChart() {

        long[] cash = new long[12];
        long[] cashTbc = new long[12];
        long[] credit = new long[12];

        for (PaymentTypeAdmissionDTO dto : paymentTypeAdmissionCountList) {

            if (dto.isIsGrandTotal()) {
                continue;
            }
            int m = dto.getMonth();
            cash[m] = dto.getCash();
            cashTbc[m] = dto.getCashToBeClaim();
            credit[m] = dto.getCredit();
        }

        LineChart chart = new LineChart();
        LineData data = new LineData();

        data.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

        // Cash
        LineDataset cashDs = new LineDataset()
                .setLabel("Cash")
                .setBorderColor(new RGBAColor(54, 162, 235, 1))
                .setFill(new Fill(false))
                .setTension(0.4f);
        for (long v : cash) {
            cashDs.addData(v);
        }

        // Cash To Be Claim
        LineDataset tbcDs = new LineDataset()
                .setLabel("Cash (To Be Claim)")
                .setBorderColor(new RGBAColor(255, 159, 64, 1))
                .setFill(new Fill(false))
                .setTension(0.4f);
        for (long v : cashTbc) {
            tbcDs.addData(v);
        }

        // Credit
        LineDataset creditDs = new LineDataset()
                .setLabel("Credit")
                .setBorderColor(new RGBAColor(255, 99, 132, 1))
                .setFill(new Fill(false))
                .setTension(0.4f);
        for (long v : credit) {
            creditDs.addData(v);
        }

        data.addDataset(cashDs);
        data.addDataset(tbcDs);
        data.addDataset(creditDs);

        chart.setData(data);

        LineOptions options = new LineOptions();
        options.setPlugins(new Plugins()
                .setTitle(new Title().setDisplay(true)
                        .setText("Payment Type Wise Admission Count"))
                .setLegend(new Legend().setDisplay(true)));

        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(5)));
        options.setScales(scales);
        chart.setOptions(options);

        paymentTypeLineChartModel = chart.toJson();
    }

    private void createPaymentTypeBarChart() {

        long[] cash = new long[12];
        long[] cashTbc = new long[12];
        long[] credit = new long[12];

        for (PaymentTypeAdmissionDTO dto : paymentTypeAdmissionCountList) {
            if (dto.isIsGrandTotal()) {
                continue;
            }
            int m = dto.getMonth();
            cash[m] = dto.getCash();
            cashTbc[m] = dto.getCashToBeClaim();
            credit[m] = dto.getCredit();
        }

        BarChart chart = new BarChart();
        BarData data = new BarData();

        data.addLabels("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

        BarDataset cashDs = new BarDataset().setLabel("Cash").setBackgroundColor(new RGBAColor(54, 162, 235, 0.7f))
                .setBorderColor(new RGBAColor(54, 162, 235, 1f))
                .setBorderWidth(1);
        for (long v : cash) {
            cashDs.addData(v);
        }

        BarDataset tbcDs = new BarDataset().setLabel("Cash (To Be Claim)").setBackgroundColor(new RGBAColor(255, 159, 64, 0.7f))
                .setBorderColor(new RGBAColor(255, 159, 64, 1f))
                .setBorderWidth(1);
        for (long v : cashTbc) {
            tbcDs.addData(v);
        }

        BarDataset creditDs = new BarDataset().setLabel("Credit").setBackgroundColor(new RGBAColor(255, 99, 132, 0.7f))
                .setBorderColor(new RGBAColor(255, 99, 132, 1f))
                .setBorderWidth(1);
        for (long v : credit) {
            creditDs.addData(v);
        }

        data.addDataset(cashDs);
        data.addDataset(tbcDs);
        data.addDataset(creditDs);

        chart.setData(data);

        BarOptions options = new BarOptions();
        options.setPlugins(new Plugins()
                .setTitle(new Title().setDisplay(true)
                        .setText("Payment Type Wise Admission Count"))
                .setLegend(new Legend().setDisplay(true)));

        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true).setTicks(new LinearTickOptions().setStepSize(5)));
        options.setScales(scales);

        chart.setOptions(options);

        paymentTypeBarChartModel = chart.toJson();
    }

    public void fillAdmissions(Boolean discharged, Boolean finalized) {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        if (withoutCancelBHT) {
            sql += " and b.retired=false ";
        }
        //// // System.out.println("discharged = " + discharged);
        if (discharged != null) {
            if (discharged) {
                sql += " and b.discharged=true ";
            } else {
                sql += " and b.discharged=false ";
            }
        }
        if (finalized != null) {
            if (finalized) {
                sql += " and b.paymentFinalized=true ";
            } else {
                sql += " and b.paymentFinalized=false ";
            }
        }

        sql += " order by b.dateOfAdmission,b.bhtNo ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        calTtoal();
        calTtoal(patientEncounters);
    }

    private void calTtoal() {
        if (patientEncounters == null) {
            return;
        }

        netTotal = 0;
        netPaid = 0;
        for (PatientEncounter p : patientEncounters) {
            bhtSummeryController.setPatientEncounter((Admission) p);
            bhtSummeryController.createTables();
            p.setTransTotal(bhtSummeryController.getGrantTotal());
            p.setTransPaid(bhtSummeryController.getPaid());

            netTotal += p.getTransTotal();
            netPaid += p.getTransPaid();
        }
    }

    private void calTtoal(List<PatientEncounter> patientEncounters) {
        if (patientEncounters == null) {
            return;
        }
        netTotal = 0;
        netPaid = 0;
        for (PatientEncounter p : patientEncounters) {
            if (p.getFinalBill() != null) {
                netTotal += p.getFinalBill().getNetTotal();
                netPaid += p.getPaidByCreditCompany() + p.getFinalBill().getPaidAmount();
            }
        }
    }

    public void fillAdmissionBookOnlyInwardDeleted() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=true "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    double total;
    double paid;
    double creditPaid;
    double creditUsed;
    double calTotal;
    double totalVat;
    double totalVatCalculatedValue;

    public void fillDischargeBook() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        calTotalDischarged();
    }

    public void calTotalDischarged() {
        if (patientEncounters == null) {
            return;
        }
        setTotal(0);
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : patientEncounters) {
            inwardReportControllerBht.setPatientEncounter(p);
            inwardReportControllerBht.process();
            p.setTransTotal(inwardReportControllerBht.getNetTotal());

            if (p.getFinalBill() != null) {
                setTotal(getTotal() + p.getFinalBill().getNetTotal());
                paid += p.getFinalBill().getPaidAmount();
            }

            creditUsed += p.getCreditUsedAmount();
            creditPaid += p.getPaidByCreditCompany();
            calTotal += p.getTransTotal();
        }
    }

    public void calTotalDischargedNoChanges() {
        if (patientEncounters == null) {
            return;
        }

        total = 0.0;
        totalVat = 0.0;
        totalVatCalculatedValue = 0.0;
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : patientEncounters) {
            p.setTransPaidByPatient(calPaidByPatient(p));
            p.setTransPaidByCompany(calPaidByCompany(p));
            if (p.getFinalBill() == null) {
                continue;
            }
            for (BillItem bi : p.getFinalBill().getBillItems()) {
                if (bi.getInwardChargeType() == InwardChargeType.VAT) {
                    p.getFinalBill().setVat(bi.getNetValue() + p.getFinalBill().getVat());
                }
                if (bi.getInwardChargeType() != InwardChargeType.VAT && bi.getInwardChargeType() != InwardChargeType.Medicine) {
                    p.getFinalBill().setVatCalulatedAmount(bi.getNetValue() + p.getFinalBill().getVatCalulatedAmount());
                }
            }

            total += p.getFinalBill().getNetTotal();
            totalVat += p.getFinalBill().getVat();
            totalVatCalculatedValue += p.getFinalBill().getVatCalulatedAmount();
            paid += p.getTransPaidByPatient();
            creditPaid += p.getTransPaidByCompany();
        }
    }

    private double calPaidByPatient(PatientEncounter patientEncounter) {
        Map m = new HashMap();
        String sql = "select sum(b.netTotal) from Bill b "
                + " where b.patientEncounter=:pe"
                + " and b.billType=:btp "
                + " and b.createdAt <= :td ";

        m.put("btp", BillType.InwardPaymentBill);
        m.put("td", toDate);
        m.put("pe", patientEncounter);
        return getPeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private double calPaidByCompany(PatientEncounter patientEncounter) {
        Map m = new HashMap();
        String sql = "select sum(b.netValue) "
                + "  from BillItem b "
                + " where b.patientEncounter=:pe"
                + " and b.bill.billType=:btp "
                + " and b.bill.createdAt <= :td ";

        m.put("btp", BillType.CashRecieveBill);
        m.put("td", toDate);
        m.put("pe", patientEncounter);
        return getPeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void fillDischargeBookPaymentNotFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=false "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischarged();
    }

    public void fillDischargeBookPaymentFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischarged();
    }

    public void fillDischargeBookPaymentFinalizedNoChanges() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischargedNoChanges();

    }

    public void fillDischargeBookPaymentFinalizedNoChangesOnlyDue() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischargedNoChanges();

        List<PatientEncounter> list = patientEncounters;
        patientEncounters = null;
        patientEncounters = new ArrayList<>();
        setTotal(0);
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : list) {
            if (p.getFinalBill() == null) {
                continue;
            }
            p.setTransPaidByPatient(calPaidByPatient(p));
            p.setTransPaidByCompany(calPaidByCompany(p));

            double paidValue = p.getTransPaidByPatient() + p.getTransPaidByCompany();
            double dueValue = p.getFinalBill().getNetTotal() - paidValue;

            if (Math.round(dueValue) != 0) {
                setTotal(getTotal() + p.getFinalBill().getNetTotal());
                paid += p.getTransPaidByPatient();
                creditPaid += p.getTransPaidByCompany();

                patientEncounters.add(p);
            }

        }

    }

    public void makeListNull() {
        billItems = null;
    }

    public void updateOutSideBill(BillItem bi) {
        if (bi.getBill().isPaid()) {
            if (bi.getDescreption() == null || bi.getDescreption().equals("")) {
                JsfUtil.addErrorMessage("Please Enter Memo");
                return;
            }
            if (bi.getBill().getEditedAt() == null && bi.getBill().getEditor() == null) {
                bi.getBill().setEditor(getSessionController().getLoggedUser());
                bi.getBill().setEditedAt(new Date());
                getBillFacade().edit(bi.getBill());
                getBillItemFacade().edit(bi);
                JsfUtil.addSuccessMessage("This Bill Mark as Paid");
            } else {
                JsfUtil.addErrorMessage("Alreddy Mark as Paid");
            }
        } else {
            bi.getBill().setEditor(null);
            bi.getBill().setEditedAt(null);
            getBillFacade().edit(bi.getBill());
            bi.setDescreption("");
            getBillItemFacade().edit(bi);
            JsfUtil.addSuccessMessage("This Bill Mark as Un Paid");
        }
    }

    public void createOutSideBills() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b"
                + " where b.bill.billType = :billType "
                + " and b.retired=false "
                + " and b.bill.retired=false ";

        if (reportKeyWord.getString().equals("0")) {
            sql += " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
        }

        if (reportKeyWord.getString().equals("1")) {
            sql += " and b.bill.createdAt between :fromDate and :toDate ";
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
        }

        if (reportKeyWord.getString1().equals("0")) {
            sql += " and b.bill.paid!=true ";
        }

        if (reportKeyWord.getString1().equals("1")) {
            sql += " and b.bill.paid=true ";
        }

        if (institution != null) {
            sql += " and b.bill.fromInstitution=:ins ";
            temMap.put("ins", institution);
        }

        temMap.put("billType", BillType.InwardOutSideBill);

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (billItems == null) {
            billItems = new ArrayList<>();
        }

        total = 0.0;
        for (BillItem b : billItems) {
            total += b.getBill().getNetTotal();
        }

    }

//    public void createOutSideBillsByAddedDate() {
//        Date startTime = new Date();
//
//        makeListNull();
//        String sql;
//        Map temMap = new HashMap();
//        sql = "select b from BillItem b"
//                + " where b.bill.billType = :billType "
//                + " and b.bill.createdAt between :fromDate and :toDate "
//                + " and b.retired=false "
//                + " and b.bill.retired=false ";
//
//        if (institution != null) {
//            sql += " and b.bill.fromInstitution=:ins ";
//            temMap.put("ins", institution);
//        }
//
//        temMap.put("billType", BillType.InwardOutSideBill);
//        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);
//
//        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//
//        }
//
//        setTotal(0.0);
//        for (BillItem b : billItems) {
//            setTotal(getTotal() + b.getBill().getNetTotal());
//        }
//
//
//
//    }
//
//    public void createOutSideBillsByDischargeDate() {
//
//        Date startTime = new Date();
//
//        makeListNull();
//        String sql;
//        Map temMap = new HashMap();
//        sql = "select b from BillItem b"
//                + " where b.bill.billType = :billType "
//                + " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate "
//                + " and b.retired=false "
//                + " and b.bill.retired=false ";
//
//        if (institution != null) {
//            sql += " and b.bill.fromInstitution=:ins ";
//            temMap.put("ins", institution);
//        }
//
//        temMap.put("billType", BillType.InwardOutSideBill);
//        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);
//
//        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//
//        }
//
//        setTotal(0.0);
//        for (BillItem b : billItems) {
//            setTotal(getTotal() + b.getBill().getNetTotal());
//        }
//
//
//
//    }
    public void createPatientInvestigationsTableAll() {
        Date startTime = new Date();

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + "and pi.encounter is not null ";

        Map temMap = new HashMap();

        if (patientEncounter != null) {
            sql += "and pi.encounter=:en";
            temMap.put("en", patientEncounter);
        }

        if (getPatientCode() != null && !getPatientCode().trim().equals("")) {
            sql += " and  (((pi.billItem.bill.patientEncounter.patient.code) =:number ) or ((pi.billItem.bill..patientEncounter.patient.phn) =:number )) ";
            temMap.put("number", getPatientCode().trim().toUpperCase());
        }
//
        sql += " order by pi.id desc  ";
//

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public Admission getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(Admission patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    public BhtSummeryController getBhtSummeryController() {
        return bhtSummeryController;
    }

    public void setBhtSummeryController(BhtSummeryController bhtSummeryController) {
        this.bhtSummeryController = bhtSummeryController;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getPaid() {
        return paid;
    }

    public void setPaid(double paid) {
        this.paid = paid;
    }

    public List<IncomeByCategoryRecord> getIncomeByCategoryRecords() {
        return incomeByCategoryRecords;
    }

    public void setIncomeByCategoryRecords(List<IncomeByCategoryRecord> incomeByCategoryRecords) {
        this.incomeByCategoryRecords = incomeByCategoryRecords;
    }

    public void listBhtViceIncome() {
        Date startTime = new Date();

        String sql;
        individualBhtIncomeByCategoryRecord = new ArrayList<>();
        grossTotals = 0.0;
        netTotals = 0.0;
        discounts = 0.0;
        Map m = new HashMap();
        sql = "select pe, category,"
                + " bf.billItem.item.inwardChargeType, "
                + " sum(bf.feeGrossValue), sum(bf.feeDiscount),"
                + " sum(bf.feeValue) "
                + "from BillFee bf "
                + "join bf.billItem.item.category as category "
                + "join bf.bill.patientEncounter as pe "
                + "where "
                + "pe is not null and "
                + "bf.bill.billType=:billType and "
                + "pe.dateOfDischarge between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billType", BillType.InwardBill);

        sql = sql + " group by pe.id, category.name, bf.billItem.item.inwardChargeType ";
        sql = sql + " order by pe.id, bf.billItem.item.inwardChargeType, category.name";

//        Item item;
//        item.getInwardChargeType()
        List<Object[]> results = getPeFacade().findAggregates(sql, m, TemporalType.DATE);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        for (Object[] objs : results) {
            IndividualBhtIncomeByCategoryRecord ibr = new IndividualBhtIncomeByCategoryRecord();
            PatientEncounter pe = (PatientEncounter) objs[0];
            Category cat = (Category) objs[1];
            InwardChargeType ict = (InwardChargeType) objs[2];
            ibr.setBht(pe);
            ibr.setFinalBill(pe.getFinalBill());
            ibr.setCategory(cat);
            ibr.setInwardChargeType(ict);
            ibr.setGrossValue((Double) objs[3]);
            ibr.setDiscount((Double) objs[4]);
            ibr.setNetValue((Double) objs[5]);

            grossTotals = grossTotals + ibr.getGrossValue();
            discounts = discounts + ibr.getDiscount();
            netTotals = netTotals + ibr.getNetValue();

            individualBhtIncomeByCategoryRecord.add(ibr);
        }

    }

    public void listDischargedBhtIncomeByCategories() {
        String sql;
        incomeByCategoryRecords = new ArrayList<>();
        grossTotals = 0.0;
        netTotals = 0.0;
        discounts = 0.0;
        Map m = new HashMap();
        sql = "select bf.billItem.item.category, "
                + " sum(bf.feeDiscount),"
                + " sum(bf.feeMargin),"
                + " sum(bf.feeGrossValue),"
                + " sum(bf.feeValue)"
                + " from BillFee bf where"
                + " bf.bill.patientEncounter is not null"
                + " and bf.bill.patientEncounter.discharged=true ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billType", BillType.InwardBill);
        sql = sql + " and bf.bill.billType=:billType and"
                + " bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " group by bf.billItem.item.category order by bf.billItem.item.category.name";
        List<Object[]> results = getPeFacade().findAggregates(sql, m, TemporalType.DATE);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        for (Object[] objs : results) {

            IncomeByCategoryRecord ibr = new IncomeByCategoryRecord();
            ibr.setCategory((Category) objs[0]);
            ibr.setDiscount((double) objs[1]);
            ibr.setMatrix((double) objs[2]);
            ibr.setGrossAmount((double) objs[3]);
            ibr.setNetAmount((double) objs[4]);

            grossTotals = grossTotals + ibr.getGrossAmount();
            discounts = discounts + ibr.getDiscount();
            netTotals = netTotals + ibr.getNetAmount();

            incomeByCategoryRecords.add(ibr);

        }

    }

    public void fillProfessionalPaymentDone() {
        Date startTime = new Date();

        billedBill = createBilledBillProfessionalPaymentTableInwardAll(new BilledBill());
        cancelledBill = createCancelBillRefundBillProfessionalPaymentTableInwardAll(new CancelledBill());
        refundBill = createCancelBillRefundBillProfessionalPaymentTableInwardAll(new RefundBill());

        totalBilledBill = calTotalCreateBilledBillProfessionalPaymentTableInwardAll(new BilledBill());
        totalCancelledBill = calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(new CancelledBill());
        totalRefundBill = calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(new RefundBill());

    }

    public void fillProfessionalPaymentDoneOPD() {
        Date startTime = new Date();

        BillType[] bts = {BillType.OpdBill};
        List<BillType> billTypes = Arrays.asList(bts);
        billedBill = createProfessionalPaymentTable(new BilledBill(), BillType.PaymentBill, billTypes);
        cancelledBill = createProfessionalPaymentTable(new CancelledBill(), BillType.PaymentBill, null);
        refundBill = createProfessionalPaymentTable(new RefundBill(), BillType.PaymentBill, null);

        totalBilledBill = createProfessionalPaymentTableTotals(new BilledBill(), BillType.PaymentBill, billTypes);
        totalCancelledBill = createProfessionalPaymentTableTotals(new CancelledBill(), BillType.PaymentBill, null);
        totalRefundBill = createProfessionalPaymentTableTotals(new RefundBill(), BillType.PaymentBill, null);

    }

    List<BillItem> createBilledBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and (b.referenceBill.billType=:refType "
                    + " or b.referenceBill.billType=:refType2) ";
            temMap.put("at", admissionType);
        }

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    List<BillItem> createCancelBillRefundBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
//        temMap.put("refType", BillType.InwardBill);
//        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter is not null"
                + " and type(b.bill)=:bclass"
                //                + " and (b.bill.billedBill.referenceBill.billType=:refType "
                //                + " or b.bill.billedBill.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    List<BillItem> createProfessionalPaymentTable(Bill bill, BillType bt, List<BillType> billTypes) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bt", bt);
        String sql = " Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bt "
                + " and type(b.bill)=:bclass "
                + " and b.paidForBillFee.bill.patientEncounter is null "
                + " and b.createdAt between :fromDate and :toDate ";

        if (paymentMethod != null) {
            sql = sql + " and b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", institution);
        }
        if (billTypes != null) {
            sql += " and b.referenceBill.billType in :bts ";
            temMap.put("bts", billTypes);
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double createProfessionalPaymentTableTotals(Bill bill, BillType bt, List<BillType> billTypes) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bt", bt);
        String sql = " Select sum(b.netValue) FROM BillItem b  "
                + " where b.retired=false "
                + " and b.bill.billType=:bt "
                + " and type(b.bill)=:bclass"
                + " and b.paidForBillFee.bill.patientEncounter is null "
                + " and b.createdAt between :fromDate and :toDate ";

        if (paymentMethod != null) {
            sql = sql + " and b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        if (billTypes != null) {
            sql += " and b.referenceBill.billType in :bts ";
            temMap.put("bts", billTypes);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calTotalCreateBilledBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select sum(b.netValue) FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
//        temMap.put("refType", BillType.InwardBill);
//        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select sum(b.netValue) FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter is not null"
                + " and type(b.bill)=:bclass"
                //                + " and (b.bill.billedBill.referenceBill.billType=:refType "
                //                + " or b.bill.billedBill.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void listnerDeveloperCheckBox() {
        if (developers) {
            dischargeDate = false;
            bhtNo = false;
            paymentMethord = false;
            creditCompany = false;
            person = false;
            guardian = false;
            room = false;
            refDoctor = false;
            AddmitDetails = false;
            billedBy = false;
            finalBillTotal = false;
            paidByPatient = false;
            creditPaidAmount = false;
            dueAmount = false;
            calculatedAmount = false;
            differentAmount = false;
        } else {
            dischargeDate = true;
            bhtNo = true;
            paymentMethord = true;
            creditCompany = true;
            person = true;
            guardian = true;
            room = true;
            refDoctor = true;
            AddmitDetails = true;
            billedBy = true;
            finalBillTotal = true;
            paidByPatient = true;
            creditPaidAmount = true;
            dueAmount = true;
            calculatedAmount = true;
            differentAmount = true;
        }
    }

    public StreamedContent getAdmissionCountPdf() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            HtmlConverter.convertToPdf(buildAdmissionCountHtml(), pdfDoc, new ConverterProperties());
            byte[] bytes = out.toByteArray();
            return DefaultStreamedContent.builder()
                    .name("Admission_Count_Doctor_Wise.pdf")
                    .contentType("application/pdf")
                    .stream(() -> new ByteArrayInputStream(bytes))
                    .build();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(InwardReportController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "PDF generation failed", e);
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
            return null;
        }
    }

    private String buildAdmissionCountHtml() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");
        SimpleDateFormat sdtf = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss");
        String fromDate = fromYearStartDate != null ? sdf.format(fromYearStartDate) : "";
        String toDate = toYearEndDate != null ? sdf.format(toYearEndDate) : "";
        String institutionName = (sessionController != null && sessionController.getInstitution() != null)
                ? sessionController.getInstitution().getName() : "";
        String processedBy = admissionReportProcessedBy != null ? admissionReportProcessedBy : "";
        String processedAt = admissionReportProcessedAt != null ? sdtf.format(admissionReportProcessedAt) : "";
        String printedBy = "";
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            com.divudi.core.entity.WebUser u = sessionController.getLoggedUser();
            String personName = (u.getWebUserPerson() != null && u.getWebUserPerson().getName() != null
                    && !u.getWebUserPerson().getName().isBlank())
                    ? u.getWebUserPerson().getName() : null;
            printedBy = personName != null ? personName : u.getName();
        }
        String printedAt = sdtf.format(new Date());

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;font-size:10px;margin:8mm;}")
                .append("h1{text-align:center;font-size:15px;margin:0 0 2px 0;}")
                .append("h2{text-align:center;font-size:12px;margin:0 0 4px 0;}")
                .append(".dates{text-align:center;font-size:10px;margin-bottom:6px;}")
                .append("table{border-collapse:collapse;width:100%;font-size:9px;}")
                .append("th,td{border:1px solid #000;padding:2px 3px;}")
                .append("th{background-color:#c8c8c8;font-weight:bold;text-align:center;}")
                .append(".name{text-align:left;}")
                .append(".num{text-align:center;}")
                .append(".subtotal td{font-weight:bold;background-color:#ebebef;}")
                .append(".grandtotal td{font-weight:bold;background-color:#d0d0d0;font-size:10px;}")
                .append(".total{background-color:#a0a0a0;font-weight:bold;text-align:center;}")
                .append(".meta{font-size:9px;margin-bottom:6px;border-collapse:collapse;width:100%;}")
                .append(".meta td{border:none;padding:1px 4px;vertical-align:top;}")
                .append("</style></head><body>");

        sb.append("<h1>").append(escapeHtml(institutionName)).append("</h1>");
        sb.append("<h2>Doctor Wise Admission Count Report</h2>");
        sb.append("<div class='dates'>From: <b>").append(fromDate)
                .append("</b>&nbsp;&nbsp;&nbsp;To: <b>").append(toDate).append("</b></div>");

        sb.append("<table><thead><tr>")
                .append("<th class='name'>Doctor Name</th>")
                .append("<th class='name'>Speciality</th>");
        for (String m : new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}) {
            sb.append("<th>").append(m).append("</th>");
        }
        sb.append("<th>Total</th></tr></thead><tbody>");

        if (list != null) {
            for (InwardAdmissionDTO dto : list) {
                boolean sub = dto.isSubtotal();
                boolean grand = dto.isGrandTotal();
                sb.append("<tr class='").append(grand ? "grandtotal" : sub ? "subtotal" : "").append("'>");
                if (!sub && !grand) {
                    sb.append("<td class='name'>").append(escapeHtml(dto.getNameWithTitle())).append("</td>");
                    sb.append("<td class='name'>").append(escapeHtml(dto.getSpecialityName())).append("</td>");
                } else {
                    sb.append("<td colspan='2' class='name'>").append(escapeHtml(dto.getNameWithTitle())).append("</td>");
                }
                int[] months = {dto.getJanuary(), dto.getFebruary(), dto.getMarch(), dto.getApril(),
                    dto.getMay(), dto.getJune(), dto.getJuly(), dto.getAugust(),
                    dto.getSeptember(), dto.getOctober(), dto.getNovember(), dto.getDecember()};
                for (int v : months) {
                    sb.append("<td class='num'>").append(v > 0 ? v : "").append("</td>");
                }
                sb.append("<td class='total'>").append(dto.getTotalAdmissions()).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        sb.append("<table class='meta'>")
                .append("<tr>")
                .append("<td>")
                .append("<b>Processed By:</b> ").append(escapeHtml(processedBy)).append("<br/>")
                .append("<b>Processed At:</b> ").append(escapeHtml(processedAt))
                .append("</td>")
                .append("<td style='text-align:right;'>")
                .append("<b>Printed By:</b> ").append(escapeHtml(printedBy)).append("<br/>")
                .append("<b>Printed At:</b> ").append(escapeHtml(printedAt))
                .append("</td>")
                .append("</tr>")
                .append("</table>");

        // Append charts if captured from the browser
        String[] chartTitles = {
            "Specialty Wise Admission Trend",
            "Specialty Wise Admission Count",
            "Doctor Wise Admission Trend",
            "Doctor Wise Admission Count"
        };
        String[] chartImages = {specialtyLineChartImage, specialtyBarChartImage, doctorLineChartImage, doctorBarChartImage};
        boolean hasCharts = false;
        for (String img : chartImages) {
            if (img != null && img.startsWith("data:image/png;base64,")) {
                hasCharts = true;
                break;
            }
        }
        if (hasCharts) {
            sb.append("<div style='page-break-before:always; margin-top:10px;'>")
                    .append("<h2>Admission Count Visual Reports</h2>")
                    .append("<table style='border:none; width:100%;'>");
            int col = 0;
            for (int i = 0; i < chartImages.length; i++) {
                if (col % 2 == 0) {
                    if (col > 0) {
                        sb.append("</tr>");
                    }
                    sb.append("<tr>");
                }
                sb.append("<td style='border:none; width:50%; padding:5px; text-align:center; vertical-align:top;'>");
                String img = chartImages[i];
                if (img != null && img.startsWith("data:image/png;base64,")) {
                    sb.append("<div style='font-weight:bold; margin-bottom:4px;'>").append(chartTitles[i]).append("</div>");
                    sb.append("<img src='").append(img).append("' style='width:100%;'/>");
                }
                sb.append("</td>");
                col++;
            }
            sb.append("</tr></table></div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public StreamedContent getAdmissionCountExcelWithCharts() {
        if (list == null || list.isEmpty()) {
            JsfUtil.addErrorMessage("No admission data available to export.");
            return null;
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // --- Sheet 1: Data table ---
            XSSFSheet dataSheet = wb.createSheet("Admission Data");
            String[] headers = {"Doctor Name", "Speciality", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Total"};
            Row headerRow = dataSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (InwardAdmissionDTO dto : list) {
                Row row = dataSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getNameWithTitle() != null ? dto.getNameWithTitle() : "");
                row.createCell(1).setCellValue(!dto.isSubtotal() && !dto.isGrandTotal() && dto.getSpecialityName() != null ? dto.getSpecialityName() : "");
                row.createCell(2).setCellValue(dto.getJanuary());
                row.createCell(3).setCellValue(dto.getFebruary());
                row.createCell(4).setCellValue(dto.getMarch());
                row.createCell(5).setCellValue(dto.getApril());
                row.createCell(6).setCellValue(dto.getMay());
                row.createCell(7).setCellValue(dto.getJune());
                row.createCell(8).setCellValue(dto.getJuly());
                row.createCell(9).setCellValue(dto.getAugust());
                row.createCell(10).setCellValue(dto.getSeptember());
                row.createCell(11).setCellValue(dto.getOctober());
                row.createCell(12).setCellValue(dto.getNovember());
                row.createCell(13).setCellValue(dto.getDecember());
                row.createCell(14).setCellValue(dto.getTotalAdmissions());
            }

            // --- Charts appended below the data table on the same sheet ---
            XSSFDrawing drawing = dataSheet.createDrawingPatriarch();
            // Leave 2 blank rows as a gap after the last data row
            int chartStartRow = rowNum + 2;
            String[][] chartDefs = {
                {specialtyLineChartImage, "Specialty Wise Line Chart"},
                {specialtyBarChartImage, "Specialty Wise Bar Chart"},
                {doctorLineChartImage, "Doctor Wise Line Chart"},
                {doctorBarChartImage, "Doctor Wise Bar Chart"}
            };
            for (String[] def : chartDefs) {
                String b64 = def[0];
                String title = def[1];
                if (b64 != null && b64.startsWith("data:image/png;base64,")) {
                    b64 = b64.substring("data:image/png;base64,".length());
                    byte[] imgBytes = Base64.getDecoder().decode(b64);
                    int picIdx = wb.addPicture(imgBytes, Workbook.PICTURE_TYPE_PNG);
                    dataSheet.createRow(chartStartRow).createCell(0).setCellValue(title);
                    XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, 0, chartStartRow + 1, 15, chartStartRow + 31);
                    drawing.createPicture(anchor, picIdx);
                    chartStartRow += 33;
                }
            }

            wb.write(out);
            byte[] bytes = out.toByteArray();
            return DefaultStreamedContent.builder()
                    .name("Admission_Count_Doctor_Wise.xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> new ByteArrayInputStream(bytes))
                    .build();

        } catch (IOException e) {
            java.util.logging.Logger.getLogger(InwardReportController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Excel generation failed", e);
            JsfUtil.addErrorMessage("Failed to generate Excel: " + e.getMessage());
            return null;
        }
    }

    private static final int MAX_CHART_IMAGE_DATA_URL_LENGTH = 3000000;

    private String sanitizeChartImage(String image) {
        if (image == null || image.isBlank()) {
            return null;
        }
        if (!image.startsWith("data:image/png;base64,")) {
            return null;
        }
        if (image.length() > MAX_CHART_IMAGE_DATA_URL_LENGTH) {
            JsfUtil.addErrorMessage("Chart image is too large to export.");
            return null;
        }
        return image;
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

    public void setDepartment(Department deptartment) {
        this.department = deptartment;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Deprecated
    public List<AdmissionType> getAdmissionty() {
        admissionty = getAdmissionTypeFacade().findAll("name", true);
        return admissionty;
    }

    public void setAdmissionty(List<AdmissionType> admissionty) {
        this.admissionty = admissionty;
    }

    public AdmissionTypeFacade getAdmissionTypeFacade() {
        return admissionTypeFacade;
    }

    public void setAdmissionTypeFacade(AdmissionTypeFacade admissionTypeFacade) {
        this.admissionTypeFacade = admissionTypeFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.core.util.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public List<BillItem> getBilledBill() {
        return billedBill;
    }

    public void setBilledBill(List<BillItem> billedBill) {
        this.billedBill = billedBill;
    }

    public List<BillItem> getCancelledBill() {
        return cancelledBill;
    }

    public void setCancelledBill(List<BillItem> cancelledBill) {
        this.cancelledBill = cancelledBill;
    }

    public List<BillItem> getRefundBill() {
        return refundBill;
    }

    public void setRefundBill(List<BillItem> refundBill) {
        this.refundBill = refundBill;
    }

    public double getTotalBilledBill() {
        return totalBilledBill;
    }

    public void setTotalBilledBill(double totalBilledBill) {
        this.totalBilledBill = totalBilledBill;
    }

    public double getTotalCancelledBill() {
        return totalCancelledBill;
    }

    public void setTotalCancelledBill(double totalCancelledBill) {
        this.totalCancelledBill = totalCancelledBill;
    }

    public double getTotalRefundBill() {
        return totalRefundBill;
    }

    public void setTotalRefundBill(double totalRefundBill) {
        this.totalRefundBill = totalRefundBill;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.core.util.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounterFacade getPeFacade() {
        return peFacade;
    }

    public void setPeFacade(PatientEncounterFacade peFacade) {
        this.peFacade = peFacade;
    }

    public double getGrossTotals() {
        return grossTotals;
    }

    public void setGrossTotals(double grossTotals) {
        this.grossTotals = grossTotals;
    }

    public double getDiscounts() {
        return discounts;
    }

    public void setDiscounts(double discounts) {
        this.discounts = discounts;
    }

    public double getNetTotals() {
        return netTotals;
    }

    public void setNetTotals(double netTotals) {
        this.netTotals = netTotals;
    }

    public List<IndividualBhtIncomeByCategoryRecord> getIndividualBhtIncomeByCategoryRecord() {
        return individualBhtIncomeByCategoryRecord;
    }

    public void setIndividualBhtIncomeByCategoryRecord(List<IndividualBhtIncomeByCategoryRecord> individualBhtIncomeByCategoryRecord) {
        this.individualBhtIncomeByCategoryRecord = individualBhtIncomeByCategoryRecord;
    }

    public boolean isWithFooter() {
        return withFooter;
    }

    public void setWithFooter(boolean withFooter) {
        this.withFooter = withFooter;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<AdmissionType> getAdmissionTypes() {
        if (admissionTypes == null) {
            fillAdmissionTypes();
        }
        return admissionTypes;
    }

    public void setAdmissionTypes(List<AdmissionType> admissionTypes) {
        this.admissionTypes = admissionTypes;
    }

    private void fillAdmissionTypes() {
        String jpql = "select ad from AdmissionType ad "
                + "where ad.retired=false "
                + "order by ad.name";
        admissionTypes = admissionTypeFacade.findByJpql(jpql);
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public Date getFromYearStartDate() {
        if (fromYearStartDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            fromYearStartDate = cal.getTime();
        }
        return fromYearStartDate;
    }

    public void setFromYearStartDate(Date fromYearStartDate) {
        this.fromYearStartDate = fromYearStartDate;
    }

    public Date getToYearEndDate() {
        if (toYearEndDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            toYearEndDate = cal.getTime();
        }
        return toYearEndDate;
    }

    public void setToYearEndDate(Date toYearEndDate) {
        this.toYearEndDate = toYearEndDate;
    }

    public List<SurgeryCountDoctorWiseDTO> getBillList() {
        return billList;
    }

    public void setBillList(List<SurgeryCountDoctorWiseDTO> billList) {
        this.billList = billList;
    }

    public Speciality getCurrentSpeciality() {
        return currentSpeciality;
    }

    public void setCurrentSpeciality(Speciality currentSpeciality) {
        this.currentSpeciality = currentSpeciality;
    }

    public String getLineChartModel() {
        return lineChartModel;
    }

    public void setLineChartModel(String lineChartModel) {
        this.lineChartModel = lineChartModel;
    }

    public String getBarChartModel() {
        return barChartModel;
    }

    public void setBarChartModel(String barChartModel) {
        this.barChartModel = barChartModel;
    }

    public String getSpecialtyLineChartModel() {
        return specialtyLineChartModel;
    }

    public void setSpecialtyLineChartModel(String specialtyLineChartModel) {
        this.specialtyLineChartModel = specialtyLineChartModel;
    }

    public String getSpecialtyBarChartModel() {
        return specialtyBarChartModel;
    }

    public void setSpecialtyBarChartModel(String specialtyBarChartModel) {
        this.specialtyBarChartModel = specialtyBarChartModel;
    }

    public List<PaymentTypeAdmissionDTO> getPaymentTypeAdmissionCountList() {
        return paymentTypeAdmissionCountList;
    }

    public void setPaymentTypeAdmissionCountList(List<PaymentTypeAdmissionDTO> paymentTypeAdmissionCountList) {
        this.paymentTypeAdmissionCountList = paymentTypeAdmissionCountList;
    }

    public String getPaymentTypeLineChartModel() {
        return paymentTypeLineChartModel;
    }

    public String getPaymentTypeBarChartModel() {
        return paymentTypeBarChartModel;
    }

    public List<InwardAdmissionDTO> getList() {
        return list;
    }

    public void setList(List<InwardAdmissionDTO> list) {
        this.list = list;
    }

    public List<InwardIncomeDoctorSpecialtyDTO> getSpcDocIncomeBillList() {
        return spcDocIncomeBillList;
    }

    public void setSpcDocIncomeBillList(List<InwardIncomeDoctorSpecialtyDTO> spcDocIncomeBillList) {
        this.spcDocIncomeBillList = spcDocIncomeBillList;
    }

    public InwardIncomeDoctorSpecialtyDTO getTotalValuesSpcDocIncome() {
        return totalValuesSpcDocIncome;
    }

    public void setTotalValuesSpcDocIncome(InwardIncomeDoctorSpecialtyDTO totalValuesSpcDocIncome) {
        this.totalValuesSpcDocIncome = totalValuesSpcDocIncome;
    }

    public Doctor getCurrentDoctor() {
        return currentDoctor;
    }

    public void setCurrentDoctor(Doctor currentDoctor) {
        this.currentDoctor = currentDoctor;
    }

    public boolean getByDoctor() {
        return byDoctor;
    }

    public void setByDoctor(boolean dw) {
        this.byDoctor = dw;
    }

    public boolean isDemographicGeneratedByDoctor() {
        return demographicGeneratedByDoctor;
    }

    public void setDemographicGeneratedByDoctor(boolean dw) {
        this.demographicGeneratedByDoctor = dw;
    }

    public void setDemographicDataList(List<InwardAdmissionDemographicDataDTO> list) {
        this.demographicDataList = list;
    }

    public List<InwardAdmissionDemographicDataDTO> getDemographicDataList() {
        return demographicDataList;
    }

    public boolean getDemographicDataUnknownGender() {
        return demographicDataUnknownGender;
    }

    public void setDemographicDataUnknownGender(boolean demographicDataUnknownGender) {
        this.demographicDataUnknownGender = demographicDataUnknownGender;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public List<MonthlySurgeryCountDTO> getMonthlySurgeryCountList() {
        return monthlySurgeryCountList;
    }

    public void setMonthlySurgeryCountList(List<MonthlySurgeryCountDTO> monthlySurgeryCountList) {
        this.monthlySurgeryCountList = monthlySurgeryCountList;
    }

    public List<String> getSurgeryHeaders() {
        return surgeryHeaders;
    }

    public void setSurgeryHeaders(List<String> surgeryHeaders) {
        this.surgeryHeaders = surgeryHeaders;
    }

    public SurgeryType getSurgeryType() {
        return surgeryType;
    }

    public void setSurgeryType(SurgeryType surgeryType) {
        this.surgeryType = surgeryType;
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

    public void setInvoiceApprovedFromDate(Date invoiceApprovedFromDate) {
        this.invoiceApprovedFromDate = invoiceApprovedFromDate;
    }

    public Date getInvoiceApprovedToDate() {
        return invoiceApprovedToDate;
    }

    public void setInvoiceApprovedToDate(Date invoiceApprovedToDate) {
        this.invoiceApprovedToDate = invoiceApprovedToDate;
    }

    public Department getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(Department serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public Institution getSponsor() {
        return sponsor;
    }

    public void setSponsor(Institution sponsor) {
        this.sponsor = sponsor;
    }

    public String getDischargeType() {
        return dischargeType;
    }

    public void setDischargeType(String dischargeType) {
        this.dischargeType = dischargeType;
    }

    public String getPatientCategory() {
        return patientCategory;
    }

    public void setPatientCategory(String patientCategory) {
        this.patientCategory = patientCategory;
    }

    public AdmissionStatus getAdmissionStatus() {
        return admissionStatus;
    }

    public void setAdmissionStatus(AdmissionStatus admissionStatus) {
        this.admissionStatus = admissionStatus;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
    }

    public Staff getConsultant() {
        return consultant;
    }

    public void setConsultant(Staff consultant) {
        this.consultant = consultant;
    }

    public List<IpUnsettledInvoiceDTO> getUnsettledInvoicesList() {
        return unsettledInvoicesList;
    }

    public void setUnsettledInvoicesList(List<IpUnsettledInvoiceDTO> unsettledInvoicesList) {
        this.unsettledInvoicesList = unsettledInvoicesList;

    }

    public List<SurgeryCountSurgeryWiseDTO> getSurgeryCountSurgeryWiseList() {
        return surgeryCountSurgeryWiseList;
    }

    public void setSurgeryCountSurgeryWiseList(List<SurgeryCountSurgeryWiseDTO> surgeryCountSurgeryWiseList) {
        this.surgeryCountSurgeryWiseList = surgeryCountSurgeryWiseList;
    }

    public Item getSurgeryItem() {
        return surgeryItem;
    }

    public void setSurgeryItem(Item surgeryItem) {
        this.surgeryItem = surgeryItem;
    }

    public String getSurgeryWiseLineChartModel() {
        return surgeryWiseLineChartModel;
    }

    public void setSurgeryWiseLineChartModel(String surgeryWiseLineChartModel) {
        this.surgeryWiseLineChartModel = surgeryWiseLineChartModel;
    }

    public String getSurgeryWiseBarChartModel() {
        return surgeryWiseBarChartModel;
    }

    public void setSurgeryWiseBarChartModel(String surgeryWiseBarChartModel) {
        this.surgeryWiseBarChartModel = surgeryWiseBarChartModel;
    }

    public String getSpecialtyLineChartImage() {
        return specialtyLineChartImage;
    }

    public void setSpecialtyLineChartImage(String specialtyLineChartImage) {
        this.specialtyLineChartImage = sanitizeChartImage(specialtyLineChartImage);
    }

    public String getSpecialtyBarChartImage() {
        return specialtyBarChartImage;
    }

    public void setSpecialtyBarChartImage(String specialtyBarChartImage) {
        this.specialtyBarChartImage = sanitizeChartImage(specialtyBarChartImage);
    }

    public String getDoctorLineChartImage() {
        return doctorLineChartImage;
    }

    public void setDoctorLineChartImage(String doctorLineChartImage) {
        this.doctorLineChartImage = sanitizeChartImage(doctorLineChartImage);
    }

    public String getDoctorBarChartImage() {
        return doctorBarChartImage;
    }

    public void setDoctorBarChartImage(String doctorBarChartImage) {
        this.doctorBarChartImage = sanitizeChartImage(doctorBarChartImage);
    }

    public Date getAdmissionReportProcessedAt() {
        return admissionReportProcessedAt;
    }

    public void setAdmissionReportProcessedAt(Date admissionReportProcessedAt) {
        this.admissionReportProcessedAt = admissionReportProcessedAt;
    }

    public String getAdmissionReportProcessedBy() {
        return admissionReportProcessedBy;
    }

    public void setAdmissionReportProcessedBy(String admissionReportProcessedBy) {
        this.admissionReportProcessedBy = admissionReportProcessedBy;
    }

    public class IncomeByCategoryRecord {

        Category category;
        Category subCategory;
        double grossAmount;
        double discount;
        double matrix;
        double netAmount;

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public Category getSubCategory() {
            return subCategory;
        }

        public void setSubCategory(Category subCategory) {
            this.subCategory = subCategory;
        }

        public double getGrossAmount() {
            return grossAmount;
        }

        public void setGrossAmount(double grossAmount) {
            this.grossAmount = grossAmount;
        }

        public double getMatrix() {
            return matrix;
        }

        public void setMatrix(double matrix) {
            this.matrix = matrix;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getNetAmount() {
            return netAmount;
        }

        public void setNetAmount(double netAmount) {
            this.netAmount = netAmount;
        }

    }

    public class IndividualBhtIncomeByCategoryRecord {

        PatientEncounter bht;
        Bill finalBill;
        Category category;
        Category subCategory;
        InwardChargeType inwardChargeType;
        double grossValue;
        double discount;
        double inwardAddition;
        double netValue;

        public PatientEncounter getBht() {
            return bht;
        }

        public void setBht(PatientEncounter bht) {
            this.bht = bht;
        }

        public Bill getFinalBill() {
            return finalBill;
        }

        public void setFinalBill(Bill finalBill) {
            this.finalBill = finalBill;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public Category getSubCategory() {
            return subCategory;
        }

        public void setSubCategory(Category subCategory) {
            this.subCategory = subCategory;
        }

        public InwardChargeType getInwardChargeType() {
            return inwardChargeType;
        }

        public void setInwardChargeType(InwardChargeType inwardChargeType) {
            this.inwardChargeType = inwardChargeType;
        }

        public double getGrossValue() {
            return grossValue;
        }

        public void setGrossValue(double grossValue) {
            this.grossValue = grossValue;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getInwardAddition() {
            return inwardAddition;
        }

        public void setInwardAddition(double inwardAddition) {
            this.inwardAddition = inwardAddition;
        }

        public double getNetValue() {
            return netValue;
        }

        public void setNetValue(double netValue) {
            this.netValue = netValue;
        }

    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getNetPaid() {
        return netPaid;
    }

    public void setNetPaid(double netPaid) {
        this.netPaid = netPaid;
    }

    public double getCalTotal() {
        return calTotal;
    }

    public void setCalTotal(double calTotal) {
        this.calTotal = calTotal;
    }

    public double getCreditPaid() {
        return creditPaid;
    }

    public void setCreditPaid(double creditPaid) {
        this.creditPaid = creditPaid;
    }

    public double getCreditUsed() {
        return creditUsed;
    }

    public void setCreditUsed(double creditUsed) {
        this.creditUsed = creditUsed;
    }

    public InwardReportControllerBht getInwardReportControllerBht() {
        return inwardReportControllerBht;
    }

    public void setInwardReportControllerBht(InwardReportControllerBht inwardReportControllerBht) {
        this.inwardReportControllerBht = inwardReportControllerBht;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public boolean isDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(boolean dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public boolean isBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(boolean bhtNo) {
        this.bhtNo = bhtNo;
    }

    public boolean isPaymentMethord() {
        return paymentMethord;
    }

    public void setPaymentMethord(boolean paymentMethord) {
        this.paymentMethord = paymentMethord;
    }

    public boolean isCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(boolean creditCompany) {
        this.creditCompany = creditCompany;
    }

    public boolean isPerson() {
        return person;
    }

    public void setPerson(boolean person) {
        this.person = person;
    }

    public boolean isRoom() {
        return room;
    }

    public void setRoom(boolean room) {
        this.room = room;
    }

    public boolean isRefDoctor() {
        return refDoctor;
    }

    public void setRefDoctor(boolean refDoctor) {
        this.refDoctor = refDoctor;
    }

    public boolean isAddmitDetails() {
        return AddmitDetails;
    }

    public void setAddmitDetails(boolean AddmitDetails) {
        this.AddmitDetails = AddmitDetails;
    }

    public boolean isBilledBy() {
        return billedBy;
    }

    public void setBilledBy(boolean billedBy) {
        this.billedBy = billedBy;
    }

    public boolean isFinalBillTotal() {
        return finalBillTotal;
    }

    public void setFinalBillTotal(boolean finalBillTotal) {
        this.finalBillTotal = finalBillTotal;
    }

    public boolean isPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(boolean paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public boolean isCreditPaidAmount() {
        return creditPaidAmount;
    }

    public void setCreditPaidAmount(boolean creditPaidAmount) {
        this.creditPaidAmount = creditPaidAmount;
    }

    public boolean isDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(boolean dueAmount) {
        this.dueAmount = dueAmount;
    }

    public boolean isCalculatedAmount() {
        return calculatedAmount;
    }

    public void setCalculatedAmount(boolean calculatedAmount) {
        this.calculatedAmount = calculatedAmount;
    }

    public boolean isDifferentAmount() {
        return differentAmount;
    }

    public void setDifferentAmount(boolean differentAmount) {
        this.differentAmount = differentAmount;
    }

    public boolean isGuardian() {
        return guardian;
    }

    public void setGuardian(boolean guardian) {
        this.guardian = guardian;
    }

    public boolean isDevelopers() {
        return developers;
    }

    public void setDevelopers(boolean developers) {
        this.developers = developers;
    }

    public boolean isWithoutCancelBHT() {
        return withoutCancelBHT;
    }

    public void setWithoutCancelBHT(boolean withoutCancelBHT) {
        this.withoutCancelBHT = withoutCancelBHT;
    }

    public String getInvoceNo() {
        return invoceNo;
    }

    public void setInvoceNo(String invoceNo) {
        this.invoceNo = invoceNo;
    }

    public double getTotalVat() {
        return totalVat;
    }

    public void setTotalVat(double totalVat) {
        this.totalVat = totalVat;
    }

    public double getTotalVatCalculatedValue() {
        return totalVatCalculatedValue;
    }

    public void setTotalVatCalculatedValue(double totalVatCalculatedValue) {
        this.totalVatCalculatedValue = totalVatCalculatedValue;
    }

    public String getVatRegNo() {
        return vatRegNo;
    }

    public void setVatRegNo(String vatRegNo) {
        this.vatRegNo = vatRegNo;
    }

}
