/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.InvestigationItemType;
import java.util.TimeZone;
import com.divudi.data.ReportItemType;
import com.divudi.entity.Category;
import com.divudi.facade.CommonReportItemFacade;
import com.divudi.entity.lab.CommonReportItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class CommonReportItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private CommonReportItemFacade ejbFacade;
    List<CommonReportItem> selectedItems;
    private CommonReportItem current;
    private List<CommonReportItem> items = null;
    String selectText = "";
    Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        ////System.out.println("Common Report Format Category is " + category);
        items = null;
    }

    public void addNewLabel() {
        current = new CommonReportItem();
        current.setName("New Label");
        current.setCategory(category);
        getEjbFacade().create(current);
    }

    public void addNewCombo() {
        current = new CommonReportItem();
        current.setName("New Combo");
        current.setCategory(category);
        current.setIxItemType(InvestigationItemType.ItemsCatetgory);
        getEjbFacade().create(current);
    }

    public CommonReportItemController() {
    }

    public void removeItem() {
        current.setRetired(true);
        current.setRetirer(getSessionController().getLoggedUser());
        current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getEjbFacade().edit(getCurrent());
        getItems().remove(getCurrent());

    }

    public void addNewValue() {
        current = new CommonReportItem();
        current.setName("New Value");
        current.setReportItemType(ReportItemType.PatientName);
        current.setCategory(category);
        getEjbFacade().create(current);
    }

    public void prepareAdd() {
        current = new CommonReportItem();
    }

    public void setSelectedItems(List<CommonReportItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        current = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("Saved Successfully");
        }
//        recreateModel();
//        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public CommonReportItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(CommonReportItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public CommonReportItem getCurrent() {
        if (current == null) {
            current = new CommonReportItem();
        }
        return current;
    }

    public void setCurrent(CommonReportItem current) {
        this.current = current;
    }

    private CommonReportItemFacade getFacade() {
        return ejbFacade;
    }

    public void addAllToCat() {
        List<CommonReportItem> is = getFacade().findAll();
        for (CommonReportItem ci : is) {
            ci.setCategory(category);
            getFacade().edit(ci);
        }
    }

    public List<CommonReportItem> getItems() {
        if (items != null) {
            return items;
        }
        String temSql;
        if (category != null) {
            temSql = "SELECT i FROM CommonReportItem i where i.retired=false and i.category=:cat order by i.name";
            Map m = new HashMap();
            m.put("cat", category);
            ////System.out.println("common report cat sql is " + temSql + " and " + m.toString());
            items = getFacade().findBySQL(temSql, m);
        } else {
            items = new ArrayList<>();
        }
        return items;
    }

    public List<CommonReportItem> getCategoryItems(Category cat) {
        List<CommonReportItem> cis;
        String temSql;
        if (cat != null) {
            temSql = "SELECT i FROM CommonReportItem i where i.retired=false and i.category=:cat order by i.name";
            Map m = new HashMap();
            m.put("cat", cat);
            ////System.out.println("common report cat sql is " + temSql + " and " + m.toString());
            cis = getFacade().findBySQL(temSql, m);
        } else {
            cis = new ArrayList<>();
        }
        return cis;
    }

    /**
     *
     */
    @FacesConverter(forClass = CommonReportItem.class)
    public static class CommonReportItemControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CommonReportItemController controller = (CommonReportItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "commonReportItemController");
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
            if (object instanceof CommonReportItem) {
                CommonReportItem o = (CommonReportItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + CommonReportItemController.class.getName());
            }
        }
    }
}
