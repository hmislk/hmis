/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 *
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;

import com.divudi.entity.Category;
import com.divudi.entity.lab.InvestigationCategory;
import com.divudi.entity.lab.Machine;
import com.divudi.facade.InvestigationCategoryFacade;
import com.divudi.facade.MachineFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InvestigationCategoryController implements Serializable {

    /*
    * EJBs
     */
    @EJB
    private InvestigationCategoryFacade ejbFacade;
    @EJB
    MachineFacade machineFacade;
    /*
    * Controllers
     */
    @Inject
    SessionController sessionController;
    /*
    * Class Variable
     */
    List<InvestigationCategory> selectedItems;
    private InvestigationCategory current;
    private List<InvestigationCategory> items = null;
    private List<Machine> machines;
    private UploadedFile file;
    String selectText = "";
    int manageItemIndex=-1;

    public List<InvestigationCategory> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from InvestigationCategory c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new InvestigationCategory();
    }

    public void setSelectedItems(List<InvestigationCategory> selectedItems) {
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

    public InvestigationCategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationCategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InvestigationCategoryController() {
    }

    public InvestigationCategory getCurrent() {
        if (current == null) {
            current = new InvestigationCategory();
        }
        
        return current;
    }

    public void setCurrent(InvestigationCategory current) {
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
    }

    private InvestigationCategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<InvestigationCategory> getItems() {
        if (items == null) {
            items = fillItems();
        }
        return items;
    }

    public List<InvestigationCategory> fillItems() {
        String jpql = "select c "
                + " from InvestigationCategory c "
                + " where c.retired=:ret "
                + " order by c.name";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(jpql, m);
    }
    
    public InvestigationCategory findAndCreateCategoryByName(String qry) {
        InvestigationCategory c;
        String jpql;
        jpql = "select c from "
                + " InvestigationCategory c "
                + " where c.retired=:ret "
                + " and c.name=:name "
                + " order by c.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", qry);
        c = getFacade().findFirstByJpql(jpql, m);
        if(c==null){
            c = new InvestigationCategory();
            c.setName(qry);
            c.setCreatedAt(new Date());
            c.setCreater(sessionController.getLoggedUser());
            getFacade().create(c);
        }
        return c;
    }

    public List<Machine> getMachines() {
        machines = machineFacade.findAll("name", true);
        return machines;
    }

    public int getManageItemIndex() {
        return manageItemIndex;
    }

    public void setManageItemIndex(int manageItemIndex) {
        this.manageItemIndex = manageItemIndex;
    }

    public String navigateToInvestigations() {
        return "/admin/lims/manage_investigation.xhtml";
    }

    public String navigateToInvestigationFees() {
        return "/admin/lims/investigation_fee.xhtml";
    }

    public String navigateToOpdServiceCategory() {
        return "/admin/items/opd_service_category.xhtml";
    }

    @Deprecated
    public String navigateToAddInvestigationCategoryForAdmin() {
        prepareAdd();
        return "/admin/lims/investigation_category";
    }

    @Deprecated
    public String navigateToEditInvestigationCategoryForAdmin() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/admin/lims/investigation_category";
    }

    @Deprecated
    public String navigateToListInvestigationCategoriesForAdmin() {
        getItems();
        return "/admin/items/investigation_category_list";
    }
    

    public String navigateToManageInvestigationCategoris() {
        prepareAdd();
        return "/admin/lims/manage_categories.xhtml?faces-redirect=true";
    }
    
    
    public String navigateToAddInvestigationCategory() {
        prepareAdd();
        return "/admin/lims/category?faces-redirect=true";
    }


    public String navigateToEditInvestigationCategory() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/admin/lims/category?faces-redirect=true";
    }

    public String navigateToListInvestigationCategories() {
        getItems();
        return "/admin/lims/category_list?faces-redirect=true";
    }

    public String navigateToOpdServiceSubCategory() {
        return "/admin/items/opd_service_subcategory.xhtml";
    }
    
    public String navigateToItemsAndServices() {
        return "/admin/items/items.xhtml?faces-redirect=true";
    }
    
    public String navigateToItemsAndServiceCountsByDepartment() {
        return "/admin/items/item_counts_by_department.xhtml?faces-redirect=true";
    }
    
    public String navigateToItemsAndServiceCountsByInstitution() {
        return "/admin/items/item_counts_by_institution.xhtml?faces-redirect=true";
    }

    public String navigateToOpdService() {
        return "/admin/items/opd_service.xhtml?faces-redirect=true";
    }

    public String navigateToInwardService() {
        return "/admin/items/inward_service.xhtml?faces-redirect=true";
    }

    public String navigateToTheatreService() {
        return "/admin/items/theatre_service.xhtml?faces-redirect=true";
    }

    public String navigateToBillExpenses() {
        return "/admin/items/bill_expenses.xhtml?faces-redirect=true";
    }

    public String navigateToRelationships() {
        return "/admin/items/relationships.xhtml?faces-redirect=true";
    }

    public String navigateToReligion() {
        return "/admin/items/religion.xhtml?faces-redirect=true";
    }

    public String navigateToNationality() {
        return "/admin/items/nationality.xhtml?faces-redirect=true";
    }

    public String navigateToOpdListToRemove() {
        return "/admin/items/opd_service_list_to_remove.xhtml?faces-redirect=true";
    }

    public String navigateToItemBulkDelete() {
        return "/admin/items/item_bulk_un_delete.xhtml?faces-redirect=true";
    }

    public String navigateToFormFormat() {
        return "/admin/items/form_format.xhtml?faces-redirect=true";
    }

    public String navigateToFormFormatCategory() {
        return "/admin/items/form_format_category.xhtml?faces-redirect=true";
    }

    public String navigateToInvestigationListToRemove() {
        return "/admin/items/investigation_list_to_remove.xhtml?faces-redirect=true";
    }

    public String navigateToInvestigationBulkUnDelete() {
        return "/admin/items/investigation_bulk_un_delete.xhtml?faces-redirect=true";
    }

    public String navigateToMetaDataSuperCategory() {
        return "/admin/items/metadata_super_category.xhtml?faces-redirect=true";
    }

    public String navigateToInvestigationFormat() {
        if (file == null) {
            JsfUtil.addErrorMessage("No file");
            return "";
        }
        try {
            InputStream inputStream = file.getInputStream();
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

        } catch (IOException ex) {
        }
        return "/admin/items/investigation_format.xhtml?faces-redirect=true";
    }

    /**
     *
     */
    @FacesConverter(forClass = InvestigationCategory.class)
    public static class InvestigationCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationCategoryController controller = (InvestigationCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationCategoryController");
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
            if (object instanceof InvestigationCategory) {
                InvestigationCategory o = (InvestigationCategory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationCategoryController.class.getName());
            }
        }
    }
}
