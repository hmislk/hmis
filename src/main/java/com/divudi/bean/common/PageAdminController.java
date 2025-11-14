/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;

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
     * Navigate back to the original page
     * @return Navigation outcome (returns null to stay on current page per JSF convention)
     */
    public String navigateBackToPage() {
        if (selectedPagePath != null && !selectedPagePath.isEmpty()) {
            return "/" + selectedPagePath + "?faces-redirect=true";
        }
        // Return null per JSF convention: null navigation outcome means "stay on current page"
        return null;
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
