/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.inward;

/**
 * The scope an InwardDiscountMatrix entry belongs to -- which family of
 * categories (pharmacy items, or services/investigations) it applies
 * within.
 *
 * Stored explicitly on every InwardDiscountMatrix row (not just wildcard
 * rows with no category), so InwardDiscountMatrixApi can filter by scope
 * with a plain equality check instead of inferring it from the category's
 * Java type via a JPQL type() discriminator check -- which proved unreliable
 * once the row's category is null (a wildcard row applying to every
 * category in the scope).
 *
 * @author Dr M H B Ariyaratne
 */
public enum InwardDiscountMatrixScope {

    PHARMACY("Pharmacy"),
    SERVICE("Service / Investigation");

    private final String label;

    InwardDiscountMatrixScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
