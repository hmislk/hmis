/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.lab.Antibiotic;
import com.divudi.facade.AntibioticFacade;
import com.divudi.facade.SpecialityFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class AntibioticController implements Serializable {
    /**
     * EJBs
     */
    @EJB
    private AntibioticFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    private BillBeanController billBean;
    /**
     * Properties
     */
    private static final long serialVersionUID = 1L;
    List<Antibiotic> selectedItems;
    private Antibiotic current;
    private List<Antibiotic> items = null;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;

    public String navigateToManageAntibiotics() {
        prepareAdd();
        return "/admin/lims/manage_antibiotics?faces-redirect=true";
    }
    
    public List<Antibiotic> completeAntibiotic(String query) {
        List<Antibiotic> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Antibiotic c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public boolean isBilledAs() {
        return billedAs;
    }

    public void setBilledAs(boolean billedAs) {
        this.billedAs = billedAs;
    }

    public boolean isReportedAs() {
        return reportedAs;
    }

    public void setReportedAs(boolean reportedAs) {
        this.reportedAs = reportedAs;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }


    public String getBulkText() {
        return bulkText;
    }

    public void setBulkText(String bulkText) {
        this.bulkText = bulkText;
    }

    public List<Antibiotic> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findByJpql("select c from Antibiotic c where c.retired=false order by c.name");
        } else {
            String sql = "select c from Antibiotic c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
            selectedItems = getFacade().findByJpql(sql);
        }
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Antibiotic();
    }

    public void bulkUpload() {
        List<String> lstLines = Arrays.asList(getBulkText().split("\\r?\\n"));
        for (String s : lstLines) {
            List<String> w = Arrays.asList(s.split(","));
            try {
                String code = w.get(0);
                String ix = w.get(1);
                String ic = w.get(2);
                String f = w.get(4);
                Antibiotic tix = new Antibiotic();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<Antibiotic> selectedItems) {
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
            if (billedAs == false) {
                //////// // System.out.println("2");
                getCurrent().setBilledAs(getCurrent());

            }
            if (reportedAs == false) {
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            if (billedAs == false) {
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public AntibioticFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AntibioticFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AntibioticController() {
    }

    public Antibiotic getCurrent() {
        if (current == null) {
            current = new Antibiotic();
        }
        return current;
    }

    public void setCurrent(Antibiotic current) {
        this.current = current;
        if (current != null) {
            billedAs = current.getBilledAs() != current;
            reportedAs = current.getReportedAs() != current;
        }
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
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

    private AntibioticFacade getFacade() {
        return ejbFacade;
    }

    public List<Antibiotic> getItems() {
        if (items == null) {
            String j;
            j = "select an from Antibiotic an where an.retired=false order by an.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    /**
     *
     */
    @FacesConverter("antibiotic")
    public static class AntibioticControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AntibioticController controller = (AntibioticController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "antibioticController");
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
            if (object instanceof Antibiotic) {
                Antibiotic o = (Antibiotic) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AntibioticController.class.getName());
            }
        }
    }

}
