package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.SymanticType;
import com.divudi.data.clinical.DocumentTemplateType;
import com.divudi.entity.WebUser;
import com.divudi.entity.clinical.DocumentTemplate;
import com.divudi.facade.DocumentTemplateFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, Msc, MD
 *
 */
@Named
@SessionScoped
public class DocumentTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;

    @EJB
    private DocumentTemplateFacade ejbFacade;

    private DocumentTemplate current;
    private List<DocumentTemplate> items = null;

    public ArrayList<DocumentTemplateType> getDocumentTemplateTypes() {
        return new ArrayList<>(Arrays.asList(DocumentTemplateType.values()));
    }

    public List<DocumentTemplate> completeDocumentTemplate(String qry) {
        List<DocumentTemplate> c;
        Map m = new HashMap();
        m.put("ret", false);
        m.put("n", "%" + qry + "%");
        String sql;
        sql = "select c "
                + " from DocumentTemplate c "
                + " where c.retired=:ret "
                + " and c.name like :n "
                + " and c.symanticType=:t "
                + " order by c.name";
        c = getFacade().findByJpql(sql, m, 10);
        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<DocumentTemplate> fillAllItems() {
        return fillAllItems(null);
    }

    public List<DocumentTemplate> fillAllItems(WebUser u) {
        Map m = new HashMap();
        m.put("ret", false);
        String j;
        j = "select c "
                + " from DocumentTemplate c "
                + " where c.retired=:ret ";
        if (u != null) {
            j += " and c.webUser=:u ";
            m.put("u", u);
        }
        j += " order by c.name";
        return getFacade().findByJpql(j, m);
    }

    public String navigateToAddNewUserDocumentTemplate() {
        current = new DocumentTemplate();
        current.setWebUser(sessionController.getLoggedUser());
        current.setContents(generateDefaultTemplateContents());
        return "/emr/settings/document_template";
    }

    public String generateDefaultTemplateContents() {
        String contents = "";
        contents = "{name}<br/>"
                + "{age}<br/>"
                + "{sex}<br/>"
                + "{address}<br/>"
                + "{phone}<br/>"
                + "{medicines}<br/>"
                + "{comments}<br/>"
                + "{outdoor}<br/>"
                + "{indoor}<br/>"
                + "{ix}<br/>"
                + "{past-dx}<br/>"
                + "{routine-medicines}<br/>"
                + "{allergies}<br/>"
                + "{visit-date}<br/>"
                + "{height}<br/>"
                + "{weight}<br/>"
                + "{bmi}<br/>"
                + "{bp}";
        return contents;
    }

    public String navigateToEditUserDocumentTemplates() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        return "/emr/settings/document_template";
    }

    public String navigateToListUserDocumentTemplate() {
        items = fillAllItems(null);
        return "/emr/settings/document_templates";
    }

    public void saveUserDocumentTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (current.getWebUser() == null) {
            current.setWebUser(sessionController.getLoggedUser());
        }
        saveSelected();
        fillAllItems(null);
        JsfUtil.addSuccessMessage("Saved");
    }

    public void removeUserDocumentTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        current.setWebUser(sessionController.getLoggedUser());
        delete();
        fillAllItems(sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage("Saved");
    }

    private void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Saved");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Updates");
        }
        items = null;
    }

    public DocumentTemplateFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public DocumentTemplateController() {
    }

    public DocumentTemplate getCurrent() {
        if (current == null) {
            current = new DocumentTemplate();
        }
        return current;
    }

    public void setCurrent(DocumentTemplate current) {
        this.current = current;
    }

    private void delete() {
        if (current != null) {
            current.setRetired(true);
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        items = null;
        current = null;
    }

    private DocumentTemplateFacade getFacade() {
        return ejbFacade;
    }

    public List<DocumentTemplate> getItems() {
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = DocumentTemplate.class)
    public static class DocumentTemplateConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DocumentTemplateController controller = (DocumentTemplateController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "documentTemplateController");
            if (controller == null) {
                JsfUtil.addErrorMessage("controller null");
                return null;
            }
            if (controller.getEjbFacade() == null) {
                JsfUtil.addErrorMessage("facade null");
                return null;
            }
            if (value == null) {
                JsfUtil.addErrorMessage("value null");
                return null;
            }
            Long lngValue = getKey(value);
            if (lngValue == null) {
                JsfUtil.addErrorMessage("lng value null");
                return null;
            }
            return controller.getEjbFacade().find(lngValue);
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
            if (object instanceof DocumentTemplate) {
                DocumentTemplate o = (DocumentTemplate) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DocumentTemplateController.class.getName());
            }
        }
    }
}
