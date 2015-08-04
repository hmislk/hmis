/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.memberShip;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.ServiceCategory;
import com.divudi.entity.ServiceSubCategory;
import com.divudi.entity.lab.InvestigationCategory;
import com.divudi.entity.memberShip.MembershipScheme;
import com.divudi.entity.memberShip.OpdMemberShipDiscount;
import com.divudi.entity.memberShip.PaymentSchemeDiscount;
import com.divudi.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.facade.PriceMatrixFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class OpdMemberShipDiscountController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PriceMatrixFacade ejbFacade;
    private PriceMatrix current;
    private List<PriceMatrix> items = null;
    BillType billType;
    PaymentScheme paymentScheme;
    PaymentMethod paymentMethod;
    MembershipScheme membershipScheme;
    Category category;
    Item item;
    Institution institution;
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
    }

    public void saveSelectedDepartmentPaymentScheme() {
        PriceMatrix a = new PaymentSchemeDiscount();
        saveDepartment(a);
        createItemsDepartmentsPaymentScheme();
        clearInstanceVars();

    }

    public void saveSelectedDepartmentPaymentMethod() {
        PriceMatrix a = new PaymentSchemeDiscount();
        saveDepartmentForPaymentMethod(a);
        createItemsDepartmentsPaymentMethod();
        clearInstanceVars();

    }

    public void saveSelectedDepartment() {
        PriceMatrix a = new OpdMemberShipDiscount();
        saveDepartment(a);
        createItemsDepartments();
        clearInstanceVars();
    }

    public void saveDepartment(PriceMatrix a) {

        if (membershipScheme == null && paymentScheme == null) {
            UtilityController.addErrorMessage("Membership Scheme or Payment Scheme");
            return;
        }

        if (department == null) {
            UtilityController.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            UtilityController.addErrorMessage("Please select Payment Method");
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
        a.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        UtilityController.addSuccessMessage("Saved Successfully");
        //    recreateModel();

    }

    public void saveDepartmentForPaymentMethod(PriceMatrix a) {

        if (department == null) {
            UtilityController.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            UtilityController.addErrorMessage("Please select Payment Method");
            return;
        }

        //  PriceMatrix a = new OpdMemberShipDiscount();
        a.setMembershipScheme(null);
        a.setPaymentScheme(null);
        a.setPaymentMethod(paymentMethod);
        a.setDepartment(department);
        if (department != null) {
            a.setInstitution(department.getInstitution());
        }
        a.setDiscountPercent(margin);
        a.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        UtilityController.addSuccessMessage("Saved Successfully");
        //    recreateModel();

    }

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

    public void savePharmacyCategory() {
        PriceMatrix p = new OpdMemberShipDiscount();
        saveSelectedCategory(p);
        createItemsCategoryPharmacy();
        clearInstanceVars();
    }

    public void savePharmacyCategoryPaymentScheme() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategory(p);
        createItemsCategoryPharmacyPaymentScheme();
        clearInstanceVars();
    }

    public void savePharmacyCategoryPaymentMethod() {
        PriceMatrix p = new PaymentSchemeDiscount();
        saveSelectedCategoryPaymentMethod(p);
        createItemsCategoryPharmacyPaymentMethod();
        clearInstanceVars();
    }

    public void saveSelectedCategory(PriceMatrix a) {

        if (membershipScheme == null && paymentScheme == null) {
            UtilityController.addErrorMessage("Membership Scheme or Payment Scheme");
            return;
        }

        if (category == null && item == null) {
            UtilityController.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            UtilityController.addErrorMessage("Please select Payment Method");
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
        a.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        UtilityController.addSuccessMessage("Saved Successfully");
        //    recreateModel();

    }

    public void saveSelectedCategoryPaymentMethod(PriceMatrix a) {

        if (category == null && item == null) {
            UtilityController.addErrorMessage("Please select a department");
            return;
        }

        if (paymentMethod == null) {
            UtilityController.addErrorMessage("Please select Payment Method");
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
        a.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        a.setCreater(getSessionController().getLoggedUser());
        getFacade().create(a);
        UtilityController.addSuccessMessage("Saved Successfully");
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

    public void deleteDepartmentPaymentMethod() {
        deleteDepartment();
        createItemsDepartmentsPaymentMethod();
    }

    public void deleteDepartment() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        //    recreateModel();

        current = null;
        getCurrent();
        filterItems = null;
        createItemsDepartments();
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
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
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

    public void createItemsDepartments() {
        filterItems = null;
        String sql;
        sql = "select a from OpdMemberShipDiscount a "
                + " where a.retired=false "
                + " and a.category is null"
                + " order by a.membershipScheme.name,a.department.name";
        items = getFacade().findBySQL(sql);
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
        items = getFacade().findBySQL(sql, hm);
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
        items = getFacade().findBySQL(sql, hm);
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
        items = getFacade().findBySQL(sql, temMap);
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
        items = getFacade().findBySQL(sql, temMap);
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
        items = getFacade().findBySQL(sql, temMap);
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
        items = getFacade().findBySQL(sql, temMap);
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
        items = getFacade().findBySQL(sql, temMap);
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
        items = getFacade().findBySQL(sql, temMap);
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
        items = getFacade().findBySQL(sql, temMap);
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
        items = getFacade().findBySQL(sql, temMap);
    }

    public void onEdit(PriceMatrix tmp) {
        //Cheking Minus Value && Null
        getFacade().edit(tmp);

        //  createItems();
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

}
