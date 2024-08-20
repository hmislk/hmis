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

import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.facade.AmppFacade;
import com.divudi.facade.VmppFacade;
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
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class AmppController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AmppFacade ejbFacade;
    private List<Ampp> selectedItems;
    private Ampp current;
    private List<Ampp> items = null;
    @EJB
    private VmppFacade vmppFacade;
    double dblValue;
    @EJB
    PharmacyBean pharmacyBean;
    MeasurementUnit packUnit;
    private String selectText = "";

    public MeasurementUnit getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(MeasurementUnit packUnit) {
        this.packUnit = packUnit;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public double getDblValue() {
        return dblValue;
    }

    public void setDblValue(double dblValue) {
        this.dblValue = dblValue;
    }

    public List<Ampp> completeAmpp(String qry) {
        List<Ampp> a = null;
        if (qry != null) {
            a = getFacade().findByJpql("select c from Ampp c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

//    public List<Ampp> completeAmpp() {
//        List<Ampp> a = null;
//
//        a = getFacade().findByJpql("select c from Ampp c where c.retired=false order by c.name");
//
//        return a;
//    }

    public void prepareAdd() {
        current = new Ampp();
        current.setVmpp(new Vmpp());
        dblValue = 0.0;
    }

    private void recreateModel() {
        items = null;
    }

//    private void saveVmpp() {
//        if (getCurrent().getVmpp().getId() == null) {
//            getVmppFacade().create(getCurrent().getVmpp());
//        } else {
//            getVmppFacade().edit(getCurrent().getVmpp());
//        }
//    }
    public void saveSelected() {
        Vmpp tmp = getPharmacyBean().getVmpp(getCurrent(), getPackUnit());
        getCurrent().setVmpp(tmp);

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
//
//        tmp.setName(getCurrent().getName() + "(Vmpp)");
//        getVmppFacade().edit(tmp);

        recreateModel();
        getItems();
    }

    public AmppFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AmppFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AmppController() {
    }

    public Ampp getCurrent() {
        if (current == null) {
            current = new Ampp();
            current.setDblValue(1.0f);
        }
        return current;
    }

    public void setCurrent(Ampp current) {
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

    private AmppFacade getFacade() {
        return ejbFacade;
    }

    public List<Ampp> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public List<Ampp> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<Ampp> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public VmppFacade getVmppFacade() {
        return vmppFacade;
    }

    public void setVmppFacade(VmppFacade vmppFacade) {
        this.vmppFacade = vmppFacade;
    }

    public void searchItems(AjaxBehaviorEvent e) {
        selectedItems = getFacade().findByJpql("select c from Ampp c where c.retired=false "
                + " and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");

    }

    public String getSelectText() {
        return selectText;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    /**
     *
     */
    @FacesConverter(forClass = Ampp.class)
    public static class AmppConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AmppController controller = (AmppController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "amppController");
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
            if (object instanceof Ampp) {
                Ampp o = (Ampp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AmppController.class.getName());
            }
        }
    }
}
