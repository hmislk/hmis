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
import com.divudi.core.data.dataStructure.InstitutionBills;
import com.divudi.core.data.dataStructure.InstitutionEncounters;
import com.divudi.core.data.table.String1Value5;

import com.divudi.core.facade.*;
import com.divudi.ejb.CreditBean;
import com.divudi.core.entity.*;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.util.CommonFunctions;

import java.io.OutputStream;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

    private Date fromDate;
    private Date toDate;
    Admission patientEncounter;
    boolean withOutDueUpdate;
    Institution creditCompany;
    private int manageInwardDueAndAccessIndex;
    private int managePharmacyDueAndAccessIndex;
    ////////////
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
    AdmissionFacade admissionFacade;

    double finalTotal;
    double finalPaidTotal;
    double finalPaidTotalPatient;
    double finalTransPaidTotal;
    double finalTransPaidTotalPatient;

    private Institution institutionOfDepartment;
    private Department department;
    private Institution site;

    private List<Bill> bills = new ArrayList<>();

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

    public Institution getInstitutionOfDepartment() {
        return institutionOfDepartment;
    }

    public void setInstitutionOfDepartment(Institution institutionOfDepartment) {
        this.institutionOfDepartment = institutionOfDepartment;
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

    public void makeNull() {
        fromDate = null;
        toDate = null;
        items = null;
        institutionEncounters = null;
        creditCompanyAge = null;
        filteredList = null;
    }

    public void createAgeTable() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBills(true);

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
        Date startTime = new Date();

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(true, PaymentMethod.Credit);
        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setInstitution(ins);
            setInwardValues(ins, newRow, PaymentMethod.Credit);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    public void createInwardAgeTableWithFilters() {
        Date startTime = new Date();

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(
                true, PaymentMethod.Credit, institutionOfDepartment, department, site);
        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setInstitution(ins);
            setInwardValues(ins, newRow, PaymentMethod.Credit, institutionOfDepartment, department, site);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
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

            XSSFCellStyle boldStyle = workbook.createCellStyle();
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("Credit Company");
            headerRow.createCell(1).setCellValue("0-30 Days");
            headerRow.createCell(2).setCellValue("30-60 Days");
            headerRow.createCell(3).setCellValue("60-90 Days");
            headerRow.createCell(4).setCellValue("90+ Days");

            for (int i = 0; i <= 4; i++) {
                headerRow.getCell(i).setCellStyle(boldStyle);
            }

            for (String1Value5 i : getCreditCompanyAge()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(i.getInstitution().getName());
                row.createCell(1).setCellValue(i.getValue1());
                row.createCell(2).setCellValue(i.getValue2());
                row.createCell(3).setCellValue(i.getValue3());
                row.createCell(4).setCellValue(i.getValue4());

                XSSFCellStyle mergedStyle = workbook.createCellStyle();
                mergedStyle.cloneStyleFrom(amountStyle);
                mergedStyle.setFont(boldFont);

                for (int j = 0; j <= 4; j++) {
                    row.getCell(j).setCellStyle(mergedStyle);
                }

                rowIndex = exportInnerDataTable(sheet, rowIndex, i.getValue1PatientEncounters(), "0-30 Days");
                rowIndex = exportInnerDataTable(sheet, rowIndex, i.getValue2PatientEncounters(), "30-60 Days");
                rowIndex = exportInnerDataTable(sheet, rowIndex, i.getValue3PatientEncounters(), "60-90 Days");
                rowIndex = exportInnerDataTable(sheet, rowIndex, i.getValue4PatientEncounters(), "90+ Days");
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage());
        }
    }

    private int exportInnerDataTable(XSSFSheet sheet, int rowIndex, List<PatientEncounter> encounters, String period) {
        for (PatientEncounter p1 : encounters) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(0).setCellValue(" ");
            dataRow.createCell(1).setCellValue(period);
            dataRow.createCell(2).setCellValue(p1.getBhtNo());
            dataRow.createCell(3).setCellValue(p1.getPatient().getPerson().getName());
            dataRow.createCell(4).setCellValue(p1.getDateOfAdmission().toString());
            dataRow.createCell(5).setCellValue(p1.getDateOfDischarge().toString());
            dataRow.createCell(6).setCellValue(p1.getCreditUsedAmount() + p1.getCreditPaidAmount());

            XSSFCellStyle amountStyle = sheet.getWorkbook().createCellStyle();
            amountStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("#,##0.00"));
            dataRow.getCell(6).setCellStyle(amountStyle);
        }
        return rowIndex;
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
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(true, PaymentMethod.Cash);
        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValues(ins, newRow, PaymentMethod.Cash);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    public void createInwardAgeTableAccess() {
        Date startTime = new Date();

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(false, PaymentMethod.Credit);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValuesAccess(ins, newRow, PaymentMethod.Credit);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }

    }

    public void createInwardAgeTableAccessWithFilters() {
        Date startTime = new Date();

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(
                false, PaymentMethod.Credit, institutionOfDepartment, department, site);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValuesAccessForExcess(ins, newRow, PaymentMethod.Credit);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    public void createInwardCashAgeTableAccess() {
        Date startTime = new Date();

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(false, PaymentMethod.Cash);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValuesAccess(ins, newRow, PaymentMethod.Cash);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    public void createInwardCashAgeTableAccessWithFilters() {
        Date startTime = new Date();

        makeNull();
        Set<Institution> setIns = new HashSet<>();

        List<Institution> list = getCreditBean().getCreditCompanyFromBht(
                false, PaymentMethod.Cash, institutionOfDepartment, department, site);

        setIns.addAll(list);

        creditCompanyAge = new ArrayList<>();
        for (Institution ins : setIns) {
            if (ins == null) {
                continue;
            }

            String1Value5 newRow = new String1Value5();
            newRow.setString(ins.getName());
            setInwardValuesAccess(ins, newRow, PaymentMethod.Cash);

            if (newRow.getValue1() != 0
                    || newRow.getValue2() != 0
                    || newRow.getValue3() != 0
                    || newRow.getValue4() != 0) {
                creditCompanyAge.add(newRow);
            }
        }
    }

    private void setValues(Institution inst, String1Value5 dataTable5Value) {

        List<Bill> lst = getCreditBean().getCreditBills(inst, true);
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

    private void setValuesPharmacy(Institution inst, String1Value5 dataTable5Value) {

        List<Bill> lst = getCreditBean().getCreditBillsPharmacy(inst, true);
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

    private void setInwardValues(Institution inst, String1Value5 dataTable5Value, PaymentMethod paymentMethod) {
        List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounters(inst, true, paymentMethod);
        for (PatientEncounter b : lst) {
            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());
            b.setTransDayCount(dayCount);
            double finalValue = b.getFinalBill().getNetTotal() - (Math.abs(b.getFinalBill().getPaidAmount()) + Math.abs(b.getCreditPaidAmount()));
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

    private void setInwardValuesAccess(Institution inst, String1Value5 dataTable5Value, PaymentMethod paymentMethod) {

        List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounters(inst, false, paymentMethod);
        for (PatientEncounter b : lst) {

            Long dayCount = CommonFunctions.getDayCountTillNow(b.getCreatedAt());

            double finalValue = (Math.abs(b.getCreditUsedAmount()) - Math.abs(b.getCreditPaidAmount()));

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

    public CreditCompanyDueController() {
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

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @EJB
    private CreditBean creditBean;

    public void createOpdCreditDue() {
        Date startTime = new Date();

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

    public void createPharmacyCreditDue() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitutionPharmacy(Arrays.asList(new BillType[]{BillType.PharmacyWholeSale, BillType.PharmacySale}), getFromDate(), getToDate(), true);
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            List<Bill> bills = getCreditBean().getCreditBillsPharmacy(ins, Arrays.asList(new BillType[]{BillType.PharmacyWholeSale, BillType.PharmacySale}), getFromDate(), getToDate(), true);
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

    public void createOpdCreditAccess() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitution(BillType.OpdBill, getFromDate(), getToDate(), false);
        items = new ArrayList<>();
        for (Institution ins : setIns) {
            List<Bill> bills = getCreditBean().getCreditBills(ins, BillType.OpdBill, getFromDate(), getToDate(), false);
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

    public void createInwardCreditDue() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(), PaymentMethod.Credit, true);
        institutionEncounters = new ArrayList<>();
        finalTotal = 0.0;
        finalPaidTotal = 0.0;
        finalPaidTotalPatient = 0.0;
        finalTransPaidTotal = 0.0;
        finalTransPaidTotalPatient = 0.0;
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(), PaymentMethod.Credit, true);

            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);
            for (PatientEncounter b : lst) {
                b.setTransPaidByPatient(createInwardPaymentTotal(b, getFromDate(), getToDate(), BillType.InwardPaymentBill));
                b.setTransPaidByCompany(createInwardPaymentTotalCredit(b, getFromDate(), getToDate(), BillType.CashRecieveBill));
                newIns.setTotal(newIns.getTotal() + b.getFinalBill().getNetTotal());
                newIns.setPaidTotalPatient(newIns.getPaidTotalPatient() + b.getFinalBill().getPaidAmount());
                newIns.setTransPaidTotalPatient(newIns.getTransPaidTotalPatient() + b.getTransPaidByPatient());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getPaidByCreditCompany());
                newIns.setTransPaidTotal(newIns.getTransPaidTotal() + b.getTransPaidByCompany());
            }
            finalTotal += newIns.getTotal();
            finalPaidTotal += newIns.getPaidTotal();
            finalPaidTotalPatient += newIns.getPaidTotalPatient();
            finalTransPaidTotal += newIns.getTransPaidTotal();
            finalTransPaidTotalPatient += newIns.getTransPaidTotalPatient();

            institutionEncounters.add(newIns);
        }

    }

    public void createInwardCreditDueWithAdditionalFilters() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounterWithFinalizedPayments(getFromDate(), getToDate(),
                PaymentMethod.Credit, institutionOfDepartment, department, site);
        institutionEncounters = new ArrayList<>();
        finalTotal = 0.0;
        finalPaidTotal = 0.0;
        finalPaidTotalPatient = 0.0;
        finalTransPaidTotal = 0.0;
        finalTransPaidTotalPatient = 0.0;
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounterWithFinalizedPayments(ins, getFromDate(), getToDate(),
                    PaymentMethod.Credit, institutionOfDepartment, department, site);

            updateSettledAmountsForIP(lst);

            if (withOutDueUpdate) {
                removeSettledAndExcessBills(lst);
            }

            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
//                b.setTransPaidByPatient(createInwardPaymentTotal(b, getFromDate(), getToDate(), BillType.InwardPaymentBill));
//                b.setTransPaidByCompany(createInwardPaymentTotalCredit(b, getFromDate(), getToDate(), BillType.CashRecieveBill));

                newIns.setTotal(newIns.getTotal() + b.getFinalBill().getNetTotal());
                newIns.setPaidTotalPatient(newIns.getPaidTotalPatient() + b.getFinalBill().getSettledAmountByPatient());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getFinalBill().getSettledAmountBySponsor());
            }

            finalTotal += newIns.getTotal();
            finalPaidTotal += newIns.getPaidTotal();
            finalPaidTotalPatient += newIns.getPaidTotalPatient();

            if (newIns.getPatientEncounters().isEmpty()) {
                continue;
            }

            institutionEncounters.add(newIns);
        }
    }

    public void exportDueSearchCreditCompanyToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Due_Search_Credit_Company.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Institution Encounters Report");
            int rowIndex = 0;

            XSSFCellStyle boldStyle = workbook.createCellStyle();
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            XSSFCellStyle mergedStyle = workbook.createCellStyle();
            mergedStyle.cloneStyleFrom(amountStyle);
            mergedStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {"BHT No", "Date Of Discharge", "Patient Name", "Billed Amount", "Paid By Patient", "Paid By Company", "Net Amount"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            for (InstitutionEncounters i : getInstitutionEncounters()) {
                Row institutionRow = sheet.createRow(rowIndex++);
                Cell institutionCell = institutionRow.createCell(0);
                institutionCell.setCellValue(i.getInstitution().getName());
                institutionCell.setCellStyle(boldStyle);

                for (PatientEncounter b : i.getPatientEncounters()) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(b.getBhtNo());
                    dataRow.createCell(1).setCellValue(b.getDateOfDischarge().toString());
                    dataRow.createCell(2).setCellValue(b.getPatient().getPerson().getNameWithTitle());
                    dataRow.createCell(3).setCellValue(b.getFinalBill().getNetTotal());
                    dataRow.createCell(4).setCellValue(b.getFinalBill().getSettledAmountByPatient());
                    dataRow.createCell(5).setCellValue(b.getFinalBill().getSettledAmountBySponsor());
                    dataRow.createCell(6).setCellValue(b.getFinalBill().getNetTotal() - (b.getFinalBill().getSettledAmountByPatient() + b.getFinalBill().getSettledAmountBySponsor()));

                    for (int j = 3; j <= 6; j++) {
                        dataRow.getCell(j).setCellStyle(amountStyle);
                    }
                }
                Row institutionFooterRow = sheet.createRow(rowIndex++);
                institutionFooterRow.createCell(3).setCellValue(i.getTotal());
                institutionFooterRow.createCell(4).setCellValue(i.getPaidTotalPatient());
                institutionFooterRow.createCell(5).setCellValue(i.getPaidTotal());
                institutionFooterRow.createCell(6).setCellValue(i.getTotal() - (i.getPaidTotal() + i.getPaidTotalPatient()));

                for (int j = 3; j <= 6; j++) {
                    institutionFooterRow.getCell(j).setCellStyle(mergedStyle);
                }
            }

            Row footerRow = sheet.createRow(rowIndex++);
            footerRow.createCell(3).setCellValue(finalTotal);
            footerRow.createCell(4).setCellValue(finalPaidTotalPatient);
            footerRow.createCell(5).setCellValue(finalPaidTotal);
            footerRow.createCell(6).setCellValue(finalTotal - (finalPaidTotal + finalPaidTotalPatient));

            for (int j = 3; j <= 6; j++) {
                footerRow.getCell(j).setCellStyle(mergedStyle);
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage());
        }
    }

    public void exportDueSearchCreditCompanyToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Due_Search_Credit_Company.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Institution Encounters Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            float[] columnWidths = {1.5f, 2.5f, 3.0f, 2.5f, 2.5f, 2.5f, 2.5f};
            table.setWidths(columnWidths);

            String[] headers = {"BHT No", "Date Of Discharge", "Patient Name", "Billed Amount", "Paid By Patient", "Paid By Company", "Net Amount"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

            for (InstitutionEncounters i : getInstitutionEncounters()) {
                PdfPCell institutionCell = new PdfPCell(new Phrase(i.getInstitution().getName(), boldFont));
                institutionCell.setColspan(7);
                table.addCell(institutionCell);

                for (PatientEncounter b : i.getPatientEncounters()) {
                    table.addCell(new PdfPCell(new Phrase(b.getBhtNo(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(dateFormatter.format(b.getDateOfDischarge()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(b.getPatient().getPerson().getNameWithTitle(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getFinalBill().getNetTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getFinalBill().getSettledAmountByPatient()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getFinalBill().getSettledAmountBySponsor()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(
                            b.getFinalBill().getNetTotal() - (b.getFinalBill().getSettledAmountByPatient() + b.getFinalBill().getSettledAmountBySponsor())), normalFont)));
                }

                PdfPCell subtotalLabel = new PdfPCell(new Phrase("Sub Total", boldFont));
                subtotalLabel.setColspan(3);
                table.addCell(subtotalLabel);
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(i.getTotal()), boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(i.getPaidTotalPatient()), boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(i.getPaidTotal()), boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(i.getTotal() - (i.getPaidTotal() + i.getPaidTotalPatient())), boldFont)));
            }

            PdfPCell netTotalLabel = new PdfPCell(new Phrase("Net Total", boldFont));
            netTotalLabel.setColspan(3);
            table.addCell(netTotalLabel);
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(finalTotal), boldFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(finalPaidTotalPatient), boldFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(finalPaidTotal), boldFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(finalTotal - (finalPaidTotal + finalPaidTotalPatient)), boldFont)));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(CreditCompanyDueController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage());
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

        HashMap m = new HashMap();
        String sql = " Select b from PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " where b.retired=false "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td "
                + " and (abs(b.finalBill.netTotal)-(abs(b.finalBill.paidAmount)+abs(b.creditPaidAmount))) > 0.1 ";

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
        billed = 0;
        paidByPatient = 0;
        paidByCompany = 0;
        for (PatientEncounter p : patientEncounters) {
            billed += p.getFinalBill().getNetTotal();
            paidByPatient += p.getFinalBill().getPaidAmount();
            paidByCompany += p.getPaidByCreditCompany();

        }
    }

    public void createInwardCashDueData() {
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
        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
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

        updateSettledAmountsForIP(patientEncounters);
        removeSettledAndExcessBills(patientEncounters);

        billed = 0;
        paidByPatient = 0;
        paidByCompany = 0;
        for (PatientEncounter p : patientEncounters) {
            billed += p.getFinalBill().getNetTotal();
            paidByPatient += p.getFinalBill().getSettledAmountByPatient();
            paidByCompany += p.getFinalBill().getSettledAmountBySponsor();
        }
    }

    private void removeSettledAndExcessBills(List<PatientEncounter> patientEncounters) {
        patientEncounters.removeIf(p -> p.getFinalBill().getNetTotal() - (Math.abs(p.getFinalBill().getPaidAmount()) + Math.abs(p.getCreditPaidAmount())) <= 0);
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

        bts.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
        bts.add(BillTypeAtomic.INWARD_DEPOSIT);
        bts.add(BillTypeAtomic.INWARD_DEPOSIT_REFUND);

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

    public void createInwardCreditAccess() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(), PaymentMethod.Credit, false);

        institutionEncounters = new ArrayList<>();
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(), PaymentMethod.Credit, false);
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
//                newIns.setTotal(newIns.getTotal() + b.getCreditUsedAmount());
//                newIns.setPaidTotal(newIns.getPaidTotal() + b.getCreditPaidAmount());
                b.getFinalBill().setNetTotal(com.divudi.core.util.CommonFunctions.round(b.getFinalBill().getNetTotal()));
                b.setCreditPaidAmount(Math.abs(b.getCreditPaidAmount()));
                b.setCreditPaidAmount(com.divudi.core.util.CommonFunctions.round(b.getCreditPaidAmount()));
                b.getFinalBill().setPaidAmount(com.divudi.core.util.CommonFunctions.round(b.getFinalBill().getPaidAmount()));
                b.setTransPaid(b.getFinalBill().getPaidAmount() + b.getCreditPaidAmount());
                //// // System.out.println("b.getTransPaid() = " + b.getTransPaid());
                b.setTransPaid(com.divudi.core.util.CommonFunctions.round(b.getTransPaid()));

                newIns.setTotal(newIns.getTotal() + b.getFinalBill().getNetTotal());
//                newIns.setPaidTotal(newIns.getPaidTotal() + (Math.abs(b.getCreditPaidAmount()) + Math.abs(b.getFinalBill().getPaidAmount())));
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getTransPaid());

            }
            newIns.setTotal(com.divudi.core.util.CommonFunctions.round(newIns.getTotal()));
            newIns.setPaidTotal(com.divudi.core.util.CommonFunctions.round(newIns.getPaidTotal()));
            institutionEncounters.add(newIns);
        }

    }

    public void createInwardCreditAccessWithFilters() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(),
                PaymentMethod.Credit, false, institutionOfDepartment, department, site);

        institutionEncounters = new ArrayList<>();
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(),
                    PaymentMethod.Credit, false, institutionOfDepartment, department, site);
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
//                newIns.setTotal(newIns.getTotal() + b.getCreditUsedAmount());
//                newIns.setPaidTotal(newIns.getPaidTotal() + b.getCreditPaidAmount());
                b.getFinalBill().setNetTotal(com.divudi.core.util.CommonFunctions.round(b.getFinalBill().getNetTotal()));
                b.setCreditPaidAmount(Math.abs(b.getCreditPaidAmount()));
                b.setCreditPaidAmount(com.divudi.core.util.CommonFunctions.round(b.getCreditPaidAmount()));
                b.getFinalBill().setPaidAmount(com.divudi.core.util.CommonFunctions.round(b.getFinalBill().getPaidAmount()));
                b.setTransPaid(b.getFinalBill().getPaidAmount() + b.getCreditPaidAmount());
                //// // System.out.println("b.getTransPaid() = " + b.getTransPaid());
                b.setTransPaid(com.divudi.core.util.CommonFunctions.round(b.getTransPaid()));

                newIns.setTotal(newIns.getTotal() + b.getFinalBill().getNetTotal());
//                newIns.setPaidTotal(newIns.getPaidTotal() + (Math.abs(b.getCreditPaidAmount()) + Math.abs(b.getFinalBill().getPaidAmount())));
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getTransPaid());

            }
            newIns.setTotal(com.divudi.core.util.CommonFunctions.round(newIns.getTotal()));
            newIns.setPaidTotal(com.divudi.core.util.CommonFunctions.round(newIns.getPaidTotal()));
            institutionEncounters.add(newIns);
        }
    }

    public void createInwardCashAccess() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(), PaymentMethod.Cash, false);

        institutionEncounters = new ArrayList<>();
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(), PaymentMethod.Cash, false);
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
                newIns.setTotal(newIns.getTotal() + b.getCreditUsedAmount());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getCreditPaidAmount());
            }

            institutionEncounters.add(newIns);
        }

    }

    public void createInwardCashAccessWithFilters() {
        Date startTime = new Date();

        List<Institution> setIns = getCreditBean().getCreditInstitutionByPatientEncounter(getFromDate(), getToDate(),
                PaymentMethod.Cash, false, institutionOfDepartment, department, site);

        institutionEncounters = new ArrayList<>();
        for (Institution ins : setIns) {
            List<PatientEncounter> lst = getCreditBean().getCreditPatientEncounter(ins, getFromDate(), getToDate(),
                    PaymentMethod.Cash, false, institutionOfDepartment, department, site);
            InstitutionEncounters newIns = new InstitutionEncounters();
            newIns.setInstitution(ins);
            newIns.setPatientEncounters(lst);

            for (PatientEncounter b : lst) {
                newIns.setTotal(newIns.getTotal() + b.getCreditUsedAmount());
                newIns.setPaidTotal(newIns.getPaidTotal() + b.getCreditPaidAmount());
            }

            institutionEncounters.add(newIns);
        }
    }

    public void createInwardPaymentBills() {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  "
                + " and (b.forwardReferenceBill.netTotal - b.forwardReferenceBill.paidAmount) < 0";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        if (institutionOfDepartment != null) {
            temMap.put("ins", institutionOfDepartment);
            sql += " and b.department.institution = :ins ";
        }

        sql += " order by b.deptId desc  ";

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

        finalPaidTotal = 0;
        finalTotal = 0;
        finalTransPaidTotal = 0;

        for (Bill bill : bills) {
            finalTransPaidTotal += bill.getForwardReferenceBill().getNetTotal();
            finalPaidTotal += bill.getForwardReferenceBill().getPaidAmount();
            finalTotal += bill.getForwardReferenceBill().getNetTotal() - bill.getForwardReferenceBill().getPaidAmount();
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

        try (Workbook workbook = new XSSFWorkbook()) {
            // Create a sheet
            Sheet sheet = workbook.createSheet("Due Report");
            int rowIndex = 0;

            // Create Header Row
            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {"Institution Name", "Bill No", "Client Name", "Bill Date", "Billed Amount", "Staff Fee", "Paid Amount", "Net Amount"};
            int colIndex = 0;


            for (String header : headers) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
            }

            // Populate Data Rows
            for (InstitutionBills institution : items) {
                for (Bill bill : institution.getBills()) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    colIndex = 0;

                    dataRow.createCell(colIndex++).setCellValue(institution.getInstitution().getName());
                    dataRow.createCell(colIndex++).setCellValue(bill.getDeptId());
                    dataRow.createCell(colIndex++).setCellValue(bill.getPatient().getPerson().getNameWithTitle());
                    dataRow.createCell(colIndex++).setCellValue(bill.getCreatedAt().toString());
                    dataRow.createCell(colIndex++).setCellValue(bill.getNetTotal());
                    dataRow.createCell(colIndex++).setCellValue(bill.getStaffFee());
                    dataRow.createCell(colIndex++).setCellValue(bill.getPaidAmount());
                    dataRow.createCell(colIndex++).setCellValue(bill.getNetTotal() + bill.getPaidAmount());
                }
            }

            // Auto-size Columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Set Response Headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=Credit Company Due_Report.xlsx");
            OutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);

            // Complete Response
            facesContext.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

}
