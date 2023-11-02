package com.divudi.bean.report;

import com.divudi.bean.common.InstitutionController;
import com.divudi.data.BillType;
import com.divudi.data.CategoryCount;
import com.divudi.data.ItemCount;
import com.divudi.data.Sex;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Speciality;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.Machine;
import com.divudi.facade.BillItemFacade;
import com.divudi.java.CommonFunctions;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@SessionScoped
public class ReportController implements Serializable {

    @EJB
    BillItemFacade billItemFacade;

    @Inject
    private InstitutionController institutionController;

    private int reportIndex;
    private Institution institution;
    private Department department;
    private Institution fromInstitution;
    private Institution toInstitution;
    private Department fromDepartment;
    private Department toDepartment;
    private Date fromDate;
    private Date toDate;
    private Category category;
    private Item item;
    private Machine machine;
    private String processBy;
    private String ccName;
    private String ccRoute;
    private Date financialYear;

    private double investigationResult;

    private String visitType;
    private Patient patient;
    private String diagnosis;
    private Doctor referingDoctor;

    private Investigation investigation;
    private Speciality currentSpeciality;

    private String priorityType;
    private String patientMrn;

    private String status;
    private String refDocName;
    private String totalAverage;
    private String visit;

    private List<Bill> bills;
    private List<ItemCount> reportLabTestCounts;
    private List<CategoryCount> reportList;
    
    private Date warrentyStartDate;
    private Date warrentyEndDate;
    
    private Date amcStartDate;
    private Date amcEndDate;

    public ReportController() {
    }

    // Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI.
    public void processLabTestCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and TYPE(bi.item) = Investigation ";
        Map<String, Object> m = new HashMap<>();
        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (toInstitution != null) {
            jpql += " and bi.bill.toInstitution=:ti ";
            m.put("ti", toInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (machine != null) {
            jpql += " and bi.item.machine=:machine ";
            m.put("machine", machine);
        }

        jpql += " group by bi.item.category.name, bi.item.name ";
        jpql += " order by bi.item.category.name, bi.item.name";

        // Unchecked cast here
        reportLabTestCounts = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);

        Map<String, CategoryCount> categoryReports = new HashMap<>();

        for (ItemCount count : reportLabTestCounts) {
            categoryReports.computeIfAbsent(count.getCategory(), k -> new CategoryCount(k, new ArrayList<>(), 0L))
                    .getItems().add(count);
            categoryReports.get(count.getCategory()).setTotal(categoryReports.get(count.getCategory()).getTotal() + count.getTestCount());
        }

        // Convert the map values to a list to be used in the JSF page
        reportList = new ArrayList<>(categoryReports.values());
    }

    public void processPharmacySaleItemCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType=:bitype ";
        Map<String, Object> m = new HashMap<>();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        jpql += " group by bi.item.category.name, bi.item.name ";
        jpql += " order by bi.item.category.name, bi.item.name";

        // Unchecked cast here
        reportLabTestCounts = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);

        Map<String, CategoryCount> categoryReports = new HashMap<>();

        for (ItemCount count : reportLabTestCounts) {
            categoryReports.computeIfAbsent(count.getCategory(), k -> new CategoryCount(k, new ArrayList<>(), 0L))
                    .getItems().add(count);
            categoryReports.get(count.getCategory()).setTotal(categoryReports.get(count.getCategory()).getTotal() + count.getTestCount());
        }

        // Convert the map values to a list to be used in the JSF page
        reportList = new ArrayList<>(categoryReports.values());
    }

    public void processOpdServiceCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType=:bitype ";
        Map<String, Object> m = new HashMap<>();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.OpdBill);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (toInstitution != null) {
            jpql += " and bi.bill.toInstitution=:ti ";
            m.put("ti", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " and bi.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        jpql += " group by bi.item.category.name, bi.item.name ";
        jpql += " order by bi.item.category.name, bi.item.name";

        // Unchecked cast here
        reportLabTestCounts = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);

        Map<String, CategoryCount> categoryReports = new HashMap<>();

        for (ItemCount count : reportLabTestCounts) {
            categoryReports.computeIfAbsent(count.getCategory(), k -> new CategoryCount(k, new ArrayList<>(), 0L))
                    .getItems().add(count);
            categoryReports.get(count.getCategory()).setTotal(categoryReports.get(count.getCategory()).getTotal() + count.getTestCount());
        }

        // Convert the map values to a list to be used in the JSF page
        reportList = new ArrayList<>(categoryReports.values());
    }

    public void downloadLabTestCount() {
        Workbook workbook = exportToExcel(reportList, "Test Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=test_counts.xlsx");

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadPharmacySaleItemCount() {
        Workbook workbook = exportToExcel(reportList, "Sale Item Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=Sale_Item_Count.xlsx");

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadOpdServiceCount() {
        Workbook workbook = exportToExcel(reportList, "Opd Service Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=service_count.xlsx");

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Workbook exportToExcel(List<CategoryCount> reportList, String reportName) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(reportName);
        int rowCount = 0;
        int cateColNo = 0;
        int itemColNo = 1;
        int catCountColNo = 2;
        int itemCountColNo = 3;
        Long grandTotal = 0l;

        Row reportHeaderRow = sheet.createRow(rowCount++);
        Cell headerRowCatCell = reportHeaderRow.createCell(cateColNo);
        headerRowCatCell.setCellValue("Category");
        Cell headerRowItemCell = reportHeaderRow.createCell(itemColNo);
        headerRowItemCell.setCellValue("Item");
        Cell headerRowCatCountCell = reportHeaderRow.createCell(catCountColNo);
        headerRowCatCountCell.setCellValue("Category Count");
        Cell headerRowItemCountCell = reportHeaderRow.createCell(itemCountColNo);
        headerRowItemCountCell.setCellValue("Item Count");

        for (CategoryCount catCount : reportList) {
            Row headerRow = sheet.createRow(rowCount++);
            Cell headerCell1 = headerRow.createCell(cateColNo);
            headerCell1.setCellValue(catCount.getCategory());
            Cell headerCell2 = headerRow.createCell(catCountColNo);
            headerCell2.setCellValue(catCount.getTotal());
            grandTotal += catCount.getTotal();

            for (ItemCount itemCount : catCount.getItems()) {
                Row itemRow = sheet.createRow(rowCount++);
                Cell cell1 = itemRow.createCell(itemColNo);
                cell1.setCellValue(itemCount.getTestName());
                Cell cell2 = itemRow.createCell(itemCountColNo);
                cell2.setCellValue(itemCount.getTestCount());
            }
        }

        Row reportFooterRow = sheet.createRow(rowCount++);
        Cell reportFooterCellTotal = reportFooterRow.createCell(cateColNo);
        reportFooterCellTotal.setCellValue("Grand Total");
        Cell reportFooterRowItemCell = reportFooterRow.createCell(itemCountColNo);
        reportFooterRowItemCell.setCellValue(grandTotal);

        return workbook;
    }

    public String navigateToAssetRegister() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }

        return "/reports/assets/asset_register";
    }

    public String navigateToLabReportsTestCount() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/test_count";
    }

    public String navigateToLabPeakHourStatistics() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/peak_hour_statistics";
    }

    public String navigateToLabInvetigationWiseReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/investigation_wise_report";
    }

    public String navigateToExternalLaborataryWorkloadReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/external_laboratary_workload";
    }

    public String navigateToLabOrganismAntibioticSensitivityReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/organism_antibiotic_sensitivity";
    }

    public String navigateToLabRegisterReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/lab_register";
    }

    public String navigateToTurnAroundTimeDetails() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/turn_around_time_details";
    }

    public String navigateToAnnualTestStatistics() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/annual_test_statistics";
    }

    public String navigateToPoStatusReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }

        return "/reports/assets/po_status_report";
    }

    public String navigateToEmployeeAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/employee_asset_issue";
    }

    public String navigateToFixedAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/fixed_asset_issue";
    }

    public String navigateToAssetWarentyExpireReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/asset_warranty_expire_report";
    }

    public String navigateToAssetGrnReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/asset_grn_report";
    }

    public String navigateToAssetTransferReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/assest_transfer_report";

    }
    
    public String navigateToAssetAmcExpiryReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/assest_amc_expiry_report";

    }

    public String navigateToAssetAmcReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/amc_report";

    }
    public String navigateToTurnAroundTimeHourly() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/turn_around_time_hourly";

    }

    public String navigateToCollectionCenterStatement() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/collection_center_statement";

    }

    public String navigateToManagementAdmissionCountReport() {

        return "/reports/managementReports/admission_count(consultant_wise)";
    }

    public String navigateToSurgeryWiseCount() {

        return "/reports/managementReports/surgery_wise_count";
    }
    
    public String navigateToSurgeryCountDoctorWise() {

        return "/reports/managementReports/surgery_count_doctor_wise";
    }
    
    public String navigateToLeaveReport() {

        return "/reports/HRReports/leave_report";
    }
    
    public String navigateToLeaveReportSummery() {

        return "/reports/HRReports/leave_report_summery";
    }
    
     public String navigateToLateLeaveDetails() {

        return "/reports/HRReports/late_leave_details";
    }
     
      public String navigateToLeaveSummeryReport() {

        return "/reports/HRReports/leave_summery_report";
    }

    public String navigateToDepartmentReports() {

        return "/reports/HRReports/department_report";
    } 
    
    public String navigateToEmployeeDetails() {

        return "/reports/HRReports/employee_details";
    }
    
    public String navigateToEmployeeToRetired() {

        return "/reports/HRReports/employee_to_retired";
    }
    
    public String navigateToStaffShiftReport() {

        return "/reports/HRReports/staff_shift_report";
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public int getReportIndex() {
        return reportIndex;
    }

    public void setReportIndex(int reportIndex) {
        this.reportIndex = reportIndex;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }
    
    

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<ItemCount> getReportLabTestCounts() {
        return reportLabTestCounts;
    }

    public void setReportLabTestCounts(List<ItemCount> reportLabTestCounts) {
        this.reportLabTestCounts = reportLabTestCounts;
    }

    public List<CategoryCount> getReportList() {
        return reportList;
    }

    public void setReportList(List<CategoryCount> reportList) {
        this.reportList = reportList;
    }

    public String getProcessBy() {
        return processBy;
    }

    public void setProcessBy(String processBy) {
        this.processBy = processBy;
    }

    public String getCcName() {
        return ccName;
    }

    public void setCcName(String ccName) {
        this.ccName = ccName;
    }

    public String getCcRoute() {
        return ccRoute;
    }

    public void setCcRoute(String ccRoute) {
        this.ccRoute = ccRoute;
    }

    public double getInvestigationResult() {
        return investigationResult;
    }

    public void setInvestigationResult(double investigationResult) {
        this.investigationResult = investigationResult;
    }

    public String getVisitType() {
        return visitType;
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Doctor getReferingDoctor() {
        return referingDoctor;
    }

    public void setReferingDoctor(Doctor referingDoctor) {
        this.referingDoctor = referingDoctor;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public String getPriorityType() {
        return priorityType;
    }

    public void setPriorityType(String priorityType) {
        this.priorityType = priorityType;
    }

    public String getPatientMrn() {
        return patientMrn;
    }

    public void setPatientMrn(String patientMrn) {
        this.patientMrn = patientMrn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTotalAverage() {
        return totalAverage;
    }

    public void setTotalAverage(String totalAverage) {
        this.totalAverage = totalAverage;
    }

    public String getVisit() {
        return visit;
    }

    public void setVisit(String visit) {
        this.visit = visit;
    }

    public String getRefDocName() {
        return refDocName;
    }

    public void setRefDocName(String refDocName) {
        this.refDocName = refDocName;
    }

    public Date getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(Date financialYear) {
        this.financialYear = financialYear;
    }

    public Speciality getCurrentSpeciality() {
        return currentSpeciality;
    }

    public void setCurrentSpeciality(Speciality currentSpeciality) {
        this.currentSpeciality = currentSpeciality;
    }

    public Date getWarrentyStartDate() {
        if (warrentyStartDate == null) {
            warrentyStartDate = CommonFunctions.getEndOfDay();
        }
        return warrentyStartDate;
    }

    public void setWarrentyStartDate(Date warrentyStartDate) {
        this.warrentyStartDate = warrentyStartDate;
    }

    public Date getWarrentyEndDate() {
        if (warrentyEndDate == null) {
            warrentyEndDate = CommonFunctions.getEndOfMonth(toDate);
        }
      
        return warrentyEndDate;
    }

    public void setWarrentyEndDate(Date warrentyEndDate) {
        this.warrentyEndDate = warrentyEndDate;
    }

    public Date getAmcStartDate() {
        if (amcStartDate == null) {
            amcStartDate = CommonFunctions.getEndOfDay();
        }
        return amcStartDate;
    }

    public void setAmcStartDate(Date amcStartDate) {
        this.amcStartDate = amcStartDate;
    }

    public Date getAmcEndDate() {
        if (amcEndDate == null) {
            amcEndDate = CommonFunctions.getEndOfMonth(toDate);
        }
      
        return amcEndDate;
    }

    public void setAmcEndDate(Date amcEndDate) {
        this.amcEndDate = amcEndDate;
    }

}
