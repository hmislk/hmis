/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.StaffDesignation;
import com.divudi.entity.hr.StaffEmployeeStatus;
import com.divudi.entity.hr.StaffGrade;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffStaffCategory;
import com.divudi.entity.hr.StaffWorkingDepartment;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.StaffDesignationFacade;
import com.divudi.facade.StaffEmployeeStatusFacade;
import com.divudi.facade.StaffEmploymentFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffGradeFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.facade.StaffStaffCategoryFacade;
import com.divudi.facade.StaffWorkingDepartmentFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StaffChangeController implements Serializable {

    private Staff staff;
    private Date fromDate;
    /////////////
    @Inject
    private SessionController sessionController;
    ///////////
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private StaffEmploymentFacade staffEmploymentFacade;
    @EJB
    private StaffWorkingDepartmentFacade staffWorkingDepartmentFacade;
    @EJB
    private StaffEmployeeStatusFacade staffEmployeeStatusFacade;
    @EJB
    private StaffStaffCategoryFacade staffStaffCategoryFacade;
    @EJB
    private StaffGradeFacade staffGradeFacade;
    @EJB
    private StaffDesignationFacade staffDesignationFacade;
    @EJB
    private StaffPaysheetComponentFacade staffPaysheetComponentFacade;
    @EJB
    private HumanResourceBean humanResourceBean;

    public void findBasic(Date date) {
        StaffPaysheetComponent tmp = getHumanResourceBean().getBasic(getStaff(), date);

        if (tmp != null) {
            getStaff().setBasic(tmp.getStaffPaySheetComponentValue());
        }
    }

    public void findBasic() {
        Date date= new Date();
        StaffPaysheetComponent tmp = getHumanResourceBean().getBasic(getStaff(), date);

        if (tmp != null) {
            getStaff().setBasic(tmp.getStaffPaySheetComponentValue());
        }
    }
    
    public void update() {
        if (getFromDate() == null) {
            JsfUtil.addErrorMessage("Select Active Date");
            return;
        }
        getStaffFacade().edit(getStaff());
        updateStaffEmployment();
    }

    private void updateStaffEmployment() {
        createComponent();
        getStaffEmploymentFacade().edit(getStaff().getStaffEmployment());
    }

    private void createComponent() {
        if (!getStaff().getStaffEmployment().getStaffWorkingDepartments().isEmpty()
                && getStaff().getWorkingDepartment().getId()
                != getStaff().getStaffEmployment().getStaffWorkingDepartments().get(
                        getStaff().getStaffEmployment().getStaffWorkingDepartments().size() - 1).getDepartment().getId()) {
            ////////
            getStaff().getStaffEmployment().getStaffWorkingDepartments().get(
                    getStaff().getStaffEmployment().getStaffWorkingDepartments().size() - 1).setToDate(getFromDate());
            getStaffWorkingDepartmentFacade().edit(getStaff().getStaffEmployment().getStaffWorkingDepartments().get(
                    getStaff().getStaffEmployment().getStaffWorkingDepartments().size() - 1));
            ///////

            StaffWorkingDepartment tmp = new StaffWorkingDepartment();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(getFromDate());
            tmp.setDepartment(getStaff().getWorkingDepartment());
            tmp.setStaffEmployment(getStaff().getStaffEmployment());
            getStaff().getStaffEmployment().getStaffWorkingDepartments().add(tmp);
        }

        if (!getStaff().getStaffEmployment().getStaffEmployeeStatuss().isEmpty()
                && getStaff().getEmployeeStatus()
                != getStaff().getStaffEmployment().getStaffEmployeeStatuss().get(
                        getStaff().getStaffEmployment().getStaffEmployeeStatuss().size() - 1).getEmployeeStatus()) {

            ////////
            getStaff().getStaffEmployment().getStaffEmployeeStatuss().get(
                    getStaff().getStaffEmployment().getStaffEmployeeStatuss().size() - 1).setToDate(getFromDate());
            getStaffEmployeeStatusFacade().edit(getStaff().getStaffEmployment().getStaffEmployeeStatuss().get(
                    getStaff().getStaffEmployment().getStaffEmployeeStatuss().size() - 1));
            ///////
            StaffEmployeeStatus tmp = new StaffEmployeeStatus();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(getFromDate());
            tmp.setEmployeeStatus(getStaff().getEmployeeStatus());
            tmp.setStaffEmployment(getStaff().getStaffEmployment());
            getStaff().getStaffEmployment().getStaffEmployeeStatuss().add(tmp);
        }

        if (!getStaff().getStaffEmployment().getStaffStaffCategorys().isEmpty()
                && getStaff().getStaffCategory().getId()
                != getStaff().getStaffEmployment().getStaffStaffCategorys().get(
                        getStaff().getStaffEmployment().getStaffStaffCategorys().size() - 1).getStaffCategory().getId()) {

            ////////
            getStaff().getStaffEmployment().getStaffStaffCategorys().get(
                    getStaff().getStaffEmployment().getStaffStaffCategorys().size() - 1).setToDate(getFromDate());
            getStaffStaffCategoryFacade().edit(getStaff().getStaffEmployment().getStaffStaffCategorys().get(
                    getStaff().getStaffEmployment().getStaffStaffCategorys().size() - 1));
            ///////

            StaffStaffCategory tmp = new StaffStaffCategory();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(getFromDate());
            tmp.setStaffCategory(getStaff().getStaffCategory());
            tmp.setStaffEmployment(getStaff().getStaffEmployment());
            getStaff().getStaffEmployment().getStaffStaffCategorys().add(tmp);
        }

        if (!getStaff().getStaffEmployment().getStaffGrades().isEmpty()
                && getStaff().getGrade().getId()
                != getStaff().getStaffEmployment().getStaffGrades().get(
                        getStaff().getStaffEmployment().getStaffGrades().size() - 1).getGrade().getId()) {

            ////////
            getStaff().getStaffEmployment().getStaffGrades().get(
                    getStaff().getStaffEmployment().getStaffGrades().size() - 1).setToDate(getFromDate());
            getStaffGradeFacade().edit(getStaff().getStaffEmployment().getStaffGrades().get(
                    getStaff().getStaffEmployment().getStaffGrades().size() - 1));
            ///////

            StaffGrade tmp = new StaffGrade();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(new Date());
            tmp.setGrade(getStaff().getGrade());
            tmp.setStaffEmployment(getStaff().getStaffEmployment());
            getStaff().getStaffEmployment().getStaffGrades().add(tmp);
        }

        if (!getStaff().getStaffEmployment().getStaffDesignations().isEmpty()
                && getStaff().getDesignation().getId()
                != getStaff().getStaffEmployment().getStaffDesignations().get(
                        getStaff().getStaffEmployment().getStaffDesignations().size() - 1).getDesignation().getId()) {

            ////////
            getStaff().getStaffEmployment().getStaffDesignations().get(
                    getStaff().getStaffEmployment().getStaffDesignations().size() - 1).setToDate(getFromDate());
            getStaffDesignationFacade().edit(getStaff().getStaffEmployment().getStaffDesignations().get(
                    getStaff().getStaffEmployment().getStaffDesignations().size() - 1));
            ///////

            StaffDesignation tmp = new StaffDesignation();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setFromDate(new Date());
            tmp.setDesignation(getStaff().getDesignation());
            tmp.setStaffEmployment(getStaff().getStaffEmployment());
            getStaff().getStaffEmployment().getStaffDesignations().add(tmp);
        }

    }

    public List<Department> getInstitutionDepatrments() {

        String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getSessionController().getInstitution().getId();

        return getDepartmentFacade().findByJpql(sql);
    }

    public StaffChangeController() {
    }

    public Staff getStaff() {
        if (staff == null) {
            staff = new Staff();
        }
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public StaffEmploymentFacade getStaffEmploymentFacade() {
        return staffEmploymentFacade;
    }

    public void setStaffEmploymentFacade(StaffEmploymentFacade staffEmploymentFacade) {
        this.staffEmploymentFacade = staffEmploymentFacade;
    }

    public StaffWorkingDepartmentFacade getStaffWorkingDepartmentFacade() {
        return staffWorkingDepartmentFacade;
    }

    public void setStaffWorkingDepartmentFacade(StaffWorkingDepartmentFacade staffWorkingDepartmentFacade) {
        this.staffWorkingDepartmentFacade = staffWorkingDepartmentFacade;
    }

    public StaffEmployeeStatusFacade getStaffEmployeeStatusFacade() {
        return staffEmployeeStatusFacade;
    }

    public void setStaffEmployeeStatusFacade(StaffEmployeeStatusFacade staffEmployeeStatusFacade) {
        this.staffEmployeeStatusFacade = staffEmployeeStatusFacade;
    }

    public StaffStaffCategoryFacade getStaffStaffCategoryFacade() {
        return staffStaffCategoryFacade;
    }

    public void setStaffStaffCategoryFacade(StaffStaffCategoryFacade staffStaffCategoryFacade) {
        this.staffStaffCategoryFacade = staffStaffCategoryFacade;
    }

    public StaffGradeFacade getStaffGradeFacade() {
        return staffGradeFacade;
    }

    public void setStaffGradeFacade(StaffGradeFacade staffGradeFacade) {
        this.staffGradeFacade = staffGradeFacade;
    }

    public StaffDesignationFacade getStaffDesignationFacade() {
        return staffDesignationFacade;
    }

    public void setStaffDesignationFacade(StaffDesignationFacade staffDesignationFacade) {
        this.staffDesignationFacade = staffDesignationFacade;
    }

    public StaffPaysheetComponentFacade getStaffPaysheetComponentFacade() {
        return staffPaysheetComponentFacade;
    }

    public void setStaffPaysheetComponentFacade(StaffPaysheetComponentFacade staffPaysheetComponentFacade) {
        this.staffPaysheetComponentFacade = staffPaysheetComponentFacade;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }
}
