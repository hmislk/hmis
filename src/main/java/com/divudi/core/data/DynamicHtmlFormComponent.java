package com.divudi.core.data;

import com.divudi.core.entity.web.CaptureComponent;

/**
 * Represents a component of a dynamic HTML form which can be either a segment of HTML content
 * or a JSF component wrapped by a CaptureComponent.
 */
public class DynamicHtmlFormComponent {

    private String htmlContent; // HTML content when it's not a JSF component
    private CaptureComponent jsfComponent;   // Holds the JSF component if applicable
    private boolean containsJsfComponent; // True if this component wraps a JSF component

    /**
     * Constructor for HTML content segments.
     * @param htmlContent the static HTML content
     */
    public DynamicHtmlFormComponent(String htmlContent) {
        this.htmlContent = htmlContent;
        this.jsfComponent = null;
        this.containsJsfComponent = false;
    }

    /**
     * Constructor for JSF components.
     * @param component the JSF component to wrap
     */
    public DynamicHtmlFormComponent(CaptureComponent component) {
        this.jsfComponent = component;
        this.htmlContent = null;
        this.containsJsfComponent = true;
    }

    // Standard getters and setters
    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public CaptureComponent getJsfComponent() {
        return jsfComponent;
    }

    public void setJsfComponent(CaptureComponent component) {
        this.jsfComponent = component;
    }

    public boolean isContainsJsfComponent() {
        return containsJsfComponent;
    }

    public void setContainsJsfComponent(boolean containsJsfComponent) {
        this.containsJsfComponent = containsJsfComponent;
    }


}
