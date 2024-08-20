/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import java.util.List;

/**
 *
 * @author buddh
 */
public class JsonInvestigation {
    String name;
    String printingName;
    String fullName;
    String code;
    String category;
    String sample;
    String tube;
    String reportFormat;
    String container;
    String analyzer;
    String worksheet;
    String reportType;
    List<JsonInvestigationItem> ixItems;
    List<JsonItemFee> itemFees;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrintingName() {
        return printingName;
    }

    public void setPrintingName(String printingName) {
        this.printingName = printingName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getTube() {
        return tube;
    }

    public void setTube(String tube) {
        this.tube = tube;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public String getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public List<JsonInvestigationItem> getIxItems() {
        return ixItems;
    }

    public void setIxItems(List<JsonInvestigationItem> ixItems) {
        this.ixItems = ixItems;
    }

    public List<JsonItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<JsonItemFee> itemFees) {
        this.itemFees = itemFees;
    }
    
    
    
    
    
}
