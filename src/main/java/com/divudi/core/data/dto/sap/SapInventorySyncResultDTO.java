/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.sap;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary result returned after a SAP → HMIS inventory sync run.
 */
public class SapInventorySyncResultDTO {

    private int totalDocumentItems;
    private int matchedItems;
    private int unmatchedItems;
    private String syncFromDate;
    private String syncToDate;
    private String lastSyncTimestamp;
    private List<String> unmatchedMaterials = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public SapInventorySyncResultDTO() {
    }

    public int getTotalDocumentItems() { return totalDocumentItems; }
    public void setTotalDocumentItems(int totalDocumentItems) { this.totalDocumentItems = totalDocumentItems; }
    public int getMatchedItems() { return matchedItems; }
    public void setMatchedItems(int matchedItems) { this.matchedItems = matchedItems; }
    public int getUnmatchedItems() { return unmatchedItems; }
    public void setUnmatchedItems(int unmatchedItems) { this.unmatchedItems = unmatchedItems; }
    public String getSyncFromDate() { return syncFromDate; }
    public void setSyncFromDate(String syncFromDate) { this.syncFromDate = syncFromDate; }
    public String getSyncToDate() { return syncToDate; }
    public void setSyncToDate(String syncToDate) { this.syncToDate = syncToDate; }
    public String getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(String lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }
    public List<String> getUnmatchedMaterials() { return unmatchedMaterials; }
    public void setUnmatchedMaterials(List<String> unmatchedMaterials) { this.unmatchedMaterials = unmatchedMaterials; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }
    public void addUnmatched(String material) { this.unmatchedMaterials.add(material); }
}
