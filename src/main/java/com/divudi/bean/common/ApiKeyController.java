/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.ApiKeyType;
import com.divudi.entity.ApiKey;
import com.divudi.facade.ApiKeyFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ApiKeyController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ApiKeyFacade ejbFacade;
    private ApiKey current;
    private ApiKey removing;
    private List<ApiKey> items = null;

    public String toManageMyApiKeys() {
        listMyApiKeys();
        return "/user_api_key";
    }

    public ApiKeyType[] getApiKeyTypes() {
        return ApiKeyType.values();
    }

    public void listMyApiKeys() {
        String j;
        j = "select a "
                + " from ApiKey a "
                + " where a.retired=false "
                + " and a.webUser=:wu "
                + " and a.dateOfExpiary > :ed "
                + " order by a.dateOfExpiary";
        Map m = new HashMap();
        m.put("wu", sessionController.getLoggedUser());
        m.put("ed", new Date());
        items = getFacade().findByJpql(j, m, TemporalType.DATE);
    }

    public boolean isValidKey(String key) {
        if (key == null || key.trim().equals("")) {
            return false;
        }
        ApiKey k = findApiKey(key);
        if (k == null) {
            return false;
        }
        if (k.getDateOfExpiary().before(new Date())) {
            return false;
        }
        if (k.getKeyType() == ApiKeyType.Config) {
            return true;
        }
        if (k.getWebUser() == null) {
            return false;
        }
        if (k.getWebUser().isRetired()) {
            return false;
        }
        if (!k.getWebUser().isActivated()) {
            return false;
        }

        return true;
    }

    public ApiKey findApiKey(String keyValue) {
        String j;
        j = "select a "
                + " from ApiKey a "
                + " where a.keyValue=:kv";
        Map m = new HashMap();
        m.put("kv", keyValue);
        return getFacade().findFirstByJpql(j, m);
    }

    public void prepareAdd() {
        createNewApiKey();
    }

    public void createNewApiKey() {
        UUID uuid = UUID.randomUUID();
        current = new ApiKey();
        current.setWebUser(sessionController.getLoggedUser());
        current.setInstitution(sessionController.getInstitution());
        current.setKeyType(ApiKeyType.Finance);
        current.setKeyValue(uuid.toString());
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        current.setDateOfExpiary(c.getTime());
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getKeyValue() == null || getCurrent().getKeyValue().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter Key Value");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        listMyApiKeys();
        setCurrent(null);
        getCurrent();
    }

    public ApiKeyFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ApiKeyFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ApiKeyController() {
    }

    public ApiKey getCurrent() {
        if (current == null) {
            createNewApiKey();
        }
        return current;
    }

    public void setCurrent(ApiKey current) {
        this.current = current;
    }

    public void delete() {
        if (removing != null) {
            removing.setRetired(true);
            removing.setRetiredAt(new Date());
            removing.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(removing);
            JsfUtil.addSuccessMessage("Removed Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        listMyApiKeys();
        removing = null;
    }

    private ApiKeyFacade getFacade() {
        return ejbFacade;
    }

    public List<ApiKey> getItems() {
        return items;
    }

    public ApiKey getRemoving() {
        return removing;
    }

    public void setRemoving(ApiKey removing) {
        this.removing = removing;
    }

    /**
     *
     */
    @FacesConverter(forClass = ApiKey.class)
    public static class ApiKeyConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ApiKeyController controller = (ApiKeyController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "apiKeyController");
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
            if (object instanceof ApiKey) {
                ApiKey o = (ApiKey) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ApiKeyController.class.getName());
            }
        }
    }

}
