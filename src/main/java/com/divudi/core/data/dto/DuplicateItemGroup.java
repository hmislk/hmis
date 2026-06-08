package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A group of pharmaceutical items flagged as possible duplicates of each other
 * by the "Possible Duplicate Items" report.
 *
 * The {@link #matchType} explains why they were grouped (same code, same
 * barcode, same normalised name, or a fuzzy name similarity), and
 * {@link #matchKey} is the shared value (e.g. the code or normalised name).
 */
public class DuplicateItemGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /** How the members were matched. */
    public enum MatchType {
        CODE("Same Code"),
        BARCODE("Same Barcode"),
        NAME("Same Name"),
        FUZZY_NAME("Similar Name");

        private final String label;

        MatchType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private MatchType matchType;
    private String matchKey;
    private List<DuplicateItemDto> items;

    public DuplicateItemGroup() {
        this.items = new ArrayList<>();
    }

    public DuplicateItemGroup(MatchType matchType, String matchKey, List<DuplicateItemDto> items) {
        this.matchType = matchType;
        this.matchKey = matchKey;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getMatchTypeLabel() {
        return matchType != null ? matchType.getLabel() : "";
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public String getMatchKey() {
        return matchKey;
    }

    public void setMatchKey(String matchKey) {
        this.matchKey = matchKey;
    }

    public List<DuplicateItemDto> getItems() {
        return items;
    }

    public void setItems(List<DuplicateItemDto> items) {
        this.items = items;
    }
}
