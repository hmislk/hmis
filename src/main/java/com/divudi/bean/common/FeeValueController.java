/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 *  Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.FeeValue;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.FeeValueFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class FeeValueController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private FeeValueFacade ejbFacade;

    private List<FeeValue> items;

    public void fillItems() {
        items = getFacade().findAll();
    }

    public void save(FeeValue feeValue) {
        if (feeValue == null) {
            return;
        }
        if (feeValue.getId() != null) {
            getFacade().edit(feeValue);
        } else {
            feeValue.setCreatedAt(new Date());
            feeValue.setCreater(getSessionController().getLoggedUser());
            getFacade().create(feeValue);
        }
    }

    public Double getFeeForLocals(Service item, Institution institution) {
        return getFeeForLocals((Item) item, institution);  // Calls the original method for Item
    }

    public Double getFeeForForeigners(Service item, Institution institution) {
        return getFeeForForeigners((Item) item, institution);  // Calls the original method for Item
    }

    public Double getFeeForLocals(Investigation item, Institution institution) {
        return getFeeForLocals((Item) item, institution);  // Calls the original method for Item
    }

    public Double getFeeForForeigners(Investigation item, Institution institution) {
        return getFeeForForeigners((Item) item, institution);  // Calls the original method for Item
    }

    public Double getFeeForLocals(Item item, Institution institution) {
        FeeValue feeValue = getFeeValue(item, institution);
        return feeValue != null ? feeValue.getTotalValueForLocals() : 0.0;
    }

    public Double getFeeForLocals(Item item, Department department) {
        FeeValue feeValue = getFeeValue(item, department);
        return feeValue != null ? feeValue.getTotalValueForLocals() : 0.0;
    }

    public Double getFeeForForeigners(Item item, Institution institution) {
        FeeValue feeValue = getFeeValue(item, institution);
        return feeValue != null ? feeValue.getTotalValueForForeigners() : 0.0;
    }

    public Double getFeeForForeigners(Item item, Department department) {
        if (item == null || department == null) {
            return 0.0;
        }
        FeeValue feeValue = getFeeValue(item, department);
        if (feeValue == null || feeValue.getTotalValueForForeigners() == null) {
            return 0.0;
        }
        return feeValue.getTotalValueForForeigners();
    }

    public FeeValue getFeeValue(Item item, Department department) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item = :item AND f.department = :department";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("department", department);

        return getFacade().findFirstByJpql(jpql, params);
    }

    public FeeValue getFeeValue(Item item, Institution institution) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item = :item AND f.institution = :institution";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("institution", institution);
        return getFacade().findFirstByJpql(jpql, params);
    }

    public FeeValue getFeeValue(Item item, Category category) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item = :item AND f.category = :category";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("category", category);

        return getFacade().findFirstByJpql(jpql, params);
    }

    public FeeValue getFeeValue(Long itemId, Category category) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item.id = :iid AND f.category = :category";
        Map<String, Object> params = new HashMap<>();
        params.put("iid", itemId);
        params.put("category", category);

        return getFacade().findFirstByJpql(jpql, params);
    }

    public FeeValue getCollectingCentreFeeValue(Long itemId, Institution collectingCentre) {

        String jpql = "SELECT f "
                + " FROM FeeValue f "
                + " WHERE f.item.id=:iid "
                + " AND f.institution.id=:collectingCentre";
        Map<String, Object> params = new HashMap<>();
        params.put("iid", itemId);
        params.put("collectingCentre", collectingCentre.getId());

        FeeValue fv = getFacade().findFirstByJpql(jpql, params);

        if (fv != null) {
            return fv;
        }

        jpql = "SELECT f "
                + " FROM FeeValue f "
                + " WHERE f.item.id = :iid "
                + " AND f.totalValueForLocals > 0 "
                + " AND f.retired=:ret "
                + " AND f.category.id=:category";
        params = new HashMap<>();
        params.put("iid", itemId);
        params.put("ret", false);
        params.put("category", collectingCentre.getFeeListType().getId());

        fv = getFacade().findFirstByJpql(jpql, params);

        return fv;
    }

    public FeeValue getSiteFeeValue(Long itemId, Institution site) {

        String jpql = "SELECT f "
                + " FROM FeeValue f "
                + " WHERE f.item.id = :iid "
                + " AND f.totalValueForLocals > 0 "
                + " AND f.retired=:ret "
                + " AND f.institution = :site";
        Map<String, Object> params = new HashMap<>();
        params.put("iid", itemId);
        params.put("ret", false);
        params.put("site", site);

        FeeValue fv = getFacade().findFirstByJpql(jpql, params);

        if (fv != null) {
            return fv;
        }

        jpql = "SELECT f "
                + " FROM FeeValue f "
                + " WHERE f.item.id = :iid "
                + " AND f.totalValueForLocals > 0 "
                + " AND f.retired=:ret "
                + " AND f.category is null"
                + " AND f.department is null"
                + " AND f.institution is null";
        params = new HashMap<>();
        params.put("iid", itemId);
        params.put("ret", false);

        fv = getFacade().findFirstByJpql(jpql, params);

        return fv;
    }
    
    public FeeValue getDepartmentFeeValue(Long itemId, Department dept) {
        String jpql = "SELECT f "
                + " FROM FeeValue f "
                + " WHERE f.item.id = :iid "
                + " AND f.totalValueForLocals > 0 "
                + " AND f.retired=:ret "
                + " AND f.department = :dept";
        Map<String, Object> params = new HashMap<>();
        params.put("iid", itemId);
        params.put("ret", false);
        params.put("dept", dept);
        FeeValue fv = getFacade().findFirstByJpql(jpql, params);
        if (fv != null) {
            return fv;
        }
        jpql = "SELECT f "
                + " FROM FeeValue f "
                + " WHERE f.item.id = :iid "
                + " AND f.totalValueForLocals > 0 "
                + " AND f.retired=:ret "
                + " AND f.category is null"
                + " AND f.department is null"
                + " AND f.institution is null";
        params = new HashMap<>();
        params.put("iid", itemId);
        params.put("ret", false);

        fv = getFacade().findFirstByJpql(jpql, params);

        return fv;
    }

    public FeeValue getFeeValue(Item item, Department dept, Institution ins, Category category) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item = :item AND f.department = :dept AND f.institution = :ins AND f.category = :category";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("dept", dept);
        params.put("ins", ins);
        params.put("category", category);

        FeeValue fvals = getFacade().findFirstByJpql(jpql, params);
        if (fvals == null) {
            fvals = new FeeValue();
            fvals.setItem(item);
            fvals.setDepartment(dept);
            fvals.setInstitution(ins);
            fvals.setCategory(category);
        }
        return fvals;
    }

    public void updateFeeValue(Item item, Department dept, Double feeValueForLocals, Double feeValueForForeigners) {
        FeeValue feeValue = getFeeValue(item, dept);
        if (feeValue == null) {
            feeValue = new FeeValue();
            feeValue.setItem(item);
            feeValue.setDepartment(dept);
            feeValue.setCreatedAt(new Date());
            feeValue.setCreater(getSessionController().getLoggedUser());
        }
        feeValue.setTotalValueForLocals(feeValueForLocals);
        feeValue.setTotalValueForForeigners(feeValueForForeigners);
        save(feeValue);
    }

    public void updateFeeValue(Item item, Institution ins, Double feeValueForLocals, Double feeValueForForeigners) {
        FeeValue feeValue = getFeeValue(item, ins);
        if (feeValue == null) {
            feeValue = new FeeValue();
            feeValue.setItem(item);
            feeValue.setInstitution(ins);
            feeValue.setCreatedAt(new Date());
            feeValue.setCreater(getSessionController().getLoggedUser());
        }
        feeValue.setTotalValueForLocals(feeValueForLocals);
        feeValue.setTotalValueForForeigners(feeValueForForeigners);
        save(feeValue);
    }

    public Double getFeeValueForLocals(Item item, Department dept, Institution ins, Category category) {
        FeeValue feeValue = getFeeValue(item, dept, ins, category);
        if (feeValue != null) {
            return feeValue.getTotalValueForLocals(); // Assuming `getFee()` returns the fee for locals
        }
        return null; // Or return 0.0 if you prefer to return a default value
    }

    public Double getFeeValueForLocalsByItemIdForLoggedSite(Long id) {
        String jpql = "SELECT f.totalValueForLocals "
                + " FROM FeeValue f "
                + " WHERE f.item.id = :id "
                + " AND f.institution = :institution";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("institution", sessionController.getDepartment().getSite());
        return getFacade().findDoubleByJpql(jpql, params);
    }

    public Double getFeeValueForForeigners(Item item, Department dept, Institution ins, Category category) {
        FeeValue feeValue = getFeeValue(item, dept, ins, category);
        if (feeValue != null) {
            return feeValue.getTotalValueForForeigners(); // Assuming `getFfee()` returns the fee for foreigners
        }
        return null; // Or return 0.0 if you prefer to return a default value
    }

    public void updateFeeValue(Item item, Category cat, Double feeValueForLocals, Double feeValueForForeigners) {
        FeeValue feeValue = getFeeValue(item, cat);
        if (feeValue == null) {
            feeValue = new FeeValue();
            feeValue.setItem(item);
            feeValue.setCategory(cat);
            feeValue.setCreatedAt(new Date());
            feeValue.setCreater(getSessionController().getLoggedUser());
        }
        feeValue.setTotalValueForLocals(feeValueForLocals);
        feeValue.setTotalValueForForeigners(feeValueForForeigners);
        save(feeValue);
    }

    public FeeValueFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public FeeValueController() {
    }

    private FeeValueFacade getFacade() {
        return ejbFacade;
    }

    public List<FeeValue> getItems() {
        return items;
    }

    public void setItems(List<FeeValue> items) {
        this.items = items;
    }

    @FacesConverter(forClass = FeeValue.class)
    public static class FeeValueConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            FeeValueController controller = (FeeValueController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "feeValueController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            return Long.valueOf(value);
        }

        String getStringKey(java.lang.Long value) {
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof FeeValue) {
                FeeValue o = (FeeValue) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + FeeValue.class.getName());
            }
        }
    }
}
