package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.inward.AdmissionCancellationReason;
import com.divudi.core.facade.AdmissionCancellationReasonFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Admin page for the admission cancellation reason list (#25745). Needs the Admin privilege.
 */
@Named
@SessionScoped
public class AdmissionCancellationReasonController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AdmissionCancellationReasonFacade facade;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    private AdmissionCancellationReason current;
    private List<AdmissionCancellationReason> items;

    public boolean isCanManage() {
        return webUserController.hasPrivilege("Admin");
    }

    private boolean deny() {
        if (isCanManage()) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege to manage cancellation reasons.");
        return true;
    }

    public String navigateToReasons() {
        if (deny()) {
            return null;
        }
        current = new AdmissionCancellationReason();
        items = null;
        return "/inward/inward_cancellation_reasons?faces-redirect=true";
    }

    public void prepareAdd() {
        current = new AdmissionCancellationReason();
    }

    public void save() {
        if (deny()) {
            return;
        }
        String name = current == null || current.getName() == null ? null : current.getName().trim();
        if (name == null || name.isEmpty()) {
            JsfUtil.addErrorMessage("Enter the reason name.");
            return;
        }
        current.setName(name);
        if (current.getId() != null) {
            facade.edit(current);
            JsfUtil.addSuccessMessage("Updated.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(sessionController.getLoggedUser());
            facade.create(current);
            JsfUtil.addSuccessMessage("Saved.");
        }
        items = null;
        current = new AdmissionCancellationReason();
    }

    public void retire() {
        if (deny()) {
            return;
        }
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Select a reason first.");
            return;
        }
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(sessionController.getLoggedUser());
        facade.edit(current);
        JsfUtil.addSuccessMessage("Reason removed. Cancelled admissions keep it.");
        items = null;
        current = new AdmissionCancellationReason();
    }

    public AdmissionCancellationReason getCurrent() {
        if (current == null) {
            current = new AdmissionCancellationReason();
        }
        return current;
    }

    public void setCurrent(AdmissionCancellationReason current) {
        this.current = current;
    }

    public List<AdmissionCancellationReason> getItems() {
        if (items == null) {
            items = facade.findByJpql("select c from AdmissionCancellationReason c where c.retired=false order by c.name");
            if (items == null) {
                items = new ArrayList<>();
            }
        }
        return items;
    }

    /**
     * The reasons offered on the Cancel Admission page. Read fresh each time so a
     * reason added or removed by an admin shows at once in every session.
     */
    public List<AdmissionCancellationReason> getActiveReasons() {
        List<AdmissionCancellationReason> list = facade.findByJpql(
                "select c from AdmissionCancellationReason c where c.retired=false order by c.name");
        return list == null ? new ArrayList<>() : list;
    }

    public AdmissionCancellationReasonFacade getFacade() {
        return facade;
    }

    @FacesConverter(forClass = AdmissionCancellationReason.class)
    public static class AdmissionCancellationReasonConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            AdmissionCancellationReasonController controller = (AdmissionCancellationReasonController) facesContext
                    .getApplication().getELResolver()
                    .getValue(facesContext.getELContext(), null, "admissionCancellationReasonController");
            return controller.getFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof AdmissionCancellationReason) {
                Long id = ((AdmissionCancellationReason) object).getId();
                return id == null ? null : String.valueOf(id);
            }
            throw new IllegalArgumentException("object " + object + " is of type "
                    + object.getClass().getName() + "; expected type: " + AdmissionCancellationReason.class.getName());
        }
    }
}
