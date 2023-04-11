/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import java.util.Date;
import java.util.List;

/**
 *
 * @author chrishantha
 */
public class InvestigationResultForGraph {
    private Date dates;
    private String label;
    private List<String> seriesNames;
    private List<Double> seriesValues;

    public Date getDates() {
        return dates;
    }

    public void setDates(Date dates) {
        this.dates = dates;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getSeriesNames() {
        return seriesNames;
    }

    public void setSeriesNames(List<String> seriesNames) {
        this.seriesNames = seriesNames;
    }

    public List<Double> getSeriesValues() {
        return seriesValues;
    }

    public void setSeriesValues(List<Double> seriesValues) {
        this.seriesValues = seriesValues;
    }
    
    
    
}
