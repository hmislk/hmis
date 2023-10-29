package com.divudi.bean.report;

import com.divudi.bean.common.InstitutionController;
import com.divudi.data.BillType;
import com.divudi.data.CategoryCount;
import com.divudi.data.ItemCount;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
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

    private List<Bill> bills;
    private List<ItemCount> reportLabTestCounts;
    private List<CategoryCount> reportList;

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

        return "/reports/asset_register";
    }

    public String navigateToLabReportsTestCount() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/test_count";
    }

    public String navigateToPoStatusReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }

        return "/reports/po_status_report";
    }

    public String navigateToEmployeeAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/employee_asset_issue";
    }

    public String navigateToFixedAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/fixed_asset_issue";
    }

    public String navigateToAssetWarentyExpireReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/asset_warranty_expire_report";
    }

    public String navigateToAssetGrnReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/asset_grn_report";
    }

    public String navigateToAssetTransferReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assest_transfer_report";

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

}
