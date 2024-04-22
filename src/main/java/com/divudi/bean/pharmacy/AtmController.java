/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.pharmacy.Atm;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.facade.AtmFacade;
import java.io.Serializable;
import java.util.ArrayList;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class AtmController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AtmFacade ejbFacade;
    List<Atm> selectedItems;
    private Atm current;
    private List<Atm> items;
    List<Atm> atmList;
    String selectText;

    public String navigateToListAllAtms() {
        String jpql = "Select atm "
                + " from Atm atm "
                + " where atm.retired=:ret "
                + " order by atm.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(jpql, m);
        return "/emr/reports/atms?faces-redirect=trues";
    }

    public List<Atm> completeAtm(String query) {
        String sql;
        if (query == null) {
            atmList = new ArrayList<>();
        } else {
            sql = "select c from Atm c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            atmList = getFacade().findByJpql(sql);
        }
        return atmList;
    }

    public Atm findAndSaveAtmByNameAndCode(Atm atm) {
        String jpql;
        Map m = new HashMap();
        Atm natm;
        if (atm == null) {
            return null;
        } else {
            m.put("ret", false);
            m.put("name", atm.getName());
            m.put("code", atm.getCode());
            jpql = "select c "
                    + " from Atm c "
                    + " where c.retired=:ret "
                    + " and c.name=:name"
                    + " and c.code=:code";
            natm = getFacade().findFirstByJpql(jpql, m);
        }
        if (natm != null) {
            return natm;
        }
        if (atm.getId() == null) {
            getFacade().create(atm);
        }
        return atm;
    }

    public Atm findAtmByName(String atm) {
        String jpql;
        Map m = new HashMap();
        Atm natm;
        if (atm == null) {
            return null;
        } else {
            m.put("ret", false);
            m.put("name", atm);
            jpql = "select c "
                    + " from Atm c "
                    + " where c.retired=:ret "
                    + " and c.name=:name";
            natm = getFacade().findFirstByJpql(jpql, m);
        }
        return natm;
    }

    public Atm findAndSaveAtmByNameAndCode(Atm atm, Vtm vtm) {
        String jpql;
        Map m = new HashMap();
        Atm natm;
        if (atm == null) {
            return null;
        } else {
            m.put("ret", false);
            m.put("name", atm.getName());
            m.put("code", atm.getCode());
            m.put("vtm", vtm);
            jpql = "select c "
                    + " from Atm c "
                    + " where c.retired=:ret "
                    + " and c.vtm=:vtm "
                    + " and c.name=:name"
                    + " and c.code=:code";
            natm = getFacade().findFirstByJpql(jpql, m);
        }
        if (natm != null) {
            return natm;
        }
        if (atm.getId() == null) {
            atm.setVtm(vtm);
            getFacade().create(atm);
        }
        return atm;
    }

    public List<Atm> getSelectedItems() {

        if (selectText == null || selectText.trim().equals("")) {
            selectedItems = getFacade().findByJpql("select c from Atm c where c.retired=false order by c.name");
        } else {
            String sql = "select c from Atm c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
            selectedItems = getFacade().findByJpql(sql);

        }
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Atm();
    }

    public void setSelectedItems(List<Atm> selectedItems) {
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
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }
    
    public void saveAtm(Atm atm) {
        if(atm==null) return;
        
        if (atm.getId() != null) {
            getFacade().edit(atm);
        } else {
            getFacade().create(atm);
        }
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public AtmFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AtmFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AtmController() {
    }

    public Atm getCurrent() {
        if (current == null) {
            current = new Atm();
        }
        return current;
    }

    public void setCurrent(Atm current) {
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

    private AtmFacade getFacade() {
        return ejbFacade;
    }

    public List<Atm> getItems() {
         String sql = " select c from Atm c where "
                + " c.retired=false "
                + " order by c.name ";

        items = getFacade().findByJpql(sql);
        return items;
    }

    public void setItems(List<Atm> items) {
        this.items = items;
    }

    /**
     *
     */
    @FacesConverter(forClass = Atm.class)
    public static class AtmControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AtmController controller = (AtmController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "atmController");
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
            if (object instanceof Atm) {
                Atm o = (Atm) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AtmController.class.getName());
            }
        }
    }

}
