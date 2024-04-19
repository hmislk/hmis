/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.SymanticType;
import com.divudi.entity.clinical.ClinicalEntity;
import com.divudi.facade.ClinicalEntityFacade;
import java.io.Serializable;
import java.util.ArrayList;
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
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ClinicalEntityController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ClinicalEntityFacade ejbFacade;
    List<ClinicalEntity> selectedItems;
    private ClinicalEntity current;
    private List<ClinicalEntity> items = null;
    String selectText = "";

    public String navigateToManageClinicalEntities() {
        return "/emr/admin/clinical_entities";
    }

    public List<ClinicalEntity> listClinicalEntity(SymanticType type) {
        List<ClinicalEntity> c;
        Map m = new HashMap();
        m.put("t", type);
        String sql;
        sql = "select c from ClinicalEntity c where c.retired=false and c.symanticType=:t order by c.name";
        c = getFacade().findByJpql(sql, m, 10);
        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<ClinicalEntity> getRaces() {
        return listClinicalEntity(SymanticType.Race);
    }

    public List<ClinicalEntity> getEhnicity() {
        return listClinicalEntity(SymanticType.Religion);
    }

    public List<ClinicalEntity> getOccupation() {
        return listClinicalEntity(SymanticType.Employment);
    }

    public List<ClinicalEntity> getBloodGroup() {
        return listClinicalEntity(SymanticType.Blood_Group);
    }
    
    public List<ClinicalEntity> getCivilStatus() {
        return listClinicalEntity(SymanticType.Civil_status);
    }
    
     public List<ClinicalEntity> getRelationships() {
        return listClinicalEntity(SymanticType.Relationships);
    }

    public List<ClinicalEntity> getSelectedItems() {
        Map m = new HashMap();
        m.put("n", "%" + getSelectText().toUpperCase() + "%");
        String sql;
        sql = "select c from ClinicalEntity c where c.retired=false and (c.name) like :n order by c.name";
        selectedItems = getFacade().findByJpql(sql, m);
        return selectedItems;
    }

    public void prepareAdd() {
        current = new ClinicalEntity();
        //TODO:
    }

    public void setSelectedItems(List<ClinicalEntity> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Saved");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Updated");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public ClinicalEntityFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ClinicalEntityFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ClinicalEntityController() {
    }

    public ClinicalEntity getCurrent() {
        if (current == null) {
            current = new ClinicalEntity();
        }
        return current;
    }

    public void setCurrent(ClinicalEntity current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private ClinicalEntityFacade getFacade() {
        return ejbFacade;
    }

    public List<ClinicalEntity> getItems() {
        if (items == null) {
            Map m = new HashMap();
            String sql;
            sql = "select c from ClinicalEntity c where c.retired=false order by c.name";
            items = getFacade().findByJpql(sql, m);
        }
        return items;

    }

    /**
     *
     */
    @FacesConverter(forClass = ClinicalEntity.class)
    public static class ClinicalEntityConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ClinicalEntityController controller = (ClinicalEntityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "clinicalEntityController");
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
            if (object instanceof ClinicalEntity) {
                ClinicalEntity o = (ClinicalEntity) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ClinicalEntity.class.getName());
            }
        }
    }
}
