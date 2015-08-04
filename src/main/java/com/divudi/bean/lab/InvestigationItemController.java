/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.InvestigationItemType;
import com.divudi.entity.Category;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.InvestigationItemValue;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.InvestigationItemValueFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class InvestigationItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private InvestigationItemFacade ejbFacade;
    @EJB
    InvestigationItemValueFacade iivFacade;
    List<InvestigationItem> selectedItems;
    private InvestigationItem current;
    private Investigation currentInvestigation;
    private List<InvestigationItem> items = null;
    String selectText = "";
    InvestigationItemValue removingItem;
    InvestigationItemValue addingItem;
    String addingString;

    Investigation copyingFromInvestigation;
    Investigation copyingToInvestigation;
    String ixXml;

    public String copyInvestigation() {
        if (copyingFromInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an iinvestigation to copy from");
            return "";
        }
        if (copyingToInvestigation == null) {
            JsfUtil.addErrorMessage("Please select an iinvestigation to copy from");
            return "";
        }

        //System.out.println("copyingFromInvestigation = " + copyingFromInvestigation);
        //System.out.println("copyingToInvestigation = " + copyingToInvestigation);

        for (InvestigationItem ii : copyingFromInvestigation.getReportItems()) {

            //System.out.println("ii = " + ii);

            if (!ii.isRetired()) {

                InvestigationItem nii = new InvestigationItem();
                nii.setCategory(ii.getCategory());
                nii.setCreatedAt(new Date());
                nii.setCreater(getSessionController().getLoggedUser());
                nii.setCssBackColor(ii.getCssBackColor());
                nii.setCssBorder(ii.getCssBorder());
                nii.setCssBorderRadius(ii.getCssBorderRadius());
                nii.setCssClip(ii.getCssClip());
                nii.setCssColor(ii.getCssColor());
                nii.setCssFontFamily(ii.getCssFontFamily());
                nii.setCssFontSize(ii.getCssFontSize());
                nii.setCssFontStyle(ii.getCssFontStyle());
                nii.setCssFontVariant(ii.getCssFontVariant());
                nii.setCssFontWeight(ii.getCssFontWeight());
                nii.setCssHeight(ii.getCssHeight());
                nii.setCssLeft(ii.getCssLeft());
                nii.setCssLineHeight(ii.getCssLineHeight());
                nii.setCssMargin(ii.getCssMargin());
                nii.setCssOverflow(ii.getCssOverflow());
                nii.setCssPadding(ii.getCssPadding());
                nii.setCssPosition(ii.getCssPosition());
                nii.setCssStyle(ii.getCssStyle());
                nii.setCssTextAlign(ii.getCssTextAlign());
                nii.setCssTop(ii.getCssTop());
                nii.setCssVerticalAlign(ii.getCssVerticalAlign());
                nii.setCssWidth(ii.getCssWidth());
                nii.setCssZorder(ii.getCssZorder());

                nii.setIxItemType(ii.getIxItemType());
                nii.setIxItemValueType(ii.getIxItemValueType());
                nii.setItem(copyingToInvestigation);

                nii.setName(ii.getName());
                nii.setReportItemType(ii.getReportItemType());

                List<InvestigationItemValue> niivs = new ArrayList<>();
                for (InvestigationItemValue iiv : ii.getInvestigationItemValues()) {

                    //System.out.println("iiv = " + iiv);

                    InvestigationItemValue niiv = new InvestigationItemValue();
                    niiv.setCode(iiv.getCode());
                    niiv.setCreatedAt(new Date());
                    niiv.setCreater(getSessionController().getLoggedUser());
                    niiv.setInvestigationItem(nii);
                    niiv.setName(iiv.getName());
                    niiv.setOrderNo(iiv.getOrderNo());
                    niivs.add(niiv);
                }

                nii.setInvestigationItemValues(niivs);

                getEjbFacade().create(nii);
                
            }

        }
        
        setCurrentInvestigation(copyingToInvestigation);

        return "/lab_investigation_format";

    }

    public String getIxXml() {
        return ixXml;
    }

    public void setIxXml(String ixXml) {
        this.ixXml = ixXml;
    }

    public Investigation getCopyingFromInvestigation() {
        return copyingFromInvestigation;
    }

    public void setCopyingFromInvestigation(Investigation copyingFromInvestigation) {
        this.copyingFromInvestigation = copyingFromInvestigation;
    }

    public Investigation getCopyingToInvestigation() {
        return copyingToInvestigation;
    }

    public void setCopyingToInvestigation(Investigation copyingToInvestigation) {
        this.copyingToInvestigation = copyingToInvestigation;
    }

    public InvestigationItemValueFacade getIivFacade() {
        return iivFacade;
    }

    public void setIivFacade(InvestigationItemValueFacade iivFacade) {
        this.iivFacade = iivFacade;
    }

    public String getAddingString() {
        return addingString;
    }

    public void setAddingString(String addingString) {
        this.addingString = addingString;
    }

    public List<InvestigationItem> completeIxItem(String qry) {
        List<InvestigationItem> iivs;
        if (qry.trim().equals("") || currentInvestigation == null || currentInvestigation.getId() == null) {
            return new ArrayList<InvestigationItem>();
        } else {
            String sql;
            sql = "select i from InvestigationItem i where i.retired=false and i.ixItemType = com.divudi.data.InvestigationItemType.Value and upper(i.name) like '%" + qry.toUpperCase() + "%' and i.item.id = " + currentInvestigation.getId();
            iivs = getEjbFacade().findBySQL(sql);
        }
        if (iivs == null) {
            iivs = new ArrayList<InvestigationItem>();
        }
        return iivs;
    }

    public List<InvestigationItem> getCurrentIxItems() {
        List<InvestigationItem> iivs;
        if (currentInvestigation == null || currentInvestigation.getId() == null) {
            return new ArrayList<InvestigationItem>();
        } else {
            String sql;
            sql = "select i from InvestigationItem i where i.retired=false and i.ixItemType = com.divudi.data.InvestigationItemType.Value and i.item.id = " + currentInvestigation.getId();
            iivs = getEjbFacade().findBySQL(sql);
        }
        if (iivs == null) {
            iivs = new ArrayList<InvestigationItem>();
        }
        return iivs;
    }

    public void addValueToIxItem() {
        if (current == null) {
            UtilityController.addErrorMessage("Please select an Ix");
            return;
        }
        if (addingString.trim().equals("")) {
            UtilityController.addErrorMessage("Enter a value");
            return;
        }
        InvestigationItemValue i = new InvestigationItemValue();
        i.setName(addingString);
        i.setInvestigationItem(current);
        current.getInvestigationItemValues().add(i);
        getEjbFacade().edit(current);
        UtilityController.addSuccessMessage("Added");
        addingString = "";
    }

    public InvestigationItemValue getRemovingItem() {
        return removingItem;
    }

    public void setRemovingItem(InvestigationItemValue removingItem) {
        this.removingItem = removingItem;
    }

    public InvestigationItemValue getAddingItem() {
        return addingItem;
    }

    public void setAddingItem(InvestigationItemValue addingItem) {
        this.addingItem = addingItem;
    }

    public List<InvestigationItem> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from InvestigationItem c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        if (selectedItems == null) {
            selectedItems = new ArrayList<InvestigationItem>();
        }

        return selectedItems;
    }

    public void listInvestigationItem() {
        if (getCurrentInvestigation() == null || getCurrentInvestigation().getId() == null) {
            items = new ArrayList<InvestigationItem>();
        } else {
            items = getEjbFacade().findBySQL("select ii from InvestigationItem ii where ii.retired=false and ii.item.id=" + getCurrentInvestigation().getId());
        }
    }

    public void addNewLabel() {
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("New Label");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Label);
//        getEjbFacade().create(current);
//        listInvestigationItem();
        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
    }

    public void removeItem() {
        current.setRetired(true);
        current.setRetirer(getSessionController().getLoggedUser());
        current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getEjbFacade().edit(getCurrent());
        getItems().remove(getCurrent());

    }

    public void removeInvestigationItemValue() {
        ////System.out.println("1");
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to Remove");
            return;
        }
        ////System.out.println("1");
        if (removingItem == null) {
            UtilityController.addErrorMessage("Nothing to Remove");
            return;
        }
        ////System.out.println("3");
        getIivFacade().remove(removingItem);
        ////System.out.println("4");
        current.getInvestigationItemValues().remove(removingItem);
        ////System.out.println("5");
        getEjbFacade().edit(current);
        ////System.out.println("6");

        UtilityController.addSuccessMessage("Removed");
    }

    public void addNewValue() {
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("New Value");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Value);
//        getEjbFacade().create(current);
        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
    }

    public void addNewCalculation() {
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("New Calculation");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Calculation);
//        getEjbFacade().create(current);
        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
        listInvestigationItem();
    }

    public void addNewFlag() {
        if (currentInvestigation == null) {
            UtilityController.addErrorMessage("Please select an investigation");
            return;
        }
        current = new InvestigationItem();
        current.setName("New Flag");
        current.setItem(currentInvestigation);
        current.setIxItemType(InvestigationItemType.Flag);
//        getEjbFacade().create(current);
        currentInvestigation.getReportItems().add(current);
        getIxFacade().edit(currentInvestigation);
        listInvestigationItem();
        listInvestigationItem();
    }

    public void prepareAdd() {
        current = new InvestigationItem();
    }

    public void setSelectedItems(List<InvestigationItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        current = null;
        currentInvestigation = null;

    }

    public void saveSelectedItemValue(InvestigationItemValue iiv){
        if(current==null){
            return;
        }
        if(iiv==null){
            return;
        }
        getIivFacade().edit(iiv);
    }
    
    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("Saved Successfully");
            getCurrentInvestigation().getReportItems().add(current);
            getIxFacade().edit(currentInvestigation);
        }

//        recreateModel();
//        getItems();
    }
    @EJB
    InvestigationFacade ixFacade;

    public InvestigationFacade getIxFacade() {
        return ixFacade;
    }

    public void setIxFacade(InvestigationFacade ixFacade) {
        this.ixFacade = ixFacade;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public InvestigationItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InvestigationItemController() {
    }

    public InvestigationItem getCurrent() {
        if (current == null) {
            current = new InvestigationItem();
        }
        return current;
    }

    public void setCurrent(InvestigationItem current) {
        this.current = current;
    }

    private InvestigationItemFacade getFacade() {
        return ejbFacade;
    }

    public List<InvestigationItem> getItems() {
        if (getCurrentInvestigation().getId() != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i where i.retired=false and i.item.id = " + getCurrentInvestigation().getId() + " order by i.ixItemType, i.cssTop , i.cssLeft";
            items = getFacade().findBySQL(temSql);
        } else {
            items = new ArrayList<InvestigationItem>();
        }
        return items;
    }

    public Investigation getCurrentInvestigation() {
        if (currentInvestigation == null) {
            currentInvestigation = new Investigation();
            //current = null;
        }
        current = null;
        return currentInvestigation;
    }

    public void setCurrentInvestigation(Investigation currentInvestigation) {
        this.currentInvestigation = currentInvestigation;
        listInvestigationItem();
    }

    /**
     *
     */
    @FacesConverter("iiCon")
//    @FacesConverter("ixConverter")
    public static class InvestigationItemConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemController controller = (InvestigationItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationItemController");
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
            if (object instanceof InvestigationItem) {
                InvestigationItem o = (InvestigationItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationItemController.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = InvestigationItem.class)
//    @FacesConverter("ixConverter")
    public static class InvestigationItemControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemController controller = (InvestigationItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationItemController");
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
            if (object instanceof InvestigationItem) {
                InvestigationItem o = (InvestigationItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationItemController.class.getName());
            }
        }
    }
}
