/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

/**
 * Raised by {@link PharmaceuticalItemApiService} when a create request matches an
 * existing, non-retired row by name (case-insensitive). Carries the already-loaded
 * existing entity's ID and response DTO directly, rather than just an ID for the
 * caller to re-fetch: a re-fetch call reopens a window where a concurrent retire
 * of that same row would turn the intended 409 already_exists into a confusing
 * 500 "not found" (findItemById excludes retired rows).
 *
 * <p>This is a checked (non-RuntimeException) exception on purpose: the service is a
 * {@code @Stateless} EJB, and unchecked exceptions escaping a business method are
 * wrapped by the container into {@code javax.ejb.EJBException} before the caller
 * sees them, which silently defeats a {@code catch} on the original type. A checked
 * exception is not wrapped and reaches {@link PharmaceuticalItemApi} intact.
 *
 * @author Buddhika
 */
public class DuplicateItemException extends Exception {

    private final Long existingId;
    private final Object existingDto;

    public DuplicateItemException(Long existingId, Object existingDto) {
        super("Duplicate item, ID: " + existingId);
        this.existingId = existingId;
        this.existingDto = existingDto;
    }

    public Long getExistingId() {
        return existingId;
    }

    public Object getExistingDto() {
        return existingDto;
    }
}
