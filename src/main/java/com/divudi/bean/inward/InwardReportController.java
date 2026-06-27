/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.EncounterType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PaymentType;
import com.divudi.core.data.Sex;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.ServiceType;
import com.divudi.core.data.dto.InwardAdmissionDTO;
import com.divudi.core.data.dto.InwardAdmissionDemographicDataDTO;
import com.divudi.core.data.dto.InwardIncomeDoctorSpecialtyDTO;
import com.divudi.core.data.dto.IpIncomeCategoryWiseRowDTO;
import com.divudi.core.data.dto.MonthServiceCountDTO;
import com.divudi.core.data.dto.MonthlySurgeryCountDTO;
import com.divudi.core.data.dto.AdmissionCategoryWiseAdmissionDTO;
import com.divudi.core.data.dto.IpUnsettledInvoiceDTO;
import com.divudi.core.data.dto.PaymentTypeAdmissionDTO;
import com.divudi.core.data.dto.SurgeryCountDoctorWiseDTO;
import com.divudi.core.data.dto.SurgeryCountSurgeryWiseDTO;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.inward.AdmissionStatus;
import static com.divudi.core.data.inward.AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;
import static com.divudi.core.data.inward.AdmissionStatus.ANY_STATUS;
import static com.divudi.core.data.inward.AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
import static com.divudi.core.data.inward.AdmissionStatus.DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED;
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
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.common.EnumController;
import com.divudi.core.data.dto.AdmissionDischargeDTO;
import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.data.dto.RoomCategoryOccupancyDTO;
import com.divudi.core.data.dto.RoomOccupancyRowDTO;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.data.dto.SurgeryCostEstimationDTO;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.entity.inward.EncounterComponent;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.data.inward.TheatreOccupancyStatus;
import com.divudi.core.data.inward.PatientEncounterComponentType;
import com.divudi.core.facade.EncounterComponentFacade;
import com.divudi.core.facade.PatientTransferRequestFacade;
import java.io.Serializable;
import java.text.DecimalFormat;
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
import java.util.TreeMap;

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
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import java.util.stream.Collectors;

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
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.lowagie.text.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

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
    @EJB
    PatientRoomFacade patientRoomFacade;
    @EJB
    EncounterComponentFacade encounterComponentFacade;
    @EJB
    PatientTransferRequestFacade patientTransferRequestFacade;

    @Inject
    SessionController sessionController;
    @Inject
    InwardReportControllerBht inwardReportControllerBht;
    @Inject
    BhtSummeryController bhtSummeryController;
    @Inject
    InwardBeanController inwardBeanController;
    @Inject
    EnumController enumController;;
    @Inject
    RoomCategoryController roomCategoryController;

    PaymentMethod paymentMethod;
    AdmissionType admissionType;
    Institution institution;
    Institution site;
    Department department;
    private String dateBasis = "createdAt";
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

    // Surgery Cost Estimation Report
    private List<SurgeryCostEstimationDTO> surgeryCostEstimationList;
    private PatientEncounterDto selectedPatient;
    private Staff selectedAdmitDoctor;
    private Staff selectedSurgeon;
    private Staff selectedAssistantSurgeon;
    private RoomFacilityCharge selectedOtRoom;
    private TheatreOccupancyStatus selectedSurgeryStatus;
    private String surgeryCostEstimationReportType;

    // Surgery Survey Report
    private String reportType;
    private String visitType;
    private String paymentType;
    private Category category;
    private List<RoomCategory> roomCategories;
    private boolean withProfessionalFee;
    private double ipIncomeTotalSponsorPay;
    private double ipIncomeTotalPatientPay;
    private ReportTemplateRowBundle bundle;
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
    private List<AdmissionCategoryWiseAdmissionDTO> admissionCategoryWiseAdmissionList;
    private List<AdmissionDischargeDTO> admissionDischargesList;
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
    private double ipIncomeCashTotal;
    private double ipIncomeCreditTotal;

    private List<Map<String, Object>> ipIncomeBillDiscounts;
    private double ipIncomeTotalBillDiscount;

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }

    public List<SurgeryCostEstimationDTO> getSurgeryCostEstimationList() {
        return surgeryCostEstimationList;
    }

    public void setSurgeryCostEstimationList(List<SurgeryCostEstimationDTO> surgeryCostEstimationList) {
        this.surgeryCostEstimationList = surgeryCostEstimationList;
    }

    public PatientEncounterDto getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatient(PatientEncounterDto selectedPatient) {
        this.selectedPatient = selectedPatient;
    }

    public Staff getSelectedAdmitDoctor() {
        return selectedAdmitDoctor;
    }

    public void setSelectedAdmitDoctor(Staff selectedAdmitDoctor) {
        this.selectedAdmitDoctor = selectedAdmitDoctor;
    }

    public Staff getSelectedSurgeon() {
        return selectedSurgeon;
    }

    public void setSelectedSurgeon(Staff selectedSurgeon) {
        this.selectedSurgeon = selectedSurgeon;
    }

    public Staff getSelectedAssistantSurgeon() {
        return selectedAssistantSurgeon;
    }

    public void setSelectedAssistantSurgeon(Staff selectedAssistantSurgeon) {
        this.selectedAssistantSurgeon = selectedAssistantSurgeon;
    }

    public RoomFacilityCharge getSelectedOtRoom() {
        return selectedOtRoom;
    }

    public void setSelectedOtRoom(RoomFacilityCharge selectedOtRoom) {
        this.selectedOtRoom = selectedOtRoom;
    }

    public TheatreOccupancyStatus getSelectedSurgeryStatus() {
        return selectedSurgeryStatus;
    }

    public void setSelectedSurgeryStatus(TheatreOccupancyStatus selectedSurgeryStatus) {
        this.selectedSurgeryStatus = selectedSurgeryStatus;
    }

    public String getSurgeryCostEstimationReportType() {
        return surgeryCostEstimationReportType;
    }

    public void setSurgeryCostEstimationReportType(String surgeryCostEstimationReportType) {
        this.surgeryCostEstimationReportType = surgeryCostEstimationReportType;
    }
    double netTotal;
    double netPaid;

    private Map<Long, Long> categoryAvailableRoomsCache;
    private List<RoomOccupancyRowDTO> roomOccupancyList;
    private RoomOccupancyRowDTO roomOccupancyGrandTotal;
    private String roomOccupancyRatioMode = RoomCategoryOccupancyDTO.RATIO_MODE_AGGREGATED_ROOM_UTILIZATION;
    private List<RoomCategory> allRoomCategories;
    private List<RoomCategory> selectedRoomCategories;

    public void processRoomOccupancyReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select From and To dates.");
            return;
        }

        allRoomCategories = new ArrayList<>(getRoomOccupancyCategoriesForReport());

        roomOccupancyList = new ArrayList<>();
        Map<Integer, Map<Integer, RoomOccupancyRowDTO>> grid = new TreeMap<>();

        loadAdmissionsIntoGrid(grid);
        loadRoomCategoryMetricsIntoGrid(grid);

        roomOccupancyGrandTotal = new RoomOccupancyRowDTO(null, null);
        roomOccupancyGrandTotal.setGrandTotal(true);

        for (Map.Entry<Integer, Map<Integer, RoomOccupancyRowDTO>> yearEntry : grid.entrySet()) {
            for (Map.Entry<Integer, RoomOccupancyRowDTO> monthEntry : yearEntry.getValue().entrySet()) {
                RoomOccupancyRowDTO row = monthEntry.getValue();

                row.ensureCategories(allRoomCategories);
                applyAvailableCounts(row);

                int daysInMonth = java.time.YearMonth.of(
                        yearEntry.getKey(), monthEntry.getKey()).lengthOfMonth();
                calculateRowDerived(row, daysInMonth);

                roomOccupancyList.add(row);
                roomOccupancyGrandTotal.merge(row);
            }
        }

        roomOccupancyGrandTotal.ensureCategories(allRoomCategories);
        applyAvailableCounts(roomOccupancyGrandTotal);
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                fromDate.toInstant(), toDate.toInstant()) + 1;
        calculateRowDerived(roomOccupancyGrandTotal, totalDays);
    }

    public void downloadRoomOccupancyExcel() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select From and To dates.");
            return;
        }

        if (roomOccupancyList == null || roomOccupancyList.isEmpty()) {
            processRoomOccupancyReport();
        }

        if (roomOccupancyList == null || roomOccupancyList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Room Occupancy");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");

            List<RoomCategory> exportCategories = allRoomCategories != null
                    ? allRoomCategories : new ArrayList<>();
            int lastColumn = 2 + (exportCategories.size() * 4);

            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle infoLabelStyle = workbook.createCellStyle();
            XSSFFont infoLabelFont = workbook.createFont();
            infoLabelFont.setBold(true);
            infoLabelStyle.setFont(infoLabelFont);

            XSSFCellStyle headerStyle = createRoomOccupancyHeaderStyle(workbook);
            XSSFCellStyle subHeaderStyle = createRoomOccupancySubHeaderStyle(workbook);
            XSSFCellStyle textStyle = createRoomOccupancyTextStyle(workbook);
            XSSFCellStyle integerStyle = createRoomOccupancyNumberStyle(workbook, "#,##0");
            XSSFCellStyle decimalStyle = createRoomOccupancyNumberStyle(workbook, "0.00");
            XSSFCellStyle totalTextStyle = createRoomOccupancyTotalStyle(workbook, false, null);
            XSSFCellStyle totalIntegerStyle = createRoomOccupancyTotalStyle(workbook, true, "#,##0");
            XSSFCellStyle totalDecimalStyle = createRoomOccupancyTotalStyle(workbook, true, "0.00");

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(24);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Room Occupancy Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));

            String[][] infoRows = {
                {"From Date:", sdf.format(fromDate)},
                {"To Date:", sdf.format(toDate)},
                {"Institution:", institution != null ? institution.getName() : "All"},
                {"Site:", site != null ? site.getName() : "All"},
                {"Department:", department != null ? department.getName() : "All"},
                {"Ratio Mode:", formatRoomOccupancyRatioMode()},
                {"Generated:", sdf.format(new Date())}
            };
            for (String[] info : infoRows) {
                Row infoRow = sheet.createRow(rowIndex++);
                Cell labelCell = infoRow.createCell(0);
                labelCell.setCellValue(info[0]);
                labelCell.setCellStyle(infoLabelStyle);
                infoRow.createCell(1).setCellValue(info[1]);
            }

            rowIndex++;

            int headerStartRow = rowIndex;
            Row groupHeaderRow = sheet.createRow(rowIndex++);
            Row categoryHeaderRow = sheet.createRow(rowIndex++);
            Row metricHeaderRow = sheet.createRow(rowIndex++);

            createMergedHeaderCell(sheet, groupHeaderRow, headerStartRow, headerStartRow + 2, 0, 0, "Year", headerStyle);
            createMergedHeaderCell(sheet, groupHeaderRow, headerStartRow, headerStartRow + 2, 1, 1, "Month", headerStyle);
            createMergedHeaderCell(sheet, groupHeaderRow, headerStartRow, headerStartRow + 2, 2, 2, "No of Admission", headerStyle);

            if (!exportCategories.isEmpty()) {
                createMergedHeaderCell(sheet, groupHeaderRow, headerStartRow, headerStartRow, 3, lastColumn, "Rooms", headerStyle);
            }

            int column = 3;
            for (RoomCategory category : exportCategories) {
                createMergedHeaderCell(sheet, categoryHeaderRow, headerStartRow + 1, headerStartRow + 1,
                        column, column + 3, category != null ? category.getName() : "", subHeaderStyle);
                String[] metricHeaders = {"Rooms", "Days", "Ratio", "Avg"};
                for (String metricHeader : metricHeaders) {
                    Cell cell = metricHeaderRow.createCell(column++);
                    cell.setCellValue(metricHeader);
                    cell.setCellStyle(subHeaderStyle);
                }
            }

            for (RoomOccupancyRowDTO rowDto : roomOccupancyList) {
                Row dataRow = sheet.createRow(rowIndex++);
                writeRoomOccupancyRow(dataRow, rowDto, exportCategories, textStyle, integerStyle, decimalStyle);
            }

            if (roomOccupancyGrandTotal != null) {
                Row totalRow = sheet.createRow(rowIndex++);
                writeRoomOccupancyRow(totalRow, roomOccupancyGrandTotal, exportCategories,
                        totalTextStyle, totalIntegerStyle, totalDecimalStyle);
            }

            sheet.createFreezePane(3, headerStartRow + 3);
            sheet.setAutoFilter(new CellRangeAddress(headerStartRow + 2, rowIndex - 1, 0, lastColumn));
            sheet.setColumnWidth(0, 2500);
            sheet.setColumnWidth(1, 2500);
            sheet.setColumnWidth(2, 4200);
            for (int i = 3; i <= lastColumn; i++) {
                sheet.setColumnWidth(i, 3000);
            }

            workbook.write(baos);
            byte[] excelBytes = baos.toByteArray();

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            externalContext.setResponseContentLength(excelBytes.length);
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"Room_Occupancy_" + fileDateFormat.format(new Date()) + ".xlsx\"");

            OutputStream out = externalContext.getResponseOutputStream();
            out.write(excelBytes);
            out.flush();
            facesContext.responseComplete();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating Excel: " + e.getMessage());
        }
    }

    private void writeRoomOccupancyRow(Row dataRow, RoomOccupancyRowDTO rowDto, List<RoomCategory> categories,
            CellStyle textStyle, CellStyle integerStyle, CellStyle decimalStyle) {
        boolean grandTotalRow = rowDto != null && rowDto.isGrandTotal();
        Cell yearCell = dataRow.createCell(0);
        if (grandTotalRow) {
            yearCell.setCellValue("Grand Total");
        } else if (rowDto != null && rowDto.getYear() != null) {
            yearCell.setCellValue(rowDto.getYear());
        } else {
            yearCell.setCellValue("");
        }
        yearCell.setCellStyle(textStyle);

        Cell monthCell = dataRow.createCell(1);
        monthCell.setCellValue(grandTotalRow || rowDto == null ? "" : rowDto.getMonthName());
        monthCell.setCellStyle(textStyle);

        Cell admissionsCell = dataRow.createCell(2);
        admissionsCell.setCellValue(rowDto != null && rowDto.getNumberOfAdmissions() != null
                ? rowDto.getNumberOfAdmissions() : 0L);
        admissionsCell.setCellStyle(integerStyle);

        int column = 3;
        for (RoomCategory category : categories) {
            RoomCategoryOccupancyDTO metric = rowDto != null ? rowDto.metricFor(category) : new RoomCategoryOccupancyDTO();
            createLongCell(dataRow, column++, metric.getNumberOfRooms(), integerStyle);
            createLongCell(dataRow, column++, metric.getNumberOfDays(), integerStyle);
            createDoubleCell(dataRow, column++, metric.getRatio(), decimalStyle);
            createDoubleCell(dataRow, column++, metric.getAvg(), decimalStyle);
        }
    }

    private void createLongCell(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : 0L);
        cell.setCellStyle(style);
    }

    private void createDoubleCell(Row row, int column, Double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : 0.0);
        cell.setCellStyle(style);
    }

    private void createMergedHeaderCell(Sheet sheet, Row row, int firstRow, int lastRow,
            int firstColumn, int lastColumn, String value, CellStyle style) {
        Cell cell = row.createCell(firstColumn);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        if (firstRow != lastRow || firstColumn != lastColumn) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
        }
        for (int column = firstColumn + 1; column <= lastColumn; column++) {
            row.createCell(column).setCellStyle(style);
        }
    }

    private XSSFCellStyle createRoomOccupancyHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyRoomOccupancyBorders(style);
        return style;
    }

    private XSSFCellStyle createRoomOccupancySubHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyRoomOccupancyBorders(style);
        return style;
    }

    private XSSFCellStyle createRoomOccupancyTextStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        applyRoomOccupancyBorders(style);
        return style;
    }

    private XSSFCellStyle createRoomOccupancyNumberStyle(XSSFWorkbook workbook, String format) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        applyRoomOccupancyBorders(style);
        return style;
    }

    private XSSFCellStyle createRoomOccupancyTotalStyle(XSSFWorkbook workbook, boolean numeric, String format) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(numeric ? HorizontalAlignment.RIGHT : HorizontalAlignment.LEFT);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (format != null) {
            style.setDataFormat(workbook.createDataFormat().getFormat(format));
        }
        applyRoomOccupancyBorders(style);
        return style;
    }

    private void applyRoomOccupancyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String formatRoomOccupancyRatioMode() {
        if (RoomCategoryOccupancyDTO.RATIO_MODE_PATIENT_CATEGORY_DURATION.equals(roomOccupancyRatioMode)) {
            return "Patient Category Duration";
        }
        return "Aggregated Room Utilization";
    }

    private List<RoomCategory> getRoomOccupancyCategoriesForReport() {
        if (selectedRoomCategories != null && !selectedRoomCategories.isEmpty()) {
            return selectedRoomCategories;
        }
        List<RoomCategory> categories = roomCategoryController != null ? roomCategoryController.getItems() : null;
        return categories != null ? categories : new ArrayList<>();
    }

    public void downloadRoomOccupancyPdf() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select From and To dates.");
            return;
        }

        if (roomOccupancyList == null || roomOccupancyList.isEmpty()) {
            processRoomOccupancyReport();
        }

        if (roomOccupancyList == null || roomOccupancyList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(com.lowagie.text.PageSize.A3.rotate(), 18, 18, 24, 18);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            List<RoomCategory> exportCategories = getRoomOccupancyCategoriesForReport();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font infoLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            com.lowagie.text.Font infoValueFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            int totalColumns = 3 + (exportCategories.size() * 4);
            float tableFontSize = totalColumns > 35 ? 5f : totalColumns > 25 ? 6f : 7f;
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, tableFontSize,
                    com.lowagie.text.Font.NORMAL, java.awt.Color.WHITE);
            com.lowagie.text.Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, tableFontSize);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, tableFontSize);
            com.lowagie.text.Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, tableFontSize);

            Paragraph title = new Paragraph("Room Occupancy Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(45);
            infoTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            infoTable.setWidths(new float[]{1.4f, 3f});
            infoTable.setSpacingAfter(10);

            addRoomOccupancyPdfInfoRow(infoTable, "From Date:", sdf.format(fromDate), infoLabelFont, infoValueFont);
            addRoomOccupancyPdfInfoRow(infoTable, "To Date:", sdf.format(toDate), infoLabelFont, infoValueFont);
            addRoomOccupancyPdfInfoRow(infoTable, "Institution:", institution != null ? institution.getName() : "All", infoLabelFont, infoValueFont);
            addRoomOccupancyPdfInfoRow(infoTable, "Site:", site != null ? site.getName() : "All", infoLabelFont, infoValueFont);
            addRoomOccupancyPdfInfoRow(infoTable, "Department:", department != null ? department.getName() : "All", infoLabelFont, infoValueFont);
            addRoomOccupancyPdfInfoRow(infoTable, "Ratio Mode:", formatRoomOccupancyRatioMode(), infoLabelFont, infoValueFont);
            addRoomOccupancyPdfInfoRow(infoTable, "Generated:", sdf.format(new Date()), infoLabelFont, infoValueFont);
            document.add(infoTable);

            PdfPTable table = new PdfPTable(totalColumns);
            table.setWidthPercentage(100);
            table.setWidths(buildRoomOccupancyPdfColumnWidths(exportCategories.size()));
            table.setHeaderRows(3);
            table.setSpacingBefore(5);

            java.awt.Color headerBg = new java.awt.Color(41, 128, 185);
            java.awt.Color subHeaderBg = new java.awt.Color(224, 224, 224);
            java.awt.Color totalBg = new java.awt.Color(255, 242, 204);
            java.awt.Color oddRowBg = new java.awt.Color(248, 249, 250);

            addRoomOccupancyPdfHeaderCell(table, "Year", headerFont, headerBg, 3, 1);
            addRoomOccupancyPdfHeaderCell(table, "Month", headerFont, headerBg, 3, 1);
            addRoomOccupancyPdfHeaderCell(table, "No of Admission", headerFont, headerBg, 3, 1);
            if (!exportCategories.isEmpty()) {
                addRoomOccupancyPdfHeaderCell(table, "Rooms", headerFont, headerBg, 1, exportCategories.size() * 4);
            }

            for (RoomCategory category : exportCategories) {
                addRoomOccupancyPdfHeaderCell(table, category != null ? category.getName() : "",
                        subHeaderFont, subHeaderBg, 1, 4);
            }

            for (int i = 0; i < exportCategories.size(); i++) {
                addRoomOccupancyPdfHeaderCell(table, "Rooms", subHeaderFont, subHeaderBg, 1, 1);
                addRoomOccupancyPdfHeaderCell(table, "Days", subHeaderFont, subHeaderBg, 1, 1);
                addRoomOccupancyPdfHeaderCell(table, "Ratio", subHeaderFont, subHeaderBg, 1, 1);
                addRoomOccupancyPdfHeaderCell(table, "Avg", subHeaderFont, subHeaderBg, 1, 1);
            }

            int rowIndex = 0;
            for (RoomOccupancyRowDTO row : roomOccupancyList) {
                java.awt.Color rowBg = rowIndex % 2 == 0 ? null : oddRowBg;
                addRoomOccupancyPdfRow(table, row, exportCategories, normalFont, rowBg);
                rowIndex++;
            }

            if (roomOccupancyGrandTotal != null) {
                addRoomOccupancyPdfRow(table, roomOccupancyGrandTotal, exportCategories, totalFont, totalBg);
            }

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseContentLength(pdfBytes.length);
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"Room_Occupancy_" + fileDateFormat.format(new Date()) + ".pdf\"");

            OutputStream out = externalContext.getResponseOutputStream();
            out.write(pdfBytes);
            out.flush();
            facesContext.responseComplete();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
        }
    }

    private void addRoomOccupancyPdfInfoRow(PdfPTable table, String label, String value,
            com.lowagie.text.Font labelFont, com.lowagie.text.Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        labelCell.setPadding(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        valueCell.setPadding(2);
        table.addCell(valueCell);
    }

    private float[] buildRoomOccupancyPdfColumnWidths(int categoryCount) {
        float[] widths = new float[3 + (categoryCount * 4)];
        widths[0] = 1.0f;
        widths[1] = 1.0f;
        widths[2] = 1.6f;
        int index = 3;
        for (int i = 0; i < categoryCount; i++) {
            widths[index++] = 1.0f;
            widths[index++] = 1.0f;
            widths[index++] = 1.0f;
            widths[index++] = 1.0f;
        }
        return widths;
    }

    private void addRoomOccupancyPdfHeaderCell(PdfPTable table, String value, com.lowagie.text.Font font,
            java.awt.Color backgroundColor, int rowspan, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        cell.setRowspan(rowspan);
        cell.setColspan(colspan);
        table.addCell(cell);
    }

    private void addRoomOccupancyPdfRow(PdfPTable table, RoomOccupancyRowDTO row,
            List<RoomCategory> categories, com.lowagie.text.Font font, java.awt.Color backgroundColor) {
        boolean grandTotalRow = row != null && row.isGrandTotal();
        addRoomOccupancyPdfCell(table, grandTotalRow ? "Grand Total" : row != null && row.getYear() != null ? row.getYear().toString() : "",
                font, backgroundColor, Element.ALIGN_LEFT);
        addRoomOccupancyPdfCell(table, grandTotalRow || row == null ? "" : row.getMonthName(),
                font, backgroundColor, Element.ALIGN_LEFT);
        addRoomOccupancyPdfCell(table, formatRoomOccupancyLong(row != null ? row.getNumberOfAdmissions() : null),
                font, backgroundColor, Element.ALIGN_RIGHT);

        for (RoomCategory category : categories) {
            RoomCategoryOccupancyDTO metric = row != null ? row.metricFor(category) : new RoomCategoryOccupancyDTO();
            addRoomOccupancyPdfCell(table, formatRoomOccupancyLong(metric.getNumberOfRooms()), font, backgroundColor, Element.ALIGN_RIGHT);
            addRoomOccupancyPdfCell(table, formatRoomOccupancyLong(metric.getNumberOfDays()), font, backgroundColor, Element.ALIGN_RIGHT);
            addRoomOccupancyPdfCell(table, formatRoomOccupancyDouble(metric.getRatio()), font, backgroundColor, Element.ALIGN_RIGHT);
            addRoomOccupancyPdfCell(table, formatRoomOccupancyDouble(metric.getAvg()), font, backgroundColor, Element.ALIGN_RIGHT);
        }
    }

    private void addRoomOccupancyPdfCell(PdfPTable table, String value, com.lowagie.text.Font font,
            java.awt.Color backgroundColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
        if (backgroundColor != null) {
            cell.setBackgroundColor(backgroundColor);
        }
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);
        table.addCell(cell);
    }

    private String formatRoomOccupancyLong(Long value) {
        return String.format("%,d", value != null ? value : 0L);
    }

    private String formatRoomOccupancyDouble(Double value) {
        return String.format("%.2f", value != null ? value : 0.0);
    }

// LOAD: admissions per year/month
    private void loadAdmissionsIntoGrid(Map<Integer, Map<Integer, RoomOccupancyRowDTO>> grid) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT FUNCTION('YEAR', pe.dateOfAdmission), ")
                .append("FUNCTION('MONTH', pe.dateOfAdmission), ")
                .append("COUNT(pe.id) ")
                .append("FROM PatientEncounter pe ")
                .append("WHERE pe.retired = false ")
                .append("AND pe.dateOfAdmission BETWEEN :fd AND :td ");

        params.put("fd", fromDate);
        params.put("td", toDate);

        if (institution != null) {
            jpql.append(" and pe.institution = :ins ");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and pe.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and pe.department = :dep ");
            params.put("dep", department);
        }
        if (selectedRoomCategories != null && !selectedRoomCategories.isEmpty()) {
            jpql.append("AND pe.currentPatientRoom.roomFacilityCharge.roomCategory IN :cat ");
            params.put("cat", selectedRoomCategories);
        }

        jpql.append("GROUP BY FUNCTION('YEAR', pe.dateOfAdmission), FUNCTION('MONTH', pe.dateOfAdmission) ")
                .append("ORDER BY 1, 2");

        List<Object[]> rows = peFacade.findObjectsArrayByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP);
        for (Object[] r : rows) {
            RoomOccupancyRowDTO row = getOrCreateRow(grid, toInteger(r[0]), toInteger(r[1]));
            row.addAdmissions(toLong(r[2]));
        }
    }

    private void loadRoomCategoryMetricsIntoGrid(Map<Integer, Map<Integer, RoomOccupancyRowDTO>> grid) {
        categoryAvailableRoomsCache = new HashMap<>();

        List<RoomCategory> reportCategories = allRoomCategories != null
                ? allRoomCategories : getRoomOccupancyCategoriesForReport();
        if (reportCategories.isEmpty()) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT FUNCTION('YEAR', pr.admittedAt), ")
                .append("FUNCTION('MONTH', pr.admittedAt), ")
                .append("rfc.roomCategory, ")
                .append("pr.patientEncounter.id, ")
                .append("COUNT(DISTINCT pr.id), ")
                .append("SUM(FUNCTION('DATEDIFF', COALESCE(pr.dischargedAt, CURRENT_TIMESTAMP), pr.admittedAt) + 1), ")
                .append("MAX(FUNCTION('DATEDIFF', COALESCE(pr.patientEncounter.dateOfDischarge, CURRENT_TIMESTAMP), pr.patientEncounter.dateOfAdmission) + 1), ")
                .append("(SELECT COUNT(DISTINCT availableRfc.id) ")
                .append("FROM RoomFacilityCharge availableRfc ")
                .append("WHERE availableRfc.retired = false ")
                .append("AND availableRfc.roomCategory = rfc.roomCategory) ")
                .append("FROM PatientRoom pr ")
                .append("JOIN pr.roomFacilityCharge rfc ")
                .append("WHERE pr.retired = false ")
                .append("AND pr.admittedAt BETWEEN :fd AND :td ");
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (institution != null) {
            jpql.append(" and rfc.company = :ins ");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and rfc.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and rfc.department = :dep ");
            params.put("dep", department);
        }
        jpql.append(" AND rfc.roomCategory IN :cat ");
        params.put("cat", reportCategories);
        jpql.append("GROUP BY FUNCTION('YEAR', pr.admittedAt), FUNCTION('MONTH', pr.admittedAt), ")
                .append("rfc.roomCategory, pr.patientEncounter.id ")
                .append("ORDER BY 1, 2, 3");

        List<Object[]> rows = patientRoomFacade.findObjectsArrayByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP);
        for (Object[] r : rows) {
            Integer year = toInteger(r[0]);
            Integer month = toInteger(r[1]);
            RoomCategory category = (RoomCategory) r[2];
            Long roomCount = toLong(r[4]);
            Long categoryDays = toLong(r[5]);
            Long totalLengthOfStayDays = toLong(r[6]);
            Long availableRooms = toLong(r[7]);

            if (category != null && category.getId() != null) {
                categoryAvailableRoomsCache.put(category.getId(), availableRooms);
            }

            RoomOccupancyRowDTO row = getOrCreateRow(grid, year, month);
            row.addCategoryDays(category, roomCount, categoryDays);
            row.addCategoryPatientRatioDays(category, categoryDays, totalLengthOfStayDays);
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private RoomOccupancyRowDTO getOrCreateRow(Map<Integer, Map<Integer, RoomOccupancyRowDTO>> grid,
            Integer year, Integer month) {
        return grid.computeIfAbsent(year, y -> new TreeMap<>())
                .computeIfAbsent(month, m -> new RoomOccupancyRowDTO(year, month));
    }

    /**
     * Sets totalAvailable on each category slot using the id-keyed cache.
     */
    private void applyAvailableCounts(RoomOccupancyRowDTO row) {
        for (Map.Entry<RoomCategory, RoomCategoryOccupancyDTO> e : row.getCategoryMetrics().entrySet()) {
            Long available = categoryAvailableRoomsCache.getOrDefault(e.getKey().getId(), 0L);
            e.getValue().setTotalAvailable(available);
        }
    }

    private void calculateRowDerived(RoomOccupancyRowDTO row, long daysInPeriod) {
        for (RoomCategoryOccupancyDTO cat : row.getCategoryMetrics().values()) {
            cat.calculateDerived(daysInPeriod, roomOccupancyRatioMode);
        }
        row.getIcuOccupancy().calculateDerived(daysInPeriod);
        row.getWardBedOldOccupancy().calculateDerived(daysInPeriod);
        row.getWardBedNewOccupancy().calculateDerived(daysInPeriod);
        row.calculateDerivedMetrics();
    }

    public List<RoomOccupancyRowDTO> getRoomOccupancyList() {
        return roomOccupancyList;
    }

    public void setRoomOccupancyList(List<RoomOccupancyRowDTO> v) {
        roomOccupancyList = v;
    }

    public RoomOccupancyRowDTO getRoomOccupancyGrandTotal() {
        return roomOccupancyGrandTotal;
    }

    public String getRoomOccupancyRatioMode() {
        return roomOccupancyRatioMode;
    }

    public void setRoomOccupancyRatioMode(String roomOccupancyRatioMode) {
        this.roomOccupancyRatioMode = roomOccupancyRatioMode;
    }

    public String getRoomOccupancyRatioModeAggregatedRoomUtilization() {
        return RoomCategoryOccupancyDTO.RATIO_MODE_AGGREGATED_ROOM_UTILIZATION;
    }

    public String getRoomOccupancyRatioModePatientCategoryDuration() {
        return RoomCategoryOccupancyDTO.RATIO_MODE_PATIENT_CATEGORY_DURATION;
    }

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

    public void createIpIncomeCategoryWiseReport() {
        if (reportType == null || reportType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a report type");
            return;
        }
        if (visitType == null || visitType.trim().isEmpty() || visitType.equals("Any")) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }
        bundle = generateIpIncomeCategoryWiseReport();
    }

    public ReportTemplateRowBundle generateIpIncomeCategoryWiseReport() {
        ReportTemplateRowBundle rtrb = new ReportTemplateRowBundle();

        if (paymentType == null || paymentType.trim().isEmpty()) {
            paymentType = "Any";
        }

        List<BillTypeAtomic> btasOP = new ArrayList<>();
        List<BillTypeAtomic> btasIP = new ArrayList<>();

        if ("IP".equals(visitType)) {
            btasIP.addAll(BillTypeAtomic.findByServiceType(ServiceType.INWARD_SERVICE));
        }
        if ("OP".equals(visitType)) {
            btasOP.addAll(BillTypeAtomic.findByServiceType(ServiceType.OPD));
        }
        if ("Any".equals(visitType)) {
            btasIP.addAll(BillTypeAtomic.findByServiceType(ServiceType.INWARD_SERVICE));
            btasOP.addAll(BillTypeAtomic.findByServiceType(ServiceType.OPD));

        }

        if (withProfessionalFee) {
            if ("IP".equals(visitType)) {
                List<BillTypeAtomic> profBillTypes = Arrays.asList(
                        BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL,
                        BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL_CANCELLATION,
                        BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL,
                        BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL_CANCELLATION
                );
                btasIP.addAll(profBillTypes);
            }
            if ("OP".equals(visitType)) {
                List<BillTypeAtomic> profBillTypes = Arrays.asList(
                        BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES,
                        BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN);
                btasOP.addAll(profBillTypes);
            }
            if ("Any".equals(visitType)) {
                List<BillTypeAtomic> opProfBillTypes = Arrays.asList(
                        BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES,
                        BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN
                );
                List<BillTypeAtomic> ipProfBillTypes = Arrays.asList(
                        BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL,
                        BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL_CANCELLATION,
                        BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL,
                        BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL_CANCELLATION
                );
                btasIP.addAll(ipProfBillTypes);
                btasOP.addAll(opProfBillTypes);
            }
        }

        List<IpIncomeCategoryWiseRowDTO> rows = findIpIncomeCategoryWiseRowsSingleQuery(btasOP, btasIP);

        // ── Summarise ────────────────────────────────────────────────────────────
        boolean includeItems = "detail".equalsIgnoreCase(reportType);
        summarizeIpIncomeCategoryWiseRows(rtrb, rows, includeItems);

        populateIpIncomeProfitMatrixAndBillDiscounts(rows);

        rtrb.setName(includeItems
                ? "IP Income Category Wise Report - Detail"
                : "IP Income Category Wise Report - Summary");
        rtrb.setBundleType(includeItems
                ? "ip_income_category_wise_detail"
                : "ip_income_category_wise_summary");

        rtrb.getReportTemplateRows().forEach(rtr -> {
            rtr.setInstitution(institution);
            rtr.setDepartment(department);
            rtr.setSite(site);
            rtr.setFromDate(fromDate);
            rtr.setToDate(toDate);
        });

        return rtrb;
    }

    private List<IpIncomeCategoryWiseRowDTO> findIpIncomeCategoryWiseRowsSingleQuery(List<BillTypeAtomic> btasOP, List<BillTypeAtomic> btasIP) {

        List<PaymentMethod> creditPaymentMethods = enumController.getPaymentTypeOfPaymentMethods(PaymentType.CREDIT);
        List<PaymentMethod> nonCreditPaymentMethods = enumController.getPaymentTypeOfPaymentMethods(PaymentType.NON_CREDIT);

        StringBuilder jpql = new StringBuilder();
        jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.IpIncomeCategoryWiseRowDTO("
                + " b.id, b.billClassType, b.billType, b.discount, b.deptId,"
                + " bi.grossValue, bi.hospitalFee, bi.discount, bi.staffFee, bi.netValue,"
                + " COALESCE(i.id, refI.id, refRefI.id),"
                + " COALESCE(i.name, refI.name, refRefI.name),"
                + " COALESCE(c.id, refC.id, refRefC.id),"
                + " COALESCE(c.name, refC.name, refRefC.name),"
                + " pe.paymentMethod, b.paymentMethod"
                + ")"
                + " from BillItem bi"
                + " join bi.bill b"
                + " left join bi.item i"
                + " left join i.category c"
                + " left join b.patientEncounter pe"
                + " left join bi.referanceBillItem refBi"
                + " left join refBi.item refI"
                + " left join refI.category refC"
                + " left join refBi.referanceBillItem refRefBi"
                + " left join refRefBi.item refRefI"
                + " left join refRefI.category refRefC"
                + " where b.retired = :br"
                + " and b.createdAt between :fd and :td ");

        Map<String, Object> m = new HashMap<>();
        m.put("br", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        switch (visitType) {
            case "IP":
                jpql.append(" and bi.bill.billTypeAtomic in :btas ");
                m.put("btas", btasIP);

                if (roomCategories != null && !roomCategories.isEmpty()) {
                    jpql.append(" AND bi.bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory IN :cat ");
                    m.put("cat", roomCategories);
                }
                if (admissionTypes != null && !admissionTypes.isEmpty()) {
                    jpql.append(" AND bi.bill.patientEncounter.admissionType IN :admTypes ");
                    m.put("admTypes", admissionTypes);
                }
                if (paymentType != null && !paymentType.isEmpty() && !"Any".equalsIgnoreCase(paymentType)) {
                    jpql.append(" and bi.bill.patientEncounter.paymentMethod in :pmIp ");
                    m.put("pmIp", "Credit".equals(paymentType) ? creditPaymentMethods : nonCreditPaymentMethods);
                }
                break;

            case "OP":
                jpql.append(" and bi.bill.billTypeAtomic in :btas ");
                m.put("btas", btasOP);

                if (paymentType != null && !paymentType.isEmpty() && !"Any".equalsIgnoreCase(paymentType)) {
                    jpql.append(" and bi.bill.paymentMethod in :pmOp ");
                    m.put("pmOp", "Credit".equals(paymentType)
                            ? creditPaymentMethods
                            : nonCreditPaymentMethods);
                }
                break;

            default:
                List<BillTypeAtomic> allBtas = new ArrayList<>();
                allBtas.addAll(btasIP);
                allBtas.addAll(btasOP);
                if (!allBtas.isEmpty()) {
                    jpql.append(" and bi.bill.billTypeAtomic in :btas ");
                    m.put("btas", allBtas);
                }
                if (paymentType != null && !paymentType.isEmpty() && !"Any".equalsIgnoreCase(paymentType)) {
                    jpql.append(" and bi.bill.patientEncounter.paymentMethod in :pmAny ");
                    m.put("pmAny", "Credit".equals(paymentType) ? creditPaymentMethods : nonCreditPaymentMethods);
                }
                break;
        }

        if (department != null) {
            jpql.append(" and bi.bill.department = :dep ");
            m.put("dep", department);
        }
        if (institution != null) {
            jpql.append(" and bi.bill.department.institution = :ins ");
            m.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and bi.bill.department.site = :site ");
            m.put("site", site);
        }
        if (category != null) {
            if (withProfessionalFee) {
                jpql.append(" and (i.category = :cat OR refI.category = :cat) ");
            } else {
                jpql.append(" and bi.item.category = :cat ");
            }
            m.put("cat", category);
        }

        return (List<IpIncomeCategoryWiseRowDTO>) billItemFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
    }

    private void summarizeIpIncomeCategoryWiseRows(ReportTemplateRowBundle reportBundle,
            List<IpIncomeCategoryWiseRowDTO> rows, boolean includeItems) {
        Map<String, ReportTemplateRow> categoryMap = new TreeMap<>();
        Map<String, ReportTemplateRow> itemMap = new TreeMap<>();
        Map<String, String> itemKeyToCategoryName = new HashMap<>();
        List<ReportTemplateRow> rowsToAdd = new ArrayList<>();
        double totalNetIncome = 0.0;
        double totalIncome = 0.0;
        double totalDiscount = 0.0;
        double totalHospitalFees = 0.0;
        double totalStaffFees = 0.0;
        double totalSponsorPay = 0.0;
        double totalPatientPay = 0.0;
        long totalCount = 0L;

        for (IpIncomeCategoryWiseRowDTO rowDto : rows) {
            if (rowDto.getBillId() == null) {
                continue;
            }

            PaymentMethod paymentMethodForBillItem = null;
            paymentMethodForBillItem = rowDto.getEncounterPaymentMethod() != null
                    ? rowDto.getEncounterPaymentMethod()
                    : rowDto.getBillPaymentMethod();

            if (paymentMethodForBillItem == null || paymentMethodForBillItem.getPaymentType() == PaymentType.NONE) {
                continue;
            }

            String categoryName = rowDto.getCategoryName() != null
                    ? rowDto.getCategoryName()
                    : "No Category";
            String itemName = rowDto.getItemName() != null
                    ? rowDto.getItemName()
                    : "No Item";
            String itemKey = categoryName + "->" + itemName;

            categoryMap.putIfAbsent(categoryName, new ReportTemplateRow());

            ReportTemplateRow categoryRow = categoryMap.get(categoryName);
            if (rowDto.getCategoryId() != null) {
                Category categoryRef = new Category();
                categoryRef.setId(rowDto.getCategoryId());
                categoryRef.setName(rowDto.getCategoryName());
                categoryRow.setCategory(categoryRef);
            }

            ReportTemplateRow itemRow = null;
            if (includeItems) {
                itemRow = itemMap.get(itemKey);
                if (itemRow == null) {
                    itemRow = new ReportTemplateRow();
                    if (rowDto.getItemId() != null) {
                        Item itemRef = new Item();
                        itemRef.setId(rowDto.getItemId());
                        itemRef.setName(rowDto.getItemName());
                        if (rowDto.getCategoryId() != null) {
                            Category categoryRef = new Category();
                            categoryRef.setId(rowDto.getCategoryId());
                            categoryRef.setName(rowDto.getCategoryName());
                            itemRef.setCategory(categoryRef);
                        }
                        itemRow.setItem(itemRef);
                    }
                    itemMap.put(itemKey, itemRow);
                    itemKeyToCategoryName.put(itemKey, categoryName);
                }
            }

            long countModifier = (rowDto.getBillClassType() == BillClassType.CancelledBill
                    || rowDto.getBillClassType() == BillClassType.RefundBill) ? -1 : 1;

            double grossValue = countModifier * Math.abs(nullSafeDouble(rowDto.getGrossValue()));
            double hospitalFee = countModifier * Math.abs(nullSafeDouble(rowDto.getHospitalFee()));
            double iteratingDiscount = countModifier * Math.abs(nullSafeDouble(rowDto.getDiscount()));
            double staffFee = countModifier * Math.abs(nullSafeDouble(rowDto.getStaffFee()));
            boolean professionalPaymentBill = rowDto.getBillType() == BillType.PaymentBill;
            double netValue;
            if (professionalPaymentBill) {
                netValue = countModifier * Math.abs(nullSafeDouble(rowDto.getNetValue()));
            } else if (withProfessionalFee) {
                netValue = countModifier * Math.abs(nullSafeDouble(rowDto.getNetValue()));
            } else {
                netValue = countModifier * Math.abs(nullSafeDouble(rowDto.getNetValue()) - nullSafeDouble(rowDto.getStaffFee()));
            }

            double sponsorDiscount = 0.0;
            double sponsorPay = 0.0;
            double patientPay = 0.0;
            if (paymentMethodForBillItem.getPaymentType() == PaymentType.CREDIT) {
                sponsorPay = netValue;
            } else if (paymentMethodForBillItem.getPaymentType() == PaymentType.NON_CREDIT) {
                patientPay = netValue;
            }

            totalIncome += grossValue;
            totalNetIncome += netValue;
            totalHospitalFees += hospitalFee;
            totalDiscount += iteratingDiscount;
            totalStaffFees += staffFee;
            totalSponsorPay += sponsorPay;
            totalPatientPay += patientPay;
            totalCount += countModifier;

            updateIpIncomeCategoryRow(categoryRow, countModifier, grossValue, hospitalFee, iteratingDiscount,
                    sponsorDiscount, staffFee, netValue, sponsorPay, patientPay);

            if (includeItems && itemRow != null) {
                updateIpIncomeCategoryRow(itemRow, countModifier, grossValue, hospitalFee, iteratingDiscount,
                        sponsorDiscount, staffFee, netValue, sponsorPay, patientPay);
            }
        }

        Map<String, List<ReportTemplateRow>> itemRowsByCategory = new HashMap<>();
        if (includeItems) {
            for (Map.Entry<String, ReportTemplateRow> entry : itemMap.entrySet()) {
                String categoryName = itemKeyToCategoryName.get(entry.getKey());
                itemRowsByCategory
                        .computeIfAbsent(categoryName, k -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        categoryMap.forEach((categoryName, catRow) -> {
            rowsToAdd.add(catRow);
            if (includeItems) {
                List<ReportTemplateRow> itemRows = itemRowsByCategory.get(categoryName);
                if (itemRows != null) {
                    rowsToAdd.addAll(itemRows);
                }
            }
        });

        reportBundle.getReportTemplateRows().addAll(rowsToAdd);

        reportBundle.setTotal(totalNetIncome);
        reportBundle.setDiscount(totalDiscount);
        reportBundle.setGrossTotal(totalIncome);
        reportBundle.setHospitalTotal(totalHospitalFees);
        reportBundle.setStaffTotal(totalStaffFees);
        reportBundle.setCount(totalCount);

        ipIncomeTotalSponsorPay = totalSponsorPay;
        ipIncomeTotalPatientPay = totalPatientPay;
    }

    private void updateIpIncomeCategoryRow(ReportTemplateRow row, long countModifier, double grossValue, double hospitalFee,
            double discount, double sponsorDiscount, double professionalFee, double netValue, double sponsorPay, double patientPay) {

        if (row.getItemCount() == null) {
            row.setItemCount(0L);
        }
        if (row.getItemTotal() == null) {
            row.setItemTotal(0.0);
        }
        if (row.getItemHospitalFee() == null) {
            row.setItemHospitalFee(0.0);
        }
        if (row.getItemDiscountAmount() == null) {
            row.setItemDiscountAmount(0.0);
        }
        if (row.getItemDiscount() == null) {
            row.setItemDiscount(0.0);
        }
        if (row.getItemProfessionalFee() == null) {
            row.setItemProfessionalFee(0.0);
        }
        if (row.getItemNetTotal() == null) {
            row.setItemNetTotal(0.0);
        }
        if (row.getRowValueIn() == null) {
            row.setRowValueIn(0.0);
        }
        if (row.getRowValueOut() == null) {
            row.setRowValueOut(0.0);
        }

        row.setItemCount(row.getItemCount() + countModifier);
        row.setItemTotal(row.getItemTotal() + grossValue);
        row.setItemHospitalFee(row.getItemHospitalFee() + hospitalFee);
        row.setItemDiscountAmount(row.getItemDiscountAmount() + discount);
        row.setItemDiscount(row.getItemDiscount() + sponsorDiscount);
        row.setItemProfessionalFee(row.getItemProfessionalFee() + professionalFee);
        row.setItemNetTotal(row.getItemNetTotal() + netValue);
        row.setRowValueIn(row.getRowValueIn() + sponsorPay);
        row.setRowValueOut(row.getRowValueOut() + patientPay);
    }

    private void populateIpIncomeProfitMatrixAndBillDiscounts(List<IpIncomeCategoryWiseRowDTO> rows) {
        ipIncomeCashTotal = 0.0;
        ipIncomeCreditTotal = 0.0;
        ipIncomeTotalBillDiscount = 0.0;
        ipIncomeBillDiscounts = new ArrayList<>();

        // Collect per-bill bill-level discounts (one entry per bill, avoid duplicates)
        // Key: bill.id  →  { invoiceNo, billDiscount }
        Map<Long, Map<String, Object>> billDiscountMap = new LinkedHashMap<>();

        for (IpIncomeCategoryWiseRowDTO rowDto : rows) {
            if (rowDto.getBillId() == null) {
                continue;
            }

            // ── Resolve payment method ──────────────────────────────────────────
            PaymentMethod pm = null;
            pm = rowDto.getEncounterPaymentMethod() != null
                    ? rowDto.getEncounterPaymentMethod()
                    : rowDto.getBillPaymentMethod();
            if (pm == null || pm.getPaymentType() == PaymentType.NONE) {
                continue;
            }

            // ── Count modifier (cancellations/refunds subtract) ─────────────────
            long countModifier = (rowDto.getBillClassType() == BillClassType.CancelledBill
                    || rowDto.getBillClassType() == BillClassType.RefundBill) ? -1 : 1;

            // ── Net value (same logic as summarizeBillItemsToIpIncomeCategoryWise) ──
            boolean isProfPayment = rowDto.getBillType() == BillType.PaymentBill;
            double netValue;
            if (isProfPayment || withProfessionalFee) {
                netValue = countModifier * Math.abs(nullSafeDouble(rowDto.getNetValue()));
            } else {
                netValue = countModifier * Math.abs(nullSafeDouble(rowDto.getNetValue()) - nullSafeDouble(rowDto.getStaffFee()));
            }

            // ── Profit Matrix: Cash vs Credit ───────────────────────────────────
            if (pm.getPaymentType() == PaymentType.NON_CREDIT) {
                ipIncomeCashTotal += netValue;
            } else if (pm.getPaymentType() == PaymentType.CREDIT) {
                ipIncomeCreditTotal += netValue;
            }

            // ── Bill Discount: aggregate per bill ───────────────────────────────
            Double billDiscount = rowDto.getBillDiscount();
            if (billDiscount != null && billDiscount != 0.0) {
                Long billId = rowDto.getBillId();
                if (!billDiscountMap.containsKey(billId)) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("invoiceNo", rowDto.getBillDeptId() != null
                            ? rowDto.getBillDeptId() : String.valueOf(billId));
                    entry.put("discount", countModifier * Math.abs(billDiscount));
                    billDiscountMap.put(billId, entry);
                } else {
                    Map<String, Object> entry = billDiscountMap.get(billId);
                    double existing = (Double) entry.get("discount");
                    entry.put("discount", existing + countModifier * Math.abs(billDiscount));
                }
            }
        }

        ipIncomeBillDiscounts = new ArrayList<>(billDiscountMap.values());
        ipIncomeTotalBillDiscount = ipIncomeBillDiscounts.stream()
                .mapToDouble(e -> (Double) e.get("discount"))
                .sum();
    }

    private double nullSafeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

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

        // Post-process to set the doctor name with title
        for (SurgeryCountDoctorWiseDTO dto : rawList) {
            if (dto.getStaff() != null && dto.getStaff().getPerson() != null) {
                dto.setDoctorName(dto.getStaff().getPerson().getNameWithTitle());
            }
        }

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

    public void downloadSurgeryCountDoctorWisePdf() throws Exception {
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
            // ── Doctor-wise charts ─────────────────────────────────────────────────
            document.add(buildDoctorLineChart(reportYear));
            document.add(buildDoctorBarChart(reportYear));

            // ── Specialty-wise charts ──────────────────────────────────────────────
            document.add(buildSpecialtyLineChart(reportYear));
            document.add(buildSpecialtyBarChart(reportYear));

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

    private Image buildChartImage(JFreeChart chart, int width, int height)
            throws Exception {
        BufferedImage bi = chart.createBufferedImage(width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        Image img = Image.getInstance(baos.toByteArray());
        img.setWidthPercentage(100);
        return img;
    }

    private List<SurgeryCountDoctorWiseDTO> getDoctorChartRows() {
        if (billList == null) {
            return new ArrayList<>();
        }
        return billList.stream()
                .filter(dto -> !dto.isSubtotal() && !dto.isGrandTotal())
                .collect(Collectors.toList());
    }

    private List<SurgeryCountDoctorWiseDTO> getSpecialtyChartRows() {
        if (billList == null) {
            return new ArrayList<>();
        }
        return billList.stream()
                .filter(SurgeryCountDoctorWiseDTO::isSubtotal)
                .collect(Collectors.toList());
    }

    private int[] writeChartDataBlock(XSSFSheet sheet,
            int startRow,
            String seriesHeader,
            List<SurgeryCountDoctorWiseDTO> rows,
            boolean useSpecialtyName) {
        int headerRowIndex = startRow;
        Row headerRow = sheet.createRow(startRow++);
        headerRow.createCell(0).setCellValue(seriesHeader);
        for (int i = 0; i < MONTH_LABELS.length; i++) {
            headerRow.createCell(i + 1).setCellValue(MONTH_LABELS[i]);
        }

        int firstDataRow = startRow;
        for (SurgeryCountDoctorWiseDTO dto : rows) {
            Row row = sheet.createRow(startRow++);
            String label = useSpecialtyName
                    ? (dto.getSpecialityName() != null ? dto.getSpecialityName() : "")
                    : (dto.getDoctorName() != null ? dto.getDoctorName() : "");
            row.createCell(0).setCellValue(label);

            int[] monthValues = {
                dto.getJanuary(), dto.getFebruary(), dto.getMarch(),
                dto.getApril(), dto.getMay(), dto.getJune(),
                dto.getJuly(), dto.getAugust(), dto.getSeptember(),
                dto.getOctober(), dto.getNovember(), dto.getDecember()
            };
            for (int i = 0; i < monthValues.length; i++) {
                row.createCell(i + 1).setCellValue(monthValues[i]);
            }
        }

        int lastDataRow = startRow - 1;
        return new int[]{headerRowIndex, firstDataRow, lastDataRow};
    }

    private void addLineChart(XSSFSheet sheet,
            XSSFDrawing drawing,
            int col,
            int row,
            int[] block,
            String title) {
        if (block[1] > block[2]) {
            return;
        }

        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col, row, col + 12, row + 20);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.RIGHT);

        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet, new CellRangeAddress(block[0], block[0], 1, 12));
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, xAxis, yAxis);

        for (int r = block[1]; r <= block[2]; r++) {
            XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(r, r, 1, 12));
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(categories, values);
            String seriesName = "";
            if (sheet.getRow(r) != null && sheet.getRow(r).getCell(0) != null) {
                seriesName = sheet.getRow(r).getCell(0).getStringCellValue();
            }
            series.setTitle(seriesName, null);
            series.setSmooth(false);
            series.setMarkerStyle(MarkerStyle.CIRCLE);
        }

        chart.plot(data);
    }

    private void addBarChart(XSSFSheet sheet,
            XSSFDrawing drawing,
            int col,
            int row,
            int[] block,
            String title) {
        if (block[1] > block[2]) {
            return;
        }

        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col, row, col + 12, row + 20);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP);

        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
        yAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet, new CellRangeAddress(block[0], block[0], 1, 12));
        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, xAxis, yAxis);
        data.setBarDirection(BarDirection.COL);

        for (int r = block[1]; r <= block[2]; r++) {
            XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(r, r, 1, 12));
            XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(categories, values);
            String seriesName = "";
            if (sheet.getRow(r) != null && sheet.getRow(r).getCell(0) != null) {
                seriesName = sheet.getRow(r).getCell(0).getStringCellValue();
            }
            series.setTitle(seriesName, null);
        }

        chart.plot(data);
    }
    private static final String[] MONTH_LABELS
            = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private static final java.awt.Color[] DOCTOR_COLORS = {
        new java.awt.Color(75, 192, 192), new java.awt.Color(255, 99, 132),
        new java.awt.Color(54, 162, 235), new java.awt.Color(255, 206, 86),
        new java.awt.Color(153, 102, 255), new java.awt.Color(255, 159, 64),
        new java.awt.Color(199, 199, 199), new java.awt.Color(83, 102, 255),
        new java.awt.Color(255, 99, 255), new java.awt.Color(99, 255, 132)
    };

    private static final java.awt.Color[] SPECIALTY_COLORS = {
        new java.awt.Color(220, 20, 60), new java.awt.Color(65, 105, 225),
        new java.awt.Color(255, 140, 0), new java.awt.Color(34, 139, 34),
        new java.awt.Color(138, 43, 226), new java.awt.Color(255, 215, 0)
    };

    /**
     * Fills a DefaultCategoryDataset from billList for doctor rows
     * (non-subtotal, non-grand).
     */
    private DefaultCategoryDataset buildDoctorDataset() {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (dto.isSubtotal() || dto.isGrandTotal()) {
                continue;
            }
            int[] vals = {dto.getJanuary(), dto.getFebruary(), dto.getMarch(),
                dto.getApril(), dto.getMay(), dto.getJune(),
                dto.getJuly(), dto.getAugust(), dto.getSeptember(),
                dto.getOctober(), dto.getNovember(), dto.getDecember()};
            for (int i = 0; i < 12; i++) {
                ds.addValue(vals[i], dto.getDoctorName(), MONTH_LABELS[i]);
            }
        }
        return ds;
    }

    /**
     * Fills a DefaultCategoryDataset from billList for specialty subtotal rows.
     */
    private DefaultCategoryDataset buildSpecialtyDataset() {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (!dto.isSubtotal()) {
                continue;
            }
            int[] vals = {dto.getJanuary(), dto.getFebruary(), dto.getMarch(),
                dto.getApril(), dto.getMay(), dto.getJune(),
                dto.getJuly(), dto.getAugust(), dto.getSeptember(),
                dto.getOctober(), dto.getNovember(), dto.getDecember()};
            for (int i = 0; i < 12; i++) {
                ds.addValue(vals[i], dto.getSpecialityName(), MONTH_LABELS[i]);
            }
        }
        return ds;
    }

    /**
     * Applies a color array to every series in a CategoryPlot renderer.
     */
    private void applyColors(CategoryPlot plot, java.awt.Color[] palette) {
        for (int i = 0; i < plot.getDataset().getRowCount(); i++) {
            plot.getRenderer().setSeriesPaint(i, palette[i % palette.length]);
        }
    }

// ── Doctor line chart ──────────────────────────────────────────────────────
    private Image buildDoctorLineChart(int year) throws Exception {
        DefaultCategoryDataset ds = buildDoctorDataset();
        JFreeChart chart = ChartFactory.createLineChart(
                "Doctor Wise Surgery Count – Year " + year,
                "Month", "Surgery Count",
                ds, PlotOrientation.VERTICAL, true, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(
                NumberAxis.createIntegerTickUnits());
        plot.getRangeAxis().setLowerBound(0);

        LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, true);
        renderer.setDefaultStroke(new java.awt.BasicStroke(2f));
        plot.setRenderer(renderer);
        applyColors(plot, DOCTOR_COLORS);

        chart.getLegend().setPosition(
                org.jfree.chart.ui.RectangleEdge.RIGHT);

        Image img = buildChartImage(chart, 1100, 400);
        img.setSpacingBefore(20);
        img.setSpacingAfter(10);
        return img;
    }

// ── Doctor bar chart ───────────────────────────────────────────────────────
    private Image buildDoctorBarChart(int year) throws Exception {
        DefaultCategoryDataset ds = buildDoctorDataset();
        JFreeChart chart = ChartFactory.createBarChart(
                "Doctor Wise Surgery Count – Year " + year,
                "Month", "Surgery Count",
                ds, PlotOrientation.VERTICAL, true, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(
                NumberAxis.createIntegerTickUnits());
        plot.getRangeAxis().setLowerBound(0);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(true);
        applyColors(plot, DOCTOR_COLORS);

        chart.getLegend().setPosition(
                org.jfree.chart.ui.RectangleEdge.TOP);

        Image img = buildChartImage(chart, 1100, 400);
        img.setSpacingBefore(10);
        img.setSpacingAfter(20);
        return img;
    }

// ── Specialty line chart ───────────────────────────────────────────────────
    private Image buildSpecialtyLineChart(int year) throws Exception {
        DefaultCategoryDataset ds = buildSpecialtyDataset();
        JFreeChart chart = ChartFactory.createLineChart(
                "Specialty Wise Surgery Count – Year " + year,
                "Month", "Surgery Count",
                ds, PlotOrientation.VERTICAL, true, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(
                NumberAxis.createIntegerTickUnits());
        plot.getRangeAxis().setLowerBound(0);

        LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, true);
        renderer.setDefaultStroke(new java.awt.BasicStroke(3f));
        plot.setRenderer(renderer);
        applyColors(plot, SPECIALTY_COLORS);

        chart.getLegend().setPosition(
                org.jfree.chart.ui.RectangleEdge.RIGHT);

        Image img = buildChartImage(chart, 1100, 400);
        img.setSpacingBefore(20);
        img.setSpacingAfter(10);
        return img;
    }

// ── Specialty bar chart ────────────────────────────────────────────────────
    private Image buildSpecialtyBarChart(int year) throws Exception {
        DefaultCategoryDataset ds = buildSpecialtyDataset();
        JFreeChart chart = ChartFactory.createBarChart(
                "Specialty Wise Surgery Count – Year " + year,
                "Month", "Surgery Count",
                ds, PlotOrientation.VERTICAL, true, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ((NumberAxis) plot.getRangeAxis()).setStandardTickUnits(
                NumberAxis.createIntegerTickUnits());
        plot.getRangeAxis().setLowerBound(0);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(true);
        applyColors(plot, SPECIALTY_COLORS);

        chart.getLegend().setPosition(
                org.jfree.chart.ui.RectangleEdge.TOP);

        Image img = buildChartImage(chart, 1100, 400);
        img.setSpacingBefore(10);
        img.setSpacingAfter(20);
        return img;
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

            // ── Charts sheet (native Excel charts) ─────────────────────────────
            XSSFSheet chartSheet = workbook.createSheet("Charts");
            XSSFDrawing drawing = chartSheet.createDrawingPatriarch();

            List<SurgeryCountDoctorWiseDTO> doctorChartRows = getDoctorChartRows();
            List<SurgeryCountDoctorWiseDTO> specialtyChartRows = getSpecialtyChartRows();

            int chartRowStart = 0;
            if (!doctorChartRows.isEmpty()) {
                int[] doctorBlock = writeChartDataBlock(
                        chartSheet, chartRowStart, "Doctor", doctorChartRows, false);
                int doctorChartsStart = doctorBlock[2] + 2;
                addLineChart(chartSheet, drawing, 0, doctorChartsStart, doctorBlock,
                        "Doctor Wise Surgery Trend - Year " + reportYear);
                addBarChart(chartSheet, drawing, 0, doctorChartsStart + 22, doctorBlock,
                        "Doctor Wise Surgery Count - Year " + reportYear);
                chartRowStart = doctorChartsStart + 45;
            }

            if (!specialtyChartRows.isEmpty()) {
                int[] specialtyBlock = writeChartDataBlock(
                        chartSheet, chartRowStart, "Speciality", specialtyChartRows, true);
                int specialtyChartsStart = specialtyBlock[2] + 2;
                addLineChart(chartSheet, drawing, 0, specialtyChartsStart, specialtyBlock,
                        "Specialty Wise Surgery Trend - Year " + reportYear);
                addBarChart(chartSheet, drawing, 0, specialtyChartsStart + 22, specialtyBlock,
                        "Specialty Wise Surgery Count - Year " + reportYear);
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
                    .append(" COALESCE(c.name, 'Uncategorized'), ")
                    .append(" count(s) ")
                    .append(") ")
                    .append(" from PatientEncounter s ")
                    .append(" join s.parentEncounter a ")
                    .append(" left join s.item i ")
                    .append(" left join i.category c ")
                    .append(" Where s.retired = false ")
                    .append(" and a.discharged = true ")
                    .append(" and a.dateOfDischarge is not null ")
                    .append(" AND a.dateOfDischarge BETWEEN :fromDate AND :toDate ")
                    .append("   and exists ( ")
                    .append("       select bf.id ")
                    .append("       from Bill bf ")
                    .append("       where bf.retired = false ")
                    .append("         and bf.cancelled = false ")
                    .append("         and bf.billTypeAtomic = :bt ")
                    .append("         and (bf.patientEncounter = s OR bf.patientEncounter = a) ")
                    .append("         and not exists ( ")
                    .append("             select cb.id from CancelledBill cb ")
                    .append("             where cb.retired = false ")
                    .append("             and cb.billedBill = bf ")
                    .append("         ) ")
                    .append("   ) ");

            params.put("bt", BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
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

            jpql.append(" Group By FUNCTION('MONTH', a.dateOfDischarge), COALESCE(c.name, 'Uncategorized') ");

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
                    .append(" AND a.dateOfDischarge BETWEEN :fromDate AND :toDate ")
                    .append("   and exists ( ")
                    .append("       select bf.id ")
                    .append("       from Bill bf ")
                    .append("       where bf.retired = false ")
                    .append("         and bf.cancelled = false ")
                    .append("         and bf.billTypeAtomic = :bt ")
                    .append("         and (bf.patientEncounter = s OR bf.patientEncounter = a) ")
                    .append("         and not exists ( ")
                    .append("             select cb.id from CancelledBill cb ")
                    .append("             where cb.retired = false ")
                    .append("             and cb.billedBill = bf ")
                    .append("         ) ")
                    .append("   ) ");

            params.put("bt", BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);

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

    public void downloadSurgerySurveyPdf() {
        if (monthlySurgeryCountList == null || monthlySurgeryCountList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        com.lowagie.text.Document document = null;
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");

            String fileName = "Surgery_Survey_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();

            document = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A3.rotate(), 20, 20, 30, 20);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            com.lowagie.text.Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");

            String title = "Surgery Survey " + reportType + " Report";
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(6);
            document.add(titlePara);

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(50);
            info.setWidths(new float[]{1.4f, 3f});
            info.setSpacingAfter(10);

            addInfoRow(info, "From Date:", fromDate != null ? sdf.format(fromDate) : "");
            addInfoRow(info, "To Date:", toDate != null ? sdf.format(toDate) : "");
            addInfoRow(info, "Surgery Type:", surgeryType != null ? surgeryType.getName() : "All");
            addInfoRow(info, "Institution:", institution != null ? institution.getName() : "All");
            addInfoRow(info, "Site:", site != null ? site.getName() : "All");
            addInfoRow(info, "Department:", department != null ? department.getName() : "All");
            document.add(info);

            // Build table header: Month + dynamic surgery headers + Total
            int colCount = 2 + (surgeryHeaders != null ? surgeryHeaders.size() : 0);
            PdfPTable table = new PdfPTable(colCount);
            table.setWidthPercentage(100);

            PdfPCell monthHeader = new PdfPCell(new Phrase("Month", headerFont));
            monthHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(monthHeader);

            if (surgeryHeaders != null) {
                for (String h : surgeryHeaders) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }
            }

            PdfPCell totalHeader = new PdfPCell(new Phrase("Total", headerFont));
            totalHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(totalHeader);

            for (MonthlySurgeryCountDTO row : monthlySurgeryCountList) {
                table.addCell(new Phrase(row.isGrandTotal() ? "Grand Total" : row.getMonthName(), normalFont));

                if (surgeryHeaders != null) {
                    for (String h : surgeryHeaders) {
                        Long val = row.getServiceCountMap().get(h);
                        table.addCell(new Phrase(val == null ? "0" : val.toString(), normalFont));
                    }
                }

                table.addCell(new Phrase(String.valueOf(row.getTotal()), normalFont));
            }

            document.add(table);
            facesContext.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    private void addInfoRow(PdfPTable info, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        l.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        l.setPadding(3);
        info.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        v.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        v.setPadding(3);
        info.addCell(v);
    }

    public void processIpUnsettledInvoicesReport() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

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
                .append("AND pe.dateOfAdmission BETWEEN :fd AND :td ");

        params.put("ret", false);
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (dischargeFromDate != null && dischargeToDate != null) {
            jpql.append("AND pe.dateOfDischarge BETWEEN :dfd AND :dtd ");
            params.put("dfd", dischargeFromDate);
            params.put("dtd", dischargeToDate);
        }

        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null) {
            jpql.append("AND pe.finalBill IS NOT NULL ")
                    .append("AND pe.finalBill.createdAt BETWEEN :iafd AND :iatd ");
            params.put("iafd", invoiceApprovedFromDate);
            params.put("iatd", invoiceApprovedToDate);
        }

        if (institution != null) {
            jpql.append("AND pe.institution = :inst ");
            params.put("inst", institution);
        }
        if (site != null) {
            jpql.append("AND pe.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append("AND pe.department = :dept ");
            params.put("dept", department);
        }
        if (consultant != null) {
            jpql.append("AND pe.referringConsultant = :cons ");
            params.put("cons", consultant);
        }
        if (serviceCenter != null) {
            jpql.append("AND pe.department = :sc ");
            params.put("sc", serviceCenter);
        }
        if (sponsor != null) {
            jpql.append("AND pe.creditCompany = :sponsor ");
            params.put("sponsor", sponsor);
        }
        if (admissionType != null) {
            jpql.append("AND pe.admissionType = :at ");
            params.put("at", admissionType);
        }
        if (paymentMethod != null) {
            jpql.append("AND pe.paymentMethod = :pm ");
            params.put("pm", paymentMethod);
        }
        if (roomCategory != null) {
            jpql.append("AND rfc.roomCategory = :rc ");
            params.put("rc", roomCategory);
        }
        if (admissionStatus != null) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = FALSE ");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = FALSE ");
                    params.put("dis", true);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = TRUE ");
                    params.put("dis", true);
                    break;
                case ANY_STATUS:
                default:
                    jpql.append("AND pe.paymentFinalized = FALSE ");
                    break;
            }
        } else {
            jpql.append("AND pe.paymentFinalized = FALSE ");
        }

        jpql.append("ORDER BY pe.dateOfAdmission ");

        try {
            unsettledInvoicesList = (List<IpUnsettledInvoiceDTO>) peFacade.findLightsByJpql(
                    jpql.toString(), params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error loading unsettled invoices: " + e.getMessage());
            unsettledInvoicesList = new ArrayList<>();
            return;
        }

        if (unsettledInvoicesList == null || unsettledInvoicesList.isEmpty()) {
            unsettledInvoicesList = new ArrayList<>();
            return;
        }

        List<Long> encounterIds = unsettledInvoicesList.stream()
                .filter(dto -> dto != null && dto.getAdmissionId() != null)
                .map(IpUnsettledInvoiceDTO::getAdmissionId)
                .collect(Collectors.toList());

        if (encounterIds.isEmpty()) {
            return;
        }

        List<PatientEncounter> encounters = peFacade.findByJpql(
                "SELECT pe FROM PatientEncounter pe WHERE pe.id IN :ids",
                Collections.singletonMap("ids", encounterIds));

        Map<Long, PatientEncounter> encounterById = (encounters == null)
                ? Collections.emptyMap()
                : encounters.stream().collect(
                        Collectors.toMap(PatientEncounter::getId, pe -> pe));

        List<PatientEncounter> allChildren = peFacade.findByJpql(
                "SELECT pe FROM PatientEncounter pe WHERE pe.parentEncounter.id IN :ids AND pe.retired = false",
                Collections.singletonMap("ids", encounterIds));
        Map<Long, List<PatientEncounter>> childrenByParentId = (allChildren == null)
                ? Collections.emptyMap()
                : allChildren.stream()
                        .filter(pe -> pe.getParentEncounter() != null)
                        .collect(Collectors.groupingBy(pe -> pe.getParentEncounter().getId()));

        Map<Long, Double> paidByEncounterId = batchFetchPaidAmounts(encounterIds);

        for (IpUnsettledInvoiceDTO dto : unsettledInvoicesList) {
            if (dto == null) {
                continue;
            }

            PatientEncounter pe = encounterById.get(dto.getAdmissionId());
            if (pe == null) {
                dto.setNetTotal(0.0);
                dto.setCreditPaidAmount(0.0);
                continue;
            }

            List<PatientEncounter> children = childrenByParentId.getOrDefault(dto.getAdmissionId(), Collections.emptyList());
            double total = inwardBeanController.calculateInwardTotal(pe, children);
            double collected = paidByEncounterId.getOrDefault(dto.getAdmissionId(), 0.0);
            collected = Math.min(collected, total);

            dto.setNetTotal(total);
            dto.setCreditPaidAmount(collected);
        }
    }

    public void processAdmissionDischargeReport() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT new com.divudi.core.data.dto.AdmissionDischargeDTO(")
                .append("pe.patient.phn, ")
                .append("pe.patient.person.name, ")
                .append("pe.patient.person.mobile, ")
                .append("pe.bhtNo, ")
                .append("pe.patient.person.address, ")
                .append("pe.comments, ")
                .append("pe.admissionType.name, ")
                .append("pe.patient.person.dob, ")
                .append("pe.patient.person.sex, ")
                .append("pe.department.name, ")
                .append("pe.dateOfAdmission, ")
                .append("pe.dateOfDischarge, ")
                .append("dc.name, ")
                .append("rfc.name, ")
                .append("rcp.name, ")
                .append("cc.name, ")
                .append("pe.totalCompanyPaidAtFinalProcessing, ")
                .append("pe.totalPatientPaidAtFinalProcessing, ")
                .append("pe.discount, ")
                .append("pe.netTotal, ")
                .append("pe.amountDueAtFinalProcessing, ")
                .append("cd.name, ")
                .append("pe.clinicalDischargeDateTime, ")
                .append("fb.creater.name, ")
                .append("fb.createdAt) ")
                .append("FROM PatientEncounter pe ")
                .append("LEFT JOIN pe.dischargeCondition d ")
                .append("LEFT JOIN d.category dc ")
                .append("LEFT JOIN pe.currentPatientRoom room ")
                .append("LEFT JOIN room.roomFacilityCharge rfc ")
                .append("LEFT JOIN pe.referringConsultant rc ")
                .append("LEFT JOIN rc.person rcp ")
                .append("LEFT JOIN pe.creditCompany cc ")
                .append("LEFT JOIN pe.clinicalDischargedBy cd ")
                .append("LEFT JOIN pe.finalBill fb ");

        jpql.append("WHERE pe.retired = :ret ")
                .append("AND pe.dateOfAdmission BETWEEN :fd AND :td ");
        params.put("ret", false);
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (dischargeFromDate != null && dischargeToDate != null) {
            jpql.append("AND pe.dateOfDischarge BETWEEN :dfd AND :dtd ");
            params.put("dfd", dischargeFromDate);
            params.put("dtd", dischargeToDate);
        }
        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null) {
            jpql.append("AND pe.finalBill IS NOT NULL ")
                    .append("AND pe.finalBill.createdAt BETWEEN :iafd AND :iatd ");
            params.put("iafd", invoiceApprovedFromDate);
            params.put("iatd", invoiceApprovedToDate);
        }
        if (institution != null) {
            jpql.append("AND pe.institution = :inst ");
            params.put("inst", institution);
        }
        if (site != null) {
            jpql.append("AND pe.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append("AND pe.department = :dept ");
            params.put("dept", department);
        }
        if (consultant != null) {
            jpql.append("AND pe.referringConsultant = :cons ");
            params.put("cons", consultant);
        }
        if (serviceCenter != null) {
            jpql.append("AND pe.department = :sc ");
            params.put("sc", serviceCenter);
        }
        if (sponsor != null) {
            jpql.append("AND pe.creditCompany = :sponsor ");
            params.put("sponsor", sponsor);
        }
        if (admissionType != null) {
            jpql.append("AND pe.admissionType = :at ");
            params.put("at", admissionType);
        }
        if (paymentMethod != null) {
            jpql.append("AND pe.paymentMethod = :pm ");
            params.put("pm", paymentMethod);
        }
        if (roomCategory != null) {
            jpql.append("AND rfc.roomCategory = :rc ");
            params.put("rc", roomCategory);
        }
        if (admissionStatus != null) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = FALSE ");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = FALSE ");
                    params.put("dis", true);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = TRUE ");
                    params.put("dis", true);
                    break;
                case ANY_STATUS:
                default:
                    break;
            }
        }

        jpql.append("ORDER BY pe.dateOfAdmission ");

        try {
            admissionDischargesList = (List<AdmissionDischargeDTO>) peFacade.findLightsByJpql(
                    jpql.toString(),
                    params,
                    TemporalType.TIMESTAMP
            );
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error loading admissions & discharges: " + e.getMessage());
            admissionDischargesList = new ArrayList<>();
        }
    }

    private Map<Long, Double> batchFetchPaidAmounts(List<Long> encounterIds) {
        if (encounterIds == null || encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = "SELECT b.patientEncounter.id, SUM(b.netTotal) "
                + "FROM Bill b "
                + "WHERE b.retired = false "
                + "  AND b.cancelled = false "
                + "  AND b.billType = :btp "
                + "  AND b.patientEncounter.id IN :ids "
                + "GROUP BY b.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardPaymentBill);
        params.put("ids", encounterIds);

        List<Object[]> rows = getBillFacade().findObjectsArrayByJpql(sql, params, TemporalType.TIMESTAMP);

        Map<Long, Double> result = new HashMap<>(encounterIds.size());
        if (rows != null) {
            for (Object[] row : rows) {
                Long id = ((Number) row[0]).longValue();
                Double paid = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                result.put(id, Math.max(0.0, paid));
            }
        }
        return result;
    }

    public void processAdmissionCategoryWiseAdmissionReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Admission From Date and Admission To Date are required.");
            admissionCategoryWiseAdmissionList = new ArrayList<>();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT new com.divudi.core.data.dto.AdmissionCategoryWiseAdmissionDTO(")
                .append("ad.id, ")
                .append("ad.bhtNo, ")
                .append("ad.patient.person.name, ")
                .append("ad.patient.person.title, ")
                .append("ad.admissionType, ")
                .append("ad.paymentMethod, ")
                .append("ad.paymentFinalized")
                .append(") FROM Admission ad ");

        if (roomCategory != null) {
            jpql.append("LEFT JOIN ad.currentPatientRoom room ")
                    .append("LEFT JOIN room.roomFacilityCharge rfc ");
        }

        jpql.append("WHERE ad.retired = :ret ")
                .append("AND ad.dateOfAdmission BETWEEN :fd AND :td ");

        params.put("ret", false);
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (dischargeFromDate != null && dischargeToDate != null) {
            jpql.append("AND ad.dateOfDischarge BETWEEN :dfd AND :dtd ");
            params.put("dfd", dischargeFromDate);
            params.put("dtd", dischargeToDate);
        }

        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null) {
            jpql.append("AND ad.finalBill IS NOT NULL ")
                    .append("AND ad.finalBill.createdAt BETWEEN :iafd AND :iatd ");
            params.put("iafd", invoiceApprovedFromDate);
            params.put("iatd", invoiceApprovedToDate);
        }

        if (institution != null) {
            jpql.append("AND ad.institution = :inst ");
            params.put("inst", institution);
        }
        if (site != null) {
            jpql.append("AND ad.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append("AND ad.department = :dept ");
            params.put("dept", department);
        }
        if (consultant != null) {
            jpql.append("AND ad.referringConsultant = :cons ");
            params.put("cons", consultant);
        }
        if (serviceCenter != null) {
            jpql.append("AND ad.department = :sc ");
            params.put("sc", serviceCenter);
        }
        if (sponsor != null) {
            jpql.append("AND ad.creditCompany = :sponsor ");
            params.put("sponsor", sponsor);
        }
        if (admissionType != null) {
            jpql.append("AND ad.admissionType = :at ");
            params.put("at", admissionType);
        }
        if (paymentMethod != null) {
            jpql.append("AND ad.paymentMethod = :pm ");
            params.put("pm", paymentMethod);
        }
        if (roomCategory != null) {
            jpql.append("AND rfc.roomCategory = :rc ");
            params.put("rc", roomCategory);
        }
        if (admissionStatus != null && admissionStatus != ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append("AND ad.discharged = :dis AND ad.paymentFinalized = FALSE ");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append("AND ad.discharged = :dis AND ad.paymentFinalized = FALSE ");
                    params.put("dis", true);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append("AND ad.discharged = :dis AND ad.paymentFinalized = TRUE ");
                    params.put("dis", true);
                    break;
                default:
                    break;
            }
        }

        jpql.append("ORDER BY ad.admissionType.name, ad.bhtNo ");

        try {
            admissionCategoryWiseAdmissionList = (List<AdmissionCategoryWiseAdmissionDTO>) peFacade.findLightsByJpql(
                    jpql.toString(), params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error loading admission category wise report: " + e.getMessage());
            admissionCategoryWiseAdmissionList = new ArrayList<>();
            return;
        }

        if (admissionCategoryWiseAdmissionList == null || admissionCategoryWiseAdmissionList.isEmpty()) {
            admissionCategoryWiseAdmissionList = new ArrayList<>();
            return;
        }

        enrichAdmissionCategoryWiseFinancials(admissionCategoryWiseAdmissionList);
    }

    private void enrichAdmissionCategoryWiseFinancials(List<AdmissionCategoryWiseAdmissionDTO> rows) {
        List<Long> encounterIds = rows.stream()
                .filter(dto -> dto != null && dto.getAdmissionId() != null)
                .map(AdmissionCategoryWiseAdmissionDTO::getAdmissionId)
                .collect(Collectors.toList());

        if (encounterIds.isEmpty()) {
            return;
        }

        List<PatientEncounter> encounters = peFacade.findByJpql(
                "SELECT pe FROM PatientEncounter pe WHERE pe.id IN :ids",
                Collections.singletonMap("ids", encounterIds));

        Map<Long, PatientEncounter> encounterById = (encounters == null)
                ? Collections.emptyMap()
                : encounters.stream().collect(Collectors.toMap(PatientEncounter::getId, pe -> pe));

        List<PatientEncounter> allChildren = peFacade.findByJpql(
                "SELECT pe FROM PatientEncounter pe WHERE pe.parentEncounter.id IN :ids AND pe.retired = false",
                Collections.singletonMap("ids", encounterIds));
        Map<Long, List<PatientEncounter>> childrenByParentId = (allChildren == null)
                ? Collections.emptyMap()
                : allChildren.stream()
                        .filter(pe -> pe.getParentEncounter() != null)
                        .collect(Collectors.groupingBy(pe -> pe.getParentEncounter().getId()));

        Map<Long, Bill> finalBillByEncounterId = batchFetchFinalBillsByEncounterIds(encounterIds);
        Map<Long, Double> depositByEncounterId = batchFetchDepositTotalsByEncounterIds(encounterIds);
        Map<Long, Double> paidByCompanyByEncounterId = batchFetchPaidByCompanyByEncounterIds(encounterIds);
        Map<Long, Double> paidByPatientByEncounterId = batchFetchPaidByPatientByEncounterIds(encounterIds);

        for (AdmissionCategoryWiseAdmissionDTO dto : rows) {
            if (dto == null || dto.getAdmissionId() == null) {
                continue;
            }

            Long id = dto.getAdmissionId();
            PatientEncounter pe = encounterById.get(id);
            Bill finalBill = finalBillByEncounterId.get(id);
            List<PatientEncounter> children = childrenByParentId.getOrDefault(id, Collections.emptyList());

            double invoiceAmount;
            double professionalFees = 0.0;
            double hospitalAmount = 0.0;
            double discount = 0.0;
            double sponsorAmount = 0.0;
            double patientAmount = 0.0;

            if (finalBill != null) {
                invoiceAmount = finalBill.getNetTotal();
                professionalFees = finalBill.getProfessionalFee();
                hospitalAmount = finalBill.getHospitalFee();
                discount = finalBill.getDiscount();
                sponsorAmount = finalBill.getSettledAmountBySponsor();
                patientAmount = finalBill.getSettledAmountByPatient();
            } else if (pe != null) {
                invoiceAmount = inwardBeanController.calculateInwardTotal(pe, children);
                discount = pe.getDiscount();
            } else {
                invoiceAmount = 0.0;
            }

            if (sponsorAmount == 0.0 && patientAmount == 0.0 && invoiceAmount > 0.0) {
                if (dto.getPaymentMethod() == PaymentMethod.Credit) {
                    sponsorAmount = invoiceAmount;
                } else {
                    patientAmount = invoiceAmount;
                }
            }

            double advance = depositByEncounterId.getOrDefault(id, 0.0);
            double paidByCompany = paidByCompanyByEncounterId.getOrDefault(id, 0.0);
            double paidByPatient = paidByPatientByEncounterId.getOrDefault(id, 0.0);
            double totalCollected = advance + paidByCompany;

            dto.setAdvance(advance);
            dto.setProfessionalFees(professionalFees);
            dto.setHospitalAmount(hospitalAmount);
            dto.setSponsorAmount(sponsorAmount);
            dto.setPatientAmount(patientAmount);
            dto.setDiscount(discount);
            dto.setInvoiceAmount(invoiceAmount);
            dto.setBillBalance(Math.max(0.0, invoiceAmount - totalCollected));
            dto.setPatientBalance(Math.max(0.0, patientAmount - paidByPatient));
        }
    }

    private Map<Long, Bill> batchFetchFinalBillsByEncounterIds(List<Long> encounterIds) {
        if (encounterIds == null || encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT b FROM BilledBill b "
                + "WHERE b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billType = :bt "
                + "AND b.patientEncounter.id IN :ids "
                + "ORDER BY b.patientEncounter.id, b.id DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.InwardFinalBill);
        params.put("ids", encounterIds);

        List<Bill> bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        Map<Long, Bill> result = new HashMap<>();
        if (bills != null) {
            for (Bill bill : bills) {
                if (bill.getPatientEncounter() != null && bill.getPatientEncounter().getId() != null) {
                    result.putIfAbsent(bill.getPatientEncounter().getId(), bill);
                }
            }
        }
        return result;
    }

    private Map<Long, Double> batchFetchDepositTotalsByEncounterIds(List<Long> encounterIds) {
        if (encounterIds == null || encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT b.patientEncounter.id, SUM(ABS(p.paidValue)) "
                + "FROM Payment p "
                + "JOIN p.bill b "
                + "WHERE p.retired = false "
                + "AND b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.patientEncounter.id IN :ids "
                + "GROUP BY b.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_DEPOSIT);
        params.put("ids", encounterIds);

        return mapEncounterDoubleAggregate(billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP));
    }

    private Map<Long, Double> batchFetchPaidByCompanyByEncounterIds(List<Long> encounterIds) {
        if (encounterIds == null || encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT b.patientEncounter.id, SUM(b.netTotal) "
                + "FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billTypeAtomic IN :bts "
                + "AND b.patientEncounter.id IN :ids "
                + "GROUP BY b.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("bts", Arrays.asList(
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION));
        params.put("ids", encounterIds);

        return mapEncounterDoubleAggregate(billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP));
    }

    private Map<Long, Double> batchFetchPaidByPatientByEncounterIds(List<Long> encounterIds) {
        if (encounterIds == null || encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT b.patientEncounter.id, SUM(b.netTotal) "
                + "FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billType = :btp "
                + "AND b.paymentMethod <> :pm "
                + "AND b.patientEncounter.id IN :ids "
                + "GROUP BY b.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardPaymentBill);
        params.put("pm", PaymentMethod.Credit);
        params.put("ids", encounterIds);

        return mapEncounterDoubleAggregate(billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP));
    }

    private Map<Long, Double> mapEncounterDoubleAggregate(List<Object[]> rows) {
        Map<Long, Double> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            Double value = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            result.put(id, Math.max(0.0, value));
        }
        return result;
    }

    public StreamedContent getAdmissionCategoryWiseAdmissionExcel() {
        if (admissionCategoryWiseAdmissionList == null || admissionCategoryWiseAdmissionList.isEmpty()) {
            JsfUtil.addErrorMessage("No data available to export.");
            return null;
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Admission Category Wise");

            String[] headers = {
                "No", "BHT", "Patient Name", "Admission Category", "Advance",
                "Professional Fees", "Hospital Amount", "Sponsor Amount", "Patient Amount",
                "Discount", "Invoice Amount", "Bill Balance", "Patient Balance"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            CreationHelper helper = wb.getCreationHelper();
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

            int rowNum = 1;
            int idx = 1;
            for (AdmissionCategoryWiseAdmissionDTO dto : admissionCategoryWiseAdmissionList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(dto.getBhtNo() != null ? dto.getBhtNo() : "");
                row.createCell(2).setCellValue(dto.getPatientName() != null ? dto.getPatientName() : "");
                row.createCell(3).setCellValue(dto.getCategoryName() != null ? dto.getCategoryName() : "");

                for (int col = 4; col <= 12; col++) {
                    Cell moneyCell = row.createCell(col);
                    moneyCell.setCellStyle(moneyStyle);
                }
                row.getCell(4).setCellValue(dto.getAdvance());
                row.getCell(5).setCellValue(dto.getProfessionalFees());
                row.getCell(6).setCellValue(dto.getHospitalAmount());
                row.getCell(7).setCellValue(dto.getSponsorAmount());
                row.getCell(8).setCellValue(dto.getPatientAmount());
                row.getCell(9).setCellValue(dto.getDiscount());
                row.getCell(10).setCellValue(dto.getInvoiceAmount());
                row.getCell(11).setCellValue(dto.getBillBalance());
                row.getCell(12).setCellValue(dto.getPatientBalance());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            byte[] bytes = out.toByteArray();
            return DefaultStreamedContent.builder()
                    .name("Admission_Category_Wise_Admission.xlsx")
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

    public void downloadAdmissionCategoryWiseAdmissionPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        String dates = CommonFunctions.dateRangeForFileName(
                fromDate, toDate,
                sessionController.getApplicationPreference().getLongDateFormat());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        SimpleDateFormat sdt = new SimpleDateFormat("dd MMM yyyy HH:mm");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(com.lowagie.text.PageSize.A4.rotate());
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            String institutionName = sessionController.getInstitution() != null
                    ? sessionController.getInstitution().getName()
                    : "No Logged Institution";

            document.add(new Paragraph(institutionName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Admission Category Wise Admission Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Date: " + sdf.format(new Date()), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            if (admissionCategoryWiseAdmissionList == null || admissionCategoryWiseAdmissionList.isEmpty()) {
                document.add(new Paragraph("No admissions for the selected criteria.",
                        FontFactory.getFont(FontFactory.HELVETICA, 12)));
                document.close();
                context.responseComplete();
                return;
            }

            PdfPTable infoTable = buildAdmissionCategoryWiseInfoTable(sdt);
            if (infoTable != null) {
                document.add(infoTable);
            }

            PdfPTable table = new PdfPTable(13);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            float[] columnWidths = {0.8f, 1.5f, 2.5f, 1.8f, 1.2f, 1.4f, 1.4f, 1.3f, 1.3f, 1.1f, 1.3f, 1.3f, 1.3f};
            table.setWidths(columnWidths);

            addAdmissionCategoryWiseHeaderRow(table);

            int idx = 1;
            for (AdmissionCategoryWiseAdmissionDTO row : admissionCategoryWiseAdmissionList) {
                addAdmissionCategoryWiseRow(table, row, idx++);
            }

            document.add(table);
            document.close();

            byte[] bytes = baos.toByteArray();
            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=Admission_Category_Wise_Admission_" + dates + ".pdf");
            response.setContentLength(bytes.length);

            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }

            context.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
        }
    }

    private PdfPTable buildAdmissionCategoryWiseInfoTable(SimpleDateFormat sdt) throws DocumentException {
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(60);
        info.setSpacingBefore(5);
        info.setWidths(new float[]{1f, 2f});

        addInfoCell(info, "Institution:", institution != null ? institution.getName() : "All");
        addInfoCell(info, "Site:", site != null ? site.getName() : "All");
        addInfoCell(info, "Department:", department != null ? department.getName() : "All");
        addInfoCell(info, "Admission Category:", admissionType != null ? admissionType.getName() : "All");
        addInfoCell(info, "From Date:", fromDate != null ? sdt.format(fromDate) : "-");
        addInfoCell(info, "To Date:", toDate != null ? sdt.format(toDate) : "-");
        addInfoCell(info, "Generated:", sdt.format(new Date()));
        return info;
    }

    private void addAdmissionCategoryWiseHeaderRow(PdfPTable table) {
        java.awt.Color headerBg = new java.awt.Color(33, 37, 41);
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.WHITE);

        String[] headers = {
            "#", "BHT", "Patient Name", "Category", "Advance", "Prof. Fees", "Hospital",
            "Sponsor", "Patient", "Discount", "Invoice", "Bill Bal.", "Patient Bal."
        };

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    private void addAdmissionCategoryWiseRow(PdfPTable table, AdmissionCategoryWiseAdmissionDTO row, int idx) {
        com.lowagie.text.Font normal = FontFactory.getFont(FontFactory.HELVETICA, 7);

        table.addCell(new Phrase(String.valueOf(idx), normal));
        table.addCell(new Phrase(nullSafe(row.getBhtNo()), normal));
        table.addCell(new Phrase(nullSafe(row.getPatientName()), normal));
        table.addCell(new Phrase(nullSafe(row.getCategoryName()), normal));
        table.addCell(new Phrase(formatAmount(row.getAdvance()), normal));
        table.addCell(new Phrase(formatAmount(row.getProfessionalFees()), normal));
        table.addCell(new Phrase(formatAmount(row.getHospitalAmount()), normal));
        table.addCell(new Phrase(formatAmount(row.getSponsorAmount()), normal));
        table.addCell(new Phrase(formatAmount(row.getPatientAmount()), normal));
        table.addCell(new Phrase(formatAmount(row.getDiscount()), normal));
        table.addCell(new Phrase(formatAmount(row.getInvoiceAmount()), normal));
        table.addCell(new Phrase(formatAmount(row.getBillBalance()), normal));
        table.addCell(new Phrase(formatAmount(row.getPatientBalance()), normal));
    }

    public void downloadIpUnsettledInvoicesPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        String dates = CommonFunctions.dateRangeForFileName(
                fromDate, toDate,
                sessionController.getApplicationPreference().getLongDateFormat());

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=IP_Unsettled_Invoices_" + dates + ".pdf");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        SimpleDateFormat sdt = new SimpleDateFormat("dd MMM yyyy HH:mm");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(com.lowagie.text.PageSize.A4.rotate());
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            String institutionName = sessionController.getInstitution() != null
                    ? sessionController.getInstitution().getName()
                    : "No Logged Institution";

            document.add(new Paragraph(institutionName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("IP Unsettled Invoices Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Date: " + sdf.format(new Date()), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            if (unsettledInvoicesList == null || unsettledInvoicesList.isEmpty()) {
                document.add(new Paragraph("No unsettled invoices for the selected criteria.",
                        FontFactory.getFont(FontFactory.HELVETICA, 12)));
                document.close();
                context.responseComplete();
                return;
            }

            PdfPTable infoTable = buildIpUnsettledInfoTable(sdf, sdt);
            if (infoTable != null) {
                document.add(infoTable);
            }

            PdfPTable table = new PdfPTable(12);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            float[] columnWidths = {1.2f, 2.2f, 3.5f, 2.2f, 1.2f, 2.8f, 2.2f, 1.8f, 2.2f, 2.2f, 2.2f, 2.8f};
            table.setWidths(columnWidths);

            addIpUnsettledHeaderRow(table);

            double totalNet = 0.0;
            double totalCollected = 0.0;
            double totalDue = 0.0;

            int idx = 1;
            for (IpUnsettledInvoiceDTO row : unsettledInvoicesList) {
                addIpUnsettledRow(table, row, idx++, sdt);

                totalNet += row.getNetTotal() != null ? row.getNetTotal() : 0.0;
                totalCollected += row.getCreditPaidAmount() != null ? row.getCreditPaidAmount() : 0.0;
                totalDue += row.getAmountToBePaid() != null ? row.getAmountToBePaid() : 0.0;
            }

            addIpUnsettledGrandTotalRow(table, totalNet, totalCollected, totalDue);
            document.add(table);

            document.close();
            context.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
        }
    }

    private PdfPTable buildIpUnsettledInfoTable(SimpleDateFormat sdf, SimpleDateFormat sdt) throws DocumentException {
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(60);
        info.setSpacingBefore(5);
        info.setWidths(new float[]{1f, 2f});

        addInfoCell(info, "Institution:", institution != null ? institution.getName() : "All");
        addInfoCell(info, "Site:", site != null ? site.getName() : "All");
        addInfoCell(info, "Department:", department != null ? department.getName() : "All");
        addInfoCell(info, "From Date:", fromDate != null ? sdt.format(fromDate) : "-");
        addInfoCell(info, "To Date:", toDate != null ? sdt.format(toDate) : "-");
        addInfoCell(info, "Generated:", sdt.format(new Date()));
        return info;
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        com.lowagie.text.Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        com.lowagie.text.Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPCell c1 = new PdfPCell(new Phrase(label, bold));
        c1.setBorder(0);
        c1.setPadding(2);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "", normal));
        c2.setBorder(0);
        c2.setPadding(2);
        table.addCell(c2);
    }

    private void addIpUnsettledHeaderRow(PdfPTable table) {
        java.awt.Color headerBg = new java.awt.Color(33, 37, 41);
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);

        String[] headers = {
            "#", "MRN", "Patient Name", "Mobile", "Age", "Location",
            "Discharged On", "Status", "Total", "Collected", "Due", "Discharged By"
        };

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private void addIpUnsettledRow(PdfPTable table, IpUnsettledInvoiceDTO row, int idx, SimpleDateFormat sdt) {
        com.lowagie.text.Font normal = FontFactory.getFont(FontFactory.HELVETICA, 8);

        table.addCell(new Phrase(String.valueOf(idx), normal));
        table.addCell(new Phrase(nullSafe(row.getPhn()), normal));
        table.addCell(new Phrase(nullSafe(row.getPatientNameWithTitle()), normal));
        table.addCell(new Phrase(nullSafe(row.getMobileNumber()), normal));
        table.addCell(new Phrase(row.getAge() != null ? row.getAge().toString() : "", normal));

        String location = "";
        if (row.getRoomCategoryName() != null
                && row.getRoomCategoryName().getRoomFacilityCharge() != null
                && row.getRoomCategoryName().getRoomFacilityCharge().getName() != null) {
            location = row.getRoomCategoryName().getRoomFacilityCharge().getName();
        }
        table.addCell(new Phrase(location, normal));

        table.addCell(new Phrase(row.getDateOfDischarge() != null ? sdt.format(row.getDateOfDischarge()) : "", normal));
        table.addCell(new Phrase(nullSafe(row.getPaymentStatusLabel()), normal));
        table.addCell(new Phrase(formatAmount(row.getNetTotal()), normal));
        table.addCell(new Phrase(formatAmount(row.getCreditPaidAmount()), normal));
        table.addCell(new Phrase(formatAmount(row.getAmountToBePaid()), normal));

        String dischargedBy = row.getCreaterName() != null ? nullSafe(row.getCreaterName().getName()) : "";
        table.addCell(new Phrase(dischargedBy, normal));
    }

    private void addIpUnsettledGrandTotalRow(PdfPTable table, double totalNet, double totalCollected, double totalDue) {
        com.lowagie.text.Font boldWhite = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);
        java.awt.Color bg = new java.awt.Color(52, 58, 64);

        PdfPCell label = new PdfPCell(new Phrase("Grand Total", boldWhite));
        label.setColspan(8);
        label.setHorizontalAlignment(Element.ALIGN_RIGHT);
        label.setBackgroundColor(bg);
        label.setPadding(4);
        table.addCell(label);

        table.addCell(makeTotalCell(totalNet, boldWhite, bg));
        table.addCell(makeTotalCell(totalCollected, boldWhite, bg));
        table.addCell(makeTotalCell(totalDue, boldWhite, bg));
        table.addCell(makeTotalCell("", boldWhite, bg));
    }

    private PdfPCell makeTotalCell(double value, com.lowagie.text.Font font, java.awt.Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(formatAmount(value), font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell makeTotalCell(String value, com.lowagie.text.Font font, java.awt.Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        return cell;
    }

    private String formatAmount(Double v) {
        return String.format("%,.2f", v != null ? v : 0.0);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    public StreamedContent getIpUnsettledInvoicesExcel() {
        if (unsettledInvoicesList == null || unsettledInvoicesList.isEmpty()) {
            JsfUtil.addErrorMessage("No data available to export.");
            return null;
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("IP Unsettled Invoices");

            String[] headers = {
                "SI No", "MRN", "Patient Name", "Mobile", "Age", "Location",
                "Discharged On", "Status", "Total", "Collected", "Due", "Discharged By"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            CreationHelper helper = wb.getCreationHelper();
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd-MMM-yyyy HH:mm"));
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

            int rowNum = 1;
            int idx = 1;
            for (IpUnsettledInvoiceDTO dto : unsettledInvoicesList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(dto.getPhn() != null ? dto.getPhn() : "");
                row.createCell(2).setCellValue(dto.getPatientNameWithTitle() != null ? dto.getPatientNameWithTitle() : "");
                row.createCell(3).setCellValue(dto.getMobileNumber() != null ? dto.getMobileNumber() : "");
                row.createCell(4).setCellValue(dto.getAge() != null ? dto.getAge() : 0);

                String location = "";
                if (dto.getRoomCategoryName() != null
                        && dto.getRoomCategoryName().getRoomFacilityCharge() != null
                        && dto.getRoomCategoryName().getRoomFacilityCharge().getName() != null) {
                    location = dto.getRoomCategoryName().getRoomFacilityCharge().getName();
                }
                row.createCell(5).setCellValue(location);

                Cell dischargeCell = row.createCell(6);
                if (dto.getDateOfDischarge() != null) {
                    dischargeCell.setCellValue(dto.getDateOfDischarge());
                    dischargeCell.setCellStyle(dateStyle);
                }

                row.createCell(7).setCellValue(dto.getPaymentStatusLabel() != null ? dto.getPaymentStatusLabel() : "");

                Cell totalCell = row.createCell(8);
                totalCell.setCellValue(dto.getNetTotal() != null ? dto.getNetTotal() : 0.0);
                totalCell.setCellStyle(moneyStyle);

                Cell collectedCell = row.createCell(9);
                collectedCell.setCellValue(dto.getCreditPaidAmount() != null ? dto.getCreditPaidAmount() : 0.0);
                collectedCell.setCellStyle(moneyStyle);

                Cell dueCell = row.createCell(10);
                dueCell.setCellValue(dto.getAmountToBePaid() != null ? dto.getAmountToBePaid() : 0.0);
                dueCell.setCellStyle(moneyStyle);

                String dischargedBy = dto.getCreaterName() != null ? dto.getCreaterName().getName() : "";
                row.createCell(11).setCellValue(dischargedBy);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            byte[] bytes = out.toByteArray();
            return DefaultStreamedContent.builder()
                    .name("IP_Unsettled_Invoices.xlsx")
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
        String dateField = "admissionDate".equals(dateBasis) ? "b.dateOfAdmission" : "b.createdAt";
        String sql = "select b from PatientEncounter b "
                + " where " + dateField + " between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.institution =:inst ";
            m.put("inst", institution);
        }

        if (site != null) {
            sql += " and b.department.site =:site ";
            m.put("site", site);
        }

        if (department != null) {
            sql += " and b.department =:dept ";
            m.put("dept", department);
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
        String dateField = "admissionDate".equals(dateBasis) ? "b.dateOfAdmission" : "b.createdAt";
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and " + dateField + " between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (site != null) {
            sql += " and b.department.site =:site ";
            m.put("site", site);
        }

        if (department != null) {
            sql += " and b.department =:dept ";
            m.put("dept", department);
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

        String dateField = "admissionDate".equals(dateBasis) ? "b.dateOfAdmission" : "b.dateOfDischarge";
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and " + dateField + " between :fd and :td ";

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

        String dateField = "admissionDate".equals(dateBasis) ? "b.dateOfAdmission" : "b.dateOfDischarge";
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and " + dateField + " between :fd and :td ";

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

    public void clearDepartment() {
        this.department = null;
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

    public List<AdmissionCategoryWiseAdmissionDTO> getAdmissionCategoryWiseAdmissionList() {
        return admissionCategoryWiseAdmissionList;
    }

    public void setAdmissionCategoryWiseAdmissionList(List<AdmissionCategoryWiseAdmissionDTO> admissionCategoryWiseAdmissionList) {
        this.admissionCategoryWiseAdmissionList = admissionCategoryWiseAdmissionList;
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

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean isWithProfessionalFee() {
        return withProfessionalFee;
    }

    public void setWithProfessionalFee(boolean withProfessionalFee) {
        this.withProfessionalFee = withProfessionalFee;
    }

    public double getIpIncomeTotalSponsorPay() {
        return ipIncomeTotalSponsorPay;
    }

    public void setIpIncomeTotalSponsorPay(double ipIncomeTotalSponsorPay) {
        this.ipIncomeTotalSponsorPay = ipIncomeTotalSponsorPay;
    }

    public double getIpIncomeTotalPatientPay() {
        return ipIncomeTotalPatientPay;
    }

    public void setIpIncomeTotalPatientPay(double ipIncomeTotalPatientPay) {
        this.ipIncomeTotalPatientPay = ipIncomeTotalPatientPay;
    }

    public ReportTemplateRowBundle getBundle() {
        return bundle;
    }

    public void setBundle(ReportTemplateRowBundle bundle) {
        this.bundle = bundle;
    }

    public List<RoomCategory> getRoomCategories() {
        return roomCategories;
    }

    public void setRoomCategories(List<RoomCategory> roomCategories) {
        this.roomCategories = roomCategories;
    }

    public double getIpIncomeCashTotal() {
        return ipIncomeCashTotal;
    }

    public void setIpIncomeCashTotal(double ipIncomeCashTotal) {
        this.ipIncomeCashTotal = ipIncomeCashTotal;
    }

    public double getIpIncomeCreditTotal() {
        return ipIncomeCreditTotal;
    }

    public void setIpIncomeCreditTotal(double ipIncomeCreditTotal) {
        this.ipIncomeCreditTotal = ipIncomeCreditTotal;
    }

    public List<Map<String, Object>> getIpIncomeBillDiscounts() {
        return ipIncomeBillDiscounts;
    }

    public void setIpIncomeBillDiscounts(List<Map<String, Object>> ipIncomeBillDiscounts) {
        this.ipIncomeBillDiscounts = ipIncomeBillDiscounts;
    }

    public double getIpIncomeTotalBillDiscount() {
        return ipIncomeTotalBillDiscount;
    }

    public void setIpIncomeTotalBillDiscount(double ipIncomeTotalBillDiscount) {
        this.ipIncomeTotalBillDiscount = ipIncomeTotalBillDiscount;
    }

    public List<AdmissionDischargeDTO> getAdmissionDischargesList() {
        return admissionDischargesList;
    }

    public void setAdmissionDischargesList(List<AdmissionDischargeDTO> admissionDischargesList) {
        this.admissionDischargesList = admissionDischargesList;
    }

    public List<RoomCategory> getAllRoomCategories() {
        return allRoomCategories;
    }

    public void setAllRoomCategories(List<RoomCategory> allRoomCategories) {
        this.allRoomCategories = allRoomCategories;
    }

    public List<RoomCategory> getSelectedRoomCategories() {
        return selectedRoomCategories;
    }

    public void setSelectedRoomCategories(List<RoomCategory> selectedRoomCategories) {
        this.selectedRoomCategories = selectedRoomCategories;
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

    public void setVatRegNo(String vatRegNo) {
        this.vatRegNo = vatRegNo;
    }

    public String getTheatreOccupancyStatusLabel(TheatreOccupancyStatus status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case SCHEDULED: return "Scheduled";
            case SENT_TO_THEATRE: return "Sent to Theatre";
            case RECEIVED_IN_THEATRE: return "Received in Theatre";
            case IN_THEATRE: return "In Theatre";
            case PROCEDURE_COMPLETED: return "Procedure Completed";
            case IN_RECOVERY: return "In Recovery";
            case RETURNED_TO_WARD: return "Returned to Ward";
            case CANCELLED: return "Cancelled";
            default: return status.name();
        }
    }

    public void processSurgeryCostEstimationReport() {
        surgeryCostEstimationList = new ArrayList<>();
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT new com.divudi.core.data.dto.SurgeryCostEstimationDTO(")
            .append("  sb.id, proc.id, ")
            .append("  admission.patient.phn, admission.patient.person.name, ")
            .append("  admission.bhtNo, admission.dateOfAdmission, ")
            .append("  room.name, ")
            .append("  item.name, cat.name ")
            .append(") ")
            .append("FROM BilledBill sb ")
            .append("JOIN sb.procedure proc ")
            .append("JOIN proc.item item ")
            .append("LEFT JOIN item.category cat ")
            .append("JOIN sb.patientEncounter admission ")
            .append("LEFT JOIN admission.currentPatientRoom room ")
            .append("WHERE sb.retired = false ")
            .append("  AND sb.cancelled = false ")
            .append("  AND sb.billType = :surgeryBillType ")
            .append("  AND admission.discharged = true ")
            .append("  AND admission.dateOfDischarge BETWEEN :fromDate AND :toDate ");

        params.put("surgeryBillType", BillType.SurgeryBill);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        jpql.append("  AND NOT EXISTS ( ")
            .append("      SELECT cb.id FROM CancelledBill cb ")
            .append("      WHERE cb.retired = false ")
            .append("        AND cb.billedBill = sb ")
            .append("  ) ");

        if (institution != null) {
            jpql.append(" AND sb.institution = :institution ");
            params.put("institution", institution);
        }
        if (site != null) {
            jpql.append(" AND sb.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" AND sb.department = :department ");
            params.put("department", department);
        }
        if (surgeryType != null) {
            jpql.append(" AND item.category = :surgeryType ");
            params.put("surgeryType", surgeryType);
        }
        if (surgeryItem != null) {
            jpql.append(" AND item = :surgeryItem ");
            params.put("surgeryItem", surgeryItem);
        }
//        if (selectedPatient != null) {
//            jpql.append(" AND admission.patient.person.name = :selectedPatient ");
//            params.put("selectedPatient", selectedPatient.getPatientName());
//        }
        if (selectedPatient != null) {
            String searchTerm = null;
            if (selectedPatient.getPatientName() != null
                    && !selectedPatient.getPatientName().trim().isEmpty()) {
                searchTerm = selectedPatient.getPatientName().trim().toLowerCase();
            } else if (selectedPatient.getPhn() != null
                    && !selectedPatient.getPhn().trim().isEmpty()) {
                searchTerm = selectedPatient.getPhn().trim().toLowerCase();
            }
            if (searchTerm != null) {
                jpql.append("AND (LOWER(admission.patient.person.name) LIKE :pn ")
                        .append("OR LOWER(admission.patient.phn) LIKE :pn) ");
                params.put("pn", "%" + searchTerm + "%");
            }
        }
        if (selectedAdmitDoctor != null) {
            jpql.append(" AND admission.referringDoctor = :selectedAdmitDoctor ");
            params.put("selectedAdmitDoctor", selectedAdmitDoctor);
        }
        if (selectedSurgeon != null) {
            jpql.append(" AND EXISTS (SELECT ec.id FROM EncounterComponent ec ")
                .append("   WHERE ec.patientEncounter = proc AND ec.retired = false ")
                .append("     AND ec.patientEncounterComponentType = com.divudi.core.data.inward.PatientEncounterComponentType.Performed_By ")
                .append("     AND ec.staff = :selectedSurgeon) ");
            params.put("selectedSurgeon", selectedSurgeon);
        }
        if (selectedAssistantSurgeon != null) {
            jpql.append(" AND EXISTS (SELECT ec.id FROM EncounterComponent ec ")
                .append("   WHERE ec.patientEncounter = proc AND ec.retired = false ")
                .append("     AND ec.patientEncounterComponentType = com.divudi.core.data.inward.PatientEncounterComponentType.Assisted_by ")
                .append("     AND ec.staff = :selectedAssistantSurgeon) ");
            params.put("selectedAssistantSurgeon", selectedAssistantSurgeon);
        }
        if (selectedOtRoom != null) {
            jpql.append(" AND EXISTS (SELECT ptr.id FROM PatientTransferRequest ptr ")
                .append("   WHERE ptr.surgeryBill = sb AND ptr.retired = false ")
                .append("     AND ptr.toRoomFacilityCharge = :selectedOtRoom) ");
            params.put("selectedOtRoom", selectedOtRoom);
        }
        if (selectedSurgeryStatus != null) {
            jpql.append(" AND EXISTS (SELECT ptr.id FROM PatientTransferRequest ptr ")
                .append("   WHERE ptr.surgeryBill = sb AND ptr.retired = false ")
                .append("     AND ptr.theatreOccupancyStatus = :selectedSurgeryStatus) ");
            params.put("selectedSurgeryStatus", selectedSurgeryStatus);
        }

        jpql.append(" ORDER BY admission.dateOfDischarge ASC ");

        List<SurgeryCostEstimationDTO> list = (List<SurgeryCostEstimationDTO>) billFacade.findDTOsByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP);

        if (list == null || list.isEmpty()) {
            JsfUtil.addErrorMessage("No records found.");
            return;
        }

        List<Long> billIds = new ArrayList<>();
        List<Long> procIds = new ArrayList<>();
        Map<Long, SurgeryCostEstimationDTO> dtoByBillId = new HashMap<>();
        Map<Long, List<SurgeryCostEstimationDTO>> dtosByProcId = new HashMap<>();

        for (SurgeryCostEstimationDTO dto : list) {
            billIds.add(dto.getSurgeryBillId());
            procIds.add(dto.getProcedureId());
            dtoByBillId.put(dto.getSurgeryBillId(), dto);
            
            if (!dtosByProcId.containsKey(dto.getProcedureId())) {
                dtosByProcId.put(dto.getProcedureId(), new ArrayList<SurgeryCostEstimationDTO>());
            }
            dtosByProcId.get(dto.getProcedureId()).add(dto);

            dto.setTotalHospitalCharge(0.0);
            dto.setProfessionalCharge(0.0);
            dto.setTotalAmount(0.0);
            dto.setBillDiscount(0.0);
            dto.setNetAmount(0.0);
        }

        // 1. Enrich Surgeons & Assistants
        if (!procIds.isEmpty()) {
            String ecJpql = "SELECT ec FROM EncounterComponent ec "
                    + "WHERE ec.retired = false AND ec.patientEncounter.id IN :procIds "
                    + "AND ec.patientEncounterComponentType IN (:perfType, :asstType)";
            Map<String, Object> ecParams = new HashMap<>();
            ecParams.put("procIds", procIds);
            ecParams.put("perfType", PatientEncounterComponentType.Performed_By);
            ecParams.put("asstType", PatientEncounterComponentType.Assisted_by);
            List<EncounterComponent> ecList = encounterComponentFacade.findByJpql(ecJpql, ecParams);

            if (ecList != null) {
                for (EncounterComponent ec : ecList) {
                    Long procId = ec.getPatientEncounter().getId();
                    List<SurgeryCostEstimationDTO> targetDtos = dtosByProcId.get(procId);
                    if (targetDtos != null) {
                        String staffName = "";
                        if (ec.getStaff() != null) {
                            if (ec.getStaff().getPerson() != null) {
                                staffName = ec.getStaff().getPerson().getNameWithTitle();
                            } else {
                                staffName = ec.getStaff().getName();
                            }
                        }
                        for (SurgeryCostEstimationDTO dto : targetDtos) {
                            if (ec.getPatientEncounterComponentType() == PatientEncounterComponentType.Performed_By) {
                                dto.setSurgeonName(staffName);
                            } else if (ec.getPatientEncounterComponentType() == PatientEncounterComponentType.Assisted_by) {
                                dto.setAssistantSurgeonName(staffName);
                            }
                        }
                    }
                }
            }
        }

        // 2. Enrich OT Room & Surgery Status
        if (!billIds.isEmpty()) {
            String ptrJpql = "SELECT ptr FROM PatientTransferRequest ptr "
                    + "WHERE ptr.retired = false AND ptr.surgeryBill.id IN :billIds "
                    + "ORDER BY ptr.id ASC";
            Map<String, Object> ptrParams = new HashMap<>();
            ptrParams.put("billIds", billIds);
            List<PatientTransferRequest> ptrList = patientTransferRequestFacade.findByJpql(ptrJpql, ptrParams);

            if (ptrList != null) {
                Map<Long, PatientTransferRequest> latestPtrMap = new HashMap<>();
                for (PatientTransferRequest ptr : ptrList) {
                    if (ptr.getSurgeryBill() != null) {
                        latestPtrMap.put(ptr.getSurgeryBill().getId(), ptr);
                    }
                }

                for (Map.Entry<Long, PatientTransferRequest> entry : latestPtrMap.entrySet()) {
                    SurgeryCostEstimationDTO dto = dtoByBillId.get(entry.getKey());
                    if (dto != null) {
                        PatientTransferRequest ptr = entry.getValue();
                        if (ptr.getToRoomFacilityCharge() != null) {
                            dto.setOtRoomName(ptr.getToRoomFacilityCharge().getName());
                        }
                        if (ptr.getTheatreOccupancyStatus() != null) {
                            dto.setSurgeryStatusLabel(getTheatreOccupancyStatusLabel(ptr.getTheatreOccupancyStatus()));
                        }
                    }
                }
            }
        }

        // 3. Aggregate Child Bills (Charges)
        if (!billIds.isEmpty()) {
            String childJpql = "SELECT b FROM Bill b "
                    + "WHERE b.retired = false AND b.cancelled = false "
                    + "AND b.forwardReferenceBill.id IN :billIds";
            Map<String, Object> childParams = new HashMap<>();
            childParams.put("billIds", billIds);
            List<Bill> childBills = billFacade.findByJpql(childJpql, childParams);

            if (childBills != null) {
                for (Bill childBill : childBills) {
                    Long parentId = childBill.getForwardReferenceBill().getId();
                    SurgeryCostEstimationDTO dto = dtoByBillId.get(parentId);
                    if (dto != null) {
                        double net = childBill.getNetTotal();
                        double discount = childBill.getDiscount();

                        if (childBill.getSurgeryBillType() == com.divudi.core.data.inward.SurgeryBillType.ProfessionalFee) {
                            dto.setProfessionalCharge(dto.getProfessionalCharge() + net);
                        } else if (childBill.getSurgeryBillType() == com.divudi.core.data.inward.SurgeryBillType.Service
                                || childBill.getSurgeryBillType() == com.divudi.core.data.inward.SurgeryBillType.PharmacyItem
                                || childBill.getSurgeryBillType() == com.divudi.core.data.inward.SurgeryBillType.TimedService) {
                            dto.setTotalHospitalCharge(dto.getTotalHospitalCharge() + net);
                        }
                        dto.setBillDiscount(dto.getBillDiscount() + discount);
                    }
                }
            }
        }

        for (SurgeryCostEstimationDTO dto : list) {
            double net = dto.getTotalHospitalCharge() + dto.getProfessionalCharge();
            double total = net + dto.getBillDiscount();
            dto.setNetAmount(net);
            dto.setTotalAmount(total);
        }

        surgeryCostEstimationList = list;
    }

    public void downloadSurgeryCostEstimationExcel() {
        if (surgeryCostEstimationList == null || surgeryCostEstimationList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Surgery Cost Estimation");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // Styles
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle infoStyle = workbook.createCellStyle();
            XSSFFont infoFont = workbook.createFont();
            infoFont.setFontHeightInPoints((short) 9);
            infoStyle.setFont(infoFont);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 9);
            headerFont.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 41, (byte) 128, (byte) 185}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle normalStyle = workbook.createCellStyle();
            XSSFFont normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 8);
            normalStyle.setFont(normalFont);
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(normalStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            XSSFDataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));

            XSSFCellStyle totalStyle = workbook.createCellStyle();
            XSSFFont totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalFont.setFontHeightInPoints((short) 9);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 200, (byte) 100}, null));
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalStyle.setBorderBottom(BorderStyle.MEDIUM);
            totalStyle.setBorderTop(BorderStyle.MEDIUM);
            totalStyle.setBorderLeft(BorderStyle.THIN);
            totalStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle totalNumberStyle = workbook.createCellStyle();
            totalNumberStyle.cloneStyleFrom(totalStyle);
            totalNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalNumberStyle.setDataFormat(format.getFormat("#,##0.00"));

            int rowIdx = 0;

            // Title Row
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(22);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Surgery Cost Estimation Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 15));

            rowIdx++; // Blank row

            // Info rows
            Row infoRow1 = sheet.createRow(rowIdx++);
            infoRow1.createCell(0).setCellValue("From Date:");
            infoRow1.createCell(1).setCellValue(fromDate != null ? sdf.format(fromDate) : "");
            infoRow1.createCell(3).setCellValue("To Date:");
            infoRow1.createCell(4).setCellValue(toDate != null ? sdf.format(toDate) : "");
            for (int col = 0; col < 6; col++) {
                Cell c = infoRow1.getCell(col);
                if (c != null) c.setCellStyle(infoStyle);
            }

            rowIdx++; // Blank row

            // Headers
            String[] headers = {
                "SL No", "MRN", "Patient Name", "Admission No", "Admission Date", "Bed No",
                "Surgeon", "Surgery Type", "Service Name", "Room Charges", "Drug Charges",
                "Total Hospital Charge", "Professional Charge", "Total Amount", "Bill Discount", "Net Amount"
            };

            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(25);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            double grandHospital = 0;
            double grandProfessional = 0;
            double grandTotalAmt = 0;
            double grandDiscount = 0;
            double grandNet = 0;

            int slNo = 1;
            for (SurgeryCostEstimationDTO dto : surgeryCostEstimationList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(slNo++);
                row.createCell(1).setCellValue(dto.getPhn());
                row.createCell(2).setCellValue(dto.getPatientName());
                row.createCell(3).setCellValue(dto.getAdmissionNo());
                row.createCell(4).setCellValue(dto.getAdmissionDate() != null ? sdf.format(dto.getAdmissionDate()) : "");
                row.createCell(5).setCellValue(dto.getBedNo());
                row.createCell(6).setCellValue(dto.getSurgeonName());
                row.createCell(7).setCellValue(dto.getSurgeryTypeName());
                row.createCell(8).setCellValue(dto.getServiceName());
                row.createCell(9).setCellValue("N/A");
                row.createCell(10).setCellValue("N/A");

                Cell c11 = row.createCell(11);
                c11.setCellValue(dto.getTotalHospitalCharge() != null ? dto.getTotalHospitalCharge() : 0.0);
                c11.setCellStyle(numberStyle);
                grandHospital += dto.getTotalHospitalCharge() != null ? dto.getTotalHospitalCharge() : 0.0;

                Cell c12 = row.createCell(12);
                c12.setCellValue(dto.getProfessionalCharge() != null ? dto.getProfessionalCharge() : 0.0);
                c12.setCellStyle(numberStyle);
                grandProfessional += dto.getProfessionalCharge() != null ? dto.getProfessionalCharge() : 0.0;

                Cell c13 = row.createCell(13);
                c13.setCellValue(dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0);
                c13.setCellStyle(numberStyle);
                grandTotalAmt += dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0;

                Cell c14 = row.createCell(14);
                c14.setCellValue(dto.getBillDiscount() != null ? dto.getBillDiscount() : 0.0);
                c14.setCellStyle(numberStyle);
                grandDiscount += dto.getBillDiscount() != null ? dto.getBillDiscount() : 0.0;

                Cell c15 = row.createCell(15);
                c15.setCellValue(dto.getNetAmount() != null ? dto.getNetAmount() : 0.0);
                c15.setCellStyle(numberStyle);
                grandNet += dto.getNetAmount() != null ? dto.getNetAmount() : 0.0;

                for (int col = 0; col < 11; col++) {
                    row.getCell(col).setCellStyle(normalStyle);
                }
            }

            // Grand Total Row
            Row totalRow = sheet.createRow(rowIdx++);
            totalRow.setHeightInPoints(20);
            Cell lblCell = totalRow.createCell(0);
            lblCell.setCellValue("Grand Total");
            lblCell.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 10));

            for (int col = 1; col <= 10; col++) {
                totalRow.createCell(col).setCellStyle(totalStyle);
            }

            Cell tc11 = totalRow.createCell(11);
            tc11.setCellValue(grandHospital);
            tc11.setCellStyle(totalNumberStyle);

            Cell tc12 = totalRow.createCell(12);
            tc12.setCellValue(grandProfessional);
            tc12.setCellStyle(totalNumberStyle);

            Cell tc13 = totalRow.createCell(13);
            tc13.setCellValue(grandTotalAmt);
            tc13.setCellStyle(totalNumberStyle);

            Cell tc14 = totalRow.createCell(14);
            tc14.setCellValue(grandDiscount);
            tc14.setCellStyle(totalNumberStyle);

            Cell tc15 = totalRow.createCell(15);
            tc15.setCellValue(grandNet);
            tc15.setCellStyle(totalNumberStyle);

            // Auto fit column widths
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] excelBytes = baos.toByteArray();

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            externalContext.setResponseContentLength(excelBytes.length);
            String fileName = "Surgery_Cost_Estimation_Report_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

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

    public void downloadSurgeryCostEstimationPdf() {
        if (surgeryCostEstimationList == null || surgeryCostEstimationList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        com.lowagie.text.Document document = null;
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");

            String fileName = "Surgery_Cost_Estimation_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();

            document = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A3.rotate(), 15, 15, 30, 20);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy HH:mm");

            Paragraph titlePara = new Paragraph("Surgery Cost Estimation Report", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(10);
            document.add(titlePara);

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(40);
            info.setWidths(new float[]{1.5f, 3.5f});
            info.setSpacingAfter(15);
            info.setHorizontalAlignment(Element.ALIGN_LEFT);

            addInfoRow(info, "From Date:", fromDate != null ? sdf.format(fromDate) : "");
            addInfoRow(info, "To Date:", toDate != null ? sdf.format(toDate) : "");
            document.add(info);

            String[] headers = {
                "SL No", "MRN", "Patient Name", "Admission No", "Admission Date", "Bed No",
                "Surgeon", "Surgery Type", "Service Name", "Room Charges", "Drug Charges",
                "Total Hospital Charge", "Professional Charge", "Total Amount", "Bill Discount", "Net Amount"
            };

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            float[] widths = {2f, 4f, 8f, 5f, 7f, 4f, 7f, 6f, 8f, 4f, 4f, 6f, 6f, 6f, 5f, 6f};
            table.setWidths(widths);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            double grandHospital = 0;
            double grandProfessional = 0;
            double grandTotalAmt = 0;
            double grandDiscount = 0;
            double grandNet = 0;

            int slNo = 1;
            DecimalFormat df = new DecimalFormat("#,##0.00");

            for (SurgeryCostEstimationDTO dto : surgeryCostEstimationList) {
                table.addCell(new Phrase(String.valueOf(slNo++), normalFont));
                table.addCell(new Phrase(dto.getPhn(), normalFont));
                table.addCell(new Phrase(dto.getPatientName(), normalFont));
                table.addCell(new Phrase(dto.getAdmissionNo(), normalFont));
                table.addCell(new Phrase(dto.getAdmissionDate() != null ? sdf.format(dto.getAdmissionDate()) : "", normalFont));
                table.addCell(new Phrase(dto.getBedNo(), normalFont));
                table.addCell(new Phrase(dto.getSurgeonName(), normalFont));
                table.addCell(new Phrase(dto.getSurgeryTypeName(), normalFont));
                table.addCell(new Phrase(dto.getServiceName(), normalFont));
                table.addCell(new Phrase("N/A", normalFont));
                table.addCell(new Phrase("N/A", normalFont));

                double hosp = dto.getTotalHospitalCharge() != null ? dto.getTotalHospitalCharge() : 0.0;
                double prof = dto.getProfessionalCharge() != null ? dto.getProfessionalCharge() : 0.0;
                double tot = dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0;
                double disc = dto.getBillDiscount() != null ? dto.getBillDiscount() : 0.0;
                double net = dto.getNetAmount() != null ? dto.getNetAmount() : 0.0;

                grandHospital += hosp;
                grandProfessional += prof;
                grandTotalAmt += tot;
                grandDiscount += disc;
                grandNet += net;

                PdfPCell cHosp = new PdfPCell(new Phrase(df.format(hosp), normalFont));
                cHosp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cHosp);

                PdfPCell cProf = new PdfPCell(new Phrase(df.format(prof), normalFont));
                cProf.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cProf);

                PdfPCell cTot = new PdfPCell(new Phrase(df.format(tot), normalFont));
                cTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cTot);

                PdfPCell cDisc = new PdfPCell(new Phrase(df.format(disc), normalFont));
                cDisc.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cDisc);

                PdfPCell cNet = new PdfPCell(new Phrase(df.format(net), normalFont));
                cNet.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cNet);
            }

            PdfPCell totalLblCell = new PdfPCell(new Phrase("Grand Total", boldFont));
            totalLblCell.setColspan(11);
            totalLblCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(totalLblCell);

            PdfPCell tgHosp = new PdfPCell(new Phrase(df.format(grandHospital), boldFont));
            tgHosp.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgHosp);

            PdfPCell tgProf = new PdfPCell(new Phrase(df.format(grandProfessional), boldFont));
            tgProf.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgProf);

            PdfPCell tgTot = new PdfPCell(new Phrase(df.format(grandTotalAmt), boldFont));
            tgTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgTot);

            PdfPCell tgDisc = new PdfPCell(new Phrase(df.format(grandDiscount), boldFont));
            tgDisc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgDisc);

            PdfPCell tgNet = new PdfPCell(new Phrase(df.format(grandNet), boldFont));
            tgNet.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgNet);

            document.add(table);
            facesContext.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

}
