/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.UtilityController;
import com.divudi.bean.common.SessionController;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.entity.hr.FingerPrintRecord;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
public class FingerPrintRecordController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private FingerPrintRecordFacade ejbFacade;
    List<FingerPrintRecord> selectedItems;
    private FingerPrintRecord current;
    private List<FingerPrintRecord> items = null;
    String selectText = "";

    public List<FingerPrintRecord> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from FingerPrintRecord c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public List<FingerPrintRecord> completeFingerPrintRecord(String qry) {
        List<FingerPrintRecord> a = null;
        if (qry != null) {
            a = getFacade().findBySQL("select c from FingerPrintRecord c where c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public void prepareAdd() {
        current = new FingerPrintRecord();
    }

    public void setSelectedItems(List<FingerPrintRecord> selectedItems) {
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
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public FingerPrintRecordFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(FingerPrintRecordFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public FingerPrintRecordController() {
    }

    public FingerPrintRecord getCurrent() {
        if (current == null) {
            current = new FingerPrintRecord();
        }
        return current;
    }

    public void setCurrent(FingerPrintRecord current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private FingerPrintRecordFacade getFacade() {
        return ejbFacade;
    }

    public List<FingerPrintRecord> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = FingerPrintRecord.class)
    public static class FingerPrintRecordConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            FingerPrintRecordController controller = (FingerPrintRecordController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "fingerPrintRecordController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0l;
            try {
                key = Long.valueOf(value);
            } catch (NumberFormatException e) {
                key = 0l;
                System.err.println(e.getMessage());
            }

            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null || object == "") {
                return null;
            }
            if (object instanceof FingerPrintRecord) {
                FingerPrintRecord o = (FingerPrintRecord) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + FingerPrintRecordController.class.getName());
            }
        }
    }

    @FacesConverter("fingerPrintRecordCon")
    public static class FingerPrintRecordControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            FingerPrintRecordController controller = (FingerPrintRecordController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "fingerPrintRecordController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0L;
//            System.err.println("Value + "+value);
            if ("".equals(value)) {
                return key;
            }

            try {
                key = Long.valueOf(value);
                return key;
            } catch (NumberFormatException e) {
                key = 0l;
//                System.err.println(e.getMessage());
            }

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

            if (object == "") {
                return null;
            }

            if (object instanceof FingerPrintRecord) {
                FingerPrintRecord o = (FingerPrintRecord) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + FingerPrintRecordController.class.getName());
            }
        }
    }
}
