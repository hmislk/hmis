/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import java.util.TimeZone;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.entity.inward.PatientRoom;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
public class PatientRoomController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PatientRoomFacade ejbFacade;
    List<PatientRoom> selectedItems;
    private PatientRoom current;
    private List<PatientRoom> items = null;
    String selectText = "";

    public void edit(PatientRoom patientRoom) {
        getFacade().edit(patientRoom);
    }

    public List<PatientRoom> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from PatientRoom c where c.retired=false and i.patientRoomType = com.divudi.data.PatientRoomType.Pharmacy and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new PatientRoom();

    }

    // Need new Enum PatientRoom type
    public void setSelectedItems(List<PatientRoom> selectedItems) {
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

    public PatientRoomFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PatientRoomFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientRoomController() {
    }

    public PatientRoom getCurrent() {
        if (current == null) {
            current = new PatientRoom();

        }
        return current;
    }

    public void setCurrent(PatientRoom current) {
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

    private PatientRoomFacade getFacade() {
        return ejbFacade;
    }

    public List<PatientRoom> getItems() {
        // items = getFacade().findAll("name", true);
        String sql = "SELECT i FROM PatientRoom i where i.retired=false order by i.name";
        items = getEjbFacade().findBySQL(sql);
        if (items == null) {
            items = new ArrayList<PatientRoom>();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = PatientRoom.class)
    public static class PatientRoomControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientRoomController controller = (PatientRoomController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientRoomController");
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
            if (object instanceof PatientRoom) {
                PatientRoom o = (PatientRoom) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientRoomController.class.getName());
            }
        }
    }
}
