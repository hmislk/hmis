/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import javax.ejb.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PharmaceuticalItemApiExceptionTest {

    @Test
    @DisplayName("PharmaceuticalItemApiException must be annotated with @ApplicationException(rollback = true)")
    public void testApplicationExceptionAnnotation() {
        ApplicationException ann = PharmaceuticalItemApiException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ann, "PharmaceuticalItemApiException must be annotated with @ApplicationException");
        assertTrue(ann.rollback(), "@ApplicationException rollback must be true");
    }

    @Test
    @DisplayName("Status defaults to 404 for not found and 400 otherwise")
    public void testDefaultStatus() {
        PharmaceuticalItemApiException notFound = new PharmaceuticalItemApiException("Item not found with ID: 1");
        assertEquals(404, notFound.getStatus());

        PharmaceuticalItemApiException badRequest = new PharmaceuticalItemApiException("Invalid VMPP reference");
        assertEquals(400, badRequest.getStatus());

        PharmaceuticalItemApiException explicit = new PharmaceuticalItemApiException("Custom error", 422);
        assertEquals(422, explicit.getStatus());
    }
}

