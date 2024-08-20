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
    private int adminStaffMenuIndex;

    public NavigationController() {
    }

    public String navigateToInstitutionBranch() {
        return "/admin/institutions/institution_branch?faces-redirect=true";
    }

    public String navigateToManageDepartment() {
        return "/admin/institutions/department_management?faces-redirect=true";
    }

    public String navigateToManageInstitution() {
        return "/admin/institutions/institution_management?faces-redirect=true";
    }

    public String navigateToCollectingCenter() {
        return "/admin/institutions/collecting_centre?faces-redirect=true";
    }

    public String navigateToCollectingCenterUpload() {
        return "/admin/institutions/collecting_centre_upload?faces-redirect=true";
    }

    public String navigateToCreditCompany() {
        return "/admin/institutions/credit_company?faces-redirect=true";
    }

    public String navigateToCreditCompanyUpload() {
        return "/admin/institutions/credit_company_upload?faces-redirect=true";
    }

    public String navigateToArea() {
        return "/admin/institutions/area?faces-redirect=true";
    }

    public String navigateToStaffSignature() {
        return "/admin/institutions/admin_staff_signature?faces-redirect=true";
    }

    public String navigateToInstitutionBulkDelete() {
        return "/admin/institutions/institution_bulk_delete?faces-redirect=true";
    }

    public String navigateToDepartmentBulkDelete() {
        return "/admin/institutions/department_bulk_delete?faces-redirect=true";
    }

    public String navigateToAdminDoctorSpeciality() {
        return "/admin/staff/admin_doctor_speciality?faces-redirect=true";
    }

    public String navigateToViewStaffSignatures() {
        return "/admin/staff/admin_staff_signature.xhtml?faces-redirect=true";
    }

   

    public String navigateToAdminDoctorConsultant() {
        return "/admin/staff/admin_doctor_consultant?faces-redirect=true";
    }

    public String navigateToDoctorSpecilaity() {
        return "/admin/staff/admin_speciality?faces-redirect=true";
    }

    

    public String navigateToAdminUserStaff() {
        return "/admin/staff/admin_user_staff?faces-redirect=true";
    }

    public String navigateToStaffList() {
        return "/admin/staff/admin_staff_list?faces-redirect=true";
    }

    public String navigateToPersonInstitution() {
        return "/admin/staff/person_institution?faces-redirect=true";
    }

    public String navigateToStaffBulkDelete() {
        return "/admin/staff/staff_bulk_delete?faces-redirect=true";
    }

    public String navigateToAdminStaffSignature() {
        return "/admin/staff/admin_staff_signature?faces-redirect=true";
    }

    

    public String navigateToMembershipPaymentMethodAllowed() {
        return "/admin/pricing/membership_scheme_payment_method_allowed?faces-redirect=true";
    }

    public String navigateToMembershipSchemeDiscountInward() {
        return "/admin/pricing/membership_scheme_discount_inward?faces-redirect=true";
    }

    public String navigateToMembershipSchemeDiscountOpdByCategory() {
        return "/admin/pricing/membership_scheme_discount_opd_by_category?faces-redirect=true";
    }

    public String navigateToMembershipSchemeDiscountOpdByDepartment() {
        return "/admin/pricing/membership_scheme_discount_opd_by_department?faces-redirect=true";
    }

    public String navigateToMembershipSchemeDiscountPharmacyByCategory() {
        return "/admin/pricing/payment_method_discount_pharmacy_by_category?faces-redirect=true";
    }

    public String navigateToMembershipSchemeDiscountChannelingByDepartment() {
        return "/admin/pricing/membership_scheme_discount_channelling_by_department?faces-redirect=true";
    }


    public String navigateToPaymentSchemeDiscountChannel() {
        return "/admin/pricing/payment_scheme_discount_channel?faces-redirect=true";
    }

    public String navigateToPaymentSchemeDiscountOpdBtCategory() {
        return "/admin/pricing/payment_scheme_discount_opd_by_category?faces-redirect=true";
    }

    public String navigateToPaymentSchemeDiscountOpdByDepartment() {
        return "/admin/pricing/payment_scheme_discount_opd_by_department?faces-redirect=true";
    }

    public String navigateToPaymentSchemeDiscountOpdByItem() {
        return "/admin/pricing/payment_scheme_discount_opd_by_item?faces-redirect=true";
    }

    public String navigateToPaymentSchemeDiscountPharmacyByCategory() {
        return "/admin/pricing/payment_scheme_discount_pharmacy_by_category?faces-redirect=true";
    }

    public String navigateToPaymentMethodAllowed() {
        return "/admin/pricing/payment_scheme_payment_method_allowed?faces-redirect=true";
    }

    public String navigateToPaymentMethodDiscountOpdByCategory() {
        return "/admin/pricing/payment_scheme_payment_method_allowed?faces-redirect=true";
    }

    public String navigateToPaymentMethodDiscountOpdByDepartment() {
        return "/admin/pricing/payment_method_discount_opd_by_department?faces-redirect=true";
    }

    public String navigateToPaymentMethodDiscountOpdByItem() {
        return "/admin/pricing/payment_method_discount_opd_by_item?faces-redirect=true";
    }

    public String navigateToReportInward() {
        return "/admin/pricing/report_inward?faces-redirect=true";
    }

    public String navigateToReportOpd() {
        return "/admin/pricing/report_opd?faces-redirect=true";
    }

    public int getAdminStaffMenuIndex() {
        return adminStaffMenuIndex;
    }

    public void setAdminStaffMenuIndex(int adminStaffMenuIndex) {
        this.adminStaffMenuIndex = adminStaffMenuIndex;
    }
}
