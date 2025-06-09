package com.divudi.bean.common;

import com.divudi.core.entity.Relation;

import com.divudi.core.facade.RelationFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.JsfUtil.PersistAction;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

@Named
@SessionScoped
public class RelationController implements Serializable {

    @EJB
    private RelationFacade ejbFacade;
    @Inject
    SessionController sessionController;

    private List<Relation> items = null;
    private Relation current;

    public RelationController() {
    }

    public void prepareAdd() {
        current = new Relation();
        fillItems();
    }

    public void fillItems() {
        String j;
        j = "select s "
                + " from Relation s "
                + " where s.retired=:ret "
                + " order by s.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(j, m);
    }

    public Relation getCurrent() {
        return current;
    }

    public void setCurrent(Relation current) {
        this.current = current;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private RelationFacade getFacade() {
        return ejbFacade;
    }

    public Relation prepareCreate() {
        current = new Relation();
        initializeEmbeddableKey();
        return current;
    }

    public void create() {
        persist(PersistAction.CREATE, "Saved");
        if (JsfUtil.isValidationPassed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, "Updated");
    }

    public void destroy() {
        persist(PersistAction.DELETE, "Deleted");
        if (JsfUtil.isValidationPassed()) {
            current = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<Relation> getItems() {
        if (items == null) {
            String j = "select r from Relation r where r.retired=false order by r.orderNo";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    public Relation fetchRelationByName(String relationName) {
        String j = "select r from Relation r where r.name=:relationName";
        Map m = new HashMap();
        m.put("relationName", relationName);
        Relation relation = getFacade().findFirstByJpql(j, m);
        if (relation == null) {
            relation = new Relation();
            relation.setName(relationName);
            relation.setCreatedAt(new Date());
            relation.setCreater(sessionController.getLoggedUser());
            getFacade().create(relation);
        }
        relation.setRetired(false);
        getFacade().edit(relation);
        return relation;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        fillItems();
        current = null;
        getCurrent();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(sessionController.getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        fillItems();
    }


    private void persist(PersistAction persistAction, String successMessage) {
        if (current != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(current);
                } else {
                    getFacade().remove(current);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            }
        }
    }

    public Relation getRelation(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<Relation> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<Relation> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    @FacesConverter(forClass = Relation.class)
    public static class RelationControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RelationController controller = (RelationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "relationController");
            return controller.getRelation(getKey(value));
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
            if (object instanceof Relation) {
                Relation o = (Relation) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), Relation.class.getName()});
                return null;
            }
        }

    }

}
