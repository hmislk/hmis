/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.divudi.data;

/**
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum ItemListingStrategy {

    ALL_ITEMS("All Items"),
    ITEMS_OF_LOGGED_DEPARTMENT("Items of the Logged Department"),
    ITEMS_OF_LOGGED_INSTITUTION("Items of the Logged Institution"),
    ITEMS_OF_SELECTED_DEPARTMENT("Items of the Selected Department"),
    ITEMS_OF_SELECTED_INSTITUTIONS("Items of the Selected Institutions"),
    ITEMS_MAPPED_TO_LOGGED_DEPARTMENT("Items Mapped to the Logged Department"),
    ITEMS_MAPPED_TO_LOGGED_INSTITUTION("Items Mapped to the Logged Institution"),
    ITEMS_MAPPED_TO_SELECTED_DEPARTMENT("Items Mapped to the Selected Department"),
    ITEMS_MAPPED_TO_SELECTED_INSTITUTION("Items Mapped to the Selected Institution");

    private final String label;

    ItemListingStrategy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
