/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.entity.Item;
import com.divudi.entity.MedicalPackage;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.MedicalPackageFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent; import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
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
            sql = "select p from MedicalPackage p where p.retired=false"
                    + " and (p.inactive=false or p.inactive is null)"
                    + "and ((p.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
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
        items = getFacade().findByJpql(temSql);
        if (items == null) {
           items = new ArrayList<>();
        }
        return items;
    }
    
  
    
    public List<MedicalPackage> getWithoutInactiveItems() {
        String temSql;
        temSql = "SELECT i FROM MedicalPackage i where i.retired=false "
                + " and (i.inactive=false or i.inactive is null) "
                + " order by i.name";
        items = getFacade().findByJpql(temSql);
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

            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
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
