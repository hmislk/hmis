/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.sap;

/**
 * Thrown when an SAP integration operation fails (token fetch, API call, etc.).
 * Callers should catch this and return a graceful error response rather than
 * letting it propagate as an unchecked exception that would roll back transactions.
 */
public class SapIntegrationException extends Exception {

    public SapIntegrationException(String message) {
        super(message);
    }

    public SapIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
