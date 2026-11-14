/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.dataStructure.DealerDueDetailRow;
import com.divudi.core.data.dataStructure.InstitutionBillEncounter;
import com.divudi.core.data.dataStructure.InstitutionBills;
import com.divudi.core.data.dataStructure.InstitutionEncounters;
import com.divudi.core.data.reports.CreditReport;
import com.divudi.core.data.reports.FinancialReport;
import com.divudi.core.data.table.String1Value5;

import com.divudi.core.facade.*;
import com.divudi.ejb.CreditBean;
import com.divudi.core.entity.*;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;

import java.io.OutputStream;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javax.faces.context.ExternalContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author safrin
 */
@Named
@SessionScoped
public class CreditCompanyDueController implements Serializable {

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private CashRecieveBillController cashRecieveBillController;
    @EJB
    private CreditBean creditBean;
    @EJB
    BillService billService;

    private Date fromDate;
    private Date toDate;
    /** Date basis for inward due/access reports: "dischargeDate" (default) | "admissionDate" */
    private String dateBasis = "dischargeDate";
    Admission patientEncounter;
    boolean withOutDueUpdate;
    Institution creditCompany;
    private int manageInwardDueAndAccessIndex;
    private int managePharmacyDueAndAccessIndex;
    /// /////////
    private List<InstitutionBills> items;
    private List<InstitutionEncounters> institutionEncounters;
    List<PatientEncounter> patientEncounters;
    private List<String1Value5> creditCompanyAge;
    private List<String1Value5> filteredList;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private EncounterCreditCompanyFacade encounterCreditCompanyFacade;

    @EJB
    AdmissionFacade admissionFacade;
    @EJB
    private ReportTimerController reportTimerController;

    double finalTotal;
    double finalPaidTotal;
    double finalPaidTotalPatient;
    double finalTransPaidTotal;
    double finalTransPaidTotalPatient;
    private double payableByPatient;
    private double payableByCompany;
    private double gopAmount;

    private Institution institutionOfDepartment;
    private Department department;
    private Institution site;

    private String billType;
    private String visitType;

    Map<PatientEncounter, List<Bill>> billPatientEncounterMap = new HashMap<>();
    private Map<String, Map<String, EncounterCreditCompany>> encounterCreditCompanyMap;

    private int rowCounter = 0;

    private List<Bill> bills = new ArrayList<>();
    private Map<Long, Boolean> paymentSelectionMap = new HashMap<>();

    Map<Institution, Double> instituteGopMap = new HashMap<>();
    Map<Institution, Double> institutPaidByCompanyMap = new HashMap<>();
    Map<Institution, Double> institutePayableByCompanyMap = new HashMap<>();

    private Map<PatientEncounter, Double> patientEncounterGopMap;
    private Map<PatientEncounter, Double> patientEncounterPaidByCompanyMap;

    Map<Institution, List<InstitutionBillEncounter>> billInstitutionEncounterMap;
    private Map<PatientEncounter, List<InstitutionBillEncounter>> institutionBillPatientEncounterMap;

    public CreditCompanyDueController() {
    }

    public Map<Institution, List<InstitutionBillEncounter>> getBillInstitutionEncounterMap() {
        return billInstitutionEncounterMap;
    }

    public void setBillInstitutionEncounterMap(Map<Institution, List<InstitutionBillEncounter>> billInstitutionEncounterMap) {
        if (billInstitutionEncounterMap == null) {
            billInstitutionEncounterMap = new HashMap<>();
        }

        this.billInstitutionEncounterMap = billInstitutionEncounterMap;
    }

    public Map<PatientEncounter, Double> getPatientEncounterGopMap() {
        if (patientEncounterGopMap == null) {
            patientEncounterGopMap = new HashMap<>();
        }
        return patientEncounterGopMap;
    }

    public void setPatientEncounterGopMap(Map<PatientEncounter, Double> patientEncounterGopMap) {
        this.patientEncounterGopMap = patientEncounterGopMap;
    }

    public Map<PatientEncounter, Double> getPatientEncounterPaidByCompanyMap() {
        if (patientEncounterPaidByCompanyMap == null) {
            patientEncounterPaidByCompanyMap = new HashMap<>();
        }
        return patientEncounterPaidByCompanyMap;
    }

    public void setPatientEncounterPaidByCompanyMap(Map<PatientEncounter, Double> patientEncounterPaidByCompanyMap) {
        this.patientEncounterPaidByCompanyMap = patientEncounterPaidByCompanyMap;
    }

    public Map<PatientEncounter, List<InstitutionBillEncounter>> getInstitutionBillPatientEncounterMap() {
        if (institutionBillPatientEncounterMap == null) {
            institutionBillPatientEncounterMap = new HashMap<>();
        }
        return institutionBillPatientEncounterMap;
    }

    public void setInstitutionBillPatientEncounterMap(Map<PatientEncounter, List<InstitutionBillEncounter>> institutionBillPatientEncounterMap) {
        if (institutionBillPatientEncounterMap == null) {
            institutionBillPatientEncounterMap = new HashMap<>();
        }
        this.institutionBillPatientEncounterMap = institutionBillPatientEncounterMap;
    }

    public Map<Institution, Double> getInstituteGopMap() {
        return instituteGopMap;
    }

    public void setInstituteGopMap(Map<Institution, Double> instituteGopMap) {
        this.instituteGopMap = instituteGopMap;
    }

    public Map<Institution, Double> getInstitutPaidByCompanyMap() {
        return institutPaidByCompanyMap;
    }

    public void setInstitutPaidByCompanyMap(Map<Institution, Double> institutPaidByCompanyMap) {
        this.institutPaidByCompanyMap = institutPaidByCompanyMap;
    }

    public Map<Institution, Double> getInstitutePayableByCompanyMap() {
        return institutePayableByCompanyMap;
    }

    public void setInstitutePayableByCompanyMap(Map<Institution, Double> institutePayableByCompanyMap) {
        this.institutePayableByCompanyMap = institutePayableByCompanyMap;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }

    public EncounterCreditCompanyFacade getEncounterCreditCompanyFacade() {
        return encounterCreditCompanyFacade;
    }

    public void setEncounterCreditCompanyFacade(EncounterCreditCompanyFacade encounterCreditCompanyFacade) {
        this.encounterCreditCompanyFacade = encounterCreditCompanyFacade;
    }

    public Institution getInstitutionOfDepartment() {
        return institutionOfDepartment;
    }

    public void setInstitutionOfDepartment(Institution institutionOfDepartment) {
        if (!Objects.equals(this.institutionOfDepartment, institutionOfDepartment)) {
            this.institutionOfDepartment = institutionOfDepartment;
            setSite(null);
            setDepartment(null);
        } else {
            this.institutionOfDepartment = institutionOfDepartment;
        }
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public void resetLocationFilters() {
        setSite(null);
        setDepartment(null);
    }

    public void resetDepartment() {
        setDepartment(null);
    }

    public int nextRowCounter() {
        return ++rowCounter;
    }

    public void resetCounter() {
        rowCounter = 0;
    }

    public Map<PatientEncounter, List<Bill>> getBillPatientEncounterMap() {
        return billPatientEncounterMap;
    }

    public void setBillPatientEncounterMap(Map<PatientEncounter, List<Bill>> billPatientEncounterMap) {
        this.billPatientEncounterMap = billPatientEncounterMap;
    }

    public Map<String, Map<String, EncounterCreditCompany>> getEncounterCreditCompanyMap() {
        if (encounterCreditCompanyMap == null) {
            encounterCreditCompanyMap = new HashMap<>();
        }

        return encounterCreditCompanyMap;
    }

    public void setEncounterCreditCompanyMap(Map<String, Map<String, EncounterCreditCompany>> encounterCreditCompanyMap) {
        this.encounterCreditCompanyMap = encounterCreditCompanyMap;
    }

    public double getPayableByPatient() {
        return payableByPatient;
    }

    public double getPayableByCompany() {
        return payableByCompany;
    }

    public void setPayableByCompany(double payableByCompany) {
        this.payableByCompany = payableByCompany;
    }

    public double getGopAmount() {
        return gopAmount;
    }

    public void setGopAmount(double gopAmount) {
        this.gopAmount = gopAmount;
    }

    public void setPayableByPatient(double payableByPatient) {
        this.payableByPatient = payableByPatient;
    }

    public void makeNull() {
        fromDate = null;
        toDate = null;
        items = null;
        institutionEncounters = null;
        creditCompanyAge = null;
        filteredList = null;
        visitType = null;
        institutionOfDepartment = null;
        site = null;
        department = null;
        institution = null;
        creditCompany = null;
    }

    public void createAgeTable() {
        makeNull();
        Set<Institution> setIns = new HashSet<>();
        List<Institution> list = getCreditBean().getCreditCompanyFromBillsFromBillTypeAtomic(true);
        setIns.addAll(list);
        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setValues(ins, newRow);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    /**
     * @deprecated This method will be removed in the next iteration. Pharmacy
     * credit bills are now managed through the OPD credit due age methods,
     * which handle both OPD and Pharmacy credit bills. The separate Pharmacy
     * Credit Settle bill type is being deprecated in favor of the unified OPD
     * Credit Settle bill type. Please use the OPD Due Age functionality
     * instead.
     */
    @Deprecated
    public void createAgeTablePharmacy() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBillsPharmacy(true);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setValuesPharmacy(ins, newRow);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    public void createAgeAccessTable() {
        Date startTime = new Date();

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBills(false);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setValuesAccess(ins, newRow);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    public void createInwardAgeTable() {
        makeNull();
        creditCompanyAge = new ArrayList<>();
        // Admissions picked and valued from the commitment and payment bills (#25601),
        // not from the encounter's creditPaidAmount, which is no longer written.
        for (Map.Entry<Institution, List<PatientEncounter>> entry
                : dischargedAdmissionsByCreditCompany(PaymentMethod.Credit, true, null, null, null).entrySet()) {
            String1Value5 newRow = new String1Value5();
            newRow.setInstitution(entry.getKey());
            setInwardDueAgeValues(entry.getValue(), entry.getKey(), newRow);
            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    //    public void createInwardAgeTableWithFilters() {
//        Date startTime = new Date();
//
//        makeNull();
//        Set<Institution> setIns = new HashSet<>();
//
//        List<Institution> list = getCreditBean().getCreditCompanyFromBht(
//                true, PaymentMethod.Credit, institutionOfDepartment, department, site);
//        setIns.addAll(list);
//
//        creditCompanyAge = new ArrayList<>();
//        for (Institution ins : setIns) {
//            if (ins == null) {
//                continue;
//            }
//
//            String1Value5 newRow = new String1Value5();
//            newRow.setInstitution(ins);
//            setInwardValues(ins, newRow, PaymentMethod.Credit, institutionOfDepartment, department, site);
//
//            if (newRow.getValue1() != 0
//                    || newRow.getValue2() != 0
//                    || newRow.getValue3() != 0
//                    || newRow.getValue4() != 0) {
//                creditCompanyAge.add(newRow);
//            }
//        }
//    }
    public void createInwardAgeTableWithFilters() {
        reportTimerController.trackReportExecution(() -> {
            Date startTime = new Date();

            // Reset only output lists — preserve scope/filter fields (institution, dates, etc.)
            creditCompanyAge = null;
            filteredList = null;

            Map<Institution, List<Bill>> institutionMap = getCreditCompanyBillsGroupedByCreditCompany();
            final List<PatientEncounter> allPatientEncounters = new ArrayList<>();

            creditCompanyAge = new ArrayList<>();
            for (Institution ins : institutionMap.keySet()) {
                if (ins == null) {
                    continue;
                }

                String1Value5 newRow = new String1Value5();
                newRow.setInstitution(ins);
                setInwardValues(newRow, institutionMap.get(ins));

                if (newRow.getValue1() != 0
                        || newRow.getValue2() != 0
                        || newRow.getValue3() != 0
                        || newRow.getValue4() != 0) {
                    creditCompanyAge.add(newRow);

                    allPatientEncounters.addAll(newRow.getAllPatientEncountersByBills());
                }
            }

            setEncounterCreditCompanyMap(getEncounterCreditCompanies(allPatientEncounters));
        }, FinancialReport.DUE_AGE_DETAIL_REPORT, sessionController.getLoggedUser());
    }

    // Map<bht, Map<Credit Company, Encounter Credit Company>>
    private Map<String, Map<String, EncounterCreditCompany>> getEncounterCreditCompanies(final List<PatientEncounter> patientEncounters) {
        List<Long> patientEncounterIds = patientEncounters.stream()
                .map(PatientEncounter::getId)
                .collect(Collectors.toList());

        if (patientEncounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT ecc FROM EncounterCreditCompany ecc WHERE ecc.patientEncounter.id IN :patientEncounterIds";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("patientEncounterIds", patientEncounterIds);

        List<EncounterCreditCompany> results = encounterCreditCompanyFacade.findByJpql(jpql, parameters);

        Map<String, Map<String, EncounterCreditCompany>> encounterCreditCompanyMap = new HashMap<>();

        for (EncounterCreditCompany ecc : results) {
            String bhtNo = ecc.getPatientEncounter().getBhtNo();
            String institutionName = ecc.getInstitution().getName();

            encounterCreditCompanyMap
                    .computeIfAbsent(bhtNo, k -> new HashMap<>())
                    .putIfAbsent(institutionName, ecc);
        }

        return encounterCreditCompanyMap;
    }

    private Map<Institution, List<Bill>> getCreditCompanyBillsGroupedByCreditCompany() {
        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

        String jpql = "SELECT bill FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND (bill.cancelled = false OR bill.cancelled IS NULL) "
                + "AND bill.billTypeAtomic IN :bts "
                + "AND bill.referenceBill.confirmedFinalBill = true "
                + "AND (ABS(bill.netTotal) - ABS(bill.paidAmount)) > :minBalance";

        parameters.put("br", true);
        parameters.put("bts", bts);
        parameters.put("minBalance", 0.01);

        if (institutionOfDepartment != null) {
            jpql += " AND bill.institution = :ins";
            parameters.put("ins", institutionOfDepartment);
        }

        List<Bill> bills = billFacade.findByJpql(jpql, parameters);

        return bills.stream()
                .filter(b -> b.getCreditCompany() != null)
                .collect(Collectors.groupingBy(Bill::getCreditCompany));
    }

    public void exportCreditCompanyDueAgeDetailToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Credit_Company_Due.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Credit Company Due Report");
            int rowIndex = 0;

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);

            XSSFCellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            XSSFCellStyle boldStyleCentered = workbook.createCellStyle();
            boldStyleCentered.setFont(boldFont);
            boldStyleCentered.setAlignment(HorizontalAlignment.CENTER);
            boldStyleCentered.setVerticalAlignment(VerticalAlignment.CENTER);

            Row headerRow = sheet.createRow(rowIndex++);
            Cell cell0 = headerRow.createCell(0);
            cell0.setCellValue("Credit Company");
            cell0.setCellStyle(boldStyle);

            Cell cell1 = headerRow.createCell(1);
            cell1.setCellValue("0-30 Days");
            cell1.setCellStyle(boldStyleCentered);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 7));

            Cell cell2 = headerRow.createCell(8);
            cell2.setCellValue("30-60 Days");
            cell2.setCellStyle(boldStyleCentered);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 8, 14));

            Cell cell3 = headerRow.createCell(15);
            cell3.setCellValue("60-90 Days");
            cell3.setCellStyle(boldStyleCentered);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 15, 21));

            Cell cell4 = headerRow.createCell(22);
            cell4.setCellValue("90+ Days");
            cell4.setCellStyle(boldStyleCentered);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 22, 28));

            XSSFCellStyle mergedStyle = workbook.createCellStyle();
            mergedStyle.cloneStyleFrom(amountStyle);
            mergedStyle.setFont(boldFont);
            mergedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            mergedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (String1Value5 i : getCreditCompanyAge()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(i.getInstitution().getName());
                row.getCell(0).setCellStyle(mergedStyle);

                row.createCell(1).setCellValue(i.getValue1());
                row.getCell(1).setCellStyle(mergedStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 1, 7));

                row.createCell(8).setCellValue(i.getValue2());
                row.getCell(8).setCellStyle(mergedStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 8, 14));

                row.createCell(15).setCellValue(i.getValue3());
                row.getCell(15).setCellStyle(mergedStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 15, 21));

                row.createCell(22).setCellValue(i.getValue4());
                row.getCell(22).setCellStyle(mergedStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 22, 28));

                int maxRows = Math.max(
                        Math.max(i.getValue1Bills().size(), i.getValue2Bills().size()),
                        Math.max(i.getValue3Bills().size(), i.getValue4Bills().size())
                );

                Row subHeaderRow = sheet.createRow(rowIndex++);
                insertSubHeaderCell(subHeaderRow, 1, boldStyle);
                insertSubHeaderCell(subHeaderRow, 8, boldStyle);
                insertSubHeaderCell(subHeaderRow, 15, boldStyle);
                insertSubHeaderCell(subHeaderRow, 22, boldStyle);

                for (int j = 0; j < maxRows; j++) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(" ");

                    insertBillCell(dataRow, i.getValue1Bills(), i.getInstitution(), j, 1, amountStyle);
                    insertBillCell(dataRow, i.getValue2Bills(), i.getInstitution(), j, 8, amountStyle);
                    insertBillCell(dataRow, i.getValue3Bills(), i.getInstitution(), j, 15, amountStyle);
                    insertBillCell(dataRow, i.getValue4Bills(), i.getInstitution(), j, 22, amountStyle);
                }
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage());
        }
    }

    private void insertBillCell(Row row, List<Bill> bills, Institution institution, int index, int startCol, XSSFCellStyle amountStyle) {
        if (index < bills.size()) {
            Bill b = bills.get(index);
            row.createCell(startCol).setCellValue(b.getPatientEncounter().getBhtNo());
            row.createCell(startCol + 1).setCellValue(b.getPatientEncounter().getPatient().getPerson().getName());
            row.createCell(startCol + 2).setCellValue(getPolicyNumberFromEncounterCreditCompanyMap(b.getPatientEncounter().getBhtNo(), institution.getName()));
            row.createCell(startCol + 3).setCellValue(getReferenceNumberFromEncounterCreditCompanyMap(b.getPatientEncounter().getBhtNo(), institution.getName()));
            Date doa = b.getPatientEncounter().getDateOfDischarge();
            row.createCell(startCol + 4).setCellValue(doa != null ? doa.toString() : "");
            Date dod = b.getPatientEncounter().getDateOfDischarge();
            row.createCell(startCol + 5).setCellValue(dod != null ? dod.toString() : "");

            Cell valueCell = row.createCell(startCol + 6);
            valueCell.setCellValue(b.getNetTotal() - b.getPaidAmount());

            valueCell.setCellStyle(amountStyle);
        } else {
            for (int k = 0; k < 5; k++) {
                row.createCell(startCol + k).setCellValue("");
            }
        }
    }

    private void insertSubHeaderCell(Row row, int startCol, XSSFCellStyle boldStyle) {
        Cell cell0 = row.createCell(startCol);
        cell0.setCellValue("BHT No");
        cell0.setCellStyle(boldStyle);

        Cell cell1 = row.createCell(startCol + 1);
        cell1.setCellValue("Patient");
        cell1.setCellStyle(boldStyle);

        Cell cell2 = row.createCell(startCol + 2);
        cell2.setCellValue("Policy Number");
        cell2.setCellStyle(boldStyle);

        Cell cell3 = row.createCell(startCol + 3);
        cell3.setCellValue("Reference Number");
        cell3.setCellStyle(boldStyle);

        Cell cell4 = row.createCell(startCol + 4);
        cell4.setCellValue("Admission Date");
        cell4.setCellStyle(boldStyle);

        Cell cell5 = row.createCell(startCol + 5);
        cell5.setCellValue("Discharge Date");
        cell5.setCellStyle(boldStyle);

        Cell cell6 = row.createCell(startCol + 6);
        cell6.setCellValue("Amount");
        cell6.setCellStyle(boldStyle);
    }

    List<DealerDueDetailRow> dealerDueDetailRows;

    public List<DealerDueDetailRow> getDealerDueDetailRows() {
        return dealerDueDetailRows;
    }

    public void setDealerDueDetailRows(List<DealerDueDetailRow> dealerDueDetailRows) {
        this.dealerDueDetailRows = dealerDueDetailRows;
    }

    public void createInwardAgeDetailAnalysis() {
        Date startTime = new Date();

        dealerDueDetailRows = new ArrayList<>();
        createInwardAgeTable();
        Institution dealer = null;
        for (String1Value5 s : creditCompanyAge) {
            DealerDueDetailRow row = new DealerDueDetailRow();
            if (dealer == null || dealer != s.getInstitution()) {
                dealer = s.getInstitution();
                row.setDealer(dealer);
                row.setZeroToThirty(s.getValue1());
                row.setThirtyToSixty(s.getValue2());
                row.setSixtyToNinty(s.getValue3());
                row.setMoreThanNinty(s.getValue4());
                dealerDueDetailRows.add(row);
            }

            int rowsForDealer = 0;
            if (rowsForDealer < s.getValue1PatientEncounters().size()) {
                rowsForDealer = s.getValue1PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue2PatientEncounters().size()) {
                rowsForDealer = s.getValue2PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue3PatientEncounters().size()) {
                rowsForDealer = s.getValue3PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue4PatientEncounters().size()) {
                rowsForDealer = s.getValue4PatientEncounters().size();
            }

            for (int i = 0; i < rowsForDealer; i++) {
                DealerDueDetailRow rowi = new DealerDueDetailRow();
                rowi.setCompany(dealer);
                if (s.getValue1PatientEncounters().size() > i) {
                    rowi.setZeroToThirtyEncounter(s.getValue1PatientEncounters().get(i));
                }
                if (s.getValue2PatientEncounters().size() > i) {
                    rowi.setThirtyToSixtyEncounter(s.getValue2PatientEncounters().get(i));
                }
                if (s.getValue3PatientEncounters().size() > i) {
                    rowi.setSixtyToNintyEncounter(s.getValue3PatientEncounters().get(i));
                }
                if (s.getValue4PatientEncounters().size() > i) {
                    rowi.setMoreThanNintyEncounter(s.getValue4PatientEncounters().get(i));
                }
                dealerDueDetailRows.add(rowi);
            }

        }

        creditCompanyAge = new ArrayList<>();
    }

    public void createInwardAgeDetailAnalysisWithFilters() {
        Date startTime = new Date();

        dealerDueDetailRows = new ArrayList<>();
        createInwardAgeTableWithFilters();
        Institution dealer = null;
        for (String1Value5 s : creditCompanyAge) {
            DealerDueDetailRow row = new DealerDueDetailRow();
            if (dealer == null || dealer != s.getInstitution()) {
                dealer = s.getInstitution();
                row.setDealer(dealer);
                row.setZeroToThirty(s.getValue1());
                row.setThirtyToSixty(s.getValue2());
                row.setSixtyToNinty(s.getValue3());
                row.setMoreThanNinty(s.getValue4());
                dealerDueDetailRows.add(row);
            }

            int rowsForDealer = 0;
            if (rowsForDealer < s.getValue1PatientEncounters().size()) {
                rowsForDealer = s.getValue1PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue2PatientEncounters().size()) {
                rowsForDealer = s.getValue2PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue3PatientEncounters().size()) {
                rowsForDealer = s.getValue3PatientEncounters().size();
            }
            if (rowsForDealer < s.getValue4PatientEncounters().size()) {
                rowsForDealer = s.getValue4PatientEncounters().size();
            }

            for (int i = 0; i < rowsForDealer; i++) {
                DealerDueDetailRow rowi = new DealerDueDetailRow();
                rowi.setCompany(dealer);
                if (s.getValue1PatientEncounters().size() > i) {
                    rowi.setZeroToThirtyEncounter(s.getValue1PatientEncounters().get(i));
                }
                if (s.getValue2PatientEncounters().size() > i) {
                    rowi.setThirtyToSixtyEncounter(s.getValue2PatientEncounters().get(i));
                }
                if (s.getValue3PatientEncounters().size() > i) {
                    rowi.setSixtyToNintyEncounter(s.getValue3PatientEncounters().get(i));
                }
                if (s.getValue4PatientEncounters().size() > i) {
                    rowi.setMoreThanNintyEncounter(s.getValue4PatientEncounters().get(i));
                }
                dealerDueDetailRows.add(rowi);
            }
        }

        creditCompanyAge = new ArrayList<>();
    }

    public void createInwardCashAgeTable() {
        makeNull();
        creditCompanyAge = new ArrayList<>();
        // Admissions picked and valued from the commitment and payment bills (#25601),
        // not from the encounter's creditPaidAmount, which is no longer written.
        for (Map.Entry<Institution, List<PatientEncounter>> entry
                : dischargedAdmissionsByCreditCompany(PaymentMethod.Cash, true, null, null, null).entrySet()) {
            String1Value5 newRow = new String1Value5();
            newRow.setString(entry.getKey().getName());
            setInwardDueAgeValues(entry.getValue(), entry.getKey(), newRow);
            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    public void createInwardAgeTableAccess() {
        makeNull();
        creditCompanyAge = new ArrayList<>();
        // Admissions picked and valued from the commitment and payment bills (#25601),
        // not from the encounter's creditPaidAmount, which is no longer written.
        for (Map.Entry<Institution, List<PatientEncounter>> entry
                : dischargedAdmissionsByCreditCompany(PaymentMethod.Credit, false, null, null, null).entrySet()) {
            String1Value5 newRow = new String1Value5();
            newRow.setString(entry.getKey().getName());
            setInwardAccessAgeValues(entry.getValue(), entry.getKey(), newRow, true);
            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

//    public void createInwardAgeTableAccessWithFilters() {
//        Date startTime = new Date();
//
//        makeNull();
//        Set<Institution> setIns = new HashSet<>();
//
//        List<Institution> list = getCreditBean().getCreditCompanyFromBht(
//                false, PaymentMethod.Credit, institutionOfDepartment, department, site);
//
//        setIns.addAll(list);
//
//        creditCompanyAge = new ArrayList<>();
//        for (Institution ins : setIns) {
//            if (ins == null) {
//                continue;
//            }
//
//            String1Value5 newRow = new String1Value5();
//            newRow.setString(ins.getName());
//            setInwardValuesAccessForExcess(ins, newRow, PaymentMethod.Credit);
//
//            if (newRow.getValue1() != 0
//                    || newRow.getValue2() != 0
//                    || newRow.getValue3() != 0
//                    || newRow.getValue4() != 0) {
//                creditCompanyAge.add(newRow);
//            }
//        }
//    }
    public void createInwardAgeTableAccessWithFilters() {
        reportTimerController.trackReportExecution(() -> {
            HashMap m = new HashMap();
            String sql = " Select b from PatientEncounter b"
                    + " JOIN b.finalBill fb"
                    + " where b.retired=false "
                    + " and b.paymentFinalized=true ";

            if (admissionType != null) {
                sql += " and b.admissionType =:ad ";
                m.put("ad", admissionType);
            }

            if (paymentMethod != null) {
                sql += " and b.paymentMethod =:pm ";
                m.put("pm", paymentMethod);
            }

            if (institutionOfDepartment != null) {
                sql += "AND fb.institution = :insd ";
                m.put("insd", institutionOfDepartment);
            }

            if (department != null) {
                sql += "AND fb.department = :dep ";
                m.put("dep", department);
            }

            if (site != null) {
                sql += "AND fb.department.site = :site ";
                m.put("site", site);
            }

            patientEncounters = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

            if (patientEncounters == null) {
                return;
            }

            updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(patientEncounters);

            setBillPatientEncounterMap(getCreditCompanyBills(patientEncounters, "any", institution));
            calculateCreditCompanyAmounts();

            List<InstitutionBillEncounter> institutionEncounters = new ArrayList<>(
                    InstitutionBillEncounter.createInstitutionBillEncounter(getBillPatientEncounterMap(), "excess", "company"));

            setBillInstitutionEncounterMap(InstitutionBillEncounter.createInstitutionBillEncounterMap(institutionEncounters));

            creditCompanyAge = new ArrayList<>();

            for (Institution ins : getBillInstitutionEncounterMap().keySet()) {
                if (ins == null) {
                    continue;
                }

                List<InstitutionBillEncounter> institutionBillEncounters = getBillInstitutionEncounterMap().get(ins);

                String1Value5 newRow = new String1Value5();
                newRow.setString(ins.getName());
                setInwardValuesAccessForExcess(institutionBillEncounters, newRow);

                if (newRow.getValue1() != 0
                        || newRow.getValue2() != 0
                        || newRow.getValue3() != 0
                        || newRow.getValue4() != 0) {
                    creditCompanyAge.add(newRow);
                }
            }
        }, FinancialReport.INWARD_CREDIT_EXCESS_AGE_CREDIT_COMPANY, sessionController.getLoggedUser());
    }

    public void createInwardCashAgeTableAccess() {
        makeNull();
        creditCompanyAge = new ArrayList<>();
        // Admissions picked and valued from the commitment and payment bills (#25601),
        // not from the encounter's creditPaidAmount, which is no longer written.
        for (Map.Entry<Institution, List<PatientEncounter>> entry
                : dischargedAdmissionsByCreditCompany(PaymentMethod.Cash, false, null, null, null).entrySet()) {
            String1Value5 newRow = new String1Value5();
            newRow.setString(entry.getKey().getName());
            setInwardAccessAgeValues(entry.getValue(), entry.getKey(), newRow, false);
            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    public void createInwardCashAgeTableAccessWithFilters() {
        makeNull();
        creditCompanyAge = new ArrayList<>();
        // Admissions picked and valued from the commitment and payment bills (#25601),
        // not from the encounter's creditPaidAmount, which is no longer written.
        for (Map.Entry<Institution, List<PatientEncounter>> entry
                : dischargedAdmissionsByCreditCompany(PaymentMethod.Cash, false, institutionOfDepartment, department, site).entrySet()) {
            String1Value5 newRow = new String1Value5();
            newRow.setString(entry.getKey().getName());
            setInwardAccessAgeValues(entry.getValue(), entry.getKey(), newRow, false);
            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    private void setValues(Institution inst, String1Value5 dataTable5Value) {

        // Outstanding due per bill, the same rule as OPD Due Search (#25534).
        List<Bill> lst = getCreditBean().getOutstandingOpdFinanceCreditBills(inst);
        for (Bill b : lst) {

            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());

            double finalValue = b.getNetTotal() - (b.getRefundAmount() + b.getPaidAmount());

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    private void setValuesPharmacy(Institution inst, String1Value5 dataTable5Value) {

        List<Bill> lst = getCreditBean().getCreditBillsPharmacy(inst, true);
        for (Bill b : lst) {

            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());

            double finalValue = b.getNetTotal() - b.getRefundAmount() - b.getPaidAmount();

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    private void setValuesAccess(Institution inst, String1Value5 dataTable5Value) {

        List<Bill> lst = getCreditBean().getCreditBills(inst, false);
        for (Bill b : lst) {

            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());

            double finalValue = (b.getNetTotal() + b.getPaidAmount());

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }

        }

    }

    /**
     * Discharged admissions of the given payment method that have a credit
     * company, grouped by company in name order. With {@code withCompanyDue},
     * only admissions the companies still owe on (ccDue above 1 cent);
     * otherwise only fully covered ones (net total less patient money less
     * company money not above 1 cent), as on the access searches. Figures are
     * loaded once for the whole result (#25601).
     */
    private Map<Institution, List<PatientEncounter>> dischargedAdmissionsByCreditCompany(PaymentMethod pm,
            boolean withCompanyDue, Institution insd, Department dep, Institution siteFilter) {
        HashMap<String, Object> m = new HashMap<>();
        String sql = "Select b From PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " where b.retired=false"
                + " and b.discharged=true"
                + " and b.paymentMethod=:pm";
        if (insd != null) {
            sql += " and fb.institution = :insd";
            m.put("insd", insd);
        }
        if (dep != null) {
            sql += " and fb.department = :dep";
            m.put("dep", dep);
        }
        if (siteFilter != null) {
            sql += " and fb.department.site = :site";
            m.put("site", siteFilter);
        }
        m.put("pm", pm);
        List<PatientEncounter> candidates = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        if (candidates == null) {
            return new LinkedHashMap<>();
        }
        loadCcFigures(candidates);
        List<PatientEncounter> kept = new ArrayList<>();
        for (PatientEncounter b : candidates) {
            if (b.getFinalBill() == null) {
                continue;
            }
            // The fully covered test stays on the whole admission; only the rows are split by company.
            if (!withCompanyDue) {
                double outstanding = Math.round((b.getFinalBill().getNetTotal() - getPatientPaid(b) - getCcPaid(b)) * 100.0) / 100.0;
                if (outstanding > 0.01) {
                    continue;
                }
            }
            kept.add(b);
        }
        return groupByCommitmentCompany(kept, withCompanyDue);
    }

    /** Ages each admission's due of the given commitment company into the four buckets by admission creation date. */
    private void setInwardDueAgeValues(List<PatientEncounter> lst, Institution company, String1Value5 dataTable5Value) {
        for (PatientEncounter b : lst) {
            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());
            b.setTransDayCount(dayCount);
            double finalValue = getCcDueForCompany(b, company);
            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
                dataTable5Value.getValue1PatientEncounters().add(b);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
                dataTable5Value.getValue2PatientEncounters().add(b);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
                dataTable5Value.getValue3PatientEncounters().add(b);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
                dataTable5Value.getValue4PatientEncounters().add(b);
            }
        }
    }

    /**
     * Same buckets for the access (settled) age tables, from the company's own
     * figures, signed. With {@code paidLessUsed} the value is paid less committed
     * (the Credit Access excess direction, positive when the company overpaid);
     * otherwise committed less paid (the cash access direction).
     */
    private void setInwardAccessAgeValues(List<PatientEncounter> lst, Institution company, String1Value5 dataTable5Value,
            boolean paidLessUsed) {
        for (PatientEncounter b : lst) {
            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());
            double balance = getCcBalanceForCompany(b, company);
            double finalValue = paidLessUsed ? -balance : balance;
            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }
        }
    }

    

    private void setInwardValues(Institution inst, String1Value5 dataTable5Value, PaymentMethod paymentMethod,
            Institution institutionOfDepartment, Department department, Institution site) {
        List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounters(
                inst, true, paymentMethod, institutionOfDepartment, department, site);
        for (PatientEncounter b : lst) {
            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());
            b.setTransDayCount(dayCount);
//            double finalValue = b.getFinalBill().getNetTotal() - (Math.abs(b.getFinalBill().getPaidAmount()) + Math.abs(b.getCreditPaidAmount()));
            double finalValue = b.getCreditUsedAmount() + b.getCreditPaidAmount();

            if (finalValue == 0) {
                continue;
            }

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
                dataTable5Value.getValue1PatientEncounters().add(b);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
                dataTable5Value.getValue2PatientEncounters().add(b);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
                dataTable5Value.getValue3PatientEncounters().add(b);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
                dataTable5Value.getValue4PatientEncounters().add(b);
            }
        }
    }

    private void setInwardValues(String1Value5 dataTable5Value, List<Bill> bills) {
        for (Bill b : bills) {
            // Use billDate (financial obligation date) not createdAt (DB insert timestamp)
            Date ageFrom = b.getBillDate() != null ? b.getBillDate() : b.getCreatedAt();
            long dayCount = CommonFunctions.getDayCountTillNow(ageFrom);

            double finalValue = b.getNetTotal() - b.getPaidAmount();

            if (finalValue == 0) {
                continue;
            }

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
                dataTable5Value.getValue1Bills().add(b);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
                dataTable5Value.getValue2Bills().add(b);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
                dataTable5Value.getValue3Bills().add(b);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
                dataTable5Value.getValue4Bills().add(b);
            }
        }
    }

    

    private void setInwardValuesAccessForExcess(Institution inst, String1Value5 dataTable5Value, PaymentMethod paymentMethod) {
        List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounters(inst, false, paymentMethod);
        for (PatientEncounter b : lst) {

            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());

            double finalValue = (Math.abs(b.getFinalBill().getNetTotal()) - Math.abs(b.getFinalBill().getPaidAmount()));

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }
        }
    }

    private void setInwardValuesAccessForExcess(List<InstitutionBillEncounter> institutionBillEncounters, String1Value5 dataTable5Value) {
        for (InstitutionBillEncounter b : institutionBillEncounters) {

            Long dayCount = CommonFunctions.getDayCountTillNow(b.getPatientEncounter().getFinalBill().getCreatedAt());

            double finalValue = (b.getCompanyExcess());

            if (dayCount < 30) {
                dataTable5Value.setValue1(dataTable5Value.getValue1() + finalValue);
            } else if (dayCount < 60) {
                dataTable5Value.setValue2(dataTable5Value.getValue2() + finalValue);
            } else if (dayCount < 90) {
                dataTable5Value.setValue3(dataTable5Value.getValue3() + finalValue);
            } else {
                dataTable5Value.setValue4(dataTable5Value.getValue4() + finalValue);
            }
        }
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public String getDateBasis() {
        return dateBasis;
    }

    public void setDateBasis(String dateBasis) {
        this.dateBasis = dateBasis;
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

    @Deprecated // UsecreateOpdCreditDueByBillTypeAtomic
    public void createOpdCreditDue() {
        List<Institution> setIns = getCreditBean().getCreditInstitution(BillType.OpdBill, getFromDate(), getToDate(), true);
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            List<Bill> bills = getCreditBean().getCreditBills(ins, BillType.OpdBill, getFromDate(), getToDate(), true);
            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            newIns.setBills(bills);
            for (Bill b : bills) {
                newIns.setTotal(newIns.getTotal() + b.getNetTotal());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidAmount());
            }
            items.add(newIns);
        }
    }

    public void createOpdCreditDueByBillTypeAtomic() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> btas;
            switch (billType) {
                case "ALL":
                    btas = billService.fetchBillTypeAtomicsForOpdFinance();
                    break;
                case "OPD":
                    btas = billService.fetchBillTypeAtomicsForOnlyOpdBills();
                    break;
                case "PHARMACY":
                    btas = billService.fetchBillTypeAtomicsPharmacySale();
                    break;
                case "PACKAGE":
                    btas = billService.fetchBillTypeAtomicsForOnlyPackageBills();
                    break;
                default:
                    btas = billService.fetchBillTypeAtomicsForOpdFinance();
            }

            List<Institution> setIns = new ArrayList<>();

            if (creditCompany == null) {
                setIns = getCreditBean().getCreditInstitution(btas, getFromDate(), getToDate(), true);
            } else {
                setIns.add(creditCompany);
            }

            items = new ArrayList<>();
            paymentSelectionMap = new HashMap<>();
            for (Institution ins : setIns) {
                List<Payment> payments = getCreditBean().getCreditPayments(ins, btas, getFromDate(), getToDate(), true);
                InstitutionBills newIns = new InstitutionBills();
                newIns.setInstitution(ins);
                newIns.setPayments(payments);
                newIns.setBills(new ArrayList<>());

                Set<Long> countedBillIds = new HashSet<>(); // Assuming bill.getId() is Long

                for (Payment p : payments) {
                    if (p.getBill() == null || countedBillIds.contains(p.getBill().getId())) {
                        continue;
                    }

                    countedBillIds.add(p.getBill().getId());

                    newIns.getBills().add(p.getBill());
                    newIns.setTotal(newIns.getTotal() + p.getBill().getNetTotal());
                    newIns.setPaidTotal(newIns.getPaidTotal() + p.getBill().getPaidAmount());
                    newIns.setRefundTotal(newIns.getRefundTotal() + p.getBill().getRefundAmount());
                }

                items.add(newIns);
            }
        }, CreditReport.OPD_CREDIT_DUE, sessionController.getLoggedUser());
    }

    /**
     * @deprecated This method will be removed in the next iteration. Pharmacy
     * credit bills are now managed through the OPD credit due methods, which
     * handle both OPD and Pharmacy credit bills. The separate Pharmacy Credit
     * Settle bill type is being deprecated in favor of the unified OPD Credit
     * Settle bill type. Please use the OPD Due Search functionality instead.
     */
    @Deprecated
    public void createPharmacyCreditDue() {
        List<BillType> billTypes = Arrays.asList(BillType.PharmacyWholeSale, BillType.PharmacySale);

        List<Institution> creditCompanies = getCreditBean().getCreditInstitutionPharmacy(
                billTypes,
                getFromDate(),
                getToDate(),
                true,
                getInstitution(),
                getSite(),
                getDepartment(),
                getVisitType(),
                getCreditCompany());

        items = new ArrayList<>();

        for (Institution ins : creditCompanies) {
            List<Bill> bills = getCreditBean().getCreditBillsPharmacy(
                    ins,
                    billTypes,
                    getFromDate(),
                    getToDate(),
                    true,
                    getInstitution(),
                    getSite(),
                    getDepartment(),
                    getVisitType());

            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            newIns.setBills(bills);

            for (Bill b : bills) {
                newIns.setTotal(newIns.getTotal() + b.getNetTotal());
                newIns.setRefundTotal(newIns.getRefundTotal() + b.getRefundAmount());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidAmount());
            }

            items.add(newIns);
        }
    }

    public void createOpdCreditDueBillItem() {
        Date startTime = new Date();

        List<Institution> setIns = new ArrayList<>();
        if (creditCompany != null) {
            setIns.add(creditCompany);
        } else {
            setIns.addAll(getCreditBean().getCreditInstitution(BillType.OpdBill, getFromDate(), getToDate(), true));
        }
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            List<BillItem> billItems = getCreditBean().getCreditBillItems(ins, BillType.OpdBill, getFromDate(), getToDate(), true);
            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            newIns.setBillItems(billItems);

            for (BillItem bi : billItems) {
                newIns.setTotal(newIns.getTotal() + bi.getNetValue());
            }

            items.add(newIns);
        }

    }

    // Settled OPD credit bills. Refunds and settlements are kept on the credit batch bill,
    // so the list reads batch bills and treats a bill as settled when net total less
    // refunds less settlements is nil (#25465).
    public void createOpdCreditAccess() {
        List<Institution> setIns = getCreditBean().getCreditCompaniesWithSettledOpdFinanceBills(getFromDate(), getToDate());
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            List<Bill> bills = getCreditBean().getSettledOpdFinanceCreditBills(ins, getFromDate(), getToDate());
            InstitutionBills newIns = new InstitutionBills();
            newIns.setInstitution(ins);
            newIns.setBills(bills);

            for (Bill b : bills) {
                newIns.setTotal(newIns.getTotal() + b.getNetTotal());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidAmount());
                newIns.setRefundTotal(newIns.getRefundTotal() + b.getRefundAmount());
            }

            items.add(newIns);
        }

    }

    public void createInwardCreditDue() {
        List<Institution> companies = getCreditBean().getCreditCompaniesWithUnpaidInwardCCBills(getFromDate(), getToDate(), admissionType, dateBasis);
        institutionEncounters = new ArrayList<>();
        finalTotal = 0.0;
        finalPaidTotal = 0.0;
        finalPaidTotalPatient = 0.0;
        finalTransPaidTotal = 0.0;
        finalTransPaidTotalPatient = 0.0;

        for (Institution company : companies) {
            if (company == null) {
                continue;
            }
            List<Bill> bills = getCreditBean().getUnpaidInwardCCBills(company, getFromDate(), getToDate(), admissionType, dateBasis);
            if (bills.isEmpty()) {
                continue;
            }

            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(company);
            newIns.setBills(bills);

            List<PatientEncounter> encounters = new ArrayList<>();
            for (Bill bill : bills) {
                newIns.setTotal(newIns.getTotal() + bill.getNetTotal());
                newIns.setPaidTotal(newIns.getPaidTotal() + bill.getPaidAmount());
                if (bill.getPatientEncounter() != null) {
                    encounters.add(bill.getPatientEncounter());
                }
            }
            newIns.setPatientEncounters(encounters);
            finalTotal += newIns.getTotal();
            finalPaidTotal += newIns.getPaidTotal();

            institutionEncounters.add(newIns);
        }
    }

    /**
     * Excel export for the Inward Credit Due search
     * ({@code credit/inward_due_search_credit_company.xhtml}). The on-screen
     * table uses a grouped {@code p:subTable} (institution -> bills), which
     * {@code p:dataExporter} cannot serialize - it always produces an empty
     * workbook for that structure. Building the workbook directly with POI
     * (same pattern as {@link #downloadExcel()} for the OPD due search)
     * avoids that limitation.
     */
    public void downloadInwardCreditDueExcel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        List<InstitutionEncounters> data = institutionEncounters == null ? Collections.emptyList() : institutionEncounters;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inward Credit Due");

            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 15);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle filterStyle = workbook.createCellStyle();
            filterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle sectionHeaderStyle = workbook.createCellStyle();
            sectionHeaderStyle.setFont(boldFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.cloneStyleFrom(dataStyle);
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            DataFormat format = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));

            int headerCount = 8;
            int rowIndex = 0;

            SimpleDateFormat sdf = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateTimeFormat());

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Due Search (Credit Company) - Inward");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, headerCount - 1));

            Row dateRow = sheet.createRow(rowIndex++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue(
                    "From : " + (getFromDate() != null ? sdf.format(getFromDate()) : "-")
                    + "    To : " + (getToDate() != null ? sdf.format(getToDate()) : "-")
                    + "    Date Basis : " + ("admissionDate".equals(dateBasis) ? "Admission Date" : "Discharge Date"));
            dateCell.setCellStyle(filterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, headerCount - 1));

            Row filterRow = sheet.createRow(rowIndex++);
            Cell filterCell = filterRow.createCell(0);
            filterCell.setCellValue(
                    "Admission Type : " + (admissionType != null ? admissionType.getName() : "All")
                    + "    Payment Method : " + (paymentMethod != null ? paymentMethod.getLabel() : "All")
                    + "    Institution : " + (institution != null ? institution.getName() : "All"));
            filterCell.setCellStyle(filterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, headerCount - 1));

            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            int headerIndex = 0;
            headerIndex = createHeaderCell(headerRow, headerIndex, "BHT No", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Date Of Admission", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Date Of Discharge", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Patient Name", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Credit Company", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Bill Amount", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Paid", headerStyle);
            createHeaderCell(headerRow, headerIndex, "Outstanding", headerStyle);

            for (InstitutionEncounters ins : data) {
                if (ins == null) {
                    continue;
                }

                Row institutionRow = sheet.createRow(rowIndex++);
                Cell institutionCell = institutionRow.createCell(0);
                institutionCell.setCellValue(ins.getInstitution() != null ? ins.getInstitution().getName() : "");
                institutionCell.setCellStyle(sectionHeaderStyle);
                sheet.addMergedRegion(new CellRangeAddress(institutionRow.getRowNum(), institutionRow.getRowNum(), 0, headerCount - 1));

                for (Bill bill : ins.getBills()) {
                    if (bill == null) {
                        continue;
                    }
                    Row dataRow = sheet.createRow(rowIndex++);
                    int dataIndex = 0;

                    Cell bhtCell = dataRow.createCell(dataIndex++);
                    bhtCell.setCellValue(bill.getPatientEncounter() != null ? bill.getPatientEncounter().getBhtNo() : "");
                    bhtCell.setCellStyle(dataStyle);

                    Cell admissionCell = dataRow.createCell(dataIndex++);
                    Date doa = bill.getPatientEncounter() != null ? bill.getPatientEncounter().getDateOfAdmission() : null;
                    if (doa != null) {
                        admissionCell.setCellValue(doa);
                        admissionCell.setCellStyle(dateStyle);
                    } else {
                        admissionCell.setCellValue("");
                        admissionCell.setCellStyle(dataStyle);
                    }

                    Cell dischargeCell = dataRow.createCell(dataIndex++);
                    Date dod = bill.getPatientEncounter() != null ? bill.getPatientEncounter().getDateOfDischarge() : null;
                    if (dod != null) {
                        dischargeCell.setCellValue(dod);
                        dischargeCell.setCellStyle(dateStyle);
                    } else {
                        dischargeCell.setCellValue("");
                        dischargeCell.setCellStyle(dataStyle);
                    }

                    Cell nameCell = dataRow.createCell(dataIndex++);
                    nameCell.setCellValue(bill.getPatient() != null && bill.getPatient().getPerson() != null
                            ? bill.getPatient().getPerson().getNameWithTitle() : "");
                    nameCell.setCellStyle(dataStyle);

                    Cell companyCell = dataRow.createCell(dataIndex++);
                    companyCell.setCellValue(bill.getCreditCompany() != null ? bill.getCreditCompany().getName() : "");
                    companyCell.setCellStyle(dataStyle);

                    Cell amountCell = dataRow.createCell(dataIndex++);
                    amountCell.setCellValue(bill.getNetTotal());
                    amountCell.setCellStyle(numberStyle);

                    Cell paidCell = dataRow.createCell(dataIndex++);
                    paidCell.setCellValue(bill.getPaidAmount());
                    paidCell.setCellStyle(numberStyle);

                    Cell outstandingCell = dataRow.createCell(dataIndex);
                    outstandingCell.setCellValue(bill.getNetTotal() - bill.getPaidAmount());
                    outstandingCell.setCellStyle(numberStyle);
                }

                Row subTotalRow = sheet.createRow(rowIndex++);
                Cell subTotalLabel = subTotalRow.createCell(0);
                subTotalLabel.setCellValue("Sub Total");
                subTotalLabel.setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(subTotalRow.getRowNum(), subTotalRow.getRowNum(), 0, 4));

                for (int i = 1; i <= 4; i++) {
                    subTotalRow.createCell(i);
                }

                Cell subTotalAmount = subTotalRow.createCell(5);
                subTotalAmount.setCellValue(ins.getTotal());
                subTotalAmount.setCellStyle(numberStyle);

                Cell subTotalPaid = subTotalRow.createCell(6);
                subTotalPaid.setCellValue(ins.getPaidTotal());
                subTotalPaid.setCellStyle(numberStyle);

                Cell subTotalOutstanding = subTotalRow.createCell(7);
                subTotalOutstanding.setCellValue(ins.getTotal() - ins.getPaidTotal());
                subTotalOutstanding.setCellStyle(numberStyle);
            }

            Row grandTotalRow = sheet.createRow(rowIndex++);
            Cell grandTotalLabel = grandTotalRow.createCell(0);
            grandTotalLabel.setCellValue("Grand Total");
            grandTotalLabel.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(grandTotalRow.getRowNum(), grandTotalRow.getRowNum(), 0, 4));

            for (int i = 1; i <= 4; i++) {
                grandTotalRow.createCell(i);
            }

            Cell grandTotalAmount = grandTotalRow.createCell(5);
            grandTotalAmount.setCellValue(finalTotal);
            grandTotalAmount.setCellStyle(numberStyle);

            Cell grandTotalPaid = grandTotalRow.createCell(6);
            grandTotalPaid.setCellValue(finalPaidTotal);
            grandTotalPaid.setCellStyle(numberStyle);

            Cell grandTotalOutstanding = grandTotalRow.createCell(7);
            grandTotalOutstanding.setCellValue(finalTotal - finalPaidTotal);
            grandTotalOutstanding.setCellStyle(numberStyle);

            if (sessionController != null && sessionController.getLoggedUser() != null
                    && sessionController.getLoggedUser().getWebUserPerson() != null) {
                Row printedByRow = sheet.createRow(rowIndex++);
                printedByRow.createCell(0).setCellValue(
                        "Printed By : " + sessionController.getLoggedUser().getWebUserPerson().getName());
            }

            for (int i = 0; i < headerCount; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=Inward_Credit_Due_Report.xlsx");
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                facesContext.responseComplete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    public void createInwardCreditDueWithAdditionalFilters() {
//        Date startTime = new Date();
//
//        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounterWithFinalizedPayments(getFromDate(), getToDate(),
//                PaymentMethod.Credit, institutionOfDepartment, department, site);
//        institutionEncounters = new ArrayList<>();
//        finalTotal = 0.0;
//        finalPaidTotal = 0.0;
//        finalPaidTotalPatient = 0.0;
//        finalTransPaidTotal = 0.0;
//        finalTransPaidTotalPatient = 0.0;
//        for (Institution ins : setIns) {
//            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounterWithFinalizedPayments(ins, getFromDate(), getToDate(),
//                    PaymentMethod.Credit, institutionOfDepartment, department, site);
//
//            updateSettledAmountsForIP(lst);
//
//            if (withOutDueUpdate) {
//                removeSettledAndExcessBills(lst);
//            }
//
//            InstitutionEncounters newIns = new InstitutionEncounters();
//            newIns.setInstitution(ins);
//            newIns.setPatientEncounters(lst);
//
//            for (PatientEncounter b : lst) {
////                b.setTransPaidByPatient(createInwardPaymentTotal(b, getFromDate(), getToDate(), BillType.InwardPaymentBill));
////                b.setTransPaidByCompany(createInwardPaymentTotalCredit(b, getFromDate(), getToDate(), BillType.CashRecieveBill));
//
//                newIns.setTotal(newIns.getTotal() + b.getFinalBill().getNetTotal());
//                newIns.setPaidTotalPatient(newIns.getPaidTotalPatient() + b.getFinalBill().getSettledAmountByPatient());
//                newIns.setPaidTotal(newIns.getPaidTotal() + b.getFinalBill().getSettledAmountBySponsor());
//            }
//
//            finalTotal += newIns.getTotal();
//            finalPaidTotal += newIns.getPaidTotal();
//            finalPaidTotalPatient += newIns.getPaidTotalPatient();
//
//            if (newIns.getPatientEncounters().isEmpty()) {
//                continue;
//            }
//
//            institutionEncounters.add(newIns);
//        }
//    }
    public void createInwardCreditDueWithAdditionalFilters() {
        reportTimerController.trackReportExecution(() -> {
            HashMap m = new HashMap();
            String sql = " Select b from PatientEncounter b"
                    + " JOIN b.finalBill fb"
                    + " where b.retired=false "
                    + " and b.paymentFinalized=true "
                    + " and b.dateOfDischarge between :fd and :td ";

            if (admissionType != null) {
                sql += " and b.admissionType =:ad ";
                m.put("ad", admissionType);
            }

            if (paymentMethod != null) {
                sql += " and b.paymentMethod =:pm ";
                m.put("pm", paymentMethod);
            }

            if (institutionOfDepartment != null) {
                sql += "AND fb.institution = :insd ";
                m.put("insd", institutionOfDepartment);
            }

            if (department != null) {
                sql += "AND fb.department = :dep ";
                m.put("dep", department);
            }

            if (site != null) {
                sql += "AND fb.department.site = :site ";
                m.put("site", site);
            }

            sql += " order by  b.dateOfDischarge";

            m.put("fd", fromDate);
            m.put("td", toDate);
            patientEncounters = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

            if (patientEncounters == null) {
                return;
            }

            updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(patientEncounters);

            setBillPatientEncounterMap(getCreditCompanyBills(patientEncounters, "due", institution));
            calculateCreditCompanyAmounts();

            List<InstitutionBillEncounter> institutionEncounters = new ArrayList<>(
                    InstitutionBillEncounter.createInstitutionBillEncounter(getBillPatientEncounterMap(), "due"));

            setBillInstitutionEncounterMap(InstitutionBillEncounter.createInstitutionBillEncounterMap(institutionEncounters));
            calculateCreditCompanyDueTotals();

            setEncounterCreditCompanyMap(getEncounterCreditCompanies());
        }, CreditReport.INWARD_CREDIT_DUE, sessionController.getLoggedUser());
    }

    private void calculateCreditCompanyDueTotals() {
        gopAmount = 0;
        paidByCompany = 0;
        payableByCompany = 0;

        for (Institution ins : getBillInstitutionEncounterMap().keySet()) {
            double gop = 0;
            double paidByComp = 0;
            double payableByComp = 0;

            List<InstitutionBillEncounter> encounters = getBillInstitutionEncounterMap().get(ins);

            for (InstitutionBillEncounter ibe : encounters) {
                gop += ibe.getGopAmount();
                paidByComp += ibe.getPaidByCompany();
                payableByComp += ibe.getCompanyDue();
            }

            instituteGopMap.put(ins, gop);
            institutPaidByCompanyMap.put(ins, paidByComp);
            institutePayableByCompanyMap.put(ins, payableByComp);

            gopAmount += gop;
            paidByCompany += paidByComp;
            payableByCompany += payableByComp;
        }
    }

    private void calculateCreditCompanyExcessTotals() {
        gopAmount = 0;
        paidByCompany = 0;
        payableByCompany = 0;

        for (Institution ins : getBillInstitutionEncounterMap().keySet()) {
            double gop = 0;
            double paidByComp = 0;
            double payableByComp = 0;

            List<InstitutionBillEncounter> encounters = getBillInstitutionEncounterMap().get(ins);

            for (InstitutionBillEncounter ibe : encounters) {
                gop += ibe.getGopAmount();
                paidByComp += ibe.getPaidByCompany();
                payableByComp += ibe.getCompanyExcess() + ibe.getPatientExcess();
            }

            instituteGopMap.put(ins, gop);
            institutPaidByCompanyMap.put(ins, paidByComp);
            institutePayableByCompanyMap.put(ins, payableByComp);

            gopAmount += gop;
            paidByCompany += paidByComp;
            payableByCompany += payableByComp;
        }
    }

    public void exportDueSearchCreditCompanyToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Credit_Company_Due_Report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Credit Company Due");
            int rowIndex = 0;

            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);

            XSSFCellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            XSSFCellStyle mergedStyle = workbook.createCellStyle();
            mergedStyle.cloneStyleFrom(amountStyle);
            mergedStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {"BHT No", "Date Of Discharge", "Patient Name", "Policy Number", "Reference Number",
                "Billed Amount", "GOP Amount", "Paid by Patient", "Patient Due", "Paid by Company", "Company Due"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            for (Map.Entry<Institution, List<InstitutionBillEncounter>> entry : getBillInstitutionEncounterMap().entrySet()) {
                Institution institution = entry.getKey();
                List<InstitutionBillEncounter> encounters = entry.getValue();

                Row instRow = sheet.createRow(rowIndex++);
                Cell instCell = instRow.createCell(0);
                instCell.setCellValue(institution.getName());
                instCell.setCellStyle(boldStyle);

                for (InstitutionBillEncounter b : encounters) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(b.getBhtNo());
                    dataRow.createCell(1).setCellValue(b.getDateOfDischarge().toString());
                    dataRow.createCell(2).setCellValue(b.getPatient().getPerson().getNameWithTitle());
                    dataRow.createCell(3).setCellValue(getPolicyNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()));
                    dataRow.createCell(4).setCellValue(getReferenceNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()));
                    dataRow.createCell(5).setCellValue(b.getNetTotal());
                    dataRow.createCell(6).setCellValue(b.getGopAmount());
                    dataRow.createCell(7).setCellValue(b.getPaidByPatient());
                    dataRow.createCell(8).setCellValue(b.getPatientDue());
                    dataRow.createCell(9).setCellValue(b.getPaidByCompany());
                    dataRow.createCell(10).setCellValue(b.getCompanyDue());

                    for (int j = 5; j <= 10; j++) {
                        dataRow.getCell(j).setCellStyle(amountStyle);
                    }
                }

                Row subtotalRow = sheet.createRow(rowIndex++);
                subtotalRow.createCell(6).setCellValue(getInstituteGopMap().get(institution));
                subtotalRow.createCell(9).setCellValue(getInstitutPaidByCompanyMap().get(institution));
                subtotalRow.createCell(10).setCellValue(getInstitutePayableByCompanyMap().get(institution));

                subtotalRow.getCell(6).setCellStyle(mergedStyle);
                subtotalRow.getCell(9).setCellStyle(mergedStyle);
                subtotalRow.getCell(10).setCellStyle(mergedStyle);
            }

            Row footerRow = sheet.createRow(rowIndex++);
            footerRow.createCell(0).setCellValue("Total");
            footerRow.getCell(0).setCellStyle(boldStyle);

            footerRow.createCell(6).setCellValue(getGopAmount());
            footerRow.createCell(9).setCellValue(getPaidByCompany());
            footerRow.createCell(10).setCellValue(getPayableByCompany());

            footerRow.getCell(6).setCellStyle(mergedStyle);
            footerRow.getCell(9).setCellStyle(mergedStyle);
            footerRow.getCell(10).setCellStyle(mergedStyle);

            for (int i = 0; i <= 10; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void exportDueSearchCreditCompanyToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Credit_Company_Due_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Due Search (Credit Company)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            Paragraph dateRange = new Paragraph(
                    "From: " + sdf.format(getFromDate())
                    + "    To: " + sdf.format(getToDate()),
                    normalFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(10);
            document.add(dateRange);

            PdfPTable table = new PdfPTable(11);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2.5f, 3f, 1.2f, 1.2f, 2f, 2f, 2f, 2f, 2f, 2f});

            String[] headers = {"BHT No", "Date Of Discharge", "Patient Name", "Policy Number", "Reference Number",
                "Billed Amount", "GOP Amount", "Paid by Patient", "Patient Due", "Paid by Company", "Company Due"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            DecimalFormat df = new DecimalFormat("#,##0.00");

            for (Map.Entry<Institution, List<InstitutionBillEncounter>> entry : getBillInstitutionEncounterMap().entrySet()) {
                Institution institution = entry.getKey();
                List<InstitutionBillEncounter> encounters = entry.getValue();

                PdfPCell groupHeader = new PdfPCell(new Phrase(institution.getName(), boldFont));
                groupHeader.setColspan(11);
                groupHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(groupHeader);

                for (InstitutionBillEncounter b : encounters) {
                    table.addCell(new Phrase(b.getBhtNo(), normalFont));
                    table.addCell(new Phrase(sdf.format(b.getDateOfDischarge()), normalFont));
                    table.addCell(new Phrase(b.getPatient().getPerson().getNameWithTitle(), normalFont));
                    table.addCell(new Phrase(getPolicyNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()), normalFont));
                    table.addCell(new Phrase(getReferenceNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()), normalFont));
                    table.addCell(new Phrase(df.format(b.getNetTotal()), normalFont));
                    table.addCell(new Phrase(df.format(b.getGopAmount()), normalFont));
                    table.addCell(new Phrase(df.format(b.getPaidByPatient()), normalFont));
                    table.addCell(new Phrase(df.format(b.getPatientDue()), normalFont));
                    table.addCell(new Phrase(df.format(b.getPaidByCompany()), normalFont));
                    table.addCell(new Phrase(df.format(b.getCompanyDue()), normalFont));
                }

                PdfPCell subtotalLabel = new PdfPCell(new Phrase("Sub Total", boldFont));
                subtotalLabel.setColspan(6);
                subtotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(subtotalLabel);
                table.addCell(new Phrase(df.format(getInstituteGopMap().get(institution)), boldFont));
                table.addCell(new Phrase("—", boldFont));
                table.addCell(new Phrase("—", boldFont));
                table.addCell(new Phrase(df.format(getInstitutPaidByCompanyMap().get(institution)), boldFont));
                table.addCell(new Phrase(df.format(getInstitutePayableByCompanyMap().get(institution)), boldFont));
            }

            PdfPCell netTotalLabel = new PdfPCell(new Phrase("Net Total", boldFont));
            netTotalLabel.setColspan(6);
            netTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(netTotalLabel);
            table.addCell(new Phrase(df.format(getGopAmount()), boldFont));
            table.addCell(new Phrase("—", boldFont));
            table.addCell(new Phrase("—", boldFont));
            table.addCell(new Phrase(df.format(getPaidByCompany()), boldFont));
            table.addCell(new Phrase(df.format(getPayableByCompany()), boldFont));

            document.add(table);

            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public double createInwardPaymentTotal(PatientEncounter pe, Date fd, Date td, BillType bt) {

        String sql;
        Map m = new HashMap();
        sql = "select sum(b.netTotal) from BilledBill b where "
                + " b.billType=:billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.refunded=false "
                + " and b.cancelled=false "
                + " and b.patientEncounter=:pe ";

        m.put("pe", pe);
        m.put("billType", bt);
        m.put("toDate", td);
        m.put("fromDate", fd);

        if (institutionOfDepartment != null) {
            sql += "AND b.institution = :insd ";
            m.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += "AND b.department = :dep ";
            m.put("dep", department);
        }

        if (site != null) {
            sql += "AND b.department.site = :site ";
            m.put("site", site);
        }

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double createInwardPaymentTotalCredit(PatientEncounter pe, Date fd, Date td, BillType bt) {

        String sql;
        Map m = new HashMap();
        sql = "select sum(bi.netValue) from BillItem bi where "
                + " bi.bill.billType=:billType "
                + " and type(bi.bill)=:cl "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.retired=false "
                + " and bi.bill.refunded=false "
                + " and bi.bill.cancelled=false "
                + " and bi.patientEncounter=:pe ";

        m.put("pe", pe);
        m.put("billType", bt);
        m.put("toDate", td);
        m.put("fromDate", fd);
        m.put("cl", BilledBill.class);

        if (institutionOfDepartment != null) {
            sql += "AND bi.bill.institution = :insd ";
            m.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += "AND bi.bill.department = :dep ";
            m.put("dep", department);
        }

        if (site != null) {
            sql += "AND bi.bill.department.site = :site ";
            m.put("site", site);
        }
//        //// // System.out.println("sql = " + sql);
        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    AdmissionType admissionType;
    PaymentMethod paymentMethod;
    @EJB
    PatientEncounterFacade patientEncounterFacade;

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public void createInwardCashDue() {
        Date startTime = new Date();

        String dateField = "admissionDate".equals(dateBasis) ? "b.dateOfAdmission" : "b.dateOfDischarge";
        HashMap m = new HashMap();
        String sql = " Select b from PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " where b.retired=false "
                + " and b.paymentFinalized=true "
                + " and " + dateField + " between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }
        // The company filter applies to the commitment bills' company (below), not the admission's own company.

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (institutionOfDepartment != null) {
            sql += "AND fb.institution = :insd ";
            m.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += "AND fb.department = :dep ";
            m.put("dep", department);
        }

        if (site != null) {
            sql += "AND fb.department.site = :site ";
            m.put("site", site);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        List<PatientEncounter> candidates = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        patientEncounters = new ArrayList<>();
        ccDueRows = new ArrayList<>();
        billed = 0;
        paidByPatient = 0;
        paidByCompany = 0;
        dueByCompany = 0;
        if (candidates == null) {
            return;
        }
        // Figures come from the commitment bills and payment bills (#25601), loaded once for the whole result.
        // One row per admission and commitment company with that company's due above 1 cent.
        loadCcFigures(candidates);
        for (PatientEncounter p : candidates) {
            if (p.getFinalBill() == null) {
                continue;
            }
            boolean first = true;
            for (CcCompanyFigures f : ccCompanyFigures(p)) {
                if (f.due() <= 0.01) {
                    continue;
                }
                if (institution != null && !isSameCompany(f.company, institution)) {
                    continue;
                }
                ccDueRows.add(new CompanyDueRow(p, f.company, f.committed, f.paid, first));
                if (first) {
                    // Admission-level figures are counted once per admission, not per company row
                    patientEncounters.add(p);
                    billed += p.getFinalBill().getNetTotal();
                    paidByPatient += getPatientPaid(p);
                    first = false;
                }
                paidByCompany += f.paid;
                dueByCompany += f.due();
            }
        }
    }

    /** Rows of the last Inward CC Due Search: one per admission and commitment company. */
    private List<CompanyDueRow> ccDueRows = new ArrayList<>();

    public List<CompanyDueRow> getCcDueRows() {
        return ccDueRows;
    }

    /** Number of distinct admissions in the last Inward CC Due Search (one admission can have a row per company). */
    public int getCcDueAdmissionCount() {
        return patientEncounters == null ? 0 : patientEncounters.size();
    }

    /** Total still owed by the credit companies over the last Inward CC Due Search. */
    public double getDueByCompany() {
        return dueByCompany;
    }

    public void createInwardCashDueData() {
        reportTimerController.trackReportExecution(() -> {
            Date startTime = new Date();

            HashMap m = new HashMap();
            String sql = " Select b from PatientEncounter b"
                    + " JOIN b.finalBill fb"
                    + " where b.retired=false "
                    + " and b.paymentFinalized=true "
                    + " and b.dateOfDischarge between :fd and :td ";
//                + " and (abs(b.finalBill.netTotal)-(abs(b.finalBill.paidAmount)+abs(b.creditPaidAmount))) > 0.1 ";

            if (admissionType != null) {
                sql += " and b.admissionType =:ad ";
                m.put("ad", admissionType);
            }
//        if (institution != null) {
//            sql += " and b.creditCompany =:ins ";
//            m.put("ins", institution);
//        }

            if (paymentMethod != null) {
                sql += " and b.paymentMethod =:pm ";
                m.put("pm", paymentMethod);
            }

            if (institutionOfDepartment != null) {
                sql += "AND fb.institution = :insd ";
                m.put("insd", institutionOfDepartment);
            }

            if (department != null) {
                sql += "AND fb.department = :dep ";
                m.put("dep", department);
            }

            if (site != null) {
                sql += "AND fb.department.site = :site ";
                m.put("site", site);
            }

            sql += " order by  b.dateOfDischarge";

            m.put("fd", fromDate);
            m.put("td", toDate);
            patientEncounters = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

            if (patientEncounters == null) {
                return;
            }

            updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(patientEncounters);

            setBillPatientEncounterMap(getCreditCompanyBills(patientEncounters, "any", null));

            setInstitutionBillPatientEncounterMap(InstitutionBillEncounter.createPatientEncounterBillEncounterMap(
                    InstitutionBillEncounter.createInstitutionBillEncounter(getBillPatientEncounterMap(),
                            "due", "any", institution, null)));

            calculateCreditCompanyAmounts();

            setEncounterCreditCompanyMap(getEncounterCreditCompanies());

            billed = 0;
            paidByPatient = 0;
            paidByCompany = 0;
            payableByPatient = 0;
            double peGop = 0.0;
            double pePaidByCompany = 0.0;

            Map<PatientEncounter, Double> billGopMap = new HashMap<>();
            Map<PatientEncounter, Double> billPaidByCompanyMap = new HashMap<>();

            for (PatientEncounter p : getInstitutionBillPatientEncounterMap().keySet()) {
                List<InstitutionBillEncounter> encounters = getInstitutionBillPatientEncounterMap().get(p);
                if (encounters == null || encounters.isEmpty()) {
                    continue;
                }
                peGop = encounters.stream()
                        .mapToDouble(InstitutionBillEncounter::getGopAmount)
                        .sum();
                pePaidByCompany = encounters.stream()
                        .mapToDouble(InstitutionBillEncounter::getPaidByCompany)
                        .sum();

                billed += p.getFinalBill().getNetTotal();
                paidByPatient += getInstitutionBillPatientEncounterMap().get(p).get(0).getPaidByPatient();
                paidByCompany += getInstitutionBillPatientEncounterMap().get(p).get(0).getTotalPaidByCompanies();
                payableByPatient += getInstitutionBillPatientEncounterMap().get(p).get(0).getPatientGopAmount();

                billGopMap.put(p, peGop);
                billPaidByCompanyMap.put(p, pePaidByCompany);
            }

            setPatientEncounterGopMap(billGopMap);
            setPatientEncounterPaidByCompanyMap(billPaidByCompanyMap);
        }, FinancialReport.INWARD_DUE_SEARCH, sessionController.getLoggedUser());
    }

    public String getPolicyNumberFromEncounterCreditCompanyMap(final String bht, final String creditCompanyName) {
        Map<String, EncounterCreditCompany> creditCompanyMap = getEncounterCreditCompanyMap().get(bht);

        if (creditCompanyMap != null) {
            EncounterCreditCompany ecc = creditCompanyMap.get(creditCompanyName);
            if (ecc != null) {
                return ecc.getPolicyNo();
            }
        }
        return "";
    }

    public String getReferenceNumberFromEncounterCreditCompanyMap(final String bht, final String creditCompanyName) {
        Map<String, EncounterCreditCompany> creditCompanyMap = getEncounterCreditCompanyMap().get(bht);

        if (creditCompanyMap != null) {
            EncounterCreditCompany ecc = creditCompanyMap.get(creditCompanyName);
            if (ecc != null) {
                return ecc.getReferanceNo();
            }
        }
        return "";
    }

    public double calculatePayableByPatient(final PatientEncounter patientEncounter, final List<Bill> bills) {
        final double netTotal = patientEncounter.getFinalBill().getNetTotal();

        final double payableNetTotalByCreditCompanies = bills.stream()
                .mapToDouble(Bill::getNetTotal)
                .sum();

        return netTotal - payableNetTotalByCreditCompanies;
    }

    public void createInwardCashExcess() {
        reportTimerController.trackReportExecution(() -> {
            Date startTime = new Date();

            HashMap m = new HashMap();
            String sql = " Select b from PatientEncounter b"
                    + " JOIN b.finalBill fb"
                    + " where b.retired=false "
                    + " and b.paymentFinalized=true "
                    + " and b.dateOfDischarge between :fd and :td ";

            if (admissionType != null) {
                sql += " and b.admissionType =:ad ";
                m.put("ad", admissionType);
            }
//        if (institution != null) {
//            sql += " and b.creditCompany =:ins ";
//            m.put("ins", institution);
//        }

            if (paymentMethod != null) {
                sql += " and b.paymentMethod =:pm ";
                m.put("pm", paymentMethod);
            }

            if (institutionOfDepartment != null) {
                sql += "AND fb.institution = :insd ";
                m.put("insd", institutionOfDepartment);
            }

            if (department != null) {
                sql += "AND fb.department = :dep ";
                m.put("dep", department);
            }

            if (site != null) {
                sql += "AND fb.department.site = :site ";
                m.put("site", site);
            }

            sql += " order by  b.dateOfDischarge";

            m.put("fd", fromDate);
            m.put("td", toDate);
            patientEncounters = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

            if (patientEncounters == null) {
                return;
            }

            updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(patientEncounters);

            setBillPatientEncounterMap(getCreditCompanyBills(patientEncounters, "excess", institution));
            calculateCreditCompanyAmounts();

            billed = 0;
            paidByPatient = 0;
            paidByCompany = 0;
            for (PatientEncounter p : getBillPatientEncounterMap().keySet()) {
                billed += p.getFinalBill().getNetTotal();
                paidByPatient += p.getFinalBill().getSettledAmountByPatient();
                paidByCompany += p.getFinalBill().getSettledAmountBySponsor();
            }
        }, FinancialReport.INWARD_CASH_EXCESS, sessionController.getLoggedUser());
    }

    // Map<bht, Map<Credit Company, Encounter Credit Company>>
    private Map<String, Map<String, EncounterCreditCompany>> getEncounterCreditCompanies() {
        List<Long> patientEncounterIds = getBillPatientEncounterMap().keySet().stream()
                .map(PatientEncounter::getId)
                .collect(Collectors.toList());

        if (patientEncounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT ecc FROM EncounterCreditCompany ecc WHERE ecc.patientEncounter.id IN :patientEncounterIds";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("patientEncounterIds", patientEncounterIds);

        List<EncounterCreditCompany> results = encounterCreditCompanyFacade.findByJpql(jpql, parameters);

        Map<String, Map<String, EncounterCreditCompany>> encounterCreditCompanyMap = new HashMap<>();

        for (EncounterCreditCompany ecc : results) {
            String bhtNo = ecc.getPatientEncounter().getBhtNo();
            String institutionName = ecc.getInstitution().getName();

            encounterCreditCompanyMap
                    .computeIfAbsent(bhtNo, k -> new HashMap<>())
                    .putIfAbsent(institutionName, ecc);
        }

        return encounterCreditCompanyMap;
    }

    private Map<PatientEncounter, List<Bill>> getCreditCompanyBills(List<PatientEncounter> patientEncounters, String dueType,
            Institution filteringCreditCompany) {
        if (dueType == null || (!dueType.equalsIgnoreCase("due") && !dueType.equalsIgnoreCase("any")
                && !dueType.equalsIgnoreCase("excess") && !dueType.equalsIgnoreCase("settled"))) {
            return Collections.emptyMap();
        }

        List<Long> patientEncounterIds = patientEncounters.stream()
                .map(PatientEncounter::getId)
                .collect(Collectors.toList());

        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        bts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

        String jpql = "SELECT bill from Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.patientEncounter.id in :patientEncounterIds ";

        parameters.put("br", true);
        parameters.put("patientEncounterIds", patientEncounterIds);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        jpql += "AND bill.referenceBill.confirmedFinalBill = true ";

        if (filteringCreditCompany != null) {
            jpql += " and bill.creditCompany =:ins ";
            parameters.put("ins", filteringCreditCompany);
        }

        List<Bill> rs = (List<Bill>) billFacade.findByJpql(jpql, parameters);

        List<Bill> detachedClones = rs.stream()
                .map(b -> {
                    Bill clonedBill = new Bill();
                    clonedBill.clone(b);
                    return clonedBill;
                })
                .collect(Collectors.toList());

        Map<Long, PatientEncounter> encounterMap = patientEncounters.stream()
                .collect(Collectors.toMap(PatientEncounter::getId, pe -> pe));

        if (dueType.equalsIgnoreCase("settled")) {
            return detachedClones.stream()
                    .filter(bill -> {
                        PatientEncounter pe = encounterMap.get(bill.getPatientEncounter().getId());
                        Bill referenceBill = bill.getReferenceBill();

                        if (pe == null || pe.getFinalBill() == null || referenceBill == null) {
                            return false;
                        }

                        if (referenceBill.isCancelled() || referenceBill.isRefunded()) {
                            bill.setNetTotal(0.0);
                        }

                        double netTotal = pe.getFinalBill().getNetTotal();
                        double settledByPatient = pe.getFinalBill().getSettledAmountByPatient();
                        double settledBySponsor = pe.getFinalBill().getSettledAmountBySponsor();

                        return (netTotal - settledByPatient - settledBySponsor) == 0;
                    })
                    .collect(Collectors.groupingBy(
                            bill -> encounterMap.get(bill.getPatientEncounter().getId())
                    ));
        }

        if (dueType.equalsIgnoreCase("excess")) {
            return detachedClones.stream()
                    .filter(bill -> {
                        PatientEncounter pe = encounterMap.get(bill.getPatientEncounter().getId());
                        Bill referenceBill = bill.getReferenceBill();

                        if (pe == null || pe.getFinalBill() == null || referenceBill == null) {
                            return false;
                        }

                        if (referenceBill.isCancelled() || referenceBill.isRefunded()) {
                            bill.setNetTotal(0.0);
                        }

                        double netTotal = pe.getFinalBill().getNetTotal();
                        double settledByPatient = pe.getFinalBill().getSettledAmountByPatient();
                        double settledBySponsor = pe.getFinalBill().getSettledAmountBySponsor();

                        return (netTotal - settledByPatient - settledBySponsor) < 0;
                    })
                    .collect(Collectors.groupingBy(
                            bill -> encounterMap.get(bill.getPatientEncounter().getId())
                    ));
        }

        if (dueType.equalsIgnoreCase("due")) {
            return detachedClones.stream()
                    .filter(bill -> {
                        PatientEncounter pe = encounterMap.get(bill.getPatientEncounter().getId());

                        Bill referenceBill = bill.getReferenceBill();

                        if (pe == null || pe.getFinalBill() == null || referenceBill == null) {
                            return false;
                        }

                        if (referenceBill.isCancelled() || referenceBill.isRefunded()) {
                            bill.setNetTotal(0.0);
                        }

                        double netTotal = pe.getFinalBill().getNetTotal();
                        double settledByPatient = pe.getFinalBill().getSettledAmountByPatient();
                        double settledBySponsor = pe.getFinalBill().getSettledAmountBySponsor();

                        return (netTotal - settledByPatient - settledBySponsor) > 0;
                    })
                    .collect(Collectors.groupingBy(
                            bill -> encounterMap.get(bill.getPatientEncounter().getId())
                    ));
        }

        return detachedClones.stream()
                .collect(Collectors.groupingBy(
                        bill -> encounterMap.get(bill.getPatientEncounter().getId())
                ));
    }

    private void calculateCreditCompanyAmounts() {
        for (Map.Entry<PatientEncounter, List<Bill>> entry : getBillPatientEncounterMap().entrySet()) {

            PatientEncounter patientEncounter = entry.getKey();
            List<Bill> bills = entry.getValue();

            double totalPayableByCompanies = bills.stream().mapToDouble(Bill::getNetTotal).sum();
            double totalPaidByCompanies = bills.stream().mapToDouble(Bill::getPaidAmount).sum();

            patientEncounter.setTransPaid(totalPayableByCompanies);
            patientEncounter.setTransPaidByCompany(totalPaidByCompanies);
        }
    }

    private void removeSettledAndExcessBills(List<PatientEncounter> patientEncounters) {
        loadCcFigures(patientEncounters);
        // Outstanding = what the companies still owe + what the patient still owes (#25601)
        patientEncounters.removeIf(p -> getCcDue(p) + getPatientCopaymentDue(p) <= 0);
    }

    private void updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(List<PatientEncounter> patientEncounters) {
        if (patientEncounters == null || patientEncounters.isEmpty()) {
            return;
        }

        Map<Long, Double> settledAmounts = calculateSettledSponsorBillIP(patientEncounters);

        for (PatientEncounter patientEncounter : patientEncounters) {
            Bill finalBill = patientEncounter.getFinalBill();

            if (finalBill.isCancelled() || finalBill.isRefunded()) {
                continue;
            }

            List<Bill> bills = calculateSettledPatientBillIP(finalBill);
            double total = bills.stream().mapToDouble(Bill::getNetTotal).sum();

            synchronized (finalBill) {
                finalBill.setSettledAmountByPatient(total);
            }

            total = settledAmounts.getOrDefault(patientEncounter.getId(), 0.0);

            synchronized (finalBill) {
                finalBill.setSettledAmountBySponsor(total);
            }
        }
    }

    private Map<Long, Double> calculateSettledSponsorBillIP(List<PatientEncounter> patientEncounters) {
        List<Long> patientEncounterIds = patientEncounters.stream()
                .map(PatientEncounter::getId)
                .collect(Collectors.toList());

        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        bts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

        String jpql = "SELECT bill from Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.patientEncounter.id in :patientEncounterIds ";

        parameters.put("br", true);
        parameters.put("patientEncounterIds", patientEncounterIds);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        jpql += "AND bill.referenceBill.confirmedFinalBill = true ";

        if (institution != null) {
            jpql += " and bill.creditCompany =:ins ";
            parameters.put("ins", institution);
        }

        List<Bill> rs = (List<Bill>) billFacade.findByJpql(jpql, parameters);

        return rs.stream()
                .collect(Collectors.groupingBy(
                        bill -> bill.getPatientEncounter().getId(),
                        Collectors.summingDouble(Bill::getPaidAmount)
                ));
    }

    private void updateSettledAmountsForIP(List<PatientEncounter> patientEncounters) {
        for (PatientEncounter patientEncounter : patientEncounters) {
            Bill finalBill = patientEncounter.getFinalBill();

            if (finalBill.isCancelled() || finalBill.isRefunded()) {
                continue;
            }

            List<Bill> bills = calculateSettledPatientBillIP(finalBill);
            double total = bills.stream().mapToDouble(Bill::getNetTotal).sum();

            synchronized (finalBill) {
                finalBill.setSettledAmountByPatient(total);
            }

            bills = calculateSettledSponsorBillIP(finalBill);
            total = bills.stream().mapToDouble(Bill::getNetTotal).sum();

            synchronized (finalBill) {
                finalBill.setSettledAmountBySponsor(total);
            }
        }
    }

    private List<Bill> calculateSettledPatientBillIP(Bill bill) {
        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        bts.add(BillTypeAtomic.INWARD_PAYMENT_CANCELLATION);
        bts.add(BillTypeAtomic.INWARD_PAYMENT);
        bts.add(BillTypeAtomic.INWARD_PAYMENT_REFUND);

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br ";

        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        jpql += "AND bill.forwardReferenceBill.id = :rb ";
        parameters.put("rb", bill.getId());

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        return rs.stream().map(ReportTemplateRow::getBill).collect(Collectors.toList());
    }

    private List<Bill> calculateSettledSponsorBillIP(Bill bill) {
        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        bts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        bts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);

        parameters.put("br", true);
        parameters.put("bts", bts);
        parameters.put("rb", bill.getId());

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.billTypeAtomic in :bts "
                + "AND bill.forwardReferenceBill.id = :rb ";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        List<Bill> bills = rs.stream().map(ReportTemplateRow::getBill).collect(Collectors.toList());

        String sql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.billTypeAtomic in :bts "
                + "AND bill.billedBill.forwardReferenceBill.id = :rb ";

        rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP);

        bills.addAll(rs.stream().map(ReportTemplateRow::getBill).collect(Collectors.toList()));

        return bills;
    }

    double billed;
    double paidByPatient;
    double paidByCompany;
    private double dueByCompany;

    public double getBilled() {
        return billed;
    }

    public void setBilled(double billed) {
        this.billed = billed;
    }

    public double getPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(double paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public double getPaidByCompany() {
        return paidByCompany;
    }

    public void setPaidByCompany(double paidByCompany) {
        this.paidByCompany = paidByCompany;
    }

    public void exportInwardCashDueToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Inward_Cash_Due_Report.xlsx");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MM yyyy hh:mm:ss a");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Inward Cash Due");
            int rowIndex = 0;

            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            Row mainHeader = sheet.createRow(rowIndex++);
            Cell headerCell = mainHeader.createCell(0);
            headerCell.setCellValue("Inward Cash Due");
            headerCell.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 15));

            Row columnHeader = sheet.createRow(rowIndex++);
            String[] headers = {
                "", "BHT", "Admitted At", "Discharged At", "Final Total", "GOP by Patient", "Paid by Patient",
                "Patient Due", "Paid by Companies", "Total Due", "Company Details"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = columnHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 10, 15));

            int counter = 1;
            for (Map.Entry<PatientEncounter, List<InstitutionBillEncounter>> entry : institutionBillPatientEncounterMap.entrySet()) {
                PatientEncounter pe = entry.getKey();
                List<InstitutionBillEncounter> bills = entry.getValue();

                Row row = sheet.createRow(rowIndex++);
                int col = 0;

                row.createCell(col++).setCellValue(counter++);
                row.createCell(col++).setCellValue(pe.getBhtNo());
                row.createCell(col++).setCellValue(sdf.format(pe.getDateOfAdmission()));
                row.createCell(col++).setCellValue(sdf.format(pe.getDateOfDischarge()));
                row.createCell(col++).setCellValue(pe.getFinalBill().getNetTotal());
                row.createCell(col++).setCellValue(bills.get(0).getPatientGopAmount());
                row.createCell(col++).setCellValue(bills.get(0).getPaidByPatient());
                row.createCell(col++).setCellValue(bills.get(0).getPatientDue());
                row.createCell(col++).setCellValue(bills.get(0).getTotalPaidByCompanies());
                row.createCell(col++).setCellValue(bills.get(0).getTotalDue());

                Row subHeader = sheet.createRow(rowIndex++);
                String[] innerHeaders = {
                    "Company Name", "Policy Number", "Reference Number", "GOP by Company",
                    "Paid by Company", "Company Due"
                };
                for (int i = 0; i < innerHeaders.length; i++) {
                    Cell cell = subHeader.createCell(i + 10);
                    cell.setCellValue(innerHeaders[i]);
                    cell.setCellStyle(boldStyle);
                }

                for (InstitutionBillEncounter bill : bills) {
                    Row billRow = sheet.createRow(rowIndex++);
                    billRow.createCell(10).setCellValue(bill.getInstitution().getName());
                    billRow.createCell(11).setCellValue(getPolicyNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()));
                    billRow.createCell(12).setCellValue(getReferenceNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()));
                    billRow.createCell(13).setCellValue(bill.getGopAmount());
                    billRow.createCell(14).setCellValue(bill.getPaidByCompany());
                    billRow.createCell(15).setCellValue(bill.getCompanyDue() != 0 ? bill.getCompanyDue() : bill.getCompanyExcess());
                }

                Row totalsRow = sheet.createRow(rowIndex++);
                Cell totalLabelCell = totalsRow.createCell(10);
                totalLabelCell.setCellValue("Total");
                totalLabelCell.setCellStyle(boldStyle);

                Cell total1 = totalsRow.createCell(13);
                total1.setCellValue(patientEncounterGopMap.get(pe));
                total1.setCellStyle(boldStyle);

                Cell total2 = totalsRow.createCell(14);
                total2.setCellValue(patientEncounterPaidByCompanyMap.get(pe));
                total2.setCellStyle(boldStyle);

                Cell total3 = totalsRow.createCell(15);
                total3.setCellValue(patientEncounterGopMap.get(pe) - patientEncounterPaidByCompanyMap.get(pe));
                total3.setCellStyle(boldStyle);
            }

            Row totalFooter = sheet.createRow(rowIndex++);
            Cell footerLabel = totalFooter.createCell(0);
            footerLabel.setCellValue("Total");
            footerLabel.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 3));

            int[] footerCols = {4, 5, 6, 7, 8, 9};
            double[] footerValues = {
                getBilled(),
                getPayableByPatient(),
                getPaidByPatient(),
                getPayableByPatient() - getPaidByPatient(),
                getPaidByCompany(),
                getBilled() - (getPaidByCompany() + getPaidByPatient())
            };

            for (int i = 0; i < footerCols.length; i++) {
                Cell cell = totalFooter.createCell(footerCols[i]);
                cell.setCellValue(footerValues[i]);
                cell.setCellStyle(boldStyle);
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage());
        }
    }

    public void exportInwardCashDueToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Inward_Cash_Due_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);

            document.open();

            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Inward Cash Due", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            PdfPTable mainTable = new PdfPTable(11);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new float[]{0.6f, 1.2f, 1.8f, 1.8f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 8.0f});

            String[] headers = {"", "BHT", "Admitted At", "Discharged At", "Final Total", "GOP by Patient", "Paid by Patient", "Patient Due", "Paid by Companies", "Total Due", "Company Details"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                mainTable.addCell(cell);
            }

            int counter = 1;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");

            for (Map.Entry<PatientEncounter, List<InstitutionBillEncounter>> entry : institutionBillPatientEncounterMap.entrySet()) {
                PatientEncounter pe = entry.getKey();
                List<InstitutionBillEncounter> bills = entry.getValue();

                mainTable.addCell(new Phrase(String.valueOf(counter++), normalFont));
                mainTable.addCell(new Phrase(pe.getBhtNo(), normalFont));
                mainTable.addCell(new Phrase(sdf.format(pe.getDateOfAdmission()), normalFont));
                mainTable.addCell(new Phrase(sdf.format(pe.getDateOfDischarge()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(pe.getFinalBill().getNetTotal()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getPatientGopAmount()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getPaidByPatient()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getPatientDue()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getTotalPaidByCompanies()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getTotalDue()), normalFont));

                PdfPTable nestedTable = new PdfPTable(6);
                nestedTable.setWidthPercentage(100);
                nestedTable.setWidths(new float[]{3f, 2.5f, 2.5f, 2f, 2f, 2f});

                String[] subHeaders = {"Company Name", "Policy Number", "Reference Number", "GOP by Company", "Paid by Company", "Company Due"};
                for (String sh : subHeaders) {
                    PdfPCell shCell = new PdfPCell(new Phrase(sh, boldFont));
                    shCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    nestedTable.addCell(shCell);
                }

                for (InstitutionBillEncounter bill : bills) {
                    nestedTable.addCell(new Phrase(bill.getInstitution().getName(), normalFont));
                    nestedTable.addCell(new Phrase(getPolicyNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()), normalFont));
                    nestedTable.addCell(new Phrase(getReferenceNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()), normalFont));
                    nestedTable.addCell(new Phrase(String.valueOf(bill.getGopAmount()), normalFont));
                    nestedTable.addCell(new Phrase(String.valueOf(bill.getPaidByCompany()), normalFont));
                    nestedTable.addCell(new Phrase(String.valueOf(bill.getCompanyDue() != 0 ? bill.getCompanyDue() : bill.getCompanyExcess()), normalFont));
                }

                nestedTable.addCell(new Phrase("Total", boldFont));
                nestedTable.addCell("");
                nestedTable.addCell("");
                nestedTable.addCell(new Phrase(String.valueOf(patientEncounterGopMap.get(pe)), boldFont));
                nestedTable.addCell(new Phrase(String.valueOf(patientEncounterPaidByCompanyMap.get(pe)), boldFont));
                nestedTable.addCell(new Phrase(String.valueOf(patientEncounterGopMap.get(pe) - patientEncounterPaidByCompanyMap.get(pe)), boldFont));

                PdfPCell nestedCell = new PdfPCell(nestedTable);
                nestedCell.setColspan(1);
                mainTable.addCell(nestedCell);
            }

            PdfPCell footerLabel = new PdfPCell(new Phrase("Total", boldFont));
            footerLabel.setColspan(4);
            footerLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            mainTable.addCell(footerLabel);

            mainTable.addCell(new Phrase(String.valueOf(getBilled()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPayableByPatient()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPaidByPatient()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPayableByPatient() - getPaidByPatient()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPaidByCompany()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getBilled() - (getPaidByCompany() + getPaidByPatient())), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(""), boldFont));

            document.add(mainTable);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(Level.SEVERE, e.getMessage());
        }
    }

    public void createInwardCreditAccess() {
        institutionEncounters = new ArrayList<>();
        creditAccessGroups = new ArrayList<>();
        // An admission is listed under each company it has figures for; each row shows that
        // company's own committed, paid and balance. Admission-level figures (final total,
        // paid by patient) are shown and counted on the admission's first row only (#25601).
        Set<Long> seenAdmissions = new HashSet<>();
        for (Map.Entry<Institution, List<PatientEncounter>> entry
                : searchSettledAdmissionsByCreditCompany(PaymentMethod.Credit, false).entrySet()) {
            Institution company = entry.getKey();
            CreditAccessGroup group = new CreditAccessGroup(company);
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(company);
            newIns.setPatientEncounters(entry.getValue());

            for (PatientEncounter b : entry.getValue()) {
                boolean first = b.getId() == null || seenAdmissions.add(b.getId());
                CompanyDueRow row = new CompanyDueRow(b, company,
                        getCcCommittedForCompany(b, company), getCcPaidForCompany(b, company), first);
                group.rows.add(row);
                group.committedTotal += row.getCommitted();
                group.paidTotal += row.getPaid();
                if (first) {
                    group.finalTotal += b.getFinalBill().getNetTotal();
                    group.patientPaidTotal += getPatientPaid(b);
                }
            }
            group.committedTotal = com.divudi.core.util.CommonFunctions.round(group.committedTotal);
            group.paidTotal = com.divudi.core.util.CommonFunctions.round(group.paidTotal);
            group.finalTotal = com.divudi.core.util.CommonFunctions.round(group.finalTotal);
            group.patientPaidTotal = com.divudi.core.util.CommonFunctions.round(group.patientPaidTotal);
            newIns.setTotal(group.committedTotal);
            newIns.setPaidTotal(group.paidTotal);
            institutionEncounters.add(newIns);
            creditAccessGroups.add(group);
        }
    }

    /** Groups of the last Inward Credit Access search: one per commitment company. */
    private List<CreditAccessGroup> creditAccessGroups = new ArrayList<>();

    public List<CreditAccessGroup> getCreditAccessGroups() {
        return creditAccessGroups;
    }

    /** One company's rows on the Inward Credit Access page, with its totals (admission-level totals count each admission once across the whole page). */
    public static class CreditAccessGroup {

        private final Institution institution;
        private final List<CompanyDueRow> rows = new ArrayList<>();
        private double committedTotal;
        private double paidTotal;
        private double finalTotal;
        private double patientPaidTotal;

        private CreditAccessGroup(Institution institution) {
            this.institution = institution;
        }

        public Institution getInstitution() {
            return institution;
        }

        public List<CompanyDueRow> getRows() {
            return rows;
        }

        public double getCommittedTotal() {
            return committedTotal;
        }

        public double getPaidTotal() {
            return paidTotal;
        }

        /** Committed less paid for the company, signed. */
        public double getBalanceTotal() {
            return committedTotal - paidTotal;
        }

        /** Paid less committed for the company, signed (the Credit Access page direction). */
        public double getExcessTotal() {
            return paidTotal - committedTotal;
        }

        public double getFinalTotal() {
            return finalTotal;
        }

        public double getPatientPaidTotal() {
            return patientPaidTotal;
        }
    }

    /**
     * Discharged admissions of the given payment method whose bill is fully
     * covered (net total less patient money less credit company money is not
     * above 1 cent; tested on the whole admission), listed under each
     * commitment company the admission has figures for, in name order. Patient and
     * company money come from the payment and commitment bills, loaded once
     * for the whole result (#25601), not from the encounter's creditPaidAmount.
     */
    private Map<Institution, List<PatientEncounter>> searchSettledAdmissionsByCreditCompany(
            PaymentMethod pm, boolean applyDepartmentFilters) {
        HashMap<String, Object> m = new HashMap<>();
        String sql = "Select b From PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " where b.retired=false"
                + " and b.discharged=true"
                + " and b.paymentMethod=:pm"
                + " and b.dateOfDischarge between :frm and :to";
        if (applyDepartmentFilters) {
            if (institutionOfDepartment != null) {
                sql += " and fb.institution = :insd";
                m.put("insd", institutionOfDepartment);
            }
            if (department != null) {
                sql += " and fb.department = :dep";
                m.put("dep", department);
            }
            if (site != null) {
                sql += " and fb.department.site = :site";
                m.put("site", site);
            }
        }
        sql += " order by b.dateOfDischarge";
        m.put("pm", pm);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        List<PatientEncounter> candidates = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        if (candidates == null) {
            return new LinkedHashMap<>();
        }
        loadCcFigures(candidates);
        List<PatientEncounter> kept = new ArrayList<>();
        for (PatientEncounter b : candidates) {
            if (b.getFinalBill() == null) {
                continue;
            }
            // The fully covered test stays on the whole admission; only the rows are split by company.
            double outstanding = Math.round((b.getFinalBill().getNetTotal() - getPatientPaid(b) - getCcPaid(b)) * 100.0) / 100.0;
            if (outstanding > 0.01) {
                continue;
            }
            kept.add(b);
        }
        return groupByCommitmentCompany(kept, false);
    }

//    public void createInwardCreditAccessWithFilters() {
//        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(),
//                PaymentMethod.Credit, false, institutionOfDepartment, department, site);
//
//        institutionEncounters = new ArrayList<>();
//        for (Institution ins : setIns) {
//            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(),
//                    PaymentMethod.Credit, false, institutionOfDepartment, department, site);
//
//            if (lst != null && !lst.isEmpty()) {
//                updateSettledAmountsForIP(lst);
//            }
//
//            InstitutionEncounters newIns = new InstitutionEncounters();
//            newIns.setInstitution(ins);
//            newIns.setPatientEncounters(lst);
//
//            for (PatientEncounter b : lst) {
//                newIns.setTotal(newIns.getTotal() + b.getFinalBill().getNetTotal());
//                newIns.setPaidTotal(newIns.getPaidTotal() + b.getFinalBill().getSettledAmountBySponsor() + b.getFinalBill().getSettledAmountByPatient());
//            }
//
//            newIns.setTotal(com.divudi.core.util.CommonFunctions.round(newIns.getTotal()));
//            newIns.setPaidTotal(com.divudi.core.util.CommonFunctions.round(newIns.getPaidTotal()));
//            institutionEncounters.add(newIns);
//        }
//    }
    public void createInwardCreditAccessWithFilters() {
        reportTimerController.trackReportExecution(() -> {
            HashMap m = new HashMap();
            String sql = " Select b from PatientEncounter b"
                    + " JOIN b.finalBill fb"
                    + " where b.retired=false "
                    + " and b.paymentFinalized=true "
                    + " and b.dateOfDischarge between :fd and :td ";

            if (admissionType != null) {
                sql += " and b.admissionType =:ad ";
                m.put("ad", admissionType);
            }

            if (paymentMethod != null) {
                sql += " and b.paymentMethod =:pm ";
                m.put("pm", paymentMethod);
            }

            if (institutionOfDepartment != null) {
                sql += "AND fb.institution = :insd ";
                m.put("insd", institutionOfDepartment);
            }

            if (department != null) {
                sql += "AND fb.department = :dep ";
                m.put("dep", department);
            }

            if (site != null) {
                sql += "AND fb.department.site = :site ";
                m.put("site", site);
            }

            sql += " order by  b.dateOfDischarge";

            m.put("fd", fromDate);
            m.put("td", toDate);
            patientEncounters = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

            if (patientEncounters == null) {
                return;
            }

            updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(patientEncounters);

            setBillPatientEncounterMap(getCreditCompanyBills(patientEncounters, "any", institution));
            calculateCreditCompanyAmounts();

            List<InstitutionBillEncounter> institutionEncounters = new ArrayList<>(
                    InstitutionBillEncounter.createInstitutionBillEncounter(getBillPatientEncounterMap(), "excess", "any"));

            setBillInstitutionEncounterMap(InstitutionBillEncounter.createInstitutionBillEncounterMap(institutionEncounters));
            calculateCreditCompanyExcessTotals();

            setEncounterCreditCompanyMap(getEncounterCreditCompanies());
        }, FinancialReport.INWARD_CREDIT_EXCESS, sessionController.getLoggedUser());
    }

    public void exportCreditCompanyInwardExcessToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Excess_Report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Inward Credit Excess");
            int rowIndex = 0;

            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);

            XSSFCellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            XSSFCellStyle mergedStyle = workbook.createCellStyle();
            mergedStyle.cloneStyleFrom(amountStyle);
            mergedStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {"BHT No", "Date Of Discharge", "Patient Name", "Policy Number", "Reference Number",
                "Billed Amount", "Paid by Patient", "Paid by Company", "Excess Amount"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            for (Map.Entry<Institution, List<InstitutionBillEncounter>> entry : getBillInstitutionEncounterMap().entrySet()) {
                Institution institution = entry.getKey();
                List<InstitutionBillEncounter> encounters = entry.getValue();

                Row instRow = sheet.createRow(rowIndex++);
                Cell instCell = instRow.createCell(0);
                instCell.setCellValue(institution.getName());
                instCell.setCellStyle(boldStyle);

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                for (InstitutionBillEncounter b : encounters) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(b.getBhtNo());
                    dataRow.createCell(1).setCellValue(b.getDateOfDischarge() != null ? sdf.format(b.getDateOfDischarge()) : "");
                    dataRow.createCell(2).setCellValue(b.getPatient().getPerson().getNameWithTitle());
                    dataRow.createCell(3).setCellValue(getPolicyNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()));
                    dataRow.createCell(4).setCellValue(getReferenceNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()));
                    dataRow.createCell(5).setCellValue(b.getNetTotal());
                    dataRow.createCell(6).setCellValue(b.getPaidByPatient());
                    dataRow.createCell(7).setCellValue(b.getPaidByCompany());
                    dataRow.createCell(8).setCellValue(b.getCompanyDue());

                    for (int j = 5; j <= 8; j++) {
                        dataRow.getCell(j).setCellStyle(amountStyle);
                    }
                }

                Row subtotalRow = sheet.createRow(rowIndex++);
                subtotalRow.createCell(7).setCellValue(getInstitutPaidByCompanyMap().get(institution));
                subtotalRow.createCell(8).setCellValue(getInstitutePayableByCompanyMap().get(institution));

                subtotalRow.getCell(7).setCellStyle(mergedStyle);
                subtotalRow.getCell(8).setCellStyle(mergedStyle);
            }

            Row footerRow = sheet.createRow(rowIndex++);
            footerRow.createCell(0).setCellValue("Total");
            footerRow.getCell(0).setCellStyle(boldStyle);

            footerRow.createCell(7).setCellValue(getPaidByCompany());
            footerRow.createCell(8).setCellValue(getPayableByCompany());

            footerRow.getCell(7).setCellStyle(mergedStyle);
            footerRow.getCell(8).setCellStyle(mergedStyle);

            for (int i = 0; i <= 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void exportCreditCompanyInwardExcessToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Excess_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("EXCESS SEARCH", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            Paragraph dateRange = new Paragraph(
                    "From: " + sdf.format(getFromDate())
                    + "    To: " + sdf.format(getToDate()),
                    normalFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(10);
            document.add(dateRange);

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2.5f, 3f, 1.2f, 1.2f, 2f, 2f, 2f, 2f});

            String[] headers = {"BHT No", "Date Of Discharge", "Patient Name", "Policy Number", "Reference Number",
                "Billed Amount", "Paid by Patient", "Paid by Company", "Excess Amount"};

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            DecimalFormat df = new DecimalFormat("#,##0.00");

            for (Map.Entry<Institution, List<InstitutionBillEncounter>> entry : getBillInstitutionEncounterMap().entrySet()) {
                Institution institution = entry.getKey();
                List<InstitutionBillEncounter> encounters = entry.getValue();

                PdfPCell groupHeader = new PdfPCell(new Phrase(institution.getName(), boldFont));
                groupHeader.setColspan(9);
                groupHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(groupHeader);

                for (InstitutionBillEncounter b : encounters) {
                    table.addCell(new Phrase(b.getBhtNo(), normalFont));
                    table.addCell(new Phrase(sdf.format(b.getDateOfDischarge()), normalFont));
                    table.addCell(new Phrase(b.getPatient().getPerson().getNameWithTitle(), normalFont));
                    table.addCell(new Phrase(getPolicyNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()), normalFont));
                    table.addCell(new Phrase(getReferenceNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()), normalFont));
                    table.addCell(new Phrase(df.format(b.getNetTotal()), normalFont));
                    table.addCell(new Phrase(df.format(b.getPaidByPatient()), normalFont));
                    table.addCell(new Phrase(df.format(b.getPaidByCompany()), normalFont));
                    table.addCell(new Phrase(df.format(b.getCompanyDue()), normalFont));
                }

                PdfPCell subtotalLabel = new PdfPCell(new Phrase("Sub Total", boldFont));
                subtotalLabel.setColspan(6);
                subtotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(subtotalLabel);
                table.addCell(new Phrase("—", boldFont));
                table.addCell(new Phrase(df.format(getInstitutPaidByCompanyMap().get(institution)), boldFont));
                table.addCell(new Phrase(df.format(getInstitutePayableByCompanyMap().get(institution)), boldFont));
            }

            PdfPCell netTotalLabel = new PdfPCell(new Phrase("Net Total", boldFont));
            netTotalLabel.setColspan(6);
            netTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(netTotalLabel);
            table.addCell(new Phrase("—", boldFont));
            table.addCell(new Phrase(df.format(getPaidByCompany()), boldFont));
            table.addCell(new Phrase(df.format(getPayableByCompany()), boldFont));

            document.add(table);

            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void createInwardCashAccess() {
        Date startTime = new Date();
        fillCashAccess(false);
    }

    public void createInwardCashAccessWithFilters() {
        Date startTime = new Date();
        fillCashAccess(true);
    }

    private void fillCashAccess(boolean applyDepartmentFilters) {
        institutionEncounters = new ArrayList<>();
        for (Map.Entry<Institution, List<PatientEncounter>> entry
                : searchSettledAdmissionsByCreditCompany(PaymentMethod.Cash, applyDepartmentFilters).entrySet()) {
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(entry.getKey());
            newIns.setPatientEncounters(entry.getValue());

            for (PatientEncounter b : entry.getValue()) {
                newIns.setTotal(newIns.getTotal() + getCcCommittedForCompany(b, entry.getKey()));
                newIns.setPaidTotal(newIns.getPaidTotal() + getCcPaidForCompany(b, entry.getKey()));
            }

            institutionEncounters.add(newIns);
        }
    }

    public void createInwardPaymentBills() {
        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(),
                PaymentMethod.Credit, false, institutionOfDepartment, department, site);

        patientEncounters = new ArrayList<>();
        finalTotal = 0;
        finalPaidTotal = 0;

        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(),
                    PaymentMethod.Credit, false, institutionOfDepartment, department, site);

            if (lst != null && !lst.isEmpty()) {
                updateSettledAmountsForIP(lst);
            }

            for (PatientEncounter b : lst) {
                finalTotal += b.getFinalBill().getNetTotal();
                finalPaidTotal += b.getFinalBill().getSettledAmountByPatient() + b.getFinalBill().getSettledAmountBySponsor();
            }

            patientEncounters.addAll(lst);
        }
    }

    public List<InstitutionBills> getItems() {
        return items;
    }

    private Institution institution;

    public List<Bill> getItems2() {
        String sql;
        HashMap hm;

        sql = "Select b From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double getCreditTotal() {
        String sql;
        HashMap hm;

        sql = "Select sum(b.netTotal) From Bill b where b.retired=false and b.createdAt "
                + "  between :frm and :to and b.creditCompany=:cc "
                + " and b.paymentMethod= :pm and b.billType=:tp";
        hm = new HashMap();
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("cc", getInstitution());
        hm.put("pm", PaymentMethod.Credit);
        hm.put("tp", BillType.OpdBill);
        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void downloadExcel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        boolean hideStaffFee = configOptionApplicationController != null
                && configOptionApplicationController.getBooleanValueByKey("OPD Due Search - Hide Staff Fee", false);

        List<InstitutionBills> data = getItems() == null ? Collections.emptyList() : getItems();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("OPD Credit Due");

            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 15);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle filterStyle = workbook.createCellStyle();
            filterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle sectionHeaderStyle = workbook.createCellStyle();
            sectionHeaderStyle.setFont(boldFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.cloneStyleFrom(dataStyle);
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy MM dd"));

            DataFormat format = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));

            int rowIndex = 0;
            int headerCount = hideStaffFee ? 9 : 10;

            SimpleDateFormat sdf = new SimpleDateFormat(
                    sessionController.getApplicationPreference().getLongDateTimeFormat()
            );

            // Credit Company
            Row companyRow = sheet.createRow(rowIndex++);
            Cell companyCell = companyRow.createCell(0);
            companyCell.setCellValue(
                    creditCompany != null ? creditCompany.getName() : "All Credit Companies"
            );
            companyCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, headerCount - 1));

            // Bill Type
            Row billTypeRow = sheet.createRow(rowIndex++);
            Cell billTypeCell = billTypeRow.createCell(0);
            billTypeCell.setCellValue("Bill Type : " + (billType != null ? billType : "All"));
            billTypeCell.setCellStyle(filterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, headerCount - 1));

            // Date Filter
            Row dateRow = sheet.createRow(rowIndex++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue(
                    "From Date : " + (getFromDate() != null ? sdf.format(getFromDate()) : "-")
                    + " | To Date : " + (getToDate() != null ? sdf.format(getToDate()) : "-")
            );
            dateCell.setCellStyle(filterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, headerCount - 1));

            rowIndex++;

            // =========================
            // TITLE
            // =========================
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("OPD Credit Due Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, headerCount - 1));

            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            int headerIndex = 0;
            headerIndex = createHeaderCell(headerRow, headerIndex, "Bill No", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Policy No", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Ref No", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Client Name", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Bill Date", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Billed Amount", headerStyle);
            if (!hideStaffFee) {
                headerIndex = createHeaderCell(headerRow, headerIndex, "Staff Fee", headerStyle);
            }
            headerIndex = createHeaderCell(headerRow, headerIndex, "Refund Amount", headerStyle);
            headerIndex = createHeaderCell(headerRow, headerIndex, "Paid Amount", headerStyle);
            createHeaderCell(headerRow, headerIndex, "Due Amount", headerStyle);

            for (InstitutionBills institutionBills : data) {
                if (institutionBills == null) {
                    continue;
                }

                Row institutionRow = sheet.createRow(rowIndex++);
                Cell institutionCell = institutionRow.createCell(0);
                institutionCell.setCellValue(institutionBills.getInstitution() != null
                        ? institutionBills.getInstitution().getName()
                        : "");
                institutionCell.setCellStyle(sectionHeaderStyle);
                sheet.addMergedRegion(new CellRangeAddress(institutionRow.getRowNum(), institutionRow.getRowNum(), 0, headerCount - 1));

                List<Payment> payments = institutionBills.getPayments();
                if (payments == null) {
                    payments = Collections.emptyList();
                }

                for (Payment payment : payments) {
                    if (payment == null) {
                        continue;
                    }

                    Bill bill = payment.getBill();
                    Row dataRow = sheet.createRow(rowIndex++);
                    int dataIndex = 0;

                    Cell d0 = dataRow.createCell(dataIndex++);
                    d0.setCellValue(bill != null && bill.getDeptId() != null ? bill.getDeptId() : "");
                    d0.setCellStyle(dataStyle);

                    Cell d1 = dataRow.createCell(dataIndex++);
                    d1.setCellValue(payment.getPolicyNo() != null && !payment.getPolicyNo().isEmpty() ? payment.getPolicyNo() : "N/A");
                    d1.setCellStyle(dataStyle);

                    Cell d2 = dataRow.createCell(dataIndex++);
                    d2.setCellValue(payment.getReferenceNo() != null && !payment.getReferenceNo().isEmpty() ? payment.getReferenceNo() : "N/A");
                    d2.setCellStyle(dataStyle);

                    Cell d3 = dataRow.createCell(dataIndex++);
                    d3.setCellValue(bill != null && bill.getPatient() != null && bill.getPatient().getPerson() != null
                            ? bill.getPatient().getPerson().getNameWithTitle()
                            : "");
                    d3.setCellStyle(dataStyle);

                    Cell billDateCell = dataRow.createCell(dataIndex++);
                    if (bill != null && bill.getCreatedAt() != null) {
                        billDateCell.setCellValue(bill.getCreatedAt());
                        billDateCell.setCellStyle(dateStyle);
                    } else {
                        billDateCell.setCellValue("");
                        billDateCell.setCellStyle(dataStyle);
                    }

                    Cell billedAmountCell = dataRow.createCell(dataIndex++);
                    if (bill != null) {
                        billedAmountCell.setCellValue(bill.getNetTotal());
                        billedAmountCell.setCellStyle(numberStyle);
                    } else {
                        billedAmountCell.setCellValue(0d);
                        billedAmountCell.setCellStyle(numberStyle);
                    }

                    if (!hideStaffFee) {
                        Cell staffFeeCell = dataRow.createCell(dataIndex++);
                        double staffFee = bill != null ? bill.getStaffFee() : 0d;
                        staffFeeCell.setCellValue(staffFee);
                        staffFeeCell.setCellStyle(numberStyle);
                    }

                    Cell refundAmountCell = dataRow.createCell(dataIndex++);
                    refundAmountCell.setCellValue(bill != null ? bill.getRefundAmount() : 0d);
                    refundAmountCell.setCellStyle(numberStyle);

                    Cell paidAmountCell = dataRow.createCell(dataIndex++);
                    if (bill != null) {
                        paidAmountCell.setCellValue(bill.getPaidAmount());
                        paidAmountCell.setCellStyle(numberStyle);
                    } else {
                        paidAmountCell.setCellValue(0d);
                        paidAmountCell.setCellStyle(numberStyle);
                    }

                    Cell dueAmountCell = dataRow.createCell(dataIndex);
                    if (bill != null) {
                        dueAmountCell.setCellValue(bill.getNetTotal() - bill.getRefundAmount() - bill.getPaidAmount());
                        dueAmountCell.setCellStyle(numberStyle);
                    } else {
                        dueAmountCell.setCellValue(0d);
                        dueAmountCell.setCellStyle(numberStyle);
                    }
                }

                Row totalRow = sheet.createRow(rowIndex++);
                Cell totalLabelCell = totalRow.createCell(0);
                totalLabelCell.setCellValue("Total");
                totalLabelCell.setCellStyle(headerStyle);

                for (int i = 1; i < 5; i++) {
                    totalRow.createCell(i);
                }

                Cell billedTotalCell = totalRow.createCell(5);
                billedTotalCell.setCellValue(institutionBills.getTotal());
                billedTotalCell.setCellStyle(numberStyle);

                int refundIndex = hideStaffFee ? 6 : 7;
                if (!hideStaffFee) {
                    totalRow.createCell(6);
                }

                Cell refundTotalCell = totalRow.createCell(refundIndex);
                refundTotalCell.setCellValue(institutionBills.getRefundTotal());
                refundTotalCell.setCellStyle(numberStyle);

                int paidIndex = refundIndex + 1;
                Cell paidTotalCell = totalRow.createCell(paidIndex);
                paidTotalCell.setCellValue(institutionBills.getPaidTotal());
                paidTotalCell.setCellStyle(numberStyle);

                Cell dueTotalCell = totalRow.createCell(paidIndex + 1);
                dueTotalCell.setCellValue(institutionBills.getTotal() - institutionBills.getRefundTotal() - institutionBills.getPaidTotal());
                dueTotalCell.setCellStyle(numberStyle);
            }

            if (sessionController != null && sessionController.getLoggedUser() != null
                    && sessionController.getLoggedUser().getWebUserPerson() != null) {
                Row printedByRow = sheet.createRow(rowIndex++);
                printedByRow.createCell(0).setCellValue(
                        "Printed By : " + sessionController.getLoggedUser().getWebUserPerson().getName());
            }

            for (int i = 0; i < headerCount; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=OPD_Credit_Due_Report.xlsx");
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                facesContext.responseComplete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportOpdCreditDueToPDF() {

        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response
                = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=OPD_Credit_Due_Report.pdf");

        boolean hideStaffFee = configOptionApplicationController != null
                && configOptionApplicationController.getBooleanValueByKey("OPD Due Search - Hide Staff Fee", false);

        List<InstitutionBills> data = getItems() == null ? Collections.emptyList() : getItems();

        String datePattern = sessionController.getApplicationPreference().getLongDateTimeFormat();
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

        try (OutputStream out = response.getOutputStream()) {

            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // =========================
            // FONTS
            // =========================
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font filterFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // =========================
            // HEADER SECTION
            // =========================
            Paragraph companyPara = new Paragraph(
                    creditCompany != null ? creditCompany.getName() : "All Credit Companies",
                    titleFont);
            companyPara.setAlignment(Element.ALIGN_CENTER);
            document.add(companyPara);

            Paragraph billTypePara = new Paragraph(
                    "Bill Type : " + (billType != null ? billType : "ALL"),
                    filterFont);
            billTypePara.setAlignment(Element.ALIGN_CENTER);
            document.add(billTypePara);

            Paragraph datePara = new Paragraph(
                    "From : " + (getFromDate() != null ? sdf.format(getFromDate()) : "-")
                    + " | To : " + (getToDate() != null ? sdf.format(getToDate()) : "-"),
                    filterFont);
            datePara.setAlignment(Element.ALIGN_CENTER);
            document.add(datePara);

            Paragraph title = new Paragraph("OPD Credit Due Report", subTitleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            // =========================
            // TABLE
            // =========================
            int columnCount = hideStaffFee ? 9 : 10;

            PdfPTable table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);

            float[] widths = hideStaffFee
                    ? new float[]{8f, 4f, 4f, 6f, 4f, 4f, 4f, 4f, 4f}
                    : new float[]{8f, 4f, 4f, 6f, 4f, 4f, 4f, 4f, 4f, 4f};

            table.setWidths(widths);

            String[] headers = hideStaffFee
                    ? new String[]{"Bill No", "Policy No", "Ref No", "Client Name", "Bill Date", "Billed Amount", "Refund Amount", "Paid Amount", "Due Amount"}
                    : new String[]{"Bill No", "Policy No", "Ref No", "Client Name", "Bill Date", "Billed Amount", "Staff Fee", "Refund Amount", "Paid Amount", "Due Amount"};

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // =========================
            // DATA
            // =========================
            for (InstitutionBills ib : data) {

                if (ib == null) {
                    continue;
                }

                // Institution row
                PdfPCell instCell = new PdfPCell(
                        new Phrase(
                                ib.getInstitution() != null ? ib.getInstitution().getName() : "",
                                headerFont
                        )
                );
                instCell.setColspan(columnCount);
                instCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(instCell);

                List<Payment> payments = ib.getPayments() != null
                        ? ib.getPayments() : Collections.emptyList();

                for (Payment p : payments) {

                    Bill bill = p.getBill();

                    table.addCell(new Phrase(
                            bill != null ? bill.getDeptId() : "", dataFont));

                    table.addCell(new Phrase(
                            p.getPolicyNo() != null ? p.getPolicyNo() : "N/A", dataFont));

                    table.addCell(new Phrase(
                            p.getReferenceNo() != null ? p.getReferenceNo() : "N/A", dataFont));

                    table.addCell(new Phrase(
                            bill != null && bill.getPatient() != null
                            ? bill.getPatient().getPerson().getNameWithTitle()
                            : "", dataFont));

                    PdfPCell bDateCell = new PdfPCell(
                            new Phrase(bill != null && bill.getCreatedAt() != null
                                    ? new SimpleDateFormat("yyyy MM dd").format(bill.getCreatedAt())
                                    : "", dataFont));
                    bDateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(bDateCell);

                    // Billed Amount
                    PdfPCell billedA = new PdfPCell(new Phrase(
                            String.format("%,.2f", bill != null ? bill.getNetTotal() : 0.0),
                            dataFont));
                    billedA.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(billedA);

                    // Staff Fee
                    if (!hideStaffFee) {
                        PdfPCell staff = new PdfPCell(new Phrase(
                                String.format("%,.2f", bill != null ? bill.getStaffFee() : 0.0),
                                dataFont));
                        staff.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(staff);
                    }

                    // Refund
                    PdfPCell refund = new PdfPCell(new Phrase(
                            String.format("%,.2f", bill != null ? bill.getRefundAmount() : 0.0),
                            dataFont));
                    refund.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(refund);

                    // Paid
                    PdfPCell paid = new PdfPCell(new Phrase(
                            String.format("%,.2f", bill != null ? bill.getPaidAmount() : 0.0),
                            dataFont));
                    paid.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(paid);

                    // Due
                    double dueVal = bill != null
                            ? bill.getNetTotal() - bill.getRefundAmount() - bill.getPaidAmount() : 0.0;

                    PdfPCell due = new PdfPCell(new Phrase(
                            String.format("%,.2f", dueVal), dataFont));
                    due.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(due);
                }

                // =========================
                // TOTAL ROW PER INSTITUTION
                // =========================
                PdfPCell totalLabel = new PdfPCell(new Phrase("Total", headerFont));
                totalLabel.setColspan(5);
                totalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(totalLabel);

                PdfPCell totalBilled = new PdfPCell(new Phrase(
                        String.format("%,.2f", ib.getTotal()), headerFont));
                totalBilled.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalBilled);

                if (!hideStaffFee) {
                    table.addCell(new PdfPCell(new Phrase(" ")));
                }

                PdfPCell totalRefund = new PdfPCell(new Phrase(
                        String.format("%,.2f", ib.getRefundTotal()), headerFont));
                totalRefund.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalRefund);

                PdfPCell totalPaid = new PdfPCell(new Phrase(
                        String.format("%,.2f", ib.getPaidTotal()), headerFont));
                totalPaid.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalPaid);

                PdfPCell totalDue = new PdfPCell(new Phrase(
                        String.format("%,.2f", ib.getTotal() - ib.getRefundTotal() - ib.getPaidTotal()),
                        headerFont));
                totalDue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalDue);
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int createHeaderCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
        return columnIndex + 1;
    }

    //    public List<Admission> completePatientDishcargedNotFinalized(String query) {
//        List<Admission> suggestions;
//        String sql;
//        HashMap h = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from Admission c where c.retired=false and "
//                    + " ( c.paymentFinalized is null or c.paymentFinalized=false )"
//                    + " and ( ((c.bhtNo) like :q )or ((c.patient.person.name)"
//                    + " like :q) ) order by c.bhtNo";
//            //////// // System.out.println(sql);
//            //      h.put("btp", BillType.InwardPaymentBill);
//            h.put("q", "%" + query.toUpperCase() + "%");
//            //suggestions = admissionFacade().findByJpql(sql, h);
//        }
//        //return suggestions;
//    }
    public void setItems(List<InstitutionBills> items) {
        this.items = items;
    }

    public Map<Long, Boolean> getPaymentSelectionMap() {
        if (paymentSelectionMap == null) {
            paymentSelectionMap = new HashMap<>();
        }
        return paymentSelectionMap;
    }

    public void setPaymentSelectionMap(Map<Long, Boolean> paymentSelectionMap) {
        this.paymentSelectionMap = paymentSelectionMap;
    }

    public void selectAllPaymentsForInstitution(Institution institution) {
        if (items == null || institution == null) {
            return;
        }
        for (InstitutionBills ib : items) {
            if (ib.getInstitution() != null && institution.getId().equals(ib.getInstitution().getId())) {
                if (ib.getPayments() != null) {
                    for (Payment p : ib.getPayments()) {
                        if (p.getId() != null) {
                            getPaymentSelectionMap().put(p.getId(), Boolean.TRUE);
                        }
                    }
                }
            }
        }
    }

    public String settleSelectedOpdDueBills() {
        if (items == null) {
            JsfUtil.addErrorMessage("No data loaded. Please run the due report first.");
            return null;
        }
        List<Bill> selectedBills = new ArrayList<>();
        Set<Long> selectedBillIds = new HashSet<>();
        Institution firstCompany = null;
        for (InstitutionBills ib : items) {
            if (ib.getPayments() == null) {
                continue;
            }
            for (Payment p : ib.getPayments()) {
                if (Boolean.TRUE.equals(getPaymentSelectionMap().get(p.getId()))) {
                    Bill bill = p.getBill();
                    if (bill == null) {
                        continue;
                    }
                    if (bill.getId() != null && !selectedBillIds.add(bill.getId())) {
                        continue;
                    }
                    if (firstCompany == null) {
                        firstCompany = ib.getInstitution();
                    } else if (!firstCompany.getId().equals(ib.getInstitution().getId())) {
                        JsfUtil.addErrorMessage("Please select bills from one company only.");
                        return null;
                    }
                    selectedBills.add(bill);
                }
            }
        }
        if (selectedBills.isEmpty()) {
            JsfUtil.addErrorMessage("Please select at least one bill to settle.");
            return null;
        }
        return cashRecieveBillController.navigateWithPreloadedBills(selectedBills);
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
    }

    public List<String1Value5> getCreditCompanyAge() {
        return creditCompanyAge;
    }

    public void setCreditCompanyAge(List<String1Value5> creditCompanyAge) {
        this.creditCompanyAge = creditCompanyAge;
    }

    public List<String1Value5> getFilteredList() {
        return filteredList;
    }

    public void setFilteredList(List<String1Value5> filteredList) {
        this.filteredList = filteredList;
    }

    public List<InstitutionEncounters> getInstitutionEncounters() {
        return institutionEncounters;
    }

    public void setInstitutionEncounters(List<InstitutionEncounters> institutionEncounters) {
        this.institutionEncounters = institutionEncounters;
    }

    public Admission getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(Admission patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public AdmissionFacade getAdmissionFacade() {
        return admissionFacade;
    }

    public void setAdmissionFacade(AdmissionFacade admissionFacade) {
        this.admissionFacade = admissionFacade;
    }

    public boolean isWithOutDueUpdate() {
        return withOutDueUpdate;
    }

    public void setWithOutDueUpdate(boolean withOutDueUpdate) {
        this.withOutDueUpdate = withOutDueUpdate;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public double getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(double finalTotal) {
        this.finalTotal = finalTotal;
    }

    public double getFinalPaidTotal() {
        return finalPaidTotal;
    }

    public void setFinalPaidTotal(double finalPaidTotal) {
        this.finalPaidTotal = finalPaidTotal;
    }

    public double getFinalPaidTotalPatient() {
        return finalPaidTotalPatient;
    }

    public void setFinalPaidTotalPatient(double finalPaidTotalPatient) {
        this.finalPaidTotalPatient = finalPaidTotalPatient;
    }

    public double getFinalTransPaidTotal() {
        return finalTransPaidTotal;
    }

    public void setFinalTransPaidTotal(double finalTransPaidTotal) {
        this.finalTransPaidTotal = finalTransPaidTotal;
    }

    public double getFinalTransPaidTotalPatient() {
        return finalTransPaidTotalPatient;
    }

    public void setFinalTransPaidTotalPatient(double finalTransPaidTotalPatient) {
        this.finalTransPaidTotalPatient = finalTransPaidTotalPatient;
    }

    public int getManageInwardDueAndAccessIndex() {
        return manageInwardDueAndAccessIndex;
    }

    public void setManageInwardDueAndAccessIndex(int manageInwardDueAndAccessIndex) {
        this.manageInwardDueAndAccessIndex = manageInwardDueAndAccessIndex;
    }

    public int getManagePharmacyDueAndAccessIndex() {
        return managePharmacyDueAndAccessIndex;
    }

    public void setManagePharmacyDueAndAccessIndex(int managePharmacyDueAndAccessIndex) {
        this.managePharmacyDueAndAccessIndex = managePharmacyDueAndAccessIndex;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    // =========================================================================
    // Inward Patient Co-payment Due Reports
    // =========================================================================

    // -------------------------------------------------------------------------
    // Per-admission credit company figures (#25601)
    //
    // Company receipts and cancels recalculate each commitment bill
    // (INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY, referenceBill = final bill)
    // from the database. finalBill.paidAmount is not used: company receipts add
    // to it, but the next patient payment resets it to patient money only
    // (InwardBeanController.updateFinalFill). Patient money is read from the
    // InwardPaymentBill bills instead, and the encounter's creditPaidAmount
    // (no longer written) must not be read.
    // -------------------------------------------------------------------------

    /** Admission id -> patient money paid (non-Credit InwardPaymentBill bills, signed). */
    private final Map<Long, Double> patientPaidByEncounterId = new HashMap<>();

    /** Final bill id -> {committed, paid by company}; an empty array means the final bill has no commitment bills. */
    private final Map<Long, double[]> ccFiguresByFinalBillId = new HashMap<>();

    /**
     * Loads the commitment bill totals of these admissions' final bills in a
     * few grouped queries, replacing what was loaded before.
     */
    private void loadCcFigures(List<PatientEncounter> encounters) {
        ccFiguresByFinalBillId.clear();
        ccCompanyRawByFinalBillId.clear();
        ccCompanyMergedByFinalBillId.clear();
        patientPaidByEncounterId.clear();
        addCcFigures(encounters);
    }

    /** Same as {@link #loadCcFigures(List)} but keeps what is already loaded (searches that gather admissions in several steps). */
    private void addCcFigures(List<PatientEncounter> encounters) {
        if (encounters == null || encounters.isEmpty()) {
            return;
        }
        List<Long> encounterIds = new ArrayList<>();
        for (PatientEncounter pe : encounters) {
            if (pe != null && pe.getId() != null && !patientPaidByEncounterId.containsKey(pe.getId())
                    && !encounterIds.contains(pe.getId())) {
                encounterIds.add(pe.getId());
            }
        }
        for (int from = 0; from < encounterIds.size(); from += 500) {
            putPatientPaid(encounterIds.subList(from, Math.min(from + 500, encounterIds.size())));
        }
        List<Long> finalBillIds = new ArrayList<>();
        for (PatientEncounter pe : encounters) {
            if (pe != null && pe.getFinalBill() != null && pe.getFinalBill().getId() != null
                    && !ccFiguresByFinalBillId.containsKey(pe.getFinalBill().getId())
                    && !finalBillIds.contains(pe.getFinalBill().getId())) {
                finalBillIds.add(pe.getFinalBill().getId());
            }
        }
        for (int from = 0; from < finalBillIds.size(); from += 500) {
            List<Long> chunk = finalBillIds.subList(from, Math.min(from + 500, finalBillIds.size()));
            for (Long id : chunk) {
                ccFiguresByFinalBillId.put(id, new double[0]);
            }
            putCcFigures(chunk);
        }
    }

    /**
     * Same filter as InwardBeanController.getPaidByPatientValue (used at
     * finalization): non-retired InwardPaymentBill bills, leaving out the
     * Credit method, which old flows used for company money. Cancellation and
     * refund bills carry a negative net total, so the sum is net of them.
     */
    private void putPatientPaid(List<Long> encounterIds) {
        for (Long id : encounterIds) {
            patientPaidByEncounterId.put(id, 0.0);
        }
        String jpql = "SELECT b.patientEncounter.id, SUM(b.netTotal) FROM Bill b"
                + " WHERE b.retired = false"
                + " AND b.billType = :btp"
                + " AND b.paymentMethod <> :pm"
                + " AND b.patientEncounter.id IN :ids"
                + " GROUP BY b.patientEncounter.id";
        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.InwardPaymentBill);
        params.put("pm", PaymentMethod.Credit);
        params.put("ids", new ArrayList<>(encounterIds));
        List<Object[]> rows = billFacade.findAggregates(jpql, params);
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            patientPaidByEncounterId.put(id, row[1] == null ? 0.0 : ((Number) row[1]).doubleValue());
        }
    }

    private void putCcFigures(List<Long> finalBillIds) {
        String jpql = "SELECT c.referenceBill.id, SUM(c.netTotal), SUM(c.settledAmountBySponsor) FROM Bill c"
                + " WHERE c.billTypeAtomic = :bta"
                + " AND c.referenceBill.id IN :ids"
                + " AND c.cancelled = false"
                + " AND c.retired = false"
                + " GROUP BY c.referenceBill.id";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        params.put("ids", new ArrayList<>(finalBillIds));
        List<Object[]> rows = billFacade.findAggregates(jpql, params);
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            double committed = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            double paid = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();
            ccFiguresByFinalBillId.put(id, new double[]{committed, paid});
        }
        putCcCompanyFigures(finalBillIds);
    }

    /**
     * Per-company split of the same commitment bills (same filters as the
     * admission totals above): one grouped query per chunk, grouped by final
     * bill and commitment bill company. A bill with no company is kept under a
     * null company id so the split always adds up to the admission total.
     */
    private void putCcCompanyFigures(List<Long> finalBillIds) {
        String jpql = "SELECT c.referenceBill.id, cc.id, SUM(c.netTotal), SUM(c.settledAmountBySponsor) FROM Bill c"
                + " LEFT JOIN c.creditCompany cc"
                + " WHERE c.billTypeAtomic = :bta"
                + " AND c.referenceBill.id IN :ids"
                + " AND c.cancelled = false"
                + " AND c.retired = false"
                + " GROUP BY c.referenceBill.id, cc.id";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        params.put("ids", new ArrayList<>(finalBillIds));
        List<Object[]> rows = billFacade.findAggregates(jpql, params);
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            Long finalBillId = (Long) row[0];
            Long companyId = row[1] == null ? null : ((Number) row[1]).longValue();
            double committed = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();
            double paid = row[3] == null ? 0.0 : ((Number) row[3]).doubleValue();
            ccCompanyRawByFinalBillId.computeIfAbsent(finalBillId, k -> new LinkedHashMap<>())
                    .put(companyId, new double[]{committed, paid});
        }
    }

    /** Final bill id -> commitment company id (null = bill has none) -> {committed, paid}. */
    private final Map<Long, Map<Long, double[]>> ccCompanyRawByFinalBillId = new HashMap<>();

    /** Final bill id -> per-company figures after the company fallback (cached). */
    private final Map<Long, List<CcCompanyFigures>> ccCompanyMergedByFinalBillId = new HashMap<>();

    private final Map<Long, Institution> institutionsById = new HashMap<>();

    /** One company's share of one admission: committed and paid from its commitment bills. */
    private static class CcCompanyFigures {

        private final Institution company;
        private double committed;
        private double paid;

        private CcCompanyFigures(Institution company, double committed, double paid) {
            this.company = company;
            this.committed = committed;
            this.paid = paid;
        }

        private double due() {
            return Math.max(0.0, committed - paid);
        }
    }

    /** One row of the Inward CC Due Search: an admission and one company's figures. */
    public static class CompanyDueRow {

        private final PatientEncounter encounter;
        private final Institution company;
        private final double committed;
        private final double paid;
        private final boolean firstOfAdmission;

        private CompanyDueRow(PatientEncounter encounter, Institution company, double committed, double paid, boolean firstOfAdmission) {
            this.encounter = encounter;
            this.company = company;
            this.committed = committed;
            this.paid = paid;
            this.firstOfAdmission = firstOfAdmission;
        }

        public PatientEncounter getEncounter() {
            return encounter;
        }

        public Institution getCompany() {
            return company;
        }

        public double getCommitted() {
            return committed;
        }

        public double getPaid() {
            return paid;
        }

        public double getDue() {
            return Math.max(0.0, committed - paid);
        }

        /** Committed less paid, signed (negative when the company has overpaid). */
        public double getBalance() {
            return committed - paid;
        }

        /** Paid less committed, signed (positive when the company has overpaid); the Credit Access (excess) page direction. */
        public double getExcess() {
            return paid - committed;
        }

        /** True on the first row of an admission; admission-level figures (final total, paid by patient) are shown there only, so they are not counted per company. */
        public boolean isFirstOfAdmission() {
            return firstOfAdmission;
        }
    }

    private Institution institutionById(Long id) {
        Institution i = institutionsById.get(id);
        if (i == null) {
            i = institutionFacade.find(id);
            if (i != null) {
                institutionsById.put(id, i);
            }
        }
        return i;
    }

    private static boolean isSameCompany(Institution a, Institution b) {
        Long ai = a == null ? null : a.getId();
        Long bi = b == null ? null : b.getId();
        return ai == null ? bi == null : ai.equals(bi);
    }

    /**
     * The admission's commitment figures split by commitment company. A bill
     * with no company falls back to the admission's own company; if that is
     * missing too it stays under a null company. An admission with no
     * commitment bills (older admissions) gets one row from the legacy
     * figures, so the rows always add up to getCcCommitted / getCcPaid.
     */
    private List<CcCompanyFigures> ccCompanyFigures(PatientEncounter pe) {
        if (pe == null || pe.getFinalBill() == null || pe.getFinalBill().getId() == null) {
            return Collections.emptyList();
        }
        double[] admissionFigures = ccFigures(pe); // loads the admission and its split when not yet loaded
        Long fbId = pe.getFinalBill().getId();
        List<CcCompanyFigures> merged = ccCompanyMergedByFinalBillId.get(fbId);
        if (merged != null) {
            return merged;
        }
        merged = new ArrayList<>();
        Map<Long, double[]> raw = ccCompanyRawByFinalBillId.get(fbId);
        if (admissionFigures == null || raw == null || raw.isEmpty()) {
            double used = pe.getCreditUsedAmount();
            double settled = pe.getFinalBill().getSettledAmountBySponsor();
            if (pe.getCreditCompany() != null || used != 0 || settled != 0) {
                merged.add(new CcCompanyFigures(pe.getCreditCompany(), used, settled));
            }
        } else {
            Map<Long, CcCompanyFigures> byCompany = new LinkedHashMap<>();
            for (Map.Entry<Long, double[]> e : raw.entrySet()) {
                Institution company = e.getKey() == null ? pe.getCreditCompany() : institutionById(e.getKey());
                Long key = company == null ? null : company.getId();
                CcCompanyFigures f = byCompany.get(key);
                if (f == null) {
                    byCompany.put(key, new CcCompanyFigures(company, e.getValue()[0], e.getValue()[1]));
                } else {
                    f.committed += e.getValue()[0];
                    f.paid += e.getValue()[1];
                }
            }
            merged.addAll(byCompany.values());
        }
        ccCompanyMergedByFinalBillId.put(fbId, merged);
        return merged;
    }

    private CcCompanyFigures ccFiguresForCompany(PatientEncounter pe, Institution company) {
        for (CcCompanyFigures f : ccCompanyFigures(pe)) {
            if (isSameCompany(f.company, company)) {
                return f;
            }
        }
        return null;
    }

    /** Amount this company took on for the admission (its commitment bills). */
    public double getCcCommittedForCompany(PatientEncounter pe, Institution company) {
        CcCompanyFigures f = ccFiguresForCompany(pe, company);
        return f == null ? 0.0 : f.committed;
    }

    /** Amount this company has paid for the admission (receipts less cancels on its commitment bills). */
    public double getCcPaidForCompany(PatientEncounter pe, Institution company) {
        CcCompanyFigures f = ccFiguresForCompany(pe, company);
        return f == null ? 0.0 : f.paid;
    }

    /** Amount this company still owes for the admission, never below 0. */
    public double getCcDueForCompany(PatientEncounter pe, Institution company) {
        CcCompanyFigures f = ccFiguresForCompany(pe, company);
        return f == null ? 0.0 : f.due();
    }

    /** Committed less paid for this company and admission, signed: an excess page shows a negative value when the company has overpaid. */
    public double getCcBalanceForCompany(PatientEncounter pe, Institution company) {
        CcCompanyFigures f = ccFiguresForCompany(pe, company);
        return f == null ? 0.0 : f.committed - f.paid;
    }

    /** Placeholder shown for commitment figures that have no company at all. */
    private static Institution noCompanyPlaceholder() {
        Institution i = new Institution();
        i.setName("(no company)");
        return i;
    }

    /**
     * Groups admissions by commitment company, keeping an admission under each
     * company it has figures for. The key is the same object for one company
     * across admissions; null-company figures go under one placeholder.
     * With {@code dueOnly} a company row needs a due above 1 cent.
     */
    private Map<Institution, List<PatientEncounter>> groupByCommitmentCompany(List<PatientEncounter> admissions, boolean dueOnly) {
        Map<Long, Institution> canonical = new HashMap<>();
        Institution placeholder = noCompanyPlaceholder();
        Map<Institution, List<PatientEncounter>> grouped = new LinkedHashMap<>();
        for (PatientEncounter pe : admissions) {
            for (CcCompanyFigures f : ccCompanyFigures(pe)) {
                if (dueOnly && f.due() <= 0.01) {
                    continue;
                }
                Institution key;
                if (f.company == null || f.company.getId() == null) {
                    key = placeholder;
                } else {
                    key = canonical.computeIfAbsent(f.company.getId(), k -> f.company);
                }
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(pe);
            }
        }
        List<Map.Entry<Institution, List<PatientEncounter>>> entries = new ArrayList<>(grouped.entrySet());
        entries.sort((a, b) -> String.valueOf(a.getKey().getName()).compareToIgnoreCase(String.valueOf(b.getKey().getName())));
        Map<Institution, List<PatientEncounter>> sorted = new LinkedHashMap<>();
        for (Map.Entry<Institution, List<PatientEncounter>> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }

    /** Commitment bill totals of the admission's final bill, or null when it has none (older admissions). */
    private double[] ccFigures(PatientEncounter pe) {
        if (pe == null || pe.getFinalBill() == null || pe.getFinalBill().getId() == null) {
            return null;
        }
        Long id = pe.getFinalBill().getId();
        if (!ccFiguresByFinalBillId.containsKey(id)) {
            ccFiguresByFinalBillId.put(id, new double[0]);
            putCcFigures(Collections.singletonList(id));
        }
        double[] figures = ccFiguresByFinalBillId.get(id);
        return figures.length == 0 ? null : figures;
    }

    /** Amount the credit companies took on for this admission. */
    public double getCcCommitted(PatientEncounter pe) {
        double[] f = ccFigures(pe);
        return f == null ? (pe == null ? 0.0 : pe.getCreditUsedAmount()) : f[0];
    }

    /** Amount the credit companies have paid for this admission (receipts less cancels). */
    public double getCcPaid(PatientEncounter pe) {
        double[] f = ccFigures(pe);
        if (f != null) {
            return f[1];
        }
        return pe == null || pe.getFinalBill() == null ? 0.0 : pe.getFinalBill().getSettledAmountBySponsor();
    }

    /** Amount still owed by the credit companies for this admission. */
    public double getCcDue(PatientEncounter pe) {
        return Math.max(0.0, getCcCommitted(pe) - getCcPaid(pe));
    }

    /** Patient money paid for this admission (payments less cancels and refunds), never below 0. */
    public double getPatientPaid(PatientEncounter pe) {
        if (pe == null || pe.getId() == null) {
            return 0.0;
        }
        if (!patientPaidByEncounterId.containsKey(pe.getId())) {
            putPatientPaid(Collections.singletonList(pe.getId()));
        }
        return Math.max(0.0, patientPaidByEncounterId.get(pe.getId()));
    }

    /**
     * Populates {@code patientEncounters} with all finalized inward admissions
     * (with or without CC coverage) where the patient still has an outstanding
     * co-payment balance.
     *
     * Patient due = max(0, finalBill.netTotal - ccCommitted - patientPaid), see
     * {@link #getPatientCopaymentDue(PatientEncounter)}.
     *
     * Also resets {@code billed}, {@code paidByPatient}, {@code paidByCompany}
     * and {@code payableByPatient} to the matching totals.
     */
    public void createInwardPatientCopaymentDueSearch() {
        String dateField = "admissionDate".equals(dateBasis) ? "b.dateOfAdmission" : "b.dateOfDischarge";
        HashMap m = new HashMap();
        String sql = "SELECT b FROM PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " WHERE b.retired = false"
                + " AND b.paymentFinalized = true"
                + " AND " + dateField + " BETWEEN :fd AND :td";
        if (admissionType != null) {
            sql += " AND b.admissionType = :ad";
            m.put("ad", admissionType);
        }
        if (site != null) {
            sql += " AND fb.department.site = :site";
            m.put("site", site);
        }
        if (department != null) {
            sql += " AND fb.department = :dep";
            m.put("dep", department);
        }
        sql += " ORDER BY " + dateField;
        m.put("fd", fromDate);
        m.put("td", toDate);
        List<PatientEncounter> candidates = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        loadCcFigures(candidates);

        patientEncounters = new ArrayList<>();
        billed = 0;
        paidByPatient = 0;
        paidByCompany = 0;
        payableByPatient = 0;
        if (candidates != null) {
            for (PatientEncounter pe : candidates) {
                if (pe.getFinalBill() == null) {
                    continue;
                }
                double patientDue = getPatientCopaymentDue(pe);
                if (patientDue <= 0.01) {
                    continue;
                }
                patientEncounters.add(pe);
                billed += pe.getFinalBill().getNetTotal();
                paidByPatient += getPatientPaid(pe);
                paidByCompany += getCcPaid(pe);
                payableByPatient += patientDue;
            }
        }
    }

    /**
     * Populates {@code creditCompanyAge} with one {@link String1Value5} row per
     * patient where the outstanding co-payment amount is placed in the age bucket
     * that corresponds to days since discharge.
     * Buckets: value1 = 0–30 days, value2 = 31–60, value3 = 61–90, value4 = 91+.
     */
    public void createInwardPatientCopaymentDueAge() {
        createInwardPatientCopaymentDueSearch();
        creditCompanyAge = new ArrayList<>();
        if (patientEncounters == null) {
            return;
        }
        Date today = new Date();
        for (PatientEncounter pe : patientEncounters) {
            if (pe.getFinalBill() == null) {
                continue;
            }
            double due = getPatientCopaymentDue(pe);
            if (due <= 0.01) {
                continue;
            }
            int days = 0;
            if (pe.getDateOfDischarge() != null) {
                days = (int) ((today.getTime() - pe.getDateOfDischarge().getTime()) / (1000L * 60 * 60 * 24));
            }
            String1Value5 row = new String1Value5();
            String patientName = (pe.getPatient() != null && pe.getPatient().getPerson() != null)
                    ? pe.getPatient().getPerson().getNameWithTitle() : "";
            row.setString((pe.getBhtNo() != null ? pe.getBhtNo() : "") + " – " + patientName);
            if (days <= 30) {
                row.setValue1(due);
            } else if (days <= 60) {
                row.setValue2(due);
            } else if (days <= 90) {
                row.setValue3(due);
            } else {
                row.setValue4(due);
            }
            creditCompanyAge.add(row);
        }
    }

    /**
     * Populates {@code creditCompanyAge} with three aggregate summary rows:
     * <ol>
     *   <li>CC Receivable – total CC outstanding by age bucket</li>
     *   <li>Patient Co-payment – total patient outstanding by age bucket</li>
     *   <li>Both Outstanding – total where BOTH CC and patient owe, by age bucket</li>
     * </ol>
     * Designed as a management/CFO view of all inward receivables.
     */
    public void createInwardOutstandingSummary() {
        String dateField = "admissionDate".equals(dateBasis) ? "b.dateOfAdmission" : "b.dateOfDischarge";
        HashMap m = new HashMap();
        String sql = "SELECT b FROM PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " WHERE b.retired = false"
                + " AND b.paymentFinalized = true"
                + " AND " + dateField + " BETWEEN :fd AND :td";
        if (admissionType != null) {
            sql += " AND b.admissionType = :ad";
            m.put("ad", admissionType);
        }
        if (site != null) {
            sql += " AND fb.department.site = :site";
            m.put("site", site);
        }
        if (department != null) {
            sql += " AND fb.department = :dep";
            m.put("dep", department);
        }
        sql += " ORDER BY " + dateField;
        m.put("fd", fromDate);
        m.put("td", toDate);
        List<PatientEncounter> allAdmissions = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        loadCcFigures(allAdmissions);

        String1Value5 ccRow = new String1Value5();
        ccRow.setString("CC Receivable");
        String1Value5 patientRow = new String1Value5();
        patientRow.setString("Patient Co-payment");
        String1Value5 bothRow = new String1Value5();
        bothRow.setString("Both Outstanding");

        Date today = new Date();
        if (allAdmissions != null) {
            for (PatientEncounter pe : allAdmissions) {
                if (pe.getFinalBill() == null) {
                    continue;
                }
                double ccDue = getCcDue(pe);
                double patientDue = getPatientCopaymentDue(pe);
                if (ccDue <= 0.01 && patientDue <= 0.01) {
                    continue;
                }
                int days = 0;
                if (pe.getDateOfDischarge() != null) {
                    days = (int) ((today.getTime() - pe.getDateOfDischarge().getTime()) / (1000L * 60 * 60 * 24));
                }
                if (ccDue > 0.01) {
                    addToAgeBucket(ccRow, ccDue, days);
                }
                if (patientDue > 0.01) {
                    addToAgeBucket(patientRow, patientDue, days);
                }
                if (ccDue > 0.01 && patientDue > 0.01) {
                    addToAgeBucket(bothRow, ccDue + patientDue, days);
                }
            }
        }

        creditCompanyAge = new ArrayList<>();
        creditCompanyAge.add(ccRow);
        creditCompanyAge.add(patientRow);
        creditCompanyAge.add(bothRow);
    }

    /** Adds {@code amount} to the correct age bucket of {@code row}. */
    private void addToAgeBucket(String1Value5 row, double amount, int days) {
        if (days <= 30) {
            row.setValue1(row.getValue1() + amount);
        } else if (days <= 60) {
            row.setValue2(row.getValue2() + amount);
        } else if (days <= 90) {
            row.setValue3(row.getValue3() + amount);
        } else {
            row.setValue4(row.getValue4() + amount);
        }
    }

    /**
     * Returns the CC adjudication status for a patient encounter.
     * PENDING  – CC has committed but paid nothing
     * PARTIAL  – CC has paid some but not all
     * SETTLED  – CC has paid in full (or more)
     * N/A      – no CC coverage on this encounter
     */
    public String getPatientCcStatus(PatientEncounter pe) {
        double ccCommitted = getCcCommitted(pe);
        if (ccCommitted <= 0.01) {
            return "N/A";
        }
        double ccPaid = getCcPaid(pe);
        if (ccPaid >= ccCommitted - 0.01) {
            return "SETTLED";
        }
        if (ccPaid > 0.01) {
            return "PARTIAL";
        }
        return "PENDING";
    }

    /**
     * Returns the outstanding patient co-payment for a given encounter.
     * Formula: max(0, finalBill.netTotal − ccCommitted − patientPaid), where
     * patientPaid is finalBill.paidAmount less the companies' payments (#25601).
     */
    public double getPatientCopaymentDue(PatientEncounter pe) {
        if (pe == null || pe.getFinalBill() == null) {
            return 0.0;
        }
        double due = pe.getFinalBill().getNetTotal() - getCcCommitted(pe) - getPatientPaid(pe);
        // Round to cents so floating-point noise never crosses the 0.01 threshold.
        return Math.max(0.0, Math.round(due * 100.0) / 100.0);
    }

}
