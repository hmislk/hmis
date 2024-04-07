/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.OptionScope;
import com.divudi.data.OptionValueType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Option;
import com.divudi.entity.WebUser;
import com.divudi.facade.OptionFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named(value = "optionController")
@SessionScoped
public class OptionController implements Serializable {

    @EJB
    private OptionFacade optionFacade;

    @Inject
    private SessionController sessionController;

    private Option option;
    private Institution institution;
    private Department department;
    private WebUser webUser;
    private List<Option> options;

    /**
     * Creates a new instance of OptionController
     */
    public OptionController() {
    }

    public void saveOption(Option option) {
        if (option == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (option.getId() == null) {
            option.setCreatedAt(new Date());
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.create(option);
        } else {
            optionFacade.edit(option);
        }
    }

    public Option getOptionValueByKey(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        StringBuilder jpql = new StringBuilder("SELECT o FROM Option o WHERE o.key = :key AND o.scope = :scope");
        Map<String, Object> params = new HashMap<>();
        params.put("key", key);
        params.put("scope", scope);

        switch (scope) {
            case DEPARTMENT:
                if (department != null) {
                    jpql.append(" AND o.department = :department");
                    params.put("department", department);
                } else {
                    // Handle null department case, if applicable
                }
                break;
            case INSTITUTION:
                if (institution != null) {
                    jpql.append(" AND o.institution = :institution");
                    params.put("institution", institution);
                } else {
                    // Handle null institution case, if applicable
                }
                break;
            case USER:
                if (webUser != null) {
                    jpql.append(" AND o.webUser = :webUser");
                    params.put("webUser", webUser);
                } else {
                    // Handle null webUser case, if applicable
                }
                break;
            default:
                throw new AssertionError("Unhandled scope: " + scope);
        }

        Option option = optionFacade.findFirstByJpql(jpql.toString(), params);
        if (option == null) {
            option = new Option();
            option.setKey(key);
            option.setScope(scope);
            option.setInstitution(institution);
            option.setDepartment(department);
            option.setWebUser(webUser);
            optionFacade.create(option);
        }
        return option;
    }

    public <E extends Enum<E>> E getEnumValue(Option option, Class<E> enumClass) {
        if (option.getEnumType() == null || option.getEnumValue() == null) {
            return null; // Or throw an exception if appropriate
        }
        if (!option.getEnumType().equals(enumClass.getName())) {
            throw new IllegalArgumentException("The option does not match the expected enum type.");
        }
        return E.valueOf(enumClass, option.getEnumValue());
    }

    public Option getOptionValueByKeyForInstitution(String key, Institution institution) {
        return getOptionValueByKey(key, OptionScope.INSTITUTION, institution, null, null);
    }

    public Option getOptionValueByKeyForDepartment(String key, Department department) {
        return getOptionValueByKey(key, OptionScope.DEPARTMENT, null, department, null);
    }

    public Option getOptionValueByKeyForWebUser(String key, WebUser webUser) {
        return getOptionValueByKey(key, OptionScope.USER, null, null, webUser);
    }

    public List<Option> searchOptions(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Splitting the searchText into keywords
        String[] keywords = searchText.split("\\s+");

        // Starting the JPQL query construction
        StringBuilder jpql = new StringBuilder("SELECT o FROM Option o WHERE o.retired = false"); // Assuming there's a 'retired' field.

        // Parameters map for dynamic values
        Map<String, Object> params = new HashMap<>();

        // Constructing the dynamic part of the JPQL based on keywords
        for (int i = 0; i < keywords.length; i++) {
            String param = "keyword" + i;
            jpql.append(i == 0 ? " AND (" : " OR ");
            jpql.append("LOWER(o.key) LIKE LOWER(:").append(param).append(")");
            params.put(param, "%" + keywords[i] + "%");
        }
        jpql.append(") ORDER BY o.key"); // Closing the OR conditions and adding an ORDER BY clause

        // Utilizing the existing findByJpql method from AbstractFacade
        return getFacade().findByJpql(jpql.toString(), params);
    }

    private <T> T convertOptionValue(Option option, Class<T> type) {
        String value = option.getValue();
        OptionValueType valueType = option.getValueType();

        try {
            switch (valueType) {
                case LONG_TEXT:
                case SHORT_TEXT:
                    if (String.class.equals(type)) {
                        return type.cast(value);
                    }
                    break; // Could add more specific handling for text types if needed
                case BOOLEAN:
                    if (Boolean.class.equals(type) || boolean.class.equals(type)) {
                        return type.cast(Boolean.valueOf(value));
                    }
                    break;
                case LONG:
                    if (Long.class.equals(type) || long.class.equals(type)) {
                        return type.cast(Long.valueOf(value));
                    }
                    break;
                case DOUBLE:
                    if (Double.class.equals(type) || double.class.equals(type)) {
                        return type.cast(Double.valueOf(value));
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to convert value to the requested type.", e);
        }

        throw new IllegalArgumentException("Unsupported type conversion requested: " + type.getSimpleName() + " for value type " + valueType);
    }

    public OptionFacade getOptionFacade() {
        return optionFacade;
    }

    public void setOptionFacade(OptionFacade optionFacade) {
        this.optionFacade = optionFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Option getOption() {
        return option;
    }

    public void setOption(Option option) {
        this.option = option;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    private OptionFacade getFacade() {
        return optionFacade;
    }

    @FacesConverter(forClass = Option.class)
    public static class OptionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            OptionController controller = (OptionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "optionController");
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
            if (object instanceof Option) {
                Option o = (Option) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Option.class.getName());
            }
        }
    }

}
