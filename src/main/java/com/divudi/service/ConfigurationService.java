package com.divudi.service;

import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.facade.ConfigOptionFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class ConfigurationService {

    @EJB
    private ConfigOptionFacade optionFacade;

    public String getShortTextValueByKey(String key, String defaultValue) {
        ConfigOption option = findApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = createOption(key, OptionValueType.SHORT_TEXT, defaultValue);
        }
        return option.getOptionValue();
    }

    public String getShortTextValueByKey(String key) {
        return getShortTextValueByKey(key, "");
    }

    public String getLongTextValueByKey(String key, String defaultValue) {
        ConfigOption option = findApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = createOption(key, OptionValueType.LONG_TEXT, defaultValue);
        }
        return option.getOptionValue();
    }

    public String getLongTextValueByKey(String key) {
        return getLongTextValueByKey(key, "");
    }

    public Double getDoubleValueByKey(String key, Double defaultValue) {
        ConfigOption option = findApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.DOUBLE) {
            option = createOption(key, OptionValueType.DOUBLE, String.valueOf(defaultValue != null ? defaultValue : 0.0));
        }
        try {
            return Double.parseDouble(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getDoubleValueByKey(String key) {
        return getDoubleValueByKey(key, 0.0);
    }

    public Long getLongValueByKey(String key, Long defaultValue) {
        ConfigOption option = findApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG) {
            option = createOption(key, OptionValueType.LONG, String.valueOf(defaultValue != null ? defaultValue : 0));
        }
        try {
            return Long.parseLong(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Long getLongValueByKey(String key) {
        return getLongValueByKey(key, 0L);
    }

    public Boolean getBooleanValueByKey(String key, boolean defaultValue) {
        ConfigOption option = findApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = createOption(key, OptionValueType.BOOLEAN, String.valueOf(defaultValue));
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public Boolean getBooleanValueByKey(String key) {
        return getBooleanValueByKey(key, false);
    }

    public String getColorValueByKey(String key, String defaultColorHashCode) {
        ConfigOption option = findApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.COLOR) {
            option = createOption(key, OptionValueType.COLOR, defaultColorHashCode);
        }
        return option.getOptionValue();
    }

    public String getColorValueByKey(String key) {
        return getColorValueByKey(key, "");
    }

    public String getEnumValueByKey(String key, String defaultEnumValue) {
        ConfigOption option = findApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.ENUM) {
            option = createOption(key, OptionValueType.ENUM, defaultEnumValue);
        }
        return option.getOptionValue();
    }

    public String getEnumValueByKey(String key) {
        return getEnumValueByKey(key, "");
    }

    private ConfigOption findApplicationOption(String key) {
        String jpql = "SELECT o FROM ConfigOption o WHERE o.optionKey = :key AND o.scope = :scope AND o.department IS NULL AND o.institution IS NULL AND o.webUser IS NULL";
        Map<String, Object> params = new HashMap<>();
        params.put("key", key);
        params.put("scope", OptionScope.APPLICATION);
        return optionFacade.findFirstByJpql(jpql, params);
    }

    private ConfigOption createOption(String key, OptionValueType type, String value) {
        ConfigOption option = new ConfigOption();
        option.setCreatedAt(new Date());
        option.setOptionKey(key);
        option.setScope(OptionScope.APPLICATION);
        option.setValueType(type);
        option.setOptionValue(value);
        optionFacade.create(option);
        return option;
    }
}
