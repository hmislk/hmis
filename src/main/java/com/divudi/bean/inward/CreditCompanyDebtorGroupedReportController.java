package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CountedServiceType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.CreditCompanyDebtorGroupDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Controller for the Credit Company Debtor Grouped Report.
 * Produces the same data as the flat debtor report but groups rows by credit
 * company, with per-company subtotals and a grand total.
 */
@Named
@SessionScoped
public class CreditCompanyDebtorGroupedReportController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    BillItemFacade BillItemFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @Inject
    private SessionController sessionController;

    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();
    private String dateBasis = "createdAt";
    private Institution institution;
    private Institution admittingInstitution;
    private Institution site;
    private Department department;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private boolean outstandingOnly = false;

    private List<CreditCompanyDebtorGroupDTO> groups;
    private double grandBillTotal;
    private double grandSettledByCompany;
    private double grandSettledByPatient;
    private double grandPaidTotal;
    private double grandOutstandingTotal;

    public void generateReport() {
        groups = new ArrayList<>();
        grandBillTotal = 0;
        grandSettledByCompany = 0;
        grandSettledByPatient = 0;
        grandPaidTotal = 0;
        grandOutstandingTotal = 0;

        List<Bill> allBills = new ArrayList<>();

        // --- Discharged patients: query INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY ---
        HashMap<String, Object> hm = new HashMap<>();
        String dateField = resolveDateField(dateBasis, "b.billDate", "b.patientEncounter");
        String sql = "Select b from Bill b"
                + " where b.retired=false"
                + " and (b.cancelled=false or b.cancelled is null)"
                + " and b.billTypeAtomic=:bta"
                + " and " + dateField + " between :frm and :to";

        if (institution != null) {
            sql += " and b.creditCompany=:cc";
            hm.put("cc", institution);
        }
        if (admittingInstitution != null) {
            sql += " and b.patientEncounter.institution=:ins";
            hm.put("ins", admittingInstitution);
        }
        if (site != null) {
            sql += " and b.patientEncounter.department.site=:site";
            hm.put("site", site);
        }
        if (department != null) {
            sql += " and b.patientEncounter.department=:dept";
            hm.put("dept", department);
        }
        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType=:at";
            hm.put("at", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod=:pm";
            hm.put("pm", paymentMethod);
        }
        sql += " order by b.creditCompany.name, b.billDate";
        hm.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        List<Bill> dischargedBills = billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        List<BillTypeAtomic> settlementTypes =
                BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_COMPANY);

        for (Bill b : dischargedBills) {
            String settledSql = "Select sum(bi.netValue) from BillItem bi"
                    + " where bi.retired=false"
                    + " and bi.referenceBill=:bill"
                    + " and bi.bill.billTypeAtomic in :types";
            HashMap<String, Object> settledParams = new HashMap<>();
            settledParams.put("bill", b);
            settledParams.put("types", settlementTypes);
            double settled = BillItemFacade.findDoubleByJpql(settledSql, settledParams);
            b.setPaidAmount(settled);
            b.setSettledAmountBySponsor(settled);

            double outstanding = b.getNetTotal() - settled;
            if (outstandingOnly && outstanding <= 0.01) {
                continue;
            }
            allBills.add(b);
        }

        // --- Non-discharged patients ---
        List<BillTypeAtomic> chargeTypes = Arrays.asList(
                BillTypeAtomic.INWARD_SERVICE_BILL,
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BATCH_BILL,
                BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL,
                BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN
        );
        List<BillTypeAtomic> ccPaymentTypes = Arrays.asList(
                BillTypeAtomic.CREDIT_COMPANY_INPATIENT_PAYMENT,
                BillTypeAtomic.CREDIT_COMPANY_INPATIENT_PAYMENT_CANCELLATION,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION
        );
        List<BillTypeAtomic> depositTypes = Arrays.asList(
                BillTypeAtomic.INWARD_DEPOSIT,
                BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION,
                BillTypeAtomic.INWARD_DEPOSIT_REFUND,
                BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION
        );

        String encSql = "Select pe from PatientEncounter pe"
                + " where pe.retired=false"
                + " and (pe.discharged=false or pe.discharged is null)"
                + " and pe.creditCompany is not null"
                + " and pe.dateOfAdmission between :frm and :to";
        HashMap<String, Object> encHm = new HashMap<>();
        if (institution != null) {
            encSql += " and pe.creditCompany=:cc";
            encHm.put("cc", institution);
        }
        if (admittingInstitution != null) {
            encSql += " and pe.institution=:ins";
            encHm.put("ins", admittingInstitution);
        }
        if (site != null) {
            encSql += " and pe.department.site=:site";
            encHm.put("site", site);
        }
        if (department != null) {
            encSql += " and pe.department=:dept";
            encHm.put("dept", department);
        }
        if (admissionType != null) {
            encSql += " and pe.admissionType=:at";
            encHm.put("at", admissionType);
        }
        if (paymentMethod != null) {
            encSql += " and pe.paymentMethod=:pm";
            encHm.put("pm", paymentMethod);
        }
        encSql += " order by pe.creditCompany.name, pe.dateOfAdmission";
        encHm.put("frm", fromDate);
        encHm.put("to", toDate);

        List<PatientEncounter> nonDischargedEncounters =
                patientEncounterFacade.findByJpql(encSql, encHm, TemporalType.TIMESTAMP);

        for (PatientEncounter pe : nonDischargedEncounters) {
            HashMap<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("pe", pe);
            chargeParams.put("chargeTypes", chargeTypes);
            double chargeTotal = billFacade.findDoubleByJpql(
                    "Select sum(b.netTotal) from Bill b where b.retired=false and b.patientEncounter=:pe and b.billTypeAtomic in :chargeTypes",
                    chargeParams);

            HashMap<String, Object> ccPaidParams = new HashMap<>();
            ccPaidParams.put("pe", pe);
            ccPaidParams.put("ccPaymentTypes", ccPaymentTypes);
            double ccPaid = billFacade.findDoubleByJpql(
                    "Select sum(b.netTotal) from Bill b where b.retired=false and b.patientEncounter=:pe and b.billTypeAtomic in :ccPaymentTypes",
                    ccPaidParams);

            HashMap<String, Object> depositParams = new HashMap<>();
            depositParams.put("pe", pe);
            depositParams.put("depositTypes", depositTypes);
            double deposited = billFacade.findDoubleByJpql(
                    "Select sum(b.netTotal) from Bill b where b.retired=false and b.patientEncounter=:pe and b.billTypeAtomic in :depositTypes",
                    depositParams);

            double totalPaid = ccPaid + deposited;
            double outstanding = chargeTotal - totalPaid;

            if (outstandingOnly && outstanding <= 0.01) {
                continue;
            }

            Bill syntheticBill = new Bill();
            syntheticBill.setPatientEncounter(pe);
            syntheticBill.setPatient(pe.getPatient());
            syntheticBill.setCreditCompany(pe.getCreditCompany());
            syntheticBill.setDeptId("(Active)");
            syntheticBill.setBillDate(pe.getDateOfAdmission());
            syntheticBill.setNetTotal(chargeTotal);
            syntheticBill.setSettledAmountBySponsor(ccPaid);
            syntheticBill.setSettledAmountByPatient(deposited);
            syntheticBill.setPaidAmount(totalPaid);
            allBills.add(syntheticBill);
        }

        // --- Group bills by credit company ---
        LinkedHashMap<String, CreditCompanyDebtorGroupDTO> groupMap = new LinkedHashMap<>();
        for (Bill b : allBills) {
            String companyName = b.getCreditCompany() != null ? b.getCreditCompany().getName() : "(Unknown)";
            CreditCompanyDebtorGroupDTO group = groupMap.computeIfAbsent(companyName,
                    CreditCompanyDebtorGroupDTO::new);
            group.addBill(b);
        }

        groups = new ArrayList<>(groupMap.values());
        for (CreditCompanyDebtorGroupDTO g : groups) {
            grandBillTotal += g.getSubBillTotal();
            grandSettledByCompany += g.getSubSettledByCompany();
            grandSettledByPatient += g.getSubSettledByPatient();
            grandPaidTotal += g.getSubTotalPaid();
            grandOutstandingTotal += g.getSubOutstandingTotal();
        }
    }

    public void downloadExcel() {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response =
                (HttpServletResponse) facesContext.getExternalContext().getResponse();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("CC Debtor Grouped");

            // --- Cell styles ---
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            org.apache.poi.ss.usermodel.Font boldWhiteFont = workbook.createFont();
            boldWhiteFont.setBold(true);
            boldWhiteFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle filterLabelStyle = workbook.createCellStyle();
            filterLabelStyle.setFont(boldFont);
            filterLabelStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle filterValueStyle = workbook.createCellStyle();
            filterValueStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(boldWhiteFont);
            setBorders(headerStyle);

            CellStyle groupHeaderStyle = workbook.createCellStyle();
            groupHeaderStyle.setFont(boldFont);
            groupHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            groupHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(groupHeaderStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            setBorders(dataStyle);

            DataFormat dataFormat = workbook.createDataFormat();

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle subtotalStyle = workbook.createCellStyle();
            subtotalStyle.setFont(boldFont);
            subtotalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            subtotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(subtotalStyle);

            CellStyle subtotalNumberStyle = workbook.createCellStyle();
            subtotalNumberStyle.cloneStyleFrom(subtotalStyle);
            subtotalNumberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            subtotalNumberStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle grandTotalStyle = workbook.createCellStyle();
            grandTotalStyle.setFont(boldWhiteFont);
            grandTotalStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            grandTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(grandTotalStyle);

            CellStyle grandTotalNumberStyle = workbook.createCellStyle();
            grandTotalNumberStyle.cloneStyleFrom(grandTotalStyle);
            grandTotalNumberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            grandTotalNumberStyle.setAlignment(HorizontalAlignment.RIGHT);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat dtf = new SimpleDateFormat("dd MMM yyyy HH:mm");

            int COL_COUNT = 11;
            int rowIdx = 0;

            // --- Header rows ---
            String institutionName = sessionController.getInstitution() != null
                    ? sessionController.getInstitution().getName() : "Institution";
            rowIdx = createMergedRow(sheet, rowIdx, institutionName, titleStyle, COL_COUNT);

            rowIdx = createMergedRow(sheet, rowIdx,
                    "Inpatient Credit Company Debtor Report (Grouped)", subtitleStyle, COL_COUNT);

            rowIdx = createMergedRow(sheet, rowIdx,
                    "Printed By: "
                    + (sessionController.getLoggedUser() != null
                       && sessionController.getLoggedUser().getWebUserPerson() != null
                            ? sessionController.getLoggedUser().getWebUserPerson().getName() : "-")
                    + "   at " + dtf.format(new Date()),
                    subtitleStyle, COL_COUNT);

            rowIdx++; // blank row

            // --- Filter details ---
            String dateBasisLabel;
            switch (dateBasis) {
                case "dischargeDate": dateBasisLabel = "Discharge Date"; break;
                case "admissionDate": dateBasisLabel = "Admission Date"; break;
                default:              dateBasisLabel = "Payment / Bill Date"; break;
            }
            rowIdx = createFilterRow(sheet, rowIdx, "Date Basis",
                    dateBasisLabel, filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "From",
                    fromDate != null ? dtf.format(fromDate) : "-",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "To",
                    toDate != null ? dtf.format(toDate) : "-",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "Credit Company",
                    institution != null ? institution.getName() : "All",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "Institution",
                    admittingInstitution != null ? admittingInstitution.getName() : "All",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "Site",
                    site != null ? site.getName() : "All",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "Department",
                    department != null ? department.getName() : "All",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "Admission Type",
                    admissionType != null ? admissionType.getName() : "All",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "Payment Method",
                    paymentMethod != null ? paymentMethod.toString() : "All",
                    filterLabelStyle, filterValueStyle, COL_COUNT);
            rowIdx = createFilterRow(sheet, rowIdx, "Outstanding Only",
                    outstandingOnly ? "Yes" : "No",
                    filterLabelStyle, filterValueStyle, COL_COUNT);

            rowIdx++; // blank row before data

            // --- Column headers ---
            String[] headers = {
                "Bill No", "BHT No", "Patient Name", "Bill Date",
                "Admitted", "Discharged",
                "Bill Total", "Settled by Company", "Settled by Patient",
                "Total Paid", "Outstanding"
            };
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // --- Data rows ---
            for (CreditCompanyDebtorGroupDTO group : groups) {
                // Company group header
                Row groupRow = sheet.createRow(rowIdx++);
                Cell groupCell = groupRow.createCell(0);
                groupCell.setCellValue(group.getCreditCompanyName());
                groupCell.setCellStyle(groupHeaderStyle);
                for (int i = 1; i < COL_COUNT; i++) {
                    groupRow.createCell(i).setCellStyle(groupHeaderStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(
                        groupRow.getRowNum(), groupRow.getRowNum(), 0, COL_COUNT - 1));

                // Bill rows
                for (Bill b : group.getBills()) {
                    Row dataRow = sheet.createRow(rowIdx++);
                    int ci = 0;

                    createStringCell(dataRow, ci++, b.getDeptId(), dataStyle);
                    createStringCell(dataRow, ci++,
                            b.getPatientEncounter() != null ? b.getPatientEncounter().getBhtNo() : "",
                            dataStyle);
                    createStringCell(dataRow, ci++,
                            b.getPatient() != null && b.getPatient().getPerson() != null
                                    ? b.getPatient().getPerson().getName() : "",
                            dataStyle);
                    createStringCell(dataRow, ci++,
                            b.getBillDate() != null ? sdf.format(b.getBillDate()) : "", dataStyle);
                    createStringCell(dataRow, ci++,
                            b.getPatientEncounter() != null && b.getPatientEncounter().getDateOfAdmission() != null
                                    ? sdf.format(b.getPatientEncounter().getDateOfAdmission()) : "",
                            dataStyle);
                    createStringCell(dataRow, ci++,
                            b.getPatientEncounter() != null && b.getPatientEncounter().getDateOfDischarge() != null
                                    ? sdf.format(b.getPatientEncounter().getDateOfDischarge()) : "",
                            dataStyle);
                    createNumberCell(dataRow, ci++, b.getNetTotal(), numberStyle);
                    createNumberCell(dataRow, ci++, b.getSettledAmountBySponsor(), numberStyle);
                    createNumberCell(dataRow, ci++, b.getSettledAmountByPatient(), numberStyle);
                    createNumberCell(dataRow, ci++, b.getPaidAmount(), numberStyle);
                    createNumberCell(dataRow, ci, b.getNetTotal() - b.getPaidAmount(), numberStyle);
                }

                // Subtotal row
                Row subRow = sheet.createRow(rowIdx++);
                int si = 0;
                Cell subLabelCell = subRow.createCell(si++);
                subLabelCell.setCellValue(group.getCreditCompanyName() + " Subtotal");
                subLabelCell.setCellStyle(subtotalStyle);
                for (int i = 1; i < 6; i++) {
                    subRow.createCell(si++).setCellStyle(subtotalStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(
                        subRow.getRowNum(), subRow.getRowNum(), 0, 5));
                createNumberCell(subRow, si++, group.getSubBillTotal(), subtotalNumberStyle);
                createNumberCell(subRow, si++, group.getSubSettledByCompany(), subtotalNumberStyle);
                createNumberCell(subRow, si++, group.getSubSettledByPatient(), subtotalNumberStyle);
                createNumberCell(subRow, si++, group.getSubTotalPaid(), subtotalNumberStyle);
                createNumberCell(subRow, si, group.getSubOutstandingTotal(), subtotalNumberStyle);
            }

            // --- Grand total row ---
            Row grandRow = sheet.createRow(rowIdx++);
            int gi = 0;
            Cell grandLabelCell = grandRow.createCell(gi++);
            grandLabelCell.setCellValue("Grand Total");
            grandLabelCell.setCellStyle(grandTotalStyle);
            for (int i = 1; i < 6; i++) {
                grandRow.createCell(gi++).setCellStyle(grandTotalStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(
                    grandRow.getRowNum(), grandRow.getRowNum(), 0, 5));
            createNumberCell(grandRow, gi++, grandBillTotal, grandTotalNumberStyle);
            createNumberCell(grandRow, gi++, grandSettledByCompany, grandTotalNumberStyle);
            createNumberCell(grandRow, gi++, grandSettledByPatient, grandTotalNumberStyle);
            createNumberCell(grandRow, gi++, grandPaidTotal, grandTotalNumberStyle);
            createNumberCell(grandRow, gi, grandOutstandingTotal, grandTotalNumberStyle);

            // --- Auto-size columns ---
            for (int i = 0; i < COL_COUNT; i++) {
                sheet.autoSizeColumn(i);
            }

            // --- Write response ---
            String filename = "CC_Debtor_Grouped_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }
            facesContext.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int createFilterRow(XSSFSheet sheet, int rowIdx, String label, String value,
            CellStyle labelStyle, CellStyle valueStyle, int colCount) {
        Row row = sheet.createRow(rowIdx);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
        if (colCount > 2) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 1, colCount - 1));
        }
        return rowIdx + 1;
    }

    private int createMergedRow(XSSFSheet sheet, int rowIdx, String value,
            CellStyle style, int colCount) {
        Row row = sheet.createRow(rowIdx);
        Cell cell = row.createCell(0);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        for (int i = 1; i < colCount; i++) {
            row.createCell(i).setCellStyle(style);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));
        return rowIdx + 1;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createStringCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String resolveDateField(String basis, String defaultField, String encounterAlias) {
        if ("dischargeDate".equals(basis)) {
            return encounterAlias + ".dateOfDischarge";
        } else if ("admissionDate".equals(basis)) {
            return encounterAlias + ".dateOfAdmission";
        }
        return defaultField;
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        dateBasis = "createdAt";
        institution = null;
        admittingInstitution = null;
        site = null;
        department = null;
        admissionType = null;
        paymentMethod = null;
        outstandingOnly = false;
        groups = null;
        grandBillTotal = 0;
        grandSettledByCompany = 0;
        grandSettledByPatient = 0;
        grandPaidTotal = 0;
        grandOutstandingTotal = 0;
    }

    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // Getters and setters

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public String getDateBasis() { return dateBasis; }
    public void setDateBasis(String dateBasis) { this.dateBasis = dateBasis; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Institution getAdmittingInstitution() { return admittingInstitution; }
    public void setAdmittingInstitution(Institution admittingInstitution) { this.admittingInstitution = admittingInstitution; }

    public Institution getSite() { return site; }
    public void setSite(Institution site) { this.site = site; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isOutstandingOnly() { return outstandingOnly; }
    public void setOutstandingOnly(boolean outstandingOnly) { this.outstandingOnly = outstandingOnly; }

    public List<CreditCompanyDebtorGroupDTO> getGroups() { return groups; }
    public double getGrandBillTotal() { return grandBillTotal; }
    public double getGrandSettledByCompany() { return grandSettledByCompany; }
    public double getGrandSettledByPatient() { return grandSettledByPatient; }
    public double getGrandPaidTotal() { return grandPaidTotal; }
    public double getGrandOutstandingTotal() { return grandOutstandingTotal; }
}
