package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ConfigOptionFacade;
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
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

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
//    private List<Denomination> denominations;

    /**
     * Creates a new instance of OptionController
     */
    public ConfigOptionApplicationController() {
    }

    private Map<String, ConfigOption> applicationOptions;

    private ConfigOption findActiveOptionWithLock(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        StringBuilder jpql = new StringBuilder("SELECT o FROM ConfigOption o WHERE o.retired=false AND o.optionKey=:key AND o.scope=:scope");
        Map<String, Object> params = new HashMap<>();
        params.put("key", key);
        params.put("scope", scope);
        if (institution != null) {
            jpql.append(" AND o.institution = :institution");
            params.put("institution", institution);
        } else {
            jpql.append(" AND o.institution IS NULL");
        }
        if (department != null) {
            jpql.append(" AND o.department = :department");
            params.put("department", department);
        } else {
            jpql.append(" AND o.department IS NULL");
        }
        if (webUser != null) {
            jpql.append(" AND o.webUser = :webUser");
            params.put("webUser", webUser);
        } else {
            jpql.append(" AND o.webUser IS NULL");
        }
        return optionFacade.findFirstByJpqlWithLock(jpql.toString(), params);
    }

    private ConfigOption createApplicationOptionIfAbsent(String key, OptionValueType type, String value) {
        ConfigOption option = findActiveOptionWithLock(key, OptionScope.APPLICATION, null, null, null);
        if (option != null) {
            return option;
        }
        option = new ConfigOption();
        option.setCreatedAt(new Date());
        option.setOptionKey(key);
        option.setScope(OptionScope.APPLICATION);
        option.setInstitution(null);
        option.setDepartment(null);
        option.setWebUser(null);
        option.setValueType(type);
        option.setOptionValue(value);
        optionFacade.create(option);
        loadApplicationOptions();
        return option;
    }

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
        loadEmailGatewayConfigurationDefaults();
        loadPharmacyConfigurationDefaults();
        loadPharmacyIssueReceiptConfigurationDefaults();
        loadPharmacyTransferIssueReceiptConfigurationDefaults();
        loadPharmacyDirectPurchaseWithoutCostingConfigurationDefaults();
    }

    private void loadEmailGatewayConfigurationDefaults() {
        getIntegerValueByKey("Email Gateway - SMTP Port", 587);
        getBooleanValueByKey("Email Gateway - SMTP Auth Enabled", true);
        getBooleanValueByKey("Email Gateway - StartTLS Enabled", true);
        getBooleanValueByKey("Email Gateway - SSL Enabled", false);
        // DO NOT set defaults for these, just trigger their presence in DB:
        getShortTextValueByKey("Email Gateway - Username", "");
        getShortTextValueByKey("Email Gateway - Password", "");
        getShortTextValueByKey("Email Gateway - SMTP Host", "");
        getShortTextValueByKey("Email Gateway - URL", "");
        //
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after one minute", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after two minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 5 minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 10 minutes", true);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 15 minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after 20 minutes", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after half an hour", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after one hour", false);
        getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Send after two hours", false);
    }

    private void loadPharmacyConfigurationDefaults() {
        getDoubleValueByKey("Wholesale Rate Factor", 1.08);
        getDoubleValueByKey("Retail to Purchase Factor", 1.15);
        getDoubleValueByKey("Maximum Retail Price Change Percentage", 15.0);
        getBooleanValueByKey("Direct Issue Based On Retail Rate", true);
        getBooleanValueByKey("Direct Issue Based On Purchase Rate", false);
        getBooleanValueByKey("Direct Issue Based On Cost Rate", false);
        getBooleanValueByKey("Direct Purchase Return Based On Purchase Rate", true);
        getBooleanValueByKey("Direct Purchase Return Based On Line Cost Rate", false);
        getBooleanValueByKey("Direct Purchase Return Based On Total Cost Rate", false);
        getBooleanValueByKey("Direct Purchase Return by Quantity and Free Quantity", true);
        getBooleanValueByKey("Direct Purchase Return by Total Quantity", false);
        getBooleanValueByKey("Show Profit Percentage in GRN", true);
    }

    private void loadPharmacyIssueReceiptConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Issue Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "}\n"
                + ".receipt-header, .receipt-title, .receipt-separator, .receipt-summary {\n"
                + "    margin-bottom: 10px;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 16px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 11px;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "    text-decoration: underline;\n"
                + "}\n"
                + ".receipt-details-table, .receipt-items-table, .receipt-summary-table {\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-header {\n"
                + "    font-weight: bold;\n"
                + "    border-bottom: 1px solid #ccc;\n"
                + "}\n"
                + ".item-name, .item-qty, .item-rate, .item-value {\n"
                + "    padding: 4px;\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".item-qty, .item-rate, .item-value {\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".summary-label {\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".summary-value {\n"
                + "    text-align: right;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".total-amount {\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    text-align: right;\n"
                + "    text-decoration: overline;\n"
                + "}"
        );
    }

    private void loadPharmacyTransferIssueReceiptConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Transfer Issue Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "}\n"
                + ".receipt-header, .receipt-title, .receipt-separator, .receipt-summary {\n"
                + "    margin-bottom: 10px;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 16px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 11px;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "    text-decoration: underline;\n"
                + "}\n"
                + ".receipt-details-table, .receipt-items-table, .receipt-summary-table {\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-header {\n"
                + "    font-weight: bold;\n"
                + "    border-bottom: 1px solid #ccc;\n"
                + "}\n"
                + ".item-name, .item-qty, .item-rate, .item-value {\n"
                + "    padding: 4px;\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".item-qty, .item-rate, .item-value {\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".summary-label {\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".summary-value {\n"
                + "    text-align: right;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".total-amount {\n"
                + "    font-size: 14px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    text-align: right;\n"
                + "    text-decoration: overline;\n"
                + "}"
        );
    }

    private void loadPharmacyDirectPurchaseWithoutCostingConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Direct Purchase without Costing Receipt CSS",
                ".receipt-container {\n"
                + "    font-family: Verdana, sans-serif;\n"
                + "    font-size: 12px;\n"
                + "    color: #000;\n"
                + "    page-break-inside: avoid;\n"
                + "}\n"
                + ".receipt-header-section {\n"
                + "    padding: 10px 0;\n"
                + "    page-break-after: avoid;\n"
                + "}\n"
                + ".receipt-body-section {\n"
                + "    padding: 10px 0;\n"
                + "    page-break-after: always;\n"
                + "}\n"
                + ".receipt-footer-section {\n"
                + "    padding: 10px 0;\n"
                + "    page-break-before: always;\n"
                + "}\n"
                + ".receipt-institution-name {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 23px;\n"
                + "    text-align: center;\n"
                + "    font-family: monospace;\n"
                + "    text-transform: capitalize;\n"
                + "}\n"
                + ".receipt-institution-contact {\n"
                + "    text-align: center;\n"
                + "    font-size: 16px;\n"
                + "    font-family: monospace;\n"
                + "}\n"
                + ".receipt-title {\n"
                + "    text-align: center;\n"
                + "    font-size: 18px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + ".receipt-details-table {\n"
                + "    font-size: 16px;\n"
                + "    font-family: sans-serif;\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + ".receipt-items-table {\n"
                + "    font-size: 16px;\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "    margin-left: 3%;\n"
                + "    margin-right: 3%;\n"
                + "}\n"
                + ".receipt-items-table td, .receipt-items-table th {\n"
                + "    padding: 4px;\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".receipt-items-table td:first-child {\n"
                + "    text-align: left;\n"
                + "}\n"
                + ".receipt-cashier {\n"
                + "    margin-top: 20px;\n"
                + "    margin-left: 3%;\n"
                + "    margin-right: 3%;\n"
                + "    text-align: right;\n"
                + "}\n"
                + ".showRetailValue {\n"
                + "    display: table-cell;\n"
                + "}\n"
                + ".hideRetailValue {\n"
                + "    display: none;\n"
                + "}\n"
                + ".showProfit {\n"
                + "    display: table-cell;\n"
                + "}\n"
                + ".hideProfit {\n"
                + "    display: none;\n"
                + "}\n"
                + "@media print {\n"
                + "  .receipt-body-section {\n"
                + "    page-break-after: always;\n"
                + "  }\n"
                + "  .receipt-footer-section {\n"
                + "    page-break-before: always;\n"
                + "  }\n"
                + "}"
        );
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

//    public List<Denomination> getDenominations() {
//        if (denominations == null) {
//            initializeDenominations();
//        }
//        for (Denomination d : denominations) {
//            d.setCount(0);
//        }
//        return denominations;
//    }
    public void saveShortTextOption(String key, String value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.SHORT_TEXT, value);
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
            option = createApplicationOptionIfAbsent(key, OptionValueType.ENUM, "");
            option.setEnumType(enumClass.getName());
            optionFacade.edit(option);
        }

        return getEnumValue(option, enumClass);
    }

    public Integer getIntegerValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.INTEGER) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.INTEGER, "0");
        }
        try {
            return Integer.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getIntegerValueByKey(String key, Integer defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.INTEGER) {
            String dv = defaultValue == null ? "" : defaultValue + "";
            option = createApplicationOptionIfAbsent(key, OptionValueType.INTEGER, dv);
        }
        try {
            return Integer.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getDoubleValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.DOUBLE) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.DOUBLE, "0.0");
        }
        try {
            return Double.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getDoubleValueByKey(String key, Double defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.DOUBLE) {
            String dv = defaultValue == null ? "" : defaultValue + "";
            option = createApplicationOptionIfAbsent(key, OptionValueType.DOUBLE, dv);
        }
        try {
            return Double.valueOf(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLongTextValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG_TEXT, "");
        }
        return option.getOptionValue();
    }

    public String getLongTextValueByKey(String key, String defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG_TEXT, defaultValue);
        }
        return option.getOptionValue();
    }

    public void setLongTextValueByKey(String key, String value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG_TEXT, "");
        }
        String sanitized = Jsoup.clean(value, Safelist.basic());
        option.setOptionValue(sanitized);
        optionFacade.edit(option);
        loadApplicationOptions();
    }

    public String getShortTextValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.SHORT_TEXT, "");
        }
        return option.getOptionValue();
    }

    public String getShortTextValueByKey(String key, String defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.SHORT_TEXT, defaultValue);
        }
        return option.getOptionValue();
    }

    public String getColorValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.COLOR) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.COLOR, "");
        }
        return option.getOptionValue();
    }

    public String getColorValueByKey(String key, String defaultColorHashCode) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.COLOR) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.COLOR, defaultColorHashCode);
        }
        return option.getOptionValue();
    }

    public String getEnumValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.ENUM) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.ENUM, "");
        }
        return option.getOptionValue();
    }

    public Long getLongValueByKey(String key) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.LONG) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG, "0");
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
            String dv = defaultValue != null ? "" + defaultValue : "0";
            option = createApplicationOptionIfAbsent(key, OptionValueType.LONG, dv);
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
            option = createApplicationOptionIfAbsent(key, OptionValueType.BOOLEAN, "false");
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public Boolean getBooleanValueByKey(String key, boolean defaultValue) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            String dv = defaultValue ? "true" : "false";
            option = createApplicationOptionIfAbsent(key, OptionValueType.BOOLEAN, dv);
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public void setBooleanValueByKey(String key, boolean value) {
        ConfigOption option = getApplicationOption(key);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = createApplicationOptionIfAbsent(key, OptionValueType.BOOLEAN, Boolean.toString(value));
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
