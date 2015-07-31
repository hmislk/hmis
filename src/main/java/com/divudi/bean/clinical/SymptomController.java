/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.SymanticType;
import com.divudi.facade.ClinicalFindingItemFacade;
import com.divudi.entity.clinical.ClinicalFindingItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class SymptomController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ClinicalFindingItemFacade ejbFacade;
    List<ClinicalFindingItem> selectedItems;
    private ClinicalFindingItem current;
    private List<ClinicalFindingItem> items = null;
    String selectText = "";

    public List<ClinicalFindingItem> completeDiagnosis(String qry) {
        List<ClinicalFindingItem> c;
        Map m = new HashMap();
        m.put("t", SymanticType.Symptom);
        m.put("n", "%" + qry.toUpperCase() + "%");
        String sql;
        sql = "select c from ClinicalFindingItem c where c.retired=false and upper(c.name) like :n and c.symanticType=:t order by c.name";
        c = getFacade().findBySQL(sql, m, 10);
        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<ClinicalFindingItem> getSelectedItems() {
        Map m = new HashMap();
        m.put("t", SymanticType.Symptom);
        m.put("n", "%" + getSelectText().toUpperCase() + "%");
        String sql;
        sql = "select c from ClinicalFindingItem c where c.retired=false and upper(c.name) like :n and c.symanticType=:t order by c.name";
        selectedItems = getFacade().findBySQL(sql, m);
        return selectedItems;
    }

    public void prepareAdd() {
        current = new ClinicalFindingItem();
        current.setSymanticType(SymanticType.Symptom);
        //TODO:
    }

    public void setSelectedItems(List<ClinicalFindingItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        current.setSymanticType(SymanticType.Symptom);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Saved");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Updates");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public ClinicalFindingItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ClinicalFindingItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SymptomController() {
    }

    public ClinicalFindingItem getCurrent() {
        if (current == null) {
            current = new ClinicalFindingItem();
        }
        return current;
    }

    public void setCurrent(ClinicalFindingItem current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private ClinicalFindingItemFacade getFacade() {
        return ejbFacade;
    }

    public List<ClinicalFindingItem> getItems() {
        if (items == null) {
            Map m = new HashMap();
            m.put("t", SymanticType.Symptom);
            String sql;
            sql = "select c from ClinicalFindingItem c where c.retired=false and c.symanticType=:t order by c.name";
            items = getFacade().findBySQL(sql, m);
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter("symptomConverter")
    public static class SymptomConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SymptomController controller = (SymptomController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "symptomController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof ClinicalFindingItem) {
                ClinicalFindingItem o = (ClinicalFindingItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SymptomController.class.getName());
            }
        }
    }
}
