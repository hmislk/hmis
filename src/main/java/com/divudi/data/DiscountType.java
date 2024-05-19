package com.divudi.data;

/**
 * Enumerates various types of discounts.
 * Designed to support different schemes for both individuals and families.
 * Each enum constant includes a label for better readability.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 * @author Assistant at OpenAI
 */
public enum DiscountType {

    INDIVIDUAL_MEMBERSHIP("Individual Membership"),
    INDIVIDUAL_LOYALTY_CARD("Individual Loyalty Card"),
    FAMILY_MEMBERSHIP("Family Membership"),
    FAMILY_LOYALTY_CARD("Family Loyalty Card"),
    DISCOUNT_SCHEME("Discount Scheme"),
    STAFF_DISCOUNT("Staff Discount");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
