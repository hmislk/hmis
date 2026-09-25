/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.CategoryController;
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
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.lab.InvestigationCategory;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.ServiceSubCategory;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.PriceMatrixFacade;
import com.divudi.service.inward.DiscountSetupService;
import com.divudi.service.inward.DiscountSetupService.ItemScope;
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
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private DiscountSetupService discountSetupService;

    private PriceMatrix current;
    private List<PriceMatrix> items;
    private List<PriceMatrix> filterItems;

    private Department department;
    private Category category;
    private List<Category> categories;
    private List<Category> serviceInvestigationCategories;
    private List<String> roomChargeTypeNames;
    private List<Category> roomCategories;
    private List<Category> allRoomCategories;
    private List<String> setupScopeNames;
    private Long legacyCategoryCount;
    private Map<String, Long> discountNotAllowedCounts;
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
        serviceInvestigationCategories = null;
        roomChargeTypeNames = null;
        roomCategories = null;
        allRoomCategories = null;
        legacyCategoryCount = null;
        discountNotAllowedCounts = null;
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
    /**
     * Adds one service/investigation discount row per selected category
     * (issue #24038). Same behaviour as {@link #saveForPharmacy()}.
     */
    public void saveForServiceInvestigation() {
        saveCategoryRows(this::loadServiceInvestigation);
    }

    /**
     * Adds one pharmacy discount row per selected category (issue #24029).
     * Same behaviour as {@link #saveForServiceInvestigation()}.
     */
    public void saveForPharmacy() {
        saveCategoryRows(this::loadPharmacy);
    }

    /**
     * Creates one row per selected category. Active rows with an identical
     * combination are skipped rather than duplicated. The scheme, department,
     * admission type, BHT type, credit company and percentage stay filled in so
     * further categories can be added without re-entering them; only the
     * category selection is cleared. A blank category is not allowed here: a
     * scheme row with no category is the catch-all fallback for both pharmacy
     * and services, so it would leak across the two matrices.
     */
    private void saveCategoryRows(Runnable reload) {
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
        reload.run();
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

    /**
     * Adds one room-charge discount row per selected charge type and room
     * category (issue #24011). With no room category selected, one row per
     * charge type applies to all rooms. Active identical rows are skipped;
     * the scheme and other fields stay filled in, only the charge type and
     * room category selections are cleared.
     */
    public void saveForRoomCharges() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a Discount Scheme");
            return;
        }
        List<InwardChargeType> types = getSelectedRoomChargeTypes();
        if (types.isEmpty()) {
            JsfUtil.addErrorMessage("Please select at least one Room Charge Type");
            return;
        }
        if (discountPercent < 0.0 || discountPercent > 100.0) {
            JsfUtil.addErrorMessage("Discount % must be between 0 and 100");
            return;
        }
        List<Category> rcs = new ArrayList<>();
        if (roomCategories == null || roomCategories.isEmpty()) {
            rcs.add(null);
        } else {
            rcs.addAll(roomCategories);
        }

        int added = 0;
        int skipped = 0;
        for (InwardChargeType t : types) {
            for (Category rc : rcs) {
                if (findActiveRoomDuplicate(t, rc) != null) {
                    skipped++;
                    continue;
                }
                InwardDiscountMatrix entry = buildEntry();
                entry.setInwardChargeType(t);
                entry.setRoomCategory(rc);
                ejbFacade.create(entry);
                added++;
            }
        }

        if (added > 0) {
            JsfUtil.addSuccessMessage(added + " added" + (skipped > 0 ? ", " + skipped + " skipped (already exist)" : ""));
        } else {
            JsfUtil.addErrorMessage("Nothing added - all " + skipped + " selected combinations already have this discount");
        }
        roomChargeTypeNames = null;
        roomCategories = null;
        loadRoomCharges();
    }

    private InwardDiscountMatrix findActiveRoomDuplicate(InwardChargeType chargeType, Category roomCategory) {
        StringBuilder jpql = new StringBuilder("select a from InwardDiscountMatrix a"
                + " where a.retired = false and a.inwardChargeType = :ict");
        Map<String, Object> params = new HashMap<>();
        params.put("ict", chargeType);
        appendNullSafe(jpql, params, "a.roomCategory", "rc", roomCategory);
        appendNullSafe(jpql, params, "a.department", "dep", department);
        appendNullSafe(jpql, params, "a.category", "cat", null);
        appendNullSafe(jpql, params, "a.admissionType", "at", admissionType);
        appendNullSafe(jpql, params, "a.paymentMethod", "pm", paymentMethod);
        appendNullSafe(jpql, params, "a.paymentScheme", "ps", paymentScheme);
        appendNullSafe(jpql, params, "a.creditCompany", "cc", creditCompany);
        return (InwardDiscountMatrix) ejbFacade.findFirstByJpql(jpql.toString(), params);
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
    /**
     * Lists service/investigation discount rows, narrowed by the selections
     * in the entry form (same rules as {@link #loadPharmacy()}).
     */
    public void loadServiceInvestigation() {
        filterItems = null;
        Map<String, Object> hm = new HashMap<>();
        StringBuilder sql = new StringBuilder("select a from InwardDiscountMatrix a"
                + " left join a.paymentScheme ps"
                + " left join a.department dept"
                + " left join a.category cat"
                + " where a.retired = false"
                + " and a.inwardChargeType is null"
                + " and (type(a.category) = :svc"
                + "   or type(a.category) = :sub"
                + "   or type(a.category) = :inv"
                + "   or a.category is null)");
        hm.put("svc", ServiceCategory.class);
        hm.put("sub", ServiceSubCategory.class);
        hm.put("inv", InvestigationCategory.class);
        appendSelectionFilters(sql, hm);
        sql.append(" order by ps.name, dept.name, cat.name");
        items = ejbFacade.findByJpql(sql.toString(), hm);
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
        appendSelectionFilters(sql, hm);
        sql.append(" order by ps.name, dept.name, cat.name");
        items = ejbFacade.findByJpql(sql.toString(), hm);
    }

    /**
     * Narrows a matrix listing by each field selected in the entry form;
     * fields left blank do not filter.
     */
    private void appendSelectionFilters(StringBuilder sql, Map<String, Object> hm) {
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
    }

    /**
     * Lists room-charge discount rows, narrowed by the scheme, charge types,
     * room categories, admission type, BHT type and credit company selected
     * in the entry form; blank fields do not filter.
     */
    public void loadRoomCharges() {
        filterItems = null;
        Map<String, Object> hm = new HashMap<>();
        StringBuilder sql = new StringBuilder("select a from InwardDiscountMatrix a"
                + " left join a.paymentScheme ps"
                + " left join a.roomCategory rcat"
                + " where a.retired = false"
                + " and a.inwardChargeType is not null");
        List<InwardChargeType> types = getSelectedRoomChargeTypes();
        if (!types.isEmpty()) {
            sql.append(" and a.inwardChargeType in :icts");
            hm.put("icts", types);
        }
        if (roomCategories != null && !roomCategories.isEmpty()) {
            sql.append(" and a.roomCategory in :rcs");
            hm.put("rcs", new ArrayList<>(roomCategories));
        }
        // The shared filters add the category condition only when categories
        // are selected, which this page never does.
        appendSelectionFilters(sql, hm);
        sql.append(" order by ps.name, a.inwardChargeType, rcat.name");
        items = ejbFacade.findByJpql(sql.toString(), hm);
    }

    // -------------------------------------------------------------------------
    // Discount setup checks (issue #24038)
    // -------------------------------------------------------------------------
    /**
     * Counts what would stop category discounts from applying: investigations
     * whose category is only in the legacy field, and items / fees in the
     * selected scopes that do not allow discounts. Read only.
     */
    public void checkDiscountSetup() {
        legacyCategoryCount = discountSetupService.countInvestigationsUsingLegacyCategory();
        discountNotAllowedCounts = getSetupScopes().isEmpty() ? null
                : discountSetupService.countDiscountNotAllowed(getSetupScopes());
    }

    public void copyLegacyInvestigationCategories() {
        int n = discountSetupService.copyLegacyInvestigationCategories(sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage(n + " investigation(s) now have their legacy category stored as Category");
        checkDiscountSetup();
        serviceInvestigationCategories = null;
    }

    public void allowDiscountsForSetupScopes() {
        if (getSetupScopes().isEmpty()) {
            JsfUtil.addErrorMessage("Select at least one item type");
            return;
        }
        Map<String, Integer> r;
        try {
            r = discountSetupService.allowDiscounts(getSetupScopes(), sessionController.getLoggedUser());
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Could not update Discount Allowed: " + e.getMessage());
            return;
        }
        JsfUtil.addSuccessMessage("Discount Allowed turned on for " + r.get("itemsUpdated") + " item(s) and "
                + r.get("feesUpdated") + " fee(s)");
        checkDiscountSetup();
    }

    public ItemScope[] getAllSetupScopes() {
        return ItemScope.values();
    }

    /**
     * Selected scopes. The page binds the names ({@link #getSetupScopeNames()})
     * because a List of enums cannot be converted by JSF (type erasure).
     */
    public List<ItemScope> getSetupScopes() {
        List<ItemScope> r = new ArrayList<>();
        for (String n : getSetupScopeNames()) {
            r.add(ItemScope.valueOf(n));
        }
        return r;
    }

    public List<String> getSetupScopeNames() {
        if (setupScopeNames == null) {
            setupScopeNames = new ArrayList<>();
            setupScopeNames.add(ItemScope.INVESTIGATION.name());
            setupScopeNames.add(ItemScope.SERVICE.name());
            setupScopeNames.add(ItemScope.INWARD_SERVICE.name());
        }
        return setupScopeNames;
    }

    public void setSetupScopeNames(List<String> setupScopeNames) {
        this.setupScopeNames = setupScopeNames;
    }

    public Long getLegacyCategoryCount() {
        return legacyCategoryCount;
    }

    public Map<String, Long> getDiscountNotAllowedCounts() {
        return discountNotAllowedCounts;
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

    /**
     * Converter for the service/investigation multi-select. The generic
     * Category converter resolves any subtype (service, sub-category,
     * investigation category).
     */
    public Converter getCategoryConverter() {
        return new CategoryController.CategoryControllerConverter();
    }

    /**
     * Active categories offered on the Services and Investigations matrix:
     * service categories, service sub-categories and investigation
     * categories, grouped by type then name.
     */
    public List<Category> getServiceInvestigationCategories() {
        if (serviceInvestigationCategories == null) {
            serviceInvestigationCategories = new ArrayList<>();
            serviceInvestigationCategories.addAll(activeCategoriesOfType(ServiceCategory.class));
            serviceInvestigationCategories.addAll(activeCategoriesOfType(ServiceSubCategory.class));
            serviceInvestigationCategories.addAll(activeCategoriesOfType(InvestigationCategory.class));
        }
        return serviceInvestigationCategories;
    }

    private List<Category> activeCategoriesOfType(Class<? extends Category> type) {
        Map<String, Object> m = new HashMap<>();
        m.put("t", type);
        return categoryFacade.findByJpql("select c from Category c where c.retired = false and type(c) = :t order by c.name", m);
    }

    /**
     * Short label for a category's kind, shown next to its name so mixed
     * service/investigation lists stay readable.
     */
    public String categoryTypeLabel(Category c) {
        if (c instanceof InvestigationCategory) {
            return "Investigation";
        }
        if (c instanceof ServiceSubCategory) {
            return "Service Sub-category";
        }
        if (c instanceof ServiceCategory) {
            return "Service";
        }
        if (c instanceof PharmaceuticalItemCategory) {
            return "Pharmaceutical";
        }
        return "";
    }

    public List<InwardChargeType> getSelectedRoomChargeTypes() {
        List<InwardChargeType> r = new ArrayList<>();
        if (roomChargeTypeNames != null) {
            for (String n : roomChargeTypeNames) {
                r.add(InwardChargeType.valueOf(n));
            }
        }
        return r;
    }

    /**
     * Bound by the page as names, because JSF cannot convert a List of enums
     * (type erasure).
     */
    public List<String> getRoomChargeTypeNames() {
        return roomChargeTypeNames;
    }

    public void setRoomChargeTypeNames(List<String> roomChargeTypeNames) {
        this.roomChargeTypeNames = roomChargeTypeNames;
    }

    public List<Category> getRoomCategories() {
        return roomCategories;
    }

    public void setRoomCategories(List<Category> roomCategories) {
        this.roomCategories = roomCategories;
    }

    public List<Category> getAllRoomCategories() {
        if (allRoomCategories == null) {
            allRoomCategories = activeCategoriesOfType(RoomCategory.class);
        }
        return allRoomCategories;
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
