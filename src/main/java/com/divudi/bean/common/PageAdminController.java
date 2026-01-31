/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.OptionScope;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.annotation.PostConstruct;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.DefaultStreamedContent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Controller for the page administration interface.
 * Manages viewing and navigating to page configuration and privilege management.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PageAdminController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PageAdminController.class.getName());

    @Inject
    private PageMetadataRegistry metadataRegistry;

    @Inject
    private ConfigOptionController configOptionController;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    private String selectedPagePath;
    private PageMetadata currentMetadata;

    public PageAdminController() {
    }

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (metadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("admin/page_configuration_view");
        metadata.setPageName("Page Configuration Management");
        metadata.setDescription("Administrative interface for viewing and managing page-specific configuration options and privileges");
        metadata.setControllerClass("PageAdminController");

        // Configuration Options - Global Bill Number Settings
        metadata.addConfigOption(new ConfigOptionInfo(
            "Add the Institution Code to the Bill Number Generator",
            "Includes institution code in generated bill numbers across all modules system-wide",
            "admin/page_configuration_view",
            OptionScope.APPLICATION
        ));

        // Privileges
        metadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to page configuration management interface - required to view and manage page metadata, configuration options, and privileges"
        ));

        metadataRegistry.registerPage(metadata);
    }

    /**
     * Load metadata for a specific page
     * @param pagePath The page path to load
     */
    public void loadPage(String pagePath) {
        this.selectedPagePath = pagePath;
        this.currentMetadata = metadataRegistry.getMetadata(pagePath);

        if (this.currentMetadata == null) {
            LOGGER.log(Level.WARNING, "Page metadata not found for path: {0}. Page may not be registered.", pagePath);
            JsfUtil.addErrorMessage("Page configuration not found. This page may not be registered in the admin system.");
        }
    }

    /**
     * Navigate to the page admin interface for a specific page
     * @param pagePath The page path
     * @return Navigation outcome
     */
    public String navigateToPageAdmin(String pagePath) {
        loadPage(pagePath);
        return "/admin/page_configuration_view?faces-redirect=true";
    }

    /**
     * Navigate back to the original page.
     * If no page path is set, returns to the main index page as a safe fallback.
     *
     * @return Navigation outcome - either the original page path or index page
     */
    public String navigateBackToPage() {
        if (selectedPagePath != null && !selectedPagePath.isEmpty()) {
            return "/" + selectedPagePath + "?faces-redirect=true";
        }
        // Safe fallback: return to index when no page path is known
        LOGGER.log(Level.INFO, "No page path set, returning to index page");
        return "/index?faces-redirect=true";
    }

    /**
     * Navigate to page-specific configuration management.
     * Shows only the configuration options used by the current page.
     *
     * @return Navigation outcome to page-specific configuration page
     */
    public String navigateToPageSpecificConfigManagement() {
        if (currentMetadata == null) {
            JsfUtil.addErrorMessage("No page metadata available");
            return null;
        }
        return "/admin/page_specific_config_management?faces-redirect=true";
    }

    /**
     * Navigate to page-specific privilege management.
     * Shows only the privileges required by the current page.
     *
     * @return Navigation outcome to page-specific privilege page
     */
    public String navigateToPageSpecificPrivilegeManagement() {
        if (currentMetadata == null) {
            JsfUtil.addErrorMessage("No page metadata available");
            return null;
        }
        return "/admin/page_specific_privilege_management?faces-redirect=true";
    }

    /**
     * Navigate to department-specific configuration management.
     * Shows department configuration options for the current page.
     *
     * @return Navigation outcome to department configuration page
     */
    public String navigateToDepartmentConfigManagement() {
        if (currentMetadata == null) {
            JsfUtil.addErrorMessage("No page metadata available");
            return null;
        }
        return "/admin/page_specific_department_config_management?faces-redirect=true";
    }

    /**
     * Check if the current page has department-scoped configuration options
     * @return true if department configs exist for this page
     */
    public boolean hasDepartmentConfigOptions() {
        if (currentMetadata == null || currentMetadata.getConfigOptions() == null) {
            return false;
        }
        return currentMetadata.getConfigOptions().stream()
            .anyMatch(config -> config.getKey().contains("[Department Name]"));
    }

    /**
     * Get department-scoped configuration options for the current page.
     * These are APPLICATION-scoped options with department names embedded in keys.
     * @return List of department config options with actual department names
     */
    public java.util.List<ConfigOptionInfo> getDepartmentConfigOptions() {
        if (currentMetadata == null || currentMetadata.getConfigOptions() == null) {
            return new java.util.ArrayList<>();
        }

        // Get configs that contain "[Department Name]" placeholder (these are department-specific APPLICATION configs)
        return currentMetadata.getConfigOptions().stream()
            .filter(config -> config.getKey().contains("[Department Name]"))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get pure application-scoped configuration options for the current page.
     * Excludes department-specific configs with "[Department Name]" placeholders.
     * @return List of pure application config options
     */
    public java.util.List<ConfigOptionInfo> getApplicationConfigOptions() {
        if (currentMetadata == null || currentMetadata.getConfigOptions() == null) {
            return new java.util.ArrayList<>();
        }

        // Get configs that do NOT contain "[Department Name]" placeholder (pure APPLICATION configs)
        return currentMetadata.getConfigOptions().stream()
            .filter(config -> !config.getKey().contains("[Department Name]"))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Export configuration options for the current page to CSV format
     * @return StreamedContent for file download
     */
    public StreamedContent exportPageConfiguration() {
        if (currentMetadata == null || currentMetadata.getConfigOptions() == null) {
            JsfUtil.addErrorMessage("No configuration metadata available for export");
            return null;
        }

        try {
            StringBuilder csvContent = new StringBuilder();

            // CSV Header
            csvContent.append("\"Option Key\",\"Option Value\",\"Value Type\",\"Enum Type\"\n");

            // Export each configuration option for this page
            for (ConfigOptionInfo configInfo : currentMetadata.getConfigOptions()) {
                String optionKey = configInfo.getKey();

                // Handle department-specific configs by removing the placeholder
                if (optionKey.contains("[Department Name]")) {
                    // Skip department-specific configs in export since they need actual department context
                    continue;
                }

                // Get current value from the application configuration
                String optionValue = configOptionApplicationController.getApplicationOption(optionKey) != null
                    ? configOptionApplicationController.getApplicationOption(optionKey).getOptionValue()
                    : "";

                // Determine value type (default to SHORT_TEXT if not determinable)
                String valueType = "SHORT_TEXT";
                if (configOptionApplicationController.getApplicationOption(optionKey) != null) {
                    valueType = configOptionApplicationController.getApplicationOption(optionKey).getValueType().toString();
                }

                String enumType = ""; // Most configs don't use enums

                // Escape quotes in values
                optionKey = escapeQuotes(optionKey);
                optionValue = escapeQuotes(optionValue);
                valueType = escapeQuotes(valueType);
                enumType = escapeQuotes(enumType);

                // Add CSV row
                csvContent.append("\"").append(optionKey).append("\",")
                          .append("\"").append(optionValue).append("\",")
                          .append("\"").append(valueType).append("\",")
                          .append("\"").append(enumType).append("\"\n");
            }

            // Generate filename with timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            String pageName = currentMetadata.getPageName() != null
                ? currentMetadata.getPageName().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "page_config";
            String filename = pageName + "_configuration_" + timestamp + ".csv";

            // Create streamed content
            InputStream stream = new ByteArrayInputStream(csvContent.toString().getBytes(StandardCharsets.UTF_8));
            return DefaultStreamedContent.builder()
                    .name(filename)
                    .contentType("text/csv")
                    .stream(() -> stream)
                    .build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting page configuration", e);
            JsfUtil.addErrorMessage("Error exporting configuration: " + e.getMessage());
            return null;
        }
    }

    /**
     * Escape quotes in CSV values
     * @param value The value to escape
     * @return Escaped value
     */
    private String escapeQuotes(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    // Getters and Setters

    public String getSelectedPagePath() {
        return selectedPagePath;
    }

    public void setSelectedPagePath(String selectedPagePath) {
        this.selectedPagePath = selectedPagePath;
    }

    public PageMetadata getCurrentMetadata() {
        return currentMetadata;
    }

    public void setCurrentMetadata(PageMetadata currentMetadata) {
        this.currentMetadata = currentMetadata;
    }

    public PageMetadataRegistry getMetadataRegistry() {
        return metadataRegistry;
    }

    public void setMetadataRegistry(PageMetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }
}
