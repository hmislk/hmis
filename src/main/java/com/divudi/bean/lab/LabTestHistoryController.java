package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.lab.LabTestHistory;
import com.divudi.core.facade.lab.LabTestHistoryFacade;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;


import javax.faces.convert.Converter;


/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com> and
 *
 */
@Named(value = "labTestHistoryController")
@SessionScoped
public class LabTestHistoryController implements Serializable {

    /**
     * Creates a new instance of LabTestHistoryController
     */
    public LabTestHistoryController() {
    }

    @EJB
    private LabTestHistoryFacade facade;

    @Inject
    private SessionController sessionController;

    private LabTestHistory current;
    private List<LabTestHistory> items = null;

    public void save() {
        if (current == null) {
            return;
        }
        try {
            if (current.getId() != null) {
                getFacade().edit(current);
                JsfUtil.addSuccessMessage("Updated Successfully.");
            } else {
                current.setCreatedAt(new Date());
                current.setCreatedBy(getSessionController().getLoggedUser());
                getFacade().create(current);
                JsfUtil.addSuccessMessage("Saved Successfully");
            }
            items = null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving: " + e.getMessage());
        }
    }

    public LabTestHistory getCurrent() {
        if (current == null) {
            current = new LabTestHistory();
        }
        return current;
    }

    public void setCurrent(LabTestHistory current) {
        this.current = current;
    }

    public List<LabTestHistory> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    public LabTestHistoryFacade getFacade() {
        return facade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    @FacesConverter(forClass = LabTestHistory.class)
    public static class LabTestHistoryConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            LabTestHistoryController controller = (LabTestHistoryController) fc.getApplication()
                    .getELResolver().getValue(fc.getELContext(), null, "labTestHistoryController");
            return controller.getFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext fc, UIComponent uic, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof LabTestHistory) {
                LabTestHistory lth = (LabTestHistory) object;
                return lth.getId() == null ? "" : lth.getId().toString();
            } else {
                throw new IllegalArgumentException("Unexpected object type");
            }
        }
    }
}
