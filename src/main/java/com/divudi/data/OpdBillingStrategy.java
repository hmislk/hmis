/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.divudi.data;

/**
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum OpdBillingStrategy {

    SINGLE_BILL_FOR_ALL_ORDERS("Single Bill for All Orders"),
    ONE_BILL_PER_DEPARTMENT("One Bill for each Department that provides the Service or Investigation"),
    ONE_BILL_PER_DEPARTMENT_AND_CATEGORY("One Bill for each Department and service/investigation category");

    private final String label;

    OpdBillingStrategy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
