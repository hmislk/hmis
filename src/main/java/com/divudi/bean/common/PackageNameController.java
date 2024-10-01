package com.divudi.bean.common;
import com.divudi.entity.Item;
import com.divudi.entity.Packege;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PackegeFacade;
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
public  class PackageNameController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private PackegeFacade ejbFacade;
    @EJB
    private ItemFacade itemFacade;
    @Inject
    SessionController sessionController;
    private Packege current;
    private List<Packege> items = null;
    private List<Item> itemList = null;

    
     public List<Packege> completePack(String query) {
        List<Packege> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select p from Packege p where p.retired=false "
                    + "and (p.inactive=false or p.inactive is null)"
                    + "and ((p.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }
     
     public String navigateToPackageNames(){
         return "/admin/pricing/package_name?faces-redirect=true";
     }
    
    /**
     *
     */
    public PackageNameController() {
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new Packege();
    }

    /**
     *
     * @return
     */
    public PackegeFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(PackegeFacade ejbFacade) {
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
    public Packege getCurrent() {
        if (current == null) {
            current = new Packege();
        }
        return current;
    }

    /**
     * Set the current item
     *
     * @param current
     */
    public void setCurrent(Packege current) {
        this.current = current;
    }

    private PackegeFacade getFacade() {
        return ejbFacade;
    }

    /**
     *
     * @return
     */
    public List<Packege> getItems() {
        String temSql;
        temSql = "SELECT i FROM Packege i where i.retired=false order by i.name";
        items = getFacade().findByJpql(temSql);
        if (items == null) {
            items = new ArrayList<Packege>();
        }
        return items;
    }
     public List<Packege> getWithoutInativeItems() {
        String temSql;
        temSql = "SELECT i FROM Packege i where i.retired=false "
                + " and (i.inactive=false or i.inactive is null)"
                + " order by i.name";
        items = getFacade().findByJpql(temSql);
        if (items == null) {
            items = new ArrayList<Packege>();
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
        current = null;
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
     * Delete the current Packege
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

    public void setItems(List<Packege> items) {
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
            itemList = new ArrayList<Item>();
        }

        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    /**
     *
     */
    @FacesConverter("pac")
    public static class PackegeControllerConverter implements Converter {

        /**
         *
         * @param facesContext
         * @param component
         * @param value
         * @return
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PackageNameController controller = (PackageNameController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "packageNameController");
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

        /**
         *
         * @param facesContext
         * @param component
         * @param object
         * @return
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Packege) {
                Packege o = (Packege) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PackageNameController.class.getName());
            }
        }
    }
}
