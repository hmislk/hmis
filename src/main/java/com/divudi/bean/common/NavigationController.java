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
    
    public String navigateToMembershipScheme(){
        return "/admin/discount/membership_scheme";
    }
    
    public String navigateToMembershipPaymentMethodAllowed(){
        return "/admin/discount/membership_scheme_payment_method_allowed";
    }
    
    public String navigateToMembershipSchemeDiscountInward(){
        return "/admin/discount/membership_scheme_discount_inward";
    }
    
    public String navigateToMembershipSchemeDiscountOpdByCategory(){
        return "/admin/discount/membership_scheme_discount_opd_by_category";
    }
    
    public String navigateToMembershipSchemeDiscountOpdByDepartment(){
        return "/admin/discount/membership_scheme_discount_opd_by_department";
    }
    
    public String navigateToMembershipSchemeDiscountPharmacyByCategory(){
        return "/admin/discount/payment_method_discount_pharmacy_by_category";
    }
    
    public String navigateToMembershipSchemeDiscountChannelingByDepartment(){
        return "/admin/discount/membership_scheme_discount_channelling_by_department";
    }
    
    public String navigateToPaymentScheme(){
        return "/admin/discount/payment_scheme";
    }
    
    public String navigateToPaymentSchemeDiscountChannel(){
        return "/admin/discount/payment_scheme_discount_channel";
    }
    
    public String navigateToPaymentSchemeDiscountOpdBtCategory(){
        return "/admin/discount/payment_scheme_discount_opd_by_category";
    }
    
    public String navigateToPaymentSchemeDiscountOpdByDepartment(){
        return "/admin/discount/payment_scheme_discount_opd_by_department";
    }
    
    public String navigateToPaymentSchemeDiscountOpdByItem(){
        return "/admin/discount/payment_scheme_discount_opd_by_item";
    }
    
    public String navigateToPaymentSchemeDiscountPharmacyByCategory(){
        return "/admin/discount/payment_scheme_discount_pharmacy_by_category";
    }
    
    public String navigateToPaymentMethodAllowed(){
        return "/admin/discount/payment_scheme_payment_method_allowed";
    }
    
    public String navigateToPaymentMethodDiscountOpdByCategory(){
        return "/admin/discount/payment_scheme_payment_method_allowed";
    }
    
    public String navigateToPaymentMethodDiscountOpdByDepartment(){
        return "/admin/discount/payment_method_discount_opd_by_department";
    }
    
    public String navigateToPaymentMethodDiscountOpdByItem(){
        return "/admin/discount/payment_method_discount_opd_by_item";
    }
    
    public String navigateToReportInward(){
        return "/admin/discount/report_inward";
    }
    
    public String navigateToReportOpd(){
        return "/admin/discount/report_opd";
    }
}
