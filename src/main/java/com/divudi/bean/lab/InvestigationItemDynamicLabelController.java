/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;

import com.divudi.data.InvestigationItemType;
import com.divudi.data.Sex;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.InvestigationItemValueFlag;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.InvestigationItemValueFlagFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InvestigationItemDynamicLabelController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private InvestigationItemValueFlagFacade ejbFacade;
    @EJB
    InvestigationItemFacade investigationItemFacade;
    @EJB
    InvestigationFacade investigationFacade;
    List<InvestigationItemValueFlag> selectedItems;
    private InvestigationItemValueFlag current;
    private List<InvestigationItemValueFlag> items = null;
    private List<InvestigationItemValueFlag> dynamicLabels = null;
    String selectText = "";
    Investigation investigation;
    List<InvestigationItem> investigationItemsOfValueType;
    List<InvestigationItem> investigationItemsOfDynamicLabelType;
    InvestigationItem investigationItemOfValueType;
    InvestigationItem investigationItemofDynamicLabelType;
    long fromAge;
    long toAge;
    String fromAgeUnit;
    String toAgeUnit;
    String flagMessage;
    String lowMessage;
    String highMessage;
    double fromValue;
    double toValue;
    Sex sex;
    InvestigationItemValueFlag removingInvestigationItemofDynamicLabelType;

    public void removeDynamicLabelValue() {
        if (removingInvestigationItemofDynamicLabelType == null) {
            return;
        }
        removingInvestigationItemofDynamicLabelType.setRetired(true);
        removingInvestigationItemofDynamicLabelType.setRetirer(sessionController.getWebUser());
        removingInvestigationItemofDynamicLabelType.setRetiredAt(new Date());
        getFacade().edit(removingInvestigationItemofDynamicLabelType);
        JsfUtil.addSuccessMessage("Removed");
        dynamicLabels = null;
    }

    public double getFromValue() {
        return fromValue;
    }

    public void setFromValue(double fromValue) {
        this.fromValue = fromValue;
    }

    public double getToValue() {
        return toValue;
    }

    public void setToValue(double toValue) {
        this.toValue = toValue;
    }

    public String getLowMessage() {
        return lowMessage;
    }

    public void setLowMessage(String lowMessage) {
        this.lowMessage = lowMessage;
    }

    public String getHighMessage() {
        return highMessage;
    }

    public void setHighMessage(String highMessage) {
        this.highMessage = highMessage;
    }

    public InvestigationItemValueFlag getRemovingInvestigationItemofDynamicLabelType() {
        return removingInvestigationItemofDynamicLabelType;
    }

    public void setRemovingInvestigationItemofDynamicLabelType(InvestigationItemValueFlag removingInvestigationItemofDynamicLabelType) {
        this.removingInvestigationItemofDynamicLabelType = removingInvestigationItemofDynamicLabelType;
    }

    public InvestigationItem getInvestigationItemofDynamicLabelType() {
        return investigationItemofDynamicLabelType;
    }

    public void setInvestigationItemofDynamicLabelType(InvestigationItem investigationItemofDynamicLabelType) {
        this.investigationItemofDynamicLabelType = investigationItemofDynamicLabelType;
        recreateModel();
    }

    public List<InvestigationItem> getInvestigationItemsOfDynamicLabelType() {
        if (investigation != null) {
            investigationItemsOfDynamicLabelType = getInvestigationItemFacade().findByJpql("select i from InvestigationItem i where i.retired=false and i.item.id = " + investigation.getId() + " and i.ixItemType = com.divudi.data.InvestigationItemType.DynamicLabel");
        }
        if (investigationItemsOfDynamicLabelType == null) {
            investigationItemsOfDynamicLabelType = new ArrayList<InvestigationItem>();
        }
        return investigationItemsOfDynamicLabelType;
    }

    public void setInvestigationItemsOfDynamicLabelType(List<InvestigationItem> investigationItemsOfDynamicLabelType) {
        this.investigationItemsOfDynamicLabelType = investigationItemsOfDynamicLabelType;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public String getFlagMessage() {
        return flagMessage;
    }

    public void setFlagMessage(String flagMessage) {
        this.flagMessage = flagMessage;
    }

    private void clearForNew() {
        investigationItemOfValueType = null;
        fromAge = 0;
        toAge = 0;
        fromAgeUnit = "Years";
        toAgeUnit = "Years";
        sex = null;
        flagMessage = "";
        lowMessage = "";
        highMessage = "";
        fromValue = 0.0;
        toValue = 0.0;
    }

    public String getToAgeUnit() {
        return toAgeUnit;
    }

    public void setToAgeUnit(String toAgeUnit) {
        this.toAgeUnit = toAgeUnit;
    }

    public long getFromAge() {
        return fromAge;
    }

    public void setFromAge(long fromAge) {
        this.fromAge = fromAge;
    }

    public long getToAge() {
        return toAge;
    }

    public void setToAge(long toAge) {
        this.toAge = toAge;
    }

    public String getFromAgeUnit() {
        return fromAgeUnit;
    }

    public void setFromAgeUnit(String fromAgeUnit) {
        this.fromAgeUnit = fromAgeUnit;
    }

    public InvestigationItemFacade getInvestigationItemFacade() {
        return investigationItemFacade;
    }

    public void setInvestigationItemFacade(InvestigationItemFacade investigationItemFacade) {
        this.investigationItemFacade = investigationItemFacade;
    }

    public InvestigationFacade getInvestigationFacade() {
        return investigationFacade;
    }

    public void setInvestigationFacade(InvestigationFacade investigationFacade) {
        this.investigationFacade = investigationFacade;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
        recreateModel();
    }

    public List<InvestigationItem> getInvestigationItemsOfValueType() {
        if (investigation != null) {
            Map m = new HashMap();
            m.put("iit", InvestigationItemType.Value);
            investigationItemsOfValueType = getInvestigationItemFacade().findByJpql("select i from InvestigationItem i where i.retired=false and i.item.id = " + investigation.getId() + " and i.ixItemType =:iit", m, TemporalType.TIMESTAMP);
        }
        if (investigationItemsOfValueType == null) {
            investigationItemsOfValueType = new ArrayList<InvestigationItem>();
        }
        return investigationItemsOfValueType;
    }

    public void setInvestigationItemsOfValueType(List<InvestigationItem> investigationItemsOfValueType) {
        this.investigationItemsOfValueType = investigationItemsOfValueType;
    }

    public InvestigationItem getInvestigationItemOfValueType() {
        return investigationItemOfValueType;
    }

    public void setInvestigationItemOfValueType(InvestigationItem investigationItemOfValueType) {
        this.investigationItemOfValueType = investigationItemOfValueType;
    }

    public List<InvestigationItemValueFlag> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from InvestigationItemValueFlag c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new InvestigationItemValueFlag();
    }

    public void setSelectedItems(List<InvestigationItemValueFlag> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        dynamicLabels = null;
    }

    public void saveSelected() {
        //////// // System.out.println("going to save");
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

    public InvestigationItemValueFlagFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationItemValueFlagFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InvestigationItemDynamicLabelController() {
    }

    public InvestigationItemValueFlag getCurrent() {
        return current;
    }

    public void setCurrent(InvestigationItemValueFlag current) {
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

    private InvestigationItemValueFlagFacade getFacade() {
        return ejbFacade;
    }

    public List<InvestigationItemValueFlag> getItems() {
        String sql;
        if (investigation != null) {
            sql = "select i from InvestigationItemValueFlag i where i.retired=false and i.item.id = " + investigation.getId();
            items = getFacade().findByJpql(sql);
        }
        if (items == null) {
            items = new ArrayList<InvestigationItemValueFlag>();
        }
        return items;
    }

    public void saveForDynamicLabel() {
        if (investigation == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }
        if (investigationItemofDynamicLabelType == null) {
            JsfUtil.addErrorMessage("Please select a dynamic label");
            return;
        }
        if (sex == null) {
            JsfUtil.addErrorMessage("Please select a sex");
            return;
        }
        if (toAge == 0) {
            JsfUtil.addErrorMessage("Please select a dynamic label");
            return;
        }
        InvestigationItemValueFlag i = new InvestigationItemValueFlag();
//        i.setItem(investigation);
        i.setCreatedAt(Calendar.getInstance().getTime());
        i.setCreater(getSessionController().getLoggedUser());
        i.setFlagMessage(flagMessage);
        if ("Days".equals(fromAgeUnit)) {
            i.setFromAge(fromAge);
        } else if ("Months".equals(fromAgeUnit)) {
            i.setFromAge(fromAge * 30);
        } else {
            i.setFromAge(fromAge * 365);
        }
        if ("Days".equals(toAgeUnit)) {
            i.setToAge(toAge);
        } else if ("Months".equals(toAgeUnit)) {
            i.setToAge(toAge * 30);
        } else {
            i.setToAge(toAge * 365);
        }
        i.setInvestigationItemOfLabelType(investigationItemofDynamicLabelType);
        i.setSex(sex);
        if (i.getId() == null) {
            getEjbFacade().create(i);
        } else {
            getEjbFacade().edit(i);
        }
        JsfUtil.addSuccessMessage("Saved");
    }

    public void saveFlags() {
        for (InvestigationItemValueFlag f : dynamicLabels) {
            getFacade().edit(f);
        }
    }

    public List<InvestigationItemValueFlag> getDynamicLabels() {
        String sql;
        if (dynamicLabels == null) {
            if (investigation != null && investigationItemofDynamicLabelType != null) {

                sql = "select i "
                        + " from InvestigationItemValueFlag i "
                        + " where i.retired=false and  "
                        + " i.investigationItemOfLabelType.id = " + investigationItemofDynamicLabelType.getId();
                dynamicLabels = getFacade().findByJpql(sql);
            } else {
                dynamicLabels = null;
            }
        }
        if (dynamicLabels == null) {
            dynamicLabels = new ArrayList<InvestigationItemValueFlag>();
        }
        return dynamicLabels;
    }

    public List<InvestigationItemValueFlag> getDynamicLabelsByIxItId(InvestigationItem ii) {
        String sql;
        List<InvestigationItemValueFlag> d;
        if (ii != null) {
            sql = "select i from InvestigationItemValueFlag i where i.retired=false and  "
                    + " i.investigationItemOfLabelType.id = " + ii.getId();
            d = getFacade().findByJpql(sql);
        } else {
            d = null;
        }
        if (d == null) {
            d = new ArrayList<>();
        }
        return d;
    }

    public void setDynamicLabels(List<InvestigationItemValueFlag> dynamicLabels) {
        this.dynamicLabels = dynamicLabels;
    }

    /**
     *
     */
    @FacesConverter(forClass = InvestigationItemValueFlag.class)
    public static class InvestigationItemValueFlagControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemDynamicLabelController controller = (InvestigationItemDynamicLabelController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationItemValueFlagController");
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
            if (object instanceof InvestigationItemValueFlag) {
                InvestigationItemValueFlag o = (InvestigationItemValueFlag) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationItemDynamicLabelController.class.getName());
            }
        }
    }
}
