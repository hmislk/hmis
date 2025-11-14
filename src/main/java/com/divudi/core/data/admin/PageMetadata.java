/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata about a page including its configuration options and privileges.
 * Used by the admin interface to display comprehensive page configuration information.
 *
 * @author Dr M H B Ariyaratne
 */
public class PageMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pagePath;
    private String pageName;
    private String description;
    private String controllerClass;
    private List<ConfigOptionInfo> configOptions;
    private List<PrivilegeInfo> privileges;

    public PageMetadata() {
        this.configOptions = new ArrayList<>();
        this.privileges = new ArrayList<>();
    }

    public PageMetadata(String pagePath, String pageName, String description, String controllerClass) {
        this();
        this.pagePath = pagePath;
        this.pageName = pageName;
        this.description = description;
        this.controllerClass = controllerClass;
    }

    // Helper methods to add config options and privileges

    public void addConfigOption(ConfigOptionInfo configOption) {
        this.configOptions.add(configOption);
    }

    public void addPrivilege(PrivilegeInfo privilege) {
        this.privileges.add(privilege);
    }

    // Getters and Setters

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(String controllerClass) {
        this.controllerClass = controllerClass;
    }

    public List<ConfigOptionInfo> getConfigOptions() {
        return configOptions;
    }

    public void setConfigOptions(List<ConfigOptionInfo> configOptions) {
        this.configOptions = (configOptions != null) ? configOptions : new ArrayList<>();
    }

    public List<PrivilegeInfo> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<PrivilegeInfo> privileges) {
        this.privileges = (privileges != null) ? privileges : new ArrayList<>();
    }
}
