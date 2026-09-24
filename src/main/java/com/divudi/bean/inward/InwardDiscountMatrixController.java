/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.PharmaceuticalItemCategoryController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.InwardDiscountMatrix;
import com.divudi.core.entity.lab.InvestigationCategory;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.ServiceSubCategory;
import com.divudi.core.facade.PriceMatrixFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.faces.convert.Converter;

/**
 * Controller for the Inward Discount Matrix configuration pages.
 *
 * Manages discount percentage entries keyed by department, category, BHT type
 * (paymentMethod), admission type, and discount scheme (paymentScheme).
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class InwardDiscountMatrixController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;

    @EJB
    private PriceMatrixFacade ejbFacade;

    private PriceMatrix current;
    private List<PriceMatrix> items;
    private List<PriceMatrix> filterItems;

    private Department department;
    private Category category;
    private List<Category> categories;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private PaymentScheme paymentScheme;
    private double discountPercent;
    private InwardChargeType inwardChargeType;
    private Institution creditCompany;

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------
    public String navigateToDiscountMatrixServiceInvestigation() {
        prepareAdd();
        return "/inward/inward_discount_matrix_service_investigation?faces-redirect=true";
    }

    public String navigateToDiscountMatrixPharmacy() {
        prepareAdd();
        return "/inward/inward_discount_matrix_pharmacy?faces-redirect=true";
    }

    public String navigateToDiscountMatrixRoomCharges() {
        prepareAdd();
        return "/inward/inward_discount_matrix_room_charges?faces-redirect=true";
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    public void prepareAdd() {
        department = null;
        category = null;
        categories = null;
        admissionType = null;
        paymentMethod = null;
        paymentScheme = null;
        discountPercent = 0.0;
        inwardChargeType = null;
        creditCompany = null;
        items = null;
        filterItems = null;
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------
    public void saveForServiceInvestigation() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a Discount Scheme");
            return;
        }
        InwardDiscountMatrix entry = buildEntry();
        ejbFacade.create(entry);
        JsfUtil.addSuccessMessage("Saved Successfully");
        loadServiceInvestigation();
        clearInputFields();
    }

    /**
     * Adds one pharmacy discount row per selected category (issue #24029).
     * Active rows with an identical combination are skipped rather than
     * duplicated. The scheme, department, admission type, BHT type, credit
     * company and percentage stay filled in so further categories can be
     * added without re-entering them; only the category selection is cleared.
     */
    public void saveForPharmacy() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a Discount Scheme");
            return;
        }

        if (categories == null || categories.isEmpty()) {
            JsfUtil.addErrorMessage("Please select at least one category");
            return;
        }

        if (discountPercent < 0.0 || discountPercent > 100.0) {
            JsfUtil.addErrorMessage("Discount % must be between 0 and 100");
            return;
        }

        int added = 0;
        int skipped = 0;
        for (Category c : categories) {
            if (c == null) {
                continue;
            }
            if (findActiveDuplicate(department, c, admissionType, paymentMethod, paymentScheme, creditCompany) != null) {
                skipped++;
                continue;
            }
            InwardDiscountMatrix entry = buildEntry();
            entry.setCategory(c);
            ejbFacade.create(entry);
            added++;
        }

        if (added > 0) {
            JsfUtil.addSuccessMessage(added + " added" + (skipped > 0 ? ", " + skipped + " skipped (already exist)" : ""));
        } else {
            JsfUtil.addErrorMessage("Nothing added - all " + skipped + " selected categories already have this discount");
        }

        categories = null;
        loadPharmacy();
    }

    /**
     * Finds an active category-level discount row with exactly this
     * combination. Null arguments match only null columns, so a wildcard row
     * and a specific row are not duplicates of each other. Same criteria as
     * {@code InwardDiscountMatrixApi.findDuplicate}.
     */
    private InwardDiscountMatrix findActiveDuplicate(Department dep, Category cat, AdmissionType at,
            PaymentMethod pm, PaymentScheme ps, Institution cc) {
        StringBuilder jpql = new StringBuilder("select a from InwardDiscountMatrix a"
                + " where a.retired = false and a.inwardChargeType is null");
        Map<String, Object> params = new HashMap<>();
        appendNullSafe(jpql, params, "a.department", "dep", dep);
        appendNullSafe(jpql, params, "a.category", "cat", cat);
        appendNullSafe(jpql, params, "a.admissionType", "at", at);
        appendNullSafe(jpql, params, "a.paymentMethod", "pm", pm);
        appendNullSafe(jpql, params, "a.paymentScheme", "ps", ps);
        appendNullSafe(jpql, params, "a.creditCompany", "cc", cc);
        return (InwardDiscountMatrix) ejbFacade.findFirstByJpql(jpql.toString(), params);
    }

    private void appendNullSafe(StringBuilder jpql, Map<String, Object> params, String path, String name, Object value) {
        if (value == null) {
            jpql.append(" and ").append(path).append(" is null");
        } else {
            jpql.append(" and ").append(path).append(" = :").append(name);
            params.put(name, value);
        }
    }

    public void saveForRoomCharges() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a Discount Scheme");
            return;
        }

        if (inwardChargeType == null) {
            JsfUtil.addErrorMessage("Please select a Room Charge Type");
            return;
        }
        InwardDiscountMatrix entry = buildEntry();
        entry.setInwardChargeType(inwardChargeType);
        ejbFacade.create(entry);
        JsfUtil.addSuccessMessage("Saved Successfully");
        loadRoomCharges();
        clearInputFields();
    }

    private InwardDiscountMatrix buildEntry() {
        InwardDiscountMatrix entry = new InwardDiscountMatrix();
        entry.setDepartment(department);
        entry.setCategory(category);
        entry.setAdmissionType(admissionType);
        entry.setPaymentMethod(paymentMethod);
        entry.setPaymentScheme(paymentScheme);
        entry.setDiscountPercent(discountPercent);
        entry.setCreditCompany(creditCompany);
        if (department != null) {
            entry.setInstitution(department.getInstitution());
        }
        entry.setCreatedAt(new Date());
        entry.setCreater(sessionController.getLoggedUser());
        return entry;
    }

    private void clearInputFields() {
        department = null;
        category = null;
        admissionType = null;
        paymentMethod = null;
        paymentScheme = null;
        discountPercent = 0.0;
        inwardChargeType = null;
        creditCompany = null;
    }

    // -------------------------------------------------------------------------
    // Load / Fill
    // -------------------------------------------------------------------------
    public void loadServiceInvestigation() {
        filterItems = null;
        HashMap<String, Object> hm = new HashMap<>();
        String sql = "select a from InwardDiscountMatrix a"
                + " left join a.paymentScheme ps"
                + " left join a.department dept"
                + " left join a.category cat"
                + " where a.retired = false"
                + " and a.inwardChargeType is null"
                + " and (type(a.category) = :svc"
                + "   or type(a.category) = :sub"
                + "   or type(a.category) = :inv"
                + "   or a.category is null)"
                + " order by ps.name, dept.name, cat.name";
        hm.put("svc", ServiceCategory.class);
        hm.put("sub", ServiceSubCategory.class);
        hm.put("inv", InvestigationCategory.class);
        items = ejbFacade.findByJpql(sql, hm);
    }

    /**
     * Lists pharmacy discount rows. Any selection made in the entry form
     * (scheme, department, categories, admission type, BHT type, credit
     * company) narrows the list; unselected fields do not filter.
     */
    public void loadPharmacy() {
        filterItems = null;
        Map<String, Object> hm = new HashMap<>();
        StringBuilder sql = new StringBuilder("select a from InwardDiscountMatrix a"
                + " left join a.paymentScheme ps"
                + " left join a.department dept"
                + " left join a.category cat"
                + " where a.retired = false"
                + " and a.inwardChargeType is null"
                + " and (type(a.category) = :pharm"
                + "   or a.category is null)");
        hm.put("pharm", PharmaceuticalItemCategory.class);
        if (paymentScheme != null) {
            sql.append(" and a.paymentScheme = :ps");
            hm.put("ps", paymentScheme);
        }
        if (department != null) {
            sql.append(" and a.department = :dep");
            hm.put("dep", department);
        }
        if (categories != null && !categories.isEmpty()) {
            sql.append(" and a.category in :cats");
            hm.put("cats", new ArrayList<>(categories));
        }
        if (admissionType != null) {
            sql.append(" and a.admissionType = :at");
            hm.put("at", admissionType);
        }
        if (paymentMethod != null) {
            sql.append(" and a.paymentMethod = :pm");
            hm.put("pm", paymentMethod);
        }
        if (creditCompany != null) {
            sql.append(" and a.creditCompany = :cc");
            hm.put("cc", creditCompany);
        }
        sql.append(" order by ps.name, dept.name, cat.name");
        items = ejbFacade.findByJpql(sql.toString(), hm);
    }

    public void loadRoomCharges() {
        filterItems = null;
        String sql = "select a from InwardDiscountMatrix a"
                + " where a.retired = false"
                + " and a.inwardChargeType is not null"
                + " order by a.inwardChargeType";
        items = ejbFacade.findByJpql(sql);
    }

    // -------------------------------------------------------------------------
    // Edit / Delete
    // -------------------------------------------------------------------------
    public void onEdit(PriceMatrix entry) {
        ejbFacade.edit(entry);
        JsfUtil.addSuccessMessage("Updated Successfully");
    }

    public void delete() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return;
        }
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(sessionController.getLoggedUser());
        ejbFacade.edit(current);
        JsfUtil.addSuccessMessage("Deleted Successfully");
        items = null;
        current = null;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------
    public PriceMatrix getCurrent() {
        return current;
    }

    public void setCurrent(PriceMatrix current) {
        this.current = current;
    }

    public List<PriceMatrix> getItems() {
        return items;
    }

    public void setItems(List<PriceMatrix> items) {
        this.items = items;
    }

    public List<PriceMatrix> getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(List<PriceMatrix> filterItems) {
        this.filterItems = filterItems;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Converter for the multi-select category list. JSF cannot infer one for a
     * {@code List<Category>} value (generic type is erased), so the page names
     * it explicitly.
     */
    public Converter getPharmaceuticalCategoryConverter() {
        return new PharmaceuticalItemCategoryController.PharmaceuticalItemCategoryControllerConverter();
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }
}
