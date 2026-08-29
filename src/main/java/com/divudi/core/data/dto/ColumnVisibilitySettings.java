package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for storing column visibility and table preferences
 * Used for persistent user-specific UI settings
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class ColumnVisibilitySettings implements Serializable {

    private static final long serialVersionUID = 1L;

    // Map of column ID to visibility status
    private Map<String, Boolean> columnVisible;

    // Map of column ID to display order
    private Map<String, Integer> columnOrder;

    // Rows per page preference
    private Integer pageSize;

    // Default sort field
    private String sortField;

    // Sort order (ASC/DESC)
    private String sortOrder;

    public ColumnVisibilitySettings() {
        this.columnVisible = new HashMap<>();
        this.columnOrder = new HashMap<>();
    }

    /**
     * Check if a column is visible
     * @param columnId The column identifier
     * @return true if visible, false otherwise (default: true)
     */
    public boolean isColumnVisible(String columnId) {
        return columnVisible.getOrDefault(columnId, true);
    }

    /**
     * Set column visibility
     * @param columnId The column identifier
     * @param visible Whether the column should be visible
     */
    public void setColumnVisible(String columnId, boolean visible) {
        columnVisible.put(columnId, visible);
    }

    /**
     * Get column display order
     * @param columnId The column identifier
     * @return The display order, or null if not set
     */
    public Integer getColumnOrder(String columnId) {
        return columnOrder.get(columnId);
    }

    /**
     * Set column display order
     * @param columnId The column identifier
     * @param order The display order
     */
    public void setColumnOrder(String columnId, Integer order) {
        columnOrder.put(columnId, order);
    }

    // Getters and Setters

    public Map<String, Boolean> getColumnVisible() {
        return columnVisible;
    }

    public void setColumnVisible(Map<String, Boolean> columnVisible) {
        this.columnVisible = columnVisible;
    }

    public Map<String, Integer> getColumnOrder() {
        return columnOrder;
    }

    public void setColumnOrder(Map<String, Integer> columnOrder) {
        this.columnOrder = columnOrder;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
