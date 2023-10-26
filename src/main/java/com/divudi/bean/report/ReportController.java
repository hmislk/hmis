package com.divudi.bean.report;

import com.divudi.bean.common.InstitutionController;
import com.divudi.data.ReportLabTestCount;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.lab.Machine;
import com.divudi.facade.BillItemFacade;
import com.divudi.java.CommonFunctions;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;

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

    private List<Bill> bills;
    private List<ReportLabTestCount> reportLabTestCounts;

    public ReportController() {
    }

    public void processLabTestCount() {
        String jpql = "select new com.divudi.data.ReportLabTestCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td ";

        Map m = new HashMap();
        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        jpql += " group by bi.item ";
        jpql += " order by bi.item.category.name, bi.item.name";

        //String category, String testName, Long testCount
//        BillItem bi = new BillItem();
//        bi.getItem().getCategory().getName();
//        bi.getBill().getBillDate();
        System.out.println("jpql = " + jpql);
        System.out.println("m = " + m);

        reportLabTestCounts = billItemFacade.findLightsByJpql(jpql, m);
        System.out.println("reportLabTestCounts = " + reportLabTestCounts.size());
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
    
    public String navigateToLabPeakHourStatistics() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/peak_hour_statistics";
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

    public List<ReportLabTestCount> getReportLabTestCounts() {
        return reportLabTestCounts;
    }

    public void setReportLabTestCounts(List<ReportLabTestCount> reportLabTestCounts) {
        this.reportLabTestCounts = reportLabTestCounts;
    }

    public String getCcRoute() {
        return ccRoute;
    }

    public void setCcRoute(String ccRoute) {
        this.ccRoute = ccRoute;
    }

    public String getCcName() {
        return ccName;
    }

    public void setCcName(String ccName) {
        this.ccName = ccName;
    }

    public String getProcessBy() {
        return processBy;
    }

    public void setProcessBy(String processBy) {
        this.processBy = processBy;
    }

}
