/*
 * Open Hospital Management Information System
 *
 * Data structure for the auto-generated roster table.
 * Layout: shifts as rows, dates as columns, staff names in cells.
 *
 * Package: com.divudi.core.data.dataStructure
 * File: RosterTable.java
 */
package com.divudi.core.data.dataStructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holds the complete generated roster table.
 *
 * Structure:
 *   dates  = column headers (one per date in range)
 *   rows   = one per shift (ordered by shiftOrder)
 *   Each row has cells (one per date), each cell has a list of assigned staff names.
 */
public class RosterTable implements Serializable {

    private List<Date> dates;              // column headers
    private List<RosterRow> rows;          // one per shift
    private List<String> warnings;         // understaffing warnings etc.

    public RosterTable() {
        this.dates = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public List<Date> getDates() {
        return dates;
    }

    public void setDates(List<Date> dates) {
        this.dates = dates;
    }

    public List<RosterRow> getRows() {
        return rows;
    }

    public void setRows(List<RosterRow> rows) {
        this.rows = rows;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}