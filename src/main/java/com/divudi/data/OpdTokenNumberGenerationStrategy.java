/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.divudi.data;

/**
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum OpdTokenNumberGenerationStrategy {

    NO_TOKEN_GENERATION("No Token Generation"),
    BILLS_BY_DEPARTMENT("Bills by Department"),
    BILLS_BY_DEPARTMENT_AND_CATEGORY("Bills by Department and Category"),
    BILLS_BY_DEPARTMENT_CATEGORY_AND_FROMSTAFF("Bills by Department, Category, and fromStaff");

    private final String label;

    OpdTokenNumberGenerationStrategy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
