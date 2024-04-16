/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.SymanticType;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.entity.pharmacy.VirtualProductIngredient;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.VmppFacade;
import com.divudi.facade.VirtualProductIngredientFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class VmppController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VmppFacade ejbFacade;
    private Vmpp current;
    private List<Vmpp> items = null;

    @Deprecated
    public String navigateToListAllVmpps() {
        return "/emr/reports/vmpps?faces-redirect=true";
    }

    public List<Vmpp> completeVmpp(String query) {
        List<Vmpp> vmppList;
        String jpql;
        if (query == null || query.isEmpty()) {
            vmppList = new ArrayList<>();
        } else {
            jpql = "select c "
                    + " from Vmpp c "
                    + " where c.retired=:ret "
                    + " and c.name like :n "
                    + " order by c.name";
            Map m = new HashMap();
            m.put("n", "%" + query.trim() + "%");
            m.put("ret", false);
            vmppList = getFacade().findByJpql(jpql, m);
        }
        return vmppList;
    }

    private void saveVmpp() {
        if (current.getName() == null || current.getName().equals("")) {
            JsfUtil.addErrorMessage("No Name");
            return;
        }

        if (current.getId() == null || current.getId() == 0) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }

    }

    public void prepareAdd() {
        current = new Vmpp();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        }
        recreateModel();
        getItems();
    }

    public void save() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully.");
        }
        recreateModel();
        getItems();
    }

    public VmppFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(VmppFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Vmpp getCurrent() {
        if (current == null) {
            current = new Vmpp();
        }
        return current;
    }

    public void setCurrent(Vmpp current) {
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

    private VmppFacade getFacade() {
        return ejbFacade;
    }

    public List<Vmpp> getItems() {
        if (items == null) {
            items = fillItems();
        }
        return items;
    }

    public List<Vmpp> fillItems() {
        String jpql = "SELECT v FROM Vmpp v WHERE v.retired = :ret AND v.name IS NOT NULL AND v.name <> '' ORDER BY v.name";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ret", false);

        List<Vmpp> vmpps = getFacade().findByJpql(jpql, parameters);

        // If vmpps is null, initialize it to prevent NullPointerException
        if (vmpps == null) {
            vmpps = new ArrayList<>();
        }

        return vmpps;
    }

    /**
     *
     */
    @FacesConverter(forClass = Vmpp.class)
    public static class VmppControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            VmppController controller = (VmppController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vmppController");
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
            if (object instanceof Vmpp) {
                Vmpp o = (Vmpp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Vmpp.class.getName());
            }
        }
    }
}
