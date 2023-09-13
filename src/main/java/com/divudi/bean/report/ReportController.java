package com.divudi.bean.report;

import com.divudi.bean.common.InstitutionController;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

/**
 *
 * @author Senula Nanayakkara
 */
@Named(value = "reportController")
@SessionScoped
public class ReportController implements Serializable {

    @Inject
    InstitutionController institutionController;

    private int reportIndex;
    private Institution institution;
    private Department department;
    private Department fromDepartment;
    private Department toDepartment;
    private Date fromDate;
    private Date toDate;
    private Category category;
    private Item item;

    private List<Bill> bills;

    public ReportController() {
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
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
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

    public String navigateToAssetRegister() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
  
        return "/reports/asset_register";
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
    
    public String navigateToAssetWarentyExpireReport(){
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/asset_warranty_expire_report";
    }
      
    public String navigateToAssetGrnReport(){
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/asset_grn_report";
    }
    
    public String navigateToAssetTransferReport(){
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

}
