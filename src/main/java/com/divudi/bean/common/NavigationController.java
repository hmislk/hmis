package com.divudi.bean.common;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author buddh
 */
@Named
@SessionScoped
public class NavigationController implements Serializable {

    /**
     * Creates a new instance of NavigationController
     */
    public NavigationController() {
    }
    
    public String navigateToInstitutionBranch(){
        return "/admin/institutions/institution_branch";
    }
    
    public String navigateToManageDepartment(){
        return "/admin/institutions/department_management";
    }
    
    public String navigateToCollectingCenter(){
        return "/admin/institutions/collecting_centre";
    }
    
    public String navigateToCreditCompany(){
        return "/admin/institutions/credit_company";
    }
    
    public String navigateToArea(){
        return "/admin/institutions/area";
    }
    
    public String navigateToStaffSignature(){
        return "/admin/institutions/admin_staff_signature";
    }
    
    public String navigateToInstitutionBulkDelete(){
        return "/admin/institutions/institution_bulk_delete";
    }
    
    public String navigateToDepartmentBulkDelete(){
        return "/admin/institutions/department_bulk_delete";
    }
    
    public String navigateToAdminDoctorSpeciality(){
        return "/admin/staff/admin_doctor_speciality";
    }
    
     public String navigateToAdminDoctor(){
        return "/admin/staff/admin_doctor";
    }
    public String navigateToAdminDoctorConsultant(){
        return "/admin/staff/admin_doctor_consultant";
    }
    
    public String navigateToDoctorSpecilaity(){
        return "/admin/staff/admin_speciality";
    }
    
    public String navigateToAdminUserStaff(){
        return "/admin/staff/admin_user_staff";
    }
    public String navigateToStaffList(){
        return "/admin/staff/admin_staff_list";
    }
    
    public String navigateToPersonInstitution(){
        return "/admin/staff/person_institution";
    }
    public String navigateToStaffBulkDelete(){
        return "/admin/staff/staff_bulk_delete";
    }
    
    public String navigateToAdminStaffSignature(){
        return "/admin/staff/admin_staff_signature";
    }
}
