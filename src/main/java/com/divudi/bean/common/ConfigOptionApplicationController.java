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
        loadPharmacyAnalyticsConfigurationDefaults();
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
        loadPharmacyTransferRequestReceiptConfigurationDefaults();
        loadPharmacyDirectPurchaseWithoutCostingConfigurationDefaults();
        loadPatientNameConfigurationDefaults();
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

    private void loadPharmacyTransferRequestReceiptConfigurationDefaults() {
        getLongTextValueByKey("Pharmacy Transfer Request Receipt CSS",
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

    private void loadPatientNameConfigurationDefaults() {
        getBooleanValueByKey("Capitalize Entire Patient Name", false);
        getBooleanValueByKey("Capitalize Each Word in Patient Name", false);
    }

    private void loadPharmacyAnalyticsConfigurationDefaults() {
        List<String> tabOptions = Arrays.asList(
                "Show Pharmacy Analytics Summary Reports Tab",
                "Show Pharmacy Analytics Financial Reports Tab",
                "Show Pharmacy Analytics Stock Reports Tab",
                "Show Pharmacy Analytics Item Reports Tab",
                "Show Pharmacy Analytics Movement Reports Tab",
                "Show Pharmacy Analytics Retail Sale Reports Tab",
                "Show Pharmacy Analytics Wholesale Reports Tab",
                "Show Pharmacy Analytics Inpatient Reports Tab",
                "Show Pharmacy Analytics Procurement Reports Tab",
                "Show Pharmacy Analytics Disbursement Reports Tab",
                "Show Pharmacy Analytics Adjustment Reports Tab",
                "Show Pharmacy Analytics Disposal Reports Tab"
        );
        tabOptions.forEach(k -> getBooleanValueByKey(k, true));

        List<String> buttonOptions = Arrays.asList(
                "Show Pharmacy Income Report",
                "Show Pharmacy Income & Cost",
                "Show Daily Stock Values",
                "Show Bill Types",
                "Show Cash In/Out Report",
                "Show Cashier Report",
                "Show Cashier Summary",
                "Show All Cashier Report",
                "Show All Cashier Summary",
                "Show Cashier Detailed Report by Department",
                "Show Pharmacy Sale Summary",
                "Show Pharmacy Sale Summary Date",
                "Show All Department Sale Summary",
                "Show Sale Summary - By Bill Type",
                "Show Sale Summary - By Payment Method",
                "Show Sale Summary - By Payment Method (By Bill)",
                "Show Stock Overview Report",
                "Show Stock Report by Batch",
                "Show Expiring Stock Report by Batch",
                "Show Stock Report by Expiry",
                "Show Zero Stock Item Report",
                "Show Suppliers Expiring Stocks",
                "Show Stock Report by Item",
                "Show Stock Report by Item - Order by VMP",
                "Show Stock Report by Product",
                "Show Stock Report of Single Product",
                "Show Supplier Stock Report",
                "Show Suppliers Stock Summary",
                "Show Category Stock Report",
                "Show Category Stock Summary",
                "Show All Staff Stock",
                "Show Stock History",
                "Show Before Stock Taking Report",
                "Show After Stock Taking Report",
                "Show Stock Taking Report(New)",
                "Show Stock With Movement",
                "Show Stock Summary (with Suppliers)",
                "Show Stock Report (with Suppliers)",
                "Show Stock Report by Batch for Export",
                "Show Bin Card",
                "Show Items (AMP) List",
                "Show Medicine (VTM,ATM,VMP,AMP,VMPP & AMPP) List",
                "Show Single Items Summary",
                "Show All Items Summary",
                "Show Items Without Distributor",
                "Show Items With Suppliers and Prices",
                "Show Items With Distributor",
                "Show Items With Multiple Distributor(Items Only)",
                "Show Item With Multiple Distributor",
                "Show ROL & ROQ Management",
                "Show Reorder Analysis",
                "Show Movement Report Stock By Date",
                "Show Movement Report Stock By Date - By Batch",
                "Show Pharmacy All Report",
                "Show Movement Out by Sale, Issue, and Consumption with Current Stock Report",
                "Show Sale Report",
                "Show Prescription Report",
                "Show Institution Item Movement",
                "Show Fast Moving",
                "Show Slow Moving",
                "Show Non Moving",
                "Show Prescription Summary",
                "Show Presciption List",
                "Show List of Pharmacy Bills",
                "Show Retail Sale Bill List",
                "Show Sale Detail - By Bill",
                "Show Sale Detail - By Bill Items",
                "Show Sale Detail - By Discount Scheme",
                "Show Sale Summary By Discount Scheme Summary",
                "Show Sale Detail - By Payment Method",
                "Show Pharmacy Sale Report",
                "Show Pharmacy Wholesale Report",
                "Show Pharmacy Wholesale Credit Bills",
                "Show BHT Issue - By Bill",
                "Show BHT Issue - By Bill Item",
                "Show BHT Issue - By Item",
                "Show BHT Issue - Staff",
                "Show BHT Issue With Margin Report",
                "Show Pharmacy Procurement Report",
                "Show GRN Summary",
                "Show Department Stock By Batch",
                "Show Purchase Orders Not Approved",
                "Show Department Stock By Batch to Upload",
                "Show Item - wise Purchase/Good Receive",
                "Show Purcharse Bill with Supplier",
                "Show Pharmacy GRN Report",
                "Show Pharmacy GRN and purchase Report",
                "Show GRN Purchase Items by Supplier",
                "Show GRN Summary By Supplier",
                "Show GRN Bill Item Report",
                "Show GRN Registry",
                "Show GRN Return List",
                "Show Purchase Order Summary",
                "Show Purchase Bills by Department",
                "Show Purchase Summary By Supplier",
                "Show Purchase Summary (Credit / Cash )",
                "Show Purchase & GRN Summary (Credit / Cash )",
                "Show Purchase Summary By Supplier (Credit / Cash)",
                "Show Purchase Bill Item",
                "Show GRN Payment Summary",
                "Show GRN Payment Summary By Supplier",
                "Show Pharmacy Return Without Traising",
                "Show Procurement Bill Item List",
                "Show Transfer Issue By Bill Item",
                "Show Transfer Receive By Bill Item",
                "Show Transfer Issue by Bill",
                "Show Transfer Receive by Bill",
                "Show Transfer Issue by Bill(Summary)",
                "Show Transfer Receive by Bill(Summary)",
                "Show Report Transfer Issued not Recieved",
                "Show Staff Stock Report",
                "Show Transfer Report Summary",
                "Show Transfer Issue Summary Report By Date",
                "Show Transfer Receive Vs BHT Issue Quntity Totals By Item",
                "Show Item-vice adjustments",
                "Show Unit Issue by bill",
                "Show Unit Issue by Department",
                "Show Unit Issue by Item (Batch)",
                "Show Unit Issue by Item"
        );

        buttonOptions.forEach(k -> getBooleanValueByKey(k, true));
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
