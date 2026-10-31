package com.divudi.service.pharmacy;

import javax.ejb.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GrnApprovalExceptionTest {

    @Test
    public void grnApprovalExceptionHasApplicationExceptionAnnotation() {
        ApplicationException ann = GrnApprovalException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ann, "GrnApprovalException must be annotated with @ApplicationException");
        assertTrue(ann.rollback(), "@ApplicationException rollback must be true");
    }

    @Test
    public void exceptionStoresMessage() {
        GrnApprovalException ex = new GrnApprovalException("GRN already approved");
        assertEquals("GRN already approved", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }
}
