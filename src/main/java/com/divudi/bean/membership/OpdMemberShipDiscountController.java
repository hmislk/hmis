/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.membership;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmaceuticalItemCategoryController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.ServiceSubCategory;
import com.divudi.core.entity.lab.InvestigationCategory;
import com.divudi.core.entity.membership.ChannellingMemberShipDiscount;
import com.divudi.core.entity.membership.MembershipScheme;
import com.divudi.core.entity.membership.OpdMemberShipDiscount;
import com.divudi.core.entity.membership.PaymentSchemeDiscount;
import com.divudi.core.entity.membership.PharmacyMemberShipDiscount;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.facade.PaymentSchemeDiscountFacade;
import com.divudi.core.facade.PriceMatrixFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class OpdMemberShipDiscountController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    PharmaceuticalItemCategoryController pharmaceuticalItemCategoryController;
    @EJB
    private PriceMatrixFacade ejbFacade;
    @EJB
    PaymentSchemeDiscountFacade paymentSchemeDiscountFacade;
    private PriceMatrix current;
    private List<PriceMatrix> items = null;
    BillType billType;
    PaymentScheme paymentScheme;
    PaymentMethod paymentMethod;
    MembershipScheme membershipScheme;
    Category category;
    Item item;
    Institution institution;
    private Institution site;
    Department department;
    double fromPrice;
    double toPrice;
    double margin;
    private Category roomLocation;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

    public void recreateModel() {
        fromPrice = toPrice + 1;
        toPrice = 0.0;
        margin = 0;
        items = null;
        membershipScheme = null;
        paymentMethod = null;
        site = null;
    }

    public void saveSelectedDepartmentPaymentScheme() {
        PriceMatrix a = new PaymentSchemeDiscount();
        saveDepartment(a);
        createItemsDepartmentsPaymentScheme();
        clearInstanceVars();

    }

    public void saveSelectedSitePaymentScheme() {
        PriceMatrix a = new PaymentSchemeDiscount();
        saveSite(a);
        createItemsSitesPaymentScheme();
        clearInstanceVars();

    }

    public void saveSite(PriceMatrix a) {

        if (membershipScheme == null && paymentScheme == null) {
            JsfUtil.addErrorMessage("Membership Scheme or Payment Scheme");
            return;
        }

        if (site == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        //  PriceMatrix a = new OpdMemberShipDiscount();
        a.setMembershipScheme(membershipScheme);
        a.setPaymentScheme(paymentScheme);
        a.setPaymentMethod(paymentMethod);
        a.setToInstitution(site);
        if (department != null) {
            a.setInstitution(department.getInstitution());
        }
        a.setDiscountPercent(margin);
        a.setCreatedAt(new Date());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        JsfUtil.addSuccessMessage("Saved Successfully");
        //    recreateModel();

    }

    public void createItemsSitesPaymentScheme() {
        filterItems = null;
        String sql;
        HashMap hm = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false"
                + " and a.paymentScheme=:pm "
                + " and a.category is null "
                + " and a.toInstitution is not null"
                + " order by a.paymentScheme.name,a.toInstitution.name";

        hm.put("pm", paymentScheme);
        items = getFacade().findByJpql(sql, hm);
    }

//    public void saveSelectedDepartmentPaymentMethod() {
//        PriceMatrix a = new PaymentSchemeDiscount();
//        saveDepartmentForPaymentMethod(a);
//        createItemsDepartmentsPaymentMethod();
//        clearInstanceVars();
//
//    }
    public void saveSelectedDepartment() {
        PriceMatrix a = new OpdMemberShipDiscount();
        saveDepartment(a);
        createItemsDepartments();
        clearInstanceVars();
    }

    public void saveChannellingDiscountMatrixForDepartment() {
        if (membershipScheme == null) {
            JsfUtil.addErrorMessage("Select a Membership Scheme");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }
        ChannellingMemberShipDiscount a = new ChannellingMemberShipDiscount();
        a.setMembershipScheme(membershipScheme);
        a.setPaymentScheme(paymentScheme);
        a.setPaymentMethod(paymentMethod);
        a.setDepartment(department);
        if (department != null) {
            a.setInstitution(department.getInstitution());
        }
        a.setDiscountPercent(margin);
        a.setCreatedAt(new Date());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        JsfUtil.addSuccessMessage("Saved Successfully");

        fillMatricesForChannellingMembershipsForItemsDepartments();
        clearInstanceVars();
    }

    public String toManageDiscountMatrixForChannellingByDepartment() {
        return "/membership/membership_scheme_discount_channelling_by_department";
    }

    public String toManageDiscountMatrixForPharmacyByDepartmentAndCategory() {
        fillDiscountMetrixesForPharmacyForDepartmentAndCategory();
        return "/membership/membership_scheme_discount_pharmacy_by_department_and_category";
    }

    public void saveSelectedChannelPaymentScheme() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Membership Scheme or Payment Scheme");
            return;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }
        if (category == null) {
            JsfUtil.addErrorMessage("Please select Category");
            return;
        }

        PaymentSchemeDiscount a = new PaymentSchemeDiscount();
        a.setPaymentScheme(paymentScheme);
        a.setPaymentMethod(paymentMethod);
        a.setCategory(category);
        a.setBillType(BillType.ChannelCash);
        a.setDiscountPercent(margin);
        a.setCreatedAt(new Date());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        JsfUtil.addSuccessMessage("Saved Successfully");
        createItemsChannelPaymentScheme();
        clearInstanceVars();

    }

    public void saveDepartment(PriceMatrix a) {

        if (membershipScheme == null && paymentScheme == null) {
            JsfUtil.addErrorMessage("Membership Scheme or Payment Scheme");
            return;
        }

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        //  PriceMatrix a = new OpdMemberShipDiscount();
        a.setMembershipScheme(membershipScheme);
        a.setPaymentScheme(paymentScheme);
        a.setPaymentMethod(paymentMethod);
        a.setDepartment(department);
        if (department != null) {
            a.setInstitution(department.getInstitution());
        }
        a.setDiscountPercent(margin);
        a.setCreatedAt(new Date());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        JsfUtil.addSuccessMessage("Saved Successfully");
        //    recreateModel();

    }

//    public void saveDepartmentForPaymentMethod(PriceMatrix a) {
//
//        if (department == null) {
//            JsfUtil.addErrorMessage("Please select a department");
//            return;
//        }
//
//        if (paymentMethod == null) {
//            JsfUtil.addErrorMessage("Please select Payment Method");
//            return;
//        }
//
//        //  PriceMatrix a = new OpdMemberShipDiscount();
//        a.setMembershipScheme(null);
//        a.setPaymentScheme(null);
//        a.setPaymentMethod(paymentMethod);
//        a.setDepartment(department);
//        if (department != null) {
//            a.setInstitution(department.getInstitution());
//        }
//        a.setDiscountPercent(margin);
//        a.setCreatedAt(new Date());
//        a.setCreater(getSessionController().getLoggedUser());
//        getFacade().create(a);
//        JsfUtil.addSuccessMessage("Saved Successfully");
//        //    recreateModel();
//
//    }
    public void saveOpdCategory() {
        PriceMatrix p = new OpdMemberShipDiscount();
        saveSelectedCategory(p);
        createItemsCategoryOpd();
        clearInstanceVars();
    }

    public void saveOpdCategoryPaymentScheme() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategory(p);
        createItemsCategoryOpdPaymentScheme();
        clearInstanceVars();
    }

    public void saveOpdCategoryPaymentMethod() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategoryPaymentMethod(p);
        createItemsCategoryOpdPaymentMethod();
        clearInstanceVars();
    }

    public void clearInstanceVars() {
        item = null;
        category = null;
        department = null;
        paymentMethod = null;
        site = null;
        margin = 0.0;
    }

    public void saveItemPaymentScheme() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategory(p);
        createItemsPaymentScheme();
        clearInstanceVars();
    }

    public void saveItemPaymentMethod() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategoryPaymentMethod(p);
        createItemsPaymentMethod();
        clearInstanceVars();
    }

    public void savePharmacyDiscountMatrixForDepartmentAndCategory() {

        if (membershipScheme == null) {
            JsfUtil.addErrorMessage("Membership Scheme ?");
            return;
        }

        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Payment Method?");
            return;
        }

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }

        if (category == null) {
            JsfUtil.addErrorMessage("Please select Category");
            return;
        }

        PharmacyMemberShipDiscount a = new PharmacyMemberShipDiscount();

        a.setMembershipScheme(membershipScheme);
        a.setPaymentMethod(paymentMethod);
        a.setCategory(category);
        a.setDepartment(department);
        if (department != null) {
            a.setInstitution(department.getInstitution());
        }
        a.setDiscountPercent(margin);
        a.setCreatedAt(new Date());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);

        //    recreateModel();
        fillDiscountMetrixesForPharmacyForDepartmentAndCategory();
        JsfUtil.addSuccessMessage("Saved Successfully");
    }

    public void fillDiscountMetrixesForPharmacyForDepartmentAndCategory() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from PharmacyMemberShipDiscount a "
                + " where a.retired=false "
                + " and a.membershipScheme is not null "
                + " order by a.membershipScheme.name,a.category.name";
        items = getFacade().findByJpql(sql);
    }

    public void savePharmacyCategoryPaymentScheme() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategory(p);
        createItemsCategoryPharmacyPaymentScheme();
        clearInstanceVars();
    }

    public void savePharmacyCategoryPaymentSchemeForAllCategoriesAndAllPaymentMethods() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a Payment Scheme");
            return;
        }
        for (PaymentMethod pm : PaymentMethod.asList()) {
            for (Category c : pharmaceuticalItemCategoryController.getItems()) {
                PaymentSchemeDiscount p = fetchPaymentSchemeDiscount(null,
                        c,
                        null,
                        paymentScheme,
                        pm,
                        null,
                        null);
                p.setDiscountPercent(margin);
                System.out.println("p = " + p);
                paymentSchemeDiscountFacade.edit(p);
            }
        }
    }

    public void savePharmacyCategoryPaymentSchemeForAllCategoriesAndSelectedPaymentMethod() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a Payment Scheme");
            return;
        }
        if(paymentMethod == null){
            JsfUtil.addErrorMessage("Please select a Payment Method");
            return;
        }
            for (Category c : pharmaceuticalItemCategoryController.getItems()) {
                PaymentSchemeDiscount p = fetchPaymentSchemeDiscount(null,
                        c,
                        null,
                        paymentScheme,
                        paymentMethod,
                        null,
                        null);
                p.setDiscountPercent(margin);
                System.out.println("p = " + p);
                paymentSchemeDiscountFacade.edit(p);
            }
    }

    public void savePharmacyCategoryPaymentMethod() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategoryPaymentMethod(p);
        createItemsCategoryPharmacyPaymentMethod();
        clearInstanceVars();
    }

    public void saveSelectedCategory(PriceMatrix a) {

        if (membershipScheme == null && paymentScheme == null) {
            JsfUtil.addErrorMessage("Membership Scheme or Payment Scheme");
            return;
        }

        if (category == null && item == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        //  PriceMatrix a = new OpdMemberShipDiscount();
        a.setMembershipScheme(membershipScheme);
        a.setPaymentScheme(paymentScheme);
        a.setPaymentMethod(paymentMethod);
        a.setCategory(category);
        a.setItem(item);
        if (department != null) {
            a.setInstitution(department.getInstitution());
        }
        a.setDiscountPercent(margin);
        a.setCreatedAt(new Date());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        JsfUtil.addSuccessMessage("Saved Successfully");
        //    recreateModel();

    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(Item item, Category category, MembershipScheme membershipScheme,
            PaymentScheme paymentScheme, PaymentMethod paymentMethod,
            Department department, Institution institution) {
        StringBuilder jpql = new StringBuilder("select m from PaymentSchemeDiscount m where m.retired = :ret");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);

        if (item != null) {
            jpql.append(" and m.item = :item");
            parameters.put("item", item);
        }
        if (category != null) {
            jpql.append(" and m.category = :category");
            parameters.put("category", category);
        }
        if (membershipScheme != null) {
            jpql.append(" and m.membershipScheme = :membershipScheme");
            parameters.put("membershipScheme", membershipScheme);
        }
        if (paymentScheme != null) {
            jpql.append(" and m.paymentScheme = :paymentScheme");
            parameters.put("paymentScheme", paymentScheme);
        }
        if (paymentMethod != null) {
            jpql.append(" and m.paymentMethod = :paymentMethod");
            parameters.put("paymentMethod", paymentMethod);
        }
        if (department != null) {
            jpql.append(" and m.institution = :institution");
            parameters.put("institution", department.getInstitution());
        }
        if (institution != null) {
            jpql.append(" and m.institution = :institution");
            parameters.put("institution", institution);
        }

        PaymentSchemeDiscount m = paymentSchemeDiscountFacade.findFirstByJpql(jpql.toString(), parameters);

        if (m == null) {
            m = new PaymentSchemeDiscount();
            if (item != null) {
                m.setItem(item);
            }
            if (category != null) {
                m.setCategory(category);
            }
            if (membershipScheme != null) {
                m.setMembershipScheme(membershipScheme);
            }
            if (paymentScheme != null) {
                m.setPaymentScheme(paymentScheme);
            }
            if (paymentMethod != null) {
                m.setPaymentMethod(paymentMethod);
            }
            if (department != null) {
                m.setInstitution(department.getInstitution());
            }
            if (institution != null) {
                m.setInstitution(institution);
            }
            m.setCreatedAt(new Date());
            m.setCreater(getSessionController().getLoggedUser());
            m.setRetired(false); // Default value
            getFacade().create(m);
        }

        return m;
    }

    public void saveSelectedCategoryPaymentMethod(PriceMatrix a) {

        if (category == null && item == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select Payment Method");
            return;
        }

        //  PriceMatrix a = new OpdMemberShipDiscount();
        a.setMembershipScheme(null);
        a.setPaymentScheme(null);
        a.setPaymentMethod(paymentMethod);
        a.setCategory(category);
        a.setItem(item);
        if (department != null) {
            a.setInstitution(department.getInstitution());
        }
        a.setDiscountPercent(margin);
        a.setCreatedAt(new Date());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        JsfUtil.addSuccessMessage("Saved Successfully");
        //    recreateModel();

    }

    public PriceMatrixFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PriceMatrixFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public OpdMemberShipDiscountController() {
    }

    public PriceMatrix getCurrent() {
        return current;
    }

    public void setCurrent(PriceMatrix current) {

        this.current = current;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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

    public double getFromPrice() {
        return fromPrice;
    }

    public void setFromPrice(double fromPrice) {
        this.fromPrice = fromPrice;
    }

    public double getToPrice() {
        return toPrice;
    }

    public void setToPrice(double toPrice) {
        this.toPrice = toPrice;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }

    public void deleteDepartmentPaymentScheme() {
        deleteDepartment();
        createItemsDepartmentsPaymentScheme();
    }

    public void deleteSitePaymentScheme() {
        deleteSite();
        createItemsSitesPaymentScheme();
        clearInstanceVars();
    }

    public void deleteDepartmentPaymentMethod() {
        deleteDepartment();
        createItemsDepartmentsPaymentMethod();
    }

    public void deleteDepartment() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        //    recreateModel();

        current = null;
        getCurrent();
        filterItems = null;
        createItemsDepartments();
    }

    public void deleteSite() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        //    recreateModel();

        current = null;
        getCurrent();
        filterItems = null;
    }

    public void deleteCategoryOpd() {
        deleteCategory();
        createItemsCategoryOpd();
    }

    public void deleteCategoryOpdPaymentScheme() {
        deleteCategory();
        createItemsCategoryOpdPaymentScheme();
    }

    public void deleteCategoryOpdPaymentMethod() {
        deleteCategory();
        createItemsCategoryOpdPaymentMethod();
    }

    public void deleteItemPaymentScheme() {
        deleteCategory();
        createItemsPaymentScheme();
    }

    public void deleteItemPaymentMethod() {
        deleteCategory();
        createItemsPaymentMethod();
    }

    public void deleteCategoryPharmacy() {
        deleteCategory();
        createItemsCategoryPharmacy();
    }

    public void deleteCategoryPharmacyPaymentScheme() {
        deleteCategory();
        createItemsCategoryPharmacyPaymentScheme();
    }

    public void deleteCategoryPharmacyPaymentMethod() {
        deleteCategory();
        createItemsCategoryPharmacyPaymentMethod();
    }

    public void deleteCategory() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        //    recreateModel();

        current = null;
        getCurrent();
        filterItems = null;

    }

    private PriceMatrixFacade getFacade() {
        return ejbFacade;
    }

    private List<PriceMatrix> filterItems;

    public List<PriceMatrix> getItems() {

        return items;
    }

    public String toManageDiscountMatrixForOpdByDepartment() {
//        toManageDiscountMatrixForChannellingByDepartment
        filterItems = null;
        String sql;
        sql = "select a from OpdMemberShipDiscount a "
                + " where a.retired=false "
                + " and a.category is null"
                + " order by a.membershipScheme.name,a.department.name";
        items = getFacade().findByJpql(sql);
        return "/membership/membership_scheme_discount_opd_by_department";

    }

    public void createItemsDepartments() {
        filterItems = null;
        String sql;
        sql = "select a from OpdMemberShipDiscount a "
                + " where a.retired=false "
                + " and a.category is null"
                + " order by a.membershipScheme.name,a.department.name";
        items = getFacade().findByJpql(sql);
    }

    public void fillMatricesForChannellingMembershipsForItemsDepartments() {
        filterItems = null;
        String sql;
        sql = "select a from ChannellingMemberShipDiscount a "
                + " where a.retired=false "
                + " and a.category is null"
                + " order by a.membershipScheme.name,a.department.name";
        items = getFacade().findByJpql(sql);
    }

    public void createItemsDepartmentsPaymentScheme() {
        filterItems = null;
        String sql;
        HashMap hm = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false"
                + " and a.paymentScheme=:pm "
                + " and a.category is null "
                + " order by a.paymentScheme.name,a.department.name";
        hm.put("pm", paymentScheme);
        items = getFacade().findByJpql(sql, hm);
    }

    public void createItemsChannelPaymentScheme() {
        filterItems = null;
        String sql;
        HashMap hm = new HashMap();
        sql = "select a "
                + " from PaymentSchemeDiscount a "
                + " where a.retired=false"
                + " and a.paymentScheme=:pm "
                + " and a.billType=:bt "
                + " order by a.paymentScheme.name ";
        hm.put("pm", paymentScheme);
        hm.put("bt", BillType.ChannelCash);
        items = getFacade().findByJpql(sql, hm);
    }

    public void createItemsDepartmentsPaymentMethod() {
        filterItems = null;
        String sql;
        HashMap hm = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false"
                + " and a.paymentScheme is null "
                + " and a.category is null"
                + " and a.membershipScheme is null "
                + " order by a.department.name";
        //  hm.put("pm", paymentScheme);
        items = getFacade().findByJpql(sql, hm);
    }

    public void createItemsCategoryOpd() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from OpdMemberShipDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and (type(a.category)=:service "
                + " or type(a.category)=:sub "
                + " or type(a.category)=:invest )"
                + " order by a.membershipScheme.name,a.category.name";
        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void createItemsCategoryOpdPaymentScheme() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and a.paymentScheme=:pm "
                + " and (type(a.category)=:service "
                + " or type(a.category)=:sub "
                + " or type(a.category)=:invest )"
                + " order by a.paymentScheme.name,"
                + " a.category.name";
        temMap.put("pm", paymentScheme);
        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void createItemsCategoryOpdPaymentMethod() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and a.paymentScheme is null"
                + " and a.membershipScheme is null "
                + " and (type(a.category)=:service "
                + " or type(a.category)=:sub "
                + " or type(a.category)=:invest )"
                + " order by a.category.name";
        //     temMap.put("pm", paymentScheme);
        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void createItemsPaymentScheme() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and a.paymentScheme=:pm "
                + " order by a.paymentScheme.name,"
                + " a.item.name";
        temMap.put("pm", paymentScheme);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void createItemsPaymentMethod() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and a.paymentScheme is null "
                + " and a.membershipScheme is null "
                + " order by a.item.name";
        // temMap.put("pm", paymentScheme);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void createItemsCategoryPharmacy() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from OpdMemberShipDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and type(a.category)=:ph "
                + " order by a.membershipScheme.name,a.category.name";
        temMap.put("ph", PharmaceuticalItemCategory.class);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void createItemsCategoryPharmacyPaymentScheme() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and a.paymentScheme=:pm "
                + " and type(a.category)=:ph "
                + " order by a.paymentScheme.name,a.category.name";
        temMap.put("pm", paymentScheme);
        temMap.put("ph", PharmaceuticalItemCategory.class);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void createItemsCategoryPharmacyPaymentMethod() {
        filterItems = null;
        String sql;
        HashMap temMap = new HashMap();
        sql = "select a from PaymentSchemeDiscount a "
                + " where a.retired=false "
                + " and a.department is null "
                + " and a.paymentScheme is null "
                + " and a.membershipScheme is null "
                + " and type(a.category)=:ph "
                + " order by a.category.name";
        //  temMap.put("pm", paymentScheme);
        temMap.put("ph", PharmaceuticalItemCategory.class);
        items = getFacade().findByJpql(sql, temMap);
    }

    public void onEdit(PriceMatrix tmp) {
        getFacade().edit(tmp);
        JsfUtil.addSuccessMessage("Update Successfully");
        clearInstanceVars();

    }

    public Category getRoomLocation() {

        return roomLocation;
    }

    public void setRoomLocation(Category roomLocation) {
        this.roomLocation = roomLocation;
    }

    public List<PriceMatrix> getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(List<PriceMatrix> filterItems) {
        this.filterItems = filterItems;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

}
