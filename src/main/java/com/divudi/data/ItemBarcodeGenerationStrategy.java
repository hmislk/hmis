/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author buddhika_ari
 */
public enum ItemBarcodeGenerationStrategy {
    BY_ITEM("By Item"),
    BY_BATCH("By Batch"),
    BY_INDIVIDUAL_UNIT("By Individual Unit");

    private final String description;

    ItemBarcodeGenerationStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
