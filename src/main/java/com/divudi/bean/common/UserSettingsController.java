package com.divudi.bean.common;

import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
import com.divudi.core.data.dto.ColumnVisibilitySettings;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ConfigOptionFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.json.JSONObject;

/**
 * Session-scoped controller for managing user-specific UI settings
 * Provides persistent storage for column visibility, table preferences, and other UI customizations
 *
 * Performance Note: Session-scoped to avoid repeated database queries
 * Settings are loaded once at login and cached for the entire session
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Named
@SessionScoped
public class UserSettingsController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ConfigOptionFacade configOptionFacade;

    @Inject
    private SessionController sessionController;

    // Cache for loaded settings to minimize database queries
    private Map<String, ConfigOption> settingsCache;

    /**
     * Constructor
     */
    public UserSettingsController() {
        this.settingsCache = new HashMap<>();
    }

    /**
     * Load user settings at login
     * Called automatically when user logs in
     * Loads only current user's settings for performance
     */
    public void loadUserSettings() {
        if (sessionController.getLoggedUser() == null) {
            return;
        }

        settingsCache.clear();

        // Load only current user's settings (not all users - performance critical!)
        String jpql = "SELECT c FROM ConfigOption c WHERE c.retired = false "
                + "AND c.scope = :scope "
                + "AND c.webUser = :user";

        Map<String, Object> params = new HashMap<>();
        params.put("scope", OptionScope.USER);
        params.put("user", sessionController.getLoggedUser());

        try {
            configOptionFacade.findByJpql(jpql, params).forEach(option -> {
                settingsCache.put(option.getOptionKey(), option);
            });
        } catch (Exception e) {
            // Graceful degradation - if settings can't be loaded, continue with defaults

        }
    }

    /**
     * Clear the settings cache
     * Useful when user preferences are reset or changed externally
     */
    public void clearCache() {
        settingsCache.clear();
    }

    /**
     * Get a user setting as a string
     * @param key The setting key
     * @param defaultValue The default value if setting doesn't exist
     * @return The setting value or default
     */
    public String getUserSetting(String key, String defaultValue) {
        // Check cache first
        if (settingsCache.containsKey(key)) {
            return settingsCache.get(key).getOptionValue();
        }

        // Hierarchical fallback: User → Department → Institution → Application → Default
        ConfigOption option = getOptionWithHierarchicalFallback(key);
        if (option != null) {
            settingsCache.put(key, option);
            return option.getOptionValue();
        }

        return defaultValue;
    }

    /**
     * Get a user setting as a boolean
     * @param key The setting key
     * @param defaultValue The default value if setting doesn't exist
     * @return The setting value or default
     */
    public boolean getUserBooleanSetting(String key, boolean defaultValue) {
        String value = getUserSetting(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Get a user setting as an integer
     * @param key The setting key
     * @param defaultValue The default value if setting doesn't exist
     * @return The setting value or default
     */
    public Integer getUserIntegerSetting(String key, Integer defaultValue) {
        String value = getUserSetting(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get a user setting as JSON and deserialize to object
     * @param key The setting key
     * @param clazz The class to deserialize to
     * @param defaultValue The default value if setting doesn't exist
     * @return The deserialized object or default
     */
    public <T> T getUserJsonSetting(String key, Class<T> clazz, T defaultValue) {
        String jsonString = getUserSetting(key, null);
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return deserializeFromJson(jsonString, clazz);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Save a user setting as a string
     * @param key The setting key
     * @param value The setting value
     */
    public void saveUserSetting(String key, String value) {
        saveUserSetting(key, value, OptionValueType.SHORT_TEXT);
    }

    /**
     * Save a user setting with specified type
     * @param key The setting key
     * @param value The setting value
     * @param valueType The value type
     */
    public void saveUserSetting(String key, String value, OptionValueType valueType) {
        if (sessionController.getLoggedUser() == null) {
            return;
        }

        try {
            ConfigOption option = configOptionFacade.createOptionIfNotExists(
                    key,
                    OptionScope.USER,
                    null, // institution
                    null, // department
                    sessionController.getLoggedUser(),
                    valueType,
                    value
            );

            // Update value if it already exists
            if (!value.equals(option.getOptionValue())) {
                option.setOptionValue(value);
                configOptionFacade.edit(option);
            }

            // Update cache
            settingsCache.put(key, option);

        } catch (Exception e) {
        }
    }

    /**
     * Save a user setting as a boolean
     * @param key The setting key
     * @param value The setting value
     */
    public void saveUserBooleanSetting(String key, boolean value) {
        saveUserSetting(key, String.valueOf(value), OptionValueType.BOOLEAN);
    }

    /**
     * Save a user setting as an integer
     * @param key The setting key
     * @param value The setting value
     */
    public void saveUserIntegerSetting(String key, Integer value) {
        saveUserSetting(key, String.valueOf(value), OptionValueType.INTEGER);
    }

    /**
     * Save a user setting as JSON
     * @param key The setting key
     * @param value The object to serialize
     */
    public void saveUserJsonSetting(String key, Object value) {
        try {
            String jsonString = serializeToJson(value);
            saveUserSetting(key, jsonString, OptionValueType.LONG_TEXT);
        } catch (Exception e) {
        }
    }

    /**
     * Get column visibility settings for a page
     * @param pageId The page identifier (e.g., "pharmacy_grn_return_request")
     * @return The column visibility settings
     */
    public ColumnVisibilitySettings getColumnVisibility(String pageId) {
        String key = "ui." + pageId + ".columns.visibility";
        return getUserJsonSetting(key, ColumnVisibilitySettings.class, new ColumnVisibilitySettings());
    }

    /**
     * Save column visibility settings for a page
     * @param pageId The page identifier
     * @param settings The column visibility settings
     */
    public void saveColumnVisibility(String pageId, ColumnVisibilitySettings settings) {
        String key = "ui." + pageId + ".columns.visibility";
        saveUserJsonSetting(key, settings);
    }

    /**
     * Get page size preference for a page
     * @param pageId The page identifier
     * @param defaultSize The default page size
     * @return The user's preferred page size
     */
    public Integer getPageSize(String pageId, Integer defaultSize) {
        ColumnVisibilitySettings settings = getColumnVisibility(pageId);
        return settings.getPageSize() != null ? settings.getPageSize() : defaultSize;
    }

    /**
     * Save page size preference for a page
     * @param pageId The page identifier
     * @param pageSize The page size
     */
    public void savePageSize(String pageId, Integer pageSize) {
        ColumnVisibilitySettings settings = getColumnVisibility(pageId);
        settings.setPageSize(pageSize);
        saveColumnVisibility(pageId, settings);
    }

    /**
     * Toggle column visibility
     * @param pageId The page identifier
     * @param columnId The column identifier
     */
    public void toggleColumnVisibility(String pageId, String columnId) {
        ColumnVisibilitySettings settings = getColumnVisibility(pageId);
        boolean currentState = settings.isColumnVisible(columnId);
        settings.setColumnVisible(columnId, !currentState);
        saveColumnVisibility(pageId, settings);
    }

    /**
     * Check if a column is visible
     * @param pageId The page identifier
     * @param columnId The column identifier
     * @return true if visible
     */
    public boolean isColumnVisible(String pageId, String columnId) {
        ColumnVisibilitySettings settings = getColumnVisibility(pageId);
        return settings.isColumnVisible(columnId);
    }

    /**
     * Reset all settings for a page
     * @param pageId The page identifier
     */
    public void resetPageSettings(String pageId) {
        String key = "ui." + pageId + ".columns.visibility";
        if (sessionController.getLoggedUser() == null) {
            return;
        }

        try {
            String jpql = "SELECT c FROM ConfigOption c WHERE c.retired = false "
                    + "AND c.optionKey = :key "
                    + "AND c.scope = :scope "
                    + "AND c.webUser = :user";

            Map<String, Object> params = new HashMap<>();
            params.put("key", key);
            params.put("scope", OptionScope.USER);
            params.put("user", sessionController.getLoggedUser());

            ConfigOption option = configOptionFacade.findFirstByJpql(jpql, params);
            if (option != null) {
                option.setRetired(true);
                option.setRetiredAt(new Date());
                option.setRetirer(sessionController.getLoggedUser());
                configOptionFacade.edit(option);

                // Remove from cache
                settingsCache.remove(key);

                JsfUtil.addSuccessMessage("Settings reset to defaults");
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error resetting settings");
        }
    }

    /**
     * Get option with hierarchical fallback
     * Priority: USER → DEPARTMENT → INSTITUTION → APPLICATION
     * @param key The option key
     * @return The config option or null
     */
    private ConfigOption getOptionWithHierarchicalFallback(String key) {
        WebUser user = sessionController.getLoggedUser();
        Department department = sessionController.getDepartment();

        // Try USER scope
        if (user != null) {
            ConfigOption userOption = getOption(key, OptionScope.USER, null, null, user);
            if (userOption != null) {
                return userOption;
            }
        }

        // Try DEPARTMENT scope
        if (department != null) {
            ConfigOption deptOption = getOption(key, OptionScope.DEPARTMENT, null, department, null);
            if (deptOption != null) {
                return deptOption;
            }
        }

        // Try APPLICATION scope
        ConfigOption appOption = getOption(key, OptionScope.APPLICATION, null, null, null);
        if (appOption != null) {
            return appOption;
        }

        return null;
    }

    /**
     * Get a config option
     * @param key The option key
     * @param scope The option scope
     * @param institution The institution (nullable)
     * @param department The department (nullable)
     * @param webUser The web user (nullable)
     * @return The config option or null
     */
    private ConfigOption getOption(String key, OptionScope scope,
            com.divudi.core.entity.Institution institution,
            Department department, WebUser webUser) {

        StringBuilder jpql = new StringBuilder("SELECT c FROM ConfigOption c WHERE c.retired = false "
                + "AND c.optionKey = :key AND c.scope = :scope");

        Map<String, Object> params = new HashMap<>();
        params.put("key", key);
        params.put("scope", scope);

        switch (scope) {
            case USER:
                if (webUser != null) {
                    jpql.append(" AND c.webUser = :user");
                    params.put("user", webUser);
                } else {
                    return null;
                }
                break;
            case DEPARTMENT:
                if (department != null) {
                    jpql.append(" AND c.department = :dept");
                    params.put("dept", department);
                } else {
                    return null;
                }
                break;
            case INSTITUTION:
                if (institution != null) {
                    jpql.append(" AND c.institution = :inst");
                    params.put("inst", institution);
                } else {
                    return null;
                }
                break;
            case APPLICATION:
                jpql.append(" AND c.institution IS NULL AND c.department IS NULL AND c.webUser IS NULL");
                break;
        }

        return configOptionFacade.findFirstByJpql(jpql.toString(), params);
    }

    /**
     * Serialize an object to JSON string
     * @param obj The object to serialize
     * @return JSON string
     */
    private String serializeToJson(Object obj) {
        if (obj == null) {
            return "{}";
        }

        if (obj instanceof ColumnVisibilitySettings) {
            ColumnVisibilitySettings settings = (ColumnVisibilitySettings) obj;
            JSONObject json = new JSONObject();

            // Serialize column visibility
            JSONObject columnVisible = new JSONObject();
            if (settings.getColumnVisible() != null) {
                settings.getColumnVisible().forEach(columnVisible::put);
            }
            json.put("columnVisible", columnVisible);

            // Serialize column order
            JSONObject columnOrder = new JSONObject();
            if (settings.getColumnOrder() != null) {
                settings.getColumnOrder().forEach(columnOrder::put);
            }
            json.put("columnOrder", columnOrder);

            // Serialize other properties
            if (settings.getPageSize() != null) {
                json.put("pageSize", settings.getPageSize());
            }
            if (settings.getSortField() != null) {
                json.put("sortField", settings.getSortField());
            }
            if (settings.getSortOrder() != null) {
                json.put("sortOrder", settings.getSortOrder());
            }

            return json.toString();
        }

        // For other objects, use toString or implement custom serialization
        return obj.toString();
    }

    /**
     * Deserialize JSON string to object
     * @param jsonString The JSON string
     * @param clazz The class to deserialize to
     * @return The deserialized object
     */
    private <T> T deserializeFromJson(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        if (clazz == ColumnVisibilitySettings.class) {
            JSONObject json = new JSONObject(jsonString);
            ColumnVisibilitySettings settings = new ColumnVisibilitySettings();

            // Deserialize column visibility
            if (json.has("columnVisible")) {
                JSONObject columnVisible = json.getJSONObject("columnVisible");
                Map<String, Boolean> visibilityMap = new HashMap<>();
                columnVisible.keySet().forEach(key -> {
                    visibilityMap.put(key, columnVisible.getBoolean(key));
                });
                settings.setColumnVisible(visibilityMap);
            }

            // Deserialize column order
            if (json.has("columnOrder")) {
                JSONObject columnOrder = json.getJSONObject("columnOrder");
                Map<String, Integer> orderMap = new HashMap<>();
                columnOrder.keySet().forEach(key -> {
                    orderMap.put(key, columnOrder.getInt(key));
                });
                settings.setColumnOrder(orderMap);
            }

            // Deserialize other properties
            if (json.has("pageSize")) {
                settings.setPageSize(json.getInt("pageSize"));
            }
            if (json.has("sortField")) {
                settings.setSortField(json.getString("sortField"));
            }
            if (json.has("sortOrder")) {
                settings.setSortOrder(json.getString("sortOrder"));
            }

            return clazz.cast(settings);
        }

        return null;
    }

    // Pharmacy GRN Return Request Column Visibility Properties
    // These properties provide JSF-compatible getter/setter pairs for the specific page

    public boolean isPharmacyGrnReturnRequestSupplierVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "supplier");
    }

    public void setPharmacyGrnReturnRequestSupplierVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("supplier", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    public boolean isPharmacyGrnReturnRequestPoNoVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "poNo");
    }

    public void setPharmacyGrnReturnRequestPoNoVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("poNo", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    public boolean isPharmacyGrnReturnRequestGrnNoVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "grnNo");
    }

    public void setPharmacyGrnReturnRequestGrnNoVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("grnNo", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    public boolean isPharmacyGrnReturnRequestInvoiceNoVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "invoiceNo");
    }

    public void setPharmacyGrnReturnRequestInvoiceNoVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("invoiceNo", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    public boolean isPharmacyGrnReturnRequestPoDetailsVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "poDetails");
    }

    public void setPharmacyGrnReturnRequestPoDetailsVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("poDetails", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    public boolean isPharmacyGrnReturnRequestGrnDetailsVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "grnDetails");
    }

    public void setPharmacyGrnReturnRequestGrnDetailsVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("grnDetails", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    public boolean isPharmacyGrnReturnRequestPoValueVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "poValue");
    }

    public void setPharmacyGrnReturnRequestPoValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("poValue", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    public boolean isPharmacyGrnReturnRequestGrnValueVisible() {
        return isColumnVisible("pharmacy_grn_return_request", "grnValue");
    }

    public void setPharmacyGrnReturnRequestGrnValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
        settings.setColumnVisible("grnValue", visible);
        saveColumnVisibility("pharmacy_grn_return_request", settings);
    }

    // Pharmacy GRN List For Return Column Visibility Properties
    // These properties provide JSF-compatible getter/setter pairs for the specific page

    public boolean isPharmacyGrnListForReturnDistributorVisible() {
        return isColumnVisible("pharmacy_grn_list_for_return", "distributor");
    }

    public void setPharmacyGrnListForReturnDistributorVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_list_for_return");
        settings.setColumnVisible("distributor", visible);
        saveColumnVisibility("pharmacy_grn_list_for_return", settings);
    }

    public boolean isPharmacyGrnListForReturnPoNoVisible() {
        return isColumnVisible("pharmacy_grn_list_for_return", "poNo");
    }

    public void setPharmacyGrnListForReturnPoNoVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_list_for_return");
        settings.setColumnVisible("poNo", visible);
        saveColumnVisibility("pharmacy_grn_list_for_return", settings);
    }

    public boolean isPharmacyGrnListForReturnGrnNoVisible() {
        return isColumnVisible("pharmacy_grn_list_for_return", "grnNo");
    }

    public void setPharmacyGrnListForReturnGrnNoVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_list_for_return");
        settings.setColumnVisible("grnNo", visible);
        saveColumnVisibility("pharmacy_grn_list_for_return", settings);
    }

    public boolean isPharmacyGrnListForReturnGrnAtVisible() {
        return isColumnVisible("pharmacy_grn_list_for_return", "grnAt");
    }

    public void setPharmacyGrnListForReturnGrnAtVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_list_for_return");
        settings.setColumnVisible("grnAt", visible);
        saveColumnVisibility("pharmacy_grn_list_for_return", settings);
    }

    public boolean isPharmacyGrnListForReturnGrnByVisible() {
        return isColumnVisible("pharmacy_grn_list_for_return", "grnBy");
    }

    public void setPharmacyGrnListForReturnGrnByVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_list_for_return");
        settings.setColumnVisible("grnBy", visible);
        saveColumnVisibility("pharmacy_grn_list_for_return", settings);
    }
    
    public boolean isPharmacyGrnListForReturnPoValueVisible() {
        return isColumnVisible("pharmacy_grn_list_for_return", "poValue");
    }

    public void setPharmacyGrnListForReturnPoValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_list_for_return");
        settings.setColumnVisible("poValue", visible);
        saveColumnVisibility("pharmacy_grn_list_for_return", settings);
    }

    public boolean isPharmacyGrnListForReturnGrnValueVisible() {
        return isColumnVisible("pharmacy_grn_list_for_return", "grnValue");
    }

    public void setPharmacyGrnListForReturnGrnValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_list_for_return");
        settings.setColumnVisible("grnValue", visible);
        saveColumnVisibility("pharmacy_grn_list_for_return", settings);
    }

    public int getPharmacyGrnListForReturnPageSize() {
        return getPageSize("pharmacy_grn_list_for_return", 10);
    }

    public void setPharmacyGrnListForReturnPageSize(int pagesize) {
        savePageSize("pharmacy_grn_list_for_return", pagesize);
    }
    
    // Pharmacy Return Without Tressing Column Visibility Properties
    // These properties provide JSF-compatible getter/setter pairs for the specific page
    
    public boolean isPharmacyReturnWithouttressingItemVisible(){
        return isColumnVisible("pharmacy_return_withouttressing", "item");
    }
    
    public void setPharmacyReturnWithouttressingItemVisible(boolean visible){
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_return_withouttressing");
        settings.setColumnVisible("item", visible);
        saveColumnVisibility("pharmacy_return_withouttressing", settings);
    }
    
    public boolean isPharmacyReturnWithouttressingQtyVisible(){
        return isColumnVisible("pharmacy_return_withouttressing", "qty");
    }
    
    public void setPharmacyReturnWithouttressingQtyVisible(boolean visible){
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_return_withouttressing");
        settings.setColumnVisible("qty", visible);
        saveColumnVisibility("pharmacy_return_withouttressing", settings);
    }
    
    public boolean isPharmacyReturnWithouttressingReturnRateVisible(){
        return isColumnVisible("pharmacy_return_withouttressing", "returnRate");
    }
    
    public void setPharmacyReturnWithouttressingReturnRateVisible(boolean visible){
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_return_withouttressing");
        settings.setColumnVisible("returnRate", visible);
        saveColumnVisibility("pharmacy_return_withouttressing", settings);
    }
    
    public boolean isPharmacyReturnWithouttressingReturnValueVisible(){
        return isColumnVisible("pharmacy_return_withouttressing", "returnValue");
    }
    
    public void setPharmacyReturnWithouttressingReturnValueVisible(boolean visible){
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_return_withouttressing");
        settings.setColumnVisible("returnValue", visible);
        saveColumnVisibility("pharmacy_return_withouttressing", settings);
    }
    
    public boolean isPharmacyReturnWithouttressingRetailRateVisible(){
        return isColumnVisible("pharmacy_return_withouttressing", "retailRate");
    }
    
    public void setPharmacyReturnWithouttresingRetailRateVisible(boolean visible){
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_return_withouttresing");
        settings.setColumnVisible("retailRate", visible);
        saveColumnVisibility("pharmacy_return_withouttresing", settings);
    }
    
    public boolean isPharmacyReturnWithouttresingRetailValueVisible(){
        return isColumnVisible("pharmacy_return_withouttresing", "retailValue");
    }
    
    public void setPharmacyReturnWithouttresingRetailValueVisible(boolean visible){
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_return_withouttresing");
        settings.setColumnVisible("retailValue", visible);
        saveColumnVisibility("pharmacy_return_withouttresing", settings);
    }
    
    public boolean isPharmacyReturnWithouttresingExpiryVisible(){
        return isColumnVisible("pharmacy_return_withouttresing", "expiry");
    }
    
    public void setPharmacyReturnWithouttresingExpiryVisible(boolean visible){
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_return_withouttresing");
        settings.setColumnVisible("expiry", visible);
        saveColumnVisibility("pharmacy_return_withouttresing", settings);
    }
    
    // Stock Ledger Column Visibility Properties
    // These properties provide JSF-compatible getter/setter pairs for the stock ledger report page

    public boolean isStockLedgerCategoryVisible() {
        return isColumnVisible("stock_ledger", "category");
    }

    public void setStockLedgerCategoryVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("category", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerItemCodeVisible() {
        return isColumnVisible("stock_ledger", "itemCode");
    }

    public void setStockLedgerItemCodeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("itemCode", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerUomVisible() {
        return isColumnVisible("stock_ledger", "uom");
    }

    public void setStockLedgerUomVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("uom", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerReferenceBillVisible() {
        return isColumnVisible("stock_ledger", "referenceBill");
    }

    public void setStockLedgerReferenceBillVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("referenceBill", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerRefTransactionAtVisible() {
        return isColumnVisible("stock_ledger", "refTransactionAt");
    }

    public void setStockLedgerRefTransactionAtVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("refTransactionAt", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerFromStoreVisible() {
        return isColumnVisible("stock_ledger", "fromStore");
    }

    public void setStockLedgerFromStoreVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("fromStore", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerToStoreVisible() {
        return isColumnVisible("stock_ledger", "toStore");
    }

    public void setStockLedgerToStoreVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("toStore", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerConsumptionDeptVisible() {
        return isColumnVisible("stock_ledger", "consumptionDept");
    }

    public void setStockLedgerConsumptionDeptVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("consumptionDept", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerDocumentTypeVisible() {
        return isColumnVisible("stock_ledger", "documentType");
    }

    public void setStockLedgerDocumentTypeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("documentType", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerRateVisible() {
        return isColumnVisible("stock_ledger", "rate");
    }

    public void setStockLedgerRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("rate", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerCostRateVisible() {
        return isColumnVisible("stock_ledger", "costRate");
    }

    public void setStockLedgerCostRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("costRate", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerValueAtPurchaseRateVisible() {
        return isColumnVisible("stock_ledger", "valueAtPurchaseRate");
    }

    public void setStockLedgerValueAtPurchaseRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("valueAtPurchaseRate", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerValueAtCostRateVisible() {
        return isColumnVisible("stock_ledger", "valueAtCostRate");
    }

    public void setStockLedgerValueAtCostRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("valueAtCostRate", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerValueAtRetailRateVisible() {
        return isColumnVisible("stock_ledger", "valueAtRetailRate");
    }

    public void setStockLedgerValueAtRetailRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("valueAtRetailRate", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerBatchCodeVisible() {
        return isColumnVisible("stock_ledger", "batchCode");
    }

    public void setStockLedgerBatchCodeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("batchCode", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerMrpVisible() {
        return isColumnVisible("stock_ledger", "mrp");
    }

    public void setStockLedgerMrpVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("mrp", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerExpiryDateVisible() {
        return isColumnVisible("stock_ledger", "expiryDate");
    }

    public void setStockLedgerExpiryDateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("expiryDate", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerUserVisible() {
        return isColumnVisible("stock_ledger", "user");
    }

    public void setStockLedgerUserVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("user", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerDepartmentVisible() {
        return isColumnVisible("stock_ledger", "department");
    }

    public void setStockLedgerDepartmentVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("department", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerNameVisible() {
        return isColumnVisible("stock_ledger", "name");
    }

    public void setStockLedgerNameVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("name", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerTransactionTypeVisible() {
        return isColumnVisible("stock_ledger", "transactionType");
    }

    public void setStockLedgerTransactionTypeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("transactionType", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerBillNumberVisible() {
        return isColumnVisible("stock_ledger", "billNumber");
    }

    public void setStockLedgerBillNumberVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("billNumber", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerTransactionAtVisible() {
        return isColumnVisible("stock_ledger", "transactionAt");
    }

    public void setStockLedgerTransactionAtVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("transactionAt", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerStockInQtyVisible() {
        return isColumnVisible("stock_ledger", "stockInQty");
    }

    public void setStockLedgerStockInQtyVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("stockInQty", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    public boolean isStockLedgerStockOutQtyVisible() {
        return isColumnVisible("stock_ledger", "stockOutQty");
    }

    public void setStockLedgerStockOutQtyVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("stock_ledger");
        settings.setColumnVisible("stockOutQty", visible);
        saveColumnVisibility("stock_ledger", settings);
    }

    // Pharmacy Department Stock by Batch Page Column Visibility Properties

    public boolean isPharmacyDepartmentStockByBatchCategoryVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "category");
    }

    public void setPharmacyDepartmentStockByBatchCategoryVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("category", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchTypeVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "type");
    }

    public void setPharmacyDepartmentStockByBatchTypeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("type", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchCodeVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "code");
    }

    public void setPharmacyDepartmentStockByBatchCodeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("code", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchGenericNameVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "genericName");
    }

    public void setPharmacyDepartmentStockByBatchGenericNameVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("genericName", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchExpiryVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "expiry");
    }

    public void setPharmacyDepartmentStockByBatchExpiryVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("expiry", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchBatchNoVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "batchNo");
    }

    public void setPharmacyDepartmentStockByBatchBatchNoVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("batchNo", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchPurchaseRateVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "purchaseRate");
    }

    public void setPharmacyDepartmentStockByBatchPurchaseRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("purchaseRate", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchPurchaseValueVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "purchaseValue");
    }

    public void setPharmacyDepartmentStockByBatchPurchaseValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("purchaseValue", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchCostRateVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "costRate");
    }

    public void setPharmacyDepartmentStockByBatchCostRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("costRate", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchCostValueVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "costValue");
    }

    public void setPharmacyDepartmentStockByBatchCostValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("costValue", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchRetailRateVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "retailRate");
    }

    public void setPharmacyDepartmentStockByBatchRetailRateVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("retailRate", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    public boolean isPharmacyDepartmentStockByBatchRetailValueVisible() {
        return isColumnVisible("pharmacy_department_stock_by_batch", "retailValue");
    }

    public void setPharmacyDepartmentStockByBatchRetailValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_department_stock_by_batch");
        settings.setColumnVisible("retailValue", visible);
        saveColumnVisibility("pharmacy_department_stock_by_batch", settings);
    }

    // Page: creditCompanyOpdCreditSettleBills

    // Column: billNo
    public boolean isCreditCompanyOpdCreditSettleBillsBillNoVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "billNo");
    }

    public void setCreditCompanyOpdCreditSettleBillsBillNoVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("billNo", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Column: billType
    public boolean isCreditCompanyOpdCreditSettleBillsBillTypeVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "billType");
    }

    public void setCreditCompanyOpdCreditSettleBillsBillTypeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("billType", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Column: paymentMethod
    public boolean isCreditCompanyOpdCreditSettleBillsPaymentMethodVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "paymentMethod");
    }

    public void setCreditCompanyOpdCreditSettleBillsPaymentMethodVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("paymentMethod", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Column: dateTime
    public boolean isCreditCompanyOpdCreditSettleBillsDateTimeVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "dateTime");
    }

    public void setCreditCompanyOpdCreditSettleBillsDateTimeVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("dateTime", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Column: user
    public boolean isCreditCompanyOpdCreditSettleBillsUserVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "user");
    }

    public void setCreditCompanyOpdCreditSettleBillsUserVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("user", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Column: status
    public boolean isCreditCompanyOpdCreditSettleBillsStatusVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "status");
    }

    public void setCreditCompanyOpdCreditSettleBillsStatusVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("status", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Column: netValue
    public boolean isCreditCompanyOpdCreditSettleBillsNetValueVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "netValue");
    }

    public void setCreditCompanyOpdCreditSettleBillsNetValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("netValue", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Column: action
    public boolean isCreditCompanyOpdCreditSettleBillsActionVisible() {
        return isColumnVisible("creditCompanyOpdCreditSettleBills", "action");
    }

    public void setCreditCompanyOpdCreditSettleBillsActionVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("creditCompanyOpdCreditSettleBills");
        settings.setColumnVisible("action", visible);
        saveColumnVisibility("creditCompanyOpdCreditSettleBills", settings);
    }

    // Getters and Setters

    public ConfigOptionFacade getConfigOptionFacade() {
        return configOptionFacade;
    }

    public void setConfigOptionFacade(ConfigOptionFacade configOptionFacade) {
        this.configOptionFacade = configOptionFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }
    
    // page: pharmacy_purchase_order_list_to_cancel.xhtml
    // column: Requested At
    public boolean isPharmacyPurchaseOrderListToCancelRequestedAtVisible() {
        return isColumnVisible("pharmacy_purchase_order_list_to_cancel", "requestedAt");
    }
    
    public void setPharmacyPurchaseOrderListToCancelRequestedAtVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_purchase_order_list_to_cancel");
        settings.setColumnVisible("requestedAt", visible);
        saveColumnVisibility("pharmacy_purchase_order_list_to_cancel", settings);
    }
    
    //column: PO Number
    public boolean isPharmacyPurchaseOrderListToCancelPoNumberVisible() {
        return isColumnVisible("pharmacy_purchase_order_list_to_cancel", "poNumber");
    }
    
    public void setPharmacyPurchaseOrderListToCancelPoNumberVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_purchase_order_list_to_cancel");
        settings.setColumnVisible("poNumber", visible);
        saveColumnVisibility("pharmacy_purchase_order_list_to_cancel", settings);
    }
    
    //column: RequestedBy
    public boolean isPharmacyPurchaseOrderListToCancelRequestedByVisible() {
        return isColumnVisible("pharmacy_purchase_order_list_to_cancel", "requestedBy");
    }
    
    public void setPharmacyPurchaseOrderListToCancelRequestedByVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_purchase_order_list_to_cancel");
        settings.setColumnVisible("requestedBy", visible);
        saveColumnVisibility("pharmacy_purchase_order_list_to_cancel", settings);
    }
    
    //column: Supplier
    public boolean isPharmacyPurchaseOrderListToCancelSupplierVisible() {
        return isColumnVisible("pharmacy_purchase_order_list_to_cancel", "supplier");
    }
    
    public void setPharmacyPurchaseOrderListToCancelSupplierVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_purchase_order_list_to_cancel");
        settings.setColumnVisible("supplier", visible);
        saveColumnVisibility("pharmacy_purchase_order_list_to_cancel", settings);
    }
    
    //column: Requested Department
    public boolean isPharmacyPurchaseOrderListToCancelRequestedDepartmentVisible() {
        return isColumnVisible("pharmacy_purchase_order_list_to_cancel", "requestedDepartment");
    }
    
    public void setPharmacyPurchaseOrderListToCancelRequestedDepartmentVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_purchase_order_list_to_cancel");
        settings.setColumnVisible("requestedDepartment", visible);
        saveColumnVisibility("pharmacy_purchase_order_list_to_cancel", settings);
    }
    
    //column: Requested Value
    public boolean isPharmacyPurchaseOrderListToCancelRequestedValueVisible() {
        return isColumnVisible("pharmacy_purchase_order_list_to_cancel", "requestedValue");
    }
    
    public void setPharmacyPurchaseOrderListToCancelRequestedValueVisible(boolean visible) {
        ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_purchase_order_list_to_cancel");
        settings.setColumnVisible("requestedValue", visible);
        saveColumnVisibility("pharmacy_purchase_order_list_to_cancel", settings);
    }
    
    // page size
    public int getPharmacyPurchaseOrderListToCancelPageSize() {
        return getPageSize("pharmacy_purchase_order_list_to_cancel", 10);
    }

    public void setPharmacyPurchaseOrderListToCancelPageSize(int pagesize) {
        savePageSize("pharmacy_purchase_order_list_to_cancel", pagesize);
    }
}
