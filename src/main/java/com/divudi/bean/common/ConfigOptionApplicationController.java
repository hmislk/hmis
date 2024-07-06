package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.OptionScope;
import com.divudi.data.OptionValueType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.ConfigOption;
import com.divudi.entity.WebUser;
import com.divudi.facade.ConfigOptionFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@ApplicationScoped
public class ConfigOptionApplicationController implements Serializable {

    @EJB
    private ConfigOptionFacade optionFacade;

    private List<ConfigOption> options;

    /**
     * Creates a new instance of OptionController
     */
    public ConfigOptionApplicationController() {
    }

    private Map<String, ConfigOption> applicationOptions;

    @PostConstruct
    public void init() {
        loadApplicationOptions();
        
    }

    public void loadApplicationOptions() {
        applicationOptions = new HashMap<>();
        List<ConfigOption> options = getApplicationOptions();
        for (ConfigOption option : options) {
            applicationOptions.put(option.getOptionKey(), option);
        }
    }

    public ConfigOption getApplicationOption(String key) {
        if (applicationOptions == null) {
            loadApplicationOptions();
        }
        ConfigOption c = applicationOptions.get(key);
        return c;
    }

    public void saveOption(ConfigOption option) {
        if (option == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (option.getId() == null) {
            option.setCreatedAt(new Date());
            optionFacade.create(option);
        } else {
            optionFacade.edit(option);
        }
    }

    public void saveShortTextOption(String key, String value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.SHORT_TEXT);
            option.setOptionValue(value);
            optionFacade.create(option);
            loadApplicationOptions();
        }
    }

//    public ConfigOption getOptionValueByKey(String key) {
//        StringBuilder jpql = new StringBuilder("SELECT o FROM ConfigOption o WHERE o.optionKey = :key AND o.scope = :scope");
//        Map<String, Object> params = new HashMap<>();
//        params.put("key", key);
//        params.put("scope", OptionScope.APPLICATION);
//        jpql.append(" AND o.department IS NULL AND o.institution IS NULL AND o.webUser IS NULL");
//        ConfigOption option = optionFacade.findFirstByJpql(jpql.toString(), params);
//        return option;
//    }
    public <E extends Enum<E>> E getEnumValue(ConfigOption option, Class<E> enumClass) {
        if (option.getEnumType() == null || option.getEnumValue() == null) {
            return null; // Or throw an exception if appropriate
        }
        if (!option.getEnumType().equals(enumClass.getName())) {
            throw new IllegalArgumentException("The option does not match the expected enum type.");
        }
        return E.valueOf(enumClass, option.getEnumValue());
    }

    public <E extends Enum<E>> E getEnumValueByKey(String key, Class<E> enumClass) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.ENUM || !option.getEnumType().equals(enumClass.getName())) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());

            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.ENUM);
            option.setEnumType(enumClass.getName());
            optionFacade.create(option); // Persist the new ConfigOption entity
            loadApplicationOptions();
        }

        return getEnumValue(option, enumClass);
    }

    public Double getDoubleValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.DOUBLE) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());

            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.DOUBLE);
            optionFacade.create(option);

            loadApplicationOptions();
        }
        try {
            return Double.parseDouble(option.getEnumValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLongTextValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());

            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.LONG_TEXT);
            option.setOptionValue(""); // Assuming an empty string is an appropriate default. Adjust as necessary.
            optionFacade.create(option);
            loadApplicationOptions();
        }
        return option.getOptionValue();
    }

    public String getLongTextValueByKey(String key, String defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setValueType(OptionValueType.LONG_TEXT);
            option.setOptionValue(defaultValue);
            optionFacade.create(option);
            loadApplicationOptions();
        }
        return option.getOptionValue();
    }

    public void setLongTextValueByKey(String key, String value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setValueType(OptionValueType.LONG_TEXT);
            optionFacade.create(option);
        }
        option.setOptionValue(value);
        optionFacade.edit(option);
        loadApplicationOptions();
    }

    public String getShortTextValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.SHORT_TEXT);
            option.setOptionValue("");
            optionFacade.create(option);
            loadApplicationOptions();
        }
        return option.getOptionValue();
    }

    public String getShortTextValueByKey(String key, String defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.SHORT_TEXT);
            option.setOptionValue(defaultValue);
            optionFacade.create(option);
            loadApplicationOptions();
        }
        return option.getOptionValue();
    }

    public String getEnumValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.ENUM) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.ENUM);
            option.setOptionValue("");
            optionFacade.create(option);
            loadApplicationOptions();
        }
        return option.getOptionValue();
    }

    public Long getLongValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.LONG);
            option.setOptionValue("0");
            optionFacade.create(option);
            loadApplicationOptions();
        }

        try {
            // Attempt to convert the option's value to a Long
            return Long.parseLong(option.getOptionValue());
        } catch (NumberFormatException e) {
// Log or handle the case where the value cannot be parsed into a Long
            return null;
        }
    }

    public Long getLongValueByKey(String key, Long defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.LONG);
            if (defaultValue != null) {
                option.setOptionValue("" + defaultValue);
            } else {
                option.setOptionValue("0");
            }
            optionFacade.create(option);
            loadApplicationOptions();
        }
        try {
            return Long.parseLong(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<String> getListOfCustomOptions(String optionName) {
        // Fetch the string that contains options separated by line breaks
        String listOfOptionSeperatedByLineBreaks = getLongTextValueByKey("Custom option values for " + optionName);
        // Check if the string is not null or empty before processing
        if (listOfOptionSeperatedByLineBreaks == null || listOfOptionSeperatedByLineBreaks.isEmpty()) {
            return Collections.emptyList(); // Return an empty list if there's nothing to process
        }
        // Split the string by any standard line break sequence and convert to a list
        List<String> listOfCustomOptions = Arrays.stream(listOfOptionSeperatedByLineBreaks.split("\\r?\\n|\\r"))
                .map(String::trim) // Trim leading and trailing whitespaces
                .filter(s -> !s.isEmpty()) // Filter out any empty strings
                .collect(Collectors.toList());
        return listOfCustomOptions;
    }

    public Boolean getBooleanValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.BOOLEAN);
            option.setOptionValue("false"); // Defaulting to false. Adjust as necessary.
            optionFacade.create(option);
            loadApplicationOptions();
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public Boolean getBooleanValueByKey(String key, boolean defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.BOOLEAN);
            if (defaultValue) {
                option.setOptionValue("true");
            } else {
                option.setOptionValue("false");
            }
            optionFacade.create(option);
            loadApplicationOptions();
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public void setBooleanValueByKey(String key, boolean value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = new ConfigOption();
            option.setCreatedAt(new Date());
            option.setOptionKey(key);
            option.setScope(OptionScope.APPLICATION);
            option.setInstitution(null);
            option.setDepartment(null);
            option.setWebUser(null);
            option.setValueType(OptionValueType.BOOLEAN);
            optionFacade.create(option);
        }
        option.setOptionValue(Boolean.toString(value));
        optionFacade.edit(option);
        loadApplicationOptions();
    }

    public List<ConfigOption> getAllOptions(Object entity) {
        String jpql = "SELECT o FROM ConfigOption o WHERE o.retired = false"; // Assuming there's a 'retired' field.
        Map<String, Object> params = new HashMap<>();

        if (entity == null) {
            // Fetch options that are not associated with any specific department, institution, or user.
            jpql += " AND o.department IS NULL AND o.institution IS NULL AND o.webUser IS NULL";
        } else if (entity instanceof Department) {
            jpql += " AND o.department = :entity";
            params.put("entity", entity);
        } else if (entity instanceof Institution) {
            jpql += " AND o.institution = :entity";
            params.put("entity", entity);
        } else if (entity instanceof WebUser) {
            jpql += " AND o.webUser = :entity";
            params.put("entity", entity);
        } else {
            // This could be adjusted if there are more entity types to consider or removed if all types are accounted for.
            throw new IllegalArgumentException("Unsupported entity type provided.");
        }

        return optionFacade.findByJpql(jpql, params);
    }

    public List<ConfigOption> getApplicationOptions() {
        return getAllOptions(null);
    }

    private <T> T convertOptionValue(ConfigOption option, Class<T> type) {
        String value = option.getOptionValue();
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

    public List<ConfigOption> getOptions() {
        return options;
    }

    public void setOptions(List<ConfigOption> options) {
        this.options = options;
    }

    public void listApplicationOptions() {
        options = getApplicationOptions();
    }

}
