/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.admin;

import java.io.Serializable;

/**
 * Represents information about a privilege used on a page.
 * Used by the admin interface to display privilege metadata.
 *
 * @author Dr M H B Ariyaratne
 */
public class PrivilegeInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String privilegeName;
    private String description;
    private String usageLocation;

    public PrivilegeInfo() {
    }

    public PrivilegeInfo(String privilegeName, String description, String usageLocation) {
        this.privilegeName = privilegeName;
        this.description = description;
        this.usageLocation = usageLocation;
    }

    /**
     * Convenience constructor without usage location for simpler usage.
     * Usage location can be set later via setter if needed.
     */
    public PrivilegeInfo(String privilegeName, String description) {
        this.privilegeName = privilegeName;
        this.description = description;
        this.usageLocation = null; // Optional field
    }

    // Getters and Setters

    public String getPrivilegeName() {
        return privilegeName;
    }

    public void setPrivilegeName(String privilegeName) {
        this.privilegeName = privilegeName;
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
}
