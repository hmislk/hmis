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
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.facade.InvestigationItemValueFacade;
import com.divudi.entity.lab.InvestigationItemValue;
import com.divudi.entity.lab.PatientReportItemValue;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
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
public class InvestigationItemValueController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private InvestigationItemValueFacade ejbFacade;
    List<InvestigationItemValue> selectedItems;
    private InvestigationItemValue current;
    private List<InvestigationItemValue> items = null;
    String selectText = "";

    public List<InvestigationItemValue> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from InvestigationItemValue c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public List<String> completeValues(String qry) {
        ////System.out.println("completing values");
        FacesContext context = FacesContext.getCurrentInstance();
        InvestigationItem ii;
        ////System.out.println("1");
        ////System.out.println(context
//                .getApplication()
//                .evaluateExpressionGet(FacesContext.getCurrentInstance(),
//                        "#{pv}", PatientReportItemValue.class));
        ////System.out.println("2");
        try {
            ////System.out.println("3");
            PatientReportItemValue priv = (PatientReportItemValue) context
                    .getApplication()
                    .evaluateExpressionGet(FacesContext.getCurrentInstance(),
                            "#{pvm}", PatientReportItemValue.class);
            ////System.out.println("4");
            ii = priv.getInvestigationItem();
            ////System.out.println("5");
            ////System.out.println("ii = " + ii);
        } catch (Exception e) {
            ii = null;
            ////System.out.println("error " + e.getMessage());
            ////System.out.println("6");
        }
        ////System.out.println("7");
        Map m = new HashMap();
        String sql;
        sql = "select v.name from InvestigationItemValue v "
                + "where v.investigationItem=:ii and v.retired=false and"
                + " (upper(v.code) like :s or upper(v.name) like :s) order by v.name";
        m.put("s", "%" + qry.toUpperCase() + "%");
        m.put("ii", ii);
        List<String> sls = getFacade().findString(sql, m);
        ////System.out.println("m = " + m);
        ////System.out.println("sql = " + sql);
        ////System.out.println("sls = " + sls);
        return sls;
    }

    public void prepareAdd() {
        current = new InvestigationItemValue();
    }

    public void setSelectedItems(List<InvestigationItemValue> selectedItems) {
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
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public InvestigationItemValueFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationItemValueFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InvestigationItemValueController() {
    }

    public InvestigationItemValue getCurrent() {
        return current;
    }

    public void setCurrent(InvestigationItemValue current) {
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

    private InvestigationItemValueFacade getFacade() {
        return ejbFacade;
    }

    public List<InvestigationItemValue> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = InvestigationItemValue.class)
    public static class InvestigationItemValueControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemValueController controller = (InvestigationItemValueController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationItemValueController");
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
            if (object instanceof InvestigationItemValue) {
                InvestigationItemValue o = (InvestigationItemValue) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationItemValueController.class.getName());
            }
        }
    }

    @FacesConverter("iivcon")
    public static class InvestigationItemValueConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemValueController controller = (InvestigationItemValueController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationItemValueController");
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
            if (object instanceof InvestigationItemValue) {
                InvestigationItemValue o = (InvestigationItemValue) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationItemValueController.class.getName());
            }
        }
    }

}
