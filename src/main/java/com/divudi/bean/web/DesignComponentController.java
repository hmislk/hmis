package com.divudi.bean.web;

import com.divudi.core.data.web.ComponentDataType;
import com.divudi.core.data.web.ComponentPresentationType;
import com.divudi.core.entity.web.DesignComponent;
import com.divudi.core.entity.web.DesignComponentChoice;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.web.ComponentMappingType;
import com.divudi.core.facade.web.DesignComponentChoiceFacade;
import com.divudi.core.facade.web.DesignComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@SessionScoped
public class DesignComponentController implements Serializable {

    @EJB
    DesignComponentFacade facade;
    @EJB
    DesignComponentChoiceFacade choiceFacade;
    private static final Set<ComponentPresentationType> SHORT_TEXT_TYPES = EnumSet.of(
            ComponentPresentationType.Input_text,
            ComponentPresentationType.SelectOneMenu,
            ComponentPresentationType.SelectOneRadio,
            ComponentPresentationType.SelectOneListBox,
            ComponentPresentationType.AutoComplete
    );
    private static final Set<ComponentPresentationType> LONG_TEXT_TYPES = EnumSet.of(
            ComponentPresentationType.Input_text_Area,
            ComponentPresentationType.TextEditor,
            ComponentPresentationType.SelectCheckBoxMenu,
            ComponentPresentationType.SelectManyButton,
            ComponentPresentationType.MultiSelectListBox
    );
    private static final Set<ComponentPresentationType> NUMERIC_TYPES = EnumSet.of(
            ComponentPresentationType.Input_Number,
            ComponentPresentationType.Spinner,
            ComponentPresentationType.Slider
    );
    private static final Set<ComponentPresentationType> BOOLEAN_TYPES = EnumSet.of(
            ComponentPresentationType.SelectBooleanCheckBox,
            ComponentPresentationType.SelectBooleanButton,
            ComponentPresentationType.ToggleSwitch,
            ComponentPresentationType.TriStateCheckBox
    );
    private static final Set<ComponentPresentationType> CHOICE_TYPES = EnumSet.of(
            ComponentPresentationType.SelectOneMenu,
            ComponentPresentationType.SelectOneRadio,
            ComponentPresentationType.SelectOneListBox,
            ComponentPresentationType.SelectCheckBoxMenu,
            ComponentPresentationType.SelectManyButton,
            ComponentPresentationType.MultiSelectListBox,
            ComponentPresentationType.AutoComplete
    );

    private int manageEmrIndex;
    private DesignComponent current;
    private List<DesignComponent> list;
    private DesignComponent currentDataEntryForm;
    private List<DesignComponent> listOfDataEntryForms;
    private DesignComponent currentDataEntryItem;
    private List<DesignComponent> listOfDataEntryItems;
    private List<DesignComponentChoice> currentItemChoices;
    private DesignComponentChoice currentChoice;

    public List<ComponentPresentationType> getComponentPresentationTypes() {
        return Arrays.asList(ComponentPresentationType.values());
    }

    public List<ComponentMappingType> getComponentMappingTypes() {
        return Arrays.asList(ComponentMappingType.values());
    }

    public List<ComponentDataType> getComponentDataTypes() {
        return Arrays.asList(ComponentDataType.values());
    }

    public DesignComponent getCurrent() {
        return current;
    }

    public void setCurrent(DesignComponent current) {
        this.current = current;
    }

    public List<DesignComponent> getList() {
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void setList(List<DesignComponent> list) {
        this.list = list;
    }

    @Deprecated
    public String navigateToAddDesignComponent() {
        current = new DesignComponent();
        return "/webcontent/design_component?faces-redirect=true";
    }

    public String navigateToAddNewDataEntryForm() {
        currentDataEntryForm = new DesignComponent();
        currentDataEntryForm.setComponentPresentationType(ComponentPresentationType.DataEntryForm);
        listOfDataEntryItems = new ArrayList<>();
        return "/forms/data_entry_form?faces-redirect=true";
    }

    public String navigateToEditDesignComponent() {
        if (currentDataEntryItem == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        currentItemChoices = loadChoicesForItem(currentDataEntryItem);
        currentChoice = new DesignComponentChoice();
        return "/forms/data_entry_item?faces-redirect=true";
    }

    public String navigateToEditDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        listOfDataEntryItems = listItemsOfDataEntryForm(currentDataEntryForm);
        return "/forms/data_entry_form?faces-redirect=true";
    }

    public String navigateToListDataEntryForms() {
        listOfDataEntryForms = listDataEntryForms();
        return "/forms/data_entry_forms?faces-redirect=true";
    }

    public String navigateToListDataEntryItems() {

        listOfDataEntryItems = listDataEntryForms();
        return "/forms/data_entry_items?faces-redirect=true";
    }

    public String navigateToAddComponentsToDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }

        if (currentDataEntryForm.getId() == null) {
            JsfUtil.addErrorMessage("Please save first");
            return "";
        }

        currentDataEntryItem = new DesignComponent();
        currentDataEntryItem.setDataEntryForm(currentDataEntryForm);
        currentItemChoices = new ArrayList<>();
        currentChoice = new DesignComponentChoice();
        return "/forms/data_entry_item?faces-redirect=true";
    }

    public String navigateToListComponentsOfDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }

        if (currentDataEntryForm.getId() == null) {
            JsfUtil.addErrorMessage("Please save first");
            return "";
        }

        listOfDataEntryItems = listItemsOfDataEntryForm(currentDataEntryForm);
        return "/forms/data_entry_items?faces-redirect=true";
    }

    public void saveCurrent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (current.getId() == null) {
            facade.create(current);
        } else {
            facade.edit(current);
        }
    }

    public void saveCurrentDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (currentDataEntryForm.getId() == null) {
            facade.create(currentDataEntryForm);
            getListOfDataEntryForms().add(currentDataEntryForm);
            listOfDataEntryItems = new ArrayList<>();
        } else {
            facade.edit(currentDataEntryForm);
        }
    }

    public DesignComponentFacade getFacade() {
        return facade;
    }

    public void saveDataEntryItemOfForm() {
        if (currentDataEntryItem == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (currentDataEntryItem.getId() == null) {
            facade.create(currentDataEntryItem);
        } else {
            facade.edit(currentDataEntryItem);
        }
        getListOfDataEntryItems().add(currentDataEntryItem);
        currentDataEntryItem = new DesignComponent();
    }

    public String saveDataEntryComponentOfForm() {
        if (currentDataEntryItem == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }

        if (!validatePresentationDataTypeCombination(currentDataEntryItem)) {
            return null;
        }

        if (currentDataEntryItem.getId() == null) {
            facade.create(currentDataEntryItem);
            getListOfDataEntryItems().add(currentDataEntryItem);
        } else {
            facade.edit(currentDataEntryItem);
        }

        saveCurrentChoices();
        currentDataEntryItem = new DesignComponent();
        currentItemChoices = new ArrayList<>();
        currentChoice = new DesignComponentChoice();
        return navigateToListComponentsOfDataEntryForm();
    }

    private void listItems() {
        String jpql = "select d "
                + " from DesignComponent d";
        list = facade.findByJpql(jpql);
    }

    private List<DesignComponent> listDataEntryForms() {
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.componentPresentationType=:pt"
                + " and d.retired=:ret";
        Map m = new HashMap();
        m.put("pt", ComponentPresentationType.DataEntryForm);
        m.put("ret", false);
        designComponents = facade.findByJpql(jpql, m);

        return designComponents;
    }

    private List<DesignComponent> listItemsOfDataEntryForm(DesignComponent dataEntryForm) {
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.dataEntryForm=:def"
                + " and d.retired=:ret"
                + " order by d.orderNo";
        Map m = new HashMap();
        m.put("def", dataEntryForm);
        m.put("ret", false);
        designComponents = facade.findByJpql(jpql, m);

        return designComponents;
    }

    public void removeDataEntryItem(DesignComponent item) {
        if (item == null || item.getId() == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        item.setRetired(true);
        facade.edit(item);
        if (currentDataEntryForm != null) {
            listOfDataEntryItems = listItemsOfDataEntryForm(currentDataEntryForm);
        }
        JsfUtil.addSuccessMessage("Removed");
    }

    private boolean validatePresentationDataTypeCombination(DesignComponent item) {
        ComponentPresentationType pt = item.getComponentPresentationType();
        ComponentDataType dt = item.getComponentDataType();
        if (pt == null) {
            JsfUtil.addErrorMessage("Please select an Input Type");
            return false;
        }
        if (dt == null) {
            JsfUtil.addErrorMessage("Please select a Data Type");
            return false;
        }
        if (SHORT_TEXT_TYPES.contains(pt) && dt != ComponentDataType.Short_Text) {
            JsfUtil.addErrorMessage(pt + " requires Data Type: Short_Text");
            return false;
        }
        if (LONG_TEXT_TYPES.contains(pt) && dt != ComponentDataType.Long_Text) {
            JsfUtil.addErrorMessage(pt + " requires Data Type: Long_Text");
            return false;
        }
        if (NUMERIC_TYPES.contains(pt)
                && dt != ComponentDataType.Double
                && dt != ComponentDataType.Integer
                && dt != ComponentDataType.Long
                && dt != ComponentDataType.Short) {
            JsfUtil.addErrorMessage(pt + " requires a numeric Data Type (Double, Integer, Long or Short)");
            return false;
        }
        if (BOOLEAN_TYPES.contains(pt) && dt != ComponentDataType.Boolean) {
            JsfUtil.addErrorMessage(pt + " requires Data Type: Boolean");
            return false;
        }
        if (pt == ComponentPresentationType.Rating && dt != ComponentDataType.Integer) {
            JsfUtil.addErrorMessage("Rating requires Data Type: Integer");
            return false;
        }
        if (pt == ComponentPresentationType.Calendar && dt != ComponentDataType.Date) {
            JsfUtil.addErrorMessage("Calendar requires Data Type: Date");
            return false;
        }
        if (pt == ComponentPresentationType.Signature && dt != ComponentDataType.Long_Text) {
            JsfUtil.addErrorMessage("Signature requires Data Type: Long_Text (stored as Base64 string by PrimeFaces)");
            return false;
        }
        return true;
    }

    public boolean isChoiceType() {
        return currentDataEntryItem != null
                && CHOICE_TYPES.contains(currentDataEntryItem.getComponentPresentationType());
    }

    public boolean isNumericType() {
        return currentDataEntryItem != null
                && NUMERIC_TYPES.contains(currentDataEntryItem.getComponentPresentationType());
    }

    public boolean isRatingType() {
        return currentDataEntryItem != null
                && currentDataEntryItem.getComponentPresentationType() == ComponentPresentationType.Rating;
    }

    public boolean isBooleanButtonType() {
        return currentDataEntryItem != null
                && currentDataEntryItem.getComponentPresentationType() == ComponentPresentationType.SelectBooleanButton;
    }

    public boolean isTextInputType() {
        return currentDataEntryItem != null
                && (currentDataEntryItem.getComponentPresentationType() == ComponentPresentationType.Input_text
                || currentDataEntryItem.getComponentPresentationType() == ComponentPresentationType.Input_text_Area);
    }

    public void addChoice() {
        if (currentDataEntryItem == null) {
            return;
        }
        if (currentChoice == null || currentChoice.getLabel() == null || currentChoice.getLabel().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a label for the choice");
            return;
        }
        if (currentChoice.getValue() == null || currentChoice.getValue().trim().isEmpty()) {
            currentChoice.setValue(currentChoice.getLabel());
        }
        currentChoice.setOrderNo(currentItemChoices.size() + 1);
        currentItemChoices.add(currentChoice);
        currentChoice = new DesignComponentChoice();
    }

    public void removeChoice(DesignComponentChoice choice) {
        currentItemChoices.remove(choice);
        if (choice.getId() != null) {
            choice.setRetired(true);
            choiceFacade.edit(choice);
        }
    }

    private void saveCurrentChoices() {
        if (currentDataEntryItem == null || currentDataEntryItem.getId() == null) {
            return;
        }
        for (DesignComponentChoice choice : currentItemChoices) {
            choice.setDesignComponent(currentDataEntryItem);
            if (choice.getId() == null) {
                choiceFacade.create(choice);
            } else {
                choiceFacade.edit(choice);
            }
        }
    }

    private List<DesignComponentChoice> loadChoicesForItem(DesignComponent item) {
        if (item == null || item.getId() == null) {
            return new ArrayList<>();
        }
        String jpql = "select c from DesignComponentChoice c"
                + " where c.designComponent=:dc"
                + " and c.retired=:ret"
                + " order by c.orderNo";
        Map<String, Object> m = new HashMap<>();
        m.put("dc", item);
        m.put("ret", false);
        return choiceFacade.findByJpql(jpql, m);
    }

    public List<DesignComponent> completeDesignComponents(String query) {
        String jpql = "SELECT d FROM DesignComponent d WHERE "
                + " d.retired=false"
                + "and LOWER(d.name) LIKE :query";
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toLowerCase() + "%");
        return facade.findByJpql(jpql, params);
    }

    public int getManageEmrIndex() {
        return manageEmrIndex;
    }

    public void setManageEmrIndex(int manageEmrIndex) {
        this.manageEmrIndex = manageEmrIndex;
    }

    public DesignComponent getCurrentDataEntryForm() {
        return currentDataEntryForm;
    }

    public void setCurrentDataEntryForm(DesignComponent currentDataEntryForm) {
        this.currentDataEntryForm = currentDataEntryForm;
    }

    public List<DesignComponent> getListOfDataEntryForms() {
        if (listOfDataEntryForms == null) {
            listOfDataEntryForms = new ArrayList<>();
        }
        return listOfDataEntryForms;
    }

    public void setListOfDataEntryForms(List<DesignComponent> listOfDataEntryForms) {
        this.listOfDataEntryForms = listOfDataEntryForms;
    }

    public DesignComponent getCurrentDataEntryItem() {
        return currentDataEntryItem;
    }

    public void setCurrentDataEntryItem(DesignComponent currentDataEntryItem) {
        this.currentDataEntryItem = currentDataEntryItem;
    }

    public List<DesignComponent> getListOfDataEntryItems() {
        if (listOfDataEntryItems == null) {
            listOfDataEntryItems = new ArrayList<>();
        }
        return listOfDataEntryItems;
    }

    public void setListOfDataEntryItems(List<DesignComponent> listOfDataEntryItems) {
        this.listOfDataEntryItems = listOfDataEntryItems;
    }

    public List<DesignComponentChoice> getCurrentItemChoices() {
        if (currentItemChoices == null) {
            currentItemChoices = new ArrayList<>();
        }
        return currentItemChoices;
    }

    public void setCurrentItemChoices(List<DesignComponentChoice> currentItemChoices) {
        this.currentItemChoices = currentItemChoices;
    }

    public DesignComponentChoice getCurrentChoice() {
        if (currentChoice == null) {
            currentChoice = new DesignComponentChoice();
        }
        return currentChoice;
    }

    public void setCurrentChoice(DesignComponentChoice currentChoice) {
        this.currentChoice = currentChoice;
    }

    @FacesConverter(forClass = DesignComponent.class)
    public static class DesignComponentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DesignComponentController controller = (DesignComponentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "designComponentController");
            return controller.getFacade().find(getKey(value));
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
            if (object instanceof DesignComponent) {
                DesignComponent o = (DesignComponent) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DesignComponentController.class.getName());
            }
        }
    }
}
