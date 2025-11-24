/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.admin;

import com.divudi.core.data.OptionScope;
import java.io.Serializable;

/**
 * Represents information about a configuration option used on a page.
 * Used by the admin interface to display configuration metadata.
 *
 * @author Dr M H B Ariyaratne
 */
public class ConfigOptionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String description;
    private String usageLocation;
    private OptionScope scope;

    public ConfigOptionInfo() {
    }

    public ConfigOptionInfo(String key, String description, String usageLocation, OptionScope scope) {
        this.key = key;
        this.description = description;
        this.usageLocation = usageLocation;
        this.scope = scope;
    }

    /**
     * Convenience constructor without usage location for simpler usage.
     * Usage location can be set later via setter if needed.
     */
    public ConfigOptionInfo(String key, String description, OptionScope scope) {
        this.key = key;
        this.description = description;
        this.scope = scope;
        this.usageLocation = null; // Optional field
    }

    // Getters and Setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsageLocation() {
        return usageLocation;
    }

    public void setUsageLocation(String usageLocation) {
        this.usageLocation = usageLocation;
    }

    public OptionScope getScope() {
        return scope;
    }

    public void setScope(OptionScope scope) {
        this.scope = scope;
    }
}
