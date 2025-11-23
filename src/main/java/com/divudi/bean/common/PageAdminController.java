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
