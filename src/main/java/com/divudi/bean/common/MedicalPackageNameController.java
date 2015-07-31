/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;
import java.util.TimeZone;
import com.divudi.entity.Item;
import com.divudi.facade.MedicalPackageFacade;
import com.divudi.entity.MedicalPackage;
import com.divudi.entity.MedicalPackageFee;
import com.divudi.facade.ItemFacade;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.inject.Named; import javax.ejb.EJB;
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
public  class MedicalPackageNameController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private MedicalPackageFacade ejbFacade;
    @EJB
    private ItemFacade itemFacade;
    @Inject
    SessionController sessionController;
    private MedicalPackage current;
    private List<MedicalPackage> items = null;
    private List<Item> itemList = null;

    
     public List<MedicalPackage> completePack(String query) {
        List<MedicalPackage> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from MedicalPackage p where p.retired=false and (upper(p.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.name";
            ////System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;
    }
    
    /**
     *
     */
    public MedicalPackageNameController() {
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new MedicalPackage();
    }

    /**
     *
     * @return
     */
    public MedicalPackageFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(MedicalPackageFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    /**
     *
     * @return
     */
    public SessionController getSessionController() {
        return sessionController;
    }

    /**
     *
     * @param sessionController
     */
    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * Return the current item
     *
     * @return
     */
    public MedicalPackage getCurrent() {
        if (current == null) {
            current = new MedicalPackage();
        }
        return current;
    }

    /**
     * Set the current item
     *
     * @param current
     */
    public void setCurrent(MedicalPackage current) {
        this.current = current;
    }

    private MedicalPackageFacade getFacade() {
        return ejbFacade;
    }

    /**
     *
     * @return
     */
    public List<MedicalPackage> getItems() {
        String temSql;
        temSql = "SELECT i FROM MedicalPackage i where i.retired=false order by i.name";
        items = getFacade().findBySQL(temSql);
        if (items == null) {
           items = new ArrayList<>();
        }
        return items;
    }

    /**
     *
     * Set all ItemPackages to null
     *
     */
    private void recreateModel() {
        itemList = null;
        items = null;
    }

    /**
     *
     */
    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {

            getFacade().edit(current);

            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    /**
     *
     * Delete the current MedicalPackage
     *
     */
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

    public void setItems(List<MedicalPackage> items) {
        this.items = items;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public List<Item> getItemList() {
        if (itemList == null) {
            itemList = new ArrayList<>();
        }

        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

   @FacesConverter("conMedical")
    public static class MedicalPackageNameControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MedicalPackageNameController controller = (MedicalPackageNameController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "medicalPackageNameController");
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
            if (object instanceof MedicalPackage) {
                MedicalPackage o = (MedicalPackage) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + MedicalPackageNameController.class.getName());
            }
        }
    }
}
