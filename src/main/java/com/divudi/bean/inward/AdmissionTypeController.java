/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.CategoryItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.CategoryItem;
import com.divudi.entity.Item;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.AdmissionTypeFacade;
import java.io.Serializable;
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
public class AdmissionTypeController implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    CategoryItemController categoryItemController;
    
    @EJB
    private AdmissionTypeFacade ejbFacade;
    
    
    private static final long serialVersionUID = 1L;
    
    
    List<AdmissionType> selectedItems;
    private AdmissionType current;
    private Item currentItem;
    private List<CategoryItem> categoryItemList;
    private List<AdmissionType> items = null;
    String selectText = "";
    
    
    
    public String navigateToManageAdmissionTypes(){
        return "/inward/inward_admission_type?faces-redirect=true;";
    }
    
     public String navigateToManageAdmissionItemsAndFees(){
        return "/inward/inward_admission_items_and_fees?faces-redirect=true;";
    }

    public List<AdmissionType> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from AdmissionType c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new AdmissionType();
    }
    
    public void addItemForAdmissionType(){
        if(current==null){
            JsfUtil.addErrorMessage("No Admission Type");
            return;
        }
        if(currentItem==null){
            JsfUtil.addErrorMessage("No Item");
            return;
        }
        CategoryItem ci = new CategoryItem();
        ci.setCategory(current);
        ci.setItem(currentItem);
        categoryItemController.save(ci);
        JsfUtil.addSuccessMessage("Added");
    }

    public void setSelectedItems(List<AdmissionType> selectedItems) {
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

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public AdmissionTypeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AdmissionTypeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AdmissionTypeController() {
    }

    public AdmissionType getCurrent() {
        if (current == null) {
            current = new AdmissionType();
        }
        return current;
    }

    public void setCurrent(AdmissionType current) {
        this.current = current;
    }

    public void fillCategoryItems(){
        categoryItemList = categoryItemController.fillCategoryItems(current);
    }
    
    public void delete() {

        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private AdmissionTypeFacade getFacade() {
        return ejbFacade;
    }
    
    

    public List<AdmissionType> getItems() {
        if (items == null) {
            String j;
            j="select t "
                    + " from AdmissionType t "
                    + " where t.retired=false "
                    + " order by t.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    public Item getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(Item currentItem) {
        this.currentItem = currentItem;
    }

    public List<CategoryItem> getCategoryItemList() {
        return categoryItemList;
    }

    public void setCategoryItemList(List<CategoryItem> categoryItemList) {
        this.categoryItemList = categoryItemList;
    }
    
    

    /**
     *
     */
    @FacesConverter(forClass = AdmissionType.class)
    public static class AdmissionTypeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AdmissionTypeController controller = (AdmissionTypeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "admissionTypeController");
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
            if (object instanceof AdmissionType) {
                AdmissionType o = (AdmissionType) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AdmissionTypeController.class.getName());
            }
        }
    }
}
