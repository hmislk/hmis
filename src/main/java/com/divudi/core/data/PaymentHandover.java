package com.divudi.core.data;

/**
 *
 * @author Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 * Consultant in Health Informatics
 *
 */
public enum PaymentHandover {
    FLOATS("Floats"),
    USER_COLLECTED("Collected by User"),
    OTHER_USERS_COLLECTED_AND_HANDED_OVER("Collected by Other Users and Handed Over");

    private final String description;

    PaymentHandover(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
