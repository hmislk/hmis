/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.VtmsVmps;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.VmpFacade;
import com.divudi.facade.VtmsVmpsFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import javax.inject.Inject;
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
 Informatics)
 */
@Named
@SessionScoped
public class VmpController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VmpFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @Inject
    private BillBeanController billBean;
    List<Vmp> selectedItems;
    private Vmp current;
    private List<Vmp> items = null;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    VtmsVmps addingVtmInVmp;
    VtmsVmps removingVtmInVmp;
    @EJB
    VtmsVmpsFacade vivFacade;
    List<VtmsVmps> vivs;
    
    List<Vmp> vmpList;

    public List<Vmp> completeVmp(String query) {
        
        String sql;
        if (query == null) {
            vmpList = new ArrayList<Vmp>();
        } else {
            sql = "select c from Vmp c where c.retired=false and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            ////System.out.println(sql);
            vmpList = getFacade().findBySQL(sql);
        }
        return vmpList;
    }

    public List<VtmsVmps> getVivs() {
        if (getCurrent().getId() == null) {
            return new ArrayList<VtmsVmps>();
        } else {

            vivs = getVivFacade().findBySQL("select v from VtmsVmps v where v.vmp.id = " + getCurrent().getId());

            if (vivs == null) {
                return new ArrayList<VtmsVmps>();
            }

            return vivs;
        }
    }

    public void remove() {
        getVivFacade().remove(removingVtmInVmp);
    }

    public void setVivs(List<VtmsVmps> vivs) {
        this.vivs = vivs;
    }

    private boolean errorCheck() {
        if (addingVtmInVmp == null) {
            return true;
        }
        if (addingVtmInVmp.getVtm() == null) {
            UtilityController.addErrorMessage("Select Vtm");
            return true;
        }
//        TODO:Message
        if (current == null) {            
            return true;
        }
        if (addingVtmInVmp.getStrength() == 0.0) {
            UtilityController.addErrorMessage("Type Strength");
            return true;
        }
        if (current.getCategory() == null) {
            UtilityController.addErrorMessage("Select Category");
            return true;
        }
        if (addingVtmInVmp.getStrengthUnit() == null) {
            UtilityController.addErrorMessage("Select Strenth Unit");
            return true;
        }

        return false;
    }

    public void addVtmInVmp() {
        if (errorCheck()) {
            return;
        }

        saveVmp();
        getAddingVtmInVmp().setVmp(current);
        getVivFacade().create(getAddingVtmInVmp());
        
        UtilityController.addSuccessMessage("Added");

        addingVtmInVmp = null;





    }

    private void saveVmp() {
        if (current.getName() == null || current.getName().equals("")) {
            current.setName(createVmpName());
        }

        if (current.getId() == null || current.getId() == 0) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }

    }

    public String createVmpName() {
        return addingVtmInVmp.getVtm().getName() + " " + addingVtmInVmp.getStrength() + " " + addingVtmInVmp.getStrengthUnit().getName() + " " + current.getCategory().getName();
    }

    public VtmsVmps getAddingVtmInVmp() {
        if (addingVtmInVmp == null) {
            addingVtmInVmp = new VtmsVmps();
        }
        return addingVtmInVmp;
    }

    public void setAddingVtmInVmp(VtmsVmps addingVtmInVmp) {
        this.addingVtmInVmp = addingVtmInVmp;
    }

    public VtmsVmps getRemovingVtmInVmp() {
        return removingVtmInVmp;
    }

    public void setRemovingVtmInVmp(VtmsVmps removingVtmInVmp) {
        this.removingVtmInVmp = removingVtmInVmp;
    }

    public VtmsVmpsFacade getVivFacade() {
        return vivFacade;
    }

    public void setVivFacade(VtmsVmpsFacade vivFacade) {
        this.vivFacade = vivFacade;
    }

    public List<Vmp> completeInvest(String query) {
        List<Vmp> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Vmp>();
        } else {
            sql = "select c from Vmp c where c.retired=false and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            ////System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
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

    public List<Vmp> getSelectedItems() {

        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Vmp c where c.retired=false order by c.name");
        } else {
            String sql = "select c from Vmp c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
            selectedItems = getFacade().findBySQL(sql);

        }
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Vmp();
        addingVtmInVmp = new VtmsVmps();
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
                ////System.out.println(code + " " + ix + " " + ic + " " + f);


                Vmp tix = new Vmp();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);



            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<Vmp> selectedItems) {
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
            UtilityController.addSuccessMessage("Updated Successfully.");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public VmpFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(VmpFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public VmpController() {
    }

    public Vmp getCurrent() {
        if (current == null) {
            current = new Vmp();
        }
        return current;
    }

    public void setCurrent(Vmp current) {
        this.current = current;
        if (current != null) {
            if (current.getBilledAs() == current) {
                billedAs = false;
            } else {
                billedAs = true;
            }
            if (current.getReportedAs() == current) {
                reportedAs = false;
            } else {
                reportedAs = true;
            }
        }
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

    private VmpFacade getFacade() {
        return ejbFacade;
    }

    public List<Vmp> getItems() {
        items = getFacade().findAll("name", true);
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
    @FacesConverter("vmp")
    public static class VmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            VmpController controller = (VmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vmpController");
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
            if (object instanceof Vmp) {
                Vmp o = (Vmp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + VmpController.class.getName());
            }
        }
    }
}
