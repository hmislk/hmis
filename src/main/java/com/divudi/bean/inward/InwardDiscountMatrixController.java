/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for the Inward Discount Matrix configuration pages.
 *
 * Manages discount percentage entries keyed by department, category,
 * BHT type (paymentMethod), admission type, and discount scheme (paymentScheme).
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
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private PaymentScheme paymentScheme;
    private double discountPercent;
    private InwardChargeType inwardChargeType;

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
        admissionType = null;
        paymentMethod = null;
        paymentScheme = null;
        discountPercent = 0.0;
        inwardChargeType = null;
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

    public void saveForPharmacy() {
        if (paymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a Discount Scheme");
            return;
        }
        InwardDiscountMatrix entry = buildEntry();
        ejbFacade.create(entry);
        JsfUtil.addSuccessMessage("Saved Successfully");
        loadPharmacy();
        clearInputFields();
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
    }

    // -------------------------------------------------------------------------
    // Load / Fill
    // -------------------------------------------------------------------------

    public void loadServiceInvestigation() {
        filterItems = null;
        HashMap<String, Object> hm = new HashMap<>();
        String sql = "select a from InwardDiscountMatrix a"
                + " where a.retired = false"
                + " and a.inwardChargeType is null"
                + " and (type(a.category) = :svc"
                + "   or type(a.category) = :sub"
                + "   or type(a.category) = :inv"
                + "   or a.category is null)"
                + " order by a.paymentScheme.name, a.department.name, a.category.name";
        hm.put("svc", ServiceCategory.class);
        hm.put("sub", ServiceSubCategory.class);
        hm.put("inv", InvestigationCategory.class);
        items = ejbFacade.findByJpql(sql, hm);
    }

    public void loadPharmacy() {
        filterItems = null;
        HashMap<String, Object> hm = new HashMap<>();
        String sql = "select a from InwardDiscountMatrix a"
                + " where a.retired = false"
                + " and a.inwardChargeType is null"
                + " and (type(a.category) = :pharm"
                + "   or a.category is null)"
                + " order by a.paymentScheme.name, a.department.name, a.category.name";
        hm.put("pharm", PharmaceuticalItemCategory.class);
        items = ejbFacade.findByJpql(sql, hm);
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
}
